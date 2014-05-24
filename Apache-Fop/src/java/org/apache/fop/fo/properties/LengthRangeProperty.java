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

/* $Id: LengthRangeProperty.java 1303891 2012-03-22 17:04:12Z vhennebert $ */

package org.apache.fop.fo.properties;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.datatypes.CompoundDatatype;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.util.CompareUtil;

/**
 * Superclass for properties that contain LengthRange values
 */
@Slf4j
public class LengthRangeProperty extends Property implements CompoundDatatype {
    private Property minimum;
    private Property optimum;
    private Property maximum;
    private static final int MINSET = 1;
    private static final int OPTSET = 2;
    private static final int MAXSET = 4;
    private int bfSet = 0; // bit field
    private boolean consistent = false;

    /**
     * Converts this <code>LengthRangeProperty</code> to a
     * <code>MinOptMax</code>.
     *
     * @param context
     *            Percentage evaluation context
     * @return the requested MinOptMax instance
     */
    public MinOptMax toMinOptMax(final PercentBaseContext context) {
        final int min = getMinimum(context).isAuto() ? 0 : getMinimum(context)
                .getLength().getValue(context);
        final int opt = getOptimum(context).isAuto() ? min
                : getOptimum(context).getLength().getValue(context);
        final int max = getMaximum(context).isAuto() ? Integer.MAX_VALUE
                : getMaximum(context).getLength().getValue(context);
        return MinOptMax.getInstance(min, opt, max);
    }

    /**
     * Inner class for a Maker for LengthProperty objects
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
         * Create a new empty instance of LengthRangeProperty.
         *
         * @return the new instance.
         */
        @Override
        public Property makeNewProperty() {
            return new LengthRangeProperty();
        }

        private boolean isNegativeLength(final Length len) {
            return len instanceof PercentLength
                    && ((PercentLength) len).getPercentage() < 0
                    || len.isAbsolute() && len.getValue() < 0;
        }

