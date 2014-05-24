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

/* $Id: CommonMarginBlock.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Store all common margin properties for blocks. See Sec. 7.10 of the XSL-FO
 * Standard. Public "structure" allows direct member access.
 */
public class CommonMarginBlock {
    /**
     * The "margin-top" property.
     */
    public Length marginTop; // CSOK: VisibilityModifier

    /**
     * The "margin-bottom" property.
     */
    public Length marginBottom; // CSOK: VisibilityModifier

    /**
     * The "margin-left" property.
     */
    public Length marginLeft; // CSOK: VisibilityModifier

    /**
     * The "margin-right" property.
     */
    public Length marginRight; // CSOK: VisibilityModifier

    /**
     * The "space-before" property.
     */
    public SpaceProperty spaceBefore; // CSOK: VisibilityModifier

    /**
     * The "space-after" property.
     */
    public SpaceProperty spaceAfter; // CSOK: VisibilityModifier

    /**
     * The "start-indent" property.
     */
    public Length startIndent; // CSOK: VisibilityModifier

    /**
     * The "end-indent" property.
     */
    public Length endIndent; // CSOK: VisibilityModifier

    /**
     * Create a CommonMarginBlock object.
     * 
     * @param pList
     *            The PropertyList with propery values.
     * @throws PropertyException
     *             if a property exception occurs
     */
    public CommonMarginBlock(final PropertyList pList) throws PropertyException {
        this.marginTop = pList.get(Constants.PR_MARGIN_TOP).getLength();
        this.marginBottom = pList.get(Constants.PR_MARGIN_BOTTOM).getLength();
        this.marginLeft = pList.get(Constants.PR_MARGIN_LEFT).getLength();
        this.marginRight = pList.get(Constants.PR_MARGIN_RIGHT).getLength();

        this.spaceBefore = pList.get(Constants.PR_SPACE_BEFORE).getSpace();
        this.spaceAfter = pList.get(Constants.PR_SPACE_AFTER).getSpace();

        this.startIndent = pList.get(Constants.PR_START_INDENT).getLength();
        this.endIndent = pList.get(Constants.PR_END_INDENT).getLength();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "CommonMarginBlock:\n" + "Margins (top, bottom, left, right): ("
                + this.marginTop + ", " + this.marginBottom + ", "
                + this.marginLeft + ", " + this.marginRight + ")\n"
                + "Space (before, after): (" + this.spaceBefore + ", "
                + this.spaceAfter + ")\n" + "Indents (start, end): ("
                + this.startIndent + ", " + this.endIndent + ")\n";
    }

}
