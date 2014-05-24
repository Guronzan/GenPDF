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

/* $Id: BlockKnuthSequence.java 1036179 2010-11-17 19:45:27Z spepping $ */

package org.apache.fop.layoutmgr;

import java.util.List;

/**
 * Represents a list of block level Knuth elements.
 */
public class BlockKnuthSequence extends KnuthSequence {

    private static final long serialVersionUID = 1648962416582509095L;

    private boolean isClosed = false;

    /**
     * Creates a new and empty list.
     */
    public BlockKnuthSequence() {
        super();
    }

    /**
     * Creates a new list from an existing list.
     *
     * @param list
     *            The list from which to create the new list.
     */
    public BlockKnuthSequence(final List list) {
        super(list);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInlineSequence() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canAppendSequence(final KnuthSequence sequence) {
        return !sequence.isInlineSequence() && !this.isClosed;
    }

    /** {@inheritDoc} */
    @Override
    public boolean appendSequence(final KnuthSequence sequence) {
        // log.debug("Cannot append a sequence without a BreakElement");
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean appendSequence(final KnuthSequence sequence,
            final boolean keepTogether, final BreakElement breakElement) {
        if (!canAppendSequence(sequence)) {
            return false;
        }
        if (keepTogether) {
            breakElement.setPenaltyValue(KnuthElement.INFINITE);
            add(breakElement);
        } else if (!getLast().isGlue()) {
            breakElement.setPenaltyValue(0);
            add(breakElement);
        }
        addAll(sequence);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public KnuthSequence endSequence() {
        this.isClosed = true;
        return this;
    }

}
