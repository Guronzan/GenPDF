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

/* $Id: Footnote.java 1242848 2012-02-10 16:51:08Z phancock $ */

package org.apache.fop.fo.flow;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_footnote">
 * <code>fo:footnote</code></a> object.
 */
public class Footnote extends FObj implements CommonAccessibilityHolder {

    private CommonAccessibility commonAccessibility;

    private Inline footnoteCitation = null;
    private FootnoteBody footnoteBody;

    /**
     * Create a Footnote instance that is a child of the given {@link FONode}
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public Footnote(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.commonAccessibility = CommonAccessibility.getInstance(pList);
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        getFOEventHandler().startFootnote(this);
    }

    /**
     * Make sure content model satisfied, if so then tell the
     * {@link org.apache.fop.fo.FOEventHandler} that we are at the end of the
     * footnote.
     *
     * {@inheritDoc}
     */
    @Override
    protected void endOfNode() throws FOPException {
        super.endOfNode();
        if (this.footnoteCitation == null || this.footnoteBody == null) {
            missingChildElementError("(inline,footnote-body)");
        }
        getFOEventHandler().endFootnote(this);
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: (inline,footnote-body) TODO implement additional
     * constraint: A fo:footnote is not permitted to have a fo:float,
     * fo:footnote, or fo:marker as a descendant. TODO implement additional
     * constraint: A fo:footnote is not permitted to have as a descendant a
     * fo:block-container that generates an absolutely positioned area.
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (localName.equals("inline")) {
                if (this.footnoteCitation != null) {
                    tooManyNodesError(loc, "fo:inline");
                }
            } else if (localName.equals("footnote-body")) {
                if (this.footnoteCitation == null) {
                    nodesOutOfOrderError(loc, "fo:inline", "fo:footnote-body");
                } else if (this.footnoteBody != null) {
                    tooManyNodesError(loc, "fo:footnote-body");
                }
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addChildNode(final FONode child) {
        if (child.getNameId() == FO_INLINE) {
            this.footnoteCitation = (Inline) child;
        } else if (child.getNameId() == FO_FOOTNOTE_BODY) {
            this.footnoteBody = (FootnoteBody) child;
        }
    }

    /** {@inheritDoc} */
    @Override
    public CommonAccessibility getCommonAccessibility() {
        return this.commonAccessibility;
    }

    /**
     * Public accessor for inline FO
     *
     * @return the {@link Inline} child
     */
    public Inline getFootnoteCitation() {
        return this.footnoteCitation;
    }

    /**
     * Public accessor for footnote-body FO
     *
     * @return the {@link FootnoteBody} child
     */
    public FootnoteBody getFootnoteBody() {
        return this.footnoteBody;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "footnote";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_FOOTNOTE}
     */
    @Override
    public int getNameId() {
        return FO_FOOTNOTE;
    }
}
