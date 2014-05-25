/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: TestConverter.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.tools;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.cli.InputHandler;
import org.apache.fop.tools.anttasks.FileCompare;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * TestConverter is used to process a set of tests specified in a testsuite.
 * This class retrieves the data in the testsuite and uses FOP to convert the
 * xml and xsl file into either an xml representation of the area tree or a pdf
 * document. The area tree can be used for automatic comparisons between
 * different versions of FOP or the pdf can be view for manual checking and pdf
 * rendering.
 */
@Slf4j
public class TestConverter {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();

    private boolean failOnly = false;
    private String outputFormat = MimeConstants.MIME_FOP_AREA_TREE;
    private File destdir;
    private File compare = null;
    private String baseDir = "./";
    private final Map differ = new java.util.HashMap();

    /**
     * This main method can be used to run the test converter from the command
     * line. This will take a specified testsuite xml and process all tests in
     * it. The command line options are: -b to set the base directory for where
     * the testsuite and associated files are -failOnly to process only the
     * tests which are specified as fail in the test results -pdf to output the
     * result as pdf
     *
     * @param args
     *            command-line arguments
     */
    public static void main(final String[] args) {
        if (args == null || args.length == 0) {
            log.info("test suite file name required");
            return;
        }
        final TestConverter tc = new TestConverter();

        String results = "results";
        String testFile = null;
        for (int count = 0; count < args.length; count++) {
            if (args[count].equals("-failOnly")) {
                tc.setFailOnly(true);
            } else if (args[count].equals("-pdf")) {
                tc.setOutputFormat(org.apache.xmlgraphics.util.MimeConstants.MIME_PDF);
            } else if (args[count].equals("-rtf")) {
                tc.setOutputFormat(org.apache.xmlgraphics.util.MimeConstants.MIME_RTF);
            } else if (args[count].equals("-ps")) {
                tc.setOutputFormat(org.apache.xmlgraphics.util.MimeConstants.MIME_POSTSCRIPT);
            } else if (args[count].equals("-b")) {
                tc.setBaseDir(args[++count]);
            } else if (args[count].equals("-results")) {
                results = args[++count];
            } else {
                testFile = args[count];
            }
        }
        if (testFile == null) {
            log.info("test suite file name required");
        }

        tc.runTests(testFile, results, null);
    }

    /**
     * Construct a new TestConverter
     */
    public TestConverter() {
    }

    /**
     * Controls output format to generate
     *
     * @param outputFormat
     *            the MIME type of the output format
     */
    public void setOutputFormat(final String outputFormat) {
        this.outputFormat = outputFormat;
    }

    /**
     * Controls whether to process only the tests which are specified as fail in
     * the test results.
     *
     * @param fail
     *            True if only fail tests should be processed
     */
    public void setFailOnly(final boolean fail) {
        this.failOnly = fail;
    }

    /**
     * Sets the base directory.
     *
     * @param str
     *            base directory
     */
    public void setBaseDir(final String str) {
        this.baseDir = str;
    }

    /**
     * Run the Tests. This runs the tests specified in the xml file fname. The
     * document is read as a dom and each testcase is covered.
     *
     * @param fname
     *            filename of the input file
     * @param dest
     *            destination directory
     * @param compDir
     *            comparison directory
     * @return Map a Map containing differences
     */
    public Map runTests(final String fname, final String dest,
            final String compDir) {
        log.debug("running tests in file:" + fname);
        try {
            if (compDir != null) {
                this.compare = new File(this.baseDir + "/" + compDir);
            }
            this.destdir = new File(this.baseDir + "/" + dest);
            this.destdir.mkdirs();
            final File f = new File(this.baseDir + "/" + fname);
            final DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            final DocumentBuilder db = factory.newDocumentBuilder();
            final Document doc = db.parse(f);

            final NodeList suitelist = doc.getChildNodes();
            if (suitelist.getLength() == 0) {
                return this.differ;
            }

            Node testsuite = null;
            testsuite = doc.getDocumentElement();

            if (testsuite.hasAttributes()) {
                final String profile = testsuite.getAttributes()
                        .getNamedItem("profile").getNodeValue();
                log.debug("testing test suite:" + profile);
            }

            final NodeList testcases = testsuite.getChildNodes();
            for (int count = 0; count < testcases.getLength(); count++) {
                final Node testcase = testcases.item(count);
                if (testcase.getNodeName().equals("testcases")) {
                    runTestCase(testcase);
                }
            }
        } catch (final Exception e) {
            log.error("Error while running tests", e);
        }
        return this.differ;
    }

