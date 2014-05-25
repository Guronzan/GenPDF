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

/* $Id: FileIDGeneratorTestCase.java 1233854 2012-01-20 10:39:42Z cbowditch $ */

package org.apache.fop.pdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link FileIDGenerator} class.
 */
@RunWith(Parameterized.class)
public class FileIDGeneratorTestCase {

    /** The generator under test. */
    protected FileIDGenerator fileIDGenerator;

    private final TestGetter initializer;

    @Parameters
    public static Collection<TestGetter[]> getParameters() {
        final ArrayList<TestGetter[]> params = new ArrayList<TestGetter[]>();
        params.add(new TestGetter[] { new RandomFileIDGeneratorTest() });
        params.add(new TestGetter[] { new DigestFileIDGeneratorTest() });
        return params;
    }

    public FileIDGeneratorTestCase(final TestGetter initializer) {
        this.initializer = initializer;
    }

    @Before
    public void setUp() throws Exception {
        this.fileIDGenerator = this.initializer.getSut();
    }

    /** Tests that the getOriginalFileID method generates valid output. */
    @Test
    public void testOriginal() {
        final byte[] fileID = this.fileIDGenerator.getOriginalFileID();
        fileIDMustBeValid(fileID);
    }

    /** Tests that the getUpdatedFileID method generates valid output. */
    @Test
    public void testUpdated() {
        final byte[] fileID = this.fileIDGenerator.getUpdatedFileID();
        fileIDMustBeValid(fileID);
    }

    private void fileIDMustBeValid(final byte[] fileID) {
        assertNotNull(fileID);
        assertEquals(16, fileID.length);
    }

    /**
     * Tests that multiple calls to getOriginalFileID method always return the
     * same value.
     */
    @Test
    public void testOriginalMultipleCalls() {
        final byte[] fileID1 = this.fileIDGenerator.getUpdatedFileID();
        final byte[] fileID2 = this.fileIDGenerator.getUpdatedFileID();
        assertTrue(Arrays.equals(fileID1, fileID2));
    }

    /** Tests that getUpdatedFileID returns the same value as getOriginalFileID. */
    @Test
    public void testUpdateEqualsOriginal() {
        final byte[] originalFileID = this.fileIDGenerator.getOriginalFileID();
        final byte[] updatedFileID = this.fileIDGenerator.getUpdatedFileID();
        assertTrue(Arrays.equals(originalFileID, updatedFileID));
    }

    private static interface TestGetter {
        FileIDGenerator getSut() throws Exception;
    }

    /**
     * Tests the random file ID generator.
     */
    private static class RandomFileIDGeneratorTest implements TestGetter {

        @Override
        public FileIDGenerator getSut() throws Exception {
            return FileIDGenerator.getRandomFileIDGenerator();
        }

    }

    /**
     * Tests the file ID generator based on an MD5 digest.
     */
    private static class DigestFileIDGeneratorTest implements TestGetter {

        @Override
        public FileIDGenerator getSut() throws Exception {
            return FileIDGenerator.getDigestFileIDGenerator(new PDFDocument(
                    "Apache FOP"));
        }

    }

}
