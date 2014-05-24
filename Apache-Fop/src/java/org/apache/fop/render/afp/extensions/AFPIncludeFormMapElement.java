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

/* $Id: AFPIncludeFormMapElement.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.afp.extensions;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * This class extends the {@link org.apache.fop.fo.extensions.ExtensionObj}
 * class. It represents the "include-form-map" extension in the FO tree.
 */
public class AFPIncludeFormMapElement extends AbstractAFPExtensionObject {

    private static final String ATT_SRC = "src";

    /**
     * Constructs an AFP object (called by Maker).
     *
     * @param parent
     *            the parent formatting object
     * @param name
     *            the name of the AFP element
     */
    public AFPIncludeFormMapElement(final FONode parent, final String name) {
        super(parent, name);
    }

    private AFPIncludeFormMap getFormMapAttachment() {
        return (AFPIncludeFormMap) getExtensionAttachment();
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        if (this.parent.getNameId() != Constants.FO_DECLARATIONS) {
            invalidChildError(getLocator(), this.parent.getName(),
                    getNamespaceURI(), getName(), "rule.childOfDeclarations");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList propertyList)
            throws FOPException {
        super.processNode(elementName, locator, attlist, propertyList);
        final AFPIncludeFormMap formMap = getFormMapAttachment();
        final String attr = attlist.getValue(ATT_SRC);
        if (attr != null && attr.length() > 0) {
            try {
                formMap.setSrc(new URI(attr));
            } catch (final URISyntaxException e) {
                getFOValidationEventProducer().invalidPropertyValue(this,
                        elementName, ATT_SRC, attr, null, getLocator());
            }
        } else {
            missingPropertyError(ATT_SRC);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected ExtensionAttachment instantiateExtensionAttachment() {
        return new AFPIncludeFormMap();
    }
}
