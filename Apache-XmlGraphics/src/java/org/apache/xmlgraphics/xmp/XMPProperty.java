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

/* $Id: XMPProperty.java 1311929 2012-04-10 19:00:48Z gadams $ */

package org.apache.xmlgraphics.xmp;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import org.apache.xmlgraphics.util.QName;
import org.apache.xmlgraphics.util.XMLizable;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class is the base class for all XMP properties.
 */
public class XMPProperty implements XMLizable {

    private final QName name;
    private Object value;
    private String xmllang;
    private Map qualifiers;
    private final boolean uri;

    /**
     * Creates a new XMP property.
     * 
     * @param name
     *            the name of the property
     * @param value
     *            the value for the property
     */
    public XMPProperty(final QName name, final Object value) {
        this.name = name;
        this.value = value;
        this.uri = false;
    }

    /** @return the qualified name of the property (namespace URI + local name) */
    public QName getName() {
        return this.name;
    }

    /** @return the namespace URI of the property */
    public String getNamespace() {
        return getName().getNamespaceURI();
    }

    /**
     * Sets the value of the property
     * 
     * @param value
     *            the new value
     */
    public void setValue(final Object value) {
        this.value = value;
    }

    /**
     * @return the property value (can be a normal Java object (normally a
     *         String) or a descendant of XMPComplexValue.
     */
    public Object getValue() {
        return this.value;
    }

    /**
     * Sets the xml:lang value for this property
     * 
     * @param lang
     *            the language ("x-default" for the default language, null to
     *            make the value language-independent)
     */
    public void setXMLLang(final String lang) {
        this.xmllang = lang;
    }

    /**
     * @return the language for language-dependent values ("x-default" for the
     *         default language)
     */
    public String getXMLLang() {
        return this.xmllang;
    }

    /**
     * Indicates whether the property is an array.
     * 
     * @return true if the property is an array
     */
    public boolean isArray() {
        return this.value instanceof XMPArray;
    }

    /** @return the XMPArray for an array or null if the value is not an array. */
    public XMPArray getArrayValue() {
        return isArray() ? (XMPArray) this.value : null;
    }

    /**
     * Converts a simple value to an array of a given type if the value is not
     * already an array.
     * 
     * @param type
     *            the desired type of array
     * @return the array value
     */
    public XMPArray convertSimpleValueToArray(final XMPArrayType type) {
        if (getArrayValue() == null) {
            final XMPArray array = new XMPArray(type);
            if (getXMLLang() != null) {
                array.add(getValue().toString(), getXMLLang());
            } else {
                array.add(getValue());
            }
            setValue(array);
            setXMLLang(null);
            return array;
        } else {
            return getArrayValue();
        }
    }

    /**
     * @return the XMPStructure for a structure or null if the value is not a
     *         structure.
     */
    public PropertyAccess getStructureValue() {
        return this.value instanceof XMPStructure ? (XMPStructure) this.value
                : null;
    }

    private boolean hasPropertyQualifiers() {
        return this.qualifiers == null || this.qualifiers.size() == 0;
    }

    /**
     * Indicates whether this property is actually not a structure, but a normal
     * property with property qualifiers. If this method returns true, this
     * structure can be converted to an simple XMPProperty using the simplify()
     * method.
     * 
     * @return true if this property is a structure property with property
     *         qualifiers
     */
    public boolean isQualifiedProperty() {
        final PropertyAccess props = getStructureValue();
        if (props != null) {
            final XMPProperty rdfValue = props.getValueProperty();
            return rdfValue != null;
        } else {
            return hasPropertyQualifiers();
        }
    }

    public void simplify() {
        final PropertyAccess props = getStructureValue();
        if (props != null) {
            final XMPProperty rdfValue = props.getValueProperty();
            if (rdfValue != null) {
                if (hasPropertyQualifiers()) {
                    throw new IllegalStateException(
                            "Illegal internal state"
                                    + " (qualifiers present on non-simplified property)");
                }
                final XMPProperty prop = new XMPProperty(getName(), rdfValue);
                final Iterator iter = props.iterator();
                while (iter.hasNext()) {
                    final QName name = (QName) iter.next();
                    if (!XMPConstants.RDF_VALUE.equals(name)) {
                        prop.setPropertyQualifier(name, props.getProperty(name));
                    }
                }
            }
        }
    }

    private void setPropertyQualifier(final QName name,
            final XMPProperty property) {
        if (this.qualifiers == null) {
            this.qualifiers = new java.util.HashMap();
        }
        this.qualifiers.put(name, property);
    }

    private String getEffectiveQName() {
        String prefix = getName().getPrefix();
        if (prefix == null || "".equals(prefix)) {
            final XMPSchema schema = XMPSchemaRegistry.getInstance().getSchema(
                    getNamespace());
            prefix = schema.getPreferredPrefix();
        }
        return prefix + ":" + getName().getLocalName();
    }

    /** @see org.apache.xmlgraphics.util.XMLizable#toSAX(org.xml.sax.ContentHandler) */
    @Override
    public void toSAX(final ContentHandler handler) throws SAXException {
        final AttributesImpl atts = new AttributesImpl();
        final String qName = getEffectiveQName();
        if (this.value instanceof URI) {
            atts.addAttribute(XMPConstants.RDF_NAMESPACE, "resource",
                    "rdf:resource", "CDATA", ((URI) this.value).toString());
        }
        handler.startElement(getName().getNamespaceURI(), getName()
                .getLocalName(), qName, atts);
        if (this.value instanceof XMPComplexValue) {
            final XMPComplexValue cv = (XMPComplexValue) this.value;
            cv.toSAX(handler);
        } else if (!(this.value instanceof URI)) {
            final char[] chars = this.value.toString().toCharArray();
            handler.characters(chars, 0, chars.length);
        }
        handler.endElement(getName().getNamespaceURI(), getName()
                .getLocalName(), qName);
    }

    /** @see java.lang.Object#toString() */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("XMP Property ");
        sb.append(getName()).append(": ");
        sb.append(getValue());
        return sb.toString();
    }

}
