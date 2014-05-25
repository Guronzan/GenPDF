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

/* $Id: SimpleRenderedImage.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.image.codec.util;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

// CSOFF: WhitespaceAround

/**
 * A simple class implemented the <code>RenderedImage</code> interface. Only the
 * <code>getTile()</code> method needs to be implemented by subclasses. The
 * instance variables must also be filled in properly.
 *
 * <p>
 * Normally in JAI <code>PlanarImage</code> is used for this purpose, but in the
 * interest of modularity the use of <code>PlanarImage</code> has been avoided.
 *
 * @version $Id: SimpleRenderedImage.java 1345683 2012-06-03 14:50:33Z gadams $
 */
public abstract class SimpleRenderedImage implements RenderedImage {

    /** The X coordinate of the image's upper-left pixel. */
    protected int minX;

    /** The Y coordinate of the image's upper-left pixel. */
    protected int minY;

    /** The image's width in pixels. */
    protected int width;

    /** The image's height in pixels. */
    protected int height;

    /** The width of a tile. */
    protected int tileWidth;

    /** The height of a tile. */
    protected int tileHeight;

    /** The X coordinate of the upper-left pixel of tile (0, 0). */
    protected int tileGridXOffset = 0;

    /** The Y coordinate of the upper-left pixel of tile (0, 0). */
    protected int tileGridYOffset = 0;

    /** The image's SampleModel. */
    protected SampleModel sampleModel = null;

    /** The image's ColorModel. */
    protected ColorModel colorModel = null;

    /** The image's sources, stored in a List. */
    protected List sources = new ArrayList();

    /** A Hashtable containing the image properties. */
    protected Map properties = new HashMap();

    public SimpleRenderedImage() {
    }

    /** Returns the X coordinate of the leftmost column of the image. */
    @Override
    public int getMinX() {
        return this.minX;
    }

    /**
     * Returns the X coordinate of the column immediatetely to the right of the
     * rightmost column of the image. getMaxX() is implemented in terms of
     * getMinX() and getWidth() and so does not need to be implemented by
     * subclasses.
     */
    public final int getMaxX() {
        return getMinX() + getWidth();
    }

    /** Returns the X coordinate of the uppermost row of the image. */
    @Override
    public int getMinY() {
        return this.minY;
    }

    /**
     * Returns the Y coordinate of the row immediately below the bottom row of
     * the image. getMaxY() is implemented in terms of getMinY() and getHeight()
     * and so does not need to be implemented by subclasses.
     */
    public final int getMaxY() {
        return getMinY() + getHeight();
    }

    /** Returns the width of the image. */
    @Override
    public int getWidth() {
        return this.width;
    }

    /** Returns the height of the image. */
    @Override
    public int getHeight() {
        return this.height;
    }

    /** Returns a Rectangle indicating the image bounds. */
    public Rectangle getBounds() {
        return new Rectangle(getMinX(), getMinY(), getWidth(), getHeight());
    }

    /** Returns the width of a tile. */
    @Override
    public int getTileWidth() {
        return this.tileWidth;
    }

    /** Returns the height of a tile. */
    @Override
    public int getTileHeight() {
        return this.tileHeight;
    }

    /**
     * Returns the X coordinate of the upper-left pixel of tile (0, 0).
     */
    @Override
    public int getTileGridXOffset() {
        return this.tileGridXOffset;
    }

    /**
     * Returns the Y coordinate of the upper-left pixel of tile (0, 0).
     */
    @Override
    public int getTileGridYOffset() {
        return this.tileGridYOffset;
    }

    /**
     * Returns the horizontal index of the leftmost column of tiles.
     * getMinTileX() is implemented in terms of getMinX() and so does not need
     * to be implemented by subclasses.
     */
    @Override
    public int getMinTileX() {
        return convertXToTileX(getMinX());
    }

