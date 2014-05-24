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

/* $Id: ExampleXML2PDF.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package embedding;

//Java
import java.io.File;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
//JAXP
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

//FOP
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;

/**
 * This class demonstrates the conversion of an XML file to PDF using JAXP
 * (XSLT) and FOP (XSL-FO).
 */
@Slf4j
public class ExampleXML2PDF {

    /**
     * Main method.
     *
     * @param args
     *            command-line arguments
     */
    public static void main(final String[] args) {
        try {
            log.info("FOP ExampleXML2PDF\n");
            log.info("Preparing...");

            // Setup directories
            final File baseDir = new File(".");
            final File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output files
            final File xmlfile = new File(baseDir, "xml/xml/projectteam.xml");
            final File xsltfile = new File(baseDir,
                    "xml/xslt/projectteam2fo.xsl");
            final File pdffile = new File(outDir, "ResultXML2PDF.pdf");

            log.info("Input: XML (" + xmlfile + ")");
            log.info("Stylesheet: " + xsltfile);
            log.info("Output: PDF (" + pdffile + ")");
            log.info("");
            log.info("Transforming...");

            // configure fopFactory as desired
            final FopFactory fopFactory = FopFactory.newInstance();

            final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            // configure foUserAgent as desired

            // Setup output
            OutputStream out = new java.io.FileOutputStream(pdffile);
            out = new java.io.BufferedOutputStream(out);

            try {
                // Construct fop with desired output format
                final Fop fop = fopFactory.newFop(
                        org.apache.xmlgraphics.util.MimeConstants.MIME_PDF,
                        foUserAgent, out);

                // Setup XSLT
                final TransformerFactory factory = TransformerFactory
                        .newInstance();
                final Transformer transformer = factory
                        .newTransformer(new StreamSource(xsltfile));

                // Set the value of a <param> in the stylesheet
                transformer.setParameter("versionParam", "2.0");

                // Setup input for XSLT transformation
                final Source src = new StreamSource(xmlfile);

                // Resulting SAX events (the generated FO) must be piped through
                // to FOP
                final Result res = new SAXResult(fop.getDefaultHandler());

                // Start XSLT transformation and FOP processing
                transformer.transform(src, res);
            } finally {
                out.close();
            }

            log.info("Success!");
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
