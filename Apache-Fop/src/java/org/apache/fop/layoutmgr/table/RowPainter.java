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

/* $Id: RowPainter.java 1328515 2012-04-20 21:26:43Z gadams $ */

package org.apache.fop.layoutmgr.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Block;
import org.apache.fop.area.Trait;
import org.apache.fop.fo.flow.table.ConditionalBorder;
import org.apache.fop.fo.flow.table.EffRow;
import org.apache.fop.fo.flow.table.EmptyGridUnit;
import org.apache.fop.fo.flow.table.GridUnit;
import org.apache.fop.fo.flow.table.PrimaryGridUnit;
import org.apache.fop.fo.flow.table.Table;
import org.apache.fop.fo.flow.table.TableColumn;
import org.apache.fop.fo.flow.table.TablePart;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground.BorderInfo;
import org.apache.fop.layoutmgr.ElementListUtils;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthPossPosIter;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.SpaceResolver;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.traits.BorderProps;

@Slf4j
class RowPainter {
    private final int colCount;
    private int currentRowOffset = 0;
    /** Currently handled row (= last encountered row). */
    private EffRow currentRow = null;
    private final LayoutContext layoutContext;
    /**
     * Index of the first row of the current part present on the current page.
     */
    private int firstRowIndex;

    /**
     * Index of the very first row on the current page. Needed to properly
     * handle {@link BorderProps#COLLAPSE_OUTER}. This is not the same as
     * {@link #firstRowIndex} when the table has headers!
     */
    private int firstRowOnPageIndex;

    /**
     * Keeps track of the y-offsets of each row on a page. This is particularly
     * needed for spanned cells where you need to know the y-offset of the
     * starting row when the area is generated at the time the cell is closed.
     */
    private final List rowOffsets = new ArrayList();

    private final int[] cellHeights;
    private final boolean[] firstCellOnPage;
    private final CellPart[] firstCellParts;
    private final CellPart[] lastCellParts;

    /** y-offset of the current table part. */
    private int tablePartOffset = 0;
    /** See {@link RowPainter#registerPartBackgroundArea(Block)}. */
    private CommonBorderPaddingBackground tablePartBackground;
    /** See {@link RowPainter#registerPartBackgroundArea(Block)}. */
    private List tablePartBackgroundAreas;

    private final TableContentLayoutManager tclm;

    RowPainter(final TableContentLayoutManager tclm,
            final LayoutContext layoutContext) {
        this.tclm = tclm;
        this.layoutContext = layoutContext;
        this.colCount = tclm.getColumns().getColumnCount();
        this.cellHeights = new int[this.colCount];
        this.firstCellOnPage = new boolean[this.colCount];
        this.firstCellParts = new CellPart[this.colCount];
        this.lastCellParts = new CellPart[this.colCount];
        this.firstRowIndex = -1;
        this.firstRowOnPageIndex = -1;
    }

    void startTablePart(final TablePart tablePart) {
        final CommonBorderPaddingBackground background = tablePart
                .getCommonBorderPaddingBackground();
        if (background.hasBackground()) {
            this.tablePartBackground = background;
            if (this.tablePartBackgroundAreas == null) {
                this.tablePartBackgroundAreas = new ArrayList();
            }
        }
        this.tablePartOffset = this.currentRowOffset;
    }

    /**
     * Signals that the end of the current table part is reached.
     *
     * @param lastInBody
     *            true if the part is the last table-body element to be
     *            displayed on the current page. In which case all the cells
     *            must be flushed even if they aren't finished, plus the proper
     *            collapsed borders must be selected (trailing instead of
     *            normal, or rest if the cell is unfinished)
     * @param lastOnPage
     *            true if the part is the last to be displayed on the current
     *            page. In which case collapsed after borders for the cells on
     *            the last row must be drawn in the outer mode
     */
    void endTablePart(final boolean lastInBody, final boolean lastOnPage) {
        addAreasAndFlushRow(lastInBody, lastOnPage);

        if (this.tablePartBackground != null) {
            final TableLayoutManager tableLM = this.tclm.getTableLM();
            for (final Iterator iter = this.tablePartBackgroundAreas.iterator(); iter
                    .hasNext();) {
                final Block backgroundArea = (Block) iter.next();
                TraitSetter.addBackground(backgroundArea,
                        this.tablePartBackground, tableLM,
                        -backgroundArea.getXOffset(), this.tablePartOffset
                        - backgroundArea.getYOffset(),
                        tableLM.getContentAreaIPD(), this.currentRowOffset
                        - this.tablePartOffset);
            }
            this.tablePartBackground = null;
            this.tablePartBackgroundAreas.clear();
        }
    }

