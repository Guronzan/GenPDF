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

/* $Id: CrossReferenceTableTestCase.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf.xref;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class CrossReferenceTableTestCase extends CrossReferenceObjectTest {

    private List<Long> offsets;

    @Test
    public void testWithNoOffset() throws IOException {
        final List<Long> emptyList = Collections.emptyList();
        runTest(emptyList);
    }

    @Test
    public void testWithOffsets() throws IOException {
        runTest(Arrays.asList(0L, 1L, 2L, 3L, 4L));
    }

    @Test
    public void testWithBigOffsets() throws IOException {
        runTest(Arrays.asList(0xffL, 0xffffL, 0x7fffffffL));
    }

    private void runTest(final List<Long> offsets) throws IOException {
        this.offsets = offsets;
        runTest();
    }

    @Override
    protected CrossReferenceObject createCrossReferenceObject() {
        return new CrossReferenceTable(this.trailerDictionary, STARTXREF,
                this.offsets);
    }

    @Override
    protected byte[] createExpectedCrossReferenceData() throws IOException {
        final StringBuilder expected = new StringBuilder(256);
        expected.append("xref\n0 ").append(this.offsets.size() + 1)
        .append("\n0000000000 65535 f \n");
        for (final Long objectReference : this.offsets) {
            final String padding = "0000000000";
            final String s = String.valueOf(objectReference).toString();
            final String loc = padding.substring(s.length()) + s;
            expected.append(loc).append(" 00000 n \n");
        }
        expected.append("trailer\n<<\n").append("  /Root 1 0 R\n")
        .append("  /Info 2 0 R\n")
        .append("  /ID [<0123456789ABCDEF> <0123456789ABCDEF>]\n")
        .append("  /Size ")
                .append(Integer.toString(this.offsets.size() + 1)).append('\n')
        .append(">>\n");
        return getBytes(expected);
    }

}