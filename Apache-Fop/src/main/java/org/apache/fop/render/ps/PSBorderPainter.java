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

/* $Id: PSBorderPainter.java 1095878 2011-04-22 07:15:43Z jeremias $ */

package org.apache.fop.render.ps;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fo.Constants;
import org.apache.fop.render.intermediate.BorderPainter;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.ColorUtil;
import org.apache.xmlgraphics.ps.PSGenerator;

/**
 * PostScript-specific implementation of the {@link BorderPainter}.
 */
@Slf4j
public class PSBorderPainter extends BorderPainter {

    private final PSGenerator generator;

    /**
     * Creates a new border painter for PostScript.
     *
     * @param generator
     *            the PostScript generator
     */
    public PSBorderPainter(final PSGenerator generator) {
        this.generator = generator;
    }

    /** {@inheritDoc} */
    @Override
    protected void drawBorderLine(
            // CSOK: ParameterNumber
            final int x1, final int y1, final int x2, final int y2,
            final boolean horz, final boolean startOrBefore, final int style,
            final Color col) throws IOException {
        drawBorderLine(this.generator, toPoints(x1), toPoints(y1),
                toPoints(x2), toPoints(y2), horz, startOrBefore, style, col);
    }

    private static void drawLine(final PSGenerator gen, final float startx,
            final float starty, final float endx, final float endy)
                    throws IOException {
        gen.writeln(gen.formatDouble(startx) + " " + gen.formatDouble(starty)
                + " " + gen.mapCommand("moveto") + " " + gen.formatDouble(endx)
                + " " + gen.formatDouble(endy) + " " + gen.mapCommand("lineto")
                + " " + gen.mapCommand("stroke") + " "
                + gen.mapCommand("newpath"));
    }

