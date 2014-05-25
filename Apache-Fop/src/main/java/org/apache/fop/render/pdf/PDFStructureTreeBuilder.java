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

/* $Id$ */

package org.apache.fop.render.pdf;

import java.util.LinkedList;
import java.util.Locale;

import org.apache.fop.accessibility.StructureTreeElement;
import org.apache.fop.accessibility.StructureTreeEventHandler;
import org.apache.fop.events.EventBroadcaster;
import org.apache.fop.fo.extensions.ExtensionElementMapping;
import org.apache.fop.pdf.PDFFactory;
import org.apache.fop.pdf.PDFName;
import org.apache.fop.pdf.PDFObject;
import org.apache.fop.pdf.PDFParentTree;
import org.apache.fop.pdf.PDFStructElem;
import org.apache.fop.pdf.PDFStructTreeRoot;
import org.xml.sax.Attributes;

class PDFStructureTreeBuilder implements StructureTreeEventHandler {

    private PDFFactory pdfFactory;

    private PDFLogicalStructureHandler logicalStructureHandler;

    private EventBroadcaster eventBroadcaster;

    private LinkedList<PDFStructElem> ancestors = new LinkedList<PDFStructElem>();

    private PDFStructElem rootStructureElement;

    void setPdfFactory(final PDFFactory pdfFactory) {
        this.pdfFactory = pdfFactory;
    }

    void setLogicalStructureHandler(
            final PDFLogicalStructureHandler logicalStructureHandler) {
        this.logicalStructureHandler = logicalStructureHandler;
        createRootStructureElement();
    }

    private void createRootStructureElement() {
        assert this.rootStructureElement == null;
        final PDFParentTree parentTree = this.logicalStructureHandler
                .getParentTree();
        final PDFStructTreeRoot structTreeRoot = this.pdfFactory.getDocument()
                .makeStructTreeRoot(parentTree);
        this.rootStructureElement = createStructureElement("root",
                structTreeRoot, null);
        structTreeRoot.addKid(this.rootStructureElement);
    }

    void setEventBroadcaster(final EventBroadcaster eventBroadcaster) {
        this.eventBroadcaster = eventBroadcaster;
    }

    @Override
    public void startPageSequence(final Locale language, final String role) {
        this.ancestors = new LinkedList<PDFStructElem>();
        final PDFStructElem structElem = createStructureElement(
                "page-sequence", this.rootStructureElement, role);
        if (language != null) {
            structElem.setLanguage(language);
        }
        this.rootStructureElement.addKid(structElem);
        this.ancestors.add(structElem);
    }

    private PDFStructElem createStructureElement(final String name,
            final PDFObject parent, final String role) {
        final PDFName structureType = FOToPDFRoleMap.mapFormattingObject(name,
                role, parent, this.eventBroadcaster);
        return this.pdfFactory.getDocument().makeStructureElement(
                structureType, parent);
    }

    @Override
    public void endPageSequence() {
    }

    @Override
    public StructureTreeElement startNode(final String name,
            final Attributes attributes) {
        final PDFStructElem parent = this.ancestors.getFirst();
        final String role = attributes.getValue("role");
        final PDFStructElem structElem = createStructureElement(name, parent,
                role);
        parent.addKid(structElem);
        this.ancestors.addFirst(structElem);
        return structElem;
    }

    @Override
    public void endNode(final String name) {
        removeFirstAncestor();
    }

    private void removeFirstAncestor() {
        this.ancestors.removeFirst();
    }

    @Override
    public StructureTreeElement startImageNode(final String name,
            final Attributes attributes) {
        final PDFStructElem parent = this.ancestors.getFirst();
        final String role = attributes.getValue("role");
        final PDFStructElem structElem = createStructureElement(name, parent,
                role);
        parent.addKid(structElem);
        final String altTextNode = attributes.getValue(
                ExtensionElementMapping.URI, "alt-text");
        if (altTextNode != null) {
            structElem.put("Alt", altTextNode);
        } else {
            structElem.put("Alt", "No alternate text specified");
        }
        this.ancestors.addFirst(structElem);
        return structElem;
    }

    @Override
    public StructureTreeElement startReferencedNode(final String name,
            final Attributes attributes) {
        final PDFStructElem parent = this.ancestors.getFirst();
        final String role = attributes.getValue("role");
        PDFStructElem structElem;
        if ("#PCDATA".equals(name)) {
            structElem = new PDFStructElem.Placeholder(parent, name);
        } else {
            structElem = createStructureElement(name, parent, role);
        }
        parent.addKid(structElem);
        this.ancestors.addFirst(structElem);
        return structElem;
    }

}
