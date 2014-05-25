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

/* $Id: ASCII85OutputStreamTestCase.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.util.io;

import java.io.OutputStream;

import junit.framework.TestCase;

import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * Test case for ASCII85OutputStream
 */
public class ASCII85OutputStreamTestCase extends TestCase {

    /** Test data */
    public static final byte[] DATA = new byte[100];

    static {
        // Fill in some data
        for (int i = 0; i < 100; i++) {
            DATA[i] = (byte) i;
        }
    }

    /**
     * @see junit.framework.TestCase#TestCase(String)
     */
    public ASCII85OutputStreamTestCase(final String name) {
        super(name);
    }

    private String encode(final int count) throws Exception {
        return encode(DATA, count);
    }

    private String encode(final byte[] data, final int len) throws Exception {
        final ByteArrayOutputStream baout = new ByteArrayOutputStream();
        final OutputStream out = new ASCII85OutputStream(baout);
        out.write(data, 0, len);
        out.close();
        return new String(baout.toByteArray(), "US-ASCII");
    }

    /**
     * Tests the output of ASCII85.
     * 
     * @throws Exception
     *             if an error occurs
     */
    public void testOutput() throws Exception {
        final String sz = encode(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 }, 8);
        assertEquals("zz~>", sz);

        final String s3 = encode(3);
        // log.info(">>>" + s3 + "<<<");
        assertEquals("!!*-~>", s3);

        final String s10 = encode(10);
        // log.info(">>>" + s10 + "<<<");
        assertEquals("!!*-'\"9eu7#RL~>", s10);

        final String s62 = encode(62);
        // log.info(">>>" + s62 + "<<<");
        assertEquals("!!*-'\"9eu7#RLhG$k3[W&.oNg'GVB\"(`=52*$$(B+<_pR,"
                + "UFcb-n-Vr/1iJ-0JP==1c70M3&s#]4?W~>", s62);

        final String s63 = encode(63);
        // log.info(">>>" + s63 + "<<<");
        assertEquals("!!*-'\"9eu7#RLhG$k3[W&.oNg'GVB\"(`=52*$$(B+<_pR,"
                + "UFcb-n-Vr/1iJ-0JP==1c70M3&s#]4?Yk\n~>", s63);

        final String s64 = encode(64);
        // log.info(">>>" + s64 + "<<<");
        assertEquals("!!*-'\"9eu7#RLhG$k3[W&.oNg'GVB\"(`=52*$$(B+<_pR,"
                + "UFcb-n-Vr/1iJ-0JP==1c70M3&s#]4?Ykm\n~>", s64);

        final String s65 = encode(65);
        // log.info(">>>" + s65 + "<<<");
        assertEquals("!!*-'\"9eu7#RLhG$k3[W&.oNg'GVB\"(`=52*$$(B+<_pR,"
                + "UFcb-n-Vr/1iJ-0JP==1c70M3&s#]4?Ykm\n5Q~>", s65);

    }

}