    int getAccumulatedBPD() {
        return this.currentRowOffset;
    }

    /**
     * Records the fragment of row represented by the given position. If it
     * belongs to another (grid) row than the current one, that latter is
     * painted and flushed first.
     *
     * @param tcpos
     *            a position representing the row fragment
     */
    void handleTableContentPosition(final TableContentPosition tcpos) {
        if (log.isDebugEnabled()) {
            log.debug("===handleTableContentPosition(" + tcpos);
        }
        if (this.currentRow == null) {
            this.currentRow = tcpos.getNewPageRow();
        } else {
            final EffRow row = tcpos.getRow();
            if (row.getIndex() > this.currentRow.getIndex()) {
                addAreasAndFlushRow(false, false);
                this.currentRow = row;
            }
        }
        if (this.firstRowIndex < 0) {
            this.firstRowIndex = this.currentRow.getIndex();
            if (this.firstRowOnPageIndex < 0) {
                this.firstRowOnPageIndex = this.firstRowIndex;
            }
        }
        final Iterator partIter = tcpos.cellParts.iterator();
        // Iterate over all grid units in the current step
        while (partIter.hasNext()) {
            final CellPart cellPart = (CellPart) partIter.next();
            if (log.isDebugEnabled()) {
                log.debug(">" + cellPart);
            }
            final int colIndex = cellPart.pgu.getColIndex();
            if (this.firstCellParts[colIndex] == null) {
                this.firstCellParts[colIndex] = cellPart;
                this.cellHeights[colIndex] = cellPart
                        .getBorderPaddingBefore(this.firstCellOnPage[colIndex]);
            } else {
                assert this.firstCellParts[colIndex].pgu == cellPart.pgu;
                this.cellHeights[colIndex] += cellPart
                        .getConditionalBeforeContentLength();
            }
            this.cellHeights[colIndex] += cellPart.getLength();
            this.lastCellParts[colIndex] = cellPart;
        }
    }

