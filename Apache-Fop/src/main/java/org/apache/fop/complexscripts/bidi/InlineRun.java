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

/* $Id$ */

package org.apache.fop.complexscripts.bidi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.fop.area.inline.Anchor;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineBlockParent;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.area.inline.InlineViewport;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.Space;
import org.apache.fop.area.inline.SpaceArea;
import org.apache.fop.area.inline.UnresolvedPageNumber;
import org.apache.fop.area.inline.WordArea;
import org.apache.fop.util.CharUtilities;

// CSOFF: EmptyForIteratorPadCheck
// CSOFF: InnerAssignmentCheck
// CSOFF: NoWhitespaceAfterCheck
// CSOFF: SimplifyBooleanReturnCheck

/**
 * The <code>InlineRun</code> class is a utility class, the instances of which
 * are used to capture a sequence of reordering levels associated with an inline
 * area.
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
public class InlineRun {
    private final InlineArea inline;
    private final int[] levels;
    private int minLevel;
    private int maxLevel;
    private int reversals;

    /**
     * Primary constructor.
     *
     * @param inline
     *            which generated this inline run
     * @param levels
     *            levels array
     */
    public InlineRun(final InlineArea inline, final int[] levels) {
        assert inline != null;
        assert levels != null;
        this.inline = inline;
        this.levels = levels;
        setMinMax(levels);
    }

    /**
     * Alternate constructor.
     *
     * @param inline
     *            which generated this inline run
     * @param level
     *            for each index
     * @param count
     *            of indices
     */
    public InlineRun(final InlineArea inline, final int level, final int count) {
        this(inline, makeLevels(level, count));
    }

    /**
     * Obtain inline area that generated this inline run.
     *
     * @return inline area that generated this inline run.
     */
    public InlineArea getInline() {
        return this.inline;
    }

    /**
     * Obtain minimum bidi level for this run.
     *
     * @return minimum bidi level
     */
    public int getMinLevel() {
        return this.minLevel;
    }

    /**
     * Obtain maximum bidi level for this run.
     *
     * @return maximum bidi level
     */
    public int getMaxLevel() {
        return this.maxLevel;
    }

    private void setMinMax(final int[] levels) {
        int mn = Integer.MAX_VALUE;
        int mx = Integer.MIN_VALUE;
        if (levels != null && levels.length > 0) {
            for (final int l : levels) {
                if (l < mn) {
                    mn = l;
                }
                if (l > mx) {
                    mx = l;
                }
            }
        } else {
            mn = mx = -1;
        }
        this.minLevel = mn;
        this.maxLevel = mx;
    }

    /**
     * Determine if this run has homogenous (same valued) bidi levels.
     *
     * @return true if homogenous
     */
    public boolean isHomogenous() {
        return this.minLevel == this.maxLevel;
    }

    /**
     * Split this inline run into homogenous runs.
     *
     * @return list of new runs
     */
    public List<InlineRun> split() {
        final List<InlineRun> runs = new ArrayList<InlineRun>();
        for (int i = 0, n = this.levels.length; i < n;) {
            final int l = this.levels[i];
            final int s = i;
            int e = s;
            while (e < n) {
                if (this.levels[e] != l) {
                    break;
                } else {
                    ++e;
                }
            }
            if (s < e) {
                runs.add(new InlineRun(this.inline, l, e - s));
            }
            i = e;
        }
        assert runs.size() < 2 : "heterogeneous inlines not yet supported!!";
        return runs;
    }

    /**
     * Update a min/max array to correspond with this run's min/max values.
     *
     * @param mm
     *            reference to min/max array
     */
    public void updateMinMax(final int[] mm) {
        if (this.minLevel < mm[0]) {
            mm[0] = this.minLevel;
        }
        if (this.maxLevel > mm[1]) {
            mm[1] = this.maxLevel;
        }
    }

    /**
     * Determine if run needs mirroring.
     *
     * @return true if run is homogenous and odd (i.e., right to left)
     */
    public boolean maybeNeedsMirroring() {
        return this.minLevel == this.maxLevel && (this.minLevel & 1) != 0;
    }

    /**
     * Reverse run (by incrementing reversal count, not actually reversing).
     */
    public void reverse() {
        this.reversals++;
    }

    /**
     * Reverse inline area if it is a word area and it requires reversal.
     *
     * @param mirror
     *            if true then also mirror characters
     */
    public void maybeReverseWord(final boolean mirror) {
        if (this.inline instanceof WordArea) {
            final WordArea w = (WordArea) this.inline;
            // if not already reversed, then reverse now
            if (!w.isReversed()) {
                if ((this.reversals & 1) != 0) {
                    w.reverse(mirror);
                } else if (mirror && maybeNeedsMirroring()) {
                    w.mirror();
                }
            }
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof InlineRun) {
            final InlineRun ir = (InlineRun) o;
            if (ir.inline != this.inline) {
                return false;
            } else if (ir.minLevel != this.minLevel) {
                return false;
            } else if (ir.maxLevel != this.maxLevel) {
                return false;
            } else if (ir.levels != null && this.levels != null) {
                if (ir.levels.length != this.levels.length) {
                    return false;
                } else {
                    for (int i = 0, n = this.levels.length; i < n; i++) {
                        if (ir.levels[i] != this.levels[i]) {
                            return false;
                        }
                    }
                    return true;
                }
            } else if (ir.levels == null && this.levels == null) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int l = this.inline != null ? this.inline.hashCode() : 0;
        l = (l ^ this.minLevel) + (l << 19);
        l = (l ^ this.maxLevel) + (l << 11);
        return l;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RR: { type = \'");
        char c;
        String content = null;
        if (this.inline instanceof WordArea) {
            c = 'W';
            content = ((WordArea) this.inline).getWord();
        } else if (this.inline instanceof SpaceArea) {
            c = 'S';
            content = ((SpaceArea) this.inline).getSpace();
        } else if (this.inline instanceof Anchor) {
            c = 'A';
        } else if (this.inline instanceof Leader) {
            c = 'L';
        } else if (this.inline instanceof Space) {
            c = 'S';
        } else if (this.inline instanceof UnresolvedPageNumber) {
            c = '#';
            content = ((UnresolvedPageNumber) this.inline).getText();
        } else if (this.inline instanceof InlineBlockParent) {
            c = 'B';
        } else if (this.inline instanceof InlineViewport) {
            c = 'V';
        } else if (this.inline instanceof InlineParent) {
            c = 'I';
        } else {
            c = '?';
        }
        sb.append(c);
        sb.append("\', levels = \'");
        sb.append(generateLevels(this.levels));
        sb.append("\', min = ");
        sb.append(this.minLevel);
        sb.append(", max = ");
        sb.append(this.maxLevel);
        sb.append(", reversals = ");
        sb.append(this.reversals);
        sb.append(", content = <");
        sb.append(CharUtilities.toNCRefs(content));
        sb.append("> }");
        return sb.toString();
    }

    private String generateLevels(final int[] levels) {
        final StringBuilder lb = new StringBuilder();
        int maxLevel = -1;
        final int numLevels = levels.length;
        for (int i = 0; i < numLevels; i++) {
            final int l = levels[i];
            if (l > maxLevel) {
                maxLevel = l;
            }
        }
        if (maxLevel < 0) {
            // leave level buffer empty
        } else if (maxLevel < 10) {
            // use string of decimal digits
            for (int i = 0; i < numLevels; i++) {
                lb.append((char) ('0' + levels[i]));
            }
        } else {
            // use comma separated list
            boolean first = true;
            for (int i = 0; i < numLevels; i++) {
                if (first) {
                    first = false;
                } else {
                    lb.append(',');
                }
                lb.append(levels[i]);
            }
        }
        return lb.toString();
    }

    private static int[] makeLevels(final int level, final int count) {
        final int[] levels = new int[count];
        Arrays.fill(levels, level);
        return levels;
    }
}
