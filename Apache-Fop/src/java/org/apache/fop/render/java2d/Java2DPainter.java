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

/* $Id: Java2DPainter.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.java2d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Stack;

import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.render.intermediate.AbstractIFPainter;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFState;
import org.apache.fop.render.intermediate.IFUtil;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.CharUtilities;
import org.w3c.dom.Document;

/**
 * {@link org.apache.fop.render.intermediate.IFPainter} implementation that
 * paints on a Graphics2D instance.
 */
public class Java2DPainter extends AbstractIFPainter {

    /** the IF context */
    protected IFContext ifContext;

    /** The font information */
    protected FontInfo fontInfo;

    private final Java2DBorderPainter borderPainter;

    /** The current state, holds a Graphics2D and its context */
    protected Java2DGraphicsState g2dState;
    private final Stack g2dStateStack = new Stack();

    /**
     * Main constructor.
     *
     * @param g2d
     *            the target Graphics2D instance
     * @param context
     *            the IF context
     * @param fontInfo
     *            the font information
     */
    public Java2DPainter(final Graphics2D g2d, final IFContext context,
            final FontInfo fontInfo) {
        this(g2d, context, fontInfo, null);
    }

    /**
     * Special constructor for embedded use (when another painter uses
     * Java2DPainter to convert part of a document into a bitmap, for example).
     *
     * @param g2d
     *            the target Graphics2D instance
     * @param context
     *            the IF context
     * @param fontInfo
     *            the font information
     * @param state
     *            the IF state object
     */
    public Java2DPainter(final Graphics2D g2d, final IFContext context,
            final FontInfo fontInfo, final IFState state) {
        super();
        this.ifContext = context;
        if (state != null) {
            this.state = state.push();
        } else {
            this.state = IFState.create();
        }
        this.fontInfo = fontInfo;
        this.g2dState = new Java2DGraphicsState(g2d, fontInfo,
                g2d.getTransform());
        this.borderPainter = new Java2DBorderPainter(this);
    }

    /** {@inheritDoc} */
    @Override
    public IFContext getContext() {
        return this.ifContext;
    }

    /**
     * Returns the associated {@link FontInfo} object.
     *
     * @return the font info
     */
    protected FontInfo getFontInfo() {
        return this.fontInfo;
    }

    /**
     * Returns the Java2D graphics state.
     *
     * @return the graphics state
     */
    protected Java2DGraphicsState getState() {
        return this.g2dState;
    }

    // ----------------------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void startViewport(final AffineTransform transform,
            final Dimension size, final Rectangle clipRect) throws IFException {
        saveGraphicsState();
        concatenateTransformationMatrix(transform);
        clipRect(clipRect);
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
        concatenateTransformationMatrix(transform);
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
        final Java2DRenderingContext java2dContext = new Java2DRenderingContext(
                getUserAgent(), this.g2dState.getGraph(), getFontInfo());
        return java2dContext;
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
        getState().updateClip(rect);
    }

    /** {@inheritDoc} */
    @Override
    public void fillRect(final Rectangle rect, final Paint fill)
            throws IFException {
        if (fill == null) {
            return;
        }
        if (rect.width != 0 && rect.height != 0) {
            this.g2dState.updatePaint(fill);
            this.g2dState.getGraph().fill(rect);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawBorderRect(final Rectangle rect, final BorderProps top,
            final BorderProps bottom, final BorderProps left,
            final BorderProps right) throws IFException {
        if (top != null || bottom != null || left != null || right != null) {
            try {
                this.borderPainter.drawBorders(rect, top, bottom, left, right);
            } catch (final IOException e) {
                // Won't happen with Java2D
                throw new IllegalStateException("Unexpected I/O error");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawLine(final Point start, final Point end, final int width,
            final Color color, final RuleStyle style) throws IFException {
        this.borderPainter.drawLine(start, end, width, color, style);
    }

    /** {@inheritDoc} */
    @Override
    public void drawText(final int x, final int y, final int letterSpacing,
            final int wordSpacing, final int[][] dp, final String text)
                    throws IFException {
        this.g2dState.updateColor(this.state.getTextColor());
        final FontTriplet triplet = new FontTriplet(this.state.getFontFamily(),
                this.state.getFontStyle(), this.state.getFontWeight());
        // TODO Ignored: state.getFontVariant()
        // TODO Opportunity for font caching if font state is more heavily used
        final Font font = getFontInfo().getFontInstance(triplet,
                this.state.getFontSize());
        // String fontName = font.getFontName();
        // float fontSize = state.getFontSize() / 1000f;
        this.g2dState.updateFont(font.getFontName(),
                this.state.getFontSize() * 1000);

        final Graphics2D g2d = this.g2dState.getGraph();
        final GlyphVector gv = g2d.getFont().createGlyphVector(
                g2d.getFontRenderContext(), text);
        final Point2D cursor = new Point2D.Float(0, 0);

        final int l = text.length();
        final int[] dx = IFUtil.convertDPToDX(dp);
        final int dxl = dx != null ? dx.length : 0;

        if (dx != null && dxl > 0 && dx[0] != 0) {
            cursor.setLocation(cursor.getX() - dx[0] / 10f, cursor.getY());
            gv.setGlyphPosition(0, cursor);
        }
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

            cursor.setLocation(cursor.getX() + cw + glyphAdjust, cursor.getY());
            gv.setGlyphPosition(i + 1, cursor);
        }
        g2d.drawGlyphVector(gv, x, y);
    }

    /** Saves the current graphics state on the stack. */
    protected void saveGraphicsState() {
        this.g2dStateStack.push(this.g2dState);
        this.g2dState = new Java2DGraphicsState(this.g2dState);
    }

    /** Restores the last graphics state from the stack. */
    protected void restoreGraphicsState() {
        this.g2dState.dispose();
        this.g2dState = (Java2DGraphicsState) this.g2dStateStack.pop();
    }

    private void concatenateTransformationMatrix(final AffineTransform transform) {
        this.g2dState.transform(transform);
    }

}
