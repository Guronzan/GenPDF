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

/* $Id: InlineLayoutManager.java 1334058 2012-05-04 16:52:35Z gadams $ */

package org.apache.fop.layoutmgr.inline;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Area;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineBlockParent;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.flow.BasicLink;
import org.apache.fop.fo.flow.Inline;
import org.apache.fop.fo.flow.InlineLevel;
import org.apache.fop.fo.flow.Leader;
import org.apache.fop.fo.pagination.Title;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonFont;
import org.apache.fop.fo.properties.CommonMarginInline;
import org.apache.fop.fo.properties.SpaceProperty;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.layoutmgr.BlockKnuthSequence;
import org.apache.fop.layoutmgr.BlockLevelLayoutManager;
import org.apache.fop.layoutmgr.BreakElement;
import org.apache.fop.layoutmgr.InlineKnuthSequence;
import org.apache.fop.layoutmgr.KnuthBox;
import org.apache.fop.layoutmgr.KnuthSequence;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.NonLeafPosition;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.SpaceSpecifier;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.traits.SpaceVal;
import org.apache.fop.util.ListUtil;

/**
 * LayoutManager for objects which stack children in the inline direction, such
 * as Inline or Line
 */
@Slf4j
public class InlineLayoutManager extends InlineStackingLayoutManager {

    private CommonMarginInline inlineProps = null;
    private CommonBorderPaddingBackground borderProps = null;

    private boolean areaCreated = false;
    private LayoutManager lastChildLM = null; // Set when return last breakposs;

    private Font font;

    /** The alignment adjust property */
    protected Length alignmentAdjust;
    /** The alignment baseline property */
    protected int alignmentBaseline = EN_BASELINE;
    /** The baseline shift property */
    protected Length baselineShift;
    /** The dominant baseline property */
    protected int dominantBaseline;
    /** The line height property */
    protected SpaceProperty lineHeight;
    /** The keep-together property */
    // private KeepProperty keepTogether;

    private AlignmentContext alignmentContext = null;

    /**
     * Create an inline layout manager. This is used for fo's that create areas
     * that contain inline areas.
     *
     * @param node
     *            the formatting object that creates the area
     */
    // The node should be FObjMixed
    public InlineLayoutManager(final InlineLevel node) {
        super(node);
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        final InlineLevel fobj = (InlineLevel) this.fobj;

        int padding = 0;

        final FontInfo fi = fobj.getFOEventHandler().getFontInfo();
        final CommonFont commonFont = fobj.getCommonFont();
        final FontTriplet[] fontkeys = commonFont.getFontState(fi);
        this.font = fi.getFontInstance(fontkeys[0],
                commonFont.fontSize.getValue(this));

        this.lineHeight = fobj.getLineHeight();
        this.borderProps = fobj.getCommonBorderPaddingBackground();
        this.inlineProps = fobj.getCommonMarginInline();

        if (fobj instanceof Inline) {
            this.alignmentAdjust = ((Inline) fobj).getAlignmentAdjust();
            this.alignmentBaseline = ((Inline) fobj).getAlignmentBaseline();
            this.baselineShift = ((Inline) fobj).getBaselineShift();
            this.dominantBaseline = ((Inline) fobj).getDominantBaseline();
        } else if (fobj instanceof Leader) {
            this.alignmentAdjust = ((Leader) fobj).getAlignmentAdjust();
            this.alignmentBaseline = ((Leader) fobj).getAlignmentBaseline();
            this.baselineShift = ((Leader) fobj).getBaselineShift();
            this.dominantBaseline = ((Leader) fobj).getDominantBaseline();
        } else if (fobj instanceof BasicLink) {
            this.alignmentAdjust = ((BasicLink) fobj).getAlignmentAdjust();
            this.alignmentBaseline = ((BasicLink) fobj).getAlignmentBaseline();
            this.baselineShift = ((BasicLink) fobj).getBaselineShift();
            this.dominantBaseline = ((BasicLink) fobj).getDominantBaseline();
        }
        if (this.borderProps != null) {
            padding = this.borderProps.getPadding(
                    CommonBorderPaddingBackground.BEFORE, false, this);
            padding += this.borderProps.getBorderWidth(
                    CommonBorderPaddingBackground.BEFORE, false);
            padding += this.borderProps.getPadding(
                    CommonBorderPaddingBackground.AFTER, false, this);
            padding += this.borderProps.getBorderWidth(
                    CommonBorderPaddingBackground.AFTER, false);
        }
        this.extraBPD = MinOptMax.getInstance(padding);

    }

