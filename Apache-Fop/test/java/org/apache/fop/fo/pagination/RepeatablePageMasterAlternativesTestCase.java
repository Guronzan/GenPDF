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

package org.apache.fop.fo.pagination;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.NumericProperty;
import org.apache.fop.fo.properties.Property;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit Test for RepeatablePageMasterAlternatives
 *
 */
public class RepeatablePageMasterAlternativesTestCase implements Constants {

    /**
     *
     * @throws Exception
     *             exception
     */
    @Test
    public void testIsInfinite1() throws Exception {
        // Create fixture
        final Property maximumRepeats = mock(Property.class);
        final ConditionalPageMasterReference cpmr = createCPMR("empty");

        when(maximumRepeats.getEnum()).thenReturn(EN_NO_LIMIT);

        final RepeatablePageMasterAlternatives objectUnderTest = createRepeatablePageMasterAlternatives(
                cpmr, maximumRepeats);

        assertTrue("is infinite", objectUnderTest.isInfinite());
    }

    /**
     *
     * @throws Exception
     *             exception
     */
    @Test
    public void testIsInfinite2() throws Exception {
        // Create fixture
        final Property maximumRepeats = mock(Property.class);
        final ConditionalPageMasterReference cpmr = createCPMR("empty");

        final NumericProperty numericProperty = mock(NumericProperty.class);

        final int maxRepeatNum = 0;
        assertTrue(maxRepeatNum != EN_NO_LIMIT);

        when(maximumRepeats.getEnum()).thenReturn(maxRepeatNum);
        when(maximumRepeats.getNumeric()).thenReturn(numericProperty);

        final RepeatablePageMasterAlternatives objectUnderTest = createRepeatablePageMasterAlternatives(
                createCPMR("empty"), maximumRepeats);

        assertTrue("is infinite", !objectUnderTest.isInfinite());
    }

    /**
     * Test that an infinite sequence of empty page masters has willTerminiate()
     * returning false
     * 
     * @throws Exception
     *             exception
     */
    @Test
    public void testCanProcess1() throws Exception {
        // Create fixture
        final Property maximumRepeats = mock(Property.class);
        final ConditionalPageMasterReference cpmr = createCPMR("empty");

        when(maximumRepeats.getEnum()).thenReturn(EN_NO_LIMIT);
        when(
                cpmr.isValid(anyBoolean(), anyBoolean(), anyBoolean(),
                        anyBoolean())).thenReturn(true);

        final RepeatablePageMasterAlternatives objectUnderTest = createRepeatablePageMasterAlternatives(
                cpmr, maximumRepeats);

        // Fixture assertion
        assertTrue("Should be infinite", objectUnderTest.isInfinite());

        // Test assertion
        assertTrue("Infinite sequences that do not process the main flow will "
                + " not terminate", !objectUnderTest.canProcess("main-flow"));
    }

    /**
     * Test that a finite sequence of simple page masters has willTerminate()
     * returning true
     *
     * @throws Exception
     *             exception
     */
    @Test
    public void testCanProcess2() throws Exception {
        // Create fixture
        final Property maximumRepeats = mock(Property.class);
        final NumericProperty numericProperty = mock(NumericProperty.class);

        final int maxRepeatNum = 0;

        when(maximumRepeats.getEnum()).thenReturn(maxRepeatNum);
        when(maximumRepeats.getNumeric()).thenReturn(numericProperty);

        final RepeatablePageMasterAlternatives objectUnderTest = createRepeatablePageMasterAlternatives(
                createCPMR("empty"), maximumRepeats);

        // Fixture assertion
        assertTrue("Should be finite sequence", !objectUnderTest.isInfinite());

        // Test assertion
        assertTrue("Finite sequences will terminate",
                objectUnderTest.canProcess("main-flow"));
    }

    private ConditionalPageMasterReference createCPMR(final String regionName) {
        final ConditionalPageMasterReference cpmr = mock(ConditionalPageMasterReference.class);
        final SimplePageMaster master = mock(SimplePageMaster.class);
        final Region region = mock(Region.class);
        when(master.getRegion(anyInt())).thenReturn(region);
        when(region.getRegionName()).thenReturn(regionName);
        when(cpmr.getMaster()).thenReturn(master);

        return cpmr;
    }

    private RepeatablePageMasterAlternatives createRepeatablePageMasterAlternatives(
            final ConditionalPageMasterReference cpmr,
            final Property maximumRepeats) throws Exception {

        final PropertyList pList = mock(PropertyList.class);

        when(pList.get(anyInt())).thenReturn(maximumRepeats);

        final PageSequenceMaster parent = mock(PageSequenceMaster.class);
        when(parent.getName()).thenReturn("fo:page-sequence-master");

        final RepeatablePageMasterAlternatives sut = new RepeatablePageMasterAlternatives(
                parent);

        sut.startOfNode();
        sut.bind(pList);
        sut.addConditionalPageMasterReference(cpmr);
        return sut;
    }

}
