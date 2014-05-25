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

/* $Id: PositionIterator.java 1067353 2011-02-05 00:13:18Z adelmelle $ */

package org.apache.fop.layoutmgr;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over {@link Position} instances, that is wrapped around another
 * 'parent' {@link Iterator}. The parent can be either another
 * {@code PositionIterator}, or an iterator over {@link KnuthElement}s, for
 * example.<br/>
 * The {@link #next()} method always returns a {@link Position}. The
 * {@link #getPos(Object)} method can be overridden in subclasses to take care
 * of obtaining the {@link LayoutManager} or {@link Position} from the object
 * returned by the parent iterator's {@code next()} method.
 */
public class PositionIterator implements Iterator<Position> {

    private final Iterator parentIter;
    private Object nextObj;
    private LayoutManager childLM;
    private boolean hasNext;

    /**
     * Construct position iterator.
     * 
     * @param parentIter
     *            an iterator to use as parent
     */
    public PositionIterator(final Iterator parentIter) {
        this.parentIter = parentIter;
        lookAhead();
        // checkNext();
    }

    /** @return layout manager of next child layout manager or null */
    public LayoutManager getNextChildLM() {
        // Move to next "segment" of iterator, ie: new childLM
        if (this.childLM == null && this.nextObj != null) {
            this.childLM = getLM(this.nextObj);
            this.hasNext = true;
        }
        return this.childLM;
    }

    /**
     * @param nextObj
     *            next object from which to obtain position
     * @return layout manager
     */
    protected LayoutManager getLM(final Object nextObj) {
        return getPos(nextObj).getLM();
    }

    /**
     * Default implementation assumes that the passed {@code nextObj} is itself
     * a {@link Position}, and just returns it. Subclasses for which this is not
     * the case, <em>must</em> provide a suitable override this method.
     * 
     * @param nextObj
     *            next object from which to obtain position
     * @return position of next object.
     */
    protected Position getPos(final Object nextObj) {
        if (nextObj instanceof Position) {
            return (Position) nextObj;
        }
        throw new IllegalArgumentException(
                "Cannot obtain Position from the given object.");
    }

    private void lookAhead() {
        if (this.parentIter.hasNext()) {
            this.hasNext = true;
            this.nextObj = this.parentIter.next();
        } else {
            endIter();
        }
    }

    /** @return true if not at end of sub-sequence with same child layout manager */
    protected boolean checkNext() {
        final LayoutManager lm = getLM(this.nextObj);
        if (this.childLM == null) {
            this.childLM = lm;
        } else if (this.childLM != lm && lm != null) {
            // End of this sub-sequence with same child LM
            this.hasNext = false;
            this.childLM = null;
            return false;
        }
        return true;
    }

    /** end (reset) iterator */
    protected void endIter() {
        this.hasNext = false;
        this.nextObj = null;
        this.childLM = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        return this.hasNext && checkNext();
    }

    /** {@inheritDoc} */
    @Override
    public Position next() throws NoSuchElementException {
        if (this.hasNext) {
            final Position retPos = getPos(this.nextObj);
            lookAhead();
            return retPos;
        } else {
            throw new NoSuchElementException("PosIter");
        }
    }

    /** @return peek at next object */
    public Object peekNext() {
        return this.nextObj;
    }

    /** {@inheritDoc} */
    @Override
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "PositionIterator doesn't support remove");
    }
}
