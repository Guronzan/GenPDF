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

import org.apache.fop.afp.fonts.CharactersetEncoder.EncodedChars;
import org.apache.fop.afp.ptoca.TransparentDataControlSequence.TransparentData;
import org.junit.Test;

import static org.apache.fop.afp.ptoca.PtocaConstants.TRANSPARENT_DATA_MAX_SIZE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransparentDataControlSequenceTestCase {

    private EncodedChars encodedChars;
    private final OutputStream outStream = mock(OutputStream.class);

    @Test
    public void testSingleByteCharacterSet() throws IOException {
        testTRNs(false);
    }

    @Test
    public void testDoubleByteCharacterSets() throws IOException {
        testTRNs(true);
    }

    public void testTRNs(final boolean isDBCS) throws IOException {
        for (int length = 100; length < 10000; length += 1000) {
            createTRNControlSequence(isDBCS, length);
            final int maxTRNSize = TRANSPARENT_DATA_MAX_SIZE - (isDBCS ? 1 : 0);
            final int numberOfTRNs = length / maxTRNSize;
            for (int i = 0; i < numberOfTRNs; i++) {
                verify(this.encodedChars, times(1)).writeTo(this.outStream,
                        i * maxTRNSize, maxTRNSize);
            }
            final int lastOffset = numberOfTRNs * maxTRNSize;
            verify(this.encodedChars, times(1)).writeTo(this.outStream,
                    numberOfTRNs * maxTRNSize, length - lastOffset);
        }
    }

    private void createTRNControlSequence(final boolean isDBCS, final int length)
            throws IOException {
        this.encodedChars = mock(EncodedChars.class);
        when(this.encodedChars.isDBCS()).thenReturn(isDBCS);
        when(this.encodedChars.getLength()).thenReturn(length);
        for (final TransparentData trn : new TransparentDataControlSequence(
                this.encodedChars)) {
            trn.writeTo(this.outStream);
        }
    }
}
