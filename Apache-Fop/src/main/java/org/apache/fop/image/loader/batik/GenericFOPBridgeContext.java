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

/* $Id: GenericFOPBridgeContext.java 964316 2010-07-15 06:50:21Z jeremias $ */

package org.apache.fop.image.loader.batik;

import java.awt.geom.AffineTransform;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.dom.svg.SVGOMDocument;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.svg.AbstractFOPBridgeContext;
import org.apache.fop.svg.SVGUserAgent;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;

/**
 * BridgeContext which registers the custom bridges for Java2D output.
 */
class GenericFOPBridgeContext extends AbstractFOPBridgeContext {

    /**
     * Constructs a new bridge context.
     * 
     * @param userAgent
     *            the user agent
     * @param documentLoader
     *            the Document Loader to use for referenced documents.
     * @param fontInfo
     *            the font list for the text painter, may be null in which case
     *            text is painted as shapes
     * @param imageManager
     *            an image manager
     * @param imageSessionContext
     *            an image session context
     * @param linkTransform
     *            AffineTransform to properly place links, may be null
     */
    public GenericFOPBridgeContext(final UserAgent userAgent,
            final DocumentLoader documentLoader, final FontInfo fontInfo,
            final ImageManager imageManager,
            final ImageSessionContext imageSessionContext,
            final AffineTransform linkTransform) {
        super(userAgent, documentLoader, fontInfo, imageManager,
                imageSessionContext, linkTransform);
    }

    /**
     * Constructs a new bridge context.
     * 
     * @param userAgent
     *            the user agent
     * @param fontInfo
     *            the font list for the text painter, may be null in which case
     *            text is painted as shapes
     * @param imageManager
     *            an image manager
     * @param imageSessionContext
     *            an image session context
     */
    public GenericFOPBridgeContext(final UserAgent userAgent,
            final FontInfo fontInfo, final ImageManager imageManager,
            final ImageSessionContext imageSessionContext) {
        super(userAgent, fontInfo, imageManager, imageSessionContext);
    }

    /**
     * Constructs a new bridge context.
     * 
     * @param userAgent
     *            the user agent
     * @param fontInfo
     *            the font list for the text painter, may be null in which case
     *            text is painted as shapes
     * @param imageManager
     *            an image manager
     * @param imageSessionContext
     *            an image session context
     * @param linkTransform
     *            AffineTransform to properly place links, may be null
     */
    public GenericFOPBridgeContext(final SVGUserAgent userAgent,
            final FontInfo fontInfo, final ImageManager imageManager,
            final ImageSessionContext imageSessionContext,
            final AffineTransform linkTransform) {
        super(userAgent, fontInfo, imageManager, imageSessionContext,
                linkTransform);
    }

    /** {@inheritDoc} */
    @Override
    public void registerSVGBridges() {
        super.registerSVGBridges();

        // This makes Batik load images via FOP and the XGC image loading
        // framework
        putBridge(new GenericFOPImageElementBridge());
    }

    /** {@inheritDoc} */
    @Override
    public BridgeContext createBridgeContext(final SVGOMDocument doc) {
        return createBridgeContext();
    }

    /** {@inheritDoc} */
    @Override
    public BridgeContext createBridgeContext() {
        return new GenericFOPBridgeContext(getUserAgent(), getDocumentLoader(),
                this.fontInfo, getImageManager(), getImageSessionContext(),
                this.linkTransform);
    }

}
