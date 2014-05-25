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

/* $Id: AbstractPathOrientedRenderer.java 1297008 2012-03-05 11:19:47Z vhennebert $ */

package org.apache.fop.render;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;

import org.apache.batik.parser.AWTTransformProducer;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.CTM;
import org.apache.fop.area.NormalFlow;
import org.apache.fop.area.RegionReference;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.ForeignObject;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineViewport;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.extensions.ExtensionElementMapping;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.traits.BorderProps;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.util.QName;
import org.apache.xmlgraphics.util.UnitConv;
import org.w3c.dom.Document;

/**
 * Abstract base class for renderers like PDF and PostScript where many painting
 * operations follow similar patterns which makes it possible to share some
 * code.
 */
public abstract class AbstractPathOrientedRenderer extends PrintRenderer {

    /**
     * @param userAgent
     *            the user agent that contains configuration details. This
     *            cannot be null.
     */
    public AbstractPathOrientedRenderer(final FOUserAgent userAgent) {
        super(userAgent);
    }

    /**
     * Handle block traits. The block could be any sort of block with any
     * positioning so this should render the traits such as border and
     * background in its position.
     *
     * @param block
     *            the block to render the traits
     */
    @Override
    protected void handleBlockTraits(final Block block) {
        final float borderPaddingStart = block.getBorderAndPaddingWidthStart() / 1000f;
        final float borderPaddingEnd = block.getBorderAndPaddingWidthEnd() / 1000f;
        final float borderPaddingBefore = block
                .getBorderAndPaddingWidthBefore() / 1000f;
        final float borderPaddingAfter = block.getBorderAndPaddingWidthAfter() / 1000f;

        float startx = this.currentIPPosition / 1000f;
        final float starty = this.currentBPPosition / 1000f;
        float width = block.getIPD() / 1000f;
        float height = block.getBPD() / 1000f;

        final int level = block.getBidiLevel();
        if (level == -1 || (level & 1) == 0) {
            startx += block.getStartIndent() / 1000f;
            startx -= borderPaddingStart;
        } else {
            startx += block.getEndIndent() / 1000f;
            startx -= borderPaddingEnd;
        }

        width += borderPaddingStart;
        width += borderPaddingEnd;
        height += borderPaddingBefore;
        height += borderPaddingAfter;

        drawBackAndBorders(block, startx, starty, width, height);
    }

    /**
     * Handle the traits for a region This is used to draw the traits for the
     * given page region. (See Sect. 6.4.1.2 of XSL-FO spec.)
     * 
     * @param region
     *            the RegionViewport whose region is to be drawn
     */
    @Override
    protected void handleRegionTraits(final RegionViewport region) {
        final Rectangle2D viewArea = region.getViewArea();
        final RegionReference referenceArea = region.getRegionReference();
        final float startx = (float) (viewArea.getX() / 1000f);
        final float starty = (float) (viewArea.getY() / 1000f);
        final float width = (float) (viewArea.getWidth() / 1000f);
        final float height = (float) (viewArea.getHeight() / 1000f);

        // adjust the current position according to region borders and padding
        this.currentBPPosition = referenceArea.getBorderAndPaddingWidthBefore();
        final int level = region.getBidiLevel();
        if (level == -1 || (level & 1) == 0) {
            this.currentIPPosition = referenceArea
                    .getBorderAndPaddingWidthStart();
        } else {
            this.currentIPPosition = referenceArea
                    .getBorderAndPaddingWidthEnd();
        }
        // draw background (traits are in the RegionViewport)
        // and borders (traits are in the RegionReference)
        drawBackAndBorders(region, referenceArea, startx, starty, width, height);
    }

    /**
     * Draw the background and borders. This draws the background and border
     * traits for an area given the position.
     *
     * @param area
     *            the area to get the traits from
     * @param startx
     *            the start x position
     * @param starty
     *            the start y position
     * @param width
     *            the width of the area
     * @param height
     *            the height of the area
     */
    protected void drawBackAndBorders(final Area area, final float startx,
            final float starty, final float width, final float height) {
        drawBackAndBorders(area, area, startx, starty, width, height);
    }

