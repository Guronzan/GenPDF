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

/* $Id: PSTTFGeneratorTestCase.java 1352986 2012-06-22 18:07:04Z vhennebert $ */

package org.apache.fop.render.ps.fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.xmlgraphics.ps.PSGenerator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The test class for org.apache.fop.render.ps.fonts.PSGenerator
 */
public class PSTTFGeneratorTestCase {
    private PSTTFGenerator ttfGen;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final PSGenerator gen = new PSGenerator(this.out);
    private final byte[] byteArray;

    /**
     * Constructor
     */
    public PSTTFGeneratorTestCase() {
        this.byteArray = new byte[65536];
        for (int i = 0; i < 65536; i++) {
            this.byteArray[i] = (byte) i;
        }
    }

    @Before
    public void setUp() {
        this.ttfGen = new PSTTFGenerator(this.gen);
    }

    /**
     * Tests startString() - starts the string in an appropriate way for a
     * PostScript file.
     * 
     * @exception IOException
     *                write error
     */
    @Test
    public void testStartString() throws IOException {
        this.ttfGen.startString();
        assertEquals("<\n", this.out.toString());
    }

    /**
     * Test streamBytes() - tests that strings are written to file in the proper
     * format.
     * 
     * @throws IOException
     *             write error.
     */
    @Test
    public void testStreamBytes() throws IOException {
        this.ttfGen.streamBytes(this.byteArray, 0, 16);
        assertEquals("000102030405060708090A0B0C0D0E0F", this.out.toString());
        /*
         * 65520 is the closes multiple of 80 to 65535 (max string size in PS
         * document) and since one byte takes up two characters, 65520 / 2 - 16
         * (16 bytes already written)= 32744.
         */
        this.ttfGen.streamBytes(this.byteArray, 0, 32744);
        // Using a regex to ensure that the format is correct
        assertTrue(this.out.toString().matches("([0-9A-F]{80}\n){819}"));
        try {
            this.ttfGen.streamBytes(this.byteArray, 0,
                    PSTTFGenerator.MAX_BUFFER_SIZE + 1);
            fail("Shouldn't be able to write more than MAX_BUFFER_SIZE to a PS document");
        } catch (final UnsupportedOperationException e) {
            // PASS
        }
    }

    /**
     * Test reset() - reset should reset the line counter such that when reset()
     * is invoked the following string streamed to the PS document should be 80
     * chars long.
     * 
     * @throws IOException
     *             file write error.
     */
    @Test
    public void testReset() throws IOException {
        this.ttfGen.streamBytes(this.byteArray, 0, 40);
        assertTrue(this.out.toString().matches("([0-9A-F]{80}\n)"));
        this.ttfGen.streamBytes(this.byteArray, 0, 40);
        assertTrue(this.out.toString().matches("([0-9A-F]{80}\n){2}"));

    }

    /**
     * Test endString() - ensures strings are ended in the PostScript document
     * in the correct format, a "00" needs to be appended to the end of a
     * string.
     * 
     * @throws IOException
     *             file write error
     */
    @Test
    public void testEndString() throws IOException {
        this.ttfGen.endString();
        assertEquals("00\n> ", this.out.toString());
        this.out.reset();
        // we need to check that this doesn't write more than 80 chars per line
        this.ttfGen.streamBytes(this.byteArray, 0, 40);
        this.ttfGen.endString();
        assertTrue(this.out.toString().matches("([0-9A-F]{80}\n)00\n> "));
    }
}
