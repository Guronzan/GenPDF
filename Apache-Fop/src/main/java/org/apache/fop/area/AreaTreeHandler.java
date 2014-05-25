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

/* $Id: AreaTreeHandler.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.area;

// Java
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.fop.fo.extensions.ExternalDocument;
import org.apache.fop.fo.extensions.destination.Destination;
import org.apache.fop.fo.pagination.AbstractPageSequence;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.Root;
import org.apache.fop.fo.pagination.bookmarks.BookmarkTree;
import org.apache.fop.layoutmgr.ExternalDocumentLayoutManager;
import org.apache.fop.layoutmgr.LayoutManagerMaker;
import org.apache.fop.layoutmgr.LayoutManagerMapping;
import org.apache.fop.layoutmgr.PageSequenceLayoutManager;
import org.apache.fop.layoutmgr.TopLevelLayoutManager;
import org.xml.sax.SAXException;

/**
 * Area tree handler for formatting objects.
 *
 * Concepts: The area tree is to be as small as possible. With minimal classes
 * and data to fully represent an area tree for formatting objects. The area
 * tree needs to be simple to render and follow the spec closely. This area tree
 * has the concept of page sequences. Wherever possible information is discarded
 * or optimized to keep memory use low. The data is also organized to make it
 * possible for renderers to minimize their output. A page can be saved if not
 * fully resolved and once rendered a page contains only size and id reference
 * information. The area tree pages are organized in a model that depends on the
 * type of renderer.
 */
@Slf4j
public class AreaTreeHandler extends FOEventHandler {

    // Recorder of debug statistics
    private Statistics statistics = null;

    // The LayoutManager maker
    private LayoutManagerMaker lmMaker;

    /** The AreaTreeModel in use */
    protected AreaTreeModel model;

    // Flag for controlling complex script features (default: true).
    private boolean useComplexScriptFeatures = true;

    // Keeps track of all meaningful id references
    private final IDTracker idTracker;

    // The fo:root node of the document
    private Root rootFObj;

    // The formatting results to be handed back to the caller.
    private final FormattingResults results = new FormattingResults();

    private TopLevelLayoutManager prevPageSeqLM;

    private int idGen = 0;

    /**
     * Constructor.
     *
     * @param userAgent
     *            FOUserAgent object for process
     * @param outputFormat
     *            the MIME type of the output format to use (ex.
     *            "application/pdf").
     * @param stream
     *            OutputStream
     * @throws FOPException
     *             if the RenderPagesModel cannot be created
     */
    public AreaTreeHandler(final FOUserAgent userAgent,
            final String outputFormat, final OutputStream stream)
                    throws FOPException {
        super(userAgent);

        setupModel(userAgent, outputFormat, stream);

        this.lmMaker = userAgent.getFactory().getLayoutManagerMakerOverride();
        if (this.lmMaker == null) {
            this.lmMaker = new LayoutManagerMapping();
        }

        this.idTracker = new IDTracker();

        this.useComplexScriptFeatures = userAgent
                .isComplexScriptFeaturesEnabled();

        if (log.isDebugEnabled()) {
            this.statistics = new Statistics();
        }
    }

    /**
     * Sets up the AreaTreeModel instance for use by the AreaTreeHandler.
     *
     * @param userAgent
     *            FOUserAgent object for process
     * @param outputFormat
     *            the MIME type of the output format to use (ex.
     *            "application/pdf").
     * @param stream
     *            OutputStream
     * @throws FOPException
     *             if the RenderPagesModel cannot be created
     */
    protected void setupModel(final FOUserAgent userAgent,
            final String outputFormat, final OutputStream stream)
                    throws FOPException {
        if (userAgent.isConserveMemoryPolicyEnabled()) {
            this.model = new CachedRenderPagesModel(userAgent, outputFormat,
                    this.fontInfo, stream);
        } else {
            this.model = new RenderPagesModel(userAgent, outputFormat,
                    this.fontInfo, stream);
        }
    }

    /**
     * Get the area tree model for this area tree.
     *
     * @return AreaTreeModel the model being used for this area tree
     */
    public AreaTreeModel getAreaTreeModel() {
        return this.model;
    }

