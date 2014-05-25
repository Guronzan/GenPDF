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

package org.apache.fop.complexscripts.fonts.ttx;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.fop.complexscripts.fonts.GlyphClassTable;
import org.apache.fop.complexscripts.fonts.GlyphCoverageTable;
import org.apache.fop.complexscripts.fonts.GlyphDefinitionTable;
import org.apache.fop.complexscripts.fonts.GlyphMappingTable;
import org.apache.fop.complexscripts.fonts.GlyphPositioningTable;
import org.apache.fop.complexscripts.fonts.GlyphPositioningTable.Anchor;
import org.apache.fop.complexscripts.fonts.GlyphPositioningTable.MarkAnchor;
import org.apache.fop.complexscripts.fonts.GlyphPositioningTable.PairValues;
import org.apache.fop.complexscripts.fonts.GlyphPositioningTable.Value;
import org.apache.fop.complexscripts.fonts.GlyphSubstitutionTable;
import org.apache.fop.complexscripts.fonts.GlyphSubstitutionTable.Ligature;
import org.apache.fop.complexscripts.fonts.GlyphSubstitutionTable.LigatureSet;
import org.apache.fop.complexscripts.fonts.GlyphSubtable;
import org.apache.fop.complexscripts.fonts.GlyphTable;
import org.apache.fop.complexscripts.fonts.GlyphTable.RuleLookup;
import org.apache.fop.complexscripts.util.GlyphSequence;
import org.apache.fop.complexscripts.util.UTF32;
import org.apache.fop.util.CharUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

// CSOFF: InnerAssignmentCheck
// CSOFF: LineLengthCheck
// CSOFF: NoWhitespaceAfterCheck

/**
 * This class supports a subset of the <code>TTX</code> file as produced by the
 * Adobe FLEX SDK (AFDKO). In particular, it is used to parse a <code>TTX</code>
 * file in order to extract character to glyph code mapping data, glyph
 * definition data, glyph substitution data, and glyph positioning data.
 *
 * <code>TTX</code> files are used in FOP for testing and debugging purposes
 * only. Such files are used to represent font data employed by complex script
 * processing, and normally extracted directly from an opentype (or truetype)
 * file. However, due to copyright restrictions, it is not possible to include
 * most opentype (or truetype) font files directly in the FOP distribution. In
 * such cases, <code>TTX</code> files are used to distribute a subset of the
 * complex script advanced table information contained in certain font files to
 * facilitate testing.
 *
 * @author Glenn Adams
 */
public class TTXFile {

    /** default script tag */
    private static final String DEFAULT_SCRIPT_TAG = "dflt";
    /** default language tag */
    private static final String DEFAULT_LANGUAGE_TAG = "dflt";

    /** ttxfile cache */
    private static Map<String, TTXFile> cache = new HashMap<String, TTXFile>();

    // transient parsing state
    private Locator locator; // current document locator
    private final Stack<String[]> elements; // stack of ttx elements being
    // parsed
    private final Map<String, Integer> glyphIds; // map of glyph names to glyph
    // identifiers
    private final List<int[]> cmapEntries; // list of <charCode,glyphCode> pairs
    private final List<int[]> hmtxEntries; // vector of <width,lsb> pairs
    private final Map<String, Integer> glyphClasses; // map of glyph names to
    // glyph classes
    private final Map<String, Map<String, List<String>>> scripts; // map of
    // script tag
    // to
    // Map<language-tag,List<features-id>>>
    private final Map<String, List<String>> languages; // map of language tag to
    // List<feature-id>
    private final Map<String, Object[]> features; // map of feature id to
    // Object[2] : { feature-tag,
    // List<lookup-id> }
    private final List<String> languageFeatures; // list of language system
    // feature ids, where first is
    // (possibly null) required
    // feature id
    private final List<String> featureLookups; // list of lookup ids for feature
    // being constructed
    private final List<Integer> coverageEntries; // list of entries for coverage
    // table being constructed
    private final Map<String, GlyphCoverageTable> coverages; // map of coverage
    // table keys to
    // coverage tables
    private final List subtableEntries; // list of lookup subtable entries
    private final List<GlyphSubtable> subtables; // list of constructed
    // subtables
    private final List<Integer> alternates; // list of alternates in alternate
    // set being constructed
    private final List<Ligature> ligatures; // list of ligatures in ligature set
    // being constructed
    private final List<Integer> substitutes; // list of substitutes in (multiple
    // substitution) sequence being
    // constructed
    private final List<PairValues> pairs; // list of pair value records being
    // constructed
    private final List<PairValues[]> pairSets; // list of pair value sets (as
    // arrays) being constructed
    private final List<Anchor> anchors; // list of anchors of
    // base|mark|component record being
    // constructed
    private final List<Anchor[]> components; // list of ligature component
    // anchors being constructed
    private final List<MarkAnchor> markAnchors; // list of mark anchors being
    // constructed
    private final List<Anchor[]> baseOrMarkAnchors; // list of base|mark2
    // anchors being constructed
    private final List<Anchor[][]> ligatureAnchors; // list of ligature anchors
    // being constructed
    private final List<Anchor[]> attachmentAnchors; // list of entry|exit
    // attachment anchors being
    // constructed
    private final List<RuleLookup> ruleLookups; // list of rule lookups being
    // constructed
    private int glyphIdMax; // maximum glyph id
    private int cmPlatform; // plaform id of cmap being constructed
    private int cmEncoding; // plaform id of cmap being constructed
    private int cmLanguage; // plaform id of cmap being constructed
    private int flIndex; // index of feature being constructed
    private int flSequence; // feature sequence within feature list
    private int ltIndex; // index of lookup table being constructed
    private int ltSequence; // lookup sequence within table
    private int ltFlags; // flags of current lookup being constructed
    private int stSequence; // subtable sequence number within lookup
    private int stFormat; // format of current subtable being constructed
    private int ctFormat; // format of coverage table being constructed
    private int ctIndex; // index of coverage table being constructed
    private int rlSequence; // rule lookup sequence index
    private int rlLookup; // rule lookup lookup index
    private int psIndex; // pair set index
    private int vf1; // value format 1 (used with pair pos and single pos)
    private int vf2; // value format 2 (used with pair pos)
    private int g2; // glyph id 2 (used with pair pos)
    private int xCoord; // x coordinate of anchor being constructed
    private int yCoord; // y coordinate of anchor being constructed
    private int markClass; // mark class of mark anchor being constructed
    private final String defaultScriptTag; // tag of default script
    private String scriptTag; // tag of script being constructed
    private final String defaultLanguageTag; // tag of default language system
    private String languageTag; // tag of language system being constructed
    private String featureTag; // tag of feature being constructed
    private Value v1; // positioining value 1
    private Value v2; // positioining value 2

    // resultant state
    private int upem; // units per em
    private Map<Integer, Integer> cmap; // constructed character map
    private Map<Integer, Integer> gmap; // constructed glyph map
    private int[][] hmtx; // constructed horizontal metrics - array of design {
    // width, lsb } pairs, indexed by glyph code
    private int[] widths; // pdf normalized widths (millipoints)
    private GlyphDefinitionTable gdef; // constructed glyph definition table
    private GlyphSubstitutionTable gsub; // constructed glyph substitution table
    private GlyphPositioningTable gpos; // constructed glyph positioning table

    public TTXFile() {
        this.elements = new Stack<String[]>();
        this.glyphIds = new HashMap<String, Integer>();
        this.cmapEntries = new ArrayList<int[]>();
        this.hmtxEntries = new ArrayList<int[]>();
        this.glyphClasses = new HashMap<String, Integer>();
        this.scripts = new HashMap<String, Map<String, List<String>>>();
        this.languages = new HashMap<String, List<String>>();
        this.features = new HashMap<String, Object[]>();
        this.languageFeatures = new ArrayList<String>();
        this.featureLookups = new ArrayList<String>();
        this.coverageEntries = new ArrayList<Integer>();
        this.coverages = new HashMap<String, GlyphCoverageTable>();
        this.subtableEntries = new ArrayList();
        this.subtables = new ArrayList<GlyphSubtable>();
        this.alternates = new ArrayList<Integer>();
        this.ligatures = new ArrayList<Ligature>();
        this.substitutes = new ArrayList<Integer>();
        this.pairs = new ArrayList<PairValues>();
        this.pairSets = new ArrayList<PairValues[]>();
        this.anchors = new ArrayList<Anchor>();
        this.markAnchors = new ArrayList<MarkAnchor>();
        this.baseOrMarkAnchors = new ArrayList<Anchor[]>();
        this.ligatureAnchors = new ArrayList<Anchor[][]>();
        this.components = new ArrayList<Anchor[]>();
        this.attachmentAnchors = new ArrayList<Anchor[]>();
        this.ruleLookups = new ArrayList<RuleLookup>();
        this.glyphIdMax = -1;
        this.cmPlatform = -1;
        this.cmEncoding = -1;
        this.cmLanguage = -1;
        this.flIndex = -1;
        this.flSequence = 0;
        this.ltIndex = -1;
        this.ltSequence = 0;
        this.ltFlags = 0;
        this.stSequence = 0;
        this.stFormat = 0;
        this.ctFormat = -1;
        this.ctIndex = -1;
        this.rlSequence = -1;
        this.rlLookup = -1;
        this.psIndex = -1;
        this.vf1 = -1;
        this.vf2 = -1;
        this.g2 = -1;
        this.xCoord = Integer.MIN_VALUE;
        this.yCoord = Integer.MIN_VALUE;
        this.markClass = -1;
        this.defaultScriptTag = DEFAULT_SCRIPT_TAG;
        this.scriptTag = null;
        this.defaultLanguageTag = DEFAULT_LANGUAGE_TAG;
        this.languageTag = null;
        this.featureTag = null;
        this.v1 = null;
        this.v2 = null;
        this.upem = -1;
    }

    public void parse(final String filename) {
        parse(new File(filename));
    }

