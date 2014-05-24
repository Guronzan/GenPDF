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

/* $Id: PrintRendererConfigurator.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.render;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.fonts.CustomFontCollection;
import org.apache.fop.fonts.EmbedFontInfo;
import org.apache.fop.fonts.FontCollection;
import org.apache.fop.fonts.FontEventAdapter;
import org.apache.fop.fonts.FontEventListener;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontInfoConfigurator;
import org.apache.fop.fonts.FontManager;
import org.apache.fop.fonts.FontResolver;
import org.apache.fop.fonts.base14.Base14FontCollection;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFDocumentHandlerConfigurator;

/**
 * Base Print renderer configurator (mostly handles font configuration)
 */
@Slf4j
public class PrintRendererConfigurator extends AbstractRendererConfigurator
implements RendererConfigurator, IFDocumentHandlerConfigurator {

    /**
     * Default constructor
     *
     * @param userAgent
     *            user agent
     */
    public PrintRendererConfigurator(final FOUserAgent userAgent) {
        super(userAgent);
    }

    /**
     * Builds a list of EmbedFontInfo objects for use with the setup() method.
     *
     * @param renderer
     *            print renderer
     * @throws FOPException
     *             if something's wrong with the config data
     */
    @Override
    public void configure(final Renderer renderer) throws FOPException {
        final Configuration cfg = getRendererConfig(renderer);
        if (cfg == null) {
            log.trace("no configuration found for " + renderer);
            return;
        }

        final PrintRenderer printRenderer = (PrintRenderer) renderer;
        final FontResolver fontResolver = printRenderer.getFontResolver();

        final FontEventListener listener = new FontEventAdapter(renderer
                .getUserAgent().getEventBroadcaster());
        final List<EmbedFontInfo> embedFontInfoList = buildFontList(cfg,
                fontResolver, listener);
        printRenderer.addFontList(embedFontInfoList);
    }

    /**
     * Builds the font list from configuration.
     *
     * @param cfg
     *            the configuration object
     * @param fontResolver
     *            a font resolver
     * @param listener
     *            the font event listener
     * @return the list of {@link EmbedFontInfo} objects
     * @throws FOPException
     *             if an error occurs while processing the configuration
     */
    protected List<EmbedFontInfo> buildFontList(final Configuration cfg,
            FontResolver fontResolver, final FontEventListener listener)
                    throws FOPException {
        final FopFactory factory = this.userAgent.getFactory();
        final FontManager fontManager = factory.getFontManager();
        if (fontResolver == null) {
            // Ensure that we have minimal font resolution capabilities
            fontResolver = FontManager.createMinimalFontResolver(this.userAgent
                    .isComplexScriptFeaturesEnabled());
        }

        final boolean strict = factory.validateUserConfigStrictly();

        // Read font configuration
        final FontInfoConfigurator fontInfoConfigurator = new FontInfoConfigurator(
                cfg, fontManager, fontResolver, listener, strict);
        final List<EmbedFontInfo> fontInfoList = new ArrayList<EmbedFontInfo>();
        fontInfoConfigurator.configure(fontInfoList);
        return fontInfoList;
    }

    // ---=== IFDocumentHandler configuration ===---

    /** {@inheritDoc} */
    @Override
    public void configure(final IFDocumentHandler documentHandler)
            throws FOPException {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void setupFontInfo(final IFDocumentHandler documentHandler,
            final FontInfo fontInfo) throws FOPException {
        final FontManager fontManager = this.userAgent.getFactory()
                .getFontManager();
        final List<FontCollection> fontCollections = new ArrayList<FontCollection>();
        fontCollections.add(new Base14FontCollection(fontManager
                .isBase14KerningEnabled()));

        final Configuration cfg = super.getRendererConfig(documentHandler
                .getMimeType());
        if (cfg != null) {
            final FontResolver fontResolver = new DefaultFontResolver(
                    this.userAgent);
            final FontEventListener listener = new FontEventAdapter(
                    this.userAgent.getEventBroadcaster());
            final List<EmbedFontInfo> fontList = buildFontList(cfg,
                    fontResolver, listener);
            fontCollections.add(new CustomFontCollection(fontResolver,
                    fontList, this.userAgent.isComplexScriptFeaturesEnabled()));
        }

        fontManager.setup(fontInfo, fontCollections
                .toArray(new FontCollection[fontCollections.size()]));
        documentHandler.setFontInfo(fontInfo);
    }
}
