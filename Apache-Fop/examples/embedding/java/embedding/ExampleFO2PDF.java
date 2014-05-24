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

/* $Id: ExampleFO2PDF.java 1036809 2010-11-19 11:25:15Z spepping $ */

package embedding;

// Java
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
//JAXP
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
// FOP
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.apps.PageSequenceResults;

/**
 * This class demonstrates the conversion of an FO file to PDF using FOP.
 */
@Slf4j
public class ExampleFO2PDF {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();

    /**
     * Converts an FO file to a PDF file using FOP
     *
     * @param fo
     *            the FO file
     * @param pdf
     *            the target PDF file
     * @throws IOException
     *             In case of an I/O problem
     * @throws FOPException
     *             In case of a FOP problem
     */
    public void convertFO2PDF(final File fo, final File pdf)
            throws IOException, FOPException {

        OutputStream out = null;

        try {
            final FOUserAgent foUserAgent = this.fopFactory.newFOUserAgent();
            // configure foUserAgent as desired

            // Setup output stream. Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new FileOutputStream(pdf);
            out = new BufferedOutputStream(out);

            // Construct fop with desired output format
            final Fop fop = this.fopFactory.newFop(
                    org.apache.xmlgraphics.util.MimeConstants.MIME_PDF,
                    foUserAgent, out);

            // Setup JAXP using identity transformer
            final TransformerFactory factory = TransformerFactory.newInstance();
            final Transformer transformer = factory.newTransformer(); // identity
            // transformer

            // Setup input stream
            final Source src = new StreamSource(fo);

            // Resulting SAX events (the generated FO) must be piped through to
            // FOP
            final Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);

            // Result processing
            final FormattingResults foResults = fop.getResults();
            final java.util.List pageSequences = foResults.getPageSequences();
            for (final java.util.Iterator it = pageSequences.iterator(); it
                    .hasNext();) {
                final PageSequenceResults pageSequenceResults = (PageSequenceResults) it
                        .next();
                log.info("PageSequence "
                        + (String.valueOf(pageSequenceResults.getID()).length() > 0 ? pageSequenceResults
                                .getID() : "<no id>") + " generated "
                                + pageSequenceResults.getPageCount() + " pages.");
            }
            log.info("Generated " + foResults.getPageCount()
                    + " pages in total.");

        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
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
            log.info("FOP ExampleFO2PDF\n");
            log.info("Preparing...");

            // Setup directories
            final File baseDir = new File(".");
            final File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output files
            final File fofile = new File(baseDir, "xml/fo/helloworld.fo");
            // File fofile = new File(baseDir,
            // "../fo/pagination/franklin_2pageseqs.fo");
            final File pdffile = new File(outDir, "ResultFO2PDF.pdf");

            log.info("Input: XSL-FO (" + fofile + ")");
            log.info("Output: PDF (" + pdffile + ")");
            log.info("");
            log.info("Transforming...");

            final ExampleFO2PDF app = new ExampleFO2PDF();
            app.convertFO2PDF(fofile, pdffile);

            log.info("Success!");
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
