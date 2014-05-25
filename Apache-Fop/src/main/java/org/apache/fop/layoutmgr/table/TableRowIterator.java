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

/* $Id: TableRowIterator.java 985571 2010-08-14 19:28:26Z jeremias $ */

package org.apache.fop.layoutmgr.table;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FONode.FONodeIterator;
import org.apache.fop.fo.flow.table.EffRow;
import org.apache.fop.fo.flow.table.Table;
import org.apache.fop.fo.flow.table.TableBody;

/**
 * Iterator that lets the table layout manager step over all the rows of a part
 * of the table (table-header, table-footer or table-body).
 * <p>
 * Note: This class is not thread-safe.
 * </p>
 */
public class TableRowIterator {

    /** Selects the table-body elements for iteration. */
    public static final int BODY = 0;
    /** Selects the table-header elements for iteration. */
    public static final int HEADER = 1;
    /** Selects the table-footer elements for iteration. */
    public static final int FOOTER = 2;

    /** The table on which this instance operates. */
    protected Table table;

    /** Part of the table over which to iterate. One of BODY, HEADER or FOOTER. */
    private final int tablePart;

    private Iterator rowGroupsIter;

    private int rowIndex = 0;

    /**
     * Creates a new TableRowIterator.
     * 
     * @param table
     *            the table to iterate over
     * @param tablePart
     *            indicates what part of the table to iterate over (HEADER,
     *            FOOTER, BODY)
     */
    public TableRowIterator(final Table table, final int tablePart) {
        this.table = table;
        this.tablePart = tablePart;
        switch (tablePart) {
        case HEADER:
            this.rowGroupsIter = table.getTableHeader().getRowGroups()
                    .iterator();
            break;
        case FOOTER:
            this.rowGroupsIter = table.getTableFooter().getRowGroups()
                    .iterator();
            break;
        case BODY:
            final List rowGroupsList = new LinkedList();
            // TODO this is ugly
            for (final FONodeIterator iter = table.getChildNodes(); iter
                    .hasNext();) {
                final FONode node = iter.nextNode();
                if (node instanceof TableBody) {
                    rowGroupsList.addAll(((TableBody) node).getRowGroups());
                }
            }
            this.rowGroupsIter = rowGroupsList.iterator();
            break;
        default:
            throw new IllegalArgumentException("Unrecognised TablePart: "
                    + tablePart);
        }
    }

    /**
     * Returns the next row group if any. A row group in this context is the
     * minimum number of consecutive rows which contains all spanned grid units
     * of its cells.
     * 
     * @return the next row group, or null
     */
    EffRow[] getNextRowGroup() {
        if (!this.rowGroupsIter.hasNext()) {
            return null;
        }
        final List rowGroup = (List) this.rowGroupsIter.next();
        final EffRow[] effRowGroup = new EffRow[rowGroup.size()];
        int i = 0;
        for (final Iterator rowIter = rowGroup.iterator(); rowIter.hasNext();) {
            final List gridUnits = (List) rowIter.next();
            effRowGroup[i++] = new EffRow(this.rowIndex++, this.tablePart,
                    gridUnits);
        }
        return effRowGroup;
    }

}
