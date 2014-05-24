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

/* $Id: GlyfTable.java 1155024 2011-08-08 17:34:46Z vhennebert $ */

package org.apache.fop.fonts.truetype;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This "glyf" table in a TrueType font file contains information that describes
 * the glyphs. This class is responsible for creating a subset of the "glyf"
 * table given a set of glyph indices.
 */
public class GlyfTable {

    private final TTFMtxEntry[] mtxTab;

    private final long tableOffset;

    private final Set<Long> remappedComposites;

    private final Map<Integer, Integer> subset;

    private final FontFileReader in;

    /** All the composite glyphs that appear in the subset. */
    private final Set<Integer> compositeGlyphs = new TreeSet<Integer>();

    /** All the glyphs that are composed, but do not appear in the subset. */
    private final Set<Integer> composedGlyphs = new TreeSet<Integer>();

    GlyfTable(final FontFileReader in, final TTFMtxEntry[] metrics,
            final TTFDirTabEntry dirTableEntry,
            final Map<Integer, Integer> glyphs) {
        this.mtxTab = metrics;
        this.tableOffset = dirTableEntry.getOffset();
        this.remappedComposites = new HashSet<Long>();
        this.subset = glyphs;
        this.in = in;
    }

    private static enum GlyfFlags {

        ARG_1_AND_2_ARE_WORDS(4, 2), ARGS_ARE_XY_VALUES, ROUND_XY_TO_GRID, WE_HAVE_A_SCALE(
                2), RESERVED, MORE_COMPONENTS, WE_HAVE_AN_X_AND_Y_SCALE(4), WE_HAVE_A_TWO_BY_TWO(
                        8), WE_HAVE_INSTRUCTIONS, USE_MY_METRICS, OVERLAP_COMPOUND, SCALED_COMPONENT_OFFSET, UNSCALED_COMPONENT_OFFSET;

        private final int bitMask;
        private final int argsCountIfSet;
        private final int argsCountIfNotSet;

        private GlyfFlags(final int argsCountIfSet, final int argsCountIfNotSet) {
            this.bitMask = 1 << ordinal();
            this.argsCountIfSet = argsCountIfSet;
            this.argsCountIfNotSet = argsCountIfNotSet;
        }

        private GlyfFlags(final int argsCountIfSet) {
            this(argsCountIfSet, 0);
        }

        private GlyfFlags() {
            this(0, 0);
        }

        /**
         * Calculates, from the given flags, the offset to the next glyph index.
         *
         * @param flags
         *            the glyph data flags
         * @return offset to the next glyph if any, or 0
         */
        static int getOffsetToNextComposedGlyf(final int flags) {
            int offset = 0;
            for (final GlyfFlags flag : GlyfFlags.values()) {
                offset += (flags & flag.bitMask) > 0 ? flag.argsCountIfSet
                        : flag.argsCountIfNotSet;
            }
            return offset;
        }

        /**
         * Checks the given flags to see if there is another composed glyph.
         *
         * @param flags
         *            the glyph data flags
         * @return true if there is another composed glyph, otherwise false.
         */
        static boolean hasMoreComposites(final int flags) {
            return (flags & MORE_COMPONENTS.bitMask) > 0;
        }
    }

    /**
     * Populates the map of subset glyphs with all the glyphs that compose the
     * glyphs in the subset. This also re-maps the indices of composed glyphs to
     * their new index in the subset font.
     *
     * @throws IOException
     *             an I/O error
     */
    void populateGlyphsWithComposites() throws IOException {
        for (final int indexInOriginal : this.subset.keySet()) {
            scanGlyphsRecursively(indexInOriginal);
        }

        addAllComposedGlyphsToSubset();

        for (final int compositeGlyph : this.compositeGlyphs) {
            final long offset = this.tableOffset
                    + this.mtxTab[compositeGlyph].getOffset() + 10;
            if (!this.remappedComposites.contains(offset)) {
                remapComposite(offset);
            }
        }
    }

