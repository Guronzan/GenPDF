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

/* $Id: TempFileStreamCache.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.pdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

/**
 * StreamCache implementation that uses temporary files rather than heap.
 */
public class TempFileStreamCache implements StreamCache {

    /**
     * The current output stream.
     */
    private OutputStream output;

    /**
     * The temp file.
     */
    private final File tempFile;

    /**
     * Creates a new TempFileStreamCache.
     *
     * @throws IOException
     *             if there is an IO error
     */
    public TempFileStreamCache() throws IOException {
        this.tempFile = File.createTempFile("org.apache.fop.pdf.StreamCache-",
                ".temp");
        this.tempFile.deleteOnExit();
    }

    /**
     * Get the current OutputStream. Do not store it - it may change from call
     * to call.
     *
     * @throws IOException
     *             if there is an IO error
     * @return the output stream for this cache
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        if (this.output == null) {
            this.output = new java.io.BufferedOutputStream(
                    new java.io.FileOutputStream(this.tempFile));
        }
        return this.output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final byte[] data) throws IOException {
        getOutputStream().write(data);
    }

    /**
     * Outputs the cached bytes to the given stream.
     *
     * @param out
     *            the output stream to write to
     * @return the number of bytes written
     * @throws IOException
     *             if there is an IO error
     */
    @Override
    public int outputContents(final OutputStream out) throws IOException {
        if (this.output == null) {
            return 0;
        }

        this.output.close();
        this.output = null;

        // don't need a buffer because copy() is buffered
        final InputStream input = new java.io.FileInputStream(this.tempFile);
        try {
            return IOUtils.copy(input, out);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    /**
     * Returns the current size of the stream.
     *
     * @throws IOException
     *             if there is an IO error
     * @return the size of the cache
     */
    @Override
    public int getSize() throws IOException {
        if (this.output != null) {
            this.output.flush();
        }
        return (int) this.tempFile.length();
    }

    /**
     * Clears and resets the cache.
     *
     * @throws IOException
     *             if there is an IO error
     */
    @Override
    public void clear() throws IOException {
        if (this.output != null) {
            this.output.close();
            this.output = null;
        }
        if (this.tempFile.exists()) {
            this.tempFile.delete();
        }
    }
}
