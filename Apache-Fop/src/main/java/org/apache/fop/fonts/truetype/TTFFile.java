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

/* $Id: TTFFile.java 1395925 2012-10-09 09:13:18Z jeremias $ */

package org.apache.fop.fonts.truetype;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.complexscripts.fonts.AdvancedTypographicTableFormatException;
import org.apache.fop.complexscripts.fonts.GlyphDefinitionTable;
import org.apache.fop.complexscripts.fonts.GlyphPositioningTable;
import org.apache.fop.complexscripts.fonts.GlyphSubstitutionTable;
import org.apache.fop.complexscripts.fonts.OTFAdvancedTypographicTableReader;
import org.apache.fop.fonts.CMapSegment;
import org.apache.fop.fonts.FontUtil;
import org.apache.xmlgraphics.fonts.Glyphs;

/**
 * Reads a TrueType file or a TrueType Collection. The TrueType spec can be
 * found at the Microsoft. Typography site: http://www.microsoft.com/truetype/
 */
@Slf4j
public class TTFFile {

    static final byte NTABS = 24;
    static final int MAX_CHAR_CODE = 255;
    static final int ENC_BUF_SIZE = 1024;

    private static final String[] MAC_GLYPH_ORDERING = {
    /* 0x000 */
    ".notdef", ".null", "nonmarkingreturn", "space", "exclam", "quotedbl",
            "numbersign", "dollar", "percent", "ampersand", "quotesingle",
            "parenleft", "parenright", "asterisk", "plus", "comma",
            /* 0x010 */
            "hyphen", "period", "slash", "zero", "one", "two", "three", "four",
            "five", "six", "seven", "eight", "nine", "colon", "semicolon",
            "less",
            /* 0x020 */
            "equal", "greater", "question", "at", "A", "B", "C", "D", "E", "F",
            "G", "H", "I", "J", "K", "L",
            /* 0x030 */
            "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y",
            "Z", "bracketleft", "backslash",
            /* 0x040 */
            "bracketright", "asciicircum", "underscore", "grave", "a", "b",
            "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
            /* 0x050 */
            "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y",
            "z", "braceleft", "bar",
            /* 0x060 */
            "braceright", "asciitilde", "Adieresis", "Aring", "Ccedilla",
            "Eacute", "Ntilde", "Odieresis", "Udieresis", "aacute", "agrave",
            "acircumflex", "adieresis", "atilde", "aring", "ccedilla",
            /* 0x070 */
            "eacute", "egrave", "ecircumflex", "edieresis", "iacute", "igrave",
            "icircumflex", "idieresis", "ntilde", "oacute", "ograve",
            "ocircumflex", "odieresis", "otilde", "uacute", "ugrave",
            /* 0x080 */
            "ucircumflex", "udieresis", "dagger", "degree", "cent", "sterling",
            "section", "bullet", "paragraph", "germandbls", "registered",
            "copyright", "trademark", "acute", "dieresis", "notequal",
            /* 0x090 */
            "AE", "Oslash", "infinity", "plusminus", "lessequal",
            "greaterequal", "yen", "mu", "partialdiff", "summation", "product",
            "pi", "integral", "ordfeminine", "ordmasculine", "Omega",
            /* 0x0A0 */
            "ae", "oslash", "questiondown", "exclamdown", "logicalnot",
            "radical", "florin", "approxequal", "Delta", "guillemotleft",
            "guillemotright", "ellipsis", "nonbreakingspace", "Agrave",
            "Atilde", "Otilde",
            /* 0x0B0 */
            "OE", "oe", "endash", "emdash", "quotedblleft", "quotedblright",
            "quoteleft", "quoteright", "divide", "lozenge", "ydieresis",
            "Ydieresis", "fraction", "currency", "guilsinglleft",
            "guilsinglright",
            /* 0x0C0 */
            "fi", "fl", "daggerdbl", "periodcentered", "quotesinglbase",
            "quotedblbase", "perthousand", "Acircumflex", "Ecircumflex",
            "Aacute", "Edieresis", "Egrave", "Iacute", "Icircumflex",
            "Idieresis", "Igrave",
            /* 0x0D0 */
            "Oacute", "Ocircumflex", "apple", "Ograve", "Uacute",
            "Ucircumflex", "Ugrave", "dotlessi", "circumflex", "tilde",
            "macron", "breve", "dotaccent", "ring", "cedilla", "hungarumlaut",
            /* 0x0E0 */
            "ogonek", "caron", "Lslash", "lslash", "Scaron", "scaron",
            "Zcaron", "zcaron", "brokenbar", "Eth", "eth", "Yacute", "yacute",
            "Thorn", "thorn", "minus",
            /* 0x0F0 */
            "multiply", "onesuperior", "twosuperior", "threesuperior",
            "onehalf", "onequarter", "threequarters", "franc", "Gbreve",
            "gbreve", "Idotaccent", "Scedilla", "scedilla", "Cacute", "cacute",
            "Ccaron",
            /* 0x100 */
            "ccaron", "dcroat" };

    /** The FontFileReader used to read this TrueType font. */
    protected FontFileReader fontFile;

    /** Set to true to get even more debug output than with level DEBUG */
    public static final boolean TRACE_ENABLED = false;

    private final String encoding = "WinAnsiEncoding"; // Default encoding

    private final short firstChar = 0;

    private boolean useKerning = false;

    private boolean isEmbeddable = true;
    private boolean hasSerifs = true;
    /**
     * Table directory
     */
    protected Map<TTFTableName, TTFDirTabEntry> dirTabs;
    private Map<Integer, Map<Integer, Integer>> kerningTab; // for CIDs
    private Map<Integer, Map<Integer, Integer>> ansiKerningTab; // For
    // winAnsiEncoding
    private List<CMapSegment> cmaps;
    private Set<UnicodeMapping> unicodeMappings;

    private int upem; // unitsPerEm from "head" table
    private int nhmtx; // Number of horizontal metrics
    private PostScriptVersion postScriptVersion;
    private int locaFormat;
    /**
     * Offset to last loca
     */
    protected long lastLoca = 0;
    private int numberOfGlyphs; // Number of glyphs in font (read from "maxp"
    // table)

    /**
     * Contains glyph data
     */
    protected TTFMtxEntry[] mtxTab; // Contains glyph data

    private String postScriptName = "";
    private String fullName = "";
    private String notice = "";
    private final Set<String> familyNames = new HashSet<String>();
    private String subFamilyName = "";

    private long italicAngle = 0;
    private long isFixedPitch = 0;
    private int fontBBox1 = 0;
    private int fontBBox2 = 0;
    private int fontBBox3 = 0;
    private int fontBBox4 = 0;
    private int capHeight = 0;
    private int os2CapHeight = 0;
    private int underlinePosition = 0;
    private int underlineThickness = 0;
    private int xHeight = 0;
    private int os2xHeight = 0;
    // Effective ascender/descender
    private int ascender = 0;
    private int descender = 0;
    // Ascender/descender from hhea table
    private int hheaAscender = 0;
    private int hheaDescender = 0;
    // Ascender/descender from OS/2 table
    private int os2Ascender = 0;
    private int os2Descender = 0;
    private int usWeightClass = 0;

    private short lastChar = 0;

    private int[] ansiWidth;
    private Map<Integer, List<Integer>> ansiIndex;

    // internal mapping of glyph indexes to unicode indexes
    // used for quick mappings in this class
    private final Map<Integer, Integer> glyphToUnicodeMap = new HashMap<Integer, Integer>();
    private final Map<Integer, Integer> unicodeToGlyphMap = new HashMap<Integer, Integer>();

    private TTFDirTabEntry currentDirTab;

    private boolean isCFF;

    // advanced typographic table support
    private boolean useAdvanced = false;
    private OTFAdvancedTypographicTableReader advancedTableReader;

    public TTFFile() {
        this(true, false);
    }

    /**
     * Constructor
     *
     * @param useKerning
     *            true if kerning data should be loaded
     * @param useAdvanced
     *            true if advanced typographic tables should be loaded
     */
    public TTFFile(final boolean useKerning, final boolean useAdvanced) {
        this.useKerning = useKerning;
        this.useAdvanced = useAdvanced;
    }

    /**
     * Key-value helper class.
     */
    final class UnicodeMapping implements Comparable {

        private final int unicodeIndex;
        private final int glyphIndex;

        UnicodeMapping(final int glyphIndex, final int unicodeIndex) {
            this.unicodeIndex = unicodeIndex;
            this.glyphIndex = glyphIndex;
            TTFFile.this.glyphToUnicodeMap.put(new Integer(glyphIndex),
                    new Integer(unicodeIndex));
            TTFFile.this.unicodeToGlyphMap.put(new Integer(unicodeIndex),
                    new Integer(glyphIndex));
        }

