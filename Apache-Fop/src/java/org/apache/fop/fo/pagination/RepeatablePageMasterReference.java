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

/* $Id: RepeatablePageMasterReference.java 1296104 2012-03-02 09:50:50Z phancock $ */

package org.apache.fop.fo.pagination;

// XML
import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.layoutmgr.BlockLevelEventProducer;
import org.xml.sax.Locator;

/**
 * Class modelling the <a
 * href="http://www.w3.org/TR/xsl/#fo_repeatable-page-master-reference">
 * <code>fo:repeatable-page-master-reference</code></a> object. This handles a
 * reference with a specified number of repeating instances of the referenced
 * page master (may have no limit).
 */
@Slf4j
public class RepeatablePageMasterReference extends FObj implements
SubSequenceSpecifier {

    // The value of properties relevant for fo:repeatable-page-master-reference.
    private String masterReference;
    // The simple page master referenced
    private SimplePageMaster master;
    private Property maximumRepeats;
    // End of property values

    private static final int INFINITE = -1;

    private int numberConsumed = 0;

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public RepeatablePageMasterReference(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.masterReference = pList.get(PR_MASTER_REFERENCE).getString();
        this.maximumRepeats = pList.get(PR_MAXIMUM_REPEATS);

        if (this.masterReference == null || this.masterReference.equals("")) {
            missingPropertyError("master-reference");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        final PageSequenceMaster pageSequenceMaster = (PageSequenceMaster) this.parent;

        if (this.masterReference == null) {
            missingPropertyError("master-reference");
        } else {
            pageSequenceMaster.addSubsequenceSpecifier(this);
        }
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: empty
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        invalidChildError(loc, nsURI, localName);
    }

    /** {@inheritDoc} */
    @Override
    public SimplePageMaster getNextPageMaster(final boolean isOddPage,
            final boolean isFirstPage, final boolean isLastPage,
            final boolean isEmptyPage) {
        if (getMaximumRepeats() != INFINITE
                && this.numberConsumed >= getMaximumRepeats()) {
            return null;
        }
        this.numberConsumed++;
        return this.master;
    }

    /**
     * Get the value of the <code>maximum-repeats</code> property.
     *
     * @return the "maximum-repeats" property
     */
    public int getMaximumRepeats() {
        if (this.maximumRepeats.getEnum() == EN_NO_LIMIT) {
            return INFINITE;
        } else {
            int mr = this.maximumRepeats.getNumeric().getValue();
            if (mr < 0) {
                log.debug("negative maximum-repeats: " + this.maximumRepeats);
                mr = 0;
            }
            return mr;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        this.numberConsumed = 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean goToPrevious() {
        if (this.numberConsumed == 0) {
            return false;
        } else {
            this.numberConsumed--;
            return true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasPagePositionLast() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasPagePositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "repeatable-page-master-reference";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_REPEATABLE_PAGE_MASTER_REFERENCE}
     */
    @Override
    public int getNameId() {
        return FO_REPEATABLE_PAGE_MASTER_REFERENCE;
    }

    /** {@inheritDoc} */
    @Override
    public void resolveReferences(final LayoutMasterSet layoutMasterSet)
            throws ValidationException {
        this.master = layoutMasterSet.getSimplePageMaster(this.masterReference);
        if (this.master == null) {
            BlockLevelEventProducer.Provider.get(
                    getUserAgent().getEventBroadcaster()).noMatchingPageMaster(
                            this, this.parent.getName(), this.masterReference,
                            getLocator());
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean canProcess(final String flowName) {
        assert this.master != null;
        return this.master.getRegion(FO_REGION_BODY).getRegionName()
                .equals(flowName);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInfinite() {
        return getMaximumRepeats() == INFINITE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReusable() {
        return false;
    }

}
