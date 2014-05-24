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

/* $Id: EnumLength.java 1303891 2012-03-22 17:04:12Z vhennebert $ */

package org.apache.fop.fo.properties;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.util.CompareUtil;

/**
 * A length quantity in XSL which is specified as an enum, such as "auto"
 */
@Slf4j
public class EnumLength extends LengthProperty {
    private final Property enumProperty;

    /**
     * Construct an enumerated length from an enum property.
     *
     * @param enumProperty
     *            the enumeration property
     */
    public EnumLength(final Property enumProperty) {
        this.enumProperty = enumProperty;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getEnum() {
        return this.enumProperty.getEnum();
    }

    /** @return true if absolute */
    @Override
    public boolean isAbsolute() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getValue() {
        log.error("getValue() called on " + this.enumProperty + " length");
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getValue(final PercentBaseContext context) {
        log.error("getValue() called on " + this.enumProperty + " length");
        return 0;
    }

    /**
     * {@inheritDoc}
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
    public double getNumericValue(final PercentBaseContext context) {
        log.error("getNumericValue() called on " + this.enumProperty
                + " number");
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString() {
        return this.enumProperty.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getObject() {
        return this.enumProperty.getObject();
    }

    @Override
    public int hashCode() {
        return this.enumProperty.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EnumLength)) {
            return false;
        }
        final EnumLength other = (EnumLength) obj;
        return CompareUtil.equal(this.enumProperty, other.enumProperty);
    }
}
