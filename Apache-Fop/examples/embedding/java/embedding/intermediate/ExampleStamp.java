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

/* $Id: ExampleStamp.java 747752 2009-02-25 11:31:16Z jeremias $ */

package embedding.intermediate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFParser;
import org.apache.fop.render.intermediate.IFUtil;
import org.xml.sax.SAXException;

import embedding.ExampleObj2XML;
import embedding.model.ProjectTeam;

/**
 * Example for the intermediate format that demonstrates the stamping of a
 * document with some kind of watermark. The resulting document is then rendered
 * to a PDF file.
 */
@Slf4j
public class ExampleStamp {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();

    /**
     * Stamps an intermediate file and renders it to a PDF file.
     *
     * @param iffile
     *            the intermediate file (area tree XML)
     * @param stampSheet
     *            the stylesheet that does the stamping
     * @param pdffile
     *            the target PDF file
     * @throws IOException
     *             In case of an I/O problem
     * @throws TransformerException
     *             In case of a XSL transformation problem
     * @throws SAXException
     *             In case of an XML-related problem
     * @throws IFException
     *             if there was an IF-related error while creating the output
     *             file
     */
    public void stampToPDF(final File iffile, final File stampSheet,
            final File pdffile) throws IOException, TransformerException,
            SAXException, IFException {
        // Setup output
        OutputStream out = new java.io.FileOutputStream(pdffile);
        out = new java.io.BufferedOutputStream(out);
        try {
            // user agent
            final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();

            // Setup target handler
            final String mime = org.apache.xmlgraphics.util.MimeConstants.MIME_PDF;
            final IFDocumentHandler targetHandler = this.fopFactory
                    .getRendererFactory()
                    .createDocumentHandler(userAgent, mime);

            // Setup fonts
            IFUtil.setupFonts(targetHandler);
            targetHandler.setResult(new StreamResult(pdffile));

            final IFParser parser = new IFParser();

            final Source src = new StreamSource(iffile);
            final Source xslt = new StreamSource(stampSheet);

            // Setup Transformer for XSLT processing
            final TransformerFactory tFactory = TransformerFactory
                    .newInstance();
            final Transformer transformer = tFactory.newTransformer(xslt);

            // Send XSLT result to AreaTreeParser
            final SAXResult res = new SAXResult(parser.getContentHandler(
                    targetHandler, userAgent));

            // Start XSLT transformation and area tree parsing
            transformer.transform(src, res);
        } finally {
            out.close();
        }
    }

    /**
     * Main method.
     *
     * @param args
     *            command-line arguments
     */
    public static void main(final String[] args) {
        try {
            log.info("FOP ExampleConcat (for the Intermediate Format)\n");

            // Setup directories
            final File baseDir = new File(".");
            final File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup output file
            final File xsltfile = new File(baseDir,
                    "xml/xslt/projectteam2fo.xsl");
            final File iffile = new File(outDir, "team.if.xml");
            final File stampxsltfile = new File(baseDir, "xml/xslt/ifstamp.xsl");
            final File pdffile = new File(outDir, "ResultIFStamped.pdf");
            log.info("Intermediate file : " + iffile.getCanonicalPath());
            log.info("Stamp XSLT: " + stampxsltfile.getCanonicalPath());
            log.info("PDF Output File: " + pdffile.getCanonicalPath());
            log.info("");

            final ProjectTeam team1 = ExampleObj2XML.createSampleProjectTeam();

            // Create intermediate file
            final ExampleConcat concatapp = new ExampleConcat();
            concatapp.convertToIntermediate(team1.getSourceForProjectTeam(),
                    new StreamSource(xsltfile), iffile);

            // Stamp document and produce a PDF from the intermediate format
            final ExampleStamp app = new ExampleStamp();
            app.stampToPDF(iffile, stampxsltfile, pdffile);

            log.info("Success!");

        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

}
