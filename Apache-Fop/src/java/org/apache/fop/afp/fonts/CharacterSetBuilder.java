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

/* $Id: CharacterSetBuilder.java 1338605 2012-05-15 09:07:02Z mehdi $ */

package org.apache.fop.afp.fonts;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.AFPConstants;
import org.apache.fop.afp.AFPEventProducer;
import org.apache.fop.afp.util.ResourceAccessor;
import org.apache.fop.afp.util.StructuredFieldReader;
import org.apache.fop.fonts.Typeface;
import org.apache.xmlgraphics.image.loader.util.SoftMapCache;

/**
 * The CharacterSetBuilder is responsible building the a CharacterSet instance
 * that holds the font metric data. The data is either read from disk and passed
 * to a CharacterSet (*) or a FopCharacterSet is instantiated that is composed
 * of a Typeface instance configured with this data.
 * <p/>
 * -*- For referenced fonts CharacterSetBuilder is responsible for reading the
 * font attributes from binary code page files and the character set metric
 * files. In IBM font structure, a code page maps each character of text to the
 * characters in a character set. Each character is translated into a code
 * point. When the character is printed, each code point is matched to a
 * character ID on the code page specified. The character ID is then matched to
 * the image (raster pattern or outline pattern) of the character in the
 * character set specified. The image in the character set is the image that is
 * printed in the document. To be a valid code page for a particular character
 * set, all character IDs in the code page must be included in that character
 * set.
 * <p/>
 * This class will read the font information from the binary code page files and
 * character set metric files in order to determine the correct metrics to use
 * when rendering the formatted object.
 * <p/>
 *
 */
@Slf4j
public abstract class CharacterSetBuilder {

    /**
     * Template used to convert lists to arrays.
     */
    private static final CharacterSetOrientation[] EMPTY_CSO_ARRAY = new CharacterSetOrientation[0];

    /** Codepage MO:DCA structured field. */
    private static final byte[] CODEPAGE_SF = new byte[] { (byte) 0xD3,
        (byte) 0xA8, (byte) 0x87 };

    /** Character table MO:DCA structured field. */
    private static final byte[] CHARACTER_TABLE_SF = new byte[] { (byte) 0xD3,
        (byte) 0x8C, (byte) 0x87 };

    /** Font descriptor MO:DCA structured field. */
    private static final byte[] FONT_DESCRIPTOR_SF = new byte[] { (byte) 0xD3,
        (byte) 0xA6, (byte) 0x89 };

    /** Font control MO:DCA structured field. */
    private static final byte[] FONT_CONTROL_SF = new byte[] { (byte) 0xD3,
        (byte) 0xA7, (byte) 0x89 };

    /** Font orientation MO:DCA structured field. */
    private static final byte[] FONT_ORIENTATION_SF = new byte[] { (byte) 0xD3,
        (byte) 0xAE, (byte) 0x89 };

    /** Font position MO:DCA structured field. */
    private static final byte[] FONT_POSITION_SF = new byte[] { (byte) 0xD3,
        (byte) 0xAC, (byte) 0x89 };

    /** Font index MO:DCA structured field. */
    private static final byte[] FONT_INDEX_SF = new byte[] { (byte) 0xD3,
        (byte) 0x8C, (byte) 0x89 };

    /**
     * The collection of code pages
     */
    private final Map<String, Map<String, String>> codePagesCache = Collections
            .synchronizedMap(new WeakHashMap<String, Map<String, String>>());

    /**
     * Cache of charactersets
     */
    private final SoftMapCache characterSetsCache = new SoftMapCache(true);

    /** Default constructor. */
    private CharacterSetBuilder() {
    }

    /**
     * Factory method for the single-byte implementation of AFPFontReader.
     *
     * @return AFPFontReader
     */
    public static CharacterSetBuilder getSingleByteInstance() {
        return SingleByteLoader.getInstance();
    }

    /**
     * Factory method for the double-byte (CID Keyed font (Type 0))
     * implementation of AFPFontReader.
     *
     * @return AFPFontReader
     */
    public static CharacterSetBuilder getDoubleByteInstance() {
        return DoubleByteLoader.getInstance();
    }