    /**
     * Returns the horizontal index of the rightmost column of tiles.
     * getMaxTileX() is implemented in terms of getMaxX() and so does not need
     * to be implemented by subclasses.
     */
    public int getMaxTileX() {
        return convertXToTileX(getMaxX() - 1);
    }

    /**
     * Returns the number of tiles along the tile grid in the horizontal
     * direction. getNumXTiles() is implemented in terms of getMinTileX() and
     * getMaxTileX() and so does not need to be implemented by subclasses.
     */
    @Override
    public int getNumXTiles() {
        return getMaxTileX() - getMinTileX() + 1;
    }

    /**
     * Returns the vertical index of the uppermost row of tiles. getMinTileY()
     * is implemented in terms of getMinY() and so does not need to be
     * implemented by subclasses.
     */
    @Override
    public int getMinTileY() {
        return convertYToTileY(getMinY());
    }

    /**
     * Returns the vertical index of the bottom row of tiles. getMaxTileY() is
     * implemented in terms of getMaxY() and so does not need to be implemented
     * by subclasses.
     */
    public int getMaxTileY() {
        return convertYToTileY(getMaxY() - 1);
    }

    /**
     * Returns the number of tiles along the tile grid in the vertical
     * direction. getNumYTiles() is implemented in terms of getMinTileY() and
     * getMaxTileY() and so does not need to be implemented by subclasses.
     */
    @Override
    public int getNumYTiles() {
        return getMaxTileY() - getMinTileY() + 1;
    }

    /** Returns the SampleModel of the image. */
    @Override
    public SampleModel getSampleModel() {
        return this.sampleModel;
    }

    /** Returns the ColorModel of the image. */
    @Override
    public ColorModel getColorModel() {
        return this.colorModel;
    }

    /**
     * Gets a property from the property set of this image. If the property name
     * is not recognized, <code>null</code> will be returned.
     *
     * @param name
     *            the name of the property to get, as a <code>String</code>.
     * @return a reference to the property <code>Object</code>, or the value
     *         <code>null</code>
     */
    @Override
    public Object getProperty(String name) {
        name = name.toLowerCase();
        return this.properties.get(name);
    }

    /**
     * Returns a list of the properties recognized by this image. If no
     * properties are available, an empty String[] will be returned.
     *
     * @return an array of <code>String</code>s representing valid property
     *         names.
     */
    @Override
    public String[] getPropertyNames() {
        final String[] names = new String[this.properties.size()];
        this.properties.keySet().toArray(names);
        return names;
    }

    /**
     * Returns an array of <code>String</code>s recognized as names by this
     * property source that begin with the supplied prefix. If no property names
     * match, <code>null</code> will be returned. The comparison is done in a
     * case-independent manner.
     *
     * <p>
     * The default implementation calls <code>getPropertyNames()</code> and
     * searches the list of names for matches.
     *
     * @return an array of <code>String</code>s giving the valid property names
     *         (can be null).
     */
    public String[] getPropertyNames(String prefix) {
        final String[] propertyNames = getPropertyNames();
        if (propertyNames == null) {
            return null;
        }

        prefix = prefix.toLowerCase();

        final List names = new ArrayList();
        for (final String propertyName : propertyNames) {
            if (propertyName.startsWith(prefix)) {
                names.add(propertyName);
            }
        }

        if (names.size() == 0) {
            return null;
        }

        // Copy the strings from the List over to a String array.
        final String[] prefixNames = new String[names.size()];
        names.toArray(prefixNames);
        return prefixNames;
    }

    // Utility methods.

    /**
     * Converts a pixel's X coordinate into a horizontal tile index relative to
     * a given tile grid layout specified by its X offset and tile width.
     */
    public static int convertXToTileX(int x, final int tileGridXOffset,
            final int tileWidth) {
        x -= tileGridXOffset;
        if (x < 0) {
            x += 1 - tileWidth; // Force round to -infinity
        }
        return x / tileWidth;
    }

