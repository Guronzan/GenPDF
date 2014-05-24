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

/* $Id: TablePart.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.fo.flow.table;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * An abstract base class modelling a TablePart (i.e. fo:table-header,
 * fo:table-footer and fo:table-body).
 */
public abstract class TablePart extends TableCellContainer {
    // The value of properties relevant for fo:table-body.
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    // Unused but valid items, commented out for performance:
    // private CommonAccessibility commonAccessibility;
    // private CommonAural commonAural;
    // private CommonRelativePosition commonRelativePosition;
    // private int visibility;
    // End of property values

    /** table rows found */
    protected boolean tableRowsFound = false;
    /** table cells found */
    protected boolean tableCellsFound = false;

    private boolean firstRow = true;

    private boolean rowsStarted = false;

    private boolean lastCellEndsRow = true;

    private List rowGroups = new LinkedList();

    /**
     * Create a TablePart instance with the given {@link FONode} as parent.
     * 
     * @param parent
     *            FONode that is the parent of the object
     */
    public TablePart(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    protected Object clone() {
        final TablePart clone = (TablePart) super.clone();
        clone.rowGroups = new LinkedList(this.rowGroups);
        return clone;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        super.bind(pList);
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList pList)
            throws FOPException {

        super.processNode(elementName, locator, attlist, pList);
        if (!inMarker()) {
            final Table t = getTable();
            if (t.hasExplicitColumns()) {
                final int size = t.getNumberOfColumns();
                this.pendingSpans = new ArrayList(size);
                for (int i = 0; i < size; i++) {
                    this.pendingSpans.add(null);
                }
            } else {
                this.pendingSpans = new ArrayList();
            }
            this.columnNumberManager = new ColumnNumberManager();
        }

    }

    /** {@inheritDoc} */
    @Override
    public void finalizeNode() throws FOPException {
        if (!inMarker()) {
            this.pendingSpans = null;
            this.columnNumberManager = null;
        }
        if (!(this.tableRowsFound || this.tableCellsFound)) {
            missingChildElementError("marker* (table-row+|table-cell+)", true);
            getParent().removeChild(this);
        } else {
            finishLastRowGroup();
        }

    }

    /** {@inheritDoc} */
    @Override
    TablePart getTablePart() {
        return this;
    }

    /**
     * Finish last row group.
     * 
     * @throws ValidationException
     *             if content validation exception
     */
    protected void finishLastRowGroup() throws ValidationException {
        if (!inMarker()) {
            final RowGroupBuilder rowGroupBuilder = getTable()
                    .getRowGroupBuilder();
            if (this.tableRowsFound) {
                rowGroupBuilder.endTableRow();
            } else if (!this.lastCellEndsRow) {
                rowGroupBuilder.endRow(this);
            }
            try {
                rowGroupBuilder.endTablePart();
            } catch (final ValidationException e) {
                e.setLocator(this.locator);
                throw e;
            }
        }
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: marker* (table-row+|table-cell+)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (localName.equals("marker")) {
                if (this.tableRowsFound || this.tableCellsFound) {
                    nodesOutOfOrderError(loc, "fo:marker",
                            "(table-row+|table-cell+)");
                }
            } else if (localName.equals("table-row")) {
                this.tableRowsFound = true;
                if (this.tableCellsFound) {
                    final TableEventProducer eventProducer = TableEventProducer.Provider
                            .get(getUserAgent().getEventBroadcaster());
                    eventProducer.noMixRowsAndCells(this, getName(),
                            getLocator());
                }
            } else if (localName.equals("table-cell")) {
                this.tableCellsFound = true;
                if (this.tableRowsFound) {
                    final TableEventProducer eventProducer = TableEventProducer.Provider
                            .get(getUserAgent().getEventBroadcaster());
                    eventProducer.noMixRowsAndCells(this, getName(),
                            getLocator());
                }
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void addChildNode(final FONode child) throws FOPException {
        if (!inMarker()) {
            switch (child.getNameId()) {
            case FO_TABLE_ROW:
                if (!this.rowsStarted) {
                    getTable().getRowGroupBuilder().startTablePart(this);
                } else {
                    this.columnNumberManager
                            .prepareForNextRow(this.pendingSpans);
                    getTable().getRowGroupBuilder().endTableRow();
                }
                this.rowsStarted = true;
                getTable().getRowGroupBuilder().startTableRow((TableRow) child);
                break;
            case FO_TABLE_CELL:
                if (!this.rowsStarted) {
                    getTable().getRowGroupBuilder().startTablePart(this);
                }
                this.rowsStarted = true;
                final TableCell cell = (TableCell) child;
                addTableCellChild(cell, this.firstRow);
                this.lastCellEndsRow = cell.endsRow();
                if (this.lastCellEndsRow) {
                    this.firstRow = false;
                    this.columnNumberManager
                            .prepareForNextRow(this.pendingSpans);
                    getTable().getRowGroupBuilder().endRow(this);
                }
                break;
            default:
                // nop
            }
        }
        // TODO: possible performance problems in case of large tables...
        // If the number of children grows significantly large, the default
        // implementation in FObj will get slower and slower...
        super.addChildNode(child);
    }

    void addRowGroup(final List rowGroup) {
        this.rowGroups.add(rowGroup);
    }

    /** @return list of row groups */
    public List getRowGroups() {
        return this.rowGroups;
    }

    /**
     * Get the {@link CommonBorderPaddingBackground} instance attached to this
     * TableBody.
     * 
     * @return the {@link CommonBorderPaddingBackground} instance.
     */
    @Override
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /**
     * @param obj
     *            table row in question
     * @return true if the given table row is the first row of this body.
     */
    public boolean isFirst(final TableRow obj) {
        return this.firstChild == null || this.firstChild == obj;
    }

    void signalNewRow() {
        if (this.rowsStarted) {
            this.firstRow = false;
            if (!this.lastCellEndsRow) {
                this.columnNumberManager.prepareForNextRow(this.pendingSpans);
                getTable().getRowGroupBuilder().endRow(this);
            }
        }
    }

}
