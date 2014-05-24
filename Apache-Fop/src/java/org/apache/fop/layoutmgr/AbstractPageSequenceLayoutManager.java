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

/* $Id: AbstractPageSequenceLayoutManager.java 1229622 2012-01-10 16:14:05Z cbowditch $ */

package org.apache.fop.layoutmgr;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.AreaTreeHandler;
import org.apache.fop.area.AreaTreeModel;
import org.apache.fop.area.IDTracker;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.Resolvable;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.flow.Marker;
import org.apache.fop.fo.flow.RetrieveMarker;
import org.apache.fop.fo.pagination.AbstractPageSequence;

/**
 * Abstract base class for a page sequence layout manager.
 */
@Slf4j
public abstract class AbstractPageSequenceLayoutManager extends
AbstractLayoutManager implements TopLevelLayoutManager {

    /**
     * AreaTreeHandler which activates the PSLM and controls the rendering of
     * its pages.
     */
    protected AreaTreeHandler areaTreeHandler;

    /** ID tracker supplied by the AreaTreeHandler */
    protected IDTracker idTracker;

    /** page sequence formatting object being processed by this class */
    protected AbstractPageSequence pageSeq;

    /** Current page with page-viewport-area being filled by the PSLM. */
    protected Page curPage;

    /** the current page number */
    protected int currentPageNum = 0;
    /** The stating page number */
    protected int startPageNum = 0;

    /**
     * Constructor
     *
     * @param ath
     *            the area tree handler object
     * @param pseq
     *            fo:page-sequence to process
     */
    public AbstractPageSequenceLayoutManager(final AreaTreeHandler ath,
            final AbstractPageSequence pseq) {
        super(pseq);
        this.areaTreeHandler = ath;
        this.idTracker = ath.getIDTracker();
        this.pageSeq = pseq;
    }

    /**
     * @return the LayoutManagerMaker object associated to the areaTreeHandler
     */
    public LayoutManagerMaker getLayoutManagerMaker() {
        return this.areaTreeHandler.getLayoutManagerMaker();
    }

    /**
     * Provides access to the current page.
     *
     * @return the current Page
     */
    @Override
    public Page getCurrentPage() {
        return this.curPage;
    }

    /**
     * Provides access for setting the current page.
     *
     * @param currentPage
     *            the new current Page
     */
    protected void setCurrentPage(final Page currentPage) {
        this.curPage = currentPage;
    }

    /**
     * Provides access to the current page number
     *
     * @return the current page number
     */
    protected int getCurrentPageNum() {
        return this.currentPageNum;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        this.startPageNum = this.pageSeq.getStartingPageNumber();
        this.currentPageNum = this.startPageNum - 1;
    }

    /**
     * This returns the first PageViewport that contains an id trait matching
     * the idref argument, or null if no such PV exists.
     *
     * @param idref
     *            the idref trait needing to be resolved
     * @return the first PageViewport that contains the ID trait
     */
    public PageViewport getFirstPVWithID(final String idref) {
        final List list = this.idTracker.getPageViewportsContainingID(idref);
        if (list != null && list.size() > 0) {
            return (PageViewport) list.get(0);
        }
        return null;
    }

    /**
     * This returns the last PageViewport that contains an id trait matching the
     * idref argument, or null if no such PV exists.
     *
     * @param idref
     *            the idref trait needing to be resolved
     * @return the last PageViewport that contains the ID trait
     */
    public PageViewport getLastPVWithID(final String idref) {
        final List list = this.idTracker.getPageViewportsContainingID(idref);
        if (list != null && list.size() > 0) {
            return (PageViewport) list.get(list.size() - 1);
        }
        return null;
    }

    /**
     * Add an ID reference to the current page. When adding areas the area adds
     * its ID reference. For the page layout manager it adds the id reference
     * with the current page to the area tree.
     *
     * @param id
     *            the ID reference to add
     */
    public void addIDToPage(final String id) {
        if (id != null && id.length() > 0) {
            this.idTracker.associateIDWithPageViewport(id,
                    this.curPage.getPageViewport());
        }
    }

    /**
     * Add an id reference of the layout manager in the AreaTreeHandler, if the
     * id hasn't been resolved yet
     *
     * @param id
     *            the id to track
     * @return a boolean indicating if the id has already been resolved TODO
     *         Maybe give this a better name
     */
    public boolean associateLayoutManagerID(final String id) {
        if (log.isDebugEnabled()) {
            log.debug("associateLayoutManagerID(" + id + ")");
        }
        if (!this.idTracker.alreadyResolvedID(id)) {
            this.idTracker.signalPendingID(id);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Notify the areaTreeHandler that the LayoutManagers containing idrefs have
     * finished creating areas
     *
     * @param id
     *            the id for which layout has finished
     */
    public void notifyEndOfLayout(final String id) {
        this.idTracker.signalIDProcessed(id);
    }

    /**
     * Identify an unresolved area (one needing an idref to be resolved, e.g.
     * the internal-destination of an fo:basic-link) for both the
     * AreaTreeHandler and PageViewport object.
     *
     * The IDTracker keeps a document-wide list of idref's and the PV's needing
     * them to be resolved. It uses this to send notifications to the PV's when
     * an id has been resolved.
     *
     * The PageViewport keeps lists of id's needing resolving, along with the
     * child areas (page-number-citation, basic-link, etc.) of the PV needing
     * their resolution.
     *
     * @param id
     *            the ID reference to add
     * @param res
     *            the resolvable object that needs resolving
     */
    public void addUnresolvedArea(final String id, final Resolvable res) {
        this.curPage.getPageViewport().addUnresolvedIDRef(id, res);
        this.idTracker.addUnresolvedIDRef(id, this.curPage.getPageViewport());
    }

    /**
     * Bind the RetrieveMarker to the corresponding Marker subtree. If the
     * boundary is page then it will only check the current page. For
     * page-sequence and document it will lookup preceding pages from the area
     * tree and try to find a marker. If we retrieve a marker from a preceding
     * page, then the containing page does not have a qualifying area, and all
     * qualifying areas have ended. Therefore we use last-ending-within-page
     * (Constants.EN_LEWP) as the position.
     *
     * @param rm
     *            the RetrieveMarker instance whose properties are to used to
     *            find the matching Marker.
     * @return a bound RetrieveMarker instance, or null if no Marker could be
     *         found.
     */
    public RetrieveMarker resolveRetrieveMarker(final RetrieveMarker rm) {
        final AreaTreeModel areaTreeModel = this.areaTreeHandler
                .getAreaTreeModel();
        final String name = rm.getRetrieveClassName();
        final int pos = rm.getRetrievePosition();
        final int boundary = rm.getRetrieveBoundary();

        // get marker from the current markers on area tree
        Marker mark = getCurrentPV().getMarker(name, pos);
        if (mark == null && boundary != EN_PAGE) {
            // go back over pages until mark found
            // if document boundary then keep going
            final boolean doc = boundary == EN_DOCUMENT;
            int seq = areaTreeModel.getPageSequenceCount();
            int page = areaTreeModel.getPageCount(seq) - 1;
            while (page < 0 && doc && seq > 1) {
                seq--;
                page = areaTreeModel.getPageCount(seq) - 1;
            }
            while (page >= 0) {
                final PageViewport pv = areaTreeModel.getPage(seq, page);
                mark = pv.getMarker(name, Constants.EN_LEWP);
                if (mark != null) {
                    break;
                }
                page--;
                if (page < 0 && doc && seq > 1) {
                    seq--;
                    page = areaTreeModel.getPageCount(seq) - 1;
                }
            }
        }

        if (mark == null) {
            log.debug("found no marker with name: " + name);
            return null;
        } else {
            rm.bindMarker(mark);
            return rm;
        }
    }

    /**
     * Creates and returns a new page.
     *
     * @param pageNumber
     *            the page number
     * @param isBlank
     *            true if it's a blank page
     * @return the newly created page
     */
    protected abstract Page createPage(final int pageNumber,
            final boolean isBlank);

    /**
     * Makes a new page
     *
     * @param isBlank
     *            whether this page is blank or not
     * @return a new page
     */
    protected Page makeNewPage(final boolean isBlank) {
        if (this.curPage != null) {
            finishPage();
        }

        this.currentPageNum++;

        this.curPage = createPage(this.currentPageNum, isBlank);

        if (log.isDebugEnabled()) {
            log.debug("["
                    + this.curPage.getPageViewport().getPageNumberString()
                    + (isBlank ? "*" : "") + "]");
        }

        addIDToPage(this.pageSeq.getRoot().getId());
        addIDToPage(this.pageSeq.getId());
        return this.curPage;
    }

    /**
     * Finishes a page in preparation for a new page.
     */
    protected void finishPage() {
        if (log.isTraceEnabled()) {
            this.curPage.getPageViewport().dumpMarkers();
        }

        // Try to resolve any unresolved IDs for the current page.
        //
        this.idTracker.tryIDResolution(this.curPage.getPageViewport());
        // Queue for ID resolution and rendering
        this.areaTreeHandler.getAreaTreeModel().addPage(
                this.curPage.getPageViewport());
        if (log.isDebugEnabled()) {
            log.debug("page finished: "
                    + this.curPage.getPageViewport().getPageNumberString()
                    + ", current num: " + this.currentPageNum);
        }
        this.curPage = null;
    }

    /** {@inheritDoc} */
    @Override
    public void doForcePageCount(final Numeric nextPageSeqInitialPageNumber) {

        int forcePageCount = this.pageSeq.getForcePageCount();

        // xsl-spec version 1.0 (15.oct 2001)
        // auto | even | odd | end-on-even | end-on-odd | no-force | inherit
        // auto:
        // Force the last page in this page-sequence to be an odd-page
        // if the initial-page-number of the next page-sequence is even.
        // Force it to be an even-page
        // if the initial-page-number of the next page-sequence is odd.
        // If there is no next page-sequence
        // or if the value of its initial-page-number is "auto" do not force any
        // page.

        // if force-page-count is auto then set the value of forcePageCount
        // depending on the initial-page-number of the next page-sequence
        if (nextPageSeqInitialPageNumber != null
                && forcePageCount == Constants.EN_AUTO) {
            if (nextPageSeqInitialPageNumber.getEnum() != 0) {
                // auto | auto-odd | auto-even
                final int nextPageSeqPageNumberType = nextPageSeqInitialPageNumber
                        .getEnum();
                if (nextPageSeqPageNumberType == Constants.EN_AUTO_ODD) {
                    forcePageCount = Constants.EN_END_ON_EVEN;
                } else if (nextPageSeqPageNumberType == Constants.EN_AUTO_EVEN) {
                    forcePageCount = Constants.EN_END_ON_ODD;
                } else { // auto
                    forcePageCount = Constants.EN_NO_FORCE;
                }
            } else { // <integer> for explicit page number
                int nextPageSeqPageStart = nextPageSeqInitialPageNumber
                        .getValue();
                // spec rule
                nextPageSeqPageStart = nextPageSeqPageStart > 0 ? nextPageSeqPageStart
                        : 1;
                if (nextPageSeqPageStart % 2 == 0) { // explicit even
                    // startnumber
                    forcePageCount = Constants.EN_END_ON_ODD;
                } else { // explicit odd startnumber
                    forcePageCount = Constants.EN_END_ON_EVEN;
                }
            }
        }

        if (forcePageCount == Constants.EN_EVEN) {
            if ((this.currentPageNum - this.startPageNum + 1) % 2 != 0) { // we
                // have
                // an
                // odd
                // number
                // of
                // pages
                this.curPage = makeNewPage(true);
            }
        } else if (forcePageCount == Constants.EN_ODD) {
            if ((this.currentPageNum - this.startPageNum + 1) % 2 == 0) { // we
                // have
                // an
                // even
                // number
                // of
                // pages
                this.curPage = makeNewPage(true);
            }
        } else if (forcePageCount == Constants.EN_END_ON_EVEN) {
            if (this.currentPageNum % 2 != 0) { // we are now on an odd page
                this.curPage = makeNewPage(true);
            }
        } else if (forcePageCount == Constants.EN_END_ON_ODD) {
            if (this.currentPageNum % 2 == 0) { // we are now on an even page
                this.curPage = makeNewPage(true);
            }
        } else if (forcePageCount == Constants.EN_NO_FORCE) {
            // i hope: nothing special at all
        }

        if (this.curPage != null) {
            finishPage();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        throw new IllegalStateException();
    }

}
