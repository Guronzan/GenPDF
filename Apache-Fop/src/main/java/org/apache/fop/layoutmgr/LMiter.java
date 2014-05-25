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

/* $Id: LMiter.java 1067558 2011-02-06 00:56:28Z adelmelle $ */

package org.apache.fop.layoutmgr;

import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/** An iterator for layout managers. */
public class LMiter implements ListIterator<LayoutManager> {

    /** list of layout managers */
    protected List<LayoutManager> listLMs;
    /** current position in iteration */
    protected int curPos = 0;
    /** The LayoutManager to which this LMiter is attached **/
    private final LayoutManager lp;

    /**
     * Construct a layout manager iterator.
     * 
     * @param lp
     *            the associated layout manager (parent)
     */
    public LMiter(final LayoutManager lp) {
        this.lp = lp;
        this.listLMs = lp.getChildLMs();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        return this.curPos < this.listLMs.size()
                || this.lp.createNextChildLMs(this.curPos);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasPrevious() {
        return this.curPos > 0;
    }

    /** {@inheritDoc} */
    @Override
    public LayoutManager previous() throws NoSuchElementException {
        if (this.curPos > 0) {
            return this.listLMs.get(--this.curPos);
        } else {
            throw new NoSuchElementException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public LayoutManager next() throws NoSuchElementException {
        if (this.curPos < this.listLMs.size()) {
            return this.listLMs.get(this.curPos++);
        } else {
            throw new NoSuchElementException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void remove() throws NoSuchElementException {
        if (this.curPos > 0) {
            this.listLMs.remove(--this.curPos);
            // Note: doesn't actually remove it from the base!
        } else {
            throw new NoSuchElementException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void add(final LayoutManager lm)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException("LMiter doesn't support add");
    }

    /** {@inheritDoc} */
    @Override
    public void set(final LayoutManager lm)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException("LMiter doesn't support set");
    }

    /** {@inheritDoc} */
    @Override
    public int nextIndex() {
        return this.curPos;
    }

    /** {@inheritDoc} */
    @Override
    public int previousIndex() {
        return this.curPos - 1;
    }

}
