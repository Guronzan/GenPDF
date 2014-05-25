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

/* $Id: SVGDataUrlImageHandler.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.render.svg;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.util.XMLConstants;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageRawStream;
import org.apache.xmlgraphics.util.QName;
import org.apache.xmlgraphics.util.uri.DataURLUtil;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Image handler implementation that embeds JPEG bitmaps as RFC 2397 data URLs
 * in the target SVG file.
 */
public class SVGDataUrlImageHandler implements ImageHandler, SVGConstants {

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
        return new ImageFlavor[] { ImageFlavor.RAW_PNG, ImageFlavor.RAW_JPEG, };
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
        final ImageRawStream raw = (ImageRawStream) image;
        final InputStream in = raw.createInputStream();
        try {
            final ContentHandler handler = svgContext.getContentHandler();
            final String url = DataURLUtil.createDataURL(in, raw.getMimeType());
            final AttributesImpl atts = new AttributesImpl();
            addAttribute(atts, XMLConstants.XLINK_HREF, url);
            atts.addAttribute("", "x", "x", CDATA, Integer.toString(pos.x));
            atts.addAttribute("", "y", "y", CDATA, Integer.toString(pos.y));
            atts.addAttribute("", "width", "width", CDATA,
                    Integer.toString(pos.width));
            atts.addAttribute("", "height", "height", CDATA,
                    Integer.toString(pos.height));
            try {
                handler.startElement(NAMESPACE, "image", "image", atts);
                handler.endElement(NAMESPACE, "image", "image");
            } catch (final SAXException e) {
                throw new IOException(e.getMessage());
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompatible(final RenderingContext targetContext,
            final Image image) {
        return (image == null || image instanceof ImageRawStream)
                && targetContext instanceof SVGRenderingContext;
    }

}
