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

/* $Id: FOTreeBuilder.java 1296496 2012-03-02 22:19:46Z gadams $ */

package org.apache.fop.fo;

import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.accessibility.fo.FO2StructureTreeConverter;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.fo.ElementMapping.Maker;
import org.apache.fop.fo.extensions.ExtensionElementMapping;
import org.apache.fop.fo.pagination.Root;
import org.apache.fop.util.ContentHandlerFactory;
import org.apache.fop.util.ContentHandlerFactory.ObjectBuiltListener;
import org.apache.fop.util.ContentHandlerFactory.ObjectSource;
import org.apache.xmlgraphics.util.QName;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX Handler that passes parsed data to the various FO objects, where they can
 * be used either to build an FO Tree, or used by Structure Renderers to build
 * other data structures.
 */
@Slf4j
public class FOTreeBuilder extends DefaultHandler {

    /** The registry for ElementMapping instances */
    protected ElementMappingRegistry elementMappingRegistry;

    /** The root of the formatting object tree */
    protected Root rootFObj = null;

    /** Main DefaultHandler that handles the FO namespace. */
    protected MainFOHandler mainFOHandler;

    /** Current delegate ContentHandler to receive the SAX events */
    protected ContentHandler delegate;

    /** Provides information used during tree building stage. */
    private FOTreeBuilderContext builderContext;

    /** The object that handles formatting and rendering to a stream */
    private FOEventHandler foEventHandler;

    /** The SAX locator object managing the line and column counters */
    private Locator locator;

    /** The user agent for this processing run. */
    private FOUserAgent userAgent;

    private boolean used = false;
    private boolean empty = true;

    private int depth;

