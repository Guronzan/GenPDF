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

/* $Id: LineHeightPropertyMaker.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.LengthBase;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;

/**
 * A maker which calculates the line-height property. This property maker is
 * special because line-height inherits the specified value, instead of the
 * computed value. So when a line-height is create based on an attribute, the
 * specified value is stored in the property and in compute() the stored
 * specified value of the nearest specified is used to recalculate the
 * line-height.
 */

public class LineHeightPropertyMaker extends SpaceProperty.Maker {
    /**
     * Create a maker for line-height.
     * 
     * @param propId
     *            the is for linehight.
     */
    public LineHeightPropertyMaker(final int propId) {
        super(propId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property make(final PropertyList propertyList, final String value,
            final FObj fo) throws PropertyException {
        /*
         * if value was specified as a number/length/percentage then
         * conditionality and precedence components are overridden
         */
        final Property p = super.make(propertyList, value, fo);
        p.getSpace().setConditionality(
                EnumProperty.getInstance(Constants.EN_RETAIN, "RETAIN"), true);
        p.getSpace().setPrecedence(
                EnumProperty.getInstance(Constants.EN_FORCE, "FORCE"), true);
        return p;
    }

    /**
     * Recalculate the line-height value based on the nearest specified value.
     * {@inheritDoc}
     */
    @Override
    protected Property compute(final PropertyList propertyList)
            throws PropertyException {
        // recalculate based on last specified value
        // Climb up propertylist and find last spec'd value
        final Property specProp = propertyList.getNearestSpecified(this.propId);
        if (specProp != null) {
            final String specVal = specProp.getSpecifiedValue();
            if (specVal != null) {
                return make(propertyList, specVal, propertyList.getParentFObj());
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property convertProperty(Property p,
            final PropertyList propertyList, final FObj fo)
            throws PropertyException {
        final Numeric numval = p.getNumeric();
        if (numval != null && numval.getDimension() == 0) {
            if (getPercentBase(propertyList) instanceof LengthBase) {
                final Length base = ((LengthBase) getPercentBase(propertyList))
                        .getBaseLength();
                if (base != null && base.isAbsolute()) {
                    p = FixedLength.getInstance(numval.getNumericValue()
                            * base.getNumericValue());
                } else {
                    p = new PercentLength(numval.getNumericValue(),
                            getPercentBase(propertyList));
                }
            }
            final Property spaceProp = super.convertProperty(p, propertyList,
                    fo);
            spaceProp
                    .setSpecifiedValue(String.valueOf(numval.getNumericValue()));
            return spaceProp;
        }
        return super.convertProperty(p, propertyList, fo);
    }
}
