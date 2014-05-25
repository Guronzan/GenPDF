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

/* $Id: CondLengthProperty.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.CompoundDatatype;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.util.CompareUtil;

/**
 * Superclass for properties that have conditional lengths
 */
public class CondLengthProperty extends Property implements CompoundDatatype {

    /** cache holding canonical instances (for absolute conditional lengths) */
    private static final PropertyCache<CondLengthProperty> CACHE = new PropertyCache<CondLengthProperty>();

    /** components */
    private Property length;
    private EnumProperty conditionality;

    private boolean isCached = false;
    private int hash = -1;

    /**
     * Inner class for creating instances of CondLengthProperty
     */
    public static class Maker extends CompoundPropertyMaker {

        /**
         * @param propId
         *            the id of the property for which a Maker should be created
         */
        public Maker(final int propId) {
            super(propId);
        }

        /**
         * Create a new empty instance of CondLengthProperty.
         * 
         * @return the new instance.
         */
        @Override
        public Property makeNewProperty() {
            return new CondLengthProperty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Property convertProperty(final Property p,
                final PropertyList propertyList, final FObj fo)
                throws PropertyException {
            if (p instanceof KeepProperty) {
                return p;
            }
            return super.convertProperty(p, propertyList, fo);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setComponent(final int cmpId, final Property cmpnValue,
            final boolean bIsDefault) {
        if (this.isCached) {
            throw new IllegalStateException(
                    "CondLengthProperty.setComponent() called on a cached value!");
        }

        if (cmpId == CP_LENGTH) {
            this.length = cmpnValue;
        } else if (cmpId == CP_CONDITIONALITY) {
            this.conditionality = (EnumProperty) cmpnValue;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property getComponent(final int cmpId) {
        if (cmpId == CP_LENGTH) {
            return this.length;
        } else if (cmpId == CP_CONDITIONALITY) {
            return this.conditionality;
        } else {
            return null;
        }
    }

    /**
     * Returns the conditionality.
     * 
     * @return the conditionality
     */
    public Property getConditionality() {
        return this.conditionality;
    }

    /**
     * Returns the length.
     * 
     * @return the length
     */
    public Property getLengthComponent() {
        return this.length;
    }

    /**
     * Indicates if the length can be discarded on certain conditions.
     * 
     * @return true if the length can be discarded.
     */
    public boolean isDiscard() {
        return this.conditionality.getEnum() == Constants.EN_DISCARD;
    }

    /**
     * Returns the computed length value.
     * 
     * @return the length in millipoints
     */
    public int getLengthValue() {
        return this.length.getLength().getValue();
    }

    /**
     * Returns the computed length value.
     * 
     * @param context
     *            The context for the length calculation (for percentage based
     *            lengths)
     * @return the length in millipoints
     */
    public int getLengthValue(final PercentBaseContext context) {
        return this.length.getLength().getValue(context);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "CondLength["
                + this.length.getObject().toString()
                + ", "
                + (isDiscard() ? this.conditionality.toString().toLowerCase()
                                : this.conditionality.toString()) + "]";
    }

    /**
     * @return this.condLength
     */
    @Override
    public CondLengthProperty getCondLength() {
        if (this.length.getLength().isAbsolute()) {
            final CondLengthProperty clp = CACHE.fetch(this);
            if (clp == this) {
                this.isCached = true;
            }
            return clp;
        } else {
            return this;
        }
    }

    /**
     * TODO: Should we allow this?
     * 
     * @return this.condLength cast as a Length
     */
    @Override
    public Length getLength() {
        return this.length.getLength();
    }

    /**
     * @return this.condLength cast as an Object
     */
    @Override
    public Object getObject() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof CondLengthProperty) {
            final CondLengthProperty clp = (CondLengthProperty) obj;
            return CompareUtil.equal(this.length, clp.length)
                    && CompareUtil.equal(this.conditionality,
                            clp.conditionality);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        if (this.hash == -1) {
            int hash = 17;
            hash = 37 * hash
                    + (this.length == null ? 0 : this.length.hashCode());
            hash = 37
                    * hash
                    + (this.conditionality == null ? 0 : this.conditionality
                            .hashCode());
            this.hash = hash;
        }
        return this.hash;
    }
}
