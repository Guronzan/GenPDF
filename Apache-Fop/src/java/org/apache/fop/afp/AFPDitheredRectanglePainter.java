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

/* $Id: AFPDitheredRectanglePainter.java 1195952 2011-11-01 12:20:21Z phancock $ */

package org.apache.fop.afp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;

import org.apache.fop.afp.modca.triplets.MappingOptionTriplet;
import org.apache.fop.util.bitmap.DitherUtil;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.util.MimeConstants;

/**
 * A painter of rectangles in AFP
 */
public class AFPDitheredRectanglePainter extends AbstractAFPPainter {

    private final AFPResourceManager resourceManager;

    /**
     * Main constructor
     *
     * @param paintingState
     *            the AFP painting state
     * @param dataStream
     *            the AFP datastream
     * @param resourceManager
     *            the resource manager
     */
    public AFPDitheredRectanglePainter(final AFPPaintingState paintingState,
            final DataStream dataStream,
            final AFPResourceManager resourceManager) {
        super(paintingState, dataStream);
        this.resourceManager = resourceManager;
    }

    /** {@inheritDoc} */
    @Override
    public void paint(final PaintingInfo paintInfo) throws IOException {
        final RectanglePaintingInfo rectanglePaintInfo = (RectanglePaintingInfo) paintInfo;
        if (rectanglePaintInfo.getWidth() <= 0
                || rectanglePaintInfo.getHeight() <= 0) {
            return;
        }

        final int ditherMatrix = DitherUtil.DITHER_MATRIX_8X8;
        final Dimension ditherSize = new Dimension(ditherMatrix, ditherMatrix);

        // Prepare an FS10 bi-level image
        final AFPImageObjectInfo imageObjectInfo = new AFPImageObjectInfo();
        imageObjectInfo.setMimeType(MimeConstants.MIME_AFP_IOCA_FS10);
        // imageObjectInfo.setCreatePageSegment(true);
        imageObjectInfo.getResourceInfo().setLevel(
                new AFPResourceLevel(AFPResourceLevel.INLINE));
        imageObjectInfo.getResourceInfo().setImageDimension(ditherSize);
        imageObjectInfo.setBitsPerPixel(1);
        imageObjectInfo.setColor(false);
        // Note: the following may not be supported by older implementations
        imageObjectInfo
                .setMappingOption(MappingOptionTriplet.REPLICATE_AND_TRIM);

        // Dither image size
        final int resolution = this.paintingState.getResolution();
        final ImageSize ditherBitmapSize = new ImageSize(ditherSize.width,
                ditherSize.height, resolution);
        imageObjectInfo.setDataHeightRes((int) Math.round(ditherBitmapSize
                .getDpiHorizontal() * 10));
        imageObjectInfo.setDataWidthRes((int) Math.round(ditherBitmapSize
                .getDpiVertical() * 10));
        imageObjectInfo.setDataWidth(ditherSize.width);
        imageObjectInfo.setDataHeight(ditherSize.height);

        // Create dither image
        final Color col = this.paintingState.getColor();
        final byte[] dither = DitherUtil.getBayerDither(ditherMatrix, col,
                false);
        imageObjectInfo.setData(dither);

        // Positioning
        final int rotation = this.paintingState.getRotation();
        final AffineTransform at = this.paintingState.getData().getTransform();
        final Point2D origin = at.transform(new Point2D.Float(
                rectanglePaintInfo.getX() * 1000,
                rectanglePaintInfo.getY() * 1000), null);
        final AFPUnitConverter unitConv = this.paintingState.getUnitConverter();
        final float width = unitConv.pt2units(rectanglePaintInfo.getWidth());
        final float height = unitConv.pt2units(rectanglePaintInfo.getHeight());
        final AFPObjectAreaInfo objectAreaInfo = new AFPObjectAreaInfo(
                (int) Math.round(origin.getX()),
                (int) Math.round(origin.getY()), Math.round(width),
                Math.round(height), resolution, rotation);
        imageObjectInfo.setObjectAreaInfo(objectAreaInfo);

        // Create rectangle
        this.resourceManager.createObject(imageObjectInfo);
    }

}
