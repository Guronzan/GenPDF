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

/* $Id: PDFImageXObject.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.pdf;

// Java
import java.io.IOException;
import java.io.OutputStream;

/* modified by JKT to integrate with 0.12.0 */
/* modified by Eric SCHAEFFER to integrate with 0.13.0 */

/**
 * PDF XObject
 *
 * A derivative of the PDF Object, is a PDF Stream that has not only a
 * dictionary but a stream of image data. The dictionary just provides
 * information like the stream length. This outputs the image dictionary and the
 * image data. This is used as a reference for inserting the same image in the
 * document in another place.
 */
public class PDFImageXObject extends PDFXObject {

    private PDFImage pdfimage;

    /**
     * create an XObject with the given number and name and load the image in
     * the object
     *
     * @param xnumber
     *            the pdf object X number
     * @param img
     *            the pdf image that contains the image data
     */
    public PDFImageXObject(final int xnumber, final PDFImage img) {
        super();
        put("Name", new PDFName("Im" + xnumber));
        this.pdfimage = img;
    }

    /**
     * Output the image as PDF. This sets up the image dictionary and adds the
     * image data stream.
     *
     * @param stream
     *            the output stream to write the data
     * @throws IOException
     *             if there is an error writing the data
     * @return the length of the data written
     */
    @Override
    public int output(final OutputStream stream) throws IOException {
        final int length = super.output(stream);

        // let it gc
        // this object is retained as a reference to inserting
        // the same image but the image data is no longer needed
        this.pdfimage = null;
        return length;
    }

    /** {@inheritDoc} */
    @Override
    protected void populateStreamDict(final Object lengthEntry) {
        super.populateStreamDict(lengthEntry);
        if (this.pdfimage.isPS()) {
            populateDictionaryFromPS();
        } else {
            populateDictionaryFromImage();
        }
    }

    private void populateDictionaryFromPS() {
        getDocumentSafely().getProfile().verifyPSXObjectsAllowed();
        put("Subtype", new PDFName("PS"));
    }

    private void populateDictionaryFromImage() {
        put("Subtype", new PDFName("Image"));
        put("Width", new Integer(this.pdfimage.getWidth()));
        put("Height", new Integer(this.pdfimage.getHeight()));
        put("BitsPerComponent",
                new Integer(this.pdfimage.getBitsPerComponent()));

        final PDFICCStream pdfICCStream = this.pdfimage.getICCStream();
        if (pdfICCStream != null) {
            put("ColorSpace", new PDFArray(this, new Object[] {
                    new PDFName("ICCBased"), pdfICCStream }));
        } else {
            final PDFDeviceColorSpace cs = this.pdfimage.getColorSpace();
            put("ColorSpace", new PDFName(cs.getName()));
        }

        if (this.pdfimage.isInverted()) {
            /*
             * PhotoShop generates CMYK values that's inverse, this will invert
             * the values - too bad if it's not a PhotoShop image...
             */
            final Float zero = new Float(0.0f);
            final Float one = new Float(1.0f);
            final PDFArray decode = new PDFArray(this);
            for (int i = 0, c = this.pdfimage.getColorSpace()
                    .getNumComponents(); i < c; i++) {
                decode.add(one);
                decode.add(zero);
            }
            put("Decode", decode);
        }

        if (this.pdfimage.isTransparent()) {
            final PDFColor transp = this.pdfimage.getTransparentColor();
            final PDFArray mask = new PDFArray(this);
            if (this.pdfimage.getColorSpace().isGrayColorSpace()) {
                mask.add(new Integer(transp.red255()));
                mask.add(new Integer(transp.red255()));
            } else {
                mask.add(new Integer(transp.red255()));
                mask.add(new Integer(transp.red255()));
                mask.add(new Integer(transp.green255()));
                mask.add(new Integer(transp.green255()));
                mask.add(new Integer(transp.blue255()));
                mask.add(new Integer(transp.blue255()));
            }
            put("Mask", mask);
        }
        final PDFReference ref = this.pdfimage.getSoftMaskReference();
        if (ref != null) {
            put("SMask", ref);
        }
        // Important: do this at the end so previous values can be overwritten.
        this.pdfimage.populateXObjectDictionary(getDictionary());
    }

    /** {@inheritDoc} */
    @Override
    protected void outputRawStreamData(final OutputStream out)
            throws IOException {
        this.pdfimage.outputContents(out);
    }

    /** {@inheritDoc} */
    @Override
    protected int getSizeHint() throws IOException {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    protected void prepareImplicitFilters() {
        final PDFFilter pdfFilter = this.pdfimage.getPDFFilter();
        if (pdfFilter != null) {
            getFilterList().ensureFilterInPlace(pdfFilter);
        }
    }

    /**
     * {@inheritDoc} This class uses the PDFImage instance to determine the
     * default filter.
     */
    @Override
    protected String getDefaultFilterName() {
        return this.pdfimage.getFilterHint();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean multipleFiltersAllowed() {
        return this.pdfimage.multipleFiltersAllowed();
    }

}
