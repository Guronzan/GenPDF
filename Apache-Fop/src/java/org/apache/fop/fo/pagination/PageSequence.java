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

/* $Id: PageSequence.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.fo.pagination;

import java.util.Map;
import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.complexscripts.bidi.DelimitedTextRange;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.traits.Direction;
import org.apache.fop.traits.WritingMode;
import org.apache.fop.traits.WritingModeTraits;
import org.apache.fop.traits.WritingModeTraitsGetter;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_page-sequence">
 * <code>fo:page-sequence</code></a> object.
 */
@Slf4j
public class PageSequence extends AbstractPageSequence implements
WritingModeTraitsGetter {

    // The value of FO traits (refined properties) that apply to
    // fo:page-sequence.
    private String country;
    private String language;
    private String masterReference;
    private Numeric referenceOrientation;
    private WritingModeTraits writingModeTraits;
    // End of trait values

    // There doesn't seem to be anything in the spec requiring flows
    // to be in the order given, only that they map to the regions
    // defined in the page sequence, so all we need is this one hashmap
    // the set of flows includes StaticContent flows also

    /** Map of flows to their flow name (flow-name, Flow) */
    private Map<String, FONode> flowMap;

    /**
     * The currentSimplePageMaster is either the page master for the whole page
     * sequence if master-reference refers to a simple-page-master, or the
     * simple page master produced by the page sequence master otherwise. The
     * pageSequenceMaster is null if master-reference refers to a
     * simple-page-master.
     */
    private SimplePageMaster simplePageMaster;
    private PageSequenceMaster pageSequenceMaster;

    /**
     * The fo:title object for this page-sequence.
     */
    private Title titleFO;

    /**
     * The fo:flow object for this page-sequence.
     */
    private Flow mainFlow = null;

    /**
     * Create a PageSequence instance that is a child of the given
     * {@link FONode}.
     *
     * @param parent
     *            the parent {@link FONode}
     */
    public PageSequence(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        super.bind(pList);
        this.country = pList.get(PR_COUNTRY).getString();
        this.language = pList.get(PR_LANGUAGE).getString();
        this.masterReference = pList.get(PR_MASTER_REFERENCE).getString();
        this.referenceOrientation = pList.get(PR_REFERENCE_ORIENTATION)
                .getNumeric();
        this.writingModeTraits = new WritingModeTraits(
                WritingMode.valueOf(pList.get(PR_WRITING_MODE).getEnum()));
        if (this.masterReference == null || this.masterReference.equals("")) {
            missingPropertyError("master-reference");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        this.flowMap = new java.util.HashMap<String, FONode>();

        this.simplePageMaster = getRoot().getLayoutMasterSet()
                .getSimplePageMaster(this.masterReference);
        if (this.simplePageMaster == null) {
            this.pageSequenceMaster = getRoot().getLayoutMasterSet()
                    .getPageSequenceMaster(this.masterReference);
            if (this.pageSequenceMaster == null) {
                getFOValidationEventProducer().masterNotFound(this, getName(),
                        this.masterReference, getLocator());
            }
        }
        getFOEventHandler().startPageSequence(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (this.mainFlow == null) {
            missingChildElementError("(title?,static-content*,flow)");
        }

        getFOEventHandler().endPageSequence(this);
    }

    /**
     * {@inheritDoc} XSL Content Model: (title?,static-content*,flow)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if ("title".equals(localName)) {
                if (this.titleFO != null) {
                    tooManyNodesError(loc, "fo:title");
                } else if (!this.flowMap.isEmpty()) {
                    nodesOutOfOrderError(loc, "fo:title", "fo:static-content");
                } else if (this.mainFlow != null) {
                    nodesOutOfOrderError(loc, "fo:title", "fo:flow");
                }
            } else if ("static-content".equals(localName)) {
                if (this.mainFlow != null) {
                    nodesOutOfOrderError(loc, "fo:static-content", "fo:flow");
                }
            } else if ("flow".equals(localName)) {
                if (this.mainFlow != null) {
                    tooManyNodesError(loc, "fo:flow");
                }
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /**
     * {@inheritDoc} TODO see if addChildNode() should also be called for fo's
     * other than fo:flow.
     */
    @Override
    public void addChildNode(final FONode child) throws FOPException {
        final int childId = child.getNameId();

        switch (childId) {
        case FO_TITLE:
            this.titleFO = (Title) child;
            break;
        case FO_FLOW:
            this.mainFlow = (Flow) child;
            addFlow(this.mainFlow);
            break;
        case FO_STATIC_CONTENT:
            addFlow((StaticContent) child);
            this.flowMap.put(((Flow) child).getFlowName(), child);
            break;
        default:
            super.addChildNode(child);
        }
    }

    /**
     * Add a flow or static content, mapped by its flow-name. The flow-name is
     * used to associate the flow with a region on a page, based on the
     * region-names given to the regions in the page-master used to generate
     * that page.
     *
     * @param flow
     *            the {@link Flow} instance to be added
     * @throws org.apache.fop.fo.ValidationException
     *             if the fo:flow maps to an invalid page-region
     */
    private void addFlow(final Flow flow) throws ValidationException {
        final String flowName = flow.getFlowName();

        if (hasFlowName(flowName)) {
            getFOValidationEventProducer().duplicateFlowNameInPageSequence(
                    this, flow.getName(), flowName, flow.getLocator());
        }

        if (!getRoot().getLayoutMasterSet().regionNameExists(flowName)
                && !flowName.equals("xsl-before-float-separator")
                && !flowName.equals("xsl-footnote-separator")) {
            getFOValidationEventProducer().flowNameNotMapped(this,
                    flow.getName(), flowName, flow.getLocator());
        }
    }

    /**
     * Get the static content FO node from the flow map. This gets the static
     * content flow for the given flow name.
     *
     * @param name
     *            the flow name to find
     * @return the static content FO node
     */
    public StaticContent getStaticContent(final String name) {
        return (StaticContent) this.flowMap.get(name);
    }

    /**
     * Accessor method for the fo:title associated with this fo:page-sequence
     *
     * @return titleFO for this object
     */
    public Title getTitleFO() {
        return this.titleFO;
    }

    /**
     * Public accessor for getting the MainFlow to which this PageSequence is
     * attached.
     *
     * @return the MainFlow object to which this PageSequence is attached.
     */
    public Flow getMainFlow() {
        return this.mainFlow;
    }

    /**
     * Determine if this PageSequence already has a flow with the given
     * flow-name Used for validation of incoming fo:flow or fo:static-content
     * objects
     *
     * @param flowName
     *            The flow-name to search for
     * @return true if flow-name already defined within this page sequence,
     *         false otherwise
     */
    public boolean hasFlowName(final String flowName) {
        return this.flowMap.containsKey(flowName);
    }

    /** @return the flow map for this page-sequence */
    public Map<String, FONode> getFlowMap() {
        return this.flowMap;
    }

    /**
     * Public accessor for determining the next page master to use within this
     * page sequence.
     *
     * @param page
     *            the page number of the page to be created
     * @param isFirstPage
     *            indicator whether this page is the first page of the page
     *            sequence
     * @param isLastPage
     *            indicator whether this page is the last page of the page
     *            sequence
     * @param isBlank
     *            indicator whether the page will be blank
     * @return the SimplePageMaster to use for this page
     * @throws PageProductionException
     *             if there's a problem determining the page master
     */
    public SimplePageMaster getNextSimplePageMaster(final int page,
            final boolean isFirstPage, final boolean isLastPage,
            final boolean isBlank) throws PageProductionException {

        if (this.pageSequenceMaster == null) {
            return this.simplePageMaster;
        }
        final boolean isOddPage = page % 2 == 1;
        if (log.isDebugEnabled()) {
            log.debug("getNextSimplePageMaster(page=" + page + " isOdd="
                    + isOddPage + " isFirst=" + isFirstPage + " isLast="
                    + isLastPage + " isBlank=" + isBlank + ")");
        }
        return this.pageSequenceMaster.getNextSimplePageMaster(isOddPage,
                isFirstPage, isLastPage, isBlank, getMainFlow().getFlowName());
    }

    /**
     * Used to set the "cursor position" for the page masters to the previous
     * item.
     *
     * @return true if there is a previous item, false if the current one was
     *         the first one.
     */
    public boolean goToPreviousSimplePageMaster() {
        return this.pageSequenceMaster == null
                || this.pageSequenceMaster.goToPreviousSimplePageMaster();
    }

    /**
     * @return true if the page-sequence has a page-master with
     *         page-position="last"
     */
    public boolean hasPagePositionLast() {
        return this.pageSequenceMaster != null
                && this.pageSequenceMaster.hasPagePositionLast();
    }

    /**
     * @return true if the page-sequence has a page-master with
     *         page-position="only"
     */
    public boolean hasPagePositionOnly() {
        return this.pageSequenceMaster != null
                && this.pageSequenceMaster.hasPagePositionOnly();
    }

    /**
     * Get the value of the <code>master-reference</code> trait.
     *
     * @return the "master-reference" trait
     */
    public String getMasterReference() {
        return this.masterReference;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "page-sequence";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_PAGE_SEQUENCE}
     */
    @Override
    public int getNameId() {
        return FO_PAGE_SEQUENCE;
    }

    /**
     * Get the value of the <code>country</code> trait.
     *
     * @return the country trait value
     */
    public String getCountry() {
        return this.country;
    }

    /**
     * Get the value of the <code>language</code> trait.
     *
     * @return the language trait value
     */
    public String getLanguage() {
        return this.language;
    }

    /**
     * Get the value of the <code>reference-orientation</code> trait.
     *
     * @return the reference orientation trait value
     */
    @Override
    public int getReferenceOrientation() {
        if (this.referenceOrientation != null) {
            return this.referenceOrientation.getValue();
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Direction getInlineProgressionDirection() {
        if (this.writingModeTraits != null) {
            return this.writingModeTraits.getInlineProgressionDirection();
        } else {
            return Direction.LR;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Direction getBlockProgressionDirection() {
        if (this.writingModeTraits != null) {
            return this.writingModeTraits.getBlockProgressionDirection();
        } else {
            return Direction.TB;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Direction getColumnProgressionDirection() {
        if (this.writingModeTraits != null) {
            return this.writingModeTraits.getColumnProgressionDirection();
        } else {
            return Direction.LR;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Direction getRowProgressionDirection() {
        if (this.writingModeTraits != null) {
            return this.writingModeTraits.getRowProgressionDirection();
        } else {
            return Direction.TB;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Direction getShiftDirection() {
        if (this.writingModeTraits != null) {
            return this.writingModeTraits.getShiftDirection();
        } else {
            return Direction.TB;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WritingMode getWritingMode() {
        if (this.writingModeTraits != null) {
            return this.writingModeTraits.getWritingMode();
        } else {
            return WritingMode.LR_TB;
        }
    }

    @Override
    protected Stack collectDelimitedTextRanges(Stack ranges,
            final DelimitedTextRange currentRange) {
        // collect ranges from static content flows
        final Map<String, FONode> flows = getFlowMap();
        if (flows != null) {
            for (final FONode fn : flows.values()) {
                if (fn instanceof StaticContent) {
                    ranges = ((StaticContent) fn)
                            .collectDelimitedTextRanges(ranges);
                }
            }
        }
        // collect ranges in main flow
        final Flow main = getMainFlow();
        if (main != null) {
            ranges = main.collectDelimitedTextRanges(ranges);
        }
        return ranges;
    }

    /**
     * Releases a page-sequence's children after the page-sequence has been
     * fully processed.
     */
    public void releasePageSequence() {
        this.mainFlow = null;
        this.flowMap.clear();
    }

}
