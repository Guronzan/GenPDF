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

/* $Id: ObjectStreamTestCase.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ObjectStreamTestCase {

    private static final String OBJECT_CONTENT = "<<\n  /Foo True\n  /Bar False\n>>\n";

    private PDFDocument pdfDocument;

    private ObjectStream objectStream;

    private List<MockCompressedObject> compressedObjects;

    @Before
    public void setUp() {
        this.pdfDocument = new PDFDocument("PDFObjectStreamTestCase");
        this.objectStream = new ObjectStream();
        this.pdfDocument.assignObjectNumber(this.objectStream);
        this.compressedObjects = Arrays.asList(new MockCompressedObject(),
                new MockCompressedObject());
    }

    @Test
    public void testSingleObjectStream() throws IOException {
        populateObjectStream();
        testOutput();
    }

    @Test
    public void testObjectStreamCollection() throws IOException {
        this.objectStream = new ObjectStream(this.objectStream);
        this.pdfDocument.assignObjectNumber(this.objectStream);
        populateObjectStream();
        testOutput();
    }

    @Test(expected = IllegalStateException.class)
    public void directObjectsAreNotAllowed() {
        this.objectStream.addObject(new MockCompressedObject());
    }

    @Test(expected = NullPointerException.class)
    public void nullObjectsAreNotAllowed() {
        this.objectStream.addObject(null);
    }

    private void testOutput() throws IOException {
        final String expected = getExpectedOutput();
        final String actual = getActualOutput();
        assertEquals(expected, actual);
    }

    private void populateObjectStream() {
        for (final MockCompressedObject obj : this.compressedObjects) {
            this.pdfDocument.assignObjectNumber(obj);
            this.objectStream.addObject(obj);
        }
    }

    private String getExpectedOutput() {
        final int numObs = this.compressedObjects.size();
        final int objectStreamNumber = this.objectStream.getObjectNumber();
        final int offsetsLength = 9;
        final StringBuilder expected = new StringBuilder();
        expected.append("<<\n");
        final ObjectStream previous = (ObjectStream) this.objectStream
                .get("Extends");
        if (previous != null) {
            expected.append("  /Extends ").append(previous.getObjectNumber())
                    .append(" 0 R\n");
        }
        expected.append("  /Type /ObjStm\n").append("  /N ").append(numObs)
                .append("\n").append("  /First ").append(offsetsLength)
                .append('\n').append("  /Length ")
                .append(OBJECT_CONTENT.length() * 2 + offsetsLength + 1)
                .append('\n').append(">>\n").append("stream\n");
        int offset = 0;
        int num = 1;
        for (final PDFObject ob : this.compressedObjects) {
            expected.append(objectStreamNumber + num++).append(' ')
                    .append(offset).append('\n');
            offset += ob.toPDFString().length();
        }
        for (final PDFObject ob : this.compressedObjects) {
            expected.append(ob.toPDFString());
        }
        expected.append("\nendstream");
        return expected.toString();
    }

    private String getActualOutput() throws IOException {
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        this.objectStream.getFilterList().setDisableAllFilters(true);
        this.objectStream.output(actual);
        return actual.toString("US-ASCII");
    }

    private static class MockCompressedObject extends PDFObject implements
            CompressedObject {

        @Override
        protected String toPDFString() {
            return OBJECT_CONTENT;
        }
    }

}
