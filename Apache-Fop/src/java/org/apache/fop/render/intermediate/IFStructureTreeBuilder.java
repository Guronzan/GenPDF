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

package org.apache.fop.render.intermediate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.fop.accessibility.StructureTree2SAXEventAdapter;
import org.apache.fop.accessibility.StructureTreeElement;
import org.apache.fop.accessibility.StructureTreeEventHandler;
import org.apache.fop.fo.extensions.InternalElementMapping;
import org.apache.fop.util.XMLConstants;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Saves structure tree events as SAX events in order to replay them when it's
 * time to stream the structure tree to the output.
 */
final class IFStructureTreeBuilder implements StructureTreeEventHandler {

    static final class IFStructureTreeElement implements StructureTreeElement {

        private final String id;

        IFStructureTreeElement() {
            this.id = null;
        }

        IFStructureTreeElement(final String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    }

    /** A SAX handler that records events to replay them later. */
    static class SAXEventRecorder extends DefaultHandler {

        private final List<SAXEventRecorder.Event> events = new ArrayList<SAXEventRecorder.Event>();

        private abstract static class Event {
            abstract void replay(final ContentHandler handler)
                    throws SAXException;
        }

        private abstract static class Element extends SAXEventRecorder.Event {

            protected final String uri;
            protected final String localName;
            protected final String qName;

            private Element(final String uri, final String localName,
                    final String qName) {
                this.uri = uri;
                this.localName = localName;
                this.qName = qName;
            }
        }

        private static final class StartElement extends
                SAXEventRecorder.Element {

            private final Attributes attributes;

            private StartElement(final String uri, final String localName,
                    final String qName, final Attributes attributes) {
                super(uri, localName, qName);
                this.attributes = attributes;
            }

            @Override
            void replay(final ContentHandler handler) throws SAXException {
                handler.startElement(this.uri, this.localName, this.qName,
                        this.attributes);
            }
        }

        private static final class EndElement extends SAXEventRecorder.Element {

            private EndElement(final String uri, final String localName,
                    final String qName) {
                super(uri, localName, qName);
            }

            @Override
            void replay(final ContentHandler handler) throws SAXException {
                handler.endElement(this.uri, this.localName, this.qName);
            }
        }

        private static final class StartPrefixMapping extends
                SAXEventRecorder.Event {

            private final String prefix;
            private final String uri;

            private StartPrefixMapping(final String prefix, final String uri) {
                this.prefix = prefix;
                this.uri = uri;
            }

            @Override
            void replay(final ContentHandler handler) throws SAXException {
                handler.startPrefixMapping(this.prefix, this.uri);
            }
        }

        private static final class EndPrefixMapping extends
                SAXEventRecorder.Event {

            private final String prefix;

            private EndPrefixMapping(final String prefix) {
                this.prefix = prefix;
            }

            @Override
            void replay(final ContentHandler handler) throws SAXException {
                handler.endPrefixMapping(this.prefix);
            }
        }

        @Override
        public void startElement(final String uri, final String localName,
                final String qName, final Attributes attributes)
                throws SAXException {
            this.events
                    .add(new StartElement(uri, localName, qName, attributes));
        }

        @Override
        public void endElement(final String uri, final String localName,
                final String qName) throws SAXException {
            this.events.add(new EndElement(uri, localName, qName));
        }

        @Override
        public void startPrefixMapping(final String prefix, final String uri)
                throws SAXException {
            this.events.add(new StartPrefixMapping(prefix, uri));
        }

        @Override
        public void endPrefixMapping(final String prefix) throws SAXException {
            this.events.add(new EndPrefixMapping(prefix));
        }

        /**
         * Replays the recorded events.
         *
         * @param handler
         *            {@code ContentHandler} to replay events on
         */
        public void replay(final ContentHandler handler) throws SAXException {
            for (final SAXEventRecorder.Event e : this.events) {
                e.replay(handler);
            }
        }
    }

    private StructureTreeEventHandler delegate;

    private final List<SAXEventRecorder> pageSequenceEventRecorders = new ArrayList<SAXEventRecorder>();

    private int idCounter;

    /**
     * Replay SAX events for a page sequence.
     * 
     * @param handler
     *            The handler that receives SAX events
     * @param pageSequenceIndex
     *            The index of the page sequence
     * @throws SAXException
     */
    public void replayEventsForPageSequence(final ContentHandler handler,
            final int pageSequenceIndex) throws SAXException {
        this.pageSequenceEventRecorders.get(pageSequenceIndex).replay(handler);
    }

    @Override
    public void startPageSequence(final Locale locale, final String role) {
        final SAXEventRecorder eventRecorder = new SAXEventRecorder();
        this.pageSequenceEventRecorders.add(eventRecorder);
        this.delegate = StructureTree2SAXEventAdapter
                .newInstance(eventRecorder);
        this.delegate.startPageSequence(locale, role);
    }

    @Override
    public void endPageSequence() {
        this.delegate.endPageSequence();
    }

    @Override
    public StructureTreeElement startNode(final String name,
            final Attributes attributes) {
        this.delegate.startNode(name, attributes);
        return new IFStructureTreeElement();
    }

    @Override
    public void endNode(final String name) {
        this.delegate.endNode(name);
    }

    @Override
    public StructureTreeElement startImageNode(final String name,
            final Attributes attributes) {
        final String id = getNextID();
        final AttributesImpl atts = addIDAttribute(attributes, id);
        this.delegate.startImageNode(name, atts);
        return new IFStructureTreeElement(id);
    }

    @Override
    public StructureTreeElement startReferencedNode(final String name,
            final Attributes attributes) {
        final String id = getNextID();
        final AttributesImpl atts = addIDAttribute(attributes, id);
        this.delegate.startReferencedNode(name, atts);
        return new IFStructureTreeElement(id);
    }

    private String getNextID() {
        return Integer.toHexString(this.idCounter++);
    }

    private AttributesImpl addIDAttribute(final Attributes attributes,
            final String id) {
        final AttributesImpl atts = new AttributesImpl(attributes);
        atts.addAttribute(InternalElementMapping.URI,
                InternalElementMapping.STRUCT_ID,
                InternalElementMapping.STANDARD_PREFIX + ":"
                        + InternalElementMapping.STRUCT_ID, XMLConstants.CDATA,
                id);
        return atts;
    }
}
