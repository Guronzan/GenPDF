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

/* $Id: PDFGoTo.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.awt.geom.Point2D;

/**
 * class representing a /GoTo object. This can either have a Goto to a page
 * reference and location or to a specified PDF reference string.
 */
public class PDFGoTo extends PDFAction {

    /**
     * the pageReference
     */
    private String pageReference;
    private String destination = null;
    private float xPosition = 0;
    private float yPosition = 0;

    /**
     * create a /GoTo object.
     *
     * @param pageReference
     *            the pageReference represented by this object
     */
    public PDFGoTo(final String pageReference) {
        super();
        setPageReference(pageReference);
    }

    /**
     * create a /GoTo object.
     *
     * @param pageReference
     *            the PDF reference to the target page
     * @param position
     *            the target area's on-page coordinates in points
     */
    public PDFGoTo(final String pageReference, final Point2D position) {
        /* generic creation of object */
        this(pageReference);
        setPosition(position);
    }

    /**
     * Sets page reference after object has been created
     *
     * @param pageReference
     *            the new page reference to use
     */
    public void setPageReference(final String pageReference) {
        this.pageReference = pageReference;
    }

    /**
     * Sets the target (X,Y) position
     *
     * @param position
     *            the target's on-page coordinates in points
     */
    public void setPosition(final Point2D position) {
        this.xPosition = (float) position.getX();
        this.yPosition = (float) position.getY();
    }

    /**
     * Sets the x Position to jump to
     *
     * @param xPosition
     *            x position
     */
    public void setXPosition(final float xPosition) {
        this.xPosition = xPosition;
    }

    /**
     * Sets the Y position to jump to
     *
     * @param yPosition
     *            y position
     */
    public void setYPosition(final float yPosition) {
        this.yPosition = yPosition;
    }

    /**
     * Set the destination string for this Goto.
     *
     * @param dest
     *            the PDF destination string
     */
    public void setDestination(final String dest) {
        this.destination = dest;
    }

    /**
     * Get the PDF reference for the GoTo action.
     *
     * @return the PDF reference for the action
     */
    @Override
    public String getAction() {
        return referencePDF();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toPDFString() {
        String dest;
        if (this.destination == null) {
            dest = "/D [" + this.pageReference + " /XYZ " + this.xPosition
                    + " " + this.yPosition + " null]\n";
        } else {
            dest = "/D [" + this.pageReference + " " + this.destination + "]\n";
        }
        return "<< /Type /Action\n/S /GoTo\n" + dest + ">>";
    }

    /*
     * example 29 0 obj << /S /GoTo /D [23 0 R /FitH 600] >> endobj
     */

    /** {@inheritDoc} */
    @Override
    protected boolean contentEquals(final PDFObject obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFGoTo)) {
            return false;
        }

        final PDFGoTo gt = (PDFGoTo) obj;

        if (gt.pageReference == null) {
            if (this.pageReference != null) {
                return false;
            }
        } else {
            if (!gt.pageReference.equals(this.pageReference)) {
                return false;
            }
        }

        if (this.destination == null) {
            if (!(gt.destination == null && gt.xPosition == this.xPosition && gt.yPosition == this.yPosition)) {
                return false;
            }
        } else {
            if (!this.destination.equals(gt.destination)) {
                return false;
            }
        }

        return true;
    }
}
