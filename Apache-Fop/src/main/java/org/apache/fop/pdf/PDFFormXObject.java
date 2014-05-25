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

/* $Id: PDFFormXObject.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

// Java
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;

/**
 * PDF Form XObject
 *
 * A derivative of the PDFXObject, is a PDF Stream that has not only a
 * dictionary but a stream of image data.
 */
public class PDFFormXObject extends PDFXObject {

    private PDFStream contents;

    /**
     * create a FormXObject with the given number and name and load the image in
     * the object
     *
     * @param xnumber
     *            the pdf object X number
     * @param contents
     *            the form's contents
     * @param resources
     *            the resource PDF reference
     */
    public PDFFormXObject(final int xnumber, final PDFStream contents,
            final PDFReference resources) {
        super(contents.getDictionary());
        put("Name", new PDFName("Form" + xnumber));
        this.contents = contents;

        put("Type", new PDFName("XObject"));
        put("Subtype", new PDFName("Form"));
        put("FormType", new Integer(1));
        setMatrix(new AffineTransform());
        if (resources != null) {
            put("Resources", resources);
        }
    }

    /**
     * Sets the bounding box of the Form XObject.
     * 
     * @param bbox
     *            the bounding box
     */
    public void setBBox(final Rectangle2D bbox) {
        PDFArray array = (PDFArray) get("BBox");
        if (array == null) {
            array = new PDFArray(this);
            array.add(bbox.getX());
            array.add(bbox.getY());
            array.add(bbox.getWidth());
            array.add(bbox.getHeight());
            put("BBox", array);
        } else {
            array.set(0, bbox.getX());
            array.set(1, bbox.getY());
            array.set(2, bbox.getWidth());
            array.set(3, bbox.getHeight());
        }
    }

    /**
     * Returns the bounding box.
     * 
     * @return the BBox value
     */
    public Rectangle2D getBBox() {
        final PDFArray array = (PDFArray) get("BBox");
        if (array != null) {
            final Rectangle2D rect = new Rectangle2D.Double();
            final double x = ((Number) array.get(0)).doubleValue();
            final double y = ((Number) array.get(1)).doubleValue();
            final double w = ((Number) array.get(2)).doubleValue();
            final double h = ((Number) array.get(3)).doubleValue();
            rect.setFrame(x, y, w, h);
            return rect;
        } else {
            return null;
        }
    }

    /**
     * Sets the Matrix value
     * 
     * @param at
     *            the AffineTransform defining the transformation matrix
     */
    public void setMatrix(final AffineTransform at) {
        PDFArray array = (PDFArray) get("Matrix");
        final double[] m = new double[6];
        at.getMatrix(m);
        if (array == null) {
            array = new PDFArray(this);
            array.add(m[0]);
            array.add(m[1]);
            array.add(m[2]);
            array.add(m[3]);
            array.add(m[4]);
            array.add(m[5]);
            put("Matrix", array);
        } else {
            array.set(0, m[0]);
            array.set(1, m[1]);
            array.set(2, m[2]);
            array.set(3, m[3]);
            array.set(4, m[4]);
            array.set(5, m[5]);
        }
    }

    /**
     * Returns the Matrix value.
     * 
     * @return the Matrix
     */
    public AffineTransform getMatrix() {
        final PDFArray array = (PDFArray) get("Matrix");
        if (array != null) {
            final AffineTransform at = new AffineTransform();
            final double m00 = ((Number) array.get(0)).doubleValue();
            final double m10 = ((Number) array.get(1)).doubleValue();
            final double m01 = ((Number) array.get(2)).doubleValue();
            final double m11 = ((Number) array.get(3)).doubleValue();
            final double m02 = ((Number) array.get(4)).doubleValue();
            final double m12 = ((Number) array.get(5)).doubleValue();
            at.setTransform(m00, m10, m01, m11, m02, m12);
            return at;
        } else {
            return null;
        }
    }

    /**
     * Used to set the contents of the PDF stream.
     * 
     * @param data
     *            the contents as a byte array
     * @throws IOException
     *             in case of an I/O problem
     */
    public void setData(final byte[] data) throws IOException {
        this.contents.setData(data);
    }

    /** {@inheritDoc} */
    @Override
    protected void outputRawStreamData(final OutputStream out)
            throws IOException {
        this.contents.outputRawStreamData(out);
    }

    /** {@inheritDoc} */
    @Override
    public int output(final OutputStream stream) throws IOException {
        final int len = super.output(stream);

        // Now that the data has been written, it can be discarded.
        this.contents = null;
        return len;
    }

    /** {@inheritDoc} */
    @Override
    protected void populateStreamDict(final Object lengthEntry) {
        if (get("Matrix") == null) {
            put("Matrix", new PDFArray(this, new int[] { 1, 0, 0, 1, 0, 0 }));
        }
        super.populateStreamDict(lengthEntry);
    }

}
