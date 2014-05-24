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

/* $Id: PDFStreamTestCase.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PDFStreamTestCase {

    private PDFStream stream;

    @Before
    public void createStream() {
        this.stream = new PDFStream();
        this.stream.setObjectNumber(1);
        final PDFDocument pdfDocument = new PDFDocument("Apache FOP");
        this.stream.setDocument(pdfDocument);
    }

    @Test
    public void testFilterSetup() {
        testGetFilterList();
        testSetupFilterList();
    }

    private void testGetFilterList() {
        final PDFFilterList filterList = this.stream.getFilterList();
        assertFalse(filterList.isInitialized());
        assertEquals(0, filterList.getFilters().size());
    }

    private void testSetupFilterList() {
        this.stream.setupFilterList();
        final PDFFilterList filterList = this.stream.getFilterList();
        assertTrue(filterList.isInitialized());
        assertEquals(1, filterList.getFilters().size());
        final PDFFilter filter = filterList.getFilters().get(0);
        assertEquals("/FlateDecode", filter.getName());
    }

    @Test
    public void customFilter() {
        final PDFFilterList filters = this.stream.getFilterList();
        filters.addFilter("null");
        assertTrue(filters.isInitialized());
        assertEquals(1, filters.getFilters().size());
        final PDFFilter filter = filters.getFilters().get(0);
        assertEquals("", filter.getName());
    }

    @Test
    public void testStream() throws IOException {
        final PDFFilterList filters = this.stream.getFilterList();
        filters.addFilter("null");
        final byte[] bytes = createSampleData();
        this.stream.setData(bytes);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        this.stream.outputRawStreamData(actual);
        assertArrayEquals(bytes, actual.toByteArray());
    }

    @Test
    public void testEncodeStream() throws IOException {
        final PDFFilterList filters = this.stream.getFilterList();
        filters.addFilter("null");
        final byte[] bytes = createSampleData();
        this.stream.setData(bytes);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final StreamCache streamCache = this.stream.encodeStream();
        streamCache.outputContents(actual);
        assertArrayEquals(bytes, actual.toByteArray());
    }

    @Test
    public void testEncodeAndWriteStream() throws IOException {
        final PDFFilterList filters = this.stream.getFilterList();
        filters.addFilter("null");
        final byte[] bytes = createSampleData();
        this.stream.setData(bytes);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final PDFNumber number = new PDFNumber();
        this.stream.encodeAndWriteStream(actual, number);
        assertArrayEquals(createSampleStreamData(), actual.toByteArray());
    }

    private byte[] createSampleData() {
        final byte[] bytes = new byte[10];
        for (int i = 0; i < 10; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    private byte[] createSampleStreamData() throws IOException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write("stream\n".getBytes("US-ASCII"));
        stream.write(createSampleData());
        stream.write("\nendstream".getBytes("US-ASCII"));
        return stream.toByteArray();
    }
}
