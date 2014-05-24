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

/* $Id: PDFDocumentGraphics2D.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.svg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import org.apache.fop.Version;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontSetup;
import org.apache.fop.pdf.PDFAnnotList;
import org.apache.fop.pdf.PDFColorHandler;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFFilterList;
import org.apache.fop.pdf.PDFNumber;
import org.apache.fop.pdf.PDFPage;
import org.apache.fop.pdf.PDFPaintingState;
import org.apache.fop.pdf.PDFResources;
import org.apache.fop.pdf.PDFStream;
import org.apache.xmlgraphics.image.GraphicsConstants;

/**
 * This class is a wrapper for the {@link PDFGraphics2D} that is used to create
 * a full document around the PDF rendering from {@link PDFGraphics2D}.
 *
 * @see org.apache.fop.svg.PDFGraphics2D
 */
public class PDFDocumentGraphics2D extends PDFGraphics2D {

    private final PDFContext pdfContext;

    private int width;
    private int height;

    // for SVG scaling
    private float svgWidth;
    private float svgHeight;

    /** Normal PDF resolution (72dpi) */
    public static final int NORMAL_PDF_RESOLUTION = 72;
    /**
     * Default device resolution (300dpi is a resonable quality for most
     * purposes)
     */
    public static final int DEFAULT_NATIVE_DPI = GraphicsConstants.DEFAULT_SAMPLE_DPI;

    /**
     * The device resolution may be different from the normal target resolution.
     * See http://issues.apache.org/bugzilla/show_bug.cgi?id=37305
     */
    private float deviceDPI = DEFAULT_NATIVE_DPI;

    /**
     * Initial clipping area, used to restore to original setting when a new
     * page is started.
     */
    protected Shape initialClip;

    /**
     * Initial transformation matrix, used to restore to original setting when a
     * new page is started.
     */
    protected AffineTransform initialTransform;

    /**
     * Create a new PDFDocumentGraphics2D. This is used to create a new pdf
     * document, the height, width and output stream can be setup later. For use
     * by the transcoder which needs font information for the bridge before the
     * document size is known. The resulting document is written to the stream
     * after rendering.
     *
     * @param textAsShapes
     *            set this to true so that text will be rendered using curves
     *            and not the font.
     */
    public PDFDocumentGraphics2D(final boolean textAsShapes) {
        super(textAsShapes);

        this.pdfDoc = new PDFDocument("Apache FOP Version "
                + Version.getVersion() + ": PDFDocumentGraphics2D");
        this.pdfContext = new PDFContext();
        this.colorHandler = new PDFColorHandler(this.pdfDoc.getResources());
    }

    /**
     * Create a new PDFDocumentGraphics2D. This is used to create a new pdf
     * document of the given height and width. The resulting document is written
     * to the stream after rendering.
     *
     * @param textAsShapes
     *            set this to true so that text will be rendered using curves
     *            and not the font.
     * @param stream
     *            the stream that the final document should be written to.
     * @param width
     *            the width of the document (in points)
     * @param height
     *            the height of the document (in points)
     * @throws IOException
     *             an io exception if there is a problem writing to the output
     *             stream
     */
    public PDFDocumentGraphics2D(final boolean textAsShapes,
            final OutputStream stream, final int width, final int height)
            throws IOException {
        this(textAsShapes);
        setupDocument(stream, width, height);
    }

    /**
     * Create a new PDFDocumentGraphics2D. This is used to create a new pdf
     * document. For use by the transcoder which needs font information for the
     * bridge before the document size is known. The resulting document is
     * written to the stream after rendering. This constructor is Avalon-style.
     */
    public PDFDocumentGraphics2D() {
        this(false);
    }

    /**
     * Setup the document.
     * 
     * @param stream
     *            the output stream to write the document
     * @param width
     *            the width of the page
     * @param height
     *            the height of the page
     * @throws IOException
     *             an io exception if there is a problem writing to the output
     *             stream
     */
    public void setupDocument(final OutputStream stream, final int width,
            final int height) throws IOException {
        this.width = width;
        this.height = height;

        this.pdfDoc.outputHeader(stream);
        setOutputStream(stream);
    }

