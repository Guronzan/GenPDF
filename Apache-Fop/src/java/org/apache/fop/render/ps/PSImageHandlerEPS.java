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

/* $Id: PSImageHandlerEPS.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.ps;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.RenderingContext;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.impl.ImageRawEPS;
import org.apache.xmlgraphics.ps.PSGenerator;
import org.apache.xmlgraphics.ps.PSImageUtils;

/**
 * Image handler implementation which handles EPS images for PostScript output.
 */
public class PSImageHandlerEPS implements ImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] { ImageFlavor.RAW_EPS };

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final PSRenderingContext psContext = (PSRenderingContext) context;
        final PSGenerator gen = psContext.getGenerator();
        final ImageRawEPS eps = (ImageRawEPS) image;

        final float x = (float) pos.getX() / 1000f;
        final float y = (float) pos.getY() / 1000f;
        final float w = (float) pos.getWidth() / 1000f;
        final float h = (float) pos.getHeight() / 1000f;

        final ImageInfo info = image.getInfo();
        Rectangle2D bbox = eps.getBoundingBox();
        if (bbox == null) {
            bbox = new Rectangle2D.Double();
            bbox.setFrame(new Point2D.Double(), info.getSize().getDimensionPt());
        }
        final InputStream in = eps.createInputStream();
        try {
            String resourceName = info.getOriginalURI();
            if (resourceName == null) {
                resourceName = "inline image";
            }
            PSImageUtils.renderEPS(in, resourceName, new Rectangle2D.Float(x,
                    y, w, h), bbox, gen);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 200;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageRawEPS.class;
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
            return !psContext.isCreateForms()
                    && (image == null || image instanceof ImageRawEPS);
        }
        return false;
    }

}
