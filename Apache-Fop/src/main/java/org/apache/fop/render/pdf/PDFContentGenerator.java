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

/* $Id: PDFContentGenerator.java 1088231 2011-04-03 09:40:27Z adelmelle $ */

package org.apache.fop.render.pdf;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.pdf.PDFColorHandler;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFFilterList;
import org.apache.fop.pdf.PDFNumber;
import org.apache.fop.pdf.PDFPaintingState;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFStream;
import org.apache.fop.pdf.PDFTextUtil;
import org.apache.fop.pdf.PDFXObject;

/**
 * Generator class encapsulating all object references and state necessary to
 * generate a PDF content stream.
 */
public class PDFContentGenerator {

    /** Controls whether comments are written to the PDF stream. */
    protected static final boolean WRITE_COMMENTS = true;

    private final PDFDocument document;
    private final OutputStream outputStream;
    private final PDFResourceContext resourceContext;

    /** the current stream to add PDF commands to */
    private final PDFStream currentStream;

    private final PDFColorHandler colorHandler;

    /** drawing state */
    protected PDFPaintingState currentState = null;
    /** Text generation utility holding the current font status */
    protected PDFTextUtil textutil;

    private boolean inMarkedContentSequence;
    private boolean inArtifactMode;

    /**
     * Main constructor. Creates a new PDF stream and additional helper classes
     * for text painting and state management.
     * 
     * @param document
     *            the PDF document
     * @param out
     *            the output stream the PDF document is generated to
     * @param resourceContext
     *            the resource context
     */
    public PDFContentGenerator(final PDFDocument document,
            final OutputStream out, final PDFResourceContext resourceContext) {
        this.document = document;
        this.outputStream = out;
        this.resourceContext = resourceContext;
        this.currentStream = document.getFactory().makeStream(
                PDFFilterList.CONTENT_FILTER, false);
        this.textutil = new PDFTextUtil() {
            @Override
            protected void write(final String code) {
                PDFContentGenerator.this.currentStream.add(code);
            }
        };

        this.currentState = new PDFPaintingState();
        this.colorHandler = new PDFColorHandler(document.getResources());
    }

    /**
     * Returns the applicable resource context for the generator.
     * 
     * @return the resource context
     */
    public PDFDocument getDocument() {
        return this.document;
    }

    /**
     * Returns the output stream the PDF document is written to.
     * 
     * @return the output stream
     */
    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    /**
     * Returns the applicable resource context for the generator.
     * 
     * @return the resource context
     */
    public PDFResourceContext getResourceContext() {
        return this.resourceContext;
    }

    /**
     * Returns the {@link PDFStream} associated with this instance.
     * 
     * @return the PDF stream
     */
    public PDFStream getStream() {
        return this.currentStream;
    }

    /**
     * Returns the {@link PDFPaintingState} associated with this instance.
     * 
     * @return the PDF state
     */
    public PDFPaintingState getState() {
        return this.currentState;
    }

    /**
     * Returns the {@link PDFTextUtil} associated with this instance.
     * 
     * @return the text utility
     */
    public PDFTextUtil getTextUtil() {
        return this.textutil;
    }

    /**
     * Flushes all queued PDF objects ready to be written to the output stream.
     * 
     * @throws IOException
     *             if an error occurs while flushing the PDF objects
     */
    public void flushPDFDoc() throws IOException {
        this.document.output(this.outputStream);
    }

    /**
     * Writes out a comment.
     * 
     * @param text
     *            text for the comment
     */
    protected void comment(final String text) {
        if (WRITE_COMMENTS) {
            this.currentStream.add("% " + text + "\n");
        }
    }

    /** Save graphics state. */
    protected void saveGraphicsState() {
        endTextObject();
        this.currentState.save();
        this.currentStream.add("q\n");
    }

    /**
     * Save graphics state.
     * 
     * @param structElemType
     *            an element type
     * @param sequenceNum
     *            a sequence number
     */
    protected void saveGraphicsState(final String structElemType,
            final int sequenceNum) {
        endTextObject();
        this.currentState.save();
        beginMarkedContentSequence(structElemType, sequenceNum);
        this.currentStream.add("q\n");
    }