    /**
     * Setup a default FontInfo instance if none has been setup before.
     */
    public void setupDefaultFontInfo() {
        if (this.fontInfo == null) {
            // Default minimal fonts
            final FontInfo fontInfo = new FontInfo();
            final boolean base14Kerning = false;
            FontSetup.setup(fontInfo, base14Kerning);
            setFontInfo(fontInfo);
        }
    }

    /**
     * Set the device resolution for rendering. Will take effect at the start of
     * the next page.
     * 
     * @param deviceDPI
     *            the device resolution (in dpi)
     */
    public void setDeviceDPI(final float deviceDPI) {
        this.deviceDPI = deviceDPI;
    }

    /**
     * @return the device resolution (in dpi) for rendering.
     */
    public float getDeviceDPI() {
        return this.deviceDPI;
    }

    /**
     * Sets the font info for this PDF document.
     * 
     * @param fontInfo
     *            the font info object with all the fonts
     */
    public void setFontInfo(final FontInfo fontInfo) {
        this.fontInfo = fontInfo;
    }

    /**
     * Get the font info for this pdf document.
     * 
     * @return the font information
     */
    public FontInfo getFontInfo() {
        return this.fontInfo;
    }

    /**
     * Get the pdf document created by this class.
     * 
     * @return the pdf document
     */
    public PDFDocument getPDFDocument() {
        return this.pdfDoc;
    }

    /**
     * Return the PDFContext for this instance.
     * 
     * @return the PDFContext
     */
    public PDFContext getPDFContext() {
        return this.pdfContext;
    }

    /**
     * Set the dimensions of the svg document that will be drawn. This is useful
     * if the dimensions of the svg document are different from the pdf document
     * that is to be created. The result is scaled so that the svg fits
     * correctly inside the pdf document.
     * 
     * @param w
     *            the width of the page
     * @param h
     *            the height of the page
     */
    public void setSVGDimension(final float w, final float h) {
        this.svgWidth = w;
        this.svgHeight = h;
    }

    /**
     * Set the background of the pdf document. This is used to set the
     * background for the pdf document Rather than leaving it as the default
     * white.
     * 
     * @param col
     *            the background colour to fill
     */
    public void setBackgroundColor(final Color col) {
        final StringBuffer sb = new StringBuffer();
        sb.append("q\n");
        this.colorHandler.establishColor(sb, col, true);

        sb.append("0 0 ").append(this.width).append(" ").append(this.height)
                .append(" re\n");

        sb.append("f\n");
        sb.append("Q\n");
        this.currentStream.write(sb.toString());
    }

    /**
     * Is called to prepare the PDFDocumentGraphics2D for the next page to be
     * painted. Basically, this closes the current page. A new page is prepared
     * as soon as painting starts.
     */
    public void nextPage() {
        closePage();
    }

    /**
     * Is called to prepare the PDFDocumentGraphics2D for the next page to be
     * painted. Basically, this closes the current page. A new page is prepared
     * as soon as painting starts. This method allows to start the new page (and
     * following pages) with a different page size.
     * 
     * @param width
     *            the width of the new page (in points)
     * @param height
     *            the height of the new page (in points)
     */
    public void nextPage(final int width, final int height) {
        this.width = width;
        this.height = height;
        nextPage();
    }

    /**
     * Closes the current page and adds it to the PDF file.
     */
    protected void closePage() {
        if (!this.pdfContext.isPagePending()) {
            return; // ignore
        }
        this.currentStream.write("Q\n");
        // Finish page
        final PDFStream pdfStream = this.pdfDoc.getFactory().makeStream(
                PDFFilterList.CONTENT_FILTER, false);
        pdfStream.add(getString());
        this.currentStream = null;
        this.pdfDoc.registerObject(pdfStream);
        this.pdfContext.getCurrentPage().setContents(pdfStream);
        final PDFAnnotList annots = this.pdfContext.getCurrentPage()
                .getAnnotations();
        if (annots != null) {
            this.pdfDoc.addObject(annots);
        }
        this.pdfDoc.addObject(this.pdfContext.getCurrentPage());
        this.pdfContext.clearCurrentPage();
    }

    /** {@inheritDoc} */
    @Override
    protected void preparePainting() {
        if (this.pdfContext.isPagePending()) {
            return;
        }
        // Setup default font info if no more font configuration has been done
        // by the user.
        if (!this.textAsShapes && getFontInfo() == null) {
            setupDefaultFontInfo();
        }
        try {
            startPage();
        } catch (final IOException ioe) {
            handleIOException(ioe);
        }
    }

