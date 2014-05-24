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

/* $Id: WrapperLayoutManager.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.layoutmgr.inline;

import org.apache.fop.area.Block;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.fo.flow.Wrapper;
import org.apache.fop.layoutmgr.BlockLayoutManager;
import org.apache.fop.layoutmgr.BlockStackingLayoutManager;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.TraitSetter;

/**
 * This is the layout manager for the fo:wrapper formatting object.
 */
public class WrapperLayoutManager extends LeafNodeLayoutManager {

    /**
     * Creates a new LM for fo:wrapper.
     * 
     * @param node
     *            the fo:wrapper
     */
    public WrapperLayoutManager(final Wrapper node) {
        super(node);
    }

    /** {@inheritDoc} */
    @Override
    public InlineArea get(final LayoutContext context) {
        // Create a zero-width, zero-height dummy area so this node can
        // participate in the ID handling. Otherwise, addId() wouldn't
        // be called. The area must also be added to the tree, because
        // determination of the X,Y position is done in the renderer.
        final InlineArea area = new InlineArea();
        if (this.fobj.hasId()) {
            TraitSetter.setProducerID(area, this.fobj.getId());
        }
        return area;
    }

    /**
     * Add the area for this layout manager. This adds the dummy area to the
     * parent, *if* it has an id - otherwise it serves no purpose.
     *
     * @param posIter
     *            the position iterator
     * @param context
     *            the layout context for adding the area
     */
    @Override
    public void addAreas(final PositionIterator posIter,
            final LayoutContext context) {
        if (this.fobj.hasId()) {
            addId();
            if (this.parentLayoutManager instanceof BlockStackingLayoutManager
                    && !(this.parentLayoutManager instanceof BlockLayoutManager)) {
                final Block helperBlock = new Block();
                TraitSetter.setProducerID(helperBlock, this.fobj.getId());
                this.parentLayoutManager.addChildArea(helperBlock);
            } else {
                final InlineArea area = getEffectiveArea();
                this.parentLayoutManager.addChildArea(area);
            }
        }
        while (posIter.hasNext()) {
            posIter.next();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void addId() {
        getPSLM().addIDToPage(this.fobj.getId());
    }

}
