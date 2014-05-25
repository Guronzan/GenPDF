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

/* $Id: FOText.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.fo;

import java.awt.Color;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.fop.accessibility.StructureTreeElement;
import org.apache.fop.apps.FOPException;
import org.apache.fop.complexscripts.bidi.DelimitedTextRange;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.flow.Block;
import org.apache.fop.fo.properties.CommonFont;
import org.apache.fop.fo.properties.CommonHyphenation;
import org.apache.fop.fo.properties.CommonTextDecoration;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.fo.properties.SpaceProperty;
import org.apache.fop.util.CharUtilities;
import org.xml.sax.Locator;

/**
 * A text node (PCDATA) in the formatting object tree.
 */
public class FOText extends FONode implements CharSequence {

    /** the <code>CharBuffer</code> containing the text */
    private CharBuffer charBuffer;

    // The value of FO traits (refined properties) that apply to #PCDATA
    // (aka implicit sequence of fo:character)
    private CommonFont commonFont;
    private CommonHyphenation commonHyphenation;
    private Color color;
    private KeepProperty keepTogether;
    private Property letterSpacing;
    private SpaceProperty lineHeight;
    private int whiteSpaceTreatment;
    private int whiteSpaceCollapse;
    private int textTransform;
    private Property wordSpacing;
    private int wrapOption;
    private Length baselineShift;
    private String country;
    private String language;
    private String script;
    // End of trait values

    /**
     * Points to the previous FOText object created within the current block. If
     * this is "null", this is the first such object.
     */
    private FOText prevFOTextThisBlock = null;

    /**
     * Points to the next FOText object created within the current block. If
     * this is "null", this is the last such object.
     */
    private FOText nextFOTextThisBlock = null;

    /**
     * Points to the ancestor Block object. This is used to keep track of which
     * FOText nodes are descendants of the same block.
     */
    private Block ancestorBlock = null;

    /** Holds the text decoration values. May be null */
    private CommonTextDecoration textDecoration;

    private StructureTreeElement structureTreeElement;

    /* bidi levels */
    private int[] bidiLevels;

    /* advanced script processing state */
    private Map/* <MapRange,String> */mappings;

    private static final int IS_WORD_CHAR_FALSE = 0;
    private static final int IS_WORD_CHAR_TRUE = 1;
    private static final int IS_WORD_CHAR_MAYBE = 2;

