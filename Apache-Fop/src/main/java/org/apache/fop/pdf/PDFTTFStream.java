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

/* $Id: PDFTTFStream.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * Special PDFStream for embeddable TrueType fonts.
 */
@Slf4j
public class PDFTTFStream extends AbstractPDFFontStream {

    private final int origLength;
    private byte[] ttfData;

    /**
     * Main constructor
     *
     * @param len
     *            original length
     */
    public PDFTTFStream(final int len) {
        super();
        this.origLength = len;
    }

    /** {@inheritDoc} */
    @Override
    protected int getSizeHint() throws IOException {
        if (this.ttfData != null) {
            return this.ttfData.length;
        } else {
            return 0; // no hint available
        }
    }

    /**
     * Overload the base object method so we don't have to copy byte arrays
     * around so much {@inheritDoc}
     */
    @Override
    public int output(final java.io.OutputStream stream)
            throws java.io.IOException {
        if (log.isDebugEnabled()) {
            log.debug("Writing " + this.origLength + " bytes of TTF font data");
        }

        final int length = super.output(stream);
        log.debug("Embedded TrueType/OpenType font");
        return length;
    }

    /** {@inheritDoc} */
    @Override
    protected void outputRawStreamData(final OutputStream out)
            throws IOException {
        out.write(this.ttfData);
    }

    /** {@inheritDoc} */
    @Override
    protected void populateStreamDict(final Object lengthEntry) {
        put("Length1", this.origLength);
        super.populateStreamDict(lengthEntry);
    }

    /**
     * Sets the TrueType font data.
     *
     * @param data
     *            the font payload
     * @param size
     *            size of the payload
     * @throws IOException
     *             in case of an I/O problem
     */
    public void setData(final byte[] data, final int size) throws IOException {
        this.ttfData = new byte[size];
        System.arraycopy(data, 0, this.ttfData, 0, size);
    }

}
