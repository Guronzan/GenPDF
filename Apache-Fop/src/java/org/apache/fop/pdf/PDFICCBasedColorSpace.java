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

/* $Id: PDFICCBasedColorSpace.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.xmlgraphics.java2d.color.profile.ColorProfileUtil;

/**
 * Represents an ICCBased color space in PDF.
 */
public class PDFICCBasedColorSpace extends PDFObject implements PDFColorSpace {

    private final PDFICCStream iccStream;
    private final String explicitName;
    private final int numComponents;

    /**
     * Constructs a the ICCBased color space with an explicit name (ex.
     * "DefaultRGB").
     * 
     * @param explicitName
     *            an explicit name or null if a name should be generated
     * @param iccStream
     *            the ICC stream to associate with this color space
     */
    public PDFICCBasedColorSpace(final String explicitName,
            final PDFICCStream iccStream) {
        this.explicitName = explicitName;
        this.iccStream = iccStream;
        this.numComponents = iccStream.getICCProfile().getNumComponents();
    }

    /**
     * Constructs a the ICCBased color space.
     * 
     * @param iccStream
     *            the ICC stream to associate with this color space
     */
    public PDFICCBasedColorSpace(final PDFICCStream iccStream) {
        this(null, iccStream);
    }

    /** @return the ICC stream associated with this color space */
    public PDFICCStream getICCStream() {
        return this.iccStream;
    }

    /** {@inheritDoc} */
    @Override
    public int getNumComponents() {
        return this.numComponents;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        if (this.explicitName != null) {
            return this.explicitName;
        } else {
            return "ICC" + this.iccStream.getObjectNumber();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDeviceColorSpace() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRGBColorSpace() {
        return getNumComponents() == 3;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCMYKColorSpace() {
        return getNumComponents() == 4;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isGrayColorSpace() {
        return getNumComponents() == 1;
    }

    /** {@inheritDoc} */
    @Override
    protected String toPDFString() {
        final StringBuilder sb = new StringBuilder(64);
        sb.append("[/ICCBased ").append(getICCStream().referencePDF())
                .append("]");
        return sb.toString();
    }

    /**
     * Sets sRGB as the DefaultRGB color space in the PDF document.
     * 
     * @param pdfDoc
     *            the PDF document
     * @return the newly installed color space object
     */
    public static PDFICCBasedColorSpace setupsRGBAsDefaultRGBColorSpace(
            final PDFDocument pdfDoc) {
        final PDFICCStream sRGBProfile = setupsRGBColorProfile(pdfDoc);

        // Map sRGB as default RGB profile for DeviceRGB
        return pdfDoc.getFactory().makeICCBasedColorSpace(null, "DefaultRGB",
                sRGBProfile);
    }

    /**
     * Installs the sRGB color space in the PDF document.
     * 
     * @param pdfDoc
     *            the PDF document
     * @return the newly installed color space object
     */
    public static PDFICCBasedColorSpace setupsRGBColorSpace(
            final PDFDocument pdfDoc) {
        final PDFICCStream sRGBProfile = setupsRGBColorProfile(pdfDoc);

        // Map sRGB as default RGB profile for DeviceRGB
        return pdfDoc.getFactory().makeICCBasedColorSpace(null, null,
                sRGBProfile);
    }

    /**
     * Sets up the sRGB color profile in the PDF document. It does so by trying
     * to install a very small ICC profile (~4KB) instead of the very big one
     * (~140KB) the Sun JVM uses.
     * 
     * @param pdfDoc
     *            the PDF document
     * @return the ICC stream with the sRGB profile
     */
    public static PDFICCStream setupsRGBColorProfile(final PDFDocument pdfDoc) {
        ICC_Profile profile;
        final PDFICCStream sRGBProfile = pdfDoc.getFactory().makePDFICCStream();
        final InputStream in = PDFDocument.class
                .getResourceAsStream("sRGB Color Space Profile.icm");
        if (in != null) {
            try {
                profile = ColorProfileUtil.getICC_Profile(in);
            } catch (final IOException ioe) {
                throw new RuntimeException(
                        "Unexpected IOException loading the sRGB profile: "
                                + ioe.getMessage());
            } finally {
                IOUtils.closeQuietly(in);
            }
        } else {
            // Fallback: Use the sRGB profile from the JRE (about 140KB)
            profile = ColorProfileUtil.getICC_Profile(ColorSpace.CS_sRGB);
        }
        sRGBProfile.setColorSpace(profile, null);
        return sRGBProfile;
    }

}
