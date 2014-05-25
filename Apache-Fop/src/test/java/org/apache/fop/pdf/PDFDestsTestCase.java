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

/* $Id: PDFDestsTestCase.java 1233854 2012-01-20 10:39:42Z cbowditch $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Test case for {@link PDFDests}.
 */
public class PDFDestsTestCase extends PDFObjectTestCase {

    private PDFDests dests = new PDFDests();
    private final String expectedString = "<< /Names [(number) 10 (name) /Test#20name] >>\n";

    @Override
    @Before
    public void setUp() {
        final List<PDFDestination> destinations = new ArrayList<PDFDestination>();
        final PDFNumber number = new PDFNumber();
        number.setNumber(10);
        final PDFDestination testNumber = new PDFDestination("number", number);
        testNumber.setDocument(this.doc);
        destinations.add(testNumber);
        final PDFDestination testName = new PDFDestination("name", new PDFName(
                "Test name"));
        testName.setDocument(this.doc);
        destinations.add(testName);

        this.dests = new PDFDests(destinations);
        this.dests.setDocument(this.doc);
        this.dests.setParent(this.parent);
        this.pdfObjectUnderTest = this.dests;
    }

    /**
     * Populate the object with some arbitrary values and ensure they are
     * wrapped properly.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    @Test
    public void testConstructor() throws IOException {
        // Seems the only way to test this is by testing the output
        testOutputStreams(this.expectedString, this.dests);
    }
}
