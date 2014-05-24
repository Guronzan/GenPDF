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

/* $Id: TableCellLayoutManager.java 1084205 2011-03-22 14:55:21Z vhennebert $ */

package org.apache.fop.layoutmgr.table;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.Trait;
import org.apache.fop.fo.flow.table.ConditionalBorder;
import org.apache.fop.fo.flow.table.GridUnit;
import org.apache.fop.fo.flow.table.PrimaryGridUnit;
import org.apache.fop.fo.flow.table.Table;
import org.apache.fop.fo.flow.table.TableCell;
import org.apache.fop.fo.flow.table.TableColumn;
import org.apache.fop.fo.flow.table.TablePart;
import org.apache.fop.fo.flow.table.TableRow;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground.BorderInfo;
import org.apache.fop.layoutmgr.AreaAdditionUtil;
import org.apache.fop.layoutmgr.BlockStackingLayoutManager;
import org.apache.fop.layoutmgr.ElementListUtils;
import org.apache.fop.layoutmgr.Keep;
import org.apache.fop.layoutmgr.KnuthBox;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthGlue;
import org.apache.fop.layoutmgr.KnuthPenalty;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.SpaceResolver;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.util.ListUtil;

/**
 * LayoutManager for a table-cell FO. A cell contains blocks. These blocks fill
 * the cell.
 */
public class TableCellLayoutManager extends BlockStackingLayoutManager {

    /**
     * logging instance
     */
    private static Log log = LogFactory.getLog(TableCellLayoutManager.class);

    private final PrimaryGridUnit primaryGridUnit;

    private Block curBlockArea;

    private int xoffset;
    private int yoffset;
    private int cellIPD;
    private int totalHeight;
    private int usedBPD;
    private final boolean emptyCell = true;

    /**
     * Create a new Cell layout manager.
     * 
     * @param node
     *            table-cell FO for which to create the LM
     * @param pgu
     *            primary grid unit for the cell
     */
    public TableCellLayoutManager(final TableCell node,
            final PrimaryGridUnit pgu) {
        super(node);
        this.primaryGridUnit = pgu;
    }

    /** @return the table-cell FO */
    public TableCell getTableCell() {
        return (TableCell) this.fobj;
    }

    private boolean isSeparateBorderModel() {
        return getTable().isSeparateBorderModel();
    }

    /**
     * @return the table owning this cell
     */
    public Table getTable() {
        return getTableCell().getTable();
    }

    /** {@inheritDoc} */
    @Override
    protected int getIPIndents() {
        final int[] startEndBorderWidths = this.primaryGridUnit
                .getStartEndBorderWidths();
        this.startIndent = startEndBorderWidths[0];
        this.endIndent = startEndBorderWidths[1];
        if (isSeparateBorderModel()) {
            final int borderSep = getTable().getBorderSeparation()
                    .getLengthPair().getIPD().getLength().getValue(this);
            this.startIndent += borderSep / 2;
            this.endIndent += borderSep / 2;
        } else {
            this.startIndent /= 2;
            this.endIndent /= 2;
        }
        this.startIndent += getTableCell().getCommonBorderPaddingBackground()
                .getPaddingStart(false, this);
        this.endIndent += getTableCell().getCommonBorderPaddingBackground()
                .getPaddingEnd(false, this);
        return this.startIndent + this.endIndent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        final MinOptMax stackLimit = context.getStackLimitBP();

        this.referenceIPD = context.getRefIPD();
        this.cellIPD = this.referenceIPD;
        this.cellIPD -= getIPIndents();

        List returnedList;
        final List contentList = new LinkedList();
        final List returnList = new LinkedList();

        LayoutManager curLM; // currently active LM
        LayoutManager prevLM = null; // previously active LM
        while ((curLM = getChildLM()) != null) {
            final LayoutContext childLC = new LayoutContext(0);
            // curLM is a ?
            childLC.setStackLimitBP(context.getStackLimitBP().minus(stackLimit));
            childLC.setRefIPD(this.cellIPD);

            // get elements from curLM
            returnedList = curLM.getNextKnuthElements(childLC, alignment);
            if (childLC.isKeepWithNextPending()) {
                log.debug("child LM signals pending keep with next");
            }
            if (contentList.isEmpty() && childLC.isKeepWithPreviousPending()) {
                this.primaryGridUnit.setKeepWithPrevious(childLC
                        .getKeepWithPreviousPending());
                childLC.clearKeepWithPreviousPending();
            }

            if (prevLM != null
                    && !ElementListUtils.endsWithForcedBreak(contentList)) {
                // there is a block handled by prevLM
                // before the one handled by curLM
                addInBetweenBreak(contentList, context, childLC);
            }
            contentList.addAll(returnedList);
            if (returnedList.isEmpty()) {
                // Avoid NoSuchElementException below (happens with empty
                // blocks)
                continue;
            }
            if (childLC.isKeepWithNextPending()) {
                // Clear and propagate
                context.updateKeepWithNextPending(childLC
                        .getKeepWithNextPending());
                childLC.clearKeepWithNextPending();
            }
            prevLM = curLM;
        }
        this.primaryGridUnit.setKeepWithNext(context.getKeepWithNextPending());

        returnedList = new LinkedList();
        if (!contentList.isEmpty()) {
            wrapPositionElements(contentList, returnList);
        } else {
            // In relaxed validation mode, table-cells having no children are
            // authorised.
            // Add a zero-width block here to not have to take this special case
            // into
            // account later
            // Copied from BlockStackingLM
            returnList
                    .add(new KnuthBox(0, notifyPos(new Position(this)), true));
        }
        // Space resolution
        SpaceResolver.resolveElementList(returnList);
        if (((KnuthElement) returnList.get(0)).isForcedBreak()) {
            this.primaryGridUnit.setBreakBefore(((KnuthPenalty) returnList
                    .get(0)).getBreakClass());
            returnList.remove(0);
            assert !returnList.isEmpty();
        }
        final KnuthElement lastItem = (KnuthElement) ListUtil
                .getLast(returnList);
        if (lastItem.isForcedBreak()) {
            final KnuthPenalty p = (KnuthPenalty) lastItem;
            this.primaryGridUnit.setBreakAfter(p.getBreakClass());
            p.setPenalty(0);
        }

        setFinished(true);
        return returnList;
    }

