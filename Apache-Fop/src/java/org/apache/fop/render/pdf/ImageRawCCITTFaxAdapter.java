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

/* $Id: ImageRawCCITTFaxAdapter.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.pdf;

import java.awt.color.ColorSpace;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.pdf.CCFFilter;
import org.apache.fop.pdf.PDFDeviceColorSpace;
import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFFilter;
import org.apache.fop.pdf.PDFFilterList;
import org.apache.xmlgraphics.image.codec.tiff.TIFFImage;
import org.apache.xmlgraphics.image.loader.impl.ImageRawCCITTFax;

/**
 * PDFImage implementation for the PDF renderer which handles raw CCITT fax
 * images.
 */
public class ImageRawCCITTFaxAdapter extends AbstractImageAdapter {

    private PDFFilter pdfFilter = null;

    /**
     * Creates a new PDFImage from an Image instance.
     * 
     * @param image
     *            the CCITT encoded image
     * @param key
     *            XObject key
     */
    public ImageRawCCITTFaxAdapter(final ImageRawCCITTFax image,
            final String key) {
        super(image, key);
    }

    /**
     * Returns the {@link ImageRawCCITTFax} instance for this adapter.
     * 
     * @return the image instance
     */
    public ImageRawCCITTFax getImage() {
        return (ImageRawCCITTFax) this.image;
    }

    /** {@inheritDoc} */
    @Override
    public void setup(final PDFDocument doc) {
        this.pdfFilter = new CCFFilter();
        this.pdfFilter.setApplied(true);
        final PDFDictionary dict = new PDFDictionary();
        dict.put("Columns", this.image.getSize().getWidthPx());
        final int compression = getImage().getCompression();
        switch (compression) {
        case TIFFImage.COMP_FAX_G3_1D:
            dict.put("K", 0);
            break;
        case TIFFImage.COMP_FAX_G3_2D:
            dict.put("K", 1);
            break;
        case TIFFImage.COMP_FAX_G4_2D:
            dict.put("K", -1);
            break;
        default:
            throw new IllegalStateException("Invalid compression scheme: "
                    + compression);
        }
        ((CCFFilter) this.pdfFilter).setDecodeParms(dict);

        super.setup(doc);
    }

    /** {@inheritDoc} */
    @Override
    public PDFDeviceColorSpace getColorSpace() {
        return toPDFColorSpace(ColorSpace.getInstance(ColorSpace.CS_GRAY));
    }

    /** {@inheritDoc} */
    @Override
    public int getBitsPerComponent() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public PDFFilter getPDFFilter() {
        return this.pdfFilter;
    }

    /** {@inheritDoc} */
    @Override
    public void outputContents(final OutputStream out) throws IOException {
        getImage().writeTo(out);
    }

    /** {@inheritDoc} */
    @Override
    public String getFilterHint() {
        return PDFFilterList.TIFF_FILTER;
    }

}
