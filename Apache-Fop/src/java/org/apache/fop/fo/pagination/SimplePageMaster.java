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

/* $Id: SimplePageMaster.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.fo.pagination;

// Java
import java.util.HashMap;
import java.util.Map;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.datatypes.SimplePercentBaseContext;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.CommonMarginBlock;
import org.apache.fop.traits.WritingMode;
import org.xml.sax.Locator;

/**
 * Class modelling the <a
 * href="http://www.w3.org/TR/xsl/#fo_simple-page-master">
 * <code>fo:simple-page-master</code></a> object. This creates a simple page
 * from the specified regions and attributes.
 */
public class SimplePageMaster extends FObj {
    // The value of FO traits (refined properties) that apply to
    // fo:simple-page-master.
    private CommonMarginBlock commonMarginBlock;
    private String masterName;
    private Length pageHeight;
    private Length pageWidth;
    private Numeric referenceOrientation;
    private WritingMode writingMode;
    // End of FO trait values

    /**
     * Page regions (regionClass, Region)
     */
    private Map<String, Region> regions;

    // used for node validation
    private boolean hasRegionBody = false;
    private boolean hasRegionBefore = false;
    private boolean hasRegionAfter = false;
    private boolean hasRegionStart = false;
    private boolean hasRegionEnd = false;

    /**
     * Base constructor
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public SimplePageMaster(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        this.commonMarginBlock = pList.getMarginBlockProps();
        this.masterName = pList.get(PR_MASTER_NAME).getString();
        this.pageHeight = pList.get(PR_PAGE_HEIGHT).getLength();
        this.pageWidth = pList.get(PR_PAGE_WIDTH).getLength();
        this.referenceOrientation = pList.get(PR_REFERENCE_ORIENTATION)
                .getNumeric();
        this.writingMode = WritingMode.valueOf(pList.get(PR_WRITING_MODE)
                .getEnum());

        if (this.masterName == null || this.masterName.equals("")) {
            missingPropertyError("master-name");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        final LayoutMasterSet layoutMasterSet = (LayoutMasterSet) this.parent;

        if (this.masterName == null) {
            missingPropertyError("master-name");
        } else {
            layoutMasterSet.addSimplePageMaster(this);
        }

        // Well, there are only 5 regions so we can save a bit of memory here
        this.regions = new HashMap<String, Region>(5);
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (!this.hasRegionBody) {
            missingChildElementError("(region-body, region-before?, region-after?, region-start?, region-end?)");
        }
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model:
     * (region-body,region-before?,region-after?,region-start?,region-end?)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (localName.equals("region-body")) {
                if (this.hasRegionBody) {
                    tooManyNodesError(loc, "fo:region-body");
                } else {
                    this.hasRegionBody = true;
                }
            } else if (localName.equals("region-before")) {
                if (!this.hasRegionBody) {
                    nodesOutOfOrderError(loc, "fo:region-body",
                            "fo:region-before");
                } else if (this.hasRegionBefore) {
                    tooManyNodesError(loc, "fo:region-before");
                } else if (this.hasRegionAfter) {
                    nodesOutOfOrderError(loc, "fo:region-before",
                            "fo:region-after");
                } else if (this.hasRegionStart) {
                    nodesOutOfOrderError(loc, "fo:region-before",
                            "fo:region-start");
                } else if (this.hasRegionEnd) {
                    nodesOutOfOrderError(loc, "fo:region-before",
                            "fo:region-end");
                } else {
                    this.hasRegionBefore = true;
                }
            } else if (localName.equals("region-after")) {
                if (!this.hasRegionBody) {
                    nodesOutOfOrderError(loc, "fo:region-body",
                            "fo:region-after");
                } else if (this.hasRegionAfter) {
                    tooManyNodesError(loc, "fo:region-after");
                } else if (this.hasRegionStart) {
                    nodesOutOfOrderError(loc, "fo:region-after",
                            "fo:region-start");
                } else if (this.hasRegionEnd) {
                    nodesOutOfOrderError(loc, "fo:region-after",
                            "fo:region-end");
                } else {
                    this.hasRegionAfter = true;
                }
            } else if (localName.equals("region-start")) {
                if (!this.hasRegionBody) {
                    nodesOutOfOrderError(loc, "fo:region-body",
                            "fo:region-start");
                } else if (this.hasRegionStart) {
                    tooManyNodesError(loc, "fo:region-start");
                } else if (this.hasRegionEnd) {
                    nodesOutOfOrderError(loc, "fo:region-start",
                            "fo:region-end");
                } else {
                    this.hasRegionStart = true;
                }
            } else if (localName.equals("region-end")) {
                if (!this.hasRegionBody) {
                    nodesOutOfOrderError(loc, "fo:region-body", "fo:region-end");
                } else if (this.hasRegionEnd) {
                    tooManyNodesError(loc, "fo:region-end");
                } else {
                    this.hasRegionEnd = true;
                }
            } else {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean generatesReferenceAreas() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void addChildNode(final FONode child) throws FOPException {
        if (child instanceof Region) {
            addRegion((Region) child);
        } else {
            super.addChildNode(child);
        }
    }

    /**
     * Adds a region to this simple-page-master.
     * 
     * @param region
     *            region to add
     */
    protected void addRegion(final Region region) {
        this.regions.put(String.valueOf(region.getNameId()), region);
    }

