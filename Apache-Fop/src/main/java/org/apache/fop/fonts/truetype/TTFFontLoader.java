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

/* $Id: TTFFontLoader.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.fonts.truetype;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.fop.fonts.CIDFontType;
import org.apache.fop.fonts.CMapSegment;
import org.apache.fop.fonts.EmbeddingMode;
import org.apache.fop.fonts.EncodingMode;
import org.apache.fop.fonts.FontLoader;
import org.apache.fop.fonts.FontResolver;
import org.apache.fop.fonts.FontType;
import org.apache.fop.fonts.MultiByteFont;
import org.apache.fop.fonts.NamedCharacter;
import org.apache.fop.fonts.SingleByteFont;
import org.apache.fop.fonts.truetype.TTFFile.PostScriptVersion;
import org.apache.fop.util.HexEncoder;

/**
 * Loads a TrueType font into memory directly from the original font file.
 */
public class TTFFontLoader extends FontLoader {

    private MultiByteFont multiFont;
    private SingleByteFont singleFont;
    private final String subFontName;
    private EncodingMode encodingMode;
    private EmbeddingMode embeddingMode;

    /**
     * Default constructor
     * 
     * @param fontFileURI
     *            the URI representing the font file
     * @param resolver
     *            the FontResolver for font URI resolution
     */
    public TTFFontLoader(final String fontFileURI, final FontResolver resolver) {
        this(fontFileURI, null, true, EmbeddingMode.AUTO, EncodingMode.AUTO,
                true, true, resolver);
    }

