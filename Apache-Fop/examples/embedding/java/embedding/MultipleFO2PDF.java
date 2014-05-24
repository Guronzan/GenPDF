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

/* $Id: MultipleFO2PDF.java 1036809 2010-11-19 11:25:15Z spepping $ */

package embedding;

// Java
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
//JAXP
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

// FOP
import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.apps.PageSequenceResults;

/**
 * This class demonstrates the conversion of multiple FO files to PDF using FOP.
 * The FopFactory is reused. Its configuration is applied to each rendering run.
 * The FOUserAgent and Fop are newly created by the FopFactory for each run. The
 * FOUserAgent can be configured differently for each run.
 */
@Slf4j
public class MultipleFO2PDF {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();

    // JAXP TransformerFactory can be reused, too
    private final TransformerFactory factory = TransformerFactory.newInstance();

    /**
     * Converts an FO file to a PDF file using FOP
     *
     * @param fo
     *            the FO file
     * @param pdf
     *            the target PDF file
     * @throws TransformerException
     *             in case of a transformation problem
     * @throws IOException
     *             in case of an I/O problem
     * @throws FOPException
     *             in case of a FOP problem
     * @return the formatting results of the run
     */
    public FormattingResults convertFO2PDF(final File fo, final File pdf)
            throws TransformerException, IOException, FOPException {

        OutputStream out = null;
        Fop fop;

        try {
            final FOUserAgent foUserAgent = this.fopFactory.newFOUserAgent();
            // configure foUserAgent as desired

            // Setup output stream. Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new FileOutputStream(pdf);
            out = new BufferedOutputStream(out);

            // Construct fop with desired output format and output stream
            fop = this.fopFactory.newFop(
                    org.apache.xmlgraphics.util.MimeConstants.MIME_PDF,
                    foUserAgent, out);

            // Setup JAXP using identity transformer
            final Transformer transformer = this.factory.newTransformer(); // identity
            // transformer

            // Setup input stream
            final Source src = new StreamSource(fo);

            // Resulting SAX events (the generated FO) must be piped through to
            // FOP
            final Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);
        } finally {
            IOUtils.closeQuietly(out);
        }

        return fop.getResults();
    }

    /**
     * Listens on standard in for names of fo files to be transformed to pdf.
     * 'quit' or the null string (for piped input) cause the listener to stop
     * listening.
     */
    public void listen() {

        // Setup directories
        final File baseDir = new File(".");
        final File outDir = new File(baseDir, "out");
        outDir.mkdirs();
        final BufferedReader in = new BufferedReader(
                new java.io.InputStreamReader(System.in));

        while (true) {
            try {
                // Listen for the input file name
                System.out.print("Input XSL-FO file ('quit' to stop): ");
                final String foname = in.readLine();
                if (foname == null) {
                    log.info("Null input, quitting");
                    return;
                }
                foname.trim();
                if (foname.equals("quit")) {
                    log.info("Quitting");
                    return;
                }
                final File fofile = new File(baseDir, foname);
                String pdfname = foname;
                final int p = foname.lastIndexOf('.');
                pdfname = foname.substring(0, p) + ".pdf";
                final File pdffile = new File(outDir, pdfname);

                // transform and render
                System.out.print("Transforming " + fofile + " to PDF file "
                        + pdffile + "...");
                final FormattingResults foResults = convertFO2PDF(fofile,
                        pdffile);
                log.info("done!");

                // Result processing
                final java.util.List pageSequences = foResults
                        .getPageSequences();
                for (final java.util.Iterator it = pageSequences.iterator(); it
                        .hasNext();) {
                    final PageSequenceResults pageSequenceResults = (PageSequenceResults) it
                            .next();
                    log.info("PageSequence "
                            + (String.valueOf(pageSequenceResults.getID())
                                    .length() > 0 ? pageSequenceResults.getID()
                                            : "<no id>") + " generated "
                                            + pageSequenceResults.getPageCount() + " pages.");
                }
                log.info("Generated " + foResults.getPageCount()
                        + " pages in total.");

            } catch (final Exception e) {
                log.info("failure!");
                e.printStackTrace(System.out);
            } finally {
                log.info("");
            }
        }
    }

    /**
     * Main method. Set up the listener.
     *
     * @param args
     *            command-line arguments
     */
    public static void main(final String[] args) {
        log.info("FOP MultipleFO2PDF\n");
        log.info("Preparing...");
        final MultipleFO2PDF m = new MultipleFO2PDF();
        m.listen();
    }

}
