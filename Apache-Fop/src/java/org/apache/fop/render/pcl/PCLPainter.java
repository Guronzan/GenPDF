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

/* $Id: PCLPainter.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.pcl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Map;
import java.util.Stack;

import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.render.ImageHandlerUtil;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.render.intermediate.AbstractIFPainter;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFState;
import org.apache.fop.render.intermediate.IFUtil;
import org.apache.fop.render.java2d.FontMetricsMapper;
import org.apache.fop.render.java2d.Java2DPainter;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.CharUtilities;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageProcessingHints;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.ImageGraphics2D;
import org.apache.xmlgraphics.java2d.GraphicContext;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;
import org.w3c.dom.Document;

/**
 * {@link org.apache.fop.render.intermediate.IFPainter} implementation that
 * produces PCL 5.
 */
public class PCLPainter extends AbstractIFPainter implements PCLConstants {

    private static final boolean DEBUG = false;

    private final PCLDocumentHandler parent;

    /** The PCL generator */
    private final PCLGenerator gen;

    private final PCLPageDefinition currentPageDefinition;
    private int currentPrintDirection = 0;
    // private GeneralPath currentPath = null;

    private final Stack graphicContextStack = new Stack();
    private GraphicContext graphicContext = new GraphicContext();

    /**
     * Main constructor.
     *
     * @param parent
     *            the parent document handler
     * @param pageDefinition
     *            the page definition describing the page to be rendered
     */
    public PCLPainter(final PCLDocumentHandler parent,
            final PCLPageDefinition pageDefinition) {
        this.parent = parent;
        this.gen = parent.getPCLGenerator();
        this.state = IFState.create();
        this.currentPageDefinition = pageDefinition;
    }

    /** {@inheritDoc} */
    @Override
    public IFContext getContext() {
        return this.parent.getContext();
    }

    PCLRenderingUtil getPCLUtil() {
        return this.parent.getPCLUtil();
    }

    /** @return the target resolution */
    protected int getResolution() {
        final int resolution = Math.round(getUserAgent().getTargetResolution());
        if (resolution <= 300) {
            return 300;
        } else {
            return 600;
        }
    }

    private boolean isSpeedOptimized() {
        return getPCLUtil().getRenderingMode() == PCLRenderingMode.SPEED;
    }

