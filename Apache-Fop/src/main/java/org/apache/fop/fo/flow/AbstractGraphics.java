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

/* $Id: AbstractGraphics.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.fo.flow;

import java.util.Stack;

import org.apache.fop.accessibility.StructureTreeElement;
import org.apache.fop.apps.FOPException;
import org.apache.fop.complexscripts.bidi.DelimitedTextRange;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.GraphicsProperties;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.apache.fop.fo.properties.SpaceProperty;
import org.apache.fop.fo.properties.StructureTreeElementHolder;
import org.apache.fop.util.CharUtilities;

/**
 * Common base class for the <a
 * href="http://www.w3.org/TR/xsl/#fo_instream-foreign-object">
 * <code>fo:instream-foreign-object</code></a> and <a
 * href="http://www.w3.org/TR/xsl/#fo_external-graphic">
 * <code>fo:external-graphic</code></a> flow formatting objects.
 */
public abstract class AbstractGraphics extends FObj implements
        GraphicsProperties, StructureTreeElementHolder,
        CommonAccessibilityHolder {

    // The value of properties relevant for fo:instream-foreign-object
    // and external-graphics.
    private CommonAccessibility commonAccessibility;
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private Length alignmentAdjust;
    private int alignmentBaseline;
    private Length baselineShift;
    private LengthRangeProperty blockProgressionDimension;
    // private ToBeImplementedProperty clip;
    private Length contentHeight;
    private Length contentWidth;
    private int displayAlign;
    private int dominantBaseline;
    private Length height;
    private String id;
    private LengthRangeProperty inlineProgressionDimension;
    private KeepProperty keepWithNext;
    private KeepProperty keepWithPrevious;
    private SpaceProperty lineHeight;
    private int overflow;
    private int scaling;
    private int textAlign;
    private Length width;
    private String altText;
    private StructureTreeElement structureTreeElement;

    // Unused but valid items, commented out for performance:
    // private CommonAccessibility commonAccessibility;
    // private CommonAural commonAural;
    // private CommonMarginInline commonMarginInline;
    // private CommonRelativePosition commonRelativePosition;
    // private String contentType;
    // private int scalingMethod;
    // End of property values

    /**
     * constructs an instream-foreign-object object (called by Maker).
     *
     * @param parent
     *            the parent formatting object
     */
    public AbstractGraphics(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.commonAccessibility = CommonAccessibility.getInstance(pList);
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        this.alignmentAdjust = pList.get(PR_ALIGNMENT_ADJUST).getLength();
        this.alignmentBaseline = pList.get(PR_ALIGNMENT_BASELINE).getEnum();
        this.baselineShift = pList.get(PR_BASELINE_SHIFT).getLength();
        this.blockProgressionDimension = pList.get(
                PR_BLOCK_PROGRESSION_DIMENSION).getLengthRange();
        // clip = pList.get(PR_CLIP);
        this.contentHeight = pList.get(PR_CONTENT_HEIGHT).getLength();
        this.contentWidth = pList.get(PR_CONTENT_WIDTH).getLength();
        this.displayAlign = pList.get(PR_DISPLAY_ALIGN).getEnum();
        this.dominantBaseline = pList.get(PR_DOMINANT_BASELINE).getEnum();
        this.height = pList.get(PR_HEIGHT).getLength();
        this.id = pList.get(PR_ID).getString();
        this.inlineProgressionDimension = pList.get(
                PR_INLINE_PROGRESSION_DIMENSION).getLengthRange();
        this.keepWithNext = pList.get(PR_KEEP_WITH_NEXT).getKeep();
        this.keepWithPrevious = pList.get(PR_KEEP_WITH_PREVIOUS).getKeep();
        this.lineHeight = pList.get(PR_LINE_HEIGHT).getSpace();
        this.overflow = pList.get(PR_OVERFLOW).getEnum();
        this.scaling = pList.get(PR_SCALING).getEnum();
        this.textAlign = pList.get(PR_TEXT_ALIGN).getEnum();
        this.width = pList.get(PR_WIDTH).getLength();
        if (getUserAgent().isAccessibilityEnabled()) {
            this.altText = pList.get(PR_X_ALT_TEXT).getString();
            if (this.altText.equals("")) {
                getFOValidationEventProducer().altTextMissing(this,
                        getLocalName(), getLocator());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CommonAccessibility getCommonAccessibility() {
        return this.commonAccessibility;
    }

    /**
     * @return the "id" property.
     */
    @Override
    public String getId() {
        return this.id;
    }

    /** @return the {@link CommonBorderPaddingBackground} */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /** @return the "line-height" property */
    public SpaceProperty getLineHeight() {
        return this.lineHeight;
    }

    /** @return the "inline-progression-dimension" property */
    @Override
    public LengthRangeProperty getInlineProgressionDimension() {
        return this.inlineProgressionDimension;
    }

    /** @return the "block-progression-dimension" property */
    @Override
    public LengthRangeProperty getBlockProgressionDimension() {
        return this.blockProgressionDimension;
    }

    /** @return the "height" property */
    @Override
    public Length getHeight() {
        return this.height;
    }

    /** @return the "width" property */
    @Override
    public Length getWidth() {
        return this.width;
    }

    /** @return the "content-height" property */
    @Override
    public Length getContentHeight() {
        return this.contentHeight;
    }

    /** @return the "content-width" property */
    @Override
    public Length getContentWidth() {
        return this.contentWidth;
    }

    /** @return the "scaling" property */
    @Override
    public int getScaling() {
        return this.scaling;
    }

    /** @return the "overflow" property */
    @Override
    public int getOverflow() {
        return this.overflow;
    }

    /** {@inheritDoc} */
    @Override
    public int getDisplayAlign() {
        return this.displayAlign;
    }

    /** {@inheritDoc} */
    @Override
    public int getTextAlign() {
        return this.textAlign;
    }

    /** @return the "alignment-adjust" property */
    public Length getAlignmentAdjust() {
        if (this.alignmentAdjust.getEnum() == EN_AUTO) {
            final Length intrinsicAlignmentAdjust = getIntrinsicAlignmentAdjust();
            if (intrinsicAlignmentAdjust != null) {
                return intrinsicAlignmentAdjust;
            }
        }
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

    /** @return the "keep-with-next" property */
    public KeepProperty getKeepWithNext() {
        return this.keepWithNext;
    }

    /** @return the "keep-with-previous" property */
    public KeepProperty getKeepWithPrevious() {
        return this.keepWithPrevious;
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

    /** @return the alternative text property. */
    public String getAltText() {
        return this.altText;
    }

    /** @return the graphic's intrinsic width in millipoints */
    public abstract int getIntrinsicWidth();

    /** @return the graphic's intrinsic height in millipoints */
    public abstract int getIntrinsicHeight();

    /** @return the graphic's intrinsic alignment-adjust */
    public abstract Length getIntrinsicAlignmentAdjust();

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
