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

/* $Id: AbstractAFPExtensionObject.java 748981 2009-03-01 08:55:35Z jeremias $ */

package org.apache.fop.render.afp.extensions;

// FOP
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Base class for the AFP-specific extension elements.
 */
public abstract class AbstractAFPExtensionObject extends FONode {

    /**
     * the AFP extension attachment
     */
    protected AFPExtensionAttachment extensionAttachment;

    /**
     * the element name of this extension
     */
    protected String name;

    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     * @param parent
     *            the parent formatting object
     * @param name
     *            the name of the afp element
     */
    public AbstractAFPExtensionObject(final FONode parent, final String name) {
        super(parent);
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getNamespaceURI() {
        return AFPElementMapping.NAMESPACE;
    }

    /** {@inheritDoc} */
    @Override
    public String getNormalNamespacePrefix() {
        return AFPElementMapping.NAMESPACE_PREFIX;
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList propertyList)
            throws FOPException {
        getExtensionAttachment();
        final String attr = attlist.getValue("name");
        if (attr != null && attr.length() > 0) {
            this.extensionAttachment.setName(attr);
        } else {
            throw new FOPException(elementName + " must have a name attribute.");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        super.endOfNode();
    }

    /**
     * Instantiates extension attachment object
     * 
     * @return extension attachment
     */
    protected abstract ExtensionAttachment instantiateExtensionAttachment();

    /** {@inheritDoc} */
    @Override
    public ExtensionAttachment getExtensionAttachment() {
        if (this.extensionAttachment == null) {
            this.extensionAttachment = (AFPExtensionAttachment) instantiateExtensionAttachment();
        }
        return this.extensionAttachment;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return this.name;
    }
}
