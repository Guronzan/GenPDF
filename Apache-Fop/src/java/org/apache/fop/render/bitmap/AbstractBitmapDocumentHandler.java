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

/* $Id: AbstractBitmapDocumentHandler.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.bitmap;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.FopFactoryConfigurator;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.render.intermediate.AbstractBinaryWritingIFDocumentHandler;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandlerConfigurator;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFPainter;
import org.apache.fop.render.java2d.Java2DPainter;
import org.apache.fop.render.java2d.Java2DUtil;
import org.apache.xmlgraphics.image.writer.ImageWriter;
import org.apache.xmlgraphics.image.writer.ImageWriterRegistry;
import org.apache.xmlgraphics.image.writer.MultiImageWriter;

/**
 * Abstract {@link org.apache.fop.render.intermediate.IFDocumentHandler}
 * implementation for producing bitmap images.
 */
@Slf4j
public abstract class AbstractBitmapDocumentHandler extends
AbstractBinaryWritingIFDocumentHandler {

    /**
     * Rendering Options key for the controlling the required bitmap size to
     * create. This is used to create thumbnails, for example. If used, the
     * target resolution is ignored. Value type: java.awt.Dimension (size in
     * pixels)
     */
    public static final String TARGET_BITMAP_SIZE = "target-bitmap-size";

    private ImageWriter imageWriter;
    private MultiImageWriter multiImageWriter;

    /** Helper class for generating multiple files */
    private MultiFileRenderingUtil multiFileUtil;

    private int pageCount;
    private Dimension currentPageDimensions;
    private BufferedImage currentImage;

    private final BitmapRenderingSettings bitmapSettings = new BitmapRenderingSettings();

    private final double scaleFactor = 1.0;
    private Dimension targetBitmapSize;

    /**
     * Default constructor.
     */
    public AbstractBitmapDocumentHandler() {
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsPagesOutOfOrder() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public abstract String getMimeType();

    /**
     * Returns the default file extension for the supported image type.
     *
     * @return the default file extension (ex. "png")
     */
    public abstract String getDefaultExtension();

    /** {@inheritDoc} */
    @Override
    public void setContext(final IFContext context) {
        super.setContext(context);

        // Set target resolution
        final int dpi = Math
                .round(context.getUserAgent().getTargetResolution());
        getSettings().getWriterParams().setResolution(dpi);

        final Map renderingOptions = getUserAgent().getRendererOptions();
        setTargetBitmapSize((Dimension) renderingOptions
                .get(TARGET_BITMAP_SIZE));
    }

    /** {@inheritDoc} */
    @Override
    public abstract IFDocumentHandlerConfigurator getConfigurator();

    /**
     * Returns the settings for bitmap rendering.
     *
     * @return the settings object
     */
    public BitmapRenderingSettings getSettings() {
        return this.bitmapSettings;
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultFontInfo(final FontInfo fontInfo) {
        final FontInfo fi = Java2DUtil.buildDefaultJava2DBasedFontInfo(
                fontInfo, getUserAgent());
        setFontInfo(fi);
    }

    /**
     * Sets the target bitmap size (in pixels) of the bitmap that should be
     * produced. Normally, the bitmap size is calculated automatically based on
     * the page size and the target resolution. But for example, if you want to
     * create thumbnails or small preview bitmaps from pages it is more
     * practical (and efficient) to set the required bitmap size.
     *
     * @param size
     *            the target bitmap size (in pixels)
     */
    public void setTargetBitmapSize(final Dimension size) {
        this.targetBitmapSize = size;
    }

    // ----------------------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws IFException {
        super.startDocument();
        try {
            // Creates writer
            this.imageWriter = ImageWriterRegistry.getInstance().getWriterFor(
                    getMimeType());
            if (this.imageWriter == null) {
                final BitmapRendererEventProducer eventProducer = BitmapRendererEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.noImageWriterFound(this, getMimeType());
            }
            if (this.imageWriter.supportsMultiImageWriter()) {
                this.multiImageWriter = this.imageWriter
                        .createMultiImageWriter(this.outputStream);
            } else {
                this.multiFileUtil = new MultiFileRenderingUtil(
                        getDefaultExtension(), getUserAgent().getOutputFile());
            }
            this.pageCount = 0;
        } catch (final IOException e) {
            throw new IFException("I/O error in startDocument()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endDocumentHeader() throws IFException {
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws IFException {
        try {
            if (this.multiImageWriter != null) {
                this.multiImageWriter.close();
            }
            this.multiImageWriter = null;
            this.imageWriter = null;
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endDocument()", ioe);
        }
        super.endDocument();
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final String id) throws IFException {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void endPageSequence() throws IFException {
        // nop
    }

    /** {@inheritDoc} */
    @Override
    public void startPage(final int index, final String name,
            final String pageMasterName, final Dimension size)
                    throws IFException {
        this.pageCount++;
        this.currentPageDimensions = new Dimension(size);
    }

    /** {@inheritDoc} */
    @Override
    public IFPainter startPageContent() throws IFException {
        int bitmapWidth;
        int bitmapHeight;
        double scale;
        Point2D offset = null;
        if (this.targetBitmapSize != null) {
            // Fit the generated page proportionally into the given rectangle
            // (in pixels)
            final double scale2w = 1000 * this.targetBitmapSize.width
                    / this.currentPageDimensions.getWidth();
            final double scale2h = 1000 * this.targetBitmapSize.height
                    / this.currentPageDimensions.getHeight();
            bitmapWidth = this.targetBitmapSize.width;
            bitmapHeight = this.targetBitmapSize.height;

            // Centering the page in the given bitmap
            offset = new Point2D.Double();
            if (scale2w < scale2h) {
                scale = scale2w;
                final double h = this.currentPageDimensions.height * scale
                        / 1000;
                offset.setLocation(0, (bitmapHeight - h) / 2.0);
            } else {
                scale = scale2h;
                final double w = this.currentPageDimensions.width * scale
                        / 1000;
                offset.setLocation((bitmapWidth - w) / 2.0, 0);
            }
        } else {
            // Normal case: just scale according to the target resolution
            scale = this.scaleFactor * getUserAgent().getTargetResolution()
                    / FopFactoryConfigurator.DEFAULT_TARGET_RESOLUTION;
            bitmapWidth = (int) (this.currentPageDimensions.width * scale
                    / 1000f + 0.5f);
            bitmapHeight = (int) (this.currentPageDimensions.height * scale
                    / 1000f + 0.5f);
        }

        // Set up bitmap to paint on
        this.currentImage = createBufferedImage(bitmapWidth, bitmapHeight);
        final Graphics2D graphics2D = this.currentImage.createGraphics();

        // draw page background
        if (!getSettings().hasTransparentPageBackground()) {
            graphics2D.setBackground(getSettings().getPageBackgroundColor());
            graphics2D.setPaint(getSettings().getPageBackgroundColor());
            graphics2D.fillRect(0, 0, bitmapWidth, bitmapHeight);
        }

        // Set rendering hints
        graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        if (getSettings().isAntiAliasingEnabled()
                && this.currentImage.getColorModel().getPixelSize() > 1) {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        if (getSettings().isQualityRenderingEnabled()) {
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
        } else {
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);
        }
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        // Set up initial coordinate system for the page
        if (offset != null) {
            graphics2D.translate(offset.getX(), offset.getY());
        }
        graphics2D.scale(scale / 1000f, scale / 1000f);

        return new Java2DPainter(graphics2D, getContext(), getFontInfo());
    }

    /**
     * Creates a new BufferedImage.
     *
     * @param bitmapWidth
     *            the desired width in pixels
     * @param bitmapHeight
     *            the desired height in pixels
     * @return the new BufferedImage instance
     */
    protected BufferedImage createBufferedImage(final int bitmapWidth,
            final int bitmapHeight) {
        return new BufferedImage(bitmapWidth, bitmapHeight, getSettings()
                .getBufferedImageType());
    }

    /** {@inheritDoc} */
    @Override
    public void endPageContent() throws IFException {
        try {
            if (this.multiImageWriter == null) {
                switch (this.pageCount) {
                case 1:
                    this.imageWriter.writeImage(this.currentImage,
                            this.outputStream, getSettings().getWriterParams());
                    IOUtils.closeQuietly(this.outputStream);
                    this.outputStream = null;
                    break;
                default:
                    final OutputStream out = this.multiFileUtil
                    .createOutputStream(this.pageCount - 1);
                    if (out == null) {
                        final BitmapRendererEventProducer eventProducer = BitmapRendererEventProducer.Provider
                                .get(getUserAgent().getEventBroadcaster());
                        eventProducer.stoppingAfterFirstPageNoFilename(this);
                    } else {
                        try {
                            this.imageWriter.writeImage(this.currentImage, out,
                                    getSettings().getWriterParams());
                        } finally {
                            IOUtils.closeQuietly(out);
                        }
                    }
                }
            } else {
                this.multiImageWriter.writeImage(this.currentImage,
                        getSettings().getWriterParams());
            }
            this.currentImage = null;
        } catch (final IOException ioe) {
            throw new IFException("I/O error while encoding BufferedImage", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPage() throws IFException {
        this.currentPageDimensions = null;
    }

    /** {@inheritDoc} */
    @Override
    public void handleExtensionObject(final Object extension)
            throws IFException {
        log.debug("Don't know how to handle extension object. Ignoring: "
                + extension + " (" + extension.getClass().getName() + ")");
    }

}
