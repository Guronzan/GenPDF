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

/* $Id: XMLWhiteSpaceHandler.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.fo;

import java.util.List;
import java.util.Stack;

import org.apache.fop.fo.flow.Block;
import org.apache.fop.util.CharUtilities;

/**
 * Class encapsulating the functionality for white-space-handling during
 * refinement stage. The <code>handleWhiteSpace()</code> methods are called
 * during FOTree-building and marker-cloning: <br>
 * <ul>
 * <li>from <code>FObjMixed.addChildNode()</code></li>
 * <li>from <code>FObjMixed.endOfNode()</code></li>
 * <li>from <code>FObjMixed.handleWhiteSpaceFor()</code></li>
 * </ul>
 * <br>
 * Each time one of the variants is called, white-space is handled for all
 * <code>FOText</code> or <code>Character</code> nodes that were added: <br>
 * <ul>
 * <li>either prior to <code>newChild</code> (and after the previous non-text
 * child node)</li>
 * <li>or, if <code>newChild</code> is <code>null</code>, after the previous
 * non-text child</li>
 * </ul>
 * <br>
 * The iteration always starts at <code>firstTextNode</code>, goes on until the
 * last text-node is reached, and deals only with <code>FOText</code> or
 * <code>Character</code> nodes. <br>
 * <em>Note</em>: if the method is called from an inline's endOfNode(), there is
 * too little context to decide whether trailing white-space may be removed, so
 * the pending inline is stored in a List, together with an iterator for which
 * the next() method returns the first in the trailing sequence of white- space
 * characters. This List is processed again at the end of the ancestor block.
 */
public class XMLWhiteSpaceHandler {

    /** True if we are in a run of white space */
    private boolean inWhiteSpace = false;
    /** True if the last char was a linefeed */
    private boolean afterLinefeed = true;
    /** Counter, increased every time a non-white-space is encountered */
    private int nonWhiteSpaceCount;

    private int linefeedTreatment;
    private int whiteSpaceTreatment;
    private int whiteSpaceCollapse;
    private boolean endOfBlock;
    private boolean nextChildIsBlockLevel;
    private RecursiveCharIterator charIter;

    private List pendingInlines;
    private final Stack nestedBlockStack = new java.util.Stack();
    private CharIterator firstWhiteSpaceInSeq;

