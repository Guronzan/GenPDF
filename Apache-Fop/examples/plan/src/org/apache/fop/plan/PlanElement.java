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

/* $Id: PlanElement.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.plan;

import java.awt.geom.Point2D;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * This class defines the plan element.
 */
@Slf4j
public class PlanElement extends PlanObj {

    private Document svgDoc = null;
    private float width;
    private float height;
    private boolean converted;

    /**
     * @see org.apache.fop.fo.FONode#FONode(FONode)
     */
    public PlanElement(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList propertyList)
                    throws FOPException {
        super.processNode(elementName, locator, attlist, propertyList);
        createBasicDocument();
    }

    /**
     * Converts the element to SVG.
     */
    public void convertToSVG() {
        try {
            if (!this.converted) {
                this.converted = true;
                final PlanRenderer pr = new PlanRenderer();
                pr.setFontInfo("Helvetica", 12);
                this.svgDoc = pr.createSVGDocument(this.doc);
                this.width = pr.getWidth();
                this.height = pr.getHeight();

                this.doc = this.svgDoc;
            }
        } catch (final Throwable t) {
            log.error("Could not convert Plan to SVG", t);
            this.width = 0;
            this.height = 0;
        }

    }

    /** {@inheritDoc} */
    @Override
    public Document getDOMDocument() {
        convertToSVG();
        return this.doc;
    }

    /** {@inheritDoc} */
    @Override
    public String getNamespaceURI() {
        if (this.svgDoc == null) {
            return PlanElementMapping.NAMESPACE;
        }
        return "http://www.w3.org/2000/svg";
    }

    /** {@inheritDoc} */
    @Override
    public Point2D getDimension(final Point2D view) {
        convertToSVG();
        return new Point2D.Float(this.width, this.height);
    }
}
