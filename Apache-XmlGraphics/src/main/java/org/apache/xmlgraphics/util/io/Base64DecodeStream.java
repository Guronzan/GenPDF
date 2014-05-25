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

/* $Id: Base64DecodeStream.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.util.io;

import java.io.IOException;
import java.io.InputStream;

// CSOFF: ConstantName
// CSOFF: MemberName
// CSOFF: MultipleVariableDeclarations
// CSOFF: NeedBraces
// CSOFF: OperatorWrap
// CSOFF: WhitespaceAround

/**
 * This class implements a Base64 Character decoder as specified in RFC1113.
 * Unlike some other encoding schemes there is nothing in this encoding that
 * tells the decoder where a buffer starts or stops, so to use it you will need
 * to isolate your encoded data into a single chunk and then feed them this
 * decoder. The simplest way to do that is to read all of the encoded data into
 * a string and then use:
 * 
 * <pre>
 * byte data[];
 * InputStream is = new ByteArrayInputStream(data);
 * is = new Base64DecodeStream(is);
 * </pre>
 *
 * On errors, this class throws a IOException with the following detail strings:
 * 
 * <pre>
 *    "Base64DecodeStream: Bad Padding byte (2)."
 *    "Base64DecodeStream: Bad Padding byte (1)."
 * </pre>
 *
 * @version $Id: Base64DecodeStream.java 1345683 2012-06-03 14:50:33Z gadams $
 *
 *          Originally authored by Thomas DeWeese, Vincent Hardy, and Chuck
 *          McManis.
 */

public class Base64DecodeStream extends InputStream {

    InputStream src;

    public Base64DecodeStream(final InputStream src) {
        this.src = src;
    }

    private static final byte[] pem_array = new byte[256];
    static {
        for (int i = 0; i < pem_array.length; i++) {
            pem_array[i] = -1;
        }

        int idx = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            pem_array[c] = (byte) idx++;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            pem_array[c] = (byte) idx++;
        }

        for (char c = '0'; c <= '9'; c++) {
            pem_array[c] = (byte) idx++;
        }

        pem_array['+'] = (byte) idx++;
        pem_array['/'] = (byte) idx++;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void close() throws IOException {
        this.EOF = true;
    }

    @Override
    public int available() throws IOException {
        return 3 - this.out_offset;
    }

    byte[] decode_buffer = new byte[4];
    byte[] out_buffer = new byte[3];
    int out_offset = 3;
    boolean EOF = false;

    @Override
    public int read() throws IOException {

        if (this.out_offset == 3) {
            if (this.EOF || getNextAtom()) {
                this.EOF = true;
                return -1;
            }
        }

        return this.out_buffer[this.out_offset++] & 0xFF;
    }

    @Override
    public int read(final byte[] out, final int offset, final int len)
            throws IOException {

        int idx = 0;
        while (idx < len) {
            if (this.out_offset == 3) {
                if (this.EOF || getNextAtom()) {
                    this.EOF = true;
                    if (idx == 0) {
                        return -1;
                    } else {
                        return idx;
                    }
                }
            }

            out[offset + idx] = this.out_buffer[this.out_offset++];

            idx++;
        }
        return idx;
    }

    final boolean getNextAtom() throws IOException {
        int count, a, b, c, d;

        int off = 0;
        while (off != 4) {
            count = this.src.read(this.decode_buffer, off, 4 - off);
            if (count == -1) {
                return true;
            }

            int in = off, out = off;
            while (in < off + count) {
                if (this.decode_buffer[in] != '\n'
                        && this.decode_buffer[in] != '\r'
                        && this.decode_buffer[in] != ' ') {
                    this.decode_buffer[out++] = this.decode_buffer[in];
                }
                in++;
            }

            off = out;
        }

        a = pem_array[this.decode_buffer[0] & 0xFF];
        b = pem_array[this.decode_buffer[1] & 0xFF];
        c = pem_array[this.decode_buffer[2] & 0xFF];
        d = pem_array[this.decode_buffer[3] & 0xFF];

        this.out_buffer[0] = (byte) (a << 2 | b >>> 4);
        this.out_buffer[1] = (byte) (b << 4 | c >>> 2);
        this.out_buffer[2] = (byte) (c << 6 | d);

        if (this.decode_buffer[3] != '=') {
            // All three bytes are good.
            this.out_offset = 0;
        } else if (this.decode_buffer[2] == '=') {
            // Only one byte of output.
            this.out_buffer[2] = this.out_buffer[0];
            this.out_offset = 2;
            this.EOF = true;
        } else {
            // Only two bytes of output.
            this.out_buffer[2] = this.out_buffer[1];
            this.out_buffer[1] = this.out_buffer[0];
            this.out_offset = 1;
            this.EOF = true;
        }

        return false;
    }
}
