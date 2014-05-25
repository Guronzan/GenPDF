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

/* $Id: ExampleConcat.java 1237582 2012-01-30 09:49:22Z mehdi $ */

package embedding.atxml;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.area.AreaTreeModel;
import org.apache.fop.area.AreaTreeParser;
import org.apache.fop.area.RenderPagesModel;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.xml.XMLRenderer;
import org.xml.sax.SAXException;

import embedding.ExampleObj2XML;
import embedding.model.ProjectMember;
import embedding.model.ProjectTeam;

/**
 * Example for the area tree XML format that demonstrates the concatenation of
 * two documents rendered to the area tree XML format. A single PDF file is
 * generated from the two area tree files.
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
     * Converts an XSL-FO document to an area tree XML file.
     *
     * @param src
     *            the source file
     * @param xslt
     *            the stylesheet file
     * @param areaTreeFile
     *            the target area tree XML file
     * @throws IOException
     *             In case of an I/O problem
     * @throws FOPException
     *             In case of a FOP problem
     * @throws TransformerException
     *             In case of a XSL transformation problem
     */
    public void convertToAreaTreeXML(final Source src, final Source xslt,
            final File areaTreeFile) throws IOException, FOPException,
            TransformerException {

        // Create a user agent
        final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();

        // Create an instance of the target renderer so the XMLRenderer can use
        // its font setup
        final Renderer targetRenderer = userAgent.getRendererFactory()
                .createRenderer(userAgent,
                        org.apache.xmlgraphics.util.MimeConstants.MIME_PDF);

        // Create the XMLRenderer to create the area tree XML
        final XMLRenderer xmlRenderer = new XMLRenderer(userAgent);

        // Tell the XMLRenderer to mimic the target renderer
        xmlRenderer.mimicRenderer(targetRenderer);

        // Make sure the prepared XMLRenderer is used
        userAgent.setRendererOverride(xmlRenderer);

        // Setup output
        OutputStream out = new java.io.FileOutputStream(areaTreeFile);
        out = new java.io.BufferedOutputStream(out);
        try {
            // Construct fop (the MIME type here is unimportant due to the
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
     * Concatenates an array of area tree XML files to a single PDF file.
     *
     * @param files
     *            the array of area tree XML files
     * @param pdffile
     *            the target PDF file
     * @throws IOException
     *             In case of an I/O problem
     * @throws TransformerException
     *             In case of a XSL transformation problem
     * @throws SAXException
     *             In case of an XML-related problem
     */
    public void concatToPDF(final File[] files, final File pdffile)
            throws IOException, TransformerException, SAXException {
        // Setup output
        OutputStream out = new java.io.FileOutputStream(pdffile);
        out = new java.io.BufferedOutputStream(out);
        try {
            // Setup fonts and user agent
            final FontInfo fontInfo = new FontInfo();
            final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();

            // Construct the AreaTreeModel that will received the individual
            // pages
            final AreaTreeModel treeModel = new RenderPagesModel(userAgent,
                    org.apache.xmlgraphics.util.MimeConstants.MIME_PDF,
                    fontInfo, out);

            // Iterate over all area tree files
            final AreaTreeParser parser = new AreaTreeParser();
            for (final File file : files) {
                final Source src = new StreamSource(file);
                parser.parse(src, treeModel, userAgent);
            }

            // Signal the end of the processing. The renderer can finalize the
            // target document.
            treeModel.endDocument();
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
            log.info("FOP ExampleConcat\n");

            // Setup directories
            final File baseDir = new File(".");
            final File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup output file
            final File xsltfile = new File(baseDir,
                    "xml/xslt/projectteam2fo.xsl");
            final File[] files = new File[] { new File(outDir, "team1.at.xml"),
                    new File(outDir, "team2.at.xml") };
            final File pdffile = new File(outDir, "ResultConcat.pdf");
            for (int i = 0; i < files.length; i++) {
                log.info("Area Tree XML file " + (i + 1) + ": "
                        + files[i].getCanonicalPath());
            }
            log.info("PDF Output File: " + pdffile.getCanonicalPath());
            log.info("");

            final ProjectTeam team1 = ExampleObj2XML.createSampleProjectTeam();
            final ProjectTeam team2 = createAnotherProjectTeam();

            final ExampleConcat app = new ExampleConcat();

            // Create area tree XML files
            app.convertToAreaTreeXML(team1.getSourceForProjectTeam(),
                    new StreamSource(xsltfile), files[0]);
            app.convertToAreaTreeXML(team2.getSourceForProjectTeam(),
                    new StreamSource(xsltfile), files[1]);

            // Concatenate the individual area tree files to one document
            app.concatToPDF(files, pdffile);

            log.info("Success!");

        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

}
