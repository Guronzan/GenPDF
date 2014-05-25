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

/* $Id: Java2DImageHandlerRenderedImage.java 1038291 2010-11-23 19:23:59Z vhennebert $ */

package org.apache.fop.render.java2d;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;

import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.RenderingContext;
import org.apache.xmlgraphics.image.GraphicsConstants;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.impl.ImageRendered;

/**
 * Image handler implementation that paints {@link RenderedImage} instances on a
 * {@link Graphics2D} object.
 */
public class Java2DImageHandlerRenderedImage implements ImageHandler {

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 300;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageRendered.class;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedImageFlavors() {
        return new ImageFlavor[] { ImageFlavor.BUFFERED_IMAGE,
                ImageFlavor.RENDERED_IMAGE, };
    }

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final Java2DRenderingContext java2dContext = (Java2DRenderingContext) context;
        final ImageInfo info = image.getInfo();
        final ImageRendered imageRend = (ImageRendered) image;
        final Graphics2D g2d = java2dContext.getGraphics2D();

        final AffineTransform at = new AffineTransform();
        at.translate(pos.x, pos.y);
        // scaling based on layout instructions
        double sx = pos.getWidth() / info.getSize().getWidthMpt();
        double sy = pos.getHeight() / info.getSize().getHeightMpt();

        // scaling because of image resolution
        // float sourceResolution =
        // java2dContext.getUserAgent().getSourceResolution();
        // source resolution seems to be a bad idea, not sure why
        float sourceResolution = GraphicsConstants.DEFAULT_DPI;
        sourceResolution *= 1000; // we're working in the millipoint area
        sx *= sourceResolution / info.getSize().getDpiHorizontal();
        sy *= sourceResolution / info.getSize().getDpiVertical();
        at.scale(sx, sy);
        final RenderedImage rend = imageRend.getRenderedImage();
        if (imageRend.getTransparentColor() != null
                && !rend.getColorModel().hasAlpha()) {
            final int transCol = imageRend.getTransparentColor().getRGB();
            final BufferedImage bufImage = makeTransparentImage(rend);
            final WritableRaster alphaRaster = bufImage.getAlphaRaster();
            // TODO Masked images: Does anyone know a more efficient method to
            // do this?
            final int[] transparent = new int[] { 0x00 };
            for (int y = 0, maxy = bufImage.getHeight(); y < maxy; y++) {
                for (int x = 0, maxx = bufImage.getWidth(); x < maxx; x++) {
                    final int col = bufImage.getRGB(x, y);
                    if (col == transCol) {
                        // Mask out all pixels that match the transparent color
                        alphaRaster.setPixel(x, y, transparent);
                    }
                }
            }
            g2d.drawRenderedImage(bufImage, at);
        } else {
            g2d.drawRenderedImage(rend, at);
        }
    }

    private BufferedImage makeTransparentImage(final RenderedImage src) {
        final BufferedImage bufImage = new BufferedImage(src.getWidth(),
                src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = bufImage.createGraphics();
        g2d.drawRenderedImage(src, new AffineTransform());
        g2d.dispose();
        return bufImage;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        return (image == null || image instanceof ImageRendered)
                && targetContext instanceof Java2DRenderingContext;
    }

}
