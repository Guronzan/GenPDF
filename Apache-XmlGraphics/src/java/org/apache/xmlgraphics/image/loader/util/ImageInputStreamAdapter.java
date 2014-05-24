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

/* $Id: ImageInputStreamAdapter.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.image.loader.util;

import java.io.IOException;
import java.io.InputStream;

import javax.imageio.stream.ImageInputStream;

/**
 * Decorates an ImageInputStream with an InputStream interface. The methods
 * <code>mark()</code> and <code>reset()</code> are fully supported. The method
 * <code>available()</code> will always return 0.
 */
public class ImageInputStreamAdapter extends InputStream {

    private ImageInputStream iin;

    private long lastMarkPosition;

    /**
     * Creates a new ImageInputStreamAdapter.
     * 
     * @param iin
     *            the underlying ImageInputStream
     */
    public ImageInputStreamAdapter(final ImageInputStream iin) {
        assert iin != null : "InputStream is null";
        this.iin = iin;
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] b, final int off, final int len)
            throws IOException {
        return this.iin.read(b, off, len);
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] b) throws IOException {
        return this.iin.read(b);
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return this.iin.read();
    }

    /** {@inheritDoc} */
    @Override
    public long skip(final long n) throws IOException {
        return this.iin.skipBytes(n);
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        this.iin.close();
        this.iin = null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void mark(final int readlimit) {
        // Parameter readlimit is ignored
        try {
            // Cannot use mark()/reset() since they are nestable, and
            // InputStream's are not
            this.lastMarkPosition = this.iin.getStreamPosition();
        } catch (final IOException ioe) {
            throw new RuntimeException(
                    "Unexpected IOException in ImageInputStream.getStreamPosition()",
                    ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean markSupported() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void reset() throws IOException {
        this.iin.seek(this.lastMarkPosition);
    }

    /** {@inheritDoc} */
    @Override
    public int available() throws IOException {
        return 0;
    }

}
