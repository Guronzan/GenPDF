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

/* $Id: AFPImageHandlerSVG.java 1095874 2011-04-22 06:43:22Z jeremias $ */

package org.apache.fop.render.afp;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.afp.AFPGraphics2D;
import org.apache.fop.afp.AFPGraphicsObjectInfo;
import org.apache.fop.afp.AFPObjectAreaInfo;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPResourceLevel;
import org.apache.fop.afp.AFPResourceManager;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.image.loader.batik.BatikImageFlavors;
import org.apache.fop.image.loader.batik.BatikUtil;
import org.apache.fop.image.loader.batik.Graphics2DImagePainterImpl;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.ImageHandlerUtil;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.svg.SVGEventProducer;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageXMLDOM;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;
import org.w3c.dom.Document;

/**
 * Image handler implementation which handles SVG images for AFP output.
 * <p>
 * Note: This class is not intended to be used as an {@link AFPImageHandler} but
 * only as an {@link ImageHandler}. It subclasses {@link AFPImageHandler} only
 * to get access to common methods.
 */
public class AFPImageHandlerSVG implements ImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] { BatikImageFlavors.SVG_DOM };

    /** @return a new AFP data object info instance */
    protected AFPDataObjectInfo createDataObjectInfo() {
        return new AFPGraphicsObjectInfo();
    }

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final AFPRenderingContext afpContext = (AFPRenderingContext) context;
        final ImageXMLDOM imageSVG = (ImageXMLDOM) image;
        final FOUserAgent userAgent = afpContext.getUserAgent();

        final AFPGraphicsObjectInfo graphicsObjectInfo = (AFPGraphicsObjectInfo) createDataObjectInfo();
        final AFPResourceInfo resourceInfo = graphicsObjectInfo
                .getResourceInfo();
        setDefaultToInlineResourceLevel(graphicsObjectInfo);

        // Create a new AFPGraphics2D
        final AFPPaintingState paintingState = afpContext.getPaintingState();
        final boolean textAsShapes = paintingState.isStrokeGOCAText();
        final AFPGraphics2D g2d = new AFPGraphics2D(textAsShapes,
                afpContext.getPaintingState(), afpContext.getResourceManager(),
                resourceInfo, textAsShapes ? null : afpContext.getFontInfo());
        g2d.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());

        paintingState.setImageUri(image.getInfo().getOriginalURI());

        // Create an AFPBridgeContext
        final BridgeContext bridgeContext = AFPSVGHandler.createBridgeContext(
                userAgent, g2d);

        // Cloning SVG DOM as Batik attaches non-thread-safe facilities (like
        // the CSS engine)
        // to it.
        final Document clonedDoc = BatikUtil.cloneSVGDocument(imageSVG
                .getDocument());

        // Build the SVG DOM and provide the painter with it
        GraphicsNode root;
        try {
            final GVTBuilder builder = new GVTBuilder();
            root = builder.build(bridgeContext, clonedDoc);
        } catch (final Exception e) {
            final SVGEventProducer eventProducer = SVGEventProducer.Provider
                    .get(context.getUserAgent().getEventBroadcaster());
            eventProducer
                    .svgNotBuilt(this, e, image.getInfo().getOriginalURI());
            return;
        }

        // Image positioning
        final AFPObjectAreaInfo objectAreaInfo = AFPImageHandler
                .createObjectAreaInfo(paintingState, pos);
        graphicsObjectInfo.setObjectAreaInfo(objectAreaInfo);

        paintingState.save(); // save
        final AffineTransform placement = new AffineTransform();
        placement.translate(pos.x, pos.y);
        paintingState.concatenate(placement);

        // Set up painter and target
        graphicsObjectInfo.setGraphics2D(g2d);
        // Create Graphics2DImagePainter
        final Dimension imageSize = image.getSize().getDimensionMpt();
        final Graphics2DImagePainter painter = new Graphics2DImagePainterImpl(
                root, bridgeContext, imageSize);
        graphicsObjectInfo.setPainter(painter);

        // Create the GOCA GraphicsObject in the DataStream
        final AFPResourceManager resourceManager = afpContext
                .getResourceManager();
        resourceManager.createObject(graphicsObjectInfo);

        paintingState.restore(); // resume
    }

    private void setDefaultToInlineResourceLevel(
            final AFPGraphicsObjectInfo graphicsObjectInfo) {
        final AFPResourceInfo resourceInfo = graphicsObjectInfo
                .getResourceInfo();
        // level not explicitly set/changed so default to inline for GOCA
        // graphic objects
        // (due to a bug in the IBM AFP Workbench Viewer (2.04.01.07), hard copy
        // works just fine)
        if (!resourceInfo.levelChanged()) {
            resourceInfo
                    .setLevel(new AFPResourceLevel(AFPResourceLevel.INLINE));
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 400;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageXMLDOM.class;
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
        final boolean supported = (image == null || image instanceof ImageXMLDOM
                && image.getFlavor().isCompatible(BatikImageFlavors.SVG_DOM))
                && targetContext instanceof AFPRenderingContext;
        if (supported) {
            final AFPRenderingContext afpContext = (AFPRenderingContext) targetContext;
            if (!afpContext.getPaintingState().isGOCAEnabled()) {
                return false;
            }
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
