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

/* $Id: Graphics2DImagePainterImpl.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.image.loader.batik;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;

/**
 * A graphics 2D image painter implementation for painting SVG images using
 * Batik.
 */
public class Graphics2DImagePainterImpl implements Graphics2DImagePainter {

    private final GraphicsNode root;
    /** the Batik bridge context */
    protected final BridgeContext ctx;
    /** the intrinsic size of the image */
    protected final Dimension imageSize;

    /**
     * Main constructor
     *
     * @param root
     *            the graphics node root
     * @param ctx
     *            the bridge context
     * @param imageSize
     *            the image size
     */
    public Graphics2DImagePainterImpl(final GraphicsNode root,
            final BridgeContext ctx, final Dimension imageSize) {
        this.root = root;
        this.imageSize = imageSize;
        this.ctx = ctx;
    }

    /** {@inheritDoc} */
    @Override
    public Dimension getImageSize() {
        return this.imageSize;
    }

    private void prepare(final Graphics2D g2d, final Rectangle2D area) {
        // If no viewbox is defined in the svg file, a viewbox of 100x100 is
        // assumed, as defined in SVGUserAgent.getViewportSize()
        final double tx = area.getX();
        final double ty = area.getY();
        if (tx != 0 || ty != 0) {
            g2d.translate(tx, ty);
        }

        final float iw = (float) this.ctx.getDocumentSize().getWidth();
        final float ih = (float) this.ctx.getDocumentSize().getHeight();
        final float w = (float) area.getWidth();
        final float h = (float) area.getHeight();
        final float sx = w / iw;
        final float sy = h / ih;
        if (sx != 1.0 || sy != 1.0) {
            g2d.scale(sx, sy);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void paint(final Graphics2D g2d, final Rectangle2D area) {
        prepare(g2d, area);
        this.root.paint(g2d);
    }

}
