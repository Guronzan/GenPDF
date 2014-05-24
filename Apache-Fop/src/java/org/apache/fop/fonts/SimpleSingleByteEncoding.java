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

/* $Id: SimpleSingleByteEncoding.java 1142192 2011-07-02 10:22:03Z jeremias $ */

package org.apache.fop.fonts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fop.util.CharUtilities;
import org.apache.xmlgraphics.fonts.Glyphs;

/**
 * A simple implementation of the OneByteEncoding mostly used for encodings that
 * are constructed on-the-fly.
 */
public class SimpleSingleByteEncoding implements SingleByteEncoding {

    private final String name;
    private final List<NamedCharacter> mapping = new ArrayList<NamedCharacter>();
    private final Map<Character, Character> charMap = new HashMap<Character, Character>();

    /**
     * Main constructor.
     * 
     * @param name
     *            the encoding's name
     */
    public SimpleSingleByteEncoding(final String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public char mapChar(final char c) {
        final Character nc = this.charMap.get(Character.valueOf(c));
        if (nc != null) {
            return nc.charValue();
        }
        return NOT_FOUND_CODE_POINT;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getCharNameMap() {
        final String[] map = new String[getSize()];
        Arrays.fill(map, Glyphs.NOTDEF);
        for (int i = getFirstChar(); i <= getLastChar(); i++) {
            final NamedCharacter ch = this.mapping.get(i - 1);
            map[i] = ch.getName();
        }
        return map;
    }

    /**
     * Returns the index of the first defined character.
     * 
     * @return the index of the first defined character (always 1 for this
     *         class)
     */
    public int getFirstChar() {
        return 1;
    }

    /**
     * Returns the index of the last defined character.
     * 
     * @return the index of the last defined character
     */
    public int getLastChar() {
        return this.mapping.size();
    }

    /**
     * Returns the number of characters defined by this encoding.
     * 
     * @return the number of characters
     */
    public int getSize() {
        return this.mapping.size() + 1;
    }

    /**
     * Indicates whether the encoding is full (with 256 code points).
     * 
     * @return true if the encoding is full
     */
    public boolean isFull() {
        return getSize() == 256;
    }

    /**
     * Adds a new character to the encoding.
     * 
     * @param ch
     *            the named character
     * @return the code point assigned to the character
     */
    public char addCharacter(final NamedCharacter ch) {
        if (!ch.hasSingleUnicodeValue()) {
            throw new IllegalArgumentException(
                    "Only NamedCharacters with a single Unicode value"
                            + " are currently supported!");
        }
        if (isFull()) {
            throw new IllegalStateException("Encoding is full!");
        }
        final char newSlot = (char) (getLastChar() + 1);
        this.mapping.add(ch);
        this.charMap.put(Character.valueOf(ch.getSingleUnicodeValue()),
                Character.valueOf(newSlot));
        return newSlot;
    }

    /**
     * Returns the named character at a given code point in the encoding.
     * 
     * @param codePoint
     *            the code point of the character
     * @return the NamedCharacter (or null if no character is at this position)
     */
    public NamedCharacter getCharacterForIndex(final int codePoint) {
        if (codePoint < 0 || codePoint > 255) {
            throw new IllegalArgumentException(
                    "codePoint must be between 0 and 255");
        }
        if (codePoint <= getLastChar()) {
            return this.mapping.get(codePoint - 1);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public char[] getUnicodeCharMap() {
        final char[] map = new char[getLastChar() + 1];
        for (int i = 0; i < getFirstChar(); i++) {
            map[i] = CharUtilities.NOT_A_CHARACTER;
        }
        for (int i = getFirstChar(); i <= getLastChar(); i++) {
            map[i] = getCharacterForIndex(i).getSingleUnicodeValue();
        }
        return map;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getName() + " (" + getSize() + " chars)";
    }

}