    /**
     * Converts a pixel's Y coordinate into a vertical tile index relative to a
     * given tile grid layout specified by its Y offset and tile height.
     */
    public static int convertYToTileY(int y, final int tileGridYOffset,
            final int tileHeight) {
        y -= tileGridYOffset;
        if (y < 0) {
            y += 1 - tileHeight; // Force round to -infinity
        }
        return y / tileHeight;
    }

    /**
     * Converts a pixel's X coordinate into a horizontal tile index. This is a
     * convenience method. No attempt is made to detect out-of-range
     * coordinates.
     *
     * @param x
     *            the X coordinate of a pixel.
     * @return the X index of the tile containing the pixel.
     */
    public int convertXToTileX(final int x) {
        return convertXToTileX(x, getTileGridXOffset(), getTileWidth());
    }

    /**
     * Converts a pixel's Y coordinate into a vertical tile index. This is a
     * convenience method. No attempt is made to detect out-of-range
     * coordinates.
     *
     * @param y
     *            the Y coordinate of a pixel.
     * @return the Y index of the tile containing the pixel.
     */
    public int convertYToTileY(final int y) {
        return convertYToTileY(y, getTileGridYOffset(), getTileHeight());
    }

    /**
     * Converts a horizontal tile index into the X coordinate of its upper left
     * pixel relative to a given tile grid layout specified by its X offset and
     * tile width.
     */
    public static int tileXToX(final int tx, final int tileGridXOffset,
            final int tileWidth) {
        return tx * tileWidth + tileGridXOffset;
    }

    /**
     * Converts a vertical tile index into the Y coordinate of its upper left
     * pixel relative to a given tile grid layout specified by its Y offset and
     * tile height.
     */
    public static int tileYToY(final int ty, final int tileGridYOffset,
            final int tileHeight) {
        return ty * tileHeight + tileGridYOffset;
    }

    /**
     * Converts a horizontal tile index into the X coordinate of its upper left
     * pixel. This is a convenience method. No attempt is made to detect
     * out-of-range indices.
     *
     * @param tx
     *            the horizontal index of a tile.
     * @return the X coordinate of the tile's upper left pixel.
     */
    public int tileXToX(final int tx) {
        return tx * this.tileWidth + this.tileGridXOffset;
    }

    /**
     * Converts a vertical tile index into the Y coordinate of its upper left
     * pixel. This is a convenience method. No attempt is made to detect
     * out-of-range indices.
     *
     * @param ty
     *            the vertical index of a tile.
     * @return the Y coordinate of the tile's upper left pixel.
     */
    public int tileYToY(final int ty) {
        return ty * this.tileHeight + this.tileGridYOffset;
    }

    @Override
    public Vector<RenderedImage> getSources() {
        return null;
    }

    /**
     * Returns the entire image in a single Raster. For images with multiple
     * tiles this will require making a copy.
     *
     * <p>
     * The returned Raster is semantically a copy. This means that updates to
     * the source image will not be reflected in the returned Raster. For
     * non-writable (immutable) source images, the returned value may be a
     * reference to the image's internal data. The returned Raster should be
     * considered non-writable; any attempt to alter its pixel data (such as by
     * casting it to WritableRaster or obtaining and modifying its DataBuffer)
     * may result in undefined behavior. The copyData method should be used if
     * the returned Raster is to be modified.
     *
     * @return a Raster containing a copy of this image's data.
     */
    @Override
    public Raster getData() {
        final Rectangle rect = new Rectangle(getMinX(), getMinY(), getWidth(),
                getHeight());
        return getData(rect);
    }

