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

/* $Id: PSImageUtils.java 1353184 2012-06-23 19:18:01Z gadams $ */

package org.apache.xmlgraphics.ps;

import java.awt.Dimension;
import java.awt.color.ColorSpace;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.xmlgraphics.util.io.ASCII85OutputStream;
import org.apache.xmlgraphics.util.io.Finalizable;
import org.apache.xmlgraphics.util.io.FlateEncodeOutputStream;
import org.apache.xmlgraphics.util.io.RunLengthEncodeOutputStream;

// CSOFF: HideUtilityClassConstructor

/**
 * Utility code for rendering images in PostScript.
 */
public class PSImageUtils {

    public PSImageUtils() {
    }

    /**
     * Writes a bitmap image to the PostScript stream.
     *
     * @param encoder
     *            the image encoder
     * @param imgDim
     *            the dimensions of the image
     * @param imgDescription
     *            the name of the image
     * @param targetRect
     *            the target rectangle to place the image in
     * @param colorSpace
     *            the color space of the image
     * @param bitsPerComponent
     *            the number of bits per component
     * @param invertImage
     *            true if the image shall be inverted
     * @param gen
     *            the PostScript generator
     * @throws IOException
     *             In case of an I/O exception
     */
    public static void writeImage(final ImageEncoder encoder,
            final Dimension imgDim, final String imgDescription,
            final Rectangle2D targetRect, final ColorSpace colorSpace,
            final int bitsPerComponent, final boolean invertImage,
            final PSGenerator gen) throws IOException {
        gen.saveGraphicsState();
        translateAndScale(gen, null, targetRect);

        gen.commentln("%AXGBeginBitmap: " + imgDescription);

        gen.writeln("{{");
        // Template: (RawData is used for the EOF signal only)
        // gen.write("/RawData currentfile <first filter> filter def");
        // gen.write("/Data RawData <second filter> <third filter> [...] def");
        final String implicitFilter = encoder.getImplicitFilter();
        if (implicitFilter != null) {
            gen.writeln("/RawData currentfile /ASCII85Decode filter def");
            gen.writeln("/Data RawData " + implicitFilter + " filter def");
        } else {
            if (gen.getPSLevel() >= 3) {
                gen.writeln("/RawData currentfile /ASCII85Decode filter def");
                gen.writeln("/Data RawData /FlateDecode filter def");
            } else {
                gen.writeln("/RawData currentfile /ASCII85Decode filter def");
                gen.writeln("/Data RawData /RunLengthDecode filter def");
            }
        }
        final PSDictionary imageDict = new PSDictionary();
        imageDict.put("/DataSource", "Data");
        imageDict.put("/BitsPerComponent", Integer.toString(bitsPerComponent));
        writeImageCommand(imageDict, imgDim, colorSpace, invertImage, gen);
        /*
         * the following two lines could be enabled if something still goes
         * wrong gen.write("Data closefile"); gen.write("RawData flushfile");
         */
        gen.writeln("} stopped {handleerror} if");
        gen.writeln("  RawData flushfile");
        gen.writeln("} exec");

        compressAndWriteBitmap(encoder, gen);

        gen.newLine();
        gen.commentln("%AXGEndBitmap");
        gen.restoreGraphicsState();
    }

    /**
     * Writes a bitmap image to the PostScript stream.
     *
     * @param encoder
     *            the image encoder
     * @param imgDim
     *            the dimensions of the image
     * @param imgDescription
     *            the name of the image
     * @param targetRect
     *            the target rectangle to place the image in
     * @param colorModel
     *            the color model of the image
     * @param gen
     *            the PostScript generator
     * @throws IOException
     *             In case of an I/O exception
     */
    public static void writeImage(final ImageEncoder encoder,
            final Dimension imgDim, final String imgDescription,
            final Rectangle2D targetRect, final ColorModel colorModel,
            final PSGenerator gen) throws IOException {

        gen.saveGraphicsState();
        translateAndScale(gen, null, targetRect);
        gen.commentln("%AXGBeginBitmap: " + imgDescription);
        gen.writeln("{{");

        final String implicitFilter = encoder.getImplicitFilter();
        if (implicitFilter != null) {
            gen.writeln("/RawData currentfile /ASCII85Decode filter def");
            gen.writeln("/Data RawData " + implicitFilter + " filter def");
        } else {
            if (gen.getPSLevel() >= 3) {
                gen.writeln("/RawData currentfile /ASCII85Decode filter def");
                gen.writeln("/Data RawData /FlateDecode filter def");
            } else {
                gen.writeln("/RawData currentfile /ASCII85Decode filter def");
                gen.writeln("/Data RawData /RunLengthDecode filter def");
            }
        }

        final PSDictionary imageDict = new PSDictionary();
        imageDict.put("/DataSource", "Data");

        populateImageDictionary(imgDim, colorModel, imageDict);
        writeImageCommand(imageDict, colorModel, gen);

        /*
         * the following two lines could be enabled if something still goes
         * wrong gen.write("Data closefile"); gen.write("RawData flushfile");
         */
        gen.writeln("} stopped {handleerror} if");
        gen.writeln("  RawData flushfile");
        gen.writeln("} exec");

        compressAndWriteBitmap(encoder, gen);

        gen.newLine();
        gen.commentln("%AXGEndBitmap");
        gen.restoreGraphicsState();
    }