    /**
     * @param gen
     *            ps content generator
     * @see BorderPainter#drawBorderLine
     */
    public static void drawBorderLine(
            // CSOK: ParameterNumber
            final PSGenerator gen, final float x1, final float y1,
            final float x2, final float y2, final boolean horz, // CSOK:
            // JavadocMethod
            final boolean startOrBefore, final int style, final Color col) // CSOK:
            // JavadocMethod
            throws IOException { // CSOK: JavadocMethod
        final float w = x2 - x1;
        final float h = y2 - y1;
        if (w < 0 || h < 0) {
            log.error("Negative extent received. Border won't be painted.");
            return;
        }
        switch (style) {
        case Constants.EN_DASHED:
            gen.useColor(col);
            if (horz) {
                float unit = Math.abs(2 * h);
                int rep = (int) (w / unit);
                if (rep % 2 == 0) {
                    rep++;
                }
                unit = w / rep;
                gen.useDash("[" + unit + "] 0");
                gen.useLineCap(0);
                gen.useLineWidth(h);
                final float ym = y1 + h / 2;
                drawLine(gen, x1, ym, x2, ym);
            } else {
                float unit = Math.abs(2 * w);
                int rep = (int) (h / unit);
                if (rep % 2 == 0) {
                    rep++;
                }
                unit = h / rep;
                gen.useDash("[" + unit + "] 0");
                gen.useLineCap(0);
                gen.useLineWidth(w);
                final float xm = x1 + w / 2;
                drawLine(gen, xm, y1, xm, y2);
            }
            break;
        case Constants.EN_DOTTED:
            gen.useColor(col);
            gen.useLineCap(1); // Rounded!
            if (horz) {
                float unit = Math.abs(2 * h);
                int rep = (int) (w / unit);
                if (rep % 2 == 0) {
                    rep++;
                }
                unit = w / rep;
                gen.useDash("[0 " + unit + "] 0");
                gen.useLineWidth(h);
                final float ym = y1 + h / 2;
                drawLine(gen, x1, ym, x2, ym);
            } else {
                float unit = Math.abs(2 * w);
                int rep = (int) (h / unit);
                if (rep % 2 == 0) {
                    rep++;
                }
                unit = h / rep;
                gen.useDash("[0 " + unit + "] 0");
                gen.useLineWidth(w);
                final float xm = x1 + w / 2;
                drawLine(gen, xm, y1, xm, y2);
            }
            break;
        case Constants.EN_DOUBLE:
            gen.useColor(col);
            gen.useDash(null);
            if (horz) {
                final float h3 = h / 3;
                gen.useLineWidth(h3);
                final float ym1 = y1 + h3 / 2;
                final float ym2 = ym1 + h3 + h3;
                drawLine(gen, x1, ym1, x2, ym1);
                drawLine(gen, x1, ym2, x2, ym2);
            } else {
                final float w3 = w / 3;
                gen.useLineWidth(w3);
                final float xm1 = x1 + w3 / 2;
                final float xm2 = xm1 + w3 + w3;
                drawLine(gen, xm1, y1, xm1, y2);
                drawLine(gen, xm2, y1, xm2, y2);
            }
            break;
        case Constants.EN_GROOVE:
        case Constants.EN_RIDGE:
            float colFactor = style == Constants.EN_GROOVE ? 0.4f : -0.4f;
            gen.useDash(null);
            if (horz) {
                final Color uppercol = ColorUtil.lightenColor(col, -colFactor);
                final Color lowercol = ColorUtil.lightenColor(col, colFactor);
                final float h3 = h / 3;
                gen.useLineWidth(h3);
                final float ym1 = y1 + h3 / 2;
                gen.useColor(uppercol);
                drawLine(gen, x1, ym1, x2, ym1);
                gen.useColor(col);
                drawLine(gen, x1, ym1 + h3, x2, ym1 + h3);
                gen.useColor(lowercol);
                drawLine(gen, x1, ym1 + h3 + h3, x2, ym1 + h3 + h3);
            } else {
                final Color leftcol = ColorUtil.lightenColor(col, -colFactor);
                final Color rightcol = ColorUtil.lightenColor(col, colFactor);
                final float w3 = w / 3;
                gen.useLineWidth(w3);
                final float xm1 = x1 + w3 / 2;
                gen.useColor(leftcol);
                drawLine(gen, xm1, y1, xm1, y2);
                gen.useColor(col);
                drawLine(gen, xm1 + w3, y1, xm1 + w3, y2);
                gen.useColor(rightcol);
                drawLine(gen, xm1 + w3 + w3, y1, xm1 + w3 + w3, y2);
            }
            break;
        case Constants.EN_INSET:
        case Constants.EN_OUTSET:
            colFactor = style == Constants.EN_OUTSET ? 0.4f : -0.4f;
            gen.useDash(null);
            if (horz) {
                final Color c = ColorUtil.lightenColor(col, (startOrBefore ? 1
                        : -1) * colFactor);
                gen.useLineWidth(h);
                final float ym1 = y1 + h / 2;
                gen.useColor(c);
                drawLine(gen, x1, ym1, x2, ym1);
            } else {
                final Color c = ColorUtil.lightenColor(col, (startOrBefore ? 1
                        : -1) * colFactor);
                gen.useLineWidth(w);
                final float xm1 = x1 + w / 2;
                gen.useColor(c);
                drawLine(gen, xm1, y1, xm1, y2);
            }
            break;
        case Constants.EN_HIDDEN:
            break;
        default:
            gen.useColor(col);
            gen.useDash(null);
            gen.useLineCap(0);
            if (horz) {
                gen.useLineWidth(h);
                final float ym = y1 + h / 2;
                drawLine(gen, x1, ym, x2, ym);
            } else {
                gen.useLineWidth(w);
                final float xm = x1 + w / 2;
                drawLine(gen, xm, y1, xm, y2);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawLine(final Point start, final Point end, final int width,
            final Color color, final RuleStyle style) throws IOException {
        if (start.y != end.y) {
            // TODO Support arbitrary lines if necessary
            throw new UnsupportedOperationException(
                    "Can only deal with horizontal lines right now");
        }

        saveGraphicsState();
        final int half = width / 2;
        final int starty = start.y - half;
        // Rectangle boundingRect = new Rectangle(start.x, start.y - half, end.x
        // - start.x, width);

        switch (style.getEnumValue()) {
        case Constants.EN_SOLID:
        case Constants.EN_DASHED:
        case Constants.EN_DOUBLE:
            drawBorderLine(start.x, starty, end.x, starty + width, true, true,
                    style.getEnumValue(), color);
            break;
        case Constants.EN_DOTTED:
            clipRect(start.x, starty, end.x - start.x, width);
            // This displaces the dots to the right by half a dot's width
            // TODO There's room for improvement here
            this.generator.concatMatrix(1, 0, 0, 1, toPoints(half), 0);
            drawBorderLine(start.x, starty, end.x, starty + width, true, true,
                    style.getEnumValue(), color);
            break;
        case Constants.EN_GROOVE:
        case Constants.EN_RIDGE:
            this.generator.useColor(ColorUtil.lightenColor(color, 0.6f));
            moveTo(start.x, starty);
            lineTo(end.x, starty);
            lineTo(end.x, starty + 2 * half);
            lineTo(start.x, starty + 2 * half);
            closePath();
            this.generator.write(" " + this.generator.mapCommand("fill"));
            this.generator.writeln(" " + this.generator.mapCommand("newpath"));
            this.generator.useColor(color);
            if (style == RuleStyle.GROOVE) {
                moveTo(start.x, starty);
                lineTo(end.x, starty);
                lineTo(end.x, starty + half);
                lineTo(start.x + half, starty + half);
                lineTo(start.x, starty + 2 * half);
            } else {
                moveTo(end.x, starty);
                lineTo(end.x, starty + 2 * half);
                lineTo(start.x, starty + 2 * half);
                lineTo(start.x, starty + half);
                lineTo(end.x - half, starty + half);
            }
            closePath();
            this.generator.write(" " + this.generator.mapCommand("fill"));
            this.generator.writeln(" " + this.generator.mapCommand("newpath"));
            break;
        default:
            throw new UnsupportedOperationException("rule style not supported");
        }

        restoreGraphicsState();

    }

    private static float toPoints(final int mpt) {
        return mpt / 1000f;
    }

    /** {@inheritDoc} */
    @Override
    protected void moveTo(final int x, final int y) throws IOException {
        this.generator.writeln(this.generator.formatDouble(toPoints(x)) + " "
                + this.generator.formatDouble(toPoints(y)) + " "
                + this.generator.mapCommand("moveto"));
    }

    /** {@inheritDoc} */
    @Override
    protected void lineTo(final int x, final int y) throws IOException {
        this.generator.writeln(this.generator.formatDouble(toPoints(x)) + " "
                + this.generator.formatDouble(toPoints(y)) + " "
                + this.generator.mapCommand("lineto"));
    }

    /** {@inheritDoc} */
    @Override
    protected void closePath() throws IOException {
        this.generator.writeln("cp");
    }

    private void clipRect(final int x, final int y, final int width,
            final int height) throws IOException {
        this.generator.defineRect(toPoints(x), toPoints(y), toPoints(width),
                toPoints(height));
        clip();
    }

    /** {@inheritDoc} */
    @Override
    protected void clip() throws IOException {
        this.generator.writeln(this.generator.mapCommand("clip") + " "
                + this.generator.mapCommand("newpath"));
    }

    /** {@inheritDoc} */
    @Override
    protected void saveGraphicsState() throws IOException {
        this.generator.saveGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    protected void restoreGraphicsState() throws IOException {
        this.generator.restoreGraphicsState();
    }

}
