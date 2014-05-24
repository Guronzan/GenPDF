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

/* $Id: PercentLength.java 1303891 2012-03-22 17:04:12Z vhennebert $ */

package org.apache.fop.fo.properties;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.datatypes.PercentBase;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.util.CompareUtil;

/**
 * a percent specified length quantity in XSL
 */
@Slf4j
public class PercentLength extends LengthProperty {

    /**
     * The percentage itself, expressed as a decimal value, e.g. for 95%, set
     * the value to .95
     */
    private final double factor;

    /**
     * A PercentBase implementation that contains the base length to which the
     * {@link #factor} should be applied to compute the actual length
     */
    private final PercentBase lbase;

    /**
     * Main constructor. Construct an object based on a factor (the percent, as
     * a factor) and an object which has a method to return the Length which
     * provides the "base" for the actual length that is modeled.
     *
     * @param factor
     *            the percentage factor, expressed as a decimal (e.g. use .95 to
     *            represent 95%)
     * @param lbase
     *            base property to which the factor should be applied
     */
    public PercentLength(final double factor, final PercentBase lbase) {
        this.factor = factor;
        this.lbase = lbase;
    }

    /**
     * @return the base
     */
    public PercentBase getBaseLength() {
        return this.lbase;
    }

    /**
     * Used during property resolution to check for negative percentages
     *
     * @return the percentage value
     */
    protected double getPercentage() {
        return this.factor * 100;
    }

    /**
     * Return false because percent-length are always relative. {@inheritDoc}
     */
    @Override
    public boolean isAbsolute() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public double getNumericValue() {
        return getNumericValue(null);
    }

    /** {@inheritDoc} */
    @Override
    public double getNumericValue(final PercentBaseContext context) {
        try {
            return this.factor * this.lbase.getBaseLength(context);
        } catch (final PropertyException exc) {
            log.error(exc.getMessage(), exc);
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getString() {
        return this.factor * 100.0 + "%";
    }

    /**
     * Return the length of this PercentLength. {@inheritDoc}
     */
    @Override
    public int getValue() {
        return (int) getNumericValue();
    }

    /** {@inheritDoc} */
    @Override
    public int getValue(final PercentBaseContext context) {
        return (int) getNumericValue(context);
    }

    /**
     * @return the String equivalent of this
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(PercentLength.class.getName())
        .append("[factor=").append(this.factor).append(",lbase=")
                .append(this.lbase).append("]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + CompareUtil.getHashCode(this.factor);
        result = prime * result + CompareUtil.getHashCode(this.lbase);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PercentLength)) {
            return false;
        }
        final PercentLength other = (PercentLength) obj;
        return CompareUtil.equal(this.factor, other.factor)
                && CompareUtil.equal(this.lbase, other.lbase);
    }
}
