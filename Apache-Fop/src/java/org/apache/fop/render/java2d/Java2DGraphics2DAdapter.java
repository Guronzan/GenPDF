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

/* $Id: Java2DGraphics2DAdapter.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.java2d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.apache.fop.render.AbstractGraphics2DAdapter;
import org.apache.fop.render.RendererContext;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;

/**
 * Graphics2DAdapter implementation for Java2D.
 */
public class Java2DGraphics2DAdapter extends AbstractGraphics2DAdapter {

    /** {@inheritDoc} */
    @Override
    public void paintImage(final Graphics2DImagePainter painter,
            final RendererContext context, final int x, final int y,
            final int width, final int height) throws IOException {

        final float fwidth = width / 1000f;
        final float fheight = height / 1000f;
        final float fx = x / 1000f;
        final float fy = y / 1000f;

        // get the 'width' and 'height' attributes of the SVG document
        final Dimension dim = painter.getImageSize();
        final float imw = (float) dim.getWidth() / 1000f;
        final float imh = (float) dim.getHeight() / 1000f;

        final float sx = fwidth / imw;
        final float sy = fheight / imh;

        final Java2DRenderer renderer = (Java2DRenderer) context.getRenderer();
        final Java2DGraphicsState state = renderer.state;

        // Create copy and paint on that
        final Graphics2D g2d = (Graphics2D) state.getGraph().create();
        g2d.setColor(Color.black);
        g2d.setBackground(Color.black);

        // TODO Clip to the image area.

        // transform so that the coordinates (0,0) is from the top left
        // and positive is down and to the right. (0,0) is where the
        // viewBox puts it.
        g2d.translate(fx, fy);
        final AffineTransform at = AffineTransform.getScaleInstance(sx, sy);
        if (!at.isIdentity()) {
            g2d.transform(at);
        }

        final Rectangle2D area = new Rectangle2D.Double(0.0, 0.0, imw, imh);
        painter.paint(g2d, area);

        g2d.dispose();
    }

}
