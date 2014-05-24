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

/* $Id: AbstractOutlineFont.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.afp.fonts;

/**
 * A font defined as a set of lines and curves as opposed to a bitmap font. An
 * outline font can be scaled to any size and otherwise transformed more easily
 * than a bitmap font, and with more attractive results.
 */
public abstract class AbstractOutlineFont extends AFPFont {

    /** The character set for this font */
    protected CharacterSet charSet = null;

    /**
     * Constructor for an outline font.
     *
     * @param name
     *            the name of the font
     * @param charSet
     *            the chracter set
     */
    public AbstractOutlineFont(final String name, final CharacterSet charSet) {
        super(name);
        this.charSet = charSet;
    }

    /**
     * Get the character set metrics.
     *
     * @return the character set
     */
    public CharacterSet getCharacterSet() {
        return this.charSet;
    }

    /**
     * Get the character set metrics.
     * 
     * @param size
     *            ignored
     * @return the character set
     */
    @Override
    public CharacterSet getCharacterSet(final int size) {
        return this.charSet;
    }

    /**
     * Get the first character in this font.
     * 
     * @return the first character in this font
     */
    public int getFirstChar() {
        return this.charSet.getFirstChar();
    }

    /**
     * Get the last character in this font.
     * 
     * @return the last character in this font
     */
    public int getLastChar() {
        return this.charSet.getLastChar();
    }

    /**
     * The ascender is the part of a lowercase letter that extends above the
     * "x-height" (the height of the letter "x"), such as "d", "t", or "h". Also
     * used to denote the part of the letter extending above the x-height.
     *
     * @param size
     *            the font size (in mpt)
     * @return the ascender for the given size
     */
    @Override
    public int getAscender(final int size) {
        return this.charSet.getAscender() * size;
    }

    /**
     * Obtains the height of capital letters for the specified point size.
     *
     * @param size
     *            the font size (in mpt)
     * @return the cap height for the given size
     */
    @Override
    public int getCapHeight(final int size) {
        return this.charSet.getCapHeight() * size;
    }

    /**
     * The descender is the part of a lowercase letter that extends below the
     * base line, such as "g", "j", or "p". Also used to denote the part of the
     * letter extending below the base line.
     *
     * @param size
     *            the font size (in mpt)
     * @return the descender for the given size
     */
    @Override
    public int getDescender(final int size) {
        return this.charSet.getDescender() * size;
    }

    /**
     * The "x-height" (the height of the letter "x").
     *
     * @param size
     *            the font size (in mpt)
     * @return the x height for the given size
     */
    @Override
    public int getXHeight(final int size) {
        return this.charSet.getXHeight() * size;
    }

    /**
     * Obtain the width of the character for the specified point size.
     * 
     * @param character
     *            the character
     * @param size
     *            the font size (in mpt)
     * @return the width of the character for the specified point size
     */
    @Override
    public int getWidth(final int character, final int size) {
        return this.charSet.getWidth(toUnicodeCodepoint(character)) * size;
    }

    /**
     * Get the getWidth (in 1/1000ths of a point size) of all characters in this
     * character set.
     *
     * @param size
     *            the font size (in mpt)
     * @return the widths of all characters
     */
    public int[] getWidths(final int size) {
        final int[] widths = this.charSet.getWidths();
        for (int i = 0; i < widths.length; i++) {
            widths[i] = widths[i] * size;
        }
        return widths;
    }

    /**
     * Get the getWidth (in 1/1000ths of a point size) of all characters in this
     * character set.
     *
     * @return the widths of all characters
     */
    @Override
    public int[] getWidths() {
        return getWidths(1000);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChar(final char c) {
        return this.charSet.hasChar(c);
    }

    /**
     * Map a Unicode character to a code point in the font.
     * 
     * @param c
     *            character to map
     * @return the mapped character
     */
    @Override
    public char mapChar(final char c) {
        return this.charSet.mapChar(c);
    }

    /** {@inheritDoc} */
    @Override
    public String getEncodingName() {
        return this.charSet.getEncoding();
    }

}
