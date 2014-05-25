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

/* $Id: ImageIOImageWriter.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.image.writer.imageio;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import org.apache.xmlgraphics.image.writer.ImageWriter;
import org.apache.xmlgraphics.image.writer.ImageWriterParams;
import org.apache.xmlgraphics.image.writer.MultiImageWriter;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * ImageWriter implementation that uses Image I/O to write images.
 *
 * @version $Id: ImageIOImageWriter.java 750418 2009-03-05 11:03:54Z vhennebert
 *          $
 */
public class ImageIOImageWriter implements ImageWriter, IIOWriteWarningListener {

    private static final String STANDARD_METADATA_FORMAT = "javax_imageio_1.0";

    private final String targetMIME;

    /**
     * Main constructor.
     * 
     * @param mime
     *            the MIME type of the image format
     */
    public ImageIOImageWriter(final String mime) {
        this.targetMIME = mime;
    }

    /**
     * @see ImageWriter#writeImage(java.awt.image.RenderedImage,
     *      java.io.OutputStream)
     */
    @Override
    public void writeImage(final RenderedImage image, final OutputStream out)
            throws IOException {
        writeImage(image, out, null);
    }

    /**
     * @see ImageWriter#writeImage(java.awt.image.RenderedImage,
     *      java.io.OutputStream, ImageWriterParams)
     */
    @Override
    public void writeImage(final RenderedImage image, final OutputStream out,
            final ImageWriterParams params) throws IOException {
        final javax.imageio.ImageWriter iiowriter = getIIOImageWriter();
        iiowriter.addIIOWriteWarningListener(this);

        final ImageOutputStream imgout = ImageIO.createImageOutputStream(out);
        try {

            final ImageWriteParam iwParam = getDefaultWriteParam(iiowriter,
                    image, params);

            ImageTypeSpecifier type;
            if (iwParam.getDestinationType() != null) {
                type = iwParam.getDestinationType();
            } else {
                type = ImageTypeSpecifier.createFromRenderedImage(image);
            }

            // Handle metadata
            IIOMetadata meta = iiowriter.getDefaultImageMetadata(type, iwParam);
            // meta might be null for some JAI codecs as they don't support
            // metadata
            if (params != null && meta != null) {
                meta = updateMetadata(meta, params);
            }

            // Write image
            iiowriter.setOutput(imgout);
            final IIOImage iioimg = new IIOImage(image, null, meta);
            iiowriter.write(null, iioimg, iwParam);

        } finally {
            imgout.close();
            iiowriter.dispose();
        }
    }

    private javax.imageio.ImageWriter getIIOImageWriter() {
        final Iterator iter = ImageIO.getImageWritersByMIMEType(getMIMEType());
        javax.imageio.ImageWriter iiowriter = null;
        if (iter.hasNext()) {
            iiowriter = (javax.imageio.ImageWriter) iter.next();
        }
        if (iiowriter == null) {
            throw new UnsupportedOperationException(
                    "No ImageIO codec for writing " + getMIMEType()
                            + " is available!");
        }
        return iiowriter;
    }

