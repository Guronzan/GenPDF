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

/* $Id: BitmapImage.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Bitmap image. This is used to create a bitmap image that will be inserted
 * into pdf.
 */
public class BitmapImage implements PDFImage {
    private final int height;
    private final int width;
    private final int bitsPerComponent;
    private PDFDeviceColorSpace colorSpace;
    private final byte[] bitmaps;
    private PDFReference maskRef;
    private PDFColor transparent = null;
    private final String key;
    private PDFDocument pdfDoc;
    private PDFFilter pdfFilter;
    private boolean multipleFiltersAllowed = true;

    /**
     * Create a bitmap image. Creates a new bitmap image with the given data.
     *
     * @param k
     *            the key to be used to lookup the image
     * @param width
     *            the width of the image
     * @param height
     *            the height of the image
     * @param data
     *            the bitmap data
     * @param mask
     *            the transparency mask reference if any
     */
    public BitmapImage(final String k, final int width, final int height,
            final byte[] data, final String mask) {
        this.key = k;
        this.height = height;
        this.width = width;
        this.bitsPerComponent = 8;
        this.colorSpace = new PDFDeviceColorSpace(
                PDFDeviceColorSpace.DEVICE_RGB);
        this.bitmaps = data;
        if (mask != null) {
            this.maskRef = new PDFReference(mask);
        }
    }

    /**
     * Setup this image with the pdf document.
     *
     * @param doc
     *            the pdf document this will be inserted into
     */
    @Override
    public void setup(final PDFDocument doc) {
        this.pdfDoc = doc;
    }

    /**
     * Get the key for this image. This key is used by the pdf document so that
     * it will only insert an image once. All other references to the same image
     * will use the same XObject reference.
     *
     * @return the unique key to identify this image
     */
    @Override
    public String getKey() {
        return this.key;
    }

    /**
     * Get the width of this image.
     *
     * @return the width of the image
     */
    @Override
    public int getWidth() {
        return this.width;
    }

    /**
     * Get the height of this image.
     *
     * @return the height of the image
     */
    @Override
    public int getHeight() {
        return this.height;
    }

    /**
     * Set the color space for this image.
     *
     * @param cs
     *            the pdf color space
     */
    public void setColorSpace(final PDFDeviceColorSpace cs) {
        this.colorSpace = cs;
    }

    /**
     * Get the color space for the image data. Possible options are: DeviceGray,
     * DeviceRGB, or DeviceCMYK
     *
     * @return the pdf doclor space
     */
    @Override
    public PDFDeviceColorSpace getColorSpace() {
        return this.colorSpace;
    }

    /** {@inheritDoc} */
    @Override
    public int getBitsPerComponent() {
        return this.bitsPerComponent;
    }

    /**
     * Set the transparent color for this iamge.
     *
     * @param t
     *            the transparent color
     */
    public void setTransparent(final PDFColor t) {
        this.transparent = t;
    }

    /**
     * Check if this image has a transparent color.
     *
     * @return true if it has a transparent color
     */
    @Override
    public boolean isTransparent() {
        return this.transparent != null;
    }

    /**
     * Get the transparent color for this image.
     *
     * @return the transparent color if any
     */
    @Override
    public PDFColor getTransparentColor() {
        return this.transparent;
    }

    /**
     * Get the bitmap mask reference for this image. Current not supported.
     *
     * @return the bitmap mask reference
     */
    @Override
    public String getMask() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public PDFReference getSoftMaskReference() {
        return this.maskRef;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInverted() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void outputContents(final OutputStream out) throws IOException {
        out.write(this.bitmaps);
    }

    /** {@inheritDoc} */
    @Override
    public void populateXObjectDictionary(final PDFDictionary dict) {
        // nop
    }

    /**
     * Get the ICC stream.
     * 
     * @return always returns null since this has no icc color space
     */
    @Override
    public PDFICCStream getICCStream() {
        return null;
    }

    /**
     * Check if this is a postscript image.
     * 
     * @return always returns false
     */
    @Override
    public boolean isPS() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilterHint() {
        return PDFFilterList.IMAGE_FILTER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PDFFilter getPDFFilter() {
        return this.pdfFilter;
    }

    public void setPDFFilter(final PDFFilter pdfFilter) {
        this.pdfFilter = pdfFilter;
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
