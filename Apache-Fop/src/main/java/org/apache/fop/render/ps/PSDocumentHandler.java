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

/* $Id: PSDocumentHandler.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.ps;

import java.awt.Dimension;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.render.intermediate.AbstractBinaryWritingIFDocumentHandler;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandlerConfigurator;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFPainter;
import org.apache.fop.render.ps.extensions.PSCommentAfter;
import org.apache.fop.render.ps.extensions.PSCommentBefore;
import org.apache.fop.render.ps.extensions.PSPageTrailerCodeBefore;
import org.apache.fop.render.ps.extensions.PSSetPageDevice;
import org.apache.fop.render.ps.extensions.PSSetupCode;
import org.apache.xmlgraphics.java2d.Dimension2DDouble;
import org.apache.xmlgraphics.ps.DSCConstants;
import org.apache.xmlgraphics.ps.PSDictionary;
import org.apache.xmlgraphics.ps.PSDictionaryFormatException;
import org.apache.xmlgraphics.ps.PSGenerator;
import org.apache.xmlgraphics.ps.PSPageDeviceDictionary;
import org.apache.xmlgraphics.ps.PSProcSets;
import org.apache.xmlgraphics.ps.PSResource;
import org.apache.xmlgraphics.ps.dsc.DSCException;
import org.apache.xmlgraphics.ps.dsc.ResourceTracker;
import org.apache.xmlgraphics.ps.dsc.events.DSCCommentBoundingBox;
import org.apache.xmlgraphics.ps.dsc.events.DSCCommentHiResBoundingBox;

/**
 * {@link org.apache.fop.render.intermediate.IFDocumentHandler} implementation
 * that produces PostScript.
 */
@Slf4j
public class PSDocumentHandler extends AbstractBinaryWritingIFDocumentHandler {

    /**
     * Utility class which enables all sorts of features that are not directly
     * connected to the normal rendering process.
     */
    protected PSRenderingUtil psUtil;

    /** The PostScript generator used to output the PostScript */
    protected PSGenerator gen;

    /** the temporary file in case of two-pass processing */
    private File tempFile;

    private int currentPageNumber = 0;
    private PageDefinition currentPageDefinition;

    /** Is used to determine the document's bounding box */
    private Rectangle2D documentBoundingBox;

    /**
     * Used to temporarily store PSSetupCode instance until they can be written.
     */
    private List setupCodeList;

    /** This is a cache of PSResource instances of all fonts defined */
    private FontResourceCache fontResources;
    /** This is a map of PSResource instances of all forms (key: uri) */
    private Map formResources;

    /** encapsulation of dictionary used in setpagedevice instruction **/
    private PSPageDeviceDictionary pageDeviceDictionary;

    /** This is a collection holding all document header comments */
    private final Collection[] comments = new Collection[4];
    private static final int COMMENT_DOCUMENT_HEADER = 0;
    private static final int COMMENT_DOCUMENT_TRAILER = 1;
    private static final int COMMENT_PAGE_TRAILER = 2;
    private static final int PAGE_TRAILER_CODE_BEFORE = 3;

    private PSEventProducer eventProducer;

    /**
     * Default constructor.
     */
    public PSDocumentHandler() {
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsPagesOutOfOrder() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return org.apache.xmlgraphics.util.MimeConstants.MIME_POSTSCRIPT;
    }

    /** {@inheritDoc} */
    @Override
    public void setContext(final IFContext context) {
        super.setContext(context);
        final FOUserAgent userAgent = context.getUserAgent();
        this.psUtil = new PSRenderingUtil(userAgent);
        this.eventProducer = PSEventProducer.Provider.get(userAgent
                .getEventBroadcaster());
    }

    /** {@inheritDoc} */
    @Override
    public IFDocumentHandlerConfigurator getConfigurator() {
        return new PSRendererConfigurator(getUserAgent());
    }

