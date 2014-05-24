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

/* $Id: RtfTextrun.java 1310948 2012-04-08 03:57:07Z gadams $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

// Java
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Class which contains a linear text run. It has methods to add attributes,
 * text, paragraph breaks....
 * </p>
 *
 * <p>
 * This work was authored by Peter Herweg (pherweg@web.de).
 * </p>
 */
@Slf4j
public class RtfTextrun extends RtfContainer {

    /** Constant for no page break */
    public static final int BREAK_NONE = 0;
    /** Constant for a normal page break */
    public static final int BREAK_PAGE = 1;
    /** Constant for a column break */
    public static final int BREAK_COLUMN = 2;
    /** Constant for a even page break */
    public static final int BREAK_EVEN_PAGE = 3;
    /** Constant for a odd page break */
    public static final int BREAK_ODD_PAGE = 4;

    private boolean bSuppressLastPar = false;
    private RtfListItem rtfListItem;

    /** Manager for handling space-* property. */
    private final RtfSpaceManager rtfSpaceManager = new RtfSpaceManager();

    /** Class which represents the opening of a RTF group mark. */
    private class RtfOpenGroupMark extends RtfElement {

        RtfOpenGroupMark(final RtfContainer parent, final Writer w,
                final RtfAttributes attr) throws IOException {
            super(parent, w, attr);
        }

        /**
         * @return true if this element would generate no "useful" RTF content
         */
        @Override
        public boolean isEmpty() {
            return false;
        }

        /**
         * write RTF code of all our children
         *
         * @throws IOException
         *             for I/O problems
         */
        @Override
        protected void writeRtfContent() throws IOException {
            writeGroupMark(true);
            writeAttributes(getRtfAttributes(), null);
        }
    }

    /** Class which represents the closing of a RTF group mark. */
    private class RtfCloseGroupMark extends RtfElement {
        private int breakType = BREAK_NONE;

        RtfCloseGroupMark(final RtfContainer parent, final Writer w,
                final int breakType) throws IOException {
            super(parent, w);
            this.breakType = breakType;
        }

        /**
         * @return true if this element would generate no "useful" RTF content
         */
        @Override
        public boolean isEmpty() {
            return false;
        }

        /**
         * Returns the break type.
         *
         * @return the break type (BREAK_* constants)
         */
        public int getBreakType() {
            return this.breakType;
        }

        /**
         * Write RTF code of all our children.
         *
         * @throws IOException
         *             for I/O problems
         */
        @Override
        protected void writeRtfContent() throws IOException {
            writeGroupMark(false);
            final boolean bHasTableCellParent = getParentOfClass(RtfTableCell.class) != null;

            // Unknown behavior when a table starts a new section,
            // Word may crash
            if (this.breakType != BREAK_NONE) {
                if (!bHasTableCellParent) {
                    writeControlWord("sect");
                    /*
                     * The following modifiers don't seem to appear in the right
                     * place
                     */
                    switch (this.breakType) {
                    case BREAK_EVEN_PAGE:
                        writeControlWord("sbkeven");
                        break;
                    case BREAK_ODD_PAGE:
                        writeControlWord("sbkodd");
                        break;
                    case BREAK_COLUMN:
                        writeControlWord("sbkcol");
                        break;
                    default:
                        writeControlWord("sbkpage");
                    }
                } else {
                    log.warn("Cannot create break-after for a paragraph inside a table.");
                }
            }
        }
    }

    /** Create an RTF container as a child of given container */
    RtfTextrun(final RtfContainer parent, final Writer w,
            final RtfAttributes attrs) throws IOException {
        super(parent, w, attrs);
    }

    /**
     * Adds instance of <code>OpenGroupMark</code> as a child with attributes.
     *
     * @param attrs
     *            attributes to add
     * @throws IOException
     *             for I/O problems
     */
    private void addOpenGroupMark(final RtfAttributes attrs) throws IOException {
        final RtfOpenGroupMark r = new RtfOpenGroupMark(this, this.writer,
                attrs);
    }

