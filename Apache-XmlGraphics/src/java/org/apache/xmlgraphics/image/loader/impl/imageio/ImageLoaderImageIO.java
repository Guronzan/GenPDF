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

/* $Id: ImageLoaderImageIO.java 1353185 2012-06-23 19:18:19Z gadams $ */

package org.apache.xmlgraphics.image.loader.impl.imageio;

import java.awt.Color;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.IIOServiceProvider;
import javax.imageio.stream.ImageInputStream;
import javax.xml.transform.Source;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.impl.AbstractImageLoader;
import org.apache.xmlgraphics.image.loader.impl.ImageBuffered;
import org.apache.xmlgraphics.image.loader.impl.ImageRendered;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.apache.xmlgraphics.java2d.color.profile.ColorProfileUtil;
import org.w3c.dom.Element;

/**
 * An ImageLoader implementation based on ImageIO for loading bitmap images.
 */
@Slf4j
public class ImageLoaderImageIO extends AbstractImageLoader {

    private final ImageFlavor targetFlavor;

    private static final String PNG_METADATA_NODE = "javax_imageio_png_1.0";

    private static final String JPEG_METADATA_NODE = "javax_imageio_jpeg_image_1.0";

    private static final Set providersIgnoringICC = new HashSet(); // CSOK:
    // ConstantName