    /**
     * Begins a new marked content sequence (BDC or BMC). If the parameter
     * structElemType is null, the sequenceNum is ignored and instead of a BDC
     * with the MCID as parameter, an "Artifact" and a BMC command is generated.
     * 
     * @param structElemType
     *            Structure Element Type
     * @param mcid
     *            Sequence number
     */
    protected void beginMarkedContentSequence(final String structElemType,
            final int mcid) {
        assert !this.inMarkedContentSequence;
        assert !this.inArtifactMode;
        if (structElemType != null) {
            this.currentStream.add(structElemType + " <</MCID "
                    + String.valueOf(mcid) + ">>\n" + "BDC\n");
        } else {
            this.currentStream.add("/Artifact\nBMC\n");
            this.inArtifactMode = true;
        }
        this.inMarkedContentSequence = true;
    }

    void endMarkedContentSequence() {
        this.currentStream.add("EMC\n");
        this.inMarkedContentSequence = false;
        this.inArtifactMode = false;
    }

    /**
     * Restored the graphics state valid before the previous
     * {@link #saveGraphicsState()}.
     * 
     * @param popState
     *            true if the state should also be popped, false if only the PDF
     *            command should be issued
     */
    protected void restoreGraphicsState(final boolean popState) {
        endTextObject();
        this.currentStream.add("Q\n");
        if (popState) {
            this.currentState.restore();
        }
    }

    /**
     * Same as {@link #restoreGraphicsState(boolean)}, with <code>true</code> as
     * a parameter.
     */
    protected void restoreGraphicsState() {
        restoreGraphicsState(true);
    }

    /**
     * Same as {@link #restoreGraphicsState()}, additionally ending the current
     * marked content sequence if any.
     */
    protected void restoreGraphicsStateAccess() {
        endTextObject();
        this.currentStream.add("Q\n");
        if (this.inMarkedContentSequence) {
            endMarkedContentSequence();
        }
        this.currentState.restore();
    }

    /**
     * Separates 2 text elements, ending the current marked content sequence and
     * starting a new one.
     *
     * @param structElemType
     *            structure element type
     * @param mcid
     *            sequence number
     * @see #beginMarkedContentSequence(String, int)
     */
    protected void separateTextElements(final String structElemType,
            final int mcid) {
        this.textutil.endTextObject();
        endMarkedContentSequence();
        beginMarkedContentSequence(structElemType, mcid);
        this.textutil.beginTextObject();
    }

    /** Indicates the beginning of a text object. */
    protected void beginTextObject() {
        if (!this.textutil.isInTextObject()) {
            this.textutil.beginTextObject();
        }
    }

    /**
     * Indicates the beginning of a marked-content text object.
     *
     * @param structElemType
     *            structure element type
     * @param mcid
     *            sequence number
     * @see #beginTextObject()
     * @see #beginMarkedContentSequence(String, int)
     */
    protected void beginTextObject(final String structElemType, final int mcid) {
        if (!this.textutil.isInTextObject()) {
            beginMarkedContentSequence(structElemType, mcid);
            this.textutil.beginTextObject();
        }
    }

    /** Indicates the end of a text object. */
    protected void endTextObject() {
        if (this.textutil.isInTextObject()) {
            this.textutil.endTextObject();
            if (this.inMarkedContentSequence) {
                endMarkedContentSequence();
            }
        }
    }

    /**
     * Concatenates the given transformation matrix with the current one.
     * 
     * @param transform
     *            the transformation matrix (in points)
     */
    public void concatenate(final AffineTransform transform) {
        if (!transform.isIdentity()) {
            this.currentState.concatenate(transform);
            this.currentStream.add(CTMHelper.toPDFString(transform, false)
                    + " cm\n");
        }
    }

    /**
     * Intersects the current clip region with the given rectangle.
     * 
     * @param rect
     *            the clip rectangle
     */
    public void clipRect(final Rectangle rect) {
        final StringBuilder sb = new StringBuilder();
        sb.append(format(rect.x / 1000f)).append(' ');
        sb.append(format(rect.y / 1000f)).append(' ');
        sb.append(format(rect.width / 1000f)).append(' ');
        sb.append(format(rect.height / 1000f)).append(" re W n\n");
        add(sb.toString());
    }

    /**
     * Adds content to the stream.
     * 
     * @param content
     *            the PDF content
     */
    public void add(final String content) {
        this.currentStream.add(content);
    }

