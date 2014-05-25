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

/* $Id: DimensionPropertyMaker.java 1297284 2012-03-05 23:29:29Z gadams $ */

package org.apache.fop.fo.properties;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;

/**
 * <p>
 * Dimensioned property maker.
 * </p>
 */
public class DimensionPropertyMaker extends CorrespondingPropertyMaker {

    private int[][] extraCorresponding = null;

    /**
     * Instantiate a dimension property maker.
     * 
     * @param baseMaker
     *            the base property maker
     */
    public DimensionPropertyMaker(final PropertyMaker baseMaker) {
        super(baseMaker);
    }

    /**
     * Set extra correspondences.
     * 
     * @param extraCorresponding
     *            an array of four element integer arrays
     */
    public void setExtraCorresponding(final int[][] extraCorresponding) {
        if (extraCorresponding == null) {
            throw new NullPointerException();
        }
        for (int i = 0; i < extraCorresponding.length; i++) {
            final int[] eca = extraCorresponding[i];
            if (eca == null || eca.length != 4) {
                throw new IllegalArgumentException("bad sub-array @ [" + i
                        + "]");
            }
        }
        this.extraCorresponding = extraCorresponding;
    }

    /**
     * Determine if corresponding property is forced.
     * 
     * @param propertyList
     *            the property list to use
     * @return true if it is forced
     */
    @Override
    public boolean isCorrespondingForced(final PropertyList propertyList) {
        if (super.isCorrespondingForced(propertyList)) {
            return true;
        }
        for (int i = 0; i < this.extraCorresponding.length; i++) {
            final int wmcorr = this.extraCorresponding[i][0]; // propertyList.getWritingMode()];
            if (propertyList.getExplicit(wmcorr) != null) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Property compute(final PropertyList propertyList)
            throws PropertyException {
        // Based on [width|height]
        Property p = super.compute(propertyList);
        if (p == null) {
            p = this.baseMaker.make(propertyList);
        }

        // Based on min-[width|height]
        int wmcorr = propertyList.selectFromWritingMode(
                this.extraCorresponding[0][0], this.extraCorresponding[0][1],
                this.extraCorresponding[0][2], this.extraCorresponding[0][3]);
        Property subprop = propertyList.getExplicitOrShorthand(wmcorr);
        if (subprop != null) {
            this.baseMaker.setSubprop(p, Constants.CP_MINIMUM, subprop);
        }

        // Based on max-[width|height]
        wmcorr = propertyList.selectFromWritingMode(
                this.extraCorresponding[1][0], this.extraCorresponding[1][1],
                this.extraCorresponding[1][2], this.extraCorresponding[1][3]);
        subprop = propertyList.getExplicitOrShorthand(wmcorr);
        // TODO: Don't set when NONE.
        if (subprop != null) {
            this.baseMaker.setSubprop(p, Constants.CP_MAXIMUM, subprop);
        }

        return p;
    }
}
