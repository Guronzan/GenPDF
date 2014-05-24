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

/* $Id: PDFEncryptionJCE.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * An implementation of the Standard Security Handler.
 */
public final class PDFEncryptionJCE extends PDFObject implements PDFEncryption {

    private final MessageDigest digest;

    private byte[] encryptionKey;

    private String encryptionDictionary;

    private class EncryptionInitializer {

        private final PDFEncryptionParams encryptionParams;

        private int encryptionLength;

        private int version;

        private int revision;

        EncryptionInitializer(final PDFEncryptionParams params) {
            this.encryptionParams = new PDFEncryptionParams(params);
        }

        void init() {
            this.encryptionLength = this.encryptionParams
                    .getEncryptionLengthInBits();
            determineEncryptionAlgorithm();
            final int permissions = Permission
                    .computePermissions(this.encryptionParams);
            final EncryptionSettings encryptionSettings = new EncryptionSettings(
                    this.encryptionLength, permissions,
                    this.encryptionParams.getUserPassword(),
                    this.encryptionParams.getOwnerPassword());
            final InitializationEngine initializationEngine = this.revision == 2 ? new Rev2Engine(
                    encryptionSettings) : new Rev3Engine(encryptionSettings);
                    initializationEngine.run();
                    PDFEncryptionJCE.this.encryptionDictionary = createEncryptionDictionary(
                    permissions, initializationEngine.oValue,
                            initializationEngine.uValue);
        }

        private void determineEncryptionAlgorithm() {
            if (isVersion1Revision2Algorithm()) {
                this.version = 1;
                this.revision = 2;
            } else {
                this.version = 2;
                this.revision = 3;
            }
        }

        private boolean isVersion1Revision2Algorithm() {
            return this.encryptionLength == 40
                    && this.encryptionParams.isAllowFillInForms()
                    && this.encryptionParams.isAllowAccessContent()
                    && this.encryptionParams.isAllowAssembleDocument()
                    && this.encryptionParams.isAllowPrintHq();
        }

        private String createEncryptionDictionary(final int permissions,
                final byte[] oValue, final byte[] uValue) {
            return "<< /Filter /Standard\n" + "/V " + this.version + "\n"
                    + "/R " + this.revision + "\n" + "/Length "
                    + this.encryptionLength + "\n" + "/P " + permissions + "\n"
                    + "/O " + PDFText.toHex(oValue) + "\n" + "/U "
                    + PDFText.toHex(uValue) + "\n" + ">>";
        }

    }

    private static enum Permission {

        PRINT(3), EDIT_CONTENT(4), COPY_CONTENT(5), EDIT_ANNOTATIONS(6), FILL_IN_FORMS(
                9), ACCESS_CONTENT(10), ASSEMBLE_DOCUMENT(11), PRINT_HQ(12);

        private final int mask;

        /**
         * Creates a new permission.
         *
         * @param bit
         *            bit position for this permission, 1-based to match the PDF
         *            Reference
         */
        private Permission(final int bit) {
            this.mask = 1 << bit - 1;
        }

        private int removeFrom(final int permissions) {
            return permissions - this.mask;
        }

        static int computePermissions(final PDFEncryptionParams encryptionParams) {
            int permissions = -4;

            if (!encryptionParams.isAllowPrint()) {
                permissions = PRINT.removeFrom(permissions);
            }
            if (!encryptionParams.isAllowCopyContent()) {
                permissions = COPY_CONTENT.removeFrom(permissions);
            }
            if (!encryptionParams.isAllowEditContent()) {
                permissions = EDIT_CONTENT.removeFrom(permissions);
            }
            if (!encryptionParams.isAllowEditAnnotations()) {
                permissions = EDIT_ANNOTATIONS.removeFrom(permissions);
            }
            if (!encryptionParams.isAllowFillInForms()) {
                permissions = FILL_IN_FORMS.removeFrom(permissions);
            }
            if (!encryptionParams.isAllowAccessContent()) {
                permissions = ACCESS_CONTENT.removeFrom(permissions);
            }
            if (!encryptionParams.isAllowAssembleDocument()) {
                permissions = ASSEMBLE_DOCUMENT.removeFrom(permissions);
            }
            if (!encryptionParams.isAllowPrintHq()) {
                permissions = PRINT_HQ.removeFrom(permissions);
            }
            return permissions;
        }
    }

    private static final class EncryptionSettings {

        final int encryptionLength; // CSOK: VisibilityModifier

        final int permissions; // CSOK: VisibilityModifier