    /**
     * Called to prepare a new page
     * 
     * @throws IOException
     *             if starting the new page fails due to I/O errors.
     */
    protected void startPage() throws IOException {
        if (this.pdfContext.isPagePending()) {
            throw new IllegalStateException(
                    "Close page first before starting another");
        }
        // Start page
        this.paintingState = new PDFPaintingState();
        if (this.initialTransform == null) {
            // Save initial transformation matrix
            this.initialTransform = getTransform();
            this.initialClip = getClip();
        } else {
            // Reset transformation matrix
            setTransform(this.initialTransform);
            setClip(this.initialClip);
        }

        this.currentFontName = "";
        this.currentFontSize = 0;

        if (this.currentStream == null) {
            this.currentStream = new StringWriter();
        }

        final PDFResources pdfResources = this.pdfDoc.getResources();
        final PDFPage page = this.pdfDoc.getFactory().makePage(pdfResources,
                this.width, this.height);
        this.resourceContext = page;
        this.pdfContext.setCurrentPage(page);
        this.pageRef = page.referencePDF();

        this.currentStream.write("q\n");
        final AffineTransform at = new AffineTransform(1.0, 0.0, 0.0, -1.0,
                0.0, this.height);
        this.currentStream.write("1 0 0 -1 0 " + this.height + " cm\n");
        if (this.svgWidth != 0) {
            final double scaleX = this.width / this.svgWidth;
            final double scaleY = this.height / this.svgHeight;
            at.scale(scaleX, scaleY);
            this.currentStream.write("" + PDFNumber.doubleOut(scaleX) + " 0 0 "
                    + PDFNumber.doubleOut(scaleY) + " 0 0 cm\n");
        }
        if (this.deviceDPI != NORMAL_PDF_RESOLUTION) {
            final double s = NORMAL_PDF_RESOLUTION / this.deviceDPI;
            at.scale(s, s);
            this.currentStream.write("" + PDFNumber.doubleOut(s) + " 0 0 "
                    + PDFNumber.doubleOut(s) + " 0 0 cm\n");

            scale(1 / s, 1 / s);
        }
        // Remember the transform we installed.
        this.paintingState.concatenate(at);

        this.pdfContext.increasePageCount();
    }

    /**
     * The rendering process has finished. This should be called after the
     * rendering has completed as there is no other indication it is complete.
     * This will then write the results to the output stream.
     * 
     * @throws IOException
     *             an io exception if there is a problem writing to the output
     *             stream
     */
    public void finish() throws IOException {
        // restorePDFState();

        closePage();
        if (this.fontInfo != null) {
            this.pdfDoc.getResources().addFonts(this.pdfDoc, this.fontInfo);
        }
        this.pdfDoc.output(this.outputStream);
        this.pdfDoc.outputTrailer(this.outputStream);

        this.outputStream.flush();
    }

    /**
     * This constructor supports the create method
     * 
     * @param g
     *            the pdf document graphics to make a copy of
     */
    public PDFDocumentGraphics2D(final PDFDocumentGraphics2D g) {
        super(g);
        this.pdfContext = g.pdfContext;
        this.width = g.width;
        this.height = g.height;
        this.svgWidth = g.svgWidth;
        this.svgHeight = g.svgHeight;
    }

    /**
     * Creates a new <code>Graphics</code> object that is a copy of this
     * <code>Graphics</code> object.
     * 
     * @return a new graphics context that is a copy of this graphics context.
     */
    @Override
    public Graphics create() {
        preparePainting();
        return new PDFDocumentGraphics2D(this);
    }

    /**
     * Draw a string to the pdf document. This either draws the string directly
     * or if drawing text as shapes it converts the string into shapes and draws
     * that.
     * 
     * @param s
     *            the string to draw
     * @param x
     *            the x position
     * @param y
     *            the y position
     */
    @Override
    public void drawString(final String s, final float x, final float y) {
        if (super.textAsShapes) {
            final Font font = super.getFont();
            final FontRenderContext frc = super.getFontRenderContext();
            final GlyphVector gv = font.createGlyphVector(frc, s);
            final Shape glyphOutline = gv.getOutline(x, y);
            super.fill(glyphOutline);
        } else {
            super.drawString(s, x, y);
        }
    }

}
