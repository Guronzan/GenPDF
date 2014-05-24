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

/* $Id: ListBlock.java 1242848 2012-02-10 16:51:08Z phancock $ */

package org.apache.fop.fo.flow;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.BreakPropertySet;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.fo.properties.KeepProperty;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href=http://www.w3.org/TR/xsl/#fo_list-block">
 * <code>fo:list-block</code></a> object.
 */
public class ListBlock extends FObj implements BreakPropertySet,
        CommonAccessibilityHolder {
    // The value of properties relevant for fo:list-block.
    private CommonAccessibility commonAccessibility;
    private CommonBorderPaddingBackground commonBorderPaddingBackground;
    private CommonMarginBlock commonMarginBlock;
    private int breakAfter;
    private int breakBefore;
    private KeepProperty keepTogether;
    private KeepProperty keepWithNext;
    private KeepProperty keepWithPrevious;
    // Unused but valid items, commented out for performance:
    // private CommonAural commonAural;
    // private CommonRelativePosition commonRelativePosition;
    // private int intrusionDisplace;
    // private Length provisionalDistanceBetweenStarts;
    // private Length provisionalLabelSeparation;
    // End of property values

    /** extension properties */
    private Length widowContentLimit;
    private Length orphanContentLimit;

    // used for child node validation
    private boolean hasListItem = false;

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public ListBlock(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.commonAccessibility = CommonAccessibility.getInstance(pList);
        this.commonBorderPaddingBackground = pList
                .getBorderPaddingBackgroundProps();
        this.commonMarginBlock = pList.getMarginBlockProps();
        this.breakAfter = pList.get(PR_BREAK_AFTER).getEnum();
        this.breakBefore = pList.get(PR_BREAK_BEFORE).getEnum();
        this.keepTogether = pList.get(PR_KEEP_TOGETHER).getKeep();
        this.keepWithNext = pList.get(PR_KEEP_WITH_NEXT).getKeep();
        this.keepWithPrevious = pList.get(PR_KEEP_WITH_PREVIOUS).getKeep();
        // Bind extension properties
        this.widowContentLimit = pList.get(PR_X_WIDOW_CONTENT_LIMIT)
                .getLength();
        this.orphanContentLimit = pList.get(PR_X_ORPHAN_CONTENT_LIMIT)
                .getLength();
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startList(this);
    }

    /**
     * Make sure the content model is satisfied, if so then tell the
     * {@link org.apache.fop.fo.FOEventHandler} that we are at the end of the
     * list-block. {@inheritDoc}
     */
    @Override
    protected void endOfNode() throws FOPException {
        if (!this.hasListItem) {
            missingChildElementError("marker* (list-item)+");
        }
        getFOEventHandler().endList(this);
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: marker* (list-item)+
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (localName.equals("marker")) {
                if (this.hasListItem) {
                    nodesOutOfOrderError(loc, "fo:marker", "fo:list-item");
                }
            } else if (localName.equals("list-item")) {
                this.hasListItem = true;
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CommonAccessibility getCommonAccessibility() {
        return this.commonAccessibility;
    }

    /** @return the {@link CommonMarginBlock} */
    public CommonMarginBlock getCommonMarginBlock() {
        return this.commonMarginBlock;
    }

    /** @return the {@link CommonBorderPaddingBackground} */
    public CommonBorderPaddingBackground getCommonBorderPaddingBackground() {
        return this.commonBorderPaddingBackground;
    }

    /** @return the "break-after" property */
    @Override
    public int getBreakAfter() {
        return this.breakAfter;
    }

    /** @return the "break-before" property */
    @Override
    public int getBreakBefore() {
        return this.breakBefore;
    }

    /** @return the "keep-with-next" property. */
    public KeepProperty getKeepWithNext() {
        return this.keepWithNext;
    }

    /** @return the "keep-with-previous" property. */
    public KeepProperty getKeepWithPrevious() {
        return this.keepWithPrevious;
    }

    /** @return the "keep-together" property. */
    public KeepProperty getKeepTogether() {
        return this.keepTogether;
    }

    /** @return the "fox:widow-content-limit" extension property */
    public Length getWidowContentLimit() {
        return this.widowContentLimit;
    }

    /** @return the "fox:orphan-content-limit" extension property */
    public Length getOrphanContentLimit() {
        return this.orphanContentLimit;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "list-block";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_LIST_BLOCK}
     */
    @Override
    public int getNameId() {
        return FO_LIST_BLOCK;
    }
}
