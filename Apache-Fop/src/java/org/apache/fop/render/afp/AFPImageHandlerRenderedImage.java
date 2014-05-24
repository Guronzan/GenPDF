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

/* $Id: AFPImageHandlerRenderedImage.java 1325277 2012-04-12 14:17:05Z phancock $ */

package org.apache.fop.render.afp;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.afp.AFPImageObjectInfo;
import org.apache.fop.afp.AFPObjectAreaInfo;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPResourceManager;
import org.apache.fop.afp.ioca.ImageContent;
import org.apache.fop.afp.modca.ResourceObject;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.util.bitmap.BitmapImageUtil;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.ImageRendered;
import org.apache.xmlgraphics.image.writer.ImageWriter;
import org.apache.xmlgraphics.image.writer.ImageWriterParams;
import org.apache.xmlgraphics.image.writer.ImageWriterRegistry;
import org.apache.xmlgraphics.ps.ImageEncodingHelper;
import org.apache.xmlgraphics.util.MimeConstants;
import org.apache.xmlgraphics.util.UnitConv;

/**
 * PDFImageHandler implementation which handles RenderedImage instances.
 */
@Slf4j
public class AFPImageHandlerRenderedImage extends AFPImageHandler implements
ImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] {
        ImageFlavor.BUFFERED_IMAGE, ImageFlavor.RENDERED_IMAGE };

    private void setDefaultResourceLevel(
            final AFPImageObjectInfo imageObjectInfo,
            final AFPResourceManager resourceManager) {
        final AFPResourceInfo resourceInfo = imageObjectInfo.getResourceInfo();
        if (!resourceInfo.levelChanged()) {
            resourceInfo.setLevel(resourceManager.getResourceLevelDefaults()
                    .getDefaultResourceLevel(ResourceObject.TYPE_IMAGE));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected AFPDataObjectInfo createDataObjectInfo() {
        return new AFPImageObjectInfo();
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 300;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageRendered.class;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedImageFlavors() {
        return FLAVORS;
    }

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final AFPRenderingContext afpContext = (AFPRenderingContext) context;

        final AFPImageObjectInfo imageObjectInfo = (AFPImageObjectInfo) createDataObjectInfo();
        final AFPPaintingState paintingState = afpContext.getPaintingState();

        // set resource information
        setResourceInformation(imageObjectInfo, image.getInfo()
                .getOriginalURI(), afpContext.getForeignAttributes());
        setDefaultResourceLevel(imageObjectInfo,
                afpContext.getResourceManager());

        // Positioning
        imageObjectInfo.setObjectAreaInfo(createObjectAreaInfo(paintingState,
                pos));
        final Dimension targetSize = pos.getSize();

        // Image content
        final ImageRendered imageRend = (ImageRendered) image;
        final RenderedImageEncoder encoder = new RenderedImageEncoder(
                imageRend, targetSize);
        encoder.prepareEncoding(imageObjectInfo, paintingState);

        final boolean included = afpContext.getResourceManager()
                .tryIncludeObject(imageObjectInfo);
        if (!included) {
            final long start = System.currentTimeMillis();
            // encode only if the same image has not been encoded, yet
            encoder.encodeImage(imageObjectInfo, paintingState);
            if (log.isDebugEnabled()) {
                final long duration = System.currentTimeMillis() - start;
                log.debug("Image encoding took " + duration + "ms.");
            }

            // Create image
            afpContext.getResourceManager().createObject(imageObjectInfo);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        return (image == null || image instanceof ImageRendered)
                && targetContext instanceof AFPRenderingContext;
    }

    private static final class RenderedImageEncoder {

        private enum FunctionSet {

            FS10(MimeConstants.MIME_AFP_IOCA_FS10), FS11(
                    MimeConstants.MIME_AFP_IOCA_FS11), FS45(
                            MimeConstants.MIME_AFP_IOCA_FS45);

            private String mimeType;

            FunctionSet(final String mimeType) {
                this.mimeType = mimeType;
            }

            private String getMimeType() {
                return this.mimeType;
            }
        };

        private final ImageRendered imageRendered;
        private final Dimension targetSize;

        private boolean useFS10;
        private int maxPixelSize;
        private boolean usePageSegments;
        private boolean resample;
        private Dimension resampledDim;
        private ImageSize intrinsicSize;
        private ImageSize effIntrinsicSize;

        private RenderedImageEncoder(final ImageRendered imageRendered,
                final Dimension targetSize) {
            this.imageRendered = imageRendered;
            this.targetSize = targetSize;

        }

        private void prepareEncoding(final AFPImageObjectInfo imageObjectInfo,
                final AFPPaintingState paintingState) {
            this.maxPixelSize = paintingState.getBitsPerPixel();
            if (paintingState.isColorImages()) {
                if (paintingState.isCMYKImagesSupported()) {
                    this.maxPixelSize *= 4; // CMYK is maximum
                } else {
                    this.maxPixelSize *= 3; // RGB is maximum
                }
            }
            final RenderedImage renderedImage = this.imageRendered
                    .getRenderedImage();
            this.useFS10 = this.maxPixelSize == 1
                    || BitmapImageUtil.isMonochromeImage(renderedImage);

            final ImageInfo imageInfo = this.imageRendered.getInfo();
            this.intrinsicSize = imageInfo.getSize();
            this.effIntrinsicSize = this.intrinsicSize;

            final AFPResourceInfo resourceInfo = imageObjectInfo
                    .getResourceInfo();
            this.usePageSegments = this.useFS10
                    && !resourceInfo.getLevel().isInline();
            if (this.usePageSegments) {
                // The image may need to be resized/resampled for use as a page
                // segment
                final int resolution = paintingState.getResolution();
                this.resampledDim = new Dimension((int) Math.ceil(UnitConv
                        .mpt2px(this.targetSize.getWidth(), resolution)),
                        (int) Math.ceil(UnitConv.mpt2px(
                                this.targetSize.getHeight(), resolution)));
                resourceInfo.setImageDimension(this.resampledDim);
                // Only resample/downsample if image is smaller than its
                // intrinsic size
                // to make print file smaller
                this.resample = this.resampledDim.width < renderedImage
                        .getWidth()
                        && this.resampledDim.height < renderedImage.getHeight();
                if (this.resample) {
                    this.effIntrinsicSize = new ImageSize(
                            this.resampledDim.width, this.resampledDim.height,
                            resolution);
                }
            }

            // Update image object info
            imageObjectInfo.setDataHeightRes((int) Math
                    .round(this.effIntrinsicSize.getDpiHorizontal() * 10));
            imageObjectInfo.setDataWidthRes((int) Math
                    .round(this.effIntrinsicSize.getDpiVertical() * 10));
            imageObjectInfo.setDataHeight(this.effIntrinsicSize.getHeightPx());
            imageObjectInfo.setDataWidth(this.effIntrinsicSize.getWidthPx());

            // set object area info
            final int resolution = paintingState.getResolution();
            final AFPObjectAreaInfo objectAreaInfo = imageObjectInfo
                    .getObjectAreaInfo();
            objectAreaInfo.setWidthRes(resolution);
            objectAreaInfo.setHeightRes(resolution);
        }

        private AFPDataObjectInfo encodeImage(
                final AFPImageObjectInfo imageObjectInfo,
                final AFPPaintingState paintingState) throws IOException {

            RenderedImage renderedImage = this.imageRendered.getRenderedImage();
            FunctionSet functionSet = this.useFS10 ? FunctionSet.FS10
                    : FunctionSet.FS11;

            if (this.usePageSegments) {
                assert this.resampledDim != null;
                // Resize, optionally resample and convert image

                imageObjectInfo.setCreatePageSegment(true);

                final float ditheringQuality = paintingState
                        .getDitheringQuality();
                if (this.resample) {
                    if (log.isDebugEnabled()) {
                        log.debug("Resample from "
                                + this.intrinsicSize.getDimensionPx() + " to "
                                + this.resampledDim);
                    }
                    renderedImage = BitmapImageUtil.convertToMonochrome(
                            renderedImage, this.resampledDim, ditheringQuality);
                } else if (ditheringQuality >= 0.5f) {
                    renderedImage = BitmapImageUtil.convertToMonochrome(
                            renderedImage, this.intrinsicSize.getDimensionPx(),
                            ditheringQuality);
                }
            }

            // TODO To reduce AFP file size, investigate using a compression
            // scheme.
            // Currently, all image data is uncompressed.
            final ColorModel cm = renderedImage.getColorModel();
            if (log.isTraceEnabled()) {
                log.trace("ColorModel: " + cm);
            }
            int pixelSize = cm.getPixelSize();
            if (cm.hasAlpha()) {
                pixelSize -= 8;
            }

            byte[] imageData = null;
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final boolean allowDirectEncoding = true;
            if (allowDirectEncoding && pixelSize <= this.maxPixelSize) {
                // Attempt to encode without resampling the image
                final ImageEncodingHelper helper = new ImageEncodingHelper(
                        renderedImage, pixelSize == 32);
                final ColorModel encodedColorModel = helper
                        .getEncodedColorModel();
                boolean directEncode = true;
                if (helper.getEncodedColorModel().getPixelSize() > this.maxPixelSize) {
                    directEncode = false; // pixel size needs to be reduced
                }
                if (BitmapImageUtil.getColorIndexSize(renderedImage) > 2) {
                    directEncode = false; // Lookup tables are not implemented,
                    // yet
                }
                if (this.useFS10
                        && BitmapImageUtil.isMonochromeImage(renderedImage)
                        && BitmapImageUtil.isZeroBlack(renderedImage)) {
                    directEncode = false;
                    // need a special method to invert the bit-stream since
                    // setting the
                    // subtractive mode in AFP alone doesn't seem to do the
                    // trick.
                    if (encodeInvertedBilevel(helper, imageObjectInfo, baos)) {
                        imageData = baos.toByteArray();
                    }
                }
                if (directEncode) {
                    log.debug("Encoding image directly...");
                    imageObjectInfo.setBitsPerPixel(encodedColorModel
                            .getPixelSize());
                    if (pixelSize == 32) {
                        functionSet = FunctionSet.FS45; // IOCA FS45 required
                        // for CMYK
                    }

                    // Lossy or loss-less?
                    if (!paintingState.canEmbedJpeg()
                            && paintingState.getBitmapEncodingQuality() < 1.0f) {
                        try {
                            if (log.isDebugEnabled()) {
                                log.debug("Encoding using baseline DCT (JPEG, q="
                                        + paintingState
                                        .getBitmapEncodingQuality()
                                        + ")...");
                            }
                            encodeToBaselineDCT(renderedImage,
                                    paintingState.getBitmapEncodingQuality(),
                                    paintingState.getResolution(), baos);
                            imageObjectInfo
                            .setCompression(ImageContent.COMPID_JPEG);
                        } catch (final IOException ioe) {
                            // Some JPEG codecs cannot encode CMYK
                            helper.encode(baos);
                        }
                    } else {
                        helper.encode(baos);
                    }
                    imageData = baos.toByteArray();
                }
            }
            if (imageData == null) {
                log.debug("Encoding image via RGB...");
                imageData = encodeViaRGB(renderedImage, imageObjectInfo,
                        paintingState, baos);
            }
            // Should image be FS45?
            if (paintingState.getFS45()) {
                functionSet = FunctionSet.FS45;
            }
            // Wrapping 300+ resolution FS11 IOCA in a page segment is
            // apparently necessary(?)
            imageObjectInfo.setCreatePageSegment((functionSet
                    .equals(FunctionSet.FS11) || functionSet
                    .equals(FunctionSet.FS45))
                    && paintingState.getWrapPSeg());
            imageObjectInfo.setMimeType(functionSet.getMimeType());
            imageObjectInfo.setData(imageData);
            return imageObjectInfo;
        }

        private byte[] encodeViaRGB(final RenderedImage renderedImage,
                final AFPImageObjectInfo imageObjectInfo,
                final AFPPaintingState paintingState,
                final ByteArrayOutputStream baos) throws IOException {
            byte[] imageData;
            // Convert image to 24bit RGB
            ImageEncodingHelper.encodeRenderedImageAsRGB(renderedImage, baos);
            imageData = baos.toByteArray();
            imageObjectInfo.setBitsPerPixel(24);

            final boolean colorImages = paintingState.isColorImages();
            imageObjectInfo.setColor(colorImages);

            // convert to grayscale
            if (!colorImages) {
                log.debug("Converting RGB image to grayscale...");
                baos.reset();
                final int bitsPerPixel = paintingState.getBitsPerPixel();
                imageObjectInfo.setBitsPerPixel(bitsPerPixel);
                // TODO this should be done off the RenderedImage to avoid
                // buffering the
                // intermediate 24bit image
                ImageEncodingHelper.encodeRGBAsGrayScale(imageData,
                        renderedImage.getWidth(), renderedImage.getHeight(),
                        bitsPerPixel, baos);
                imageData = baos.toByteArray();
                if (bitsPerPixel == 1) {
                    imageObjectInfo.setSubtractive(true);
                }
            }
            return imageData;
        }

        /**
         * Efficiently encodes a bi-level image in inverted form as a plain
         * bit-stream.
         *
         * @param helper
         *            the image encoding helper used to analyze the image
         * @param imageObjectInfo
         *            the AFP image object
         * @param out
         *            the output stream
         * @return true if the image was encoded, false if there was something
         *         prohibiting that
         * @throws IOException
         *             if an I/O error occurs
         */
        private boolean encodeInvertedBilevel(final ImageEncodingHelper helper,
                final AFPImageObjectInfo imageObjectInfo, final OutputStream out)
                        throws IOException {
            final RenderedImage renderedImage = helper.getImage();
            if (!BitmapImageUtil.isMonochromeImage(renderedImage)) {
                throw new IllegalStateException(
                        "This method only supports binary images!");
            }
            final int tiles = renderedImage.getNumXTiles()
                    * renderedImage.getNumYTiles();
            if (tiles > 1) {
                return false;
            }
            final SampleModel sampleModel = renderedImage.getSampleModel();
            final SampleModel expectedSampleModel = new MultiPixelPackedSampleModel(
                    DataBuffer.TYPE_BYTE, renderedImage.getWidth(),
                    renderedImage.getHeight(), 1);
            if (!expectedSampleModel.equals(sampleModel)) {
                return false; // Pixels are not packed
            }

            imageObjectInfo.setBitsPerPixel(1);

            final Raster raster = renderedImage.getTile(0, 0);
            final DataBuffer buffer = raster.getDataBuffer();
            if (buffer instanceof DataBufferByte) {
                final DataBufferByte byteBuffer = (DataBufferByte) buffer;
                log.debug("Encoding image as inverted bi-level...");
                final byte[] rawData = byteBuffer.getData();
                int remaining = rawData.length;
                int pos = 0;
                final byte[] data = new byte[4096];
                while (remaining > 0) {
                    final int size = Math.min(remaining, data.length);
                    for (int i = 0; i < size; i++) {
                        data[i] = (byte) ~rawData[pos]; // invert bits
                        pos++;
                    }
                    out.write(data, 0, size);
                    remaining -= size;
                }
                return true;
            }
            return false;
        }

        private void encodeToBaselineDCT(final RenderedImage image,
                final float quality, final int resolution,
                final OutputStream out) throws IOException {
            final ImageWriter writer = ImageWriterRegistry.getInstance()
                    .getWriterFor("image/jpeg");
            final ImageWriterParams params = new ImageWriterParams();
            params.setJPEGQuality(quality, true);
            params.setResolution(resolution);
            writer.writeImage(image, out, params);
        }

    }
}
