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

/* $Id: PSPainter.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.ps;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.fonts.LazyFont;
import org.apache.fop.fonts.MultiByteFont;
import org.apache.fop.fonts.SingleByteFont;
import org.apache.fop.fonts.Typeface;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.render.intermediate.AbstractIFPainter;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFState;
import org.apache.fop.render.intermediate.IFUtil;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.CharUtilities;
import org.apache.fop.util.HexEncoder;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageProcessingHints;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.ps.PSGenerator;
import org.apache.xmlgraphics.ps.PSResource;
import org.w3c.dom.Document;

/**
 * IFPainter implementation that produces PostScript.
 */
@Slf4j
public class PSPainter extends AbstractIFPainter {

    private final PSDocumentHandler documentHandler;
    private final PSBorderPainter borderPainter;

    private boolean inTextMode = false;

    /**
     * Default constructor.
     *
     * @param documentHandler
     *            the parent document handler
     */
    public PSPainter(final PSDocumentHandler documentHandler) {
        this(documentHandler, IFState.create());
    }

    protected PSPainter(final PSDocumentHandler documentHandler,
            final IFState state) {
        super();
        this.documentHandler = documentHandler;
        this.borderPainter = new PSBorderPainter(documentHandler.gen);
        this.state = state;
    }

    /** {@inheritDoc} */
    @Override
    protected IFContext getContext() {
        return this.documentHandler.getContext();
    }

    PSRenderingUtil getPSUtil() {
        return this.documentHandler.psUtil;
    }

    FontInfo getFontInfo() {
        return this.documentHandler.getFontInfo();
    }

    private PSGenerator getGenerator() {
        return this.documentHandler.gen;
    }