    /**
     * Creates the areas corresponding to the last row. That is, an area with
     * background for the row, plus areas for all the cells that finish on the
     * row (not spanning over further rows).
     *
     * @param lastInPart
     *            true if the row is the last from its table part to be
     *            displayed on the current page. In which case all the cells
     *            must be flushed even if they aren't finished, plus the proper
     *            collapsed borders must be selected (trailing instead of
     *            normal, or rest if the cell is unfinished)
     * @param lastOnPage
     *            true if the row is the very last row of the table that will be
     *            displayed on the current page. In which case collapsed after
     *            borders must be drawn in the outer mode
     */
    private void addAreasAndFlushRow(final boolean lastInPart,
            final boolean lastOnPage) {
        if (log.isDebugEnabled()) {
            log.debug("Remembering yoffset for row "
                    + this.currentRow.getIndex() + ": " + this.currentRowOffset);
        }
        recordRowOffset(this.currentRow.getIndex(), this.currentRowOffset);

        // Need to compute the actual row height first
        // and determine border behaviour for empty cells
        boolean firstCellPart = true;
        boolean lastCellPart = true;
        int actualRowHeight = 0;
        for (int i = 0; i < this.colCount; i++) {
            final GridUnit currentGU = this.currentRow.getGridUnit(i);
            if (currentGU.isEmpty()) {
                continue;
            }
            if (currentGU.getColSpanIndex() == 0
                    && (lastInPart || currentGU.isLastGridUnitRowSpan())
                    && this.firstCellParts[i] != null) {
                // TODO
                // The last test above is a workaround for the stepping
                // algorithm's
                // fundamental flaw making it unable to produce the right
                // element list for
                // multiple breaks inside a same row group.
                // (see
                // http://wiki.apache.org/xmlgraphics-fop/TableLayout/KnownProblems)
                // In some extremely rare cases (forced breaks, very small page
                // height), a
                // TableContentPosition produced during row delaying may end up
                // alone on a
                // page. It will not contain the CellPart instances for the
                // cells starting
                // the next row, so firstCellParts[i] will still be null for
                // those ones.
                int cellHeight = this.cellHeights[i];
                cellHeight += this.lastCellParts[i]
                        .getConditionalAfterContentLength();
                cellHeight += this.lastCellParts[i]
                        .getBorderPaddingAfter(lastInPart);
                final int cellOffset = getRowOffset(Math.max(
                        this.firstCellParts[i].pgu.getRowIndex(),
                        this.firstRowIndex));
                actualRowHeight = Math.max(actualRowHeight, cellOffset
                        + cellHeight - this.currentRowOffset);
            }

            if (this.firstCellParts[i] != null
                    && !this.firstCellParts[i].isFirstPart()) {
                firstCellPart = false;
            }
            if (this.lastCellParts[i] != null
                    && !this.lastCellParts[i].isLastPart()) {
                lastCellPart = false;
            }
        }

        // Then add areas for cells finishing on the current row
        for (int i = 0; i < this.colCount; i++) {
            final GridUnit currentGU = this.currentRow.getGridUnit(i);
            if (currentGU.isEmpty() && !this.tclm.isSeparateBorderModel()) {
                int borderBeforeWhich;
                if (firstCellPart) {
                    if (this.firstCellOnPage[i]) {
                        borderBeforeWhich = ConditionalBorder.LEADING_TRAILING;
                    } else {
                        borderBeforeWhich = ConditionalBorder.NORMAL;
                    }
                } else {
                    borderBeforeWhich = ConditionalBorder.REST;
                }
                int borderAfterWhich;
                if (lastCellPart) {
                    if (lastInPart) {
                        borderAfterWhich = ConditionalBorder.LEADING_TRAILING;
                    } else {
                        borderAfterWhich = ConditionalBorder.NORMAL;
                    }
                } else {
                    borderAfterWhich = ConditionalBorder.REST;
                }
                addAreaForEmptyGridUnit((EmptyGridUnit) currentGU,
                        this.currentRow.getIndex(), i, actualRowHeight,
                        borderBeforeWhich, borderAfterWhich, lastOnPage);

                this.firstCellOnPage[i] = false;
            } else if (currentGU.getColSpanIndex() == 0
                    && (lastInPart || currentGU.isLastGridUnitRowSpan())
                    && this.firstCellParts[i] != null) {
                assert this.firstCellParts[i].pgu == currentGU.getPrimary();

                int borderBeforeWhich;
                if (this.firstCellParts[i].isFirstPart()) {
                    if (this.firstCellOnPage[i]) {
                        borderBeforeWhich = ConditionalBorder.LEADING_TRAILING;
                    } else {
                        borderBeforeWhich = ConditionalBorder.NORMAL;
                    }
                } else {
                    assert this.firstCellOnPage[i];
                    borderBeforeWhich = ConditionalBorder.REST;
                }
                int borderAfterWhich;
                if (this.lastCellParts[i].isLastPart()) {
                    if (lastInPart) {
                        borderAfterWhich = ConditionalBorder.LEADING_TRAILING;
                    } else {
                        borderAfterWhich = ConditionalBorder.NORMAL;
                    }
                } else {
                    borderAfterWhich = ConditionalBorder.REST;
                }

                addAreasForCell(this.firstCellParts[i].pgu,
                        this.firstCellParts[i].start,
                        this.lastCellParts[i].end, actualRowHeight,
                        borderBeforeWhich, borderAfterWhich, lastOnPage);
                this.firstCellParts[i] = null;
                Arrays.fill(this.firstCellOnPage, i, i
                        + currentGU.getCell().getNumberColumnsSpanned(), false);
            }
        }
        this.currentRowOffset += actualRowHeight;
        if (lastInPart) {
            /*
             * Either the end of the page is reached, then this was the last
             * call of this method and we no longer care about currentRow; or
             * the end of a table-part (header, footer, body) has been reached,
             * and the next row will anyway be different from the current one,
             * and this is unnecessary to call this method again in the first
             * lines of handleTableContentPosition, so we may reset the
             * following variables.
             */
            this.currentRow = null;
            this.firstRowIndex = -1;
            this.rowOffsets.clear();
            /*
             * The current table part has just been handled. Be it the first one
             * or not, the header or the body, in any case the borders-before of
             * the next row (i.e., the first row of the next part if any) must
             * be painted in COLLAPSE_INNER mode. So the firstRowOnPageIndex
             * indicator must be kept disabled. The following way is not the
             * most elegant one but will be good enough.
             */
            this.firstRowOnPageIndex = Integer.MAX_VALUE;
        }
    }

