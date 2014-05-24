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

/* $Id: AbstractXMLRenderer.java 1237610 2012-01-30 11:46:13Z mehdi $ */

package org.apache.fop.render.xml;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.BookmarkData;
import org.apache.fop.area.OffDocumentExtensionAttachment;
import org.apache.fop.area.OffDocumentItem;
import org.apache.fop.area.PageViewport;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.fop.render.PrintRenderer;
import org.apache.fop.render.RendererContext;
import org.apache.xmlgraphics.util.QName;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

/** Abstract xml renderer base class. */
@Slf4j
public abstract class AbstractXMLRenderer extends PrintRenderer {

    /**
     * @param userAgent
     *            the user agent that contains configuration details. This
     *            cannot be null.
     */
    public AbstractXMLRenderer(final FOUserAgent userAgent) {
        super(userAgent);
    }

    /** Main namespace in use. */
    public static final String NS = "";

    /** CDATA type */
    public static final String CDATA = "CDATA";

    /** An empty Attributes object used when no attributes are needed. */
    public static final Attributes EMPTY_ATTS = new AttributesImpl();

    /** AttributesImpl instance that can be used during XML generation. */
    protected AttributesImpl atts = new AttributesImpl();

    /** ContentHandler that the generated XML is written to */
    protected ContentHandler handler;

    /** The OutputStream to write the generated XML to. */
    protected OutputStream out;

    /** The renderer context. */
    protected RendererContext context;

    /**
     * A list of ExtensionAttachements received through processOffDocumentItem()
     */
    protected List extensionAttachments;

    /**
     * Handles SAXExceptions.
     *
     * @param saxe
     *            the SAXException to handle
     */
    protected void handleSAXException(final SAXException saxe) {
        throw new RuntimeException(saxe.getMessage());
    }

    /**
     * Handles page extension attachments
     *
     * @param page
     *            the page viewport
     */
    protected void handlePageExtensionAttachments(final PageViewport page) {
        handleExtensionAttachments(page.getExtensionAttachments());
    }

    /**
     * Writes a comment to the generated XML.
     *
     * @param comment
     *            the comment
     */
    protected void comment(final String comment) {
        if (this.handler instanceof LexicalHandler) {
            try {
                ((LexicalHandler) this.handler).comment(comment.toCharArray(),
                        0, comment.length());
            } catch (final SAXException saxe) {
                handleSAXException(saxe);
            }
        }
    }

    /**
     * Starts a new element (without attributes).
     *
     * @param tagName
     *            tag name of the element
     */
    protected void startElement(final String tagName) {
        startElement(tagName, EMPTY_ATTS);
    }

    /**
     * Starts a new element.
     *
     * @param tagName
     *            tag name of the element
     * @param atts
     *            attributes to add
     */
    protected void startElement(final String tagName, final Attributes atts) {
        try {
            this.handler.startElement(NS, tagName, tagName, atts);
        } catch (final SAXException saxe) {
            handleSAXException(saxe);
        }
    }

    /**
     * Ends an element.
     *
     * @param tagName
     *            tag name of the element
     */
    protected void endElement(final String tagName) {
        try {
            this.handler.endElement(NS, tagName, tagName);
        } catch (final SAXException saxe) {
            handleSAXException(saxe);
        }
    }

    /**
     * Sends plain text to the XML
     *
     * @param text
     *            the text
     */
    protected void characters(final String text) {
        try {
            final char[] ca = text.toCharArray();
            this.handler.characters(ca, 0, ca.length);
        } catch (final SAXException saxe) {
            handleSAXException(saxe);
        }
    }

    /**
     * Adds a new attribute to the protected member variable "atts".
     *
     * @param name
     *            name of the attribute
     * @param value
     *            value of the attribute
     */
    protected void addAttribute(final String name, final String value) {
        this.atts.addAttribute(NS, name, name, CDATA, value);
    }

    /**
     * Adds a new attribute to the protected member variable "atts".
     *
     * @param name
     *            name of the attribute
     * @param value
     *            value of the attribute
     */
    protected void addAttribute(final QName name, final String value) {
        this.atts.addAttribute(name.getNamespaceURI(), name.getLocalName(),
                name.getQName(), CDATA, value);
    }

    /**
     * Adds a new attribute to the protected member variable "atts".
     *
     * @param name
     *            name of the attribute
     * @param value
     *            value of the attribute
     */
    protected void addAttribute(final String name, final int value) {
        addAttribute(name, Integer.toString(value));
    }

    private String createString(final Rectangle2D rect) {
        return "" + (int) rect.getX() + " " + (int) rect.getY() + " "
                + (int) rect.getWidth() + " " + (int) rect.getHeight();
    }

    /**
     * Adds a new attribute to the protected member variable "atts".
     *
     * @param name
     *            name of the attribute
     * @param rect
     *            a Rectangle2D to format and use as attribute value
     */
    protected void addAttribute(final String name, final Rectangle2D rect) {
        addAttribute(name, createString(rect));
    }

    /** {@inheritDoc} */
    @Override
    public void startRenderer(final OutputStream outputStream)
            throws IOException {
        if (this.handler == null) {
            final SAXTransformerFactory factory = (SAXTransformerFactory) TransformerFactory
                    .newInstance();
            try {
                final TransformerHandler transformerHandler = factory
                        .newTransformerHandler();
                setContentHandler(transformerHandler);
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
    }

    /** {@inheritDoc} */
    @Override
    public void stopRenderer() throws IOException {
        try {
            this.handler.endDocument();
        } catch (final SAXException saxe) {
            handleSAXException(saxe);
        }
        if (this.out != null) {
            this.out.flush();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void processOffDocumentItem(final OffDocumentItem oDI) {
        if (oDI instanceof BookmarkData) {
            renderBookmarkTree((BookmarkData) oDI);
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

    /** Handle document extension attachments. */
    protected void handleDocumentExtensionAttachments() {
        if (this.extensionAttachments != null
                && this.extensionAttachments.size() > 0) {
            handleExtensionAttachments(this.extensionAttachments);
            this.extensionAttachments.clear();
        }
    }

    /**
     * Sets an outside TransformerHandler to use instead of the default one
     * create in this class in startRenderer().
     *
     * @param handler
     *            Overriding TransformerHandler
     */
    public void setContentHandler(final ContentHandler handler) {
        this.handler = handler;
    }

    /**
     * Handles a list of extension attachments
     *
     * @param attachments
     *            a list of extension attachments
     */
    protected abstract void handleExtensionAttachments(final List attachments);

    /**
     * Renders a bookmark tree
     *
     * @param odi
     *            the bookmark data
     */
    protected abstract void renderBookmarkTree(final BookmarkData odi);
}
