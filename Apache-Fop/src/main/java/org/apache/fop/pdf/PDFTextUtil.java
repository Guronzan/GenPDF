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

/* $Id: PDFTextUtil.java 1293736 2012-02-26 02:29:01Z gadams $ */

package org.apache.fop.pdf;

import java.awt.geom.AffineTransform;

/**
 * Utility class for generating PDF text objects. It needs to be subclassed to
 * add writing functionality (see {@link #write(String)}).
 */
public abstract class PDFTextUtil {

    /** The number of decimal places. */
    private static final int DEC = 8;

    /** PDF text rendering mode: Fill text */
    public static final int TR_FILL = 0;
    /** PDF text rendering mode: Stroke text */
    public static final int TR_STROKE = 1;
    /** PDF text rendering mode: Fill, then stroke text */
    public static final int TR_FILL_STROKE = 2;
    /** PDF text rendering mode: Neither fill nor stroke text (invisible) */
    public static final int TR_INVISIBLE = 3;
    /** PDF text rendering mode: Fill text and add to path for clipping */
    public static final int TR_FILL_CLIP = 4;
    /** PDF text rendering mode: Stroke text and add to path for clipping */
    public static final int TR_STROKE_CLIP = 5;
    /**
     * PDF text rendering mode: Fill, then stroke text and add to path for
     * clipping
     */
    public static final int TR_FILL_STROKE_CLIP = 6;
    /** PDF text rendering mode: Add text to path for clipping */
    public static final int TR_CLIP = 7;

    private boolean inTextObject = false;
    private String startText;
    private String endText;
    private boolean useMultiByte;
    private StringBuilder bufTJ;
    private int textRenderingMode = TR_FILL;

    private String currentFontName;
    private double currentFontSize;

    /**
     * Main constructor.
     */
    public PDFTextUtil() {
        // nop
    }

    /**
     * Writes PDF code.
     * 
     * @param code
     *            the PDF code to write
     */
    protected abstract void write(final String code);

    private void writeAffineTransform(final AffineTransform at,
            final StringBuilder sb) {
        final double[] lt = new double[6];
        at.getMatrix(lt);
        sb.append(PDFNumber.doubleOut(lt[0], DEC)).append(" ");
        sb.append(PDFNumber.doubleOut(lt[1], DEC)).append(" ");
        sb.append(PDFNumber.doubleOut(lt[2], DEC)).append(" ");
        sb.append(PDFNumber.doubleOut(lt[3], DEC)).append(" ");
        sb.append(PDFNumber.doubleOut(lt[4], DEC)).append(" ");
        sb.append(PDFNumber.doubleOut(lt[5], DEC));
    }

    private static void writeChar(final char ch, final StringBuilder sb,
            final boolean multibyte) {
        if (!multibyte) {
            if (ch < 32 || ch > 127) {
                sb.append("\\").append(Integer.toOctalString(ch));
            } else {
                switch (ch) {
                case '(':
                case ')':
                case '\\':
                    sb.append("\\");
                    break;
                default:
                }
                sb.append(ch);
            }
        } else {
            sb.append(PDFText.toUnicodeHex(ch));
        }
    }

    private void writeChar(final char ch, final StringBuilder sb) {
        writeChar(ch, sb, this.useMultiByte);
    }

    private void checkInTextObject() {
        if (!this.inTextObject) {
            throw new IllegalStateException("Not in text object");
        }
    }

    /**
     * Indicates whether we are in a text object or not.
     * 
     * @return true if we are in a text object
     */
    public boolean isInTextObject() {
        return this.inTextObject;
    }

    /**
     * Called when a new text object should be started. Be sure to call
     * setFont() before issuing any text painting commands.
     */
    public void beginTextObject() {
        if (this.inTextObject) {
            throw new IllegalStateException("Already in text object");
        }
        write("BT\n");
        this.inTextObject = true;
    }

    /**
     * Called when a text object should be ended.
     */
    public void endTextObject() {
        checkInTextObject();
        write("ET\n");
        this.inTextObject = false;
        initValues();
    }

    /**
     * Resets the state fields.
     */
    protected void initValues() {
        this.currentFontName = null;
        this.currentFontSize = 0.0;
        this.textRenderingMode = TR_FILL;
    }

    /**
     * Creates a "cm" command.
     * 
     * @param at
     *            the transformation matrix
     */
    public void concatMatrix(final AffineTransform at) {
        if (!at.isIdentity()) {
            writeTJ();
            final StringBuilder sb = new StringBuilder();
            writeAffineTransform(at, sb);
            sb.append(" cm\n");
            write(sb.toString());
        }
    }

    /**
     * Writes a "Tf" command, setting a new current font.
     * 
     * @param fontName
     *            the name of the font to select
     * @param fontSize
     *            the font size (in points)
     */
    public void writeTf(final String fontName, final double fontSize) {
        checkInTextObject();
        write("/" + fontName + " " + PDFNumber.doubleOut(fontSize) + " Tf\n");

        this.startText = this.useMultiByte ? "<" : "(";
        this.endText = this.useMultiByte ? ">" : ")";
    }

