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

/* $Id: NativeTextPainter.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.svg;

import java.awt.Graphics2D;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.gvt.renderer.StrokingTextPainter;
import org.apache.batik.gvt.text.TextSpanLayout;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.util.CharUtilities;

/**
 * Abstract base class for text painters that use specialized text commands
 * native to an output format to render text.
 */
@Slf4j
public abstract class NativeTextPainter extends StrokingTextPainter {

    /** the font collection */
    protected final FontInfo fontInfo;

    /**
     * Creates a new instance.
     *
     * @param fontInfo
     *            the font collection
     */
    public NativeTextPainter(final FontInfo fontInfo) {
        this.fontInfo = fontInfo;
    }

    /**
     * Indicates whether the given {@link Graphics2D} instance if compatible
     * with this text painter implementation.
     *
     * @param g2d
     *            the instance to check
     * @return true if the instance is compatible.
     */
    protected abstract boolean isSupported(final Graphics2D g2d);

    /**
     * Paints a single text run.
     *
     * @param textRun
     *            the text run
     * @param g2d
     *            the target Graphics2D instance
     * @throws IOException
     *             if an I/O error occurs while rendering the text
     */
    protected abstract void paintTextRun(final TextRun textRun,
            final Graphics2D g2d) throws IOException;

    /** {@inheritDoc} */
    @Override
    protected void paintTextRuns(final List textRuns, final Graphics2D g2d) {
        if (log.isTraceEnabled()) {
            log.trace("paintTextRuns: count = " + textRuns.size());
        }
        if (!isSupported(g2d)) {
            super.paintTextRuns(textRuns, g2d);
            return;
        }
        for (int i = 0; i < textRuns.size(); i++) {
            final TextRun textRun = (TextRun) textRuns.get(i);
            try {
                paintTextRun(textRun, g2d);
            } catch (final IOException ioe) {
                // No other possibility than to use a RuntimeException
                throw new RuntimeException(ioe);
            }
        }
    }

    /**
     * Finds an array of suitable fonts for a given AttributedCharacterIterator.
     *
     * @param aci
     *            the character iterator
     * @return the array of fonts
     */
    protected Font[] findFonts(final AttributedCharacterIterator aci) {
        final Font[] fonts = ACIUtils.findFontsForBatikACI(aci, this.fontInfo);
        return fonts;
    }

    /**
     * Collects all characters from an {@link AttributedCharacterIterator}.
     *
     * @param runaci
     *            the character iterator
     * @return the characters
     */
    protected CharSequence collectCharacters(
            final AttributedCharacterIterator runaci) {
        final StringBuffer chars = new StringBuffer();
        for (runaci.first(); runaci.getIndex() < runaci.getEndIndex();) {
            chars.append(runaci.current());
            runaci.next();
        }
        return chars;
    }

    /**
     * @param runaci
     *            an attributed character iterator
     * @param layout
     *            a text span layout
     */
    protected final void logTextRun(final AttributedCharacterIterator runaci,
            final TextSpanLayout layout) {
        if (log.isTraceEnabled()) {
            final int charCount = runaci.getEndIndex() - runaci.getBeginIndex();
            log.trace("================================================");
            log.trace("New text run:");
            log.trace("char count: " + charCount);
            log.trace("range: " + runaci.getBeginIndex() + " - "
                    + runaci.getEndIndex());
            log.trace("glyph count: " + layout.getGlyphCount()); // =getNumGlyphs()
        }
    }

    /**
     * @param ch
     *            a character
     * @param layout
     *            a text span layout
     * @param index
     *            an index
     * @param visibleChar
     *            visible character flag
     */
    protected final void logCharacter(final char ch,
            final TextSpanLayout layout, final int index,
            final boolean visibleChar) {
        if (log.isTraceEnabled()) {
            log.trace("glyph " + index + " -> " + layout.getGlyphIndex(index)
                    + " => " + ch);
            if (CharUtilities.isAnySpace(ch) && ch != 32) {
                log.trace("Space found: " + Integer.toHexString(ch));
            } else if (ch == CharUtilities.ZERO_WIDTH_JOINER) {
                log.trace("ZWJ found: " + Integer.toHexString(ch));
            } else if (ch == CharUtilities.SOFT_HYPHEN) {
                log.trace("Soft hyphen found: " + Integer.toHexString(ch));
            }
            if (!visibleChar) {
                log.trace("Invisible glyph found: " + Integer.toHexString(ch));
            }
        }
    }

}
