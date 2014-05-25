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

/* $Id: MainReference.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.area;

import java.util.ArrayList;
import java.util.List;

import org.apache.fop.traits.WritingModeTraitsGetter;

/**
 * The main-reference-area generated by an fo:region-body This object holds one
 * or more span-reference-areas (block-areas stacked in the block progression
 * direction) See fo:region-body definition in the XSL Rec for more information.
 */
public class MainReference extends Area {

    private static final long serialVersionUID = 7635126485620012448L;

    private final BodyRegion parent;
    private List<Span> spanAreas = new java.util.ArrayList<Span>();
    private boolean isEmpty = true;

    /**
     * Constructor
     *
     * @param parent
     *            the body region this reference area is placed in.
     */
    public MainReference(final BodyRegion parent) {
        this.parent = parent;
        addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
    }

    /**
     * Add a span area to this area.
     *
     * @param spanAll
     *            whether to make a single-column span
     * @return the created span area.
     */
    public Span createSpan(final boolean spanAll) {
        if (this.spanAreas.size() > 0 && getCurrentSpan().isEmpty()) {
            // Remove the current one if it is empty
            this.spanAreas.remove(this.spanAreas.size() - 1);
        }
        final RegionViewport rv = this.parent.getRegionViewport();
        final int ipdWidth = this.parent.getIPD()
                - rv.getBorderAndPaddingWidthStart()
                - rv.getBorderAndPaddingWidthEnd();

        final Span newSpan = new Span(spanAll ? 1 : getColumnCount(),
                getColumnGap(), ipdWidth);
        this.spanAreas.add(newSpan);
        return getCurrentSpan();
    }

    /**
     * Get the span areas from this area.
     *
     * @return the list of span areas
     */
    public List getSpans() {
        return this.spanAreas;
    }

    /**
     * Do not use. Used to handle special page-master for last page: transfer
     * the content that had already been added to a normal page to this main
     * reference for the last page. TODO this is hacky.
     *
     * @param spans
     *            content already laid out
     */
    public void setSpans(final List<Span> spans) {
        this.spanAreas = new ArrayList<Span>(spans);
    }

    /**
     * Get the span area currently being filled (i.e., the last span created).
     * 
     * @return the active span.
     */
    public Span getCurrentSpan() {
        return this.spanAreas.get(this.spanAreas.size() - 1);
    }

    /**
     * Indicates whether any child areas have been added to this reference area.
     *
     * This is achieved by looping through each span.
     * 
     * @return true if no child areas have been added yet.
     */
    public boolean isEmpty() {
        if (this.isEmpty && this.spanAreas != null) {
            for (final Span spanArea : this.spanAreas) {
                if (!spanArea.isEmpty()) {
                    this.isEmpty = false;
                    break;
                }
            }
        }
        return this.isEmpty;
    }

    /** @return the number of columns */
    public int getColumnCount() {
        return this.parent.getColumnCount();
    }

    /** @return the column gap in millipoints */
    public int getColumnGap() {
        return this.parent.getColumnGap();
    }

    /**
     * Sets the writing mode traits for the spans of this main reference area.
     * 
     * @param wmtg
     *            a WM traits getter
     */
    @Override
    public void setWritingModeTraits(final WritingModeTraitsGetter wmtg) {
        for (final Span s : (List<Span>) getSpans()) {
            s.setWritingModeTraits(wmtg);
        }
    }

}