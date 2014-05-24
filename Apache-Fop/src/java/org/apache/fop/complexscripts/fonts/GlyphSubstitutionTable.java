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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.fop.complexscripts.scripts.ScriptProcessor;
import org.apache.fop.complexscripts.util.GlyphSequence;
import org.apache.fop.complexscripts.util.GlyphTester;

// CSOFF: InnerAssignmentCheck
// CSOFF: LineLengthCheck
// CSOFF: NoWhitespaceAfterCheck

/**
 * <p>
 * The <code>GlyphSubstitutionTable</code> class is a glyph table that
 * implements <code>GlyphSubstitution</code> functionality.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
public class GlyphSubstitutionTable extends GlyphTable {

    /** single substitution subtable type */
    public static final int GSUB_LOOKUP_TYPE_SINGLE = 1;
    /** multiple substitution subtable type */
    public static final int GSUB_LOOKUP_TYPE_MULTIPLE = 2;
    /** alternate substitution subtable type */
    public static final int GSUB_LOOKUP_TYPE_ALTERNATE = 3;
    /** ligature substitution subtable type */
    public static final int GSUB_LOOKUP_TYPE_LIGATURE = 4;
    /** contextual substitution subtable type */
    public static final int GSUB_LOOKUP_TYPE_CONTEXTUAL = 5;
    /** chained contextual substitution subtable type */
    public static final int GSUB_LOOKUP_TYPE_CHAINED_CONTEXTUAL = 6;
    /** extension substitution substitution subtable type */
    public static final int GSUB_LOOKUP_TYPE_EXTENSION_SUBSTITUTION = 7;
    /** reverse chained contextual single substitution subtable type */
    public static final int GSUB_LOOKUP_TYPE_REVERSE_CHAINED_SINGLE = 8;

    /**
     * Instantiate a <code>GlyphSubstitutionTable</code> object using the
     * specified lookups and subtables.
     *
     * @param gdef
     *            glyph definition table that applies
     * @param lookups
     *            a map of lookup specifications to subtable identifier strings
     * @param subtables
     *            a list of identified subtables
     */
    public GlyphSubstitutionTable(final GlyphDefinitionTable gdef,
            final Map lookups, final List subtables) {
        super(gdef, lookups);
        if (subtables == null || subtables.size() == 0) {
            throw new AdvancedTypographicTableFormatException(
                    "subtables must be non-empty");
        } else {
            for (final Iterator it = subtables.iterator(); it.hasNext();) {
                final Object o = it.next();
                if (o instanceof GlyphSubstitutionSubtable) {
                    addSubtable((GlyphSubtable) o);
                } else {
                    throw new AdvancedTypographicTableFormatException(
                            "subtable must be a glyph substitution subtable");
                }
            }
            freezeSubtables();
        }
    }

    /**
     * Perform substitution processing using all matching lookups.
     *
     * @param gs
     *            an input glyph sequence
     * @param script
     *            a script identifier
     * @param language
     *            a language identifier
     * @return the substituted (output) glyph sequence
     */
    public GlyphSequence substitute(final GlyphSequence gs,
            final String script, final String language) {
        GlyphSequence ogs;
        final Map/* <LookupSpec,List<LookupTable>> */lookups = matchLookups(
                script, language, "*");
        if (lookups != null && lookups.size() > 0) {
            final ScriptProcessor sp = ScriptProcessor.getInstance(script);
            ogs = sp.substitute(this, gs, script, language, lookups);
        } else {
            ogs = gs;
        }
        return ogs;
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
            t = GSUB_LOOKUP_TYPE_SINGLE;
        } else if ("multiple".equals(s)) {
            t = GSUB_LOOKUP_TYPE_MULTIPLE;
        } else if ("alternate".equals(s)) {
            t = GSUB_LOOKUP_TYPE_ALTERNATE;
        } else if ("ligature".equals(s)) {
            t = GSUB_LOOKUP_TYPE_LIGATURE;
        } else if ("contextual".equals(s)) {
            t = GSUB_LOOKUP_TYPE_CONTEXTUAL;
        } else if ("chainedcontextual".equals(s)) {
            t = GSUB_LOOKUP_TYPE_CHAINED_CONTEXTUAL;
        } else if ("extensionsubstitution".equals(s)) {
            t = GSUB_LOOKUP_TYPE_EXTENSION_SUBSTITUTION;
        } else if ("reversechainiingcontextualsingle".equals(s)) {
            t = GSUB_LOOKUP_TYPE_REVERSE_CHAINED_SINGLE;
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
        String tn = null;
        switch (type) {
        case GSUB_LOOKUP_TYPE_SINGLE:
            tn = "single";
            break;
        case GSUB_LOOKUP_TYPE_MULTIPLE:
            tn = "multiple";
            break;
        case GSUB_LOOKUP_TYPE_ALTERNATE:
            tn = "alternate";
            break;
        case GSUB_LOOKUP_TYPE_LIGATURE:
            tn = "ligature";
            break;
        case GSUB_LOOKUP_TYPE_CONTEXTUAL:
            tn = "contextual";
            break;
        case GSUB_LOOKUP_TYPE_CHAINED_CONTEXTUAL:
            tn = "chainedcontextual";
            break;
        case GSUB_LOOKUP_TYPE_EXTENSION_SUBSTITUTION:
            tn = "extensionsubstitution";
            break;
        case GSUB_LOOKUP_TYPE_REVERSE_CHAINED_SINGLE:
            tn = "reversechainiingcontextualsingle";
            break;
        default:
            tn = "unknown";
            break;
        }
        return tn;
    }

    /**
     * Create a substitution subtable according to the specified arguments.
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
        case GSUB_LOOKUP_TYPE_SINGLE:
            st = SingleSubtable.create(id, sequence, flags, format, coverage,
                    entries);
            break;
        case GSUB_LOOKUP_TYPE_MULTIPLE:
            st = MultipleSubtable.create(id, sequence, flags, format, coverage,
                    entries);
            break;
        case GSUB_LOOKUP_TYPE_ALTERNATE:
            st = AlternateSubtable.create(id, sequence, flags, format,
                    coverage, entries);
            break;
        case GSUB_LOOKUP_TYPE_LIGATURE:
            st = LigatureSubtable.create(id, sequence, flags, format, coverage,
                    entries);
            break;
        case GSUB_LOOKUP_TYPE_CONTEXTUAL:
            st = ContextualSubtable.create(id, sequence, flags, format,
                    coverage, entries);
            break;
        case GSUB_LOOKUP_TYPE_CHAINED_CONTEXTUAL:
            st = ChainedContextualSubtable.create(id, sequence, flags, format,
                    coverage, entries);
            break;
        case GSUB_LOOKUP_TYPE_REVERSE_CHAINED_SINGLE:
            st = ReverseChainedSingleSubtable.create(id, sequence, flags,
                    format, coverage, entries);
            break;
        default:
            break;
        }
        return st;
    }

    /**
     * Create a substitution subtable according to the specified arguments.
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

    private abstract static class SingleSubtable extends
    GlyphSubstitutionSubtable {
        SingleSubtable(final String id, final int sequence, final int flags,
                final int format, final GlyphCoverageTable coverage,
                final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GSUB_LOOKUP_TYPE_SINGLE;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof SingleSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean substitute(final GlyphSubstitutionState ss) {
            final int gi = ss.getGlyph();
            int ci;
            if ((ci = getCoverageIndex(gi)) < 0) {
                return false;
            } else {
                int go = getGlyphForCoverageIndex(ci, gi);
                if (go < 0 || go > 65535) {
                    go = 65535;
                }
                ss.putGlyph(go, ss.getAssociation(), Boolean.TRUE);
                ss.consume(1);
                return true;
            }
        }

        /**
         * Obtain glyph for coverage index.
         *
         * @param ci
         *            coverage index
         * @param gi
         *            original glyph index
         * @return substituted glyph value
         * @throws IllegalArgumentException
         *             if coverage index is not valid
         */
        public abstract int getGlyphForCoverageIndex(final int ci, final int gi)
                throws IllegalArgumentException;

        static GlyphSubstitutionSubtable create(final String id,
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
        private int delta;
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
            final List entries = new ArrayList(1);
            entries.add(Integer.valueOf(this.delta));
            return entries;
        }

        /** {@inheritDoc} */
        @Override
        public int getGlyphForCoverageIndex(final int ci, final int gi)
                throws IllegalArgumentException {
            if (ci <= this.ciMax) {
                return gi + this.delta;
            } else {
                throw new IllegalArgumentException("coverage index " + ci
                        + " out of range, maximum coverage index is "
                        + this.ciMax);
            }
        }

        private void populate(final List entries) {
            if (entries == null || entries.size() != 1) {
                throw new AdvancedTypographicTableFormatException(
                        "illegal entries, must be non-null and contain exactly one entry");
            } else {
                final Object o = entries.get(0);
                int delta = 0;
                if (o instanceof Integer) {
                    delta = ((Integer) o).intValue();
                } else {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries entry, must be Integer, but is: "
                                    + o);
                }
                this.delta = delta;
                this.ciMax = getCoverageSize() - 1;
            }
        }
    }

    private static class SingleSubtableFormat2 extends SingleSubtable {
        private int[] glyphs;

        SingleSubtableFormat2(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            final List entries = new ArrayList(this.glyphs.length);
            for (final int glyph : this.glyphs) {
                entries.add(Integer.valueOf(glyph));
            }
            return entries;
        }

        /** {@inheritDoc} */
        @Override
        public int getGlyphForCoverageIndex(final int ci, final int gi)
                throws IllegalArgumentException {
            if (this.glyphs == null) {
                return -1;
            } else if (ci >= this.glyphs.length) {
                throw new IllegalArgumentException("coverage index " + ci
                        + " out of range, maximum coverage index is "
                        + this.glyphs.length);
            } else {
                return this.glyphs[ci];
            }
        }

        private void populate(final List entries) {
            int i = 0;
            final int n = entries.size();
            final int[] glyphs = new int[n];
            for (final Iterator it = entries.iterator(); it.hasNext();) {
                final Object o = it.next();
                if (o instanceof Integer) {
                    final int gid = ((Integer) o).intValue();
                    if (gid >= 0 && gid < 65536) {
                        glyphs[i++] = gid;
                    } else {
                        throw new AdvancedTypographicTableFormatException(
                                "illegal glyph index: " + gid);
                    }
                } else {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries entry, must be Integer: " + o);
                }
            }
            assert i == n;
            assert this.glyphs == null;
            this.glyphs = glyphs;
        }
    }

    private abstract static class MultipleSubtable extends
    GlyphSubstitutionSubtable {
        public MultipleSubtable(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GSUB_LOOKUP_TYPE_MULTIPLE;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof MultipleSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean substitute(final GlyphSubstitutionState ss) {
            final int gi = ss.getGlyph();
            int ci;
            if ((ci = getCoverageIndex(gi)) < 0) {
                return false;
            } else {
                final int[] ga = getGlyphsForCoverageIndex(ci, gi);
                if (ga != null) {
                    ss.putGlyphs(
                            ga,
                            GlyphSequence.CharAssociation.replicate(
                                    ss.getAssociation(), ga.length),
                                    Boolean.TRUE);
                    ss.consume(1);
                }
                return true;
            }
        }

        /**
         * Obtain glyph sequence for coverage index.
         *
         * @param ci
         *            coverage index
         * @param gi
         *            original glyph index
         * @return sequence of glyphs to substitute for input glyph
         * @throws IllegalArgumentException
         *             if coverage index is not valid
         */
        public abstract int[] getGlyphsForCoverageIndex(final int ci,
                final int gi) throws IllegalArgumentException;

        static GlyphSubstitutionSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new MultipleSubtableFormat1(id, sequence, flags, format,
                        coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class MultipleSubtableFormat1 extends MultipleSubtable {
        private int[][] gsa; // glyph sequence array, ordered by coverage index

        MultipleSubtableFormat1(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            if (this.gsa != null) {
                final List entries = new ArrayList(1);
                entries.add(this.gsa);
                return entries;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int[] getGlyphsForCoverageIndex(final int ci, final int gi)
                throws IllegalArgumentException {
            if (this.gsa == null) {
                return null;
            } else if (ci >= this.gsa.length) {
                throw new IllegalArgumentException("coverage index " + ci
                        + " out of range, maximum coverage index is "
                        + this.gsa.length);
            } else {
                return this.gsa[ci];
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
                if ((o = entries.get(0)) == null || !(o instanceof int[][])) {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries, first entry must be an int[][], but is: "
                                    + (o != null ? o.getClass() : null));
                } else {
                    this.gsa = (int[][]) o;
                }
            }
        }
    }

    private abstract static class AlternateSubtable extends
    GlyphSubstitutionSubtable {
        public AlternateSubtable(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GSUB_LOOKUP_TYPE_ALTERNATE;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof AlternateSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean substitute(final GlyphSubstitutionState ss) {
            final int gi = ss.getGlyph();
            int ci;
            if ((ci = getCoverageIndex(gi)) < 0) {
                return false;
            } else {
                final int[] ga = getAlternatesForCoverageIndex(ci, gi);
                final int ai = ss.getAlternatesIndex(ci);
                int go;
                if (ai < 0 || ai >= ga.length) {
                    go = gi;
                } else {
                    go = ga[ai];
                }
                if (go < 0 || go > 65535) {
                    go = 65535;
                }
                ss.putGlyph(go, ss.getAssociation(), Boolean.TRUE);
                ss.consume(1);
                return true;
            }
        }

        /**
         * Obtain glyph alternates for coverage index.
         *
         * @param ci
         *            coverage index
         * @param gi
         *            original glyph index
         * @return sequence of glyphs to substitute for input glyph
         * @throws IllegalArgumentException
         *             if coverage index is not valid
         */
        public abstract int[] getAlternatesForCoverageIndex(final int ci,
                final int gi) throws IllegalArgumentException;

        static GlyphSubstitutionSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new AlternateSubtableFormat1(id, sequence, flags,
                        format, coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class AlternateSubtableFormat1 extends AlternateSubtable {
        private int[][] gaa; // glyph alternates array, ordered by coverage
        // index

        AlternateSubtableFormat1(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            final List entries = new ArrayList(this.gaa.length);
            for (final int[] element : this.gaa) {
                entries.add(element);
            }
            return entries;
        }

        /** {@inheritDoc} */
        @Override
        public int[] getAlternatesForCoverageIndex(final int ci, final int gi)
                throws IllegalArgumentException {
            if (this.gaa == null) {
                return null;
            } else if (ci >= this.gaa.length) {
                throw new IllegalArgumentException("coverage index " + ci
                        + " out of range, maximum coverage index is "
                        + this.gaa.length);
            } else {
                return this.gaa[ci];
            }
        }

        private void populate(final List entries) {
            int i = 0;
            final int n = entries.size();
            final int[][] gaa = new int[n][];
            for (final Iterator it = entries.iterator(); it.hasNext();) {
                final Object o = it.next();
                if (o instanceof int[]) {
                    gaa[i++] = (int[]) o;
                } else {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal entries entry, must be int[]: " + o);
                }
            }
            assert i == n;
            assert this.gaa == null;
            this.gaa = gaa;
        }
    }

    private abstract static class LigatureSubtable extends
    GlyphSubstitutionSubtable {
        public LigatureSubtable(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GSUB_LOOKUP_TYPE_LIGATURE;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof LigatureSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean substitute(final GlyphSubstitutionState ss) {
            final int gi = ss.getGlyph();
            int ci;
            if ((ci = getCoverageIndex(gi)) < 0) {
                return false;
            } else {
                final LigatureSet ls = getLigatureSetForCoverageIndex(ci, gi);
                if (ls != null) {
                    final boolean reverse = false;
                    final GlyphTester ignores = ss.getIgnoreDefault();
                    final int[] counts = ss.getGlyphsAvailable(0, reverse,
                            ignores);
                    int nga = counts[0];
                    int ngi;
                    if (nga > 1) {
                        final int[] iga = ss.getGlyphs(0, nga, reverse,
                                ignores, null, counts);
                        final Ligature l = findLigature(ls, iga);
                        if (l != null) {
                            int go = l.getLigature();
                            if (go < 0 || go > 65535) {
                                go = 65535;
                            }
                            final int nmg = 1 + l.getNumComponents();
                            // fetch matched number of component glyphs to
                            // determine matched and ignored count
                            ss.getGlyphs(0, nmg, reverse, ignores, null, counts);
                            nga = counts[0];
                            ngi = counts[1];
                            // fetch associations of matched component glyphs
                            final GlyphSequence.CharAssociation[] laa = ss
                                    .getAssociations(0, nga);
                            // output ligature glyph and its association
                            ss.putGlyph(go,
                                    GlyphSequence.CharAssociation.join(laa),
                                    Boolean.TRUE);
                            // fetch and output ignored glyphs (if necessary)
                            if (ngi > 0) {
                                ss.putGlyphs(ss.getIgnoredGlyphs(0, ngi),
                                        ss.getIgnoredAssociations(0, ngi), null);
                            }
                            ss.consume(nga + ngi);
                        }
                    }
                }
                return true;
            }
        }

        private Ligature findLigature(final LigatureSet ls, final int[] glyphs) {
            final Ligature[] la = ls.getLigatures();
            int k = -1;
            int maxComponents = -1;
            for (int i = 0, n = la.length; i < n; i++) {
                final Ligature l = la[i];
                if (l.matchesComponents(glyphs)) {
                    final int nc = l.getNumComponents();
                    if (nc > maxComponents) {
                        maxComponents = nc;
                        k = i;
                    }
                }
            }
            if (k >= 0) {
                return la[k];
            } else {
                return null;
            }
        }

        /**
         * Obtain ligature set for coverage index.
         *
         * @param ci
         *            coverage index
         * @param gi
         *            original glyph index
         * @return ligature set (or null if none defined)
         * @throws IllegalArgumentException
         *             if coverage index is not valid
         */
        public abstract LigatureSet getLigatureSetForCoverageIndex(
                final int ci, final int gi) throws IllegalArgumentException;

        static GlyphSubstitutionSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new LigatureSubtableFormat1(id, sequence, flags, format,
                        coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class LigatureSubtableFormat1 extends LigatureSubtable {
        private LigatureSet[] ligatureSets;

        public LigatureSubtableFormat1(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            final List entries = new ArrayList(this.ligatureSets.length);
            for (final LigatureSet ligatureSet : this.ligatureSets) {
                entries.add(ligatureSet);
            }
            return entries;
        }

        /** {@inheritDoc} */
        @Override
        public LigatureSet getLigatureSetForCoverageIndex(final int ci,
                final int gi) throws IllegalArgumentException {
            if (this.ligatureSets == null) {
                return null;
            } else if (ci >= this.ligatureSets.length) {
                throw new IllegalArgumentException("coverage index " + ci
                        + " out of range, maximum coverage index is "
                        + this.ligatureSets.length);
            } else {
                return this.ligatureSets[ci];
            }
        }

        private void populate(final List entries) {
            int i = 0;
            final int n = entries.size();
            final LigatureSet[] ligatureSets = new LigatureSet[n];
            for (final Iterator it = entries.iterator(); it.hasNext();) {
                final Object o = it.next();
                if (o instanceof LigatureSet) {
                    ligatureSets[i++] = (LigatureSet) o;
                } else {
                    throw new AdvancedTypographicTableFormatException(
                            "illegal ligatures entry, must be LigatureSet: "
                                    + o);
                }
            }
            assert i == n;
            assert this.ligatureSets == null;
            this.ligatureSets = ligatureSets;
        }
    }

    private abstract static class ContextualSubtable extends
    GlyphSubstitutionSubtable {
        public ContextualSubtable(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GSUB_LOOKUP_TYPE_CONTEXTUAL;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof ContextualSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean substitute(final GlyphSubstitutionState ss) {
            final int gi = ss.getGlyph();
            int ci;
            if ((ci = getCoverageIndex(gi)) < 0) {
                return false;
            } else {
                final int[] rv = new int[1];
                final RuleLookup[] la = getLookups(ci, gi, ss, rv);
                if (la != null) {
                    ss.apply(la, rv[0]);
                }
                return true;
            }
        }

        /**
         * Obtain rule lookups set associated current input glyph context.
         *
         * @param ci
         *            coverage index of glyph at current position
         * @param gi
         *            glyph index of glyph at current position
         * @param ss
         *            glyph substitution state
         * @param rv
         *            array of ints used to receive multiple return values, must
         *            be of length 1 or greater, where the first entry is used
         *            to return the input sequence length of the matched rule
         * @return array of rule lookups or null if none applies
         */
        public abstract RuleLookup[] getLookups(final int ci, final int gi,
                final GlyphSubstitutionState ss, final int[] rv);

        static GlyphSubstitutionSubtable create(final String id,
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
                final GlyphSubstitutionState ss, final int[] rv) {
            assert ss != null;
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
                            if (matches(ss, iga, 0, rv)) {
                                return r.getLookups();
                            }
                        }
                    }
                }
            }
            return null;
        }

        static boolean matches(final GlyphSubstitutionState ss,
                final int[] glyphs, final int offset, final int[] rv) {
            if (glyphs == null || glyphs.length == 0) {
                return true; // match null or empty glyph sequence
            } else {
                final boolean reverse = offset < 0;
                final GlyphTester ignores = ss.getIgnoreDefault();
                final int[] counts = ss.getGlyphsAvailable(offset, reverse,
                        ignores);
                final int nga = counts[0];
                final int ngm = glyphs.length;
                if (nga < ngm) {
                    return false; // insufficient glyphs available to match
                } else {
                    final int[] ga = ss.getGlyphs(offset, ngm, reverse,
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
                final GlyphSubstitutionState ss, final int[] rv) {
            assert ss != null;
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
                                            ss.getClassMatchSet(gi)));
                            if (matches(ss, this.cdt, ca, 0, rv)) {
                                return r.getLookups();
                            }
                        }
                    }
                }
            }
            return null;
        }

        static boolean matches(final GlyphSubstitutionState ss,
                final GlyphClassTable cdt, final int[] classes,
                final int offset, final int[] rv) {
            if (cdt == null || classes == null || classes.length == 0) {
                return true; // match null class definitions, null or empty
                // class sequence
            } else {
                final boolean reverse = offset < 0;
                final GlyphTester ignores = ss.getIgnoreDefault();
                final int[] counts = ss.getGlyphsAvailable(offset, reverse,
                        ignores);
                final int nga = counts[0];
                final int ngm = classes.length;
                if (nga < ngm) {
                    return false; // insufficient glyphs available to match
                } else {
                    final int[] ga = ss.getGlyphs(offset, ngm, reverse,
                            ignores, null, counts);
                    for (int k = 0; k < ngm; k++) {
                        final int gi = ga[k];
                        final int ms = ss.getClassMatchSet(gi);
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
                final GlyphSubstitutionState ss, final int[] rv) {
            assert ss != null;
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
                            if (matches(ss, gca, 0, rv)) {
                                return r.getLookups();
                            }
                        }
                    }
                }
            }
            return null;
        }

        static boolean matches(final GlyphSubstitutionState ss,
                final GlyphCoverageTable[] gca, final int offset, final int[] rv) {
            if (gca == null || gca.length == 0) {
                return true; // match null or empty coverage array
            } else {
                final boolean reverse = offset < 0;
                final GlyphTester ignores = ss.getIgnoreDefault();
                final int[] counts = ss.getGlyphsAvailable(offset, reverse,
                        ignores);
                final int nga = counts[0];
                final int ngm = gca.length;
                if (nga < ngm) {
                    return false; // insufficient glyphs available to match
                } else {
                    final int[] ga = ss.getGlyphs(offset, ngm, reverse,
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
    GlyphSubstitutionSubtable {
        public ChainedContextualSubtable(final String id, final int sequence,
                final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GSUB_LOOKUP_TYPE_CHAINED_CONTEXTUAL;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof ChainedContextualSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean substitute(final GlyphSubstitutionState ss) {
            final int gi = ss.getGlyph();
            int ci;
            if ((ci = getCoverageIndex(gi)) < 0) {
                return false;
            } else {
                final int[] rv = new int[1];
                final RuleLookup[] la = getLookups(ci, gi, ss, rv);
                if (la != null) {
                    ss.apply(la, rv[0]);
                    return true;
                } else {
                    return false;
                }
            }
        }

        /**
         * Obtain rule lookups set associated current input glyph context.
         *
         * @param ci
         *            coverage index of glyph at current position
         * @param gi
         *            glyph index of glyph at current position
         * @param ss
         *            glyph substitution state
         * @param rv
         *            array of ints used to receive multiple return values, must
         *            be of length 1 or greater
         * @return array of rule lookups or null if none applies
         */
        public abstract RuleLookup[] getLookups(final int ci, final int gi,
                final GlyphSubstitutionState ss, final int[] rv);

        static GlyphSubstitutionSubtable create(final String id,
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
                final GlyphSubstitutionState ss, final int[] rv) {
            assert ss != null;
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
                            if (matches(ss, iga, 0, rv)) {
                                final int[] bga = cr.getBacktrackGlyphs();
                                if (matches(ss, bga, -1, null)) {
                                    final int[] lga = cr.getLookaheadGlyphs();
                                    if (matches(ss, lga, rv[0], null)) {
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

        private boolean matches(final GlyphSubstitutionState ss,
                final int[] glyphs, final int offset, final int[] rv) {
            return ContextualSubtableFormat1.matches(ss, glyphs, offset, rv);
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
        public RuleLookup[] getLookups(final int ci, final int gi,
                final GlyphSubstitutionState ss, final int[] rv) {
            assert ss != null;
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
                                            ss.getClassMatchSet(gi)));
                            if (matches(ss, this.icdt, ica, 0, rv)) {
                                final int[] bca = cr.getBacktrackClasses();
                                if (matches(ss, this.bcdt, bca, -1, null)) {
                                    final int[] lca = cr.getLookaheadClasses();
                                    if (matches(ss, this.lcdt, lca, rv[0], null)) {
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

        private boolean matches(final GlyphSubstitutionState ss,
                final GlyphClassTable cdt, final int[] classes,
                final int offset, final int[] rv) {
            return ContextualSubtableFormat2.matches(ss, cdt, classes, offset,
                    rv);
        }

        /** {@inheritDoc} */
        @Override
        public void resolveLookupReferences(
                final Map/* <String,LookupTable> */lookupTables) {
            GlyphTable.resolveLookupReferences(this.rsa, lookupTables);
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
                final GlyphSubstitutionState ss, final int[] rv) {
            assert ss != null;
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
                            if (matches(ss, igca, 0, rv)) {
                                final GlyphCoverageTable[] bgca = cr
                                        .getBacktrackCoverages();
                                if (matches(ss, bgca, -1, null)) {
                                    final GlyphCoverageTable[] lgca = cr
                                            .getLookaheadCoverages();
                                    if (matches(ss, lgca, rv[0], null)) {
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

        private boolean matches(final GlyphSubstitutionState ss,
                final GlyphCoverageTable[] gca, final int offset, final int[] rv) {
            return ContextualSubtableFormat3.matches(ss, gca, offset, rv);
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

    private abstract static class ReverseChainedSingleSubtable extends
    GlyphSubstitutionSubtable {
        public ReverseChainedSingleSubtable(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage);
        }

        /** {@inheritDoc} */
        @Override
        public int getType() {
            return GSUB_LOOKUP_TYPE_REVERSE_CHAINED_SINGLE;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCompatible(final GlyphSubtable subtable) {
            return subtable instanceof ReverseChainedSingleSubtable;
        }

        /** {@inheritDoc} */
        @Override
        public boolean usesReverseScan() {
            return true;
        }

        static GlyphSubstitutionSubtable create(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            if (format == 1) {
                return new ReverseChainedSingleSubtableFormat1(id, sequence,
                        flags, format, coverage, entries);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class ReverseChainedSingleSubtableFormat1 extends
    ReverseChainedSingleSubtable {
        ReverseChainedSingleSubtableFormat1(final String id,
                final int sequence, final int flags, final int format,
                final GlyphCoverageTable coverage, final List entries) {
            super(id, sequence, flags, format, coverage, entries);
            populate(entries);
        }

        /** {@inheritDoc} */
        @Override
        public List getEntries() {
            return null;
        }

        private void populate(final List entries) {
        }
    }

    /**
     * The <code>Ligature</code> class implements a ligature lookup result in
     * terms of a ligature glyph (code) and the <emph>N+1...</emph> components
     * that comprise the ligature, where the <emph>Nth</emph> component was
     * consumed in the coverage table lookup mapping to this ligature instance.
     */
    public static class Ligature {

        private final int ligature; // (resulting) ligature glyph
        private final int[] components; // component glyph codes (note that
        // first component is implied)

        /**
         * Instantiate a ligature.
         *
         * @param ligature
         *            glyph id
         * @param components
         *            sequence of <emph>N+1...</emph> component glyph (or
         *            character) identifiers
         */
        public Ligature(final int ligature, final int[] components) {
            if (ligature < 0 || ligature > 65535) {
                throw new AdvancedTypographicTableFormatException(
                        "invalid ligature glyph index: " + ligature);
            } else if (components == null) {
                throw new AdvancedTypographicTableFormatException(
                        "invalid ligature components, must be non-null array");
            } else {
                for (final int gc : components) {
                    if (gc < 0 || gc > 65535) {
                        throw new AdvancedTypographicTableFormatException(
                                "invalid component glyph index: " + gc);
                    }
                }
                this.ligature = ligature;
                this.components = components;
            }
        }

        /** @return ligature glyph id */
        public int getLigature() {
            return this.ligature;
        }

        /** @return array of <emph>N+1...</emph> components */
        public int[] getComponents() {
            return this.components;
        }

        /** @return components count */
        public int getNumComponents() {
            return this.components.length;
        }

        /**
         * Determine if input sequence at offset matches ligature's components.
         *
         * @param glyphs
         *            array of glyph components to match (including first,
         *            implied glyph)
         * @return true if matches
         */
        public boolean matchesComponents(final int[] glyphs) {
            if (glyphs.length < this.components.length + 1) {
                return false;
            } else {
                for (int i = 0, n = this.components.length; i < n; i++) {
                    if (glyphs[i + 1] != this.components[i]) {
                        return false;
                    }
                }
                return true;
            }
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append("{components={");
            for (int i = 0, n = this.components.length; i < n; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(Integer.toString(this.components[i]));
            }
            sb.append("},ligature=");
            sb.append(Integer.toString(this.ligature));
            sb.append("}");
            return sb.toString();
        }

    }

    /**
     * The <code>LigatureSet</code> class implements a set of ligatures.
     */
    public static class LigatureSet {

        private final Ligature[] ligatures; // set of ligatures all of which
        // share the first (implied)
        // component
        private final int maxComponents; // maximum number of components
        // (including first)

        /**
         * Instantiate a set of ligatures.
         *
         * @param ligatures
         *            collection of ligatures
         */
        public LigatureSet(final List ligatures) {
            this((Ligature[]) ligatures.toArray(new Ligature[ligatures.size()]));
        }

        /**
         * Instantiate a set of ligatures.
         *
         * @param ligatures
         *            array of ligatures
         */
        public LigatureSet(final Ligature[] ligatures) {
            if (ligatures == null) {
                throw new AdvancedTypographicTableFormatException(
                        "invalid ligatures, must be non-null array");
            } else {
                this.ligatures = ligatures;
                int ncMax = -1;
                for (final Ligature l : ligatures) {
                    final int nc = l.getNumComponents() + 1;
                    if (nc > ncMax) {
                        ncMax = nc;
                    }
                }
                this.maxComponents = ncMax;
            }
        }

        /** @return array of ligatures in this ligature set */
        public Ligature[] getLigatures() {
            return this.ligatures;
        }

        /** @return count of ligatures in this ligature set */
        public int getNumLigatures() {
            return this.ligatures.length;
        }

        /**
         * @return maximum number of components in one ligature (including first
         *         component)
         */
        public int getMaxComponents() {
            return this.maxComponents;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append("{ligs={");
            for (int i = 0, n = this.ligatures.length; i < n; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(this.ligatures[i]);
            }
            sb.append("}}");
            return sb.toString();
        }

    }

}
