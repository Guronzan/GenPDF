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

/* $Id: AbstractResourceGroupContainer.java 1296418 2012-03-02 19:56:33Z gadams $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;

import org.apache.fop.afp.Completable;
import org.apache.fop.afp.Factory;

/**
 * An abstract container of resource objects
 */
public abstract class AbstractResourceGroupContainer extends AbstractPageObject {

    /** The container started state */
    protected boolean started = false;

    /** the resource group object */
    protected ResourceGroup resourceGroup = null;

    /**
     * Default constructor
     *
     * @param factory
     *            the object factory
     */
    public AbstractResourceGroupContainer(final Factory factory) {
        super(factory);
    }

    /**
     * Named constructor
     *
     * @param factory
     *            the object factory
     * @param name
     *            the name of this resource container
     */
    public AbstractResourceGroupContainer(final Factory factory,
            final String name) {
        super(factory, name);
    }

    /**
     * Construct a new page object for the specified name argument, the page
     * name should be an 8 character identifier.
     *
     * @param factory
     *            the object factory
     * @param name
     *            the name of the page.
     * @param width
     *            the width of the page.
     * @param height
     *            the height of the page.
     * @param rotation
     *            the rotation of the page.
     * @param widthRes
     *            the width resolution of the page.
     * @param heightRes
     *            the height resolution of the page.
     */
    public AbstractResourceGroupContainer(final Factory factory,
            final String name, final int width, final int height,
            final int rotation, final int widthRes, final int heightRes) {
        super(factory, name, width, height, rotation, widthRes, heightRes);
    }

    /**
     * Return the number of resources in this container
     *
     * @return the number of resources in this container
     */
    protected int getResourceCount() {
        if (this.resourceGroup != null) {
            return this.resourceGroup.getResourceCount();
        }
        return 0;
    }

    /**
     * Returns true if this resource group container contains resources
     *
     * @return true if this resource group container contains resources
     */
    protected boolean hasResources() {
        return this.resourceGroup != null
                && this.resourceGroup.getResourceCount() > 0;
    }

    /**
     * Returns the resource group in this resource group container
     *
     * @return the resource group in this resource group container
     */
    public ResourceGroup getResourceGroup() {
        if (this.resourceGroup == null) {
            this.resourceGroup = this.factory.createResourceGroup();
        }
        return this.resourceGroup;
    }

    // /** {@inheritDoc} */
    // protected void writeContent(OutputStream os) throws IOException {
    // if (resourceGroup != null) {
    // resourceGroup.writeToStream(os);
    // }
    // super.writeContent(os);
    // }

    /** {@inheritDoc} */
    @Override
    public void writeToStream(final OutputStream os) throws IOException {
        if (!this.started) {
            writeStart(os);
            this.started = true;
        }

        writeContent(os);

        if (this.complete) {
            writeEnd(os);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void writeObjects(
            final Collection/* <AbstractAFPObject> */objects,
            final OutputStream os) throws IOException {
        writeObjects(objects, os, false);
    }

    /**
     * Writes a collection of {@link AbstractAFPObject}s to the AFP Datastream.
     *
     * @param objects
     *            a list of AFPObjects
     * @param os
     *            The stream to write to
     * @param forceWrite
     *            true if writing should happen in any case
     * @throws java.io.IOException
     *             an I/O exception of some sort has occurred.
     */
    protected void writeObjects(
            final Collection/* <AbstractAFPObject> */objects,
            final OutputStream os, final boolean forceWrite) throws IOException {
        if (objects != null && objects.size() > 0) {
            final Iterator it = objects.iterator();
            while (it.hasNext()) {
                final AbstractAFPObject ao = (AbstractAFPObject) it.next();
                if (forceWrite || canWrite(ao)) {
                    ao.writeToStream(os);
                    it.remove();
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Returns true if this object can be written
     *
     * @param obj
     *            an AFP object
     * @return true if this object can be written
     */
    protected boolean canWrite(final AbstractAFPObject obj) {
        if (obj instanceof Completable) {
            return ((Completable) obj).isComplete();
        } else {
            return isComplete();
        }
    }
}
