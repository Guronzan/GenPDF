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

/* $Id: AFPPainter.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.render.afp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.fop.afp.AFPBorderPainter;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPUnitConverter;
import org.apache.fop.afp.AbstractAFPPainter;
import org.apache.fop.afp.BorderPaintingInfo;
import org.apache.fop.afp.DataStream;
import org.apache.fop.afp.RectanglePaintingInfo;
import org.apache.fop.afp.fonts.AFPFont;
import org.apache.fop.afp.fonts.AFPFontAttributes;
import org.apache.fop.afp.fonts.AFPPageFonts;
import org.apache.fop.afp.fonts.CharacterSet;
import org.apache.fop.afp.modca.AbstractPageObject;
import org.apache.fop.afp.modca.PresentationTextObject;
import org.apache.fop.afp.ptoca.PtocaBuilder;
import org.apache.fop.afp.ptoca.PtocaProducer;
import org.apache.fop.afp.util.DefaultFOPResourceAccessor;
import org.apache.fop.afp.util.ResourceAccessor;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.fonts.Typeface;
import org.apache.fop.render.RenderingContext;
import org.apache.fop.render.intermediate.AbstractIFPainter;
import org.apache.fop.render.intermediate.BorderPainter;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFState;
import org.apache.fop.render.intermediate.IFUtil;
import org.apache.fop.traits.BorderProps;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.CharUtilities;
import org.apache.xmlgraphics.image.loader.ImageProcessingHints;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.w3c.dom.Document;

/**
 * IFPainter implementation that produces AFP (MO:DCA).
 */
public class AFPPainter extends AbstractIFPainter {

    // ** logging instance */
    // private static Log log = LogFactory.getLog(AFPPainter.class);

    private static final int X = 0;
    private static final int Y = 1;

    private final AFPDocumentHandler documentHandler;

    /** the border painter */
    private final AFPBorderPainterAdapter borderPainter;
    /** the rectangle painter */
    private final AbstractAFPPainter rectanglePainter;

    /** unit converter */
    private final AFPUnitConverter unitConv;

    /**
     * Default constructor.
     * 
     * @param documentHandler
     *            the parent document handler
     */
    public AFPPainter(final AFPDocumentHandler documentHandler) {
        super();
        this.documentHandler = documentHandler;
        this.state = IFState.create();
        this.borderPainter = new AFPBorderPainterAdapter(new AFPBorderPainter(
                getPaintingState(), getDataStream()));
        this.rectanglePainter = documentHandler.createRectanglePainter();
        this.unitConv = getPaintingState().getUnitConverter();
    }

    /** {@inheritDoc} */
    @Override
    protected IFContext getContext() {
        return this.documentHandler.getContext();
    }

    FontInfo getFontInfo() {
        return this.documentHandler.getFontInfo();
    }

    AFPPaintingState getPaintingState() {
        return this.documentHandler.getPaintingState();
    }

    DataStream getDataStream() {
        return this.documentHandler.getDataStream();
    }

