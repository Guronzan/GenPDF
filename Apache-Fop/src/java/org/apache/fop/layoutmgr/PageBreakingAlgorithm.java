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

/* $Id: PageBreakingAlgorithm.java 1296496 2012-03-02 22:19:46Z gadams $ */

package org.apache.fop.layoutmgr;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FObj;
import org.apache.fop.layoutmgr.AbstractBreaker.PageBreakPosition;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.util.ListUtil;

@Slf4j
class PageBreakingAlgorithm extends BreakingAlgorithm {

    private final LayoutManager topLevelLM;
    private final PageProvider pageProvider;
    private final PageBreakingLayoutListener layoutListener;
    /** List of PageBreakPosition elements. */
    private LinkedList<PageBreakPosition> pageBreaks = null;

    /**
     * Footnotes which are cited between the currently considered active node
     * (previous break) and the current considered break. Its type is
     * List&lt;List&lt;KnuthElement&gt;&gt;, it contains the sequences of
     * KnuthElement representing the footnotes bodies.
     */
    private List<List<KnuthElement>> footnotesList = null;
    /** Cumulated bpd of unhandled footnotes. */
    private List<Integer> lengthList = null;
    /** Length of all the footnotes which will be put on the current page. */
    private int totalFootnotesLength = 0;
    /**
     * Length of all the footnotes which have already been inserted, up to the
     * currently considered element. That is, footnotes from the currently
     * considered page plus footnotes from its preceding pages.
     */
    private int insertedFootnotesLength = 0;

    /**
     * True if footnote citations have been met since the beginning of the page
     * sequence.
     */
    private boolean footnotesPending = false;
    /**
     * True if the elements met after the previous break point contain footnote
     * citations.
     */
    private boolean newFootnotes = false;
    /** Index of the first footnote met after the previous break point. */
    private int firstNewFootnoteIndex = 0;
    /** Index of the last footnote inserted on the current page. */
    private int footnoteListIndex = 0;
    /**
     * Index of the last element of the last footnote inserted on the current
     * page.
     */
    private int footnoteElementIndex = -1;

    // demerits for a page break that splits a footnote
    private final int splitFootnoteDemerits = 5000;
    // demerits for a page break that defers a whole footnote to the following
    // page
    private final int deferredFootnoteDemerits = 10000;
    private MinOptMax footnoteSeparatorLength = null;

    // the method noBreakBetween(int, int) uses these variables
    // to store parameters and result of the last call, in order
    // to reuse them and take less time
    private int storedPrevBreakIndex = -1;
    private int storedBreakIndex = -1;
    private boolean storedValue = false;

    // Controls whether overflows should be warned about or not
    private boolean autoHeight = false;

    // Controls whether a single part should be forced if possible (ex.
    // block-container)
    private boolean favorSinglePart = false;

    private int ipdDifference;
    private KnuthNode bestNodeForIPDChange;

    // Used to keep track of switches in keep-context
    private int currentKeepContext = Constants.EN_AUTO;
    private KnuthNode lastBeforeKeepContextSwitch;

    /**
     * Construct a page breaking algorithm.
     *
     * @param topLevelLM
     *            the top level layout manager
     * @param pageProvider
     *            the page provider
     * @param layoutListener
     *            the layout listener
     * @param alignment
     *            alignment of the paragraph/page. One of
     *            {@link Constants#EN_START}, {@link Constants#EN_JUSTIFY},
     *            {@link Constants#EN_CENTER}, {@link Constants#EN_END}. For
     *            pages, {@link Constants#EN_BEFORE} and
     *            {@link Constants#EN_AFTER} are mapped to the corresponding
     *            inline properties, {@link Constants#EN_START} and
     *            {@link Constants#EN_END}.
     * @param alignmentLast
     *            alignment of the paragraph's last line
     * @param footnoteSeparatorLength
     *            length of footnote separator
     * @param partOverflowRecovery
     *            {@code true} if too long elements should be moved to the next
     *            line/part
     * @param autoHeight
     *            true if auto height
     * @param favorSinglePart
     *            true if favoring single part
     * @see BreakingAlgorithm
     */
    public PageBreakingAlgorithm(
            final LayoutManager topLevelLM, // CSOK: ParameterNumber
            final PageProvider pageProvider,
            final PageBreakingLayoutListener layoutListener,
            final int alignment, final int alignmentLast,
            final MinOptMax footnoteSeparatorLength,
            final boolean partOverflowRecovery, final boolean autoHeight,
            final boolean favorSinglePart) {
        super(alignment, alignmentLast, true, partOverflowRecovery, 0);
        this.topLevelLM = topLevelLM;
        this.pageProvider = pageProvider;
        this.layoutListener = layoutListener;
        this.best = new BestPageRecords();
        this.footnoteSeparatorLength = footnoteSeparatorLength;
        this.autoHeight = autoHeight;
        this.favorSinglePart = favorSinglePart;
    }

    /**
     * This class represents a feasible breaking point with extra information
     * about footnotes.
     */
    protected class KnuthPageNode extends KnuthNode {

        /** Additional length due to footnotes. */
        public int totalFootnotes; // CSOK: VisibilityModifier

        /** Index of the last inserted footnote. */
        public int footnoteListIndex; // CSOK: VisibilityModifier

        /** Index of the last inserted element of the last inserted footnote. */
        public int footnoteElementIndex; // CSOK: VisibilityModifier

