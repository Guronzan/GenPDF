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

/* $Id: PSSetPageDevice.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.ps.extensions;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Element for postscript setpagedevice instruction This is a an extension which
 * provides a pass-through value dictionary object for the postscript
 * setpagedevice instruction.
 */
public class PSSetPageDevice extends PSExtensionAttachment {
    /** element name */
    protected static final String ELEMENT = "ps-setpagedevice";

    private static final String ATT_NAME = "name";

    /**
     * name attribute
     */
    protected String name = null;

    /**
     * default constructor
     * 
     * @param content
     *            set page device dictionary
     */
    public PSSetPageDevice(final String content) {
        super(content);
    }

    /**
     * constructor
     * 
     * @param name
     *            name attribute of this setpagedevice content
     * @param content
     *            set page device dictionary
     */
    public PSSetPageDevice(final String name, final String content) {
        this(content);
        this.name = name;
    }

    /**
     * constructor
     */
    public PSSetPageDevice() {
    }

    /** @return the name */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of the setup code object.
     * 
     * @param name
     *            The name to set.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return a string representation of this object
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "PSSetPageDevice(name=" + getName() + ", content='"
                + getContent() + "')";
    }

    /**
     * @return a string representation of this object
     * @see org.apache.fop.render.ps.extensions.PSExtensionAttachment#getElement()
     */
    @Override
    protected String getElement() {
        return ELEMENT;
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
        if (this.name != null && this.name.length() > 0) {
            atts.addAttribute(null, ATT_NAME, ATT_NAME, "CDATA", this.name);
        }
        final String element = getElement();
        handler.startElement(CATEGORY, element, element, atts);
        if (this.content != null && this.content.length() > 0) {
            final char[] chars = this.content.toCharArray();
            handler.characters(chars, 0, chars.length);
        }
        handler.endElement(CATEGORY, element, element);
    }
}
