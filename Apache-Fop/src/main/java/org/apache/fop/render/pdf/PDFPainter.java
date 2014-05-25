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

/* $Id: PDFPainter.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.render.pdf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.fonts.LazyFont;
import org.apache.fop.fonts.SingleByteFont;
import org.apache.fop.fonts.Typeface;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFNumber;
import org.apache.fop.pdf.PDFStructElem;
import org.apache.fop.pdf.PDFTextUtil;
import org.apache.fop.pdf.PDFXObject;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.render.intermediate.AbstractIFPainter;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFState;
import org.apache.fop.render.intermediate.IFUtil;
import org.apache.fop.render.pdf.PDFLogicalStructureHandler.MarkedContentInfo;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.traits.Direction;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.CharUtilities;
import org.w3c.dom.Document;

/**
 * IFPainter implementation that produces PDF.
 */
public class PDFPainter extends AbstractIFPainter {

    private final PDFDocumentHandler documentHandler;

    /** The current content generator */
    protected PDFContentGenerator generator;

    private final PDFBorderPainter borderPainter;

    private final boolean accessEnabled;

    private MarkedContentInfo imageMCI;

    private final PDFLogicalStructureHandler logicalStructureHandler;

    /**
     * Default constructor.
     *
     * @param documentHandler
     *            the parent document handler
     * @param logicalStructureHandler
     *            the logical structure handler
     */
    public PDFPainter(final PDFDocumentHandler documentHandler,
            final PDFLogicalStructureHandler logicalStructureHandler) {
        super();
        this.documentHandler = documentHandler;
        this.logicalStructureHandler = logicalStructureHandler;
        this.generator = documentHandler.generator;
        this.borderPainter = new PDFBorderPainter(this.generator);
        this.state = IFState.create();
        this.accessEnabled = getUserAgent().isAccessibilityEnabled();
    }

    /** {@inheritDoc} */
    @Override
    protected IFContext getContext() {
        return this.documentHandler.getContext();
    }

    PDFRenderingUtil getPDFUtil() {
        return this.documentHandler.pdfUtil;
    }

    PDFDocument getPDFDoc() {
        return this.documentHandler.pdfDoc;
    }

    FontInfo getFontInfo() {
        return this.documentHandler.getFontInfo();
    }