    // TODO this is not very efficient and should probably be done another way
    // this method is only necessary when display-align = center or after, in
    // which case
    // the exact content length is needed to compute the size of the empty block
    // that will
    // be used as padding.
    // This should be handled automatically by a proper use of Knuth elements
    private int computeContentLength(final PrimaryGridUnit pgu,
            final int startIndex, final int endIndex) {
        if (startIndex > endIndex) {
            // May happen if the cell contributes no content on the current page
            // (empty
            // cell, in most cases)
            return 0;
        } else {
            final ListIterator iter = pgu.getElements()
                    .listIterator(startIndex);
            // Skip from the content length calculation glues and penalties
            // occurring at the
            // beginning of the page
            boolean nextIsBox = false;
            while (iter.nextIndex() <= endIndex && !nextIsBox) {
                nextIsBox = ((KnuthElement) iter.next()).isBox();
            }
            int len = 0;
            if (((KnuthElement) iter.previous()).isBox()) {
                while (iter.nextIndex() < endIndex) {
                    final KnuthElement el = (KnuthElement) iter.next();
                    if (el.isBox() || el.isGlue()) {
                        len += el.getWidth();
                    }
                }
                len += ActiveCell.getElementContentLength((KnuthElement) iter
                        .next());
            }
            return len;
        }
    }

    private void addAreasForCell(final PrimaryGridUnit pgu, final int startPos,
            final int endPos, final int rowHeight, final int borderBeforeWhich,
            final int borderAfterWhich, final boolean lastOnPage) {
        /*
         * Determine the index of the first row of this cell that will be
         * displayed on the current page.
         */
        final int currentRowIndex = this.currentRow.getIndex();
        int startRowIndex;
        int firstRowHeight;
        if (pgu.getRowIndex() >= this.firstRowIndex) {
            startRowIndex = pgu.getRowIndex();
            if (startRowIndex < currentRowIndex) {
                firstRowHeight = getRowOffset(startRowIndex + 1)
                        - getRowOffset(startRowIndex);
            } else {
                firstRowHeight = rowHeight;
            }
        } else {
            startRowIndex = this.firstRowIndex;
            firstRowHeight = 0;
        }

        /*
         * In collapsing-border model, if the cell spans over several
         * columns/rows then dedicated areas will be created for each grid unit
         * to hold the corresponding borders. For that we need to know the
         * height of each grid unit, that is of each grid row spanned over by
         * the cell
         */
        int[] spannedGridRowHeights = null;
        if (!this.tclm.getTableLM().getTable().isSeparateBorderModel()
                && pgu.hasSpanning()) {
            spannedGridRowHeights = new int[currentRowIndex - startRowIndex + 1];
            int prevOffset = getRowOffset(startRowIndex);
            for (int i = 0; i < currentRowIndex - startRowIndex; i++) {
                final int newOffset = getRowOffset(startRowIndex + i + 1);
                spannedGridRowHeights[i] = newOffset - prevOffset;
                prevOffset = newOffset;
            }
            spannedGridRowHeights[currentRowIndex - startRowIndex] = rowHeight;
        }
        final int cellOffset = getRowOffset(startRowIndex);
        final int cellTotalHeight = rowHeight + this.currentRowOffset
                - cellOffset;
        if (log.isDebugEnabled()) {
            log.debug("Creating area for cell:");
            log.debug("  start row: " + pgu.getRowIndex() + " "
                    + this.currentRowOffset + " " + cellOffset);
            log.debug(" rowHeight=" + rowHeight + " cellTotalHeight="
                    + cellTotalHeight);
        }
        final TableCellLayoutManager cellLM = pgu.getCellLM();
        cellLM.setXOffset(this.tclm.getXOffsetOfGridUnit(pgu));
        cellLM.setYOffset(cellOffset);
        cellLM.setContentHeight(computeContentLength(pgu, startPos, endPos));
        cellLM.setTotalHeight(cellTotalHeight);
        final int prevBreak = ElementListUtils.determinePreviousBreak(
                pgu.getElements(), startPos);
        if (endPos >= 0) {
            SpaceResolver.performConditionalsNotification(pgu.getElements(),
                    startPos, endPos, prevBreak);
        }
        cellLM.addAreas(new KnuthPossPosIter(pgu.getElements(), startPos,
                endPos + 1), this.layoutContext, spannedGridRowHeights,
                startRowIndex - pgu.getRowIndex(),
                currentRowIndex - pgu.getRowIndex(), borderBeforeWhich,
                borderAfterWhich, startRowIndex == this.firstRowOnPageIndex,
                lastOnPage, this, firstRowHeight);
    }

