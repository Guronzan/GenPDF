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

/* $Id: PNGRenderer.java 1237610 2012-01-30 11:46:13Z mehdi $ */

package org.apache.fop.render.bitmap;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.area.PageViewport;
import org.apache.fop.render.java2d.Java2DRenderer;
import org.apache.xmlgraphics.image.writer.ImageWriter;
import org.apache.xmlgraphics.image.writer.ImageWriterParams;
import org.apache.xmlgraphics.image.writer.ImageWriterRegistry;

/**
 * PNG Renderer This class actually does not render itself, instead it extends
 * <code>org.apache.fop.render.java2D.Java2DRenderer</code> and just encode
 * rendering results into PNG format using Batik's image codec
 */
@Slf4j
public class PNGRenderer extends Java2DRenderer {

    /**
     * @param userAgent
     *            the user agent that contains configuration details. This
     *            cannot be null.
     */
    public PNGRenderer(final FOUserAgent userAgent) {
        super(userAgent);
    }

    /** The MIME type for png-Rendering */
    public static final String MIME_TYPE = MimeConstants.MIME_PNG;

    /** The file extension expected for PNG files */
    private static final String PNG_FILE_EXTENSION = "png";

    /** The OutputStream for the first Image */
    private OutputStream firstOutputStream;

    /** Helper class for generating multiple files */
    private MultiFileRenderingUtil multiFileUtil;

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public void startRenderer(final OutputStream outputStream)
            throws IOException {
        log.info("rendering areas to PNG");
        this.multiFileUtil = new MultiFileRenderingUtil(PNG_FILE_EXTENSION,
                getUserAgent().getOutputFile());
        this.firstOutputStream = outputStream;
    }

    /** {@inheritDoc} */
    @Override
    public void stopRenderer() throws IOException {

        super.stopRenderer();

        for (int i = 0; i < this.pageViewportList.size(); i++) {

            final OutputStream os = getCurrentOutputStream(i);
            if (os == null) {
                final BitmapRendererEventProducer eventProducer = BitmapRendererEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.stoppingAfterFirstPageNoFilename(this);
                break;
            }
            try {
                // Do the rendering: get the image for this page
                final PageViewport pv = (PageViewport) this.pageViewportList
                        .get(i);
                final RenderedImage image = getPageImage(pv);

                // Encode this image
                if (log.isDebugEnabled()) {
                    log.debug("Encoding page " + (i + 1));
                }
                writeImage(os, image);
            } finally {
                // Only close self-created OutputStreams
                if (os != this.firstOutputStream) {
                    IOUtils.closeQuietly(os);
                }
            }
        }
    }

    private void writeImage(final OutputStream os, final RenderedImage image)
            throws IOException {
        final ImageWriterParams params = new ImageWriterParams();
        params.setResolution(Math.round(this.userAgent.getTargetResolution()));

        // Encode PNG image
        final ImageWriter writer = ImageWriterRegistry.getInstance()
                .getWriterFor(getMimeType());
        if (writer == null) {
            final BitmapRendererEventProducer eventProducer = BitmapRendererEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.noImageWriterFound(this, getMimeType());
        }
        if (log.isDebugEnabled()) {
            log.debug("Writing image using " + writer.getClass().getName());
        }
        writer.writeImage(image, os, params);
    }

    /**
     * Returns the OutputStream corresponding to this page
     *
     * @param pageNumber
     *            0-based page number
     * @return the corresponding OutputStream
     * @throws IOException
     *             In case of an I/O error
     */
    protected OutputStream getCurrentOutputStream(final int pageNumber)
            throws IOException {

        if (pageNumber == 0) {
            return this.firstOutputStream;
        } else {
            return this.multiFileUtil.createOutputStream(pageNumber);
        }

    }
}