    /**
     * Formats a float value (normally coordinates in points) as Strings.
     * 
     * @param value
     *            the value
     * @return the formatted value
     */
    public static final String format(final float value) {
        return PDFNumber.doubleOut(value);
    }

    /**
     * Sets the current line width in points.
     * 
     * @param width
     *            line width in points
     */
    public void updateLineWidth(final float width) {
        if (this.currentState.setLineWidth(width)) {
            // Only write if value has changed WRT the current line width
            this.currentStream.add(format(width) + " w\n");
        }
    }

    /**
     * Sets the current character spacing (Tc) value.
     * 
     * @param value
     *            the Tc value (in unscaled text units)
     */
    public void updateCharacterSpacing(final float value) {
        if (getState().setCharacterSpacing(value)) {
            this.currentStream.add(format(value) + " Tc\n");
        }
    }

    /**
     * Establishes a new foreground or fill color.
     * 
     * @param col
     *            the color to apply
     * @param fill
     *            true to set the fill color, false for the foreground color
     * @param stream
     *            the PDFStream to write the PDF code to
     */
    public void setColor(final Color col, final boolean fill,
            final PDFStream stream) {
        assert stream != null;
        final StringBuilder sb = new StringBuilder();
        setColor(col, fill, sb);
        stream.add(sb.toString());
    }

    /**
     * Establishes a new foreground or fill color.
     * 
     * @param col
     *            the color to apply
     * @param fill
     *            true to set the fill color, false for the foreground color
     */
    public void setColor(final Color col, final boolean fill) {
        setColor(col, fill, getStream());
    }

    /**
     * Establishes a new foreground or fill color. In contrast to updateColor
     * this method does not check the PDFState for optimization possibilities.
     * 
     * @param col
     *            the color to apply
     * @param fill
     *            true to set the fill color, false for the foreground color
     * @param pdf
     *            StringBuilder to write the PDF code to, if null, the code is
     *            written to the current stream.
     */
    protected void setColor(final Color col, final boolean fill,
            final StringBuilder pdf) {
        if (pdf != null) {
            this.colorHandler.establishColor(pdf, col, fill);
        } else {
            setColor(col, fill, this.currentStream);
        }
    }

    /**
     * Establishes a new foreground or fill color.
     * 
     * @param col
     *            the color to apply (null skips this operation)
     * @param fill
     *            true to set the fill color, false for the foreground color
     * @param pdf
     *            StringBuilder to write the PDF code to, if null, the code is
     *            written to the current stream.
     */
    public void updateColor(final Color col, final boolean fill,
            final StringBuilder pdf) {
        if (col == null) {
            return;
        }
        boolean update = false;
        if (fill) {
            update = getState().setBackColor(col);
        } else {
            update = getState().setColor(col);
        }

        if (update) {
            setColor(col, fill, pdf);
        }
    }

    /**
     * Places a previously registered image at a certain place on the page.
     * 
     * @param x
     *            X coordinate
     * @param y
     *            Y coordinate
     * @param w
     *            width for image
     * @param h
     *            height for image
     * @param xobj
     *            the image XObject
     */
    public void placeImage(final float x, final float y, final float w,
            final float h, final PDFXObject xobj) {
        saveGraphicsState();
        add(format(w) + " 0 0 " + format(-h) + " " + format(x) + " "
                + format(y + h) + " cm\n" + xobj.getName() + " Do\n");
        restoreGraphicsState();
    }

    /**
     * Places a previously registered image at a certain place on the page,
     * bracketing it as a marked-content sequence.
     *
     * @param x
     *            X coordinate
     * @param y
     *            Y coordinate
     * @param w
     *            width for image
     * @param h
     *            height for image
     * @param xobj
     *            the image XObject
     * @param structElemType
     *            structure element type
     * @param mcid
     *            sequence number
     * @see #beginMarkedContentSequence(String, int)
     */
    public void placeImage(final float x, final float y, final float w,
            final float h, final PDFXObject xobj, final String structElemType,
            final int mcid) {
        saveGraphicsState(structElemType, mcid);
        add(format(w) + " 0 0 " + format(-h) + " " + format(x) + " "
                + format(y + h) + " cm\n" + xobj.getName() + " Do\n");
        restoreGraphicsStateAccess();
    }

}