    /**
     * Handle white-space for the fo that is passed in, starting at
     * firstTextNode
     * 
     * @param fo
     *            the FO for which to handle white-space
     * @param firstTextNode
     *            the node at which to start
     * @param nextChild
     *            the node that will be added to the list after firstTextNode
     */
    public void handleWhiteSpace(final FObjMixed fo,
            final FONode firstTextNode, final FONode nextChild) {

        Block currentBlock = null;
        final int foId = fo.getNameId();

        /* set the current block */
        switch (foId) {
        case Constants.FO_BLOCK:
            currentBlock = (Block) fo;
            if (this.nestedBlockStack.empty()
                    || fo != this.nestedBlockStack.peek()) {
                if (nextChild != null) {
                    /*
                     * if already in a block, push the current block onto the
                     * stack of nested blocks
                     */
                    this.nestedBlockStack.push(currentBlock);
                }
            } else {
                if (nextChild == null) {
                    this.nestedBlockStack.pop();
                }
            }
            break;

        case Constants.FO_RETRIEVE_MARKER:
            /* look for the nearest block ancestor, if any */
            FONode ancestor = fo;
            do {
                ancestor = ancestor.getParent();
            } while (ancestor.getNameId() != Constants.FO_BLOCK
                    && ancestor.getNameId() != Constants.FO_STATIC_CONTENT);

            if (ancestor.getNameId() == Constants.FO_BLOCK) {
                currentBlock = (Block) ancestor;
                this.nestedBlockStack.push(currentBlock);
            }
            break;

        default:
            if (!this.nestedBlockStack.empty()) {
                currentBlock = (Block) this.nestedBlockStack.peek();
            }
        }

        if (currentBlock != null) {
            this.linefeedTreatment = currentBlock.getLinefeedTreatment();
            this.whiteSpaceCollapse = currentBlock.getWhitespaceCollapse();
            this.whiteSpaceTreatment = currentBlock.getWhitespaceTreatment();
        } else {
            this.linefeedTreatment = Constants.EN_TREAT_AS_SPACE;
            this.whiteSpaceCollapse = Constants.EN_TRUE;
            this.whiteSpaceTreatment = Constants.EN_IGNORE_IF_SURROUNDING_LINEFEED;
        }

        this.endOfBlock = nextChild == null && fo == currentBlock;

        if (firstTextNode == null) {
            // no text means no white-space to handle; return early
            this.afterLinefeed = fo == currentBlock && fo.firstChild == null;
            this.nonWhiteSpaceCount = 0;
            if (this.endOfBlock) {
                handlePendingInlines();
            }
            return;
        }

        this.charIter = new RecursiveCharIterator(fo, firstTextNode);
        this.inWhiteSpace = false;

        if (fo == currentBlock || currentBlock == null
                || foId == Constants.FO_RETRIEVE_MARKER
                && fo.getParent() == currentBlock) {
            if (firstTextNode == fo.firstChild) {
                this.afterLinefeed = true;
            } else {
                final int previousChildId = firstTextNode.siblings[0]
                        .getNameId();
                this.afterLinefeed = previousChildId == Constants.FO_BLOCK
                        || previousChildId == Constants.FO_TABLE_AND_CAPTION
                        || previousChildId == Constants.FO_TABLE
                        || previousChildId == Constants.FO_LIST_BLOCK
                        || previousChildId == Constants.FO_BLOCK_CONTAINER;
            }
        }

        if (foId == Constants.FO_WRAPPER) {
            FONode parent = fo.parent;
            int parentId = parent.getNameId();
            while (parentId == Constants.FO_WRAPPER) {
                parent = parent.parent;
                parentId = parent.getNameId();
            }
            if (parentId == Constants.FO_FLOW
                    || parentId == Constants.FO_STATIC_CONTENT
                    || parentId == Constants.FO_BLOCK_CONTAINER
                    || parentId == Constants.FO_TABLE_CELL) {
                this.endOfBlock = nextChild == null;
            }
        }

        if (nextChild != null) {
            final int nextChildId = nextChild.getNameId();
            this.nextChildIsBlockLevel = nextChildId == Constants.FO_BLOCK
                    || nextChildId == Constants.FO_TABLE_AND_CAPTION
                    || nextChildId == Constants.FO_TABLE
                    || nextChildId == Constants.FO_LIST_BLOCK
                    || nextChildId == Constants.FO_BLOCK_CONTAINER;
        } else {
            this.nextChildIsBlockLevel = false;
        }

        handleWhiteSpace();

        if (fo == currentBlock
                && (this.endOfBlock || this.nextChildIsBlockLevel)) {
            handlePendingInlines();
        }

        if (nextChild == null) {
            if (fo != currentBlock) {
                /* current FO is not a block, and is about to end */
                if (this.nonWhiteSpaceCount > 0 && this.pendingInlines != null) {
                    /*
                     * there is non-white-space text between the pending
                     * inline(s) and the end of the non-block node; clear list
                     * of pending inlines
                     */
                    this.pendingInlines.clear();
                }
                if (this.inWhiteSpace) {
                    /*
                     * means there is at least one trailing space in the inline
                     * FO that is about to end
                     */
                    addPendingInline(fo);
                }
            } else {
                /*
                 * end of block: clear the references and pop the nested block
                 * stack
                 */
                if (!this.nestedBlockStack.empty()) {
                    this.nestedBlockStack.pop();
                }
                this.charIter = null;
                this.firstWhiteSpaceInSeq = null;
            }
        }
    }

    /**
     * Reset the handler, release all references
     */
    protected final void reset() {
        if (this.pendingInlines != null) {
            this.pendingInlines.clear();
        }
        this.nestedBlockStack.clear();
        this.charIter = null;
        this.firstWhiteSpaceInSeq = null;
    }

    /**
     * Handle white-space for the fo that is passed in, starting at
     * firstTextNode (when a nested FO is encountered)
     * 
     * @param fo
     *            the FO for which to handle white-space
     * @param firstTextNode
     *            the node at which to start
     */
    public void handleWhiteSpace(final FObjMixed fo, final FONode firstTextNode) {
        handleWhiteSpace(fo, firstTextNode, null);
    }

