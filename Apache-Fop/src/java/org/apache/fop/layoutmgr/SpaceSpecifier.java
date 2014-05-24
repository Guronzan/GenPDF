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

/* $Id: SpaceSpecifier.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.layoutmgr;

import java.util.ArrayList;
import java.util.List;

import org.apache.fop.traits.MinOptMax;
import org.apache.fop.traits.SpaceVal;

/**
 * Accumulate a sequence of space-specifiers (XSL space type) on areas with a
 * stacking constraint. Provide a way to resolve these into a single MinOptMax
 * value.
 */
public class SpaceSpecifier implements Cloneable {

    private boolean startsReferenceArea;
    private boolean hasForcing = false;
    private List spaceVals = new ArrayList();

    /**
     * Creates a new SpaceSpecifier.
     * 
     * @param startsReferenceArea
     *            true if it starts a new reference area
     */
    public SpaceSpecifier(final boolean startsReferenceArea) {
        this.startsReferenceArea = startsReferenceArea;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        try {
            final SpaceSpecifier ss = (SpaceSpecifier) super.clone();
            ss.startsReferenceArea = this.startsReferenceArea;
            ss.hasForcing = this.hasForcing;
            // Clone the vector, but share the objects in it!
            ss.spaceVals = new ArrayList();
            ss.spaceVals.addAll(this.spaceVals);
            return ss;
        } catch (final CloneNotSupportedException cnse) {
            return null;
        }

    }

    /**
     * Clear all space specifiers
     */
    public void clear() {
        this.hasForcing = false;
        this.spaceVals.clear();
    }

    /**
     * Indicates whether any space-specifiers have been added.
     * 
     * @return true if any space-specifiers have been added.
     */
    public boolean hasSpaces() {
        return !this.spaceVals.isEmpty();
    }

    /**
     * Add a new space to the sequence. If this sequence starts a reference
     * area, and the added space is conditional, and there are no
     * non-conditional values in the sequence yet, then ignore it. Otherwise add
     * it to the sequence.
     *
     * @param space
     *            the space to add.
     */
    public void addSpace(final SpaceVal space) {
        if (!this.startsReferenceArea || !space.isConditional() || hasSpaces()) {
            if (space.isForcing()) {
                if (!this.hasForcing) {
                    // Remove all other values (must all be non-forcing)
                    this.spaceVals.clear();
                    this.hasForcing = true;
                }
                this.spaceVals.add(space);
            } else if (!this.hasForcing) {
                // Don't bother adding all 0 space-specifier if not forcing
                if (space.getSpace().isNonZero()) {
                    this.spaceVals.add(space);
                }
            }
        }
    }

    /**
     * Resolve the current sequence of space-specifiers, accounting for forcing
     * values.
     *
     * @param endsReferenceArea
     *            whether the sequence should be resolved at the trailing edge
     *            of reference area.
     * @return the resolved value as a min/opt/max triple.
     */
    public MinOptMax resolve(final boolean endsReferenceArea) {
        int lastIndex = this.spaceVals.size();
        if (endsReferenceArea) {
            // Start from the end and count conditional specifiers
            // Stop at first non-conditional
            for (; lastIndex > 0; --lastIndex) {
                final SpaceVal spaceVal = (SpaceVal) this.spaceVals
                        .get(lastIndex - 1);
                if (!spaceVal.isConditional()) {
                    break;
                }
            }
        }
        MinOptMax resolvedSpace = MinOptMax.ZERO;
        int maxPrecedence = -1;
        for (int index = 0; index < lastIndex; index++) {
            final SpaceVal spaceVal = (SpaceVal) this.spaceVals.get(index);
            final MinOptMax space = spaceVal.getSpace();
            if (this.hasForcing) {
                resolvedSpace = resolvedSpace.plus(space);
            } else {
                final int precedence = spaceVal.getPrecedence();
                if (precedence > maxPrecedence) {
                    maxPrecedence = precedence;
                    resolvedSpace = space;
                } else if (precedence == maxPrecedence) {
                    if (space.getOpt() > resolvedSpace.getOpt()) {
                        resolvedSpace = space;
                    } else if (space.getOpt() == resolvedSpace.getOpt()) {
                        if (resolvedSpace.getMin() < space.getMin()) {
                            resolvedSpace = MinOptMax.getInstance(
                                    space.getMin(), resolvedSpace.getOpt(),
                                    resolvedSpace.getMax());
                        }
                        if (resolvedSpace.getMax() > space.getMax()) {
                            resolvedSpace = MinOptMax.getInstance(
                                    resolvedSpace.getMin(),
                                    resolvedSpace.getOpt(), space.getMax());
                        }
                    }
                }
            }

        }
        return resolvedSpace;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Space Specifier (resolved at begin/end of ref. area:):\n"
                + resolve(false) + "\n" + resolve(true);
    }
}
