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

/* $Id: AbstractSVGDocumentHandler.java 830257 2009-10-27 17:37:14Z vhennebert $ */

package org.apache.fop.render.svg;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fonts.FontInfo;
import org.apache.fop.render.intermediate.AbstractXMLWritingIFDocumentHandler;
import org.apache.fop.render.intermediate.IFDocumentHandlerConfigurator;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFState;
import org.apache.fop.render.java2d.Java2DUtil;
import org.apache.xmlgraphics.xmp.Metadata;
import org.xml.sax.SAXException;

/**
 * Abstract base class for SVG Painter implementations.
 */
@Slf4j
public abstract class AbstractSVGDocumentHandler extends
AbstractXMLWritingIFDocumentHandler implements SVGConstants {

    /** Font configuration */
    protected FontInfo fontInfo;

    /** Holds the intermediate format state */
    protected IFState state;

    private static final int MODE_NORMAL = 0;
    private static final int MODE_TEXT = 1;

    private final int mode = MODE_NORMAL;

    /** {@inheritDoc} */
    @Override
    protected String getMainNamespace() {
        return NAMESPACE;
    }

    /** {@inheritDoc} */
    @Override
    public FontInfo getFontInfo() {
        return this.fontInfo;
    }

    /** {@inheritDoc} */
    @Override
    public void setFontInfo(final FontInfo fontInfo) {
        this.fontInfo = fontInfo;
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultFontInfo(final FontInfo fontInfo) {
        final FontInfo fi = Java2DUtil.buildDefaultJava2DBasedFontInfo(
                fontInfo, getUserAgent());
        setFontInfo(fi);
    }

    /** {@inheritDoc} */
    @Override
    public IFDocumentHandlerConfigurator getConfigurator() {
        return null; // No configurator, yet.
    }

    /** {@inheritDoc} */
    @Override
    public void startDocumentHeader() throws IFException {
        try {
            this.handler.startElement("defs");
        } catch (final SAXException e) {
            throw new IFException("SAX error in startDocumentHeader()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endDocumentHeader() throws IFException {
        try {
            this.handler.endElement("defs");
        } catch (final SAXException e) {
            throw new IFException("SAX error in startDocumentHeader()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleExtensionObject(final Object extension)
            throws IFException {
        if (extension instanceof Metadata) {
            final Metadata meta = (Metadata) extension;
            try {
                this.handler.startElement("metadata");
                meta.toSAX(this.handler);
                this.handler.endElement("metadata");
            } catch (final SAXException e) {
                throw new IFException(
                        "SAX error while handling extension object", e);
            }
        } else {
            log.debug("Don't know how to handle extension object. Ignoring: "
                    + extension + " (" + extension.getClass().getName() + ")");
        }
    }
}
