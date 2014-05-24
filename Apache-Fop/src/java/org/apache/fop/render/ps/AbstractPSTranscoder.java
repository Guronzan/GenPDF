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

/* $Id: AbstractPSTranscoder.java 1297284 2012-03-05 23:29:29Z gadams $ */

package org.apache.fop.render.ps;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.UnitProcessor;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.svg.AbstractFOPTranscoder;
import org.apache.fop.svg.PDFDocumentGraphics2DConfigurator;
import org.apache.xmlgraphics.java2d.ps.AbstractPSDocumentGraphics2D;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGLength;

/**
 * <p>
 * This class enables to transcode an input to a PostScript document.
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
 * This work was authored by Keiron Liddle (keiron@aftexsw.com).
 * </p>
 */
@Slf4j
public abstract class AbstractPSTranscoder extends AbstractFOPTranscoder {

    /** the root Graphics2D instance for generating PostScript */
    protected AbstractPSDocumentGraphics2D graphics = null;

    private FontInfo fontInfo;

    /**
     * Constructs a new {@link AbstractPSTranscoder}.
     */
    public AbstractPSTranscoder() {
        super();
    }

    /**
     * Creates the root Graphics2D instance for generating PostScript.
     *
     * @return the root Graphics2D
     */
    protected abstract AbstractPSDocumentGraphics2D createDocumentGraphics2D();

    /** {@inheritDoc} */
    @Override
    protected boolean getAutoFontsDefault() {
        // Currently set to false because auto-fonts requires a lot of memory in
        // the PostScript
        // case: All fonts (even the unsupported TTF fonts) need to be loaded
        // and TrueType loading
        // is currently very memory-intensive. At default JVM memory settings,
        // this would result
        // in OutOfMemoryErrors otherwise.
        return false;
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

        this.graphics = createDocumentGraphics2D();
        if (!isTextStroked()) {
            try {
                final boolean useComplexScriptFeatures = false; // TODO - FIX ME
                this.fontInfo = PDFDocumentGraphics2DConfigurator
                        .createFontInfo(getEffectiveConfiguration(),
                                useComplexScriptFeatures);
                this.graphics.setCustomTextHandler(new NativeTextHandler(
                        this.graphics, this.fontInfo));
            } catch (final FOPException fe) {
                throw new TranscoderException(fe);
            }
        }

        super.transcode(document, uri, output);

        log.trace("document size: " + this.width + " x " + this.height);

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
        log.trace("document size: " + w + "pt x " + h + "pt");

        try {
            OutputStream out = output.getOutputStream();
            if (!(out instanceof BufferedOutputStream)) {
                out = new BufferedOutputStream(out);
            }
            this.graphics.setupDocument(out, w, h);
            this.graphics.setViewportDimension(this.width, this.height);

            if (this.hints.containsKey(ImageTranscoder.KEY_BACKGROUND_COLOR)) {
                this.graphics.setBackgroundColor((Color) this.hints
                        .get(ImageTranscoder.KEY_BACKGROUND_COLOR));
            }
            this.graphics
            .setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());
            this.graphics.setTransform(this.curTxf);

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
        final BridgeContext ctx = new PSBridgeContext(this.userAgent,
                isTextStroked() ? null : this.fontInfo, getImageManager(),
                        getImageSessionContext());
        return ctx;
    }

}
