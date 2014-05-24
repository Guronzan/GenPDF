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

/* $Id: Java2DGraphicsState.java 1069439 2011-02-10 15:58:57Z jeremias $ */

package org.apache.fop.render.java2d;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;

import org.apache.fop.fo.Constants;
import org.apache.fop.fonts.FontInfo;
import org.apache.xmlgraphics.java2d.color.ColorUtil;

/**
 * Keeps information about the current state of the Graphics2D currentGraphics.
 * It is also used as a stack to hold a graphics context.
 * <p>
 * The graphics context is updated with the updateXXX() methods.
 */
public class Java2DGraphicsState {

    /** Holds the datas of the current state */
    private Graphics2D currentGraphics;

    private BasicStroke currentStroke;

    private float currentStrokeWidth;

    private int currentStrokeStyle;

    /** Font configuration, passed from AWTRenderer */
    private final FontInfo fontInfo;

    /** Initial AffinTransform passed by the renderer, includes scaling-info */
    private final AffineTransform initialTransform;

    /**
     * State for storing graphics state.
     * 
     * @param graphics
     *            the graphics associated with the BufferedImage
     * @param fontInfo
     *            the FontInfo from the renderer
     * @param at
     *            the initial AffineTransform containing the scale
     *            transformation
     */
    public Java2DGraphicsState(final Graphics2D graphics,
            final FontInfo fontInfo, final AffineTransform at) {
        this.fontInfo = fontInfo;
        this.currentGraphics = graphics;
        this.initialTransform = at;
        this.currentGraphics.setTransform(at);
    }

    /**
     * Copy constructor.
     * 
     * @param org
     *            the instance to copy
     */
    public Java2DGraphicsState(final Java2DGraphicsState org) {
        this.currentGraphics = (Graphics2D) org.currentGraphics.create();
        this.fontInfo = org.fontInfo;
        this.initialTransform = org.initialTransform;
        this.currentStroke = org.currentStroke;
        this.currentStrokeStyle = org.currentStrokeStyle;
        this.currentStrokeWidth = org.currentStrokeWidth;
    }

    /**
     * @return the currently valid state
     */
    public Graphics2D getGraph() {
        return this.currentGraphics;
    }

    /** Frees resources allocated by the current Graphics2D instance. */
    public void dispose() {
        this.currentGraphics.dispose();
        this.currentGraphics = null;

    }

    /**
     * Set the current background color. Check if the background color will
     * change and then set the new color.
     *
     * @param col
     *            the new color as a java.awt.Color
     * @return true if the background color has changed
     */
    public boolean updateColor(final Color col) {
        if (!ColorUtil.isSameColor(col, getGraph().getColor())) {
            getGraph().setColor(col);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the current java.awt.Color
     */
    public java.awt.Color getColor() {
        return this.currentGraphics.getColor();
    }

    /**
     * Set the current font name. Check if the font name will change and then
     * set the new name.
     *
     * @param name
     *            the new font name
     * @param size
     *            the font size
     * @return true if the new Font changes the current Font
     */
    public boolean updateFont(final String name, final int size) {

        final FontMetricsMapper mapper = (FontMetricsMapper) this.fontInfo
                .getMetricsFor(name);
        final boolean updateName = !mapper.getFontName().equals(
                getGraph().getFont().getFontName());
        final boolean updateSize = size != getGraph().getFont().getSize() * 1000;

        if (updateName || updateSize) {
            // the font name and/or the font size have changed
            final java.awt.Font font = mapper.getFont(size);

            this.currentGraphics.setFont(font);
            return true;
        } else {
            return false;
        }
    }

    /** @return the current java.awt.Font */
    public java.awt.Font getFont() {
        return this.currentGraphics.getFont();
    }

    /**
     * Sets the current Stroke. The line width should be set with
     * updateLineWidth() before calling this method
     *
     * @param width
     *            the line width
     * @param style
     *            the constant for the style of the line as an int
     * @return true if the new Stroke changes the current Stroke
     */
    public boolean updateStroke(final float width, final int style) {

        boolean update = false;

        // only update if necessary
        if (width != this.currentStrokeWidth
                || style != this.currentStrokeStyle) {

            update = true;

            switch (style) {
            case Constants.EN_DOTTED:

                this.currentStroke = new BasicStroke(width,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0f,
                        new float[] { 0, 2 * width }, width);
                this.currentGraphics.setStroke(this.currentStroke);

                this.currentStrokeWidth = width;
                this.currentStrokeStyle = style;

                break;

            case Constants.EN_DASHED:

                this.currentStroke = new BasicStroke(width,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f,
                        new float[] { 8f, 2f }, 0f);
                this.currentGraphics.setStroke(this.currentStroke);

                this.currentStrokeWidth = width;
                this.currentStrokeStyle = style;

                break;

            default: // EN_SOLID:

                this.currentStroke = new BasicStroke(width);
                this.currentGraphics.setStroke(this.currentStroke);

                this.currentStrokeWidth = width;
                this.currentStrokeStyle = style;

                break;
            }
        }

        return update;
    }

    /** @return the currently active Stroke */
    public BasicStroke getStroke() {
        return (BasicStroke) this.currentGraphics.getStroke();
    }

    /**
     * Set the current paint. This checks if the paint will change and then sets
     * the current paint.
     *
     * @param p
     *            the new paint
     * @return true if the new paint changes the current paint
     */
    public boolean updatePaint(final Paint p) {
        final Paint currentPaint = getGraph().getPaint();
        if (currentPaint == null) {
            if (p != null) {
                getGraph().setPaint(p);
                return true;
            }
        } else if (p instanceof Color && currentPaint instanceof Color) {
            if (!ColorUtil.isSameColor((Color) p, (Color) currentPaint)) {
                getGraph().setPaint(p);
                return true;
            }
        } else if (!p.equals(currentPaint)) {
            getGraph().setPaint(p);
            return true;
        }
        return false;
    }

    /**
     * Set the current clip. This either sets a new clip or sets the clip to the
     * intersect of the old clip and the new clip.
     *
     * @param cl
     *            the new clip in the current state
     * @return true if the clip shape needed to be updated
     */
    public boolean updateClip(final Shape cl) {
        if (getGraph().getClip() != null) {
            final Area newClip = new Area(getGraph().getClip());
            newClip.intersect(new Area(cl));
            getGraph().setClip(new GeneralPath(newClip));
        } else {
            getGraph().setClip(cl);
        }
        return true; // TODO only update if necessary
    }

    /**
     * Composes an AffineTransform object with the Transform in this Graphics2D
     * according to the rule last-specified-first-applied.
     * 
     * @see java.awt.Graphics2D#transform(AffineTransform)
     *
     * @param tf
     *            the transform to concatenate to the current level transform
     */
    public void transform(final AffineTransform tf) {
        if (!tf.isIdentity()) {
            getGraph().transform(tf);
        }
    }

    /**
     * Get the current transform. This gets the combination of all transforms in
     * the current state.
     *
     * @return the calculate combined transform for the current state
     */
    public AffineTransform getTransform() {
        return getGraph().getTransform();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String s = "Java2DGraphicsState "
                + this.currentGraphics.toString() + ", Stroke (width: "
                + this.currentStrokeWidth + " style: "
                + this.currentStrokeStyle + "), " + getTransform();
        return s;
    }

}