        public KnuthPageNode(
                final int position, // CSOK: ParameterNumber
                final int line, final int fitness, final int totalWidth,
                final int totalStretch, final int totalShrink,
                final int totalFootnotes, final int footnoteListIndex,
                final int footnoteElementIndex, final double adjustRatio,
                final int availableShrink, final int availableStretch,
                final int difference, final double totalDemerits,
                final KnuthNode previous) {
            super(position, line, fitness, totalWidth, totalStretch,
                    totalShrink, adjustRatio, availableShrink,
                    availableStretch, difference, totalDemerits, previous);
            this.totalFootnotes = totalFootnotes;
            this.footnoteListIndex = footnoteListIndex;
            this.footnoteElementIndex = footnoteElementIndex;
        }

    }

    /**
     * this class stores information about how the nodes which could start a
     * line ending at the current element
     */
    protected class BestPageRecords extends BestRecords {

        private final int[] bestFootnotesLength = new int[4];
        private final int[] bestFootnoteListIndex = new int[4];
        private final int[] bestFootnoteElementIndex = new int[4];

        @Override
        public void addRecord(final double demerits, final KnuthNode node,
                final double adjust, final int availableShrink,
                final int availableStretch, final int difference,
                final int fitness) {
            super.addRecord(demerits, node, adjust, availableShrink,
                    availableStretch, difference, fitness);
            this.bestFootnotesLength[fitness] = PageBreakingAlgorithm.this.insertedFootnotesLength;
            this.bestFootnoteListIndex[fitness] = PageBreakingAlgorithm.this.footnoteListIndex;
            this.bestFootnoteElementIndex[fitness] = PageBreakingAlgorithm.this.footnoteElementIndex;
        }

        public int getFootnotesLength(final int fitness) {
            return this.bestFootnotesLength[fitness];
        }

        public int getFootnoteListIndex(final int fitness) {
            return this.bestFootnoteListIndex[fitness];
        }

