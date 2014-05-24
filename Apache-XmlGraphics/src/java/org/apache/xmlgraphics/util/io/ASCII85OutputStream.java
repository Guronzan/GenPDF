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

/* $Id: ASCII85OutputStream.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.util.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * This class applies a ASCII85 encoding to the stream.
 *
 * @version $Id: ASCII85OutputStream.java 1345683 2012-06-03 14:50:33Z gadams $
 */
@Slf4j
public class ASCII85OutputStream extends FilterOutputStream implements
ASCII85Constants, Finalizable {

    private static final boolean DEBUG = false;

    private int pos = 0;
    private long buffer = 0;
    private int posinline = 0;
    private int bw = 0;

    /** @see java.io.FilterOutputStream **/
    public ASCII85OutputStream(final OutputStream out) {
        super(out);
    }

    /** @see java.io.FilterOutputStream **/
    @Override
    public void write(final int b) throws IOException {
        if (this.pos == 0) {
            this.buffer += b << 24 & 0xff000000L;
        } else if (this.pos == 1) {
            this.buffer += b << 16 & 0xff0000L;
        } else if (this.pos == 2) {
            this.buffer += b << 8 & 0xff00L;
        } else {
            this.buffer += b & 0xffL;
        }
        this.pos++;

        if (this.pos > 3) {
            checkedWrite(convertWord(this.buffer));
            this.buffer = 0;
            this.pos = 0;
        }
    }

    /*
     * UNUSED ATM private void checkedWrite(int b) throws IOException { if
     * (posinline == 80) { out.write(EOL); bw++; posinline = 0; }
     * checkedWrite(b); posinline++; bw++; }
     */

    private void checkedWrite(final byte[] buf) throws IOException {
        checkedWrite(buf, buf.length, false);
    }

    private void checkedWrite(final byte[] buf, final boolean nosplit)
            throws IOException {
        checkedWrite(buf, buf.length, nosplit);
    }

    private void checkedWrite(final byte[] buf, final int len)
            throws IOException {
        checkedWrite(buf, len, false);
    }

    private void checkedWrite(final byte[] buf, final int len,
            final boolean nosplit) throws IOException {
        if (this.posinline + len > 80) {
            final int firstpart = nosplit ? 0 : len
                    - (this.posinline + len - 80);
            if (firstpart > 0) {
                this.out.write(buf, 0, firstpart);
            }
            this.out.write(EOL);
            this.bw++;
            final int rest = len - firstpart;
            if (rest > 0) {
                this.out.write(buf, firstpart, rest);
            }
            this.posinline = rest;
        } else {
            this.out.write(buf, 0, len);
            this.posinline += len;
        }
        this.bw += len;
    }

    /**
     * This converts a 32 bit value (4 bytes) into 5 bytes using base 85. each
     * byte in the result starts with zero at the '!' character so the resulting
     * base85 number fits into printable ascii chars
     *
     * @param word
     *            the 32 bit unsigned (hence the long datatype) word
     * @return 5 bytes (or a single byte of the 'z' character for word values of
     *         0)
     */
    private byte[] convertWord(long word) {
        word = word & 0xffffffff;

        if (word == 0) {
            return ZERO_ARRAY;
        } else {
            if (word < 0) {
                word = -word;
            }
            final byte c1 = (byte) (word / POW85[0] & 0xFF);
            final byte c2 = (byte) ((word - c1 * POW85[0]) / POW85[1] & 0xFF);
            final byte c3 = (byte) ((word - c1 * POW85[0] - c2 * POW85[1])
                    / POW85[2] & 0xFF);
            final byte c4 = (byte) ((word - c1 * POW85[0] - c2 * POW85[1] - c3
                    * POW85[2])
                    / POW85[3] & 0xFF);
            final byte c5 = (byte) (word - c1 * POW85[0] - c2 * POW85[1] - c3
                    * POW85[2] - c4 * POW85[3] & 0xFF);

            final byte[] ret = { (byte) (c1 + START), (byte) (c2 + START),
                    (byte) (c3 + START), (byte) (c4 + START),
                    (byte) (c5 + START) };

            if (DEBUG) {
                for (final byte element : ret) {
                    if (element < 33 || element > 117) {
                        log.info("Illegal char value " + new Integer(element));
                    }
                }
            }
            return ret;
        }
    }

    /** @see Finalizable **/
    @Override
    public void finalizeStream() throws IOException {
        // now take care of the trailing few bytes.
        // with n leftover bytes, we append 0 bytes to make a full group of 4
        // then convert like normal (except not applying the special zero rule)
        // and write out the first n+1 bytes from the result
        if (this.pos > 0) {
            final int rest = this.pos;
            /*
             * byte[] lastdata = new byte[4]; int i = 0; for (int j = 0; j < 4;
             * j++) { if (j < rest) { lastdata[j] = data[i++]; } else {
             * lastdata[j] = 0; } }
             *
             * long val = ((lastdata[0] << 24) & 0xff000000L) + ((lastdata[1] <<
             * 16) & 0xff0000L) + ((lastdata[2] << 8) & 0xff00L) + (lastdata[3]
             * & 0xffL);
             */

            byte[] conv;
            // special rule for handling zeros at the end
            if (this.buffer != 0) {
                conv = convertWord(this.buffer);
            } else {
                conv = new byte[5];
                for (int j = 0; j < 5; j++) {
                    conv[j] = (byte) '!';
                }
            }
            // assert rest+1 <= 5
            checkedWrite(conv, rest + 1);
        }
        // finally write the two character end of data marker
        checkedWrite(EOD, true);

        flush();
        if (this.out instanceof Finalizable) {
            ((Finalizable) this.out).finalizeStream();
        }
    }

    /** @see java.io.FilterOutputStream **/
    @Override
    public void close() throws IOException {
        finalizeStream();
        super.close();
    }

}