        /**
         * Returns the glyphIndex.
         *
         * @return the glyph index
         */
        public int getGlyphIndex() {
            return this.glyphIndex;
        }

        /**
         * Returns the unicodeIndex.
         *
         * @return the Unicode index
         */
        public int getUnicodeIndex() {
            return this.unicodeIndex;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            int hc = this.unicodeIndex;
            hc = 19 * hc + (hc ^ this.glyphIndex);
            return hc;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object o) {
            if (o instanceof UnicodeMapping) {
                final UnicodeMapping m = (UnicodeMapping) o;
                if (this.unicodeIndex != m.unicodeIndex) {
                    return false;
                } else {
                    return this.glyphIndex == m.glyphIndex;
                }
            } else {
                return false;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final Object o) {
            if (o instanceof UnicodeMapping) {
                final UnicodeMapping m = (UnicodeMapping) o;
                if (this.unicodeIndex > m.unicodeIndex) {
                    return 1;
                } else if (this.unicodeIndex < m.unicodeIndex) {
                    return -1;
                } else {
                    return 0;
                }
            } else {
                return -1;
            }
        }
    }

    /**
     * Version of the PostScript table (<q>post</q>) contained in this font.
     */
    public static enum PostScriptVersion {
        /** PostScript table version 1.0. */
        V1,
        /** PostScript table version 2.0. */
        V2,
        /** PostScript table version 3.0. */
        V3,
        /** Unknown version of the PostScript table. */
        UNKNOWN;
    }

    /**
     * Obtain directory table entry.
     *
     * @param name
     *            (tag) of entry
     * @return a directory table entry or null if none found
     */
    public TTFDirTabEntry getDirectoryEntry(final TTFTableName name) {
        return this.dirTabs.get(name);
    }

    /**
     * Position inputstream to position indicated in the dirtab offset + offset
     *
     * @param in
     *            font file reader
     * @param tableName
     *            (tag) of table
     * @param offset
     *            from start of table
     * @return true if seek succeeded
     * @throws IOException
     *             if I/O exception occurs during seek
     */
    public boolean seekTab(final FontFileReader in,
            final TTFTableName tableName, final long offset) throws IOException {
        final TTFDirTabEntry dt = this.dirTabs.get(tableName);
        if (dt == null) {
            log.error("Dirtab " + tableName.getName() + " not found.");
            return false;
        } else {
            in.seekSet(dt.getOffset() + offset);
            this.currentDirTab = dt;
        }
        return true;
    }

    /**
     * Convert from truetype unit to pdf unit based on the unitsPerEm field in
     * the "head" table
     *
     * @param n
     *            truetype unit
     * @return pdf unit
     */
    public int convertTTFUnit2PDFUnit(final int n) {
        int ret;
        if (n < 0) {
            final long rest1 = n % this.upem;
            final long storrest = 1000 * rest1;
            final long ledd2 = storrest != 0 ? rest1 / storrest : 0;
            ret = -(-1000 * n / this.upem - (int) ledd2);
        } else {
            ret = n / this.upem * 1000 + n % this.upem * 1000 / this.upem;
        }

        return ret;
    }

    /**
     * Read the cmap table, return false if the table is not present or only
     * unsupported tables are present. Currently only unicode cmaps are
     * supported. Set the unicodeIndex in the TTFMtxEntries and fills in the
     * cmaps vector.
     */
    private boolean readCMAP() throws IOException {

        this.unicodeMappings = new java.util.TreeSet();

        seekTab(this.fontFile, TTFTableName.CMAP, 2);
        final int numCMap = this.fontFile.readTTFUShort(); // Number of cmap
        // subtables
        long cmapUniOffset = 0;
        long symbolMapOffset = 0;

        if (log.isDebugEnabled()) {
            log.debug(numCMap + " cmap tables");
        }

        // Read offset for all tables. We are only interested in the unicode
        // table
        for (int i = 0; i < numCMap; i++) {
            final int cmapPID = this.fontFile.readTTFUShort();
            final int cmapEID = this.fontFile.readTTFUShort();
            final long cmapOffset = this.fontFile.readTTFLong();

            if (log.isDebugEnabled()) {
                log.debug("Platform ID: " + cmapPID + " Encoding: " + cmapEID);
            }

            if (cmapPID == 3 && cmapEID == 1) {
                cmapUniOffset = cmapOffset;
            }
            if (cmapPID == 3 && cmapEID == 0) {
                symbolMapOffset = cmapOffset;
            }
        }

        if (cmapUniOffset > 0) {
            return readUnicodeCmap(cmapUniOffset, 1);
        } else if (symbolMapOffset > 0) {
            return readUnicodeCmap(symbolMapOffset, 0);
        } else {
            log.error("Unsupported TrueType font: No Unicode or Symbol cmap table"
                    + " not present. Aborting");
            return false;
        }
    }

    private boolean readUnicodeCmap // CSOK: MethodLength
    (final long cmapUniOffset, final int encodingID) throws IOException {
        // Read CMAP table and correct mtxTab.index
        int mtxPtr = 0;

        // Read unicode cmap
        seekTab(this.fontFile, TTFTableName.CMAP, cmapUniOffset);
        final int cmapFormat = this.fontFile.readTTFUShort();
        /* int cmap_length = */this.fontFile.readTTFUShort(); // skip cmap
        // length

        if (log.isDebugEnabled()) {
            log.debug("CMAP format: " + cmapFormat);
        }

        if (cmapFormat == 4) {
            this.fontFile.skip(2); // Skip version number
            final int cmapSegCountX2 = this.fontFile.readTTFUShort();
            final int cmapSearchRange = this.fontFile.readTTFUShort();
            final int cmapEntrySelector = this.fontFile.readTTFUShort();
            final int cmapRangeShift = this.fontFile.readTTFUShort();

            if (log.isDebugEnabled()) {
                log.debug("segCountX2   : " + cmapSegCountX2);
                log.debug("searchRange  : " + cmapSearchRange);
                log.debug("entrySelector: " + cmapEntrySelector);
                log.debug("rangeShift   : " + cmapRangeShift);
            }

            final int[] cmapEndCounts = new int[cmapSegCountX2 / 2];
            final int[] cmapStartCounts = new int[cmapSegCountX2 / 2];
            final int[] cmapDeltas = new int[cmapSegCountX2 / 2];
            final int[] cmapRangeOffsets = new int[cmapSegCountX2 / 2];

            for (int i = 0; i < cmapSegCountX2 / 2; i++) {
                cmapEndCounts[i] = this.fontFile.readTTFUShort();
            }

            this.fontFile.skip(2); // Skip reservedPad

            for (int i = 0; i < cmapSegCountX2 / 2; i++) {
                cmapStartCounts[i] = this.fontFile.readTTFUShort();
            }

            for (int i = 0; i < cmapSegCountX2 / 2; i++) {
                cmapDeltas[i] = this.fontFile.readTTFShort();
            }

            // int startRangeOffset = in.getCurrentPos();

            for (int i = 0; i < cmapSegCountX2 / 2; i++) {
                cmapRangeOffsets[i] = this.fontFile.readTTFUShort();
            }

            final int glyphIdArrayOffset = this.fontFile.getCurrentPos();

            final BitSet eightBitGlyphs = new BitSet(256);

            // Insert the unicode id for the glyphs in mtxTab
            // and fill in the cmaps ArrayList

            for (int i = 0; i < cmapStartCounts.length; i++) {

                if (log.isTraceEnabled()) {
                    log.trace(i + ": " + cmapStartCounts[i] + " - "
                            + cmapEndCounts[i]);
                }
                if (log.isDebugEnabled()) {
                    if (isInPrivateUseArea(cmapStartCounts[i], cmapEndCounts[i])) {
                        log.debug("Font contains glyphs in the Unicode private use area: "
                                + Integer.toHexString(cmapStartCounts[i])
                                + " - " + Integer.toHexString(cmapEndCounts[i]));
                    }
                }

                for (int j = cmapStartCounts[i]; j <= cmapEndCounts[i]; j++) {

                    // Update lastChar
                    if (j < 256 && j > this.lastChar) {
                        this.lastChar = (short) j;
                    }

                    if (j < 256) {
                        eightBitGlyphs.set(j);
                    }

                    if (mtxPtr < this.mtxTab.length) {
                        int glyphIdx;
                        // the last character 65535 = .notdef
                        // may have a range offset
                        if (cmapRangeOffsets[i] != 0 && j != 65535) {
                            final int glyphOffset = glyphIdArrayOffset
                                    + (cmapRangeOffsets[i] / 2 + j
                                            - cmapStartCounts[i] + i - cmapSegCountX2 / 2)
                                    * 2;
                            this.fontFile.seekSet(glyphOffset);
                            glyphIdx = this.fontFile.readTTFUShort()
                                    + cmapDeltas[i] & 0xffff;

                            this.unicodeMappings.add(new UnicodeMapping(
                                    glyphIdx, j));
                            this.mtxTab[glyphIdx].getUnicodeIndex().add(
                                    new Integer(j));

                            // Also add winAnsiWidth
                            final List<Integer> v = this.ansiIndex
                                    .get(new Integer(j));
                            if (v != null) {
                                for (final Integer aIdx : v) {
                                    this.ansiWidth[aIdx.intValue()] = this.mtxTab[glyphIdx]
                                            .getWx();

                                    if (log.isTraceEnabled()) {
                                        log.trace("Added width "
                                                + this.mtxTab[glyphIdx].getWx()
                                                + " uni: " + j + " ansi: "
                                                + aIdx.intValue());
                                    }
                                }
                            }

                            if (log.isTraceEnabled()) {
                                log.trace("Idx: " + glyphIdx + " Delta: "
                                        + cmapDeltas[i] + " Unicode: " + j
                                        + " name: "
                                        + this.mtxTab[glyphIdx].getName());
                            }
                        } else {
                            glyphIdx = j + cmapDeltas[i] & 0xffff;

                            if (glyphIdx < this.mtxTab.length) {
                                this.mtxTab[glyphIdx].getUnicodeIndex().add(
                                        new Integer(j));
                            } else {
                                log.debug("Glyph " + glyphIdx
                                        + " out of range: "
                                        + this.mtxTab.length);
                            }

                            this.unicodeMappings.add(new UnicodeMapping(
                                    glyphIdx, j));
                            if (glyphIdx < this.mtxTab.length) {
                                this.mtxTab[glyphIdx].getUnicodeIndex().add(
                                        new Integer(j));
                            } else {
                                log.debug("Glyph " + glyphIdx
                                        + " out of range: "
                                        + this.mtxTab.length);
                            }

                            // Also add winAnsiWidth
                            final List<Integer> v = this.ansiIndex
                                    .get(new Integer(j));
                            if (v != null) {
                                for (final Integer aIdx : v) {
                                    this.ansiWidth[aIdx.intValue()] = this.mtxTab[glyphIdx]
                                            .getWx();
                                }
                            }

                            // getLogger().debug("IIdx: " +
                            // mtxPtr +
                            // " Delta: " + cmap_deltas[i] +
                            // " Unicode: " + j +
                            // " name: " +
                            // mtxTab[(j+cmap_deltas[i]) & 0xffff].name);

                        }
                        if (glyphIdx < this.mtxTab.length) {
                            if (this.mtxTab[glyphIdx].getUnicodeIndex().size() < 2) {
                                mtxPtr++;
                            }
                        }
                    }
                }
            }
        } else {
            log.error("Cmap format not supported: " + cmapFormat);
            return false;
        }
        return true;
    }

