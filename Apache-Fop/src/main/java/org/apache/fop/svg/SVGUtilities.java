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

/* $Id: SVGUtilities.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.svg;

import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.StringTokenizer;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.XMLConstants;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Some utilities for creating svg DOM documents and elements.
 */
public final class SVGUtilities {

    private SVGUtilities() {
    }

    private static final String SVG_NS = SVGDOMImplementation.SVG_NAMESPACE_URI;

    /**
     * Create a new svg document with batik.
     * 
     * @param width
     *            the width of the root svg element
     * @param height
     *            the height of the root svg element
     * @return a new SVG Document
     */
    public static Document createSVGDocument(final float width,
            final float height) {
        final DOMImplementation impl = SVGDOMImplementation
                .getDOMImplementation();
        final Document doc = impl.createDocument(SVG_NS, "svg", null);

        final Element svgRoot = doc.getDocumentElement();
        svgRoot.setAttributeNS(null, "width", "" + width);
        svgRoot.setAttributeNS(null, "height", "" + height);
        return doc;
    }

    /**
     * Get the string width for a particular string given the font.
     * 
     * @param str
     *            the string
     * @param font
     *            the font
     * @return the width of the string in the given font
     */
    public static float getStringWidth(final String str,
            final java.awt.Font font) {
        final Rectangle2D rect = font.getStringBounds(str, 0, str.length(),
                new FontRenderContext(new AffineTransform(), true, true));
        return (float) rect.getWidth();
    }

    /**
     * Get the string height for a particular string given the font.
     * 
     * @param str
     *            the string
     * @param font
     *            the font
     * @return the height of the string in the given font
     */
    public static float getStringHeight(final String str,
            final java.awt.Font font) {
        final Rectangle2D rect = font.getStringBounds(str, 0, str.length(),
                new FontRenderContext(new AffineTransform(), true, true));
        return (float) rect.getHeight();
    }

    /**
     * Get the string bounds for a particular string given the font.
     * 
     * @param str
     *            the string
     * @param font
     *            the font
     * @return the bounds of the string
     */
    public static Rectangle2D getStringBounds(final String str,
            final java.awt.Font font) {
        return font.getStringBounds(str, 0, str.length(),
                new FontRenderContext(new AffineTransform(), true, true));
    }

    /**
     * Create an SVG Line
     * 
     * @param doc
     *            the document to create the element
     * @param x
     *            the start x position
     * @param y
     *            the start y position
     * @param x2
     *            the end x position
     * @param y2
     *            the end y position
     * @return the new line element
     */
    public static Element createLine(final Document doc, final float x,
            final float y, final float x2, final float y2) {
        final Element ellipse = doc.createElementNS(SVG_NS, "line");
        ellipse.setAttributeNS(null, "x1", "" + x);
        ellipse.setAttributeNS(null, "x2", "" + x2);
        ellipse.setAttributeNS(null, "y1", "" + y);
        ellipse.setAttributeNS(null, "y2", "" + y2);
        return ellipse;
    }

    /**
     * Create an SVG Ellipse
     * 
     * @param doc
     *            the document to create the element
     * @param cx
     *            the centre x position
     * @param cy
     *            the centre y position
     * @param rx
     *            the x axis radius
     * @param ry
     *            the y axis radius
     * @return the new ellipse element
     */
    public static Element createEllipse(final Document doc, final float cx,
            final float cy, final float rx, final float ry) {
        final Element ellipse = doc.createElementNS(SVG_NS, "ellipse");
        ellipse.setAttributeNS(null, "cx", "" + cx);
        ellipse.setAttributeNS(null, "rx", "" + rx);
        ellipse.setAttributeNS(null, "cy", "" + cy);
        ellipse.setAttributeNS(null, "ry", "" + ry);
        return ellipse;
    }

