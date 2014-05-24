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

/* $Id: PSExtensionAttachment.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.ps.extensions;

import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.xmlgraphics.util.XMLizable;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This is the pass-through value object for the PostScript extension.
 */
public abstract class PSExtensionAttachment implements ExtensionAttachment,
        XMLizable {

    /** extension node content */
    protected String content;

    /** The category URI for this extension attachment. */
    public static final String CATEGORY = "apache:fop:extensions:postscript";

    /**
     * Default constructor.
     * 
     * @param content
     *            the content of the setup code object
     */
    public PSExtensionAttachment(final String content) {
        this.content = content;
    }

    /**
     * No-argument contructor.
     */
    public PSExtensionAttachment() {
    }

    /**
     * @return the category URI
     * @see org.apache.fop.fo.extensions.ExtensionAttachment#getCategory()
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /** @return the content */
    public String getContent() {
        return this.content;
    }

    /**
     * Sets the content for the setup code object.
     * 
     * @param content
     *            The content to set.
     */
    public void setContent(final String content) {
        this.content = content;
    }

    /**
     * Generates SAX events representing the object's state.
     *
     * @param handler
     *            ContentHandler instance to send the SAX events to
     * @throws SAXException
     *             if there's a problem generating the SAX events
     * @see org.apache.xmlgraphics.util.XMLizable#toSAX(org.xml.sax.ContentHandler)
     */
    @Override
    public void toSAX(final ContentHandler handler) throws SAXException {
        final AttributesImpl atts = new AttributesImpl();
        final String element = getElement();
        handler.startElement(CATEGORY, element, element, atts);
        if (this.content != null && this.content.length() > 0) {
            final char[] chars = this.content.toCharArray();
            handler.characters(chars, 0, chars.length);
        }
        handler.endElement(CATEGORY, element, element);
    }

    /** @return type name */
    public String getType() {
        final String className = getClass().getName();
        return className.substring(className.lastIndexOf('.') + 3);
    }

    /**
     * @return a string representation of this object
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getType() + ": content=" + this.content;
    }

    /** @return element */
    protected abstract String getElement();
}
