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

/* $Id: ExampleConcat.java 747752 2009-02-25 11:31:16Z jeremias $ */

package embedding.intermediate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFSerializer;
import org.apache.fop.render.intermediate.IFUtil;
import org.apache.fop.render.intermediate.util.IFConcatenator;
import org.xml.sax.SAXException;

import embedding.ExampleObj2XML;
import embedding.model.ProjectMember;
import embedding.model.ProjectTeam;

/**
 * Example for the intermediate format that demonstrates the concatenation of
 * two documents rendered to the intermediate format. A single PDF file is
 * generated from the two intermediate files.
 */
@Slf4j
public class ExampleConcat {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();

    /**
     * Creates a sample ProjectTeam instance for this demo.
     *
     * @return ProjectTeam the newly created ProjectTeam instance
     */
    public static ProjectTeam createAnotherProjectTeam() {
        final ProjectTeam team = new ProjectTeam();
        team.setProjectName("The Dynamic Duo");
        team.addMember(new ProjectMember("Batman", "lead", "batman@heroes.org"));
        team.addMember(new ProjectMember("Robin", "aid", "robin@heroes.org"));
        return team;
    }

    /**
     * Converts an XSL-FO document to an intermediate file.
     *
     * @param src
     *            the source file
     * @param xslt
     *            the stylesheet file
     * @param intermediate
     *            the target intermediate file
     * @throws IOException
     *             In case of an I/O problem
     * @throws FOPException
     *             In case of a FOP problem
     * @throws TransformerException
     *             In case of a XSL transformation problem
     */
    public void convertToIntermediate(final Source src, final Source xslt,
            final File intermediate) throws IOException, FOPException,
            TransformerException {

        // Create a user agent
        final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();

        // Create an instance of the target document handler so the IFSerializer
        // can use its font setup
        final IFDocumentHandler targetHandler = userAgent.getRendererFactory()
                .createDocumentHandler(userAgent,
                        org.apache.xmlgraphics.util.MimeConstants.MIME_PDF);

        // Create the IFSerializer to write the intermediate format
        final IFSerializer ifSerializer = new IFSerializer();
        ifSerializer.setContext(new IFContext(userAgent));

        // Tell the IFSerializer to mimic the target format
        ifSerializer.mimicDocumentHandler(targetHandler);

        // Make sure the prepared document handler is used
        userAgent.setDocumentHandlerOverride(ifSerializer);

        // Setup output
        OutputStream out = new java.io.FileOutputStream(intermediate);
        out = new java.io.BufferedOutputStream(out);
        try {
            // Construct FOP (the MIME type here is unimportant due to the
            // override
            // on the user agent)
            final Fop fop = this.fopFactory.newFop(null, userAgent, out);

            // Setup XSLT
            final TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer;
            if (xslt != null) {
                transformer = factory.newTransformer(xslt);
            } else {
                transformer = factory.newTransformer();
            }

            // Resulting SAX events (the generated FO) must be piped through to
            // FOP
            final Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);
        } finally {
            out.close();
        }
    }

    /**
     * Concatenates an array of intermediate files to a single PDF file.
     *
     * @param files
     *            the array of intermediate files
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
    public void concatToPDF(final File[] files, final File pdffile)
            throws IOException, TransformerException, SAXException, IFException {
        // Setup output
        OutputStream out = new java.io.FileOutputStream(pdffile);
        out = new java.io.BufferedOutputStream(out);
        try {
            // Setup user agent
            final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();

            // Setup target handler
            final String mime = org.apache.xmlgraphics.util.MimeConstants.MIME_PDF;
            final IFDocumentHandler targetHandler = this.fopFactory
                    .getRendererFactory()
                    .createDocumentHandler(userAgent, mime);

            // Setup fonts
            IFUtil.setupFonts(targetHandler);
            targetHandler.setResult(new StreamResult(pdffile));

            final IFConcatenator concatenator = new IFConcatenator(
                    targetHandler, null);

            // Iterate over all intermediate files
            for (final File file : files) {
                final Source src = new StreamSource(file);
                concatenator.appendDocument(src);
            }

            // Signal the end of the processing so the target file can be
            // finalized properly.
            concatenator.finish();
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
            final File[] files = new File[] { new File(outDir, "team1.if.xml"),
                    new File(outDir, "team2.if.xml") };
            final File pdffile = new File(outDir, "ResultIFConcat.pdf");
            for (int i = 0; i < files.length; i++) {
                log.info("Intermediate file " + (i + 1) + ": "
                        + files[i].getCanonicalPath());
            }
            log.info("PDF Output File: " + pdffile.getCanonicalPath());
            log.info("");

            final ProjectTeam team1 = ExampleObj2XML.createSampleProjectTeam();
            final ProjectTeam team2 = createAnotherProjectTeam();

            final ExampleConcat app = new ExampleConcat();

            // Create intermediate files
            app.convertToIntermediate(team1.getSourceForProjectTeam(),
                    new StreamSource(xsltfile), files[0]);
            app.convertToIntermediate(team2.getSourceForProjectTeam(),
                    new StreamSource(xsltfile), files[1]);

            // Concatenate the individual intermediate files to one document
            app.concatToPDF(files, pdffile);

            log.info("Success!");

        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

}
