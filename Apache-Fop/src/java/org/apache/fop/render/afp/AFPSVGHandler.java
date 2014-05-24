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

/* $Id: AFPSVGHandler.java 1195952 2011-11-01 12:20:21Z phancock $ */

package org.apache.fop.render.afp;

// FOP
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.fop.afp.AFPGraphics2D;
import org.apache.fop.afp.AFPGraphicsObjectInfo;
import org.apache.fop.afp.AFPObjectAreaInfo;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPResourceManager;
import org.apache.fop.afp.AFPUnitConverter;
import org.apache.fop.afp.svg.AFPBridgeContext;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.image.loader.batik.BatikUtil;
import org.apache.fop.image.loader.batik.Graphics2DImagePainterImpl;
import org.apache.fop.render.AbstractGenericSVGHandler;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RendererContext.RendererContextWrapper;
import org.apache.fop.svg.SVGEventProducer;
import org.apache.fop.svg.SVGUserAgent;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;
import org.apache.xmlgraphics.util.MimeConstants;
import org.w3c.dom.Document;

/**
 * AFP XML handler for SVG. Uses Apache Batik for SVG processing. This handler
 * handles XML for foreign objects and delegates to AFPGraphics2DAdapter.
 *
 * @see AFPGraphics2DAdapter
 */
public class AFPSVGHandler extends AbstractGenericSVGHandler {

    private boolean paintAsBitmap = false;

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @Override
    public void handleXML(final RendererContext context, final Document doc,
            final String ns) throws IOException {
        if (SVGDOMImplementation.SVG_NAMESPACE_URI.equals(ns)) {
            renderSVGDocument(context, doc);
        }
    }

    /**
     * Render the SVG document.
     *
     * @param rendererContext
     *            the renderer context
     * @param doc
     *            the SVG document
     * @throws IOException
     *             In case of an I/O error while painting the image
     */
    @Override
    protected void renderSVGDocument(final RendererContext rendererContext,
            final Document doc) throws IOException {

        final AFPRendererContext afpRendererContext = (AFPRendererContext) rendererContext;
        final AFPInfo afpInfo = afpRendererContext.getInfo();

        this.paintAsBitmap = afpInfo.paintAsBitmap();

        final FOUserAgent userAgent = rendererContext.getUserAgent();

        // fallback paint as bitmap
        final String uri = getDocumentURI(doc);
        if (this.paintAsBitmap) {
            try {
                super.renderSVGDocument(rendererContext, doc);
            } catch (final IOException ioe) {
                final SVGEventProducer eventProducer = SVGEventProducer.Provider
                        .get(userAgent.getEventBroadcaster());
                eventProducer.svgRenderingError(this, ioe, uri);
            }
            return;
        }

        // Create a new AFPGraphics2D
        final boolean textAsShapes = afpInfo.strokeText();
        final AFPGraphics2D g2d = afpInfo.createGraphics2D(textAsShapes);

        final AFPPaintingState paintingState = g2d.getPaintingState();
        paintingState.setImageUri(uri);

        // Create an AFPBridgeContext
        final BridgeContext bridgeContext = createBridgeContext(userAgent, g2d);

        // Cloning SVG DOM as Batik attaches non-thread-safe facilities (like
        // the CSS engine)
        // to it.
        final Document clonedDoc = BatikUtil.cloneSVGDocument(doc);

        // Build the SVG DOM and provide the painter with it
        final GraphicsNode root = buildGraphicsNode(userAgent, bridgeContext,
                clonedDoc);

        // Create Graphics2DImagePainter
        final RendererContextWrapper wrappedContext = RendererContext
                .wrapRendererContext(rendererContext);
        final Dimension imageSize = getImageSize(wrappedContext);
        final Graphics2DImagePainter painter = createGraphics2DImagePainter(
                bridgeContext, root, imageSize);

        // Create AFPObjectAreaInfo
        final RendererContextWrapper rctx = RendererContext
                .wrapRendererContext(rendererContext);
        final int x = rctx.getCurrentXPosition();
        final int y = rctx.getCurrentYPosition();
        final int width = afpInfo.getWidth();
        final int height = afpInfo.getHeight();
        final int resolution = afpInfo.getResolution();

        paintingState.save(); // save

        final AFPObjectAreaInfo objectAreaInfo = createObjectAreaInfo(
                paintingState, x, y, width, height, resolution);

        // Create AFPGraphicsObjectInfo
        final AFPResourceInfo resourceInfo = afpInfo.getResourceInfo();
        final AFPGraphicsObjectInfo graphicsObjectInfo = createGraphicsObjectInfo(
                paintingState, painter, userAgent, resourceInfo, g2d);
        graphicsObjectInfo.setObjectAreaInfo(objectAreaInfo);

        // Create the GOCA GraphicsObject in the DataStream
        final AFPResourceManager resourceManager = afpInfo.getResourceManager();
        resourceManager.createObject(graphicsObjectInfo);

        paintingState.restore(); // resume
    }

