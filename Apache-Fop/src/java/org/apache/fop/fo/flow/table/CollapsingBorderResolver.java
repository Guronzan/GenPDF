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

/* $Id: CollapsingBorderResolver.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.flow.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.layoutmgr.table.CollapsingBorderModel;

/**
 * A class that implements the border-collapsing model.
 */
class CollapsingBorderResolver implements BorderResolver {

    private final Table table;

    private final CollapsingBorderModel collapsingBorderModel;

    /**
     * The previously registered row, either in the header or the body(-ies),
     * but not in the footer (handled separately).
     */
    private List/* <GridUnit> */previousRow;

    private boolean firstInTable;

    private List/* <GridUnit> */footerFirstRow;

    /** The last currently registered footer row. */
    private List/* <GridUnit> */footerLastRow;

    private Resolver delegate;

    // Re-use the same ResolverInBody for every table-body
    // Important to properly handle firstInBody!!
    private final Resolver resolverInBody = new ResolverInBody();

    private Resolver resolverInFooter;

    private List/* <ConditionalBorder> */leadingBorders;

    private List/* <ConditionalBorder> */trailingBorders;

    /* TODO Temporary hack for resolved borders in header */
    /* Currently the normal border is always used. */
    private List/* <GridUnit> */headerLastRow = null;

    /* End of temporary hack */

    /**
     * Base class for delegate resolvers. Implementation of the State design
     * pattern: the treatment differs slightly whether we are in the table's
     * header, footer or body. To avoid complicated if statements, specialised
     * delegate resolvers will be used instead.
     */
    private abstract class Resolver {

        protected TablePart tablePart;

        protected boolean firstInPart;

        private BorderSpecification borderStartTableAndBody;
        private BorderSpecification borderEndTableAndBody;

        /**
         * Integrates border-before specified on the table and its column.
         *
         * @param row
         *            the first row of the table (in the header, or in the body
         *            if the table has no header)
         * @param withNormal
         * @param withLeadingTrailing
         * @param withRest
         */
        void resolveBordersFirstRowInTable(final List/* <GridUnit> */row,
                final boolean withNormal, final boolean withLeadingTrailing,
                final boolean withRest) {
            assert CollapsingBorderResolver.this.firstInTable;
            for (int i = 0; i < row.size(); i++) {
                final TableColumn column = CollapsingBorderResolver.this.table
                        .getColumn(i);
                ((GridUnit) row.get(i)).integrateBorderSegment(
                        CommonBorderPaddingBackground.BEFORE, column,
                        withNormal, withLeadingTrailing, withRest);
            }
            CollapsingBorderResolver.this.firstInTable = false;
        }

        /**
         * Resolves border-after for the first row, border-before for the second
         * one.
         *
         * @param rowBefore
         * @param rowAfter
         */
        void resolveBordersBetweenRows(final List/* <GridUnit> */rowBefore,
                final List/* <GridUnit> */rowAfter) {
            assert rowBefore != null && rowAfter != null;
            for (int i = 0; i < rowAfter.size(); i++) {
                final GridUnit gu = (GridUnit) rowAfter.get(i);
                if (gu.getRowSpanIndex() == 0) {
                    final GridUnit beforeGU = (GridUnit) rowBefore.get(i);
                    gu.resolveBorder(beforeGU,
                            CommonBorderPaddingBackground.BEFORE);
                }
            }
        }

        /** Integrates the border-after of the part. */
        void resolveBordersLastRowInPart(final List/* <GridUnit> */row,
                final boolean withNormal, final boolean withLeadingTrailing,
                final boolean withRest) {
            for (int i = 0; i < row.size(); i++) {
                ((GridUnit) row.get(i)).integrateBorderSegment(
                        CommonBorderPaddingBackground.AFTER, this.tablePart,
                        withNormal, withLeadingTrailing, withRest);
            }
        }

