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

/* $Id: AbstractFOPTextPainter.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.svg;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.util.Iterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.dom.svg.SVGOMTextElement;
import org.apache.batik.gvt.TextNode;
import org.apache.batik.gvt.TextPainter;
import org.apache.batik.gvt.renderer.StrokingTextPainter;
import org.apache.batik.gvt.text.GVTAttributedCharacterIterator;
import org.apache.batik.gvt.text.Mark;
import org.apache.batik.gvt.text.TextPaintInfo;
import org.apache.fop.afp.AFPGraphics2D;
import org.apache.fop.fonts.Font;

/**
 * Renders the attributed character iterator of a {@link TextNode}. This class
 * draws the text directly into the Graphics2D so that the text is not drawn
 * using shapes. If the text is simple enough to draw then it sets the font and
 * calls drawString. If the text is complex or the cannot be translated into a
 * simple drawString the StrokingTextPainter is used instead.
 */
@Slf4j
public abstract class AbstractFOPTextPainter implements TextPainter {

    private final FOPTextHandler nativeTextHandler;

    /**
     * Use the stroking text painter to get the bounds and shape. Also used as a
     * fallback to draw the string with strokes.
     */
    protected static final TextPainter PROXY_PAINTER = StrokingTextPainter
            .getInstance();

    /**
     * Create a new PS text painter with the given font information.
     *
     * @param nativeTextHandler
     *            the NativeTextHandler instance used for text painting
     */
    public AbstractFOPTextPainter(final FOPTextHandler nativeTextHandler) {
        this.nativeTextHandler = nativeTextHandler;
    }

