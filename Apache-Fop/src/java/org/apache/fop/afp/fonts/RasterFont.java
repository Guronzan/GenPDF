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

/* $Id: RasterFont.java 1311638 2012-04-10 08:39:31Z mehdi $ */

package org.apache.fop.afp.fonts;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import lombok.extern.slf4j.Slf4j;

/**
 * A font where each character is stored as an array of pixels (a bitmap). Such
 * fonts are not easily scalable, in contrast to vectored fonts. With this type
 * of font, the font metrics information is held in character set files (one for
 * each size and style).
 * <p/>
 *
 */
@Slf4j
public class RasterFont extends AFPFont {

    private final SortedMap<Integer, CharacterSet> charSets = new TreeMap<Integer, CharacterSet>();
    private Map<Integer, CharacterSet> substitutionCharSets;

    private CharacterSet charSet = null;

    /**
     * Constructor for the raster font requires the name, weight and style
     * attribute to be available as this forms the key to the font.
     *
     * @param name
     *            the name of the font
     */
    public RasterFont(final String name) {
        super(name);
    }

    /**
     * Adds the character set for the given point size
     *
     * @param size
     *            point size (in mpt)
     * @param characterSet
     *            character set
     */
    public void addCharacterSet(final int size, final CharacterSet characterSet) {
        // TODO: replace with Integer.valueOf() once we switch to Java 5
        this.charSets.put(new Integer(size), characterSet);
        this.charSet = characterSet;
    }

    /**
     * Get the character set metrics for the specified point size.
     *
     * @param sizeInMpt
     *            the point size (in mpt)
     * @return the character set metrics
     */
    @Override
    public CharacterSet getCharacterSet(final int sizeInMpt) {

        final Integer requestedSize = Integer.valueOf(sizeInMpt);
        CharacterSet csm = this.charSets.get(requestedSize);
        final double sizeInPt = sizeInMpt / 1000.0;

        if (csm != null) {
            return csm;
        }

        if (this.substitutionCharSets != null) {
            // Check first if a substitution has already been added
            csm = this.substitutionCharSets.get(requestedSize);
        }

        if (csm == null && !this.charSets.isEmpty()) {
            // No match or substitution found, but there exist entries
            // for other sizes
            // Get char set with nearest, smallest font size
            final SortedMap<Integer, CharacterSet> smallerSizes = this.charSets
                    .headMap(requestedSize);
            final SortedMap<Integer, CharacterSet> largerSizes = this.charSets
                    .tailMap(requestedSize);
            final int smallerSize = smallerSizes.isEmpty() ? 0 : smallerSizes
                    .lastKey().intValue();
            final int largerSize = largerSizes.isEmpty() ? Integer.MAX_VALUE
                    : largerSizes.firstKey().intValue();

            Integer fontSize;
            if (!smallerSizes.isEmpty()
                    && sizeInMpt - smallerSize <= largerSize - sizeInMpt) {
                fontSize = Integer.valueOf(smallerSize);
            } else {
                fontSize = Integer.valueOf(largerSize);
            }
            csm = this.charSets.get(fontSize);

            if (csm != null) {
                // Add the substitute mapping, so subsequent calls will
                // find it immediately
                if (this.substitutionCharSets == null) {
                    this.substitutionCharSets = new HashMap<Integer, CharacterSet>();
                }
                this.substitutionCharSets.put(requestedSize, csm);
                // do not output the warning if the font size is closer to an
                // integer less than 0.1
                if (!(Math.abs(fontSize.intValue() / 1000.0 - sizeInPt) < 0.1)) {
                    final String msg = "No " + sizeInPt + "pt font "
                            + getFontName() + " found, substituted with "
                            + fontSize.intValue() / 1000f + "pt font";
                    log.warn(msg);
                }
            }
        }

        if (csm == null) {
            // Still no match -> error
            final String msg = "No font found for font " + getFontName()
                    + " with point size " + sizeInPt;
            log.error(msg);
            throw new FontRuntimeException(msg);
        }

        return csm;

    }

    /**
     * Get the first character in this font.
     *
     * @return the first character in this font.
     */
    public int getFirstChar() {
        final Iterator<CharacterSet> it = this.charSets.values().iterator();
        if (it.hasNext()) {
            final CharacterSet csm = it.next();
            return csm.getFirstChar();
        } else {
            final String msg = "getFirstChar() - No character set found for font:"
                    + getFontName();
            log.error(msg);
            throw new FontRuntimeException(msg);
        }
    }

    /**
     * Get the last character in this font.
     *
     * @return the last character in this font.
     */
    public int getLastChar() {

        final Iterator<CharacterSet> it = this.charSets.values().iterator();
        if (it.hasNext()) {
            final CharacterSet csm = it.next();
            return csm.getLastChar();
        } else {
            final String msg = "getLastChar() - No character set found for font:"
                    + getFontName();
            log.error(msg);
            throw new FontRuntimeException(msg);
        }

    }

    private int metricsToAbsoluteSize(final CharacterSet cs, final int value,
            final int givenSize) {
        final int nominalVerticalSize = cs.getNominalVerticalSize();
        if (nominalVerticalSize != 0) {
            return value * nominalVerticalSize;
        } else {
            return value * givenSize;
        }
    }

    /**
     * The ascender is the part of a lowercase letter that extends above the
     * "x-height" (the height of the letter "x"), such as "d", "t", or "h". Also
     * used to denote the part of the letter extending above the x-height.
     *
     * @param size
     *            the font size (in mpt)
     * @return the ascender for the given point size
     */
    @Override
    public int getAscender(final int size) {
        final CharacterSet cs = getCharacterSet(size);
        return metricsToAbsoluteSize(cs, cs.getAscender(), size);
    }

    /**
     * Obtains the height of capital letters for the specified point size.
     *
     * @param size
     *            the font size (in mpt)
     * @return the cap height for the specified point size
     */
    @Override
    public int getCapHeight(final int size) {
        final CharacterSet cs = getCharacterSet(size);
        return metricsToAbsoluteSize(cs, cs.getCapHeight(), size);
    }

    /**
     * The descender is the part of a lowercase letter that extends below the
     * base line, such as "g", "j", or "p". Also used to denote the part of the
     * letter extending below the base line.
     *
     * @param size
     *            the font size (in mpt)
     * @return the descender for the specified point size
     */
    @Override
    public int getDescender(final int size) {
        final CharacterSet cs = getCharacterSet(size);
        return metricsToAbsoluteSize(cs, cs.getDescender(), size);
    }

    /**
     * The "x-height" (the height of the letter "x").
     *
     * @param size
     *            the font size (in mpt)
     * @return the x height for the given point size
     */
    @Override
    public int getXHeight(final int size) {
        final CharacterSet cs = getCharacterSet(size);
        return metricsToAbsoluteSize(cs, cs.getXHeight(), size);
    }

    /**
     * Obtain the width of the character for the specified point size.
     *
     * @param character
     *            the character
     * @param size
     *            the font size (in mpt)
     * @return the width for the given point size
     */
    @Override
    public int getWidth(final int character, final int size) {
        final CharacterSet cs = getCharacterSet(size);
        return metricsToAbsoluteSize(cs,
                cs.getWidth(toUnicodeCodepoint(character)), size);
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
        final CharacterSet cs = getCharacterSet(size);
        final int[] widths = cs.getWidths();
        for (int i = 0, c = widths.length; i < c; i++) {
            widths[i] = metricsToAbsoluteSize(cs, widths[i], size);
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
