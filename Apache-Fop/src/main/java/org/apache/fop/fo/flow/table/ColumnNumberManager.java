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

/* $Id: ColumnNumberManager.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.fo.flow.table;

import java.util.BitSet;
import java.util.List;

/**
 * Helper class maintaining a record of occupied columns and an index to the
 * next non-occupied column.
 */
public class ColumnNumberManager {

    private int columnNumber = 1;

    /**
     * We use the term "index" instead of "number" because, unlike column
     * numbers, it's 0-based.
     */
    private final BitSet usedColumnIndices = new BitSet();

    /**
     * Returns the number of the column that shall receive the next parsed cell.
     *
     * @return a column number, 1-based
     */
    int getCurrentColumnNumber() {
        return this.columnNumber;
    }

    /**
     * Flags columns <code>start</code> to <code>end</code> as occupied, and
     * updates the number of the next available column.
     *
     * @param start
     *            start number, inclusive, 1-based
     * @param end
     *            end number, inclusive
     */
    void signalUsedColumnNumbers(final int start, final int end) {
        for (int i = start - 1; i < end; i++) {
            this.usedColumnIndices.set(i);
        }

        this.columnNumber = end + 1;
        while (this.usedColumnIndices.get(this.columnNumber - 1)) {
            this.columnNumber++;
        }
    }

    /**
     * Resets the record of occupied columns, taking into account columns
     * already occupied by previous spanning cells, and computes the number of
     * the first free column.
     *
     * @param pendingSpans
     *            List&lt;PendingSpan&gt; of possible spans over the next row
     */
    void prepareForNextRow(final List pendingSpans) {
        this.usedColumnIndices.clear();
        PendingSpan pSpan;
        for (int i = 0; i < pendingSpans.size(); i++) {
            pSpan = (PendingSpan) pendingSpans.get(i);
            if (pSpan != null) {
                if (pSpan.decrRowsLeft() == 0) {
                    pendingSpans.set(i, null);
                } else {
                    this.usedColumnIndices.set(i);
                }
            }
        }
        // Set columnNumber to the first available column
        this.columnNumber = 1;
        while (this.usedColumnIndices.get(this.columnNumber - 1)) {
            this.columnNumber++;
        }
    }

    /**
     * Checks whether a given column-number is already in use for the current
     * row.
     *
     * @param colNr
     *            the column-number to check
     * @return true if column-number is already occupied
     */
    public boolean isColumnNumberUsed(final int colNr) {
        return this.usedColumnIndices.get(colNr - 1);
    }

}
