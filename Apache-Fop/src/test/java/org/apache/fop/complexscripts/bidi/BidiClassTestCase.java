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

/* $Id$ */

package org.apache.fop.complexscripts.bidi;

import org.apache.fop.util.CharUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class BidiClassTestCase {

    @Test
    public void testBidiClasses() {
        final String tdPfx = BidiTestData.TD_PFX;
        final int tdCount = BidiTestData.TD_CNT;
        for (int i = 0; i < tdCount; i++) {
            final int[] da = BidiTestData.readTestData(tdPfx, i);
            if (da != null) {
                testBidiClass(da);
            } else {
                fail("unable to read bidi test data for resource at index " + i);
            }
        }
    }

    private void testBidiClass(final int[] da) {
        final int bc = da[0];
        for (int i = 1, n = da.length; i < n; i += 2) {
            final int s = da[i + 0];
            final int e = da[i + 1];
            for (int c = s; c < e; c++) {
                final int cbc = BidiClass.getBidiClass(c);
                assertEquals("bad bidi class for CH(" + CharUtilities.format(c)
                        + ")", bc, cbc);
            }
        }
    }

}
