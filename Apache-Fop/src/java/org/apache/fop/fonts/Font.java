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

/* $Id: Font.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.fonts;

import java.util.Collections;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.complexscripts.fonts.Positionable;
import org.apache.fop.complexscripts.fonts.Substitutable;

// CSOFF: LineLengthCheck

/**
 * This class holds font state information and provides access to the font
 * metrics.
 */
@Slf4j
public class Font implements Substitutable, Positionable {

    /** Extra Bold font weight */
    public static final int WEIGHT_EXTRA_BOLD = 800;

    /** Bold font weight */
    public static final int WEIGHT_BOLD = 700;

    /** Normal font weight */
    public static final int WEIGHT_NORMAL = 400;

    /** Light font weight */
    public static final int WEIGHT_LIGHT = 200;

    /** Normal font style */
    public static final String STYLE_NORMAL = "normal";

    /** Italic font style */
    public static final String STYLE_ITALIC = "italic";

    /** Oblique font style */
    public static final String STYLE_OBLIQUE = "oblique";

    /** Inclined font style */
    public static final String STYLE_INCLINED = "inclined";

    /** Default selection priority */
    public static final int PRIORITY_DEFAULT = 0;

    /** Default fallback key */
    public static final FontTriplet DEFAULT_FONT = new FontTriplet("any",
            STYLE_NORMAL, WEIGHT_NORMAL, PRIORITY_DEFAULT);

    private final String fontName;
    private final FontTriplet triplet;
    private final int fontSize;

    /**
     * normal or small-caps font
     */
    // private int fontVariant;

    private final FontMetrics metric;

    /**
     * Main constructor
     *
     * @param key
     *            key of the font
     * @param triplet
     *            the font triplet that was used to lookup this font (may be
     *            null)
     * @param met
     *            font metrics
     * @param fontSize
     *            font size
     */
    public Font(final String key, final FontTriplet triplet,
            final FontMetrics met, final int fontSize) {
        this.fontName = key;
        this.triplet = triplet;
        this.metric = met;
        this.fontSize = fontSize;
    }

    /**
     * Returns the associated font metrics object.
     *
     * @return the font metrics
     */
    public FontMetrics getFontMetrics() {
        return this.metric;
    }

    /**
     * Returns the font's ascender.
     *
     * @return the ascender
     */
    public int getAscender() {
        return this.metric.getAscender(this.fontSize) / 1000;
    }

    /**
     * Returns the font's CapHeight.
     *
     * @return the capital height
     */
    public int getCapHeight() {
        return this.metric.getCapHeight(this.fontSize) / 1000;
    }

    /**
     * Returns the font's Descender.
     *
     * @return the descender
     */
    public int getDescender() {
        return this.metric.getDescender(this.fontSize) / 1000;
    }

    /**
     * Returns the font's name.
     *
     * @return the font name
     */
    public String getFontName() {
        return this.fontName;
    }

    /** @return the font triplet that selected this font */
    public FontTriplet getFontTriplet() {
        return this.triplet;
    }

    /**
     * Returns the font size
     *
     * @return the font size
     */
    public int getFontSize() {
        return this.fontSize;
    }

    /**
     * Returns the XHeight
     *
     * @return the XHeight
     */
    public int getXHeight() {
        return this.metric.getXHeight(this.fontSize) / 1000;
    }

    /** @return true if the font has kerning info */
    public boolean hasKerning() {
        return this.metric.hasKerningInfo();
    }

