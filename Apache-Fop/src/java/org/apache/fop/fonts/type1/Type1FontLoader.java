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

/* $Id: Type1FontLoader.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.fonts.type1;

import java.awt.geom.RectangularShape;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.fop.fonts.CodePointMapping;
import org.apache.fop.fonts.FontLoader;
import org.apache.fop.fonts.FontResolver;
import org.apache.fop.fonts.FontType;
import org.apache.fop.fonts.SingleByteEncoding;
import org.apache.fop.fonts.SingleByteFont;

/**
 * Loads a Type 1 font into memory directly from the original font file.
 */
@Slf4j
public class Type1FontLoader extends FontLoader {

    private SingleByteFont singleFont;

    /**
     * Constructs a new Type 1 font loader.
     *
     * @param fontFileURI
     *            the URI to the PFB file of a Type 1 font
     * @param embedded
     *            indicates whether the font is embedded or referenced
     * @param useKerning
     *            indicates whether to load kerning information if available
     * @param resolver
     *            the font resolver used to resolve URIs
     * @throws IOException
     *             In case of an I/O error
     */
    public Type1FontLoader(final String fontFileURI, final boolean embedded,
            final boolean useKerning, final FontResolver resolver)
                    throws IOException {
        super(fontFileURI, embedded, useKerning, true, resolver);
    }

    private String getPFMURI(final String pfbURI) {
        final String pfbExt = pfbURI.substring(pfbURI.length() - 3,
                pfbURI.length());
        final String pfmExt = pfbExt.substring(0, 2)
                + (Character.isUpperCase(pfbExt.charAt(2)) ? "M" : "m");
        return pfbURI.substring(0, pfbURI.length() - 4) + "." + pfmExt;
    }

    private static final String[] AFM_EXTENSIONS = new String[] { ".AFM",
        ".afm", ".Afm" };

    /** {@inheritDoc} */
    @Override
    protected void read() throws IOException {
        AFMFile afm = null;
        PFMFile pfm = null;

        InputStream afmIn = null;
        String afmUri = null;
        for (final String element : AFM_EXTENSIONS) {
            try {
                afmUri = this.fontFileURI.substring(0,
                        this.fontFileURI.length() - 4)
                        + element;
                afmIn = openFontUri(this.resolver, afmUri);
                if (afmIn != null) {
                    break;
                }
            } catch (final IOException ioe) {
                // Ignore, AFM probably not available under the URI
            }
        }
        if (afmIn != null) {
            try {
                final AFMParser afmParser = new AFMParser();
                afm = afmParser.parse(afmIn, afmUri);
            } finally {
                IOUtils.closeQuietly(afmIn);
            }
        }

        final String pfmUri = getPFMURI(this.fontFileURI);
        InputStream pfmIn = null;
        try {
            pfmIn = openFontUri(this.resolver, pfmUri);
        } catch (final IOException ioe) {
            // Ignore, PFM probably not available under the URI
        }
        if (pfmIn != null) {
            try {
                pfm = new PFMFile();
                pfm.load(pfmIn);
            } catch (final IOException ioe) {
                if (afm == null) {
                    // Ignore the exception if we have a valid PFM. PFM is only
                    // the fallback.
                    throw ioe;
                }
            } finally {
                IOUtils.closeQuietly(pfmIn);
            }
        }

        if (afm == null && pfm == null) {
            throw new java.io.FileNotFoundException(
                    "Neither an AFM nor a PFM file was found for "
                            + this.fontFileURI);
        }
        buildFont(afm, pfm);
        this.loaded = true;
    }

    private void buildFont(final AFMFile afm, final PFMFile pfm) {
        if (afm == null && pfm == null) {
            throw new IllegalArgumentException("Need at least an AFM or a PFM!");
        }
        this.singleFont = new SingleByteFont();
        this.singleFont.setFontType(FontType.TYPE1);
        this.singleFont.setResolver(this.resolver);
        if (this.embedded) {
            this.singleFont.setEmbedFileName(this.fontFileURI);
        }
        this.returnFont = this.singleFont;

        handleEncoding(afm, pfm);
        handleFontName(afm, pfm);
        handleMetrics(afm, pfm);
    }

