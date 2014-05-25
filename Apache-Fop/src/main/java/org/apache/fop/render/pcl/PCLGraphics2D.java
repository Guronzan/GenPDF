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

/* $Id: PCLGraphics2D.java 1297232 2012-03-05 21:13:28Z gadams $ */

package org.apache.fop.render.pcl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.IOException;
import java.text.AttributedCharacterIterator;

import org.apache.xmlgraphics.java2d.AbstractGraphics2D;
import org.apache.xmlgraphics.java2d.GraphicContext;
import org.apache.xmlgraphics.util.UnitConv;

/**
 * Graphics2D implementation implementing PCL and HP GL/2. Note: This class
 * cannot be used stand-alone to create full PCL documents.
 */
public class PCLGraphics2D extends AbstractGraphics2D {

    /** The PCL generator */
    protected PCLGenerator gen;

    private final boolean failOnUnsupportedFeature = true;
    private boolean clippingDisabled = false;

    /**
     * Create a new PCLGraphics2D.
     *
     * @param gen
     *            the PCL Generator to paint with
     */
    public PCLGraphics2D(final PCLGenerator gen) {
        super(true);
        this.gen = gen;
    }

    /**
     * Copy constructor
     *
     * @param g
     *            parent PCLGraphics2D
     */
    public PCLGraphics2D(final PCLGraphics2D g) {
        super(true);
        this.gen = g.gen;
    }

    /** {@inheritDoc} */
    @Override
    public Graphics create() {
        final PCLGraphics2D copy = new PCLGraphics2D(this);
        copy.setGraphicContext((GraphicContext) getGraphicContext().clone());
        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        this.gen = null;
    }

    /**
     * Sets the GraphicContext
     *
     * @param c
     *            GraphicContext to use
     */
    public void setGraphicContext(final GraphicContext c) {
        this.gc = c;
    }

    /**
     * Allows to disable all clipping operations.
     *
     * @param value
     *            true if clipping should be disabled.
     */
    public void setClippingDisabled(final boolean value) {
        this.clippingDisabled = value;
    }

    /**
     * Central handler for IOExceptions for this class.
     *
     * @param ioe
     *            IOException to handle
     */
    public void handleIOException(final IOException ioe) {
        // TODO Surely, there's a better way to do this.
        ioe.printStackTrace();
    }

    /**
     * Raises an UnsupportedOperationException if this instance is configured to
     * do so and an unsupported feature has been requested. Clients can make use
     * of this to fall back to a more compatible way of painting a PCL graphic.
     *
     * @param msg
     *            the error message to be displayed
     */
    protected void handleUnsupportedFeature(final String msg) {
        if (this.failOnUnsupportedFeature) {
            throw new UnsupportedOperationException(msg);
        }
    }

