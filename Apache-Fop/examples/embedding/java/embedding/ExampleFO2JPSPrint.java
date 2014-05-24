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

/* $Id: ExampleFO2JPSPrint.java 1237582 2012-01-30 09:49:22Z mehdi $ */

package embedding;

// Java
import java.io.File;
import java.io.IOException;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUI;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
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
import org.apache.fop.render.print.PageableRenderer;

/**
 * This class demonstrates printing an FO file using JPS (Java Printing System).
 */
@Slf4j
public class ExampleFO2JPSPrint {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();

    private DocPrintJob createDocPrintJob() {
        final PrintService[] services = PrintServiceLookup.lookupPrintServices(
                DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
        final PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        final PrintService printService = ServiceUI.printDialog(null, 50, 50,
                services, services[0], null, attributes);
        if (printService != null) {
            return printService.createPrintJob();
        } else {
            return null;
        }
    }

    /**
     * Prints an FO file using JPS.
     *
     * @param fo
     *            the FO file
     * @throws IOException
     *             In case of an I/O problem
     * @throws FOPException
     *             In case of a FOP problem
     * @throws TransformerException
     *             In case of a problem during XSLT processing
     * @throws PrintException
     *             If an error occurs while printing
     */
    public void printFO(final File fo) throws IOException, FOPException,
    TransformerException, PrintException {

        // Set up DocPrintJob instance
        final DocPrintJob printJob = createDocPrintJob();

        // Set up a custom user agent so we can supply our own renderer instance
        final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();

        final PageableRenderer renderer = new PageableRenderer(userAgent);
        userAgent.setRendererOverride(renderer);

        // Construct FOP with desired output format
        final Fop fop = this.fopFactory.newFop(userAgent);

        // Setup JAXP using identity transformer
        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer transformer = factory.newTransformer(); // identity
        // transformer

        // Setup input stream
        final Source src = new StreamSource(fo);

        // Resulting SAX events (the generated FO) must be piped through to FOP
        final Result res = new SAXResult(fop.getDefaultHandler());

        // Start XSLT transformation and FOP processing
        transformer.transform(src, res);

        final Doc doc = new SimpleDoc(renderer,
                DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
        printJob.print(doc, null);
    }

    /**
     * Main method.
     *
     * @param args
     *            command-line arguments
     */
    public static void main(final String[] args) {
        try {
            log.info("FOP ExampleFO2JPSPrint\n");
            log.info("Preparing...");

            // Setup directories
            final File baseDir = new File(".");
            final File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output files
            final File fofile = new File(baseDir, "xml/fo/helloworld.fo");

            log.info("Input: XSL-FO (" + fofile + ")");
            log.info("Output: JPS (Java Printing System)");
            log.info("");
            log.info("Transforming...");

            final ExampleFO2JPSPrint app = new ExampleFO2JPSPrint();
            app.printFO(fofile);

            log.info("Success!");
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