        /**
         * Integrates border-after specified on the table and its columns.
         *
         * @param row
         *            the last row of the footer, or of the last body if the
         *            table has no footer
         * @param withNormal
         * @param withLeadingTrailing
         * @param withRest
         */
        void resolveBordersLastRowInTable(final List/* <GridUnit> */row,
                final boolean withNormal, final boolean withLeadingTrailing,
                final boolean withRest) {
            for (int i = 0; i < row.size(); i++) {
                final TableColumn column = CollapsingBorderResolver.this.table
                        .getColumn(i);
                ((GridUnit) row.get(i)).integrateBorderSegment(
                        CommonBorderPaddingBackground.AFTER, column,
                        withNormal, withLeadingTrailing, withRest);
            }
        }

        /**
         * Integrates either border-before specified on the table and its
         * columns if the table has no header, or border-after specified on the
         * cells of the header's last row. For the case the grid unit are at the
         * top of a page.
         *
         * @param row
         */
        void integrateLeadingBorders(final List/* <GridUnit> */row) {
            for (int i = 0; i < CollapsingBorderResolver.this.table
                    .getNumberOfColumns(); i++) {
                final GridUnit gu = (GridUnit) row.get(i);
                final ConditionalBorder border = (ConditionalBorder) CollapsingBorderResolver.this.leadingBorders
                        .get(i);
                gu.integrateCompetingBorder(
                        CommonBorderPaddingBackground.BEFORE, border, false,
                        true, true);
            }
        }

        /**
         * Integrates either border-after specified on the table and its columns
         * if the table has no footer, or border-before specified on the cells
         * of the footer's first row. For the case the grid unit are at the
         * bottom of a page.
         *
         * @param row
         */
        void integrateTrailingBorders(final List/* <GridUnit> */row) {
            for (int i = 0; i < CollapsingBorderResolver.this.table
                    .getNumberOfColumns(); i++) {
                final GridUnit gu = (GridUnit) row.get(i);
                final ConditionalBorder border = (ConditionalBorder) CollapsingBorderResolver.this.trailingBorders
                        .get(i);
                gu.integrateCompetingBorder(
                        CommonBorderPaddingBackground.AFTER, border, false,
                        true, true);
            }
        }

        void startPart(final TablePart part) {
            this.tablePart = part;
            this.firstInPart = true;
            this.borderStartTableAndBody = CollapsingBorderResolver.this.collapsingBorderModel
                    .determineWinner(
                            CollapsingBorderResolver.this.table.borderStart,
                            this.tablePart.borderStart);
            this.borderEndTableAndBody = CollapsingBorderResolver.this.collapsingBorderModel
                    .determineWinner(
                            CollapsingBorderResolver.this.table.borderEnd,
                            this.tablePart.borderEnd);
        }