    /**
     * Create an SVG Path.
     * 
     * @param doc
     *            the document to create the element
     * @param str
     *            the string for the d attribute on the path
     * @return the new path element
     */
    public static Element createPath(final Document doc, final String str) {
        final Element path = doc.createElementNS(SVG_NS, "path");
        path.setAttributeNS(null, "d", str);
        return path;
    }

    /**
     * Create an SVG Text object.
     * 
     * @param doc
     *            the document to create the element
     * @param x
     *            the start x position
     * @param y
     *            the start y position
     * @param str
     *            the string
     * @return the new text element
     */
    public static Element createText(final Document doc, final float x,
            final float y, final String str) {
        final Element textGraph = doc.createElementNS(SVG_NS, "text");
        textGraph.setAttributeNS(null, "x", "" + x);
        textGraph.setAttributeNS(null, "y", "" + y);
        final org.w3c.dom.Text text = doc.createTextNode(str);
        textGraph.appendChild(text);
        return textGraph;
    }

    /**
     * Create an SVG Rectangle.
     * 
     * @param doc
     *            the document to create the element
     * @param x
     *            the start x position
     * @param y
     *            the start y position
     * @param width
     *            the width of the rectangle
     * @param height
     *            the height of the rectangle
     * @return the new rectangle element
     */
    public static Element createRect(final Document doc, final float x,
            final float y, final float width, final float height) {
        final Element border = doc.createElementNS(SVG_NS, "rect");
        border.setAttributeNS(null, "x", "" + x);
        border.setAttributeNS(null, "y", "" + y);
        border.setAttributeNS(null, "width", "" + width);
        border.setAttributeNS(null, "height", "" + height);
        return border;
    }

    /**
     * Create an SVG G.
     * 
     * @param doc
     *            the document to create the element
     * @return the new g element
     */
    public static Element createG(final Document doc) {
        final Element border = doc.createElementNS(SVG_NS, "g");
        return border;
    }

    /**
     * Create an SVG Clip.
     * 
     * @param doc
     *            the document to create the element
     * @param els
     *            the child elements that make the clip
     * @param id
     *            the id of the clipping path
     * @return the new clip element
     */
    public static Element createClip(final Document doc, final Element els,
            final String id) {
        final Element border = doc.createElementNS(SVG_NS, "clipPath");
        border.setAttributeNS(null, "id", id);
        border.appendChild(els);
        return border;
    }

    /**
     * Create and svg image element.
     * 
     * @param doc
     *            the document to create the element
     * @param ref
     *            the href link to the image
     * @param width
     *            the width to set on the image
     * @param height
     *            the height to set on the image
     * @return a new image element
     */
    public static Element createImage(final Document doc, final String ref,
            final float width, final float height) {
        final Element border = doc.createElementNS(SVG_NS, "image");
        border.setAttributeNS(XMLConstants.XLINK_NAMESPACE_URI, "href", ref);
        border.setAttributeNS(null, "width", "" + width);
        border.setAttributeNS(null, "height", "" + height);
        return border;
    }

    /**
     * Create some SVG text that is wrapped into a specified width.
     * 
     * @param doc
     *            the document to create the elements
     * @param str
     *            the string to wrap
     * @param font
     *            the font
     * @param width
     *            the width to wrap
     * @return the new element containing the wrapped text
     */
    public static Element wrapText(final Document doc, final String str,
            final java.awt.Font font, final float width) {
        final Element g = createG(doc);
        Element text;
        final StringTokenizer st = new StringTokenizer(str, " \t\r\n");
        float totalWidth = 0;
        String totalStr = "";
        int line = 0;
        final float height = getStringHeight(str, font);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            float strwidth = getStringWidth(token, font);
            totalWidth += strwidth;
            if (totalWidth > width) {
                if (totalStr.equals("")) {
                    totalStr = token;
                    token = "";
                    strwidth = 0;
                }
                text = createText(doc, 0, line * (height + 5), totalStr);
                g.appendChild(text);
                totalStr = token;
                totalWidth = strwidth;
                line++;
            } else {
                totalStr = totalStr + " " + token;
            }
        }

        return g;
    }

}
