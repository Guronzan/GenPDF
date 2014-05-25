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

/* $Id: SVGPainter.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.render.svg;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.fop.ResourceEventProducer;
import org.apache.fop.render.ImageHandlerUtil;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.render.intermediate.AbstractIFPainter;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFState;
import org.apache.fop.render.intermediate.IFUtil;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.ColorUtil;
import org.apache.fop.util.GenerationHelperContentHandler;
import org.apache.fop.util.XMLConstants;
import org.apache.fop.util.XMLUtil;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.xmp.Metadata;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * IFPainter implementation that writes SVG.
 */
public class SVGPainter extends AbstractIFPainter implements SVGConstants {

    private final AbstractSVGDocumentHandler parent;

    /** The SAX content handler that receives the generated XML events. */
    protected GenerationHelperContentHandler handler;

    private static final int MODE_NORMAL = 0;
    private static final int MODE_TEXT = 1;

    private int mode = MODE_NORMAL;

    /**
     * Main constructor.
     * 
     * @param parent
     *            the parent document handler
     * @param contentHandler
     *            the target SAX content handler
     */
    public SVGPainter(final AbstractSVGDocumentHandler parent,
            final GenerationHelperContentHandler contentHandler) {
        super();
        this.parent = parent;
        this.handler = contentHandler;
        this.state = IFState.create();
    }

    /** {@inheritDoc} */
    @Override
    protected IFContext getContext() {
        return this.parent.getContext();
    }

    /** {@inheritDoc} */
    @Override
    public void startViewport(final AffineTransform transform,
            final Dimension size, final Rectangle clipRect) throws IFException {
        startViewport(SVGUtil.formatAffineTransformMptToPt(transform), size,
                clipRect);
    }

    /** {@inheritDoc} */
    @Override
    public void startViewport(final AffineTransform[] transforms,
            final Dimension size, final Rectangle clipRect) throws IFException {
        startViewport(SVGUtil.formatAffineTransformsMptToPt(transforms), size,
                clipRect);
    }