    /**
     * Creates a new FO text node.
     *
     * @param parent
     *            FONode that is the parent of this object
     */
    public FOText(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    protected void characters(final char[] data, final int start,
            final int length, final PropertyList list, final Locator locator)
            throws FOPException {
        if (this.charBuffer == null) {
            // buffer not yet initialized, do so now
            final int newLength = length < 16 ? 16 : length;
            this.charBuffer = CharBuffer.allocate(newLength);
        } else {
            // allocate a larger buffer, and transfer contents
            final int requires = this.charBuffer.position() + length;
            final int capacity = this.charBuffer.capacity();
            if (requires > capacity) {
                int newCapacity = capacity * 2;
                if (requires > newCapacity) {
                    newCapacity = requires;
                }
                final CharBuffer newBuffer = CharBuffer.allocate(newCapacity);
                this.charBuffer.rewind();
                newBuffer.put(this.charBuffer);
                this.charBuffer = newBuffer;
            }
        }
        // extend limit to capacity
        this.charBuffer.limit(this.charBuffer.capacity());
        // append characters
        this.charBuffer.put(data, start, length);
        // shrink limit to position
        this.charBuffer.limit(this.charBuffer.position());
    }

    /**
     * Return the array of characters for this instance.
     *
     * @return a char sequence containing the text
     */
    public CharSequence getCharSequence() {
        if (this.charBuffer == null) {
            return null;
        }
        this.charBuffer.rewind();
        return this.charBuffer.asReadOnlyBuffer().subSequence(0,
                this.charBuffer.limit());
    }

    /** {@inheritDoc} */
    @Override
    public FONode clone(final FONode parent, final boolean removeChildren)
            throws FOPException {
        final FOText ft = (FOText) super.clone(parent, removeChildren);
        if (removeChildren) {
            // not really removing, just make sure the char buffer
            // pointed to is really a different one
            if (this.charBuffer != null) {
                ft.charBuffer = CharBuffer.allocate(this.charBuffer.limit());
                this.charBuffer.rewind();
                ft.charBuffer.put(this.charBuffer);
                ft.charBuffer.rewind();
            }
        }
        ft.prevFOTextThisBlock = null;
        ft.nextFOTextThisBlock = null;
        ft.ancestorBlock = null;
        return ft;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.commonFont = pList.getFontProps();
        this.commonHyphenation = pList.getHyphenationProps();
        this.color = pList.get(Constants.PR_COLOR).getColor(getUserAgent());
        this.keepTogether = pList.get(Constants.PR_KEEP_TOGETHER).getKeep();
        this.lineHeight = pList.get(Constants.PR_LINE_HEIGHT).getSpace();
        this.letterSpacing = pList.get(Constants.PR_LETTER_SPACING);
        this.whiteSpaceCollapse = pList.get(Constants.PR_WHITE_SPACE_COLLAPSE)
                .getEnum();
        this.whiteSpaceTreatment = pList
                .get(Constants.PR_WHITE_SPACE_TREATMENT).getEnum();
        this.textTransform = pList.get(Constants.PR_TEXT_TRANSFORM).getEnum();
        this.wordSpacing = pList.get(Constants.PR_WORD_SPACING);
        this.wrapOption = pList.get(Constants.PR_WRAP_OPTION).getEnum();
        this.textDecoration = pList.getTextDecorationProps();
        this.baselineShift = pList.get(Constants.PR_BASELINE_SHIFT).getLength();
        this.country = pList.get(Constants.PR_COUNTRY).getString();
        this.language = pList.get(Constants.PR_LANGUAGE).getString();
        this.script = pList.get(Constants.PR_SCRIPT).getString();
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (this.charBuffer != null) {
            this.charBuffer.rewind();
        }
        super.endOfNode();
        getFOEventHandler().characters(this);
    }

    /** {@inheritDoc} */
    @Override
    public void finalizeNode() {
        textTransform();
    }

    /**
     * Check if this text node will create an area. This means either there is
     * non-whitespace or it is preserved whitespace. Maybe this just needs to
     * check length > 0, since char iterators handle whitespace.
     *
     * @return true if this will create an area in the output
     */
    public boolean willCreateArea() {
        if (this.whiteSpaceCollapse == Constants.EN_FALSE
                && this.charBuffer.limit() > 0) {
            return true;
        }

        char ch;
        this.charBuffer.rewind();
        while (this.charBuffer.hasRemaining()) {
            ch = this.charBuffer.get();
            if (!(ch == CharUtilities.SPACE
                    || ch == CharUtilities.LINEFEED_CHAR
                    || ch == CharUtilities.CARRIAGE_RETURN || ch == CharUtilities.TAB)) {
                // not whitespace
                this.charBuffer.rewind();
                return true;
            }
        }
        return false;
    }

    /**
     * @return a new TextCharIterator
     */
    @Override
    public CharIterator charIterator() {
        return new TextCharIterator();
    }

    /**
     * This method is run as part of the ancestor Block's flushText(), to create
     * xref pointers to the previous FOText objects within the same Block
     * 
     * @param ancestorBlock
     *            the ancestor fo:block
     */
    protected void createBlockPointers(final Block ancestorBlock) {
        this.ancestorBlock = ancestorBlock;
        // if the last FOText is a sibling, point to it, and have it point here
        if (ancestorBlock.lastFOTextProcessed != null) {
            if (ancestorBlock.lastFOTextProcessed.ancestorBlock == this.ancestorBlock) {
                this.prevFOTextThisBlock = ancestorBlock.lastFOTextProcessed;
                this.prevFOTextThisBlock.nextFOTextThisBlock = this;
            } else {
                this.prevFOTextThisBlock = null;
            }
        }
    }

    /**
     * This method is run as part of endOfNode(), to handle the text-transform
     * property for accumulated FOText
     */
    private void textTransform() {
        if (getBuilderContext().inMarker()
                || this.textTransform == Constants.EN_NONE) {
            return;
        }

        this.charBuffer.rewind();
        final CharBuffer tmp = this.charBuffer.slice();
        char c;
        final int lim = this.charBuffer.limit();
        int pos = -1;
        while (++pos < lim) {
            c = this.charBuffer.get();
            switch (this.textTransform) {
            case Constants.EN_UPPERCASE:
                tmp.put(Character.toUpperCase(c));
                break;
            case Constants.EN_LOWERCASE:
                tmp.put(Character.toLowerCase(c));
                break;
            case Constants.EN_CAPITALIZE:
                if (isStartOfWord(pos)) {
                    /*
                     * Use toTitleCase here. Apparently, some languages use a
                     * different character to represent a letter when using
                     * initial caps than when all of the letters in the word are
                     * capitalized. We will try to let Java handle this.
                     */
                    tmp.put(Character.toTitleCase(c));
                } else {
                    tmp.put(c);
                }
                break;
            default:
                // should never happen as the property subsystem catches that
                // case
                assert false;
                // nop
            }
        }
    }

    /**
     * Determines whether a particular location in an FOText object's text is
     * the start of a new "word". The use of "word" here is specifically for the
     * text-transform property, but may be useful for other things as well, such
     * as word-spacing. The definition of "word" is somewhat ambiguous and
     * appears to be definable by the user agent.
     *
     * @param i
     *            index into charBuffer
     *
     * @return True if the character at this location is the start of a new
     *         word.
     */
    private boolean isStartOfWord(final int i) {
        final char prevChar = getRelativeCharInBlock(i, -1);
        /*
         * All we are really concerned about here is of what type prevChar is.
         * If inputChar is not part of a word, then the Java conversions will
         * (we hope) simply return inputChar.
         */
        switch (isWordChar(prevChar)) {
        case IS_WORD_CHAR_TRUE:
            return false;
        case IS_WORD_CHAR_FALSE:
            return true;
            /*
             * "MAYBE" implies that additional context is needed. An example is
             * a single-quote, either straight or closing, which might be
             * interpreted as a possessive or a contraction, or might be a
             * closing quote.
             */
        case IS_WORD_CHAR_MAYBE:
            final char prevPrevChar = getRelativeCharInBlock(i, -2);
            switch (isWordChar(prevPrevChar)) {
            case IS_WORD_CHAR_TRUE:
                return false;
            case IS_WORD_CHAR_FALSE:
                return true;
            case IS_WORD_CHAR_MAYBE:
                return true;
            default:
                return false;
            }
        default:
            return false;
        }
    }

    /**
     * Finds a character within the current Block that is relative in location
     * to a character in the current FOText. Treats all FOText objects within a
     * block as one unit, allowing text in adjoining FOText objects to be
     * returned if the parameters are outside of the current object.
     *
     * @param i
     *            index into the CharBuffer
     * @param offset
     *            signed integer with relative position within the block of the
     *            character to return. To return the character immediately
     *            preceding i, pass -1. To return the character immediately
     *            after i, pass 1.
     * @return the character in the offset position within the block; \u0000 if
     *         the offset points to an area outside of the block.
     */
    private char getRelativeCharInBlock(final int i, final int offset) {

        final int charIndex = i + offset;
        // The easy case is where the desired character is in the same FOText
        if (charIndex >= 0 && charIndex < length()) {
            return charAt(i + offset);
        }

        // For now, we can't look at following FOText nodes
        if (offset > 0) {
            return CharUtilities.NULL_CHAR;
        }

        // Remaining case has the text in some previous FOText node
        boolean foundChar = false;
        char charToReturn = CharUtilities.NULL_CHAR;
        FOText nodeToTest = this;
        int remainingOffset = offset + i;
        while (!foundChar) {
            if (nodeToTest.prevFOTextThisBlock == null) {
                break;
            }
            nodeToTest = nodeToTest.prevFOTextThisBlock;
            final int diff = nodeToTest.length() + remainingOffset - 1;
            if (diff >= 0) {
                charToReturn = nodeToTest.charAt(diff);
                foundChar = true;
            } else {
                remainingOffset += diff;
            }
        }
        return charToReturn;
    }

    /**
     * @return The previous FOText node in this Block; null, if this is the
     *         first FOText in this Block.
     */
    // public FOText getPrevFOTextThisBlock () {
    // return prevFOTextThisBlock;
    // }

    /**
     * @return The next FOText node in this Block; null if this is the last
     *         FOText in this Block; null if subsequent FOText nodes have not
     *         yet been processed.
     */
    // public FOText getNextFOTextThisBlock () {
    // return nextFOTextThisBlock;
    // }

    /**
     * @return The nearest ancestor block object which contains this FOText.
     */
    // public Block getAncestorBlock () {
    // return ancestorBlock;
    // }

    /**
     * Determines whether the input char should be considered part of a "word".
     * This is used primarily to determine whether the character immediately
     * following starts a new word, but may have other uses. We have not found a
     * definition of "word" in the standard (1.0), so the logic used here is
     * based on the programmer's best guess.
     *
     * @param inputChar
     *            the character to be tested.
     * @return int IS_WORD_CHAR_TRUE, IS_WORD_CHAR_FALSE, or IS_WORD_CHAR_MAYBE,
     *         depending on whether the character should be considered part of a
     *         word or not.
     */
    private static int isWordChar(final char inputChar) {
        switch (Character.getType(inputChar)) {
        case Character.COMBINING_SPACING_MARK:
            return IS_WORD_CHAR_TRUE;
        case Character.CONNECTOR_PUNCTUATION:
            return IS_WORD_CHAR_TRUE;
        case Character.CONTROL:
            return IS_WORD_CHAR_FALSE;
        case Character.CURRENCY_SYMBOL:
            return IS_WORD_CHAR_TRUE;
        case Character.DASH_PUNCTUATION:
            if (inputChar == '-') {
                return IS_WORD_CHAR_TRUE; // hyphen
            }
            return IS_WORD_CHAR_FALSE;
        case Character.DECIMAL_DIGIT_NUMBER:
            return IS_WORD_CHAR_TRUE;
        case Character.ENCLOSING_MARK:
            return IS_WORD_CHAR_FALSE;
        case Character.END_PUNCTUATION:
            if (inputChar == '\u2019') {
                return IS_WORD_CHAR_MAYBE; // apostrophe, right single quote
            }
            return IS_WORD_CHAR_FALSE;
        case Character.FORMAT:
            return IS_WORD_CHAR_FALSE;
        case Character.LETTER_NUMBER:
            return IS_WORD_CHAR_TRUE;
        case Character.LINE_SEPARATOR:
            return IS_WORD_CHAR_FALSE;
        case Character.LOWERCASE_LETTER:
            return IS_WORD_CHAR_TRUE;
        case Character.MATH_SYMBOL:
            return IS_WORD_CHAR_FALSE;
        case Character.MODIFIER_LETTER:
            return IS_WORD_CHAR_TRUE;
        case Character.MODIFIER_SYMBOL:
            return IS_WORD_CHAR_TRUE;
        case Character.NON_SPACING_MARK:
            return IS_WORD_CHAR_TRUE;
        case Character.OTHER_LETTER:
            return IS_WORD_CHAR_TRUE;
        case Character.OTHER_NUMBER:
            return IS_WORD_CHAR_TRUE;
        case Character.OTHER_PUNCTUATION:
            if (inputChar == '\'') {
                return IS_WORD_CHAR_MAYBE; // ASCII apostrophe
            }
            return IS_WORD_CHAR_FALSE;
        case Character.OTHER_SYMBOL:
            return IS_WORD_CHAR_TRUE;
        case Character.PARAGRAPH_SEPARATOR:
            return IS_WORD_CHAR_FALSE;
        case Character.PRIVATE_USE:
            return IS_WORD_CHAR_FALSE;
        case Character.SPACE_SEPARATOR:
            return IS_WORD_CHAR_FALSE;
        case Character.START_PUNCTUATION:
            return IS_WORD_CHAR_FALSE;
        case Character.SURROGATE:
            return IS_WORD_CHAR_FALSE;
        case Character.TITLECASE_LETTER:
            return IS_WORD_CHAR_TRUE;
        case Character.UNASSIGNED:
            return IS_WORD_CHAR_FALSE;
        case Character.UPPERCASE_LETTER:
            return IS_WORD_CHAR_TRUE;
        default:
            return IS_WORD_CHAR_FALSE;
        }
    }

    private class TextCharIterator extends CharIterator {

        private int currentPosition = 0;

        private boolean canRemove = false;
        private boolean canReplace = false;

        public TextCharIterator() {
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return this.currentPosition < FOText.this.charBuffer.limit();
        }

        /** {@inheritDoc} */
        @Override
        public char nextChar() {

            if (this.currentPosition < FOText.this.charBuffer.limit()) {
                this.canRemove = true;
                this.canReplace = true;
                return FOText.this.charBuffer.get(this.currentPosition++);
            } else {
                throw new NoSuchElementException();
            }

        }

        /** {@inheritDoc} */
        @Override
        public void remove() {

            if (this.canRemove) {
                FOText.this.charBuffer.position(this.currentPosition);
                // Slice the buffer at the current position
                final CharBuffer tmp = FOText.this.charBuffer.slice();
                // Reset position to before current character
                FOText.this.charBuffer.position(--this.currentPosition);
                if (tmp.hasRemaining()) {
                    // Transfer any remaining characters
                    FOText.this.charBuffer.mark();
                    FOText.this.charBuffer.put(tmp);
                    FOText.this.charBuffer.reset();
                }
                // Decrease limit
                FOText.this.charBuffer
                        .limit(FOText.this.charBuffer.limit() - 1);
                // Make sure following calls fail, unless nextChar() was called
                this.canRemove = false;
            } else {
                throw new IllegalStateException();
            }

        }

        /** {@inheritDoc} */
        @Override
        public void replaceChar(final char c) {

            if (this.canReplace) {
                FOText.this.charBuffer.put(this.currentPosition - 1, c);
            } else {
                throw new IllegalStateException();
            }

        }

    }

    /**
     * @return the Common Font Properties.
     */
    public CommonFont getCommonFont() {
        return this.commonFont;
    }

    /**
     * @return the Common Hyphenation Properties.
     */
    public CommonHyphenation getCommonHyphenation() {
        return this.commonHyphenation;
    }

    /**
     * @return the "color" trait.
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * @return the "keep-together" trait.
     */
    public KeepProperty getKeepTogether() {
        return this.keepTogether;
    }

    /**
     * @return the "letter-spacing" trait.
     */
    public Property getLetterSpacing() {
        return this.letterSpacing;
    }

    /**
     * @return the "line-height" trait.
     */
    public SpaceProperty getLineHeight() {
        return this.lineHeight;
    }

    /**
     * @return the "white-space-treatment" trait
     */
    public int getWhitespaceTreatment() {
        return this.whiteSpaceTreatment;
    }

    /**
     * @return the "word-spacing" trait.
     */
    public Property getWordSpacing() {
        return this.wordSpacing;
    }

    /**
     * @return the "wrap-option" trait.
     */
    public int getWrapOption() {
        return this.wrapOption;
    }

    /** @return the "text-decoration" trait. */
    public CommonTextDecoration getTextDecoration() {
        return this.textDecoration;
    }

    /** @return the baseline-shift trait */
    public Length getBaseLineShift() {
        return this.baselineShift;
    }

    /** @return the country trait */
    public String getCountry() {
        return this.country;
    }

    /** @return the language trait */
    public String getLanguage() {
        return this.language;
    }

    /** @return the script trait */
    public String getScript() {
        return this.script;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (this.charBuffer == null) {
            return "";
        } else {
            final CharBuffer cb = this.charBuffer.duplicate();
            cb.rewind();
            return cb.toString();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "#PCDATA";
    }

    /** {@inheritDoc} */
    @Override
    public String getNormalNamespacePrefix() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected String gatherContextInfo() {
        if (this.locator != null) {
            return super.gatherContextInfo();
        } else {
            return toString();
        }
    }

    /** {@inheritDoc} */
    @Override
    public char charAt(final int position) {
        return this.charBuffer.get(position);
    }

    /** {@inheritDoc} */
    @Override
    public CharSequence subSequence(final int start, final int end) {
        return this.charBuffer.subSequence(start, end);
    }

    /** {@inheritDoc} */
    @Override
    public int length() {
        return this.charBuffer.limit();
    }

    /**
     * Resets the backing <code>java.nio.CharBuffer</code>
     */
    public void resetBuffer() {
        if (this.charBuffer != null) {
            this.charBuffer.rewind();
        }
    }

    @Override
    public boolean isDelimitedTextRangeBoundary(final int boundary) {
        return false;
    }

    @Override
    public void setStructureTreeElement(
            final StructureTreeElement structureTreeElement) {
        this.structureTreeElement = structureTreeElement;
    }

    /** @return the structure tree element. */
    public StructureTreeElement getStructureTreeElement() {
        return this.structureTreeElement;
    }

    /**
     * Set bidirectional level over interval [start,end).
     * 
     * @param level
     *            the resolved level
     * @param start
     *            the starting index of interval
     * @param end
     *            the ending index of interval
     */
    public void setBidiLevel(final int level, final int start, final int end) {
        if (start < end) {
            if (this.bidiLevels == null) {
                this.bidiLevels = new int[length()];
            }
            for (int i = start, n = end; i < n; i++) {
                this.bidiLevels[i] = level;
            }
            if (this.parent != null) {
                ((FObj) this.parent).setBidiLevel(level);
            }
        } else {
            assert start < end;
        }
    }

    /**
     * Obtain bidirectional level of each character represented by this FOText.
     * 
     * @return a (possibly empty) array of bidi levels or null in case no bidi
     *         levels have been assigned
     */
    public int[] getBidiLevels() {
        return this.bidiLevels;
    }

    /**
     * Obtain bidirectional level of each character over interval [start,end).
     * 
     * @param start
     *            the starting index of interval
     * @param end
     *            the ending index of interval
     * @return a (possibly empty) array of bidi levels or null in case no bidi
     *         levels have been assigned
     */
    public int[] getBidiLevels(final int start, final int end) {
        if (this.bidiLevels != null) {
            assert start <= end;
            final int n = end - start;
            final int[] bidiLevels = new int[n];
            for (int i = 0; i < n; i++) {
                bidiLevels[i] = this.bidiLevels[start + i];
            }
            return bidiLevels;
        } else {
            return null;
        }
    }

    /**
     * Obtain bidirectional level of character at specified position, which must
     * be a non-negative integer less than the length of this FO.
     * 
     * @param position
     *            an offset position into FO's characters
     * @return a resolved bidi level or -1 if default
     * @throws IndexOutOfBoundsException
     *             if position is not non-negative integer or is greater than or
     *             equal to length
     */
    public int bidiLevelAt(final int position) throws IndexOutOfBoundsException {
        if (position < 0 || position >= length()) {
            throw new IndexOutOfBoundsException();
        } else if (this.bidiLevels != null) {
            return this.bidiLevels[position];
        } else {
            return -1;
        }
    }

    /**
     * Add characters mapped by script substitution processing.
     * 
     * @param start
     *            index in character buffer
     * @param end
     *            index in character buffer
     * @param mappedChars
     *            sequence of character codes denoting substituted characters
     */
    public void addMapping(final int start, final int end,
            final CharSequence mappedChars) {
        if (this.mappings == null) {
            this.mappings = new java.util.HashMap();
        }
        this.mappings.put(new MapRange(start, end), mappedChars.toString());
    }

    /**
     * Determine if characters over specific interval have a mapping.
     * 
     * @param start
     *            index in character buffer
     * @param end
     *            index in character buffer
     * @return true if a mapping exist such that the mapping's interval is
     *         coincident to [start,end)
     */
    public boolean hasMapping(final int start, final int end) {
        return this.mappings != null
                && this.mappings.containsKey(new MapRange(start, end));
    }

    /**
     * Obtain mapping of characters over specific interval.
     * 
     * @param start
     *            index in character buffer
     * @param end
     *            index in character buffer
     * @return a string of characters representing the mapping over the interval
     *         [start,end)
     */
    public String getMapping(final int start, final int end) {
        if (this.mappings != null) {
            return (String) this.mappings.get(new MapRange(start, end));
        } else {
            return null;
        }
    }

    /**
     * Obtain length of mapping of characters over specific interval.
     * 
     * @param start
     *            index in character buffer
     * @param end
     *            index in character buffer
     * @return the length of the mapping (if present) or zero
     */
    public int getMappingLength(final int start, final int end) {
        if (this.mappings != null) {
            return ((String) this.mappings.get(new MapRange(start, end)))
                    .length();
        } else {
            return 0;
        }
    }

    /**
     * Obtain bidirectional levels of mapping of characters over specific
     * interval.
     * 
     * @param start
     *            index in character buffer
     * @param end
     *            index in character buffer
     * @return a (possibly empty) array of bidi levels or null in case no bidi
     *         levels have been assigned
     */
    public int[] getMappingBidiLevels(final int start, final int end) {
        if (hasMapping(start, end)) {
            final int nc = end - start;
            final int nm = getMappingLength(start, end);
            final int[] la = getBidiLevels(start, end);
            if (la == null) {
                return null;
            } else if (nm == nc) { // mapping is same length as mapped range
                return la;
            } else if (nm > nc) { // mapping is longer than mapped range
                final int[] ma = new int[nm];
                System.arraycopy(la, 0, ma, 0, la.length);
                for (int i = la.length, n = ma.length, l = i > 0 ? la[i - 1]
                        : 0; i < n; i++) {
                    ma[i] = l;
                }
                return ma;
            } else { // mapping is shorter than mapped range
                final int[] ma = new int[nm];
                System.arraycopy(la, 0, ma, 0, ma.length);
                return ma;
            }
        } else {
            return getBidiLevels(start, end);
        }
    }

    @Override
    protected Stack collectDelimitedTextRanges(final Stack ranges,
            final DelimitedTextRange currentRange) {
        if (currentRange != null) {
            currentRange.append(charIterator(), this);
        }
        return ranges;
    }

    private static class MapRange {
        private final int start;
        private final int end;

        MapRange(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public int hashCode() {
            return this.start * 31 + this.end;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof MapRange) {
                final MapRange r = (MapRange) o;
                return r.start == this.start && r.end == this.end;
            } else {
                return false;
            }
        }
    }

}
