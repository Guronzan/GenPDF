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

/* $Id: LineLayoutPossibilities.java 985571 2010-08-14 19:28:26Z jeremias $ */

package org.apache.fop.layoutmgr.inline;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.layoutmgr.Position;

/**
 * Line layout possibilities.
 */
@Slf4j
public class LineLayoutPossibilities {

    private final class Possibility {
        private final int lineCount;
        private final double demerits;
        private final List breakPositions;

        private Possibility(final int lc, final double dem) {
            this.lineCount = lc;
            this.demerits = dem;
            this.breakPositions = new java.util.ArrayList(lc);
        }

        private int getLineCount() {
            return this.lineCount;
        }

        private double getDemerits() {
            return this.demerits;
        }

        private void addBreakPosition(final Position pos) {
            // Positions are always added with index 0 because
            // they are created backward, from the last one to
            // the first one
            this.breakPositions.add(0, pos);
        }

        private Position getBreakPosition(final int i) {
            return (Position) this.breakPositions.get(i);
        }
    }

    private List<Possibility> possibilitiesList;
    private List<Possibility> savedPossibilities;
    private int minimumIndex;
    private int optimumIndex;
    private int maximumIndex;
    private int chosenIndex;
    private int savedOptLineCount;

    /** default constructor */
    public LineLayoutPossibilities() {
        this.possibilitiesList = new java.util.ArrayList<Possibility>();
        this.savedPossibilities = new java.util.ArrayList<Possibility>();
        this.optimumIndex = -1;
    }

    /**
     * Add possibility.
     *
     * @param ln
     *            line number
     * @param dem
     *            demerits
     */
    public void addPossibility(final int ln, final double dem) {
        this.possibilitiesList.add(new Possibility(ln, dem));
        if (this.possibilitiesList.size() == 1) {
            // first Possibility added
            this.minimumIndex = 0;
            this.optimumIndex = 0;
            this.maximumIndex = 0;
            this.chosenIndex = 0;
        } else {
            if (dem < this.possibilitiesList.get(this.optimumIndex)
                    .getDemerits()) {
                this.optimumIndex = this.possibilitiesList.size() - 1;
                this.chosenIndex = this.optimumIndex;
            }
            if (ln < this.possibilitiesList.get(this.minimumIndex)
                    .getLineCount()) {
                this.minimumIndex = this.possibilitiesList.size() - 1;
            }
            if (ln > this.possibilitiesList.get(this.maximumIndex)
                    .getLineCount()) {
                this.maximumIndex = this.possibilitiesList.size() - 1;
            }
        }
    }

    /**
     * Save in a different array the computed Possibilities, so
     * possibilitiesList is ready to store different Possibilities.
     *
     * @param bSaveOptLineCount
     *            true if should save optimum line count
     */
    public void savePossibilities(final boolean bSaveOptLineCount) {
        if (bSaveOptLineCount) {
            this.savedOptLineCount = getOptLineCount();
        } else {
            this.savedOptLineCount = 0;
        }
        this.savedPossibilities = this.possibilitiesList;
        this.possibilitiesList = new java.util.ArrayList();
    }