    /**
     * Additional constructor for TrueType Collections.
     * 
     * @param fontFileURI
     *            the URI representing the font file
     * @param subFontName
     *            the sub-fontname of a font in a TrueType Collection (or null
     *            for normal TrueType fonts)
     * @param embedded
     *            indicates whether the font is embedded or referenced
     * @param embeddingMode
     *            the embedding mode of the font
     * @param encodingMode
     *            the requested encoding mode
     * @param useKerning
     *            true to enable loading kerning info if available, false to
     *            disable
     * @param useAdvanced
     *            true to enable loading advanced info if available, false to
     *            disable
     * @param resolver
     *            the FontResolver for font URI resolution
     */
    public TTFFontLoader(final String fontFileURI, final String subFontName,
            final boolean embedded, final EmbeddingMode embeddingMode,
            final EncodingMode encodingMode, final boolean useKerning,
            final boolean useAdvanced, final FontResolver resolver) {
        super(fontFileURI, embedded, useKerning, useAdvanced, resolver);
        this.subFontName = subFontName;
        this.encodingMode = encodingMode;
        this.embeddingMode = embeddingMode;
        if (this.encodingMode == EncodingMode.AUTO) {
            this.encodingMode = EncodingMode.CID; // Default to CID mode for
                                                  // TrueType
        }
        if (this.embeddingMode == EmbeddingMode.AUTO) {
            this.embeddingMode = EmbeddingMode.SUBSET;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void read() throws IOException {
        read(this.subFontName);
    }

    /**
     * Reads a TrueType font.
     * 
     * @param ttcFontName
     *            the TrueType sub-font name of TrueType Collection (may be null
     *            for normal TrueType fonts)
     * @throws IOException
     *             if an I/O error occurs
     */
    private void read(final String ttcFontName) throws IOException {
        final InputStream in = openFontUri(this.resolver, this.fontFileURI);
        try {
            final TTFFile ttf = new TTFFile(this.useKerning, this.useAdvanced);
            final FontFileReader reader = new FontFileReader(in);
            final boolean supported = ttf.readFont(reader, ttcFontName);
            if (!supported) {
                throw new IOException("TrueType font is not supported: "
                        + this.fontFileURI);
            }
            buildFont(ttf, ttcFontName);
            this.loaded = true;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void buildFont(final TTFFile ttf, final String ttcFontName) {
        if (ttf.isCFF()) {
            throw new UnsupportedOperationException(
                    "OpenType fonts with CFF data are not supported, yet");
        }

        boolean isCid = this.embedded;
        if (this.encodingMode == EncodingMode.SINGLE_BYTE) {
            isCid = false;
        }

        if (isCid) {
            this.multiFont = new MultiByteFont();
            this.returnFont = this.multiFont;
            this.multiFont.setTTCName(ttcFontName);
        } else {
            this.singleFont = new SingleByteFont();
            this.returnFont = this.singleFont;
        }
        this.returnFont.setResolver(this.resolver);

        this.returnFont.setFontName(ttf.getPostScriptName());
        this.returnFont.setFullName(ttf.getFullName());
        this.returnFont.setFamilyNames(ttf.getFamilyNames());
        this.returnFont.setFontSubFamilyName(ttf.getSubFamilyName());
        this.returnFont.setCapHeight(ttf.getCapHeight());
        this.returnFont.setXHeight(ttf.getXHeight());
        this.returnFont.setAscender(ttf.getLowerCaseAscent());
        this.returnFont.setDescender(ttf.getLowerCaseDescent());
        this.returnFont.setFontBBox(ttf.getFontBBox());
        this.returnFont.setFlags(ttf.getFlags());
        this.returnFont.setStemV(Integer.parseInt(ttf.getStemV())); // not used
                                                                    // for TTF
        this.returnFont.setItalicAngle(Integer.parseInt(ttf.getItalicAngle()));
        this.returnFont.setMissingWidth(0);
        this.returnFont.setWeight(ttf.getWeightClass());
        this.returnFont.setEmbeddingMode(this.embeddingMode);
        if (isCid) {
            this.multiFont.setCIDType(CIDFontType.CIDTYPE2);
            final int[] wx = ttf.getWidths();
            this.multiFont.setWidthArray(wx);
        } else {
            this.singleFont.setFontType(FontType.TRUETYPE);
            this.singleFont.setEncoding(ttf.getCharSetName());
            this.returnFont.setFirstChar(ttf.getFirstChar());
            this.returnFont.setLastChar(ttf.getLastChar());
            this.singleFont.setTrueTypePostScriptVersion(ttf
                    .getPostScriptVersion());
            copyWidthsSingleByte(ttf);
        }
        this.returnFont.setCMap(getCMap(ttf));

        if (this.useKerning) {
            copyKerning(ttf, isCid);
        }
        if (this.useAdvanced) {
            copyAdvanced(ttf);
        }
        if (this.embedded) {
            if (ttf.isEmbeddable()) {
                this.returnFont.setEmbedFileName(this.fontFileURI);
            } else {
                final String msg = "The font " + this.fontFileURI
                        + " is not embeddable due to a"
                        + " licensing restriction.";
                throw new RuntimeException(msg);
            }
        }
    }

    private CMapSegment[] getCMap(final TTFFile ttf) {
        final CMapSegment[] array = new CMapSegment[ttf.getCMaps().size()];
        return ttf.getCMaps().toArray(array);
    }

    private void copyWidthsSingleByte(final TTFFile ttf) {
        final int[] wx = ttf.getWidths();
        for (int i = this.singleFont.getFirstChar(); i <= this.singleFont
                .getLastChar(); i++) {
            this.singleFont.setWidth(i, ttf.getCharWidth(i));
        }

        for (final CMapSegment segment : ttf.getCMaps()) {
            if (segment.getUnicodeStart() < 0xFFFE) {
                for (char u = (char) segment.getUnicodeStart(); u <= segment
                        .getUnicodeEnd(); u++) {
                    final int codePoint = this.singleFont.getEncoding()
                            .mapChar(u);
                    if (codePoint <= 0) {
                        final int glyphIndex = segment.getGlyphStartIndex() + u
                                - segment.getUnicodeStart();
                        String glyphName = ttf.getGlyphName(glyphIndex);
                        if (glyphName.length() == 0
                                && ttf.getPostScriptVersion() != PostScriptVersion.V2) {
                            glyphName = "u" + HexEncoder.encode(u);
                        }
                        if (glyphName.length() > 0) {
                            final String unicode = Character.toString(u);
                            final NamedCharacter nc = new NamedCharacter(
                                    glyphName, unicode);
                            this.singleFont.addUnencodedCharacter(nc,
                                    wx[glyphIndex]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Copy kerning information.
     */
    private void copyKerning(final TTFFile ttf, final boolean isCid) {

        // Get kerning
        Set<Integer> kerningSet;
        if (isCid) {
            kerningSet = ttf.getKerning().keySet();
        } else {
            kerningSet = ttf.getAnsiKerning().keySet();
        }

        for (final Integer kpx1 : kerningSet) {
            Map<Integer, Integer> h2;
            if (isCid) {
                h2 = ttf.getKerning().get(kpx1);
            } else {
                h2 = ttf.getAnsiKerning().get(kpx1);
            }
            this.returnFont.putKerningEntry(kpx1, h2);
        }
    }

    /**
     * Copy advanced typographic information.
     */
    private void copyAdvanced(final TTFFile ttf) {
        if (this.returnFont instanceof MultiByteFont) {
            final MultiByteFont mbf = (MultiByteFont) this.returnFont;
            mbf.setGDEF(ttf.getGDEF());
            mbf.setGSUB(ttf.getGSUB());
            mbf.setGPOS(ttf.getGPOS());
        }
    }

}
