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

/* $Id: TableLayoutManager.java 992029 2010-09-02 17:34:43Z vhennebert $ */

package org.apache.fop.layoutmgr.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.datatypes.LengthBase;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.flow.table.Table;
import org.apache.fop.fo.flow.table.TableColumn;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.layoutmgr.BlockLevelEventProducer;
import org.apache.fop.layoutmgr.BlockStackingLayoutManager;
import org.apache.fop.layoutmgr.BreakElement;
import org.apache.fop.layoutmgr.ConditionalElementListener;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthGlue;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LeafPosition;
import org.apache.fop.layoutmgr.ListElement;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.RelSide;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.traits.SpaceVal;
import org.apache.fop.util.BreakUtil;

/**
 * LayoutManager for a table FO. A table consists of columns, table header,
 * table footer and multiple table bodies. The header, footer and body add the
 * areas created from the table cells. The table then creates areas for the
 * columns, bodies and rows the render background.
 */
@Slf4j
public class TableLayoutManager extends BlockStackingLayoutManager implements
ConditionalElementListener {

    private TableContentLayoutManager contentLM;
    private ColumnSetup columns = null;

    private Block curBlockArea;

    private double tableUnit;
    private boolean autoLayout = true;

    private boolean discardBorderBefore;
    private boolean discardBorderAfter;
    private boolean discardPaddingBefore;
    private boolean discardPaddingAfter;
    private MinOptMax effSpaceBefore;
    private MinOptMax effSpaceAfter;

    private int halfBorderSeparationBPD;
    private int halfBorderSeparationIPD;

    /**
     * See
     * {@link TableLayoutManager#registerColumnBackgroundArea(TableColumn, Block, int)}
     * .
     */
    private List columnBackgroundAreas;

    private Position auxiliaryPosition;

    /**
     * Temporary holder of column background informations for a table-cell's
     * area.
     *
     * @see TableLayoutManager#registerColumnBackgroundArea(TableColumn, Block,
     *      int)
     */
    private static final class ColumnBackgroundInfo {
        private final TableColumn column;
        private final Block backgroundArea;
        private final int xShift;

        private ColumnBackgroundInfo(final TableColumn column,
                final Block backgroundArea, final int xShift) {
            this.column = column;
            this.backgroundArea = backgroundArea;
            this.xShift = xShift;
        }
    }

    /**
     * Create a new table layout manager.
     *
     * @param node
     *            the table FO
     */
    public TableLayoutManager(final Table node) {
        super(node);
        this.columns = new ColumnSetup(node);
    }

    /** @return the table FO */
    public Table getTable() {
        return (Table) this.fobj;
    }

    /**
     * @return the column setup for this table.
     */
    public ColumnSetup getColumns() {
        return this.columns;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        this.foSpaceBefore = new SpaceVal(
                getTable().getCommonMarginBlock().spaceBefore, this).getSpace();
        this.foSpaceAfter = new SpaceVal(
                getTable().getCommonMarginBlock().spaceAfter, this).getSpace();
        this.startIndent = getTable().getCommonMarginBlock().startIndent
                .getValue(this);
        this.endIndent = getTable().getCommonMarginBlock().endIndent
                .getValue(this);

        if (getTable().isSeparateBorderModel()) {
            this.halfBorderSeparationBPD = getTable().getBorderSeparation()
                    .getBPD().getLength().getValue(this) / 2;
            this.halfBorderSeparationIPD = getTable().getBorderSeparation()
                    .getIPD().getLength().getValue(this) / 2;
        } else {
            this.halfBorderSeparationBPD = 0;
            this.halfBorderSeparationIPD = 0;
        }

        if (!getTable().isAutoLayout()
                && getTable().getInlineProgressionDimension().getOptimum(this)
                .getEnum() != EN_AUTO) {
            this.autoLayout = false;
        }
    }

    private void resetSpaces() {
        this.discardBorderBefore = false;
        this.discardBorderAfter = false;
        this.discardPaddingBefore = false;
        this.discardPaddingAfter = false;
        this.effSpaceBefore = null;
        this.effSpaceAfter = null;
    }

    /**
     * @return half the value of border-separation.block-progression-dimension,
     *         or 0 if border-collapse="collapse".
     */
    public int getHalfBorderSeparationBPD() {
        return this.halfBorderSeparationBPD;
    }

    /**
     * @return half the value of border-separation.inline-progression-dimension,
     *         or 0 if border-collapse="collapse".
     */
    public int getHalfBorderSeparationIPD() {
        return this.halfBorderSeparationIPD;
    }

    /** {@inheritDoc} */
    @Override
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment) {

        final List returnList = new LinkedList();

        /*
         * Compute the IPD and adjust it if necessary (overconstrained)
         */
        this.referenceIPD = context.getRefIPD();
        if (getTable().getInlineProgressionDimension().getOptimum(this)
                .getEnum() != EN_AUTO) {
            final int contentIPD = getTable().getInlineProgressionDimension()
                    .getOptimum(this).getLength().getValue(this);
            updateContentAreaIPDwithOverconstrainedAdjust(contentIPD);
        } else {
            if (!getTable().isAutoLayout()) {
                final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                        .get(getTable().getUserAgent().getEventBroadcaster());
                eventProducer.tableFixedAutoWidthNotSupported(this, getTable()
                        .getLocator());
            }
            updateContentAreaIPDwithOverconstrainedAdjust();
        }
        final int sumOfColumns = this.columns.getSumOfColumnWidths(this);
        if (!this.autoLayout && sumOfColumns > getContentAreaIPD()) {
            log.debug(FONode
                    .decorateWithContextInfo(
                            "The sum of all column widths is larger than the specified table width.",
                            getTable()));
            updateContentAreaIPDwithOverconstrainedAdjust(sumOfColumns);
        }
        final int availableIPD = this.referenceIPD - getIPIndents();
        if (getContentAreaIPD() > availableIPD) {
            final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                    .get(getTable().getUserAgent().getEventBroadcaster());
            eventProducer.objectTooWide(this, getTable().getName(),
                    getContentAreaIPD(), context.getRefIPD(), getTable()
                    .getLocator());
        }

        /*
         * initialize unit to determine computed values for
         * proportional-column-width()
         */
        if (this.tableUnit == 0.0) {
            this.tableUnit = this.columns.computeTableUnit(this);
        }

        if (!this.firstVisibleMarkServed) {
            addKnuthElementsForSpaceBefore(returnList, alignment);
        }

        if (getTable().isSeparateBorderModel()) {
            addKnuthElementsForBorderPaddingBefore(returnList,
                    !this.firstVisibleMarkServed);
            this.firstVisibleMarkServed = true;
            // Border and padding to be repeated at each break
            // This must be done only in the separate-border model, as in
            // collapsing
            // tables have no padding and borders are determined at the cell
            // level
            addPendingMarks(context);
        }

        // Elements for the table-header/footer/body
        List contentKnuthElements;
        this.contentLM = new TableContentLayoutManager(this);
        final LayoutContext childLC = new LayoutContext(0);
        /*
         * childLC.setStackLimit( MinOptMax.subtract(context.getStackLimit(),
         * stackSize));
         */
        childLC.setRefIPD(context.getRefIPD());
        childLC.copyPendingMarksFrom(context);

        contentKnuthElements = this.contentLM.getNextKnuthElements(childLC,
                alignment);
        // Set index values on elements coming from the content LM
        final Iterator iter = contentKnuthElements.iterator();
        while (iter.hasNext()) {
            final ListElement el = (ListElement) iter.next();
            notifyPos(el.getPosition());
        }
        // TODO fixme : don't toString() a list...
        log.debug(contentKnuthElements.toString());
        wrapPositionElements(contentKnuthElements, returnList);

        context.updateKeepWithPreviousPending(getKeepWithPrevious());
        context.updateKeepWithPreviousPending(childLC
                .getKeepWithPreviousPending());

        context.updateKeepWithNextPending(getKeepWithNext());
        context.updateKeepWithNextPending(childLC.getKeepWithNextPending());

        if (getTable().isSeparateBorderModel()) {
            addKnuthElementsForBorderPaddingAfter(returnList, true);
        }
        addKnuthElementsForSpaceAfter(returnList, alignment);

        if (!context.suppressBreakBefore()) {
            // addKnuthElementsForBreakBefore(returnList, context);
            final int breakBefore = BreakUtil.compareBreakClasses(getTable()
                    .getBreakBefore(), childLC.getBreakBefore());
            if (breakBefore != Constants.EN_AUTO) {
                returnList.add(0, new BreakElement(getAuxiliaryPosition(), 0,
                        -KnuthElement.INFINITE, breakBefore, context));
            }
        }

        // addKnuthElementsForBreakAfter(returnList, context);
        final int breakAfter = BreakUtil.compareBreakClasses(getTable()
                .getBreakAfter(), childLC.getBreakAfter());
        if (breakAfter != Constants.EN_AUTO) {
            returnList.add(new BreakElement(getAuxiliaryPosition(), 0,
                    -KnuthElement.INFINITE, breakAfter, context));
        }

        setFinished(true);
        resetSpaces();
        return returnList;
    }

    /** {@inheritDoc} */
    @Override
    public Position getAuxiliaryPosition() {
        /*
         * Redefined to return a LeafPosition instead of a NonLeafPosition. The
         * SpaceResolver.SpaceHandlingBreakPosition constructors unwraps all
         * NonLeafPositions, which can lead to a NPE when a break in a table
         * occurs at a page with different ipd.
         */
        if (this.auxiliaryPosition == null) {
            this.auxiliaryPosition = new LeafPosition(this, 0);
        }
        return this.auxiliaryPosition;
    }

    /**
     * Registers the given area, that will be used to render the part of column
     * background covered by a table-cell. If percentages are used to place the
     * background image, the final bpd of the (fraction of) table that will be
     * rendered on the current page must be known. The traits can't then be set
     * when the areas for the cell are created since at that moment this bpd is
     * yet unknown. So they will instead be set in TableLM's
     * {@link #addAreas(PositionIterator, LayoutContext)} method.
     *
     * @param column
     *            the table-column element from which the cell gets background
     *            informations
     * @param backgroundArea
     *            the block of the cell's dimensions that will hold the column
     *            background
     * @param xShift
     *            additional amount by which the image must be shifted to be
     *            correctly placed (to counterbalance the cell's start border)
     */
    void registerColumnBackgroundArea(final TableColumn column,
            final Block backgroundArea, final int xShift) {
        addBackgroundArea(backgroundArea);
        if (this.columnBackgroundAreas == null) {
            this.columnBackgroundAreas = new ArrayList();
        }
        this.columnBackgroundAreas.add(new ColumnBackgroundInfo(column,
                backgroundArea, xShift));
    }

    /**
     * The table area is a reference area that contains areas for columns,
     * bodies, rows and the contents are in cells.
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

        // add space before, in order to implement display-align = "center" or
        // "after"
        if (layoutContext.getSpaceBefore() != 0) {
            addBlockSpacing(0.0,
                    MinOptMax.getInstance(layoutContext.getSpaceBefore()));
        }

        final int startXOffset = getTable().getCommonMarginBlock().startIndent
                .getValue(this);

        // add column, body then row areas

        // BPD of the table, i.e., height of its content; table's borders and
        // paddings not counted
        int tableHeight = 0;
        // Body childLM;
        final LayoutContext lc = new LayoutContext(0);

        lc.setRefIPD(getContentAreaIPD());
        this.contentLM.setStartXOffset(startXOffset);
        this.contentLM.addAreas(parentIter, lc);
        tableHeight += this.contentLM.getUsedBPD();

        this.curBlockArea.setBPD(tableHeight);

        if (this.columnBackgroundAreas != null) {
            for (final Iterator iter = this.columnBackgroundAreas.iterator(); iter
                    .hasNext();) {
                final ColumnBackgroundInfo b = (ColumnBackgroundInfo) iter
                        .next();
                TraitSetter.addBackground(b.backgroundArea, b.column
                        .getCommonBorderPaddingBackground(), this, b.xShift,
                        -b.backgroundArea.getYOffset(), b.column
                        .getColumnWidth().getValue(this), tableHeight);
            }
            this.columnBackgroundAreas.clear();
        }

        if (getTable().isSeparateBorderModel()) {
            TraitSetter.addBorders(this.curBlockArea, getTable()
                    .getCommonBorderPaddingBackground(),
                    this.discardBorderBefore, this.discardBorderAfter, false,
                    false, this);
            TraitSetter.addPadding(this.curBlockArea, getTable()
                    .getCommonBorderPaddingBackground(),
                    this.discardPaddingBefore, this.discardPaddingAfter, false,
                    false, this);
        }
        TraitSetter.addBackground(this.curBlockArea, getTable()
                .getCommonBorderPaddingBackground(), this);
        TraitSetter.addMargins(this.curBlockArea, getTable()
                .getCommonBorderPaddingBackground(), this.startIndent,
                this.endIndent, this);
        TraitSetter.addBreaks(this.curBlockArea, getTable().getBreakBefore(),
                getTable().getBreakAfter());
        TraitSetter.addSpaceBeforeAfter(this.curBlockArea,
                layoutContext.getSpaceAdjust(), this.effSpaceBefore,
                this.effSpaceAfter);

        flush();

        resetSpaces();
        this.curBlockArea = null;

        notifyEndOfLayout();
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

            TraitSetter.setProducerID(this.curBlockArea, getTable().getId());

            this.curBlockArea.setIPD(getContentAreaIPD());

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

    /**
     * Adds the given area to this layout manager's area, without updating the
     * used bpd.
     *
     * @param background
     *            an area
     */
    void addBackgroundArea(final Block background) {
        this.curBlockArea.addChildArea(background);
    }

    /** {@inheritDoc} */
    @Override
    public int negotiateBPDAdjustment(final int adj,
            final KnuthElement lastElement) {
        // TODO Auto-generated method stub
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void discardSpace(final KnuthGlue spaceGlue) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepTogetherProperty() {
        return getTable().getKeepTogether();
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepWithPreviousProperty() {
        return getTable().getKeepWithPrevious();
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepWithNextProperty() {
        return getTable().getKeepWithNext();
    }

    // --------- Property Resolution related functions --------- //

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseLength(final int lengthBase, final FObj fobj) {
        // Special handler for TableColumn width specifications
        if (fobj instanceof TableColumn && fobj.getParent() == getFObj()) {
            switch (lengthBase) {
            case LengthBase.CONTAINING_BLOCK_WIDTH:
                return getContentAreaIPD();
            case LengthBase.TABLE_UNITS:
                return (int) this.tableUnit;
            default:
                log.error("Unknown base type for LengthBase.");
                return 0;
            }
        } else {
            switch (lengthBase) {
            case LengthBase.TABLE_UNITS:
                return (int) this.tableUnit;
            default:
                return super.getBaseLength(lengthBase, fobj);
            }
        }
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
        this.curBlockArea = null;
        this.tableUnit = 0.0;
    }

}
