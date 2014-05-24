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

/* $Id: FontReader.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.fonts;

//Java
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParserFactory;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fonts.apps.TTFReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class for reading a metric.xml file and creating a font object. Typical
 * usage:
 * 
 * <pre>
 * FontReader reader = new FontReader(<path til metrics.xml>);
 * reader.setFontEmbedPath(<path to a .ttf or .pfb file or null to diable embedding>);
 * reader.useKerning(true);
 * Font f = reader.getFont();
 * </pre>
 */
public class FontReader extends DefaultHandler {

    // private Locator locator = null; // not used at present
    private boolean isCID = false;
    private CustomFont returnFont = null;
    private MultiByteFont multiFont = null;
    private SingleByteFont singleFont = null;
    private final StringBuffer text = new StringBuffer();

    private List<Integer> cidWidths = null;
    private int cidWidthIndex = 0;

    private Map<Integer, Integer> currentKerning = null;

    private List<CMapSegment> bfranges = null;

    private void createFont(final InputSource source) throws FOPException {
        XMLReader parser = null;

        try {
            final SAXParserFactory factory = javax.xml.parsers.SAXParserFactory
                    .newInstance();
            factory.setNamespaceAware(true);
            parser = factory.newSAXParser().getXMLReader();
        } catch (final Exception e) {
            throw new FOPException(e);
        }
        if (parser == null) {
            throw new FOPException("Unable to create SAX parser");
        }

        try {
            parser.setFeature("http://xml.org/sax/features/namespace-prefixes",
                    false);
        } catch (final SAXException e) {
            throw new FOPException(
                    "You need a SAX parser which supports SAX version 2", e);
        }

        parser.setContentHandler(this);

        try {
            parser.parse(source);
        } catch (final SAXException e) {
            throw new FOPException(e);
        } catch (final IOException e) {
            throw new FOPException(e);
        }

    }

    /**
     * Sets the path to embed a font. A null value disables font embedding.
     * 
     * @param path
     *            URI for the embeddable file
     */
    public void setFontEmbedPath(final String path) {
        this.returnFont.setEmbedFileName(path);
    }

    /**
     * Enable/disable use of kerning for the font
     * 
     * @param enabled
     *            true to enable kerning, false to disable
     */
    public void setKerningEnabled(final boolean enabled) {
        this.returnFont.setKerningEnabled(enabled);
    }

    /**
     * Enable/disable use of advanced typographic features for the font
     * 
     * @param enabled
     *            true to enable, false to disable
     */
    public void setAdvancedEnabled(final boolean enabled) {
        this.returnFont.setAdvancedEnabled(enabled);
    }

    /**
     * Sets the font resolver. Needed for URI resolution.
     * 
     * @param resolver
     *            the font resolver
     */
    public void setResolver(final FontResolver resolver) {
        this.returnFont.setResolver(resolver);
    }

    /**
     * Get the generated font object
     * 
     * @return the font
     */
    public Typeface getFont() {
        return this.returnFont;
    }