    /**
     * Returns an InputStream to a given file path and filename
     *
     * * @param accessor the resource accessor
     *
     * @param filename
     *            the file name
     * @param eventProducer
     *            for handling AFP related events
     * @return an inputStream
     *
     * @throws IOException
     *             in the event that an I/O exception of some sort has occurred
     */
    protected InputStream openInputStream(final ResourceAccessor accessor,
            final String filename, final AFPEventProducer eventProducer)
            throws IOException {
        URI uri;
        try {
            uri = new URI(filename.trim());
        } catch (final URISyntaxException e) {
            throw new FileNotFoundException("Invalid filename: " + filename
                    + " (" + e.getMessage() + ")");
        }

        if (log.isDebugEnabled()) {
            log.debug("Opening " + uri);
        }
        final InputStream inputStream = accessor.createInputStream(uri);
        return inputStream;
    }

    /**
     * Closes the inputstream
     *
     * @param inputStream
     *            the inputstream to close
     */
    protected void closeInputStream(final InputStream inputStream) {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (final Exception ex) {
            // Lets log at least!
            log.error(ex.getMessage());
        }
    }

    /**
     * Load the font details and metrics into the CharacterSetMetric object,
     * this will use the actual afp code page and character set files to load
     * the object with the necessary metrics.
     *
     * @param characterSetName
     *            name of the characterset
     * @param codePageName
     *            name of the code page file
     * @param encoding
     *            encoding name
     * @param accessor
     *            used to load codepage and characterset
     * @param eventProducer
     *            for handling AFP related events
     * @return CharacterSet object
     * @throws IOException
     *             if an I/O error occurs
     */
    public CharacterSet buildSBCS(final String characterSetName,
            final String codePageName, final String encoding,
            final ResourceAccessor accessor,
            final AFPEventProducer eventProducer) throws IOException {
        return processFont(characterSetName, codePageName, encoding,
                CharacterSetType.SINGLE_BYTE, accessor, eventProducer);
    }

    /**
     * Load the font details and metrics into the CharacterSetMetric object,
     * this will use the actual afp code page and character set files to load
     * the object with the necessary metrics. This method is to be used for
     * double byte character sets (DBCS).
     *
     * @param characterSetName
     *            name of the characterset
     * @param codePageName
     *            name of the code page file
     * @param encoding
     *            encoding name
     * @param charsetType
     *            the characterset type
     * @param accessor
     *            used to load codepage and characterset
     * @param eventProducer
     *            for handling AFP related events
     * @return CharacterSet object
     * @throws IOException
     *             if an I/O error occurs
     */
    public CharacterSet buildDBCS(final String characterSetName,
            final String codePageName, final String encoding,
            final CharacterSetType charsetType,
            final ResourceAccessor accessor,
            final AFPEventProducer eventProducer) throws IOException {
        return processFont(characterSetName, codePageName, encoding,
                charsetType, accessor, eventProducer);
    }

    /**
     * Load the font details and metrics into the CharacterSetMetric object,
     * this will use the actual afp code page and character set files to load
     * the object with the necessary metrics.
     *
     * @param characterSetName
     *            the CharacterSetMetric object to populate
     * @param codePageName
     *            the name of the code page to use
     * @param encoding
     *            name of the encoding in use
     * @param typeface
     *            base14 font name
     * @param eventProducer
     *            for handling AFP related events
     * @return CharacterSet object
     * @throws IOException
     *             if an I/O error occurs
     */
    public CharacterSet build(final String characterSetName,
            final String codePageName, final String encoding,
            final Typeface typeface, final AFPEventProducer eventProducer)
                    throws IOException {
        return new FopCharacterSet(codePageName, encoding, characterSetName,
                typeface, eventProducer);
    }

