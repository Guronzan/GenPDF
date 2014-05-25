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

/* $Id: Character.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.fo.flow;

import java.awt.Color;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.fop.accessibility.StructureTreeElement;
import org.apache.fop.apps.FOPException;
import org.apache.fop.complexscripts.bidi.DelimitedTextRange;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.CharIterator;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonFont;
import org.apache.fop.fo.properties.CommonHyphenation;
import org.apache.fop.fo.properties.CommonTextDecoration;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.fo.properties.SpaceProperty;
import org.apache.fop.fo.properties.StructureTreeElementHolder;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_character">
 * <code>fo:character</code></a> object.
 */
public class Character extends FObj implements StructureTreeElementHolder {
    // The value of properties relevant for fo:character.
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private CommonFont commonFont;
    private CommonHyphenation commonHyphenation;
    private Length alignmentAdjust;
    private int alignmentBaseline;
    private Length baselineShift;
    private char character;
    private Color color;
    private int dominantBaseline;
    private KeepProperty keepWithNext;
    private KeepProperty keepWithPrevious;
    private Property letterSpacing;
    private SpaceProperty lineHeight;
    /** Holds the text decoration values. May be null */
    private CommonTextDecoration textDecoration;
    // private ToBeImplementedProperty textShadow;
    private Property wordSpacing;
    private StructureTreeElement structureTreeElement;
    // Unused but valid items, commented out for performance:
    // private CommonAural commonAural;
    // private CommonMarginInline commonMarginInline;
    // private CommonRelativePosition commonRelativePosition;
    // private ToBeImplementedProperty glyphOrientationHorizontal;
    // private ToBeImplementedProperty glyphOrientationVertical;
    // private int treatAsWordSpace;
    // private Length textDepth;
    // private Length textAltitude;
    // private int scoreSpaces;
    // private int suppressAtLineBreak;
    // private int textTransform;
    // private int visibility;
    // End of property values

    /** constant indicating that the character is OK */
    public static final int OK = 0;
    /** constant indicating that the character does not fit */
    public static final int DOESNOT_FIT = 1;

    /**
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public Character(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        this.commonFont = pList.getFontProps();
        this.commonHyphenation = pList.getHyphenationProps();

        this.alignmentAdjust = pList.get(PR_ALIGNMENT_ADJUST).getLength();
        this.alignmentBaseline = pList.get(PR_ALIGNMENT_BASELINE).getEnum();
        this.baselineShift = pList.get(PR_BASELINE_SHIFT).getLength();
        this.character = pList.get(PR_CHARACTER).getCharacter();
        this.color = pList.get(PR_COLOR).getColor(getUserAgent());
        this.dominantBaseline = pList.get(PR_DOMINANT_BASELINE).getEnum();
        this.keepWithNext = pList.get(PR_KEEP_WITH_NEXT).getKeep();
        this.keepWithPrevious = pList.get(PR_KEEP_WITH_PREVIOUS).getKeep();
        this.letterSpacing = pList.get(PR_LETTER_SPACING);
        this.lineHeight = pList.get(PR_LINE_HEIGHT).getSpace();
        this.textDecoration = pList.getTextDecorationProps();
        this.wordSpacing = pList.get(PR_WORD_SPACING);
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().character(this);
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
    public CharIterator charIterator() {
        return new FOCharIterator(this);
    }

    /** @return the Common Border, Padding, and Background Properties */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /** @return the Common Font Properties */
    public CommonFont getCommonFont() {
        return this.commonFont;
    }

    /** @return the Common Hyphenation Properties */
    public CommonHyphenation getCommonHyphenation() {
        return this.commonHyphenation;
    }

    /** @return the "character" property */
    public char getCharacter() {
        return this.character;
    }

    /** @return the "color" property */
    public Color getColor() {
        return this.color;
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

    /** @return the "letter-spacing" property */
    public Property getLetterSpacing() {
        return this.letterSpacing;
    }

    /** @return the "line-height" property */
    public SpaceProperty getLineHeight() {
        return this.lineHeight;
    }

    /** @return the "text-decoration" property. */
    public CommonTextDecoration getTextDecoration() {
        return this.textDecoration;
    }

    /** @return the "word-spacing" property */
    public Property getWordSpacing() {
        return this.wordSpacing;
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

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "character";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_CHARACTER}
     */
    @Override
    public int getNameId() {
        return FO_CHARACTER;
    }

    @Override
    public boolean isDelimitedTextRangeBoundary(final int boundary) {
        return false;
    }

    @Override
    protected Stack collectDelimitedTextRanges(final Stack ranges,
            final DelimitedTextRange currentRange) {
        if (currentRange != null) {
            currentRange.append(charIterator(), this);
        }
        return ranges;
    }

    private class FOCharIterator extends CharIterator {

        private boolean bFirst = true;
        private final Character foChar;

        FOCharIterator(final Character foChar) {
            this.foChar = foChar;
        }

        @Override
        public boolean hasNext() {
            return this.bFirst;
        }

        @Override
        public char nextChar() {
            if (this.bFirst) {
                this.bFirst = false;
                return this.foChar.character;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            this.foChar.parent.removeChild(this.foChar);
        }

        @Override
        public void replaceChar(final char c) {
            this.foChar.character = c;
        }

    }

}