        final String userPassword; // CSOK: VisibilityModifier

        final String ownerPassword; // CSOK: VisibilityModifier

        EncryptionSettings(final int encryptionLength, final int permissions,
                final String userPassword, final String ownerPassword) {
            this.encryptionLength = encryptionLength;
            this.permissions = permissions;
            this.userPassword = userPassword;
            this.ownerPassword = ownerPassword;
        }

    }

    private abstract class InitializationEngine {

        /** Padding for passwords. */
        protected final byte[] padding = new byte[] { (byte) 0x28, (byte) 0xBF,
                (byte) 0x4E, (byte) 0x5E, (byte) 0x4E, (byte) 0x75,
                (byte) 0x8A, (byte) 0x41, (byte) 0x64, (byte) 0x00,
                (byte) 0x4E, (byte) 0x56, (byte) 0xFF, (byte) 0xFA,
                (byte) 0x01, (byte) 0x08, (byte) 0x2E, (byte) 0x2E,
                (byte) 0x00, (byte) 0xB6, (byte) 0xD0, (byte) 0x68,
                (byte) 0x3E, (byte) 0x80, (byte) 0x2F, (byte) 0x0C,
                (byte) 0xA9, (byte) 0xFE, (byte) 0x64, (byte) 0x53,
                (byte) 0x69, (byte) 0x7A };

        protected final int encryptionLengthInBytes;

        private final int permissions;

        private byte[] oValue;

        private byte[] uValue;

        private final byte[] preparedUserPassword;

        protected final String ownerPassword;

        InitializationEngine(final EncryptionSettings encryptionSettings) {
            this.encryptionLengthInBytes = encryptionSettings.encryptionLength / 8;
            this.permissions = encryptionSettings.permissions;
            this.preparedUserPassword = preparePassword(encryptionSettings.userPassword);
            this.ownerPassword = encryptionSettings.ownerPassword;
        }

        void run() {
            this.oValue = computeOValue();
            createEncryptionKey();
            this.uValue = computeUValue();
        }

        /**
         * Applies Algorithm 3.3 Page 79 of the PDF 1.4 Reference.
         *
         * @return the O value
         */
        private byte[] computeOValue() {
            // Step 1
            final byte[] md5Input = prepareMD5Input();
            // Step 2
            PDFEncryptionJCE.this.digest.reset();
            byte[] hash = PDFEncryptionJCE.this.digest.digest(md5Input);
            // Step 3
            hash = computeOValueStep3(hash);
            // Step 4
            final byte[] key = new byte[this.encryptionLengthInBytes];
            System.arraycopy(hash, 0, key, 0, this.encryptionLengthInBytes);
            // Steps 5, 6
            byte[] encryptionResult = encryptWithKey(key,
                    this.preparedUserPassword);
            // Step 7
            encryptionResult = computeOValueStep7(key, encryptionResult);
            // Step 8
            return encryptionResult;
        }

        /**
         * Applies Algorithm 3.2 Page 78 of the PDF 1.4 Reference.
         */
        private void createEncryptionKey() {
            // Steps 1, 2
            PDFEncryptionJCE.this.digest.reset();
            PDFEncryptionJCE.this.digest.update(this.preparedUserPassword);
            // Step 3
            PDFEncryptionJCE.this.digest.update(this.oValue);
            // Step 4
            PDFEncryptionJCE.this.digest
                    .update((byte) (this.permissions >>> 0));
            PDFEncryptionJCE.this.digest
                    .update((byte) (this.permissions >>> 8));
            PDFEncryptionJCE.this.digest
                    .update((byte) (this.permissions >>> 16));
            PDFEncryptionJCE.this.digest
                    .update((byte) (this.permissions >>> 24));
            // Step 5
            PDFEncryptionJCE.this.digest.update(getDocumentSafely()
                    .getFileIDGenerator().getOriginalFileID());
            byte[] hash = PDFEncryptionJCE.this.digest.digest();
            // Step 6
            hash = createEncryptionKeyStep6(hash);
            // Step 7
            PDFEncryptionJCE.this.encryptionKey = new byte[this.encryptionLengthInBytes];
            System.arraycopy(hash, 0, PDFEncryptionJCE.this.encryptionKey, 0,
                    this.encryptionLengthInBytes);
        }

        protected abstract byte[] computeUValue();

