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

/* $Id: AFPPageOverlayElement.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.render.afp.extensions;

import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPUnitConverter;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.xmlgraphics.util.UnitConv;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * This class extends the
 * org.apache.fop.render.afp.extensions.AbstractAFPExtensionObject class. This
 * object will be used to map the page overlay object in AFPElementMapping.
 * <p/>
 */
public class AFPPageOverlayElement extends AbstractAFPExtensionObject {

    private static final String ATT_X = "x";
    private static final String ATT_Y = "y";

    /**
     * Constructs an AFP object (called by Maker).
     *
     * @param parent
     *            the parent formatting object
     * @param name
     *            the name of the afp element
     */
    public AFPPageOverlayElement(final FONode parent, final String name) {
        super(parent, name);
    }

    private AFPPageOverlay getPageSetupAttachment() {
        return (AFPPageOverlay) getExtensionAttachment();
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        if (AFPElementMapping.INCLUDE_PAGE_OVERLAY.equals(getLocalName())) {
            if (this.parent.getNameId() != Constants.FO_SIMPLE_PAGE_MASTER
                    && this.parent.getNameId() != Constants.FO_PAGE_SEQUENCE) {
                invalidChildError(getLocator(), this.parent.getName(),
                        getNamespaceURI(), getName(),
                        "rule.childOfPageSequenceOrSPM");
            }
        } else {
            if (this.parent.getNameId() != Constants.FO_SIMPLE_PAGE_MASTER) {
                invalidChildError(getLocator(), this.parent.getName(),
                        getNamespaceURI(), getName(), "rule.childOfSPM");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList propertyList)
            throws FOPException {
        super.processNode(elementName, locator, attlist, propertyList);
        final AFPPageOverlay pageOverlay = getPageSetupAttachment();
        if (AFPElementMapping.INCLUDE_PAGE_OVERLAY.equals(elementName)) {
            // convert user specific units to mpts and set the coordinates for
            // the page overlay
            final AFPPaintingState paintingState = new AFPPaintingState();
            final AFPUnitConverter unitConverter = new AFPUnitConverter(
                    paintingState);
            final int x = (int) unitConverter.mpt2units(UnitConv
                    .convert(attlist.getValue(ATT_X)));
            final int y = (int) unitConverter.mpt2units(UnitConv
                    .convert(attlist.getValue(ATT_Y)));
            pageOverlay.setX(x);
            pageOverlay.setY(y);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected ExtensionAttachment instantiateExtensionAttachment() {
        return new AFPPageOverlay();
    }
}
