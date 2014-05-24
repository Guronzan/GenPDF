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

/* $Id: TXTRenderer.java 1326144 2012-04-14 16:48:59Z gadams $ */

package org.apache.fop.render.txt;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.Area;
import org.apache.fop.area.CTM;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.inline.Image;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.render.AbstractPathOrientedRenderer;
import org.apache.fop.render.txt.border.AbstractBorderElement;
import org.apache.fop.render.txt.border.BorderManager;
import org.apache.xmlgraphics.util.UnitConv;

/**
 * <p>
 * Renderer that renders areas to plain text.
 * </p>
 *
 * <p>
 * This work was authored by Art Welch and Mark Lillywhite
 * (mark-fop@inomial.com) [to use the new Renderer interface].
 * </p>
 */
@Slf4j
public class TXTRenderer extends AbstractPathOrientedRenderer {

    private static final char LIGHT_SHADE = '\u2591';

    private static final char MEDIUM_SHADE = '\u2592';

    private static final char DARK_SHADE = '\u2593';

    private static final char FULL_BLOCK = '\u2588';

    private static final char IMAGE_CHAR = '#';

    /** The stream for output */
    private OutputStream outputStream;

    /** The current stream to add Text commands to. */
    private TXTStream currentStream;

    /** Buffer for text. */
    private StringBuffer[] charData;

    /** Buffer for background and images. */
    private StringBuffer[] decoData;

    /** Leading of line containing Courier font size of 10pt. */
    public static final int LINE_LEADING = 1070;

    /** Height of one symbol in Courier font size of 10pt. */
    public static final int CHAR_HEIGHT = 7860;

    /** Width of one symbol in Courier font size of 10pt. */
    public static final int CHAR_WIDTH = 6000;

    /** Current processing page width. */
    private int pageWidth;

    /** Current processing page height. */
    private int pageHeight;

    /**
     * Every line except the last line on a page (which will end with
     * pageEnding) will be terminated with this string.
     */
    private final String lineEnding = "\r\n";

    /** Every page except the last one will end with this string. */
    private final String pageEnding = "\f";

    /** Equals true, if current page is first. */
    private boolean firstPage = false;

    /** Manager for storing border's information. */
    private BorderManager bm;

    /** Char for current filling. */
    private char fillChar;

    /** Saves current coordinate transformation. */
    private final TXTState currentState = new TXTState();

    private String encoding;

    /**
     * Constructs a newly allocated <code>TXTRenderer</code> object.
     *
     * @param userAgent
     *            the user agent that contains configuration details. This
     *            cannot be null.
     */
    public TXTRenderer(final FOUserAgent userAgent) {
        super(userAgent);
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return "text/plain";
    }

    /**
     * Sets the encoding of the target file.
     *
     * @param encoding
     *            the encoding, null to select the default encoding (UTF-8)
     */
    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    /**
     * Indicates if point (x, y) lay inside currentPage.
     *
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     * @return <b>true</b> if point lay inside page
     */
    public boolean isLayInside(final int x, final int y) {
        return x >= 0 && x < this.pageWidth && y >= 0 && y < this.pageHeight;
    }

    /**
     * Add char to text buffer.
     *
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     * @param ch
     *            char to add
     * @param ischar
     *            boolean, repersenting is character adding to text buffer
     */
    protected void addChar(final int x, final int y, final char ch,
            final boolean ischar) {
        final Point point = this.currentState.transformPoint(x, y);
        putChar(point.x, point.y, ch, ischar);
    }

    /**
     * Add char to text or background buffer.
     *
     * @param x
     *            x coordinate
     * @param y
     *            x coordinate
     * @param ch
     *            char to add
     * @param ischar
     *            indicates if it char or background
     */
    protected void putChar(final int x, final int y, final char ch,
            final boolean ischar) {
        if (isLayInside(x, y)) {
            final StringBuffer sb = ischar ? this.charData[y]
                    : this.decoData[y];
            while (sb.length() <= x) {
                sb.append(' ');
            }
            sb.setCharAt(x, ch);
        }
    }

