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

/* $Id: GraphicsImage.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.afp.goca;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.afp.util.BinaryUtils;

/**
 * A GOCA Image
 */
public class GraphicsImage extends AbstractGraphicsDrawingOrder {

    /** the maximum image data length */
    public static final short MAX_DATA_LEN = 255;

    /** x coordinate */
    private final int x;

    /** y coordinate */
    private final int y;

    /** width */
    private final int width;

    /** height */
    private final int height;

    /** image data */
    private final byte[] imageData;

    /**
     * Main constructor
     *
     * @param x
     *            the x coordinate of the image
     * @param y
     *            the y coordinate of the image
     * @param width
     *            the image width
     * @param height
     *            the image height
     * @param imageData
     *            the image data
     */
    public GraphicsImage(final int x, final int y, final int width,
            final int height, final byte[] imageData) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.imageData = imageData;
    }

    /** {@inheritDoc} */
    @Override
    public int getDataLength() {
        // TODO:
        return 0;
    }

    @Override
    byte getOrderCode() {
        return (byte) 0xD1;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToStream(final OutputStream os) throws IOException {
        final byte[] xcoord = BinaryUtils.convert(this.x, 2);
        final byte[] ycoord = BinaryUtils.convert(this.y, 2);
        final byte[] w = BinaryUtils.convert(this.width, 2);
        final byte[] h = BinaryUtils.convert(this.height, 2);
        final byte[] startData = new byte[] { getOrderCode(), // GBIMG order
                                                              // code
                (byte) 0x0A, // LENGTH
                xcoord[0], xcoord[1], ycoord[0], ycoord[1], 0x00, // FORMAT
                0x00, // RES
                w[0], // WIDTH
                w[1], //
                h[0], // HEIGHT
                h[1] //
        };
        os.write(startData);

        final byte[] dataHeader = new byte[] { (byte) 0x92 // GIMD
        };
        final int lengthOffset = 1;
        writeChunksToStream(this.imageData, dataHeader, lengthOffset,
                MAX_DATA_LEN, os);

        final byte[] endData = new byte[] { (byte) 0x93, // GEIMG order code
                0x00 // LENGTH
        };
        os.write(endData);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "GraphicsImage{x=" + this.x + ", y=" + this.y + ", width="
                + this.width + ", height=" + this.height + "}";
    }
}
