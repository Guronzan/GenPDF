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

/* $Id: AbstractGraphicsCoord.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.afp.goca;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.afp.util.BinaryUtils;

/**
 * A base class encapsulating the structure of coordinate based GOCA objects
 */
public abstract class AbstractGraphicsCoord extends
        AbstractGraphicsDrawingOrder {

    /** array of x/y coordinates */
    protected int[] coords = null;

    /** if true, then uses relative drawing order */
    protected boolean relative = false;

    /**
     * Constructor
     *
     * @param coords
     *            the x/y coordinates for this object
     */
    public AbstractGraphicsCoord(final int[] coords) {
        if (coords == null) {
            this.relative = true;
        } else {
            this.coords = coords;
        }
    }

    /**
     * Constructor
     *
     * @param coords
     *            the x/y coordinates for this object
     * @param relative
     *            true if relative drawing order
     */
    public AbstractGraphicsCoord(final int[] coords, final boolean relative) {
        this(coords);
        this.relative = relative;
    }

    /**
     * Constructor
     *
     * @param x
     *            the x coordinate for this object
     * @param y
     *            the y coordinate for this object
     */
    public AbstractGraphicsCoord(final int x, final int y) {
        this(new int[] { x, y });
    }

    /**
     * Constructor
     *
     * @param x1
     *            the x1 coordinate for this object
     * @param y1
     *            the y1 coordinate for this object
     * @param x2
     *            the x2 coordinate for this object
     * @param y2
     *            the y2 coordinate for this object
     */
    public AbstractGraphicsCoord(final int x1, final int y1, final int x2,
            final int y2) {
        this(new int[] { x1, y1, x2, y2 });
    }

    /** {@inheritDoc} */
    @Override
    public int getDataLength() {
        return 2 + (this.coords != null ? this.coords.length * 2 : 0);
    }

    /**
     * Returns the coordinate data start index
     *
     * @return the coordinate data start index
     */
    int getCoordinateDataStartIndex() {
        return 2;
    }

    /**
     * Returns the coordinate data
     *
     * @return the coordinate data
     */
    @Override
    byte[] getData() {
        final byte[] data = super.getData();
        if (this.coords != null) {
            addCoords(data, getCoordinateDataStartIndex());
        }
        return data;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToStream(final OutputStream os) throws IOException {
        os.write(getData());
    }

    /**
     * Adds the coordinates to the structured field data
     *
     * @param data
     *            the structured field data
     * @param fromIndex
     *            the start index
     */
    protected void addCoords(final byte[] data, int fromIndex) {
        // X/Y POS
        for (int i = 0; i < this.coords.length; i++, fromIndex += 2) {
            final byte[] coord = BinaryUtils.convert(this.coords[i], 2);
            data[fromIndex] = coord[0];
            data[fromIndex + 1] = coord[1];
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String coordsStr = "";
        for (int i = 0; i < this.coords.length; i++) {
            coordsStr += i % 2 == 0 ? "x" : "y";
            coordsStr += i / 2 + "=" + this.coords[i] + ",";
        }
        coordsStr = coordsStr.substring(0, coordsStr.length() - 1);
        return getName() + "{" + coordsStr + "}";
    }

    /**
     * Returns true if this is a relative drawing order
     *
     * @return true if this is a relative drawing order
     */
    protected boolean isRelative() {
        return this.relative;
    }
}
