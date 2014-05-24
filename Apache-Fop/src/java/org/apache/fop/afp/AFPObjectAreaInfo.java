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

/* $Id: AFPObjectAreaInfo.java 1195952 2011-11-01 12:20:21Z phancock $ */

package org.apache.fop.afp;

/**
 * A common class used to convey locations, dimensions and resolutions of data
 * objects.
 */
public class AFPObjectAreaInfo {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private int widthRes;
    private int heightRes;
    private final int rotation;

    /**
     * Constructor
     *
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param width
     *            the width
     * @param height
     *            the height
     * @param resolution
     *            the resolution (sets both width and height resolutions)
     * @param rotation
     *            the rotation angle
     */
    public AFPObjectAreaInfo(final int x, final int y, final int width,
            final int height, final int resolution, final int rotation) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.widthRes = resolution;
        this.heightRes = resolution;
    }

    /**
     * Sets both the width and the height resolutions.
     *
     * @param resolution
     *            the resolution
     */
    public void setResolution(final int resolution) {
        this.widthRes = resolution;
        this.heightRes = resolution;
    }

    /**
     * Sets the width resolution.
     *
     * @param resolution
     *            the resolution
     */
    public void setWidthRes(final int resolution) {
        this.widthRes = resolution;
    }

    /**
     * Sets the height resolution.
     *
     * @param resolution
     *            the resolution
     */
    public void setHeightRes(final int resolution) {
        this.heightRes = resolution;
    }

    /**
     * Returns the x coordinate of this data object
     *
     * @return the x coordinate of this data object
     */
    public int getX() {
        return this.x;
    }

    /**
     * Returns the y coordinate of this data object
     *
     * @return the y coordinate of this data object
     */
    public int getY() {
        return this.y;
    }

    /**
     * Returns the width of this data object
     *
     * @return the width of this data object
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Returns the height of this data object
     *
     * @return the height of this data object
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Returns the width resolution of this data object
     *
     * @return the resolution of this data object
     */
    public int getWidthRes() {
        return this.widthRes;
    }

    /**
     * Returns the height resolution of this data object
     *
     * @return the resolution of this data object
     */
    public int getHeightRes() {
        return this.heightRes;
    }

    /**
     * Returns the rotation of this data object
     *
     * @return the rotation of this data object
     */
    public int getRotation() {
        return this.rotation;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "x=" + this.x + ", y=" + this.y + ", width=" + this.width
                + ", height=" + this.height + ", widthRes=" + this.widthRes
                + ", heigtRes=" + this.heightRes + ", rotation="
                + this.rotation;
    }

}