    /**
     * Get the LayoutManager maker for this area tree.
     *
     * @return LayoutManagerMaker the LayoutManager maker being used for this
     *         area tree
     */
    public LayoutManagerMaker getLayoutManagerMaker() {
        return this.lmMaker;
    }

    /**
     * Get the IDTracker for this area tree.
     *
     * @return IDTracker used to track reference ids for items in this area tree
     */
    public IDTracker getIDTracker() {
        return this.idTracker;
    }

    /**
     * Get information about the rendered output, like number of pages created.
     *
     * @return the results structure
     */
    @Override
    public FormattingResults getResults() {
        return this.results;
    }

    /**
     * Check whether complex script features are enabled.
     *
     * @return true if using complex script features
     */
    public boolean isComplexScriptFeaturesEnabled() {
        return this.useComplexScriptFeatures;
    }

    /**
     * Prepare AreaTreeHandler for document processing This is called from
     * FOTreeBuilder.startDocument()
     *
     * @throws SAXException
     *             if there is an error
     */
    @Override
    public void startDocument() throws SAXException {
        // Initialize statistics
        if (this.statistics != null) {
            this.statistics.start();
        }
    }

    @Override
    public void startRoot(final Root root) {
        final Locale locale = root.getLocale();
        if (locale != null) {
            this.model.setDocumentLocale(locale);
        }
    }