    /**
     * Draw the background and borders. This draws the background and border
     * traits for an area given the position.
     *
     * @param backgroundArea
     *            the area to get the background traits from
     * @param borderArea
     *            the area to get the border traits from
     * @param startx
     *            the start x position
     * @param starty
     *            the start y position
     * @param width
     *            the width of the area
     * @param height
     *            the height of the area
     */
    protected void drawBackAndBorders(final Area backgroundArea,
            final Area borderArea, final float startx, final float starty,
            final float width, final float height) {
        // draw background then border

        final BorderProps bpsBefore = (BorderProps) borderArea
                .getTrait(Trait.BORDER_BEFORE);
        final BorderProps bpsAfter = (BorderProps) borderArea
                .getTrait(Trait.BORDER_AFTER);
        final BorderProps bpsStart = (BorderProps) borderArea
                .getTrait(Trait.BORDER_START);
        final BorderProps bpsEnd = (BorderProps) borderArea
                .getTrait(Trait.BORDER_END);

        drawBackground(startx, starty, width, height,
                (Trait.Background) backgroundArea.getTrait(Trait.BACKGROUND),
                bpsBefore, bpsAfter, bpsStart, bpsEnd,
                backgroundArea.getBidiLevel());
        drawBorders(startx, starty, width, height, bpsBefore, bpsAfter,
                bpsStart, bpsEnd, borderArea.getBidiLevel());
    }

    /**
     * Draw the background. This draws the background given the position and the
     * traits.
     *
     * @param startx
     *            the start x position
     * @param starty
     *            the start y position
     * @param width
     *            the width of the area
     * @param height
     *            the height of the area
     * @param back
     *            the background traits
     * @param bpsBefore
     *            the border-before traits
     * @param bpsAfter
     *            the border-after traits
     * @param bpsStart
     *            the border-start traits
     * @param bpsEnd
     *            the border-end traits
     * @param level
     *            of bidirectional embedding
     */
    protected void drawBackground(
            // CSOK: ParameterNumber
            final float startx, final float starty, final float width,
            final float height, final Trait.Background back,
            final BorderProps bpsBefore, final BorderProps bpsAfter,
            final BorderProps bpsStart, final BorderProps bpsEnd,
            final int level) {
        final BorderProps bpsTop = bpsBefore;
        final BorderProps bpsBottom = bpsAfter;
        BorderProps bpsLeft;
        BorderProps bpsRight;
        if (level == -1 || (level & 1) == 0) {
            bpsLeft = bpsStart;
            bpsRight = bpsEnd;
        } else {
            bpsLeft = bpsEnd;
            bpsRight = bpsStart;
        }
        drawBackground(startx, starty, width, height, back, bpsTop, bpsBottom,
                bpsLeft, bpsRight);
    }

    /**
     * Draw the background. This draws the background given the position and the
     * traits.
     *
     * @param startx
     *            the start x position
     * @param starty
     *            the start y position
     * @param width
     *            the width of the area
     * @param height
     *            the height of the area
     * @param back
     *            the background traits
     * @param bpsTop
     *            the border specification on the top edge
     * @param bpsBottom
     *            the border traits associated with bottom edge
     * @param bpsLeft
     *            the border specification on the left edge
     * @param bpsRight
     *            the border specification on the right edge
     */
    protected void drawBackground(
            // CSOK: ParameterNumber
            final float startx, final float starty, final float width,
            final float height, final Trait.Background back,
            final BorderProps bpsTop, final BorderProps bpsBottom,
            final BorderProps bpsLeft, final BorderProps bpsRight) {
        if (back != null) {
            endTextObject();

            // Calculate padding rectangle
            float sx = startx;
            float sy = starty;
            float paddRectWidth = width;
            float paddRectHeight = height;
            if (bpsLeft != null) {
                sx += bpsLeft.width / 1000f;
                paddRectWidth -= bpsLeft.width / 1000f;
            }
            if (bpsTop != null) {
                sy += bpsTop.width / 1000f;
                paddRectHeight -= bpsTop.width / 1000f;
            }
            if (bpsRight != null) {
                paddRectWidth -= bpsRight.width / 1000f;
            }
            if (bpsBottom != null) {
                paddRectHeight -= bpsBottom.width / 1000f;
            }

            if (back.getColor() != null) {
                updateColor(back.getColor(), true);
                fillRect(sx, sy, paddRectWidth, paddRectHeight);
            }
            if (back.getImageInfo() != null) {
                final ImageSize imageSize = back.getImageInfo().getSize();
                saveGraphicsState();
                clipRect(sx, sy, paddRectWidth, paddRectHeight);
                int horzCount = (int) (paddRectWidth * 1000
                        / imageSize.getWidthMpt() + 1.0f);
                int vertCount = (int) (paddRectHeight * 1000
                        / imageSize.getHeightMpt() + 1.0f);
                if (back.getRepeat() == EN_NOREPEAT) {
                    horzCount = 1;
                    vertCount = 1;
                } else if (back.getRepeat() == EN_REPEATX) {
                    vertCount = 1;
                } else if (back.getRepeat() == EN_REPEATY) {
                    horzCount = 1;
                }
                // change from points to millipoints
                sx *= 1000;
                sy *= 1000;
                if (horzCount == 1) {
                    sx += back.getHoriz();
                }
                if (vertCount == 1) {
                    sy += back.getVertical();
                }
                for (int x = 0; x < horzCount; x++) {
                    for (int y = 0; y < vertCount; y++) {
                        // place once
                        Rectangle2D pos;
                        // Image positions are relative to the currentIP/BP
                        pos = new Rectangle2D.Float(sx - this.currentIPPosition
                                + x * imageSize.getWidthMpt(), sy
                                - this.currentBPPosition + y
                                * imageSize.getHeightMpt(),
                                imageSize.getWidthMpt(),
                                imageSize.getHeightMpt());
                        drawImage(back.getURL(), pos);
                    }
                }

                restoreGraphicsState();
            }
        }
    }