    /**
     * Returns an arbitrary rectangular region of the RenderedImage in a Raster.
     * The rectangle of interest will be clipped against the image bounds.
     *
     * <p>
     * The returned Raster is semantically a copy. This means that updates to
     * the source image will not be reflected in the returned Raster. For
     * non-writable (immutable) source images, the returned value may be a
     * reference to the image's internal data. The returned Raster should be
     * considered non-writable; any attempt to alter its pixel data (such as by
     * casting it to WritableRaster or obtaining and modifying its DataBuffer)
     * may result in undefined behavior. The copyData method should be used if
     * the returned Raster is to be modified.
     *
     * @param bounds
     *            the region of the RenderedImage to be returned.
     */
    @Override
    public Raster getData(final Rectangle bounds) {
        final int startX = convertXToTileX(bounds.x);
        final int startY = convertYToTileY(bounds.y);
        final int endX = convertXToTileX(bounds.x + bounds.width - 1);
        final int endY = convertYToTileY(bounds.y + bounds.height - 1);
        Raster tile;

        if (startX == endX && startY == endY) {
            tile = getTile(startX, startY);
            return tile.createChild(bounds.x, bounds.y, bounds.width,
                    bounds.height, bounds.x, bounds.y, null);
        } else {
            // Create a WritableRaster of the desired size
            final SampleModel sm = this.sampleModel
                    .createCompatibleSampleModel(bounds.width, bounds.height);

            // Translate it
            final WritableRaster dest = Raster.createWritableRaster(sm,
                    bounds.getLocation());

            for (int j = startY; j <= endY; j++) {
                for (int i = startX; i <= endX; i++) {
                    tile = getTile(i, j);
                    final Rectangle intersectRect = bounds.intersection(tile
                            .getBounds());
                    final Raster liveRaster = tile.createChild(intersectRect.x,
                            intersectRect.y, intersectRect.width,
                            intersectRect.height, intersectRect.x,
                            intersectRect.y, null);
                    dest.setDataElements(0, 0, liveRaster);
                }
            }
            return dest;
        }
    }

    /**
     * Copies an arbitrary rectangular region of the RenderedImage into a
     * caller-supplied WritableRaster. The region to be computed is determined
     * by clipping the bounds of the supplied WritableRaster against the bounds
     * of the image. The supplied WritableRaster must have a SampleModel that is
     * compatible with that of the image.
     *
     * <p>
     * If the raster argument is null, the entire image will be copied into a
     * newly-created WritableRaster with a SampleModel that is compatible with
     * that of the image.
     *
     * @param dest
     *            a WritableRaster to hold the returned portion of the image.
     * @return a reference to the supplied WritableRaster, or to a new
     *         WritableRaster if the supplied one was null.
     */
    @Override
    public WritableRaster copyData(WritableRaster dest) {
        Rectangle bounds;
        Raster tile;

        if (dest == null) {
            bounds = getBounds();
            final Point p = new Point(this.minX, this.minY);
            /* A SampleModel to hold the entire image. */
            final SampleModel sm = this.sampleModel
                    .createCompatibleSampleModel(this.width, this.height);
            dest = Raster.createWritableRaster(sm, p);
        } else {
            bounds = dest.getBounds();
        }

        final int startX = convertXToTileX(bounds.x);
        final int startY = convertYToTileY(bounds.y);
        final int endX = convertXToTileX(bounds.x + bounds.width - 1);
        final int endY = convertYToTileY(bounds.y + bounds.height - 1);

        for (int j = startY; j <= endY; j++) {
            for (int i = startX; i <= endX; i++) {
                tile = getTile(i, j);
                final Rectangle intersectRect = bounds.intersection(tile
                        .getBounds());
                final Raster liveRaster = tile.createChild(intersectRect.x,
                        intersectRect.y, intersectRect.width,
                        intersectRect.height, intersectRect.x, intersectRect.y,
                        null);

                /*
                 * WritableRaster.setDataElements takes into account of
                 * inRaster's minX and minY and add these to x and y. Since
                 * liveRaster has the origin at the correct location, the
                 * following call should not again give these coordinates in
                 * places of x and y.
                 */
                dest.setDataElements(0, 0, liveRaster);
            }
        }
        return dest;
    }
}
