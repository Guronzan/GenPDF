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

/* $Id: TableAndCaptionLayoutManager.java 990148 2010-08-27 13:31:41Z vhennebert $ */

package org.apache.fop.layoutmgr.table;

import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.fo.flow.table.TableAndCaption;
import org.apache.fop.layoutmgr.BlockStackingLayoutManager;
import org.apache.fop.layoutmgr.Keep;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.PositionIterator;

/**
 * LayoutManager for a table-and-caption FO. A table and caption consists of a
 * table and a caption. The caption contains blocks that are positioned next to
 * the table on the caption side. The caption blocks have an implicit keep with
 * the table. TODO Implement getNextKnuthElements()
 */
public class TableAndCaptionLayoutManager extends BlockStackingLayoutManager {

    private Block curBlockArea;

    // private List childBreaks = new java.util.ArrayList();

    /**
     * Create a new table and caption layout manager.
     * 
     * @param node
     *            table-and-caption FO
     */
    public TableAndCaptionLayoutManager(final TableAndCaption node) {
        super(node);
    }

    /**
     * Returns the table-and-caption formatting object.
     * 
     * @return the table-and-caption formatting object
     */
    public TableAndCaption getTableAndCaptionFO() {
        return (TableAndCaption) this.fobj;
    }

    /**
     * Get the next break possibility.
     *
     * @param context
     *            the layout context for getting breaks
     * @return the next break possibility
     */
    /*
     * public BreakPoss getNextBreakPoss(LayoutContext context) { LayoutManager
     * curLM; // currently active LM
     * 
     * MinOptMax stackSize = new MinOptMax(); // if starting add space before //
     * stackSize.add(spaceBefore); BreakPoss lastPos = null;
     * 
     * // if there is a caption then get the side and work out when // to handle
     * it
     * 
     * while ((curLM = getChildLM()) != null) { // Make break positions and
     * return blocks! // Set up a LayoutContext int ipd = context.getRefIPD();
     * BreakPoss bp;
     * 
     * LayoutContext childLC = new LayoutContext(0); // if line layout manager
     * then set stack limit to ipd // line LM actually generates a LineArea
     * which is a block childLC.setStackLimit(
     * MinOptMax.subtract(context.getStackLimit(), stackSize));
     * childLC.setRefIPD(ipd);
     * 
     * boolean over = false; while (!curLM.isFinished()) { if ((bp =
     * curLM.getNextBreakPoss(childLC)) != null) { if (stackSize.opt +
     * bp.getStackingSize().opt > context.getStackLimit().max) { // reset to
     * last break if (lastPos != null) { LayoutManager lm =
     * lastPos.getLayoutManager(); lm.resetPosition(lastPos.getPosition()); if
     * (lm != curLM) { curLM.resetPosition(null); } } else {
     * curLM.resetPosition(null); } over = true; break; }
     * stackSize.add(bp.getStackingSize()); lastPos = bp; childBreaks.add(bp);
     * 
     * if (bp.nextBreakOverflows()) { over = true; break; }
     * 
     * childLC.setStackLimit(MinOptMax.subtract( context.getStackLimit(),
     * stackSize)); } } BreakPoss breakPoss = new BreakPoss( new
     * LeafPosition(this, childBreaks.size() - 1)); if (over) {
     * breakPoss.setFlag(BreakPoss.NEXT_OVERFLOWS, true); }
     * breakPoss.setStackingSize(stackSize); return breakPoss; }
     * setFinished(true); return null; }
     */

    /**
     * Add the areas.
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

        /*
         * TODO: Reimplement using Knuth approach LayoutManager childLM; int
         * iStartPos = 0; LayoutContext lc = new LayoutContext(0); while
         * (parentIter.hasNext()) { LeafPosition lfp = (LeafPosition)
         * parentIter.next(); // Add the block areas to Area PositionIterator
         * breakPosIter = new BreakPossPosIter( childBreaks, iStartPos,
         * lfp.getLeafPos() + 1); iStartPos = lfp.getLeafPos() + 1; while
         * ((childLM = breakPosIter.getNextChildLM()) != null) {
         * childLM.addAreas(breakPosIter, lc); } }
         */

        flush();

        // childBreaks.clear();
        this.curBlockArea = null;
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
     *            the child area to locate the parent
     * @return the area for this table and caption
     */
    @Override
    public Area getParentArea(final Area childArea) {
        if (this.curBlockArea == null) {
            this.curBlockArea = new Block();
            // Set up dimensions
            // Must get dimensions from parent area
            final Area parentArea = this.parentLayoutManager
                    .getParentArea(this.curBlockArea);
            final int referenceIPD = parentArea.getIPD();
            this.curBlockArea.setIPD(referenceIPD);
            // Get reference IPD from parentArea
            setCurrentArea(this.curBlockArea); // ??? for generic operations
        }
        return this.curBlockArea;
    }

    /**
     * Add the child to the current area.
     *
     * @param childArea
     *            the area to add
     */
    @Override
    public void addChildArea(final Area childArea) {
        if (this.curBlockArea != null) {
            this.curBlockArea.addBlock((Block) childArea);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithNext() {
        return Keep.KEEP_AUTO;
        /*
         * TODO Complete me! return KeepUtil.getCombinedBlockLevelKeepStrength(
         * getTableAndCaptionFO().getKeepWithNext());
         */
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithPrevious() {
        return Keep.KEEP_AUTO;
        /*
         * TODO Complete me! return KeepUtil.getCombinedBlockLevelKeepStrength(
         * getTableAndCaptionFO().getKeepWithPrevious());
         */
    }

}
