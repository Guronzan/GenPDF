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

/* $Id: LayoutEngineTestCase.java 1237582 2012-01-30 09:49:22Z mehdi $ */

package org.apache.fop.layoutengine;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.fop.DebugHelper;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.area.AreaTreeModel;
import org.apache.fop.area.AreaTreeParser;
import org.apache.fop.area.RenderPagesModel;
import org.apache.fop.events.model.EventSeverity;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.intermediate.IFTester;
import org.apache.fop.intermediate.TestAssistant;
import org.apache.fop.layoutmgr.ElementListObserver;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFRenderer;
import org.apache.fop.render.intermediate.IFSerializer;
import org.apache.fop.render.xml.XMLRenderer;
import org.apache.fop.util.ConsoleEventListenerForTests;
import org.apache.fop.util.DelegatingContentHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Class for testing the FOP's layout engine using testcases specified in XML
 * files.
 */
@RunWith(Parameterized.class)
public class LayoutEngineTestCase {
    private static File areaTreeBackupDir;

    @BeforeClass
    public static void makeDirAndRegisterDebugHelper() throws IOException {
        DebugHelper.registerStandardElementListObservers();
        areaTreeBackupDir = new File("build/test-results/layoutengine");
        if (!areaTreeBackupDir.mkdirs() && !areaTreeBackupDir.exists()) {
            throw new IOException(
                    "Failed to create the layout engine directory at "
                            + "build/test-results/layoutengine");
        }
    }

    /**
     * Creates the parameters for this test.
     *
     * @return the list of file arrays populated with test files
     * @throws IOException
     *             if an I/O error occurs while reading the test file
     */
    @Parameters
    public static Collection<File[]> getParameters() throws IOException {
        return LayoutEngineTestUtils.getLayoutTestFiles();
    }

    private final TestAssistant testAssistant = new TestAssistant();

    private final LayoutEngineChecksFactory layoutEngineChecksFactory = new LayoutEngineChecksFactory();

    private final IFTester ifTester;
    private final File testFile;

    private final TransformerFactory tfactory = TransformerFactory
            .newInstance();

    /**
     * Constructs a new instance.
     *
     * @param testFile
     *            the test file
     */
    public LayoutEngineTestCase(final File testFile) {
        this.ifTester = new IFTester(this.tfactory, areaTreeBackupDir);
        this.testFile = testFile;
    }

    /**
     * Runs a single layout engine test case.
     * 
     * @throws TransformerException
     *             In case of an XSLT/JAXP problem
     * @throws IOException
     *             In case of an I/O problem
     * @throws SAXException
     *             In case of a problem during SAX processing
     * @throws ParserConfigurationException
     *             In case of a problem with the XML parser setup
     */
    @Test
    public void runTest() throws TransformerException, SAXException,
            IOException, ParserConfigurationException {

        final DOMResult domres = new DOMResult();

        final ElementListCollector elCollector = new ElementListCollector();
        ElementListObserver.addObserver(elCollector);

        Fop fop;
        FopFactory effFactory;
        try {
            final Document testDoc = this.testAssistant
                    .loadTestCase(this.testFile);
            effFactory = this.testAssistant.getFopFactory(testDoc);

            // Setup Transformer to convert the testcase XML to XSL-FO
            final Transformer transformer = this.testAssistant
                    .getTestcase2FOStylesheet().newTransformer();
            final Source src = new DOMSource(testDoc);

            // Setup Transformer to convert the area tree to a DOM
            TransformerHandler athandler;
            athandler = this.testAssistant.getTransformerFactory()
                    .newTransformerHandler();
            athandler.setResult(domres);

            // Setup FOP for area tree rendering
            final FOUserAgent ua = effFactory.newFOUserAgent();
            ua.setBaseURL(this.testFile.getParentFile().toURI().toURL()
                    .toString());
            ua.getEventBroadcaster().addEventListener(
                    new ConsoleEventListenerForTests(this.testFile.getName(),
                            EventSeverity.WARN));

            final XMLRenderer atrenderer = new XMLRenderer(ua);
            atrenderer.setContentHandler(athandler);
            ua.setRendererOverride(atrenderer);
            fop = effFactory.newFop(ua);

            final SAXResult fores = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, fores);
        } finally {
            ElementListObserver.removeObserver(elCollector);
        }