        /**
         * Resolves the applicable borders for the given row.
         * <ul>
         * <li>Integrates the border-before/after of the containing table-row if
         * any;</li>
         * <li>Integrates the border-before of the containing part, if first
         * row;</li>
         * <li>Resolves border-start/end between grid units.</li>
         * </ul>
         *
         * @param row
         *            the row being finished
         * @param container
         *            the containing element
         */
        void endRow(final List/* <GridUnit> */row,
                final TableCellContainer container) {
            BorderSpecification borderStart = this.borderStartTableAndBody;
            BorderSpecification borderEnd = this.borderEndTableAndBody;
            // Resolve before- and after-borders for the table-row
            if (container instanceof TableRow) {
                final TableRow tableRow = (TableRow) container;
                for (final Iterator iter = row.iterator(); iter.hasNext();) {
                    final GridUnit gu = (GridUnit) iter.next();
                    final boolean first = gu.getRowSpanIndex() == 0;
                    final boolean last = gu.isLastGridUnitRowSpan();
                    gu.integrateBorderSegment(
                            CommonBorderPaddingBackground.BEFORE, tableRow,
                            first, first, true);
                    gu.integrateBorderSegment(
                            CommonBorderPaddingBackground.AFTER, tableRow,
                            last, last, true);
                }
                borderStart = CollapsingBorderResolver.this.collapsingBorderModel
                        .determineWinner(borderStart, tableRow.borderStart);
                borderEnd = CollapsingBorderResolver.this.collapsingBorderModel
                        .determineWinner(borderEnd, tableRow.borderEnd);
            }
            if (this.firstInPart) {
                // Integrate the border-before of the part
                for (int i = 0; i < row.size(); i++) {
                    ((GridUnit) row.get(i)).integrateBorderSegment(
                            CommonBorderPaddingBackground.BEFORE,
                            this.tablePart, true, true, true);
                }
                this.firstInPart = false;
            }
            // Resolve start/end borders in the row
            final Iterator guIter = row.iterator();
            GridUnit gu = (GridUnit) guIter.next();
            final Iterator colIter = CollapsingBorderResolver.this.table
                    .getColumns().iterator();
            TableColumn col = (TableColumn) colIter.next();
            gu.integrateBorderSegment(CommonBorderPaddingBackground.START, col);
            gu.integrateBorderSegment(CommonBorderPaddingBackground.START,
                    borderStart);
            while (guIter.hasNext()) {
                final GridUnit nextGU = (GridUnit) guIter.next();
                final TableColumn nextCol = (TableColumn) colIter.next();
                if (gu.isLastGridUnitColSpan()) {
                    gu.integrateBorderSegment(
                            CommonBorderPaddingBackground.END, col);
                    nextGU.integrateBorderSegment(
                            CommonBorderPaddingBackground.START, nextCol);
                    gu.resolveBorder(nextGU, CommonBorderPaddingBackground.END);
                }
                gu = nextGU;
                col = nextCol;
            }
            gu.integrateBorderSegment(CommonBorderPaddingBackground.END, col);
            gu.integrateBorderSegment(CommonBorderPaddingBackground.END,
                    borderEnd);
        }

        void endPart() {
            resolveBordersLastRowInPart(
                    CollapsingBorderResolver.this.previousRow, true, true, true);
        }

        abstract void endTable();
    }

    private class ResolverInHeader extends Resolver {

        @Override
        void endRow(final List/* <GridUnit> */row,
                final TableCellContainer container) {
            super.endRow(row, container);
            if (CollapsingBorderResolver.this.previousRow != null) {
                resolveBordersBetweenRows(
                        CollapsingBorderResolver.this.previousRow, row);
            } else {
                /*
                 * This is a bit hacky... The two only sensible values for
                 * border-before on the header's first row are: - at the
                 * beginning of the table (normal case) - if the header is
                 * repeated after each page break To represent those values we
                 * (ab)use the normal and the rest fields of ConditionalBorder.
                 * But strictly speaking this is not their purposes.
                 */
                for (final Iterator guIter = row.iterator(); guIter.hasNext();) {
                    final ConditionalBorder borderBefore = ((GridUnit) guIter
                            .next()).borderBefore;
                    borderBefore.leadingTrailing = borderBefore.normal;
                    borderBefore.rest = borderBefore.normal;
                }
                resolveBordersFirstRowInTable(row, true, false, true);
            }
            CollapsingBorderResolver.this.previousRow = row;
        }

        @Override
        void endPart() {
            super.endPart();
            CollapsingBorderResolver.this.leadingBorders = new ArrayList(
                    CollapsingBorderResolver.this.table.getNumberOfColumns());
            /*
             * Another hack... The border-after of a header is always the same.
             * Leading and rest don't apply to cells in the header since they
             * are never broken. To ease resolution we override the (normally
             * unused) leadingTrailing and rest fields of ConditionalBorder with
             * the only sensible normal field. That way grid units from the body
             * will always resolve against the same, normal header border.
             */
            for (final Iterator guIter = CollapsingBorderResolver.this.previousRow
                    .iterator(); guIter.hasNext();) {
                final ConditionalBorder borderAfter = ((GridUnit) guIter.next()).borderAfter;
                borderAfter.leadingTrailing = borderAfter.normal;
                borderAfter.rest = borderAfter.normal;
                CollapsingBorderResolver.this.leadingBorders.add(borderAfter);
            }
            /* TODO Temporary hack for resolved borders in header */
            CollapsingBorderResolver.this.headerLastRow = CollapsingBorderResolver.this.previousRow;
            /* End of temporary hack */
        }

