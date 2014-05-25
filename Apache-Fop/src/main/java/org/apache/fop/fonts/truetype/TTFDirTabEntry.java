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

/* $Id: TTFDirTabEntry.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.fonts.truetype;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * This class represents an entry to a TrueType font's Dir Tab.
 */
public class TTFDirTabEntry {

    private final byte[] tag = new byte[4];
    private int checksum;
    private long offset;
    private long length;

    public TTFDirTabEntry() {
    }

    public TTFDirTabEntry(final long offset, final long length) {
        this.offset = offset;
        this.length = length;
    }

    /**
     * Read Dir Tab.
     * 
     * @param in
     *            font file reader
     * @return tag name
     * @throws IOException
     *             upon I/O exception
     */
    public String read(final FontFileReader in) throws IOException {
        this.tag[0] = in.readTTFByte();
        this.tag[1] = in.readTTFByte();
        this.tag[2] = in.readTTFByte();
        this.tag[3] = in.readTTFByte();

        in.skip(4); // Skip checksum

        this.offset = in.readTTFULong();
        this.length = in.readTTFULong();
        final String tagStr = new String(this.tag, "ISO-8859-1");

        return tagStr;
    }

    @Override
    public String toString() {
        return "Read dir tab [" + this.tag[0] + " " + this.tag[1] + " "
                + this.tag[2] + " " + this.tag[3] + "]" + " offset: "
                + this.offset + " length: " + this.length + " name: "
                + this.tag;
    }

    /**
     * Returns the checksum.
     * 
     * @return int
     */
    public int getChecksum() {
        return this.checksum;
    }

    /**
     * Returns the length.
     * 
     * @return long
     */
    public long getLength() {
        return this.length;
    }

    /**
     * Returns the offset.
     * 
     * @return long
     */
    public long getOffset() {
        return this.offset;
    }

    /**
     * Returns the tag bytes.
     * 
     * @return byte[]
     */
    public byte[] getTag() {
        return this.tag;
    }

    /**
     * Returns the tag bytes.
     * 
     * @return byte[]
     */
    public String getTagString() {
        try {
            return new String(this.tag, "ISO-8859-1");
        } catch (final UnsupportedEncodingException e) {
            return toString(); // Should never happen.
        }
    }

}
