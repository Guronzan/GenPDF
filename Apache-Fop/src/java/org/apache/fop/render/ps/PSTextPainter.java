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

/* $Id: PSTextPainter.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.ps;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.Iterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.gvt.font.GVTGlyphVector;
import org.apache.batik.gvt.text.TextPaintInfo;
import org.apache.batik.gvt.text.TextSpanLayout;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.fonts.LazyFont;
import org.apache.fop.fonts.MultiByteFont;
import org.apache.fop.svg.NativeTextPainter;
import org.apache.fop.util.CharUtilities;
import org.apache.fop.util.HexEncoder;
import org.apache.xmlgraphics.java2d.ps.PSGraphics2D;
import org.apache.xmlgraphics.ps.PSGenerator;

/**
 * Renders the attributed character iterator of a text node. This class draws
 * the text directly using PostScript text operators so the text is not drawn
 * using shapes which makes the PS files larger.
 * <p>
 * The text runs are split into smaller text runs that can be bundles in single
 * calls of the xshow, yshow or xyshow operators. For outline text, the charpath
 * operator is used.
 */
@Slf4j
public class PSTextPainter extends NativeTextPainter {

    private static final boolean DEBUG = false;

    private final FontResourceCache fontResources;

    private static final AffineTransform IDENTITY_TRANSFORM = new AffineTransform();

