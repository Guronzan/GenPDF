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

/* $Id: DestinationData.java 1062901 2011-01-24 18:06:25Z adelmelle $ */

package org.apache.fop.area;

import java.util.List;

import org.apache.fop.fo.extensions.destination.Destination;

/**
 * An instance of this class is named destination from fox:destination
 */
public class DestinationData extends AbstractOffDocumentItem implements
        Resolvable {

    // ID Reference for this bookmark
    private final String idRef;

    // String Array to satisfy getIDRefs method
    private final String[] idRefs;

    // PageViewport that the idRef item refers to
    private PageViewport pageRef = null;

    /**
     * Create a new pdf destination data object. This is used by the destination
     * to create a data object with a idref. During processing, this idref will
     * be subsequently resolved to a particular PageViewport.
     *
     * @param destination
     *            the fo:bookmark object
     */
    public DestinationData(final Destination destination) {
        this(destination.getInternalDestination());
    }

    /**
     * Create a new named destination.
     * 
     * @param idRef
     *            the id reference of the destination
     */
    public DestinationData(final String idRef) {
        this.idRef = idRef;
        this.idRefs = new String[] { idRef };
    }

    /**
     * Get the idref for this destination
     *
     * @return the idref for the destination
     */
    public String getIDRef() {
        return this.idRef;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getIDRefs() {
        return this.idRefs;
    }

    /**
     * Get the PageViewport object that this destination refers to
     *
     * @return the PageViewport that this destination points to
     */
    public PageViewport getPageViewport() {
        return this.pageRef;
    }

    /**
     * Check if this resolvable object has been resolved. For now, just return
     * true. To do: Find a way to determine whether the destination has been
     * resolved.
     *
     * @return true if this object has been resolved
     */
    @Override
    public boolean isResolved() {
        return true;
    }

    /**
     * Resolves the idref of this object by getting the PageViewport object that
     * corresponds to the IDRef
     *
     * {@inheritDoc} TODO check to make sure it works if multiple bookmark-items
     * have the same idref
     */
    @Override
    public void resolveIDRef(final String id, final List<PageViewport> pages) {
        this.pageRef = pages.get(0);
        // TODO get rect area of id on page
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "Destination";
    }

}
