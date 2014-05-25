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

/* $Id: ImageRawStream.java 734655 2009-01-15 10:14:39Z jeremias $ */

package org.apache.xmlgraphics.image.loader.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.MimeEnabledImageFlavor;

/**
 * This class is an implementation of the Image interface exposing an
 * InputStream for loading the raw/undecoded image.
 */
public class ImageRawStream extends AbstractImage {

    private final ImageFlavor flavor;
    private InputStreamFactory streamFactory;

    /**
     * Main constructor.
     * 
     * @param info
     *            the image info object
     * @param flavor
     *            the image flavor for the raw image
     * @param streamFactory
     *            the InputStreamFactory that is used to create InputStream
     *            instances
     */
    public ImageRawStream(final ImageInfo info, final ImageFlavor flavor,
            final InputStreamFactory streamFactory) {
        super(info);
        this.flavor = flavor;
        setInputStreamFactory(streamFactory);
    }

    /**
     * Constructor for a simple InputStream as parameter.
     * 
     * @param info
     *            the image info object
     * @param flavor
     *            the image flavor for the raw image
     * @param in
     *            the InputStream with the raw content
     */
    public ImageRawStream(final ImageInfo info, final ImageFlavor flavor,
            final InputStream in) {
        this(info, flavor, new SingleStreamFactory(in));
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor getFlavor() {
        return this.flavor;
    }

    /**
     * Returns the MIME type of the stream data.
     * 
     * @return the MIME type
     */
    public String getMimeType() {
        if (getFlavor() instanceof MimeEnabledImageFlavor) {
            return getFlavor().getMimeType();
        } else {
            // Undetermined
            return "application/octet-stream";
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCacheable() {
        return !this.streamFactory.isUsedOnceOnly();
    }

    /**
     * Sets the InputStreamFactory to be used by this image. This method allows
     * to replace the original factory.
     * 
     * @param factory
     *            the new InputStreamFactory
     */
    public void setInputStreamFactory(final InputStreamFactory factory) {
        if (this.streamFactory != null) {
            this.streamFactory.close();
        }
        this.streamFactory = factory;
    }

    /**
     * Returns a new InputStream to access the raw image.
     * 
     * @return the InputStream
     */
    public InputStream createInputStream() {
        return this.streamFactory.createInputStream();
    }

    /**
     * Writes the content of the image to an OutputStream. The OutputStream in
     * NOT closed at the end.
     * 
     * @param out
     *            the OutputStream
     * @throws IOException
     *             if an I/O error occurs
     */
    public void writeTo(final OutputStream out) throws IOException {
        final InputStream in = createInputStream();
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Writes the content of the image to a File.
     * 
     * @param target
     *            the file to be written
     * @throws IOException
     *             if an I/O error occurs
     */
    public void writeTo(final File target) throws IOException {
        final OutputStream out = new java.io.FileOutputStream(target);
        try {
            writeTo(out);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Represents a factory for InputStream objects. Make sure the class is
     * thread-safe!
     */
    public interface InputStreamFactory {

        /**
         * Indicates whether this factory is only usable once or many times.
         * 
         * @return true if the factory can only be used once
         */
        boolean isUsedOnceOnly();

        /**
         * Creates and returns a new InputStream.
         * 
         * @return the new InputStream
         */
        InputStream createInputStream();

        /**
         * Closes the factory and releases any resources held open during the
         * lifetime of this object.
         */
        void close();

    }

    /**
     * InputStream factory that can return a pre-constructed InputStream exactly
     * once.
     */
    private static class SingleStreamFactory implements InputStreamFactory {

        private InputStream in;

        public SingleStreamFactory(final InputStream in) {
            this.in = in;
        }

        @Override
        public synchronized InputStream createInputStream() {
            if (this.in != null) {
                final InputStream tempin = this.in;
                this.in = null; // Don't close, just remove the reference
                return tempin;
            } else {
                throw new IllegalStateException(
                        "Can only create an InputStream once!");
            }
        }

        @Override
        public synchronized void close() {
            IOUtils.closeQuietly(this.in);
            this.in = null;
        }

        @Override
        public boolean isUsedOnceOnly() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        protected void finalize() {
            close();
        }

    }

    /**
     * InputStream factory that wraps a byte array.
     */
    public static class ByteArrayStreamFactory implements InputStreamFactory {

        private final byte[] data;

        /**
         * Main constructor.
         * 
         * @param data
         *            the byte array
         */
        public ByteArrayStreamFactory(final byte[] data) {
            this.data = data;
        }

        /** {@inheritDoc} */
        @Override
        public InputStream createInputStream() {
            return new ByteArrayInputStream(this.data);
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
            // nop
        }

        /** {@inheritDoc} */
        @Override
        public boolean isUsedOnceOnly() {
            return false;
        }

    }

}
