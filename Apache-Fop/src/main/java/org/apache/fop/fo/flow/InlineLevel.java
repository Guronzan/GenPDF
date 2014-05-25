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

/* $Id: InlineLevel.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.fo.flow;

import java.awt.Color;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObjMixed;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonFont;
import org.apache.fop.fo.properties.CommonMarginInline;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.SpaceProperty;

/**
 * Class modelling the commonalities of several inline-level formatting objects.
 */
public abstract class InlineLevel extends FObjMixed implements
        CommonAccessibilityHolder {

    // The value of FO traits (refined properties) that apply to inline level
    // FOs.
    private CommonAccessibility commonAccessibility;
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private CommonMarginInline commonMarginInline;
    private CommonFont commonFont;
    private Color color;
    private KeepProperty keepWithNext;
    private KeepProperty keepWithPrevious;
    private SpaceProperty lineHeight;

    // End of trait values

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    protected InlineLevel(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.commonAccessibility = CommonAccessibility.getInstance(pList);
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        this.commonMarginInline = pList.getMarginInlineProps();
        this.commonFont = pList.getFontProps();
        this.color = pList.get(PR_COLOR).getColor(getUserAgent());
        this.keepWithNext = pList.get(PR_KEEP_WITH_NEXT).getKeep();
        this.keepWithPrevious = pList.get(PR_KEEP_WITH_PREVIOUS).getKeep();
        this.lineHeight = pList.get(PR_LINE_HEIGHT).getSpace();
    }

    /** {@inheritDoc} */
    @Override
    public CommonAccessibility getCommonAccessibility() {
        return this.commonAccessibility;
    }

    /** @return the {@link CommonMarginInline} */
    public CommonMarginInline getCommonMarginInline() {
        return this.commonMarginInline;
    }

    /** @return the {@link CommonBorderPaddingBackground} */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /** @return the {@link CommonFont} */
    public CommonFont getCommonFont() {
        return this.commonFont;
    }

    /** @return the "color" trait */
    public Color getColor() {
        return this.color;
    }

    /** @return the "line-height" trait */
    public SpaceProperty getLineHeight() {
        return this.lineHeight;
    }

    /** @return the "keep-with-next" trait */
    public KeepProperty getKeepWithNext() {
        return this.keepWithNext;
    }

    /** @return the "keep-with-previous" trait */
    public KeepProperty getKeepWithPrevious() {
        return this.keepWithPrevious;
    }

    @Override
    public boolean isDelimitedTextRangeBoundary(final int boundary) {
        return false;
    }

}
