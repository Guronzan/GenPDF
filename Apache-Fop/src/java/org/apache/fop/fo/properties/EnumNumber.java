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

/* $Id: EnumNumber.java 1303891 2012-03-22 17:04:12Z vhennebert $ */

package org.apache.fop.fo.properties;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.util.CompareUtil;

/**
 * A number quantity in XSL which is specified as an enum, such as "no-limit".
 */
@Slf4j
public final class EnumNumber extends Property implements Numeric {

    /** cache holding all canonical EnumNumber instances */
    private static final PropertyCache<EnumNumber> CACHE = new PropertyCache<EnumNumber>();

    private final EnumProperty enumProperty;

    /**
     * Constructor
     *
     * @param enumProperty
     *            the base EnumProperty
     */
    private EnumNumber(final Property enumProperty) {
        this.enumProperty = (EnumProperty) enumProperty;
    }

    /**
     * Returns the canonical EnumNumber instance corresponding to the given
     * Property
     *
     * @param enumProperty
     *            the base EnumProperty
     * @return the canonical instance
     */
    public static EnumNumber getInstance(final Property enumProperty) {
        return CACHE.fetch(new EnumNumber(enumProperty));
    }

    /** {@inheritDoc} */
    @Override
    public int getEnum() {
        return this.enumProperty.getEnum();
    }

    /** {@inheritDoc} */
    @Override
    public String getString() {
        return this.enumProperty.toString();
    }

    /** {@inheritDoc} */
    @Override
    public Object getObject() {
        return this.enumProperty.getObject();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EnumNumber)) {
            return false;
        }
        final EnumNumber other = (EnumNumber) obj;
        return CompareUtil.equal(this.enumProperty, other.enumProperty);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.enumProperty.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return 0;
    }

    /**
     * {@inheritDoc} Always <code>true</code> for instances of this type
     */
    @Override
    public boolean isAbsolute() {
        return true;
    }

    /**
     * {@inheritDoc} logs an error, because it's not supposed to be called
     */
    @Override
    public double getNumericValue(final PercentBaseContext context)
            throws PropertyException {
        log.error("getNumericValue() called on " + this.enumProperty
                + " number");
        return 0;
    }

    /**
     * {@inheritDoc} logs an error, because it's not supposed to be called
     */
    @Override
    public int getValue(final PercentBaseContext context) {
        log.error("getValue() called on " + this.enumProperty + " number");
        return 0;
    }

    /**
     * {@inheritDoc} logs an error, because it's not supposed to be called
     */
    @Override
    public int getValue() {
        log.error("getValue() called on " + this.enumProperty + " number");
        return 0;
    }

    /**
     * {@inheritDoc} logs an error, because it's not supposed to be called
     */
    @Override
    public double getNumericValue() {
        log.error("getNumericValue() called on " + this.enumProperty
                + " number");
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Numeric getNumeric() {
        return this;
    }

}
