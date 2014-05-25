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

/* $Id: PreloaderBMP.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.image.loader.impl;

import java.io.IOException;
import java.nio.ByteOrder;

import javax.imageio.stream.ImageInputStream;
import javax.xml.transform.Source;

import org.apache.xmlgraphics.image.loader.ImageContext;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.apache.xmlgraphics.util.UnitConv;

/**
 * Image preloader for BMP images.
 */
public class PreloaderBMP extends AbstractImagePreloader {

    /** Length of the BMP header */
    protected static final int BMP_SIG_LENGTH = 2;

    /** offset to width */
    private static final int WIDTH_OFFSET = 18;

    /** {@inheritDoc} */
    @Override
    public ImageInfo preloadImage(final String uri, final Source src,
            final ImageContext context) throws IOException, ImageException {
        if (!ImageUtil.hasImageInputStream(src)) {
            return null;
        }
        final ImageInputStream in = ImageUtil.needImageInputStream(src);
        final byte[] header = getHeader(in, BMP_SIG_LENGTH);
        final boolean supported = header[0] == (byte) 0x42
                && header[1] == (byte) 0x4d;

        if (supported) {
            final ImageInfo info = new ImageInfo(uri, "image/bmp");
            info.setSize(determineSize(in, context));
            return info;
        } else {
            return null;
        }
    }

    private ImageSize determineSize(final ImageInputStream in,
            final ImageContext context) throws IOException {
        in.mark();
        final ByteOrder oldByteOrder = in.getByteOrder();
        try {
            final ImageSize size = new ImageSize();

            // BMP uses little endian notation!
            in.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            in.skipBytes(WIDTH_OFFSET);
            final int width = in.readInt();
            final int height = in.readInt();
            size.setSizeInPixels(width, height);

            in.skipBytes(12);
            final int xRes = in.readInt();
            double xResDPI = UnitConv.in2mm(xRes / 1000d);
            if (xResDPI == 0) {
                xResDPI = context.getSourceResolution();
            }

            final int yRes = in.readInt();
            double yResDPI = UnitConv.in2mm(yRes / 1000d);
            if (yResDPI == 0) {
                yResDPI = context.getSourceResolution();
            }

            size.setResolution(xResDPI, yResDPI);
            size.calcSizeFromPixels();
            return size;
        } finally {
            in.setByteOrder(oldByteOrder);
            in.reset();
        }
    }

}
