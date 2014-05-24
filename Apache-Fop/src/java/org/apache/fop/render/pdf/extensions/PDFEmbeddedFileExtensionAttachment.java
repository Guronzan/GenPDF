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

/* $Id: PDFEmbeddedFileExtensionAttachment.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.render.pdf.extensions;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This is the pass-through value object for the PDF extension.
 */
public class PDFEmbeddedFileExtensionAttachment extends PDFExtensionAttachment {

    /** element name */
    protected static final String ELEMENT = "embedded-file";

    /** name of file to be embedded */
    private static final String ATT_NAME = "filename";

    /** source of file to be embedded (URI) */
    private static final String ATT_SRC = "src";

    /** a description of the file to be embedded */
    private static final String ATT_DESC = "desc";

    /** filename attribute */
    private String filename = null;

    /** description attribute (optional) */
    private String desc = null;

    /** source name attribute */
    private String src = null;

    /**
     * No-argument contructor.
     */
    public PDFEmbeddedFileExtensionAttachment() {
        super();
    }

    /**
     * Default constructor.
     * 
     * @param filename
     *            the name of the file
     * @param src
     *            the location of the file
     * @param desc
     *            the description of the file
     */
    public PDFEmbeddedFileExtensionAttachment(final String filename,
            final String src, final String desc) {
        super();
        this.filename = filename;
        this.src = src;
        this.desc = desc;
    }

    /**
     * Returns the file name.
     * 
     * @return the file name
     */
    public String getFilename() {
        return this.filename;
    }

    /**
     * Sets the file name.
     * 
     * @param name
     *            The file name to set.
     */
    public void setFilename(final String name) {
        this.filename = name;
    }

    /**
     * Returns the file description.
     * 
     * @return the description
     */
    public String getDesc() {
        return this.desc;
    }

    /**
     * Sets the description of the file.
     * 
     * @param desc
     *            the description to set
     */
    public void setDesc(final String desc) {
        this.desc = desc;
    }

    /**
     * Returns the source URI of the file.
     * 
     * @return the source URI
     */
    public String getSrc() {
        return this.src;
    }

    /**
     * Sets the source URI of the file.
     * 
     * @param src
     *            the source URI
     */
    public void setSrc(final String src) {
        this.src = src;
    }

    /** {@inheritDoc} */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "PDFEmbeddedFile(name=" + getFilename() + ", " + getSrc() + ")";
    }

    /**
     * @return the element name
     */
    @Override
    protected String getElement() {
        return ELEMENT;
    }

    /** {@inheritDoc} */
    @Override
    public void toSAX(final ContentHandler handler) throws SAXException {
        final AttributesImpl atts = new AttributesImpl();
        if (this.filename != null && this.filename.length() > 0) {
            atts.addAttribute(null, ATT_NAME, ATT_NAME, "CDATA", this.filename);
        }
        if (this.src != null && this.src.length() > 0) {
            atts.addAttribute(null, ATT_SRC, ATT_SRC, "CDATA", this.src);
        }
        if (this.desc != null && this.desc.length() > 0) {
            atts.addAttribute(null, ATT_DESC, ATT_DESC, "CDATA", this.desc);
        }
        final String element = getElement();
        handler.startElement(CATEGORY, element, element, atts);
        handler.endElement(CATEGORY, element, element);
    }

}
