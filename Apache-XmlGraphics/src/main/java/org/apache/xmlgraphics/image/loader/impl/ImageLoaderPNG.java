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

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;

import javax.imageio.stream.ImageInputStream;
import javax.xml.transform.Source;

import org.apache.xmlgraphics.image.codec.png.PNGDecodeParam;
import org.apache.xmlgraphics.image.codec.png.PNGImageDecoder;
import org.apache.xmlgraphics.image.codec.util.ImageInputStreamSeekableStreamAdapter;
import org.apache.xmlgraphics.image.codec.util.SeekableStream;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;

public class ImageLoaderPNG extends AbstractImageLoader {

    public ImageLoaderPNG() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public Image loadImage(final ImageInfo info, final Map hints,
            final ImageSessionContext session) throws ImageException,
    IOException {

        final Source src = session.needSource(info.getOriginalURI());
        final ImageInputStream imgStream = ImageUtil.needImageInputStream(src);

        final SeekableStream seekStream = new ImageInputStreamSeekableStreamAdapter(
                imgStream);

        final PNGImageDecoder decoder = new PNGImageDecoder(seekStream,
                new PNGDecodeParam());
        final RenderedImage image = decoder.decodeAsRenderedImage();

        // need transparency here?
        return new ImageRendered(info, image, null);
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getTargetFlavor() {
        return ImageFlavor.RENDERED_IMAGE;
    }

    /** {@inheritDoc} */
    @Override
    public int getUsagePenalty() {
        // since this image loader does not provide any benefits over the
        // default sun.imageio one we add
        // some penalty to it so that it is not chosen by default; instead users
        // need to give it a negative
        // penalty in fop.xconf so that it is used; this image loader is mostly
        // for testing purposes for now.
        return 1000;
    }

}
