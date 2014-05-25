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

/* $Id: ImageConverterSVG2G2D.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.image.loader.batik;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.fop.svg.SimpleSVGUserAgent;
import org.apache.xmlgraphics.image.GraphicsConstants;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageProcessingHints;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.XMLNamespaceEnabledImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.AbstractImageConverter;
import org.apache.xmlgraphics.image.loader.impl.ImageGraphics2D;
import org.apache.xmlgraphics.image.loader.impl.ImageXMLDOM;
import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;
import org.apache.xmlgraphics.util.UnitConv;
import org.w3c.dom.Document;

/**
 * This ImageConverter converts SVG images to Java2D.
 * <p>
 * Note: The target flavor is "generic" Java2D. No Batik-specific bridges are
 * hooked into the conversion process. Specialized renderers may want to provide
 * specialized adapters to profit from target-format features (for example with
 * PDF or PS). This converter is mainly for formats which only support bitmap
 * images or rudimentary Java2D support.
 */
@Slf4j
public class ImageConverterSVG2G2D extends AbstractImageConverter {

    /** {@inheritDoc} */
    @Override
    public Image convert(final Image src, final Map hints)
            throws ImageException {
        checkSourceFlavor(src);
        final ImageXMLDOM svg = (ImageXMLDOM) src;
        if (!SVGDOMImplementation.SVG_NAMESPACE_URI.equals(svg
                .getRootNamespace())) {
            throw new IllegalArgumentException(
                    "XML DOM is not in the SVG namespace: "
                            + svg.getRootNamespace());
        }

        // Prepare
        float pxToMillimeter = UnitConv.IN2MM / GraphicsConstants.DEFAULT_DPI;
        final Number ptm = (Number) hints
                .get(ImageProcessingHints.SOURCE_RESOLUTION);
        if (ptm != null) {
            pxToMillimeter = (float) (UnitConv.IN2MM / ptm.doubleValue());
        }
        final UserAgent ua = createBatikUserAgent(pxToMillimeter);
        final GVTBuilder builder = new GVTBuilder();

        final ImageManager imageManager = (ImageManager) hints
                .get(ImageProcessingHints.IMAGE_MANAGER);
        final ImageSessionContext sessionContext = (ImageSessionContext) hints
                .get(ImageProcessingHints.IMAGE_SESSION_CONTEXT);

        final boolean useEnhancedBridgeContext = imageManager != null
                && sessionContext != null;
        final BridgeContext ctx = useEnhancedBridgeContext ? new GenericFOPBridgeContext(
                ua, null, imageManager, sessionContext) : new BridgeContext(ua);

        final Document doc = svg.getDocument();
        // Cloning SVG DOM as Batik attaches non-thread-safe facilities (like
                // the CSS engine)
        // to it.
        final Document clonedDoc = BatikUtil.cloneSVGDocument(doc);

        // Build the GVT tree
        final GraphicsNode root;
        try {
            root = builder.build(ctx, clonedDoc);
        } catch (final Exception e) {
            throw new ImageException(
                            "GVT tree could not be built for SVG graphic", e);
        }

        // Create the painter
        final int width = svg.getSize().getWidthMpt();
        final int height = svg.getSize().getHeightMpt();
        final Dimension imageSize = new Dimension(width, height);
        final Graphics2DImagePainter painter = createPainter(ctx, root,
                        imageSize);

        // Create g2d image
        final ImageInfo imageInfo = src.getInfo();
        final ImageGraphics2D g2dImage = new ImageGraphics2D(imageInfo, painter);
        return g2dImage;
    }

    /**
     * Creates a user agent for Batik. Override to provide your own user agent.
     *
     * @param pxToMillimeter
     *            the source resolution (in px per millimeter)
     * @return the newly created user agent
     */
    protected SimpleSVGUserAgent createBatikUserAgent(final float pxToMillimeter) {
        return new SimpleSVGUserAgent(pxToMillimeter, new AffineTransform()) {

            /** {@inheritDoc} */
            @Override
            public void displayMessage(final String message) {
                // TODO Refine and pipe through to caller
                log.info(message);
            }

            /** {@inheritDoc} */
            @Override
            public void displayError(final Exception e) {
                log.error("Error converting SVG to a Java2D graphic", e);
            }

            /** {@inheritDoc} */
            @Override
            public void displayError(final String message) {
                log.error(message);
            }

        };
    }

    /**
     * Creates a Graphics 2D image painter
     *
     * @param ctx
     *            the bridge context
     * @param root
     *            the graphics node root
     * @param imageSize
     *            the image size
     * @return the newly created graphics 2d image painter
     */
    protected Graphics2DImagePainter createPainter(final BridgeContext ctx,
            final GraphicsNode root, final Dimension imageSize) {
        return new Graphics2DImagePainterImpl(root, ctx, imageSize);
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getSourceFlavor() {
        return XMLNamespaceEnabledImageFlavor.SVG_DOM;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getTargetFlavor() {
        return ImageFlavor.GRAPHICS2D;
    }

}