    /**
     * Run a test case. This goes through a test case in the document. A
     * testcase can contain a test, a result or more test cases. A test case is
     * handled recursively otherwise the test is run.
     *
     * @param tcase
     *            Test case node to run
     */
    protected void runTestCase(final Node tcase) {
        if (tcase.hasAttributes()) {
            final String profile = tcase.getAttributes()
                    .getNamedItem("profile").getNodeValue();
            log.debug("testing profile:" + profile);
        }

        final NodeList cases = tcase.getChildNodes();
        for (int count = 0; count < cases.getLength(); count++) {
            final Node node = cases.item(count);
            final String nodename = node.getNodeName();
            if (nodename.equals("testcases")) {
                runTestCase(node);
            } else if (nodename.equals("test")) {
                runTest(tcase, node);
            } else if (nodename.equals("result")) {
                // nop
            }

        }

    }

    /**
     * Run a particular test. This runs a test defined by the xml and xsl
     * documents. If the test has a result specified it is checked. This creates
     * an XSLTInputHandler to provide the input for FOP and writes the data out
     * to an XML are tree.
     *
     * @param testcase
     *            Test case to run
     * @param test
     *            Test
     */
    protected void runTest(final Node testcase, final Node test) {
        final String id = test.getAttributes().getNamedItem("id")
                .getNodeValue();
        final Node result = locateResult(testcase, id);
        boolean pass = false;
        if (result != null) {
            final String agreement = result.getAttributes()
                    .getNamedItem("agreement").getNodeValue();
            pass = agreement.equals("full");
        }

        if (pass && this.failOnly) {
            return;
        }

        final String xml = test.getAttributes().getNamedItem("xml")
                .getNodeValue();
        final Node xslNode = test.getAttributes().getNamedItem("xsl");
        String xsl = null;
        if (xslNode != null) {
            xsl = xslNode.getNodeValue();
        }
        log.debug("converting xml:" + xml + " and xsl:" + xsl + " to area tree");

        String res = xml;
        final Node resNode = test.getAttributes().getNamedItem("results");
        if (resNode != null) {
            res = resNode.getNodeValue();
        }
        try {
            final File xmlFile = new File(this.baseDir + "/" + xml);
            String baseURL = null;
            try {
                baseURL = xmlFile.getParentFile().toURI().toURL()
                        .toExternalForm();
            } catch (final Exception e) {
                log.error("Error setting base directory");
            }

            InputHandler inputHandler = null;
            if (xsl == null) {
                inputHandler = new InputHandler(xmlFile);
            } else {
                inputHandler = new InputHandler(xmlFile, new File(this.baseDir
                        + "/" + xsl), null);
            }

            final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();
            userAgent.setBaseURL(baseURL);

            userAgent.getRendererOptions().put("fineDetail", Boolean.FALSE);
            userAgent.getRendererOptions().put("consistentOutput",
                    new Boolean(true));
            userAgent.setProducer("Testsuite Converter");

            String outname = res;
            if (outname.endsWith(".xml") || outname.endsWith(".pdf")) {
                outname = outname.substring(0, outname.length() - 4);
            }
            final File outputFile = new File(this.destdir, outname
                    + makeResultExtension());

            outputFile.getParentFile().mkdirs();
            final OutputStream outStream = new java.io.BufferedOutputStream(
                    new java.io.FileOutputStream(outputFile));
            log.debug("ddir:" + this.destdir + " on:" + outputFile.getName());
            inputHandler.renderTo(userAgent, this.outputFormat, outStream);
            outStream.close();

            // check difference
            if (this.compare != null) {
                final File f1 = new File(this.destdir, outname + ".at.xml");
                final File f2 = new File(this.compare, outname + ".at.xml");
                if (!compareFiles(f1, f2)) {
                    this.differ.put(outname + ".at.xml", new Boolean(pass));
                }
            }
        } catch (final Exception e) {
            log.error("Error while running tests", e);
        }
    }

    /**
     * Return a suitable file extension for the output format.
     */
    private String makeResultExtension() {
        if (org.apache.xmlgraphics.util.MimeConstants.MIME_PDF
                .equals(this.outputFormat)) {
            return ".pdf";
        } else if (org.apache.xmlgraphics.util.MimeConstants.MIME_RTF
                .equals(this.outputFormat)) {
            return ".rtf";
        } else if (org.apache.xmlgraphics.util.MimeConstants.MIME_POSTSCRIPT
                .equals(this.outputFormat)) {
            return ".ps";
        } else {
            return ".at.xml";
        }
    }

    /**
     * Compare files.
     *
     * @param f1
     *            first file
     * @param f2
     *            second file
     * @return true if equal
     */
    protected boolean compareFiles(final File f1, final File f2) {
        try {
            return FileCompare.compareFiles(f1, f2);
        } catch (final Exception e) {
            log.error("Error while comparing files", e);
            return false;
        }
    }

    private Node locateResult(final Node testcase, final String id) {
        final NodeList cases = testcase.getChildNodes();
        for (int count = 0; count < cases.getLength(); count++) {
            final Node node = cases.item(count);
            final String nodename = node.getNodeName();
            if (nodename.equals("result")) {
                final String resultid = node.getAttributes().getNamedItem("id")
                        .getNodeValue();
                if (id.equals(resultid)) {
                    return node;
                }
            }
        }
        return null;
    }

}
