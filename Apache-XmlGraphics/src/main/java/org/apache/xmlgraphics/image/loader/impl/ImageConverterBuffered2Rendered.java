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

/* $Id: ImageConverterBuffered2Rendered.java 798806 2009-07-29 08:11:13Z maxberger $ */

package org.apache.xmlgraphics.image.loader.impl;

import java.util.Map;

import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;

/**
 * This ImageConverter converts BufferedImages to RenderedImages (well, it's
 * basically just a class cast).
 */
public class ImageConverterBuffered2Rendered extends AbstractImageConverter {

    /** {@inheritDoc} */
    @Override
    public Image convert(final Image src, final Map hints) {
        checkSourceFlavor(src);
        final ImageBuffered buffered = (ImageBuffered) src;
        return new ImageRendered(buffered.getInfo(),
                buffered.getRenderedImage(), buffered.getTransparentColor());
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getSourceFlavor() {
        return ImageFlavor.BUFFERED_IMAGE;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getTargetFlavor() {
        return ImageFlavor.RENDERED_IMAGE;
    }

    /** {@inheritDoc} */
    @Override
    public int getConversionPenalty() {
        return NO_CONVERSION_PENALTY;
    }

}
