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

/* $Id: PDFLink.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

// Java
import java.awt.geom.Rectangle2D;

/**
 * class representing an /Annot object of /Subtype /Link
 */
public class PDFLink extends PDFObject {
    /**
     * Used to represent an external link.
     */
    public static final int EXTERNAL = 0;

    /**
     * Used to represent an internal link.
     */
    public static final int INTERNAL = 1;

    private final float ulx;
    private final float uly;
    private final float brx;
    private final float bry;
    private final String color;
    private PDFAction action;
    private Integer structParent;

    /**
     * create objects associated with a link annotation (GoToR)
     *
     * @param r
     *            the rectangle of the link hotspot in absolute coordinates
     */
    public PDFLink(final Rectangle2D r) {
        /* generic creation of PDF object */
        super();

        this.ulx = (float) r.getX();
        this.uly = (float) r.getY();
        this.brx = (float) (r.getX() + r.getWidth());
        this.bry = (float) (r.getY() + r.getHeight());
        this.color = "0 0 0"; // just for now

    }

    /**
     * Set the pdf action for this link.
     * 
     * @param action
     *            the pdf action that is activated for this link
     */
    public void setAction(final PDFAction action) {
        this.action = action;
    }

    /**
     * Sets the value of the StructParent entry for this link.
     *
     * @param structParent
     *            key in the structure parent tree
     */
    public void setStructParent(final int structParent) {
        this.structParent = new Integer(structParent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toPDFString() {
        getDocumentSafely().getProfile().verifyAnnotAllowed();
        String fFlag = "";
        if (getDocumentSafely().getProfile().getPDFAMode().isPDFA1LevelB()) {
            int f = 0;
            f |= 1 << 3 - 1; // Print, bit 3
            f |= 1 << 4 - 1; // NoZoom, bit 4
            f |= 1 << 5 - 1; // NoRotate, bit 5
            fFlag = "/F " + f;
        }
        final String s = "<< /Type /Annot\n"
                + "/Subtype /Link\n"
                + "/Rect [ "
                + this.ulx
                + " "
                + this.uly
                + " "
                + this.brx
                + " "
                + this.bry
                + " ]\n"
                + "/C [ "
                + this.color
                + " ]\n"
                + "/Border [ 0 0 0 ]\n"
                + "/A "
                + this.action.getAction()
                + "\n"
                + "/H /I\n"
                + (this.structParent != null ? "/StructParent "
                        + this.structParent.toString() + "\n" : "") + fFlag
                + "\n>>";
        return s;
    }

    /*
     * example 19 0 obj << /Type /Annot /Subtype /Link /Rect [ 176.032 678.48412
     * 228.73579 692.356 ] /C [ 0.86491 0.03421 0.02591 ] /Border [ 0 0 1 ] /A
     * 28 0 R /H /I >> endobj
     */

    /** {@inheritDoc} */
    @Override
    protected boolean contentEquals(final PDFObject obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFLink)) {
            return false;
        }

        final PDFLink link = (PDFLink) obj;

        if (!(link.ulx == this.ulx && link.uly == this.uly
                && link.brx == this.brx && link.bry == this.bry)) {
            return false;
        }

        if (!(link.color.equals(this.color) && link.action.getAction().equals(
                this.action.getAction()))) {
            return false;
        }

        return true;
    }
}
