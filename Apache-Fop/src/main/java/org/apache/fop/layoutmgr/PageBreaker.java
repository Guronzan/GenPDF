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

/* $Id: PageBreaker.java 1229622 2012-01-10 16:14:05Z cbowditch $ */

package org.apache.fop.layoutmgr;

import java.util.List;
import java.util.ListIterator;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Block;
import org.apache.fop.area.BodyRegion;
import org.apache.fop.area.Footnote;
import org.apache.fop.area.PageViewport;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.pagination.RegionBody;
import org.apache.fop.fo.pagination.StaticContent;
import org.apache.fop.layoutmgr.PageBreakingAlgorithm.PageBreakingLayoutListener;
import org.apache.fop.traits.MinOptMax;

/**
 * Handles the breaking of pages in an fo:flow
 */
@Slf4j
public class PageBreaker extends AbstractBreaker {

    private final PageSequenceLayoutManager pslm;
    private boolean firstPart = true;
    private boolean pageBreakHandled;
    private boolean needColumnBalancing;
    private final PageProvider pageProvider;
    private Block separatorArea;
    private boolean spanAllActive;

    /**
     * The FlowLayoutManager object, which processes the single fo:flow of the
     * fo:page-sequence
     */
    private FlowLayoutManager childFLM = null;

    private StaticContentLayoutManager footnoteSeparatorLM = null;

    /**
     * Construct page breaker.
     *
     * @param pslm
     *            the page sequence layout manager
     */
    public PageBreaker(final PageSequenceLayoutManager pslm) {
        this.pslm = pslm;
        this.pageProvider = pslm.getPageProvider();
        this.childFLM = pslm.getLayoutManagerMaker().makeFlowLayoutManager(
                pslm, pslm.getPageSequence().getMainFlow());
    }

    /** {@inheritDoc} */
    @Override
    protected void updateLayoutContext(final LayoutContext context) {
        final int flowIPD = this.pslm.getCurrentPV().getCurrentSpan()
                .getColumnWidth();
        context.setRefIPD(flowIPD);
    }

    /** {@inheritDoc} */
    @Override
    protected LayoutManager getTopLevelLM() {
        return this.pslm;
    }

    /** {@inheritDoc} */
    @Override
    protected PageProvider getPageProvider() {
        return this.pslm.getPageProvider();
    }

    /**
     * Starts the page breaking process.
     *
     * @param flowBPD
     *            the constant available block-progression-dimension (used for
     *            every part)
     */
    void doLayout(final int flowBPD) {
        doLayout(flowBPD, false);
    }

    /** {@inheritDoc} */
    @Override
    protected PageBreakingLayoutListener createLayoutListener() {
        return new PageBreakingLayoutListener() {

            @Override
            public void notifyOverflow(final int part, final int amount,
                    final FObj obj) {
                final Page p = PageBreaker.this.pageProvider.getPage(false,
                        part, PageProvider.RELTO_CURRENT_ELEMENT_LIST);
                final RegionBody body = (RegionBody) p.getSimplePageMaster()
                        .getRegion(Constants.FO_REGION_BODY);
                final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                        .get(body.getUserAgent().getEventBroadcaster());

                final boolean canRecover = body.getOverflow() != Constants.EN_ERROR_IF_OVERFLOW;
                final boolean needClip = body.getOverflow() == Constants.EN_HIDDEN
                        || body.getOverflow() == Constants.EN_ERROR_IF_OVERFLOW;
                eventProducer.regionOverflow(this, body.getName(), p
                        .getPageViewport().getPageNumberString(), amount,
                        needClip, canRecover, body.getLocator());
            }

        };
    }

    /** {@inheritDoc} */
    @Override
    protected int handleSpanChange(final LayoutContext childLC,
            int nextSequenceStartsOn) {
        this.needColumnBalancing = false;
        if (childLC.getNextSpan() != Constants.NOT_SET) {
            // Next block list will have a different span.
            nextSequenceStartsOn = childLC.getNextSpan();
            this.needColumnBalancing = childLC.getNextSpan() == Constants.EN_ALL
                    && childLC.getDisableColumnBalancing() == Constants.EN_FALSE;

        }
        if (this.needColumnBalancing) {
            log.debug("Column balancing necessary for the next element list!!!");
        }
        return nextSequenceStartsOn;
    }

    /** {@inheritDoc} */
    @Override
    protected int getNextBlockList(final LayoutContext childLC,
            final int nextSequenceStartsOn) {
        return getNextBlockList(childLC, nextSequenceStartsOn, null, null, null);
    }