    /**
     * Paints the specified attributed character iterator using the specified
     * Graphics2D and context and font context.
     *
     * @param node
     *            the TextNode to paint
     * @param g2d
     *            the Graphics2D to use
     */
    @Override
    public void paint(final TextNode node, final Graphics2D g2d) {
        final Point2D loc = node.getLocation();
        if (!isSupportedGraphics2D(g2d) || hasUnsupportedAttributes(node)) {
            if (log.isDebugEnabled()) {
                log.debug("painting text node "
                        + node
                        + " by stroking due to unsupported attributes or an incompatible Graphics2D");
            }
            PROXY_PAINTER.paint(node, g2d);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("painting text node " + node + " normally.");
            }
            paintTextRuns(node.getTextRuns(), g2d, loc);
        }
    }

    /**
     * Checks whether the Graphics2D is compatible with this text painter. Batik
     * may pass in a Graphics2D instance that paints on a special buffer image,
     * for example for filtering operations. In that case, the text painter
     * should be bypassed.
     *
     * @param g2d
     *            the Graphics2D instance to check
     * @return true if the Graphics2D is supported
     */
    protected abstract boolean isSupportedGraphics2D(final Graphics2D g2d);

    private boolean hasUnsupportedAttributes(final TextNode node) {
        final Iterator iter = node.getTextRuns().iterator();
        while (iter.hasNext()) {
            final StrokingTextPainter.TextRun run = (StrokingTextPainter.TextRun) iter
                    .next();
            final AttributedCharacterIterator aci = run.getACI();
            final boolean hasUnsupported = hasUnsupportedAttributes(aci);
            if (hasUnsupported) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUnsupportedAttributes(
            final AttributedCharacterIterator aci) {
        boolean hasUnsupported = false;

        final Font font = getFont(aci);
        final String text = getText(aci);
        if (hasUnsupportedGlyphs(text, font)) {
            log.trace("-> Unsupported glyphs found");
            hasUnsupported = true;
        }

        final TextPaintInfo tpi = (TextPaintInfo) aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.PAINT_INFO);
        if (tpi != null
                && (tpi.strokeStroke != null && tpi.strokePaint != null
                || tpi.strikethroughStroke != null
                || tpi.underlineStroke != null || tpi.overlineStroke != null)) {
            log.trace("-> under/overlines etc. found");
            hasUnsupported = true;
        }

        // Alpha is not supported
        final Paint foreground = (Paint) aci
                .getAttribute(TextAttribute.FOREGROUND);
        if (foreground instanceof Color) {
            final Color col = (Color) foreground;
            if (col.getAlpha() != 255) {
                log.trace("-> transparency found");
                hasUnsupported = true;
            }
        }

        final Object letSpace = aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.LETTER_SPACING);
        if (letSpace != null) {
            log.trace("-> letter spacing found");
            hasUnsupported = true;
        }

        final Object wordSpace = aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.WORD_SPACING);
        if (wordSpace != null) {
            log.trace("-> word spacing found");
            hasUnsupported = true;
        }

        final Object lengthAdjust = aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.LENGTH_ADJUST);
        if (lengthAdjust != null) {
            log.trace("-> length adjustments found");
            hasUnsupported = true;
        }

        final Object writeMod = aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.WRITING_MODE);
        if (writeMod != null
                && !GVTAttributedCharacterIterator.TextAttribute.WRITING_MODE_LTR
                .equals(writeMod)) {
            log.trace("-> Unsupported writing modes found");
            hasUnsupported = true;
        }

        final Object vertOr = aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.VERTICAL_ORIENTATION);
        if (GVTAttributedCharacterIterator.TextAttribute.ORIENTATION_ANGLE
                .equals(vertOr)) {
            log.trace("-> vertical orientation found");
            hasUnsupported = true;
        }

        final Object rcDel = aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.TEXT_COMPOUND_DELIMITER);
        // Batik 1.6 returns null here which makes it impossible to determine
        // whether this can
        // be painted or not, i.e. fall back to stroking. :-(
        if (rcDel != null && !(rcDel instanceof SVGOMTextElement)) {
            log.trace("-> spans found");
            hasUnsupported = true; // Filter spans
        }

        if (hasUnsupported) {
            log.trace("Unsupported attributes found in ACI, using StrokingTextPainter");
        }
        return hasUnsupported;
    }

    /**
     * Paint a list of text runs on the Graphics2D at a given location.
     *
     * @param textRuns
     *            the list of text runs
     * @param g2d
     *            the Graphics2D to paint to
     * @param loc
     *            the current location of the "cursor"
     */
    protected void paintTextRuns(final List textRuns, final Graphics2D g2d,
            final Point2D loc) {
        Point2D currentloc = loc;
        final Iterator i = textRuns.iterator();
        while (i.hasNext()) {
            final StrokingTextPainter.TextRun run = (StrokingTextPainter.TextRun) i
                    .next();
            currentloc = paintTextRun(run, g2d, currentloc);
        }
    }

    /**
     * Paint a single text run on the Graphics2D at a given location.
     *
     * @param run
     *            the text run to paint
     * @param g2d
     *            the Graphics2D to paint to
     * @param loc
     *            the current location of the "cursor"
     * @return the new location of the "cursor" after painting the text run
     */
    protected Point2D paintTextRun(final StrokingTextPainter.TextRun run,
            final Graphics2D g2d, Point2D loc) {
        final AttributedCharacterIterator aci = run.getACI();
        aci.first();

        updateLocationFromACI(aci, loc);
        final AffineTransform at = g2d.getTransform();
        loc = at.transform(loc, null);

        // font
        final Font font = getFont(aci);
        if (font != null) {
            this.nativeTextHandler.setOverrideFont(font);
        }

        // color
        final TextPaintInfo tpi = (TextPaintInfo) aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.PAINT_INFO);
        if (tpi == null) {
            return loc;
        }
        final Paint foreground = tpi.fillPaint;
        if (foreground instanceof Color) {
            final Color col = (Color) foreground;
            g2d.setColor(col);
        }
        g2d.setPaint(foreground);

        // text anchor
        final TextNode.Anchor anchor = (TextNode.Anchor) aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.ANCHOR_TYPE);

        // text
        final String txt = getText(aci);
        final float advance = getStringWidth(txt, font);
        float tx = 0;
        if (anchor != null) {
            switch (anchor.getType()) {
            case TextNode.Anchor.ANCHOR_MIDDLE:
                tx = -advance / 2;
                break;
            case TextNode.Anchor.ANCHOR_END:
                tx = -advance;
                break;
            default: // nop
            }
        }

        // draw string
        final double x = loc.getX();
        final double y = loc.getY();
        try {
            try {
                this.nativeTextHandler.drawString(g2d, txt, (float) x + tx,
                        (float) y);
            } catch (final IOException ioe) {
                if (g2d instanceof AFPGraphics2D) {
                    ((AFPGraphics2D) g2d).handleIOException(ioe);
                }
            }
        } finally {
            this.nativeTextHandler.setOverrideFont(null);
        }
        loc.setLocation(loc.getX() + advance, loc.getY());
        return loc;
    }

    /**
     * Extract the raw text from an ACI.
     *
     * @param aci
     *            ACI to inspect
     * @return the extracted text
     */
    protected String getText(final AttributedCharacterIterator aci) {
        final StringBuffer sb = new StringBuffer(aci.getEndIndex()
                - aci.getBeginIndex());
        for (char c = aci.first(); c != CharacterIterator.DONE; c = aci.next()) {
            sb.append(c);
        }
        return sb.toString();
    }

    private void updateLocationFromACI(final AttributedCharacterIterator aci,
            final Point2D loc) {
        // Adjust position of span
        final Float xpos = (Float) aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.X);
        final Float ypos = (Float) aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.Y);
        final Float dxpos = (Float) aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.DX);
        final Float dypos = (Float) aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.DY);
        if (xpos != null) {
            loc.setLocation(xpos.doubleValue(), loc.getY());
        }
        if (ypos != null) {
            loc.setLocation(loc.getX(), ypos.doubleValue());
        }
        if (dxpos != null) {
            loc.setLocation(loc.getX() + dxpos.doubleValue(), loc.getY());
        }
        if (dypos != null) {
            loc.setLocation(loc.getX(), loc.getY() + dypos.doubleValue());
        }
    }

    private Font getFont(final AttributedCharacterIterator aci) {
        final Font[] fonts = ACIUtils.findFontsForBatikACI(aci,
                this.nativeTextHandler.getFontInfo());
        return fonts[0];
    }

    private float getStringWidth(final String str, final Font font) {
        float wordWidth = 0;
        final float whitespaceWidth = font.getWidth(font.mapChar(' '));

        for (int i = 0; i < str.length(); i++) {
            float charWidth;
            final char c = str.charAt(i);
            if (!(c == ' ' || c == '\n' || c == '\r' || c == '\t')) {
                charWidth = font.getWidth(font.mapChar(c));
                if (charWidth <= 0) {
                    charWidth = whitespaceWidth;
                }
            } else {
                charWidth = whitespaceWidth;
            }
            wordWidth += charWidth;
        }
        return wordWidth / 1000f;
    }

    private boolean hasUnsupportedGlyphs(final String str, final Font font) {
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (!(c == ' ' || c == '\n' || c == '\r' || c == '\t')) {
                if (!font.hasChar(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the outline shape of the text characters. This uses the
     * StrokingTextPainter to get the outline shape since in theory it should be
     * the same.
     *
     * @param node
     *            the text node
     * @return the outline shape of the text characters
     */
    @Override
    public Shape getOutline(final TextNode node) {
        return PROXY_PAINTER.getOutline(node);
    }

    /**
     * Get the bounds. This uses the StrokingTextPainter to get the bounds since
     * in theory it should be the same.
     *
     * @param node
     *            the text node
     * @return the bounds of the text
     */
    @Override
    public Rectangle2D getBounds2D(final TextNode node) {
        /*
         * (todo) getBounds2D() is too slow because it uses the
         * StrokingTextPainter. We should implement this method ourselves.
         */
        return PROXY_PAINTER.getBounds2D(node);
    }

    /**
     * Get the geometry bounds. This uses the StrokingTextPainter to get the
     * bounds since in theory it should be the same.
     *
     * @param node
     *            the text node
     * @return the bounds of the text
     */
    @Override
    public Rectangle2D getGeometryBounds(final TextNode node) {
        return PROXY_PAINTER.getGeometryBounds(node);
    }

    // Methods that have no purpose for PS

    /**
     * Get the mark. This does nothing since the output is AFP and not
     * interactive.
     *
     * @param node
     *            the text node
     * @param pos
     *            the position
     * @param all
     *            select all
     * @return null
     */
    @Override
    public Mark getMark(final TextNode node, final int pos, final boolean all) {
        return null;
    }

    /**
     * Select at. This does nothing since the output is AFP and not interactive.
     *
     * @param x
     *            the x position
     * @param y
     *            the y position
     * @param node
     *            the text node
     * @return null
     */
    @Override
    public Mark selectAt(final double x, final double y, final TextNode node) {
        return null;
    }

    /**
     * Select to. This does nothing since the output is AFP and not interactive.
     *
     * @param x
     *            the x position
     * @param y
     *            the y position
     * @param beginMark
     *            the start mark
     * @return null
     */
    @Override
    public Mark selectTo(final double x, final double y, final Mark beginMark) {
        return null;
    }

    /**
     * Selec first. This does nothing since the output is AFP and not
     * interactive.
     *
     * @param node
     *            the text node
     * @return null
     */
    @Override
    public Mark selectFirst(final TextNode node) {
        return null;
    }

    /**
     * Select last. This does nothing since the output is AFP and not
     * interactive.
     *
     * @param node
     *            the text node
     * @return null
     */
    @Override
    public Mark selectLast(final TextNode node) {
        return null;
    }

    /**
     * Get selected. This does nothing since the output is AFP and not
     * interactive.
     *
     * @param start
     *            the start mark
     * @param finish
     *            the finish mark
     * @return null
     */
    @Override
    public int[] getSelected(final Mark start, final Mark finish) {
        return null;
    }

    /**
     * Get the highlighted shape. This does nothing since the output is AFP and
     * not interactive.
     *
     * @param beginMark
     *            the start mark
     * @param endMark
     *            the end mark
     * @return null
     */
    @Override
    public Shape getHighlightShape(final Mark beginMark, final Mark endMark) {
        return null;
    }

}
