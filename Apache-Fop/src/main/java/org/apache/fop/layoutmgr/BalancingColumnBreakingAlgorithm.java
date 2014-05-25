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

/* $Id: BalancingColumnBreakingAlgorithm.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.layoutmgr;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.traits.MinOptMax;

/**
 * This is a the breaking algorithm that is responsible for balancing columns in
 * multi-column layout.
 */
@Slf4j
public class BalancingColumnBreakingAlgorithm extends PageBreakingAlgorithm {

    private final int columnCount;
    private int fullLen;
    private int idealPartLen;

    /**
     * Construct a balancing column breaking algorithm.
     *
     * @param topLevelLM
     *            the top level layout manager
     * @param pageProvider
     *            the page provider
     * @param layoutListener
     *            the layout listener
     * @param alignment
     *            alignment of the paragraph/page. One of
     *            {@link org.apache.fop.fo.Constants#EN_START},
     *            {@link org.apache.fop.fo.Constants#EN_JUSTIFY},
     *            {@link org.apache.fop.fo.Constants#EN_CENTER},
     *            {@link org.apache.fop.fo.Constants#EN_END}. For pages,
     *            {@link org.apache.fop.fo.Constants#EN_BEFORE} and
     *            {@link org.apache.fop.fo.Constants#EN_AFTER} are mapped to the
     *            corresponding inline properties,
     *            {@link org.apache.fop.fo.Constants#EN_START} and
     *            {@link org.apache.fop.fo.Constants#EN_END}.
     * @param alignmentLast
     *            alignment of the paragraph's last line
     * @param footnoteSeparatorLength
     *            length of footnote separator
     * @param partOverflowRecovery
     *            {@code true} if too long elements should be moved to the next
     *            line/part
     * @param columnCount
     *            number of columns
     * @see PageBreakingAlgorithm
     */
    public BalancingColumnBreakingAlgorithm(
            // CSOK: ParameterNumber
            final LayoutManager topLevelLM, final PageProvider pageProvider,
            final PageBreakingLayoutListener layoutListener,
            final int alignment, final int alignmentLast,
            final MinOptMax footnoteSeparatorLength,
            final boolean partOverflowRecovery, final int columnCount) {
        super(topLevelLM, pageProvider, layoutListener, alignment,
                alignmentLast, footnoteSeparatorLength, partOverflowRecovery,
                false, false);
        this.columnCount = columnCount;
        this.considerTooShort = true; // This is important!
    }

    /** {@inheritDoc} */
    @Override
    protected double computeDemerits(final KnuthNode activeNode,
            final KnuthElement element, final int fitnessClass, final double r) {
        double dem = super
                .computeDemerits(activeNode, element, fitnessClass, r);
        if (log.isTraceEnabled()) {
            log.trace("original demerit=" + dem + " " + this.totalWidth
                    + " line=" + activeNode.line + "/" + this.columnCount
                    + " pos=" + activeNode.position + "/"
                    + (this.par.size() - 1));
        }
        final int remParts = this.columnCount - activeNode.line;
        final int curPos = this.par.indexOf(element);
        if (this.fullLen == 0) {
            this.fullLen = ElementListUtils.calcContentLength(this.par,
                    activeNode.position, this.par.size() - 1);
            this.idealPartLen = this.fullLen / this.columnCount;
        }
        final int partLen = ElementListUtils.calcContentLength(this.par,
                activeNode.position, curPos - 1);
        final int restLen = ElementListUtils.calcContentLength(this.par,
                curPos - 1, this.par.size() - 1);
        int avgRestLen = 0;
        if (remParts > 0) {
            avgRestLen = restLen / remParts;
        }
        if (log.isTraceEnabled()) {
            log.trace("remaining parts: " + remParts + " rest len: " + restLen
                    + " avg=" + avgRestLen);
        }
        final double balance = (this.idealPartLen - partLen) / 1000f;
        if (log.isTraceEnabled()) {
            log.trace("balance=" + balance);
        }
        final double absBalance = Math.abs(balance);
        dem = absBalance;
        // Step 1: This does the rough balancing
        if (this.columnCount > 2) {
            if (balance > 0) {
                // shorter parts are less desired than longer ones
                dem = dem * 1.2f;
            }
        } else {
            if (balance < 0) {
                // shorter parts are less desired than longer ones
                dem = dem * 1.2f;
            }
        }
        // Step 2: This helps keep the trailing parts shorter than the previous
        // ones
        dem += avgRestLen / 1000f;

        if (activeNode.line >= this.columnCount) {
            // We don't want more columns than available
            dem = Double.MAX_VALUE;
        }
        if (log.isTraceEnabled()) {
            log.trace("effective dem=" + dem + " " + this.totalWidth);
        }
        return dem;
    }
}
