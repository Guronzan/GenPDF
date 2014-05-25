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

/* $Id: RTFHandler.java 1310948 2012-04-08 03:57:07Z gadams $ */

package org.apache.fop.render.rtf;

// Java
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.fop.ResourceEventProducer;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.datatypes.LengthBase;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.XMLObj;
import org.apache.fop.fo.flow.AbstractGraphics;
import org.apache.fop.fo.flow.BasicLink;
import org.apache.fop.fo.flow.Block;
import org.apache.fop.fo.flow.BlockContainer;
import org.apache.fop.fo.flow.Character;
import org.apache.fop.fo.flow.ExternalGraphic;
import org.apache.fop.fo.flow.Footnote;
import org.apache.fop.fo.flow.FootnoteBody;
import org.apache.fop.fo.flow.Inline;
import org.apache.fop.fo.flow.InstreamForeignObject;
import org.apache.fop.fo.flow.Leader;
import org.apache.fop.fo.flow.ListBlock;
import org.apache.fop.fo.flow.ListItem;
import org.apache.fop.fo.flow.ListItemBody;
import org.apache.fop.fo.flow.ListItemLabel;
import org.apache.fop.fo.flow.PageNumber;
import org.apache.fop.fo.flow.PageNumberCitation;
import org.apache.fop.fo.flow.PageNumberCitationLast;
import org.apache.fop.fo.flow.table.Table;
import org.apache.fop.fo.flow.table.TableBody;
import org.apache.fop.fo.flow.table.TableCell;
import org.apache.fop.fo.flow.table.TableColumn;
import org.apache.fop.fo.flow.table.TableFooter;
import org.apache.fop.fo.flow.table.TableHeader;
import org.apache.fop.fo.flow.table.TablePart;
import org.apache.fop.fo.flow.table.TableRow;
import org.apache.fop.fo.pagination.Flow;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.PageSequenceMaster;
import org.apache.fop.fo.pagination.Region;
import org.apache.fop.fo.pagination.SimplePageMaster;
import org.apache.fop.fo.pagination.StaticContent;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.EnumLength;
import org.apache.fop.fonts.FontSetup;
import org.apache.fop.layoutmgr.inline.ImageLayout;
import org.apache.fop.layoutmgr.table.ColumnSetup;
import org.apache.fop.render.DefaultFontResolver;
import org.apache.fop.render.RendererEventProducer;
import org.apache.fop.render.rtf.rtflib.exceptions.RtfException;
import org.apache.fop.render.rtf.rtflib.rtfdoc.IRtfAfterContainer;
import org.apache.fop.render.rtf.rtflib.rtfdoc.IRtfBeforeContainer;
import org.apache.fop.render.rtf.rtflib.rtfdoc.IRtfListContainer;
import org.apache.fop.render.rtf.rtflib.rtfdoc.IRtfTableContainer;
import org.apache.fop.render.rtf.rtflib.rtfdoc.IRtfTextrunContainer;
import org.apache.fop.render.rtf.rtflib.rtfdoc.ITableAttributes;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfAfter;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfAttributes;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfBefore;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfDocumentArea;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfElement;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfExternalGraphic;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfFile;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfFootnote;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfHyperLink;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfList;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfListItem;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfListItem.RtfListItemLabel;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfPage;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfParagraphBreak;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfSection;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfTable;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfTableCell;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfTableRow;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfTextrun;
import org.apache.fop.render.rtf.rtflib.tools.BuilderContext;
import org.apache.fop.render.rtf.rtflib.tools.PercentContext;
import org.apache.fop.render.rtf.rtflib.tools.TableContext;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.ImageRawStream;
import org.apache.xmlgraphics.image.loader.impl.ImageXMLDOM;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * RTF Handler: generates RTF output using the structure events from the FO Tree
 * sent to this structure handler.
 */
@Slf4j
public class RTFHandler extends FOEventHandler {

    private RtfFile rtfFile;
    private final OutputStream os;
    private RtfSection sect;
    private RtfDocumentArea docArea;
    private boolean bDefer; // true, if each called handler shall be
    // processed at later time.
    private boolean bPrevHeaderSpecified = false; // true, if there has been a
    // header in any page-sequence
    private boolean bPrevFooterSpecified = false; // true, if there has been a
    // footer in any page-sequence
    private boolean bHeaderSpecified = false; // true, if there is a header
    // in current page-sequence
    private boolean bFooterSpecified = false; // true, if there is a footer
    // in current page-sequence
    private final BuilderContext builderContext = new BuilderContext(null);

    private SimplePageMaster pagemaster;

    private int nestedTableDepth = 1;

    private final PercentContext percentManager = new PercentContext();

    /**
     * Creates a new RTF structure handler.
     *
     * @param userAgent
     *            the FOUserAgent for this process
     * @param os
     *            OutputStream to write to
     */
    public RTFHandler(final FOUserAgent userAgent, final OutputStream os) {
        super(userAgent);
        this.os = os;
        this.bDefer = true;

        final boolean base14Kerning = false;
        FontSetup.setup(this.fontInfo, null,
                new DefaultFontResolver(userAgent), base14Kerning);
    }

