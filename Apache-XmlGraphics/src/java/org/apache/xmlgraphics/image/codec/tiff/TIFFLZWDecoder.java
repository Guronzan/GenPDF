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

/* $Id: TIFFLZWDecoder.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.image.codec.tiff;

import org.apache.xmlgraphics.image.codec.util.PropertyUtil;

// CSOFF: InnerAssignment
// CSOFF: MultipleVariableDeclarations
// CSOFF: OneStatementPerLine
// CSOFF: OperatorWrap
// CSOFF: WhitespaceAround

/**
 * A class for performing LZW decoding.
 */
public class TIFFLZWDecoder {

    byte[][] stringTable;
    byte[] data = null;
    byte[] uncompData;
    int tableIndex, bitsToGet = 9;
    int bytePointer, bitPointer;
    int dstIndex;
    int w, h;
    int predictor, samplesPerPixel;
    int nextData = 0;
    int nextBits = 0;

    int[] andTable = { 511, 1023, 2047, 4095 };

    public TIFFLZWDecoder(final int w, final int predictor,
            final int samplesPerPixel) {
        this.w = w;
        this.predictor = predictor;
        this.samplesPerPixel = samplesPerPixel;
    }

    /**
     * Method to decode LZW compressed data.
     *
     * @param data
     *            The compressed data.
     * @param uncompData
     *            Array to return the uncompressed data in.
     * @param h
     *            The number of rows the compressed data contains.
     */
    public byte[] decode(final byte[] data, final byte[] uncompData, final int h) {

        if (data[0] == (byte) 0x00 && data[1] == (byte) 0x01) {
            throw new UnsupportedOperationException(
                    PropertyUtil.getString("TIFFLZWDecoder0"));
        }

        initializeStringTable();

        this.data = data;
        this.h = h;
        this.uncompData = uncompData;

        // Initialize pointers
        this.bytePointer = 0;
        this.bitPointer = 0;
        this.dstIndex = 0;

        this.nextData = 0;
        this.nextBits = 0;

        int code, oldCode = 0;
        byte[] string;

        while ((code = getNextCode()) != 257
                && this.dstIndex != uncompData.length) {

            if (code == 256) {

                initializeStringTable();
                code = getNextCode();

                if (code == 257) {
                    break;
                }

                writeString(this.stringTable[code]);
                oldCode = code;

            } else {

                if (code < this.tableIndex) {

                    string = this.stringTable[code];

                    writeString(string);
                    addStringToTable(this.stringTable[oldCode], string[0]);
                    oldCode = code;

                } else {

                    string = this.stringTable[oldCode];
                    string = composeString(string, string[0]);
                    writeString(string);
                    addStringToTable(string);
                    oldCode = code;
                }

            }

        }

        // Horizontal Differencing Predictor
        if (this.predictor == 2) {

            int count;
            for (int j = 0; j < h; j++) {

                count = this.samplesPerPixel * (j * this.w + 1);

                for (int i = this.samplesPerPixel; i < this.w
                        * this.samplesPerPixel; i++) {

                    uncompData[count] += uncompData[count
                            - this.samplesPerPixel];
                    count++;
                }
            }
        }

        return uncompData;
    }

    /**
     * Initialize the string table.
     */
    public void initializeStringTable() {

        this.stringTable = new byte[4096][];

        for (int i = 0; i < 256; i++) {
            this.stringTable[i] = new byte[1];
            this.stringTable[i][0] = (byte) i;
        }

        this.tableIndex = 258;
        this.bitsToGet = 9;
    }

    /**
     * Write out the string just uncompressed.
     */
    public void writeString(final byte[] string) {

        for (int i = 0; i < string.length; i++) {
            this.uncompData[this.dstIndex++] = string[i];
        }
    }

    /**
     * Add a new string to the string table.
     */
    public void addStringToTable(final byte[] oldString, final byte newString) {
        final int length = oldString.length;
        final byte[] string = new byte[length + 1];
        System.arraycopy(oldString, 0, string, 0, length);
        string[length] = newString;

        // Add this new String to the table
        this.stringTable[this.tableIndex++] = string;

        if (this.tableIndex == 511) {
            this.bitsToGet = 10;
        } else if (this.tableIndex == 1023) {
            this.bitsToGet = 11;
        } else if (this.tableIndex == 2047) {
            this.bitsToGet = 12;
        }
    }

    /**
     * Add a new string to the string table.
     */
    public void addStringToTable(final byte[] string) {

        // Add this new String to the table
        this.stringTable[this.tableIndex++] = string;

        if (this.tableIndex == 511) {
            this.bitsToGet = 10;
        } else if (this.tableIndex == 1023) {
            this.bitsToGet = 11;
        } else if (this.tableIndex == 2047) {
            this.bitsToGet = 12;
        }
    }

    /**
     * Append <code>newString</code> to the end of <code>oldString</code>.
     */
    public byte[] composeString(final byte[] oldString, final byte newString) {
        final int length = oldString.length;
        final byte[] string = new byte[length + 1];
        System.arraycopy(oldString, 0, string, 0, length);
        string[length] = newString;

        return string;
    }

    // Returns the next 9, 10, 11 or 12 bits
    public int getNextCode() {
        // Attempt to get the next code. The exception is caught to make
        // this robust to cases wherein the EndOfInformation code has been
        // omitted from a strip. Examples of such cases have been observed
        // in practice.
        try {
            this.nextData = this.nextData << 8 | this.data[this.bytePointer++]
                    & 0xff;
            this.nextBits += 8;

            if (this.nextBits < this.bitsToGet) {
                this.nextData = this.nextData << 8
                        | this.data[this.bytePointer++] & 0xff;
                this.nextBits += 8;
            }

            final int code = this.nextData >> this.nextBits - this.bitsToGet
                    & this.andTable[this.bitsToGet - 9];
                this.nextBits -= this.bitsToGet;

                return code;
        } catch (final ArrayIndexOutOfBoundsException e) {
            // Strip not terminated as expected: return EndOfInformation code.
            return 257;
        }
    }
}
