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

/* $Id: IFParser.java 1342680 2012-05-25 15:15:28Z vhennebert $ */

package org.apache.fop.render.intermediate;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.accessibility.AccessibilityEventProducer;
import org.apache.fop.accessibility.StructureTreeElement;
import org.apache.fop.accessibility.StructureTreeEventHandler;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.ElementMappingRegistry;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.extensions.InternalElementMapping;
import org.apache.fop.render.intermediate.extensions.DocumentNavigationExtensionConstants;
import org.apache.fop.render.intermediate.extensions.DocumentNavigationHandler;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.ColorUtil;
import org.apache.fop.util.ContentHandlerFactory;
import org.apache.fop.util.ContentHandlerFactoryRegistry;
import org.apache.fop.util.DOMBuilderContentHandlerFactory;
import org.apache.fop.util.DefaultErrorListener;
import org.apache.fop.util.LanguageTags;
import org.apache.fop.util.XMLUtil;
import org.apache.xmlgraphics.util.QName;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is a parser for the intermediate format XML which converts the
 * intermediate file into {@link IFPainter} events.
 */
@Slf4j
public class IFParser implements IFConstants {

    private static SAXTransformerFactory tFactory = (SAXTransformerFactory) SAXTransformerFactory
            .newInstance();

    private static Set<String> handledNamespaces = new java.util.HashSet<String>();

    static {
        handledNamespaces.add(XMLNS_NAMESPACE_URI);
        handledNamespaces.add(XML_NAMESPACE);
        handledNamespaces.add(NAMESPACE);
        handledNamespaces.add(XLINK_NAMESPACE);
    }

    /**
     * Parses an intermediate file and paints it.
     *
     * @param src
     *            the Source instance pointing to the intermediate file
     * @param documentHandler
     *            the intermediate format document handler used to process the
     *            IF events
     * @param userAgent
     *            the user agent
     * @throws TransformerException
     *             if an error occurs while parsing the area tree XML
     * @throws IFException
     *             if an IF-related error occurs inside the target document
     *             handler
     */
    public void parse(final Source src,
            final IFDocumentHandler documentHandler, final FOUserAgent userAgent)
                    throws TransformerException, IFException {
        try {
            final Transformer transformer = tFactory.newTransformer();
            transformer.setErrorListener(new DefaultErrorListener(log));

            final SAXResult res = new SAXResult(getContentHandler(
                    documentHandler, userAgent));

            transformer.transform(src, res);
        } catch (final TransformerException te) {
            // Unpack original IFException if applicable
            if (te.getCause() instanceof SAXException) {
                final SAXException se = (SAXException) te.getCause();
                if (se.getCause() instanceof IFException) {
                    throw (IFException) se.getCause();
                }
            } else if (te.getCause() instanceof IFException) {
                throw (IFException) te.getCause();
            }
            throw te;
        }
    }

    /**
     * Creates a new ContentHandler instance that you can send the area tree XML
     * to. The parsed pages are added to the AreaTreeModel instance you pass in
     * as a parameter.
     *
     * @param documentHandler
     *            the intermediate format document handler used to process the
     *            IF events
     * @param userAgent
     *            the user agent
     * @return the ContentHandler instance to receive the SAX stream from the
     *         area tree XML
     */
    public ContentHandler getContentHandler(
            final IFDocumentHandler documentHandler, final FOUserAgent userAgent) {
        final ElementMappingRegistry elementMappingRegistry = userAgent
                .getFactory().getElementMappingRegistry();
        return new Handler(documentHandler, userAgent, elementMappingRegistry);
    }

    private static class Handler extends DefaultHandler {

        private final Map<String, ElementHandler> elementHandlers = new HashMap<String, ElementHandler>();

        private final IFDocumentHandler documentHandler;
        private IFPainter painter;
        private final FOUserAgent userAgent;
        private final ElementMappingRegistry elementMappingRegistry;

