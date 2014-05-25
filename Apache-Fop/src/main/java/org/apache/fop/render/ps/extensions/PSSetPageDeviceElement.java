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

/* $Id: PSSetPageDeviceElement.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.ps.extensions;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Extension element for ps:ps-setpagedevice.
 */
public class PSSetPageDeviceElement extends AbstractPSExtensionElement {

    /** The element name */
    protected static final String ELEMENT = "ps-setpagedevice";

    /**
     * Main constructor
     * 
     * @param parent
     *            parent FO node
     */
    protected PSSetPageDeviceElement(final FONode parent) {
        super(parent);
    }

    /**
     * Called after processNode() is called. Subclasses can do additional
     * processing.
     * 
     * @throws FOPException
     *             if there's a problem during processing
     * @see org.apache.fop.fo.FONode#startOfNode()
     */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        if (!(this.parent.getNameId() == Constants.FO_DECLARATIONS || this.parent
                .getNameId() == Constants.FO_SIMPLE_PAGE_MASTER)) {
            invalidChildError(getLocator(), this.parent.getName(),
                    getNamespaceURI(), getName(),
                    "rule.childOfSPMorDeclarations");
        }
    }

    /**
     * Initialize the node with its name, location information, and attributes
     * The attributes must be used immediately as the sax attributes will be
     * altered for the next element.
     * 
     * @param elementName
     *            element name (e.g., "fo:block")
     * @param locator
     *            Locator object (ignored by default)
     * @param attlist
     *            Collection of attributes passed to us from the parser.
     * @param propertyList
     *            property list
     * @throws FOPException
     *             if there's a problem during processing
     * @see org.apache.fop.fo.FONode#processNode
     */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList propertyList)
            throws FOPException {
        final String name = attlist.getValue("name");
        if (name != null && name.length() > 0) {
            ((PSSetPageDevice) getExtensionAttachment()).setName(name);
        }
    }

    /**
     * @return local name
     * @see org.apache.fop.fo.FONode#getLocalName()
     */
    @Override
    public String getLocalName() {
        return ELEMENT;
    }

    /**
     * @return a new PSSetPageDevice object
     * @see org.apache.fop.render.ps.extensions.AbstractPSExtensionElement
     *      #instantiateExtensionAttachment()
     */
    @Override
    protected ExtensionAttachment instantiateExtensionAttachment() {
        return new PSSetPageDevice();
    }
}
