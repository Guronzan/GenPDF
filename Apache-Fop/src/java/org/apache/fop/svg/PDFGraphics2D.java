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

/* $Id: PDFGraphics2D.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.svg;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.RenderableImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.apache.batik.ext.awt.LinearGradientPaint;
import org.apache.batik.ext.awt.MultipleGradientPaint;
import org.apache.batik.ext.awt.RadialGradientPaint;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.PatternPaint;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontSetup;
import org.apache.fop.pdf.BitmapImage;
import org.apache.fop.pdf.PDFAnnotList;
import org.apache.fop.pdf.PDFColor;
import org.apache.fop.pdf.PDFColorHandler;
import org.apache.fop.pdf.PDFConformanceException;
import org.apache.fop.pdf.PDFDeviceColorSpace;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFGState;
import org.apache.fop.pdf.PDFImage;
import org.apache.fop.pdf.PDFImageXObject;
import org.apache.fop.pdf.PDFLink;
import org.apache.fop.pdf.PDFNumber;
import org.apache.fop.pdf.PDFPaintingState;
import org.apache.fop.pdf.PDFPattern;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFResources;
import org.apache.fop.pdf.PDFText;
import org.apache.fop.pdf.PDFXObject;
import org.apache.fop.render.pdf.ImageRawCCITTFaxAdapter;
import org.apache.fop.render.pdf.ImageRawJPEGAdapter;
import org.apache.fop.render.pdf.ImageRenderedAdapter;
import org.apache.xmlgraphics.image.GraphicsConstants;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.ImageRawCCITTFax;
import org.apache.xmlgraphics.image.loader.impl.ImageRawJPEG;
import org.apache.xmlgraphics.image.loader.impl.ImageRendered;
import org.apache.xmlgraphics.java2d.AbstractGraphics2D;
import org.apache.xmlgraphics.java2d.GraphicContext;

/**
 * <p>
 * PDF Graphics 2D. Used for drawing into a pdf document as if it is a graphics
 * object. This takes a pdf document and draws into it.
 * </p>
 *
 * <p>
 * This work was authored by Keiron Liddle (keiron@aftexsw.com).
 * </p>
 *
 * @see org.apache.batik.ext.awt.g2d.AbstractGraphics2D
 */