    private CharacterSet processFont(final String characterSetName,
            final String codePageName, final String encoding,
            final CharacterSetType charsetType,
            final ResourceAccessor accessor,
            final AFPEventProducer eventProducer) throws IOException {
        // check for cached version of the characterset
        final String descriptor = characterSetName + "_" + encoding + "_"
                + codePageName;
        CharacterSet characterSet = (CharacterSet) this.characterSetsCache
                .get(descriptor);

        if (characterSet != null) {
            return characterSet;
        }

        // characterset not in the cache, so recreating
        characterSet = new CharacterSet(codePageName, encoding, charsetType,
                characterSetName, accessor, eventProducer);

        InputStream inputStream = null;

        try {

            /**
             * Get the code page which contains the character mapping
             * information to map the unicode character id to the graphic
             * chracter global identifier.
             */
            Map<String, String> codePage;
            synchronized (this.codePagesCache) {
                codePage = this.codePagesCache.get(codePageName);

                if (codePage == null) {
                    codePage = loadCodePage(codePageName, encoding, accessor,
                            eventProducer);
                    this.codePagesCache.put(codePageName, codePage);
                }
            }

            inputStream = openInputStream(accessor, characterSetName,
                    eventProducer);

            final StructuredFieldReader structuredFieldReader = new StructuredFieldReader(
                    inputStream);

            // Process D3A689 Font Descriptor
            final FontDescriptor fontDescriptor = processFontDescriptor(structuredFieldReader);
            characterSet.setNominalVerticalSize(fontDescriptor
                    .getNominalFontSizeInMillipoints());

            // Process D3A789 Font Control
            final FontControl fontControl = processFontControl(structuredFieldReader);

            if (fontControl != null) {
                // process D3AE89 Font Orientation
                final CharacterSetOrientation[] characterSetOrientations = processFontOrientation(structuredFieldReader);

                double metricNormalizationFactor;
                if (fontControl.isRelative()) {
                    metricNormalizationFactor = 1;
                } else {
                    final int dpi = fontControl.getDpi();
                    metricNormalizationFactor = 1000.0d * 72000.0d
                            / fontDescriptor.getNominalFontSizeInMillipoints()
                            / dpi;
                }

                // process D3AC89 Font Position
                processFontPosition(structuredFieldReader,
                        characterSetOrientations, metricNormalizationFactor);

                // process D38C89 Font Index (per orientation)
                for (final CharacterSetOrientation characterSetOrientation : characterSetOrientations) {
                    processFontIndex(structuredFieldReader,
                            characterSetOrientation, codePage,
                            metricNormalizationFactor);
                    characterSet
                    .addCharacterSetOrientation(characterSetOrientation);
                }
            } else {
                throw new IOException(
                        "Missing D3AE89 Font Control structured field.");
            }

        } finally {
            closeInputStream(inputStream);
        }
        this.characterSetsCache.put(descriptor, characterSet);
        return characterSet;
    }

    /**
     * Load the code page information from the appropriate file. The file name
     * to load is determined by the code page name and the file extension 'CDP'.
     *
     * @param codePage
     *            the code page identifier
     * @param encoding
     *            the encoding to use for the character decoding
     * @param accessor
     *            the resource accessor
     * @param eventProducer
     *            for handling AFP related events
     * @return a code page mapping (key: GCGID, value: Unicode character)
     * @throws IOException
     *             if an I/O exception of some sort has occurred.
     */
    protected Map<String, String> loadCodePage(final String codePage,
            final String encoding, final ResourceAccessor accessor,
            final AFPEventProducer eventProducer) throws IOException {

        // Create the HashMap to store code page information
        final Map<String, String> codePages = new HashMap<String, String>();

        InputStream inputStream = null;
        try {
            inputStream = openInputStream(accessor, codePage.trim(),
                    eventProducer);

            final StructuredFieldReader structuredFieldReader = new StructuredFieldReader(
                    inputStream);
            final byte[] data = structuredFieldReader
                    .getNext(CHARACTER_TABLE_SF);

            int position = 0;
            final byte[] gcgiBytes = new byte[8];
            final byte[] charBytes = new byte[1];

            // Read data, ignoring bytes 0 - 2
            for (int index = 3; index < data.length; index++) {
                if (position < 8) {
                    // Build the graphic character global identifier key
                    gcgiBytes[position] = data[index];
                    position++;
                } else if (position == 9) {
                    position = 0;
                    // Set the character
                    charBytes[0] = data[index];
                    final String gcgiString = new String(gcgiBytes,
                            AFPConstants.EBCIDIC_ENCODING);
                    // Use the 8-bit char index to find the Unicode character
                    // using the Java encoding
                    // given in the configuration. If the code page and the Java
                    // encoding don't
                    // match, a wrong Unicode character will be associated with
                    // the AFP GCGID.
                    // Idea: we could use IBM's GCGID to Unicode map and build
                    // code pages ourselves.
                    final String charString = new String(charBytes, encoding);
                    codePages.put(gcgiString, charString);
                } else {
                    position++;
                }
            }
        } catch (final FileNotFoundException e) {
            eventProducer.codePageNotFound(this, e);
        } finally {
            closeInputStream(inputStream);
        }

        return codePages;
    }

