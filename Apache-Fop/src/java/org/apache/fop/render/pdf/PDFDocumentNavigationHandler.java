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

/* $Id: PDFDocumentNavigationHandler.java 1310666 2012-04-07 04:04:14Z gadams $ */

package org.apache.fop.render.pdf;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Map;

import org.apache.fop.pdf.PDFAction;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFFactory;
import org.apache.fop.pdf.PDFGoTo;
import org.apache.fop.pdf.PDFLink;
import org.apache.fop.pdf.PDFOutline;
import org.apache.fop.pdf.PDFStructElem;
import org.apache.fop.render.intermediate.IFDocumentNavigationHandler;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.extensions.AbstractAction;
import org.apache.fop.render.intermediate.extensions.Bookmark;
import org.apache.fop.render.intermediate.extensions.BookmarkTree;
import org.apache.fop.render.intermediate.extensions.GoToXYAction;
import org.apache.fop.render.intermediate.extensions.Link;
import org.apache.fop.render.intermediate.extensions.NamedDestination;
import org.apache.fop.render.intermediate.extensions.URIAction;
import org.apache.fop.render.pdf.PDFDocumentHandler.PageReference;

/**
 * Implementation of the {@link IFDocumentNavigationHandler} interface for PDF
 * output.
 */
public class PDFDocumentNavigationHandler implements
        IFDocumentNavigationHandler {

    private final PDFDocumentHandler documentHandler;

    private final Map incompleteActions = new java.util.HashMap();
    private final Map completeActions = new java.util.HashMap();

    /**
     * Default constructor.
     * 
     * @param documentHandler
     *            the parent document handler
     */
    public PDFDocumentNavigationHandler(final PDFDocumentHandler documentHandler) {
        super();
        this.documentHandler = documentHandler;
    }

    PDFDocument getPDFDoc() {
        return this.documentHandler.pdfDoc;
    }

    /** {@inheritDoc} */
    @Override
    public void renderNamedDestination(final NamedDestination destination)
            throws IFException {
        final PDFAction action = getAction(destination.getAction());
        getPDFDoc().getFactory().makeDestination(destination.getName(),
                action.makeReference());
    }

    /** {@inheritDoc} */
    @Override
    public void renderBookmarkTree(final BookmarkTree tree) throws IFException {
        final Iterator iter = tree.getBookmarks().iterator();
        while (iter.hasNext()) {
            final Bookmark b = (Bookmark) iter.next();
            renderBookmark(b, null);
        }
    }

    private void renderBookmark(final Bookmark bookmark, PDFOutline parent)
            throws IFException {
        if (parent == null) {
            parent = getPDFDoc().getOutlineRoot();
        }
        final PDFAction action = getAction(bookmark.getAction());
        final String actionRef = action != null ? action.makeReference()
                .toString() : null;
        final PDFOutline pdfOutline = getPDFDoc().getFactory().makeOutline(
                parent, bookmark.getTitle(), actionRef, bookmark.isShown());
        final Iterator iter = bookmark.getChildBookmarks().iterator();
        while (iter.hasNext()) {
            final Bookmark b = (Bookmark) iter.next();
            renderBookmark(b, pdfOutline);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renderLink(final Link link) throws IFException {
        final Rectangle targetRect = link.getTargetRect();
        final int pageHeight = this.documentHandler.currentPageRef
                .getPageDimension().height;
        final Rectangle2D targetRect2D = new Rectangle2D.Double(
                targetRect.getMinX() / 1000.0,
                (pageHeight - targetRect.getMinY() - targetRect.getHeight()) / 1000.0,
                targetRect.getWidth() / 1000.0, targetRect.getHeight() / 1000.0);
        final PDFAction pdfAction = getAction(link.getAction());
        // makeLink() currently needs a PDFAction and not a reference
        // TODO Revisit when PDFLink is converted to a PDFDictionary
        final PDFLink pdfLink = getPDFDoc().getFactory().makeLink(targetRect2D,
                pdfAction);
        if (pdfLink != null) {
            final PDFStructElem structure = (PDFStructElem) link.getAction()
                    .getStructureTreeElement();
            if (this.documentHandler.getUserAgent().isAccessibilityEnabled()
                    && structure != null) {
                this.documentHandler.getLogicalStructureHandler()
                        .addLinkContentItem(pdfLink, structure);
            }
            this.documentHandler.currentPage.addAnnotation(pdfLink);
        }
    }

    /**
     * Commits all pending elements to the PDF document.
     */
    public void commit() {
    }

    /** {@inheritDoc} */
    @Override
    public void addResolvedAction(final AbstractAction action)
            throws IFException {
        assert action.isComplete();
        final PDFAction pdfAction = (PDFAction) this.incompleteActions
                .remove(action.getID());
        if (pdfAction == null) {
            getAction(action);
        } else if (pdfAction instanceof PDFGoTo) {
            final PDFGoTo pdfGoTo = (PDFGoTo) pdfAction;
            updateTargetLocation(pdfGoTo, (GoToXYAction) action);
        } else {
            throw new UnsupportedOperationException(
                    "Action type not supported: "
                            + pdfAction.getClass().getName());
        }
    }

    private PDFAction getAction(final AbstractAction action) throws IFException {
        if (action == null) {
            return null;
        }
        PDFAction pdfAction = (PDFAction) this.completeActions.get(action
                .getID());
        if (pdfAction != null) {
            return pdfAction;
        } else if (action instanceof GoToXYAction) {
            pdfAction = (PDFAction) this.incompleteActions.get(action.getID());
            if (pdfAction != null) {
                return pdfAction;
            } else {
                final GoToXYAction a = (GoToXYAction) action;
                final PDFGoTo pdfGoTo = new PDFGoTo(null);
                getPDFDoc().assignObjectNumber(pdfGoTo);
                if (action.isComplete()) {
                    updateTargetLocation(pdfGoTo, a);
                } else {
                    this.incompleteActions.put(action.getID(), pdfGoTo);
                }
                return pdfGoTo;
            }
        } else if (action instanceof URIAction) {
            final URIAction u = (URIAction) action;
            assert u.isComplete();
            final String uri = u.getURI();
            final PDFFactory factory = getPDFDoc().getFactory();
            pdfAction = factory.getExternalAction(uri, u.isNewWindow());
            if (!pdfAction.hasObjectNumber()) {
                // Some PDF actions are pooled
                getPDFDoc().registerObject(pdfAction);
            }
            this.completeActions.put(action.getID(), pdfAction);
            return pdfAction;
        } else {
            throw new UnsupportedOperationException("Unsupported action type: "
                    + action + " (" + action.getClass().getName() + ")");
        }
    }

    private void updateTargetLocation(final PDFGoTo pdfGoTo,
            final GoToXYAction action) throws IFException {
        final PageReference pageRef = this.documentHandler
                .getPageReference(action.getPageIndex());
        if (pageRef == null) {
            throw new IFException("Can't resolve page reference @ index: "
                    + action.getPageIndex(), null);
        } else {
            // Convert target location from millipoints to points and adjust for
            // different
            // page origin
            Point2D p2d = null;
            p2d = new Point2D.Double(action.getTargetLocation().x / 1000.0,
                    (pageRef.getPageDimension().height - action
                            .getTargetLocation().y) / 1000.0);
            final String pdfPageRef = pageRef.getPageRef();
            pdfGoTo.setPageReference(pdfPageRef);
            pdfGoTo.setPosition(p2d);

            // Queue this object now that it's complete
            getPDFDoc().addObject(pdfGoTo);
            this.completeActions.put(action.getID(), pdfGoTo);
        }
    }

}
