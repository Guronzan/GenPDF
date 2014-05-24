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

/* $Id: AFPResourceInfo.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.afp;

import java.awt.Dimension;

/**
 * The level at which a resource is to reside in the AFP output
 */
public class AFPResourceInfo {

    /** the general default resource level */
    public static final AFPResourceLevel DEFAULT_LEVEL = new AFPResourceLevel(
            AFPResourceLevel.PRINT_FILE);

    /** the URI of this resource */
    private String uri = null;

    /**
     * the image dimension in page coordinates (non-null only when page segments
     * are generated because the cannot be scaled for painting).
     */
    private Dimension imageDimension = null;

    /** the reference name of this resource */
    private String name = null;

    /** the resource level of this resource */
    private AFPResourceLevel level = DEFAULT_LEVEL;

    /** true when the resource level was changed */
    private boolean levelChanged = false;

    /**
     * Sets the data object URI.
     *
     * @param uri
     *            the data object URI
     */
    public void setUri(final String uri) {
        this.uri = uri;
    }

    /**
     * Returns the URI of this data object.
     *
     * @return the URI of this data object
     */
    public String getUri() {
        return this.uri;
    }

    /**
     * Sets an optional image dimension (in page coordinates). This is only used
     * if a page segment is created for this resource as page segments cannot be
     * rescaled for painting.
     * 
     * @param dim
     *            the image dimension (in page coordinates)
     */
    public void setImageDimension(final Dimension dim) {
        this.imageDimension = dim;
    }

    /**
     * Returns an optional image dimension (in page coordinates). This is only
     * used if a page segment is created for this resource as page segments
     * cannot be rescaled for painting.
     * 
     * @return the image dimension (or null if not applicable)
     */
    public Dimension getImageDimension() {
        return this.imageDimension;
    }

    /**
     * Sets the resource reference name
     *
     * @param resourceName
     *            the resource reference name
     */
    public void setName(final String resourceName) {
        this.name = resourceName;
    }

    /**
     * Returns the resource reference name
     *
     * @return the resource reference name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the resource level
     *
     * @return the resource level
     */
    public AFPResourceLevel getLevel() {
        if (this.level == null) {
            return DEFAULT_LEVEL;
        }
        return this.level;
    }

    /**
     * Sets the resource level
     *
     * @param resourceLevel
     *            the resource level
     */
    public void setLevel(final AFPResourceLevel resourceLevel) {
        this.level = resourceLevel;
        this.levelChanged = true;
    }

    /**
     * Returns true when the resource level was set
     *
     * @return true when the resource level was set
     */
    public boolean levelChanged() {
        return this.levelChanged;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AFPResourceInfo{uri="
                + this.uri
                + (this.imageDimension != null ? ", "
                        + this.imageDimension.width + "x"
                        + this.imageDimension.height : "")
                + (this.name != null ? ", name=" + this.name : "")
                + (this.level != null ? ", level=" + this.level : "") + "}";

    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof AFPResourceInfo)) {
            return false;
        }

        final AFPResourceInfo ri = (AFPResourceInfo) obj;
        return (this.uri == ri.uri || this.uri != null
                && this.uri.equals(ri.uri))
                && (this.imageDimension == ri.imageDimension || this.imageDimension != null
                        && this.imageDimension.equals(ri.imageDimension))
                && (this.name == ri.name || this.name != null
                        && this.name.equals(ri.name))
                && (this.level == ri.level || this.level != null
                        && this.level.equals(ri.level));
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (null == this.uri ? 0 : this.uri.hashCode());
        hash = 31
                * hash
                + (null == this.imageDimension ? 0 : this.imageDimension
                        .hashCode());
        hash = 31 * hash + (null == this.name ? 0 : this.name.hashCode());
        hash = 31 * hash + (null == this.level ? 0 : this.level.hashCode());
        return hash;
    }
}