        /** {@inheritDoc} */
        @Override
        public Property convertProperty(Property p,
                final PropertyList propertyList, final FObj fo)
                        throws PropertyException {

            if (p instanceof LengthRangeProperty) {
                return p;
            }

            if (this.propId == PR_BLOCK_PROGRESSION_DIMENSION
                    || this.propId == PR_INLINE_PROGRESSION_DIMENSION) {
                final Length len = p.getLength();
                if (len != null) {
                    if (isNegativeLength(len)) {
                        log.warn(FONode.decorateWithContextInfo(
                                "Replaced negative value (" + len + ") for "
                                        + getName() + " with 0mpt", fo));
                        p = FixedLength.ZERO_FIXED_LENGTH;
                    }
                }
            }

            return super.convertProperty(p, propertyList, fo);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Property setSubprop(final Property baseProperty,
                final int subpropertyId, final Property subproperty) {
            final CompoundDatatype val = (CompoundDatatype) baseProperty
                    .getObject();
            if (this.propId == PR_BLOCK_PROGRESSION_DIMENSION
                    || this.propId == PR_INLINE_PROGRESSION_DIMENSION) {
                final Length len = subproperty.getLength();
                if (len != null) {
                    if (isNegativeLength(len)) {
                        log.warn("Replaced negative value (" + len + ") for "
                                + getName() + " with 0mpt");
                        val.setComponent(subpropertyId,
                                FixedLength.ZERO_FIXED_LENGTH, false);
                        return baseProperty;
                    }
                }
            }
            val.setComponent(subpropertyId, subproperty, false);
            return baseProperty;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setComponent(final int cmpId, final Property cmpnValue,
            final boolean bIsDefault) {
        if (cmpId == CP_MINIMUM) {
            setMinimum(cmpnValue, bIsDefault);
        } else if (cmpId == CP_OPTIMUM) {
            setOptimum(cmpnValue, bIsDefault);
        } else if (cmpId == CP_MAXIMUM) {
            setMaximum(cmpnValue, bIsDefault);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property getComponent(final int cmpId) {
        if (cmpId == CP_MINIMUM) {
            return getMinimum(null);
        } else if (cmpId == CP_OPTIMUM) {
            return getOptimum(null);
        } else if (cmpId == CP_MAXIMUM) {
            return getMaximum(null);
        } else {
            return null; // SHOULDN'T HAPPEN
        }
    }

    /**
     * Set minimum value to min.
     *
     * @param minimum
     *            A Length value specifying the minimum value for this
     *            LengthRange.
     * @param bIsDefault
     *            If true, this is set as a "default" value and not a
     *            user-specified explicit value.
     */
    protected void setMinimum(final Property minimum, final boolean bIsDefault) {
        this.minimum = minimum;
        if (!bIsDefault) {
            this.bfSet |= MINSET;
        }
        this.consistent = false;
    }

    /**
     * Set maximum value to max if it is >= optimum or optimum isn't set.
     *
     * @param max
     *            A Length value specifying the maximum value for this
     * @param bIsDefault
     *            If true, this is set as a "default" value and not a
     *            user-specified explicit value.
     */
    protected void setMaximum(final Property max, final boolean bIsDefault) {
        this.maximum = max;
        if (!bIsDefault) {
            this.bfSet |= MAXSET;
        }
        this.consistent = false;
    }

    /**
     * Set the optimum value.
     *
     * @param opt
     *            A Length value specifying the optimum value for this
     * @param bIsDefault
     *            If true, this is set as a "default" value and not a
     *            user-specified explicit value.
     */
    protected void setOptimum(final Property opt, final boolean bIsDefault) {
        this.optimum = opt;
        if (!bIsDefault) {
            this.bfSet |= OPTSET;
        }
        this.consistent = false;
    }

    // Minimum is prioritaire, if explicit
    private void checkConsistency(final PercentBaseContext context) {
        if (this.consistent) {
            return;
        }
        if (context == null) {
            return;
        }
        // Make sure max >= min
        // Must also control if have any allowed enum values!

        if (!this.minimum.isAuto()
                && !this.maximum.isAuto()
                && this.minimum.getLength().getValue(context) > this.maximum
                .getLength().getValue(context)) {
            if ((this.bfSet & MINSET) != 0) {
                // if minimum is explicit, force max to min
                if ((this.bfSet & MAXSET) != 0) {
                    // Warning: min>max, resetting max to min
                    log.error("forcing max to min in LengthRange");
                }
                this.maximum = this.minimum;
            } else {
                this.minimum = this.maximum; // minimum was default value
            }
        }
        // Now make sure opt <= max and opt >= min
        if (!this.optimum.isAuto()
                && !this.maximum.isAuto()
                && this.optimum.getLength().getValue(context) > this.maximum
                .getLength().getValue(context)) {
            if ((this.bfSet & OPTSET) != 0) {
                if ((this.bfSet & MAXSET) != 0) {
                    // Warning: opt > max, resetting opt to max
                    log.error("forcing opt to max in LengthRange");
                    this.optimum = this.maximum;
                } else {
                    this.maximum = this.optimum; // maximum was default value
                }
            } else {
                // opt is default and max is explicit or default
                this.optimum = this.maximum;
            }
        } else if (!this.optimum.isAuto()
                && !this.minimum.isAuto()
                && this.optimum.getLength().getValue(context) < this.minimum
                .getLength().getValue(context)) {
            if ((this.bfSet & MINSET) != 0) {
                // if minimum is explicit, force opt to min
                if ((this.bfSet & OPTSET) != 0) {
                    log.error("forcing opt to min in LengthRange");
                }
                this.optimum = this.minimum;
            } else {
                this.minimum = this.optimum; // minimum was default value
            }
        }

        this.consistent = true;
    }

    /**
     * @param context
     *            Percentage evaluation context
     * @return minimum length
     */
    public Property getMinimum(final PercentBaseContext context) {
        checkConsistency(context);
        return this.minimum;
    }

    /**
     * @param context
     *            Percentage evaluation context
     * @return maximum length
     */
    public Property getMaximum(final PercentBaseContext context) {
        checkConsistency(context);
        return this.maximum;
    }

    /**
     * @param context
     *            Percentage evaluation context
     * @return optimum length
     */
    public Property getOptimum(final PercentBaseContext context) {
        checkConsistency(context);
        return this.optimum;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "LengthRange[" + "min:" + getMinimum(null).getObject()
                + ", max:" + getMaximum(null).getObject() + ", opt:"
                + getOptimum(null).getObject() + "]";
    }

    /**
     * @return this.lengthRange
     */
    @Override
    public LengthRangeProperty getLengthRange() {
        return this;
    }

    /**
     * @return this.lengthRange cast as an Object
     */
    @Override
    public Object getObject() {
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.bfSet;
        result = prime * result + (this.consistent ? 1231 : 1237);
        result = prime * result + CompareUtil.getHashCode(this.minimum);
        result = prime * result + CompareUtil.getHashCode(this.optimum);
        result = prime * result + CompareUtil.getHashCode(this.maximum);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LengthRangeProperty)) {
            return false;
        }
        final LengthRangeProperty other = (LengthRangeProperty) obj;
        return this.bfSet == other.bfSet && this.consistent == other.consistent
                && CompareUtil.equal(this.minimum, other.minimum)
                && CompareUtil.equal(this.optimum, other.optimum)
                && CompareUtil.equal(this.maximum, other.maximum);
    }
}
