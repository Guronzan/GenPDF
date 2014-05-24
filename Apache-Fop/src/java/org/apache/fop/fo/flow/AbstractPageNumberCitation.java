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

/* $Id: AbstractPageNumberCitation.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.fo.flow;

import java.awt.Color;
import java.util.Stack;

import org.apache.fop.accessibility.StructureTreeElement;
import org.apache.fop.apps.FOPException;
import org.apache.fop.complexscripts.bidi.DelimitedTextRange;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonFont;
import org.apache.fop.fo.properties.CommonTextDecoration;
import org.apache.fop.fo.properties.SpaceProperty;
import org.apache.fop.fo.properties.StructureTreeElementHolder;
import org.apache.fop.util.CharUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Common base class for the <a
 * href="http://www.w3.org/TR/xsl/#fo_page-number-citation">
 * <code>fo:page-number-citation</code></a> and <a
 * href="http://www.w3.org/TR/xsl/#fo_page-number-citation-last">
 * <code>fo:page-number-citation-last</code></a> objects.
 */
public abstract class AbstractPageNumberCitation extends FObj implements
        StructureTreeElementHolder, CommonAccessibilityHolder {

    // The value of properties relevant for fo:page-number-citation(-last).
    private CommonAccessibility commonAccessibility;
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private CommonFont commonFont;
    private Length alignmentAdjust;
    private int alignmentBaseline;
    private Length baselineShift;
    private int dominantBaseline;
    private StructureTreeElement structureTreeElement;
    // private ToBeImplementedProperty letterSpacing;
    private SpaceProperty lineHeight;
    private String refId;
    /** Holds the text decoration values. May be null */
    private CommonTextDecoration textDecoration;
    // private ToBeImplementedProperty textShadow;
    // Unused but valid items, commented out for performance:
    // private CommonAural commonAural;
    // private CommonMarginInline commonMarginInline;
    // private CommonRelativePosition commonRelativePosition;
    // private KeepProperty keepWithNext;
    // private KeepProperty keepWithPrevious;
    // private int scoreSpaces;
    // private Length textAltitude;
    // private Length textDepth;
    // private int textTransform;
    // private int visibility;
    // private SpaceProperty wordSpacing;
    // private int wrapOption;
    // End of property values

    // Properties which are not explicitely listed but are still applicable
    private Color color;

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public AbstractPageNumberCitation(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.commonAccessibility = CommonAccessibility.getInstance(pList);
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        this.commonFont = pList.getFontProps();
        this.alignmentAdjust = pList.get(PR_ALIGNMENT_ADJUST).getLength();
        this.alignmentBaseline = pList.get(PR_ALIGNMENT_BASELINE).getEnum();
        this.baselineShift = pList.get(PR_BASELINE_SHIFT).getLength();
        this.dominantBaseline = pList.get(PR_DOMINANT_BASELINE).getEnum();
        // letterSpacing = pList.get(PR_LETTER_SPACING);
        this.lineHeight = pList.get(PR_LINE_HEIGHT).getSpace();
        this.refId = pList.get(PR_REF_ID).getString();
        this.textDecoration = pList.getTextDecorationProps();
        // textShadow = pList.get(PR_TEXT_SHADOW);

        // implicit properties
        this.color = pList.get(Constants.PR_COLOR).getColor(getUserAgent());
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList pList)
            throws FOPException {
        super.processNode(elementName, locator, attlist, pList);
        if (!inMarker() && (this.refId == null || "".equals(this.refId))) {
            missingPropertyError("ref-id");
        }
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: empty
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CommonAccessibility getCommonAccessibility() {
        return this.commonAccessibility;
    }

    /** @return the {@link CommonFont} */
    public CommonFont getCommonFont() {
        return this.commonFont;
    }

    /** @return the "color" property. */
    public Color getColor() {
        return this.color;
    }

    /** @return the "text-decoration" property. */
    public CommonTextDecoration getTextDecoration() {
        return this.textDecoration;
    }

    @Override
    public void setStructureTreeElement(
            final StructureTreeElement structureTreeElement) {
        this.structureTreeElement = structureTreeElement;
    }

    /** {@inheritDoc} */
    @Override
    public StructureTreeElement getStructureTreeElement() {
        return this.structureTreeElement;
    }

    /** @return the "alignment-adjust" property */
    public Length getAlignmentAdjust() {
        return this.alignmentAdjust;
    }

    /** @return the "alignment-baseline" property */
    public int getAlignmentBaseline() {
        return this.alignmentBaseline;
    }

    /** @return the "baseline-shift" property */
    public Length getBaselineShift() {
        return this.baselineShift;
    }

    /** @return the "dominant-baseline" property */
    public int getDominantBaseline() {
        return this.dominantBaseline;
    }

    /** @return the {@link CommonBorderPaddingBackground} */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /** @return the "line-height" property */
    public SpaceProperty getLineHeight() {
        return this.lineHeight;
    }

    /** @return the "ref-id" property. */
    public String getRefId() {
        return this.refId;
    }

    @Override
    public boolean isDelimitedTextRangeBoundary(final int boundary) {
        return false;
    }

    @Override
    protected Stack collectDelimitedTextRanges(final Stack ranges,
            final DelimitedTextRange currentRange) {
        if (currentRange != null) {
            currentRange.append(CharUtilities.OBJECT_REPLACEMENT_CHARACTER,
                    this);
        }
        return ranges;
    }

}
