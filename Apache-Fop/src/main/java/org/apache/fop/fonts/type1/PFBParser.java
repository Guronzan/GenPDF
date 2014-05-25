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

/* $Id: PFBParser.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.fonts.type1;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * This class represents a parser for Adobe Type 1 PFB files.
 *
 * @see PFBData
 */
public class PFBParser {

    private static final byte[] CURRENTFILE_EEXEC;
    private static final byte[] CLEARTOMARK;

    static {
        try {
            CURRENTFILE_EEXEC = "currentfile eexec".getBytes("US-ASCII");
            CLEARTOMARK = "cleartomark".getBytes("US-ASCII");
        } catch (final java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(
                    "Incompatible VM. It doesn't support the US-ASCII encoding");
        }
    }

    /**
     * Parses a PFB file into a PFBData object.
     * 
     * @param url
     *            URL to load the PFB file from
     * @return PFBData memory representation of the font
     * @throws IOException
     *             In case of an I/O problem
     */
    public PFBData parsePFB(final java.net.URL url) throws IOException {
        final InputStream in = url.openStream();
        try {
            return parsePFB(in);
        } finally {
            in.close();
        }
    }

    /**
     * Parses a PFB file into a PFBData object.
     * 
     * @param pfbFile
     *            File to load the PFB file from
     * @return PFBData memory representation of the font
     * @throws IOException
     *             In case of an I/O problem
     */
    public PFBData parsePFB(final java.io.File pfbFile) throws IOException {
        final InputStream in = new java.io.FileInputStream(pfbFile);
        try {
            return parsePFB(in);
        } finally {
            in.close();
        }
    }

    /**
     * Parses a PFB file into a PFBData object.
     * 
     * @param in
     *            InputStream to load the PFB file from
     * @return PFBData memory representation of the font
     * @throws IOException
     *             In case of an I/O problem
     */
    public PFBData parsePFB(final InputStream in) throws IOException {
        final PFBData pfb = new PFBData();
        final BufferedInputStream bin = new BufferedInputStream(in);
        final DataInputStream din = new DataInputStream(bin);
        din.mark(32);
        final int firstByte = din.readUnsignedByte();
        din.reset();
        if (firstByte == 128) {
            pfb.setPFBFormat(PFBData.PFB_PC);
            parsePCFormat(pfb, din);
        } else {
            pfb.setPFBFormat(PFBData.PFB_RAW);
            parseRAWFormat(pfb, bin);
        }
        return pfb;
    }

    private static int swapInteger(final int value) {
        return ((value >> 0 & 0xff) << 24) + ((value >> 8 & 0xff) << 16)
                + ((value >> 16 & 0xff) << 8) + ((value >> 24 & 0xff) << 0);
    }

    private void parsePCFormat(final PFBData pfb, final DataInputStream din)
            throws IOException {
        int segmentHead;
        int segmentType;
        final int bytesRead;

        // Read first segment
        segmentHead = din.readUnsignedByte();
        if (segmentHead != 128) {
            throw new IOException("Invalid file format. Expected ASCII 80hex");
        }
        segmentType = din.readUnsignedByte(); // Read
        final int len1 = swapInteger(din.readInt());
        final byte[] headerSegment = new byte[len1];
        din.readFully(headerSegment);
        pfb.setHeaderSegment(headerSegment);

        // Read second segment
        segmentHead = din.readUnsignedByte();
        if (segmentHead != 128) {
            throw new IOException("Invalid file format. Expected ASCII 80hex");
        }
        segmentType = din.readUnsignedByte();
        final int len2 = swapInteger(din.readInt());
        final byte[] encryptedSegment = new byte[len2];
        din.readFully(encryptedSegment);
        pfb.setEncryptedSegment(encryptedSegment);

        // Read third segment
        segmentHead = din.readUnsignedByte();
        if (segmentHead != 128) {
            throw new IOException("Invalid file format. Expected ASCII 80hex");
        }
        segmentType = din.readUnsignedByte();
        final int len3 = swapInteger(din.readInt());
        final byte[] trailerSegment = new byte[len3];
        din.readFully(trailerSegment);
        pfb.setTrailerSegment(trailerSegment);

        // Read EOF indicator
        segmentHead = din.readUnsignedByte();
        if (segmentHead != 128) {
            throw new IOException("Invalid file format. Expected ASCII 80hex");
        }
        segmentType = din.readUnsignedByte();
        if (segmentType != 3) {
            throw new IOException("Expected segment type 3, but found: "
                    + segmentType);
        }
    }

    private static boolean byteCmp(final byte[] src, final int srcOffset,
            final byte[] cmp) {
        for (int i = 0; i < cmp.length; i++) {
            // log.info("Compare: " + src[srcOffset + i] + " " + cmp[i]);
            if (src[srcOffset + i] != cmp[i]) {
                return false;
            }
        }
        return true;
    }

    private void calcLengths(final PFBData pfb, final byte[] originalData) {
        // Calculate length 1 and 3
        // log.info ("Checking font, size = "+originalData.length);

        // Length1 is the size of the initial ascii portion
        // search for "currentfile eexec"
        // Get the first binary number and search backwards for "eexec"
        int len1 = 30;

        // log.info("Length1="+len1);
        while (!byteCmp(originalData, len1 - CURRENTFILE_EEXEC.length,
                CURRENTFILE_EEXEC)) {
            len1++;
        }

        // Skip newline
        len1++;

        // Length3 is length of the last portion of the file
        int len3 = 0;
        len3 -= CLEARTOMARK.length;
        while (!byteCmp(originalData, originalData.length + len3, CLEARTOMARK)) {
            len3--;
            // log.info("Len3="+len3);
        }
        len3 = -len3;
        len3++;
        // Eat 512 zeroes
        int numZeroes = 0;
        final byte[] ws1 = new byte[] { 0x0D }; // CR
        final byte[] ws2 = new byte[] { 0x0A }; // LF
        final byte[] ws3 = new byte[] { 0x30 }; // "0"
        while ((originalData[originalData.length - len3] == ws1[0]
                || originalData[originalData.length - len3] == ws2[0] || originalData[originalData.length
                - len3] == ws3[0])
                        && numZeroes < 512) {
            len3++;
            if (originalData[originalData.length - len3] == ws3[0]) {
                numZeroes++;
            }
        }
        // log.info("Length3="+len3);

        // Create the 3 segments
        byte[] buffer = new byte[len1];
        System.arraycopy(originalData, 0, buffer, 0, len1);
        pfb.setHeaderSegment(buffer);

        final int len2 = originalData.length - len3 - len1;
        buffer = new byte[len2];
        System.arraycopy(originalData, len1, buffer, 0, len2);
        pfb.setEncryptedSegment(buffer);

        buffer = new byte[len3];
        System.arraycopy(originalData, len1 + len2, buffer, 0, len3);
        pfb.setTrailerSegment(buffer);
    }

    private void parseRAWFormat(final PFBData pfb, final BufferedInputStream bin)
            throws IOException {
        calcLengths(pfb, IOUtils.toByteArray(bin));
    }

}
