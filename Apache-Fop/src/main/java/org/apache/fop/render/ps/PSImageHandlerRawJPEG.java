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

/* $Id: PSImageHandlerRawJPEG.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.ps;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.apache.fop.render.RenderingContext;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.impl.ImageRawJPEG;
import org.apache.xmlgraphics.ps.FormGenerator;
import org.apache.xmlgraphics.ps.ImageEncoder;
import org.apache.xmlgraphics.ps.ImageFormGenerator;
import org.apache.xmlgraphics.ps.PSGenerator;
import org.apache.xmlgraphics.ps.PSImageUtils;

/**
 * Image handler implementation which handles undecoded JPEG images for
 * PostScript output.
 */
public class PSImageHandlerRawJPEG implements PSImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] { ImageFlavor.RAW_JPEG };

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final PSRenderingContext psContext = (PSRenderingContext) context;
        final PSGenerator gen = psContext.getGenerator();
        final ImageRawJPEG jpeg = (ImageRawJPEG) image;

        final float x = (float) pos.getX() / 1000f;
        final float y = (float) pos.getY() / 1000f;
        final float w = (float) pos.getWidth() / 1000f;
        final float h = (float) pos.getHeight() / 1000f;
        final Rectangle2D targetRect = new Rectangle2D.Float(x, y, w, h);

        final ImageInfo info = image.getInfo();

        final ImageEncoder encoder = new ImageEncoderJPEG(jpeg);
        PSImageUtils.writeImage(encoder, info.getSize().getDimensionPx(),
                info.getOriginalURI(), targetRect, jpeg.getColorSpace(), 8,
                jpeg.isInverted(), gen);
    }

    /** {@inheritDoc} */
    @Override
    public void generateForm(final RenderingContext context, final Image image,
            final PSImageFormResource form) throws IOException {
        final PSRenderingContext psContext = (PSRenderingContext) context;
        final PSGenerator gen = psContext.getGenerator();
        final ImageRawJPEG jpeg = (ImageRawJPEG) image;
        final ImageInfo info = image.getInfo();
        final String imageDescription = info.getMimeType() + " "
                + info.getOriginalURI();

        final ImageEncoder encoder = new ImageEncoderJPEG(jpeg);
        final FormGenerator formGen = new ImageFormGenerator(form.getName(),
                imageDescription, info.getSize().getDimensionPt(), info
                        .getSize().getDimensionPx(), encoder,
                jpeg.getColorSpace(), jpeg.isInverted());
        formGen.generate(gen);
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 200;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageRawJPEG.class;
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
        if (targetContext instanceof PSRenderingContext) {
            final PSRenderingContext psContext = (PSRenderingContext) targetContext;
            // The filters required for this implementation need PS level 2 or
            // higher
            if (psContext.getGenerator().getPSLevel() >= 2) {
                return image == null || image instanceof ImageRawJPEG;
            }
        }
        return false;
    }

}
