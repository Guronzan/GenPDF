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

/* $Id: ResourceTracker.java 734420 2009-01-14 15:38:32Z maxberger $ */

package org.apache.xmlgraphics.ps.dsc;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.xmlgraphics.ps.PSGenerator;
import org.apache.xmlgraphics.ps.PSResource;
import org.apache.xmlgraphics.ps.dsc.events.DSCCommentDocumentNeededResources;
import org.apache.xmlgraphics.ps.dsc.events.DSCCommentDocumentSuppliedResources;
import org.apache.xmlgraphics.ps.dsc.events.DSCCommentPageResources;

/**
 * This class is used to track resources in a DSC-compliant PostScript file. The
 * distinction is made between supplied and needed resources. For the details of
 * this distinction, please see the DSC specification.
 */
public class ResourceTracker {

    private Set documentSuppliedResources;
    private Set documentNeededResources;
    private Set usedResources;
    private Set pageResources;

    // Map<PSResource, Integer>
    private Map resourceUsageCounts;

    /**
     * Returns the set of supplied resources.
     * 
     * @return the set of supplied resources
     */
    public Set getDocumentSuppliedResources() {
        if (this.documentSuppliedResources != null) {
            return Collections.unmodifiableSet(this.documentSuppliedResources);
        } else {
            return Collections.EMPTY_SET;
        }
    }

    /**
     * Returns the set of needed resources.
     * 
     * @return the set of needed resources
     */
    public Set getDocumentNeededResources() {
        if (this.documentNeededResources != null) {
            return Collections.unmodifiableSet(this.documentNeededResources);
        } else {
            return Collections.EMPTY_SET;
        }
    }

    /**
     * Notifies the resource tracker that a new page has been started and that
     * the page resource set can be cleared.
     */
    public void notifyStartNewPage() {
        if (this.pageResources != null) {
            this.pageResources.clear();
        }
    }

    /**
     * Registers a supplied resource. If the same resources is already in the
     * set of needed resources, it is removed there.
     * 
     * @param res
     *            the resource
     */
    public void registerSuppliedResource(final PSResource res) {
        if (this.documentSuppliedResources == null) {
            this.documentSuppliedResources = new java.util.HashSet();
        }
        this.documentSuppliedResources.add(res);
        if (this.documentNeededResources != null) {
            this.documentNeededResources.remove(res);
        }
    }

    /**
     * Registers a needed resource. If the same resources is already in the set
     * of supplied resources, it is ignored, i.e. it is assumed to be supplied.
     * 
     * @param res
     *            the resource
     */
    public void registerNeededResource(final PSResource res) {
        if (this.documentNeededResources == null) {
            this.documentNeededResources = new java.util.HashSet();
        }
        if (!this.documentSuppliedResources.contains(res)) {
            this.documentNeededResources.add(res);
        }
    }

    private void preparePageResources() {
        if (this.pageResources == null) {
            this.pageResources = new java.util.HashSet();
        }
    }

    private void prepareUsageCounts() {
        if (this.resourceUsageCounts == null) {
            this.resourceUsageCounts = new java.util.HashMap();
        }
    }

    /**
     * Notifies the resource tracker about the usage of a resource on the
     * current page.
     * 
     * @param res
     *            the resource being used
     */
    public void notifyResourceUsageOnPage(final PSResource res) {
        preparePageResources();
        this.pageResources.add(res);

        prepareUsageCounts();
        final Counter counter = (Counter) this.resourceUsageCounts.get(res);
        if (counter == null) {
            this.resourceUsageCounts.put(res, new Counter());
        } else {
            counter.inc();
        }
    }

    /**
     * Notifies the resource tracker about the usage of resources on the current
     * page.
     * 
     * @param resources
     *            the resources being used
     */
    public void notifyResourceUsageOnPage(final Collection resources) {
        preparePageResources();
        final Iterator iter = resources.iterator();
        while (iter.hasNext()) {
            final PSResource res = (PSResource) iter.next();
            notifyResourceUsageOnPage(res);
        }
    }

    /**
     * Indicates whether a particular resource is supplied, rather than needed.
     * 
     * @param res
     *            the resource
     * @return true if the resource is registered as being supplied.
     */
    public boolean isResourceSupplied(final PSResource res) {
        return this.documentSuppliedResources != null
                && this.documentSuppliedResources.contains(res);
    }

    /**
     * Writes a DSC comment for the accumulated used resources, either at page
     * level or at document level.
     * 
     * @param pageLevel
     *            true if the DSC comment for the page level should be
     *            generated, false for the document level (in the trailer)
     * @param gen
     *            the PSGenerator to write the DSC comments with
     * @exception IOException
     *                In case of an I/O problem
     */
    public void writeResources(final boolean pageLevel, final PSGenerator gen)
            throws IOException {
        if (pageLevel) {
            writePageResources(gen);
        } else {
            writeDocumentResources(gen);
        }
    }

    /**
     * Writes a DSC comment for the accumulated used resources on the current
     * page. Then it commits all those resources to the used resources on
     * document level.
     * 
     * @param gen
     *            the PSGenerator to write the DSC comments with
     * @exception IOException
     *                In case of an I/O problem
     */
    public void writePageResources(final PSGenerator gen) throws IOException {
        new DSCCommentPageResources(this.pageResources).generate(gen);
        if (this.usedResources == null) {
            this.usedResources = new java.util.HashSet();
        }
        this.usedResources.addAll(this.pageResources);
    }

    /**
     * Writes a DSC comment for the needed and supplied resourced for the
     * current DSC document.
     * 
     * @param gen
     *            the PSGenerator to write the DSC comments with
     * @exception IOException
     *                In case of an I/O problem
     */
    public void writeDocumentResources(final PSGenerator gen)
            throws IOException {
        if (this.usedResources != null) {
            final Iterator iter = this.usedResources.iterator();
            while (iter.hasNext()) {
                final PSResource res = (PSResource) iter.next();
                if (this.documentSuppliedResources == null
                        || !this.documentSuppliedResources.contains(res)) {
                    registerNeededResource(res);
                }
            }
        }
        new DSCCommentDocumentNeededResources(this.documentNeededResources)
                .generate(gen);
        new DSCCommentDocumentSuppliedResources(this.documentSuppliedResources)
                .generate(gen);
    }

    /**
     * This method declares that the given resource will be inlined and can
     * therefore be removed from resource tracking. This is useful when you
     * don't know beforehand if a resource will be used multiple times. If it's
     * only used once it's better to inline the resource to lower the maximum
     * memory needed inside the PostScript interpreter.
     * 
     * @param res
     *            the resource
     */
    public void declareInlined(final PSResource res) {
        if (this.documentNeededResources != null) {
            this.documentNeededResources.remove(res);
        }
        if (this.documentSuppliedResources != null) {
            this.documentSuppliedResources.remove(res);
        }
        if (this.pageResources != null) {
            this.pageResources.remove(res);
        }
        if (this.usedResources != null) {
            this.usedResources.remove(res);
        }
    }

    /**
     * Returns the number of times a resource has been used inside the current
     * DSC document.
     * 
     * @param res
     *            the resource
     * @return the number of times the resource has been used
     */
    public long getUsageCount(final PSResource res) {
        final Counter counter = (Counter) this.resourceUsageCounts.get(res);
        return counter != null ? counter.getCount() : 0;
    }

    private static class Counter {

        private long count = 1;

        public void inc() {
            this.count++;
        }

        public long getCount() {
            return this.count;
        }

        @Override
        public String toString() {
            return Long.toString(this.count);
        }
    }

}