    /**
     * Main constructor.
     *
     * @param targetFlavor
     *            the target flavor
     */
    public ImageLoaderImageIO(final ImageFlavor targetFlavor) {
        if (!(ImageFlavor.BUFFERED_IMAGE.equals(targetFlavor) || ImageFlavor.RENDERED_IMAGE
                .equals(targetFlavor))) {
            throw new IllegalArgumentException(
                    "Unsupported target ImageFlavor: " + targetFlavor);
        }
        this.targetFlavor = targetFlavor;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getTargetFlavor() {
        return this.targetFlavor;
    }

    /** {@inheritDoc} */
    @Override
    public Image loadImage(final ImageInfo info, final Map hints,
            final ImageSessionContext session) throws ImageException,
            IOException {
        RenderedImage imageData = null;
        IIOException firstException = null;

        IIOMetadata iiometa = (IIOMetadata) info.getCustomObjects().get(
                ImageIOUtil.IMAGEIO_METADATA);
        final boolean ignoreMetadata = iiometa != null;
        boolean providerIgnoresICC = false;

        final Source src = session.needSource(info.getOriginalURI());
        final ImageInputStream imgStream = ImageUtil.needImageInputStream(src);
        try {
            final Iterator<ImageReader> iter = ImageIO
                    .getImageReaders(imgStream);
            while (iter.hasNext()) {
                final ImageReader reader = iter.next();
                try {
                    imgStream.mark();
                    final ImageReadParam param = reader.getDefaultReadParam();
                    reader.setInput(imgStream, false, ignoreMetadata);
                    final int pageIndex = ImageUtil.needPageIndexFromURI(info
                            .getOriginalURI());
                    try {
                        if (ImageFlavor.BUFFERED_IMAGE
                                .equals(this.targetFlavor)) {
                            imageData = reader.read(pageIndex, param);
                        } else {
                            imageData = reader.read(pageIndex, param);
                            // imageData = reader.readAsRenderedImage(pageIndex,
                            // param);
                            // TODO Reenable the above when proper listeners are
                            // implemented
                            // to react to late pixel population (so the stream
                            // can be closed
                            // properly).
                        }
                        if (iiometa == null) {
                            iiometa = reader.getImageMetadata(pageIndex);
                        }
                        providerIgnoresICC = checkProviderIgnoresICC(reader
                                .getOriginatingProvider());
                        break; // Quit early, we have the image
                    } catch (final IndexOutOfBoundsException indexe) {
                        throw new ImageException(
                                "Page does not exist. Invalid image index: "
                                        + pageIndex);
                    } catch (final IllegalArgumentException iae) {
                        // Some codecs like
                        // com.sun.imageio.plugins.wbmp.WBMPImageReader throw
                        // IllegalArgumentExceptions when they have trouble
                        // parsing the image.
                        throw new ImageException(
                                "Error loading image using ImageIO codec", iae);
                    } catch (final IIOException iioe) {
                        if (firstException == null) {
                            firstException = iioe;
                        } else {
                            log.debug("non-first error loading image: "
                                    + iioe.getMessage());
                        }
                    }
                    try {
                        // Try fallback for CMYK images
                        final BufferedImage bi = getFallbackBufferedImage(
                                reader, pageIndex, param);
                        imageData = bi;
                        firstException = null; // Clear exception after
                        // successful fallback attempt
                        break;
                    } catch (final IIOException iioe) {
                        // ignore
                    }
                    imgStream.reset();
                } finally {
                    reader.dispose();
                }
            }
        } finally {
            ImageUtil.closeQuietly(src);
            // TODO Some codecs may do late reading.
        }
        if (firstException != null) {
            throw new ImageException("Error while loading image: "
                    + firstException.getMessage(), firstException);
        }
        if (imageData == null) {
            throw new ImageException("No ImageIO ImageReader found .");
        }

        ColorModel cm = imageData.getColorModel();

        Color transparentColor = null;
        if (cm instanceof IndexColorModel) {
            // transparent color will be extracted later from the image
        } else {
            if (providerIgnoresICC && cm instanceof ComponentColorModel) {
                // Apply ICC Profile to Image by creating a new image with a new
                // color model.
                final ICC_Profile iccProf = tryToExctractICCProfile(iiometa);
                if (iccProf != null) {
                    final ColorModel cm2 = new ComponentColorModel(
                            new ICC_ColorSpace(iccProf), cm.hasAlpha(),
                            cm.isAlphaPremultiplied(), cm.getTransparency(),
                            cm.getTransferType());
                    final WritableRaster wr = Raster.createWritableRaster(
                            imageData.getSampleModel(), null);
                    imageData.copyData(wr);
                    try {
                        final BufferedImage bi = new BufferedImage(cm2, wr,
                                cm2.isAlphaPremultiplied(), null);
                        imageData = bi;
                        cm = cm2;
                    } catch (final IllegalArgumentException iae) {
                        log.warn("Image " + info.getOriginalURI()
                                + " has an incompatible color profile."
                                + " The color profile will be ignored."
                                + "\nColor model of loaded bitmap: " + cm
                                + "\nColor model of color profile: " + cm2);
                    }
                }
            }

            // ImageIOUtil.dumpMetadataToSystemOut(iiometa);
            // Retrieve the transparent color from the metadata
            if (iiometa != null && iiometa.isStandardMetadataFormatSupported()) {
                final Element metanode = (Element) iiometa
                        .getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
                final Element dim = ImageIOUtil.getChild(metanode,
                        "Transparency");
                if (dim != null) {
                    Element child;
                    child = ImageIOUtil.getChild(dim, "TransparentColor");
                    if (child != null) {
                        final String value = child.getAttribute("value");
                        if (value == null || value.length() == 0) {
                            // ignore
                        } else if (cm.getNumColorComponents() == 1) {
                            final int gray = Integer.parseInt(value);
                            transparentColor = new Color(gray, gray, gray);
                        } else {
                            final StringTokenizer st = new StringTokenizer(
                                    value);
                            transparentColor = new Color(Integer.parseInt(st
                                    .nextToken()), Integer.parseInt(st
                                            .nextToken()), Integer.parseInt(st
                                                    .nextToken()));
                        }
                    }
                }
            }
        }

        if (ImageFlavor.BUFFERED_IMAGE.equals(this.targetFlavor)) {
            return new ImageBuffered(info, (BufferedImage) imageData,
                    transparentColor);
        } else {
            return new ImageRendered(info, imageData, transparentColor);
        }
    }

    /**
     * Checks if the provider ignores the ICC color profile. This method will
     * assume providers work correctly, and return false if the provider is
     * unknown. This ensures backward-compatibility.
     *
     * @param provider
     *            the ImageIO Provider
     * @return true if we know the provider to be broken and ignore ICC
     *         profiles.
     */
    private boolean checkProviderIgnoresICC(final IIOServiceProvider provider) {
        // TODO: This information could be cached.
        final StringBuffer b = new StringBuffer(
                provider.getDescription(Locale.ENGLISH));
        b.append('/').append(provider.getVendorName());
        b.append('/').append(provider.getVersion());
        if (log.isDebugEnabled()) {
            log.debug("Image Provider: " + b.toString());
        }
        return ImageLoaderImageIO.providersIgnoringICC.contains(b.toString());
    }

    /**
     * Extract ICC Profile from ImageIO Metadata. This method currently only
     * supports PNG and JPEG metadata.
     *
     * @param iiometa
     *            The ImageIO Metadata
     * @return an ICC Profile or null.
     */
    private ICC_Profile tryToExctractICCProfile(final IIOMetadata iiometa) {
        ICC_Profile iccProf = null;
        final String[] supportedFormats = iiometa.getMetadataFormatNames();
        for (final String format : supportedFormats) {
            final Element root = (Element) iiometa.getAsTree(format);
            if (PNG_METADATA_NODE.equals(format)) {
                iccProf = tryToExctractICCProfileFromPNGMetadataNode(root);
            } else if (JPEG_METADATA_NODE.equals(format)) {
                iccProf = tryToExctractICCProfileFromJPEGMetadataNode(root);
            }
        }
        return iccProf;
    }

    private ICC_Profile tryToExctractICCProfileFromPNGMetadataNode(
            final Element pngNode) {
        ICC_Profile iccProf = null;
        final Element iccpNode = ImageIOUtil.getChild(pngNode, "iCCP");
        if (iccpNode instanceof IIOMetadataNode) {
            final IIOMetadataNode imn = (IIOMetadataNode) iccpNode;
            final byte[] prof = (byte[]) imn.getUserObject();
            final String comp = imn.getAttribute("compressionMethod");
            if ("deflate".equalsIgnoreCase(comp)) {
                final Inflater decompresser = new Inflater();
                decompresser.setInput(prof);
                final byte[] result = new byte[100];
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                boolean failed = false;
                while (!decompresser.finished() && !failed) {
                    try {
                        final int resultLength = decompresser.inflate(result);
                        bos.write(result, 0, resultLength);
                        if (resultLength == 0) {
                            // this means more data or an external dictionary is
                            // needed. Both of which are not available, so we
                            // fail.
                            log.debug("Failed to deflate ICC Profile");
                            failed = true;
                        }
                    } catch (final DataFormatException e) {
                        log.debug("Failed to deflate ICC Profile", e);
                        failed = true;
                    }
                }
                decompresser.end();
                try {
                    iccProf = ColorProfileUtil
                            .getICC_Profile(bos.toByteArray());
                } catch (final IllegalArgumentException e) {
                    log.debug("Failed to interpret embedded ICC Profile", e);
                    iccProf = null;
                }
            }
        }
        return iccProf;
    }

    private ICC_Profile tryToExctractICCProfileFromJPEGMetadataNode(
            final Element jpgNode) {
        ICC_Profile iccProf = null;
        final Element jfifNode = ImageIOUtil.getChild(jpgNode, "app0JFIF");
        if (jfifNode != null) {
            final Element app2iccNode = ImageIOUtil.getChild(jfifNode,
                    "app2ICC");
            if (app2iccNode instanceof IIOMetadataNode) {
                final IIOMetadataNode imn = (IIOMetadataNode) app2iccNode;
                iccProf = (ICC_Profile) imn.getUserObject();
            }
        }
        return iccProf;
    }

    private BufferedImage getFallbackBufferedImage(final ImageReader reader,
            final int pageIndex, final ImageReadParam param) throws IOException {
        // Work-around found at:
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4799903
        // There are some additional ideas there if someone wants to go further.

        // Try reading a Raster (no color conversion).
        final Raster raster = reader.readRaster(pageIndex, param);

        // Arbitrarily select a BufferedImage type.
        int imageType;
        final int numBands = raster.getNumBands();
        switch (numBands) {
        case 1:
            imageType = BufferedImage.TYPE_BYTE_GRAY;
            break;
        case 3:
            imageType = BufferedImage.TYPE_3BYTE_BGR;
            break;
        case 4:
            imageType = BufferedImage.TYPE_4BYTE_ABGR;
            break;
        default:
            throw new UnsupportedOperationException("Unsupported band count: "
                    + numBands);
        }

        // Create a BufferedImage.
        final BufferedImage bi = new BufferedImage(raster.getWidth(),
                raster.getHeight(), imageType);

        // Set the image data.
        bi.getRaster().setRect(raster);
        return bi;
    }

    static {
        // TODO: This list could be kept in a resource file.
        providersIgnoringICC
                .add("Standard PNG image reader/Sun Microsystems, Inc./1.0");
        providersIgnoringICC
                .add("Standard JPEG Image Reader/Sun Microsystems, Inc./0.5");
    }
}
