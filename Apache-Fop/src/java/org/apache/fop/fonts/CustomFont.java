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

/* $Id: CustomFont.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.fonts;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;

/**
 * Abstract base class for custom fonts loaded from files, for example.
 */
public abstract class CustomFont extends Typeface implements FontDescriptor,
        MutableFont {

    private String fontName = null;
    private String fullName = null;
    private Set<String> familyNames = null;
    private String fontSubName = null;
    private String embedFileName = null;
    private String embedResourceName = null;
    private FontResolver resolver = null;
    private EmbeddingMode embeddingMode = EmbeddingMode.AUTO;

    private int capHeight = 0;
    private int xHeight = 0;
    private int ascender = 0;
    private int descender = 0;
    private int[] fontBBox = { 0, 0, 0, 0 };
    private int flags = 4;
    private int weight = 0; // 0 means unknown weight
    private int stemV = 0;
    private int italicAngle = 0;
    private int missingWidth = 0;
    private FontType fontType = FontType.TYPE1;
    private int firstChar = 0;
    private int lastChar = 255;

    private Map<Integer, Map<Integer, Integer>> kerning;

    private boolean useKerning = true;
    private boolean useAdvanced = true;

    /** the character map, mapping Unicode ranges to glyph indices. */
    protected CMapSegment[] cmap;

    /** {@inheritDoc} */
    @Override
    public String getFontName() {
        return this.fontName;
    }

    /** {@inheritDoc} */
    @Override
    public String getEmbedFontName() {
        return getFontName();
    }

    /** {@inheritDoc} */
    @Override
    public String getFullName() {
        return this.fullName;
    }

    /**
     * Returns the font family names.
     * 
     * @return the font family names (a Set of Strings)
     */
    @Override
    public Set<String> getFamilyNames() {
        return Collections.unmodifiableSet(this.familyNames);
    }

    /**
     * Returns the font family name stripped of whitespace.
     * 
     * @return the stripped font family
     * @see FontUtil#stripWhiteSpace(String)
     */
    public String getStrippedFontName() {
        return FontUtil.stripWhiteSpace(getFontName());
    }

    /**
     * Returns font's subfamily name.
     * 
     * @return the font's subfamily name
     */
    public String getFontSubName() {
        return this.fontSubName;
    }

    /**
     * Returns an URI representing an embeddable font file. The URI will often
     * be a filename or an URL.
     * 
     * @return URI to an embeddable font file or null if not available.
     */
    public String getEmbedFileName() {
        return this.embedFileName;
    }

    /**
     * Returns the embedding mode for this font.
     * 
     * @return embedding mode
     */
    public EmbeddingMode getEmbeddingMode() {
        return this.embeddingMode;
    }

    /**
     * Returns a Source representing an embeddable font file.
     * 
     * @return Source for an embeddable font file
     * @throws IOException
     *             if embedFileName is not null but Source is not found
     */
    public Source getEmbedFileSource() throws IOException {
        Source result = null;
        if (this.resolver != null && this.embedFileName != null) {
            result = this.resolver.resolve(this.embedFileName);
            if (result == null) {
                throw new IOException("Unable to resolve Source '"
                        + this.embedFileName + "' for embedded font");
            }
        }
        return result;
    }

    /**
     * Returns the lookup name to an embeddable font file available as a
     * resource. (todo) Remove this method, this should be done using a
     * resource: URI.
     * 
     * @return the lookup name
     */
    public String getEmbedResourceName() {
        return this.embedResourceName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAscender() {
        return this.ascender;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDescender() {
        return this.descender;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCapHeight() {
        return this.capHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAscender(final int size) {
        return size * this.ascender;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDescender(final int size) {
        return size * this.descender;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCapHeight(final int size) {
        return size * this.capHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getXHeight(final int size) {
        return size * this.xHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getFontBBox() {
        return this.fontBBox;
    }

    /** {@inheritDoc} */
    @Override
    public int getFlags() {
        return this.flags;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSymbolicFont() {
        return (getFlags() & 4) != 0
                || "ZapfDingbatsEncoding".equals(getEncodingName());
        // Note: The check for ZapfDingbats is necessary as the PFM does not
        // reliably indicate
        // if a font is symbolic.
    }

    /**
     * Returns the font weight (100, 200...800, 900). This value may be
     * different from the one that was actually used to register the font.
     * 
     * @return the font weight (or 0 if the font weight is unknown)
     */
    public int getWeight() {
        return this.weight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getStemV() {
        return this.stemV;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getItalicAngle() {
        return this.italicAngle;
    }

    /**
     * Returns the width to be used when no width is available.
     * 
     * @return a character width
     */
    public int getMissingWidth() {
        return this.missingWidth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FontType getFontType() {
        return this.fontType;
    }

    /**
     * Returns the index of the first character defined in this font.
     * 
     * @return the index of the first character
     */
    public int getFirstChar() {
        return this.firstChar;
    }

    /**
     * Returns the index of the last character defined in this font.
     * 
     * @return the index of the last character
     */
    public int getLastChar() {
        return this.lastChar;
    }

    /**
     * Used to determine if kerning is enabled.
     * 
     * @return True if kerning is enabled.
     */
    public boolean isKerningEnabled() {
        return this.useKerning;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean hasKerningInfo() {
        return isKerningEnabled() && this.kerning != null
                && !this.kerning.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Map<Integer, Map<Integer, Integer>> getKerningInfo() {
        if (hasKerningInfo()) {
            return this.kerning;
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Used to determine if advanced typographic features are enabled. By
     * default, this is false, but may be overridden by subclasses.
     * 
     * @return true if enabled.
     */
    public boolean isAdvancedEnabled() {
        return this.useAdvanced;
    }

    /* ---- MutableFont interface ---- */

    /** {@inheritDoc} */
    @Override
    public void setFontName(final String name) {
        this.fontName = name;
    }

    /** {@inheritDoc} */
    @Override
    public void setFullName(final String name) {
        this.fullName = name;
    }

    /** {@inheritDoc} */
    @Override
    public void setFamilyNames(final Set<String> names) {
        this.familyNames = new HashSet<String>(names);
    }

    /**
     * Sets the font's subfamily name.
     * 
     * @param subFamilyName
     *            the subfamily name of the font
     */
    public void setFontSubFamilyName(final String subFamilyName) {
        this.fontSubName = subFamilyName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEmbedFileName(final String path) {
        this.embedFileName = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEmbedResourceName(final String name) {
        this.embedResourceName = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEmbeddingMode(final EmbeddingMode embeddingMode) {
        this.embeddingMode = embeddingMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCapHeight(final int capHeight) {
        this.capHeight = capHeight;
    }

    /**
     * Returns the XHeight value of the font.
     * 
     * @param xHeight
     *            the XHeight value
     */
    public void setXHeight(final int xHeight) {
        this.xHeight = xHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAscender(final int ascender) {
        this.ascender = ascender;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDescender(final int descender) {
        this.descender = descender;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFontBBox(final int[] bbox) {
        this.fontBBox = bbox;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFlags(final int flags) {
        this.flags = flags;
    }

    /**
     * Sets the font weight. Valid values are 100, 200...800, 900.
     * 
     * @param weight
     *            the font weight
     */
    public void setWeight(int weight) {
        weight = weight / 100 * 100;
        weight = Math.max(100, weight);
        weight = Math.min(900, weight);
        this.weight = weight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStemV(final int stemV) {
        this.stemV = stemV;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setItalicAngle(final int italicAngle) {
        this.italicAngle = italicAngle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMissingWidth(final int width) {
        this.missingWidth = width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFontType(final FontType fontType) {
        this.fontType = fontType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFirstChar(final int index) {
        this.firstChar = index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLastChar(final int index) {
        this.lastChar = index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKerningEnabled(final boolean enabled) {
        this.useKerning = enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAdvancedEnabled(final boolean enabled) {
        this.useAdvanced = enabled;
    }

    /**
     * Sets the font resolver. Needed for URI resolution.
     * 
     * @param resolver
     *            the font resolver
     */
    public void setResolver(final FontResolver resolver) {
        this.resolver = resolver;
    }

    /** {@inheritDoc} */
    @Override
    public void putKerningEntry(final Integer key,
            final Map<Integer, Integer> value) {
        if (this.kerning == null) {
            this.kerning = new HashMap<Integer, Map<Integer, Integer>>();
        }
        this.kerning.put(key, value);
    }

    /**
     * Replaces the existing kerning map with a new one.
     * 
     * @param kerningMap
     *            the kerning map (Map<Integer, Map<Integer, Integer>, the
     *            integers are character codes)
     */
    public void replaceKerningMap(
            final Map<Integer, Map<Integer, Integer>> kerningMap) {
        if (kerningMap == null) {
            this.kerning = Collections.emptyMap();
        } else {
            this.kerning = kerningMap;
        }
    }

    /**
     * Sets the character map for this font. It maps all available Unicode
     * characters to their glyph indices inside the font.
     * 
     * @param cmap
     *            the character map
     */
    public void setCMap(final CMapSegment[] cmap) {
        this.cmap = new CMapSegment[cmap.length];
        System.arraycopy(cmap, 0, this.cmap, 0, cmap.length);
    }

    /**
     * Returns the character map for this font. It maps all available Unicode
     * characters to their glyph indices inside the font.
     * 
     * @return the character map
     */
    public CMapSegment[] getCMap() {
        final CMapSegment[] copy = new CMapSegment[this.cmap.length];
        System.arraycopy(this.cmap, 0, copy, 0, this.cmap.length);
        return copy;
    }

}
