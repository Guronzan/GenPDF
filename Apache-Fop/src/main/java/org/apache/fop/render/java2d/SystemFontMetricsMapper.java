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

/* $Id: SystemFontMetricsMapper.java 1296496 2012-03-02 22:19:46Z gadams $ */

package org.apache.fop.render.java2d;

// Java
import java.util.Map;
import java.util.Set;

import org.apache.fop.fonts.FontType;
import org.apache.fop.fonts.Typeface;

/**
 * This class implements org.apache.fop.layout.FontMetrics and is added to the
 * hash table in FontInfo. It deferes the actual calculation of the metrics to
 * Java2DFontMetrics. It only keeps the java name and style as member varibles
 */

public class SystemFontMetricsMapper extends Typeface implements
        FontMetricsMapper {

    /**
     * This is a Java2DFontMetrics that does the real calculation. It is only
     * one class that dynamically determines the font-size.
     */
    private final Java2DFontMetrics java2DFontMetrics;

    /**
     * The java name of the font. # Make the family name immutable.
     */
    private final String family;

    /**
     * The java style of the font. # Make the style immutable.
     */
    private final int style;

    /**
     * Constructs a new Font-metrics.
     * 
     * @param family
     *            the family name of the font (java value)
     * @param style
     *            the java type style value of the font
     * @param java2DFontMetrics
     *            metric calculations delegated to this
     */
    public SystemFontMetricsMapper(final String family, final int style,
            final Java2DFontMetrics java2DFontMetrics) {
        this.family = family;

        this.style = style;

        this.java2DFontMetrics = java2DFontMetrics;
    }

    /** {@inheritDoc} */
    @Override
    public String getFontName() {
        return this.family;
    }

    /** {@inheritDoc} */
    @Override
    public String getEmbedFontName() {
        return getFontName();
    }

    /** {@inheritDoc} */
    @Override
    public String getFullName() {
        return getFontName();
    }

    /** {@inheritDoc} */
    @Override
    public Set getFamilyNames() {
        final Set s = new java.util.HashSet();
        s.add(this.family);
        return s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FontType getFontType() {
        return FontType.OTHER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxAscent(final int size) {
        return this.java2DFontMetrics.getMaxAscent(this.family, this.style,
                size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAscender(final int size) {
        return this.java2DFontMetrics
                .getAscender(this.family, this.style, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCapHeight(final int size) {
        return this.java2DFontMetrics.getCapHeight(this.family, this.style,
                size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDescender(final int size) {
        return this.java2DFontMetrics.getDescender(this.family, this.style,
                size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getXHeight(final int size) {
        return this.java2DFontMetrics.getXHeight(this.family, this.style, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth(final int i, final int size) {
        return this.java2DFontMetrics.width(i, this.family, this.style, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getWidths() {
        return this.java2DFontMetrics.getWidths(this.family, this.style,
                Java2DFontMetrics.FONT_SIZE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.awt.Font getFont(final int size) {
        return this.java2DFontMetrics.getFont(this.family, this.style, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getKerningInfo() {
        return java.util.Collections.EMPTY_MAP;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasKerningInfo() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getEncodingName() {
        return null; // Not applicable to Java2D rendering
    }

    /** {@inheritDoc} */
    @Override
    public char mapChar(final char c) {
        return c;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChar(final char c) {
        return this.java2DFontMetrics.hasChar(this.family, this.style,
                Java2DFontMetrics.FONT_SIZE, c);
    }

}
