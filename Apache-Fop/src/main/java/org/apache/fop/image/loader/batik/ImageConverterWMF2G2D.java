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

/* $Id: ImageConverterWMF2G2D.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.image.loader.batik;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.transcoder.wmf.tosvg.WMFPainter;
import org.apache.batik.transcoder.wmf.tosvg.WMFRecordStore;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.AbstractImageConverter;
import org.apache.xmlgraphics.image.loader.impl.ImageGraphics2D;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;

/**
 * This ImageConverter converts WMF (Windows Metafile) images (represented by
 * Batik's WMFRecordStore) to Java2D.
 */
@Slf4j
public class ImageConverterWMF2G2D extends AbstractImageConverter {

    /** {@inheritDoc} */
    @Override
    public Image convert(final Image src, final Map hints) {
        checkSourceFlavor(src);
        final ImageWMF wmf = (ImageWMF) src;

        Graphics2DImagePainter painter;
        painter = new Graphics2DImagePainterWMF(wmf);

        final ImageGraphics2D g2dImage = new ImageGraphics2D(src.getInfo(),
                painter);
        return g2dImage;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getSourceFlavor() {
        return ImageWMF.WMF_IMAGE;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getTargetFlavor() {
        return ImageFlavor.GRAPHICS2D;
    }

    private static class Graphics2DImagePainterWMF implements
    Graphics2DImagePainter {

        private final ImageWMF wmf;

        public Graphics2DImagePainterWMF(final ImageWMF wmf) {
            this.wmf = wmf;
        }

        /** {@inheritDoc} */
        @Override
        public Dimension getImageSize() {
            return this.wmf.getSize().getDimensionMpt();
        }

        /** {@inheritDoc} */
        @Override
        public void paint(final Graphics2D g2d, final Rectangle2D area) {
            final WMFRecordStore wmfStore = this.wmf.getRecordStore();
            final double w = area.getWidth();
            final double h = area.getHeight();

            // Fit in paint area
            g2d.translate(area.getX(), area.getY());
            final double sx = w / wmfStore.getWidthPixels();
            final double sy = h / wmfStore.getHeightPixels();
            if (sx != 1.0 || sy != 1.0) {
                g2d.scale(sx, sy);
            }

            final WMFPainter painter = new WMFPainter(wmfStore, 1.0f);
            final long start = System.currentTimeMillis();
            painter.paint(g2d);
            if (log.isDebugEnabled()) {
                final long duration = System.currentTimeMillis() - start;
                log.debug("Painting WMF took " + duration + " ms.");
            }
        }

    }

}
