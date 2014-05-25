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

/* $Id: LeaderLayoutManager.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.layoutmgr.inline;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.FilledArea;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.Space;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.flow.Leader;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.layoutmgr.InlineKnuthSequence;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthGlue;
import org.apache.fop.layoutmgr.KnuthPenalty;
import org.apache.fop.layoutmgr.KnuthPossPosIter;
import org.apache.fop.layoutmgr.KnuthSequence;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LeafPosition;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.traits.MinOptMax;

/**
 * LayoutManager for the fo:leader formatting object
 */
public class LeaderLayoutManager extends LeafNodeLayoutManager {
    private final Leader fobj;
    private Font font = null;

    private List contentList = null;
    private ContentLayoutManager clm = null;

    private int contentAreaIPD = 0;

    /**
     * Constructor
     *
     * @param node
     *            the formatting object that creates this area
     */
    public LeaderLayoutManager(final Leader node) {
        super(node);
        this.fobj = node;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        final FontInfo fi = this.fobj.getFOEventHandler().getFontInfo();
        final FontTriplet[] fontkeys = this.fobj.getCommonFont().getFontState(
                fi);
        this.font = fi.getFontInstance(fontkeys[0],
                this.fobj.getCommonFont().fontSize.getValue(this));
        // the property leader-alignment does not affect vertical positioning
        // (see section 7.21.1 in the XSL Recommendation)
        // setAlignment(node.getLeaderAlignment());
        setCommonBorderPaddingBackground(this.fobj
                .getCommonBorderPaddingBackground());
    }

    /**
     * Return the inline area for this leader.
     * 
     * @param context
     *            the layout context
     * @return the inline area
     */
    @Override
    public InlineArea get(final LayoutContext context) {
        return getLeaderInlineArea(context);
    }

    /**
     * Return the allocated IPD for this area.
     * 
     * @param refIPD
     *            the IPD of the reference area
     * @return the allocated IPD
     */
    @Override
    protected MinOptMax getAllocationIPD(final int refIPD) {
        return getLeaderAllocIPD(refIPD);
    }

    private MinOptMax getLeaderAllocIPD(final int ipd) {
        // length of the leader
        int borderPaddingWidth = 0;
        if (this.commonBorderPaddingBackground != null) {
            borderPaddingWidth = this.commonBorderPaddingBackground
                    .getIPPaddingAndBorder(false, this);
        }
        setContentAreaIPD(ipd - borderPaddingWidth);
        final int opt = this.fobj.getLeaderLength().getOptimum(this)
                .getLength().getValue(this)
                - borderPaddingWidth;
        final int min = this.fobj.getLeaderLength().getMinimum(this)
                .getLength().getValue(this)
                - borderPaddingWidth;
        final int max = this.fobj.getLeaderLength().getMaximum(this)
                .getLength().getValue(this)
                - borderPaddingWidth;
        return MinOptMax.getInstance(min, opt, max);
    }

