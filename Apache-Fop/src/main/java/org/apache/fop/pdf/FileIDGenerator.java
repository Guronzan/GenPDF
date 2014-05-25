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

/* $Id: FileIDGenerator.java 1154998 2011-08-08 15:51:43Z vhennebert $ */

package org.apache.fop.pdf;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * A class to generate the File Identifier of a PDF document (the ID entry of
 * the file trailer dictionary).
 */
abstract class FileIDGenerator {

    abstract byte[] getOriginalFileID();

    abstract byte[] getUpdatedFileID();

    private static final class RandomFileIDGenerator extends FileIDGenerator {

        private final byte[] fileID;

        private RandomFileIDGenerator() {
            final Random random = new Random();
            this.fileID = new byte[16];
            random.nextBytes(this.fileID);
        }

        @Override
        byte[] getOriginalFileID() {
            return this.fileID;
        }

        @Override
        byte[] getUpdatedFileID() {
            return this.fileID;
        }

    }

    private static final class DigestFileIDGenerator extends FileIDGenerator {

        private byte[] fileID;

        private final PDFDocument document;

        private final MessageDigest digest;

        DigestFileIDGenerator(final PDFDocument document)
                throws NoSuchAlgorithmException {
            this.document = document;
            this.digest = MessageDigest.getInstance("MD5");
        }

        @Override
        byte[] getOriginalFileID() {
            if (this.fileID == null) {
                generateFileID();
            }
            return this.fileID;
        }

        @Override
        byte[] getUpdatedFileID() {
            return getOriginalFileID();
        }

        private void generateFileID() {
            final DateFormat df = new SimpleDateFormat(
                    "yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS");
            this.digest.update(PDFDocument.encode(df.format(new Date())));
            // Ignoring the filename here for simplicity even though it's
            // recommended
            // by the PDF spec
            this.digest.update(PDFDocument.encode(String.valueOf(this.document
                    .getCurrentFileSize())));
            this.digest.update(this.document.getInfo().toPDF());
            this.fileID = this.digest.digest();
        }

    }

    /**
     * Use this method when the file ID is needed before the document is
     * finalized. The digest method recommended by the PDF Reference is based,
     * among other things, on the file size.
     *
     * @return an instance that generates a random sequence of bytes for the
     *         File Identifier
     */
    static FileIDGenerator getRandomFileIDGenerator() {
        return new RandomFileIDGenerator();
    }

    /**
     * Returns an instance that generates a file ID using the digest method
     * recommended by the PDF Reference. To properly follow the Reference, the
     * size of the document must no longer change after this method is called.
     *
     * @param document
     *            the document whose File Identifier must be generated
     * @return the generator
     * @throws NoSuchAlgorithmException
     *             if the MD5 Digest algorithm is not available
     */
    static FileIDGenerator getDigestFileIDGenerator(final PDFDocument document)
            throws NoSuchAlgorithmException {
        return new DigestFileIDGenerator(document);
    }
}
