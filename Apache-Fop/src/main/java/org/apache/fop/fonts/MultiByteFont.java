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

/* $Id: MultiByteFont.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.fonts;

import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.complexscripts.fonts.GlyphDefinitionTable;
import org.apache.fop.complexscripts.fonts.GlyphPositioningTable;
import org.apache.fop.complexscripts.fonts.GlyphSubstitutionTable;
import org.apache.fop.complexscripts.fonts.Positionable;
import org.apache.fop.complexscripts.fonts.Substitutable;
import org.apache.fop.complexscripts.util.GlyphSequence;
import org.apache.fop.util.CharUtilities;

/**
 * Generic MultiByte (CID) font
 */
@Slf4j
public class MultiByteFont extends CIDFont implements Substitutable,
Positionable {

    private String ttcName = null;
    private final String encoding = "Identity-H";

    private int defaultWidth = 0;
    private CIDFontType cidType = CIDFontType.CIDTYPE2;

    private final CIDSubset subset = new CIDSubset();

    /* advanced typographic support */
    private GlyphDefinitionTable gdef;
    private GlyphSubstitutionTable gsub;
    private GlyphPositioningTable gpos;

    /* dynamic private use (character) mappings */
    private int numMapped;
    private int numUnmapped;
    private int nextPrivateUse = 0xE000;
    private int firstPrivate;
    private int lastPrivate;
    private int firstUnmapped;
    private int lastUnmapped;

    /**
     * Default constructor
     */
    public MultiByteFont() {
        this.subset.setupFirstGlyph();
        setFontType(FontType.TYPE0);
    }

    /** {@inheritDoc} */
    @Override
    public int getDefaultWidth() {
        return this.defaultWidth;
    }

    /** {@inheritDoc} */
    @Override
    public String getRegistry() {
        return "Adobe";
    }

    /** {@inheritDoc} */
    @Override
    public String getOrdering() {
        return "UCS";
    }

    /** {@inheritDoc} */
    @Override
    public int getSupplement() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public CIDFontType getCIDType() {
        return this.cidType;
    }

    /**
     * Sets the CIDType.
     *
     * @param cidType
     *            The cidType to set
     */
    public void setCIDType(final CIDFontType cidType) {
        this.cidType = cidType;
    }

    /** {@inheritDoc} */
    @Override
    public String getEmbedFontName() {
        if (isEmbeddable()) {
            return FontUtil.stripWhiteSpace(super.getFontName());
        } else {
            return super.getFontName();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmbeddable() {
        return !(getEmbedFileName() == null && getEmbedResourceName() == null);
    }

    @Override
    public boolean isSubsetEmbedded() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public CIDSubset getCIDSubset() {
        return this.subset;
    }

    /** {@inheritDoc} */
    @Override
    public String getEncodingName() {
        return this.encoding;
    }

    /** {@inheritDoc} */
    @Override
    public int getWidth(final int i, final int size) {
        if (isEmbeddable()) {
            final int glyphIndex = this.subset.getGlyphIndexForSubsetIndex(i);
            return size * this.width[glyphIndex];
        } else {
            return size * this.width[i];
        }
    }

    /** {@inheritDoc} */
    @Override
    public int[] getWidths() {
        final int[] arr = new int[this.width.length];
        System.arraycopy(this.width, 0, arr, 0, this.width.length);
        return arr;
    }

    /**
     * Returns the glyph index for a Unicode character. The method returns 0 if
     * there's no such glyph in the character map.
     *
     * @param c
     *            the Unicode character index
     * @return the glyph index (or 0 if the glyph is not available)
     */
    // [TBD] - needs optimization, i.e., change from linear search to binary
    // search
    private int findGlyphIndex(final int c) {
        final int idx = c;
        int retIdx = SingleByteEncoding.NOT_FOUND_CODE_POINT;

        for (int i = 0; i < this.cmap.length && retIdx == 0; i++) {
            if (this.cmap[i].getUnicodeStart() <= idx
                    && this.cmap[i].getUnicodeEnd() >= idx) {

                retIdx = this.cmap[i].getGlyphStartIndex() + idx
                        - this.cmap[i].getUnicodeStart();
            }
        }
        return retIdx;
    }

    /**
     * Add a private use mapping {PU,GI} to the existing character map. N.B.
     * Does not insert in order, merely appends to end of existing map.
     */
    private synchronized void addPrivateUseMapping(final int pu, final int gi) {
        assert findGlyphIndex(pu) == SingleByteEncoding.NOT_FOUND_CODE_POINT;
        final CMapSegment[] oldCmap = this.cmap;
        final int cmapLength = oldCmap.length;
        final CMapSegment[] newCmap = new CMapSegment[cmapLength + 1];
        System.arraycopy(oldCmap, 0, newCmap, 0, cmapLength);
        newCmap[cmapLength] = new CMapSegment(pu, pu, gi);
        this.cmap = newCmap;
    }

    /**
     * Given a glyph index, create a new private use mapping, augmenting the
     * bfentries table. This is needed to accommodate the presence of an
     * (output) glyph index in a complex script glyph substitution that does not
     * correspond to a character in the font's CMAP. The creation of such
     * private use mappings is deferred until an attempt is actually made to
     * perform the reverse lookup from the glyph index. This is necessary in
     * order to avoid exhausting the private use space on fonts containing many
     * such non-mapped glyph indices, if these mappings had been created
     * statically at font load time.
     *
     * @param gi
     *            glyph index
     * @returns unicode scalar value
     */
    private int createPrivateUseMapping(final int gi) {
        while (this.nextPrivateUse < 0xF900
                && findGlyphIndex(this.nextPrivateUse) != SingleByteEncoding.NOT_FOUND_CODE_POINT) {
            this.nextPrivateUse++;
        }
        if (this.nextPrivateUse < 0xF900) {
            final int pu = this.nextPrivateUse;
            addPrivateUseMapping(pu, gi);
            if (this.firstPrivate == 0) {
                this.firstPrivate = pu;
            }
            this.lastPrivate = pu;
            this.numMapped++;
            if (log.isDebugEnabled()) {
                log.debug("Create private use mapping from "
                        + CharUtilities.format(pu) + " to glyph index " + gi
                        + " in font '" + getFullName() + "'");
            }
            return pu;
        } else {
            if (this.firstUnmapped == 0) {
                this.firstUnmapped = gi;
            }
            this.lastUnmapped = gi;
            this.numUnmapped++;
            log.warn("Exhausted private use area: unable to map "
                    + this.numUnmapped + " glyphs in glyph index range ["
                    + this.firstUnmapped + "," + this.lastUnmapped
                    + "] (inclusive) of font '" + getFullName() + "'");
            return 0;
        }
    }

    /**
     * Returns the Unicode scalar value that corresponds to the glyph index. If
     * more than one correspondence exists, then the first one is returned
     * (ordered by bfentries[]).
     *
     * @param gi
     *            glyph index
     * @returns unicode scalar value
     */
    // [TBD] - needs optimization, i.e., change from linear search to binary
    // search
    private int findCharacterFromGlyphIndex(final int gi, final boolean augment) {
        int cc = 0;
        for (final CMapSegment segment : this.cmap) {
            final int s = segment.getGlyphStartIndex();
            final int e = s + segment.getUnicodeEnd()
                    - segment.getUnicodeStart();
            if (gi >= s && gi <= e) {
                cc = segment.getUnicodeStart() + gi - s;
                break;
            }
        }
        if (cc == 0 && augment) {
            cc = createPrivateUseMapping(gi);
        }
        return cc;
    }

    private int findCharacterFromGlyphIndex(final int gi) {
        return findCharacterFromGlyphIndex(gi, true);
    }

    /** {@inheritDoc} */
    @Override
    public char mapChar(final char c) {
        notifyMapOperation();
        int glyphIndex = findGlyphIndex(c);
        if (glyphIndex == SingleByteEncoding.NOT_FOUND_CODE_POINT) {
            warnMissingGlyph(c);
            glyphIndex = findGlyphIndex(Typeface.NOT_FOUND);
        }
        if (isEmbeddable()) {
            glyphIndex = this.subset.mapSubsetChar(glyphIndex, c);
        }
        return (char) glyphIndex;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChar(final char c) {
        return findGlyphIndex(c) != SingleByteEncoding.NOT_FOUND_CODE_POINT;
    }

    /**
     * Sets the defaultWidth.
     *
     * @param defaultWidth
     *            The defaultWidth to set
     */
    public void setDefaultWidth(final int defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    /**
     * Returns the TrueType Collection Name.
     *
     * @return the TrueType Collection Name
     */
    public String getTTCName() {
        return this.ttcName;
    }

    /**
     * Sets the the TrueType Collection Name.
     *
     * @param ttcName
     *            the TrueType Collection Name
     */
    public void setTTCName(final String ttcName) {
        this.ttcName = ttcName;
    }

    /**
     * Sets the width array.
     *
     * @param wds
     *            array of widths.
     */
    public void setWidthArray(final int[] wds) {
        this.width = wds;
    }

    /**
     * Returns a Map of used Glyphs.
     *
     * @return Map Map of used Glyphs
     */
    public Map<Integer, Integer> getUsedGlyphs() {
        return this.subset.getSubsetGlyphs();
    }

    /** @return an array of the chars used */
    public char[] getCharsUsed() {
        if (!isEmbeddable()) {
            return null;
        }
        return this.subset.getSubsetChars();
    }

    /**
     * Establishes the glyph definition table.
     *
     * @param gdef
     *            the glyph definition table to be used by this font
     */
    public void setGDEF(final GlyphDefinitionTable gdef) {
        if (this.gdef == null || gdef == null) {
            this.gdef = gdef;
        } else {
            throw new IllegalStateException(
                    "font already associated with GDEF table");
        }
    }

    /**
     * Obtain glyph definition table.
     *
     * @return glyph definition table or null if none is associated with font
     */
    public GlyphDefinitionTable getGDEF() {
        return this.gdef;
    }

    /**
     * Establishes the glyph substitution table.
     *
     * @param gsub
     *            the glyph substitution table to be used by this font
     */
    public void setGSUB(final GlyphSubstitutionTable gsub) {
        if (this.gsub == null || gsub == null) {
            this.gsub = gsub;
        } else {
            throw new IllegalStateException(
                    "font already associated with GSUB table");
        }
    }

    /**
     * Obtain glyph substitution table.
     *
     * @return glyph substitution table or null if none is associated with font
     */
    public GlyphSubstitutionTable getGSUB() {
        return this.gsub;
    }

    /**
     * Establishes the glyph positioning table.
     *
     * @param gpos
     *            the glyph positioning table to be used by this font
     */
    public void setGPOS(final GlyphPositioningTable gpos) {
        if (this.gpos == null || gpos == null) {
            this.gpos = gpos;
        } else {
            throw new IllegalStateException(
                    "font already associated with GPOS table");
        }
    }

    /**
     * Obtain glyph positioning table.
     *
     * @return glyph positioning table or null if none is associated with font
     */
    public GlyphPositioningTable getGPOS() {
        return this.gpos;
    }

    /** {@inheritDoc} */
    @Override
    public boolean performsSubstitution() {
        return this.gsub != null;
    }

    /** {@inheritDoc} */
    @Override
    public CharSequence performSubstitution(final CharSequence cs,
            final String script, final String language) {
        if (this.gsub != null) {
            final GlyphSequence igs = mapCharsToGlyphs(cs);
            final GlyphSequence ogs = this.gsub.substitute(igs, script,
                    language);
            final CharSequence ocs = mapGlyphsToChars(ogs);
            return ocs;
        } else {
            return cs;
        }
    }

    /** {@inheritDoc} */
    @Override
    public CharSequence reorderCombiningMarks(final CharSequence cs,
            final int[][] gpa, final String script, final String language) {
        if (this.gdef != null) {
            final GlyphSequence igs = mapCharsToGlyphs(cs);
            final GlyphSequence ogs = this.gdef.reorderCombiningMarks(igs, gpa,
                    script, language);
            final CharSequence ocs = mapGlyphsToChars(ogs);
            return ocs;
        } else {
            return cs;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean performsPositioning() {
        return this.gpos != null;
    }

    /** {@inheritDoc} */
    @Override
    public int[][] performPositioning(final CharSequence cs,
            final String script, final String language, final int fontSize) {
        if (this.gpos != null) {
            final GlyphSequence gs = mapCharsToGlyphs(cs);
            final int[][] adjustments = new int[gs.getGlyphCount()][4];
            if (this.gpos.position(gs, script, language, fontSize, this.width,
                    adjustments)) {
                return scaleAdjustments(adjustments, fontSize);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int[][] performPositioning(final CharSequence cs,
            final String script, final String language) {
        throw new UnsupportedOperationException();
    }

    private int[][] scaleAdjustments(final int[][] adjustments,
            final int fontSize) {
        if (adjustments != null) {
            for (final int[] adjustment : adjustments) {
                final int[] gpa = adjustment;
                for (int k = 0; k < 4; k++) {
                    gpa[k] = gpa[k] * fontSize / 1000;
                }
            }
            return adjustments;
        } else {
            return null;
        }
    }

    /**
     * Map sequence CS, comprising a sequence of UTF-16 encoded Unicode Code
     * Points, to an output character sequence GS, comprising a sequence of
     * Glyph Indices. N.B. Unlike mapChar(), this method does not make use of
     * embedded subset encodings.
     *
     * @param cs
     *            a CharSequence containing UTF-16 encoded Unicode characters
     * @returns a CharSequence containing glyph indices
     */
    private GlyphSequence mapCharsToGlyphs(final CharSequence cs) {
        final IntBuffer cb = IntBuffer.allocate(cs.length());
        final IntBuffer gb = IntBuffer.allocate(cs.length());
        int gi;
        final int giMissing = findGlyphIndex(Typeface.NOT_FOUND);
        for (int i = 0, n = cs.length(); i < n; i++) {
            int cc = cs.charAt(i);
            if (cc >= 0xD800 && cc < 0xDC00) {
                if (i + 1 < n) {
                    final int sh = cc;
                    final int sl = cs.charAt(++i);
                    if (sl >= 0xDC00 && sl < 0xE000) {
                        cc = 0x10000 + (sh - 0xD800 << 10) + (sl - 0xDC00 << 0);
                    } else {
                        throw new IllegalArgumentException(
                                "ill-formed UTF-16 sequence, "
                                        + "contains isolated high surrogate at index "
                                        + i);
                    }
                } else {
                    throw new IllegalArgumentException(
                            "ill-formed UTF-16 sequence, "
                                    + "contains isolated high surrogate at end of sequence");
                }
            } else if (cc >= 0xDC00 && cc < 0xE000) {
                throw new IllegalArgumentException(
                        "ill-formed UTF-16 sequence, "
                                + "contains isolated low surrogate at index "
                                + i);
            }
            notifyMapOperation();
            gi = findGlyphIndex(cc);
            if (gi == SingleByteEncoding.NOT_FOUND_CODE_POINT) {
                warnMissingGlyph((char) cc);
                gi = giMissing;
            }
            cb.put(cc);
            gb.put(gi);
        }
        cb.flip();
        gb.flip();
        return new GlyphSequence(cb, gb, null);
    }

    /**
     * Map sequence GS, comprising a sequence of Glyph Indices, to output
     * sequence CS, comprising a sequence of UTF-16 encoded Unicode Code Points.
     *
     * @param gs
     *            a GlyphSequence containing glyph indices
     * @returns a CharSequence containing UTF-16 encoded Unicode characters
     */
    private CharSequence mapGlyphsToChars(final GlyphSequence gs) {
        final int ng = gs.getGlyphCount();
        final CharBuffer cb = CharBuffer.allocate(ng);
        final int ccMissing = Typeface.NOT_FOUND;
        for (int i = 0, n = ng; i < n; i++) {
            final int gi = gs.getGlyph(i);
            int cc = findCharacterFromGlyphIndex(gi);
            if (cc == 0 || cc > 0x10FFFF) {
                cc = ccMissing;
                log.warn("Unable to map glyph index " + gi
                        + " to Unicode scalar in font '" + getFullName()
                        + "', substituting missing character '" + (char) cc
                        + "'");
            }
            if (cc > 0x00FFFF) {
                int sh;
                int sl;
                cc -= 0x10000;
                sh = (cc >> 10 & 0x3FF) + 0xD800;
                sl = (cc >> 0 & 0x3FF) + 0xDC00;
                cb.put((char) sh);
                cb.put((char) sl);
            } else {
                cb.put((char) cc);
            }
        }
        cb.flip();
        return cb;
    }

}
