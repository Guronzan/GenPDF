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

/* $Id: InlineParent.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.area.inline;

import java.util.Iterator;
import java.util.List;

import org.apache.fop.area.Area;

/**
 * Inline parent area. This is an inline area that can have other inlines as
 * children.
 */
public class InlineParent extends InlineArea {

    private static final long serialVersionUID = -3047168298770354813L;

    /**
     * The list of inline areas added to this inline parent.
     */
    protected List<InlineArea> inlines = new java.util.ArrayList<InlineArea>();

    /**
     * Controls whether the IPD is automatically adjusted based on the area's
     * children.
     */
    protected transient boolean autoSize;

    /** The offset of the <q>beforest</q> child area of this area. */
    protected int minChildOffset;

    /**
     * The offset of the <q>afterest</q> child area of this area. Offset from
     * the before-edge of this area's content-rectangle and the after-edge of
     * the child area's allocation-rectangle.
     */
    private int maxAfterEdge;

    @Override
    public void addChildArea(final Area c) {
        assert c instanceof InlineArea;
        if (this.inlines.size() == 0) {
            this.autoSize = getIPD() == 0;
        }
        final InlineArea childArea = (InlineArea) c;
        this.inlines.add(childArea);
        // set the parent area for the child area
        childArea.setParentArea(this);
        if (this.autoSize) {
            increaseIPD(childArea.getAllocIPD());
        }
        updateLevel(childArea.getBidiLevel());
        final int childOffset = childArea.getVirtualOffset();
        this.minChildOffset = Math.min(this.minChildOffset, childOffset);
        this.maxAfterEdge = Math.max(this.maxAfterEdge,
                childOffset + childArea.getVirtualBPD());
    }

    @Override
    int getVirtualOffset() {
        return getBlockProgressionOffset() + this.minChildOffset;
    }

    @Override
    int getVirtualBPD() {
        return this.maxAfterEdge - this.minChildOffset;
    }

    /**
     * Get the child areas for this inline parent.
     *
     * @return the list of child areas
     */
    public List<InlineArea> getChildAreas() {
        return this.inlines;
    }

    /**
     * recursively apply the variation factor to all descendant areas
     * 
     * @param variationFactor
     *            the variation factor that must be applied to adjustments
     * @param lineStretch
     *            the total stretch of the line
     * @param lineShrink
     *            the total shrink of the line
     * @return true if there is an UnresolvedArea descendant
     */
    @Override
    public boolean applyVariationFactor(final double variationFactor,
            final int lineStretch, final int lineShrink) {
        boolean hasUnresolvedAreas = false;
        int cumulativeIPD = 0;
        // recursively apply variation factor to descendant areas
        for (int i = 0, len = this.inlines.size(); i < len; i++) {
            final InlineArea inline = this.inlines.get(i);
            hasUnresolvedAreas |= inline.applyVariationFactor(variationFactor,
                    lineStretch, lineShrink);
            cumulativeIPD += inline.getIPD(); // Update this area's IPD based on
                                              // changes to children
        }
        setIPD(cumulativeIPD);

        return hasUnresolvedAreas;
    }

    @Override
    public List collectInlineRuns(List runs) {
        for (final Iterator<InlineArea> it = getChildAreas().iterator(); it
                .hasNext();) {
            final InlineArea ia = it.next();
            runs = ia.collectInlineRuns(runs);
        }
        return runs;
    }

    /**
     * Reset bidirectionality level of all children to default (-1), signalling
     * that they will inherit the level of their parent text area.
     */
    public void resetChildrenLevel() {
        for (final Iterator it = this.inlines.iterator(); it.hasNext();) {
            ((InlineArea) it.next()).resetBidiLevel();
        }
    }

    private void updateLevel(final int newLevel) {
        if (newLevel >= 0) {
            final int curLevel = getBidiLevel();
            if (curLevel >= 0) {
                if (newLevel < curLevel) {
                    setBidiLevel(newLevel);
                }
            } else {
                setBidiLevel(newLevel);
            }
        }
    }

}