    /**
     * Process the font descriptor details using the structured field reader.
     *
     * @param structuredFieldReader
     *            the structured field reader
     * @return a class representing the font descriptor
     * @throws IOException
     *             if an I/O exception of some sort has occurred.
     */
    protected static FontDescriptor processFontDescriptor(
            final StructuredFieldReader structuredFieldReader)
                    throws IOException {

        final byte[] fndData = structuredFieldReader
                .getNext(FONT_DESCRIPTOR_SF);
        return new FontDescriptor(fndData);
    }

    /**
     * Process the font control details using the structured field reader.
     *
     * @param structuredFieldReader
     *            the structured field reader
     * @return the FontControl
     * @throws IOException
     *             if an I/O exception of some sort has occurred.
     */
    protected FontControl processFontControl(
            final StructuredFieldReader structuredFieldReader)
                    throws IOException {

        final byte[] fncData = structuredFieldReader.getNext(FONT_CONTROL_SF);

        FontControl fontControl = null;
        if (fncData != null) {
            fontControl = new FontControl();

            if (fncData[7] == (byte) 0x02) {
                fontControl.setRelative(true);
            }
            final int metricResolution = getUBIN(fncData, 9);
            if (metricResolution == 1000) {
                // Special case: 1000 units per em (rather than dpi)
                fontControl.setUnitsPerEm(1000);
            } else {
                fontControl.setDpi(metricResolution / 10);
            }
        }
        return fontControl;
    }

    /**
     * Process the font orientation details from using the structured field
     * reader.
     *
     * @param structuredFieldReader
     *            the structured field reader
     * @return CharacterSetOrientation array
     * @throws IOException
     *             if an I/O exception of some sort has occurred.
     */
    protected CharacterSetOrientation[] processFontOrientation(
            final StructuredFieldReader structuredFieldReader)
                    throws IOException {

        final byte[] data = structuredFieldReader.getNext(FONT_ORIENTATION_SF);

        int position = 0;
        final byte[] fnoData = new byte[26];

        final List<CharacterSetOrientation> orientations = new ArrayList<CharacterSetOrientation>();

        // Read data, ignoring bytes 0 - 2
        for (int index = 3; index < data.length; index++) {
            // Build the font orientation record
            fnoData[position] = data[index];
            position++;

            if (position == 26) {

                position = 0;

                final int orientation = determineOrientation(fnoData[2]);
                // Space Increment
                final int space = ((fnoData[8] & 0xFF) << 8)
                        + (fnoData[9] & 0xFF);
                // Em-Space Increment
                final int em = ((fnoData[14] & 0xFF) << 8)
                        + (fnoData[15] & 0xFF);

                final CharacterSetOrientation cso = new CharacterSetOrientation(
                        orientation);
                cso.setSpaceIncrement(space);
                cso.setEmSpaceIncrement(em);
                orientations.add(cso);

            }
        }

        return orientations.toArray(EMPTY_CSO_ARRAY);
    }

