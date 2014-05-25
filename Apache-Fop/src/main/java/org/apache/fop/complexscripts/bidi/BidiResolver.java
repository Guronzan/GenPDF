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
import java.util.List;
import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.LineArea;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.fo.pagination.PageSequence;

// CSOFF: EmptyForIteratorPadCheck
// CSOFF: InnerAssignmentCheck
// CSOFF: LineLengthCheck
// CSOFF: NoWhitespaceAfterCheck
// CSOFF: SimplifyBooleanReturnCheck

/**
 * <p>
 * A utility class for performing bidirectional resolution processing.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
@Slf4j
public final class BidiResolver {

    private BidiResolver() {
    }

    /**
     * Resolve inline directionality.
     *
     * @param ps
     *            a page sequence FO instance
     */
    public static void resolveInlineDirectionality(final PageSequence ps) {
        if (log.isDebugEnabled()) {
            log.debug("BD: RESOLVE: " + ps);
        }
        final List<DelimitedTextRange> ranges = pruneEmptyRanges(ps
                .collectDelimitedTextRanges(new Stack<Object>()));
        resolveInlineDirectionality(ranges);
    }

    /**
     * Reorder line area.
     *
     * @param la
     *            a line area instance
     */
    public static void reorder(final LineArea la) {

        // 1. collect inline levels
        List<InlineRun> runs = collectRuns(la.getInlineAreas(),
                new ArrayList<InlineRun>());
        if (log.isDebugEnabled()) {
            dumpRuns("BD: REORDER: INPUT:", runs);
        }

        // 2. split heterogeneous inlines
        runs = splitRuns(runs);
        if (log.isDebugEnabled()) {
            dumpRuns("BD: REORDER: SPLIT INLINES:", runs);
        }

        // 3. determine minimum and maximum levels
        final int[] mm = computeMinMaxLevel(runs, null);
        if (log.isDebugEnabled()) {
            log.debug("BD: REORDER: { min = " + mm[0] + ", max = " + mm[1]
                    + "}");
        }

        // 4. reorder from maximum level to minimum odd level
        final int mn = mm[0];
        final int mx = mm[1];
        if (mx > 0) {
            for (int l1 = mx, l2 = (mn & 1) == 0 ? mn + 1 : mn; l1 >= l2; l1--) {
                runs = reorderRuns(runs, l1);
            }
        }
        if (log.isDebugEnabled()) {
            dumpRuns("BD: REORDER: REORDERED RUNS:", runs);
        }

        // 5. reverse word consituents (characters and glyphs) while mirroring
        final boolean mirror = true;
        reverseWords(runs, mirror);
        if (log.isDebugEnabled()) {
            dumpRuns("BD: REORDER: REORDERED WORDS:", runs);
        }

        // 6. replace line area's inline areas with reordered runs' inline areas
        replaceInlines(la, replicateSplitWords(runs));
    }

    private static void resolveInlineDirectionality(
            final List<DelimitedTextRange> ranges) {
        for (final DelimitedTextRange delimitedTextRange : ranges) {
            final DelimitedTextRange r = delimitedTextRange;
            r.resolve();
            if (log.isDebugEnabled()) {
                log.debug(r.toString());
            }
        }
    }

    private static List<InlineRun> collectRuns(final List<InlineArea> inlines,
            List<InlineRun> runs) {
        for (final InlineArea ia : inlines) {
            runs = ia.collectInlineRuns(runs);
        }
        return runs;
    }

    private static List<InlineRun> splitRuns(List<InlineRun> runs) {
        final List<InlineRun> runsNew = new ArrayList<InlineRun>();
        for (final InlineRun inlineRun : runs) {
            final InlineRun ir = inlineRun;
            if (ir.isHomogenous()) {
                runsNew.add(ir);
            } else {
                runsNew.addAll(ir.split());
            }
        }
        if (!runsNew.equals(runs)) {
            runs = runsNew;
        }
        return runs;
    }

    private static int[] computeMinMaxLevel(final List<InlineRun> runs, int[] mm) {
        if (mm == null) {
            mm = new int[] { Integer.MAX_VALUE, Integer.MIN_VALUE };
        }
        for (final InlineRun inlineRun : runs) {
            final InlineRun ir = inlineRun;
            ir.updateMinMax(mm);
        }
        return mm;
    }

    private static List<InlineRun> reorderRuns(List<InlineRun> runs,
            final int level) {
        assert level >= 0;
        final List<InlineRun> runsNew = new ArrayList<InlineRun>();
        for (int i = 0, n = runs.size(); i < n; i++) {
            final InlineRun iri = runs.get(i);
            if (iri.getMinLevel() < level) {
                runsNew.add(iri);
            } else {
                final int s = i;
                int e = s;
                while (e < n) {
                    final InlineRun ire = runs.get(e);
                    if (ire.getMinLevel() < level) {
                        break;
                    } else {
                        e++;
                    }
                }
                if (s < e) {
                    runsNew.addAll(reverseRuns(runs, s, e));
                }
                i = e - 1;
            }
        }
        if (!runsNew.equals(runs)) {
            runs = runsNew;
        }
        return runs;
    }

    private static List<InlineRun> reverseRuns(final List<InlineRun> runs,
            final int s, final int e) {
        final int n = e - s;
        final List<InlineRun> runsNew = new ArrayList<InlineRun>(n);
        if (n > 0) {
            for (int i = 0; i < n; i++) {
                final int k = n - i - 1;
                final InlineRun ir = runs.get(s + k);
                ir.reverse();
                runsNew.add(ir);
            }
        }
        return runsNew;
    }

    private static void reverseWords(final List<InlineRun> runs,
            final boolean mirror) {
        for (final InlineRun inlineRun : runs) {
            final InlineRun ir = inlineRun;
            ir.maybeReverseWord(mirror);
        }
    }

    private static List<InlineRun> replicateSplitWords(
            final List<InlineRun> runs) {
        // [TBD] for each run which inline word area appears multiple times in
        // runs, replicate that word
        return runs;
    }

    private static void replaceInlines(final LineArea la,
            final List<InlineRun> runs) {
        final List<InlineArea> inlines = new ArrayList<InlineArea>();
        for (final InlineRun inlineRun : runs) {
            final InlineRun ir = inlineRun;
            inlines.add(ir.getInline());
        }
        la.setInlineAreas(unflattenInlines(inlines));
    }

    private static List<?> unflattenInlines(final List<InlineArea> inlines) {
        return new UnflattenProcessor(inlines).unflatten();
    }

    private static void dumpRuns(final String header, final List<InlineRun> runs) {
        log.debug(header);
        for (final InlineRun inlineRun : runs) {
            final InlineRun ir = inlineRun;
            log.debug(ir.toString());
        }
    }

    private static List<DelimitedTextRange> pruneEmptyRanges(
            final Stack<?> ranges) {
        final List<DelimitedTextRange> rv = new ArrayList<DelimitedTextRange>();
        for (final Object name : ranges) {
            final DelimitedTextRange r = (DelimitedTextRange) name;
            if (!r.isEmpty()) {
                rv.add(r);
            }
        }
        return rv;
    }

}
