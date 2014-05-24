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

/* $Id: ConditionalPageMasterReference.java 1229622 2012-01-10 16:14:05Z cbowditch $ */

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
 * href="http://www.w3.org/TR/xsl/#fo_conditional-page-master-reference">
 * <code>fo:conditional-page-master-reference</code></a> object.
 *
 * This is a reference to a page master with a set of conditions. The conditions
 * must be satisfied for the referenced master to be used. This element is must
 * be the child of a repeatable-page-master-alternatives element.
 */
public class ConditionalPageMasterReference extends FObj {
    // The value of properties relevant for
    // fo:conditional-page-master-reference.
    private String masterReference;
    // The simple page master referenced
    private SimplePageMaster master;
    private int pagePosition;
    private int oddOrEven;
    private int blankOrNotBlank;

    // End of property values

    /**
     * Create a ConditionalPageMasterReference instance that is a child of the
     * given {@link FONode}.
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public ConditionalPageMasterReference(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.masterReference = pList.get(PR_MASTER_REFERENCE).getString();
        this.pagePosition = pList.get(PR_PAGE_POSITION).getEnum();
        this.oddOrEven = pList.get(PR_ODD_OR_EVEN).getEnum();
        this.blankOrNotBlank = pList.get(PR_BLANK_OR_NOT_BLANK).getEnum();

        if (this.masterReference == null || this.masterReference.equals("")) {
            missingPropertyError("master-reference");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        getConcreteParent().addConditionalPageMasterReference(this);
    }

    private RepeatablePageMasterAlternatives getConcreteParent() {
        return (RepeatablePageMasterAlternatives) this.parent;
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

    /**
     * Check if the conditions for this reference are met. checks the page
     * number and emptyness to determine if this matches.
     * 
     * @param isOddPage
     *            True if page number odd
     * @param isFirstPage
     *            True if page is first page
     * @param isLastPage
     *            True if page is last page
     * @param isBlankPage
     *            True if page is blank
     * @return True if the conditions for this reference are met
     */
    protected boolean isValid(final boolean isOddPage,
            final boolean isFirstPage, final boolean isLastPage,
            final boolean isBlankPage) {

        return (this.pagePosition == EN_ANY || this.pagePosition == EN_FIRST
                && isFirstPage || this.pagePosition == EN_LAST && isLastPage
                || this.pagePosition == EN_ONLY && isFirstPage && isLastPage || this.pagePosition == EN_REST
                && !(isFirstPage || isLastPage))
                // odd-or-even
                && (this.oddOrEven == EN_ANY || this.oddOrEven == EN_ODD
                        && isOddPage || this.oddOrEven == EN_EVEN && !isOddPage)
                // blank-or-not-blank
                && (this.blankOrNotBlank == EN_ANY
                        || this.blankOrNotBlank == EN_BLANK && isBlankPage || this.blankOrNotBlank == EN_NOT_BLANK
                        && !isBlankPage);

    }

    /**
     * Get the value for the <code>master-reference</code> property.
     * 
     * @return the "master-reference" property
     */
    public SimplePageMaster getMaster() {
        return this.master;
    }

    /**
     * Get the value for the <code>page-position</code> property.
     * 
     * @return the page-position property value
     */
    public int getPagePosition() {
        return this.pagePosition;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "conditional-page-master-reference";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_CONDITIONAL_PAGE_MASTER_REFERENCE}
     */
    @Override
    public int getNameId() {
        return FO_CONDITIONAL_PAGE_MASTER_REFERENCE;
    }

    /**
     * called by the parent RepeatablePageMasterAlternatives to resolve object
     * references from simple page master reference names
     * 
     * @param layoutMasterSet
     *            the layout-master-set
     * @throws ValidationException
     *             when a named reference cannot be resolved
     * */
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
}
