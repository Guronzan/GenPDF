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

/* $Id: PDFExtensionHandler.java 1036179 2010-11-17 19:45:27Z spepping $ */

package org.apache.fop.render.pdf.extensions;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.util.ContentHandlerFactory;
import org.apache.fop.util.ContentHandlerFactory.ObjectBuiltListener;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * ContentHandler (parser) for restoring PDF extension objects from XML.
 */
@Slf4j
public class PDFExtensionHandler extends DefaultHandler implements
ContentHandlerFactory.ObjectSource {

    private Attributes lastAttributes;

    private PDFExtensionAttachment returnedObject;
    private ObjectBuiltListener listener;

    /** {@inheritDoc} */
    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attributes)
                    throws SAXException {
        boolean handled = false;
        if (PDFExtensionAttachment.CATEGORY.equals(uri)) {
            this.lastAttributes = new AttributesImpl(attributes);
            handled = false;
            if (localName.equals(PDFEmbeddedFileExtensionAttachment.ELEMENT)) {
                // handled in endElement
                handled = true;
            }
        }
        if (!handled) {
            if (PDFExtensionAttachment.CATEGORY.equals(uri)) {
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
        if (PDFExtensionAttachment.CATEGORY.equals(uri)) {
            if (PDFEmbeddedFileExtensionAttachment.ELEMENT.equals(localName)) {
                final String name = this.lastAttributes.getValue("name");
                final String src = this.lastAttributes.getValue("src");
                final String desc = this.lastAttributes.getValue("description");
                this.returnedObject = new PDFEmbeddedFileExtensionAttachment(
                        name, src, desc);
            }
        }
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
