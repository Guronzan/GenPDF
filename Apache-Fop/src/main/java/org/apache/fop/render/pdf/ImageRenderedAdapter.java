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

/* $Id: ImageRenderedAdapter.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.pdf;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.pdf.AlphaRasterImage;
import org.apache.fop.pdf.PDFColor;
import org.apache.fop.pdf.PDFDeviceColorSpace;
import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFFilter;
import org.apache.fop.pdf.PDFFilterList;
import org.apache.fop.pdf.PDFReference;
import org.apache.xmlgraphics.image.loader.impl.ImageRendered;
import org.apache.xmlgraphics.ps.ImageEncodingHelper;

/**
 * PDFImage implementation for the PDF renderer which handles RenderedImages.
 */
@Slf4j
public class ImageRenderedAdapter extends AbstractImageAdapter {

    private final ImageEncodingHelper encodingHelper;

    private final PDFFilter pdfFilter = null;
    private String maskRef;
    private PDFReference softMask;

    /**
     * Creates a new PDFImage from an Image instance.
     *
     * @param image
     *            the image
     * @param key
     *            XObject key
     */
    public ImageRenderedAdapter(final ImageRendered image, final String key) {
        super(image, key);
        this.encodingHelper = new ImageEncodingHelper(image.getRenderedImage(),
                true);
    }

    /**
     * Returns the ImageRendered instance for this adapter.
     *
     * @return the ImageRendered instance
     */
    public ImageRendered getImage() {
        return (ImageRendered) this.image;
    }

    /** {@inheritDoc} */
    @Override
    public int getWidth() {
        final RenderedImage ri = getImage().getRenderedImage();
        return ri.getWidth();
    }

    /** {@inheritDoc} */
    @Override
    public int getHeight() {
        final RenderedImage ri = getImage().getRenderedImage();
        return ri.getHeight();
    }

    private ColorModel getEffectiveColorModel() {
        return this.encodingHelper.getEncodedColorModel();
    }

    /** {@inheritDoc} */
    @Override
    protected ColorSpace getImageColorSpace() {
        return getEffectiveColorModel().getColorSpace();
    }

    /** {@inheritDoc} */
    @Override
    protected ICC_Profile getEffectiveICCProfile() {
        final ColorSpace cs = getImageColorSpace();
        if (cs instanceof ICC_ColorSpace) {
            final ICC_ColorSpace iccSpace = (ICC_ColorSpace) cs;
            return iccSpace.getProfile();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setup(final PDFDocument doc) {
        final RenderedImage ri = getImage().getRenderedImage();

        super.setup(doc);

        // Handle transparency mask if applicable
        final ColorModel orgcm = ri.getColorModel();
        if (orgcm.hasAlpha()
                && orgcm.getTransparency() == Transparency.TRANSLUCENT) {
            doc.getProfile().verifyTransparencyAllowed(
                    this.image.getInfo().getOriginalURI());
            // TODO Implement code to combine image with background color if
            // transparency is not
            // allowed (need BufferedImage support for that)

            final AlphaRasterImage alphaImage = new AlphaRasterImage("Mask:"
                    + getKey(), ri);
            this.softMask = doc.addImage(null, alphaImage).makeReference();
        }
    }

    /** {@inheritDoc} */
    @Override
    public PDFDeviceColorSpace getColorSpace() {
        // DeviceGray, DeviceRGB, or DeviceCMYK
        return toPDFColorSpace(getEffectiveColorModel().getColorSpace());
    }

    /** {@inheritDoc} */
    @Override
    public int getBitsPerComponent() {
        final ColorModel cm = getEffectiveColorModel();
        if (cm instanceof IndexColorModel) {
            final IndexColorModel icm = (IndexColorModel) cm;
            return icm.getComponentSize(0);
        } else {
            return cm.getComponentSize(0);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTransparent() {
        final ColorModel cm = getEffectiveColorModel();
        if (cm instanceof IndexColorModel) {
            if (cm.getTransparency() == Transparency.TRANSLUCENT) {
                return true;
            }
        }
        return getImage().getTransparentColor() != null;
    }

    /** {@inheritDoc} */
    @Override
    public PDFColor getTransparentColor() {
        final ColorModel cm = getEffectiveColorModel();
        if (cm instanceof IndexColorModel) {
            final IndexColorModel icm = (IndexColorModel) cm;
            if (cm.getTransparency() == Transparency.TRANSLUCENT) {
                final int transPixel = icm.getTransparentPixel();
                return new PDFColor(icm.getRed(transPixel),
                        icm.getGreen(transPixel), icm.getBlue(transPixel));
            }
        }
        return new PDFColor(getImage().getTransparentColor());
    }

    /** {@inheritDoc} */
    @Override
    public String getMask() {
        return this.maskRef;
    }

    /** {@inheritDoc} */
    @Override
    public PDFReference getSoftMaskReference() {
        return this.softMask;
    }

    /** {@inheritDoc} */
    @Override
    public PDFFilter getPDFFilter() {
        return this.pdfFilter;
    }

    /** {@inheritDoc} */
    @Override
    public void outputContents(final OutputStream out) throws IOException {
        final long start = System.currentTimeMillis();
        this.encodingHelper.encode(out);
        final long duration = System.currentTimeMillis() - start;
        if (log.isDebugEnabled()) {
            log.debug("Image encoding took " + duration + "ms");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void populateXObjectDictionary(final PDFDictionary dict) {
        final ColorModel cm = getEffectiveColorModel();
        if (cm instanceof IndexColorModel) {
            final IndexColorModel icm = (IndexColorModel) cm;
            super.populateXObjectDictionaryForIndexColorModel(dict, icm);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getFilterHint() {
        return PDFFilterList.IMAGE_FILTER;
    }

}
