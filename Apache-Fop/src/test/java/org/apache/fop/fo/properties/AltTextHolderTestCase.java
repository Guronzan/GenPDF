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

/* $Id: AltTextHolderTestCase.java 1242848 2012-02-10 16:51:08Z phancock $ */

package org.apache.fop.fo.properties;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FONodeMocks;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.flow.AbstractGraphics;
import org.apache.fop.fo.flow.ExternalGraphic;
import org.apache.fop.fo.flow.InstreamForeignObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that the fox:alt-text property is correctly set on objects that support
 * it.
 */
public class AltTextHolderTestCase {

    private final String altText = "alternative text";

    @Test
    public void externalGraphicHasAltText() throws FOPException {
        testAltTextGetter(new ExternalGraphic(mockFONode()));
    }

    @Test
    public void instreamForeignObjectHasAltText() throws FOPException {
        testAltTextGetter(new InstreamForeignObject(mockFONode()));
    }

    private FONode mockFONode() {
        final FONode mockFONode = FONodeMocks.mockFONode();
        final FOUserAgent mockFOUserAgent = mockFONode.getFOEventHandler()
                .getUserAgent();
        when(mockFOUserAgent.isAccessibilityEnabled()).thenReturn(true);
        return mockFONode;
    }

    private void testAltTextGetter(final AbstractGraphics g)
            throws FOPException {
        g.bind(mockPropertyList());
        assertEquals(this.altText, g.getAltText());
    }

    private PropertyList mockPropertyList() throws PropertyException {
        final PropertyList mockPropertyList = PropertyListMocks
                .mockPropertyList();
        final Property mockAltText = mock(Property.class);
        when(mockAltText.getString()).thenReturn(this.altText);
        when(mockPropertyList.get(Constants.PR_X_ALT_TEXT)).thenReturn(
                mockAltText);
        return mockPropertyList;
    }

}
