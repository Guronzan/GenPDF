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

/* $Id: PDFArrayTestCase.java 1233854 2012-01-20 10:39:42Z cbowditch $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test case for {@link PDFArray}.
 */
public class PDFArrayTestCase extends PDFObjectTestCase {
    private PDFArray intArray;
    private String intArrayOutput;
    private PDFArray doubleArray;
    private String doubleArrayOutput;
    private PDFArray collectionArray;
    private String collectionArrayOutput;
    private PDFArray objArray;
    private String objArrayOutput;

    /** A PDF object used solely for testing */
    private PDFNumber num;

    @Override
    @Before
    public void setUp() {
        this.intArray = new PDFArray(this.parent, new int[] { 1, 2, 3, 4, 5 });
        this.intArrayOutput = "[1 2 3 4 5]";

        this.doubleArray = new PDFArray(this.parent, new double[] { 1.1, 2.2,
                3.3, 4.4, 5.5 });
        this.doubleArrayOutput = "[1.1 2.2 3.3 4.4 5.5]";

        final List<Object> strList = new ArrayList<Object>();
        strList.add("one");
        strList.add("two");
        strList.add("three");
        this.collectionArray = new PDFArray(this.parent, strList);
        this.collectionArrayOutput = "[(one) (two) (three)]";

        // Set arbitrary values here
        this.num = new PDFNumber();
        this.num.setNumber(20);
        this.num.setObjectNumber(4);
        this.objArray = new PDFArray(this.parent, new Object[] { "one", 2,
                3.0f, this.num });
        this.objArrayOutput = "[(one) 2 3 4 0 R]";

        // set the document
        this.intArray.setDocument(this.doc);
        this.doubleArray.setDocument(this.doc);
        this.collectionArray.setDocument(this.doc);
        this.objArray.setDocument(this.doc);

        // Test the progenitor in the inheritance stack
        this.objArray.setParent(this.parent);
        this.pdfObjectUnderTest = this.objArray;
    }

    private void intArrayContainsTests() {
        for (int i = 1; i <= 5; i++) {
            assertTrue(this.intArray.contains(i));
        }
        assertFalse(this.intArray.contains(6));
        assertFalse(this.intArray.contains(0));
    }

    private void doubleArrayContainsTests() {
        assertTrue(this.doubleArray.contains(1.1));
        assertTrue(this.doubleArray.contains(2.2));
        assertTrue(this.doubleArray.contains(3.3));
        assertTrue(this.doubleArray.contains(4.4));
        assertTrue(this.doubleArray.contains(5.5));
        assertFalse(this.doubleArray.contains(10.0));
        assertFalse(this.doubleArray.contains(0.0));
    }

    private void collectionArrayContainsTests() {
        assertTrue(this.collectionArray.contains("one"));
        assertTrue(this.collectionArray.contains("two"));
        assertTrue(this.collectionArray.contains("three"));
        assertFalse(this.collectionArray.contains("zero"));
        assertFalse(this.collectionArray.contains("four"));
    }

    private void objectArrayContainsTests() {
        assertTrue(this.objArray.contains("one"));
        assertTrue(this.objArray.contains(2));
        assertTrue(this.objArray.contains(3.0f));
        assertTrue(this.objArray.contains(this.num));
        assertFalse(this.objArray.contains("four"));
        assertFalse(this.objArray.contains(0.0));
    }

    /**
     * Test contains() - test whether this PDFArray contains an object.
     */
    @Test
    public void testContains() {
        // Test some arbitrary values
        intArrayContainsTests();
        doubleArrayContainsTests();
        collectionArrayContainsTests();
        objectArrayContainsTests();
    }

    /**
     * Test length() - tests the length of an array.
     */
    @Test
    public void testLength() {
        assertEquals(5, this.intArray.length());
        assertEquals(5, this.doubleArray.length());
        assertEquals(3, this.collectionArray.length());
        assertEquals(4, this.objArray.length());

        // Test the count is incremented when an object is added (this only
        // needs to be tested once)
        this.intArray.add(6);
        assertEquals(6, this.intArray.length());
    }

    /**
     * Test set() - tests that a particular point has been properly set.
     */
    @Test
    public void testSet() {
        final PDFName name = new PDFName("zero test");
        this.objArray.set(0, name);
        assertEquals(name, this.objArray.get(0));

        this.objArray.set(1, "test");
        assertEquals("test", this.objArray.get(1));
        // This goes through the set(int, double) code path rather than set(int,
        // Object)
        this.objArray.set(2, 5);
        assertEquals(5.0, this.objArray.get(2));
        try {
            this.objArray.set(4, 2);
            fail("out of bounds");
        } catch (final IndexOutOfBoundsException e) {
            // Pass
        }
    }

    /**
     * Test get() - gets the object stored at a given index.
     */
    @Test
    public void testGet() {
        // Test some arbitrary values
        for (int i = 1; i <= 5; i++) {
            assertEquals(i, this.intArray.get(i - 1));
        }

        assertEquals(1.1, this.doubleArray.get(0));
        assertEquals(2.2, this.doubleArray.get(1));
        assertEquals(3.3, this.doubleArray.get(2));
        assertEquals(4.4, this.doubleArray.get(3));
        assertEquals(5.5, this.doubleArray.get(4));

        assertEquals("one", this.collectionArray.get(0));
        assertEquals("two", this.collectionArray.get(1));
        assertEquals("three", this.collectionArray.get(2));

        assertEquals("one", this.objArray.get(0));
        assertEquals(2, this.objArray.get(1));
        assertEquals(0, Double.compare(3.0, (Float) this.objArray.get(2)));
        assertEquals(this.num, this.objArray.get(3));
    }

    /**
     * Tests add() - tests that objects are appended to the end of the array as
     * expected.
     */
    @Test
    public void testAdd() {
        this.intArray.add(new Integer(6));
        this.doubleArray.add(6.6);
        // Test some arbitrary values
        for (int i = 1; i <= 6; i++) {
            assertEquals(i, this.intArray.get(i - 1));
        }

        assertEquals(1.1, this.doubleArray.get(0));
        assertEquals(2.2, this.doubleArray.get(1));
        assertEquals(3.3, this.doubleArray.get(2));
        assertEquals(4.4, this.doubleArray.get(3));
        assertEquals(5.5, this.doubleArray.get(4));
        assertEquals(6.6, this.doubleArray.get(5));

        this.collectionArray.add(1);
        assertEquals("one", this.collectionArray.get(0));
        assertEquals("two", this.collectionArray.get(1));
        assertEquals("three", this.collectionArray.get(2));
        assertEquals(1.0, this.collectionArray.get(3));

        this.objArray.add("four");
        assertEquals("one", this.objArray.get(0));
        assertEquals(2, this.objArray.get(1));
        assertEquals(0, Double.compare(3.0, (Float) this.objArray.get(2)));
        assertEquals("four", this.objArray.get(4));
    }

    /**
     * Tests output() - tests that this object is properly streamed to the PDF
     * document.
     * 
     * @throws IOException
     *             error caused by I/O
     */
    @Test
    public void testOutput() throws IOException {
        testOutputStreams(this.intArrayOutput, this.intArray);
        testOutputStreams(this.doubleArrayOutput, this.doubleArray);
        testOutputStreams(this.collectionArrayOutput, this.collectionArray);
        testOutputStreams(this.objArrayOutput, this.objArray);
    }
}