    private boolean isInPrivateUseArea(final int start, final int end) {
        return isInPrivateUseArea(start) || isInPrivateUseArea(end);
    }

    private boolean isInPrivateUseArea(final int unicode) {
        return unicode >= 0xE000 && unicode <= 0xF8FF;
    }

    /**
     * Print first char/last char
     */
    private void printMaxMin() {
        int min = 255;
        int max = 0;
        for (final TTFMtxEntry element : this.mtxTab) {
            if (element.getIndex() < min) {
                min = element.getIndex();
            }
            if (element.getIndex() > max) {
                max = element.getIndex();
            }
        }
        log.info("Min: " + min);
        log.info("Max: " + max);
    }

    /**
     * Reads the font using a FontFileReader.
     *
     * @param in
     *            The FontFileReader to use
     * @throws IOException
     *             In case of an I/O problem
     */
    public void readFont(final FontFileReader in) throws IOException {
        readFont(in, (String) null);
    }

    /**
     * initialize the ansiWidths array (for winAnsiEncoding) and fill with the
     * missingwidth
     */
    private void initAnsiWidths() {
        this.ansiWidth = new int[256];
        for (int i = 0; i < 256; i++) {
            this.ansiWidth[i] = this.mtxTab[0].getWx();
        }

        // Create an index hash to the ansiWidth
        // Can't just index the winAnsiEncoding when inserting widths
        // same char (eg bullet) is repeated more than one place
        this.ansiIndex = new HashMap<Integer, List<Integer>>();
        for (int i = 32; i < Glyphs.WINANSI_ENCODING.length; i++) {
            final Integer ansi = new Integer(i);
            final Integer uni = new Integer(Glyphs.WINANSI_ENCODING[i]);

            List<Integer> v = this.ansiIndex.get(uni);
            if (v == null) {
                v = new ArrayList<Integer>();
                this.ansiIndex.put(uni, v);
            }
            v.add(ansi);
        }
    }

    /**
     * Read the font data. If the fontfile is a TrueType Collection (.ttc file)
     * the name of the font to read data for must be supplied, else the name is
     * ignored.
     *
     * @param in
     *            The FontFileReader to use
     * @param name
     *            The name of the font
     * @return boolean Returns true if the font is valid
     * @throws IOException
     *             In case of an I/O problem
     */
    public boolean readFont(final FontFileReader in, final String name)
            throws IOException {
        this.fontFile = in;
        /*
         * Check if TrueType collection, and that the name exists in the
         * collection
         */
        if (!checkTTC(name)) {
            if (name == null) {
                throw new IllegalArgumentException(
                        "For TrueType collection you must specify which font "
                                + "to select (-ttcname)");
            } else {
                throw new IOException(
                        "Name does not exist in the TrueType collection: "
                                + name);
            }
        }

        readDirTabs();
        readFontHeader();
        getNumGlyphs();
        if (log.isDebugEnabled()) {
            log.debug("Number of glyphs in font: " + this.numberOfGlyphs);
        }
        readHorizontalHeader();
        readHorizontalMetrics();
        initAnsiWidths();
        readPostScript();
        readOS2();
        determineAscDesc();
        if (!this.isCFF) {
            readIndexToLocation();
            readGlyf();
        }
        readName();
        final boolean pcltFound = readPCLT();
        // Read cmap table and fill in ansiwidths
        final boolean valid = readCMAP();
        if (!valid) {
            return false;
        }
        // Create cmaps for bfentries
        createCMaps();

        if (this.useKerning) {
            readKerning();
        }

        // Read advanced typographic tables.
        if (this.useAdvanced) {
            try {
                final OTFAdvancedTypographicTableReader atr = new OTFAdvancedTypographicTableReader(
                        this, in);
                atr.readAll();
                this.advancedTableReader = atr;
            } catch (final AdvancedTypographicTableFormatException e) {
                log.warn("Encountered format constraint violation in advanced (typographic) table (AT) "
                        + "in font '"
                        + getFullName()
                        + "', ignoring AT data: "
                        + e.getMessage());
            }
        }

        guessVerticalMetricsFromGlyphBBox();
        return true;
    }

    /**
     * Reads a font.
     *
     * @param in
     *            FontFileReader to read from
     * @param name
     *            Name to be checked for in the font file
     * @param glyphs
     *            Map of glyphs (glyphs has old index as (Integer) key and new
     *            index as (Integer) value)
     * @throws IOException
     *             in case of an I/O problem
     */
    public void readFont(final FontFileReader in, final String name,
            final Map<Integer, Integer> glyphs) throws IOException {
        readFont(in, name);
    }

    private void createCMaps() {
        this.cmaps = new ArrayList<CMapSegment>();
        int unicodeStart;
        int glyphStart;
        int unicodeEnd;

        final Iterator<UnicodeMapping> e = this.unicodeMappings.iterator();
        UnicodeMapping um = e.next();
        UnicodeMapping lastMapping = um;

        unicodeStart = um.getUnicodeIndex();
        glyphStart = um.getGlyphIndex();

        while (e.hasNext()) {
            um = e.next();
            if (lastMapping.getUnicodeIndex() + 1 != um.getUnicodeIndex()
                    || lastMapping.getGlyphIndex() + 1 != um.getGlyphIndex()) {
                unicodeEnd = lastMapping.getUnicodeIndex();
                this.cmaps.add(new CMapSegment(unicodeStart, unicodeEnd,
                        glyphStart));
                unicodeStart = um.getUnicodeIndex();
                glyphStart = um.getGlyphIndex();
            }
            lastMapping = um;
        }

        unicodeEnd = lastMapping.getUnicodeIndex();
        this.cmaps.add(new CMapSegment(unicodeStart, unicodeEnd, glyphStart));
    }

    /**
     * Returns the PostScript name of the font.
     *
     * @return String The PostScript name
     */
    public String getPostScriptName() {
        if (this.postScriptName.length() == 0) {
            return FontUtil.stripWhiteSpace(getFullName());
        } else {
            return this.postScriptName;
        }
    }

