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

/* $Id: LazyFont.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.fonts;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.complexscripts.fonts.Positionable;
import org.apache.fop.complexscripts.fonts.Substitutable;
import org.xml.sax.InputSource;

/**
 * This class is used to defer the loading of a font until it is really used.
 */
@Slf4j
public class LazyFont extends Typeface implements FontDescriptor,
Substitutable, Positionable {

    private final String metricsFileName;
    private final String fontEmbedPath;
    private final boolean useKerning;
    private boolean useAdvanced;
    private EncodingMode encodingMode = EncodingMode.AUTO;
    private EmbeddingMode embeddingMode = EmbeddingMode.AUTO;
    private boolean embedded = true;
    private final String subFontName;

    private boolean isMetricsLoaded;
    private Typeface realFont;
    private FontDescriptor realFontDescriptor;

    private final FontResolver resolver;

    /**
     * Main constructor
     *
     * @param fontInfo
     *            the font info to embed
     * @param resolver
     *            the font resolver to handle font URIs
     */
    public LazyFont(final EmbedFontInfo fontInfo, final FontResolver resolver) {

        this.metricsFileName = fontInfo.getMetricsFile();
        this.fontEmbedPath = fontInfo.getEmbedFile();
        this.useKerning = fontInfo.getKerning();
        if (resolver != null) {
            this.useAdvanced = resolver.isComplexScriptFeaturesEnabled();
        } else {
            this.useAdvanced = fontInfo.getAdvanced();
        }
        this.encodingMode = fontInfo.getEncodingMode();
        this.embeddingMode = fontInfo.getEmbeddingMode();
        this.subFontName = fontInfo.getSubFontName();
        this.embedded = fontInfo.isEmbedded();
        this.resolver = resolver;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sbuf = new StringBuilder(super.toString());
        sbuf.append('{');
        sbuf.append("metrics-url=" + this.metricsFileName);
        sbuf.append(",embed-url=" + this.fontEmbedPath);
        sbuf.append(",kerning=" + this.useKerning);
        sbuf.append(",advanced=" + this.useAdvanced);
        sbuf.append('}');
        return sbuf.toString();
    }

    private void load(final boolean fail) {
        if (!this.isMetricsLoaded) {
            try {
                if (this.metricsFileName != null) {
                    /** @todo Possible thread problem here */
                    FontReader reader = null;
                    if (this.resolver != null) {
                        final Source source = this.resolver
                                .resolve(this.metricsFileName);
                        if (source == null) {
                            final String err = "Cannot load font: failed to create Source from metrics file "
                                    + this.metricsFileName;
                            if (fail) {
                                throw new RuntimeException(err);
                            } else {
                                log.error(err);
                            }
                            return;
                        }
                        InputStream in = null;
                        if (source instanceof StreamSource) {
                            in = ((StreamSource) source).getInputStream();
                        }
                        if (in == null && source.getSystemId() != null) {
                            in = new java.net.URL(source.getSystemId())
                            .openStream();
                        }
                        if (in == null) {
                            final String err = "Cannot load font: After URI resolution, the returned"
                                    + " Source object does not contain an InputStream"
                                    + " or a valid URL (system identifier) for metrics file: "
                                    + this.metricsFileName;
                            if (fail) {
                                throw new RuntimeException(err);
                            } else {
                                log.error(err);
                            }
                            return;
                        }
                        final InputSource src = new InputSource(in);
                        src.setSystemId(source.getSystemId());
                        reader = new FontReader(src);
                    } else {
                        reader = new FontReader(new InputSource(new URL(
                                this.metricsFileName).openStream()));
                    }
                    reader.setKerningEnabled(this.useKerning);
                    reader.setAdvancedEnabled(this.useAdvanced);
                    if (this.embedded) {
                        reader.setFontEmbedPath(this.fontEmbedPath);
                    }
                    reader.setResolver(this.resolver);
                    this.realFont = reader.getFont();
                } else {
                    if (this.fontEmbedPath == null) {
                        throw new RuntimeException(
                                "Cannot load font. No font URIs available.");
                    }
                    this.realFont = FontLoader.loadFont(this.fontEmbedPath,
                            this.subFontName, this.embedded,
                            this.embeddingMode, this.encodingMode,
                            this.useKerning, this.useAdvanced, this.resolver);
                }
                if (this.realFont instanceof FontDescriptor) {
                    this.realFontDescriptor = (FontDescriptor) this.realFont;
                }
            } catch (final FOPException fopex) {
                log.error("Failed to read font metrics file "
                        + this.metricsFileName, fopex);
                if (fail) {
                    throw new RuntimeException(fopex.getMessage());
                }
            } catch (final IOException ioex) {
                log.error("Failed to read font metrics file "
                        + this.metricsFileName, ioex);
                if (fail) {
                    throw new RuntimeException(ioex.getMessage());
                }
            }
            this.realFont.setEventListener(this.eventListener);
            this.isMetricsLoaded = true;
        }
    }

    /**
     * Gets the real font.
     *
     * @return the real font
     */
    public Typeface getRealFont() {
        load(false);
        return this.realFont;
    }

    // ---- Font ----
    /** {@inheritDoc} */
    @Override
    public String getEncodingName() {
        load(true);
        return this.realFont.getEncodingName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char mapChar(final char c) {
        load(true);
        return this.realFont.mapChar(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hadMappingOperations() {
        load(true);
        return this.realFont.hadMappingOperations();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChar(final char c) {
        load(true);
        return this.realFont.hasChar(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultiByte() {
        load(true);
        return this.realFont.isMultiByte();
    }

    // ---- FontMetrics interface ----
    /** {@inheritDoc} */
    @Override
    public String getFontName() {
        load(true);
        return this.realFont.getFontName();
    }

    /** {@inheritDoc} */
    @Override
    public String getEmbedFontName() {
        load(true);
        return this.realFont.getEmbedFontName();
    }

    /** {@inheritDoc} */
    @Override
    public String getFullName() {
        load(true);
        return this.realFont.getFullName();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getFamilyNames() {
        load(true);
        return this.realFont.getFamilyNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxAscent(final int size) {
        load(true);
        return this.realFont.getMaxAscent(size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAscender(final int size) {
        load(true);
        return this.realFont.getAscender(size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCapHeight(final int size) {
        load(true);
        return this.realFont.getCapHeight(size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDescender(final int size) {
        load(true);
        return this.realFont.getDescender(size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getXHeight(final int size) {
        load(true);
        return this.realFont.getXHeight(size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth(final int i, final int size) {
        load(true);
        return this.realFont.getWidth(i, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getWidths() {
        load(true);
        return this.realFont.getWidths();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasKerningInfo() {
        load(true);
        return this.realFont.hasKerningInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, Map<Integer, Integer>> getKerningInfo() {
        load(true);
        return this.realFont.getKerningInfo();
    }

    // ---- FontDescriptor interface ----
    /**
     * {@inheritDoc}
     */
    @Override
    public int getCapHeight() {
        load(true);
        return this.realFontDescriptor.getCapHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDescender() {
        load(true);
        return this.realFontDescriptor.getDescender();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAscender() {
        load(true);
        return this.realFontDescriptor.getAscender();
    }

    /** {@inheritDoc} */
    @Override
    public int getFlags() {
        load(true);
        return this.realFontDescriptor.getFlags();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSymbolicFont() {
        load(true);
        return this.realFontDescriptor.isSymbolicFont();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getFontBBox() {
        load(true);
        return this.realFontDescriptor.getFontBBox();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getItalicAngle() {
        load(true);
        return this.realFontDescriptor.getItalicAngle();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getStemV() {
        load(true);
        return this.realFontDescriptor.getStemV();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FontType getFontType() {
        load(true);
        return this.realFontDescriptor.getFontType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmbeddable() {
        load(true);
        return this.realFontDescriptor.isEmbeddable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performsSubstitution() {
        load(true);
        if (this.realFontDescriptor instanceof Substitutable) {
            return ((Substitutable) this.realFontDescriptor)
                    .performsSubstitution();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence performSubstitution(final CharSequence cs,
            final String script, final String language) {
        load(true);
        if (this.realFontDescriptor instanceof Substitutable) {
            return ((Substitutable) this.realFontDescriptor)
                    .performSubstitution(cs, script, language);
        } else {
            return cs;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence reorderCombiningMarks(final CharSequence cs,
            final int[][] gpa, final String script, final String language) {
        load(true);
        if (this.realFontDescriptor instanceof Substitutable) {
            return ((Substitutable) this.realFontDescriptor)
                    .reorderCombiningMarks(cs, gpa, script, language);
        } else {
            return cs;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performsPositioning() {
        load(true);
        if (this.realFontDescriptor instanceof Positionable) {
            return ((Positionable) this.realFontDescriptor)
                    .performsPositioning();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[][] performPositioning(final CharSequence cs,
            final String script, final String language, final int fontSize) {
        load(true);
        if (this.realFontDescriptor instanceof Positionable) {
            return ((Positionable) this.realFontDescriptor).performPositioning(
                    cs, script, language, fontSize);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[][] performPositioning(final CharSequence cs,
            final String script, final String language) {
        load(true);
        if (this.realFontDescriptor instanceof Positionable) {
            return ((Positionable) this.realFontDescriptor).performPositioning(
                    cs, script, language);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubsetEmbedded() {
        load(true);
        return this.realFont.isMultiByte();
    }

}
