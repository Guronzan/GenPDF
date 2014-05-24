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

/* $Id: PDFCIELabColorSpace.java 1069439 2011-02-10 15:58:57Z jeremias $ */

package org.apache.fop.pdf;

/**
 * This class represents a "CIE L*a*b*" color space. It is expected that the
 * components have the following ranges: L* [0..100], a* and b* [-127..127]
 */
public class PDFCIELabColorSpace extends PDFArray implements PDFColorSpace {

    /**
     * Creates a new "CIE L*a*b*" color space. Valid value ranges for the white
     * and black point are [0..1] as per the PDF spec.
     * 
     * @param whitePoint
     *            the white point
     * @param blackPoint
     *            the optional black point (may be null)
     */
    public PDFCIELabColorSpace(final float[] whitePoint,
            final float[] blackPoint) {
        super();

        add(new PDFName("Lab"));
        final PDFDictionary dict = new PDFDictionary();
        dict.put("WhitePoint", toPDFArray("White point", whitePoint));
        if (whitePoint[1] != 1f) {
            throw new IllegalArgumentException(
                    "The white point's Y coordinate must be 1.0");
        }
        if (blackPoint != null) {
            dict.put("BlackPoint", toPDFArray("Black point", blackPoint));
        }
        dict.put("Range",
                new PDFArray(dict, new int[] { -128, 128, -128, 128 }));
        add(dict);
    }

    private PDFArray toPDFArray(final String name, final float[] whitePoint) {
        final PDFArray wp = new PDFArray();
        if (whitePoint == null || whitePoint.length != 3) {
            throw new IllegalArgumentException(name
                    + " must be given an have 3 components");
        }
        for (int i = 0; i < 3; i++) {
            wp.add(whitePoint[i]);
        }
        return wp;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "CS" + getObjectNumber();
    }

    /** {@inheritDoc} */
    @Override
    public int getNumComponents() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCMYKColorSpace() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDeviceColorSpace() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isGrayColorSpace() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRGBColorSpace() {
        return false;
    }

}
