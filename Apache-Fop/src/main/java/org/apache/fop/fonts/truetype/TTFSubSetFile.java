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

/* $Id: TTFSubSetFile.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.fonts.truetype;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import lombok.extern.slf4j.Slf4j;

/**
 * Reads a TrueType file and generates a subset that can be used to embed a
 * TrueType CID font. TrueType tables needed for embedded CID fonts are: "head",
 * "hhea", "loca", "maxp", "cvt ", "prep", "glyf", "hmtx" and "fpgm". The
 * TrueType spec can be found at the Microsoft Typography site:
 * http://www.microsoft.com/truetype/
 */
@Slf4j
public class TTFSubSetFile extends TTFFile {

    private byte[] output = null;
    private int realSize = 0;
    private int currentPos = 0;

    /*
     * Offsets in name table to be filled out by table. The offsets are to the
     * checkSum field
     */
    private final Map<TTFTableName, Integer> offsets = new HashMap<TTFTableName, Integer>();

    private int checkSumAdjustmentOffset = 0;
    private int locaOffset = 0;

    /** Stores the glyph offsets so that we can end strings at glyph boundaries */
    private int[] glyphOffsets;

    /**
     * Default Constructor
     */
    public TTFSubSetFile() {
    }

    /**
     * Constructor
     *
     * @param useKerning
     *            true if kerning data should be loaded
     * @param useAdvanced
     *            true if advanced typographic tables should be loaded
     */
    public TTFSubSetFile(final boolean useKerning, final boolean useAdvanced) {
        super(useKerning, useAdvanced);
    }

    /** The dir tab entries in the new subset font. */
    private final Map<TTFTableName, TTFDirTabEntry> newDirTabs = new HashMap<TTFTableName, TTFDirTabEntry>();

    private int determineTableCount() {
        int numTables = 4; // 4 req'd tables: head,hhea,hmtx,maxp
        if (isCFF()) {
            throw new UnsupportedOperationException(
                    "OpenType fonts with CFF glyphs are not supported");
        } else {
            numTables += 5; // 5 req'd tables: glyf,loca,post,name,OS/2
            if (hasCvt()) {
                numTables++;
            }
            if (hasFpgm()) {
                numTables++;
            }
            if (hasPrep()) {
                numTables++;
            }
        }
        return numTables;
    }

    /**
     * Create the directory table
     */
    private void createDirectory() {
        final int numTables = determineTableCount();
        // Create the TrueType header
        writeByte((byte) 0);
        writeByte((byte) 1);
        writeByte((byte) 0);
        writeByte((byte) 0);
        this.realSize += 4;

        writeUShort(numTables);
        this.realSize += 2;

        // Create searchRange, entrySelector and rangeShift
        final int maxPow = maxPow2(numTables);
        final int searchRange = (int) Math.pow(2, maxPow) * 16;
        writeUShort(searchRange);
        this.realSize += 2;

        writeUShort(maxPow);
        this.realSize += 2;

        writeUShort(numTables * 16 - searchRange);
        this.realSize += 2;
        // Create space for the table entries (these must be in ASCII
        // alphabetical order[A-Z] then[a-z])
        writeTableName(TTFTableName.OS2);

        if (hasCvt()) {
            writeTableName(TTFTableName.CVT);
        }
        if (hasFpgm()) {
            writeTableName(TTFTableName.FPGM);
        }
        writeTableName(TTFTableName.GLYF);
        writeTableName(TTFTableName.HEAD);
        writeTableName(TTFTableName.HHEA);
        writeTableName(TTFTableName.HMTX);
        writeTableName(TTFTableName.LOCA);
        writeTableName(TTFTableName.MAXP);
        writeTableName(TTFTableName.NAME);
        writeTableName(TTFTableName.POST);
        if (hasPrep()) {
            writeTableName(TTFTableName.PREP);
        }
        this.newDirTabs.put(TTFTableName.TABLE_DIRECTORY, new TTFDirTabEntry(0,
                this.currentPos));
    }