    /**
     * <code>FOTreeBuilder</code> constructor
     *
     * @param outputFormat
     *            the MIME type of the output format to use (ex.
     *            "application/pdf").
     * @param foUserAgent
     *            the {@link FOUserAgent} in effect for this process
     * @param stream
     *            the <code>OutputStream</code> to direct the results to
     * @throws FOPException
     *             if the <code>FOTreeBuilder</code> cannot be properly created
     */
    public FOTreeBuilder(final String outputFormat,
            final FOUserAgent foUserAgent, final OutputStream stream)
            throws FOPException {

        this.userAgent = foUserAgent;
        this.elementMappingRegistry = this.userAgent.getFactory()
                .getElementMappingRegistry();
        // This creates either an AreaTreeHandler and ultimately a Renderer, or
        // one of the RTF-, MIF- etc. Handlers.
        this.foEventHandler = foUserAgent.getRendererFactory()
                .createFOEventHandler(foUserAgent, outputFormat, stream);
        if (this.userAgent.isAccessibilityEnabled()) {
            this.foEventHandler = new FO2StructureTreeConverter(
                    foUserAgent.getStructureTreeEventHandler(),
                    this.foEventHandler);
        }
        this.builderContext = new FOTreeBuilderContext();
        this.builderContext.setPropertyListMaker(new PropertyListMaker() {
            @Override
            public PropertyList make(final FObj fobj,
                    final PropertyList parentPropertyList) {
                return new StaticPropertyList(fobj, parentPropertyList);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void setDocumentLocator(final Locator locator) {
        this.locator = locator;
    }

    /**
     * @return a {@link Locator} instance if it is available and not disabled
     */
    protected Locator getEffectiveLocator() {
        return this.userAgent.isLocatorEnabled() ? this.locator : null;
    }

    /** {@inheritDoc} */
    @Override
    public void characters(final char[] data, final int start, final int length)
            throws SAXException {
        this.delegate.characters(data, start, length);
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws SAXException {
        if (this.used) {
            throw new IllegalStateException(
                    "FOTreeBuilder (and the Fop class) cannot be reused."
                            + " Please instantiate a new instance.");
        }

        this.used = true;
        this.empty = true;
        this.rootFObj = null; // allows FOTreeBuilder to be reused
        if (log.isDebugEnabled()) {
            log.debug("Building formatting object tree");
        }
        this.foEventHandler.startDocument();
        this.mainFOHandler = new MainFOHandler();
        this.mainFOHandler.startDocument();
        this.delegate = this.mainFOHandler;
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {
        this.delegate.endDocument();
        if (this.rootFObj == null && this.empty) {
            final FOValidationEventProducer eventProducer = FOValidationEventProducer.Provider
                    .get(this.userAgent.getEventBroadcaster());
            eventProducer.emptyDocument(this);
        }
        this.rootFObj = null;
        if (log.isDebugEnabled()) {
            log.debug("Parsing of document complete");
        }
        this.foEventHandler.endDocument();
    }

    /** {@inheritDoc} */
    @Override
    public void startElement(final String namespaceURI, final String localName,
            final String rawName, final Attributes attlist) throws SAXException {
        this.depth++;
        this.delegate.startElement(namespaceURI, localName, rawName, attlist);
    }

    /** {@inheritDoc} */
    @Override
    public void endElement(final String uri, final String localName,
            final String rawName) throws SAXException {
        this.delegate.endElement(uri, localName, rawName);
        this.depth--;
        if (this.depth == 0) {
            if (this.delegate != this.mainFOHandler) {
                // Return from sub-handler back to main handler
                this.delegate.endDocument();
                this.delegate = this.mainFOHandler;
                this.delegate.endElement(uri, localName, rawName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void warning(final SAXParseException e) {
        log.warn(e.getLocalizedMessage());
    }

    /** {@inheritDoc} */
    @Override
    public void error(final SAXParseException e) {
        log.error(e.toString());
    }

    /** {@inheritDoc} */
    @Override
    public void fatalError(final SAXParseException e) throws SAXException {
        log.error(e.toString());
        throw e;
    }

    /**
     * Provides access to the underlying {@link FOEventHandler} object.
     *
     * @return the FOEventHandler object
     */
    public FOEventHandler getEventHandler() {
        return this.foEventHandler;
    }

    /**
     * Returns the results of the rendering process. Information includes the
     * total number of pages generated and the number of pages per
     * page-sequence.
     *
     * @return the results of the rendering process.
     */
    public FormattingResults getResults() {
        return getEventHandler().getResults();
    }

    /**
     * Main <code>DefaultHandler</code> implementation which builds the FO tree.
     */
    private class MainFOHandler extends DefaultHandler {

        /** Current formatting object being handled */
        protected FONode currentFObj = null;

        /** Current propertyList for the node being handled */
        protected PropertyList currentPropertyList;

        /** Current marker nesting-depth */
        private int nestedMarkerDepth = 0;

        /** {@inheritDoc} */
        @Override
        public void startElement(final String namespaceURI,
                final String localName, final String rawName,
                final Attributes attlist) throws SAXException {

            /* the node found in the FO document */
            FONode foNode;
            PropertyList propertyList = null;

            // Check to ensure first node encountered is an fo:root
            if (FOTreeBuilder.this.rootFObj == null) {
                FOTreeBuilder.this.empty = false;
                if (!namespaceURI.equals(FOElementMapping.URI)
                        || !localName.equals("root")) {
                    final FOValidationEventProducer eventProducer = FOValidationEventProducer.Provider
                            .get(FOTreeBuilder.this.userAgent
                                    .getEventBroadcaster());
                    eventProducer.invalidFORoot(this,
                            FONode.getNodeString(namespaceURI, localName),
                            getEffectiveLocator());
                }
            } else { // check that incoming node is valid for currentFObj
                if (this.currentFObj.getNamespaceURI().equals(
                        FOElementMapping.URI)
                        || this.currentFObj.getNamespaceURI().equals(
                                ExtensionElementMapping.URI)) {
                    this.currentFObj
                    .validateChildNode(FOTreeBuilder.this.locator,
                            namespaceURI, localName);
                }
            }

            final ElementMapping.Maker fobjMaker = findFOMaker(namespaceURI,
                    localName);

            try {
                foNode = fobjMaker.make(this.currentFObj);
                if (FOTreeBuilder.this.rootFObj == null) {
                    FOTreeBuilder.this.rootFObj = (Root) foNode;
                    FOTreeBuilder.this.rootFObj
                    .setBuilderContext(FOTreeBuilder.this.builderContext);
                    FOTreeBuilder.this.rootFObj
                    .setFOEventHandler(FOTreeBuilder.this.foEventHandler);
                }
                propertyList = foNode.createPropertyList(
                        this.currentPropertyList,
                        FOTreeBuilder.this.foEventHandler);
                foNode.processNode(localName, getEffectiveLocator(), attlist,
                        propertyList);
                if (foNode.getNameId() == Constants.FO_MARKER) {
                    if (FOTreeBuilder.this.builderContext.inMarker()) {
                        this.nestedMarkerDepth++;
                    } else {
                        FOTreeBuilder.this.builderContext
                        .switchMarkerContext(true);
                    }
                }
                if (foNode.getNameId() == Constants.FO_PAGE_SEQUENCE) {
                    FOTreeBuilder.this.builderContext.getXMLWhiteSpaceHandler()
                    .reset();
                }
            } catch (final IllegalArgumentException e) {
                throw new SAXException(e);
            }

            final ContentHandlerFactory chFactory = foNode
                    .getContentHandlerFactory();
            if (chFactory != null) {
                final ContentHandler subHandler = chFactory
                        .createContentHandler();
                if (subHandler instanceof ObjectSource
                        && foNode instanceof ObjectBuiltListener) {
                    ((ObjectSource) subHandler)
                    .setObjectBuiltListener((ObjectBuiltListener) foNode);
                }

                subHandler.startDocument();
                subHandler.startElement(namespaceURI, localName, rawName,
                        attlist);
                FOTreeBuilder.this.depth = 1;
                FOTreeBuilder.this.delegate = subHandler;
            }

            if (this.currentFObj != null) {
                this.currentFObj.addChildNode(foNode);
            }

            this.currentFObj = foNode;
            if (propertyList != null
                    && !FOTreeBuilder.this.builderContext.inMarker()) {
                this.currentPropertyList = propertyList;
            }

            // fo:characters can potentially be removed during
            // white-space handling.
            // Do not notify the FOEventHandler.
            if (this.currentFObj.getNameId() != Constants.FO_CHARACTER) {
                this.currentFObj.startOfNode();
            }
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName,
                final String rawName) throws SAXException {
            if (this.currentFObj == null) {
                throw new SAXException("endElement() called for " + rawName
                        + " where there is no current element.");
            } else if (!this.currentFObj.getLocalName().equals(localName)
                    || !this.currentFObj.getNamespaceURI().equals(uri)) {
                throw new SAXException("Mismatch: "
                        + this.currentFObj.getLocalName() + " ("
                        + this.currentFObj.getNamespaceURI() + ") vs. "
                        + localName + " (" + uri + ")");
            }

            // fo:characters can potentially be removed during
            // white-space handling.
            // Do not notify the FOEventHandler.
            if (this.currentFObj.getNameId() != Constants.FO_CHARACTER) {
                this.currentFObj.endOfNode();
            }

            if (this.currentPropertyList != null
                    && this.currentPropertyList.getFObj() == this.currentFObj
                    && !FOTreeBuilder.this.builderContext.inMarker()) {
                this.currentPropertyList = this.currentPropertyList
                        .getParentPropertyList();
            }

            if (this.currentFObj.getNameId() == Constants.FO_MARKER) {
                if (this.nestedMarkerDepth == 0) {
                    FOTreeBuilder.this.builderContext
                    .switchMarkerContext(false);
                } else {
                    this.nestedMarkerDepth--;
                }
            }

            if (this.currentFObj.getParent() == null) {
                log.debug("endElement for top-level "
                        + this.currentFObj.getName());
            }

            this.currentFObj = this.currentFObj.getParent();
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] data, final int start,
                final int length) throws FOPException {
            if (this.currentFObj != null) {
                this.currentFObj.characters(data, start, length,
                        this.currentPropertyList, getEffectiveLocator());
            }
        }

        /** {@inheritDoc} */
        @Override
        public void endDocument() throws SAXException {
            this.currentFObj = null;
        }

        /**
         * Finds the {@link Maker} used to create {@link FONode} objects of a
         * particular type
         *
         * @param namespaceURI
         *            URI for the namespace of the element
         * @param localName
         *            name of the Element
         * @return the ElementMapping.Maker that can create an FO object for
         *         this element
         * @throws FOPException
         *             if a Maker could not be found for a bound namespace.
         */
        private Maker findFOMaker(final String namespaceURI,
                final String localName) throws FOPException {
            final Maker maker = FOTreeBuilder.this.elementMappingRegistry
                    .findFOMaker(namespaceURI, localName,
                            FOTreeBuilder.this.locator);
            if (maker instanceof UnknownXMLObj.Maker) {
                final FOValidationEventProducer eventProducer = FOValidationEventProducer.Provider
                        .get(FOTreeBuilder.this.userAgent.getEventBroadcaster());
                final String name = this.currentFObj != null ? this.currentFObj
                        .getName() : "{" + namespaceURI + "}" + localName;
                eventProducer.unknownFormattingObject(this, name, new QName(
                                namespaceURI, localName), getEffectiveLocator());
            }
            return maker;
        }

    }
}
