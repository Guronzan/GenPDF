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

/* $Id: AreaTreeParser.java 1296405 2012-03-02 19:36:45Z gadams $ */

package org.apache.fop.area;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.ResourceEventProducer;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.Trait.Background;
import org.apache.fop.area.Trait.InternalLink;
import org.apache.fop.area.inline.AbstractTextArea;
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
import org.apache.fop.fo.ElementMappingRegistry;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.util.ColorUtil;
import org.apache.fop.util.ContentHandlerFactory;
import org.apache.fop.util.ContentHandlerFactoryRegistry;
import org.apache.fop.util.ConversionUtils;
import org.apache.fop.util.DefaultErrorListener;
import org.apache.fop.util.XMLConstants;
import org.apache.fop.util.XMLUtil;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.util.QName;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import static org.apache.fop.fo.Constants.FO_REGION_AFTER;
import static org.apache.fop.fo.Constants.FO_REGION_BEFORE;
import static org.apache.fop.fo.Constants.FO_REGION_BODY;
import static org.apache.fop.fo.Constants.FO_REGION_END;
import static org.apache.fop.fo.Constants.FO_REGION_START;

/**
 * This is a parser for the area tree XML (intermediate format) which is used to
 * reread an area tree (or part of it) into memory again for rendering to the
 * final output format.
 */
@Slf4j
public class AreaTreeParser {

    private static SAXTransformerFactory tFactory = (SAXTransformerFactory) SAXTransformerFactory
            .newInstance();

    /**
     * Parses an intermediate file (area tree XML) into an AreaTreeModel
     * instance by adding pages to it.
     *
     * @param src
     *            the Source instance pointing to the intermediate file
     * @param treeModel
     *            the AreaTreeModel that the parsed pages are added to
     * @param userAgent
     *            the user agent
     * @throws TransformerException
     *             if an error occurs while parsing the area tree XML
     */
    public void parse(final Source src, final AreaTreeModel treeModel,
            final FOUserAgent userAgent) throws TransformerException {
        final Transformer transformer = tFactory.newTransformer();
        transformer.setErrorListener(new DefaultErrorListener(log));

        final SAXResult res = new SAXResult(getContentHandler(treeModel,
                userAgent));

        transformer.transform(src, res);
    }

    /**
     * Creates a new ContentHandler instance that you can send the area tree XML
     * to. The parsed pages are added to the AreaTreeModel instance you pass in
     * as a parameter.
     *
     * @param treeModel
     *            the AreaTreeModel that the parsed pages are added to
     * @param userAgent
     *            the user agent
     * @return the ContentHandler instance to receive the SAX stream from the
     *         area tree XML
     */
    public ContentHandler getContentHandler(final AreaTreeModel treeModel,
            final FOUserAgent userAgent) {
        final ElementMappingRegistry elementMappingRegistry = userAgent
                .getFactory().getElementMappingRegistry();
        return new Handler(treeModel, userAgent, elementMappingRegistry);
    }

    private static class Handler extends DefaultHandler {

        private final Map<String, AbstractMaker> makers = new java.util.HashMap<String, AbstractMaker>();

        private final AreaTreeModel treeModel;
        private final FOUserAgent userAgent;
        private final ElementMappingRegistry elementMappingRegistry;

        private Attributes lastAttributes;

        private CharBuffer content = CharBuffer.allocate(64);
        private boolean ignoreCharacters = true;

        private PageViewport currentPageViewport;
        private final Map<String, PageViewport> pageViewportsByKey = new java.util.HashMap<String, PageViewport>();
        // set of "ID firsts" that have already been assigned to a PV:
        private final Set<String> idFirstsAssigned = new java.util.HashSet<String>();

        private final Stack<Object> areaStack = new Stack<Object>();
        private boolean firstFlow;

        private final Stack<String> delegateStack = new Stack<String>();
        private ContentHandler delegate;
        private DOMImplementation domImplementation;
        private Locator locator;

        public Handler(final AreaTreeModel treeModel,
                final FOUserAgent userAgent,
                final ElementMappingRegistry elementMappingRegistry) {
            this.treeModel = treeModel;
            this.userAgent = userAgent;
            this.elementMappingRegistry = elementMappingRegistry;
            this.makers.put("areaTree", new AreaTreeMaker());
            this.makers.put("page", new PageMaker());
            this.makers.put("pageSequence", new PageSequenceMaker());
            this.makers.put("title", new TitleMaker());
            this.makers.put("pageViewport", new PageViewportMaker());
            this.makers.put("regionViewport", new RegionViewportMaker());
            this.makers.put("regionBefore", new RegionBeforeMaker());
            this.makers.put("regionAfter", new RegionAfterMaker());
            this.makers.put("regionStart", new RegionStartMaker());
            this.makers.put("regionEnd", new RegionEndMaker());
            this.makers.put("regionBody", new RegionBodyMaker());
            this.makers.put("flow", new FlowMaker());
            this.makers.put("mainReference", new MainReferenceMaker());
            this.makers.put("span", new SpanMaker());
            this.makers.put("footnote", new FootnoteMaker());
            this.makers.put("beforeFloat", new BeforeFloatMaker());
            this.makers.put("block", new BlockMaker());
            this.makers.put("lineArea", new LineAreaMaker());
            this.makers.put("inline", new InlineMaker());
            this.makers.put("inlineparent", new InlineParentMaker());
            this.makers.put("inlineblockparent", new InlineBlockParentMaker());
            this.makers.put("text", new TextMaker());
            this.makers.put("word", new WordMaker());
            this.makers.put("space", new SpaceMaker());
            this.makers.put("leader", new LeaderMaker());
            this.makers.put("viewport", new InlineViewportMaker());
            this.makers.put("image", new ImageMaker());
            this.makers.put("foreignObject", new ForeignObjectMaker());
            this.makers.put("bookmarkTree", new BookmarkTreeMaker());
            this.makers.put("bookmark", new BookmarkMaker());
            this.makers.put("destination", new DestinationMaker());
        }