    /** {@inheritDoc} */
    @Override
    protected int getNextBlockList(final LayoutContext childLC,
            final int nextSequenceStartsOn, final Position positionAtIPDChange,
            final LayoutManager restartLM, final List firstElements) {
        if (!this.firstPart) {
            // if this is the first page that will be created by
            // the current BlockSequence, it could have a break
            // condition that must be satisfied;
            // otherwise, we may simply need a new page
            handleBreakTrait(nextSequenceStartsOn);
        }
        this.firstPart = false;
        this.pageBreakHandled = true;

        this.pageProvider.setStartOfNextElementList(
                this.pslm.getCurrentPageNum(), this.pslm.getCurrentPV()
                .getCurrentSpan().getCurrentFlowIndex(),
                this.spanAllActive);
        return super.getNextBlockList(childLC, nextSequenceStartsOn,
                positionAtIPDChange, restartLM, firstElements);
    }

    private boolean containsFootnotes(final List contentList,
            final LayoutContext context) {

        boolean containsFootnotes = false;
        if (contentList != null) {
            final ListIterator contentListIterator = contentList.listIterator();
            while (contentListIterator.hasNext()) {
                final ListElement element = (ListElement) contentListIterator
                        .next();
                if (element instanceof KnuthBlockBox
                        && ((KnuthBlockBox) element).hasAnchors()) {
                    // element represents a line with footnote citations
                    containsFootnotes = true;
                    final LayoutContext footnoteContext = new LayoutContext(
                            context);
                    footnoteContext.setStackLimitBP(context.getStackLimitBP());
                    footnoteContext.setRefIPD(this.pslm.getCurrentPV()
                            .getRegionReference(Constants.FO_REGION_BODY)
                            .getIPD());
                    final List footnoteBodyLMs = ((KnuthBlockBox) element)
                            .getFootnoteBodyLMs();
                    final ListIterator footnoteBodyIterator = footnoteBodyLMs
                            .listIterator();
                    // store the lists of elements representing the footnote
                    // bodies
                    // in the box representing the line containing their
                    // references
                    while (footnoteBodyIterator.hasNext()) {
                        final FootnoteBodyLayoutManager fblm = (FootnoteBodyLayoutManager) footnoteBodyIterator
                                .next();
                        fblm.setParent(this.childFLM);
                        fblm.initialize();
                        ((KnuthBlockBox) element).addElementList(fblm
                                .getNextKnuthElements(footnoteContext,
                                        this.alignment));
                    }
                }
            }
        }
        return containsFootnotes;
    }

