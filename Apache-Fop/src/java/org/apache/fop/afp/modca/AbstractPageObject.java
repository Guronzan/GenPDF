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

/* $Id: AbstractPageObject.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.fop.afp.AFPLineDataInfo;
import org.apache.fop.afp.Completable;
import org.apache.fop.afp.Factory;
import org.apache.fop.afp.fonts.AFPFont;
import org.apache.fop.afp.ptoca.PtocaProducer;

/**
 * Pages contain the data objects that comprise a presentation document. Each
 * page has a set of data objects associated with it. Each page within a
 * document is independent from any other page, and each must establish its own
 * environment parameters.
 * <p>
 * The page is the level in the document component hierarchy that is used for
 * printing or displaying a document's content. The data objects contained in
 * the page envelope in the data stream are presented when the page is
 * presented. Each data object has layout information associated with it that
 * directs the placement and orientation of the data on the page. In addition,
 * each page contains layout information that specifies the measurement units,
 * page width, and page depth.
 * <p>
 * A page is initiated by a begin page structured field and terminated by an end
 * page structured field. Structured fields that define objects and active
 * environment groups or that specify attributes of the page may be encountered
 * in page state.
 */
public abstract class AbstractPageObject extends AbstractNamedAFPObject
        implements Completable {

    /** The active environment group for the page */
    protected ActiveEnvironmentGroup activeEnvironmentGroup = null;

    /** The current presentation text object */
    private PresentationTextObject currentPresentationTextObject = null;

    /** The list of objects within this resource container */
    protected List/* <AbstractStructuredObject> */objects = new java.util.ArrayList();

    /** The page width */
    private int width;

    /** The page height */
    private int height;

    /** The page rotation */
    protected int rotation = 0;

    /** The page state */
    protected boolean complete = false;

    /** The width resolution */
    private int widthRes;

    /** The height resolution */
    private int heightRes;

    /** the object factory */
    protected final Factory factory;

    /**
     * Default constructor
     *
     * @param factory
     *            the object factory
     */
    public AbstractPageObject(final Factory factory) {
        this.factory = factory;
    }

    /**
     * Main constructor
     *
     * @param factory
     *            the object factory
     * @param name
     *            the name of this page object
     */
    public AbstractPageObject(final Factory factory, final String name) {
        super(name);
        this.factory = factory;
    }

    /**
     * Construct a new page object for the specified name argument, the page
     * name should be an 8 character identifier.
     *
     * @param factory
     *            the object factory.
     * @param name
     *            the name of the page.
     * @param width
     *            the width of the page.
     * @param height
     *            the height of the page.
     * @param rotation
     *            the rotation of the page.
     * @param widthRes
     *            the width resolution of the page.
     * @param heightRes
     *            the height resolution of the page.
     */
    public AbstractPageObject(final Factory factory, final String name,
            final int width, final int height, final int rotation,
            final int widthRes, final int heightRes) {
        super(name);

        this.factory = factory;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.widthRes = widthRes;
        this.heightRes = heightRes;
    }

    /**
     * Helper method to create a map coded font object on the current page, this
     * method delegates the construction of the map coded font object to the
     * active environment group on the page.
     *
     * @param fontReference
     *            the font number used as the resource identifier
     * @param font
     *            the font
     * @param size
     *            the point size of the font
     */
    public void createFont(final int fontReference, final AFPFont font,
            final int size) {
        getActiveEnvironmentGroup().createFont(fontReference, font, size, 0);
    }

    /**
     * Helper method to create a line on the current page, this method delegates
     * to the presentation text object in order to construct the line.
     *
     * @param lineDataInfo
     *            the line data information.
     */
    public void createLine(final AFPLineDataInfo lineDataInfo) {
        getPresentationTextObject().createLineData(lineDataInfo);
    }

    /**
     * Helper method to create text on the current page, this method delegates
     * to the presentation text object in order to construct the text.
     *
     * @param producer
     *            the producer
     * @throws UnsupportedEncodingException
     *             thrown if character encoding is not supported
     */
    public void createText(final PtocaProducer producer)
            throws UnsupportedEncodingException {
        // getPresentationTextObject().createTextData(textDataInfo);
        getPresentationTextObject().createControlSequences(producer);

    }

    /**
     * Helper method to mark the end of the page. This should end the control
     * sequence on the current presentation text object.
     */
    public void endPage() {
        if (this.currentPresentationTextObject != null) {
            this.currentPresentationTextObject.endControlSequence();
        }
        setComplete(true);
    }

    /**
     * Ends the presentation text object
     */
    protected void endPresentationObject() {
        if (this.currentPresentationTextObject != null) {
            this.currentPresentationTextObject.endControlSequence();
            this.currentPresentationTextObject = null;
        }
    }

    /**
     * Helper method to create a presentation text object on the current page
     * and to return the object.
     *
     * @return the presentation text object
     */
    public PresentationTextObject getPresentationTextObject() {
        if (this.currentPresentationTextObject == null) {
            final PresentationTextObject presentationTextObject = this.factory
                    .createPresentationTextObject();
            addObject(presentationTextObject);
            this.currentPresentationTextObject = presentationTextObject;
        }
        return this.currentPresentationTextObject;
    }

    /**
     * Returns the list of {@link TagLogicalElement}s.
     * 
     * @return the TLEs
     */
    protected List getTagLogicalElements() {
        if (this.objects == null) {
            this.objects = new java.util.ArrayList/* <AbstractStructuredObject> */();
        }
        return this.objects;
    }

    /**
     * Creates a TagLogicalElement on the page.
     *
     * @param name
     *            the name of the tag
     * @param value
     *            the value of the tag
     * @param tleID
     *            unique ID within AFP stream
     */
    public void createTagLogicalElement(final String name, final String value,
            final int tleID) {
        final TagLogicalElement tle = new TagLogicalElement(name, value, tleID);
        final List list = getTagLogicalElements();
        list.add(tle);
    }

    /**
     * Creates a NoOperation on the page.
     *
     * @param content
     *            the byte data
     */
    public void createNoOperation(final String content) {
        addObject(new NoOperation(content));
    }

    /**
     * Creates an IncludePageSegment on the current page.
     *
     * @param name
     *            the name of the page segment
     * @param x
     *            the x coordinate of the page segment.
     * @param y
     *            the y coordinate of the page segment.
     * @param hard
     *            true if hard page segment possible
     */
    public void createIncludePageSegment(final String name, final int x,
            final int y, final boolean hard) {
        final IncludePageSegment ips = this.factory.createIncludePageSegment(
                name, x, y);
        addObject(ips);
        if (hard) {
            // For performance reasons, page segments can be turned into hard
            // page segments
            // using the Map Page Segment (MPS) structured field.
            getActiveEnvironmentGroup().addMapPageSegment(name);
        }
    }

    /**
     * Returns the ActiveEnvironmentGroup associated with this page.
     *
     * @return the ActiveEnvironmentGroup object
     */
    public ActiveEnvironmentGroup getActiveEnvironmentGroup() {
        if (this.activeEnvironmentGroup == null) {
            // every page object must have an ActiveEnvironmentGroup
            this.activeEnvironmentGroup = this.factory
                    .createActiveEnvironmentGroup(this.width, this.height,
                            this.widthRes, this.heightRes);

            if (this.rotation != 0) {
                switch (this.rotation) {
                case 90:
                    this.activeEnvironmentGroup.setObjectAreaPosition(
                            this.width, 0, this.rotation);
                    break;
                case 180:
                    this.activeEnvironmentGroup.setObjectAreaPosition(
                            this.width, this.height, this.rotation);
                    break;
                case 270:
                    this.activeEnvironmentGroup.setObjectAreaPosition(0,
                            this.height, this.rotation);
                    break;
                default:
                }
            }
        }
        return this.activeEnvironmentGroup;
    }

    /**
     * Returns the height of the page
     *
     * @return the height of the page
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Returns the width of the page
     *
     * @return the width of the page
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Returns the rotation of the page
     *
     * @return the rotation of the page
     */
    public int getRotation() {
        return this.rotation;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final OutputStream os) throws IOException {
        super.writeContent(os);
        writeObjects(this.objects, os);
    }

    /**
     * Adds an AFP object reference to this page
     *
     * @param obj
     *            an AFP object
     */
    public void addObject(final Object obj) {
        this.objects.add(obj);
    }

    /** {@inheritDoc} */
    @Override
    public void setComplete(final boolean complete) {
        this.complete = complete;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isComplete() {
        return this.complete;
    }
}
