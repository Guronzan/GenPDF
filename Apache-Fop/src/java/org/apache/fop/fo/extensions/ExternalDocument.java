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

/* $Id: ExternalDocument.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.fo.extensions;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.GraphicsProperties;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.pagination.AbstractPageSequence;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.xml.sax.Locator;

/**
 * Class for the fox:external-document extension element.
 */
public class ExternalDocument extends AbstractPageSequence implements
        GraphicsProperties {

    // The value of properties relevant for fox:external-document
    private LengthRangeProperty blockProgressionDimension;
    private Length contentHeight;
    private Length contentWidth;
    private int displayAlign;
    private Length height;
    private LengthRangeProperty inlineProgressionDimension;
    private int overflow;
    private int scaling;
    private String src;
    private int textAlign;
    private Length width;

    // Unused but valid items, commented out for performance:
    // private CommonAccessibility commonAccessibility;
    // private CommonAural commonAural;
    // private String contentType;
    // private int scalingMethod;
    // End of property values

    /**
     * Constructs a ExternalDocument object (called by Maker).
     * 
     * @param parent
     *            the parent formatting object
     */
    public ExternalDocument(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.blockProgressionDimension = pList.get(
                PR_BLOCK_PROGRESSION_DIMENSION).getLengthRange();
        this.contentHeight = pList.get(PR_CONTENT_HEIGHT).getLength();
        this.contentWidth = pList.get(PR_CONTENT_WIDTH).getLength();
        this.displayAlign = pList.get(PR_DISPLAY_ALIGN).getEnum();
        this.height = pList.get(PR_HEIGHT).getLength();
        this.inlineProgressionDimension = pList.get(
                PR_INLINE_PROGRESSION_DIMENSION).getLengthRange();
        this.overflow = pList.get(PR_OVERFLOW).getEnum();
        this.scaling = pList.get(PR_SCALING).getEnum();
        this.textAlign = pList.get(PR_TEXT_ALIGN).getEnum();
        this.width = pList.get(PR_WIDTH).getLength();
        this.src = pList.get(PR_SRC).getString();

        if (this.src == null || this.src.length() == 0) {
            missingPropertyError("src");
        }
    }

    /**
     * @throws FOPException
     *             in case of processing exception
     * @see org.apache.fop.fo.FONode#startOfNode()
     */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startExternalDocument(this);
    }

    /**
     * @throws FOPException
     *             in case of processing exception
     * @see org.apache.fop.fo.FONode#endOfNode()
     */
    @Override
    protected void endOfNode() throws FOPException {
        getFOEventHandler().endExternalDocument(this);
        super.endOfNode();
    }

    /**
     * @param loc
     *            a locator
     * @param nsURI
     *            a namespace uri or null
     * @param localName
     *            a local name
     * @throws ValidationException
     *             if invalid child
     * @see org.apache.fop.fo.FONode#validateChildNode(Locator, String, String)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        invalidChildError(loc, nsURI, localName);
    }

    /**
     * Returns the src attribute (the URI to the embedded document).
     * 
     * @return the src attribute
     */
    public String getSrc() {
        return this.src;
    }

    /** {@inheritDoc} */
    @Override
    public LengthRangeProperty getInlineProgressionDimension() {
        return this.inlineProgressionDimension;
    }

    /** {@inheritDoc} */
    @Override
    public LengthRangeProperty getBlockProgressionDimension() {
        return this.blockProgressionDimension;
    }

    /** {@inheritDoc} */
    @Override
    public Length getHeight() {
        return this.height;
    }

    /** {@inheritDoc} */
    @Override
    public Length getWidth() {
        return this.width;
    }

    /** {@inheritDoc} */
    @Override
    public Length getContentHeight() {
        return this.contentHeight;
    }

    /** {@inheritDoc} */
    @Override
    public Length getContentWidth() {
        return this.contentWidth;
    }

    /** {@inheritDoc} */
    @Override
    public int getScaling() {
        return this.scaling;
    }

    /** {@inheritDoc} */
    @Override
    public int getOverflow() {
        return this.overflow;
    }

    /** {@inheritDoc} */
    @Override
    public int getDisplayAlign() {
        return this.displayAlign;
    }

    /** {@inheritDoc} */
    @Override
    public int getTextAlign() {
        return this.textAlign;
    }

    /**
     * @return namespace uri
     * @see org.apache.fop.fo.FONode#getNamespaceURI()
     */
    @Override
    public String getNamespaceURI() {
        return ExtensionElementMapping.URI;
    }

    /**
     * @return namespace prefix
     * @see org.apache.fop.fo.FONode#getNormalNamespacePrefix()
     */
    @Override
    public String getNormalNamespacePrefix() {
        return "fox";
    }

    /**
     * @return local name
     * @see org.apache.fop.fo.FONode#getLocalName()
     */
    @Override
    public String getLocalName() {
        return "external-document";
    }

}
