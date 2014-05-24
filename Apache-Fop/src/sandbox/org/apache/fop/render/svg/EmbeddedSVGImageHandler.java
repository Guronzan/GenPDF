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

/* $Id: EmbeddedSVGImageHandler.java 816116 2009-09-17 09:58:48Z jeremias $ */

package org.apache.fop.render.svg;

import java.awt.Rectangle;
import java.io.IOException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.image.loader.batik.BatikImageFlavors;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.render.intermediate.DelegatingFragmentContentHandler;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageRawStream;
import org.apache.xmlgraphics.image.loader.impl.ImageXMLDOM;
import org.apache.xmlgraphics.util.QName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Image handler implementation that embeds SVG images in the target SVG file.
 */
@Slf4j
public class EmbeddedSVGImageHandler implements ImageHandler, SVGConstants {

    /** Constant for the "CDATA" attribute type. */
    private static final String CDATA = "CDATA";

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 500;
    }

    /** {@inheritDoc} */
    @Override
    public Class getSupportedImageClass() {
        return ImageRawStream.class;
    }

    /** {@inheritDoc} */
    @Override
    public ImageFlavor[] getSupportedImageFlavors() {
        return new ImageFlavor[] { BatikImageFlavors.SVG_DOM };
    }

    private void addAttribute(final AttributesImpl atts, final QName attribute,
            final String value) {
        atts.addAttribute(attribute.getNamespaceURI(),
                attribute.getLocalName(), attribute.getQName(), CDATA, value);
    }

    /** {@inheritDoc} */
    @Override
    public void handleImage(final RenderingContext context, final Image image,
            final Rectangle pos) throws IOException {
        final SVGRenderingContext svgContext = (SVGRenderingContext) context;
        final ImageXMLDOM svg = (ImageXMLDOM) image;
        final ContentHandler handler = svgContext.getContentHandler();
        final AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "x", "x", CDATA, SVGUtil.formatMptToPt(pos.x));
        atts.addAttribute("", "y", "y", CDATA, SVGUtil.formatMptToPt(pos.y));
        atts.addAttribute("", "width", "width", CDATA,
                SVGUtil.formatMptToPt(pos.width));
        atts.addAttribute("", "height", "height", CDATA,
                SVGUtil.formatMptToPt(pos.height));
        try {

            final Document doc = svg.getDocument();
            final Element svgEl = doc.getDocumentElement();
            if (svgEl.getAttribute("viewBox").length() == 0) {
                log.warn("SVG doesn't have a viewBox. The result might not be scaled correctly!");
            }

            final TransformerFactory tFactory = TransformerFactory
                    .newInstance();
            final Transformer transformer = tFactory.newTransformer();
            final DOMSource src = new DOMSource(svg.getDocument());
            final SAXResult res = new SAXResult(
                    new DelegatingFragmentContentHandler(handler) {

                        private boolean topLevelSVGFound = false;

                        private void setAttribute(final AttributesImpl atts,
                                final String localName, final String value) {
                            int index;
                            index = atts.getIndex("", localName);
                            if (index < 0) {
                                atts.addAttribute("", localName, localName,
                                        CDATA, value);
                            } else {
                                atts.setAttribute(index, "", localName,
                                        localName, CDATA, value);
                            }
                        }

                        @Override
                        public void startElement(final String uri,
                                final String localName, final String name,
                                final Attributes atts) throws SAXException {
                            if (!this.topLevelSVGFound
                                    && SVG_ELEMENT.getNamespaceURI()
                                    .equals(uri)
                                    && SVG_ELEMENT.getLocalName().equals(
                                            localName)) {
                                this.topLevelSVGFound = true;
                                final AttributesImpl modAtts = new AttributesImpl(
                                        atts);
                                setAttribute(modAtts, "x",
                                        SVGUtil.formatMptToPt(pos.x));
                                setAttribute(modAtts, "y",
                                        SVGUtil.formatMptToPt(pos.y));
                                setAttribute(modAtts, "width",
                                        SVGUtil.formatMptToPt(pos.width));
                                setAttribute(modAtts, "height",
                                        SVGUtil.formatMptToPt(pos.height));
                                super.startElement(uri, localName, name,
                                        modAtts);
                            } else {
                                super.startElement(uri, localName, name, atts);
                            }
                        }

                    });
            transformer.transform(src, res);
        } catch (final TransformerException te) {
            throw new IOException(te.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        if (targetContext instanceof SVGRenderingContext) {
            if (image == null) {
                return true;
            }
            if (image instanceof ImageXMLDOM) {
                final ImageXMLDOM svg = (ImageXMLDOM) image;
                return NAMESPACE.equals(svg.getRootNamespace());
            }
        }
        return false;
    }

}
