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

/* $Id: ActiveCell.java 893238 2009-12-22 17:20:51Z vhennebert $ */

package org.apache.fop.layoutmgr.table;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.flow.table.ConditionalBorder;
import org.apache.fop.fo.flow.table.EffRow;
import org.apache.fop.fo.flow.table.PrimaryGridUnit;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.layoutmgr.ElementListUtils;
import org.apache.fop.layoutmgr.Keep;
import org.apache.fop.layoutmgr.KnuthBlockBox;
import org.apache.fop.layoutmgr.KnuthBox;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthPenalty;
import org.apache.fop.traits.MinOptMax;

/**
 * A cell playing in the construction of steps for a row-group.
 */
@Slf4j
class ActiveCell {

    private final PrimaryGridUnit pgu;
    /** Knuth elements for this active cell. */
    private final List elementList;
    /** Iterator over the Knuth element list. */
    private final ListIterator knuthIter;
    /** Number of the row where the row-span ends, zero-based. */
    private final int endRowIndex;
    /** Length of the Knuth elements not yet included in the steps. */
    private int remainingLength;
    /**
     * Total length of this cell's content plus the lengths of the previous
     * rows.
     */
    private final int totalLength;
    /** Length of the Knuth elements already included in the steps. */
    private int includedLength;

    private final int paddingBeforeNormal;
    private final int paddingBeforeLeading;
    private final int paddingAfterNormal;
    private final int paddingAfterTrailing;

    private final int bpBeforeNormal;
    private int bpBeforeLeading;
    private final int bpAfterNormal;
    private int bpAfterTrailing;

    /**
     * True if the next CellPart that will be created will be the last one for
     * this cell.
     */
    private boolean lastCellPart;

    private Keep keepWithNext;

    private int spanIndex = 0;

    private final Step previousStep;
    private final Step nextStep;
    /**
     * The step following nextStep. Computing it early allows to calculate
     * {@link Step#condBeforeContentLength}, thus to easily determine the
     * remaining length. That also helps for {@link #increaseCurrentStep(int)}.
     */
    private final Step afterNextStep;

    /**
     * Auxiliary class to store all the informations related to a breaking step.
     */
    private static class Step {
        /**
         * Index, in the list of Knuth elements, of the element starting this
         * step.
         */
        private int start;
        /**
         * Index, in the list of Knuth elements, of the element ending this
         * step.
         */
        private int end;
        /** Length of the Knuth elements up to this step. */
        private int contentLength;
        /** Total length up to this step, including paddings and borders. */
        private int totalLength;
        /** Length of the penalty ending this step, if any. */
        private int penaltyLength;
        /**
         * Value of the penalty ending this step, 0 if the step does not end on
         * a penalty.
         */
        private int penaltyValue;
        /** List of footnotes for this step. */
        private List footnoteList;
        /**
         * One of {@link Constants#EN_AUTO}, {@link Constants#EN_COLUMN},
         * {@link Constants#EN_PAGE}, {@link Constants#EN_EVEN_PAGE},
         * {@link Constants#EN_ODD_PAGE}. Set to auto if the break isn't at a
         * penalty element.
         */
        private int breakClass;
        /**
         * Length of the optional content at the beginning of the step. That is,
         * content that will not appear if this step starts a new page.
         */
        private int condBeforeContentLength;

        Step(final int contentLength) {
            this.contentLength = contentLength;
            this.end = -1;
        }

        Step(final Step other) {
            set(other);
        }

        void set(final Step other) {
            this.start = other.start;
            this.end = other.end;
            this.contentLength = other.contentLength;
            this.totalLength = other.totalLength;
            this.penaltyLength = other.penaltyLength;
            this.penaltyValue = other.penaltyValue;
            if (other.footnoteList != null) {
                if (this.footnoteList == null) {
                    this.footnoteList = new ArrayList();
                }
                this.footnoteList.addAll(other.footnoteList);
            }
            this.condBeforeContentLength = other.condBeforeContentLength;
            this.breakClass = other.breakClass;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Step: start=" + this.start + " end=" + this.end
                    + " length=" + this.totalLength;
        }
    }

    // TODO to be removed along with the RowPainter#computeContentLength method
    /** See {@link ActiveCell#handleExplicitHeight(MinOptMax, MinOptMax)}. */
    private static class FillerPenalty extends KnuthPenalty {

        private final int contentLength;

        FillerPenalty(final KnuthPenalty p, final int length) {
            super(length, p.getPenalty(), p.isPenaltyFlagged(), p
                    .getBreakClass(), p.getPosition(), p.isAuxiliary());
            this.contentLength = p.getWidth();
        }

