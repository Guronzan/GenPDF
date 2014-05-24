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

/* $Id: ASCII85InputStreamTestCase.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.util.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.xmlgraphics.util.HexUtil;

/**
 * Test case for ASCII85InputStream.
 * <p>
 * ATTENTION: Some of the tests here depend on the correct behaviour of
 * ASCII85OutputStream. If something fails here make sure
 * ASCII85OutputStreamTestCase runs!
 */
@Slf4j
public class ASCII85InputStreamTestCase extends TestCase {

    private static final boolean DEBUG = false;

    /**
     * @see junit.framework.TestCase#TestCase(String)
     */
    public ASCII85InputStreamTestCase(final String name) {
        super(name);
    }

    private byte[] decode(final String text) throws Exception {
        final byte[] ascii85 = text.getBytes("US-ASCII");
        final InputStream in = new ByteArrayInputStream(ascii85);
        final InputStream decoder = new ASCII85InputStream(in);
        return IOUtils.toByteArray(decoder);
    }

    private byte[] getChunk(final int count) {
        final byte[] buf = new byte[count];
        System.arraycopy(ASCII85OutputStreamTestCase.DATA, 0, buf, 0,
                buf.length);
        return buf;
    }

    private String encode(final byte[] data, final int len) throws Exception {
        final ByteArrayOutputStream baout = new ByteArrayOutputStream();
        final java.io.OutputStream out = new ASCII85OutputStream(baout);
        out.write(data, 0, len);
        out.close();
        return new String(baout.toByteArray(), "US-ASCII");
    }

    private void innerTestDecode(final byte[] data) throws Exception {
        final String encoded = encode(data, data.length);
        if (DEBUG) {
            if (data[0] == 0) {
                log.info("self-encode: " + data.length
                        + " chunk 000102030405...");
            } else {
                log.info("self-encode: " + new String(data, "US-ASCII") + " "
                        + HexUtil.toHex(data));
            }
            log.info("  ---> " + encoded);
        }
        final byte[] decoded = decode(encoded);
        if (DEBUG) {
            if (data[0] == 0) {
                log.info("decoded: " + data.length + " chunk 000102030405...");
            } else {
                log.info("decoded: " + new String(decoded, "US-ASCII") + " "
                        + HexUtil.toHex(decoded));
            }
        }
        assertEquals(HexUtil.toHex(data), HexUtil.toHex(decoded));
    }

    /**
     * Tests the output of ASCII85.
     *
     * @throws Exception
     *             if an error occurs
     */
    public void testDecode() throws Exception {
        final byte[] buf;
        innerTestDecode("1. Bodypart".getBytes("US-ASCII"));
        if (DEBUG) {
            log.info("===========================================");
        }

        innerTestDecode(getChunk(1));
        innerTestDecode(getChunk(2));
        innerTestDecode(getChunk(3));
        innerTestDecode(getChunk(4));
        innerTestDecode(getChunk(5));
        if (DEBUG) {
            log.info("===========================================");
        }

        innerTestDecode(getChunk(10));
        innerTestDecode(getChunk(62));
        innerTestDecode(getChunk(63));
        innerTestDecode(getChunk(64));
        innerTestDecode(getChunk(65));

        if (DEBUG) {
            log.info("===========================================");
        }
        String sz;
        sz = HexUtil.toHex(decode("zz~>"));
        assertEquals(HexUtil.toHex(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 }), sz);
        sz = HexUtil.toHex(decode("z\t \0z\n~>"));
        assertEquals(HexUtil.toHex(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 }), sz);
        if (DEBUG) {
            log.info("===========================================");
        }
        try {
            decode("vz~>");
            fail("Illegal character should be detected");
        } catch (final IOException ioe) {
            // expected
        }
        /*
         * DISABLED because of try/catch in InputStream.read(byte[], int, int).
         * Only the exception happening on the first byte in a block is being
         * reported. BUG in JDK???
         *
         * try { decode("zv~>"); fail("Illegal character should be detected"); }
         * catch (IOException ioe) { //expected }
         */
    }

    private byte[] getFullASCIIRange() {
        final java.io.ByteArrayOutputStream baout = new java.io.ByteArrayOutputStream(
                256);
        for (int i = 254; i < 256; i++) {
            baout.write(i);
        }
        return baout.toByteArray();
    }

    /**
     * Tests the full 8-bit ASCII range.
     *
     * @throws Exception
     *             if an error occurs
     */
    public void testFullASCIIRange() throws Exception {
        innerTestDecode(getFullASCIIRange());
    }

}
