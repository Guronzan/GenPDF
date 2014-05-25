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

/* $Id: TIFFRendererConfigurator.java 820637 2009-10-01 12:56:26Z jeremias $ */

package org.apache.fop.render.bitmap;

import java.awt.image.BufferedImage;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.intermediate.IFDocumentHandler;

/**
 * TIFF Renderer configurator
 */
@Slf4j
public class TIFFRendererConfigurator extends BitmapRendererConfigurator {

    /**
     * Default constructor
     *
     * @param userAgent
     *            user agent
     */
    public TIFFRendererConfigurator(final FOUserAgent userAgent) {
        super(userAgent);
    }

    /**
     * Configure the TIFF renderer. Get the configuration to be used for
     * compression
     *
     * @param renderer
     *            tiff renderer
     * @throws FOPException
     *             fop exception {@inheritDoc}
     */
    @Override
    public void configure(final Renderer renderer) throws FOPException {
        final Configuration cfg = super.getRendererConfig(renderer);
        if (cfg != null) {
            final TIFFRenderer tiffRenderer = (TIFFRenderer) renderer;
            // set compression
            final String name = cfg.getChild("compression").getValue(
                    TIFFConstants.COMPRESSION_PACKBITS);
            // Some compression formats need a special image format:
            tiffRenderer.setBufferedImageType(getBufferedImageTypeFor(name));
            if (!"NONE".equalsIgnoreCase(name)) {
                tiffRenderer.getWriterParams().setCompressionMethod(name);
            }
            if (log.isInfoEnabled()) {
                log.info("TIFF compression set to " + name);
            }
        }
        super.configure(renderer);
    }

    /**
     * Determines the type value for the BufferedImage to be produced for
     * rendering the bitmap image.
     *
     * @param compressionName
     *            the compression name
     * @return a value from the {@link BufferedImage}.TYPE_* constants
     */
    private int getBufferedImageTypeFor(final String compressionName) {
        if (compressionName
                .equalsIgnoreCase(TIFFConstants.COMPRESSION_CCITT_T6)) {
            return BufferedImage.TYPE_BYTE_BINARY;
        } else if (compressionName
                .equalsIgnoreCase(TIFFConstants.COMPRESSION_CCITT_T4)) {
            return BufferedImage.TYPE_BYTE_BINARY;
        } else {
            return BufferedImage.TYPE_INT_ARGB;
        }
    }

    // ---=== IFDocumentHandler configuration ===---

    /** {@inheritDoc} */
    @Override
    public void configure(final IFDocumentHandler documentHandler)
            throws FOPException {
        super.configure(documentHandler);
        final Configuration cfg = super.getRendererConfig(documentHandler
                .getMimeType());
        if (cfg != null) {
            final TIFFDocumentHandler tiffHandler = (TIFFDocumentHandler) documentHandler;
            final BitmapRenderingSettings settings = tiffHandler.getSettings();
            // set compression
            final String name = cfg.getChild("compression").getValue(
                    TIFFConstants.COMPRESSION_PACKBITS);
            // Some compression formats need a special image format:
            settings.setBufferedImageType(getBufferedImageTypeFor(name));
            if (!"NONE".equalsIgnoreCase(name)) {
                settings.getWriterParams().setCompressionMethod(name);
            }
            if (log.isInfoEnabled()) {
                log.info("TIFF compression set to " + name);
            }
        }
    }

}
