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

/* $Id: PageNumberCitationLastLayoutManager.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.layoutmgr.inline;

import org.apache.fop.area.PageViewport;
import org.apache.fop.area.Resolvable;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.UnresolvedPageNumber;
import org.apache.fop.fo.flow.PageNumberCitationLast;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LayoutManager;

/**
 * LayoutManager for the fo:page-number-citation-last formatting object
 */
public class PageNumberCitationLastLayoutManager extends
        AbstractPageNumberCitationLayoutManager {

    /**
     * Constructor
     *
     * @param node
     *            the formatting object that creates this area TODO better
     *            retrieval of font info
     */
    public PageNumberCitationLastLayoutManager(final PageNumberCitationLast node) {
        super(node);
        this.fobj = node;
    }

    /** {@inheritDoc} */
    @Override
    public InlineArea get(final LayoutContext context) {
        this.curArea = getPageNumberCitationLastInlineArea(this.parentLayoutManager);
        return this.curArea;
    }

    /**
     * if id can be resolved then simply return a word, otherwise return a
     * resolvable area
     */
    private InlineArea getPageNumberCitationLastInlineArea(
            final LayoutManager parentLM) {
        TextArea text = null;
        final int level = getBidiLevel();
        if (!getPSLM().associateLayoutManagerID(this.fobj.getRefId())) {
            text = new UnresolvedPageNumber(this.fobj.getRefId(), this.font,
                    UnresolvedPageNumber.LAST);
            getPSLM()
                    .addUnresolvedArea(this.fobj.getRefId(), (Resolvable) text);
            final String str = "MMM"; // reserve three spaces for page number
            final int width = getStringWidth(str);
            text.setBidiLevel(level);
            text.setIPD(width);
            this.resolved = false;
        } else {
            final PageViewport page = getPSLM().getLastPVWithID(
                    this.fobj.getRefId());
            final String str = page.getPageNumberString();
            // get page string from parent, build area
            text = new TextArea();
            final int width = getStringWidth(str);
            text.setBidiLevel(level);
            text.addWord(str, 0, level);
            text.setIPD(width);
            this.resolved = true;
        }

        updateTextAreaTraits(text);

        return text;
    }
}