        private Attributes lastAttributes;

        private final StringBuilder content = new StringBuilder();
        private boolean ignoreCharacters = true;

        // private Stack delegateStack = new Stack();
        private int delegateDepth;
        private ContentHandler delegate;
        private boolean inForeignObject;
        private Document foreignObject;

        private ContentHandler navParser;

        private StructureTreeHandler structureTreeHandler;

        private Attributes pageSequenceAttributes;

        private final Map<String, StructureTreeElement> structureTreeElements = new HashMap<String, StructureTreeElement>();

        private final class StructureTreeHandler extends DefaultHandler {

            private final Locale pageSequenceLanguage;

            private final StructureTreeEventHandler structureTreeEventHandler;

            private StructureTreeHandler(
                    final StructureTreeEventHandler structureTreeEventHandler,
                    final Locale pageSequenceLanguage) {
                this.pageSequenceLanguage = pageSequenceLanguage;
                this.structureTreeEventHandler = structureTreeEventHandler;
            }

            void startStructureTree(final String type) {
                this.structureTreeEventHandler.startPageSequence(
                        this.pageSequenceLanguage, type);
            }

            @Override
            public void endDocument() throws SAXException {
                startIFElement(EL_PAGE_SEQUENCE,
                        Handler.this.pageSequenceAttributes);
                Handler.this.pageSequenceAttributes = null;
            }

            @Override
            public void startElement(final String uri, String localName,
                    final String qName, final Attributes attributes)
                    throws SAXException {
                if (!"structure-tree".equals(localName)) {
                    if (localName.equals("marked-content")) {
                        localName = "#PCDATA";
                    }
                    final String structID = attributes.getValue(
                            InternalElementMapping.URI,
                            InternalElementMapping.STRUCT_ID);
                    if (structID == null) {
                        this.structureTreeEventHandler.startNode(localName,
                                attributes);
                    } else if (localName.equals("external-graphic")
                            || localName.equals("instream-foreign-object")) {
                        final StructureTreeElement structureTreeElement = this.structureTreeEventHandler
                                .startImageNode(localName, attributes);
                        Handler.this.structureTreeElements.put(structID,
                                structureTreeElement);
                    } else {
                        final StructureTreeElement structureTreeElement = this.structureTreeEventHandler
                                .startReferencedNode(localName, attributes);
                        Handler.this.structureTreeElements.put(structID,
                                structureTreeElement);
                    }
                }
            }

            @Override
            public void endElement(final String uri, final String localName,
                    final String arqNameg2) throws SAXException {
                if (!"structure-tree".equals(localName)) {
                    this.structureTreeEventHandler.endNode(localName);
                }
            }
        }

        public Handler(final IFDocumentHandler documentHandler,
                final FOUserAgent userAgent,
                final ElementMappingRegistry elementMappingRegistry) {
            this.documentHandler = documentHandler;
            this.userAgent = userAgent;
            this.elementMappingRegistry = elementMappingRegistry;
            this.elementHandlers.put(EL_DOCUMENT, new DocumentHandler());
            this.elementHandlers.put(EL_HEADER, new DocumentHeaderHandler());
            this.elementHandlers.put(EL_LOCALE, new LocaleHandler());
            this.elementHandlers.put(EL_TRAILER, new DocumentTrailerHandler());
            this.elementHandlers.put(EL_PAGE_SEQUENCE,
                    new PageSequenceHandler());
            this.elementHandlers.put(EL_PAGE, new PageHandler());
            this.elementHandlers.put(EL_PAGE_HEADER, new PageHeaderHandler());
            this.elementHandlers.put(EL_PAGE_CONTENT, new PageContentHandler());
            this.elementHandlers.put(EL_PAGE_TRAILER, new PageTrailerHandler());
            // Page content
            this.elementHandlers.put(EL_VIEWPORT, new ViewportHandler());
            this.elementHandlers.put(EL_GROUP, new GroupHandler());
            this.elementHandlers.put(EL_ID, new IDHandler());
            this.elementHandlers.put(EL_FONT, new FontHandler());
            this.elementHandlers.put(EL_TEXT, new TextHandler());
            this.elementHandlers.put(EL_CLIP_RECT, new ClipRectHandler());
            this.elementHandlers.put(EL_RECT, new RectHandler());
            this.elementHandlers.put(EL_LINE, new LineHandler());
            this.elementHandlers.put(EL_BORDER_RECT, new BorderRectHandler());
            this.elementHandlers.put(EL_IMAGE, new ImageHandler());
        }

