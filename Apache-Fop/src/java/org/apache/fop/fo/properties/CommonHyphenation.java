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

/* $Id: CommonHyphenation.java 1304264 2012-03-23 10:26:13Z vhennebert $ */

package org.apache.fop.fo.properties;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.fonts.Typeface;

/**
 * Store all common hyphenation properties. See Sec. 7.9 of the XSL-FO Standard.
 * Public "structure" allows direct member access.
 */
@Slf4j
public final class CommonHyphenation {

    private static final PropertyCache<CommonHyphenation> CACHE = new PropertyCache<CommonHyphenation>();

    private int hash = 0;

    /** The "language" property */
    public final StringProperty language; // CSOK: VisibilityModifier

    /** The "country" property */
    public final StringProperty country; // CSOK: VisibilityModifier

    /** The "script" property */
    public final StringProperty script; // CSOK: VisibilityModifier

    /** The "hyphenate" property */
    public final EnumProperty hyphenate; // CSOK: VisibilityModifier

    /** The "hyphenation-character" property */
    public final CharacterProperty hyphenationCharacter; // CSOK:
    // VisibilityModifier

    /** The "hyphenation-push-character-count" property */
    public final NumberProperty hyphenationPushCharacterCount; // CSOK:
    // VisibilityModifier

    /** The "hyphenation-remain-character-count" property */
    public final NumberProperty hyphenationRemainCharacterCount; // CSOK:
    // VisibilityModifier

    /**
     * Construct a CommonHyphenation object holding the given properties
     *
     */
    private CommonHyphenation(final StringProperty language,
            final StringProperty country, final StringProperty script,
            final EnumProperty hyphenate,
            final CharacterProperty hyphenationCharacter,
            final NumberProperty hyphenationPushCharacterCount,
            final NumberProperty hyphenationRemainCharacterCount) {
        this.language = language;
        this.country = country;
        this.script = script;
        this.hyphenate = hyphenate;
        this.hyphenationCharacter = hyphenationCharacter;
        this.hyphenationPushCharacterCount = hyphenationPushCharacterCount;
        this.hyphenationRemainCharacterCount = hyphenationRemainCharacterCount;
    }

    /**
     * Gets the canonical <code>CommonHyphenation</code> instance corresponding
     * to the values of the related properties present on the given
     * <code>PropertyList</code>
     *
     * @param propertyList
     *            the <code>PropertyList</code>
     * @return a common hyphenation instance
     * @throws PropertyException
     *             if a a property exception occurs
     */
    public static CommonHyphenation getInstance(final PropertyList propertyList)
            throws PropertyException {
        final StringProperty language = (StringProperty) propertyList
                .get(Constants.PR_LANGUAGE);
        final StringProperty country = (StringProperty) propertyList
                .get(Constants.PR_COUNTRY);
        final StringProperty script = (StringProperty) propertyList
                .get(Constants.PR_SCRIPT);
        final EnumProperty hyphenate = (EnumProperty) propertyList
                .get(Constants.PR_HYPHENATE);
        final CharacterProperty hyphenationCharacter = (CharacterProperty) propertyList
                .get(Constants.PR_HYPHENATION_CHARACTER);
        final NumberProperty hyphenationPushCharacterCount = (NumberProperty) propertyList
                .get(Constants.PR_HYPHENATION_PUSH_CHARACTER_COUNT);
        final NumberProperty hyphenationRemainCharacterCount = (NumberProperty) propertyList
                .get(Constants.PR_HYPHENATION_REMAIN_CHARACTER_COUNT);

        final CommonHyphenation instance = new CommonHyphenation(language,
                country, script, hyphenate, hyphenationCharacter,
                hyphenationPushCharacterCount, hyphenationRemainCharacterCount);

        return CACHE.fetch(instance);
    }

    private static final char HYPHEN_MINUS = '-';
    private static final char MINUS_SIGN = '\u2212';

