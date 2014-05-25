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

/* $Id: PSTTFTableOutputStreamTestCase.java 1352986 2012-06-22 18:07:04Z vhennebert $ */

package org.apache.fop.render.ps.fonts;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Test class for unit testing PSTTFTableOutputStream
 */
public class PSTTFTableOutputStreamTestCase {
    private PSTTFGenerator mockGen;
    private PSTTFTableOutputStream tableOut;

    @Before
    public void setUp() {
        this.mockGen = mock(PSTTFGenerator.class);
        this.tableOut = new PSTTFTableOutputStream(this.mockGen);
    }

    /**
     * Test streamTable() - several paths to test (2. and 3. test corner cases):
     * 1) that a table of length < PSTTFGenerator.MAX_BUFFER_SIZE invokes the
     * correct methods in PSTTFGenerator. 2) that a table of length >
     * PSTTFGenerator.MAX_BUFFER_SIZE and length == n *
     * PSTTFGenerator.MAX_BUFFER_SIZE is split up and the methods in
     * PSTTFGenerator are invoked. 3) that a table of length >
     * PSTTFGenerator.MAX_BUFFER_SIZE but length != n *
     * PSTTFGenerator.MAX_BUFFER_SIZE is split up and the methods in
     * PSTTFGenerator are invoked.
     * 
     * @throws IOException
     *             file write error.
     */
    @Test
    public void testStreamTable() throws IOException {
        final byte[] byteArray = new byte[PSTTFGenerator.MAX_BUFFER_SIZE * 3];
        this.tableOut.streamTable(byteArray, 0, 10);
        InOrder inOrder = inOrder(this.mockGen);
        inOrder.verify(this.mockGen).startString();
        inOrder.verify(this.mockGen).streamBytes(byteArray, 0, 10);
        inOrder.verify(this.mockGen).endString();

        setUp(); // reset all all the method calls
        /*
         * We're going to run this 3 times to ensure the proper method calls are
         * invoked and all the bytes are streamed
         */
        this.tableOut.streamTable(byteArray, 0, byteArray.length);
        inOrder = inOrder(this.mockGen);
        for (int i = 0; i < 3; i++) {
            final int offset = PSTTFGenerator.MAX_BUFFER_SIZE * i;
            inOrder.verify(this.mockGen).startString();
            inOrder.verify(this.mockGen).streamBytes(byteArray, offset,
                    PSTTFGenerator.MAX_BUFFER_SIZE);
            inOrder.verify(this.mockGen).endString();
        }

        setUp(); // reset all the method calls
        this.tableOut.streamTable(byteArray, 0,
                PSTTFGenerator.MAX_BUFFER_SIZE + 1);
        inOrder = inOrder(this.mockGen);
        inOrder.verify(this.mockGen).startString();
        inOrder.verify(this.mockGen).streamBytes(byteArray, 0,
                PSTTFGenerator.MAX_BUFFER_SIZE);
        inOrder.verify(this.mockGen).endString();
        inOrder.verify(this.mockGen).startString();
        inOrder.verify(this.mockGen).streamBytes(byteArray,
                PSTTFGenerator.MAX_BUFFER_SIZE, 1);
        inOrder.verify(this.mockGen).endString();
    }
}
