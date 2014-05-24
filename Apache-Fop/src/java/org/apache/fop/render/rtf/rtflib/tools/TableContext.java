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

/* $Id: TableContext.java 1297284 2012-03-05 23:29:29Z gadams $ */

package org.apache.fop.render.rtf.rtflib.tools;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.render.rtf.rtflib.rtfdoc.ITableColumnsInfo;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfAttributes;

/**
 * <p>
 * Used when handling fo:table to hold information to build the table.
 * </p>
 *
 * <p>
 * This work was authored by Bertrand Delacretaz (bdelacretaz@codeconsult.ch),
 * Ed Trembicki-Guy (guye@dnb.com), Boris Poudérous
 * (boris.pouderous@eads-telecom.com), and Peter Herweg (pherweg@web.de).
 * </p>
 *
 * This class was originally developed for the JFOR project and is now
 * integrated into FOP.
 */
@Slf4j
public class TableContext implements ITableColumnsInfo {
    private final BuilderContext context;
    private final List colWidths = new java.util.ArrayList();
    private int colIndex;

    /**
     * This ArrayList contains one element for each column in the table. value
     * == 0 means there is no row-spanning value > 0 means there is row-spanning
     * Each value in the list is decreased by 1 after each finished table-row
     */
    private final List colRowSpanningNumber = new java.util.ArrayList();

    /**
     * If there has a vertical merged cell to be created, its attributes are
     * inherited from the corresponding MERGE_START-cell. For this purpose the
     * attributes of a cell are stored in this array, as soon as a
     * number-rows-spanned attribute has been found.
     */
    private final List colRowSpanningAttrs = new java.util.ArrayList();

    /**
     * This ArrayList contains one element for each column in the table. value
     * == true means, it's the first of multiple spanned columns value == false
     * meanst, it's NOT the first of multiple spanned columns
     */
    private final List colFirstSpanningCol = new java.util.ArrayList();

    private boolean bNextRowBelongsToHeader = false;

    /**
     *
     * @param value
     *            Specifies, if next row belongs to header
     */
    public void setNextRowBelongsToHeader(final boolean value) {
        this.bNextRowBelongsToHeader = value;
    }

    /**
     *
     * @return true, if next row belongs to header
     */
    public boolean getNextRowBelongsToHeader() {
        return this.bNextRowBelongsToHeader;
    }

    /**
     *
     * @param ctx
     *            BuilderContext
     */
    public TableContext(final BuilderContext ctx) {
        this.context = ctx;
    }

    /**
     * Adds a column and sets its width.
     *
     * @param width
     *            Width of next column
     */
    public void setNextColumnWidth(final Float width) {
        this.colWidths.add(width);
    }

    /**
     *
     * @return RtfAttributes of current row-spanning cell
     */
    public RtfAttributes getColumnRowSpanningAttrs() {
        return (RtfAttributes) this.colRowSpanningAttrs.get(this.colIndex);
    }

    /**
     *
     * @return Number of currently spanned rows
     */
    public Integer getColumnRowSpanningNumber() {
        return (Integer) this.colRowSpanningNumber.get(this.colIndex);
    }

    /**
     *
     * @return true, if it's the first of multiple spanning columns
     */
    @Override
    public boolean getFirstSpanningCol() {
        final Boolean b = (Boolean) this.colFirstSpanningCol.get(this.colIndex);
        return b.booleanValue();
    }

    /**
     *
     * @param iRowSpanning
     *            number of rows to span
     * @param attrs
     *            RtfAttributes of row-spanning cell
     */
    public void setCurrentColumnRowSpanning(final Integer iRowSpanning,
            final RtfAttributes attrs) {

        if (this.colIndex < this.colRowSpanningNumber.size()) {
            this.colRowSpanningNumber.set(this.colIndex, iRowSpanning);
            this.colRowSpanningAttrs.set(this.colIndex, attrs);
        } else {
            this.colRowSpanningNumber.add(iRowSpanning);
            this.colRowSpanningAttrs.add(this.colIndex, attrs);
        }
    }

