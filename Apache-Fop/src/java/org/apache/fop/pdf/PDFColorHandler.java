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

/* $Id: PDFColorHandler.java 1331950 2012-04-29 17:00:36Z gadams $ */

package org.apache.fop.pdf;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.java2d.color.CIELabColorSpace;
import org.apache.xmlgraphics.java2d.color.ColorUtil;
import org.apache.xmlgraphics.java2d.color.ColorWithAlternatives;
import org.apache.xmlgraphics.java2d.color.DeviceCMYKColorSpace;
import org.apache.xmlgraphics.java2d.color.NamedColorSpace;
import org.apache.xmlgraphics.java2d.color.profile.ColorProfileUtil;
import org.apache.xmlgraphics.util.DoubleFormatUtil;

/**
 * This class handles the registration of color spaces and the generation of PDF
 * code to select the right colors given a {@link Color} instance.
 */
@Slf4j
public class PDFColorHandler {

    private final PDFResources resources;

    private Map<String, PDFCIELabColorSpace> cieLabColorSpaces;

    /**
     * Create a new instance for the given {@link PDFResources}
     *
     * @param resources
     *            the PDF resources
     */
    public PDFColorHandler(final PDFResources resources) {
        this.resources = resources;
    }

    private PDFDocument getDocument() {
        return this.resources.getDocumentSafely();
    }

    /**
     * Generates code to select the given color and handles the registration of
     * color spaces in PDF where necessary.
     *
     * @param codeBuffer
     *            the target buffer to receive the color selection code
     * @param color
     *            the color
     * @param fill
     *            true for fill color, false for stroke color
     */
    public void establishColor(final StringBuilder codeBuffer,
            final Color color, final boolean fill) {
        if (color instanceof ColorWithAlternatives) {
            final ColorWithAlternatives colExt = (ColorWithAlternatives) color;
            // Alternate colors have priority
            final Color[] alt = colExt.getAlternativeColors();
            for (final Color col : alt) {
                final boolean established = establishColorFromColor(codeBuffer,
                        col, fill);
                if (established) {
                    return;
                }
            }
            if (log.isDebugEnabled() && alt.length > 0) {
                log.debug("None of the alternative colors are supported. Using fallback: "
                        + color);
            }
        }

        // Fallback
        final boolean established = establishColorFromColor(codeBuffer, color,
                fill);
        if (!established) {
            establishDeviceRGB(codeBuffer, color, fill);
        }
    }

    private boolean establishColorFromColor(final StringBuilder codeBuffer,
            final Color color, final boolean fill) {
        final ColorSpace cs = color.getColorSpace();
        if (cs instanceof DeviceCMYKColorSpace) {
            establishDeviceCMYK(codeBuffer, color, fill);
            return true;
        } else if (!cs.isCS_sRGB()) {
            if (cs instanceof ICC_ColorSpace) {
                final PDFICCBasedColorSpace pdfcs = getICCBasedColorSpace((ICC_ColorSpace) cs);
                establishColor(codeBuffer, pdfcs, color, fill);
                return true;
            } else if (cs instanceof NamedColorSpace) {
                final PDFSeparationColorSpace sepcs = getSeparationColorSpace((NamedColorSpace) cs);
                establishColor(codeBuffer, sepcs, color, fill);
                return true;
            } else if (cs instanceof CIELabColorSpace) {
                final CIELabColorSpace labcs = (CIELabColorSpace) cs;
                final PDFCIELabColorSpace pdflab = getCIELabColorSpace(labcs);
                selectColorSpace(codeBuffer, pdflab, fill);
                final float[] comps = color.getColorComponents(null);
                final float[] nativeComps = labcs.toNativeComponents(comps);
                writeColor(codeBuffer, nativeComps, labcs.getNumComponents(),
                        fill ? "sc" : "SC");
                return true;
            }
        }
        return false;
    }