        private void establishForeignAttributes(
                final Map<QName, String> foreignAttributes) {
            this.documentHandler.getContext().setForeignAttributes(
                    foreignAttributes);
        }

        private void resetForeignAttributes() {
            this.documentHandler.getContext().resetForeignAttributes();
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final String uri, final String localName,
                final String qName, final Attributes attributes)
                throws SAXException {
            if (this.delegate != null) {
                this.delegateDepth++;
                this.delegate.startElement(uri, localName, qName, attributes);
            } else {
                boolean handled = true;
                if (NAMESPACE.equals(uri)) {
                    if (localName.equals(EL_PAGE_SEQUENCE)
                            && this.userAgent.isAccessibilityEnabled()) {
                        this.pageSequenceAttributes = new AttributesImpl(
                                attributes);
                        final Locale language = getLanguage(attributes);
                        this.structureTreeHandler = new StructureTreeHandler(
                                this.userAgent.getStructureTreeEventHandler(),
                                language);

                    } else if (localName.equals(EL_STRUCTURE_TREE)) {
                        if (this.userAgent.isAccessibilityEnabled()) {
                            final String type = attributes.getValue("type");
                            this.structureTreeHandler.startStructureTree(type);
                            this.delegate = this.structureTreeHandler;
                        } else {
                            /* Delegate to a handler that does nothing */
                            this.delegate = new DefaultHandler();
                        }
                        this.delegateDepth++;
                        this.delegate.startDocument();
                        this.delegate.startElement(uri, localName, qName,
                                attributes);
                    } else {
                        if (this.pageSequenceAttributes != null) {
                            /*
                             * This means that no structure-element tag was
                             * found in the XML, otherwise a
                             * StructureTreeBuilderWrapper object would have
                             * been created, which would have reset the
                             * pageSequenceAttributes field.
                             */
                            AccessibilityEventProducer.Provider.get(
                                    this.userAgent.getEventBroadcaster())
                                    .noStructureTreeInXML(this);
                        }
                        handled = startIFElement(localName, attributes);
                    }
                } else if (DocumentNavigationExtensionConstants.NAMESPACE
                        .equals(uri)) {
                    if (this.navParser == null) {
                        this.navParser = new DocumentNavigationHandler(
                                this.documentHandler
                                        .getDocumentNavigationHandler(),
                                this.structureTreeElements);
                    }
                    this.delegate = this.navParser;
                    this.delegateDepth++;
                    this.delegate.startDocument();
                    this.delegate.startElement(uri, localName, qName,
                            attributes);
                } else {
                    final ContentHandlerFactoryRegistry registry = this.userAgent
                            .getFactory().getContentHandlerFactoryRegistry();
                    ContentHandlerFactory factory = registry.getFactory(uri);
                    if (factory == null) {
                        DOMImplementation domImplementation = this.elementMappingRegistry
                                .getDOMImplementationForNamespace(uri);
                        if (domImplementation == null) {
                            domImplementation = ElementMapping
                                    .getDefaultDOMImplementation();
                            /*
                             * throw new
                             * SAXException("No DOMImplementation could be" +
                             * " identified to handle namespace: " + uri);
                             */
                        }
                        factory = new DOMBuilderContentHandlerFactory(uri,
                                domImplementation);
                    }
                    this.delegate = factory.createContentHandler();
                    this.delegateDepth++;
                    this.delegate.startDocument();
                    this.delegate.startElement(uri, localName, qName,
                            attributes);
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

        private static Locale getLanguage(final Attributes attributes) {
            final String xmllang = attributes.getValue(XML_NAMESPACE, "lang");
            return xmllang == null ? null : LanguageTags.toLocale(xmllang);
        }

        private boolean startIFElement(final String localName,
                final Attributes attributes) throws SAXException {
            this.lastAttributes = new AttributesImpl(attributes);
            final ElementHandler elementHandler = this.elementHandlers
                    .get(localName);
            this.content.setLength(0);
            this.ignoreCharacters = true;
            if (elementHandler != null) {
                this.ignoreCharacters = elementHandler.ignoreCharacters();
                try {
                    elementHandler.startElement(attributes);
                } catch (final IFException ife) {
                    handleIFException(ife);
                }
                return true;
            } else {
                return false;
            }
        }

        private void handleIFException(final IFException ife)
                throws SAXException {
            if (ife.getCause() instanceof SAXException) {
                // unwrap
                throw (SAXException) ife.getCause();
            } else {
                // wrap
                throw new SAXException(ife);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName,
                final String qName) throws SAXException {
            if (this.delegate != null) {
                this.delegate.endElement(uri, localName, qName);
                this.delegateDepth--;
                if (this.delegateDepth == 0) {
                    this.delegate.endDocument();
                    if (this.delegate instanceof ContentHandlerFactory.ObjectSource) {
                        final Object obj = ((ContentHandlerFactory.ObjectSource) this.delegate)
                                .getObject();
                        if (this.inForeignObject) {
                            this.foreignObject = (Document) obj;
                        } else {
                            handleExternallyGeneratedObject(obj);
                        }
                    }
                    this.delegate = null; // Sub-document is processed, return
                    // to normal processing
                }
            } else {
                if (NAMESPACE.equals(uri)) {
                    final ElementHandler elementHandler = this.elementHandlers
                            .get(localName);
                    if (elementHandler != null) {
                        try {
                            elementHandler.endElement();
                        } catch (final IFException ife) {
                            handleIFException(ife);
                        }
                        this.content.setLength(0);
                    }
                    this.ignoreCharacters = true;
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("Ignoring " + localName + " in namespace: "
                                + uri);
                    }
                }
            }
        }

        // ============== Element handlers for the intermediate format
        // =============

        private interface ElementHandler {
            void startElement(final Attributes attributes) throws IFException,
                    SAXException;

            void endElement() throws IFException;

            boolean ignoreCharacters();
        }

        private abstract class AbstractElementHandler implements ElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException, SAXException {
                // nop
            }

            @Override
            public void endElement() throws IFException {
                // nop
            }

            @Override
            public boolean ignoreCharacters() {
                return true;
            }
        }

        private class DocumentHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                Handler.this.documentHandler.startDocument();
            }

            @Override
            public void endElement() throws IFException {
                Handler.this.documentHandler.endDocument();
            }

        }

