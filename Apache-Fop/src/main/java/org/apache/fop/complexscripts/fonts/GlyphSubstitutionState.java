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

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.fop.complexscripts.util.GlyphSequence;
import org.apache.fop.complexscripts.util.ScriptContextTester;

// CSOFF: LineLengthCheck
// CSOFF: NoWhitespaceAfterCheck

/**
 * <p>
 * The <code>GlyphSubstitutionState</code> implements an state object used
 * during glyph substitution processing.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */

public class GlyphSubstitutionState extends GlyphProcessingState {

    /** alternates index */
    private int[] alternatesIndex;
    /** current output glyph sequence */
    private IntBuffer ogb;
    /** current output glyph to character associations */
    private final List oal;
    /** character association predications */
    private boolean predications;

    /**
     * Construct glyph substitution state.
     * 
     * @param gs
     *            input glyph sequence
     * @param script
     *            script identifier
     * @param language
     *            language identifier
     * @param feature
     *            feature identifier
     * @param sct
     *            script context tester (or null)
     */
    public GlyphSubstitutionState(final GlyphSequence gs, final String script,
            final String language, final String feature,
            final ScriptContextTester sct) {
        super(gs, script, language, feature, sct);
        this.ogb = IntBuffer.allocate(gs.getGlyphCount());
        this.oal = new ArrayList(gs.getGlyphCount());
        this.predications = gs.getPredications();
    }

    /**
     * Construct glyph substitution state using an existing state object using
     * shallow copy except as follows: input glyph sequence is copied deep
     * except for its characters array.
     * 
     * @param ss
     *            existing positioning state to copy from
     */
    public GlyphSubstitutionState(final GlyphSubstitutionState ss) {
        super(ss);
        this.ogb = IntBuffer.allocate(this.indexLast);
        this.oal = new ArrayList(this.indexLast);
    }

    /**
     * Set alternates indices.
     * 
     * @param alternates
     *            array of alternates indices ordered by coverage index
     */
    public void setAlternates(final int[] alternates) {
        this.alternatesIndex = alternates;
    }

    /**
     * Obtain alternates index associated with specified coverage index. An
     * alternates index is used to select among stylistic alternates of a glyph
     * at a particular coverage index. This information must be provided by the
     * document itself (in the form of an extension attribute value), since a
     * font has no way to determine which alternate the user desires.
     * 
     * @param ci
     *            coverage index
     * @return an alternates index
     */
    public int getAlternatesIndex(final int ci) {
        if (this.alternatesIndex == null) {
            return 0;
        } else if (ci < 0 || ci > this.alternatesIndex.length) {
            return 0;
        } else {
            return this.alternatesIndex[ci];
        }
    }

    /**
     * Put (write) glyph into glyph output buffer.
     * 
     * @param glyph
     *            to write
     * @param a
     *            character association that applies to glyph
     * @param predication
     *            a predication value to add to association A if predications
     *            enabled
     */
    public void putGlyph(final int glyph,
            final GlyphSequence.CharAssociation a, final Object predication) {
        if (!this.ogb.hasRemaining()) {
            this.ogb = growBuffer(this.ogb);
        }
        this.ogb.put(glyph);
        if (this.predications && predication != null) {
            a.setPredication(this.feature, predication);
        }
        this.oal.add(a);
    }

    /**
     * Put (write) array of glyphs into glyph output buffer.
     * 
     * @param glyphs
     *            to write
     * @param associations
     *            array of character associations that apply to glyphs
     * @param predication
     *            optional predicaion object to be associated with glyphs'
     *            associations
     */
    public void putGlyphs(final int[] glyphs,
            final GlyphSequence.CharAssociation[] associations,
            final Object predication) {
        assert glyphs != null;
        assert associations != null;
        assert associations.length >= glyphs.length;
        for (int i = 0, n = glyphs.length; i < n; i++) {
            putGlyph(glyphs[i], associations[i], predication);
        }
    }

    /**
     * Obtain output glyph sequence.
     * 
     * @return newly constructed glyph sequence comprised of original
     *         characters, output glyphs, and output associations
     */
    public GlyphSequence getOutput() {
        final int position = this.ogb.position();
        if (position > 0) {
            this.ogb.limit(position);
            this.ogb.rewind();
            return new GlyphSequence(this.igs.getCharacters(), this.ogb,
                    this.oal);
        } else {
            return this.igs;
        }
    }

    /**
     * Apply substitution subtable to current state at current position (only),
     * resulting in the consumption of zero or more input glyphs, and possibly
     * replacing the current input glyphs starting at the current position, in
     * which case it is possible that indexLast is altered to be either less
     * than or greater than its value prior to this application.
     * 
     * @param st
     *            the glyph substitution subtable to apply
     * @return true if subtable applied, or false if it did not (e.g., its input
     *         coverage table did not match current input context)
     */
    public boolean apply(final GlyphSubstitutionSubtable st) {
        assert st != null;
        updateSubtableState(st);
        final boolean applied = st.substitute(this);
        resetSubtableState();
        return applied;
    }

    /**
     * Apply a sequence of matched rule lookups to the <code>nig</code> input
     * glyphs starting at the current position. If lookups are non-null and
     * non-empty, then all input glyphs specified by <code>nig</code> are
     * consumed irregardless of whether any specified lookup applied.
     * 
     * @param lookups
     *            array of matched lookups (or null)
     * @param nig
     *            number of glyphs in input sequence, starting at current
     *            position, to which the lookups are to apply, and to be
     *            consumed once the application has finished
     * @return true if lookups are non-null and non-empty; otherwise, false
     */
    public boolean apply(final GlyphTable.RuleLookup[] lookups, final int nig) {
        // int nbg = index;
        final int nlg = this.indexLast - (this.index + nig);
        int nog = 0;
        if (lookups != null && lookups.length > 0) {
            // apply each rule lookup to extracted input glyph array
            for (int i = 0, n = lookups.length; i < n; i++) {
                final GlyphTable.RuleLookup l = lookups[i];
                if (l != null) {
                    final GlyphTable.LookupTable lt = l.getLookup();
                    if (lt != null) {
                        // perform substitution on a copy of previous state
                        final GlyphSubstitutionState ss = new GlyphSubstitutionState(
                                this);
                        // apply lookup table substitutions
                        final GlyphSequence gs = lt.substitute(ss,
                                l.getSequenceIndex());
                        // replace current input sequence starting at current
                        // position with result
                        if (replaceInput(0, -1, gs)) {
                            nog = gs.getGlyphCount() - nlg;
                        }
                    }
                }
            }
            // output glyphs and associations
            putGlyphs(getGlyphs(0, nog, false, null, null, null),
                    getAssociations(0, nog, false, null, null, null), null);
            // consume replaced input glyphs
            consume(nog);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Apply default application semantices; namely, consume one input glyph,
     * writing that glyph (and its association) to the output glyphs (and
     * associations).
     */
    @Override
    public void applyDefault() {
        super.applyDefault();
        final int gi = getGlyph();
        if (gi != 65535) {
            putGlyph(gi, getAssociation(), null);
        }
    }

    private static IntBuffer growBuffer(final IntBuffer ib) {
        final int capacity = ib.capacity();
        final int capacityNew = capacity * 2;
        final IntBuffer ibNew = IntBuffer.allocate(capacityNew);
        ib.rewind();
        return ibNew.put(ib);
    }

}
