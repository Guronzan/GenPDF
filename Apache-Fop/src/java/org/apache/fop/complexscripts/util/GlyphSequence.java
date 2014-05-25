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

package org.apache.fop.complexscripts.util;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// CSOFF: InnerAssignmentCheck
// CSOFF: LineLengthCheck
// CSOFF: WhitespaceAfterCheck
// CSOFF: NoWhitespaceAfterCheck

/**
 * <p>
 * A GlyphSequence encapsulates a sequence of character codes, a sequence of
 * glyph codes, and a sequence of character associations, where, for each glyph
 * in the sequence of glyph codes, there is a corresponding character
 * association. Character associations server to relate the glyph codes in a
 * glyph sequence to the specific characters in an original character code
 * sequence with which the glyph codes are associated.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
public class GlyphSequence implements Cloneable {

    /**
     * default character buffer capacity in case new character buffer is created
     */
    private static final int DEFAULT_CHARS_CAPACITY = 8;

    /** character buffer */
    private IntBuffer characters;
    /** glyph buffer */
    private IntBuffer glyphs;
    /** association list */
    private List associations;
    /** predications flag */
    private boolean predications;

    /**
     * Instantiate a glyph sequence, reusing (i.e., not copying) the referenced
     * character and glyph buffers and associations. If characters is null, then
     * an empty character buffer is created. If glyphs is null, then a glyph
     * buffer is created whose capacity is that of the character buffer. If
     * associations is null, then identity associations are created.
     * 
     * @param characters
     *            a (possibly null) buffer of associated (originating)
     *            characters
     * @param glyphs
     *            a (possibly null) buffer of glyphs
     * @param associations
     *            a (possibly null) array of glyph to character associations
     * @param predications
     *            true if predications are enabled
     */
    public GlyphSequence(IntBuffer characters, IntBuffer glyphs,
            List associations, final boolean predications) {
        if (characters == null) {
            characters = IntBuffer.allocate(DEFAULT_CHARS_CAPACITY);
        }
        if (glyphs == null) {
            glyphs = IntBuffer.allocate(characters.capacity());
        }
        if (associations == null) {
            associations = makeIdentityAssociations(characters.limit(),
                    glyphs.limit());
        }
        this.characters = characters;
        this.glyphs = glyphs;
        this.associations = associations;
        this.predications = predications;
    }

    /**
     * Instantiate a glyph sequence, reusing (i.e., not copying) the referenced
     * character and glyph buffers and associations. If characters is null, then
     * an empty character buffer is created. If glyphs is null, then a glyph
     * buffer is created whose capacity is that of the character buffer. If
     * associations is null, then identity associations are created.
     * 
     * @param characters
     *            a (possibly null) buffer of associated (originating)
     *            characters
     * @param glyphs
     *            a (possibly null) buffer of glyphs
     * @param associations
     *            a (possibly null) array of glyph to character associations
     */
    public GlyphSequence(final IntBuffer characters, final IntBuffer glyphs,
            final List associations) {
        this(characters, glyphs, associations, false);
    }

    /**
     * Instantiate a glyph sequence using an existing glyph sequence, where the
     * new glyph sequence shares the character array of the existing sequence
     * (but not the buffer object), and creates new copies of glyphs buffer and
     * association list.
     * 
     * @param gs
     *            an existing glyph sequence
     */
    public GlyphSequence(final GlyphSequence gs) {
        this(gs.characters.duplicate(), copyBuffer(gs.glyphs),
                copyAssociations(gs.associations), gs.predications);
    }

    /**
     * Instantiate a glyph sequence using an existing glyph sequence, where the
     * new glyph sequence shares the character array of the existing sequence
     * (but not the buffer object), but uses the specified backtrack, input, and
     * lookahead glyph arrays to populate the glyphs, and uses the specified of
     * glyphs buffer and association list. backtrack, input, and lookahead
     * association arrays to populate the associations.
     * 
     * @param gs
     *            an existing glyph sequence
     * @param bga
     *            backtrack glyph array
     * @param iga
     *            input glyph array
     * @param lga
     *            lookahead glyph array
     * @param bal
     *            backtrack association list
     * @param ial
     *            input association list
     * @param lal
     *            lookahead association list
     */
    public GlyphSequence(final GlyphSequence gs, final int[] bga,
            final int[] iga, final int[] lga, final CharAssociation[] bal,
            final CharAssociation[] ial, final CharAssociation[] lal) {
        this(gs.characters.duplicate(), concatGlyphs(bga, iga, lga),
                concatAssociations(bal, ial, lal), gs.predications);
    }

    /**
     * Obtain reference to underlying character buffer.
     * 
     * @return character buffer reference
     */
    public IntBuffer getCharacters() {
        return this.characters;
    }

    /**
     * Obtain array of characters. If <code>copy</code> is true, then a newly
     * instantiated array is returned, otherwise a reference to the underlying
     * buffer's array is returned. N.B. in case a reference to the undelying
     * buffer's array is returned, the length of the array is not necessarily
     * the number of characters in array. To determine the number of characters,
     * use {@link #getCharacterCount}.
     * 
     * @param copy
     *            true if to return a newly instantiated array of characters
     * @return array of characters
     */
    public int[] getCharacterArray(final boolean copy) {
        if (copy) {
            return toArray(this.characters);
        } else {
            return this.characters.array();
        }
    }

    /**
     * Obtain the number of characters in character array, where each character
     * constitutes a unicode scalar value.
     * 
     * @return number of characters available in character array
     */
    public int getCharacterCount() {
        return this.characters.limit();
    }

    /**
     * Obtain glyph id at specified index.
     * 
     * @param index
     *            to obtain glyph
     * @return the glyph identifier of glyph at specified index
     * @throws IndexOutOfBoundsException
     *             if index is less than zero or exceeds last valid position
     */
    public int getGlyph(final int index) throws IndexOutOfBoundsException {
        return this.glyphs.get(index);
    }

    /**
     * Set glyph id at specified index.
     * 
     * @param index
     *            to set glyph
     * @param gi
     *            glyph index
     * @throws IndexOutOfBoundsException
     *             if index is greater or equal to the limit of the underlying
     *             glyph buffer
     */
    public void setGlyph(final int index, int gi)
            throws IndexOutOfBoundsException {
        if (gi > 65535) {
            gi = 65535;
        }
        this.glyphs.put(index, gi);
    }

    /**
     * Obtain reference to underlying glyph buffer.
     * 
     * @return glyph buffer reference
     */
    public IntBuffer getGlyphs() {
        return this.glyphs;
    }

    /**
     * Obtain count glyphs starting at offset. If <code>count</code> is
     * negative, then it is treated as if the number of available glyphs were
     * specified.
     * 
     * @param offset
     *            into glyph sequence
     * @param count
     *            of glyphs to obtain starting at offset, or negative,
     *            indicating all avaialble glyphs starting at offset
     * @return glyph array
     */
    public int[] getGlyphs(int offset, int count) {
        final int ng = getGlyphCount();
        if (offset < 0) {
            offset = 0;
        } else if (offset > ng) {
            offset = ng;
        }
        if (count < 0) {
            count = ng - offset;
        }
        final int[] ga = new int[count];
        for (int i = offset, n = offset + count, k = 0; i < n; i++) {
            if (k < ga.length) {
                ga[k++] = this.glyphs.get(i);
            }
        }
        return ga;
    }

    /**
     * Obtain array of glyphs. If <code>copy</code> is true, then a newly
     * instantiated array is returned, otherwise a reference to the underlying
     * buffer's array is returned. N.B. in case a reference to the undelying
     * buffer's array is returned, the length of the array is not necessarily
     * the number of glyphs in array. To determine the number of glyphs, use
     * {@link #getGlyphCount}.
     * 
     * @param copy
     *            true if to return a newly instantiated array of glyphs
     * @return array of glyphs
     */
    public int[] getGlyphArray(final boolean copy) {
        if (copy) {
            return toArray(this.glyphs);
        } else {
            return this.glyphs.array();
        }
    }

    /**
     * Obtain the number of glyphs in glyphs array, where each glyph constitutes
     * a font specific glyph index.
     * 
     * @return number of glyphs available in character array
     */
    public int getGlyphCount() {
        return this.glyphs.limit();
    }

    /**
     * Obtain association at specified index.
     * 
     * @param index
     *            into associations array
     * @return glyph to character associations at specified index
     * @throws IndexOutOfBoundsException
     *             if index is less than zero or exceeds last valid position
     */
    public CharAssociation getAssociation(final int index)
            throws IndexOutOfBoundsException {
        return (CharAssociation) this.associations.get(index);
    }

    /**
     * Obtain reference to underlying associations list.
     * 
     * @return associations list
     */
    public List getAssociations() {
        return this.associations;
    }

    /**
     * Obtain count associations starting at offset.
     * 
     * @param offset
     *            into glyph sequence
     * @param count
     *            of associations to obtain starting at offset, or negative,
     *            indicating all avaialble associations starting at offset
     * @return associations
     */
    public CharAssociation[] getAssociations(int offset, int count) {
        final int ng = getGlyphCount();
        if (offset < 0) {
            offset = 0;
        } else if (offset > ng) {
            offset = ng;
        }
        if (count < 0) {
            count = ng - offset;
        }
        final CharAssociation[] aa = new CharAssociation[count];
        for (int i = offset, n = offset + count, k = 0; i < n; i++) {
            if (k < aa.length) {
                aa[k++] = (CharAssociation) this.associations.get(i);
            }
        }
        return aa;
    }

    /**
     * Enable or disable predications.
     * 
     * @param enable
     *            true if predications are to be enabled; otherwise false to
     *            disable
     */
    public void setPredications(final boolean enable) {
        this.predications = enable;
    }

    /**
     * Obtain predications state.
     * 
     * @return true if predications are enabled
     */
    public boolean getPredications() {
        return this.predications;
    }

    /**
     * Set predication <KEY,VALUE> at glyph sequence OFFSET.
     * 
     * @param offset
     *            offset (index) into glyph sequence
     * @param key
     *            predication key
     * @param value
     *            predication value
     */
    public void setPredication(final int offset, final String key,
            final Object value) {
        if (this.predications) {
            final CharAssociation[] aa = getAssociations(offset, 1);
            final CharAssociation ca = aa[0];
            ca.setPredication(key, value);
        }
    }

    /**
     * Get predication KEY at glyph sequence OFFSET.
     * 
     * @param offset
     *            offset (index) into glyph sequence
     * @param key
     *            predication key
     * @return predication KEY at OFFSET or null if none exists
     */
    public Object getPredication(final int offset, final String key) {
        if (this.predications) {
            final CharAssociation[] aa = getAssociations(offset, 1);
            final CharAssociation ca = aa[0];
            return ca.getPredication(key);
        } else {
            return null;
        }
    }

    /**
     * Compare glyphs.
     * 
     * @param gb
     *            buffer containing glyph indices with which this glyph
     *            sequence's glyphs are to be compared
     * @return zero if glyphs are the same, otherwise returns 1 or -1 according
     *         to whether this glyph sequence's glyphs are lexicographically
     *         greater or lesser than the glyphs in the specified string buffer
     */
    public int compareGlyphs(final IntBuffer gb) {
        final int ng = getGlyphCount();
        for (int i = 0, n = gb.limit(); i < n; i++) {
            if (i < ng) {
                final int g1 = this.glyphs.get(i);
                final int g2 = gb.get(i);
                if (g1 > g2) {
                    return 1;
                } else if (g1 < g2) {
                    return -1;
                }
            } else {
                return -1; // this gb is a proper prefix of specified gb
            }
        }
        return 0; // same lengths with no difference
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() {
        try {
            final GlyphSequence gs = (GlyphSequence) super.clone();
            gs.characters = copyBuffer(this.characters);
            gs.glyphs = copyBuffer(this.glyphs);
            gs.associations = copyAssociations(this.associations);
            return gs;
        } catch (final CloneNotSupportedException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("chars = [");
        sb.append(this.characters);
        sb.append("], glyphs = [");
        sb.append(this.glyphs);
        sb.append("], associations = [");
        sb.append(this.associations);
        sb.append("]");
        sb.append('}');
        return sb.toString();
    }

    /**
     * Determine if two arrays of glyphs are identical.
     * 
     * @param ga1
     *            first glyph array
     * @param ga2
     *            second glyph array
     * @return true if arrays are botth null or both non-null and have identical
     *         elements
     */
    public static boolean sameGlyphs(final int[] ga1, final int[] ga2) {
        if (ga1 == ga2) {
            return true;
        } else if (ga1 == null || ga2 == null) {
            return false;
        } else if (ga1.length != ga2.length) {
            return false;
        } else {
            for (int i = 0, n = ga1.length; i < n; i++) {
                if (ga1[i] != ga2[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Concatenante glyph arrays.
     * 
     * @param bga
     *            backtrack glyph array
     * @param iga
     *            input glyph array
     * @param lga
     *            lookahead glyph array
     * @return new integer buffer containing concatenated glyphs
     */
    public static IntBuffer concatGlyphs(final int[] bga, final int[] iga,
            final int[] lga) {
        int ng = 0;
        if (bga != null) {
            ng += bga.length;
        }
        if (iga != null) {
            ng += iga.length;
        }
        if (lga != null) {
            ng += lga.length;
        }
        final IntBuffer gb = IntBuffer.allocate(ng);
        if (bga != null) {
            gb.put(bga);
        }
        if (iga != null) {
            gb.put(iga);
        }
        if (lga != null) {
            gb.put(lga);
        }
        gb.flip();
        return gb;
    }

    /**
     * Concatenante association arrays.
     * 
     * @param baa
     *            backtrack association array
     * @param iaa
     *            input association array
     * @param laa
     *            lookahead association array
     * @return new list containing concatenated associations
     */
    public static List concatAssociations(final CharAssociation[] baa,
            final CharAssociation[] iaa, final CharAssociation[] laa) {
        int na = 0;
        if (baa != null) {
            na += baa.length;
        }
        if (iaa != null) {
            na += iaa.length;
        }
        if (laa != null) {
            na += laa.length;
        }
        if (na > 0) {
            final List gl = new ArrayList(na);
            if (baa != null) {
                for (int i = 0; i < baa.length; i++) {
                    gl.add(baa[i]);
                }
            }
            if (iaa != null) {
                for (int i = 0; i < iaa.length; i++) {
                    gl.add(iaa[i]);
                }
            }
            if (laa != null) {
                for (int i = 0; i < laa.length; i++) {
                    gl.add(laa[i]);
                }
            }
            return gl;
        } else {
            return null;
        }
    }

    /**
     * Join (concatenate) glyph sequences.
     * 
     * @param gs
     *            original glyph sequence from which to reuse character array
     *            reference
     * @param sa
     *            array of glyph sequences, whose glyph arrays and association
     *            lists are to be concatenated
     * @return new glyph sequence referring to character array of GS and
     *         concatenated glyphs and associations of SA
     */
    public static GlyphSequence join(final GlyphSequence gs,
            final GlyphSequence[] sa) {
        assert sa != null;
        int tg = 0;
        int ta = 0;
        for (int i = 0, n = sa.length; i < n; i++) {
            final GlyphSequence s = sa[i];
            final IntBuffer ga = s.getGlyphs();
            assert ga != null;
            final int ng = ga.limit();
            final List al = s.getAssociations();
            assert al != null;
            final int na = al.size();
            assert na == ng;
            tg += ng;
            ta += na;
        }
        final IntBuffer uga = IntBuffer.allocate(tg);
        final ArrayList ual = new ArrayList(ta);
        for (int i = 0, n = sa.length; i < n; i++) {
            final GlyphSequence s = sa[i];
            uga.put(s.getGlyphs());
            ual.addAll(s.getAssociations());
        }
        return new GlyphSequence(gs.getCharacters(), uga, ual,
                gs.getPredications());
    }

    /**
     * Reorder sequence such that [SOURCE,SOURCE+COUNT) is moved just prior to
     * TARGET.
     * 
     * @param gs
     *            input sequence
     * @param source
     *            index of sub-sequence to reorder
     * @param count
     *            length of sub-sequence to reorder
     * @param target
     *            index to which source sub-sequence is to be moved
     * @return reordered sequence (or original if no reordering performed)
     */
    public static GlyphSequence reorder(final GlyphSequence gs,
            final int source, final int count, final int target) {
        if (source != target) {
            final int ng = gs.getGlyphCount();
            final int[] ga = gs.getGlyphArray(false);
            final int[] nga = new int[ng];
            final GlyphSequence.CharAssociation[] aa = gs
                    .getAssociations(0, ng);
            final GlyphSequence.CharAssociation[] naa = new GlyphSequence.CharAssociation[ng];
            if (source < target) {
                int t = 0;
                for (int s = 0, e = source; s < e; s++, t++) {
                    nga[t] = ga[s];
                    naa[t] = aa[s];
                }
                for (int s = source + count, e = target; s < e; s++, t++) {
                    nga[t] = ga[s];
                    naa[t] = aa[s];
                }
                for (int s = source, e = source + count; s < e; s++, t++) {
                    nga[t] = ga[s];
                    naa[t] = aa[s];
                }
                for (int s = target, e = ng; s < e; s++, t++) {
                    nga[t] = ga[s];
                    naa[t] = aa[s];
                }
            } else {
                int t = 0;
                for (int s = 0, e = target; s < e; s++, t++) {
                    nga[t] = ga[s];
                    naa[t] = aa[s];
                }
                for (int s = source, e = source + count; s < e; s++, t++) {
                    nga[t] = ga[s];
                    naa[t] = aa[s];
                }
                for (int s = target, e = source; s < e; s++, t++) {
                    nga[t] = ga[s];
                    naa[t] = aa[s];
                }
                for (int s = source + count, e = ng; s < e; s++, t++) {
                    nga[t] = ga[s];
                    naa[t] = aa[s];
                }
            }
            return new GlyphSequence(gs, null, nga, null, null, naa, null);
        } else {
            return gs;
        }
    }

    private static int[] toArray(final IntBuffer ib) {
        if (ib != null) {
            final int n = ib.limit();
            final int[] ia = new int[n];
            ib.get(ia, 0, n);
            return ia;
        } else {
            return new int[0];
        }
    }

    private static List makeIdentityAssociations(final int numChars,
            final int numGlyphs) {
        final int nc = numChars;
        final int ng = numGlyphs;
        final List av = new ArrayList(ng);
        for (int i = 0, n = ng; i < n; i++) {
            final int k = i > nc ? nc : i;
            av.add(new CharAssociation(i, k == nc ? 0 : 1));
        }
        return av;
    }

    private static IntBuffer copyBuffer(final IntBuffer ib) {
        if (ib != null) {
            final int[] ia = new int[ib.capacity()];
            final int p = ib.position();
            final int l = ib.limit();
            System.arraycopy(ib.array(), 0, ia, 0, ia.length);
            return IntBuffer.wrap(ia, p, l - p);
        } else {
            return null;
        }
    }

    private static List copyAssociations(final List ca) {
        if (ca != null) {
            return new ArrayList(ca);
        } else {
            return ca;
        }
    }

    /**
     * A structure class encapsulating an interval of characters expressed as an
     * offset and count of Unicode scalar values (in an IntBuffer). A
     * <code>CharAssociation</code> is used to maintain a backpointer from a
     * glyph to one or more character intervals from which the glyph was
     * derived.
     *
     * Each glyph in a glyph sequence is associated with a single
     * <code>CharAssociation</code> instance.
     *
     * A <code>CharAssociation</code> instance is additionally (and optionally)
     * used to record predication information about the glyph, such as whether
     * the glyph was produced by the application of a specific substitution
     * table or whether its position was adjusted by a specific poisitioning
     * table.
     */
    public static class CharAssociation implements Cloneable {

        // instance state
        private final int offset;
        private final int count;
        private final int[] subIntervals;
        private Map<String, Object> predications;

        // class state
        private static volatile Map<String, PredicationMerger> predicationMergers;

        interface PredicationMerger {
            Object merge(final String key, final Object v1, final Object v2);
        }

        /**
         * Instantiate a character association.
         * 
         * @param offset
         *            into array of Unicode scalar values (in associated
         *            IntBuffer)
         * @param count
         *            of Unicode scalar values (in associated IntBuffer)
         * @param subIntervals
         *            if disjoint, then array of sub-intervals, otherwise null;
         *            even members of array are sub-interval starts, and odd
         *            members are sub-interval ends (exclusive)
         */
        public CharAssociation(final int offset, final int count,
                final int[] subIntervals) {
            this.offset = offset;
            this.count = count;
            this.subIntervals = subIntervals != null && subIntervals.length > 2 ? subIntervals
                    : null;
        }

        /**
         * Instantiate a non-disjoint character association.
         * 
         * @param offset
         *            into array of UTF-16 code elements (in associated
         *            CharSequence)
         * @param count
         *            of UTF-16 character code elements (in associated
         *            CharSequence)
         */
        public CharAssociation(final int offset, final int count) {
            this(offset, count, null);
        }

        /**
         * Instantiate a non-disjoint character association.
         * 
         * @param subIntervals
         *            if disjoint, then array of sub-intervals, otherwise null;
         *            even members of array are sub-interval starts, and odd
         *            members are sub-interval ends (exclusive)
         */
        public CharAssociation(final int[] subIntervals) {
            this(getSubIntervalsStart(subIntervals),
                    getSubIntervalsLength(subIntervals), subIntervals);
        }

        /** @return offset (start of association interval) */
        public int getOffset() {
            return this.offset;
        }

        /** @return count (number of characer codes in association) */
        public int getCount() {
            return this.count;
        }

        /** @return start of association interval */
        public int getStart() {
            return getOffset();
        }

        /** @return end of association interval */
        public int getEnd() {
            return getOffset() + getCount();
        }

        /** @return true if association is disjoint */
        public boolean isDisjoint() {
            return this.subIntervals != null;
        }

        /** @return subintervals of disjoint association */
        public int[] getSubIntervals() {
            return this.subIntervals;
        }

        /** @return count of subintervals of disjoint association */
        public int getSubIntervalCount() {
            return this.subIntervals != null ? this.subIntervals.length / 2 : 0;
        }

        /**
         * @param offset
         *            of interval in sequence
         * @param count
         *            length of interval
         * @return true if this association is contained within
         *         [offset,offset+count)
         */
        public boolean contained(final int offset, final int count) {
            final int s = offset;
            final int e = offset + count;
            if (!isDisjoint()) {
                final int s0 = getStart();
                final int e0 = getEnd();
                return s0 >= s && e0 <= e;
            } else {
                final int ns = getSubIntervalCount();
                for (int i = 0; i < ns; i++) {
                    final int s0 = this.subIntervals[2 * i + 0];
                    final int e0 = this.subIntervals[2 * i + 1];
                    if (s0 >= s && e0 <= e) {
                        return true;
                    }
                }
                return false;
            }
        }

        /**
         * Set predication <KEY,VALUE>.
         * 
         * @param key
         *            predication key
         * @param value
         *            predication value
         */
        public void setPredication(final String key, final Object value) {
            if (this.predications == null) {
                this.predications = new HashMap<String, Object>();
            }
            if (this.predications != null) {
                this.predications.put(key, value);
            }
        }

        /**
         * Get predication KEY.
         * 
         * @param key
         *            predication key
         * @return predication KEY at OFFSET or null if none exists
         */
        public Object getPredication(final String key) {
            if (this.predications != null) {
                return this.predications.get(key);
            } else {
                return null;
            }
        }

        /**
         * Merge predication <KEY,VALUE>.
         * 
         * @param key
         *            predication key
         * @param value
         *            predication value
         */
        public void mergePredication(final String key, final Object value) {
            if (this.predications == null) {
                this.predications = new HashMap<String, Object>();
            }
            if (this.predications != null) {
                if (this.predications.containsKey(key)) {
                    final Object v1 = this.predications.get(key);
                    final Object v2 = value;
                    this.predications.put(key,
                            mergePredicationValues(key, v1, v2));
                } else {
                    this.predications.put(key, value);
                }
            }
        }

        /**
         * Merge predication values V1 and V2 on KEY. Uses registered
         * <code>PredicationMerger</code> if one exists, otherwise uses V2 if
         * non-null, otherwise uses V1.
         * 
         * @param key
         *            predication key
         * @param v1
         *            first (original) predication value
         * @param v2
         *            second (to be merged) predication value
         * @return merged value
         */
        public static Object mergePredicationValues(final String key,
                final Object v1, final Object v2) {
            final PredicationMerger pm = getPredicationMerger(key);
            if (pm != null) {
                return pm.merge(key, v1, v2);
            } else if (v2 != null) {
                return v2;
            } else {
                return v1;
            }
        }

        /**
         * Merge predications from another CA.
         * 
         * @param ca
         *            from which to merge
         */
        public void mergePredications(final CharAssociation ca) {
            if (ca.predications != null) {
                for (final Map.Entry<String, Object> e : ca.predications
                        .entrySet()) {
                    mergePredication(e.getKey(), e.getValue());
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public Object clone() {
            try {
                final CharAssociation ca = (CharAssociation) super.clone();
                if (this.predications != null) {
                    ca.predications = new HashMap<String, Object>(
                            this.predications);
                }
                return ca;
            } catch (final CloneNotSupportedException e) {
                return null;
            }
        }

        /**
         * Register predication merger PM for KEY.
         * 
         * @param key
         *            for predication merger
         * @param pm
         *            predication merger
         */
        public static void setPredicationMerger(final String key,
                final PredicationMerger pm) {
            if (predicationMergers == null) {
                predicationMergers = new HashMap<String, PredicationMerger>();
            }
            if (predicationMergers != null) {
                predicationMergers.put(key, pm);
            }
        }

        /**
         * Obtain predication merger for KEY.
         * 
         * @param key
         *            for predication merger
         * @return predication merger or null if none exists
         */
        public static PredicationMerger getPredicationMerger(final String key) {
            if (predicationMergers != null) {
                return predicationMergers.get(key);
            } else {
                return null;
            }
        }

        /**
         * Replicate association to form <code>repeat</code> new associations.
         * 
         * @param a
         *            association to replicate
         * @param repeat
         *            count
         * @return array of replicated associations
         */
        public static CharAssociation[] replicate(final CharAssociation a,
                final int repeat) {
            final CharAssociation[] aa = new CharAssociation[repeat];
            for (int i = 0, n = aa.length; i < n; i++) {
                aa[i] = (CharAssociation) a.clone();
            }
            return aa;
        }

        /**
         * Join (merge) multiple associations into a single, potentially
         * disjoint association.
         * 
         * @param aa
         *            array of associations to join
         * @return (possibly disjoint) association containing joined
         *         associations
         */
        public static CharAssociation join(final CharAssociation[] aa) {
            CharAssociation ca;
            // extract sorted intervals
            final int[] ia = extractIntervals(aa);
            if (ia == null || ia.length == 0) {
                ca = new CharAssociation(0, 0);
            } else if (ia.length == 2) {
                final int s = ia[0];
                final int e = ia[1];
                ca = new CharAssociation(s, e - s);
            } else {
                ca = new CharAssociation(mergeIntervals(ia));
            }
            return mergePredicates(ca, aa);
        }

        private static CharAssociation mergePredicates(
                final CharAssociation ca, final CharAssociation[] aa) {
            for (final CharAssociation a : aa) {
                ca.mergePredications(a);
            }
            return ca;
        }

        private static int getSubIntervalsStart(final int[] ia) {
            int us = Integer.MAX_VALUE;
            int ue = Integer.MIN_VALUE;
            if (ia != null) {
                for (int i = 0, n = ia.length; i < n; i += 2) {
                    final int s = ia[i + 0];
                    final int e = ia[i + 1];
                    if (s < us) {
                        us = s;
                    }
                    if (e > ue) {
                        ue = e;
                    }
                }
                if (ue < 0) {
                    ue = 0;
                }
                if (us > ue) {
                    us = ue;
                }
            }
            return us;
        }

        private static int getSubIntervalsLength(final int[] ia) {
            int us = Integer.MAX_VALUE;
            int ue = Integer.MIN_VALUE;
            if (ia != null) {
                for (int i = 0, n = ia.length; i < n; i += 2) {
                    final int s = ia[i + 0];
                    final int e = ia[i + 1];
                    if (s < us) {
                        us = s;
                    }
                    if (e > ue) {
                        ue = e;
                    }
                }
                if (ue < 0) {
                    ue = 0;
                }
                if (us > ue) {
                    us = ue;
                }
            }
            return ue - us;
        }

        /**
         * Extract sorted sub-intervals.
         */
        private static int[] extractIntervals(final CharAssociation[] aa) {
            int ni = 0;
            for (int i = 0, n = aa.length; i < n; i++) {
                final CharAssociation a = aa[i];
                if (a.isDisjoint()) {
                    ni += a.getSubIntervalCount();
                } else {
                    ni += 1;
                }
            }
            final int[] sa = new int[ni];
            final int[] ea = new int[ni];
            for (int i = 0, k = 0; i < aa.length; i++) {
                final CharAssociation a = aa[i];
                if (a.isDisjoint()) {
                    final int[] da = a.getSubIntervals();
                    for (int j = 0; j < da.length; j += 2) {
                        sa[k] = da[j + 0];
                        ea[k] = da[j + 1];
                        k++;
                    }
                } else {
                    sa[k] = a.getStart();
                    ea[k] = a.getEnd();
                    k++;
                }
            }
            return sortIntervals(sa, ea);
        }

        private static final int[] sortIncrements16 // CSOK: ConstantNameCheck
        = { 1391376, 463792, 198768, 86961, 33936, 13776, 4592, 1968, 861, 336,
                112, 48, 21, 7, 3, 1 };

        private static final int[] sortIncrements03 // CSOK: ConstantNameCheck
        = { 7, 3, 1 };

        /**
         * Sort sub-intervals using modified Shell Sort.
         */
        private static int[] sortIntervals(final int[] sa, final int[] ea) {
            assert sa != null;
            assert ea != null;
            assert sa.length == ea.length;
            final int ni = sa.length;
            final int[] incr = ni < 21 ? sortIncrements03 : sortIncrements16;
            for (int k = 0; k < incr.length; k++) {
                for (int h = incr[k], i = h, n = ni, j; i < n; i++) {
                    final int s1 = sa[i];
                    final int e1 = ea[i];
                    for (j = i; j >= h; j -= h) {
                        final int s2 = sa[j - h];
                        final int e2 = ea[j - h];
                        if (s2 > s1) {
                            sa[j] = s2;
                            ea[j] = e2;
                        } else if (s2 == s1 && e2 > e1) {
                            sa[j] = s2;
                            ea[j] = e2;
                        } else {
                            break;
                        }
                    }
                    sa[j] = s1;
                    ea[j] = e1;
                }
            }
            final int[] ia = new int[ni * 2];
            for (int i = 0; i < ni; i++) {
                ia[i * 2 + 0] = sa[i];
                ia[i * 2 + 1] = ea[i];
            }
            return ia;
        }

        /**
         * Merge overlapping and abutting sub-intervals.
         */
        private static int[] mergeIntervals(final int[] ia) {
            final int ni = ia.length;
            int i;
            int n;
            int nm;
            int is;
            int ie;
            // count merged sub-intervals
            for (i = 0, n = ni, nm = 0, is = ie = -1; i < n; i += 2) {
                final int s = ia[i + 0];
                final int e = ia[i + 1];
                if (ie < 0 || s > ie) {
                    is = s;
                    ie = e;
                    nm++;
                } else if (s >= is) {
                    if (e > ie) {
                        ie = e;
                    }
                }
            }
            final int[] mi = new int[nm * 2];
            // populate merged sub-intervals
            for (i = 0, n = ni, nm = 0, is = ie = -1; i < n; i += 2) {
                final int s = ia[i + 0];
                final int e = ia[i + 1];
                final int k = nm * 2;
                if (ie < 0 || s > ie) {
                    is = s;
                    ie = e;
                    mi[k + 0] = is;
                    mi[k + 1] = ie;
                    nm++;
                } else if (s >= is) {
                    if (e > ie) {
                        ie = e;
                    }
                    mi[k - 1] = ie;
                }
            }
            return mi;
        }

    }

}