    private static ColorModel populateImageDictionary(final Dimension imgDim,
            final ColorModel colorModel, final PSDictionary imageDict) {
        final String w = Integer.toString(imgDim.width);
        final String h = Integer.toString(imgDim.height);
        imageDict.put("/ImageType", "1");
        imageDict.put("/Width", w);
        imageDict.put("/Height", h);

        final boolean invertColors = false;
        String decodeArray = getDecodeArray(colorModel.getNumColorComponents(),
                invertColors);
        int bitsPerComp = colorModel.getComponentSize(0);

        // Setup scanning for left-to-right and top-to-bottom
        imageDict.put("/ImageMatrix", "[" + w + " 0 0 " + h + " 0 0]");

        if (colorModel instanceof IndexColorModel) {
            final IndexColorModel indexColorModel = (IndexColorModel) colorModel;
            final int c = indexColorModel.getMapSize();
            final int hival = c - 1;
            if (hival > 4095) {
                throw new UnsupportedOperationException(
                        "hival must not go beyond 4095");
            }
            bitsPerComp = indexColorModel.getPixelSize();
            final int ceiling = (int) Math.pow(2, bitsPerComp) - 1;
            decodeArray = "[0 " + ceiling + "]";
        }
        imageDict.put("/BitsPerComponent", Integer.toString(bitsPerComp));
        imageDict.put("/Decode", decodeArray);
        return colorModel;
    }