    /**
     * Create a new PS text painter with the given font information.
     *
     * @param fontInfo
     *            the font collection
     */
    public PSTextPainter(final FontInfo fontInfo) {
        super(fontInfo);
        this.fontResources = new FontResourceCache(fontInfo);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isSupported(final Graphics2D g2d) {
        return g2d instanceof PSGraphics2D;
    }

    /** {@inheritDoc} */
    @Override
    protected void paintTextRun(final TextRun textRun, final Graphics2D g2d)
            throws IOException {
        final AttributedCharacterIterator runaci = textRun.getACI();
        runaci.first();

        final TextPaintInfo tpi = (TextPaintInfo) runaci
                .getAttribute(PAINT_INFO);
        if (tpi == null || !tpi.visible) {
            return;
        }
        if (tpi != null && tpi.composite != null) {
            g2d.setComposite(tpi.composite);
        }

        // ------------------------------------
        final TextSpanLayout layout = textRun.getLayout();
        logTextRun(runaci, layout);
        final CharSequence chars = collectCharacters(runaci);
        runaci.first(); // Reset ACI

        final PSGraphics2D ps = (PSGraphics2D) g2d;
        final PSGenerator gen = ps.getPSGenerator();
        ps.preparePainting();

        if (DEBUG) {
            log.debug("Text: " + chars);
            gen.commentln("%Text: " + chars);
        }

        GeneralPath debugShapes = null;
        if (DEBUG) {
            debugShapes = new GeneralPath();
        }

        final TextUtil textUtil = new TextUtil(gen);
        textUtil.setupFonts(runaci);
        if (!textUtil.hasFonts()) {
            // Draw using Java2D when no native fonts are available
            textRun.getLayout().draw(g2d);
            return;
        }

        gen.saveGraphicsState();
        gen.concatMatrix(g2d.getTransform());
        final Shape imclip = g2d.getClip();
        clip(ps, imclip);

        gen.writeln("BT"); // beginTextObject()

        final AffineTransform localTransform = new AffineTransform();
        Point2D prevPos = null;
        final GVTGlyphVector gv = layout.getGlyphVector();
        final PSTextRun psRun = new PSTextRun(); // Used to split a text run
        // into smaller runs
        for (int index = 0, c = gv.getNumGlyphs(); index < c; index++) {
            final char ch = chars.charAt(index);
            final boolean visibleChar = gv.isGlyphVisible(index)
                    || CharUtilities.isAnySpace(ch)
                    && !CharUtilities.isZeroWidthSpace(ch);
            logCharacter(ch, layout, index, visibleChar);
            if (!visibleChar) {
                continue;
            }
            final Point2D glyphPos = gv.getGlyphPosition(index);

            final AffineTransform glyphTransform = gv.getGlyphTransform(index);
            if (log.isTraceEnabled()) {
                log.trace("pos " + glyphPos + ", transform " + glyphTransform);
            }
            if (DEBUG) {
                Shape sh = gv.getGlyphLogicalBounds(index);
                if (sh == null) {
                    sh = new Ellipse2D.Double(glyphPos.getX(), glyphPos.getY(),
                            2, 2);
                }
                debugShapes.append(sh, false);
            }

            // Exact position of the glyph
            localTransform.setToIdentity();
            localTransform.translate(glyphPos.getX(), glyphPos.getY());
            if (glyphTransform != null) {
                localTransform.concatenate(glyphTransform);
            }
            localTransform.scale(1, -1);

            boolean flushCurrentRun = false;
            // Try to optimize by combining characters using the same font and
            // on the same line.
            if (glyphTransform != null) {
                // Happens for text-on-a-path
                flushCurrentRun = true;
            }
            if (psRun.getRunLength() >= 128) {
                // Don't let a run get too long
                flushCurrentRun = true;
            }

            // Note the position of the glyph relative to the previous one
            Point2D relPos;
            if (prevPos == null) {
                relPos = new Point2D.Double(0, 0);
            } else {
                relPos = new Point2D.Double(glyphPos.getX() - prevPos.getX(),
                        glyphPos.getY() - prevPos.getY());
            }
            if (psRun.vertChanges == 0 && psRun.getHorizRunLength() > 2
                    && relPos.getY() != 0) {
                // new line
                flushCurrentRun = true;
            }

            // Select the actual character to paint
            final char paintChar = CharUtilities.isAnySpace(ch) ? ' ' : ch;

            // Select (sub)font for character
            final Font f = textUtil.selectFontForChar(paintChar);
            final char mapped = f.mapChar(ch);
            final boolean fontChanging = textUtil.isFontChanging(f, mapped);
            if (fontChanging) {
                flushCurrentRun = true;
            }

            if (flushCurrentRun) {
                // Paint the current run and reset for the next run
                psRun.paint(ps, textUtil, tpi);
                psRun.reset();
            }

            // Track current run
            psRun.addCharacter(paintChar, relPos);
            psRun.noteStartingTransformation(localTransform);

            // Change font if necessary
            if (fontChanging) {
                textUtil.setCurrentFont(f, mapped);
            }

            // Update last position
            prevPos = glyphPos;
        }
        psRun.paint(ps, textUtil, tpi);
        gen.writeln("ET"); // endTextObject()
        gen.restoreGraphicsState();

        if (DEBUG) {
            // Paint debug shapes
            g2d.setStroke(new BasicStroke(0));
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.draw(debugShapes);
        }
    }

    private void applyColor(final Paint paint, final PSGenerator gen)
            throws IOException {
        if (paint == null) {
            return;
        } else if (paint instanceof Color) {
            final Color col = (Color) paint;
            gen.useColor(col);
        } else {
            log.warn("Paint not supported: " + paint.toString());
        }
    }

    private PSFontResource getResourceForFont(final Font f, final String postfix) {
        final String key = postfix != null ? f.getFontName() + '_' + postfix
                : f.getFontName();
        return this.fontResources.getFontResourceForFontKey(key);
    }

    private void clip(final PSGraphics2D ps, final Shape shape)
            throws IOException {
        if (shape == null) {
            return;
        }
        ps.getPSGenerator().writeln("newpath");
        final PathIterator iter = shape.getPathIterator(IDENTITY_TRANSFORM);
        ps.processPathIterator(iter);
        ps.getPSGenerator().writeln("clip");
    }

    private class TextUtil {

        private final PSGenerator gen;
        private Font[] fonts;
        private Font currentFont;
        private int currentEncoding = -1;

        public TextUtil(final PSGenerator gen) {
            this.gen = gen;
        }

        public Font selectFontForChar(final char ch) {
            for (final Font font : this.fonts) {
                if (font.hasChar(ch)) {
                    return font;
                }
            }
            return this.fonts[0]; // TODO Maybe fall back to painting with
            // shapes
        }

        public void writeTextMatrix(final AffineTransform transform)
                throws IOException {
            final double[] matrix = new double[6];
            transform.getMatrix(matrix);
            this.gen.writeln(this.gen.formatDouble5(matrix[0]) + " "
                    + this.gen.formatDouble5(matrix[1]) + " "
                    + this.gen.formatDouble5(matrix[2]) + " "
                    + this.gen.formatDouble5(matrix[3]) + " "
                    + this.gen.formatDouble5(matrix[4]) + " "
                    + this.gen.formatDouble5(matrix[5]) + " Tm");
        }

        public boolean isFontChanging(final Font f, final char mapped) {
            if (f != getCurrentFont()) {
                return true;
            }
            if (mapped / 256 != getCurrentFontEncoding()) {
                return true;
            }
            return false; // Font is the same
        }

        public void selectFont(final Font f, final char mapped)
                throws IOException {
            final int encoding = mapped / 256;
            final String postfix = encoding == 0 ? null : Integer
                    .toString(encoding);
            final PSFontResource res = getResourceForFont(f, postfix);
            this.gen.useFont("/" + res.getName(), f.getFontSize() / 1000f);
            res.notifyResourceUsageOnPage(this.gen.getResourceTracker());
        }

        public Font getCurrentFont() {
            return this.currentFont;
        }

        public int getCurrentFontEncoding() {
            return this.currentEncoding;
        }

        public void setCurrentFont(final Font font, final int encoding) {
            this.currentFont = font;
            this.currentEncoding = encoding;
        }

        public void setCurrentFont(final Font font, final char mapped) {
            final int encoding = mapped / 256;
            setCurrentFont(font, encoding);
        }

        public void setupFonts(final AttributedCharacterIterator runaci) {
            this.fonts = findFonts(runaci);
        }

        public boolean hasFonts() {
            return this.fonts != null && this.fonts.length > 0;
        }

    }

    private class PSTextRun {

        private AffineTransform textTransform;
        private final List relativePositions = new java.util.LinkedList();
        private final StringBuilder currentChars = new StringBuilder();
        private int horizChanges = 0;
        private int vertChanges = 0;

        public void reset() {
            this.textTransform = null;
            this.currentChars.setLength(0);
            this.horizChanges = 0;
            this.vertChanges = 0;
            this.relativePositions.clear();
        }

        public int getHorizRunLength() {
            if (this.vertChanges == 0 && getRunLength() > 0) {
                return getRunLength();
            }
            return 0;
        }

        public void addCharacter(final char paintChar, final Point2D relPos) {
            addRelativePosition(relPos);
            this.currentChars.append(paintChar);
        }

        private void addRelativePosition(final Point2D relPos) {
            if (getRunLength() > 0) {
                if (relPos.getX() != 0) {
                    this.horizChanges++;
                }
                if (relPos.getY() != 0) {
                    this.vertChanges++;
                }
            }
            this.relativePositions.add(relPos);
        }

        public void noteStartingTransformation(final AffineTransform transform) {
            if (this.textTransform == null) {
                this.textTransform = new AffineTransform(transform);
            }
        }

        public int getRunLength() {
            return this.currentChars.length();
        }

        private boolean isXShow() {
            return this.vertChanges == 0;
        }

        private boolean isYShow() {
            return this.horizChanges == 0;
        }

        public void paint(final PSGraphics2D g2d, final TextUtil textUtil,
                final TextPaintInfo tpi) throws IOException {
            if (getRunLength() > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Text run: " + this.currentChars);
                }
                textUtil.writeTextMatrix(this.textTransform);
                if (isXShow()) {
                    log.debug("Horizontal text: xshow");
                    paintXYShow(g2d, textUtil, tpi.fillPaint, true, false);
                } else if (isYShow()) {
                    log.debug("Vertical text: yshow");
                    paintXYShow(g2d, textUtil, tpi.fillPaint, false, true);
                } else {
                    log.debug("Arbitrary text: xyshow");
                    paintXYShow(g2d, textUtil, tpi.fillPaint, true, true);
                }
                final boolean stroke = tpi.strokePaint != null
                        && tpi.strokeStroke != null;
                if (stroke) {
                    log.debug("Stroked glyph outlines");
                    paintStrokedGlyphs(g2d, textUtil, tpi.strokePaint,
                            tpi.strokeStroke);
                }
            }
        }

        private void paintXYShow(final PSGraphics2D g2d,
                final TextUtil textUtil, final Paint paint, final boolean x,
                final boolean y) throws IOException {
            final PSGenerator gen = textUtil.gen;
            final char firstChar = this.currentChars.charAt(0);
            // Font only has to be setup up before the first character
            final Font f = textUtil.selectFontForChar(firstChar);
            char mapped = f.mapChar(firstChar);
            textUtil.selectFont(f, mapped);
            textUtil.setCurrentFont(f, mapped);
            applyColor(paint, gen);

            final FontMetrics metrics = f.getFontMetrics();
            final boolean multiByte = metrics instanceof MultiByteFont
                    || metrics instanceof LazyFont
                    && ((LazyFont) metrics).getRealFont() instanceof MultiByteFont;
            final StringBuilder sb = new StringBuilder();
            sb.append(multiByte ? '<' : '(');
            for (int i = 0, c = this.currentChars.length(); i < c; i++) {
                final char ch = this.currentChars.charAt(i);
                mapped = f.mapChar(ch);
                if (multiByte) {
                    sb.append(HexEncoder.encode(mapped));
                } else {
                    final char codepoint = (char) (mapped % 256);
                    PSGenerator.escapeChar(codepoint, sb);
                }
            }
            sb.append(multiByte ? '>' : ')');
            if (x || y) {
                sb.append("\n[");
                int idx = 0;
                final Iterator iter = this.relativePositions.iterator();
                while (iter.hasNext()) {
                    final Point2D pt = (Point2D) iter.next();
                    if (idx > 0) {
                        if (x) {
                            sb.append(format(gen, pt.getX()));
                        }
                        if (y) {
                            if (x) {
                                sb.append(' ');
                            }
                            sb.append(format(gen, -pt.getY()));
                        }
                        if (idx % 8 == 0) {
                            sb.append('\n');
                        } else {
                            sb.append(' ');
                        }
                    }
                    idx++;
                }
                if (x) {
                    sb.append('0');
                }
                if (y) {
                    if (x) {
                        sb.append(' ');
                    }
                    sb.append('0');
                }
                sb.append(']');
            }
            sb.append(' ');
            if (x) {
                sb.append('x');
            }
            if (y) {
                sb.append('y');
            }
            sb.append("show"); // --> xshow, yshow or xyshow
            gen.writeln(sb.toString());
        }

        private String format(final PSGenerator gen, final double coord) {
            if (Math.abs(coord) < 0.00001) {
                return "0";
            } else {
                return gen.formatDouble5(coord);
            }
        }

        private void paintStrokedGlyphs(final PSGraphics2D g2d,
                final TextUtil textUtil, final Paint strokePaint,
                final Stroke stroke) throws IOException {
            final PSGenerator gen = textUtil.gen;

            applyColor(strokePaint, gen);
            PSGraphics2D.applyStroke(stroke, gen);

            Font f = null;
            final Iterator iter = this.relativePositions.iterator();
            iter.next();
            final Point2D pos = new Point2D.Double(0, 0);
            gen.writeln("0 0 M");
            for (int i = 0, c = this.currentChars.length(); i < c; i++) {
                final char ch = this.currentChars.charAt(0);
                if (i == 0) {
                    // Font only has to be setup up before the first character
                    f = textUtil.selectFontForChar(ch);
                }
                char mapped = f.mapChar(ch);
                if (i == 0) {
                    textUtil.selectFont(f, mapped);
                    textUtil.setCurrentFont(f, mapped);
                }
                // add glyph outlines to current path
                mapped = f.mapChar(this.currentChars.charAt(i));
                final FontMetrics metrics = f.getFontMetrics();
                final boolean multiByte = metrics instanceof MultiByteFont
                        || metrics instanceof LazyFont
                        && ((LazyFont) metrics).getRealFont() instanceof MultiByteFont;
                if (multiByte) {
                    gen.write('<');
                    gen.write(HexEncoder.encode(mapped));
                    gen.write('>');
                } else {
                    final char codepoint = (char) (mapped % 256);
                    gen.write("(" + codepoint + ")");
                }
                gen.writeln(" false charpath");

                if (iter.hasNext()) {
                    // Position for the next character
                    final Point2D pt = (Point2D) iter.next();
                    pos.setLocation(pos.getX() + pt.getX(),
                            pos.getY() - pt.getY());
                    gen.writeln(gen.formatDouble5(pos.getX()) + " "
                            + gen.formatDouble5(pos.getY()) + " M");
                }
            }
            gen.writeln("stroke"); // paints all accumulated glyph outlines
        }

    }

}
