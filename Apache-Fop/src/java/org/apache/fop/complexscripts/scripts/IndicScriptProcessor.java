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

package org.apache.fop.complexscripts.scripts;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.complexscripts.fonts.GlyphTable;
import org.apache.fop.complexscripts.fonts.GlyphTable.UseSpec;
import org.apache.fop.complexscripts.util.CharScript;
import org.apache.fop.complexscripts.util.GlyphContextTester;
import org.apache.fop.complexscripts.util.GlyphSequence;
import org.apache.fop.complexscripts.util.ScriptContextTester;

// CSOFF: AvoidNestedBlocksCheck
// CSOFF: NoWhitespaceAfterCheck
// CSOFF: InnerAssignmentCheck
// CSOFF: SimplifyBooleanReturnCheck
// CSOFF: EmptyForIteratorPadCheck
// CSOFF: WhitespaceAfterCheck
// CSOFF: ParameterNumberCheck
// CSOFF: LineLengthCheck

/**
 * <p>
 * The <code>IndicScriptProcessor</code> class implements a script processor for
 * performing glyph substitution and positioning operations on content
 * associated with the Indic script.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
@Slf4j
public class IndicScriptProcessor extends DefaultScriptProcessor {

    /** required features to use for substitutions */
    private static final String[] gsubReqFeatures = // CSOK: ConstantNameCheck
    { "abvf", // above base forms
        "abvs", // above base substitutions
        "akhn", // akhand
        "blwf", // below base forms
        "blws", // below base substitutions
        "ccmp", // glyph composition/decomposition
        "cjct", // conjunct forms
        "clig", // contextual ligatures
        "half", // half forms
        "haln", // halant forms
        "locl", // localized forms
        "nukt", // nukta forms
        "pref", // pre-base forms
        "pres", // pre-base substitutions
        "pstf", // post-base forms
        "psts", // post-base substitutions
        "rkrf", // rakar forms
        "rphf", // reph form
        "vatu" // vattu variants
    };

    /** optional features to use for substitutions */
    private static final String[] gsubOptFeatures = // CSOK: ConstantNameCheck
    { "afrc", // alternative fractions
        "calt", // contextual alternatives
        "dlig" // discretionary ligatures
    };

    /** required features to use for positioning */
    private static final String[] gposReqFeatures = // CSOK: ConstantNameCheck
    { "abvm", // above base marks
        "blwm", // below base marks
        "dist", // distance (adjustment)
        "kern" // kerning
    };

    /** required features to use for positioning */
    private static final String[] gposOptFeatures = // CSOK: ConstantNameCheck
    {};

    private static class SubstitutionScriptContextTester implements
    ScriptContextTester {
        private static Map/* <String,GlyphContextTester> */testerMap = new HashMap/*
         * <
         * String
         * ,
         * GlyphContextTester
         * >
         */();

        @Override
        public GlyphContextTester getTester(final String feature) {
            return (GlyphContextTester) testerMap.get(feature);
        }
    }

    private static class PositioningScriptContextTester implements
    ScriptContextTester {
        private static Map/* <String,GlyphContextTester> */testerMap = new HashMap/*
         * <
         * String
         * ,
         * GlyphContextTester
         * >
         */();

        @Override
        public GlyphContextTester getTester(final String feature) {
            return (GlyphContextTester) testerMap.get(feature);
        }
    }

    /**
     * Make script specific flavor of Indic script processor.
     *
     * @param script
     *            tag
     * @return script processor instance
     */
    public static ScriptProcessor makeProcessor(final String script) {
        switch (CharScript.scriptCodeFromTag(script)) {
        case CharScript.SCRIPT_DEVANAGARI:
        case CharScript.SCRIPT_DEVANAGARI_2:
            return new DevanagariScriptProcessor(script);
        case CharScript.SCRIPT_GUJARATI:
        case CharScript.SCRIPT_GUJARATI_2:
            return new GujaratiScriptProcessor(script);
        case CharScript.SCRIPT_GURMUKHI:
        case CharScript.SCRIPT_GURMUKHI_2:
            return new GurmukhiScriptProcessor(script);
            // [TBD] implement other script processors
        default:
            return new IndicScriptProcessor(script);
        }
    }

    private final ScriptContextTester subContextTester;
    private final ScriptContextTester posContextTester;

    IndicScriptProcessor(final String script) {
        super(script);
        this.subContextTester = new SubstitutionScriptContextTester();
        this.posContextTester = new PositioningScriptContextTester();
    }

    /** {@inheritDoc} */
    @Override
    public String[] getSubstitutionFeatures() {
        return gsubReqFeatures;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getOptionalSubstitutionFeatures() {
        return gsubOptFeatures;
    }

    /** {@inheritDoc} */
    @Override
    public ScriptContextTester getSubstitutionContextTester() {
        return this.subContextTester;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getPositioningFeatures() {
        return gposReqFeatures;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getOptionalPositioningFeatures() {
        return gposOptFeatures;
    }

    /** {@inheritDoc} */
    @Override
    public ScriptContextTester getPositioningContextTester() {
        return this.posContextTester;
    }

    /** {@inheritDoc} */
    @Override
    public GlyphSequence substitute(final GlyphSequence gs,
            final String script, final String language,
            final GlyphTable.UseSpec[] usa, final ScriptContextTester sct) {
        assert usa != null;
        // 1. syllabize
        final GlyphSequence[] sa = syllabize(gs, script, language);
        // 2. process each syllable
        for (int i = 0, n = sa.length; i < n; i++) {
            GlyphSequence s = sa[i];
            // apply basic shaping subs
            for (final UseSpec us : usa) {
                if (isBasicShapingUse(us)) {
                    s.setPredications(true);
                    s = us.substitute(s, script, language, sct);
                }
            }
            // reorder pre-base matra
            s = reorderPreBaseMatra(s);
            // reorder reph
            s = reorderReph(s);
            // apply presentation subs
            for (final UseSpec us : usa) {
                if (isPresentationUse(us)) {
                    s.setPredications(true);
                    s = us.substitute(s, script, language, sct);
                }
            }
            // record result
            sa[i] = s;
        }
        // 3. return reassembled substituted syllables
        return unsyllabize(gs, sa);
    }

    /**
     * Get script specific syllabizer class.
     *
     * @return a syllabizer class object or null
     */
    protected Class<? extends Syllabizer> getSyllabizerClass() {
        return null;
    }

    private GlyphSequence[] syllabize(final GlyphSequence gs,
            final String script, final String language) {
        return Syllabizer.getSyllabizer(script, language, getSyllabizerClass())
                .syllabize(gs);
    }

    private GlyphSequence unsyllabize(final GlyphSequence gs,
            final GlyphSequence[] sa) {
        return GlyphSequence.join(gs, sa);
    }

    private static Set<String> basicShapingFeatures;
    private static final String[] basicShapingFeatureStrings = { // CSOK:
            // ConstantNameCheck
            "abvf", "akhn", "blwf", "cjct", "half", "locl", "nukt", "pref",
            "pstf", "rkrf", "rphf", "vatu", };
    static {
        basicShapingFeatures = new HashSet<String>();
        for (final String s : basicShapingFeatureStrings) {
            basicShapingFeatures.add(s);
        }
    }

    private boolean isBasicShapingUse(final GlyphTable.UseSpec us) {
        assert us != null;
        if (basicShapingFeatures != null) {
            return basicShapingFeatures.contains(us.getFeature());
        } else {
            return false;
        }
    }

    private static Set<String> presentationFeatures;
    private static final String[] presentationFeatureStrings = { // CSOK:
        // ConstantNameCheck
            "abvs", "blws", "calt", "haln", "pres", "psts", };
    static {
        presentationFeatures = new HashSet<String>();
        for (final String s : presentationFeatureStrings) {
            presentationFeatures.add(s);
        }
    }

    private boolean isPresentationUse(final GlyphTable.UseSpec us) {
        assert us != null;
        if (presentationFeatures != null) {
            return presentationFeatures.contains(us.getFeature());
        } else {
            return false;
        }
    }

    private GlyphSequence reorderPreBaseMatra(GlyphSequence gs) {
        int source;
        if ((source = findPreBaseMatra(gs)) >= 0) {
            int target;
            if ((target = findPreBaseMatraTarget(gs, source)) >= 0) {
                if (target != source) {
                    gs = reorder(gs, source, target);
                }
            }
        }
        return gs;
    }

    /**
     * Find pre-base matra in sequence.
     *
     * @param gs
     *            input sequence
     * @return index of pre-base matra or -1 if not found
     */
    protected int findPreBaseMatra(final GlyphSequence gs) {
        return -1;
    }

    /**
     * Find pre-base matra target in sequence.
     *
     * @param gs
     *            input sequence
     * @param source
     *            index of pre-base matra
     * @return index of pre-base matra target or -1
     */
    protected int findPreBaseMatraTarget(final GlyphSequence gs,
            final int source) {
        return -1;
    }

    private GlyphSequence reorderReph(GlyphSequence gs) {
        int source;
        if ((source = findReph(gs)) >= 0) {
            int target;
            if ((target = findRephTarget(gs, source)) >= 0) {
                if (target != source) {
                    gs = reorder(gs, source, target);
                }
            }
        }
        return gs;
    }

    /**
     * Find reph in sequence.
     *
     * @param gs
     *            input sequence
     * @return index of reph or -1 if not found
     */
    protected int findReph(final GlyphSequence gs) {
        return -1;
    }

    /**
     * Find reph target in sequence.
     *
     * @param gs
     *            input sequence
     * @param source
     *            index of reph
     * @return index of reph target or -1
     */
    protected int findRephTarget(final GlyphSequence gs, final int source) {
        return -1;
    }

    private GlyphSequence reorder(final GlyphSequence gs, final int source,
            final int target) {
        return GlyphSequence.reorder(gs, source, 1, target);
    }

    /** {@inheritDoc} */
    @Override
    public boolean position(final GlyphSequence gs, final String script,
            final String language, final int fontSize,
            final GlyphTable.UseSpec[] usa, final int[] widths,
            final int[][] adjustments, final ScriptContextTester sct) {
        final boolean adjusted = super.position(gs, script, language, fontSize,
                usa, widths, adjustments, sct);
        return adjusted;
    }

    /** Abstract syllabizer. */
    protected abstract static class Syllabizer implements Comparable {
        private final String script;
        private final String language;

        Syllabizer(final String script, final String language) {
            this.script = script;
            this.language = language;
        }

        /**
         * Subdivide glyph sequence GS into syllabic segments each represented
         * by a distinct output glyph sequence.
         *
         * @param gs
         *            input glyph sequence
         * @return segmented syllabic glyph sequences
         */
        abstract GlyphSequence[] syllabize(final GlyphSequence gs);

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            int hc = 0;
            hc = 7 * hc + (hc ^ this.script.hashCode());
            hc = 11 * hc + (hc ^ this.language.hashCode());
            return hc;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object o) {
            if (o instanceof Syllabizer) {
                final Syllabizer s = (Syllabizer) o;
                if (!s.script.equals(this.script)) {
                    return false;
                } else if (!s.language.equals(this.language)) {
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
            if (o instanceof Syllabizer) {
                final Syllabizer s = (Syllabizer) o;
                if ((d = this.script.compareTo(s.script)) == 0) {
                    d = this.language.compareTo(s.language);
                }
            } else {
                d = -1;
            }
            return d;
        }

        private static Map<String, Syllabizer> syllabizers = new HashMap<String, Syllabizer>();

        static Syllabizer getSyllabizer(final String script,
                final String language,
                final Class<? extends Syllabizer> syllabizerClass) {
            final String sid = makeSyllabizerId(script, language);
            Syllabizer s = syllabizers.get(sid);
            if (s == null) {
                if ((s = makeSyllabizer(script, language, syllabizerClass)) == null) {
                    s = new DefaultSyllabizer(script, language);
                }
                syllabizers.put(sid, s);
            }
            return s;
        }

        static String makeSyllabizerId(final String script,
                final String language) {
            return script + ":" + language;
        }

        static Syllabizer makeSyllabizer(final String script,
                final String language,
                final Class<? extends Syllabizer> syllabizerClass) {
            Syllabizer s;
            try {
                final Constructor<? extends Syllabizer> cf = syllabizerClass
                        .getDeclaredConstructor(new Class[] { String.class,
                                String.class });
                s = cf.newInstance(script, language);
            } catch (final NoSuchMethodException e) {
                s = null;
            } catch (final InstantiationException e) {
                s = null;
            } catch (final IllegalAccessException e) {
                s = null;
            } catch (final InvocationTargetException e) {
                s = null;
            }
            return s;
        }
    }

    /** Default syllabizer. */
    protected static class DefaultSyllabizer extends Syllabizer {
        DefaultSyllabizer(final String script, final String language) {
            super(script, language);
        }

        /** {@inheritDoc} */
        @Override
        GlyphSequence[] syllabize(final GlyphSequence gs) {
            final int[] ca = gs.getCharacterArray(false);
            final int nc = gs.getCharacterCount();
            if (nc == 0) {
                return new GlyphSequence[] { gs };
            } else {
                return segmentize(gs, segmentize(ca, nc));
            }
        }

        /**
         * Construct array of segements from original character array
         * (associated with original glyph sequence)
         *
         * @param ca
         *            input character sequence
         * @param nc
         *            number of characters in sequence
         * @return array of syllable segments
         */
        protected Segment[] segmentize(final int[] ca, final int nc) {
            final Vector<Segment> sv = new Vector<Segment>(nc);
            for (int s = 0, e = nc; s < e;) {
                int i;
                if ((i = findStartOfSyllable(ca, s, e)) > s) {
                    // from s to i is non-syllable segment
                    sv.add(new Segment(s, i, Segment.OTHER));
                    s = i; // move s to start of syllable
                } else if (i > s) {
                    // from s to e is non-syllable segment
                    sv.add(new Segment(s, e, Segment.OTHER));
                    s = e; // move s to end of input sequence
                }
                if ((i = findEndOfSyllable(ca, s, e)) > s) {
                    // from s to i is syllable segment
                    sv.add(new Segment(s, i, Segment.SYLLABLE));
                    s = i; // move s to end of syllable
                } else {
                    // from s to e is non-syllable segment
                    sv.add(new Segment(s, e, Segment.OTHER));
                    s = e; // move s to end of input sequence
                }
            }
            return sv.toArray(new Segment[sv.size()]);
        }

        /**
         * Construct array of glyph sequences from original glyph sequence and
         * segment array.
         *
         * @param gs
         *            original input glyph sequence
         * @param sa
         *            segment array
         * @return array of glyph sequences each belonging to an (ordered)
         *         segment in SA
         */
        protected GlyphSequence[] segmentize(final GlyphSequence gs,
                final Segment[] sa) {
            final int ng = gs.getGlyphCount();
            final int[] ga = gs.getGlyphArray(false);
            final GlyphSequence.CharAssociation[] aa = gs
                    .getAssociations(0, -1);
            final Vector<GlyphSequence> nsv = new Vector<GlyphSequence>();
            for (final Segment s : sa) {
                final Vector<Integer> ngv = new Vector<Integer>(ng);
                final Vector<GlyphSequence.CharAssociation> nav = new Vector<GlyphSequence.CharAssociation>(
                        ng);
                for (int j = 0; j < ng; j++) {
                    final GlyphSequence.CharAssociation ca = aa[j];
                    if (ca.contained(s.getOffset(), s.getCount())) {
                        ngv.add(ga[j]);
                        nav.add(ca);
                    }
                }
                if (ngv.size() > 0) {
                    nsv.add(new GlyphSequence(gs, null, toIntArray(ngv), null,
                            null,
                            nav.toArray(new GlyphSequence.CharAssociation[nav
                                                                          .size()]), null));
                }
            }
            if (nsv.size() > 0) {
                return nsv.toArray(new GlyphSequence[nsv.size()]);
            } else {
                return new GlyphSequence[] { gs };
            }
        }

        /**
         * Find start of syllable in character array, starting at S, ending at
         * E.
         *
         * @param ca
         *            character array
         * @param s
         *            start index
         * @param e
         *            end index
         * @return index of start or E if no start found
         */
        protected int findStartOfSyllable(final int[] ca, final int s,
                final int e) {
            return e;
        }

        /**
         * Find end of syllable in character array, starting at S, ending at E.
         *
         * @param ca
         *            character array
         * @param s
         *            start index
         * @param e
         *            end index
         * @return index of start or S if no end found
         */
        protected int findEndOfSyllable(final int[] ca, final int s, final int e) {
            return s;
        }

        private static int[] toIntArray(final Vector<Integer> iv) {
            final int ni = iv.size();
            final int[] ia = new int[iv.size()];
            for (int i = 0, n = ni; i < n; i++) {
                ia[i] = iv.get(i);
            }
            return ia;
        }
    }

    /** Syllabic segment. */
    protected static class Segment {

        static final int OTHER = 0; // other (non-syllable) characters
        static final int SYLLABLE = 1; // (orthographic) syllable

        private final int start;
        private final int end;
        private final int type;

        Segment(final int start, final int end, final int type) {
            this.start = start;
            this.end = end;
            this.type = type;
        }

        int getStart() {
            return this.start;
        }

        int getEnd() {
            return this.end;
        }

        int getOffset() {
            return this.start;
        }

        int getCount() {
            return this.end - this.start;
        }

        int getType() {
            return this.type;
        }
    }
}
