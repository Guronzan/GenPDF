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

/* $Id: ConditionalBorder.java 1326144 2012-04-14 16:48:59Z gadams $ */

package org.apache.fop.fo.flow.table;

import org.apache.fop.layoutmgr.table.CollapsingBorderModel;

/**
 * A class that holds the three possible values for a border-before/after on a
 * table-cell, in the collapsing model. These three values are (for
 * border-before, similar for border-after):
 * <ul>
 * <li>normal: common case, when a cell follows the cell before on a same page;</li>
 * <li>leading: when the table is broken and the cell appears at the top of a
 * page, in which case its border must be resolved with the header (or the top
 * of the table) instead of with the previous cell;</li>
 * <li>rest: when a cell is broken over several pages; same as leading but with
 * conditionality taken into account.</li>
 * </ul>
 */
public class ConditionalBorder {

    /** normal border */
    public static final int NORMAL = 0;

    /** leading and trailing border */
    public static final int LEADING_TRAILING = 1;

    /** all the rest */
    public static final int REST = 2;

    /** Normal case, no break. */
    BorderSpecification normal; // CSOK: VisibilityModifier

    /** Special case: the cell is at the top or the bottom of the page. */
    BorderSpecification leadingTrailing; // CSOK: VisibilityModifier

    /** Special case: break inside the cell. */
    BorderSpecification rest; // CSOK: VisibilityModifier

    /** The model used to resolve borders. */
    private final CollapsingBorderModel collapsingBorderModel;

    private ConditionalBorder(final BorderSpecification normal,
            final BorderSpecification leadingTrailing,
            final BorderSpecification rest,
            final CollapsingBorderModel collapsingBorderModel) {
        assert collapsingBorderModel != null;
        this.normal = normal;
        this.leadingTrailing = leadingTrailing;
        this.rest = rest;
        this.collapsingBorderModel = collapsingBorderModel;
    }

    /**
     * Creates a new conditional border.
     *
     * @param borderSpecification
     *            the border specification to take as a basis
     * @param collapsingBorderModel
     *            the model that will be used to resolved borders
     */
    ConditionalBorder(final BorderSpecification borderSpecification,
            final CollapsingBorderModel collapsingBorderModel) {
        this(borderSpecification, borderSpecification, borderSpecification
                .getBorderInfo().getWidth().isDiscard() ? BorderSpecification
                .getDefaultBorder() : borderSpecification,
                collapsingBorderModel);
    }

    /**
     * Resolves and updates the relevant parts of this border as well as the
     * given one.
     *
     * @param competitor
     * @param withNormal
     * @param withLeadingTrailing
     * @param withRest
     */
    void resolve(final ConditionalBorder competitor, final boolean withNormal,
            final boolean withLeadingTrailing, final boolean withRest) {
        if (withNormal) {
            final BorderSpecification resolvedBorder = this.collapsingBorderModel
                    .determineWinner(this.normal, competitor.normal);
            if (resolvedBorder != null) {
                this.normal = resolvedBorder;
                competitor.normal = resolvedBorder;
            }
        }
        if (withLeadingTrailing) {
            final BorderSpecification resolvedBorder = this.collapsingBorderModel
                    .determineWinner(this.leadingTrailing,
                            competitor.leadingTrailing);
            if (resolvedBorder != null) {
                this.leadingTrailing = resolvedBorder;
                competitor.leadingTrailing = resolvedBorder;
            }
        }
        if (withRest) {
            final BorderSpecification resolvedBorder = this.collapsingBorderModel
                    .determineWinner(this.rest, competitor.rest);
            if (resolvedBorder != null) {
                this.rest = resolvedBorder;
                competitor.rest = resolvedBorder;
            }
        }
    }

    /**
     * Integrates the given segment in this border. Unlike for
     * {@link #integrateSegment(ConditionalBorder, boolean, boolean, boolean)},
     * this method nicely handles the case where the CollapsingBorderModel
     * returns null, by keeping the components to their old values.
     *
     * @param competitor
     * @param withNormal
     * @param withLeadingTrailing
     * @param withRest
     */
    void integrateCompetingSegment(final ConditionalBorder competitor,
            final boolean withNormal, final boolean withLeadingTrailing,
            final boolean withRest) {
        if (withNormal) {
            final BorderSpecification resolvedBorder = this.collapsingBorderModel
                    .determineWinner(this.normal, competitor.normal);
            if (resolvedBorder != null) {
                this.normal = resolvedBorder;
            }
        }
        if (withLeadingTrailing) {
            final BorderSpecification resolvedBorder = this.collapsingBorderModel
                    .determineWinner(this.leadingTrailing,
                            competitor.leadingTrailing);
            if (resolvedBorder != null) {
                this.leadingTrailing = resolvedBorder;
            }
        }
        if (withRest) {
            final BorderSpecification resolvedBorder = this.collapsingBorderModel
                    .determineWinner(this.rest, competitor.rest);
            if (resolvedBorder != null) {
                this.rest = resolvedBorder;
            }
        }
    }

    /**
     * Updates this border after taking into account the given segment. The
     * CollapsingBorderModel is not expected to return null.
     *
     * @param segment
     * @param withNormal
     * @param withLeadingTrailing
     * @param withRest
     */
    void integrateSegment(final ConditionalBorder segment,
            final boolean withNormal, final boolean withLeadingTrailing,
            final boolean withRest) {
        if (withNormal) {
            this.normal = this.collapsingBorderModel.determineWinner(
                    this.normal, segment.normal);
            assert this.normal != null;
        }
        if (withLeadingTrailing) {
            this.leadingTrailing = this.collapsingBorderModel.determineWinner(
                    this.leadingTrailing, segment.leadingTrailing);
            assert this.leadingTrailing != null;
        }
        if (withRest) {
            this.rest = this.collapsingBorderModel.determineWinner(this.rest,
                    segment.rest);
            assert this.rest != null;
        }
    }

    /**
     * Returns a shallow copy of this border.
     *
     * @return a copy of this border
     */
    ConditionalBorder copy() {
        return new ConditionalBorder(this.normal, this.leadingTrailing,
                this.rest, this.collapsingBorderModel);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "{normal: " + this.normal + ", leading: " + this.leadingTrailing
                + ", rest: " + this.rest + "}";
    }

    /**
     * Returns a default border specification.
     *
     * @param collapsingBorderModel
     *            the model that will be used to resolve borders
     * @return a border with style 'none' for all of the three components
     */
    static ConditionalBorder getDefaultBorder(
            final CollapsingBorderModel collapsingBorderModel) {
        final BorderSpecification defaultBorderSpec = BorderSpecification
                .getDefaultBorder();
        return new ConditionalBorder(defaultBorderSpec, defaultBorderSpec,
                defaultBorderSpec, collapsingBorderModel);
    }
}
