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

/* $Id: SVGElementMapping.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.fo.extensions.svg;

import java.util.HashMap;

import javax.xml.parsers.SAXParserFactory;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FONode;
import org.w3c.dom.DOMImplementation;

/**
 * Setup the SVG element mapping. This adds the svg element mappings used to
 * create the objects that create the SVG Document.
 */
@Slf4j
public class SVGElementMapping extends ElementMapping {

    /** the SVG namespace */
    public static final String URI = SVGDOMImplementation.SVG_NAMESPACE_URI;

    private boolean batikAvailable = true;

    /** Main constructor. */
    public SVGElementMapping() {
        this.namespaceURI = URI;
    }

    /** {@inheritDoc} */
    @Override
    public DOMImplementation getDOMImplementation() {
        return SVGDOMImplementation.getDOMImplementation();
    }

    /**
     * Returns the fully qualified classname of an XML parser for Batik classes
     * that apparently need it (error messages, perhaps)
     *
     * @return an XML parser classname
     */
    private String getAParserClassName() {
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            return factory.newSAXParser().getXMLReader().getClass().getName();
        } catch (final Exception e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void initialize() {
        if (this.foObjs == null && this.batikAvailable) {
            // this sets the parser that will be used
            // by default (SVGBrokenLinkProvider)
            // normally the user agent value is used
            try {
                XMLResourceDescriptor
                .setXMLParserClassName(getAParserClassName());

                this.foObjs = new HashMap<String, Maker>();
                this.foObjs.put("svg", new SE());
                this.foObjs.put(DEFAULT, new SVGMaker());
            } catch (final Throwable t) {
                log.error("Error while initializing the Batik SVG extensions",
                        t);
                // if the classes are not available
                // the DISPLAY is not checked
                this.batikAvailable = false;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getStandardPrefix() {
        return "svg";
    }

    static class SVGMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new SVGObj(parent);
        }
    }

    static class SE extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new SVGElement(parent);
        }
    }

}
