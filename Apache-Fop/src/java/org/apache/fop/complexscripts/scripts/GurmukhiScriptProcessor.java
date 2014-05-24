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

import org.apache.fop.complexscripts.fonts.GlyphDefinitionTable;
import org.apache.fop.complexscripts.util.GlyphSequence;

// CSOFF: AvoidNestedBlocksCheck
// CSOFF: NoWhitespaceAfterCheck
// CSOFF: WhitespaceAfter
// CSOFF: InnerAssignmentCheck
// CSOFF: SimplifyBooleanReturnCheck
// CSOFF: LineLengthCheck

/**
 * <p>
 * The <code>GurmukhiScriptProcessor</code> class implements a script processor
 * for performing glyph substitution and positioning operations on content
 * associated with the Gurmukhi script.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
public class GurmukhiScriptProcessor extends IndicScriptProcessor {

    GurmukhiScriptProcessor(final String script) {
        super(script);
    }

    @Override
    protected Class<? extends GurmukhiSyllabizer> getSyllabizerClass() {
        return GurmukhiSyllabizer.class;
    }

    @Override
    // find rightmost pre-base matra
    protected int findPreBaseMatra(final GlyphSequence gs) {
        final int ng = gs.getGlyphCount();
        int lk = -1;
        for (int i = ng; i > 0; i--) {
            final int k = i - 1;
            if (containsPreBaseMatra(gs, k)) {
                lk = k;
                break;
            }
        }
        return lk;
    }

    @Override
    // find leftmost pre-base matra target, starting from source
    protected int findPreBaseMatraTarget(final GlyphSequence gs,
            final int source) {
        final int ng = gs.getGlyphCount();
        int lk = -1;
        for (int i = source < ng ? source : ng; i > 0; i--) {
            final int k = i - 1;
            if (containsConsonant(gs, k)) {
                if (containsHalfConsonant(gs, k)) {
                    lk = k;
                } else if (lk == -1) {
                    lk = k;
                } else {
                    break;
                }
            }
        }
        return lk;
    }

    private static boolean containsPreBaseMatra(final GlyphSequence gs,
            final int k) {
        final GlyphSequence.CharAssociation a = gs.getAssociation(k);
        final int[] ca = gs.getCharacterArray(false);
        for (int i = a.getStart(), e = a.getEnd(); i < e; i++) {
            if (isPreM(ca[i])) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsConsonant(final GlyphSequence gs, final int k) {
        final GlyphSequence.CharAssociation a = gs.getAssociation(k);
        final int[] ca = gs.getCharacterArray(false);
        for (int i = a.getStart(), e = a.getEnd(); i < e; i++) {
            if (isC(ca[i])) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsHalfConsonant(final GlyphSequence gs,
            final int k) {
        final Boolean half = (Boolean) gs.getAssociation(k).getPredication(
                "half");
        return half != null ? half.booleanValue() : false;
    }

    @Override
    protected int findReph(final GlyphSequence gs) {
        final int ng = gs.getGlyphCount();
        int li = -1;
        for (int i = 0; i < ng; i++) {
            if (containsReph(gs, i)) {
                li = i;
                break;
            }
        }
        return li;
    }

    @Override
    protected int findRephTarget(final GlyphSequence gs, final int source) {
        final int ng = gs.getGlyphCount();
        int c1 = -1;
        int c2 = -1;
        // first candidate target is after first non-half consonant
        for (int i = 0; i < ng; i++) {
            if (i != source && containsConsonant(gs, i)) {
                if (!containsHalfConsonant(gs, i)) {
                    c1 = i + 1;
                    break;
                }
            }
        }
        // second candidate target is after last non-prebase matra after first
        // candidate or before first syllable or vedic mark
        for (int i = c1 >= 0 ? c1 : 0; i < ng; i++) {
            if (containsMatra(gs, i) && !containsPreBaseMatra(gs, i)) {
                c2 = i + 1;
            } else if (containsOtherMark(gs, i)) {
                c2 = i;
                break;
            }
        }
        if (c2 >= 0) {
            return c2;
        } else if (c1 >= 0) {
            return c1;
        } else {
            return source;
        }
    }

    private static boolean containsReph(final GlyphSequence gs, final int k) {
        final Boolean rphf = (Boolean) gs.getAssociation(k).getPredication(
                "rphf");
        return rphf != null ? rphf.booleanValue() : false;
    }

    private static boolean containsMatra(final GlyphSequence gs, final int k) {
        final GlyphSequence.CharAssociation a = gs.getAssociation(k);
        final int[] ca = gs.getCharacterArray(false);
        for (int i = a.getStart(), e = a.getEnd(); i < e; i++) {
            if (isM(ca[i])) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsOtherMark(final GlyphSequence gs, final int k) {
        final GlyphSequence.CharAssociation a = gs.getAssociation(k);
        final int[] ca = gs.getCharacterArray(false);
        for (int i = a.getStart(), e = a.getEnd(); i < e; i++) {
            switch (typeOf(ca[i])) {
            case C_T: // tone (e.g., udatta, anudatta)
            case C_A: // accent (e.g., acute, grave)
            case C_O: // other (e.g., candrabindu, anusvara, visarga, etc)
                return true;
            default:
                break;
            }
        }
        return false;
    }

    private static class GurmukhiSyllabizer extends DefaultSyllabizer {
        GurmukhiSyllabizer(final String script, final String language) {
            super(script, language);
        }

        @Override
        // | C ...
        protected int findStartOfSyllable(final int[] ca, int s, final int e) {
            if (s < 0 || s >= e) {
                return -1;
            } else {
                while (s < e) {
                    final int c = ca[s];
                    if (isC(c)) {
                        break;
                    } else {
                        s++;
                    }
                }
                return s;
            }
        }

        @Override
        // D* L? | ...
        protected int findEndOfSyllable(final int[] ca, int s, final int e) {
            if (s < 0 || s >= e) {
                return -1;
            } else {
                int nd = 0;
                int nl = 0;
                int i;
                // consume dead consonants
                while ((i = isDeadConsonant(ca, s, e)) > s) {
                    s = i;
                    nd++;
                }
                // consume zero or one live consonant
                if ((i = isLiveConsonant(ca, s, e)) > s) {
                    s = i;
                    nl++;
                }
                return nd > 0 || nl > 0 ? s : -1;
            }
        }

        // D := ( C N? H )?
        private int isDeadConsonant(final int[] ca, final int s, final int e) {
            if (s < 0) {
                return -1;
            } else {
                int c;
                int i = 0;
                int nc = 0;
                int nh = 0;
                do {
                    // C
                    if (s + i < e) {
                        c = ca[s + i];
                        if (isC(c)) {
                            i++;
                            nc++;
                        } else {
                            break;
                        }
                    }
                    // N?
                    if (s + i < e) {
                        c = ca[s + 1];
                        if (isN(c)) {
                            i++;
                        }
                    }
                    // H
                    if (s + i < e) {
                        c = ca[s + i];
                        if (isH(c)) {
                            i++;
                            nh++;
                        } else {
                            break;
                        }
                    }
                } while (false);
                return nc > 0 && nh > 0 ? s + i : -1;
            }
        }

        // L := ( (C|V) N? X* )?; where X = ( MATRA | ACCENT MARK | TONE MARK |
        // OTHER MARK )
        private int isLiveConsonant(final int[] ca, final int s, final int e) {
            if (s < 0) {
                return -1;
            } else {
                int c;
                int i = 0;
                int nc = 0;
                int nv = 0;
                int nx = 0;
                do {
                    // C
                    if (s + i < e) {
                        c = ca[s + i];
                        if (isC(c)) {
                            i++;
                            nc++;
                        } else if (isV(c)) {
                            i++;
                            nv++;
                        } else {
                            break;
                        }
                    }
                    // N?
                    if (s + i < e) {
                        c = ca[s + i];
                        if (isN(c)) {
                            i++;
                        }
                    }
                    // X*
                    while (s + i < e) {
                        c = ca[s + i];
                        if (isX(c)) {
                            i++;
                            nx++;
                        } else {
                            break;
                        }
                    }
                } while (false);
                // if no X but has H, then ignore C|I
                if (nx == 0) {
                    if (s + i < e) {
                        c = ca[s + i];
                        if (isH(c)) {
                            if (nc > 0) {
                                nc--;
                            } else if (nv > 0) {
                                nv--;
                            }
                        }
                    }
                }
                return nc > 0 || nv > 0 ? s + i : -1;
            }
        }
    }

    // gurmukhi character types
    static final short C_U = 0; // unassigned
    static final short C_C = 1; // consonant
    static final short C_V = 2; // vowel
    static final short C_M = 3; // vowel sign (matra)
    static final short C_S = 4; // symbol or sign
    static final short C_T = 5; // tone mark
    static final short C_A = 6; // accent mark
    static final short C_P = 7; // punctuation
    static final short C_D = 8; // digit
    static final short C_H = 9; // halant (virama)
    static final short C_O = 10; // other signs
    static final short C_N = 0x0100; // nukta(ized)
    static final short C_R = 0x0200; // reph(ized)
    static final short C_PRE = 0x0400; // pre-base
    static final short C_M_TYPE = 0x00FF; // type mask
    static final short C_M_FLAGS = 0x7F00; // flag mask
    // gurmukhi block range
    static final int ccaStart = 0x0A00; // first code point mapped by cca //
    // CSOK: ConstantNameCheck
    static final int ccaEnd = 0x0A80; // last code point + 1 mapped by cca //
    // CSOK: ConstantNameCheck
    // gurmukhi character type lookups
    static final short[] cca = { // CSOK: ConstantNameCheck
        C_U, // 0x0A00 // UNASSIGNED
        C_O, // 0x0A01 // ADAK BINDI
        C_O, // 0x0A02 // BINDI
        C_O, // 0x0A03 // VISARGA
        C_U, // 0x0A04 // UNASSIGNED
        C_V, // 0x0A05 // A
        C_V, // 0x0A06 // AA
        C_V, // 0x0A07 // I
        C_V, // 0x0A08 // II
        C_V, // 0x0A09 // U
        C_V, // 0x0A0A // UU
        C_U, // 0x0A0B // UNASSIGNED
        C_U, // 0x0A0C // UNASSIGNED
        C_U, // 0x0A0D // UNASSIGNED
        C_U, // 0x0A0E // UNASSIGNED
        C_V, // 0x0A0F // E
        C_V, // 0x0A10 // AI
        C_U, // 0x0A11 // UNASSIGNED
        C_U, // 0x0A12 // UNASSIGNED
        C_V, // 0x0A13 // O
        C_V, // 0x0A14 // AU
        C_C, // 0x0A15 // KA
        C_C, // 0x0A16 // KHA
        C_C, // 0x0A17 // GA
        C_C, // 0x0A18 // GHA
        C_C, // 0x0A19 // NGA
        C_C, // 0x0A1A // CA
        C_C, // 0x0A1B // CHA
        C_C, // 0x0A1C // JA
        C_C, // 0x0A1D // JHA
        C_C, // 0x0A1E // NYA
        C_C, // 0x0A1F // TTA
        C_C, // 0x0A20 // TTHA
        C_C, // 0x0A21 // DDA
        C_C, // 0x0A22 // DDHA
        C_C, // 0x0A23 // NNA
        C_C, // 0x0A24 // TA
        C_C, // 0x0A25 // THA
        C_C, // 0x0A26 // DA
        C_C, // 0x0A27 // DHA
        C_C, // 0x0A28 // NA
        C_U, // 0x0A29 // UNASSIGNED
        C_C, // 0x0A2A // PA
        C_C, // 0x0A2B // PHA
        C_C, // 0x0A2C // BA
        C_C, // 0x0A2D // BHA
        C_C, // 0x0A2E // MA
        C_C, // 0x0A2F // YA
        C_C | C_R, // 0x0A30 // RA // CSOK: WhitespaceAround
        C_U, // 0x0A31 // UNASSIGNED
        C_C, // 0x0A32 // LA
        C_C, // 0x0A33 // LLA
        C_U, // 0x0A34 // UNASSIGNED
        C_C, // 0x0A35 // VA
        C_C, // 0x0A36 // SHA
        C_U, // 0x0A37 // UNASSIGNED
        C_C, // 0x0A38 // SA
        C_C, // 0x0A39 // HA
        C_U, // 0x0A3A // UNASSIGNED
        C_U, // 0x0A3B // UNASSIGNED
        C_N, // 0x0A3C // NUKTA
        C_U, // 0x0A3D // UNASSIGNED
        C_M, // 0x0A3E // AA
        C_M | C_PRE, // 0x0A3F // I // CSOK: WhitespaceAround
        C_M, // 0x0A40 // II
        C_M, // 0x0A41 // U
        C_M, // 0x0A42 // UU
        C_U, // 0x0A43 // UNASSIGNED
        C_U, // 0x0A44 // UNASSIGNED
        C_U, // 0x0A45 // UNASSIGNED
        C_U, // 0x0A46 // UNASSIGNED
        C_M, // 0x0A47 // EE
        C_M, // 0x0A48 // AI
        C_U, // 0x0A49 // UNASSIGNED
        C_U, // 0x0A4A // UNASSIGNED
        C_M, // 0x0A4B // OO
        C_M, // 0x0A4C // AU
        C_H, // 0x0A4D // VIRAMA (HALANT)
        C_U, // 0x0A4E // UNASSIGNED
        C_U, // 0x0A4F // UNASSIGNED
        C_U, // 0x0A50 // UNASSIGNED
        C_T, // 0x0A51 // UDATTA
        C_U, // 0x0A52 // UNASSIGNED
        C_U, // 0x0A53 // UNASSIGNED
        C_U, // 0x0A54 // UNASSIGNED
        C_U, // 0x0A55 // UNASSIGNED
        C_U, // 0x0A56 // UNASSIGNED
        C_U, // 0x0A57 // UNASSIGNED
        C_U, // 0x0A58 // UNASSIGNED
        C_C | C_N, // 0x0A59 // KHHA // CSOK: WhitespaceAround
        C_C | C_N, // 0x0A5A // GHHA // CSOK: WhitespaceAround
        C_C | C_N, // 0x0A5B // ZA // CSOK: WhitespaceAround
        C_C | C_N, // 0x0A5C // RRA // CSOK: WhitespaceAround
        C_U, // 0x0A5D // UNASSIGNED
        C_C | C_N, // 0x0A5E // FA // CSOK: WhitespaceAround
        C_U, // 0x0A5F // UNASSIGNED
        C_U, // 0x0A60 // UNASSIGNED
        C_U, // 0x0A61 // UNASSIGNED
        C_U, // 0x0A62 // UNASSIGNED
        C_U, // 0x0A63 // UNASSIGNED
        C_U, // 0x0A64 // UNASSIGNED
        C_U, // 0x0A65 // UNASSIGNED
        C_D, // 0x0A66 // ZERO
        C_D, // 0x0A67 // ONE
        C_D, // 0x0A68 // TWO
        C_D, // 0x0A69 // THREE
        C_D, // 0x0A6A // FOUR
        C_D, // 0x0A6B // FIVE
        C_D, // 0x0A6C // SIX
        C_D, // 0x0A6D // SEVEN
        C_D, // 0x0A6E // EIGHT
        C_D, // 0x0A6F // NINE
        C_O, // 0x0A70 // TIPPI
        C_O, // 0x0A71 // ADDAK
        C_V, // 0x0A72 // IRI
        C_V, // 0x0A73 // URA
        C_S, // 0x0A74 // EK ONKAR
        C_O, // 0x0A75 // YAKASH
        C_U, // 0x0A76 // UNASSIGNED
        C_U, // 0x0A77 // UNASSIGNED
        C_U, // 0x0A78 // UNASSIGNED
        C_U, // 0x0A79 // UNASSIGNED
        C_U, // 0x0A7A // UNASSIGNED
        C_U, // 0x0A7B // UNASSIGNED
        C_U, // 0x0A7C // UNASSIGNED
        C_U, // 0x0A7D // UNASSIGNED
        C_U, // 0x0A7E // UNASSIGNED
        C_U // 0x0A7F // UNASSIGNED
    };

    static int typeOf(final int c) {
        if (c >= ccaStart && c < ccaEnd) {
            return cca[c - ccaStart] & C_M_TYPE;
        } else {
            return C_U;
        }
    }

    static boolean isType(final int c, final int t) {
        return typeOf(c) == t;
    }

    static boolean hasFlag(final int c, final int f) {
        if (c >= ccaStart && c < ccaEnd) {
            return (cca[c - ccaStart] & f) == f;
        } else {
            return false;
        }
    }

    static boolean isC(final int c) {
        return isType(c, C_C);
    }

    static boolean isR(final int c) {
        return isType(c, C_C) && hasR(c);
    }

    static boolean isV(final int c) {
        return isType(c, C_V);
    }

    static boolean isN(final int c) {
        return c == 0x0A3C;
    }

    static boolean isH(final int c) {
        return c == 0x0A4D;
    }

    static boolean isM(final int c) {
        return isType(c, C_M);
    }

    static boolean isPreM(final int c) {
        return isType(c, C_M) && hasFlag(c, C_PRE);
    }

    static boolean isX(final int c) {
        switch (typeOf(c)) {
        case C_M: // matra (combining vowel)
        case C_A: // accent mark
        case C_T: // tone mark
        case C_O: // other (modifying) mark
            return true;
        default:
            return false;
        }
    }

    static boolean hasR(final int c) {
        return hasFlag(c, C_R);
    }

    static boolean hasN(final int c) {
        return hasFlag(c, C_N);
    }

    @Override
    public GlyphSequence reorderCombiningMarks(final GlyphDefinitionTable gdef,
            final GlyphSequence gs, final int[][] gpa, final String script,
            final String language) {
        return super.reorderCombiningMarks(gdef, gs, gpa, script, language);
    }

}
