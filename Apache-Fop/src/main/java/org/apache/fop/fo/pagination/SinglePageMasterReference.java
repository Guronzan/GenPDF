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

/* $Id: SinglePageMasterReference.java 1296104 2012-03-02 09:50:50Z phancock $ */

package org.apache.fop.fo.pagination;

// XML
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.layoutmgr.BlockLevelEventProducer;
import org.xml.sax.Locator;

/**
 * Class modelling the <a
 * href="http://www.w3.org/TR/xsl/#fo_single-page-master-reference">
 * <code>fo:single-page-master-reference</code></a> object. This is a reference
 * for a single page. It returns the master name only once until reset.
 */
public class SinglePageMasterReference extends FObj implements
        SubSequenceSpecifier {

    // The value of properties relevant for fo:single-page-master-reference.
    private String masterReference;

    // The simple page master referenced
    private SimplePageMaster master;

    // End of property values

    private static final int FIRST = 0;
    private static final int DONE = 1;

    private int state;

    /**
     * Creates a new SinglePageMasterReference instance that is a child of the
     * given {@link FONode}.
     * 
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public SinglePageMasterReference(final FONode parent) {
        super(parent);
        this.state = FIRST;
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.masterReference = pList.get(PR_MASTER_REFERENCE).getString();

        if (this.masterReference == null || this.masterReference.equals("")) {
            missingPropertyError("master-reference");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        final PageSequenceMaster pageSequenceMaster = (PageSequenceMaster) this.parent;
        pageSequenceMaster.addSubsequenceSpecifier(this);
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: empty
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public SimplePageMaster getNextPageMaster(final boolean isOddPage,
            final boolean isFirstPage, final boolean isLastPage,
            final boolean isBlankPage) {
        if (this.state == FIRST) {
            this.state = DONE;
            return this.master;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        this.state = FIRST;
    }

    /** {@inheritDoc} */
    @Override
    public boolean goToPrevious() {
        if (this.state == FIRST) {
            return false;
        } else {
            this.state = FIRST;
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
        return "single-page-master-reference";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_SINGLE_PAGE_MASTER_REFERENCE}
     */
    @Override
    public int getNameId() {
        return FO_SINGLE_PAGE_MASTER_REFERENCE;
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
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReusable() {
        return true;
    }

}