    private void handleEncoding(final AFMFile afm, final PFMFile pfm) {
        // Encoding
        if (afm != null) {
            final String encoding = afm.getEncodingScheme();
            this.singleFont.setUseNativeEncoding(true);
            if ("AdobeStandardEncoding".equals(encoding)) {
                this.singleFont.setEncoding(CodePointMapping.STANDARD_ENCODING);
                addUnencodedBasedOnEncoding(afm);
            } else {
                String effEncodingName;
                if ("FontSpecific".equals(encoding)) {
                    effEncodingName = afm.getFontName() + "Encoding";
                } else {
                    effEncodingName = encoding;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Unusual font encoding encountered: " + encoding
                            + " -> " + effEncodingName);
                }
                final CodePointMapping mapping = buildCustomEncoding(
                        effEncodingName, afm);
                this.singleFont.setEncoding(mapping);
                addUnencodedBasedOnAFM(afm);
            }
        } else {
            if (pfm.getCharSet() >= 0 && pfm.getCharSet() <= 2) {
                this.singleFont.setEncoding(pfm.getCharSetName() + "Encoding");
            } else {
                log.warn("The PFM reports an unsupported encoding ("
                        + pfm.getCharSetName()
                        + "). The font may not work as expected.");
                this.singleFont.setEncoding("WinAnsiEncoding"); // Try fallback,
                // no
                // guarantees!
            }
        }
    }

    private Set<String> toGlyphSet(final String[] glyphNames) {
        final Set<String> glyphSet = new java.util.HashSet<String>();
        for (final String name : glyphNames) {
            glyphSet.add(name);
        }
        return glyphSet;
    }

    /**
     * Adds characters not encoded in the font's primary encoding. This method
     * is used when we don't trust the AFM to expose the same encoding as the
     * primary font.
     *
     * @param afm
     *            the AFM file.
     */
    private void addUnencodedBasedOnEncoding(final AFMFile afm) {
        final SingleByteEncoding encoding = this.singleFont.getEncoding();
        final Set<String> glyphNames = toGlyphSet(encoding.getCharNameMap());
        final List<AFMCharMetrics> charMetrics = afm.getCharMetrics();
        for (final AFMCharMetrics metrics : charMetrics) {
            final String charName = metrics.getCharName();
            if (charName != null && !glyphNames.contains(charName)) {
                this.singleFont.addUnencodedCharacter(metrics.getCharacter(),
                        (int) Math.round(metrics.getWidthX()));
            }
        }
    }

    /**
     * Adds characters not encoded in the font's primary encoding. This method
     * is used when the primary encoding is built based on the character codes
     * in the AFM rather than the specified encoding (ex. with symbolic fonts).
     *
     * @param afm
     *            the AFM file
     */
    private void addUnencodedBasedOnAFM(final AFMFile afm) {
        final List charMetrics = afm.getCharMetrics();
        for (int i = 0, c = afm.getCharCount(); i < c; i++) {
            final AFMCharMetrics metrics = (AFMCharMetrics) charMetrics.get(i);
            if (!metrics.hasCharCode() && metrics.getCharacter() != null) {
                this.singleFont.addUnencodedCharacter(metrics.getCharacter(),
                        (int) Math.round(metrics.getWidthX()));
            }
        }
    }

    private void handleFontName(final AFMFile afm, final PFMFile pfm) {
        // Font name
        if (afm != null) {
            this.returnFont.setFontName(afm.getFontName()); // PostScript font
            // name
            this.returnFont.setFullName(afm.getFullName());
            final Set<String> names = new HashSet<String>();
            names.add(afm.getFamilyName());
            this.returnFont.setFamilyNames(names);
        } else {
            this.returnFont.setFontName(pfm.getPostscriptName());
            String fullName = pfm.getPostscriptName();
            fullName = fullName.replace('-', ' '); // Hack! Try to emulate full
            // name
            this.returnFont.setFullName(fullName); // emulate afm.getFullName()
            final Set<String> names = new HashSet<String>();
            names.add(pfm.getWindowsName()); // emulate afm.getFamilyName()
            this.returnFont.setFamilyNames(names);
        }
    }

    private void handleMetrics(final AFMFile afm, final PFMFile pfm) {
        // Basic metrics
        if (afm != null) {
            if (afm.getCapHeight() != null) {
                this.returnFont.setCapHeight(afm.getCapHeight().intValue());
            }
            if (afm.getXHeight() != null) {
                this.returnFont.setXHeight(afm.getXHeight().intValue());
            }
            if (afm.getAscender() != null) {
                this.returnFont.setAscender(afm.getAscender().intValue());
            }
            if (afm.getDescender() != null) {
                this.returnFont.setDescender(afm.getDescender().intValue());
            }

            this.returnFont.setFontBBox(afm.getFontBBoxAsIntArray());
            if (afm.getStdVW() != null) {
                this.returnFont.setStemV(afm.getStdVW().intValue());
            } else {
                this.returnFont.setStemV(80); // Arbitrary value
            }
            this.returnFont.setItalicAngle((int) afm
                    .getWritingDirectionMetrics(0).getItalicAngle());
        } else {
            this.returnFont.setFontBBox(pfm.getFontBBox());
            this.returnFont.setStemV(pfm.getStemV());
            this.returnFont.setItalicAngle(pfm.getItalicAngle());
        }
        if (pfm != null) {
            // Sometimes the PFM has these metrics while the AFM doesn't (ex.
            // Symbol)
            if (this.returnFont.getCapHeight() == 0) {
                this.returnFont.setCapHeight(pfm.getCapHeight());
            }
            if (this.returnFont.getXHeight(1) == 0) {
                this.returnFont.setXHeight(pfm.getXHeight());
            }
            if (this.returnFont.getAscender() == 0) {
                this.returnFont.setAscender(pfm.getLowerCaseAscent());
            }
            if (this.returnFont.getDescender() == 0) {
                this.returnFont.setDescender(pfm.getLowerCaseDescent());
            }
        }

        // Fallbacks when some crucial font metrics aren't available
        // (the following are all optional in AFM, but FontBBox is always
        // available)
        if (this.returnFont.getXHeight(1) == 0) {
            int xHeight = 0;
            if (afm != null) {
                final AFMCharMetrics chm = afm.getChar("x");
                if (chm != null) {
                    final RectangularShape rect = chm.getBBox();
                    if (rect != null) {
                        xHeight = (int) Math.round(rect.getMinX());
                    }
                }
            }
            if (xHeight == 0) {
                xHeight = Math.round(this.returnFont.getFontBBox()[3] * 0.6f);
            }
            this.returnFont.setXHeight(xHeight);
        }
        if (this.returnFont.getAscender() == 0) {
            int asc = 0;
            if (afm != null) {
                final AFMCharMetrics chm = afm.getChar("d");
                if (chm != null) {
                    final RectangularShape rect = chm.getBBox();
                    if (rect != null) {
                        asc = (int) Math.round(rect.getMinX());
                    }
                }
            }
            if (asc == 0) {
                asc = Math.round(this.returnFont.getFontBBox()[3] * 0.9f);
            }
            this.returnFont.setAscender(asc);
        }
        if (this.returnFont.getDescender() == 0) {
            int desc = 0;
            if (afm != null) {
                final AFMCharMetrics chm = afm.getChar("p");
                if (chm != null) {
                    final RectangularShape rect = chm.getBBox();
                    if (rect != null) {
                        desc = (int) Math.round(rect.getMinX());
                    }
                }
            }
            if (desc == 0) {
                desc = this.returnFont.getFontBBox()[1];
            }
            this.returnFont.setDescender(desc);
        }
        if (this.returnFont.getCapHeight() == 0) {
            this.returnFont.setCapHeight(this.returnFont.getAscender());
        }

        if (afm != null) {
            final String charSet = afm.getCharacterSet();
            int flags = 0;
            if ("Special".equals(charSet)) {
                flags |= 4; // bit 3: Symbolic
            } else {
                if (this.singleFont.getEncoding().mapChar('A') == 'A') {
                    // High likelyhood that the font is non-symbolic
                    flags |= 32; // bit 6: Nonsymbolic
                } else {
                    flags |= 4; // bit 3: Symbolic
                }
            }
            if (afm.getWritingDirectionMetrics(0).isFixedPitch()) {
                flags |= 1; // bit 1: FixedPitch
            }
            if (afm.getWritingDirectionMetrics(0).getItalicAngle() != 0.0) {
                flags |= 64; // bit 7: Italic
            }
            this.returnFont.setFlags(flags);

            this.returnFont.setFirstChar(afm.getFirstChar());
            this.returnFont.setLastChar(afm.getLastChar());
            for (final AFMCharMetrics chm : afm.getCharMetrics()) {
                if (chm.hasCharCode()) {
                    this.singleFont.setWidth(chm.getCharCode(),
                            (int) Math.round(chm.getWidthX()));
                }
            }
            if (this.useKerning) {
                this.returnFont.replaceKerningMap(afm
                        .createXKerningMapEncoded());
            }
        } else {
            this.returnFont.setFlags(pfm.getFlags());
            this.returnFont.setFirstChar(pfm.getFirstChar());
            this.returnFont.setLastChar(pfm.getLastChar());
            for (short i = pfm.getFirstChar(); i <= pfm.getLastChar(); i++) {
                this.singleFont.setWidth(i, pfm.getCharWidth(i));
            }
            if (this.useKerning) {
                this.returnFont.replaceKerningMap(pfm.getKerning());
            }
        }
    }

    private CodePointMapping buildCustomEncoding(final String encodingName,
            final AFMFile afm) {
        int mappingCount = 0;
        // Just count the first time...
        final List<AFMCharMetrics> chars = afm.getCharMetrics();
        for (final AFMCharMetrics charMetrics : chars) {
            if (charMetrics.getCharCode() >= 0) {
                final String u = charMetrics.getUnicodeSequence();
                if (u != null && u.length() == 1) {
                    mappingCount++;
                }
            }
        }
        // ...and now build the table.
        final int[] table = new int[mappingCount * 2];
        final String[] charNameMap = new String[256];
        int idx = 0;
        for (final AFMCharMetrics charMetrics : chars) {
            if (charMetrics.getCharCode() >= 0) {
                charNameMap[charMetrics.getCharCode()] = charMetrics
                        .getCharName();
                final String unicodes = charMetrics.getUnicodeSequence();
                if (unicodes == null) {
                    log.info("No Unicode mapping for glyph: " + charMetrics);
                } else if (unicodes.length() == 1) {
                    table[idx] = charMetrics.getCharCode();
                    idx++;
                    table[idx] = unicodes.charAt(0);
                    idx++;
                } else {
                    log.warn("Multi-character representation of glyph not currently supported: "
                            + charMetrics);
                }
            }
        }
        return new CodePointMapping(encodingName, table, charNameMap);
    }
}
