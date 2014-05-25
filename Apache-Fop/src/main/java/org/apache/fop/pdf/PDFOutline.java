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

/* $Id: PDFOutline.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * This represents a single Outline object in a PDF, including the root Outlines
 * object. Outlines provide the bookmark bar, usually rendered to the right of a
 * PDF document in user agents such as Acrobat Reader.
 * </p>
 *
 * <p>
 * This work was authored by Kelly A. Campbell.
 * </p>
 */
@Slf4j
public class PDFOutline extends PDFObject {

    /**
     * list of sub-entries (outline objects)
     */
    private final List subentries;

    /**
     * parent outline object. Root Outlines parent is null
     */
    private PDFOutline parent;

    private PDFOutline prev;
    private PDFOutline next;

    private PDFOutline first;
    private PDFOutline last;

    private int count;

    // whether to show this outline item's child outline items
    private boolean openItem = false;

    /**
     * title to display for the bookmark entry
     */
    private String title;

    private final String actionRef;

    /**
     * Create a PDF outline with the title and action.
     *
     * @param title
     *            the title of the outline entry (can only be null for root
     *            Outlines obj)
     * @param action
     *            the action for this outline
     * @param openItem
     *            indicator of whether child items are visible or not
     */
    public PDFOutline(final String title, final String action,
            final boolean openItem) {
        super();
        this.subentries = new java.util.ArrayList();
        this.count = 0;
        this.parent = null;
        this.prev = null;
        this.next = null;
        this.first = null;
        this.last = null;
        this.title = title;
        this.actionRef = action;
        this.openItem = openItem;
    }

    /**
     * Set the title of this Outline object.
     *
     * @param t
     *            the title of the outline
     */
    public void setTitle(final String t) {
        this.title = t;
    }

    /**
     * Add a sub element to this outline.
     *
     * @param outline
     *            a sub outline
     */
    public void addOutline(final PDFOutline outline) {
        if (this.subentries.size() > 0) {
            outline.prev = (PDFOutline) this.subentries.get(this.subentries
                    .size() - 1);
            outline.prev.next = outline;
        } else {
            this.first = outline;
        }

        this.subentries.add(outline);
        outline.parent = this;

        // note: count is not just the immediate children
        incrementCount();

        this.last = outline;
    }

    /**
     * Increment the number of subentries and descendants.
     */
    private void incrementCount() {
        // count is a total of our immediate subentries
        // and all descendent subentries
        this.count++;
        if (this.parent != null) {
            this.parent.incrementCount();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] toPDF() {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream(128);
        try {
            bout.write(encode("<<"));
            if (this.parent == null) {
                // root Outlines object
                if (this.first != null && this.last != null) {
                    bout.write(encode(" /First " + this.first.referencePDF()
                            + "\n"));
                    bout.write(encode(" /Last " + this.last.referencePDF()
                            + "\n"));
                    // no count... we start with the outline completely closed
                    // for now
                }
            } else {
                // subentry Outline item object
                bout.write(encode(" /Title "));
                bout.write(encodeText(this.title));
                bout.write(encode("\n"));
                bout.write(encode(" /Parent " + this.parent.referencePDF()
                        + "\n"));
                if (this.prev != null) {
                    bout.write(encode(" /Prev " + this.prev.referencePDF()
                            + "\n"));
                }
                if (this.next != null) {
                    bout.write(encode(" /Next " + this.next.referencePDF()
                            + "\n"));
                }
                if (this.first != null && this.last != null) {
                    bout.write(encode(" /First " + this.first.referencePDF()
                            + "\n"));
                    bout.write(encode(" /Last " + this.last.referencePDF()
                            + "\n"));
                }
                if (this.count > 0) {
                    bout.write(encode(" /Count " + (this.openItem ? "" : "-")
                            + this.count + "\n"));
                }
                if (this.actionRef != null) {
                    bout.write(encode(" /A " + this.actionRef + "\n"));
                }
            }
            bout.write(encode(">>"));
        } catch (final IOException ioe) {
            log.error("Ignored I/O exception", ioe);
        }
        return bout.toByteArray();
    }

}
