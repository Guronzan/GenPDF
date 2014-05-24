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

/* $Id: ExampleEvents.java 932497 2010-04-09 16:34:29Z vhennebert $ */

package embedding.events;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;
import org.xml.sax.SAXException;

/**
 * This class demonstrates how to register an event listener with FOP so you can
 * customize FOP's error behaviour.
 */
@Slf4j
public class ExampleEvents {

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
     * @throws TransformerException
     *             In case of a problem with XSLT
     */
    public void convertFO2PDF(final URL fo, final File pdf) throws IOException,
    FOPException, TransformerException {

        OutputStream out = null;

        try {
            // Create the user agent for this processing run
            final FOUserAgent foUserAgent = this.fopFactory.newFOUserAgent();

            // Adding a simple logging listener that writes to stdout and stderr
            foUserAgent.getEventBroadcaster().addEventListener(
                    new SysOutEventListener());

            // Add your own event listener
            foUserAgent.getEventBroadcaster().addEventListener(
                    new MyEventListener());

            // configure foUserAgent further as desired

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
            final Source src = new StreamSource(fo.toExternalForm());

            // Resulting SAX events (the generated FO) must be piped through to
            // FOP
            final Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);

        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private static class MyEventListener implements EventListener {

        @Override
        public void processEvent(final Event event) {
            if ("org.apache.fop.ResourceEventProducer.imageNotFound"
                    .equals(event.getEventID())) {

                // Get the FileNotFoundException that's part of the event's
                // parameters
                final FileNotFoundException fnfe = (FileNotFoundException) event
                        .getParam("fnfe");

                log.info("---=== imageNotFound Event for "
                        + event.getParam("uri") + "!!! ===---");
                // Stop processing when an image could not be found. Otherwise,
                // FOP would just
                // continue without the image!

                log.info("Throwing a RuntimeException...");
                throw new RuntimeException(EventFormatter.format(event), fnfe);
            } else {
                // ignore all other events
            }
        }

    }

    /** A simple event listener that writes the events to stdout and sterr. */
    private static class SysOutEventListener implements EventListener {

        /** {@inheritDoc} */
        @Override
        public void processEvent(final Event event) {
            final String msg = EventFormatter.format(event);
            final EventSeverity severity = event.getSeverity();
            if (severity == EventSeverity.INFO) {
                log.info("[INFO ] " + msg);
            } else if (severity == EventSeverity.WARN) {
                log.info("[WARN ] " + msg);
            } else if (severity == EventSeverity.ERROR) {
                System.err.println("[ERROR] " + msg);
            } else if (severity == EventSeverity.FATAL) {
                System.err.println("[FATAL] " + msg);
            } else {
                assert false;
            }
        }
    }

    /**
     * This method extracts the original exception from some exception. The
     * exception might be nested multiple levels deep.
     *
     * @param t
     *            the Throwable to inspect
     * @return the original Throwable or the method parameter t if there are no
     *         nested Throwables.
     */
    private static Throwable getOriginalThrowable(final Throwable t) {
        if (t instanceof SAXException) {
            final SAXException saxe = (SAXException) t;
            if (saxe.getException() != null) {
                return getOriginalThrowable(saxe.getException());
            } else {
                return saxe;
            }
        } else {
            if (t.getCause() != null) {
                return getOriginalThrowable(t.getCause());
            } else {
                return t;
            }
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
            log.info("FOP ExampleEvents\n");
            log.info("Preparing...");

            // Setup directories
            final File baseDir = new File(".");
            final File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output files
            final URL fo = ExampleEvents.class.getResource("missing-image.fo");
            final File pdffile = new File(outDir, "out.pdf");

            log.info("Input: XSL-FO (" + fo.toExternalForm() + ")");
            log.info("Output: PDF (" + pdffile + ")");
            log.info("");
            log.info("Transforming...");

            final ExampleEvents app = new ExampleEvents();

            try {
                app.convertFO2PDF(fo, pdffile);
            } catch (final TransformerException te) {
                // Note: We don't get the original exception here!
                // FOP needs to embed the exception in a SAXException and the
                // TraX transformer
                // again wraps the SAXException in a TransformerException. Even
                // our own
                // RuntimeException just wraps the original
                // FileNotFoundException.
                // So we need to unpack to get the original exception (about
                // three layers deep).
                final Throwable originalThrowable = getOriginalThrowable(te);
                originalThrowable.printStackTrace(System.err);
                log.info("Aborted!");
                System.exit(-1);
            }

            log.info("Success!");
        } catch (final Exception e) {
            // Some other error (shouldn't happen in this example)
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

}
