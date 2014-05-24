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

/* $Id: TransformerDefaultHandler.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.util;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A DefaultHandler implementation that delegates all the method calls to a
 * {@link TransformerHandler} instance.
 */
public class TransformerDefaultHandler extends DefaultHandler2 {

    private final TransformerHandler transformerHandler;

    /**
     * Creates a new instance delegating to the given TransformerHandler object.
     *
     * @param transformerHandler
     *            the object to which all the method calls will be delegated
     */
    public TransformerDefaultHandler(final TransformerHandler transformerHandler) {
        this.transformerHandler = transformerHandler;
    }

    /**
     * Returns the delegate TransformerHandler instance.
     *
     * @return the object to which all method calls are delegated
     */
    public TransformerHandler getTransformerHandler() {
        return this.transformerHandler;
    }

    /** {@inheritDoc} */
    @Override
    public void setDocumentLocator(final Locator locator) {
        this.transformerHandler.setDocumentLocator(locator);
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws SAXException {
        this.transformerHandler.startDocument();
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {
        this.transformerHandler.endDocument();
    }

    /** {@inheritDoc} */
    @Override
    public void startPrefixMapping(final String prefix, final String uri)
            throws SAXException {
        this.transformerHandler.startPrefixMapping(prefix, uri);
    }

    /** {@inheritDoc} */
    @Override
    public void endPrefixMapping(final String string) throws SAXException {
        this.transformerHandler.endPrefixMapping(string);
    }

    /** {@inheritDoc} */
    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attrs) throws SAXException {
        final AttributesImpl ai = new AttributesImpl(attrs);
        this.transformerHandler.startElement(uri, localName, qName, ai);
    }

    /** {@inheritDoc} */
    @Override
    public void endElement(final String uri, final String localName,
            final String qName) throws SAXException {
        this.transformerHandler.endElement(uri, localName, qName);
    }

    /** {@inheritDoc} */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        this.transformerHandler.characters(ch, start, length);
    }

    /** {@inheritDoc} */
    @Override
    public void ignorableWhitespace(final char[] ch, final int start,
            final int length) throws SAXException {
        this.transformerHandler.ignorableWhitespace(ch, start, length);
    }

    /** {@inheritDoc} */
    @Override
    public void processingInstruction(final String target, final String data)
            throws SAXException {
        this.transformerHandler.processingInstruction(target, data);
    }

    /** {@inheritDoc} */
    @Override
    public void skippedEntity(final String name) throws SAXException {
        this.transformerHandler.skippedEntity(name);
    }

    /** {@inheritDoc} */
    @Override
    public void notationDecl(final String name, final String publicId,
            final String systemId) throws SAXException {
        this.transformerHandler.notationDecl(name, publicId, systemId);
    }

    /** {@inheritDoc} */
    @Override
    public void unparsedEntityDecl(final String name, final String publicId,
            final String systemId, final String notationName)
            throws SAXException {
        this.transformerHandler.unparsedEntityDecl(name, publicId, systemId,
                notationName);
    }

    /** {@inheritDoc} */
    @Override
    public void startDTD(final String name, final String pid, final String lid)
            throws SAXException {
        this.transformerHandler.startDTD(name, pid, lid);
    }

    /** {@inheritDoc} */
    @Override
    public void endDTD() throws SAXException {
        this.transformerHandler.endDTD();
    }

    /** {@inheritDoc} */
    @Override
    public void startEntity(final String name) throws SAXException {
        this.transformerHandler.startEntity(name);
    }

    /** {@inheritDoc} */
    @Override
    public void endEntity(final String name) throws SAXException {
        this.transformerHandler.endEntity(name);
    }

    /** {@inheritDoc} */
    @Override
    public void startCDATA() throws SAXException {
        this.transformerHandler.startCDATA();
    }

    /** {@inheritDoc} */
    @Override
    public void endCDATA() throws SAXException {
        this.transformerHandler.endCDATA();
    }

    /** {@inheritDoc} */
    @Override
    public void comment(final char[] charArray, final int start,
            final int length) throws SAXException {
        this.transformerHandler.comment(charArray, start, length);
    }

}
