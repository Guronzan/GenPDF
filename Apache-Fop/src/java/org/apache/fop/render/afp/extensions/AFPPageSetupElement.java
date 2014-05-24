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

/* $Id: AFPPageSetupElement.java 1095882 2011-04-22 07:44:46Z jeremias $ */

package org.apache.fop.render.afp.extensions;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * This class extends the org.apache.fop.extensions.ExtensionObj class. The
 * object facilitates extraction of elements from formatted objects based on the
 * static list as defined in the AFPElementMapping implementation.
 */
public class AFPPageSetupElement extends AbstractAFPExtensionObject {

    private static final String ATT_SRC = "src";

    /**
     * Constructs an AFP object (called by Maker).
     *
     * @param parent
     *            the parent formatting object
     * @param name
     *            the name of the afp element
     */
    public AFPPageSetupElement(final FONode parent, final String name) {
        super(parent, name);
    }

    private AFPPageSetup getPageSetupAttachment() {
        return (AFPPageSetup) getExtensionAttachment();
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        if (AFPElementMapping.TAG_LOGICAL_ELEMENT.equals(getLocalName())) {
            if (this.parent.getNameId() != Constants.FO_SIMPLE_PAGE_MASTER
                    && this.parent.getNameId() != Constants.FO_PAGE_SEQUENCE) {
                invalidChildError(getLocator(), this.parent.getName(),
                        getNamespaceURI(), getName(),
                        "rule.childOfPageSequenceOrSPM");
            }
        } else {
            if (this.parent.getNameId() != Constants.FO_SIMPLE_PAGE_MASTER
                    && this.parent.getNameId() != Constants.FO_PAGE_SEQUENCE
                    && this.parent.getNameId() != Constants.FO_DECLARATIONS) {
                invalidChildError(getLocator(), this.parent.getName(),
                        getNamespaceURI(), getName(),
                        "rule.childOfSPMorPSorDeclarations");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void characters(final char[] data, final int start,
            final int length, final PropertyList pList, final Locator locator)
            throws FOPException {
        final StringBuffer sb = new StringBuffer();
        final AFPPageSetup pageSetup = getPageSetupAttachment();
        if (pageSetup.getContent() != null) {
            sb.append(pageSetup.getContent());
        }
        sb.append(data, start, length);
        pageSetup.setContent(sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList propertyList)
            throws FOPException {
        super.processNode(elementName, locator, attlist, propertyList);
        final AFPPageSetup pageSetup = getPageSetupAttachment();
        if (AFPElementMapping.INCLUDE_PAGE_SEGMENT.equals(elementName)) {
            final String attr = attlist.getValue(ATT_SRC);
            if (attr != null && attr.length() > 0) {
                pageSetup.setValue(attr);
            } else {
                missingPropertyError(ATT_SRC);
            }
        } else if (AFPElementMapping.TAG_LOGICAL_ELEMENT.equals(elementName)) {
            final String attr = attlist.getValue(AFPPageSetup.ATT_VALUE);
            if (attr != null && attr.length() > 0) {
                pageSetup.setValue(attr);
            } else {
                missingPropertyError(AFPPageSetup.ATT_VALUE);
            }
        }
        final String placement = attlist.getValue(AFPPageSetup.ATT_PLACEMENT);
        if (placement != null && placement.length() > 0) {
            pageSetup.setPlacement(ExtensionPlacement.fromXMLValue(placement));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected ExtensionAttachment instantiateExtensionAttachment() {
        return new AFPPageSetup(getLocalName());
    }
}
