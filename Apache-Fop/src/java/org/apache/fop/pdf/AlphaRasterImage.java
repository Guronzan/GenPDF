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

/* $Id: AlphaRasterImage.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.pdf;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.xmlgraphics.image.GraphicsUtil;

/**
 * PDFImage implementation for alpha channel "images".
 */
public class AlphaRasterImage implements PDFImage {

    private final int bitsPerComponent;
    private final PDFDeviceColorSpace colorSpace;
    private final Raster alpha;
    private final String key;

    /**
     * Create a alpha channel image. Creates a new bitmap image with the given
     * data.
     *
     * @param k
     *            the key to be used to lookup the image
     * @param alpha
     *            the alpha channel raster
     */
    public AlphaRasterImage(final String k, final Raster alpha) {
        this.key = k;
        // Enable the commented line below if 16-bit alpha channels are desired.
        // Otherwise, we compress the alpha channel to 8 bit which should be
        // sufficient.
        // this.bitsPerComponent = alpha.getSampleModel().getSampleSize(0);
        this.bitsPerComponent = 8;
        this.colorSpace = new PDFDeviceColorSpace(
                PDFDeviceColorSpace.DEVICE_GRAY);
        if (alpha == null) {
            throw new NullPointerException("Parameter alpha must not be null");
        }
        this.alpha = alpha;
    }

    /**
     * Create a alpha channel image. Extracts the alpha channel from the
     * RenderedImage and creates a new bitmap image with the given data.
     *
     * @param k
     *            the key to be used to lookup the image
     * @param image
     *            the image (must have an alpha channel)
     */
    public AlphaRasterImage(final String k, final RenderedImage image) {
        this(k, GraphicsUtil.getAlphaRaster(image));
    }

    /** {@inheritDoc} */
    @Override
    public void setup(final PDFDocument doc) {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public String getKey() {
        return this.key;
    }

    /** {@inheritDoc} */
    @Override
    public int getWidth() {
        return this.alpha.getWidth();
    }

    /** {@inheritDoc} */
    @Override
    public int getHeight() {
        return this.alpha.getHeight();
    }

    /** {@inheritDoc} */
    @Override
    public PDFDeviceColorSpace getColorSpace() {
        return this.colorSpace;
    }

    /** {@inheritDoc} */
    @Override
    public int getBitsPerComponent() {
        return this.bitsPerComponent;
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

    /** @return null (unless overridden) */
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
    public void outputContents(final OutputStream out) throws IOException {
        final int w = getWidth();
        final int h = getHeight();

        // Check Raster
        final int nbands = this.alpha.getNumBands();
        if (nbands != 1) {
            throw new UnsupportedOperationException(
                    "Expected only one band/component for the alpha channel");
        }

        // ...and write the Raster line by line with a reusable buffer
        final int dataType = this.alpha.getDataBuffer().getDataType();
        if (dataType == DataBuffer.TYPE_BYTE) {
            final byte[] line = new byte[nbands * w];
            for (int y = 0; y < h; y++) {
                this.alpha.getDataElements(0, y, w, 1, line);
                out.write(line);
            }
        } else if (dataType == DataBuffer.TYPE_USHORT) {
            final short[] sline = new short[nbands * w];
            final byte[] line = new byte[nbands * w];
            for (int y = 0; y < h; y++) {
                this.alpha.getDataElements(0, y, w, 1, sline);
                for (int i = 0; i < w; i++) {
                    // this compresses a 16-bit alpha channel to 8 bits!
                    // we probably don't ever need a 16-bit channel
                    line[i] = (byte) (sline[i] >> 8);
                }
                out.write(line);
            }
        } else if (dataType == DataBuffer.TYPE_INT) {
            // Is there an better way to get a 8bit raster from a TYPE_INT
            // raster?
            int shift = 24;
            final SampleModel sampleModel = this.alpha.getSampleModel();
            if (sampleModel instanceof SinglePixelPackedSampleModel) {
                final SinglePixelPackedSampleModel m = (SinglePixelPackedSampleModel) sampleModel;
                shift = m.getBitOffsets()[0];
            }
            final int[] iline = new int[nbands * w];
            final byte[] line = new byte[nbands * w];
            for (int y = 0; y < h; y++) {
                this.alpha.getDataElements(0, y, w, 1, iline);
                for (int i = 0; i < w; i++) {
                    line[i] = (byte) (iline[i] >> shift);
                }
                out.write(line);
            }
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported DataBuffer type: "
                            + this.alpha.getDataBuffer().getClass().getName());
        }

    }

    /** {@inheritDoc} */
    @Override
    public void populateXObjectDictionary(final PDFDictionary dict) {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public PDFICCStream getICCStream() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPS() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getFilterHint() {
        return PDFFilterList.IMAGE_FILTER;
    }

    /** {@inheritDoc} */
    @Override
    public PDFFilter getPDFFilter() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean multipleFiltersAllowed() {
        return true;
    }
}
