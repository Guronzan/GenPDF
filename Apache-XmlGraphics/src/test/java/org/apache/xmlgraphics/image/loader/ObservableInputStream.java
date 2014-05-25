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

/* $Id: ObservableInputStream.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.image.loader;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * This is a proxying input stream that records whether a stream has been closed
 * or not.
 */
@Slf4j
public class ObservableInputStream extends FilterInputStream implements
ObservableStream {

    private boolean closed;
    private final String systemID;

    /**
     * Main constructor.
     *
     * @param in
     *            the underlying input stream
     * @param systemID
     *            the system ID for the input stream for reference
     */
    public ObservableInputStream(final InputStream in, final String systemID) {
        super(in);
        this.systemID = systemID;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        if (!this.closed) {
            log.debug("Stream is being closed: " + getSystemID());
            try {
                this.in.close();
            } catch (final IOException ioe) {
                log.error("Error while closing underlying stream: ", ioe);
            }
            this.closed = true;
        } else {
            throw new IllegalStateException("Stream is already closed!");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isClosed() {
        return this.closed;
    }

    /** {@inheritDoc} */
    @Override
    public String getSystemID() {
        return this.systemID;
    }

}
