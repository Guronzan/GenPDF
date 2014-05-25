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

/* $Id: CrossReferenceTable.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf.xref;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.pdf.PDFDocument;

/**
 * A cross-reference table, as described in Section 3.4.3 of the PDF 1.5
 * Reference.
 */
public class CrossReferenceTable extends CrossReferenceObject {

    private final List<Long> objectReferences;

    private final StringBuilder pdf = new StringBuilder(256);

    public CrossReferenceTable(final TrailerDictionary trailerDictionary,
            final long startxref, final List<Long> location) {
        super(trailerDictionary, startxref);
        this.objectReferences = location;
    }

    @Override
    public void output(final OutputStream stream) throws IOException {
        outputXref();
        writeTrailer(stream);
    }

    private void outputXref() throws IOException {
        this.pdf.append("xref\n0 ");
        this.pdf.append(this.objectReferences.size() + 1);
        this.pdf.append("\n0000000000 65535 f \n");
        for (final Long objectReference : this.objectReferences) {
            final String padding = "0000000000";
            final String s = String.valueOf(objectReference);
            if (s.length() > 10) {
                throw new IOException("PDF file too large."
                        + " PDF 1.4 cannot grow beyond approx. 9.3GB.");
            }
            final String loc = padding.substring(s.length()) + s;
            this.pdf.append(loc).append(" 00000 n \n");
        }
    }

    private void writeTrailer(final OutputStream stream) throws IOException {
        this.pdf.append("trailer\n");
        stream.write(PDFDocument.encode(this.pdf.toString()));
        final PDFDictionary dictionary = this.trailerDictionary.getDictionary();
        dictionary.put("/Size", this.objectReferences.size() + 1);
        dictionary.output(stream);
    }

}