    /**
     * Returns the effective hyphenation character for a font. The hyphenation
     * character specified in XSL-FO may be substituted if it's not available in
     * the font.
     *
     * @param font
     *            the font
     * @return the effective hyphenation character.
     */
    public char getHyphChar(final org.apache.fop.fonts.Font font) {
        final char hyphChar = this.hyphenationCharacter.getCharacter();
        if (font.hasChar(hyphChar)) {
            return hyphChar; // short-cut
        }
        char effHyphChar = hyphChar;
        boolean warn = false;
        if (font.hasChar(HYPHEN_MINUS)) {
            effHyphChar = HYPHEN_MINUS;
            warn = true;
        } else if (font.hasChar(MINUS_SIGN)) {
            effHyphChar = MINUS_SIGN;
            final FontMetrics metrics = font.getFontMetrics();
            if (metrics instanceof Typeface) {
                final Typeface typeface = (Typeface) metrics;
                if ("SymbolEncoding".equals(typeface.getEncodingName())) {
                    // SymbolEncoding doesn't have HYPHEN_MINUS, so replace by
                    // MINUS_SIGN
                } else {
                    // only warn if the encoding is not SymbolEncoding
                    warn = true;
                }
            }
        } else {
            effHyphChar = ' ';
            final FontMetrics metrics = font.getFontMetrics();
            if (metrics instanceof Typeface) {
                final Typeface typeface = (Typeface) metrics;
                if ("ZapfDingbatsEncoding".equals(typeface.getEncodingName())) {
                    // ZapfDingbatsEncoding doesn't have HYPHEN_MINUS, so
                    // replace by ' '
                } else {
                    // only warn if the encoding is not ZapfDingbatsEncoding
                    warn = true;
                }
            }
        }
        if (warn) {
            log.warn("Substituted specified hyphenation character (0x"
                    + Integer.toHexString(hyphChar)
                    + ") with 0x"
                    + Integer.toHexString(effHyphChar)
                    + " because the font doesn't have the specified hyphenation character: "
                    + font.getFontTriplet());
        }
        return effHyphChar;
    }

    /**
     * Returns the IPD for the hyphenation character for a font.
     *
     * @param font
     *            the font
     * @return the IPD in millipoints for the hyphenation character.
     */
    public int getHyphIPD(final org.apache.fop.fonts.Font font) {
        final char hyphChar = getHyphChar(font);
        return font.getCharWidth(hyphChar);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CommonHyphenation) {
            final CommonHyphenation ch = (CommonHyphenation) obj;
            return ch.language == this.language
                    && ch.country == this.country
                    && ch.script == this.script
                    && ch.hyphenate == this.hyphenate
                    && ch.hyphenationCharacter == this.hyphenationCharacter
                    && ch.hyphenationPushCharacterCount == this.hyphenationPushCharacterCount
                    && ch.hyphenationRemainCharacterCount == this.hyphenationRemainCharacterCount;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        if (this.hash == 0) {
            int hash = 17;
            hash = 37 * hash
                    + (this.language == null ? 0 : this.language.hashCode());
            hash = 37 * hash
                    + (this.script == null ? 0 : this.script.hashCode());
            hash = 37 * hash
                    + (this.country == null ? 0 : this.country.hashCode());
            hash = 37 * hash
                    + (this.hyphenate == null ? 0 : this.hyphenate.hashCode());
            hash = 37
                    * hash
                    + (this.hyphenationCharacter == null ? 0
                            : this.hyphenationCharacter.hashCode());
            hash = 37
                    * hash
                    + (this.hyphenationPushCharacterCount == null ? 0
                            : this.hyphenationPushCharacterCount.hashCode());
            hash = 37
                    * hash
                    + (this.hyphenationRemainCharacterCount == null ? 0
                            : this.hyphenationRemainCharacterCount.hashCode());
            this.hash = hash;
        }
        return this.hash;
    }

}