    /**
     * Adds string to text buffer (<code>charData</code>).
     * <p>
     * Chars of string map in turn.
     *
     * @param row
     *            x coordinate
     * @param col
     *            y coordinate
     * @param s
     *            string to add
     */
    protected void addString(final int row, final int col, final String s) {
        for (int l = 0; l < s.length(); l++) {
            addChar(col + l, row, s.charAt(l), true);
        }
    }

    /**
     * Render TextArea to Text.
     *
     * @param area
     *            inline area to render
     */
    @Override
    protected void renderText(final TextArea area) {
        final int col = Helper.ceilPosition(this.currentIPPosition, CHAR_WIDTH);
        final int row = Helper.ceilPosition(this.currentBPPosition
                - LINE_LEADING, CHAR_HEIGHT + 2 * LINE_LEADING);

        final String s = area.getText();

        addString(row, col, s);

        super.renderText(area);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderPage(final PageViewport page) throws IOException,
    FOPException {
        if (this.firstPage) {
            this.firstPage = false;
        } else {
            this.currentStream.add(this.pageEnding);
        }

        final Rectangle2D bounds = page.getViewArea();
        final double width = bounds.getWidth();
        final double height = bounds.getHeight();

        this.pageWidth = Helper.ceilPosition((int) width, CHAR_WIDTH);
        this.pageHeight = Helper.ceilPosition((int) height, CHAR_HEIGHT + 2
                * LINE_LEADING);

        // init buffers
        this.charData = new StringBuffer[this.pageHeight];
        this.decoData = new StringBuffer[this.pageHeight];
        for (int i = 0; i < this.pageHeight; i++) {
            this.charData[i] = new StringBuffer();
            this.decoData[i] = new StringBuffer();
        }

        this.bm = new BorderManager(this.pageWidth, this.pageHeight,
                this.currentState);

        super.renderPage(page);

        flushBorderToBuffer();
        flushBuffer();
    }

    /**
     * Projects current page borders (i.e.<code>bm</code>) to buffer for
     * background and images (i.e.<code>decoData</code>).
     */
    private void flushBorderToBuffer() {
        for (int x = 0; x < this.pageWidth; x++) {
            for (int y = 0; y < this.pageHeight; y++) {
                final Character c = this.bm.getCharacter(x, y);
                if (c != null) {
                    putChar(x, y, c.charValue(), false);
                }
            }
        }
    }

    /**
     * Write out the buffer to output stream.
     */
    private void flushBuffer() {
        for (int row = 0; row < this.pageHeight; row++) {
            final StringBuffer cr = this.charData[row];
            final StringBuffer dr = this.decoData[row];
            StringBuffer outr = null;

            if (cr != null && dr == null) {
                outr = cr;
            } else if (dr != null && cr == null) {
                outr = dr;
            } else if (cr != null && dr != null) {
                int len = dr.length();
                if (cr.length() > len) {
                    len = cr.length();
                }
                outr = new StringBuffer();
                for (int countr = 0; countr < len; countr++) {
                    if (countr < cr.length() && cr.charAt(countr) != ' ') {
                        outr.append(cr.charAt(countr));
                    } else if (countr < dr.length()) {
                        outr.append(dr.charAt(countr));
                    } else {
                        outr.append(' ');
                    }
                }
            }

            if (outr != null) {
                this.currentStream.add(outr.toString());
            }
            if (row < this.pageHeight) {
                this.currentStream.add(this.lineEnding);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startRenderer(final OutputStream os) throws IOException {
        log.info("Rendering areas to TEXT.");
        this.outputStream = os;
        this.currentStream = new TXTStream(os);
        this.currentStream.setEncoding(this.encoding);
        this.firstPage = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopRenderer() throws IOException {
        log.info("writing out TEXT");
        this.outputStream.flush();
        super.stopRenderer();
    }

    /**
     * Does nothing. {@inheritDoc}
     */
    @Override
    protected void restoreStateStackAfterBreakOut(final List breakOutList) {
    }

    /**
     * Does nothing.
     *
     * @return null {@inheritDoc}
     */
    @Override
    protected List breakOutOfStateStack() {
        return null;
    }

    /**
     * Does nothing. {@inheritDoc}
     */
    @Override
    protected void saveGraphicsState() {
        this.currentState.push(new CTM());
    }

    /**
     * Does nothing. {@inheritDoc}
     */
    @Override
    protected void restoreGraphicsState() {
        this.currentState.pop();
    }

    /**
     * Does nothing. {@inheritDoc}
     */
    @Override
    protected void beginTextObject() {
    }

    /**
     * Does nothing. {@inheritDoc}
     */
    @Override
    protected void endTextObject() {
    }

    /**
     * Does nothing. {@inheritDoc}
     */
    @Override
    protected void clip() {
    }

    /**
     * Does nothing. {@inheritDoc}
     */
    @Override
    protected void clipRect(final float x, final float y, final float width,
            final float height) {
    }

    /**
     * Does nothing. {@inheritDoc}
     */
    @Override
    protected void moveTo(final float x, final float y) {
    }

    /**
     * Does nothing. {@inheritDoc}
     */
    @Override
    protected void lineTo(final float x, final float y) {
    }

    /**
     * Does nothing. {@inheritDoc}
     */
    @Override
    protected void closePath() {
    }

    /**
     * Fills rectangle startX, startY, width, height with char
     * <code>charToFill</code>.
     *
     * @param startX
     *            x-coordinate of upper left point
     * @param startY
     *            y-coordinate of upper left point
     * @param width
     *            width of rectangle
     * @param height
     *            height of rectangle
     * @param charToFill
     *            filling char
     */
    private void fillRect(final int startX, final int startY, final int width,
            final int height, final char charToFill) {
        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                addChar(x, y, charToFill, false);
            }
        }
    }

    /**
     * Fills a rectangular area with the current filling char. {@inheritDoc}
     */
    @Override
    protected void fillRect(final float x, final float y, final float width,
            final float height) {
        fillRect(this.bm.getStartX(), this.bm.getStartY(), this.bm.getWidth(),
                this.bm.getHeight(), this.fillChar);
    }

    /**
     * Changes current filling char. {@inheritDoc}
     */
    @Override
    protected void updateColor(final Color col, final boolean fill) {
        if (col == null) {
            return;
        }
        // fillShade evaluation was taken from fop-0.20.5
        // TODO: This fillShase is catually the luminance component of the color
        // transformed to the YUV (YPrBb) Colorspace. It should use standard
        // Java methods for its conversion instead of the formula given here.
        double fillShade = 0.30f / 255f * col.getRed() + 0.59f / 255f
                * col.getGreen() + 0.11f / 255f * col.getBlue();
        fillShade = 1 - fillShade;

        if (fillShade > 0.8f) {
            this.fillChar = FULL_BLOCK;
        } else if (fillShade > 0.6f) {
            this.fillChar = DARK_SHADE;
        } else if (fillShade > 0.4f) {
            this.fillChar = MEDIUM_SHADE;
        } else if (fillShade > 0.2f) {
            this.fillChar = LIGHT_SHADE;
        } else {
            this.fillChar = ' ';
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void drawImage(final String url, final Rectangle2D pos,
            final Map foreignAttributes) {
        // No images are painted here
    }

    /**
     * Fills image rectangle with a <code>IMAGE_CHAR</code>.
     *
     * @param image
     *            the base image
     * @param pos
     *            the position of the image
     */
    @Override
    public void renderImage(final Image image, final Rectangle2D pos) {
        final int x1 = Helper.ceilPosition(this.currentIPPosition, CHAR_WIDTH);
        final int y1 = Helper.ceilPosition(this.currentBPPosition
                - LINE_LEADING, CHAR_HEIGHT + 2 * LINE_LEADING);
        final int width = Helper.ceilPosition((int) pos.getWidth(), CHAR_WIDTH);
        final int height = Helper.ceilPosition((int) pos.getHeight(),
                CHAR_HEIGHT + 2 * LINE_LEADING);

        fillRect(x1, y1, width, height, IMAGE_CHAR);
    }

    /**
     * Returns the closest integer to the multiplication of a number and 1000.
     *
     * @param x
     *            the value of the argument, multiplied by 1000 and rounded
     * @return the value of the argument multiplied by 1000 and rounded to the
     *         nearest integer
     */
    protected int toMilli(final float x) {
        return Math.round(x * 1000f);
    }

    /**
     * Adds one element of border.
     *
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     * @param style
     *            integer, representing border style
     * @param type
     *            integer, representing border element type
     */
    private void addBitOfBorder(final int x, final int y, final int style,
            final int type) {
        final Point point = this.currentState.transformPoint(x, y);
        if (isLayInside(point.x, point.y)) {
            this.bm.addBorderElement(point.x, point.y, style, type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void drawBorderLine(
            // CSOK: ParameterNumber
            final float x1, final float y1, final float x2, final float y2,
            final boolean horz, final boolean startOrBefore, final int style,
            final Color col) {

        final int borderHeight = this.bm.getHeight();
        final int borderWidth = this.bm.getWidth();
        final int borderStartX = this.bm.getStartX();
        final int borderStartY = this.bm.getStartY();

        int x;
        int y;
        if (horz && startOrBefore) { // BEFORE
            x = borderStartX;
            y = borderStartY;
        } else if (horz && !startOrBefore) { // AFTER
            x = borderStartX;
            y = borderStartY + borderHeight - 1;
        } else if (!horz && startOrBefore) { // START
            x = borderStartX;
            y = borderStartY;
        } else { // END
            x = borderStartX + borderWidth - 1;
            y = borderStartY;
        }

        int dx;
        int dy;
        int length;
        int startType;
        int endType;
        if (horz) {
            length = borderWidth;
            dx = 1;
            dy = 0;
            startType = 1 << AbstractBorderElement.RIGHT;
            endType = 1 << AbstractBorderElement.LEFT;
        } else {
            length = borderHeight;
            dx = 0;
            dy = 1;
            startType = 1 << AbstractBorderElement.DOWN;
            endType = 1 << AbstractBorderElement.UP;
        }

        addBitOfBorder(x, y, style, startType);
        for (int i = 0; i < length - 2; i++) {
            x += dx;
            y += dy;
            addBitOfBorder(x, y, style, startType + endType);
        }
        x += dx;
        y += dy;
        addBitOfBorder(x, y, style, endType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void drawBackAndBorders(final Area area, final float startx,
            final float starty, final float width, final float height) {
        this.bm.setWidth(Helper.ceilPosition(toMilli(width), CHAR_WIDTH));
        this.bm.setHeight(Helper.ceilPosition(toMilli(height), CHAR_HEIGHT + 2
                * LINE_LEADING));
        this.bm.setStartX(Helper.ceilPosition(toMilli(startx), CHAR_WIDTH));
        this.bm.setStartY(Helper.ceilPosition(toMilli(starty), CHAR_HEIGHT + 2
                * LINE_LEADING));

        super.drawBackAndBorders(area, startx, starty, width, height);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void startVParea(final CTM ctm, final Rectangle clippingRect) {
        this.currentState.push(ctm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void endVParea() {
        this.currentState.pop();
    }

    /** {@inheritDoc} */
    @Override
    protected void concatenateTransformationMatrix(final AffineTransform at) {
        this.currentState.push(new CTM(UnitConv.ptToMpt(at)));
    }

}
