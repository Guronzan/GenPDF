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

/* $Id: PageNumberLayoutManager.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.layoutmgr.inline;

import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.fo.flow.PageNumber;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.traits.MinOptMax;

/**
 * LayoutManager for the fo:page-number formatting object
 */
public class PageNumberLayoutManager extends LeafNodeLayoutManager {

    private final PageNumber fobj;
    private Font font;

    /**
     * Constructor
     *
     * @param node
     *            the fo:page-number formatting object that creates the area
     *            TODO better null checking of node, font
     */
    public PageNumberLayoutManager(final PageNumber node) {
        super(node);
        this.fobj = node;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        final FontInfo fi = this.fobj.getFOEventHandler().getFontInfo();
        final FontTriplet[] fontkeys = this.fobj.getCommonFont().getFontState(
                fi);
        this.font = fi.getFontInstance(fontkeys[0],
                this.fobj.getCommonFont().fontSize.getValue(this));
        setCommonBorderPaddingBackground(this.fobj
                .getCommonBorderPaddingBackground());
    }

    /**
     * {@inheritDoc} #makeAlignmentContext(LayoutContext)
     */
    @Override
    protected AlignmentContext makeAlignmentContext(final LayoutContext context) {
        return new AlignmentContext(this.font, this.fobj.getLineHeight()
                .getOptimum(this).getLength().getValue(this),
                this.fobj.getAlignmentAdjust(),
                this.fobj.getAlignmentBaseline(), this.fobj.getBaselineShift(),
                this.fobj.getDominantBaseline(), context.getAlignmentContext());
    }

    /** {@inheritDoc} */
    @Override
    public InlineArea get(final LayoutContext context) {
        // get page string from parent, build area
        final TextArea text = new TextArea();
        final String str = getCurrentPV().getPageNumberString();
        final int width = getStringWidth(str);
        text.addWord(str, 0);
        text.setIPD(width);
        text.setBPD(this.font.getAscender() - this.font.getDescender());
        text.setBaselineOffset(this.font.getAscender());
        TraitSetter.addFontTraits(text, this.font);
        text.addTrait(Trait.COLOR, this.fobj.getColor());
        TraitSetter.addStructureTreeElement(text,
                this.fobj.getStructureTreeElement());
        TraitSetter.addTextDecoration(text, this.fobj.getTextDecoration());

        return text;
    }

    /** {@inheritDoc} */
    @Override
    protected InlineArea getEffectiveArea() {
        final TextArea baseArea = (TextArea) this.curArea;
        // TODO Maybe replace that with a clone() call or better, a copy
        // constructor
        // TODO or even better: delay area creation until addAreas() stage
        // TextArea is cloned because the LM is reused in static areas and the
        // area can't be.
        final TextArea ta = new TextArea();
        TraitSetter.setProducerID(ta, this.fobj.getId());
        ta.setIPD(baseArea.getIPD());
        ta.setBPD(baseArea.getBPD());
        ta.setBlockProgressionOffset(baseArea.getBlockProgressionOffset());
        ta.setBaselineOffset(baseArea.getBaselineOffset());
        ta.addTrait(Trait.COLOR, this.fobj.getColor()); // only to initialize
                                                        // the trait map
        ta.getTraits().putAll(baseArea.getTraits());
        updateContent(ta);
        return ta;
    }

    private void updateContent(final TextArea area) {
        // get the page number of the page actually being built
        area.removeText();
        area.addWord(getCurrentPV().getPageNumberString(), 0);
        // update the ipd of the area
        area.handleIPDVariation(getStringWidth(area.getText()) - area.getIPD());
        // update the width stored in the AreaInfo object
        this.areaInfo.ipdArea = MinOptMax.getInstance(area.getIPD());
    }

    /**
     * @param str
     *            string to be measured
     * @return width of the string
     */
    private int getStringWidth(final String str) {
        int width = 0;
        for (int count = 0; count < str.length(); count++) {
            width += this.font.getCharWidth(str.charAt(count));
        }
        return width;
    }

}