public class PDFGraphics2D extends AbstractGraphics2D implements
NativeImageHandler {
    private static final AffineTransform IDENTITY_TRANSFORM = new AffineTransform();

    /** The number of decimal places. */
    private static final int DEC = 8;

    /** Convenience constant for full opacity */
    static final int OPAQUE = 255;

    /**
     * the PDF Document being created
     */
    protected PDFDocument pdfDoc;

    /**
     * The current resource context for adding fonts, patterns etc.
     */
    protected PDFResourceContext resourceContext;

    /**
     * The PDF reference of the current page.
     */
    protected String pageRef;

    /**
     * The PDF painting state
     */
    protected PDFPaintingState paintingState;

    /** the PDF color handler */
    protected PDFColorHandler colorHandler;

    /**
     * The PDF graphics state level that this svg is being drawn into.
     */
    protected int baseLevel = 0;

    /**
     * The count of natively handled images added to document so they receive
     * unique keys.
     */
    protected int nativeCount = 0;

    /**
     * The current font information.
     */
    protected FontInfo fontInfo;

    /**
     * The override font state used when drawing text and the font cannot be set
     * using java fonts.
     */
    protected Font ovFontState = null;

    /**
     * the current stream to add PDF commands to
     */
    protected StringWriter currentStream = new StringWriter();

    /**
     * the current (internal) font name
     */
    protected String currentFontName;

    /**
     * the current font size in millipoints
     */
    protected float currentFontSize;

    /**
     * The output stream for the pdf document. If this is set then it can
     * progressively output the pdf document objects to reduce memory.
     * Especially with images.
     */
    protected OutputStream outputStream = null;

    /**
     * Create a new PDFGraphics2D with the given pdf document info. This is used
     * to create a Graphics object for use inside an already existing document.
     *
     * @param textAsShapes
     *            if true then draw text as shapes
     * @param fi
     *            the current font information
     * @param doc
     *            the pdf document for creating pdf objects
     * @param page
     *            the current resource context or page
     * @param pref
     *            the PDF reference of the current page
     * @param font
     *            the current font name
     * @param size
     *            the current font size
     */
    public PDFGraphics2D(final boolean textAsShapes, final FontInfo fi,
            final PDFDocument doc, final PDFResourceContext page,
            final String pref, final String font, final float size) {
        this(textAsShapes);
        this.pdfDoc = doc;
        this.colorHandler = new PDFColorHandler(doc.getResources());
        this.resourceContext = page;
        this.currentFontName = font;
        this.currentFontSize = size;
        this.fontInfo = fi;
        this.pageRef = pref;
        this.paintingState = new PDFPaintingState();
    }

    /**
     * Create a new PDFGraphics2D.
     *
     * @param textAsShapes
     *            true if drawing text as shapes
     */
    protected PDFGraphics2D(final boolean textAsShapes) {
        super(textAsShapes);
    }

    /**
     * This constructor supports the create method. This is not implemented
     * properly.
     *
     * @param g
     *            the PDF graphics to make a copy of
     */
    public PDFGraphics2D(final PDFGraphics2D g) {
        super(g);
        this.pdfDoc = g.pdfDoc;
        this.colorHandler = g.colorHandler;
        this.resourceContext = g.resourceContext;
        this.currentFontName = g.currentFontName;
        this.currentFontSize = g.currentFontSize;
        this.fontInfo = g.fontInfo;
        this.pageRef = g.pageRef;
        this.paintingState = g.paintingState;
        this.currentStream = g.currentStream;
        this.nativeCount = g.nativeCount;
        this.outputStream = g.outputStream;
        this.ovFontState = g.ovFontState;
    }

    /**
     * Creates a new <code>Graphics</code> object that is a copy of this
     * <code>Graphics</code> object.
     *
     * @return a new graphics context that is a copy of this graphics context.
     */
    @Override
    public Graphics create() {
        return new PDFGraphics2D(this);
    }

    /**
     * Central handler for IOExceptions for this class.
     *
     * @param ioe
     *            IOException to handle
     */
    protected void handleIOException(final IOException ioe) {
        // TODO Surely, there's a better way to do this.
        ioe.printStackTrace();
    }

    /**
     * This method is used by PDFDocumentGraphics2D to prepare a new page if
     * necessary.
     */
    protected void preparePainting() {
        // nop, used by PDFDocumentGraphics2D
    }

    /**
     * Set the PDF state to use when starting to draw into the PDF graphics.
     *
     * @param state
     *            the PDF state
     */
    public void setPaintingState(final PDFPaintingState state) {
        this.paintingState = state;
        this.baseLevel = this.paintingState.getStackLevel();
    }

    /**
     * Set the output stream that this PDF document is being drawn to. This is
     * so that it can progressively use the PDF document to output data such as
     * images. This results in a significant saving on memory.
     *
     * @param os
     *            the output stream that is being used for the PDF document
     */
    public void setOutputStream(final OutputStream os) {
        this.outputStream = os;
    }

    /**
     * Get the string containing all the commands written into this Graphics.
     *
     * @return the string containing the PDF markup
     */
    public String getString() {
        return this.currentStream.toString();
    }

    /**
     * Get the string buffer from the currentStream, containing all the commands
     * written into this Graphics so far.
     *
     * @return the StringBuilder containing the PDF markup
     */
    public StringBuffer getBuffer() {
        return this.currentStream.getBuffer();
    }

    /**
     * Gets the PDF reference of the current page.
     *
     * @return the PDF reference of the current page
     */
    public String getPageReference() {
        return this.pageRef;
    }

    /**
     * Set the Graphics context.
     *
     * @param c
     *            the graphics context to use
     */
    public void setGraphicContext(final GraphicContext c) {
        this.gc = c;
        setPrivateHints();
    }

    private void setPrivateHints() {
        setRenderingHint(RenderingHintsKeyExt.KEY_AVOID_TILE_PAINTING,
                RenderingHintsKeyExt.VALUE_AVOID_TILE_PAINTING_ON);
    }

    /**
     * Set the override font state for drawing text. This is used by the PDF
     * text painter so that it can temporarily set the font state when a java
     * font cannot be used. The next drawString will use this font state.
     *
     * @param infont
     *            the font state to use
     */
    public void setOverrideFontState(final Font infont) {
        this.ovFontState = infont;
    }

    /**
     * Restore the PDF graphics state to the starting state level.
     */
    /*
     * seems not to be used public void restorePDFState() { for (int count =
     * graphicsState.getStackLevel(); count > baseLevel; count--) {
     * currentStream.write("Q\n"); } graphicsState.restoreLevel(baseLevel); }
     */

    private void concatMatrix(final double[] matrix) {
        this.currentStream.write(PDFNumber.doubleOut(matrix[0], DEC) + " "
                + PDFNumber.doubleOut(matrix[1], DEC) + " "
                + PDFNumber.doubleOut(matrix[2], DEC) + " "
                + PDFNumber.doubleOut(matrix[3], DEC) + " "
                + PDFNumber.doubleOut(matrix[4], DEC) + " "
                + PDFNumber.doubleOut(matrix[5], DEC) + " cm\n");
    }

    private void concatMatrix(final AffineTransform transform) {
        if (!transform.isIdentity()) {
            final double[] matrix = new double[6];
            transform.getMatrix(matrix);
            concatMatrix(matrix);
        }
    }

    /**
     * This is mainly used for shading patterns which use the document-global
     * coordinate system instead of the local one.
     *
     * @return the transformation matrix that established the basic user space
     *         for this document
     */
    protected AffineTransform getBaseTransform() {
        final AffineTransform at = new AffineTransform(
                this.paintingState.getTransform());
        return at;
    }

    /**
     * This is a pdf specific method used to add a link to the pdf document.
     *
     * @param bounds
     *            the bounds of the link in user coordinates
     * @param trans
     *            the transform of the current drawing position
     * @param dest
     *            the PDF destination
     * @param linkType
     *            the type of link, internal or external
     */
    public void addLink(final Rectangle2D bounds, final AffineTransform trans,
            final String dest, final int linkType) {
        if (!this.pdfDoc.getProfile().isAnnotationAllowed()) {
            return;
        }
        preparePainting();
        final AffineTransform at = getTransform();
        Shape b = at.createTransformedShape(bounds);
        b = trans.createTransformedShape(b);
        if (b != null) {
            final Rectangle rect = b.getBounds();

            if (linkType != PDFLink.EXTERNAL) {
                final String pdfdest = "/FitR " + dest;
                this.resourceContext.addAnnotation(this.pdfDoc.getFactory()
                        .makeLink(rect, getPageReference(), pdfdest));
            } else {
                this.resourceContext.addAnnotation(this.pdfDoc.getFactory()
                        .makeLink(rect, dest, linkType, 0));
            }
        }
    }

    /**
     * Add a natively handled image directly to the PDF document. This is used
     * by the PDFImageElementBridge to draw a natively handled image (like JPEG
     * or CCITT images) directly into the PDF document rather than converting
     * the image into a bitmap and increasing the size.
     *
     * @param image
     *            the image to draw
     * @param x
     *            the x position
     * @param y
     *            the y position
     * @param width
     *            the width to draw the image
     * @param height
     *            the height to draw the image
     */
    @Override
    public void addNativeImage(
            final org.apache.xmlgraphics.image.loader.Image image,
            final float x, final float y, final float width, final float height) {
        preparePainting();
        String key = image.getInfo().getOriginalURI();
        if (key == null) {
            // Need to include hash code as when invoked from FO you
            // may have several 'independent' PDFGraphics2D so the
            // count is not enough.
            key = "__AddNative_" + hashCode() + "_" + this.nativeCount;
            this.nativeCount++;
        }

        PDFImage pdfImage;
        if (image instanceof ImageRawJPEG) {
            pdfImage = new ImageRawJPEGAdapter((ImageRawJPEG) image, key);
        } else if (image instanceof ImageRawCCITTFax) {
            pdfImage = new ImageRawCCITTFaxAdapter((ImageRawCCITTFax) image,
                    key);
        } else {
            throw new IllegalArgumentException("Unsupported Image subclass: "
                    + image.getClass().getName());
        }

        final PDFXObject xObject = this.pdfDoc.addImage(this.resourceContext,
                pdfImage);
        flushPDFDocument();

        final AffineTransform at = new AffineTransform();
        at.translate(x, y);
        useXObject(xObject, at, width, height);
    }

    private void flushPDFDocument() {
        if (this.outputStream != null) {
            try {
                this.pdfDoc.output(this.outputStream);
            } catch (final IOException ioe) {
                // ignore exception, will be thrown again later
            }
        }
    }

    /**
     * Draws as much of the specified image as is currently available. The image
     * is drawn with its top-left corner at (<i>x</i>,&nbsp;<i>y</i>) in this
     * graphics context's coordinate space. Transparent pixels in the image do
     * not affect whatever pixels are already there.
     * <p>
     * This method returns immediately in all cases, even if the complete image
     * has not yet been loaded, and it has not been dithered and converted for
     * the current output device.
     * <p>
     * If the image has not yet been completely loaded, then
     * <code>drawImage</code> returns <code>false</code>. As more of the image
     * becomes available, the process that draws the image notifies the
     * specified image observer.
     *
     * @param img
     *            the specified image to be drawn.
     * @param x
     *            the <i>x</i> coordinate.
     * @param y
     *            the <i>y</i> coordinate.
     * @param observer
     *            object to be notified as more of the image is converted.
     * @return true if the image was drawn
     * @see java.awt.Image
     * @see java.awt.image.ImageObserver
     * @see java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int,
     *      int, int, int)
     */
    @Override
    public boolean drawImage(final Image img, final int x, final int y,
            final ImageObserver observer) {
        preparePainting();

        final int width = img.getWidth(observer);
        final int height = img.getHeight(observer);

        if (width == -1 || height == -1) {
            return false;
        }

        return drawImage(img, x, y, width, height, observer);
    }

    private BufferedImage buildBufferedImage(final Dimension size) {
        return new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_INT_ARGB);
    }

    /** {@inheritDoc} */
    @Override
    public boolean drawImage(final Image img, final int x, final int y,
            final int width, final int height, final ImageObserver observer) {
        preparePainting();
        // first we look to see if we've already added this image to
        // the pdf document. If so, we just reuse the reference;
        // otherwise we have to build a FopImage and add it to the pdf
        // document
        final String key = "TempImage:" + img.toString();
        PDFXObject xObject = this.pdfDoc.getXObject(key);
        if (xObject == null) {
            // OK, have to build and add a PDF image

            final Dimension size = new Dimension(width, height);
            final BufferedImage buf = buildBufferedImage(size);

            final java.awt.Graphics2D g = buf.createGraphics();
            g.setComposite(AlphaComposite.SrcOver);
            g.setBackground(new Color(1, 1, 1, 0));
            g.setPaint(new Color(1, 1, 1, 0));
            g.fillRect(0, 0, width, height);

            final int imageWidth = buf.getWidth();
            final int imageHeight = buf.getHeight();
            g.clip(new Rectangle(0, 0, imageWidth, imageHeight));
            g.setComposite(this.gc.getComposite());

            final boolean drawn = g.drawImage(img, 0, 0, imageWidth,
                    imageHeight, observer);
            if (!drawn) {
                return false;
            }
            g.dispose();

            xObject = addRenderedImage(key, buf);
        } else {
            this.resourceContext.getPDFResources().addXObject(xObject);
        }

        final AffineTransform at = new AffineTransform();
        at.translate(x, y);
        useXObject(xObject, at, width, height);
        return true;
    }

    /**
     * Disposes of this graphics context and releases any system resources that
     * it is using. A <code>Graphics</code> object cannot be used after
     * <code>dispose</code>has been called.
     * <p>
     * When a Java program runs, a large number of <code>Graphics</code> objects
     * can be created within a short time frame. Although the finalization
     * process of the garbage collector also disposes of the same system
     * resources, it is preferable to manually free the associated resources by
     * calling this method rather than to rely on a finalization process which
     * may not run to completion for a long period of time.
     * <p>
     * Graphics objects which are provided as arguments to the
     * <code>paint</code> and <code>update</code> methods of components are
     * automatically released by the system when those methods return. For
     * efficiency, programmers should call <code>dispose</code> when finished
     * using a <code>Graphics</code> object only if it was created directly from
     * a component or another <code>Graphics</code> object.
     *
     * @see java.awt.Graphics#finalize
     * @see java.awt.Component#paint
     * @see java.awt.Component#update
     * @see java.awt.Component#getGraphics
     * @see java.awt.Graphics#create
     */
    @Override
    public void dispose() {
        this.pdfDoc = null;
        this.fontInfo = null;
        this.currentStream = null;
        this.currentFontName = null;
    }

    /**
     * Strokes the outline of a <code>Shape</code> using the settings of the
     * current <code>Graphics2D</code> context. The rendering attributes applied
     * include the <code>Clip</code>, <code>Transform</code>, <code>Paint</code>
     * , <code>Composite</code> and <code>Stroke</code> attributes.
     *
     * @param s
     *            the <code>Shape</code> to be rendered
     * @see #setStroke
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #clip
     * @see #setClip
     * @see #setComposite
     */
    @Override
    public void draw(final Shape s) {
        preparePainting();

        // Transparency shortcut
        Color c;
        c = getColor();
        if (c.getAlpha() == 0) {
            return;
        }

        final AffineTransform trans = getTransform();
        final double[] tranvals = new double[6];
        trans.getMatrix(tranvals);

        final Shape imclip = getClip();
        final boolean newClip = this.paintingState.checkClip(imclip);
        final boolean newTransform = this.paintingState.checkTransform(trans)
                && !trans.isIdentity();

        if (newClip || newTransform) {
            saveGraphicsState();
            if (newTransform) {
                concatMatrix(tranvals);
            }
            if (newClip) {
                writeClip(imclip);
            }
        }

        applyAlpha(OPAQUE, c.getAlpha());

        c = getColor();
        applyColor(c, false);
        c = getBackground();
        applyColor(c, true);

        final Paint paint = getPaint();
        if (this.paintingState.setPaint(paint)) {
            if (!applyPaint(paint, false)) {
                // Stroke the shape and use it to 'clip'
                // the paint contents.
                final Shape ss = getStroke().createStrokedShape(s);
                applyUnknownPaint(paint, ss);

                if (newClip || newTransform) {
                    restoreGraphicsState();
                }
                return;
            }
        }
        applyStroke(getStroke());

        final PathIterator iter = s.getPathIterator(IDENTITY_TRANSFORM);
        processPathIterator(iter);
        doDrawing(false, true, false);
        if (newClip || newTransform) {
            restoreGraphicsState();
        }
    }

    /*
     * // in theory we could set the clip using these methods // it doesn't seem
     * to improve the file sizes much // and makes everything more complicated
     *
     * Shape lastClip = null;
     *
     * public void clip(Shape cl) { super.clip(cl); Shape newClip = getClip();
     * if (newClip == null || lastClip == null || !(new Area(newClip).equals(new
     * Area(lastClip)))) { graphicsState.setClip(newClip); writeClip(newClip); }
     *
     * lastClip = newClip; }
     *
     * public void setClip(Shape cl) { super.setClip(cl); Shape newClip =
     * getClip(); if (newClip == null || lastClip == null || !(new
     * Area(newClip).equals(new Area(lastClip)))) { for (int count =
     * graphicsState.getStackLevel(); count > baseLevel; count--) {
     * currentStream.write("Q\n"); } graphicsState.restoreLevel(baseLevel);
     * currentStream.write("q\n"); graphicsState.push(); if (newClip != null) {
     * graphicsState.setClip(newClip); } writeClip(newClip); }
     *
     * lastClip = newClip; }
     */

    /**
     * Set the clipping shape for future PDF drawing in the current graphics
     * state. This sets creates and writes a clipping shape that will apply to
     * future drawings in the current graphics state.
     *
     * @param s
     *            the clipping shape
     */
    protected void writeClip(final Shape s) {
        if (s == null) {
            return;
        }
        final PathIterator iter = s.getPathIterator(IDENTITY_TRANSFORM);
        if (iter.isDone()) {
            // no segments available. Not worth doing anything
            return;
        }
        preparePainting();

        processPathIterator(iter);
        // clip area
        this.currentStream.write("W\n");
        this.currentStream.write("n\n");
    }

    /**
     * Apply the java Color to PDF. This converts the java colour to a PDF
     * colour and sets it for the next drawing.
     *
     * @param col
     *            the java colour
     * @param fill
     *            true if the colour will be used for filling
     */
    protected void applyColor(final Color col, final boolean fill) {
        preparePainting();

        // TODO Handle this in PDFColorHandler by automatically converting the
        // color.
        // This won't work properly anyway after the redesign of ColorExt
        if (col.getColorSpace().getType() == ColorSpace.TYPE_CMYK) {
            if (this.pdfDoc.getProfile().getPDFAMode().isPDFA1LevelB()) {
                // See PDF/A-1, ISO 19005:1:2005(E), 6.2.3.3
                // FOP is currently restricted to DeviceRGB if PDF/A-1 is
                // active.
                throw new PDFConformanceException(
                        "PDF/A-1 does not allow mixing DeviceRGB and DeviceCMYK.");
            }
        }

        boolean doWrite = false;
        if (fill) {
            if (this.paintingState.setBackColor(col)) {
                doWrite = true;
            }
        } else {
            if (this.paintingState.setColor(col)) {
                doWrite = true;
            }
        }
        if (doWrite) {
            final StringBuilder sb = new StringBuilder();
            this.colorHandler.establishColor(sb, col, fill);
            this.currentStream.write(sb.toString());
        }
    }

    /**
     * Apply the java paint to the PDF. This takes the java paint sets up the
     * appropraite PDF commands for the drawing with that paint. Currently this
     * supports the gradients and patterns from batik.
     *
     * @param paint
     *            the paint to convert to PDF
     * @param fill
     *            true if the paint should be set for filling
     * @return true if the paint is handled natively, false if the paint should
     *         be rasterized
     */
    protected boolean applyPaint(Paint paint, final boolean fill) { // CSOK:
        // MethodLength
        preparePainting();

        if (paint instanceof Color) {
            return true;
        }
        // convert java.awt.GradientPaint to LinearGradientPaint to avoid
        // rasterization
        if (paint instanceof GradientPaint) {
            final GradientPaint gpaint = (GradientPaint) paint;
            paint = new LinearGradientPaint((float) gpaint.getPoint1().getX(),
                    (float) gpaint.getPoint1().getY(), (float) gpaint
                    .getPoint2().getX(), (float) gpaint.getPoint2()
                    .getY(), new float[] { 0, 1 }, new Color[] {
                    gpaint.getColor1(), gpaint.getColor2() },
                    gpaint.isCyclic() ? MultipleGradientPaint.REPEAT
                            : MultipleGradientPaint.NO_CYCLE);
        }
        if (paint instanceof LinearGradientPaint) {
            final LinearGradientPaint gp = (LinearGradientPaint) paint;

            // This code currently doesn't support 'repeat'.
            // For linear gradients it is possible to construct
            // a 'tile' that is repeated with a PDF pattern, but
            // it would be very tricky as you would have to rotate
            // the coordinate system so the repeat was axially
            // aligned. At this point I'm just going to rasterize it.
            final MultipleGradientPaint.CycleMethodEnum cycle = gp
                    .getCycleMethod();
            if (cycle != MultipleGradientPaint.NO_CYCLE) {
                return false;
            }

            final Color[] cols = gp.getColors();
            final float[] fractions = gp.getFractions();

            // Build proper transform from gradient space to page space
            // ('Patterns' don't get userspace transform).
            AffineTransform transform;
            transform = new AffineTransform(getBaseTransform());
            transform.concatenate(getTransform());
            transform.concatenate(gp.getTransform());

            final List<Double> theMatrix = new java.util.ArrayList<Double>();
            final double[] mat = new double[6];
            transform.getMatrix(mat);
            for (final double element : mat) {
                theMatrix.add(new Double(element));
            }

            final Point2D p1 = gp.getStartPoint();
            final Point2D p2 = gp.getEndPoint();
            final List<Double> theCoords = new java.util.ArrayList<Double>();
            theCoords.add(new Double(p1.getX()));
            theCoords.add(new Double(p1.getY()));
            theCoords.add(new Double(p2.getX()));
            theCoords.add(new Double(p2.getY()));

            final List<Boolean> theExtend = new java.util.ArrayList<Boolean>();
            theExtend.add(Boolean.TRUE);
            theExtend.add(Boolean.TRUE);

            final List<Double> theDomain = new java.util.ArrayList<Double>();
            theDomain.add(new Double(0));
            theDomain.add(new Double(1));

            final List<Double> theEncode = new java.util.ArrayList<Double>();
            theEncode.add(new Double(0));
            theEncode.add(new Double(1));
            theEncode.add(new Double(0));
            theEncode.add(new Double(1));

            final List<Double> theBounds = new java.util.ArrayList<Double>();

            final List<Color> someColors = new java.util.ArrayList<Color>();

            for (int count = 0; count < cols.length; count++) {
                final Color c1 = cols[count];
                if (c1.getAlpha() != 255) {
                    return false; // PDF can't do alpha
                }

                // PDFColor color1 = new PDFColor(c1.getRed(), c1.getGreen(),
                // c1.getBlue());
                someColors.add(c1);
                if (count > 0 && count < cols.length - 1) {
                    theBounds.add(new Double(fractions[count]));
                }
            }

            // Gradients are currently restricted to sRGB
            PDFDeviceColorSpace aColorSpace;
            aColorSpace = new PDFDeviceColorSpace(
                    PDFDeviceColorSpace.DEVICE_RGB);
            final PDFPattern myPat = this.pdfDoc.getFactory().makeGradient(
                    this.resourceContext, false, aColorSpace, someColors,
                    theBounds, theCoords, theMatrix);
            this.currentStream.write(myPat.getColorSpaceOut(fill));

            return true;
        }
        if (paint instanceof RadialGradientPaint) {
            final RadialGradientPaint rgp = (RadialGradientPaint) paint;

            // There is essentially no way to support repeats
            // in PDF for radial gradients (the one option would
            // be to 'grow' the outer circle until it fully covered
            // the bounds and then grow the stops accordingly, the
            // problem is that this may require an extremely large
            // number of stops for cases where the focus is near
            // the edge of the outer circle). so we rasterize.
            final MultipleGradientPaint.CycleMethodEnum cycle = rgp
                    .getCycleMethod();
            if (cycle != MultipleGradientPaint.NO_CYCLE) {
                return false;
            }

            AffineTransform transform;
            transform = new AffineTransform(getBaseTransform());
            transform.concatenate(getTransform());
            transform.concatenate(rgp.getTransform());

            final List<Double> theMatrix = new java.util.ArrayList<Double>();
            final double[] mat = new double[6];
            transform.getMatrix(mat);
            for (final double element : mat) {
                theMatrix.add(new Double(element));
            }

            final double ar = rgp.getRadius();
            final Point2D ac = rgp.getCenterPoint();
            final Point2D af = rgp.getFocusPoint();

            final List<Double> theCoords = new java.util.ArrayList<Double>();
            double dx = af.getX() - ac.getX();
            double dy = af.getY() - ac.getY();
            final double d = Math.sqrt(dx * dx + dy * dy);
            if (d > ar) {
                // the center point af must be within the circle with
                // radius ar centered at ac so limit it to that.
                final double scale = ar * .9999 / d;
                dx = dx * scale;
                dy = dy * scale;
            }

            theCoords.add(new Double(ac.getX() + dx)); // Fx
            theCoords.add(new Double(ac.getY() + dy)); // Fy
            theCoords.add(new Double(0));
            theCoords.add(new Double(ac.getX()));
            theCoords.add(new Double(ac.getY()));
            theCoords.add(new Double(ar));

            final Color[] cols = rgp.getColors();
            final List<Color> someColors = new java.util.ArrayList<Color>();
            for (final Color cc : cols) {
                if (cc.getAlpha() != 255) {
                    return false; // PDF can't do alpha
                }

                someColors.add(cc);
            }

            final float[] fractions = rgp.getFractions();
            final List<Double> theBounds = new java.util.ArrayList<Double>();
            for (int count = 1; count < fractions.length - 1; count++) {
                final float offset = fractions[count];
                theBounds.add(new Double(offset));
            }
            PDFDeviceColorSpace colSpace;
            colSpace = new PDFDeviceColorSpace(PDFDeviceColorSpace.DEVICE_RGB);

            final PDFPattern myPat = this.pdfDoc.getFactory().makeGradient(
                    this.resourceContext, true, colSpace, someColors,
                    theBounds, theCoords, theMatrix);

            this.currentStream.write(myPat.getColorSpaceOut(fill));

            return true;
        }
        if (paint instanceof PatternPaint) {
            final PatternPaint pp = (PatternPaint) paint;
            return createPattern(pp, fill);
        }
        return false; // unknown paint
    }

    private boolean createPattern(final PatternPaint pp, final boolean fill) {
        preparePainting();

        final FontInfo specialFontInfo = new FontInfo();
        final boolean base14Kerning = false;
        FontSetup.setup(specialFontInfo, base14Kerning);

        final PDFResources res = this.pdfDoc.getFactory().makeResources();
        final PDFResourceContext context = new PDFResourceContext(res);
        final PDFGraphics2D pattGraphic = new PDFGraphics2D(this.textAsShapes,
                specialFontInfo, this.pdfDoc, context, getPageReference(), "",
                0);
        pattGraphic.setGraphicContext(new GraphicContext());
        pattGraphic.gc.validateTransformStack();
        pattGraphic.setRenderingHints(getRenderingHints());
        pattGraphic.setOutputStream(this.outputStream);

        final GraphicsNode gn = pp.getGraphicsNode();
        // Rectangle2D gnBBox = gn.getBounds();
        final Rectangle2D rect = pp.getPatternRect();

        // if (!pp.getOverflow()) {
        gn.paint(pattGraphic);
        // } else {
        // /* Commented out until SVN version of Batik is included */
        // // For overflow we need to paint the content from
        // // all the tiles who's overflow will intersect one
        // // tile (left->right, top->bottom). Then we can
        // // simply replicate that tile as normal.
        // double gnMinX = gnBBox.getX();
        // double gnMaxX = gnBBox.getX() + gnBBox.getWidth();
        // double gnMinY = gnBBox.getY();
        // double gnMaxY = gnBBox.getY() + gnBBox.getHeight();
        // double patMaxX = rect.getX() + rect.getWidth();
        // double patMaxY = rect.getY() + rect.getHeight();
        // double stepX = rect.getWidth();
        // double stepY = rect.getHeight();
        //
        // int startX = (int)((rect.getX() - gnMaxX)/stepX);
        // int startY = (int)((rect.getY() - gnMaxY)/stepY);
        //
        // int endX = (int)((patMaxX - gnMinX)/stepX);
        // int endY = (int)((patMaxY - gnMinY)/stepY);
        //
        // pattGraphic.translate(startX*stepX, startY*stepY);
        // for (int yIdx=startY; yIdx<=endY; yIdx++) {
        // for (int xIdx=startX; xIdx<=endX; xIdx++) {
        // gn.paint(pattGraphic);
        // pattGraphic.translate(stepX,0);
        // }
        // pattGraphic.translate(-(endX-startX+1)*stepX, stepY);
        // }
        // }

        final List<Double> bbox = new java.util.ArrayList<Double>();
        bbox.add(new Double(rect.getX()));
        bbox.add(new Double(rect.getHeight() + rect.getY()));
        bbox.add(new Double(rect.getWidth() + rect.getX()));
        bbox.add(new Double(rect.getY()));

        AffineTransform transform;
        transform = new AffineTransform(getBaseTransform());
        transform.concatenate(getTransform());
        transform.concatenate(pp.getPatternTransform());

        final List<Double> theMatrix = new java.util.ArrayList<Double>();
        final double[] mat = new double[6];
        transform.getMatrix(mat);
        for (final double element : mat) {
            theMatrix.add(new Double(element));
        }

        /**
         * @todo see if pdfDoc and res can be linked here, (currently res <>
         *       PDFDocument's resources) so addFonts() can be moved to
         *       PDFDocument class
         */
        res.addFonts(this.pdfDoc, specialFontInfo);

        final PDFPattern myPat = this.pdfDoc.getFactory().makePattern(
                this.resourceContext, 1, res, 1, 1, bbox, rect.getWidth(),
                rect.getHeight(), theMatrix, null, pattGraphic.getBuffer());

        this.currentStream.write(myPat.getColorSpaceOut(fill));

        final PDFAnnotList annots = context.getAnnotations();
        if (annots != null) {
            this.pdfDoc.addObject(annots);
        }

        flushPDFDocument();
        return true;
    }

    /**
     * @param paint
     *            some paint
     * @param shape
     *            a shape
     * @return true (always)
     */
    protected boolean applyUnknownPaint(final Paint paint, final Shape shape) {
        preparePainting();

        final Shape clip = getClip();
        Rectangle2D usrClipBounds;
        Rectangle2D usrBounds;
        usrBounds = shape.getBounds2D();
        if (clip != null) {
            usrClipBounds = clip.getBounds2D();
            if (!usrClipBounds.intersects(usrBounds)) {
                return true;
            }
            Rectangle2D.intersect(usrBounds, usrClipBounds, usrBounds);
        }
        final double usrX = usrBounds.getX();
        final double usrY = usrBounds.getY();
        final double usrW = usrBounds.getWidth();
        final double usrH = usrBounds.getHeight();

        Rectangle devShapeBounds;
        Rectangle devClipBounds;
        Rectangle devBounds;
        final AffineTransform at = getTransform();
        devShapeBounds = at.createTransformedShape(shape).getBounds();
        if (clip != null) {
            devClipBounds = at.createTransformedShape(clip).getBounds();
            if (!devClipBounds.intersects(devShapeBounds)) {
                return true;
            }
            devBounds = devShapeBounds.intersection(devClipBounds);
        } else {
            devBounds = devShapeBounds;
        }
        final int devX = devBounds.x;
        final int devY = devBounds.y;
        final int devW = devBounds.width;
        final int devH = devBounds.height;

        final ColorSpace rgbCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        final ColorModel rgbCM = new DirectColorModel(rgbCS, 32, 0x00FF0000,
                0x0000FF00, 0x000000FF, 0xFF000000, false, DataBuffer.TYPE_BYTE);

        final PaintContext pctx = paint.createContext(rgbCM, devBounds,
                usrBounds, at, getRenderingHints());
        PDFXObject imageInfo = this.pdfDoc.getXObject("TempImage:"
                + pctx.toString());
        if (imageInfo != null) {
            this.resourceContext.getPDFResources().addXObject(imageInfo);
        } else {
            final Raster r = pctx.getRaster(devX, devY, devW, devH);
            WritableRaster wr = (WritableRaster) r;
            wr = wr.createWritableTranslatedChild(0, 0);

            final ColorModel pcm = pctx.getColorModel();
            final BufferedImage bi = new BufferedImage(pcm, wr,
                    pcm.isAlphaPremultiplied(), null);
            final byte[] rgb = new byte[devW * devH * 3];
            final int[] line = new int[devW];
            final byte[] mask;
            int x;
            int y;
            int val;
            int rgbIdx = 0;

            if (pcm.hasAlpha()) {
                mask = new byte[devW * devH];
                int maskIdx = 0;
                for (y = 0; y < devH; y++) {
                    bi.getRGB(0, y, devW, 1, line, 0, devW);
                    for (x = 0; x < devW; x++) {
                        val = line[x];
                        mask[maskIdx++] = (byte) (val >>> 24);
                        rgb[rgbIdx++] = (byte) (val >> 16 & 0x0FF);
                        rgb[rgbIdx++] = (byte) (val >> 8 & 0x0FF);
                        rgb[rgbIdx++] = (byte) (val & 0x0FF);
                    }
                }
            } else {
                mask = null;
                for (y = 0; y < devH; y++) {
                    bi.getRGB(0, y, devW, 1, line, 0, devW);
                    for (x = 0; x < devW; x++) {
                        val = line[x];
                        rgb[rgbIdx++] = (byte) (val >> 16 & 0x0FF);
                        rgb[rgbIdx++] = (byte) (val >> 8 & 0x0FF);
                        rgb[rgbIdx++] = (byte) (val & 0x0FF);
                    }
                }
            }

            String maskRef = null;
            if (mask != null) {
                final BitmapImage fopimg = new BitmapImage("TempImageMask:"
                        + pctx.toString(), devW, devH, mask, null);
                fopimg.setColorSpace(new PDFDeviceColorSpace(
                        PDFDeviceColorSpace.DEVICE_GRAY));
                final PDFImageXObject xobj = this.pdfDoc.addImage(
                        this.resourceContext, fopimg);
                maskRef = xobj.referencePDF();

                flushPDFDocument();
            }
            BitmapImage fopimg;
            fopimg = new BitmapImage("TempImage:" + pctx.toString(), devW,
                    devH, rgb, maskRef);
            fopimg.setTransparent(new PDFColor(255, 255, 255));
            imageInfo = this.pdfDoc.addImage(this.resourceContext, fopimg);
            flushPDFDocument();
        }

        this.currentStream.write("q\n");
        writeClip(shape);
        this.currentStream.write("" + PDFNumber.doubleOut(usrW) + " 0 0 "
                + PDFNumber.doubleOut(-usrH) + " " + PDFNumber.doubleOut(usrX)
                + " " + PDFNumber.doubleOut(usrY + usrH) + " cm\n"
                + imageInfo.getName() + " Do\nQ\n");
        return true;
    }

    /**
     * Apply the stroke to the PDF. This takes the java stroke and outputs the
     * appropriate settings to the PDF so that the stroke attributes are
     * handled.
     *
     * @param stroke
     *            the java stroke
     */
    protected void applyStroke(final Stroke stroke) {
        preparePainting();
        if (stroke instanceof BasicStroke) {
            final BasicStroke bs = (BasicStroke) stroke;

            final float[] da = bs.getDashArray();
            if (da != null) {
                this.currentStream.write("[");
                for (int count = 0; count < da.length; count++) {
                    this.currentStream.write(PDFNumber.doubleOut(da[count]));
                    if (count < da.length - 1) {
                        this.currentStream.write(" ");
                    }
                }
                this.currentStream.write("] ");
                final float offset = bs.getDashPhase();
                this.currentStream.write(PDFNumber.doubleOut(offset) + " d\n");
            } else {
                this.currentStream.write("[] 0 d\n");
            }
            final int ec = bs.getEndCap();
            switch (ec) {
            case BasicStroke.CAP_BUTT:
                this.currentStream.write(0 + " J\n");
                break;
            case BasicStroke.CAP_ROUND:
                this.currentStream.write(1 + " J\n");
                break;
            case BasicStroke.CAP_SQUARE:
                this.currentStream.write(2 + " J\n");
                break;
            default:
                break;
            }

            final int lj = bs.getLineJoin();
            switch (lj) {
            case BasicStroke.JOIN_MITER:
                this.currentStream.write(0 + " j\n");
                break;
            case BasicStroke.JOIN_ROUND:
                this.currentStream.write(1 + " j\n");
                break;
            case BasicStroke.JOIN_BEVEL:
                this.currentStream.write(2 + " j\n");
                break;
            default:
                break;
            }
            final float lw = bs.getLineWidth();
            this.currentStream.write(PDFNumber.doubleOut(lw) + " w\n");

            final float ml = bs.getMiterLimit();
            this.currentStream.write(PDFNumber.doubleOut(ml) + " M\n");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawRenderedImage(final RenderedImage img,
            final AffineTransform xform) {
        final String key = "TempImage:" + img.toString();
        drawInnerRenderedImage(key, img, xform);
    }

    /**
     * @param key
     *            a key
     * @param img
     *            an image
     * @param xform
     *            a transform
     */
    public void drawInnerRenderedImage(final String key,
            final RenderedImage img, final AffineTransform xform) {
        preparePainting();
        PDFXObject xObject = this.pdfDoc.getXObject(key);
        if (xObject == null) {
            xObject = addRenderedImage(key, img);
        } else {
            this.resourceContext.getPDFResources().addXObject(xObject);
        }

        useXObject(xObject, xform, img.getWidth(), img.getHeight());
    }

    private void useXObject(final PDFXObject xObject,
            final AffineTransform xform, final float width, final float height) {
        // now do any transformation required and add the actual image
        // placement instance
        this.currentStream.write("q\n");
        concatMatrix(getTransform());
        final Shape imclip = getClip();
        writeClip(imclip);
        concatMatrix(xform);
        final String w = PDFNumber.doubleOut(width, DEC);
        final String h = PDFNumber.doubleOut(height, DEC);
        this.currentStream.write("" + w + " 0 0 -" + h + " 0 " + h + " cm\n"
                + xObject.getName() + " Do\nQ\n");
    }

    private PDFXObject addRenderedImage(final String key,
            final RenderedImage img) {
        final ImageInfo info = new ImageInfo(null, "image/unknown");
        final ImageSize size = new ImageSize(img.getWidth(), img.getHeight(),
                GraphicsConstants.DEFAULT_DPI);
        info.setSize(size);
        final ImageRendered imgRend = new ImageRendered(info, img, null);
        final ImageRenderedAdapter adapter = new ImageRenderedAdapter(imgRend,
                key);
        final PDFXObject xObject = this.pdfDoc.addImage(this.resourceContext,
                adapter);
        flushPDFDocument();
        return xObject;
    }

    /** {@inheritDoc} */
    @Override
    public void drawRenderableImage(final RenderableImage img,
            final AffineTransform xform) {
        // TODO Check if this is good enough
        drawRenderedImage(img.createDefaultRendering(), xform);
    }

    /**
     * Renders the text specified by the specified <code>String</code>, using
     * the current <code>Font</code> and <code>Paint</code> attributes in the
     * <code>Graphics2D</code> context. The baseline of the first character is
     * at position (<i>x</i>,&nbsp;<i>y</i>) in the User Space. The rendering
     * attributes applied include the <code>Clip</code>, <code>Transform</code>,
     * <code>Paint</code>, <code>Font</code> and <code>Composite</code>
     * attributes. For characters in script systems such as Hebrew and Arabic,
     * the glyphs can be rendered from right to left, in which case the
     * coordinate supplied is the location of the leftmost character on the
     * baseline.
     *
     * @param s
     *            the <code>String</code> to be rendered
     * @param x
     *            the coordinate where the <code>String</code> should be
     *            rendered
     * @param y
     *            the coordinate where the <code>String</code> should be
     *            rendered
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see java.awt.Graphics#setFont
     * @see #setTransform
     * @see #setComposite
     * @see #setClip
     */
    @Override
    public void drawString(final String s, final float x, final float y) {
        preparePainting();

        Font fontState;
        AffineTransform fontTransform = null;
        if (this.ovFontState == null) {
            final java.awt.Font gFont = getFont();
            fontTransform = gFont.getTransform();
            fontState = this.fontInfo.getFontInstanceForAWTFont(gFont);
        } else {
            fontState = this.fontInfo.getFontInstance(
                    this.ovFontState.getFontTriplet(),
                    this.ovFontState.getFontSize());
            this.ovFontState = null;
        }
        updateCurrentFont(fontState);

        saveGraphicsState();

        final Color c = getColor();
        applyColor(c, true);
        applyPaint(getPaint(), true);
        applyAlpha(c.getAlpha(), OPAQUE);

        final Map<Integer, Map<Integer, Integer>> kerning = fontState
                .getKerning();
        final boolean kerningAvailable = kerning != null && !kerning.isEmpty();

        final boolean useMultiByte = isMultiByteFont(this.currentFontName);

        // String startText = useMultiByte ? "<FEFF" : "(";
        final String startText = useMultiByte ? "<" : "(";
        final String endText = useMultiByte ? "> " : ") ";

        final AffineTransform trans = getTransform();
        // trans.translate(x, y);
        final double[] vals = new double[6];
        trans.getMatrix(vals);

        concatMatrix(vals);
        final Shape imclip = getClip();
        writeClip(imclip);

        this.currentStream.write("BT\n");

        final AffineTransform localTransform = new AffineTransform();
        localTransform.translate(x, y);
        if (fontTransform != null) {
            localTransform.concatenate(fontTransform);
        }
        localTransform.scale(1, -1);
        final double[] lt = new double[6];
        localTransform.getMatrix(lt);
        this.currentStream.write(PDFNumber.doubleOut(lt[0]) + " "
                + PDFNumber.doubleOut(lt[1]) + " " + PDFNumber.doubleOut(lt[2])
                + " " + PDFNumber.doubleOut(lt[3]) + " "
                + PDFNumber.doubleOut(lt[4]) + " " + PDFNumber.doubleOut(lt[5])
                + " Tm [" + startText);

        final int l = s.length();

        for (int i = 0; i < l; i++) {
            final char ch = fontState.mapChar(s.charAt(i));

            if (!useMultiByte) {
                if (ch > 127) {
                    this.currentStream.write("\\");
                    this.currentStream.write(Integer.toOctalString(ch));
                } else {
                    switch (ch) {
                    case '(':
                    case ')':
                    case '\\':
                        this.currentStream.write("\\");
                        break;
                    default:
                    }
                    this.currentStream.write(ch);
                }
            } else {
                this.currentStream.write(PDFText.toUnicodeHex(ch));
            }

            if (kerningAvailable && i + 1 < l) {
                addKerning(this.currentStream, Integer.valueOf(ch),
                        Integer.valueOf(fontState.mapChar(s.charAt(i + 1))),
                        kerning, startText, endText);
            }

        }
        this.currentStream.write(endText);

        this.currentStream.write("] TJ\n");
        this.currentStream.write("ET\n");
        restoreGraphicsState();
    }

    /**
     * Applies the given alpha values for filling and stroking.
     *
     * @param fillAlpha
     *            A value between 0 and 255 (=OPAQUE) for filling
     * @param strokeAlpha
     *            A value between 0 and 255 (=OPAQUE) for stroking
     */
    protected void applyAlpha(final int fillAlpha, final int strokeAlpha) {
        if (fillAlpha != OPAQUE || strokeAlpha != OPAQUE) {
            checkTransparencyAllowed();
            final Map<String, Float> vals = new java.util.HashMap<String, Float>();
            if (fillAlpha != OPAQUE) {
                vals.put(PDFGState.GSTATE_ALPHA_NONSTROKE, new Float(
                        fillAlpha / 255f));
            }
            if (strokeAlpha != OPAQUE) {
                vals.put(PDFGState.GSTATE_ALPHA_STROKE, new Float(
                        strokeAlpha / 255f));
            }
            final PDFGState gstate = this.pdfDoc.getFactory().makeGState(vals,
                    this.paintingState.getGState());
            this.resourceContext.addGState(gstate);
            this.currentStream.write("/" + gstate.getName() + " gs\n");
        }
    }

    /**
     * Updates the currently selected font.
     *
     * @param font
     *            the new font to use
     */
    protected void updateCurrentFont(final Font font) {
        final String name = font.getFontName();
        final float size = font.getFontSize() / 1000f;

        // Only update if necessary
        if (!name.equals(this.currentFontName) || size != this.currentFontSize) {
            this.currentFontName = name;
            this.currentFontSize = size;
            this.currentStream.write("/" + name + " " + size + " Tf\n");
        }
    }

    /**
     * Returns a suitable internal font given an AWT Font instance.
     *
     * @param awtFont
     *            the AWT font
     * @return the internal Font
     * @deprecated use FontInfo.getFontInstanceForAWTFont(java.awt.Font awtFont)
     *             instead
     */
    @Deprecated
    protected Font getInternalFontForAWTFont(final java.awt.Font awtFont) {
        return this.fontInfo.getFontInstanceForAWTFont(awtFont);
    }

    /**
     * Determines whether the font with the given name is a multi-byte font.
     *
     * @param name
     *            the name of the font
     * @return true if it's a multi-byte font
     */
    protected boolean isMultiByteFont(final String name) {
        // This assumes that *all* CIDFonts use a /ToUnicode mapping
        final org.apache.fop.fonts.Typeface f = this.fontInfo.getFonts().get(
                name);
        return f.isMultiByte();
    }

    private void addKerning(final StringWriter buf, final Integer ch1,
            final Integer ch2,
            final Map<Integer, Map<Integer, Integer>> kerning,
            final String startText, final String endText) {
        preparePainting();
        final Map<Integer, Integer> kernPair = kerning.get(ch1);

        if (kernPair != null) {
            final Integer width = kernPair.get(ch2);
            if (width != null) {
                this.currentStream.write(endText + -width.intValue() + " "
                        + startText);
            }
        }
    }

    /**
     * Renders the text of the specified iterator, using the
     * <code>Graphics2D</code> context's current <code>Paint</code>. The
     * iterator must specify a font for each character. The baseline of the
     * first character is at position (<i>x</i>,&nbsp;<i>y</i>) in the User
     * Space. The rendering attributes applied include the <code>Clip</code>,
     * <code>Transform</code>, <code>Paint</code>, and <code>Composite</code>
     * attributes. For characters in script systems such as Hebrew and Arabic,
     * the glyphs can be rendered from right to left, in which case the
     * coordinate supplied is the location of the leftmost character on the
     * baseline.
     *
     * @param iterator
     *            the iterator whose text is to be rendered
     * @param x
     *            the coordinate where the iterator's text is to be rendered
     * @param y
     *            the coordinate where the iterator's text is to be rendered
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #setTransform
     * @see #setComposite
     * @see #setClip
     */
    /*
     * TODO Reimplement for higher efficiency similar to the way it was done in
     * PDFTextPainter public void drawString(AttributedCharacterIterator
     * iterator, float x, float y) { preparePainting();
     *
     * Font fontState = null;
     *
     * Shape imclip = getClip(); writeClip(imclip); Color c = getColor();
     * applyColor(c, true); applyPaint(getPaint(), true);
     *
     * boolean fill = true; boolean stroke = false; if (true) { Stroke
     * currentStroke = getStroke(); stroke = true; applyStroke(currentStroke);
     * applyColor(c, false); applyPaint(getPaint(), false); }
     *
     * currentStream.write("BT\n");
     *
     * // set text rendering mode: // 0 - fill, 1 - stroke, 2 - fill then stroke
     * int textr = 0; if (fill && stroke) { textr = 2; } else if (stroke) {
     * textr = 1; } currentStream.write(textr + " Tr\n");
     *
     * AffineTransform trans = getTransform(); trans.translate(x, y); double[]
     * vals = new double[6]; trans.getMatrix(vals);
     *
     * for (char ch = iterator.first(); ch != CharacterIterator.DONE; ch =
     * iterator.next()) { //Map attr = iterator.getAttributes();
     *
     * String name = fontState.getFontName(); int size =
     * fontState.getFontSize(); if ((!name.equals(this.currentFontName)) ||
     * (size != this.currentFontSize)) { this.currentFontName = name;
     * this.currentFontSize = size; currentStream.write("/" + name + " " + (size
     * / 1000) + " Tf\n");
     *
     * }
     *
     * currentStream.write(PDFNumber.doubleOut(vals[0], DEC) + " " +
     * PDFNumber.doubleOut(vals[1], DEC) + " " + PDFNumber.doubleOut(vals[2],
     * DEC) + " " + PDFNumber.doubleOut(vals[3], DEC) + " " +
     * PDFNumber.doubleOut(vals[4], DEC) + " " + PDFNumber.doubleOut(vals[5],
     * DEC) + " Tm (" + ch + ") Tj\n"); }
     *
     * currentStream.write("ET\n"); }
     */

    /**
     * Fills the interior of a <code>Shape</code> using the settings of the
     * <code>Graphics2D</code> context. The rendering attributes applied include
     * the <code>Clip</code>, <code>Transform</code>, <code>Paint</code>, and
     * <code>Composite</code>.
     *
     * @param s
     *            the <code>Shape</code> to be filled
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    @Override
    public void fill(final Shape s) {
        preparePainting();

        // Transparency shortcut
        Color c;
        c = getBackground();
        if (c.getAlpha() == 0) {
            c = getColor();
            if (c.getAlpha() == 0) {
                return;
            }
        }

        final AffineTransform trans = getTransform();
        final double[] tranvals = new double[6];
        trans.getMatrix(tranvals);

        final Shape imclip = getClip();
        final boolean newClip = this.paintingState.checkClip(imclip);
        final boolean newTransform = this.paintingState.checkTransform(trans)
                && !trans.isIdentity();

        if (newClip || newTransform) {
            saveGraphicsState();
            if (newTransform) {
                concatMatrix(tranvals);
            }
            if (newClip) {
                writeClip(imclip);
            }
        }

        applyAlpha(c.getAlpha(), OPAQUE);

        c = getColor();
        applyColor(c, true);
        c = getBackground();
        applyColor(c, false);

        final Paint paint = getPaint();
        if (this.paintingState.setPaint(paint)) {
            if (!applyPaint(paint, true)) {
                // Use the shape to 'clip' the paint contents.
                applyUnknownPaint(paint, s);

                if (newClip || newTransform) {
                    restoreGraphicsState();
                }
                return;
            }
        }

        if (s instanceof Rectangle2D) {
            final Rectangle2D rect = (Rectangle2D) s;
            this.currentStream.write(PDFNumber.doubleOut(rect.getMinX(), DEC)
                    + " " + PDFNumber.doubleOut(rect.getMinY(), DEC) + " ");
            this.currentStream
            .write(PDFNumber.doubleOut(rect.getWidth(), DEC) + " "
                    + PDFNumber.doubleOut(rect.getHeight(), DEC)
                    + " re ");
            doDrawing(true, false, false);
        } else {
            final PathIterator iter = s.getPathIterator(IDENTITY_TRANSFORM);
            processPathIterator(iter);
            doDrawing(true, false,
                    iter.getWindingRule() == PathIterator.WIND_EVEN_ODD);
        }
        if (newClip || newTransform) {
            restoreGraphicsState();
        }
    }

    void saveGraphicsState() {
        this.currentStream.write("q\n");
        this.paintingState.save();
    }

    void restoreGraphicsState() {
        this.currentStream.write("Q\n");
        this.paintingState.restore();
    }

    /** Checks whether the use of transparency is allowed. */
    protected void checkTransparencyAllowed() {
        this.pdfDoc.getProfile().verifyTransparencyAllowed("Java2D graphics");
    }

    /**
     * Processes a path iterator generating the necessary painting operations.
     *
     * @param iter
     *            PathIterator to process
     */
    public void processPathIterator(final PathIterator iter) {
        while (!iter.isDone()) {
            final double[] vals = new double[6];
            final int type = iter.currentSegment(vals);
            switch (type) {
            case PathIterator.SEG_CUBICTO:
                this.currentStream.write(PDFNumber.doubleOut(vals[0], DEC)
                        + " " + PDFNumber.doubleOut(vals[1], DEC) + " "
                        + PDFNumber.doubleOut(vals[2], DEC) + " "
                        + PDFNumber.doubleOut(vals[3], DEC) + " "
                        + PDFNumber.doubleOut(vals[4], DEC) + " "
                        + PDFNumber.doubleOut(vals[5], DEC) + " c\n");
                break;
            case PathIterator.SEG_LINETO:
                this.currentStream.write(PDFNumber.doubleOut(vals[0], DEC)
                        + " " + PDFNumber.doubleOut(vals[1], DEC) + " l\n");
                break;
            case PathIterator.SEG_MOVETO:
                this.currentStream.write(PDFNumber.doubleOut(vals[0], DEC)
                        + " " + PDFNumber.doubleOut(vals[1], DEC) + " m\n");
                break;
            case PathIterator.SEG_QUADTO:
                this.currentStream.write(PDFNumber.doubleOut(vals[0], DEC)
                        + " " + PDFNumber.doubleOut(vals[1], DEC) + " "
                        + PDFNumber.doubleOut(vals[2], DEC) + " "
                        + PDFNumber.doubleOut(vals[3], DEC) + " y\n");
                break;
            case PathIterator.SEG_CLOSE:
                this.currentStream.write("h\n");
                break;
            default:
                break;
            }
            iter.next();
        }
    }

    /**
     * Do the PDF drawing command. This does the PDF drawing command according
     * to fill stroke and winding rule.
     *
     * @param fill
     *            true if filling the path
     * @param stroke
     *            true if stroking the path
     * @param nonzero
     *            true if using the non-zero winding rule
     */
    protected void doDrawing(final boolean fill, final boolean stroke,
            final boolean nonzero) {
        preparePainting();
        if (fill) {
            if (stroke) {
                if (nonzero) {
                    this.currentStream.write("B*\n");
                } else {
                    this.currentStream.write("B\n");
                }
            } else {
                if (nonzero) {
                    this.currentStream.write("f*\n");
                } else {
                    this.currentStream.write("f\n");
                }
            }
        } else {
            // if (stroke)
            this.currentStream.write("S\n");
        }
    }

    /**
     * Returns the device configuration associated with this
     * <code>Graphics2D</code>.
     *
     * @return the PDF graphics configuration
     */
    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return new PDFGraphicsConfiguration();
    }

    /**
     * Used to create proper font metrics
     */
    private Graphics2D fmg;

    {
        final BufferedImage bi = new BufferedImage(1, 1,
                BufferedImage.TYPE_INT_ARGB);

        this.fmg = bi.createGraphics();
    }

    /**
     * Gets the font metrics for the specified font.
     *
     * @return the font metrics for the specified font.
     * @param f
     *            the specified font
     * @see java.awt.Graphics#getFont
     * @see java.awt.FontMetrics
     * @see java.awt.Graphics#getFontMetrics()
     */
    @Override
    public java.awt.FontMetrics getFontMetrics(final java.awt.Font f) {
        return this.fmg.getFontMetrics(f);
    }

    /**
     * Sets the paint mode of this graphics context to alternate between this
     * graphics context's current color and the new specified color. This
     * specifies that logical pixel operations are performed in the XOR mode,
     * which alternates pixels between the current color and a specified XOR
     * color.
     * <p>
     * When drawing operations are performed, pixels which are the current color
     * are changed to the specified color, and vice versa.
     * <p>
     * Pixels that are of colors other than those two colors are changed in an
     * unpredictable but reversible manner; if the same figure is drawn twice,
     * then all pixels are restored to their original values.
     *
     * @param c1
     *            the XOR alternation color
     */
    @Override
    public void setXORMode(final Color c1) {
        // NYI
    }

    /**
     * Copies an area of the component by a distance specified by
     * <code>dx</code> and <code>dy</code>. From the point specified by
     * <code>x</code> and <code>y</code>, this method copies downwards and to
     * the right. To copy an area of the component to the left or upwards,
     * specify a negative value for <code>dx</code> or <code>dy</code>. If a
     * portion of the source rectangle lies outside the bounds of the component,
     * or is obscured by another window or component, <code>copyArea</code> will
     * be unable to copy the associated pixels. The area that is omitted can be
     * refreshed by calling the component's <code>paint</code> method.
     *
     * @param x
     *            the <i>x</i> coordinate of the source rectangle.
     * @param y
     *            the <i>y</i> coordinate of the source rectangle.
     * @param width
     *            the width of the source rectangle.
     * @param height
     *            the height of the source rectangle.
     * @param dx
     *            the horizontal distance to copy the pixels.
     * @param dy
     *            the vertical distance to copy the pixels.
     */
    @Override
    public void copyArea(final int x, final int y, final int width,
            final int height, final int dx, final int dy) {
        // NYI
    }

}