    /** {@inheritDoc} */
    @Override
    public void startViewport(final AffineTransform transform,
            final Dimension size, final Rectangle clipRect) throws IFException {
        try {
            final PSGenerator generator = getGenerator();
            saveGraphicsState();
            generator.concatMatrix(toPoints(transform));
        } catch (final IOException ioe) {
            throw new IFException("I/O error in startViewport()", ioe);
        }
        if (clipRect != null) {
            clipRect(clipRect);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endViewport() throws IFException {
        try {
            restoreGraphicsState();
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endViewport()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startGroup(final AffineTransform transform) throws IFException {
        try {
            final PSGenerator generator = getGenerator();
            saveGraphicsState();
            generator.concatMatrix(toPoints(transform));
        } catch (final IOException ioe) {
            throw new IFException("I/O error in startGroup()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endGroup() throws IFException {
        try {
            restoreGraphicsState();
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endGroup()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Map createDefaultImageProcessingHints(
            final ImageSessionContext sessionContext) {
        final Map hints = super
                .createDefaultImageProcessingHints(sessionContext);

        // PostScript doesn't support alpha channels
        hints.put(ImageProcessingHints.TRANSPARENCY_INTENT,
                ImageProcessingHints.TRANSPARENCY_INTENT_IGNORE);
        // TODO We might want to support image masks in the future.
        return hints;
    }

    /** {@inheritDoc} */
    @Override
    protected RenderingContext createRenderingContext() {
        final PSRenderingContext psContext = new PSRenderingContext(
                getUserAgent(), getGenerator(), getFontInfo());
        return psContext;
    }

    /** {@inheritDoc} */
    @Override
    protected void drawImageUsingImageHandler(final ImageInfo info,
            final Rectangle rect) throws ImageException, IOException {
        if (!getPSUtil().isOptimizeResources()
                || PSImageUtils.isImageInlined(info,
                        (PSRenderingContext) createRenderingContext())) {
            super.drawImageUsingImageHandler(info, rect);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Image " + info + " is embedded as a form later");
            }
            // Don't load image at this time, just put a form placeholder in the
            // stream
            final PSResource form = this.documentHandler.getFormForImage(info
                    .getOriginalURI());
            PSImageUtils.drawForm(form, info, rect, getGenerator());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final String uri, final Rectangle rect)
            throws IFException {
        try {
            endTextObject();
        } catch (final IOException ioe) {
            throw new IFException("I/O error in drawImage()", ioe);
        }
        drawImageUsingURI(uri, rect);
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final Document doc, final Rectangle rect)
            throws IFException {
        try {
            endTextObject();
        } catch (final IOException ioe) {
            throw new IFException("I/O error in drawImage()", ioe);
        }
        drawImageUsingDocument(doc, rect);
    }

    /** {@inheritDoc} */
    @Override
    public void clipRect(final Rectangle rect) throws IFException {
        try {
            final PSGenerator generator = getGenerator();
            endTextObject();
            generator.defineRect(rect.x / 1000.0, rect.y / 1000.0,
                    rect.width / 1000.0, rect.height / 1000.0);
            generator.writeln(generator.mapCommand("clip") + " "
                    + generator.mapCommand("newpath"));
        } catch (final IOException ioe) {
            throw new IFException("I/O error in clipRect()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void fillRect(final Rectangle rect, final Paint fill)
            throws IFException {
        if (fill == null) {
            return;
        }
        if (rect.width != 0 && rect.height != 0) {
            try {
                endTextObject();
                final PSGenerator generator = getGenerator();
                if (fill != null) {
                    if (fill instanceof Color) {
                        generator.useColor((Color) fill);
                    } else {
                        throw new UnsupportedOperationException(
                                "Non-Color paints NYI");
                    }
                }
                generator.defineRect(rect.x / 1000.0, rect.y / 1000.0,
                        rect.width / 1000.0, rect.height / 1000.0);
                generator.writeln(generator.mapCommand("fill"));
            } catch (final IOException ioe) {
                throw new IFException("I/O error in fillRect()", ioe);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawBorderRect(final Rectangle rect, final BorderProps top,
            final BorderProps bottom, final BorderProps left,
            final BorderProps right) throws IFException {
        if (top != null || bottom != null || left != null || right != null) {
            try {
                endTextObject();
                if (getPSUtil().getRenderingMode() == PSRenderingMode.SIZE
                        && hasOnlySolidBorders(top, bottom, left, right)) {
                    super.drawBorderRect(rect, top, bottom, left, right);
                } else {
                    this.borderPainter.drawBorders(rect, top, bottom, left,
                            right);
                }
            } catch (final IOException ioe) {
                throw new IFException("I/O error in drawBorderRect()", ioe);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawLine(final Point start, final Point end, final int width,
            final Color color, final RuleStyle style) throws IFException {
        try {
            endTextObject();
            this.borderPainter.drawLine(start, end, width, color, style);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in drawLine()", ioe);
        }
    }

    private Typeface getTypeface(final String fontName) {
        if (fontName == null) {
            throw new NullPointerException("fontName must not be null");
        }
        Typeface tf = getFontInfo().getFonts().get(fontName);
        if (tf instanceof LazyFont) {
            tf = ((LazyFont) tf).getRealFont();
        }
        return tf;
    }

    /**
     * Saves the graphics state of the rendering engine.
     *
     * @throws IOException
     *             if an I/O error occurs
     */
    protected void saveGraphicsState() throws IOException {
        endTextObject();
        getGenerator().saveGraphicsState();
    }

    /**
     * Restores the last graphics state of the rendering engine.
     *
     * @throws IOException
     *             if an I/O error occurs
     */
    protected void restoreGraphicsState() throws IOException {
        endTextObject();
        getGenerator().restoreGraphicsState();
    }

    /**
     * Indicates the beginning of a text object.
     *
     * @throws IOException
     *             if an I/O error occurs
     */
    protected void beginTextObject() throws IOException {
        if (!this.inTextMode) {
            final PSGenerator generator = getGenerator();
            generator.saveGraphicsState();
            generator.writeln("BT");
            this.inTextMode = true;
        }
    }

    /**
     * Indicates the end of a text object.
     *
     * @throws IOException
     *             if an I/O error occurs
     */
    protected void endTextObject() throws IOException {
        if (this.inTextMode) {
            this.inTextMode = false;
            final PSGenerator generator = getGenerator();
            generator.writeln("ET");
            generator.restoreGraphicsState();
        }
    }

    private String formatMptAsPt(final PSGenerator gen, final int value) {
        return gen.formatDouble(value / 1000.0);
    }

    /*
     * Disabled: performance experiment (incomplete)
     *
     * private static final String ZEROS = "0.00";
     *
     * private String formatMptAsPt1(int value) { String s =
     * Integer.toString(value); int len = s.length(); StringBuffer sb = new
     * StringBuffer(); if (len < 4) { sb.append(ZEROS.substring(0, 5 - len));
     * sb.append(s); } else { int dec = len - 3; sb.append(s.substring(0, dec));
     * sb.append('.'); sb.append(s.substring(dec)); } return sb.toString(); }
     */

    /** {@inheritDoc} */
    @Override
    public void drawText(final int x, final int y, final int letterSpacing,
            final int wordSpacing, final int[][] dp, final String text)
                    throws IFException {
        try {
            // Do not draw text if font-size is 0 as it creates an invalid
            // PostScript file
            if (this.state.getFontSize() == 0) {
                return;
            }
            final PSGenerator generator = getGenerator();
            generator.useColor(this.state.getTextColor());
            beginTextObject();
            final FontTriplet triplet = new FontTriplet(
                    this.state.getFontFamily(), this.state.getFontStyle(),
                    this.state.getFontWeight());
            // TODO Ignored: state.getFontVariant()
            // TODO Opportunity for font caching if font state is more heavily
            // used
            final String fontKey = getFontInfo().getInternalFontKey(triplet);
            if (fontKey == null) {
                throw new IFException("Font not available: " + triplet, null);
            }
            final int sizeMillipoints = this.state.getFontSize();

            // This assumes that *all* CIDFonts use a /ToUnicode mapping
            final Typeface tf = getTypeface(fontKey);
            SingleByteFont singleByteFont = null;
            if (tf instanceof SingleByteFont) {
                singleByteFont = (SingleByteFont) tf;
            }
            final Font font = getFontInfo().getFontInstance(triplet,
                    sizeMillipoints);

            useFont(fontKey, sizeMillipoints);

            generator.writeln("1 0 0 -1 " + formatMptAsPt(generator, x) + " "
                    + formatMptAsPt(generator, y) + " Tm");

            final int textLen = text.length();
            int start = 0;
            if (singleByteFont != null) {
                // Analyze string and split up in order to paint in different
                // sub-fonts/encodings
                int currentEncoding = -1;
                for (int i = 0; i < textLen; i++) {
                    final char c = text.charAt(i);
                    final char mapped = tf.mapChar(c);
                    final int encoding = mapped / 256;
                    if (currentEncoding != encoding) {
                        if (i > 0) {
                            writeText(text, start, i - start, letterSpacing,
                                    wordSpacing, dp, font, tf, false);
                        }
                        if (encoding == 0) {
                            useFont(fontKey, sizeMillipoints);
                        } else {
                            useFont(fontKey + "_" + Integer.toString(encoding),
                                    sizeMillipoints);
                        }
                        currentEncoding = encoding;
                        start = i;
                    }
                }
            } else {
                useFont(fontKey, sizeMillipoints);
            }
            writeText(text, start, textLen - start, letterSpacing, wordSpacing,
                    dp, font, tf, tf instanceof MultiByteFont);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in drawText()", ioe);
        }
    }

    private void writeText(final String text, final int start, final int len,
            final int letterSpacing, final int wordSpacing, final int[][] dp,
            final Font font, final Typeface tf, final boolean multiByte)
                    throws IOException {
        final PSGenerator generator = getGenerator();
        final int end = start + len;
        int initialSize = len;
        initialSize += initialSize / 2;

        final boolean hasLetterSpacing = letterSpacing != 0;
        boolean needTJ = false;

        int lineStart = 0;
        final StringBuffer accText = new StringBuffer(initialSize);
        final StringBuffer sb = new StringBuffer(initialSize);
        final int[] dx = IFUtil.convertDPToDX(dp);
        final int dxl = dx != null ? dx.length : 0;
        for (int i = start; i < end; i++) {
            final char orgChar = text.charAt(i);
            char ch;
            int cw;
            int glyphAdjust = 0;
            if (CharUtilities.isFixedWidthSpace(orgChar)) {
                // Fixed width space are rendered as spaces so copy/paste works
                // in a reader
                ch = font.mapChar(CharUtilities.SPACE);
                cw = font.getCharWidth(orgChar);
                glyphAdjust = font.getCharWidth(ch) - cw;
            } else {
                if (wordSpacing != 0
                        && CharUtilities.isAdjustableSpace(orgChar)) {
                    glyphAdjust -= wordSpacing;
                }
                ch = font.mapChar(orgChar);
                cw = font.getCharWidth(orgChar);
            }

            if (dx != null && i < dxl - 1) {
                glyphAdjust -= dx[i + 1];
            }
            if (multiByte) {
                accText.append(HexEncoder.encode(ch));
            } else {
                final char codepoint = (char) (ch % 256);
                PSGenerator.escapeChar(codepoint, accText); // add character to
                // accumulated text
            }
            if (glyphAdjust != 0) {
                needTJ = true;
                if (sb.length() == 0) {
                    sb.append('['); // Need to start TJ
                }
                if (accText.length() > 0) {
                    if (sb.length() - lineStart + accText.length() > 200) {
                        sb.append(PSGenerator.LF);
                        lineStart = sb.length();
                    }
                    lineStart = writePostScriptString(sb, accText, multiByte,
                            lineStart);
                    sb.append(' ');
                    accText.setLength(0); // reset accumulated text
                }
                sb.append(Integer.toString(glyphAdjust)).append(' ');
            }
        }
        if (needTJ) {
            if (accText.length() > 0) {
                if (sb.length() - lineStart + accText.length() > 200) {
                    sb.append(PSGenerator.LF);
                }
                writePostScriptString(sb, accText, multiByte);
            }
            if (hasLetterSpacing) {
                sb.append("] " + formatMptAsPt(generator, letterSpacing)
                        + " ATJ");
            } else {
                sb.append("] TJ");
            }
        } else {
            writePostScriptString(sb, accText, multiByte);
            if (hasLetterSpacing) {
                final StringBuffer spb = new StringBuffer();
                spb.append(formatMptAsPt(generator, letterSpacing)).append(
                        " 0 ");
                sb.insert(0, spb.toString());
                sb.append(" " + generator.mapCommand("ashow"));
            } else {
                sb.append(" " + generator.mapCommand("show"));
            }
        }
        generator.writeln(sb.toString());
    }

    private void writePostScriptString(final StringBuffer buffer,
            final StringBuffer string, final boolean multiByte) {
        writePostScriptString(buffer, string, multiByte, 0);
    }

    private int writePostScriptString(final StringBuffer buffer,
            final StringBuffer string, final boolean multiByte, int lineStart) {
        buffer.append(multiByte ? '<' : '(');
        final int l = string.length();
        int index = 0;
        final int maxCol = 200;
        buffer.append(string.substring(index, Math.min(index + maxCol, l)));
        index += maxCol;
        while (index < l) {
            if (!multiByte) {
                buffer.append('\\');
            }
            buffer.append(PSGenerator.LF);
            lineStart = buffer.length();
            buffer.append(string.substring(index, Math.min(index + maxCol, l)));
            index += maxCol;
        }
        buffer.append(multiByte ? '>' : ')');
        return lineStart;
    }

    private void useFont(final String key, final int size) throws IOException {
        final PSFontResource res = this.documentHandler
                .getPSResourceForFontKey(key);
        final PSGenerator generator = getGenerator();
        generator.useFont("/" + res.getName(), size / 1000f);
        res.notifyResourceUsageOnPage(generator.getResourceTracker());
    }

}