    /** {@inheritDoc} */
    @Override
    protected MinOptMax getExtraIPD(final boolean isNotFirst,
            final boolean isNotLast) {
        int borderAndPadding = 0;
        if (this.borderProps != null) {
            borderAndPadding = this.borderProps.getPadding(
                    CommonBorderPaddingBackground.START, isNotFirst, this);
            borderAndPadding += this.borderProps.getBorderWidth(
                    CommonBorderPaddingBackground.START, isNotFirst);
            borderAndPadding += this.borderProps.getPadding(
                    CommonBorderPaddingBackground.END, isNotLast, this);
            borderAndPadding += this.borderProps.getBorderWidth(
                    CommonBorderPaddingBackground.END, isNotLast);
        }
        return MinOptMax.getInstance(borderAndPadding);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasLeadingFence(final boolean isNotFirst) {
        return this.borderProps != null
                && (this.borderProps.getPadding(
                        CommonBorderPaddingBackground.START, isNotFirst, this) > 0 || this.borderProps
                        .getBorderWidth(CommonBorderPaddingBackground.START,
                                isNotFirst) > 0);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasTrailingFence(final boolean isNotLast) {
        return this.borderProps != null
                && (this.borderProps.getPadding(
                        CommonBorderPaddingBackground.END, isNotLast, this) > 0 || this.borderProps
                        .getBorderWidth(CommonBorderPaddingBackground.END,
                                isNotLast) > 0);
    }

    /** {@inheritDoc} */
    @Override
    protected SpaceProperty getSpaceStart() {
        return this.inlineProps != null ? this.inlineProps.spaceStart : null;
    }

    /** {@inheritDoc} */
    @Override
    protected SpaceProperty getSpaceEnd() {
        return this.inlineProps != null ? this.inlineProps.spaceEnd : null;
    }

    /**
     * Create and initialize an <code>InlineArea</code>
     *
     * @param isInline
     *            true if the parent is an inline
     * @return the area
     */
    protected InlineArea createArea(final boolean isInline) {
        InlineArea area;
        if (isInline) {
            area = createInlineParent();
            area.setBlockProgressionOffset(0);
        } else {
            area = new InlineBlockParent();
        }
        if (this.fobj instanceof Inline || this.fobj instanceof BasicLink) {
            TraitSetter.setProducerID(area, this.fobj.getId());
        }
        return area;
    }

    /**
     * Creates the inline area that will contain the areas returned by the
     * children of this layout manager.
     *
     * @return a new inline area
     */
    protected InlineParent createInlineParent() {
        return new InlineParent();
    }

    /** {@inheritDoc} */
    @Override
    protected void setTraits(final boolean isNotFirst, final boolean isNotLast) {
        if (this.borderProps != null) {
            // Add border and padding to current area and set flags (FIRST, LAST
            // ...)
            TraitSetter.setBorderPaddingTraits(getCurrentArea(),
                    this.borderProps, isNotFirst, isNotLast, this);
            TraitSetter.addBackground(getCurrentArea(), this.borderProps, this);
        }
    }

    /**
     * @return true if this element must be kept together
     */
    public boolean mustKeepTogether() {
        return mustKeepTogether(getParent());
    }

    private boolean mustKeepTogether(final LayoutManager lm) {
        if (lm instanceof BlockLevelLayoutManager) {
            return ((BlockLevelLayoutManager) lm).mustKeepTogether();
        } else if (lm instanceof InlineLayoutManager) {
            return ((InlineLayoutManager) lm).mustKeepTogether();
        } else {
            return mustKeepTogether(lm.getParent());
        }
    }

    /** {@inheritDoc} */
    @Override
    // CSOK: MethodLength
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        LayoutManager curLM;

        // the list returned by child LM
        List<KnuthSequence> returnedList;

        // the list which will be returned to the parent LM
        final List<KnuthSequence> returnList = new LinkedList<KnuthSequence>();
        KnuthSequence lastSequence = null;

        if (this.fobj instanceof Title) {
            this.alignmentContext = new AlignmentContext(
                    this.font,
                    this.lineHeight.getOptimum(this).getLength().getValue(this),
                    context.getWritingMode());

        } else {
            this.alignmentContext = new AlignmentContext(
                    this.font,
                    this.lineHeight.getOptimum(this).getLength().getValue(this),
                    this.alignmentAdjust, this.alignmentBaseline,
                    this.baselineShift, this.dominantBaseline, context
                    .getAlignmentContext());
        }

        this.childLC = new LayoutContext(context);
        this.childLC.setAlignmentContext(this.alignmentContext);

        if (context.startsNewArea()) {
            // First call to this LM in new parent "area", but this may
            // not be the first area created by this inline
            if (getSpaceStart() != null) {
                context.getLeadingSpace().addSpace(
                        new SpaceVal(getSpaceStart(), this));
            }
        }

        final StringBuffer trace = new StringBuffer("InlineLM:");

        // We'll add the border to the first inline sequence created.
        // This flag makes sure we do it only once.
        boolean borderAdded = false;

        if (this.borderProps != null) {
            this.childLC.setLineStartBorderAndPaddingWidth(context
                    .getLineStartBorderAndPaddingWidth()
                    + this.borderProps.getPaddingStart(true, this)
                    + this.borderProps.getBorderStartWidth(true));
            this.childLC.setLineEndBorderAndPaddingWidth(context
                    .getLineEndBorderAndPaddingWidth()
                    + this.borderProps.getPaddingEnd(true, this)
                    + this.borderProps.getBorderEndWidth(true));
        }

        while ((curLM = getChildLM()) != null) {

            if (!(curLM instanceof InlineLevelLayoutManager)) {
                // A block LM
                // Leave room for start/end border and padding
                if (this.borderProps != null) {
                    this.childLC
                    .setRefIPD(this.childLC.getRefIPD()
                            - this.borderProps.getPaddingStart(
                                    this.lastChildLM != null, this)
                                    - this.borderProps
                                    .getBorderStartWidth(this.lastChildLM != null)
                                    - this.borderProps.getPaddingEnd(
                                            hasNextChildLM(), this)
                                            - this.borderProps
                                            .getBorderEndWidth(hasNextChildLM()));
                }
            }

            // get KnuthElements from curLM
            returnedList = curLM.getNextKnuthElements(this.childLC, alignment);
            if (returnList.isEmpty()
                    && this.childLC.isKeepWithPreviousPending()) {
                this.childLC.clearKeepWithPreviousPending();
            }
            if (returnedList == null || returnedList.isEmpty()) {
                // curLM returned null or an empty list, because it finished;
                // just iterate once more to see if there is another child
                continue;
            }

            if (curLM instanceof InlineLevelLayoutManager) {
                context.clearKeepWithNextPending();
                // "wrap" the Position stored in each element of returnedList
                final ListIterator seqIter = returnedList.listIterator();
                while (seqIter.hasNext()) {
                    final KnuthSequence sequence = (KnuthSequence) seqIter
                            .next();
                    sequence.wrapPositions(this);
                }
                int insertionStartIndex = 0;
                if (lastSequence != null
                        && lastSequence.appendSequenceOrClose(returnedList
                                .get(0))) {
                    insertionStartIndex = 1;
                }
                // add border and padding to the first complete sequence of this
                // LM
                if (!borderAdded && !returnedList.isEmpty()) {
                    addKnuthElementsForBorderPaddingStart(returnedList.get(0));
                    borderAdded = true;
                }
                for (final Iterator<KnuthSequence> iter = returnedList
                        .listIterator(insertionStartIndex); iter.hasNext();) {
                    returnList.add(iter.next());
                }
            } else { // A block LM
                final BlockKnuthSequence sequence = new BlockKnuthSequence(
                        returnedList);
                sequence.wrapPositions(this);
                boolean appended = false;
                if (lastSequence != null) {
                    if (lastSequence.canAppendSequence(sequence)) {
                        final BreakElement bk = new BreakElement(new Position(
                                this), 0, context);
                        final boolean keepTogether = mustKeepTogether()
                                || context.isKeepWithNextPending()
                                || this.childLC.isKeepWithPreviousPending();
                        appended = lastSequence.appendSequenceOrClose(sequence,
                                keepTogether, bk);
                    } else {
                        lastSequence.endSequence();
                    }
                }
                if (!appended) {
                    // add border and padding to the first complete sequence of
                    // this LM
                    if (!borderAdded) {
                        addKnuthElementsForBorderPaddingStart(sequence);
                        borderAdded = true;
                    }
                    returnList.add(sequence);
                }
                // propagate and clear
                context.updateKeepWithNextPending(this.childLC
                        .getKeepWithNextPending());
                this.childLC.clearKeepsPending();
            }
            lastSequence = ListUtil.getLast(returnList);
            this.lastChildLM = curLM;
            // the context used to create this childLC above was applied a
            // LayoutContext.SUPPRESS_BREAK_BEFORE
            // in the getNextChildElements() method of the parent
            // BlockLayoutManger; as a consequence all
            // line breaks in blocks nested inside the inline associated with
            // this ILM are being supressed;
            // here we revert that supression; we do not need to do that for the
            // first element since that
            // is handled by the getBreakBefore() method of the wrapping
            // BlockStackingLayoutManager.
            // Note: this fix seems to work but is far from being the ideal way
            // to do this
            this.childLC.setFlags(LayoutContext.SUPPRESS_BREAK_BEFORE, false);
        }

        if (lastSequence != null) {
            addKnuthElementsForBorderPaddingEnd(lastSequence);
        }

        setFinished(true);
        log.trace(trace.toString());

        if (returnList.isEmpty()) {
            /*
             * if the FO itself is empty, but has an id specified or associated
             * fo:markers, then we still need a dummy sequence to register its
             * position in the area tree
             */
            if (this.fobj.hasId() || this.fobj.hasMarkers()) {
                final InlineKnuthSequence emptySeq = new InlineKnuthSequence();
                emptySeq.add(new KnuthInlineBox(0, this.alignmentContext,
                        notifyPos(getAuxiliaryPosition()), true));
                returnList.add(emptySeq);
            }
        }

        return returnList.isEmpty() ? null : returnList;
    }

    /**
     * Generate and add areas to parent area. Set size of each area. This should
     * only create and return one inline area for any inline parent area.
     *
     * @param parentIter
     *            Iterator over Position information returned by this
     *            LayoutManager.
     * @param context
     *            layout context.
     */
    @Override
    public void addAreas(final PositionIterator parentIter,
            final LayoutContext context) {

        addId();

        setChildContext(new LayoutContext(context)); // Store current value

        // "Unwrap" the NonLeafPositions stored in parentIter and put
        // them in a new list. Set lastLM to be the LayoutManager
        // which created the last Position: if the LAST_AREA flag is
        // set in the layout context, it must be also set in the
        // layout context given to lastLM, but must be cleared in the
        // layout context given to the other LMs.
        final List<Position> positionList = new LinkedList<Position>();
        Position pos;
        LayoutManager lastLM = null; // last child LM in this iterator
        Position lastPos = null;
        while (parentIter.hasNext()) {
            pos = parentIter.next();
            if (pos != null && pos.getPosition() != null) {
                if (isFirst(pos)) {
                    /*
                     * If this element is a descendant of a table-header/footer,
                     * its content may be repeated over pages, so the generation
                     * of its areas may be restarted.
                     */
                    this.areaCreated = false;
                }
                positionList.add(pos.getPosition());
                lastLM = pos.getPosition().getLM();
                lastPos = pos;
            }
        }

        // If this LM has fence, make a new leading space specifier.
        if (hasLeadingFence(this.areaCreated)) {
            getContext().setLeadingSpace(new SpaceSpecifier(false));
            getContext().setFlags(LayoutContext.RESOLVE_LEADING_SPACE, true);
        } else {
            getContext().setFlags(LayoutContext.RESOLVE_LEADING_SPACE, false);
        }

        if (getSpaceStart() != null) {
            context.getLeadingSpace().addSpace(
                    new SpaceVal(getSpaceStart(), this));
        }

        addMarkersToPage(true, !this.areaCreated, lastPos == null
                || isLast(lastPos));

        final InlineArea parent = createArea(lastLM == null
                || lastLM instanceof InlineLevelLayoutManager);
        parent.setBPD(this.alignmentContext.getHeight());
        if (parent instanceof InlineParent) {
            parent.setBlockProgressionOffset(this.alignmentContext.getOffset());
        } else if (parent instanceof InlineBlockParent) {
            // All inline elements are positioned by the renderers relative to
            // the before edge of their content rectangle
            if (this.borderProps != null) {
                parent.setBlockProgressionOffset(this.borderProps
                        .getPaddingBefore(false, this)
                        + this.borderProps.getBorderBeforeWidth(false));
            }
        }
        setCurrentArea(parent);

        final PositionIterator childPosIter = new PositionIterator(
                positionList.listIterator());

        LayoutManager prevLM = null;
        LayoutManager childLM;
        while ((childLM = childPosIter.getNextChildLM()) != null) {
            getContext().setFlags(LayoutContext.LAST_AREA,
                    context.isLastArea() && childLM == lastLM);
            childLM.addAreas(childPosIter, getContext());
            getContext().setLeadingSpace(getContext().getTrailingSpace());
            getContext().setFlags(LayoutContext.RESOLVE_LEADING_SPACE, true);
            prevLM = childLM;
        }

        /*
         * If this LM has a trailing fence, resolve trailing space specs from
         * descendants. Otherwise, propagate any trailing space specs to the
         * parent LM via the layout context. If the last child LM called returns
         * LAST_AREA in the layout context and it is the last child LM for this
         * LM, then this must be the last area for the current LM too.
         */
        final boolean isLast = getContext().isLastArea()
                && prevLM == this.lastChildLM;

        if (hasTrailingFence(isLast)) {
            addSpace(getCurrentArea(),
                    getContext().getTrailingSpace().resolve(false),
                    getContext().getSpaceAdjust());
            context.setTrailingSpace(new SpaceSpecifier(false));
        } else {
            // Propagate trailing space-spec sequence to parent LM in context.
            context.setTrailingSpace(getContext().getTrailingSpace());
        }
        // Add own trailing space to parent context (or set on area?)
        if (context.getTrailingSpace() != null && getSpaceEnd() != null) {
            context.getTrailingSpace().addSpace(
                    new SpaceVal(getSpaceEnd(), this));
        }

        // Not sure if lastPos can legally be null or if that masks a different
        // problem.
        // But it seems to fix bug 38053.
        setTraits(this.areaCreated, lastPos == null || !isLast(lastPos));
        this.parentLayoutManager.addChildArea(getCurrentArea());

        addMarkersToPage(false, !this.areaCreated, lastPos == null
                || isLast(lastPos));

        context.setFlags(LayoutContext.LAST_AREA, isLast);
        this.areaCreated = true;
        checkEndOfLayout(lastPos);
    }

    /** {@inheritDoc} */
    @Override
    public void addChildArea(final Area childArea) {
        final Area parent = getCurrentArea();
        if (getContext().resolveLeadingSpace()) {
            addSpace(parent, getContext().getLeadingSpace().resolve(false),
                    getContext().getSpaceAdjust());
        }
        parent.addChildArea(childArea);
    }

    /** {@inheritDoc} */
    @Override
    public List getChangedKnuthElements(final List oldList,
            final int alignment, final int depth) {
        final List returnedList = new LinkedList();
        addKnuthElementsForBorderPaddingStart(returnedList);
        returnedList.addAll(super.getChangedKnuthElements(oldList, alignment,
                depth));
        addKnuthElementsForBorderPaddingEnd(returnedList);
        return returnedList;
    }

    /**
     * Creates Knuth elements for start border padding and adds them to the
     * return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     */
    protected void addKnuthElementsForBorderPaddingStart(final List returnList) {
        // Border and Padding (start)
        /*
         * If the returnlist is a BlockKnuthSequence, the border and padding
         * should be added to the first paragraph inside it, but it is too late
         * to do that now. At least, avoid adding it to the bpd sequence.
         */
        if (returnList instanceof BlockKnuthSequence) {
            return;
        }
        final CommonBorderPaddingBackground borderAndPadding = ((InlineLevel) this.fobj)
                .getCommonBorderPaddingBackground();
        if (borderAndPadding != null) {
            final int ipStart = borderAndPadding.getBorderStartWidth(false)
                    + borderAndPadding.getPaddingStart(false, this);
            if (ipStart > 0) {
                returnList.add(0, new KnuthBox(ipStart, getAuxiliaryPosition(),
                        true));
            }
        }
    }

    /**
     * Creates Knuth elements for end border padding and adds them to the return
     * list.
     *
     * @param returnList
     *            return list to add the additional elements to
     */
    protected void addKnuthElementsForBorderPaddingEnd(final List returnList) {
        // Border and Padding (after)
        /*
         * If the returnlist is a BlockKnuthSequence, the border and padding
         * should be added to the last paragraph inside it, but it is too late
         * to do that now. At least, avoid adding it to the bpd sequence.
         */
        if (returnList instanceof BlockKnuthSequence) {
            return;
        }
        final CommonBorderPaddingBackground borderAndPadding = ((InlineLevel) this.fobj)
                .getCommonBorderPaddingBackground();
        if (borderAndPadding != null) {
            final int ipEnd = borderAndPadding.getBorderEndWidth(false)
                    + borderAndPadding.getPaddingEnd(false, this);
            if (ipEnd > 0) {
                returnList
                .add(new KnuthBox(ipEnd, getAuxiliaryPosition(), true));
            }
        }
    }

    /**
     * @return an auxiliary {@link Position} instance used for things like
     *         spaces.
     */
    protected Position getAuxiliaryPosition() {
        return new NonLeafPosition(this, null);
    }
}
