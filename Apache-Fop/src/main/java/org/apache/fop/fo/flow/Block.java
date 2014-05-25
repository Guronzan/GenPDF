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

/* $Id: Block.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.fo.flow;

import java.awt.Color;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.CharIterator;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObjMixed;
import org.apache.fop.fo.NullCharIterator;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.BreakPropertySet;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonFont;
import org.apache.fop.fo.properties.CommonHyphenation;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.properties.CommonRelativePosition;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.SpaceProperty;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_block">
 * <code>fo:block object</code></a>.
 */
public class Block extends FObjMixed implements BreakPropertySet,
CommonAccessibilityHolder {

    // used for FO validation
    private boolean blockOrInlineItemFound = false;
    private boolean initialPropertySetFound = false;

    // The value of FO traits (refined properties) that apply to fo:block.
    private CommonAccessibility commonAccessibility;
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private CommonFont commonFont;
    private CommonHyphenation commonHyphenation;
    private CommonMarginBlock commonMarginBlock;
    private CommonRelativePosition commonRelativePosition;
    private int breakAfter;
    private int breakBefore;
    private Color color;
    private int hyphenationKeep;
    private Numeric hyphenationLadderCount;
    private int intrusionDisplace;
    private KeepProperty keepTogether;
    private KeepProperty keepWithNext;
    private KeepProperty keepWithPrevious;
    private Length lastLineEndIndent;
    private int linefeedTreatment;
    private SpaceProperty lineHeight;
    private int lineHeightShiftAdjustment;
    private int lineStackingStrategy;
    private Numeric orphans;
    private int whiteSpaceTreatment;
    private int span;
    private int textAlign;
    private int textAlignLast;
    private Length textIndent;
    private int whiteSpaceCollapse;
    private Numeric widows;
    private int wrapOption;
    private int disableColumnBalancing;

    // Unused but valid items, commented out for performance:
    // private CommonAccessibility commonAccessibility;
    // private CommonAural commonAural;
    // private Length textDepth;
    // private Length textAltitude;
    // private int visibility;
    // End of FO trait values

    /**
     * Base constructor
     *
     * @param parent
     *            FONode that is the parent of this object
     *
     */
    public Block(final FONode parent) {
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
        this.commonHyphenation = pList.getHyphenationProps();
        this.commonMarginBlock = pList.getMarginBlockProps();
        this.commonRelativePosition = pList.getRelativePositionProps();

        this.breakAfter = pList.get(PR_BREAK_AFTER).getEnum();
        this.breakBefore = pList.get(PR_BREAK_BEFORE).getEnum();
        this.color = pList.get(PR_COLOR).getColor(getUserAgent());
        this.hyphenationKeep = pList.get(PR_HYPHENATION_KEEP).getEnum();
        this.hyphenationLadderCount = pList.get(PR_HYPHENATION_LADDER_COUNT)
                .getNumeric();
        this.intrusionDisplace = pList.get(PR_INTRUSION_DISPLACE).getEnum();
        this.keepTogether = pList.get(PR_KEEP_TOGETHER).getKeep();
        this.keepWithNext = pList.get(PR_KEEP_WITH_NEXT).getKeep();
        this.keepWithPrevious = pList.get(PR_KEEP_WITH_PREVIOUS).getKeep();
        this.lastLineEndIndent = pList.get(PR_LAST_LINE_END_INDENT).getLength();
        this.linefeedTreatment = pList.get(PR_LINEFEED_TREATMENT).getEnum();
        this.lineHeight = pList.get(PR_LINE_HEIGHT).getSpace();
        this.lineHeightShiftAdjustment = pList.get(
                PR_LINE_HEIGHT_SHIFT_ADJUSTMENT).getEnum();
        this.lineStackingStrategy = pList.get(PR_LINE_STACKING_STRATEGY)
                .getEnum();
        this.orphans = pList.get(PR_ORPHANS).getNumeric();
        this.whiteSpaceTreatment = pList.get(PR_WHITE_SPACE_TREATMENT)
                .getEnum();
        this.span = pList.get(PR_SPAN).getEnum();
        this.textAlign = pList.get(PR_TEXT_ALIGN).getEnum();
        this.textAlignLast = pList.get(PR_TEXT_ALIGN_LAST).getEnum();
        this.textIndent = pList.get(PR_TEXT_INDENT).getLength();
        this.whiteSpaceCollapse = pList.get(PR_WHITE_SPACE_COLLAPSE).getEnum();
        this.widows = pList.get(PR_WIDOWS).getNumeric();
        this.wrapOption = pList.get(PR_WRAP_OPTION).getEnum();
        this.disableColumnBalancing = pList.get(PR_X_DISABLE_COLUMN_BALANCING)
                .getEnum();
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startBlock(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        super.endOfNode();
        getFOEventHandler().endBlock(this);
    }

    /** {@inheritDoc} */
    @Override
    public CommonAccessibility getCommonAccessibility() {
        return this.commonAccessibility;
    }

    /** @return the {@link CommonMarginBlock} */
    public CommonMarginBlock getCommonMarginBlock() {
        return this.commonMarginBlock;
    }

    /** @return the {@link CommonBorderPaddingBackground} */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /**
     * @return the {@link CommonFont}
     */
    public CommonFont getCommonFont() {
        return this.commonFont;
    }

    /** @return the {@link CommonHyphenation} */
    public CommonHyphenation getCommonHyphenation() {
        return this.commonHyphenation;
    }

    /** @return the "break-after" trait. */
    @Override
    public int getBreakAfter() {
        return this.breakAfter;
    }

    /** @return the "break-before" trait. */
    @Override
    public int getBreakBefore() {
        return this.breakBefore;
    }

    /** @return the "hyphenation-ladder-count" trait. */
    public Numeric getHyphenationLadderCount() {
        return this.hyphenationLadderCount;
    }

    /** @return the "keep-with-next" trait. */
    public KeepProperty getKeepWithNext() {
        return this.keepWithNext;
    }

    /** @return the "keep-with-previous" trait. */
    public KeepProperty getKeepWithPrevious() {
        return this.keepWithPrevious;
    }

    /** @return the "keep-together" trait. */
    public KeepProperty getKeepTogether() {
        return this.keepTogether;
    }

    /** @return the "orphans" trait. */
    public int getOrphans() {
        return this.orphans.getValue();
    }

    /** @return the "widows" trait. */
    public int getWidows() {
        return this.widows.getValue();
    }

    /** @return the "line-stacking-strategy" trait. */
    public int getLineStackingStrategy() {
        return this.lineStackingStrategy;
    }

    /** @return the "color" trait */
    public Color getColor() {
        return this.color;
    }

    /** @return the "line-height" trait */
    public SpaceProperty getLineHeight() {
        return this.lineHeight;
    }

    /** @return the "span" trait */
    public int getSpan() {
        return this.span;
    }

    /** @return the "text-align" trait */
    public int getTextAlign() {
        return this.textAlign;
    }

    /** @return the "text-align-last" trait */
    public int getTextAlignLast() {
        return this.textAlignLast;
    }

    /** @return the "text-indent" trait */
    public Length getTextIndent() {
        return this.textIndent;
    }

    /** @return the "last-line-end-indent" trait */
    public Length getLastLineEndIndent() {
        return this.lastLineEndIndent;
    }

    /** @return the "wrap-option" trait */
    public int getWrapOption() {
        return this.wrapOption;
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: marker* initial-property-set?
     * (#PCDATA|%inline;|%block;)* <br>
     * <i>Additionally: "An fo:bidi-override that is a descendant of an
     * fo:leader or of the fo:inline child of an fo:footnote may not have
     * block-level children, unless it has a nearer ancestor that is an
     * fo:inline-container."</i>
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if ("marker".equals(localName)) {
                if (this.blockOrInlineItemFound || this.initialPropertySetFound) {
                    nodesOutOfOrderError(loc, "fo:marker",
                            "initial-property-set? (#PCDATA|%inline;|%block;)");
                }
            } else if ("initial-property-set".equals(localName)) {
                if (this.initialPropertySetFound) {
                    tooManyNodesError(loc, "fo:initial-property-set");
                } else if (this.blockOrInlineItemFound) {
                    nodesOutOfOrderError(loc, "fo:initial-property-set",
                            "(#PCDATA|%inline;|%block;)");
                } else {
                    this.initialPropertySetFound = true;
                }
            } else if (isBlockOrInlineItem(nsURI, localName)) {
                this.blockOrInlineItemFound = true;
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** @return the "linefeed-treatment" trait */
    public int getLinefeedTreatment() {
        return this.linefeedTreatment;
    }

    /** @return the "white-space-treatment" trait */
    public int getWhitespaceTreatment() {
        return this.whiteSpaceTreatment;
    }

    /** @return the "white-space-collapse" trait */
    public int getWhitespaceCollapse() {
        return this.whiteSpaceCollapse;
    }

    /** @return the {@link CommonRelativePosition} */
    public CommonRelativePosition getCommonRelativePosition() {
        return this.commonRelativePosition;
    }

    /** @return the "hyphenation-keep" trait */
    public int getHyphenationKeep() {
        return this.hyphenationKeep;
    }

    /** @return the "intrusion-displace" trait */
    public int getIntrusionDisplace() {
        return this.intrusionDisplace;
    }

    /** @return the "line-height-shift-adjustment" trait */
    public int getLineHeightShiftAdjustment() {
        return this.lineHeightShiftAdjustment;
    }

    /**
     * @return the "fox:disable-column-balancing" property, one of
     *         {@link org.apache.fop.fo.Constants#EN_TRUE},
     *         {@link org.apache.fop.fo.Constants#EN_FALSE}
     */
    public int getDisableColumnBalancing() {
        return this.disableColumnBalancing;
    }

    /** {@inheritDoc} */
    @Override
    public CharIterator charIterator() {
        return NullCharIterator.getInstance();
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "block";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_BLOCK}
     */
    @Override
    public int getNameId() {
        return FO_BLOCK;
    }

}
