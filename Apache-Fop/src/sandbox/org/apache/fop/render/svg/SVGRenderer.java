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

/* $Id: SVGRenderer.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.render.svg;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.area.PageViewport;
import org.apache.fop.render.bitmap.MultiFileRenderingUtil;
import org.apache.fop.render.java2d.Java2DGraphicsState;
import org.apache.fop.render.java2d.Java2DRenderer;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * <p>
 * This renderer generates SVG (Scalable Vector Graphics) format.
 * <p>
 * This class actually does not render itself, instead it extends
 * <code>org.apache.fop.render.java2D.Java2DRenderer</code> and uses Apache
 * Batik's SVGGraphics2D for SVG generation.
 */
@Slf4j
public class SVGRenderer extends Java2DRenderer {

    /** The MIME type for the SVG format */
    public static final String MIME_TYPE = MimeConstants.MIME_SVG;

    private static final String SVG_FILE_EXTENSION = "svg";

    private OutputStream firstOutputStream;

    private Document document;

    private SVGGraphics2D svgGenerator;

    /** Helper class for generating multiple files */
    private MultiFileRenderingUtil multiFileUtil;

    /**
     * @param userAgent
     *            the user agent that contains configuration details. This
     *            cannot be null.
     */
    public SVGRenderer(final FOUserAgent userAgent) {
        super(userAgent);
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public void startRenderer(final OutputStream outputStream)
            throws IOException {
        this.firstOutputStream = outputStream;
        this.multiFileUtil = new MultiFileRenderingUtil(SVG_FILE_EXTENSION,
                getUserAgent().getOutputFile());
        super.startRenderer(this.firstOutputStream);
    }

    /** {@inheritDoc} */
    @Override
    public void renderPage(final PageViewport pageViewport) throws IOException {
        log.debug("Rendering page: " + pageViewport.getPageNumberString());
        // Get a DOMImplementation
        final DOMImplementation domImpl = GenericDOMImplementation
                .getDOMImplementation();

        // Create an instance of org.w3c.dom.Document
        this.document = domImpl.createDocument(null, "svg", null);

        // Create an SVGGeneratorContext to customize SVG generation
        final SVGGeneratorContext ctx = SVGGeneratorContext
                .createDefault(this.document);
        ctx.setComment("Generated by " + this.userAgent.getProducer()
                + " with Batik SVG Generator");
        ctx.setEmbeddedFontsOn(true);

        // Create an instance of the SVG Generator
        this.svgGenerator = new SVGGraphics2D(ctx, true);
        final Rectangle2D viewArea = pageViewport.getViewArea();
        final Dimension dim = new Dimension();
        dim.setSize(viewArea.getWidth() / 1000, viewArea.getHeight() / 1000);
        this.svgGenerator.setSVGCanvasSize(dim);

        final AffineTransform at = this.svgGenerator.getTransform();
        this.state = new Java2DGraphicsState(this.svgGenerator, this.fontInfo,
                at);
        try {
            // super.renderPage(pageViewport);
            renderPageAreas(pageViewport.getPage());
        } finally {
            this.state = null;
        }
        writeSVGFile(pageViewport.getPageIndex());

        this.svgGenerator = null;
        this.document = null;

    }

    /** {@inheritDoc} */
    @Override
    public void stopRenderer() throws IOException {
        super.stopRenderer();

        // Cleaning
        clearViewportList();
        log.debug("SVG generation complete.");
    }

    private void writeSVGFile(final int pageNumber) throws IOException {
        log.debug("Writing out SVG file...");
        // Finally, stream out SVG to the standard output using UTF-8
        // character to byte encoding
        final boolean useCSS = true; // we want to use CSS style attribute
        final OutputStream out = getCurrentOutputStream(pageNumber);
        if (out == null) {
            log.warn("No filename information available."
                    + " Stopping early after the first page.");
            return;
        }
        try {
            final Writer writer = new java.io.OutputStreamWriter(out, "UTF-8");
            this.svgGenerator.stream(writer, useCSS);
        } finally {
            if (out != this.firstOutputStream) {
                IOUtils.closeQuietly(out);
            } else {
                out.flush();
            }
        }
    }

    /**
     * Returns the OutputStream corresponding to this page
     *
     * @param pageNumber
     *            0-based page number
     * @return the corresponding OutputStream
     * @throws IOException
     *             In case of an I/O error
     */
    protected OutputStream getCurrentOutputStream(final int pageNumber)
            throws IOException {
        if (pageNumber == 0) {
            return this.firstOutputStream;
        } else {
            return this.multiFileUtil.createOutputStream(pageNumber);
        }

    }

}