    /**
     * Returns the font's kerning table
     *
     * @return the kerning table
     */
    public Map<Integer, Map<Integer, Integer>> getKerning() {
        if (this.metric.hasKerningInfo()) {
            return this.metric.getKerningInfo();
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Returns the amount of kerning between two characters.
     *
     * The value returned measures in pt. So it is already adjusted for font
     * size.
     *
     * @param ch1
     *            first character
     * @param ch2
     *            second character
     * @return the distance to adjust for kerning, 0 if there's no kerning
     */
    public int getKernValue(final char ch1, final char ch2) {
        final Map<Integer, Integer> kernPair = getKerning().get((int) ch1);
        if (kernPair != null) {
            final Integer width = kernPair.get((int) ch2);
            if (width != null) {
                return width.intValue() * getFontSize() / 1000;
            }
        }
        return 0;
    }

    /**
     * Returns the amount of kerning between two characters.
     *
     * The value returned measures in pt. So it is already adjusted for font
     * size.
     *
     * @param ch1
     *            first character
     * @param ch2
     *            second character
     * @return the distance to adjust for kerning, 0 if there's no kerning
     */
    public int getKernValue(final int ch1, final int ch2) {
        // TODO !BMP
        if (ch1 > 0x10000) {
            return 0;
        } else if (ch1 >= 0xD800 && ch1 <= 0xE000) {
            return 0;
        } else if (ch2 > 0x10000) {
            return 0;
        } else if (ch2 >= 0xD800 && ch2 <= 0xE000) {
            return 0;
        } else {
            return getKernValue((char) ch1, (char) ch2);
        }
    }

    /**
     * Returns the width of a character
     *
     * @param charnum
     *            character to look up
     * @return width of the character
     */
    public int getWidth(final int charnum) {
        // returns width of given character number in millipoints
        return this.metric.getWidth(charnum, this.fontSize) / 1000;
    }

    /**
     * Map a java character (unicode) to a font character. Default uses
     * CodePointMapping.
     *
     * @param c
     *            character to map
     * @return the mapped character
     */
    public char mapChar(char c) {

        if (this.metric instanceof org.apache.fop.fonts.Typeface) {
            return ((org.apache.fop.fonts.Typeface) this.metric).mapChar(c);
        }

        // Use default CodePointMapping
        final char d = CodePointMapping.getMapping("WinAnsiEncoding")
                .mapChar(c);
        if (d != SingleByteEncoding.NOT_FOUND_CODE_POINT) {
            c = d;
        } else {
            log.warn("Glyph " + (int) c + " not available in font "
                    + this.fontName);
            c = Typeface.NOT_FOUND;
        }

        return c;
    }

    /**
     * Determines whether this font contains a particular character/glyph.
     *
     * @param c
     *            character to check
     * @return True if the character is supported, Falso otherwise
     */
    public boolean hasChar(final char c) {
        if (this.metric instanceof org.apache.fop.fonts.Typeface) {
            return ((org.apache.fop.fonts.Typeface) this.metric).hasChar(c);
        } else {
            // Use default CodePointMapping
            return CodePointMapping.getMapping("WinAnsiEncoding").mapChar(c) > 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuffer sbuf = new StringBuffer(super.toString());
        sbuf.append('{');
        /*
         * sbuf.append(fontFamily); sbuf.append(',');
         */
        sbuf.append(this.fontName);
        sbuf.append(',');
        sbuf.append(this.fontSize);
        /*
         * sbuf.append(','); sbuf.append(fontStyle); sbuf.append(',');
         * sbuf.append(fontWeight);
         */
        sbuf.append('}');
        return sbuf.toString();
    }

    /**
     * Helper method for getting the width of a unicode char from the current
     * fontstate. This also performs some guessing on widths on various versions
     * of space that might not exists in the font.
     *
     * @param c
     *            character to inspect
     * @return the width of the character or -1 if no width available
     */
    public int getCharWidth(final char c) {
        int width;

        if (c == '\n' || c == '\r' || c == '\t' || c == '\u00A0') {
            width = getCharWidth(' ');
        } else {
            if (hasChar(c)) {
                final int mappedChar = mapChar(c);
                width = getWidth(mappedChar);
            } else {
                width = -1;
            }
            if (width <= 0) {
                // Estimate the width of spaces not represented in
                // the font
                final int em = getFontSize(); // http://en.wikipedia.org/wiki/Em_(typography)
                final int en = em / 2; // http://en.wikipedia.org/wiki/En_(typography)

                if (c == ' ') {
                    width = em;
                } else if (c == '\u2000') {
                    width = en;
                } else if (c == '\u2001') {
                    width = em;
                } else if (c == '\u2002') {
                    width = em / 2;
                } else if (c == '\u2003') {
                    width = getFontSize();
                } else if (c == '\u2004') {
                    width = em / 3;
                } else if (c == '\u2005') {
                    width = em / 4;
                } else if (c == '\u2006') {
                    width = em / 6;
                } else if (c == '\u2007') {
                    width = getCharWidth('0');
                } else if (c == '\u2008') {
                    width = getCharWidth('.');
                } else if (c == '\u2009') {
                    width = em / 5;
                } else if (c == '\u200A') {
                    width = em / 10;
                } else if (c == '\u200B') {
                    width = 0;
                } else if (c == '\u202F') {
                    width = getCharWidth(' ') / 2;
                } else if (c == '\u2060') {
                    width = 0;
                } else if (c == '\u3000') {
                    width = getCharWidth(' ') * 2;
                } else if (c == '\ufeff') {
                    width = 0;
                } else {
                    // Will be internally replaced by "#" if not found
                    width = getWidth(mapChar(c));
                }
            }
        }

        return width;
    }

    /**
     * Helper method for getting the width of a unicode char from the current
     * fontstate. This also performs some guessing on widths on various versions
     * of space that might not exists in the font.
     *
     * @param c
     *            character to inspect
     * @return the width of the character or -1 if no width available
     */
    public int getCharWidth(final int c) {
        if (c < 0x10000) {
            return getCharWidth((char) c);
        } else {
            // TODO !BMP
            return -1;
        }
    }

    /**
     * Calculates the word width.
     *
     * @param word
     *            text to get width for
     * @return the width of the text
     */
    public int getWordWidth(final String word) {
        if (word == null) {
            return 0;
        }
        final int wordLength = word.length();
        int width = 0;
        final char[] characters = new char[wordLength];
        word.getChars(0, wordLength, characters, 0);
        for (int i = 0; i < wordLength; i++) {
            width += getCharWidth(characters[i]);
        }
        return width;
    }

    /** {@inheritDoc} */
    @Override
    public boolean performsSubstitution() {
        if (this.metric instanceof Substitutable) {
            final Substitutable s = (Substitutable) this.metric;
            return s.performsSubstitution();
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public CharSequence performSubstitution(final CharSequence cs,
            final String script, final String language) {
        if (this.metric instanceof Substitutable) {
            final Substitutable s = (Substitutable) this.metric;
            return s.performSubstitution(cs, script, language);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public CharSequence reorderCombiningMarks(final CharSequence cs,
            final int[][] gpa, final String script, final String language) {
        if (this.metric instanceof Substitutable) {
            final Substitutable s = (Substitutable) this.metric;
            return s.reorderCombiningMarks(cs, gpa, script, language);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean performsPositioning() {
        if (this.metric instanceof Positionable) {
            final Positionable p = (Positionable) this.metric;
            return p.performsPositioning();
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int[][] performPositioning(final CharSequence cs,
            final String script, final String language, final int fontSize) {
        if (this.metric instanceof Positionable) {
            final Positionable p = (Positionable) this.metric;
            return p.performPositioning(cs, script, language, fontSize);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int[][] performPositioning(final CharSequence cs,
            final String script, final String language) {
        return performPositioning(cs, script, language, this.fontSize);
    }

}