    /**
     * Adds instance of <code>CloseGroupMark</code> as a child.
     *
     * @throws IOException
     *             for I/O problems
     */
    private void addCloseGroupMark(final int breakType) throws IOException {
        final RtfCloseGroupMark r = new RtfCloseGroupMark(this, this.writer,
                breakType);
    }

    /**
     * Adds instance of <code>CloseGroupMark</code> as a child, but without a
     * break option. Inline attributes do not need that for example
     *
     * @throws IOException
     *             for I/O problems
     */
    private void addCloseGroupMark() throws IOException {
        final RtfCloseGroupMark r = new RtfCloseGroupMark(this, this.writer,
                BREAK_NONE);
    }

    /**
     * Pushes block attributes, notifies all opened blocks about pushing block
     * attributes, adds <code>OpenGroupMark</code> as a child.
     *
     * @param attrs
     *            the block attributes to push
     * @throws IOException
     *             for I/O problems
     */
    public void pushBlockAttributes(final RtfAttributes attrs)
            throws IOException {
        this.rtfSpaceManager.stopUpdatingSpaceBefore();
        final RtfSpaceSplitter splitter = this.rtfSpaceManager
                .pushRtfSpaceSplitter(attrs);
        addOpenGroupMark(splitter.getCommonAttributes());
    }

    /**
     * Pops block attributes, notifies all opened blocks about pushing block
     * attributes, adds <code>CloseGroupMark</code> as a child.
     *
     * @param breakType
     *            the break type
     * @throws IOException
     *             for I/O problems
     */
    public void popBlockAttributes(final int breakType) throws IOException {
        this.rtfSpaceManager.popRtfSpaceSplitter();
        this.rtfSpaceManager.stopUpdatingSpaceBefore();
        addCloseGroupMark(breakType);
    }

    /**
     * Pushes inline attributes.
     *
     * @param attrs
     *            the inline attributes to push
     * @throws IOException
     *             for I/O problems
     */
    public void pushInlineAttributes(final RtfAttributes attrs)
            throws IOException {
        this.rtfSpaceManager.pushInlineAttributes(attrs);
        addOpenGroupMark(attrs);
    }

    /**
     * Inserts a page number citation.
     *
     * @param refId
     *            the identifier being referenced
     * @throws IOException
     *             for I/O problems
     */
    public void addPageNumberCitation(final String refId) throws IOException {
        final RtfPageNumberCitation r = new RtfPageNumberCitation(this,
                this.writer, refId);
    }

    /**
     * Pop inline attributes.
     *
     * @throws IOException
     *             for I/O problems
     */
    public void popInlineAttributes() throws IOException {
        this.rtfSpaceManager.popInlineAttributes();
        addCloseGroupMark();
    }

    /**
     * Add string to children list.
     *
     * @param s
     *            string to add
     * @throws IOException
     *             for I/O problems
     */
    public void addString(final String s) throws IOException {
        if (s.equals("")) {
            return;
        }
        final RtfAttributes attrs = this.rtfSpaceManager
                .getLastInlineAttribute();
        // add RtfSpaceSplitter to inherit accumulated space
        this.rtfSpaceManager.pushRtfSpaceSplitter(attrs);
        this.rtfSpaceManager.setCandidate(attrs);
        // create a string and add it as a child
        new RtfString(this, this.writer, s);
        this.rtfSpaceManager.popRtfSpaceSplitter();
    }

    /**
     * Inserts a footnote.
     *
     * @return inserted footnote
     * @throws IOException
     *             for I/O problems
     */
    public RtfFootnote addFootnote() throws IOException {
        return new RtfFootnote(this, this.writer);
    }

