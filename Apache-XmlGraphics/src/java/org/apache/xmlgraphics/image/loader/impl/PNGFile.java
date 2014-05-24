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

package org.apache.xmlgraphics.image.loader.impl;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.xmlgraphics.image.codec.png.PNGChunk;
import org.apache.xmlgraphics.image.codec.util.PropertyUtil;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageInfo;

// CSOFF: MethodName

/**
 * Provides methods useful for processing PNG files.
 */
class PNGFile implements PNGConstants {

    private ColorModel colorModel;
    private ICC_Profile iccProfile;
    private int bitDepth;
    private int colorType;
    private boolean isTransparent;
    private int grayTransparentAlpha;
    private int redTransparentAlpha;
    private int greenTransparentAlpha;
    private int blueTransparentAlpha;
    private final List<InputStream> streamVec = new ArrayList<InputStream>();
    private int paletteEntries;
    private byte[] redPalette;
    private byte[] greenPalette;
    private byte[] bluePalette;
    private byte[] alphaPalette;
    private boolean hasPalette;
    private boolean hasAlphaPalette = false;

    public PNGFile(InputStream stream) throws IOException, ImageException {
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        final DataInputStream distream = new DataInputStream(stream);
        final long magic = distream.readLong();
        if (magic != PNG_SIGNATURE) {
            final String msg = PropertyUtil.getString("PNGImageDecoder0");
            throw new ImageException(msg);
        }
        // only some chunks are worth parsing in the current implementation
        do {
            try {
                PNGChunk chunk;
                final String chunkType = PNGChunk.getChunkType(distream);
                if (chunkType.equals(PNGChunk.ChunkType.IHDR.name())) {
                    chunk = PNGChunk.readChunk(distream);
                    parse_IHDR_chunk(chunk);
                } else if (chunkType.equals(PNGChunk.ChunkType.PLTE.name())) {
                    chunk = PNGChunk.readChunk(distream);
                    parse_PLTE_chunk(chunk);
                } else if (chunkType.equals(PNGChunk.ChunkType.IDAT.name())) {
                    chunk = PNGChunk.readChunk(distream);
                    this.streamVec
                            .add(new ByteArrayInputStream(chunk.getData()));
                } else if (chunkType.equals(PNGChunk.ChunkType.IEND.name())) {
                    // chunk = PNGChunk.readChunk(distream);
                    PNGChunk.skipChunk(distream);
                    break; // fall through to the bottom
                } else if (chunkType.equals(PNGChunk.ChunkType.tRNS.name())) {
                    chunk = PNGChunk.readChunk(distream);
                    parse_tRNS_chunk(chunk);
                } else {
                    // chunk = PNGChunk.readChunk(distream);
                    PNGChunk.skipChunk(distream);
                }
            } catch (final Exception e) {
                e.printStackTrace();
                final String msg = PropertyUtil.getString("PNGImageDecoder2");
                throw new RuntimeException(msg);
            }
        } while (true);
    }