    private void writeTableName(final TTFTableName tableName) {
        writeString(tableName.getName());
        this.offsets.put(tableName, this.currentPos);
        this.currentPos += 12;
        this.realSize += 16;
    }

    private boolean hasCvt() {
        return this.dirTabs.containsKey(TTFTableName.CVT);
    }

    private boolean hasFpgm() {
        return this.dirTabs.containsKey(TTFTableName.FPGM);
    }

    private boolean hasPrep() {
        return this.dirTabs.containsKey(TTFTableName.PREP);
    }

    /**
     * Create an empty loca table without updating checksum
     */
    private void createLoca(final int size) {
        pad4();
        this.locaOffset = this.currentPos;
        final int dirTableOffset = this.offsets.get(TTFTableName.LOCA);
        writeULong(dirTableOffset + 4, this.currentPos);
        writeULong(dirTableOffset + 8, size * 4 + 4);
        this.currentPos += size * 4 + 4;
        this.realSize += size * 4 + 4;
    }

    private boolean copyTable(final FontFileReader in,
            final TTFTableName tableName) throws IOException {
        final TTFDirTabEntry entry = this.dirTabs.get(tableName);
        if (entry != null) {
            pad4();
            seekTab(in, tableName, 0);
            System.arraycopy(
                    in.getBytes((int) entry.getOffset(),
                            (int) entry.getLength()), 0, this.output,
                    this.currentPos, (int) entry.getLength());

            updateCheckSum(this.currentPos, (int) entry.getLength(), tableName);
            this.currentPos += (int) entry.getLength();
            this.realSize += (int) entry.getLength();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Copy the cvt table as is from original font to subset font
     */
    private boolean createCvt(final FontFileReader in) throws IOException {
        return copyTable(in, TTFTableName.CVT);
    }

    /**
     * Copy the fpgm table as is from original font to subset font
     */
    private boolean createFpgm(final FontFileReader in) throws IOException {
        return copyTable(in, TTFTableName.FPGM);
    }

    /**
     * Copy the name table as is from the original.
     */
    private boolean createName(final FontFileReader in) throws IOException {
        return copyTable(in, TTFTableName.NAME);
    }

    /**
     * Copy the OS/2 table as is from the original.
     */
    private boolean createOS2(final FontFileReader in) throws IOException {
        return copyTable(in, TTFTableName.OS2);
    }

    /**
     * Copy the maxp table as is from original font to subset font and set num
     * glyphs to size
     */
    private void createMaxp(final FontFileReader in, final int size)
            throws IOException {
        final TTFTableName maxp = TTFTableName.MAXP;
        final TTFDirTabEntry entry = this.dirTabs.get(maxp);
        if (entry != null) {
            pad4();
            seekTab(in, maxp, 0);
            System.arraycopy(
                    in.getBytes((int) entry.getOffset(),
                            (int) entry.getLength()), 0, this.output,
                    this.currentPos, (int) entry.getLength());
            writeUShort(this.currentPos + 4, size);

            updateCheckSum(this.currentPos, (int) entry.getLength(), maxp);
            this.currentPos += (int) entry.getLength();
            this.realSize += (int) entry.getLength();
        } else {
            throw new IOException("Can't find maxp table");
        }
    }

    private void createPost(final FontFileReader in) throws IOException {
        final TTFTableName post = TTFTableName.POST;
        final TTFDirTabEntry entry = this.dirTabs.get(post);
        if (entry != null) {
            pad4();
            seekTab(in, post, 0);
            final int newTableSize = 32; // This is the post table size with
            // glyphs truncated
            final byte[] newPostTable = new byte[newTableSize];
            // We only want the first 28 bytes (truncate the glyph names);
            System.arraycopy(
                    in.getBytes((int) entry.getOffset(), newTableSize), 0,
                    newPostTable, 0, newTableSize);
            // set the post table to Format 3.0
            newPostTable[1] = 0x03;
            System.arraycopy(newPostTable, 0, this.output, this.currentPos,
                    newTableSize);
            updateCheckSum(this.currentPos, newTableSize, post);
            this.currentPos += newTableSize;
            this.realSize += newTableSize;
        } else {
            throw new IOException("Can't find post table");
        }
    }

    /**
     * Copy the prep table as is from original font to subset font
     */
    private boolean createPrep(final FontFileReader in) throws IOException {
        return copyTable(in, TTFTableName.PREP);
    }

    /**
     * Copy the hhea table as is from original font to subset font and fill in
     * size of hmtx table
     */
    private void createHhea(final FontFileReader in, final int size)
            throws IOException {
        final TTFDirTabEntry entry = this.dirTabs.get(TTFTableName.HHEA);
        if (entry != null) {
            pad4();
            seekTab(in, TTFTableName.HHEA, 0);
            System.arraycopy(
                    in.getBytes((int) entry.getOffset(),
                            (int) entry.getLength()), 0, this.output,
                    this.currentPos, (int) entry.getLength());
            writeUShort((int) entry.getLength() + this.currentPos - 2, size);

            updateCheckSum(this.currentPos, (int) entry.getLength(),
                    TTFTableName.HHEA);
            this.currentPos += (int) entry.getLength();
            this.realSize += (int) entry.getLength();
        } else {
            throw new IOException("Can't find hhea table");
        }
    }

    /**
     * Copy the head table as is from original font to subset font and set
     * indexToLocaFormat to long and set checkSumAdjustment to 0, store offset
     * to checkSumAdjustment in checkSumAdjustmentOffset
     */
    private void createHead(final FontFileReader in) throws IOException {
        final TTFTableName head = TTFTableName.HEAD;
        final TTFDirTabEntry entry = this.dirTabs.get(head);
        if (entry != null) {
            pad4();
            seekTab(in, head, 0);
            System.arraycopy(
                    in.getBytes((int) entry.getOffset(),
                            (int) entry.getLength()), 0, this.output,
                    this.currentPos, (int) entry.getLength());

            this.checkSumAdjustmentOffset = this.currentPos + 8;
            this.output[this.currentPos + 8] = 0; // Set checkSumAdjustment to 0
            this.output[this.currentPos + 9] = 0;
            this.output[this.currentPos + 10] = 0;
            this.output[this.currentPos + 11] = 0;
            this.output[this.currentPos + 50] = 0; // long locaformat
            this.output[this.currentPos + 51] = 1; // long locaformat

            updateCheckSum(this.currentPos, (int) entry.getLength(), head);
            this.currentPos += (int) entry.getLength();
            this.realSize += (int) entry.getLength();
        } else {
            throw new IOException("Can't find head table");
        }
    }

    /**
     * Create the glyf table and fill in loca table
     */
    private void createGlyf(final FontFileReader in,
            final Map<Integer, Integer> glyphs) throws IOException {
        final TTFTableName glyf = TTFTableName.GLYF;
        final TTFDirTabEntry entry = this.dirTabs.get(glyf);
        int size = 0;
        int startPos = 0;
        int endOffset = 0; // Store this as the last loca
        if (entry != null) {
            pad4();
            startPos = this.currentPos;

            /*
             * Loca table must be in order by glyph index, so build an array
             * first and then write the glyph info and location offset.
             */
            final int[] origIndexes = buildSubsetIndexToOrigIndexMap(glyphs);
            this.glyphOffsets = new int[origIndexes.length];

            for (int i = 0; i < origIndexes.length; i++) {
                int nextOffset = 0;
                final int origGlyphIndex = origIndexes[i];
                if (origGlyphIndex >= this.mtxTab.length - 1) {
                    nextOffset = (int) this.lastLoca;
                } else {
                    nextOffset = (int) this.mtxTab[origGlyphIndex + 1]
                            .getOffset();
                }
                final int glyphOffset = (int) this.mtxTab[origGlyphIndex]
                        .getOffset();
                final int glyphLength = nextOffset - glyphOffset;

                final byte[] glyphData = in.getBytes((int) entry.getOffset()
                        + glyphOffset, glyphLength);
                int endOffset1 = endOffset;
                // Copy glyph
                System.arraycopy(glyphData, 0, this.output, this.currentPos,
                        glyphLength);

                // Update loca table
                writeULong(this.locaOffset + i * 4, this.currentPos - startPos);
                if (this.currentPos - startPos + glyphLength > endOffset1) {
                    endOffset1 = this.currentPos - startPos + glyphLength;
                }

                // Store the glyph boundary positions relative to the start of
                // the font
                this.glyphOffsets[i] = this.currentPos;
                this.currentPos += glyphLength;
                this.realSize += glyphLength;

                endOffset = endOffset1;
            }

            size = this.currentPos - startPos;

            this.currentPos += 12;
            this.realSize += 12;
            updateCheckSum(startPos, size + 12, glyf);

            // Update loca checksum and last loca index
            writeULong(this.locaOffset + glyphs.size() * 4, endOffset);
            final int locaSize = glyphs.size() * 4 + 4;
            final int checksum = getCheckSum(this.output, this.locaOffset,
                    locaSize);
            writeULong(this.offsets.get(TTFTableName.LOCA), checksum);
            final int padSize = (this.locaOffset + locaSize) % 4;
            this.newDirTabs.put(TTFTableName.LOCA, new TTFDirTabEntry(
                    this.locaOffset, locaSize + padSize));
        } else {
            throw new IOException("Can't find glyf table");
        }
    }

    private int[] buildSubsetIndexToOrigIndexMap(
            final Map<Integer, Integer> glyphs) {
        final int[] origIndexes = new int[glyphs.size()];
        for (final Map.Entry<Integer, Integer> glyph : glyphs.entrySet()) {
            final int origIndex = glyph.getKey();
            final int subsetIndex = glyph.getValue();
            origIndexes[subsetIndex] = origIndex;
        }
        return origIndexes;
    }

    /**
     * Create the hmtx table by copying metrics from original font to subset
     * font. The glyphs Map contains an Integer key and Integer value that maps
     * the original metric (key) to the subset metric (value)
     */
    private void createHmtx(final FontFileReader in,
            final Map<Integer, Integer> glyphs) throws IOException {
        final TTFTableName hmtx = TTFTableName.HMTX;
        final TTFDirTabEntry entry = this.dirTabs.get(hmtx);

        final int longHorMetricSize = glyphs.size() * 2;
        final int leftSideBearingSize = glyphs.size() * 2;
        final int hmtxSize = longHorMetricSize + leftSideBearingSize;

        if (entry != null) {
            pad4();
            // int offset = (int)entry.offset;
            for (final Map.Entry<Integer, Integer> glyph : glyphs.entrySet()) {
                final Integer origIndex = glyph.getKey();
                final Integer subsetIndex = glyph.getValue();

                writeUShort(this.currentPos + subsetIndex.intValue() * 4,
                        this.mtxTab[origIndex.intValue()].getWx());
                writeUShort(this.currentPos + subsetIndex.intValue() * 4 + 2,
                        this.mtxTab[origIndex.intValue()].getLsb());
            }

            updateCheckSum(this.currentPos, hmtxSize, hmtx);
            this.currentPos += hmtxSize;
            this.realSize += hmtxSize;
        } else {
            throw new IOException("Can't find hmtx table");
        }
    }

    /**
     * Reads a font and creates a subset of the font.
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
    @Override
    public void readFont(final FontFileReader in, final String name,
            final Map<Integer, Integer> glyphs) throws IOException {
        this.fontFile = in;
        // Check if TrueType collection, and that the name exists in the
        // collection
        if (!checkTTC(name)) {
            throw new IOException("Failed to read font");
        }

        // Copy the Map as we're going to modify it
        final Map<Integer, Integer> subsetGlyphs = new HashMap<Integer, Integer>(
                glyphs);

        this.output = new byte[in.getFileSize()];

        readDirTabs();
        readFontHeader();
        getNumGlyphs();
        readHorizontalHeader();
        readHorizontalMetrics();
        readIndexToLocation();

        scanGlyphs(in, subsetGlyphs);

        createDirectory(); // Create the TrueType header and directory

        boolean optionalTableFound;
        optionalTableFound = createCvt(in); // copy the cvt table
        if (!optionalTableFound) {
            // cvt is optional (used in TrueType fonts only)
            log.debug("TrueType: ctv table not present. Skipped.");
        }

        optionalTableFound = createFpgm(in); // copy fpgm table
        if (!optionalTableFound) {
            // fpgm is optional (used in TrueType fonts only)
            log.debug("TrueType: fpgm table not present. Skipped.");
        }
        createLoca(subsetGlyphs.size()); // create empty loca table
        createGlyf(in, subsetGlyphs); // create glyf table and update loca table

        createOS2(in); // copy the OS/2 table
        createHead(in);
        createHhea(in, subsetGlyphs.size()); // Create the hhea table
        createHmtx(in, subsetGlyphs); // Create hmtx table
        createMaxp(in, subsetGlyphs.size()); // copy the maxp table
        createName(in); // copy the name table
        createPost(in); // copy the post table

        optionalTableFound = createPrep(in); // copy prep table
        if (!optionalTableFound) {
            // prep is optional (used in TrueType fonts only)
            log.debug("TrueType: prep table not present. Skipped.");
        }

        pad4();
        createCheckSumAdjustment();
    }

    /**
     * Returns a subset of the fonts (readFont() MUST be called first in order
     * to create the subset).
     *
     * @return byte array
     */
    public byte[] getFontSubset() {
        final byte[] ret = new byte[this.realSize];
        System.arraycopy(this.output, 0, ret, 0, this.realSize);
        return ret;
    }

    private void handleGlyphSubset(final TTFGlyphOutputStream glyphOut)
            throws IOException {
        glyphOut.startGlyphStream();
        // Stream all but the last glyph
        for (int i = 0; i < this.glyphOffsets.length - 1; i++) {
            glyphOut.streamGlyph(this.output, this.glyphOffsets[i],
                    this.glyphOffsets[i + 1] - this.glyphOffsets[i]);
        }
        // Stream the last glyph
        final TTFDirTabEntry glyf = this.newDirTabs.get(TTFTableName.GLYF);
        final long lastGlyphLength = glyf.getLength()
                - (this.glyphOffsets[this.glyphOffsets.length - 1] - glyf
                        .getOffset());
        glyphOut.streamGlyph(this.output,
                this.glyphOffsets[this.glyphOffsets.length - 1],
                (int) lastGlyphLength);
        glyphOut.endGlyphStream();
    }

    @Override
    public void stream(final TTFOutputStream ttfOut) throws IOException {
        final SortedSet<Map.Entry<TTFTableName, TTFDirTabEntry>> sortedDirTabs = sortDirTabMap(this.newDirTabs);
        final TTFTableOutputStream tableOut = ttfOut.getTableOutputStream();
        final TTFGlyphOutputStream glyphOut = ttfOut.getGlyphOutputStream();

        ttfOut.startFontStream();
        for (final Map.Entry<TTFTableName, TTFDirTabEntry> entry : sortedDirTabs) {
            if (entry.getKey().equals(TTFTableName.GLYF)) {
                handleGlyphSubset(glyphOut);
            } else {
                tableOut.streamTable(this.output, (int) entry.getValue()
                        .getOffset(), (int) entry.getValue().getLength());
            }
        }
        ttfOut.endFontStream();
    }

    private void scanGlyphs(final FontFileReader in,
            final Map<Integer, Integer> subsetGlyphs) throws IOException {
        final TTFDirTabEntry glyfTableInfo = this.dirTabs
                .get(TTFTableName.GLYF);
        if (glyfTableInfo == null) {
            throw new IOException("Glyf table could not be found");
        }

        final GlyfTable glyfTable = new GlyfTable(in, this.mtxTab,
                glyfTableInfo, subsetGlyphs);
        glyfTable.populateGlyphsWithComposites();
    }

    /**
     * writes a ISO-8859-1 string at the currentPosition updates currentPosition
     * but not realSize
     *
     * @return number of bytes written
     */
    private int writeString(final String str) {
        int length = 0;
        try {
            final byte[] buf = str.getBytes("ISO-8859-1");
            System.arraycopy(buf, 0, this.output, this.currentPos, buf.length);
            length = buf.length;
            this.currentPos += length;
        } catch (final java.io.UnsupportedEncodingException e) {
            // This should never happen!
        }

        return length;
    }

    /**
     * Appends a byte to the output array, updates currentPost but not realSize
     */
    private void writeByte(final byte b) {
        this.output[this.currentPos++] = b;
    }

    /**
     * Appends a USHORT to the output array, updates currentPost but not
     * realSize
     */
    private void writeUShort(final int s) {
        final byte b1 = (byte) (s >> 8 & 0xff);
        final byte b2 = (byte) (s & 0xff);
        writeByte(b1);
        writeByte(b2);
    }

    /**
     * Appends a USHORT to the output array, at the given position without
     * changing currentPos
     */
    private void writeUShort(final int pos, final int s) {
        final byte b1 = (byte) (s >> 8 & 0xff);
        final byte b2 = (byte) (s & 0xff);
        this.output[pos] = b1;
        this.output[pos + 1] = b2;
    }

    /**
     * Appends a ULONG to the output array, at the given position without
     * changing currentPos
     */
    private void writeULong(final int pos, final int s) {
        final byte b1 = (byte) (s >> 24 & 0xff);
        final byte b2 = (byte) (s >> 16 & 0xff);
        final byte b3 = (byte) (s >> 8 & 0xff);
        final byte b4 = (byte) (s & 0xff);
        this.output[pos] = b1;
        this.output[pos + 1] = b2;
        this.output[pos + 2] = b3;
        this.output[pos + 3] = b4;
    }

    /**
     * Create a padding in the fontfile to align on a 4-byte boundary
     */
    private void pad4() {
        final int padSize = getPadSize(this.currentPos);
        if (padSize < 4) {
            for (int i = 0; i < padSize; i++) {
                this.output[this.currentPos++] = 0;
                this.realSize++;
            }
        }
    }

    /**
     * Returns the maximum power of 2 <= max
     */
    private int maxPow2(final int max) {
        int i = 0;
        while (Math.pow(2, i) <= max) {
            i++;
        }

        return i - 1;
    }

    private void updateCheckSum(final int tableStart, final int tableSize,
            final TTFTableName tableName) {
        final int checksum = getCheckSum(this.output, tableStart, tableSize);
        final int offset = this.offsets.get(tableName);
        final int padSize = getPadSize(tableStart + tableSize);
        this.newDirTabs.put(tableName, new TTFDirTabEntry(tableStart, tableSize
                + padSize));
        writeULong(offset, checksum);
        writeULong(offset + 4, tableStart);
        writeULong(offset + 8, tableSize);
    }

    private static int getCheckSum(final byte[] data, final int start, int size) {
        // All the tables here are aligned on four byte boundaries
        // Add remainder to size if it's not a multiple of 4
        final int remainder = size % 4;
        if (remainder != 0) {
            size += remainder;
        }

        long sum = 0;

        for (int i = 0; i < size; i += 4) {
            long l = 0;
            for (int j = 0; j < 4; j++) {
                l <<= 8;
                l |= data[start + i + j] & 0xff;
            }
            sum += l;
        }
        return (int) sum;
    }

    private void createCheckSumAdjustment() {
        final long sum = getCheckSum(this.output, 0, this.realSize);
        final int checksum = (int) (0xb1b0afba - sum);
        writeULong(this.checkSumAdjustmentOffset, checksum);
    }
}
