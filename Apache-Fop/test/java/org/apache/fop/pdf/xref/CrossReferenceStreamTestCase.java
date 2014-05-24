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

/* $Id: CrossReferenceStreamTestCase.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf.xref;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class CrossReferenceStreamTestCase extends CrossReferenceObjectTest {

    private List<Long> uncompressedObjectOffsets;

    private List<CompressedObjectReference> compressedObjectReferences;

    @Test
    public void testWithNoOffset() throws IOException {
        final List<Long> emptyList = Collections.emptyList();
        test(emptyList);
    }

    @Test
    public void testWithOffsets() throws IOException {
        test(new ArrayList<Long>(Arrays.asList(0L, 1L, 2L, 3L, 4L)));
    }

    @Test
    public void testWithBigOffsets() throws IOException {
        test(new ArrayList<Long>(Arrays.asList(0xffL, 0xffffL, 0xffffffffL,
                0xffffffffffffffffL)));
    }

    @Test
    public void testWithObjectStreams1() throws IOException {
        final List<CompressedObjectReference> compressedObjectReferences = Arrays
                .asList(new CompressedObjectReference(2, 1, 0));
        test(Arrays.asList(0L, null), compressedObjectReferences);
    }

    @Test
    public void testWithObjectStreams2() throws IOException {
        final int numIndirectObjects = 2;
        final int numCompressedObjects = 1;
        final List<Long> indirectObjectOffsets = new ArrayList<Long>(
                numIndirectObjects + numCompressedObjects);
        for (long i = 0; i < numIndirectObjects; i++) {
            indirectObjectOffsets.add(i);
        }
        final List<CompressedObjectReference> compressedObjectReferences = new ArrayList<CompressedObjectReference>();
        for (int index = 0; index < numCompressedObjects; index++) {
            indirectObjectOffsets.add(null);
            final int obNum = numIndirectObjects + index + 1;
            compressedObjectReferences.add(new CompressedObjectReference(obNum,
                    numIndirectObjects, index));
        }
        test(indirectObjectOffsets, compressedObjectReferences);
    }

    private void test(final List<Long> indirectObjectOffsets)
            throws IOException {
        final List<CompressedObjectReference> compressedObjectReferences = Collections
                .emptyList();
        test(indirectObjectOffsets, compressedObjectReferences);
    }

    private void test(final List<Long> indirectObjectOffsets,
            final List<CompressedObjectReference> compressedObjectReferences)
            throws IOException {
        this.uncompressedObjectOffsets = indirectObjectOffsets;
        this.compressedObjectReferences = compressedObjectReferences;
        runTest();
    }

    @Override
    protected CrossReferenceObject createCrossReferenceObject() {
        return new CrossReferenceStream(this.pdfDocument,
                this.uncompressedObjectOffsets.size() + 1,
                this.trailerDictionary, STARTXREF,
                this.uncompressedObjectOffsets, this.compressedObjectReferences);
    }

    @Override
    protected byte[] createExpectedCrossReferenceData() throws IOException {
        final List<ObjectReference> objectReferences = new ArrayList<ObjectReference>(
                this.uncompressedObjectOffsets.size());
        for (final Long offset : this.uncompressedObjectOffsets) {
            objectReferences.add(offset == null ? null
                    : new UncompressedObjectReference(offset));
        }
        for (final CompressedObjectReference ref : this.compressedObjectReferences) {
            objectReferences.set(ref.getObjectNumber() - 1, ref);
        }
        final int maxObjectNumber = objectReferences.size() + 1;
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final StringBuilder expected = new StringBuilder(256);
        expected.append(maxObjectNumber + " 0 obj\n").append("<<\n")
        .append("  /Root 1 0 R\n").append("  /Info 2 0 R\n")
        .append("  /ID [<0123456789ABCDEF> <0123456789ABCDEF>]\n")
        .append("  /Type /XRef\n").append("  /Size ")
                .append(Integer.toString(maxObjectNumber + 1)).append('\n')
        .append("  /W [1 8 2]\n").append("  /Length ")
                .append(Integer.toString((maxObjectNumber + 1) * 11 + 1))
                .append('\n').append(">>\n").append("stream\n");
        stream.write(getBytes(expected));
        final DataOutputStream data = new DataOutputStream(stream);
        data.write(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff,
                (byte) 0xff });
        for (final ObjectReference objectReference : objectReferences) {
            objectReference.output(data);
        }
        data.write(1);
        data.writeLong(STARTXREF);
        data.write(0);
        data.write(0);
        data.close();
        stream.write(getBytes("\nendstream\nendobj\n"));
        return stream.toByteArray();
    }

}