    /**
     * Returns the default write parameters for encoding the image.
     * 
     * @param iiowriter
     *            The IIO ImageWriter that will be used
     * @param image
     *            the image to be encoded
     * @param params
     *            the parameters for this writer instance
     * @return the IIO ImageWriteParam instance
     */
    protected ImageWriteParam getDefaultWriteParam(
            final javax.imageio.ImageWriter iiowriter,
            final RenderedImage image, final ImageWriterParams params) {
        final ImageWriteParam param = iiowriter.getDefaultWriteParam();
        // System.err.println("Param: " + params);
        if (params != null && params.getCompressionMethod() != null) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType(params.getCompressionMethod());
        }
        return param;
    }

    /**
     * Updates the metadata information based on the parameters to this writer.
     * 
     * @param meta
     *            the metadata
     * @param params
     *            the parameters
     * @return the updated metadata
     */
    protected IIOMetadata updateMetadata(final IIOMetadata meta,
            final ImageWriterParams params) {
        if (meta.isStandardMetadataFormatSupported()) {
            final IIOMetadataNode root = (IIOMetadataNode) meta
                    .getAsTree(STANDARD_METADATA_FORMAT);
            final IIOMetadataNode dim = getChildNode(root, "Dimension");
            IIOMetadataNode child;
            if (params.getResolution() != null) {
                child = getChildNode(dim, "HorizontalPixelSize");
                if (child == null) {
                    child = new IIOMetadataNode("HorizontalPixelSize");
                    dim.appendChild(child);
                }
                child.setAttribute("value", Double.toString(params
                        .getResolution().doubleValue() / 25.4));
                child = getChildNode(dim, "VerticalPixelSize");
                if (child == null) {
                    child = new IIOMetadataNode("VerticalPixelSize");
                    dim.appendChild(child);
                }
                child.setAttribute("value", Double.toString(params
                        .getResolution().doubleValue() / 25.4));
            }
            try {
                meta.mergeTree(STANDARD_METADATA_FORMAT, root);
            } catch (final IIOInvalidTreeException e) {
                throw new RuntimeException("Cannot update image metadata: "
                        + e.getMessage());
            }
        }
        return meta;
    }

    /**
     * Returns a specific metadata child node
     * 
     * @param n
     *            the base node
     * @param name
     *            the name of the child
     * @return the requested child node
     */
    protected static IIOMetadataNode getChildNode(final Node n,
            final String name) {
        final NodeList nodes = n.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node child = nodes.item(i);
            if (name.equals(child.getNodeName())) {
                return (IIOMetadataNode) child;
            }
        }
        return null;
    }

    /** @see ImageWriter#getMIMEType() */
    @Override
    public String getMIMEType() {
        return this.targetMIME;
    }

    /** @see org.apache.xmlgraphics.image.writer.ImageWriter#isFunctional() */
    @Override
    public boolean isFunctional() {
        final Iterator iter = ImageIO.getImageWritersByMIMEType(getMIMEType());
        // Only return true if an IIO ImageWriter is available in the current
        // environment
        return iter.hasNext();
    }

    /**
     * @see javax.imageio.event.IIOWriteWarningListener#warningOccurred(javax.imageio.ImageWriter,
     *      int, java.lang.String)
     */
    @Override
    public void warningOccurred(final javax.imageio.ImageWriter source,
            final int imageIndex, final String warning) {
        System.err.println("Problem while writing image using ImageI/O: "
                + warning);
    }

    /**
     * @see org.apache.xmlgraphics.image.writer.ImageWriter#createMultiImageWriter(java.io.OutputStream)
     */
    @Override
    public MultiImageWriter createMultiImageWriter(final OutputStream out)
            throws IOException {
        return new IIOMultiImageWriter(out);
    }

    /** @see org.apache.xmlgraphics.image.writer.ImageWriter#supportsMultiImageWriter() */
    @Override
    public boolean supportsMultiImageWriter() {
        final javax.imageio.ImageWriter iiowriter = getIIOImageWriter();
        try {
            return iiowriter.canWriteSequence();
        } finally {
            iiowriter.dispose();
        }
    }

    private class IIOMultiImageWriter implements MultiImageWriter {

        private javax.imageio.ImageWriter iiowriter;
        private ImageOutputStream imageStream;

        public IIOMultiImageWriter(final OutputStream out) throws IOException {
            this.iiowriter = getIIOImageWriter();
            if (!this.iiowriter.canWriteSequence()) {
                throw new UnsupportedOperationException(
                        "This ImageWriter does not support writing"
                                + " multiple images to a single image file.");
            }
            this.iiowriter.addIIOWriteWarningListener(ImageIOImageWriter.this);

            this.imageStream = ImageIO.createImageOutputStream(out);
            this.iiowriter.setOutput(this.imageStream);
            this.iiowriter.prepareWriteSequence(null);
        }

        @Override
        public void writeImage(final RenderedImage image,
                final ImageWriterParams params) throws IOException {
            if (this.iiowriter == null) {
                throw new IllegalStateException(
                        "MultiImageWriter already closed!");
            }
            final ImageWriteParam iwParam = getDefaultWriteParam(
                    this.iiowriter, image, params);

            ImageTypeSpecifier type;
            if (iwParam.getDestinationType() != null) {
                type = iwParam.getDestinationType();
            } else {
                type = ImageTypeSpecifier.createFromRenderedImage(image);
            }

            // Handle metadata
            IIOMetadata meta = this.iiowriter.getDefaultImageMetadata(type,
                    iwParam);
            // meta might be null for some JAI codecs as they don't support
            // metadata
            if (params != null && meta != null) {
                meta = updateMetadata(meta, params);
            }

            // Write image
            final IIOImage iioimg = new IIOImage(image, null, meta);
            this.iiowriter.writeToSequence(iioimg, iwParam);
        }

        @Override
        public void close() throws IOException {
            this.imageStream.close();
            this.imageStream = null;
            this.iiowriter.dispose();
            this.iiowriter = null;
        }

    }

}