    /**
     * Gets the context for the width of the page-reference-area, taking into
     * account the reference-orientation.
     *
     * @param lengthBase
     *            the lengthBase to use to resolve percentages
     * @return context for the width of the page-reference-area
     */
    protected final PercentBaseContext getPageWidthContext(final int lengthBase) {
        return this.referenceOrientation.getValue() % 180 == 0 ? new SimplePercentBaseContext(
                null, lengthBase, getPageWidth().getValue())
        : new SimplePercentBaseContext(null, lengthBase,
                getPageHeight().getValue());
    }

    /**
     * Gets the context for the height of the page-reference-area, taking into
     * account the reference-orientation.
     *
     * @param lengthBase
     *            the lengthBase to use to resolve percentages
     * @return the context for the height of the page-reference-area
     */
    protected final PercentBaseContext getPageHeightContext(final int lengthBase) {
        return this.referenceOrientation.getValue() % 180 == 0 ? new SimplePercentBaseContext(
                null, lengthBase, getPageHeight().getValue())
        : new SimplePercentBaseContext(null, lengthBase, getPageWidth()
                        .getValue());
    }

    /**
     * Returns the region for a given region class.
     * 
     * @param regionId
     *            Constants ID of the FO representing the region
     * @return the region, null if it doesn't exist
     */
    public Region getRegion(final int regionId) {
        return this.regions.get(String.valueOf(regionId));
    }

    /**
     * Returns a Map of regions associated with this simple-page-master
     * 
     * @return the regions
     */
    public Map<String, Region> getRegions() {
        return this.regions;
    }

    /**
     * Indicates if a region with a given name exists in this
     * simple-page-master.
     * 
     * @param regionName
     *            name of the region to lookup
     * @return True if a region with this name exists
     */
    protected boolean regionNameExists(final String regionName) {
        for (final Region r : this.regions.values()) {
            if (r.getRegionName().equals(regionName)) {
                return true;
            }
        }
        return false;
    }

    /** @return the Common Margin Properties-Block. */
    public CommonMarginBlock getCommonMarginBlock() {
        return this.commonMarginBlock;
    }

    /** @return "master-name" FO trait. */
    public String getMasterName() {
        return this.masterName;
    }

    /** @return the "page-width" FO trait. */
    public Length getPageWidth() {
        return this.pageWidth;
    }

    /** @return the "page-height" FO trait. */
    public Length getPageHeight() {
        return this.pageHeight;
    }

    /** @return the "reference-orientation" FO trait. */
    public int getReferenceOrientation() {
        return this.referenceOrientation.getValue();
    }

    /** @return the "writing-mode" FO trait. */
    public WritingMode getWritingMode() {
        return this.writingMode;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "simple-page-master";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_SIMPLE_PAGE_MASTER}
     */
    @Override
    public int getNameId() {
        return FO_SIMPLE_PAGE_MASTER;
    }
}