    /**
     *
     * @param iRowSpanning
     *            number of rows to span in next column
     * @param attrs
     *            RtfAttributes of row-spanning cell
     */
    public void setNextColumnRowSpanning(final Integer iRowSpanning,
            final RtfAttributes attrs) {
        this.colRowSpanningNumber.add(iRowSpanning);
        this.colRowSpanningAttrs.add(this.colIndex, attrs);
    }

    /**
     *
     * @param bFirstSpanningCol
     *            specifies, if it's the first of multiple spanned columns
     */
    public void setCurrentFirstSpanningCol(final boolean bFirstSpanningCol) {

        if (this.colIndex < this.colRowSpanningNumber.size()) {
            while (this.colIndex >= this.colFirstSpanningCol.size()) {
                setNextFirstSpanningCol(false);
            }
            this.colFirstSpanningCol.set(this.colIndex, new Boolean(
                    bFirstSpanningCol));
        } else {
            this.colFirstSpanningCol.add(new Boolean(bFirstSpanningCol));
        }
    }

    /**
     *
     * @param bFirstSpanningCol
     *            specifies, if it's the first of multiple spanned columns
     */
    public void setNextFirstSpanningCol(final boolean bFirstSpanningCol) {
        this.colFirstSpanningCol.add(new Boolean(bFirstSpanningCol));
    }

    /**
     * Added by Peter Herweg on 2002-06-29 This function is called after each
     * finished table-row. It decreases all values in colRowSpanningNumber by 1.
     * If a value reaches 0 row-spanning is finished, and the value won't be
     * decreased anymore.
     */
    public void decreaseRowSpannings() {
        for (int z = 0; z < this.colRowSpanningNumber.size(); ++z) {
            Integer i = (Integer) this.colRowSpanningNumber.get(z);

            if (i.intValue() > 0) {
                i = new Integer(i.intValue() - 1);
            }

            this.colRowSpanningNumber.set(z, i);

            if (i.intValue() == 0) {
                this.colRowSpanningAttrs.set(z, null);
                this.colFirstSpanningCol.set(z, Boolean.FALSE);
            }
        }
    }

    /**
     * Reset the column iteration index, meant to be called when creating a new
     * row The 'public' modifier has been added by Boris Poudérous for
     * 'number-columns-spanned' processing
     */
    @Override
    public void selectFirstColumn() {
        this.colIndex = 0;
    }

    /**
     * Increment the column iteration index The 'public' modifier has been added
     * by Boris Poudérous for 'number-columns-spanned' processing
     */
    @Override
    public void selectNextColumn() {
        this.colIndex++;
    }

    /**
     * Get current column width according to column iteration index
     *
     * @return INVALID_COLUMN_WIDTH if we cannot find the value The 'public'
     *         modifier has been added by Boris Poudérous for
     *         'number-columns-spanned' processing
     */
    @Override
    public float getColumnWidth() {
        if (this.colIndex < 0) {
            throw new IllegalStateException("colIndex must not be negative!");
        } else if (this.colIndex >= getNumberOfColumns()) {
            log.warn("Column width for column " + (this.colIndex + 1)
                    + " is not defined, using " + INVALID_COLUMN_WIDTH);
            while (this.colIndex >= getNumberOfColumns()) {
                setNextColumnWidth(new Float(INVALID_COLUMN_WIDTH));
            }
        }
        return ((Float) this.colWidths.get(this.colIndex)).floatValue();
    }

    /**
     * Set current column index.
     *
     * @param index
     *            New column index
     */
    public void setColumnIndex(final int index) {
        this.colIndex = index;
    }

    /**
     * @return Index of current column
     */
    @Override
    public int getColumnIndex() {
        return this.colIndex;
    }

    /** - end - */

    /**
     * @return Number of columns
     */
    @Override
    public int getNumberOfColumns() {
        return this.colWidths.size();
    }
    /** - end - */
}
