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

/* $Id: AbstractLayoutManager.java 1334058 2012-05-04 16:52:35Z gadams $ */

package org.apache.fop.layoutmgr;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Area;
import org.apache.fop.area.AreaTreeObject;
import org.apache.fop.area.PageViewport;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.flow.Marker;
import org.apache.fop.fo.flow.RetrieveMarker;
import org.apache.xmlgraphics.util.QName;

/**
 * The base class for most LayoutManagers.
 */
@Slf4j
public abstract class AbstractLayoutManager extends AbstractBaseLayoutManager
implements Constants {

    /** Parent LayoutManager for this LayoutManager */
    protected LayoutManager parentLayoutManager;
    /** List of child LayoutManagers */
    protected List<LayoutManager> childLMs;
    /** Iterator for child LayoutManagers */
    protected ListIterator fobjIter;
    /** Marker map for markers related to this LayoutManager */
    private Map<String, Marker> markers;

    /** True if this LayoutManager has handled all of its content. */
    private boolean isFinished;

    /** child LM during getNextKnuthElement phase */
    protected LayoutManager curChildLM;

    /** child LM iterator during getNextKnuthElement phase */
    protected ListIterator<LayoutManager> childLMiter;

    private int lastGeneratedPosition = -1;
    private int smallestPosNumberChecked = Integer.MAX_VALUE;

    /**
     * Abstract layout manager.
     */
    public AbstractLayoutManager() {
    }

    /**
     * Abstract layout manager.
     *
     * @param fo
     *            the formatting object for this layout manager
     */
    public AbstractLayoutManager(final FObj fo) {
        super(fo);
        if (fo == null) {
            throw new IllegalStateException("Null formatting object found.");
        }
        this.markers = fo.getMarkers();
        this.fobjIter = fo.getChildNodes();
        this.childLMiter = new LMiter(this);
    }

    /** {@inheritDoc} */
    @Override
    public void setParent(final LayoutManager lm) {
        this.parentLayoutManager = lm;
    }

    /** {@inheritDoc} */
    @Override
    public LayoutManager getParent() {
        return this.parentLayoutManager;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        // Empty
    }

    /**
     * Return currently active child LayoutManager or null if all children have
     * finished layout. Note: child must implement LayoutManager! If it doesn't,
     * skip it and print a warning.
     *
     * @return the current child LayoutManager
     */
    protected LayoutManager getChildLM() {
        if (this.curChildLM != null && !this.curChildLM.isFinished()) {
            return this.curChildLM;
        }
        if (this.childLMiter.hasNext()) {
            this.curChildLM = this.childLMiter.next();
            this.curChildLM.initialize();
            return this.curChildLM;
        }
        return null;
    }

    /**
     * Set currently active child layout manager.
     *
     * @param childLM
     *            the child layout manager
     */
    protected void setCurrentChildLM(final LayoutManager childLM) {
        this.curChildLM = childLM;
        this.childLMiter = new LMiter(this);
        do {
            this.curChildLM = this.childLMiter.next();
        } while (this.curChildLM != childLM);
    }

    /**
     * Return indication if getChildLM will return another LM.
     *
     * @return true if another child LM is still available
     */
    protected boolean hasNextChildLM() {
        return this.childLMiter.hasNext();
    }

    /**
     * Tell whether this LayoutManager has handled all of its content.
     *
     * @return True if there are no more break possibilities, ie. the last one
     *         returned represents the end of the content.
     */
    @Override
    public boolean isFinished() {
        return this.isFinished;
    }

    /**
     * Set the flag indicating the LayoutManager has handled all of its content.
     *
     * @param fin
     *            the flag value to be set
     */
    @Override
    public void setFinished(final boolean fin) {
        this.isFinished = fin;
    }

    /** {@inheritDoc} */
    @Override
    public void addAreas(final PositionIterator posIter,
            final LayoutContext context) {
    }

    /** {@inheritDoc} */
    @Override
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        log.warn("null implementation of getNextKnuthElements() called!");
        setFinished(true);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List getChangedKnuthElements(final List oldList, final int alignment) {
        log.warn("null implementation of getChangeKnuthElement() called!");
        return null;
    }

    /**
     * Return an Area which can contain the passed childArea. The childArea may
     * not yet have any content, but it has essential traits set. In general, if
     * the LayoutManager already has an Area it simply returns it. Otherwise, it
     * makes a new Area of the appropriate class. It gets a parent area for its
     * area by calling its parent LM. Finally, based on the dimensions of the
     * parent area, it initializes its own area. This includes setting the
     * content IPD and the maximum BPD.
     *
     * @param childArea
     *            the child area for which the parent area is wanted
     * @return the parent area for the given child
     */
    @Override
    public Area getParentArea(final Area childArea) {
        return null;
    }

    /**
     * Add a child area to the current area. If this causes the maximum
     * dimension of the current area to be exceeded, the parent LM is called to
     * add it.
     *
     * @param childArea
     *            the child area to be added
     */
    @Override
    public void addChildArea(final Area childArea) {
    }

    /**
     * Create the LM instances for the children of the formatting object being
     * handled by this LM.
     *
     * @param size
     *            the requested number of child LMs
     * @return the list with the preloaded child LMs
     */
    protected List<LayoutManager> createChildLMs(final int size) {
        if (this.fobjIter == null) {
            return null;
        }
        final List<LayoutManager> newLMs = new ArrayList<LayoutManager>(size);
        while (this.fobjIter.hasNext() && newLMs.size() < size) {
            final Object theobj = this.fobjIter.next();
            if (theobj instanceof FONode) {
                FONode foNode = (FONode) theobj;
                if (foNode instanceof RetrieveMarker) {
                    foNode = getPSLM().resolveRetrieveMarker(
                            (RetrieveMarker) foNode);
                }
                if (foNode != null) {
                    getPSLM().getLayoutManagerMaker().makeLayoutManagers(
                            foNode, newLMs);
                }
            }
        }
        return newLMs;
    }

    /** {@inheritDoc} */
    @Override
    public PageSequenceLayoutManager getPSLM() {
        return this.parentLayoutManager.getPSLM();
    }

    /**
     * @see PageSequenceLayoutManager#getCurrentPage()
     * @return the {@link Page} instance corresponding to the current page
     */
    public Page getCurrentPage() {
        return getPSLM().getCurrentPage();
    }

    /** @return the current page viewport */
    public PageViewport getCurrentPV() {
        return getPSLM().getCurrentPage().getPageViewport();
    }

    /** {@inheritDoc} */
    @Override
    public boolean createNextChildLMs(final int pos) {
        final List<LayoutManager> newLMs = createChildLMs(pos + 1
                - this.childLMs.size());
        addChildLMs(newLMs);
        return pos < this.childLMs.size();
    }

    /** {@inheritDoc} */
    @Override
    public List<LayoutManager> getChildLMs() {
        if (this.childLMs == null) {
            this.childLMs = new java.util.ArrayList<LayoutManager>(10);
        }
        return this.childLMs;
    }

    /** {@inheritDoc} */
    @Override
    public void addChildLM(final LayoutManager lm) {
        if (lm == null) {
            return;
        }
        lm.setParent(this);
        if (this.childLMs == null) {
            this.childLMs = new java.util.ArrayList<LayoutManager>(10);
        }
        this.childLMs.add(lm);
        if (log.isTraceEnabled()) {
            log.trace(this.getClass().getName() + ": Adding child LM "
                    + lm.getClass().getName());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addChildLMs(final List newLMs) {
        if (newLMs == null || newLMs.size() == 0) {
            return;
        }
        final ListIterator<LayoutManager> iter = newLMs.listIterator();
        while (iter.hasNext()) {
            addChildLM(iter.next());
        }
    }

    /**
     * Adds a Position to the Position participating in the first|last
     * determination by assigning it a unique position index.
     *
     * @param pos
     *            the Position
     * @return the same Position but with a position index
     */
    @Override
    public Position notifyPos(final Position pos) {
        if (pos.getIndex() >= 0) {
            throw new IllegalStateException("Position already got its index");
        }

        pos.setIndex(++this.lastGeneratedPosition);
        return pos;
    }

    private void verifyNonNullPosition(final Position pos) {
        if (pos == null || pos.getIndex() < 0) {
            throw new IllegalArgumentException(
                    "Only non-null Positions with an index can be checked");
        }
    }

    /**
     * Indicates whether the given Position is the first area-generating
     * Position of this LM.
     *
     * @param pos
     *            the Position (must be one with a position index)
     * @return True if it is the first Position
     */
    public boolean isFirst(final Position pos) {
        // log.trace("isFirst() smallestPosNumberChecked=" +
        // smallestPosNumberChecked + " " + pos);
        verifyNonNullPosition(pos);
        if (pos.getIndex() == this.smallestPosNumberChecked) {
            return true;
        } else if (pos.getIndex() < this.smallestPosNumberChecked) {
            this.smallestPosNumberChecked = pos.getIndex();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Indicates whether the given Position is the last area-generating Position
     * of this LM.
     *
     * @param pos
     *            the Position (must be one with a position index)
     * @return True if it is the last Position
     */
    public boolean isLast(final Position pos) {
        verifyNonNullPosition(pos);
        return pos.getIndex() == this.lastGeneratedPosition && isFinished();
    }

    /**
     * Transfers foreign attributes from the formatting object to the area.
     *
     * @param targetArea
     *            the area to set the attributes on
     */
    protected void transferForeignAttributes(final AreaTreeObject targetArea) {
        final Map<QName, String> atts = this.fobj.getForeignAttributes();
        targetArea.setForeignAttributes(atts);
    }

    /**
     * Transfers extension attachments from the formatting object to the area.
     *
     * @param targetArea
     *            the area to set the extensions on
     */
    protected void transferExtensionAttachments(final AreaTreeObject targetArea) {
        if (this.fobj.hasExtensionAttachments()) {
            targetArea.setExtensionAttachments(this.fobj
                    .getExtensionAttachments());
        }
    }

    /**
     * Transfers extensions (foreign attributes and extension attachments) from
     * the formatting object to the area.
     *
     * @param targetArea
     *            the area to set the extensions on
     */
    protected void transferExtensions(final AreaTreeObject targetArea) {
        transferForeignAttributes(targetArea);
        transferExtensionAttachments(targetArea);
    }

    /**
     * Registers the FO's markers on the current PageViewport
     *
     * @param isStarting
     *            boolean indicating whether the markers qualify as 'starting'
     * @param isFirst
     *            boolean indicating whether the markers qualify as 'first'
     * @param isLast
     *            boolean indicating whether the markers qualify as 'last'
     */
    protected void addMarkersToPage(final boolean isStarting,
            final boolean isFirst, final boolean isLast) {
        if (this.markers != null) {
            getCurrentPV()
            .addMarkers(this.markers, isStarting, isFirst, isLast);
        }
    }

    /**
     * Registers the FO's id on the current PageViewport
     */
    protected void addId() {
        if (this.fobj != null) {
            getPSLM().addIDToPage(this.fobj.getId());
        }
    }

    /**
     * Notifies the {@link PageSequenceLayoutManager} that layout for this LM
     * has ended.
     */
    protected void notifyEndOfLayout() {
        if (this.fobj != null) {
            getPSLM().notifyEndOfLayout(this.fobj.getId());
        }
    }

    /**
     * Checks to see if the incoming {@link Position} is the last one for this
     * LM, and if so, calls {@link #notifyEndOfLayout()} and cleans up.
     *
     * @param pos
     *            the {@link Position} to check
     */
    protected void checkEndOfLayout(final Position pos) {
        if (pos != null && pos.getLM() == this && isLast(pos)) {

            notifyEndOfLayout();

            /*
             * References to the child LMs are no longer needed
             */
            this.childLMs = null;
            this.curChildLM = null;
            this.childLMiter = null;

            /*
             * markers that qualify have been transferred to the page
             */
            this.markers = null;

            /*
             * References to the FO's children can be released if the LM is a
             * descendant of the FlowLM. For static-content the FO may still be
             * needed on following pages.
             */
            LayoutManager lm = this.parentLayoutManager;
            while (!(lm instanceof FlowLayoutManager || lm instanceof PageSequenceLayoutManager)) {
                lm = lm.getParent();
            }
            if (lm instanceof FlowLayoutManager) {
                this.fobj.clearChildNodes();
                this.fobjIter = null;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return super.toString()
                + (this.fobj != null ? "{fobj = " + this.fobj.toString() + "}"
                        : "");
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        this.isFinished = false;
        this.curChildLM = null;
        this.childLMiter = new LMiter(this);
        /* Reset all the children LM that have been created so far. */
        for (final LayoutManager childLM : getChildLMs()) {
            childLM.reset();
        }
        if (this.fobj != null) {
            this.markers = this.fobj.getMarkers();
        }
        this.lastGeneratedPosition = -1;
    }

}