    PostScriptVersion getPostScriptVersion() {
        return this.postScriptVersion;
    }

    /**
     * Returns the font family names of the font.
     *
     * @return Set The family names (a Set of Strings)
     */
    public Set<String> getFamilyNames() {
        return this.familyNames;
    }

    /**
     * Returns the font sub family name of the font.
     *
     * @return String The sub family name
     */
    public String getSubFamilyName() {
        return this.subFamilyName;
    }

    /**
     * Returns the full name of the font.
     *
     * @return String The full name
     */
    public String getFullName() {
        return this.fullName;
    }

    /**
     * Returns the name of the character set used.
     *
     * @return String The caracter set
     */
    public String getCharSetName() {
        return this.encoding;
    }

    /**
     * Returns the CapHeight attribute of the font.
     *
     * @return int The CapHeight
     */
    public int getCapHeight() {
        return convertTTFUnit2PDFUnit(this.capHeight);
    }

    /**
     * Returns the XHeight attribute of the font.
     *
     * @return int The XHeight
     */
    public int getXHeight() {
        return convertTTFUnit2PDFUnit(this.xHeight);
    }

    /**
     * Returns the number of bytes necessary to pad the currentPosition so that
     * a table begins on a 4-byte boundary.
     *
     * @param currentPosition
     *            the position to pad.
     * @return int the number of bytes to pad.
     */
    protected int getPadSize(final int currentPosition) {
        final int padSize = 4 - currentPosition % 4;
        return padSize < 4 ? padSize : 0;
    }

    /**
     * Returns the Flags attribute of the font.
     *
     * @return int The Flags
     */
    public int getFlags() {
        int flags = 32; // Use Adobe Standard charset
        if (this.italicAngle != 0) {
            flags |= 64;
        }
        if (this.isFixedPitch != 0) {
            flags |= 2;
        }
        if (this.hasSerifs) {
            flags |= 1;
        }
        return flags;
    }

    /**
     * Returns the weight class of this font. Valid values are 100, 200....,800,
     * 900.
     *
     * @return the weight class value (or 0 if there was no OS/2 table in the
     *         font)
     */
    public int getWeightClass() {
        return this.usWeightClass;
    }

    /**
     * Returns the StemV attribute of the font.
     *
     * @return String The StemV
     */
    public String getStemV() {
        return "0";
    }

    /**
     * Returns the ItalicAngle attribute of the font.
     *
     * @return String The ItalicAngle
     */
    public String getItalicAngle() {
        final String ia = Short.toString((short) (this.italicAngle / 0x10000));

        // This is the correct italic angle, however only int italic
        // angles are supported at the moment so this is commented out.
        /*
         * if ((italicAngle % 0x10000) > 0 )
         * ia=ia+(comma+Short.toString((short)((short)((italicAngle %
         * 0x10000)*1000)/0x10000)));
         */
        return ia;
    }

    /**
     * @return int[] The font bbox
     */
    public int[] getFontBBox() {
        final int[] fbb = new int[4];
        fbb[0] = convertTTFUnit2PDFUnit(this.fontBBox1);
        fbb[1] = convertTTFUnit2PDFUnit(this.fontBBox2);
        fbb[2] = convertTTFUnit2PDFUnit(this.fontBBox3);
        fbb[3] = convertTTFUnit2PDFUnit(this.fontBBox4);

        return fbb;
    }

    /**
     * Returns the LowerCaseAscent attribute of the font.
     *
     * @return int The LowerCaseAscent
     */
    public int getLowerCaseAscent() {
        return convertTTFUnit2PDFUnit(this.ascender);
    }

    /**
     * Returns the LowerCaseDescent attribute of the font.
     *
     * @return int The LowerCaseDescent
     */
    public int getLowerCaseDescent() {
        return convertTTFUnit2PDFUnit(this.descender);
    }

    /**
     * Returns the index of the last character, but this is for WinAnsiEncoding
     * only, so the last char is < 256.
     *
     * @return short Index of the last character (<256)
     */
    public short getLastChar() {
        return this.lastChar;
    }

    /**
     * Returns the index of the first character.
     *
     * @return short Index of the first character
     */
    public short getFirstChar() {
        return this.firstChar;
    }

    /**
     * Returns an array of character widths.
     *
     * @return int[] The character widths
     */
    public int[] getWidths() {
        final int[] wx = new int[this.mtxTab.length];
        for (int i = 0; i < wx.length; i++) {
            wx[i] = convertTTFUnit2PDFUnit(this.mtxTab[i].getWx());
        }

        return wx;
    }

    /**
     * Returns the width of a given character.
     *
     * @param idx
     *            Index of the character
     * @return int Standard width
     */
    public int getCharWidth(final int idx) {
        return convertTTFUnit2PDFUnit(this.ansiWidth[idx]);
    }

    /**
     * Returns the kerning table.
     *
     * @return Map The kerning table
     */
    public Map<Integer, Map<Integer, Integer>> getKerning() {
        return this.kerningTab;
    }

    /**
     * Returns the ANSI kerning table.
     *
     * @return Map The ANSI kerning table
     */
    public Map<Integer, Map<Integer, Integer>> getAnsiKerning() {
        return this.ansiKerningTab;
    }

    /**
     * Indicates if the font may be embedded.
     *
     * @return boolean True if it may be embedded
     */
    public boolean isEmbeddable() {
        return this.isEmbeddable;
    }

    /**
     * Indicates whether or not the font is an OpenType CFF font (rather than a
     * TrueType font).
     *
     * @return true if the font is in OpenType CFF format.
     */
    public boolean isCFF() {
        return this.isCFF;
    }

    /**
     * Read Table Directory from the current position in the FontFileReader and
     * fill the global HashMap dirTabs with the table name (String) as key and a
     * TTFDirTabEntry as value.
     *
     * @throws IOException
     *             in case of an I/O problem
     */
    protected void readDirTabs() throws IOException {
        final int sfntVersion = this.fontFile.readTTFLong(); // TTF_FIXED_SIZE
        // (4 bytes)
        switch (sfntVersion) {
        case 0x10000:
            log.debug("sfnt version: OpenType 1.0");
            break;
        case 0x4F54544F: // "OTTO"
            this.isCFF = true;
            log.debug("sfnt version: OpenType with CFF data");
            break;
        case 0x74727565: // "true"
            log.debug("sfnt version: Apple TrueType");
            break;
        case 0x74797031: // "typ1"
            log.debug("sfnt version: Apple Type 1 housed in sfnt wrapper");
            break;
        default:
            log.debug("Unknown sfnt version: "
                    + Integer.toHexString(sfntVersion));
            break;
        }
        final int ntabs = this.fontFile.readTTFUShort();
        this.fontFile.skip(6); // 3xTTF_USHORT_SIZE

        this.dirTabs = new HashMap<TTFTableName, TTFDirTabEntry>();
        final TTFDirTabEntry[] pd = new TTFDirTabEntry[ntabs];
        log.debug("Reading " + ntabs + " dir tables");

        for (int i = 0; i < ntabs; i++) {
            pd[i] = new TTFDirTabEntry();
            final String tableName = pd[i].read(this.fontFile);
            this.dirTabs.put(TTFTableName.getValue(tableName), pd[i]);
        }
        this.dirTabs.put(TTFTableName.TABLE_DIRECTORY, new TTFDirTabEntry(0L,
                this.fontFile.getCurrentPos()));
        log.debug("dir tables: " + this.dirTabs.keySet());
    }

    /**
     * Read the "head" table, this reads the bounding box and sets the upem
     * (unitsPerEM) variable
     *
     * @throws IOException
     *             in case of an I/O problem
     */
    protected void readFontHeader() throws IOException {
        seekTab(this.fontFile, TTFTableName.HEAD, 2 * 4 + 2 * 4);
        final int flags = this.fontFile.readTTFUShort();
        if (log.isDebugEnabled()) {
            log.debug("flags: " + flags + " - " + Integer.toString(flags, 2));
        }
        this.upem = this.fontFile.readTTFUShort();
        if (log.isDebugEnabled()) {
            log.debug("unit per em: " + this.upem);
        }

        this.fontFile.skip(16);

        this.fontBBox1 = this.fontFile.readTTFShort();
        this.fontBBox2 = this.fontFile.readTTFShort();
        this.fontBBox3 = this.fontFile.readTTFShort();
        this.fontBBox4 = this.fontFile.readTTFShort();
        if (log.isDebugEnabled()) {
            log.debug("font bbox: xMin=" + this.fontBBox1 + " yMin="
                    + this.fontBBox2 + " xMax=" + this.fontBBox3 + " yMax="
                    + this.fontBBox4);
        }

        this.fontFile.skip(2 + 2 + 2);

        this.locaFormat = this.fontFile.readTTFShort();
    }

