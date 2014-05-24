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

/* $Id: NumericProperty.java 1303891 2012-03-22 17:04:12Z vhennebert $ */

package org.apache.fop.fo.expr;

import java.awt.Color;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.properties.FixedLength;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.util.CompareUtil;

/**
 * A numeric property which hold the final absolute result of an expression
 * calculations.
 */
@Slf4j
public class NumericProperty extends Property implements Length {
    private final double value;
    private final int dim;

    /**
     * Construct a Numeric object by specifying one or more components,
     * including absolute length, percent length, table units.
     *
     * @param value
     *            The value of the numeric.
     * @param dim
     *            The dimension of the value. 0 for a Number, 1 for a Length
     *            (any type), >1, <0 if Lengths have been multiplied or divided.
     */
    protected NumericProperty(final double value, final int dim) {
        this.value = value;
        this.dim = dim;
    }

    /**
     * Return the dimension. {@inheritDoc}
     */
    @Override
    public int getDimension() {
        return this.dim;
    }

    /**
     * Return the value. {@inheritDoc}
     */
    @Override
    public double getNumericValue() {
        return this.value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getNumericValue(final PercentBaseContext context) {
        return this.value;
    }

    /**
     * Return true of the numeric is absolute. {@inheritDoc}
     */
    @Override
    public boolean isAbsolute() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Numeric getNumeric() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Number getNumber() {
        return new Double(this.value);
    }

    /** {@inheritDoc} */
    @Override
    public int getValue() {
        return (int) this.value;
    }

    /** {@inheritDoc} */
    @Override
    public int getValue(final PercentBaseContext context) {
        return (int) this.value;
    }

    /** {@inheritDoc} */
    @Override
    public Length getLength() {
        if (this.dim == 1) {
            return this;
        }
        log.error("Can't create length with dimension " + this.dim);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Color getColor(final FOUserAgent foUserAgent) {
        // TODO: try converting to numeric number and then to color
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Object getObject() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (this.dim == 1) {
            return (int) this.value + FixedLength.MPT;
        } else {
            return this.value + "^" + this.dim;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.dim;
        result = prime * result + CompareUtil.getHashCode(this.value);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NumericProperty)) {
            return false;
        }
        final NumericProperty other = (NumericProperty) obj;
        return this.dim == other.dim
                && CompareUtil.equal(this.value, other.value);
    }
}
