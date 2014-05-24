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

/* $Id: NativeTextHandler.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.ps;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontSetup;
import org.apache.xmlgraphics.java2d.ps.PSGraphics2D;
import org.apache.xmlgraphics.java2d.ps.PSTextHandler;
import org.apache.xmlgraphics.ps.PSGenerator;

/**
 * Specialized TextHandler implementation that the PSGraphics2D class delegates
 * to to paint text using PostScript text operations.
 */
public class NativeTextHandler implements PSTextHandler {

    private final PSGraphics2D rootG2D;

    /** FontInfo containing all available fonts */
    protected FontInfo fontInfo;

    /** Currently valid Font */
    protected Font font;

    /** Overriding FontState */
    protected Font overrideFont = null;

    /** the current (internal) font name */
    protected String currentFontName;

    /** the current font size in millipoints */
    protected int currentFontSize;

    /**
     * Main constructor.
     * 
     * @param g2d
     *            the PSGraphics2D instance this instances is used by
     * @param fontInfo
     *            the FontInfo object with all available fonts
     */
    public NativeTextHandler(final PSGraphics2D g2d, final FontInfo fontInfo) {
        this.rootG2D = g2d;
        if (fontInfo != null) {
            this.fontInfo = fontInfo;
        } else {
            setupFontInfo();
        }
    }

    private void setupFontInfo() {
        // Sets up a FontInfo with default fonts
        this.fontInfo = new FontInfo();
        final boolean base14Kerning = false;
        FontSetup.setup(this.fontInfo, base14Kerning);
    }

    /**
     * Return the font information associated with this object
     * 
     * @return the FontInfo object
     */
    public FontInfo getFontInfo() {
        return this.fontInfo;
    }

    private PSGenerator getPSGenerator() {
        return this.rootG2D.getPSGenerator();
    }

    /** {@inheritDoc} */
    @Override
    public void writeSetup() throws IOException {
        if (this.fontInfo != null) {
            PSFontUtils.writeFontDict(getPSGenerator(), this.fontInfo);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writePageSetup() throws IOException {
        // nop
    }

    /**
     * Draw a string to the PostScript document. The text is painted using text
     * operations. {@inheritDoc}
     */
    @Override
    public void drawString(final Graphics2D g, final String s, final float x,
            final float y) throws IOException {
        final PSGraphics2D g2d = (PSGraphics2D) g;
        g2d.preparePainting();
        if (this.overrideFont == null) {
            final java.awt.Font awtFont = g2d.getFont();
            this.font = createFont(awtFont);
        } else {
            this.font = this.overrideFont;
            this.overrideFont = null;
        }

        // Color and Font state
        g2d.establishColor(g2d.getColor());
        establishCurrentFont();

        final PSGenerator gen = getPSGenerator();
        gen.saveGraphicsState();

        // Clip
        final Shape imclip = g2d.getClip();
        g2d.writeClip(imclip);

        // Prepare correct transformation
        final AffineTransform trans = g2d.getTransform();
        gen.concatMatrix(trans);
        gen.writeln(gen.formatDouble(x) + " " + gen.formatDouble(y)
                + " moveto ");
        gen.writeln("1 -1 scale");

        final StringBuffer sb = new StringBuffer("(");
        escapeText(s, sb);
        sb.append(") t ");

        gen.writeln(sb.toString());

        gen.restoreGraphicsState();
    }

    private void escapeText(final String text, final StringBuffer target) {
        final int l = text.length();
        for (int i = 0; i < l; i++) {
            final char ch = text.charAt(i);
            final char mch = this.font.mapChar(ch);
            PSGenerator.escapeChar(mch, target);
        }
    }

    private Font createFont(final java.awt.Font f) {
        return this.fontInfo.getFontInstanceForAWTFont(f);
    }

    private void establishCurrentFont() throws IOException {
        if (this.currentFontName != this.font.getFontName()
                || this.currentFontSize != this.font.getFontSize()) {
            final PSGenerator gen = getPSGenerator();
            gen.writeln("/" + this.font.getFontTriplet().getName() + " "
                    + gen.formatDouble(this.font.getFontSize() / 1000f) + " F");
            this.currentFontName = this.font.getFontName();
            this.currentFontSize = this.font.getFontSize();
        }
    }

    /**
     * Sets the overriding font.
     * 
     * @param override
     *            Overriding Font to set
     */
    public void setOverrideFont(final Font override) {
        this.overrideFont = override;
    }

}