    /**
     * Read the number of glyphs from the "maxp" table
     *
     * @throws IOException
     *             in case of an I/O problem
     */
    protected void getNumGlyphs() throws IOException {
        seekTab(this.fontFile, TTFTableName.MAXP, 4);
        this.numberOfGlyphs = this.fontFile.readTTFUShort();
    }

    /**
     * Read the "hhea" table to find the ascender and descender and size of
     * "hmtx" table, as a fixed size font might have only one width.
     *
     * @throws IOException
     *             in case of an I/O problem
     */
    protected void readHorizontalHeader() throws IOException {
        seekTab(this.fontFile, TTFTableName.HHEA, 4);
        this.hheaAscender = this.fontFile.readTTFShort();
        this.hheaDescender = this.fontFile.readTTFShort();

        this.fontFile.skip(2 + 2 + 3 * 2 + 8 * 2);
        this.nhmtx = this.fontFile.readTTFUShort();

        if (log.isDebugEnabled()) {
            log.debug("hhea.Ascender: "
                    + formatUnitsForDebug(this.hheaAscender));
            log.debug("hhea.Descender: "
                    + formatUnitsForDebug(this.hheaDescender));
            log.debug("Number of horizontal metrics: " + this.nhmtx);
        }
    }

    /**
     * Read "hmtx" table and put the horizontal metrics in the mtxTab array. If
     * the number of metrics is less than the number of glyphs (eg fixed size
     * fonts), extend the mtxTab array and fill in the missing widths
     *
     * @throws IOException
     *             in case of an I/O problem
     */
    protected void readHorizontalMetrics() throws IOException {
        seekTab(this.fontFile, TTFTableName.HMTX, 0);

        final int mtxSize = Math.max(this.numberOfGlyphs, this.nhmtx);
        this.mtxTab = new TTFMtxEntry[mtxSize];

        if (log.isTraceEnabled()) {
            log.trace("*** Widths array: \n");
        }
        for (int i = 0; i < mtxSize; i++) {
            this.mtxTab[i] = new TTFMtxEntry();
        }
        for (int i = 0; i < this.nhmtx; i++) {
            this.mtxTab[i].setWx(this.fontFile.readTTFUShort());
            this.mtxTab[i].setLsb(this.fontFile.readTTFUShort());

            if (log.isTraceEnabled()) {
                log.trace("   width[" + i + "] = "
                        + convertTTFUnit2PDFUnit(this.mtxTab[i].getWx()) + ";");
            }
        }

        if (this.nhmtx < mtxSize) {
            // Fill in the missing widths
            final int lastWidth = this.mtxTab[this.nhmtx - 1].getWx();
            for (int i = this.nhmtx; i < mtxSize; i++) {
                this.mtxTab[i].setWx(lastWidth);
                this.mtxTab[i].setLsb(this.fontFile.readTTFUShort());
            }
        }
    }

    /**
     * Read the "post" table containing the PostScript names of the glyphs.
     */
    private void readPostScript() throws IOException {
        seekTab(this.fontFile, TTFTableName.POST, 0);
        final int postFormat = this.fontFile.readTTFLong();
        this.italicAngle = this.fontFile.readTTFULong();
        this.underlinePosition = this.fontFile.readTTFShort();
        this.underlineThickness = this.fontFile.readTTFShort();
        this.isFixedPitch = this.fontFile.readTTFULong();

        // Skip memory usage values
        this.fontFile.skip(4 * 4);

        log.debug("PostScript format: 0x" + Integer.toHexString(postFormat));
        switch (postFormat) {
        case 0x00010000:
            log.debug("PostScript format 1");
            this.postScriptVersion = PostScriptVersion.V1;
            for (int i = 0; i < MAC_GLYPH_ORDERING.length; i++) {
                this.mtxTab[i].setName(MAC_GLYPH_ORDERING[i]);
            }
            break;
        case 0x00020000:
            log.debug("PostScript format 2");
            this.postScriptVersion = PostScriptVersion.V2;
            int numGlyphStrings = 0;

            // Read Number of Glyphs
            final int l = this.fontFile.readTTFUShort();

            // Read indexes
            for (int i = 0; i < l; i++) {
                this.mtxTab[i].setIndex(this.fontFile.readTTFUShort());

                if (this.mtxTab[i].getIndex() > 257) {
                    // Index is not in the Macintosh standard set
                    numGlyphStrings++;
                }

                if (log.isTraceEnabled()) {
                    log.trace("PostScript index: "
                            + this.mtxTab[i].getIndexAsString());
                }
            }

            // firstChar=minIndex;
            final String[] psGlyphsBuffer = new String[numGlyphStrings];
            if (log.isDebugEnabled()) {
                log.debug("Reading " + numGlyphStrings
                        + " glyphnames, that are not in the standard Macintosh"
                        + " set. Total number of glyphs=" + l);
            }
            for (int i = 0; i < psGlyphsBuffer.length; i++) {
                psGlyphsBuffer[i] = this.fontFile.readTTFString(this.fontFile
                        .readTTFUByte());
            }

            // Set glyph names
            for (int i = 0; i < l; i++) {
                if (this.mtxTab[i].getIndex() < MAC_GLYPH_ORDERING.length) {
                    this.mtxTab[i].setName(MAC_GLYPH_ORDERING[this.mtxTab[i]
                            .getIndex()]);
                } else {
                    if (!this.mtxTab[i].isIndexReserved()) {
                        final int k = this.mtxTab[i].getIndex()
                                - MAC_GLYPH_ORDERING.length;

                        if (log.isTraceEnabled()) {
                            log.trace(k + " i=" + i + " mtx="
                                    + this.mtxTab.length + " ps="
                                    + psGlyphsBuffer.length);
                        }

                        this.mtxTab[i].setName(psGlyphsBuffer[k]);
                    }
                }
            }

            break;
        case 0x00030000:
            // PostScript format 3 contains no glyph names
            log.debug("PostScript format 3");
            this.postScriptVersion = PostScriptVersion.V3;
            break;
        default:
            log.error("Unknown PostScript format: " + postFormat);
            this.postScriptVersion = PostScriptVersion.UNKNOWN;
        }
    }

    /**
     * Read the "OS/2" table
     */
    private void readOS2() throws IOException {
        // Check if font is embeddable
        final TTFDirTabEntry os2Entry = this.dirTabs.get(TTFTableName.OS2);
        if (os2Entry != null) {
            seekTab(this.fontFile, TTFTableName.OS2, 0);
            final int version = this.fontFile.readTTFUShort();
            if (log.isDebugEnabled()) {
                log.debug("OS/2 table: version=" + version + ", offset="
                        + os2Entry.getOffset() + ", len="
                        + os2Entry.getLength());
            }
            this.fontFile.skip(2); // xAvgCharWidth
            this.usWeightClass = this.fontFile.readTTFUShort();

            // usWidthClass
            this.fontFile.skip(2);

            final int fsType = this.fontFile.readTTFUShort();
            if (fsType == 2) {
                this.isEmbeddable = false;
            } else {
                this.isEmbeddable = true;
            }
            this.fontFile.skip(11 * 2);
            this.fontFile.skip(10); // panose array
            this.fontFile.skip(4 * 4); // unicode ranges
            this.fontFile.skip(4);
            this.fontFile.skip(3 * 2);
            int v;
            this.os2Ascender = this.fontFile.readTTFShort(); // sTypoAscender
            this.os2Descender = this.fontFile.readTTFShort(); // sTypoDescender
            if (log.isDebugEnabled()) {
                log.debug("sTypoAscender: " + this.os2Ascender
                        + " -> internal "
                        + convertTTFUnit2PDFUnit(this.os2Ascender));
                log.debug("sTypoDescender: " + this.os2Descender
                        + " -> internal "
                        + convertTTFUnit2PDFUnit(this.os2Descender));
            }
            v = this.fontFile.readTTFShort(); // sTypoLineGap
            if (log.isDebugEnabled()) {
                log.debug("sTypoLineGap: " + v);
            }
            v = this.fontFile.readTTFUShort(); // usWinAscent
            if (log.isDebugEnabled()) {
                log.debug("usWinAscent: " + formatUnitsForDebug(v));
            }
            v = this.fontFile.readTTFUShort(); // usWinDescent
            if (log.isDebugEnabled()) {
                log.debug("usWinDescent: " + formatUnitsForDebug(v));
            }

            // version 1 OS/2 table might end here
            if (os2Entry.getLength() >= 78 + 2 * 4 + 2 * 2) {
                this.fontFile.skip(2 * 4);
                this.os2xHeight = this.fontFile.readTTFShort(); // sxHeight
                this.os2CapHeight = this.fontFile.readTTFShort(); // sCapHeight
                if (log.isDebugEnabled()) {
                    log.debug("sxHeight: " + this.os2xHeight);
                    log.debug("sCapHeight: " + this.os2CapHeight);
                }
            }

        } else {
            this.isEmbeddable = true;
        }
    }