    /**
     * Populate the CharacterSetOrientation object in the suplied array with the
     * font position details using the supplied structured field reader.
     *
     * @param structuredFieldReader
     *            the structured field reader
     * @param characterSetOrientations
     *            the array of CharacterSetOrientation objects
     * @param metricNormalizationFactor
     *            factor to apply to the metrics to get normalized font metric
     *            values
     * @throws IOException
     *             if an I/O exception of some sort has occurred.
     */
    protected void processFontPosition(
            final StructuredFieldReader structuredFieldReader,
            final CharacterSetOrientation[] characterSetOrientations,
            final double metricNormalizationFactor) throws IOException {

        final byte[] data = structuredFieldReader.getNext(FONT_POSITION_SF);

        int position = 0;
        final byte[] fpData = new byte[26];

        int characterSetOrientationIndex = 0;

        // Read data, ignoring bytes 0 - 2
        for (int index = 3; index < data.length; index++) {
            if (position < 22) {
                // Build the font orientation record
                fpData[position] = data[index];
                if (position == 9) {
                    final CharacterSetOrientation characterSetOrientation = characterSetOrientations[characterSetOrientationIndex];

                    final int xHeight = getSBIN(fpData, 2);
                    final int capHeight = getSBIN(fpData, 4);
                    final int ascHeight = getSBIN(fpData, 6);
                    int dscHeight = getSBIN(fpData, 8);

                    dscHeight = dscHeight * -1;

                    characterSetOrientation.setXHeight((int) Math.round(xHeight
                            * metricNormalizationFactor));
                    characterSetOrientation.setCapHeight((int) Math
                            .round(capHeight * metricNormalizationFactor));
                    characterSetOrientation.setAscender((int) Math
                            .round(ascHeight * metricNormalizationFactor));
                    characterSetOrientation.setDescender((int) Math
                            .round(dscHeight * metricNormalizationFactor));
                }
            } else if (position == 22) {
                position = 0;
                characterSetOrientationIndex++;
                fpData[position] = data[index];
            }

            position++;
        }

    }

    /**
     * Process the font index details for the character set orientation.
     *
     * @param structuredFieldReader
     *            the structured field reader
     * @param cso
     *            the CharacterSetOrientation object to populate
     * @param codepage
     *            the map of code pages
     * @param metricNormalizationFactor
     *            factor to apply to the metrics to get normalized font metric
     *            values
     * @throws IOException
     *             if an I/O exception of some sort has occurred.
     */
    protected void processFontIndex(
            final StructuredFieldReader structuredFieldReader,
            final CharacterSetOrientation cso,
            final Map<String, String> codepage,
            final double metricNormalizationFactor) throws IOException {

        final byte[] data = structuredFieldReader.getNext(FONT_INDEX_SF);

        int position = 0;

        final byte[] gcgid = new byte[8];
        final byte[] fiData = new byte[20];

        char lowest = 255;
        char highest = 0;
        String firstABCMismatch = null;

        // Read data, ignoring bytes 0 - 2
        for (int index = 3; index < data.length; index++) {
            if (position < 8) {
                gcgid[position] = data[index];
                position++;
            } else if (position < 27) {
                fiData[position - 8] = data[index];
                position++;
            } else if (position == 27) {

                fiData[position - 8] = data[index];

                position = 0;

                final String gcgiString = new String(gcgid,
                        AFPConstants.EBCIDIC_ENCODING);

                final String idx = codepage.get(gcgiString);

                if (idx != null) {

                    final char cidx = idx.charAt(0);
                    final int width = getUBIN(fiData, 0);
                    final int a = getSBIN(fiData, 10);
                    final int b = getUBIN(fiData, 12);
                    final int c = getSBIN(fiData, 14);
                    final int abc = a + b + c;
                    final int diff = Math.abs(abc - width);
                    if (diff != 0 && width != 0) {
                        final double diffPercent = 100 * diff / (double) width;
                        if (diffPercent > 2) {
                            if (log.isTraceEnabled()) {
                                log.trace(gcgiString + ": " + a + " + " + b
                                        + " + " + c + " = " + (a + b + c)
                                        + " but found: " + width);
                            }
                            if (firstABCMismatch == null) {
                                firstABCMismatch = gcgiString;
                            }
                        }
                    }

                    if (cidx < lowest) {
                        lowest = cidx;
                    }

                    if (cidx > highest) {
                        highest = cidx;
                    }

                    final int normalizedWidth = (int) Math.round(width
                            * metricNormalizationFactor);

                    cso.setWidth(cidx, normalizedWidth);

                }

            }
        }

        cso.setFirstChar(lowest);
        cso.setLastChar(highest);

        if (log.isDebugEnabled() && firstABCMismatch != null) {
            // Debug level because it usually is no problem.
            log.debug("Font has metrics inconsitencies where A+B+C doesn't equal the"
                    + " character increment. The first such character found: "
                    + firstABCMismatch);
        }
    }

