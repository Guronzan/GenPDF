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

/* $Id$ */

package org.apache.fop.fonts.type1;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test case for {@link AFMParser}.
 */
public class AFMParserTestCase {

    private final AFMParser sut = new AFMParser();

    /**
     * We're testing with two identical files except one has: EncodingScheme
     * AdobeStandardEncoding the other has: EncodingScheme ExpectedEncoding Both
     * files have the correct character metrics data, and we're checking that
     * both are handled consistently with both encoding settings.
     *
     * @throws IOException
     *             if an I/O error occurs
     */
    @Test
    public void testMappingAgainstAdobeStandardEncoding() throws IOException {
        final InputStream expectedStream = getClass().getResourceAsStream(
                "adobe-charset_unknown-encoding.afm");
        final InputStream adobeStandardStream = getClass().getResourceAsStream(
                "adobe-charset_adobe-encoding.afm");
        final AFMFile expectedParser = this.sut.parse(expectedStream, null);
        final AFMFile adobeStandard = this.sut.parse(adobeStandardStream, null);
        final List<AFMCharMetrics> adobeMetrics = adobeStandard
                .getCharMetrics();
        checkCharMtrxList(true, expectedParser.getCharMetrics(), adobeMetrics);

        compareMetrics(adobeMetrics);

        nonAdobeCharsetUnknownEncoding(adobeMetrics);

        nonAdobeCharsetAdobeEncoding(adobeMetrics);
    }

    private void compareMetrics(final List<AFMCharMetrics> charMetrics) {
        // in order to ensure that every character is parsed properly, we're
        // going to check them
        // against the AFM file (bboxes were created with a counter)
        final AdobeStandardEncoding[] standardEncoding = AdobeStandardEncoding
                .values();
        for (int i = 0; i < charMetrics.size(); i++) {
            final Rectangle expectedBbox = new Rectangle(i + 1, i + 1, 0, 0);
            final AFMCharMetrics thisMetric = charMetrics.get(i);
            assertTrue(thisMetric.getBBox().equals(expectedBbox));
            assertEquals(thisMetric.getCharName(),
                    standardEncoding[i].getAdobeName());
        }
    }

    /**
     * A non-adobe encoded file is tested, all the character codes are not
     * AdobeStandardEncoding and the encoding is not AdobeStandardEncoding, we
     * are checking a failure case here. Checking that the AdobeStandardEncoding
     * isn't forced on other encodings.
     *
     * @param expected
     *            the AdobeStandardEncoding encoded character metrics list
     * @throws IOException
     *             if an IO error occurs
     */
    private void nonAdobeCharsetUnknownEncoding(
            final List<AFMCharMetrics> expected) throws IOException {
        final InputStream inStream = getClass().getResourceAsStream(
                "notadobe-charset_unknown-encoding.afm");
        final AFMFile afmFile = this.sut.parse(inStream, null);
        final List<AFMCharMetrics> unknownEncodingMetrics = afmFile
                .getCharMetrics();
        checkCharMtrxList(false, expected, unknownEncodingMetrics);
    }

    /**
     * This tests a poorly encoded file, it has AdobeStandardEncoding. We are
     * checking that the metrics are correctly analysed against properly encoded
     * char metrics.
     *
     * @param expected
     * @throws IOException
     */
    private void nonAdobeCharsetAdobeEncoding(
            final List<AFMCharMetrics> expected) throws IOException {
        final InputStream inStream = getClass().getResourceAsStream(
                "notadobe-charset_adobe-encoding.afm");
        final AFMFile afmFile = this.sut.parse(inStream, null);
        final List<AFMCharMetrics> correctedCharMetrics = afmFile
                .getCharMetrics();
        checkCharMtrxList(true, expected, correctedCharMetrics);
    }

    private boolean charMetricsEqual(final AFMCharMetrics o1,
            final AFMCharMetrics o2) {
        return o1.getCharCode() == o2.getCharCode()
                && objectEquals(o1.getCharacter(), o2.getCharacter())
                && o1.getWidthX() == o2.getWidthX()
                && o1.getWidthY() == o2.getWidthY()
                && objectEquals(o1.getBBox(), o2.getBBox());
    }

    private void checkCharMtrxList(final boolean expectedResult,
            final List<AFMCharMetrics> expectedList,
            final List<AFMCharMetrics> actualList) {
        assertEquals(expectedList.size(), actualList.size());
        for (int i = 0; i < expectedList.size(); i++) {
            assertEquals(expectedResult,
                    charMetricsEqual(expectedList.get(i), actualList.get(i)));
        }
    }

    private boolean objectEquals(final Object o1, final Object o2) {
        return o1 == null ? o2 == null : o1 == o2 || o1.equals(o2);
    }
}
