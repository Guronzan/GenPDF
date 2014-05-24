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

/* $Id: ImageObject.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.afp.AFPDataObjectInfo;
import org.apache.fop.afp.AFPImageObjectInfo;
import org.apache.fop.afp.Factory;
import org.apache.fop.afp.ioca.ImageSegment;
import org.apache.xmlgraphics.util.MimeConstants;

/**
 * An IOCA Image Data Object
 */
public class ImageObject extends AbstractDataObject {

    private static final int MAX_DATA_LEN = 8192;

    /** the image segment */
    private ImageSegment imageSegment = null;

    /**
     * Constructor for the image object with the specified name, the name must
     * be a fixed length of eight characters.
     *
     * @param name
     *            The name of the image.
     * @param factory
     *            the resource manager
     */
    public ImageObject(final Factory factory, final String name) {
        super(factory, name);
    }

    /**
     * Returns the image segment object associated with this image object.
     *
     * @return the image segment
     */
    public ImageSegment getImageSegment() {
        if (this.imageSegment == null) {
            this.imageSegment = this.factory.createImageSegment();
        }
        return this.imageSegment;
    }

    /** {@inheritDoc} */
    @Override
    public void setViewport(final AFPDataObjectInfo dataObjectInfo) {
        super.setViewport(dataObjectInfo);

        final AFPImageObjectInfo imageObjectInfo = (AFPImageObjectInfo) dataObjectInfo;
        final int dataWidth = imageObjectInfo.getDataWidth();
        final int dataHeight = imageObjectInfo.getDataHeight();

        final int dataWidthRes = imageObjectInfo.getDataWidthRes();
        final int dataHeightRes = imageObjectInfo.getDataWidthRes();
        final ImageDataDescriptor imageDataDescriptor = this.factory
                .createImageDataDescriptor(dataWidth, dataHeight, dataWidthRes,
                        dataHeightRes);

        if (MimeConstants.MIME_AFP_IOCA_FS45.equals(imageObjectInfo
                .getMimeType())) {
            imageDataDescriptor
            .setFunctionSet(ImageDataDescriptor.FUNCTION_SET_FS45);
        } else if (imageObjectInfo.getBitsPerPixel() == 1) {
            imageDataDescriptor
            .setFunctionSet(ImageDataDescriptor.FUNCTION_SET_FS10);
        }
        getObjectEnvironmentGroup().setDataDescriptor(imageDataDescriptor);
        getObjectEnvironmentGroup().setMapImageObject(
                new MapImageObject(dataObjectInfo.getMappingOption()));

        getImageSegment().setImageSize(dataWidth, dataHeight, dataWidthRes,
                dataHeightRes);
    }

    /**
     * Sets the image encoding.
     *
     * @param encoding
     *            The image encoding.
     */
    public void setEncoding(final byte encoding) {
        getImageSegment().setEncoding(encoding);
    }

    /**
     * Sets the image compression.
     *
     * @param compression
     *            The image compression.
     */
    public void setCompression(final byte compression) {
        getImageSegment().setCompression(compression);
    }

    /**
     * Sets the image IDE size.
     *
     * @param size
     *            The IDE size.
     */
    public void setIDESize(final byte size) {
        getImageSegment().setIDESize(size);
    }

    /**
     * Set the data of the image.
     *
     * @param imageData
     *            the image data
     */
    public void setData(final byte[] imageData) {
        getImageSegment().setData(imageData);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeStart(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.BEGIN, Category.IMAGE);
        os.write(data);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final OutputStream os) throws IOException {
        super.writeContent(os);

        if (this.imageSegment != null) {
            final byte[] dataHeader = new byte[9];
            copySF(dataHeader, SF_CLASS, Type.DATA, Category.IMAGE);
            final int lengthOffset = 1;

            // TODO save memory!
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            this.imageSegment.writeToStream(baos);
            final byte[] data = baos.toByteArray();
            writeChunksToStream(data, dataHeader, lengthOffset, MAX_DATA_LEN,
                    os);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEnd(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.END, Category.IMAGE);
        os.write(data);
    }
}
