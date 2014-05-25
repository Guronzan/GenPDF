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

/* $Id$ */

// Original author: Matthias Reichenbacher

package org.apache.fop.render.pdf;

import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.pdf.BitmapImage;
import org.apache.fop.pdf.FlateFilter;
import org.apache.fop.pdf.PDFColor;
import org.apache.fop.pdf.PDFDeviceColorSpace;
import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFFilter;
import org.apache.fop.pdf.PDFFilterException;
import org.apache.fop.pdf.PDFFilterList;
import org.apache.fop.pdf.PDFICCStream;
import org.apache.fop.pdf.PDFReference;
import org.apache.xmlgraphics.image.loader.impl.ImageRawPNG;
import org.apache.xmlgraphics.image.loader.impl.ImageRawStream;

public class ImageRawPNGAdapter extends AbstractImageAdapter {

    private PDFICCStream pdfICCStream;
    private PDFFilter pdfFilter;
    private String maskRef;
    private PDFReference softMask;
    private int numberOfInterleavedComponents;

    /**
     * Creates a new PDFImage from an Image instance.
     *
     * @param image
     *            the image
     * @param key
     *            XObject key
     */
    public ImageRawPNGAdapter(final ImageRawPNG image, final String key) {
        super(image, key);
    }

    /** {@inheritDoc} */
    @Override
    public void setup(final PDFDocument doc) {
        super.setup(doc);
        final ColorModel cm = ((ImageRawPNG) this.image).getColorModel();
        if (cm instanceof IndexColorModel) {
            this.numberOfInterleavedComponents = 1;
        } else {
            // this can be 1 (gray), 2 (gray + alpha), 3 (rgb) or 4 (rgb +
            // alpha)
            // numberOfInterleavedComponents = (cm.hasAlpha() ? 1 : 0) +
            // cm.getNumColorComponents();
            this.numberOfInterleavedComponents = cm.getNumComponents();
        }

        // set up image compression for non-alpha channel
        FlateFilter flate;
        try {
            flate = new FlateFilter();
            flate.setApplied(true);
            flate.setPredictor(FlateFilter.PREDICTION_PNG_OPT);
            if (this.numberOfInterleavedComponents < 3) {
                // means palette (1) or gray (1) or gray + alpha (2)
                flate.setColors(1);
            } else {
                // means rgb (3) or rgb + alpha (4)
                flate.setColors(3);
            }
            flate.setColumns(this.image.getSize().getWidthPx());
            flate.setBitsPerComponent(getBitsPerComponent());
        } catch (final PDFFilterException e) {
            throw new RuntimeException("FlateFilter configuration error", e);
        }
        this.pdfFilter = flate;
        disallowMultipleFilters();

        // Handle transparency channel if applicable; note that for palette
        // images the transparency is
        // not TRANSLUCENT
        if (cm.hasAlpha() && cm.getTransparency() == Transparency.TRANSLUCENT) {
            doc.getProfile().verifyTransparencyAllowed(
                    this.image.getInfo().getOriginalURI());
            // TODO: Implement code to combine image with background color if
            // transparency is not allowed
            // here we need to inflate the PNG pixel data, which includes alpha,
            // separate the alpha channel
            // and then deflate it back again
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DeflaterOutputStream dos = new DeflaterOutputStream(baos,
                    new Deflater());
            final InputStream in = ((ImageRawStream) this.image)
                    .createInputStream();
            try {
                final InflaterInputStream infStream = new InflaterInputStream(
                        in, new Inflater());
                final DataInputStream dataStream = new DataInputStream(
                        infStream);
                // offset is the byte offset of the alpha component
                int offset = this.numberOfInterleavedComponents - 1; // 1 for
                // GA, 3
                // for RGBA
                final int numColumns = this.image.getSize().getWidthPx();
                final int bytesPerRow = this.numberOfInterleavedComponents
                        * numColumns;
                int filter;
                // read line by line; the first byte holds the filter
                while ((filter = dataStream.read()) != -1) {
                    final byte[] bytes = new byte[bytesPerRow];
                    dataStream.readFully(bytes, 0, bytesPerRow);
                    dos.write((byte) filter);
                    for (int j = 0; j < numColumns; j++) {
                        dos.write(bytes, offset, 1);
                        offset += this.numberOfInterleavedComponents;
                    }
                    offset = this.numberOfInterleavedComponents - 1;
                }
                dos.close();
            } catch (final IOException e) {
                throw new RuntimeException(
                        "Error processing transparency channel:", e);
            } finally {
                IOUtils.closeQuietly(in);
            }
            // set up alpha channel compression
            FlateFilter transFlate;
            try {
                transFlate = new FlateFilter();
                transFlate.setApplied(true);
                transFlate.setPredictor(FlateFilter.PREDICTION_PNG_OPT);
                transFlate.setColors(1);
                transFlate.setColumns(this.image.getSize().getWidthPx());
                transFlate.setBitsPerComponent(getBitsPerComponent());
            } catch (final PDFFilterException e) {
                throw new RuntimeException("FlateFilter configuration error", e);
            }
            final BitmapImage alphaMask = new BitmapImage("Mask:" + getKey(),
                    this.image.getSize().getWidthPx(), this.image.getSize()
                    .getHeightPx(), baos.toByteArray(), null);
            alphaMask.setPDFFilter(transFlate);
            alphaMask.disallowMultipleFilters();
            alphaMask.setColorSpace(new PDFDeviceColorSpace(
                    PDFDeviceColorSpace.DEVICE_GRAY));
            this.softMask = doc.addImage(null, alphaMask).makeReference();
        }
    }

