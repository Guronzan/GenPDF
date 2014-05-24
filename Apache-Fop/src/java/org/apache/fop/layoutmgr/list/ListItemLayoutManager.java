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

/* $Id: ListItemLayoutManager.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.layoutmgr.list;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.fo.flow.ListItem;
import org.apache.fop.fo.flow.ListItemBody;
import org.apache.fop.fo.flow.ListItemLabel;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.layoutmgr.BlockStackingLayoutManager;
import org.apache.fop.layoutmgr.BreakElement;
import org.apache.fop.layoutmgr.ConditionalElementListener;
import org.apache.fop.layoutmgr.ElementListObserver;
import org.apache.fop.layoutmgr.ElementListUtils;
import org.apache.fop.layoutmgr.FootnoteBodyLayoutManager;
import org.apache.fop.layoutmgr.Keep;
import org.apache.fop.layoutmgr.KnuthBlockBox;
import org.apache.fop.layoutmgr.KnuthBox;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthPenalty;
import org.apache.fop.layoutmgr.KnuthPossPosIter;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.ListElement;
import org.apache.fop.layoutmgr.NonLeafPosition;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.RelSide;
import org.apache.fop.layoutmgr.SpaceResolver;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.traits.SpaceVal;

/**
 * LayoutManager for a list-item FO. The list item contains a list item label
 * and a list item body.
 */