    PSRenderingUtil getPSUtil() {
        return this.psUtil;
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws IFException {
        super.startDocument();
        this.fontResources = new FontResourceCache(getFontInfo());
        try {
            OutputStream out;
            if (this.psUtil.isOptimizeResources()) {
                this.tempFile = File.createTempFile("fop", ".ps");
                out = new java.io.FileOutputStream(this.tempFile);
                out = new java.io.BufferedOutputStream(out);
            } else {
                out = this.outputStream;
            }

            // Setup for PostScript generation
            this.gen = new PSGenerator(out) {
                /** Need to subclass PSGenerator to have better URI resolution */
                @Override
                public Source resolveURI(final String uri) {
                    return getUserAgent().resolveURI(uri);
                }
            };
            this.gen.setPSLevel(this.psUtil.getLanguageLevel());
            this.currentPageNumber = 0;
            this.documentBoundingBox = new Rectangle2D.Double();

            // Initial default page device dictionary settings
            this.pageDeviceDictionary = new PSPageDeviceDictionary();
            this.pageDeviceDictionary.setFlushOnRetrieval(!this.psUtil
                    .isDSCComplianceEnabled());
            this.pageDeviceDictionary.put("/ImagingBBox", "null");
        } catch (final IOException e) {
            throw new IFException("I/O error in startDocument()", e);
        }
    }

    private void writeHeader() throws IOException {
        // PostScript Header
        this.gen.writeln(DSCConstants.PS_ADOBE_30);
        this.gen.writeDSCComment(DSCConstants.CREATOR,
                new String[] { getUserAgent().getProducer() });
        this.gen.writeDSCComment(DSCConstants.CREATION_DATE,
                new Object[] { new java.util.Date() });
        this.gen.writeDSCComment(DSCConstants.LANGUAGE_LEVEL, new Integer(
                this.gen.getPSLevel()));
        this.gen.writeDSCComment(DSCConstants.PAGES,
                new Object[] { DSCConstants.ATEND });
        this.gen.writeDSCComment(DSCConstants.BBOX, DSCConstants.ATEND);
        this.gen.writeDSCComment(DSCConstants.HIRES_BBOX, DSCConstants.ATEND);
        this.gen.writeDSCComment(DSCConstants.DOCUMENT_SUPPLIED_RESOURCES,
                new Object[] { DSCConstants.ATEND });
        writeExtensions(COMMENT_DOCUMENT_HEADER);
        this.gen.writeDSCComment(DSCConstants.END_COMMENTS);

        // Defaults
        this.gen.writeDSCComment(DSCConstants.BEGIN_DEFAULTS);
        this.gen.writeDSCComment(DSCConstants.END_DEFAULTS);

        // Prolog and Setup written right before the first page-sequence, see
        // startPageSequence()
        // Do this only once, as soon as we have all the content for the Setup
        // section!
        // Prolog
        this.gen.writeDSCComment(DSCConstants.BEGIN_PROLOG);
        PSProcSets.writeStdProcSet(this.gen);
        PSProcSets.writeEPSProcSet(this.gen);
        FOPProcSet.INSTANCE.writeTo(this.gen);
        this.gen.writeDSCComment(DSCConstants.END_PROLOG);

        // Setup
        this.gen.writeDSCComment(DSCConstants.BEGIN_SETUP);
        PSRenderingUtil.writeSetupCodeList(this.gen, this.setupCodeList,
                "SetupCode");
        if (!this.psUtil.isOptimizeResources()) {
            this.fontResources.addAll(PSFontUtils.writeFontDict(this.gen,
                    this.fontInfo, this.eventProducer));
        } else {
            this.gen.commentln("%FOPFontSetup"); // Place-holder, will be
            // replaced in the second pass
        }
        this.gen.writeDSCComment(DSCConstants.END_SETUP);
    }

    /** {@inheritDoc} */
    @Override
    public void endDocumentHeader() throws IFException {
        try {
            writeHeader();
        } catch (final IOException ioe) {
            throw new IFException("I/O error writing the PostScript header",
                    ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws IFException {
        try {
            // Write trailer
            this.gen.writeDSCComment(DSCConstants.TRAILER);
            writeExtensions(COMMENT_DOCUMENT_TRAILER);
            this.gen.writeDSCComment(DSCConstants.PAGES, new Integer(
                    this.currentPageNumber));
            new DSCCommentBoundingBox(this.documentBoundingBox)
            .generate(this.gen);
            new DSCCommentHiResBoundingBox(this.documentBoundingBox)
            .generate(this.gen);
            this.gen.getResourceTracker().writeResources(false, this.gen);
            this.gen.writeDSCComment(DSCConstants.EOF);
            this.gen.flush();
            log.debug("Rendering to PostScript complete.");
            if (this.psUtil.isOptimizeResources()) {
                IOUtils.closeQuietly(this.gen.getOutputStream());
                rewritePostScriptFile();
            }
            if (this.pageDeviceDictionary != null) {
                this.pageDeviceDictionary.clear();
            }
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endDocument()", ioe);
        }
        super.endDocument();
    }

    /**
     * Used for two-pass production. This will rewrite the PostScript file from
     * the temporary file while adding all needed resources.
     *
     * @throws IOException
     *             In case of an I/O error.
     */
    private void rewritePostScriptFile() throws IOException {
        log.debug("Processing PostScript resources...");
        final long startTime = System.currentTimeMillis();
        final ResourceTracker resTracker = this.gen.getResourceTracker();
        InputStream in = new java.io.FileInputStream(this.tempFile);
        in = new java.io.BufferedInputStream(in);
        try {
            try {
                final ResourceHandler handler = new ResourceHandler(
                        getUserAgent(), this.eventProducer, this.fontInfo,
                        resTracker, this.formResources);
                handler.process(in, this.outputStream, this.currentPageNumber,
                        this.documentBoundingBox);
                this.outputStream.flush();
            } catch (final DSCException e) {
                throw new RuntimeException(e.getMessage());
            }
        } finally {
            IOUtils.closeQuietly(in);
            if (!this.tempFile.delete()) {
                this.tempFile.deleteOnExit();
                log.warn("Could not delete temporary file: " + this.tempFile);
            }
        }
        if (log.isDebugEnabled()) {
            final long duration = System.currentTimeMillis() - startTime;
            log.debug("Resource Processing complete in " + duration + " ms.");
        }
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
            if (this.currentPageNumber == 0) {
                // writeHeader();
            }

            this.currentPageNumber++;

            this.gen.getResourceTracker().notifyStartNewPage();
            this.gen.getResourceTracker().notifyResourceUsageOnPage(
                    PSProcSets.STD_PROCSET);
            this.gen.writeDSCComment(DSCConstants.PAGE, new Object[] { name,
                    new Integer(this.currentPageNumber) });

            final double pageWidth = size.width / 1000.0;
            final double pageHeight = size.height / 1000.0;
            boolean rotate = false;
            final List pageSizes = new java.util.ArrayList();
            if (this.psUtil.isAutoRotateLandscape() && pageHeight < pageWidth) {
                rotate = true;
                pageSizes.add(new Long(Math.round(pageHeight)));
                pageSizes.add(new Long(Math.round(pageWidth)));
            } else {
                pageSizes.add(new Long(Math.round(pageWidth)));
                pageSizes.add(new Long(Math.round(pageHeight)));
            }
            this.pageDeviceDictionary.put("/PageSize", pageSizes);
            this.currentPageDefinition = new PageDefinition(
                    new Dimension2DDouble(pageWidth, pageHeight), rotate);

            // TODO Handle extension attachments for the page!!!!!!!
            /*
             * if (page.hasExtensionAttachments()) { for (Iterator iter =
             * page.getExtensionAttachments().iterator(); iter.hasNext();) {
             * ExtensionAttachment attachment = (ExtensionAttachment)
             * iter.next(); if (attachment instanceof PSSetPageDevice) {
             */
            /**
             * Extract all PSSetPageDevice instances from the attachment list on
             * the s-p-m and add all dictionary entries to our internal
             * representation of the the page device dictionary.
             */
            /*
             * PSSetPageDevice setPageDevice = (PSSetPageDevice)attachment;
             * String content = setPageDevice.getContent(); if (content != null)
             * { try {
             * pageDeviceDictionary.putAll(PSDictionary.valueOf(content)); }
             * catch (PSDictionaryFormatException e) { PSEventProducer
             * eventProducer = PSEventProducer.Provider.get(
             * getUserAgent().getEventBroadcaster());
             * eventProducer.postscriptDictionaryParseError(this, content, e); }
             * } } } }
             */

            final Integer zero = new Integer(0);
            final Rectangle2D pageBoundingBox = new Rectangle2D.Double();
            if (rotate) {
                pageBoundingBox.setRect(0, 0, pageHeight, pageWidth);
                this.gen.writeDSCComment(DSCConstants.PAGE_BBOX, new Object[] {
                        zero, zero, new Long(Math.round(pageHeight)),
                        new Long(Math.round(pageWidth)) });
                this.gen.writeDSCComment(DSCConstants.PAGE_HIRES_BBOX,
                        new Object[] { zero, zero, new Double(pageHeight),
                        new Double(pageWidth) });
                this.gen.writeDSCComment(DSCConstants.PAGE_ORIENTATION,
                        "Landscape");
            } else {
                pageBoundingBox.setRect(0, 0, pageWidth, pageHeight);
                this.gen.writeDSCComment(DSCConstants.PAGE_BBOX, new Object[] {
                        zero, zero, new Long(Math.round(pageWidth)),
                        new Long(Math.round(pageHeight)) });
                this.gen.writeDSCComment(DSCConstants.PAGE_HIRES_BBOX,
                        new Object[] { zero, zero, new Double(pageWidth),
                        new Double(pageHeight) });
                if (this.psUtil.isAutoRotateLandscape()) {
                    this.gen.writeDSCComment(DSCConstants.PAGE_ORIENTATION,
                            "Portrait");
                }
            }
            this.documentBoundingBox.add(pageBoundingBox);
            this.gen.writeDSCComment(DSCConstants.PAGE_RESOURCES,
                    new Object[] { DSCConstants.ATEND });

            this.gen.commentln("%FOPSimplePageMaster: " + pageMasterName);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in startPage()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageHeader() throws IFException {
        super.startPageHeader();

        try {
            this.gen.writeDSCComment(DSCConstants.BEGIN_PAGE_SETUP);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in startPageHeader()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPageHeader() throws IFException {
        try {
            // Write any unwritten changes to page device dictionary
            if (!this.pageDeviceDictionary.isEmpty()) {
                String content = this.pageDeviceDictionary.getContent();
                if (this.psUtil.isSafeSetPageDevice()) {
                    content += " SSPD";
                } else {
                    content += " setpagedevice";
                }
                PSRenderingUtil.writeEnclosedExtensionAttachment(this.gen,
                        new PSSetPageDevice(content));
            }

            final double pageHeight = this.currentPageDefinition.dimensions
                    .getHeight();
            if (this.currentPageDefinition.rotate) {
                this.gen.writeln(this.gen.formatDouble(pageHeight)
                        + " 0 translate");
                this.gen.writeln("90 rotate");
            }
            this.gen.concatMatrix(1, 0, 0, -1, 0, pageHeight);

            this.gen.writeDSCComment(DSCConstants.END_PAGE_SETUP);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endPageHeader()", ioe);
        }

        super.endPageHeader();
    }

    private void writeExtensions(final int which) throws IOException {
        final Collection extensions = this.comments[which];
        if (extensions != null) {
            PSRenderingUtil.writeEnclosedExtensionAttachments(this.gen,
                    extensions);
            extensions.clear();
        }
    }

    /** {@inheritDoc} */
    @Override
    public IFPainter startPageContent() throws IFException {
        return new PSPainter(this);
    }

    /** {@inheritDoc} */
    @Override
    public void endPageContent() throws IFException {
        try {
            this.gen.showPage();
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endPageContent()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageTrailer() throws IFException {
        try {
            writeExtensions(PAGE_TRAILER_CODE_BEFORE);
            super.startPageTrailer();
            this.gen.writeDSCComment(DSCConstants.PAGE_TRAILER);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in startPageTrailer()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPageTrailer() throws IFException {
        try {
            writeExtensions(COMMENT_PAGE_TRAILER);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endPageTrailer()", ioe);
        }
        super.endPageTrailer();
    }

    /** {@inheritDoc} */
    @Override
    public void endPage() throws IFException {
        try {
            this.gen.getResourceTracker().writeResources(true, this.gen);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endPage()", ioe);
        }

        this.currentPageDefinition = null;
    }

    private boolean inPage() {
        return this.currentPageDefinition != null;
    }

    /** {@inheritDoc} */
    @Override
    public void handleExtensionObject(final Object extension)
            throws IFException {
        try {
            if (extension instanceof PSSetupCode) {
                if (inPage()) {
                    PSRenderingUtil.writeEnclosedExtensionAttachment(this.gen,
                            (PSSetupCode) extension);
                } else {
                    // A special collection for setup code as it's put in a
                    // different place
                    // than the "before comments".
                    if (this.setupCodeList == null) {
                        this.setupCodeList = new java.util.ArrayList();
                    }
                    if (!this.setupCodeList.contains(extension)) {
                        this.setupCodeList.add(extension);
                    }
                }
            } else if (extension instanceof PSSetPageDevice) {
                /**
                 * Extract all PSSetPageDevice instances from the attachment
                 * list on the s-p-m and add all dictionary entries to our
                 * internal representation of the the page device dictionary.
                 */
                final PSSetPageDevice setPageDevice = (PSSetPageDevice) extension;
                final String content = setPageDevice.getContent();
                if (content != null) {
                    try {
                        this.pageDeviceDictionary.putAll(PSDictionary
                                .valueOf(content));
                    } catch (final PSDictionaryFormatException e) {
                        final PSEventProducer eventProducer = PSEventProducer.Provider
                                .get(getUserAgent().getEventBroadcaster());
                        eventProducer.postscriptDictionaryParseError(this,
                                content, e);
                    }
                }
            } else if (extension instanceof PSCommentBefore) {
                if (inPage()) {
                    PSRenderingUtil.writeEnclosedExtensionAttachment(this.gen,
                            (PSCommentBefore) extension);
                } else {
                    if (this.comments[COMMENT_DOCUMENT_HEADER] == null) {
                        this.comments[COMMENT_DOCUMENT_HEADER] = new java.util.ArrayList();
                    }
                    this.comments[COMMENT_DOCUMENT_HEADER].add(extension);
                }
            } else if (extension instanceof PSCommentAfter) {
                final int targetCollection = inPage() ? COMMENT_PAGE_TRAILER
                        : COMMENT_DOCUMENT_TRAILER;
                if (this.comments[targetCollection] == null) {
                    this.comments[targetCollection] = new java.util.ArrayList();
                }
                this.comments[targetCollection].add(extension);
            } else if (extension instanceof PSPageTrailerCodeBefore) {
                if (this.comments[PAGE_TRAILER_CODE_BEFORE] == null) {
                    this.comments[PAGE_TRAILER_CODE_BEFORE] = new ArrayList();
                }
                this.comments[PAGE_TRAILER_CODE_BEFORE].add(extension);
            }
        } catch (final IOException ioe) {
            throw new IFException("I/O error in handleExtensionObject()", ioe);
        }
    }

    /**
     * Returns the PSResource for the given font key.
     *
     * @param key
     *            the font key ("F*")
     * @return the matching PSResource
     */
    protected PSFontResource getPSResourceForFontKey(final String key) {
        return this.fontResources.getFontResourceForFontKey(key);
    }

    /**
     * Returns a PSResource instance representing a image as a PostScript form.
     *
     * @param uri
     *            the image URI
     * @return a PSResource instance
     */
    protected PSResource getFormForImage(final String uri) {
        if (uri == null || "".equals(uri)) {
            throw new IllegalArgumentException("uri must not be empty or null");
        }
        if (this.formResources == null) {
            this.formResources = new java.util.HashMap();
        }
        PSResource form = (PSResource) this.formResources.get(uri);
        if (form == null) {
            form = new PSImageFormResource(this.formResources.size() + 1, uri);
            this.formResources.put(uri, form);
        }
        return form;
    }

    private static final class PageDefinition {
        private final Dimension2D dimensions;
        private final boolean rotate;

        private PageDefinition(final Dimension2D dimensions,
                final boolean rotate) {
            this.dimensions = dimensions;
            this.rotate = rotate;
        }
    }

}
