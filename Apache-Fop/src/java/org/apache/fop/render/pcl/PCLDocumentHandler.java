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

/* $Id: PCLDocumentHandler.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.pcl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FopFactoryConfigurator;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.render.intermediate.AbstractBinaryWritingIFDocumentHandler;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandlerConfigurator;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFPainter;
import org.apache.fop.render.java2d.Java2DPainter;
import org.apache.fop.render.java2d.Java2DUtil;
import org.apache.fop.render.pcl.extensions.PCLElementMapping;
import org.apache.xmlgraphics.util.UnitConv;

/**
 * {@link org.apache.fop.render.intermediate.IFDocumentHandler} implementation
 * that produces PCL 5.
 */
@Slf4j
public class PCLDocumentHandler extends AbstractBinaryWritingIFDocumentHandler
        implements PCLConstants {

    /**
     * Utility class for handling all sorts of peripheral tasks around PCL
     * generation.
     */
    protected PCLRenderingUtil pclUtil;

    /** The PCL generator */
    private PCLGenerator gen;

    private PCLPageDefinition currentPageDefinition;

    /** contains the pageWith of the last printed page */
    private long pageWidth = 0;
    /** contains the pageHeight of the last printed page */
    private long pageHeight = 0;

    /** the current page image (only set when all-bitmap painting is activated) */
    private BufferedImage currentImage;

    /**
     * Default constructor.
     */
    public PCLDocumentHandler() {
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsPagesOutOfOrder() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return org.apache.xmlgraphics.util.MimeConstants.MIME_PCL;
    }

    /** {@inheritDoc} */
    @Override
    public void setContext(final IFContext context) {
        super.setContext(context);
        this.pclUtil = new PCLRenderingUtil(context.getUserAgent());
    }

    /** {@inheritDoc} */
    @Override
    public IFDocumentHandlerConfigurator getConfigurator() {
        return new PCLRendererConfigurator(getUserAgent());
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultFontInfo(final FontInfo fontInfo) {
        final FontInfo fi = Java2DUtil.buildDefaultJava2DBasedFontInfo(
                fontInfo, getUserAgent());
        setFontInfo(fi);
    }

    PCLRenderingUtil getPCLUtil() {
        return this.pclUtil;
    }

    PCLGenerator getPCLGenerator() {
        return this.gen;
    }

    /** @return the target resolution */
    protected int getResolution() {
        final int resolution = Math.round(getUserAgent().getTargetResolution());
        if (resolution <= 300) {
            return 300;
        } else {
            return 600;
        }
    }

    // ----------------------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws IFException {
        super.startDocument();
        try {
            this.gen = new PCLGenerator(this.outputStream, getResolution());
            this.gen.setDitheringQuality(this.pclUtil.getDitheringQuality());

            if (!this.pclUtil.isPJLDisabled()) {
                this.gen.universalEndOfLanguage();
                this.gen.writeText("@PJL COMMENT Produced by "
                        + getUserAgent().getProducer() + "\n");
                if (getUserAgent().getTitle() != null) {
                    this.gen.writeText("@PJL JOB NAME = \""
                            + getUserAgent().getTitle() + "\"\n");
                }
                this.gen.writeText("@PJL SET RESOLUTION = " + getResolution()
                        + "\n");
                this.gen.writeText("@PJL ENTER LANGUAGE = PCL\n");
            }
            this.gen.resetPrinter();
            this.gen.setUnitOfMeasure(getResolution());
            this.gen.setRasterGraphicsResolution(getResolution());
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
            this.gen.separateJobs();
            this.gen.resetPrinter();
            if (!this.pclUtil.isPJLDisabled()) {
                this.gen.universalEndOfLanguage();
            }
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

        try {
            // Paper source
            final Object paperSource = getContext().getForeignAttribute(
                    PCLElementMapping.PCL_PAPER_SOURCE);
            if (paperSource != null) {
                this.gen.selectPaperSource(Integer.parseInt(paperSource
                        .toString()));
            }

            // Output bin
            final Object outputBin = getContext().getForeignAttribute(
                    PCLElementMapping.PCL_OUTPUT_BIN);
            if (outputBin != null) {
                this.gen.selectOutputBin(Integer.parseInt(outputBin.toString()));
            }

            // Is Page duplex?
            final Object pageDuplex = getContext().getForeignAttribute(
                    PCLElementMapping.PCL_DUPLEX_MODE);
            if (pageDuplex != null) {
                this.gen.selectDuplexMode(Integer.parseInt(pageDuplex
                        .toString()));
            }

            // Page size
            final long pagewidth = size.width;
            final long pageheight = size.height;
            selectPageFormat(pagewidth, pageheight);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in startPage()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public IFPainter startPageContent() throws IFException {
        if (this.pclUtil.getRenderingMode() == PCLRenderingMode.BITMAP) {
            return createAllBitmapPainter();
        } else {
            return new PCLPainter(this, this.currentPageDefinition);
        }
    }

    private IFPainter createAllBitmapPainter() {
        final double scale = this.gen.getMaximumBitmapResolution()
                / FopFactoryConfigurator.DEFAULT_TARGET_RESOLUTION;
        final Rectangle printArea = this.currentPageDefinition
                .getLogicalPageRect();
        final int bitmapWidth = (int) Math.ceil(UnitConv.mpt2px(
                printArea.width, this.gen.getMaximumBitmapResolution()));
        final int bitmapHeight = (int) Math.ceil(UnitConv.mpt2px(
                printArea.height, this.gen.getMaximumBitmapResolution()));
        this.currentImage = createBufferedImage(bitmapWidth, bitmapHeight);
        final Graphics2D graphics2D = this.currentImage.createGraphics();

        if (!PCLGenerator.isJAIAvailable()) {
            final RenderingHints hints = new RenderingHints(null);
            // These hints don't seem to make a difference :-( Not seeing any
            // dithering on Sun Java.
            hints.put(RenderingHints.KEY_DITHERING,
                    RenderingHints.VALUE_DITHER_ENABLE);
            graphics2D.addRenderingHints(hints);
        }

        // Ensure white page background
        graphics2D.setBackground(Color.WHITE);
        graphics2D.clearRect(0, 0, bitmapWidth, bitmapHeight);

        graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        graphics2D.scale(scale / 1000f, scale / 1000f);
        graphics2D.translate(-printArea.x, -printArea.y);

        return new Java2DPainter(graphics2D, getContext(), getFontInfo());
    }

    private BufferedImage createBufferedImage(final int bitmapWidth,
            final int bitmapHeight) {
        int bitmapType;
        if (PCLGenerator.isJAIAvailable()) {
            // TYPE_BYTE_GRAY was used to work around the lack of dithering when
            // using
            // TYPE_BYTE_BINARY. Adding RenderingHints didn't help.
            bitmapType = BufferedImage.TYPE_BYTE_GRAY;
            // bitmapType = BufferedImage.TYPE_INT_RGB; //Use to enable Batik
            // gradients
        } else {
            bitmapType = BufferedImage.TYPE_BYTE_BINARY;
        }
        return new BufferedImage(bitmapWidth, bitmapHeight, bitmapType);
    }

    /** {@inheritDoc} */
    @Override
    public void endPageContent() throws IFException {
        if (this.currentImage != null) {
            try {
                // ImageWriterUtil.saveAsPNG(this.currentImage, new
                // java.io.File("D:/page.png"));
                final Rectangle printArea = this.currentPageDefinition
                        .getLogicalPageRect();
                this.gen.setCursorPos(0, 0);
                this.gen.paintBitmap(this.currentImage, printArea.getSize(),
                        true);
            } catch (final IOException ioe) {
                throw new IFException("I/O error while encoding page image",
                        ioe);
            } finally {
                this.currentImage = null;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPage() throws IFException {
        try {
            // Eject page
            this.gen.formFeed();
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endPage()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleExtensionObject(final Object extension)
            throws IFException {
        log.debug("Don't know how to handle extension object. Ignoring: "
                + extension + " (" + extension.getClass().getName() + ")");
    }

    private void selectPageFormat(final long pagewidth, final long pageheight)
            throws IOException {
        // Only set the page format if it changes (otherwise duplex printing
        // won't work)
        if (pagewidth != this.pageWidth || pageheight != this.pageHeight) {
            this.pageWidth = pagewidth;
            this.pageHeight = pageheight;

            this.currentPageDefinition = PCLPageDefinition.getPageDefinition(
                    pagewidth, pageheight, 1000);

            if (this.currentPageDefinition == null) {
                this.currentPageDefinition = PCLPageDefinition
                        .getDefaultPageDefinition();
                log.warn("Paper type could not be determined. Falling back to: "
                        + this.currentPageDefinition.getName());
            }
            if (log.isDebugEnabled()) {
                log.debug("page size: "
                        + this.currentPageDefinition.getPhysicalPageSize());
                log.debug("logical page: "
                        + this.currentPageDefinition.getLogicalPageRect());
            }

            if (this.currentPageDefinition.isLandscapeFormat()) {
                this.gen.writeCommand("&l1O"); // Landscape Orientation
            } else {
                this.gen.writeCommand("&l0O"); // Portrait Orientation
            }
            this.gen.selectPageSize(this.currentPageDefinition.getSelector());

            this.gen.clearHorizontalMargins();
            this.gen.setTopMargin(0);
        }
    }

}
