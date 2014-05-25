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

/* $Id: PDFOutputIntent.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * Represents the OutputIntent dictionary.
 *
 * @since PDF 1.4
 */
@Slf4j
public class PDFOutputIntent extends PDFObject {

    /** Subtype for PDF/X output intents */
    public static final String GTS_PDFX = "GTS_PDFX";
    /** Subtype for PDF/A-1 output intents */
    public static final String GTS_PDFA1 = "GTS_PDFA1";

    private String subtype; // S in the PDF spec
    private String outputCondition;
    private String outputConditionIdentifier;
    private String registryName;
    private String info;
    private PDFICCStream destOutputProfile;

    /** @return the output intent subtype. */
    public String getSubtype() {
        return this.subtype;
    }

    /**
     * Sets the output intent subtype.
     *
     * @param subtype
     *            the subtype (usually "GTS_PDFX")
     */
    public void setSubtype(final String subtype) {
        this.subtype = subtype;
    }

    /** @return the OutputCondition field */
    public String getOutputCondition() {
        return this.outputCondition;
    }

    /**
     * Sets the human-readable form of the output condition.
     *
     * @param outputCondition
     *            A text string concisely identifying the intended output device
     *            or production condition in human-readable form.
     */
    public void setOutputCondition(final String outputCondition) {
        this.outputCondition = outputCondition;
    }

    /** @return the OutputConditionIdentifier field */
    public String getOutputConditionIdentifier() {
        return this.outputConditionIdentifier;
    }

    /**
     * Sets the identifier for the output condition.
     *
     * @param outputConditionIdentifier
     *            A string identifying the intended output device or production
     *            condition in human- or machine-readable form.
     */
    public void setOutputConditionIdentifier(
            final String outputConditionIdentifier) {
        this.outputConditionIdentifier = outputConditionIdentifier;
    }

    /** @return the RegistryName field */
    public String getRegistryName() {
        return this.registryName;
    }

    /**
     * Sets the registry name.
     *
     * @param registryName
     *            A string (conventionally a uniform resource identifier, or
     *            URI) identifying the registry in which the condition
     *            designated by OutputConditionIdentifier is defined.
     */
    public void setRegistryName(final String registryName) {
        this.registryName = registryName;
    }

    /** @return the Info field */
    public String getInfo() {
        return this.info;
    }

    /**
     * Sets the Info field.
     *
     * @param info
     *            A human-readable text string containing additional information
     *            or comments about the intended target device or production
     *            condition.
     */
    public void setInfo(final String info) {
        this.info = info;
    }

    /** @return the DestOutputProfile */
    public PDFICCStream getDestOutputProfile() {
        return this.destOutputProfile;
    }

    /**
     * Sets the destination ICC profile.
     *
     * @param destOutputProfile
     *            An ICC profile stream defining the transformation from the PDF
     *            document's source colors to output device colorants.
     */
    public void setDestOutputProfile(final PDFICCStream destOutputProfile) {
        this.destOutputProfile = destOutputProfile;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] toPDF() {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream(128);
        try {
            bout.write(encode("<<\n"));
            bout.write(encode("/Type /OutputIntent\n"));

            bout.write(encode("/S /"));
            bout.write(encode(this.subtype));
            bout.write(encode("\n"));

            if (this.outputCondition != null) {
                bout.write(encode("/OutputCondition "));
                bout.write(encodeText(this.outputCondition));
                bout.write(encode("\n"));
            }

            bout.write(encode("/OutputConditionIdentifier "));
            bout.write(encodeText(this.outputConditionIdentifier));
            bout.write(encode("\n"));

            if (this.registryName != null) {
                bout.write(encode("/RegistryName "));
                bout.write(encodeText(this.registryName));
                bout.write(encode("\n"));
            }

            if (this.info != null) {
                bout.write(encode("/Info "));
                bout.write(encodeText(this.info));
                bout.write(encode("\n"));
            }

            if (this.destOutputProfile != null) {
                bout.write(encode("/DestOutputProfile "
                        + this.destOutputProfile.referencePDF() + "\n"));
            }

            bout.write(encode(">>"));
        } catch (final IOException ioe) {
            log.error("Ignored I/O exception", ioe);
        }
        return bout.toByteArray();
    }

}
