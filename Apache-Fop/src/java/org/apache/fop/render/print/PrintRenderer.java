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

/* $Id: PrintRenderer.java 1237610 2012-01-30 11:46:13Z mehdi $ */

package org.apache.fop.render.print;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOUserAgent;

/**
 * Renderer that prints through java.awt.PrintJob. The actual printing is
 * handled by Java2DRenderer since both PrintRenderer and AWTRenderer need to
 * support printing.
 */
@Slf4j
public class PrintRenderer extends PageableRenderer {

    /**
     * Printing parameter: the preconfigured PrinterJob to use, datatype:
     * java.awt.print.PrinterJob
     */
    public static final String PRINTER_JOB = "printerjob";

    /**
     * Printing parameter: the number of copies of the document to be printed,
     * datatype: a positive Integer
     */
    public static final String COPIES = "copies";

    private int copies = 1;

    private PrinterJob printerJob;

    /**
     * Creates a new PrintRenderer with the options set through the renderer
     * options if a custom PrinterJob is not given in FOUserAgent's renderer
     * options.
     *
     * @param userAgent
     *            the user agent that contains configuration details. This
     *            cannot be null.
     */
    public PrintRenderer(final FOUserAgent userAgent) {
        super(userAgent);
        setRendererOptions();
    }

    private void initializePrinterJob() {
        if (this.printerJob == null) {
            this.printerJob = PrinterJob.getPrinterJob();
            this.printerJob.setJobName("FOP Document");
            this.printerJob.setCopies(this.copies);
            if (System.getProperty("dialog") != null) {
                if (!this.printerJob.printDialog()) {
                    throw new RuntimeException("Printing cancelled by operator");
                }
            }
            this.printerJob.setPageable(this);
        }
    }

    private void setRendererOptions() {
        final Map rendererOptions = getUserAgent().getRendererOptions();

        final Object printerJobO = rendererOptions
                .get(PrintRenderer.PRINTER_JOB);
        if (printerJobO != null) {
            if (!(printerJobO instanceof PrinterJob)) {
                throw new IllegalArgumentException(
                        "Renderer option "
                                + PrintRenderer.PRINTER_JOB
                                + " must be an instance of java.awt.print.PrinterJob, but an instance of "
                                + printerJobO.getClass().getName()
                                + " was given.");
            }
            this.printerJob = (PrinterJob) printerJobO;
            this.printerJob.setPageable(this);
        }
        final Object o = rendererOptions.get(PrintRenderer.COPIES);
        if (o != null) {
            this.copies = getPositiveInteger(o);
        }
        initializePrinterJob();
    }

    /** @return the PrinterJob instance that this renderer prints to */
    public PrinterJob getPrinterJob() {
        return this.printerJob;
    }

    /** @return the ending page number */
    public int getEndNumber() {
        return this.endNumber;
    }

    /**
     * Sets the number of the last page to be printed.
     *
     * @param end
     *            The ending page number
     */
    public void setEndPage(final int end) {
        this.endNumber = end;
    }

    /** @return the starting page number */
    public int getStartPage() {
        return this.startNumber;
    }

    /**
     * Sets the number of the first page to be printed.
     *
     * @param start
     *            The starting page number
     */
    public void setStartPage(final int start) {
        this.startNumber = start;
    }

    /** {@inheritDoc} */
    @Override
    public void stopRenderer() throws IOException {
        super.stopRenderer();

        try {
            this.printerJob.print();
        } catch (final PrinterException e) {
            log.error(e.getMessage(), e);
            throw new IOException("Unable to print: " + e.getClass().getName()
                    + ": " + e.getMessage());
        }
        clearViewportList();
    }

}
