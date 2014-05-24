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

/* $Id: TTFMtxEntry.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fonts.truetype;

import java.util.List;

/**
 * This class represents a TrueType Mtx Entry.
 */
class TTFMtxEntry {

    private int wx;
    private int lsb;
    private String name = "";
    private int index;
    private final List unicodeIndex = new java.util.ArrayList();
    private int[] boundingBox = new int[4];
    private long offset;
    private byte found = 0;

    /**
     * Returns a String representation of this object.
     *
     * @param t
     *            TTFFile to use for unit conversion
     * @return String String representation
     */
    public String toString(final TTFFile t) {
        return "Glyph " + this.name + " index: " + getIndexAsString()
                + " bbox [" + t.convertTTFUnit2PDFUnit(this.boundingBox[0])
                + " " + t.convertTTFUnit2PDFUnit(this.boundingBox[1]) + " "
                + t.convertTTFUnit2PDFUnit(this.boundingBox[2]) + " "
                + t.convertTTFUnit2PDFUnit(this.boundingBox[3]) + "] wx: "
                + t.convertTTFUnit2PDFUnit(this.wx);
    }

    /**
     * Returns the boundingBox.
     * 
     * @return int[]
     */
    public int[] getBoundingBox() {
        return this.boundingBox;
    }

    /**
     * Sets the boundingBox.
     * 
     * @param boundingBox
     *            The boundingBox to set
     */
    public void setBoundingBox(final int[] boundingBox) {
        this.boundingBox = boundingBox;
    }

    /**
     * Returns the found.
     * 
     * @return byte
     */
    public byte getFound() {
        return this.found;
    }

    /**
     * Returns the index.
     * 
     * @return int
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Determines whether this index represents a reserved character.
     * 
     * @return True if it is reserved
     */
    public boolean isIndexReserved() {
        return getIndex() >= 32768 && getIndex() <= 65535;
    }

    /**
     * Returns a String representation of the index taking into account if the
     * index is in the reserved range.
     * 
     * @return index as String
     */
    public String getIndexAsString() {
        if (isIndexReserved()) {
            return Integer.toString(getIndex()) + " (reserved)";
        } else {
            return Integer.toString(getIndex());
        }
    }

    /**
     * Returns the lsb.
     * 
     * @return int
     */
    public int getLsb() {
        return this.lsb;
    }

    /**
     * Returns the name.
     * 
     * @return String
     */
    public String getName() {
        return this.name;
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
     * Returns the unicodeIndex.
     * 
     * @return List
     */
    public List getUnicodeIndex() {
        return this.unicodeIndex;
    }

    /**
     * Returns the wx.
     * 
     * @return int
     */
    public int getWx() {
        return this.wx;
    }

    /**
     * Sets the found.
     * 
     * @param found
     *            The found to set
     */
    public void setFound(final byte found) {
        this.found = found;
    }

    /**
     * Sets the index.
     * 
     * @param index
     *            The index to set
     */
    public void setIndex(final int index) {
        this.index = index;
    }

    /**
     * Sets the lsb.
     * 
     * @param lsb
     *            The lsb to set
     */
    public void setLsb(final int lsb) {
        this.lsb = lsb;
    }

    /**
     * Sets the name.
     * 
     * @param name
     *            The name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the offset.
     * 
     * @param offset
     *            The offset to set
     */
    public void setOffset(final long offset) {
        this.offset = offset;
    }

    /**
     * Sets the wx.
     * 
     * @param wx
     *            The wx to set
     */
    public void setWx(final int wx) {
        this.wx = wx;
    }

}