        FillerPenalty(final int length) {
            super(length, 0, false, null, true);
            this.contentLength = 0;
        }
    }

    /** See {@link ActiveCell#handleExplicitHeight(MinOptMax, MinOptMax)}. */
    private static class FillerBox extends KnuthBox {
        FillerBox(final int length) {
            super(length, null, true);
        }
    }

    /**
     * Returns the actual length of the content represented by the given
     * element. In the case where this element is used as a filler to match a
     * row's fixed height, the value returned by the getW() method will be
     * higher than the actual content.
     *
     * @param el
     *            an element
     * @return the actual content length corresponding to the element
     */
    static int getElementContentLength(final KnuthElement el) {
        if (el instanceof FillerPenalty) {
            return ((FillerPenalty) el).contentLength;
        } else if (el instanceof FillerBox) {
            return 0;
        } else {
            return el.getWidth();
        }
    }

    ActiveCell(final PrimaryGridUnit pgu, final EffRow row, final int rowIndex,
            final int previousRowsLength, final TableLayoutManager tableLM) {
        this.pgu = pgu;
        final CommonBorderPaddingBackground bordersPaddings = pgu.getCell()
                .getCommonBorderPaddingBackground();
        final TableCellLayoutManager cellLM = pgu.getCellLM();
        this.paddingBeforeNormal = bordersPaddings.getPaddingBefore(false,
                cellLM);
        this.paddingBeforeLeading = bordersPaddings.getPaddingBefore(true,
                cellLM);
        this.paddingAfterNormal = bordersPaddings
                .getPaddingAfter(false, cellLM);
        this.paddingAfterTrailing = bordersPaddings.getPaddingAfter(true,
                cellLM);
        this.bpBeforeNormal = this.paddingBeforeNormal
                + pgu.getBeforeBorderWidth(0, ConditionalBorder.NORMAL);
        this.bpBeforeLeading = this.paddingBeforeLeading
                + pgu.getBeforeBorderWidth(0, ConditionalBorder.REST);
        this.bpAfterNormal = this.paddingAfterNormal
                + pgu.getAfterBorderWidth(ConditionalBorder.NORMAL);
        this.bpAfterTrailing = this.paddingAfterTrailing
                + pgu.getAfterBorderWidth(0, ConditionalBorder.REST);
        this.elementList = pgu.getElements();
        handleExplicitHeight(pgu.getCell().getBlockProgressionDimension()
                .toMinOptMax(tableLM), row.getExplicitHeight());
        this.knuthIter = this.elementList.listIterator();
        this.includedLength = -1; // Avoid troubles with cells having content of
        // zero length
        this.totalLength = previousRowsLength
                + ElementListUtils.calcContentLength(this.elementList);
        this.endRowIndex = rowIndex + pgu.getCell().getNumberRowsSpanned() - 1;
        this.keepWithNext = Keep.KEEP_AUTO;
        this.remainingLength = this.totalLength - previousRowsLength;

        this.afterNextStep = new Step(previousRowsLength);
        this.previousStep = new Step(this.afterNextStep);
        gotoNextLegalBreak();
        this.nextStep = new Step(this.afterNextStep);
        if (this.afterNextStep.end < this.elementList.size() - 1) {
            gotoNextLegalBreak();
        }
    }

    /**
     * Modifies the cell's element list by putting filler elements, so that the
     * cell's or row's explicit height is always reached.
     *
     * TODO this will work properly only for the first break. Then the
     * limitation explained on
     * http://wiki.apache.org/xmlgraphics-fop/TableLayout/KnownProblems occurs.
     * The list of elements needs to be re-adjusted after each break.
     */
    private void handleExplicitHeight(final MinOptMax cellBPD,
            final MinOptMax rowBPD) {
        final int minBPD = Math.max(cellBPD.getMin(), rowBPD.getMin());
        if (minBPD > 0) {
            final ListIterator iter = this.elementList.listIterator();
            int cumulateLength = 0;
            boolean prevIsBox = false;
            while (iter.hasNext() && cumulateLength < minBPD) {
                final KnuthElement el = (KnuthElement) iter.next();
                if (el.isBox()) {
                    prevIsBox = true;
                    cumulateLength += el.getWidth();
                } else if (el.isGlue()) {
                    if (prevIsBox) {
                        this.elementList.add(iter.nextIndex() - 1,
                                new FillerPenalty(minBPD - cumulateLength));
                    }
                    prevIsBox = false;
                    cumulateLength += el.getWidth();
                } else {
                    prevIsBox = false;
                    if (cumulateLength + el.getWidth() < minBPD) {
                        iter.set(new FillerPenalty((KnuthPenalty) el, minBPD
                                - cumulateLength));
                    }
                }
            }
        }
        final int optBPD = Math.max(minBPD,
                Math.max(cellBPD.getOpt(), rowBPD.getOpt()));
        if (this.pgu.getContentLength() < optBPD) {
            this.elementList.add(new FillerBox(optBPD
                    - this.pgu.getContentLength()));
        }
    }