    /**
     * Draw the borders. This draws the border traits given the position and the
     * traits.
     *
     * @param startx
     *            the start x position
     * @param starty
     *            the start y position
     * @param width
     *            the width of the area
     * @param height
     *            the height of the area
     * @param bpsBefore
     *            the border traits associated with before edge
     * @param bpsAfter
     *            the border traits associated with after edge
     * @param bpsStart
     *            the border traits associated with start edge
     * @param bpsEnd
     *            the border traits associated with end edge
     * @param level
     *            of bidirectional embedding
     */
    protected void drawBorders(
            // CSOK: ParameterNumber
            final float startx, final float starty, final float width,
            final float height, final BorderProps bpsBefore,
            final BorderProps bpsAfter, final BorderProps bpsStart,
            final BorderProps bpsEnd, final int level) {
        final Rectangle2D.Float borderRect = new Rectangle2D.Float(startx,
                starty, width, height);
        final BorderProps bpsTop = bpsBefore;
        final BorderProps bpsBottom = bpsAfter;
        BorderProps bpsLeft;
        BorderProps bpsRight;
        if (level == -1 || (level & 1) == 0) {
            bpsLeft = bpsStart;
            bpsRight = bpsEnd;
        } else {
            bpsLeft = bpsEnd;
            bpsRight = bpsStart;
        }
        drawBorders(borderRect, bpsTop, bpsBottom, bpsLeft, bpsRight);
    }

    private static final int TOP = 0;
    private static final int RIGHT = 1;
    private static final int BOTTOM = 2;
    private static final int LEFT = 3;

