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

/* $Id: EmptyGridUnit.java 631276 2008-02-26 16:23:15Z vhennebert $ */

package org.apache.fop.fo.flow.table;

/**
 * GridUnit subclass for empty grid units.
 */
public class EmptyGridUnit extends GridUnit {

    /**
     * @param table
     *            the containing table
     * @param row
     *            the table-row element this grid unit belongs to (if any)
     * @param colIndex
     *            column index, 0-based
     */
    EmptyGridUnit(final Table table, final TableRow row, final int colIndex) {
        super(table, 0, 0);
        setRow(row);
    }

    /** {@inheritDoc} */
    @Override
    protected void setBordersFromCell() {
        this.borderBefore = ConditionalBorder
                .getDefaultBorder(this.collapsingBorderModel);
        this.borderAfter = ConditionalBorder
                .getDefaultBorder(this.collapsingBorderModel);
        this.borderStart = BorderSpecification.getDefaultBorder();
        this.borderEnd = BorderSpecification.getDefaultBorder();
    }

    /** {@inheritDoc} */
    @Override
    public PrimaryGridUnit getPrimary() {
        throw new UnsupportedOperationException();
        // return this; TODO
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrimary() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLastGridUnitColSpan() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLastGridUnitRowSpan() {
        return true;
    }
}
