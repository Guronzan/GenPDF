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

/* $Id: HyphenationLayoutTestCase.java 1203749 2011-11-18 17:08:20Z vhennebert $ */

package org.apache.fop.layoutengine;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * Class for testing the FOP's hyphenation layout engine using testcases
 * specified in XML files.
 */
public class HyphenationLayoutTestCase extends LayoutEngineTestCase {

    /**
     * Creates the parameters for this test.
     *
     * @return the list of file arrays populated with test files
     * @throws IOException
     *             if an I/O error occurs while reading the test file
     */
    @Parameters
    public static Collection<File[]> getParameters() throws IOException {
        return LayoutEngineTestUtils
                .getLayoutTestFiles("hyphenation-testcases");
    }

    /**
     * Constructor
     * 
     * @param testFile
     *            the file to test
     */
    public HyphenationLayoutTestCase(final File testFile) {
        super(testFile);
    }

}
