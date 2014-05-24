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

/* $Id: VariableColRowGroupBuilder.java 1296405 2012-03-02 19:36:45Z gadams $ */

package org.apache.fop.fo.flow.table;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.fop.fo.ValidationException;

/**
 * A row group builder accommodating a variable number of columns. More
 * flexible, but less efficient.
 */
class VariableColRowGroupBuilder extends RowGroupBuilder {

    VariableColRowGroupBuilder(final Table t) {
        super(t);
    }

    /**
     * Each event is recorded and will be played once the table is finished, and
     * the final number of columns known.
     */
    private interface Event {
        /**
         * Plays this event
         *
         * @param rowGroupBuilder
         *            the delegate builder which will actually create the row
         *            groups
         * @throws ValidationException
         *             if a row-spanning cell overflows its parent body
         */
        void play(final RowGroupBuilder rowGroupBuilder)
                throws ValidationException;
    }

    /** The queue of events sent to this builder. */
    private final List events = new LinkedList();

    /** {@inheritDoc} */
    @Override
    void addTableCell(final TableCell cell) {
        this.events.add(new Event() {
            @Override
            public void play(final RowGroupBuilder rowGroupBuilder) {
                rowGroupBuilder.addTableCell(cell);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    void startTableRow(final TableRow tableRow) {
        this.events.add(new Event() {
            @Override
            public void play(final RowGroupBuilder rowGroupBuilder) {
                rowGroupBuilder.startTableRow(tableRow);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    void endTableRow() {
        this.events.add(new Event() {
            @Override
            public void play(final RowGroupBuilder rowGroupBuilder) {
                rowGroupBuilder.endTableRow();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    void endRow(final TablePart part) {
        this.events.add(new Event() {
            @Override
            public void play(final RowGroupBuilder rowGroupBuilder) {
                rowGroupBuilder.endRow(part);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    void startTablePart(final TablePart part) {
        this.events.add(new Event() {
            @Override
            public void play(final RowGroupBuilder rowGroupBuilder) {
                rowGroupBuilder.startTablePart(part);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    void endTablePart() throws ValidationException {
        // TODO catch the ValidationException sooner?
        this.events.add(new Event() {
            @Override
            public void play(final RowGroupBuilder rowGroupBuilder)
                    throws ValidationException {
                rowGroupBuilder.endTablePart();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    void endTable() throws ValidationException {
        final RowGroupBuilder delegate = new FixedColRowGroupBuilder(this.table);
        for (final Iterator eventIter = this.events.iterator(); eventIter
                .hasNext();) {
            ((Event) eventIter.next()).play(delegate);
        }
        delegate.endTable();
    }
}
