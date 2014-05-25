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

/* $Id: XMLObj.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.fo;

import java.awt.geom.Point2D;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.util.ContentHandlerFactory.ObjectBuiltListener;
import org.apache.fop.util.XMLConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Abstract class modelling generic, non-XSL-FO XML objects. Such objects are
 * stored in a DOM.
 */
@Slf4j
public abstract class XMLObj extends FONode implements ObjectBuiltListener {

    // temp reference for attributes
    private Attributes attr = null;

    /** DOM element representing this node */
    protected Element element;

    /** DOM document containing this node */
    protected Document doc;

    /** Name of the node */
    protected String name;

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public XMLObj(final FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc} <br>
     * Here, blocks XSL-FO's from having non-FO parents.
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList propertyList)
                    throws FOPException {
        setLocator(locator);
        this.name = elementName;
        this.attr = attlist;
    }

    /**
     * @return DOM document representing this foreign XML
     */
    public Document getDOMDocument() {
        return this.doc;
    }

    /**
     * Returns the dimensions of the generated area in pts.
     *
     * @param view
     *            Point2D instance to receive the dimensions
     * @return the requested dimensions in pts.
     */
    public Point2D getDimension(final Point2D view) {
        return null;
    }

    /**
     * Retrieve the intrinsic alignment-adjust of the child element.
     *
     * @return the intrinsic alignment-adjust.
     */
    public Length getIntrinsicAlignmentAdjust() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return this.name;
    }

    private static HashMap ns = new HashMap();
    static {
        ns.put(XMLConstants.XLINK_PREFIX, XMLConstants.XLINK_NAMESPACE);
    }

    /**
     * Add an element to the DOM document
     *
     * @param doc
     *            DOM document to which to add an element
     * @param parent
     *            the parent element of the element that is being added
     */
    public void addElement(final Document doc, final Element parent) {
        this.doc = doc;
        this.element = doc.createElementNS(getNamespaceURI(), this.name);

        setAttributes(this.element, this.attr);
        this.attr = null;
        parent.appendChild(this.element);
    }

    private static void setAttributes(final Element element,
            final Attributes attr) {
        for (int count = 0; count < attr.getLength(); count++) {
            final String rf = attr.getValue(count);
            final String qname = attr.getQName(count);
            final int idx = qname.indexOf(":");
            if (idx == -1) {
                element.setAttribute(qname, rf);
            } else {
                final String pref = qname.substring(0, idx);
                final String tail = qname.substring(idx + 1);
                if (pref.equals(XMLConstants.XMLNS_PREFIX)) {
                    ns.put(tail, rf);
                } else {
                    element.setAttributeNS((String) ns.get(pref), tail, rf);
                }
            }
        }
    }

    /**
     * Add the top-level element to the DOM document
     *
     * @param doc
     *            DOM document
     * @param svgRoot
     *            non-XSL-FO element to be added as the root of this document
     */
    public void buildTopLevel(final Document doc, final Element svgRoot) {
        // build up the info for the top level element
        setAttributes(this.element, this.attr);
    }

    /**
     * Create an empty DOM document
     *
     * @return DOM document
     */
    public Document createBasicDocument() {
        this.doc = null;

        this.element = null;
        try {
            final DocumentBuilderFactory fact = DocumentBuilderFactory
                    .newInstance();
            fact.setNamespaceAware(true);
            this.doc = fact.newDocumentBuilder().newDocument();
            final Element el = this.doc.createElementNS(getNamespaceURI(),
                    this.name);
            this.doc.appendChild(el);

            this.element = this.doc.getDocumentElement();
            buildTopLevel(this.doc, this.element);
            if (!this.element.hasAttributeNS(XMLConstants.XMLNS_NAMESPACE_URI,
                    XMLConstants.XMLNS_PREFIX)) {
                this.element.setAttributeNS(XMLConstants.XMLNS_NAMESPACE_URI,
                        XMLConstants.XMLNS_PREFIX, getNamespaceURI());
            }

        } catch (final Exception e) {
            // TODO this is ugly because there may be subsequent failures like
            // NPEs
            log.error("Error while trying to instantiate a DOM Document", e);
        }
        return this.doc;
    }

    /** {@inheritDoc} */
    @Override
    protected void addChildNode(final FONode child) {
        if (child instanceof XMLObj) {
            ((XMLObj) child).addElement(this.doc, this.element);
        } else {
            // in theory someone might want to embed some defined
            // xml (eg. fo) inside the foreign xml
            // they could use a different namespace
            log.debug("Invalid element: " + child.getName()
                    + " inside foreign xml markup");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void characters(final char[] data, final int start,
            final int length, final PropertyList pList, final Locator locator)
                    throws FOPException {
        super.characters(data, start, length, pList, locator);
        final String str = new String(data, start, length);
        final org.w3c.dom.Text text = this.doc.createTextNode(str);
        this.element.appendChild(text);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyObjectBuilt(final Object obj) {
        this.doc = (Document) obj;
        this.element = this.doc.getDocumentElement();
    }

}
