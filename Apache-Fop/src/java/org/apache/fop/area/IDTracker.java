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

/* $Id: IDTracker.java 1062901 2011-01-24 18:06:25Z adelmelle $ */

package org.apache.fop.area;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Used by the AreaTreeHandler to keep track of ID reference usage on a
 * PageViewport level.
 */
@Slf4j
public class IDTracker {

    // Map of ID's whose area is located on one or more consecutive
    // PageViewports. Each ID has a list of PageViewports that
    // form the defined area of this ID
    private final Map<String, List<PageViewport>> idLocations = new java.util.HashMap<String, List<PageViewport>>();

    // idref's whose target PageViewports have yet to be identified
    // Each idref has a HashSet of Resolvable objects containing that idref
    private final Map<String, Set<Resolvable>> unresolvedIDRefs = new java.util.HashMap<String, Set<Resolvable>>();

    private final Set<String> unfinishedIDs = new java.util.HashSet<String>();

    private final Set<String> alreadyResolvedIDs = new java.util.HashSet<String>();

    /**
     * Tie a PageViewport with an ID found on a child area of the PV. Note that
     * an area with a given ID may be on more than one PV, hence an ID may have
     * more than one PV associated with it.
     *
     * @param id
     *            the property ID of the area
     * @param pv
     *            a page viewport that contains the area with this ID
     */
    public void associateIDWithPageViewport(final String id,
            final PageViewport pv) {
        if (log.isDebugEnabled()) {
            log.debug("associateIDWithPageViewport(" + id + ", " + pv + ")");
        }
        List<PageViewport> pvList = this.idLocations.get(id);
        if (pvList == null) { // first time ID located
            pvList = new java.util.ArrayList<PageViewport>();
            this.idLocations.put(id, pvList);
            pvList.add(pv);
            // signal the PageViewport that it is the first PV to contain this
            // id:
            pv.setFirstWithID(id);
            /*
             * See if this ID is in the unresolved idref list, if so resolve
             * Resolvable objects tied to it.
             */
            if (!this.unfinishedIDs.contains(id)) {
                tryIDResolution(id, pvList);
            }
        } else {
            /*
             * TODO: The check is a quick-fix to avoid a waste when adding
             * inline-ids to the page
             */
            if (!pvList.contains(pv)) {
                pvList.add(pv);
            }
        }
    }

    /**
     * This method tie an ID to the areaTreeHandler until this one is ready to
     * be processed. This is used in page-number-citation-last processing so we
     * know when an id can be resolved.
     *
     * @param id
     *            the id of the object being processed
     */
    public void signalPendingID(final String id) {
        if (log.isDebugEnabled()) {
            log.debug("signalPendingID(" + id + ")");
        }
        this.unfinishedIDs.add(id);
    }

    /**
     * Signals that all areas for the formatting object with the given ID have
     * been generated. This is used to determine when page-number-citation-last
     * ref-ids can be resolved.
     *
     * @param id
     *            the id of the formatting object which was just finished
     */
    public void signalIDProcessed(final String id) {
        if (log.isDebugEnabled()) {
            log.debug("signalIDProcessed(" + id + ")");
        }

        this.alreadyResolvedIDs.add(id);
        if (!this.unfinishedIDs.contains(id)) {
            return;
        }
        this.unfinishedIDs.remove(id);

        final List<PageViewport> idLocs = this.idLocations.get(id);
        final Set<Resolvable> todo = this.unresolvedIDRefs.get(id);
        if (todo != null) {
            for (final Resolvable res : todo) {
                res.resolveIDRef(id, idLocs);
            }
            this.unresolvedIDRefs.remove(id);
        }
    }

    /**
     * Check if an ID has already been resolved
     *
     * @param id
     *            the id to check
     * @return true if the ID has been resolved
     */
    public boolean alreadyResolvedID(final String id) {
        return this.alreadyResolvedIDs.contains(id);
    }

    /**
     * Tries to resolve all unresolved ID references on the given set of pages.
     *
     * @param id
     *            ID to resolve
     * @param pvList
     *            list of PageViewports
     */
    private void tryIDResolution(final String id,
            final List<PageViewport> pvList) {
        final Set<Resolvable> todo = this.unresolvedIDRefs.get(id);
        if (todo != null) {
            for (final Resolvable res : todo) {
                if (!this.unfinishedIDs.contains(id)) {
                    res.resolveIDRef(id, pvList);
                } else {
                    return;
                }
            }
            this.alreadyResolvedIDs.add(id);
            this.unresolvedIDRefs.remove(id);
        }
    }

    /**
     * Tries to resolve all unresolved ID references on the given page.
     *
     * @param pv
     *            page viewport whose ID refs to resolve
     */
    public void tryIDResolution(final PageViewport pv) {
        final String[] ids = pv.getIDRefs();
        if (ids != null) {
            for (final String id : ids) {
                final List<PageViewport> pvList = this.idLocations.get(id);
                if (!(pvList == null || pvList.isEmpty())) {
                    tryIDResolution(id, pvList);
                }
            }
        }
    }

    /**
     * Get the list of page viewports that have an area with a given id.
     *
     * @param id
     *            the id to lookup
     * @return the list of PageViewports
     */
    public List<PageViewport> getPageViewportsContainingID(final String id) {
        if (!(this.idLocations == null || this.idLocations.isEmpty())) {
            final List<PageViewport> idLocs = this.idLocations.get(id);
            if (idLocs != null) {
                return idLocs;
            }
        }
        return Collections.emptyList();
    }

    /**
     * Get the first {@link PageViewport} containing content generated by the FO
     * with the given {@code id}.
     *
     * @param id
     *            the id
     * @return the first {@link PageViewport} for the id; {@code null} if no
     *         matching {@link PageViewport} was found
     */
    public PageViewport getFirstPageViewportContaining(final String id) {
        final List<PageViewport> list = getPageViewportsContainingID(id);
        if (!(list == null || list.isEmpty())) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Get the last {@link PageViewport} containing content generated by the FO
     * with the given {@code id}.
     *
     * @param id
     *            the id
     * @return the last {@link PageViewport} for the id; {@code null} if no
     *         matching {@link PageViewport} was found
     */
    public PageViewport getLastPageViewportContaining(final String id) {
        final List<PageViewport> list = getPageViewportsContainingID(id);
        if (!(list == null || list.isEmpty())) {
            return list.get(list.size() - 1);
        }
        return null;
    }

    /**
     * Add an Resolvable object with an unresolved idref
     *
     * @param idref
     *            the idref whose target id has not yet been located
     * @param res
     *            the Resolvable object needing the idref to be resolved
     */
    public void addUnresolvedIDRef(final String idref, final Resolvable res) {
        Set<Resolvable> todo = this.unresolvedIDRefs.get(idref);
        if (todo == null) {
            todo = new java.util.HashSet<Resolvable>();
            this.unresolvedIDRefs.put(idref, todo);
        }
        // add Resolvable object to this HashSet
        todo.add(res);
    }
}
