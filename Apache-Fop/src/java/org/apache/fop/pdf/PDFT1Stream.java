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

/* $Id: PDFT1Stream.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

// Java
import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fonts.type1.PFBData;

/**
 * Special PDFStream for embedding Type 1 fonts.
 */
@Slf4j
public class PDFT1Stream extends AbstractPDFFontStream {

    private PFBData pfb;

    /** {@inheritDoc} */
    @Override
    protected int getSizeHint() throws IOException {
        if (this.pfb != null) {
            return this.pfb.getLength();
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
        if (this.pfb == null) {
            throw new IllegalStateException(
                    "pfb must not be null at this point");
        }
        if (log.isDebugEnabled()) {
            log.debug("Writing " + this.pfb.getLength()
                    + " bytes of Type 1 font data");
        }

        final int length = super.output(stream);
        log.debug("Embedded Type1 font");
        return length;
    }

    /** {@inheritDoc} */
    @Override
    protected void populateStreamDict(final Object lengthEntry) {
        super.populateStreamDict(lengthEntry);
        put("Length1", new Integer(this.pfb.getLength1()));
        put("Length2", new Integer(this.pfb.getLength2()));
        put("Length3", new Integer(this.pfb.getLength3()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void outputRawStreamData(final OutputStream out)
            throws IOException {
        this.pfb.outputAllParts(out);
    }

    /**
     * Used to set the PFBData object that represents the embeddable Type 1
     * font.
     *
     * @param pfb
     *            The PFB file
     * @throws IOException
     *             in case of an I/O problem
     */
    public void setData(final PFBData pfb) throws IOException {
        this.pfb = pfb;
    }

}
