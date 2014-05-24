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

package org.apache.fop.render.pdf;

import java.awt.image.ComponentColorModel;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.fop.pdf.FlateFilter;
import org.apache.fop.pdf.PDFAMode;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFProfile;
import org.apache.fop.render.RawPNGTestUtil;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.ImageRawPNG;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImageRawPNGAdapterTestCase {

    @Test
    public void testSetupWithIndexColorModel() {
        final IndexColorModel cm = mock(IndexColorModel.class);
        final ImageRawPNG irpng = mock(ImageRawPNG.class);
        final PDFDocument doc = mock(PDFDocument.class);
        final PDFProfile profile = mock(PDFProfile.class);
        final ImageRawPNGAdapter irpnga = new ImageRawPNGAdapter(irpng, "mock");
        final ImageSize is = RawPNGTestUtil.getImageSize();

        when(irpng.getColorModel()).thenReturn(cm);
        // when(cm.hasAlpha()).thenReturn(false);
        when(doc.getProfile()).thenReturn(profile);
        when(profile.getPDFAMode()).thenReturn(PDFAMode.PDFA_1A);
        when(irpng.getSize()).thenReturn(is);
        irpnga.setup(doc);
        final FlateFilter filter = (FlateFilter) irpnga.getPDFFilter();
        assertEquals(1, filter.getColors());
    }

    @Test
    public void testSetupWithComponentColorModel() throws IOException {
        final ComponentColorModel cm = mock(ComponentColorModel.class);
        final ImageRawPNG irpng = mock(ImageRawPNG.class);
        final PDFDocument doc = mock(PDFDocument.class);
        final PDFProfile profile = mock(PDFProfile.class);
        final ImageRawPNGAdapter irpnga = new ImageRawPNGAdapter(irpng, "mock");
        final ImageSize is = RawPNGTestUtil.getImageSize();

        when(irpng.getColorModel()).thenReturn(cm);
        when(cm.getNumComponents()).thenReturn(3);
        // when(cm.hasAlpha()).thenReturn(false);
        when(doc.getProfile()).thenReturn(profile);
        when(profile.getPDFAMode()).thenReturn(PDFAMode.PDFA_1A);
        when(irpng.getSize()).thenReturn(is);
        irpnga.setup(doc);
        final FlateFilter filter = (FlateFilter) irpnga.getPDFFilter();
        assertEquals(3, filter.getColors());
    }

    @Test
    public void testOutputContentsWithRGBPNG() throws IOException {
        testOutputContentsWithGRGBAPNG(-1, 128, 128, 128, -1);
    }

    @Test
    public void testOutputContentsWithRGBAPNG() throws IOException {
        testOutputContentsWithGRGBAPNG(-1, 128, 128, 128, 128);
    }

    @Test
    public void testOutputContentsWithGPNG() throws IOException {
        testOutputContentsWithGRGBAPNG(128, -1, -1, -1, -1);
    }

    @Test
    public void testOutputContentsWithGAPNG() throws IOException {
        testOutputContentsWithGRGBAPNG(128, -1, -1, -1, 128);
    }

    private void testOutputContentsWithGRGBAPNG(final int gray, final int red,
            final int green, final int blue, final int alpha)
            throws IOException {
        final int numColorComponents = gray > -1 ? 1 : 3;
        final int numComponents = numColorComponents + (alpha > -1 ? 1 : 0);
        final ComponentColorModel cm = mock(ComponentColorModel.class);
        final ImageRawPNG irpng = mock(ImageRawPNG.class);
        final PDFDocument doc = mock(PDFDocument.class);
        final PDFProfile profile = mock(PDFProfile.class);
        final ImageRawPNGAdapter irpnga = new ImageRawPNGAdapter(irpng, "mock");
        final ImageSize is = RawPNGTestUtil.getImageSize();

        when(irpng.getColorModel()).thenReturn(cm);
        when(cm.getNumComponents()).thenReturn(numComponents);
        // when(cm.hasAlpha()).thenReturn(false);
        when(doc.getProfile()).thenReturn(profile);
        when(profile.getPDFAMode()).thenReturn(PDFAMode.PDFA_1A);
        when(irpng.getSize()).thenReturn(is);
        irpnga.setup(doc);
        final FlateFilter filter = (FlateFilter) irpnga.getPDFFilter();
        assertEquals(numColorComponents, filter.getColors());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] data = RawPNGTestUtil.buildGRGBAData(gray, red, green,
                blue, alpha);
        final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        when(irpng.createInputStream()).thenReturn(bais);
        irpnga.outputContents(baos);
        if (alpha > -1) {
            final byte[] expected = RawPNGTestUtil.buildGRGBAData(gray, red,
                    green, blue, -1);
            assertArrayEquals(expected, baos.toByteArray());
        } else {
            assertArrayEquals(data, baos.toByteArray());
        }
    }

}
