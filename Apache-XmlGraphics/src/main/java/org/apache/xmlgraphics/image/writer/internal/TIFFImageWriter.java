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

/* $Id: TIFFImageWriter.java 1028385 2010-10-28 17:02:39Z jeremias $ */

package org.apache.xmlgraphics.image.writer.internal;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.xmlgraphics.image.codec.tiff.TIFFEncodeParam;
import org.apache.xmlgraphics.image.codec.tiff.TIFFField;
import org.apache.xmlgraphics.image.codec.tiff.TIFFImageDecoder;
import org.apache.xmlgraphics.image.codec.tiff.TIFFImageEncoder;
import org.apache.xmlgraphics.image.writer.AbstractImageWriter;
import org.apache.xmlgraphics.image.writer.ImageWriterParams;
import org.apache.xmlgraphics.image.writer.MultiImageWriter;

/**
 * ImageWriter implementation that uses the internal TIFF codec to write TIFF
 * files.
 *
 * @version $Id: TIFFImageWriter.java 1028385 2010-10-28 17:02:39Z jeremias $
 */
public class TIFFImageWriter extends AbstractImageWriter {

    /** {@inheritDoc} */
    @Override
    public void writeImage(final RenderedImage image, final OutputStream out)
            throws IOException {
        writeImage(image, out, null);
    }

    /** {@inheritDoc} */
    @Override
    public void writeImage(final RenderedImage image, final OutputStream out,
            final ImageWriterParams params) throws IOException {
        final TIFFEncodeParam encodeParams = createTIFFEncodeParams(params);
        final TIFFImageEncoder encoder = new TIFFImageEncoder(out, encodeParams);
        encoder.encode(image);
    }

    private TIFFEncodeParam createTIFFEncodeParams(
            final ImageWriterParams params) {
        final TIFFEncodeParam encodeParams = new TIFFEncodeParam();
        if (params == null) {
            encodeParams.setCompression(TIFFEncodeParam.COMPRESSION_NONE);
        } else {
            if (params.getCompressionMethod() == null) {
                // PackBits as default
                encodeParams
                .setCompression(TIFFEncodeParam.COMPRESSION_PACKBITS);
            } else if ("PackBits".equalsIgnoreCase(params
                    .getCompressionMethod())) {
                encodeParams
                .setCompression(TIFFEncodeParam.COMPRESSION_PACKBITS);
            } else if ("NONE".equalsIgnoreCase(params.getCompressionMethod())) {
                encodeParams.setCompression(TIFFEncodeParam.COMPRESSION_NONE);
            } else if ("Deflate"
                    .equalsIgnoreCase(params.getCompressionMethod())) {
                encodeParams
                .setCompression(TIFFEncodeParam.COMPRESSION_DEFLATE);
            } else {
                throw new UnsupportedOperationException(
                        "Compression method not supported: "
                                + params.getCompressionMethod());
            }

            if (params.getResolution() != null) {
                // Set target resolution
                final float pixSzMM = 25.4f / params.getResolution()
                        .floatValue();
                // num Pixs in 100 Meters
                final int numPix = (int) (1000 * 100 / pixSzMM + 0.5);
                final int denom = 100 * 100; // Centimeters per 100 Meters;
                final long[] rational = { numPix, denom };
                final TIFFField[] fields = {
                        new TIFFField(TIFFImageDecoder.TIFF_RESOLUTION_UNIT,
                                TIFFField.TIFF_SHORT, 1,
                                new char[] { (char) 3 }),
                                new TIFFField(TIFFImageDecoder.TIFF_X_RESOLUTION,
                                        TIFFField.TIFF_RATIONAL, 1,
                                        new long[][] { rational }),
                                        new TIFFField(TIFFImageDecoder.TIFF_Y_RESOLUTION,
                                                TIFFField.TIFF_RATIONAL, 1,
                                                new long[][] { rational }) };
                encodeParams.setExtraFields(fields);
            }
        }
        return encodeParams;
    }

    /** {@inheritDoc} */
    @Override
    public String getMIMEType() {
        return "image/tiff";
    }

    /** {@inheritDoc} */
    @Override
    public MultiImageWriter createMultiImageWriter(final OutputStream out)
            throws IOException {
        return new TIFFMultiImageWriter(out);
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsMultiImageWriter() {
        return true;
    }

    private class TIFFMultiImageWriter implements MultiImageWriter {

        private final OutputStream out;
        private TIFFEncodeParam encodeParams;
        private TIFFImageEncoder encoder;
        private Object context;

        public TIFFMultiImageWriter(final OutputStream out) {
            this.out = out;
        }

        @Override
        public void writeImage(final RenderedImage image,
                final ImageWriterParams params) throws IOException {
            if (this.encoder == null) {
                this.encodeParams = createTIFFEncodeParams(params);
                this.encoder = new TIFFImageEncoder(this.out, this.encodeParams);
            }
            this.context = this.encoder.encodeMultiple(this.context, image);
        }

        @Override
        public void close() throws IOException {
            if (this.encoder != null) {
                this.encoder.finishMultiple(this.context);
            }
            this.encoder = null;
            this.encodeParams = null;
            this.out.flush();
        }

    }

}
