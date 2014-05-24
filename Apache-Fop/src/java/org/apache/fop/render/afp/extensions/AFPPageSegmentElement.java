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

/* $Id: AFPPageSegmentElement.java 1005350 2010-10-07 07:41:48Z jeremias $ */

package org.apache.fop.render.afp.extensions;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class extends the org.apache.fop.extensions.ExtensionObj class. The
 * object faciliates extraction of elements from formatted objects based on the
 * static list as defined in the AFPElementMapping implementation.
 * <p/>
 */
public class AFPPageSegmentElement extends AFPPageSetupElement {

    private static final String ATT_RESOURCE_SRC = "resource-file";

    /**
     * Constructs an AFP object (called by Maker).
     *
     * @param parent
     *            the parent formatting object
     * @param name
     *            the name of the afp element
     */
    public AFPPageSegmentElement(final FONode parent, final String name) {
        super(parent, name);
    }

    private AFPPageSegmentSetup getPageSetupAttachment() {
        return (AFPPageSegmentSetup) getExtensionAttachment();
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList propertyList)
            throws FOPException {

        final AFPPageSegmentSetup pageSetup = getPageSetupAttachment();
        super.processNode(elementName, locator, attlist, propertyList);

        final String attr = attlist.getValue(ATT_RESOURCE_SRC);

        if (attr != null && attr.length() > 0) {
            pageSetup.setResourceSrc(attr);
        }

    }

    /** {@inheritDoc} */
    @Override
    protected ExtensionAttachment instantiateExtensionAttachment() {
        return new AFPPageSegmentSetup(getLocalName());
    }

    /**
     * This is the pass-through value object for the AFP extension.
     */
    public static class AFPPageSegmentSetup extends AFPPageSetup {

        private static final long serialVersionUID = 1L;

        private String resourceSrc;

        /**
         * Default constructor.
         *
         * @param elementName
         *            the name of the setup code object, may be null
         */
        public AFPPageSegmentSetup(final String elementName) {
            super(elementName);
        }

        /**
         * Returns the source URI for the page segment.
         * 
         * @return the source URI
         */
        public String getResourceSrc() {
            return this.resourceSrc;
        }

        /**
         * Sets the source URI for the page segment.
         * 
         * @param resourceSrc
         *            the source URI
         */
        public void setResourceSrc(final String resourceSrc) {
            this.resourceSrc = resourceSrc.trim();
        }

        /** {@inheritDoc} */
        @Override
        public void toSAX(final ContentHandler handler) throws SAXException {
            final AttributesImpl atts = new AttributesImpl();
            if (this.name != null && this.name.length() > 0) {
                atts.addAttribute(null, ATT_NAME, ATT_NAME, "CDATA", this.name);
            }
            if (this.value != null && this.value.length() > 0) {
                atts.addAttribute(null, ATT_VALUE, ATT_VALUE, "CDATA",
                        this.value);
            }

            if (this.resourceSrc != null && this.resourceSrc.length() > 0) {
                atts.addAttribute(null, ATT_RESOURCE_SRC, ATT_RESOURCE_SRC,
                        "CDATA", this.resourceSrc);
            }

            handler.startElement(CATEGORY, this.elementName, this.elementName,
                    atts);
            if (this.content != null && this.content.length() > 0) {
                final char[] chars = this.content.toCharArray();
                handler.characters(chars, 0, chars.length);
            }
            handler.endElement(CATEGORY, this.elementName, this.elementName);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "AFPPageSegmentSetup(element-name=" + getElementName()
                    + " name=" + getName() + " value=" + getValue()
                    + " resource=" + getResourceSrc() + ")";
        }

    }

}
