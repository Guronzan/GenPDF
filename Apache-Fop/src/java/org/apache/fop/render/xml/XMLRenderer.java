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

/* $Id: XMLRenderer.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.xml;

// Java
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.area.Area;
import org.apache.fop.area.AreaTreeObject;
import org.apache.fop.area.BeforeFloat;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.BodyRegion;
import org.apache.fop.area.BookmarkData;
import org.apache.fop.area.CTM;
import org.apache.fop.area.DestinationData;
import org.apache.fop.area.Footnote;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.MainReference;
import org.apache.fop.area.NormalFlow;
import org.apache.fop.area.OffDocumentExtensionAttachment;
import org.apache.fop.area.OffDocumentItem;
import org.apache.fop.area.PageSequence;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.RegionReference;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.Span;
import org.apache.fop.area.Trait;
import org.apache.fop.area.Trait.Background;
import org.apache.fop.area.Trait.InternalLink;
import org.apache.fop.area.inline.Container;
import org.apache.fop.area.inline.ForeignObject;
import org.apache.fop.area.inline.Image;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineBlockParent;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.area.inline.InlineViewport;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.Space;
import org.apache.fop.area.inline.SpaceArea;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.WordArea;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.XMLHandler;
import org.apache.fop.util.ColorUtil;
import org.apache.fop.util.XMLUtil;
import org.apache.xmlgraphics.util.QName;
import org.apache.xmlgraphics.util.XMLizable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Renderer that renders areas to XML for debugging purposes. This creates an
 * xml that contains the information of the area tree. It does not output any
 * state or derived information. The output can be used to build a new area tree
 * which can be rendered to any renderer.
 */
@Slf4j
public class XMLRenderer extends AbstractXMLRenderer {

    /**
     * Area Tree (AT) version, used to express an @version attribute in the root
     * element of the AT document, the initial value of which is set to '2.0' to
     * signify that something preceded it (but didn't happen to be marked as
     * such), and that this version is not necessarily backwards compatible with
     * the unmarked (<2.0) version.
     */
    public static final String VERSION = "2.0";

    /** XML MIME type */
    public static final String XML_MIME_TYPE = MimeConstants.MIME_FOP_AREA_TREE;

    private boolean startedSequence = false;
    private boolean compactFormat = false;

    /**
     * If not null, the XMLRenderer will mimic another renderer by using its
     * font setup.
     */
    protected Renderer mimic;

    /**
     * @param userAgent
     *            the user agent that contains configuration details. This
     *            cannot be null.
     */
    public XMLRenderer(final FOUserAgent userAgent) {
        super(userAgent);
        this.context = new RendererContext(this, XML_MIME_TYPE);
        final XMLHandler xmlHandler = new XMLXMLHandler();
        userAgent.getXMLHandlerRegistry().addXMLHandler(xmlHandler);
        final Boolean b = (Boolean) userAgent.getRendererOptions().get(
                "compact-format");
        if (b != null) {
            setCompactFormat(b.booleanValue());
        }
    }

    /**
     * Call this method to make the XMLRenderer mimic a different renderer by
     * using its font setup. This is useful when working with the intermediate
     * format parser.
     *
     * @param renderer
     *            the renderer to mimic
     */
    public void mimicRenderer(final Renderer renderer) {
        this.mimic = renderer;
    }

    /** {@inheritDoc} */
    @Override
    public void setupFontInfo(final FontInfo inFontInfo) throws FOPException {
        if (this.mimic != null) {
            this.mimic.setupFontInfo(inFontInfo);
        } else {
            super.setupFontInfo(inFontInfo);
        }
    }

    /**
     * Controls whether to create a more compact format which omit certain
     * attributes.
     *
     * @param compact
     *            true to activate the compact format
     */
    public void setCompactFormat(final boolean compact) {
        this.compactFormat = compact;
    }

    private boolean isDetailedFormat() {
        return !this.compactFormat;
    }

    /**
     * Adds the general Area attributes.
     *
     * @param area
     *            Area to extract attributes from
     */
    protected void addAreaAttributes(final Area area) {
        addAttribute("ipd", area.getIPD());
        addAttribute("bpd", area.getBPD());
        maybeAddLevelAttribute(area);
        if (isDetailedFormat()) {
            if (area.getIPD() != 0) {
                addAttribute("ipda", area.getAllocIPD());
            }
            if (area.getBPD() != 0) {
                addAttribute("bpda", area.getAllocBPD());
            }
            addAttribute(
                    "bap",
                    area.getBorderAndPaddingWidthStart() + " "
                            + area.getBorderAndPaddingWidthEnd() + " "
                            + area.getBorderAndPaddingWidthBefore() + " "
                            + area.getBorderAndPaddingWidthAfter());
        }
    }