        private Area findAreaType(final Class clazz) {
            if (this.areaStack.size() > 0) {
                int pos = this.areaStack.size() - 1;
                Object obj = null;
                while (pos >= 0) {
                    obj = this.areaStack.get(pos);
                    if (clazz.isInstance(obj)) {
                        break;
                    } else {
                        pos--;
                    }
                }
                if (pos >= 0) {
                    return (Area) obj;
                }
            }
            return null;
        }

        private RegionViewport getCurrentRegionViewport() {
            return (RegionViewport) findAreaType(RegionViewport.class);
        }

        private BodyRegion getCurrentBodyRegion() {
            return (BodyRegion) findAreaType(BodyRegion.class);
        }

        private BlockParent getCurrentBlockParent() {
            return (BlockParent) findAreaType(BlockParent.class);
        }

        private AbstractTextArea getCurrentText() {
            return (AbstractTextArea) findAreaType(AbstractTextArea.class);
        }

        private InlineViewport getCurrentViewport() {
            return (InlineViewport) findAreaType(InlineViewport.class);
        }

        /** {@inheritDoc} */
        @Override
        public void setDocumentLocator(final Locator locator) {
            this.locator = locator;
        }

        private Locator getLocator() {
            return this.locator;
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final String uri, final String localName,
                final String qName, final Attributes attributes)
                        throws SAXException {
            if (this.delegate != null) {
                this.delegateStack.push(qName);
                this.delegate.startElement(uri, localName, qName, attributes);
            } else if (this.domImplementation != null) {
                // domImplementation is set so we need to start a new DOM
                // building sub-process
                TransformerHandler handler;
                try {
                    handler = tFactory.newTransformerHandler();
                } catch (final TransformerConfigurationException e) {
                    throw new SAXException(
                            "Error creating a new TransformerHandler", e);
                }
                final Document doc = this.domImplementation.createDocument(uri,
                        qName, null);
                // It's easier to work with an empty document, so remove the
                // root element
                doc.removeChild(doc.getDocumentElement());
                handler.setResult(new DOMResult(doc));
                final Area parent = (Area) this.areaStack.peek();
                ((ForeignObject) parent).setDocument(doc);

                // activate delegate for nested foreign document
                this.domImplementation = null; // Not needed anymore now
                this.delegate = handler;
                this.delegateStack.push(qName);
                this.delegate.startDocument();
                this.delegate.startElement(uri, localName, qName, attributes);
            } else {
                boolean handled = true;
                if ("".equals(uri)) {
                    if (localName.equals("structureTree")) {

                        /*
                         * The area tree parser no longer supports the structure
                         * tree.
                         */
                        this.delegate = new DefaultHandler();

                        this.delegateStack.push(qName);
                        this.delegate.startDocument();
                        this.delegate.startElement(uri, localName, qName,
                                attributes);
                    } else {
                        handled = startAreaTreeElement(localName, attributes);
                    }
                } else {
                    final ContentHandlerFactoryRegistry registry = this.userAgent
                            .getFactory().getContentHandlerFactoryRegistry();
                    final ContentHandlerFactory factory = registry
                            .getFactory(uri);
                    if (factory != null) {
                        this.delegate = factory.createContentHandler();
                        this.delegateStack.push(qName);
                        this.delegate.startDocument();
                        this.delegate.startElement(uri, localName, qName,
                                attributes);
                    } else {
                        handled = false;
                    }
                }
                if (!handled) {
                    if (uri == null || uri.length() == 0) {
                        throw new SAXException("Unhandled element " + localName
                                + " in namespace: " + uri);
                    } else {
                        log.warn("Unhandled element " + localName
                                + " in namespace: " + uri);
                    }
                }
            }
        }

        private boolean startAreaTreeElement(final String localName,
                final Attributes attributes) throws SAXException {
            this.lastAttributes = new AttributesImpl(attributes);
            final Maker maker = this.makers.get(localName);
            this.content.clear();
            this.ignoreCharacters = true;
            if (maker != null) {
                this.ignoreCharacters = maker.ignoreCharacters();
                maker.startElement(attributes);
            } else if ("extension-attachments".equals(localName)) {
                // TODO implement me
            } else {
                return false;
            }
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName,
                final String qName) throws SAXException {
            if (this.delegate != null) {
                this.delegate.endElement(uri, localName, qName);
                this.delegateStack.pop();
                if (this.delegateStack.size() == 0) {
                    this.delegate.endDocument();
                    if (this.delegate instanceof ContentHandlerFactory.ObjectSource) {
                        final Object obj = ((ContentHandlerFactory.ObjectSource) this.delegate)
                                .getObject();
                        handleExternallyGeneratedObject(obj);
                    }
                    this.delegate = null; // Sub-document is processed, return
                    // to normal processing
                }
            } else {
                if ("".equals(uri)) {
                    final Maker maker = this.makers.get(localName);
                    if (maker != null) {
                        maker.endElement();
                        this.content.clear();
                    }
                    this.ignoreCharacters = true;
                } else {
                    // log.debug("Ignoring " + localName + " in namespace: " +
                    // uri);
                }
            }
        }

