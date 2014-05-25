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

/* $Id: ObjectReferenceTest.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf.xref;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

abstract class ObjectReferenceTest {

    protected ObjectReference sut;

    protected long computeNumberFromBytes(
            final List<Integer> expectedOffsetBytes) {
        assert expectedOffsetBytes.size() <= 8;
        long offset = 0;
        for (final int b : expectedOffsetBytes) {
            offset = offset << 8 | b;
        }
        return offset;
    }

    protected byte[] createExpectedOutput(final byte field1,
            final List<Integer> field2, final int field3) {
        assert field2.size() == 8;
        assert (field3 & 0xffff) == field3;
        final byte[] expected = new byte[11];
        int index = 0;
        expected[index++] = field1;
        for (final Integer b : field2) {
            expected[index++] = b.byteValue();
        }
        expected[index++] = (byte) ((field3 & 0xff00) >> 8);
        expected[index++] = (byte) (field3 & 0xff);
        return expected;
    }

    protected byte[] getActualOutput() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(out);
        this.sut.output(dataOutputStream);
        dataOutputStream.close();
        return out.toByteArray();
    }

}