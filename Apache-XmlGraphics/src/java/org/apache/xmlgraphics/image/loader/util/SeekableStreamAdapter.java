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

/* $Id: SeekableStreamAdapter.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.image.loader.util;

import java.io.IOException;

import javax.imageio.stream.ImageInputStream;

import org.apache.xmlgraphics.image.codec.util.SeekableStream;

/**
 * Adapter which provides a SeekableStream interface over an ImageInputStream.
 */
public class SeekableStreamAdapter extends SeekableStream {

    private final ImageInputStream iin;

    /**
     * Main constructor
     * 
     * @param iin
     *            the ImageInputStream to operate on
     */
    public SeekableStreamAdapter(final ImageInputStream iin) {
        this.iin = iin;
    }

    /** {@inheritDoc} */
    @Override
    public long getFilePointer() throws IOException {
        return this.iin.getStreamPosition();
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return this.iin.read();
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] b, final int off, final int len)
            throws IOException {
        return this.iin.read(b, off, len);
    }

    /** {@inheritDoc} */
    @Override
    public void seek(final long pos) throws IOException {
        this.iin.seek(pos);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canSeekBackwards() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int skipBytes(final int n) throws IOException {
        return this.iin.skipBytes(n);
    }

}