    PrimaryGridUnit getPrimaryGridUnit() {
        return this.pgu;
    }

    /**
     * Returns true if this cell ends on the given row.
     *
     * @param rowIndex
     *            index of a row in the row-group, zero-based
     * @return true if this cell ends on the given row
     */
    boolean endsOnRow(final int rowIndex) {
        return rowIndex == this.endRowIndex;
    }

    /**
     * Returns the length of this cell's content not yet included in the steps,
     * plus the cell's borders and paddings if applicable.
     *
     * @return the remaining length, zero if the cell is finished
     */
    int getRemainingLength() {
        if (includedInLastStep()
                && this.nextStep.end == this.elementList.size() - 1) {
            // The cell is finished
            return 0;
        } else {
            return this.bpBeforeLeading + this.remainingLength
                    + this.bpAfterNormal;
        }
    }

    private void gotoNextLegalBreak() {
        this.afterNextStep.penaltyLength = 0;
        this.afterNextStep.penaltyValue = 0;
        this.afterNextStep.condBeforeContentLength = 0;
        this.afterNextStep.breakClass = Constants.EN_AUTO;
        if (this.afterNextStep.footnoteList != null) {
            this.afterNextStep.footnoteList.clear();
        }
        boolean breakFound = false;
        boolean prevIsBox = false;
        boolean boxFound = false;
        while (!breakFound && this.knuthIter.hasNext()) {
            final KnuthElement el = (KnuthElement) this.knuthIter.next();
            if (el.isPenalty()) {
                prevIsBox = false;
                if (el.getPenalty() < KnuthElement.INFINITE
                        || ((KnuthPenalty) el).getBreakClass() == Constants.EN_PAGE) {
                    // TODO too much is being done in that test, only to handle
                    // keep.within-column properly.

                    // First legal break point
                    breakFound = true;
                    final KnuthPenalty p = (KnuthPenalty) el;
                    this.afterNextStep.penaltyLength = p.getWidth();
                    this.afterNextStep.penaltyValue = p.getPenalty();
                    if (p.isForcedBreak()) {
                        this.afterNextStep.breakClass = p.getBreakClass();
                    }
                }
            } else if (el.isGlue()) {
                if (prevIsBox) {
                    // Second legal break point
                    breakFound = true;
                } else {
                    this.afterNextStep.contentLength += el.getWidth();
                    if (!boxFound) {
                        this.afterNextStep.condBeforeContentLength += el
                                .getWidth();
                    }
                }
                prevIsBox = false;
            } else {
                if (el instanceof KnuthBlockBox
                        && ((KnuthBlockBox) el).hasAnchors()) {
                    if (this.afterNextStep.footnoteList == null) {
                        this.afterNextStep.footnoteList = new LinkedList();
                    }
                    this.afterNextStep.footnoteList.addAll(((KnuthBlockBox) el)
                            .getFootnoteBodyLMs());
                }
                prevIsBox = true;
                boxFound = true;
                this.afterNextStep.contentLength += el.getWidth();
            }
        }
        this.afterNextStep.end = this.knuthIter.nextIndex() - 1;
        this.afterNextStep.totalLength = this.bpBeforeNormal
                + this.afterNextStep.contentLength
                + this.afterNextStep.penaltyLength + this.bpAfterTrailing;
    }

    /**
     * Returns the minimal step that is needed for this cell to contribute some
     * content.
     *
     * @return the step for this cell's first legal break
     */
    int getFirstStep() {
        log.debug(this + ": min first step = " + this.nextStep.totalLength);
        return this.nextStep.totalLength;
    }