    /**
     * Inserts paragraph break before all close group marks.
     *
     * @throws IOException
     *             for I/O problems
     * @return The paragraph break element
     */
    public RtfParagraphBreak addParagraphBreak() throws IOException {
        // get copy of children list
        final List children = getChildren();
        final Stack tmp = new Stack();
        RtfParagraphBreak par = null;

        // delete all previous CloseGroupMark
        int deletedCloseGroupCount = 0;

        final ListIterator lit = children.listIterator(children.size());
        while (lit.hasPrevious() && lit.previous() instanceof RtfCloseGroupMark) {
            tmp.push(Integer.valueOf(((RtfCloseGroupMark) lit.next())
                    .getBreakType()));
            lit.remove();
            deletedCloseGroupCount++;
        }

        if (children.size() != 0) {
            // add paragraph break and restore all deleted close group marks
            setChildren(children);
            par = new RtfParagraphBreak(this, this.writer);
            for (int i = 0; i < deletedCloseGroupCount; i++) {
                addCloseGroupMark(((Integer) tmp.pop()).intValue());
            }
        }
        return par;
    }

    /**
     * Inserts a leader.
     *
     * @param attrs
     *            Attributes for the leader
     * @throws IOException
     *             for I/O problems
     */
    public void addLeader(final RtfAttributes attrs) throws IOException {
        new RtfLeader(this, this.writer, attrs);
    }

    /**
     * Inserts a page number.
     *
     * @param attr
     *            Attributes for the page number to insert.
     * @throws IOException
     *             for I/O problems
     */
    public void addPageNumber(final RtfAttributes attr) throws IOException {
        final RtfPageNumber r = new RtfPageNumber(this, this.writer, attr);
    }

    /**
     * Inserts a hyperlink.
     *
     * @param attr
     *            Attributes for the hyperlink to insert.
     * @return inserted hyperlink
     * @throws IOException
     *             for I/O problems
     */
    public RtfHyperLink addHyperlink(final RtfAttributes attr)
            throws IOException {
        return new RtfHyperLink(this, this.writer, attr);
    }

    /**
     * Inserts a bookmark.
     *
     * @param id
     *            Id for the inserted bookmark
     * @throws IOException
     *             for I/O problems
     */
    public void addBookmark(final String id) throws IOException {
        if (id != "") {
            // if id is not empty, add boormark
            new RtfBookmark(this, this.writer, id);
        }
    }

    /**
     * Inserts an image.
     *
     * @return inserted image
     * @throws IOException
     *             for I/O problems
     */
    public RtfExternalGraphic newImage() throws IOException {
        return new RtfExternalGraphic(this, this.writer);
    }

    /**
     * Adds a new RtfTextrun to the given container if necessary, and returns
     * it.
     *
     * @param container
     *            RtfContainer, which is the parent of the returned RtfTextrun
     * @param writer
     *            Writer of the given RtfContainer
     * @param attrs
     *            RtfAttributes which are to write at the beginning of the
     *            RtfTextrun
     * @return new or existing RtfTextrun object.
     * @throws IOException
     *             for I/O problems
     */
    public static RtfTextrun getTextrun(final RtfContainer container,
            final Writer writer, final RtfAttributes attrs) throws IOException {

        final List list = container.getChildren();

        if (list.size() == 0) {
            // add a new RtfTextrun
            final RtfTextrun textrun = new RtfTextrun(container, writer, attrs);
            list.add(textrun);

            return textrun;
        }

        final Object obj = list.get(list.size() - 1);

        if (obj instanceof RtfTextrun) {
            // if the last child is a RtfTextrun, return it
            return (RtfTextrun) obj;
        }

        // add a new RtfTextrun as the last child
        final RtfTextrun textrun = new RtfTextrun(container, writer, attrs);
        list.add(textrun);

        return textrun;
    }

    /**
     * specify, if the last paragraph control word (\par) should be suppressed.
     *
     * @param bSuppress
     *            true, if the last \par should be suppressed
     */
    public void setSuppressLastPar(final boolean bSuppress) {
        this.bSuppressLastPar = bSuppress;
    }

    /**
     * write RTF code of all our children
     *
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeRtfContent() throws IOException {
        /**
         * TODO: The textrun's children are iterated twice: 1. To determine the
         * last RtfParagraphBreak 2. To write the children Maybe this can be
         * done more efficient.
         */

