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

/* $Id: LinkResolver.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.area;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Link resolving for resolving internal links.
 */
public class LinkResolver implements Resolvable, Serializable {

    private static final long serialVersionUID = -7102134165192960718L;

    private boolean resolved = false;
    private final String idRef;
    private Area area;
    private transient List<Resolvable> dependents = null;

    /**
     * Create a new link resolver.
     *
     * @param id
     *            the id to resolve
     * @param a
     *            the area that will have the link attribute
     */
    public LinkResolver(final String id, final Area a) {
        this.idRef = id;
        this.area = a;
    }

    /**
     * @return true if this link is resolved
     */
    @Override
    public boolean isResolved() {
        return this.resolved;
    }

    /**
     * Get the references for this link.
     *
     * @return the String array of references.
     */
    @Override
    public String[] getIDRefs() {
        return new String[] { this.idRef };
    }

    /**
     * Resolve by adding an internal link to the first PageViewport in the list.
     *
     * {@inheritDoc}
     */
    @Override
    public void resolveIDRef(final String id, final List<PageViewport> pages) {
        resolveIDRef(id, pages.get(0));
    }

    /**
     * Resolve by adding an InternalLink trait to the area
     *
     * @param id
     *            the target id (should be equal to the object's idRef)
     * @param pv
     *            the PageViewport containing the first area with the given id
     */
    public void resolveIDRef(final String id, final PageViewport pv) {
        if (this.idRef.equals(id) && pv != null) {
            this.resolved = true;
            if (this.area != null) {
                final Trait.InternalLink iLink = new Trait.InternalLink(
                        pv.getKey(), this.idRef);
                this.area.addTrait(Trait.INTERNAL_LINK, iLink);
                this.area = null; // break circular reference from basic link
                                  // area to this resolver
            }
            resolveDependents(id, pv);
        }
    }

    /**
     * Add dependent resolvable. Used to resolve second-order resolvables that
     * depend on resolution of this resolver.
     * 
     * @param dependent
     *            resolvable
     */
    public void addDependent(final Resolvable dependent) {
        if (this.dependents == null) {
            this.dependents = new ArrayList<Resolvable>();
        }
        this.dependents.add(dependent);
    }

    private void resolveDependents(final String id, final PageViewport pv) {
        if (this.dependents != null) {
            final List<PageViewport> pages = new ArrayList<PageViewport>();
            pages.add(pv);
            for (final Resolvable r : this.dependents) {
                r.resolveIDRef(id, pages);
            }
        }
    }

}
