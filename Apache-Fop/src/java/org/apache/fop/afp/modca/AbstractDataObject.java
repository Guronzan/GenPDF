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

/* $Id: AbstractDataObject.java 1195952 2011-11-01 12:20:21Z phancock $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.afp.AFPObjectAreaInfo;
import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPResourceLevel;
import org.apache.fop.afp.Completable;
import org.apache.fop.afp.Factory;
import org.apache.fop.afp.Startable;

/**
 * Abstract base class used by the ImageObject and GraphicsObject which both
 * have define an ObjectEnvironmentGroup
 */
public abstract class AbstractDataObject extends AbstractNamedAFPObject
implements Startable, Completable {

    /** the object environment group */
    protected ObjectEnvironmentGroup objectEnvironmentGroup = null;

    /** the object factory */
    protected final Factory factory;

    /** the completion status of this object */
    private boolean complete;

    /** the starting status of this object */
    private boolean started;

    /**
     * Named constructor
     *
     * @param factory
     *            the object factory
     * @param name
     *            data object name
     */
    public AbstractDataObject(final Factory factory, final String name) {
        super(name);
        this.factory = factory;
    }

    /**
     * Sets the object view port (area position and size).
     *
     * @param dataObjectInfo
     *            the object area info
     */
    public void setViewport(final AFPDataObjectInfo dataObjectInfo) {
        final AFPObjectAreaInfo objectAreaInfo = dataObjectInfo
                .getObjectAreaInfo();

        // object area descriptor
        final int width = objectAreaInfo.getWidth();
        final int height = objectAreaInfo.getHeight();
        final int widthRes = objectAreaInfo.getWidthRes();
        final int heightRes = objectAreaInfo.getHeightRes();
        final ObjectAreaDescriptor objectAreaDescriptor = this.factory
                .createObjectAreaDescriptor(width, height, widthRes, heightRes);
        getObjectEnvironmentGroup().setObjectAreaDescriptor(
                objectAreaDescriptor);

        // object area position
        final AFPResourceInfo resourceInfo = dataObjectInfo.getResourceInfo();
        final AFPResourceLevel resourceLevel = resourceInfo.getLevel();
        ObjectAreaPosition objectAreaPosition = null;
        final int rotation = objectAreaInfo.getRotation();
        if (resourceLevel.isInline()) {
            final int x = objectAreaInfo.getX();
            final int y = objectAreaInfo.getY();
            objectAreaPosition = this.factory.createObjectAreaPosition(x, y,
                    rotation);
        } else {
            // positional values are specified in the oaOffset of the include
            // object
            objectAreaPosition = this.factory.createObjectAreaPosition(0, 0,
                    rotation);
        }
        objectAreaPosition
                .setReferenceCoordinateSystem(ObjectAreaPosition.REFCSYS_PAGE_SEGMENT_RELATIVE);
        getObjectEnvironmentGroup().setObjectAreaPosition(objectAreaPosition);
    }

    /**
     * Gets the ObjectEnvironmentGroup
     *
     * @return the object environment group
     */
    public ObjectEnvironmentGroup getObjectEnvironmentGroup() {
        if (this.objectEnvironmentGroup == null) {
            this.objectEnvironmentGroup = this.factory
                    .createObjectEnvironmentGroup();
        }
        return this.objectEnvironmentGroup;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeStart(final OutputStream os) throws IOException {
        setStarted(true);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final OutputStream os) throws IOException {
        writeTriplets(os);
        if (this.objectEnvironmentGroup != null) {
            this.objectEnvironmentGroup.writeToStream(os);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setStarted(final boolean started) {
        this.started = started;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isStarted() {
        return this.started;
    }

    /** {@inheritDoc} */
    @Override
    public void setComplete(final boolean complete) {
        this.complete = complete;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isComplete() {
        return this.complete;
    }
}