    /**
     * Updates the current font. This method only writes a "Tf" if the current
     * font changes.
     * 
     * @param fontName
     *            the name of the font to select
     * @param fontSize
     *            the font size (in points)
     * @param multiByte
     *            true indicates the font is a multi-byte font, false means
     *            single-byte
     */
    public void updateTf(final String fontName, final double fontSize,
            final boolean multiByte) {
        checkInTextObject();
        if (!fontName.equals(this.currentFontName)
                || fontSize != this.currentFontSize) {
            writeTJ();
            this.currentFontName = fontName;
            this.currentFontSize = fontSize;
            this.useMultiByte = multiByte;
            writeTf(fontName, fontSize);
        }
    }

    /**
     * Sets the text rendering mode.
     * 
     * @param mode
     *            the rendering mode (value 0 to 7, see PDF Spec, constants:
     *            TR_*)
     */
    public void setTextRenderingMode(final int mode) {
        if (mode < 0 || mode > 7) {
            throw new IllegalArgumentException(
                    "Illegal value for text rendering mode. Expected: 0-7");
        }
        if (mode != this.textRenderingMode) {
            writeTJ();
            this.textRenderingMode = mode;
            write(this.textRenderingMode + " Tr\n");
        }
    }

    /**
     * Sets the text rendering mode.
     * 
     * @param fill
     *            true if the text should be filled
     * @param stroke
     *            true if the text should be stroked
     * @param addToClip
     *            true if the path should be added for clipping
     */
    public void setTextRenderingMode(final boolean fill, final boolean stroke,
            final boolean addToClip) {
        int mode;
        if (fill) {
            mode = stroke ? 2 : 0;
        } else {
            mode = stroke ? 1 : 3;
        }
        if (addToClip) {
            mode += 4;
        }
        setTextRenderingMode(mode);
    }

    /**
     * Writes a "Tm" command, setting a new text transformation matrix.
     * 
     * @param localTransform
     *            the new text transformation matrix
     */
    public void writeTextMatrix(final AffineTransform localTransform) {
        final StringBuilder sb = new StringBuilder();
        writeAffineTransform(localTransform, sb);
        sb.append(" Tm ");
        write(sb.toString());
    }

    /**
     * Writes a char to the "TJ-Buffer".
     * 
     * @param codepoint
     *            the mapped character (code point/character code)
     */
    public void writeTJMappedChar(final char codepoint) {
        if (this.bufTJ == null) {
            this.bufTJ = new StringBuilder();
        }
        if (this.bufTJ.length() == 0) {
            this.bufTJ.append("[");
            this.bufTJ.append(this.startText);
        }
        writeChar(codepoint, this.bufTJ);
    }

    /**
     * Writes a glyph adjust value to the "TJ-Buffer".
     * 
     * <p>
     * Assumes the following:
     * </p>
     * <ol>
     * <li>if buffer is currently empty, then this is the start of the array
     * object that encodes the adjustment and character values, and, therfore, a
     * LEFT SQUARE BRACKET '[' must be prepended; and</li>
     * <li>otherwise (the buffer is not empty), then the last element written to
     * the buffer was a mapped character, and, therefore, a terminating '&gt;'
     * or ')' followed by a space must be appended to the buffer prior to
     * appending the adjustment value.</li>
     * </ol>
     * 
     * @param adjust
     *            the glyph adjust value in thousands of text unit space.
     */
    public void adjustGlyphTJ(final double adjust) {
        if (this.bufTJ == null) {
            this.bufTJ = new StringBuilder();
        }
        if (this.bufTJ.length() == 0) {
            this.bufTJ.append("[");
        } else {
            this.bufTJ.append(this.endText);
            this.bufTJ.append(" ");
        }
        this.bufTJ.append(PDFNumber.doubleOut(adjust, DEC - 4));
        this.bufTJ.append(" ");
        this.bufTJ.append(this.startText);
    }

    /**
     * Writes a "TJ" command, writing out the accumulated buffer with the
     * characters and glyph positioning values. The buffer is reset afterwards.
     */
    public void writeTJ() {
        if (isInString()) {
            this.bufTJ.append(this.endText).append("] TJ\n");
            write(this.bufTJ.toString());
            this.bufTJ.setLength(0);
        }
    }

    private boolean isInString() {
        return this.bufTJ != null && this.bufTJ.length() > 0;
    }

    /**
     * Writes a "Td" command with specified x and y coordinates.
     * 
     * @param x
     *            coordinate
     * @param y
     *            coordinate
     */
    public void writeTd(final double x, final double y) {
        final StringBuilder sb = new StringBuilder();
        sb.append(PDFNumber.doubleOut(x, DEC));
        sb.append(' ');
        sb.append(PDFNumber.doubleOut(y, DEC));
        sb.append(" Td\n");
        write(sb.toString());
    }

    /**
     * Writes a "Tj" command with specified character code.
     * 
     * @param ch
     *            character code to write
     */
    public void writeTj(final char ch) {
        final StringBuilder sb = new StringBuilder();
        sb.append('<');
        writeChar(ch, sb, true);
        sb.append('>');
        sb.append(" Tj\n");
        write(sb.toString());
    }

}