    /**
     * Draws borders.
     * 
     * @param borderRect
     *            the border rectangle
     * @param bpsTop
     *            the border specification on the top edge
     * @param bpsBottom
     *            the border traits associated with bottom edge
     * @param bpsLeft
     *            the border specification on the left edge
     * @param bpsRight
     *            the border specification on the right edge
     */
    protected void drawBorders(
            // CSOK: MethodLength
            final Rectangle2D.Float borderRect, final BorderProps bpsTop,
            final BorderProps bpsBottom, final BorderProps bpsLeft,
            final BorderProps bpsRight) {
        // TODO generalize each of the four conditions into using a
        // parameterized drawBorder()
        final boolean[] border = new boolean[] { bpsTop != null,
                bpsRight != null, bpsBottom != null, bpsLeft != null };
        float startx = borderRect.x;
        float starty = borderRect.y;
        float width = borderRect.width;
        float height = borderRect.height;
        final float[] borderWidth = new float[] {
                border[TOP] ? bpsTop.width / 1000f : 0.0f,
                border[RIGHT] ? bpsRight.width / 1000f : 0.0f,
                border[BOTTOM] ? bpsBottom.width / 1000f : 0.0f,
                border[LEFT] ? bpsLeft.width / 1000f : 0.0f };
        final float[] clipw = new float[] {
                BorderProps.getClippedWidth(bpsTop) / 1000f,
                BorderProps.getClippedWidth(bpsRight) / 1000f,
                BorderProps.getClippedWidth(bpsBottom) / 1000f,
                BorderProps.getClippedWidth(bpsLeft) / 1000f };

        starty += clipw[TOP];
        height -= clipw[TOP];
        height -= clipw[BOTTOM];
        startx += clipw[LEFT];
        width -= clipw[LEFT];
        width -= clipw[RIGHT];

        final boolean[] slant = new boolean[] { border[LEFT] && border[TOP],
                border[TOP] && border[RIGHT], border[RIGHT] && border[BOTTOM],
                border[BOTTOM] && border[LEFT] };
        if (bpsTop != null) {
            endTextObject();

            final float sx1 = startx;
            final float sx2 = slant[TOP] ? sx1 + borderWidth[LEFT]
                    - clipw[LEFT] : sx1;
            final float ex1 = startx + width;
            final float ex2 = slant[RIGHT] ? ex1 - borderWidth[RIGHT]
                    + clipw[RIGHT] : ex1;
            final float outery = starty - clipw[TOP];
            final float clipy = outery + clipw[TOP];
            final float innery = outery + borderWidth[TOP];

            saveGraphicsState();
            moveTo(sx1, clipy);
            float sx1a = sx1;
            float ex1a = ex1;
            if (bpsTop.mode == BorderProps.COLLAPSE_OUTER) {
                if (bpsLeft != null
                        && bpsLeft.mode == BorderProps.COLLAPSE_OUTER) {
                    sx1a -= clipw[LEFT];
                }
                if (bpsRight != null
                        && bpsRight.mode == BorderProps.COLLAPSE_OUTER) {
                    ex1a += clipw[RIGHT];
                }
                lineTo(sx1a, outery);
                lineTo(ex1a, outery);
            }
            lineTo(ex1, clipy);
            lineTo(ex2, innery);
            lineTo(sx2, innery);
            closePath();
            clip();
            drawBorderLine(sx1a, outery, ex1a, innery, true, true,
                    bpsTop.style, bpsTop.color);
            restoreGraphicsState();
        }
        if (bpsRight != null) {
            endTextObject();

            final float sy1 = starty;
            final float sy2 = slant[RIGHT] ? sy1 + borderWidth[TOP]
                    - clipw[TOP] : sy1;
            final float ey1 = starty + height;
            final float ey2 = slant[BOTTOM] ? ey1 - borderWidth[BOTTOM]
                    + clipw[BOTTOM] : ey1;
            final float outerx = startx + width + clipw[RIGHT];
            final float clipx = outerx - clipw[RIGHT];
            final float innerx = outerx - borderWidth[RIGHT];

            saveGraphicsState();
            moveTo(clipx, sy1);
            float sy1a = sy1;
            float ey1a = ey1;
            if (bpsRight.mode == BorderProps.COLLAPSE_OUTER) {
                if (bpsTop != null && bpsTop.mode == BorderProps.COLLAPSE_OUTER) {
                    sy1a -= clipw[TOP];
                }
                if (bpsBottom != null
                        && bpsBottom.mode == BorderProps.COLLAPSE_OUTER) {
                    ey1a += clipw[BOTTOM];
                }
                lineTo(outerx, sy1a);
                lineTo(outerx, ey1a);
            }
            lineTo(clipx, ey1);
            lineTo(innerx, ey2);
            lineTo(innerx, sy2);
            closePath();
            clip();
            drawBorderLine(innerx, sy1a, outerx, ey1a, false, false,
                    bpsRight.style, bpsRight.color);
            restoreGraphicsState();
        }
        if (bpsBottom != null) {
            endTextObject();

            final float sx1 = startx;
            final float sx2 = slant[LEFT] ? sx1 + borderWidth[LEFT]
                    - clipw[LEFT] : sx1;
            final float ex1 = startx + width;
            final float ex2 = slant[BOTTOM] ? ex1 - borderWidth[RIGHT]
                    + clipw[RIGHT] : ex1;
            final float outery = starty + height + clipw[BOTTOM];
            final float clipy = outery - clipw[BOTTOM];
            final float innery = outery - borderWidth[BOTTOM];

            saveGraphicsState();
            moveTo(ex1, clipy);
            float sx1a = sx1;
            float ex1a = ex1;
            if (bpsBottom.mode == BorderProps.COLLAPSE_OUTER) {
                if (bpsLeft != null
                        && bpsLeft.mode == BorderProps.COLLAPSE_OUTER) {
                    sx1a -= clipw[LEFT];
                }
                if (bpsRight != null
                        && bpsRight.mode == BorderProps.COLLAPSE_OUTER) {
                    ex1a += clipw[RIGHT];
                }
                lineTo(ex1a, outery);
                lineTo(sx1a, outery);
            }
            lineTo(sx1, clipy);
            lineTo(sx2, innery);
            lineTo(ex2, innery);
            closePath();
            clip();
            drawBorderLine(sx1a, innery, ex1a, outery, true, false,
                    bpsBottom.style, bpsBottom.color);
            restoreGraphicsState();
        }
        if (bpsLeft != null) {
            endTextObject();

            final float sy1 = starty;
            final float sy2 = slant[TOP] ? sy1 + borderWidth[TOP] - clipw[TOP]
                    : sy1;
            final float ey1 = sy1 + height;
            final float ey2 = slant[LEFT] ? ey1 - borderWidth[BOTTOM]
                    + clipw[BOTTOM] : ey1;
            final float outerx = startx - clipw[LEFT];
            final float clipx = outerx + clipw[LEFT];
            final float innerx = outerx + borderWidth[LEFT];

            saveGraphicsState();
            moveTo(clipx, ey1);
            float sy1a = sy1;
            float ey1a = ey1;
            if (bpsLeft.mode == BorderProps.COLLAPSE_OUTER) {
                if (bpsTop != null && bpsTop.mode == BorderProps.COLLAPSE_OUTER) {
                    sy1a -= clipw[TOP];
                }
                if (bpsBottom != null
                        && bpsBottom.mode == BorderProps.COLLAPSE_OUTER) {
                    ey1a += clipw[BOTTOM];
                }
                lineTo(outerx, ey1a);
                lineTo(outerx, sy1a);
            }
            lineTo(clipx, sy1);
            lineTo(innerx, sy2);
            lineTo(innerx, ey2);
            closePath();
            clip();
            drawBorderLine(outerx, sy1a, innerx, ey1a, false, true,
                    bpsLeft.style, bpsLeft.color);
            restoreGraphicsState();
        }
    }

