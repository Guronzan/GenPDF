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

/* $Id: BlockStackingLayoutManager.java 1334058 2012-05-04 16:52:35Z gadams $ */

package org.apache.fop.layoutmgr;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockParent;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.properties.BreakPropertySet;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.SpaceProperty;
import org.apache.fop.layoutmgr.inline.InlineLayoutManager;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.util.ListUtil;

/**
 * Base LayoutManager class for all areas which stack their child areas in the
 * block-progression direction, such as Flow, Block, ListBlock.
 */
@Slf4j
public abstract class BlockStackingLayoutManager extends AbstractLayoutManager
implements BlockLevelLayoutManager {

    /** parent area */
    protected BlockParent parentArea;

    /** Value of the block-progression-unit (non-standard property) */
    protected int bpUnit;
    /** space-before value adjusted for block-progression-unit handling */
    protected int adjustedSpaceBefore;
    /** space-after value adjusted for block-progression-unit handling */
    protected int adjustedSpaceAfter;
    /** Only used to store the original list when createUnitElements is called */
    protected List<KnuthElement> storedList;
    /** Indicates whether break before has been served or not */
    protected boolean breakBeforeServed;
    /**
     * Indicates whether the first visible mark has been returned by this LM,
     * yet
     */
    protected boolean firstVisibleMarkServed;
    /** Reference IPD available */
    protected int referenceIPD;
    /** the effective start-indent value */
    protected int startIndent;
    /** the effective end-indent value */
    protected int endIndent;
    /**
     * Holds the (one-time use) fo:block space-before and -after properties.
     * Large fo:blocks are split into multiple Area. Blocks to accomodate the
     * subsequent regions (pages) they are placed on. space-before is applied at
     * the beginning of the first Block and space-after at the end of the last
     * Block used in rendering the fo:block.
     */
    protected MinOptMax foSpaceBefore;
    /** see foSpaceBefore */
    protected MinOptMax foSpaceAfter;

    private Position auxiliaryPosition;

    private int contentAreaIPD;

    /**
     * @param node
     *            the fo this LM deals with
     */
    public BlockStackingLayoutManager(final FObj node) {
        super(node);
        setGeneratesBlockArea(true);
    }

    /**
     * @return current area being filled
     */
    protected BlockParent getCurrentArea() {
        return this.parentArea;
    }

    /**
     * Set the current area being filled.
     *
     * @param parentArea
     *            the current area to be filled
     */
    protected void setCurrentArea(final BlockParent parentArea) {
        this.parentArea = parentArea;
    }

    /**
     * Add a block spacer for space before and space after a block. This adds an
     * empty Block area that acts as a block space.
     *
     * @param adjust
     *            the adjustment value
     * @param minoptmax
     *            the min/opt/max value of the spacing
     */
    public void addBlockSpacing(final double adjust, final MinOptMax minoptmax) {
        final int sp = TraitSetter.getEffectiveSpace(adjust, minoptmax);
        if (sp != 0) {
            final Block spacer = new Block();
            spacer.setBPD(sp);
            this.parentLayoutManager.addChildArea(spacer);
        }
    }

    /**
     * Add the childArea to the passed area. Called by child LayoutManager when
     * it has filled one of its areas. The LM should already have an Area in
     * which to put the child. See if the area will fit in the current area. If
     * so, add it. Otherwise initiate breaking.
     *
     * @param childArea
     *            the area to add: will be some block-stacked Area.
     * @param parentArea
     *            the area in which to add the childArea
     */
    protected void addChildToArea(final Area childArea,
            final BlockParent parentArea) {
        // This should be a block-level Area (Block in the generic sense)
        if (!(childArea instanceof Block)) {
            // log.error("Child not a Block in BlockStackingLM!");
        }

        parentArea.addBlock((Block) childArea);
        flush(); // hand off current area to parent
    }

    /**
     * Add the childArea to the current area. Called by child LayoutManager when
     * it has filled one of its areas. The LM should already have an Area in
     * which to put the child. See if the area will fit in the current area. If
     * so, add it. Otherwise initiate breaking.
     *
     * @param childArea
     *            the area to add: will be some block-stacked Area.
     */
    @Override
    public void addChildArea(final Area childArea) {
        addChildToArea(childArea, getCurrentArea());
    }

    /**
     * Force current area to be added to parent area.
     */
    protected void flush() {
        if (getCurrentArea() != null) {
            this.parentLayoutManager.addChildArea(getCurrentArea());
        }
    }

    /** @return a cached auxiliary Position instance used for things like spaces. */
    protected Position getAuxiliaryPosition() {
        if (this.auxiliaryPosition == null) {
            this.auxiliaryPosition = new NonLeafPosition(this, null);
        }
        return this.auxiliaryPosition;
    }

    /**
     * @param len
     *            length in millipoints to span with bp units
     * @return the minimum integer n such that n * bpUnit >= len
     */
    protected int neededUnits(final int len) {
        return (int) Math.ceil((float) len / this.bpUnit);
    }

    /**
     * Determines and sets the content area IPD based on available reference
     * area IPD, start- and end-indent properties. end-indent is adjusted based
     * on overconstrained geometry rules, if necessary.
     *
     * @return the resulting content area IPD
     */
    protected int updateContentAreaIPDwithOverconstrainedAdjust() {
        int ipd = this.referenceIPD - (this.startIndent + this.endIndent);
        if (ipd < 0) {
            // 5.3.4, XSL 1.0, Overconstrained Geometry
            log.debug("Adjusting end-indent based on overconstrained geometry rules for "
                    + this.fobj);
            final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                    .get(getFObj().getUserAgent().getEventBroadcaster());
            eventProducer.overconstrainedAdjustEndIndent(this, getFObj()
                    .getName(), ipd, getFObj().getLocator());
            this.endIndent += ipd;
            ipd = 0;
            // TODO Should we skip layout for a block that has ipd=0?
        }
        setContentAreaIPD(ipd);
        return ipd;
    }

    /**
     * Sets the content area IPD by directly supplying the value. end-indent is
     * adjusted based on overconstrained geometry rules, if necessary.
     *
     * @param contentIPD
     *            the IPD of the content
     * @return the resulting content area IPD
     */
    protected int updateContentAreaIPDwithOverconstrainedAdjust(
            final int contentIPD) {
        final int ipd = this.referenceIPD
                - (contentIPD + this.startIndent + this.endIndent);
        if (ipd < 0) {
            // 5.3.4, XSL 1.0, Overconstrained Geometry
            log.debug("Adjusting end-indent based on overconstrained geometry rules for "
                    + this.fobj);
            final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                    .get(getFObj().getUserAgent().getEventBroadcaster());
            eventProducer.overconstrainedAdjustEndIndent(this, getFObj()
                    .getName(), ipd, getFObj().getLocator());
            this.endIndent += ipd;
        }
        setContentAreaIPD(contentIPD);
        return contentIPD;
    }

    /** {@inheritDoc} */
    @Override
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        return getNextKnuthElements(context, alignment, null, null, null);
    }

    /** {@inheritDoc} */
    @Override
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment, final Stack lmStack,
            final Position restartPosition, final LayoutManager restartAtLM) {
        this.referenceIPD = context.getRefIPD();
        updateContentAreaIPDwithOverconstrainedAdjust();

        final boolean isRestart = lmStack != null;
        boolean emptyStack = !isRestart || lmStack.isEmpty();
        final List<ListElement> contentList = new LinkedList<ListElement>();
        final List<ListElement> elements = new LinkedList<ListElement>();

        if (!breakBeforeServed(context, elements)) {
            // if this FO has break-before specified, and it
            // has not yet been processed, return now
            return elements;
        }

        addFirstVisibleMarks(elements, context, alignment);

        // Used to indicate a special break-after case when all content has
        // already been generated.
        BreakElement forcedBreakAfterLast = null;

        LayoutContext childLC;
        List<ListElement> childElements;
        LayoutManager currentChildLM;
        if (isRestart) {
            if (emptyStack) {
                assert restartAtLM != null && restartAtLM.getParent() == this;
                currentChildLM = restartAtLM;
            } else {
                currentChildLM = (LayoutManager) lmStack.pop();
            }
            setCurrentChildLM(currentChildLM);
        } else {
            currentChildLM = getChildLM();
        }

        while (currentChildLM != null) {

            childLC = makeChildLayoutContext(context);

            if (!isRestart || emptyStack) {
                if (isRestart) {
                    currentChildLM.reset(); // TODO won't work with forced
                    // breaks
                }

                childElements = getNextChildElements(currentChildLM, context,
                        childLC, alignment, null, null, null);
            } else {
                // restart && non-empty LM stack
                childElements = getNextChildElements(currentChildLM, context,
                        childLC, alignment, lmStack, restartPosition,
                        restartAtLM);
                // once encountered, irrelevant for following child LMs
                emptyStack = true;
            }

            if (contentList.isEmpty()) {
                // propagate keep-with-previous up from the first child
                context.updateKeepWithPreviousPending(childLC
                        .getKeepWithPreviousPending());
            }

            // handle non-empty child
            if (childElements != null && !childElements.isEmpty()) {
                if (!contentList.isEmpty()
                        && !ElementListUtils
                        .startsWithForcedBreak(childElements)) {
                    // there is a block handled by prevLM before the one
                    // handled by curLM, and the one handled
                    // by the current LM does not begin with a break
                    addInBetweenBreak(contentList, context, childLC);
                }
                if (childElements.size() == 1
                        && ElementListUtils
                        .startsWithForcedBreak(childElements)) {
                    // a descendant of this block has break-before
                    if (currentChildLM.isFinished() && !hasNextChildLM()) {
                        // if there is no more content, make sure pending
                        // marks are cleared
                        forcedBreakAfterLast = (BreakElement) childElements
                                .get(0);
                        context.clearPendingMarks();
                        // break without adding the child elements
                        break;
                    }
                    if (contentList.isEmpty()) {
                        // empty fo:block: zero-length box makes sure the IDs
                        // and/or markers
                        // are registered and borders/padding are painted.
                        elements.add(makeAuxiliaryZeroWidthBox());
                    }
                    // add the forced break
                    contentList.addAll(childElements);
                    // wrap position and return
                    wrapPositionElements(contentList, elements);
                    return elements;
                } else {
                    // add all accumulated child elements
                    contentList.addAll(childElements);
                    if (ElementListUtils.endsWithForcedBreak(childElements)) {
                        // a descendant of this block has break-after
                        if (currentChildLM.isFinished() && !hasNextChildLM()) {
                            // if there is no more content, make sure any
                            // pending marks are cleared
                            forcedBreakAfterLast = (BreakElement) ListUtil
                                    .removeLast(contentList);
                            context.clearPendingMarks();
                            break;
                        }
                        // wrap positions and return
                        wrapPositionElements(contentList, elements);
                        return elements;
                    }
                }
                context.updateKeepWithNextPending(childLC
                        .getKeepWithNextPending());
            }
            currentChildLM = getChildLM();
        }

        if (contentList.isEmpty()) {
            if (forcedBreakAfterLast == null) {
                // empty fo:block: zero-length box makes sure the IDs and/or
                // markers
                // are registered.
                elements.add(makeAuxiliaryZeroWidthBox());
            }
        } else {
            // wrap child positions
            wrapPositionElements(contentList, elements);
        }

        addLastVisibleMarks(elements, context, alignment);

        if (forcedBreakAfterLast == null) {
            addKnuthElementsForBreakAfter(elements, context);
        } else {
            forcedBreakAfterLast.clearPendingMarks();
            elements.add(forcedBreakAfterLast);
        }

        context.updateKeepWithNextPending(getKeepWithNext());
        setFinished(true);
        return elements;
    }

    /**
     * Creates and initializes a {@link LayoutContext} to pass to the child LM
     *
     * @param context
     *            the parent {@link LayoutContext}
     * @return a new child layout context
     */
    protected LayoutContext makeChildLayoutContext(final LayoutContext context) {
        final LayoutContext childLC = new LayoutContext(0);
        childLC.copyPendingMarksFrom(context);
        childLC.setStackLimitBP(context.getStackLimitBP());
        childLC.setRefIPD(this.referenceIPD);
        return childLC;
    }

    /**
     * Checks if this LM's first "visible marks" (= borders, padding, spaces)
     * have already been processed, and if necessary, adds corresponding
     * elements to the specified list, and updates the given layout context
     * accordingly.
     *
     * @param elements
     *            the element list
     * @param context
     *            the layout context
     * @param alignment
     *            the vertical alignment
     */
    protected void addFirstVisibleMarks(final List<ListElement> elements,
            final LayoutContext context, final int alignment) {
        if (!this.firstVisibleMarkServed) {
            addKnuthElementsForSpaceBefore(elements, alignment);
            context.updateKeepWithPreviousPending(getKeepWithPrevious());
        }
        addKnuthElementsForBorderPaddingBefore(elements,
                !this.firstVisibleMarkServed);
        this.firstVisibleMarkServed = true;

        // Spaces, border and padding to be repeated at each break
        addPendingMarks(context);
    }

    /**
     * Adds elements the LM's last/closing marks to the specified list, and
     * updates the layout context accordingly.
     *
     * @param elements
     *            the element list
     * @param context
     *            the layout context
     * @param alignment
     *            the vertical alignment
     */
    protected void addLastVisibleMarks(final List<ListElement> elements,
            final LayoutContext context, final int alignment) {
        addKnuthElementsForBorderPaddingAfter(elements, true);
        addKnuthElementsForSpaceAfter(elements, alignment);

        // All child content processed. Only break-after can occur now, so...
        context.clearPendingMarks();
    }

    /**
     * Check whether there is a break-before condition. If so, and the specified
     * {@code context} allows it, add the necessary elements to the given
     * {@code elements} list.
     *
     * @param context
     *            the layout context
     * @param elements
     *            the element list
     * @return {@code false} if there is a break-before condition, and it has
     *         not been served; {@code true} otherwise
     */
    protected boolean breakBeforeServed(final LayoutContext context,
            final List<ListElement> elements) {
        if (!this.breakBeforeServed) {
            this.breakBeforeServed = true;
            if (!context.suppressBreakBefore()) {
                if (addKnuthElementsForBreakBefore(elements, context)) {
                    return false;
                }
            }
        }
        return this.breakBeforeServed;
    }

    private KnuthBox makeZeroWidthBox() {
        return new KnuthBox(0, new NonLeafPosition(this, null), false);
    }

    private KnuthBox makeAuxiliaryZeroWidthBox() {
        return new KnuthBox(0, notifyPos(new Position(this)), true);
    }

    private KnuthPenalty makeZeroWidthPenalty(final int penaltyValue) {
        return new KnuthPenalty(0, penaltyValue, false, new NonLeafPosition(
                this, null), false);
    }

    private KnuthGlue makeSpaceAdjustmentGlue(final int width,
            final Adjustment adjustmentClass, final boolean isAuxiliary) {
        return new KnuthGlue(width, 0, 0, adjustmentClass, new NonLeafPosition(
                this, null), isAuxiliary);
    }

    /**
     * Gets the next set of child elements for the given childLM. The default
     * implementation basically copies the pending marks to the child layout
     * context, and subsequently calls the appropriate variant of
     * {@code childLM.getNextKnuthElements()}, passing it all relevant
     * parameters.
     *
     * @param childLM
     *            the current child LM
     * @param context
     *            the layout context
     * @param childLC
     *            the child layout context
     * @param alignment
     *            the vertical alignment
     * @param lmStack
     *            the stack of currently active LMs (if any)
     * @param restartPosition
     *            the position to restart from (if any)
     * @param restartAtLM
     *            the LM to restart from (if any)
     * @return list of elements corresponding to the content generated by
     *         childLM
     */
    protected List<ListElement> getNextChildElements(
            final LayoutManager childLM, final LayoutContext context,
            final LayoutContext childLC, final int alignment,
            final Stack<LayoutManager> lmStack, final Position restartPosition,
            final LayoutManager restartAtLM) {

        if (childLM == this.childLMs.get(0)) {
            childLC.setFlags(LayoutContext.SUPPRESS_BREAK_BEFORE);
            // Handled already by the parent (break collapsing, see above)
        }

        if (lmStack == null) {
            // route to default implementation, in case childLM does not provide
            // an override similar to this class
            return childLM.getNextKnuthElements(childLC, alignment);
        } else {
            return childLM.getNextKnuthElements(childLC, alignment, lmStack,
                    restartPosition, restartAtLM);
        }
    }

    /**
     * Adds a break element to the content list between individual child
     * elements.
     *
     * @param contentList
     *            the content list
     * @param parentLC
     *            the parent layout context
     * @param childLC
     *            the currently active child layout context
     */
    protected void addInBetweenBreak(final List<ListElement> contentList,
            final LayoutContext parentLC, final LayoutContext childLC) {

        if (mustKeepTogether() || parentLC.isKeepWithNextPending()
                || childLC.isKeepWithPreviousPending()) {

            Keep keep = getKeepTogether();

            // Handle pending keep-with-next
            keep = keep.compare(parentLC.getKeepWithNextPending());
            parentLC.clearKeepWithNextPending();

            // Handle pending keep-with-previous from child LM
            keep = keep.compare(childLC.getKeepWithPreviousPending());
            childLC.clearKeepWithPreviousPending();

            // add a penalty to forbid or discourage a break between blocks
            contentList.add(new BreakElement(new Position(this), keep
                    .getPenalty(), keep.getContext(), parentLC));
            return;
        }

        final ListElement last = ListUtil.getLast(contentList);
        if (last.isGlue()) {
            // the last element in contentList is a glue;
            // it is a feasible breakpoint, there is no need to add
            // a penalty
            log.warn("glue-type break possibility not handled properly, yet");
            // TODO Does this happen? If yes, need to deal with border and
            // padding
            // at the break possibility
        } else if (!ElementListUtils.endsWithNonInfinitePenalty(contentList)) {

            // TODO vh: this is hacky
            // The getNextKnuthElements method of TableCellLM must not be called
            // twice, otherwise some settings like indents or borders will be
            // counted several times and lead to a wrong output. Anyway the
            // getNextKnuthElements methods should be called only once
            // eventually
            // (i.e., when multi-threading the code), even when there are forced
            // breaks.
            // If we add a break possibility after a forced break the
            // AreaAdditionUtil.addAreas method will act on a sequence starting
            // with a SpaceResolver.SpaceHandlingBreakPosition element, having
            // no
            // LM associated to it. Thus it will stop early instead of adding
            // areas for following Positions. The above test aims at preventing
            // such a situation from occurring. add a null penalty to allow a
            // break
            // between blocks

            // add a null penalty to allow a break between blocks
            contentList.add(new BreakElement(new Position(this), 0,
                    Constants.EN_AUTO, parentLC));
        }
    }

    /** {@inheritDoc} */
    @Override
    public int negotiateBPDAdjustment(final int adj,
            final KnuthElement lastElement) {
        assert lastElement != null && lastElement.getPosition() != null;
        final Position innerPosition = lastElement.getPosition().getPosition();

        if (innerPosition == null && lastElement.isGlue()) {
            // this adjustment applies to space-before or space-after of this
            // block
            if (((KnuthGlue) lastElement).getAdjustmentClass() == Adjustment.SPACE_BEFORE_ADJUSTMENT) {
                // this adjustment applies to space-before
                this.adjustedSpaceBefore += adj;
            } else {
                // this adjustment applies to space-after
                this.adjustedSpaceAfter += adj;
            }
            return adj;
        } else if (innerPosition instanceof MappingPosition) {
            // this block has block-progression-unit > 0: the adjustment can
            // concern
            // - the space-before or space-after of this block,
            // - the line number of a descendant of this block
            final MappingPosition mappingPos = (MappingPosition) innerPosition;
            if (lastElement.isGlue()) {
                // lastElement is a glue
                final ListIterator storedListIterator = this.storedList
                        .listIterator(mappingPos.getFirstIndex());
                int newAdjustment = 0;
                while (storedListIterator.nextIndex() <= mappingPos
                        .getLastIndex()) {
                    final KnuthElement storedElement = (KnuthElement) storedListIterator
                            .next();
                    if (storedElement.isGlue()) {
                        newAdjustment += ((BlockLevelLayoutManager) storedElement
                                .getLayoutManager()).negotiateBPDAdjustment(adj
                                        - newAdjustment, storedElement);
                    }
                }
                newAdjustment = newAdjustment > 0 ? this.bpUnit
                        * neededUnits(newAdjustment) : -this.bpUnit
                        * neededUnits(-newAdjustment);
                return newAdjustment;
            } else {
                // lastElement is a penalty: this means that the paragraph
                // has been split between consecutive pages:
                // this may involve a change in the number of lines
                final KnuthPenalty storedPenalty = (KnuthPenalty) this.storedList
                        .get(mappingPos.getLastIndex());
                if (storedPenalty.getWidth() > 0) {
                    // the original penalty has width > 0
                    return ((BlockLevelLayoutManager) storedPenalty
                            .getLayoutManager()).negotiateBPDAdjustment(
                                    storedPenalty.getWidth(), storedPenalty);
                } else {
                    // the original penalty has width = 0
                    // the adjustment involves only the spaces before and after
                    return adj;
                }
            }
        } else if (innerPosition != null && innerPosition.getLM() != this) {
            // this adjustment concerns another LM
            final NonLeafPosition savedPos = (NonLeafPosition) lastElement
                    .getPosition();
            lastElement.setPosition(innerPosition);
            final int returnValue = ((BlockLevelLayoutManager) lastElement
                    .getLayoutManager()).negotiateBPDAdjustment(adj,
                            lastElement);
            lastElement.setPosition(savedPos);
            return returnValue;
        } else {
            // this should never happen
            log.error("BlockLayoutManager.negotiateBPDAdjustment(): unexpected Position");
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void discardSpace(final KnuthGlue spaceGlue) {
        assert spaceGlue != null && spaceGlue.getPosition() != null;
        final Position innerPosition = spaceGlue.getPosition().getPosition();

        if (innerPosition == null || innerPosition.getLM() == this) {
            // if this block has block-progression-unit > 0, innerPosition can
            // be
            // a MappingPosition
            // spaceGlue represents space before or space after of this block
            if (spaceGlue.getAdjustmentClass() == Adjustment.SPACE_BEFORE_ADJUSTMENT) {
                // space-before must be discarded
                this.adjustedSpaceBefore = 0;
                this.foSpaceBefore = MinOptMax.ZERO;
            } else {
                // space-after must be discarded
                this.adjustedSpaceAfter = 0;
                this.foSpaceAfter = MinOptMax.ZERO;
                // TODO Why are both cases handled in the same way?
            }
        } else {
            // this element was not created by this BlockLM
            final NonLeafPosition savedPos = (NonLeafPosition) spaceGlue
                    .getPosition();
            spaceGlue.setPosition(innerPosition);
            ((BlockLevelLayoutManager) spaceGlue.getLayoutManager())
            .discardSpace(spaceGlue);
            spaceGlue.setPosition(savedPos);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List getChangedKnuthElements(final List oldList, final int alignment) {
        final ListIterator<KnuthElement> oldListIterator = oldList
                .listIterator();
        KnuthElement currElement = null;
        KnuthElement prevElement = null;
        final List<KnuthElement> returnedList = new LinkedList<KnuthElement>();
        final List<KnuthElement> returnList = new LinkedList<KnuthElement>();
        int fromIndex = 0;

        // "unwrap" the Positions stored in the elements
        KnuthElement oldElement;
        while (oldListIterator.hasNext()) {
            oldElement = oldListIterator.next();
            assert oldElement.getPosition() != null;
            final Position innerPosition = oldElement.getPosition()
                    .getPosition();
            if (innerPosition != null) {
                // oldElement was created by a descendant
                oldElement.setPosition(innerPosition);
            } else {
                // oldElement was created by this LM:
                // modify its position in order to recognize it was not created
                // by a child
                oldElement.setPosition(new Position(this));
            }
        }

        // create the iterator
        final ListIterator<KnuthElement> workListIterator = oldList
                .listIterator();
        while (workListIterator.hasNext()) {
            currElement = workListIterator.next();
            if (prevElement != null
                    && prevElement.getLayoutManager() != currElement
                    .getLayoutManager()) {
                // prevElement is the last element generated by the same LM
                final BlockLevelLayoutManager prevLM = (BlockLevelLayoutManager) prevElement
                        .getLayoutManager();
                final BlockLevelLayoutManager currLM = (BlockLevelLayoutManager) currElement
                        .getLayoutManager();
                boolean somethingAdded = false;
                if (prevLM != this) {
                    returnedList.addAll(prevLM.getChangedKnuthElements(
                            oldList.subList(fromIndex,
                                    workListIterator.previousIndex()),
                                    alignment));
                    somethingAdded = true;
                } else {
                    // do nothing
                }
                fromIndex = workListIterator.previousIndex();

                /*
                 * TODO: why are KnuthPenalties added here, while in getNextKE
                 * they were changed to BreakElements?
                 */
                // there is another block after this one
                if (somethingAdded
                        && (mustKeepTogether() || prevLM.mustKeepWithNext() || currLM
                                .mustKeepWithPrevious())) {
                    // add an infinite penalty to forbid a break between blocks
                    returnedList
                    .add(makeZeroWidthPenalty(KnuthElement.INFINITE));
                } else if (somethingAdded
                        && !ListUtil.getLast(returnedList).isGlue()) {
                    // add a null penalty to allow a break between blocks
                    returnedList
                    .add(makeZeroWidthPenalty(KnuthElement.INFINITE));
                }
            }
            prevElement = currElement;
        }
        if (currElement != null) {
            final LayoutManager currLM = currElement.getLayoutManager();
            if (currLM != this) {
                returnedList.addAll(currLM.getChangedKnuthElements(
                        oldList.subList(fromIndex, oldList.size()), alignment));
            } else {
                // there are no more elements to add
                // remove the last penalty added to returnedList
                if (!returnedList.isEmpty()) {
                    ListUtil.removeLast(returnedList);
                }
            }
        }

        // append elements representing space-before
        boolean spaceBeforeIsConditional = true;
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            spaceBeforeIsConditional = getSpaceBeforeProperty().isDiscard();
        }
        if (this.adjustedSpaceBefore != 0) {
            if (!spaceBeforeIsConditional) {
                // add elements to prevent the glue to be discarded
                returnList.add(makeZeroWidthBox());
                returnList.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
            }

            returnList.add(makeSpaceAdjustmentGlue(this.adjustedSpaceBefore,
                    Adjustment.SPACE_BEFORE_ADJUSTMENT, false));
        }

        // "wrap" the Position stored in each element of returnedList
        // and add elements to returnList
        for (final KnuthElement el : returnedList) {
            el.setPosition(new NonLeafPosition(this, el.getPosition()));
            returnList.add(el);
        }

        // append elements representing space-after
        boolean spaceAfterIsConditional = true;
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            spaceAfterIsConditional = getSpaceAfterProperty().isDiscard();
        }
        if (this.adjustedSpaceAfter != 0) {
            if (!spaceAfterIsConditional) {
                returnList.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
            }

            returnList
            .add(makeSpaceAdjustmentGlue(this.adjustedSpaceAfter,
                    Adjustment.SPACE_AFTER_ADJUSTMENT,
                    spaceAfterIsConditional));

            if (!spaceAfterIsConditional) {
                returnList.add(makeZeroWidthBox());
            }
        }

        return returnList;
    }

    /**
     * Retrieves and returns the keep-together strength from the parent element.
     *
     * @return the keep-together strength
     */
    protected Keep getParentKeepTogether() {
        Keep keep = Keep.KEEP_AUTO;
        if (getParent() instanceof BlockLevelLayoutManager) {
            keep = ((BlockLevelLayoutManager) getParent()).getKeepTogether();
        } else if (getParent() instanceof InlineLayoutManager) {
            if (((InlineLayoutManager) getParent()).mustKeepTogether()) {
                keep = Keep.KEEP_ALWAYS;
            }
            // TODO Fix me
            // strength = ((InlineLayoutManager)
            // getParent()).getKeepTogetherStrength();
        }
        return keep;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustKeepTogether() {
        return !getKeepTogether().isAuto();
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustKeepWithPrevious() {
        return !getKeepWithPrevious().isAuto();
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustKeepWithNext() {
        return !getKeepWithNext().isAuto();
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepTogether() {
        Keep keep = Keep.getKeep(getKeepTogetherProperty());
        keep = keep.compare(getParentKeepTogether());
        return keep;
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithPrevious() {
        return Keep.getKeep(getKeepWithPreviousProperty());
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithNext() {
        return Keep.getKeep(getKeepWithNextProperty());
    }

    /**
     * {@inheritDoc} Default implementation throws a
     * {@link IllegalStateException}. Must be implemented by the subclass, if
     * applicable.
     */
    @Override
    public KeepProperty getKeepTogetherProperty() {
        throw new IllegalStateException();
    }

    /**
     * {@inheritDoc} Default implementation throws a
     * {@link IllegalStateException}. Must be implemented by the subclass, if
     * applicable.
     */
    @Override
    public KeepProperty getKeepWithPreviousProperty() {
        throw new IllegalStateException();
    }

    /**
     * {@inheritDoc} Default implementation throws a
     * {@link IllegalStateException}. Must be implemented by the subclass, if
     * applicable.
     */
    @Override
    public KeepProperty getKeepWithNextProperty() {
        throw new IllegalStateException();
    }

    /**
     * Adds the unresolved elements for border and padding to a layout context
     * so break possibilities can be properly constructed.
     *
     * @param context
     *            the layout context
     */
    protected void addPendingMarks(final LayoutContext context) {
        final CommonBorderPaddingBackground borderAndPadding = getBorderPaddingBackground();
        if (borderAndPadding != null) {
            if (borderAndPadding.getBorderBeforeWidth(false) > 0) {
                context.addPendingBeforeMark(new BorderElement(
                        getAuxiliaryPosition(), borderAndPadding.getBorderInfo(
                                CommonBorderPaddingBackground.BEFORE)
                                .getWidth(), RelSide.BEFORE, false, false, this));
            }
            if (borderAndPadding.getPaddingBefore(false, this) > 0) {
                context.addPendingBeforeMark(new PaddingElement(
                        getAuxiliaryPosition(),
                        borderAndPadding
                        .getPaddingLengthProperty(CommonBorderPaddingBackground.BEFORE),
                        RelSide.BEFORE, false, false, this));
            }
            if (borderAndPadding.getBorderAfterWidth(false) > 0) {
                context.addPendingAfterMark(new BorderElement(
                        getAuxiliaryPosition(),
                        borderAndPadding.getBorderInfo(
                                CommonBorderPaddingBackground.AFTER).getWidth(),
                                RelSide.AFTER, false, false, this));
            }
            if (borderAndPadding.getPaddingAfter(false, this) > 0) {
                context.addPendingAfterMark(new PaddingElement(
                        getAuxiliaryPosition(),
                        borderAndPadding
                        .getPaddingLengthProperty(CommonBorderPaddingBackground.AFTER),
                        RelSide.AFTER, false, false, this));
            }
        }
    }

    /** @return the border, padding and background info structure */
    private CommonBorderPaddingBackground getBorderPaddingBackground() {
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            return ((org.apache.fop.fo.flow.Block) this.fobj)
                    .getCommonBorderPaddingBackground();
        } else if (this.fobj instanceof org.apache.fop.fo.flow.BlockContainer) {
            return ((org.apache.fop.fo.flow.BlockContainer) this.fobj)
                    .getCommonBorderPaddingBackground();
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListBlock) {
            return ((org.apache.fop.fo.flow.ListBlock) this.fobj)
                    .getCommonBorderPaddingBackground();
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListItem) {
            return ((org.apache.fop.fo.flow.ListItem) this.fobj)
                    .getCommonBorderPaddingBackground();
        } else if (this.fobj instanceof org.apache.fop.fo.flow.table.Table) {
            return ((org.apache.fop.fo.flow.table.Table) this.fobj)
                    .getCommonBorderPaddingBackground();
        } else {
            return null;
        }
    }

    /** @return the space-before property */
    protected SpaceProperty getSpaceBeforeProperty() {
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            return ((org.apache.fop.fo.flow.Block) this.fobj)
                    .getCommonMarginBlock().spaceBefore;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.BlockContainer) {
            return ((org.apache.fop.fo.flow.BlockContainer) this.fobj)
                    .getCommonMarginBlock().spaceBefore;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListBlock) {
            return ((org.apache.fop.fo.flow.ListBlock) this.fobj)
                    .getCommonMarginBlock().spaceBefore;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListItem) {
            return ((org.apache.fop.fo.flow.ListItem) this.fobj)
                    .getCommonMarginBlock().spaceBefore;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.table.Table) {
            return ((org.apache.fop.fo.flow.table.Table) this.fobj)
                    .getCommonMarginBlock().spaceBefore;
        } else {
            return null;
        }
    }

    /** @return the space-after property */
    protected SpaceProperty getSpaceAfterProperty() {
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            return ((org.apache.fop.fo.flow.Block) this.fobj)
                    .getCommonMarginBlock().spaceAfter;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.BlockContainer) {
            return ((org.apache.fop.fo.flow.BlockContainer) this.fobj)
                    .getCommonMarginBlock().spaceAfter;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListBlock) {
            return ((org.apache.fop.fo.flow.ListBlock) this.fobj)
                    .getCommonMarginBlock().spaceAfter;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListItem) {
            return ((org.apache.fop.fo.flow.ListItem) this.fobj)
                    .getCommonMarginBlock().spaceAfter;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.table.Table) {
            return ((org.apache.fop.fo.flow.table.Table) this.fobj)
                    .getCommonMarginBlock().spaceAfter;
        } else {
            return null;
        }
    }

    /**
     * Creates Knuth elements for before border padding and adds them to the
     * return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param isFirst
     *            true if this is the first time a layout manager instance needs
     *            to generate border and padding
     */
    protected void addKnuthElementsForBorderPaddingBefore(
            final List returnList, final boolean isFirst) {
        // Border and Padding (before)
        final CommonBorderPaddingBackground borderAndPadding = getBorderPaddingBackground();
        if (borderAndPadding != null) {
            if (borderAndPadding.getBorderBeforeWidth(false) > 0) {
                returnList.add(new BorderElement(getAuxiliaryPosition(),
                        borderAndPadding.getBorderInfo(
                                CommonBorderPaddingBackground.BEFORE)
                                .getWidth(), RelSide.BEFORE, isFirst, false,
                                this));
            }
            if (borderAndPadding.getPaddingBefore(false, this) > 0) {
                returnList
                .add(new PaddingElement(
                        getAuxiliaryPosition(),
                        borderAndPadding
                        .getPaddingLengthProperty(CommonBorderPaddingBackground.BEFORE),
                        RelSide.BEFORE, isFirst, false, this));
            }
        }
    }

    /**
     * Creates Knuth elements for after border padding and adds them to the
     * return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param isLast
     *            true if this is the last time a layout manager instance needs
     *            to generate border and padding
     */
    protected void addKnuthElementsForBorderPaddingAfter(final List returnList,
            final boolean isLast) {
        // Border and Padding (after)
        final CommonBorderPaddingBackground borderAndPadding = getBorderPaddingBackground();
        if (borderAndPadding != null) {
            if (borderAndPadding.getPaddingAfter(false, this) > 0) {
                returnList
                .add(new PaddingElement(
                        getAuxiliaryPosition(),
                        borderAndPadding
                        .getPaddingLengthProperty(CommonBorderPaddingBackground.AFTER),
                        RelSide.AFTER, false, isLast, this));
            }
            if (borderAndPadding.getBorderAfterWidth(false) > 0) {
                returnList
                .add(new BorderElement(getAuxiliaryPosition(),
                        borderAndPadding.getBorderInfo(
                                CommonBorderPaddingBackground.AFTER)
                                .getWidth(), RelSide.AFTER, false,
                                isLast, this));
            }
        }
    }

    /**
     * Creates Knuth elements for break-before and adds them to the return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param context
     *            the layout context
     * @return true if an element has been added due to a break-before.
     */
    protected boolean addKnuthElementsForBreakBefore(final List returnList,
            final LayoutContext context) {
        final int breakBefore = BreakOpportunityHelper.getBreakBefore(this);
        if (breakBefore == EN_PAGE || breakBefore == EN_COLUMN
                || breakBefore == EN_EVEN_PAGE || breakBefore == EN_ODD_PAGE) {
            // return a penalty element, representing a forced page break
            returnList.add(new BreakElement(getAuxiliaryPosition(), 0,
                    -KnuthElement.INFINITE, breakBefore, context));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates Knuth elements for break-after and adds them to the return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param context
     *            the layout context
     * @return true if an element has been added due to a break-after.
     */
    protected boolean addKnuthElementsForBreakAfter(final List returnList,
            final LayoutContext context) {
        int breakAfter = -1;
        if (this.fobj instanceof BreakPropertySet) {
            breakAfter = ((BreakPropertySet) this.fobj).getBreakAfter();
        }
        if (breakAfter == EN_PAGE || breakAfter == EN_COLUMN
                || breakAfter == EN_EVEN_PAGE || breakAfter == EN_ODD_PAGE) {
            // add a penalty element, representing a forced page break
            returnList.add(new BreakElement(getAuxiliaryPosition(), 0,
                    -KnuthElement.INFINITE, breakAfter, context));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates Knuth elements for space-before and adds them to the return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param alignment
     *            vertical alignment
     */
    protected void addKnuthElementsForSpaceBefore(final List returnList,
            final int alignment) {
        final SpaceProperty spaceBefore = getSpaceBeforeProperty();
        // append elements representing space-before
        if (spaceBefore != null
                && !(spaceBefore.getMinimum(this).getLength().getValue(this) == 0 && spaceBefore
                .getMaximum(this).getLength().getValue(this) == 0)) {
            returnList.add(new SpaceElement(getAuxiliaryPosition(),
                    spaceBefore, RelSide.BEFORE, true, false, this));
        }
    }

    /**
     * Creates Knuth elements for space-after and adds them to the return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param alignment
     *            vertical alignment
     */
    protected void addKnuthElementsForSpaceAfter(final List returnList,
            final int alignment) {
        final SpaceProperty spaceAfter = getSpaceAfterProperty();
        // append elements representing space-after
        if (spaceAfter != null
                && !(spaceAfter.getMinimum(this).getLength().getValue(this) == 0 && spaceAfter
                .getMaximum(this).getLength().getValue(this) == 0)) {
            returnList.add(new SpaceElement(getAuxiliaryPosition(), spaceAfter,
                    RelSide.AFTER, false, true, this));
        }
    }

    /** A mapping position. */
    protected static class MappingPosition extends Position {

        private final int firstIndex;
        private final int lastIndex;

        /**
         * Construct mapping position.
         *
         * @param lm
         *            layout manager
         * @param first
         *            position
         * @param last
         *            position
         */
        public MappingPosition(final LayoutManager lm, final int first,
                final int last) {
            super(lm);
            this.firstIndex = first;
            this.lastIndex = last;
        }

        /** @return first index */
        public int getFirstIndex() {
            return this.firstIndex;
        }

        /** @return last index */
        public int getLastIndex() {
            return this.lastIndex;
        }
    }

    /**
     * "wrap" the Position inside each element moving the elements from
     * SourceList to targetList
     *
     * @param sourceList
     *            source list
     * @param targetList
     *            target list receiving the wrapped position elements
     */
    protected void wrapPositionElements(final List sourceList,
            final List targetList) {
        wrapPositionElements(sourceList, targetList, false);
    }

    /**
     * "wrap" the Position inside each element moving the elements from
     * SourceList to targetList
     *
     * @param sourceList
     *            source list
     * @param targetList
     *            target list receiving the wrapped position elements
     * @param force
     *            if true, every Position is wrapped regardless of its LM of
     *            origin
     */
    protected void wrapPositionElements(final List sourceList,
            final List targetList, final boolean force) {

        final ListIterator listIter = sourceList.listIterator();
        Object tempElement;
        while (listIter.hasNext()) {
            tempElement = listIter.next();
            if (tempElement instanceof ListElement) {
                wrapPositionElement((ListElement) tempElement, targetList,
                        force);
            } else if (tempElement instanceof List) {
                wrapPositionElements((List) tempElement, targetList, force);
            }
        }
    }

    /**
     * "wrap" the Position inside the given element and add it to the target
     * list.
     *
     * @param el
     *            the list element
     * @param targetList
     *            target list receiving the wrapped position elements
     * @param force
     *            if true, every Position is wrapped regardless of its LM of
     *            origin
     */
    protected void wrapPositionElement(final ListElement el,
            final List targetList, final boolean force) {
        if (force || el.getLayoutManager() != this) {
            el.setPosition(notifyPos(new NonLeafPosition(this, el.getPosition())));
        }
        targetList.add(el);
    }

    /** @return the sum of start-indent and end-indent */
    protected int getIPIndents() {
        return this.startIndent + this.endIndent;
    }

    /**
     * Returns the IPD of the content area
     *
     * @return the IPD of the content area
     */
    @Override
    public int getContentAreaIPD() {
        return this.contentAreaIPD;
    }

    /**
     * Sets the IPD of the content area
     *
     * @param contentAreaIPD
     *            the IPD of the content area
     */
    protected void setContentAreaIPD(final int contentAreaIPD) {
        this.contentAreaIPD = contentAreaIPD;
    }

    /**
     * Returns the BPD of the content area
     *
     * @return the BPD of the content area
     */
    @Override
    public int getContentAreaBPD() {
        return -1;
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        super.reset();
        this.breakBeforeServed = false;
        this.firstVisibleMarkServed = false;
        // TODO startIndent, endIndent
    }

    /**
     * Whether this LM can handle horizontal overflow error messages (only a
     * BlockContainerLayoutManager can).
     *
     * @param milliPoints
     *            horizontal overflow
     * @return true if handled by a BlockContainerLayoutManager
     */
    public boolean handleOverflow(final int milliPoints) {
        if (getParent() instanceof BlockStackingLayoutManager) {
            return ((BlockStackingLayoutManager) getParent())
                    .handleOverflow(milliPoints);
        }
        return false;
    }
}
