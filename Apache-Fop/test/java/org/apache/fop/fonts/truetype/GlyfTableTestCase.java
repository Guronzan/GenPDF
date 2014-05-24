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

/* $Id: GlyfTableTestCase.java 1352986 2012-06-22 18:07:04Z vhennebert $ */

package org.apache.fop.fonts.truetype;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests {@link GlyfTable}.
 */
public class GlyfTableTestCase {

    private final static class DirData {

        final long offset;
        final long length;

        DirData(final long offset, final long length) {
            this.offset = offset;
            this.length = length;
        }
    }

    private FontFileReader subsetReader;

    private long[] glyphOffsets;

    private FontFileReader originalFontReader;

    @Before
    public void setUp() throws IOException {
        this.originalFontReader = new FontFileReader(
                "test/resources/fonts/ttf/DejaVuLGCSerif.ttf");
    }

    /**
     * Tests that composed glyphs are included in the glyph subset if a
     * composite glyph is used.
     *
     * @throws IOException
     *             if an I/O error occurs
     */
    @Test
    public void testPopulateGlyphsWithComposites() throws IOException {
        // Glyph 408 -> U+01D8 "uni01D8" this is a composite glyph.
        final int[] composedIndices = setupTest(408);

        final int[] expected = new int[composedIndices.length];
        expected[1] = 6;
        expected[5] = 2;
        expected[6] = 4;

        assertArrayEquals(expected, composedIndices);
    }

    /**
     * Tests that no glyphs are added if there are no composite glyphs the
     * subset.
     *
     * @throws IOException
     *             if an I/O error occurs
     */
    @Test
    public void testPopulateNoCompositeGlyphs() throws IOException {
        final int[] composedIndices = setupTest(36, 37, 38); // "A", "B", "C"
        final int[] expected = new int[composedIndices.length];

        // There should be NO composite glyphs
        assertArrayEquals(expected, composedIndices);
    }

    /**
     * Tests that glyphs aren't remapped twice if the glyph before a composite
     * glyph has 0-length.
     *
     * @throws IOException
     *             if an I/O error occurs
     */
    @Test
    public void testGlyphsNotRemappedTwice() throws IOException {
        final int composedGlyph = 12;
        // The order of these glyph indices, must NOT be changed! (see javadoc
        // above)
        final int[] composedIndices = setupTest(1, 2, 3, 16, 2014, 4, 7, 8, 13,
                2015, composedGlyph);

        // There are 2 composed glyphs within the subset
        final int[] expected = new int[composedIndices.length];
        expected[10] = composedGlyph;

        assertArrayEquals(expected, composedIndices);
    }

    /**
     * Tests that the correct glyph is included in the subset, when a composite
     * glyph composed of a composite glyph is used.
     *
     * @throws IOException
     *             if an I/O error occurs
     */
    @Test
    public void testSingleRecursionStep() throws IOException {
        // Glyph 2077 -> U+283F "uni283F" this is composed of a composite glyph
        // (recursive).
        final int[] composedIndices = setupTest(2077);

        final int[] expected = new int[composedIndices.length];
        expected[1] = 2;

        assertArrayEquals(expected, composedIndices);
    }

    private int[] setupTest(final int... glyphIndices) throws IOException {
        final Map<Integer, Integer> glyphs = new HashMap<Integer, Integer>();
        int index = 0;
        glyphs.put(0, index++); // Glyph 0 (.notdef) must ALWAYS be in the
                                // subset

        for (final int glyphIndex : glyphIndices) {
            glyphs.put(glyphIndex, index++);
        }
        setupSubsetReader(glyphs);
        readLoca();

        return retrieveIndicesOfComposedGlyphs();
    }

    private void setupSubsetReader(final Map<Integer, Integer> glyphs)
            throws IOException {
        final TTFSubSetFile fontFile = new TTFSubSetFile();
        fontFile.readFont(this.originalFontReader, "Deja", glyphs);
        final byte[] subsetFont = fontFile.getFontSubset();
        final InputStream intputStream = new ByteArrayInputStream(subsetFont);
        this.subsetReader = new FontFileReader(intputStream);
    }

    private void readLoca() throws IOException {
        final DirData loca = getTableData("loca");
        final int numberOfGlyphs = (int) (loca.length - 4) / 4;
        this.glyphOffsets = new long[numberOfGlyphs];
        this.subsetReader.seekSet(loca.offset);

        for (int i = 0; i < numberOfGlyphs; i++) {
            this.glyphOffsets[i] = this.subsetReader.readTTFULong();
        }
    }

    private int[] retrieveIndicesOfComposedGlyphs() throws IOException {
        final DirData glyf = getTableData("glyf");
        final int[] composedGlyphIndices = new int[this.glyphOffsets.length];

        for (int i = 0; i < this.glyphOffsets.length; i++) {
            final long glyphOffset = this.glyphOffsets[i];
            if (i != this.glyphOffsets.length - 1
                    && glyphOffset == this.glyphOffsets[i + 1]) {
                continue;
            }
            this.subsetReader.seekSet(glyf.offset + glyphOffset);
            final short numberOfContours = this.subsetReader.readTTFShort();
            if (numberOfContours < 0) {
                this.subsetReader.skip(8);
                this.subsetReader.readTTFUShort(); // flags
                final int glyphIndex = this.subsetReader.readTTFUShort();
                composedGlyphIndices[i] = glyphIndex;
            }
        }
        return composedGlyphIndices;
    }

    private DirData getTableData(final String tableName) throws IOException {
        this.subsetReader.seekSet(0);
        this.subsetReader.skip(12);
        String name;
        do {
            name = this.subsetReader.readTTFString(4);
            this.subsetReader.skip(4 * 3);
        } while (!name.equals(tableName));

        this.subsetReader.skip(-8); // We've found the table, go back to get the
                                    // data we skipped over
        return new DirData(this.subsetReader.readTTFLong(),
                this.subsetReader.readTTFLong());
    }

    private void assertArrayEquals(final int[] expected, final int[] actual) {
        assertTrue(Arrays.equals(expected, actual));
    }
}