        final Document doc = (Document) domres.getNode();
        if (areaTreeBackupDir != null) {
            this.testAssistant.saveDOM(doc, new File(areaTreeBackupDir,
                    this.testFile.getName() + ".at.xml"));
        }
        final FormattingResults results = fop.getResults();
        final LayoutResult result = new LayoutResult(doc, elCollector, results);
        checkAll(effFactory, this.testFile, result);
    }

    /**
     * Perform all checks on the area tree and, optionally, on the intermediate
     * format.
     * 
     * @param fopFactory
     *            the FOP factory
     * @param testFile
     *            Test case XML file
     * @param result
     *            The layout results
     * @throws TransformerException
     *             if a problem occurs in XSLT/JAXP
     */
    protected void checkAll(final FopFactory fopFactory, final File testFile,
            final LayoutResult result) throws TransformerException {
        final Element testRoot = this.testAssistant.getTestRoot(testFile);

        NodeList nodes;
        // AT tests only when checks are available
        nodes = testRoot.getElementsByTagName("at-checks");
        if (nodes.getLength() > 0) {
            final Element atChecks = (Element) nodes.item(0);
            doATChecks(atChecks, result);
        }

        // IF tests only when checks are available
        nodes = testRoot.getElementsByTagName("if-checks");
        if (nodes.getLength() > 0) {
            final Element ifChecks = (Element) nodes.item(0);
            final Document ifDocument = createIF(fopFactory, testFile,
                    result.getAreaTree());
            this.ifTester.doIFChecks(testFile.getName(), ifChecks, ifDocument);
        }
    }

    private Document createIF(final FopFactory fopFactory, final File testFile,
            final Document areaTreeXML) throws TransformerException {
        try {
            final FOUserAgent ua = fopFactory.newFOUserAgent();
            ua.setBaseURL(testFile.getParentFile().toURI().toURL()
                    .toExternalForm());
            ua.getEventBroadcaster().addEventListener(
                    new ConsoleEventListenerForTests(testFile.getName(),
                            EventSeverity.WARN));

            final IFRenderer ifRenderer = new IFRenderer(ua);

            final IFSerializer serializer = new IFSerializer();
            serializer.setContext(new IFContext(ua));
            final DOMResult result = new DOMResult();
            serializer.setResult(result);
            ifRenderer.setDocumentHandler(serializer);

            ua.setRendererOverride(ifRenderer);
            final FontInfo fontInfo = new FontInfo();
            // Construct the AreaTreeModel that will received the individual
            // pages
            final AreaTreeModel treeModel = new RenderPagesModel(ua, null,
                    fontInfo, null);

            // Iterate over all intermediate files
            final AreaTreeParser parser = new AreaTreeParser();
            final ContentHandler handler = parser.getContentHandler(treeModel,
                    ua);

            final DelegatingContentHandler proxy = new DelegatingContentHandler() {

                @Override
                public void endDocument() throws SAXException {
                    super.endDocument();
                    // Signal the end of the processing.
                    // The renderer can finalize the target document.
                    treeModel.endDocument();
                }

            };
            proxy.setDelegateContentHandler(handler);

            final Transformer transformer = this.tfactory.newTransformer();
            transformer.transform(new DOMSource(areaTreeXML), new SAXResult(
                    proxy));

            return (Document) result.getNode();
        } catch (final Exception e) {
            throw new TransformerException(
                    "Error while generating intermediate format file: "
                            + e.getMessage(), e);
        }
    }

    private void doATChecks(final Element checksRoot, final LayoutResult result) {
        final List<LayoutEngineCheck> checks = this.layoutEngineChecksFactory
                .createCheckList(checksRoot);
        if (checks.size() == 0) {
            throw new RuntimeException("No available area tree check");
        }
        for (final LayoutEngineCheck check : checks) {
            check.check(result);
        }
    }

}
