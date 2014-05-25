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

/* $Id: PDFTextUtil.java 1244656 2012-02-15 19:17:55Z vhennebert $ */

package org.apache.fop.svg;

import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.Typeface;

/**
 * Utility class for generating PDF text objects. It needs to be subclassed to
 * add writing functionality (see {@link #write(String)}).
 */
public abstract class PDFTextUtil extends org.apache.fop.pdf.PDFTextUtil {

    private final FontInfo fontInfo;
    private Font[] fonts;
    private Font font;
    private int encoding;

    /**
     * Main constructor.
     *
     * @param fontInfo
     *            the font catalog
     */
    public PDFTextUtil(final FontInfo fontInfo) {
        super();
        this.fontInfo = fontInfo;
    }

    /** {@inheritDoc} */
    @Override
    protected void initValues() {
        super.initValues();
        this.font = null;
    }

    /**
     * Sets the current fonts for the text object. For every character, the
     * suitable font will be selected.
     *
     * @param fonts
     *            the new fonts
     */
    public void setFonts(final Font[] fonts) {
        this.fonts = fonts;
    }

    /**
     * Sets the current font for the text object.
     *
     * @param font
     *            the new font
     */
    public void setFont(final Font font) {
        setFonts(new Font[] { font });
    }

    /**
     * Returns the current font in use.
     *
     * @return the current font or null if no font is currently active.
     */
    public Font getCurrentFont() {
        return this.font;
    }

    /**
     * Returns the current encoding.
     *
     * @return the current encoding
     */
    public int getCurrentEncoding() {
        return this.encoding;
    }

    /**
     * Sets the current font.
     *
     * @param f
     *            the new font to use
     */
    public void setCurrentFont(final Font f) {
        this.font = f;
    }

    /**
     * Sets the current encoding.
     *
     * @param encoding
     *            the new encoding
     */
    public void setCurrentEncoding(final int encoding) {
        this.encoding = encoding;
    }

    /**
     * Determines whether the font with the given name is a multi-byte font.
     *
     * @param name
     *            the name of the font
     * @return true if it's a multi-byte font
     */
    protected boolean isMultiByteFont(final String name) {
        final Typeface f = this.fontInfo.getFonts().get(name);
        return f.isMultiByte();
    }

    /**
     * Writes a "Tf" command, setting a new current font.
     *
     * @param f
     *            the font to select
     */
    public void writeTf(final Font f) {
        final String fontName = f.getFontName();
        final float fontSize = f.getFontSize() / 1000f;
        final boolean isMultiByte = isMultiByteFont(fontName);
        if (!isMultiByte && this.encoding != 0) {
            updateTf(fontName + "_" + Integer.toString(this.encoding),
                    fontSize, isMultiByte);
        } else {
            updateTf(fontName, fontSize, isMultiByte);
        }
    }

    /**
     * Selects a font from the font list suitable to display the given
     * character.
     *
     * @param ch
     *            the character
     * @return the recommended Font to use
     */
    public Font selectFontForChar(final char ch) {
        for (final Font font2 : this.fonts) {
            if (font2.hasChar(ch)) {
                return font2;
            }
        }
        return this.fonts[0]; // TODO Maybe fall back to painting with shapes
    }

    /**
     * Writes a char to the "TJ-Buffer".
     *
     * @param ch
     *            the unmapped character
     */
    public void writeTJChar(final char ch) {
        final char mappedChar = this.font.mapChar(ch);
        writeTJMappedChar(mappedChar);
    }

}
