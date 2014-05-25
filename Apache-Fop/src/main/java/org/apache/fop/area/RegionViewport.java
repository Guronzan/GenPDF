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

/* $Id: RegionViewport.java 1311120 2012-04-08 23:48:11Z gadams $ */

package org.apache.fop.area;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.fop.traits.WritingModeTraitsGetter;

/**
 * Region Viewport area. This object represents the region-viewport-area. It has
 * a region-reference-area as its child. These areas are described in the
 * fo:region-body description in the XSL Recommendation.
 */
public class RegionViewport extends Area implements Viewport {

    private static final long serialVersionUID = 505781815165102572L;

    // this rectangle is relative to the page
    private RegionReference regionReference;
    private Rectangle2D viewArea;
    private boolean clip = false;

    /**
     * Create a new region-viewport-area
     *
     * @param viewArea
     *            the view area of this viewport
     */
    public RegionViewport(final Rectangle2D viewArea) {
        this.viewArea = viewArea;
        addTrait(Trait.IS_VIEWPORT_AREA, Boolean.TRUE);
    }

    /**
     * Set the region-reference-area for this region viewport.
     *
     * @param reg
     *            the child region-reference-area inside this viewport
     */
    public void setRegionReference(final RegionReference reg) {
        this.regionReference = reg;
    }

    /**
     * Get the region-reference-area for this region viewport.
     *
     * @return the child region-reference-area inside this viewport
     */
    public RegionReference getRegionReference() {
        return this.regionReference;
    }

    /**
     * Set the clipping for this region viewport.
     *
     * @param c
     *            the clipping value
     */
    public void setClip(final boolean c) {
        this.clip = c;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasClip() {
        return this.clip;
    }

    /** {@inheritDoc} */
    @Override
    public Rectangle getClipRectangle() {
        if (this.clip) {
            return new Rectangle(getIPD(), getBPD());
        } else {
            return null;
        }
    }

    /**
     * Get the view area of this viewport.
     *
     * @return the viewport rectangle area
     */
    public Rectangle2D getViewArea() {
        return this.viewArea;
    }

    private void writeObject(final java.io.ObjectOutputStream out)
            throws IOException {
        out.writeFloat((float) this.viewArea.getX());
        out.writeFloat((float) this.viewArea.getY());
        out.writeFloat((float) this.viewArea.getWidth());
        out.writeFloat((float) this.viewArea.getHeight());
        out.writeBoolean(this.clip);
        out.writeObject(this.traits);
        out.writeObject(this.regionReference);
    }

    private void readObject(final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        this.viewArea = new Rectangle2D.Float(in.readFloat(), in.readFloat(),
                in.readFloat(), in.readFloat());
        this.clip = in.readBoolean();
        this.traits = (TreeMap) in.readObject();
        setRegionReference((RegionReference) in.readObject());
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() throws CloneNotSupportedException {
        final RegionViewport rv = (RegionViewport) super.clone();
        rv.regionReference = (RegionReference) this.regionReference.clone();
        rv.viewArea = (Rectangle2D) this.viewArea.clone();
        return rv;
    }

    /**
     * Sets the writing mode traits for the region reference of this region
     * viewport
     *
     * @param wmtg
     *            a WM traits getter
     */
    @Override
    public void setWritingModeTraits(final WritingModeTraitsGetter wmtg) {
        if (this.regionReference != null) {
            this.regionReference.setWritingModeTraits(wmtg);
        }
    }

}