    /**
     * Set the y offset of this cell. This offset is used to set the absolute
     * position of the cell.
     *
     * @param off
     *            the y direction offset
     */
    public void setYOffset(final int off) {
        this.yoffset = off;
    }

    /**
     * Set the x offset of this cell (usually the same as its parent row). This
     * offset is used to determine the absolute position of the cell.
     *
     * @param off
     *            the x offset
     */
    public void setXOffset(final int off) {
        this.xoffset = off;
    }

    /**
     * Set the content height for this cell. This method is used during
     * addAreas() stage.
     *
     * @param h
     *            the height of the contents of this cell
     */
    public void setContentHeight(final int h) {
        this.usedBPD = h;
    }

    /**
     * Sets the total height of this cell on the current page. That is, the
     * cell's bpd plus before and after borders and paddings, plus the table's
     * border-separation.
     *
     * @param h
     *            the height of cell
     */
    public void setTotalHeight(final int h) {
        this.totalHeight = h;
    }

    /**
     * Add the areas for the break points. The cell contains block stacking
     * layout managers that add block areas.
     *
     * <p>
     * In the collapsing-border model, the borders of a cell that spans over
     * several rows or columns are drawn separately for each grid unit.
     * Therefore we must know the height of each grid row spanned over by the
     * cell. Also, if the cell is broken over two pages we must know which
     * spanned grid rows are present on the current page.
     * </p>
     *
     * @param parentIter
     *            the iterator of the break positions
     * @param layoutContext
     *            the layout context for adding the areas
     * @param spannedGridRowHeights
     *            in collapsing-border model for a spanning cell, height of each
     *            spanned grid row
     * @param startRow
     *            first grid row on the current page spanned over by the cell,
     *            inclusive
     * @param endRow
     *            last grid row on the current page spanned over by the cell,
     *            inclusive
     * @param borderBeforeWhich
     *            one of {@link ConditionalBorder#NORMAL},
     *            {@link ConditionalBorder#LEADING_TRAILING} or
     *            {@link ConditionalBorder#REST}
     * @param borderAfterWhich
     *            one of {@link ConditionalBorder#NORMAL},
     *            {@link ConditionalBorder#LEADING_TRAILING} or
     *            {@link ConditionalBorder#REST}
     * @param firstOnPage
     *            true if the cell will be the very first one on the page, in
     *            which case collapsed before borders must be drawn in the outer
     *            mode
     * @param lastOnPage
     *            true if the cell will be the very last one on the page, in
     *            which case collapsed after borders must be drawn in the outer
     *            mode
     * @param painter
     *            painter
     * @param firstRowHeight
     *            height of the first row spanned by this cell (may be zero if
     *            this row is placed on a previous page). Used to calculate the
     *            placement of the row's background image if any
     */
    public void addAreas(
            // CSOK: ParameterNumber
            final PositionIterator parentIter,
            final LayoutContext layoutContext,
            final int[] spannedGridRowHeights, final int startRow,
            final int endRow, final int borderBeforeWhich,
            final int borderAfterWhich, final boolean firstOnPage,
            final boolean lastOnPage, final RowPainter painter,
            final int firstRowHeight) {
        getParentArea(null);

        addId();

        final int borderBeforeWidth = this.primaryGridUnit
                .getBeforeBorderWidth(startRow, borderBeforeWhich);
        final int borderAfterWidth = this.primaryGridUnit.getAfterBorderWidth(
                endRow, borderAfterWhich);

        final CommonBorderPaddingBackground padding = this.primaryGridUnit
                .getCell().getCommonBorderPaddingBackground();
        final int paddingRectBPD = this.totalHeight - borderBeforeWidth
                - borderAfterWidth;
        int cellBPD = paddingRectBPD;
        cellBPD -= padding.getPaddingBefore(
                borderBeforeWhich == ConditionalBorder.REST, this);
        cellBPD -= padding.getPaddingAfter(
                borderAfterWhich == ConditionalBorder.REST, this);

        addBackgroundAreas(painter, firstRowHeight, borderBeforeWidth,
                paddingRectBPD);

        if (isSeparateBorderModel()) {
            if (!this.emptyCell || getTableCell().showEmptyCells()) {
                if (borderBeforeWidth > 0) {
                    final int halfBorderSepBPD = getTableCell().getTable()
                            .getBorderSeparation().getBPD().getLength()
                            .getValue() / 2;
                    adjustYOffset(this.curBlockArea, halfBorderSepBPD);
                }
                TraitSetter.addBorders(this.curBlockArea, getTableCell()
                        .getCommonBorderPaddingBackground(),
                        borderBeforeWidth == 0, borderAfterWidth == 0, false,
                        false, this);
            }
        } else {
            final boolean inFirstColumn = this.primaryGridUnit.getColIndex() == 0;
            final boolean inLastColumn = this.primaryGridUnit.getColIndex()
                    + getTableCell().getNumberColumnsSpanned() == getTable()
                    .getNumberOfColumns();
            if (!this.primaryGridUnit.hasSpanning()) {
                adjustYOffset(this.curBlockArea, -borderBeforeWidth);
                // Can set the borders directly if there's no span
                final boolean[] outer = new boolean[] { firstOnPage,
                        lastOnPage, inFirstColumn, inLastColumn };
                TraitSetter
                        .addCollapsingBorders(this.curBlockArea,
                                this.primaryGridUnit
                                        .getBorderBefore(borderBeforeWhich),
                                this.primaryGridUnit
                                        .getBorderAfter(borderAfterWhich),
                                this.primaryGridUnit.getBorderStart(),
                                this.primaryGridUnit.getBorderEnd(), outer);
            } else {
                adjustYOffset(this.curBlockArea, borderBeforeWidth);
                final Block[][] blocks = new Block[getTableCell()
                        .getNumberRowsSpanned()][getTableCell()
                                                                                          .getNumberColumnsSpanned()];
                GridUnit[] gridUnits = (GridUnit[]) this.primaryGridUnit
                        .getRows().get(startRow);
                for (int x = 0; x < getTableCell().getNumberColumnsSpanned(); x++) {
                    final GridUnit gu = gridUnits[x];
                    final BorderInfo border = gu
                            .getBorderBefore(borderBeforeWhich);
                    final int borderWidth = border.getRetainedWidth() / 2;
                    if (borderWidth > 0) {
                        addBorder(blocks, startRow, x, Trait.BORDER_BEFORE,
                                border, firstOnPage);
                        adjustYOffset(blocks[startRow][x], -borderWidth);
                        adjustBPD(blocks[startRow][x], -borderWidth);
                    }
                }
                gridUnits = (GridUnit[]) this.primaryGridUnit.getRows().get(
                        endRow);
                for (int x = 0; x < getTableCell().getNumberColumnsSpanned(); x++) {
                    final GridUnit gu = gridUnits[x];
                    final BorderInfo border = gu
                            .getBorderAfter(borderAfterWhich);
                    final int borderWidth = border.getRetainedWidth() / 2;
                    if (borderWidth > 0) {
                        addBorder(blocks, endRow, x, Trait.BORDER_AFTER,
                                border, lastOnPage);
                        adjustBPD(blocks[endRow][x], -borderWidth);
                    }
                }
                for (int y = startRow; y <= endRow; y++) {
                    gridUnits = (GridUnit[]) this.primaryGridUnit.getRows()
                            .get(y);
                    BorderInfo border = gridUnits[0].getBorderStart();
                    int borderWidth = border.getRetainedWidth() / 2;
                    if (borderWidth > 0) {
                        addBorder(blocks, y, 0, Trait.BORDER_START, border,
                                inFirstColumn);
                        adjustXOffset(blocks[y][0], borderWidth);
                        adjustIPD(blocks[y][0], -borderWidth);
                    }
                    border = gridUnits[gridUnits.length - 1].getBorderEnd();
                    borderWidth = border.getRetainedWidth() / 2;
                    if (borderWidth > 0) {
                        addBorder(blocks, y, gridUnits.length - 1,
                                Trait.BORDER_END, border, inLastColumn);
                        adjustIPD(blocks[y][gridUnits.length - 1], -borderWidth);
                    }
                }
                int dy = this.yoffset;
                for (int y = startRow; y <= endRow; y++) {
                    final int bpd = spannedGridRowHeights[y - startRow];
                    int dx = this.xoffset;
                    for (int x = 0; x < gridUnits.length; x++) {
                        final int ipd = getTable()
                                .getColumn(
                                        this.primaryGridUnit.getColIndex() + x)
                                .getColumnWidth().getValue(getParent());
                        if (blocks[y][x] != null) {
                            final Block block = blocks[y][x];
                            adjustYOffset(block, dy);
                            adjustXOffset(block, dx);
                            adjustIPD(block, ipd);
                            adjustBPD(block, bpd);
                            this.parentLayoutManager.addChildArea(block);
                        }
                        dx += ipd;
                    }
                    dy += bpd;
                }
            }
        }

        TraitSetter.addPadding(this.curBlockArea, padding,
                borderBeforeWhich == ConditionalBorder.REST,
                borderAfterWhich == ConditionalBorder.REST, false, false, this);

        // Handle display-align
        if (this.usedBPD < cellBPD) {
            if (getTableCell().getDisplayAlign() == EN_CENTER) {
                final Block space = new Block();
                space.setBPD((cellBPD - this.usedBPD) / 2);
                this.curBlockArea.addBlock(space);
            } else if (getTableCell().getDisplayAlign() == EN_AFTER) {
                final Block space = new Block();
                space.setBPD(cellBPD - this.usedBPD);
                this.curBlockArea.addBlock(space);
            }
        }

        AreaAdditionUtil.addAreas(this, parentIter, layoutContext);
        // Re-adjust the cell's bpd as it may have been modified by the previous
        // call
        // for some reason (?)
        this.curBlockArea.setBPD(cellBPD);

        // Add background after we know the BPD
        if (!isSeparateBorderModel() || !this.emptyCell
                || getTableCell().showEmptyCells()) {
            TraitSetter.addBackground(this.curBlockArea, getTableCell()
                    .getCommonBorderPaddingBackground(), this);
        }

        flush();

        this.curBlockArea = null;

        notifyEndOfLayout();
    }

