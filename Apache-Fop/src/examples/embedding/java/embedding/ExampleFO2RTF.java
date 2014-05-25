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

/* $Id: ExampleFO2RTF.java 679326 2008-07-24 09:35:34Z vhennebert $ */

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

/**
 * This class demonstrates the conversion of an FO file to RTF using FOP.
 * <p>
 * Please note that this is practically the same as the ExampleFO2PDF example.
 * Only the MIME parameter to the newFop() method is different!
 */
@Slf4j
public class ExampleFO2RTF {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();

    /**
     * Converts an FO file to a RTF file using FOP
     *
     * @param fo
     *            the FO file
     * @param rtf
     *            the target RTF file
     * @throws IOException
     *             In case of an I/O problem
     * @throws FOPException
     *             In case of a FOP problem
     */
    public void convertFO2RTF(final File fo, final File rtf)
            throws IOException, FOPException {

        final FOUserAgent foUserAgent = this.fopFactory.newFOUserAgent();
        // configure foUserAgent as desired

        OutputStream out = null;

        try {
            // Setup output stream. Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new FileOutputStream(rtf);
            out = new BufferedOutputStream(out);

            // Construct fop with desired output format
            final Fop fop = this.fopFactory.newFop(
                    org.apache.xmlgraphics.util.MimeConstants.MIME_RTF,
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

            // Please note: getResults() won't work for RTF and other flow
            // formats (like MIF)
            // as the layout engine is not involved in the conversion. The
            // page-breaking
            // is done by the application opening the generated file (like MS
            // Word).
            // FormattingResults foResults = fop.getResults();

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
            log.info("FOP ExampleFO2RTF\n");
            log.info("Preparing...");

            // Setup directories
            final File baseDir = new File(".");
            final File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output files
            final File fofile = new File(baseDir, "xml/fo/helloworld.fo");
            final File rtffile = new File(outDir, "ResultFO2RTF.rtf");

            log.info("Input: XSL-FO (" + fofile + ")");
            log.info("Output: PDF (" + rtffile + ")");
            log.info("");
            log.info("Transforming...");

            final ExampleFO2RTF app = new ExampleFO2RTF();
            app.convertFO2RTF(fofile, rtffile);

            log.info("Success!");
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
