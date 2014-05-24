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

/* $Id: Java2DImageHandlerGraphics2D.java 820672 2009-10-01 14:48:27Z jeremias $ */

package org.apache.fop.render.java2d;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.RenderingContext;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.impl.ImageGraphics2D;

/**
 * Image handler implementation that paints {@link Graphics2D} image on another
 * {@link Graphics2D} target.
 */
public class Java2DImageHandlerGraphics2D implements ImageHandler {

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 200;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageGraphics2D.class;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedImageFlavors() {
        return new ImageFlavor[] { ImageFlavor.GRAPHICS2D };
    }

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final Java2DRenderingContext java2dContext = (Java2DRenderingContext) context;
        final ImageInfo info = image.getInfo();
        final ImageGraphics2D imageG2D = (ImageGraphics2D) image;

        final Dimension dim = info.getSize().getDimensionMpt();

        final Graphics2D g2d = (Graphics2D) java2dContext.getGraphics2D()
                .create();
        g2d.translate(pos.x, pos.y);
        final double sx = pos.width / dim.getWidth();
        final double sy = pos.height / dim.getHeight();
        g2d.scale(sx, sy);

        final Rectangle2D area = new Rectangle2D.Double(0.0, 0.0,
                dim.getWidth(), dim.getHeight());
        imageG2D.getGraphics2DImagePainter().paint(g2d, area);
        g2d.dispose();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        return (image == null || image instanceof ImageGraphics2D)
                && targetContext instanceof Java2DRenderingContext;
    }

}
