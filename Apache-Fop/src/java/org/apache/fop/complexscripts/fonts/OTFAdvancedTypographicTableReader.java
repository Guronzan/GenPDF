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

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fonts.truetype.FontFileReader;
import org.apache.fop.fonts.truetype.TTFDirTabEntry;
import org.apache.fop.fonts.truetype.TTFFile;
import org.apache.fop.fonts.truetype.TTFTableName;

// CSOFF: AvoidNestedBlocksCheck
// CSOFF: NoWhitespaceAfterCheck
// CSOFF: InnerAssignmentCheck
// CSOFF: SimplifyBooleanReturnCheck
// CSOFF: LineLengthCheck

/**
 * <p>
 * OpenType Font (OTF) advanced typographic table reader. Used by @{Link
 * org.apache.fop.fonts.truetype.TTFFile} to read advanced typographic tables
 * (GDEF, GSUB, GPOS).
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
@Slf4j
public final class OTFAdvancedTypographicTableReader {

    // instance state
    private final TTFFile ttf; // parent font file reader
    private final FontFileReader in; // input reader
    private GlyphDefinitionTable gdef; // glyph definition table
    private GlyphSubstitutionTable gsub; // glyph substitution table
    private GlyphPositioningTable gpos; // glyph positioning table
    // transient parsing state
    private transient Map/* <String,Object[3]> */seScripts; // script-tag =>
    // Object[3] : {
    // default-language-tag,
    // List(language-tag),
    // seLanguages }
    private transient Map/* <String,Object[2]> */seLanguages; // language-tag =>
    // Object[2] : {
    // "f<required-feature-index>",
    // List("f<feature-index>")
    private transient Map/* <String,List<String>> */seFeatures; // "f<feature-index>"
    // => Object[2]
    // : {
    // feature-tag,
    // List("lu<lookup-index>")
    // }
    private transient GlyphMappingTable seMapping; // subtable entry mappings
    private transient List seEntries; // subtable entry entries
    private transient List seSubtables; // subtable entry subtables

    /**
     * Construct an <code>OTFAdvancedTypographicTableReader</code> instance.
     *
     * @param ttf
     *            parent font file reader (must be non-null)
     * @param in
     *            font file reader (must be non-null)
     */
    public OTFAdvancedTypographicTableReader(final TTFFile ttf,
            final FontFileReader in) {
        assert ttf != null;
        assert in != null;
        this.ttf = ttf;
        this.in = in;
    }

    /**
     * Read all advanced typographic tables.
     *
     * @throws AdvancedTypographicTableFormatException
     *             if ATT table has invalid format
     */
    public void readAll() throws AdvancedTypographicTableFormatException {
        try {
            readGDEF();
            readGSUB();
            readGPOS();
        } catch (final AdvancedTypographicTableFormatException e) {
            resetATStateAll();
            throw e;
        } catch (final IOException e) {
            resetATStateAll();
            throw new AdvancedTypographicTableFormatException(e.getMessage(), e);
        } finally {
            resetATState();
        }
    }

    /**
     * Determine if advanced (typographic) table is present.
     *
     * @return true if advanced (typographic) table is present
     */
    public boolean hasAdvancedTable() {
        return this.gdef != null || this.gsub != null || this.gpos != null;
    }

    /**
     * Returns the GDEF table or null if none present.
     *
     * @return the GDEF table
     */
    public GlyphDefinitionTable getGDEF() {
        return this.gdef;
    }

    /**
     * Returns the GSUB table or null if none present.
     *
     * @return the GSUB table
     */
    public GlyphSubstitutionTable getGSUB() {
        return this.gsub;
    }

    /**
     * Returns the GPOS table or null if none present.
     *
     * @return the GPOS table
     */
    public GlyphPositioningTable getGPOS() {
        return this.gpos;
    }

    private void readLangSysTable(final TTFTableName tableTag,
            final long langSysTable, final String langSysTag)
                    throws IOException {
        this.in.seekSet(langSysTable);
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " lang sys table: " + langSysTag);
        }
        // read lookup order (reorder) table offset
        final int lo = this.in.readTTFUShort();
        // read required feature index
        final int rf = this.in.readTTFUShort();
        String rfi;
        if (rf != 65535) {
            rfi = "f" + rf;
        } else {
            rfi = null;
        }
        // read (non-required) feature count
        final int nf = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " lang sys table reorder table: " + lo);
            log.debug(tableTag + " lang sys table required feature index: "
                    + rf);
            log.debug(tableTag + " lang sys table non-required feature count: "
                    + nf);
        }
        // read (non-required) feature indices
        final int[] fia = new int[nf];
        final List fl = new java.util.ArrayList();
        for (int i = 0; i < nf; i++) {
            final int fi = this.in.readTTFUShort();
            if (log.isDebugEnabled()) {
                log.debug(tableTag
                        + " lang sys table non-required feature index: " + fi);
            }
            fia[i] = fi;
            fl.add("f" + fi);
        }
        if (this.seLanguages == null) {
            this.seLanguages = new java.util.LinkedHashMap();
        }
        this.seLanguages.put(langSysTag, new Object[] { rfi, fl });
    }

    private static String defaultTag = "dflt";

    private void readScriptTable(final TTFTableName tableTag,
            final long scriptTable, final String scriptTag) throws IOException {
        this.in.seekSet(scriptTable);
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " script table: " + scriptTag);
        }
        // read default language system table offset
        int dl = this.in.readTTFUShort();
        String dt = defaultTag;
        if (dl > 0) {
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " default lang sys tag: " + dt);
                log.debug(tableTag + " default lang sys table offset: " + dl);
            }
        }
        // read language system record count
        final int nl = this.in.readTTFUShort();
        final List ll = new java.util.ArrayList();
        if (nl > 0) {
            final String[] lta = new String[nl];
            final int[] loa = new int[nl];
            // read language system records
            for (int i = 0, n = nl; i < n; i++) {
                final String lt = this.in.readTTFString(4);
                final int lo = this.in.readTTFUShort();
                if (log.isDebugEnabled()) {
                    log.debug(tableTag + " lang sys tag: " + lt);
                    log.debug(tableTag + " lang sys table offset: " + lo);
                }
                lta[i] = lt;
                loa[i] = lo;
                if (dl == lo) {
                    dl = 0;
                    dt = lt;
                }
                ll.add(lt);
            }
            // read non-default language system tables
            for (int i = 0, n = nl; i < n; i++) {
                readLangSysTable(tableTag, scriptTable + loa[i], lta[i]);
            }
        }
        // read default language system table (if specified)
        if (dl > 0) {
            readLangSysTable(tableTag, scriptTable + dl, dt);
        } else if (dt != null) {
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " lang sys default: " + dt);
            }
        }
        this.seScripts
        .put(scriptTag, new Object[] { dt, ll, this.seLanguages });
        this.seLanguages = null;
    }

    private void readScriptList(final TTFTableName tableTag,
            final long scriptList) throws IOException {
        this.in.seekSet(scriptList);
        // read script record count
        final int ns = this.in.readTTFUShort();
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " script list record count: " + ns);
        }
        if (ns > 0) {
            final String[] sta = new String[ns];
            final int[] soa = new int[ns];
            // read script records
            for (int i = 0, n = ns; i < n; i++) {
                final String st = this.in.readTTFString(4);
                final int so = this.in.readTTFUShort();
                if (log.isDebugEnabled()) {
                    log.debug(tableTag + " script tag: " + st);
                    log.debug(tableTag + " script table offset: " + so);
                }
                sta[i] = st;
                soa[i] = so;
            }
            // read script tables
            for (int i = 0, n = ns; i < n; i++) {
                this.seLanguages = null;
                readScriptTable(tableTag, scriptList + soa[i], sta[i]);
            }
        }
    }

    private void readFeatureTable(final TTFTableName tableTag,
            final long featureTable, final String featureTag,
            final int featureIndex) throws IOException {
        this.in.seekSet(featureTable);
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " feature table: " + featureTag);
        }
        // read feature params offset
        final int po = this.in.readTTFUShort();
        // read lookup list indices count
        final int nl = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " feature table parameters offset: " + po);
            log.debug(tableTag + " feature table lookup list index count: "
                    + nl);
        }
        // read lookup table indices
        final int[] lia = new int[nl];
        final List lul = new java.util.ArrayList();
        for (int i = 0; i < nl; i++) {
            final int li = this.in.readTTFUShort();
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " feature table lookup index: " + li);
            }
            lia[i] = li;
            lul.add("lu" + li);
        }
        this.seFeatures.put("f" + featureIndex,
                new Object[] { featureTag, lul });
    }

    private void readFeatureList(final TTFTableName tableTag,
            final long featureList) throws IOException {
        this.in.seekSet(featureList);
        // read feature record count
        final int nf = this.in.readTTFUShort();
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " feature list record count: " + nf);
        }
        if (nf > 0) {
            final String[] fta = new String[nf];
            final int[] foa = new int[nf];
            // read feature records
            for (int i = 0, n = nf; i < n; i++) {
                final String ft = this.in.readTTFString(4);
                final int fo = this.in.readTTFUShort();
                if (log.isDebugEnabled()) {
                    log.debug(tableTag + " feature tag: " + ft);
                    log.debug(tableTag + " feature table offset: " + fo);
                }
                fta[i] = ft;
                foa[i] = fo;
            }
            // read feature tables
            for (int i = 0, n = nf; i < n; i++) {
                if (log.isDebugEnabled()) {
                    log.debug(tableTag + " feature index: " + i);
                }
                readFeatureTable(tableTag, featureList + foa[i], fta[i], i);
            }
        }
    }

    static final class GDEFLookupType {
        static final int GLYPH_CLASS = 1;
        static final int ATTACHMENT_POINT = 2;
        static final int LIGATURE_CARET = 3;
        static final int MARK_ATTACHMENT = 4;

        private GDEFLookupType() {
        }

        public static int getSubtableType(final int lt) {
            int st;
            switch (lt) {
            case GDEFLookupType.GLYPH_CLASS:
                st = GlyphDefinitionTable.GDEF_LOOKUP_TYPE_GLYPH_CLASS;
                break;
            case GDEFLookupType.ATTACHMENT_POINT:
                st = GlyphDefinitionTable.GDEF_LOOKUP_TYPE_ATTACHMENT_POINT;
                break;
            case GDEFLookupType.LIGATURE_CARET:
                st = GlyphDefinitionTable.GDEF_LOOKUP_TYPE_LIGATURE_CARET;
                break;
            case GDEFLookupType.MARK_ATTACHMENT:
                st = GlyphDefinitionTable.GDEF_LOOKUP_TYPE_MARK_ATTACHMENT;
                break;
            default:
                st = -1;
                break;
            }
            return st;
        }

        public static String toString(final int type) {
            String s;
            switch (type) {
            case GLYPH_CLASS:
                s = "GlyphClass";
                break;
            case ATTACHMENT_POINT:
                s = "AttachmentPoint";
                break;
            case LIGATURE_CARET:
                s = "LigatureCaret";
                break;
            case MARK_ATTACHMENT:
                s = "MarkAttachment";
                break;
            default:
                s = "?";
                break;
            }
            return s;
        }
    }

    static final class GSUBLookupType {
        static final int SINGLE = 1;
        static final int MULTIPLE = 2;
        static final int ALTERNATE = 3;
        static final int LIGATURE = 4;
        static final int CONTEXTUAL = 5;
        static final int CHAINED_CONTEXTUAL = 6;
        static final int EXTENSION = 7;
        static final int REVERSE_CHAINED_SINGLE = 8;

        private GSUBLookupType() {
        }

        public static int getSubtableType(final int lt) {
            int st;
            switch (lt) {
            case GSUBLookupType.SINGLE:
                st = GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_SINGLE;
                break;
            case GSUBLookupType.MULTIPLE:
                st = GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_MULTIPLE;
                break;
            case GSUBLookupType.ALTERNATE:
                st = GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_ALTERNATE;
                break;
            case GSUBLookupType.LIGATURE:
                st = GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_LIGATURE;
                break;
            case GSUBLookupType.CONTEXTUAL:
                st = GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_CONTEXTUAL;
                break;
            case GSUBLookupType.CHAINED_CONTEXTUAL:
                st = GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_CHAINED_CONTEXTUAL;
                break;
            case GSUBLookupType.EXTENSION:
                st = GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_EXTENSION_SUBSTITUTION;
                break;
            case GSUBLookupType.REVERSE_CHAINED_SINGLE:
                st = GlyphSubstitutionTable.GSUB_LOOKUP_TYPE_REVERSE_CHAINED_SINGLE;
                break;
            default:
                st = -1;
                break;
            }
            return st;
        }

        public static String toString(final int type) {
            String s;
            switch (type) {
            case SINGLE:
                s = "Single";
                break;
            case MULTIPLE:
                s = "Multiple";
                break;
            case ALTERNATE:
                s = "Alternate";
                break;
            case LIGATURE:
                s = "Ligature";
                break;
            case CONTEXTUAL:
                s = "Contextual";
                break;
            case CHAINED_CONTEXTUAL:
                s = "ChainedContextual";
                break;
            case EXTENSION:
                s = "Extension";
                break;
            case REVERSE_CHAINED_SINGLE:
                s = "ReverseChainedSingle";
                break;
            default:
                s = "?";
                break;
            }
            return s;
        }
    }

    static final class GPOSLookupType {
        static final int SINGLE = 1;
        static final int PAIR = 2;
        static final int CURSIVE = 3;
        static final int MARK_TO_BASE = 4;
        static final int MARK_TO_LIGATURE = 5;
        static final int MARK_TO_MARK = 6;
        static final int CONTEXTUAL = 7;
        static final int CHAINED_CONTEXTUAL = 8;
        static final int EXTENSION = 9;

        private GPOSLookupType() {
        }

        public static String toString(final int type) {
            String s;
            switch (type) {
            case SINGLE:
                s = "Single";
                break;
            case PAIR:
                s = "Pair";
                break;
            case CURSIVE:
                s = "Cursive";
                break;
            case MARK_TO_BASE:
                s = "MarkToBase";
                break;
            case MARK_TO_LIGATURE:
                s = "MarkToLigature";
                break;
            case MARK_TO_MARK:
                s = "MarkToMark";
                break;
            case CONTEXTUAL:
                s = "Contextual";
                break;
            case CHAINED_CONTEXTUAL:
                s = "ChainedContextual";
                break;
            case EXTENSION:
                s = "Extension";
                break;
            default:
                s = "?";
                break;
            }
            return s;
        }
    }

    static final class LookupFlag {
        static final int RIGHT_TO_LEFT = 0x0001;
        static final int IGNORE_BASE_GLYPHS = 0x0002;
        static final int IGNORE_LIGATURE = 0x0004;
        static final int IGNORE_MARKS = 0x0008;
        static final int USE_MARK_FILTERING_SET = 0x0010;
        static final int MARK_ATTACHMENT_TYPE = 0xFF00;

        private LookupFlag() {
        }

        public static String toString(final int flags) {
            final StringBuffer sb = new StringBuffer();
            boolean first = true;
            if ((flags & RIGHT_TO_LEFT) != 0) {
                if (first) {
                    first = false;
                } else {
                    sb.append('|');
                }
                sb.append("RightToLeft");
            }
            if ((flags & IGNORE_BASE_GLYPHS) != 0) {
                if (first) {
                    first = false;
                } else {
                    sb.append('|');
                }
                sb.append("IgnoreBaseGlyphs");
            }
            if ((flags & IGNORE_LIGATURE) != 0) {
                if (first) {
                    first = false;
                } else {
                    sb.append('|');
                }
                sb.append("IgnoreLigature");
            }
            if ((flags & IGNORE_MARKS) != 0) {
                if (first) {
                    first = false;
                } else {
                    sb.append('|');
                }
                sb.append("IgnoreMarks");
            }
            if ((flags & USE_MARK_FILTERING_SET) != 0) {
                if (first) {
                    first = false;
                } else {
                    sb.append('|');
                }
                sb.append("UseMarkFilteringSet");
            }
            if (sb.length() == 0) {
                sb.append('-');
            }
            return sb.toString();
        }
    }

    private GlyphCoverageTable readCoverageTableFormat1(final String label,
            final long tableOffset, final int coverageFormat)
                    throws IOException {
        final List entries = new java.util.ArrayList();
        this.in.seekSet(tableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read glyph count
        final int ng = this.in.readTTFUShort();
        final int[] ga = new int[ng];
        for (int i = 0, n = ng; i < n; i++) {
            final int g = this.in.readTTFUShort();
            ga[i] = g;
            entries.add(Integer.valueOf(g));
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(label + " glyphs: " + toString(ga));
        }
        return GlyphCoverageTable.createCoverageTable(entries);
    }

    private GlyphCoverageTable readCoverageTableFormat2(final String label,
            final long tableOffset, final int coverageFormat)
                    throws IOException {
        final List entries = new java.util.ArrayList();
        this.in.seekSet(tableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read range record count
        final int nr = this.in.readTTFUShort();
        for (int i = 0, n = nr; i < n; i++) {
            // read range start
            final int s = this.in.readTTFUShort();
            // read range end
            final int e = this.in.readTTFUShort();
            // read range coverage (mapping) index
            final int m = this.in.readTTFUShort();
            // dump info if debugging
            if (log.isDebugEnabled()) {
                log.debug(label + " range[" + i + "]: [" + s + "," + e + "]: "
                        + m);
            }
            entries.add(new GlyphCoverageTable.MappingRange(s, e, m));
        }
        return GlyphCoverageTable.createCoverageTable(entries);
    }

    private GlyphCoverageTable readCoverageTable(final String label,
            final long tableOffset) throws IOException {
        GlyphCoverageTable gct;
        final long cp = this.in.getCurrentPos();
        this.in.seekSet(tableOffset);
        // read coverage table format
        final int cf = this.in.readTTFUShort();
        if (cf == 1) {
            gct = readCoverageTableFormat1(label, tableOffset, cf);
        } else if (cf == 2) {
            gct = readCoverageTableFormat2(label, tableOffset, cf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported coverage table format: " + cf);
        }
        this.in.seekSet(cp);
        return gct;
    }

    private GlyphClassTable readClassDefTableFormat1(final String label,
            final long tableOffset, final int classFormat) throws IOException {
        final List entries = new java.util.ArrayList();
        this.in.seekSet(tableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read start glyph
        final int sg = this.in.readTTFUShort();
        entries.add(Integer.valueOf(sg));
        // read glyph count
        final int ng = this.in.readTTFUShort();
        // read glyph classes
        final int[] ca = new int[ng];
        for (int i = 0, n = ng; i < n; i++) {
            final int gc = this.in.readTTFUShort();
            ca[i] = gc;
            entries.add(Integer.valueOf(gc));
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(label + " glyph classes: " + toString(ca));
        }
        return GlyphClassTable.createClassTable(entries);
    }

    private GlyphClassTable readClassDefTableFormat2(final String label,
            final long tableOffset, final int classFormat) throws IOException {
        final List entries = new java.util.ArrayList();
        this.in.seekSet(tableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read range record count
        final int nr = this.in.readTTFUShort();
        for (int i = 0, n = nr; i < n; i++) {
            // read range start
            final int s = this.in.readTTFUShort();
            // read range end
            final int e = this.in.readTTFUShort();
            // read range glyph class (mapping) index
            final int m = this.in.readTTFUShort();
            // dump info if debugging
            if (log.isDebugEnabled()) {
                log.debug(label + " range[" + i + "]: [" + s + "," + e + "]: "
                        + m);
            }
            entries.add(new GlyphClassTable.MappingRange(s, e, m));
        }
        return GlyphClassTable.createClassTable(entries);
    }

    private GlyphClassTable readClassDefTable(final String label,
            final long tableOffset) throws IOException {
        GlyphClassTable gct;
        final long cp = this.in.getCurrentPos();
        this.in.seekSet(tableOffset);
        // read class table format
        final int cf = this.in.readTTFUShort();
        if (cf == 1) {
            gct = readClassDefTableFormat1(label, tableOffset, cf);
        } else if (cf == 2) {
            gct = readClassDefTableFormat2(label, tableOffset, cf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported class definition table format: " + cf);
        }
        this.in.seekSet(cp);
        return gct;
    }

    private void readSingleSubTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read delta glyph
        final int dg = this.in.readTTFShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " single substitution subtable format: "
                    + subtableFormat + " (delta)");
            log.debug(tableTag + " single substitution coverage table offset: "
                    + co);
            log.debug(tableTag + " single substitution delta: " + dg);
        }
        // read coverage table
        this.seMapping = readCoverageTable(tableTag
                + " single substitution coverage", subtableOffset + co);
        this.seEntries.add(Integer.valueOf(dg));
    }

    private void readSingleSubTableFormat2(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read glyph count
        final int ng = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " single substitution subtable format: "
                    + subtableFormat + " (mapped)");
            log.debug(tableTag + " single substitution coverage table offset: "
                    + co);
            log.debug(tableTag + " single substitution glyph count: " + ng);
        }
        // read coverage table
        this.seMapping = readCoverageTable(tableTag
                + " single substitution coverage", subtableOffset + co);
        // read glyph substitutions
        final int[] gsa = new int[ng];
        for (int i = 0, n = ng; i < n; i++) {
            final int gs = this.in.readTTFUShort();
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " single substitution glyph[" + i + "]: "
                        + gs);
            }
            gsa[i] = gs;
            this.seEntries.add(Integer.valueOf(gs));
        }
    }

    private int readSingleSubTable(final int lookupType, final int lookupFlags,
            final long subtableOffset) throws IOException {
        this.in.seekSet(subtableOffset);
        // read substitution subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readSingleSubTableFormat1(lookupType, lookupFlags, subtableOffset,
                    sf);
        } else if (sf == 2) {
            readSingleSubTableFormat2(lookupType, lookupFlags, subtableOffset,
                    sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported single substitution subtable format: " + sf);
        }
        return sf;
    }

    private void readMultipleSubTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read sequence count
        final int ns = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " multiple substitution subtable format: "
                    + subtableFormat + " (mapped)");
            log.debug(tableTag
                    + " multiple substitution coverage table offset: " + co);
            log.debug(tableTag + " multiple substitution sequence count: " + ns);
        }
        // read coverage table
        this.seMapping = readCoverageTable(tableTag
                + " multiple substitution coverage", subtableOffset + co);
        // read sequence table offsets
        final int[] soa = new int[ns];
        for (int i = 0, n = ns; i < n; i++) {
            soa[i] = this.in.readTTFUShort();
        }
        // read sequence tables
        final int[][] gsa = new int[ns][];
        for (int i = 0, n = ns; i < n; i++) {
            final int so = soa[i];
            int[] ga;
            if (so > 0) {
                this.in.seekSet(subtableOffset + so);
                // read glyph count
                final int ng = this.in.readTTFUShort();
                ga = new int[ng];
                for (int j = 0; j < ng; j++) {
                    ga[j] = this.in.readTTFUShort();
                }
            } else {
                ga = null;
            }
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " multiple substitution sequence[" + i
                        + "]: " + toString(ga));
            }
            gsa[i] = ga;
        }
        this.seEntries.add(gsa);
    }

    private int readMultipleSubTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read substitution subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readMultipleSubTableFormat1(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported multiple substitution subtable format: " + sf);
        }
        return sf;
    }

    private void readAlternateSubTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read alternate set count
        final int ns = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " alternate substitution subtable format: "
                    + subtableFormat + " (mapped)");
            log.debug(tableTag
                    + " alternate substitution coverage table offset: " + co);
            log.debug(tableTag
                    + " alternate substitution alternate set count: " + ns);
        }
        // read coverage table
        this.seMapping = readCoverageTable(tableTag
                + " alternate substitution coverage", subtableOffset + co);
        // read alternate set table offsets
        final int[] soa = new int[ns];
        for (int i = 0, n = ns; i < n; i++) {
            soa[i] = this.in.readTTFUShort();
        }
        // read alternate set tables
        for (int i = 0, n = ns; i < n; i++) {
            final int so = soa[i];
            this.in.seekSet(subtableOffset + so);
            // read glyph count
            final int ng = this.in.readTTFUShort();
            final int[] ga = new int[ng];
            for (int j = 0; j < ng; j++) {
                final int gs = this.in.readTTFUShort();
                ga[j] = gs;
            }
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " alternate substitution alternate set["
                        + i + "]: " + toString(ga));
            }
            this.seEntries.add(ga);
        }
    }

    private int readAlternateSubTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read substitution subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readAlternateSubTableFormat1(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported alternate substitution subtable format: " + sf);
        }
        return sf;
    }

    private void readLigatureSubTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read ligature set count
        final int ns = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " ligature substitution subtable format: "
                    + subtableFormat + " (mapped)");
            log.debug(tableTag
                    + " ligature substitution coverage table offset: " + co);
            log.debug(tableTag + " ligature substitution ligature set count: "
                    + ns);
        }
        // read coverage table
        this.seMapping = readCoverageTable(tableTag
                + " ligature substitution coverage", subtableOffset + co);
        // read ligature set table offsets
        final int[] soa = new int[ns];
        for (int i = 0, n = ns; i < n; i++) {
            soa[i] = this.in.readTTFUShort();
        }
        // read ligature set tables
        for (int i = 0, n = ns; i < n; i++) {
            final int so = soa[i];
            this.in.seekSet(subtableOffset + so);
            // read ligature table count
            final int nl = this.in.readTTFUShort();
            final int[] loa = new int[nl];
            for (int j = 0; j < nl; j++) {
                loa[j] = this.in.readTTFUShort();
            }
            final List ligs = new java.util.ArrayList();
            for (int j = 0; j < nl; j++) {
                final int lo = loa[j];
                this.in.seekSet(subtableOffset + so + lo);
                // read ligature glyph id
                final int lg = this.in.readTTFUShort();
                // read ligature (input) component count
                final int nc = this.in.readTTFUShort();
                final int[] ca = new int[nc - 1];
                // read ligature (input) component glyph ids
                for (int k = 0; k < nc - 1; k++) {
                    ca[k] = this.in.readTTFUShort();
                }
                if (log.isDebugEnabled()) {
                    log.debug(tableTag + " ligature substitution ligature set["
                            + i + "]: ligature(" + lg + "), components: "
                            + toString(ca));
                }
                ligs.add(new GlyphSubstitutionTable.Ligature(lg, ca));
            }
            this.seEntries.add(new GlyphSubstitutionTable.LigatureSet(ligs));
        }
    }

    private int readLigatureSubTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read substitution subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readLigatureSubTableFormat1(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported ligature substitution subtable format: " + sf);
        }
        return sf;
    }

    private GlyphTable.RuleLookup[] readRuleLookups(final int numLookups,
            final String header) throws IOException {
        final GlyphTable.RuleLookup[] la = new GlyphTable.RuleLookup[numLookups];
        for (int i = 0, n = numLookups; i < n; i++) {
            final int sequenceIndex = this.in.readTTFUShort();
            final int lookupIndex = this.in.readTTFUShort();
            la[i] = new GlyphTable.RuleLookup(sequenceIndex, lookupIndex);
            // dump info if debugging and header is non-null
            if (log.isDebugEnabled() && header != null) {
                log.debug(header + "lookup[" + i + "]: " + la[i]);
            }
        }
        return la;
    }

    private void readContextualSubTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read rule set count
        final int nrs = this.in.readTTFUShort();
        // read rule set offsets
        final int[] rsoa = new int[nrs];
        for (int i = 0; i < nrs; i++) {
            rsoa[i] = this.in.readTTFUShort();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " contextual substitution format: "
                    + subtableFormat + " (glyphs)");
            log.debug(tableTag
                    + " contextual substitution coverage table offset: " + co);
            log.debug(tableTag + " contextual substitution rule set count: "
                    + nrs);
            for (int i = 0; i < nrs; i++) {
                log.debug(tableTag
                        + " contextual substitution rule set offset[" + i
                        + "]: " + rsoa[i]);
            }
        }
        // read coverage table
        GlyphCoverageTable ct;
        if (co > 0) {
            ct = readCoverageTable(tableTag
                    + " contextual substitution coverage", subtableOffset + co);
        } else {
            ct = null;
        }
        // read rule sets
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[nrs];
        String header = null;
        for (int i = 0; i < nrs; i++) {
            GlyphTable.RuleSet rs;
            final int rso = rsoa[i];
            if (rso > 0) {
                // seek to rule set [ i ]
                this.in.seekSet(subtableOffset + rso);
                // read rule count
                final int nr = this.in.readTTFUShort();
                // read rule offsets
                final int[] roa = new int[nr];
                final GlyphTable.Rule[] ra = new GlyphTable.Rule[nr];
                for (int j = 0; j < nr; j++) {
                    roa[j] = this.in.readTTFUShort();
                }
                // read glyph sequence rules
                for (int j = 0; j < nr; j++) {
                    GlyphTable.GlyphSequenceRule r;
                    final int ro = roa[j];
                    if (ro > 0) {
                        // seek to rule [ j ]
                        this.in.seekSet(subtableOffset + rso + ro);
                        // read glyph count
                        final int ng = this.in.readTTFUShort();
                        // read rule lookup count
                        final int nl = this.in.readTTFUShort();
                        // read glyphs
                        final int[] glyphs = new int[ng - 1];
                        for (int k = 0, nk = glyphs.length; k < nk; k++) {
                            glyphs[k] = this.in.readTTFUShort();
                        }
                        // read rule lookups
                        if (log.isDebugEnabled()) {
                            header = tableTag
                                    + " contextual substitution lookups @rule["
                                    + i + "][" + j + "]: ";
                        }
                        final GlyphTable.RuleLookup[] lookups = readRuleLookups(
                                nl, header);
                        r = new GlyphTable.GlyphSequenceRule(lookups, ng,
                                glyphs);
                    } else {
                        r = null;
                    }
                    ra[j] = r;
                }
                rs = new GlyphTable.HomogeneousRuleSet(ra);
            } else {
                rs = null;
            }
            rsa[i] = rs;
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(rsa);
    }

    private void readContextualSubTableFormat2(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read class def table offset
        final int cdo = this.in.readTTFUShort();
        // read class rule set count
        final int ngc = this.in.readTTFUShort();
        // read class rule set offsets
        final int[] csoa = new int[ngc];
        for (int i = 0; i < ngc; i++) {
            csoa[i] = this.in.readTTFUShort();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " contextual substitution format: "
                    + subtableFormat + " (glyph classes)");
            log.debug(tableTag
                    + " contextual substitution coverage table offset: " + co);
            log.debug(tableTag + " contextual substitution class set count: "
                    + ngc);
            for (int i = 0; i < ngc; i++) {
                log.debug(tableTag
                        + " contextual substitution class set offset[" + i
                        + "]: " + csoa[i]);
            }
        }
        // read coverage table
        GlyphCoverageTable ct;
        if (co > 0) {
            ct = readCoverageTable(tableTag
                    + " contextual substitution coverage", subtableOffset + co);
        } else {
            ct = null;
        }
        // read class definition table
        GlyphClassTable cdt;
        if (cdo > 0) {
            cdt = readClassDefTable(tableTag
                    + " contextual substitution class definition",
                    subtableOffset + cdo);
        } else {
            cdt = null;
        }
        // read rule sets
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[ngc];
        String header = null;
        for (int i = 0; i < ngc; i++) {
            final int cso = csoa[i];
            GlyphTable.RuleSet rs;
            if (cso > 0) {
                // seek to rule set [ i ]
                this.in.seekSet(subtableOffset + cso);
                // read rule count
                final int nr = this.in.readTTFUShort();
                // read rule offsets
                final int[] roa = new int[nr];
                final GlyphTable.Rule[] ra = new GlyphTable.Rule[nr];
                for (int j = 0; j < nr; j++) {
                    roa[j] = this.in.readTTFUShort();
                }
                // read glyph sequence rules
                for (int j = 0; j < nr; j++) {
                    final int ro = roa[j];
                    GlyphTable.ClassSequenceRule r;
                    if (ro > 0) {
                        // seek to rule [ j ]
                        this.in.seekSet(subtableOffset + cso + ro);
                        // read glyph count
                        final int ng = this.in.readTTFUShort();
                        // read rule lookup count
                        final int nl = this.in.readTTFUShort();
                        // read classes
                        final int[] classes = new int[ng - 1];
                        for (int k = 0, nk = classes.length; k < nk; k++) {
                            classes[k] = this.in.readTTFUShort();
                        }
                        // read rule lookups
                        if (log.isDebugEnabled()) {
                            header = tableTag
                                    + " contextual substitution lookups @rule["
                                    + i + "][" + j + "]: ";
                        }
                        final GlyphTable.RuleLookup[] lookups = readRuleLookups(
                                nl, header);
                        r = new GlyphTable.ClassSequenceRule(lookups, ng,
                                classes);
                    } else {
                        assert ro > 0 : "unexpected null subclass rule offset";
                        r = null;
                    }
                    ra[j] = r;
                }
                rs = new GlyphTable.HomogeneousRuleSet(ra);
            } else {
                rs = null;
            }
            rsa[i] = rs;
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(cdt);
        this.seEntries.add(Integer.valueOf(ngc));
        this.seEntries.add(rsa);
    }

    private void readContextualSubTableFormat3(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read glyph (input sequence length) count
        final int ng = this.in.readTTFUShort();
        // read substitution lookup count
        final int nl = this.in.readTTFUShort();
        // read glyph coverage offsets, one per glyph input sequence length
        // count
        final int[] gcoa = new int[ng];
        for (int i = 0; i < ng; i++) {
            gcoa[i] = this.in.readTTFUShort();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " contextual substitution format: "
                    + subtableFormat + " (glyph sets)");
            log.debug(tableTag
                    + " contextual substitution glyph input sequence length count: "
                    + ng);
            log.debug(tableTag + " contextual substitution lookup count: " + nl);
            for (int i = 0; i < ng; i++) {
                log.debug(tableTag
                        + " contextual substitution coverage table offset[" + i
                        + "]: " + gcoa[i]);
            }
        }
        // read coverage tables
        final GlyphCoverageTable[] gca = new GlyphCoverageTable[ng];
        for (int i = 0; i < ng; i++) {
            final int gco = gcoa[i];
            GlyphCoverageTable gct;
            if (gco > 0) {
                gct = readCoverageTable(tableTag
                        + " contextual substitution coverage[" + i + "]",
                        subtableOffset + gco);
            } else {
                gct = null;
            }
            gca[i] = gct;
        }
        // read rule lookups
        String header = null;
        if (log.isDebugEnabled()) {
            header = tableTag + " contextual substitution lookups: ";
        }
        final GlyphTable.RuleLookup[] lookups = readRuleLookups(nl, header);
        // construct rule, rule set, and rule set array
        final GlyphTable.Rule r = new GlyphTable.CoverageSequenceRule(lookups,
                ng, gca);
        final GlyphTable.RuleSet rs = new GlyphTable.HomogeneousRuleSet(
                new GlyphTable.Rule[] { r });
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[] { rs };
        // store results
        assert gca != null && gca.length > 0;
        this.seMapping = gca[0];
        this.seEntries.add(rsa);
    }

    private int readContextualSubTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read substitution subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readContextualSubTableFormat1(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else if (sf == 2) {
            readContextualSubTableFormat2(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else if (sf == 3) {
            readContextualSubTableFormat3(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported contextual substitution subtable format: "
                            + sf);
        }
        return sf;
    }

    private void readChainedContextualSubTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read rule set count
        final int nrs = this.in.readTTFUShort();
        // read rule set offsets
        final int[] rsoa = new int[nrs];
        for (int i = 0; i < nrs; i++) {
            rsoa[i] = this.in.readTTFUShort();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " chained contextual substitution format: "
                    + subtableFormat + " (glyphs)");
            log.debug(tableTag
                    + " chained contextual substitution coverage table offset: "
                    + co);
            log.debug(tableTag
                    + " chained contextual substitution rule set count: " + nrs);
            for (int i = 0; i < nrs; i++) {
                log.debug(tableTag
                        + " chained contextual substitution rule set offset["
                        + i + "]: " + rsoa[i]);
            }
        }
        // read coverage table
        GlyphCoverageTable ct;
        if (co > 0) {
            ct = readCoverageTable(tableTag
                    + " chained contextual substitution coverage",
                    subtableOffset + co);
        } else {
            ct = null;
        }
        // read rule sets
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[nrs];
        String header = null;
        for (int i = 0; i < nrs; i++) {
            GlyphTable.RuleSet rs;
            final int rso = rsoa[i];
            if (rso > 0) {
                // seek to rule set [ i ]
                this.in.seekSet(subtableOffset + rso);
                // read rule count
                final int nr = this.in.readTTFUShort();
                // read rule offsets
                final int[] roa = new int[nr];
                final GlyphTable.Rule[] ra = new GlyphTable.Rule[nr];
                for (int j = 0; j < nr; j++) {
                    roa[j] = this.in.readTTFUShort();
                }
                // read glyph sequence rules
                for (int j = 0; j < nr; j++) {
                    GlyphTable.ChainedGlyphSequenceRule r;
                    final int ro = roa[j];
                    if (ro > 0) {
                        // seek to rule [ j ]
                        this.in.seekSet(subtableOffset + rso + ro);
                        // read backtrack glyph count
                        final int nbg = this.in.readTTFUShort();
                        // read backtrack glyphs
                        final int[] backtrackGlyphs = new int[nbg];
                        for (int k = 0, nk = backtrackGlyphs.length; k < nk; k++) {
                            backtrackGlyphs[k] = this.in.readTTFUShort();
                        }
                        // read input glyph count
                        final int nig = this.in.readTTFUShort();
                        // read glyphs
                        final int[] glyphs = new int[nig - 1];
                        for (int k = 0, nk = glyphs.length; k < nk; k++) {
                            glyphs[k] = this.in.readTTFUShort();
                        }
                        // read lookahead glyph count
                        final int nlg = this.in.readTTFUShort();
                        // read lookahead glyphs
                        final int[] lookaheadGlyphs = new int[nlg];
                        for (int k = 0, nk = lookaheadGlyphs.length; k < nk; k++) {
                            lookaheadGlyphs[k] = this.in.readTTFUShort();
                        }
                        // read rule lookup count
                        final int nl = this.in.readTTFUShort();
                        // read rule lookups
                        if (log.isDebugEnabled()) {
                            header = tableTag
                                    + " contextual substitution lookups @rule["
                                    + i + "][" + j + "]: ";
                        }
                        final GlyphTable.RuleLookup[] lookups = readRuleLookups(
                                nl, header);
                        r = new GlyphTable.ChainedGlyphSequenceRule(lookups,
                                nig, glyphs, backtrackGlyphs, lookaheadGlyphs);
                    } else {
                        r = null;
                    }
                    ra[j] = r;
                }
                rs = new GlyphTable.HomogeneousRuleSet(ra);
            } else {
                rs = null;
            }
            rsa[i] = rs;
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(rsa);
    }

    private void readChainedContextualSubTableFormat2(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read backtrack class def table offset
        final int bcdo = this.in.readTTFUShort();
        // read input class def table offset
        final int icdo = this.in.readTTFUShort();
        // read lookahead class def table offset
        final int lcdo = this.in.readTTFUShort();
        // read class set count
        final int ngc = this.in.readTTFUShort();
        // read class set offsets
        final int[] csoa = new int[ngc];
        for (int i = 0; i < ngc; i++) {
            csoa[i] = this.in.readTTFUShort();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " chained contextual substitution format: "
                    + subtableFormat + " (glyph classes)");
            log.debug(tableTag
                    + " chained contextual substitution coverage table offset: "
                    + co);
            log.debug(tableTag
                    + " chained contextual substitution class set count: "
                    + ngc);
            for (int i = 0; i < ngc; i++) {
                log.debug(tableTag
                        + " chained contextual substitution class set offset["
                        + i + "]: " + csoa[i]);
            }
        }
        // read coverage table
        GlyphCoverageTable ct;
        if (co > 0) {
            ct = readCoverageTable(tableTag
                    + " chained contextual substitution coverage",
                    subtableOffset + co);
        } else {
            ct = null;
        }
        // read backtrack class definition table
        GlyphClassTable bcdt;
        if (bcdo > 0) {
            bcdt = readClassDefTable(tableTag
                    + " contextual substitution backtrack class definition",
                    subtableOffset + bcdo);
        } else {
            bcdt = null;
        }
        // read input class definition table
        GlyphClassTable icdt;
        if (icdo > 0) {
            icdt = readClassDefTable(tableTag
                    + " contextual substitution input class definition",
                    subtableOffset + icdo);
        } else {
            icdt = null;
        }
        // read lookahead class definition table
        GlyphClassTable lcdt;
        if (lcdo > 0) {
            lcdt = readClassDefTable(tableTag
                    + " contextual substitution lookahead class definition",
                    subtableOffset + lcdo);
        } else {
            lcdt = null;
        }
        // read rule sets
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[ngc];
        String header = null;
        for (int i = 0; i < ngc; i++) {
            final int cso = csoa[i];
            GlyphTable.RuleSet rs;
            if (cso > 0) {
                // seek to rule set [ i ]
                this.in.seekSet(subtableOffset + cso);
                // read rule count
                final int nr = this.in.readTTFUShort();
                // read rule offsets
                final int[] roa = new int[nr];
                final GlyphTable.Rule[] ra = new GlyphTable.Rule[nr];
                for (int j = 0; j < nr; j++) {
                    roa[j] = this.in.readTTFUShort();
                }
                // read glyph sequence rules
                for (int j = 0; j < nr; j++) {
                    final int ro = roa[j];
                    GlyphTable.ChainedClassSequenceRule r;
                    if (ro > 0) {
                        // seek to rule [ j ]
                        this.in.seekSet(subtableOffset + cso + ro);
                        // read backtrack glyph class count
                        final int nbc = this.in.readTTFUShort();
                        // read backtrack glyph classes
                        final int[] backtrackClasses = new int[nbc];
                        for (int k = 0, nk = backtrackClasses.length; k < nk; k++) {
                            backtrackClasses[k] = this.in.readTTFUShort();
                        }
                        // read input glyph class count
                        final int nic = this.in.readTTFUShort();
                        // read input glyph classes
                        final int[] classes = new int[nic - 1];
                        for (int k = 0, nk = classes.length; k < nk; k++) {
                            classes[k] = this.in.readTTFUShort();
                        }
                        // read lookahead glyph class count
                        final int nlc = this.in.readTTFUShort();
                        // read lookahead glyph classes
                        final int[] lookaheadClasses = new int[nlc];
                        for (int k = 0, nk = lookaheadClasses.length; k < nk; k++) {
                            lookaheadClasses[k] = this.in.readTTFUShort();
                        }
                        // read rule lookup count
                        final int nl = this.in.readTTFUShort();
                        // read rule lookups
                        if (log.isDebugEnabled()) {
                            header = tableTag
                                    + " contextual substitution lookups @rule["
                                    + i + "][" + j + "]: ";
                        }
                        final GlyphTable.RuleLookup[] lookups = readRuleLookups(
                                nl, header);
                        r = new GlyphTable.ChainedClassSequenceRule(lookups,
                                nic, classes, backtrackClasses,
                                lookaheadClasses);
                    } else {
                        r = null;
                    }
                    ra[j] = r;
                }
                rs = new GlyphTable.HomogeneousRuleSet(ra);
            } else {
                rs = null;
            }
            rsa[i] = rs;
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(icdt);
        this.seEntries.add(bcdt);
        this.seEntries.add(lcdt);
        this.seEntries.add(Integer.valueOf(ngc));
        this.seEntries.add(rsa);
    }

    private void readChainedContextualSubTableFormat3(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read backtrack glyph count
        final int nbg = this.in.readTTFUShort();
        // read backtrack glyph coverage offsets
        final int[] bgcoa = new int[nbg];
        for (int i = 0; i < nbg; i++) {
            bgcoa[i] = this.in.readTTFUShort();
        }
        // read input glyph count
        final int nig = this.in.readTTFUShort();
        // read input glyph coverage offsets
        final int[] igcoa = new int[nig];
        for (int i = 0; i < nig; i++) {
            igcoa[i] = this.in.readTTFUShort();
        }
        // read lookahead glyph count
        final int nlg = this.in.readTTFUShort();
        // read lookahead glyph coverage offsets
        final int[] lgcoa = new int[nlg];
        for (int i = 0; i < nlg; i++) {
            lgcoa[i] = this.in.readTTFUShort();
        }
        // read substitution lookup count
        final int nl = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " chained contextual substitution format: "
                    + subtableFormat + " (glyph sets)");
            log.debug(tableTag
                    + " chained contextual substitution backtrack glyph count: "
                    + nbg);
            for (int i = 0; i < nbg; i++) {
                log.debug(tableTag
                        + " chained contextual substitution backtrack coverage table offset["
                        + i + "]: " + bgcoa[i]);
            }
            log.debug(tableTag
                    + " chained contextual substitution input glyph count: "
                    + nig);
            for (int i = 0; i < nig; i++) {
                log.debug(tableTag
                        + " chained contextual substitution input coverage table offset["
                        + i + "]: " + igcoa[i]);
            }
            log.debug(tableTag
                    + " chained contextual substitution lookahead glyph count: "
                    + nlg);
            for (int i = 0; i < nlg; i++) {
                log.debug(tableTag
                        + " chained contextual substitution lookahead coverage table offset["
                        + i + "]: " + lgcoa[i]);
            }
            log.debug(tableTag
                    + " chained contextual substitution lookup count: " + nl);
        }
        // read backtrack coverage tables
        final GlyphCoverageTable[] bgca = new GlyphCoverageTable[nbg];
        for (int i = 0; i < nbg; i++) {
            final int bgco = bgcoa[i];
            GlyphCoverageTable bgct;
            if (bgco > 0) {
                bgct = readCoverageTable(
                        tableTag
                        + " chained contextual substitution backtrack coverage["
                        + i + "]", subtableOffset + bgco);
            } else {
                bgct = null;
            }
            bgca[i] = bgct;
        }
        // read input coverage tables
        final GlyphCoverageTable[] igca = new GlyphCoverageTable[nig];
        for (int i = 0; i < nig; i++) {
            final int igco = igcoa[i];
            GlyphCoverageTable igct;
            if (igco > 0) {
                igct = readCoverageTable(tableTag
                        + " chained contextual substitution input coverage["
                        + i + "]", subtableOffset + igco);
            } else {
                igct = null;
            }
            igca[i] = igct;
        }
        // read lookahead coverage tables
        final GlyphCoverageTable[] lgca = new GlyphCoverageTable[nlg];
        for (int i = 0; i < nlg; i++) {
            final int lgco = lgcoa[i];
            GlyphCoverageTable lgct;
            if (lgco > 0) {
                lgct = readCoverageTable(
                        tableTag
                        + " chained contextual substitution lookahead coverage["
                        + i + "]", subtableOffset + lgco);
            } else {
                lgct = null;
            }
            lgca[i] = lgct;
        }
        // read rule lookups
        String header = null;
        if (log.isDebugEnabled()) {
            header = tableTag + " chained contextual substitution lookups: ";
        }
        final GlyphTable.RuleLookup[] lookups = readRuleLookups(nl, header);
        // construct rule, rule set, and rule set array
        final GlyphTable.Rule r = new GlyphTable.ChainedCoverageSequenceRule(
                lookups, nig, igca, bgca, lgca);
        final GlyphTable.RuleSet rs = new GlyphTable.HomogeneousRuleSet(
                new GlyphTable.Rule[] { r });
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[] { rs };
        // store results
        assert igca != null && igca.length > 0;
        this.seMapping = igca[0];
        this.seEntries.add(rsa);
    }

    private int readChainedContextualSubTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read substitution subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readChainedContextualSubTableFormat1(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else if (sf == 2) {
            readChainedContextualSubTableFormat2(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else if (sf == 3) {
            readChainedContextualSubTableFormat3(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported chained contextual substitution subtable format: "
                            + sf);
        }
        return sf;
    }

    private void readExtensionSubTableFormat1(final int lookupType,
            final int lookupFlags, final int lookupSequence,
            final int subtableSequence, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read extension lookup type
        final int lt = this.in.readTTFUShort();
        // read extension offset
        final long eo = this.in.readTTFULong();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " extension substitution subtable format: "
                    + subtableFormat);
            log.debug(tableTag + " extension substitution lookup type: " + lt);
            log.debug(tableTag
                    + " extension substitution lookup table offset: " + eo);
        }
        // read referenced subtable from extended offset
        readGSUBSubtable(lt, lookupFlags, lookupSequence, subtableSequence,
                subtableOffset + eo);
    }

    private int readExtensionSubTable(final int lookupType,
            final int lookupFlags, final int lookupSequence,
            final int subtableSequence, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read substitution subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readExtensionSubTableFormat1(lookupType, lookupFlags,
                    lookupSequence, subtableSequence, subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported extension substitution subtable format: " + sf);
        }
        return sf;
    }

    private void readReverseChainedSingleSubTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GSUB";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read backtrack glyph count
        final int nbg = this.in.readTTFUShort();
        // read backtrack glyph coverage offsets
        final int[] bgcoa = new int[nbg];
        for (int i = 0; i < nbg; i++) {
            bgcoa[i] = this.in.readTTFUShort();
        }
        // read lookahead glyph count
        final int nlg = this.in.readTTFUShort();
        // read backtrack glyph coverage offsets
        final int[] lgcoa = new int[nlg];
        for (int i = 0; i < nlg; i++) {
            lgcoa[i] = this.in.readTTFUShort();
        }
        // read substitution (output) glyph count
        final int ng = this.in.readTTFUShort();
        // read substitution (output) glyphs
        final int[] glyphs = new int[ng];
        for (int i = 0, n = ng; i < n; i++) {
            glyphs[i] = this.in.readTTFUShort();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag
                    + " reverse chained contextual substitution format: "
                    + subtableFormat);
            log.debug(tableTag
                    + " reverse chained contextual substitution coverage table offset: "
                    + co);
            log.debug(tableTag
                    + " reverse chained contextual substitution backtrack glyph count: "
                    + nbg);
            for (int i = 0; i < nbg; i++) {
                log.debug(tableTag
                        + " reverse chained contextual substitution backtrack coverage table offset["
                        + i + "]: " + bgcoa[i]);
            }
            log.debug(tableTag
                    + " reverse chained contextual substitution lookahead glyph count: "
                    + nlg);
            for (int i = 0; i < nlg; i++) {
                log.debug(tableTag
                        + " reverse chained contextual substitution lookahead coverage table offset["
                        + i + "]: " + lgcoa[i]);
            }
            log.debug(tableTag
                    + " reverse chained contextual substitution glyphs: "
                    + toString(glyphs));
        }
        // read coverage table
        final GlyphCoverageTable ct = readCoverageTable(tableTag
                + " reverse chained contextual substitution coverage",
                subtableOffset + co);
        // read backtrack coverage tables
        final GlyphCoverageTable[] bgca = new GlyphCoverageTable[nbg];
        for (int i = 0; i < nbg; i++) {
            final int bgco = bgcoa[i];
            GlyphCoverageTable bgct;
            if (bgco > 0) {
                bgct = readCoverageTable(
                        tableTag
                        + " reverse chained contextual substitution backtrack coverage["
                        + i + "]", subtableOffset + bgco);
            } else {
                bgct = null;
            }
            bgca[i] = bgct;
        }
        // read lookahead coverage tables
        final GlyphCoverageTable[] lgca = new GlyphCoverageTable[nlg];
        for (int i = 0; i < nlg; i++) {
            final int lgco = lgcoa[i];
            GlyphCoverageTable lgct;
            if (lgco > 0) {
                lgct = readCoverageTable(
                        tableTag
                        + " reverse chained contextual substitution lookahead coverage["
                        + i + "]", subtableOffset + lgco);
            } else {
                lgct = null;
            }
            lgca[i] = lgct;
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(bgca);
        this.seEntries.add(lgca);
        this.seEntries.add(glyphs);
    }

    private int readReverseChainedSingleSubTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read substitution subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readReverseChainedSingleSubTableFormat1(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported reverse chained single substitution subtable format: "
                            + sf);
        }
        return sf;
    }

    private void readGSUBSubtable(final int lookupType, final int lookupFlags,
            final int lookupSequence, final int subtableSequence,
            final long subtableOffset) throws IOException {
        initATSubState();
        int subtableFormat = -1;
        switch (lookupType) {
        case GSUBLookupType.SINGLE:
            subtableFormat = readSingleSubTable(lookupType, lookupFlags,
                    subtableOffset);
            break;
        case GSUBLookupType.MULTIPLE:
            subtableFormat = readMultipleSubTable(lookupType, lookupFlags,
                    subtableOffset);
            break;
        case GSUBLookupType.ALTERNATE:
            subtableFormat = readAlternateSubTable(lookupType, lookupFlags,
                    subtableOffset);
            break;
        case GSUBLookupType.LIGATURE:
            subtableFormat = readLigatureSubTable(lookupType, lookupFlags,
                    subtableOffset);
            break;
        case GSUBLookupType.CONTEXTUAL:
            subtableFormat = readContextualSubTable(lookupType, lookupFlags,
                    subtableOffset);
            break;
        case GSUBLookupType.CHAINED_CONTEXTUAL:
            subtableFormat = readChainedContextualSubTable(lookupType,
                    lookupFlags, subtableOffset);
            break;
        case GSUBLookupType.REVERSE_CHAINED_SINGLE:
            subtableFormat = readReverseChainedSingleSubTable(lookupType,
                    lookupFlags, subtableOffset);
            break;
        case GSUBLookupType.EXTENSION:
            subtableFormat = readExtensionSubTable(lookupType, lookupFlags,
                    lookupSequence, subtableSequence, subtableOffset);
            break;
        default:
            break;
        }
        extractSESubState(GlyphTable.GLYPH_TABLE_TYPE_SUBSTITUTION, lookupType,
                lookupFlags, lookupSequence, subtableSequence, subtableFormat);
        resetATSubState();
    }

    private GlyphPositioningTable.DeviceTable readPosDeviceTable(
            final long subtableOffset, final long deviceTableOffset)
                    throws IOException {
        final long cp = this.in.getCurrentPos();
        this.in.seekSet(subtableOffset + deviceTableOffset);
        // read start size
        final int ss = this.in.readTTFUShort();
        // read end size
        final int es = this.in.readTTFUShort();
        // read delta format
        final int df = this.in.readTTFUShort();
        int s1;
        int m1;
        int dm;
        int dd;
        int s2;
        if (df == 1) {
            s1 = 14;
            m1 = 0x3;
            dm = 1;
            dd = 4;
            s2 = 2;
        } else if (df == 2) {
            s1 = 12;
            m1 = 0xF;
            dm = 7;
            dd = 16;
            s2 = 4;
        } else if (df == 3) {
            s1 = 8;
            m1 = 0xFF;
            dm = 127;
            dd = 256;
            s2 = 8;
        } else {
            log.debug("unsupported device table delta format: " + df
                    + ", ignoring device table");
            return null;
        }
        // read deltas
        final int n = es - ss + 1;
        if (n < 0) {
            log.debug("invalid device table delta count: " + n
                    + ", ignoring device table");
            return null;
        }
        final int[] da = new int[n];
        for (int i = 0; i < n && s2 > 0;) {
            int p = this.in.readTTFUShort();
            for (int j = 0, k = 16 / s2; j < k; j++) {
                int d = p >> s1 & m1;
                if (d > dm) {
                    d -= dd;
                }
                if (i < n) {
                    da[i++] = d;
                } else {
                    break;
                }
                p <<= s2;
            }
        }
        this.in.seekSet(cp);
        return new GlyphPositioningTable.DeviceTable(ss, es, da);
    }

    private GlyphPositioningTable.Value readPosValue(final long subtableOffset,
            final int valueFormat) throws IOException {
        // XPlacement
        int xp;
        if ((valueFormat & GlyphPositioningTable.Value.X_PLACEMENT) != 0) {
            xp = this.ttf.convertTTFUnit2PDFUnit(this.in.readTTFShort());
        } else {
            xp = 0;
        }
        // YPlacement
        int yp;
        if ((valueFormat & GlyphPositioningTable.Value.Y_PLACEMENT) != 0) {
            yp = this.ttf.convertTTFUnit2PDFUnit(this.in.readTTFShort());
        } else {
            yp = 0;
        }
        // XAdvance
        int xa;
        if ((valueFormat & GlyphPositioningTable.Value.X_ADVANCE) != 0) {
            xa = this.ttf.convertTTFUnit2PDFUnit(this.in.readTTFShort());
        } else {
            xa = 0;
        }
        // YAdvance
        int ya;
        if ((valueFormat & GlyphPositioningTable.Value.Y_ADVANCE) != 0) {
            ya = this.ttf.convertTTFUnit2PDFUnit(this.in.readTTFShort());
        } else {
            ya = 0;
        }
        // XPlaDevice
        GlyphPositioningTable.DeviceTable xpd;
        if ((valueFormat & GlyphPositioningTable.Value.X_PLACEMENT_DEVICE) != 0) {
            final int xpdo = this.in.readTTFUShort();
            xpd = readPosDeviceTable(subtableOffset, xpdo);
        } else {
            xpd = null;
        }
        // YPlaDevice
        GlyphPositioningTable.DeviceTable ypd;
        if ((valueFormat & GlyphPositioningTable.Value.Y_PLACEMENT_DEVICE) != 0) {
            final int ypdo = this.in.readTTFUShort();
            ypd = readPosDeviceTable(subtableOffset, ypdo);
        } else {
            ypd = null;
        }
        // XAdvDevice
        GlyphPositioningTable.DeviceTable xad;
        if ((valueFormat & GlyphPositioningTable.Value.X_ADVANCE_DEVICE) != 0) {
            final int xado = this.in.readTTFUShort();
            xad = readPosDeviceTable(subtableOffset, xado);
        } else {
            xad = null;
        }
        // YAdvDevice
        GlyphPositioningTable.DeviceTable yad;
        if ((valueFormat & GlyphPositioningTable.Value.Y_ADVANCE_DEVICE) != 0) {
            final int yado = this.in.readTTFUShort();
            yad = readPosDeviceTable(subtableOffset, yado);
        } else {
            yad = null;
        }
        return new GlyphPositioningTable.Value(xp, yp, xa, ya, xpd, ypd, xad,
                yad);
    }

    private void readSinglePosTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read value format
        final int vf = this.in.readTTFUShort();
        // read value
        final GlyphPositioningTable.Value v = readPosValue(subtableOffset, vf);
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " single positioning subtable format: "
                    + subtableFormat + " (delta)");
            log.debug(tableTag + " single positioning coverage table offset: "
                    + co);
            log.debug(tableTag + " single positioning value: " + v);
        }
        // read coverage table
        final GlyphCoverageTable ct = readCoverageTable(tableTag
                + " single positioning coverage", subtableOffset + co);
        // store results
        this.seMapping = ct;
        this.seEntries.add(v);
    }

    private void readSinglePosTableFormat2(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read value format
        final int vf = this.in.readTTFUShort();
        // read value count
        final int nv = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " single positioning subtable format: "
                    + subtableFormat + " (mapped)");
            log.debug(tableTag + " single positioning coverage table offset: "
                    + co);
            log.debug(tableTag + " single positioning value count: " + nv);
        }
        // read coverage table
        final GlyphCoverageTable ct = readCoverageTable(tableTag
                + " single positioning coverage", subtableOffset + co);
        // read positioning values
        final GlyphPositioningTable.Value[] pva = new GlyphPositioningTable.Value[nv];
        for (int i = 0, n = nv; i < n; i++) {
            final GlyphPositioningTable.Value pv = readPosValue(subtableOffset,
                    vf);
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " single positioning value[" + i + "]: "
                        + pv);
            }
            pva[i] = pv;
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(pva);
    }

    private int readSinglePosTable(final int lookupType, final int lookupFlags,
            final long subtableOffset) throws IOException {
        this.in.seekSet(subtableOffset);
        // read positionining subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readSinglePosTableFormat1(lookupType, lookupFlags, subtableOffset,
                    sf);
        } else if (sf == 2) {
            readSinglePosTableFormat2(lookupType, lookupFlags, subtableOffset,
                    sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported single positioning subtable format: " + sf);
        }
        return sf;
    }

    private GlyphPositioningTable.PairValues readPosPairValues(
            final long subtableOffset, final boolean hasGlyph, final int vf1,
            final int vf2) throws IOException {
        // read glyph (if present)
        int glyph;
        if (hasGlyph) {
            glyph = this.in.readTTFUShort();
        } else {
            glyph = 0;
        }
        // read first value (if present)
        GlyphPositioningTable.Value v1;
        if (vf1 != 0) {
            v1 = readPosValue(subtableOffset, vf1);
        } else {
            v1 = null;
        }
        // read second value (if present)
        GlyphPositioningTable.Value v2;
        if (vf2 != 0) {
            v2 = readPosValue(subtableOffset, vf2);
        } else {
            v2 = null;
        }
        return new GlyphPositioningTable.PairValues(glyph, v1, v2);
    }

    private GlyphPositioningTable.PairValues[] readPosPairSetTable(
            final long subtableOffset, final int pairSetTableOffset,
            final int vf1, final int vf2) throws IOException {
        final String tableTag = "GPOS";
        final long cp = this.in.getCurrentPos();
        this.in.seekSet(subtableOffset + pairSetTableOffset);
        // read pair values count
        final int npv = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " pair set table offset: "
                    + pairSetTableOffset);
            log.debug(tableTag + " pair set table values count: " + npv);
        }
        // read pair values
        final GlyphPositioningTable.PairValues[] pva = new GlyphPositioningTable.PairValues[npv];
        for (int i = 0, n = npv; i < n; i++) {
            final GlyphPositioningTable.PairValues pv = readPosPairValues(
                    subtableOffset, true, vf1, vf2);
            pva[i] = pv;
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " pair set table value[" + i + "]: " + pv);
            }
        }
        this.in.seekSet(cp);
        return pva;
    }

    private void readPairPosTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read value format for first glyph
        final int vf1 = this.in.readTTFUShort();
        // read value format for second glyph
        final int vf2 = this.in.readTTFUShort();
        // read number (count) of pair sets
        final int nps = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " pair positioning subtable format: "
                    + subtableFormat + " (glyphs)");
            log.debug(tableTag + " pair positioning coverage table offset: "
                    + co);
            log.debug(tableTag + " pair positioning value format #1: " + vf1);
            log.debug(tableTag + " pair positioning value format #2: " + vf2);
        }
        // read coverage table
        final GlyphCoverageTable ct = readCoverageTable(tableTag
                + " pair positioning coverage", subtableOffset + co);
        // read pair value matrix
        final GlyphPositioningTable.PairValues[][] pvm = new GlyphPositioningTable.PairValues[nps][];
        for (int i = 0, n = nps; i < n; i++) {
            // read pair set offset
            final int pso = this.in.readTTFUShort();
            // read pair set table at offset
            pvm[i] = readPosPairSetTable(subtableOffset, pso, vf1, vf2);
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(pvm);
    }

    private void readPairPosTableFormat2(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read value format for first glyph
        final int vf1 = this.in.readTTFUShort();
        // read value format for second glyph
        final int vf2 = this.in.readTTFUShort();
        // read class def 1 offset
        final int cd1o = this.in.readTTFUShort();
        // read class def 2 offset
        final int cd2o = this.in.readTTFUShort();
        // read number (count) of classes in class def 1 table
        final int nc1 = this.in.readTTFUShort();
        // read number (count) of classes in class def 2 table
        final int nc2 = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " pair positioning subtable format: "
                    + subtableFormat + " (glyph classes)");
            log.debug(tableTag + " pair positioning coverage table offset: "
                    + co);
            log.debug(tableTag + " pair positioning value format #1: " + vf1);
            log.debug(tableTag + " pair positioning value format #2: " + vf2);
            log.debug(tableTag
                    + " pair positioning class def table #1 offset: " + cd1o);
            log.debug(tableTag
                    + " pair positioning class def table #2 offset: " + cd2o);
            log.debug(tableTag + " pair positioning class #1 count: " + nc1);
            log.debug(tableTag + " pair positioning class #2 count: " + nc2);
        }
        // read coverage table
        final GlyphCoverageTable ct = readCoverageTable(tableTag
                + " pair positioning coverage", subtableOffset + co);
        // read class definition table #1
        final GlyphClassTable cdt1 = readClassDefTable(tableTag
                + " pair positioning class definition #1", subtableOffset
                + cd1o);
        // read class definition table #2
        final GlyphClassTable cdt2 = readClassDefTable(tableTag
                + " pair positioning class definition #2", subtableOffset
                + cd2o);
        // read pair value matrix
        final GlyphPositioningTable.PairValues[][] pvm = new GlyphPositioningTable.PairValues[nc1][nc2];
        for (int i = 0; i < nc1; i++) {
            for (int j = 0; j < nc2; j++) {
                final GlyphPositioningTable.PairValues pv = readPosPairValues(
                        subtableOffset, false, vf1, vf2);
                pvm[i][j] = pv;
                if (log.isDebugEnabled()) {
                    log.debug(tableTag + " pair set table value[" + i + "]["
                            + j + "]: " + pv);
                }
            }
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(cdt1);
        this.seEntries.add(cdt2);
        this.seEntries.add(Integer.valueOf(nc1));
        this.seEntries.add(Integer.valueOf(nc2));
        this.seEntries.add(pvm);
    }

    private int readPairPosTable(final int lookupType, final int lookupFlags,
            final long subtableOffset) throws IOException {
        this.in.seekSet(subtableOffset);
        // read positioning subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readPairPosTableFormat1(lookupType, lookupFlags, subtableOffset, sf);
        } else if (sf == 2) {
            readPairPosTableFormat2(lookupType, lookupFlags, subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported pair positioning subtable format: " + sf);
        }
        return sf;
    }

    private GlyphPositioningTable.Anchor readPosAnchor(
            final long anchorTableOffset) throws IOException {
        GlyphPositioningTable.Anchor a;
        final long cp = this.in.getCurrentPos();
        this.in.seekSet(anchorTableOffset);
        // read anchor table format
        final int af = this.in.readTTFUShort();
        if (af == 1) {
            // read x coordinate
            final int x = this.ttf.convertTTFUnit2PDFUnit(this.in
                    .readTTFShort());
            // read y coordinate
            final int y = this.ttf.convertTTFUnit2PDFUnit(this.in
                    .readTTFShort());
            a = new GlyphPositioningTable.Anchor(x, y);
        } else if (af == 2) {
            // read x coordinate
            final int x = this.ttf.convertTTFUnit2PDFUnit(this.in
                    .readTTFShort());
            // read y coordinate
            final int y = this.ttf.convertTTFUnit2PDFUnit(this.in
                    .readTTFShort());
            // read anchor point index
            final int ap = this.in.readTTFUShort();
            a = new GlyphPositioningTable.Anchor(x, y, ap);
        } else if (af == 3) {
            // read x coordinate
            final int x = this.ttf.convertTTFUnit2PDFUnit(this.in
                    .readTTFShort());
            // read y coordinate
            final int y = this.ttf.convertTTFUnit2PDFUnit(this.in
                    .readTTFShort());
            // read x device table offset
            final int xdo = this.in.readTTFUShort();
            // read y device table offset
            final int ydo = this.in.readTTFUShort();
            // read x device table (if present)
            GlyphPositioningTable.DeviceTable xd;
            if (xdo != 0) {
                xd = readPosDeviceTable(cp, xdo);
            } else {
                xd = null;
            }
            // read y device table (if present)
            GlyphPositioningTable.DeviceTable yd;
            if (ydo != 0) {
                yd = readPosDeviceTable(cp, ydo);
            } else {
                yd = null;
            }
            a = new GlyphPositioningTable.Anchor(x, y, xd, yd);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported positioning anchor format: " + af);
        }
        this.in.seekSet(cp);
        return a;
    }

    private void readCursivePosTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read entry/exit count
        final int ec = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " cursive positioning subtable format: "
                    + subtableFormat);
            log.debug(tableTag + " cursive positioning coverage table offset: "
                    + co);
            log.debug(tableTag + " cursive positioning entry/exit count: " + ec);
        }
        // read coverage table
        final GlyphCoverageTable ct = readCoverageTable(tableTag
                + " cursive positioning coverage", subtableOffset + co);
        // read entry/exit records
        final GlyphPositioningTable.Anchor[] aa = new GlyphPositioningTable.Anchor[ec * 2];
        for (int i = 0, n = ec; i < n; i++) {
            // read entry anchor offset
            final int eno = this.in.readTTFUShort();
            // read exit anchor offset
            final int exo = this.in.readTTFUShort();
            // read entry anchor
            GlyphPositioningTable.Anchor ena;
            if (eno > 0) {
                ena = readPosAnchor(subtableOffset + eno);
            } else {
                ena = null;
            }
            // read exit anchor
            GlyphPositioningTable.Anchor exa;
            if (exo > 0) {
                exa = readPosAnchor(subtableOffset + exo);
            } else {
                exa = null;
            }
            aa[i * 2 + 0] = ena;
            aa[i * 2 + 1] = exa;
            if (log.isDebugEnabled()) {
                if (ena != null) {
                    log.debug(tableTag + " cursive entry anchor [" + i + "]: "
                            + ena);
                }
                if (exa != null) {
                    log.debug(tableTag + " cursive exit anchor  [" + i + "]: "
                            + exa);
                }
            }
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(aa);
    }

    private int readCursivePosTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read positioning subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readCursivePosTableFormat1(lookupType, lookupFlags, subtableOffset,
                    sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported cursive positioning subtable format: " + sf);
        }
        return sf;
    }

    private void readMarkToBasePosTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read mark coverage offset
        final int mco = this.in.readTTFUShort();
        // read base coverage offset
        final int bco = this.in.readTTFUShort();
        // read mark class count
        final int nmc = this.in.readTTFUShort();
        // read mark array offset
        final int mao = this.in.readTTFUShort();
        // read base array offset
        final int bao = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " mark-to-base positioning subtable format: "
                    + subtableFormat);
            log.debug(tableTag
                    + " mark-to-base positioning mark coverage table offset: "
                    + mco);
            log.debug(tableTag
                    + " mark-to-base positioning base coverage table offset: "
                    + bco);
            log.debug(tableTag + " mark-to-base positioning mark class count: "
                    + nmc);
            log.debug(tableTag
                    + " mark-to-base positioning mark array offset: " + mao);
            log.debug(tableTag
                    + " mark-to-base positioning base array offset: " + bao);
        }
        // read mark coverage table
        final GlyphCoverageTable mct = readCoverageTable(tableTag
                + " mark-to-base positioning mark coverage", subtableOffset
                + mco);
        // read base coverage table
        final GlyphCoverageTable bct = readCoverageTable(tableTag
                + " mark-to-base positioning base coverage", subtableOffset
                + bco);
        // read mark anchor array
        // seek to mark array
        this.in.seekSet(subtableOffset + mao);
        // read mark count
        final int nm = this.in.readTTFUShort();
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " mark-to-base positioning mark count: " + nm);
        }
        // read mark anchor array, where i:{0...markCount}
        final GlyphPositioningTable.MarkAnchor[] maa = new GlyphPositioningTable.MarkAnchor[nm];
        for (int i = 0; i < nm; i++) {
            // read mark class
            final int mc = this.in.readTTFUShort();
            // read mark anchor offset
            final int ao = this.in.readTTFUShort();
            GlyphPositioningTable.Anchor a;
            if (ao > 0) {
                a = readPosAnchor(subtableOffset + mao + ao);
            } else {
                a = null;
            }
            GlyphPositioningTable.MarkAnchor ma;
            if (a != null) {
                ma = new GlyphPositioningTable.MarkAnchor(mc, a);
            } else {
                ma = null;
            }
            maa[i] = ma;
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " mark-to-base positioning mark anchor["
                        + i + "]: " + ma);
            }

        }
        // read base anchor matrix
        // seek to base array
        this.in.seekSet(subtableOffset + bao);
        // read base count
        final int nb = this.in.readTTFUShort();
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " mark-to-base positioning base count: " + nb);
        }
        // read anchor matrix, where i:{0...baseCount - 1},
        // j:{0...markClassCount - 1}
        final GlyphPositioningTable.Anchor[][] bam = new GlyphPositioningTable.Anchor[nb][nmc];
        for (int i = 0; i < nb; i++) {
            for (int j = 0; j < nmc; j++) {
                // read base anchor offset
                final int ao = this.in.readTTFUShort();
                GlyphPositioningTable.Anchor a;
                if (ao > 0) {
                    a = readPosAnchor(subtableOffset + bao + ao);
                } else {
                    a = null;
                }
                bam[i][j] = a;
                if (log.isDebugEnabled()) {
                    log.debug(tableTag
                            + " mark-to-base positioning base anchor[" + i
                            + "][" + j + "]: " + a);
                }
            }
        }
        // store results
        this.seMapping = mct;
        this.seEntries.add(bct);
        this.seEntries.add(Integer.valueOf(nmc));
        this.seEntries.add(maa);
        this.seEntries.add(bam);
    }

    private int readMarkToBasePosTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read positioning subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readMarkToBasePosTableFormat1(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported mark-to-base positioning subtable format: "
                            + sf);
        }
        return sf;
    }

    private void readMarkToLigaturePosTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read mark coverage offset
        final int mco = this.in.readTTFUShort();
        // read ligature coverage offset
        final int lco = this.in.readTTFUShort();
        // read mark class count
        final int nmc = this.in.readTTFUShort();
        // read mark array offset
        final int mao = this.in.readTTFUShort();
        // read ligature array offset
        final int lao = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag
                    + " mark-to-ligature positioning subtable format: "
                    + subtableFormat);
            log.debug(tableTag
                    + " mark-to-ligature positioning mark coverage table offset: "
                    + mco);
            log.debug(tableTag
                    + " mark-to-ligature positioning ligature coverage table offset: "
                    + lco);
            log.debug(tableTag
                    + " mark-to-ligature positioning mark class count: " + nmc);
            log.debug(tableTag
                    + " mark-to-ligature positioning mark array offset: " + mao);
            log.debug(tableTag
                    + " mark-to-ligature positioning ligature array offset: "
                    + lao);
        }
        // read mark coverage table
        final GlyphCoverageTable mct = readCoverageTable(tableTag
                + " mark-to-ligature positioning mark coverage", subtableOffset
                + mco);
        // read ligature coverage table
        final GlyphCoverageTable lct = readCoverageTable(tableTag
                + " mark-to-ligature positioning ligature coverage",
                subtableOffset + lco);
        // read mark anchor array
        // seek to mark array
        this.in.seekSet(subtableOffset + mao);
        // read mark count
        final int nm = this.in.readTTFUShort();
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " mark-to-ligature positioning mark count: "
                    + nm);
        }
        // read mark anchor array, where i:{0...markCount}
        final GlyphPositioningTable.MarkAnchor[] maa = new GlyphPositioningTable.MarkAnchor[nm];
        for (int i = 0; i < nm; i++) {
            // read mark class
            final int mc = this.in.readTTFUShort();
            // read mark anchor offset
            final int ao = this.in.readTTFUShort();
            GlyphPositioningTable.Anchor a;
            if (ao > 0) {
                a = readPosAnchor(subtableOffset + mao + ao);
            } else {
                a = null;
            }
            GlyphPositioningTable.MarkAnchor ma;
            if (a != null) {
                ma = new GlyphPositioningTable.MarkAnchor(mc, a);
            } else {
                ma = null;
            }
            maa[i] = ma;
            if (log.isDebugEnabled()) {
                log.debug(tableTag
                        + " mark-to-ligature positioning mark anchor[" + i
                        + "]: " + ma);
            }
        }
        // read ligature anchor matrix
        // seek to ligature array
        this.in.seekSet(subtableOffset + lao);
        // read ligature count
        final int nl = this.in.readTTFUShort();
        if (log.isDebugEnabled()) {
            log.debug(tableTag
                    + " mark-to-ligature positioning ligature count: " + nl);
        }
        // read ligature attach table offsets
        final int[] laoa = new int[nl];
        for (int i = 0; i < nl; i++) {
            laoa[i] = this.in.readTTFUShort();
        }
        // iterate over ligature attach tables, recording maximum component
        // count
        int mxc = 0;
        for (int i = 0; i < nl; i++) {
            final int lato = laoa[i];
            this.in.seekSet(subtableOffset + lao + lato);
            // read component count
            final int cc = this.in.readTTFUShort();
            if (cc > mxc) {
                mxc = cc;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(tableTag
                    + " mark-to-ligature positioning maximum component count: "
                    + mxc);
        }
        // read anchor matrix, where i:{0...ligatureCount - 1},
        // j:{0...maxComponentCount - 1}, k:{0...markClassCount - 1}
        final GlyphPositioningTable.Anchor[][][] lam = new GlyphPositioningTable.Anchor[nl][][];
        for (int i = 0; i < nl; i++) {
            final int lato = laoa[i];
            // seek to ligature attach table for ligature[i]
            this.in.seekSet(subtableOffset + lao + lato);
            // read component count
            final int cc = this.in.readTTFUShort();
            final GlyphPositioningTable.Anchor[][] lcm = new GlyphPositioningTable.Anchor[cc][nmc];
            for (int j = 0; j < cc; j++) {
                for (int k = 0; k < nmc; k++) {
                    // read ligature anchor offset
                    final int ao = this.in.readTTFUShort();
                    GlyphPositioningTable.Anchor a;
                    if (ao > 0) {
                        a = readPosAnchor(subtableOffset + lao + lato + ao);
                    } else {
                        a = null;
                    }
                    lcm[j][k] = a;
                    if (log.isDebugEnabled()) {
                        log.debug(tableTag
                                + " mark-to-ligature positioning ligature anchor["
                                + i + "][" + j + "][" + k + "]: " + a);
                    }
                }
            }
            lam[i] = lcm;
        }
        // store results
        this.seMapping = mct;
        this.seEntries.add(lct);
        this.seEntries.add(Integer.valueOf(nmc));
        this.seEntries.add(Integer.valueOf(mxc));
        this.seEntries.add(maa);
        this.seEntries.add(lam);
    }

    private int readMarkToLigaturePosTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read positioning subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readMarkToLigaturePosTableFormat1(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported mark-to-ligature positioning subtable format: "
                            + sf);
        }
        return sf;
    }

    private void readMarkToMarkPosTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read mark #1 coverage offset
        final int m1co = this.in.readTTFUShort();
        // read mark #2 coverage offset
        final int m2co = this.in.readTTFUShort();
        // read mark class count
        final int nmc = this.in.readTTFUShort();
        // read mark #1 array offset
        final int m1ao = this.in.readTTFUShort();
        // read mark #2 array offset
        final int m2ao = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " mark-to-mark positioning subtable format: "
                    + subtableFormat);
            log.debug(tableTag
                    + " mark-to-mark positioning mark #1 coverage table offset: "
                    + m1co);
            log.debug(tableTag
                    + " mark-to-mark positioning mark #2 coverage table offset: "
                    + m2co);
            log.debug(tableTag + " mark-to-mark positioning mark class count: "
                    + nmc);
            log.debug(tableTag
                    + " mark-to-mark positioning mark #1 array offset: " + m1ao);
            log.debug(tableTag
                    + " mark-to-mark positioning mark #2 array offset: " + m2ao);
        }
        // read mark #1 coverage table
        final GlyphCoverageTable mct1 = readCoverageTable(tableTag
                + " mark-to-mark positioning mark #1 coverage", subtableOffset
                + m1co);
        // read mark #2 coverage table
        final GlyphCoverageTable mct2 = readCoverageTable(tableTag
                + " mark-to-mark positioning mark #2 coverage", subtableOffset
                + m2co);
        // read mark #1 anchor array
        // seek to mark array
        this.in.seekSet(subtableOffset + m1ao);
        // read mark count
        final int nm1 = this.in.readTTFUShort();
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " mark-to-mark positioning mark #1 count: "
                    + nm1);
        }
        // read mark anchor array, where i:{0...mark1Count}
        final GlyphPositioningTable.MarkAnchor[] maa = new GlyphPositioningTable.MarkAnchor[nm1];
        for (int i = 0; i < nm1; i++) {
            // read mark class
            final int mc = this.in.readTTFUShort();
            // read mark anchor offset
            final int ao = this.in.readTTFUShort();
            GlyphPositioningTable.Anchor a;
            if (ao > 0) {
                a = readPosAnchor(subtableOffset + m1ao + ao);
            } else {
                a = null;
            }
            GlyphPositioningTable.MarkAnchor ma;
            if (a != null) {
                ma = new GlyphPositioningTable.MarkAnchor(mc, a);
            } else {
                ma = null;
            }
            maa[i] = ma;
            if (log.isDebugEnabled()) {
                log.debug(tableTag
                        + " mark-to-mark positioning mark #1 anchor[" + i
                        + "]: " + ma);
            }
        }
        // read mark #2 anchor matrix
        // seek to mark #2 array
        this.in.seekSet(subtableOffset + m2ao);
        // read mark #2 count
        final int nm2 = this.in.readTTFUShort();
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " mark-to-mark positioning mark #2 count: "
                    + nm2);
        }
        // read anchor matrix, where i:{0...mark2Count - 1},
        // j:{0...markClassCount - 1}
        final GlyphPositioningTable.Anchor[][] mam = new GlyphPositioningTable.Anchor[nm2][nmc];
        for (int i = 0; i < nm2; i++) {
            for (int j = 0; j < nmc; j++) {
                // read mark anchor offset
                final int ao = this.in.readTTFUShort();
                GlyphPositioningTable.Anchor a;
                if (ao > 0) {
                    a = readPosAnchor(subtableOffset + m2ao + ao);
                } else {
                    a = null;
                }
                mam[i][j] = a;
                if (log.isDebugEnabled()) {
                    log.debug(tableTag
                            + " mark-to-mark positioning mark #2 anchor[" + i
                            + "][" + j + "]: " + a);
                }
            }
        }
        // store results
        this.seMapping = mct1;
        this.seEntries.add(mct2);
        this.seEntries.add(Integer.valueOf(nmc));
        this.seEntries.add(maa);
        this.seEntries.add(mam);
    }

    private int readMarkToMarkPosTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read positioning subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readMarkToMarkPosTableFormat1(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported mark-to-mark positioning subtable format: "
                            + sf);
        }
        return sf;
    }

    private void readContextualPosTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read rule set count
        final int nrs = this.in.readTTFUShort();
        // read rule set offsets
        final int[] rsoa = new int[nrs];
        for (int i = 0; i < nrs; i++) {
            rsoa[i] = this.in.readTTFUShort();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " contextual positioning subtable format: "
                    + subtableFormat + " (glyphs)");
            log.debug(tableTag
                    + " contextual positioning coverage table offset: " + co);
            log.debug(tableTag + " contextual positioning rule set count: "
                    + nrs);
            for (int i = 0; i < nrs; i++) {
                log.debug(tableTag + " contextual positioning rule set offset["
                        + i + "]: " + rsoa[i]);
            }
        }
        // read coverage table
        GlyphCoverageTable ct;
        if (co > 0) {
            ct = readCoverageTable(tableTag
                    + " contextual positioning coverage", subtableOffset + co);
        } else {
            ct = null;
        }
        // read rule sets
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[nrs];
        String header = null;
        for (int i = 0; i < nrs; i++) {
            GlyphTable.RuleSet rs;
            final int rso = rsoa[i];
            if (rso > 0) {
                // seek to rule set [ i ]
                this.in.seekSet(subtableOffset + rso);
                // read rule count
                final int nr = this.in.readTTFUShort();
                // read rule offsets
                final int[] roa = new int[nr];
                final GlyphTable.Rule[] ra = new GlyphTable.Rule[nr];
                for (int j = 0; j < nr; j++) {
                    roa[j] = this.in.readTTFUShort();
                }
                // read glyph sequence rules
                for (int j = 0; j < nr; j++) {
                    GlyphTable.GlyphSequenceRule r;
                    final int ro = roa[j];
                    if (ro > 0) {
                        // seek to rule [ j ]
                        this.in.seekSet(subtableOffset + rso + ro);
                        // read glyph count
                        final int ng = this.in.readTTFUShort();
                        // read rule lookup count
                        final int nl = this.in.readTTFUShort();
                        // read glyphs
                        final int[] glyphs = new int[ng - 1];
                        for (int k = 0, nk = glyphs.length; k < nk; k++) {
                            glyphs[k] = this.in.readTTFUShort();
                        }
                        // read rule lookups
                        if (log.isDebugEnabled()) {
                            header = tableTag
                                    + " contextual positioning lookups @rule["
                                    + i + "][" + j + "]: ";
                        }
                        final GlyphTable.RuleLookup[] lookups = readRuleLookups(
                                nl, header);
                        r = new GlyphTable.GlyphSequenceRule(lookups, ng,
                                glyphs);
                    } else {
                        r = null;
                    }
                    ra[j] = r;
                }
                rs = new GlyphTable.HomogeneousRuleSet(ra);
            } else {
                rs = null;
            }
            rsa[i] = rs;
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(rsa);
    }

    private void readContextualPosTableFormat2(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read class def table offset
        final int cdo = this.in.readTTFUShort();
        // read class rule set count
        final int ngc = this.in.readTTFUShort();
        // read class rule set offsets
        final int[] csoa = new int[ngc];
        for (int i = 0; i < ngc; i++) {
            csoa[i] = this.in.readTTFUShort();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " contextual positioning subtable format: "
                    + subtableFormat + " (glyph classes)");
            log.debug(tableTag
                    + " contextual positioning coverage table offset: " + co);
            log.debug(tableTag + " contextual positioning class set count: "
                    + ngc);
            for (int i = 0; i < ngc; i++) {
                log.debug(tableTag
                        + " contextual positioning class set offset[" + i
                        + "]: " + csoa[i]);
            }
        }
        // read coverage table
        GlyphCoverageTable ct;
        if (co > 0) {
            ct = readCoverageTable(tableTag
                    + " contextual positioning coverage", subtableOffset + co);
        } else {
            ct = null;
        }
        // read class definition table
        GlyphClassTable cdt;
        if (cdo > 0) {
            cdt = readClassDefTable(tableTag
                    + " contextual positioning class definition",
                    subtableOffset + cdo);
        } else {
            cdt = null;
        }
        // read rule sets
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[ngc];
        String header = null;
        for (int i = 0; i < ngc; i++) {
            final int cso = csoa[i];
            GlyphTable.RuleSet rs;
            if (cso > 0) {
                // seek to rule set [ i ]
                this.in.seekSet(subtableOffset + cso);
                // read rule count
                final int nr = this.in.readTTFUShort();
                // read rule offsets
                final int[] roa = new int[nr];
                final GlyphTable.Rule[] ra = new GlyphTable.Rule[nr];
                for (int j = 0; j < nr; j++) {
                    roa[j] = this.in.readTTFUShort();
                }
                // read glyph sequence rules
                for (int j = 0; j < nr; j++) {
                    final int ro = roa[j];
                    GlyphTable.ClassSequenceRule r;
                    if (ro > 0) {
                        // seek to rule [ j ]
                        this.in.seekSet(subtableOffset + cso + ro);
                        // read glyph count
                        final int ng = this.in.readTTFUShort();
                        // read rule lookup count
                        final int nl = this.in.readTTFUShort();
                        // read classes
                        final int[] classes = new int[ng - 1];
                        for (int k = 0, nk = classes.length; k < nk; k++) {
                            classes[k] = this.in.readTTFUShort();
                        }
                        // read rule lookups
                        if (log.isDebugEnabled()) {
                            header = tableTag
                                    + " contextual positioning lookups @rule["
                                    + i + "][" + j + "]: ";
                        }
                        final GlyphTable.RuleLookup[] lookups = readRuleLookups(
                                nl, header);
                        r = new GlyphTable.ClassSequenceRule(lookups, ng,
                                classes);
                    } else {
                        r = null;
                    }
                    ra[j] = r;
                }
                rs = new GlyphTable.HomogeneousRuleSet(ra);
            } else {
                rs = null;
            }
            rsa[i] = rs;
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(cdt);
        this.seEntries.add(Integer.valueOf(ngc));
        this.seEntries.add(rsa);
    }

    private void readContextualPosTableFormat3(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read glyph (input sequence length) count
        final int ng = this.in.readTTFUShort();
        // read positioning lookup count
        final int nl = this.in.readTTFUShort();
        // read glyph coverage offsets, one per glyph input sequence length
        // count
        final int[] gcoa = new int[ng];
        for (int i = 0; i < ng; i++) {
            gcoa[i] = this.in.readTTFUShort();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " contextual positioning subtable format: "
                    + subtableFormat + " (glyph sets)");
            log.debug(tableTag
                    + " contextual positioning glyph input sequence length count: "
                    + ng);
            log.debug(tableTag + " contextual positioning lookup count: " + nl);
            for (int i = 0; i < ng; i++) {
                log.debug(tableTag
                        + " contextual positioning coverage table offset[" + i
                        + "]: " + gcoa[i]);
            }
        }
        // read coverage tables
        final GlyphCoverageTable[] gca = new GlyphCoverageTable[ng];
        for (int i = 0; i < ng; i++) {
            final int gco = gcoa[i];
            GlyphCoverageTable gct;
            if (gco > 0) {
                gct = readCoverageTable(tableTag
                        + " contextual positioning coverage[" + i + "]",
                        subtableOffset + gcoa[i]);
            } else {
                gct = null;
            }
            gca[i] = gct;
        }
        // read rule lookups
        String header = null;
        if (log.isDebugEnabled()) {
            header = tableTag + " contextual positioning lookups: ";
        }
        final GlyphTable.RuleLookup[] lookups = readRuleLookups(nl, header);
        // construct rule, rule set, and rule set array
        final GlyphTable.Rule r = new GlyphTable.CoverageSequenceRule(lookups,
                ng, gca);
        final GlyphTable.RuleSet rs = new GlyphTable.HomogeneousRuleSet(
                new GlyphTable.Rule[] { r });
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[] { rs };
        // store results
        assert gca != null && gca.length > 0;
        this.seMapping = gca[0];
        this.seEntries.add(rsa);
    }

    private int readContextualPosTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read positioning subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readContextualPosTableFormat1(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else if (sf == 2) {
            readContextualPosTableFormat2(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else if (sf == 3) {
            readContextualPosTableFormat3(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported contextual positioning subtable format: " + sf);
        }
        return sf;
    }

    private void readChainedContextualPosTableFormat1(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read rule set count
        final int nrs = this.in.readTTFUShort();
        // read rule set offsets
        final int[] rsoa = new int[nrs];
        for (int i = 0; i < nrs; i++) {
            rsoa[i] = this.in.readTTFUShort();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag
                    + " chained contextual positioning subtable format: "
                    + subtableFormat + " (glyphs)");
            log.debug(tableTag
                    + " chained contextual positioning coverage table offset: "
                    + co);
            log.debug(tableTag
                    + " chained contextual positioning rule set count: " + nrs);
            for (int i = 0; i < nrs; i++) {
                log.debug(tableTag
                        + " chained contextual positioning rule set offset["
                        + i + "]: " + rsoa[i]);
            }
        }
        // read coverage table
        GlyphCoverageTable ct;
        if (co > 0) {
            ct = readCoverageTable(tableTag
                    + " chained contextual positioning coverage",
                    subtableOffset + co);
        } else {
            ct = null;
        }
        // read rule sets
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[nrs];
        String header = null;
        for (int i = 0; i < nrs; i++) {
            GlyphTable.RuleSet rs;
            final int rso = rsoa[i];
            if (rso > 0) {
                // seek to rule set [ i ]
                this.in.seekSet(subtableOffset + rso);
                // read rule count
                final int nr = this.in.readTTFUShort();
                // read rule offsets
                final int[] roa = new int[nr];
                final GlyphTable.Rule[] ra = new GlyphTable.Rule[nr];
                for (int j = 0; j < nr; j++) {
                    roa[j] = this.in.readTTFUShort();
                }
                // read glyph sequence rules
                for (int j = 0; j < nr; j++) {
                    GlyphTable.ChainedGlyphSequenceRule r;
                    final int ro = roa[j];
                    if (ro > 0) {
                        // seek to rule [ j ]
                        this.in.seekSet(subtableOffset + rso + ro);
                        // read backtrack glyph count
                        final int nbg = this.in.readTTFUShort();
                        // read backtrack glyphs
                        final int[] backtrackGlyphs = new int[nbg];
                        for (int k = 0, nk = backtrackGlyphs.length; k < nk; k++) {
                            backtrackGlyphs[k] = this.in.readTTFUShort();
                        }
                        // read input glyph count
                        final int nig = this.in.readTTFUShort();
                        // read glyphs
                        final int[] glyphs = new int[nig - 1];
                        for (int k = 0, nk = glyphs.length; k < nk; k++) {
                            glyphs[k] = this.in.readTTFUShort();
                        }
                        // read lookahead glyph count
                        final int nlg = this.in.readTTFUShort();
                        // read lookahead glyphs
                        final int[] lookaheadGlyphs = new int[nlg];
                        for (int k = 0, nk = lookaheadGlyphs.length; k < nk; k++) {
                            lookaheadGlyphs[k] = this.in.readTTFUShort();
                        }
                        // read rule lookup count
                        final int nl = this.in.readTTFUShort();
                        // read rule lookups
                        if (log.isDebugEnabled()) {
                            header = tableTag
                                    + " contextual positioning lookups @rule["
                                    + i + "][" + j + "]: ";
                        }
                        final GlyphTable.RuleLookup[] lookups = readRuleLookups(
                                nl, header);
                        r = new GlyphTable.ChainedGlyphSequenceRule(lookups,
                                nig, glyphs, backtrackGlyphs, lookaheadGlyphs);
                    } else {
                        r = null;
                    }
                    ra[j] = r;
                }
                rs = new GlyphTable.HomogeneousRuleSet(ra);
            } else {
                rs = null;
            }
            rsa[i] = rs;
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(rsa);
    }

    private void readChainedContextualPosTableFormat2(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read backtrack class def table offset
        final int bcdo = this.in.readTTFUShort();
        // read input class def table offset
        final int icdo = this.in.readTTFUShort();
        // read lookahead class def table offset
        final int lcdo = this.in.readTTFUShort();
        // read class set count
        final int ngc = this.in.readTTFUShort();
        // read class set offsets
        final int[] csoa = new int[ngc];
        for (int i = 0; i < ngc; i++) {
            csoa[i] = this.in.readTTFUShort();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag
                    + " chained contextual positioning subtable format: "
                    + subtableFormat + " (glyph classes)");
            log.debug(tableTag
                    + " chained contextual positioning coverage table offset: "
                    + co);
            log.debug(tableTag
                    + " chained contextual positioning class set count: " + ngc);
            for (int i = 0; i < ngc; i++) {
                log.debug(tableTag
                        + " chained contextual positioning class set offset["
                        + i + "]: " + csoa[i]);
            }
        }
        // read coverage table
        GlyphCoverageTable ct;
        if (co > 0) {
            ct = readCoverageTable(tableTag
                    + " chained contextual positioning coverage",
                    subtableOffset + co);
        } else {
            ct = null;
        }
        // read backtrack class definition table
        GlyphClassTable bcdt;
        if (bcdo > 0) {
            bcdt = readClassDefTable(tableTag
                    + " contextual positioning backtrack class definition",
                    subtableOffset + bcdo);
        } else {
            bcdt = null;
        }
        // read input class definition table
        GlyphClassTable icdt;
        if (icdo > 0) {
            icdt = readClassDefTable(tableTag
                    + " contextual positioning input class definition",
                    subtableOffset + icdo);
        } else {
            icdt = null;
        }
        // read lookahead class definition table
        GlyphClassTable lcdt;
        if (lcdo > 0) {
            lcdt = readClassDefTable(tableTag
                    + " contextual positioning lookahead class definition",
                    subtableOffset + lcdo);
        } else {
            lcdt = null;
        }
        // read rule sets
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[ngc];
        String header = null;
        for (int i = 0; i < ngc; i++) {
            final int cso = csoa[i];
            GlyphTable.RuleSet rs;
            if (cso > 0) {
                // seek to rule set [ i ]
                this.in.seekSet(subtableOffset + cso);
                // read rule count
                final int nr = this.in.readTTFUShort();
                // read rule offsets
                final int[] roa = new int[nr];
                final GlyphTable.Rule[] ra = new GlyphTable.Rule[nr];
                for (int j = 0; j < nr; j++) {
                    roa[j] = this.in.readTTFUShort();
                }
                // read glyph sequence rules
                for (int j = 0; j < nr; j++) {
                    GlyphTable.ChainedClassSequenceRule r;
                    final int ro = roa[j];
                    if (ro > 0) {
                        // seek to rule [ j ]
                        this.in.seekSet(subtableOffset + cso + ro);
                        // read backtrack glyph class count
                        final int nbc = this.in.readTTFUShort();
                        // read backtrack glyph classes
                        final int[] backtrackClasses = new int[nbc];
                        for (int k = 0, nk = backtrackClasses.length; k < nk; k++) {
                            backtrackClasses[k] = this.in.readTTFUShort();
                        }
                        // read input glyph class count
                        final int nic = this.in.readTTFUShort();
                        // read input glyph classes
                        final int[] classes = new int[nic - 1];
                        for (int k = 0, nk = classes.length; k < nk; k++) {
                            classes[k] = this.in.readTTFUShort();
                        }
                        // read lookahead glyph class count
                        final int nlc = this.in.readTTFUShort();
                        // read lookahead glyph classes
                        final int[] lookaheadClasses = new int[nlc];
                        for (int k = 0, nk = lookaheadClasses.length; k < nk; k++) {
                            lookaheadClasses[k] = this.in.readTTFUShort();
                        }
                        // read rule lookup count
                        final int nl = this.in.readTTFUShort();
                        // read rule lookups
                        if (log.isDebugEnabled()) {
                            header = tableTag
                                    + " contextual positioning lookups @rule["
                                    + i + "][" + j + "]: ";
                        }
                        final GlyphTable.RuleLookup[] lookups = readRuleLookups(
                                nl, header);
                        r = new GlyphTable.ChainedClassSequenceRule(lookups,
                                nic, classes, backtrackClasses,
                                lookaheadClasses);
                    } else {
                        r = null;
                    }
                    ra[j] = r;
                }
                rs = new GlyphTable.HomogeneousRuleSet(ra);
            } else {
                rs = null;
            }
            rsa[i] = rs;
        }
        // store results
        this.seMapping = ct;
        this.seEntries.add(icdt);
        this.seEntries.add(bcdt);
        this.seEntries.add(lcdt);
        this.seEntries.add(Integer.valueOf(ngc));
        this.seEntries.add(rsa);
    }

    private void readChainedContextualPosTableFormat3(final int lookupType,
            final int lookupFlags, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read backtrack glyph count
        final int nbg = this.in.readTTFUShort();
        // read backtrack glyph coverage offsets
        final int[] bgcoa = new int[nbg];
        for (int i = 0; i < nbg; i++) {
            bgcoa[i] = this.in.readTTFUShort();
        }
        // read input glyph count
        final int nig = this.in.readTTFUShort();
        // read backtrack glyph coverage offsets
        final int[] igcoa = new int[nig];
        for (int i = 0; i < nig; i++) {
            igcoa[i] = this.in.readTTFUShort();
        }
        // read lookahead glyph count
        final int nlg = this.in.readTTFUShort();
        // read backtrack glyph coverage offsets
        final int[] lgcoa = new int[nlg];
        for (int i = 0; i < nlg; i++) {
            lgcoa[i] = this.in.readTTFUShort();
        }
        // read positioning lookup count
        final int nl = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag
                    + " chained contextual positioning subtable format: "
                    + subtableFormat + " (glyph sets)");
            log.debug(tableTag
                    + " chained contextual positioning backtrack glyph count: "
                    + nbg);
            for (int i = 0; i < nbg; i++) {
                log.debug(tableTag
                        + " chained contextual positioning backtrack coverage table offset["
                        + i + "]: " + bgcoa[i]);
            }
            log.debug(tableTag
                    + " chained contextual positioning input glyph count: "
                    + nig);
            for (int i = 0; i < nig; i++) {
                log.debug(tableTag
                        + " chained contextual positioning input coverage table offset["
                        + i + "]: " + igcoa[i]);
            }
            log.debug(tableTag
                    + " chained contextual positioning lookahead glyph count: "
                    + nlg);
            for (int i = 0; i < nlg; i++) {
                log.debug(tableTag
                        + " chained contextual positioning lookahead coverage table offset["
                        + i + "]: " + lgcoa[i]);
            }
            log.debug(tableTag
                    + " chained contextual positioning lookup count: " + nl);
        }
        // read backtrack coverage tables
        final GlyphCoverageTable[] bgca = new GlyphCoverageTable[nbg];
        for (int i = 0; i < nbg; i++) {
            final int bgco = bgcoa[i];
            GlyphCoverageTable bgct;
            if (bgco > 0) {
                bgct = readCoverageTable(tableTag
                        + " chained contextual positioning backtrack coverage["
                        + i + "]", subtableOffset + bgco);
            } else {
                bgct = null;
            }
            bgca[i] = bgct;
        }
        // read input coverage tables
        final GlyphCoverageTable[] igca = new GlyphCoverageTable[nig];
        for (int i = 0; i < nig; i++) {
            final int igco = igcoa[i];
            GlyphCoverageTable igct;
            if (igco > 0) {
                igct = readCoverageTable(tableTag
                        + " chained contextual positioning input coverage[" + i
                        + "]", subtableOffset + igco);
            } else {
                igct = null;
            }
            igca[i] = igct;
        }
        // read lookahead coverage tables
        final GlyphCoverageTable[] lgca = new GlyphCoverageTable[nlg];
        for (int i = 0; i < nlg; i++) {
            final int lgco = lgcoa[i];
            GlyphCoverageTable lgct;
            if (lgco > 0) {
                lgct = readCoverageTable(tableTag
                        + " chained contextual positioning lookahead coverage["
                        + i + "]", subtableOffset + lgco);
            } else {
                lgct = null;
            }
            lgca[i] = lgct;
        }
        // read rule lookups
        String header = null;
        if (log.isDebugEnabled()) {
            header = tableTag + " chained contextual positioning lookups: ";
        }
        final GlyphTable.RuleLookup[] lookups = readRuleLookups(nl, header);
        // construct rule, rule set, and rule set array
        final GlyphTable.Rule r = new GlyphTable.ChainedCoverageSequenceRule(
                lookups, nig, igca, bgca, lgca);
        final GlyphTable.RuleSet rs = new GlyphTable.HomogeneousRuleSet(
                new GlyphTable.Rule[] { r });
        final GlyphTable.RuleSet[] rsa = new GlyphTable.RuleSet[] { rs };
        // store results
        assert igca != null && igca.length > 0;
        this.seMapping = igca[0];
        this.seEntries.add(rsa);
    }

    private int readChainedContextualPosTable(final int lookupType,
            final int lookupFlags, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read positioning subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readChainedContextualPosTableFormat1(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else if (sf == 2) {
            readChainedContextualPosTableFormat2(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else if (sf == 3) {
            readChainedContextualPosTableFormat3(lookupType, lookupFlags,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported chained contextual positioning subtable format: "
                            + sf);
        }
        return sf;
    }

    private void readExtensionPosTableFormat1(final int lookupType,
            final int lookupFlags, final int lookupSequence,
            final int subtableSequence, final long subtableOffset,
            final int subtableFormat) throws IOException {
        final String tableTag = "GPOS";
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read extension lookup type
        final int lt = this.in.readTTFUShort();
        // read extension offset
        final long eo = this.in.readTTFULong();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " extension positioning subtable format: "
                    + subtableFormat);
            log.debug(tableTag + " extension positioning lookup type: " + lt);
            log.debug(tableTag + " extension positioning lookup table offset: "
                    + eo);
        }
        // read referenced subtable from extended offset
        readGPOSSubtable(lt, lookupFlags, lookupSequence, subtableSequence,
                subtableOffset + eo);
    }

    private int readExtensionPosTable(final int lookupType,
            final int lookupFlags, final int lookupSequence,
            final int subtableSequence, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read positioning subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readExtensionPosTableFormat1(lookupType, lookupFlags,
                    lookupSequence, subtableSequence, subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported extension positioning subtable format: " + sf);
        }
        return sf;
    }

    private void readGPOSSubtable(final int lookupType, final int lookupFlags,
            final int lookupSequence, final int subtableSequence,
            final long subtableOffset) throws IOException {
        initATSubState();
        int subtableFormat = -1;
        switch (lookupType) {
        case GPOSLookupType.SINGLE:
            subtableFormat = readSinglePosTable(lookupType, lookupFlags,
                    subtableOffset);
            break;
        case GPOSLookupType.PAIR:
            subtableFormat = readPairPosTable(lookupType, lookupFlags,
                    subtableOffset);
            break;
        case GPOSLookupType.CURSIVE:
            subtableFormat = readCursivePosTable(lookupType, lookupFlags,
                    subtableOffset);
            break;
        case GPOSLookupType.MARK_TO_BASE:
            subtableFormat = readMarkToBasePosTable(lookupType, lookupFlags,
                    subtableOffset);
            break;
        case GPOSLookupType.MARK_TO_LIGATURE:
            subtableFormat = readMarkToLigaturePosTable(lookupType,
                    lookupFlags, subtableOffset);
            break;
        case GPOSLookupType.MARK_TO_MARK:
            subtableFormat = readMarkToMarkPosTable(lookupType, lookupFlags,
                    subtableOffset);
            break;
        case GPOSLookupType.CONTEXTUAL:
            subtableFormat = readContextualPosTable(lookupType, lookupFlags,
                    subtableOffset);
            break;
        case GPOSLookupType.CHAINED_CONTEXTUAL:
            subtableFormat = readChainedContextualPosTable(lookupType,
                    lookupFlags, subtableOffset);
            break;
        case GPOSLookupType.EXTENSION:
            subtableFormat = readExtensionPosTable(lookupType, lookupFlags,
                    lookupSequence, subtableSequence, subtableOffset);
            break;
        default:
            break;
        }
        extractSESubState(GlyphTable.GLYPH_TABLE_TYPE_POSITIONING, lookupType,
                lookupFlags, lookupSequence, subtableSequence, subtableFormat);
        resetATSubState();
    }

    private void readLookupTable(final TTFTableName tableTag,
            final int lookupSequence, final long lookupTable)
                    throws IOException {
        final boolean isGSUB = tableTag.equals(TTFTableName.GSUB);
        final boolean isGPOS = tableTag.equals(TTFTableName.GPOS);
        this.in.seekSet(lookupTable);
        // read lookup type
        final int lt = this.in.readTTFUShort();
        // read lookup flags
        final int lf = this.in.readTTFUShort();
        // read sub-table count
        final int ns = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            String lts;
            if (isGSUB) {
                lts = GSUBLookupType.toString(lt);
            } else if (isGPOS) {
                lts = GPOSLookupType.toString(lt);
            } else {
                lts = "?";
            }
            log.debug(tableTag + " lookup table type: " + lt + " (" + lts + ")");
            log.debug(tableTag + " lookup table flags: " + lf + " ("
                    + LookupFlag.toString(lf) + ")");
            log.debug(tableTag + " lookup table subtable count: " + ns);
        }
        // read subtable offsets
        final int[] soa = new int[ns];
        for (int i = 0; i < ns; i++) {
            final int so = this.in.readTTFUShort();
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " lookup table subtable offset: " + so);
            }
            soa[i] = so;
        }
        // read mark filtering set
        if ((lf & LookupFlag.USE_MARK_FILTERING_SET) != 0) {
            // read mark filtering set
            final int fs = this.in.readTTFUShort();
            // dump info if debugging
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " lookup table mark filter set: " + fs);
            }
        }
        // read subtables
        for (int i = 0; i < ns; i++) {
            final int so = soa[i];
            if (isGSUB) {
                readGSUBSubtable(lt, lf, lookupSequence, i, lookupTable + so);
            } else if (isGPOS) {
                readGPOSSubtable(lt, lf, lookupSequence, i, lookupTable + so);
            }
        }
    }

    private void readLookupList(final TTFTableName tableTag,
            final long lookupList) throws IOException {
        this.in.seekSet(lookupList);
        // read lookup record count
        final int nl = this.in.readTTFUShort();
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " lookup list record count: " + nl);
        }
        if (nl > 0) {
            final int[] loa = new int[nl];
            // read lookup records
            for (int i = 0, n = nl; i < n; i++) {
                final int lo = this.in.readTTFUShort();
                if (log.isDebugEnabled()) {
                    log.debug(tableTag + " lookup table offset: " + lo);
                }
                loa[i] = lo;
            }
            // read lookup tables
            for (int i = 0, n = nl; i < n; i++) {
                if (log.isDebugEnabled()) {
                    log.debug(tableTag + " lookup index: " + i);
                }
                readLookupTable(tableTag, i, lookupList + loa[i]);
            }
        }
    }

    /**
     * Read the common layout tables (used by GSUB and GPOS).
     *
     * @param tableTag
     *            tag of table being read
     * @param scriptList
     *            offset to script list from beginning of font file
     * @param featureList
     *            offset to feature list from beginning of font file
     * @param lookupList
     *            offset to lookup list from beginning of font file
     * @throws IOException
     *             In case of a I/O problem
     */
    private void readCommonLayoutTables(final TTFTableName tableTag,
            final long scriptList, final long featureList, final long lookupList)
                    throws IOException {
        if (scriptList > 0) {
            readScriptList(tableTag, scriptList);
        }
        if (featureList > 0) {
            readFeatureList(tableTag, featureList);
        }
        if (lookupList > 0) {
            readLookupList(tableTag, lookupList);
        }
    }

    private void readGDEFClassDefTable(final TTFTableName tableTag,
            final int lookupSequence, final long subtableOffset)
                    throws IOException {
        initATSubState();
        this.in.seekSet(subtableOffset);
        // subtable is a bare class definition table
        final GlyphClassTable ct = readClassDefTable(tableTag
                + " glyph class definition table", subtableOffset);
        // store results
        this.seMapping = ct;
        // extract subtable
        extractSESubState(GlyphTable.GLYPH_TABLE_TYPE_DEFINITION,
                GDEFLookupType.GLYPH_CLASS, 0, lookupSequence, 0, 1);
        resetATSubState();
    }

    private void readGDEFAttachmentTable(final TTFTableName tableTag,
            final int lookupSequence, final long subtableOffset)
                    throws IOException {
        initATSubState();
        this.in.seekSet(subtableOffset);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " attachment point coverage table offset: "
                    + co);
        }
        // read coverage table
        final GlyphCoverageTable ct = readCoverageTable(tableTag
                + " attachment point coverage", subtableOffset + co);
        // store results
        this.seMapping = ct;
        // extract subtable
        extractSESubState(GlyphTable.GLYPH_TABLE_TYPE_DEFINITION,
                GDEFLookupType.ATTACHMENT_POINT, 0, lookupSequence, 0, 1);
        resetATSubState();
    }

    private void readGDEFLigatureCaretTable(final TTFTableName tableTag,
            final int lookupSequence, final long subtableOffset)
                    throws IOException {
        initATSubState();
        this.in.seekSet(subtableOffset);
        // read coverage offset
        final int co = this.in.readTTFUShort();
        // read ligature glyph count
        final int nl = this.in.readTTFUShort();
        // read ligature glyph table offsets
        final int[] lgto = new int[nl];
        for (int i = 0; i < nl; i++) {
            lgto[i] = this.in.readTTFUShort();
        }

        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " ligature caret coverage table offset: " + co);
            log.debug(tableTag + " ligature caret ligature glyph count: " + nl);
            for (int i = 0; i < nl; i++) {
                log.debug(tableTag + " ligature glyph table offset[" + i
                        + "]: " + lgto[i]);
            }
        }
        // read coverage table
        final GlyphCoverageTable ct = readCoverageTable(tableTag
                + " ligature caret coverage", subtableOffset + co);
        // store results
        this.seMapping = ct;
        // extract subtable
        extractSESubState(GlyphTable.GLYPH_TABLE_TYPE_DEFINITION,
                GDEFLookupType.LIGATURE_CARET, 0, lookupSequence, 0, 1);
        resetATSubState();
    }

    private void readGDEFMarkAttachmentTable(final TTFTableName tableTag,
            final int lookupSequence, final long subtableOffset)
                    throws IOException {
        initATSubState();
        this.in.seekSet(subtableOffset);
        // subtable is a bare class definition table
        final GlyphClassTable ct = readClassDefTable(tableTag
                + " glyph class definition table", subtableOffset);
        // store results
        this.seMapping = ct;
        // extract subtable
        extractSESubState(GlyphTable.GLYPH_TABLE_TYPE_DEFINITION,
                GDEFLookupType.MARK_ATTACHMENT, 0, lookupSequence, 0, 1);
        resetATSubState();
    }

    private void readGDEFMarkGlyphsTableFormat1(final TTFTableName tableTag,
            final int lookupSequence, final long subtableOffset,
            final int subtableFormat) throws IOException {
        initATSubState();
        this.in.seekSet(subtableOffset);
        // skip over format (already known)
        this.in.skip(2);
        // read mark set class count
        final int nmc = this.in.readTTFUShort();
        final long[] mso = new long[nmc];
        // read mark set coverage offsets
        for (int i = 0; i < nmc; i++) {
            mso[i] = this.in.readTTFULong();
        }
        // dump info if debugging
        if (log.isDebugEnabled()) {
            log.debug(tableTag + " mark set subtable format: " + subtableFormat
                    + " (glyph sets)");
            log.debug(tableTag + " mark set class count: " + nmc);
            for (int i = 0; i < nmc; i++) {
                log.debug(tableTag + " mark set coverage table offset[" + i
                        + "]: " + mso[i]);
            }
        }
        // read mark set coverage tables, one per class
        final GlyphCoverageTable[] msca = new GlyphCoverageTable[nmc];
        for (int i = 0; i < nmc; i++) {
            msca[i] = readCoverageTable(tableTag + " mark set coverage[" + i
                    + "]", subtableOffset + mso[i]);
        }
        // create combined class table from per-class coverage tables
        final GlyphClassTable ct = GlyphClassTable.createClassTable(Arrays
                .asList(msca));
        // store results
        this.seMapping = ct;
        // extract subtable
        extractSESubState(GlyphTable.GLYPH_TABLE_TYPE_DEFINITION,
                GDEFLookupType.MARK_ATTACHMENT, 0, lookupSequence, 0, 1);
        resetATSubState();
    }

    private void readGDEFMarkGlyphsTable(final TTFTableName tableTag,
            final int lookupSequence, final long subtableOffset)
                    throws IOException {
        this.in.seekSet(subtableOffset);
        // read mark set subtable format
        final int sf = this.in.readTTFUShort();
        if (sf == 1) {
            readGDEFMarkGlyphsTableFormat1(tableTag, lookupSequence,
                    subtableOffset, sf);
        } else {
            throw new AdvancedTypographicTableFormatException(
                    "unsupported mark glyph sets subtable format: " + sf);
        }
    }

    /**
     * Read the GDEF table.
     *
     * @throws IOException
     *             In case of a I/O problem
     */
    private void readGDEF() throws IOException {
        final TTFTableName tableTag = TTFTableName.GDEF;
        // Initialize temporary state
        initATState();
        // Read glyph definition (GDEF) table
        final TTFDirTabEntry dirTab = this.ttf.getDirectoryEntry(tableTag);
        if (this.gdef != null) {
            if (log.isDebugEnabled()) {
                log.debug(tableTag + ": ignoring duplicate table");
            }
        } else if (dirTab != null) {
            this.ttf.seekTab(this.in, tableTag, 0);
            final long version = this.in.readTTFULong();
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " version: " + version / 65536 + "."
                        + version % 65536);
            }
            // glyph class definition table offset (may be null)
            final int cdo = this.in.readTTFUShort();
            // attach point list offset (may be null)
            final int apo = this.in.readTTFUShort();
            // ligature caret list offset (may be null)
            final int lco = this.in.readTTFUShort();
            // mark attach class definition table offset (may be null)
            final int mao = this.in.readTTFUShort();
            // mark glyph sets definition table offset (may be null)
            int mgo;
            if (version >= 0x00010002) {
                mgo = this.in.readTTFUShort();
            } else {
                mgo = 0;
            }
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " glyph class definition table offset: "
                        + cdo);
                log.debug(tableTag + " attachment point list offset: " + apo);
                log.debug(tableTag + " ligature caret list offset: " + lco);
                log.debug(tableTag
                        + " mark attachment class definition table offset: "
                        + mao);
                log.debug(tableTag
                        + " mark glyph set definitions table offset: " + mgo);
            }
            // initialize subtable sequence number
            int seqno = 0;
            // obtain offset to start of gdef table
            final long to = dirTab.getOffset();
            // (optionally) read glyph class definition subtable
            if (cdo != 0) {
                readGDEFClassDefTable(tableTag, seqno++, to + cdo);
            }
            // (optionally) read glyph attachment point subtable
            if (apo != 0) {
                readGDEFAttachmentTable(tableTag, seqno++, to + apo);
            }
            // (optionally) read ligature caret subtable
            if (lco != 0) {
                readGDEFLigatureCaretTable(tableTag, seqno++, to + lco);
            }
            // (optionally) read mark attachment class subtable
            if (mao != 0) {
                readGDEFMarkAttachmentTable(tableTag, seqno++, to + mao);
            }
            // (optionally) read mark glyph sets subtable
            if (mgo != 0) {
                readGDEFMarkGlyphsTable(tableTag, seqno++, to + mgo);
            }
            GlyphDefinitionTable gdef;
            if ((gdef = constructGDEF()) != null) {
                this.gdef = gdef;
            }
        }
    }

    /**
     * Read the GSUB table.
     *
     * @throws IOException
     *             In case of a I/O problem
     */
    private void readGSUB() throws IOException {
        final TTFTableName tableTag = TTFTableName.GSUB;
        // Initialize temporary state
        initATState();
        // Read glyph substitution (GSUB) table
        final TTFDirTabEntry dirTab = this.ttf.getDirectoryEntry(tableTag);
        if (this.gpos != null) {
            if (log.isDebugEnabled()) {
                log.debug(tableTag + ": ignoring duplicate table");
            }
        } else if (dirTab != null) {
            this.ttf.seekTab(this.in, tableTag, 0);
            final int version = this.in.readTTFLong();
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " version: " + version / 65536 + "."
                        + version % 65536);
            }
            final int slo = this.in.readTTFUShort();
            final int flo = this.in.readTTFUShort();
            final int llo = this.in.readTTFUShort();
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " script list offset: " + slo);
                log.debug(tableTag + " feature list offset: " + flo);
                log.debug(tableTag + " lookup list offset: " + llo);
            }
            final long to = dirTab.getOffset();
            readCommonLayoutTables(tableTag, to + slo, to + flo, to + llo);
            GlyphSubstitutionTable gsub;
            if ((gsub = constructGSUB()) != null) {
                this.gsub = gsub;
            }
        }
    }

    /**
     * Read the GPOS table.
     *
     * @throws IOException
     *             In case of a I/O problem
     */
    private void readGPOS() throws IOException {
        final TTFTableName tableTag = TTFTableName.GPOS;
        // Initialize temporary state
        initATState();
        // Read glyph positioning (GPOS) table
        final TTFDirTabEntry dirTab = this.ttf.getDirectoryEntry(tableTag);
        if (this.gpos != null) {
            if (log.isDebugEnabled()) {
                log.debug(tableTag + ": ignoring duplicate table");
            }
        } else if (dirTab != null) {
            this.ttf.seekTab(this.in, tableTag, 0);
            final int version = this.in.readTTFLong();
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " version: " + version / 65536 + "."
                        + version % 65536);
            }
            final int slo = this.in.readTTFUShort();
            final int flo = this.in.readTTFUShort();
            final int llo = this.in.readTTFUShort();
            if (log.isDebugEnabled()) {
                log.debug(tableTag + " script list offset: " + slo);
                log.debug(tableTag + " feature list offset: " + flo);
                log.debug(tableTag + " lookup list offset: " + llo);
            }
            final long to = dirTab.getOffset();
            readCommonLayoutTables(tableTag, to + slo, to + flo, to + llo);
            GlyphPositioningTable gpos;
            if ((gpos = constructGPOS()) != null) {
                this.gpos = gpos;
            }
        }
    }

    /**
     * Construct the (internal representation of the) GDEF table based on
     * previously parsed state.
     *
     * @returns glyph definition table or null if insufficient or invalid state
     */
    private GlyphDefinitionTable constructGDEF() {
        GlyphDefinitionTable gdef = null;
        List subtables;
        if ((subtables = constructGDEFSubtables()) != null) {
            if (subtables.size() > 0) {
                gdef = new GlyphDefinitionTable(subtables);
            }
        }
        resetATState();
        return gdef;
    }

    /**
     * Construct the (internal representation of the) GSUB table based on
     * previously parsed state.
     *
     * @returns glyph substitution table or null if insufficient or invalid
     *          state
     */
    private GlyphSubstitutionTable constructGSUB() {
        GlyphSubstitutionTable gsub = null;
        Map lookups;
        if ((lookups = constructLookups()) != null) {
            List subtables;
            if ((subtables = constructGSUBSubtables()) != null) {
                if (lookups.size() > 0 && subtables.size() > 0) {
                    gsub = new GlyphSubstitutionTable(this.gdef, lookups,
                            subtables);
                }
            }
        }
        resetATState();
        return gsub;
    }

    /**
     * Construct the (internal representation of the) GPOS table based on
     * previously parsed state.
     *
     * @returns glyph positioning table or null if insufficient or invalid state
     */
    private GlyphPositioningTable constructGPOS() {
        GlyphPositioningTable gpos = null;
        Map lookups;
        if ((lookups = constructLookups()) != null) {
            List subtables;
            if ((subtables = constructGPOSSubtables()) != null) {
                if (lookups.size() > 0 && subtables.size() > 0) {
                    gpos = new GlyphPositioningTable(this.gdef, lookups,
                            subtables);
                }
            }
        }
        resetATState();
        return gpos;
    }

    private void constructLookupsFeature(final Map lookups, final String st,
            final String lt, final String fid) {
        final Object[] fp = (Object[]) this.seFeatures.get(fid);
        if (fp != null) {
            assert fp.length == 2;
            final String ft = (String) fp[0]; // feature tag
            final List/* <String> */lul = (List) fp[1]; // list of lookup table
            // ids
            if (ft != null && lul != null && lul.size() > 0) {
                final GlyphTable.LookupSpec ls = new GlyphTable.LookupSpec(st,
                        lt, ft);
                lookups.put(ls, lul);
            }
        }
    }

    private void constructLookupsFeatures(final Map lookups, final String st,
            final String lt, final List/* <String> */fids) {
        for (final Iterator fit = fids.iterator(); fit.hasNext();) {
            final String fid = (String) fit.next();
            constructLookupsFeature(lookups, st, lt, fid);
        }
    }

    private void constructLookupsLanguage(final Map lookups, final String st,
            final String lt, final Map/* <String,Object[2]> */languages) {
        final Object[] lp = (Object[]) languages.get(lt);
        if (lp != null) {
            assert lp.length == 2;
            if (lp[0] != null) { // required feature id
                constructLookupsFeature(lookups, st, lt, (String) lp[0]);
            }
            if (lp[1] != null) { // non-required features ids
                constructLookupsFeatures(lookups, st, lt, (List) lp[1]);
            }
        }
    }

    private void constructLookupsLanguages(final Map lookups, final String st,
            final List/* <String> */ll,
            final Map/* <String,Object[2]> */languages) {
        for (final Iterator lit = ll.iterator(); lit.hasNext();) {
            final String lt = (String) lit.next();
            constructLookupsLanguage(lookups, st, lt, languages);
        }
    }

    private Map constructLookups() {
        final Map/* <GlyphTable.LookupSpec,List<String>> */lookups = new java.util.LinkedHashMap();
        for (final Iterator sit = this.seScripts.keySet().iterator(); sit
                .hasNext();) {
            final String st = (String) sit.next();
            final Object[] sp = (Object[]) this.seScripts.get(st);
            if (sp != null) {
                assert sp.length == 3;
                final Map/* <String,Object[2]> */languages = (Map) sp[2];
                if (sp[0] != null) { // default language
                    constructLookupsLanguage(lookups, st, (String) sp[0],
                            languages);
                }
                if (sp[1] != null) { // non-default languages
                    constructLookupsLanguages(lookups, st, (List) sp[1],
                            languages);
                }
            }
        }
        return lookups;
    }

    private List constructGDEFSubtables() {
        final List/* <GlyphDefinitionSubtable> */subtables = new java.util.ArrayList();
        if (this.seSubtables != null) {
            for (final Iterator it = this.seSubtables.iterator(); it.hasNext();) {
                final Object[] stp = (Object[]) it.next();
                GlyphSubtable st;
                if ((st = constructGDEFSubtable(stp)) != null) {
                    subtables.add(st);
                }
            }
        }
        return subtables;
    }

    private GlyphSubtable constructGDEFSubtable(final Object[] stp) {
        GlyphSubtable st = null;
        assert stp != null && stp.length == 8;
        final Integer tt = (Integer) stp[0]; // table type
        final Integer lt = (Integer) stp[1]; // lookup type
        final Integer ln = (Integer) stp[2]; // lookup sequence number
        final Integer lf = (Integer) stp[3]; // lookup flags
        final Integer sn = (Integer) stp[4]; // subtable sequence number
        final Integer sf = (Integer) stp[5]; // subtable format
        final GlyphMappingTable mapping = (GlyphMappingTable) stp[6];
        final List entries = (List) stp[7];
        if (tt.intValue() == GlyphTable.GLYPH_TABLE_TYPE_DEFINITION) {
            final int type = GDEFLookupType.getSubtableType(lt.intValue());
            final String lid = "lu" + ln.intValue();
            final int sequence = sn.intValue();
            final int flags = lf.intValue();
            final int format = sf.intValue();
            st = GlyphDefinitionTable.createSubtable(type, lid, sequence,
                    flags, format, mapping, entries);
        }
        return st;
    }

    private List constructGSUBSubtables() {
        final List/* <GlyphSubtable> */subtables = new java.util.ArrayList();
        if (this.seSubtables != null) {
            for (final Iterator it = this.seSubtables.iterator(); it.hasNext();) {
                final Object[] stp = (Object[]) it.next();
                GlyphSubtable st;
                if ((st = constructGSUBSubtable(stp)) != null) {
                    subtables.add(st);
                }
            }
        }
        return subtables;
    }

    private GlyphSubtable constructGSUBSubtable(final Object[] stp) {
        GlyphSubtable st = null;
        assert stp != null && stp.length == 8;
        final Integer tt = (Integer) stp[0]; // table type
        final Integer lt = (Integer) stp[1]; // lookup type
        final Integer ln = (Integer) stp[2]; // lookup sequence number
        final Integer lf = (Integer) stp[3]; // lookup flags
        final Integer sn = (Integer) stp[4]; // subtable sequence number
        final Integer sf = (Integer) stp[5]; // subtable format
        final GlyphCoverageTable coverage = (GlyphCoverageTable) stp[6];
        final List entries = (List) stp[7];
        if (tt.intValue() == GlyphTable.GLYPH_TABLE_TYPE_SUBSTITUTION) {
            final int type = GSUBLookupType.getSubtableType(lt.intValue());
            final String lid = "lu" + ln.intValue();
            final int sequence = sn.intValue();
            final int flags = lf.intValue();
            final int format = sf.intValue();
            st = GlyphSubstitutionTable.createSubtable(type, lid, sequence,
                    flags, format, coverage, entries);
        }
        return st;
    }

    private List constructGPOSSubtables() {
        final List/* <GlyphSubtable> */subtables = new java.util.ArrayList();
        if (this.seSubtables != null) {
            for (final Iterator it = this.seSubtables.iterator(); it.hasNext();) {
                final Object[] stp = (Object[]) it.next();
                GlyphSubtable st;
                if ((st = constructGPOSSubtable(stp)) != null) {
                    subtables.add(st);
                }
            }
        }
        return subtables;
    }

    private GlyphSubtable constructGPOSSubtable(final Object[] stp) {
        GlyphSubtable st = null;
        assert stp != null && stp.length == 8;
        final Integer tt = (Integer) stp[0]; // table type
        final Integer lt = (Integer) stp[1]; // lookup type
        final Integer ln = (Integer) stp[2]; // lookup sequence number
        final Integer lf = (Integer) stp[3]; // lookup flags
        final Integer sn = (Integer) stp[4]; // subtable sequence number
        final Integer sf = (Integer) stp[5]; // subtable format
        final GlyphCoverageTable coverage = (GlyphCoverageTable) stp[6];
        final List entries = (List) stp[7];
        if (tt.intValue() == GlyphTable.GLYPH_TABLE_TYPE_POSITIONING) {
            final int type = GSUBLookupType.getSubtableType(lt.intValue());
            final String lid = "lu" + ln.intValue();
            final int sequence = sn.intValue();
            final int flags = lf.intValue();
            final int format = sf.intValue();
            st = GlyphPositioningTable.createSubtable(type, lid, sequence,
                    flags, format, coverage, entries);
        }
        return st;
    }

    private void initATState() {
        this.seScripts = new java.util.LinkedHashMap();
        this.seLanguages = new java.util.LinkedHashMap();
        this.seFeatures = new java.util.LinkedHashMap();
        this.seSubtables = new java.util.ArrayList();
        resetATSubState();
    }

    private void resetATState() {
        this.seScripts = null;
        this.seLanguages = null;
        this.seFeatures = null;
        this.seSubtables = null;
        resetATSubState();
    }

    private void initATSubState() {
        this.seMapping = null;
        this.seEntries = new java.util.ArrayList();
    }

    private void extractSESubState(final int tableType, final int lookupType,
            final int lookupFlags, final int lookupSequence,
            final int subtableSequence, final int subtableFormat) {
        if (this.seEntries != null) {
            if (tableType == GlyphTable.GLYPH_TABLE_TYPE_DEFINITION
                    || this.seEntries.size() > 0) {
                if (this.seSubtables != null) {
                    final Integer tt = Integer.valueOf(tableType);
                    final Integer lt = Integer.valueOf(lookupType);
                    final Integer ln = Integer.valueOf(lookupSequence);
                    final Integer lf = Integer.valueOf(lookupFlags);
                    final Integer sn = Integer.valueOf(subtableSequence);
                    final Integer sf = Integer.valueOf(subtableFormat);
                    this.seSubtables.add(new Object[] { tt, lt, ln, lf, sn, sf,
                            this.seMapping, this.seEntries });
                }
            }
        }
    }

    private void resetATSubState() {
        this.seMapping = null;
        this.seEntries = null;
    }

    private void resetATStateAll() {
        resetATState();
        this.gdef = null;
        this.gsub = null;
        this.gpos = null;
    }

    /** helper method for formatting an integer array for output */
    private String toString(final int[] ia) {
        final StringBuffer sb = new StringBuffer();
        if (ia == null || ia.length == 0) {
            sb.append('-');
        } else {
            boolean first = true;
            for (final int element : ia) {
                if (!first) {
                    sb.append(' ');
                } else {
                    first = false;
                }
                sb.append(element);
            }
        }
        return sb.toString();
    }

}
