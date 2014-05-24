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

/* $Id: PDFImageHandlerSVG.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.render.pdf;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.SVGConstants;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.image.loader.batik.BatikImageFlavors;
import org.apache.fop.image.loader.batik.BatikUtil;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.ImageHandlerUtil;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.render.pdf.PDFLogicalStructureHandler.MarkedContentInfo;
import org.apache.fop.svg.PDFAElementBridge;
import org.apache.fop.svg.PDFBridgeContext;
import org.apache.fop.svg.PDFGraphics2D;
import org.apache.fop.svg.SVGEventProducer;
import org.apache.fop.svg.SVGUserAgent;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageXMLDOM;
import org.apache.xmlgraphics.util.UnitConv;
import org.w3c.dom.Document;

/**
 * Image Handler implementation which handles SVG images.
 */
@Slf4j
public class PDFImageHandlerSVG implements ImageHandler {

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, // CSOK:
            // MethodLength
            final Image image, final Rectangle pos) throws IOException {
        final PDFRenderingContext pdfContext = (PDFRenderingContext) context;
        final PDFContentGenerator generator = pdfContext.getGenerator();
        final ImageXMLDOM imageSVG = (ImageXMLDOM) image;

        final FOUserAgent userAgent = context.getUserAgent();
        final float deviceResolution = userAgent.getTargetResolution();
        if (log.isDebugEnabled()) {
            log.debug("Generating SVG at " + deviceResolution + "dpi.");
        }

        final float uaResolution = userAgent.getSourceResolution();
        final SVGUserAgent ua = new SVGUserAgent(userAgent,
                new AffineTransform());

        GVTBuilder builder = new GVTBuilder();

        // Controls whether text painted by Batik is generated using text or
        // path operations
        final boolean strokeText = false;
        // TODO connect with configuration elsewhere.

        final BridgeContext ctx = new PDFBridgeContext(ua, strokeText ? null
                : pdfContext.getFontInfo(), userAgent.getFactory()
                .getImageManager(), userAgent.getImageSessionContext(),
                new AffineTransform());

        // Cloning SVG DOM as Batik attaches non-thread-safe facilities (like
        // the CSS engine)
        // to it.
        final Document clonedDoc = BatikUtil.cloneSVGDocument(imageSVG
                .getDocument());

        GraphicsNode root;
        try {
            root = builder.build(ctx, clonedDoc);
            builder = null;
        } catch (final Exception e) {
            final SVGEventProducer eventProducer = SVGEventProducer.Provider
                    .get(context.getUserAgent().getEventBroadcaster());
            eventProducer
                    .svgNotBuilt(this, e, image.getInfo().getOriginalURI());
            return;
        }
        // get the 'width' and 'height' attributes of the SVG document
        final float w = image.getSize().getWidthMpt();
        final float h = image.getSize().getHeightMpt();

        final float sx = pos.width / w;
        final float sy = pos.height / h;

        // Scaling and translation for the bounding box of the image
        final AffineTransform scaling = new AffineTransform(sx, 0, 0, sy,
                pos.x / 1000f, pos.y / 1000f);
        final double sourceScale = UnitConv.IN2PT / uaResolution;
        scaling.scale(sourceScale, sourceScale);

        // Scale for higher resolution on-the-fly images from Batik
        final AffineTransform resolutionScaling = new AffineTransform();
        final double targetScale = uaResolution / deviceResolution;
        resolutionScaling.scale(targetScale, targetScale);
        resolutionScaling.scale(1.0 / sx, 1.0 / sy);

        // Transformation matrix that establishes the local coordinate system
        // for the SVG graphic
        // in relation to the current coordinate system
        final AffineTransform imageTransform = new AffineTransform();
        imageTransform.concatenate(scaling);
        imageTransform.concatenate(resolutionScaling);

        if (log.isTraceEnabled()) {
            log.trace("nat size: " + w + "/" + h);
            log.trace("req size: " + pos.width + "/" + pos.height);
            log.trace("source res: " + uaResolution + ", targetRes: "
                    + deviceResolution + " --> target scaling: " + targetScale);
            log.trace(image.getSize().toString());
            log.trace("sx: " + sx + ", sy: " + sy);
            log.trace("scaling: " + scaling);
            log.trace("resolution scaling: " + resolutionScaling);
            log.trace("image transform: " + resolutionScaling);
        }

        /*
         * Clip to the svg area. Note: To have the svg overlay (under) a text
         * area then use an fo:block-container
         */
        if (log.isTraceEnabled()) {
            generator.comment("SVG setup");
        }
        generator.saveGraphicsState();
        if (context.getUserAgent().isAccessibilityEnabled()) {
            final MarkedContentInfo mci = pdfContext.getMarkedContentInfo();
            generator.beginMarkedContentSequence(mci.tag, mci.mcid);
        }
        generator.updateColor(Color.black, false, null);
        generator.updateColor(Color.black, true, null);

        if (!scaling.isIdentity()) {
            if (log.isTraceEnabled()) {
                generator.comment("viewbox");
            }
            generator.add(CTMHelper.toPDFString(scaling, false) + " cm\n");
        }

        // SVGSVGElement svg = ((SVGDocument)doc).getRootElement();

        final PDFGraphics2D graphics = new PDFGraphics2D(true,
                pdfContext.getFontInfo(), generator.getDocument(),
                generator.getResourceContext(), pdfContext.getPage()
                        .referencePDF(), "", 0);
        graphics.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());

        if (!resolutionScaling.isIdentity()) {
            if (log.isTraceEnabled()) {
                generator.comment("resolution scaling for " + uaResolution
                        + " -> " + deviceResolution);
            }
            generator.add(CTMHelper.toPDFString(resolutionScaling, false)
                    + " cm\n");
            graphics.scale(1.0 / resolutionScaling.getScaleX(),
                    1.0 / resolutionScaling.getScaleY());
        }

        if (log.isTraceEnabled()) {
            generator.comment("SVG start");
        }

        // Save state and update coordinate system for the SVG image
        generator.getState().save();
        generator.getState().concatenate(imageTransform);

        // Now that we have the complete transformation matrix for the image, we
        // can update the
        // transformation matrix for the AElementBridge.
        final PDFAElementBridge aBridge = (PDFAElementBridge) ctx.getBridge(
                SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_A_TAG);
        aBridge.getCurrentTransform().setTransform(
                generator.getState().getTransform());

        graphics.setPaintingState(generator.getState());
        graphics.setOutputStream(generator.getOutputStream());
        try {
            root.paint(graphics);
            ctx.dispose();
            generator.add(graphics.getString());
        } catch (final Exception e) {
            final SVGEventProducer eventProducer = SVGEventProducer.Provider
                    .get(context.getUserAgent().getEventBroadcaster());
            eventProducer.svgRenderingError(this, e, image.getInfo()
                    .getOriginalURI());
        }
        generator.getState().restore();
        if (context.getUserAgent().isAccessibilityEnabled()) {
            generator.restoreGraphicsStateAccess();
        } else {
            generator.restoreGraphicsState();
        }
        if (log.isTraceEnabled()) {
            generator.comment("SVG end");
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
        return new ImageFlavor[] { BatikImageFlavors.SVG_DOM };
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        final boolean supported = (image == null || image instanceof ImageXMLDOM
                && image.getFlavor().isCompatible(BatikImageFlavors.SVG_DOM))
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