    /**
     * Common method to render the background and borders for any inline area.
     * The all borders and padding are drawn outside the specified area.
     * 
     * @param area
     *            the inline area for which the background, border and padding
     *            is to be rendered
     */
    @Override
    protected void renderInlineAreaBackAndBorders(final InlineArea area) {
        final float borderPaddingStart = area.getBorderAndPaddingWidthStart() / 1000f;
        final float borderPaddingEnd = area.getBorderAndPaddingWidthEnd() / 1000f;
        final float borderPaddingBefore = area.getBorderAndPaddingWidthBefore() / 1000f;
        final float borderPaddingAfter = area.getBorderAndPaddingWidthAfter() / 1000f;
        final float bpwidth = borderPaddingStart + borderPaddingEnd;
        final float bpheight = borderPaddingBefore + borderPaddingAfter;

        final float height = area.getBPD() / 1000f;
        if (height != 0.0f || bpheight != 0.0f && bpwidth != 0.0f) {
            final float x = this.currentIPPosition / 1000f;
            final float y = (this.currentBPPosition + area
                    .getBlockProgressionOffset()) / 1000f;
            final float width = area.getIPD() / 1000f;
            drawBackAndBorders(area, x, y - borderPaddingBefore, width
                    + bpwidth, height + bpheight);
        }
    }

    /** Constant for the fox:transform extension attribute */
    protected static final QName FOX_TRANSFORM = new QName(
            ExtensionElementMapping.URI, "fox:transform");