    private static int getUBIN(final byte[] data, final int start) {
        return ((data[start] & 0xFF) << 8) + (data[start + 1] & 0xFF);
    }

    private static int getSBIN(final byte[] data, final int start) {
        final int ubin = ((data[start] & 0xFF) << 8) + (data[start + 1] & 0xFF);
        if ((ubin & 0x8000) != 0) {
            // extend sign
            return ubin | 0xFFFF0000;
        } else {
            return ubin;
        }
    }

    private class FontControl {

        private int dpi;
        private int unitsPerEm;

        private boolean isRelative = false;

        public int getDpi() {
            return this.dpi;
        }

        public void setDpi(final int i) {
            this.dpi = i;
        }

        public int getUnitsPerEm() {
            return this.unitsPerEm;
        }

        public void setUnitsPerEm(final int value) {
            this.unitsPerEm = value;
        }

        public boolean isRelative() {
            return this.isRelative;
        }

        public void setRelative(final boolean b) {
            this.isRelative = b;
        }
    }

    private static class FontDescriptor {

        private final byte[] data;

        public FontDescriptor(final byte[] data) {
            this.data = data;
        }

        public int getNominalFontSizeInMillipoints() {
            final int nominalFontSize = 100 * getUBIN(this.data, 39);
            return nominalFontSize;
        }
    }

    private static final class SingleByteLoader extends CharacterSetBuilder {

        private static final SingleByteLoader INSTANCE = new SingleByteLoader();

        private SingleByteLoader() {
            super();
        }

        private static SingleByteLoader getInstance() {
            return INSTANCE;
        }
    }

    /**
     * Double-byte (CID Keyed font (Type 0)) implementation of AFPFontReader.
     */
    private static final class DoubleByteLoader extends CharacterSetBuilder {

        private static final DoubleByteLoader INSTANCE = new DoubleByteLoader();

        private DoubleByteLoader() {
        }

        static DoubleByteLoader getInstance() {
            return INSTANCE;
        }

        @Override
        protected Map<String, String> loadCodePage(final String codePage,
                final String encoding, final ResourceAccessor accessor,
                final AFPEventProducer eventProducer) throws IOException {

            // Create the HashMap to store code page information
            final Map<String, String> codePages = new HashMap<String, String>();

            InputStream inputStream = null;
            try {
                inputStream = openInputStream(accessor, codePage.trim(),
                        eventProducer);

                final StructuredFieldReader structuredFieldReader = new StructuredFieldReader(
                        inputStream);
                byte[] data;
                while ((data = structuredFieldReader
                        .getNext(CHARACTER_TABLE_SF)) != null) {
                    int position = 0;

                    final byte[] gcgiBytes = new byte[8];
                    final byte[] charBytes = new byte[2];
                    // Read data, ignoring bytes 0 - 2
                    for (int index = 3; index < data.length; index++) {

                        if (position < 8) {
                            // Build the graphic character global identifier key
                            gcgiBytes[position] = data[index];
                            position++;
                        } else if (position == 9) {
                            // Set the character
                            charBytes[0] = data[index];
                            position++;
                        } else if (position == 10) {
                            position = 0;
                            // Set the character
                            charBytes[1] = data[index];

                            final String gcgiString = new String(gcgiBytes,
                                    AFPConstants.EBCIDIC_ENCODING);
                            final String charString = new String(charBytes,
                                    encoding);
                            codePages.put(gcgiString, charString);
                        } else {
                            position++;
                        }
                    }
                }
            } catch (final FileNotFoundException e) {
                eventProducer.codePageNotFound(this, e);
            } finally {
                closeInputStream(inputStream);
            }

            return codePages;
        }

    }

    private static int determineOrientation(final byte orientation) {
        int degrees = 0;

        switch (orientation) {
        case 0x00:
            degrees = 0;
            break;
        case 0x2D:
            degrees = 90;
            break;
        case 0x5A:
            degrees = 180;
            break;
        case (byte) 0x87:
            degrees = 270;
            break;
        default:
            throw new IllegalStateException("Invalid orientation: "
                    + orientation);
        }
        return degrees;
    }
}