    /**
     * Read the "loca" table.
     *
     * @throws IOException
     *             In case of a I/O problem
     */
    protected final void readIndexToLocation() throws IOException {
        if (!seekTab(this.fontFile, TTFTableName.LOCA, 0)) {
            throw new IOException(
                    "'loca' table not found, happens when the font file doesn't"
                            + " contain TrueType outlines (trying to read an OpenType CFF font maybe?)");
        }
        for (int i = 0; i < this.numberOfGlyphs; i++) {
            this.mtxTab[i].setOffset(this.locaFormat == 1 ? this.fontFile
                    .readTTFULong() : this.fontFile.readTTFUShort() << 1);
        }
        this.lastLoca = this.locaFormat == 1 ? this.fontFile.readTTFULong()
                : this.fontFile.readTTFUShort() << 1;
    }

    /**
     * Read the "glyf" table to find the bounding boxes.
     *
     * @throws IOException
     *             In case of a I/O problem
     */
    private void readGlyf() throws IOException {
        final TTFDirTabEntry dirTab = this.dirTabs.get(TTFTableName.GLYF);
        if (dirTab == null) {
            throw new IOException("glyf table not found, cannot continue");
        }
        for (int i = 0; i < this.numberOfGlyphs - 1; i++) {
            if (this.mtxTab[i].getOffset() != this.mtxTab[i + 1].getOffset()) {
                this.fontFile.seekSet(dirTab.getOffset()
                        + this.mtxTab[i].getOffset());
                this.fontFile.skip(2);
                final int[] bbox = { this.fontFile.readTTFShort(),
                        this.fontFile.readTTFShort(),
                        this.fontFile.readTTFShort(),
                        this.fontFile.readTTFShort() };
                this.mtxTab[i].setBoundingBox(bbox);
            } else {
                this.mtxTab[i].setBoundingBox(this.mtxTab[0].getBoundingBox());
            }
        }

        final long n = this.dirTabs.get(TTFTableName.GLYF).getOffset();
        for (int i = 0; i < this.numberOfGlyphs; i++) {
            if (i + 1 >= this.mtxTab.length
                    || this.mtxTab[i].getOffset() != this.mtxTab[i + 1]
                            .getOffset()) {
                this.fontFile.seekSet(n + this.mtxTab[i].getOffset());
                this.fontFile.skip(2);
                final int[] bbox = { this.fontFile.readTTFShort(),
                        this.fontFile.readTTFShort(),
                        this.fontFile.readTTFShort(),
                        this.fontFile.readTTFShort() };
                this.mtxTab[i].setBoundingBox(bbox);
            } else {
                /**
                 * @todo Verify that this is correct, looks like a copy/paste
                 *       bug (jm)
                 */
                final int bbox0 = this.mtxTab[0].getBoundingBox()[0];
                final int[] bbox = { bbox0, bbox0, bbox0, bbox0 };
                this.mtxTab[i].setBoundingBox(bbox);
                /*
                 * Original code mtxTab[i].bbox[0] = mtxTab[0].bbox[0];
                 * mtxTab[i].bbox[1] = mtxTab[0].bbox[0]; mtxTab[i].bbox[2] =
                 * mtxTab[0].bbox[0]; mtxTab[i].bbox[3] = mtxTab[0].bbox[0];
                 */
            }
            if (log.isTraceEnabled()) {
                log.trace(this.mtxTab[i].toString(this));
            }
        }
    }

    /**
     * Read the "name" table.
     *
     * @throws IOException
     *             In case of a I/O problem
     */
    private void readName() throws IOException {
        seekTab(this.fontFile, TTFTableName.NAME, 2);
        int i = this.fontFile.getCurrentPos();
        int n = this.fontFile.readTTFUShort();
        final int j = this.fontFile.readTTFUShort() + i - 2;
        i += 2 * 2;

        while (n-- > 0) {
            // getLogger().debug("Iteration: " + n);
            this.fontFile.seekSet(i);
            final int platformID = this.fontFile.readTTFUShort();
            final int encodingID = this.fontFile.readTTFUShort();
            final int languageID = this.fontFile.readTTFUShort();

            final int k = this.fontFile.readTTFUShort();
            final int l = this.fontFile.readTTFUShort();

            if ((platformID == 1 || platformID == 3)
                    && (encodingID == 0 || encodingID == 1)) {
                this.fontFile.seekSet(j + this.fontFile.readTTFUShort());
                String txt;
                if (platformID == 3) {
                    txt = this.fontFile.readTTFString(l, encodingID);
                } else {
                    txt = this.fontFile.readTTFString(l);
                }

                if (log.isDebugEnabled()) {
                    log.debug(platformID + " " + encodingID + " " + languageID
                            + " " + k + " " + txt);
                }
                switch (k) {
                case 0:
                    if (this.notice.length() == 0) {
                        this.notice = txt;
                    }
                    break;
                case 1: // Font Family Name
                case 16: // Preferred Family
                    this.familyNames.add(txt);
                    break;
                case 2:
                    if (this.subFamilyName.length() == 0) {
                        this.subFamilyName = txt;
                    }
                    break;
                case 4:
                    if (this.fullName.length() == 0 || platformID == 3
                            && languageID == 1033) {
                        this.fullName = txt;
                    }
                    break;
                case 6:
                    if (this.postScriptName.length() == 0) {
                        this.postScriptName = txt;
                    }
                    break;
                default:
                    break;
                }
            }
            i += 6 * 2;
        }
    }

    /**
     * Read the "PCLT" table to find xHeight and capHeight.
     *
     * @throws IOException
     *             In case of a I/O problem
     */
    private boolean readPCLT() throws IOException {
        final TTFDirTabEntry dirTab = this.dirTabs.get(TTFTableName.PCLT);
        if (dirTab != null) {
            this.fontFile.seekSet(dirTab.getOffset() + 4 + 4 + 2);
            this.xHeight = this.fontFile.readTTFUShort();
            log.debug("xHeight from PCLT: " + formatUnitsForDebug(this.xHeight));
            this.fontFile.skip(2 * 2);
            this.capHeight = this.fontFile.readTTFUShort();
            log.debug("capHeight from PCLT: "
                    + formatUnitsForDebug(this.capHeight));
            this.fontFile.skip(2 + 16 + 8 + 6 + 1 + 1);

            int serifStyle = this.fontFile.readTTFUByte();
            serifStyle = serifStyle >> 6;
        serifStyle = serifStyle & 3;
        if (serifStyle == 1) {
            this.hasSerifs = false;
        } else {
            this.hasSerifs = true;
        }
        return true;
        } else {
            return false;
        }
    }

