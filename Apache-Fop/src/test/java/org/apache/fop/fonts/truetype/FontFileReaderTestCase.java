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

/* $Id: FontFileReaderTestCase.java 1352986 2012-06-22 18:07:04Z vhennebert $ */

package org.apache.fop.fonts.truetype;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A test class for org.apache.fop.truetype.FontFileReader
 */
public class FontFileReaderTestCase {
    private FontFileReader fontReader;
    private final InputStream in;
    private final byte[] byteArray;

    /**
     * Constructor - initialises an array that only needs to be created once. It
     * creates a byte[] of form { 0x00, 0x01, 0x02, 0x03..., 0xff};
     */
    public FontFileReaderTestCase() {
        this.byteArray = new byte[256];
        for (int i = 0; i < 256; i++) {
            this.byteArray[i] = (byte) i;
        }
        this.in = new ByteArrayInputStream(this.byteArray);
    }

    /**
     * sets up the test subject object for testing.
     */
    @Before
    public void setUp() {
        try {
            this.fontReader = new FontFileReader(this.in);
        } catch (final Exception e) {
            fail("Error: " + e.getMessage());
        }
    }

    /**
     * the "destructor" method.
     *
     */
    public void tearDown() {
        this.fontReader = null;
    }

    /**
     * Test readTTFByte()
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testReadTTFByte() throws IOException {
        for (int i = 0; i < 256; i++) {
            assertEquals((byte) i, this.fontReader.readTTFByte());
        }
    }

    /**
     * Test seekSet() - check that it moves to the correct position and enforce
     * a failure case.
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testSeekSet() throws IOException {
        this.fontReader.seekSet(10);
        assertEquals(10, this.fontReader.readTTFByte());
        try {
            this.fontReader.seekSet(257);
            fail("FileFontReaderTest Failed testSeekSet");
        } catch (final IOException e) {
            // Passed
        }
    }

    /**
     * Test skip() - check that it moves to the correct position and enforce a
     * failure case.
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testSkip() throws IOException {
        this.fontReader.skip(100);
        assertEquals(100, this.fontReader.readTTFByte());
        try {
            // 100 (seekAdd) + 1 (read() = 1 byte) + 156 = 257
            this.fontReader.skip(156);
            fail("FileFontReaderTest Failed testSkip");
        } catch (final IOException e) {
            // Passed
        }
    }

    /**
     * Test getCurrentPos() - 3 checks: 1) test with seekSet(int) 2) test with
     * skip(int) 3) test with a readTTFByte() (this moves the position by the
     * size of the data being read)
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testGetCurrentPos() throws IOException {
        this.fontReader.seekSet(10);
        this.fontReader.skip(100);
        assertEquals(110, this.fontReader.getCurrentPos());
        this.fontReader.readTTFByte();
        assertEquals(111, this.fontReader.getCurrentPos());
    }

    /**
     * Test getFileSize()
     */
    @Test
    public void testGetFileSize() {
        assertEquals(256, this.fontReader.getFileSize());
    }

