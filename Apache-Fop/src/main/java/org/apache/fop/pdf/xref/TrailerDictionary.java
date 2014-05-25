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

/* $Id: TrailerDictionary.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf.xref;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.pdf.PDFArray;
import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFEncryption;
import org.apache.fop.pdf.PDFInfo;
import org.apache.fop.pdf.PDFRoot;
import org.apache.fop.pdf.PDFText;
import org.apache.fop.pdf.PDFWritable;

/**
 * A data class representing entries of the file trailer dictionary.
 */
public class TrailerDictionary {

    private final PDFDictionary dictionary;

    public TrailerDictionary(final PDFDocument pdfDocument) {
        this.dictionary = new PDFDictionary();
        this.dictionary.setDocument(pdfDocument);
    }

    /** Sets the value of the Root entry. */
    public TrailerDictionary setRoot(final PDFRoot root) {
        this.dictionary.put("/Root", root);
        return this;
    }

    /** Sets the value of the Info entry. */
    public TrailerDictionary setInfo(final PDFInfo info) {
        this.dictionary.put("/Info", info);
        return this;
    }

    /** Sets the value of the Encrypt entry. */
    public TrailerDictionary setEncryption(final PDFEncryption encryption) {
        this.dictionary.put("/Encrypt", encryption);
        return this;
    }

    /** Sets the value of the ID entry. */
    public TrailerDictionary setFileID(final byte[] originalFileID,
            final byte[] updatedFileID) {
        // TODO this is ugly! Used to circumvent the fact that the file ID will
        // be
        // encrypted if directly stored as a byte array
        class FileID implements PDFWritable {

            private final byte[] fileID;

            FileID(final byte[] id) {
                this.fileID = id;
            }

            @Override
            public void outputInline(final OutputStream out,
                    final StringBuilder textBuffer) throws IOException {
                PDFDocument.flushTextBuffer(textBuffer, out);
                final String hex = PDFText.toHex(this.fileID, true);
                final byte[] encoded = hex.getBytes("US-ASCII");
                out.write(encoded);
            }

        }
        final PDFArray fileID = new PDFArray(new FileID(originalFileID),
                new FileID(updatedFileID));
        this.dictionary.put("/ID", fileID);
        return this;
    }

    PDFDictionary getDictionary() {
        return this.dictionary;
    }

}
