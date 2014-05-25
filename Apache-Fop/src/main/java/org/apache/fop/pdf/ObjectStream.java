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

/* $Id: ObjectStream.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.fop.pdf.xref.CompressedObjectReference;

/**
 * An object stream, as described in section 3.4.6 of the PDF 1.5 Reference.
 */
public class ObjectStream extends PDFStream {

    private static final PDFName OBJ_STM = new PDFName("ObjStm");

    private final List<CompressedObject> objects = new ArrayList<CompressedObject>();

    private int firstObjectOffset;

    ObjectStream() {
        super(false);
    }

    ObjectStream(final ObjectStream previous) {
        this();
        put("Extends", previous);
    }

    CompressedObjectReference addObject(final CompressedObject obj) {
        if (obj == null) {
            throw new NullPointerException("obj must not be null");
        }
        final CompressedObjectReference reference = new CompressedObjectReference(
                obj.getObjectNumber(), getObjectNumber(), this.objects.size());
        this.objects.add(obj);
        return reference;
    }

    @Override
    protected void outputRawStreamData(final OutputStream out)
            throws IOException {
        int currentOffset = 0;
        final StringBuilder offsetsPart = new StringBuilder();
        final ByteArrayOutputStream streamContent = new ByteArrayOutputStream();
        for (final CompressedObject object : this.objects) {
            offsetsPart.append(object.getObjectNumber()).append(' ')
            .append(currentOffset).append('\n');
            currentOffset += object.output(streamContent);
        }
        final byte[] offsets = PDFDocument.encode(offsetsPart.toString());
        this.firstObjectOffset = offsets.length;
        out.write(offsets);
        streamContent.writeTo(out);
    }

    @Override
    protected void populateStreamDict(final Object lengthEntry) {
        put("Type", OBJ_STM);
        put("N", this.objects.size());
        put("First", this.firstObjectOffset);
        super.populateStreamDict(lengthEntry);
    }
}