    /** {@inheritDoc} */
    @Override
    public PDFDeviceColorSpace getColorSpace() {
        // DeviceGray, DeviceRGB, or DeviceCMYK
        return toPDFColorSpace(this.image.getColorSpace());
    }

    /** {@inheritDoc} */
    @Override
    public int getBitsPerComponent() {
        return ((ImageRawPNG) this.image).getBitDepth();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTransparent() {
        return ((ImageRawPNG) this.image).isTransparent();
    }

    /** {@inheritDoc} */
    @Override
    public PDFColor getTransparentColor() {
        return new PDFColor(((ImageRawPNG) this.image).getTransparentColor());
    }

    /** {@inheritDoc} */
    @Override
    public String getMask() {
        return this.maskRef;
    }

    /** {@inheritDoc} */
    @Override
    public String getSoftMask() {
        return this.softMask.toString();
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
        final InputStream in = ((ImageRawStream) this.image)
                .createInputStream();

        try {
            if (this.numberOfInterleavedComponents == 1
                    || this.numberOfInterleavedComponents == 3) {
                // means we have Gray, RGB, or Palette
                IOUtils.copy(in, out);
            } else {
                // means we have Gray + alpha or RGB + alpha
                // TODO: since we have alpha here do this when the alpha channel
                // is extracted
                final int numBytes = this.numberOfInterleavedComponents - 1; // 1
                // for
                // Gray,
                // 3
                // for
                // RGB
                final int numColumns = this.image.getSize().getWidthPx();
                final InflaterInputStream infStream = new InflaterInputStream(
                        in, new Inflater());
                final DataInputStream dataStream = new DataInputStream(
                        infStream);
                int offset = 0;
                final int bytesPerRow = this.numberOfInterleavedComponents
                        * numColumns;
                int filter;
                // here we need to inflate the PNG pixel data, which includes
                // alpha, separate the alpha
                // channel and then deflate the RGB channels back again
                final DeflaterOutputStream dos = new DeflaterOutputStream(out,
                        new Deflater());
                while ((filter = dataStream.read()) != -1) {
                    final byte[] bytes = new byte[bytesPerRow];
                    dataStream.readFully(bytes, 0, bytesPerRow);
                    dos.write((byte) filter);
                    for (int j = 0; j < numColumns; j++) {
                        dos.write(bytes, offset, numBytes);
                        offset += this.numberOfInterleavedComponents;
                    }
                    offset = 0;
                }
                dos.close();
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /** {@inheritDoc} */
    @Override
    public PDFICCStream getICCStream() {
        return this.pdfICCStream;
    }

    /** {@inheritDoc} */
    @Override
    public String getFilterHint() {
        return PDFFilterList.PRECOMPRESSED_FILTER;
    }

    @Override
    public void populateXObjectDictionary(final PDFDictionary dict) {
        final ColorModel cm = ((ImageRawPNG) this.image).getColorModel();
        if (cm instanceof IndexColorModel) {
            final IndexColorModel icm = (IndexColorModel) cm;
            super.populateXObjectDictionaryForIndexColorModel(dict, icm);
        }
    }
}