    private void handleFootnoteSeparator() {
        StaticContent footnoteSeparator;
        footnoteSeparator = this.pslm.getPageSequence().getStaticContent(
                "xsl-footnote-separator");
        if (footnoteSeparator != null) {
            // the footnote separator can contain page-dependent content such as
            // page numbers or retrieve markers, so its areas cannot simply be
            // obtained now and repeated in each page;
            // we need to know in advance the separator bpd: the actual
            // separator
            // could be different from page to page, but its bpd would likely be
            // always the same

            // create a Block area that will contain the separator areas
            this.separatorArea = new Block();
            this.separatorArea.setIPD(this.pslm.getCurrentPV()
                    .getRegionReference(Constants.FO_REGION_BODY).getIPD());
            // create a StaticContentLM for the footnote separator
            this.footnoteSeparatorLM = this.pslm.getLayoutManagerMaker()
                    .makeStaticContentLayoutManager(this.pslm,
                            footnoteSeparator, this.separatorArea);
            this.footnoteSeparatorLM.doLayout();

            this.footnoteSeparatorLength = MinOptMax
                    .getInstance(this.separatorArea.getBPD());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected List getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        List contentList = null;

        while (!this.childFLM.isFinished() && contentList == null) {
            contentList = this.childFLM
                    .getNextKnuthElements(context, alignment);
        }

        // scan contentList, searching for footnotes
        if (containsFootnotes(contentList, context)) {
            // handle the footnote separator
            handleFootnoteSeparator();
        }
        return contentList;
    }

    /** {@inheritDoc} */
    @Override
    protected List getNextKnuthElements(final LayoutContext context,
            final int alignment, final Position positionAtIPDChange,
            final LayoutManager restartAtLM) {
        List contentList = null;

        do {
            contentList = this.childFLM.getNextKnuthElements(context,
                    alignment, positionAtIPDChange, restartAtLM);
        } while (!this.childFLM.isFinished() && contentList == null);

        // scan contentList, searching for footnotes
        if (containsFootnotes(contentList, context)) {
            // handle the footnote separator
            handleFootnoteSeparator();
        }
        return contentList;
    }

    /**
     * @return current display alignment
     */
    @Override
    protected int getCurrentDisplayAlign() {
        return this.pslm.getCurrentPage().getSimplePageMaster()
                .getRegion(Constants.FO_REGION_BODY).getDisplayAlign();
    }

    /**
     * @return whether or not this flow has more page break opportunities
     */
    @Override
    protected boolean hasMoreContent() {
        return !this.childFLM.isFinished();
    }

    /**
     * Adds an area to the flow layout manager
     *
     * @param posIter
     *            the position iterator
     * @param context
     *            the layout context
     */
    @Override
    protected void addAreas(final PositionIterator posIter,
            final LayoutContext context) {
        if (this.footnoteSeparatorLM != null) {
            final StaticContent footnoteSeparator = this.pslm.getPageSequence()
                    .getStaticContent("xsl-footnote-separator");
            // create a Block area that will contain the separator areas
            this.separatorArea = new Block();
            this.separatorArea.setIPD(this.pslm.getCurrentPV()
                    .getRegionReference(Constants.FO_REGION_BODY).getIPD());
            // create a StaticContentLM for the footnote separator
            this.footnoteSeparatorLM = this.pslm.getLayoutManagerMaker()
                    .makeStaticContentLayoutManager(this.pslm,
                            footnoteSeparator, this.separatorArea);
            this.footnoteSeparatorLM.doLayout();
        }

        this.childFLM.addAreas(posIter, context);
    }

    /**
     * {@inheritDoc} This implementation checks whether to trigger
     * column-balancing, or whether to take into account a 'last-page'
     * condition.
     */
    @Override
    protected void doPhase3(final PageBreakingAlgorithm alg,
            final int partCount, final BlockSequence originalList,
            final BlockSequence effectiveList) {

        if (this.needColumnBalancing) {
            // column balancing for the last part
            redoLayout(alg, partCount, originalList, effectiveList);
            return;
        }

        final boolean lastPageMasterDefined = this.pslm.getPageSequence()
                .hasPagePositionLast();
        if (!hasMoreContent()) {
            // last part is reached
            if (lastPageMasterDefined) {
                // last-page condition
                redoLayout(alg, partCount, originalList, effectiveList);
                return;
            }
        }

        // nothing special: just add the areas now
        addAreas(alg, partCount, originalList, effectiveList);
    }

    /**
     * Restart the algorithm at the break corresponding to the given partCount.
     * Used to re-do the part after the last break in case of either
     * column-balancing or a last page-master.
     */
    private void redoLayout(final PageBreakingAlgorithm alg,
            final int partCount, final BlockSequence originalList,
            final BlockSequence effectiveList) {

        int newStartPos = 0;
        final int restartPoint = this.pageProvider
                .getStartingPartIndexForLastPage(partCount);
        if (restartPoint > 0) {
            // Add definitive areas for the parts before the
            // restarting point
            addAreas(alg, restartPoint, originalList, effectiveList);
            // Get page break from which we restart
            final PageBreakPosition pbp = alg.getPageBreaks().get(
                    restartPoint - 1);
            newStartPos = pbp.getLeafPos() + 1;
            // Handle page break right here to avoid any side-effects
            if (newStartPos > 0) {
                handleBreakTrait(Constants.EN_PAGE);
            }
        }

        log.debug("Restarting at " + restartPoint + ", new start position: "
                + newStartPos);

        this.pageBreakHandled = true;
        // Update so the available BPD is reported correctly
        final int currentPageNum = this.pslm.getCurrentPageNum();

        this.pageProvider.setStartOfNextElementList(currentPageNum, this.pslm
                .getCurrentPV().getCurrentSpan().getCurrentFlowIndex(),
                this.spanAllActive);

        // Make sure we only add the areas we haven't added already
        effectiveList.ignoreAtStart = newStartPos;

        PageBreakingAlgorithm algRestart;
        if (this.needColumnBalancing) {
            log.debug("Column balancing now!!!");
            log.debug("===================================================");

            // Restart last page
            algRestart = new BalancingColumnBreakingAlgorithm(getTopLevelLM(),
                    getPageProvider(), createLayoutListener(), this.alignment,
                    Constants.EN_START, this.footnoteSeparatorLength,
                    isPartOverflowRecoveryActivated(), this.pslm.getCurrentPV()
                    .getBodyRegion().getColumnCount());
            log.debug("===================================================");
        } else {
            // Handle special page-master for last page
            final BodyRegion currentBody = this.pageProvider
                    .getPage(false, currentPageNum).getPageViewport()
                    .getBodyRegion();

            setLastPageIndex(currentPageNum);

            final BodyRegion lastBody = this.pageProvider
                    .getPage(false, currentPageNum).getPageViewport()
                    .getBodyRegion();
            lastBody.getMainReference().setSpans(
                    currentBody.getMainReference().getSpans());
            log.debug("Last page handling now!!!");
            log.debug("===================================================");
            // Restart last page
            algRestart = new PageBreakingAlgorithm(getTopLevelLM(),
                    getPageProvider(), createLayoutListener(),
                    alg.getAlignment(), alg.getAlignmentLast(),
                    this.footnoteSeparatorLength,
                    isPartOverflowRecoveryActivated(), false, false);
            log.debug("===================================================");
        }

        final int optimalPageCount = algRestart.findBreakingPoints(
                effectiveList, newStartPos, 1, true,
                BreakingAlgorithm.ALL_BREAKS);
        log.debug("restart: optimalPageCount= " + optimalPageCount
                + " pageBreaks.size()= " + algRestart.getPageBreaks().size());

        final boolean fitsOnePage = optimalPageCount <= this.pslm
                .getCurrentPV().getBodyRegion().getMainReference()
                .getCurrentSpan().getColumnCount();

        if (this.needColumnBalancing) {
            if (!fitsOnePage) {
                log.warn("Breaking algorithm produced more columns than are available.");
                /*
                 * reenable when everything works throw new
                 * IllegalStateException(
                 * "Breaking algorithm must not produce more columns than available."
                 * );
                 */
            }
        } else {
            if (fitsOnePage) {
                // Replace last page
                this.pslm.setCurrentPage(this.pageProvider.getPage(false,
                        currentPageNum));
            } else {
                // Last page-master cannot hold the content.
                // Add areas now...
                addAreas(alg, restartPoint, partCount - restartPoint,
                        originalList, effectiveList);
                // ...and add a blank last page
                setLastPageIndex(currentPageNum + 1);
                this.pslm.setCurrentPage(this.pslm.makeNewPage(true));
                return;
            }
        }

        addAreas(algRestart, optimalPageCount, originalList, effectiveList);
    }

    private void setLastPageIndex(final int currentPageNum) {
        final int lastPageIndex = this.pslm
                .getForcedLastPageNum(currentPageNum);
        this.pageProvider.setLastPageIndex(lastPageIndex);
    }

    /** {@inheritDoc} */
    @Override
    protected void startPart(final BlockSequence list, final int breakClass) {
        log.debug("startPart() breakClass=" + getBreakClassName(breakClass));
        if (this.pslm.getCurrentPage() == null) {
            throw new IllegalStateException("curPage must not be null");
        }
        if (!this.pageBreakHandled) {

            // firstPart is necessary because we need the first page before we
            // start the
            // algorithm so we have a BPD and IPD. This may subject to change
            // later when we
            // start handling more complex cases.
            if (!this.firstPart) {
                // if this is the first page that will be created by
                // the current BlockSequence, it could have a break
                // condition that must be satisfied;
                // otherwise, we may simply need a new page
                handleBreakTrait(breakClass);
            }
            this.pageProvider.setStartOfNextElementList(
                    this.pslm.getCurrentPageNum(), this.pslm.getCurrentPV()
                    .getCurrentSpan().getCurrentFlowIndex(),
                    this.spanAllActive);
        }
        this.pageBreakHandled = false;
        // add static areas and resolve any new id areas
        // finish page and add to area tree
        this.firstPart = false;
    }

    /** {@inheritDoc} */
    @Override
    protected void handleEmptyContent() {
        this.pslm.getCurrentPV().getPage().fakeNonEmpty();
    }

    /** {@inheritDoc} */
    @Override
    protected void finishPart(final PageBreakingAlgorithm alg,
            final PageBreakPosition pbp) {
        // add footnote areas
        if (pbp.footnoteFirstListIndex < pbp.footnoteLastListIndex
                || pbp.footnoteFirstElementIndex <= pbp.footnoteLastElementIndex) {
            // call addAreas() for each FootnoteBodyLM
            for (int i = pbp.footnoteFirstListIndex; i <= pbp.footnoteLastListIndex; i++) {
                final List elementList = alg.getFootnoteList(i);
                final int firstIndex = i == pbp.footnoteFirstListIndex ? pbp.footnoteFirstElementIndex
                        : 0;
                final int lastIndex = i == pbp.footnoteLastListIndex ? pbp.footnoteLastElementIndex
                        : elementList.size() - 1;

                SpaceResolver.performConditionalsNotification(elementList,
                        firstIndex, lastIndex, -1);
                final LayoutContext childLC = new LayoutContext(0);
                AreaAdditionUtil.addAreas(null, new KnuthPossPosIter(
                        elementList, firstIndex, lastIndex + 1), childLC);
            }
            // set the offset from the top margin
            final Footnote parentArea = this.pslm.getCurrentPV()
                    .getBodyRegion().getFootnote();
            int topOffset = this.pslm.getCurrentPV().getBodyRegion().getBPD()
                    - parentArea.getBPD();
            if (this.separatorArea != null) {
                topOffset -= this.separatorArea.getBPD();
            }
            parentArea.setTop(topOffset);
            parentArea.setSeparator(this.separatorArea);
        }
        this.pslm.getCurrentPV().getCurrentSpan().notifyFlowsFinished();
    }

    /** {@inheritDoc} */
    @Override
    protected LayoutManager getCurrentChildLM() {
        return this.childFLM;
    }

    /** {@inheritDoc} */
    @Override
    protected void observeElementList(final List elementList) {
        ElementListObserver.observe(elementList, "breaker", this.pslm.getFObj()
                .getId());
    }

    /**
     * Depending on the kind of break condition, move to next column or page.
     * May need to make an empty page if next page would not have the desired
     * "handedness".
     *
     * @param breakVal
     *            - value of break-before or break-after trait.
     */
    private void handleBreakTrait(final int breakVal) {
        Page curPage = this.pslm.getCurrentPage();
        switch (breakVal) {
        case Constants.EN_ALL:
            // break due to span change in multi-column layout
            curPage.getPageViewport().createSpan(true);
            this.spanAllActive = true;
            return;
        case Constants.EN_NONE:
            curPage.getPageViewport().createSpan(false);
            this.spanAllActive = false;
            return;
        case Constants.EN_COLUMN:
        case Constants.EN_AUTO:
        case Constants.EN_PAGE:
        case -1:
            final PageViewport pv = curPage.getPageViewport();

            // Check if previous page was spanned
            boolean forceNewPageWithSpan = false;
            final RegionBody rb = (RegionBody) curPage.getSimplePageMaster()
                    .getRegion(Constants.FO_REGION_BODY);
            forceNewPageWithSpan = rb.getColumnCount() > 1
                    && pv.getCurrentSpan().getColumnCount() == 1;

            if (forceNewPageWithSpan) {
                log.trace("Forcing new page with span");
                curPage = this.pslm.makeNewPage(false);
                curPage.getPageViewport().createSpan(true);
            } else if (pv.getCurrentSpan().hasMoreFlows()) {
                log.trace("Moving to next flow");
                pv.getCurrentSpan().moveToNextFlow();
            } else {
                log.trace("Making new page");
                /* curPage = */this.pslm.makeNewPage(false);
            }
            return;
        default:
            log.debug("handling break-before after page "
                    + this.pslm.getCurrentPageNum() + " breakVal="
                    + getBreakClassName(breakVal));
            if (needBlankPageBeforeNew(breakVal)) {
                log.trace("Inserting blank page");
                /* curPage = */this.pslm.makeNewPage(true);
            }
            if (needNewPage(breakVal)) {
                log.trace("Making new page");
                /* curPage = */this.pslm.makeNewPage(false);
            }
        }
    }

    /**
     * Check if a blank page is needed to accomodate desired even or odd page
     * number.
     *
     * @param breakVal
     *            - value of break-before or break-after trait.
     */
    private boolean needBlankPageBeforeNew(final int breakVal) {
        if (breakVal == Constants.EN_PAGE
                || this.pslm.getCurrentPage().getPageViewport().getPage()
                .isEmpty()) {
            // any page is OK or we already have an empty page
            return false;
        } else {
            /* IF we are on the kind of page we need, we'll need a new page. */
            if (this.pslm.getCurrentPageNum() % 2 == 0) { // even page
                return breakVal == Constants.EN_EVEN_PAGE;
            } else { // odd page
                return breakVal == Constants.EN_ODD_PAGE;
            }
        }
    }

    /**
     * See if need to generate a new page
     *
     * @param breakVal
     *            - value of break-before or break-after trait.
     */
    private boolean needNewPage(final int breakVal) {
        if (this.pslm.getCurrentPage().getPageViewport().getPage().isEmpty()) {
            if (breakVal == Constants.EN_PAGE) {
                return false;
            } else if (this.pslm.getCurrentPageNum() % 2 == 0) { // even page
                return breakVal == Constants.EN_ODD_PAGE;
            } else { // odd page
                return breakVal == Constants.EN_EVEN_PAGE;
            }
        } else {
            return true;
        }
    }
}
