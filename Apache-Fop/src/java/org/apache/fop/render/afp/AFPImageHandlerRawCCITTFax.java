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

/* $Id: AFPImageHandlerRawCCITTFax.java 1195952 2011-11-01 12:20:21Z phancock $ */

package org.apache.fop.render.afp;

import java.awt.Rectangle;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.afp.AFPImageObjectInfo;
import org.apache.fop.render.RenderingContext;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageRawCCITTFax;
import org.apache.xmlgraphics.image.loader.impl.ImageRawStream;
import org.apache.xmlgraphics.util.MimeConstants;

/**
 * AFPImageHandler implementation which handles CCITT encoded images (CCITT fax
 * group 3/4).
 */
@Slf4j
public class AFPImageHandlerRawCCITTFax extends
AbstractAFPImageHandlerRawStream {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] { ImageFlavor.RAW_CCITTFAX, };

    /** {@inheritDoc} */
    @Override
    protected void setAdditionalParameters(
            final AFPDataObjectInfo dataObjectInfo, final ImageRawStream image) {
        final AFPImageObjectInfo imageObjectInfo = (AFPImageObjectInfo) dataObjectInfo;
        final ImageRawCCITTFax ccitt = (ImageRawCCITTFax) image;
        final int compression = ccitt.getCompression();
        imageObjectInfo.setCompression(compression);

        imageObjectInfo.setBitsPerPixel(1);

        // CCITTFax flavor doesn't have TIFF associated but the AFP library
        // listens to
        // that to identify CCITT encoded images. CCITT is not exclusive to
        // TIFF.
        imageObjectInfo.setMimeType(MimeConstants.MIME_TIFF);
    }

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        log.debug("Embedding undecoded CCITT data as data container...");
        super.handleImage(context, image, pos);
    }

    /** {@inheritDoc} */
    @Override
    protected AFPDataObjectInfo createDataObjectInfo() {
        return new AFPImageObjectInfo();
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 400;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageRawCCITTFax.class;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedImageFlavors() {
        return FLAVORS;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        if (targetContext instanceof AFPRenderingContext) {
            final AFPRenderingContext afpContext = (AFPRenderingContext) targetContext;
            return afpContext.getPaintingState().isNativeImagesSupported()
                    && (image == null || image instanceof ImageRawCCITTFax);
        }
        return false;
    }

}
