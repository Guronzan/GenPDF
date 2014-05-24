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

/* $Id: PSImageHandlerRawPNG.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.ps;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.io.IOException;

import org.apache.fop.render.RenderingContext;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.impl.ImageRawPNG;
import org.apache.xmlgraphics.ps.FormGenerator;
import org.apache.xmlgraphics.ps.ImageEncoder;
import org.apache.xmlgraphics.ps.ImageFormGenerator;
import org.apache.xmlgraphics.ps.PSGenerator;
import org.apache.xmlgraphics.ps.PSImageUtils;

/**
 * Image handler implementation which handles raw (not decoded) PNG images for
 * PostScript output.
 */
public class PSImageHandlerRawPNG implements PSImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] { ImageFlavor.RAW_PNG };

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final PSRenderingContext psContext = (PSRenderingContext) context;
        final PSGenerator gen = psContext.getGenerator();
        final ImageRawPNG png = (ImageRawPNG) image;

        final float x = (float) pos.getX() / 1000f;
        final float y = (float) pos.getY() / 1000f;
        final float w = (float) pos.getWidth() / 1000f;
        final float h = (float) pos.getHeight() / 1000f;
        final Rectangle2D targetRect = new Rectangle2D.Float(x, y, w, h);

        final ImageEncoder encoder = new ImageEncoderPNG(png);
        final ImageInfo info = image.getInfo();
        final Dimension imgDim = info.getSize().getDimensionPx();
        final String imgDescription = image.getClass().getName();
        final ColorModel cm = png.getColorModel();

        PSImageUtils.writeImage(encoder, imgDim, imgDescription, targetRect,
                cm, gen);
    }

    /** {@inheritDoc} */
    @Override
    public void generateForm(final RenderingContext context, final Image image,
            final PSImageFormResource form) throws IOException {
        final PSRenderingContext psContext = (PSRenderingContext) context;
        final PSGenerator gen = psContext.getGenerator();
        final ImageRawPNG png = (ImageRawPNG) image;
        final ImageInfo info = image.getInfo();
        final String imageDescription = info.getMimeType() + " "
                + info.getOriginalURI();

        final ImageEncoder encoder = new ImageEncoderPNG(png);
        final FormGenerator formGen = new ImageFormGenerator(form.getName(),
                imageDescription, info.getSize().getDimensionPt(), info
                        .getSize().getDimensionPx(), encoder,
                png.getColorSpace(), false);
        formGen.generate(gen);
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 200;
    }

    /** {@inheritDoc} */
    @Override
    public Class<ImageRawPNG> getSupportedImageClass() {
        return ImageRawPNG.class;
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
                return image == null || image instanceof ImageRawPNG;
            }
        }
        return false;
    }

}