    public void parse(final File f) {
        assert f != null;
        try {
            final SAXParserFactory spf = SAXParserFactory.newInstance();
            final SAXParser sp = spf.newSAXParser();
            sp.parse(f, new Handler());
        } catch (final FactoryConfigurationError e) {
            throw new RuntimeException(e.getMessage());
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage());
        } catch (final SAXException e) {
            throw new RuntimeException(e.getMessage());
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public GlyphSequence mapCharsToGlyphs(final String s) {
        final Integer[] ca = UTF32.toUTF32(s, 0, true);
        final int ng = ca.length;
        final IntBuffer cb = IntBuffer.allocate(ng);
        final IntBuffer gb = IntBuffer.allocate(ng);
        for (final Integer c : ca) {
            final int g = mapCharToGlyph(c);
            if (g >= 0) {
                cb.put(c);
                gb.put(g);
            } else {
                throw new IllegalArgumentException("character "
                        + CharUtilities.format(c)
                        + " has no corresponding glyph");
            }
        }
        cb.rewind();
        gb.rewind();
        return new GlyphSequence(cb, gb, null);
    }

    public int mapCharToGlyph(final int c) {
        if (this.cmap != null) {
            final Integer g = this.cmap.get(Integer.valueOf(c));
            if (g != null) {
                return g;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public int getGlyph(final String gid) {
        return mapGlyphId0(gid);
    }

    public GlyphSequence getGlyphSequence(final String[] gids) {
        assert gids != null;
        final int ng = gids.length;
        final IntBuffer cb = IntBuffer.allocate(ng);
        final IntBuffer gb = IntBuffer.allocate(ng);
        for (final String gid : gids) {
            final int g = mapGlyphId0(gid);
            if (g >= 0) {
                int c = mapGlyphIdToChar(gid);
                if (c < 0) {
                    c = CharUtilities.NOT_A_CHARACTER;
                }
                cb.put(c);
                gb.put(g);
            } else {
                throw new IllegalArgumentException("unmapped glyph id \"" + gid
                        + "\"");
            }
        }
        cb.rewind();
        gb.rewind();
        return new GlyphSequence(cb, gb, null);
    }

    public int[] getWidths(final String[] gids) {
        assert gids != null;
        final int ng = gids.length;
        final int[] widths = new int[ng];
        int i = 0;
        for (final String gid : gids) {
            final int g = mapGlyphId0(gid);
            int w = 0;
            if (g >= 0) {
                if (this.hmtx != null && g < this.hmtx.length) {
                    final int[] mtx = this.hmtx[g];
                    assert mtx != null;
                    assert mtx.length > 0;
                    w = mtx[0];
                }
            }
            widths[i++] = w;
        }
        assert i == ng;
        return widths;
    }

    public int[] getWidths() {
        if (this.widths == null) {
            if (this.hmtx != null && this.upem > 0) {
                final int[] widths = new int[this.hmtx.length];
                for (int i = 0, n = widths.length; i < n; i++) {
                    widths[i] = getPDFWidth(this.hmtx[i][0], this.upem);
                }
                this.widths = widths;
            }
        }
        return this.widths;
    }

    public static int getPDFWidth(final int tw, final int upem) {
        // N.B. The following is copied (with minor edits) from TTFFile to
        // insure same results
        int pw;
        if (tw < 0) {
            final long rest1 = tw % upem;
            final long storrest = 1000 * rest1;
            final long ledd2 = storrest != 0 ? rest1 / storrest : 0;
            pw = -(-1000 * tw / upem - (int) ledd2);
        } else {
            pw = tw / upem * 1000 + tw % upem * 1000 / upem;
        }
        return pw;
    }

    public GlyphDefinitionTable getGDEF() {
        return this.gdef;
    }

    public GlyphSubstitutionTable getGSUB() {
        return this.gsub;
    }

    public GlyphPositioningTable getGPOS() {
        return this.gpos;
    }

    public static synchronized TTXFile getFromCache(final String filename) {
        assert cache != null;
        TTXFile f;
        if ((f = cache.get(filename)) == null) {
            f = new TTXFile();
            f.parse(filename);
            cache.put(filename, f);
        }
        return f;
    }

    public static synchronized void clearCache() {
        cache.clear();
    }

    private class Handler extends DefaultHandler {
        private Handler() {
        }

        @Override
        public void startDocument() {
        }

        @Override
        public void endDocument() {
        }

        @Override
        public void setDocumentLocator(final Locator locator) {
            TTXFile.this.locator = locator;
        }

        @Override
        public void startElement(final String uri, final String localName,
                final String qName, final Attributes attrs) throws SAXException {
            final String[] en = makeExpandedName(uri, localName, qName);
            if (en[0] != null) {
                unsupportedElement(en);
            } else if (en[1].equals("Alternate")) {
                final String[] pn = new String[] { null, "AlternateSet" };
                if (isParent(pn)) {
                    final String glyph = attrs.getValue("glyph");
                    if (glyph == null) {
                        missingRequiredAttribute(en, "glyph");
                    }
                    final int gid = mapGlyphId(glyph, en);
                    TTXFile.this.alternates.add(Integer.valueOf(gid));
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("AlternateSet")) {
                final String[] pn = new String[] { null, "AlternateSubst" };
                if (isParent(pn)) {
                    final String glyph = attrs.getValue("glyph");
                    if (glyph == null) {
                        missingRequiredAttribute(en, "glyph");
                    }
                    final int gid = mapGlyphId(glyph, en);
                    TTXFile.this.coverageEntries.add(Integer.valueOf(gid));
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("AlternateSubst")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = 0;
                    TTXFile.this.ctFormat = 1;
                    assertSubtableClear();
                    assert sf >= 0;
                    TTXFile.this.stFormat = sf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("BacktrackCoverage")) {
                final String[] pn1 = new String[] { null, "ChainContextSubst" };
                final String[] pn2 = new String[] { null, "ChainContextPos" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pnx)) {
                    final String index = attrs.getValue("index");
                    int ci = -1;
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    } else {
                        ci = Integer.parseInt(index);
                    }
                    final String format = attrs.getValue("Format");
                    int cf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        cf = Integer.parseInt(format);
                        switch (cf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, cf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = ci;
                    TTXFile.this.ctFormat = cf;
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("BaseAnchor")) {
                final String[] pn = new String[] { null, "BaseRecord" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    }
                    assert TTXFile.this.xCoord == Integer.MIN_VALUE;
                    assert TTXFile.this.yCoord == Integer.MIN_VALUE;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("BaseArray")) {
                final String[] pn = new String[] { null, "MarkBasePos" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("BaseCoverage")) {
                final String[] pn = new String[] { null, "MarkBasePos" };
                if (isParent(pn)) {
                    final String format = attrs.getValue("Format");
                    int cf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        cf = Integer.parseInt(format);
                        switch (cf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, cf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = 0;
                    TTXFile.this.ctFormat = cf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("BaseRecord")) {
                final String[] pn = new String[] { null, "BaseArray" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("ChainContextPos")
                    || en[1].equals("ChainContextSubst")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                        case 2:
                        case 3:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    TTXFile.this.stFormat = sf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Class")) {
                final String[] pn = new String[] { null, "MarkRecord" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    int v = -1;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        v = Integer.parseInt(value);
                    }
                    assert TTXFile.this.markClass == -1;
                    TTXFile.this.markClass = v;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("ClassDef")) {
                final String[] pn1 = new String[] { null, "GlyphClassDef" };
                final String[] pn2 = new String[] { null, "MarkAttachClassDef" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pnx)) {
                    final String glyph = attrs.getValue("glyph");
                    if (glyph == null) {
                        missingRequiredAttribute(en, "glyph");
                    }
                    final String glyphClass = attrs.getValue("class");
                    if (glyphClass == null) {
                        missingRequiredAttribute(en, "class");
                    }
                    if (!TTXFile.this.glyphIds.containsKey(glyph)) {
                        unsupportedGlyph(en, glyph);
                    } else if (isParent(pn1)) {
                        if (TTXFile.this.glyphClasses.containsKey(glyph)) {
                            duplicateGlyphClass(en, glyph, glyphClass);
                        } else {
                            TTXFile.this.glyphClasses.put(glyph,
                                    Integer.parseInt(glyphClass));
                        }
                    } else if (isParent(pn2)) {
                        if (TTXFile.this.glyphClasses.containsKey(glyph)) {
                            duplicateGlyphClass(en, glyph, glyphClass);
                        } else {
                            TTXFile.this.glyphClasses.put(glyph,
                                    Integer.parseInt(glyphClass));
                        }
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("ComponentRecord")) {
                final String[] pn = new String[] { null, "LigatureAttach" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    assert TTXFile.this.anchors.size() == 0;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Coverage")) {
                final String[] pn1 = new String[] { null, "CursivePos" };
                final String[] pn2 = new String[] { null, "LigCaretList" };
                final String[] pn3 = new String[] { null, "MultipleSubst" };
                final String[] pn4 = new String[] { null, "PairPos" };
                final String[] pn5 = new String[] { null, "SinglePos" };
                final String[][] pnx = new String[][] { pn1, pn2, pn3, pn4, pn5 };
                if (isParent(pnx)) {
                    final String format = attrs.getValue("Format");
                    int cf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        cf = Integer.parseInt(format);
                        switch (cf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, cf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = 0;
                    TTXFile.this.ctFormat = cf;
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("CursivePos")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    TTXFile.this.stFormat = sf;
                    assert TTXFile.this.attachmentAnchors.size() == 0;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("DefaultLangSys")) {
                final String[] pn = new String[] { null, "Script" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                } else {
                    assertLanguageFeaturesClear();
                    assert TTXFile.this.languageTag == null;
                    TTXFile.this.languageTag = TTXFile.this.defaultLanguageTag;
                }
            } else if (en[1].equals("EntryAnchor")) {
                final String[] pn = new String[] { null, "EntryExitRecord" };
                if (isParent(pn)) {
                    final String format = attrs.getValue("Format");
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    }
                    assert TTXFile.this.xCoord == Integer.MIN_VALUE;
                    assert TTXFile.this.yCoord == Integer.MIN_VALUE;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("EntryExitRecord")) {
                final String[] pn = new String[] { null, "CursivePos" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("ExitAnchor")) {
                final String[] pn = new String[] { null, "EntryExitRecord" };
                if (isParent(pn)) {
                    final String format = attrs.getValue("Format");
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    }
                    assert TTXFile.this.xCoord == Integer.MIN_VALUE;
                    assert TTXFile.this.yCoord == Integer.MIN_VALUE;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Feature")) {
                final String[] pn = new String[] { null, "FeatureRecord" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                } else {
                    assertFeatureLookupsClear();
                }
            } else if (en[1].equals("FeatureIndex")) {
                final String[] pn1 = new String[] { null, "DefaultLangSys" };
                final String[] pn2 = new String[] { null, "LangSys" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pnx)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String value = attrs.getValue("value");
                    int v = -1;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        v = Integer.parseInt(value);
                    }
                    if (TTXFile.this.languageFeatures.size() == 0) {
                        TTXFile.this.languageFeatures.add(null);
                    }
                    if (v >= 0 && v < 65535) {
                        TTXFile.this.languageFeatures.add(makeFeatureId(v));
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("FeatureList")) {
                final String[] pn1 = new String[] { null, "GSUB" };
                final String[] pn2 = new String[] { null, "GPOS" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (!isParent(pnx)) {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("FeatureRecord")) {
                final String[] pn = new String[] { null, "FeatureList" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    int fi = -1;
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    } else {
                        fi = Integer.parseInt(index);
                    }
                    assertFeatureClear();
                    assert TTXFile.this.flIndex == -1;
                    TTXFile.this.flIndex = fi;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("FeatureTag")) {
                final String[] pn = new String[] { null, "FeatureRecord" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        assert TTXFile.this.featureTag == null;
                        TTXFile.this.featureTag = value;
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("GDEF")) {
                final String[] pn = new String[] { null, "ttFont" };
                if (isParent(pn)) {
                    assertSubtablesClear();
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("GPOS")) {
                final String[] pn = new String[] { null, "ttFont" };
                if (isParent(pn)) {
                    assertCoveragesClear();
                    assertSubtablesClear();
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("GSUB")) {
                final String[] pn = new String[] { null, "ttFont" };
                if (isParent(pn)) {
                    assertCoveragesClear();
                    assertSubtablesClear();
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Glyph")) {
                final String[] pn1 = new String[] { null, "Coverage" };
                final String[] pn2 = new String[] { null, "InputCoverage" };
                final String[] pn3 = new String[] { null, "LookAheadCoverage" };
                final String[] pn4 = new String[] { null, "BacktrackCoverage" };
                final String[] pn5 = new String[] { null, "MarkCoverage" };
                final String[] pn6 = new String[] { null, "Mark1Coverage" };
                final String[] pn7 = new String[] { null, "Mark2Coverage" };
                final String[] pn8 = new String[] { null, "BaseCoverage" };
                final String[] pn9 = new String[] { null, "LigatureCoverage" };
                final String[][] pnx = new String[][] { pn1, pn2, pn3, pn4,
                        pn5, pn6, pn7, pn8, pn9 };
                if (isParent(pnx)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        final int gid = mapGlyphId(value, en);
                        TTXFile.this.coverageEntries.add(Integer.valueOf(gid));
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("GlyphClassDef")) {
                final String[] pn = new String[] { null, "GDEF" };
                if (isParent(pn)) {
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    // force format 1 since TTX always writes entries as
                    // non-range entries
                    if (sf != 1) {
                        sf = 1;
                    }
                    TTXFile.this.stFormat = sf;
                    assert TTXFile.this.glyphClasses.isEmpty();
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("GlyphID")) {
                final String[] pn = new String[] { null, "GlyphOrder" };
                if (isParent(pn)) {
                    final String id = attrs.getValue("id");
                    int gid = -1;
                    if (id == null) {
                        missingRequiredAttribute(en, "id");
                    } else {
                        gid = Integer.parseInt(id);
                    }
                    final String name = attrs.getValue("name");
                    if (name == null) {
                        missingRequiredAttribute(en, "name");
                    }
                    if (TTXFile.this.glyphIds.containsKey(name)) {
                        duplicateGlyph(en, name, gid);
                    } else {
                        if (gid > TTXFile.this.glyphIdMax) {
                            TTXFile.this.glyphIdMax = gid;
                        }
                        TTXFile.this.glyphIds.put(name, gid);
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("GlyphOrder")) {
                final String[] pn = new String[] { null, "ttFont" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("InputCoverage")) {
                final String[] pn1 = new String[] { null, "ChainContextSubst" };
                final String[] pn2 = new String[] { null, "ChainContextPos" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pnx)) {
                    final String index = attrs.getValue("index");
                    int ci = -1;
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    } else {
                        ci = Integer.parseInt(index);
                    }
                    final String format = attrs.getValue("Format");
                    int cf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        cf = Integer.parseInt(format);
                        switch (cf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, cf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = ci;
                    TTXFile.this.ctFormat = cf;
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("LangSys")) {
                final String[] pn = new String[] { null, "LangSysRecord" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                } else {
                    assertLanguageFeaturesClear();
                }
            } else if (en[1].equals("LangSysRecord")) {
                final String[] pn = new String[] { null, "Script" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("LangSysTag")) {
                final String[] pn = new String[] { null, "LangSysRecord" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        assert TTXFile.this.languageTag == null;
                        TTXFile.this.languageTag = value;
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("LigCaretList")) {
                final String[] pn = new String[] { null, "GDEF" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Ligature")) {
                final String[] pn = new String[] { null, "LigatureSet" };
                if (isParent(pn)) {
                    final String components = attrs.getValue("components");
                    if (components == null) {
                        missingRequiredAttribute(en, "components");
                    }
                    final int[] cids = mapGlyphIds(components, en);
                    final String glyph = attrs.getValue("glyph");
                    if (glyph == null) {
                        missingRequiredAttribute(en, "glyph");
                    }
                    final int gid = mapGlyphId(glyph, en);
                    TTXFile.this.ligatures.add(new Ligature(gid, cids));
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("LigatureAnchor")) {
                final String[] pn = new String[] { null, "ComponentRecord" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    }
                    assert TTXFile.this.xCoord == Integer.MIN_VALUE;
                    assert TTXFile.this.yCoord == Integer.MIN_VALUE;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("LigatureArray")) {
                final String[] pn = new String[] { null, "MarkLigPos" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("LigatureAttach")) {
                final String[] pn = new String[] { null, "LigatureArray" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    assert TTXFile.this.components.size() == 0;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("LigatureCoverage")) {
                final String[] pn = new String[] { null, "MarkLigPos" };
                if (isParent(pn)) {
                    final String format = attrs.getValue("Format");
                    int cf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        cf = Integer.parseInt(format);
                        switch (cf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, cf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = 0;
                    TTXFile.this.ctFormat = cf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("LigatureSet")) {
                final String[] pn = new String[] { null, "LigatureSubst" };
                if (isParent(pn)) {
                    final String glyph = attrs.getValue("glyph");
                    if (glyph == null) {
                        missingRequiredAttribute(en, "glyph");
                    }
                    final int gid = mapGlyphId(glyph, en);
                    TTXFile.this.coverageEntries.add(Integer.valueOf(gid));
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("LigatureSubst")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = 0;
                    TTXFile.this.ctFormat = 1;
                    assertSubtableClear();
                    assert sf >= 0;
                    TTXFile.this.stFormat = sf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("LookAheadCoverage")) {
                final String[] pn1 = new String[] { null, "ChainContextSubst" };
                final String[] pn2 = new String[] { null, "ChainContextPos" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pnx)) {
                    final String index = attrs.getValue("index");
                    int ci = -1;
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    } else {
                        ci = Integer.parseInt(index);
                    }
                    final String format = attrs.getValue("Format");
                    int cf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        cf = Integer.parseInt(format);
                        switch (cf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, cf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = ci;
                    TTXFile.this.ctFormat = cf;
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("Lookup")) {
                final String[] pn = new String[] { null, "LookupList" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    int li = -1;
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    } else {
                        li = Integer.parseInt(index);
                    }
                    assertLookupClear();
                    assert TTXFile.this.ltIndex == -1;
                    TTXFile.this.ltIndex = li;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("LookupFlag")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    int lf = 0;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        lf = Integer.parseInt(value);
                    }
                    assert TTXFile.this.ltFlags == 0;
                    TTXFile.this.ltFlags = lf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("LookupList")) {
                final String[] pn1 = new String[] { null, "GSUB" };
                final String[] pn2 = new String[] { null, "GPOS" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (!isParent(pnx)) {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("LookupListIndex")) {
                final String[] pn1 = new String[] { null, "Feature" };
                final String[] pn2 = new String[] { null, "SubstLookupRecord" };
                final String[] pn3 = new String[] { null, "PosLookupRecord" };
                final String[][] pnx = new String[][] { pn1, pn2, pn3 };
                if (isParent(pnx)) {
                    final String index = attrs.getValue("index");
                    final String value = attrs.getValue("value");
                    int v = -1;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        v = Integer.parseInt(value);
                    }
                    final String[][] pny = new String[][] { pn2, pn3 };
                    if (isParent(pny)) {
                        assert TTXFile.this.rlLookup == -1;
                        assert v != -1;
                        TTXFile.this.rlLookup = v;
                    } else {
                        TTXFile.this.featureLookups.add(makeLookupId(v));
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("LookupType")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Mark1Array")) {
                final String[] pn = new String[] { null, "MarkMarkPos" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Mark1Coverage")) {
                final String[] pn = new String[] { null, "MarkMarkPos" };
                if (isParent(pn)) {
                    final String format = attrs.getValue("Format");
                    int cf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        cf = Integer.parseInt(format);
                        switch (cf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, cf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = 0;
                    TTXFile.this.ctFormat = cf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Mark2Anchor")) {
                final String[] pn = new String[] { null, "Mark2Record" };
                if (isParent(pn)) {
                    final String format = attrs.getValue("Format");
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    }
                    assert TTXFile.this.xCoord == Integer.MIN_VALUE;
                    assert TTXFile.this.yCoord == Integer.MIN_VALUE;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Mark2Array")) {
                final String[] pn = new String[] { null, "MarkMarkPos" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Mark2Coverage")) {
                final String[] pn = new String[] { null, "MarkMarkPos" };
                if (isParent(pn)) {
                    final String format = attrs.getValue("Format");
                    int cf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        cf = Integer.parseInt(format);
                        switch (cf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, cf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = 0;
                    TTXFile.this.ctFormat = cf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Mark2Record")) {
                final String[] pn = new String[] { null, "Mark2Array" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("MarkAnchor")) {
                final String[] pn = new String[] { null, "MarkRecord" };
                if (isParent(pn)) {
                    final String format = attrs.getValue("Format");
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    }
                    assert TTXFile.this.xCoord == Integer.MIN_VALUE;
                    assert TTXFile.this.yCoord == Integer.MIN_VALUE;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("MarkArray")) {
                final String[] pn1 = new String[] { null, "MarkBasePos" };
                final String[] pn2 = new String[] { null, "MarkLigPos" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (!isParent(pnx)) {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("MarkAttachClassDef")) {
                final String[] pn = new String[] { null, "GDEF" };
                if (isParent(pn)) {
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    // force format 1 since TTX always writes entries as
                    // non-range entries
                    if (sf != 1) {
                        sf = 1;
                    }
                    TTXFile.this.stFormat = sf;
                    assert TTXFile.this.glyphClasses.isEmpty();
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("MarkBasePos")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    TTXFile.this.stFormat = sf;
                    assert TTXFile.this.markAnchors.size() == 0;
                    assert TTXFile.this.baseOrMarkAnchors.size() == 0;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("MarkCoverage")) {
                final String[] pn1 = new String[] { null, "MarkBasePos" };
                final String[] pn2 = new String[] { null, "MarkLigPos" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pnx)) {
                    final String format = attrs.getValue("Format");
                    int cf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        cf = Integer.parseInt(format);
                        switch (cf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, cf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = 0;
                    TTXFile.this.ctFormat = cf;
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("MarkLigPos")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    TTXFile.this.stFormat = sf;
                    assert TTXFile.this.markAnchors.size() == 0;
                    assert TTXFile.this.ligatureAnchors.size() == 0;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("MarkMarkPos")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    TTXFile.this.stFormat = sf;
                    assert TTXFile.this.markAnchors.size() == 0;
                    assert TTXFile.this.baseOrMarkAnchors.size() == 0;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("MarkRecord")) {
                final String[] pn1 = new String[] { null, "MarkArray" };
                final String[] pn2 = new String[] { null, "Mark1Array" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pnx)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("MultipleSubst")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    TTXFile.this.stFormat = sf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("PairPos")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    TTXFile.this.stFormat = sf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("PairSet")) {
                final String[] pn = new String[] { null, "PairPos" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    int psi = -1;
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    } else {
                        psi = Integer.parseInt(index);
                    }
                    assert TTXFile.this.psIndex == -1;
                    TTXFile.this.psIndex = psi;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("PairValueRecord")) {
                final String[] pn = new String[] { null, "PairSet" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    } else {
                        assertPairClear();
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("PosLookupRecord")) {
                final String[] pn1 = new String[] { null, "ChainContextSubst" };
                final String[] pn2 = new String[] { null, "ChainContextPos" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pnx)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("ReqFeatureIndex")) {
                final String[] pn1 = new String[] { null, "DefaultLangSys" };
                final String[] pn2 = new String[] { null, "LangSys" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pnx)) {
                    final String value = attrs.getValue("value");
                    int v = -1;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        v = Integer.parseInt(value);
                    }
                    String fid;
                    if (v >= 0 && v < 65535) {
                        fid = makeFeatureId(v);
                    } else {
                        fid = null;
                    }
                    assertLanguageFeaturesClear();
                    TTXFile.this.languageFeatures.add(fid);
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("Script")) {
                final String[] pn = new String[] { null, "ScriptRecord" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("ScriptList")) {
                final String[] pn1 = new String[] { null, "GSUB" };
                final String[] pn2 = new String[] { null, "GPOS" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (!isParent(pnx)) {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("ScriptRecord")) {
                final String[] pn = new String[] { null, "ScriptList" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("ScriptTag")) {
                final String[] pn = new String[] { null, "ScriptRecord" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        assert TTXFile.this.scriptTag == null;
                        TTXFile.this.scriptTag = value;
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("SecondGlyph")) {
                final String[] pn = new String[] { null, "PairValueRecord" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        final int gid = mapGlyphId(value, en);
                        assert TTXFile.this.g2 == -1;
                        TTXFile.this.g2 = gid;
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Sequence")) {
                final String[] pn = new String[] { null, "MultipleSubst" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    } else {
                        final int i = Integer.parseInt(index);
                        if (i != TTXFile.this.subtableEntries.size()) {
                            invalidIndex(en, i,
                                    TTXFile.this.subtableEntries.size());
                        }
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("SequenceIndex")) {
                final String[] pn1 = new String[] { null, "PosLookupRecord" };
                final String[] pn2 = new String[] { null, "SubstLookupRecord" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pnx)) {
                    final String value = attrs.getValue("value");
                    int v = -1;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        v = Integer.parseInt(value);
                    }
                    assert TTXFile.this.rlSequence == -1;
                    assert v != -1;
                    TTXFile.this.rlSequence = v;
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("SinglePos")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertSubtableClear();
                    assert sf >= 0;
                    TTXFile.this.stFormat = sf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("SingleSubst")) {
                final String[] pn = new String[] { null, "Lookup" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                    final String format = attrs.getValue("Format");
                    int sf = -1;
                    if (format == null) {
                        missingRequiredAttribute(en, "Format");
                    } else {
                        sf = Integer.parseInt(format);
                        switch (sf) {
                        case 1:
                        case 2:
                            break;
                        default:
                            unsupportedFormat(en, sf);
                            break;
                        }
                    }
                    assertCoverageClear();
                    TTXFile.this.ctIndex = 0;
                    TTXFile.this.ctFormat = 1;
                    assertSubtableClear();
                    assert sf >= 0;
                    TTXFile.this.stFormat = sf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("SubstLookupRecord")) {
                final String[] pn = new String[] { null, "ChainContextSubst" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Substitute")) {
                final String[] pn = new String[] { null, "Sequence" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (index == null) {
                        missingRequiredAttribute(en, "index");
                    } else {
                        final int i = Integer.parseInt(index);
                        if (i != TTXFile.this.substitutes.size()) {
                            invalidIndex(en, i, TTXFile.this.substitutes.size());
                        }
                    }
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        final int gid = mapGlyphId(value, en);
                        TTXFile.this.substitutes.add(Integer.valueOf(gid));
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Substitution")) {
                final String[] pn = new String[] { null, "SingleSubst" };
                if (isParent(pn)) {
                    final String in = attrs.getValue("in");
                    int igid = -1;
                    int ogid = -1;
                    if (in == null) {
                        missingRequiredAttribute(en, "in");
                    } else {
                        igid = mapGlyphId(in, en);
                    }
                    final String out = attrs.getValue("out");
                    if (out == null) {
                        missingRequiredAttribute(en, "out");
                    } else {
                        ogid = mapGlyphId(out, en);
                    }
                    TTXFile.this.coverageEntries.add(Integer.valueOf(igid));
                    TTXFile.this.subtableEntries.add(Integer.valueOf(ogid));
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Value")) {
                final String[] pn = new String[] { null, "SinglePos" };
                if (isParent(pn)) {
                    final String index = attrs.getValue("index");
                    if (TTXFile.this.vf1 < 0) {
                        missingParameter(en, "value format");
                    } else {
                        TTXFile.this.subtableEntries.add(parseValue(en, attrs,
                                TTXFile.this.vf1));
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Value1")) {
                final String[] pn = new String[] { null, "PairValueRecord" };
                if (isParent(pn)) {
                    if (TTXFile.this.vf1 < 0) {
                        missingParameter(en, "value format 1");
                    } else {
                        assert TTXFile.this.v1 == null;
                        TTXFile.this.v1 = parseValue(en, attrs,
                                TTXFile.this.vf1);
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Value2")) {
                final String[] pn = new String[] { null, "PairValueRecord" };
                if (isParent(pn)) {
                    if (TTXFile.this.vf2 < 0) {
                        missingParameter(en, "value format 2");
                    } else {
                        assert TTXFile.this.v2 == null;
                        TTXFile.this.v2 = parseValue(en, attrs,
                                TTXFile.this.vf2);
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("ValueFormat")) {
                final String[] pn = new String[] { null, "SinglePos" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    int vf = -1;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        vf = Integer.parseInt(value);
                    }
                    assert TTXFile.this.vf1 == -1;
                    TTXFile.this.vf1 = vf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("ValueFormat1")) {
                final String[] pn = new String[] { null, "PairPos" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    int vf = -1;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        vf = Integer.parseInt(value);
                    }
                    assert TTXFile.this.vf1 == -1;
                    TTXFile.this.vf1 = vf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("ValueFormat2")) {
                final String[] pn = new String[] { null, "PairPos" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    int vf = -1;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        vf = Integer.parseInt(value);
                    }
                    assert TTXFile.this.vf2 == -1;
                    TTXFile.this.vf2 = vf;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("Version")) {
                final String[] pn1 = new String[] { null, "GDEF" };
                final String[] pn2 = new String[] { null, "GPOS" };
                final String[] pn3 = new String[] { null, "GSUB" };
                final String[][] pnx = new String[][] { pn1, pn2, pn3 };
                if (isParent(pnx)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("XCoordinate")) {
                final String[] pn1 = new String[] { null, "BaseAnchor" };
                final String[] pn2 = new String[] { null, "EntryAnchor" };
                final String[] pn3 = new String[] { null, "ExitAnchor" };
                final String[] pn4 = new String[] { null, "LigatureAnchor" };
                final String[] pn5 = new String[] { null, "MarkAnchor" };
                final String[] pn6 = new String[] { null, "Mark2Anchor" };
                final String[][] pnx = new String[][] { pn1, pn2, pn3, pn4,
                        pn5, pn6 };
                if (isParent(pnx)) {
                    final String value = attrs.getValue("value");
                    int x = 0;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        x = Integer.parseInt(value);
                    }
                    assert TTXFile.this.xCoord == Integer.MIN_VALUE;
                    TTXFile.this.xCoord = x;
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("YCoordinate")) {
                final String[] pn1 = new String[] { null, "BaseAnchor" };
                final String[] pn2 = new String[] { null, "EntryAnchor" };
                final String[] pn3 = new String[] { null, "ExitAnchor" };
                final String[] pn4 = new String[] { null, "LigatureAnchor" };
                final String[] pn5 = new String[] { null, "MarkAnchor" };
                final String[] pn6 = new String[] { null, "Mark2Anchor" };
                final String[][] pnx = new String[][] { pn1, pn2, pn3, pn4,
                        pn5, pn6 };
                if (isParent(pnx)) {
                    final String value = attrs.getValue("value");
                    int y = 0;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        y = Integer.parseInt(value);
                    }
                    assert TTXFile.this.yCoord == Integer.MIN_VALUE;
                    TTXFile.this.yCoord = y;
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("checkSumAdjustment")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("cmap")) {
                final String[] pn = new String[] { null, "ttFont" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("cmap_format_0")) {
                final String[] pn = new String[] { null, "cmap" };
                if (isParent(pn)) {
                    final String platformID = attrs.getValue("platformID");
                    if (platformID == null) {
                        missingRequiredAttribute(en, "platformID");
                    }
                    final String platEncID = attrs.getValue("platEncID");
                    if (platEncID == null) {
                        missingRequiredAttribute(en, "platEncID");
                    }
                    final String language = attrs.getValue("language");
                    if (language == null) {
                        missingRequiredAttribute(en, "language");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("cmap_format_4")) {
                final String[] pn = new String[] { null, "cmap" };
                if (isParent(pn)) {
                    final String platformID = attrs.getValue("platformID");
                    int pid = -1;
                    if (platformID == null) {
                        missingRequiredAttribute(en, "platformID");
                    } else {
                        pid = Integer.parseInt(platformID);
                    }
                    final String platEncID = attrs.getValue("platEncID");
                    int eid = -1;
                    if (platEncID == null) {
                        missingRequiredAttribute(en, "platEncID");
                    } else {
                        eid = Integer.parseInt(platEncID);
                    }
                    final String language = attrs.getValue("language");
                    int lid = -1;
                    if (language == null) {
                        missingRequiredAttribute(en, "language");
                    } else {
                        lid = Integer.parseInt(language);
                    }
                    assert TTXFile.this.cmapEntries.size() == 0;
                    assert TTXFile.this.cmPlatform == -1;
                    assert TTXFile.this.cmEncoding == -1;
                    assert TTXFile.this.cmLanguage == -1;
                    TTXFile.this.cmPlatform = pid;
                    TTXFile.this.cmEncoding = eid;
                    TTXFile.this.cmLanguage = lid;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("created")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("flags")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("fontDirectionHint")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("fontRevision")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("glyphDataFormat")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("head")) {
                final String[] pn = new String[] { null, "ttFont" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("hmtx")) {
                final String[] pn = new String[] { null, "ttFont" };
                if (!isParent(pn)) {
                    notPermittedInElementContext(en, getParent(), pn);
                } else if (TTXFile.this.glyphIdMax > 0) {
                    // TODO fixme setSize in Vector ==> ? in List
                    // TTXFile.this.hmtxEntries
                    // .setSize(TTXFile.this.glyphIdMax + 1);
                }
            } else if (en[1].equals("indexToLocFormat")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("lowestRecPPEM")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("macStyle")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("magicNumber")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("map")) {
                final String[] pn1 = new String[] { null, "cmap_format_0" };
                final String[] pn2 = new String[] { null, "cmap_format_4" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pnx)) {
                    String code = attrs.getValue("code");
                    int cid = -1;
                    if (code == null) {
                        missingRequiredAttribute(en, "code");
                    } else {
                        code = code.toLowerCase();
                        if (code.startsWith("0x")) {
                            cid = Integer.parseInt(code.substring(2), 16);
                        } else {
                            cid = Integer.parseInt(code, 10);
                        }
                    }
                    final String name = attrs.getValue("name");
                    int gid = -1;
                    if (name == null) {
                        missingRequiredAttribute(en, "name");
                    } else {
                        gid = mapGlyphId(name, en);
                    }
                    if (TTXFile.this.cmPlatform == 3
                            && TTXFile.this.cmEncoding == 1) {
                        TTXFile.this.cmapEntries.add(new int[] { cid, gid });
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("modified")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("mtx")) {
                final String[] pn = new String[] { null, "hmtx" };
                if (isParent(pn)) {
                    final String name = attrs.getValue("name");
                    int gid = -1;
                    if (name == null) {
                        missingRequiredAttribute(en, "name");
                    } else {
                        gid = mapGlyphId(name, en);
                    }
                    final String width = attrs.getValue("width");
                    int w = -1;
                    if (width == null) {
                        missingRequiredAttribute(en, "width");
                    } else {
                        w = Integer.parseInt(width);
                    }
                    final String lsb = attrs.getValue("lsb");
                    int l = -1;
                    if (lsb == null) {
                        missingRequiredAttribute(en, "lsb");
                    } else {
                        l = Integer.parseInt(lsb);
                    }
                    TTXFile.this.hmtxEntries.set(gid, new int[] { w, l });
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("tableVersion")) {
                final String[] pn1 = new String[] { null, "cmap" };
                final String[] pn2 = new String[] { null, "head" };
                final String[][] pnx = new String[][] { pn1, pn2 };
                if (isParent(pn1)) { // child of cmap
                    final String version = attrs.getValue("version");
                    if (version == null) {
                        missingRequiredAttribute(en, "version");
                    }
                } else if (isParent(pn2)) { // child of head
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pnx);
                }
            } else if (en[1].equals("ttFont")) {
                final String[] pn = new String[] { null, null };
                if (isParent(pn)) {
                    final String sfntVersion = attrs.getValue("sfntVersion");
                    if (sfntVersion == null) {
                        missingRequiredAttribute(en, "sfntVersion");
                    }
                    final String ttLibVersion = attrs.getValue("ttLibVersion");
                    if (ttLibVersion == null) {
                        missingRequiredAttribute(en, "ttLibVersion");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), null);
                }
            } else if (en[1].equals("unitsPerEm")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    int v = -1;
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    } else {
                        v = Integer.parseInt(value);
                    }
                    assert TTXFile.this.upem == -1;
                    TTXFile.this.upem = v;
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("xMax")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("xMin")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("yMax")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else if (en[1].equals("yMin")) {
                final String[] pn = new String[] { null, "head" };
                if (isParent(pn)) {
                    final String value = attrs.getValue("value");
                    if (value == null) {
                        missingRequiredAttribute(en, "value");
                    }
                } else {
                    notPermittedInElementContext(en, getParent(), pn);
                }
            } else {
                unsupportedElement(en);
            }
            TTXFile.this.elements.push(en);
        }

        @Override
        public void endElement(final String uri, final String localName,
                final String qName) throws SAXException {
            if (TTXFile.this.elements.empty()) {
                throw new SAXException(
                        "element stack is unbalanced, no elements on stack!");
            }
            final String[] enParent = TTXFile.this.elements.peek();
            if (enParent == null) {
                throw new SAXException(
                        "element stack is empty, elements are not balanced");
            }
            final String[] en = makeExpandedName(uri, localName, qName);
            if (!sameExpandedName(enParent, en)) {
                throw new SAXException(
                        "element stack is unbalanced, expanded name mismatch");
            }
            if (en[0] != null) {
                unsupportedElement(en);
            } else if (isAnchorElement(en[1])) {
                if (TTXFile.this.xCoord == Integer.MIN_VALUE) {
                    missingParameter(en, "x coordinate");
                } else if (TTXFile.this.yCoord == Integer.MIN_VALUE) {
                    missingParameter(en, "y coordinate");
                } else {
                    if (en[1].equals("EntryAnchor")) {
                        if (TTXFile.this.anchors.size() > 0) {
                            duplicateParameter(en, "entry anchor");
                        }
                    } else if (en[1].equals("ExitAnchor")) {
                        if (TTXFile.this.anchors.size() > 1) {
                            duplicateParameter(en, "exit anchor");
                        } else if (TTXFile.this.anchors.size() == 0) {
                            TTXFile.this.anchors.add(null);
                        }
                    }
                    TTXFile.this.anchors.add(new GlyphPositioningTable.Anchor(
                            TTXFile.this.xCoord, TTXFile.this.yCoord));
                    TTXFile.this.xCoord = TTXFile.this.yCoord = Integer.MIN_VALUE;
                }
            } else if (en[1].equals("AlternateSet")) {
                TTXFile.this.subtableEntries.add(extractAlternates());
            } else if (en[1].equals("AlternateSubst")) {
                if (!sortEntries(TTXFile.this.coverageEntries,
                        TTXFile.this.subtableEntries)) {
                    mismatchedEntries(en, TTXFile.this.coverageEntries.size(),
                            TTXFile.this.subtableEntries.size());
                }
                addGSUBSubtable(
                        GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_ALTERNATE,
                        extractCoverage());
            } else if (en[1].equals("BacktrackCoverage")) {
                final String ck = makeCoverageKey("bk", TTXFile.this.ctIndex);
                if (TTXFile.this.coverages.containsKey(ck)) {
                    duplicateCoverageIndex(en, TTXFile.this.ctIndex);
                } else {
                    TTXFile.this.coverages.put(ck, extractCoverage());
                }
            } else if (en[1].equals("BaseCoverage")) {
                TTXFile.this.coverages.put("base", extractCoverage());
            } else if (en[1].equals("BaseRecord")) {
                TTXFile.this.baseOrMarkAnchors.add(extractAnchors());
            } else if (en[1].equals("ChainContextPos")
                    || en[1].equals("ChainContextSubst")) {
                GlyphCoverageTable coverage = null;
                if (TTXFile.this.stFormat == 3) {
                    final GlyphCoverageTable igca[] = getCoveragesWithPrefix("in");
                    final GlyphCoverageTable bgca[] = getCoveragesWithPrefix("bk");
                    final GlyphCoverageTable lgca[] = getCoveragesWithPrefix("la");
                    if (igca.length == 0 || hasMissingCoverage(igca)) {
                        missingCoverage(en, "input", igca.length);
                    } else if (hasMissingCoverage(bgca)) {
                        missingCoverage(en, "backtrack", bgca.length);
                    } else if (hasMissingCoverage(lgca)) {
                        missingCoverage(en, "lookahead", lgca.length);
                    } else {
                        final GlyphTable.Rule r = new GlyphTable.ChainedCoverageSequenceRule(
                                extractRuleLookups(), igca.length, igca, bgca,
                                lgca);
                        final GlyphTable.RuleSet rs = new GlyphTable.HomogeneousRuleSet(
                                new GlyphTable.Rule[] { r });
                        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[] { rs };
                        coverage = igca[0];
                        TTXFile.this.subtableEntries.add(rsa);
                    }
                } else {
                    unsupportedFormat(en, TTXFile.this.stFormat);
                }
                if (en[1].equals("ChainContextPos")) {
                    addGPOSSubtable(
                            GlyphPositioningTable.GPOS_LOOKUP_TYPE_CHAINED_CONTEXTUAL,
                            coverage);
                } else if (en[1].equals("ChainContextSubst")) {
                    addGSUBSubtable(
                            GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_CHAINED_CONTEXTUAL,
                            coverage);
                }
            } else if (en[1].equals("ComponentRecord")) {
                TTXFile.this.components.add(extractAnchors());
            } else if (en[1].equals("Coverage")) {
                TTXFile.this.coverages.put("main", extractCoverage());
            } else if (en[1].equals("DefaultLangSys")
                    || en[1].equals("LangSysRecord")) {
                if (TTXFile.this.languageTag == null) {
                    missingTag(en, "language");
                } else if (TTXFile.this.languages
                        .containsKey(TTXFile.this.languageTag)) {
                    duplicateTag(en, "language", TTXFile.this.languageTag);
                } else {
                    TTXFile.this.languages.put(TTXFile.this.languageTag,
                            extractLanguageFeatures());
                    TTXFile.this.languageTag = null;
                }
            } else if (en[1].equals("CursivePos")) {
                final GlyphCoverageTable ct = TTXFile.this.coverages
                        .get("main");
                if (ct == null) {
                    missingParameter(en, "coverages");
                } else if (TTXFile.this.stFormat == 1) {
                    TTXFile.this.subtableEntries
                    .add(extractAttachmentAnchors());
                } else {
                    unsupportedFormat(en, TTXFile.this.stFormat);
                }
                addGPOSSubtable(GlyphPositioningTable.GPOS_LOOKUP_TYPE_CURSIVE,
                        ct);
            } else if (en[1].equals("EntryExitRecord")) {
                final int na = TTXFile.this.anchors.size();
                if (na == 0) {
                    missingParameter(en, "entry or exit anchor");
                } else if (na == 1) {
                    TTXFile.this.anchors.add(null);
                } else if (na > 2) {
                    duplicateParameter(en, "entry or exit anchor");
                }
                TTXFile.this.attachmentAnchors.add(extractAnchors());
            } else if (en[1].equals("BaseRecord")) {
                TTXFile.this.baseOrMarkAnchors.add(extractAnchors());
            } else if (en[1].equals("FeatureRecord")) {
                if (TTXFile.this.flIndex != TTXFile.this.flSequence) {
                    mismatchedIndex(en, "feature", TTXFile.this.flIndex,
                            TTXFile.this.flSequence);
                } else if (TTXFile.this.featureTag == null) {
                    missingTag(en, "feature");
                } else {
                    final String fid = makeFeatureId(TTXFile.this.flIndex);
                    TTXFile.this.features.put(fid, extractFeature());
                    nextFeature();
                }
            } else if (en[1].equals("GDEF")) {
                if (TTXFile.this.subtables.size() > 0) {
                    TTXFile.this.gdef = new GlyphDefinitionTable(
                            TTXFile.this.subtables);
                }
                clearTable();
            } else if (en[1].equals("GPOS")) {
                if (TTXFile.this.subtables.size() > 0) {
                    TTXFile.this.gpos = new GlyphPositioningTable(
                            TTXFile.this.gdef, extractLookups(),
                            TTXFile.this.subtables);
                }
                clearTable();
            } else if (en[1].equals("GSUB")) {
                if (TTXFile.this.subtables.size() > 0) {
                    TTXFile.this.gsub = new GlyphSubstitutionTable(
                            TTXFile.this.gdef, extractLookups(),
                            TTXFile.this.subtables);
                }
                clearTable();
            } else if (en[1].equals("GlyphClassDef")) {
                final GlyphMappingTable mapping = extractClassDefMapping(
                        TTXFile.this.glyphClasses, TTXFile.this.stFormat, true);
                addGDEFSubtable(
                        GlyphDefinitionTable.GDEF_LOOKUP_TYPE_GLYPH_CLASS,
                        mapping);
            } else if (en[1].equals("InputCoverage")) {
                final String ck = makeCoverageKey("in", TTXFile.this.ctIndex);
                if (TTXFile.this.coverages.containsKey(ck)) {
                    duplicateCoverageIndex(en, TTXFile.this.ctIndex);
                } else {
                    TTXFile.this.coverages.put(ck, extractCoverage());
                }
            } else if (en[1].equals("LigatureAttach")) {
                TTXFile.this.ligatureAnchors.add(extractComponents());
            } else if (en[1].equals("LigatureCoverage")) {
                TTXFile.this.coverages.put("liga", extractCoverage());
            } else if (en[1].equals("LigatureSet")) {
                TTXFile.this.subtableEntries.add(extractLigatures());
            } else if (en[1].equals("LigatureSubst")) {
                if (!sortEntries(TTXFile.this.coverageEntries,
                        TTXFile.this.subtableEntries)) {
                    mismatchedEntries(en, TTXFile.this.coverageEntries.size(),
                            TTXFile.this.subtableEntries.size());
                }
                final GlyphCoverageTable coverage = extractCoverage();
                addGSUBSubtable(
                        GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_LIGATURE,
                        coverage);
            } else if (en[1].equals("LookAheadCoverage")) {
                final String ck = makeCoverageKey("la", TTXFile.this.ctIndex);
                if (TTXFile.this.coverages.containsKey(ck)) {
                    duplicateCoverageIndex(en, TTXFile.this.ctIndex);
                } else {
                    TTXFile.this.coverages.put(ck, extractCoverage());
                }
            } else if (en[1].equals("Lookup")) {
                if (TTXFile.this.ltIndex != TTXFile.this.ltSequence) {
                    mismatchedIndex(en, "lookup", TTXFile.this.ltIndex,
                            TTXFile.this.ltSequence);
                } else {
                    nextLookup();
                }
            } else if (en[1].equals("MarkAttachClassDef")) {
                final GlyphMappingTable mapping = extractClassDefMapping(
                        TTXFile.this.glyphClasses, TTXFile.this.stFormat, true);
                addGDEFSubtable(
                        GlyphDefinitionTable.GDEF_LOOKUP_TYPE_MARK_ATTACHMENT,
                        mapping);
            } else if (en[1].equals("MarkCoverage")) {
                TTXFile.this.coverages.put("mark", extractCoverage());
            } else if (en[1].equals("Mark1Coverage")) {
                TTXFile.this.coverages.put("mrk1", extractCoverage());
            } else if (en[1].equals("Mark2Coverage")) {
                TTXFile.this.coverages.put("mrk2", extractCoverage());
            } else if (en[1].equals("MarkBasePos")) {
                final GlyphCoverageTable mct = TTXFile.this.coverages
                        .get("mark");
                final GlyphCoverageTable bct = TTXFile.this.coverages
                        .get("base");
                if (mct == null) {
                    missingParameter(en, "mark coverages");
                } else if (bct == null) {
                    missingParameter(en, "base coverages");
                } else if (TTXFile.this.stFormat == 1) {
                    final MarkAnchor[] maa = extractMarkAnchors();
                    final Anchor[][] bam = extractBaseOrMarkAnchors();
                    TTXFile.this.subtableEntries.add(bct);
                    TTXFile.this.subtableEntries.add(computeClassCount(bam));
                    TTXFile.this.subtableEntries.add(maa);
                    TTXFile.this.subtableEntries.add(bam);
                } else {
                    unsupportedFormat(en, TTXFile.this.stFormat);
                }
                addGPOSSubtable(
                        GlyphPositioningTable.GPOS_LOOKUP_TYPE_MARK_TO_BASE,
                        mct);
            } else if (en[1].equals("MarkLigPos")) {
                final GlyphCoverageTable mct = TTXFile.this.coverages
                        .get("mark");
                final GlyphCoverageTable lct = TTXFile.this.coverages
                        .get("liga");
                if (mct == null) {
                    missingParameter(en, "mark coverages");
                } else if (lct == null) {
                    missingParameter(en, "ligature coverages");
                } else if (TTXFile.this.stFormat == 1) {
                    final MarkAnchor[] maa = extractMarkAnchors();
                    final Anchor[][][] lam = extractLigatureAnchors();
                    TTXFile.this.subtableEntries.add(lct);
                    TTXFile.this.subtableEntries
                    .add(computeLigaturesClassCount(lam));
                    TTXFile.this.subtableEntries
                    .add(computeLigaturesComponentCount(lam));
                    TTXFile.this.subtableEntries.add(maa);
                    TTXFile.this.subtableEntries.add(lam);
                } else {
                    unsupportedFormat(en, TTXFile.this.stFormat);
                }
                addGPOSSubtable(
                        GlyphPositioningTable.GPOS_LOOKUP_TYPE_MARK_TO_LIGATURE,
                        mct);
            } else if (en[1].equals("MarkMarkPos")) {
                final GlyphCoverageTable mct1 = TTXFile.this.coverages
                        .get("mrk1");
                final GlyphCoverageTable mct2 = TTXFile.this.coverages
                        .get("mrk2");
                if (mct1 == null) {
                    missingParameter(en, "mark coverages 1");
                } else if (mct2 == null) {
                    missingParameter(en, "mark coverages 2");
                } else if (TTXFile.this.stFormat == 1) {
                    final MarkAnchor[] maa = extractMarkAnchors();
                    final Anchor[][] mam = extractBaseOrMarkAnchors();
                    TTXFile.this.subtableEntries.add(mct2);
                    TTXFile.this.subtableEntries.add(computeClassCount(mam));
                    TTXFile.this.subtableEntries.add(maa);
                    TTXFile.this.subtableEntries.add(mam);
                } else {
                    unsupportedFormat(en, TTXFile.this.stFormat);
                }
                addGPOSSubtable(
                        GlyphPositioningTable.GPOS_LOOKUP_TYPE_MARK_TO_MARK,
                        mct1);
            } else if (en[1].equals("MarkRecord")) {
                if (TTXFile.this.markClass == -1) {
                    missingParameter(en, "mark class");
                } else if (TTXFile.this.anchors.size() == 0) {
                    missingParameter(en, "mark anchor");
                } else if (TTXFile.this.anchors.size() > 1) {
                    duplicateParameter(en, "mark anchor");
                } else {
                    TTXFile.this.markAnchors
                    .add(new GlyphPositioningTable.MarkAnchor(
                            TTXFile.this.markClass,
                            TTXFile.this.anchors.get(0)));
                    TTXFile.this.markClass = -1;
                    TTXFile.this.anchors.clear();
                }
            } else if (en[1].equals("Mark2Record")) {
                TTXFile.this.baseOrMarkAnchors.add(extractAnchors());
            } else if (en[1].equals("MultipleSubst")) {
                final GlyphCoverageTable coverage = TTXFile.this.coverages
                        .get("main");
                addGSUBSubtable(
                        GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_MULTIPLE,
                        coverage, extractSequenceEntries());
            } else if (en[1].equals("PairPos")) {
                assertSubtableEntriesClear();
                if (TTXFile.this.stFormat == 1) {
                    if (TTXFile.this.pairSets.size() == 0) {
                        missingParameter(en, "pair set");
                    } else {
                        TTXFile.this.subtableEntries.add(extractPairSets());
                    }
                } else if (TTXFile.this.stFormat == 2) {
                    unsupportedFormat(en, TTXFile.this.stFormat);
                }
                final GlyphCoverageTable coverage = TTXFile.this.coverages
                        .get("main");
                addGPOSSubtable(GlyphPositioningTable.GPOS_LOOKUP_TYPE_PAIR,
                        coverage);
                TTXFile.this.vf1 = TTXFile.this.vf2 = -1;
                TTXFile.this.psIndex = -1;
            } else if (en[1].equals("PairSet")) {
                if (TTXFile.this.psIndex != TTXFile.this.pairSets.size()) {
                    invalidIndex(en, TTXFile.this.psIndex,
                            TTXFile.this.pairSets.size());
                } else {
                    TTXFile.this.pairSets.add(extractPairs());
                }
            } else if (en[1].equals("PairValueRecord")) {
                if (TTXFile.this.g2 == -1) {
                    missingParameter(en, "second glyph");
                } else if (TTXFile.this.v1 == null && TTXFile.this.v2 == null) {
                    missingParameter(en, "first or second value");
                } else {
                    TTXFile.this.pairs.add(new PairValues(TTXFile.this.g2,
                            TTXFile.this.v1, TTXFile.this.v2));
                    clearPair();
                }
            } else if (en[1].equals("PosLookupRecord")
                    || en[1].equals("SubstLookupRecord")) {
                if (TTXFile.this.rlSequence < 0) {
                    missingParameter(en, "sequence index");
                } else if (TTXFile.this.rlLookup < 0) {
                    missingParameter(en, "lookup index");
                } else {
                    TTXFile.this.ruleLookups.add(new GlyphTable.RuleLookup(
                            TTXFile.this.rlSequence, TTXFile.this.rlLookup));
                    TTXFile.this.rlSequence = TTXFile.this.rlLookup = -1;
                }
            } else if (en[1].equals("Script")) {
                if (TTXFile.this.scriptTag == null) {
                    missingTag(en, "script");
                } else if (TTXFile.this.scripts
                        .containsKey(TTXFile.this.scriptTag)) {
                    duplicateTag(en, "script", TTXFile.this.scriptTag);
                } else {
                    TTXFile.this.scripts.put(TTXFile.this.scriptTag,
                            extractLanguages());
                    TTXFile.this.scriptTag = null;
                }
            } else if (en[1].equals("Sequence")) {
                TTXFile.this.subtableEntries.add(extractSubstitutes());
            } else if (en[1].equals("SinglePos")) {
                final int nv = TTXFile.this.subtableEntries.size();
                if (TTXFile.this.stFormat == 1) {
                    if (nv < 0) {
                        missingParameter(en, "value");
                    } else if (nv > 1) {
                        duplicateParameter(en, "value");
                    }
                } else if (TTXFile.this.stFormat == 2) {
                    final GlyphPositioningTable.Value[] pva = (GlyphPositioningTable.Value[]) TTXFile.this.subtableEntries
                            .toArray(new GlyphPositioningTable.Value[nv]);
                    TTXFile.this.subtableEntries.clear();
                    TTXFile.this.subtableEntries.add(pva);
                }
                final GlyphCoverageTable coverage = TTXFile.this.coverages
                        .get("main");
                addGPOSSubtable(GlyphPositioningTable.GPOS_LOOKUP_TYPE_SINGLE,
                        coverage);
                TTXFile.this.vf1 = -1;
            } else if (en[1].equals("SingleSubst")) {
                if (!sortEntries(TTXFile.this.coverageEntries,
                        TTXFile.this.subtableEntries)) {
                    mismatchedEntries(en, TTXFile.this.coverageEntries.size(),
                            TTXFile.this.subtableEntries.size());
                }
                final GlyphCoverageTable coverage = extractCoverage();
                addGSUBSubtable(GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_SINGLE,
                        coverage);
            } else if (en[1].equals("cmap")) {
                TTXFile.this.cmap = getCMAP();
                TTXFile.this.gmap = getGMAP();
                TTXFile.this.cmapEntries.clear();
            } else if (en[1].equals("cmap_format_4")) {
                TTXFile.this.cmPlatform = TTXFile.this.cmEncoding = TTXFile.this.cmLanguage = -1;
            } else if (en[1].equals("hmtx")) {
                TTXFile.this.hmtx = getHMTX();
                TTXFile.this.hmtxEntries.clear();
            } else if (en[1].equals("ttFont")) {
                if (TTXFile.this.cmap == null) {
                    missingParameter(en, "cmap");
                }
                if (TTXFile.this.hmtx == null) {
                    missingParameter(en, "hmtx");
                }
            }
            TTXFile.this.elements.pop();
        }

        @Override
        public void characters(final char[] chars, final int start,
                final int length) {
        }

        private String[] getParent() {
            if (!TTXFile.this.elements.empty()) {
                return TTXFile.this.elements.peek();
            } else {
                return new String[] { null, null };
            }
        }

        private boolean isParent(final Object enx) {
            if (enx instanceof String[][]) {
                for (final String[] en : (String[][]) enx) {
                    if (isParent(en)) {
                        return true;
                    }
                }
                return false;
            } else if (enx instanceof String[]) {
                final String[] en = (String[]) enx;
                if (!TTXFile.this.elements.empty()) {
                    final String[] pn = TTXFile.this.elements.peek();
                    return pn != null && sameExpandedName(en, pn);
                } else if (en[0] == null && en[1] == null) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        private boolean isAnchorElement(final String ln) {
            if (ln.equals("BaseAnchor")) {
                return true;
            } else if (ln.equals("EntryAnchor")) {
                return true;
            } else if (ln.equals("ExitAnchor")) {
                return true;
            } else if (ln.equals("LigatureAnchor")) {
                return true;
            } else if (ln.equals("MarkAnchor")) {
                return true;
            } else if (ln.equals("Mark2Anchor")) {
                return true;
            } else {
                return false;
            }
        }

        private Map<Integer, Integer> getCMAP() {
            final Map<Integer, Integer> cmap = new TreeMap();
            for (final int[] cme : TTXFile.this.cmapEntries) {
                final Integer c = Integer.valueOf(cme[0]);
                final Integer g = Integer.valueOf(cme[1]);
                cmap.put(c, g);
            }
            return cmap;
        }

        private Map<Integer, Integer> getGMAP() {
            final Map<Integer, Integer> gmap = new TreeMap();
            for (final int[] cme : TTXFile.this.cmapEntries) {
                final Integer c = Integer.valueOf(cme[0]);
                final Integer g = Integer.valueOf(cme[1]);
                gmap.put(g, c);
            }
            return gmap;
        }

        private int[][] getHMTX() {
            final int ne = TTXFile.this.hmtxEntries.size();
            final int[][] hmtx = new int[ne][2];
            for (int i = 0; i < ne; i++) {
                final int[] ea = TTXFile.this.hmtxEntries.get(i);
                if (ea != null) {
                    hmtx[i][0] = ea[0];
                    hmtx[i][1] = ea[1];
                }
            }
            return hmtx;
        }

        private GlyphClassTable extractClassDefMapping(
                final Map<String, Integer> glyphClasses, final int format,
                final boolean clearSourceMap) {
            GlyphClassTable ct;
            if (format == 1) {
                ct = extractClassDefMapping1(extractClassMappings(glyphClasses,
                        clearSourceMap));
            } else if (format == 2) {
                ct = extractClassDefMapping2(extractClassMappings(glyphClasses,
                        clearSourceMap));
            } else {
                ct = null;
            }
            return ct;
        }

        private GlyphClassTable extractClassDefMapping1(final int[][] cma) {
            final List entries = new ArrayList<Integer>();
            int s = -1;
            int l = -1;
            final Integer zero = Integer.valueOf(0);
            for (final int[] m : cma) {
                final int g = m[0];
                final int c = m[1];
                if (s < 0) {
                    s = g;
                    l = g - 1;
                    entries.add(Integer.valueOf(s));
                }
                while (g > l + 1) {
                    entries.add(zero);
                    l++;
                }
                assert l == g - 1;
                entries.add(Integer.valueOf(c));
                l = g;
            }
            return GlyphClassTable.createClassTable(entries);
        }

        private GlyphClassTable extractClassDefMapping2(final int[][] cma) {
            final List entries = new ArrayList<Integer>();
            int s = -1;
            int e = s;
            int l = -1;
            for (final int[] m : cma) {
                final int g = m[0];
                final int c = m[1];
                if (c != l) {
                    if (s >= 0) {
                        entries.add(new GlyphClassTable.MappingRange(s, e, l));
                    }
                    s = e = g;
                } else {
                    e = g;
                }
                l = c;
            }
            return GlyphClassTable.createClassTable(entries);
        }

        private int[][] extractClassMappings(
                final Map<String, Integer> glyphClasses,
                final boolean clearSourceMap) {
            final int nc = glyphClasses.size();
            int i = 0;
            final int[][] cma = new int[nc][2];
            for (final Map.Entry<String, Integer> e : glyphClasses.entrySet()) {
                final Integer gid = TTXFile.this.glyphIds.get(e.getKey());
                assert gid != null;
                final int[] m = cma[i];
                m[0] = gid;
                m[1] = e.getValue();
                i++;
            }
            if (clearSourceMap) {
                glyphClasses.clear();
            }
            return sortClassMappings(cma);
        }

        private int[][] sortClassMappings(final int[][] cma) {
            Arrays.sort(cma, new Comparator<int[]>() {
                @Override
                public int compare(final int[] m1, final int[] m2) {
                    assert m1.length > 0;
                    assert m2.length > 0;
                    if (m1[0] < m2[0]) {
                        return -1;
                    } else if (m1[0] > m2[0]) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
            return cma;
        }

        // sort coverage entries and subtable entries together
        private boolean sortEntries(final List cel, final List sel) {
            assert cel != null;
            assert sel != null;
            if (cel.size() == sel.size()) {
                final int np = cel.size();
                final Object[][] pa = new Object[np][2];
                for (int i = 0; i < np; i++) {
                    pa[i][0] = cel.get(i);
                    pa[i][1] = sel.get(i);
                }
                Arrays.sort(pa, new Comparator<Object[]>() {
                    @Override
                    public int compare(final Object[] p1, final Object[] p2) {
                        assert p1.length == 2;
                        assert p2.length == 2;
                        final int c1 = (Integer) p1[0];
                        final int c2 = (Integer) p2[0];
                        if (c1 < c2) {
                            return -1;
                        } else if (c1 > c2) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
                cel.clear();
                sel.clear();
                for (int i = 0; i < np; i++) {
                    cel.add(pa[i][0]);
                    sel.add(pa[i][1]);
                }
                assert cel.size() == sel.size();
                return true;
            } else {
                return false;
            }
        }

        private String makeCoverageKey(final String prefix, final int index) {
            assert prefix != null;
            assert prefix.length() == 2;
            assert index < 100;
            return prefix
                    + CharUtilities
                    .padLeft(Integer.toString(index, 10), 2, '0');
        }

        private List extractCoverageEntries() {
            final List entries = new ArrayList<Integer>(
                    TTXFile.this.coverageEntries);
            clearCoverage();
            return entries;
        }

        private void clearCoverageEntries() {
            TTXFile.this.coverageEntries.clear();
            TTXFile.this.ctFormat = -1;
            TTXFile.this.ctIndex = -1;
        }

        private void assertCoverageEntriesClear() {
            assert TTXFile.this.coverageEntries.size() == 0;
        }

        private GlyphCoverageTable extractCoverage() {
            assert TTXFile.this.ctFormat == 1 || TTXFile.this.ctFormat == 2;
            assert TTXFile.this.ctIndex >= 0;
            final GlyphCoverageTable coverage = GlyphCoverageTable
                    .createCoverageTable(extractCoverageEntries());
            clearCoverage();
            return coverage;
        }

        private void clearCoverages() {
            TTXFile.this.coverages.clear();
        }

        private void assertCoverageClear() {
            assert TTXFile.this.ctFormat == -1;
            assert TTXFile.this.ctIndex == -1;
            assertCoverageEntriesClear();
        }

        private void clearCoverage() {
            TTXFile.this.ctFormat = -1;
            TTXFile.this.ctIndex = -1;
            clearCoverageEntries();
        }

        private void assertCoveragesClear() {
            assert TTXFile.this.coverages.size() == 0;
        }

        private GlyphCoverageTable[] getCoveragesWithPrefix(final String prefix) {
            assert prefix != null;
            final int prefixLength = prefix.length();
            final Set<String> keys = TTXFile.this.coverages.keySet();
            int mi = -1; // maximum coverage table index
            for (final String k : keys) {
                if (k.startsWith(prefix)) {
                    final int i = Integer.parseInt(k.substring(prefixLength));
                    if (i > mi) {
                        mi = i;
                    }
                }
            }
            final GlyphCoverageTable[] gca = new GlyphCoverageTable[mi + 1];
            for (final String k : keys) {
                if (k.startsWith(prefix)) {
                    final int i = Integer.parseInt(k.substring(prefixLength));
                    if (i >= 0) {
                        gca[i] = TTXFile.this.coverages.get(k);
                    }
                }
            }
            return gca;
        }

        private boolean hasMissingCoverage(final GlyphCoverageTable[] gca) {
            assert gca != null;
            int nc = 0;
            for (final GlyphCoverageTable element : gca) {
                if (element != null) {
                    nc++;
                }
            }
            return nc != gca.length;
        }

        private String makeFeatureId(final int fid) {
            assert fid >= 0;
            return "f" + fid;
        }

        private String makeLookupId(final int lid) {
            assert lid >= 0;
            return "lu" + lid;
        }

        private void clearScripts() {
            TTXFile.this.scripts.clear();
        }

        private List<String> extractLanguageFeatures() {
            final List<String> lfl = new ArrayList<String>(
                    TTXFile.this.languageFeatures);
            clearLanguageFeatures();
            return lfl;
        }

        private void assertLanguageFeaturesClear() {
            assert TTXFile.this.languageFeatures.size() == 0;
        }

        private void clearLanguageFeatures() {
            TTXFile.this.languageFeatures.clear();
        }

        private Map<String, List<String>> extractLanguages() {
            final Map<String, List<String>> lm = new HashMap(
                    TTXFile.this.languages);
            clearLanguages();
            return lm;
        }

        private void clearLanguages() {
            TTXFile.this.languages.clear();
        }

        private void assertFeatureLookupsClear() {
            assert TTXFile.this.featureLookups.size() == 0;
        }

        private List extractFeatureLookups() {
            final List lookups = new ArrayList<String>(
                    TTXFile.this.featureLookups);
            clearFeatureLookups();
            return lookups;
        }

        private void clearFeatureLookups() {
            TTXFile.this.featureLookups.clear();
        }

        private void assertFeatureClear() {
            assert TTXFile.this.flIndex == -1;
            assert TTXFile.this.featureTag == null;
            assertFeatureLookupsClear();
        }

        private Object[] extractFeature() {
            final Object[] fa = new Object[2];
            fa[0] = TTXFile.this.featureTag;
            fa[1] = extractFeatureLookups();
            clearFeature();
            return fa;
        }

        private void clearFeature() {
            TTXFile.this.flIndex = -1;
            TTXFile.this.featureTag = null;
            clearFeatureLookups();
        }

        private void nextFeature() {
            TTXFile.this.flSequence++;
        }

        private void clearFeatures() {
            TTXFile.this.features.clear();
        }

        private void clearSubtableInLookup() {
            TTXFile.this.stFormat = 0;
            clearCoverages();
        }

        private void clearSubtablesInLookup() {
            clearSubtableInLookup();
            TTXFile.this.stSequence = 0;
        }

        private void clearSubtablesInTable() {
            clearSubtablesInLookup();
            TTXFile.this.subtables.clear();
        }

        private void nextSubtableInLookup() {
            TTXFile.this.stSequence++;
            clearSubtableInLookup();
        }

        private void assertLookupClear() {
            assert TTXFile.this.ltIndex == -1;
            assert TTXFile.this.ltFlags == 0;
        }

        private void clearLookup() {
            TTXFile.this.ltIndex = -1;
            TTXFile.this.ltFlags = 0;
            clearSubtablesInLookup();
        }

        private Map<GlyphTable.LookupSpec, List<String>> extractLookups() {
            final Map<GlyphTable.LookupSpec, List<String>> lookups = new LinkedHashMap<GlyphTable.LookupSpec, List<String>>();
            for (final String st : TTXFile.this.scripts.keySet()) {
                final Map<String, List<String>> lm = TTXFile.this.scripts
                        .get(st);
                if (lm != null) {
                    for (final String lt : lm.keySet()) {
                        final List<String> fids = lm.get(lt);
                        if (fids != null) {
                            for (final String fid : fids) {
                                if (fid != null) {
                                    final Object[] fa = TTXFile.this.features
                                            .get(fid);
                                    if (fa != null) {
                                        assert fa.length == 2;
                                        final String ft = (String) fa[0];
                                        final List<String> lids = (List<String>) fa[1];
                                        if (lids != null && lids.size() > 0) {
                                            final GlyphTable.LookupSpec ls = new GlyphTable.LookupSpec(
                                                    st, lt, ft);
                                            lookups.put(ls, lids);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            clearScripts();
            clearLanguages();
            clearFeatures();
            return lookups;
        }

        private void clearLookups() {
            clearLookup();
            clearSubtablesInTable();
            TTXFile.this.ltSequence = 0;
            TTXFile.this.flSequence = 0;
        }

        private void nextLookup() {
            TTXFile.this.ltSequence++;
            clearLookup();
        }

        private void clearTable() {
            clearLookups();
        }

        private void assertSubtableClear() {
            assert TTXFile.this.stFormat == 0;
            assertCoverageEntriesClear();
        }

        private void assertSubtablesClear() {
            assertSubtableClear();
            assert TTXFile.this.subtables.size() == 0;
        }

        private void clearSubtableEntries() {
            TTXFile.this.subtableEntries.clear();
        }

        private void assertSubtableEntriesClear() {
            assert TTXFile.this.subtableEntries.size() == 0;
        }

        private List extractSubtableEntries() {
            final List entries = new ArrayList(TTXFile.this.subtableEntries);
            clearSubtableEntries();
            return entries;
        }

        private int[] extractAlternates() {
            final int[] aa = new int[TTXFile.this.alternates.size()];
            int i = 0;
            for (final Integer a : TTXFile.this.alternates) {
                aa[i++] = a;
            }
            clearAlternates();
            return aa;
        }

        private void clearAlternates() {
            TTXFile.this.alternates.clear();
        }

        private LigatureSet extractLigatures() {
            final LigatureSet ls = new LigatureSet(TTXFile.this.ligatures);
            clearLigatures();
            return ls;
        }

        private void clearLigatures() {
            TTXFile.this.ligatures.clear();
        }

        private int[] extractSubstitutes() {
            final int[] aa = new int[TTXFile.this.substitutes.size()];
            int i = 0;
            for (final Integer a : TTXFile.this.substitutes) {
                aa[i++] = a;
            }
            clearSubstitutes();
            return aa;
        }

        private void clearSubstitutes() {
            TTXFile.this.substitutes.clear();
        }

        private List extractSequenceEntries() {
            final List sequences = extractSubtableEntries();
            final int[][] sa = new int[sequences.size()][];
            int i = 0;
            for (final Object s : sequences) {
                if (s instanceof int[]) {
                    sa[i++] = (int[]) s;
                }
            }
            final List entries = new ArrayList();
            entries.add(sa);
            return entries;
        }

        private RuleLookup[] extractRuleLookups() {
            final RuleLookup[] lookups = TTXFile.this.ruleLookups
                    .toArray(new RuleLookup[TTXFile.this.ruleLookups.size()]);
            clearRuleLookups();
            return lookups;
        }

        private void clearRuleLookups() {
            TTXFile.this.ruleLookups.clear();
        }

        private GlyphPositioningTable.Value parseValue(final String[] en,
                final Attributes attrs, final int format) throws SAXException {
            final String xPlacement = attrs.getValue("XPlacement");
            int xp = 0;
            if (xPlacement != null) {
                xp = Integer.parseInt(xPlacement);
            } else if ((format & GlyphPositioningTable.Value.X_PLACEMENT) != 0) {
                missingParameter(en, "xPlacement");
            }
            final String yPlacement = attrs.getValue("YPlacement");
            int yp = 0;
            if (yPlacement != null) {
                yp = Integer.parseInt(yPlacement);
            } else if ((format & GlyphPositioningTable.Value.Y_PLACEMENT) != 0) {
                missingParameter(en, "yPlacement");
            }
            final String xAdvance = attrs.getValue("XAdvance");
            int xa = 0;
            if (xAdvance != null) {
                xa = Integer.parseInt(xAdvance);
            } else if ((format & GlyphPositioningTable.Value.X_ADVANCE) != 0) {
                missingParameter(en, "xAdvance");
            }
            final String yAdvance = attrs.getValue("YAdvance");
            int ya = 0;
            ;
            if (yAdvance != null) {
                ya = Integer.parseInt(yAdvance);
            } else if ((format & GlyphPositioningTable.Value.Y_ADVANCE) != 0) {
                missingParameter(en, "yAdvance");
            }
            return new GlyphPositioningTable.Value(xp, yp, xa, ya, null, null,
                    null, null);
        }

        private void assertPairClear() {
            assert TTXFile.this.g2 == -1;
            assert TTXFile.this.v1 == null;
            assert TTXFile.this.v2 == null;
        }

        private void clearPair() {
            TTXFile.this.g2 = -1;
            TTXFile.this.v1 = null;
            TTXFile.this.v2 = null;
        }

        private void assertPairsClear() {
            assert TTXFile.this.pairs.size() == 0;
        }

        private void clearPairs() {
            TTXFile.this.pairs.clear();
            TTXFile.this.psIndex = -1;
        }

        private PairValues[] extractPairs() {
            final PairValues[] pva = TTXFile.this.pairs
                    .toArray(new PairValues[TTXFile.this.pairs.size()]);
            clearPairs();
            return pva;
        }

        private void assertPairSetsClear() {
            assert TTXFile.this.pairSets.size() == 0;
        }

        private void clearPairSets() {
            TTXFile.this.pairSets.clear();
        }

        private PairValues[][] extractPairSets() {
            final PairValues[][] pvm = TTXFile.this.pairSets
                    .toArray(new PairValues[TTXFile.this.pairSets.size()][]);
            clearPairSets();
            return pvm;
        }

        private Anchor[] extractAnchors() {
            final Anchor[] aa = TTXFile.this.anchors
                    .toArray(new Anchor[TTXFile.this.anchors.size()]);
            TTXFile.this.anchors.clear();
            return aa;
        }

        private MarkAnchor[] extractMarkAnchors() {
            MarkAnchor[] maa = new MarkAnchor[TTXFile.this.markAnchors.size()];
            maa = TTXFile.this.markAnchors.toArray(new MarkAnchor[maa.length]);
            TTXFile.this.markAnchors.clear();
            return maa;
        }

        private Anchor[][] extractBaseOrMarkAnchors() {
            final int na = TTXFile.this.baseOrMarkAnchors.size();
            int ncMax = 0;
            for (final Anchor[] aa : TTXFile.this.baseOrMarkAnchors) {
                if (aa != null) {
                    final int nc = aa.length;
                    if (nc > ncMax) {
                        ncMax = nc;
                    }
                }
            }
            final Anchor[][] am = new Anchor[na][ncMax];
            for (int i = 0; i < na; i++) {
                final Anchor[] aa = TTXFile.this.baseOrMarkAnchors.get(i);
                if (aa != null) {
                    for (int j = 0; j < ncMax; j++) {
                        if (j < aa.length) {
                            am[i][j] = aa[j];
                        }
                    }
                }
            }
            TTXFile.this.baseOrMarkAnchors.clear();
            return am;
        }

        private Integer computeClassCount(final Anchor[][] am) {
            int ncMax = 0;
            for (final Anchor[] aa : am) {
                if (aa != null) {
                    final int nc = aa.length;
                    if (nc > ncMax) {
                        ncMax = nc;
                    }
                }
            }
            return Integer.valueOf(ncMax);
        }

        private Anchor[][] extractComponents() {
            Anchor[][] cam = new Anchor[TTXFile.this.components.size()][];
            cam = TTXFile.this.components.toArray(new Anchor[cam.length][]);
            TTXFile.this.components.clear();
            return cam;
        }

        private Anchor[][][] extractLigatureAnchors() {
            final int na = TTXFile.this.ligatureAnchors.size();
            int ncMax = 0;
            int nxMax = 0;
            for (final Anchor[][] cm : TTXFile.this.ligatureAnchors) {
                if (cm != null) {
                    final int nx = cm.length;
                    if (nx > nxMax) {
                        nxMax = nx;
                    }
                    for (final Anchor[] aa : cm) {
                        if (aa != null) {
                            final int nc = aa.length;
                            if (nc > ncMax) {
                                ncMax = nc;
                            }
                        }
                    }

                }
            }
            final Anchor[][][] lam = new Anchor[na][nxMax][ncMax];
            for (int i = 0; i < na; i++) {
                final Anchor[][] cm = TTXFile.this.ligatureAnchors.get(i);
                if (cm != null) {
                    for (int j = 0; j < nxMax; j++) {
                        if (j < cm.length) {
                            final Anchor[] aa = cm[j];
                            if (aa != null) {
                                for (int k = 0; k < ncMax; k++) {
                                    if (k < aa.length) {
                                        lam[i][j][k] = aa[k];
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TTXFile.this.ligatureAnchors.clear();
            return lam;
        }

        private Integer computeLigaturesClassCount(final Anchor[][][] lam) {
            int ncMax = 0;
            if (lam != null) {
                for (final Anchor[][] cm : lam) {
                    if (cm != null) {
                        for (final Anchor[] aa : cm) {
                            if (aa != null) {
                                final int nc = aa.length;
                                ;
                                if (nc > ncMax) {
                                    ncMax = nc;
                                }
                            }
                        }
                    }
                }
            }
            return Integer.valueOf(ncMax);
        }

        private Integer computeLigaturesComponentCount(final Anchor[][][] lam) {
            int nxMax = 0;
            if (lam != null) {
                for (final Anchor[][] cm : lam) {
                    if (cm != null) {
                        final int nx = cm.length;
                        ;
                        if (nx > nxMax) {
                            nxMax = nx;
                        }
                    }
                }
            }
            return Integer.valueOf(nxMax);
        }

        private Anchor[] extractAttachmentAnchors() {
            final int na = TTXFile.this.attachmentAnchors.size();
            final Anchor[] aa = new Anchor[na * 2];
            for (int i = 0; i < na; i++) {
                final Anchor[] ea = TTXFile.this.attachmentAnchors.get(i);
                final int ne = ea.length;
                if (ne > 0) {
                    aa[i * 2 + 0] = ea[0];
                }
                if (ne > 1) {
                    aa[i * 2 + 1] = ea[1];
                }
            }
            TTXFile.this.attachmentAnchors.clear();
            return aa;
        }

        private void addGDEFSubtable(final int stType,
                final GlyphMappingTable mapping) {
            TTXFile.this.subtables.add(GlyphDefinitionTable.createSubtable(
                    stType, makeLookupId(TTXFile.this.ltSequence),
                    TTXFile.this.stSequence, TTXFile.this.ltFlags,
                    TTXFile.this.stFormat, mapping, extractSubtableEntries()));
            nextSubtableInLookup();
        }

        private void addGSUBSubtable(final int stType,
                final GlyphCoverageTable coverage, final List entries) {
            TTXFile.this.subtables.add(GlyphSubstitutionTable.createSubtable(
                    stType, makeLookupId(TTXFile.this.ltSequence),
                    TTXFile.this.stSequence, TTXFile.this.ltFlags,
                    TTXFile.this.stFormat, coverage, entries));
            nextSubtableInLookup();
        }

        private void addGSUBSubtable(final int stType,
                final GlyphCoverageTable coverage) {
            addGSUBSubtable(stType, coverage, extractSubtableEntries());
        }

        private void addGPOSSubtable(final int stType,
                final GlyphCoverageTable coverage, final List entries) {
            TTXFile.this.subtables.add(GlyphPositioningTable.createSubtable(
                    stType, makeLookupId(TTXFile.this.ltSequence),
                    TTXFile.this.stSequence, TTXFile.this.ltFlags,
                    TTXFile.this.stFormat, coverage, entries));
            nextSubtableInLookup();
        }

        private void addGPOSSubtable(final int stType,
                final GlyphCoverageTable coverage) {
            addGPOSSubtable(stType, coverage, extractSubtableEntries());
        }
    }

    private int mapGlyphId0(final String glyph) {
        assert this.glyphIds != null;
        final Integer gid = this.glyphIds.get(glyph);
        if (gid != null) {
            return gid;
        } else {
            return -1;
        }
    }

    private int mapGlyphId(final String glyph, final String[] currentElement)
            throws SAXException {
        final int g = mapGlyphId0(glyph);
        if (g < 0) {
            unsupportedGlyph(currentElement, glyph);
            return -1;
        } else {
            return g;
        }
    }

    private int[] mapGlyphIds(final String glyphs, final String[] currentElement)
            throws SAXException {
        final String[] ga = glyphs.split(",");
        final int[] gids = new int[ga.length];
        int i = 0;
        for (final String glyph : ga) {
            gids[i++] = mapGlyphId(glyph, currentElement);
        }
        return gids;
    }

    private int mapGlyphIdToChar(final String glyph) {
        assert this.glyphIds != null;
        final Integer gid = this.glyphIds.get(glyph);
        if (gid != null) {
            if (this.gmap != null) {
                final Integer cid = this.gmap.get(gid);
                if (cid != null) {
                    return cid.intValue();
                }
            }
        }
        return -1;
    }

    private String formatLocator() {
        if (this.locator == null) {
            return "{null}";
        } else {
            return "{" + this.locator.getSystemId() + ":"
                    + this.locator.getLineNumber() + ":"
                    + this.locator.getColumnNumber() + "}";
        }
    }

    private void unsupportedElement(final String[] en) throws SAXException {
        throw new SAXException(formatLocator() + ": unsupported element "
                + formatExpandedName(en));
    }

    private void notPermittedInElementContext(final String[] en,
            final String[] cn, final Object xns) throws SAXException {
        assert en != null;
        assert cn != null;
        String s = "element " + formatExpandedName(en)
                + " not permitted in current element context "
                + formatExpandedName(cn);
        if (xns == null) {
            s += ", expected root context";
        } else if (xns instanceof String[][]) {
            int nxn = 0;
            s += ", expected one of { ";
            for (final String[] xn : (String[][]) xns) {
                if (nxn++ > 0) {
                    s += ", ";
                }
                s += formatExpandedName(xn);
            }
            s += " }";
        } else if (xns instanceof String[]) {
            s += ", expected " + formatExpandedName((String[]) xns);
        }
        throw new SAXException(formatLocator() + ": " + s);
    }

    private void missingRequiredAttribute(final String[] en, final String name)
            throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en) + " missing required attribute "
                + name);
    }

    private void duplicateGlyph(final String[] en, final String name,
            final int gid) throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en) + " contains duplicate name \"" + name
                + "\", with identifier value " + gid);
    }

    private void unsupportedGlyph(final String[] en, final String name)
            throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en) + " refers to unsupported glyph id \""
                + name + "\"");
    }

    private void duplicateCMAPCharacter(final String[] en, final int cid)
            throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en)
                + " contains duplicate cmap character code: "
                + CharUtilities.format(cid));
    }

    private void duplicateCMAPGlyph(final String[] en, final int gid)
            throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en)
                + " contains duplicate cmap glyph code: " + gid);
    }

    private void duplicateGlyphClass(final String[] en, final String name,
            final String glyphClass) throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en)
                + " contains duplicate glyph class for \"" + name
                + "\", with class value " + glyphClass);
    }

    private void unsupportedFormat(final String[] en, final int format)
            throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en)
                + " refers to unsupported table format \"" + format + "\"");
    }

    private void invalidIndex(final String[] en, final int actual,
            final int expected) throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en) + " specifies invalid index " + actual
                + ", expected " + expected);
    }

    private void mismatchedIndex(final String[] en, final String label,
            final int actual, final int expected) throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en) + " mismatched " + label
                + " index: got " + actual + ", expected " + expected);
    }

    private void mismatchedEntries(final String[] en, final int nce,
            final int nse) throws SAXException {
        throw new SAXException(
                formatLocator()
                + ": element "
                + formatExpandedName(en)
                + " mismatched coverage and subtable entry counts, # coverages "
                + nce + ", # entries " + nse);
    }

    private void missingParameter(final String[] en, final String label)
            throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en) + " missing " + label + " parameter");
    }

    private void duplicateParameter(final String[] en, final String label)
            throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en) + " duplicate " + label + " parameter");
    }

    private void duplicateCoverageIndex(final String[] en, final int index)
            throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en) + " duplicate coverage table index "
                + index);
    }

    private void missingCoverage(final String[] en, final String type,
            final int expected) throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en) + " missing " + type
                + " coverage table, expected " + (expected > 0 ? expected : 1)
                + " table(s)");
    }

    private void missingTag(final String[] en, final String label)
            throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en) + " missing " + label + " tag");
    }

    private void duplicateTag(final String[] en, final String label,
            final String tag) throws SAXException {
        throw new SAXException(formatLocator() + ": element "
                + formatExpandedName(en) + " duplicate " + label + " tag: "
                + tag);
    }

    private static String[] makeExpandedName(String uri, String localName,
            final String qName) {
        if (uri != null && uri.length() == 0) {
            uri = null;
        }
        if (localName != null && localName.length() == 0) {
            localName = null;
        }
        if (uri == null && localName == null) {
            uri = extractPrefix(qName);
            localName = extractLocalName(qName);
        }
        return new String[] { uri, localName };
    }

    private static String extractPrefix(final String qName) {
        final String[] sa = qName.split(":");
        if (sa.length == 2) {
            return sa[0];
        } else {
            return null;
        }
    }

    private static String extractLocalName(final String qName) {
        final String[] sa = qName.split(":");
        if (sa.length == 2) {
            return sa[1];
        } else if (sa.length == 1) {
            return sa[0];
        } else {
            return null;
        }
    }

    private static boolean sameExpandedName(final String[] n1, final String[] n2) {
        final String u1 = n1[0];
        final String u2 = n2[0];
        if (u1 == null ^ u2 == null) {
            return false;
        }
        if (u1 != null && u2 != null) {
            if (!u1.equals(u2)) {
                return false;
            }
        }
        final String l1 = n1[1];
        final String l2 = n2[1];
        if (l1 == null ^ l2 == null) {
            return false;
        }
        if (l1 != null && l2 != null) {
            if (!l1.equals(l2)) {
                return false;
            }
        }
        return true;
    }

    private static String formatExpandedName(final String[] n) {
        final String u = n[0] != null ? n[0] : "null";
        final String l = n[1] != null ? n[1] : "null";
        return "{" + u + "}" + l;
    }
}