    /**
     * Returns the last step for this cell. This includes the normal border- and
     * padding-before, the whole content, the normal padding-after, and the
     * <em>trailing</em> after border. Indeed, if the normal border is taken
     * instead, and appears to be smaller than the trailing one, the last step
     * may be smaller than the current step (see
     * TableStepper#considerRowLastStep). This will produce a wrong infinite
     * penalty, plus the cell's content won't be taken into account since the
     * final step will be smaller than the current one (see
     * {@link #signalNextStep(int)}). This actually means that the content will
     * be swallowed.
     *
     * @return the length of last step
     */
    int getLastStep() {
        assert this.nextStep.end == this.elementList.size() - 1;
        assert this.nextStep.contentLength == this.totalLength
                && this.nextStep.penaltyLength == 0;
        final int lastStep = this.bpBeforeNormal
                + this.totalLength
                + this.paddingAfterNormal
                + this.pgu
                .getAfterBorderWidth(ConditionalBorder.LEADING_TRAILING);
        log.debug(this + ": last step = " + lastStep);
        return lastStep;
    }

    /**
     * Increases the next step up to the given limit.
     *
     * @param limit
     *            the length up to which the next step is allowed to increase
     * @see #signalRowFirstStep(int)
     * @see #signalRowLastStep(int)
     */
    private void increaseCurrentStep(final int limit) {
        if (this.nextStep.end < this.elementList.size() - 1) {
            while (this.afterNextStep.totalLength <= limit
                    && this.nextStep.breakClass == Constants.EN_AUTO) {
                final int condBeforeContentLength = this.nextStep.condBeforeContentLength;
                this.nextStep.set(this.afterNextStep);
                this.nextStep.condBeforeContentLength = condBeforeContentLength;
                if (this.afterNextStep.end >= this.elementList.size() - 1) {
                    break;
                }
                gotoNextLegalBreak();
            }
        }
    }

    /**
     * Gets the selected first step for the current row. If this cell's first
     * step is smaller, then it may be able to add some more of its content,
     * since there will be no break before the given step anyway.
     *
     * @param firstStep
     *            the current row's first step
     */
    void signalRowFirstStep(final int firstStep) {
        increaseCurrentStep(firstStep);
        if (log.isTraceEnabled()) {
            log.trace(this + ": first step increased to "
                    + this.nextStep.totalLength);
        }
    }

    /** See {@link #signalRowFirstStep(int)}. */
    void signalRowLastStep(final int lastStep) {
        increaseCurrentStep(lastStep);
        if (log.isTraceEnabled()) {
            log.trace(this + ": next step increased to "
                    + this.nextStep.totalLength);
        }
    }

    /**
     * Returns the total length up to the next legal break, not yet included in
     * the steps.
     *
     * @return the total length up to the next legal break (-1 signals no
     *         further step)
     */
    int getNextStep() {
        if (includedInLastStep()) {
            this.previousStep.set(this.nextStep);
            if (this.nextStep.end >= this.elementList.size() - 1) {
                this.nextStep.start = this.elementList.size();
                return -1;
            } else {
                this.nextStep.set(this.afterNextStep);
                this.nextStep.start = this.previousStep.end + 1;
                this.afterNextStep.start = this.nextStep.start;
                if (this.afterNextStep.end < this.elementList.size() - 1) {
                    gotoNextLegalBreak();
                }
            }
        }
        return this.nextStep.totalLength;
    }

    private boolean includedInLastStep() {
        return this.includedLength == this.nextStep.contentLength;
    }

    /**
     * Signals the length of the chosen next step, so that this cell determines
     * whether its own step may be included or not.
     *
     * @param minStep
     *            length of the chosen next step
     * @return the break class of the step, if any. One of
     *         {@link Constants#EN_AUTO}, {@link Constants#EN_COLUMN},
     *         {@link Constants#EN_PAGE}, {@link Constants#EN_EVEN_PAGE},
     *         {@link Constants#EN_ODD_PAGE}. EN_AUTO if this cell's step is not
     *         included in the next step.
     */
    int signalNextStep(final int minStep) {
        if (this.nextStep.totalLength <= minStep) {
            this.includedLength = this.nextStep.contentLength;
            this.remainingLength = this.totalLength - this.includedLength
                    - this.afterNextStep.condBeforeContentLength;
            return this.nextStep.breakClass;
        } else {
            return Constants.EN_AUTO;
        }
    }

    /**
     * Receives indication that the next row is about to start, and that
     * (collapse) borders must be updated accordingly.
     */
    void nextRowStarts() {
        this.spanIndex++;
        // Subtract the old value of bpAfterTrailing...
        this.nextStep.totalLength -= this.bpAfterTrailing;
        this.afterNextStep.totalLength -= this.bpAfterTrailing;

        this.bpAfterTrailing = this.paddingAfterTrailing
                + this.pgu.getAfterBorderWidth(this.spanIndex,
                        ConditionalBorder.REST);

        // ... and add the new one
        this.nextStep.totalLength += this.bpAfterTrailing;
        this.afterNextStep.totalLength += this.bpAfterTrailing;
        // TODO if the new after border is greater than the previous one the
        // next step may
        // increase further than the row's first step, which can lead to wrong
        // output in
        // some cases
    }

