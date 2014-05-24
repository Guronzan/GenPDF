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

/* $Id: ObjectStreamManagerTestCase.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.fop.pdf.xref.CompressedObjectReference;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectStreamManagerTestCase {

    private List<CompressedObjectReference> compressedObjectReferences;

    private MockPdfDocument pdfDocument;

    @Test
    public void add() {
        final int expectedCapacity = 100;
        final int numCompressedObjects = expectedCapacity * 2 + 1;
        createCompressObjectReferences(numCompressedObjects);
        assertEquals(numCompressedObjects,
                this.compressedObjectReferences.size());
        final int objectStreamNumber1 = assertSameObjectStream(0,
                expectedCapacity);
        final int objectStreamNumber2 = assertSameObjectStream(
                expectedCapacity, expectedCapacity * 2);
        final int objectStreamNumber3 = assertSameObjectStream(
                expectedCapacity * 2, numCompressedObjects);
        assertDifferent(objectStreamNumber1, objectStreamNumber2,
                objectStreamNumber3);
        assertEquals(objectStreamNumber3,
                this.pdfDocument.previous.getObjectNumber());
    }

    private void createCompressObjectReferences(final int numObjects) {
        this.pdfDocument = new MockPdfDocument();
        final ObjectStreamManager sut = new ObjectStreamManager(
                this.pdfDocument);
        for (int obNum = 1; obNum <= numObjects; obNum++) {
            sut.add(createCompressedObject(obNum));
        }
        this.compressedObjectReferences = sut.getCompressedObjectReferences();
    }

    private static class MockPdfDocument extends PDFDocument {

        private ObjectStream previous;

        public MockPdfDocument() {
            super("");
        }

        @Override
        public void assignObjectNumber(final PDFObject obj) {
            super.assignObjectNumber(obj);
            if (obj instanceof ObjectStream) {
                final ObjectStream objStream = (ObjectStream) obj;
                final ObjectStream previous = (ObjectStream) objStream
                        .get("Extends");
                if (previous == null) {
                    assertEquals(this.previous, previous);
                }
                this.previous = objStream;
            }
        }
    }

    private CompressedObject createCompressedObject(final int objectNumber) {
        return new CompressedObject() {

            @Override
            public int getObjectNumber() {
                return objectNumber;
            }

            @Override
            public int output(final OutputStream outputStream)
                    throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }

    private int assertSameObjectStream(final int from, final int to) {
        final int objectStreamNumber = getObjectStreamNumber(from);
        for (int i = from + 1; i < to; i++) {
            assertEquals(objectStreamNumber, getObjectStreamNumber(i));
        }
        return objectStreamNumber;
    }

    private int getObjectStreamNumber(final int index) {
        return this.compressedObjectReferences.get(index)
                .getObjectStreamNumber();
    }

    private void assertDifferent(final int objectStreamNumber1,
            final int objectStreamNumber2, final int objectStreamNumber3) {
        assertTrue(objectStreamNumber1 != objectStreamNumber2);
        assertTrue(objectStreamNumber1 != objectStreamNumber3);
        assertTrue(objectStreamNumber2 != objectStreamNumber3);
    }
}
