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

/* $Id: WordArea.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.area.inline;

import java.util.Arrays;
import java.util.List;

import org.apache.fop.complexscripts.bidi.InlineRun;
import org.apache.fop.complexscripts.util.CharMirror;

/**
 * A string of characters without spaces
 */
public class WordArea extends InlineArea {

    private static final long serialVersionUID = 6444644662158970942L;

    /** The text for this word area */
    protected String word;

    /** An array of width for adjusting the individual letters (optional) */
    protected int[] letterAdjust;

    /**
     * An array of resolved bidirectional levels corresponding to each character
     * in word (optional)
     */
    protected int[] levels;

    /**
     * An array of glyph positioning adjustments to apply to each glyph 'char'
     * in word (optional)
     */
    protected int[][] gposAdjustments;

    /**
     * A flag indicating whether the content of word is reversed in relation to
     * its original logical order.
     */
    protected boolean reversed;

    /**
     * Create a word area
     * 
     * @param blockProgressionOffset
     *            the offset for this area
     * @param level
     *            the bidirectional embedding level (or -1 if not defined) for
     *            word as a group
     * @param word
     *            the word string
     * @param letterAdjust
     *            the letter adjust array (may be null)
     * @param levels
     *            array of per-character (glyph) bidirectional levels, in case
     *            word area is heterogenously leveled
     * @param gposAdjustments
     *            array of general position adjustments or null if none apply
     * @param reversed
     *            true if word is known to be reversed at construction time
     */
    public WordArea(final int blockProgressionOffset, final int level,
            final String word, final int[] letterAdjust, final int[] levels,
            final int[][] gposAdjustments, final boolean reversed) {
        super(blockProgressionOffset, level);
        final int length = word != null ? word.length() : 0;
        this.word = word;
        this.letterAdjust = maybeAdjustLength(letterAdjust, length);
        this.levels = maybePopulateLevels(levels, level, length);
        this.gposAdjustments = maybeAdjustLength(gposAdjustments, length);
        this.reversed = reversed;
    }

    /**
     * Create a word area
     * 
     * @param blockProgressionOffset
     *            the offset for this area
     * @param level
     *            the bidirectional embedding level (or -1 if not defined) for
     *            word as a group
     * @param word
     *            the word string
     * @param letterAdjust
     *            the letter adjust array (may be null)
     * @param levels
     *            array of per-character (glyph) bidirectional levels, in case
     *            word area is heterogenously leveled
     * @param gposAdjustments
     *            array of general position adjustments or null if none apply
     */
    public WordArea(final int blockProgressionOffset, final int level,
            final String word, final int[] letterAdjust, final int[] levels,
            final int[][] gposAdjustments) {
        this(blockProgressionOffset, level, word, letterAdjust, levels,
                gposAdjustments, false);
    }

    /** @return Returns the word. */
    public String getWord() {
        return this.word;
    }

    /** @return the array of letter adjust widths */
    public int[] getLetterAdjustArray() {
        return this.letterAdjust;
    }

    /**
     * Obtain per-character (glyph) bidi levels.
     * 
     * @return a (possibly empty) array of levels or null (if none resolved)
     */
    public int[] getBidiLevels() {
        return this.levels;
    }

