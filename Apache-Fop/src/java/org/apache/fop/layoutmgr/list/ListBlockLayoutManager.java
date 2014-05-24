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

/* $Id: ListBlockLayoutManager.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.layoutmgr.list;

import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.fo.flow.ListBlock;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.layoutmgr.BlockStackingLayoutManager;
import org.apache.fop.layoutmgr.ConditionalElementListener;
import org.apache.fop.layoutmgr.ElementListUtils;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.NonLeafPosition;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.RelSide;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.traits.SpaceVal;

/**
 * LayoutManager for a list-block FO. A list block contains list items which are
 * stacked within the list block area..
 */
@Slf4j
public class ListBlockLayoutManager extends BlockStackingLayoutManager
implements ConditionalElementListener {

    private Block curBlockArea;

    private boolean discardBorderBefore;
    private boolean discardBorderAfter;
    private boolean discardPaddingBefore;
    private boolean discardPaddingAfter;
    private MinOptMax effSpaceBefore;
    private MinOptMax effSpaceAfter;

    /**
     * Create a new list block layout manager.
     *
     * @param node
     *            list-block to create the layout manager for
     */
    public ListBlockLayoutManager(final ListBlock node) {
        super(node);
    }

    /**
     * Convenience method.
     *
     * @return the ListBlock node
     */
    protected ListBlock getListBlockFO() {
        return (ListBlock) this.fobj;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        this.foSpaceBefore = new SpaceVal(getListBlockFO()
                .getCommonMarginBlock().spaceBefore, this).getSpace();
        this.foSpaceAfter = new SpaceVal(getListBlockFO()
                .getCommonMarginBlock().spaceAfter, this).getSpace();
        this.startIndent = getListBlockFO().getCommonMarginBlock().startIndent
                .getValue(this);
        this.endIndent = getListBlockFO().getCommonMarginBlock().endIndent
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
        resetSpaces();
        final List returnList = super.getNextKnuthElements(context, alignment);

        // fox:widow-content-limit
        final int widowRowLimit = getListBlockFO().getWidowContentLimit()
                .getValue();
        if (widowRowLimit != 0) {
            ElementListUtils.removeLegalBreaks(returnList, widowRowLimit);
        }

        // fox:orphan-content-limit
        final int orphanRowLimit = getListBlockFO().getOrphanContentLimit()
                .getValue();
        if (orphanRowLimit != 0) {
            ElementListUtils.removeLegalBreaksFromEnd(returnList,
                    orphanRowLimit);
        }

        return returnList;
    }

    /**
     * A list block generates one or more normal block areas whose child areas
     * are normal block areas returned by the children of fo:list-block. See
     * XSL-FO 1.1 6.8.2.
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

        // if this will create the first block area in a page
        // and display-align is after or center, add space before
        if (layoutContext.getSpaceBefore() > 0) {
            addBlockSpacing(0.0,
                    MinOptMax.getInstance(layoutContext.getSpaceBefore()));
        }

        addId();

        // the list block contains areas stacked from each list item

        LayoutManager childLM;
        final LayoutContext lc = new LayoutContext(0);
        LayoutManager firstLM = null;
        LayoutManager lastLM = null;
        Position firstPos = null;
        Position lastPos = null;

        // "unwrap" the NonLeafPositions stored in parentIter
        // and put them in a new list;
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
            if (pos instanceof NonLeafPosition && pos.getPosition() != null
                    && pos.getPosition().getLM() != this) {
                // pos was created by a child of this ListBlockLM
                positionList.add(pos.getPosition());
                lastLM = pos.getPosition().getLM();
                if (firstLM == null) {
                    firstLM = lastLM;
                }
            }
        }

        addMarkersToPage(true, isFirst(firstPos), isLast(lastPos));

        final PositionIterator childPosIter = new PositionIterator(
                positionList.listIterator());
        while ((childLM = childPosIter.getNextChildLM()) != null) {
            // Add the block areas to Area
            // set the space adjustment ratio
            lc.setSpaceAdjust(layoutContext.getSpaceAdjust());
            lc.setFlags(LayoutContext.FIRST_AREA, childLM == firstLM);
            lc.setFlags(LayoutContext.LAST_AREA, childLM == lastLM);
            lc.setStackLimitBP(layoutContext.getStackLimitBP());
            childLM.addAreas(childPosIter, lc);
        }

        addMarkersToPage(false, isFirst(firstPos), isLast(lastPos));

        // We are done with this area add the background
        TraitSetter.addBackground(this.curBlockArea, getListBlockFO()
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
     * @return the parent area of the child
     */
    @Override
    public Area getParentArea(final Area childArea) {
        if (this.curBlockArea == null) {
            this.curBlockArea = new Block();

            // Set up dimensions
            // Must get dimensions from parent area
            /* Area parentArea = */this.parentLayoutManager
            .getParentArea(this.curBlockArea);

            // set traits
            TraitSetter.setProducerID(this.curBlockArea, getListBlockFO()
                    .getId());
            TraitSetter.addBorders(this.curBlockArea, getListBlockFO()
                    .getCommonBorderPaddingBackground(),
                    this.discardBorderBefore, this.discardBorderAfter, false,
                    false, this);
            TraitSetter.addPadding(this.curBlockArea, getListBlockFO()
                    .getCommonBorderPaddingBackground(),
                    this.discardPaddingBefore, this.discardPaddingAfter, false,
                    false, this);
            TraitSetter.addMargins(this.curBlockArea, getListBlockFO()
                    .getCommonBorderPaddingBackground(), getListBlockFO()
                    .getCommonMarginBlock(), this);
            TraitSetter.addBreaks(this.curBlockArea, getListBlockFO()
                    .getBreakBefore(), getListBlockFO().getBreakAfter());

            final int contentIPD = this.referenceIPD - getIPIndents();
            this.curBlockArea.setIPD(contentIPD);

            setCurrentArea(this.curBlockArea);
        }
        return this.curBlockArea;
    }

    /**
     * Add the child area to this layout manager.
     *
     * @param childArea
     *            the child area to add
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
        return getListBlockFO().getKeepTogether();
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepWithPreviousProperty() {
        return getListBlockFO().getKeepWithPrevious();
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepWithNextProperty() {
        return getListBlockFO().getKeepWithNext();
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

}
