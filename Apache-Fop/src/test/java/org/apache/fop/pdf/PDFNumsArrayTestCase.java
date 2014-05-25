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

/* $Id: PDFNumsArrayTestCase.java 1233854 2012-01-20 10:39:42Z cbowditch $ */

package org.apache.fop.pdf;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

/**
 * Test case for {@link PDFNumsArray}.
 */
public class PDFNumsArrayTestCase extends PDFObjectTestCase {
    private PDFNumsArray numsArray;
    private final String expectedString = "[0 /Test#20name 1 10]";

    @Override
    @Before
    public void setUp() {
        this.numsArray = new PDFNumsArray(this.parent);
        this.numsArray.put(0, new PDFName("Test name"));
        final PDFNumber num = new PDFNumber();
        num.setNumber(10);
        this.numsArray.put(1, num);
        this.numsArray.setDocument(this.doc);

        this.pdfObjectUnderTest = this.numsArray;
    }

    /**
     * Test output() - ensure that this object is properly outputted to the PDF
     * document.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    @Test
    public void testOutput() throws IOException {
        testOutputStreams(this.expectedString, this.numsArray);
    }
}
