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

/* $Id: PDFDocumentHandler.java 1242848 2012-02-10 16:51:08Z phancock $ */

package org.apache.fop.render.pdf;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.accessibility.StructureTreeEventHandler;
import org.apache.fop.fo.extensions.xmp.XMPMetadata;
import org.apache.fop.pdf.PDFAnnotList;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFPage;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFResources;
import org.apache.fop.render.extensions.prepress.PageBoundaries;
import org.apache.fop.render.extensions.prepress.PageScale;
import org.apache.fop.render.intermediate.AbstractBinaryWritingIFDocumentHandler;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandlerConfigurator;
import org.apache.fop.render.intermediate.IFDocumentNavigationHandler;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFPainter;
import org.apache.fop.render.pdf.extensions.PDFEmbeddedFileExtensionAttachment;
import org.apache.xmlgraphics.xmp.Metadata;

/**
 * {@link org.apache.fop.render.intermediate.IFDocumentHandler} implementation
 * that produces PDF.
 */
@Slf4j
public class PDFDocumentHandler extends AbstractBinaryWritingIFDocumentHandler {

    private boolean accessEnabled;

    private PDFLogicalStructureHandler logicalStructureHandler;

    private PDFStructureTreeBuilder structureTreeBuilder;

    /** the PDF Document being created */
    protected PDFDocument pdfDoc;

    /**
     * Utility class which enables all sorts of features that are not directly
     * connected to the normal rendering process.
     */
    protected PDFRenderingUtil pdfUtil;

    /** the /Resources object of the PDF document being created */
    protected PDFResources pdfResources;

    /** The current content generator */
    protected PDFContentGenerator generator;

    /** the current annotation list to add annotations to */
    protected PDFResourceContext currentContext;

    /** the current page to add annotations to */
    protected PDFPage currentPage;

    /** the current page's PDF reference */
    protected PageReference currentPageRef;

    /** Used for bookmarks/outlines. */
    protected Map<Integer, PageReference> pageReferences = new HashMap<Integer, PageReference>();

    private final PDFDocumentNavigationHandler documentNavigationHandler = new PDFDocumentNavigationHandler(
            this);

