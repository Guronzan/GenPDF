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

/* $Id: RenderedImageCachableRed.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.image.rendered;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;

// CSOFF: NeedBraces
// CSOFF: WhitespaceAround

/**
 * This implements CachableRed around a RenderedImage. You can use this to wrap
 * a RenderedImage that you want to appear as a CachableRed. It essentially
 * ignores the dependency and dirty region methods.
 *
 * @version $Id: RenderedImageCachableRed.java 1345683 2012-06-03 14:50:33Z
 *          gadams $
 *
 *          Originally authored by Thomas DeWeese.
 */
public class RenderedImageCachableRed implements CachableRed {

    public static CachableRed wrap(final RenderedImage ri) {
        if (ri instanceof CachableRed) {
            return (CachableRed) ri;
        }
        if (ri instanceof BufferedImage) {
            return new BufferedImageCachableRed((BufferedImage) ri);
        }
        return new RenderedImageCachableRed(ri);
    }

    private final RenderedImage src;
    private final Vector<RenderedImage> srcs = new java.util.Vector(0);

    public RenderedImageCachableRed(final RenderedImage src) {
        if (src == null) {
            throw new NullPointerException();
        }
        this.src = src;
    }

    @Override
    public Vector<RenderedImage> getSources() {
        return this.srcs; // should always be empty...
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(getMinX(), // could we cache the rectangle??
                getMinY(), getWidth(), getHeight());
    }

    @Override
    public int getMinX() {
        return this.src.getMinX();
    }

    @Override
    public int getMinY() {
        return this.src.getMinY();
    }

    @Override
    public int getWidth() {
        return this.src.getWidth();
    }

    @Override
    public int getHeight() {
        return this.src.getHeight();
    }

    @Override
    public ColorModel getColorModel() {
        return this.src.getColorModel();
    }

    @Override
    public SampleModel getSampleModel() {
        return this.src.getSampleModel();
    }

    @Override
    public int getMinTileX() {
        return this.src.getMinTileX();
    }

    @Override
    public int getMinTileY() {
        return this.src.getMinTileY();
    }

    @Override
    public int getNumXTiles() {
        return this.src.getNumXTiles();
    }

    @Override
    public int getNumYTiles() {
        return this.src.getNumYTiles();
    }

    @Override
    public int getTileGridXOffset() {
        return this.src.getTileGridXOffset();
    }

    @Override
    public int getTileGridYOffset() {
        return this.src.getTileGridYOffset();
    }

    @Override
    public int getTileWidth() {
        return this.src.getTileWidth();
    }

    @Override
    public int getTileHeight() {
        return this.src.getTileHeight();
    }

    @Override
    public Object getProperty(final String name) {
        return this.src.getProperty(name);
    }

    @Override
    public String[] getPropertyNames() {
        return this.src.getPropertyNames();
    }

    @Override
    public Raster getTile(final int tileX, final int tileY) {
        return this.src.getTile(tileX, tileY);
    }

    @Override
    public WritableRaster copyData(final WritableRaster raster) {
        return this.src.copyData(raster);
    }

    @Override
    public Raster getData() {
        return this.src.getData();
    }

    @Override
    public Raster getData(final Rectangle rect) {
        return this.src.getData(rect);
    }

    @Override
    public Shape getDependencyRegion(final int srcIndex,
            final Rectangle outputRgn) {
        throw new IndexOutOfBoundsException("Nonexistant source requested.");
    }

    @Override
    public Shape getDirtyRegion(final int srcIndex, final Rectangle inputRgn) {
        throw new IndexOutOfBoundsException("Nonexistant source requested.");
    }
}