    // ----------------------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void startViewport(final AffineTransform transform,
            final Dimension size, final Rectangle clipRect) throws IFException {
        saveGraphicsState();
        try {
            concatenateTransformationMatrix(transform);
            /*
             * PCL cannot clip! if (clipRect != null) { clipRect(clipRect); }
             */
        } catch (final IOException ioe) {
            throw new IFException("I/O error in startViewport()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endViewport() throws IFException {
        restoreGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    public void startGroup(final AffineTransform transform) throws IFException {
        saveGraphicsState();
        try {
            concatenateTransformationMatrix(transform);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in startGroup()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endGroup() throws IFException {
        restoreGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final String uri, final Rectangle rect)
            throws IFException {
        drawImageUsingURI(uri, rect);
    }

    /** {@inheritDoc} */
    @Override
    protected RenderingContext createRenderingContext() {
        final PCLRenderingContext pdfContext = new PCLRenderingContext(
                getUserAgent(), this.gen, getPCLUtil()) {

            @Override
            public Point2D transformedPoint(final int x, final int y) {
                return PCLPainter.this.transformedPoint(x, y);
            }

            @Override
            public GraphicContext getGraphicContext() {
                return PCLPainter.this.graphicContext;
            }

        };
        return pdfContext;
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final Document doc, final Rectangle rect)
            throws IFException {
        drawImageUsingDocument(doc, rect);
    }

    /** {@inheritDoc} */
    @Override
    public void clipRect(final Rectangle rect) throws IFException {
        // PCL cannot clip (only HP GL/2 can)
        // If you need clipping support, switch to RenderingMode.BITMAP.
    }

    /** {@inheritDoc} */
    @Override
    public void fillRect(final Rectangle rect, final Paint fill)
            throws IFException {
        if (fill == null) {
            return;
        }
        if (rect.width != 0 && rect.height != 0) {
            Color fillColor = null;
            if (fill != null) {
                if (fill instanceof Color) {
                    fillColor = (Color) fill;
                } else {
                    throw new UnsupportedOperationException(
                            "Non-Color paints NYI");
                }
                try {
                    setCursorPos(rect.x, rect.y);
                    this.gen.fillRect(rect.width, rect.height, fillColor);
                } catch (final IOException ioe) {
                    throw new IFException("I/O error in fillRect()", ioe);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawBorderRect(final Rectangle rect, final BorderProps top,
            final BorderProps bottom, final BorderProps left,
            final BorderProps right) throws IFException {
        if (isSpeedOptimized()) {
            super.drawBorderRect(rect, top, bottom, left, right);
            return;
        }
        if (top != null || bottom != null || left != null || right != null) {
            final Rectangle boundingBox = rect;
            final Dimension dim = boundingBox.getSize();

            final Graphics2DImagePainter painter = new Graphics2DImagePainter() {

                @Override
                public void paint(final Graphics2D g2d, final Rectangle2D area) {
                    g2d.translate(-rect.x, -rect.y);

                    final Java2DPainter painter = new Java2DPainter(g2d,
                            getContext(), PCLPainter.this.parent.getFontInfo(),
                            PCLPainter.this.state);
                    try {
                        painter.drawBorderRect(rect, top, bottom, left, right);
                    } catch (final IFException e) {
                        // This should never happen with the Java2DPainter
                        throw new RuntimeException(
                                "Unexpected error while painting borders", e);
                    }
                }

                @Override
                public Dimension getImageSize() {
                    return dim.getSize();
                }

            };
            paintMarksAsBitmap(painter, boundingBox);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawLine(final Point start, final Point end, final int width,
            final Color color, final RuleStyle style) throws IFException {
        if (isSpeedOptimized()) {
            super.drawLine(start, end, width, color, style);
            return;
        }
        final Rectangle boundingBox = getLineBoundingBox(start, end, width);
        final Dimension dim = boundingBox.getSize();

        final Graphics2DImagePainter painter = new Graphics2DImagePainter() {

            @Override
            public void paint(final Graphics2D g2d, final Rectangle2D area) {
                g2d.translate(-boundingBox.x, -boundingBox.y);

                final Java2DPainter painter = new Java2DPainter(g2d,
                        getContext(), PCLPainter.this.parent.getFontInfo(),
                        PCLPainter.this.state);
                try {
                    painter.drawLine(start, end, width, color, style);
                } catch (final IFException e) {
                    // This should never happen with the Java2DPainter
                    throw new RuntimeException(
                            "Unexpected error while painting a line", e);
                }
            }

            @Override
            public Dimension getImageSize() {
                return dim.getSize();
            }

        };
        paintMarksAsBitmap(painter, boundingBox);
    }

    private void paintMarksAsBitmap(final Graphics2DImagePainter painter,
            final Rectangle boundingBox) throws IFException {
        final ImageInfo info = new ImageInfo(null, null);
        final ImageSize size = new ImageSize();
        size.setSizeInMillipoints(boundingBox.width, boundingBox.height);
        info.setSize(size);
        final ImageGraphics2D img = new ImageGraphics2D(info, painter);

        final Map hints = new java.util.HashMap();
        if (isSpeedOptimized()) {
            // Gray text may not be painted in this case! We don't get dithering
            // in Sun JREs.
            // But this approach is about twice as fast as the grayscale image.
            hints.put(ImageProcessingHints.BITMAP_TYPE_INTENT,
                    ImageProcessingHints.BITMAP_TYPE_INTENT_MONO);
        } else {
            hints.put(ImageProcessingHints.BITMAP_TYPE_INTENT,
                    ImageProcessingHints.BITMAP_TYPE_INTENT_GRAY);
        }
        hints.put(ImageHandlerUtil.CONVERSION_MODE,
                ImageHandlerUtil.CONVERSION_MODE_BITMAP);
        final PCLRenderingContext context = (PCLRenderingContext) createRenderingContext();
        context.setSourceTransparencyEnabled(true);
        try {
            drawImage(img, boundingBox, context, true, hints);
        } catch (final IOException ioe) {
            throw new IFException(
                    "I/O error while painting marks using a bitmap", ioe);
        } catch (final ImageException ie) {
            throw new IFException("Error while painting marks using a bitmap",
                    ie);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawText(final int x, final int y, final int letterSpacing,
            final int wordSpacing, final int[][] dp, final String text)
                    throws IFException {
        try {
            final FontTriplet triplet = new FontTriplet(
                    this.state.getFontFamily(), this.state.getFontStyle(),
                    this.state.getFontWeight());
            // TODO Ignored: state.getFontVariant()
            // TODO Opportunity for font caching if font state is more heavily
            // used
            final String fontKey = this.parent.getFontInfo()
                    .getInternalFontKey(triplet);
            final boolean pclFont = getPCLUtil().isAllTextAsBitmaps() ? false
                    : HardcodedFonts.setFont(this.gen, fontKey,
                            this.state.getFontSize(), text);
            if (pclFont) {
                drawTextNative(x, y, letterSpacing, wordSpacing, dp, text,
                        triplet);
            } else {
                drawTextAsBitmap(x, y, letterSpacing, wordSpacing, dp, text,
                        triplet);
                if (DEBUG) {
                    this.state.setTextColor(Color.GRAY);
                    HardcodedFonts.setFont(this.gen, "F1",
                            this.state.getFontSize(), text);
                    drawTextNative(x, y, letterSpacing, wordSpacing, dp, text,
                            triplet);
                }
            }
        } catch (final IOException ioe) {
            throw new IFException("I/O error in drawText()", ioe);
        }
    }

    private void drawTextNative(final int x, final int y,
            final int letterSpacing, final int wordSpacing, final int[][] dp,
            final String text, final FontTriplet triplet) throws IOException {
        final Color textColor = this.state.getTextColor();
        if (textColor != null) {
            this.gen.setTransparencyMode(true, false);
            this.gen.selectGrayscale(textColor);
        }

        this.gen.setTransparencyMode(true, true);
        setCursorPos(x, y);

        final float fontSize = this.state.getFontSize() / 1000f;
        final Font font = this.parent.getFontInfo().getFontInstance(triplet,
                this.state.getFontSize());
        final int l = text.length();
        final int[] dx = IFUtil.convertDPToDX(dp);
        final int dxl = dx != null ? dx.length : 0;

        final StringBuffer sb = new StringBuffer(Math.max(16, l));
        if (dx != null && dxl > 0 && dx[0] != 0) {
            sb.append("\u001B&a+")
            .append(this.gen.formatDouble2(dx[0] / 100.0)).append('H');
        }
        for (int i = 0; i < l; i++) {
            final char orgChar = text.charAt(i);
            char ch;
            float glyphAdjust = 0;
            if (font.hasChar(orgChar)) {
                ch = font.mapChar(orgChar);
            } else {
                if (CharUtilities.isFixedWidthSpace(orgChar)) {
                    // Fixed width space are rendered as spaces so copy/paste
                    // works in a reader
                    ch = font.mapChar(CharUtilities.SPACE);
                    final int spaceDiff = font.getCharWidth(ch)
                            - font.getCharWidth(orgChar);
                    glyphAdjust = -(10 * spaceDiff / fontSize);
                } else {
                    ch = font.mapChar(orgChar);
                }
            }
            sb.append(ch);

            if (wordSpacing != 0 && CharUtilities.isAdjustableSpace(orgChar)) {
                glyphAdjust += wordSpacing;
            }
            glyphAdjust += letterSpacing;
            if (dx != null && i < dxl - 1) {
                glyphAdjust += dx[i + 1];
            }

            if (glyphAdjust != 0) {
                sb.append("\u001B&a+")
                .append(this.gen.formatDouble2(glyphAdjust / 100.0))
                .append('H');
            }

        }
        this.gen.getOutputStream().write(
                sb.toString().getBytes(this.gen.getTextEncoding()));

    }

    private static final double SAFETY_MARGIN_FACTOR = 0.05;

    private Rectangle getTextBoundingBox(
            // CSOK: ParameterNumber
            final int x, final int y, final int letterSpacing,
            final int wordSpacing, final int[][] dp, final String text,
            final Font font, final FontMetricsMapper metrics) {
        final int maxAscent = metrics.getMaxAscent(font.getFontSize()) / 1000;
        final int descent = metrics.getDescender(font.getFontSize()) / 1000; // is
        // negative
        final int safetyMargin = (int) (SAFETY_MARGIN_FACTOR * font
                .getFontSize());
        final Rectangle boundingRect = new Rectangle(x, y - maxAscent
                - safetyMargin, 0, maxAscent - descent + 2 * safetyMargin);

        final int l = text.length();
        final int[] dx = IFUtil.convertDPToDX(dp);
        final int dxl = dx != null ? dx.length : 0;

        if (dx != null && dxl > 0 && dx[0] != 0) {
            boundingRect.setLocation(
                    boundingRect.x - (int) Math.ceil(dx[0] / 10f),
                    boundingRect.y);
        }
        float width = 0.0f;
        for (int i = 0; i < l; i++) {
            final char orgChar = text.charAt(i);
            float glyphAdjust = 0;
            final int cw = font.getCharWidth(orgChar);

            if (wordSpacing != 0 && CharUtilities.isAdjustableSpace(orgChar)) {
                glyphAdjust += wordSpacing;
            }
            glyphAdjust += letterSpacing;
            if (dx != null && i < dxl - 1) {
                glyphAdjust += dx[i + 1];
            }

            width += cw + glyphAdjust;
        }
        final int extraWidth = font.getFontSize() / 3;
        boundingRect.setSize((int) Math.ceil(width) + extraWidth,
                boundingRect.height);
        return boundingRect;
    }

    private void drawTextAsBitmap(final int x, final int y,
            final int letterSpacing, final int wordSpacing, final int[][] dp,
            final String text, final FontTriplet triplet) throws IFException {
        // Use Java2D to paint different fonts via bitmap
        final Font font = this.parent.getFontInfo().getFontInstance(triplet,
                this.state.getFontSize());

        // for cursive fonts, so the text isn't clipped
        final FontMetricsMapper mapper = (FontMetricsMapper) this.parent
                .getFontInfo().getMetricsFor(font.getFontName());
        final int maxAscent = mapper.getMaxAscent(font.getFontSize()) / 1000;
        final int ascent = mapper.getAscender(font.getFontSize()) / 1000;
        final int descent = mapper.getDescender(font.getFontSize()) / 1000;
        final int safetyMargin = (int) (SAFETY_MARGIN_FACTOR * font
                .getFontSize());
        final int baselineOffset = maxAscent + safetyMargin;

        final Rectangle boundingBox = getTextBoundingBox(x, y, letterSpacing,
                wordSpacing, dp, text, font, mapper);
        final Dimension dim = boundingBox.getSize();

        final Graphics2DImagePainter painter = new Graphics2DImagePainter() {

            @Override
            public void paint(final Graphics2D g2d, final Rectangle2D area) {
                if (DEBUG) {
                    g2d.setBackground(Color.LIGHT_GRAY);
                    g2d.clearRect(0, 0, (int) area.getWidth(),
                            (int) area.getHeight());
                }
                g2d.translate(-x, -y + baselineOffset);

                if (DEBUG) {
                    Rectangle rect = new Rectangle(x, y - maxAscent, 3000,
                            maxAscent);
                    g2d.draw(rect);
                    rect = new Rectangle(x, y - ascent, 2000, ascent);
                    g2d.draw(rect);
                    rect = new Rectangle(x, y, 1000, -descent);
                    g2d.draw(rect);
                }
                final Java2DPainter painter = new Java2DPainter(g2d,
                        getContext(), PCLPainter.this.parent.getFontInfo(),
                        PCLPainter.this.state);
                try {
                    painter.drawText(x, y, letterSpacing, wordSpacing, dp, text);
                } catch (final IFException e) {
                    // This should never happen with the Java2DPainter
                    throw new RuntimeException(
                            "Unexpected error while painting text", e);
                }
            }

            @Override
            public Dimension getImageSize() {
                return dim.getSize();
            }

        };
        paintMarksAsBitmap(painter, boundingBox);
    }

    /** Saves the current graphics state on the stack. */
    private void saveGraphicsState() {
        this.graphicContextStack.push(this.graphicContext);
        this.graphicContext = (GraphicContext) this.graphicContext.clone();
    }

    /** Restores the last graphics state from the stack. */
    private void restoreGraphicsState() {
        this.graphicContext = (GraphicContext) this.graphicContextStack.pop();
    }

    private void concatenateTransformationMatrix(final AffineTransform transform)
            throws IOException {
        if (!transform.isIdentity()) {
            this.graphicContext.transform(transform);
            changePrintDirection();
        }
    }

    private Point2D transformedPoint(final int x, final int y) {
        return PCLRenderingUtil.transformedPoint(x, y,
                this.graphicContext.getTransform(), this.currentPageDefinition,
                this.currentPrintDirection);
    }

    private void changePrintDirection() throws IOException {
        final AffineTransform at = this.graphicContext.getTransform();
        int newDir;
        newDir = PCLRenderingUtil.determinePrintDirection(at);
        if (newDir != this.currentPrintDirection) {
            this.currentPrintDirection = newDir;
            this.gen.changePrintDirection(this.currentPrintDirection);
        }
    }

    /**
     * Sets the current cursor position. The coordinates are transformed to the
     * absolute position on the logical PCL page and then passed on to the
     * PCLGenerator.
     *
     * @param x
     *            the x coordinate (in millipoints)
     * @param y
     *            the y coordinate (in millipoints)
     */
    void setCursorPos(final int x, final int y) throws IOException {
        final Point2D transPoint = transformedPoint(x, y);
        this.gen.setCursorPos(transPoint.getX(), transPoint.getY());
    }

}
