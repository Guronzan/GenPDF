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

/* $Id: DOMBuilderContentHandlerFactory.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.util;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * ContentHandlerFactory which constructs ContentHandlers that build DOM
 * Documents.
 */
public class DOMBuilderContentHandlerFactory implements ContentHandlerFactory {

    private static SAXTransformerFactory tFactory = (SAXTransformerFactory) SAXTransformerFactory
            .newInstance();

    private final String namespaceURI;
    private final DOMImplementation domImplementation;

    /**
     * Main Constructor
     *
     * @param namespaceURI
     *            the main namespace URI for the DOM to be parsed
     * @param domImplementation
     *            the DOMImplementation to use for build the DOM
     */
    public DOMBuilderContentHandlerFactory(final String namespaceURI,
            final DOMImplementation domImplementation) {
        this.namespaceURI = namespaceURI;
        this.domImplementation = domImplementation;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getSupportedNamespaces() {
        return new String[] { this.namespaceURI };
    }

    /** {@inheritDoc} */
    @Override
    public ContentHandler createContentHandler() throws SAXException {
        return new Handler();
    }

    private class Handler extends DelegatingContentHandler implements
    ContentHandlerFactory.ObjectSource {

        private Document doc;
        private ObjectBuiltListener obListener;

        public Handler() {
            super();
        }

        public Document getDocument() {
            return this.doc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getObject() {
            return getDocument();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setObjectBuiltListener(final ObjectBuiltListener listener) {
            this.obListener = listener;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startDocument() throws SAXException {
            // Suppress startDocument() call if doc has not been set, yet. It
            // will be done later.
            if (this.doc != null) {
                super.startDocument();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(final String uri, final String localName,
                final String qName, final Attributes atts) throws SAXException {
            if (this.doc == null) {
                TransformerHandler handler;
                try {
                    handler = tFactory.newTransformerHandler();
                } catch (final TransformerConfigurationException e) {
                    throw new SAXException(
                            "Error creating a new TransformerHandler", e);
                }
                this.doc = DOMBuilderContentHandlerFactory.this.domImplementation
                        .createDocument(
                                DOMBuilderContentHandlerFactory.this.namespaceURI,
                                qName, null);
                // It's easier to work with an empty document, so remove the
                // root element
                this.doc.removeChild(this.doc.getDocumentElement());
                handler.setResult(new DOMResult(this.doc));
                setDelegateContentHandler(handler);
                setDelegateLexicalHandler(handler);
                setDelegateDTDHandler(handler);
                handler.startDocument();
            }
            super.startElement(uri, localName, qName, atts);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
            if (this.obListener != null) {
                this.obListener.notifyObjectBuilt(getObject());
            }
        }

    }

}
