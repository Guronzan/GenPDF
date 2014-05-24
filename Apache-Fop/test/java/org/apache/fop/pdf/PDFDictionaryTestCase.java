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

/* $Id: PDFDictionaryTestCase.java 1233854 2012-01-20 10:39:42Z cbowditch $ */

package org.apache.fop.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.output.CountingOutputStream;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test case for {@link PDFDictionary}.
 */
public class PDFDictionaryTestCase extends PDFObjectTestCase {
    /** The test subject */
    private PDFDictionary pdfDictUnderTest;
    private PDFArray testArray;
    private PDFNumber testNumber;
    /**
     * The order in which these objects are put into the dictionary MUST be
     * maintained.
     */
    private final String expectedOutput = "<<\n" + "  /String (TestValue)\n"
            + "  /int 10\n" + "  /double 3.1\n" + "  /array [1 (two) 20]\n"
            + "  /number 20\n" + "  /null null\n" + ">>\n";

    @Override
    @Before
    public void setUp() {
        // A PDFNumber for testing, this DOES have a parent
        this.testNumber = new PDFNumber();
        this.testNumber.setParent(this.parent);
        this.testNumber.setNumber(20);
        // An array for testing, this DOES NOT have a parent
        this.testArray = new PDFArray();
        this.testArray.add(1);
        this.testArray.add("two");
        this.testArray.add(this.testNumber);
        // Populating the dictionary with a parent, document and the various
        // objects
        this.pdfDictUnderTest = new PDFDictionary(this.parent);
        this.pdfDictUnderTest.setDocument(this.doc);
        this.pdfDictUnderTest.put("String", "TestValue");
        this.pdfDictUnderTest.put("int", 10);
        this.pdfDictUnderTest.put("double", Double.valueOf(3.1));
        this.pdfDictUnderTest.put("array", this.testArray);
        this.pdfDictUnderTest.put("number", this.testNumber);
        // null is a valid PDF object
        this.pdfDictUnderTest.put("null", null);
        // test that the interface is maintained
        this.pdfObjectUnderTest = this.pdfDictUnderTest;
    }

    /**
     * Tests put() - tests that the object is put into the dictionary and it is
     * handled if it is a {@link PDFObject}.
     */
    @Test
    public void testPut() {
        // The "put()" commands have already been done in setUp(), so just test
        // them.
        assertEquals("TestValue", this.pdfDictUnderTest.get("String"));
        assertEquals(10, this.pdfDictUnderTest.get("int"));
        assertEquals(3.1, this.pdfDictUnderTest.get("double"));
        // With PDFObjects, if they DO NOT have a parent, the dict becomes their
        // parent.
        assertEquals(this.testArray, this.pdfDictUnderTest.get("array"));
        assertEquals(this.pdfDictUnderTest, this.testArray.getParent());
        // With PDFObjects, if they DO have a parent, the dict DOES NOT change
        // the parent object.
        assertEquals(this.testNumber, this.pdfDictUnderTest.get("number"));
        // Test it doesn't explode when we try to get a non-existent entry
        assertNull(this.pdfDictUnderTest.get("Not in dictionary"));
        // Tests that we can over-write objects
        this.pdfDictUnderTest.put("array", 10);
        assertEquals(10, this.pdfDictUnderTest.get("array"));
        // Test that nulls are handled appropriately
        assertNull(this.pdfDictUnderTest.get("null"));
    }

    /**
     * Tests get() - tests that objects can be properly retrieved from the
     * dictionary.
     */
    @Test
    public void testGet() {
        // Tested fairly comprehensively in testPut().
    }

    /**
     * Tests writeDictionary() - tests that the dictionary is properly written
     * to the output-stream.
     */
    @Test
    public void testWriteDictionary() {
        // Ensure that the objects stored in the dictionary are streamed in the
        // correct format.
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final CountingOutputStream cout = new CountingOutputStream(outStream);
        final StringBuilder textBuffer = new StringBuilder();
        try {
            this.pdfDictUnderTest.writeDictionary(cout, textBuffer);
            PDFDocument.flushTextBuffer(textBuffer, cout);
            assertEquals(this.expectedOutput, outStream.toString());
        } catch (final IOException e) {
            fail("IOException: " + e.getMessage());
        }
    }

    /**
     * Tests output() - test that this object can write itself to an output
     * stream.
     * 
     * @throws IOException
     *             error caused by I/O
     */
    @Test
    public void testOutput() throws IOException {
        testOutputStreams(this.expectedOutput, this.pdfDictUnderTest);
    }
}