    private InlineArea getLeaderInlineArea(final LayoutContext context) {
        InlineArea leaderArea = null;
        final int level = this.fobj.getBidiLevel();
        if (this.fobj.getLeaderPattern() == EN_RULE) {
            if (this.fobj.getRuleStyle() != EN_NONE) {
                final org.apache.fop.area.inline.Leader leader = new org.apache.fop.area.inline.Leader();
                leader.setRuleStyle(this.fobj.getRuleStyle());
                leader.setRuleThickness(this.fobj.getRuleThickness().getValue(
                        this));
                leaderArea = leader;
            } else {
                leaderArea = new Space();
                if (level >= 0) {
                    leaderArea.setBidiLevel(level);
                }
            }
            leaderArea.setBPD(this.fobj.getRuleThickness().getValue(this));
            leaderArea.addTrait(Trait.COLOR, this.fobj.getColor());
            if (level >= 0) {
                leaderArea.setBidiLevel(level);
            }
        } else if (this.fobj.getLeaderPattern() == EN_SPACE) {
            leaderArea = new Space();
            leaderArea.setBPD(this.fobj.getRuleThickness().getValue(this));
            if (level >= 0) {
                leaderArea.setBidiLevel(level);
            }
        } else if (this.fobj.getLeaderPattern() == EN_DOTS) {
            final TextArea t = new TextArea();
            final char dot = '.'; // userAgent.getLeaderDotCharacter();
            int width = this.font.getCharWidth(dot);
            final int[] levels = level < 0 ? null : new int[] { level };
            t.addWord("" + dot, width, null, levels, null, 0);
            t.setIPD(width);
            t.setBPD(width);
            t.setBaselineOffset(width);
            TraitSetter.addFontTraits(t, this.font);
            t.addTrait(Trait.COLOR, this.fobj.getColor());
            Space spacer = null;
            final int widthLeaderPattern = this.fobj.getLeaderPatternWidth()
                    .getValue(this);
            if (widthLeaderPattern > width) {
                spacer = new Space();
                spacer.setIPD(widthLeaderPattern - width);
                if (level >= 0) {
                    spacer.setBidiLevel(level);
                }
                width = widthLeaderPattern;
            }
            final FilledArea fa = new FilledArea();
            fa.setUnitWidth(width);
            fa.addChildArea(t);
            if (spacer != null) {
                fa.addChildArea(spacer);
            }
            fa.setBPD(t.getBPD());
            leaderArea = fa;
        } else if (this.fobj.getLeaderPattern() == EN_USECONTENT) {
            if (this.fobj.getChildNodes() == null) {
                final InlineLevelEventProducer eventProducer = InlineLevelEventProducer.Provider
                        .get(getFObj().getUserAgent().getEventBroadcaster());
                eventProducer
                        .leaderWithoutContent(this, getFObj().getLocator());
                return null;
            }

            // child FOs are assigned to the InlineStackingLM
            this.fobjIter = null;

            // get breaks then add areas to FilledArea
            final FilledArea fa = new FilledArea();

            this.clm = new ContentLayoutManager(fa, this);
            addChildLM(this.clm);

            InlineLayoutManager lm;
            lm = new InlineLayoutManager(this.fobj);
            this.clm.addChildLM(lm);
            lm.initialize();

            final LayoutContext childContext = new LayoutContext(0);
            childContext.setAlignmentContext(context.getAlignmentContext());
            this.contentList = this.clm.getNextKnuthElements(childContext, 0);
            int width = this.clm.getStackingSize();
            if (width != 0) {
                Space spacer = null;
                if (this.fobj.getLeaderPatternWidth().getValue(this) > width) {
                    spacer = new Space();
                    spacer.setIPD(this.fobj.getLeaderPatternWidth().getValue(
                            this)
                            - width);
                    if (level >= 0) {
                        spacer.setBidiLevel(level);
                    }
                    width = this.fobj.getLeaderPatternWidth().getValue(this);
                }
                fa.setUnitWidth(width);
                if (spacer != null) {
                    fa.addChildArea(spacer);
                }
                leaderArea = fa;
            } else {
                // Content collapsed to nothing, so use a space
                leaderArea = new Space();
                leaderArea.setBPD(this.fobj.getRuleThickness().getValue(this));
                leaderArea.setBidiLevel(this.fobj.getBidiLevelRecursive());
            }
        }
        TraitSetter.setProducerID(leaderArea, this.fobj.getId());
        return leaderArea;
    }

