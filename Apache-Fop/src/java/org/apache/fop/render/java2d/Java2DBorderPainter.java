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

/* $Id: Java2DBorderPainter.java 990144 2010-08-27 13:23:11Z vhennebert $ */

package org.apache.fop.render.java2d;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fo.Constants;
import org.apache.fop.render.intermediate.BorderPainter;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.ColorUtil;

/**
 * Java2D-specific implementation of the {@link BorderPainter}.
 */
@Slf4j
public class Java2DBorderPainter extends BorderPainter {

    private final Java2DPainter painter;

    private GeneralPath currentPath = null;

    /**
     * Construct a java2d border painter.
     *
     * @param painter
     *            a painter
     */
    public Java2DBorderPainter(final Java2DPainter painter) {
        this.painter = painter;
    }

    private Java2DGraphicsState getG2DState() {
        return this.painter.g2dState;
    }

    private Graphics2D getG2D() {
        return getG2DState().getGraph();
    }

    /** {@inheritDoc} */
    @Override
    protected void drawBorderLine(
            // CSOK: ParameterNumber
            final int x1, final int y1, final int x2, final int y2,
            final boolean horz, final boolean startOrBefore, final int style,
            Color color) {
        final float w = x2 - x1;
        final float h = y2 - y1;
        if (w < 0 || h < 0) {
            log.error("Negative extent received. Border won't be painted.");
            return;
        }
        switch (style) {
        case Constants.EN_DASHED:
            getG2D().setColor(color);
            if (horz) {
                float unit = Math.abs(2 * h);
                int rep = (int) (w / unit);
                if (rep % 2 == 0) {
                    rep++;
                }
                unit = w / rep;
                final float ym = y1 + h / 2;
                final BasicStroke s = new BasicStroke(h, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10.0f, new float[] { unit }, 0);
                getG2D().setStroke(s);
                getG2D().draw(new Line2D.Float(x1, ym, x2, ym));
            } else {
                float unit = Math.abs(2 * w);
                int rep = (int) (h / unit);
                if (rep % 2 == 0) {
                    rep++;
                }
                unit = h / rep;
                final float xm = x1 + w / 2;
                final BasicStroke s = new BasicStroke(w, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10.0f, new float[] { unit }, 0);
                getG2D().setStroke(s);
                getG2D().draw(new Line2D.Float(xm, y1, xm, y2));
            }
            break;
        case Constants.EN_DOTTED:
            getG2D().setColor(color);
            if (horz) {
                float unit = Math.abs(2 * h);
                int rep = (int) (w / unit);
                if (rep % 2 == 0) {
                    rep++;
                }
                unit = w / rep;
                final float ym = y1 + h / 2;
                final BasicStroke s = new BasicStroke(h, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_MITER, 10.0f, new float[] { 0, unit },
                        0);
                getG2D().setStroke(s);
                getG2D().draw(new Line2D.Float(x1, ym, x2, ym));
            } else {
                float unit = Math.abs(2 * w);
                int rep = (int) (h / unit);
                if (rep % 2 == 0) {
                    rep++;
                }
                unit = h / rep;
                final float xm = x1 + w / 2;
                final BasicStroke s = new BasicStroke(w, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_MITER, 10.0f, new float[] { 0, unit },
                        0);
                getG2D().setStroke(s);
                getG2D().draw(new Line2D.Float(xm, y1, xm, y2));
            }
            break;
        case Constants.EN_DOUBLE:
            getG2D().setColor(color);
            if (horz) {
                final float h3 = h / 3;
                final float ym1 = y1 + h3 / 2;
                final float ym2 = ym1 + h3 + h3;
                final BasicStroke s = new BasicStroke(h3);
                getG2D().setStroke(s);
                getG2D().draw(new Line2D.Float(x1, ym1, x2, ym1));
                getG2D().draw(new Line2D.Float(x1, ym2, x2, ym2));
            } else {
                final float w3 = w / 3;
                final float xm1 = x1 + w3 / 2;
                final float xm2 = xm1 + w3 + w3;
                final BasicStroke s = new BasicStroke(w3);
                getG2D().setStroke(s);
                getG2D().draw(new Line2D.Float(xm1, y1, xm1, y2));
                getG2D().draw(new Line2D.Float(xm2, y1, xm2, y2));
            }
            break;
        case Constants.EN_GROOVE:
        case Constants.EN_RIDGE:
            float colFactor = style == Constants.EN_GROOVE ? 0.4f : -0.4f;
            if (horz) {
                final Color uppercol = ColorUtil
                        .lightenColor(color, -colFactor);
                final Color lowercol = ColorUtil.lightenColor(color, colFactor);
                final float h3 = h / 3;
                final float ym1 = y1 + h3 / 2;
                getG2D().setStroke(new BasicStroke(h3));
                getG2D().setColor(uppercol);
                getG2D().draw(new Line2D.Float(x1, ym1, x2, ym1));
                getG2D().setColor(color);
                getG2D().draw(new Line2D.Float(x1, ym1 + h3, x2, ym1 + h3));
                getG2D().setColor(lowercol);
                getG2D().draw(
                        new Line2D.Float(x1, ym1 + h3 + h3, x2, ym1 + h3 + h3));
            } else {
                final Color leftcol = ColorUtil.lightenColor(color, -colFactor);
                final Color rightcol = ColorUtil.lightenColor(color, colFactor);
                final float w3 = w / 3;
                final float xm1 = x1 + w3 / 2;
                getG2D().setStroke(new BasicStroke(w3));
                getG2D().setColor(leftcol);
                getG2D().draw(new Line2D.Float(xm1, y1, xm1, y2));
                getG2D().setColor(color);
                getG2D().draw(new Line2D.Float(xm1 + w3, y1, xm1 + w3, y2));
                getG2D().setColor(rightcol);
                getG2D().draw(
                        new Line2D.Float(xm1 + w3 + w3, y1, xm1 + w3 + w3, y2));
            }
            break;
        case Constants.EN_INSET:
        case Constants.EN_OUTSET:
            colFactor = style == Constants.EN_OUTSET ? 0.4f : -0.4f;
            if (horz) {
                color = ColorUtil.lightenColor(color, (startOrBefore ? 1 : -1)
                        * colFactor);
                getG2D().setStroke(new BasicStroke(h));
                final float ym1 = y1 + h / 2;
                getG2D().setColor(color);
                getG2D().draw(new Line2D.Float(x1, ym1, x2, ym1));
            } else {
                color = ColorUtil.lightenColor(color, (startOrBefore ? 1 : -1)
                        * colFactor);
                final float xm1 = x1 + w / 2;
                getG2D().setStroke(new BasicStroke(w));
                getG2D().setColor(color);
                getG2D().draw(new Line2D.Float(xm1, y1, xm1, y2));
            }
            break;
        case Constants.EN_HIDDEN:
            break;
        default:
            getG2D().setColor(color);
            if (horz) {
                final float ym = y1 + h / 2;
                getG2D().setStroke(new BasicStroke(h));
                getG2D().draw(new Line2D.Float(x1, ym, x2, ym));
            } else {
                final float xm = x1 + w / 2;
                getG2D().setStroke(new BasicStroke(w));
                getG2D().draw(new Line2D.Float(xm, y1, xm, y2));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void drawLine(final Point start, final Point end, final int width,
            final Color color, final RuleStyle style) {
        if (start.y != end.y) {
            // TODO Support arbitrary lines if necessary
            throw new UnsupportedOperationException(
                    "Can only deal with horizontal lines right now");
        }

        saveGraphicsState();
        final int half = width / 2;
        final int starty = start.y - half;
        final Rectangle boundingRect = new Rectangle(start.x, start.y - half,
                end.x - start.x, width);
        getG2DState().updateClip(boundingRect);

        switch (style.getEnumValue()) {
        case Constants.EN_SOLID:
        case Constants.EN_DASHED:
        case Constants.EN_DOUBLE:
            drawBorderLine(start.x, start.y - half, end.x, end.y + half, true,
                    true, style.getEnumValue(), color);
            break;
        case Constants.EN_DOTTED:
            final int shift = half; // This shifts the dots to the right by half
            // a dot's width
            drawBorderLine(start.x + shift, start.y - half, end.x + shift,
                    end.y + half, true, true, style.getEnumValue(), color);
            break;
        case Constants.EN_GROOVE:
        case Constants.EN_RIDGE:
            getG2DState().updateColor(ColorUtil.lightenColor(color, 0.6f));
            moveTo(start.x, starty);
            lineTo(end.x, starty);
            lineTo(end.x, starty + 2 * half);
            lineTo(start.x, starty + 2 * half);
            closePath();
            getG2D().fill(this.currentPath);
            this.currentPath = null;
            getG2DState().updateColor(color);
            if (style.getEnumValue() == Constants.EN_GROOVE) {
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
            getG2D().fill(this.currentPath);
            this.currentPath = null;

        case Constants.EN_NONE:
            // No rule is drawn
            break;
        default:
        } // end switch
        restoreGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    protected void clip() {
        if (this.currentPath == null) {
            throw new IllegalStateException("No current path available!");
        }
        getG2DState().updateClip(this.currentPath);
        this.currentPath = null;
    }

    /** {@inheritDoc} */
    @Override
    protected void closePath() {
        this.currentPath.closePath();
    }

    /** {@inheritDoc} */
    @Override
    protected void lineTo(final int x, final int y) {
        if (this.currentPath == null) {
            this.currentPath = new GeneralPath();
        }
        this.currentPath.lineTo(x, y);
    }

    /** {@inheritDoc} */
    @Override
    protected void moveTo(final int x, final int y) {
        if (this.currentPath == null) {
            this.currentPath = new GeneralPath();
        }
        this.currentPath.moveTo(x, y);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveGraphicsState() {
        this.painter.saveGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    protected void restoreGraphicsState() {
        this.painter.restoreGraphicsState();
        this.currentPath = null;
    }

}
