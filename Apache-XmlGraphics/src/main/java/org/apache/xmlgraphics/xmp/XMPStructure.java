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

/* $Id: XMPStructure.java 888794 2009-12-09 13:21:04Z jeremias $ */

package org.apache.xmlgraphics.xmp;

import java.util.Iterator;
import java.util.Map;

import org.apache.xmlgraphics.util.QName;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Represents an XMP structure as defined by the XMP specification.
 */
public class XMPStructure extends XMPComplexValue implements PropertyAccess {

    private final Map properties = new java.util.HashMap();

    /**
     * Main constructor
     */
    public XMPStructure() {
    }

    /** {@inheritDoc} */
    @Override
    public Object getSimpleValue() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setProperty(final XMPProperty prop) {
        this.properties.put(prop.getName(), prop);
    }

    /** {@inheritDoc} */
    @Override
    public XMPProperty getProperty(final String uri, final String localName) {
        return getProperty(new QName(uri, localName));
    }

    /** {@inheritDoc} */
    @Override
    public XMPProperty getValueProperty() {
        return getProperty(XMPConstants.RDF_VALUE);
    }

    /** {@inheritDoc} */
    @Override
    public XMPProperty getProperty(final QName name) {
        final XMPProperty prop = (XMPProperty) this.properties.get(name);
        return prop;
    }

    /** {@inheritDoc} */
    @Override
    public XMPProperty removeProperty(final QName name) {
        return (XMPProperty) this.properties.remove(name);
    }

    /** {@inheritDoc} */
    @Override
    public int getPropertyCount() {
        return this.properties.size();
    }

    /** {@inheritDoc} */
    @Override
    public Iterator iterator() {
        return this.properties.keySet().iterator();
    }

    /** {@inheritDoc} */
    @Override
    public void toSAX(final ContentHandler handler) throws SAXException {
        final AttributesImpl atts = new AttributesImpl();
        atts.clear();
        handler.startElement(XMPConstants.RDF_NAMESPACE, "RDF",
                "rdf:Description", atts);

        final Iterator props = this.properties.values().iterator();
        while (props.hasNext()) {
            final XMPProperty prop = (XMPProperty) props.next();
            // if (prop.getName().getNamespaceURI().equals(ns)) {
            prop.toSAX(handler);
            // }
        }
        handler.endElement(XMPConstants.RDF_NAMESPACE, "RDF", "rdf:Description");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "XMP structure: " + getPropertyCount();
    }

}