    private AFPObjectAreaInfo createObjectAreaInfo(
            final AFPPaintingState paintingState, final int x, final int y,
            final int width, final int height, final int resolution) {
        // set the data object parameters

        final AffineTransform at = paintingState.getData().getTransform();
        at.translate(x, y);
        final AFPUnitConverter unitConv = paintingState.getUnitConverter();

        final int rotation = paintingState.getRotation();
        final int objX = (int) Math.round(at.getTranslateX());
        final int objY = (int) Math.round(at.getTranslateY());
        final int objWidth = Math.round(unitConv.mpt2units(width));
        final int objHeight = Math.round(unitConv.mpt2units(height));
        final AFPObjectAreaInfo objectAreaInfo = new AFPObjectAreaInfo(objX,
                objY, objWidth, objHeight, resolution, rotation);
        return objectAreaInfo;
    }

    private AFPGraphicsObjectInfo createGraphicsObjectInfo(
            final AFPPaintingState paintingState,
            final Graphics2DImagePainter painter, final FOUserAgent userAgent,
            final AFPResourceInfo resourceInfo, final AFPGraphics2D g2d) {
        final AFPGraphicsObjectInfo graphicsObjectInfo = new AFPGraphicsObjectInfo();

        final String uri = paintingState.getImageUri();
        graphicsObjectInfo.setUri(uri);

        graphicsObjectInfo.setMimeType(MimeConstants.MIME_AFP_GOCA);

        graphicsObjectInfo.setResourceInfo(resourceInfo);

        graphicsObjectInfo.setPainter(painter);

        // Set the afp graphics 2d implementation
        graphicsObjectInfo.setGraphics2D(g2d);

        return graphicsObjectInfo;
    }

    /**
     * @param userAgent
     *            a user agent instance
     * @param g2d
     *            a graphics context
     * @return a bridge context
     */
    public static BridgeContext createBridgeContext(
            final FOUserAgent userAgent, final AFPGraphics2D g2d) {
        final ImageManager imageManager = userAgent.getFactory()
                .getImageManager();

        final SVGUserAgent svgUserAgent = new SVGUserAgent(userAgent,
                new AffineTransform());

        final ImageSessionContext imageSessionContext = userAgent
                .getImageSessionContext();

        final FontInfo fontInfo = g2d.getFontInfo();
        return new AFPBridgeContext(svgUserAgent, fontInfo, imageManager,
                imageSessionContext, new AffineTransform(), g2d);
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsRenderer(final Renderer renderer) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected void updateRendererContext(final RendererContext context) {
        // Work around a problem in Batik: Gradients cannot be done in
        // ColorSpace.CS_GRAY
        context.setProperty(AFPRendererContextConstants.AFP_GRAYSCALE, false);
    }

    private Graphics2DImagePainter createGraphics2DImagePainter(
            final BridgeContext ctx, final GraphicsNode root,
            final Dimension imageSize) {
        Graphics2DImagePainter painter = null;
        if (paintAsBitmap()) {
            // paint as IOCA Image
            painter = super.createGraphics2DImagePainter(root, ctx, imageSize);
        } else {
            // paint as GOCA Graphics
            painter = new Graphics2DImagePainterImpl(root, ctx, imageSize);
        }
        return painter;
    }

    /**
     * Returns true if the SVG is to be painted as a bitmap
     *
     * @return true if the SVG is to be painted as a bitmap
     */
    private boolean paintAsBitmap() {
        return this.paintAsBitmap;
    }

}