    private PDFICCBasedColorSpace getICCBasedColorSpace(final ICC_ColorSpace cs) {
        final ICC_Profile profile = cs.getProfile();
        final String desc = ColorProfileUtil.getICCProfileDescription(profile);
        if (log.isDebugEnabled()) {
            log.trace("ICC profile encountered: " + desc);
        }
        PDFICCBasedColorSpace pdfcs = this.resources
                .getICCColorSpaceByProfileName(desc);
        if (pdfcs == null) {
            // color space is not in the PDF, yet
            final PDFFactory factory = getDocument().getFactory();
            final PDFICCStream pdfICCStream = factory.makePDFICCStream();
            final PDFDeviceColorSpace altSpace = PDFDeviceColorSpace
                    .toPDFColorSpace(cs);
            pdfICCStream.setColorSpace(profile, altSpace);
            pdfcs = factory.makeICCBasedColorSpace(null, desc, pdfICCStream);
        }
        return pdfcs;
    }

    private PDFSeparationColorSpace getSeparationColorSpace(
            final NamedColorSpace cs) {
        final PDFName colorName = new PDFName(cs.getColorName());
        PDFSeparationColorSpace sepcs = (PDFSeparationColorSpace) this.resources
                .getColorSpace(colorName);
        if (sepcs == null) {
            // color space is not in the PDF, yet
            final PDFFactory factory = getDocument().getFactory();
            sepcs = factory.makeSeparationColorSpace(null, cs);
        }
        return sepcs;
    }

    private PDFCIELabColorSpace getCIELabColorSpace(final CIELabColorSpace labCS) {
        if (this.cieLabColorSpaces == null) {
            this.cieLabColorSpaces = new java.util.HashMap<String, PDFCIELabColorSpace>();
        }
        final float[] wp = labCS.getWhitePoint();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(wp[i]);
        }
        final String key = sb.toString();
        PDFCIELabColorSpace cielab = this.cieLabColorSpaces.get(key);
        if (cielab == null) {
            // color space is not in the PDF, yet
            final float[] wp1 = new float[] { wp[0] / 100f, wp[1] / 100f,
                    wp[2] / 100f };
            cielab = new PDFCIELabColorSpace(wp1, null);
            getDocument().registerObject(cielab);
            this.resources.addColorSpace(cielab);
            this.cieLabColorSpaces.put(key, cielab);
        }
        return cielab;
    }

    private void establishColor(final StringBuilder codeBuffer,
            final PDFColorSpace pdfcs, final Color color, final boolean fill) {
        selectColorSpace(codeBuffer, pdfcs, fill);
        writeColor(codeBuffer, color, pdfcs.getNumComponents(), fill ? "sc"
                : "SC");
    }

    private void selectColorSpace(final StringBuilder codeBuffer,
            final PDFColorSpace pdfcs, final boolean fill) {
        codeBuffer.append(new PDFName(pdfcs.getName()));
        if (fill) {
            codeBuffer.append(" cs ");
        } else {
            codeBuffer.append(" CS ");
        }
    }

    private void establishDeviceRGB(final StringBuilder codeBuffer,
            final Color color, final boolean fill) {
        float[] comps;
        if (color.getColorSpace().isCS_sRGB()) {
            comps = color.getColorComponents(null);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Converting color to sRGB as a fallback: " + color);
            }
            final ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            comps = color.getColorComponents(sRGB, null);
        }
        if (ColorUtil.isGray(color)) {
            comps = new float[] { comps[0] }; // assuming that all components
            // are the same
            writeColor(codeBuffer, comps, 1, fill ? "g" : "G");
        } else {
            writeColor(codeBuffer, comps, 3, fill ? "rg" : "RG");
        }
    }

    private void establishDeviceCMYK(final StringBuilder codeBuffer,
            final Color color, final boolean fill) {
        writeColor(codeBuffer, color, 4, fill ? "k" : "K");
    }

    private void writeColor(final StringBuilder codeBuffer, final Color color,
            final int componentCount, final String command) {
        final float[] comps = color.getColorComponents(null);
        writeColor(codeBuffer, comps, componentCount, command);
    }

    private void writeColor(final StringBuilder codeBuffer, final float[] comps,
            final int componentCount, final String command) {
        if (comps.length != componentCount) {
            throw new IllegalStateException(
                    "Color with unexpected component count encountered");
        }
        for (final float comp : comps) {
            DoubleFormatUtil.formatDouble(comp, 4, 4, codeBuffer);
            codeBuffer.append(" ");
        }
        codeBuffer.append(command).append("\n");
    }

}