        /**
         * Adds padding to the password as directed in page 78 of the PDF 1.4
         * Reference.
         *
         * @param password
         *            the password
         * @return the password with additional padding if necessary
         */
        private byte[] preparePassword(final String password) {
            final int finalLength = 32;
            final byte[] preparedPassword = new byte[finalLength];
            final byte[] passwordBytes = password.getBytes();
            System.arraycopy(passwordBytes, 0, preparedPassword, 0,
                    passwordBytes.length);
            System.arraycopy(this.padding, 0, preparedPassword,
                    passwordBytes.length, finalLength - passwordBytes.length);
            return preparedPassword;
        }

        private byte[] prepareMD5Input() {
            if (this.ownerPassword.length() != 0) {
                return preparePassword(this.ownerPassword);
            } else {
                return this.preparedUserPassword;
            }
        }

        protected abstract byte[] computeOValueStep3(final byte[] hash);

        protected abstract byte[] computeOValueStep7(final byte[] key,
                final byte[] encryptionResult);

        protected abstract byte[] createEncryptionKeyStep6(final byte[] hash);

    }

    private class Rev2Engine extends InitializationEngine {

        Rev2Engine(final EncryptionSettings encryptionSettings) {
            super(encryptionSettings);
        }

        @Override
        protected byte[] computeOValueStep3(final byte[] hash) {
            return hash;
        }

        @Override
        protected byte[] computeOValueStep7(final byte[] key,
                final byte[] encryptionResult) {
            return encryptionResult;
        }

        @Override
        protected byte[] createEncryptionKeyStep6(final byte[] hash) {
            return hash;
        }

        @Override
        protected byte[] computeUValue() {
            return encryptWithKey(PDFEncryptionJCE.this.encryptionKey,
                    this.padding);
        }

    }

    private class Rev3Engine extends InitializationEngine {

        Rev3Engine(final EncryptionSettings encryptionSettings) {
            super(encryptionSettings);
        }

        @Override
        protected byte[] computeOValueStep3(byte[] hash) {
            for (int i = 0; i < 50; i++) {
                hash = PDFEncryptionJCE.this.digest.digest(hash);
            }
            return hash;
        }

        @Override
        protected byte[] computeOValueStep7(final byte[] key,
                final byte[] encryptionResult) {
            return xorKeyAndEncrypt19Times(key, encryptionResult);
        }

        @Override
        protected byte[] createEncryptionKeyStep6(byte[] hash) {
            for (int i = 0; i < 50; i++) {
                PDFEncryptionJCE.this.digest.update(hash, 0,
                        this.encryptionLengthInBytes);
                hash = PDFEncryptionJCE.this.digest.digest();
            }
            return hash;
        }

        @Override
        protected byte[] computeUValue() {
            // Step 1 is encryptionKey
            // Step 2
            PDFEncryptionJCE.this.digest.reset();
            PDFEncryptionJCE.this.digest.update(this.padding);
            // Step 3
            PDFEncryptionJCE.this.digest.update(getDocumentSafely()
                    .getFileIDGenerator().getOriginalFileID());
            // Step 4
            byte[] encryptionResult = encryptWithKey(
                    PDFEncryptionJCE.this.encryptionKey,
                    PDFEncryptionJCE.this.digest.digest());
            // Step 5
            encryptionResult = xorKeyAndEncrypt19Times(
                    PDFEncryptionJCE.this.encryptionKey, encryptionResult);
            // Step 6
            final byte[] uValue = new byte[32];
            System.arraycopy(encryptionResult, 0, uValue, 0, 16);
            // Add the arbitrary padding
            Arrays.fill(uValue, 16, 32, (byte) 0);
            return uValue;
        }

        private byte[] xorKeyAndEncrypt19Times(final byte[] key,
                final byte[] input) {
            byte[] result = input;
            final byte[] encryptionKey = new byte[key.length];
            for (int i = 1; i <= 19; i++) {
                for (int j = 0; j < key.length; j++) {
                    encryptionKey[j] = (byte) (key[j] ^ i);
                }
                result = encryptWithKey(encryptionKey, result);
            }
            return result;
        }

    }

    private class EncryptionFilter extends PDFFilter {

        private final int streamNumber;

        private final int streamGeneration;

        EncryptionFilter(final int streamNumber, final int streamGeneration) {
            this.streamNumber = streamNumber;
            this.streamGeneration = streamGeneration;
        }

        /**
         * Returns a PDF string representation of this filter.
         *
         * @return the empty string
         */
        @Override
        public String getName() {
            return "";
        }

