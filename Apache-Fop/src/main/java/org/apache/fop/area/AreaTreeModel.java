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

/* $Id: AreaTreeModel.java 1242848 2012-02-10 16:51:08Z phancock $ */

package org.apache.fop.area;

// Java
import java.util.List;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

import org.xml.sax.SAXException;

/**
 * This is the model for the area tree object. The model implementation can
 * handle the page sequence, page and off-document items. The methods to access
 * the page viewports can only assume the PageViewport is valid as it remains
 * for the life of the area tree model.
 */
@Slf4j
public class AreaTreeModel {
    private List<PageSequence> pageSequenceList = null;
    private int currentPageIndex = 0;

    /** the current page sequence */
    protected PageSequence currentPageSequence;

    /**
     * Create a new store pages model
     */
    public AreaTreeModel() {
        this.pageSequenceList = new java.util.ArrayList<PageSequence>();
    }

    /**
     * Start a page sequence on this model.
     *
     * @param pageSequence
     *            the page sequence about to start
     */
    public void startPageSequence(final PageSequence pageSequence) {
        if (pageSequence == null) {
            throw new NullPointerException("pageSequence must not be null");
        }
        if (this.currentPageSequence != null) {
            this.currentPageIndex += this.currentPageSequence.getPageCount();
        }
        this.currentPageSequence = pageSequence;
        this.pageSequenceList.add(this.currentPageSequence);
    }

    /**
     * Add a page to this model.
     *
     * @param page
     *            the page to add to the model.
     */
    public void addPage(final PageViewport page) {
        this.currentPageSequence.addPage(page);
        page.setPageIndex(this.currentPageIndex
                + this.currentPageSequence.getPageCount() - 1);
        page.setPageSequence(this.currentPageSequence);
    }

    /**
     * Handle an OffDocumentItem
     *
     * @param ext
     *            the extension to handle
     */
    public void handleOffDocumentItem(final OffDocumentItem ext) {
    };

    /**
     * Signal the end of the document for any processing.
     *
     * @throws SAXException
     *             if a problem was encountered.
     */
    public void endDocument() throws SAXException {
    };

    /**
     * Returns the currently active page-sequence.
     *
     * @return the currently active page-sequence
     */
    public PageSequence getCurrentPageSequence() {
        return this.currentPageSequence;
    }

    /**
     * Get the page sequence count.
     *
     * @return the number of page sequences in the document.
     */
    public int getPageSequenceCount() {
        return this.pageSequenceList.size();
    }

    /**
     * Get the page count.
     *
     * @param seq
     *            the page sequence to count.
     * @return returns the number of pages in a page sequence
     */
    public int getPageCount(final int seq) {
        return this.pageSequenceList.get(seq - 1).getPageCount();
    }

    /**
     * Get the page for a position in the document.
     *
     * @param seq
     *            the page sequence number
     * @param count
     *            the page count in the sequence
     * @return the PageViewport for the particular page
     */
    public PageViewport getPage(final int seq, final int count) {
        return this.pageSequenceList.get(seq - 1).getPage(count);
    }

    /**
     *
     * @param locale
     *            The locale of the document
     */
    public void setDocumentLocale(final Locale locale) {
    }
}