    private static String getDecodeArray(final int numComponents,
            final boolean invertColors) {
        String decodeArray;
        final StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < numComponents; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            if (invertColors) {
                sb.append("1 0");
            } else {
                sb.append("0 1");
            }
        }
        sb.append("]");
        decodeArray = sb.toString();
        return decodeArray;
    }

    private static void prepareColorspace(final PSGenerator gen,
            final ColorSpace colorSpace) throws IOException {
        gen.writeln(getColorSpaceName(colorSpace) + " setcolorspace");
    }

    private static void prepareColorSpace(final PSGenerator gen,
            final ColorModel cm) throws IOException {
        // Prepare color space
        if (cm instanceof IndexColorModel) {
            final ColorSpace cs = cm.getColorSpace();
            final IndexColorModel im = (IndexColorModel) cm;
            gen.write("[/Indexed " + getColorSpaceName(cs));
            final int c = im.getMapSize();
            final int hival = c - 1;
            if (hival > 4095) {
                throw new UnsupportedOperationException(
                        "hival must not go beyond 4095");
            }
            gen.writeln(" " + Integer.toString(hival));
            gen.write("  <");
            final int[] palette = new int[c];
            im.getRGBs(palette);
            for (int i = 0; i < c; i++) {
                if (i > 0) {
                    if (i % 8 == 0) {
                        gen.newLine();
                        gen.write("   ");
                    } else {
                        gen.write(" ");
                    }
                }
                gen.write(rgb2Hex(palette[i]));
            }
            gen.writeln(">");
            gen.writeln("] setcolorspace");
        } else {
            gen.writeln(getColorSpaceName(cm.getColorSpace())
                    + " setcolorspace");
        }
    }

    static void writeImageCommand(final RenderedImage img,
            final PSDictionary imageDict, final PSGenerator gen)
                    throws IOException {
        final ImageEncodingHelper helper = new ImageEncodingHelper(img, true);
        final ColorModel cm = helper.getEncodedColorModel();
        final Dimension imgDim = new Dimension(img.getWidth(), img.getHeight());

        populateImageDictionary(imgDim, cm, imageDict);
        writeImageCommand(imageDict, cm, gen);
    }

    static void writeImageCommand(final PSDictionary imageDict,
            final ColorModel cm, final PSGenerator gen) throws IOException {
        prepareColorSpace(gen, cm);
        gen.write(imageDict.toString());
        gen.writeln(" image");
    }

    static void writeImageCommand(final PSDictionary imageDict,
            final Dimension imgDim, final ColorSpace colorSpace,
            final boolean invertImage, final PSGenerator gen)
                    throws IOException {
        imageDict.put("/ImageType", "1");
        imageDict.put("/Width", Integer.toString(imgDim.width));
        imageDict.put("/Height", Integer.toString(imgDim.height));
        final String decodeArray = getDecodeArray(
                colorSpace.getNumComponents(), invertImage);
        imageDict.put("/Decode", decodeArray);
        // Setup scanning for left-to-right and top-to-bottom
        imageDict.put("/ImageMatrix", "[" + imgDim.width + " 0 0 "
                + imgDim.height + " 0 0]");

        prepareColorspace(gen, colorSpace);
        gen.write(imageDict.toString());
        gen.writeln(" image");
    }

    private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private static String rgb2Hex(final int rgb) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 5; i >= 0; i--) {
            final int shift = i * 4;
            final int n = (rgb & 15 << shift) >> shift;
            sb.append(HEX[n % 16]);
        }
        return sb.toString();
    }

    /**
     * Renders a bitmap image to PostScript.
     *
     * @param img
     *            image to render
     * @param x
     *            x position
     * @param y
     *            y position
     * @param w
     *            width
     * @param h
     *            height
     * @param gen
     *            PS generator
     * @throws IOException
     *             In case of an I/O problem while rendering the image
     */
    public static void renderBitmapImage(final RenderedImage img,
            final float x, final float y, final float w, final float h,
            final PSGenerator gen) throws IOException {
        final Rectangle2D targetRect = new Rectangle2D.Double(x, y, w, h);
        final ImageEncoder encoder = ImageEncodingHelper
                .createRenderedImageEncoder(img);
        final Dimension imgDim = new Dimension(img.getWidth(), img.getHeight());
        final String imgDescription = img.getClass().getName();
        final ImageEncodingHelper helper = new ImageEncodingHelper(img);
        final ColorModel cm = helper.getEncodedColorModel();

        writeImage(encoder, imgDim, imgDescription, targetRect, cm, gen);
    }

    /**
     * Paints a reusable image (previously added as a PostScript form).
     *
     * @param form
     *            the PostScript form resource implementing the image
     * @param formDimensions
     *            the original dimensions of the form
     * @param targetRect
     *            the target rectangle to place the image in
     * @param gen
     *            the PostScript generator
     * @throws IOException
     *             In case of an I/O exception
     */
    public static void paintForm(final PSResource form,
            final Dimension2D formDimensions, final Rectangle2D targetRect,
            final PSGenerator gen) throws IOException {
        gen.saveGraphicsState();
        translateAndScale(gen, formDimensions, targetRect);
        gen.writeln(form.getName() + " execform");

        gen.getResourceTracker().notifyResourceUsageOnPage(form);
        gen.restoreGraphicsState();
    }

    private static String getColorSpaceName(final ColorSpace colorSpace) {
        if (colorSpace.getType() == ColorSpace.TYPE_CMYK) {
            return "/DeviceCMYK";
        } else if (colorSpace.getType() == ColorSpace.TYPE_GRAY) {
            return "/DeviceGray";
        } else {
            return "/DeviceRGB";
        }
    }

    static void compressAndWriteBitmap(final ImageEncoder encoder,
            final PSGenerator gen) throws IOException {
        OutputStream out = gen.getOutputStream();
        out = new ASCII85OutputStream(out);
        final String implicitFilter = encoder.getImplicitFilter();
        if (implicitFilter != null) {
            // nop
        } else {
            if (gen.getPSLevel() >= 3) {
                out = new FlateEncodeOutputStream(out);
            } else {
                out = new RunLengthEncodeOutputStream(out);
            }
        }
        encoder.writeTo(out);
        if (out instanceof Finalizable) {
            ((Finalizable) out).finalizeStream();
        } else {
            out.flush();
        }
        gen.newLine(); // Just to be sure
    }

    /**
     * Generates commands to modify the current transformation matrix so an
     * image fits into a given rectangle.
     *
     * @param gen
     *            the PostScript generator
     * @param imageDimensions
     *            the image's dimensions
     * @param targetRect
     *            the target rectangle
     * @throws IOException
     *             if an I/O error occurs
     */
    public static void translateAndScale(final PSGenerator gen,
            Dimension2D imageDimensions, final Rectangle2D targetRect)
                    throws IOException {
        gen.writeln(gen.formatDouble(targetRect.getX()) + " "
                + gen.formatDouble(targetRect.getY()) + " translate");
        if (imageDimensions == null) {
            imageDimensions = new Dimension(1, 1);
        }
        final double sx = targetRect.getWidth() / imageDimensions.getWidth();
        final double sy = targetRect.getHeight() / imageDimensions.getHeight();
        if (sx != 1 || sy != 1) {
            gen.writeln(gen.formatDouble(sx) + " " + gen.formatDouble(sy)
                    + " scale");
        }
    }

    /**
     * Extracts a packed RGB integer array of a RenderedImage.
     *
     * @param img
     *            the image
     * @param startX
     *            the starting X coordinate
     * @param startY
     *            the starting Y coordinate
     * @param w
     *            the width of the cropped image
     * @param h
     *            the height of the cropped image
     * @param rgbArray
     *            the prepared integer array to write to
     * @param offset
     *            offset in the target array
     * @param scansize
     *            width of a row in the target array
     * @return the populated integer array previously passed in as rgbArray
     *         parameter
     */
    public static int[] getRGB(final RenderedImage img, final int startX,
            final int startY, final int w, final int h, int[] rgbArray,
            final int offset, final int scansize) {
        final Raster raster = img.getData();
        int yoff = offset;
        int off;
        Object data;
        final int nbands = raster.getNumBands();
        final int dataType = raster.getDataBuffer().getDataType();
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            data = new byte[nbands];
            break;
        case DataBuffer.TYPE_USHORT:
            data = new short[nbands];
            break;
        case DataBuffer.TYPE_INT:
            data = new int[nbands];
            break;
        case DataBuffer.TYPE_FLOAT:
            data = new float[nbands];
            break;
        case DataBuffer.TYPE_DOUBLE:
            data = new double[nbands];
            break;
        default:
            throw new IllegalArgumentException("Unknown data buffer type: "
                    + dataType);
        }

        if (rgbArray == null) {
            rgbArray = new int[offset + h * scansize];
        }

        final ColorModel colorModel = img.getColorModel();
        for (int y = startY; y < startY + h; y++, yoff += scansize) {
            off = yoff;
            for (int x = startX; x < startX + w; x++) {
                rgbArray[off++] = colorModel.getRGB(raster.getDataElements(x,
                        y, data));
            }
        }

        return rgbArray;
    }

    /**
     * Places an EPS file in the PostScript stream.
     *
     * @param in
     *            the InputStream that contains the EPS stream
     * @param name
     *            name for the EPS document
     * @param viewport
     *            the viewport in points in which to place the EPS
     * @param bbox
     *            the EPS bounding box in points
     * @param gen
     *            the PS generator
     * @throws IOException
     *             in case an I/O error happens during output
     */
    public static void renderEPS(final InputStream in, final String name,
            final Rectangle2D viewport, final Rectangle2D bbox,
            final PSGenerator gen) throws IOException {
        gen.getResourceTracker().notifyResourceUsageOnPage(
                PSProcSets.EPS_PROCSET);
        gen.writeln("%AXGBeginEPS: " + name);
        gen.writeln("BeginEPSF");

        gen.writeln(gen.formatDouble(viewport.getX()) + " "
                + gen.formatDouble(viewport.getY()) + " translate");
        gen.writeln("0 " + gen.formatDouble(viewport.getHeight())
                + " translate");
        gen.writeln("1 -1 scale");
        final double sx = viewport.getWidth() / bbox.getWidth();
        final double sy = viewport.getHeight() / bbox.getHeight();
        if (sx != 1 || sy != 1) {
            gen.writeln(gen.formatDouble(sx) + " " + gen.formatDouble(sy)
                    + " scale");
        }
        if (bbox.getX() != 0 || bbox.getY() != 0) {
            gen.writeln(gen.formatDouble(-bbox.getX()) + " "
                    + gen.formatDouble(-bbox.getY()) + " translate");
        }
        gen.writeln(gen.formatDouble(bbox.getX()) + " "
                + gen.formatDouble(bbox.getY()) + " "
                + gen.formatDouble(bbox.getWidth()) + " "
                + gen.formatDouble(bbox.getHeight()) + " re clip");
        gen.writeln("newpath");

        final PSResource res = new PSResource(PSResource.TYPE_FILE, name);
        gen.getResourceTracker().registerSuppliedResource(res);
        gen.getResourceTracker().notifyResourceUsageOnPage(res);
        gen.writeDSCComment(DSCConstants.BEGIN_DOCUMENT, res.getName());
        IOUtils.copy(in, gen.getOutputStream());
        gen.newLine();
        gen.writeDSCComment(DSCConstants.END_DOCUMENT);
        gen.writeln("EndEPSF");
        gen.writeln("%AXGEndEPS");
    }

}
