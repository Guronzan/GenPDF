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

/* $Id: ForwardSeekableStream.java 1070133 2011-02-12 19:10:57Z jeremias $ */

package org.apache.xmlgraphics.image.codec.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * A subclass of <code>SeekableStream</code> that may be used to wrap a regular
 * <code>InputStream</code> efficiently. Seeking backwards is not supported.
 *
 */
public class ForwardSeekableStream extends SeekableStream {

    /** The source <code>InputStream</code>. */
    private final InputStream src;

    /** The current position. */
    long pointer = 0L;

    /**
     * Constructs a <code>InputStreamForwardSeekableStream</code> from a regular
     * <code>InputStream</code>.
     */
    public ForwardSeekableStream(final InputStream src) {
        this.src = src;
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    @Override
    public final int read() throws IOException {
        final int result = this.src.read();
        if (result != -1) {
            ++this.pointer;
        }
        return result;
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    @Override
    public final int read(final byte[] b, final int off, final int len)
            throws IOException {
        final int result = this.src.read(b, off, len);
        if (result != -1) {
            this.pointer += result;
        }
        return result;
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    @Override
    public final long skip(final long n) throws IOException {
        final long skipped = this.src.skip(n);
        this.pointer += skipped;
        return skipped;
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    @Override
    public final int available() throws IOException {
        return this.src.available();
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    @Override
    public final void close() throws IOException {
        this.src.close();
    }

    /**
     * Forwards the request to the real <code>InputStream</code>. We use
     * {@link SeekableStream#markPos}
     */
    @Override
    public final synchronized void mark(final int readLimit) {
        this.markPos = this.pointer;
        this.src.mark(readLimit);
    }

    /**
     * Forwards the request to the real <code>InputStream</code>. We use
     * {@link SeekableStream#markPos}
     */
    @Override
    public final synchronized void reset() throws IOException {
        if (this.markPos != -1) {
            this.pointer = this.markPos;
        }
        this.src.reset();
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    @Override
    public boolean markSupported() {
        return this.src.markSupported();
    }

    /** Returns <code>false</code> since seking backwards is not supported. */
    @Override
    public final boolean canSeekBackwards() {
        return false;
    }

    /** Returns the current position in the stream (bytes read). */
    @Override
    public final long getFilePointer() {
        return this.pointer;
    }

    /**
     * Seeks forward to the given position in the stream. If <code>pos</code> is
     * smaller than the current position as returned by
     * <code>getFilePointer()</code>, nothing happens.
     */
    @Override
    public final void seek(final long pos) throws IOException {
        while (pos - this.pointer > 0) {
            this.pointer += this.src.skip(pos - this.pointer);
        }
    }
}