    /**
     * Central exception handler for I/O exceptions.
     *
     * @param ioe
     *            IOException to handle
     */
    protected void handleIOTrouble(final IOException ioe) {
        final RendererEventProducer eventProducer = RendererEventProducer.Provider
                .get(getUserAgent().getEventBroadcaster());
        eventProducer.ioError(this, ioe);
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws SAXException {
        // TODO sections should be created
        try {
            this.rtfFile = new RtfFile(new OutputStreamWriter(this.os));
            this.docArea = this.rtfFile.startDocumentArea();
        } catch (final IOException ioe) {
            // TODO could we throw Exception in all FOEventHandler events?
            throw new SAXException(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {
        try {
            this.rtfFile.flush();
        } catch (final IOException ioe) {
            // TODO could we throw Exception in all FOEventHandler events?
            throw new SAXException(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final PageSequence pageSeq) {
        try {
            // This is needed for region handling
            if (this.pagemaster == null) {
                final String reference = pageSeq.getMasterReference();
                this.pagemaster = pageSeq.getRoot().getLayoutMasterSet()
                        .getSimplePageMaster(reference);
                if (this.pagemaster == null) {
                    final RTFEventProducer eventProducer = RTFEventProducer.Provider
                            .get(getUserAgent().getEventBroadcaster());
                    eventProducer.onlySPMSupported(this, reference,
                            pageSeq.getLocator());
                    final PageSequenceMaster master = pageSeq.getRoot()
                            .getLayoutMasterSet()
                            .getPageSequenceMaster(reference);
                    this.pagemaster = master.getNextSimplePageMaster(false,
                            false, false, false, pageSeq.getMainFlow()
                                    .getFlowName());
                }
            }

            if (this.bDefer) {
                return;
            }

            this.sect = this.docArea.newSection();

            // read page size and margins, if specified
            // only simple-page-master supported, so pagemaster may be null
            if (this.pagemaster != null) {
                this.sect.getRtfAttributes().set(
                        PageAttributesConverter
                                .convertPageAttributes(this.pagemaster));
            } else {
                final RTFEventProducer eventProducer = RTFEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.noSPMFound(this, pageSeq.getLocator());
            }

            this.builderContext.pushContainer(this.sect);

            // Calculate usable page width for this flow
            final int useAblePageWidth = this.pagemaster.getPageWidth()
                    .getValue()
                    - this.pagemaster.getCommonMarginBlock().marginLeft
                            .getValue()
                    - this.pagemaster.getCommonMarginBlock().marginRight
                            .getValue()
                    - this.sect.getRtfAttributes()
                            .getValueAsInteger(RtfPage.MARGIN_LEFT).intValue()
                    - this.sect.getRtfAttributes()
                            .getValueAsInteger(RtfPage.MARGIN_RIGHT).intValue();
            this.percentManager.setDimension(pageSeq, useAblePageWidth);

            this.bHeaderSpecified = false;
            this.bFooterSpecified = false;
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPageSequence(final PageSequence pageSeq) {
        if (this.bDefer) {
            // If endBlock was called while SAX parsing, and the passed FO is
            // Block
            // nested within another Block, stop deferring.
            // Now process all deferred FOs.
            this.bDefer = false;
            recurseFONode(pageSeq);
            this.pagemaster = null;
            this.bDefer = true;

            return;
        } else {
            this.builderContext.popContainer();
            this.pagemaster = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startFlow(final Flow fl) {
        if (this.bDefer) {
            return;
        }

        try {
            log.debug("starting flow: " + fl.getFlowName());
            boolean handled = false;
            final Region regionBody = this.pagemaster
                    .getRegion(Constants.FO_REGION_BODY);
            final Region regionBefore = this.pagemaster
                    .getRegion(Constants.FO_REGION_BEFORE);
            final Region regionAfter = this.pagemaster
                    .getRegion(Constants.FO_REGION_AFTER);
            if (fl.getFlowName().equals(regionBody.getRegionName())) {
                // if there is no header in current page-sequence but there has
                // been
                // a header in a previous page-sequence, insert an empty header.
                if (this.bPrevHeaderSpecified && !this.bHeaderSpecified) {
                    final RtfAttributes attr = new RtfAttributes();
                    attr.set(RtfBefore.HEADER);

                    final IRtfBeforeContainer contBefore = (IRtfBeforeContainer) this.builderContext
                            .getContainer(IRtfBeforeContainer.class, true, this);
                    contBefore.newBefore(attr);
                }

                // if there is no footer in current page-sequence but there has
                // been
                // a footer in a previous page-sequence, insert an empty footer.
                if (this.bPrevFooterSpecified && !this.bFooterSpecified) {
                    final RtfAttributes attr = new RtfAttributes();
                    attr.set(RtfAfter.FOOTER);

                    final IRtfAfterContainer contAfter = (IRtfAfterContainer) this.builderContext
                            .getContainer(IRtfAfterContainer.class, true, this);
                    contAfter.newAfter(attr);
                }
                handled = true;
            } else if (regionBefore != null
                    && fl.getFlowName().equals(regionBefore.getRegionName())) {
                this.bHeaderSpecified = true;
                this.bPrevHeaderSpecified = true;

                final IRtfBeforeContainer c = (IRtfBeforeContainer) this.builderContext
                        .getContainer(IRtfBeforeContainer.class, true, this);

                RtfAttributes beforeAttributes = ((RtfElement) c)
                        .getRtfAttributes();
                if (beforeAttributes == null) {
                    beforeAttributes = new RtfAttributes();
                }
                beforeAttributes.set(RtfBefore.HEADER);

                final RtfBefore before = c.newBefore(beforeAttributes);
                this.builderContext.pushContainer(before);
                handled = true;
            } else if (regionAfter != null
                    && fl.getFlowName().equals(regionAfter.getRegionName())) {
                this.bFooterSpecified = true;
                this.bPrevFooterSpecified = true;

                final IRtfAfterContainer c = (IRtfAfterContainer) this.builderContext
                        .getContainer(IRtfAfterContainer.class, true, this);

                RtfAttributes afterAttributes = ((RtfElement) c)
                        .getRtfAttributes();
                if (afterAttributes == null) {
                    afterAttributes = new RtfAttributes();
                }

                afterAttributes.set(RtfAfter.FOOTER);

                final RtfAfter after = c.newAfter(afterAttributes);
                this.builderContext.pushContainer(after);
                handled = true;
            }
            if (!handled) {
                log.warn("A " + fl.getLocalName() + " has been skipped: "
                        + fl.getFlowName());
            }
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startFlow: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endFlow(final Flow fl) {
        if (this.bDefer) {
            return;
        }

        try {
            final Region regionBody = this.pagemaster
                    .getRegion(Constants.FO_REGION_BODY);
            final Region regionBefore = this.pagemaster
                    .getRegion(Constants.FO_REGION_BEFORE);
            final Region regionAfter = this.pagemaster
                    .getRegion(Constants.FO_REGION_AFTER);
            if (fl.getFlowName().equals(regionBody.getRegionName())) {
                // just do nothing
            } else if (regionBefore != null
                    && fl.getFlowName().equals(regionBefore.getRegionName())) {
                this.builderContext.popContainer();
            } else if (regionAfter != null
                    && fl.getFlowName().equals(regionAfter.getRegionName())) {
                this.builderContext.popContainer();
            }
        } catch (final Exception e) {
            log.error("endFlow: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startBlock(final Block bl) {
        if (this.bDefer) {
            return;
        }

        try {
            final RtfAttributes rtfAttr = TextAttributesConverter
                    .convertAttributes(bl);

            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);

            final RtfTextrun textrun = container.getTextrun();

            textrun.addParagraphBreak();
            textrun.pushBlockAttributes(rtfAttr);
            textrun.addBookmark(bl.getId());
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startBlock: " + e.getMessage());
            throw new RuntimeException("Exception: " + e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endBlock(final Block bl) {

        if (this.bDefer) {
            return;
        }

        try {
            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);

            final RtfTextrun textrun = container.getTextrun();
            final RtfParagraphBreak par = textrun.addParagraphBreak();

            final RtfTableCell cellParent = (RtfTableCell) textrun
                    .getParentOfClass(RtfTableCell.class);
            if (cellParent != null && par != null) {
                final int iDepth = cellParent.findChildren(textrun);
                cellParent.setLastParagraph(par, iDepth);
            }

            final int breakValue = toRtfBreakValue(bl.getBreakAfter());
            textrun.popBlockAttributes(breakValue);

        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startBlock:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startBlockContainer(final BlockContainer blc) {
        if (this.bDefer) {
            return;
        }

        try {
            final RtfAttributes rtfAttr = TextAttributesConverter
                    .convertBlockContainerAttributes(blc);

            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);

            final RtfTextrun textrun = container.getTextrun();

            textrun.addParagraphBreak();
            textrun.pushBlockAttributes(rtfAttr);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startBlock: " + e.getMessage());
            throw new RuntimeException("Exception: " + e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endBlockContainer(final BlockContainer bl) {
        if (this.bDefer) {
            return;
        }

        try {
            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);

            final RtfTextrun textrun = container.getTextrun();

            textrun.addParagraphBreak();
            final int breakValue = toRtfBreakValue(bl.getBreakAfter());
            textrun.popBlockAttributes(breakValue);

        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startBlock:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private int toRtfBreakValue(final int foBreakValue) {
        switch (foBreakValue) {
        case Constants.EN_PAGE:
            return RtfTextrun.BREAK_PAGE;
        case Constants.EN_EVEN_PAGE:
            return RtfTextrun.BREAK_EVEN_PAGE;
        case Constants.EN_ODD_PAGE:
            return RtfTextrun.BREAK_ODD_PAGE;
        case Constants.EN_COLUMN:
            return RtfTextrun.BREAK_COLUMN;
        default:
            return RtfTextrun.BREAK_NONE;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startTable(final Table tbl) {
        if (this.bDefer) {
            return;
        }

        // create an RtfTable in the current table container
        final TableContext tableContext = new TableContext(this.builderContext);

        try {
            final IRtfTableContainer tc = (IRtfTableContainer) this.builderContext
                    .getContainer(IRtfTableContainer.class, true, null);

            final RtfAttributes atts = TableAttributesConverter
                    .convertTableAttributes(tbl);

            final RtfTable table = tc.newTable(atts, tableContext);
            table.setNestedTableDepth(this.nestedTableDepth);
            this.nestedTableDepth++;

            final CommonBorderPaddingBackground border = tbl
                    .getCommonBorderPaddingBackground();
            final RtfAttributes borderAttributes = new RtfAttributes();

            BorderAttributesConverter.makeBorder(border,
                    CommonBorderPaddingBackground.BEFORE, borderAttributes,
                    ITableAttributes.CELL_BORDER_TOP);
            BorderAttributesConverter.makeBorder(border,
                    CommonBorderPaddingBackground.AFTER, borderAttributes,
                    ITableAttributes.CELL_BORDER_BOTTOM);
            BorderAttributesConverter.makeBorder(border,
                    CommonBorderPaddingBackground.START, borderAttributes,
                    ITableAttributes.CELL_BORDER_LEFT);
            BorderAttributesConverter.makeBorder(border,
                    CommonBorderPaddingBackground.END, borderAttributes,
                    ITableAttributes.CELL_BORDER_RIGHT);

            table.setBorderAttributes(borderAttributes);

            this.builderContext.pushContainer(table);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startTable:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        this.builderContext.pushTableContext(tableContext);
    }

    /** {@inheritDoc} */
    @Override
    public void endTable(final Table tbl) {
        if (this.bDefer) {
            return;
        }

        this.nestedTableDepth--;
        this.builderContext.popTableContext();
        this.builderContext.popContainer();
    }

    /** {@inheritDoc} */
    @Override
    public void startColumn(final TableColumn tc) {
        if (this.bDefer) {
            return;
        }

        try {
            final int iWidth = tc.getColumnWidth()
                    .getValue(this.percentManager);
            this.percentManager.setDimension(tc, iWidth);

            // convert to twips
            final Float width = new Float(FoUnitsConverter.getInstance()
                    .convertMptToTwips(iWidth));
            this.builderContext.getTableContext().setNextColumnWidth(width);
            this.builderContext.getTableContext().setNextColumnRowSpanning(
                    new Integer(0), null);
            this.builderContext.getTableContext()
                    .setNextFirstSpanningCol(false);
        } catch (final Exception e) {
            log.error("startColumn: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endColumn(final TableColumn tc) {
        if (this.bDefer) {
            return;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startHeader(final TableHeader header) {
        startPart(header);
    }

    /** {@inheritDoc} */
    @Override
    public void endHeader(final TableHeader header) {
        endPart(header);
    }

    /** {@inheritDoc} */
    @Override
    public void startFooter(final TableFooter footer) {
        startPart(footer);
    }

    /** {@inheritDoc} */
    @Override
    public void endFooter(final TableFooter footer) {
        endPart(footer);
    }

    /** {@inheritDoc} */
    @Override
    public void startInline(final Inline inl) {
        if (this.bDefer) {
            return;
        }

        try {
            final RtfAttributes rtfAttr = TextAttributesConverter
                    .convertCharacterAttributes(inl);

            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);

            final RtfTextrun textrun = container.getTextrun();
            textrun.pushInlineAttributes(rtfAttr);
            textrun.addBookmark(inl.getId());
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startInline:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endInline(final Inline inl) {
        if (this.bDefer) {
            return;
        }

        try {
            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);

            final RtfTextrun textrun = container.getTextrun();
            textrun.popInlineAttributes();
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startInline:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private void startPart(final TablePart part) {
        if (this.bDefer) {
            return;
        }

        try {
            final RtfAttributes atts = TableAttributesConverter
                    .convertTablePartAttributes(part);

            final RtfTable tbl = (RtfTable) this.builderContext.getContainer(
                    RtfTable.class, true, this);
            tbl.setHeaderAttribs(atts);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startPart: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private void endPart(final TablePart tb) {
        if (this.bDefer) {
            return;
        }

        try {
            final RtfTable tbl = (RtfTable) this.builderContext.getContainer(
                    RtfTable.class, true, this);
            tbl.setHeaderAttribs(null);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("endPart: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startBody(final TableBody body) {
        startPart(body);
    }

    /** {@inheritDoc} */
    @Override
    public void endBody(final TableBody body) {
        endPart(body);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startRow(final TableRow tr) {
        if (this.bDefer) {
            return;
        }

        try {
            // create an RtfTableRow in the current RtfTable
            final RtfTable tbl = (RtfTable) this.builderContext.getContainer(
                    RtfTable.class, true, null);

            final RtfAttributes atts = TableAttributesConverter
                    .convertRowAttributes(tr, tbl.getHeaderAttribs());

            if (tr.getParent() instanceof TableHeader) {
                atts.set(ITableAttributes.ATTR_HEADER);
            }

            this.builderContext.pushContainer(tbl.newTableRow(atts));

            // reset column iteration index to correctly access column widths
            this.builderContext.getTableContext().selectFirstColumn();
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startRow: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endRow(final TableRow tr) {
        if (this.bDefer) {
            return;
        }

        try {
            final TableContext tctx = this.builderContext.getTableContext();
            final RtfTableRow row = (RtfTableRow) this.builderContext
                    .getContainer(RtfTableRow.class, true, null);

            // while the current column is in row-spanning, act as if
            // a vertical merged cell would have been specified.
            while (tctx.getNumberOfColumns() > tctx.getColumnIndex()
                    && tctx.getColumnRowSpanningNumber().intValue() > 0) {
                final RtfTableCell vCell = row.newTableCellMergedVertically(
                        (int) tctx.getColumnWidth(),
                        tctx.getColumnRowSpanningAttrs());

                if (!tctx.getFirstSpanningCol()) {
                    vCell.setHMerge(RtfTableCell.MERGE_WITH_PREVIOUS);
                }

                tctx.selectNextColumn();
            }
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("endRow: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        this.builderContext.popContainer();
        this.builderContext.getTableContext().decreaseRowSpannings();
    }

    /** {@inheritDoc} */
    @Override
    public void startCell(final TableCell tc) {
        if (this.bDefer) {
            return;
        }

        try {
            final TableContext tctx = this.builderContext.getTableContext();
            final RtfTableRow row = (RtfTableRow) this.builderContext
                    .getContainer(RtfTableRow.class, true, null);

            final int numberRowsSpanned = tc.getNumberRowsSpanned();
            final int numberColumnsSpanned = tc.getNumberColumnsSpanned();

            // while the current column is in row-spanning, act as if
            // a vertical merged cell would have been specified.
            while (tctx.getNumberOfColumns() > tctx.getColumnIndex()
                    && tctx.getColumnRowSpanningNumber().intValue() > 0) {
                final RtfTableCell vCell = row.newTableCellMergedVertically(
                        (int) tctx.getColumnWidth(),
                        tctx.getColumnRowSpanningAttrs());

                if (!tctx.getFirstSpanningCol()) {
                    vCell.setHMerge(RtfTableCell.MERGE_WITH_PREVIOUS);
                }

                tctx.selectNextColumn();
            }

            // get the width of the currently started cell
            float width = tctx.getColumnWidth();

            // create an RtfTableCell in the current RtfTableRow
            final RtfAttributes atts = TableAttributesConverter
                    .convertCellAttributes(tc);
            final RtfTableCell cell = row.newTableCell((int) width, atts);

            // process number-rows-spanned attribute
            if (numberRowsSpanned > 1) {
                // Start vertical merge
                cell.setVMerge(RtfTableCell.MERGE_START);

                // set the number of rows spanned
                tctx.setCurrentColumnRowSpanning(
                        new Integer(numberRowsSpanned), cell.getRtfAttributes());
            } else {
                tctx.setCurrentColumnRowSpanning(
                        new Integer(numberRowsSpanned), null);
            }

            // process number-columns-spanned attribute
            if (numberColumnsSpanned > 0) {
                // Get the number of columns spanned
                tctx.setCurrentFirstSpanningCol(true);

                // We widthdraw one cell because the first cell is already
                // created
                // (it's the current cell) !
                for (int i = 0; i < numberColumnsSpanned - 1; ++i) {
                    tctx.selectNextColumn();

                    // aggregate width for further elements
                    width += tctx.getColumnWidth();
                    tctx.setCurrentFirstSpanningCol(false);
                    final RtfTableCell hCell = row
                            .newTableCellMergedHorizontally(0, null);

                    if (numberRowsSpanned > 1) {
                        // Start vertical merge
                        hCell.setVMerge(RtfTableCell.MERGE_START);

                        // set the number of rows spanned
                        tctx.setCurrentColumnRowSpanning(new Integer(
                                numberRowsSpanned), cell.getRtfAttributes());
                    } else {
                        tctx.setCurrentColumnRowSpanning(new Integer(
                                numberRowsSpanned), cell.getRtfAttributes());
                    }
                }
            }
            // save width of the cell, convert from twips to mpt
            this.percentManager.setDimension(tc, (int) width * 50);

            this.builderContext.pushContainer(cell);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startCell: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endCell(final TableCell tc) {
        if (this.bDefer) {
            return;
        }
        try {
            final RtfTableCell cell = (RtfTableCell) this.builderContext
                    .getContainer(RtfTableCell.class, false, this);
            cell.finish();

        } catch (final Exception e) {
            log.error("endCell: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        this.builderContext.popContainer();
        this.builderContext.getTableContext().selectNextColumn();
    }

    // Lists
    /** {@inheritDoc} */
    @Override
    public void startList(final ListBlock lb) {
        if (this.bDefer) {
            return;
        }

        try {
            // create an RtfList in the current list container
            final IRtfListContainer c = (IRtfListContainer) this.builderContext
                    .getContainer(IRtfListContainer.class, true, this);
            final RtfList newList = c.newList(ListAttributesConverter
                    .convertAttributes(lb));
            this.builderContext.pushContainer(newList);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final FOPException fe) {
            log.error("startList: " + fe.getMessage());
            throw new RuntimeException(fe.getMessage());
        } catch (final Exception e) {
            log.error("startList: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endList(final ListBlock lb) {
        if (this.bDefer) {
            return;
        }

        this.builderContext.popContainer();
    }

    /** {@inheritDoc} */
    @Override
    public void startListItem(final ListItem li) {
        if (this.bDefer) {
            return;
        }

        // create an RtfListItem in the current RtfList
        try {
            RtfList list = (RtfList) this.builderContext.getContainer(
                    RtfList.class, true, this);

            /**
             * If the current list already contains a list item, then close the
             * list and open a new one, so every single list item gets its own
             * list. This allows every item to have a different list label. If
             * all the items would be in the same list, they had all the same
             * label.
             */
            // TODO: do this only, if the labels content <> previous labels
            // content
            if (list.getChildCount() > 0) {
                endListBody(null);
                endList((ListBlock) li.getParent());
                startList((ListBlock) li.getParent());
                startListBody(null);

                list = (RtfList) this.builderContext.getContainer(
                        RtfList.class, true, this);
            }

            this.builderContext.pushContainer(list.newListItem());
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startList: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endListItem(final ListItem li) {
        if (this.bDefer) {
            return;
        }

        this.builderContext.popContainer();
    }

    /** {@inheritDoc} */
    @Override
    public void startListLabel(final ListItemLabel listItemLabel) {
        if (this.bDefer) {
            return;
        }

        try {
            final RtfListItem item = (RtfListItem) this.builderContext
                    .getContainer(RtfListItem.class, true, this);

            final RtfListItemLabel label = item.new RtfListItemLabel(item);
            this.builderContext.pushContainer(label);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startPageNumber: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endListLabel(final ListItemLabel listItemLabel) {
        if (this.bDefer) {
            return;
        }

        this.builderContext.popContainer();
    }

    /** {@inheritDoc} */
    @Override
    public void startListBody(final ListItemBody listItemBody) {
    }

    /** {@inheritDoc} */
    @Override
    public void endListBody(final ListItemBody listItemBody) {
    }

    // Static Regions
    /** {@inheritDoc} */
    @Override
    public void startStatic(final StaticContent staticContent) {
    }

    /** {@inheritDoc} */
    @Override
    public void endStatic(final StaticContent statisContent) {
    }

    /** {@inheritDoc} */
    @Override
    public void startMarkup() {
    }

    /** {@inheritDoc} */
    @Override
    public void endMarkup() {
    }

    /** {@inheritDoc} */
    @Override
    public void startLink(final BasicLink basicLink) {
        if (this.bDefer) {
            return;
        }

        try {
            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);

            final RtfTextrun textrun = container.getTextrun();

            final RtfHyperLink link = textrun.addHyperlink(new RtfAttributes());

            if (basicLink.hasExternalDestination()) {
                link.setExternalURL(basicLink.getExternalDestination());
            } else {
                link.setInternalURL(basicLink.getInternalDestination());
            }

            this.builderContext.pushContainer(link);

        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startLink: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endLink(final BasicLink basicLink) {
        if (this.bDefer) {
            return;
        }

        this.builderContext.popContainer();
    }

    /** {@inheritDoc} */
    @Override
    public void image(final ExternalGraphic eg) {
        if (this.bDefer) {
            return;
        }

        final String uri = eg.getURL();
        ImageInfo info = null;
        try {

            // set image data
            final FOUserAgent userAgent = eg.getUserAgent();
            final ImageManager manager = userAgent.getFactory()
                    .getImageManager();
            info = manager
                    .getImageInfo(uri, userAgent.getImageSessionContext());

            putGraphic(eg, info);
        } catch (final ImageException ie) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageError(this,
                    info != null ? info.toString() : uri, ie, null);
        } catch (final FileNotFoundException fe) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageNotFound(this, info != null ? info.toString()
                    : uri, fe, null);
        } catch (final IOException ioe) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageIOError(this, info != null ? info.toString()
                    : uri, ioe, null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endInstreamForeignObject(final InstreamForeignObject ifo) {
        if (this.bDefer) {
            return;
        }

        try {
            final XMLObj child = ifo.getChildXMLObj();
            final Document doc = child.getDOMDocument();
            final String ns = child.getNamespaceURI();

            final ImageInfo info = new ImageInfo(null, null);
            // Set the resolution to that of the FOUserAgent
            final FOUserAgent ua = ifo.getUserAgent();
            final ImageSize size = new ImageSize();
            size.setResolution(ua.getSourceResolution());

            // Set the image size to the size of the svg.
            final Point2D csize = new Point2D.Float(-1, -1);
            final Point2D intrinsicDimensions = child.getDimension(csize);
            if (intrinsicDimensions == null) {
                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.ifoNoIntrinsicSize(this, child.getLocator());
                return;
            }
            size.setSizeInMillipoints(
                    (int) Math.round(intrinsicDimensions.getX() * 1000),
                    (int) Math.round(intrinsicDimensions.getY() * 1000));
            size.calcPixelsFromSize();
            info.setSize(size);

            final ImageXMLDOM image = new ImageXMLDOM(info, doc, ns);

            final FOUserAgent userAgent = ifo.getUserAgent();
            final ImageManager manager = userAgent.getFactory()
                    .getImageManager();
            final Map hints = ImageUtil.getDefaultHints(ua
                    .getImageSessionContext());
            final Image converted = manager.convertImage(image, FLAVORS, hints);
            putGraphic(ifo, converted);

        } catch (final ImageException ie) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageError(this, null, ie, null);
        } catch (final IOException ioe) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageIOError(this, null, ioe, null);
        }
    }

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] {
            ImageFlavor.RAW_EMF, ImageFlavor.RAW_PNG, ImageFlavor.RAW_JPEG };

    /**
     * Puts a graphic/image into the generated RTF file.
     *
     * @param abstractGraphic
     *            the graphic (external-graphic or instream-foreign-object)
     * @param info
     *            the image info object
     * @throws IOException
     *             In case of an I/O error
     */
    private void putGraphic(final AbstractGraphics abstractGraphic,
            final ImageInfo info) throws IOException {
        try {
            final FOUserAgent userAgent = abstractGraphic.getUserAgent();
            final ImageManager manager = userAgent.getFactory()
                    .getImageManager();
            final ImageSessionContext sessionContext = userAgent
                    .getImageSessionContext();
            final Map hints = ImageUtil.getDefaultHints(sessionContext);
            final Image image = manager.getImage(info, FLAVORS, hints,
                    sessionContext);

            putGraphic(abstractGraphic, image);
        } catch (final ImageException ie) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageError(this, null, ie, null);
        }
    }

    /**
     * Puts a graphic/image into the generated RTF file.
     *
     * @param abstractGraphic
     *            the graphic (external-graphic or instream-foreign-object)
     * @param image
     *            the image
     * @throws IOException
     *             In case of an I/O error
     */
    private void putGraphic(final AbstractGraphics abstractGraphic,
            final Image image) throws IOException {
        byte[] rawData = null;

        final ImageInfo info = image.getInfo();

        if (image instanceof ImageRawStream) {
            final ImageRawStream rawImage = (ImageRawStream) image;
            final InputStream in = rawImage.createInputStream();
            try {
                rawData = IOUtils.toByteArray(in);
            } finally {
                IOUtils.closeQuietly(in);
            }
        }

        if (rawData == null) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageWritingError(this, null);
            return;
        }

        // Set up percentage calculations
        this.percentManager.setDimension(abstractGraphic);
        final PercentBaseContext pContext = new PercentBaseContext() {

            @Override
            public int getBaseLength(final int lengthBase, final FObj fobj) {
                switch (lengthBase) {
                case LengthBase.IMAGE_INTRINSIC_WIDTH:
                    return info.getSize().getWidthMpt();
                case LengthBase.IMAGE_INTRINSIC_HEIGHT:
                    return info.getSize().getHeightMpt();
                default:
                    return RTFHandler.this.percentManager.getBaseLength(
                            lengthBase, fobj);
                }
            }

        };
        final ImageLayout layout = new ImageLayout(abstractGraphic, pContext,
                image.getInfo().getSize().getDimensionMpt());

        final IRtfTextrunContainer c = (IRtfTextrunContainer) this.builderContext
                .getContainer(IRtfTextrunContainer.class, true, this);

        final RtfExternalGraphic rtfGraphic = c.getTextrun().newImage();

        // set URL
        if (info.getOriginalURI() != null) {
            rtfGraphic.setURL(info.getOriginalURI());
        }
        rtfGraphic.setImageData(rawData);

        final FoUnitsConverter converter = FoUnitsConverter.getInstance();
        final Dimension viewport = layout.getViewportSize();
        final Rectangle placement = layout.getPlacement();
        final int cropLeft = Math.round(converter
                .convertMptToTwips(-placement.x));
        final int cropTop = Math.round(converter
                .convertMptToTwips(-placement.y));
        final int cropRight = Math.round(converter.convertMptToTwips(-1
                * (viewport.width - placement.x - placement.width)));
        final int cropBottom = Math.round(converter.convertMptToTwips(-1
                * (viewport.height - placement.y - placement.height)));
        rtfGraphic.setCropping(cropLeft, cropTop, cropRight, cropBottom);

        int width = Math.round(converter.convertMptToTwips(viewport.width));
        int height = Math.round(converter.convertMptToTwips(viewport.height));
        width += cropLeft + cropRight;
        height += cropTop + cropBottom;
        rtfGraphic.setWidthTwips(width);
        rtfGraphic.setHeightTwips(height);

        // TODO: make this configurable:
        // int compression =
        // m_context.m_options.getRtfExternalGraphicCompressionRate ();
        final int compression = 0;
        if (compression != 0) {
            if (!rtfGraphic.setCompressionRate(compression)) {
                log.warn("The compression rate "
                        + compression
                        + " is invalid. The value has to be between 1 and 100 %.");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void pageRef() {
    }

    /** {@inheritDoc} */
    @Override
    public void startFootnote(final Footnote footnote) {
        if (this.bDefer) {
            return;
        }

        try {
            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);

            final RtfTextrun textrun = container.getTextrun();
            final RtfFootnote rtfFootnote = textrun.addFootnote();

            this.builderContext.pushContainer(rtfFootnote);

        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startFootnote: " + e.getMessage());
            throw new RuntimeException("Exception: " + e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endFootnote(final Footnote footnote) {
        if (this.bDefer) {
            return;
        }

        this.builderContext.popContainer();
    }

    /** {@inheritDoc} */
    @Override
    public void startFootnoteBody(final FootnoteBody body) {
        if (this.bDefer) {
            return;
        }

        try {
            final RtfFootnote rtfFootnote = (RtfFootnote) this.builderContext
                    .getContainer(RtfFootnote.class, true, this);

            rtfFootnote.startBody();
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startFootnoteBody: " + e.getMessage());
            throw new RuntimeException("Exception: " + e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endFootnoteBody(final FootnoteBody body) {
        if (this.bDefer) {
            return;
        }

        try {
            final RtfFootnote rtfFootnote = (RtfFootnote) this.builderContext
                    .getContainer(RtfFootnote.class, true, this);

            rtfFootnote.endBody();
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("endFootnoteBody: " + e.getMessage());
            throw new RuntimeException("Exception: " + e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startLeader(final Leader l) {
        if (this.bDefer) {
            return;
        }

        try {
            this.percentManager.setDimension(l);
            final RtfAttributes rtfAttr = TextAttributesConverter
                    .convertLeaderAttributes(l, this.percentManager);

            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);
            final RtfTextrun textrun = container.getTextrun();

            textrun.addLeader(rtfAttr);
        } catch (final IOException e) {
            log.error("startLeader: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * @param text
     *            FOText object
     * @param characters
     *            CharSequence of the characters to process.
     */
    public void text(final FOText text, final CharSequence characters) {
        if (this.bDefer) {
            return;
        }

        try {
            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);

            final RtfTextrun textrun = container.getTextrun();
            final RtfAttributes rtfAttr = TextAttributesConverter
                    .convertCharacterAttributes(text);

            textrun.pushInlineAttributes(rtfAttr);
            textrun.addString(characters.toString());
            textrun.popInlineAttributes();
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("characters:" + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageNumber(final PageNumber pagenum) {
        if (this.bDefer) {
            return;
        }

        try {
            final RtfAttributes rtfAttr = TextAttributesConverter
                    .convertCharacterAttributes(pagenum);

            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);

            final RtfTextrun textrun = container.getTextrun();
            textrun.addPageNumber(rtfAttr);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        } catch (final Exception e) {
            log.error("startPageNumber: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPageNumber(final PageNumber pagenum) {
        if (this.bDefer) {
            return;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageNumberCitation(final PageNumberCitation l) {
        if (this.bDefer) {
            return;
        }
        try {

            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);
            final RtfTextrun textrun = container.getTextrun();

            textrun.addPageNumberCitation(l.getRefId());

        } catch (final Exception e) {
            log.error("startPageNumberCitation: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageNumberCitationLast(final PageNumberCitationLast l) {
        if (this.bDefer) {
            return;
        }
        try {

            final IRtfTextrunContainer container = (IRtfTextrunContainer) this.builderContext
                    .getContainer(IRtfTextrunContainer.class, true, this);
            final RtfTextrun textrun = container.getTextrun();

            textrun.addPageNumberCitation(l.getRefId());

        } catch (final RtfException e) {
            log.error("startPageNumberCitationLast: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        } catch (final IOException e) {
            log.error("startPageNumberCitationLast: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private void prepareTable(final Table tab) {
        // Allows to receive the available width of the table
        this.percentManager.setDimension(tab);

        // Table gets expanded by half of the border on each side inside Word
        // When using wide borders the table gets cut off
        final int tabDiff = tab.getCommonBorderPaddingBackground()
                .getBorderStartWidth(false)
                / 2
                + tab.getCommonBorderPaddingBackground().getBorderEndWidth(
                        false);

        // check for "auto" value
        if (!(tab.getInlineProgressionDimension().getMaximum(null).getLength() instanceof EnumLength)) {
            // value specified
            this.percentManager.setDimension(tab, tab
                    .getInlineProgressionDimension().getMaximum(null)
                    .getLength().getValue(this.percentManager)
                    - tabDiff);
        } else {
            // set table width again without border width
            this.percentManager.setDimension(
                    tab,
                    this.percentManager.getBaseLength(
                            LengthBase.CONTAINING_BLOCK_WIDTH, tab) - tabDiff);
        }

        final ColumnSetup columnSetup = new ColumnSetup(tab);
        // int sumOfColumns = columnSetup.getSumOfColumnWidths(percentManager);
        final float tableWidth = this.percentManager.getBaseLength(
                LengthBase.CONTAINING_BLOCK_WIDTH, tab);
        final float tableUnit = columnSetup.computeTableUnit(
                this.percentManager, Math.round(tableWidth));
        this.percentManager.setTableUnit(tab, Math.round(tableUnit));

    }

    /**
     * Calls the appropriate event handler for the passed FObj.
     *
     * @param foNode
     *            FO node whose event is to be called
     * @param bStart
     *            TRUE calls the start handler, FALSE the end handler
     */
    private void invokeDeferredEvent(final FONode foNode, final boolean bStart) { // CSOK:
        // MethodLength
        if (foNode instanceof PageSequence) {
            if (bStart) {
                startPageSequence((PageSequence) foNode);
            } else {
                endPageSequence((PageSequence) foNode);
            }
        } else if (foNode instanceof Flow) {
            if (bStart) {
                startFlow((Flow) foNode);
            } else {
                endFlow((Flow) foNode);
            }
        } else if (foNode instanceof StaticContent) {
            if (bStart) {
                startStatic(null);
            } else {
                endStatic(null);
            }
        } else if (foNode instanceof ExternalGraphic) {
            if (bStart) {
                image((ExternalGraphic) foNode);
            }
        } else if (foNode instanceof InstreamForeignObject) {
            if (bStart) {
                endInstreamForeignObject((InstreamForeignObject) foNode);
            }
        } else if (foNode instanceof Block) {
            if (bStart) {
                startBlock((Block) foNode);
            } else {
                endBlock((Block) foNode);
            }
        } else if (foNode instanceof BlockContainer) {
            if (bStart) {
                startBlockContainer((BlockContainer) foNode);
            } else {
                endBlockContainer((BlockContainer) foNode);
            }
        } else if (foNode instanceof BasicLink) {
            // BasicLink must be placed before Inline
            if (bStart) {
                startLink((BasicLink) foNode);
            } else {
                endLink(null);
            }
        } else if (foNode instanceof Inline) {
            if (bStart) {
                startInline((Inline) foNode);
            } else {
                endInline((Inline) foNode);
            }
        } else if (foNode instanceof FOText) {
            if (bStart) {
                final FOText text = (FOText) foNode;
                text(text, text.getCharSequence());
            }
        } else if (foNode instanceof Character) {
            if (bStart) {
                final Character c = (Character) foNode;
                character(c);
            }
        } else if (foNode instanceof PageNumber) {
            if (bStart) {
                startPageNumber((PageNumber) foNode);
            } else {
                endPageNumber((PageNumber) foNode);
            }
        } else if (foNode instanceof Footnote) {
            if (bStart) {
                startFootnote((Footnote) foNode);
            } else {
                endFootnote((Footnote) foNode);
            }
        } else if (foNode instanceof FootnoteBody) {
            if (bStart) {
                startFootnoteBody((FootnoteBody) foNode);
            } else {
                endFootnoteBody((FootnoteBody) foNode);
            }
        } else if (foNode instanceof ListBlock) {
            if (bStart) {
                startList((ListBlock) foNode);
            } else {
                endList((ListBlock) foNode);
            }
        } else if (foNode instanceof ListItemBody) {
            if (bStart) {
                startListBody(null);
            } else {
                endListBody(null);
            }
        } else if (foNode instanceof ListItem) {
            if (bStart) {
                startListItem((ListItem) foNode);
            } else {
                endListItem((ListItem) foNode);
            }
        } else if (foNode instanceof ListItemLabel) {
            if (bStart) {
                startListLabel(null);
            } else {
                endListLabel(null);
            }
        } else if (foNode instanceof Table) {
            if (bStart) {
                startTable((Table) foNode);
            } else {
                endTable((Table) foNode);
            }
        } else if (foNode instanceof TableHeader) {
            if (bStart) {
                startHeader((TableHeader) foNode);
            } else {
                endHeader((TableHeader) foNode);
            }
        } else if (foNode instanceof TableFooter) {
            if (bStart) {
                startFooter((TableFooter) foNode);
            } else {
                endFooter((TableFooter) foNode);
            }
        } else if (foNode instanceof TableBody) {
            if (bStart) {
                startBody((TableBody) foNode);
            } else {
                endBody((TableBody) foNode);
            }
        } else if (foNode instanceof TableColumn) {
            if (bStart) {
                startColumn((TableColumn) foNode);
            } else {
                endColumn((TableColumn) foNode);
            }
        } else if (foNode instanceof TableRow) {
            if (bStart) {
                startRow((TableRow) foNode);
            } else {
                endRow((TableRow) foNode);
            }
        } else if (foNode instanceof TableCell) {
            if (bStart) {
                startCell((TableCell) foNode);
            } else {
                endCell((TableCell) foNode);
            }
        } else if (foNode instanceof Leader) {
            if (bStart) {
                startLeader((Leader) foNode);
            }
        } else if (foNode instanceof PageNumberCitation) {
            if (bStart) {
                startPageNumberCitation((PageNumberCitation) foNode);
            } else {
                endPageNumberCitation((PageNumberCitation) foNode);
            }
        } else if (foNode instanceof PageNumberCitationLast) {
            if (bStart) {
                startPageNumberCitationLast((PageNumberCitationLast) foNode);
            } else {
                endPageNumberCitationLast((PageNumberCitationLast) foNode);
            }
        } else {
            final RTFEventProducer eventProducer = RTFEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.ignoredDeferredEvent(this, foNode, bStart,
                    foNode.getLocator());
        }
    }

    /**
     * Calls the event handlers for the passed FONode and all its elements.
     *
     * @param foNode
     *            FONode object which shall be recursed
     */
    private void recurseFONode(final FONode foNode) {
        invokeDeferredEvent(foNode, true);

        if (foNode instanceof PageSequence) {
            final PageSequence pageSequence = (PageSequence) foNode;

            final Region regionBefore = this.pagemaster
                    .getRegion(Constants.FO_REGION_BEFORE);
            if (regionBefore != null) {
                final FONode staticBefore = pageSequence.getFlowMap().get(
                        regionBefore.getRegionName());
                if (staticBefore != null) {
                    recurseFONode(staticBefore);
                }
            }
            final Region regionAfter = this.pagemaster
                    .getRegion(Constants.FO_REGION_AFTER);
            if (regionAfter != null) {
                final FONode staticAfter = pageSequence.getFlowMap().get(
                        regionAfter.getRegionName());
                if (staticAfter != null) {
                    recurseFONode(staticAfter);
                }
            }

            recurseFONode(pageSequence.getMainFlow());
        } else if (foNode instanceof Table) {
            final Table table = (Table) foNode;

            // recurse all table-columns
            if (table.getColumns() != null) {
                // Calculation for column-widths which are not set
                prepareTable(table);

                for (final Iterator it = table.getColumns().iterator(); it
                        .hasNext();) {
                    recurseFONode((FONode) it.next());
                }
            } else {
                // TODO Implement implicit column setup handling!
                final RTFEventProducer eventProducer = RTFEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.explicitTableColumnsRequired(this,
                        table.getLocator());
            }

            // recurse table-header
            if (table.getTableHeader() != null) {
                recurseFONode(table.getTableHeader());
            }

            // recurse table-footer
            if (table.getTableFooter() != null) {
                recurseFONode(table.getTableFooter());
            }

            if (foNode.getChildNodes() != null) {
                for (final Iterator it = foNode.getChildNodes(); it.hasNext();) {
                    recurseFONode((FONode) it.next());
                }
            }
        } else if (foNode instanceof ListItem) {
            final ListItem item = (ListItem) foNode;

            recurseFONode(item.getLabel());
            recurseFONode(item.getBody());
        } else if (foNode instanceof Footnote) {
            final Footnote fn = (Footnote) foNode;

            recurseFONode(fn.getFootnoteCitation());
            recurseFONode(fn.getFootnoteBody());
        } else {
            // Any other FO-Object: Simply recurse through all childNodes.
            if (foNode.getChildNodes() != null) {
                for (final Iterator it = foNode.getChildNodes(); it.hasNext();) {
                    final FONode fn = (FONode) it.next();
                    if (log.isTraceEnabled()) {
                        log.trace("  ChildNode for " + fn + " (" + fn.getName()
                                + ")");
                    }
                    recurseFONode(fn);
                }
            }
        }

        invokeDeferredEvent(foNode, false);
    }
}