    /**
     * Adds attributes from traits of an Area.
     *
     * @param area
     *            Area to extract traits from
     */
    protected void addTraitAttributes(final Area area) {
        final Map traitMap = area.getTraits();
        if (traitMap != null) {
            final Iterator iter = traitMap.entrySet().iterator();
            while (iter.hasNext()) {
                final Map.Entry traitEntry = (Map.Entry) iter.next();
                final Object key = traitEntry.getKey();
                final String name = Trait.getTraitName(key);
                final Class clazz = Trait.getTraitClass(key);
                if ("break-before".equals(name) || "break-after".equals(name)) {
                    continue;
                }
                final Object value = traitEntry.getValue();
                if (key == Trait.FONT) {
                    final FontTriplet triplet = (FontTriplet) value;
                    addAttribute("font-name", triplet.getName());
                    addAttribute("font-style", triplet.getStyle());
                    addAttribute("font-weight", triplet.getWeight());
                } else if (clazz.equals(InternalLink.class)) {
                    final InternalLink iLink = (InternalLink) value;
                    addAttribute(name, iLink.xmlAttribute());
                } else if (clazz.equals(Background.class)) {
                    final Background bkg = (Background) value;
                    // TODO Remove the following line (makes changes in the test
                    // checks necessary)
                    addAttribute(name, bkg.toString());
                    if (bkg.getColor() != null) {
                        addAttribute("bkg-color",
                                ColorUtil.colorToString(bkg.getColor()));
                    }
                    if (bkg.getURL() != null) {
                        addAttribute("bkg-img", bkg.getURL());
                        String repString;
                        final int repeat = bkg.getRepeat();
                        switch (repeat) {
                        case Constants.EN_REPEAT:
                            repString = "repeat";
                            break;
                        case Constants.EN_REPEATX:
                            repString = "repeat-x";
                            break;
                        case Constants.EN_REPEATY:
                            repString = "repeat-y";
                            break;
                        case Constants.EN_NOREPEAT:
                            repString = "no-repeat";
                            break;
                        default:
                            throw new IllegalStateException(
                                    "Illegal value for repeat encountered: "
                                            + repeat);
                        }
                        addAttribute("bkg-repeat", repString);
                        addAttribute("bkg-horz-offset", bkg.getHoriz());
                        addAttribute("bkg-vert-offset", bkg.getVertical());
                    }
                } else if (clazz.equals(Color.class)) {
                    final Color c = (Color) value;
                    addAttribute(name, ColorUtil.colorToString(c));
                } else if (key == Trait.START_INDENT || key == Trait.END_INDENT) {
                    if (((Integer) value).intValue() != 0) {
                        addAttribute(name, value.toString());
                    }
                } else {
                    addAttribute(name, value.toString());
                }
            }
        }

        transferForeignObjects(area);
    }

    private void transferForeignObjects(final AreaTreeObject ato) {
        final Map prefixes = new java.util.HashMap();
        Iterator iter = ato.getForeignAttributes().entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry entry = (Map.Entry) iter.next();
            final QName qname = (QName) entry.getKey();
            prefixes.put(qname.getPrefix(), qname.getNamespaceURI());
            addAttribute(qname, (String) entry.getValue());
        }
        // Namespace declarations
        iter = prefixes.entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry entry = (Map.Entry) iter.next();
            final String qn = "xmlns:" + (String) entry.getKey();
            this.atts.addAttribute("", (String) entry.getKey(), qn, CDATA,
                    (String) entry.getValue());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void processOffDocumentItem(final OffDocumentItem oDI) {
        if (oDI instanceof BookmarkData) {
            renderBookmarkTree((BookmarkData) oDI);
        } else if (oDI instanceof DestinationData) {
            renderDestination((DestinationData) oDI);
        } else if (oDI instanceof OffDocumentExtensionAttachment) {
            final ExtensionAttachment attachment = ((OffDocumentExtensionAttachment) oDI)
                    .getAttachment();
            if (this.extensionAttachments == null) {
                this.extensionAttachments = new java.util.ArrayList();
            }
            this.extensionAttachments.add(attachment);
        } else {
            final String warn = "Ignoring OffDocumentItem: " + oDI;
            log.warn(warn);
        }
    }

