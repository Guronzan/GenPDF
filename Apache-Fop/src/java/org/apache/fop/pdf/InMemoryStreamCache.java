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

/* $Id: InMemoryStreamCache.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * StreamCache implementation that uses temporary files rather than heap.
 */
public class InMemoryStreamCache implements StreamCache {

    private int hintSize = -1;

    /**
     * The current output stream.
     */
    private ByteArrayOutputStream output;

    /**
     * Creates a new InMemoryStreamCache.
     */
    public InMemoryStreamCache() {
    }

    /**
     * Creates a new InMemoryStreamCache.
     * 
     * @param hintSize
     *            a hint about the approximate expected size of the buffer
     */
    public InMemoryStreamCache(final int hintSize) {
        this.hintSize = hintSize;
    }

    /**
     * Get the current OutputStream. Do not store it - it may change from call
     * to call.
     * 
     * @throws IOException
     *             if there is an error getting the output stream
     * @return the output stream containing the data
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        if (this.output == null) {
            if (this.hintSize <= 0) {
                this.output = new ByteArrayOutputStream(512);
            } else {
                this.output = new ByteArrayOutputStream(this.hintSize);
            }
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
     *             if there is an IO error writing to the output stream
     */
    @Override
    public int outputContents(final OutputStream out) throws IOException {
        if (this.output == null) {
            return 0;
        }

        this.output.writeTo(out);
        return this.output.size();
    }

    /**
     * Returns the current size of the stream.
     * 
     * @throws IOException
     *             if there is an error getting the size
     * @return the length of the stream
     */
    @Override
    public int getSize() throws IOException {
        if (this.output == null) {
            return 0;
        } else {
            return this.output.size();
        }
    }

    /**
     * Clears and resets the cache.
     * 
     * @throws IOException
     *             if there is an error closing the stream
     */
    @Override
    public void clear() throws IOException {
        if (this.output != null) {
            this.output.close();
            this.output = null;
        }
    }
}