    /**
     * Scans each glyph for any composed glyphs. This populates
     * <code>compositeGlyphs</code> with all the composite glyphs being used in
     * the subset. This also populates <code>newGlyphs</code> with any new
     * glyphs that are composed and do not appear in the subset of glyphs.
     *
     * For example the double quote mark (") is often composed of two
     * apostrophes ('), if an apostrophe doesn't appear in the glyphs in the
     * subset, it will be included and will be added to newGlyphs.
     *
     * @param indexInOriginal
     *            the index of the glyph to test from the original font
     * @throws IOException
     *             an I/O error
     */
    private void scanGlyphsRecursively(final int indexInOriginal)
            throws IOException {
        if (!this.subset.containsKey(indexInOriginal)) {
            this.composedGlyphs.add(indexInOriginal);
        }

        if (isComposite(indexInOriginal)) {
            this.compositeGlyphs.add(indexInOriginal);
            final Set<Integer> composedGlyphs = retrieveComposedGlyphs(indexInOriginal);
            for (final Integer composedGlyph : composedGlyphs) {
                scanGlyphsRecursively(composedGlyph);
            }
        }
    }

    /**
     * Adds to the subset, all the glyphs that are composed by a glyph, but do
     * not appear themselves in the subset.
     */
    private void addAllComposedGlyphsToSubset() {
        int newIndex = this.subset.size();
        for (final int composedGlyph : this.composedGlyphs) {
            this.subset.put(composedGlyph, newIndex++);
        }
    }

    /**
     * Re-maps the index of composed glyphs in the original font to the index of
     * the same glyph in the subset font.
     *
     * @param glyphOffset
     *            the offset of the composite glyph
     * @throws IOException
     *             an I/O error
     */
    private void remapComposite(final long glyphOffset) throws IOException {
        long currentGlyphOffset = glyphOffset;

        this.remappedComposites.add(currentGlyphOffset);

        int flags = 0;
        do {
            flags = this.in.readTTFUShort(currentGlyphOffset);
            final int glyphIndex = this.in
                    .readTTFUShort(currentGlyphOffset + 2);
            final Integer indexInSubset = this.subset.get(glyphIndex);
            assert indexInSubset != null;
            /*
             * TODO: this should not be done here!! We're writing to the stream
             * we're reading from, this is asking for trouble! What should
             * happen is when the glyph data is copied from subset, the
             * remapping should be done there. So the original stream is left
             * untouched.
             */
            this.in.writeTTFUShort(currentGlyphOffset + 2, indexInSubset);

            currentGlyphOffset += 4 + GlyfFlags
                    .getOffsetToNextComposedGlyf(flags);
        } while (GlyfFlags.hasMoreComposites(flags));
    }

    private boolean isComposite(final int indexInOriginal) throws IOException {
        final int numberOfContours = this.in.readTTFShort(this.tableOffset
                + this.mtxTab[indexInOriginal].getOffset());
        return numberOfContours < 0;
    }

    /**
     * Reads a composite glyph at a given index and retrieves all the glyph
     * indices of contingent composed glyphs.
     *
     * @param indexInOriginal
     *            the glyph index of the composite glyph
     * @return the set of glyph indices this glyph composes
     * @throws IOException
     *             an I/O error
     */
    private Set<Integer> retrieveComposedGlyphs(final int indexInOriginal)
            throws IOException {
        final Set<Integer> composedGlyphs = new HashSet<Integer>();
        long offset = this.tableOffset
                + this.mtxTab[indexInOriginal].getOffset() + 10;
        int flags = 0;
        do {
            flags = this.in.readTTFUShort(offset);
            composedGlyphs.add(this.in.readTTFUShort(offset + 2));
            offset += 4 + GlyfFlags.getOffsetToNextComposedGlyf(flags);
        } while (GlyfFlags.hasMoreComposites(flags));

        return composedGlyphs;
    }
}
