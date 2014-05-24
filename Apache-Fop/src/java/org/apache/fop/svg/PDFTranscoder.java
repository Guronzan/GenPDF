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

/* $Id: PDFTranscoder.java 1297284 2012-03-05 23:29:29Z gadams $ */

package org.apache.fop.svg;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.UnitProcessor;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.fop.Version;
import org.apache.fop.fonts.FontInfo;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGLength;

/**
 * <p>
 * This class enables to transcode an input to a PDF document.
 * </p>
 *
 * <p>
 * Two transcoding hints (<code>KEY_WIDTH</code> and <code>KEY_HEIGHT</code>)
 * can be used to respectively specify the image width and the image height. If
 * only one of these keys is specified, the transcoder preserves the aspect
 * ratio of the original image.
 *
 * <p>
 * The <code>KEY_BACKGROUND_COLOR</code> defines the background color to use for
 * opaque image formats, or the background color that may be used for image
 * formats that support alpha channel.
 *
 * <p>
 * The <code>KEY_AOI</code> represents the area of interest to paint in device
 * space.
 *
 * <p>
 * Three additional transcoding hints that act on the SVG processor can be
 * specified:
 *
 * <p>
 * <code>KEY_LANGUAGE</code> to set the default language to use (may be used by
 * a &lt;switch> SVG element for example), <code>KEY_USER_STYLESHEET_URI</code>
 * to fix the URI of a user stylesheet, and <code>KEY_PIXEL_TO_MM</code> to
 * specify the pixel to millimeter conversion factor.
 *
 * <p>
 * <code>KEY_AUTO_FONTS</code> to disable the auto-detection of fonts installed
 * in the system. The PDF Transcoder cannot use AWT's font subsystem and that's
 * why the fonts have to be configured differently. By default, font
 * auto-detection is enabled to match the behaviour of the other transcoders,
 * but this may be associated with a price in the form of a small performance
 * penalty. If font auto-detection is not desired, it can be disable using this
 * key.
 *
 * <p>
 * This work was authored by Keiron Liddle (keiron@aftexsw.com).
 * </p>
 */
@Slf4j
public class PDFTranscoder extends AbstractFOPTranscoder {

    /** Graphics2D instance that is used to paint to */
    protected PDFDocumentGraphics2D graphics = null;

    /**
     * Constructs a new {@link PDFTranscoder}.
     */
    public PDFTranscoder() {
        super();
        this.handler = new FOPErrorHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected UserAgent createUserAgent() {
        return new AbstractFOPTranscoder.FOPTranscoderUserAgent() {
            // The PDF stuff wants everything at 72dpi
            @Override
            public float getPixelUnitToMillimeter() {
                return super.getPixelUnitToMillimeter();
                // return 25.4f / 72; //72dpi = 0.352778f;
            }
        };
    }

    /**
     * Transcodes the specified Document as an image in the specified output.
     *
     * @param document
     *            the document to transcode
     * @param uri
     *            the uri of the document or null if any
     * @param output
     *            the ouput where to transcode
     * @exception TranscoderException
     *                if an error occured while transcoding
     */
    @Override
    protected void transcode(final Document document, final String uri,
            final TranscoderOutput output) throws TranscoderException {

        this.graphics = new PDFDocumentGraphics2D(isTextStroked());
        this.graphics
        .getPDFDocument()
        .getInfo()
        .setProducer(
                "Apache FOP Version " + Version.getVersion()
                + ": PDF Transcoder for Batik");
        if (this.hints.containsKey(KEY_DEVICE_RESOLUTION)) {
            this.graphics.setDeviceDPI(getDeviceResolution());
        }

        setupImageInfrastructure(uri);

        try {
            final Configuration effCfg = getEffectiveConfiguration();

            if (effCfg != null) {
                final PDFDocumentGraphics2DConfigurator configurator = new PDFDocumentGraphics2DConfigurator();
                final boolean useComplexScriptFeatures = false; // TODO - FIX ME
                configurator.configure(this.graphics, effCfg,
                        useComplexScriptFeatures);
            } else {
                this.graphics.setupDefaultFontInfo();
            }
        } catch (final Exception e) {
            throw new TranscoderException(
                    "Error while setting up PDFDocumentGraphics2D", e);
        }

        super.transcode(document, uri, output);

        if (log.isTraceEnabled()) {
            log.trace("document size: " + this.width + " x " + this.height);
        }

        // prepare the image to be painted
        final UnitProcessor.Context uctx = UnitProcessor.createContext(
                this.ctx, document.getDocumentElement());
        final float widthInPt = org.apache.batik.parser.UnitProcessor
                .userSpaceToSVG(
                        this.width,
                        SVGLength.SVG_LENGTHTYPE_PT,
                        org.apache.batik.parser.UnitProcessor.HORIZONTAL_LENGTH,
                        uctx);
        final int w = (int) (widthInPt + 0.5);
        final float heightInPt = org.apache.batik.parser.UnitProcessor
                .userSpaceToSVG(
                        this.height,
                        SVGLength.SVG_LENGTHTYPE_PT,
                        org.apache.batik.parser.UnitProcessor.HORIZONTAL_LENGTH,
                        uctx);
        final int h = (int) (heightInPt + 0.5);
        if (log.isTraceEnabled()) {
            log.trace("document size: " + w + "pt x " + h + "pt");
        }

        // prepare the image to be painted
        // int w = (int)(width + 0.5);
        // int h = (int)(height + 0.5);

        try {
            OutputStream out = output.getOutputStream();
            if (!(out instanceof BufferedOutputStream)) {
                out = new BufferedOutputStream(out);
            }
            this.graphics.setupDocument(out, w, h);
            this.graphics.setSVGDimension(this.width, this.height);

            if (this.hints.containsKey(ImageTranscoder.KEY_BACKGROUND_COLOR)) {
                this.graphics.setBackgroundColor((Color) this.hints
                        .get(ImageTranscoder.KEY_BACKGROUND_COLOR));
            }
            this.graphics
            .setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());
            this.graphics.preparePainting();

            this.graphics.transform(this.curTxf);
            this.graphics.setRenderingHint(
                    RenderingHintsKeyExt.KEY_TRANSCODING,
                    RenderingHintsKeyExt.VALUE_TRANSCODING_VECTOR);

            this.root.paint(this.graphics);

            this.graphics.finish();
        } catch (final IOException ex) {
            throw new TranscoderException(ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected BridgeContext createBridgeContext() {
        // For compatibility with Batik 1.6
        return createBridgeContext("1.x");
    }

    /** {@inheritDoc} */
    @Override
    public BridgeContext createBridgeContext(final String version) {
        FontInfo fontInfo = this.graphics.getFontInfo();
        if (isTextStroked()) {
            fontInfo = null;
        }
        final BridgeContext ctx = new PDFBridgeContext(this.userAgent,
                fontInfo, getImageManager(), getImageSessionContext());
        return ctx;
    }

}
