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

/* $Id: PDFImageHandlerGraphics2D.java 1095876 2011-04-22 06:55:48Z jeremias $ */

package org.apache.fop.render.pdf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.apache.fop.render.AbstractImageHandlerGraphics2D;
import org.apache.fop.render.ImageHandlerUtil;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.render.pdf.PDFLogicalStructureHandler.MarkedContentInfo;
import org.apache.fop.svg.PDFGraphics2D;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageGraphics2D;

/**
 * PDFImageHandler implementation which handles Graphics2D images.
 */
public class PDFImageHandlerGraphics2D extends AbstractImageHandlerGraphics2D {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] { ImageFlavor.GRAPHICS2D, };

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final PDFRenderingContext pdfContext = (PDFRenderingContext) context;
        final PDFContentGenerator generator = pdfContext.getGenerator();
        final ImageGraphics2D imageG2D = (ImageGraphics2D) image;
        final float fwidth = pos.width / 1000f;
        final float fheight = pos.height / 1000f;
        final float fx = pos.x / 1000f;
        final float fy = pos.y / 1000f;

        // get the 'width' and 'height' attributes of the SVG document
        final Dimension dim = image.getInfo().getSize().getDimensionMpt();
        final float imw = (float) dim.getWidth() / 1000f;
        final float imh = (float) dim.getHeight() / 1000f;

        final float sx = fwidth / imw;
        final float sy = fheight / imh;

        generator.comment("G2D start");
        final boolean accessibilityEnabled = context.getUserAgent()
                .isAccessibilityEnabled();
        if (accessibilityEnabled) {
            final MarkedContentInfo mci = pdfContext.getMarkedContentInfo();
            generator.saveGraphicsState(mci.tag, mci.mcid);
        } else {
            generator.saveGraphicsState();
        }
        generator.updateColor(Color.black, false, null);
        generator.updateColor(Color.black, true, null);

        // TODO Clip to the image area.

        // transform so that the coordinates (0,0) is from the top left
        // and positive is down and to the right. (0,0) is where the
        // viewBox puts it.
        generator.add(sx + " 0 0 " + sy + " " + fx + " " + fy + " cm\n");

        final boolean textAsShapes = false;
        final PDFGraphics2D graphics = new PDFGraphics2D(textAsShapes,
                pdfContext.getFontInfo(), generator.getDocument(),
                generator.getResourceContext(), pdfContext.getPage()
                        .referencePDF(), "", 0.0f);
        graphics.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());

        final AffineTransform transform = new AffineTransform();
        transform.translate(fx, fy);
        generator.getState().concatenate(transform);
        graphics.setPaintingState(generator.getState());
        graphics.setOutputStream(generator.getOutputStream());

        final Rectangle2D area = new Rectangle2D.Double(0.0, 0.0, imw, imh);
        imageG2D.getGraphics2DImagePainter().paint(graphics, area);

        generator.add(graphics.getString());
        if (accessibilityEnabled) {
            generator.restoreGraphicsStateAccess();
        } else {
            generator.restoreGraphicsState();
        }
        generator.comment("G2D end");
    }

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
        return FLAVORS;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        final boolean supported = (image == null || image instanceof ImageGraphics2D)
                && targetContext instanceof PDFRenderingContext;
        if (supported) {
            final String mode = (String) targetContext
                    .getHint(ImageHandlerUtil.CONVERSION_MODE);
            if (ImageHandlerUtil.isConversionModeBitmap(mode)) {
                // Disabling this image handler automatically causes a bitmap to
                // be generated
                return false;
            }
        }
        return supported;
    }

}