    /** {@inheritDoc} */
    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
    }

    /**
     * Applies a new Stroke object.
     *
     * @param stroke
     *            Stroke object to use
     * @throws IOException
     *             In case of an I/O problem
     */
    protected void applyStroke(final Stroke stroke) throws IOException {
        if (stroke instanceof BasicStroke) {
            final BasicStroke bs = (BasicStroke) stroke;

            final float[] da = bs.getDashArray();
            if (da != null) {

                this.gen.writeText("UL1,");
                final int len = Math.min(20, da.length);
                float patternLen = 0.0f;
                for (int idx = 0; idx < len; idx++) {
                    patternLen += da[idx];
                }
                if (len == 1) {
                    patternLen *= 2;
                }
                for (int idx = 0; idx < len; idx++) {
                    final float perc = da[idx] * 100 / patternLen;
                    this.gen.writeText(this.gen.formatDouble2(perc));
                    if (idx < da.length - 1) {
                        this.gen.writeText(",");
                    }
                }
                if (len == 1) {
                    this.gen.writeText(","
                            + this.gen.formatDouble2(da[0] * 100 / patternLen));

                }
                this.gen.writeText(";");
                /*
                 * TODO Dash phase NYI float offset = bs.getDashPhase();
                 * gen.writeln(gen.formatDouble4(offset) + " setdash");
                 */
                final Point2D ptLen = new Point2D.Double(patternLen, 0);
                // interpret as absolute length
                getTransform().deltaTransform(ptLen, ptLen);
                final double transLen = UnitConv.pt2mm(ptLen.distance(0, 0));
                this.gen.writeText("LT1," + this.gen.formatDouble4(transLen)
                        + ",1;");
            } else {
                this.gen.writeText("LT;");
            }

            this.gen.writeText("LA1"); // line cap
            final int ec = bs.getEndCap();
            switch (ec) {
            case BasicStroke.CAP_BUTT:
                this.gen.writeText(",1");
                break;
            case BasicStroke.CAP_ROUND:
                this.gen.writeText(",4");
                break;
            case BasicStroke.CAP_SQUARE:
                this.gen.writeText(",2");
                break;
            default:
                System.err.println("Unsupported line cap: " + ec);
            }

            this.gen.writeText(",2"); // line join
            final int lj = bs.getLineJoin();
            switch (lj) {
            case BasicStroke.JOIN_MITER:
                this.gen.writeText(",1");
                break;
            case BasicStroke.JOIN_ROUND:
                this.gen.writeText(",4");
                break;
            case BasicStroke.JOIN_BEVEL:
                this.gen.writeText(",5");
                break;
            default:
                System.err.println("Unsupported line join: " + lj);
            }

            final float ml = bs.getMiterLimit();
            this.gen.writeText(",3" + this.gen.formatDouble4(ml));

            final float lw = bs.getLineWidth();
            final Point2D ptSrc = new Point2D.Double(lw, 0);
            // Pen widths are set as absolute metric values (WU0;)
            final Point2D ptDest = getTransform().deltaTransform(ptSrc, null);
            final double transDist = UnitConv.pt2mm(ptDest.distance(0, 0));
            // log.info("--" + ptDest.distance(0, 0) + " " + transDist);
            this.gen.writeText(";PW" + this.gen.formatDouble4(transDist) + ";");

        } else {
            handleUnsupportedFeature("Unsupported Stroke: "
                    + stroke.getClass().getName());
        }
    }

    /**
     * Applies a new Paint object.
     *
     * @param paint
     *            Paint object to use
     * @throws IOException
     *             In case of an I/O problem
     */
    protected void applyPaint(final Paint paint) throws IOException {
        if (paint instanceof Color) {
            final Color col = (Color) paint;
            final int shade = this.gen.convertToPCLShade(col);
            this.gen.writeText("TR0;FT10," + shade + ";");
        } else {
            handleUnsupportedFeature("Unsupported Paint: "
                    + paint.getClass().getName());
        }
    }

    private void writeClip(final Shape imclip) {
        if (this.clippingDisabled) {
            return;
        }
        if (imclip == null) {
            // gen.writeText("IW;");
        } else {
            handleUnsupportedFeature("Clipping is not supported. Shape: "
                    + imclip);
            /*
             * This is an attempt to clip using the "InputWindow" (IW) but this
             * only allows to clip a rectangular area. Force falling back to
             * bitmap mode for now. Rectangle2D bounds = imclip.getBounds2D();
             * Point2D p1 = new Point2D.Double(bounds.getX(), bounds.getY());
             * Point2D p2 = new Point2D.Double( bounds.getX() +
             * bounds.getWidth(), bounds.getY() + bounds.getHeight());
             * getTransform().transform(p1, p1); getTransform().transform(p2,
             * p2); gen.writeText("IW" + gen.formatDouble4(p1.getX()) + "," +
             * gen.formatDouble4(p2.getY()) + "," + gen.formatDouble4(p2.getX())
             * + "," + gen.formatDouble4(p1.getY()) + ";");
             */
        }
    }

    /** {@inheritDoc} */
    @Override
    public void draw(final Shape s) {
        try {
            final AffineTransform trans = getTransform();

            final Shape imclip = getClip();
            writeClip(imclip);

            if (!Color.black.equals(getColor())) {
                // TODO PCL 5 doesn't support colored pens, PCL5c has a pen
                // color (PC) command
                handleUnsupportedFeature("Only black is supported as stroke color: "
                        + getColor());
            }
            applyStroke(getStroke());

            final PathIterator iter = s.getPathIterator(trans);
            processPathIteratorStroke(iter);
            writeClip(null);
        } catch (final IOException ioe) {
            handleIOException(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void fill(final Shape s) {
        try {
            final AffineTransform trans = getTransform();
            final Shape imclip = getClip();
            writeClip(imclip);

            applyPaint(getPaint());

            final PathIterator iter = s.getPathIterator(trans);
            processPathIteratorFill(iter);
            writeClip(null);
        } catch (final IOException ioe) {
            handleIOException(ioe);
        }
    }

    /**
     * Processes a path iterator generating the nexessary painting operations.
     *
     * @param iter
     *            PathIterator to process
     * @throws IOException
     *             In case of an I/O problem.
     */
    public void processPathIteratorStroke(final PathIterator iter)
            throws IOException {
        this.gen.writeText("\n");
        final double[] vals = new double[6];
        boolean penDown = false;
        double x = 0;
        double y = 0;
        final StringBuilder sb = new StringBuilder(256);
        penUp(sb);
        while (!iter.isDone()) {
            final int type = iter.currentSegment(vals);
            if (type == PathIterator.SEG_CLOSE) {
                this.gen.writeText("PM;");
                this.gen.writeText(sb.toString());
                this.gen.writeText("PM2;EP;");
                sb.setLength(0);
                iter.next();
                continue;
            } else if (type == PathIterator.SEG_MOVETO) {
                this.gen.writeText(sb.toString());
                sb.setLength(0);
                if (penDown) {
                    penUp(sb);
                    penDown = false;
                }
            } else {
                if (!penDown) {
                    penDown(sb);
                    penDown = true;
                }
            }
            switch (type) {
            case PathIterator.SEG_CLOSE:
                break;
            case PathIterator.SEG_MOVETO:
                x = vals[0];
                y = vals[1];
                plotAbsolute(x, y, sb);
                this.gen.writeText(sb.toString());
                sb.setLength(0);
                break;
            case PathIterator.SEG_LINETO:
                x = vals[0];
                y = vals[1];
                plotAbsolute(x, y, sb);
                break;
            case PathIterator.SEG_CUBICTO:
                x = vals[4];
                y = vals[5];
                bezierAbsolute(vals[0], vals[1], vals[2], vals[3], x, y, sb);
                break;
            case PathIterator.SEG_QUADTO:
                final double originX = x;
                final double originY = y;
                x = vals[2];
                y = vals[3];
                quadraticBezierAbsolute(originX, originY, vals[0], vals[1], x,
                        y, sb);
                break;
            default:
                break;
            }
            iter.next();
        }
        sb.append("\n");
        this.gen.writeText(sb.toString());
    }

    /**
     * Processes a path iterator generating the nexessary painting operations.
     *
     * @param iter
     *            PathIterator to process
     * @throws IOException
     *             In case of an I/O problem.
     */
    public void processPathIteratorFill(final PathIterator iter)
            throws IOException {
        this.gen.writeText("\n");
        final double[] vals = new double[6];
        boolean penDown = false;
        double x = 0;
        double y = 0;
        boolean pendingPM0 = true;
        final StringBuilder sb = new StringBuilder(256);
        penUp(sb);
        while (!iter.isDone()) {
            final int type = iter.currentSegment(vals);
            if (type == PathIterator.SEG_CLOSE) {
                sb.append("PM1;");
                iter.next();
                continue;
            } else if (type == PathIterator.SEG_MOVETO) {
                if (penDown) {
                    penUp(sb);
                    penDown = false;
                }
            } else {
                if (!penDown) {
                    penDown(sb);
                    penDown = true;
                }
            }
            switch (type) {
            case PathIterator.SEG_MOVETO:
                x = vals[0];
                y = vals[1];
                plotAbsolute(x, y, sb);
                break;
            case PathIterator.SEG_LINETO:
                x = vals[0];
                y = vals[1];
                plotAbsolute(x, y, sb);
                break;
            case PathIterator.SEG_CUBICTO:
                x = vals[4];
                y = vals[5];
                bezierAbsolute(vals[0], vals[1], vals[2], vals[3], x, y, sb);
                break;
            case PathIterator.SEG_QUADTO:
                final double originX = x;
                final double originY = y;
                x = vals[2];
                y = vals[3];
                quadraticBezierAbsolute(originX, originY, vals[0], vals[1], x,
                        y, sb);
                break;
            default:
                throw new IllegalStateException("Must not get here");
            }
            if (pendingPM0) {
                pendingPM0 = false;
                sb.append("PM;");
            }
            iter.next();
        }
        sb.append("PM2;");
        fillPolygon(iter.getWindingRule(), sb);
        sb.append("\n");
        this.gen.writeText(sb.toString());
    }

    private void fillPolygon(final int windingRule, final StringBuilder sb) {
        final int fillMethod = windingRule == PathIterator.WIND_EVEN_ODD ? 0
                : 1;
        sb.append("FP").append(fillMethod).append(";");
    }

    private void plotAbsolute(final double x, final double y,
            final StringBuilder sb) {
        sb.append("PA").append(this.gen.formatDouble4(x));
        sb.append(",").append(this.gen.formatDouble4(y)).append(";");
    }

    private void bezierAbsolute(final double x1, final double y1,
            final double x2, final double y2, final double x3, final double y3,
            final StringBuilder sb) {
        sb.append("BZ").append(this.gen.formatDouble4(x1));
        sb.append(",").append(this.gen.formatDouble4(y1));
        sb.append(",").append(this.gen.formatDouble4(x2));
        sb.append(",").append(this.gen.formatDouble4(y2));
        sb.append(",").append(this.gen.formatDouble4(x3));
        sb.append(",").append(this.gen.formatDouble4(y3)).append(";");
    }

    private void quadraticBezierAbsolute(final double originX,
            final double originY, final double x1, final double y1,
            final double x2, final double y2, final StringBuilder sb) {
        // Quadratic Bezier curve can be mapped to a normal bezier curve
        // See http://pfaedit.sourceforge.net/bezier.html
        final double nx1 = originX + 2.0 / 3.0 * (x1 - originX);
        final double ny1 = originY + 2.0 / 3.0 * (y1 - originY);

        final double nx2 = nx1 + 1.0 / 3.0 * (x2 - originX);
        final double ny2 = ny1 + 1.0 / 3.0 * (y2 - originY);

        bezierAbsolute(nx1, ny1, nx2, ny2, x2, y2, sb);
    }

    private void penDown(final StringBuilder sb) {
        sb.append("PD;");
    }

    private void penUp(final StringBuilder sb) {
        sb.append("PU;");
    }

    /** {@inheritDoc} */
    @Override
    public void drawString(final String s, final float x, final float y) {
        final java.awt.Font awtFont = getFont();
        final FontRenderContext frc = getFontRenderContext();
        final GlyphVector gv = awtFont.createGlyphVector(frc, s);
        final Shape glyphOutline = gv.getOutline(x, y);
        fill(glyphOutline);
    }

    /** {@inheritDoc} */
    @Override
    public void drawString(final AttributedCharacterIterator iterator,
            final float x, final float y) {
        // TODO Auto-generated method stub
        handleUnsupportedFeature("drawString NYI");
    }

    /** {@inheritDoc} */
    @Override
    public void drawRenderedImage(final RenderedImage img,
            final AffineTransform xform) {
        handleUnsupportedFeature("Bitmap images are not supported");
    }

    /** {@inheritDoc} */
    @Override
    public void drawRenderableImage(final RenderableImage img,
            final AffineTransform xform) {
        handleUnsupportedFeature("Bitmap images are not supported");
    }

    /** {@inheritDoc} */
    @Override
    public boolean drawImage(final Image img, final int x, final int y,
            final int width, final int height, final ImageObserver observer) {
        handleUnsupportedFeature("Bitmap images are not supported");
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean drawImage(final Image img, final int x, final int y,
            final ImageObserver observer) {
        handleUnsupportedFeature("Bitmap images are not supported");
        return false;
        /*
         * First attempt disabled. Reasons: Lack of transparency control,
         * positioning and rotation issues final int width =
         * img.getWidth(observer); final int height = img.getHeight(observer);
         * if (width == -1 || height == -1) { return false; }
         * 
         * Dimension size = new Dimension(width, height); BufferedImage buf =
         * buildBufferedImage(size);
         * 
         * java.awt.Graphics2D g = buf.createGraphics(); try {
         * g.setComposite(AlphaComposite.SrcOver); g.setBackground(new
         * Color(255, 255, 255)); g.setPaint(new Color(255, 255, 255));
         * g.fillRect(0, 0, width, height); g.clip(new Rectangle(0, 0,
         * buf.getWidth(), buf.getHeight()));
         * 
         * if (!g.drawImage(img, 0, 0, observer)) { return false; } } finally {
         * g.dispose(); }
         * 
         * try { AffineTransform at = getTransform(); gen.enterPCLMode(false);
         * //Shape imclip = getClip(); Clipping is not available in PCL Point2D
         * p1 = new Point2D.Double(x, y); at.transform(p1, p1);
         * pclContext.getTransform().transform(p1, p1);
         * gen.setCursorPos(p1.getX(), p1.getY()); gen.paintBitmap(buf, 72);
         * gen.enterHPGL2Mode(false); } catch (IOException ioe) {
         * handleIOException(ioe); }
         * 
         * return true;
         */
    }

    /** {@inheritDoc} */
    @Override
    public void copyArea(final int x, final int y, final int width,
            final int height, final int dx, final int dy) {
        // TODO Auto-generated method stub
        handleUnsupportedFeature("copyArea NYI");
    }

    /** {@inheritDoc} */
    @Override
    public void setXORMode(final Color c1) {
        // TODO Auto-generated method stub
        handleUnsupportedFeature("setXORMode NYI");
    }

    /**
     * Used to create proper font metrics
     */
    private Graphics2D fmg;

    {
        final BufferedImage bi = new BufferedImage(1, 1,
                BufferedImage.TYPE_INT_ARGB);

        this.fmg = bi.createGraphics();
    }

    /**
     * Creates a buffered image.
     *
     * @param size
     *            dimensions of the image to be created
     * @return the buffered image
     */
    protected BufferedImage buildBufferedImage(final Dimension size) {
        return new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_BYTE_GRAY);
    }

    /** {@inheritDoc} */
    @Override
    public java.awt.FontMetrics getFontMetrics(final java.awt.Font f) {
        return this.fmg.getFontMetrics(f);
    }

}
