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

/* $Id: LengthBase.java 1303891 2012-03-22 17:04:12Z vhennebert $ */

package org.apache.fop.datatypes;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.util.CompareUtil;

/**
 * Models a length which can be used as a factor in a percentage length
 * calculation
 */
@Slf4j
public class LengthBase implements PercentBase {
    // Standard kinds of percent-based length
    /** constant for a custom percent-based length */
    public static final int CUSTOM_BASE = 0;
    /** constant for a font-size percent-based length */
    public static final int FONTSIZE = 1;
    /** constant for an inh font-size percent-based length */
    public static final int INH_FONTSIZE = 2;
    /** constant for a containing box percent-based length */
    public static final int PARENT_AREA_WIDTH = 3;
    /** constant for a containing refarea percent-based length */
    public static final int CONTAINING_REFAREA_WIDTH = 4;
    /** constant for a containing block percent-based length */
    public static final int CONTAINING_BLOCK_WIDTH = 5;
    /** constant for a containing block percent-based length */
    public static final int CONTAINING_BLOCK_HEIGHT = 6;
    /** constant for a image intrinsic percent-based length */
    public static final int IMAGE_INTRINSIC_WIDTH = 7;
    /** constant for a image intrinsic percent-based length */
    public static final int IMAGE_INTRINSIC_HEIGHT = 8;
    /** constant for a image background position horizontal percent-based length */
    public static final int IMAGE_BACKGROUND_POSITION_HORIZONTAL = 9;
    /** constant for a image background position vertical percent-based length */
    public static final int IMAGE_BACKGROUND_POSITION_VERTICAL = 10;
    /** constant for a table-unit-based length */
    public static final int TABLE_UNITS = 11;
    /** constant for a alignment adjust percent-based length */
    public static final int ALIGNMENT_ADJUST = 12;

    /**
     * The FO for which this property is to be calculated.
     */
    protected/* final */FObj fobj;

    /**
     * One of the defined types of LengthBase
     */
    private final/* final */int baseType;

    /** For percentages based on other length properties */
    private Length baseLength;

    /**
     * Constructor
     *
     * @param plist
     *            property list for this
     * @param baseType
     *            a constant defining the type of teh percent base
     * @throws PropertyException
     *             In case an problem occurs while evaluating values
     */
    public LengthBase(final PropertyList plist, final int baseType)
            throws PropertyException {
        this.fobj = plist.getFObj();
        this.baseType = baseType;
        switch (baseType) {
        case FONTSIZE:
            this.baseLength = plist.get(Constants.PR_FONT_SIZE).getLength();
            break;
        case INH_FONTSIZE:
            this.baseLength = plist.getInherited(Constants.PR_FONT_SIZE)
            .getLength();
            break;
        default:
            // TODO: pacify CheckStyle
            // throw new RuntimeException();
            break;
        }
    }

    /**
     * @return the dimension of this object (always 1)
     */
    @Override
    public int getDimension() {
        return 1;
    }

    /**
     * @return the base value of this object (always 1.0)
     */
    @Override
    public double getBaseValue() {
        return 1.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseLength(final PercentBaseContext context)
            throws PropertyException {
        int baseLen = 0;
        if (context != null) {
            if (this.baseType == FONTSIZE || this.baseType == INH_FONTSIZE) {
                return this.baseLength.getValue(context);
            }
            baseLen = context.getBaseLength(this.baseType, this.fobj);
        } else {
            log.error("getBaseLength called without context");
        }
        return baseLen;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return super.toString() + "[fo=" + this.fobj + "," + "baseType="
                + this.baseType + "," + "baseLength=" + this.baseLength + "]";
    }

    /** @return the base length as a {@link Length} */
    public Length getBaseLength() {
        return this.baseLength;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + CompareUtil.getHashCode(this.baseLength);
        result = prime * result + this.baseType;
        result = prime * result + CompareUtil.getHashCode(this.fobj);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LengthBase)) {
            return false;
        }
        final LengthBase other = (LengthBase) obj;
        return CompareUtil.equal(this.baseLength, other.baseLength)
                && this.baseType == other.baseType
                && CompareUtil.equal(this.fobj, other.fobj);
    }

}
