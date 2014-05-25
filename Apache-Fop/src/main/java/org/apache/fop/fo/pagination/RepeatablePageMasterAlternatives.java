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

/* $Id: RepeatablePageMasterAlternatives.java 1296104 2012-03-02 09:50:50Z phancock $ */

package org.apache.fop.fo.pagination;

// Java
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.Property;
import org.xml.sax.Locator;

/**
 * Class modelling the <a
 * href="http://www.w3.org/TR/xsl/#fo_repeatable-page-master-alternatives">
 * <code>fo:repeatable-page-master-alternatives</code></a> object. This contains
 * a list of conditional-page-master-reference and the page master is found from
 * the reference that matches the page number and emptyness.
 */
@Slf4j
public class RepeatablePageMasterAlternatives extends FObj implements
SubSequenceSpecifier {
    // The value of properties relevant for
    // fo:repeatable-page-master-alternatives.
    private Property maximumRepeats;
    // End of property values

    private static final int INFINITE = -1;

    private int numberConsumed = 0;

    private List<ConditionalPageMasterReference> conditionalPageMasterRefs;
    private boolean hasPagePositionLast = false;
    private boolean hasPagePositionOnly = false;

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public RepeatablePageMasterAlternatives(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.maximumRepeats = pList.get(PR_MAXIMUM_REPEATS);
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        this.conditionalPageMasterRefs = new java.util.ArrayList<ConditionalPageMasterReference>();

        assert this.parent.getName().equals("fo:page-sequence-master"); // Validation
        // by
        // the
        // parent
        final PageSequenceMaster pageSequenceMaster = (PageSequenceMaster) this.parent;
        pageSequenceMaster.addSubsequenceSpecifier(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (this.firstChild == null) {
            missingChildElementError("(conditional-page-master-reference+)");
        }
    }

    /**
     * {@inheritDoc} <br>
     * XSL/FOP: (conditional-page-master-reference+)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (!localName.equals("conditional-page-master-reference")) {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /**
     * Get the value of the <code>maximum-repeats</code> property?
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
    public SimplePageMaster getNextPageMaster(final boolean isOddPage,
            final boolean isFirstPage, final boolean isLastPage,
            final boolean isBlankPage) {

        if (!isInfinite() && this.numberConsumed >= getMaximumRepeats()) {
            return null;
        }

        this.numberConsumed++;

        for (final ConditionalPageMasterReference cpmr : this.conditionalPageMasterRefs) {
            if (cpmr.isValid(isOddPage, isFirstPage, isLastPage, isBlankPage)) {
                return cpmr.getMaster();
            }
        }

        return null;
    }

    /**
     * Adds a new conditional page master reference.
     *
     * @param cpmr
     *            the new conditional reference
     */
    public void addConditionalPageMasterReference(
            final ConditionalPageMasterReference cpmr) {
        this.conditionalPageMasterRefs.add(cpmr);
        if (cpmr.getPagePosition() == EN_LAST) {
            this.hasPagePositionLast = true;
        }
        if (cpmr.getPagePosition() == EN_ONLY) {
            this.hasPagePositionOnly = true;
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
        return this.hasPagePositionLast;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasPagePositionOnly() {
        return this.hasPagePositionOnly;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "repeatable-page-master-alternatives";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_REPEATABLE_PAGE_MASTER_ALTERNATIVES}
     */
    @Override
    public int getNameId() {
        return FO_REPEATABLE_PAGE_MASTER_ALTERNATIVES;
    }

    /** {@inheritDoc} */
    @Override
    public void resolveReferences(final LayoutMasterSet layoutMasterSet)
            throws ValidationException {
        for (final ConditionalPageMasterReference conditionalPageMasterReference : this.conditionalPageMasterRefs) {
            conditionalPageMasterReference.resolveReferences(layoutMasterSet);
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean canProcess(final String flowName) {

        boolean willTerminate = true;

        // Look for rest spm that cannot terminate
        final ArrayList<ConditionalPageMasterReference> rest = new ArrayList<ConditionalPageMasterReference>();
        for (final ConditionalPageMasterReference cpmr : this.conditionalPageMasterRefs) {
            if (cpmr.isValid(true, false, false, false)
                    || cpmr.isValid(false, false, false, false)) {
                rest.add(cpmr);
            }
        }
        if (!rest.isEmpty()) {
            willTerminate = false;
            for (final ConditionalPageMasterReference cpmr : rest) {
                willTerminate |= cpmr.getMaster().getRegion(FO_REGION_BODY)
                        .getRegionName().equals(flowName);
            }
        }

        return willTerminate;
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
