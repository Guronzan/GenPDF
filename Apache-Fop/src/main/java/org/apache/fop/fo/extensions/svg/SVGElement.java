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

/* $Id: SVGElement.java 719646 2008-11-21 17:24:32Z jeremias $ */

package org.apache.fop.fo.extensions.svg;

// FOP
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.bridge.UnitProcessor;
import org.apache.batik.dom.svg.SVGContext;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.svg.SVGOMDocument;
import org.apache.batik.dom.svg.SVGOMElement;
import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.XMLConstants;
import org.apache.fop.fo.FONode;
import org.apache.fop.util.ContentHandlerFactory;
import org.w3c.dom.Element;

/**
 * Class representing the SVG root element for constructing an SVG document.
 */
@Slf4j
public class SVGElement extends SVGObj {

    /**
     * Constructs an SVG object
     *
     * @param parent
     *            the parent formatting object
     */
    public SVGElement(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public ContentHandlerFactory getContentHandlerFactory() {
        return new SVGDOMContentHandlerFactory();
    }

    /**
     * Get the dimensions of this XML document.
     *
     * @param view
     *            the viewport dimensions
     * @return the dimensions of this SVG document
     */
    @Override
    public Point2D getDimension(final Point2D view) {

        // TODO change so doesn't hold onto fo, area tree
        final Element svgRoot = this.element;
        /* create an SVG area */
        /* if width and height are zero, get the bounds of the content. */

        try {
            final URL baseURL = new URL(
                    getUserAgent().getBaseURL() == null ? new java.io.File("")
                    .toURI().toURL().toExternalForm() : getUserAgent()
                    .getBaseURL());
            if (baseURL != null) {
                final SVGOMDocument svgdoc = (SVGOMDocument) this.doc;
                svgdoc.setURLObject(baseURL);
                // The following line should not be called to leave FOP
                // compatible to Batik 1.6.
                // svgdoc.setDocumentURI(baseURL.toString());
            }
        } catch (final Exception e) {
            log.error("Could not set base URL for svg", e);
        }

        final float ptmm = getUserAgent().getSourcePixelUnitToMillimeter();
        // temporary svg context
        final SVGContext dc = new SVGContext() {
            @Override
            public float getPixelToMM() {
                return ptmm;
            }

            @Override
            public float getPixelUnitToMillimeter() {
                return ptmm;
            }

            @Override
            public Rectangle2D getBBox() {
                return new Rectangle2D.Double(0, 0, view.getX(), view.getY());
            }

            /**
             * Returns the transform from the global transform space to pixels.
             */
            @Override
            public AffineTransform getScreenTransform() {
                throw new UnsupportedOperationException("NYI");
            }

            /**
             * Sets the transform to be used from the global transform space to
             * pixels.
             */
            @Override
            public void setScreenTransform(final AffineTransform at) {
                throw new UnsupportedOperationException("NYI");
            }

            @Override
            public AffineTransform getCTM() {
                return new AffineTransform();
            }

            @Override
            public AffineTransform getGlobalTransform() {
                return new AffineTransform();
            }

            @Override
            public float getViewportWidth() {
                return (float) view.getX();
            }

            @Override
            public float getViewportHeight() {
                return (float) view.getY();
            }

            @Override
            public float getFontSize() {
                return 12;
            }

            public void deselectAll() {
            }
        };
        final SVGOMElement e = (SVGOMElement) svgRoot;
        e.setSVGContext(dc);

        // if (!e.hasAttributeNS(XMLSupport.XMLNS_NAMESPACE_URI, "xmlns")) {
        e.setAttributeNS(XMLConstants.XMLNS_NAMESPACE_URI, "xmlns",
                SVGDOMImplementation.SVG_NAMESPACE_URI);
        // }
        final int fontSize = 12;
        final Point2D p2d = getSize(fontSize, svgRoot, getUserAgent()
                .getSourcePixelUnitToMillimeter());
        e.setSVGContext(null);

        return p2d;
    }

    /**
     * Get the size of the SVG root element.
     *
     * @param size
     *            the font size
     * @param svgRoot
     *            the svg root element
     * @param ptmm
     *            the pixel to millimeter conversion factor
     * @return the size of the SVG document
     */
    public static Point2D getSize(final int size, final Element svgRoot,
            final float ptmm) {
        String str;
        UnitProcessor.Context ctx;
        ctx = new PDFUnitContext(size, svgRoot, ptmm);
        str = svgRoot.getAttributeNS(null, SVGConstants.SVG_WIDTH_ATTRIBUTE);
        if (str.length() == 0) {
            str = "100%";
        }
        final float width = UnitProcessor.svgHorizontalLengthToUserSpace(str,
                SVGConstants.SVG_WIDTH_ATTRIBUTE, ctx);

        str = svgRoot.getAttributeNS(null, SVGConstants.SVG_HEIGHT_ATTRIBUTE);
        if (str.length() == 0) {
            str = "100%";
        }
        final float height = UnitProcessor.svgVerticalLengthToUserSpace(str,
                SVGConstants.SVG_HEIGHT_ATTRIBUTE, ctx);
        return new Point2D.Float(width, height);
    }

    /**
     * This class is the default context for a particular element. Information
     * not available on the element are obtained from the bridge context (such
     * as the viewport or the pixel to millimeter factor.
     */
    public static class PDFUnitContext implements UnitProcessor.Context {

        /** The element. */
        private final Element e;
        private final int fontSize;
        private final float pixeltoMM;

        /**
         * Create a PDF unit context.
         *
         * @param size
         *            the font size.
         * @param e
         *            the svg element
         * @param ptmm
         *            the pixel to millimeter factor
         */
        public PDFUnitContext(final int size, final Element e, final float ptmm) {
            this.e = e;
            this.fontSize = size;
            this.pixeltoMM = ptmm;
        }

        /**
         * Returns the element.
         *
         * @return the element
         */
        @Override
        public Element getElement() {
            return this.e;
        }

        /**
         * Returns the context of the parent element of this context. Since this
         * is always for the root SVG element there never should be one...
         *
         * @return null
         */
        public UnitProcessor.Context getParentElementContext() {
            return null;
        }

        /**
         * Returns the pixel to mm factor. (this is deprecated)
         *
         * @return the pixel to millimeter factor
         */
        @Override
        public float getPixelToMM() {
            return this.pixeltoMM;
        }

        /**
         * Returns the pixel to mm factor.
         *
         * @return the pixel to millimeter factor
         */
        @Override
        public float getPixelUnitToMillimeter() {
            return this.pixeltoMM;
        }

        /**
         * Returns the font-size value.
         *
         * @return the default font size
         */
        @Override
        public float getFontSize() {
            return this.fontSize;
        }

        /**
         * Returns the x-height value.
         *
         * @return the x-height value
         */
        @Override
        public float getXHeight() {
            return 0.5f;
        }

        /**
         * Returns the viewport width used to compute units.
         *
         * @return the default viewport width of 100
         */
        @Override
        public float getViewportWidth() {
            return 100;
        }

        /**
         * Returns the viewport height used to compute units.
         *
         * @return the default viewport height of 100
         */
        @Override
        public float getViewportHeight() {
            return 100;
        }
    }

}