    /** {@inheritDoc} */
    @Override
    public void startViewport(final AffineTransform transform,
            final Dimension size, final Rectangle clipRect) throws IFException {
        this.generator.saveGraphicsState();
        this.generator.concatenate(toPoints(transform));
        if (clipRect != null) {
            clipRect(clipRect);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endViewport() throws IFException {
        this.generator.restoreGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    public void startGroup(final AffineTransform transform) throws IFException {
        this.generator.saveGraphicsState();
        this.generator.concatenate(toPoints(transform));
    }

    /** {@inheritDoc} */
    @Override
    public void endGroup() throws IFException {
        this.generator.restoreGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final String uri, final Rectangle rect)
            throws IFException {
        final PDFXObject xobject = getPDFDoc().getXObject(uri);
        if (xobject != null) {
            if (this.accessEnabled) {
                final PDFStructElem structElem = (PDFStructElem) getContext()
                        .getStructureTreeElement();
                prepareImageMCID(structElem);
                placeImageAccess(rect, xobject);
            } else {
                placeImage(rect, xobject);
            }
        } else {
            if (this.accessEnabled) {
                final PDFStructElem structElem = (PDFStructElem) getContext()
                        .getStructureTreeElement();
                prepareImageMCID(structElem);
            }
            drawImageUsingURI(uri, rect);
            flushPDFDoc();
        }
    }

    private void prepareImageMCID(final PDFStructElem structElem) {
        this.imageMCI = this.logicalStructureHandler
                .addImageContentItem(structElem);
    }

    /** {@inheritDoc} */
    @Override
    protected RenderingContext createRenderingContext() {
        final PDFRenderingContext pdfContext = new PDFRenderingContext(
                getUserAgent(), this.generator,
                this.documentHandler.currentPage, getFontInfo());
        pdfContext.setMarkedContentInfo(this.imageMCI);
        return pdfContext;
    }

    /**
     * Places a previously registered image at a certain place on the page.
     *
     * @param rect
     *            the rectangle for the image
     * @param xobj
     *            the image XObject
     */
    private void placeImage(final Rectangle rect, final PDFXObject xobj) {
        this.generator.saveGraphicsState();
        this.generator.add(format(rect.width) + " 0 0 " + format(-rect.height)
                + " " + format(rect.x) + " " + format(rect.y + rect.height)
                + " cm " + xobj.getName() + " Do\n");
        this.generator.restoreGraphicsState();
    }

    /**
     * Places a previously registered image at a certain place on the page -
     * Accessibility version
     *
     * @param rect
     *            the rectangle for the image
     * @param xobj
     *            the image XObject
     */
    private void placeImageAccess(final Rectangle rect, final PDFXObject xobj) {
        this.generator.saveGraphicsState(this.imageMCI.tag, this.imageMCI.mcid);
        this.generator.add(format(rect.width) + " 0 0 " + format(-rect.height)
                + " " + format(rect.x) + " " + format(rect.y + rect.height)
                + " cm " + xobj.getName() + " Do\n");
        this.generator.restoreGraphicsStateAccess();
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final Document doc, final Rectangle rect)
            throws IFException {
        if (this.accessEnabled) {
            final PDFStructElem structElem = (PDFStructElem) getContext()
                    .getStructureTreeElement();
            prepareImageMCID(structElem);
        }
        drawImageUsingDocument(doc, rect);
        flushPDFDoc();
    }

    private void flushPDFDoc() throws IFException {
        // output new data
        try {
            this.generator.flushPDFDoc();
        } catch (final IOException ioe) {
            throw new IFException("I/O error flushing the PDF document", ioe);
        }
    }

    /**
     * Formats a integer value (normally coordinates in millipoints) to a
     * String.
     *
     * @param value
     *            the value (in millipoints)
     * @return the formatted value
     */
    protected static String format(final int value) {
        return PDFNumber.doubleOut(value / 1000f);
    }

    /** {@inheritDoc} */
    @Override
    public void clipRect(final Rectangle rect) throws IFException {
        this.generator.endTextObject();
        this.generator.clipRect(rect);
    }

    /** {@inheritDoc} */
    @Override
    public void fillRect(final Rectangle rect, final Paint fill)
            throws IFException {
        if (fill == null) {
            return;
        }
        if (rect.width != 0 && rect.height != 0) {
            this.generator.endTextObject();
            if (fill instanceof Color) {
                this.generator.updateColor((Color) fill, true, null);
            } else {
                throw new UnsupportedOperationException("Non-Color paints NYI");
            }
            final StringBuilder sb = new StringBuilder();
            sb.append(format(rect.x)).append(' ');
            sb.append(format(rect.y)).append(' ');
            sb.append(format(rect.width)).append(' ');
            sb.append(format(rect.height)).append(" re");
            sb.append(" f");
            /*
             * Removed from method signature as it is currently not used if
             * (stroke != null) { sb.append(" S"); }
             */
            sb.append('\n');
            this.generator.add(sb.toString());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawBorderRect(final Rectangle rect, final BorderProps top,
            final BorderProps bottom, final BorderProps left,
            final BorderProps right) throws IFException {
        if (top != null || bottom != null || left != null || right != null) {
            this.generator.endTextObject();
            try {
                this.borderPainter.drawBorders(rect, top, bottom, left, right);
            } catch (final IOException ioe) {
                throw new IFException("I/O error while drawing borders", ioe);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawLine(final Point start, final Point end, final int width,
            final Color color, final RuleStyle style) throws IFException {
        this.generator.endTextObject();
        this.borderPainter.drawLine(start, end, width, color, style);
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

    /** {@inheritDoc} */
    @Override
    public void drawText(final int x, final int y, final int letterSpacing,
            final int wordSpacing, final int[][] dp, final String text)
            throws IFException {
        if (this.accessEnabled) {
            final PDFStructElem structElem = (PDFStructElem) getContext()
                    .getStructureTreeElement();
            final MarkedContentInfo mci = this.logicalStructureHandler
                    .addTextContentItem(structElem);
            if (this.generator.getTextUtil().isInTextObject()) {
                this.generator.separateTextElements(mci.tag, mci.mcid);
            }
            this.generator.updateColor(this.state.getTextColor(), true, null);
            this.generator.beginTextObject(mci.tag, mci.mcid);
        } else {
            this.generator.updateColor(this.state.getTextColor(), true, null);
            this.generator.beginTextObject();
        }

        final FontTriplet triplet = new FontTriplet(this.state.getFontFamily(),
                this.state.getFontStyle(), this.state.getFontWeight());

        if (dp == null || IFUtil.isDPOnlyDX(dp)) {
            drawTextWithDX(x, y, text, triplet, letterSpacing, wordSpacing,
                    IFUtil.convertDPToDX(dp));
        } else {
            drawTextWithDP(x, y, text, triplet, letterSpacing, wordSpacing, dp);
        }
    }

    private void drawTextWithDX(final int x, final int y, final String text,
            final FontTriplet triplet, final int letterSpacing,
            final int wordSpacing, final int[] dx) {

        // TODO Ignored: state.getFontVariant()
        // TODO Opportunity for font caching if font state is more heavily used
        final String fontKey = getFontInfo().getInternalFontKey(triplet);
        final int sizeMillipoints = this.state.getFontSize();
        final float fontSize = sizeMillipoints / 1000f;

        // This assumes that *all* CIDFonts use a /ToUnicode mapping
        final Typeface tf = getTypeface(fontKey);
        SingleByteFont singleByteFont = null;
        if (tf instanceof SingleByteFont) {
            singleByteFont = (SingleByteFont) tf;
        }
        final Font font = getFontInfo().getFontInstance(triplet,
                sizeMillipoints);
        final String fontName = font.getFontName();

        final PDFTextUtil textutil = this.generator.getTextUtil();
        textutil.updateTf(fontKey, fontSize, tf.isMultiByte());

        this.generator.updateCharacterSpacing(letterSpacing / 1000f);

        textutil.writeTextMatrix(new AffineTransform(1, 0, 0, -1, x / 1000f,
                y / 1000f));
        final int l = text.length();
        final int dxl = dx != null ? dx.length : 0;

        if (dx != null && dxl > 0 && dx[0] != 0) {
            textutil.adjustGlyphTJ(-dx[0] / fontSize);
        }
        for (int i = 0; i < l; i++) {
            final char orgChar = text.charAt(i);
            char ch;
            float glyphAdjust = 0;
            if (font.hasChar(orgChar)) {
                ch = font.mapChar(orgChar);
                ch = selectAndMapSingleByteFont(singleByteFont, fontName,
                        fontSize, textutil, ch);
                if (wordSpacing != 0
                        && CharUtilities.isAdjustableSpace(orgChar)) {
                    glyphAdjust += wordSpacing;
                }
            } else {
                if (CharUtilities.isFixedWidthSpace(orgChar)) {
                    // Fixed width space are rendered as spaces so copy/paste
                    // works in a reader
                    ch = font.mapChar(CharUtilities.SPACE);
                    final int spaceDiff = font.getCharWidth(ch)
                            - font.getCharWidth(orgChar);
                    glyphAdjust = -spaceDiff;
                } else {
                    ch = font.mapChar(orgChar);
                    if (wordSpacing != 0
                            && CharUtilities.isAdjustableSpace(orgChar)) {
                        glyphAdjust += wordSpacing;
                    }
                }
                ch = selectAndMapSingleByteFont(singleByteFont, fontName,
                        fontSize, textutil, ch);
            }
            textutil.writeTJMappedChar(ch);

            if (dx != null && i < dxl - 1) {
                glyphAdjust += dx[i + 1];
            }

            if (glyphAdjust != 0) {
                textutil.adjustGlyphTJ(-glyphAdjust / fontSize);
            }

        }
        textutil.writeTJ();
    }

    private static int[] paZero = new int[4];

    private void drawTextWithDP(final int x, final int y, final String text,
            final FontTriplet triplet, final int letterSpacing,
            final int wordSpacing, final int[][] dp) {
        assert text != null;
        assert triplet != null;
        assert dp != null;
        final String fk = getFontInfo().getInternalFontKey(triplet);
        final Typeface tf = getTypeface(fk);
        if (tf.isMultiByte()) {
            final int fs = this.state.getFontSize();
            final float fsPoints = fs / 1000f;
            final Font f = getFontInfo().getFontInstance(triplet, fs);
            // String fn = f.getFontName();
            final PDFTextUtil tu = this.generator.getTextUtil();
            double xc = 0f;
            double yc = 0f;
            double xoLast = 0f;
            double yoLast = 0f;
            final double wox = wordSpacing;
            tu.writeTextMatrix(new AffineTransform(1, 0, 0, -1, x / 1000f,
                    y / 1000f));
            tu.updateTf(fk, fsPoints, true);
            this.generator.updateCharacterSpacing(letterSpacing / 1000f);
            for (int i = 0, n = text.length(); i < n; i++) {
                final char ch = text.charAt(i);
                final int[] pa = i < dp.length ? dp[i] : paZero;
                final double xo = xc + pa[0];
                final double yo = yc + pa[1];
                final double xa = f.getCharWidth(ch)
                        + maybeWordOffsetX(wox, ch, null);
                final double ya = 0;
                final double xd = (xo - xoLast) / 1000f;
                final double yd = (yo - yoLast) / 1000f;
                tu.writeTd(xd, yd);
                tu.writeTj(f.mapChar(ch));
                xc += xa + pa[2];
                yc += ya + pa[3];
                xoLast = xo;
                yoLast = yo;
            }
        }
    }

    private double maybeWordOffsetX(final double wox, final char ch,
            final Direction dir) {
        if (wox != 0 && CharUtilities.isAdjustableSpace(ch)
                && (dir == null || dir.isHorizontal())) {
            return wox;
        } else {
            return 0;
        }
    }

    /*
     * private double maybeWordOffsetY ( double woy, char ch, Direction dir ) {
     * if ( ( woy != 0 ) && CharUtilities.isAdjustableSpace ( ch ) &&
     * dir.isVertical() && ( ( dir != null ) && dir.isVertical() ) ) { return
     * woy; } else { return 0; } }
     */

    private char selectAndMapSingleByteFont(
            final SingleByteFont singleByteFont, final String fontName,
            final float fontSize, final PDFTextUtil textutil, char ch) {
        if (singleByteFont != null && singleByteFont.hasAdditionalEncodings()) {
            final int encoding = ch / 256;
            if (encoding == 0) {
                textutil.updateTf(fontName, fontSize,
                        singleByteFont.isMultiByte());
            } else {
                textutil.updateTf(fontName + "_" + Integer.toString(encoding),
                        fontSize, singleByteFont.isMultiByte());
                ch = (char) (ch % 256);
            }
        }
        return ch;
    }

}
