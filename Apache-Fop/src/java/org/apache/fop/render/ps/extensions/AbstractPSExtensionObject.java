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

/* $Id: AbstractPSExtensionObject.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.render.ps.extensions;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Base class for the PostScript-specific extension elements.
 */
public abstract class AbstractPSExtensionObject extends FONode {

    private final PSSetupCode setupCode = new PSSetupCode();

    /**
     * Main constructor.
     * 
     * @param parent
     *            the parent node
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    public AbstractPSExtensionObject(final FONode parent) {
        super(parent);
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
    protected void characters(final char[] data, final int start,
            final int length, final PropertyList pList, final Locator locator) {
        final String content = this.setupCode.getContent();
        if (content != null) {
            final StringBuffer sb = new StringBuffer(content);
            sb.append(data, start, length);
            this.setupCode.setContent(sb.toString());
        } else {
            this.setupCode.setContent(new String(data, start, length));
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getNamespaceURI() {
        return PSExtensionElementMapping.NAMESPACE;
    }

    /** {@inheritDoc} */
    @Override
    public String getNormalNamespacePrefix() {
        return "ps";
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList propertyList)
            throws FOPException {
        final String name = attlist.getValue("name");
        if (name != null && name.length() > 0) {
            this.setupCode.setName(name);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        super.endOfNode();
        final String s = this.setupCode.getContent();
        if (s == null || s.length() == 0) {
            missingChildElementError("#PCDATA");
        }
    }

    /** {@inheritDoc} */
    @Override
    public ExtensionAttachment getExtensionAttachment() {
        return this.setupCode;
    }

}
