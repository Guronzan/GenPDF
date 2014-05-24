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

/* $Id: GraphicsCharacterString.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.afp.goca;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.CharacterCodingException;

import org.apache.fop.afp.fonts.CharacterSet;

/**
 * A GOCA graphics string
 */
public class GraphicsCharacterString extends AbstractGraphicsCoord {

    /** Up to 255 bytes of character data */
    protected static final int MAX_STR_LEN = 255;

    /** the string to draw */
    private final String str;

    /**
     * The character set encoding to use
     */
    private final CharacterSet charSet;

    /**
     * Constructor (absolute positioning)
     *
     * @param str
     *            the character string
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param charSet
     *            the character set
     */
    public GraphicsCharacterString(final String str, final int x, final int y,
            final CharacterSet charSet) {
        super(x, y);
        this.str = truncate(str, MAX_STR_LEN);
        this.charSet = charSet;
    }

    /** {@inheritDoc} */
    @Override
    byte getOrderCode() {
        if (isRelative()) {
            return (byte) 0x83;
        } else {
            return (byte) 0xC3;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getDataLength() {
        return super.getDataLength() + this.str.length();
    }

    /** {@inheritDoc} */
    @Override
    public void writeToStream(final OutputStream os) throws IOException {
        final byte[] data = getData();
        final byte[] strData = getStringAsBytes();
        System.arraycopy(strData, 0, data, 6, strData.length);
        os.write(data);
    }

    /**
     * Returns the text string as an encoded byte array
     *
     * @return the text string as an encoded byte array
     * @throws UnsupportedEncodingException
     *             , CharacterCodingException
     */
    private byte[] getStringAsBytes() throws UnsupportedEncodingException,
    CharacterCodingException {
        return this.charSet.encodeChars(this.str).getBytes();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "GraphicsCharacterString{"
                + (this.coords != null ? "x=" + this.coords[0] + ", y="
                        + this.coords[1] : "") + "str='" + this.str + "'" + "}";
    }
}