    /** {@inheritDoc} */
    @Override
    public void addAreas(final PositionIterator posIter,
            final LayoutContext context) {
        if (this.fobj.getLeaderPattern() != EN_USECONTENT) {
            // use LeafNodeLayoutManager.addAreas()
            super.addAreas(posIter, context);
        } else {
            addId();

            widthAdjustArea(this.curArea, context);

            if (this.commonBorderPaddingBackground != null) {
                // Add border and padding to area
                TraitSetter.setBorderPaddingTraits(this.curArea,
                        this.commonBorderPaddingBackground, false, false, this);
                TraitSetter.addBackground(this.curArea,
                        this.commonBorderPaddingBackground, this);
            }

            // add content areas
            final KnuthPossPosIter contentIter = new KnuthPossPosIter(
                    this.contentList, 0, this.contentList.size());
            this.clm.addAreas(contentIter, context);

            this.parentLayoutManager.addChildArea(this.curArea);

            while (posIter.hasNext()) {
                posIter.next();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        MinOptMax ipd;
        this.curArea = get(context);
        final KnuthSequence seq = new InlineKnuthSequence();

        if (this.curArea == null) {
            setFinished(true);
            return null;
        }

        this.alignmentContext = new AlignmentContext(this.curArea.getBPD(),
                this.fobj.getAlignmentAdjust(),
                this.fobj.getAlignmentBaseline(), this.fobj.getBaselineShift(),
                this.fobj.getDominantBaseline(), context.getAlignmentContext());

        ipd = getAllocationIPD(context.getRefIPD());
        if (this.fobj.getLeaderPattern() == EN_USECONTENT
                && this.curArea instanceof FilledArea) {
            // If we have user supplied content make it fit if we can
            final int unitWidth = ((FilledArea) this.curArea).getUnitWidth();
            if (ipd.getOpt() < unitWidth && unitWidth <= ipd.getMax()) {
                ipd = MinOptMax.getInstance(ipd.getMin(), unitWidth,
                        ipd.getMax());
            }
        }

        // create the AreaInfo object to store the computed values
        this.areaInfo = new AreaInfo((short) 0, ipd, false,
                context.getAlignmentContext());
        this.curArea.setAdjustingInfo(ipd.getStretch(), ipd.getShrink(), 0);

        addKnuthElementsForBorderPaddingStart(seq);

        // node is a fo:Leader
        seq.add(new KnuthInlineBox(0, this.alignmentContext, new LeafPosition(
                this, -1), true));
        seq.add(new KnuthPenalty(0, KnuthElement.INFINITE, false,
                new LeafPosition(this, -1), true));
        if (alignment == EN_JUSTIFY || alignment == 0) {
            seq.add(new KnuthGlue(this.areaInfo.ipdArea, new LeafPosition(this,
                    0), false));
        } else {
            seq.add(new KnuthGlue(this.areaInfo.ipdArea.getOpt(), 0, 0,
                    new LeafPosition(this, 0), false));
        }
        seq.add(new KnuthInlineBox(0, this.alignmentContext, new LeafPosition(
                this, -1), true));

        addKnuthElementsForBorderPaddingEnd(seq);

        setFinished(true);
        return Collections.singletonList(seq);
    }

    /** {@inheritDoc} */
    @Override
    public void hyphenate(final Position pos, final HyphContext hc) {
        // use the AbstractLayoutManager.hyphenate() null implementation
        super.hyphenate(pos, hc);
    }

    /** {@inheritDoc} */
    @Override
    public boolean applyChanges(final List oldList) {
        setFinished(false);
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public List getChangedKnuthElements(final List oldList, final int alignment) {
        if (isFinished()) {
            return null;
        }

        final List returnList = new LinkedList();

        addKnuthElementsForBorderPaddingStart(returnList);

        returnList.add(new KnuthInlineBox(0, this.areaInfo.alignmentContext,
                new LeafPosition(this, -1), true));
        returnList.add(new KnuthPenalty(0, KnuthElement.INFINITE, false,
                new LeafPosition(this, -1), true));
        if (alignment == EN_JUSTIFY || alignment == 0) {
            returnList.add(new KnuthGlue(this.areaInfo.ipdArea,
                    new LeafPosition(this, 0), false));
        } else {
            returnList.add(new KnuthGlue(this.areaInfo.ipdArea.getOpt(), 0, 0,
                    new LeafPosition(this, 0), false));
        }
        returnList.add(new KnuthInlineBox(0, this.areaInfo.alignmentContext,
                new LeafPosition(this, -1), true));

        addKnuthElementsForBorderPaddingEnd(returnList);

        setFinished(true);
        return returnList;
    }

    /** {@inheritDoc} */
    @Override
    public int getBaseLength(final int lengthBase, final FObj fobj) {
        return getParent().getBaseLength(lengthBase, getParent().getFObj());
    }

    /**
     * Returns the IPD of the content area
     * 
     * @return the IPD of the content area
     */
    @Override
    public int getContentAreaIPD() {
        return this.contentAreaIPD;
    }

    private void setContentAreaIPD(final int contentAreaIPD) {
        this.contentAreaIPD = contentAreaIPD;
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        this.childLMs.clear();
        super.reset();
    }

}
