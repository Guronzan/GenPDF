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

/* $Id: Typeface.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.fonts;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.fonts.Glyphs;

/**
 * Base class for font classes
 */
@Slf4j
public abstract class Typeface implements FontMetrics {

    /**
     * Code point that is used if no code point for a specific character has
     * been found.
     */
    public static final char NOT_FOUND = '#';

    /**
     * Used to identify whether a font has been used (a character map operation
     * is used as the trigger). This could just as well be a boolean but is a
     * long out of statistical interest.
     */
    private long charMapOps = 0;

    /**
     * An optional event listener that receives events such as missing glyphs
     * etc.
     */
    protected FontEventListener eventListener;

    private Set<Character> warnedChars;

    /**
     * Get the encoding of the font.
     *
     * @return the encoding
     */
    public abstract String getEncodingName();

    /**
     * Map a Unicode character to a code point in the font.
     *
     * @param c
     *            character to map
     * @return the mapped character
     */
    public abstract char mapChar(final char c);

    /**
     * Used for keeping track of character mapping operations in order to
     * determine if a font was used at all or not.
     */
    protected void notifyMapOperation() {
        this.charMapOps++;
    }

    /**
     * Indicates whether this font had to do any character mapping operations.
     * If that was not the case, it's an indication that the font has never
     * actually been used.
     *
     * @return true if the font had to do any character mapping operations
     */
    public boolean hadMappingOperations() {
        return this.charMapOps > 0;
    }

    /**
     * Determines whether this font contains a particular character/glyph.
     *
     * @param c
     *            character to check
     * @return True if the character is supported, Falso otherwise
     */
    public abstract boolean hasChar(final char c);

    /**
     * Determines whether the font is a multibyte font.
     *
     * @return True if it is multibyte
     */
    public boolean isMultiByte() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxAscent(final int size) {
        return getAscender(size);
    }

    /**
     * Sets the font event listener that can be used to receive events about
     * particular events in this class.
     *
     * @param listener
     *            the font event listener
     */
    public void setEventListener(final FontEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * Provide proper warning if a glyph is not available.
     *
     * @param c
     *            the character which is missing.
     */
    protected void warnMissingGlyph(final char c) {
        // Give up, character is not available
        final Character ch = new Character(c);
        if (this.warnedChars == null) {
            this.warnedChars = new HashSet<Character>();
        }
        if (this.warnedChars.size() < 8 && !this.warnedChars.contains(ch)) {
            this.warnedChars.add(ch);
            if (this.eventListener != null) {
                this.eventListener.glyphNotAvailable(this, c, getFontName());
            } else {
                if (this.warnedChars.size() == 8) {
                    log.warn("Many requested glyphs are not available in font "
                            + getFontName());
                } else {
                    log.warn("Glyph " + (int) c + " (0x"
                            + Integer.toHexString(c) + ", "
                            + Glyphs.charToGlyphName(c)
                            + ") not available in font " + getFontName());
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sbuf = new StringBuffer(super.toString());
        sbuf.append('{');
        sbuf.append(getFullName());
        sbuf.append('}');
        return sbuf.toString();
    }
}
