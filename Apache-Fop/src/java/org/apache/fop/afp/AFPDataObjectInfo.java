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

/* $Id: AFPDataObjectInfo.java 985571 2010-08-14 19:28:26Z jeremias $ */

package org.apache.fop.afp;

import org.apache.fop.afp.modca.Registry;
import org.apache.fop.afp.modca.triplets.MappingOptionTriplet;

/**
 * A list of parameters associated with an AFP data objects
 */
public class AFPDataObjectInfo {

    /** the object area info */
    private AFPObjectAreaInfo objectAreaInfo;

    /** resource info */
    private AFPResourceInfo resourceInfo;

    /** the data object width */
    private int dataWidth;

    /** the data object height */
    private int dataHeight;

    /** the object registry mimetype */
    private String mimeType;

    /** the object data in a byte array */
    private byte[] data;

    /** the object data height resolution */
    private int dataHeightRes;

    /** the object data width resolution */
    private int dataWidthRes;

    /** controls whether to create a page segment or a simple object */
    private boolean createPageSegment;

    /** controls the mapping of the image data into the image area */
    private byte mappingOption = MappingOptionTriplet.SCALE_TO_FILL;

    /**
     * Default constructor
     */
    public AFPDataObjectInfo() {
    }

    /**
     * Sets the image mime type
     *
     * @param mimeType
     *            the image mime type
     */
    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Returns the mime type of this data object
     *
     * @return the mime type of this data object
     */
    public String getMimeType() {
        return this.mimeType;
    }

    /**
     * Convenience method to return the object type
     *
     * @return the object type
     */
    public Registry.ObjectType getObjectType() {
        return Registry.getInstance().getObjectType(getMimeType());
    }

    /**
     * Returns the resource level at which this data object should reside
     *
     * @return the resource level at which this data object should reside
     */
    public AFPResourceInfo getResourceInfo() {
        if (this.resourceInfo == null) {
            this.resourceInfo = new AFPResourceInfo();
        }
        return this.resourceInfo;
    }

    /**
     * Sets the resource level at which this object should reside
     *
     * @param resourceInfo
     *            the resource level at which this data object should reside
     */
    public void setResourceInfo(final AFPResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    /**
     * Sets the object area info
     *
     * @param objectAreaInfo
     *            the object area info
     */
    public void setObjectAreaInfo(final AFPObjectAreaInfo objectAreaInfo) {
        this.objectAreaInfo = objectAreaInfo;
    }

    /**
     * Returns the object area info
     *
     * @return the object area info
     */
    public AFPObjectAreaInfo getObjectAreaInfo() {
        return this.objectAreaInfo;
    }

    /**
     * Returns the uri of this data object
     *
     * @return the uri of this data object
     */
    public String getUri() {
        return getResourceInfo().getUri();
    }

    /**
     * Sets the data object uri
     *
     * @param uri
     *            the data object uri
     */
    public void setUri(final String uri) {
        getResourceInfo().setUri(uri);
    }

    /**
     * Returns the image data width
     *
     * @return the image data width
     */
    public int getDataWidth() {
        return this.dataWidth;
    }

    /**
     * Sets the image data width
     *
     * @param imageDataWidth
     *            the image data width
     */
    public void setDataWidth(final int imageDataWidth) {
        this.dataWidth = imageDataWidth;
    }

    /**
     * Returns the image data height
     *
     * @return the image data height
     */
    public int getDataHeight() {
        return this.dataHeight;
    }

    /**
     * Sets the image data height
     *
     * @param imageDataHeight
     *            the image data height
     */
    public void setDataHeight(final int imageDataHeight) {
        this.dataHeight = imageDataHeight;
    }

    /**
     * Returns the data height resolution
     *
     * @return the data height resolution
     */
    public int getDataHeightRes() {
        return this.dataHeightRes;
    }

    /**
     * Sets the data height resolution
     *
     * @param dataHeightRes
     *            the data height resolution
     */
    public void setDataHeightRes(final int dataHeightRes) {
        this.dataHeightRes = dataHeightRes;
    }

    /**
     * Returns the data width resolution
     *
     * @return the data width resolution
     */
    public int getDataWidthRes() {
        return this.dataWidthRes;
    }

    /**
     * Sets the data width resolution
     *
     * @param dataWidthRes
     *            the data width resolution
     */
    public void setDataWidthRes(final int dataWidthRes) {
        this.dataWidthRes = dataWidthRes;
    }

    /**
     * Sets the object data
     *
     * @param data
     *            the object data
     */
    public void setData(final byte[] data) {
        this.data = data;
    }

    /**
     * Returns the object data
     *
     * @return the object data
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * Controls whether to create a page segment or a normal object.
     * 
     * @param value
     *            true for page segments, false for objects
     */
    public void setCreatePageSegment(final boolean value) {
        this.createPageSegment = value;
    }

    /**
     * Indicates whether a page segment or a normal object shall be created.
     * 
     * @return true for page segments, false for objects
     */
    public boolean isCreatePageSegment() {
        return this.createPageSegment;
    }

    /**
     * Sets the way an image is mapped into its target area.
     * 
     * @param mappingOption
     *            the mapping option (Valid values: see Mapping Option Triplet)
     */
    public void setMappingOption(final byte mappingOption) {
        this.mappingOption = mappingOption;
    }

    /**
     * Returns the way an image is mapped into its target area. By default, this
     * is "scale to fill" behavior.
     * 
     * @return the mapping option value from the Mapping Option Triplet
     */
    public byte getMappingOption() {
        return this.mappingOption;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AFPDataObjectInfo{"
                + "mimeType="
                + this.mimeType
                + ", dataWidth="
                + this.dataWidth
                + ", dataHeight="
                + this.dataHeight
                + ", dataWidthRes="
                + this.dataWidthRes
                + ", dataHeightRes="
                + this.dataHeightRes
                + (this.objectAreaInfo != null ? ", objectAreaInfo="
                        + this.objectAreaInfo : "")
                + (this.resourceInfo != null ? ", resourceInfo="
                        + this.resourceInfo : "");
    }

}
