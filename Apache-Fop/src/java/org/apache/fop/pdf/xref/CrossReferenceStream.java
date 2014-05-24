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

/* $Id: CrossReferenceStream.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf.xref;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.fop.pdf.PDFArray;
import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFFilterList;
import org.apache.fop.pdf.PDFName;
import org.apache.fop.pdf.PDFStream;

/**
 * A cross-reference stream, as described in Section 3.4.7 of the PDF 1.5
 * Reference.
 */
public class CrossReferenceStream extends CrossReferenceObject {

    private static final PDFName XREF = new PDFName("XRef");

    private final PDFDocument document;

    private final int objectNumber;

    private final List<ObjectReference> objectReferences;

    public CrossReferenceStream(final PDFDocument document,
            final int objectNumber, final TrailerDictionary trailerDictionary,
            final long startxref,
            final List<Long> uncompressedObjectReferences,
            final List<CompressedObjectReference> compressedObjectReferences) {
        super(trailerDictionary, startxref);
        this.document = document;
        this.objectNumber = objectNumber;
        this.objectReferences = new ArrayList<ObjectReference>(
                uncompressedObjectReferences.size());
        for (final Long offset : uncompressedObjectReferences) {
            this.objectReferences.add(offset == null ? null
                    : new UncompressedObjectReference(offset));
        }
        for (final CompressedObjectReference ref : compressedObjectReferences) {
            this.objectReferences.set(ref.getObjectNumber() - 1, ref);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void output(final OutputStream stream) throws IOException {
        populateDictionary();
        final PDFStream helperStream = new PDFStream(
                this.trailerDictionary.getDictionary(), false) {

            @Override
            protected void setupFilterList() {
                final PDFFilterList filterList = getFilterList();
                assert !filterList.isInitialized();
                filterList.addDefaultFilters(
                        CrossReferenceStream.this.document.getFilterMap(),
                        getDefaultFilterName());
            }

        };
        helperStream.setObjectNumber(this.objectNumber);
        helperStream.setDocument(this.document);
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        final DataOutputStream data = new DataOutputStream(byteArray);
        addFreeEntryForObject0(data);
        for (final ObjectReference objectReference : this.objectReferences) {
            assert objectReference != null;
            objectReference.output(data);
        }
        new UncompressedObjectReference(this.startxref).output(data);
        data.close();
        helperStream.setData(byteArray.toByteArray());
        PDFDocument.outputIndirectObject(helperStream, stream);
    }

    private void populateDictionary() throws IOException {
        final int objectCount = this.objectReferences.size() + 1;
        final PDFDictionary dictionary = this.trailerDictionary.getDictionary();
        dictionary.put("/Type", XREF);
        dictionary.put("/Size", objectCount + 1);
        dictionary.put("/W", new PDFArray(1, 8, 2));
    }

    private void addFreeEntryForObject0(final DataOutputStream data)
            throws IOException {
        data.write(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff,
                (byte) 0xff });
    }

}
