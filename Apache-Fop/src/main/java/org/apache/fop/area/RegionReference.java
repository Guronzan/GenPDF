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

/* $Id: RegionReference.java 1311120 2012-04-08 23:48:11Z gadams $ */

package org.apache.fop.area;

import java.util.ArrayList;
import java.util.List;

import org.apache.fop.fo.pagination.Region;

/**
 * This is a region reference area for a page regions. This area is the direct
 * child of a region-viewport-area. It is cloneable so the page master can make
 * copies from the original page and regions.
 */
public class RegionReference extends Area {

    private static final long serialVersionUID = -298980963268244238L;

    private final int regionClass;
    private final String regionName;
    private CTM ctm;

    // the list of block areas from the static flow
    private ArrayList<Area> blocks = new ArrayList<Area>();

    /** the parent {@link RegionViewport} for this object */
    protected RegionViewport regionViewport;

    /**
     * Create a new region reference area.
     *
     * @param regionFO
     *            the region.
     * @param parent
     *            the viewport for this region.
     */
    public RegionReference(final Region regionFO, final RegionViewport parent) {
        this(regionFO.getNameId(), regionFO.getRegionName(), parent);
    }

    /**
     * Create a new region reference area.
     *
     * @param regionClass
     *            the region class (as returned by Region.getNameId())
     * @param regionName
     *            the name of the region (as returned by Region.getRegionName())
     * @param parent
     *            the viewport for this region.
     */
    public RegionReference(final int regionClass, final String regionName,
            final RegionViewport parent) {
        this.regionClass = regionClass;
        this.regionName = regionName;
        addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
        this.regionViewport = parent;
    }

    /** {@inheritDoc} */
    @Override
    public void addChildArea(final Area child) {
        this.blocks.add(child);
    }

    /**
     * Set the Coordinate Transformation Matrix which transforms content
     * coordinates in this region reference area which are specified in terms of
     * "start" and "before" into coordinates in a system which is positioned in
     * "absolute" directions (with origin at lower left of the region reference
     * area.
     *
     * @param ctm
     *            the current transform to position this region
     */
    public void setCTM(final CTM ctm) {
        this.ctm = ctm;
    }

    /**
     * @return Returns the parent RegionViewport.
     */
    public RegionViewport getRegionViewport() {
        return this.regionViewport;
    }

    /**
     * Get the current transform of this region.
     *
     * @return ctm the current transform to position this region
     */
    public CTM getCTM() {
        return this.ctm;
    }

    /**
     * Get the block in this region.
     *
     * @return the list of blocks in this region
     */
    public List<Area> getBlocks() {
        return this.blocks;
    }

    /**
     * Get the region class of this region.
     *
     * @return the region class
     */
    public int getRegionClass() {
        return this.regionClass;
    }

    /** @return the region name */
    public String getRegionName() {
        return this.regionName;
    }

    /**
     * Add a block area to this region reference area.
     *
     * @param block
     *            the block area to add
     */
    public void addBlock(final Block block) {
        addChildArea(block);
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() throws CloneNotSupportedException {
        final RegionReference rr = (RegionReference) super.clone();
        rr.blocks = (ArrayList) this.blocks.clone();
        return rr;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" {regionName=").append(this.regionName);
        sb.append(", regionClass=").append(this.regionClass);
        sb.append(", ctm=").append(this.ctm);
        sb.append("}");
        return sb.toString();
    }
}