    /**
     * Renders a BookmarkTree object
     *
     * @param bookmarkRoot
     *            the BookmarkData object representing the top of the tree
     */
    @Override
    protected void renderBookmarkTree(final BookmarkData bookmarkRoot) {
        if (bookmarkRoot.getWhenToProcess() == OffDocumentItem.END_OF_DOC) {
            endPageSequence();
        }
        /*
         * If this kind of handling is also necessary for other renderers, then
         * better add endPageSequence to the Renderer interface and call it
         * explicitly from model.endDocument()
         */

        startElement("bookmarkTree");
        for (int i = 0; i < bookmarkRoot.getCount(); i++) {
            renderBookmarkItem(bookmarkRoot.getSubData(i));
        }
        endElement("bookmarkTree");
    }

    private void renderBookmarkItem(final BookmarkData bm) {
        this.atts.clear();
        addAttribute("title", bm.getBookmarkTitle());
        addAttribute("show-children", String.valueOf(bm.showChildItems()));
        final PageViewport pv = bm.getPageViewport();
        final String pvKey = pv == null ? null : pv.getKey();
        addAttribute("internal-link",
                InternalLink.makeXMLAttribute(pvKey, bm.getIDRef()));
        startElement("bookmark", this.atts);
        for (int i = 0; i < bm.getCount(); i++) {
            renderBookmarkItem(bm.getSubData(i));
        }
        endElement("bookmark");
    }

    /**
     * Renders a DestinationData object (named destination)
     *
     * @param destination
     *            the destination object
     */
    protected void renderDestination(final DestinationData destination) {
        if (destination.getWhenToProcess() == OffDocumentItem.END_OF_DOC) {
            endPageSequence();
        }
        this.atts.clear();
        final PageViewport pv = destination.getPageViewport();
        final String pvKey = pv == null ? null : pv.getKey();
        addAttribute("internal-link",
                InternalLink.makeXMLAttribute(pvKey, destination.getIDRef()));
        startElement("destination", this.atts);
        endElement("destination");
    }

    /** {@inheritDoc} */
    @Override
    public void startRenderer(final OutputStream outputStream)
            throws IOException {
        log.debug("Rendering areas to Area Tree XML");

        if (this.handler == null) {
            final SAXTransformerFactory factory = (SAXTransformerFactory) TransformerFactory
                    .newInstance();
            try {
                final TransformerHandler transformerHandler = factory
                        .newTransformerHandler();
                this.handler = transformerHandler;
                final StreamResult res = new StreamResult(outputStream);
                transformerHandler.setResult(res);
            } catch (final TransformerConfigurationException tce) {
                throw new RuntimeException(tce.getMessage());
            }

            this.out = outputStream;
        }

        try {
            this.handler.startDocument();
        } catch (final SAXException saxe) {
            handleSAXException(saxe);
        }
        if (this.userAgent.getProducer() != null) {
            comment("Produced by " + this.userAgent.getProducer());
        }
        this.atts.clear();
        addAttribute("version", VERSION);
        startElement("areaTree", this.atts);
    }

    /** {@inheritDoc} */
    @Override
    public void stopRenderer() throws IOException {
        endPageSequence();
        endElement("areaTree");
        try {
            this.handler.endDocument();
        } catch (final SAXException saxe) {
            handleSAXException(saxe);
        }
        if (this.out != null) {
            this.out.flush();
        }
        log.debug("Written out Area Tree XML");
    }

    /** {@inheritDoc} */
    @Override
    public void renderPage(final PageViewport page) throws IOException,
    FOPException {
        this.atts.clear();
        addAttribute("bounds", page.getViewArea());
        addAttribute("key", page.getKey());
        addAttribute("nr", page.getPageNumber());
        addAttribute("formatted-nr", page.getPageNumberString());
        if (page.getSimplePageMasterName() != null) {
            addAttribute("simple-page-master-name",
                    page.getSimplePageMasterName());
        }
        if (page.isBlank()) {
            addAttribute("blank", "true");
        }
        transferForeignObjects(page);
        startElement("pageViewport", this.atts);
        startElement("page");

        handlePageExtensionAttachments(page);
        super.renderPage(page);

        endElement("page");
        endElement("pageViewport");
    }

