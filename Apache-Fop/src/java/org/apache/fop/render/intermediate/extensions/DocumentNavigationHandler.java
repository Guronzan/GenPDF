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

/* $Id: DocumentNavigationHandler.java 1242848 2012-02-10 16:51:08Z phancock $ */

package org.apache.fop.render.intermediate.extensions;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;
import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.accessibility.StructureTreeElement;
import org.apache.fop.fo.extensions.InternalElementMapping;
import org.apache.fop.render.intermediate.IFDocumentNavigationHandler;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.util.XMLUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * ContentHandler that handles the IF document navigation namespace.
 */
@Slf4j
public class DocumentNavigationHandler extends DefaultHandler implements
DocumentNavigationExtensionConstants {

    private final StringBuilder content = new StringBuilder();
    private final Stack objectStack = new Stack();

    private final IFDocumentNavigationHandler navHandler;

    private StructureTreeElement structureTreeElement;

    private final Map<String, StructureTreeElement> structureTreeElements;

    /**
     * Main constructor.
     *
     * @param navHandler
     *            the navigation handler that will receive the events
     * @param structureTreeElements
     *            the elements representing the structure of the document
     */
    public DocumentNavigationHandler(
            final IFDocumentNavigationHandler navHandler,
            final Map<String, StructureTreeElement> structureTreeElements) {
        this.navHandler = navHandler;
        assert structureTreeElements != null;
        this.structureTreeElements = structureTreeElements;
    }

    /** {@inheritDoc} */
    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes attributes)
            throws SAXException {
        boolean handled = false;
        if (NAMESPACE.equals(uri)) {
            if (BOOKMARK_TREE.getLocalName().equals(localName)) {
                if (!this.objectStack.isEmpty()) {
                    throw new SAXException(localName
                            + " must be the root element!");
                }
                final BookmarkTree bookmarkTree = new BookmarkTree();
                this.objectStack.push(bookmarkTree);
            } else if (BOOKMARK.getLocalName().equals(localName)) {
                final String title = attributes.getValue("title");
                final String s = attributes.getValue("starting-state");
                final boolean show = !"hide".equals(s);
                final Bookmark b = new Bookmark(title, show, null);
                Object o = this.objectStack.peek();
                if (o instanceof AbstractAction) {
                    final AbstractAction action = (AbstractAction) this.objectStack
                            .pop();
                    o = this.objectStack.peek();
                    ((Bookmark) o).setAction(action);
                }
                if (o instanceof BookmarkTree) {
                    ((BookmarkTree) o).addBookmark(b);
                } else {
                    ((Bookmark) o).addChildBookmark(b);
                }
                this.objectStack.push(b);
            } else if (NAMED_DESTINATION.getLocalName().equals(localName)) {
                if (!this.objectStack.isEmpty()) {
                    throw new SAXException(localName
                            + " must be the root element!");
                }
                final String name = attributes.getValue("name");
                final NamedDestination dest = new NamedDestination(name, null);
                this.objectStack.push(dest);
            } else if (LINK.getLocalName().equals(localName)) {
                if (!this.objectStack.isEmpty()) {
                    throw new SAXException(localName
                            + " must be the root element!");
                }
                final Rectangle targetRect = XMLUtil.getAttributeAsRectangle(
                        attributes, "rect");
                this.structureTreeElement = this.structureTreeElements
                        .get(attributes.getValue(InternalElementMapping.URI,
                                InternalElementMapping.STRUCT_REF));
                final Link link = new Link(null, targetRect);
                this.objectStack.push(link);
            } else if (GOTO_XY.getLocalName().equals(localName)) {
                final String idref = attributes.getValue("idref");
                GoToXYAction action;
                if (idref != null) {
                    action = new GoToXYAction(idref);
                } else {
                    final String id = attributes.getValue("id");
                    final int pageIndex = XMLUtil.getAttributeAsInt(attributes,
                            "page-index");
                    final Point location;
                    if (pageIndex < 0) {
                        location = null;
                    } else {
                        final int x = XMLUtil
                                .getAttributeAsInt(attributes, "x");
                        final int y = XMLUtil
                                .getAttributeAsInt(attributes, "y");
                        location = new Point(x, y);
                    }
                    action = new GoToXYAction(id, pageIndex, location);
                }
                if (this.structureTreeElement != null) {
                    action.setStructureTreeElement(this.structureTreeElement);
                }
                this.objectStack.push(action);
            } else if (GOTO_URI.getLocalName().equals(localName)) {
                final String id = attributes.getValue("id");
                final String gotoURI = attributes.getValue("uri");
                final String showDestination = attributes
                        .getValue("show-destination");
                final boolean newWindow = "new".equals(showDestination);
                final URIAction action = new URIAction(gotoURI, newWindow);
                if (id != null) {
                    action.setID(id);
                }
                if (this.structureTreeElement != null) {
                    action.setStructureTreeElement(this.structureTreeElement);
                }
                this.objectStack.push(action);
            } else {
                throw new SAXException("Invalid element '" + localName
                        + "' in namespace: " + uri);
            }
            handled = true;
        }
        if (!handled) {
            if (NAMESPACE.equals(uri)) {
                throw new SAXException("Unhandled element '" + localName
                        + "' in namespace: " + uri);
            } else {
                log.warn("Unhandled element '" + localName + "' in namespace: "
                        + uri);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endElement(final String uri, final String localName,
            final String qName) throws SAXException {
        if (NAMESPACE.equals(uri)) {
            try {
                if (BOOKMARK_TREE.getLocalName().equals(localName)) {
                    final BookmarkTree tree = (BookmarkTree) this.objectStack
                            .pop();
                    if (hasNavigation()) {
                        this.navHandler.renderBookmarkTree(tree);
                    }
                } else if (BOOKMARK.getLocalName().equals(localName)) {
                    if (this.objectStack.peek() instanceof AbstractAction) {
                        final AbstractAction action = (AbstractAction) this.objectStack
                                .pop();
                        final Bookmark b = (Bookmark) this.objectStack.pop();
                        b.setAction(action);
                    } else {
                        this.objectStack.pop();
                    }
                } else if (NAMED_DESTINATION.getLocalName().equals(localName)) {
                    final AbstractAction action = (AbstractAction) this.objectStack
                            .pop();
                    final NamedDestination dest = (NamedDestination) this.objectStack
                            .pop();
                    dest.setAction(action);
                    if (hasNavigation()) {
                        this.navHandler.renderNamedDestination(dest);
                    }
                } else if (LINK.getLocalName().equals(localName)) {
                    final AbstractAction action = (AbstractAction) this.objectStack
                            .pop();
                    final Link link = (Link) this.objectStack.pop();
                    link.setAction(action);
                    if (hasNavigation()) {
                        this.navHandler.renderLink(link);
                    }
                } else if (localName.startsWith("goto-")) {
                    if (this.objectStack.size() == 1) {
                        // Stand-alone action
                        final AbstractAction action = (AbstractAction) this.objectStack
                                .pop();
                        if (hasNavigation()) {
                            this.navHandler.addResolvedAction(action);
                        }
                    }
                }
            } catch (final IFException ife) {
                throw new SAXException(ife);
            }
        }
        this.content.setLength(0); // Reset text buffer (see characters())
    }

    private boolean hasNavigation() {
        return this.navHandler != null;
    }

    /** {@inheritDoc} */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        this.content.append(ch, start, length);
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {
        assert this.objectStack.isEmpty();
    }

}
