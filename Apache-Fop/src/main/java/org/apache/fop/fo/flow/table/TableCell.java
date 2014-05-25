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

/* $Id: TableCell.java 1242848 2012-02-10 16:51:08Z phancock $ */

package org.apache.fop.fo.flow.table;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_table-cell">
 * <code>fo:table-cell</code></a> object.
 */
public class TableCell extends TableFObj implements CommonAccessibilityHolder {
    // The value of properties relevant for fo:table-cell.
    private CommonAccessibility commonAccessibility;
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private LengthRangeProperty blockProgressionDimension;
    private int columnNumber;
    private int displayAlign;
    private int emptyCells;
    private int endsRow;
    private int numberColumnsSpanned;
    private int numberRowsSpanned;
    private int startsRow;
    private Length width;
    // Unused but valid items, commented out for performance:
    // private CommonAural commonAural;
    // private CommonRelativePosition commonRelativePosition;
    // private int relativeAlign;
    // private Length height;
    // private LengthRangeProperty inlineProgressionDimension;
    // private KeepProperty keepTogether;
    // private KeepProperty keepWithNext;
    // private KeepProperty keepWithPrevious;
    // End of property values

    /** used for FO validation */
    private boolean blockItemFound = false;

    /**
     * Create a TableCell instance with the given {@link FONode} as parent.
     * 
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public TableCell(final FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.commonAccessibility = CommonAccessibility.getInstance(pList);
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        this.blockProgressionDimension = pList.get(
                PR_BLOCK_PROGRESSION_DIMENSION).getLengthRange();
        this.displayAlign = pList.get(PR_DISPLAY_ALIGN).getEnum();
        this.emptyCells = pList.get(PR_EMPTY_CELLS).getEnum();
        this.startsRow = pList.get(PR_STARTS_ROW).getEnum();
        // For properly computing columnNumber
        if (startsRow() && getParent().getNameId() != FO_TABLE_ROW) {
            ((TablePart) getParent()).signalNewRow();
        }
        this.endsRow = pList.get(PR_ENDS_ROW).getEnum();
        this.columnNumber = pList.get(PR_COLUMN_NUMBER).getNumeric().getValue();
        this.numberColumnsSpanned = pList.get(PR_NUMBER_COLUMNS_SPANNED)
                .getNumeric().getValue();
        this.numberRowsSpanned = pList.get(PR_NUMBER_ROWS_SPANNED).getNumeric()
                .getValue();
        this.width = pList.get(PR_WIDTH).getLength();
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startCell(this);
    }

    /**
     * Make sure content model satisfied, if so then tell the FOEventHandler
     * that we are at the end of the table-cell. {@inheritDoc}
     */
    @Override
    protected void endOfNode() throws FOPException {
        super.endOfNode();
        getFOEventHandler().endCell(this);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeNode() throws FOPException {

        if (!this.blockItemFound) {
            missingChildElementError("marker* (%block;)+", true);
        }
        if ((startsRow() || endsRow())
                && getParent().getNameId() == FO_TABLE_ROW) {
            final TableEventProducer eventProducer = TableEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.startEndRowUnderTableRowWarning(this, getLocator());
        }

    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: marker* (%block;)+
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (localName.equals("marker")) {
                if (this.blockItemFound) {
                    nodesOutOfOrderError(loc, "fo:marker", "(%block;)");
                }
            } else if (!isBlockItem(nsURI, localName)) {
                invalidChildError(loc, nsURI, localName);
            } else {
                this.blockItemFound = true;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean generatesReferenceAreas() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public CommonAccessibility getCommonAccessibility() {
        return this.commonAccessibility;
    }

    /**
     * Get the {@link CommonBorderPaddingBackground} instance attached to this
     * TableCell.
     * 
     * @return the {@link CommonBorderPaddingBackground} instance
     */
    @Override
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /**
     * Get the value for the <code>column-number</code> property.
     * 
     * @return the "column-number" property.
     */
    public int getColumnNumber() {
        return this.columnNumber;
    }

    /**
     * Get the value for the <code>empty-cells</code> property.
     * 
     * @return true if "empty-cells" is "show"
     */
    public boolean showEmptyCells() {
        return this.emptyCells == EN_SHOW;
    }

    /**
     * Get the value for the <code>number-columns-spanned</code> property
     * 
     * @return the "number-columns-spanned" property.
     */
    public int getNumberColumnsSpanned() {
        return Math.max(this.numberColumnsSpanned, 1);
    }

    /**
     * Get the value for the <code>number-rows-spanned</code> property
     * 
     * @return the "number-rows-spanned" property.
     */
    public int getNumberRowsSpanned() {
        return Math.max(this.numberRowsSpanned, 1);
    }

    /**
     * Get the value for the <code>block-progression-dimension</code> property
     * 
     * @return the "block-progression-dimension" property.
     */
    public LengthRangeProperty getBlockProgressionDimension() {
        return this.blockProgressionDimension;
    }

    /**
     * Get the value for the <code>display-align</code> property
     * 
     * @return the display-align property.
     */
    public int getDisplayAlign() {
        return this.displayAlign;
    }

    /**
     * Get the value for the <code>width</code> property
     * 
     * @return the "width" property.
     */
    public Length getWidth() {
        return this.width;
    }

    /**
     * Get the value for the <code>starts-row</code> property
     * 
     * @return true if the cell starts a row.
     */
    public boolean startsRow() {
        return this.startsRow == EN_TRUE;
    }

    /**
     * Get the value for the <code>ends-row</code> property
     * 
     * @return true if the cell ends a row.
     */
    public boolean endsRow() {
        return this.endsRow == EN_TRUE;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "table-cell";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_TABLE_CELL}
     */
    @Override
    public final int getNameId() {
        return FO_TABLE_CELL;
    }

}
