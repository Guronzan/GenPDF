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

package org.apache.fop.render.afp;

import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.io.IOException;
import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.afp.AFPImageObjectInfo;
import org.apache.fop.afp.AFPObjectAreaInfo;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPResourceManager;
import org.apache.fop.afp.ioca.ImageContent;
import org.apache.fop.afp.modca.ResourceObject;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.RenderingContext;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.ImageRawJPEG;
import org.apache.xmlgraphics.image.loader.impl.JPEGConstants;
import org.apache.xmlgraphics.util.MimeConstants;

/**
 * {@link ImageHandler} implementation which handles ImageRawJPEG instances.
 * JPEG data is embedded directly (not decoded) into IOCA images (FS11 or FS45).
 */
@Slf4j
public class AFPImageHandlerRawJPEG extends AFPImageHandler implements
ImageHandler {

    private void setDefaultResourceLevel(
            final AFPImageObjectInfo imageObjectInfo,
            final AFPResourceManager resourceManager) {
        final AFPResourceInfo resourceInfo = imageObjectInfo.getResourceInfo();
        if (!resourceInfo.levelChanged()) {
            resourceInfo.setLevel(resourceManager.getResourceLevelDefaults()
                    .getDefaultResourceLevel(ResourceObject.TYPE_IMAGE));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected AFPDataObjectInfo createDataObjectInfo() {
        return new AFPImageObjectInfo();
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 150;
    }

    /** {@inheritDoc} */
    @Override
    public Class<?> getSupportedImageClass() {
        return ImageRawJPEG.class;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedImageFlavors() {
        return new ImageFlavor[] { ImageFlavor.RAW_JPEG };
    }

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final AFPRenderingContext afpContext = (AFPRenderingContext) context;

        final AFPImageObjectInfo imageObjectInfo = (AFPImageObjectInfo) createDataObjectInfo();
        final AFPPaintingState paintingState = afpContext.getPaintingState();

        // set resource information
        setResourceInformation(imageObjectInfo, image.getInfo()
                .getOriginalURI(), afpContext.getForeignAttributes());
        setDefaultResourceLevel(imageObjectInfo,
                afpContext.getResourceManager());

        // Positioning
        imageObjectInfo.setObjectAreaInfo(createObjectAreaInfo(paintingState,
                pos));
        updateIntrinsicSize(imageObjectInfo, paintingState, image.getSize());

        // Image content
        final ImageRawJPEG jpeg = (ImageRawJPEG) image;
        imageObjectInfo.setCompression(ImageContent.COMPID_JPEG);
        final ColorSpace cs = jpeg.getColorSpace();
        switch (cs.getType()) {
        case ColorSpace.TYPE_GRAY:
            imageObjectInfo.setMimeType(MimeConstants.MIME_AFP_IOCA_FS11);
            imageObjectInfo.setColor(false);
            imageObjectInfo.setBitsPerPixel(8);
            break;
        case ColorSpace.TYPE_RGB:
            imageObjectInfo.setMimeType(MimeConstants.MIME_AFP_IOCA_FS11);
            imageObjectInfo.setColor(true);
            imageObjectInfo.setBitsPerPixel(24);
            break;
        case ColorSpace.TYPE_CMYK:
            imageObjectInfo.setMimeType(MimeConstants.MIME_AFP_IOCA_FS45);
            imageObjectInfo.setColor(true);
            imageObjectInfo.setBitsPerPixel(32);
            break;
        default:
            throw new IllegalStateException(
                    "Color space of JPEG image not supported: " + cs);
        }

        final boolean included = afpContext.getResourceManager()
                .tryIncludeObject(imageObjectInfo);
        if (!included) {
            log.debug("Embedding undecoded JPEG as IOCA image...");
            final InputStream inputStream = jpeg.createInputStream();
            try {
                imageObjectInfo.setData(IOUtils.toByteArray(inputStream));
            } finally {
                IOUtils.closeQuietly(inputStream);
            }

            // Create image
            afpContext.getResourceManager().createObject(imageObjectInfo);
        }
    }

    private void updateIntrinsicSize(final AFPImageObjectInfo imageObjectInfo,
            final AFPPaintingState paintingState, final ImageSize targetSize) {
        // Update image object info
        imageObjectInfo.setDataHeightRes((int) Math.round(targetSize
                .getDpiHorizontal() * 10));
        imageObjectInfo.setDataWidthRes((int) Math.round(targetSize
                .getDpiVertical() * 10));
        imageObjectInfo.setDataHeight(targetSize.getHeightPx());
        imageObjectInfo.setDataWidth(targetSize.getWidthPx());

        // set object area info
        final int resolution = paintingState.getResolution();
        final AFPObjectAreaInfo objectAreaInfo = imageObjectInfo
                .getObjectAreaInfo();
        objectAreaInfo.setResolution(resolution);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        if (!(targetContext instanceof AFPRenderingContext)) {
            return false; // AFP-specific image handler
        }
        final AFPRenderingContext context = (AFPRenderingContext) targetContext;
        final AFPPaintingState paintingState = context.getPaintingState();
        if (!paintingState.canEmbedJpeg()) {
            return false;
        }
        if (paintingState.getBitsPerPixel() < 8) {
            return false; // This would stand in the way of dithering and cause
            // exceptions
        }
        if (image == null) {
            return true; // Don't know the image format, yet
        }
        if (image instanceof ImageRawJPEG) {
            final ImageRawJPEG jpeg = (ImageRawJPEG) image;
            final ColorSpace cs = jpeg.getColorSpace();
            switch (cs.getType()) {
            case ColorSpace.TYPE_GRAY:
            case ColorSpace.TYPE_RGB:
                // ok
                break;
            case ColorSpace.TYPE_CMYK:
                if (!paintingState.isCMYKImagesSupported()) {
                    return false; // CMYK is disabled
                    // Note: you may need to disable this image handler through
                    // configuration
                    // if you want to paint a CMYK JPEG on 24bit and less
                    // configurations.
                }
                break;
            default:
                return false; // not supported
            }

            if (jpeg.getSOFType() != JPEGConstants.SOF0) {
                return false; // We'll let only baseline DCT through.
            }
            return true;
        }
        return false;
    }

}