    /**
     * Default constructor.
     */
    public PDFDocumentHandler() {
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsPagesOutOfOrder() {
        return !this.accessEnabled;
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return org.apache.xmlgraphics.util.MimeConstants.MIME_PDF;
    }

    /** {@inheritDoc} */
    @Override
    public void setContext(final IFContext context) {
        super.setContext(context);
        this.pdfUtil = new PDFRenderingUtil(context.getUserAgent());
    }

    /** {@inheritDoc} */
    @Override
    public IFDocumentHandlerConfigurator getConfigurator() {
        return new PDFRendererConfigurator(getUserAgent());
    }

    /** {@inheritDoc} */
    @Override
    public IFDocumentNavigationHandler getDocumentNavigationHandler() {
        return this.documentNavigationHandler;
    }

    PDFRenderingUtil getPDFUtil() {
        return this.pdfUtil;
    }

    PDFLogicalStructureHandler getLogicalStructureHandler() {
        return this.logicalStructureHandler;
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws IFException {
        super.startDocument();
        try {
            this.pdfDoc = this.pdfUtil.setupPDFDocument(this.outputStream);
            this.accessEnabled = getUserAgent().isAccessibilityEnabled();
            if (this.accessEnabled) {
                setupAccessibility();
            }
        } catch (final IOException e) {
            throw new IFException("I/O error in startDocument()", e);
        }
    }

    private void setupAccessibility() {
        this.pdfDoc.getRoot().makeTagged();
        this.logicalStructureHandler = new PDFLogicalStructureHandler(
                this.pdfDoc);
        // TODO this is ugly. All the necessary information should be available
        // at creation time in order to enforce immutability
        this.structureTreeBuilder.setPdfFactory(this.pdfDoc.getFactory());
        this.structureTreeBuilder
        .setLogicalStructureHandler(this.logicalStructureHandler);
        this.structureTreeBuilder.setEventBroadcaster(getUserAgent()
                .getEventBroadcaster());
    }

    /** {@inheritDoc} */
    @Override
    public void endDocumentHeader() throws IFException {
        this.pdfUtil.generateDefaultXMPMetadata();
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws IFException {
        try {
            this.pdfDoc.getResources().addFonts(this.pdfDoc, this.fontInfo);
            this.pdfDoc.outputTrailer(this.outputStream);
            this.pdfDoc = null;

            this.pdfResources = null;
            this.generator = null;
            this.currentContext = null;
            this.currentPage = null;
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
        this.pdfResources = this.pdfDoc.getResources();

        final PageBoundaries boundaries = new PageBoundaries(size, getContext()
                .getForeignAttributes());

        final Rectangle trimBox = boundaries.getTrimBox();
        final Rectangle bleedBox = boundaries.getBleedBox();
        final Rectangle mediaBox = boundaries.getMediaBox();
        final Rectangle cropBox = boundaries.getCropBox();

        // set scale attributes
        double scaleX = 1;
        double scaleY = 1;
        final String scale = (String) getContext().getForeignAttribute(
                PageScale.EXT_PAGE_SCALE);
        final Point2D scales = PageScale.getScale(scale);
        if (scales != null) {
            scaleX = scales.getX();
            scaleY = scales.getY();
        }

        // PDF uses the lower left as origin, need to transform from FOP's
        // internal coord system
        final AffineTransform boxTransform = new AffineTransform(scaleX / 1000,
                0, 0, -scaleY / 1000, 0, scaleY * size.getHeight() / 1000);

        this.currentPage = this.pdfDoc.getFactory().makePage(this.pdfResources,
                index, toPDFCoordSystem(mediaBox, boxTransform),
                toPDFCoordSystem(cropBox, boxTransform),
                toPDFCoordSystem(bleedBox, boxTransform),
                toPDFCoordSystem(trimBox, boxTransform));
        if (this.accessEnabled) {
            this.logicalStructureHandler.startPage(this.currentPage);
        }

        this.pdfUtil.generatePageLabel(index, name);

        this.currentPageRef = new PageReference(this.currentPage, size);
        this.pageReferences.put(Integer.valueOf(index), this.currentPageRef);

        this.generator = new PDFContentGenerator(this.pdfDoc,
                this.outputStream, this.currentPage);
        // Transform the PDF's default coordinate system (0,0 at lower left) to
        // the PDFPainter's
        final AffineTransform basicPageTransform = new AffineTransform(1, 0, 0,
                -1, 0, scaleY * size.height / 1000f);
        basicPageTransform.scale(scaleX, scaleY);
        this.generator.saveGraphicsState();
        this.generator.concatenate(basicPageTransform);
    }

    private Rectangle2D toPDFCoordSystem(final Rectangle box,
            final AffineTransform transform) {
        return transform.createTransformedShape(box).getBounds2D();
    }

    /** {@inheritDoc} */
    @Override
    public IFPainter startPageContent() throws IFException {
        return new PDFPainter(this, this.logicalStructureHandler);
    }

    /** {@inheritDoc} */
    @Override
    public void endPageContent() throws IFException {
        this.generator.restoreGraphicsState();
        // for top-level transform to change the default coordinate system
    }

    /** {@inheritDoc} */
    @Override
    public void endPage() throws IFException {
        if (this.accessEnabled) {
            this.logicalStructureHandler.endPage();
        }
        try {
            this.documentNavigationHandler.commit();
            this.pdfDoc.registerObject(this.generator.getStream());
            this.currentPage.setContents(this.generator.getStream());
            final PDFAnnotList annots = this.currentPage.getAnnotations();
            if (annots != null) {
                this.pdfDoc.addObject(annots);
            }
            this.pdfDoc.addObject(this.currentPage);
            this.generator.flushPDFDoc();
            this.generator = null;
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endPage()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleExtensionObject(final Object extension)
            throws IFException {
        if (extension instanceof XMPMetadata) {
            this.pdfUtil.renderXMPMetadata((XMPMetadata) extension);
        } else if (extension instanceof Metadata) {
            final XMPMetadata wrapper = new XMPMetadata((Metadata) extension);
            this.pdfUtil.renderXMPMetadata(wrapper);
        } else if (extension instanceof PDFEmbeddedFileExtensionAttachment) {
            final PDFEmbeddedFileExtensionAttachment embeddedFile = (PDFEmbeddedFileExtensionAttachment) extension;
            try {
                this.pdfUtil.addEmbeddedFile(embeddedFile);
            } catch (final IOException ioe) {
                throw new IFException("Error adding embedded file: "
                        + embeddedFile.getSrc(), ioe);
            }
        } else {
            log.debug("Don't know how to handle extension object. Ignoring: "
                    + extension + " (" + extension.getClass().getName() + ")");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setDocumentLocale(final Locale locale) {
        this.pdfDoc.getRoot().setLanguage(locale);
    }

    PageReference getPageReference(final int pageIndex) {
        return this.pageReferences.get(Integer.valueOf(pageIndex));
    }

    static final class PageReference {

        private final String pageRef;
        private final Dimension pageDimension;

        private PageReference(final PDFPage page, final Dimension dim) {
            // Avoid keeping references to PDFPage as memory usage is
            // considerably increased when handling thousands of pages.
            this.pageRef = page.makeReference().toString();
            this.pageDimension = new Dimension(dim);
        }

        public String getPageRef() {
            return this.pageRef;
        }

        public Dimension getPageDimension() {
            return this.pageDimension;
        }
    }

    @Override
    public StructureTreeEventHandler getStructureTreeEventHandler() {
        if (this.structureTreeBuilder == null) {
            this.structureTreeBuilder = new PDFStructureTreeBuilder();
        }
        return this.structureTreeBuilder;
    }
}