    /**
     * Determines the right source for the ascender and descender values. The
     * problem here is that the interpretation of these values is not the same
     * for every font. There doesn't seem to be a uniform definition of an
     * ascender and a descender. In some fonts the hhea values are defined after
     * the Apple interpretation, but not in every font. The same problem is in
     * the OS/2 table. FOP needs the ascender and descender to determine the
     * baseline so we need values which add up more or less to the "em box".
     * However, due to accent modifiers a character can grow beyond the em box.
     */
    private void determineAscDesc() {
        final int hheaBoxHeight = this.hheaAscender - this.hheaDescender;
        final int os2BoxHeight = this.os2Ascender - this.os2Descender;
        if (this.os2Ascender > 0 && os2BoxHeight <= this.upem) {
            this.ascender = this.os2Ascender;
            this.descender = this.os2Descender;
        } else if (this.hheaAscender > 0 && hheaBoxHeight <= this.upem) {
            this.ascender = this.hheaAscender;
            this.descender = this.hheaDescender;
        } else {
            if (this.os2Ascender > 0) {
                // Fall back to info from OS/2 if possible
                this.ascender = this.os2Ascender;
                this.descender = this.os2Descender;
            } else {
                this.ascender = this.hheaAscender;
                this.descender = this.hheaDescender;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Font box height: " + (this.ascender - this.descender));
            if (this.ascender - this.descender > this.upem) {
                log.debug("Ascender and descender together are larger than the em box.");
            }
        }
    }

    private void guessVerticalMetricsFromGlyphBBox() {
        // Approximate capHeight from height of "H"
        // It's most unlikely that a font misses the PCLT table
        // This also assumes that postscriptnames exists ("H")
        // Should look it up in the cmap (that wouldn't help
        // for charsets without H anyway...)
        // Same for xHeight with the letter "x"
        int localCapHeight = 0;
        int localXHeight = 0;
        int localAscender = 0;
        int localDescender = 0;
        for (final TTFMtxEntry element : this.mtxTab) {
            if ("H".equals(element.getName())) {
                localCapHeight = element.getBoundingBox()[3];
            } else if ("x".equals(element.getName())) {
                localXHeight = element.getBoundingBox()[3];
            } else if ("d".equals(element.getName())) {
                localAscender = element.getBoundingBox()[3];
            } else if ("p".equals(element.getName())) {
                localDescender = element.getBoundingBox()[1];
            } else {
                // OpenType Fonts with a version 3.0 "post" table don't have
                // glyph names.
                // Use Unicode indices instead.
                final List unicodeIndex = element.getUnicodeIndex();
                if (unicodeIndex.size() > 0) {
                    // Only the first index is used
                    final char ch = (char) ((Integer) unicodeIndex.get(0))
                            .intValue();
                    if (ch == 'H') {
                        localCapHeight = element.getBoundingBox()[3];
                    } else if (ch == 'x') {
                        localXHeight = element.getBoundingBox()[3];
                    } else if (ch == 'd') {
                        localAscender = element.getBoundingBox()[3];
                    } else if (ch == 'p') {
                        localDescender = element.getBoundingBox()[1];
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Ascender from glyph 'd': "
                    + formatUnitsForDebug(localAscender));
            log.debug("Descender from glyph 'p': "
                    + formatUnitsForDebug(localDescender));
        }
        if (this.ascender - this.descender > this.upem) {
            log.debug("Replacing specified ascender/descender with derived values to get values"
                    + " which fit in the em box.");
            this.ascender = localAscender;
            this.descender = localDescender;
        }

        if (log.isDebugEnabled()) {
            log.debug("xHeight from glyph 'x': "
                    + formatUnitsForDebug(localXHeight));
            log.debug("CapHeight from glyph 'H': "
                    + formatUnitsForDebug(localCapHeight));
        }
        if (this.capHeight == 0) {
            this.capHeight = localCapHeight;
            if (this.capHeight == 0) {
                this.capHeight = this.os2CapHeight;
            }
            if (this.capHeight == 0) {
                log.debug("capHeight value could not be determined."
                        + " The font may not work as expected.");
            }
        }
        if (this.xHeight == 0) {
            this.xHeight = localXHeight;
            if (this.xHeight == 0) {
                this.xHeight = this.os2xHeight;
            }
            if (this.xHeight == 0) {
                log.debug("xHeight value could not be determined."
                        + " The font may not work as expected.");
            }
        }
    }

    /**
     * Read the kerning table, create a table for both CIDs and winAnsiEncoding.
     *
     * @throws IOException
     *             In case of a I/O problem
     */
    private void readKerning() throws IOException {
        // Read kerning
        this.kerningTab = new HashMap<Integer, Map<Integer, Integer>>();
        this.ansiKerningTab = new HashMap<Integer, Map<Integer, Integer>>();
        final TTFDirTabEntry dirTab = this.dirTabs.get(TTFTableName.KERN);
        if (dirTab != null) {
            seekTab(this.fontFile, TTFTableName.KERN, 2);
            for (int n = this.fontFile.readTTFUShort(); n > 0; n--) {
                this.fontFile.skip(2 * 2);
                int k = this.fontFile.readTTFUShort();
                if (!((k & 1) != 0) || (k & 2) != 0 || (k & 4) != 0) {
                    return;
                }
                if (k >> 8 != 0) {
                    continue;
                }

                k = this.fontFile.readTTFUShort();
                this.fontFile.skip(3 * 2);
                while (k-- > 0) {
                    final int i = this.fontFile.readTTFUShort();
                    final int j = this.fontFile.readTTFUShort();
                    final int kpx = this.fontFile.readTTFShort();
                    if (kpx != 0) {
                        // CID kerning table entry, using unicode indexes
                        final Integer iObj = glyphToUnicode(i);
                        final Integer u2 = glyphToUnicode(j);
                        if (iObj == null) {
                            // happens for many fonts (Ubuntu font set),
                            // stray entries in the kerning table??
                            log.debug("Ignoring kerning pair because no Unicode index was"
                                    + " found for the first glyph " + i);
                        } else if (u2 == null) {
                            log.debug("Ignoring kerning pair because Unicode index was"
                                    + " found for the second glyph " + i);
                        } else {
                            Map<Integer, Integer> adjTab = this.kerningTab
                                    .get(iObj);
                            if (adjTab == null) {
                                adjTab = new HashMap<Integer, Integer>();
                            }
                            adjTab.put(u2, new Integer(
                                    convertTTFUnit2PDFUnit(kpx)));
                            this.kerningTab.put(iObj, adjTab);
                        }
                    }
                }
            }

            // Create winAnsiEncoded kerning table from kerningTab
            // (could probably be simplified, for now we remap back to CID
            // indexes and
            // then to winAnsi)
            for (final Integer unicodeKey1 : this.kerningTab.keySet()) {
                final Integer cidKey1 = unicodeToGlyph(unicodeKey1.intValue());
                final Map<Integer, Integer> akpx = new HashMap<Integer, Integer>();
                final Map<Integer, Integer> ckpx = this.kerningTab
                        .get(unicodeKey1);

                for (final Integer unicodeKey2 : ckpx.keySet()) {
                    final Integer cidKey2 = unicodeToGlyph(unicodeKey2
                            .intValue());
                    final Integer kern = ckpx.get(unicodeKey2);

                    final Iterator uniMap = this.mtxTab[cidKey2.intValue()]
                            .getUnicodeIndex().listIterator();
                    while (uniMap.hasNext()) {
                        final Integer unicodeKey = (Integer) uniMap.next();
                        final Integer[] ansiKeys = unicodeToWinAnsi(unicodeKey
                                .intValue());
                        for (final Integer ansiKey : ansiKeys) {
                            akpx.put(ansiKey, kern);
                        }
                    }
                }

                if (akpx.size() > 0) {
                    final Iterator uniMap = this.mtxTab[cidKey1.intValue()]
                            .getUnicodeIndex().listIterator();
                    while (uniMap.hasNext()) {
                        final Integer unicodeKey = (Integer) uniMap.next();
                        final Integer[] ansiKeys = unicodeToWinAnsi(unicodeKey
                                .intValue());
                        for (final Integer ansiKey : ansiKeys) {
                            this.ansiKerningTab.put(ansiKey, akpx);
                        }
                    }
                }
            }
        }
    }

    /**
     * Streams a font.
     *
     * @param ttfOut
     *            The interface for streaming TrueType tables.
     * @exception IOException
     *                file write error
     */
    public void stream(final TTFOutputStream ttfOut) throws IOException {
        final SortedSet<Map.Entry<TTFTableName, TTFDirTabEntry>> sortedDirTabs = sortDirTabMap(this.dirTabs);
        final byte[] file = this.fontFile.getAllBytes();
        final TTFTableOutputStream tableOut = ttfOut.getTableOutputStream();
        final TTFGlyphOutputStream glyphOut = ttfOut.getGlyphOutputStream();
        ttfOut.startFontStream();
        for (final Map.Entry<TTFTableName, TTFDirTabEntry> entry : sortedDirTabs) {
            final int offset = (int) entry.getValue().getOffset();
            int paddedLength = (int) entry.getValue().getLength();
            paddedLength += getPadSize(offset + paddedLength);
            if (entry.getKey().equals(TTFTableName.GLYF)) {
                streamGlyf(glyphOut, file, offset, paddedLength);
            } else {
                tableOut.streamTable(file, offset, paddedLength);
            }
        }
        ttfOut.endFontStream();
    }

    private void streamGlyf(final TTFGlyphOutputStream glyphOut,
            final byte[] fontFile, final int tableOffset, final int tableLength)
            throws IOException {
        // Stream all but the last glyph
        int glyphStart = 0;
        int glyphEnd = 0;
        glyphOut.startGlyphStream();
        for (int i = 0; i < this.mtxTab.length - 1; i++) {
            glyphStart = (int) this.mtxTab[i].getOffset() + tableOffset;
            glyphEnd = (int) this.mtxTab[i + 1].getOffset() + tableOffset;
            glyphOut.streamGlyph(fontFile, glyphStart, glyphEnd - glyphStart);
        }
        glyphOut.streamGlyph(fontFile, glyphEnd, tableOffset + tableLength
                - glyphEnd);
        glyphOut.endGlyphStream();
    }

    /**
     * Returns the order in which the tables in a TrueType font should be
     * written to file.
     *
     * @param directoryTabs
     *            the map that is to be sorted.
     * @return TTFTablesNames[] an array of table names sorted in the order they
     *         should appear in the TTF file.
     */
    SortedSet<Map.Entry<TTFTableName, TTFDirTabEntry>> sortDirTabMap(
            final Map<TTFTableName, TTFDirTabEntry> directoryTabs) {
        final SortedSet<Map.Entry<TTFTableName, TTFDirTabEntry>> sortedSet = new TreeSet<Map.Entry<TTFTableName, TTFDirTabEntry>>(
                new Comparator<Map.Entry<TTFTableName, TTFDirTabEntry>>() {

                    @Override
                    public int compare(
                            final Entry<TTFTableName, TTFDirTabEntry> o1,
                            final Entry<TTFTableName, TTFDirTabEntry> o2) {
                        return (int) (o1.getValue().getOffset() - o2.getValue()
                                .getOffset());
                    }
                });
        sortedSet.addAll(directoryTabs.entrySet());
        return sortedSet;
    }

    /**
     * Returns this font's character to glyph mapping.
     *
     * @return the font's cmap
     */
    public List<CMapSegment> getCMaps() {
        return this.cmaps;
    }

    /**
     * Check if this is a TrueType collection and that the given name exists in
     * the collection. If it does, set offset in fontfile to the beginning of
     * the Table Directory for that font.
     *
     * @param name
     *            The name to check
     * @return True if not collection or font name present, false otherwise
     * @throws IOException
     *             In case of an I/O problem
     */
    protected final boolean checkTTC(final String name) throws IOException {
        final String tag = this.fontFile.readTTFString(4);

        if ("ttcf".equals(tag)) {
            // This is a TrueType Collection
            this.fontFile.skip(4);

            // Read directory offsets
            final int numDirectories = (int) this.fontFile.readTTFULong();
            // int numDirectories=in.readTTFUShort();
            final long[] dirOffsets = new long[numDirectories];
            for (int i = 0; i < numDirectories; i++) {
                dirOffsets[i] = this.fontFile.readTTFULong();
            }

            log.info("This is a TrueType collection file with "
                    + numDirectories + " fonts");
            log.info("Containing the following fonts: ");
            // Read all the directories and name tables to check
            // If the font exists - this is a bit ugly, but...
            boolean found = false;

            // Iterate through all name tables even if font
            // Is found, just to show all the names
            long dirTabOffset = 0;
            for (int i = 0; i < numDirectories; i++) {
                this.fontFile.seekSet(dirOffsets[i]);
                readDirTabs();

                readName();

                if (this.fullName.equals(name)) {
                    found = true;
                    dirTabOffset = dirOffsets[i];
                    log.info(this.fullName + " <-- selected");
                } else {
                    log.info(this.fullName);
                }

                // Reset names
                this.notice = "";
                this.fullName = "";
                this.familyNames.clear();
                this.postScriptName = "";
                this.subFamilyName = "";
            }

            this.fontFile.seekSet(dirTabOffset);
            return found;
        } else {
            this.fontFile.seekSet(0);
            return true;
        }
    }

    /**
     * Return TTC font names
     *
     * @param in
     *            FontFileReader to read from
     * @return True if not collection or font name present, false otherwise
     * @throws IOException
     *             In case of an I/O problem
     */
    public final List<String> getTTCnames(final FontFileReader in)
            throws IOException {
        this.fontFile = in;

        final List<String> fontNames = new ArrayList<String>();
        final String tag = in.readTTFString(4);

        if ("ttcf".equals(tag)) {
            // This is a TrueType Collection
            in.skip(4);

            // Read directory offsets
            final int numDirectories = (int) in.readTTFULong();
            final long[] dirOffsets = new long[numDirectories];
            for (int i = 0; i < numDirectories; i++) {
                dirOffsets[i] = in.readTTFULong();
            }

            log.info("This is a TrueType collection file with "
                    + numDirectories + " fonts");
            log.info("Containing the following fonts: ");

            for (int i = 0; i < numDirectories; i++) {
                in.seekSet(dirOffsets[i]);
                readDirTabs();

                readName();

                log.info(this.fullName);
                fontNames.add(this.fullName);

                // Reset names
                this.notice = "";
                this.fullName = "";
                this.familyNames.clear();
                this.postScriptName = "";
                this.subFamilyName = "";
            }

            in.seekSet(0);
            return fontNames;
        } else {
            log.error("Not a TTC!");
            return null;
        }
    }

    /*
     * Helper classes, they are not very efficient, but that really doesn't
     * matter...
     */
    private Integer[] unicodeToWinAnsi(final int unicode) {
        final List<Integer> ret = new ArrayList<Integer>();
        for (int i = 32; i < Glyphs.WINANSI_ENCODING.length; i++) {
            if (unicode == Glyphs.WINANSI_ENCODING[i]) {
                ret.add(new Integer(i));
            }
        }
        return ret.toArray(new Integer[0]);
    }

    /**
     * Dumps a few informational values to System.out.
     */
    public void printStuff() {
        log.info("Font name:   " + this.postScriptName);
        log.info("Full name:   " + this.fullName);
        log.info("Family name: " + this.familyNames);
        log.info("Subfamily name: " + this.subFamilyName);
        log.info("Notice:      " + this.notice);
        log.info("xHeight:     " + convertTTFUnit2PDFUnit(this.xHeight));
        log.info("capheight:   " + convertTTFUnit2PDFUnit(this.capHeight));

        final int italic = (int) (this.italicAngle >> 16);
        log.info("Italic:      " + italic);
        System.out
                .print("ItalicAngle: " + (short) (this.italicAngle / 0x10000));
        if (this.italicAngle % 0x10000 > 0) {
            System.out.print("." + (short) (this.italicAngle % 0x10000 * 1000)
                    / 0x10000);
        }
        log.info("");
        log.info("Ascender:    " + convertTTFUnit2PDFUnit(this.ascender));
        log.info("Descender:   " + convertTTFUnit2PDFUnit(this.descender));
        log.info("FontBBox:    [" + convertTTFUnit2PDFUnit(this.fontBBox1)
                + " " + convertTTFUnit2PDFUnit(this.fontBBox2) + " "
                + convertTTFUnit2PDFUnit(this.fontBBox3) + " "
                + convertTTFUnit2PDFUnit(this.fontBBox4) + "]");
    }

    private String formatUnitsForDebug(final int units) {
        return units + " -> " + convertTTFUnit2PDFUnit(units)
                + " internal units";
    }

    /**
     * Map a glyph index to the corresponding unicode code point
     *
     * @param glyphIndex
     * @return unicode code point
     */
    private Integer glyphToUnicode(final int glyphIndex) {
        return this.glyphToUnicodeMap.get(new Integer(glyphIndex));
    }

    /**
     * Map a unicode code point to the corresponding glyph index
     *
     * @param unicodeIndex
     *            unicode code point
     * @return glyph index
     */
    private Integer unicodeToGlyph(final int unicodeIndex) throws IOException {
        final Integer result = this.unicodeToGlyphMap.get(new Integer(
                unicodeIndex));
        if (result == null) {
            throw new IOException("Glyph index not found for unicode value "
                    + unicodeIndex);
        }
        return result;
    }

    String getGlyphName(final int glyphIndex) {
        return this.mtxTab[glyphIndex].getName();
    }

    /**
     * Determine if advanced (typographic) table is present.
     *
     * @return true if advanced (typographic) table is present
     */
    public boolean hasAdvancedTable() {
        if (this.advancedTableReader != null) {
            return this.advancedTableReader.hasAdvancedTable();
        } else {
            return false;
        }
    }

    /**
     * Returns the GDEF table or null if none present.
     *
     * @return the GDEF table
     */
    public GlyphDefinitionTable getGDEF() {
        if (this.advancedTableReader != null) {
            return this.advancedTableReader.getGDEF();
        } else {
            return null;
        }
    }

    /**
     * Returns the GSUB table or null if none present.
     *
     * @return the GSUB table
     */
    public GlyphSubstitutionTable getGSUB() {
        if (this.advancedTableReader != null) {
            return this.advancedTableReader.getGSUB();
        } else {
            return null;
        }
    }

    /**
     * Returns the GPOS table or null if none present.
     *
     * @return the GPOS table
     */
    public GlyphPositioningTable getGPOS() {
        if (this.advancedTableReader != null) {
            return this.advancedTableReader.getGPOS();
        } else {
            return null;
        }
    }

    /**
     * Static main method to get info about a TrueType font.
     *
     * @param args
     *            The command line arguments
     */
    public static void main(final String[] args) {
        try {
            final boolean useKerning = true;
            final boolean useAdvanced = true;
            final TTFFile ttfFile = new TTFFile(useKerning, useAdvanced);

            final FontFileReader reader = new FontFileReader(args[0]);

            String name = null;
            if (args.length >= 2) {
                name = args[1];
            }

            ttfFile.readFont(reader, name);
            ttfFile.printStuff();

        } catch (final IOException ioe) {
            System.err.println("Problem reading font: " + ioe.toString());
            ioe.printStackTrace(System.err);
        }
    }
}