        // ============== Maker classes for the area tree objects =============

        private interface Maker {
            void startElement(final Attributes attributes) throws SAXException;

            void endElement();

            boolean ignoreCharacters();
        }

        private abstract class AbstractMaker implements Maker {

            @Override
            public void startElement(final Attributes attributes)
                    throws SAXException {
                // nop
            }

            @Override
            public void endElement() {
                // nop
            }

            @Override
            public boolean ignoreCharacters() {
                return true;
            }
        }

        private class AreaTreeMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                // In case the Handler is reused:
                Handler.this.idFirstsAssigned.clear();
            }
        }

        private class PageSequenceMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final PageSequence pageSequence = new PageSequence(null);
                final String lang = attributes.getValue("language");
                pageSequence.setLanguage(lang);
                final String country = attributes.getValue("country");
                pageSequence.setCountry(country);
                transferForeignObjects(attributes, pageSequence);
                Handler.this.areaStack.push(pageSequence);
            }
        }

        private class TitleMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final LineArea line = new LineArea();
                transferForeignObjects(attributes, line);
                Handler.this.areaStack.push(line);
            }

            @Override
            public void endElement() {
                final LineArea line = (LineArea) Handler.this.areaStack.pop();
                final PageSequence pageSequence = (PageSequence) Handler.this.areaStack
                        .peek();
                pageSequence.setTitle(line);
            }

        }

        private class PageViewportMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                if (!Handler.this.areaStack.isEmpty()) {
                    final PageSequence pageSequence = (PageSequence) Handler.this.areaStack
                            .peek();
                    Handler.this.treeModel.startPageSequence(pageSequence);
                    Handler.this.areaStack.pop();
                }
                if (Handler.this.currentPageViewport != null) {
                    throw new IllegalStateException(
                            "currentPageViewport must be null");
                }
                final Rectangle viewArea = XMLUtil.getAttributeAsRectangle(
                        attributes, "bounds");
                final int pageNumber = XMLUtil.getAttributeAsInt(attributes,
                        "nr", -1);
                final String key = attributes.getValue("key");
                final String pageNumberString = attributes
                        .getValue("formatted-nr");
                final String pageMaster = attributes
                        .getValue("simple-page-master-name");
                final boolean blank = XMLUtil.getAttributeAsBoolean(attributes,
                        "blank", false);
                Handler.this.currentPageViewport = new PageViewport(viewArea,
                        pageNumber, pageNumberString, pageMaster, blank);
                transferForeignObjects(attributes,
                        Handler.this.currentPageViewport);
                Handler.this.currentPageViewport.setKey(key);
                Handler.this.pageViewportsByKey.put(key,
                        Handler.this.currentPageViewport);
            }

        }

        private class PageMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final Page p = new Page();
                Handler.this.currentPageViewport.setPage(p);
            }

            @Override
            public void endElement() {
                Handler.this.treeModel
                .addPage(Handler.this.currentPageViewport);
                Handler.this.currentPageViewport = null;
            }
        }

        private class RegionViewportMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                RegionViewport rv = getCurrentRegionViewport();
                if (rv != null) {
                    throw new IllegalStateException(
                            "Current RegionViewport must be null");
                }
                final Rectangle2D viewArea = XMLUtil.getAttributeAsRectangle2D(
                        attributes, "rect");
                rv = new RegionViewport(viewArea);
                transferForeignObjects(attributes, rv);
                rv.setClip(XMLUtil.getAttributeAsBoolean(attributes, "clipped",
                        false));
                setAreaAttributes(attributes, rv);
                setTraits(attributes, rv, SUBSET_COMMON);
                setTraits(attributes, rv, SUBSET_BOX);
                setTraits(attributes, rv, SUBSET_COLOR);
                Handler.this.areaStack.push(rv);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        RegionViewport.class);
            }
        }

        private class RegionBeforeMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                pushNewRegionReference(attributes, FO_REGION_BEFORE);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        RegionReference.class);
            }
        }

        private class RegionAfterMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                pushNewRegionReference(attributes, FO_REGION_AFTER);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        RegionReference.class);
            }
        }

        private class RegionStartMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                pushNewRegionReference(attributes, FO_REGION_START);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        RegionReference.class);
            }
        }

        private class RegionEndMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                pushNewRegionReference(attributes, FO_REGION_END);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        RegionReference.class);
            }
        }

        private class RegionBodyMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                BodyRegion body = getCurrentBodyRegion();
                if (body != null) {
                    throw new IllegalStateException(
                            "Current BodyRegion must be null");
                }
                final String regionName = attributes.getValue("name");
                final int columnCount = XMLUtil.getAttributeAsInt(attributes,
                        "columnCount", 1);
                final int columnGap = XMLUtil.getAttributeAsInt(attributes,
                        "columnGap", 0);
                final RegionViewport rv = getCurrentRegionViewport();
                body = new BodyRegion(FO_REGION_BODY, regionName, rv,
                        columnCount, columnGap);
                transferForeignObjects(attributes, body);
                body.setCTM(getAttributeAsCTM(attributes, "ctm"));
                setAreaAttributes(attributes, body);
                setTraits(attributes, body, SUBSET_BORDER_PADDING);
                rv.setRegionReference(body);
                Handler.this.currentPageViewport.getPage().setRegionViewport(
                        FO_REGION_BODY, rv);
                Handler.this.areaStack.push(body);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        BodyRegion.class);
            }
        }

        private class FlowMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final BodyRegion body = getCurrentBodyRegion();
                if (!Handler.this.firstFlow) {
                    body.getMainReference().getCurrentSpan().moveToNextFlow();
                } else {
                    Handler.this.firstFlow = false;
                }
                final NormalFlow flow = body.getMainReference()
                        .getCurrentSpan().getCurrentFlow();
                transferForeignObjects(attributes, flow);
                setAreaAttributes(attributes, flow);
                Handler.this.areaStack.push(flow);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        NormalFlow.class);
            }
        }

        private class MainReferenceMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                // mainReference is created by the BodyRegion
                final MainReference mr = getCurrentBodyRegion()
                        .getMainReference();
                transferForeignObjects(attributes, mr);
                setAreaAttributes(attributes, mr);
            }
        }

        private class SpanMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final int ipd = XMLUtil.getAttributeAsInt(attributes, "ipd", 0);
                final int columnCount = XMLUtil.getAttributeAsInt(attributes,
                        "columnCount", 1);
                final BodyRegion body = getCurrentBodyRegion();
                final Span span = new Span(columnCount, body.getColumnGap(),
                        ipd);
                transferForeignObjects(attributes, span);
                setAreaAttributes(attributes, span);
                body.getMainReference().getSpans().add(span);
                Handler.this.firstFlow = true;
            }
        }

        private class FootnoteMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final Footnote fn = getCurrentBodyRegion().getFootnote();
                transferForeignObjects(attributes, fn);
                fn.setTop(XMLUtil
                        .getAttributeAsInt(attributes, "top-offset", 0));
                Handler.this.areaStack.push(fn);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        Footnote.class);
            }
        }

        private class BeforeFloatMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final BeforeFloat bf = getCurrentBodyRegion().getBeforeFloat();
                transferForeignObjects(attributes, bf);
                Handler.this.areaStack.push(bf);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        BeforeFloat.class);
            }
        }

        private class BlockMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final boolean isViewport = XMLUtil.getAttributeAsBoolean(
                        attributes, "is-viewport-area", false);
                Block block;
                if (isViewport) {
                    final BlockViewport bv = new BlockViewport();
                    bv.setClip(XMLUtil.getAttributeAsBoolean(attributes,
                            "clipped", false));
                    bv.setCTM(getAttributeAsCTM(attributes, "ctm"));
                    if (bv.getPositioning() != Block.RELATIVE) {
                        bv.setXOffset(XMLUtil.getAttributeAsInt(attributes,
                                "left-position", 0));
                        bv.setYOffset(XMLUtil.getAttributeAsInt(attributes,
                                "top-position", 0));
                    }
                    block = bv;
                } else {
                    block = new Block();
                }
                final String positioning = attributes.getValue("positioning");
                if ("absolute".equalsIgnoreCase(positioning)) {
                    block.setPositioning(Block.ABSOLUTE);
                } else if ("fixed".equalsIgnoreCase(positioning)) {
                    block.setPositioning(Block.FIXED);
                } else if ("relative".equalsIgnoreCase(positioning)) {
                    block.setPositioning(Block.RELATIVE);
                } else {
                    block.setPositioning(Block.STACK);
                }
                if (attributes.getValue("left-offset") != null) {
                    block.setXOffset(XMLUtil.getAttributeAsInt(attributes,
                            "left-offset", 0));
                }
                if (attributes.getValue("top-offset") != null) {
                    block.setYOffset(XMLUtil.getAttributeAsInt(attributes,
                            "top-offset", 0));
                }
                transferForeignObjects(attributes, block);
                setAreaAttributes(attributes, block);
                setTraits(attributes, block, SUBSET_COMMON);
                setTraits(attributes, block, SUBSET_BOX);
                setTraits(attributes, block, SUBSET_COLOR);
                final Area parent = (Area) Handler.this.areaStack.peek();
                // BlockParent parent = getCurrentBlockParent();
                parent.addChildArea(block);
                Handler.this.areaStack.push(block);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(), Block.class);
            }
        }

        private class LineAreaMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final LineArea line = new LineArea();
                setAreaAttributes(attributes, line);
                setTraits(attributes, line, SUBSET_COMMON);
                setTraits(attributes, line, SUBSET_BOX);
                setTraits(attributes, line, SUBSET_COLOR);
                final BlockParent parent = getCurrentBlockParent();
                parent.addChildArea(line);
                Handler.this.areaStack.push(line);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        LineArea.class);
            }
        }

        // Maker for "generic" inline areas
        private class InlineMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final InlineArea inl = new InlineArea();
                transferForeignObjects(attributes, inl);
                inl.setBlockProgressionOffset(XMLUtil.getAttributeAsInt(
                        attributes, "offset", 0));
                setAreaAttributes(attributes, inl);
                setTraits(attributes, inl, SUBSET_COMMON);
                setTraits(attributes, inl, SUBSET_BOX);
                setTraits(attributes, inl, SUBSET_COLOR);
                final Area parent = (Area) Handler.this.areaStack.peek();
                parent.addChildArea(inl);
                Handler.this.areaStack.push(inl);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        InlineArea.class);
            }
        }

        private class InlineParentMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final InlineParent ip = new InlineParent();
                transferForeignObjects(attributes, ip);
                ip.setBlockProgressionOffset(XMLUtil.getAttributeAsInt(
                        attributes, "offset", 0));
                setAreaAttributes(attributes, ip);
                setTraits(attributes, ip, SUBSET_COMMON);
                setTraits(attributes, ip, SUBSET_BOX);
                setTraits(attributes, ip, SUBSET_COLOR);
                setTraits(attributes, ip, SUBSET_LINK);
                final Area parent = (Area) Handler.this.areaStack.peek();
                parent.addChildArea(ip);
                Handler.this.areaStack.push(ip);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        InlineParent.class);
            }
        }

        private class InlineBlockParentMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final InlineBlockParent ibp = new InlineBlockParent();
                transferForeignObjects(attributes, ibp);
                ibp.setBlockProgressionOffset(XMLUtil.getAttributeAsInt(
                        attributes, "offset", 0));
                setAreaAttributes(attributes, ibp);
                setTraits(attributes, ibp, SUBSET_COMMON);
                setTraits(attributes, ibp, SUBSET_BOX);
                setTraits(attributes, ibp, SUBSET_COLOR);
                final Area parent = (Area) Handler.this.areaStack.peek();
                parent.addChildArea(ibp);
                Handler.this.areaStack.push(ibp);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        InlineBlockParent.class);
            }
        }

        private class TextMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                if (getCurrentText() != null) {
                    throw new IllegalStateException("Current Text must be null");
                }
                final TextArea text = new TextArea();
                setAreaAttributes(attributes, text);
                setTraits(attributes, text, SUBSET_COMMON);
                setTraits(attributes, text, SUBSET_BOX);
                setTraits(attributes, text, SUBSET_COLOR);
                setTraits(attributes, text, SUBSET_FONT);
                text.setBaselineOffset(XMLUtil.getAttributeAsInt(attributes,
                        "baseline", 0));
                text.setBlockProgressionOffset(XMLUtil.getAttributeAsInt(
                        attributes, "offset", 0));
                text.setTextLetterSpaceAdjust(XMLUtil.getAttributeAsInt(
                        attributes, "tlsadjust", 0));
                text.setTextWordSpaceAdjust(XMLUtil.getAttributeAsInt(
                        attributes, "twsadjust", 0));
                final Area parent = (Area) Handler.this.areaStack.peek();
                parent.addChildArea(text);
                Handler.this.areaStack.push(text);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        TextArea.class);
            }
        }

        private class WordMaker extends AbstractMaker {

            @Override
            public void endElement() {
                final int offset = XMLUtil.getAttributeAsInt(
                        Handler.this.lastAttributes, "offset", 0);
                final int[] letterAdjust = ConversionUtils.toIntArray(
                        Handler.this.lastAttributes.getValue("letter-adjust"),
                        "\\s");
                final int level = XMLUtil.getAttributeAsInt(
                        Handler.this.lastAttributes, "level", -1);
                final boolean reversed = XMLUtil.getAttributeAsBoolean(
                        Handler.this.lastAttributes, "reversed", false);
                final int[][] gposAdjustments = XMLUtil
                        .getAttributeAsPositionAdjustments(
                                Handler.this.lastAttributes, "position-adjust");
                Handler.this.content.flip();
                final WordArea word = new WordArea(offset, level,
                        Handler.this.content.toString().trim(), letterAdjust,
                        null, gposAdjustments, reversed);
                final AbstractTextArea text = getCurrentText();
                word.setParentArea(text);
                text.addChildArea(word);
            }

            @Override
            public boolean ignoreCharacters() {
                return false;
            }
        }

        private class SpaceMaker extends AbstractMaker {

            @Override
            public void endElement() {
                final int offset = XMLUtil.getAttributeAsInt(
                        Handler.this.lastAttributes, "offset", 0);
                // TODO the isAdjustable parameter is currently not
                // used/implemented
                if (Handler.this.content.position() > 0) {
                    Handler.this.content.flip();
                    final boolean adjustable = XMLUtil.getAttributeAsBoolean(
                            Handler.this.lastAttributes, "adj", true);
                    final int level = XMLUtil.getAttributeAsInt(
                            Handler.this.lastAttributes, "level", -1);
                    final SpaceArea space = new SpaceArea(offset, level,
                            Handler.this.content.charAt(0), adjustable);
                    final AbstractTextArea text = getCurrentText();
                    space.setParentArea(text);
                    text.addChildArea(space);
                } else {
                    final Space space = new Space();
                    setAreaAttributes(Handler.this.lastAttributes, space);
                    setTraits(Handler.this.lastAttributes, space, SUBSET_COMMON);
                    setTraits(Handler.this.lastAttributes, space, SUBSET_BOX);
                    setTraits(Handler.this.lastAttributes, space, SUBSET_COLOR);
                    space.setBlockProgressionOffset(offset);
                    final Area parent = (Area) Handler.this.areaStack.peek();
                    parent.addChildArea(space);
                }
            }

            @Override
            public boolean ignoreCharacters() {
                return false;
            }
        }

        private class LeaderMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final Leader leader = new Leader();
                transferForeignObjects(attributes, leader);
                setAreaAttributes(attributes, leader);
                setTraits(attributes, leader, SUBSET_COMMON);
                setTraits(attributes, leader, SUBSET_BOX);
                setTraits(attributes, leader, SUBSET_COLOR);
                setTraits(attributes, leader, SUBSET_FONT);
                leader.setBlockProgressionOffset(XMLUtil.getAttributeAsInt(
                        attributes, "offset", 0));
                final String ruleStyle = attributes.getValue("ruleStyle");
                if (ruleStyle != null) {
                    leader.setRuleStyle(ruleStyle);
                }
                leader.setRuleThickness(XMLUtil.getAttributeAsInt(attributes,
                        "ruleThickness", 0));
                final Area parent = (Area) Handler.this.areaStack.peek();
                parent.addChildArea(leader);
            }
        }

        private class InlineViewportMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final int level = XMLUtil.getAttributeAsInt(attributes,
                        "level", -1);
                final InlineViewport viewport = new InlineViewport(null, level);
                transferForeignObjects(attributes, viewport);
                setAreaAttributes(attributes, viewport);
                setTraits(attributes, viewport, SUBSET_COMMON);
                setTraits(attributes, viewport, SUBSET_BOX);
                setTraits(attributes, viewport, SUBSET_COLOR);
                viewport.setContentPosition(XMLUtil.getAttributeAsRectangle2D(
                        attributes, "pos"));
                viewport.setClip(XMLUtil.getAttributeAsBoolean(attributes,
                        "clip", false));
                viewport.setBlockProgressionOffset(XMLUtil.getAttributeAsInt(
                        attributes, "offset", 0));
                final Area parent = (Area) Handler.this.areaStack.peek();
                parent.addChildArea(viewport);
                Handler.this.areaStack.push(viewport);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        InlineViewport.class);
            }
        }

        private class ImageMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final String url = attributes.getValue("url");
                final Image image = new Image(url);
                transferForeignObjects(attributes, image);
                setAreaAttributes(attributes, image);
                setTraits(attributes, image, SUBSET_COMMON);
                getCurrentViewport().setContent(image);
            }
        }

        private class ForeignObjectMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes)
                    throws SAXException {
                final String ns = attributes.getValue("ns");
                Handler.this.domImplementation = Handler.this.elementMappingRegistry
                        .getDOMImplementationForNamespace(ns);
                if (Handler.this.domImplementation == null) {
                    throw new SAXException("No DOMImplementation could be"
                            + " identified to handle namespace: " + ns);
                }
                final ForeignObject foreign = new ForeignObject(ns);
                transferForeignObjects(attributes, foreign);
                setAreaAttributes(attributes, foreign);
                setTraits(attributes, foreign, SUBSET_COMMON);
                getCurrentViewport().setContent(foreign);
                Handler.this.areaStack.push(foreign);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        ForeignObject.class);
            }
        }

        private class BookmarkTreeMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final BookmarkData bm = new BookmarkData();
                Handler.this.areaStack.push(bm);
            }

            @Override
            public void endElement() {
                final Object tos = Handler.this.areaStack.pop();
                assertObjectOfClass(tos, BookmarkData.class);
                Handler.this.treeModel
                .handleOffDocumentItem((BookmarkData) tos);
                // as long as the bookmark tree comes after the last
                // PageViewport in the
                // area tree XML, we don't have to worry about
                // resolved/unresolved. The
                // only resolution needed is the mapping of the pvKey to the PV
                // instance.
            }
        }

        private class BookmarkMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final String title = attributes.getValue("title");
                final boolean showChildren = XMLUtil.getAttributeAsBoolean(
                        attributes, "show-children", false);
                final String[] linkdata = InternalLink
                        .parseXMLAttribute(attributes.getValue("internal-link"));
                final PageViewport pv = Handler.this.pageViewportsByKey
                        .get(linkdata[0]);
                final BookmarkData bm = new BookmarkData(title, showChildren,
                        pv, linkdata[1]);
                final Object tos = Handler.this.areaStack.peek();
                if (tos instanceof BookmarkData) {
                    final BookmarkData parent = (BookmarkData) tos;
                    parent.addSubData(bm);
                }
                Handler.this.areaStack.push(bm);
            }

            @Override
            public void endElement() {
                assertObjectOfClass(Handler.this.areaStack.pop(),
                        BookmarkData.class);
            }
        }

        private class DestinationMaker extends AbstractMaker {

            @Override
            public void startElement(final Attributes attributes) {
                final String[] linkdata = InternalLink
                        .parseXMLAttribute(Handler.this.lastAttributes
                                .getValue("internal-link"));
                final PageViewport pv = Handler.this.pageViewportsByKey
                        .get(linkdata[0]);
                final DestinationData dest = new DestinationData(linkdata[1]);
                final List<PageViewport> pages = new java.util.ArrayList<PageViewport>();
                pages.add(pv);
                dest.resolveIDRef(linkdata[1], pages);
                Handler.this.areaStack.push(dest);
            }

            @Override
            public void endElement() {
                final Object tos = Handler.this.areaStack.pop();
                assertObjectOfClass(tos, DestinationData.class);
                Handler.this.treeModel
                .handleOffDocumentItem((DestinationData) tos);
            }
        }

        // ====================================================================

        private void pushNewRegionReference(final Attributes attributes,
                final int side) {
            final String regionName = attributes.getValue("name");
            final RegionViewport rv = getCurrentRegionViewport();
            final RegionReference reg = new RegionReference(side, regionName,
                    rv);
            transferForeignObjects(attributes, reg);
            reg.setCTM(getAttributeAsCTM(attributes, "ctm"));
            setAreaAttributes(attributes, reg);
            setTraits(attributes, reg, SUBSET_BORDER_PADDING);
            rv.setRegionReference(reg);
            this.currentPageViewport.getPage().setRegionViewport(side, rv);
            this.areaStack.push(reg);
        }

        private void assertObjectOfClass(final Object obj, final Class clazz) {
            if (!clazz.isInstance(obj)) {
                throw new IllegalStateException("Object is not an instance of "
                        + clazz.getName() + " but of "
                        + obj.getClass().getName());
            }
        }

        /**
         * Handles objects created by "sub-parsers" that implement the
         * ObjectSource interface. An example of object handled here are
         * ExtensionAttachments.
         *
         * @param obj
         *            the Object to be handled.
         */
        protected void handleExternallyGeneratedObject(final Object obj) {
            if (this.areaStack.size() == 0
                    && obj instanceof ExtensionAttachment) {
                final ExtensionAttachment attachment = (ExtensionAttachment) obj;
                if (this.currentPageViewport == null) {
                    this.treeModel
                    .handleOffDocumentItem(new OffDocumentExtensionAttachment(
                            attachment));
                } else {
                    this.currentPageViewport.addExtensionAttachment(attachment);
                }
            } else {
                final Object o = this.areaStack.peek();
                if (o instanceof AreaTreeObject
                        && obj instanceof ExtensionAttachment) {
                    final AreaTreeObject ato = (AreaTreeObject) o;
                    final ExtensionAttachment attachment = (ExtensionAttachment) obj;
                    ato.addExtensionAttachment(attachment);
                } else {
                    log.warn("Don't know how to handle externally generated object: "
                            + obj);
                }
            }
        }

        private void setAreaAttributes(final Attributes attributes,
                final Area area) {
            area.setIPD(Integer.parseInt(attributes.getValue("ipd")));
            area.setBPD(Integer.parseInt(attributes.getValue("bpd")));
            area.setBidiLevel(XMLUtil
                    .getAttributeAsInt(attributes, "level", -1));
        }

        private static final Object[] SUBSET_COMMON = new Object[] { Trait.PROD_ID };
        private static final Object[] SUBSET_LINK = new Object[] {
            Trait.INTERNAL_LINK, Trait.EXTERNAL_LINK };
        private static final Object[] SUBSET_COLOR = new Object[] {
            Trait.BACKGROUND, Trait.COLOR };
        private static final Object[] SUBSET_FONT = new Object[] { Trait.FONT,
            Trait.FONT_SIZE, Trait.BLINK, Trait.OVERLINE,
            Trait.OVERLINE_COLOR, Trait.LINETHROUGH,
            Trait.LINETHROUGH_COLOR, Trait.UNDERLINE, Trait.UNDERLINE_COLOR };
        private static final Object[] SUBSET_BOX = new Object[] {
            Trait.BORDER_BEFORE, Trait.BORDER_AFTER, Trait.BORDER_START,
            Trait.BORDER_END, Trait.SPACE_BEFORE, Trait.SPACE_AFTER,
            Trait.SPACE_START, Trait.SPACE_END, Trait.PADDING_BEFORE,
            Trait.PADDING_AFTER, Trait.PADDING_START, Trait.PADDING_END,
            Trait.START_INDENT, Trait.END_INDENT, Trait.IS_REFERENCE_AREA,
            Trait.IS_VIEWPORT_AREA };
        private static final Object[] SUBSET_BORDER_PADDING = new Object[] {
            Trait.BORDER_BEFORE, Trait.BORDER_AFTER, Trait.BORDER_START,
            Trait.BORDER_END, Trait.PADDING_BEFORE, Trait.PADDING_AFTER,
            Trait.PADDING_START, Trait.PADDING_END };

        private void setTraits(final Attributes attributes, final Area area,
                final Object[] traitSubset) {
            for (int i = traitSubset.length; --i >= 0;) {
                final Integer trait = (Integer) traitSubset[i];
                final String traitName = Trait.getTraitName(trait);
                final String value = attributes.getValue(traitName);
                if (value != null) {
                    final Class cl = Trait.getTraitClass(trait);
                    if (cl == Integer.class) {
                        area.addTrait(trait, new Integer(value));
                    } else if (cl == Boolean.class) {
                        area.addTrait(trait, Boolean.valueOf(value));
                    } else if (cl == String.class) {
                        area.addTrait(trait, value);
                        if (Trait.PROD_ID.equals(trait)
                                && !this.idFirstsAssigned.contains(value)
                                && this.currentPageViewport != null) {
                            this.currentPageViewport.setFirstWithID(value);
                            this.idFirstsAssigned.add(value);
                        }
                    } else if (cl == Color.class) {
                        try {
                            area.addTrait(trait, ColorUtil.parseColorString(
                                    this.userAgent, value));
                        } catch (final PropertyException e) {
                            throw new IllegalArgumentException(e.getMessage());
                        }
                    } else if (cl == InternalLink.class) {
                        area.addTrait(trait, new InternalLink(value));
                    } else if (cl == Trait.ExternalLink.class) {
                        area.addTrait(trait,
                                Trait.ExternalLink.makeFromTraitValue(value));
                    } else if (cl == Background.class) {
                        final Background bkg = new Background();
                        try {
                            final Color col = ColorUtil.parseColorString(
                                    this.userAgent,
                                    attributes.getValue("bkg-color"));
                            bkg.setColor(col);
                        } catch (final PropertyException e) {
                            throw new IllegalArgumentException(e.getMessage());
                        }
                        final String uri = attributes.getValue("bkg-img");
                        if (uri != null) {
                            bkg.setURL(uri);

                            try {
                                final ImageManager manager = this.userAgent
                                        .getFactory().getImageManager();
                                final ImageSessionContext sessionContext = this.userAgent
                                        .getImageSessionContext();
                                final ImageInfo info = manager.getImageInfo(
                                        uri, sessionContext);
                                bkg.setImageInfo(info);
                            } catch (final ImageException e) {
                                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                                        .get(this.userAgent
                                                .getEventBroadcaster());
                                eventProducer.imageError(this, uri, e,
                                        getLocator());
                            } catch (final FileNotFoundException fnfe) {
                                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                                        .get(this.userAgent
                                                .getEventBroadcaster());
                                eventProducer.imageNotFound(this, uri, fnfe,
                                        getLocator());
                            } catch (final IOException ioe) {
                                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                                        .get(this.userAgent
                                                .getEventBroadcaster());
                                eventProducer.imageIOError(this, uri, ioe,
                                        getLocator());
                            }

                            final String repeat = attributes
                                    .getValue("bkg-repeat");
                            if (repeat != null) {
                                bkg.setRepeat(repeat);
                            }
                            bkg.setHoriz(XMLUtil.getAttributeAsInt(attributes,
                                    "bkg-horz-offset", 0));
                            bkg.setVertical(XMLUtil.getAttributeAsInt(
                                    attributes, "bkg-vert-offset", 0));
                        }
                        area.addTrait(trait, bkg);
                    } else if (cl == BorderProps.class) {
                        area.addTrait(trait,
                                BorderProps.valueOf(this.userAgent, value));
                    }
                } else {
                    if (Trait.FONT.equals(trait)) {
                        final String fontName = attributes
                                .getValue("font-name");
                        if (fontName != null) {
                            final String fontStyle = attributes
                                    .getValue("font-style");
                            final int fontWeight = XMLUtil.getAttributeAsInt(
                                    attributes, "font-weight",
                                    Font.WEIGHT_NORMAL);
                            area.addTrait(trait, FontInfo.createFontKey(
                                    fontName, fontStyle, fontWeight));
                        }
                    }
                }
            }
        }

        private static CTM getAttributeAsCTM(final Attributes attributes,
                final String name) {
            String s = attributes.getValue(name).trim();
            if (s.startsWith("[") && s.endsWith("]")) {
                s = s.substring(1, s.length() - 1);
                final double[] values = ConversionUtils.toDoubleArray(s, "\\s");
                if (values.length != 6) {
                    throw new IllegalArgumentException(
                            "CTM must consist of 6 double values!");
                }
                return new CTM(values[0], values[1], values[2], values[3],
                        values[4], values[5]);
            } else {
                throw new IllegalArgumentException(
                        "CTM must be surrounded by square brackets!");
            }
        }

        private static void transferForeignObjects(final Attributes atts,
                final AreaTreeObject ato) {
            for (int i = 0, c = atts.getLength(); i < c; i++) {
                final String ns = atts.getURI(i);
                if (ns.length() > 0) {
                    if (XMLConstants.XMLNS_NAMESPACE_URI.equals(ns)) {
                        continue;
                    }
                    final QName qname = new QName(ns, atts.getQName(i));
                    ato.setForeignAttribute(qname, atts.getValue(i));
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch, final int start,
                final int length) throws SAXException {
            if (this.delegate != null) {
                this.delegate.characters(ch, start, length);
            } else if (!this.ignoreCharacters) {
                final int maxLength = this.content.capacity()
                        - this.content.position();
                if (maxLength < length) {
                    // allocate a larger buffer and transfer content
                    final CharBuffer newContent = CharBuffer
                            .allocate(this.content.position() + length);
                    this.content.flip();
                    newContent.put(this.content);
                    this.content = newContent;
                }
                // make sure the full capacity is used
                this.content.limit(this.content.capacity());
                // add characters to the buffer
                this.content.put(ch, start, length);
                // decrease the limit, if necessary
                if (this.content.position() < this.content.limit()) {
                    this.content.limit(this.content.position());
                }
            }
        }
    }
}
