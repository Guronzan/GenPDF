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

/* $Id: Metadata.java 1130449 2011-06-02 09:30:51Z spepping $ */

package org.apache.xmlgraphics.xmp;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.xmlgraphics.util.QName;
import org.apache.xmlgraphics.util.XMLizable;
import org.apache.xmlgraphics.xmp.merge.MergeRuleSet;
import org.apache.xmlgraphics.xmp.merge.PropertyMerger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class represents the root of an XMP metadata tree. It's more or less
 * equivalent to the x:xmpmeta element together with its nested rdf:RDF element.
 */
public class Metadata implements XMLizable, PropertyAccess {

    private final Map properties = new java.util.HashMap();

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
    public XMPProperty getValueProperty() {
        return getProperty(XMPConstants.RDF_VALUE);
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

    /**
     * Merges this metadata object into a given target metadata object. The
     * merge rule set provided by each schema is used for the merge.
     * 
     * @param target
     *            the target metadata to merge the local metadata into
     */
    public void mergeInto(final Metadata target) {
        final XMPSchemaRegistry registry = XMPSchemaRegistry.getInstance();
        final Iterator iter = this.properties.values().iterator();
        while (iter.hasNext()) {
            final XMPProperty prop = (XMPProperty) iter.next();
            final XMPSchema schema = registry.getSchema(prop.getNamespace());
            final MergeRuleSet rules = schema.getDefaultMergeRuleSet();
            final PropertyMerger merger = rules.getPropertyMergerFor(prop);
            merger.merge(prop, target);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void toSAX(final ContentHandler handler) throws SAXException {
        final AttributesImpl atts = new AttributesImpl();
        handler.startPrefixMapping("x", XMPConstants.XMP_NAMESPACE);
        handler.startElement(XMPConstants.XMP_NAMESPACE, "xmpmeta",
                "x:xmpmeta", atts);
        handler.startPrefixMapping("rdf", XMPConstants.RDF_NAMESPACE);
        handler.startElement(XMPConstants.RDF_NAMESPACE, "RDF", "rdf:RDF", atts);
        // Get all property namespaces
        final Set namespaces = new java.util.HashSet();
        Iterator iter = this.properties.keySet().iterator();
        while (iter.hasNext()) {
            final QName n = (QName) iter.next();
            namespaces.add(n.getNamespaceURI());
        }
        // One Description element per namespace
        iter = namespaces.iterator();
        while (iter.hasNext()) {
            final String ns = (String) iter.next();
            final XMPSchema schema = XMPSchemaRegistry.getInstance().getSchema(
                    ns);
            String prefix = schema != null ? schema.getPreferredPrefix() : null;

            boolean first = true;
            boolean empty = true;

            final Iterator props = this.properties.values().iterator();
            while (props.hasNext()) {
                final XMPProperty prop = (XMPProperty) props.next();
                if (prop.getName().getNamespaceURI().equals(ns)) {
                    if (first) {
                        if (prefix == null) {
                            prefix = prop.getName().getPrefix();
                        }
                        atts.clear();
                        atts.addAttribute(XMPConstants.RDF_NAMESPACE, "about",
                                "rdf:about", "CDATA", "");
                        if (prefix != null) {
                            handler.startPrefixMapping(prefix, ns);
                        }
                        handler.startElement(XMPConstants.RDF_NAMESPACE,
                                "Description", "rdf:Description", atts);
                        empty = false;
                        first = false;
                    }
                    prop.toSAX(handler);
                }
            }
            if (!empty) {
                handler.endElement(XMPConstants.RDF_NAMESPACE, "Description",
                        "rdf:Description");
                if (prefix != null) {
                    handler.endPrefixMapping(prefix);
                }
            }
        }

        handler.endElement(XMPConstants.RDF_NAMESPACE, "RDF", "rdf:RDF");
        handler.endPrefixMapping("rdf");
        handler.endElement(XMPConstants.XMP_NAMESPACE, "xmpmeta", "x:xmpmeta");
        handler.endPrefixMapping("x");
    }

}
