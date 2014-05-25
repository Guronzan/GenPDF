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

/* $Id: ExampleFO2OldStylePrint.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package embedding;

// Java
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

/**
 * This class demonstrates printing an FO file to a PrinterJob instance.
 */
@Slf4j
public class ExampleFO2OldStylePrint {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();

    /**
     * Prints an FO file using an old-style PrinterJob.
     *
     * @param fo
     *            the FO file
     * @throws IOException
     *             In case of an I/O problem
     * @throws FOPException
     *             In case of a FOP problem
     */
    public void printFO(final File fo) throws IOException, FOPException {

        // Set up PrinterJob instance
        final PrinterJob printerJob = PrinterJob.getPrinterJob();
        printerJob.setJobName("FOP Printing Example");

        try {
            // Set up a custom user agent so we can supply our own renderer
            // instance
            final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();
            userAgent.getRendererOptions().put("printerjob", printerJob);

            // Construct FOP with desired output format
            final Fop fop = this.fopFactory.newFop(
                    MimeConstants.MIME_FOP_PRINT, userAgent);

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

        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
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
            log.info("FOP ExampleFO2OldStylePrint\n");
            log.info("Preparing...");

            // Setup directories
            final File baseDir = new File(".");
            final File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output files
            final File fofile = new File(baseDir, "xml/fo/helloworld.fo");

            log.info("Input: XSL-FO (" + fofile + ")");
            log.info("Output: old-style printing using PrinterJob");
            log.info("");
            log.info("Transforming...");

            final ExampleFO2OldStylePrint app = new ExampleFO2OldStylePrint();
            app.printFO(fofile);

            log.info("Success!");
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
