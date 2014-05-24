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

/* $Id$ */

package org.apache.fop.complexscripts.fonts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.fop.complexscripts.scripts.ScriptProcessor;
import org.apache.fop.complexscripts.util.GlyphSequence;
import org.apache.fop.complexscripts.util.GlyphTester;

// CSOFF: LineLengthCheck
// CSOFF: InnerAssignmentCheck
// CSOFF: NoWhitespaceAfterCheck
// CSOFF: ParameterNumberCheck

/**
 * <p>
 * The <code>GlyphPositioningTable</code> class is a glyph table that implements
 * <code>GlyphPositioning</code> functionality.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
public class GlyphPositioningTable extends GlyphTable {

    /** single positioning subtable type */
    public static final int GPOS_LOOKUP_TYPE_SINGLE = 1;
    /** multiple positioning subtable type */
    public static final int GPOS_LOOKUP_TYPE_PAIR = 2;
    /** cursive positioning subtable type */
    public static final int GPOS_LOOKUP_TYPE_CURSIVE = 3;
    /** mark to base positioning subtable type */
    public static final int GPOS_LOOKUP_TYPE_MARK_TO_BASE = 4;
    /** mark to ligature positioning subtable type */
    public static final int GPOS_LOOKUP_TYPE_MARK_TO_LIGATURE = 5;
    /** mark to mark positioning subtable type */
    public static final int GPOS_LOOKUP_TYPE_MARK_TO_MARK = 6;
    /** contextual positioning subtable type */
    public static final int GPOS_LOOKUP_TYPE_CONTEXTUAL = 7;
    /** chained contextual positioning subtable type */
    public static final int GPOS_LOOKUP_TYPE_CHAINED_CONTEXTUAL = 8;
    /** extension positioning subtable type */
    public static final int GPOS_LOOKUP_TYPE_EXTENSION_POSITIONING = 9;

    /**
     * Instantiate a <code>GlyphPositioningTable</code> object using the
     * specified lookups and subtables.
     *
     * @param gdef
     *            glyph definition table that applies
     * @param lookups
     *            a map of lookup specifications to subtable identifier strings
     * @param subtables
     *            a list of identified subtables
     */
    public GlyphPositioningTable(final GlyphDefinitionTable gdef,
            final Map lookups, final List subtables) {
        super(gdef, lookups);
        if (subtables == null || subtables.size() == 0) {
            throw new AdvancedTypographicTableFormatException(
                    "subtables must be non-empty");
        } else {
            for (final Iterator it = subtables.iterator(); it.hasNext();) {
                final Object o = it.next();
                if (o instanceof GlyphPositioningSubtable) {
                    addSubtable((GlyphSubtable) o);
                } else {
                    throw new AdvancedTypographicTableFormatException(
                            "subtable must be a glyph positioning subtable");
                }
            }
            freezeSubtables();
        }
    }

    /**
     * Map a lookup type name to its constant (integer) value.
     *
     * @param name
     *            lookup type name
     * @return lookup type
     */
    public static int getLookupTypeFromName(final String name) {
        int t;
        final String s = name.toLowerCase();
        if ("single".equals(s)) {
            t = GPOS_LOOKUP_TYPE_SINGLE;
        } else if ("pair".equals(s)) {
            t = GPOS_LOOKUP_TYPE_PAIR;
        } else if ("cursive".equals(s)) {
            t = GPOS_LOOKUP_TYPE_CURSIVE;
        } else if ("marktobase".equals(s)) {
            t = GPOS_LOOKUP_TYPE_MARK_TO_BASE;
        } else if ("marktoligature".equals(s)) {
            t = GPOS_LOOKUP_TYPE_MARK_TO_LIGATURE;
        } else if ("marktomark".equals(s)) {
            t = GPOS_LOOKUP_TYPE_MARK_TO_MARK;
        } else if ("contextual".equals(s)) {
            t = GPOS_LOOKUP_TYPE_CONTEXTUAL;
        } else if ("chainedcontextual".equals(s)) {
            t = GPOS_LOOKUP_TYPE_CHAINED_CONTEXTUAL;
        } else if ("extensionpositioning".equals(s)) {
            t = GPOS_LOOKUP_TYPE_EXTENSION_POSITIONING;
        } else {
            t = -1;
        }
        return t;
    }

    /**
     * Map a lookup type constant (integer) value to its name.
     *
     * @param type
     *            lookup type
     * @return lookup type name
     */
    public static String getLookupTypeName(final int type) {
        String tn;
        switch (type) {
        case GPOS_LOOKUP_TYPE_SINGLE:
            tn = "single";
            break;
        case GPOS_LOOKUP_TYPE_PAIR:
            tn = "pair";
            break;
        case GPOS_LOOKUP_TYPE_CURSIVE:
            tn = "cursive";
            break;
        case GPOS_LOOKUP_TYPE_MARK_TO_BASE:
            tn = "marktobase";
            break;
        case GPOS_LOOKUP_TYPE_MARK_TO_LIGATURE:
            tn = "marktoligature";
            break;
        case GPOS_LOOKUP_TYPE_MARK_TO_MARK:
            tn = "marktomark";
            break;
        case GPOS_LOOKUP_TYPE_CONTEXTUAL:
            tn = "contextual";
            break;
        case GPOS_LOOKUP_TYPE_CHAINED_CONTEXTUAL:
            tn = "chainedcontextual";
            break;
        case GPOS_LOOKUP_TYPE_EXTENSION_POSITIONING:
            tn = "extensionpositioning";
            break;
        default:
            tn = "unknown";
            break;
        }
        return tn;
    }

    /**
     * Create a positioning subtable according to the specified arguments.
     *
     * @param type
     *            subtable type
     * @param id
     *            subtable identifier
     * @param sequence
     *            subtable sequence
     * @param flags
     *            subtable flags
     * @param format
     *            subtable format
     * @param coverage
     *            subtable coverage table
     * @param entries
     *            subtable entries
     * @return a glyph subtable instance
     */
    public static GlyphSubtable createSubtable(final int type, final String id,
            final int sequence, final int flags, final int format,
            final GlyphCoverageTable coverage, final List entries) {
        GlyphSubtable st = null;
        switch (type) {
        case GPOS_LOOKUP_TYPE_SINGLE:
            st = SingleSubtable.create(id, sequence, flags, format, coverage,
                    entries);
            break;
        case GPOS_LOOKUP_TYPE_PAIR:
            st = PairSubtable.create(id, sequence, flags, format, coverage,
                    entries);
            break;
        case GPOS_LOOKUP_TYPE_CURSIVE:
            st = CursiveSubtable.create(id, sequence, flags, format, coverage,
                    entries);
            break;
        case GPOS_LOOKUP_TYPE_MARK_TO_BASE:
            st = MarkToBaseSubtable.create(id, sequence, flags, format,
                    coverage, entries);
            break;
        case GPOS_LOOKUP_TYPE_MARK_TO_LIGATURE:
            st = MarkToLigatureSubtable.create(id, sequence, flags, format,
                    coverage, entries);
            break;
        case GPOS_LOOKUP_TYPE_MARK_TO_MARK:
            st = MarkToMarkSubtable.create(id, sequence, flags, format,
                    coverage, entries);
            break;
        case GPOS_LOOKUP_TYPE_CONTEXTUAL:
            st = ContextualSubtable.create(id, sequence, flags, format,
                    coverage, entries);
            break;
        case GPOS_LOOKUP_TYPE_CHAINED_CONTEXTUAL:
            st = ChainedContextualSubtable.create(id, sequence, flags, format,
                    coverage, entries);
            break;
        default:
            break;
        }
        return st;
    }

    /**
     * Create a positioning subtable according to the specified arguments.
     *
     * @param type
     *            subtable type
     * @param id
     *            subtable identifier
     * @param sequence
     *            subtable sequence
     * @param flags
     *            subtable flags
     * @param format
     *            subtable format
     * @param coverage
     *            list of coverage table entries
     * @param entries
     *            subtable entries
     * @return a glyph subtable instance
     */
    public static GlyphSubtable createSubtable(final int type, final String id,
            final int sequence, final int flags, final int format,
            final List coverage, final List entries) {
        return createSubtable(type, id, sequence, flags, format,
                GlyphCoverageTable.createCoverageTable(coverage), entries);
    }

    /**
     * Perform positioning processing using all matching lookups.
     *
     * @param gs
     *            an input glyph sequence
     * @param script
     *            a script identifier
     * @param language
     *            a language identifier
     * @param fontSize
     *            size in device units
     * @param widths
     *            array of default advancements for each glyph
     * @param adjustments
     *            accumulated adjustments array (sequence) of 4-tuples of
     *            placement [PX,PY] and advance [AX,AY] adjustments, in that
     *            order, with one 4-tuple for each element of glyph sequence
     * @return true if some adjustment is not zero; otherwise, false
     */
    public boolean position(final GlyphSequence gs, final String script,
            final String language, final int fontSize, final int[] widths,
            final int[][] adjustments) {
        final Map/* <LookupSpec,List<LookupTable>> */lookups = matchLookups(
                script, language, "*");
        if (lookups != null && lookups.size() > 0) {
            final ScriptProcessor sp = ScriptProcessor.getInstance(script);
            return sp.position(this, gs, script, language, fontSize, lookups,
                    widths, adjustments);
        } else {
            return false;
        }
    }

    private abstract static class SingleSubtable extends
    GlyphPositioningSubtable {
        SingleSubtable(final String id, final int sequence, final int flags,
                final int format, final GlyphCoverageTable coverage,
                final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GPOS_LOOKUP_TYPE_SINGLE;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof SingleSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean position(final GlyphPositioningState ps) {
            final int gi = ps.getGlyph();
            int ci;
            if ((ci = getCoverageIndex(gi)) < 0) {
                return false;
            } else {
                final Value v = getValue(ci, gi);
                if (v != null) {
                    if (ps.adjust(v)) {
                        ps.setAdjusted(true);
                    }
                    ps.consume(1);
                }
                return true;
            }
        }

        /**
         * Obtain positioning value for coverage index.
         *
         * @param ci
         *            coverage index
         * @param gi
         *            input glyph index
         * @return positioning value or null if none applies
         */
        public abstract Value getValue(final int ci, final int gi);

        static GlyphPositioningSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new SingleSubtableFormat1(id, sequence, flags, format,
                        coverage, entries);
            } else if (format == 2) {
                return new SingleSubtableFormat2(id, sequence, flags, format,
                        coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class SingleSubtableFormat1 extends SingleSubtable {
        private Value value;
        private int ciMax;

        SingleSubtableFormat1(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.value != null) {
                final List entries = new ArrayList(1);
                entries.add(this.value);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public Value getValue(final int ci, final int gi) {
            if (this.value != null && ci <= this.ciMax) {
                return this.value;
            } else {
                return null;
            }
        }

        private void populate(final List entries) {
            if (entries == null || entries.size() != 1) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null and contain exactly one entry");
            } else {
                Value v;
                final Object o = entries.get(0);
                if (o instanceof Value) {
                    v = (Value) o;
                } else {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries entry, must be Value, but is: "
                                    + (o != null ? o.getClass() : null));
                }
                assert this.value == null;
                this.value = v;
                this.ciMax = getCoverageSize() - 1;
            }
        }
    }

    private static class SingleSubtableFormat2 extends SingleSubtable {
        private Value[] values;

        SingleSubtableFormat2(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.values != null) {
                final List entries = new ArrayList(this.values.length);
                for (final Value value : this.values) {
                    entries.add(value);
                }
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public Value getValue(final int ci, final int gi) {
            if (this.values != null && ci < this.values.length) {
                return this.values[ci];
            } else {
                return null;
            }
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 1) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 1 entry");
            } else {
                Object o;
                if ((o = entries.get(0)) == null || !(o instanceof Value[])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, single entry must be a Value[], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    final Value[] va = (Value[]) o;
                    if (va.length != getCoverageSize()) {
                        throw new AdvancedTypographicTableFormatException(
                                "illegal values array, " + entries.size()
                                + " values present, but requires "
                                + getCoverageSize() + " values");
                    } else {
                        assert this.values == null;
                        this.values = va;
                    }
                }
            }
        }
    }

    private abstract static class PairSubtable extends GlyphPositioningSubtable {
        PairSubtable(final String id, final int sequence, final int flags,
                final int format, final GlyphCoverageTable coverage,
                final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GPOS_LOOKUP_TYPE_PAIR;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof PairSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean position(final GlyphPositioningState ps) {
            boolean applied = false;
            final int gi = ps.getGlyph(0);
            int ci;
            if ((ci = getCoverageIndex(gi)) >= 0) {
                final int[] counts = ps.getGlyphsAvailable(0);
                final int nga = counts[0];
                if (nga > 1) {
                    final int[] iga = ps.getGlyphs(0, 2, null, counts);
                    if (iga != null && iga.length == 2) {
                        final PairValues pv = getPairValues(ci, iga[0], iga[1]);
                        if (pv != null) {
                            final Value v1 = pv.getValue1();
                            if (v1 != null) {
                                if (ps.adjust(v1, 0)) {
                                    ps.setAdjusted(true);
                                }
                            }
                            final Value v2 = pv.getValue2();
                            if (v2 != null) {
                                if (ps.adjust(v2, 1)) {
                                    ps.setAdjusted(true);
                                }
                            }
                            ps.consume(counts[0] + counts[1]);
                            applied = true;
                        }
                    }
                }
            }
            return applied;
        }

        /**
         * Obtain associated pair values.
         *
         * @param ci
         *            coverage index
         * @param gi1
         *            first input glyph index
         * @param gi2
         *            second input glyph index
         * @return pair values or null if none applies
         */
        public abstract PairValues getPairValues(final int ci, final int gi1,
                final int gi2);

        static GlyphPositioningSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new PairSubtableFormat1(id, sequence, flags, format,
                        coverage, entries);
            } else if (format == 2) {
                return new PairSubtableFormat2(id, sequence, flags, format,
                        coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class PairSubtableFormat1 extends PairSubtable {
        private PairValues[][] pvm; // pair values matrix

        PairSubtableFormat1(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.pvm != null) {
                final List entries = new ArrayList(1);
                entries.add(this.pvm);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public PairValues getPairValues(final int ci, final int gi1,
                final int gi2) {
            if (this.pvm != null && ci < this.pvm.length) {
                final PairValues[] pvt = this.pvm[ci];
                for (final PairValues pv : pvt) {
                    if (pv != null) {
                        final int g = pv.getGlyph();
                        if (g < gi2) {
                            continue;
                        } else if (g == gi2) {
                            return pv;
                        } else {
                            break;
                        }
                    }
                }
            }
            return null;
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 1) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 1 entry");
            } else {
                Object o;
                if ((o = entries.get(0)) == null
                        || !(o instanceof PairValues[][])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first (and only) entry must be a PairValues[][], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.pvm = (PairValues[][]) o;
                }
            }
        }
    }

    private static class PairSubtableFormat2 extends PairSubtable {
        private GlyphClassTable cdt1; // class def table 1
        private GlyphClassTable cdt2; // class def table 2
        private int nc1; // class 1 count
        private int nc2; // class 2 count
        private PairValues[][] pvm; // pair values matrix

        PairSubtableFormat2(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.pvm != null) {
                final List entries = new ArrayList(5);
                entries.add(this.cdt1);
                entries.add(this.cdt2);
                entries.add(Integer.valueOf(this.nc1));
                entries.add(Integer.valueOf(this.nc2));
                entries.add(this.pvm);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public PairValues getPairValues(final int ci, final int gi1,
                final int gi2) {
            if (this.pvm != null) {
                final int c1 = this.cdt1.getClassIndex(gi1, 0);
                if (c1 >= 0 && c1 < this.nc1 && c1 < this.pvm.length) {
                    final PairValues[] pvt = this.pvm[c1];
                    if (pvt != null) {
                        final int c2 = this.cdt2.getClassIndex(gi2, 0);
                        if (c2 >= 0 && c2 < this.nc2 && c2 < pvt.length) {
                            return pvt[c2];
                        }
                    }
                }
            }
            return null;
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 5) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 5 entries");
            } else {
                Object o;
                if ((o = entries.get(0)) == null
                        || !(o instanceof GlyphClassTable)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first entry must be an GlyphClassTable, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.cdt1 = (GlyphClassTable) o;
                }
                if ((o = entries.get(1)) == null
                        || !(o instanceof GlyphClassTable)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, second entry must be an GlyphClassTable, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.cdt2 = (GlyphClassTable) o;
                }
                if ((o = entries.get(2)) == null || !(o instanceof Integer)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, third entry must be an Integer, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.nc1 = ((Integer) o).intValue();
                }
                if ((o = entries.get(3)) == null || !(o instanceof Integer)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, fourth entry must be an Integer, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.nc2 = ((Integer) o).intValue();
                }
                if ((o = entries.get(4)) == null
                        || !(o instanceof PairValues[][])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, fifth entry must be a PairValues[][], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.pvm = (PairValues[][]) o;
                }
            }
        }
    }

    private abstract static class CursiveSubtable extends
    GlyphPositioningSubtable {
        CursiveSubtable(final String id, final int sequence, final int flags,
                final int format, final GlyphCoverageTable coverage,
                final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GPOS_LOOKUP_TYPE_CURSIVE;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof CursiveSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean position(final GlyphPositioningState ps) {
            boolean applied = false;
            final int gi = ps.getGlyph(0);
            int ci;
            if ((ci = getCoverageIndex(gi)) >= 0) {
                final int[] counts = ps.getGlyphsAvailable(0);
                final int nga = counts[0];
                if (nga > 1) {
                    final int[] iga = ps.getGlyphs(0, 2, null, counts);
                    if (iga != null && iga.length == 2) {
                        // int gi1 = gi;
                        final int ci1 = ci;
                        final int gi2 = iga[1];
                        final int ci2 = getCoverageIndex(gi2);
                        final Anchor[] aa = getExitEntryAnchors(ci1, ci2);
                        if (aa != null) {
                            final Anchor exa = aa[0];
                            final Anchor ena = aa[1];
                            // int exw = ps.getWidth ( gi1 );
                            final int enw = ps.getWidth(gi2);
                            if (exa != null && ena != null) {
                                final Value v = ena.getAlignmentAdjustment(exa);
                                v.adjust(-enw, 0, 0, 0);
                                if (ps.adjust(v)) {
                                    ps.setAdjusted(true);
                                }
                            }
                            // consume only first glyph of exit/entry glyph pair
                            ps.consume(1);
                            applied = true;
                        }
                    }
                }
            }
            return applied;
        }

        /**
         * Obtain exit anchor for first glyph with coverage index
         * <code>ci1</code> and entry anchor for second glyph with coverage
         * index <code>ci2</code>.
         *
         * @param ci1
         *            coverage index of first glyph (may be negative)
         * @param ci2
         *            coverage index of second glyph (may be negative)
         * @return array of two anchors or null if either coverage index is
         *         negative or corresponding anchor is missing, where the first
         *         entry is the exit anchor of the first glyph and the second
         *         entry is the entry anchor of the second glyph
         */
        public abstract Anchor[] getExitEntryAnchors(final int ci1,
                final int ci2);

        static GlyphPositioningSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new CursiveSubtableFormat1(id, sequence, flags, format,
                        coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class CursiveSubtableFormat1 extends CursiveSubtable {
        private Anchor[] aa; // anchor array, where even entries are entry
        // anchors, and odd entries are exit anchors

        CursiveSubtableFormat1(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.aa != null) {
                final List entries = new ArrayList(1);
                entries.add(this.aa);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public Anchor[] getExitEntryAnchors(final int ci1, final int ci2) {
            if (ci1 >= 0 && ci2 >= 0) {
                final int ai1 = ci1 * 2 + 1; // ci1 denotes glyph with exit
                // anchor
                final int ai2 = ci2 * 2 + 0; // ci2 denotes glyph with entry
                // anchor
                if (this.aa != null && ai1 < this.aa.length
                        && ai2 < this.aa.length) {
                    final Anchor exa = this.aa[ai1];
                    final Anchor ena = this.aa[ai2];
                    if (exa != null && ena != null) {
                        return new Anchor[] { exa, ena };
                    }
                }
            }
            return null;
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 1) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 1 entry");
            } else {
                Object o;
                if ((o = entries.get(0)) == null || !(o instanceof Anchor[])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first (and only) entry must be a Anchor[], but is: "
                                    + (o != null ? o.getClass() : null));
                } else if (((Anchor[]) o).length % 2 != 0) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, Anchor[] array must have an even number of entries, but has: "
                                    + ((Anchor[]) o).length);
                } else {
                    this.aa = (Anchor[]) o;
                }
            }
        }
    }

    private abstract static class MarkToBaseSubtable extends
    GlyphPositioningSubtable {
        MarkToBaseSubtable(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GPOS_LOOKUP_TYPE_MARK_TO_BASE;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof MarkToBaseSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean position(final GlyphPositioningState ps) {
            boolean applied = false;
            final int giMark = ps.getGlyph();
            int ciMark;
            if ((ciMark = getCoverageIndex(giMark)) >= 0) {
                final MarkAnchor ma = getMarkAnchor(ciMark, giMark);
                if (ma != null) {
                    for (int i = 0, n = ps.getPosition(); i < n; i++) {
                        final int gi = ps.getGlyph(-(i + 1));
                        if (ps.isMark(gi)) {
                            continue;
                        } else {
                            final Anchor a = getBaseAnchor(gi,
                                    ma.getMarkClass());
                            if (a != null) {
                                final Value v = a.getAlignmentAdjustment(ma);
                                // start experimental fix for END OF AYAH in
                                // Lateef/Scheherazade
                                final int[] aa = ps.getAdjustment();
                                if (aa[2] == 0) {
                                    v.adjust(0, 0, -ps.getWidth(giMark), 0);
                                }
                                // end experimental fix for END OF AYAH in
                                // Lateef/Scheherazade
                                if (ps.adjust(v)) {
                                    ps.setAdjusted(true);
                                }
                            }
                            ps.consume(1);
                            applied = true;
                            break;
                        }
                    }
                }
            }
            return applied;
        }

        /**
         * Obtain mark anchor associated with mark coverage index.
         *
         * @param ciMark
         *            coverage index
         * @param giMark
         *            input glyph index of mark glyph
         * @return mark anchor or null if none applies
         */
        public abstract MarkAnchor getMarkAnchor(final int ciMark,
                final int giMark);

        /**
         * Obtain anchor associated with base glyph index and mark class.
         *
         * @param giBase
         *            input glyph index of base glyph
         * @param markClass
         *            class number of mark glyph
         * @return anchor or null if none applies
         */
        public abstract Anchor getBaseAnchor(final int giBase,
                final int markClass);

        static GlyphPositioningSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new MarkToBaseSubtableFormat1(id, sequence, flags,
                        format, coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class MarkToBaseSubtableFormat1 extends MarkToBaseSubtable {
        private GlyphCoverageTable bct; // base coverage table
        private int nmc; // mark class count
        private MarkAnchor[] maa; // mark anchor array, ordered by mark coverage
        // index
        private Anchor[][] bam; // base anchor matrix, ordered by base coverage
        // index, then by mark class

        MarkToBaseSubtableFormat1(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.bct != null && this.maa != null && this.nmc > 0
                    && this.bam != null) {
                final List entries = new ArrayList(4);
                entries.add(this.bct);
                entries.add(Integer.valueOf(this.nmc));
                entries.add(this.maa);
                entries.add(this.bam);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public MarkAnchor getMarkAnchor(final int ciMark, final int giMark) {
            if (this.maa != null && ciMark < this.maa.length) {
                return this.maa[ciMark];
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public Anchor getBaseAnchor(final int giBase, final int markClass) {
            int ciBase;
            if (this.bct != null
                    && (ciBase = this.bct.getCoverageIndex(giBase)) >= 0) {
                if (this.bam != null && ciBase < this.bam.length) {
                    final Anchor[] ba = this.bam[ciBase];
                    if (ba != null && markClass < ba.length) {
                        return ba[markClass];
                    }
                }
            }
            return null;
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 4) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 4 entries");
            } else {
                Object o;
                if ((o = entries.get(0)) == null
                        || !(o instanceof GlyphCoverageTable)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first entry must be an GlyphCoverageTable, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.bct = (GlyphCoverageTable) o;
                }
                if ((o = entries.get(1)) == null || !(o instanceof Integer)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, second entry must be an Integer, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.nmc = ((Integer) o).intValue();
                }
                if ((o = entries.get(2)) == null
                        || !(o instanceof MarkAnchor[])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, third entry must be a MarkAnchor[], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.maa = (MarkAnchor[]) o;
                }
                if ((o = entries.get(3)) == null || !(o instanceof Anchor[][])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, fourth entry must be a Anchor[][], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.bam = (Anchor[][]) o;
                }
            }
        }
    }

    private abstract static class MarkToLigatureSubtable extends
    GlyphPositioningSubtable {
        MarkToLigatureSubtable(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GPOS_LOOKUP_TYPE_MARK_TO_LIGATURE;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof MarkToLigatureSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean position(final GlyphPositioningState ps) {
            boolean applied = false;
            final int giMark = ps.getGlyph();
            int ciMark;
            if ((ciMark = getCoverageIndex(giMark)) >= 0) {
                final MarkAnchor ma = getMarkAnchor(ciMark, giMark);
                final int mxc = getMaxComponentCount();
                if (ma != null) {
                    for (int i = 0, n = ps.getPosition(); i < n; i++) {
                        final int gi = ps.getGlyph(-(i + 1));
                        if (ps.isMark(gi)) {
                            continue;
                        } else {
                            final Anchor a = getLigatureAnchor(gi, mxc, i,
                                    ma.getMarkClass());
                            if (a != null) {
                                if (ps.adjust(a.getAlignmentAdjustment(ma))) {
                                    ps.setAdjusted(true);
                                }
                            }
                            ps.consume(1);
                            applied = true;
                            break;
                        }
                    }
                }
            }
            return applied;
        }

        /**
         * Obtain mark anchor associated with mark coverage index.
         *
         * @param ciMark
         *            coverage index
         * @param giMark
         *            input glyph index of mark glyph
         * @return mark anchor or null if none applies
         */
        public abstract MarkAnchor getMarkAnchor(final int ciMark,
                final int giMark);

        /**
         * Obtain maximum component count.
         *
         * @return maximum component count (>=0)
         */
        public abstract int getMaxComponentCount();

        /**
         * Obtain anchor associated with ligature glyph index and mark class.
         *
         * @param giLig
         *            input glyph index of ligature glyph
         * @param maxComponents
         *            maximum component count
         * @param component
         *            component number (0...maxComponents-1)
         * @param markClass
         *            class number of mark glyph
         * @return anchor or null if none applies
         */
        public abstract Anchor getLigatureAnchor(final int giLig,
                final int maxComponents, final int component,
                final int markClass);

        static GlyphPositioningSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new MarkToLigatureSubtableFormat1(id, sequence, flags,
                        format, coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class MarkToLigatureSubtableFormat1 extends
    MarkToLigatureSubtable {
        private GlyphCoverageTable lct; // ligature coverage table
        private int nmc; // mark class count
        private int mxc; // maximum ligature component count
        private MarkAnchor[] maa; // mark anchor array, ordered by mark coverage
        // index
        private Anchor[][][] lam; // ligature anchor matrix, ordered by ligature
        // coverage index, then ligature component,
        // then mark class

        MarkToLigatureSubtableFormat1(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.lam != null) {
                final List entries = new ArrayList(5);
                entries.add(this.lct);
                entries.add(Integer.valueOf(this.nmc));
                entries.add(Integer.valueOf(this.mxc));
                entries.add(this.maa);
                entries.add(this.lam);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public MarkAnchor getMarkAnchor(final int ciMark, final int giMark) {
            if (this.maa != null && ciMark < this.maa.length) {
                return this.maa[ciMark];
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int getMaxComponentCount() {
            return this.mxc;
        }

        /** {@inheritDoc} */
        @Override
        public Anchor getLigatureAnchor(final int giLig,
                final int maxComponents, final int component,
                final int markClass) {
            int ciLig;
            if (this.lct != null
                    && (ciLig = this.lct.getCoverageIndex(giLig)) >= 0) {
                if (this.lam != null && ciLig < this.lam.length) {
                    final Anchor[][] lcm = this.lam[ciLig];
                    if (component < maxComponents) {
                        final Anchor[] la = lcm[component];
                        if (la != null && markClass < la.length) {
                            return la[markClass];
                        }
                    }
                }
            }
            return null;
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 5) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 5 entries");
            } else {
                Object o;
                if ((o = entries.get(0)) == null
                        || !(o instanceof GlyphCoverageTable)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first entry must be an GlyphCoverageTable, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.lct = (GlyphCoverageTable) o;
                }
                if ((o = entries.get(1)) == null || !(o instanceof Integer)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, second entry must be an Integer, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.nmc = ((Integer) o).intValue();
                }
                if ((o = entries.get(2)) == null || !(o instanceof Integer)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, third entry must be an Integer, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.mxc = ((Integer) o).intValue();
                }
                if ((o = entries.get(3)) == null
                        || !(o instanceof MarkAnchor[])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, fourth entry must be a MarkAnchor[], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.maa = (MarkAnchor[]) o;
                }
                if ((o = entries.get(4)) == null
                        || !(o instanceof Anchor[][][])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, fifth entry must be a Anchor[][][], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.lam = (Anchor[][][]) o;
                }
            }
        }
    }

    private abstract static class MarkToMarkSubtable extends
    GlyphPositioningSubtable {
        MarkToMarkSubtable(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GPOS_LOOKUP_TYPE_MARK_TO_MARK;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof MarkToMarkSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean position(final GlyphPositioningState ps) {
            boolean applied = false;
            final int giMark1 = ps.getGlyph();
            int ciMark1;
            if ((ciMark1 = getCoverageIndex(giMark1)) >= 0) {
                final MarkAnchor ma = getMark1Anchor(ciMark1, giMark1);
                if (ma != null) {
                    if (ps.hasPrev()) {
                        final Anchor a = getMark2Anchor(ps.getGlyph(-1),
                                ma.getMarkClass());
                        if (a != null) {
                            if (ps.adjust(a.getAlignmentAdjustment(ma))) {
                                ps.setAdjusted(true);
                            }
                        }
                        ps.consume(1);
                        applied = true;
                    }
                }
            }
            return applied;
        }

        /**
         * Obtain mark 1 anchor associated with mark 1 coverage index.
         *
         * @param ciMark1
         *            mark 1 coverage index
         * @param giMark1
         *            input glyph index of mark 1 glyph
         * @return mark 1 anchor or null if none applies
         */
        public abstract MarkAnchor getMark1Anchor(final int ciMark1,
                final int giMark1);

        /**
         * Obtain anchor associated with mark 2 glyph index and mark 1 class.
         *
         * @param giMark2
         *            input glyph index of mark 2 glyph
         * @param markClass
         *            class number of mark 1 glyph
         * @return anchor or null if none applies
         */
        public abstract Anchor getMark2Anchor(final int giBase,
                final int markClass);

        static GlyphPositioningSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new MarkToMarkSubtableFormat1(id, sequence, flags,
                        format, coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class MarkToMarkSubtableFormat1 extends MarkToMarkSubtable {
        private GlyphCoverageTable mct2; // mark 2 coverage table
        private int nmc; // mark class count
        private MarkAnchor[] maa; // mark1 anchor array, ordered by mark1
        // coverage index
        private Anchor[][] mam; // mark2 anchor matrix, ordered by mark2
        // coverage index, then by mark1 class

        MarkToMarkSubtableFormat1(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.mct2 != null && this.maa != null && this.nmc > 0
                    && this.mam != null) {
                final List entries = new ArrayList(4);
                entries.add(this.mct2);
                entries.add(Integer.valueOf(this.nmc));
                entries.add(this.maa);
                entries.add(this.mam);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public MarkAnchor getMark1Anchor(final int ciMark1, final int giMark1) {
            if (this.maa != null && ciMark1 < this.maa.length) {
                return this.maa[ciMark1];
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public Anchor getMark2Anchor(final int giMark2, final int markClass) {
            int ciMark2;
            if (this.mct2 != null
                    && (ciMark2 = this.mct2.getCoverageIndex(giMark2)) >= 0) {
                if (this.mam != null && ciMark2 < this.mam.length) {
                    final Anchor[] ma = this.mam[ciMark2];
                    if (ma != null && markClass < ma.length) {
                        return ma[markClass];
                    }
                }
            }
            return null;
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 4) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 4 entries");
            } else {
                Object o;
                if ((o = entries.get(0)) == null
                        || !(o instanceof GlyphCoverageTable)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first entry must be an GlyphCoverageTable, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.mct2 = (GlyphCoverageTable) o;
                }
                if ((o = entries.get(1)) == null || !(o instanceof Integer)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, second entry must be an Integer, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.nmc = ((Integer) o).intValue();
                }
                if ((o = entries.get(2)) == null
                        || !(o instanceof MarkAnchor[])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, third entry must be a MarkAnchor[], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.maa = (MarkAnchor[]) o;
                }
                if ((o = entries.get(3)) == null || !(o instanceof Anchor[][])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, fourth entry must be a Anchor[][], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.mam = (Anchor[][]) o;
                }
            }
        }
    }

    private abstract static class ContextualSubtable extends
    GlyphPositioningSubtable {
        ContextualSubtable(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GPOS_LOOKUP_TYPE_CONTEXTUAL;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof ContextualSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean position(final GlyphPositioningState ps) {
            boolean applied = false;
            final int gi = ps.getGlyph();
            int ci;
            if ((ci = getCoverageIndex(gi)) >= 0) {
                final int[] rv = new int[1];
                final RuleLookup[] la = getLookups(ci, gi, ps, rv);
                if (la != null) {
                    ps.apply(la, rv[0]);
                    applied = true;
                }
            }
            return applied;
        }

        /**
         * Obtain rule lookups set associated current input glyph context.
         *
         * @param ci
         *            coverage index of glyph at current position
         * @param gi
         *            glyph index of glyph at current position
         * @param ps
         *            glyph positioning state
         * @param rv
         *            array of ints used to receive multiple return values, must
         *            be of length 1 or greater, where the first entry is used
         *            to return the input sequence length of the matched rule
         * @return array of rule lookups or null if none applies
         */
        public abstract RuleLookup[] getLookups(final int ci, final int gi,
                final GlyphPositioningState ps, final int[] rv);

        static GlyphPositioningSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new ContextualSubtableFormat1(id, sequence, flags,
                        format, coverage, entries);
            } else if (format == 2) {
                return new ContextualSubtableFormat2(id, sequence, flags,
                        format, coverage, entries);
            } else if (format == 3) {
                return new ContextualSubtableFormat3(id, sequence, flags,
                        format, coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class ContextualSubtableFormat1 extends ContextualSubtable {
        private RuleSet[] rsa; // rule set array, ordered by glyph coverage
        // index

        ContextualSubtableFormat1(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.rsa != null) {
                final List entries = new ArrayList(1);
                entries.add(this.rsa);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public void resolveLookupReferences(
                final Map/* <String,LookupTable> */lookupTables) {
            GlyphTable.resolveLookupReferences(this.rsa, lookupTables);
        }

        /** {@inheritDoc} */
        @Override
        public RuleLookup[] getLookups(final int ci, final int gi,
                final GlyphPositioningState ps, final int[] rv) {
            assert ps != null;
            assert rv != null && rv.length > 0;
            assert this.rsa != null;
            if (this.rsa.length > 0) {
                final RuleSet rs = this.rsa[0];
                if (rs != null) {
                    final Rule[] ra = rs.getRules();
                    for (final Rule r : ra) {
                        if (r != null && r instanceof ChainedGlyphSequenceRule) {
                            final ChainedGlyphSequenceRule cr = (ChainedGlyphSequenceRule) r;
                            final int[] iga = cr.getGlyphs(gi);
                            if (matches(ps, iga, 0, rv)) {
                                return r.getLookups();
                            }
                        }
                    }
                }
            }
            return null;
        }

        static boolean matches(final GlyphPositioningState ps,
                final int[] glyphs, final int offset, final int[] rv) {
            if (glyphs == null || glyphs.length == 0) {
                return true; // match null or empty glyph sequence
            } else {
                final boolean reverse = offset < 0;
                final GlyphTester ignores = ps.getIgnoreDefault();
                final int[] counts = ps.getGlyphsAvailable(offset, reverse,
                        ignores);
                final int nga = counts[0];
                final int ngm = glyphs.length;
                if (nga < ngm) {
                    return false; // insufficient glyphs available to match
                } else {
                    final int[] ga = ps.getGlyphs(offset, ngm, reverse,
                            ignores, null, counts);
                    for (int k = 0; k < ngm; k++) {
                        if (ga[k] != glyphs[k]) {
                            return false; // match fails at ga [ k ]
                        }
                    }
                    if (rv != null) {
                        rv[0] = counts[0] + counts[1];
                    }
                    return true; // all glyphs match
                }
            }
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 1) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 1 entry");
            } else {
                Object o;
                if ((o = entries.get(0)) == null || !(o instanceof RuleSet[])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first entry must be an RuleSet[], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.rsa = (RuleSet[]) o;
                }
            }
        }
    }

    private static class ContextualSubtableFormat2 extends ContextualSubtable {
        private GlyphClassTable cdt; // class def table
        private int ngc; // class set count
        private RuleSet[] rsa; // rule set array, ordered by class number
        // [0...ngc - 1]

        ContextualSubtableFormat2(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.rsa != null) {
                final List entries = new ArrayList(3);
                entries.add(this.cdt);
                entries.add(Integer.valueOf(this.ngc));
                entries.add(this.rsa);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public void resolveLookupReferences(
                final Map/* <String,LookupTable> */lookupTables) {
            GlyphTable.resolveLookupReferences(this.rsa, lookupTables);
        }

        /** {@inheritDoc} */
        @Override
        public RuleLookup[] getLookups(final int ci, final int gi,
                final GlyphPositioningState ps, final int[] rv) {
            assert ps != null;
            assert rv != null && rv.length > 0;
            assert this.rsa != null;
            if (this.rsa.length > 0) {
                final RuleSet rs = this.rsa[0];
                if (rs != null) {
                    final Rule[] ra = rs.getRules();
                    for (final Rule r : ra) {
                        if (r != null && r instanceof ChainedClassSequenceRule) {
                            final ChainedClassSequenceRule cr = (ChainedClassSequenceRule) r;
                            final int[] ca = cr
                                    .getClasses(this.cdt.getClassIndex(gi,
                                            ps.getClassMatchSet(gi)));
                            if (matches(ps, this.cdt, ca, 0, rv)) {
                                return r.getLookups();
                            }
                        }
                    }
                }
            }
            return null;
        }

        static boolean matches(final GlyphPositioningState ps,
                final GlyphClassTable cdt, final int[] classes,
                final int offset, final int[] rv) {
            if (cdt == null || classes == null || classes.length == 0) {
                return true; // match null class definitions, null or empty
                // class sequence
            } else {
                final boolean reverse = offset < 0;
                final GlyphTester ignores = ps.getIgnoreDefault();
                final int[] counts = ps.getGlyphsAvailable(offset, reverse,
                        ignores);
                final int nga = counts[0];
                final int ngm = classes.length;
                if (nga < ngm) {
                    return false; // insufficient glyphs available to match
                } else {
                    final int[] ga = ps.getGlyphs(offset, ngm, reverse,
                            ignores, null, counts);
                    for (int k = 0; k < ngm; k++) {
                        final int gi = ga[k];
                        final int ms = ps.getClassMatchSet(gi);
                        final int gc = cdt.getClassIndex(gi, ms);
                        if (gc < 0 || gc >= cdt.getClassSize(ms)) {
                            return false; // none or invalid class fails mat ch
                        } else if (gc != classes[k]) {
                            return false; // match fails at ga [ k ]
                        }
                    }
                    if (rv != null) {
                        rv[0] = counts[0] + counts[1];
                    }
                    return true; // all glyphs match
                }
            }
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 3) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 3 entries");
            } else {
                Object o;
                if ((o = entries.get(0)) == null
                        || !(o instanceof GlyphClassTable)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first entry must be an GlyphClassTable, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.cdt = (GlyphClassTable) o;
                }
                if ((o = entries.get(1)) == null || !(o instanceof Integer)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, second entry must be an Integer, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.ngc = ((Integer) o).intValue();
                }
                if ((o = entries.get(2)) == null || !(o instanceof RuleSet[])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, third entry must be an RuleSet[], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.rsa = (RuleSet[]) o;
                    if (this.rsa.length != this.ngc) {
                        throw new AdvancedTypographicTableFormatException(
                                "illegal entries, RuleSet[] length is "
                                        + this.rsa.length + ", but expected "
                                        + this.ngc + " glyph classes");
                    }
                }
            }
        }
    }

    private static class ContextualSubtableFormat3 extends ContextualSubtable {
        private RuleSet[] rsa; // rule set array, containing a single rule set

        ContextualSubtableFormat3(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.rsa != null) {
                final List entries = new ArrayList(1);
                entries.add(this.rsa);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public void resolveLookupReferences(
                final Map/* <String,LookupTable> */lookupTables) {
            GlyphTable.resolveLookupReferences(this.rsa, lookupTables);
        }

        /** {@inheritDoc} */
        @Override
        public RuleLookup[] getLookups(final int ci, final int gi,
                final GlyphPositioningState ps, final int[] rv) {
            assert ps != null;
            assert rv != null && rv.length > 0;
            assert this.rsa != null;
            if (this.rsa.length > 0) {
                final RuleSet rs = this.rsa[0];
                if (rs != null) {
                    final Rule[] ra = rs.getRules();
                    for (final Rule r : ra) {
                        if (r != null
                                && r instanceof ChainedCoverageSequenceRule) {
                            final ChainedCoverageSequenceRule cr = (ChainedCoverageSequenceRule) r;
                            final GlyphCoverageTable[] gca = cr.getCoverages();
                            if (matches(ps, gca, 0, rv)) {
                                return r.getLookups();
                            }
                        }
                    }
                }
            }
            return null;
        }

        static boolean matches(final GlyphPositioningState ps,
                final GlyphCoverageTable[] gca, final int offset, final int[] rv) {
            if (gca == null || gca.length == 0) {
                return true; // match null or empty coverage array
            } else {
                final boolean reverse = offset < 0;
                final GlyphTester ignores = ps.getIgnoreDefault();
                final int[] counts = ps.getGlyphsAvailable(offset, reverse,
                        ignores);
                final int nga = counts[0];
                final int ngm = gca.length;
                if (nga < ngm) {
                    return false; // insufficient glyphs available to match
                } else {
                    final int[] ga = ps.getGlyphs(offset, ngm, reverse,
                            ignores, null, counts);
                    for (int k = 0; k < ngm; k++) {
                        final GlyphCoverageTable ct = gca[k];
                        if (ct != null) {
                            if (ct.getCoverageIndex(ga[k]) < 0) {
                                return false; // match fails at ga [ k ]
                            }
                        }
                    }
                    if (rv != null) {
                        rv[0] = counts[0] + counts[1];
                    }
                    return true; // all glyphs match
                }
            }
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 1) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 1 entry");
            } else {
                Object o;
                if ((o = entries.get(0)) == null || !(o instanceof RuleSet[])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first entry must be an RuleSet[], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.rsa = (RuleSet[]) o;
                }
            }
        }
    }

    private abstract static class ChainedContextualSubtable extends
    GlyphPositioningSubtable {
        ChainedContextualSubtable(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GPOS_LOOKUP_TYPE_CHAINED_CONTEXTUAL;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof ChainedContextualSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean position(final GlyphPositioningState ps) {
            boolean applied = false;
            final int gi = ps.getGlyph();
            int ci;
            if ((ci = getCoverageIndex(gi)) >= 0) {
                final int[] rv = new int[1];
                final RuleLookup[] la = getLookups(ci, gi, ps, rv);
                if (la != null) {
                    ps.apply(la, rv[0]);
                    applied = true;
                }
            }
            return applied;
        }

        /**
         * Obtain rule lookups set associated current input glyph context.
         *
         * @param ci
         *            coverage index of glyph at current position
         * @param gi
         *            glyph index of glyph at current position
         * @param ps
         *            glyph positioning state
         * @param rv
         *            array of ints used to receive multiple return values, must
         *            be of length 1 or greater, where the first entry is used
         *            to return the input sequence length of the matched rule
         * @return array of rule lookups or null if none applies
         */
        public abstract RuleLookup[] getLookups(final int ci, final int gi,
                final GlyphPositioningState ps, final int[] rv);

        static GlyphPositioningSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new ChainedContextualSubtableFormat1(id, sequence,
                        flags, format, coverage, entries);
            } else if (format == 2) {
                return new ChainedContextualSubtableFormat2(id, sequence,
                        flags, format, coverage, entries);
            } else if (format == 3) {
                return new ChainedContextualSubtableFormat3(id, sequence,
                        flags, format, coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class ChainedContextualSubtableFormat1 extends
    ChainedContextualSubtable {
        private RuleSet[] rsa; // rule set array, ordered by glyph coverage
        // index

        ChainedContextualSubtableFormat1(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.rsa != null) {
                final List entries = new ArrayList(1);
                entries.add(this.rsa);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public void resolveLookupReferences(
                final Map/* <String,LookupTable> */lookupTables) {
            GlyphTable.resolveLookupReferences(this.rsa, lookupTables);
        }

        /** {@inheritDoc} */
        @Override
        public RuleLookup[] getLookups(final int ci, final int gi,
                final GlyphPositioningState ps, final int[] rv) {
            assert ps != null;
            assert rv != null && rv.length > 0;
            assert this.rsa != null;
            if (this.rsa.length > 0) {
                final RuleSet rs = this.rsa[0];
                if (rs != null) {
                    final Rule[] ra = rs.getRules();
                    for (final Rule r : ra) {
                        if (r != null && r instanceof ChainedGlyphSequenceRule) {
                            final ChainedGlyphSequenceRule cr = (ChainedGlyphSequenceRule) r;
                            final int[] iga = cr.getGlyphs(gi);
                            if (matches(ps, iga, 0, rv)) {
                                final int[] bga = cr.getBacktrackGlyphs();
                                if (matches(ps, bga, -1, null)) {
                                    final int[] lga = cr.getLookaheadGlyphs();
                                    if (matches(ps, lga, rv[0], null)) {
                                        return r.getLookups();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

        private boolean matches(final GlyphPositioningState ps,
                final int[] glyphs, final int offset, final int[] rv) {
            return ContextualSubtableFormat1.matches(ps, glyphs, offset, rv);
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 1) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 1 entry");
            } else {
                Object o;
                if ((o = entries.get(0)) == null || !(o instanceof RuleSet[])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first entry must be an RuleSet[], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.rsa = (RuleSet[]) o;
                }
            }
        }
    }

    private static class ChainedContextualSubtableFormat2 extends
    ChainedContextualSubtable {
        private GlyphClassTable icdt; // input class def table
        private GlyphClassTable bcdt; // backtrack class def table
        private GlyphClassTable lcdt; // lookahead class def table
        private int ngc; // class set count
        private RuleSet[] rsa; // rule set array, ordered by class number
        // [0...ngc - 1]

        ChainedContextualSubtableFormat2(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.rsa != null) {
                final List entries = new ArrayList(5);
                entries.add(this.icdt);
                entries.add(this.bcdt);
                entries.add(this.lcdt);
                entries.add(Integer.valueOf(this.ngc));
                entries.add(this.rsa);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public void resolveLookupReferences(
                final Map/* <String,LookupTable> */lookupTables) {
            GlyphTable.resolveLookupReferences(this.rsa, lookupTables);
        }

        /** {@inheritDoc} */
        @Override
        public RuleLookup[] getLookups(final int ci, final int gi,
                final GlyphPositioningState ps, final int[] rv) {
            assert ps != null;
            assert rv != null && rv.length > 0;
            assert this.rsa != null;
            if (this.rsa.length > 0) {
                final RuleSet rs = this.rsa[0];
                if (rs != null) {
                    final Rule[] ra = rs.getRules();
                    for (final Rule r : ra) {
                        if (r != null && r instanceof ChainedClassSequenceRule) {
                            final ChainedClassSequenceRule cr = (ChainedClassSequenceRule) r;
                            final int[] ica = cr
                                    .getClasses(this.icdt.getClassIndex(gi,
                                            ps.getClassMatchSet(gi)));
                            if (matches(ps, this.icdt, ica, 0, rv)) {
                                final int[] bca = cr.getBacktrackClasses();
                                if (matches(ps, this.bcdt, bca, -1, null)) {
                                    final int[] lca = cr.getLookaheadClasses();
                                    if (matches(ps, this.lcdt, lca, rv[0], null)) {
                                        return r.getLookups();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

        private boolean matches(final GlyphPositioningState ps,
                final GlyphClassTable cdt, final int[] classes,
                final int offset, final int[] rv) {
            return ContextualSubtableFormat2.matches(ps, cdt, classes, offset,
                    rv);
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 5) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 5 entries");
            } else {
                Object o;
                if ((o = entries.get(0)) == null
                        || !(o instanceof GlyphClassTable)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first entry must be an GlyphClassTable, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.icdt = (GlyphClassTable) o;
                }
                if ((o = entries.get(1)) != null
                        && !(o instanceof GlyphClassTable)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, second entry must be an GlyphClassTable, but is: "
                                    + o.getClass());
                } else {
                    this.bcdt = (GlyphClassTable) o;
                }
                if ((o = entries.get(2)) != null
                        && !(o instanceof GlyphClassTable)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, third entry must be an GlyphClassTable, but is: "
                                    + o.getClass());
                } else {
                    this.lcdt = (GlyphClassTable) o;
                }
                if ((o = entries.get(3)) == null || !(o instanceof Integer)) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, fourth entry must be an Integer, but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.ngc = ((Integer) o).intValue();
                }
                if ((o = entries.get(4)) == null || !(o instanceof RuleSet[])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, fifth entry must be an RuleSet[], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.rsa = (RuleSet[]) o;
                    if (this.rsa.length != this.ngc) {
                        throw new AdvancedTypographicTableFormatException(
                                "illegal entries, RuleSet[] length is "
                                        + this.rsa.length + ", but expected "
                                        + this.ngc + " glyph classes");
                    }
                }
            }
        }
    }

    private static class ChainedContextualSubtableFormat3 extends
    ChainedContextualSubtable {
        private RuleSet[] rsa; // rule set array, containing a single rule set

        ChainedContextualSubtableFormat3(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.rsa != null) {
                final List entries = new ArrayList(1);
                entries.add(this.rsa);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public void resolveLookupReferences(
                final Map/* <String,LookupTable> */lookupTables) {
            GlyphTable.resolveLookupReferences(this.rsa, lookupTables);
        }

        /** {@inheritDoc} */
        @Override
        public RuleLookup[] getLookups(final int ci, final int gi,
                final GlyphPositioningState ps, final int[] rv) {
            assert ps != null;
            assert rv != null && rv.length > 0;
            assert this.rsa != null;
            if (this.rsa.length > 0) {
                final RuleSet rs = this.rsa[0];
                if (rs != null) {
                    final Rule[] ra = rs.getRules();
                    for (final Rule r : ra) {
                        if (r != null
                                && r instanceof ChainedCoverageSequenceRule) {
                            final ChainedCoverageSequenceRule cr = (ChainedCoverageSequenceRule) r;
                            final GlyphCoverageTable[] igca = cr.getCoverages();
                            if (matches(ps, igca, 0, rv)) {
                                final GlyphCoverageTable[] bgca = cr
                                        .getBacktrackCoverages();
                                if (matches(ps, bgca, -1, null)) {
                                    final GlyphCoverageTable[] lgca = cr
                                            .getLookaheadCoverages();
                                    if (matches(ps, lgca, rv[0], null)) {
                                        return r.getLookups();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

        private boolean matches(final GlyphPositioningState ps,
                final GlyphCoverageTable[] gca, final int offset, final int[] rv) {
            return ContextualSubtableFormat3.matches(ps, gca, offset, rv);
        }

        private void populate(final List entries) {
            if (entries == null) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null");
            } else if (entries.size() != 1) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, " + entries.size()
                        + " entries present, but requires 1 entry");
            } else {
                Object o;
                if ((o = entries.get(0)) == null || !(o instanceof RuleSet[])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first entry must be an RuleSet[], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.rsa = (RuleSet[]) o;
                }
            }
        }
    }

    /**
     * The <code>DeviceTable</code> class implements a positioning device table
     * record, comprising adjustments to be made to scaled design units
     * according to the scaled size.
     */
    public static class DeviceTable {

        private final int startSize;
        private final int endSize;
        private final int[] deltas;

        /**
         * Instantiate a DeviceTable.
         *
         * @param startSize
         *            the
         * @param endSize
         *            the ending (scaled) size
         * @param deltas
         *            adjustments for each scaled size
         */
        public DeviceTable(final int startSize, final int endSize,
                final int[] deltas) {
            assert startSize >= 0;
            assert startSize <= endSize;
            assert deltas != null;
            assert deltas.length == endSize - startSize + 1;
            this.startSize = startSize;
            this.endSize = endSize;
            this.deltas = deltas;
        }

        /** @return the start size */
        public int getStartSize() {
            return this.startSize;
        }

        /** @return the end size */
        public int getEndSize() {
            return this.endSize;
        }

        /** @return the deltas */
        public int[] getDeltas() {
            return this.deltas;
        }

        /**
         * Find device adjustment.
         *
         * @param fontSize
         *            the font size to search for
         * @return an adjustment if font size matches an entry
         */
        public int findAdjustment(final int fontSize) {
            // [TODO] at present, assumes that 1 device unit equals one point
            final int fs = fontSize / 1000;
            if (fs < this.startSize) {
                return 0;
            } else if (fs <= this.endSize) {
                return this.deltas[fs - this.startSize] * 1000;
            } else {
                return 0;
            }
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "{ start = " + this.startSize + ", end = " + this.endSize
                    + ", deltas = " + Arrays.toString(this.deltas) + "}";
        }

    }

    /**
     * The <code>Value</code> class implements a positioning value record,
     * comprising placement and advancement information in X and Y axes, and
     * optionally including device data used to perform device (grid-fitted)
     * specific fine grain adjustments.
     */
    public static class Value {

        /** X_PLACEMENT value format flag */
        public static final int X_PLACEMENT = 0x0001;
        /** Y_PLACEMENT value format flag */
        public static final int Y_PLACEMENT = 0x0002;
        /** X_ADVANCE value format flag */
        public static final int X_ADVANCE = 0x0004;
        /** Y_ADVANCE value format flag */
        public static final int Y_ADVANCE = 0x0008;
        /** X_PLACEMENT_DEVICE value format flag */
        public static final int X_PLACEMENT_DEVICE = 0x0010;
        /** Y_PLACEMENT_DEVICE value format flag */
        public static final int Y_PLACEMENT_DEVICE = 0x0020;
        /** X_ADVANCE_DEVICE value format flag */
        public static final int X_ADVANCE_DEVICE = 0x0040;
        /** Y_ADVANCE_DEVICE value format flag */
        public static final int Y_ADVANCE_DEVICE = 0x0080;

        /** X_PLACEMENT value index (within adjustments arrays) */
        public static final int IDX_X_PLACEMENT = 0;
        /** Y_PLACEMENT value index (within adjustments arrays) */
        public static final int IDX_Y_PLACEMENT = 1;
        /** X_ADVANCE value index (within adjustments arrays) */
        public static final int IDX_X_ADVANCE = 2;
        /** Y_ADVANCE value index (within adjustments arrays) */
        public static final int IDX_Y_ADVANCE = 3;

        private int xPlacement; // x placement
        private int yPlacement; // y placement
        private int xAdvance; // x advance
        private int yAdvance; // y advance
        private final DeviceTable xPlaDevice; // x placement device table
        private final DeviceTable yPlaDevice; // y placement device table
        private final DeviceTable xAdvDevice; // x advance device table
        private final DeviceTable yAdvDevice; // x advance device table

        /**
         * Instantiate a Value.
         *
         * @param xPlacement
         *            the x placement or zero
         * @param yPlacement
         *            the y placement or zero
         * @param xAdvance
         *            the x advance or zero
         * @param yAdvance
         *            the y advance or zero
         * @param xPlaDevice
         *            the x placement device table or null
         * @param yPlaDevice
         *            the y placement device table or null
         * @param xAdvDevice
         *            the x advance device table or null
         * @param yAdvDevice
         *            the y advance device table or null
         */
        public Value(final int xPlacement, final int yPlacement,
                final int xAdvance, final int yAdvance,
                final DeviceTable xPlaDevice, final DeviceTable yPlaDevice,
                final DeviceTable xAdvDevice, final DeviceTable yAdvDevice) {
            this.xPlacement = xPlacement;
            this.yPlacement = yPlacement;
            this.xAdvance = xAdvance;
            this.yAdvance = yAdvance;
            this.xPlaDevice = xPlaDevice;
            this.yPlaDevice = yPlaDevice;
            this.xAdvDevice = xAdvDevice;
            this.yAdvDevice = yAdvDevice;
        }

        /** @return the x placement */
        public int getXPlacement() {
            return this.xPlacement;
        }

        /** @return the y placement */
        public int getYPlacement() {
            return this.yPlacement;
        }

        /** @return the x advance */
        public int getXAdvance() {
            return this.xAdvance;
        }

        /** @return the y advance */
        public int getYAdvance() {
            return this.yAdvance;
        }

        /** @return the x placement device table */
        public DeviceTable getXPlaDevice() {
            return this.xPlaDevice;
        }

        /** @return the y placement device table */
        public DeviceTable getYPlaDevice() {
            return this.yPlaDevice;
        }

        /** @return the x advance device table */
        public DeviceTable getXAdvDevice() {
            return this.xAdvDevice;
        }

        /** @return the y advance device table */
        public DeviceTable getYAdvDevice() {
            return this.yAdvDevice;
        }

        /**
         * Apply value to specific adjustments to without use of device table
         * adjustments.
         *
         * @param xPlacement
         *            the x placement or zero
         * @param yPlacement
         *            the y placement or zero
         * @param xAdvance
         *            the x advance or zero
         * @param yAdvance
         *            the y advance or zero
         */
        public void adjust(final int xPlacement, final int yPlacement,
                final int xAdvance, final int yAdvance) {
            this.xPlacement += xPlacement;
            this.yPlacement += yPlacement;
            this.xAdvance += xAdvance;
            this.yAdvance += yAdvance;
        }

        /**
         * Apply value to adjustments using font size for device table
         * adjustments.
         *
         * @param adjustments
         *            array of four integers containing X,Y placement and X,Y
         *            advance adjustments
         * @param fontSize
         *            font size for device table adjustments
         * @return true if some adjustment was made
         */
        public boolean adjust(final int[] adjustments, final int fontSize) {
            boolean adjust = false;
            int dv;
            if ((dv = this.xPlacement) != 0) {
                adjustments[IDX_X_PLACEMENT] += dv;
                adjust = true;
            }
            if ((dv = this.yPlacement) != 0) {
                adjustments[IDX_Y_PLACEMENT] += dv;
                adjust = true;
            }
            if ((dv = this.xAdvance) != 0) {
                adjustments[IDX_X_ADVANCE] += dv;
                adjust = true;
            }
            if ((dv = this.yAdvance) != 0) {
                adjustments[IDX_Y_ADVANCE] += dv;
                adjust = true;
            }
            if (fontSize != 0) {
                DeviceTable dt;
                if ((dt = this.xPlaDevice) != null) {
                    if ((dv = dt.findAdjustment(fontSize)) != 0) {
                        adjustments[IDX_X_PLACEMENT] += dv;
                        adjust = true;
                    }
                }
                if ((dt = this.yPlaDevice) != null) {
                    if ((dv = dt.findAdjustment(fontSize)) != 0) {
                        adjustments[IDX_Y_PLACEMENT] += dv;
                        adjust = true;
                    }
                }
                if ((dt = this.xAdvDevice) != null) {
                    if ((dv = dt.findAdjustment(fontSize)) != 0) {
                        adjustments[IDX_X_ADVANCE] += dv;
                        adjust = true;
                    }
                }
                if ((dt = this.yAdvDevice) != null) {
                    if ((dv = dt.findAdjustment(fontSize)) != 0) {
                        adjustments[IDX_Y_ADVANCE] += dv;
                        adjust = true;
                    }
                }
            }
            return adjust;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            boolean first = true;
            sb.append("{ ");
            if (this.xPlacement != 0) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append("xPlacement = " + this.xPlacement);
            }
            if (this.yPlacement != 0) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append("yPlacement = " + this.yPlacement);
            }
            if (this.xAdvance != 0) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append("xAdvance = " + this.xAdvance);
            }
            if (this.yAdvance != 0) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append("yAdvance = " + this.yAdvance);
            }
            if (this.xPlaDevice != null) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append("xPlaDevice = " + this.xPlaDevice);
            }
            if (this.yPlaDevice != null) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append("xPlaDevice = " + this.yPlaDevice);
            }
            if (this.xAdvDevice != null) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append("xAdvDevice = " + this.xAdvDevice);
            }
            if (this.yAdvDevice != null) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append("xAdvDevice = " + this.yAdvDevice);
            }
            sb.append(" }");
            return sb.toString();
        }

    }

    /**
     * The <code>PairValues</code> class implements a pair value record,
     * comprising a glyph id (or zero) and two optional positioning values.
     */
    public static class PairValues {

        private final int glyph; // glyph id (or 0)
        private final Value value1; // value for first glyph in pair (or null)
        private final Value value2; // value for second glyph in pair (or null)

        /**
         * Instantiate a PairValues.
         *
         * @param glyph
         *            the glyph id (or zero)
         * @param value1
         *            the value of the first glyph in pair (or null)
         * @param value2
         *            the value of the second glyph in pair (or null)
         */
        public PairValues(final int glyph, final Value value1,
                final Value value2) {
            assert glyph >= 0;
            this.glyph = glyph;
            this.value1 = value1;
            this.value2 = value2;
        }

        /** @return the glyph id */
        public int getGlyph() {
            return this.glyph;
        }

        /** @return the first value */
        public Value getValue1() {
            return this.value1;
        }

        /** @return the second value */
        public Value getValue2() {
            return this.value2;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            boolean first = true;
            sb.append("{ ");
            if (this.glyph != 0) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append("glyph = " + this.glyph);
            }
            if (this.value1 != null) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append("value1 = " + this.value1);
            }
            if (this.value2 != null) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append("value2 = " + this.value2);
            }
            sb.append(" }");
            return sb.toString();
        }

    }

    /**
     * The <code>Anchor</code> class implements a anchor record, comprising an
     * X,Y coordinate pair, an optional anchor point index (or -1), and optional
     * X or Y device tables (or null if absent).
     */
    public static class Anchor {

        private final int x; // xCoordinate (in design units)
        private final int y; // yCoordinate (in design units)
        private final int anchorPoint; // anchor point index (or -1)
        private final DeviceTable xDevice; // x device table
        private final DeviceTable yDevice; // y device table

        /**
         * Instantiate an Anchor (format 1).
         *
         * @param x
         *            the x coordinate
         * @param y
         *            the y coordinate
         */
        public Anchor(final int x, final int y) {
            this(x, y, -1, null, null);
        }

        /**
         * Instantiate an Anchor (format 2).
         *
         * @param x
         *            the x coordinate
         * @param y
         *            the y coordinate
         * @param anchorPoint
         *            anchor index (or -1)
         */
        public Anchor(final int x, final int y, final int anchorPoint) {
            this(x, y, anchorPoint, null, null);
        }

        /**
         * Instantiate an Anchor (format 3).
         *
         * @param x
         *            the x coordinate
         * @param y
         *            the y coordinate
         * @param xDevice
         *            the x device table (or null if not present)
         * @param yDevice
         *            the y device table (or null if not present)
         */
        public Anchor(final int x, final int y, final DeviceTable xDevice,
                final DeviceTable yDevice) {
            this(x, y, -1, xDevice, yDevice);
        }

        /**
         * Instantiate an Anchor based on an existing anchor.
         *
         * @param a
         *            the existing anchor
         */
        protected Anchor(final Anchor a) {
            this(a.x, a.y, a.anchorPoint, a.xDevice, a.yDevice);
        }

        private Anchor(final int x, final int y, final int anchorPoint,
                final DeviceTable xDevice, final DeviceTable yDevice) {
            assert anchorPoint >= 0 || anchorPoint == -1;
            this.x = x;
            this.y = y;
            this.anchorPoint = anchorPoint;
            this.xDevice = xDevice;
            this.yDevice = yDevice;
        }

        /** @return the x coordinate */
        public int getX() {
            return this.x;
        }

        /** @return the y coordinate */
        public int getY() {
            return this.y;
        }

        /** @return the anchor point index (or -1 if not specified) */
        public int getAnchorPoint() {
            return this.anchorPoint;
        }

        /** @return the x device table (or null if not specified) */
        public DeviceTable getXDevice() {
            return this.xDevice;
        }

        /** @return the y device table (or null if not specified) */
        public DeviceTable getYDevice() {
            return this.yDevice;
        }

        /**
         * Obtain adjustment value required to align the specified anchor with
         * this anchor.
         *
         * @param a
         *            the anchor to align
         * @return the adjustment value needed to effect alignment
         */
        public Value getAlignmentAdjustment(final Anchor a) {
            assert a != null;
            // TODO - handle anchor point
            // TODO - handle device tables
            return new Value(this.x - a.x, this.y - a.y, 0, 0, null, null,
                    null, null);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append("{ [" + this.x + "," + this.y + "]");
            if (this.anchorPoint != -1) {
                sb.append(", anchorPoint = " + this.anchorPoint);
            }
            if (this.xDevice != null) {
                sb.append(", xDevice = " + this.xDevice);
            }
            if (this.yDevice != null) {
                sb.append(", yDevice = " + this.yDevice);
            }
            sb.append(" }");
            return sb.toString();
        }

    }

    /**
     * The <code>MarkAnchor</code> class is a subclass of the
     * <code>Anchor</code> class, adding a mark class designation.
     */
    public static class MarkAnchor extends Anchor {

        private final int markClass; // mark class

        /**
         * Instantiate a MarkAnchor
         *
         * @param markClass
         *            the mark class
         * @param a
         *            the underlying anchor (whose fields are copied)
         */
        public MarkAnchor(final int markClass, final Anchor a) {
            super(a);
            this.markClass = markClass;
        }

        /** @return the mark class */
        public int getMarkClass() {
            return this.markClass;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "{ markClass = " + this.markClass + ", anchor = "
                    + super.toString() + " }";
        }

    }

}
