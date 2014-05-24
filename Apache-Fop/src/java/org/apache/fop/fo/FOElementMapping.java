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

/* $Id: FOElementMapping.java 1242848 2012-02-10 16:51:08Z phancock $ */

package org.apache.fop.fo;

// Java
import java.util.HashMap;

import org.apache.fop.layoutmgr.BlockLevelEventProducer;
import org.apache.xmlgraphics.util.QName;

/**
 * Element mapping class for all XSL-FO elements.
 */
public class FOElementMapping extends ElementMapping {

    /** The XSL-FO namespace URI */
    public static final String URI = "http://www.w3.org/1999/XSL/Format";

    /** Standard prefix */
    public static final String STANDARD_PREFIX = "fo";

    /**
     * Basic constructor; inititializes the namespace URI for the fo: namespace
     */
    public FOElementMapping() {
        this.namespaceURI = URI;
    }

    /**
     * Initializes the collection of valid objects for the fo: namespace
     */
    @Override
    protected void initialize() {
        if (this.foObjs == null) {
            this.foObjs = new HashMap<String, Maker>();

            // Declarations and Pagination and Layout Formatting Objects
            this.foObjs.put("root", new RootMaker());
            this.foObjs.put("declarations", new DeclarationsMaker());
            this.foObjs.put("color-profile", new ColorProfileMaker());
            this.foObjs.put("bookmark-tree", new BookmarkTreeMaker());
            this.foObjs.put("bookmark", new BookmarkMaker());
            this.foObjs.put("bookmark-title", new BookmarkTitleMaker());
            // foObjs.put("page-sequence-wrapper", new
            // PageSequenceWrapperMaker());
            this.foObjs.put("page-sequence", new PageSequenceMaker());
            this.foObjs.put("layout-master-set", new LayoutMasterSetMaker());
            this.foObjs.put("page-sequence-master",
                    new PageSequenceMasterMaker());
            this.foObjs.put("single-page-master-reference",
                    new SinglePageMasterReferenceMaker());
            this.foObjs.put("repeatable-page-master-reference",
                    new RepeatablePageMasterReferenceMaker());
            this.foObjs.put("repeatable-page-master-alternatives",
                    new RepeatablePageMasterAlternativesMaker());
            this.foObjs.put("conditional-page-master-reference",
                    new ConditionalPageMasterReferenceMaker());
            this.foObjs.put("simple-page-master", new SimplePageMasterMaker());
            this.foObjs.put("region-body", new RegionBodyMaker());
            this.foObjs.put("region-before", new RegionBeforeMaker());
            this.foObjs.put("region-after", new RegionAfterMaker());
            this.foObjs.put("region-start", new RegionStartMaker());
            this.foObjs.put("region-end", new RegionEndMaker());
            this.foObjs.put("flow", new FlowMaker());
            this.foObjs.put("static-content", new StaticContentMaker());
            this.foObjs.put("title", new TitleMaker());

            // Block-level Formatting Objects
            this.foObjs.put("block", new BlockMaker());
            this.foObjs.put("block-container", new BlockContainerMaker());

            // Inline-level Formatting Objects
            this.foObjs.put("bidi-override", new BidiOverrideMaker());
            this.foObjs.put("character", new CharacterMaker());
            this.foObjs.put("initial-property-set",
                    new InitialPropertySetMaker());
            this.foObjs.put("external-graphic", new ExternalGraphicMaker());
            this.foObjs.put("instream-foreign-object",
                    new InstreamForeignObjectMaker());
            this.foObjs.put("inline", new InlineMaker());
            this.foObjs.put("inline-container", new InlineContainerMaker());
            this.foObjs.put("leader", new LeaderMaker());
            this.foObjs.put("page-number", new PageNumberMaker());
            this.foObjs.put("page-number-citation",
                    new PageNumberCitationMaker());
            this.foObjs.put("page-number-citation-last",
                    new PageNumberCitationLastMaker());

            // Formatting Objects for Tables
            this.foObjs.put("table-and-caption", new TableAndCaptionMaker());
            this.foObjs.put("table", new TableMaker());
            this.foObjs.put("table-column", new TableColumnMaker());
            this.foObjs.put("table-caption", new TableCaptionMaker());
            this.foObjs.put("table-header", new TableHeaderMaker());
            this.foObjs.put("table-footer", new TableFooterMaker());
            this.foObjs.put("table-body", new TableBodyMaker());
            this.foObjs.put("table-row", new TableRowMaker());
            this.foObjs.put("table-cell", new TableCellMaker());

            // Formatting Objects for Lists
            this.foObjs.put("list-block", new ListBlockMaker());
            this.foObjs.put("list-item", new ListItemMaker());
            this.foObjs.put("list-item-body", new ListItemBodyMaker());
            this.foObjs.put("list-item-label", new ListItemLabelMaker());

            // Dynamic Effects: Link and Multi Formatting Objects
            this.foObjs.put("basic-link", new BasicLinkMaker());
            this.foObjs.put("multi-switch", new MultiSwitchMaker());
            this.foObjs.put("multi-case", new MultiCaseMaker());
            this.foObjs.put("multi-toggle", new MultiToggleMaker());
            this.foObjs.put("multi-properties", new MultiPropertiesMaker());
            this.foObjs.put("multi-property-set", new MultiPropertySetMaker());

            // Out-of-Line Formatting Objects
            this.foObjs.put("float", new FloatMaker());
            this.foObjs.put("footnote", new FootnoteMaker());
            this.foObjs.put("footnote-body", new FootnoteBodyMaker());

            // Other Formatting Objects
            this.foObjs.put("wrapper", new WrapperMaker());
            this.foObjs.put("marker", new MarkerMaker());
            this.foObjs.put("retrieve-marker", new RetrieveMarkerMaker());
            this.foObjs.put("retrieve-table-marker",
                    new RetrieveTableMarkerMaker());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getStandardPrefix() {
        return STANDARD_PREFIX;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAttributeProperty(final QName attributeName) {
        return true; // All XSL-FO attributes are to be converted to properties.
    }

    static class RootMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.Root(parent);
        }
    }

    static class DeclarationsMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.Declarations(parent);
        }
    }

    static class ColorProfileMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.ColorProfile(parent);
        }
    }

    static class BookmarkTreeMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.bookmarks.BookmarkTree(
                    parent);
        }
    }

    static class BookmarkMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.bookmarks.Bookmark(parent);
        }
    }

    static class BookmarkTitleMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.bookmarks.BookmarkTitle(
                    parent);
        }
    }

    static class PageSequenceWrapperMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.PageSequenceWrapper(parent);
        }
    }

    static class PageSequenceMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.PageSequence(parent);
        }
    }

    static class LayoutMasterSetMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.LayoutMasterSet(parent);
        }
    }

    static class PageSequenceMasterMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.PageSequenceMaster(parent,
                    BlockLevelEventProducer.Provider.get(parent.getUserAgent()
                            .getEventBroadcaster()));
        }
    }

    static class SinglePageMasterReferenceMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.SinglePageMasterReference(
                    parent);
        }
    }

    static class RepeatablePageMasterReferenceMaker extends
            ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.RepeatablePageMasterReference(
                    parent);
        }
    }

    static class RepeatablePageMasterAlternativesMaker extends
            ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.RepeatablePageMasterAlternatives(
                    parent);
        }
    }

    static class ConditionalPageMasterReferenceMaker extends
            ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.ConditionalPageMasterReference(
                    parent);
        }
    }

    static class SimplePageMasterMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.SimplePageMaster(parent);
        }
    }

    static class RegionBodyMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.RegionBody(parent);
        }
    }

    static class RegionBeforeMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.RegionBefore(parent);
        }
    }

    static class RegionAfterMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.RegionAfter(parent);
        }
    }

    static class RegionStartMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.RegionStart(parent);
        }
    }

    static class RegionEndMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.RegionEnd(parent);
        }
    }

    static class FlowMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.Flow(parent);
        }
    }

    static class StaticContentMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.StaticContent(parent);
        }
    }

    static class TitleMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.pagination.Title(parent);
        }
    }

    static class BlockMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.Block(parent);
        }
    }

    static class BlockContainerMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.BlockContainer(parent);
        }
    }

    static class BidiOverrideMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.BidiOverride(parent);
        }
    }

    static class CharacterMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.Character(parent);
        }
    }

    static class InitialPropertySetMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.InitialPropertySet(parent);
        }
    }

    static class ExternalGraphicMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.ExternalGraphic(parent);
        }
    }

    static class InstreamForeignObjectMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.InstreamForeignObject(parent);
        }
    }

    static class InlineMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.Inline(parent);
        }
    }

    static class InlineContainerMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.InlineContainer(parent);
        }
    }

    static class LeaderMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.Leader(parent);
        }
    }

    static class PageNumberMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.PageNumber(parent);
        }
    }

    static class PageNumberCitationMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.PageNumberCitation(parent);
        }
    }

    static class PageNumberCitationLastMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.PageNumberCitationLast(parent);
        }
    }

    static class TableAndCaptionMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.table.TableAndCaption(parent);
        }
    }

    static class TableMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.table.Table(parent);
        }
    }

    static class TableColumnMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.table.TableColumn(parent);
        }
    }

    static class TableCaptionMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.table.TableCaption(parent);
        }
    }

    static class TableBodyMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.table.TableBody(parent);
        }
    }

    static class TableHeaderMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.table.TableHeader(parent);
        }
    }

    static class TableFooterMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.table.TableFooter(parent);
        }
    }

    static class TableRowMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.table.TableRow(parent);
        }
    }

    static class TableCellMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.table.TableCell(parent);
        }
    }

    static class ListBlockMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.ListBlock(parent);
        }
    }

    static class ListItemMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.ListItem(parent);
        }
    }

    static class ListItemBodyMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.ListItemBody(parent);
        }
    }

    static class ListItemLabelMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.ListItemLabel(parent);
        }
    }

    static class BasicLinkMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.BasicLink(parent);
        }
    }

    static class MultiSwitchMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.MultiSwitch(parent);
        }
    }

    static class MultiCaseMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.MultiCase(parent);
        }
    }

    static class MultiToggleMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.MultiToggle(parent);
        }
    }

    static class MultiPropertiesMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.MultiProperties(parent);
        }
    }

    static class MultiPropertySetMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.MultiPropertySet(parent);
        }
    }

    static class FloatMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.Float(parent);
        }
    }

    static class FootnoteMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.Footnote(parent);
        }
    }

    static class FootnoteBodyMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.FootnoteBody(parent);
        }
    }

    static class WrapperMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.Wrapper(parent);
        }
    }

    static class MarkerMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.Marker(parent);
        }
    }

    static class RetrieveMarkerMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.RetrieveMarker(parent);
        }
    }

    static class RetrieveTableMarkerMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new org.apache.fop.fo.flow.RetrieveTableMarker(parent);
        }
    }
}