    private void handleWhiteSpace() {

        final EOLchecker lfCheck = new EOLchecker(this.charIter);

        this.nonWhiteSpaceCount = 0;

        while (this.charIter.hasNext()) {
            if (!this.inWhiteSpace) {
                this.firstWhiteSpaceInSeq = this.charIter.mark();
            }
            char currentChar = this.charIter.nextChar();
            int currentCharClass = CharUtilities.classOf(currentChar);
            if (currentCharClass == CharUtilities.LINEFEED
                    && this.linefeedTreatment == Constants.EN_TREAT_AS_SPACE) {
                // if we have a linefeed and it is supposed to be treated
                // like a space, that's what we do and continue
                currentChar = '\u0020';
                this.charIter.replaceChar('\u0020');
                currentCharClass = CharUtilities.classOf(currentChar);
            }
            switch (CharUtilities.classOf(currentChar)) {
            case CharUtilities.XMLWHITESPACE:
                // Some kind of whitespace character, except linefeed.
                if (this.inWhiteSpace
                        && this.whiteSpaceCollapse == Constants.EN_TRUE) {
                    // We are in a run of whitespace and should collapse
                    // Just delete the char
                    this.charIter.remove();
                } else {
                    // Do the white space treatment here
                    boolean bIgnore = false;

                    switch (this.whiteSpaceTreatment) {
                    case Constants.EN_IGNORE:
                        bIgnore = true;
                        break;
                    case Constants.EN_IGNORE_IF_BEFORE_LINEFEED:
                        bIgnore = lfCheck.beforeLinefeed();
                        break;
                    case Constants.EN_IGNORE_IF_SURROUNDING_LINEFEED:
                        bIgnore = this.afterLinefeed
                                || lfCheck.beforeLinefeed();
                        break;
                    case Constants.EN_IGNORE_IF_AFTER_LINEFEED:
                        bIgnore = this.afterLinefeed;
                        break;
                    case Constants.EN_PRESERVE:
                        // nothing to do now, replacement takes place later
                        break;
                    default:
                        // nop
                    }
                    // Handle ignore and replacement
                    if (bIgnore) {
                        this.charIter.remove();
                    } else {
                        // this is to retain a single space between words
                        this.inWhiteSpace = true;
                        if (currentChar != '\u0020') {
                            this.charIter.replaceChar('\u0020');
                        }
                    }
                }
                break;

            case CharUtilities.LINEFEED:
                // A linefeed
                switch (this.linefeedTreatment) {
                case Constants.EN_IGNORE:
                    this.charIter.remove();
                    break;
                case Constants.EN_TREAT_AS_ZERO_WIDTH_SPACE:
                    this.charIter.replaceChar(CharUtilities.ZERO_WIDTH_SPACE);
                    this.inWhiteSpace = false;
                    break;
                case Constants.EN_PRESERVE:
                    lfCheck.reset();
                    this.inWhiteSpace = false;
                    this.afterLinefeed = true; // for following whitespace
                    break;
                default:
                    // nop
                }
                break;

            case CharUtilities.EOT:
                // A "boundary" objects such as non-character inline
                // or nested block object was encountered. (? can't happen)
                // If any whitespace run in progress, finish it.
                // FALL THROUGH

            default:
                // Any other character
                this.inWhiteSpace = false;
                this.afterLinefeed = false;
                this.nonWhiteSpaceCount++;
                lfCheck.reset();
                break;
            }
        }
    }

    private void addPendingInline(final FObjMixed fo) {
        if (this.pendingInlines == null) {
            this.pendingInlines = new java.util.ArrayList(5);
        }
        this.pendingInlines
                .add(new PendingInline(fo, this.firstWhiteSpaceInSeq));
    }

    private void handlePendingInlines() {
        if (!(this.pendingInlines == null || this.pendingInlines.isEmpty())) {
            if (this.nonWhiteSpaceCount == 0) {
                /* handle white-space for all pending inlines */
                PendingInline p;
                for (int i = this.pendingInlines.size(); --i >= 0;) {
                    p = (PendingInline) this.pendingInlines.get(i);
                    this.charIter = (RecursiveCharIterator) p.firstTrailingWhiteSpace;
                    handleWhiteSpace();
                    this.pendingInlines.remove(p);
                }
            } else {
                /*
                 * there is non-white-space text between the pending inline(s)
                 * and the end of the block; clear list of pending inlines
                 */
                this.pendingInlines.clear();
            }
        }
    }

    /**
     * Helper class, used during white-space handling to look ahead, and see if
     * the next character is a linefeed (or if there will be an equivalent
     * effect during layout, i.e. end-of-block or the following child is a
     * block-level FO)
     */
    private class EOLchecker {
        private boolean nextIsEOL = false;
        private final RecursiveCharIterator charIter;

        EOLchecker(final CharIterator charIter) {
            this.charIter = (RecursiveCharIterator) charIter;
        }

        boolean beforeLinefeed() {
            if (!this.nextIsEOL) {
                final CharIterator lfIter = this.charIter.mark();
                while (lfIter.hasNext()) {
                    final int charClass = CharUtilities.classOf(lfIter
                            .nextChar());
                    if (charClass == CharUtilities.LINEFEED) {
                        if (XMLWhiteSpaceHandler.this.linefeedTreatment == Constants.EN_PRESERVE) {
                            this.nextIsEOL = true;
                            return this.nextIsEOL;
                        }
                    } else if (charClass != CharUtilities.XMLWHITESPACE) {
                        return this.nextIsEOL;
                    }
                }
                // No more characters == end of text run
                // means EOL if there either is a nested block to be added,
                // or if this is the last text node in the current block
                this.nextIsEOL = XMLWhiteSpaceHandler.this.nextChildIsBlockLevel
                        || XMLWhiteSpaceHandler.this.endOfBlock;
            }
            return this.nextIsEOL;
        }

        void reset() {
            this.nextIsEOL = false;
        }
    }

    /**
     * Helper class to store unfinished inline nodes together with an iterator
     * that starts at the first white-space character in the sequence of
     * trailing white-space
     */
    private class PendingInline {
        protected FObjMixed fo;
        protected CharIterator firstTrailingWhiteSpace;

        PendingInline(final FObjMixed fo,
                final CharIterator firstTrailingWhiteSpace) {
            this.fo = fo;
            this.firstTrailingWhiteSpace = firstTrailingWhiteSpace;
        }
    }
}