        private class DocumentHeaderHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                Handler.this.documentHandler.startDocumentHeader();
            }

            @Override
            public void endElement() throws IFException {
                Handler.this.documentHandler.endDocumentHeader();
            }

        }

        private class LocaleHandler extends AbstractElementHandler {
            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                Handler.this.documentHandler
                        .setDocumentLocale(getLanguage(attributes));
            }
        }

        private class DocumentTrailerHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                Handler.this.documentHandler.startDocumentTrailer();
            }

            @Override
            public void endElement() throws IFException {
                Handler.this.documentHandler.endDocumentTrailer();
            }

        }

        private class PageSequenceHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                final String id = attributes.getValue("id");
                final Locale language = getLanguage(attributes);
                if (language != null) {
                    Handler.this.documentHandler.getContext().setLanguage(
                            language);
                }
                final Map<QName, String> foreignAttributes = getForeignAttributes(Handler.this.lastAttributes);
                establishForeignAttributes(foreignAttributes);
                Handler.this.documentHandler.startPageSequence(id);
                resetForeignAttributes();
            }

            @Override
            public void endElement() throws IFException {
                Handler.this.documentHandler.endPageSequence();
                Handler.this.documentHandler.getContext().setLanguage(null);
            }

        }

        private class PageHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                final int index = Integer
                        .parseInt(attributes.getValue("index"));
                final String name = attributes.getValue("name");
                final String pageMasterName = attributes
                        .getValue("page-master-name");
                final int width = Integer
                        .parseInt(attributes.getValue("width"));
                final int height = Integer.parseInt(attributes
                        .getValue("height"));
                final Map<QName, String> foreignAttributes = getForeignAttributes(Handler.this.lastAttributes);
                establishForeignAttributes(foreignAttributes);
                Handler.this.documentHandler.startPage(index, name,
                        pageMasterName, new Dimension(width, height));
                resetForeignAttributes();
            }

            @Override
            public void endElement() throws IFException {
                Handler.this.documentHandler.endPage();
            }

        }

        private class PageHeaderHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                Handler.this.documentHandler.startPageHeader();
            }

            @Override
            public void endElement() throws IFException {
                Handler.this.documentHandler.endPageHeader();
            }

        }

        private class PageContentHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                Handler.this.painter = Handler.this.documentHandler
                        .startPageContent();
            }

            @Override
            public void endElement() throws IFException {
                Handler.this.painter = null;
                Handler.this.documentHandler.getContext().setID("");
                Handler.this.documentHandler.endPageContent();
            }

        }

        private class PageTrailerHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                Handler.this.documentHandler.startPageTrailer();
            }

            @Override
            public void endElement() throws IFException {
                Handler.this.documentHandler.endPageTrailer();
            }

        }

        private class ViewportHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                final String transform = attributes.getValue("transform");
                final AffineTransform[] transforms = AffineTransformArrayParser
                        .createAffineTransform(transform);
                final int width = Integer
                        .parseInt(attributes.getValue("width"));
                final int height = Integer.parseInt(attributes
                        .getValue("height"));
                final Rectangle clipRect = XMLUtil.getAttributeAsRectangle(
                        attributes, "clip-rect");
                Handler.this.painter.startViewport(transforms, new Dimension(
                        width, height), clipRect);
            }

            @Override
            public void endElement() throws IFException {
                Handler.this.painter.endViewport();
            }

        }

        private class GroupHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                final String transform = attributes.getValue("transform");
                final AffineTransform[] transforms = AffineTransformArrayParser
                        .createAffineTransform(transform);
                Handler.this.painter.startGroup(transforms);
            }

            @Override
            public void endElement() throws IFException {
                Handler.this.painter.endGroup();
            }

        }

        private class IDHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException, SAXException {
                final String id = attributes.getValue("name");
                Handler.this.documentHandler.getContext().setID(id);
            }

        }

        private class FontHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                final String family = attributes.getValue("family");
                final String style = attributes.getValue("style");
                final Integer weight = XMLUtil.getAttributeAsInteger(
                        attributes, "weight");
                final String variant = attributes.getValue("variant");
                final Integer size = XMLUtil.getAttributeAsInteger(attributes,
                        "size");
                Color color;
                try {
                    color = getAttributeAsColor(attributes, "color");
                } catch (final PropertyException pe) {
                    throw new IFException("Error parsing the color attribute",
                            pe);
                }
                Handler.this.painter.setFont(family, style, weight, variant,
                        size, color);
            }

        }

        private class TextHandler extends AbstractElementHandler {

            @Override
            public void endElement() throws IFException {
                final int x = Integer.parseInt(Handler.this.lastAttributes
                        .getValue("x"));
                final int y = Integer.parseInt(Handler.this.lastAttributes
                        .getValue("y"));
                String s = Handler.this.lastAttributes
                        .getValue("letter-spacing");
                final int letterSpacing = s != null ? Integer.parseInt(s) : 0;
                s = Handler.this.lastAttributes.getValue("word-spacing");
                final int wordSpacing = s != null ? Integer.parseInt(s) : 0;
                final int[] dx = XMLUtil.getAttributeAsIntArray(
                        Handler.this.lastAttributes, "dx");
                int[][] dp = XMLUtil.getAttributeAsPositionAdjustments(
                        Handler.this.lastAttributes, "dp");
                // if only DX present, then convert DX to DP; otherwise use only
                // DP,
                // effectively ignoring DX
                if (dp == null && dx != null) {
                    dp = IFUtil.convertDXToDP(dx);
                }
                establishStructureTreeElement(Handler.this.lastAttributes);
                Handler.this.painter.drawText(x, y, letterSpacing, wordSpacing,
                        dp, Handler.this.content.toString());
                resetStructureTreeElement();
            }

            @Override
            public boolean ignoreCharacters() {
                return false;
            }

        }

        private class ClipRectHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                final int x = Integer.parseInt(attributes.getValue("x"));
                final int y = Integer.parseInt(attributes.getValue("y"));
                final int width = Integer
                        .parseInt(attributes.getValue("width"));
                final int height = Integer.parseInt(attributes
                        .getValue("height"));
                Handler.this.painter
                        .clipRect(new Rectangle(x, y, width, height));
            }

        }

        private class RectHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                final int x = Integer.parseInt(attributes.getValue("x"));
                final int y = Integer.parseInt(attributes.getValue("y"));
                final int width = Integer
                        .parseInt(attributes.getValue("width"));
                final int height = Integer.parseInt(attributes
                        .getValue("height"));
                Color fillColor;
                try {
                    fillColor = getAttributeAsColor(attributes, "fill");
                } catch (final PropertyException pe) {
                    throw new IFException("Error parsing the fill attribute",
                            pe);
                }
                Handler.this.painter.fillRect(
                        new Rectangle(x, y, width, height), fillColor);
            }

        }

        private class LineHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                final int x1 = Integer.parseInt(attributes.getValue("x1"));
                final int y1 = Integer.parseInt(attributes.getValue("y1"));
                final int x2 = Integer.parseInt(attributes.getValue("x2"));
                final int y2 = Integer.parseInt(attributes.getValue("y2"));
                final int width = Integer.parseInt(attributes
                        .getValue("stroke-width"));
                Color color;
                try {
                    color = getAttributeAsColor(attributes, "color");
                } catch (final PropertyException pe) {
                    throw new IFException("Error parsing the fill attribute",
                            pe);
                }
                final RuleStyle style = RuleStyle.valueOf(attributes
                        .getValue("style"));
                Handler.this.painter.drawLine(new Point(x1, y1), new Point(x2,
                        y2), width, color, style);
            }

        }

        private static final String[] SIDES = new String[] { "top", "bottom",
                "left", "right" };

        private class BorderRectHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                final int x = Integer.parseInt(attributes.getValue("x"));
                final int y = Integer.parseInt(attributes.getValue("y"));
                final int width = Integer
                        .parseInt(attributes.getValue("width"));
                final int height = Integer.parseInt(attributes
                        .getValue("height"));
                final BorderProps[] borders = new BorderProps[4];
                for (int i = 0; i < 4; i++) {
                    final String b = attributes.getValue(SIDES[i]);
                    if (b != null) {
                        borders[i] = BorderProps.valueOf(
                                Handler.this.userAgent, b);
                    }
                }

                Handler.this.painter
                        .drawBorderRect(new Rectangle(x, y, width, height),
                                borders[0], borders[1], borders[2], borders[3]);
            }

        }

        private class ImageHandler extends AbstractElementHandler {

            @Override
            public void startElement(final Attributes attributes)
                    throws IFException {
                Handler.this.inForeignObject = true;
            }

            @Override
            public void endElement() throws IFException {
                final int x = Integer.parseInt(Handler.this.lastAttributes
                        .getValue("x"));
                final int y = Integer.parseInt(Handler.this.lastAttributes
                        .getValue("y"));
                final int width = Integer.parseInt(Handler.this.lastAttributes
                        .getValue("width"));
                final int height = Integer.parseInt(Handler.this.lastAttributes
                        .getValue("height"));
                final Map<QName, String> foreignAttributes = getForeignAttributes(Handler.this.lastAttributes);
                establishForeignAttributes(foreignAttributes);
                establishStructureTreeElement(Handler.this.lastAttributes);
                if (Handler.this.foreignObject != null) {
                    Handler.this.painter.drawImage(Handler.this.foreignObject,
                            new Rectangle(x, y, width, height));
                    Handler.this.foreignObject = null;
                } else {
                    final String uri = Handler.this.lastAttributes.getValue(
                            XLINK_HREF.getNamespaceURI(),
                            XLINK_HREF.getLocalName());
                    if (uri == null) {
                        throw new IFException("xlink:href is missing on image",
                                null);
                    }
                    Handler.this.painter.drawImage(uri, new Rectangle(x, y,
                            width, height));
                }
                resetForeignAttributes();
                resetStructureTreeElement();
                Handler.this.inForeignObject = false;
            }

            @Override
            public boolean ignoreCharacters() {
                return false;
            }
        }

        // ====================================================================

        /**
         * Handles objects created by "sub-parsers" that implement the
         * ObjectSource interface. An example of object handled here are
         * ExtensionAttachments.
         *
         * @param obj
         *            the Object to be handled.
         * @throws SAXException
         *             if an error occurs while handling the extension object
         */
        protected void handleExternallyGeneratedObject(final Object obj)
                throws SAXException {
            try {
                this.documentHandler.handleExtensionObject(obj);
            } catch (final IFException ife) {
                handleIFException(ife);
            }
        }

        private Color getAttributeAsColor(final Attributes attributes,
                final String name) throws PropertyException {
            final String s = attributes.getValue(name);
            if (s == null) {
                return null;
            } else {
                return ColorUtil.parseColorString(this.userAgent, s);
            }
        }

        private static Map<QName, String> getForeignAttributes(
                final Attributes atts) {
            Map<QName, String> foreignAttributes = null;
            for (int i = 0, c = atts.getLength(); i < c; i++) {
                final String ns = atts.getURI(i);
                if (ns.length() > 0) {
                    if (handledNamespaces.contains(ns)) {
                        continue;
                    }
                    if (foreignAttributes == null) {
                        foreignAttributes = new java.util.HashMap<QName, String>();
                    }
                    final QName qname = new QName(ns, atts.getQName(i));
                    foreignAttributes.put(qname, atts.getValue(i));
                }
            }
            return foreignAttributes;
        }

        private void establishStructureTreeElement(final Attributes attributes) {
            final String structRef = attributes.getValue(
                    InternalElementMapping.URI,
                    InternalElementMapping.STRUCT_REF);
            if (structRef != null && structRef.length() > 0) {
                assert this.structureTreeElements.containsKey(structRef);
                final StructureTreeElement structureTreeElement = this.structureTreeElements
                        .get(structRef);
                this.documentHandler.getContext().setStructureTreeElement(
                        structureTreeElement);
            }
        }

        private void resetStructureTreeElement() {
            this.documentHandler.getContext().resetStructureTreeElement();
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch, final int start,
                final int length) throws SAXException {
            if (this.delegate != null) {
                this.delegate.characters(ch, start, length);
            } else if (!this.ignoreCharacters) {
                this.content.append(ch, start, length);
            }
        }
    }
}
