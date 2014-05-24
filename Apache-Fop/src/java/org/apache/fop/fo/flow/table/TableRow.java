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

/* $Id: TableRow.java 985227 2010-08-13 15:03:17Z spepping $ */

package org.apache.fop.fo.flow.table;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.BreakPropertySet;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_table-row">
 * <code>fo:table-row</code></a> object.
 */
public class TableRow extends TableCellContainer implements BreakPropertySet {
    // The value of properties relevant for fo:table-row.
    private LengthRangeProperty blockProgressionDimension;
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private int breakAfter;
    private int breakBefore;
    private Length height;
    private KeepProperty keepTogether;
    private KeepProperty keepWithNext;
    private KeepProperty keepWithPrevious;

    // Unused but valid items, commented out for performance:
    // private CommonAccessibility commonAccessibility;
    // private CommonAural commonAural;
    // private CommonRelativePosition commonRelativePosition;
    // private int visibility;
    // End of property values

    /**
     * Create a TableRow instance with the given {@link FONode} as parent.
     * 
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public TableRow(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.blockProgressionDimension = pList.get(
                PR_BLOCK_PROGRESSION_DIMENSION).getLengthRange();
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        this.breakAfter = pList.get(PR_BREAK_AFTER).getEnum();
        this.breakBefore = pList.get(PR_BREAK_BEFORE).getEnum();
        this.height = pList.get(PR_HEIGHT).getLength();
        this.keepTogether = pList.get(PR_KEEP_TOGETHER).getKeep();
        this.keepWithNext = pList.get(PR_KEEP_WITH_NEXT).getKeep();
        this.keepWithPrevious = pList.get(PR_KEEP_WITH_PREVIOUS).getKeep();
        super.bind(pList);
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList pList)
            throws FOPException {
        super.processNode(elementName, locator, attlist, pList);
        if (!inMarker()) {
            final TablePart part = (TablePart) this.parent;
            this.pendingSpans = part.pendingSpans;
            this.columnNumberManager = part.columnNumberManager;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void addChildNode(final FONode child) throws FOPException {
        if (!inMarker()) {
            final TableCell cell = (TableCell) child;
            final TablePart part = (TablePart) getParent();
            addTableCellChild(cell, part.isFirst(this));
        }
        super.addChildNode(child);
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startRow(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        super.endOfNode();
        getFOEventHandler().endRow(this);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeNode() throws FOPException {
        if (this.firstChild == null) {
            missingChildElementError("(table-cell+)");
        }
        if (!inMarker()) {
            this.pendingSpans = null;
            this.columnNumberManager = null;
        }
    }

    /**
     * {@inheritDoc} String, String) <br>
     * XSL Content Model: (table-cell+)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if ("marker".equals(localName)) {
                if (this.firstChild != null) {
                    // a table-cell has already been added to this row
                    nodesOutOfOrderError(loc, "fo:marker", "(table-cell+)");
                }
            } else if (!"table-cell".equals(localName)) {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    TablePart getTablePart() {
        return (TablePart) this.parent;
    }

    boolean isTableRow() {
        return true;
    }

    /** @return the "break-after" property. */
    @Override
    public int getBreakAfter() {
        return this.breakAfter;
    }

    /** @return the "break-before" property. */
    @Override
    public int getBreakBefore() {
        return this.breakBefore;
    }

    /** @return the "keep-with-previous" property. */
    public KeepProperty getKeepWithPrevious() {
        return this.keepWithPrevious;
    }

    /** @return the "keep-with-next" property. */
    public KeepProperty getKeepWithNext() {
        return this.keepWithNext;
    }

    /** @return the "keep-together" property. */
    public KeepProperty getKeepTogether() {
        return this.keepTogether;
    }

    /**
     * Convenience method to check if a keep-together constraint is specified.
     * 
     * @return true if keep-together is active.
     */
    public boolean mustKeepTogether() {
        return !getKeepTogether().getWithinPage().isAuto()
                || !getKeepTogether().getWithinColumn().isAuto();
    }

    /**
     * Convenience method to check if a keep-with-next constraint is specified.
     * 
     * @return true if keep-with-next is active.
     */
    public boolean mustKeepWithNext() {
        return !getKeepWithNext().getWithinPage().isAuto()
                || !getKeepWithNext().getWithinColumn().isAuto();
    }

    /**
     * Convenience method to check if a keep-with-previous constraint is
     * specified.
     * 
     * @return true if keep-with-previous is active.
     */
    public boolean mustKeepWithPrevious() {
        return !getKeepWithPrevious().getWithinPage().isAuto()
                || !getKeepWithPrevious().getWithinColumn().isAuto();
    }

    /**
     * @return the "block-progression-dimension" property.
     */
    public LengthRangeProperty getBlockProgressionDimension() {
        return this.blockProgressionDimension;
    }

    /**
     * @return the "height" property.
     */
    public Length getHeight() {
        return this.height;
    }

    /**
     * @return the Common Border, Padding, and Background Properties.
     */
    @Override
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "table-row";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_TABLE_ROW}
     */
    @Override
    public int getNameId() {
        return FO_TABLE_ROW;
    }
}
