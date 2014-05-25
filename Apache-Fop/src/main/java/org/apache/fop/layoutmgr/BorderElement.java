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

/* $Id: BorderElement.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.layoutmgr;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.properties.CondLengthProperty;
import org.apache.fop.traits.MinOptMax;

/**
 * This represents an unresolved border element.
 */
@Slf4j
public class BorderElement extends BorderOrPaddingElement {

    /**
     * Main constructor
     *
     * @param position
     *            the Position instance needed by the addAreas stage of the LMs.
     * @param side
     *            the side to which this space element applies.
     * @param condLength
     *            the length-conditional property for a border or padding
     *            specification
     * @param isFirst
     *            true if this is a padding- or border-before of the first area
     *            generated.
     * @param isLast
     *            true if this is a padding- or border-after of the last area
     *            generated.
     * @param context
     *            the property evaluation context
     */
    public BorderElement(final Position position,
            final CondLengthProperty condLength, final RelSide side,
            final boolean isFirst, final boolean isLast,
            final PercentBaseContext context) {
        super(position, condLength, side, isFirst, isLast, context);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyLayoutManager(final MinOptMax effectiveLength) {
        final LayoutManager lm = getOriginatingLayoutManager();
        if (lm instanceof ConditionalElementListener) {
            ((ConditionalElementListener) lm).notifyBorder(getSide(),
                    effectiveLength);
        } else {
            log.warn("Cannot notify LM. It does not implement ConditionalElementListener: "
                    + lm.getClass().getName());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Border[");
        sb.append(super.toString());
        sb.append("]");
        return sb.toString();
    }

}