    /**
     * finish the previous pageSequence
     */
    private void finishPrevPageSequence(final Numeric initialPageNumber) {
        if (this.prevPageSeqLM != null) {
            this.prevPageSeqLM.doForcePageCount(initialPageNumber);
            this.prevPageSeqLM.finishPageSequence();
            this.prevPageSeqLM = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final PageSequence pageSequence) {
        startAbstractPageSequence(pageSequence);
    }

    private void startAbstractPageSequence(
            final AbstractPageSequence pageSequence) {
        this.rootFObj = pageSequence.getRoot();

        // Before the first page-sequence...
        if (this.prevPageSeqLM == null) {
            // extension attachments from fo:root
            wrapAndAddExtensionAttachments(this.rootFObj
                    .getExtensionAttachments());
            // extension attachments from fo:declarations
            if (this.rootFObj.getDeclarations() != null) {
                wrapAndAddExtensionAttachments(this.rootFObj.getDeclarations()
                        .getExtensionAttachments());
            }
        }

        finishPrevPageSequence(pageSequence.getInitialPageNumber());
        pageSequence.initPageNumber();
    }

    private void wrapAndAddExtensionAttachments(
            final List<ExtensionAttachment> list) {
        for (final ExtensionAttachment attachment : list) {
            addOffDocumentItem(new OffDocumentExtensionAttachment(attachment));
        }
    }

    /**
     * End the PageSequence. The PageSequence formats Pages and adds them to the
     * AreaTree. The area tree then handles what happens with the pages.
     *
     * @param pageSequence
     *            the page sequence ending
     */
    @Override
    public void endPageSequence(final PageSequence pageSequence) {

        if (this.statistics != null) {
            this.statistics.end();
        }

        // If no main flow, nothing to layout!
        if (pageSequence.getMainFlow() != null) {
            PageSequenceLayoutManager pageSLM;
            pageSLM = getLayoutManagerMaker().makePageSequenceLayoutManager(
                    this, pageSequence);
            pageSLM.activateLayout();
            // preserve the current PageSequenceLayoutManger for the
            // force-page-count check at the beginning of the next PageSequence
            this.prevPageSeqLM = pageSLM;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startExternalDocument(final ExternalDocument document) {
        startAbstractPageSequence(document);
    }

    /** {@inheritDoc} */
    @Override
    public void endExternalDocument(final ExternalDocument document) {
        if (this.statistics != null) {
            this.statistics.end();
        }

        ExternalDocumentLayoutManager edLM;
        edLM = getLayoutManagerMaker().makeExternalDocumentLayoutManager(this,
                document);
        edLM.activateLayout();
        // preserve the current PageSequenceLayoutManger for the
        // force-page-count check at the beginning of the next PageSequence
        this.prevPageSeqLM = edLM;

    }

    /**
     * Called by the PageSequenceLayoutManager when it is finished with a
     * page-sequence.
     *
     * @param pageSequence
     *            the page-sequence just finished
     * @param pageCount
     *            The number of pages generated for the page-sequence
     */
    public void notifyPageSequenceFinished(
            final AbstractPageSequence pageSequence, final int pageCount) {
        this.results.haveFormattedPageSequence(pageSequence, pageCount);
        if (log.isDebugEnabled()) {
            log.debug("Last page-sequence produced " + pageCount + " pages.");
        }
    }

    /**
     * End the document.
     *
     * @throws SAXException
     *             if there is some error
     */
    @Override
    public void endDocument() throws SAXException {

        finishPrevPageSequence(null);
        // process fox:destination elements
        if (this.rootFObj != null) {
            final List<Destination> destinationList = this.rootFObj
                    .getDestinationList();
            if (destinationList != null) {
                while (destinationList.size() > 0) {
                    final Destination destination = destinationList.remove(0);
                    final DestinationData destinationData = new DestinationData(
                            destination);
                    addOffDocumentItem(destinationData);
                }
            }
            // process fo:bookmark-tree
            final BookmarkTree bookmarkTree = this.rootFObj.getBookmarkTree();
            if (bookmarkTree != null) {
                final BookmarkData data = new BookmarkData(bookmarkTree);
                addOffDocumentItem(data);
                if (!data.isResolved()) {
                    // bookmarks did not fully resolve, add anyway. (hacky?
                    // yeah)
                    this.model.handleOffDocumentItem(data);
                }
            }
            this.idTracker.signalIDProcessed(this.rootFObj.getId());
        }
        this.model.endDocument();

        if (this.statistics != null) {
            this.statistics.logResults();
        }
    }

    /**
     * Add a OffDocumentItem to the area tree model. This checks if the
     * OffDocumentItem is resolvable and attempts to resolve or add the
     * resolvable ids for later resolution.
     *
     * @param odi
     *            the OffDocumentItem to add.
     */
    private void addOffDocumentItem(final OffDocumentItem odi) {
        if (odi instanceof Resolvable) {
            final Resolvable res = (Resolvable) odi;
            final String[] ids = res.getIDRefs();
            for (final String id : ids) {
                final List<PageViewport> pageVPList = this.idTracker
                        .getPageViewportsContainingID(id);
                if (pageVPList != null && !pageVPList.isEmpty()) {
                    res.resolveIDRef(id, pageVPList);
                } else {
                    final AreaEventProducer eventProducer = AreaEventProducer.Provider
                            .get(getUserAgent().getEventBroadcaster());
                    eventProducer
                    .unresolvedIDReference(this, odi.getName(), id);
                    this.idTracker.addUnresolvedIDRef(id, res);
                }
            }
            // check to see if ODI is now fully resolved, if so process it
            if (res.isResolved()) {
                this.model.handleOffDocumentItem(odi);
            }
        } else {
            this.model.handleOffDocumentItem(odi);
        }
    }

    /**
     * Generates and returns a unique key for a page viewport.
     *
     * @return the generated key.
     */
    public String generatePageViewportKey() {
        this.idGen++;
        return "P" + this.idGen;
    }

    /**
     * Tie a PageViewport with an ID found on a child area of the PV. Note that
     * an area with a given ID may be on more than one PV, hence an ID may have
     * more than one PV associated with it.
     *
     * @param id
     *            the property ID of the area
     * @param pv
     *            a page viewport that contains the area with this ID
     * @deprecated use getIDTracker().associateIDWithPageViewport(id, pv)
     *             instead
     */
    @Deprecated
    public void associateIDWithPageViewport(final String id,
            final PageViewport pv) {
        this.idTracker.associateIDWithPageViewport(id, pv);
    }

    /**
     * This method tie an ID to the areaTreeHandler until this one is ready to
     * be processed. This is used in page-number-citation-last processing so we
     * know when an id can be resolved.
     *
     * @param id
     *            the id of the object being processed
     * @deprecated use getIDTracker().signalPendingID(id) instead
     */
    @Deprecated
    public void signalPendingID(final String id) {
        this.idTracker.signalPendingID(id);
    }

    /**
     * Signals that all areas for the formatting object with the given ID have
     * been generated. This is used to determine when page-number-citation-last
     * ref-ids can be resolved.
     *
     * @param id
     *            the id of the formatting object which was just finished
     * @deprecated use getIDTracker().signalIDProcessed(id) instead
     */
    @Deprecated
    public void signalIDProcessed(final String id) {
        this.idTracker.signalIDProcessed(id);
    }

    /**
     * Check if an ID has already been resolved
     *
     * @param id
     *            the id to check
     * @return true if the ID has been resolved
     * @deprecated use getIDTracker().alreadyResolvedID(id) instead
     */
    @Deprecated
    public boolean alreadyResolvedID(final String id) {
        return this.idTracker.alreadyResolvedID(id);
    }

    /**
     * Tries to resolve all unresolved ID references on the given page.
     *
     * @param pv
     *            page viewport whose ID refs to resolve
     * @deprecated use getIDTracker().tryIDResolution(pv) instead
     */
    @Deprecated
    public void tryIDResolution(final PageViewport pv) {
        this.idTracker.tryIDResolution(pv);
    }

    /**
     * Get the set of page viewports that have an area with a given id.
     *
     * @param id
     *            the id to lookup
     * @return the list of PageViewports
     * @deprecated use getIDTracker().getPageViewportsContainingID(id) instead
     */
    @Deprecated
    public List<PageViewport> getPageViewportsContainingID(final String id) {
        return this.idTracker.getPageViewportsContainingID(id);
    }

    /**
     * Add an Resolvable object with an unresolved idref
     *
     * @param idref
     *            the idref whose target id has not yet been located
     * @param res
     *            the Resolvable object needing the idref to be resolved
     * @deprecated use getIDTracker().addUnresolvedIDRef(idref, res) instead
     */
    @Deprecated
    public void addUnresolvedIDRef(final String idref, final Resolvable res) {
        this.idTracker.addUnresolvedIDRef(idref, res);
    }

    private class Statistics {
        // for statistics gathering
        private final Runtime runtime;

        // heap memory allocated (for statistics)
        private long initialMemory;

        // time used in rendering (for statistics)
        private long startTime;

        /**
         * Default constructor
         */
        protected Statistics() {
            this.runtime = Runtime.getRuntime();
        }

        /**
         * starts the area tree handler statistics gathering
         */
        protected void start() {
            this.initialMemory = this.runtime.totalMemory()
                    - this.runtime.freeMemory();
            this.startTime = System.currentTimeMillis();
        }

        /**
         * ends the area tree handler statistics gathering
         */
        protected void end() {
            final long memoryNow = this.runtime.totalMemory()
                    - this.runtime.freeMemory();
            log.debug("Current heap size: " + memoryNow / 1024L + "KB");
        }

        /**
         * logs the results of the area tree handler statistics gathering
         */
        protected void logResults() {
            final long memoryNow = this.runtime.totalMemory()
                    - this.runtime.freeMemory();
            final long memoryUsed = (memoryNow - this.initialMemory) / 1024L;
            final long timeUsed = System.currentTimeMillis() - this.startTime;
            final int pageCount = AreaTreeHandler.this.rootFObj
                    .getTotalPagesGenerated();
            log.debug("Initial heap size: " + this.initialMemory / 1024L + "KB");
            log.debug("Current heap size: " + memoryNow / 1024L + "KB");
            log.debug("Total memory used: " + memoryUsed + "KB");
            log.debug("Total time used: " + timeUsed + "ms");
            log.debug("Pages rendered: " + pageCount);
            if (pageCount > 0) {
                final long perPage = timeUsed / pageCount;
                final long ppm = timeUsed != 0 ? Math.round(60000 * pageCount
                        / (double) timeUsed) : -1;
                log.debug("Avg render time: " + perPage + "ms/page (" + ppm
                        + "pages/min)");
            }
        }
    }
}
