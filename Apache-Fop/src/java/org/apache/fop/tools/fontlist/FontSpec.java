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

/* $Id: FontSpec.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.tools.fontlist;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.fonts.FontTriplet;

/**
 * Represents a font with information on how it can be used from XSL-FO.
 */
public class FontSpec implements Comparable {

    private final String key;
    private final FontMetrics metrics;
    private final SortedSet<String> familyNames = new TreeSet<String>();
    private final Collection triplets = new TreeSet();

    /**
     * Creates a new font spec.
     * 
     * @param key
     *            the internal font key
     * @param metrics
     *            the font metrics
     */
    public FontSpec(final String key, final FontMetrics metrics) {
        this.key = key;
        this.metrics = metrics;
    }

    /**
     * Adds font family names.
     * 
     * @param names
     *            the names
     */
    public void addFamilyNames(final Collection<String> names) {
        this.familyNames.addAll(names);
    }

    /**
     * Adds a font triplet.
     * 
     * @param triplet
     *            the font triplet
     */
    public void addTriplet(final FontTriplet triplet) {
        this.triplets.add(triplet);
    }

    /**
     * Returns the font family names.
     * 
     * @return the font family names
     */
    public SortedSet getFamilyNames() {
        return Collections.unmodifiableSortedSet(this.familyNames);
    }

    /**
     * Returns the font triplets.
     * 
     * @return the font triplets
     */
    public Collection getTriplets() {
        return Collections.unmodifiableCollection(this.triplets);
    }

    /**
     * Returns the internal font key.
     * 
     * @return the internal font key
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Returns the font metrics.
     * 
     * @return the font metrics
     */
    public FontMetrics getFontMetrics() {
        return this.metrics;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(final Object o) {
        final FontSpec other = (FontSpec) o;
        return this.metrics.getFullName()
                .compareTo(other.metrics.getFullName());
    }

}
