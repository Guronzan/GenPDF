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

/* $Id: FixedColRowGroupBuilder.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.flow.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.ValidationException;

/**
 * A row group builder optimised for a fixed number of columns, known before the
 * parsing of cells starts (that is, if the fo:table has explicit
 * fo:table-column children).
 */
class FixedColRowGroupBuilder extends RowGroupBuilder {

    /** Number of columns in the corresponding table. */
    private final int numberOfColumns;

    private TableRow currentTableRow = null;

    /** 0-based, index in the row group. */
    private int currentRowIndex;

    /** The rows belonging to this row group. List of List of {@link GridUnit}s. */
    private List/* <List<GridUnit>> */rows;

    private boolean firstInPart = true;

    /**
     * The last encountered row. This is the last row of the table if it has no
     * footer.
     */
    private List lastRow;

    private BorderResolver borderResolver;

    FixedColRowGroupBuilder(final Table t) {
        super(t);
        this.numberOfColumns = t.getNumberOfColumns();
        if (t.isSeparateBorderModel()) {
            this.borderResolver = new SeparateBorderResolver();
        } else {
            this.borderResolver = new CollapsingBorderResolver(t);
        }
        initialize();
    }

    /**
     * Prepares this builder for creating a new row group.
     */
    private void initialize() {
        this.rows = new ArrayList();
        this.currentRowIndex = 0;
    }

    /** {@inheritDoc} */
    @Override
    void addTableCell(final TableCell cell) {
        for (int i = this.rows.size(); i < this.currentRowIndex
                + cell.getNumberRowsSpanned(); i++) {
            final List effRow = new ArrayList(this.numberOfColumns);
            for (int j = 0; j < this.numberOfColumns; j++) {
                effRow.add(null);
            }
            this.rows.add(effRow);
        }
        final int columnIndex = cell.getColumnNumber() - 1;
        final PrimaryGridUnit pgu = new PrimaryGridUnit(cell, columnIndex);
        List row = (List) this.rows.get(this.currentRowIndex);
        row.set(columnIndex, pgu);
        // TODO
        GridUnit[] cellRow = new GridUnit[cell.getNumberColumnsSpanned()];
        cellRow[0] = pgu;
        for (int j = 1; j < cell.getNumberColumnsSpanned(); j++) {
            final GridUnit gu = new GridUnit(pgu, j, 0);
            row.set(columnIndex + j, gu);
            cellRow[j] = gu;
        }
        pgu.addRow(cellRow);
        for (int i = 1; i < cell.getNumberRowsSpanned(); i++) {
            row = (List) this.rows.get(this.currentRowIndex + i);
            cellRow = new GridUnit[cell.getNumberColumnsSpanned()];
            for (int j = 0; j < cell.getNumberColumnsSpanned(); j++) {
                final GridUnit gu = new GridUnit(pgu, j, i);
                row.set(columnIndex + j, gu);
                cellRow[j] = gu;
            }
            pgu.addRow(cellRow);
        }
    }

    private static void setFlagForCols(final int flag, final List row) {
        for (final ListIterator iter = row.listIterator(); iter.hasNext();) {
            ((GridUnit) iter.next()).setFlag(flag);
        }
    }

    /** {@inheritDoc} */
    @Override
    void startTableRow(final TableRow tableRow) {
        this.currentTableRow = tableRow;
    }

    /** {@inheritDoc} */
    @Override
    void endTableRow() {
        assert this.currentTableRow != null;
        if (this.currentRowIndex > 0
                && this.currentTableRow.getBreakBefore() != Constants.EN_AUTO) {
            final TableEventProducer eventProducer = TableEventProducer.Provider
                    .get(this.currentTableRow.getUserAgent()
                            .getEventBroadcaster());
            eventProducer.breakIgnoredDueToRowSpanning(this,
                    this.currentTableRow.getName(), true,
                    this.currentTableRow.getLocator());
        }
        if (this.currentRowIndex < this.rows.size() - 1
                && this.currentTableRow.getBreakAfter() != Constants.EN_AUTO) {
            final TableEventProducer eventProducer = TableEventProducer.Provider
                    .get(this.currentTableRow.getUserAgent()
                            .getEventBroadcaster());
            eventProducer.breakIgnoredDueToRowSpanning(this,
                    this.currentTableRow.getName(), false,
                    this.currentTableRow.getLocator());
        }
        for (final Iterator iter = ((List) this.rows.get(this.currentRowIndex))
                .iterator(); iter.hasNext();) {
            final GridUnit gu = (GridUnit) iter.next();
            // The row hasn't been filled with empty grid units yet
            if (gu != null) {
                gu.setRow(this.currentTableRow);
            }
        }
        handleRowEnd(this.currentTableRow);
    }

    /** {@inheritDoc} */
    @Override
    void endRow(final TablePart part) {
        handleRowEnd(part);
    }

    private void handleRowEnd(final TableCellContainer container) {
        final List currentRow = (List) this.rows.get(this.currentRowIndex);
        this.lastRow = currentRow;
        // Fill gaps with empty grid units
        for (int i = 0; i < this.numberOfColumns; i++) {
            if (currentRow.get(i) == null) {
                currentRow.set(i, new EmptyGridUnit(this.table,
                        this.currentTableRow, i));
            }
        }
        this.borderResolver.endRow(currentRow, container);
        if (this.firstInPart) {
            setFlagForCols(GridUnit.FIRST_IN_PART, currentRow);
            this.firstInPart = false;
        }
        if (this.currentRowIndex == this.rows.size() - 1) {
            // Means that the current row has no cell spanning over following
            // rows
            container.getTablePart().addRowGroup(this.rows);
            initialize();
        } else {
            this.currentRowIndex++;
        }
        this.currentTableRow = null;
    }

    /** {@inheritDoc} */
    @Override
    void startTablePart(final TablePart part) {
        this.firstInPart = true;
        this.borderResolver.startPart(part);
    }

    /** {@inheritDoc} */
    @Override
    void endTablePart() throws ValidationException {
        if (this.rows.size() > 0) {
            throw new ValidationException(
                    "A table-cell is spanning more rows than available in its parent element.");
        }
        setFlagForCols(GridUnit.LAST_IN_PART, this.lastRow);
        this.borderResolver.endPart();
    }

    /** {@inheritDoc} */
    @Override
    void endTable() {
        this.borderResolver.endTable();
    }
}