    public ImageRawPNG getImageRawPNG(final ImageInfo info)
            throws ImageException {
        final InputStream seqStream = new SequenceInputStream(
                Collections.enumeration(this.streamVec));
        switch (this.colorType) {
        case PNG_COLOR_GRAY:
            if (this.hasPalette) {
                throw new ImageException(
                        "Corrupt PNG: color palette is not allowed!");
            }
            this.colorModel = new ComponentColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false,
                    Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            break;
        case PNG_COLOR_RGB:
            // actually a check of the sRGB chunk would be necessary to confirm
            // if it's really sRGB
            this.colorModel = new ComponentColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                    Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            break;
        case PNG_COLOR_PALETTE:
            if (this.hasAlphaPalette) {
                this.colorModel = new IndexColorModel(this.bitDepth,
                        this.paletteEntries, this.redPalette,
                        this.greenPalette, this.bluePalette, this.alphaPalette);
            } else {
                this.colorModel = new IndexColorModel(this.bitDepth,
                        this.paletteEntries, this.redPalette,
                        this.greenPalette, this.bluePalette);
            }
            break;
        case PNG_COLOR_GRAY_ALPHA:
            if (this.hasPalette) {
                throw new ImageException(
                        "Corrupt PNG: color palette is not allowed!");
            }
            this.colorModel = new ComponentColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_GRAY), true, false,
                    Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            break;
        case PNG_COLOR_RGB_ALPHA:
            // actually a check of the sRGB chunk would be necessary to confirm
            // if it's really sRGB
            this.colorModel = new ComponentColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB), true, false,
                    Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            break;
        default:
            throw new ImageException("Unsupported color type: "
                    + this.colorType);
        }
        // the iccProfile is still null for now
        final ImageRawPNG rawImage = new ImageRawPNG(info, seqStream,
                this.colorModel, this.bitDepth, this.iccProfile);
        if (this.isTransparent) {
            if (this.colorType == PNG_COLOR_GRAY) {
                rawImage.setGrayTransparentAlpha(this.grayTransparentAlpha);
            } else if (this.colorType == PNG_COLOR_RGB) {
                rawImage.setRGBTransparentAlpha(this.redTransparentAlpha,
                        this.greenTransparentAlpha, this.blueTransparentAlpha);
            } else if (this.colorType == PNG_COLOR_PALETTE) {
                rawImage.setTransparent();
            } else {
                //
            }
        }
        return rawImage;
    }

    private void parse_IHDR_chunk(final PNGChunk chunk) {
        final int width = chunk.getInt4(0);
        final int height = chunk.getInt4(4);
        this.bitDepth = chunk.getInt1(8);
        if (this.bitDepth != 8) {
            // this is a limitation of the current implementation
            throw new RuntimeException("Unsupported bit depth: "
                    + this.bitDepth);
        }
        this.colorType = chunk.getInt1(9);
        final int compressionMethod = chunk.getInt1(10);
        if (compressionMethod != 0) {
            throw new RuntimeException("Unsupported PNG compression method: "
                    + compressionMethod);
        }
        final int filterMethod = chunk.getInt1(11);
        if (filterMethod != 0) {
            throw new RuntimeException("Unsupported PNG filter method: "
                    + filterMethod);
        }
        final int interlaceMethod = chunk.getInt1(12);
        if (interlaceMethod != 0) {
            // this is a limitation of the current implementation
            throw new RuntimeException("Unsupported PNG interlace method: "
                    + interlaceMethod);
        }
    }

    private void parse_PLTE_chunk(final PNGChunk chunk) {
        this.paletteEntries = chunk.getLength() / 3;
        this.redPalette = new byte[this.paletteEntries];
        this.greenPalette = new byte[this.paletteEntries];
        this.bluePalette = new byte[this.paletteEntries];
        this.hasPalette = true;

        int pltIndex = 0;
        for (int i = 0; i < this.paletteEntries; i++) {
            this.redPalette[i] = chunk.getByte(pltIndex++);
            this.greenPalette[i] = chunk.getByte(pltIndex++);
            this.bluePalette[i] = chunk.getByte(pltIndex++);
        }
    }

    private void parse_tRNS_chunk(final PNGChunk chunk) {
        if (this.colorType == PNG_COLOR_PALETTE) {
            final int entries = chunk.getLength();
            if (entries > this.paletteEntries) {
                // Error -- mustn't have more alpha than RGB palette entries
                final String msg = PropertyUtil.getString("PNGImageDecoder14");
                throw new RuntimeException(msg);
            }
            // Load beginning of palette from the chunk
            this.alphaPalette = new byte[this.paletteEntries];
            for (int i = 0; i < entries; i++) {
                this.alphaPalette[i] = chunk.getByte(i);
            }
            // Fill rest of palette with 255
            for (int i = entries; i < this.paletteEntries; i++) {
                this.alphaPalette[i] = (byte) 255;
            }
            this.hasAlphaPalette = true;
        } else if (this.colorType == PNG_COLOR_GRAY) {
            this.grayTransparentAlpha = chunk.getInt2(0);
        } else if (this.colorType == PNG_COLOR_RGB) {
            this.redTransparentAlpha = chunk.getInt2(0);
            this.greenTransparentAlpha = chunk.getInt2(2);
            this.blueTransparentAlpha = chunk.getInt2(4);
        } else if (this.colorType == PNG_COLOR_GRAY_ALPHA
                || this.colorType == PNG_COLOR_RGB_ALPHA) {
            // Error -- GA or RGBA image can't have a tRNS chunk.
            final String msg = PropertyUtil.getString("PNGImageDecoder15");
            throw new RuntimeException(msg);
        }
        this.isTransparent = true;
    }

}
