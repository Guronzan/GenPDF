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

/* $Id: XMLXMLHandler.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.render.xml;

import org.apache.fop.render.Renderer;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.XMLHandler;
import org.apache.fop.util.DOM2SAX;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * XML handler for the XML renderer.
 */
public class XMLXMLHandler implements XMLHandler {

    /** Key for getting the TransformerHandler from the RendererContext */
    public static final String HANDLER = "handler";

    /**
     * {@inheritDoc}
     *
     * @throws SAXException
     */
    @Override
    public void handleXML(final RendererContext context,
            final org.w3c.dom.Document doc, final String ns)
                    throws SAXException {
        final ContentHandler handler = (ContentHandler) context
                .getProperty(HANDLER);

        new DOM2SAX(handler).writeDocument(doc, true);
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsRenderer(final Renderer renderer) {
        return renderer instanceof XMLRenderer;
    }

    /** {@inheritDoc} */
    @Override
    public String getNamespace() {
        return null; // Handle all XML content
    }

}
