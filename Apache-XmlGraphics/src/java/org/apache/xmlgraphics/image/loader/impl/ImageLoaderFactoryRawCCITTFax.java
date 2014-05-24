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

/* $Id: ImageLoaderFactoryRawCCITTFax.java 924666 2010-03-18 08:26:30Z jeremias $ */

package org.apache.xmlgraphics.image.loader.impl;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.image.codec.tiff.TIFFImage;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.spi.ImageLoader;
import org.apache.xmlgraphics.util.MimeConstants;

/**
 * Factory class for the ImageLoader for raw/undecoded CCITT encoded images.
 */
@Slf4j
public class ImageLoaderFactoryRawCCITTFax extends AbstractImageLoaderFactory {

    private static final String[] MIMES = new String[] { MimeConstants.MIME_TIFF };

    private static final ImageFlavor[][] FLAVORS = new ImageFlavor[][] { { ImageFlavor.RAW_CCITTFAX } };

    /**
     * Returns the MIME type for a given ImageFlavor if it is from a format that
     * is consumed without being undecoded. If the ImageFlavor is no raw flavor,
     * an IllegalArgumentException is thrown.
     *
     * @param flavor
     *            the image flavor
     * @return the associated MIME type
     */
    public static String getMimeForRawFlavor(final ImageFlavor flavor) {
        for (int i = 0, ci = FLAVORS.length; i < ci; i++) {
            for (int j = 0, cj = FLAVORS[i].length; j < cj; j++) {
                if (FLAVORS[i][j].equals(flavor)) {
                    return MIMES[i];
                }
            }
        }
        throw new IllegalArgumentException(
                "ImageFlavor is not a \"raw\" flavor: " + flavor);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getSupportedMIMETypes() {
        return MIMES;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedFlavors(final String mime) {
        for (int i = 0, c = MIMES.length; i < c; i++) {
            if (MIMES[i].equals(mime)) {
                return FLAVORS[i];
            }
        }
        throw new IllegalArgumentException("Unsupported MIME type: " + mime);
    }

    /** {@inheritDoc} */
    @Override
    public ImageLoader newImageLoader(final ImageFlavor targetFlavor) {
        if (targetFlavor.equals(ImageFlavor.RAW_CCITTFAX)) {
            return new ImageLoaderRawCCITTFax();
        } else {
            throw new IllegalArgumentException("Unsupported image flavor: "
                    + targetFlavor);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupported(final ImageInfo imageInfo) {
        final Boolean tiled = (Boolean) imageInfo.getCustomObjects().get(
                "TIFF_TILED");
        if (Boolean.TRUE.equals(tiled)) {
            // We don't support tiled images
            log.trace("Raw CCITT loading not supported for tiled TIFF image");
            return false;
        }
        final Integer compression = (Integer) imageInfo.getCustomObjects().get(
                "TIFF_COMPRESSION");
        if (compression == null) {
            return false;
        }
        switch (compression.intValue()) {
        case TIFFImage.COMP_FAX_G3_1D:
        case TIFFImage.COMP_FAX_G3_2D:
        case TIFFImage.COMP_FAX_G4_2D:
            final Integer stripCount = (Integer) imageInfo.getCustomObjects()
            .get("TIFF_STRIP_COUNT");
            final boolean supported = stripCount != null
                    && stripCount.intValue() == 1;
            if (!supported) {
                log.trace("Raw CCITT loading not supported for multi-strip TIFF image");
            }
            return supported;
        default:
            return false;
        }
    }

}
