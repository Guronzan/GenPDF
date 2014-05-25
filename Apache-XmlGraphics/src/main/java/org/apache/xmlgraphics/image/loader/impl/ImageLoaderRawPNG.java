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

package org.apache.xmlgraphics.image.loader.impl;

import java.io.IOException;
import java.util.Map;

import javax.imageio.stream.ImageInputStream;
import javax.xml.transform.Source;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.image.codec.util.ImageInputStreamSeekableStreamAdapter;
import org.apache.xmlgraphics.image.codec.util.SeekableStream;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.apache.xmlgraphics.util.MimeConstants;

@Slf4j
public class ImageLoaderRawPNG extends AbstractImageLoader {

    /**
     * Main constructor.
     */
    public ImageLoaderRawPNG() {
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getTargetFlavor() {
        return ImageFlavor.RAW_PNG;
    }

    /** {@inheritDoc} */
    @Override
    public Image loadImage(final ImageInfo info, final Map hints,
            final ImageSessionContext session) throws ImageException,
            IOException {
        if (!MimeConstants.MIME_PNG.equals(info.getMimeType())) {
            throw new IllegalArgumentException(
                    "ImageInfo must be from a image with MIME type: "
                            + MimeConstants.MIME_PNG);
        }

        final Source src = session.needSource(info.getOriginalURI());
        final ImageInputStream in = ImageUtil.needImageInputStream(src);
        // Remove streams as we do things with them at some later time.
        ImageUtil.removeStreams(src);
        final SeekableStream seekStream = new ImageInputStreamSeekableStreamAdapter(
                in);
        final PNGFile im = new PNGFile(seekStream);
        final ImageRawPNG irpng = im.getImageRawPNG(info);
        return irpng;
    }

    /** {@inheritDoc} */
    @Override
    public int getUsagePenalty() {
        // since this image loader does not handle all kinds of PNG images then
        // we add some penalty to it
        // so that it is not chosen by default; instead, users need to give it a
        // negative penalty in
        // fop.xconf so that it is used
        return 1000;
    }

}