        /**
         * Returns a parameter dictionary for this filter.
         *
         * @return null, this filter has no parameters
         */
        @Override
        public PDFObject getDecodeParms() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public OutputStream applyFilter(final OutputStream out)
                throws IOException {
            final byte[] key = createEncryptionKey(this.streamNumber,
                    this.streamGeneration);
            final Cipher cipher = initCipher(key);
            return new CipherOutputStream(out, cipher);
        }

    }

    private PDFEncryptionJCE(final int objectNumber,
            final PDFEncryptionParams params, final PDFDocument pdf) {
        setObjectNumber(objectNumber);
        try {
            this.digest = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e.getMessage());
        }
        setDocument(pdf);
        final EncryptionInitializer encryptionInitializer = new EncryptionInitializer(
                params);
        encryptionInitializer.init();
    }

    /**
     * Creates and returns an encryption object.
     *
     * @param objectNumber
     *            the object number for the encryption dictionary
     * @param params
     *            the encryption parameters
     * @param pdf
     *            the PDF document to be encrypted
     * @return the newly created encryption object
     */
    public static PDFEncryption make(final int objectNumber,
            final PDFEncryptionParams params, final PDFDocument pdf) {
        return new PDFEncryptionJCE(objectNumber, params, pdf);
    }

    /** {@inheritDoc} */
    @Override
    public byte[] encrypt(final byte[] data, final PDFObject refObj) {
        PDFObject o = refObj;
        while (o != null && !o.hasObjectNumber()) {
            o = o.getParent();
        }
        if (o == null) {
            throw new IllegalStateException(
                    "No object number could be obtained for a PDF object");
        }
        final byte[] key = createEncryptionKey(o.getObjectNumber(),
                o.getGeneration());
        return encryptWithKey(key, data);
    }

    /** {@inheritDoc} */
    @Override
    public void applyFilter(final AbstractPDFStream stream) {
        stream.getFilterList().addFilter(
                new EncryptionFilter(stream.getObjectNumber(), stream
                        .getGeneration()));
    }

    /**
     * Prepares the encryption dictionary for output to a PDF file.
     *
     * @return the encryption dictionary as a byte array
     */
    @Override
    public byte[] toPDF() {
        assert this.encryptionDictionary != null;
        return encode(this.encryptionDictionary);
    }

    /** {@inheritDoc} */
    @Override
    public String getTrailerEntry() {
        return "/Encrypt " + getObjectNumber() + " " + getGeneration() + " R\n";
    }

    private static byte[] encryptWithKey(final byte[] key, final byte[] data) {
        try {
            final Cipher c = initCipher(key);
            return c.doFinal(data);
        } catch (final IllegalBlockSizeException e) {
            throw new IllegalStateException(e.getMessage());
        } catch (final BadPaddingException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private static Cipher initCipher(final byte[] key) {
        try {
            final Cipher c = Cipher.getInstance("RC4");
            final SecretKeySpec keyspec = new SecretKeySpec(key, "RC4");
            c.init(Cipher.ENCRYPT_MODE, keyspec);
            return c;
        } catch (final InvalidKeyException e) {
            throw new IllegalStateException(e);
        } catch (final NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e);
        } catch (final NoSuchPaddingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Applies Algorithm 3.1 from the PDF 1.4 Reference.
     *
     * @param objectNumber
     *            the object number
     * @param generationNumber
     *            the generation number
     * @return the key to use for encryption
     */
    private byte[] createEncryptionKey(final int objectNumber,
            final int generationNumber) {
        // Step 1 passed in
        // Step 2
        final byte[] md5Input = prepareMD5Input(objectNumber, generationNumber);
        // Step 3
        this.digest.reset();
        final byte[] hash = this.digest.digest(md5Input);
        // Step 4
        final int keyLength = Math.min(16, md5Input.length);
        final byte[] key = new byte[keyLength];
        System.arraycopy(hash, 0, key, 0, keyLength);
        return key;
    }

    private byte[] prepareMD5Input(final int objectNumber,
            final int generationNumber) {
        final byte[] md5Input = new byte[this.encryptionKey.length + 5];
        System.arraycopy(this.encryptionKey, 0, md5Input, 0,
                this.encryptionKey.length);
        int i = this.encryptionKey.length;
        md5Input[i++] = (byte) (objectNumber >>> 0);
        md5Input[i++] = (byte) (objectNumber >>> 8);
        md5Input[i++] = (byte) (objectNumber >>> 16);
        md5Input[i++] = (byte) (generationNumber >>> 0);
        md5Input[i++] = (byte) (generationNumber >>> 8);
        return md5Input;
    }

}