        @Override
        void endTable() {
            throw new IllegalStateException();
        }
    }

    private class ResolverInFooter extends Resolver {

        @Override
        void endRow(final List/* <GridUnit> */row,
                final TableCellContainer container) {
            super.endRow(row, container);
            if (CollapsingBorderResolver.this.footerFirstRow == null) {
                CollapsingBorderResolver.this.footerFirstRow = row;
            } else {
                // There is a previous row
                resolveBordersBetweenRows(
                        CollapsingBorderResolver.this.footerLastRow, row);
            }
            CollapsingBorderResolver.this.footerLastRow = row;
        }

        @Override
        void endPart() {
            resolveBordersLastRowInPart(
                    CollapsingBorderResolver.this.footerLastRow, true, true,
                    true);
            CollapsingBorderResolver.this.trailingBorders = new ArrayList(
                    CollapsingBorderResolver.this.table.getNumberOfColumns());
            // See same method in ResolverInHeader for an explanation of the
            // hack
            for (final Iterator guIter = CollapsingBorderResolver.this.footerFirstRow
                    .iterator(); guIter.hasNext();) {
                final ConditionalBorder borderBefore = ((GridUnit) guIter
                        .next()).borderBefore;
                borderBefore.leadingTrailing = borderBefore.normal;
                borderBefore.rest = borderBefore.normal;
                CollapsingBorderResolver.this.trailingBorders.add(borderBefore);
            }
        }

        @Override
        void endTable() {
            // Resolve after/before border between the last row of table-body
            // and the
            // first row of table-footer
            resolveBordersBetweenRows(
                    CollapsingBorderResolver.this.previousRow,
                    CollapsingBorderResolver.this.footerFirstRow);
            // See endRow method in ResolverInHeader for an explanation of the
            // hack
            for (final Iterator guIter = CollapsingBorderResolver.this.footerLastRow
                    .iterator(); guIter.hasNext();) {
                final ConditionalBorder borderAfter = ((GridUnit) guIter.next()).borderAfter;
                borderAfter.leadingTrailing = borderAfter.normal;
                borderAfter.rest = borderAfter.normal;
            }
            resolveBordersLastRowInTable(
                    CollapsingBorderResolver.this.footerLastRow, true, false,
                    true);
        }
    }

    private class ResolverInBody extends Resolver {

        private boolean firstInBody = true;

        @Override
        void endRow(final List/* <GridUnit> */row,
                final TableCellContainer container) {
            super.endRow(row, container);
            if (CollapsingBorderResolver.this.firstInTable) {
                resolveBordersFirstRowInTable(row, true, true, true);
            } else {
                // Either there is a header, and then previousRow is set to the
                // header's last row,
                // or this is not the first row in the body, and previousRow is
                // not null
                resolveBordersBetweenRows(
                        CollapsingBorderResolver.this.previousRow, row);
                integrateLeadingBorders(row);
            }
            integrateTrailingBorders(row);
            CollapsingBorderResolver.this.previousRow = row;
            if (this.firstInBody) {
                this.firstInBody = false;
                for (final Iterator iter = row.iterator(); iter.hasNext();) {
                    final GridUnit gu = (GridUnit) iter.next();
                    gu.borderBefore.leadingTrailing = gu.borderBefore.normal;
                }
            }
        }