    /**
     * Replace the Possibilities stored in possibilitiesList with the ones
     * stored in savedPossibilities and having the same line number.
     */
    public void restorePossibilities() {
        int index = 0;
        while (this.savedPossibilities.size() > 0) {
            final Possibility restoredPossibility = this.savedPossibilities
                    .remove(0);
            if (restoredPossibility.getLineCount() < getMinLineCount()) {
                // if the line number of restoredPossibility is less than the
                // minimum one,
                // add restoredPossibility at the beginning of the list
                this.possibilitiesList.add(0, restoredPossibility);
                // update minimumIndex
                this.minimumIndex = 0;
                // shift the other indexes;
                this.optimumIndex++;
                this.maximumIndex++;
                this.chosenIndex++;
            } else if (restoredPossibility.getLineCount() > getMaxLineCount()) {
                // if the line number of restoredPossibility is greater than the
                // maximum one,
                // add restoredPossibility at the end of the list
                this.possibilitiesList.add(this.possibilitiesList.size(),
                        restoredPossibility);
                // update maximumIndex
                this.maximumIndex = this.possibilitiesList.size() - 1;
                index = this.maximumIndex;
            } else {
                // find the index of the Possibility that will be replaced
                while (index < this.maximumIndex
                        && getLineCount(index) < restoredPossibility
                                .getLineCount()) {
                    index++;
                }
                if (getLineCount(index) == restoredPossibility.getLineCount()) {
                    this.possibilitiesList.set(index, restoredPossibility);
                } else {
                    // this should not happen
                    log.error("LineLayoutPossibilities restorePossibilities(),"
                            + " min= " + getMinLineCount() + " max= "
                            + getMaxLineCount() + " restored= "
                            + restoredPossibility.getLineCount());
                    return;
                }
            }
            // update optimumIndex and chosenIndex
            if (this.savedOptLineCount == 0
                    && getDemerits(this.optimumIndex) > restoredPossibility
                            .getDemerits()
                    || this.savedOptLineCount != 0
                    && restoredPossibility.getLineCount() == this.savedOptLineCount) {
                this.optimumIndex = index;
                this.chosenIndex = this.optimumIndex;
            }
        }
        // log.debug(">> minLineCount = " + getMinLineCount()
        // + " optLineCount = " + getOptLineCount() + " maxLineCount() = " +
        // getMaxLineCount());
    }

    /**
     * @param pos
     *            a position
     * @param i
     *            an index into posibilities list
     */
    public void addBreakPosition(final Position pos, final int i) {
        this.possibilitiesList.get(i).addBreakPosition(pos);
    }

    /** @return true if can use more lines */
    public boolean canUseMoreLines() {
        return getOptLineCount() < getMaxLineCount();
    }

    /** @return true if can use fewer lines */
    public boolean canUseLessLines() {
        return getMinLineCount() < getOptLineCount();
    }

    /** @return the line count of the minimum index */
    public int getMinLineCount() {
        return getLineCount(this.minimumIndex);
    }

    /** @return the line count of the optimum index */
    public int getOptLineCount() {
        return getLineCount(this.optimumIndex);
    }

    /** @return the line count of the maximum index */
    public int getMaxLineCount() {
        return getLineCount(this.maximumIndex);
    }

    /** @return the line count of the chosen index */
    public int getChosenLineCount() {
        return getLineCount(this.chosenIndex);
    }

    /**
     * @param i
     *            the posibilities list index
     * @return the line count
     */
    public int getLineCount(final int i) {
        return this.possibilitiesList.get(i).getLineCount();
    }

    /** @return the demerits of the chosen index */
    public double getChosenDemerits() {
        return getDemerits(this.chosenIndex);
    }

    /**
     * @param i
     *            the posibilities list index
     * @return the demerits
     */
    public double getDemerits(final int i) {
        return this.possibilitiesList.get(i).getDemerits();
    }

    /** @return the possibilities count */
    public int getPossibilitiesNumber() {
        return this.possibilitiesList.size();
    }

    /**
     * @param i
     *            the break position index
     * @return the chosen position
     */
    public Position getChosenPosition(final int i) {
        return this.possibilitiesList.get(this.chosenIndex).getBreakPosition(i);
    }

    /**
     * @param adj
     *            the adjustment
     * @return the adjustment or zero
     */
    public int applyLineCountAdjustment(final int adj) {
        if (adj >= getMinLineCount() - getChosenLineCount()
                && adj <= getMaxLineCount() - getChosenLineCount()
                && getLineCount(this.chosenIndex + adj) == getChosenLineCount()
                        + adj) {
            this.chosenIndex += adj;
            log.debug("chosenLineCount= " + (getChosenLineCount() - adj)
                    + " adjustment= " + adj + " => chosenLineCount= "
                    + getLineCount(this.chosenIndex));
            return adj;
        } else {
            // this should not happen!
            log.warn("Cannot apply the desired line count adjustment.");
            return 0;
        }
    }

    /** print all */
    public void printAll() {
        log.info("++++++++++");
        log.info(" " + this.possibilitiesList.size() + " possibility':");
        for (int i = 0; i < this.possibilitiesList.size(); i++) {
            log.info("   " + this.possibilitiesList.get(i).getLineCount()
                    + (i == this.optimumIndex ? " *" : "")
                    + (i == this.minimumIndex ? " -" : "")
                    + (i == this.maximumIndex ? " +" : ""));
        }
        log.info("++++++++++");
    }
}
