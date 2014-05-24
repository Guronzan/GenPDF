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

/* $Id: FlowLayoutManager.java 1297008 2012-03-05 11:19:47Z vhennebert $ */

package org.apache.fop.layoutmgr;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Area;
import org.apache.fop.area.BlockParent;
import org.apache.fop.fo.pagination.Flow;
import org.apache.fop.util.ListUtil;

/**
 * LayoutManager for an fo:flow object. Its parent LM is the
 * PageSequenceLayoutManager. This LM is responsible for getting columns of the
 * appropriate size and filling them with block-level areas generated by its
 * children. TODO Reintroduce emergency counter (generate error to avoid endless
 * loop)
 */
@Slf4j
public class FlowLayoutManager extends BlockStackingLayoutManager {

    /** Array of areas currently being filled stored by area class */
    private final BlockParent[] currentAreas = new BlockParent[Area.CLASS_MAX];

    /**
     * This is the top level layout manager. It is created by the PageSequence
     * FO.
     *
     * @param pslm
     *            parent PageSequenceLayoutManager object
     * @param node
     *            Flow object
     */
    public FlowLayoutManager(final PageSequenceLayoutManager pslm,
            final Flow node) {
        super(node);
        setParent(pslm);
    }

    /** {@inheritDoc} */
    @Override
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        return getNextKnuthElements(context, alignment, null, null);
    }

    /**
     * Get a sequence of KnuthElements representing the content of the node
     * assigned to the LM.
     *
     * @param context
     *            the LayoutContext used to store layout information
     * @param alignment
     *            the desired text alignment
     * @param restartPosition
     *            {@link Position} to restart from
     * @param restartLM
     *            {@link LayoutManager} to restart from
     * @return the list of KnuthElements
     * @see LayoutManager#getNextKnuthElements(LayoutContext,int)
     */
    List getNextKnuthElements(final LayoutContext context, final int alignment,
            final Position restartPosition, final LayoutManager restartLM) {

        final List<ListElement> elements = new LinkedList<ListElement>();

        final boolean isRestart = restartPosition != null;
        // always reset in case of restart (exception: see below)
        boolean doReset = isRestart;
        LayoutManager currentChildLM;
        final Stack<LayoutManager> lmStack = new Stack<LayoutManager>();
        if (isRestart) {
            currentChildLM = restartPosition.getLM();
            if (currentChildLM == null) {
                throw new IllegalStateException(
                        "Cannot find layout manager to restart from");
            }
            if (restartLM != null && restartLM.getParent() == this) {
                currentChildLM = restartLM;
            } else {
                while (currentChildLM.getParent() != this) {
                    lmStack.push(currentChildLM);
                    currentChildLM = currentChildLM.getParent();
                }
                doReset = false;
            }
            setCurrentChildLM(currentChildLM);
        } else {
            currentChildLM = getChildLM();
        }

        while (currentChildLM != null) {
            if (!isRestart || doReset) {
                if (doReset) {
                    currentChildLM.reset(); // TODO won't work with forced
                    // breaks
                }
                if (addChildElements(elements, currentChildLM, context,
                        alignment, null, null, null) != null) {
                    return elements;
                }
            } else {
                if (addChildElements(elements, currentChildLM, context,
                        alignment, lmStack, restartPosition, restartLM) != null) {
                    return elements;
                }
                // restarted; force reset as of next child
                doReset = true;
            }
            currentChildLM = getChildLM();
        }

        SpaceResolver.resolveElementList(elements);
        setFinished(true);

        assert !elements.isEmpty();
        return elements;
    }

    private List<ListElement> addChildElements(
            final List<ListElement> elements, final LayoutManager childLM,
            final LayoutContext context, final int alignment,
            final Stack<LayoutManager> lmStack, final Position position,
            final LayoutManager restartAtLM) {
        if (handleSpanChange(childLM, context)) {
            SpaceResolver.resolveElementList(elements);
            return elements;
        }

        final LayoutContext childLC = makeChildLayoutContext(context);
        final List<ListElement> childElements = getNextChildElements(childLM,
                context, childLC, alignment, lmStack, position, restartAtLM);
        if (elements.isEmpty()) {
            context.updateKeepWithPreviousPending(childLC
                    .getKeepWithPreviousPending());
        }
        if (!elements.isEmpty()
                && !ElementListUtils.startsWithForcedBreak(childElements)) {
            addInBetweenBreak(elements, context, childLC);
        }
        context.updateKeepWithNextPending(childLC.getKeepWithNextPending());

        elements.addAll(childElements);

        if (ElementListUtils.endsWithForcedBreak(elements)) {
            // a descendant of this flow has break-before or break-after
            if (childLM.isFinished() && !hasNextChildLM()) {
                setFinished(true);
            }
            SpaceResolver.resolveElementList(elements);
            return elements;
        }
        return null;
    }

    private boolean handleSpanChange(final LayoutManager childLM,
            final LayoutContext context) {
        int span = EN_NONE;
        int disableColumnBalancing = EN_FALSE;
        if (childLM instanceof BlockLayoutManager) {
            span = ((BlockLayoutManager) childLM).getBlockFO().getSpan();
            disableColumnBalancing = ((BlockLayoutManager) childLM)
                    .getBlockFO().getDisableColumnBalancing();
        } else if (childLM instanceof BlockContainerLayoutManager) {
            span = ((BlockContainerLayoutManager) childLM)
                    .getBlockContainerFO().getSpan();
            disableColumnBalancing = ((BlockContainerLayoutManager) childLM)
                    .getBlockContainerFO().getDisableColumnBalancing();
        }

        final int currentSpan = context.getCurrentSpan();
        if (currentSpan != span) {
            if (span == EN_ALL) {
                context.setDisableColumnBalancing(disableColumnBalancing);
            }
            log.debug("span change from " + currentSpan + " to " + span);
            context.signalSpanChange(span);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Overridden to take into account the current page-master's writing-mode
     * {@inheritDoc}
     */
    @Override
    protected LayoutContext makeChildLayoutContext(final LayoutContext context) {
        final LayoutContext childLC = new LayoutContext(0);
        childLC.setStackLimitBP(context.getStackLimitBP());
        childLC.setRefIPD(context.getRefIPD());
        childLC.setWritingMode(getCurrentPage().getSimplePageMaster()
                .getWritingMode());
        return childLC;
    }

    /**
     * Overridden to wrap the child positions before returning the list
     * {@inheritDoc}
     */
    @Override
    protected List<ListElement> getNextChildElements(
            final LayoutManager childLM, final LayoutContext context,
            final LayoutContext childLC, final int alignment,
            final Stack<LayoutManager> lmStack, final Position restartPosition,
            final LayoutManager restartLM) {

        List<ListElement> childElements;
        if (lmStack == null) {
            childElements = childLM.getNextKnuthElements(childLC, alignment);
        } else {
            childElements = childLM.getNextKnuthElements(childLC, alignment,
                    lmStack, restartPosition, restartLM);
        }
        assert !childElements.isEmpty();

        // "wrap" the Position inside each element
        final List tempList = childElements;
        childElements = new LinkedList<ListElement>();
        wrapPositionElements(tempList, childElements);
        return childElements;
    }

    /** {@inheritDoc} */
    @Override
    public int negotiateBPDAdjustment(final int adj,
            final KnuthElement lastElement) {
        log.debug(" FLM.negotiateBPDAdjustment> " + adj);

        if (lastElement.getPosition() instanceof NonLeafPosition) {
            // this element was not created by this FlowLM
            final NonLeafPosition savedPos = (NonLeafPosition) lastElement
                    .getPosition();
            lastElement.setPosition(savedPos.getPosition());
            final int returnValue = ((BlockLevelLayoutManager) lastElement
                    .getLayoutManager()).negotiateBPDAdjustment(adj,
                    lastElement);
            lastElement.setPosition(savedPos);
            log.debug(" FLM.negotiateBPDAdjustment> result " + returnValue);
            return returnValue;
        } else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void discardSpace(final KnuthGlue spaceGlue) {
        log.debug(" FLM.discardSpace> ");

        if (spaceGlue.getPosition() instanceof NonLeafPosition) {
            // this element was not created by this FlowLM
            final NonLeafPosition savedPos = (NonLeafPosition) spaceGlue
                    .getPosition();
            spaceGlue.setPosition(savedPos.getPosition());
            ((BlockLevelLayoutManager) spaceGlue.getLayoutManager())
                    .discardSpace(spaceGlue);
            spaceGlue.setPosition(savedPos);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepTogether() {
        return Keep.KEEP_AUTO;
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithNext() {
        return Keep.KEEP_AUTO;
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithPrevious() {
        return Keep.KEEP_AUTO;
    }

    /** {@inheritDoc} */
    @Override
    public List<KnuthElement> getChangedKnuthElements(final List oldList,
            final int alignment) {
        ListIterator<KnuthElement> oldListIterator = oldList.listIterator();
        KnuthElement returnedElement;
        final List<KnuthElement> returnedList = new LinkedList<KnuthElement>();
        final List<KnuthElement> returnList = new LinkedList<KnuthElement>();
        KnuthElement prevElement = null;
        KnuthElement currElement = null;
        int fromIndex = 0;

        // "unwrap" the Positions stored in the elements
        KnuthElement oldElement;
        while (oldListIterator.hasNext()) {
            oldElement = oldListIterator.next();
            if (oldElement.getPosition() instanceof NonLeafPosition) {
                // oldElement was created by a descendant of this FlowLM
                oldElement.setPosition(oldElement.getPosition().getPosition());
            } else {
                // thisElement was created by this FlowLM, remove it
                oldListIterator.remove();
            }
        }
        // reset the iterator
        oldListIterator = oldList.listIterator();

        while (oldListIterator.hasNext()) {
            currElement = oldListIterator.next();
            if (prevElement != null
                    && prevElement.getLayoutManager() != currElement
                            .getLayoutManager()) {
                // prevElement is the last element generated by the same LM
                final BlockLevelLayoutManager prevLM = (BlockLevelLayoutManager) prevElement
                        .getLayoutManager();
                final BlockLevelLayoutManager currLM = (BlockLevelLayoutManager) currElement
                        .getLayoutManager();
                returnedList.addAll(prevLM.getChangedKnuthElements(
                        oldList.subList(fromIndex,
                                oldListIterator.previousIndex()), alignment));
                fromIndex = oldListIterator.previousIndex();

                // there is another block after this one
                if (prevLM.mustKeepWithNext() || currLM.mustKeepWithPrevious()) {
                    // add an infinite penalty to forbid a break between blocks
                    returnedList.add(new KnuthPenalty(0, KnuthElement.INFINITE,
                            false, new Position(this), false));
                } else if (!ListUtil.getLast(returnedList).isGlue()) {
                    // add a null penalty to allow a break between blocks
                    returnedList.add(new KnuthPenalty(0, 0, false,
                            new Position(this), false));
                }
            }
            prevElement = currElement;
        }
        if (currElement != null) {
            final BlockLevelLayoutManager currLM = (BlockLevelLayoutManager) currElement
                    .getLayoutManager();
            returnedList.addAll(currLM.getChangedKnuthElements(
                    oldList.subList(fromIndex, oldList.size()), alignment));
        }

        // "wrap" the Position stored in each element of returnedList
        // and add elements to returnList
        final ListIterator<KnuthElement> listIter = returnedList.listIterator();
        while (listIter.hasNext()) {
            returnedElement = listIter.next();
            if (returnedElement.getLayoutManager() != this) {
                returnedElement.setPosition(new NonLeafPosition(this,
                        returnedElement.getPosition()));
            }
            returnList.add(returnedElement);
        }

        return returnList;
    }

    /** {@inheritDoc} */
    @Override
    public void addAreas(final PositionIterator parentIter,
            final LayoutContext layoutContext) {
        AreaAdditionUtil.addAreas(this, parentIter, layoutContext);
        flush();
    }

    /**
     * Add child area to a the correct container, depending on its area class. A
     * Flow can fill at most one area container of any class at any one time.
     * The actual work is done by BlockStackingLM.
     *
     * @param childArea
     *            the area to add
     */
    @Override
    public void addChildArea(final Area childArea) {
        getParentArea(childArea);
        addChildToArea(childArea, this.currentAreas[childArea.getAreaClass()]);
    }

    /** {@inheritDoc} */
    @Override
    public Area getParentArea(final Area childArea) {
        BlockParent parentArea = null;
        final int aclass = childArea.getAreaClass();

        if (aclass == Area.CLASS_NORMAL) {
            parentArea = getCurrentPV().getCurrentFlow();
        } else if (aclass == Area.CLASS_BEFORE_FLOAT) {
            parentArea = getCurrentPV().getBodyRegion().getBeforeFloat();
        } else if (aclass == Area.CLASS_FOOTNOTE) {
            parentArea = getCurrentPV().getBodyRegion().getFootnote();
        } else {
            throw new IllegalStateException("(internal error) Invalid "
                    + "area class (" + aclass + ") requested.");
        }

        this.currentAreas[aclass] = parentArea;
        setCurrentArea(parentArea);
        return parentArea;
    }

    /**
     * Returns the IPD of the content area
     *
     * @return the IPD of the content area
     */
    @Override
    public int getContentAreaIPD() {
        return getCurrentPV().getCurrentSpan().getColumnWidth();
    }

    /**
     * Returns the BPD of the content area
     *
     * @return the BPD of the content area
     */
    @Override
    public int getContentAreaBPD() {
        return getCurrentPV().getBodyRegion().getBPD();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRestartable() {
        return true;
    }

}