    /** {@inheritDoc} */
    @Override
    protected void handleExtensionAttachments(final List attachments) {
        if (attachments != null && attachments.size() > 0) {
            startElement("extension-attachments");
            final Iterator i = attachments.iterator();
            while (i.hasNext()) {
                final ExtensionAttachment attachment = (ExtensionAttachment) i
                        .next();
                if (attachment instanceof XMLizable) {
                    try {
                        ((XMLizable) attachment).toSAX(this.handler);
                    } catch (final SAXException e) {
                        log.error(
                                "Error while serializing Extension Attachment",
                                e);
                    }
                } else {
                    final String warn = "Ignoring non-XMLizable ExtensionAttachment: "
                            + attachment;
                    log.warn(warn);
                }
            }
            endElement("extension-attachments");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final PageSequence pageSequence) {
        handleDocumentExtensionAttachments();
        endPageSequence(); // move this before
        // handleDocumentExtensionAttachments() ?
        this.startedSequence = true;
        this.atts.clear();
        if (pageSequence.getLanguage() != null) {
            addAttribute("language", pageSequence.getLanguage());
        }
        if (pageSequence.getCountry() != null) {
            addAttribute("country", pageSequence.getCountry());
        }
        transferForeignObjects(pageSequence);
        startElement("pageSequence", this.atts);
        handleExtensionAttachments(pageSequence.getExtensionAttachments());
        final LineArea seqTitle = pageSequence.getTitle();
        if (seqTitle != null) {
            startElement("title");
            final List children = seqTitle.getInlineAreas();

            for (int count = 0; count < children.size(); count++) {
                final InlineArea inline = (InlineArea) children.get(count);
                renderInlineArea(inline);
            }

            endElement("title");
        }
    }

    /**
     * Tells the renderer to finish the current PageSequence
     */
    public void endPageSequence() {
        if (this.startedSequence) {
            endElement("pageSequence");
        }
        this.startedSequence = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderRegionViewport(final RegionViewport port) {
        if (port != null) {
            this.atts.clear();
            addAreaAttributes(port);
            addTraitAttributes(port);
            addAttribute("rect", port.getViewArea());
            if (port.hasClip()) {
                addAttribute("clipped", "true");
            }
            startElement("regionViewport", this.atts);
            final RegionReference region = port.getRegionReference();
            this.atts.clear();
            addAreaAttributes(region);
            addTraitAttributes(region);
            addAttribute("name", region.getRegionName());
            addAttribute("ctm", region.getCTM().toString());
            if (region.getRegionClass() == FO_REGION_BEFORE) {
                startElement("regionBefore", this.atts);
                renderRegion(region);
                endElement("regionBefore");
            } else if (region.getRegionClass() == FO_REGION_START) {
                startElement("regionStart", this.atts);
                renderRegion(region);
                endElement("regionStart");
            } else if (region.getRegionClass() == FO_REGION_BODY) {
                final BodyRegion body = (BodyRegion) region;
                if (body.getColumnCount() != 1) {
                    addAttribute("columnGap", body.getColumnGap());
                    addAttribute("columnCount", body.getColumnCount());
                }
                startElement("regionBody", this.atts);
                renderBodyRegion(body);
                endElement("regionBody");
            } else if (region.getRegionClass() == FO_REGION_END) {
                startElement("regionEnd", this.atts);
                renderRegion(region);
                endElement("regionEnd");
            } else if (region.getRegionClass() == FO_REGION_AFTER) {
                startElement("regionAfter", this.atts);
                renderRegion(region);
                endElement("regionAfter");
            }
            endElement("regionViewport");
        }
    }

    @Override
    protected void startVParea(final CTM ctm, final Rectangle clippingRect) {
        // only necessary for graphical output
    }

    /** {@inheritDoc} */
    @Override
    protected void endVParea() {
        // only necessary for graphical output
    }

    /**
     * {@inheritDoc} org.apache.fop.area.inline.InlineArea)
     */
    @Override
    protected void renderInlineAreaBackAndBorders(final InlineArea area) {
        // only necessary for graphical output
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderBeforeFloat(final BeforeFloat bf) {
        startElement("beforeFloat");
        super.renderBeforeFloat(bf);
        endElement("beforeFloat");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderFootnote(final Footnote footnote) {
        this.atts.clear();
        addAttribute("top-offset", footnote.getTop());
        startElement("footnote", this.atts);
        super.renderFootnote(footnote);
        endElement("footnote");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderMainReference(final MainReference mr) {
        this.atts.clear();
        addAreaAttributes(mr);
        addTraitAttributes(mr);
        if (mr.getColumnCount() != 1) {
            addAttribute("columnGap", mr.getColumnGap());
        }
        startElement("mainReference", this.atts);

        Span span = null;
        final List spans = mr.getSpans();
        for (int count = 0; count < spans.size(); count++) {
            span = (Span) spans.get(count);
            this.atts.clear();
            if (span.getColumnCount() != 1) {
                addAttribute("columnCount", span.getColumnCount());
            }
            addAreaAttributes(span);
            addTraitAttributes(span);
            startElement("span", this.atts);
            for (int c = 0; c < span.getColumnCount(); c++) {
                final NormalFlow flow = span.getNormalFlow(c);

                renderFlow(flow);
            }
            endElement("span");
        }
        endElement("mainReference");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderFlow(final NormalFlow flow) {
        // the normal flow reference area contains stacked blocks
        this.atts.clear();
        addAreaAttributes(flow);
        addTraitAttributes(flow);
        startElement("flow", this.atts);
        super.renderFlow(flow);
        endElement("flow");
    }

    /** {@inheritDoc} */
    @Override
    protected void renderReferenceArea(final Block block) {
        handleBlockTraits(block);

        final List children = block.getChildAreas();
        if (children != null) {
            renderBlocks(block, children);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void renderBlock(final Block block) {
        this.atts.clear();
        addAreaAttributes(block);
        addTraitAttributes(block);
        final int positioning = block.getPositioning();
        if (block instanceof BlockViewport) {
            final BlockViewport bvp = (BlockViewport) block;
            boolean abspos = false;
            if (bvp.getPositioning() == Block.ABSOLUTE
                    || bvp.getPositioning() == Block.FIXED) {
                abspos = true;
            }
            if (abspos) {
                addAttribute("left-position", bvp.getXOffset());
                addAttribute("top-position", bvp.getYOffset());
            }
            addAttribute("ctm", bvp.getCTM().toString());
            if (bvp.hasClip()) {
                addAttribute("clipped", "true");
            }
        } else {
            if (block.getXOffset() != 0) {
                addAttribute("left-offset", block.getXOffset());
            }
            if (block.getYOffset() != 0) {
                addAttribute("top-offset", block.getYOffset());
            }
        }
        switch (positioning) {
        case Block.RELATIVE:
            addAttribute("positioning", "relative");
            break;
        case Block.ABSOLUTE:
            addAttribute("positioning", "absolute");
            break;
        case Block.FIXED:
            addAttribute("positioning", "fixed");
            break;
        default: // nop
        }
        startElement("block", this.atts);
        super.renderBlock(block);
        endElement("block");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderLineArea(final LineArea line) {
        this.atts.clear();
        addAreaAttributes(line);
        addTraitAttributes(line);
        startElement("lineArea", this.atts);
        super.renderLineArea(line);
        endElement("lineArea");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderInlineArea(final InlineArea inlineArea) {
        this.atts.clear();
        if (inlineArea.getClass() == InlineArea.class) {
            // Generic inline area. This is implemented to allow the 0x0 "dummy"
            // area generated by fo:wrapper to pass its id.
            addAreaAttributes(inlineArea);
            addTraitAttributes(inlineArea);
            startElement("inline", this.atts);
            endElement("inline");
        } else {
            super.renderInlineArea(inlineArea);
            // calls specific renderers for Text, Space, Viewport, etc. etc.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderInlineViewport(final InlineViewport viewport) {
        this.atts.clear();
        addAreaAttributes(viewport);
        addTraitAttributes(viewport);
        addAttribute("offset", viewport.getBlockProgressionOffset());
        addAttribute("pos", viewport.getContentPosition());
        if (viewport.hasClip()) {
            addAttribute("clip", "true");
        }
        startElement("viewport", this.atts);
        super.renderInlineViewport(viewport);
        endElement("viewport");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderImage(final Image image, final Rectangle2D pos) {
        this.atts.clear();
        addAreaAttributes(image);
        addTraitAttributes(image);
        addAttribute("url", image.getURL());
        // addAttribute("pos", pos);
        startElement("image", this.atts);
        endElement("image");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderContainer(final Container cont) {
        startElement("container");
        super.renderContainer(cont);
        endElement("container");
    }

    /**
     * Renders an fo:foreing-object.
     *
     * @param fo
     *            the foreign object
     * @param pos
     *            the position of the foreign object
     * @see org.apache.fop.render.AbstractRenderer#renderForeignObject(ForeignObject,
     *      Rectangle2D)
     */
    @Override
    public void renderForeignObject(final ForeignObject fo,
            final Rectangle2D pos) {
        this.atts.clear();
        addAreaAttributes(fo);
        addTraitAttributes(fo);
        final String ns = fo.getNameSpace();
        addAttribute("ns", ns);
        startElement("foreignObject", this.atts);
        final Document doc = fo.getDocument();
        this.context.setProperty(XMLXMLHandler.HANDLER, this.handler);
        renderXML(this.context, doc, ns);
        endElement("foreignObject");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderInlineSpace(final Space space) {
        this.atts.clear();
        addAreaAttributes(space);
        addTraitAttributes(space);
        addAttribute("offset", space.getBlockProgressionOffset());
        startElement("space", this.atts);
        endElement("space");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderText(final TextArea text) {
        this.atts.clear();
        if (text.getTextWordSpaceAdjust() != 0) {
            addAttribute("twsadjust", text.getTextWordSpaceAdjust());
        }
        if (text.getTextLetterSpaceAdjust() != 0) {
            addAttribute("tlsadjust", text.getTextLetterSpaceAdjust());
        }
        addAttribute("offset", text.getBlockProgressionOffset());
        addAttribute("baseline", text.getBaselineOffset());
        addAreaAttributes(text);
        addTraitAttributes(text);
        startElement("text", this.atts);
        super.renderText(text);
        endElement("text");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderWord(final WordArea word) {
        this.atts.clear();
        final int offset = word.getBlockProgressionOffset();
        if (offset != 0) {
            addAttribute("offset", offset);
        }
        final int[] letterAdjust = word.getLetterAdjustArray();
        if (letterAdjust != null) {
            final StringBuffer sb = new StringBuffer(64);
            boolean nonZeroFound = false;
            for (int i = 0, c = letterAdjust.length; i < c; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(letterAdjust[i]);
                nonZeroFound |= letterAdjust[i] != 0;
            }
            if (nonZeroFound) {
                addAttribute("letter-adjust", sb.toString());
            }
        }
        maybeAddLevelAttribute(word);
        maybeAddPositionAdjustAttribute(word);
        final String text = word.getWord();
        maybeAddReversedAttribute(word, text);
        startElement("word", this.atts);
        characters(text);
        endElement("word");
        super.renderWord(word);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderSpace(final SpaceArea space) {
        this.atts.clear();
        final int offset = space.getBlockProgressionOffset();
        if (offset != 0) {
            addAttribute("offset", offset);
        }
        maybeAddLevelAttribute(space);
        if (!space.isAdjustable()) {
            addAttribute("adj", "false"); // default is true
        }
        startElement("space", this.atts);
        characters(space.getSpace());
        endElement("space");
        super.renderSpace(space);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderInlineParent(final InlineParent ip) {
        this.atts.clear();
        addAreaAttributes(ip);
        addTraitAttributes(ip);
        addAttribute("offset", ip.getBlockProgressionOffset());
        startElement("inlineparent", this.atts);
        super.renderInlineParent(ip);
        endElement("inlineparent");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderInlineBlockParent(final InlineBlockParent ibp) {
        this.atts.clear();
        addAreaAttributes(ibp);
        addTraitAttributes(ibp);
        addAttribute("offset", ibp.getBlockProgressionOffset());
        startElement("inlineblockparent", this.atts);
        super.renderInlineBlockParent(ibp);
        endElement("inlineblockparent");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderLeader(final Leader area) {
        this.atts.clear();
        addAreaAttributes(area);
        addTraitAttributes(area);
        addAttribute("offset", area.getBlockProgressionOffset());
        addAttribute("ruleStyle", area.getRuleStyleAsString());
        addAttribute("ruleThickness", area.getRuleThickness());
        startElement("leader", this.atts);
        endElement("leader");
        super.renderLeader(area);
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return XML_MIME_TYPE;
    }

    private void maybeAddLevelAttribute(final Area a) {
        final int level = a.getBidiLevel();
        if (level >= 0) {
            addAttribute("level", level);
        }
    }

    private void maybeAddPositionAdjustAttribute(final WordArea w) {
        final int[][] adjustments = w.getGlyphPositionAdjustments();
        if (adjustments != null) {
            addAttribute("position-adjust",
                    XMLUtil.encodePositionAdjustments(adjustments));
        }
    }

    private void maybeAddReversedAttribute(final WordArea w, final String text) {
        if (w.isReversed() && text.length() > 1) {
            addAttribute("reversed", "true");
        }
    }

}