    /**
     * Receives indication that the current row is ending, and that (collapse)
     * borders must be updated accordingly.
     *
     * @param rowIndex
     *            the index of the ending row
     */
    void endRow(final int rowIndex) {
        if (endsOnRow(rowIndex)) {
            // Subtract the old value of bpAfterTrailing...
            this.nextStep.totalLength -= this.bpAfterTrailing;
            this.bpAfterTrailing = this.paddingAfterNormal
                    + this.pgu
                    .getAfterBorderWidth(ConditionalBorder.LEADING_TRAILING);
            // ... and add the new one
            this.nextStep.totalLength += this.bpAfterTrailing;
            this.lastCellPart = true;
        } else {
            this.bpBeforeLeading = this.paddingBeforeLeading
                    + this.pgu.getBeforeBorderWidth(this.spanIndex + 1,
                            ConditionalBorder.REST);
        }
    }

    /**
     * Returns true if this cell would be finished after the given step. That
     * is, it would be included in the step and the end of its content would be
     * reached.
     *
     * @param step
     *            the next step
     * @return true if this cell finishes at the given step
     */
    boolean finishes(final int step) {
        return this.nextStep.totalLength <= step
                && this.nextStep.end == this.elementList.size() - 1;
    }

    /**
     * Creates and returns a CellPart instance for the content of this cell
     * which is included in the next step.
     *
     * @return a CellPart instance
     */
    CellPart createCellPart() {
        if (this.nextStep.end + 1 == this.elementList.size()) {
            this.keepWithNext = this.pgu.getKeepWithNext();
            // TODO if keep-with-next is set on the row, must every cell of the
            // row
            // contribute some content from children blocks?
            // see
            // http://mail-archives.apache.org/mod_mbox/xmlgraphics-fop-dev/200802.mbox/
            // %3c47BDA379.4050606@anyware-tech.com%3e
            // Assuming no, but if yes the following code should enable this
            // behaviour
            // if (pgu.getRow() != null && pgu.getRow().mustKeepWithNext()) {
            // keepWithNextSignal = true; //to be converted to integer strengths
            // }
        }
        int bpBeforeFirst;
        if (this.nextStep.start == 0) {
            bpBeforeFirst = this.pgu.getBeforeBorderWidth(0,
                    ConditionalBorder.LEADING_TRAILING)
                    + this.paddingBeforeNormal;
        } else {
            bpBeforeFirst = this.bpBeforeLeading;
        }
        final int length = this.nextStep.contentLength
                - this.nextStep.condBeforeContentLength
                - this.previousStep.contentLength;
        if (!includedInLastStep()
                || this.nextStep.start == this.elementList.size()) {
            return new CellPart(this.pgu, this.nextStep.start,
                    this.previousStep.end, this.lastCellPart, 0, 0,
                    this.previousStep.penaltyLength, this.bpBeforeNormal,
                    bpBeforeFirst, this.bpAfterNormal, this.bpAfterTrailing);
        } else {
            return new CellPart(this.pgu, this.nextStep.start,
                    this.nextStep.end, this.lastCellPart,
                    this.nextStep.condBeforeContentLength, length,
                    this.nextStep.penaltyLength, this.bpBeforeNormal,
                    bpBeforeFirst, this.bpAfterNormal, this.bpAfterTrailing);
        }
    }

    /**
     * Adds the footnotes (if any) that are part of the next step, if this cell
     * contributes content to the next step.
     *
     * @param footnoteList
     *            the list to which this cell must add its footnotes
     */
    void addFootnotes(final List footnoteList) {
        if (includedInLastStep() && this.nextStep.footnoteList != null) {
            footnoteList.addAll(this.nextStep.footnoteList);
            this.nextStep.footnoteList.clear();
        }
    }

    Keep getKeepWithNext() {
        return this.keepWithNext;
    }

    int getPenaltyValue() {
        if (includedInLastStep()) {
            return this.nextStep.penaltyValue;
        } else {
            return this.previousStep.penaltyValue;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Cell " + (this.pgu.getRowIndex() + 1) + "."
                + (this.pgu.getColIndex() + 1);
    }
}