    /** {@inheritDoc} */
    @Override
    protected void renderBlockViewport(final BlockViewport bv,
            final List children) {
        // clip and position viewport if necessary

        // save positions
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;

        CTM ctm = bv.getCTM();
        final int borderPaddingBefore = bv.getBorderAndPaddingWidthBefore();

        final int positioning = bv.getPositioning();
        if (positioning == Block.ABSOLUTE || positioning == Block.FIXED) {

            // For FIXED, we need to break out of the current viewports to the
            // one established by the page. We save the state stack for
            // restoration
            // after the block-container has been painted. See below.
            List breakOutList = null;
            if (positioning == Block.FIXED) {
                breakOutList = breakOutOfStateStack();
            }

            final AffineTransform positionTransform = new AffineTransform();
            positionTransform.translate(bv.getXOffset(), bv.getYOffset());

            final int level = bv.getBidiLevel();
            final int borderPaddingStart = bv.getBorderAndPaddingWidthStart();
            final int borderPaddingEnd = bv.getBorderAndPaddingWidthEnd();

            // "left/"top" (bv.getX/YOffset()) specify the position of the
            // content rectangle
            if (level == -1 || (level & 1) == 0) {
                positionTransform.translate(-borderPaddingStart,
                        -borderPaddingBefore);
            } else {
                positionTransform.translate(-borderPaddingEnd,
                        -borderPaddingBefore);
            }

            // Free transformation for the block-container viewport
            String transf;
            transf = bv.getForeignAttributeValue(FOX_TRANSFORM);
            if (transf != null) {
                final AffineTransform freeTransform = AWTTransformProducer
                        .createAffineTransform(transf);
                positionTransform.concatenate(freeTransform);
            }

            // Viewport position
            if (!positionTransform.isIdentity()) {
                establishTransformationMatrix(positionTransform);
            }

            // This is the content-rect
            final float width = bv.getIPD() / 1000f;
            final float height = bv.getBPD() / 1000f;

            // Background and borders
            final float borderPaddingWidth = (borderPaddingStart + borderPaddingEnd) / 1000f;
            final float borderPaddingHeight = (borderPaddingBefore + bv
                    .getBorderAndPaddingWidthAfter()) / 1000f;
            drawBackAndBorders(bv, 0, 0, width + borderPaddingWidth, height
                    + borderPaddingHeight);

            // Shift to content rectangle after border painting
            final AffineTransform contentRectTransform = new AffineTransform();
            if (level == -1 || (level & 1) == 0) {
                contentRectTransform.translate(borderPaddingStart,
                        borderPaddingBefore);
            } else {
                contentRectTransform.translate(borderPaddingEnd,
                        borderPaddingBefore);
            }

            if (!contentRectTransform.isIdentity()) {
                establishTransformationMatrix(contentRectTransform);
            }

            // Clipping
            if (bv.hasClip()) {
                clipRect(0f, 0f, width, height);
            }

            // Set up coordinate system for content rectangle
            final AffineTransform contentTransform = ctm.toAffineTransform();
            if (!contentTransform.isIdentity()) {
                establishTransformationMatrix(contentTransform);
            }

            this.currentIPPosition = 0;
            this.currentBPPosition = 0;
            renderBlocks(bv, children);

            if (!contentTransform.isIdentity()) {
                restoreGraphicsState();
            }

            if (!contentRectTransform.isIdentity()) {
                restoreGraphicsState();
            }

            if (!positionTransform.isIdentity()) {
                restoreGraphicsState();
            }

            // For FIXED, we need to restore break out now we are done
            if (positioning == Block.FIXED) {
                if (breakOutList != null) {
                    restoreStateStackAfterBreakOut(breakOutList);
                }
            }

            this.currentIPPosition = saveIP;
            this.currentBPPosition = saveBP;
        } else {

            this.currentBPPosition += bv.getSpaceBefore();

            // borders and background in the old coordinate system
            handleBlockTraits(bv);

            // Advance to start of content area
            this.currentIPPosition += bv.getStartIndent();

            final CTM tempctm = new CTM(this.containingIPPosition,
                    this.currentBPPosition);
            ctm = tempctm.multiply(ctm);

            // Now adjust for border/padding
            this.currentBPPosition += borderPaddingBefore;

            Rectangle clippingRect = null;
            if (bv.hasClip()) {
                clippingRect = new Rectangle(this.currentIPPosition,
                        this.currentBPPosition, bv.getIPD(), bv.getBPD());
            }

            startVParea(ctm, clippingRect);
            this.currentIPPosition = 0;
            this.currentBPPosition = 0;
            renderBlocks(bv, children);
            endVParea();

            this.currentIPPosition = saveIP;
            this.currentBPPosition = saveBP;

            this.currentBPPosition += bv.getAllocBPD();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void renderReferenceArea(final Block block) {
        // save position and offset
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;

        // Establish a new coordinate system
        final AffineTransform at = new AffineTransform();
        at.translate(this.currentIPPosition, this.currentBPPosition);
        at.translate(block.getXOffset(), block.getYOffset());
        at.translate(0, block.getSpaceBefore());

        if (!at.isIdentity()) {
            establishTransformationMatrix(at);
        }

        this.currentIPPosition = 0;
        this.currentBPPosition = 0;
        handleBlockTraits(block);

        final List children = block.getChildAreas();
        if (children != null) {
            renderBlocks(block, children);
        }

        if (!at.isIdentity()) {
            restoreGraphicsState();
        }

        // stacked and relative blocks effect stacking
        this.currentIPPosition = saveIP;
        this.currentBPPosition = saveBP;
    }

    /** {@inheritDoc} */
    @Override
    protected void renderFlow(final NormalFlow flow) {
        // save position and offset
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;

        // Establish a new coordinate system
        final AffineTransform at = new AffineTransform();
        at.translate(this.currentIPPosition, this.currentBPPosition);

        if (!at.isIdentity()) {
            establishTransformationMatrix(at);
        }

        this.currentIPPosition = 0;
        this.currentBPPosition = 0;
        super.renderFlow(flow);

        if (!at.isIdentity()) {
            restoreGraphicsState();
        }

        // stacked and relative blocks effect stacking
        this.currentIPPosition = saveIP;
        this.currentBPPosition = saveBP;
    }

    /**
     * Concatenates the current transformation matrix with the given one,
     * therefore establishing a new coordinate system.
     * 
     * @param at
     *            the transformation matrix to process (coordinates in points)
     */
    protected abstract void concatenateTransformationMatrix(
            final AffineTransform at);

    /**
     * Render an inline viewport. This renders an inline viewport by clipping if
     * necessary.
     * 
     * @param viewport
     *            the viewport to handle
     */
    @Override
    public void renderInlineViewport(final InlineViewport viewport) {
        final int level = viewport.getBidiLevel();
        final float x = this.currentIPPosition / 1000f;
        final float y = (this.currentBPPosition + viewport
                .getBlockProgressionOffset()) / 1000f;
        final float width = viewport.getIPD() / 1000f;
        final float height = viewport.getBPD() / 1000f;
        // TODO: Calculate the border rect correctly.
        final float borderPaddingStart = viewport
                .getBorderAndPaddingWidthStart() / 1000f;
        final float borderPaddingEnd = viewport.getBorderAndPaddingWidthEnd() / 1000f;
        final float borderPaddingBefore = viewport
                .getBorderAndPaddingWidthBefore() / 1000f;
        final float borderPaddingAfter = viewport
                .getBorderAndPaddingWidthAfter() / 1000f;
        final float bpwidth = borderPaddingStart + borderPaddingEnd;
        final float bpheight = borderPaddingBefore + borderPaddingAfter;

        drawBackAndBorders(viewport, x, y, width + bpwidth, height + bpheight);

        if (viewport.hasClip()) {
            saveGraphicsState();
            if (level == -1 || (level & 1) == 0) {
                clipRect(x + borderPaddingStart, y + borderPaddingBefore,
                        width, height);
            } else {
                clipRect(x + borderPaddingEnd, y + borderPaddingBefore, width,
                        height);
            }
        }
        super.renderInlineViewport(viewport);

        if (viewport.hasClip()) {
            restoreGraphicsState();
        }
    }

    /**
     * Restores the state stack after a break out.
     * 
     * @param breakOutList
     *            the state stack to restore.
     */
    protected abstract void restoreStateStackAfterBreakOut(
            final List breakOutList);

    /**
     * Breaks out of the state stack to handle fixed block-containers.
     * 
     * @return the saved state stack to recreate later
     */
    protected abstract List breakOutOfStateStack();

    /** Saves the graphics state of the rendering engine. */
    protected abstract void saveGraphicsState();

    /** Restores the last graphics state of the rendering engine. */
    protected abstract void restoreGraphicsState();

    /** Indicates the beginning of a text object. */
    protected abstract void beginTextObject();

    /** Indicates the end of a text object. */
    protected abstract void endTextObject();

    /**
     * Paints the text decoration marks.
     * 
     * @param fm
     *            Current typeface
     * @param fontsize
     *            Current font size
     * @param inline
     *            inline area to paint the marks for
     * @param baseline
     *            position of the baseline
     * @param startx
     *            start IPD
     */
    protected void renderTextDecoration(final FontMetrics fm,
            final int fontsize, final InlineArea inline, final int baseline,
            final int startx) {
        final boolean hasTextDeco = inline.hasUnderline()
                || inline.hasOverline() || inline.hasLineThrough();
        if (hasTextDeco) {
            endTextObject();
            final float descender = fm.getDescender(fontsize) / 1000f;
            final float capHeight = fm.getCapHeight(fontsize) / 1000f;
            final float halfLineWidth = descender / -8f / 2f;
            final float endx = (startx + inline.getIPD()) / 1000f;
            if (inline.hasUnderline()) {
                final Color ct = (Color) inline.getTrait(Trait.UNDERLINE_COLOR);
                final float y = baseline - descender / 2f;
                drawBorderLine(startx / 1000f, (y - halfLineWidth) / 1000f,
                        endx, (y + halfLineWidth) / 1000f, true, true,
                        Constants.EN_SOLID, ct);
            }
            if (inline.hasOverline()) {
                final Color ct = (Color) inline.getTrait(Trait.OVERLINE_COLOR);
                final float y = (float) (baseline - 1.1 * capHeight);
                drawBorderLine(startx / 1000f, (y - halfLineWidth) / 1000f,
                        endx, (y + halfLineWidth) / 1000f, true, true,
                        Constants.EN_SOLID, ct);
            }
            if (inline.hasLineThrough()) {
                final Color ct = (Color) inline
                        .getTrait(Trait.LINETHROUGH_COLOR);
                final float y = (float) (baseline - 0.45 * capHeight);
                drawBorderLine(startx / 1000f, (y - halfLineWidth) / 1000f,
                        endx, (y + halfLineWidth) / 1000f, true, true,
                        Constants.EN_SOLID, ct);
            }
        }
    }

    /** Clip using the current path. */
    protected abstract void clip();

    /**
     * Clip using a rectangular area.
     * 
     * @param x
     *            the x coordinate (in points)
     * @param y
     *            the y coordinate (in points)
     * @param width
     *            the width of the rectangle (in points)
     * @param height
     *            the height of the rectangle (in points)
     */
    protected abstract void clipRect(final float x, final float y,
            final float width, final float height);

    /**
     * Moves the current point to (x, y), omitting any connecting line segment.
     * 
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     */
    protected abstract void moveTo(final float x, final float y);

    /**
     * Appends a straight line segment from the current point to (x, y). The new
     * current point is (x, y).
     * 
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     */
    protected abstract void lineTo(final float x, final float y);

    /**
     * Closes the current subpath by appending a straight line segment from the
     * current point to the starting point of the subpath.
     */
    protected abstract void closePath();

    /**
     * Fill a rectangular area.
     * 
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param width
     *            the width of the rectangle
     * @param height
     *            the height of the rectangle
     */
    protected abstract void fillRect(final float x, final float y,
            final float width, final float height);

    /**
     * Establishes a new foreground or fill color.
     * 
     * @param col
     *            the color to apply (null skips this operation)
     * @param fill
     *            true to set the fill color, false for the foreground color
     */
    protected abstract void updateColor(final Color col, final boolean fill);

    /**
     * Draw an image at the indicated location.
     * 
     * @param url
     *            the URI/URL of the image
     * @param pos
     *            the position of the image
     * @param foreignAttributes
     *            an optional Map with foreign attributes, may be null
     */
    protected abstract void drawImage(final String url, final Rectangle2D pos,
            final Map foreignAttributes);

    /**
     * Draw an image at the indicated location.
     * 
     * @param url
     *            the URI/URL of the image
     * @param pos
     *            the position of the image
     */
    protected final void drawImage(final String url, final Rectangle2D pos) {
        drawImage(url, pos, null);
    }

    /**
     * Draw a border segment of an XSL-FO style border.
     * 
     * @param x1
     *            starting x coordinate
     * @param y1
     *            starting y coordinate
     * @param x2
     *            ending x coordinate
     * @param y2
     *            ending y coordinate
     * @param horz
     *            true for horizontal border segments, false for vertical border
     *            segments
     * @param startOrBefore
     *            true for border segments on the start or before edge, false
     *            for end or after.
     * @param style
     *            the border style (one of Constants.EN_DASHED etc.)
     * @param col
     *            the color for the border segment
     */
    protected abstract void drawBorderLine(
            // CSOK: ParameterNumber
            final float x1, final float y1, final float x2, final float y2,
            final boolean horz, final boolean startOrBefore, final int style,
            final Color col);

    /** {@inheritDoc} */
    @Override
    public void renderForeignObject(final ForeignObject fo,
            final Rectangle2D pos) {
        endTextObject();
        final Document doc = fo.getDocument();
        final String ns = fo.getNameSpace();
        renderDocument(doc, ns, pos, fo.getForeignAttributes());
    }

    /**
     * Establishes a new coordinate system with the given transformation matrix.
     * The current graphics state is saved and the new coordinate system is
     * concatenated.
     * 
     * @param at
     *            the transformation matrix
     */
    protected void establishTransformationMatrix(final AffineTransform at) {
        saveGraphicsState();
        concatenateTransformationMatrix(UnitConv.mptToPt(at));
    }

}