    private void startViewport(final String transform, final Dimension size,
            final Rectangle clipRect) throws IFException {
        try {
            establish(MODE_NORMAL);
            final AttributesImpl atts = new AttributesImpl();
            if (transform != null && transform.length() > 0) {
                XMLUtil.addAttribute(atts, "transform", transform);
            }
            this.handler.startElement("g", atts);

            atts.clear();
            XMLUtil.addAttribute(atts, "width",
                    SVGUtil.formatMptToPt(size.width));
            XMLUtil.addAttribute(atts, "height",
                    SVGUtil.formatMptToPt(size.height));
            if (clipRect != null) {
                final int[] v = new int[] { clipRect.y,
                        -clipRect.x + size.width - clipRect.width,
                        -clipRect.y + size.height - clipRect.height, clipRect.x };
                int sum = 0;
                for (int i = 0; i < 4; i++) {
                    sum += Math.abs(v[i]);
                }
                if (sum != 0) {
                    final StringBuilder sb = new StringBuilder("rect(");
                    sb.append(SVGUtil.formatMptToPt(v[0])).append(',');
                    sb.append(SVGUtil.formatMptToPt(v[1])).append(',');
                    sb.append(SVGUtil.formatMptToPt(v[2])).append(',');
                    sb.append(SVGUtil.formatMptToPt(v[3])).append(')');
                    XMLUtil.addAttribute(atts, "clip", sb.toString());
                }
                XMLUtil.addAttribute(atts, "overflow", "hidden");
            } else {
                XMLUtil.addAttribute(atts, "overflow", "visible");
            }
            this.handler.startElement("svg", atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startBox()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endViewport() throws IFException {
        try {
            establish(MODE_NORMAL);
            this.handler.endElement("svg");
            this.handler.endElement("g");
        } catch (final SAXException e) {
            throw new IFException("SAX error in endBox()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startGroup(final AffineTransform[] transforms)
            throws IFException {
        startGroup(SVGUtil.formatAffineTransformsMptToPt(transforms));
    }

    /** {@inheritDoc} */
    @Override
    public void startGroup(final AffineTransform transform) throws IFException {
        startGroup(SVGUtil.formatAffineTransformMptToPt(transform));
    }

    private void startGroup(final String transform) throws IFException {
        try {
            final AttributesImpl atts = new AttributesImpl();
            if (transform != null && transform.length() > 0) {
                XMLUtil.addAttribute(atts, "transform", transform);
            }
            this.handler.startElement("g", atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in startGroup()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endGroup() throws IFException {
        try {
            establish(MODE_NORMAL);
            this.handler.endElement("g");
        } catch (final SAXException e) {
            throw new IFException("SAX error in endGroup()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final String uri, final Rectangle rect)
            throws IFException {
        try {
            establish(MODE_NORMAL);

            final ImageManager manager = getUserAgent().getFactory()
                    .getImageManager();
            ImageInfo info = null;
            try {
                final ImageSessionContext sessionContext = getUserAgent()
                        .getImageSessionContext();
                info = manager.getImageInfo(uri, sessionContext);

                final String mime = info.getMimeType();
                final Map foreignAttributes = getContext()
                        .getForeignAttributes();
                final String conversionMode = (String) foreignAttributes
                        .get(ImageHandlerUtil.CONVERSION_MODE);
                if ("reference".equals(conversionMode)
                        && (org.apache.xmlgraphics.util.MimeConstants.MIME_GIF
                                .equals(mime)
                                || org.apache.xmlgraphics.util.MimeConstants.MIME_JPEG
                                        .equals(mime)
                                || org.apache.xmlgraphics.util.MimeConstants.MIME_PNG
                                        .equals(mime) || org.apache.xmlgraphics.util.MimeConstants.MIME_SVG
                                    .equals(mime))) {
                    // Just reference the image
                    // TODO Some additional URI rewriting might be necessary
                    final AttributesImpl atts = new AttributesImpl();
                    XMLUtil.addAttribute(atts, XMLConstants.XLINK_HREF, uri);
                    XMLUtil.addAttribute(atts, "x",
                            SVGUtil.formatMptToPt(rect.x));
                    XMLUtil.addAttribute(atts, "y",
                            SVGUtil.formatMptToPt(rect.y));
                    XMLUtil.addAttribute(atts, "width",
                            SVGUtil.formatMptToPt(rect.width));
                    XMLUtil.addAttribute(atts, "height",
                            SVGUtil.formatMptToPt(rect.height));
                    this.handler.element("image", atts);
                } else {
                    drawImageUsingImageHandler(info, rect);
                }
            } catch (final ImageException ie) {
                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.imageError(this, info != null ? info.toString()
                        : uri, ie, null);
            } catch (final FileNotFoundException fe) {
                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.imageNotFound(this,
                        info != null ? info.toString() : uri, fe, null);
            } catch (final IOException ioe) {
                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                        .get(getUserAgent().getEventBroadcaster());
                eventProducer.imageIOError(this, info != null ? info.toString()
                        : uri, ioe, null);
            }
        } catch (final SAXException e) {
            throw new IFException("SAX error in drawImage()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final Document doc, final Rectangle rect)
            throws IFException {
        try {
            establish(MODE_NORMAL);

            drawImageUsingDocument(doc, rect);
        } catch (final SAXException e) {
            throw new IFException("SAX error in drawImage()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected RenderingContext createRenderingContext() {
        final SVGRenderingContext svgContext = new SVGRenderingContext(
                getUserAgent(), this.handler);
        return svgContext;
    }

    private static String toString(final Paint paint) {
        // TODO Paint serialization: Fine-tune and extend!
        if (paint instanceof Color) {
            return ColorUtil.colorToString((Color) paint);
        } else {
            throw new UnsupportedOperationException("Paint not supported: "
                    + paint);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void clipRect(final Rectangle rect) throws IFException {
        // TODO Implement me!!!
    }

    /** {@inheritDoc} */
    @Override
    public void fillRect(final Rectangle rect, final Paint fill)
            throws IFException {
        if (fill == null) {
            return;
        }
        try {
            establish(MODE_NORMAL);
            final AttributesImpl atts = new AttributesImpl();
            XMLUtil.addAttribute(atts, "x", SVGUtil.formatMptToPt(rect.x));
            XMLUtil.addAttribute(atts, "y", SVGUtil.formatMptToPt(rect.y));
            XMLUtil.addAttribute(atts, "width",
                    SVGUtil.formatMptToPt(rect.width));
            XMLUtil.addAttribute(atts, "height",
                    SVGUtil.formatMptToPt(rect.height));
            if (fill != null) {
                XMLUtil.addAttribute(atts, "fill", toString(fill));
            }
            /*
             * disabled if (stroke != null) { XMLUtil.addAttribute(atts,
             * "stroke", toString(stroke)); }
             */
            this.handler.element("rect", atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in fillRect()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawBorderRect(final Rectangle rect, final BorderProps before,
            final BorderProps after, final BorderProps start,
            final BorderProps end) throws IFException {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    @Override
    public void drawLine(final Point start, final Point end, final int width,
            final Color color, final RuleStyle style) throws IFException {
        try {
            establish(MODE_NORMAL);
            final AttributesImpl atts = new AttributesImpl();
            XMLUtil.addAttribute(atts, "x1", SVGUtil.formatMptToPt(start.x));
            XMLUtil.addAttribute(atts, "y1", SVGUtil.formatMptToPt(start.y));
            XMLUtil.addAttribute(atts, "x2", SVGUtil.formatMptToPt(end.x));
            XMLUtil.addAttribute(atts, "y2", SVGUtil.formatMptToPt(end.y));
            XMLUtil.addAttribute(atts, "stroke-width", toString(color));
            XMLUtil.addAttribute(atts, "fill", toString(color));
            // TODO Handle style parameter
            this.handler.element("line", atts);
        } catch (final SAXException e) {
            throw new IFException("SAX error in drawLine()", e);
        }
    }

    /** {@inheritDoc} */

    @Override
    public void drawText(final int x, final int y, final int letterSpacing,
            final int wordSpacing, final int[][] dp, final String text)
            throws IFException {
        try {
            establish(MODE_TEXT);
            final AttributesImpl atts = new AttributesImpl();
            XMLUtil.addAttribute(atts, XMLConstants.XML_SPACE, "preserve");
            XMLUtil.addAttribute(atts, "x", SVGUtil.formatMptToPt(x));
            XMLUtil.addAttribute(atts, "y", SVGUtil.formatMptToPt(y));
            if (letterSpacing != 0) {
                XMLUtil.addAttribute(atts, "letter-spacing",
                        SVGUtil.formatMptToPt(letterSpacing));
            }
            if (wordSpacing != 0) {
                XMLUtil.addAttribute(atts, "word-spacing",
                        SVGUtil.formatMptToPt(wordSpacing));
            }
            if (dp != null) {
                final int[] dx = IFUtil.convertDPToDX(dp);
                XMLUtil.addAttribute(atts, "dx", SVGUtil.formatMptArrayToPt(dx));
            }
            this.handler.startElement("text", atts);
            final char[] chars = text.toCharArray();
            this.handler.characters(chars, 0, chars.length);
            this.handler.endElement("text");
        } catch (final SAXException e) {
            throw new IFException("SAX error in setFont()", e);
        }
    }

    private void leaveTextMode() throws SAXException {
        assert this.mode == MODE_TEXT;
        this.handler.endElement("g");
        this.mode = MODE_NORMAL;
    }

    private void establish(final int newMode) throws SAXException {
        switch (newMode) {
        case MODE_TEXT:
            enterTextMode();
            break;
        default:
            if (this.mode == MODE_TEXT) {
                leaveTextMode();
            }
        }
    }

    private void enterTextMode() throws SAXException {
        if (this.state.isFontChanged() && this.mode == MODE_TEXT) {
            leaveTextMode();
        }
        if (this.mode != MODE_TEXT) {
            startTextGroup();
            this.mode = MODE_TEXT;
        }
    }

    private void startTextGroup() throws SAXException {
        final AttributesImpl atts = new AttributesImpl();
        XMLUtil.addAttribute(atts, "font-family",
                "'" + this.state.getFontFamily() + "'");
        XMLUtil.addAttribute(atts, "font-style", this.state.getFontStyle());
        XMLUtil.addAttribute(atts, "font-weight",
                Integer.toString(this.state.getFontWeight()));
        XMLUtil.addAttribute(atts, "font-variant", this.state.getFontVariant());
        XMLUtil.addAttribute(atts, "font-size",
                SVGUtil.formatMptToPt(this.state.getFontSize()));
        XMLUtil.addAttribute(atts, "fill", toString(this.state.getTextColor()));
        this.handler.startElement("g", atts);
        this.state.resetFontChanged();
    }

    /**
     * @param extension
     *            an extension object
     * @throws IFException
     *             if not caught
     */
    public void handleExtensionObject(final Object extension)
            throws IFException {
        if (extension instanceof Metadata) {
            final Metadata meta = (Metadata) extension;
            try {
                establish(MODE_NORMAL);
                this.handler.startElement("metadata");
                meta.toSAX(this.handler);
                this.handler.endElement("metadata");
            } catch (final SAXException e) {
                throw new IFException(
                        "SAX error while handling extension object", e);
            }
        } else {
            throw new UnsupportedOperationException(
                    "Don't know how to handle extension object: " + extension);
        }
    }

}
