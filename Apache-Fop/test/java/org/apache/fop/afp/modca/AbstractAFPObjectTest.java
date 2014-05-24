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

/* $Id$ */

package org.apache.fop.afp.modca;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.fop.afp.Streamable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the {@link AbstractAFPObject} class.
 */
public abstract class AbstractAFPObjectTest<S extends AbstractAFPObject> {

    private S sut;

    protected final S getSut() {
        return this.sut;
    }

    protected final void setSut(final S sut) {
        if (this.sut == null) {
            this.sut = sut;
        }
    }

    private final byte[] header = new byte[] { 0x5A, // Structured field
                                                     // identifier
            0x00, // Length byte 1
            0x10, // Length byte 2
            0x00, // Structured field id byte 1
            0x00, // Structured field id byte 2
            0x00, // Structured field id byte 3
            0x00, // Flags
            0x00, // Reserved
            0x00 // Reserved
    };

    @Test
    public void testCopySFStatic() {
        final byte[] actual = new byte[9];
        Arrays.fill(actual, (byte) -1);

        AbstractAFPObject.copySF(actual, (byte) 0, (byte) 0, (byte) 0);

        assertTrue(Arrays.equals(actual, this.header));

        final byte[] expected2 = new byte[9];
        System.arraycopy(this.header, 0, expected2, 0, this.header.length);

        final byte clazz = (byte) 0x01;
        final byte type = (byte) 0x02;
        final byte catagory = (byte) 0x03;
        expected2[3] = clazz;
        expected2[4] = type;
        expected2[5] = catagory;

        AbstractAFPObject.copySF(actual, clazz, type, catagory);

        assertTrue(Arrays.equals(actual, expected2));
    }

    @Test
    public void testCopySF() {
        final byte[] expected = new byte[9];
        AbstractAFPObject.copySF(expected, (byte) 0xD3, (byte) 0, (byte) 0);

        final byte[] actual = new byte[9];
        Arrays.fill(actual, (byte) -1);

        getSut().copySF(actual, (byte) 0, (byte) 0);

        assertTrue(Arrays.equals(actual, expected));

        final byte[] expected2 = new byte[9];
        System.arraycopy(expected, 0, expected2, 0, expected.length);

        final byte type = (byte) 1;
        final byte catagory = (byte) 2;
        expected2[4] = type;
        expected2[5] = catagory;

        getSut().copySF(actual, type, catagory);

        assertTrue(Arrays.equals(actual, expected2));
    }

    /**
     *
     */
    @Test
    public void testwriteObjects() {
        final byte[][] expected = { { (byte) 0, (byte) 1 },
                { (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5 } };

        final List<Streamable> objects = new ArrayList<Streamable>() {
            {
                add(StreamableObject.instance(expected[0]));
                add(StreamableObject.instance(expected[1]));
                add(StreamableObject.instance(expected[2]));
            }
        };

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            getSut().writeObjects(objects, baos);
        } catch (final IOException e) {
            fail();
        }

        final byte[] actual = baos.toByteArray();

        int index = 0;
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected[i].length; j++) {
                assertTrue("" + index, actual[index] == expected[i][j]);
                index++;
            }
        }
    }

    /**
     *
     */
    @Test
    public void testTruncate() {
        final String expected = "abc";
        assertTrue(AbstractAFPObject.truncate(expected, 4) == expected);
        assertTrue(AbstractAFPObject.truncate(expected, 3) == expected);
        assertEquals(AbstractAFPObject.truncate(expected + "d", 3), expected);
        assertEquals(AbstractAFPObject.truncate(expected, 0), "");
        try {
            assertTrue(AbstractAFPObject.truncate(null, 4) == null);
            fail();
        } catch (final NullPointerException e) {
            // PASS
        }
    }

    /**
     *
     */
    @Test
    public void testWriteChunksToStream() throws IOException {
        final byte[] data = new byte[256];
        int counter = 0;
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) counter++;
        }

        final byte[] header = new byte[9];
        // Test when chunk size % data.length == 0
        testWithGivenChunkSize(data, header, 16);

        // test when chunk size % data.length != 0
        testWithGivenChunkSize(data, header, 10);

        // test with an odd number...
        testWithGivenChunkSize(data, header, 13);
    }

    private void testWithGivenChunkSize(final byte[] data, final byte[] header,
            final int chunkSize) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AbstractAFPObject.writeChunksToStream(data, header, 0, chunkSize, baos);
        final byte[] testData = baos.toByteArray();

        final int numberOfFullDataChunks = data.length / chunkSize;
        final int lastChunkSize = data.length % chunkSize;
        int lengthOfTestData = numberOfFullDataChunks
                * (chunkSize + header.length);
        lengthOfTestData += lastChunkSize == 0 ? 0 : header.length
                + lastChunkSize;

        putLengthInHeader(header, chunkSize);

        assertEquals(lengthOfTestData, testData.length);
        int testIndex = 0;
        int expectedIndex = 0;
        for (int i = 0; i < numberOfFullDataChunks; i++) {
            checkHeaderAndData(header, data, testData, expectedIndex,
                    testIndex, chunkSize);
            expectedIndex += chunkSize + header.length;
            testIndex += chunkSize;
        }

        putLengthInHeader(header, lastChunkSize);
        // check last chunk
        if (lastChunkSize != 0) {
            checkHeaderAndData(header, data, testData, expectedIndex,
                    testIndex, lastChunkSize);
        }
    }

    private void putLengthInHeader(final byte[] header, final int chunkSize) {
        header[0] = 0;
        header[1] = (byte) (chunkSize + header.length);
    }

    private void checkHeaderAndData(final byte[] header, final byte[] data,
            final byte[] testData, int expectedIndex, final int testIndex,
            final int chunkSize) {
        for (int i = 0; i < header.length; i++) {
            assertEquals(testData[expectedIndex++], header[i]);
        }
        for (int i = 0; i < chunkSize; i++) {
            assertEquals(testData[expectedIndex++], data[i + testIndex]);
        }
    }

    /**
     *
     */
    private static class StreamableObject implements Streamable {
        private final byte[] bytes;

        StreamableObject(final byte[] bytes) {
            this.bytes = new byte[bytes.length];
            System.arraycopy(bytes, 0, this.bytes, 0, bytes.length);
        }

        private static Streamable instance(final byte[] bytes) {
            return new StreamableObject(bytes);
        }

        @Override
        public void writeToStream(final OutputStream os) throws IOException {
            os.write(this.bytes);
        }
    }
}
