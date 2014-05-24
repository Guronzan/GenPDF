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

/* $Id: Wrapper.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.fo.flow;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.FObjMixed;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href=http://www.w3.org/TR/xsl/#fo_wrapper">
 * <code>fo:wrapper</code></a> object. The <code>fo:wrapper</code> object serves
 * as a property holder for its child node objects.
 */
public class Wrapper extends FObjMixed implements CommonAccessibilityHolder {

    // used for FO validation
    private boolean blockOrInlineItemFound = false;

    private CommonAccessibility commonAccessibility;

    /**
     * Create a Wrapper instance that is a child of the given {@link FONode}
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public Wrapper(final FONode parent) {
        super(parent);
    }

    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.commonAccessibility = CommonAccessibility.getInstance(pList);
    }

    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startWrapper(this);
    }

    @Override
    protected void endOfNode() throws FOPException {
        super.endOfNode();
        getFOEventHandler().endWrapper(this);
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: marker* (#PCDATA|%inline;|%block;)* <br>
     * <i>Additionally (unimplemented): "An fo:wrapper that is a child of an
     * fo:multi-properties is only permitted to have children that would be
     * permitted in place of the fo:multi-properties."</i>
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if ("marker".equals(localName)) {
                if (this.blockOrInlineItemFound) {
                    nodesOutOfOrderError(loc, "fo:marker",
                            "(#PCDATA|%inline;|%block;)");
                }
            } else if (isBlockOrInlineItem(nsURI, localName)) {
                /*
                 * delegate validation to parent, but keep the error reporting
                 * tidy. If we would simply call validateChildNode() on the
                 * parent, the user would get a wrong impression, as only the
                 * locator (if any) will contain a reference to the offending
                 * fo:wrapper.
                 */
                try {
                    FONode.validateChildNode(this.parent, loc, nsURI, localName);
                } catch (final ValidationException vex) {
                    invalidChildError(loc, getName(), FO_URI, localName,
                            "rule.wrapperInvalidChildForParent");
                }
                this.blockOrInlineItemFound = true;
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void addChildNode(final FONode child) throws FOPException {
        super.addChildNode(child);
        /*
         * If the child is a text node, and it generates areas (i.e. contains
         * either non-white-space or preserved white-space), then check whether
         * the nearest non-wrapper ancestor allows this.
         */
        if (child instanceof FOText && ((FOText) child).willCreateArea()) {
            FONode ancestor = this.parent;
            while (ancestor.getNameId() == Constants.FO_WRAPPER) {
                ancestor = ancestor.getParent();
            }
            if (!(ancestor instanceof FObjMixed)) {
                invalidChildError(getLocator(), getLocalName(), FONode.FO_URI,
                        "#PCDATA", "rule.wrapperInvalidChildForParent");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "wrapper";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_WRAPPER}
     */
    @Override
    public int getNameId() {
        return FO_WRAPPER;
    }

    @Override
    public CommonAccessibility getCommonAccessibility() {
        return this.commonAccessibility;
    }

    @Override
    public boolean isDelimitedTextRangeBoundary(final int boundary) {
        return false;
    }

}
