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

/* $Id: PSTTFGlyphOutputStreamTestCase.java 1352986 2012-06-22 18:07:04Z vhennebert $ */

package org.apache.fop.render.ps.fonts;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.fail;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class for PSTTFGlyphOutputStream
 */
public class PSTTFGlyphOutputStreamTestCase {
    private PSTTFGenerator mockGen;
    private PSTTFGlyphOutputStream glyphOut;

    @Before
    public void setUp() {
        this.mockGen = mock(PSTTFGenerator.class);
        this.glyphOut = new PSTTFGlyphOutputStream(this.mockGen);
    }

    /**
     * Test startGlyphStream() - test that startGlyphStream() invokes reset()
     * and startString() in PSTTFGenerator.
     * 
     * @exception IOException
     *                file write error
     */
    @Test
    public void testStartGlyphStream() throws IOException {
        this.glyphOut.startGlyphStream();
        verify(this.mockGen).startString();
    }

    /**
     * Test streamGlyph(byte[],int,int) - tests several paths: 1) strings are
     * properly appended 2) when total strings size >
     * PSTTFGenerator.MAX_BUFFER_SIZE, the strings is closed and a new strings
     * is started. 3) if a glyph of size > PSTTFGenerator.MAX_BUFFER_SIZE is
     * attempted, an exception is thrown.
     * 
     * @throws IOException
     *             file write error.
     */
    @Test
    public void testStreamGlyph() throws IOException {
        final int byteArraySize = 10;
        final byte[] byteArray = new byte[byteArraySize];
        final int runs = 100;
        for (int i = 0; i < runs; i++) {
            this.glyphOut.streamGlyph(byteArray, 0, byteArraySize);
        }
        verify(this.mockGen, times(runs)).streamBytes(byteArray, 0,
                byteArraySize);

        /*
         * We want to run this for MAX_BUFFER_SIZE / byteArraySize so that go
         * over the string boundary and enforce the ending and starting of a new
         * string. Using mockito to ensure that this behaviour is performed in
         * order (since this is an integral behavioural aspect)
         */
        final int stringLimit = PSTTFGenerator.MAX_BUFFER_SIZE / byteArraySize;
        for (int i = 0; i < stringLimit; i++) {
            this.glyphOut.streamGlyph(byteArray, 0, byteArraySize);
        }
        final InOrder inOrder = inOrder(this.mockGen);
        inOrder.verify(this.mockGen, times(stringLimit)).streamBytes(byteArray,
                0, byteArraySize);
        inOrder.verify(this.mockGen).endString();
        inOrder.verify(this.mockGen).startString();
        inOrder.verify(this.mockGen, times(runs)).streamBytes(byteArray, 0,
                byteArraySize);

        try {
            this.glyphOut.streamGlyph(byteArray, 0,
                    PSTTFGenerator.MAX_BUFFER_SIZE + 1);
            fail("Shouldn't allow a length > PSTTFGenerator.MAX_BUFFER_SIZE");
        } catch (final UnsupportedOperationException e) {
            // PASS
        }
    }

    /**
     * Test endGlyphStream() - tests that PSTTFGenerator.endString() is invoked
     * when this method is called.
     * 
     * @throws IOException
     *             file write exception
     */
    @Test
    public void testEndGlyphStream() throws IOException {
        this.glyphOut.endGlyphStream();
        verify(this.mockGen).endString();
    }
}