    /**
     * Test readTTFUByte()
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testReadTTFUByte() throws IOException {
        for (int i = 0; i < 256; i++) {
            assertEquals(i, this.fontReader.readTTFUByte());
        }
    }

    /**
     * Test readTTFShort() - Test positive and negative numbers (two's
     * compliment).
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testReadTTFShort() throws IOException {
        // 0x0001 = 1
        assertEquals("Should have been 1 (0x0001)", 1,
                this.fontReader.readTTFShort());
        // 0x0203 = 515
        assertEquals(515, this.fontReader.readTTFShort());
        // now test negative numbers
        this.fontReader.seekSet(250);
        // 0xfafb
        assertEquals(-1285, this.fontReader.readTTFShort());
    }

    /**
     * Test readTTFUShort() - Test positive and potentially negative numbers
     * (two's compliment).
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testReadTTFUShort() throws IOException {
        // 0x0001
        assertEquals(1, this.fontReader.readTTFUShort());
        // 0x0203
        assertEquals(515, this.fontReader.readTTFUShort());
        // test potential negatives
        this.fontReader.seekSet(250);
        // 0xfafb
        assertEquals((250 << 8) + 251, this.fontReader.readTTFUShort());
    }

    /**
     * Test readTTFShort(int) - test reading ahead of current position and
     * behind current position and in both cases ensure that our current
     * position isn't changed.
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testReadTTFShortWithArg() throws IOException {
        // 0x6465
        assertEquals(25701, this.fontReader.readTTFShort(100));
        assertEquals(0, this.fontReader.getCurrentPos());
        // read behind current position (and negative)
        this.fontReader.seekSet(255);
        // 0xfafb
        assertEquals(-1285, this.fontReader.readTTFShort(250));
        assertEquals(255, this.fontReader.getCurrentPos());
    }

    /**
     * Test readTTFUShort(int arg) - test reading ahead of current position and
     * behind current position and in both cases ensure that our current
     * position isn't changed.
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testReadTTFUShortWithArg() throws IOException {
        // 0x6465
        assertEquals(25701, this.fontReader.readTTFUShort(100));
        assertEquals(0, this.fontReader.getCurrentPos());
        // read behind current position (and potential negative)
        this.fontReader.seekSet(255);
        // 0xfafb
        assertEquals(64251, this.fontReader.readTTFUShort(250));
        assertEquals(255, this.fontReader.getCurrentPos());
    }

    /**
     * Test readTTFLong()
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testReadTTFLong() throws IOException {
        // 0x00010203
        assertEquals(66051, this.fontReader.readTTFLong());
        // test negative numbers
        this.fontReader.seekSet(250);
        // 0xf0f1f2f3
        assertEquals(-84148995, this.fontReader.readTTFLong());
    }

    /**
     * Test readTTFULong()
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testReadTTFULong() throws IOException {
        // 0x00010203
        assertEquals(66051, this.fontReader.readTTFULong());
        // test negative numbers
        this.fontReader.seekSet(250);
        // 0xfafbfcfd
        assertEquals(4210818301L, this.fontReader.readTTFULong());
    }

    /**
     * Test readTTFString() - there are two paths to test here: 1) A null
     * terminated string 2) A string not terminated with a null (we expect this
     * to throw an EOFException)
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testReadTTFString() throws IOException {
        final byte[] strByte = { (byte) 't', (byte) 'e', (byte) 's',
                (byte) 't', 0x00 };
        this.fontReader = new FontFileReader(new ByteArrayInputStream(strByte));
        assertEquals("test", this.fontReader.readTTFString());
        try {
            // not NUL terminated
            final byte[] strByteNoNull = { (byte) 't', (byte) 'e', (byte) 's',
                    (byte) 't' };
            this.fontReader = new FontFileReader(new ByteArrayInputStream(
                    strByteNoNull));
            assertEquals("test", this.fontReader.readTTFString());
            fail("FontFileReaderTest testReadTTFString Fails.");
        } catch (final EOFException e) {
            // Pass
        }
    }

    /**
     * Test readTTFString(int arg)
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testReadTTFStringIntArg() throws IOException {
        final byte[] strByte = { (byte) 't', (byte) 'e', (byte) 's', (byte) 't' };
        this.fontReader = new FontFileReader(new ByteArrayInputStream(strByte));
        assertEquals("test", this.fontReader.readTTFString(4));
        try {
            this.fontReader = new FontFileReader(new ByteArrayInputStream(
                    strByte));
            assertEquals("test", this.fontReader.readTTFString(5));
            fail("FontFileReaderTest testReadTTFStringIntArg Fails.");
        } catch (final EOFException e) {
            // Pass
        }
    }

    /**
     * Test readTTFString(int arg1, int arg2)
     */
    public void testReadTTFString2IntArgs() {
        // currently the same as above
    }

    /**
     * Test getBytes()
     * 
     * @throws IOException
     *             exception
     */
    @Test
    public void testGetBytes() throws IOException {
        final byte[] retrievedBytes = this.fontReader.getBytes(0, 256);
        assertTrue(Arrays.equals(this.byteArray, retrievedBytes));
    }
}