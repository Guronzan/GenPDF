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

/* $Id: ListProperty.java 1303891 2012-03-22 17:04:12Z vhennebert $ */

package org.apache.fop.fo.properties;

import java.util.List;

import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.util.CompareUtil;

/**
 * Superclass for properties that are lists of other properties
 */
public class ListProperty extends Property {

    /**
     * Inner class for creating instances of {@code ListProperty}
     */
    public static class Maker extends PropertyMaker {

        /**
         * Create a maker for the given property id.
         *
         * @param propId
         *            ID of the property for which Maker should be created
         */
        public Maker(final int propId) {
            super(propId);
        }

        /** {@inheritDoc} */
        @Override
        public Property convertProperty(final Property p,
                final PropertyList propertyList, final FObj fo)
                        throws PropertyException {
            if (p instanceof ListProperty) {
                return p;
            } else {
                return new ListProperty(p);
            }
        }

    }

    /** List containing the list of sub-properties */
    protected final List<Property> list = new java.util.ArrayList<Property>();

    /**
     * Simple constructor used by subclasses to do some special processing.
     */
    protected ListProperty() {
        // nop
    }

    /**
     * Create a new instance, using the given {@link Property} as the first
     * element in the list.
     *
     * @param prop
     *            the first property to be added to the list
     */
    public ListProperty(final Property prop) {
        this();
        addProperty(prop);
    }

    /**
     * Add a new property to the list
     *
     * @param prop
     *            Property to be added to the list
     */
    public void addProperty(final Property prop) {
        this.list.add(prop);
    }

    /**
     * Return the {@code java.util.List} of {@link Property} instances contained
     * in this property.
     *
     * @return the list of properties contained in this instance
     */
    @Override
    public List<Property> getList() {
        return this.list;
    }

    /**
     * Return the {@code java.util.List} of {@link Property} instances, cast as
     * a {@code java.lang.Object}.
     *
     * @return this.list cast as an Object
     */
    @Override
    public Object getObject() {
        return this.list;
    }

    @Override
    public int hashCode() {
        return this.list.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ListProperty)) {
            return false;
        }
        final ListProperty other = (ListProperty) obj;
        return CompareUtil.equal(this.list, other.list);
    }
}