        @Override
        void endTable() {
            if (CollapsingBorderResolver.this.resolverInFooter != null) {
                CollapsingBorderResolver.this.resolverInFooter.endTable();
            } else {
                // Trailing and rest borders already resolved with
                // integrateTrailingBorders
                resolveBordersLastRowInTable(
                        CollapsingBorderResolver.this.previousRow, true, false,
                        false);
            }
            for (final Iterator iter = CollapsingBorderResolver.this.previousRow
                    .iterator(); iter.hasNext();) {
                final GridUnit gu = (GridUnit) iter.next();
                gu.borderAfter.leadingTrailing = gu.borderAfter.normal;
            }
        }
    }

    CollapsingBorderResolver(final Table table) {
        this.table = table;
        this.collapsingBorderModel = CollapsingBorderModel
                .getBorderModelFor(table.getBorderCollapse());
        this.firstInTable = true;
        // Resolve before and after borders between the table and each
        // table-column
        int index = 0;
        do {
            final TableColumn col = table.getColumn(index);
            // See endRow method in ResolverInHeader for an explanation of the
            // hack
            col.borderBefore.integrateSegment(table.borderBefore, true, false,
                    true);
            col.borderBefore.leadingTrailing = col.borderBefore.rest;
            col.borderAfter.integrateSegment(table.borderAfter, true, false,
                    true);
            col.borderAfter.leadingTrailing = col.borderAfter.rest;
            /*
             * TODO The border resolution must be done only once for each table
             * column, even if it's repeated; otherwise, re-resolving against
             * the table's borders will lead to null border specifications.
             * 
             * Eventually table columns should probably be cloned instead.
             */
            index += col.getNumberColumnsRepeated();
        } while (index < table.getNumberOfColumns());
    }

    /** {@inheritDoc} */
    @Override
    public void endRow(final List/* <GridUnit> */row,
            final TableCellContainer container) {
        this.delegate.endRow(row, container);
    }

    /** {@inheritDoc} */
    @Override
    public void startPart(final TablePart part) {
        if (part instanceof TableHeader) {
            this.delegate = new ResolverInHeader();
        } else {
            if (this.leadingBorders == null || this.table.omitHeaderAtBreak()) {
                // No header, leading borders determined by the table
                this.leadingBorders = new ArrayList(
                        this.table.getNumberOfColumns());
                for (final Iterator colIter = this.table.getColumns()
                        .iterator(); colIter.hasNext();) {
                    final ConditionalBorder border = ((TableColumn) colIter
                            .next()).borderBefore;
                    this.leadingBorders.add(border);
                }
            }
            if (part instanceof TableFooter) {
                this.resolverInFooter = new ResolverInFooter();
                this.delegate = this.resolverInFooter;
            } else {
                if (this.trailingBorders == null
                        || this.table.omitFooterAtBreak()) {
                    // No footer, trailing borders determined by the table
                    this.trailingBorders = new ArrayList(
                            this.table.getNumberOfColumns());
                    for (final Iterator colIter = this.table.getColumns()
                            .iterator(); colIter.hasNext();) {
                        final ConditionalBorder border = ((TableColumn) colIter
                                .next()).borderAfter;
                        this.trailingBorders.add(border);
                    }
                }
                this.delegate = this.resolverInBody;
            }
        }
        this.delegate.startPart(part);
    }

    /** {@inheritDoc} */
    @Override
    public void endPart() {
        this.delegate.endPart();
    }

    /** {@inheritDoc} */
    @Override
    public void endTable() {
        this.delegate.endTable();
        this.delegate = null;
        /* TODO Temporary hack for resolved borders in header */
        if (this.headerLastRow != null) {
            for (final Iterator iter = this.headerLastRow.iterator(); iter
                    .hasNext();) {
                final GridUnit gu = (GridUnit) iter.next();
                gu.borderAfter.leadingTrailing = gu.borderAfter.normal;
            }
        }
        if (this.footerLastRow != null) {
            for (final Iterator iter = this.footerLastRow.iterator(); iter
                    .hasNext();) {
                final GridUnit gu = (GridUnit) iter.next();
                gu.borderAfter.leadingTrailing = gu.borderAfter.normal;
            }
        }
        /* End of temporary hack */
    }
}
