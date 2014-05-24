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

/* $Id$ */

package org.apache.fop.afp.ptoca;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.fop.afp.fonts.CharactersetEncoder.EncodedChars;
import org.apache.fop.afp.ptoca.TransparentDataControlSequence.TransparentData;

import static org.apache.fop.afp.ptoca.PtocaConstants.TRANSPARENT_DATA_MAX_SIZE;

/**
 * This object represents a series of PTOCA TransparentData (TRN) control
 * sequences. This implements {@link Iterable} to enable iteration through the
 * TRNs.
 */
final class TransparentDataControlSequence implements Iterable<TransparentData> {

    private static final int MAX_SBCS_TRN_SIZE = TRANSPARENT_DATA_MAX_SIZE;
    // The maximum size of a TRN must be an EVEN number so that we're splitting
    // TRNs on character
    // boundaries rather than in the middle of a double-byte character
    private static final int MAX_DBCS_TRN_SIZE = MAX_SBCS_TRN_SIZE - 1;

    static final class TransparentData {
        private final int offset;
        private final int length;
        private final EncodedChars encodedChars;

        private TransparentData(final int offset, final int length,
                final EncodedChars encChars) {
            this.offset = offset;
            this.length = length;
            this.encodedChars = encChars;
        }

        void writeTo(final OutputStream outStream) throws IOException {
            this.encodedChars.writeTo(outStream, this.offset, this.length);
        }
    }

    private final List<TransparentData> trns;

    /**
     * Converts an encoded String wrapped in an {@link EncodedChars} into a
     * series of {@link TransparentData} control sequences.
     *
     * @param encChars
     *            the encoded characters to convert to TRNs
     */
    public TransparentDataControlSequence(final EncodedChars encChars) {
        final int maxTrnLength = encChars.isDBCS() ? MAX_DBCS_TRN_SIZE
                : MAX_SBCS_TRN_SIZE;
        final int numTransData = encChars.getLength() / maxTrnLength;
        int currIndex = 0;
        final List<TransparentData> trns = new ArrayList<TransparentData>();
        for (int transDataCnt = 0; transDataCnt < numTransData; transDataCnt++) {
            trns.add(new TransparentData(currIndex, maxTrnLength, encChars));
            currIndex += maxTrnLength;
        }
        final int left = encChars.getLength() - currIndex;
        trns.add(new TransparentData(currIndex, left, encChars));
        this.trns = Collections.unmodifiableList(trns);
    }

    /**
     * The {@link Iterator} for retrieving the series of TRN control sequences.
     */
    @Override
    public Iterator<TransparentData> iterator() {
        return this.trns.iterator();
    }
}