    /** {@inheritDoc} */
    @Override
    public void startViewport(final AffineTransform transform,
            final Dimension size, final Rectangle clipRect) throws IFException {
        // AFP doesn't support clipping, so we treat viewport like a group
        // this is the same code as for startGroup()
        try {
            saveGraphicsState();
            concatenateTransformationMatrix(transform);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in startViewport()", ioe);
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

    private void concatenateTransformationMatrix(final AffineTransform at) {
        if (!at.isIdentity()) {
            getPaintingState().concatenate(at);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startGroup(final AffineTransform transform) throws IFException {
        try {
            saveGraphicsState();
            concatenateTransformationMatrix(transform);
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

        // AFP doesn't support alpha channels
        hints.put(ImageProcessingHints.TRANSPARENCY_INTENT,
                ImageProcessingHints.TRANSPARENCY_INTENT_IGNORE);
        return hints;
    }

    /** {@inheritDoc} */
    @Override
    protected RenderingContext createRenderingContext() {
        final AFPRenderingContext psContext = new AFPRenderingContext(
                getUserAgent(), this.documentHandler.getResourceManager(),
                getPaintingState(), getFontInfo(), getContext()
                        .getForeignAttributes());
        return psContext;
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final String uri, final Rectangle rect)
            throws IFException {
        final PageSegmentDescriptor pageSegment = this.documentHandler
                .getPageSegmentNameFor(uri);

        if (pageSegment != null) {
            final float[] srcPts = { rect.x, rect.y };
            final int[] coords = this.unitConv.mpts2units(srcPts);
            final int width = Math.round(this.unitConv.mpt2units(rect.width));
            final int height = Math.round(this.unitConv.mpt2units(rect.height));

            getDataStream().createIncludePageSegment(pageSegment.getName(),
                    coords[X], coords[Y], width, height);

            // Do we need to embed an external page segment?
            if (pageSegment.getURI() != null) {
                final ResourceAccessor accessor = new DefaultFOPResourceAccessor(
                        this.documentHandler.getUserAgent(), null, null);
                try {
                    final URI resourceUri = new URI(pageSegment.getURI());
                    this.documentHandler.getResourceManager()
                            .createIncludedResourceFromExternal(
                                    pageSegment.getName(), resourceUri,
                                    accessor);

                } catch (final URISyntaxException urie) {
                    throw new IFException("Could not handle resource url"
                            + pageSegment.getURI(), urie);
                } catch (final IOException ioe) {
                    throw new IFException("Could not handle resource"
                            + pageSegment.getURI(), ioe);
                }
            }

        } else {
            drawImageUsingURI(uri, rect);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawImage(final Document doc, final Rectangle rect)
            throws IFException {
        drawImageUsingDocument(doc, rect);
    }

    /** {@inheritDoc} */
    @Override
    public void clipRect(final Rectangle rect) throws IFException {
        // Not supported!
    }

    private float toPoint(final int mpt) {
        return mpt / 1000f;
    }

    /** {@inheritDoc} */
    @Override
    public void fillRect(final Rectangle rect, final Paint fill)
            throws IFException {
        if (fill == null) {
            return;
        }
        if (rect.width != 0 && rect.height != 0) {
            if (fill instanceof Color) {
                getPaintingState().setColor((Color) fill);
            } else {
                throw new UnsupportedOperationException("Non-Color paints NYI");
            }
            final RectanglePaintingInfo rectanglePaintInfo = new RectanglePaintingInfo(
                    toPoint(rect.x), toPoint(rect.y), toPoint(rect.width),
                    toPoint(rect.height));
            try {
                this.rectanglePainter.paint(rectanglePaintInfo);
            } catch (final IOException ioe) {
                throw new IFException("IO error while painting rectangle", ioe);
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
                this.borderPainter.drawBorders(rect, top, bottom, left, right);
            } catch (final IOException ife) {
                throw new IFException("IO error while painting borders", ife);
            }
        }
    }

    // TODO Try to resolve the name-clash between the AFPBorderPainter in the
    // afp package
    // and this one. Not done for now to avoid a lot of re-implementation and
    // code duplication.
    private static class AFPBorderPainterAdapter extends BorderPainter {

        private final AFPBorderPainter delegate;

        public AFPBorderPainterAdapter(final AFPBorderPainter borderPainter) {
            this.delegate = borderPainter;
        }

        @Override
        protected void clip() throws IOException {
            // not supported by AFP
        }

        @Override
        protected void closePath() throws IOException {
            // used for clipping only, so not implemented
        }

        @Override
        protected void moveTo(final int x, final int y) throws IOException {
            // used for clipping only, so not implemented
        }

        @Override
        protected void lineTo(final int x, final int y) throws IOException {
            // used for clipping only, so not implemented
        }

        @Override
        protected void saveGraphicsState() throws IOException {
            // used for clipping only, so not implemented
        }

        @Override
        protected void restoreGraphicsState() throws IOException {
            // used for clipping only, so not implemented
        }

        private float toPoints(final int mpt) {
            return mpt / 1000f;
        }

        @Override
        protected void drawBorderLine(
                // CSOK: ParameterNumber
                final int x1, final int y1, final int x2, final int y2,
                final boolean horz, final boolean startOrBefore,
                final int style, final Color color) throws IOException {
            final BorderPaintingInfo borderPaintInfo = new BorderPaintingInfo(
                    toPoints(x1), toPoints(y1), toPoints(x2), toPoints(y2),
                    horz, style, color);
            this.delegate.paint(borderPaintInfo);
        }

        @Override
        public void drawLine(final Point start, final Point end,
                final int width, final Color color, final RuleStyle style)
                throws IOException {
            if (start.y != end.y) {
                // TODO Support arbitrary lines if necessary
                throw new UnsupportedOperationException(
                        "Can only deal with horizontal lines right now");
            }

            // Simply delegates to drawBorderLine() as AFP line painting is not
            // very sophisticated.
            final int halfWidth = width / 2;
            drawBorderLine(start.x, start.y - halfWidth, end.x, start.y
                    + halfWidth, true, true, style.getEnumValue(), color);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void drawLine(final Point start, final Point end, final int width,
            final Color color, final RuleStyle style) throws IFException {
        try {
            this.borderPainter.drawLine(start, end, width, color, style);
        } catch (final IOException ioe) {
            throw new IFException("I/O error in drawLine()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawText(
            // CSOK: MethodLength
            final int x, final int y, final int letterSpacing,
            final int wordSpacing, final int[][] dp, final String text)
            throws IFException {
        final int fontSize = this.state.getFontSize();
        getPaintingState().setFontSize(fontSize);

        FontTriplet triplet = new FontTriplet(this.state.getFontFamily(),
                this.state.getFontStyle(), this.state.getFontWeight());
        // TODO Ignored: state.getFontVariant()
        String fontKey = getFontInfo().getInternalFontKey(triplet);
        if (fontKey == null) {
            triplet = new FontTriplet("any", Font.STYLE_NORMAL,
                    Font.WEIGHT_NORMAL);
            fontKey = getFontInfo().getInternalFontKey(triplet);
        }

        // register font as necessary
        final Map<String, Typeface> fontMetricMap = this.documentHandler
                .getFontInfo().getFonts();
        final AFPFont afpFont = (AFPFont) fontMetricMap.get(fontKey);
        final Font font = getFontInfo().getFontInstance(triplet, fontSize);
        final AFPPageFonts pageFonts = getPaintingState().getPageFonts();
        final AFPFontAttributes fontAttributes = pageFonts.registerFont(
                fontKey, afpFont, fontSize);

        final int fontReference = fontAttributes.getFontReference();

        final int[] coords = this.unitConv.mpts2units(new float[] { x, y });

        final CharacterSet charSet = afpFont.getCharacterSet(fontSize);

        if (afpFont.isEmbeddable()) {
            try {
                this.documentHandler.getResourceManager().embedFont(afpFont,
                        charSet);
            } catch (final IOException ioe) {
                throw new IFException("Error while embedding font resources",
                        ioe);
            }
        }

        final AbstractPageObject page = getDataStream().getCurrentPage();
        final PresentationTextObject pto = page.getPresentationTextObject();
        try {
            pto.createControlSequences(new PtocaProducer() {

                @Override
                public void produce(final PtocaBuilder builder)
                        throws IOException {
                    final Point p = getPaintingState().getPoint(coords[X],
                            coords[Y]);
                    builder.setTextOrientation(getPaintingState().getRotation());
                    builder.absoluteMoveBaseline(p.y);
                    builder.absoluteMoveInline(p.x);

                    builder.setExtendedTextColor(AFPPainter.this.state
                            .getTextColor());
                    builder.setCodedFont((byte) fontReference);

                    final int l = text.length();
                    final int[] dx = IFUtil.convertDPToDX(dp);
                    final int dxl = dx != null ? dx.length : 0;
                    final StringBuilder sb = new StringBuilder();

                    if (dxl > 0 && dx[0] != 0) {
                        final int dxu = Math.round(AFPPainter.this.unitConv
                                .mpt2units(dx[0]));
                        builder.relativeMoveInline(-dxu);
                    }

                    // Following are two variants for glyph placement.
                    // SVI does not seem to be implemented in the same way
                    // everywhere, so
                    // a fallback alternative is preserved here.
                    final boolean usePTOCAWordSpacing = true;
                    if (usePTOCAWordSpacing) {

                        int interCharacterAdjustment = 0;
                        if (letterSpacing != 0) {
                            interCharacterAdjustment = Math
                                    .round(AFPPainter.this.unitConv
                                            .mpt2units(letterSpacing));
                        }
                        builder.setInterCharacterAdjustment(interCharacterAdjustment);

                        final int spaceWidth = font
                                .getCharWidth(CharUtilities.SPACE);
                        final int fixedSpaceCharacterIncrement = Math
                                .round(AFPPainter.this.unitConv
                                        .mpt2units(spaceWidth + letterSpacing));
                        int varSpaceCharacterIncrement = fixedSpaceCharacterIncrement;
                        if (wordSpacing != 0) {
                            varSpaceCharacterIncrement = Math
                                    .round(AFPPainter.this.unitConv
                                            .mpt2units(spaceWidth + wordSpacing
                                                    + letterSpacing));
                        }
                        builder.setVariableSpaceCharacterIncrement(varSpaceCharacterIncrement);

                        boolean fixedSpaceMode = false;

                        for (int i = 0; i < l; i++) {
                            final char orgChar = text.charAt(i);
                            float glyphAdjust = 0;
                            if (CharUtilities.isFixedWidthSpace(orgChar)) {
                                flushText(builder, sb, charSet);
                                builder.setVariableSpaceCharacterIncrement(fixedSpaceCharacterIncrement);
                                fixedSpaceMode = true;
                                sb.append(CharUtilities.SPACE);
                                final int charWidth = font
                                        .getCharWidth(orgChar);
                                glyphAdjust += charWidth - spaceWidth;
                            } else {
                                if (fixedSpaceMode) {
                                    flushText(builder, sb, charSet);
                                    builder.setVariableSpaceCharacterIncrement(varSpaceCharacterIncrement);
                                    fixedSpaceMode = false;
                                }
                                char ch;
                                if (orgChar == CharUtilities.NBSPACE) {
                                    ch = ' '; // converted to normal space to
                                              // allow word spacing
                                } else {
                                    ch = orgChar;
                                }
                                sb.append(ch);
                            }

                            if (i < dxl - 1) {
                                glyphAdjust += dx[i + 1];
                            }

                            if (glyphAdjust != 0) {
                                flushText(builder, sb, charSet);
                                final int increment = Math
                                        .round(AFPPainter.this.unitConv
                                                .mpt2units(glyphAdjust));
                                builder.relativeMoveInline(increment);
                            }
                        }
                    } else {
                        for (int i = 0; i < l; i++) {
                            final char orgChar = text.charAt(i);
                            float glyphAdjust = 0;
                            if (CharUtilities.isFixedWidthSpace(orgChar)) {
                                sb.append(CharUtilities.SPACE);
                                final int spaceWidth = font
                                        .getCharWidth(CharUtilities.SPACE);
                                final int charWidth = font
                                        .getCharWidth(orgChar);
                                glyphAdjust += charWidth - spaceWidth;
                            } else {
                                sb.append(orgChar);
                            }

                            if (wordSpacing != 0
                                    && CharUtilities.isAdjustableSpace(orgChar)) {
                                glyphAdjust += wordSpacing;
                            }
                            glyphAdjust += letterSpacing;
                            if (i < dxl - 1) {
                                glyphAdjust += dx[i + 1];
                            }

                            if (glyphAdjust != 0) {
                                flushText(builder, sb, charSet);
                                final int increment = Math
                                        .round(AFPPainter.this.unitConv
                                                .mpt2units(glyphAdjust));
                                builder.relativeMoveInline(increment);
                            }
                        }
                    }
                    flushText(builder, sb, charSet);
                }

                private void flushText(final PtocaBuilder builder,
                        final StringBuilder sb, final CharacterSet charSet)
                        throws IOException {
                    if (sb.length() > 0) {
                        builder.addTransparentData(charSet.encodeChars(sb));
                        sb.setLength(0);
                    }
                }

            });
        } catch (final IOException ioe) {
            throw new IFException("I/O error in drawText()", ioe);
        }
    }

    /**
     * Saves the graphics state of the rendering engine.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    protected void saveGraphicsState() throws IOException {
        getPaintingState().save();
    }

    /**
     * Restores the last graphics state of the rendering engine.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    protected void restoreGraphicsState() throws IOException {
        getPaintingState().restore();
    }

}
