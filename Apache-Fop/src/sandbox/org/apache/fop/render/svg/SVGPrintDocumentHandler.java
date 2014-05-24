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

/* $Id: SVGPrintDocumentHandler.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.svg;

import java.awt.Dimension;

import javax.xml.transform.Result;

import org.apache.fop.render.intermediate.IFConstants;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFPainter;
import org.apache.fop.util.XMLUtil;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * {@link org.apache.fop.render.intermediate.IFDocumentHandler} implementation
 * that writes SVG Print.
 */
public class SVGPrintDocumentHandler extends AbstractSVGDocumentHandler {

    /**
     * Default constructor.
     */
    public SVGPrintDocumentHandler() {
        // nop
    }

    /**
     * Creates a new SVGPrintPainter that sends the XML content it generates to
     * the given SAX ContentHandler.
     * 
     * @param result
     *            the JAXP Result object to receive the generated content
     * @throws IFException
     *             if an error occurs setting up the output
     */
    public SVGPrintDocumentHandler(final Result result) throws IFException {
        setResult(result);
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsPagesOutOfOrder() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return MIME_SVG_PRINT;
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws IFException {
        super.startDocument();
        try {
            this.handler.startDocument();
            this.handler.startPrefixMapping("", NAMESPACE);
            this.handler.startPrefixMapping(XLINK_PREFIX, XLINK_NAMESPACE);
            this.handler.startPrefixMapping("if", IFConstants.NAMESPACE);
            final AttributesImpl atts = new AttributesImpl();
            XMLUtil.addAttribute(atts, "version", "1.2"); // SVG Print is SVG
                                                          // 1.2
            this.handler.startElement("svg", atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startDocument()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws IFException {
        try {
            this.handler.endElement("svg");
            this.handler.endDocument();
        } catch (final SAXException e) {
            throw new IFException("SAX error in endDocument()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final String id) throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            if (id != null) {
                atts.addAttribute(XML_NAMESPACE, "id", "xml:id", CDATA, id);
            }
            this.handler.startElement("pageSet", atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startPageSequence()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPageSequence() throws IFException {
        try {
            this.handler.endElement("pageSet");
        } catch (final SAXException e) {
            throw new IFException("SAX error in endPageSequence()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPage(final int index, final String name,
            final String pageMasterName, final Dimension size)
            throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            /*
             * XMLUtil.addAttribute(atts, "index", Integer.toString(index));
             * XMLUtil.addAttribute(atts, "name", name);
             */
            // NOTE: SVG Print doesn't support individual page sizes for each
            // page
            atts.addAttribute(IFConstants.NAMESPACE, "width", "if:width",
                    CDATA, Integer.toString(size.width));
            atts.addAttribute(IFConstants.NAMESPACE, "height", "if:height",
                    CDATA, Integer.toString(size.height));
            atts.addAttribute(IFConstants.NAMESPACE, "viewBox", "if:viewBox",
                    CDATA, "0 0 " + Integer.toString(size.width) + " "
                            + Integer.toString(size.height));
            this.handler.startElement("page", atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startPage()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageHeader() throws IFException {
    }

    /** {@inheritDoc} */
    @Override
    public void endPageHeader() throws IFException {
    }

    /** {@inheritDoc} */
    @Override
    public IFPainter startPageContent() throws IFException {
        try {
            this.handler.startElement("g");
        } catch (final SAXException e) {
            throw new IFException("SAX error in startPageContent()", e);
        }
        return new SVGPainter(this, this.handler);
    }

    /** {@inheritDoc} */
    @Override
    public void endPageContent() throws IFException {
        try {
            this.handler.endElement("g");
        } catch (final SAXException e) {
            throw new IFException("SAX error in endPageContent()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageTrailer() throws IFException {
    }

    /** {@inheritDoc} */
    @Override
    public void endPageTrailer() throws IFException {
    }

    /** {@inheritDoc} */
    @Override
    public void endPage() throws IFException {
        try {
            this.handler.endElement("page");
        } catch (final SAXException e) {
            throw new IFException("SAX error in endPage()", e);
        }
    }

}