    /**
     * <p>
     * Obtain per-character (glyph) bidi levels over a specified subsequence.
     * </p>
     * <p>
     * If word has been reversed, then the subsequence is over the reversed
     * word.
     * </p>
     * 
     * @param start
     *            starting (inclusive) index of subsequence
     * @param end
     *            ending (exclusive) index of subsequence
     * @return a (possibly null) array of per-character (glyph) levels over the
     *         specified sequence
     */
    public int[] getBidiLevels(final int start, final int end) {
        assert start <= end;
        if (this.levels != null) {
            final int n = end - start;
            final int[] levels = new int[n];
            for (int i = 0; i < n; i++) {
                levels[i] = this.levels[start + i];
            }
            return levels;
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Obtain per-character (glyph) level at a specified index position.
     * </p>
     * <p>
     * If word has been reversed, then the position is relative to the reversed
     * word.
     * </p>
     * 
     * @param position
     *            the index of the (possibly reversed) character from which to
     *            obtain the level
     * @return a resolved bidirectional level or, if not specified, then -1
     */
    public int bidiLevelAt(final int position) {
        if (position > this.word.length()) {
            throw new IndexOutOfBoundsException();
        } else if (this.levels != null) {
            return this.levels[position];
        } else {
            return -1;
        }
    }

    @Override
    public List collectInlineRuns(final List runs) {
        assert runs != null;
        InlineRun r;
        if (getBidiLevels() != null) {
            r = new InlineRun(this, getBidiLevels());
        } else {
            r = new InlineRun(this, -1, this.word.length());
        }
        runs.add(r);
        return runs;
    }

    /**
     * Obtain per-character (glyph) position adjustments.
     * 
     * @return a (possibly empty) array of adjustments, each having four
     *         elements, or null if no adjustments apply
     */
    public int[][] getGlyphPositionAdjustments() {
        return this.gposAdjustments;
    }

    /**
     * <p>
     * Obtain per-character (glyph) position adjustments at a specified index
     * position.
     * </p>
     * <p>
     * If word has been reversed, then the position is relative to the reversed
     * word.
     * </p>
     * 
     * @param position
     *            the index of the (possibly reversed) character from which to
     *            obtain the level
     * @return an array of adjustments or null if none applies
     */
    public int[] glyphPositionAdjustmentsAt(final int position) {
        if (position > this.word.length()) {
            throw new IndexOutOfBoundsException();
        } else if (this.gposAdjustments != null) {
            return this.gposAdjustments[position];
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Reverse characters and corresponding per-character levels and glyph
     * position adjustments.
     * </p>
     * 
     * @param mirror
     *            if true, then perform mirroring if mirrorred characters
     */
    public void reverse(final boolean mirror) {
        if (this.word.length() > 0) {
            this.word = new StringBuffer(this.word).reverse().toString();
            if (this.levels != null) {
                reverse(this.levels);
            }
            if (this.gposAdjustments != null) {
                reverse(this.gposAdjustments);
            }
            this.reversed = !this.reversed;
            if (mirror) {
                this.word = CharMirror.mirror(this.word);
            }
        }
    }

    /**
     * <p>
     * Perform mirroring on mirrorable characters.
     * </p>
     */
    public void mirror() {
        if (this.word.length() > 0) {
            this.word = CharMirror.mirror(this.word);
        }
    }

    /**
     * <p>
     * Determined if word has been reversed (in relation to original logical
     * order).
     * </p>
     * <p>
     * If a word is reversed, then both its characters (glyphs) and
     * corresponding per-character levels are in reverse order.
     * </p>
     * <p>
     * Note: this information is used in order to process non-spacing marks
     * during rendering as well as provide hints for caret direction.
     * </p>
     * 
     * @return true if word is reversed
     */
    public boolean isReversed() {
        return this.reversed;
    }

    /*
     * If int[] array is not of specified length, then create a new copy of the
     * first length entries.
     */
    private static int[] maybeAdjustLength(final int[] ia, final int length) {
        if (ia != null) {
            if (ia.length == length) {
                return ia;
            } else {
                final int[] iaNew = new int[length];
                for (int i = 0, n = ia.length; i < n; i++) {
                    if (i < length) {
                        iaNew[i] = ia[i];
                    } else {
                        break;
                    }
                }
                return iaNew;
            }
        } else {
            return ia;
        }
    }

    /*
     * If int[][] matrix is not of specified length, then create a new shallow
     * copy of the first length entries.
     */
    private static int[][] maybeAdjustLength(final int[][] im, final int length) {
        if (im != null) {
            if (im.length == length) {
                return im;
            } else {
                final int[][] imNew = new int[length][];
                for (int i = 0, n = im.length; i < n; i++) {
                    if (i < length) {
                        imNew[i] = im[i];
                    } else {
                        break;
                    }
                }
                return imNew;
            }
        } else {
            return im;
        }
    }

    private static int[] maybePopulateLevels(int[] levels, final int level,
            final int count) {
        if (levels == null && level >= 0) {
            levels = new int[count];
            Arrays.fill(levels, level);
        }
        return maybeAdjustLength(levels, count);
    }

    private static void reverse(final int[] a) {
        for (int i = 0, n = a.length, m = n / 2; i < m; i++) {
            final int k = n - i - 1;
            final int t = a[k];
            a[k] = a[i];
            a[i] = t;
        }
    }

    private static void reverse(final int[][] aa) {
        for (int i = 0, n = aa.length, m = n / 2; i < m; i++) {
            final int k = n - i - 1;
            final int[] t = aa[k];
            aa[k] = aa[i];
            aa[i] = t;
        }
    }

}
