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

/* $Id: KnuthElement.java 893238 2009-12-22 17:20:51Z vhennebert $ */

package org.apache.fop.layoutmgr;

/**
 * This is the super class for KnuthBox, KnuthGlue and KnuthPenalty.
 *
 * It stores information common to all sub classes, and the methods to get it:
 * the width, a Position and a boolean marking KnuthElements used for some
 * special feature (for example, the additional elements used to represent a
 * space when text alignment is right, left or center).
 */
public abstract class KnuthElement extends ListElement {

    /** The value used as an infinite indicator. */
    public static final int INFINITE = 1000;

    private final int width;
    private final boolean auxiliary;

    /**
     * Creates a new <code>KnuthElement</code>.
     *
     * @param width
     *            the width of this element
     * @param pos
     *            the Position stored in this element
     * @param auxiliary
     *            is this an auxiliary element?
     */
    protected KnuthElement(final int width, final Position pos,
            final boolean auxiliary) {
        super(pos);
        this.width = width;
        this.auxiliary = auxiliary;
    }

    /** @return true if this element is an auxiliary one. */
    public boolean isAuxiliary() {
        return this.auxiliary;
    }

    /** @return the width of this element. */
    public int getWidth() {
        return this.width;
    }

    /** @return the penalty value of this element, if applicable. */
    public int getPenalty() {
        throw new RuntimeException("Element is not a penalty");
    }

    /** @return the stretch value of this element, if applicable. */
    public int getStretch() {
        throw new RuntimeException("Element is not a glue");
    }

    /** @return the shrink value of this element, if applicable. */
    public int getShrink() {
        throw new RuntimeException("Element is not a glue");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUnresolvedElement() {
        return false;
    }

}