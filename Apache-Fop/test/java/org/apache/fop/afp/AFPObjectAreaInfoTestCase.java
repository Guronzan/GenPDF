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

package org.apache.fop.afp;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test case for {@link AFPObjectAreaInfo}.
 */
public class AFPObjectAreaInfoTestCase {

    private AFPObjectAreaInfo sut;

    /**
     * Instantiate the system under test
     */
    @Before
    public void setUp() {
        this.sut = new AFPObjectAreaInfo(1, 2, 3, 4, 5, 6);
    }

    /**
     * Test the getter functions with arbitrary data.
     */
    @Test
    public void testGetters() {
        assertEquals(1, this.sut.getX());
        assertEquals(2, this.sut.getY());
        assertEquals(3, this.sut.getWidth());
        assertEquals(4, this.sut.getHeight());
        assertEquals(5, this.sut.getWidthRes());
        assertEquals(5, this.sut.getHeightRes());
        assertEquals(6, this.sut.getRotation());
    }

    /**
     * Test the resolution setters with arbitrary data.
     */
    @Test
    public void testSetters() {
        assertEquals(5, this.sut.getWidthRes());
        assertEquals(5, this.sut.getHeightRes());

        this.sut.setResolution(20);
        assertEquals(20, this.sut.getWidthRes());
        assertEquals(20, this.sut.getHeightRes());

        this.sut.setHeightRes(10);
        assertEquals(20, this.sut.getWidthRes());
        assertEquals(10, this.sut.getHeightRes());

        this.sut.setWidthRes(9);
        assertEquals(9, this.sut.getWidthRes());
        assertEquals(10, this.sut.getHeightRes());
    }
}
