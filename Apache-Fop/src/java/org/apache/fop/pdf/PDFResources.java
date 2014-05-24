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

/* $Id: PDFResources.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.fop.fonts.FontDescriptor;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.Typeface;
import org.apache.fop.fonts.base14.Symbol;
import org.apache.fop.fonts.base14.ZapfDingbats;
import org.apache.xmlgraphics.java2d.color.profile.ColorProfileUtil;

/**
 * Class representing a /Resources object.
 *
 * /Resources object contain a list of references to the fonts for the document
 */
public class PDFResources extends PDFDictionary {

    /**
     * /Font objects keyed by their internal name
     */
    protected Map<String, PDFFont> fonts = new LinkedHashMap<String, PDFFont>();

    /**
     * Set of XObjects
     */
    protected Set<PDFXObject> xObjects = new LinkedHashSet<PDFXObject>();

    /**
     * Set of patterns
     */
    protected Set<PDFPattern> patterns = new LinkedHashSet<PDFPattern>();

    /**
     * Set of shadings
     */
    protected Set<PDFShading> shadings = new LinkedHashSet<PDFShading>();

    /**
     * Set of ExtGStates
     */
    protected Set<PDFGState> gstates = new LinkedHashSet<PDFGState>();

    /** Map of color spaces (key: color space name) */
    protected Map<PDFName, PDFColorSpace> colorSpaces = new LinkedHashMap<PDFName, PDFColorSpace>();

    /** Map of ICC color spaces (key: ICC profile description) */
    protected Map<String, PDFICCBasedColorSpace> iccColorSpaces = new LinkedHashMap<String, PDFICCBasedColorSpace>();

    /**
     * create a /Resources object.
     *
     * @param objnum
     *            the object's number
     */
    public PDFResources(final int objnum) {
        /* generic creation of object */
        super();
        setObjectNumber(objnum);
    }

    /**
     * add font object to resources list.
     *
     * @param font
     *            the PDFFont to add
     */
    public void addFont(final PDFFont font) {
        this.fonts.put(font.getName(), font);
    }

    /**
     * Add the fonts in the font info to this PDF document's Font Resources.
     *
     * @param doc
     *            PDF document to add fonts to
     * @param fontInfo
     *            font info object to get font information from
     */
    public void addFonts(final PDFDocument doc, final FontInfo fontInfo) {
        final Map<String, Typeface> usedFonts = fontInfo.getUsedFonts();
        for (final String f : usedFonts.keySet()) {
            final Typeface font = usedFonts.get(f);

            // Check if the font actually had any mapping operations. If not, it
            // is an indication
            // that it has never actually been used and therefore doesn't have
            // to be embedded.
            if (font.hadMappingOperations()) {
                FontDescriptor desc = null;
                if (font instanceof FontDescriptor) {
                    desc = (FontDescriptor) font;
                }
                String encoding = font.getEncodingName();
                if (font instanceof Symbol || font instanceof ZapfDingbats) {
                    encoding = null; // Symbolic fonts shouldn't specify an
                                     // encoding value in PDF
                }
                addFont(doc.getFactory().makeFont(f, font.getEmbedFontName(),
                        encoding, font, desc));
            }
        }
    }

    /**
     * Add a PDFGState to the resources.
     *
     * @param gs
     *            the PDFGState to add
     */
    public void addGState(final PDFGState gs) {
        this.gstates.add(gs);
    }

    /**
     * Add a Shading to the resources.
     *
     * @param theShading
     *            the shading to add
     */
    public void addShading(final PDFShading theShading) {
        this.shadings.add(theShading);
    }

    /**
     * Add the pattern to the resources.
     *
     * @param thePattern
     *            the pattern to add
     */
    public void addPattern(final PDFPattern thePattern) {
        this.patterns.add(thePattern);
    }

    /**
     * Add an XObject to the resources.
     *
     * @param xObject
     *            the XObject to add
     */
    public void addXObject(final PDFXObject xObject) {
        this.xObjects.add(xObject);
    }

    /**
     * Add a ColorSpace dictionary to the resources.
     * 
     * @param colorSpace
     *            the color space
     */
    public void addColorSpace(final PDFColorSpace colorSpace) {
        this.colorSpaces.put(new PDFName(colorSpace.getName()), colorSpace);
        if (colorSpace instanceof PDFICCBasedColorSpace) {
            final PDFICCBasedColorSpace icc = (PDFICCBasedColorSpace) colorSpace;
            final String desc = ColorProfileUtil.getICCProfileDescription(icc
                    .getICCStream().getICCProfile());
            this.iccColorSpaces.put(desc, icc);
        }
    }

    /**
     * Returns a ICCBased color space by profile name.
     * 
     * @param desc
     *            the name of the color space
     * @return the requested color space or null if it wasn't found
     */
    public PDFICCBasedColorSpace getICCColorSpaceByProfileName(final String desc) {
        final PDFICCBasedColorSpace cs = this.iccColorSpaces.get(desc);
        return cs;
    }

    /**
     * Returns a color space by name.
     * 
     * @param name
     *            the name of the color space
     * @return the requested color space or null if it wasn't found
     */
    public PDFColorSpace getColorSpace(final PDFName name) {
        final PDFColorSpace cs = this.colorSpaces.get(name);
        return cs;
    }

    @Override
    public int output(final OutputStream stream) throws IOException {
        populateDictionary();
        return super.output(stream);
    }

    private void populateDictionary() {
        if (!this.fonts.isEmpty()) {
            final PDFDictionary dict = new PDFDictionary(this);
            /* construct PDF dictionary of font object references */
            for (final Map.Entry<String, PDFFont> entry : this.fonts.entrySet()) {
                dict.put(entry.getKey(), entry.getValue());
            }
            put("Font", dict);
        }

        if (!this.shadings.isEmpty()) {
            final PDFDictionary dict = new PDFDictionary(this);
            for (final PDFShading shading : this.shadings) {
                dict.put(shading.getName(), shading);
            }
            put("Shading", dict);
        }

        if (!this.patterns.isEmpty()) {
            final PDFDictionary dict = new PDFDictionary(this);
            for (final PDFPattern pattern : this.patterns) {
                dict.put(pattern.getName(), pattern);
            }
            put("Pattern", dict);
        }

        final PDFArray procset = new PDFArray(this);
        procset.add(new PDFName("PDF"));
        procset.add(new PDFName("ImageB"));
        procset.add(new PDFName("ImageC"));
        procset.add(new PDFName("Text"));
        put("ProcSet", procset);

        if (this.xObjects != null && !this.xObjects.isEmpty()) {
            final PDFDictionary dict = new PDFDictionary(this);
            for (final PDFXObject xObject : this.xObjects) {
                dict.put(xObject.getName().toString(), xObject);
            }
            put("XObject", dict);
        }

        if (!this.gstates.isEmpty()) {
            final PDFDictionary dict = new PDFDictionary(this);
            for (final PDFGState gstate : this.gstates) {
                dict.put(gstate.getName(), gstate);
            }
            put("ExtGState", dict);
        }

        if (!this.colorSpaces.isEmpty()) {
            final PDFDictionary dict = new PDFDictionary(this);
            for (final PDFColorSpace colorSpace : this.colorSpaces.values()) {
                dict.put(colorSpace.getName(), colorSpace);
            }
            put("ColorSpace", dict);
        }
    }

}
