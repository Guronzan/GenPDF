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

/* $Id: TableColLength.java 1303891 2012-03-22 17:04:12Z vhennebert $ */

package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.LengthBase;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.FObj;
import org.apache.fop.util.CompareUtil;

/**
 * A table-column width specification, possibly including some number of
 * proportional "column-units". The absolute size of a column-unit depends on
 * the fixed and proportional sizes of all columns in the table, and on the
 * overall size of the table. It can't be calculated until all columns have been
 * specified and until the actual width of the table is known. Since this can be
 * specified as a percent of its parent containing width, the calculation is
 * done during layout. NOTE: this is only supposed to be allowed if
 * table-layout=fixed.
 */
public class TableColLength extends LengthProperty {
    /**
     * Number of table-column proportional units
     */
    private final double tcolUnits;

    /**
     * The column the column-units are defined on.
     */
    private final FObj column;

    /**
     * Construct an object with tcolUnits of proportional measure.
     * 
     * @param tcolUnits
     *            number of table-column proportional units
     * @param column
     *            the column the column-units are defined on
     */
    public TableColLength(final double tcolUnits, final FObj column) {
        this.tcolUnits = tcolUnits;
        this.column = column;
    }

    /**
     * Override the method in Length
     * 
     * @return the number of specified proportional table-column units.
     */
    public double getTableUnits() {
        return this.tcolUnits;
    }

    /**
     * Return false because table-col-units are a relative numeric.
     * {@inheritDoc}
     */
    @Override
    public boolean isAbsolute() {
        return false;
    }

    /**
     * Return the value as a numeric value. {@inheritDoc}
     */
    @Override
    public double getNumericValue() {
        throw new UnsupportedOperationException(
                "Must call getNumericValue with PercentBaseContext");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getNumericValue(final PercentBaseContext context) {
        return this.tcolUnits
                * context.getBaseLength(LengthBase.TABLE_UNITS, this.column);
    }

    /**
     * Return the value as a length. {@inheritDoc}
     */
    @Override
    public int getValue() {
        throw new UnsupportedOperationException(
                "Must call getValue with PercentBaseContext");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getValue(final PercentBaseContext context) {
        return (int) (this.tcolUnits * context.getBaseLength(
                LengthBase.TABLE_UNITS, this.column));
    }

    /**
     * Convert this to a String
     * 
     * @return the string representation of this
     */
    @Override
    public String toString() {
        return Double.toString(this.tcolUnits) + " table-column-units";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + CompareUtil.getHashCode(this.column);
        result = prime * result + CompareUtil.getHashCode(this.tcolUnits);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TableColLength)) {
            return false;
        }
        final TableColLength other = (TableColLength) obj;
        return CompareUtil.equal(this.column, other.column)
                && CompareUtil.equal(this.tcolUnits, other.tcolUnits);
    }
}
