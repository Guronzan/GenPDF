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

/* $Id: Table.java 1327291 2012-04-17 21:34:52Z gadams $ */

package org.apache.fop.fo.flow.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.fop.apps.FOPException;
import org.apache.fop.complexscripts.bidi.DelimitedTextRange;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.ValidationPercentBaseContext;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.StaticPropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.BreakPropertySet;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.LengthPairProperty;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.apache.fop.fo.properties.TableColLength;
import org.apache.fop.traits.Direction;
import org.apache.fop.traits.WritingMode;
import org.apache.fop.traits.WritingModeTraits;
import org.apache.fop.traits.WritingModeTraitsGetter;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_table">
 * <code>fo:table</code></a> object.
 */
public class Table extends TableFObj implements ColumnNumberManagerHolder,
        BreakPropertySet, WritingModeTraitsGetter, CommonAccessibilityHolder {

    // The value of FO traits (refined properties) that apply to fo:table.
    private CommonAccessibility commonAccessibility;
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private CommonMarginBlock commonMarginBlock;
    private LengthRangeProperty blockProgressionDimension;
    private int borderCollapse;
    private LengthPairProperty borderSeparation;
    private int breakAfter;
    private int breakBefore;
    private LengthRangeProperty inlineProgressionDimension;
    private KeepProperty keepTogether;
    private KeepProperty keepWithNext;
    private KeepProperty keepWithPrevious;
    private int tableLayout;
    private int tableOmitFooterAtBreak;
    private int tableOmitHeaderAtBreak;
    private WritingModeTraits writingModeTraits;
    // Unused but valid items, commented out for performance:
    // private CommonAural commonAural;
    // private CommonRelativePosition commonRelativePosition;
    // private int intrusionDisplace;
    // End of FO trait values

    /** extension properties */
    private Length widowContentLimit;
    private Length orphanContentLimit;

    /** collection of columns in this table */
    private List columns = new ArrayList();

    private ColumnNumberManager columnNumberManager = new ColumnNumberManager();

    /** the table-header and -footer */
    private TableHeader tableHeader = null;
    private TableFooter tableFooter = null;

    /** used for validation */
    private boolean tableColumnFound = false;
    private boolean tableHeaderFound = false;
    private boolean tableFooterFound = false;
    private boolean tableBodyFound = false;

    private boolean hasExplicitColumns = false;
    private boolean columnsFinalized = false;
    private RowGroupBuilder rowGroupBuilder;

    /**
     * The table's property list. Used in case the table has no explicit
     * columns, as a parent property list to internally generated TableColumns
     */
    private PropertyList propList;

    /**
     * Construct a Table instance with the given {@link FONode} as parent.
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public Table(final FONode parent) {
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
        this.commonMarginBlock = pList.getMarginBlockProps();
        this.blockProgressionDimension = pList.get(
                PR_BLOCK_PROGRESSION_DIMENSION).getLengthRange();
        this.borderCollapse = pList.get(PR_BORDER_COLLAPSE).getEnum();
        this.borderSeparation = pList.get(PR_BORDER_SEPARATION).getLengthPair();
        this.breakAfter = pList.get(PR_BREAK_AFTER).getEnum();
        this.breakBefore = pList.get(PR_BREAK_BEFORE).getEnum();
        this.inlineProgressionDimension = pList.get(
                PR_INLINE_PROGRESSION_DIMENSION).getLengthRange();
        this.keepTogether = pList.get(PR_KEEP_TOGETHER).getKeep();
        this.keepWithNext = pList.get(PR_KEEP_WITH_NEXT).getKeep();
        this.keepWithPrevious = pList.get(PR_KEEP_WITH_PREVIOUS).getKeep();
        this.tableLayout = pList.get(PR_TABLE_LAYOUT).getEnum();
        this.tableOmitFooterAtBreak = pList.get(PR_TABLE_OMIT_FOOTER_AT_BREAK)
                .getEnum();
        this.tableOmitHeaderAtBreak = pList.get(PR_TABLE_OMIT_HEADER_AT_BREAK)
                .getEnum();
        this.writingModeTraits = new WritingModeTraits(
                WritingMode.valueOf(pList.get(PR_WRITING_MODE).getEnum()));

        // Bind extension properties
        this.widowContentLimit = pList.get(PR_X_WIDOW_CONTENT_LIMIT)
                .getLength();
        this.orphanContentLimit = pList.get(PR_X_ORPHAN_CONTENT_LIMIT)
                .getLength();

        if (!this.blockProgressionDimension.getOptimum(null).isAuto()) {
            final TableEventProducer eventProducer = TableEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.nonAutoBPDOnTable(this, getLocator());
            // Anyway, the bpd of a table is not used by the layout code
        }
        if (this.tableLayout == EN_AUTO) {
            getFOValidationEventProducer().unimplementedFeature(this,
                    getName(), "table-layout=\"auto\"", getLocator());
        }
        if (!isSeparateBorderModel()) {
            if (this.borderCollapse == EN_COLLAPSE_WITH_PRECEDENCE) {
                getFOValidationEventProducer()
                        .unimplementedFeature(
                                this,
                                getName(),
                                "border-collapse=\"collapse-with-precedence\"; defaulting to \"collapse\"",
                                getLocator());
                this.borderCollapse = EN_COLLAPSE;
            }
            if (getCommonBorderPaddingBackground().hasPadding(
                    ValidationPercentBaseContext.getPseudoContext())) {
                // See "17.6.2 The collapsing border model" in CSS2
                final TableEventProducer eventProducer = TableEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.noTablePaddingWithCollapsingBorderModel(this,
                        getLocator());
            }
        }

        /*
         * Store reference to the property list, so new lists can be created in
         * case the table has no explicit columns (see addDefaultColumn())
         */
        this.propList = pList;
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startTable(this);
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model:
     * (marker*,table-column*,table-header?,table-footer?,table-body+)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if ("marker".equals(localName)) {
                if (this.tableColumnFound || this.tableHeaderFound
                        || this.tableFooterFound || this.tableBodyFound) {
                    nodesOutOfOrderError(loc, "fo:marker",
                            "(table-column*,table-header?,table-footer?,table-body+)");
                }
            } else if ("table-column".equals(localName)) {
                this.tableColumnFound = true;
                if (this.tableHeaderFound || this.tableFooterFound
                        || this.tableBodyFound) {
                    nodesOutOfOrderError(loc, "fo:table-column",
                            "(table-header?,table-footer?,table-body+)");
                }
            } else if ("table-header".equals(localName)) {
                if (this.tableHeaderFound) {
                    tooManyNodesError(loc, "table-header");
                } else {
                    this.tableHeaderFound = true;
                    if (this.tableFooterFound || this.tableBodyFound) {
                        nodesOutOfOrderError(loc, "fo:table-header",
                                "(table-footer?,table-body+)");
                    }
                }
            } else if ("table-footer".equals(localName)) {
                if (this.tableFooterFound) {
                    tooManyNodesError(loc, "table-footer");
                } else {
                    this.tableFooterFound = true;
                    if (this.tableBodyFound) {
                        if (getUserAgent().validateStrictly()) {
                            nodesOutOfOrderError(loc, "fo:table-footer",
                                    "(table-body+)", true);
                        }
                        if (!isSeparateBorderModel()) {
                            final TableEventProducer eventProducer = TableEventProducer.Provider
                                    .get(getUserAgent().getEventBroadcaster());
                            eventProducer.footerOrderCannotRecover(this,
                                    getName(), getLocator());
                        }
                    }
                }
            } else if ("table-body".equals(localName)) {
                this.tableBodyFound = true;
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        super.endOfNode();
        getFOEventHandler().endTable(this);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeNode() throws FOPException {

        if (!this.tableBodyFound) {
            missingChildElementError("(marker*,table-column*,table-header?,table-footer?"
                    + ",table-body+)");
        }
        if (!hasChildren()) {
            getParent().removeChild(this);
            return;
        }
        if (!inMarker()) {
            this.rowGroupBuilder.endTable();
            /* clean up */
            for (int i = this.columns.size(); --i >= 0;) {
                final TableColumn col = (TableColumn) this.columns.get(i);
                if (col != null) {
                    col.releasePropertyList();
                }
            }
            this.propList = null;
            this.rowGroupBuilder = null;
        }

    }

    /** {@inheritDoc} */
    @Override
    protected void addChildNode(final FONode child) throws FOPException {

        final int childId = child.getNameId();

        switch (childId) {
        case FO_TABLE_COLUMN:
            this.hasExplicitColumns = true;
            if (!inMarker()) {
                addColumnNode((TableColumn) child);
            } else {
                this.columns.add(child);
            }
            break;
        case FO_TABLE_HEADER:
        case FO_TABLE_FOOTER:
        case FO_TABLE_BODY:
            if (!inMarker() && !this.columnsFinalized) {
                this.columnsFinalized = true;
                if (this.hasExplicitColumns) {
                    finalizeColumns();
                    this.rowGroupBuilder = new FixedColRowGroupBuilder(this);
                } else {
                    this.rowGroupBuilder = new VariableColRowGroupBuilder(this);
                }

            }
            switch (childId) {
            case FO_TABLE_FOOTER:
                this.tableFooter = (TableFooter) child;
                break;
            case FO_TABLE_HEADER:
                this.tableHeader = (TableHeader) child;
                break;
            default:
                super.addChildNode(child);
            }
            break;
        default:
            super.addChildNode(child);
        }
    }

    private void finalizeColumns() throws FOPException {
        for (int i = 0; i < this.columns.size(); i++) {
            if (this.columns.get(i) == null) {
                this.columns.set(i, createImplicitColumn(i + 1));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CommonAccessibility getCommonAccessibility() {
        return this.commonAccessibility;
    }

    /** {@inheritDoc} */
    @Override
    public Table getTable() {
        return this;
    }

    /**
     * Creates the appropriate number of additional implicit columns to match
     * the given column number. Used when the table has no explicit column: the
     * number of columns is then determined by the row that has the most
     * columns.
     *
     * @param columnNumber
     *            the table must at least have this number of column
     * @throws FOPException
     *             if there was an error creating the property list for implicit
     *             columns
     */
    void ensureColumnNumber(final int columnNumber) throws FOPException {
        assert !this.hasExplicitColumns;
        for (int i = this.columns.size() + 1; i <= columnNumber; i++) {
            this.columns.add(createImplicitColumn(i));
        }
    }

    private TableColumn createImplicitColumn(final int colNumber)
            throws FOPException {
        final TableColumn implicitColumn = new TableColumn(this, true);
        final PropertyList pList = new StaticPropertyList(implicitColumn,
                this.propList);
        implicitColumn.bind(pList);
        implicitColumn.setColumnWidth(new TableColLength(1.0, implicitColumn));
        implicitColumn.setColumnNumber(colNumber);
        if (!isSeparateBorderModel()) {
            implicitColumn.setCollapsedBorders(this.collapsingBorderModel); // TODO
        }
        return implicitColumn;
    }

    /**
     * Adds a column to the columns List, and updates the columnIndex used for
     * determining initial values for column-number
     *
     * @param col
     *            the column to add
     */
    private void addColumnNode(final TableColumn col) {

        final int colNumber = col.getColumnNumber();
        final int colRepeat = col.getNumberColumnsRepeated();

        /*
         * add nulls for non-occupied indices between the last column up to and
         * including the current one
         */
        while (this.columns.size() < colNumber + colRepeat - 1) {
            this.columns.add(null);
        }

        // in case column is repeated:
        // for the time being, add the same column
        // (colRepeat - 1) times to the columns list
        // TODO: need to force the column-number (?)
        for (int i = colNumber - 1; i < colNumber + colRepeat - 1; i++) {
            this.columns.set(i, col);
        }

        this.columnNumberManager.signalUsedColumnNumbers(colNumber, colNumber
                + colRepeat - 1);
    }

    boolean hasExplicitColumns() {
        return this.hasExplicitColumns;
    }

    /** @return true of table-layout="auto" */
    public boolean isAutoLayout() {
        return this.tableLayout == EN_AUTO;
    }

    /**
     * Returns the list of table-column elements.
     *
     * @return a list of {@link TableColumn} elements, may contain null elements
     */
    public List getColumns() {
        return this.columns;
    }

    /**
     * Returns the column at the given index.
     *
     * @param index
     *            index of the column to be retrieved, 0-based
     * @return the corresponding column (may be an implicitly created column)
     */
    public TableColumn getColumn(final int index) {
        return (TableColumn) this.columns.get(index);
    }

    /**
     * Returns the number of columns of this table.
     *
     * @return the number of columns, implicit or explicit, in this table
     */
    public int getNumberOfColumns() {
        return this.columns.size();
    }

    /** @return the body for the table-header. */
    public TableHeader getTableHeader() {
        return this.tableHeader;
    }

    /** @return the body for the table-footer. */
    public TableFooter getTableFooter() {
        return this.tableFooter;
    }

    /** @return true if the table-header should be omitted at breaks */
    public boolean omitHeaderAtBreak() {
        return this.tableOmitHeaderAtBreak == EN_TRUE;
    }

    /** @return true if the table-footer should be omitted at breaks */
    public boolean omitFooterAtBreak() {
        return this.tableOmitFooterAtBreak == EN_TRUE;
    }

    /**
     * @return the "inline-progression-dimension" FO trait.
     */
    public LengthRangeProperty getInlineProgressionDimension() {
        return this.inlineProgressionDimension;
    }

    /**
     * @return the "block-progression-dimension" FO trait.
     */
    public LengthRangeProperty getBlockProgressionDimension() {
        return this.blockProgressionDimension;
    }

    /**
     * @return the Common Margin Properties-Block.
     */
    public CommonMarginBlock getCommonMarginBlock() {
        return this.commonMarginBlock;
    }

    /**
     * @return the Common Border, Padding, and Background Properties.
     */
    @Override
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /** @return the "break-after" FO trait. */
    @Override
    public int getBreakAfter() {
        return this.breakAfter;
    }

    /** @return the "break-before" FO trait. */
    @Override
    public int getBreakBefore() {
        return this.breakBefore;
    }

    /** @return the "keep-with-next" FO trait. */
    public KeepProperty getKeepWithNext() {
        return this.keepWithNext;
    }

    /** @return the "keep-with-previous" FO trait. */
    public KeepProperty getKeepWithPrevious() {
        return this.keepWithPrevious;
    }

    /** @return the "keep-together" FO trait. */
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

    /** @return the "border-collapse" FO trait. */
    public int getBorderCollapse() {
        return this.borderCollapse;
    }

    /** @return true if the separate border model is active */
    public boolean isSeparateBorderModel() {
        return getBorderCollapse() == EN_SEPARATE;
    }

    /** @return the "border-separation" FO trait. */
    public LengthPairProperty getBorderSeparation() {
        return this.borderSeparation;
    }

    /** {@inheritDoc} */
    @Override
    public Direction getInlineProgressionDirection() {
        return this.writingModeTraits.getInlineProgressionDirection();
    }

    /** {@inheritDoc} */
    @Override
    public Direction getBlockProgressionDirection() {
        return this.writingModeTraits.getBlockProgressionDirection();
    }

    /** {@inheritDoc} */
    @Override
    public Direction getColumnProgressionDirection() {
        return this.writingModeTraits.getColumnProgressionDirection();
    }

    /** {@inheritDoc} */
    @Override
    public Direction getRowProgressionDirection() {
        return this.writingModeTraits.getRowProgressionDirection();
    }

    /** {@inheritDoc} */
    @Override
    public Direction getShiftDirection() {
        return this.writingModeTraits.getShiftDirection();
    }

    /** {@inheritDoc} */
    @Override
    public WritingMode getWritingMode() {
        return this.writingModeTraits.getWritingMode();
    }

    /** @return the "fox:widow-content-limit" extension FO trait */
    public Length getWidowContentLimit() {
        return this.widowContentLimit;
    }

    /** @return the "fox:orphan-content-limit" extension FO trait */
    public Length getOrphanContentLimit() {
        return this.orphanContentLimit;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "table";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_TABLE}
     */
    @Override
    public int getNameId() {
        return FO_TABLE;
    }

    /** {@inheritDoc} */
    @Override
    public FONode clone(final FONode parent, final boolean removeChildren)
            throws FOPException {
        final Table clone = (Table) super.clone(parent, removeChildren);
        if (removeChildren) {
            clone.columns = new ArrayList();
            clone.columnsFinalized = false;
            clone.columnNumberManager = new ColumnNumberManager();
            clone.tableHeader = null;
            clone.tableFooter = null;
            clone.rowGroupBuilder = null;
        }
        return clone;
    }

    /** {@inheritDoc} */
    @Override
    public ColumnNumberManager getColumnNumberManager() {
        return this.columnNumberManager;
    }

    RowGroupBuilder getRowGroupBuilder() {
        return this.rowGroupBuilder;
    }

    @Override
    protected Stack collectDelimitedTextRanges(Stack ranges,
            final DelimitedTextRange currentRange) {
        // header sub-tree
        final TableHeader header = getTableHeader();
        if (header != null) {
            ranges = header.collectDelimitedTextRanges(ranges);
        }
        // footer sub-tree
        final TableFooter footer = getTableFooter();
        if (footer != null) {
            ranges = footer.collectDelimitedTextRanges(ranges);
        }
        // body sub-tree
        for (final Iterator it = getChildNodes(); it != null && it.hasNext();) {
            ranges = ((FONode) it.next()).collectDelimitedTextRanges(ranges);
        }
        return ranges;
    }

}
