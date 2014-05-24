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

/* $Id: EventModelParser.java 985571 2010-08-14 19:28:26Z jeremias $ */

package org.apache.fop.events.model;

import java.util.Stack;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.util.DefaultErrorListener;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is a parser for the event model XML.
 */
@Slf4j
public final class EventModelParser {

    private EventModelParser() {
    }

    private static SAXTransformerFactory tFactory = (SAXTransformerFactory) SAXTransformerFactory
            .newInstance();

    /**
     * Parses an event model file into an EventModel instance.
     *
     * @param src
     *            the Source instance pointing to the XML file
     * @return the created event model structure
     * @throws TransformerException
     *             if an error occurs while parsing the XML file
     */
    public static EventModel parse(final Source src)
            throws TransformerException {
        final Transformer transformer = tFactory.newTransformer();
        transformer.setErrorListener(new DefaultErrorListener(log));

        final EventModel model = new EventModel();
        final SAXResult res = new SAXResult(getContentHandler(model));

        transformer.transform(src, res);
        return model;
    }

    /**
     * Creates a new ContentHandler instance that you can send the event model
     * XML to. The parsed content is accumulated in the model structure.
     *
     * @param model
     *            the EventModel
     * @return the ContentHandler instance to receive the SAX stream from the
     *         XML file
     */
    public static ContentHandler getContentHandler(final EventModel model) {
        return new Handler(model);
    }

    private static class Handler extends DefaultHandler {

        private final EventModel model;
        private final Stack objectStack = new Stack();

        public Handler(final EventModel model) {
            this.model = model;
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final String uri, final String localName,
                final String qName, final Attributes attributes)
                        throws SAXException {
            try {
                if ("event-model".equals(localName)) {
                    if (this.objectStack.size() > 0) {
                        throw new SAXException(
                                "event-model must be the root element");
                    }
                    this.objectStack.push(this.model);
                } else if ("producer".equals(localName)) {
                    final EventProducerModel producer = new EventProducerModel(
                            attributes.getValue("name"));
                    final EventModel parent = (EventModel) this.objectStack
                            .peek();
                    parent.addProducer(producer);
                    this.objectStack.push(producer);
                } else if ("method".equals(localName)) {
                    final EventSeverity severity = EventSeverity
                            .valueOf(attributes.getValue("severity"));
                    final String ex = attributes.getValue("exception");
                    final EventMethodModel method = new EventMethodModel(
                            attributes.getValue("name"), severity);
                    if (ex != null && ex.length() > 0) {
                        method.setExceptionClass(ex);
                    }
                    final EventProducerModel parent = (EventProducerModel) this.objectStack
                            .peek();
                    parent.addMethod(method);
                    this.objectStack.push(method);
                } else if ("parameter".equals(localName)) {
                    final String className = attributes.getValue("type");
                    Class type;
                    try {
                        type = Class.forName(className);
                    } catch (final ClassNotFoundException e) {
                        throw new SAXException("Could not find Class for: "
                                + className, e);
                    }
                    final String name = attributes.getValue("name");
                    final EventMethodModel parent = (EventMethodModel) this.objectStack
                            .peek();
                    this.objectStack.push(parent.addParameter(type, name));
                } else {
                    throw new SAXException("Invalid element: " + qName);
                }
            } catch (final ClassCastException cce) {
                throw new SAXException("XML format error: " + qName, cce);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName,
                final String qName) throws SAXException {
            this.objectStack.pop();
        }

    }

}