        public int getFootnoteElementIndex(final int fitness) {
            return this.bestFootnoteElementIndex[fitness];
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void initialize() {
        super.initialize();
        this.insertedFootnotesLength = 0;
        this.footnoteListIndex = 0;
        this.footnoteElementIndex = -1;
    }

    /**
     * Overridden to defer a part to the next page, if it must be kept within
     * one page, but is too large to fit in the last column. {@inheritDoc}
     */
    @Override
    protected KnuthNode recoverFromTooLong(final KnuthNode lastTooLong) {

        if (log.isDebugEnabled()) {
            log.debug("Recovering from too long: " + lastTooLong);
            log.debug("\tlastTooShort = " + getLastTooShort());
            log.debug("\tlastBeforeKeepContextSwitch = "
                    + this.lastBeforeKeepContextSwitch);
            log.debug("\tcurrentKeepContext = "
                    + AbstractBreaker
                    .getBreakClassName(this.currentKeepContext));
        }

        if (this.lastBeforeKeepContextSwitch == null
                || this.currentKeepContext == Constants.EN_AUTO) {
            return super.recoverFromTooLong(lastTooLong);
        }

        KnuthNode node = this.lastBeforeKeepContextSwitch;
        this.lastBeforeKeepContextSwitch = null;
        // content would overflow, insert empty page/column(s) and try again
        while (!this.pageProvider.endPage(node.line - 1)) {
            log.trace("Adding node for empty column");
            node = createNode(node.position, node.line + 1, 1, 0, 0, 0, 0, 0,
                    0, 0, 0, node);
        }
        return node;
    }

    /**
     * Compare two KnuthNodes and return the node with the least demerit.
     *
     * @param node1
     *            The first knuth node.
     * @param node2
     *            The other knuth node.
     * @return the node with the least demerit.
     */
    @Override
    protected KnuthNode compareNodes(final KnuthNode node1,
            final KnuthNode node2) {

        /* if either node is null, return the other one */
        if (node1 == null || node2 == null) {
            return node1 == null ? node2 : node1;
        }

        /*
         * if either one of the nodes corresponds to a mere column-break, and
         * the other one corresponds to a page-break, return the page-break node
         */
        if (this.pageProvider != null) {
            if (this.pageProvider.endPage(node1.line - 1)
                    && !this.pageProvider.endPage(node2.line - 1)) {
                return node1;
            } else if (this.pageProvider.endPage(node2.line - 1)
                    && !this.pageProvider.endPage(node1.line - 1)) {
                return node2;
            }
        }

        /* all other cases: use superclass implementation */
        return super.compareNodes(node1, node2);
    }

    /** {@inheritDoc} */
    @Override
    protected KnuthNode createNode(
            final int position, // CSOK: ParameterNumber
            final int line, final int fitness, final int totalWidth,
            final int totalStretch, final int totalShrink,
            final double adjustRatio, final int availableShrink,
            final int availableStretch, final int difference,
            final double totalDemerits, final KnuthNode previous) {
        return new KnuthPageNode(position, line, fitness, totalWidth,
                totalStretch, totalShrink, this.insertedFootnotesLength,
                this.footnoteListIndex, this.footnoteElementIndex, adjustRatio,
                availableShrink, availableStretch, difference, totalDemerits,
                previous);
    }

    /** {@inheritDoc} */
    @Override
    protected KnuthNode createNode(final int position, final int line,
            final int fitness, final int totalWidth, final int totalStretch,
            final int totalShrink) {
        return new KnuthPageNode(position, line, fitness, totalWidth,
                totalStretch, totalShrink,
                ((BestPageRecords) this.best).getFootnotesLength(fitness),
                ((BestPageRecords) this.best).getFootnoteListIndex(fitness),
                ((BestPageRecords) this.best).getFootnoteElementIndex(fitness),
                this.best.getAdjust(fitness),
                this.best.getAvailableShrink(fitness),
                this.best.getAvailableStretch(fitness),
                this.best.getDifference(fitness),
                this.best.getDemerits(fitness), this.best.getNode(fitness));
    }

    /**
     * Page-breaking specific handling of the given box. Currently it adds the
     * footnotes cited in the given box to the list of to-be-handled footnotes.
     * {@inheritDoc}
     */
    @Override
    protected void handleBox(final KnuthBox box) {
        super.handleBox(box);
        if (box instanceof KnuthBlockBox && ((KnuthBlockBox) box).hasAnchors()) {
            handleFootnotes(((KnuthBlockBox) box).getElementLists());
            if (!this.newFootnotes) {
                this.newFootnotes = true;
                this.firstNewFootnoteIndex = this.footnotesList.size() - 1;
            }
        }
    }

    /**
     * Overridden to consider penalties with value {@link KnuthElement#INFINITE}
     * as legal break-points, if the current keep-context allows this (a
     * keep-*.within-page="always" constraint still permits column-breaks)
     * {@inheritDoc}
     */
    @Override
    protected void handlePenaltyAt(final KnuthPenalty penalty,
            final int position, final int allowedBreaks) {
        super.handlePenaltyAt(penalty, position, allowedBreaks);
        /*
         * if the penalty had value INFINITE, default implementation will not
         * have considered it a legal break, but it could still be one.
         */
        if (penalty.getPenalty() == KnuthElement.INFINITE) {
            final int breakClass = penalty.getBreakClass();
            if (breakClass == Constants.EN_PAGE
                    || breakClass == Constants.EN_COLUMN) {
                considerLegalBreak(penalty, position);
            }
        }
    }

    /**
     * Handles the footnotes cited inside a block-level box. Updates
     * footnotesList and the value of totalFootnotesLength with the lengths of
     * the given footnotes.
     *
     * @param elementLists
     *            list of KnuthElement sequences corresponding to the footnotes
     *            bodies
     */
    private void handleFootnotes(final List<List<KnuthElement>> elementLists) {
        // initialization
        if (!this.footnotesPending) {
            this.footnotesPending = true;
            this.footnotesList = new ArrayList<List<KnuthElement>>();
            this.lengthList = new ArrayList<Integer>();
            this.totalFootnotesLength = 0;
        }
        if (!this.newFootnotes) {
            this.newFootnotes = true;
            this.firstNewFootnoteIndex = this.footnotesList.size();
        }

        // compute the total length of the footnotes
        for (final List<KnuthElement> noteList : elementLists) {

            // Space resolution (Note: this does not respect possible stacking
            // constraints
            // between footnotes!)
            SpaceResolver.resolveElementList(noteList);

            int noteLength = 0;
            this.footnotesList.add(noteList);
            for (final KnuthElement element : noteList) {
                if (element.isBox() || element.isGlue()) {
                    noteLength += element.getWidth();
                }
            }
            final int prevLength = this.lengthList == null
                    || this.lengthList.isEmpty() ? 0 : ListUtil
                            .getLast(this.lengthList);
            if (this.lengthList != null) {
                this.lengthList.add(prevLength + noteLength);
            }
            this.totalFootnotesLength += noteLength;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected int restartFrom(final KnuthNode restartingNode,
            final int currentIndex) {
        final int returnValue = super.restartFrom(restartingNode, currentIndex);
        this.newFootnotes = false;
        if (this.footnotesPending) {
            // remove from footnotesList the note lists that will be met
            // after the restarting point
            for (int j = currentIndex; j >= restartingNode.position; j--) {
                final KnuthElement resetElement = getElement(j);
                if (resetElement instanceof KnuthBlockBox
                        && ((KnuthBlockBox) resetElement).hasAnchors()) {
                    resetFootnotes(((KnuthBlockBox) resetElement)
                            .getElementLists());
                }
            }
        }
        return returnValue;
    }

    private void resetFootnotes(final List<List<KnuthElement>> elementLists) {
        for (int i = 0; i < elementLists.size(); i++) {
            ListUtil.removeLast(this.footnotesList);
            ListUtil.removeLast(this.lengthList);

            // update totalFootnotesLength
            if (!this.lengthList.isEmpty()) {
                this.totalFootnotesLength = ListUtil.getLast(this.lengthList);
            } else {
                this.totalFootnotesLength = 0;
            }
        }
        // update footnotesPending;
        if (this.footnotesList.size() == 0) {
            this.footnotesPending = false;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void considerLegalBreak(final KnuthElement element,
            final int elementIdx) {
        if (element.isPenalty()) {
            final int breakClass = ((KnuthPenalty) element).getBreakClass();
            switch (breakClass) {
            case Constants.EN_PAGE:
                if (this.currentKeepContext != breakClass) {
                    this.lastBeforeKeepContextSwitch = getLastTooShort();
                }
                this.currentKeepContext = breakClass;
                break;
            case Constants.EN_COLUMN:
                if (this.currentKeepContext != breakClass) {
                    this.lastBeforeKeepContextSwitch = getLastTooShort();
                }
                this.currentKeepContext = breakClass;
                break;
            case Constants.EN_AUTO:
                this.currentKeepContext = breakClass;
                break;
            default:
                // nop
            }
        }
        super.considerLegalBreak(element, elementIdx);
        this.newFootnotes = false;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean elementCanEndLine(final KnuthElement element,
            final int line, final int difference) {
        if (!element.isPenalty() || this.pageProvider == null) {
            return true;
        } else {
            final KnuthPenalty p = (KnuthPenalty) element;
            if (p.getPenalty() <= 0) {
                return true;
            } else {
                final int context = p.getBreakClass();
                switch (context) {
                case Constants.EN_LINE:
                case Constants.EN_COLUMN:
                    return p.getPenalty() < KnuthElement.INFINITE;
                case Constants.EN_PAGE:
                    return p.getPenalty() < KnuthElement.INFINITE
                            || !this.pageProvider.endPage(line - 1);
                case Constants.EN_AUTO:
                    log.debug("keep is not auto but context is");
                    return true;
                default:
                    if (p.getPenalty() < KnuthElement.INFINITE) {
                        log.debug("Non recognized keep context:" + context);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected int computeDifference(final KnuthNode activeNode,
            final KnuthElement element, final int elementIndex) {
        final KnuthPageNode pageNode = (KnuthPageNode) activeNode;
        int actualWidth = this.totalWidth - pageNode.totalWidth;
        int footnoteSplit;
        boolean canDeferOldFN;
        if (element.isPenalty()) {
            actualWidth += element.getWidth();
        }
        if (this.footnotesPending) {
            // compute the total length of the footnotes not yet inserted
            final int allFootnotes = this.totalFootnotesLength
                    - pageNode.totalFootnotes;
            if (allFootnotes > 0) {
                // this page contains some footnote citations
                // add the footnote separator width
                actualWidth += this.footnoteSeparatorLength.getOpt();
                if (actualWidth + allFootnotes <= getLineWidth(activeNode.line)) {
                    // there is enough space to insert all footnotes:
                    // add the whole allFootnotes length
                    actualWidth += allFootnotes;
                    this.insertedFootnotesLength = pageNode.totalFootnotes
                            + allFootnotes;
                    this.footnoteListIndex = this.footnotesList.size() - 1;
                    this.footnoteElementIndex = getFootnoteList(
                            this.footnoteListIndex).size() - 1;
                } else if (((canDeferOldFN = canDeferOldFootnotes // CSOK:
                        // InnerAssignment
                        (pageNode, elementIndex)) || this.newFootnotes)
                        && (footnoteSplit = getFootnoteSplit // CSOK:
                        // InnerAssignment
                        (pageNode, getLineWidth(activeNode.line) - actualWidth,
                                canDeferOldFN)) > 0) {
                    // it is allowed to break or even defer footnotes if either:
                    // - there are new footnotes in the last piece of content,
                    // and
                    // there is space to add at least a piece of the first one
                    // - or the previous page break deferred some footnote
                    // lines, and
                    // this is the first feasible break; in this case it is
                    // allowed
                    // to break and defer, if necessary, old and new footnotes
                    actualWidth += footnoteSplit;
                    this.insertedFootnotesLength = pageNode.totalFootnotes
                            + footnoteSplit;
                    // footnoteListIndex has been set in getFootnoteSplit()
                    // footnoteElementIndex has been set in getFootnoteSplit()
                } else {
                    // there is no space to add the smallest piece of footnote,
                    // or we are trying to add a piece of content with no
                    // footnotes and
                    // it does not fit in the page, because of previous footnote
                    // bodies
                    // that cannot be broken:
                    // add the whole allFootnotes length, so this breakpoint
                    // will be discarded
                    actualWidth += allFootnotes;
                    this.insertedFootnotesLength = pageNode.totalFootnotes
                            + allFootnotes;
                    this.footnoteListIndex = this.footnotesList.size() - 1;
                    this.footnoteElementIndex = getFootnoteList(
                            this.footnoteListIndex).size() - 1;
                }
            } else {
                // all footnotes have already been placed on previous pages
            }
        } else {
            // there are no footnotes
        }
        final int diff = getLineWidth(activeNode.line) - actualWidth;
        if (this.autoHeight && diff < 0) {
            // getLineWidth() for auto-height parts return 0 so the diff will be
            // negative
            return 0; // ...but we don't want to shrink in this case. Stick to
            // optimum.
        } else {
            return diff;
        }
    }

    /**
     * Checks whether footnotes from preceding pages may be deferred to the page
     * after the given element.
     *
     * @param node
     *            active node for the preceding page break
     * @param contentElementIndex
     *            index of the Knuth element considered for the current page
     *            break
     * @return true if footnotes can be deferred
     */
    private boolean canDeferOldFootnotes(final KnuthPageNode node,
            final int contentElementIndex) {
        return noBreakBetween(node.position, contentElementIndex)
                && deferredFootnotes(node.footnoteListIndex,
                        node.footnoteElementIndex, node.totalFootnotes);
    }

    /**
     * Returns true if there may be no breakpoint between the two given
     * elements.
     *
     * @param prevBreakIndex
     *            index of the element from the currently considered active node
     * @param breakIndex
     *            index of the currently considered breakpoint
     * @return true if no element between the two can be a breakpoint
     */
    private boolean noBreakBetween(final int prevBreakIndex,
            final int breakIndex) {
        // this method stores the parameters and the return value from previous
        // calls
        // in order to avoid scanning the element list unnecessarily:
        // - if there is no break between element #i and element #j
        // there will not be a break between #(i+h) and #j too
        // - if there is a break between element #i and element #j
        // there will be a break between #(i-h) and #(j+k) too
        if (this.storedPrevBreakIndex != -1
                && (prevBreakIndex >= this.storedPrevBreakIndex
                && breakIndex == this.storedBreakIndex
                && this.storedValue || prevBreakIndex <= this.storedPrevBreakIndex
                && breakIndex >= this.storedBreakIndex
                && !this.storedValue)) {
            // use the stored value, do nothing
        } else {
            // compute the new value
            int index;
            // ignore suppressed elements
            for (index = prevBreakIndex + 1; !this.par.getElement(index)
                    .isBox(); index++) {
                // nop
            }
            // find the next break
            for (; index < breakIndex; index++) {
                if (this.par.getElement(index).isGlue()
                        && this.par.getElement(index - 1).isBox()
                        || this.par.getElement(index).isPenalty()
                        && ((KnuthElement) this.par.getElement(index))
                        .getPenalty() < KnuthElement.INFINITE) {
                    // break found
                    break;
                }
            }
            // update stored parameters and value
            this.storedPrevBreakIndex = prevBreakIndex;
            this.storedBreakIndex = breakIndex;
            this.storedValue = index == breakIndex;
        }
        return this.storedValue;
    }

    /**
     * Returns true if their are (pieces of) footnotes to be typeset on the
     * current page.
     *
     * @param listIndex
     *            index of the last inserted footnote for the currently
     *            considered active node
     * @param elementIndex
     *            index of the last element of the last inserted footnote
     * @param length
     *            total length of all footnotes inserted so far
     */
    private boolean deferredFootnotes(final int listIndex,
            final int elementIndex, final int length) {
        return this.newFootnotes
                && this.firstNewFootnoteIndex != 0
                && (listIndex < this.firstNewFootnoteIndex - 1 || elementIndex < getFootnoteList(
                        listIndex).size() - 1)
                || length < this.totalFootnotesLength;
    }

    /**
     * Tries to split the flow of footnotes to put one part on the current page.
     *
     * @param activeNode
     *            currently considered previous page break
     * @param availableLength
     *            available space for footnotes
     * @param canDeferOldFootnotes
     * @return ...
     */
    private int getFootnoteSplit(final KnuthPageNode activeNode,
            final int availableLength, final boolean canDeferOldFootnotes) {
        return getFootnoteSplit(activeNode.footnoteListIndex,
                activeNode.footnoteElementIndex, activeNode.totalFootnotes,
                availableLength, canDeferOldFootnotes);
    }

    /**
     * Tries to split the flow of footnotes to put one part on the current page.
     *
     * @param prevListIndex
     *            index of the last footnote on the previous page
     * @param prevElementIndex
     *            index of the last element of the last footnote
     * @param prevLength
     *            total length of footnotes inserted so far
     * @param availableLength
     *            available space for footnotes on this page
     * @param canDeferOldFootnotes
     * @return ...
     */
    private int getFootnoteSplit(final int prevListIndex,
            final int prevElementIndex, final int prevLength,
            final int availableLength, final boolean canDeferOldFootnotes) {
        if (availableLength <= 0) {
            return 0;
        } else {
            // the split should contain a piece of the last footnote
            // together with all previous, not yet inserted footnotes;
            // but if this is not possible, try adding as much content as
            // possible
            int splitLength = 0;
            ListIterator<KnuthElement> noteListIterator;
            KnuthElement element;
            boolean somethingAdded = false;

            // prevListIndex and prevElementIndex points to the last footnote
            // element
            // already placed in a page: advance to the next element
            int listIndex = prevListIndex;
            int elementIndex = prevElementIndex;
            if (elementIndex == getFootnoteList(listIndex).size() - 1) {
                listIndex++;
                elementIndex = 0;
            } else {
                elementIndex++;
            }

            // try adding whole notes
            if (this.footnotesList.size() - 1 > listIndex) {
                // add the previous footnotes: these cannot be broken or
                // deferred
                if (!canDeferOldFootnotes && this.newFootnotes
                        && this.firstNewFootnoteIndex > 0) {
                    splitLength = this.lengthList
                            .get(this.firstNewFootnoteIndex - 1) - prevLength;
                    listIndex = this.firstNewFootnoteIndex;
                    elementIndex = 0;
                }
                // try adding the new footnotes
                while (this.lengthList.get(listIndex) - prevLength <= availableLength) {
                    splitLength = this.lengthList.get(listIndex) - prevLength;
                    somethingAdded = true;
                    listIndex++;
                    elementIndex = 0;
                }
                // as this method is called only if it is not possible to insert
                // all footnotes, at this point listIndex and elementIndex
                // points to
                // an existing element, the next one we will try to insert
            }

            // try adding a split of the next note
            noteListIterator = getFootnoteList(listIndex).listIterator(
                    elementIndex);

            int prevSplitLength = 0;
            int prevIndex = -1;
            int index = -1;

            while (!(somethingAdded && splitLength > availableLength)) {
                if (!somethingAdded) {
                    somethingAdded = true;
                } else {
                    prevSplitLength = splitLength;
                    prevIndex = index;
                }
                // get a sub-sequence from the note element list
                boolean boxPreceding = false;
                while (noteListIterator.hasNext()) {
                    // as this method is called only if it is not possible to
                    // insert
                    // all footnotes, and we have already tried (and failed) to
                    // insert
                    // this whole footnote, the while loop will never reach the
                    // end
                    // of the note sequence
                    element = noteListIterator.next();
                    if (element.isBox()) {
                        // element is a box
                        splitLength += element.getWidth();
                        boxPreceding = true;
                    } else if (element.isGlue()) {
                        // element is a glue
                        if (boxPreceding) {
                            // end of the sub-sequence
                            index = noteListIterator.previousIndex();
                            break;
                        }
                        boxPreceding = false;
                        splitLength += element.getWidth();
                    } else {
                        // element is a penalty
                        if (element.getPenalty() < KnuthElement.INFINITE) {
                            // end of the sub-sequence
                            index = noteListIterator.previousIndex();
                            break;
                        }
                    }
                }
            }

            // if prevSplitLength is 0, this means that the available length
            // isn't enough
            // to insert even the smallest split of the last footnote, so we
            // cannot end a
            // page here
            // if prevSplitLength is > 0 we can insert some footnote content in
            // this page
            // and insert the remaining in the following one
            // TODO: check this conditional, as the first one is always
            // false...?
            if (!somethingAdded) {
                // there was not enough space to add a piece of the first new
                // footnote
                // this is not a good break
                prevSplitLength = 0;
            } else if (prevSplitLength > 0) {
                // prevIndex is -1 if we have added only some whole footnotes
                this.footnoteListIndex = prevIndex != -1 ? listIndex
                        : listIndex - 1;
                this.footnoteElementIndex = prevIndex != -1 ? prevIndex
                        : getFootnoteList(this.footnoteListIndex).size() - 1;
            }
            return prevSplitLength;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected double computeAdjustmentRatio(final KnuthNode activeNode,
            final int difference) {
        // compute the adjustment ratio
        if (difference > 0) {
            int maxAdjustment = this.totalStretch - activeNode.totalStretch;
            // add the footnote separator stretch if some footnote content will
            // be added
            if (((KnuthPageNode) activeNode).totalFootnotes < this.totalFootnotesLength) {
                maxAdjustment += this.footnoteSeparatorLength.getStretch();
            }
            if (maxAdjustment > 0) {
                return (double) difference / maxAdjustment;
            } else {
                return INFINITE_RATIO;
            }
        } else if (difference < 0) {
            int maxAdjustment = this.totalShrink - activeNode.totalShrink;
            // add the footnote separator shrink if some footnote content will
            // be added
            if (((KnuthPageNode) activeNode).totalFootnotes < this.totalFootnotesLength) {
                maxAdjustment += this.footnoteSeparatorLength.getShrink();
            }
            if (maxAdjustment > 0) {
                return (double) difference / maxAdjustment;
            } else {
                return -INFINITE_RATIO;
            }
        } else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected double computeDemerits(final KnuthNode activeNode,
            final KnuthElement element, final int fitnessClass, final double r) {
        double demerits = 0;
        // compute demerits
        double f = Math.abs(r);
        f = 1 + 100 * f * f * f;
        if (element.isPenalty()) {
            final double penalty = element.getPenalty();
            if (penalty >= 0) {
                f += penalty;
                demerits = f * f;
            } else if (!element.isForcedBreak()) {
                demerits = f * f - penalty * penalty;
            } else {
                demerits = f * f;
            }
        } else {
            demerits = f * f;
        }

        if (element.isPenalty()
                && ((KnuthPenalty) element).isPenaltyFlagged()
                && getElement(activeNode.position).isPenalty()
                && ((KnuthPenalty) getElement(activeNode.position))
                .isPenaltyFlagged()) {
            // add demerit for consecutive breaks at flagged penalties
            demerits += this.repeatedFlaggedDemerit;
        }
        if (Math.abs(fitnessClass - activeNode.fitness) > 1) {
            // add demerit for consecutive breaks
            // with very different fitness classes
            demerits += this.incompatibleFitnessDemerit;
        }

        if (this.footnotesPending) {
            if (this.footnoteListIndex < this.footnotesList.size() - 1) {
                // add demerits for the deferred footnotes
                demerits += (this.footnotesList.size() - 1 - this.footnoteListIndex)
                        * this.deferredFootnoteDemerits;
            }
            if (this.footnoteListIndex < this.footnotesList.size()) {
                if (this.footnoteElementIndex < getFootnoteList(
                        this.footnoteListIndex).size() - 1) {
                    // add demerits for the footnote split between pages
                    demerits += this.splitFootnoteDemerits;
                }
            } else {
                // TODO Why can this happen in the first place? Does anybody
                // know? See #44160
            }
        }
        demerits += activeNode.totalDemerits;
        return demerits;
    }

    /** {@inheritDoc} */
    @Override
    protected void finish() {
        for (int i = this.startLine; i < this.endLine; i++) {
            for (KnuthPageNode node = (KnuthPageNode) getNode(i); node != null; node = (KnuthPageNode) node.next) {
                if (node.totalFootnotes < this.totalFootnotesLength) {
                    // layout remaining footnote bodies
                    createFootnotePages(node);
                }
            }
        }
    }

    private void createFootnotePages(final KnuthPageNode lastNode) {

        this.insertedFootnotesLength = lastNode.totalFootnotes;
        this.footnoteListIndex = lastNode.footnoteListIndex;
        this.footnoteElementIndex = lastNode.footnoteElementIndex;
        int availableBPD = getLineWidth(lastNode.line);
        int split = 0;
        KnuthPageNode prevNode = lastNode;

        // create pages containing the remaining footnote bodies
        while (this.insertedFootnotesLength < this.totalFootnotesLength) {
            final int tmpLength = this.lengthList.get(this.footnoteListIndex);
            // try adding some more content
            if (tmpLength - this.insertedFootnotesLength <= availableBPD) {
                // add a whole footnote
                availableBPD -= tmpLength - this.insertedFootnotesLength;
                this.insertedFootnotesLength = tmpLength;
                this.footnoteElementIndex = getFootnoteList(
                        this.footnoteListIndex).size() - 1;
            } else if ((split = getFootnoteSplit // CSOK: InnerAssignment
                    (this.footnoteListIndex, this.footnoteElementIndex,
                            this.insertedFootnotesLength, availableBPD, true)) > 0) {
                // add a piece of a footnote
                availableBPD -= split;
                this.insertedFootnotesLength += split;
                // footnoteListIndex has already been set in getFootnoteSplit()
                // footnoteElementIndex has already been set in
                // getFootnoteSplit()
            } else {
                // cannot add any content: create a new node and start again
                final KnuthPageNode node = (KnuthPageNode) createNode(
                        lastNode.position, prevNode.line + 1, 1,
                        this.insertedFootnotesLength - prevNode.totalFootnotes,
                        0, 0, 0, 0, 0, 0, 0, prevNode);
                addNode(node.line, node);
                removeNode(prevNode.line, prevNode);

                prevNode = node;
                availableBPD = getLineWidth(node.line);
            }
        }
        // create the last node
        final KnuthPageNode node = (KnuthPageNode) createNode(
                lastNode.position, prevNode.line + 1, 1,
                this.totalFootnotesLength - prevNode.totalFootnotes, 0, 0, 0,
                0, 0, 0, 0, prevNode);
        addNode(node.line, node);
        removeNode(prevNode.line, prevNode);
    }

    /**
     * @return a list of {@link PageBreakPosition} elements corresponding to the
     *         computed page- and column-breaks
     */
    public LinkedList<PageBreakPosition> getPageBreaks() {
        return this.pageBreaks;
    }

    /**
     * Insert the given {@link PageBreakPosition} as the first element in the
     * list of page-breaks
     *
     * @param pageBreak
     *            the position to insert
     */
    public void insertPageBreakAsFirst(final PageBreakPosition pageBreak) {
        if (this.pageBreaks == null) {
            this.pageBreaks = new LinkedList<PageBreakPosition>();
        }
        this.pageBreaks.addFirst(pageBreak);
    }

    /**
     * Removes all page breaks from the result list. This is used by
     * block-containers and static-content when it is only desired to know where
     * there is an overflow but later the whole content should be painted as one
     * part.
     */
    public void removeAllPageBreaks() {
        if (this.pageBreaks == null || this.pageBreaks.isEmpty()) {
            return;
        }
        this.pageBreaks.subList(0, this.pageBreaks.size() - 1).clear();
    }

    /** {@inheritDoc} */
    @Override
    public void updateData1(final int total, final double demerits) {
    }

    /** {@inheritDoc} */
    @Override
    public void updateData2(final KnuthNode bestActiveNode,
            final KnuthSequence sequence, final int total) {
        // int difference = (bestActiveNode.line < total)
        // ? bestActiveNode.difference : bestActiveNode.difference +
        // fillerMinWidth;
        int difference = bestActiveNode.difference;
        if (difference + bestActiveNode.availableShrink < 0) {
            if (!this.autoHeight) {
                if (this.layoutListener != null) {
                    this.layoutListener.notifyOverflow(bestActiveNode.line - 1,
                            -difference, getFObj());
                }
            }
        }
        final boolean isNonLastPage = bestActiveNode.line < total;
        final int blockAlignment = isNonLastPage ? this.alignment
                : this.alignmentLast;
        // it is always allowed to adjust space, so the ratio must be set
        // regardless of
        // the value of the property display-align; the ratio must be <= 1
        double ratio = bestActiveNode.adjustRatio;
        if (ratio < 0) {
            // page break with a negative difference:
            // spaces always have enough shrink
            difference = 0;
        } else if (ratio <= 1 && isNonLastPage) {
            // not-last page break with a positive difference smaller than the
            // available stretch:
            // spaces can stretch to fill the whole difference
            difference = 0;
        } else if (ratio > 1) {
            // not-last page with a positive difference greater than the
            // available stretch
            // spaces can stretch to fill the difference only partially
            ratio = 1;
            difference -= bestActiveNode.availableStretch;
        } else {
            // last page with a positive difference:
            // spaces do not need to stretch
            if (blockAlignment != Constants.EN_JUSTIFY) {
                ratio = 0;
            } else {
                // Stretch as much as possible on last page
                difference = 0;
            }
        }
        // compute the indexes of the first footnote list and the first element
        // in that list
        int firstListIndex = ((KnuthPageNode) bestActiveNode.previous).footnoteListIndex;
        int firstElementIndex = ((KnuthPageNode) bestActiveNode.previous).footnoteElementIndex;
        if (this.footnotesList != null
                && firstElementIndex == getFootnoteList(firstListIndex).size() - 1) {
            // advance to the next list
            firstListIndex++;
            firstElementIndex = 0;
        } else {
            firstElementIndex++;
        }

        // add nodes at the beginning of the list, as they are found
        // backwards, from the last one to the first one
        if (log.isDebugEnabled()) {
            log.debug("BBA> difference=" + difference + " ratio=" + ratio
                    + " position=" + bestActiveNode.position);
        }
        insertPageBreakAsFirst(new PageBreakPosition(this.topLevelLM,
                bestActiveNode.position, firstListIndex, firstElementIndex,
                ((KnuthPageNode) bestActiveNode).footnoteListIndex,
                ((KnuthPageNode) bestActiveNode).footnoteElementIndex, ratio,
                difference));
    }

    /** {@inheritDoc} */
    @Override
    protected int filterActiveNodes() {
        // leave only the active node with fewest total demerits
        KnuthNode bestActiveNode = null;
        for (int i = this.startLine; i < this.endLine; i++) {
            for (KnuthNode node = getNode(i); node != null; node = node.next) {
                if (this.favorSinglePart
                        && node.line > 1
                        && bestActiveNode != null
                        && Math.abs(bestActiveNode.difference) < bestActiveNode.availableShrink) {
                    // favor current best node, so just skip the current node
                    // because it would
                    // result in more than one part
                } else {
                    bestActiveNode = compareNodes(bestActiveNode, node);
                }
                if (node != bestActiveNode) {
                    removeNode(i, node);
                }
            }
        }
        assert bestActiveNode != null;
        return bestActiveNode.line;
    }

    /**
     * Obtain the element-list corresponding to the footnote at the given index.
     *
     * @param index
     *            the index in the list of footnotes
     * @return the element-list
     */
    protected final List<KnuthElement> getFootnoteList(final int index) {
        return this.footnotesList.get(index);
    }

    /** @return the associated top-level formatting object. */
    public FObj getFObj() {
        return this.topLevelLM.getFObj();
    }

    /** {@inheritDoc} */
    @Override
    protected int getLineWidth(final int line) {
        int bpd;
        if (this.pageProvider != null) {
            bpd = this.pageProvider.getAvailableBPD(line);
        } else {
            bpd = super.getLineWidth(line);
        }
        if (log.isTraceEnabled()) {
            log.trace("getLineWidth(" + line + ") -> " + bpd);
        }
        return bpd;
    }

    /**
     * Interface to notify about layout events during page breaking.
     */
    public interface PageBreakingLayoutListener {

        /**
         * Issued when an overflow is detected
         *
         * @param part
         *            the number of the part (page) this happens on
         * @param amount
         *            the amount by which the area overflows (in mpt)
         * @param obj
         *            the root FO object where this happens
         */
        void notifyOverflow(final int part, final int amount, final FObj obj);

    }

    /** {@inheritDoc} */
    @Override
    protected int getIPDdifference() {
        return this.ipdDifference;
    }

    /** {@inheritDoc} */
    @Override
    protected int handleIpdChange() {
        log.trace("Best node for ipd change:" + this.bestNodeForIPDChange);
        // TODO finish()
        /*
         * The third parameter is used to determine if this is the last page, so
         * if the content must be vertically justified or not. If we are here
         * this means that there is further content and the next page has a
         * different ipd. So tweak the parameter to fall into the non-last-page
         * case.
         */
        calculateBreakPoints(this.bestNodeForIPDChange, this.par,
                this.bestNodeForIPDChange.line + 1);
        this.activeLines = null;
        return this.bestNodeForIPDChange.line;
    }

    /**
     * Add a node at the end of the given line's existing active nodes. If this
     * is the first node in the line, adjust endLine accordingly.
     *
     * @param line
     *            number of the line ending at the node's corresponding
     *            breakpoint
     * @param node
     *            the active node to add
     */
    @Override
    protected void addNode(final int line, final KnuthNode node) {
        if (node.position < this.par.size() - 1 && line > 0
                && (this.ipdDifference = compareIPDs(line - 1)) != 0) { // CSOK:
            // InnerAssignment
            log.trace("IPD changes at page " + line);
            if (this.bestNodeForIPDChange == null
                    || node.totalDemerits < this.bestNodeForIPDChange.totalDemerits) {
                this.bestNodeForIPDChange = node;
            }
        } else {
            if (node.position == this.par.size() - 1) {
                /*
                 * The whole sequence could actually fit on the last page before
                 * the IPD change. No need to do any special handling.
                 */
                this.ipdDifference = 0;
            }
            super.addNode(line, node);
        }
    }

    KnuthNode getBestNodeBeforeIPDChange() {
        return this.bestNodeForIPDChange;
    }

    private int compareIPDs(final int line) {
        if (this.pageProvider == null) {
            return 0;
        }
        return this.pageProvider.compareIPDs(line);
    }
}
