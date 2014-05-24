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

/* $Id: ResourceGroup.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;

import org.apache.fop.afp.Streamable;

/**
 * A Resource Group contains a set of overlays.
 */
public class ResourceGroup extends AbstractNamedAFPObject {

    /** Set of resource uri */
    private final Set/* <String> */resourceSet = new java.util.HashSet/*
                                                                       * <String>
                                                                       */();

    /**
     * Constructor for the ResourceGroup, this takes a name parameter which must
     * be 8 characters long.
     *
     * @param name
     *            the resource group name
     */
    public ResourceGroup(final String name) {
        super(name);
    }

    /**
     * Add this named object to this resource group
     *
     * @param namedObject
     *            a named object
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred.
     */
    public void addObject(final AbstractNamedAFPObject namedObject)
            throws IOException {
        this.resourceSet.add(namedObject);
    }

    /**
     * Returns the number of resources contained in this resource group
     *
     * @return the number of resources contained in this resource group
     */
    public int getResourceCount() {
        return this.resourceSet.size();
    }

    /**
     * Returns true if the resource exists within this resource group, false
     * otherwise.
     *
     * @param uri
     *            the uri of the resource
     * @return true if the resource exists within this resource group
     */
    public boolean resourceExists(final String uri) {
        return this.resourceSet.contains(uri);
    }

    /** {@inheritDoc} */
    @Override
    public void writeStart(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.BEGIN, Category.RESOURCE_GROUP);
        os.write(data);
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final OutputStream os) throws IOException {
        final Iterator it = this.resourceSet.iterator();
        while (it.hasNext()) {
            final Object object = it.next();
            if (object instanceof Streamable) {
                final Streamable streamableObject = (Streamable) object;
                streamableObject.writeToStream(os);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void writeEnd(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.END, Category.RESOURCE_GROUP);
        os.write(data);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.name + " " + this.resourceSet/* getResourceMap() */;
    }
}
