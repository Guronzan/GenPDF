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

/* $Id: HyphContext.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.layoutmgr.inline;

/**
 * This class is used to pass information to the getNextBreakPoss() method
 * concerning hyphenation. A reference to an instance of the class is contained
 * in the LayoutContext object passed to each LayoutManager. It contains
 * information concerning the hyphenation points in a word and the how many of
 * those have previously been processed by a Layout Manager to generate size
 * information.
 */
public class HyphContext {
    private final int[] hyphPoints;
    private int currentOffset = 0;
    private int currentIndex = 0;

    /**
     * @param hyphPoints
     *            number of hyphenation points
     */
    public HyphContext(final int[] hyphPoints) {
        this.hyphPoints = hyphPoints;
    }

    /** @return next hyphenation point */
    public int getNextHyphPoint() {
        for (; this.currentIndex < this.hyphPoints.length; this.currentIndex++) {
            if (this.hyphPoints[this.currentIndex] > this.currentOffset) {
                return this.hyphPoints[this.currentIndex] - this.currentOffset;
            }
        }
        return -1; // AT END!
    }

    /** @return true if more hyphenation points */
    public boolean hasMoreHyphPoints() {
        for (; this.currentIndex < this.hyphPoints.length; this.currentIndex++) {
            if (this.hyphPoints[this.currentIndex] > this.currentOffset) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param iCharsProcessed
     *            amount to extend offset
     */
    public void updateOffset(final int iCharsProcessed) {
        this.currentOffset += iCharsProcessed;
    }
}
