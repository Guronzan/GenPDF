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

/* $Id: AFPGraphics2DAdapter.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.render.afp;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.apache.fop.afp.AFPGraphics2D;
import org.apache.fop.afp.AFPGraphicsObjectInfo;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPResourceManager;
import org.apache.fop.render.AbstractGraphics2DAdapter;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RendererContext.RendererContextWrapper;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;

/**
 * Graphics2DAdapter implementation for AFP.
 */
public class AFPGraphics2DAdapter extends AbstractGraphics2DAdapter {

    private final AFPPaintingState paintingState;

    /**
     * Main constructor
     *
     * @param paintingState
     *            the AFP painting state
     */
    public AFPGraphics2DAdapter(final AFPPaintingState paintingState) {
        this.paintingState = paintingState;
    }

    /** {@inheritDoc} */
    @Override
    public void paintImage(final Graphics2DImagePainter painter,
            final RendererContext rendererContext, final int x, final int y,
            final int width, final int height) throws IOException {

        final AFPRendererContext afpRendererContext = (AFPRendererContext) rendererContext;
        final AFPInfo afpInfo = afpRendererContext.getInfo();

        final boolean textAsShapes = false;
        final AFPGraphics2D g2d = afpInfo.createGraphics2D(textAsShapes);

        this.paintingState.save();

        // Fallback solution: Paint to a BufferedImage
        if (afpInfo.paintAsBitmap()) {

            // paint image
            final RendererContextWrapper rendererContextWrapper = RendererContext
                    .wrapRendererContext(rendererContext);
            final float targetResolution = rendererContext.getUserAgent()
                    .getTargetResolution();
            final int resolution = Math.round(targetResolution);
            final boolean colorImages = afpInfo.isColorSupported();
            final BufferedImage bufferedImage = paintToBufferedImage(painter,
                    rendererContextWrapper, resolution, !colorImages, false);

            // draw image
            final AffineTransform at = this.paintingState.getData()
                    .getTransform();
            at.translate(x, y);
            g2d.drawImage(bufferedImage, at, null);
        } else {
            final AFPGraphicsObjectInfo graphicsObjectInfo = new AFPGraphicsObjectInfo();
            graphicsObjectInfo.setPainter(painter);
            graphicsObjectInfo.setGraphics2D(g2d);

            // get the 'width' and 'height' attributes of the SVG document
            final Dimension imageSize = painter.getImageSize();
            final float imw = (float) imageSize.getWidth() / 1000f;
            final float imh = (float) imageSize.getHeight() / 1000f;

            final Rectangle2D area = new Rectangle2D.Double(0.0, 0.0, imw, imh);
            graphicsObjectInfo.setArea(area);
            final AFPResourceManager resourceManager = afpInfo
                    .getResourceManager();
            resourceManager.createObject(graphicsObjectInfo);
        }

        this.paintingState.restore();
    }

    /** {@inheritDoc} */
    @Override
    protected int mpt2px(final int unit, final int resolution) {
        return Math
                .round(this.paintingState.getUnitConverter().mpt2units(unit));
    }
}
