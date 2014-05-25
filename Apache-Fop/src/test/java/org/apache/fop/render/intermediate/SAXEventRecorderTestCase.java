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

/* $Id: SAXEventRecorderTestCase.java 1242848 2012-02-10 16:51:08Z phancock $ */

package org.apache.fop.render.intermediate;

import org.apache.fop.render.intermediate.IFStructureTreeBuilder.SAXEventRecorder;
import org.apache.fop.util.XMLConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link SAXEventRecorder}.
 */
public class SAXEventRecorderTestCase {

    private static final String URI = "http://www.example.com/";

    private SAXEventRecorder sut;

    @Before
    public void setUp() {
        this.sut = new SAXEventRecorder();
    }

    @Test
    public void testStartEvent() throws SAXException {
        final String localName = "element";
        final String qName = "prefix:" + localName;
        final Attributes attributes = new AttributesImpl();

        this.sut.startElement(URI, localName, qName, attributes);
        final ContentHandler handler = mock(ContentHandler.class);
        this.sut.replay(handler);
        verify(handler).startElement(URI, localName, qName, attributes);
    }

    @Test
    public void testEndEvent() throws SAXException {
        final String localName = "element";
        final String qName = "prefix:" + localName;
        this.sut.endElement(URI, localName, qName);
        final ContentHandler handler = mock(ContentHandler.class);
        this.sut.replay(handler);
        verify(handler).endElement(URI, localName, qName);
    }

    @Test
    public void testStartPrefixMapping() throws SAXException {
        final String prefix = "prefix";

        this.sut.startPrefixMapping(URI, prefix);
        final ContentHandler handler = mock(ContentHandler.class);
        this.sut.replay(handler);
        verify(handler).startPrefixMapping(URI, prefix);
    }

    @Test
    public void testEndPrefixMapping() throws SAXException {
        final String prefix = "prefix";

        this.sut.endPrefixMapping(prefix);
        final ContentHandler handler = mock(ContentHandler.class);
        this.sut.replay(handler);
        verify(handler).endPrefixMapping(prefix);
    }

    @Test
    public void completeTest() throws SAXException {
        final String localName1 = "element";
        final String qName1 = "prefix:" + localName1;
        final Attributes attributes1 = createAttributes(URI, localName1,
                qName1, "value-1");
        final String localName2 = "element2";
        final String qName2 = "prefix:" + localName2;
        final Attributes attributes2 = createAttributes(URI, localName2,
                qName2, "value-2");
        final ContentHandler handler = mock(ContentHandler.class);
        final String extensionUrl = "http://www.example.com/extension";
        final String extensionPrefix = "ext";

        this.sut.startPrefixMapping(extensionPrefix, extensionUrl);
        this.sut.startElement(URI, localName1, qName1, attributes1);
        this.sut.startElement(URI, localName2, qName2, attributes2);
        this.sut.endElement(URI, localName2, qName2);
        this.sut.endElement(URI, localName1, qName1);
        this.sut.endPrefixMapping(extensionPrefix);

        this.sut.replay(handler);

        final InOrder inOrder = inOrder(handler);
        inOrder.verify(handler).startPrefixMapping(extensionPrefix,
                extensionUrl);
        inOrder.verify(handler).startElement(URI, localName1, qName1,
                attributes1);
        inOrder.verify(handler).startElement(URI, localName2, qName2,
                attributes2);
        inOrder.verify(handler).endElement(URI, localName2, qName2);
        inOrder.verify(handler).endElement(URI, localName1, qName1);
        inOrder.verify(handler).endPrefixMapping(extensionPrefix);
    }

    private static Attributes createAttributes(final String uri,
            final String localName, final String qName, final String value) {
        final AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(uri, localName, qName, XMLConstants.CDATA, value);
        return atts;
    }

}
