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

/* $Id: LengthPairProperty.java 1303891 2012-03-22 17:04:12Z vhennebert $ */

package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.CompoundDatatype;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.util.CompareUtil;

/**
 * Superclass for properties wrapping a LengthPair value
 */
public class LengthPairProperty extends Property implements CompoundDatatype {
    private Property ipd;
    private Property bpd;

    /**
     * Inner class for creating instances of LengthPairProperty
     */
    public static class Maker extends CompoundPropertyMaker {

        /**
         * @param propId
         *            name of property for which this Maker should be created
         */
        public Maker(final int propId) {
            super(propId);
        }

        /**
         * Create a new empty instance of LengthPairProperty.
         * 
         * @return the new instance.
         */
        @Override
        public Property makeNewProperty() {
            return new LengthPairProperty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Property convertProperty(final Property p,
                final PropertyList propertyList, final FObj fo)
                throws PropertyException {
            if (p instanceof LengthPairProperty) {
                return p;
            }
            return super.convertProperty(p, propertyList, fo);
        }
    }

    /**
     * Creates a new LengthPairProperty with empty values.
     */
    public LengthPairProperty() {
        super();
    }

    /**
     * Creates a new LengthPairProperty.
     * 
     * @param ipd
     *            inline-progression-dimension
     * @param bpd
     *            block-progression-dimension
     */
    public LengthPairProperty(final Property ipd, final Property bpd) {
        this();
        this.ipd = ipd;
        this.bpd = bpd;
    }

    /**
     * Creates a new LengthPairProperty which sets both bpd and ipd to the same
     * value.
     * 
     * @param len
     *            length for both dimensions
     */
    public LengthPairProperty(final Property len) {
        this(len, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setComponent(final int cmpId, final Property cmpnValue,
            final boolean bIsDefault) {
        if (cmpId == CP_BLOCK_PROGRESSION_DIRECTION) {
            this.bpd = cmpnValue;
        } else if (cmpId == CP_INLINE_PROGRESSION_DIRECTION) {
            this.ipd = cmpnValue;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property getComponent(final int cmpId) {
        if (cmpId == CP_BLOCK_PROGRESSION_DIRECTION) {
            return getBPD();
        } else if (cmpId == CP_INLINE_PROGRESSION_DIRECTION) {
            return getIPD();
        } else {
            return null; // SHOULDN'T HAPPEN
        }
    }

    /**
     * @return Property holding the ipd length
     */
    public Property getIPD() {
        return this.ipd;
    }

    /**
     * @return Property holding the bpd length
     */
    public Property getBPD() {
        return this.bpd;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "LengthPair[" + "ipd:" + getIPD().getObject() + ", bpd:"
                + getBPD().getObject() + "]";
    }

    /**
     * @return this.lengthPair
     */
    @Override
    public LengthPairProperty getLengthPair() {
        return this;
    }

    /**
     * @return this.lengthPair cast as an Object
     */
    @Override
    public Object getObject() {
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + CompareUtil.getHashCode(this.bpd);
        result = prime * result + CompareUtil.getHashCode(this.ipd);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LengthPairProperty)) {
            return false;
        }
        final LengthPairProperty other = (LengthPairProperty) obj;
        return CompareUtil.equal(this.bpd, other.bpd)
                && CompareUtil.equal(this.ipd, other.ipd);
    }
}