@Slf4j
public class ListItemLayoutManager extends BlockStackingLayoutManager implements
ConditionalElementListener {

    private ListItemContentLayoutManager label;
    private ListItemContentLayoutManager body;

    private Block curBlockArea = null;

    private List<ListElement> labelList = null;
    private List<ListElement> bodyList = null;

    private boolean discardBorderBefore;
    private boolean discardBorderAfter;
    private boolean discardPaddingBefore;
    private boolean discardPaddingAfter;
    private MinOptMax effSpaceBefore;
    private MinOptMax effSpaceAfter;

    private Keep keepWithNextPendingOnLabel;
    private Keep keepWithNextPendingOnBody;

    private class ListItemPosition extends Position {
        private final int labelFirstIndex;
        private final int labelLastIndex;
        private final int bodyFirstIndex;
        private final int bodyLastIndex;

        public ListItemPosition(final LayoutManager lm, final int labelFirst,
                final int labelLast, final int bodyFirst, final int bodyLast) {
            super(lm);
            this.labelFirstIndex = labelFirst;
            this.labelLastIndex = labelLast;
            this.bodyFirstIndex = bodyFirst;
            this.bodyLastIndex = bodyLast;
        }

        public int getLabelFirstIndex() {
            return this.labelFirstIndex;
        }

        public int getLabelLastIndex() {
            return this.labelLastIndex;
        }

        public int getBodyFirstIndex() {
            return this.bodyFirstIndex;
        }

        public int getBodyLastIndex() {
            return this.bodyLastIndex;
        }

        /** {@inheritDoc} */
        @Override
        public boolean generatesAreas() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("ListItemPosition:");
            sb.append(getIndex()).append("(");
            sb.append("label:").append(this.labelFirstIndex).append("-")
            .append(this.labelLastIndex);
            sb.append(" body:").append(this.bodyFirstIndex).append("-")
            .append(this.bodyLastIndex);
            sb.append(")");
            return sb.toString();
        }
    }

    /**
     * Create a new list item layout manager.
     *
     * @param node
     *            list-item to create the layout manager for
     */
    public ListItemLayoutManager(final ListItem node) {
        super(node);
        setLabel(node.getLabel());
        setBody(node.getBody());
    }

    /**
     * Convenience method.
     *
     * @return the ListBlock node
     */
    protected ListItem getListItemFO() {
        return (ListItem) this.fobj;
    }

    /**
     * Create a LM for the fo:list-item-label object
     *
     * @param node
     *            the fo:list-item-label FO
     */
    public void setLabel(final ListItemLabel node) {
        this.label = new ListItemContentLayoutManager(node);
        this.label.setParent(this);
    }

    /**
     * Create a LM for the fo:list-item-body object
     *
     * @param node
     *            the fo:list-item-body FO
     */
    public void setBody(final ListItemBody node) {
        this.body = new ListItemContentLayoutManager(node);
        this.body.setParent(this);
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        this.foSpaceBefore = new SpaceVal(getListItemFO()
                .getCommonMarginBlock().spaceBefore, this).getSpace();
        this.foSpaceAfter = new SpaceVal(
                getListItemFO().getCommonMarginBlock().spaceAfter, this)
        .getSpace();
        this.startIndent = getListItemFO().getCommonMarginBlock().startIndent
                .getValue(this);
        this.endIndent = getListItemFO().getCommonMarginBlock().endIndent
                .getValue(this);
    }

    private void resetSpaces() {
        this.discardBorderBefore = false;
        this.discardBorderAfter = false;
        this.discardPaddingBefore = false;
        this.discardPaddingAfter = false;
        this.effSpaceBefore = null;
        this.effSpaceAfter = null;
    }

    /** {@inheritDoc} */
    @Override
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        this.referenceIPD = context.getRefIPD();
        LayoutContext childLC;

        final List<ListElement> returnList = new LinkedList<ListElement>();

        if (!breakBeforeServed(context, returnList)) {
            return returnList;
        }

        addFirstVisibleMarks(returnList, context, alignment);

        // label
        childLC = makeChildLayoutContext(context);
        this.label.initialize();
        this.labelList = this.label.getNextKnuthElements(childLC, alignment);

        // Space resolution as if the contents were placed in a new reference
        // area
        // (see 6.8.3, XSL 1.0, section on Constraints, last paragraph)
        SpaceResolver.resolveElementList(this.labelList);
        ElementListObserver.observe(this.labelList, "list-item-label",
                this.label.getPartFO().getId());

        context.updateKeepWithPreviousPending(childLC
                .getKeepWithPreviousPending());
        this.keepWithNextPendingOnLabel = childLC.getKeepWithNextPending();

        // body
        childLC = makeChildLayoutContext(context);
        this.body.initialize();
        this.bodyList = this.body.getNextKnuthElements(childLC, alignment);

        // Space resolution as if the contents were placed in a new reference
        // area
        // (see 6.8.3, XSL 1.0, section on Constraints, last paragraph)
        SpaceResolver.resolveElementList(this.bodyList);
        ElementListObserver.observe(this.bodyList, "list-item-body", this.body
                .getPartFO().getId());

        context.updateKeepWithPreviousPending(childLC
                .getKeepWithPreviousPending());
        this.keepWithNextPendingOnBody = childLC.getKeepWithNextPending();

        // create a combined list
        final List returnedList = getCombinedKnuthElementsForListItem(
                this.labelList, this.bodyList, context);

        // "wrap" the Position inside each element
        wrapPositionElements(returnedList, returnList, true);

        addLastVisibleMarks(returnList, context, alignment);

        addKnuthElementsForBreakAfter(returnList, context);

        context.updateKeepWithNextPending(this.keepWithNextPendingOnLabel);
        context.updateKeepWithNextPending(this.keepWithNextPendingOnBody);
        context.updateKeepWithNextPending(getKeepWithNext());
        context.updateKeepWithPreviousPending(getKeepWithPrevious());

        setFinished(true);
        resetSpaces();
        return returnList;
    }

    /**
     * Overridden to unconditionally add elements for space-before.
     * {@inheritDoc}
     */
    @Override
    protected void addFirstVisibleMarks(final List<ListElement> elements,
            final LayoutContext context, final int alignment) {
        addKnuthElementsForSpaceBefore(elements, alignment);
        addKnuthElementsForBorderPaddingBefore(elements,
                !this.firstVisibleMarkServed);
        this.firstVisibleMarkServed = true;
        // Spaces, border and padding to be repeated at each break
        addPendingMarks(context);
    }

    private List getCombinedKnuthElementsForListItem(
            final List<ListElement> labelElements,
            final List<ListElement> bodyElements, final LayoutContext context) {
        // Copy elements to array lists to improve element access performance
        final List[] elementLists = {
                new ArrayList<ListElement>(labelElements),
                new ArrayList<ListElement>(bodyElements) };
        final int[] fullHeights = {
                ElementListUtils.calcContentLength(elementLists[0]),
                ElementListUtils.calcContentLength(elementLists[1]) };
        final int[] partialHeights = { 0, 0 };
        final int[] start = { -1, -1 };
        final int[] end = { -1, -1 };

        final int totalHeight = Math.max(fullHeights[0], fullHeights[1]);
        int step;
        int addedBoxHeight = 0;
        Keep keepWithNextActive = Keep.KEEP_AUTO;

        final LinkedList<ListElement> returnList = new LinkedList<ListElement>();
        while ((step = getNextStep(elementLists, start, end, partialHeights)) > 0) {

            if (end[0] + 1 == elementLists[0].size()) {
                keepWithNextActive = keepWithNextActive
                        .compare(this.keepWithNextPendingOnLabel);
            }
            if (end[1] + 1 == elementLists[1].size()) {
                keepWithNextActive = keepWithNextActive
                        .compare(this.keepWithNextPendingOnBody);
            }

            // compute penalty height and box height
            int penaltyHeight = step
                    + getMaxRemainingHeight(fullHeights, partialHeights)
                    - totalHeight;

            // Additional penalty height from penalties in the source lists
            int additionalPenaltyHeight = 0;
            int stepPenalty = 0;
            KnuthElement endEl = (KnuthElement) elementLists[0].get(end[0]);
            if (endEl instanceof KnuthPenalty) {
                additionalPenaltyHeight = endEl.getWidth();
                stepPenalty = Math.max(stepPenalty, endEl.getPenalty());
            }
            endEl = (KnuthElement) elementLists[1].get(end[1]);
            if (endEl instanceof KnuthPenalty) {
                additionalPenaltyHeight = Math.max(additionalPenaltyHeight,
                        endEl.getWidth());
                stepPenalty = Math.max(stepPenalty, endEl.getPenalty());
            }

            final int boxHeight = step - addedBoxHeight - penaltyHeight;
            penaltyHeight += additionalPenaltyHeight; // Add AFTER calculating
            // boxHeight!

            // collect footnote information
            // TODO this should really not be done like this. ListItemLM should
            // remain as
            // footnote-agnostic as possible
            LinkedList<FootnoteBodyLayoutManager> footnoteList = null;
            ListElement el;
            for (int i = 0; i < elementLists.length; i++) {
                for (int j = start[i]; j <= end[i]; j++) {
                    el = (ListElement) elementLists[i].get(j);
                    if (el instanceof KnuthBlockBox
                            && ((KnuthBlockBox) el).hasAnchors()) {
                        if (footnoteList == null) {
                            footnoteList = new LinkedList<FootnoteBodyLayoutManager>();
                        }
                        footnoteList.addAll(((KnuthBlockBox) el)
                                .getFootnoteBodyLMs());
                    }
                }
            }

            // add the new elements
            addedBoxHeight += boxHeight;
            final ListItemPosition stepPosition = new ListItemPosition(this,
                    start[0], end[0], start[1], end[1]);
            if (footnoteList == null) {
                returnList.add(new KnuthBox(boxHeight, stepPosition, false));
            } else {
                returnList.add(new KnuthBlockBox(boxHeight, footnoteList,
                        stepPosition, false));
            }

            if (addedBoxHeight < totalHeight) {
                final Keep keep = keepWithNextActive.compare(getKeepTogether());
                int p = stepPenalty;
                if (p > -KnuthElement.INFINITE) {
                    p = Math.max(p, keep.getPenalty());
                }
                returnList.add(new BreakElement(stepPosition, penaltyHeight, p,
                        keep.getContext(), context));
            }
        }

        return returnList;
    }

    private int getNextStep(final List[] elementLists, final int[] start,
            final int[] end, final int[] partialHeights) {
        // backup of partial heights
        final int[] backupHeights = { partialHeights[0], partialHeights[1] };

        // set starting points
        start[0] = end[0] + 1;
        start[1] = end[1] + 1;

        // get next possible sequence for label and body
        int seqCount = 0;
        for (int i = 0; i < start.length; i++) {
            while (end[i] + 1 < elementLists[i].size()) {
                end[i]++;
                final KnuthElement el = (KnuthElement) elementLists[i]
                        .get(end[i]);
                if (el.isPenalty()) {
                    if (el.getPenalty() < KnuthElement.INFINITE) {
                        // First legal break point
                        break;
                    }
                } else if (el.isGlue()) {
                    if (end[i] > 0) {
                        final KnuthElement prev = (KnuthElement) elementLists[i]
                                .get(end[i] - 1);
                        if (prev.isBox()) {
                            // Second legal break point
                            break;
                        }
                    }
                    partialHeights[i] += el.getWidth();
                } else {
                    partialHeights[i] += el.getWidth();
                }
            }
            if (end[i] < start[i]) {
                partialHeights[i] = backupHeights[i];
            } else {
                seqCount++;
            }
        }
        if (seqCount == 0) {
            return 0;
        }

        // determine next step
        int step;
        if (backupHeights[0] == 0 && backupHeights[1] == 0) {
            // this is the first step: choose the maximum increase, so that
            // the smallest area in the first page will contain at least
            // a label area and a body area
            step = Math.max(end[0] >= start[0] ? partialHeights[0]
                    : Integer.MIN_VALUE, end[1] >= start[1] ? partialHeights[1]
                            : Integer.MIN_VALUE);
        } else {
            // this is not the first step: choose the minimum increase
            step = Math.min(end[0] >= start[0] ? partialHeights[0]
                    : Integer.MAX_VALUE, end[1] >= start[1] ? partialHeights[1]
                            : Integer.MAX_VALUE);
        }

        // reset bigger-than-step sequences
        for (int i = 0; i < partialHeights.length; i++) {
            if (partialHeights[i] > step) {
                partialHeights[i] = backupHeights[i];
                end[i] = start[i] - 1;
            }
        }

        return step;
    }

    private int getMaxRemainingHeight(final int[] fullHeights,
            final int[] partialHeights) {
        return Math.max(fullHeights[0] - partialHeights[0], fullHeights[1]
                - partialHeights[1]);
    }

    /** {@inheritDoc} */
    @Override
    public List getChangedKnuthElements(final List oldList, final int alignment) {
        // label
        this.labelList = this.label.getChangedKnuthElements(this.labelList,
                alignment);

        // body
        // "unwrap" the Positions stored in the elements
        final ListIterator oldListIterator = oldList.listIterator();
        KnuthElement oldElement;
        while (oldListIterator.hasNext()) {
            oldElement = (KnuthElement) oldListIterator.next();
            final Position innerPosition = oldElement.getPosition()
                    .getPosition();
            if (innerPosition != null) {
                // oldElement was created by a descendant of this BlockLM
                oldElement.setPosition(innerPosition);
            } else {
                // thisElement was created by this BlockLM
                // modify its position in order to recognize it was not created
                // by a child
                oldElement.setPosition(new Position(this));
            }
        }

        List returnedList = this.body.getChangedKnuthElements(oldList,
                alignment);
        // "wrap" the Position inside each element
        final List tempList = returnedList;
        KnuthElement tempElement;
        returnedList = new LinkedList();
        final ListIterator listIter = tempList.listIterator();
        while (listIter.hasNext()) {
            tempElement = (KnuthElement) listIter.next();
            tempElement.setPosition(new NonLeafPosition(this, tempElement
                    .getPosition()));
            returnedList.add(tempElement);
        }

        return returnedList;
    }

    /**
     * Add the areas for the break points.
     *
     * @param parentIter
     *            the position iterator
     * @param layoutContext
     *            the layout context for adding areas
     */
    @Override
    public void addAreas(final PositionIterator parentIter,
            final LayoutContext layoutContext) {
        getParentArea(null);

        addId();

        final LayoutContext lc = new LayoutContext(0);
        Position firstPos = null;
        Position lastPos = null;

        // "unwrap" the NonLeafPositions stored in parentIter
        final LinkedList<Position> positionList = new LinkedList<Position>();
        Position pos;
        while (parentIter.hasNext()) {
            pos = parentIter.next();
            if (pos.getIndex() >= 0) {
                if (firstPos == null) {
                    firstPos = pos;
                }
                lastPos = pos;
            }
            if (pos instanceof NonLeafPosition && pos.getPosition() != null) {
                // pos contains a ListItemPosition created by this ListBlockLM
                positionList.add(pos.getPosition());
            }
        }

        addMarkersToPage(true, isFirst(firstPos), isLast(lastPos));

        // use the first and the last ListItemPosition to determine the
        // corresponding indexes in the original labelList and bodyList
        final int labelFirstIndex = ((ListItemPosition) positionList.getFirst())
                .getLabelFirstIndex();
        final int labelLastIndex = ((ListItemPosition) positionList.getLast())
                .getLabelLastIndex();
        final int bodyFirstIndex = ((ListItemPosition) positionList.getFirst())
                .getBodyFirstIndex();
        final int bodyLastIndex = ((ListItemPosition) positionList.getLast())
                .getBodyLastIndex();

        // Determine previous break if any (in item label list)
        int previousBreak = ElementListUtils.determinePreviousBreak(
                this.labelList, labelFirstIndex);
        SpaceResolver.performConditionalsNotification(this.labelList,
                labelFirstIndex, labelLastIndex, previousBreak);

        // Determine previous break if any (in item body list)
        previousBreak = ElementListUtils.determinePreviousBreak(this.bodyList,
                bodyFirstIndex);
        SpaceResolver.performConditionalsNotification(this.bodyList,
                bodyFirstIndex, bodyLastIndex, previousBreak);

        // add label areas
        if (labelFirstIndex <= labelLastIndex) {
            final KnuthPossPosIter labelIter = new KnuthPossPosIter(
                    this.labelList, labelFirstIndex, labelLastIndex + 1);
            lc.setFlags(LayoutContext.FIRST_AREA, layoutContext.isFirstArea());
            lc.setFlags(LayoutContext.LAST_AREA, layoutContext.isLastArea());
            // set the space adjustment ratio
            lc.setSpaceAdjust(layoutContext.getSpaceAdjust());
            // TO DO: use the right stack limit for the label
            lc.setStackLimitBP(layoutContext.getStackLimitBP());
            this.label.addAreas(labelIter, lc);
        }

        // add body areas
        if (bodyFirstIndex <= bodyLastIndex) {
            final KnuthPossPosIter bodyIter = new KnuthPossPosIter(
                    this.bodyList, bodyFirstIndex, bodyLastIndex + 1);
            lc.setFlags(LayoutContext.FIRST_AREA, layoutContext.isFirstArea());
            lc.setFlags(LayoutContext.LAST_AREA, layoutContext.isLastArea());
            // set the space adjustment ratio
            lc.setSpaceAdjust(layoutContext.getSpaceAdjust());
            // TO DO: use the right stack limit for the body
            lc.setStackLimitBP(layoutContext.getStackLimitBP());
            this.body.addAreas(bodyIter, lc);
        }

        // after adding body areas, set the maximum area bpd
        final int childCount = this.curBlockArea.getChildAreas().size();
        assert childCount >= 1 && childCount <= 2;
        int itemBPD = ((Block) this.curBlockArea.getChildAreas().get(0))
                .getAllocBPD();
        if (childCount == 2) {
            itemBPD = Math.max(itemBPD, ((Block) this.curBlockArea
                    .getChildAreas().get(1)).getAllocBPD());
        }
        this.curBlockArea.setBPD(itemBPD);

        addMarkersToPage(false, isFirst(firstPos), isLast(lastPos));

        // We are done with this area add the background
        TraitSetter.addBackground(this.curBlockArea, getListItemFO()
                .getCommonBorderPaddingBackground(), this);
        TraitSetter.addSpaceBeforeAfter(this.curBlockArea,
                layoutContext.getSpaceAdjust(), this.effSpaceBefore,
                this.effSpaceAfter);

        flush();

        this.curBlockArea = null;
        resetSpaces();

        checkEndOfLayout(lastPos);
    }

    /**
     * Return an Area which can contain the passed childArea. The childArea may
     * not yet have any content, but it has essential traits set. In general, if
     * the LayoutManager already has an Area it simply returns it. Otherwise, it
     * makes a new Area of the appropriate class. It gets a parent area for its
     * area by calling its parent LM. Finally, based on the dimensions of the
     * parent area, it initializes its own area. This includes setting the
     * content IPD and the maximum BPD.
     *
     * @param childArea
     *            the child area
     * @return the parent are for the child
     */
    @Override
    public Area getParentArea(final Area childArea) {
        if (this.curBlockArea == null) {
            this.curBlockArea = new Block();

            // Set up dimensions
            /* Area parentArea = */this.parentLayoutManager
            .getParentArea(this.curBlockArea);

            // set traits
            final ListItem fo = getListItemFO();
            TraitSetter.setProducerID(this.curBlockArea, fo.getId());
            TraitSetter.addBorders(this.curBlockArea,
                    fo.getCommonBorderPaddingBackground(),
                    this.discardBorderBefore, this.discardBorderAfter, false,
                    false, this);
            TraitSetter.addPadding(this.curBlockArea,
                    fo.getCommonBorderPaddingBackground(),
                    this.discardPaddingBefore, this.discardPaddingAfter, false,
                    false, this);
            TraitSetter.addMargins(this.curBlockArea,
                    fo.getCommonBorderPaddingBackground(),
                    fo.getCommonMarginBlock(), this);
            TraitSetter.addBreaks(this.curBlockArea, fo.getBreakBefore(),
                    fo.getBreakAfter());

            final int contentIPD = this.referenceIPD - getIPIndents();
            this.curBlockArea.setIPD(contentIPD);

            setCurrentArea(this.curBlockArea);
        }
        return this.curBlockArea;
    }

    /**
     * Add the child. Rows return the areas returned by the child elements. This
     * simply adds the area to the parent layout manager.
     *
     * @param childArea
     *            the child area
     */
    @Override
    public void addChildArea(final Area childArea) {
        if (this.curBlockArea != null) {
            this.curBlockArea.addBlock((Block) childArea);
        }
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepTogetherProperty() {
        return getListItemFO().getKeepTogether();
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepWithPreviousProperty() {
        return getListItemFO().getKeepWithPrevious();
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepWithNextProperty() {
        return getListItemFO().getKeepWithNext();
    }

    /** {@inheritDoc} */
    @Override
    public void notifySpace(final RelSide side, final MinOptMax effectiveLength) {
        if (RelSide.BEFORE == side) {
            if (log.isDebugEnabled()) {
                log.debug(this + ": Space " + side + ", " + this.effSpaceBefore
                        + "-> " + effectiveLength);
            }
            this.effSpaceBefore = effectiveLength;
        } else {
            if (log.isDebugEnabled()) {
                log.debug(this + ": Space " + side + ", " + this.effSpaceAfter
                        + "-> " + effectiveLength);
            }
            this.effSpaceAfter = effectiveLength;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyBorder(final RelSide side, final MinOptMax effectiveLength) {
        if (effectiveLength == null) {
            if (RelSide.BEFORE == side) {
                this.discardBorderBefore = true;
            } else {
                this.discardBorderAfter = true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(this + ": Border " + side + " -> " + effectiveLength);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPadding(final RelSide side,
            final MinOptMax effectiveLength) {
        if (effectiveLength == null) {
            if (RelSide.BEFORE == side) {
                this.discardPaddingBefore = true;
            } else {
                this.discardPaddingAfter = true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(this + ": Padding " + side + " -> " + effectiveLength);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        super.reset();
        this.label.reset();
        this.body.reset();
    }

}
