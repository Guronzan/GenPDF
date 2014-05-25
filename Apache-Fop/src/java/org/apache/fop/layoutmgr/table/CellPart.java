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

/* $Id: CellPart.java 990144 2010-08-27 13:23:11Z vhennebert $ */

package org.apache.fop.layoutmgr.table;

import org.apache.fop.fo.flow.table.PrimaryGridUnit;

/**
 * Represents a non-divisible part of a grid unit. Used by the table stepper.
 */
class CellPart {

    /** Primary grid unit */
    protected PrimaryGridUnit pgu;
    /** Index of the starting element of this part */
    protected int start;
    /** Index of the ending element of this part */
    protected int end;

    private final int condBeforeContentLength;
    private final int length;
    private final int condAfterContentLength;
    private final int bpBeforeNormal;
    private final int bpBeforeFirst;
    private final int bpAfterNormal;
    private final int bpAfterLast;
    private final boolean isLast;

    /**
     * Creates a new CellPart.
     *
     * @param pgu
     *            Primary grid unit
     * @param start
     *            starting element
     * @param end
     *            ending element
     * @param last
     *            true if this cell part is the last one for the cell
     * @param condBeforeContentLength
     *            length of the additional content that will have to be
     *            displayed if this part will be the first one on the page
     * @param length
     *            length of the content represented by this cell part
     * @param condAfterContentLength
     *            length of the additional content that will have to be
     *            displayed if this part will be the last one on the page
     * @param bpBeforeNormal
     *            width of border- and padding-before in the normal case
     * @param bpBeforeFirst
     *            width of (possibly optional) border- and padding-before if
     *            this part will be the first one on the page
     * @param bpAfterNormal
     *            width of border- and padding-after in the normal case
     * @param bpAfterLast
     *            width of (possibly optional) border- and padding-after if this
     *            part will be the last one on the page
     */
    protected CellPart(
            // CSOK: ParameterNumber
            final PrimaryGridUnit pgu, final int start, final int end,
            final boolean last, final int condBeforeContentLength,
            final int length, final int condAfterContentLength,
            final int bpBeforeNormal, final int bpBeforeFirst,
            final int bpAfterNormal, final int bpAfterLast) {
        this.pgu = pgu;
        this.start = start;
        this.end = end;
        this.isLast = last;
        this.condBeforeContentLength = condBeforeContentLength;
        this.length = length;
        this.condAfterContentLength = condAfterContentLength;
        this.bpBeforeNormal = bpBeforeNormal;
        this.bpBeforeFirst = bpBeforeFirst;
        this.bpAfterNormal = bpAfterNormal;
        this.bpAfterLast = bpAfterLast;
    }

    /** @return true if this part is the first part of a cell */
    public boolean isFirstPart() {
        return this.start == 0;
    }

    /** @return true if this part is the last part of a cell */
    boolean isLastPart() {
        return this.isLast;
    }

    int getBorderPaddingBefore(final boolean firstOnPage) {
        if (firstOnPage) {
            return this.bpBeforeFirst;
        } else {
            return this.bpBeforeNormal;
        }
    }

    int getBorderPaddingAfter(final boolean lastOnPage) {
        if (lastOnPage) {
            return this.bpAfterLast;
        } else {
            return this.bpAfterNormal;
        }
    }

    int getConditionalBeforeContentLength() {
        return this.condBeforeContentLength;
    }

    int getLength() {
        return this.length;
    }

    int getConditionalAfterContentLength() {
        return this.condAfterContentLength;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Part: ");
        sb.append(this.start).append("-").append(this.end);
        sb.append(" [").append(isFirstPart() ? "F" : "-")
                .append(isLastPart() ? "L" : "-");
        sb.append("] ").append(this.pgu);
        return sb.toString();
    }

}
