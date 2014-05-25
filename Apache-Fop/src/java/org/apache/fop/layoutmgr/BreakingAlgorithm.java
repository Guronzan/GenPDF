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

/* $Id: BreakingAlgorithm.java 1297008 2012-03-05 11:19:47Z vhennebert $ */

package org.apache.fop.layoutmgr;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fo.Constants;

/**
 * The set of nodes is sorted into lines indexed into activeLines. The nodes in
 * each line are linked together in a single linked list by the
 * {@link KnuthNode#next} field. The activeLines array contains a link to the
 * head of the linked list in index 'line*2' and a link to the tail at index
 * 'line*2+1'.
 * <p>
 * The set of active nodes can be traversed by
 *
 * <pre>
 * for (int line = startLine; line &lt; endLine; line++) {
 *     for (KnuthNode node = getNode(line); node != null; node = node.next) {
 *         // Do something with 'node'
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class BreakingAlgorithm {

    /** Maximum adjustment ration */
    protected static final int INFINITE_RATIO = 1000;

    private static final int MAX_RECOVERY_ATTEMPTS = 5;

    // constants identifying a subset of the feasible breaks
    /** All feasible breaks are ok. */
    public static final int ALL_BREAKS = 0;
    /** This forbids hyphenation. */
    public static final int NO_FLAGGED_PENALTIES = 1;
    /** wrap-option = "no-wrap". */
    public static final int ONLY_FORCED_BREAKS = 2;

    /** Holder for symbolic literals for the fitness classes */
    static final class FitnessClasses {

        private FitnessClasses() {
        }

        static final int VERY_TIGHT = 0;
        static final int TIGHT = 1;
        static final int LOOSE = 2;
        static final int VERY_LOOSE = 3;

        static final String[] NAMES = { "VERY TIGHT", "TIGHT", "LOOSE",
        "VERY LOOSE" };

        /**
         * Figure out the fitness class of this line (tight, loose, very tight
         * or very loose). See the section on "More Bells and Whistles" in
         * Knuth's "Breaking Paragraphs Into Lines".
         *
         * @param adjustRatio
         *            the adjustment ratio
         * @return the fitness class
         */
        static int computeFitness(final double adjustRatio) {
            if (adjustRatio < -0.5) {
                return FitnessClasses.VERY_TIGHT;
            } else if (adjustRatio <= 0.5) {
                return FitnessClasses.TIGHT;
            } else if (adjustRatio <= 1.0) {
                return FitnessClasses.LOOSE;
            } else {
                return FitnessClasses.VERY_LOOSE;
            }
        }
    }

    // parameters of Knuth's algorithm:
    /** Demerit for consecutive lines ending at flagged penalties. */
    protected int repeatedFlaggedDemerit = KnuthPenalty.FLAGGED_PENALTY;
    /**
     * Demerit for consecutive lines belonging to incompatible fitness classes .
     */
    protected int incompatibleFitnessDemerit = KnuthPenalty.FLAGGED_PENALTY;
    /**
     * Maximum number of consecutive lines ending with a flagged penalty. Only a
     * value >= 1 is a significant limit.
     */
    protected int maxFlaggedPenaltiesCount;

    /**
     * The threshold for considering breaks to be acceptable. The adjustment
     * ratio must be inferior to this threshold.
     */
    private double threshold;

    /**
     * The paragraph of KnuthElements.
     */
    protected KnuthSequence par;

    /**
     * The width of a line (or height of a column in page-breaking mode). -1
     * indicates that the line widths are different for each line.
     */
    protected int lineWidth = -1;
    /**
     * Force the algorithm to find a set of breakpoints, even if no feasible
     * breakpoints exist.
     */
    private boolean force = false;
    /**
     * If set to true, doesn't ignore break possibilities which are definitely
     * too short.
     */
    protected boolean considerTooShort = false;

    /**
     * When in forced mode, the best node leading to a too long line. The line
     * will be too long anyway, but this one will lead to a paragraph with
     * fewest demerits.
     */
    private KnuthNode lastTooLong;
    /**
     * When in forced mode, the best node leading to a too short line. The line
     * will be too short anyway, but this one will lead to a paragraph with
     * fewest demerits.
     */
    private KnuthNode lastTooShort;
    /**
     * The node to be reactivated if no set of feasible breakpoints can be found
     * for this paragraph.
     */
    private KnuthNode lastDeactivated;

    /** Alignment of the paragraph/page. One of EN_START, EN_JUSTIFY, etc. */
    protected int alignment;
    /** Alignment of the paragraph's last line. */
    protected int alignmentLast;
    /**
     * Used to handle the text-indent property (indent the first line of a
     * paragraph).
     */
    protected boolean indentFirstPart;

    /**
     * The set of active nodes in ascending line order. For each line l,
     * activeLines[2l] contains a link to l's first active node, and
     * activeLines[2l+1] a link to l's last active node. The line number l
     * corresponds to the number of the line ending at the node's breakpoint.
     */
    protected KnuthNode[] activeLines;

    /**
     * The number of active nodes.
     */
    protected int activeNodeCount;

    /**
     * The lowest available line in the set of active nodes.
     */
    protected int startLine = 0;

    /**
     * The highest + 1 available line in the set of active nodes.
     */
    protected int endLine = 0;

    /**
     * The total width of all elements handled so far.
     */
    protected int totalWidth;

    /**
     * The total stretch of all elements handled so far.
     */
    protected int totalStretch = 0;

    /**
     * The total shrink of all elements handled so far.
     */
    protected int totalShrink = 0;

    /**
     * Best records.
     */
    protected BestRecords best;

    private boolean partOverflowRecoveryActivated = true;
    private KnuthNode lastRecovered;

    /**
     * Create a new instance.
     *
     * @param align
     *            alignment of the paragraph/page. One of
     *            {@link Constants#EN_START}, {@link Constants#EN_JUSTIFY},
     *            {@link Constants#EN_CENTER}, {@link Constants#EN_END}. For
     *            pages, {@link Constants#EN_BEFORE} and
     *            {@link Constants#EN_AFTER} are mapped to the corresponding
     *            inline properties, {@link Constants#EN_START} and
     *            {@link Constants#EN_END}.
     * @param alignLast
     *            alignment of the paragraph's last line
     * @param first
     *            for the text-indent property ({@code true} if the first line
     *            of a paragraph should be indented)
     * @param partOverflowRecovery
     *            {@code true} if too long elements should be moved to the next
     *            line/part
     * @param maxFlagCount
     *            maximum allowed number of consecutive lines ending at a
     *            flagged penalty item
     */
    public BreakingAlgorithm(final int align, final int alignLast,
            final boolean first, final boolean partOverflowRecovery,
            final int maxFlagCount) {
        this.alignment = align;
        this.alignmentLast = alignLast;
        this.indentFirstPart = first;
        this.partOverflowRecoveryActivated = partOverflowRecovery;
        this.best = new BestRecords();
        this.maxFlaggedPenaltiesCount = maxFlagCount;
    }

    /**
     * Class recording all the informations of a feasible breaking point.
     */
    public class KnuthNode {
        /** index of the breakpoint represented by this node */
        public final int position; // CSOK: VisibilityModifier

        /** number of the line ending at this breakpoint */
        public final int line; // CSOK: VisibilityModifier

        /**
         * fitness class of the line ending at this breakpoint. One of 0, 1, 2,
         * 3.
         */
        public final int fitness; // CSOK: VisibilityModifier

        /** accumulated width of the KnuthElements up to after this breakpoint. */
        public final int totalWidth; // CSOK: VisibilityModifier

        /**
         * accumulated stretchability of the KnuthElements up to after this
         * breakpoint.
         */
        public final int totalStretch; // CSOK: VisibilityModifier

        /**
         * accumulated shrinkability of the KnuthElements up to after this
         * breakpoint.
         */
        public final int totalShrink; // CSOK: VisibilityModifier

        /** adjustment ratio if the line ends at this breakpoint */
        public final double adjustRatio; // CSOK: VisibilityModifier

        /** available stretch of the line ending at this breakpoint */
        public final int availableShrink; // CSOK: VisibilityModifier

        /** available shrink of the line ending at this breakpoint */
        public final int availableStretch; // CSOK: VisibilityModifier

        /** difference between target and actual line width */
        public final int difference; // CSOK: VisibilityModifier

        /** minimum total demerits up to this breakpoint */
        public double totalDemerits; // CSOK: VisibilityModifier

        /** best node for the preceding breakpoint */
        public KnuthNode previous; // CSOK: VisibilityModifier

        /** next possible node in the same line */
        public KnuthNode next; // CSOK: VisibilityModifier

        /**
         * Holds the number of subsequent recovery attempty that are made to get
         * content fit into a line.
         */
        public int fitRecoveryCounter = 0; // CSOK: VisibilityModifier

        /**
         * Construct node.
         *
         * @param position
         *            an integer
         * @param line
         *            an integer
         * @param fitness
         *            an integer
         * @param totalWidth
         *            an integer
         * @param totalStretch
         *            an integer
         * @param totalShrink
         *            an integer
         * @param adjustRatio
         *            a real number
         * @param availableShrink
         *            an integer
         * @param availableStretch
         *            an integer
         * @param difference
         *            an integer
         * @param totalDemerits
         *            a real number
         * @param previous
         *            a node
         */
        public KnuthNode(
                // CSOK: ParameterNumber
                final int position, final int line, final int fitness,
                final int totalWidth, final int totalStretch,
                final int totalShrink, final double adjustRatio,
                final int availableShrink, final int availableStretch,
                final int difference, final double totalDemerits,
                final KnuthNode previous) {
            this.position = position;
            this.line = line;
            this.fitness = fitness;
            this.totalWidth = totalWidth;
            this.totalStretch = totalStretch;
            this.totalShrink = totalShrink;
            this.adjustRatio = adjustRatio;
            this.availableShrink = availableShrink;
            this.availableStretch = availableStretch;
            this.difference = difference;
            this.totalDemerits = totalDemerits;
            this.previous = previous;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "<KnuthNode at " + this.position + " " + this.totalWidth
                    + "+" + this.totalStretch + "-" + this.totalShrink
                    + " line:" + this.line + " prev:"
                    + (this.previous != null ? this.previous.position : -1)
                    + " dem:" + this.totalDemerits + " fitness:"
                    + FitnessClasses.NAMES[this.fitness] + ">";
        }
    }

    /**
     * Class that stores, for each fitness class, the best active node that
     * could start a line of the corresponding fitness ending at the current
     * element.
     */
    protected class BestRecords {
        private static final double INFINITE_DEMERITS = Double.POSITIVE_INFINITY;

        private final double[] bestDemerits = new double[4];
        private final KnuthNode[] bestNode = new KnuthNode[4];
        private final double[] bestAdjust = new double[4];
        private final int[] bestDifference = new int[4];
        private final int[] bestAvailableShrink = new int[4];
        private final int[] bestAvailableStretch = new int[4];
        /**
         * Points to the fitness class which currently leads to the best
         * demerits.
         */
        private int bestIndex = -1;

        /** default constructor */
        public BestRecords() {
            reset();
        }

        /**
         * Registers the new best active node for the given fitness class.
         *
         * @param demerits
         *            the total demerits of the new optimal set of breakpoints
         * @param node
         *            the node starting the line ending at the current element
         * @param adjust
         *            adjustment ratio of the current line
         * @param availableShrink
         *            how much the current line can be shrinked
         * @param availableStretch
         *            how much the current line can be stretched
         * @param difference
         *            difference between the width of the considered line and
         *            the width of the "real" line
         * @param fitness
         *            fitness class of the current line
         */
        public void addRecord(final double demerits, final KnuthNode node,
                final double adjust, final int availableShrink,
                final int availableStretch, final int difference,
                final int fitness) {
            if (demerits > this.bestDemerits[fitness]) {
                log.error("New demerits value greater than the old one");
            }
            this.bestDemerits[fitness] = demerits;
            this.bestNode[fitness] = node;
            this.bestAdjust[fitness] = adjust;
            this.bestAvailableShrink[fitness] = availableShrink;
            this.bestAvailableStretch[fitness] = availableStretch;
            this.bestDifference[fitness] = difference;
            if (this.bestIndex == -1
                    || demerits < this.bestDemerits[this.bestIndex]) {
                this.bestIndex = fitness;
            }
        }

        /** @return true if has records (best index not -1) */
        public boolean hasRecords() {
            return this.bestIndex != -1;
        }

        /**
         * @param fitness
         *            fitness class (0, 1, 2 or 3, i.e. "tight" to "very loose")
         * @return true if there is a set of feasible breakpoints registered for
         *         the given fitness.
         */
        public boolean notInfiniteDemerits(final int fitness) {
            return this.bestDemerits[fitness] != INFINITE_DEMERITS;
        }

        /**
         * @param fitness
         *            to use
         * @return best demerits
         */
        public double getDemerits(final int fitness) {
            return this.bestDemerits[fitness];
        }

        /**
         * @param fitness
         *            to use
         * @return best node
         */
        public KnuthNode getNode(final int fitness) {
            return this.bestNode[fitness];
        }

        /**
         * @param fitness
         *            to use
         * @return adjustment
         */
        public double getAdjust(final int fitness) {
            return this.bestAdjust[fitness];
        }

        /**
         * @param fitness
         *            to use
         * @return available shrink
         */
        public int getAvailableShrink(final int fitness) {
            return this.bestAvailableShrink[fitness];
        }

        /**
         * @param fitness
         *            to use
         * @return available stretch
         */
        public int getAvailableStretch(final int fitness) {
            return this.bestAvailableStretch[fitness];
        }

        /**
         * @param fitness
         *            to use
         * @return difference
         */
        public int getDifference(final int fitness) {
            return this.bestDifference[fitness];
        }

        /** @return minimum demerits */
        public double getMinDemerits() {
            if (this.bestIndex != -1) {
                return getDemerits(this.bestIndex);
            } else {
                // anyway, this should never happen
                return INFINITE_DEMERITS;
            }
        }

        /** Reset when a new breakpoint is being considered. */
        public void reset() {
            for (int i = 0; i < 4; i++) {
                this.bestDemerits[i] = INFINITE_DEMERITS;
                // there is no need to reset the other arrays
            }
            this.bestIndex = -1;
        }
    }

    /**
     * @return the number of times the algorithm should try to move overflowing
     *         content to the next line/page.
     */
    protected int getMaxRecoveryAttempts() {
        return MAX_RECOVERY_ATTEMPTS;
    }

    /**
     * Controls the behaviour of the algorithm in cases where the first element
     * of a part overflows a line/page.
     *
     * @return true if the algorithm should try to send the element to the next
     *         line/page.
     */
    protected boolean isPartOverflowRecoveryActivated() {
        return this.partOverflowRecoveryActivated;
    }

    /**
     * Empty method, hook for subclasses. Called before determining the optimal
     * breakpoints corresponding to a given active node.
     *
     * @param total
     *            number of lines for the active node
     * @param demerits
     *            total demerits of the paragraph for the active node
     */
    public abstract void updateData1(final int total, final double demerits);

    /**
     * Empty method, hook for subclasses. Called when determining the optimal
     * breakpoints for a given active node.
     *
     * @param bestActiveNode
     *            a node in the chain of best active nodes, corresponding to one
     *            of the optimal breakpoints
     * @param sequence
     *            the corresponding paragraph
     * @param total
     *            the number of lines into which the paragraph will be broken
     */
    public abstract void updateData2(final KnuthNode bestActiveNode,
            final KnuthSequence sequence, final int total);

    /**
     * @param lineWidth
     *            the line width
     */
    public void setConstantLineWidth(final int lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * @param par
     *            the paragraph to break
     * @param threshold
     *            upper bound of the adjustment ratio
     * @param force
     *            {@code true} if a set of breakpoints must be found, even if
     *            there are no feasible ones
     * @param allowedBreaks
     *            the type(s) of breaks allowed. One of
     *            {@link #ONLY_FORCED_BREAKS}, {@link #NO_FLAGGED_PENALTIES} or
     *            {@link #ALL_BREAKS}.
     *
     * @return the number of effective breaks
     * @see #findBreakingPoints(KnuthSequence, int, double, boolean, int)
     */
    public int findBreakingPoints(final KnuthSequence par,
            final double threshold, final boolean force, final int allowedBreaks) {
        return findBreakingPoints(par, 0, threshold, force, allowedBreaks);
    }

    /**
     * Finds an optimal set of breakpoints for the given paragraph.
     *
     * @param par
     *            the paragraph to break
     * @param startIndex
     *            index of the Knuth element at which the breaking must start
     * @param threshold
     *            upper bound of the adjustment ratio
     * @param force
     *            {@code true} if a set of breakpoints must be found, even if
     *            there are no feasible ones
     * @param allowedBreaks
     *            the type(s) of breaks allowed. One of
     *            {@link #ONLY_FORCED_BREAKS}, {@link #NO_FLAGGED_PENALTIES} or
     *            {@link #ALL_BREAKS}.
     *
     * @return the number of effective breaks
     */
    public int findBreakingPoints(final KnuthSequence par,
            final int startIndex, final double threshold, final boolean force,
            final int allowedBreaks) {
        this.par = par;
        this.threshold = threshold;
        this.force = force;

        // initialize the algorithm
        initialize();

        // previous element in the paragraph is a KnuthBox?
        boolean previousIsBox = false;

        // index of the first KnuthBox in the sequence, in case of non-centered
        // alignment. For centered alignment, we need to take into account
        // preceding
        // penalties+glues used for the filler spaces
        int firstBoxIndex = startIndex;
        if (this.alignment != Constants.EN_CENTER) {
            firstBoxIndex = par.getFirstBoxIndex(startIndex);
        }
        firstBoxIndex = firstBoxIndex < 0 ? 0 : firstBoxIndex;

        // create an active node representing the starting point
        addNode(0,
                createNode(firstBoxIndex, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, null));
        KnuthNode lastForced = getNode(0);

        if (log.isTraceEnabled()) {
            log.trace("Looping over " + (par.size() - startIndex) + " elements");
            log.trace(par.toString());
        }

        // main loop
        for (int elementIndex = startIndex; elementIndex < par.size(); elementIndex++) {

            previousIsBox = handleElementAt(elementIndex, previousIsBox,
                    allowedBreaks).isBox();

            if (this.activeNodeCount == 0) {
                if (getIPDdifference() != 0) {
                    return handleIpdChange();
                }
                if (!force) {
                    log.debug("Could not find a set of breaking points "
                            + threshold);
                    return 0;
                }

                // lastDeactivated was a "good" break, while lastTooShort and
                // lastTooLong
                // were "bad" breaks since the beginning;
                // if it is not the node we just restarted from, lastDeactivated
                // can
                // replace either lastTooShort or lastTooLong
                if (this.lastDeactivated != null
                        && this.lastDeactivated != lastForced) {
                    replaceLastDeactivated();
                }

                if (this.lastTooShort == null
                        || lastForced.position == this.lastTooShort.position) {
                    lastForced = recoverFromOverflow();
                } else {
                    lastForced = this.lastTooShort;
                    this.lastRecovered = null;
                }
                elementIndex = restartFrom(lastForced, elementIndex);
            }

        }

        finish();

        // there is at least one set of breaking points
        // select one or more active nodes, removing the others from the list
        final int line = filterActiveNodes();

        // for each active node, create a set of breaking points
        for (int i = this.startLine; i < this.endLine; i++) {
            for (KnuthNode node = getNode(i); node != null; node = node.next) {
                updateData1(node.line, node.totalDemerits);
                calculateBreakPoints(node, par, node.line);
            }
        }

        this.activeLines = null;
        return line;
    }

    /**
     * obtain ipd difference
     *
     * @return an integer
     */
    protected int getIPDdifference() {
        return 0;
    }

    /**
     * handle ipd change
     *
     * @return an integer
     */
    protected int handleIpdChange() {
        throw new IllegalStateException();
    }

    /**
     * Recover from a {@link KnuthNode} leading to a line that is too long. The
     * default implementation creates a new node corresponding to a break point
     * after the previous node that led to a line that was too short.
     *
     * @param lastTooLong
     *            the node that leads to a "too long" line
     * @return node corresponding to a breakpoint after the previous "too short"
     *         line
     */
    protected KnuthNode recoverFromTooLong(final KnuthNode lastTooLong) {
        if (log.isDebugEnabled()) {
            log.debug("Recovering from too long: " + lastTooLong);
        }

        // if lastTooLong would be the very first break in the blockList, and
        // the first element in the paragraph is not a penalty, add an auxiliary
        // penalty now to make it possible to create a genuine 'empty' node that
        // represents a break before the first box/glue
        if (lastTooLong.previous.previous == null) {
            final ListElement el = (ListElement) this.par.get(0);
            if (!el.isPenalty()) {
                this.par.add(0, KnuthPenalty.DUMMY_ZERO_PENALTY);
            }
        }

        // content would overflow, insert empty line/page and try again
        return createNode(lastTooLong.previous.position,
                lastTooLong.previous.line + 1, 1, 0, 0, 0, 0, 0, 0, 0, 0,
                lastTooLong.previous);
    }

    /** Initializes the algorithm's variables. */
    protected void initialize() {
        this.totalWidth = 0;
        this.totalStretch = 0;
        this.totalShrink = 0;
        this.lastTooShort = null;
        this.lastTooLong = null;
        this.startLine = 0;
        this.endLine = 0;
        this.activeLines = new KnuthNode[20];
    }

    /**
     * Creates a new active node for a feasible breakpoint at the given
     * position. Only called in forced mode.
     *
     * @param position
     *            index of the element in the Knuth sequence
     * @param line
     *            number of the line ending at the breakpoint
     * @param fitness
     *            fitness class of the line ending at the breakpoint. One of 0,
     *            1, 2, 3.
     * @param totalWidth
     *            accumulated width of the KnuthElements up to after the
     *            breakpoint
     * @param totalStretch
     *            accumulated stretchability of the KnuthElements up to after
     *            the breakpoint
     * @param totalShrink
     *            accumulated shrinkability of the KnuthElements up to after the
     *            breakpoint
     * @param adjustRatio
     *            adjustment ratio if the line ends at this breakpoint
     * @param availableShrink
     *            available stretch of the line ending at this breakpoint
     * @param availableStretch
     *            available shrink of the line ending at this breakpoint
     * @param difference
     *            difference between target and actual line width
     * @param totalDemerits
     *            minimum total demerits up to the breakpoint
     * @param previous
     *            active node for the preceding breakpoint
     * @return a new node
     */
    protected KnuthNode createNode(
            // CSOK: ParameterNumber
            final int position, final int line, final int fitness,
            final int totalWidth, final int totalStretch,
            final int totalShrink, final double adjustRatio,
            final int availableShrink, final int availableStretch,
            final int difference, final double totalDemerits,
            final KnuthNode previous) {
        return new KnuthNode(position, line, fitness, totalWidth, totalStretch,
                totalShrink, adjustRatio, availableShrink, availableStretch,
                difference, totalDemerits, previous);
    }

    /**
     * Creates a new active node for a break from the best active node of the
     * given fitness class to the element at the given position.
     *
     * @param position
     *            index of the element in the Knuth sequence
     * @param line
     *            number of the line ending at the breakpoint
     * @param fitness
     *            fitness class of the line ending at the breakpoint. One of 0,
     *            1, 2, 3.
     * @param totalWidth
     *            accumulated width of the KnuthElements up to after the
     *            breakpoint
     * @param totalStretch
     *            accumulated stretchability of the KnuthElements up to after
     *            the breakpoint
     * @param totalShrink
     *            accumulated shrinkability of the KnuthElements up to after the
     *            breakpoint
     * @return a new node
     * @see #createNode(int, int, int, int, int, int, double, int, int, int,
     *      double, org.apache.fop.layoutmgr.BreakingAlgorithm.KnuthNode)
     * @see BreakingAlgorithm.BestRecords
     */
    protected KnuthNode createNode(final int position, final int line,
            final int fitness, final int totalWidth, final int totalStretch,
            final int totalShrink) {
        return new KnuthNode(position, line, fitness, totalWidth, totalStretch,
                totalShrink, this.best.getAdjust(fitness),
                this.best.getAvailableShrink(fitness),
                this.best.getAvailableStretch(fitness),
                this.best.getDifference(fitness),
                this.best.getDemerits(fitness), this.best.getNode(fitness));
    }

    /**
     * Return the last node that yielded a too short line.
     *
     * @return the node corresponding to the last too short line
     */
    protected final KnuthNode getLastTooShort() {
        return this.lastTooShort;
    }

    /**
     * Generic handler for a {@link KnuthElement} at the given {@code position},
     * taking into account whether the preceding element was a box, and which
     * type(s) of breaks are allowed. Non-overridable. This method simply serves
     * to route the call to one of the more specific handlers (
     * {@link #handleBox(KnuthBox)},
     * {@link #handleGlueAt(KnuthGlue,int,boolean,int)} or
     * {@link #handlePenaltyAt(KnuthPenalty,int,int)}. The specialized handlers
     * can be overridden by subclasses to add to or modify the default behavior
     * for the different types of elements.
     *
     * @param position
     *            the position index of the element in the paragraph
     * @param previousIsBox
     *            {@code true} if the previous element is a box
     * @param allowedBreaks
     *            the type(s) of breaks allowed; should be one of
     *            {@link #ALL_BREAKS}, {@link #NO_FLAGGED_PENALTIES} or
     *            {@link #ONLY_FORCED_BREAKS}
     * @return the handled element
     */
    protected final KnuthElement handleElementAt(final int position,
            final boolean previousIsBox, final int allowedBreaks) {
        final KnuthElement element = getElement(position);
        if (element.isBox()) {
            handleBox((KnuthBox) element);
        } else if (element.isGlue()) {
            handleGlueAt((KnuthGlue) element, position, previousIsBox,
                    allowedBreaks);
        } else if (element.isPenalty()) {
            handlePenaltyAt((KnuthPenalty) element, position, allowedBreaks);
        } else {
            throw new IllegalArgumentException(
                    "Unknown KnuthElement type: expecting KnuthBox, KnuthGlue or KnuthPenalty");
        }
        return element;
    }

    /**
     * Handle a {@link KnuthBox}. <br/>
     * <em>Note: default implementation just adds the box's width
     * to the total content width. Subclasses that do not keep track
     * of this themselves, but override this method, should remember
     * to call {@code super.handleBox(box)} to avoid unwanted side-effects.</em>
     *
     * @param box
     *            the {@link KnuthBox} to handle
     */
    protected void handleBox(final KnuthBox box) {
        // a KnuthBox object is not a legal line break,
        // just add the width to the total
        this.totalWidth += box.getWidth();
    }

    /**
     * Handle a {@link KnuthGlue} at the given position, taking into account the
     * additional parameters.
     *
     * @param glue
     *            the {@link KnuthGlue} to handle
     * @param position
     *            the position of the glue in the list
     * @param previousIsBox
     *            {@code true} if the preceding element is a box
     * @param allowedBreaks
     *            the type of breaks that are allowed
     */
    protected void handleGlueAt(final KnuthGlue glue, final int position,
            final boolean previousIsBox, final int allowedBreaks) {
        // a KnuthGlue object is a legal line break
        // only if the previous object is a KnuthBox
        // consider these glues according to the value of allowedBreaks
        if (previousIsBox && !(allowedBreaks == ONLY_FORCED_BREAKS)) {
            considerLegalBreak(glue, position);
        }
        this.totalWidth += glue.getWidth();
        this.totalStretch += glue.getStretch();
        this.totalShrink += glue.getShrink();
    }

    /**
     * Handle a {@link KnuthPenalty} at the given position, taking into account
     * the type of breaks allowed.
     *
     * @param penalty
     *            the {@link KnuthPenalty} to handle
     * @param position
     *            the position of the penalty in the list
     * @param allowedBreaks
     *            the type of breaks that are allowed
     */
    protected void handlePenaltyAt(final KnuthPenalty penalty,
            final int position, final int allowedBreaks) {
        // a KnuthPenalty is a legal line break
        // only if its penalty is not infinite;
        // consider all penalties, non-flagged penalties or non-forcing
        // penalties
        // according to the value of allowedBreaks
        if (penalty.getPenalty() < KnuthElement.INFINITE
                && (!(allowedBreaks == NO_FLAGGED_PENALTIES) || !penalty
                        .isPenaltyFlagged())
                && (!(allowedBreaks == ONLY_FORCED_BREAKS) || penalty
                                .isForcedBreak())) {
            considerLegalBreak(penalty, position);
        }
    }

    /**
     * Replace the last too-long or too-short node by the last deactivated node,
     * if applicable.
     */
    protected final void replaceLastDeactivated() {
        if (this.lastDeactivated.adjustRatio > 0) {
            // last deactivated was too short
            this.lastTooShort = this.lastDeactivated;
        } else {
            // last deactivated was too long or exactly the right width
            this.lastTooLong = this.lastDeactivated;
        }
    }

    /**
     * Recover from an overflow condition.
     *
     * @return the new {@code lastForced} node
     */
    protected KnuthNode recoverFromOverflow() {
        KnuthNode lastForced;
        if (isPartOverflowRecoveryActivated()) {
            if (this.lastRecovered == null) {
                this.lastRecovered = this.lastTooLong;
                if (log.isDebugEnabled()) {
                    log.debug("Recovery point: " + this.lastRecovered);
                }
            }
            final KnuthNode node = recoverFromTooLong(this.lastTooLong);
            lastForced = node;
            node.fitRecoveryCounter = this.lastTooLong.previous.fitRecoveryCounter + 1;
            if (log.isDebugEnabled()) {
                log.debug("first part doesn't fit into line, recovering: "
                        + node.fitRecoveryCounter);
            }
            if (node.fitRecoveryCounter > getMaxRecoveryAttempts()) {
                while (lastForced.fitRecoveryCounter > 0
                        && lastForced.previous != null) {
                    lastForced = lastForced.previous;
                    this.lastDeactivated = lastForced.previous;
                }
                lastForced = this.lastRecovered;
                this.lastRecovered = null;
                this.startLine = lastForced.line;
                this.endLine = lastForced.line;
                log.debug("rolled back...");
            }
        } else {
            lastForced = this.lastTooLong;
        }
        return lastForced;
    }

    /**
     * Restart from the given node at the given index.
     *
     * @param restartingNode
     *            the {@link KnuthNode} to restart from
     * @param currentIndex
     *            the current position index
     * @return the index of the restart point
     */
    protected int restartFrom(final KnuthNode restartingNode,
            final int currentIndex) {
        if (log.isDebugEnabled()) {
            log.debug("Restarting at node " + restartingNode);
        }

        restartingNode.totalDemerits = 0;
        addNode(restartingNode.line, restartingNode);
        this.startLine = restartingNode.line;
        this.endLine = this.startLine + 1;
        this.totalWidth = restartingNode.totalWidth;
        this.totalStretch = restartingNode.totalStretch;
        this.totalShrink = restartingNode.totalShrink;
        this.lastTooShort = null;
        this.lastTooLong = null;
        // the width, stretch and shrink already include the width,
        // stretch and shrink of the suppressed glues;
        // advance in the sequence in order to avoid taking into account
        // these elements twice
        int restartingIndex = restartingNode.position;
        while (restartingIndex + 1 < this.par.size()
                && !getElement(restartingIndex + 1).isBox()) {
            restartingIndex++;
        }
        return restartingIndex;
    }

    /**
     * Determines if the given breakpoint is a feasible breakpoint. That is, if
     * a decent line may be built between one of the currently active nodes and
     * this breakpoint.
     *
     * @param element
     *            the paragraph's element to consider
     * @param elementIdx
     *            the element's index inside the paragraph
     */
    protected void considerLegalBreak(final KnuthElement element,
            final int elementIdx) {

        if (log.isTraceEnabled()) {
            log.trace("considerLegalBreak() at " + elementIdx + " ("
                    + this.totalWidth + "+" + this.totalStretch + "-"
                    + this.totalShrink + "), parts/lines: " + this.startLine
                    + "-" + this.endLine);
            log.trace("\tCurrent active node list: " + this.activeNodeCount
                    + " " + this.toString("\t"));
        }

        this.lastDeactivated = null;
        this.lastTooLong = null;
        for (int line = this.startLine; line < this.endLine; line++) {
            for (KnuthNode node = getNode(line); node != null; node = node.next) {
                if (node.position == elementIdx) {
                    continue;
                }
                final int difference = computeDifference(node, element,
                        elementIdx);
                if (!elementCanEndLine(element, this.endLine, difference)) {
                    log.trace("Skipping legal break");
                    break;
                }

                final double r = computeAdjustmentRatio(node, difference);
                final int availableShrink = this.totalShrink - node.totalShrink;
                final int availableStretch = this.totalStretch
                        - node.totalStretch;

                if (log.isTraceEnabled()) {
                    log.trace("\tr=" + r + " difference=" + difference);
                    log.trace("\tline=" + line);
                }

                // The line would be too long.
                if (r < -1 || element.isForcedBreak()) {
                    deactivateNode(node, line);
                }

                final int fitnessClass = FitnessClasses.computeFitness(r);
                final double demerits = computeDemerits(node, element,
                        fitnessClass, r);
                // The line is within the available shrink and the threshold.
                if (r >= -1 && r <= this.threshold) {
                    activateNode(node, difference, r, demerits, fitnessClass,
                            availableShrink, availableStretch);
                }

                // The line is way too short/long, but we are in forcing mode,
                // so a node is
                // calculated and stored in lastValidNode.
                if (this.force && (r <= -1 || r > this.threshold)) {
                    forceNode(node, line, elementIdx, difference, r, demerits,
                            fitnessClass, availableShrink, availableStretch);
                }
            }
            addBreaks(line, elementIdx);
        }
    }

    /**
     * Check if the given {@link KnuthElement} can end the line with the given
     * number.
     *
     * @param element
     *            the element
     * @param line
     *            the line number
     * @param difference
     *            an integer
     * @return {@code true} if the element can end the line
     */
    protected boolean elementCanEndLine(final KnuthElement element,
            final int line, final int difference) {
        return !element.isPenalty()
                || element.getPenalty() < KnuthElement.INFINITE;
    }

    /**
     * Force the given {@link KnuthNode}, and register it.
     *
     * @param node
     *            the node
     * @param line
     *            the line number
     * @param elementIdx
     *            the position index of the element
     * @param difference
     *            the difference between content-length and avaialable width
     * @param r
     *            the adjustment ratio
     * @param demerits
     *            demerits produced by the node
     * @param fitnessClass
     *            the fitness class
     * @param availableShrink
     *            the available amount of shrink
     * @param availableStretch
     *            tha available amount of stretch
     */
    protected void forceNode(
            final KnuthNode node, // CSOK: ParameterNumber
            final int line, final int elementIdx, final int difference,
            final double r, final double demerits, final int fitnessClass,
            final int availableShrink, final int availableStretch) {

        int newWidth = this.totalWidth;
        int newStretch = this.totalStretch;
        int newShrink = this.totalShrink;

        // add the width, stretch and shrink of glue elements after
        // the break
        // this does not affect the dimension of the line / page, only
        // the values stored in the node; these would be as if the break
        // was just before the next box element, thus ignoring glues and
        // penalties between the "real" break and the following box
        for (int i = elementIdx; i < this.par.size(); i++) {
            final KnuthElement tempElement = getElement(i);
            if (tempElement.isBox()) {
                break;
            } else if (tempElement.isGlue()) {
                newWidth += tempElement.getWidth();
                newStretch += tempElement.getStretch();
                newShrink += tempElement.getShrink();
            } else if (tempElement.isForcedBreak() && i != elementIdx) {
                break;
            }
        }

        if (r <= -1) {
            log.debug("Considering tooLong, demerits=" + demerits);
            if (this.lastTooLong == null
                    || demerits < this.lastTooLong.totalDemerits) {
                this.lastTooLong = createNode(elementIdx, line + 1,
                        fitnessClass, newWidth, newStretch, newShrink, r,
                        availableShrink, availableStretch, difference,
                        demerits, node);
                if (log.isTraceEnabled()) {
                    log.trace("Picking tooLong " + this.lastTooLong);
                }
            }
        } else {
            if (this.lastTooShort == null
                    || demerits <= this.lastTooShort.totalDemerits) {
                if (this.considerTooShort) {
                    // consider possibilities which are too short
                    this.best.addRecord(demerits, node, r, availableShrink,
                            availableStretch, difference, fitnessClass);
                }
                this.lastTooShort = createNode(elementIdx, line + 1,
                        fitnessClass, newWidth, newStretch, newShrink, r,
                        availableShrink, availableStretch, difference,
                        demerits, node);
                if (log.isTraceEnabled()) {
                    log.trace("Picking tooShort " + this.lastTooShort);
                }
            }
        }
    }

    /**
     * Activate the given node. Will result in the given {@link KnuthNode} being
     * registered as a feasible breakpoint, if the {@code demerits} are better
     * than that of the best node registered for the given {@code fitnessClass}.
     *
     * @param node
     *            the node
     * @param difference
     *            the difference between content-length and available width
     * @param r
     *            the adjustment ratio
     * @param demerits
     *            demerits produced by the node
     * @param fitnessClass
     *            the fitness class
     * @param availableShrink
     *            the available amount of shrink
     * @param availableStretch
     *            the available amount of stretch
     */
    protected void activateNode(final KnuthNode node, final int difference,
            final double r, final double demerits, final int fitnessClass,
            final int availableShrink, final int availableStretch) {

        if (log.isTraceEnabled()) {
            log.trace("\tDemerits=" + demerits);
            log.trace("\tFitness class=" + FitnessClasses.NAMES[fitnessClass]);
        }

        if (demerits < this.best.getDemerits(fitnessClass)) {
            // updates best demerits data
            this.best.addRecord(demerits, node, r, availableShrink,
                    availableStretch, difference, fitnessClass);
            this.lastTooShort = null;
        }
    }

    /**
     * Deactivate the given node
     *
     * @param node
     *            the node
     * @param line
     *            the line number
     */
    protected void deactivateNode(final KnuthNode node, final int line) {
        // Deactivate node...
        if (log.isTraceEnabled()) {
            log.trace("Removing " + node);
        }
        removeNode(line, node);
        // ... and remember it, if it was a good candidate
        this.lastDeactivated = compareNodes(this.lastDeactivated, node);
    }

    /**
     * Adds new active nodes for breaks at the given element.
     *
     * @param line
     *            number of the previous line; this element will end line number
     *            (line+1)
     * @param elementIdx
     *            the element's index
     */
    private void addBreaks(final int line, final int elementIdx) {
        if (!this.best.hasRecords()) {
            return;
        }

        int newWidth = this.totalWidth;
        int newStretch = this.totalStretch;
        int newShrink = this.totalShrink;

        // add the width, stretch and shrink of glue elements after
        // the break
        // this does not affect the dimension of the line / page, only
        // the values stored in the node; these would be as if the break
        // was just before the next box element, thus ignoring glues and
        // penalties between the "real" break and the following box
        for (int i = elementIdx; i < this.par.size(); i++) {
            final KnuthElement tempElement = getElement(i);
            if (tempElement.isBox()) {
                break;
            } else if (tempElement.isGlue()) {
                newWidth += tempElement.getWidth();
                newStretch += tempElement.getStretch();
                newShrink += tempElement.getShrink();
            } else if (tempElement.isForcedBreak() && i != elementIdx) {
                break;
            }
        }

        // add nodes to the active nodes list
        final double minimumDemerits = this.best.getMinDemerits()
                + this.incompatibleFitnessDemerit;
        for (int i = 0; i <= 3; i++) {
            if (this.best.notInfiniteDemerits(i)
                    && this.best.getDemerits(i) <= minimumDemerits) {
                // the nodes in activeList must be ordered
                // by line number and position;
                if (log.isTraceEnabled()) {
                    log.trace("\tInsert new break in list of "
                            + this.activeNodeCount + " from fitness class "
                            + FitnessClasses.NAMES[i]);
                }
                final KnuthNode newNode = createNode(elementIdx, line + 1, i,
                        newWidth, newStretch, newShrink);
                addNode(line + 1, newNode);
            }
        }
        this.best.reset();
    }

    /**
     * Return the difference between the natural width of a line that would be
     * made between the given active node and the given element, and the
     * available width of the real line.
     *
     * @param activeNode
     *            node for the previous breakpoint
     * @param element
     *            currently considered breakpoint
     * @param elementIndex
     *            index of the element that is considered as a breakpoint
     * @return The difference in width. Positive numbers mean extra space in the
     *         line, negative number that the line overflows.
     */
    protected int computeDifference(final KnuthNode activeNode,
            final KnuthElement element, final int elementIndex) {
        // compute the adjustment ratio
        int actualWidth = this.totalWidth - activeNode.totalWidth;
        if (element.isPenalty()) {
            actualWidth += element.getWidth();
        }
        return getLineWidth() - actualWidth;
    }

    /**
     * Return the adjustment ratio needed to make up for the difference. A ratio
     * of
     * <ul>
     * <li>0 means that the break has the exact right width</li>
     * <li>&gt;= -1 &amp;&amp; &lt; 0 means that the break is wider than the
     * line, but within the minimim values of the glues.</li>
     * <li>&gt;0 &amp;&amp; &lt; 1 means that the break is smaller than the line
     * width, but within the maximum values of the glues.</li>
     * <li>&gt; 1 means that the break is too small to make up for the glues.</li>
     * </ul>
     *
     * @param activeNode
     *            the currently active node
     * @param difference
     *            the difference between content-length and available width
     * @return The adjustment ratio.
     */
    protected double computeAdjustmentRatio(final KnuthNode activeNode,
            final int difference) {
        // compute the adjustment ratio
        if (difference > 0) {
            final int maxAdjustment = this.totalStretch
                    - activeNode.totalStretch;
            if (maxAdjustment > 0) {
                return (double) difference / maxAdjustment;
            } else {
                return INFINITE_RATIO;
            }
        } else if (difference < 0) {
            final int maxAdjustment = this.totalShrink - activeNode.totalShrink;
            if (maxAdjustment > 0) {
                return (double) difference / maxAdjustment;
            } else {
                return -INFINITE_RATIO;
            }
        } else {
            return 0;
        }
    }

    /**
     * Computes the demerits of the current breaking (that is, up to the given
     * element), if the next-to-last chosen breakpoint is the given active node.
     * This adds to the total demerits of the given active node, the demerits of
     * a line starting at this node and ending at the given element.
     *
     * @param activeNode
     *            considered preceding line break
     * @param element
     *            considered current line break
     * @param fitnessClass
     *            fitness of the current line
     * @param r
     *            adjustment ratio for the current line
     * @return the demerit of the current line
     */
    protected double computeDemerits(final KnuthNode activeNode,
            final KnuthElement element, final int fitnessClass, final double r) {
        double demerits = 0;
        // compute demerits
        double f = Math.abs(r);
        f = 1 + 100 * f * f * f;
        if (element.isPenalty()) {
            final double penalty = element.getPenalty();
            if (penalty >= 0) {
                f += penalty;
                demerits = f * f;
            } else if (!element.isForcedBreak()) {
                demerits = f * f - penalty * penalty;
            } else {
                demerits = f * f;
            }
        } else {
            demerits = f * f;
        }

        if (element.isPenalty()
                && ((KnuthPenalty) element).isPenaltyFlagged()
                && getElement(activeNode.position).isPenalty()
                && ((KnuthPenalty) getElement(activeNode.position))
                .isPenaltyFlagged()) {
            // add demerit for consecutive breaks at flagged penalties
            demerits += this.repeatedFlaggedDemerit;
            // there are at least two consecutive lines ending with a flagged
            // penalty;
            // check if the previous line end with a flagged penalty too,
            // and if this situation is allowed
            int flaggedPenaltiesCount = 2;
            for (KnuthNode prevNode = activeNode.previous; prevNode != null
                    && flaggedPenaltiesCount <= this.maxFlaggedPenaltiesCount; prevNode = prevNode.previous) {
                final KnuthElement prevElement = getElement(prevNode.position);
                if (prevElement.isPenalty()
                        && ((KnuthPenalty) prevElement).isPenaltyFlagged()) {
                    // the previous line ends with a flagged penalty too
                    flaggedPenaltiesCount++;
                } else {
                    // the previous line does not end with a flagged penalty,
                    // exit the loop
                    break;
                }
            }
            if (this.maxFlaggedPenaltiesCount >= 1
                    && flaggedPenaltiesCount > this.maxFlaggedPenaltiesCount) {
                // add infinite demerits, so this break will not be chosen
                // unless there isn't any alternative break
                demerits += BestRecords.INFINITE_DEMERITS;
            }
        }
        if (Math.abs(fitnessClass - activeNode.fitness) > 1) {
            // add demerit for consecutive breaks
            // with very different fitness classes
            demerits += this.incompatibleFitnessDemerit;
        }
        demerits += activeNode.totalDemerits;
        return demerits;
    }

    /**
     * Hook for subclasses to trigger special behavior after ending the main
     * loop in {@link #findBreakingPoints(KnuthSequence,int,double,boolean,int)}
     */
    protected void finish() {
        if (log.isTraceEnabled()) {
            log.trace("Main loop completed " + this.activeNodeCount);
            log.trace("Active nodes=" + toString(""));
        }
    }

    /**
     * Return the element at index idx in the paragraph.
     *
     * @param idx
     *            index of the element.
     * @return the element at index idx in the paragraph.
     */
    protected KnuthElement getElement(final int idx) {
        return (KnuthElement) this.par.get(idx);
    }

    /**
     * Compare two KnuthNodes and return the node with the least demerit.
     *
     * @param node1
     *            The first knuth node.
     * @param node2
     *            The other knuth node.
     * @return the node with the least demerit.
     */
    protected KnuthNode compareNodes(final KnuthNode node1,
            final KnuthNode node2) {
        if (node1 == null || node2.position > node1.position) {
            return node2;
        }
        if (node2.position == node1.position) {
            if (node2.totalDemerits < node1.totalDemerits) {
                return node2;
            }
        }
        return node1;
    }

    /**
     * Add a node at the end of the given line's existing active nodes. If this
     * is the first node in the line, adjust endLine accordingly.
     *
     * @param line
     *            number of the line ending at the node's corresponding
     *            breakpoint
     * @param node
     *            the active node to add
     */
    protected void addNode(final int line, final KnuthNode node) {
        final int headIdx = line * 2;
        if (headIdx >= this.activeLines.length) {
            final KnuthNode[] oldList = this.activeLines;
            this.activeLines = new KnuthNode[headIdx + headIdx];
            System.arraycopy(oldList, 0, this.activeLines, 0, oldList.length);
        }
        node.next = null;
        if (this.activeLines[headIdx + 1] != null) {
            this.activeLines[headIdx + 1].next = node;
        } else {
            this.activeLines[headIdx] = node;
            this.endLine = line + 1;
        }
        this.activeLines[headIdx + 1] = node;
        this.activeNodeCount++;
    }

    /**
     * Remove the given active node registered for the given line. If there are
     * no more active nodes for this line, adjust the startLine accordingly.
     *
     * @param line
     *            number of the line ending at the node's corresponding
     *            breakpoint
     * @param node
     *            the node to deactivate
     */
    protected void removeNode(final int line, final KnuthNode node) {
        final int headIdx = line * 2;
        KnuthNode n = getNode(line);
        if (n != node) {
            // nodes could be rightly deactivated in a different order
            KnuthNode prevNode = null;
            while (n != node) {
                prevNode = n;
                n = n.next;
            }
            prevNode.next = n.next;
            if (prevNode.next == null) {
                this.activeLines[headIdx + 1] = prevNode;
            }
        } else {
            this.activeLines[headIdx] = node.next;
            if (node.next == null) {
                this.activeLines[headIdx + 1] = null;
            }
            while (this.startLine < this.endLine
                    && getNode(this.startLine) == null) {
                this.startLine++;
            }
        }
        this.activeNodeCount--;
    }

    /**
     * Returns the first active node for the given line.
     *
     * @param line
     *            the line/part number
     * @return the requested active node
     */
    protected KnuthNode getNode(final int line) {
        return this.activeLines[line * 2];
    }

    /**
     * Returns the line/part width of a given line/part.
     *
     * @param line
     *            the line/part number
     * @return the width/length in millipoints
     */
    protected int getLineWidth(final int line) {
        assert this.lineWidth >= 0;
        return this.lineWidth;
    }

    /** @return the constant line/part width or -1 if there is no such value */
    protected int getLineWidth() {
        return this.lineWidth;
    }

    /**
     * Creates a string representation of the active nodes. Used for debugging.
     *
     * @param prepend
     *            a string to prepend on each entry
     * @return the requested string
     */
    public String toString(final String prepend) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = this.startLine; i < this.endLine; i++) {
            for (KnuthNode node = getNode(i); node != null; node = node.next) {
                sb.append(prepend).append('\t').append(node).append(",\n");
            }
        }
        sb.append(prepend).append("]");
        return sb.toString();
    }

    /**
     * Filter active nodes.
     *
     * @return an integer
     */
    protected abstract int filterActiveNodes();

    /**
     * Determines the set of optimal breakpoints corresponding to the given
     * active node.
     *
     * @param node
     *            the active node
     * @param par
     *            the corresponding paragraph
     * @param total
     *            the number of lines into which the paragraph will be broken
     */
    protected void calculateBreakPoints(final KnuthNode node,
            final KnuthSequence par, final int total) {
        KnuthNode bestActiveNode = node;
        // use bestActiveNode to determine the optimum breakpoints
        for (int i = node.line; i > 0; i--) {
            updateData2(bestActiveNode, par, total);
            bestActiveNode = bestActiveNode.previous;
        }
    }

    /** @return the alignment for normal lines/parts */
    public int getAlignment() {
        return this.alignment;
    }

    /** @return the alignment for the last line/part */
    public int getAlignmentLast() {
        return this.alignmentLast;
    }

}