    /** Adds background areas for the column, body and row, if any. */
    private void addBackgroundAreas(final RowPainter painter,
            final int firstRowHeight, final int borderBeforeWidth,
            final int paddingRectBPD) {
        final TableColumn column = getTable().getColumn(
                this.primaryGridUnit.getColIndex());
        if (column.getCommonBorderPaddingBackground().hasBackground()) {
            final Block colBackgroundArea = getBackgroundArea(paddingRectBPD,
                    borderBeforeWidth);
            ((TableLayoutManager) this.parentLayoutManager)
                    .registerColumnBackgroundArea(column, colBackgroundArea,
                            -this.startIndent);
        }

        final TablePart body = this.primaryGridUnit.getTablePart();
        if (body.getCommonBorderPaddingBackground().hasBackground()) {
            painter.registerPartBackgroundArea(getBackgroundArea(
                    paddingRectBPD, borderBeforeWidth));
        }

        final TableRow row = this.primaryGridUnit.getRow();
        if (row != null
                && row.getCommonBorderPaddingBackground().hasBackground()) {
            final Block rowBackgroundArea = getBackgroundArea(paddingRectBPD,
                    borderBeforeWidth);
            ((TableLayoutManager) this.parentLayoutManager)
                    .addBackgroundArea(rowBackgroundArea);
            TraitSetter.addBackground(rowBackgroundArea,
                    row.getCommonBorderPaddingBackground(),
                    this.parentLayoutManager, -this.xoffset - this.startIndent,
                    -borderBeforeWidth,
                    this.parentLayoutManager.getContentAreaIPD(),
                    firstRowHeight);
        }
    }

