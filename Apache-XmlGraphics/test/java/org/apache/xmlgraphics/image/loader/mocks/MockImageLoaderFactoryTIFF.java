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

/* $Id: MockImageLoaderFactoryTIFF.java 924666 2010-03-18 08:26:30Z jeremias $ */

package org.apache.xmlgraphics.image.loader.mocks;

import java.io.IOException;
import java.util.Map;

import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.impl.AbstractImageLoaderFactory;
import org.apache.xmlgraphics.image.loader.spi.ImageLoader;
import org.apache.xmlgraphics.util.MimeConstants;

import static org.junit.Assert.assertTrue;

/**
 * Mock implementation posing as a TIFF-compatible loader.
 */
public class MockImageLoaderFactoryTIFF extends AbstractImageLoaderFactory {

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedFlavors(final String mime) {
        return new ImageFlavor[] { ImageFlavor.BUFFERED_IMAGE,
                ImageFlavor.RENDERED_IMAGE };
    }

    /** {@inheritDoc} */
    @Override
    public String[] getSupportedMIMETypes() {
        return new String[] { MimeConstants.MIME_TIFF };
    }

    private void checkSuppportedFlavor(final String mime,
            final ImageFlavor flavor) {
        final ImageFlavor[] flavors = getSupportedFlavors(mime);
        boolean found = false;
        for (final ImageFlavor flavor2 : flavors) {
            if (flavor2.equals(flavor)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSupported(final ImageInfo imageInfo) {
        return MimeConstants.MIME_TIFF.equals(imageInfo.getMimeType());
    }

    /** {@inheritDoc} */
    @Override
    public ImageLoader newImageLoader(final ImageFlavor targetFlavor) {
        checkSuppportedFlavor(MimeConstants.MIME_TIFF, targetFlavor);
        return new ImageLoaderImpl(targetFlavor);
    }

    /** Mock image loader implementation. */
    private static class ImageLoaderImpl implements ImageLoader {

        private final ImageFlavor flavor;

        public ImageLoaderImpl(final ImageFlavor flavor) {
            this.flavor = flavor;
        }

        @Override
        public ImageFlavor getTargetFlavor() {
            return this.flavor;
        }

        @Override
        public int getUsagePenalty() {
            return 0;
        }

        @Override
        public Image loadImage(final ImageInfo info, final Map hints,
                final ImageSessionContext session) throws ImageException,
                IOException {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Image loadImage(final ImageInfo info,
                final ImageSessionContext session) throws ImageException,
                IOException {
            throw new UnsupportedOperationException("not implemented");
        }

    }

}