    /**
     * Construct a FontReader object from a path to a metric.xml file and read
     * metric data
     * 
     * @param source
     *            Source of the font metric file
     * @throws FOPException
     *             if loading the font fails
     */
    public FontReader(final InputSource source) throws FOPException {
        createFont(source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startDocument() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDocumentLocator(final Locator locator) {
        // this.locator = locator; // not used at present
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attributes)
            throws SAXException {
        if (localName.equals("font-metrics")) {
            if ("TYPE0".equals(attributes.getValue("type"))) {
                this.multiFont = new MultiByteFont();
                this.returnFont = this.multiFont;
                this.isCID = true;
                TTFReader.checkMetricsVersion(attributes);
            } else if ("TRUETYPE".equals(attributes.getValue("type"))) {
                this.singleFont = new SingleByteFont();
                this.singleFont.setFontType(FontType.TRUETYPE);
                this.returnFont = this.singleFont;
                this.isCID = false;
                TTFReader.checkMetricsVersion(attributes);
            } else {
                this.singleFont = new SingleByteFont();
                this.singleFont.setFontType(FontType.TYPE1);
                this.returnFont = this.singleFont;
                this.isCID = false;
            }
        } else if ("embed".equals(localName)) {
            this.returnFont.setEmbedFileName(attributes.getValue("file"));
            this.returnFont.setEmbedResourceName(attributes.getValue("class"));
        } else if ("cid-widths".equals(localName)) {
            this.cidWidthIndex = getInt(attributes.getValue("start-index"));
            this.cidWidths = new ArrayList<Integer>();
        } else if ("kerning".equals(localName)) {
            this.currentKerning = new HashMap<Integer, Integer>();
            this.returnFont.putKerningEntry(
                    new Integer(attributes.getValue("kpx1")),
                    this.currentKerning);
        } else if ("bfranges".equals(localName)) {
            this.bfranges = new ArrayList<CMapSegment>();
        } else if ("bf".equals(localName)) {
            final CMapSegment entry = new CMapSegment(
                    getInt(attributes.getValue("us")),
                    getInt(attributes.getValue("ue")),
                    getInt(attributes.getValue("gi")));
            this.bfranges.add(entry);
        } else if ("wx".equals(localName)) {
            this.cidWidths.add(new Integer(attributes.getValue("w")));
        } else if ("widths".equals(localName)) {
            // singleFont.width = new int[256];
        } else if ("char".equals(localName)) {
            try {
                this.singleFont.setWidth(
                        Integer.parseInt(attributes.getValue("idx")),
                        Integer.parseInt(attributes.getValue("wdt")));
            } catch (final NumberFormatException ne) {
                throw new SAXException("Malformed width in metric file: "
                        + ne.getMessage(), ne);
            }
        } else if ("pair".equals(localName)) {
            this.currentKerning.put(new Integer(attributes.getValue("kpx2")),
                    new Integer(attributes.getValue("kern")));
        }

    }

    private int getInt(final String str) throws SAXException {
        int ret = 0;
        try {
            ret = Integer.parseInt(str);
        } catch (final Exception e) {
            throw new SAXException("Error while parsing integer value: " + str,
                    e);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String qName) throws SAXException {
        final String content = this.text.toString().trim();
        if ("font-name".equals(localName)) {
            this.returnFont.setFontName(content);
        } else if ("full-name".equals(localName)) {
            this.returnFont.setFullName(content);
        } else if ("family-name".equals(localName)) {
            final Set<String> s = new HashSet<String>();
            s.add(content);
            this.returnFont.setFamilyNames(s);
        } else if ("ttc-name".equals(localName) && this.isCID) {
            this.multiFont.setTTCName(content);
        } else if ("encoding".equals(localName)) {
            if (this.singleFont != null
                    && this.singleFont.getFontType() == FontType.TYPE1) {
                this.singleFont.setEncoding(content);
            }
        } else if ("cap-height".equals(localName)) {
            this.returnFont.setCapHeight(getInt(content));
        } else if ("x-height".equals(localName)) {
            this.returnFont.setXHeight(getInt(content));
        } else if ("ascender".equals(localName)) {
            this.returnFont.setAscender(getInt(content));
        } else if ("descender".equals(localName)) {
            this.returnFont.setDescender(getInt(content));
        } else if ("left".equals(localName)) {
            final int[] bbox = this.returnFont.getFontBBox();
            bbox[0] = getInt(content);
            this.returnFont.setFontBBox(bbox);
        } else if ("bottom".equals(localName)) {
            final int[] bbox = this.returnFont.getFontBBox();
            bbox[1] = getInt(content);
            this.returnFont.setFontBBox(bbox);
        } else if ("right".equals(localName)) {
            final int[] bbox = this.returnFont.getFontBBox();
            bbox[2] = getInt(content);
            this.returnFont.setFontBBox(bbox);
        } else if ("top".equals(localName)) {
            final int[] bbox = this.returnFont.getFontBBox();
            bbox[3] = getInt(content);
            this.returnFont.setFontBBox(bbox);
        } else if ("first-char".equals(localName)) {
            this.returnFont.setFirstChar(getInt(content));
        } else if ("last-char".equals(localName)) {
            this.returnFont.setLastChar(getInt(content));
        } else if ("flags".equals(localName)) {
            this.returnFont.setFlags(getInt(content));
        } else if ("stemv".equals(localName)) {
            this.returnFont.setStemV(getInt(content));
        } else if ("italic-angle".equals(localName)) {
            this.returnFont.setItalicAngle(getInt(content));
        } else if ("missing-width".equals(localName)) {
            this.returnFont.setMissingWidth(getInt(content));
        } else if ("cid-type".equals(localName)) {
            this.multiFont.setCIDType(CIDFontType.byName(content));
        } else if ("default-width".equals(localName)) {
            this.multiFont.setDefaultWidth(getInt(content));
        } else if ("cid-widths".equals(localName)) {
            final int[] wds = new int[this.cidWidths.size()];
            int j = 0;
            for (int count = 0; count < this.cidWidths.size(); count++) {
                wds[j++] = this.cidWidths.get(count).intValue();
            }

            // multiFont.addCIDWidthEntry(cidWidthIndex, wds);
            this.multiFont.setWidthArray(wds);

        } else if ("bfranges".equals(localName)) {
            this.multiFont.setCMap(this.bfranges.toArray(new CMapSegment[0]));
        }
        this.text.setLength(0); // Reset text buffer (see characters())
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length) {
        this.text.append(ch, start, length);
    }

}