    private void addBorder(final Block[][] blocks, final int i, final int j,
            final Integer side, final BorderInfo border, final boolean outer) {
        if (blocks[i][j] == null) {
            blocks[i][j] = new Block();
            blocks[i][j].addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
            blocks[i][j].setPositioning(Block.ABSOLUTE);
        }
        blocks[i][j].addTrait(side,
                new BorderProps(border.getStyle(), border.getRetainedWidth(),
                        border.getColor(), outer ? BorderProps.COLLAPSE_OUTER
                                : BorderProps.COLLAPSE_INNER));
    }

    private static void adjustXOffset(final Block block, final int amount) {
        block.setXOffset(block.getXOffset() + amount);
    }

    private static void adjustYOffset(final Block block, final int amount) {
        block.setYOffset(block.getYOffset() + amount);
    }

    private static void adjustIPD(final Block block, final int amount) {
        block.setIPD(block.getIPD() + amount);
    }

    private static void adjustBPD(final Block block, final int amount) {
        block.setBPD(block.getBPD() + amount);
    }

    private Block getBackgroundArea(final int bpd, final int borderBeforeWidth) {
        final CommonBorderPaddingBackground padding = getTableCell()
                .getCommonBorderPaddingBackground();
        final int paddingStart = padding.getPaddingStart(false, this);
        final int paddingEnd = padding.getPaddingEnd(false, this);

        final Block block = new Block();
        TraitSetter.setProducerID(block, getTable().getId());
        block.setPositioning(Block.ABSOLUTE);
        block.setIPD(this.cellIPD + paddingStart + paddingEnd);
        block.setBPD(bpd);
        block.setXOffset(this.xoffset + this.startIndent - paddingStart);
        block.setYOffset(this.yoffset + borderBeforeWidth);
        return block;
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
     *            the child area to get the parent for
     * @return the parent area
     */
    @Override
    public Area getParentArea(final Area childArea) {
        if (this.curBlockArea == null) {
            this.curBlockArea = new Block();
            this.curBlockArea.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
            TraitSetter
                    .setProducerID(this.curBlockArea, getTableCell().getId());
            this.curBlockArea.setPositioning(Block.ABSOLUTE);
            this.curBlockArea.setXOffset(this.xoffset + this.startIndent);
            this.curBlockArea.setYOffset(this.yoffset);
            this.curBlockArea.setIPD(this.cellIPD);

            /* Area parentArea = */this.parentLayoutManager
                    .getParentArea(this.curBlockArea);
            // Get reference IPD from parentArea
            setCurrentArea(this.curBlockArea); // ??? for generic operations
        }
        return this.curBlockArea;
    }

    /**
     * Add the child to the cell block area.
     *
     * @param childArea
     *            the child to add to the cell
     */
    @Override
    public void addChildArea(final Area childArea) {
        if (this.curBlockArea != null) {
            this.curBlockArea.addBlock((Block) childArea);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int negotiateBPDAdjustment(final int adj,
            final KnuthElement lastElement) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void discardSpace(final KnuthGlue spaceGlue) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepTogether() {
        // keep-together does not apply to fo:table-cell
        return Keep.KEEP_AUTO;
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithNext() {
        return Keep.KEEP_AUTO; // TODO FIX ME (table-cell has no
                               // keep-with-next!)
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithPrevious() {
        return Keep.KEEP_AUTO; // TODO FIX ME (table-cell has no
                               // keep-with-previous!)
    }

    // --------- Property Resolution related functions --------- //

    /**
     * Returns the IPD of the content area
     * 
     * @return the IPD of the content area
     */
    @Override
    public int getContentAreaIPD() {
        return this.cellIPD;
    }

    /**
     * Returns the BPD of the content area
     * 
     * @return the BPD of the content area
     */
    @Override
    public int getContentAreaBPD() {
        if (this.curBlockArea != null) {
            return this.curBlockArea.getBPD();
        } else {
            log.error("getContentAreaBPD called on unknown BPD");
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getGeneratesReferenceArea() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getGeneratesBlockArea() {
        return true;
    }

}