        final boolean bHasTableCellParent = getParentOfClass(RtfTableCell.class) != null;
        final RtfAttributes attrBlockLevel = new RtfAttributes();

        // determine, if this RtfTextrun is the last child of its parent
        boolean bLast = false;
        for (final Iterator it = this.parent.getChildren().iterator(); it
                .hasNext();) {
            if (it.next() == this) {
                bLast = !it.hasNext();
                break;
            }
        }

        // get last RtfParagraphBreak, which is not followed by any visible
        // child
        RtfParagraphBreak lastParagraphBreak = null;
        if (bLast) {
            RtfElement aBefore = null;
            for (final Iterator it = getChildren().iterator(); it.hasNext();) {
                final RtfElement e = (RtfElement) it.next();
                if (e instanceof RtfParagraphBreak) {
                    // If the element before was a paragraph break or a bookmark
                    // they will be hidden and are therefore not considered as
                    // visible
                    if (!(aBefore instanceof RtfParagraphBreak)
                            && !(aBefore instanceof RtfBookmark)) {
                        lastParagraphBreak = (RtfParagraphBreak) e;
                    }
                } else {
                    if (!(e instanceof RtfOpenGroupMark)
                            && !(e instanceof RtfCloseGroupMark) && e.isEmpty()) {
                        lastParagraphBreak = null;
                    }
                }
                aBefore = e;
            }
        }

        // may contain for example \intbl
        writeAttributes(this.attrib, null);

        if (this.rtfListItem != null) {
            this.rtfListItem.getRtfListStyle().writeParagraphPrefix(this);
        }

        // write all children
        boolean bPrevPar = false;
        boolean bBookmark = false;
        boolean bFirst = true;
        for (final Iterator it = getChildren().iterator(); it.hasNext();) {
            final RtfElement e = (RtfElement) it.next();
            final boolean bRtfParagraphBreak = e instanceof RtfParagraphBreak;

            if (bHasTableCellParent) {
                attrBlockLevel.set(e.getRtfAttributes());
            }

            /**
             * -Write RtfParagraphBreak only, if the previous visible child
             * was't also a RtfParagraphBreak. -Write RtfParagraphBreak only, if
             * it is not the first visible child. -If the RtfTextrun is the last
             * child of its parent, write a RtfParagraphBreak only, if it is not
             * the last child. -If the RtfParagraphBreak can not be hidden (e.g.
             * a table cell requires it) it is also written
             */
            boolean bHide = false;
            bHide = bRtfParagraphBreak;
            bHide = bHide
                    && (bPrevPar || bFirst || this.bSuppressLastPar && bLast
                            && lastParagraphBreak != null
                            && e == lastParagraphBreak || bBookmark)
                            && ((RtfParagraphBreak) e).canHide();

            if (!bHide) {
                newLine();
                e.writeRtf();

                if (this.rtfListItem != null && e instanceof RtfParagraphBreak) {
                    this.rtfListItem.getRtfListStyle().writeParagraphPrefix(
                            this);
                }
            }

            if (e instanceof RtfParagraphBreak) {
                bPrevPar = true;
            } else if (e instanceof RtfBookmark) {
                bBookmark = true;
            } else if (e instanceof RtfCloseGroupMark) {
                // do nothing
            } else if (e instanceof RtfOpenGroupMark) {
                // do nothing
            } else {
                bPrevPar = bPrevPar && e.isEmpty();
                bFirst = bFirst && e.isEmpty();
                bBookmark = false;
            }
        } // for (Iterator it = ...)

        //
        if (bHasTableCellParent) {
            writeAttributes(attrBlockLevel, null);
        }

    }

    /**
     * Set the parent list-item of the textrun.
     *
     * @param listItem
     *            parent list-item of the textrun
     */
    public void setRtfListItem(final RtfListItem listItem) {
        this.rtfListItem = listItem;
    }

    /**
     * Gets the parent list-item of the textrun.
     *
     * @return parent list-item of the textrun
     */
    public RtfListItem getRtfListItem() {
        return this.rtfListItem;
    }
}
