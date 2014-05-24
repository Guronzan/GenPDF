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

/* $Id: LeafPosition.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.layoutmgr;

/** A leaf position. */
public class LeafPosition extends Position {

    private final int leafPos;

    /**
     * Construct a leaf position.
     * 
     * @param layoutManager
     *            the associated layout manager
     * @param pos
     *            the leaf position
     */
    public LeafPosition(final LayoutManager layoutManager, final int pos) {
        super(layoutManager);
        this.leafPos = pos;
    }

    /**
     * Construct a leaf position.
     * 
     * @param layoutManager
     *            the associated layout manager
     * @param pos
     *            the leaf position
     * @param index
     *            the index
     */
    public LeafPosition(final LayoutManager layoutManager, final int pos,
            final int index) {
        super(layoutManager, index);
        this.leafPos = pos;
    }

    /** @return leaf position */
    public int getLeafPos() {
        return this.leafPos;
    }

    /** {@inheritDoc} */
    @Override
    public boolean generatesAreas() {
        return getLM() != null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("LeafPos:").append(getIndex()).append("(");
        sb.append("pos=").append(getLeafPos());
        sb.append(", lm=").append(getShortLMName()).append(")");
        return sb.toString();
    }
}
