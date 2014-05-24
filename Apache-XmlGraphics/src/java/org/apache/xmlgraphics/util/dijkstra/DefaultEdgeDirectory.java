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

/* $Id: DefaultEdgeDirectory.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.util.dijkstra;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Default implementation of an edge directory for the {@link DijkstraAlgorithm}
 * .
 */
public class DefaultEdgeDirectory implements EdgeDirectory {

    /** The directory of edges */
    private final Map edges = new java.util.HashMap();

    // Map<Vertex,Map<Vertex,Edge>>

    /**
     * Adds a new edge between two vertices.
     * 
     * @param edge
     *            the new edge
     */
    public void addEdge(final Edge edge) {
        Map directEdges = (Map) this.edges.get(edge.getStart());
        if (directEdges == null) {
            directEdges = new java.util.HashMap();
            this.edges.put(edge.getStart(), directEdges);
        }
        directEdges.put(edge.getEnd(), edge);
    }

    /** {@inheritDoc} */
    @Override
    public int getPenalty(final Vertex start, final Vertex end) {
        final Map edgeMap = (Map) this.edges.get(start);
        if (edgeMap != null) {
            final Edge route = (Edge) edgeMap.get(end);
            if (route != null) {
                final int penalty = route.getPenalty();
                if (penalty < 0) {
                    throw new IllegalStateException(
                            "Penalty must not be negative");
                }
                return penalty;
            }
        }
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public Iterator getDestinations(final Vertex origin) {
        final Map directRoutes = (Map) this.edges.get(origin);
        if (directRoutes != null) {
            final Iterator iter = directRoutes.keySet().iterator();
            return iter;
        }
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Returns an iterator over all edges with the given origin.
     * 
     * @param origin
     *            the origin
     * @return an iterator over Edge instances
     */
    public Iterator getEdges(final Vertex origin) {
        final Map directRoutes = (Map) this.edges.get(origin);
        if (directRoutes != null) {
            final Iterator iter = directRoutes.values().iterator();
            return iter;
        }
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Returns the best edge (the edge with the lowest penalty) between two
     * given vertices.
     * 
     * @param start
     *            the start vertex
     * @param end
     *            the end vertex
     * @return the best vertex or null if none is found
     */
    public Edge getBestEdge(final Vertex start, final Vertex end) {
        Edge best = null;
        final Iterator iter = getEdges(start);
        while (iter.hasNext()) {
            final Edge edge = (Edge) iter.next();
            if (edge.getEnd().equals(end)) {
                if (best == null || edge.getPenalty() < best.getPenalty()) {
                    best = edge;
                }
            }
        }
        return best;
    }

}