    private void addAreaForEmptyGridUnit(final EmptyGridUnit gu,
            final int rowIndex, final int colIndex, final int actualRowHeight,
            final int borderBeforeWhich, final int borderAfterWhich,
            final boolean lastOnPage) {

        // get effective borders
        final BorderInfo borderBefore = gu.getBorderBefore(borderBeforeWhich);
        final BorderInfo borderAfter = gu.getBorderAfter(borderAfterWhich);
        final BorderInfo borderStart = gu.getBorderStart();
        final BorderInfo borderEnd = gu.getBorderEnd();
        if (borderBefore.getRetainedWidth() == 0
                && borderAfter.getRetainedWidth() == 0
                && borderStart.getRetainedWidth() == 0
                && borderEnd.getRetainedWidth() == 0) {
            return; // no borders, no area necessary
        }

        final TableLayoutManager tableLM = this.tclm.getTableLM();
        final Table table = tableLM.getTable();
        final TableColumn col = this.tclm.getColumns().getColumn(colIndex + 1);

        // position information
        final boolean firstOnPage = rowIndex == this.firstRowOnPageIndex;
        final boolean inFirstColumn = colIndex == 0;
        final boolean inLastColumn = colIndex == table.getNumberOfColumns() - 1;

        // determine the block area's size
        int ipd = col.getColumnWidth().getValue(tableLM);
        ipd -= (borderStart.getRetainedWidth() + borderEnd.getRetainedWidth()) / 2;
        int bpd = actualRowHeight;
        bpd -= (borderBefore.getRetainedWidth() + borderAfter
                .getRetainedWidth()) / 2;

        // generate the block area
        final Block block = new Block();
        block.setPositioning(Block.ABSOLUTE);
        block.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
        block.setIPD(ipd);
        block.setBPD(bpd);
        block.setXOffset(this.tclm.getXOffsetOfGridUnit(colIndex, 1)
                + borderStart.getRetainedWidth() / 2);
        block.setYOffset(getRowOffset(rowIndex)
                - borderBefore.getRetainedWidth() / 2);
        final boolean[] outer = new boolean[] { firstOnPage, lastOnPage,
                inFirstColumn, inLastColumn };
        TraitSetter.addCollapsingBorders(block, borderBefore, borderAfter,
                borderStart, borderEnd, outer);
        tableLM.addChildArea(block);
    }

    /**
     * Registers the given area, that will be used to render the part of
     * table-header/footer/body background covered by a table-cell. If
     * percentages are used to place the background image, the final bpd of the
     * (fraction of) table part that will be rendered on the current page must
     * be known. The traits can't then be set when the areas for the cell are
     * created since at that moment this bpd is yet unknown. So they will
     * instead be set in {@link #addAreasAndFlushRow(boolean, boolean)}.
     *
     * @param backgroundArea
     *            the block of the cell's dimensions that will hold the part
     *            background
     */
    void registerPartBackgroundArea(final Block backgroundArea) {
        this.tclm.getTableLM().addBackgroundArea(backgroundArea);
        this.tablePartBackgroundAreas.add(backgroundArea);
    }

    /**
     * Records the y-offset of the row with the given index.
     *
     * @param rowIndex
     *            index of the row
     * @param offset
     *            y-offset of the row on the page
     */
    private void recordRowOffset(final int rowIndex, final int offset) {
        /*
         * In some very rare cases a row may be skipped. See for example
         * Bugzilla #43633: in a two-column table, a row contains a row-spanning
         * cell and a missing cell. In TableStepper#goToNextRowIfCurrentFinished
         * this row will immediately be considered as finished, since it
         * contains no cell ending on this row. Thus no TableContentPosition
         * will be created for this row. Thus its index will never be recorded
         * by the #handleTableContentPosition method.
         *
         * The offset of such a row is the same as the next non-empty row. It's
         * needed to correctly offset blocks for cells starting on this row.
         * Hence the loop below.
         */
        for (int i = this.rowOffsets.size(); i <= rowIndex - this.firstRowIndex; i++) {
            this.rowOffsets.add(new Integer(offset));
        }
    }

    /**
     * Returns the offset of the row with the given index.
     *
     * @param rowIndex
     *            index of the row
     * @return its y-offset on the page
     */
    private int getRowOffset(final int rowIndex) {
        return ((Integer) this.rowOffsets.get(rowIndex - this.firstRowIndex))
                .intValue();
    }

    // TODO get rid of that
    /** Signals that the first table-body instance has started. */
    void startBody() {
        Arrays.fill(this.firstCellOnPage, true);
    }

    // TODO get rid of that
    /** Signals that the last table-body instance has ended. */
    void endBody() {
        Arrays.fill(this.firstCellOnPage, false);
    }
}
