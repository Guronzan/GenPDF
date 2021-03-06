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

/* $Id: PSTTFTableOutputStream.java 1352986 2012-06-22 18:07:04Z vhennebert $ */

package org.apache.fop.render.ps.fonts;

import java.io.IOException;

import org.apache.fop.fonts.truetype.TTFTableOutputStream;

/**
 * Streams a TrueType table according to the PostScript format.
 */
public class PSTTFTableOutputStream implements TTFTableOutputStream {

    private final PSTTFGenerator ttfGen;

    /**
     * Constructor.
     * 
     * @param ttfGen
     *            the helper object to stream TrueType data
     */
    public PSTTFTableOutputStream(final PSTTFGenerator ttfGen) {
        this.ttfGen = ttfGen;
    }

    @Override
    public void streamTable(final byte[] ttfData, final int offset,
            final int size) throws IOException {
        int offsetPosition = offset;
        // Need to split the table into MAX_BUFFER_SIZE chunks
        for (int i = 0; i < size / PSTTFGenerator.MAX_BUFFER_SIZE; i++) {
            streamString(ttfData, offsetPosition,
                    PSTTFGenerator.MAX_BUFFER_SIZE);
            offsetPosition += PSTTFGenerator.MAX_BUFFER_SIZE;
        }
        if (size % PSTTFGenerator.MAX_BUFFER_SIZE > 0) {
            streamString(ttfData, offsetPosition, size
                    % PSTTFGenerator.MAX_BUFFER_SIZE);
        }
    }

    private void streamString(final byte[] byteArray, final int offset,
            final int length) throws IOException {
        this.ttfGen.startString();
        this.ttfGen.streamBytes(byteArray, offset, length);
        this.ttfGen.endString();
    }

}
