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

/* $Id: TIFFImage.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.image.codec.tiff;

import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.apache.xmlgraphics.image.codec.util.PropertyUtil;
import org.apache.xmlgraphics.image.codec.util.SeekableStream;
import org.apache.xmlgraphics.image.rendered.AbstractRed;
import org.apache.xmlgraphics.image.rendered.CachableRed;

// CSOFF: LocalVariableName
// CSOFF: MissingSwitchDefault
// CSOFF: MultipleVariableDeclarations
// CSOFF: OperatorWrap
// CSOFF: WhitespaceAround

public class TIFFImage extends AbstractRed {

    // Compression types
    public static final int COMP_NONE = 1;
    public static final int COMP_FAX_G3_1D = 2;
    public static final int COMP_FAX_G3_2D = 3;
    public static final int COMP_FAX_G4_2D = 4;
    public static final int COMP_LZW = 5;
    public static final int COMP_JPEG_OLD = 6;
    public static final int COMP_JPEG_TTN2 = 7;
    public static final int COMP_PACKBITS = 32773;
    public static final int COMP_DEFLATE = 32946;

    // Image types
    private static final int TYPE_UNSUPPORTED = -1;
    private static final int TYPE_BILEVEL = 0;
    private static final int TYPE_GRAY_4BIT = 1;
    private static final int TYPE_GRAY = 2;
    private static final int TYPE_GRAY_ALPHA = 3;
    private static final int TYPE_PALETTE = 4;
    private static final int TYPE_RGB = 5;
    private static final int TYPE_RGB_ALPHA = 6;
    private static final int TYPE_YCBCR_SUB = 7;
    private static final int TYPE_GENERIC = 8;

    // Incidental tags
    private static final int TIFF_JPEG_TABLES = 347;
    private static final int TIFF_YCBCR_SUBSAMPLING = 530;

    SeekableStream stream;
    int tileSize;
    int tilesX, tilesY;
    long[] tileOffsets;
    long[] tileByteCounts;
    char[] colormap;
    int sampleSize;
    int compression;
    byte[] palette;
    int numBands;

    int chromaSubH;
    int chromaSubV;

    // Fax compression related variables
    long tiffT4Options;
    long tiffT6Options;
    int fillOrder;

    // LZW compression related variable
    int predictor;

    // DEFLATE variables
    Inflater inflater = null;

    // Endian-ness indicator
    boolean isBigEndian;

    int imageType;
    boolean isWhiteZero = false;
    int dataType;

    boolean decodePaletteAsShorts;
    boolean tiled;

    // Decoders
    private TIFFFaxDecoder decoder = null;
    private TIFFLZWDecoder lzwDecoder = null;

    /**
     * Inflates <code>deflated</code> into <code>inflated</code> using the
     * <code>Inflater</code> constructed during class instantiation.
     */
    private void inflate(final byte[] deflated, final byte[] inflated) {
        this.inflater.setInput(deflated);
        try {
            this.inflater.inflate(inflated);
        } catch (final DataFormatException dfe) {
            throw new RuntimeException(PropertyUtil.getString("TIFFImage17")
                    + ": " + dfe.getMessage());
        }
        this.inflater.reset();
    }

    private static SampleModel createPixelInterleavedSampleModel(
            final int dataType, final int tileWidth, final int tileHeight,
            final int bands) {
        final int[] bandOffsets = new int[bands];
        for (int i = 0; i < bands; i++) {
            bandOffsets[i] = i;
        }
        return new PixelInterleavedSampleModel(dataType, tileWidth, tileHeight,
                bands, tileWidth * bands, bandOffsets);
    }

    /**
     * Return as a long[] the value of a TIFF_LONG or TIFF_SHORT field.
     */
    private long[] getFieldAsLongs(final TIFFField field) {
        long[] value = null;

        if (field.getType() == TIFFField.TIFF_SHORT) {
            final char[] charValue = field.getAsChars();
            value = new long[charValue.length];
            for (int i = 0; i < charValue.length; i++) {
                value[i] = charValue[i] & 0xffff;
            }
        } else if (field.getType() == TIFFField.TIFF_LONG) {
            value = field.getAsLongs();
        } else {
            throw new RuntimeException(PropertyUtil.getString("TIFFImage18")
                    + ": " + field.getType());
        }

        return value;
    }

    /**
     * Constructs a TIFFImage that acquires its data from a given SeekableStream
     * and reads from a particular IFD of the stream. The index of the first IFD
     * is 0.
     *
     * @param stream
     *            the SeekableStream to read from.
     * @param param
     *            an instance of TIFFDecodeParam, or null.
     * @param directory
     *            the index of the IFD to read from.
     */
    public TIFFImage(final SeekableStream stream, TIFFDecodeParam param,
            final int directory) throws IOException {

        this.stream = stream;
        if (param == null) {
            param = new TIFFDecodeParam();
        }

        this.decodePaletteAsShorts = param.getDecodePaletteAsShorts();

        // Read the specified directory.
        final TIFFDirectory dir = param.getIFDOffset() == null ? new TIFFDirectory(
                stream, directory) : new TIFFDirectory(stream, param
                .getIFDOffset().longValue(), directory);

                // Get the number of samples per pixel
                final TIFFField sfield = dir
                .getField(TIFFImageDecoder.TIFF_SAMPLES_PER_PIXEL);
                final int samplesPerPixel = sfield == null ? 1 : (int) sfield
                .getAsLong(0);

                // Read the TIFF_PLANAR_CONFIGURATION field
                final TIFFField planarConfigurationField = dir
                .getField(TIFFImageDecoder.TIFF_PLANAR_CONFIGURATION);
                final char[] planarConfiguration = planarConfigurationField == null ? new char[] { 1 }
                : planarConfigurationField.getAsChars();

        // Support planar format (band sequential) only for 1 sample/pixel.
        if (planarConfiguration[0] != 1 && samplesPerPixel != 1) {
            throw new RuntimeException(PropertyUtil.getString("TIFFImage0"));
        }

        // Read the TIFF_BITS_PER_SAMPLE field
        final TIFFField bitsField = dir
                .getField(TIFFImageDecoder.TIFF_BITS_PER_SAMPLE);
        char[] bitsPerSample = null;
        if (bitsField != null) {
            bitsPerSample = bitsField.getAsChars();
        } else {
            bitsPerSample = new char[] { 1 };

            // Ensure that all samples have the same bit depth.
            for (int i = 1; i < bitsPerSample.length; i++) {
                if (bitsPerSample[i] != bitsPerSample[0]) {
                    throw new RuntimeException(
                            PropertyUtil.getString("TIFFImage1"));
                }
            }
        }
        this.sampleSize = bitsPerSample[0];

        // Read the TIFF_SAMPLE_FORMAT tag to see whether the data might be
        // signed or floating point
        final TIFFField sampleFormatField = dir
                .getField(TIFFImageDecoder.TIFF_SAMPLE_FORMAT);

        char[] sampleFormat = null;
        if (sampleFormatField != null) {
            sampleFormat = sampleFormatField.getAsChars();

            // Check that all the samples have the same format
            for (int l = 1; l < sampleFormat.length; l++) {
                if (sampleFormat[l] != sampleFormat[0]) {
                    throw new RuntimeException(
                            PropertyUtil.getString("TIFFImage2"));
                }
            }

        } else {
            sampleFormat = new char[] { 1 };
        }

        // Set the data type based on the sample size and format.
        boolean isValidDataFormat = false;
        switch (this.sampleSize) {
        case 1:
        case 4:
        case 8:
            if (sampleFormat[0] != 3) {
                // Ignore whether signed or unsigned: treat all as unsigned.
                this.dataType = DataBuffer.TYPE_BYTE;
                isValidDataFormat = true;
            }
            break;
        case 16:
            if (sampleFormat[0] != 3) {
                this.dataType = sampleFormat[0] == 2 ? DataBuffer.TYPE_SHORT
                        : DataBuffer.TYPE_USHORT;
                isValidDataFormat = true;
            }
            break;
        case 32:
            if (sampleFormat[0] == 3) {
                                isValidDataFormat = false;
                            } else {
                                this.dataType = DataBuffer.TYPE_INT;
                                isValidDataFormat = true;
            }
            break;
        }

        if (!isValidDataFormat) {
            throw new RuntimeException(PropertyUtil.getString("TIFFImage3"));
        }

        // Figure out what compression if any, is being used.
        final TIFFField compField = dir
                .getField(TIFFImageDecoder.TIFF_COMPRESSION);
        this.compression = compField == null ? COMP_NONE : compField
                .getAsInt(0);

        // Get the photometric interpretation.
        int photometricType;
        final TIFFField photometricTypeField = dir
                .getField(TIFFImageDecoder.TIFF_PHOTOMETRIC_INTERPRETATION);
        if (photometricTypeField == null) {
            photometricType = 0; // White is zero
        } else {
            photometricType = photometricTypeField.getAsInt(0);
        }

        // Determine which kind of image we are dealing with.
        this.imageType = TYPE_UNSUPPORTED;
        switch (photometricType) {
        case 0: // WhiteIsZero
            this.isWhiteZero = true;
        case 1: // BlackIsZero
            if (this.sampleSize == 1 && samplesPerPixel == 1) {
                this.imageType = TYPE_BILEVEL;
            } else if (this.sampleSize == 4 && samplesPerPixel == 1) {
                this.imageType = TYPE_GRAY_4BIT;
            } else if (this.sampleSize % 8 == 0) {
                if (samplesPerPixel == 1) {
                    this.imageType = TYPE_GRAY;
                } else if (samplesPerPixel == 2) {
                    this.imageType = TYPE_GRAY_ALPHA;
                } else {
                    this.imageType = TYPE_GENERIC;
                }
            }
            break;
        case 2: // RGB
            if (this.sampleSize % 8 == 0) {
                if (samplesPerPixel == 3) {
                    this.imageType = TYPE_RGB;
                } else if (samplesPerPixel == 4) {
                    this.imageType = TYPE_RGB_ALPHA;
                } else {
                    this.imageType = TYPE_GENERIC;
                }
            }
            break;
        case 3: // RGB Palette
            if (samplesPerPixel == 1
                    && (this.sampleSize == 4 || this.sampleSize == 8 || this.sampleSize == 16)) {
                this.imageType = TYPE_PALETTE;
            }
            break;
        case 4: // Transparency mask
            if (this.sampleSize == 1 && samplesPerPixel == 1) {
                this.imageType = TYPE_BILEVEL;
            }
            break;
        default: // Other including CMYK, CIE L*a*b*, unknown.
            if (this.sampleSize % 8 == 0) {
                this.imageType = TYPE_GENERIC;
            }
        }

        // Bail out if not one of the supported types.
        if (this.imageType == TYPE_UNSUPPORTED) {
            throw new RuntimeException(PropertyUtil.getString("TIFFImage4")
                    + ": " + this.imageType);
        }

        // Set basic image layout
        final Rectangle bounds = new Rectangle(0, 0,
                (int) dir.getFieldAsLong(TIFFImageDecoder.TIFF_IMAGE_WIDTH),
                (int) dir.getFieldAsLong(TIFFImageDecoder.TIFF_IMAGE_LENGTH));

        // Set a preliminary band count. This may be changed later as needed.
        this.numBands = samplesPerPixel;

        // Figure out if any extra samples are present.
        final TIFFField efield = dir
                .getField(TIFFImageDecoder.TIFF_EXTRA_SAMPLES);
        final int extraSamples = efield == null ? 0 : (int) efield.getAsLong(0);

        int tileWidth, tileHeight;
        if (dir.getField(TIFFImageDecoder.TIFF_TILE_OFFSETS) != null) {
            this.tiled = true;
            // Image is in tiled format
            tileWidth = (int) dir
                    .getFieldAsLong(TIFFImageDecoder.TIFF_TILE_WIDTH);
            tileHeight = (int) dir
                    .getFieldAsLong(TIFFImageDecoder.TIFF_TILE_LENGTH);
            this.tileOffsets = dir.getField(TIFFImageDecoder.TIFF_TILE_OFFSETS)
                    .getAsLongs();
            this.tileByteCounts = getFieldAsLongs(dir
                    .getField(TIFFImageDecoder.TIFF_TILE_BYTE_COUNTS));

        } else {
            this.tiled = false;

            // Image is in stripped format, looks like tiles to us
            // Note: Some legacy files may have tile width and height
            // written but use the strip offsets and byte counts fields
            // instead of the tile offsets and byte counts. Therefore
            // we default here to the tile dimensions if they are written.
            tileWidth = dir.getField(TIFFImageDecoder.TIFF_TILE_WIDTH) != null ? (int) dir
                    .getFieldAsLong(TIFFImageDecoder.TIFF_TILE_WIDTH)
                    : bounds.width;
            final TIFFField field = dir
                    .getField(TIFFImageDecoder.TIFF_ROWS_PER_STRIP);
            if (field == null) {
                // Default is infinity (2^32 -1), basically the entire image

                tileHeight = dir.getField(TIFFImageDecoder.TIFF_TILE_LENGTH) != null ? (int) dir
                        .getFieldAsLong(TIFFImageDecoder.TIFF_TILE_LENGTH)
                        : bounds.height;
            } else {
                final long l = field.getAsLong(0);
                long infinity = 1;
                infinity = (infinity << 32) - 1;
                if (l == infinity) {
                    // 2^32 - 1 (effectively infinity, entire image is 1 strip)
                    tileHeight = bounds.height;
                } else {
                    tileHeight = (int) l;
                }
            }

            final TIFFField tileOffsetsField = dir
                    .getField(TIFFImageDecoder.TIFF_STRIP_OFFSETS);
            if (tileOffsetsField == null) {
                throw new RuntimeException(PropertyUtil.getString("TIFFImage5"));
            } else {
                this.tileOffsets = getFieldAsLongs(tileOffsetsField);
            }

            final TIFFField tileByteCountsField = dir
                    .getField(TIFFImageDecoder.TIFF_STRIP_BYTE_COUNTS);
            if (tileByteCountsField == null) {
                throw new RuntimeException(PropertyUtil.getString("TIFFImage6"));
            } else {
                this.tileByteCounts = getFieldAsLongs(tileByteCountsField);
            }
        }

        // Calculate number of tiles and the tileSize in bytes
        this.tilesX = (bounds.width + tileWidth - 1) / tileWidth;
        this.tilesY = (bounds.height + tileHeight - 1) / tileHeight;
        this.tileSize = tileWidth * tileHeight * this.numBands;

        // Check whether big endian or little endian format is used.
        this.isBigEndian = dir.isBigEndian();

        final TIFFField fillOrderField = dir
                .getField(TIFFImageDecoder.TIFF_FILL_ORDER);
        if (fillOrderField != null) {
            this.fillOrder = fillOrderField.getAsInt(0);
        } else {
            // Default Fill Order
            this.fillOrder = 1;
        }

        switch (this.compression) {
        case COMP_NONE:
        case COMP_PACKBITS:
            // Do nothing.
            break;
        case COMP_DEFLATE:
            this.inflater = new Inflater();
            break;
        case COMP_FAX_G3_1D:
        case COMP_FAX_G3_2D:
        case COMP_FAX_G4_2D:
            if (this.sampleSize != 1) {
                throw new RuntimeException(PropertyUtil.getString("TIFFImage7"));
            }

            // Fax T.4 compression options
            if (this.compression == 3) {
                final TIFFField t4OptionsField = dir
                        .getField(TIFFImageDecoder.TIFF_T4_OPTIONS);
                if (t4OptionsField != null) {
                    this.tiffT4Options = t4OptionsField.getAsLong(0);
                } else {
                    // Use default value
                    this.tiffT4Options = 0;
                }
            }

            // Fax T.6 compression options
            if (this.compression == 4) {
                final TIFFField t6OptionsField = dir
                        .getField(TIFFImageDecoder.TIFF_T6_OPTIONS);
                if (t6OptionsField != null) {
                    this.tiffT6Options = t6OptionsField.getAsLong(0);
                } else {
                    // Use default value
                    this.tiffT6Options = 0;
                }
            }

            // Fax encoding, need to create the Fax decoder.
            this.decoder = new TIFFFaxDecoder(this.fillOrder, tileWidth,
                    tileHeight);
            break;

        case COMP_LZW:
            // LZW compression used, need to create the LZW decoder.
            final TIFFField predictorField = dir
                    .getField(TIFFImageDecoder.TIFF_PREDICTOR);

            if (predictorField == null) {
                this.predictor = 1;
            } else {
                this.predictor = predictorField.getAsInt(0);

                if (this.predictor != 1 && this.predictor != 2) {
                    throw new RuntimeException(
                            PropertyUtil.getString("TIFFImage8"));
                }

                if (this.predictor == 2 && this.sampleSize != 8) {
                    throw new RuntimeException(
                            PropertyUtil.getString("TIFFImage9"));
                }
            }

            this.lzwDecoder = new TIFFLZWDecoder(tileWidth, this.predictor,
                    samplesPerPixel);
            break;

        case COMP_JPEG_OLD:
            throw new RuntimeException(PropertyUtil.getString("TIFFImage15"));

        default:
            throw new RuntimeException(PropertyUtil.getString("TIFFImage10")
                    + ": " + this.compression);
        }

        ColorModel colorModel = null;
        SampleModel sampleModel = null;
        switch (this.imageType) {
        case TYPE_BILEVEL:
        case TYPE_GRAY_4BIT:
            sampleModel = new MultiPixelPackedSampleModel(this.dataType,
                    tileWidth, tileHeight, this.sampleSize);
            if (this.imageType == TYPE_BILEVEL) {
                final byte[] map = new byte[] {
                        (byte) (this.isWhiteZero ? 255 : 0),
                        (byte) (this.isWhiteZero ? 0 : 255) };
                colorModel = new IndexColorModel(1, 2, map, map, map);
            } else {
                final byte[] map = new byte[16];
                if (this.isWhiteZero) {
                    for (int i = 0; i < map.length; i++) {
                        map[i] = (byte) (255 - 16 * i);
                    }
                } else {
                    for (int i = 0; i < map.length; i++) {
                        map[i] = (byte) (16 * i);
                    }
                }
                colorModel = new IndexColorModel(4, 16, map, map, map);
            }
            break;

        case TYPE_GRAY:
        case TYPE_GRAY_ALPHA:
        case TYPE_RGB:
        case TYPE_RGB_ALPHA:
            // Create a pixel interleaved SampleModel with decreasing
            // band offsets.
            final int[] reverseOffsets = new int[this.numBands];
            for (int i = 0; i < this.numBands; i++) {
                reverseOffsets[i] = this.numBands - 1 - i;
            }
            sampleModel = new PixelInterleavedSampleModel(this.dataType,
                    tileWidth, tileHeight, this.numBands, this.numBands
                            * tileWidth, reverseOffsets);

            if (this.imageType == TYPE_GRAY) {
                colorModel = new ComponentColorModel(
                        ColorSpace.getInstance(ColorSpace.CS_GRAY),
                        new int[] { this.sampleSize }, false, false,
                        Transparency.OPAQUE, this.dataType);
            } else if (this.imageType == TYPE_RGB) {
                colorModel = new ComponentColorModel(
                        ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {
                                this.sampleSize, this.sampleSize,
                                this.sampleSize }, false, false,
                        Transparency.OPAQUE, this.dataType);
            } else { // hasAlpha
                // Transparency.OPAQUE signifies image data that is
                // completely opaque, meaning that all pixels have an alpha
                // value of 1.0. So the extra band gets ignored, which is
                // what we want.
                int transparency = Transparency.OPAQUE;
                if (extraSamples == 1) { // associated (premultiplied) alpha
                    transparency = Transparency.TRANSLUCENT;
                } else if (extraSamples == 2) { // unassociated alpha
                    transparency = Transparency.BITMASK;
                }

                colorModel = createAlphaComponentColorModel(this.dataType,
                        this.numBands, extraSamples == 1, transparency);
            }
            break;

        case TYPE_GENERIC:
        case TYPE_YCBCR_SUB:
            // For this case we can't display the image, so we create a
            // SampleModel with increasing bandOffsets, and keep the
            // ColorModel as null, as there is no appropriate ColorModel.

            final int[] bandOffsets = new int[this.numBands];
            for (int i = 0; i < this.numBands; i++) {
                bandOffsets[i] = i;
            }

            sampleModel = new PixelInterleavedSampleModel(this.dataType,
                    tileWidth, tileHeight, this.numBands, this.numBands
                            * tileWidth, bandOffsets);
            colorModel = null;
            break;

        case TYPE_PALETTE:
            // Get the colormap
            final TIFFField cfield = dir
                    .getField(TIFFImageDecoder.TIFF_COLORMAP);
            if (cfield == null) {
                throw new RuntimeException(
                        PropertyUtil.getString("TIFFImage11"));
            } else {
                this.colormap = cfield.getAsChars();
            }

            // Could be either 1 or 3 bands depending on whether we use
            // IndexColorModel or not.
            if (this.decodePaletteAsShorts) {
                this.numBands = 3;

                // If no SampleFormat tag was specified and if the
                // sampleSize is less than or equal to 8, then the
                // dataType was initially set to byte, but now we want to
                // expand the palette as shorts, so the dataType should
                // be ushort.
                if (this.dataType == DataBuffer.TYPE_BYTE) {
                    this.dataType = DataBuffer.TYPE_USHORT;
                }

                // Data will have to be unpacked into a 3 band short image
                // as we do not have a IndexColorModel that can deal with
                // a colormodel whose entries are of short data type.
                sampleModel = createPixelInterleavedSampleModel(this.dataType,
                        tileWidth, tileHeight, this.numBands);

                colorModel = new ComponentColorModel(
                        ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {
                                16, 16, 16 }, false, false,
                        Transparency.OPAQUE, this.dataType);

            } else {

                this.numBands = 1;

                if (this.sampleSize == 4) {
                    // Pixel data will not be unpacked, will use
                    // MPPSM to store packed data and
                    // IndexColorModel to do the unpacking.
                    sampleModel = new MultiPixelPackedSampleModel(
                            DataBuffer.TYPE_BYTE, tileWidth, tileHeight,
                            this.sampleSize);
                } else if (this.sampleSize == 8) {

                    sampleModel = createPixelInterleavedSampleModel(
                            DataBuffer.TYPE_BYTE, tileWidth, tileHeight,
                            this.numBands);
                } else if (this.sampleSize == 16) {

                    // Here datatype has to be unsigned since we
                    // are storing indices into the
                    // IndexColorModel palette. Ofcourse the
                    // actual palette entries are allowed to be
                    // negative.
                    this.dataType = DataBuffer.TYPE_USHORT;
                    sampleModel = createPixelInterleavedSampleModel(
                            DataBuffer.TYPE_USHORT, tileWidth, tileHeight,
                            this.numBands);
                }

                final int bandLength = this.colormap.length / 3;
                final byte[] r = new byte[bandLength];
                final byte[] g = new byte[bandLength];
                final byte[] b = new byte[bandLength];

                final int gIndex = bandLength;
                final int bIndex = bandLength * 2;

                if (this.dataType == DataBuffer.TYPE_SHORT) {

                    for (int i = 0; i < bandLength; i++) {
                        r[i] = param
                                .decodeSigned16BitsTo8Bits((short) this.colormap[i]);
                        g[i] = param
                                .decodeSigned16BitsTo8Bits((short) this.colormap[gIndex
                                        + i]);
                        b[i] = param
                                .decodeSigned16BitsTo8Bits((short) this.colormap[bIndex
                                        + i]);
                    }

                } else {

                    for (int i = 0; i < bandLength; i++) {
                        r[i] = param
                                .decode16BitsTo8Bits(this.colormap[i] & 0xffff);
                        g[i] = param.decode16BitsTo8Bits(this.colormap[gIndex
                                + i] & 0xffff);
                        b[i] = param.decode16BitsTo8Bits(this.colormap[bIndex
                                + i] & 0xffff);
                    }

                }

                colorModel = new IndexColorModel(this.sampleSize, bandLength,
                        r, g, b);
            }
            break;

        default:
            throw new RuntimeException(PropertyUtil.getString("TIFFImage4")
                    + ": " + this.imageType);
        }

                        final Map properties = new HashMap();
                        // Set a property "tiff_directory".
                        properties.put("tiff_directory", dir);

                        // log.info("Constructed TIFF");

                        init((CachableRed) null, bounds, colorModel, sampleModel, 0, 0,
                properties);
    }

    /**
     * Reads a private IFD from a given offset in the stream. This method may be
     * used to obtain IFDs that are referenced only by private tag values.
     */
    public TIFFDirectory getPrivateIFD(final long offset) throws IOException {
        return new TIFFDirectory(this.stream, offset, 0);
    }

    @Override
    public WritableRaster copyData(final WritableRaster wr) {
        copyToRaster(wr);
        return wr;
    }

    /**
     * Returns tile (tileX, tileY) as a Raster.
     */
    @Override
    public synchronized Raster getTile(final int tileX, final int tileY) {
        if (tileX < 0 || tileX >= this.tilesX || tileY < 0
                || tileY >= this.tilesY) {
            throw new IllegalArgumentException(
                    PropertyUtil.getString("TIFFImage12"));
        }

        // log.info("Called TIFF getTile:" + tileX + "," + tileY);

        // Get the data array out of the DataBuffer
        byte[] bdata = null;
        short[] sdata = null;
        int[] idata = null;

        final SampleModel sampleModel = getSampleModel();
        final WritableRaster tile = makeTile(tileX, tileY);

        final DataBuffer buffer = tile.getDataBuffer();

        final int dataType = sampleModel.getDataType();
        if (dataType == DataBuffer.TYPE_BYTE) {
            bdata = ((DataBufferByte) buffer).getData();
        } else if (dataType == DataBuffer.TYPE_USHORT) {
            sdata = ((DataBufferUShort) buffer).getData();
        } else if (dataType == DataBuffer.TYPE_SHORT) {
            sdata = ((DataBufferShort) buffer).getData();
        } else if (dataType == DataBuffer.TYPE_INT) {
            idata = ((DataBufferInt) buffer).getData();
        }

        // Variables used for swapping when converting from RGB to BGR
        byte bswap;
        short sswap;
        int iswap;

        // Save original file pointer position and seek to tile data location.
        long save_offset = 0;
        try {
            save_offset = this.stream.getFilePointer();
            this.stream.seek(this.tileOffsets[tileY * this.tilesX + tileX]);
        } catch (final IOException ioe) {
            throw new RuntimeException(PropertyUtil.getString("TIFFImage13")
                    + ": " + ioe.getMessage());
        }

        // Number of bytes in this tile (strip) after compression.
        final int byteCount = (int) this.tileByteCounts[tileY * this.tilesX
                + tileX];

        // Find out the number of bytes in the current tile
        Rectangle newRect;
        if (!this.tiled) {
            newRect = tile.getBounds();
        } else {
            newRect = new Rectangle(tile.getMinX(), tile.getMinY(),
                    this.tileWidth, this.tileHeight);
        }

        final int unitsInThisTile = newRect.width * newRect.height
                * this.numBands;

        // Allocate read buffer if needed.
        byte[] data = this.compression != COMP_NONE
                || this.imageType == TYPE_PALETTE ? new byte[byteCount] : null;

                // Read the data, uncompressing as needed. There are four cases:
                // bilevel, palette-RGB, 4-bit grayscale, and everything else.
                if (this.imageType == TYPE_BILEVEL) { // bilevel
                    try {
                        if (this.compression == COMP_PACKBITS) {
                            this.stream.readFully(data, 0, byteCount);

                            // Since the decompressed data will still be packed
                            // 8 pixels into 1 byte, calculate bytesInThisTile
                            int bytesInThisTile;
                            if (newRect.width % 8 == 0) {
                                bytesInThisTile = newRect.width / 8 * newRect.height;
                            } else {
                                bytesInThisTile = (newRect.width / 8 + 1)
                                * newRect.height;
                            }
                            decodePackbits(data, bytesInThisTile, bdata);
                        } else if (this.compression == COMP_LZW) {
                            this.stream.readFully(data, 0, byteCount);
                            this.lzwDecoder.decode(data, bdata, newRect.height);
                        } else if (this.compression == COMP_FAX_G3_1D) {
                            this.stream.readFully(data, 0, byteCount);
                            this.decoder.decode1D(bdata, data, 0, newRect.height);
                        } else if (this.compression == COMP_FAX_G3_2D) {
                            this.stream.readFully(data, 0, byteCount);
                            this.decoder.decode2D(bdata, data, 0, newRect.height,
                            this.tiffT4Options);
                        } else if (this.compression == COMP_FAX_G4_2D) {
                            this.stream.readFully(data, 0, byteCount);
                            this.decoder.decodeT6(bdata, data, 0, newRect.height,
                            this.tiffT6Options);
                        } else if (this.compression == COMP_DEFLATE) {
                            this.stream.readFully(data, 0, byteCount);
                            inflate(data, bdata);
                        } else if (this.compression == COMP_NONE) {
                            this.stream.readFully(bdata, 0, byteCount);
                        }

                        this.stream.seek(save_offset);
                    } catch (final IOException ioe) {
                        throw new RuntimeException(
                        PropertyUtil.getString("TIFFImage13") + ": "
                                + ioe.getMessage());
                    }
                } else if (this.imageType == TYPE_PALETTE) { // palette-RGB
                    if (this.sampleSize == 16) {

                        if (this.decodePaletteAsShorts) {

                            short[] tempData = null;

                            // At this point the data is 1 banded and will
                            // become 3 banded only after we've done the palette
                            // lookup, since unitsInThisTile was calculated with
                            // 3 bands, we need to divide this by 3.
                            final int unitsBeforeLookup = unitsInThisTile / 3;

                            // Since unitsBeforeLookup is the number of shorts,
                            // but we do our decompression in terms of bytes, we
                            // need to multiply it by 2 in order to figure out
                            // how many bytes we'll get after decompression.
                            final int entries = unitsBeforeLookup * 2;

                            // Read the data, if compressed, decode it, reset the
                    // pointer
                            try {

                                if (this.compression == COMP_PACKBITS) {

                                    this.stream.readFully(data, 0, byteCount);

                                    final byte[] byteArray = new byte[entries];
                                    decodePackbits(data, entries, byteArray);
                                    tempData = new short[unitsBeforeLookup];
                                    interpretBytesAsShorts(byteArray, tempData,
                                    unitsBeforeLookup);

                                } else if (this.compression == COMP_LZW) {

                                    // Read in all the compressed data for this tile
                                    this.stream.readFully(data, 0, byteCount);

                                    final byte[] byteArray = new byte[entries];
                                    this.lzwDecoder.decode(data, byteArray,
                                    newRect.height);
                                    tempData = new short[unitsBeforeLookup];
                                    interpretBytesAsShorts(byteArray, tempData,
                                    unitsBeforeLookup);

                                } else if (this.compression == COMP_DEFLATE) {

                                    this.stream.readFully(data, 0, byteCount);
                                    final byte[] byteArray = new byte[entries];
                                    inflate(data, byteArray);
                                    tempData = new short[unitsBeforeLookup];
                                    interpretBytesAsShorts(byteArray, tempData,
                                    unitsBeforeLookup);

                                } else if (this.compression == COMP_NONE) {

                                    // byteCount tells us how many bytes are there
                                    // in this tile, but we need to read in shorts,
                                    // which will take half the space, so while
                                    // allocating we divide byteCount by 2.
                                    tempData = new short[byteCount / 2];
                                    readShorts(byteCount / 2, tempData);
                                }

                                this.stream.seek(save_offset);

                            } catch (final IOException ioe) {
                                throw new RuntimeException(
                                PropertyUtil.getString("TIFFImage13") + ": "
                                        + ioe.getMessage());
                            }

                            if (dataType == DataBuffer.TYPE_USHORT) {

                                // Expand the palette image into an rgb image with
                        // ushort
                                // data type.
                                int cmapValue;
                                int count = 0, lookup;
                                final int len = this.colormap.length / 3;
                                final int len2 = len * 2;
                                for (int i = 0; i < unitsBeforeLookup; i++) {
                                    // Get the index into the colormap
                                    lookup = tempData[i] & 0xffff;
                                    // Get the blue value
                                    cmapValue = this.colormap[lookup + len2];
                                    sdata[count++] = (short) (cmapValue & 0xffff);
                                    // Get the green value
                                    cmapValue = this.colormap[lookup + len];
                                    sdata[count++] = (short) (cmapValue & 0xffff);
                                    // Get the red value
                                    cmapValue = this.colormap[lookup];
                                    sdata[count++] = (short) (cmapValue & 0xffff);
                                }

                            } else if (dataType == DataBuffer.TYPE_SHORT) {

                                // Expand the palette image into an rgb image with
                                // short data type.
                                int cmapValue;
                                int count = 0, lookup;
                                final int len = this.colormap.length / 3;
                                final int len2 = len * 2;
                                for (int i = 0; i < unitsBeforeLookup; i++) {
                                    // Get the index into the colormap
                                    lookup = tempData[i] & 0xffff;
                                    // Get the blue value
                                    cmapValue = this.colormap[lookup + len2];
                                    sdata[count++] = (short) cmapValue;
                                    // Get the green value
                                    cmapValue = this.colormap[lookup + len];
                                    sdata[count++] = (short) cmapValue;
                                    // Get the red value
                                    cmapValue = this.colormap[lookup];
                                    sdata[count++] = (short) cmapValue;
                                }
                            }

                        } else {

                            // No lookup being done here, when RGB values are needed,
                            // the associated IndexColorModel can be used to get them.

                            try {

                                if (this.compression == COMP_PACKBITS) {

                                    this.stream.readFully(data, 0, byteCount);

                                    // Since unitsInThisTile is the number of shorts,
                                    // but we do our decompression in terms of bytes, we
                                    // need to multiply unitsInThisTile by 2 in order to
                                    // figure out how many bytes we'll get after
                                    // decompression.
                                    final int bytesInThisTile = unitsInThisTile * 2;

                                    final byte[] byteArray = new byte[bytesInThisTile];
                                    decodePackbits(data, bytesInThisTile, byteArray);
                                    interpretBytesAsShorts(byteArray, sdata,
                                    unitsInThisTile);

                                } else if (this.compression == COMP_LZW) {

                                    this.stream.readFully(data, 0, byteCount);

                                    // Since unitsInThisTile is the number of shorts,
                                    // but we do our decompression in terms of bytes, we
                                    // need to multiply unitsInThisTile by 2 in order to
                                    // figure out how many bytes we'll get after
                                    // decompression.
                                    final byte[] byteArray = new byte[unitsInThisTile * 2];
                                    this.lzwDecoder.decode(data, byteArray,
                                    newRect.height);
                                    interpretBytesAsShorts(byteArray, sdata,
                                    unitsInThisTile);

                                } else if (this.compression == COMP_DEFLATE) {

                                    this.stream.readFully(data, 0, byteCount);
                                    final byte[] byteArray = new byte[unitsInThisTile * 2];
                                    inflate(data, byteArray);
                                    interpretBytesAsShorts(byteArray, sdata,
                                    unitsInThisTile);

                                } else if (this.compression == COMP_NONE) {

                                    readShorts(byteCount / 2, sdata);
                                }

                                this.stream.seek(save_offset);

                            } catch (final IOException ioe) {
                                throw new RuntimeException(
                                PropertyUtil.getString("TIFFImage13") + ": "
                                        + ioe.getMessage());
                            }
                        }

                    } else if (this.sampleSize == 8) {

                        if (this.decodePaletteAsShorts) {

                            byte[] tempData = null;

                            // At this point the data is 1 banded and will
                            // become 3 banded only after we've done the palette
                            // lookup, since unitsInThisTile was calculated with
                            // 3 bands, we need to divide this by 3.
                            final int unitsBeforeLookup = unitsInThisTile / 3;

                            // Read the data, if compressed, decode it, reset the
                    // pointer
                            try {

                                if (this.compression == COMP_PACKBITS) {

                                    this.stream.readFully(data, 0, byteCount);
                                    tempData = new byte[unitsBeforeLookup];
                                    decodePackbits(data, unitsBeforeLookup, tempData);

                                } else if (this.compression == COMP_LZW) {

                                    this.stream.readFully(data, 0, byteCount);
                                    tempData = new byte[unitsBeforeLookup];
                                    this.lzwDecoder.decode(data, tempData,
                                    newRect.height);

                                } else if (this.compression == COMP_DEFLATE) {

                                    this.stream.readFully(data, 0, byteCount);
                                    tempData = new byte[unitsBeforeLookup];
                                    inflate(data, tempData);

                                } else if (this.compression == COMP_NONE) {

                                    tempData = new byte[byteCount];
                                    this.stream.readFully(tempData, 0, byteCount);
                                } else {
                                    throw new RuntimeException(
                                    PropertyUtil.getString("IFFImage10") + ": "
                                            + this.compression);
                                }

                                this.stream.seek(save_offset);

                            } catch (final IOException ioe) {
                                throw new RuntimeException(
                                PropertyUtil.getString("TIFFImage13") + ": "
                                        + ioe.getMessage());
                            }

                            // Expand the palette image into an rgb image with ushort
                            // data type.
                            int cmapValue;
                            int count = 0, lookup;
                            final int len = this.colormap.length / 3;
                            final int len2 = len * 2;
                            for (int i = 0; i < unitsBeforeLookup; i++) {
                                // Get the index into the colormap
                                lookup = tempData[i] & 0xff;
                                // Get the blue value
                                cmapValue = this.colormap[lookup + len2];
                                sdata[count++] = (short) (cmapValue & 0xffff);
                                // Get the green value
                                cmapValue = this.colormap[lookup + len];
                                sdata[count++] = (short) (cmapValue & 0xffff);
                                // Get the red value
                                cmapValue = this.colormap[lookup];
                                sdata[count++] = (short) (cmapValue & 0xffff);
                            }
                        } else {

                            // No lookup being done here, when RGB values are needed,
                            // the associated IndexColorModel can be used to get them.

                            try {

                                if (this.compression == COMP_PACKBITS) {

                                    this.stream.readFully(data, 0, byteCount);
                                    decodePackbits(data, unitsInThisTile, bdata);

                                } else if (this.compression == COMP_LZW) {

                                    this.stream.readFully(data, 0, byteCount);
                                    this.lzwDecoder.decode(data, bdata, newRect.height);

                                } else if (this.compression == COMP_DEFLATE) {

                                    this.stream.readFully(data, 0, byteCount);
                                    inflate(data, bdata);

                                } else if (this.compression == COMP_NONE) {

                                    this.stream.readFully(bdata, 0, byteCount);

                                } else {
                                    throw new RuntimeException(
                                    PropertyUtil.getString("TIFFImage10")
                                            + ": " + this.compression);
                                }

                                this.stream.seek(save_offset);

                            } catch (final IOException ioe) {
                                throw new RuntimeException(
                                PropertyUtil.getString("TIFFImage13") + ": "
                                        + ioe.getMessage());
                            }
                        }

                    } else if (this.sampleSize == 4) {

                        final int padding = newRect.width % 2 == 0 ? 0 : 1;
                        final int bytesPostDecoding = (newRect.width / 2 + padding)
                        * newRect.height;

                        // Output short images
                        if (this.decodePaletteAsShorts) {

                            byte[] tempData = null;

                            try {
                                this.stream.readFully(data, 0, byteCount);
                                this.stream.seek(save_offset);
                            } catch (final IOException ioe) {
                                throw new RuntimeException(
                                PropertyUtil.getString("TIFFImage13") + ": "
                                        + ioe.getMessage());
                            }

                            // If compressed, decode the data.
                            if (this.compression == COMP_PACKBITS) {

                                tempData = new byte[bytesPostDecoding];
                                decodePackbits(data, bytesPostDecoding, tempData);

                            } else if (this.compression == COMP_LZW) {

                                tempData = new byte[bytesPostDecoding];
                                this.lzwDecoder.decode(data, tempData, newRect.height);

                            } else if (this.compression == COMP_DEFLATE) {

                                tempData = new byte[bytesPostDecoding];
                                inflate(data, tempData);

                            } else if (this.compression == COMP_NONE) {

                                tempData = data;
                            }

                            final int bytes = unitsInThisTile / 3;

                            // Unpack the 2 pixels packed into each byte.
                            data = new byte[bytes];

                            int srcCount = 0, dstCount = 0;
                            for (int j = 0; j < newRect.height; j++) {
                                for (int i = 0; i < newRect.width / 2; i++) {
                                    data[dstCount++] = (byte) ((tempData[srcCount] & 0xf0) >> 4);
                                    data[dstCount++] = (byte) (tempData[srcCount++] & 0x0f);
                                }

                                if (padding == 1) {
                                    data[dstCount++] = (byte) ((tempData[srcCount++] & 0xf0) >> 4);
                                }
                            }

                            final int len = this.colormap.length / 3;
                            final int len2 = len * 2;
                            int cmapValue, lookup;
                            int count = 0;
                            for (int i = 0; i < bytes; i++) {
                                lookup = data[i] & 0xff;
                                cmapValue = this.colormap[lookup + len2];
                                sdata[count++] = (short) (cmapValue & 0xffff);
                                cmapValue = this.colormap[lookup + len];
                                sdata[count++] = (short) (cmapValue & 0xffff);
                                cmapValue = this.colormap[lookup];
                                sdata[count++] = (short) (cmapValue & 0xffff);
                            }
                        } else {

                            // Output byte values, use IndexColorModel for unpacking
                            try {

                                // If compressed, decode the data.
                                if (this.compression == COMP_PACKBITS) {

                                    this.stream.readFully(data, 0, byteCount);
                                    decodePackbits(data, bytesPostDecoding, bdata);

                                } else if (this.compression == COMP_LZW) {

                                    this.stream.readFully(data, 0, byteCount);
                                    this.lzwDecoder.decode(data, bdata, newRect.height);

                                } else if (this.compression == COMP_DEFLATE) {

                                    this.stream.readFully(data, 0, byteCount);
                                    inflate(data, bdata);

                                } else if (this.compression == COMP_NONE) {

                                    this.stream.readFully(bdata, 0, byteCount);
                                }

                                this.stream.seek(save_offset);

                            } catch (final IOException ioe) {
                                throw new RuntimeException(
                                PropertyUtil.getString("TIFFImage13") + ": "
                                        + ioe.getMessage());
                            }
                        }
                    }
                } else if (this.imageType == TYPE_GRAY_4BIT) { // 4-bit gray
                    try {
                        if (this.compression == COMP_PACKBITS) {

                            this.stream.readFully(data, 0, byteCount);

                            // Since the decompressed data will still be packed
                            // 2 pixels into 1 byte, calculate bytesInThisTile
                            int bytesInThisTile;
                            if (newRect.width % 8 == 0) {
                                bytesInThisTile = newRect.width / 2 * newRect.height;
                            } else {
                                bytesInThisTile = (newRect.width / 2 + 1)
                                * newRect.height;
                            }

                            decodePackbits(data, bytesInThisTile, bdata);

                        } else if (this.compression == COMP_LZW) {

                            this.stream.readFully(data, 0, byteCount);
                            this.lzwDecoder.decode(data, bdata, newRect.height);

                        } else if (this.compression == COMP_DEFLATE) {

                            this.stream.readFully(data, 0, byteCount);
                            inflate(data, bdata);

                        } else {

                            this.stream.readFully(bdata, 0, byteCount);
                        }

                        this.stream.seek(save_offset);
                    } catch (final IOException ioe) {
                        throw new RuntimeException(
                        PropertyUtil.getString("TIFFImage13") + ": "
                                + ioe.getMessage());
                    }
                } else { // everything else
                    try {

                        if (this.sampleSize == 8) {

                            if (this.compression == COMP_NONE) {
                                this.stream.readFully(bdata, 0, byteCount);

                            } else if (this.compression == COMP_LZW) {

                                this.stream.readFully(data, 0, byteCount);
                                this.lzwDecoder.decode(data, bdata, newRect.height);

                            } else if (this.compression == COMP_PACKBITS) {

                                this.stream.readFully(data, 0, byteCount);
                                decodePackbits(data, unitsInThisTile, bdata);

                            } else if (this.compression == COMP_DEFLATE) {

                                this.stream.readFully(data, 0, byteCount);
                                inflate(data, bdata);

                            } else {
                                throw new RuntimeException(
                                PropertyUtil.getString("TIFFImage10") + ": "
                                        + this.compression);
                            }

                        } else if (this.sampleSize == 16) {

                            if (this.compression == COMP_NONE) {

                                readShorts(byteCount / 2, sdata);

                            } else if (this.compression == COMP_LZW) {

                                this.stream.readFully(data, 0, byteCount);

                                // Since unitsInThisTile is the number of shorts,
                                // but we do our decompression in terms of bytes, we
                                // need to multiply unitsInThisTile by 2 in order to
                                // figure out how many bytes we'll get after
                                // decompression.
                                final byte[] byteArray = new byte[unitsInThisTile * 2];
                                this.lzwDecoder.decode(data, byteArray, newRect.height);
                                interpretBytesAsShorts(byteArray, sdata,
                                unitsInThisTile);

                            } else if (this.compression == COMP_PACKBITS) {

                                this.stream.readFully(data, 0, byteCount);

                                // Since unitsInThisTile is the number of shorts,
                                // but we do our decompression in terms of bytes, we
                                // need to multiply unitsInThisTile by 2 in order to
                                // figure out how many bytes we'll get after
                                // decompression.
                                final int bytesInThisTile = unitsInThisTile * 2;

                                final byte[] byteArray = new byte[bytesInThisTile];
                                decodePackbits(data, bytesInThisTile, byteArray);
                                interpretBytesAsShorts(byteArray, sdata,
                                unitsInThisTile);
                            } else if (this.compression == COMP_DEFLATE) {

                                this.stream.readFully(data, 0, byteCount);
                                final byte[] byteArray = new byte[unitsInThisTile * 2];
                                inflate(data, byteArray);
                                interpretBytesAsShorts(byteArray, sdata,
                                unitsInThisTile);

                            }
                        } else if (this.sampleSize == 32
                        && dataType == DataBuffer.TYPE_INT) { // redundant
                            if (this.compression == COMP_NONE) {

                                readInts(byteCount / 4, idata);

                            } else if (this.compression == COMP_LZW) {

                                this.stream.readFully(data, 0, byteCount);

                                // Since unitsInThisTile is the number of ints,
                                // but we do our decompression in terms of bytes, we
                                // need to multiply unitsInThisTile by 4 in order to
                                // figure out how many bytes we'll get after
                                // decompression.
                                final byte[] byteArray = new byte[unitsInThisTile * 4];
                                this.lzwDecoder.decode(data, byteArray, newRect.height);
                                interpretBytesAsInts(byteArray, idata, unitsInThisTile);

                            } else if (this.compression == COMP_PACKBITS) {

                                this.stream.readFully(data, 0, byteCount);

                                // Since unitsInThisTile is the number of ints,
                                // but we do our decompression in terms of bytes, we
                                // need to multiply unitsInThisTile by 4 in order to
                                // figure out how many bytes we'll get after
                                // decompression.
                                final int bytesInThisTile = unitsInThisTile * 4;

                                final byte[] byteArray = new byte[bytesInThisTile];
                                decodePackbits(data, bytesInThisTile, byteArray);
                                interpretBytesAsInts(byteArray, idata, unitsInThisTile);
                            } else if (this.compression == COMP_DEFLATE) {

                                this.stream.readFully(data, 0, byteCount);
                                final byte[] byteArray = new byte[unitsInThisTile * 4];
                                inflate(data, byteArray);
                                interpretBytesAsInts(byteArray, idata, unitsInThisTile);

                            }
                        }

                        this.stream.seek(save_offset);

                    } catch (final IOException ioe) {
                        throw new RuntimeException(
                        PropertyUtil.getString("TIFFImage13") + ": "
                                + ioe.getMessage());
                    }

                    // Modify the data for certain special cases.
                    switch (this.imageType) {
                    case TYPE_GRAY:
                    case TYPE_GRAY_ALPHA:
                        if (this.isWhiteZero) {
                            // Since we are using a ComponentColorModel with this
                            // image, we need to change the WhiteIsZero data to
                            // BlackIsZero data so it will display properly.
                            if (dataType == DataBuffer.TYPE_BYTE
                            && !(getColorModel() instanceof IndexColorModel)) {

                                for (int l = 0; l < bdata.length; l += this.numBands) {
                                    bdata[l] = (byte) (255 - bdata[l]);
                                }
                            } else if (dataType == DataBuffer.TYPE_USHORT) {

                                final int ushortMax = Short.MAX_VALUE - Short.MIN_VALUE;
                                for (int l = 0; l < sdata.length; l += this.numBands) {
                                    sdata[l] = (short) (ushortMax - sdata[l]);
                                }

                            } else if (dataType == DataBuffer.TYPE_SHORT) {

                                for (int l = 0; l < sdata.length; l += this.numBands) {
                                    sdata[l] = (short) ~sdata[l];
                                }
                            } else if (dataType == DataBuffer.TYPE_INT) {

                                final long uintMax = (long) Integer.MAX_VALUE
                                - (long) Integer.MIN_VALUE;
                                for (int l = 0; l < idata.length; l += this.numBands) {
                                    idata[l] = (int) (uintMax - idata[l]);
                                }
                            }
                        }
                        break;
                    case TYPE_RGB:
                        // Change RGB to BGR order, as Java2D displays that faster.
                        // Unnecessary for JPEG-in-TIFF as the decoder handles it.
                        if (this.sampleSize == 8 && this.compression != COMP_JPEG_TTN2) {
                            for (int i = 0; i < unitsInThisTile; i += 3) {
                                bswap = bdata[i];
                                bdata[i] = bdata[i + 2];
                                bdata[i + 2] = bswap;
                            }
                        } else if (this.sampleSize == 16) {
                            for (int i = 0; i < unitsInThisTile; i += 3) {
                                sswap = sdata[i];
                                sdata[i] = sdata[i + 2];
                                sdata[i + 2] = sswap;
                            }
                        } else if (this.sampleSize == 32) {
                            if (dataType == DataBuffer.TYPE_INT) {
                                for (int i = 0; i < unitsInThisTile; i += 3) {
                                    iswap = idata[i];
                                    idata[i] = idata[i + 2];
                                    idata[i + 2] = iswap;
                                }
                            }
                        }
                        break;
                    case TYPE_RGB_ALPHA:
                        // Convert from RGBA to ABGR for Java2D
                        if (this.sampleSize == 8) {
                            for (int i = 0; i < unitsInThisTile; i += 4) {
                                // Swap R and A
                                bswap = bdata[i];
                                bdata[i] = bdata[i + 3];
                                bdata[i + 3] = bswap;

                                // Swap G and B
                                bswap = bdata[i + 1];
                                bdata[i + 1] = bdata[i + 2];
                                bdata[i + 2] = bswap;
                            }
                        } else if (this.sampleSize == 16) {
                            for (int i = 0; i < unitsInThisTile; i += 4) {
                                // Swap R and A
                                sswap = sdata[i];
                                sdata[i] = sdata[i + 3];
                                sdata[i + 3] = sswap;

                                // Swap G and B
                                sswap = sdata[i + 1];
                                sdata[i + 1] = sdata[i + 2];
                                sdata[i + 2] = sswap;
                            }
                        } else if (this.sampleSize == 32) {
                            if (dataType == DataBuffer.TYPE_INT) {
                                for (int i = 0; i < unitsInThisTile; i += 4) {
                                    // Swap R and A
                                    iswap = idata[i];
                                    idata[i] = idata[i + 3];
                                    idata[i + 3] = iswap;

                                    // Swap G and B
                                    iswap = idata[i + 1];
                                    idata[i + 1] = idata[i + 2];
                                    idata[i + 2] = iswap;
                                }
                            }
                        }
                        break;
                    case TYPE_YCBCR_SUB:
                        // Post-processing for YCbCr with subsampled chrominance:
                        // simply replicate the chroma channels for displayability.
                        final int pixelsPerDataUnit = this.chromaSubH * this.chromaSubV;

                        final int numH = newRect.width / this.chromaSubH;
                        final int numV = newRect.height / this.chromaSubV;

                        final byte[] tempData = new byte[numH * numV
                        * (pixelsPerDataUnit + 2)];
                        System.arraycopy(bdata, 0, tempData, 0, tempData.length);

                        final int samplesPerDataUnit = pixelsPerDataUnit * 3;
                        final int[] pixels = new int[samplesPerDataUnit];

                        int bOffset = 0;
                        final int offsetCb = pixelsPerDataUnit;
                        final int offsetCr = offsetCb + 1;

                        int y = newRect.y;
                        for (int j = 0; j < numV; j++) {
                            int x = newRect.x;
                            for (int i = 0; i < numH; i++) {
                                final int Cb = tempData[bOffset + offsetCb];
                                final int Cr = tempData[bOffset + offsetCr];
                                int k = 0;
                                while (k < samplesPerDataUnit) {
                                    pixels[k++] = tempData[bOffset++];
                                    pixels[k++] = Cb;
                                    pixels[k++] = Cr;
                                }
                                bOffset += 2;
                                tile.setPixels(x, y, this.chromaSubH, this.chromaSubV,
                                pixels);
                                x += this.chromaSubH;
                            }
                            y += this.chromaSubV;
                        }

                        break;
                    }
                }

                return tile;
    }

    private void readShorts(final int shortCount, final short[] shortArray) {

        // Since each short consists of 2 bytes, we need a
        // byte array of double size
        final int byteCount = 2 * shortCount;
        final byte[] byteArray = new byte[byteCount];

        try {
            this.stream.readFully(byteArray, 0, byteCount);
        } catch (final IOException ioe) {
            throw new RuntimeException(PropertyUtil.getString("TIFFImage13")
                    + ": " + ioe.getMessage());
        }

        interpretBytesAsShorts(byteArray, shortArray, shortCount);
    }

    private void readInts(final int intCount, final int[] intArray) {

        // Since each int consists of 4 bytes, we need a
        // byte array of quadruple size
        final int byteCount = 4 * intCount;
        final byte[] byteArray = new byte[byteCount];

        try {
            this.stream.readFully(byteArray, 0, byteCount);
        } catch (final IOException ioe) {
            throw new RuntimeException(PropertyUtil.getString("TIFFImage13")
                    + ": " + ioe.getMessage());
        }

        interpretBytesAsInts(byteArray, intArray, intCount);
    }

    // Method to interpret a byte array to a short array, depending on
    // whether the bytes are stored in a big endian or little endian format.
    private void interpretBytesAsShorts(final byte[] byteArray,
            final short[] shortArray, final int shortCount) {

        int j = 0;
        int firstByte, secondByte;

        if (this.isBigEndian) {

            for (int i = 0; i < shortCount; i++) {
                firstByte = byteArray[j++] & 0xff;
                secondByte = byteArray[j++] & 0xff;
                shortArray[i] = (short) ((firstByte << 8) + secondByte);
            }

        } else {

            for (int i = 0; i < shortCount; i++) {
                firstByte = byteArray[j++] & 0xff;
                secondByte = byteArray[j++] & 0xff;
                shortArray[i] = (short) ((secondByte << 8) + firstByte);
            }
        }
    }

    // Method to interpret a byte array to a int array, depending on
    // whether the bytes are stored in a big endian or little endian format.
    private void interpretBytesAsInts(final byte[] byteArray,
            final int[] intArray, final int intCount) {

        int j = 0;

        if (this.isBigEndian) {

            for (int i = 0; i < intCount; i++) {
                intArray[i] = (byteArray[j++] & 0xff) << 24
                        | (byteArray[j++] & 0xff) << 16
                        | (byteArray[j++] & 0xff) << 8 | byteArray[j++] & 0xff;
            }

        } else {

            for (int i = 0; i < intCount; i++) {
                intArray[i] = byteArray[j++] & 0xff
                        | (byteArray[j++] & 0xff) << 8
                        | (byteArray[j++] & 0xff) << 16
                        | (byteArray[j++] & 0xff) << 24;
            }
        }
    }

    // Uncompress packbits compressed image data.
    private byte[] decodePackbits(final byte[] data, final int arraySize,
            byte[] dst) {

        if (dst == null) {
            dst = new byte[arraySize];
        }

        int srcCount = 0, dstCount = 0;
        byte repeat, b;

        try {

            while (dstCount < arraySize) {

                b = data[srcCount++];

                if (b >= 0 && b <= 127) {

                    // literal run packet
                    for (int i = 0; i < b + 1; i++) {
                        dst[dstCount++] = data[srcCount++];
                    }

                } else if (b <= -1 && b >= -127) {

                    // 2 byte encoded run packet
                    repeat = data[srcCount++];
                    for (int i = 0; i < -b + 1; i++) {
                        dst[dstCount++] = repeat;
                    }

                } else {
                    // no-op packet. Do nothing
                    srcCount++;
                }
            }
        } catch (final java.lang.ArrayIndexOutOfBoundsException ae) {
            throw new RuntimeException(PropertyUtil.getString("TIFFImage14")
                    + ": " + ae.getMessage());
        }

        return dst;
    }

    // Need a createColorModel().
    // Create ComponentColorModel for TYPE_RGB images
    private ComponentColorModel createAlphaComponentColorModel(
            final int dataType, final int numBands,
            final boolean isAlphaPremultiplied, final int transparency) {

        ComponentColorModel ccm = null;
        int[] RGBBits = null;
        ColorSpace cs = null;
        switch (numBands) {
        case 2: // gray+alpha
            cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            break;
        case 4: // RGB+alpha
            cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            break;
        default:
            throw new IllegalArgumentException(
                    PropertyUtil.getString("TIFFImage19") + ": " + numBands);
        }

        int componentSize = 0;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            componentSize = 8;
            break;
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            componentSize = 16;
            break;
        case DataBuffer.TYPE_INT:
            componentSize = 32;
            break;
        default:
            throw new IllegalArgumentException(
                    PropertyUtil.getString("TIFFImage20") + ": " + dataType);
        }

        RGBBits = new int[numBands];
        for (int i = 0; i < numBands; i++) {
            RGBBits[i] = componentSize;
        }

        ccm = new ComponentColorModel(cs, RGBBits, true, isAlphaPremultiplied,
                transparency, dataType);

        return ccm;
    }

}
