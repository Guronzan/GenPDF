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

/* $Id: AbstractImageAdapter.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.pdf;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.IndexColorModel;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.pdf.PDFArray;
import org.apache.fop.pdf.PDFColor;
import org.apache.fop.pdf.PDFConformanceException;
import org.apache.fop.pdf.PDFDeviceColorSpace;
import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFICCBasedColorSpace;
import org.apache.fop.pdf.PDFICCStream;
import org.apache.fop.pdf.PDFImage;
import org.apache.fop.pdf.PDFName;
import org.apache.fop.pdf.PDFReference;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.java2d.color.profile.ColorProfileUtil;

/**
 * Abstract PDFImage implementation for the PDF renderer.
 */
@Slf4j
public abstract class AbstractImageAdapter implements PDFImage {

    private final String key;
    /** the image */
    protected Image image;

    private PDFICCStream pdfICCStream;

    private static final int MAX_HIVAL = 255;

    private boolean multipleFiltersAllowed = true;

    /**
     * Creates a new PDFImage from an Image instance.
     *
     * @param image
     *            the image
     * @param key
     *            XObject key
     */
    public AbstractImageAdapter(final Image image, final String key) {
        this.image = image;
        this.key = key;
        if (log.isDebugEnabled()) {
            log.debug("New ImageAdapter created for key: " + key);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getKey() {
        // key to look up XObject
        return this.key;
    }

    /**
     * Returns the image's color space.
     *
     * @return the color space
     */
    protected ColorSpace getImageColorSpace() {
        return this.image.getColorSpace();
    }

    /** {@inheritDoc} */
    @Override
    public void setup(final PDFDocument doc) {

        final ICC_Profile prof = getEffectiveICCProfile();
        final PDFDeviceColorSpace pdfCS = toPDFColorSpace(getImageColorSpace());
        if (prof != null) {
            this.pdfICCStream = setupColorProfile(doc, prof, pdfCS);
        }
        if (doc.getProfile().getPDFAMode().isPDFA1LevelB()) {
            if (pdfCS != null
                    && pdfCS.getColorSpace() != PDFDeviceColorSpace.DEVICE_RGB
                    && pdfCS.getColorSpace() != PDFDeviceColorSpace.DEVICE_GRAY
                    && prof == null) {
                // See PDF/A-1, ISO 19005:1:2005(E), 6.2.3.3
                // FOP is currently restricted to DeviceRGB if PDF/A-1 is
                // active.
                throw new PDFConformanceException(
                        "PDF/A-1 does not allow mixing DeviceRGB and DeviceCMYK: "
                                + this.image.getInfo());
            }
        }
    }

    /**
     * Returns the effective ICC profile for the image.
     *
     * @return an ICC profile or null
     */
    protected ICC_Profile getEffectiveICCProfile() {
        return this.image.getICCProfile();
    }

    private static PDFICCStream setupColorProfile(final PDFDocument doc,
            final ICC_Profile prof, final PDFDeviceColorSpace pdfCS) {
        final boolean defaultsRGB = ColorProfileUtil.isDefaultsRGB(prof);
        final String desc = ColorProfileUtil.getICCProfileDescription(prof);
        if (log.isDebugEnabled()) {
            log.debug("Image returns ICC profile: " + desc + ", default sRGB="
                    + defaultsRGB);
        }
        PDFICCBasedColorSpace cs = doc.getResources()
                .getICCColorSpaceByProfileName(desc);
        PDFICCStream pdfICCStream;
        if (!defaultsRGB) {
            if (cs == null) {
                pdfICCStream = doc.getFactory().makePDFICCStream();
                pdfICCStream.setColorSpace(prof, pdfCS);
                cs = doc.getFactory().makeICCBasedColorSpace(null, null,
                        pdfICCStream);
            } else {
                pdfICCStream = cs.getICCStream();
            }
        } else {
            if (cs == null) {
                if (desc == null || !desc.startsWith("sRGB")) {
                    log.warn("The default sRGB profile was indicated,"
                            + " but the profile description does not match what was expected: "
                            + desc);
                }
                // It's the default sRGB profile which we mapped to DefaultRGB
                // in PDFRenderer
                cs = (PDFICCBasedColorSpace) doc.getResources().getColorSpace(
                        new PDFName("DefaultRGB"));
            }
            if (cs == null) {
                // sRGB hasn't been set up for the PDF document
                // so install but don't set to DefaultRGB
                cs = PDFICCBasedColorSpace.setupsRGBColorSpace(doc);
            }
            pdfICCStream = cs.getICCStream();
        }
        return pdfICCStream;
    }

    /** {@inheritDoc} */
    @Override
    public int getWidth() {
        return this.image.getSize().getWidthPx();
    }

    /** {@inheritDoc} */
    @Override
    public int getHeight() {
        return this.image.getSize().getHeightPx();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTransparent() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public PDFColor getTransparentColor() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getMask() {
        return null;
    }

    /** @return null (if not overridden) */
    public String getSoftMask() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public PDFReference getSoftMaskReference() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInverted() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPS() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public PDFICCStream getICCStream() {
        return this.pdfICCStream;
    }

    /** {@inheritDoc} */
    @Override
    public void populateXObjectDictionary(final PDFDictionary dict) {
        // nop
    }

    /**
     * This is to be used by populateXObjectDictionary() when the image is
     * palette based.
     *
     * @param dict
     *            the dictionary to fill in
     * @param icm
     *            the image color model
     */
    protected void populateXObjectDictionaryForIndexColorModel(
            final PDFDictionary dict, final IndexColorModel icm) {
        final PDFArray indexed = new PDFArray(dict);
        indexed.add(new PDFName("Indexed"));
        if (icm.getColorSpace().getType() != ColorSpace.TYPE_RGB) {
            log.warn("Indexed color space is not using RGB as base color space."
                    + " The image may not be handled correctly."
                    + " Base color space: "
                    + icm.getColorSpace()
                    + " Image: "
                    + this.image.getInfo());
        }
        indexed.add(new PDFName(toPDFColorSpace(icm.getColorSpace()).getName()));
        final int c = icm.getMapSize();
        final int hival = c - 1;
        if (hival > MAX_HIVAL) {
            throw new UnsupportedOperationException("hival must not go beyond "
                    + MAX_HIVAL);
        }
        indexed.add(Integer.valueOf(hival));
        final int[] palette = new int[c];
        icm.getRGBs(palette);
        final ByteArrayOutputStream baout = new ByteArrayOutputStream();
        for (int i = 0; i < c; i++) {
            // TODO Probably doesn't work for non RGB based color spaces
            // See log warning above
            final int entry = palette[i];
            baout.write((entry & 0xFF0000) >> 16);
            baout.write((entry & 0xFF00) >> 8);
            baout.write(entry & 0xFF);
        }
        indexed.add(baout.toByteArray());

        dict.put("ColorSpace", indexed);
        dict.put("BitsPerComponent", icm.getPixelSize());

        final Integer index = getIndexOfFirstTransparentColorInPalette(icm);
        if (index != null) {
            final PDFArray mask = new PDFArray(dict);
            mask.add(index);
            mask.add(index);
            dict.put("Mask", mask);
        }
    }

    private static Integer getIndexOfFirstTransparentColorInPalette(
            final IndexColorModel icm) {
        final byte[] alphas = new byte[icm.getMapSize()];
        final byte[] reds = new byte[icm.getMapSize()];
        final byte[] greens = new byte[icm.getMapSize()];
        final byte[] blues = new byte[icm.getMapSize()];
        icm.getAlphas(alphas);
        icm.getReds(reds);
        icm.getGreens(greens);
        icm.getBlues(blues);
        for (int i = 0; i < icm.getMapSize(); i++) {
            if ((alphas[i] & 0xFF) == 0) {
                return Integer.valueOf(i);
            }
        }
        return null;
    }

    /**
     * Converts a ColorSpace object to a PDFColorSpace object.
     *
     * @param cs
     *            ColorSpace instance
     * @return PDFColorSpace new converted object
     */
    public static PDFDeviceColorSpace toPDFColorSpace(final ColorSpace cs) {
        if (cs == null) {
            return null;
        }

        final PDFDeviceColorSpace pdfCS = new PDFDeviceColorSpace(0);
        switch (cs.getType()) {
        case ColorSpace.TYPE_CMYK:
            pdfCS.setColorSpace(PDFDeviceColorSpace.DEVICE_CMYK);
            break;
        case ColorSpace.TYPE_GRAY:
            pdfCS.setColorSpace(PDFDeviceColorSpace.DEVICE_GRAY);
            break;
        default:
            pdfCS.setColorSpace(PDFDeviceColorSpace.DEVICE_RGB);
        }
        return pdfCS;
    }

    /** {@inheritDoc} */
    @Override
    public boolean multipleFiltersAllowed() {
        return this.multipleFiltersAllowed;
    }

    /**
     * Disallows multiple filters.
     */
    public void disallowMultipleFilters() {
        this.multipleFiltersAllowed = false;
    }

}
