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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.complexscripts.util.GlyphSequence;
import org.apache.fop.complexscripts.util.ScriptContextTester;

// CSOFF: EmptyForIteratorPadCheck
// CSOFF: InnerAssignmentCheck
// CSOFF: LineLengthCheck
// CSOFF: NoWhitespaceAfterCheck
// CSOFF: ParameterNumberCheck
// CSOFF: SimplifyBooleanReturnCheck

/**
 * <p>
 * Base class for all advanced typographic glyph tables.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
@Slf4j
public class GlyphTable {

    /** substitution glyph table type */
    public static final int GLYPH_TABLE_TYPE_SUBSTITUTION = 1;
    /** positioning glyph table type */
    public static final int GLYPH_TABLE_TYPE_POSITIONING = 2;
    /** justification glyph table type */
    public static final int GLYPH_TABLE_TYPE_JUSTIFICATION = 3;
    /** baseline glyph table type */
    public static final int GLYPH_TABLE_TYPE_BASELINE = 4;
    /** definition glyph table type */
    public static final int GLYPH_TABLE_TYPE_DEFINITION = 5;

    // (optional) glyph definition table in table types other than glyph
    // definition table
    private GlyphTable gdef;

    // map from lookup specs to lists of strings, each of which identifies a
    // lookup table (consisting of one or more subtables)
    private Map/* <LookupSpec,List<String>> */lookups;

    // map from lookup identifiers to lookup tables
    private Map/* <String,LookupTable> */lookupTables;

    // if true, then prevent further subtable addition
    private boolean frozen;

    /**
     * Instantiate glyph table with specified lookups.
     *
     * @param gdef
     *            glyph definition table that applies
     * @param lookups
     *            map from lookup specs to lookup tables
     */
    public GlyphTable(final GlyphTable gdef, final Map/*
     * <LookupSpec,List<String>>
     */lookups) {
        if (gdef != null && !(gdef instanceof GlyphDefinitionTable)) {
            throw new AdvancedTypographicTableFormatException(
                    "bad glyph definition table");
        } else if (lookups == null) {
            throw new AdvancedTypographicTableFormatException(
                    "lookups must be non-null map");
        } else {
            this.gdef = gdef;
            this.lookups = lookups;
            this.lookupTables = new LinkedHashMap/* <String,List<LookupTable>> */();
        }
    }

    /**
     * Obtain glyph definition table.
     *
     * @return (possibly null) glyph definition table
     */
    public GlyphDefinitionTable getGlyphDefinitions() {
        return (GlyphDefinitionTable) this.gdef;
    }

    /**
     * Obtain list of all lookup specifications.
     *
     * @return (possibly empty) list of all lookup specifications
     */
    public List/* <LookupSpec> */getLookups() {
        return matchLookupSpecs("*", "*", "*");
    }

    /**
     * Obtain ordered list of all lookup tables, where order is by lookup
     * identifier, which lexicographic ordering follows the lookup list order.
     *
     * @return (possibly empty) ordered list of all lookup tables
     */
    public List/* <LookupTable> */getLookupTables() {
        final TreeSet/* <String> */lids = new TreeSet/* <String> */(
                this.lookupTables.keySet());
        final List/* <LookupTable> */ltl = new ArrayList/* <LookupTable> */(
                lids.size());
        for (final Iterator it = lids.iterator(); it.hasNext();) {
            final String lid = (String) it.next();
            ltl.add(this.lookupTables.get(lid));
        }
        return ltl;
    }

    /**
     * Obtain lookup table by lookup id. This method is used by test code, and
     * provides access to embedded lookups not normally accessed by {script,
     * language, feature} lookup spec.
     *
     * @param lid
     *            lookup id
     * @return table associated with lookup id or null if none
     */
    public LookupTable getLookupTable(final String lid) {
        return (LookupTable) this.lookupTables.get(lid);
    }

    /**
     * Add a subtable.
     *
     * @param subtable
     *            a (non-null) glyph subtable
     */
    protected void addSubtable(final GlyphSubtable subtable) {
        // ensure table is not frozen
        if (this.frozen) {
            throw new IllegalStateException(
                    "glyph table is frozen, subtable addition prohibited");
        }
        // set subtable's table reference to this table
        subtable.setTable(this);
        // add subtable to this table's subtable collection
        final String lid = subtable.getLookupId();
        if (this.lookupTables.containsKey(lid)) {
            final LookupTable lt = (LookupTable) this.lookupTables.get(lid);
            lt.addSubtable(subtable);
        } else {
            final LookupTable lt = new LookupTable(lid, subtable);
            this.lookupTables.put(lid, lt);
        }
    }

    /**
     * Freeze subtables, i.e., do not allow further subtable addition, and
     * create resulting cached state.
     */
    protected void freezeSubtables() {
        if (!this.frozen) {
            for (final Iterator it = this.lookupTables.values().iterator(); it
                    .hasNext();) {
                final LookupTable lt = (LookupTable) it.next();
                lt.freezeSubtables(this.lookupTables);
            }
            this.frozen = true;
        }
    }

    /**
     * Match lookup specifications according to <script,language,feature> tuple,
     * where '*' is a wildcard for a tuple component.
     *
     * @param script
     *            a script identifier
     * @param language
     *            a language identifier
     * @param feature
     *            a feature identifier
     * @return a (possibly empty) array of matching lookup specifications
     */
    public List/* <LookupSpec> */matchLookupSpecs(final String script,
            final String language, final String feature) {
        final Set/* <LookupSpec> */keys = this.lookups.keySet();
        final List/* <LookupSpec> */matches = new ArrayList/* <LookupSpec> */();
        for (final Iterator it = keys.iterator(); it.hasNext();) {
            final LookupSpec ls = (LookupSpec) it.next();
            if (!"*".equals(script)) {
                if (!ls.getScript().equals(script)) {
                    continue;
                }
            }
            if (!"*".equals(language)) {
                if (!ls.getLanguage().equals(language)) {
                    continue;
                }
            }
            if (!"*".equals(feature)) {
                if (!ls.getFeature().equals(feature)) {
                    continue;
                }
            }
            matches.add(ls);
        }
        return matches;
    }

    /**
     * Match lookup specifications according to <script,language,feature> tuple,
     * where '*' is a wildcard for a tuple component.
     *
     * @param script
     *            a script identifier
     * @param language
     *            a language identifier
     * @param feature
     *            a feature identifier
     * @return a (possibly empty) map from matching lookup specifications to
     *         lists of corresponding lookup tables
     */
    public Map/* <LookupSpec,List<LookupTable>> */matchLookups(
            final String script, final String language, final String feature) {
        final List/* <LookupSpec> */lsl = matchLookupSpecs(script, language,
                feature);
        final Map lm = new LinkedHashMap();
        for (final Iterator it = lsl.iterator(); it.hasNext();) {
            final LookupSpec ls = (LookupSpec) it.next();
            lm.put(ls, findLookupTables(ls));
        }
        return lm;
    }

    /**
     * Obtain ordered list of glyph lookup tables that match a specific lookup
     * specification.
     *
     * @param ls
     *            a (non-null) lookup specification
     * @return a (possibly empty) ordered list of lookup tables whose
     *         corresponding lookup specifications match the specified lookup
     *         spec
     */
    public List/* <LookupTable> */findLookupTables(final LookupSpec ls) {
        final TreeSet/* <LookupTable> */lts = new TreeSet/* <LookupTable> */();
        List/* <String> */ids;
        if ((ids = (List/* <String> */) this.lookups.get(ls)) != null) {
            for (final Iterator it = ids.iterator(); it.hasNext();) {
                final String lid = (String) it.next();
                LookupTable lt;
                if ((lt = (LookupTable) this.lookupTables.get(lid)) != null) {
                    lts.add(lt);
                }
            }
        }
        return new ArrayList/* <LookupTable> */(lts);
    }

    /**
     * Assemble ordered array of lookup table use specifications according to
     * the specified features and candidate lookups, where the order of the
     * array is in accordance to the order of the applicable lookup list.
     *
     * @param features
     *            array of feature identifiers to apply
     * @param lookups
     *            a mapping from lookup specifications to lists of look tables
     *            from which to select lookup tables according to the specified
     *            features
     * @return ordered array of assembled lookup table use specifications
     */
    public UseSpec[] assembleLookups(final String[] features,
            final Map/* <LookupSpec,List<LookupTable>> */lookups) {
        final TreeSet/* <UseSpec> */uss = new TreeSet/* <UseSpec> */();
        for (final String feature : features) {
            for (final Iterator it = lookups.entrySet().iterator(); it
                    .hasNext();) {
                final Map.Entry/* <LookupSpec,List<LookupTable>> */e = (Map.Entry/*
                 * <
                 * LookupSpec
                 * ,
                 * List
                 * <
                 * LookupTable
                 * >>
                 */) it
                 .next();
                final LookupSpec ls = (LookupSpec) e.getKey();
                if (ls.getFeature().equals(feature)) {
                    final List/* <LookupTable> */ltl = (List/* <LookupTable> */) e
                            .getValue();
                    if (ltl != null) {
                        for (final Iterator ltit = ltl.iterator(); ltit
                                .hasNext();) {
                            final LookupTable lt = (LookupTable) ltit.next();
                            uss.add(new UseSpec(lt, feature));
                        }
                    }
                }
            }
        }
        return (UseSpec[]) uss.toArray(new UseSpec[uss.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("{");
        sb.append("lookups={");
        sb.append(this.lookups.toString());
        sb.append("},lookupTables={");
        sb.append(this.lookupTables.toString());
        sb.append("}}");
        return sb.toString();
    }

    /**
     * Obtain glyph table type from name.
     *
     * @param name
     *            of table type to map to type value
     * @return glyph table type (as an integer constant)
     */
    public static int getTableTypeFromName(final String name) {
        int t;
        final String s = name.toLowerCase();
        if ("gsub".equals(s)) {
            t = GLYPH_TABLE_TYPE_SUBSTITUTION;
        } else if ("gpos".equals(s)) {
            t = GLYPH_TABLE_TYPE_POSITIONING;
        } else if ("jstf".equals(s)) {
            t = GLYPH_TABLE_TYPE_JUSTIFICATION;
        } else if ("base".equals(s)) {
            t = GLYPH_TABLE_TYPE_BASELINE;
        } else if ("gdef".equals(s)) {
            t = GLYPH_TABLE_TYPE_DEFINITION;
        } else {
            t = -1;
        }
        return t;
    }

    /**
     * Resolve references to lookup tables in a collection of rules sets.
     *
     * @param rsa
     *            array of rule sets
     * @param lookupTables
     *            map from lookup table identifers, e.g. "lu4", to lookup tables
     */
    public static void resolveLookupReferences(final RuleSet[] rsa,
            final Map/* <String,LookupTable> */lookupTables) {
        if (rsa != null && lookupTables != null) {
            for (final RuleSet rs : rsa) {
                if (rs != null) {
                    rs.resolveLookupReferences(lookupTables);
                }
            }
        }
    }

    /**
     * A structure class encapsulating a lookup specification as a
     * <script,language,feature> tuple.
     */
    public static class LookupSpec implements Comparable {

        private final String script;
        private final String language;
        private final String feature;

        /**
         * Instantiate lookup spec.
         *
         * @param script
         *            a script identifier
         * @param language
         *            a language identifier
         * @param feature
         *            a feature identifier
         */
        public LookupSpec(final String script, final String language,
                final String feature) {
            if (script == null || script.length() == 0) {
                throw new AdvancedTypographicTableFormatException(
                        "script must be non-empty string");
            } else if (language == null || language.length() == 0) {
                throw new AdvancedTypographicTableFormatException(
                        "language must be non-empty string");
            } else if (feature == null || feature.length() == 0) {
                throw new AdvancedTypographicTableFormatException(
                        "feature must be non-empty string");
            } else if (script.equals("*")) {
                throw new AdvancedTypographicTableFormatException(
                        "script must not be wildcard");
            } else if (language.equals("*")) {
                throw new AdvancedTypographicTableFormatException(
                        "language must not be wildcard");
            } else if (feature.equals("*")) {
                throw new AdvancedTypographicTableFormatException(
                        "feature must not be wildcard");
            } else {
                this.script = script.trim();
                this.language = language.trim();
                this.feature = feature.trim();
            }
        }

        /** @return script identifier */
        public String getScript() {
            return this.script;
        }

        /** @return language identifier */
        public String getLanguage() {
            return this.language;
        }

        /** @return feature identifier */
        public String getFeature() {
            return this.feature;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            int hc = 0;
            hc = 7 * hc + (hc ^ this.script.hashCode());
            hc = 11 * hc + (hc ^ this.language.hashCode());
            hc = 17 * hc + (hc ^ this.feature.hashCode());
            return hc;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object o) {
            if (o instanceof LookupSpec) {
                final LookupSpec l = (LookupSpec) o;
                if (!l.script.equals(this.script)) {
                    return false;
                } else if (!l.language.equals(this.language)) {
                    return false;
                } else if (!l.feature.equals(this.feature)) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final Object o) {
            int d;
            if (o instanceof LookupSpec) {
                final LookupSpec ls = (LookupSpec) o;
                if ((d = this.script.compareTo(ls.script)) == 0) {
                    if ((d = this.language.compareTo(ls.language)) == 0) {
                        if ((d = this.feature.compareTo(ls.feature)) == 0) {
                            d = 0;
                        }
                    }
                }
            } else {
                d = -1;
            }
            return d;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(super.toString());
            sb.append("{");
            sb.append("<'" + this.script + "'");
            sb.append(",'" + this.language + "'");
            sb.append(",'" + this.feature + "'");
            sb.append(">}");
            return sb.toString();
        }

    }

    /**
     * The <code>LookupTable</code> class comprising an identifier and an
     * ordered list of glyph subtables, each of which employ the same lookup
     * identifier.
     */
    public static class LookupTable implements Comparable {

        private final String id; // lookup identifiers
        private final List/* <GlyphSubtable> */subtables; // list of subtables
        private boolean doesSub; // performs substitutions
        private boolean doesPos; // performs positioning
        private boolean frozen; // if true, then don't permit further subtable
        // additions
        // frozen state
        private GlyphSubtable[] subtablesArray;
        private static GlyphSubtable[] subtablesArrayEmpty = new GlyphSubtable[0];

        /**
         * Instantiate a LookupTable.
         *
         * @param id
         *            the lookup table's identifier
         * @param subtable
         *            an initial subtable (or null)
         */
        public LookupTable(final String id, final GlyphSubtable subtable) {
            this(id, makeSingleton(subtable));
        }

        /**
         * Instantiate a LookupTable.
         *
         * @param id
         *            the lookup table's identifier
         * @param subtables
         *            a pre-poplated list of subtables or null
         */
        public LookupTable(final String id,
                final List/* <GlyphSubtable> */subtables) {
            assert id != null;
            assert id.length() != 0;
            this.id = id;
            this.subtables = new LinkedList/* <GlyphSubtable> */();
            if (subtables != null) {
                for (final Iterator it = subtables.iterator(); it.hasNext();) {
                    final GlyphSubtable st = (GlyphSubtable) it.next();
                    addSubtable(st);
                }
            }
        }

        /** @return the identifier */
        public String getId() {
            return this.id;
        }

        /** @return the subtables as an array */
        public GlyphSubtable[] getSubtables() {
            if (this.frozen) {
                return this.subtablesArray != null ? this.subtablesArray
                        : subtablesArrayEmpty;
            } else {
                if (this.doesSub) {
                    return (GlyphSubtable[]) this.subtables
                            .toArray(new GlyphSubstitutionSubtable[this.subtables
                                                                   .size()]);
                } else if (this.doesPos) {
                    return (GlyphSubtable[]) this.subtables
                            .toArray(new GlyphPositioningSubtable[this.subtables
                                                                  .size()]);
                } else {
                    return null;
                }
            }
        }

        /**
         * Add a subtable into this lookup table's collecion of subtables
         * according to its natural order.
         *
         * @param subtable
         *            to add
         * @return true if subtable was not already present, otherwise false
         */
        public boolean addSubtable(GlyphSubtable subtable) {
            boolean added = false;
            // ensure table is not frozen
            if (this.frozen) {
                throw new IllegalStateException(
                        "glyph table is frozen, subtable addition prohibited");
            }
            // validate subtable to ensure consistency with current subtables
            validateSubtable(subtable);
            // insert subtable into ordered list
            for (final ListIterator/* <GlyphSubtable> */lit = this.subtables
                    .listIterator(0); lit.hasNext();) {
                final GlyphSubtable st = (GlyphSubtable) lit.next();
                int d;
                if ((d = subtable.compareTo(st)) < 0) {
                    // insert within list
                    lit.set(subtable);
                    lit.add(st);
                    added = true;
                } else if (d == 0) {
                    // duplicate entry is ignored
                    added = false;
                    subtable = null;
                }
            }
            // append at end of list
            if (!added && subtable != null) {
                this.subtables.add(subtable);
                added = true;
            }
            return added;
        }

        private void validateSubtable(final GlyphSubtable subtable) {
            if (subtable == null) {
                throw new AdvancedTypographicTableFormatException(
                        "subtable must be non-null");
            }
            if (subtable instanceof GlyphSubstitutionSubtable) {
                if (this.doesPos) {
                    throw new AdvancedTypographicTableFormatException(
                            "subtable must be positioning subtable, but is: "
                                    + subtable);
                } else {
                    this.doesSub = true;
                }
            }
            if (subtable instanceof GlyphPositioningSubtable) {
                if (this.doesSub) {
                    throw new AdvancedTypographicTableFormatException(
                            "subtable must be substitution subtable, but is: "
                                    + subtable);
                } else {
                    this.doesPos = true;
                }
            }
            if (this.subtables.size() > 0) {
                final GlyphSubtable st = (GlyphSubtable) this.subtables.get(0);
                if (!st.isCompatible(subtable)) {
                    throw new AdvancedTypographicTableFormatException(
                            "subtable " + subtable
                            + " is not compatible with subtable " + st);
                }
            }
        }

        /**
         * Freeze subtables, i.e., do not allow further subtable addition, and
         * create resulting cached state. In addition, resolve any references to
         * lookup tables that appear in this lookup table's subtables.
         *
         * @param lookupTables
         *            map from lookup table identifers, e.g. "lu4", to lookup
         *            tables
         */
        public void freezeSubtables(
                final Map/* <String,LookupTable> */lookupTables) {
            if (!this.frozen) {
                final GlyphSubtable[] sta = getSubtables();
                resolveLookupReferences(sta, lookupTables);
                this.subtablesArray = sta;
                this.frozen = true;
            }
        }

        private void resolveLookupReferences(final GlyphSubtable[] subtables,
                final Map/* <String,LookupTable> */lookupTables) {
            if (subtables != null) {
                for (final GlyphSubtable st : subtables) {
                    if (st != null) {
                        st.resolveLookupReferences(lookupTables);
                    }
                }
            }
        }

        /**
         * Determine if this glyph table performs substitution.
         *
         * @return true if it performs substitution
         */
        public boolean performsSubstitution() {
            return this.doesSub;
        }

        /**
         * Perform substitution processing using this lookup table's subtables.
         *
         * @param gs
         *            an input glyph sequence
         * @param script
         *            a script identifier
         * @param language
         *            a language identifier
         * @param feature
         *            a feature identifier
         * @param sct
         *            a script specific context tester (or null)
         * @return the substituted (output) glyph sequence
         */
        public GlyphSequence substitute(final GlyphSequence gs,
                final String script, final String language,
                final String feature, final ScriptContextTester sct) {
            if (performsSubstitution()) {
                return GlyphSubstitutionSubtable.substitute(gs, script,
                        language, feature,
                        (GlyphSubstitutionSubtable[]) this.subtablesArray, sct);
            } else {
                return gs;
            }
        }

        /**
         * Perform substitution processing on an existing glyph substitution
         * state object using this lookup table's subtables.
         *
         * @param ss
         *            a glyph substitution state object
         * @param sequenceIndex
         *            if non negative, then apply subtables only at specified
         *            sequence index
         * @return the substituted (output) glyph sequence
         */
        public GlyphSequence substitute(final GlyphSubstitutionState ss,
                final int sequenceIndex) {
            if (performsSubstitution()) {
                return GlyphSubstitutionSubtable.substitute(ss,
                        (GlyphSubstitutionSubtable[]) this.subtablesArray,
                        sequenceIndex);
            } else {
                return ss.getInput();
            }
        }

        /**
         * Determine if this glyph table performs positioning.
         *
         * @return true if it performs positioning
         */
        public boolean performsPositioning() {
            return this.doesPos;
        }

        /**
         * Perform positioning processing using this lookup table's subtables.
         *
         * @param gs
         *            an input glyph sequence
         * @param script
         *            a script identifier
         * @param language
         *            a language identifier
         * @param feature
         *            a feature identifier
         * @param fontSize
         *            size in device units
         * @param widths
         *            array of default advancements for each glyph in font
         * @param adjustments
         *            accumulated adjustments array (sequence) of 4-tuples of
         *            placement [PX,PY] and advance [AX,AY] adjustments, in that
         *            order, with one 4-tuple for each element of glyph sequence
         * @param sct
         *            a script specific context tester (or null)
         * @return true if some adjustment is not zero; otherwise, false
         */
        public boolean position(final GlyphSequence gs, final String script,
                final String language, final String feature,
                final int fontSize, final int[] widths,
                final int[][] adjustments, final ScriptContextTester sct) {
            if (performsPositioning()) {
                return GlyphPositioningSubtable.position(gs, script, language,
                        feature, fontSize,
                        (GlyphPositioningSubtable[]) this.subtablesArray,
                        widths, adjustments, sct);
            } else {
                return false;
            }
        }

        /**
         * Perform positioning processing on an existing glyph positioning state
         * object using this lookup table's subtables.
         *
         * @param ps
         *            a glyph positioning state object
         * @param sequenceIndex
         *            if non negative, then apply subtables only at specified
         *            sequence index
         * @return true if some adjustment is not zero; otherwise, false
         */
        public boolean position(final GlyphPositioningState ps,
                final int sequenceIndex) {
            if (performsPositioning()) {
                return GlyphPositioningSubtable.position(ps,
                        (GlyphPositioningSubtable[]) this.subtablesArray,
                        sequenceIndex);
            } else {
                return false;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        /**
         * {@inheritDoc}
         *
         * @return true if identifier of the specified lookup table is the same
         *         as the identifier of this lookup table
         */
        @Override
        public boolean equals(final Object o) {
            if (o instanceof LookupTable) {
                final LookupTable lt = (LookupTable) o;
                return this.id.equals(lt.id);
            } else {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         *
         * @return the result of comparing the identifier of the specified
         *         lookup table with the identifier of this lookup table; lookup
         *         table identifiers take the form "lu(DIGIT)+", with comparison
         *         based on numerical ordering of numbers expressed by (DIGIT)+.
         */
        @Override
        public int compareTo(final Object o) {
            if (o instanceof LookupTable) {
                final LookupTable lt = (LookupTable) o;
                assert this.id.startsWith("lu");
                final int i = Integer.parseInt(this.id.substring(2));
                assert lt.id.startsWith("lu");
                final int j = Integer.parseInt(lt.id.substring(2));
                if (i < j) {
                    return -1;
                } else if (i > j) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                return -1;
            }
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("{ ");
            sb.append("id = " + this.id);
            sb.append(", subtables = " + this.subtables);
            sb.append(" }");
            return sb.toString();
        }

        private static List/* <GlyphSubtable> */makeSingleton(
                final GlyphSubtable subtable) {
            if (subtable == null) {
                return null;
            } else {
                final List/* <GlyphSubtable> */stl = new ArrayList/*
                 * <GlyphSubtable
                 * >
                 */(1);
                stl.add(subtable);
                return stl;
            }
        }

    }

    /**
     * The <code>UseSpec</code> class comprises a lookup table reference and the
     * feature that selected the lookup table.
     */
    public static class UseSpec implements Comparable {

        /** lookup table to apply */
        private final LookupTable lookupTable;
        /** feature that caused selection of the lookup table */
        private final String feature;

        /**
         * Construct a glyph lookup table use specification.
         *
         * @param lookupTable
         *            a glyph lookup table
         * @param feature
         *            a feature that caused lookup table selection
         */
        public UseSpec(final LookupTable lookupTable, final String feature) {
            this.lookupTable = lookupTable;
            this.feature = feature;
        }

        /** @return the lookup table */
        public LookupTable getLookupTable() {
            return this.lookupTable;
        }

        /** @return the feature that selected this lookup table */
        public String getFeature() {
            return this.feature;
        }

        /**
         * Perform substitution processing using this use specification's lookup
         * table.
         *
         * @param gs
         *            an input glyph sequence
         * @param script
         *            a script identifier
         * @param language
         *            a language identifier
         * @param sct
         *            a script specific context tester (or null)
         * @return the substituted (output) glyph sequence
         */
        public GlyphSequence substitute(final GlyphSequence gs,
                final String script, final String language,
                final ScriptContextTester sct) {
            return this.lookupTable.substitute(gs, script, language,
                    this.feature, sct);
        }

        /**
         * Perform positioning processing using this use specification's lookup
         * table.
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
         *            array of default advancements for each glyph in font
         * @param adjustments
         *            accumulated adjustments array (sequence) of 4-tuples of
         *            placement [PX,PY] and advance [AX,AY] adjustments, in that
         *            order, with one 4-tuple for each element of glyph sequence
         * @param sct
         *            a script specific context tester (or null)
         * @return true if some adjustment is not zero; otherwise, false
         */
        public boolean position(final GlyphSequence gs, final String script,
                final String language, final int fontSize, final int[] widths,
                final int[][] adjustments, final ScriptContextTester sct) {
            return this.lookupTable.position(gs, script, language,
                    this.feature, fontSize, widths, adjustments, sct);
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return this.lookupTable.hashCode();
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object o) {
            if (o instanceof UseSpec) {
                final UseSpec u = (UseSpec) o;
                return this.lookupTable.equals(u.lookupTable);
            } else {
                return false;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final Object o) {
            if (o instanceof UseSpec) {
                final UseSpec u = (UseSpec) o;
                return this.lookupTable.compareTo(u.lookupTable);
            } else {
                return -1;
            }
        }

    }

    /**
     * The <code>RuleLookup</code> class implements a rule lookup record,
     * comprising a glyph sequence index and a lookup table index (in an
     * applicable lookup list).
     */
    public static class RuleLookup {

        private final int sequenceIndex; // index into input glyph sequence
        private final int lookupIndex; // lookup list index
        private LookupTable lookup; // resolved lookup table

        /**
         * Instantiate a RuleLookup.
         *
         * @param sequenceIndex
         *            the index into the input sequence
         * @param lookupIndex
         *            the lookup table index
         */
        public RuleLookup(final int sequenceIndex, final int lookupIndex) {
            this.sequenceIndex = sequenceIndex;
            this.lookupIndex = lookupIndex;
            this.lookup = null;
        }

        /** @return the sequence index */
        public int getSequenceIndex() {
            return this.sequenceIndex;
        }

        /** @return the lookup index */
        public int getLookupIndex() {
            return this.lookupIndex;
        }

        /** @return the lookup table */
        public LookupTable getLookup() {
            return this.lookup;
        }

        /**
         * Resolve references to lookup tables.
         *
         * @param lookupTables
         *            map from lookup table identifers, e.g. "lu4", to lookup
         *            tables
         */
        public void resolveLookupReferences(
                final Map/* <String,LookupTable> */lookupTables) {
            if (lookupTables != null) {
                final String lid = "lu" + Integer.toString(this.lookupIndex);
                final LookupTable lt = (LookupTable) lookupTables.get(lid);
                if (lt != null) {
                    this.lookup = lt;
                } else {
                    log.warn("unable to resolve glyph lookup table reference '"
                            + lid + "' amongst lookup tables: "
                            + lookupTables.values());
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "{ sequenceIndex = " + this.sequenceIndex
                    + ", lookupIndex = " + this.lookupIndex + " }";
        }

    }

    /**
     * The <code>Rule</code> class implements an array of rule lookup records.
     */
    public abstract static class Rule {

        private final RuleLookup[] lookups; // rule lookups
        private final int inputSequenceLength; // input sequence length

        /**
         * Instantiate a Rule.
         *
         * @param lookups
         *            the rule's lookups
         * @param inputSequenceLength
         *            the number of glyphs in the input sequence for this rule
         */
        protected Rule(final RuleLookup[] lookups, final int inputSequenceLength) {
            assert lookups != null;
            this.lookups = lookups;
            this.inputSequenceLength = inputSequenceLength;
        }

        /** @return the lookups */
        public RuleLookup[] getLookups() {
            return this.lookups;
        }

        /** @return the input sequence length */
        public int getInputSequenceLength() {
            return this.inputSequenceLength;
        }

        /**
         * Resolve references to lookup tables, e.g., in RuleLookup, to the
         * lookup tables themselves.
         *
         * @param lookupTables
         *            map from lookup table identifers, e.g. "lu4", to lookup
         *            tables
         */
        public void resolveLookupReferences(
                final Map/* <String,LookupTable> */lookupTables) {
            if (this.lookups != null) {
                for (final RuleLookup l : this.lookups) {
                    if (l != null) {
                        l.resolveLookupReferences(lookupTables);
                    }
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "{ lookups = " + Arrays.toString(this.lookups)
                    + ", inputSequenceLength = " + this.inputSequenceLength
                    + " }";
        }

    }

    /**
     * The <code>GlyphSequenceRule</code> class implements a subclass of
     * <code>Rule</code> that supports matching on a specific glyph sequence.
     */
    public static class GlyphSequenceRule extends Rule {

        private final int[] glyphs; // glyphs

        /**
         * Instantiate a GlyphSequenceRule.
         *
         * @param lookups
         *            the rule's lookups
         * @param inputSequenceLength
         *            number of glyphs constituting input sequence (to be
         *            consumed)
         * @param glyphs
         *            the rule's glyph sequence to match, starting with second
         *            glyph in sequence
         */
        public GlyphSequenceRule(final RuleLookup[] lookups,
                final int inputSequenceLength, final int[] glyphs) {
            super(lookups, inputSequenceLength);
            assert glyphs != null;
            this.glyphs = glyphs;
        }

        /**
         * Obtain glyphs. N.B. that this array starts with the second glyph of
         * the input sequence.
         *
         * @return the glyphs
         */
        public int[] getGlyphs() {
            return this.glyphs;
        }

        /**
         * Obtain glyphs augmented by specified first glyph entry.
         *
         * @param firstGlyph
         *            to fill in first glyph entry
         * @return the glyphs augmented by first glyph
         */
        public int[] getGlyphs(final int firstGlyph) {
            final int[] ga = new int[this.glyphs.length + 1];
            ga[0] = firstGlyph;
            System.arraycopy(this.glyphs, 0, ga, 1, this.glyphs.length);
            return ga;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("{ ");
            sb.append("lookups = " + Arrays.toString(getLookups()));
            sb.append(", glyphs = " + Arrays.toString(this.glyphs));
            sb.append(" }");
            return sb.toString();
        }

    }

    /**
     * The <code>ClassSequenceRule</code> class implements a subclass of
     * <code>Rule</code> that supports matching on a specific glyph class
     * sequence.
     */
    public static class ClassSequenceRule extends Rule {

        private final int[] classes; // glyph classes

        /**
         * Instantiate a ClassSequenceRule.
         *
         * @param lookups
         *            the rule's lookups
         * @param inputSequenceLength
         *            number of glyphs constituting input sequence (to be
         *            consumed)
         * @param classes
         *            the rule's glyph class sequence to match, starting with
         *            second glyph in sequence
         */
        public ClassSequenceRule(final RuleLookup[] lookups,
                final int inputSequenceLength, final int[] classes) {
            super(lookups, inputSequenceLength);
            assert classes != null;
            this.classes = classes;
        }

        /**
         * Obtain glyph classes. N.B. that this array starts with the class of
         * the second glyph of the input sequence.
         *
         * @return the classes
         */
        public int[] getClasses() {
            return this.classes;
        }

        /**
         * Obtain glyph classes augmented by specified first class entry.
         *
         * @param firstClass
         *            to fill in first class entry
         * @return the classes augmented by first class
         */
        public int[] getClasses(final int firstClass) {
            final int[] ca = new int[this.classes.length + 1];
            ca[0] = firstClass;
            System.arraycopy(this.classes, 0, ca, 1, this.classes.length);
            return ca;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("{ ");
            sb.append("lookups = " + Arrays.toString(getLookups()));
            sb.append(", classes = " + Arrays.toString(this.classes));
            sb.append(" }");
            return sb.toString();
        }

    }

    /**
     * The <code>CoverageSequenceRule</code> class implements a subclass of
     * <code>Rule</code> that supports matching on a specific glyph coverage
     * sequence.
     */
    public static class CoverageSequenceRule extends Rule {

        private final GlyphCoverageTable[] coverages; // glyph coverages

        /**
         * Instantiate a ClassSequenceRule.
         *
         * @param lookups
         *            the rule's lookups
         * @param inputSequenceLength
         *            number of glyphs constituting input sequence (to be
         *            consumed)
         * @param coverages
         *            the rule's glyph coverage sequence to match, starting with
         *            first glyph in sequence
         */
        public CoverageSequenceRule(final RuleLookup[] lookups,
                final int inputSequenceLength,
                final GlyphCoverageTable[] coverages) {
            super(lookups, inputSequenceLength);
            assert coverages != null;
            this.coverages = coverages;
        }

        /** @return the coverages */
        public GlyphCoverageTable[] getCoverages() {
            return this.coverages;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("{ ");
            sb.append("lookups = " + Arrays.toString(getLookups()));
            sb.append(", coverages = " + Arrays.toString(this.coverages));
            sb.append(" }");
            return sb.toString();
        }

    }

    /**
     * The <code>ChainedGlyphSequenceRule</code> class implements a subclass of
     * <code>GlyphSequenceRule</code> that supports matching on a specific glyph
     * sequence in a specific chained contextual.
     */
    public static class ChainedGlyphSequenceRule extends GlyphSequenceRule {

        private final int[] backtrackGlyphs; // backtrack glyphs
        private final int[] lookaheadGlyphs; // lookahead glyphs

        /**
         * Instantiate a ChainedGlyphSequenceRule.
         *
         * @param lookups
         *            the rule's lookups
         * @param inputSequenceLength
         *            number of glyphs constituting input sequence (to be
         *            consumed)
         * @param glyphs
         *            the rule's input glyph sequence to match, starting with
         *            second glyph in sequence
         * @param backtrackGlyphs
         *            the rule's backtrack glyph sequence to match, starting
         *            with first glyph in sequence
         * @param lookaheadGlyphs
         *            the rule's lookahead glyph sequence to match, starting
         *            with first glyph in sequence
         */
        public ChainedGlyphSequenceRule(final RuleLookup[] lookups,
                final int inputSequenceLength, final int[] glyphs,
                final int[] backtrackGlyphs, final int[] lookaheadGlyphs) {
            super(lookups, inputSequenceLength, glyphs);
            assert backtrackGlyphs != null;
            assert lookaheadGlyphs != null;
            this.backtrackGlyphs = backtrackGlyphs;
            this.lookaheadGlyphs = lookaheadGlyphs;
        }

        /** @return the backtrack glyphs */
        public int[] getBacktrackGlyphs() {
            return this.backtrackGlyphs;
        }

        /** @return the lookahead glyphs */
        public int[] getLookaheadGlyphs() {
            return this.lookaheadGlyphs;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("{ ");
            sb.append("lookups = " + Arrays.toString(getLookups()));
            sb.append(", glyphs = " + Arrays.toString(getGlyphs()));
            sb.append(", backtrackGlyphs = "
                    + Arrays.toString(this.backtrackGlyphs));
            sb.append(", lookaheadGlyphs = "
                    + Arrays.toString(this.lookaheadGlyphs));
            sb.append(" }");
            return sb.toString();
        }

    }

    /**
     * The <code>ChainedClassSequenceRule</code> class implements a subclass of
     * <code>ClassSequenceRule</code> that supports matching on a specific glyph
     * class sequence in a specific chained contextual.
     */
    public static class ChainedClassSequenceRule extends ClassSequenceRule {

        private final int[] backtrackClasses; // backtrack classes
        private final int[] lookaheadClasses; // lookahead classes

        /**
         * Instantiate a ChainedClassSequenceRule.
         *
         * @param lookups
         *            the rule's lookups
         * @param inputSequenceLength
         *            number of glyphs constituting input sequence (to be
         *            consumed)
         * @param classes
         *            the rule's input glyph class sequence to match, starting
         *            with second glyph in sequence
         * @param backtrackClasses
         *            the rule's backtrack glyph class sequence to match,
         *            starting with first glyph in sequence
         * @param lookaheadClasses
         *            the rule's lookahead glyph class sequence to match,
         *            starting with first glyph in sequence
         */
        public ChainedClassSequenceRule(final RuleLookup[] lookups,
                final int inputSequenceLength, final int[] classes,
                final int[] backtrackClasses, final int[] lookaheadClasses) {
            super(lookups, inputSequenceLength, classes);
            assert backtrackClasses != null;
            assert lookaheadClasses != null;
            this.backtrackClasses = backtrackClasses;
            this.lookaheadClasses = lookaheadClasses;
        }

        /** @return the backtrack classes */
        public int[] getBacktrackClasses() {
            return this.backtrackClasses;
        }

        /** @return the lookahead classes */
        public int[] getLookaheadClasses() {
            return this.lookaheadClasses;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("{ ");
            sb.append("lookups = " + Arrays.toString(getLookups()));
            sb.append(", classes = " + Arrays.toString(getClasses()));
            sb.append(", backtrackClasses = "
                    + Arrays.toString(this.backtrackClasses));
            sb.append(", lookaheadClasses = "
                    + Arrays.toString(this.lookaheadClasses));
            sb.append(" }");
            return sb.toString();
        }

    }

    /**
     * The <code>ChainedCoverageSequenceRule</code> class implements a subclass
     * of <code>CoverageSequenceRule</code> that supports matching on a specific
     * glyph class sequence in a specific chained contextual.
     */
    public static class ChainedCoverageSequenceRule extends
    CoverageSequenceRule {

        private final GlyphCoverageTable[] backtrackCoverages; // backtrack
        // coverages
        private final GlyphCoverageTable[] lookaheadCoverages; // lookahead
        // coverages

        /**
         * Instantiate a ChainedCoverageSequenceRule.
         *
         * @param lookups
         *            the rule's lookups
         * @param inputSequenceLength
         *            number of glyphs constituting input sequence (to be
         *            consumed)
         * @param coverages
         *            the rule's input glyph class sequence to match, starting
         *            with first glyph in sequence
         * @param backtrackCoverages
         *            the rule's backtrack glyph class sequence to match,
         *            starting with first glyph in sequence
         * @param lookaheadCoverages
         *            the rule's lookahead glyph class sequence to match,
         *            starting with first glyph in sequence
         */
        public ChainedCoverageSequenceRule(final RuleLookup[] lookups,
                final int inputSequenceLength,
                final GlyphCoverageTable[] coverages,
                final GlyphCoverageTable[] backtrackCoverages,
                final GlyphCoverageTable[] lookaheadCoverages) {
            super(lookups, inputSequenceLength, coverages);
            assert backtrackCoverages != null;
            assert lookaheadCoverages != null;
            this.backtrackCoverages = backtrackCoverages;
            this.lookaheadCoverages = lookaheadCoverages;
        }

        /** @return the backtrack coverages */
        public GlyphCoverageTable[] getBacktrackCoverages() {
            return this.backtrackCoverages;
        }

        /** @return the lookahead coverages */
        public GlyphCoverageTable[] getLookaheadCoverages() {
            return this.lookaheadCoverages;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("{ ");
            sb.append("lookups = " + Arrays.toString(getLookups()));
            sb.append(", coverages = " + Arrays.toString(getCoverages()));
            sb.append(", backtrackCoverages = "
                    + Arrays.toString(this.backtrackCoverages));
            sb.append(", lookaheadCoverages = "
                    + Arrays.toString(this.lookaheadCoverages));
            sb.append(" }");
            return sb.toString();
        }

    }

    /**
     * The <code>RuleSet</code> class implements a collection of rules, which
     * may or may not be the same rule type.
     */
    public static class RuleSet {

        private final Rule[] rules; // set of rules

        /**
         * Instantiate a Rule Set.
         *
         * @param rules
         *            the rules
         * @throws AdvancedTypographicTableFormatException
         *             if rules or some element of rules is null
         */
        public RuleSet(final Rule[] rules)
                throws AdvancedTypographicTableFormatException {
            // enforce rules array instance
            if (rules == null) {
                throw new AdvancedTypographicTableFormatException(
                        "rules[] is null");
            }
            this.rules = rules;
        }

        /** @return the rules */
        public Rule[] getRules() {
            return this.rules;
        }

        /**
         * Resolve references to lookup tables, e.g., in RuleLookup, to the
         * lookup tables themselves.
         *
         * @param lookupTables
         *            map from lookup table identifers, e.g. "lu4", to lookup
         *            tables
         */
        public void resolveLookupReferences(
                final Map/* <String,LookupTable> */lookupTables) {
            if (this.rules != null) {
                for (final Rule r : this.rules) {
                    if (r != null) {
                        r.resolveLookupReferences(lookupTables);
                    }
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "{ rules = " + Arrays.toString(this.rules) + " }";
        }

    }

    /**
     * The <code>HomogenousRuleSet</code> class implements a collection of
     * rules, which must be the same rule type (i.e., same concrete rule class)
     * or null.
     */
    public static class HomogeneousRuleSet extends RuleSet {

        /**
         * Instantiate a Homogeneous Rule Set.
         *
         * @param rules
         *            the rules
         * @throws AdvancedTypographicTableFormatException
         *             if some rule[i] is not an instance of rule[0]
         */
        public HomogeneousRuleSet(final Rule[] rules)
                throws AdvancedTypographicTableFormatException {
            super(rules);
            // find first non-null rule
            Rule r0 = null;
            for (int i = 1, n = rules.length; r0 == null && i < n; i++) {
                if (rules[i] != null) {
                    r0 = rules[i];
                }
            }
            // enforce rule instance homogeneity
            if (r0 != null) {
                final Class c = r0.getClass();
                for (int i = 1, n = rules.length; i < n; i++) {
                    final Rule r = rules[i];
                    if (r != null && !c.isInstance(r)) {
                        throw new AdvancedTypographicTableFormatException(
                                "rules[" + i + "] is not an instance of "
                                        + c.getName());
                    }
                }
            }

        }

    }

}
