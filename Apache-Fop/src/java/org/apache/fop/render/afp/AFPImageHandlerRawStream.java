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

/* $Id: AFPImageHandlerRawStream.java 1195952 2011-11-01 12:20:21Z phancock $ */

package org.apache.fop.render.afp;

import java.awt.Rectangle;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.render.RenderingContext;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageRawEPS;
import org.apache.xmlgraphics.image.loader.impl.ImageRawJPEG;
import org.apache.xmlgraphics.image.loader.impl.ImageRawStream;
import org.apache.xmlgraphics.util.MimeConstants;

/**
 * AFPImageHandler implementation which handles raw stream images.
 */
@Slf4j
public class AFPImageHandlerRawStream extends AbstractAFPImageHandlerRawStream {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] {
        ImageFlavor.RAW_JPEG, ImageFlavor.RAW_TIFF, ImageFlavor.RAW_EPS, };

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 200;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageRawStream.class;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedImageFlavors() {
        return FLAVORS;
    }

    /** {@inheritDoc} */
    @Override
    protected AFPDataObjectInfo createDataObjectInfo() {
        return new AFPDataObjectInfo();
    }

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Embedding undecoded image data ("
                    + image.getInfo().getMimeType() + ") as data container...");
        }
        super.handleImage(context, image, pos);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        if (targetContext instanceof AFPRenderingContext) {
            final AFPRenderingContext afpContext = (AFPRenderingContext) targetContext;
            return afpContext.getPaintingState().isNativeImagesSupported()
                    && (image == null || image instanceof ImageRawJPEG
                    || image instanceof ImageRawEPS || image instanceof ImageRawStream
                    && MimeConstants.MIME_TIFF
                    .equals(((ImageRawStream) image)
                            .getMimeType()));
        }
        return false;
    }
}
