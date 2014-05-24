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

/* $Id: TableColumn.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.flow.table;

// XML
import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.fo.properties.TableColLength;
import org.apache.fop.layoutmgr.table.CollapsingBorderModel;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_table-column">
 * <code>fo:table-column</code></a> object.
 */
public class TableColumn extends TableFObj {
    // The value of properties relevant for fo:table-column.
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private int columnNumber;
    private Length columnWidth;
    private int numberColumnsRepeated;
    private int numberColumnsSpanned;
    // Unused but valid items, commented out for performance:
    // private int visibility;
    // End of property values

    private final boolean implicitColumn;
    private PropertyList pList = null;

    /**
     * Create a TableColumn instance with the given {@link FONode} as parent.
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public TableColumn(final FONode parent) {
        this(parent, false);
    }

    /**
     * Create a TableColumn instance with the given {@link FONode} as parent
     *
     * @param parent
     *            FONode that is the parent of this object
     * @param implicit
     *            true if this table-column has automatically been created (does
     *            not correspond to an explicit fo:table-column in the input
     *            document)
     */
    public TableColumn(final FONode parent, final boolean implicit) {
        super(parent);
        this.implicitColumn = implicit;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        this.columnNumber = pList.get(PR_COLUMN_NUMBER).getNumeric().getValue();
        this.columnWidth = pList.get(PR_COLUMN_WIDTH).getLength();
        this.numberColumnsRepeated = pList.get(PR_NUMBER_COLUMNS_REPEATED)
                .getNumeric().getValue();
        this.numberColumnsSpanned = pList.get(PR_NUMBER_COLUMNS_SPANNED)
                .getNumeric().getValue();
        super.bind(pList);

        if (this.numberColumnsRepeated <= 0) {
            final TableEventProducer eventProducer = TableEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.valueMustBeBiggerGtEqOne(this,
                    "number-columns-repeated", this.numberColumnsRepeated,
                    getLocator());
        }
        if (this.numberColumnsSpanned <= 0) {
            final TableEventProducer eventProducer = TableEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.valueMustBeBiggerGtEqOne(this,
                    "number-columns-spanned", this.numberColumnsSpanned,
                    getLocator());
        }

        /*
         * check for unspecified width and replace with default of
         * proportional-column-width(1), in case of fixed table-layout warn only
         * for explicit columns
         */
        if (this.columnWidth.getEnum() == EN_AUTO) {
            if (!this.implicitColumn && !getTable().isAutoLayout()) {
                final TableEventProducer eventProducer = TableEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.warnImplicitColumns(this, getLocator());
            }
            this.columnWidth = new TableColLength(1.0, this);
        }

        /*
         * in case of explicit columns, from-table-column() can be used on
         * descendants of the table-cells, so we need a reference to the
         * column's property list (cleared in Table.endOfNode())
         */
        if (!this.implicitColumn) {
            this.pList = pList;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startColumn(this);
    }

    void setCollapsedBorders(final CollapsingBorderModel collapsingBorderModel) {
        this.collapsingBorderModel = collapsingBorderModel;
        setCollapsedBorders();
    }

    /** {@inheritDoc} */
    @Override
    public void endOfNode() throws FOPException {
        getFOEventHandler().endColumn(this);
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: empty
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /**
     * Get the {@link CommonBorderPaddingBackground} instance attached to this
     * TableColumn.
     * 
     * @return the {@link CommonBorderPaddingBackground} instance
     */
    @Override
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /**
     * Get a {@link Length} instance corresponding to the
     * <code>column-width</code> property.
     * 
     * @return the "column-width" property.
     */
    public Length getColumnWidth() {
        return this.columnWidth;
    }

    /**
     * Sets the column width.
     * 
     * @param columnWidth
     *            the column width
     */
    public void setColumnWidth(final Length columnWidth) {
        this.columnWidth = columnWidth;
    }

    /**
     * Get the value of the <code>column-number</code> property
     * 
     * @return the "column-number" property.
     */
    public int getColumnNumber() {
        return this.columnNumber;
    }

    /**
     * Used for setting the column-number for an implicit column
     * 
     * @param columnNumber
     *            the number to set
     */
    protected void setColumnNumber(final int columnNumber) {
        this.columnNumber = columnNumber;
    }

    /** @return value for number-columns-repeated. */
    public int getNumberColumnsRepeated() {
        return this.numberColumnsRepeated;
    }

    /** @return value for number-columns-spanned. */
    public int getNumberColumnsSpanned() {
        return this.numberColumnsSpanned;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "table-column";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_TABLE_COLUMN}
     */
    @Override
    public int getNameId() {
        return FO_TABLE_COLUMN;
    }

    /**
     * Indicates whether this table-column has been created as default column
     * for this table in case no table-columns have been defined. Note that this
     * only used to provide better user feedback (see ColumnSetup).
     * 
     * @return true if this table-column has been created as default column
     */
    public boolean isImplicitColumn() {
        return this.implicitColumn;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("fo:table-column");
        sb.append(" column-number=").append(getColumnNumber());
        if (getNumberColumnsRepeated() > 1) {
            sb.append(" number-columns-repeated=").append(
                    getNumberColumnsRepeated());
        }
        if (getNumberColumnsSpanned() > 1) {
            sb.append(" number-columns-spanned=").append(
                    getNumberColumnsSpanned());
        }
        sb.append(" column-width=").append(
                ((Property) getColumnWidth()).getString());
        return sb.toString();
    }

    /**
     * Retrieve a property value through its Id; used by from-table-column()
     * function
     *
     * @param propId
     *            the id for the property to retrieve
     * @return the requested Property
     * @throws PropertyException
     *             if there is a problem evaluating the property
     */
    public Property getProperty(final int propId) throws PropertyException {
        return this.pList.get(propId);
    }

    /**
     * Clear the reference to the PropertyList (retained for
     * from-table-column())
     */
    protected void releasePropertyList() {
        this.pList = null;
    }

}
