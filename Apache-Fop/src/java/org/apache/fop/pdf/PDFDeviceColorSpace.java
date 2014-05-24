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

/* $Id: PDFDeviceColorSpace.java 1069439 2011-02-10 15:58:57Z jeremias $ */

package org.apache.fop.pdf;

import java.awt.color.ColorSpace;

/**
 * Represents a device-specific color space. Used for mapping DeviceRGB,
 * DeviceCMYK and DeviceGray.
 */
public class PDFDeviceColorSpace implements PDFColorSpace {

    private int numComponents;

    /**
     * Unknown colorspace
     */
    public static final int DEVICE_UNKNOWN = -1;

    /**
     * Gray colorspace
     */
    public static final int DEVICE_GRAY = 1;

    /**
     * RGB colorspace
     */
    public static final int DEVICE_RGB = 2;

    /**
     * CMYK colorspace
     */
    public static final int DEVICE_CMYK = 3;

    // Are there any others?

    /**
     * Current color space value.
     */
    protected int currentColorSpace = DEVICE_UNKNOWN;

    /**
     * Create a PDF colorspace object.
     *
     * @param theColorSpace
     *            the current colorspace
     */
    public PDFDeviceColorSpace(final int theColorSpace) {
        this.currentColorSpace = theColorSpace;
        this.numComponents = calculateNumComponents();
    }

    private int calculateNumComponents() {
        if (this.currentColorSpace == DEVICE_GRAY) {
            return 1;
        } else if (this.currentColorSpace == DEVICE_RGB) {
            return 3;
        } else if (this.currentColorSpace == DEVICE_CMYK) {
            return 4;
        } else {
            return 0;
        }
    }

    /**
     * Set the current colorspace.
     *
     * @param theColorSpace
     *            the new color space value
     */
    public void setColorSpace(final int theColorSpace) {
        this.currentColorSpace = theColorSpace;
        this.numComponents = calculateNumComponents();
    }

    /**
     * Get the colorspace value
     *
     * @return the colorspace value
     */
    public int getColorSpace() {
        return this.currentColorSpace;
    }

    /**
     * Get the number of color components for this colorspace
     *
     * @return the number of components
     */
    @Override
    public int getNumComponents() {
        return this.numComponents;
    }

    /** @return the name of the color space */
    @Override
    public String getName() {
        switch (this.currentColorSpace) {
        case DEVICE_CMYK:
            return "DeviceCMYK";
        case DEVICE_GRAY:
            return "DeviceGray";
        case DEVICE_RGB:
            return "DeviceRGB";
        default:
            throw new IllegalStateException("Unsupported color space in use.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDeviceColorSpace() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRGBColorSpace() {
        return getColorSpace() == DEVICE_RGB;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCMYKColorSpace() {
        return getColorSpace() == DEVICE_CMYK;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isGrayColorSpace() {
        return getColorSpace() == DEVICE_GRAY;
    }

    /**
     * Returns a suitable {@link PDFDeviceColorSpace} object given a
     * {@link ColorSpace} object.
     * 
     * @param cs
     *            ColorSpace instance
     * @return a PDF-based color space
     */
    public static PDFDeviceColorSpace toPDFColorSpace(final ColorSpace cs) {
        if (cs == null) {
            return null;
        }

        final PDFDeviceColorSpace pdfCS = new PDFDeviceColorSpace(0);
        switch (cs.getType()) {
        case ColorSpace.TYPE_CMYK:
            pdfCS.setColorSpace(PDFDeviceColorSpace.DEVICE_CMYK);
            break;
        case ColorSpace.TYPE_GRAY:
            pdfCS.setColorSpace(PDFDeviceColorSpace.DEVICE_GRAY);
            break;
        default:
            pdfCS.setColorSpace(PDFDeviceColorSpace.DEVICE_RGB);
        }
        return pdfCS;
    }

}
