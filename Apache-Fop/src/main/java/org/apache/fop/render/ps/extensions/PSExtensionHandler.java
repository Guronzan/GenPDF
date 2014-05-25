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

/* $Id: PSExtensionHandler.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.ps.extensions;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.util.ContentHandlerFactory;
import org.apache.fop.util.ContentHandlerFactory.ObjectBuiltListener;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * ContentHandler (parser) for restoring PSExtension objects from XML.
 */
@Slf4j
public class PSExtensionHandler extends DefaultHandler implements
ContentHandlerFactory.ObjectSource {

    private final StringBuilder content = new StringBuilder();
    private Attributes lastAttributes;

    private PSExtensionAttachment returnedObject;
    private ObjectBuiltListener listener;

    /** {@inheritDoc} */
    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attributes)
                    throws SAXException {
        boolean handled = false;
        if (PSExtensionAttachment.CATEGORY.equals(uri)) {
            this.lastAttributes = new AttributesImpl(attributes);
            handled = false;
            if (localName.equals(PSSetupCode.ELEMENT)
                    || localName.equals(PSPageTrailerCodeBefore.ELEMENT)
                    || localName.equals(PSSetPageDevice.ELEMENT)
                    || localName.equals(PSCommentBefore.ELEMENT)
                    || localName.equals(PSCommentAfter.ELEMENT)) {
                // handled in endElement
                handled = true;
            }
        }
        if (!handled) {
            if (PSExtensionAttachment.CATEGORY.equals(uri)) {
                throw new SAXException("Unhandled element " + localName
                        + " in namespace: " + uri);
            } else {
                log.warn("Unhandled element " + localName + " in namespace: "
                        + uri);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endElement(final String uri, final String localName,
            final String qName) throws SAXException {
        if (PSExtensionAttachment.CATEGORY.equals(uri)) {
            if (PSSetupCode.ELEMENT.equals(localName)) {
                final String name = this.lastAttributes.getValue("name");
                this.returnedObject = new PSSetupCode(name,
                        this.content.toString());
            } else if (PSSetPageDevice.ELEMENT.equals(localName)) {
                final String name = this.lastAttributes.getValue("name");
                this.returnedObject = new PSSetPageDevice(name,
                        this.content.toString());
            } else if (PSCommentBefore.ELEMENT.equals(localName)) {
                this.returnedObject = new PSCommentBefore(
                        this.content.toString());
            } else if (PSCommentAfter.ELEMENT.equals(localName)) {
                this.returnedObject = new PSCommentAfter(
                        this.content.toString());
            } else if (PSPageTrailerCodeBefore.ELEMENT.equals(localName)) {
                this.returnedObject = new PSPageTrailerCodeBefore(
                        this.content.toString());
            }
        }
        this.content.setLength(0); // Reset text buffer (see characters())
    }

    /** {@inheritDoc} */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        this.content.append(ch, start, length);
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {
        if (this.listener != null) {
            this.listener.notifyObjectBuilt(getObject());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object getObject() {
        return this.returnedObject;
    }

    /** {@inheritDoc} */
    @Override
    public void setObjectBuiltListener(final ObjectBuiltListener listener) {
        this.listener = listener;
    }
}
