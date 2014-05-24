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

/* $Id: ContentLayoutManager.java 1052561 2010-12-24 19:28:11Z spepping $ */

package org.apache.fop.layoutmgr.inline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.pagination.Title;
import org.apache.fop.layoutmgr.AbstractBaseLayoutManager;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthPossPosIter;
import org.apache.fop.layoutmgr.KnuthSequence;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.PageSequenceLayoutManager;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.SpaceSpecifier;

/**
 * Content Layout Manager. For use with objects that contain inline areas such
 * as leader use-content and title.
 */
@Slf4j
public class ContentLayoutManager extends AbstractBaseLayoutManager implements
InlineLevelLayoutManager {

    private final Area holder;
    private int stackSize;
    private LayoutManager parentLM;
    private InlineLevelLayoutManager childLM = null;

    /**
     * Constructs a new ContentLayoutManager
     *
     * @param area
     *            The parent area
     * @param parentLM
     *            the parent layout manager
     */
    public ContentLayoutManager(final Area area, final LayoutManager parentLM) {
        this.holder = area;
        this.parentLM = parentLM;
    }

    /**
     * Constructor using a fo:title formatting object and its
     * PageSequenceLayoutManager parent. throws IllegalStateException if the
     * foTitle has no children. TODO: convert IllegalStateException to
     * FOPException; also in makeLayoutManager and makeContentLayoutManager and
     * callers.
     *
     * @param pslm
     *            the PageSequenceLayoutManager parent of this LM
     * @param foTitle
     *            the Title FO for which this LM is made
     */
    public ContentLayoutManager(final PageSequenceLayoutManager pslm,
            final Title foTitle) {
        // get breaks then add areas to title
        this.parentLM = pslm;
        this.holder = new LineArea();

        // setUserAgent(foTitle.getUserAgent());

        // use special layout manager to add the inline areas
        // to the Title.
        try {
            final LayoutManager lm = pslm.getLayoutManagerMaker()
                    .makeLayoutManager(foTitle);
            addChildLM(lm);
            fillArea(lm);
        } catch (final IllegalStateException e) {
            log.warn("Title has no content");
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        // Empty
    }

    private void fillArea(final LayoutManager curLM) {

        final int ipd = 1000000;

        final LayoutContext childLC = new LayoutContext(LayoutContext.NEW_AREA);
        childLC.setLeadingSpace(new SpaceSpecifier(false));
        childLC.setTrailingSpace(new SpaceSpecifier(false));
        childLC.setRefIPD(ipd);

        final int lineHeight = 14000;
        final int lead = 12000;
        final int follow = 2000;

        final int halfLeading = (lineHeight - lead - follow) / 2;
        // height before baseline
        final int lineLead = lead + halfLeading;
        // maximum size of top and bottom alignment
        final int maxtb = follow + halfLeading;
        // max size of middle alignment below baseline
        int middlefollow = maxtb;

        this.stackSize = 0;

        final List contentList = getNextKnuthElements(childLC,
                Constants.EN_START);
        final ListIterator contentIter = contentList.listIterator();
        while (contentIter.hasNext()) {
            final KnuthElement element = (KnuthElement) contentIter.next();
            if (element instanceof KnuthInlineBox) {
                final KnuthInlineBox box = (KnuthInlineBox) element;
                // TODO handle alignment here?
            }
        }

        if (maxtb - lineLead > middlefollow) {
            middlefollow = maxtb - lineLead;
        }

        final LayoutContext lc = new LayoutContext(0);

        lc.setFlags(LayoutContext.RESOLVE_LEADING_SPACE, true);
        lc.setLeadingSpace(new SpaceSpecifier(false));
        lc.setTrailingSpace(new SpaceSpecifier(false));
        final KnuthPossPosIter contentPosIter = new KnuthPossPosIter(
                contentList, 0, contentList.size());
        curLM.addAreas(contentPosIter, lc);
    }

    /** {@inheritDoc} */
    @Override
    public void addAreas(final PositionIterator posIter,
            final LayoutContext context) {
        // add the content areas
        // the area width has already been adjusted, and it must remain
        // unchanged
        // so save its value before calling addAreas, and set it again
        // afterwards
        final int savedIPD = ((InlineArea) this.holder).getIPD();
        // set to zero the ipd adjustment ratio, to avoid spaces in the pattern
        // to be modified
        final LayoutContext childContext = new LayoutContext(context);
        childContext.setIPDAdjust(0.0);
        this.childLM.addAreas(posIter, childContext);
        ((InlineArea) this.holder).setIPD(savedIPD);
    }

    /** @return stack size */
    public int getStackingSize() {
        return this.stackSize;
    }

    /** {@inheritDoc} */
    @Override
    public Area getParentArea(final Area childArea) {
        return this.holder;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public void addChildArea(final Area childArea) {
        this.holder.addChildArea(childArea);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParent(final LayoutManager lm) {
        this.parentLM = lm;
    }

    /** {@inheritDoc} */
    @Override
    public LayoutManager getParent() {
        return this.parentLM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFinished() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFinished(final boolean isFinished) {
        // to be done
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean createNextChildLMs(final int pos) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List getChildLMs() {
        final List childLMs = new ArrayList(1);
        childLMs.add(this.childLM);
        return childLMs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addChildLM(final LayoutManager lm) {
        if (lm == null) {
            return;
        }
        lm.setParent(this);
        this.childLM = (InlineLevelLayoutManager) lm;
        log.trace(this.getClass().getName() + ": Adding child LM "
                + lm.getClass().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addChildLMs(final List newLMs) {
        if (newLMs == null || newLMs.size() == 0) {
            return;
        }
        final ListIterator iter = newLMs.listIterator();
        while (iter.hasNext()) {
            final LayoutManager lm = (LayoutManager) iter.next();
            addChildLM(lm);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        final List contentList = new LinkedList();
        List returnedList;

        this.childLM.initialize();
        while (!this.childLM.isFinished()) {
            // get KnuthElements from childLM
            returnedList = this.childLM
                    .getNextKnuthElements(context, alignment);

            if (returnedList != null) {
                // move elements to contentList, and accumulate their size
                KnuthElement contentElement;
                while (returnedList.size() > 0) {
                    final Object obj = returnedList.remove(0);
                    if (obj instanceof KnuthSequence) {
                        final KnuthSequence ks = (KnuthSequence) obj;
                        for (final Iterator it = ks.iterator(); it.hasNext();) {
                            contentElement = (KnuthElement) it.next();
                            this.stackSize += contentElement.getWidth();
                            contentList.add(contentElement);
                        }
                    } else {
                        contentElement = (KnuthElement) obj;
                        this.stackSize += contentElement.getWidth();
                        contentList.add(contentElement);
                    }
                }
            }
        }

        setFinished(true);
        return contentList;
    }

    /** {@inheritDoc} */
    @Override
    public List addALetterSpaceTo(final List oldList) {
        return oldList;
    }

    /** {@inheritDoc} */
    @Override
    public List addALetterSpaceTo(final List oldList, final int depth) {
        return addALetterSpaceTo(oldList);
    }

    /** {@inheritDoc} */
    @Override
    public String getWordChars(final Position pos) {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public void hyphenate(final Position pos, final HyphContext hc) {
    }

    /** {@inheritDoc} */
    @Override
    public boolean applyChanges(final List oldList) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean applyChanges(final List oldList, final int depth) {
        return applyChanges(oldList);
    }

    /** {@inheritDoc} */
    @Override
    public List getChangedKnuthElements(final List oldList, final int alignment) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List getChangedKnuthElements(final List oldList,
            final int alignment, final int depth) {
        return getChangedKnuthElements(oldList, alignment);
    }

    /** {@inheritDoc} */
    @Override
    public PageSequenceLayoutManager getPSLM() {
        return this.parentLM.getPSLM();
    }

    // --------- Property Resolution related functions --------- //

    /**
     * Returns the IPD of the content area
     *
     * @return the IPD of the content area
     */
    @Override
    public int getContentAreaIPD() {
        return this.holder.getIPD();
    }

    /**
     * Returns the BPD of the content area
     *
     * @return the BPD of the content area
     */
    @Override
    public int getContentAreaBPD() {
        return this.holder.getBPD();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getGeneratesReferenceArea() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getGeneratesBlockArea() {
        return getGeneratesLineArea() || this.holder instanceof Block;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getGeneratesLineArea() {
        return this.holder instanceof LineArea;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position notifyPos(final Position pos) {
        return pos;
    }

}
