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

/* $Id: PDFEmbeddedFileElement.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.render.pdf.extensions;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.URISpecification;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Extension element for pdf:embedded-file.
 */
public class PDFEmbeddedFileElement extends AbstractPDFExtensionElement {

    /** name of element */
    protected static final String ELEMENT = "embedded-file";

    /**
     * Main constructor
     * 
     * @param parent
     *            parent FO node
     */
    protected PDFEmbeddedFileElement(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() throws FOPException {
        super.startOfNode();
        if (this.parent.getNameId() != Constants.FO_DECLARATIONS) {
            invalidChildError(getLocator(), this.parent.getName(),
                    getNamespaceURI(), getName(), "rule.childOfDeclarations");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void processNode(final String elementName, final Locator locator,
            final Attributes attlist, final PropertyList propertyList)
            throws FOPException {
        final PDFEmbeddedFileExtensionAttachment embeddedFile = (PDFEmbeddedFileExtensionAttachment) getExtensionAttachment();
        final String desc = attlist.getValue("description");
        if (desc != null && desc.length() > 0) {
            embeddedFile.setDesc(desc);
        }
        String src = attlist.getValue("src");
        src = URISpecification.getURL(src);
        if (src != null && src.length() > 0) {
            embeddedFile.setSrc(src);
        } else {
            missingPropertyError("src");
        }
        String filename = attlist.getValue("filename");
        if (filename == null || filename.length() == 0) {
            try {
                final URI uri = new URI(src);
                final String path = uri.getPath();
                final int idx = path.lastIndexOf('/');
                if (idx > 0) {
                    filename = path.substring(idx + 1);
                } else {
                    filename = path;
                }
                embeddedFile.setFilename(filename);
            } catch (final URISyntaxException e) {
                // Filename could not be deduced from URI
                missingPropertyError("name");
            }
        }
        embeddedFile.setFilename(filename);
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return ELEMENT;
    }

    /** {@inheritDoc} */
    @Override
    protected ExtensionAttachment instantiateExtensionAttachment() {
        return new PDFEmbeddedFileExtensionAttachment();
    }
}
