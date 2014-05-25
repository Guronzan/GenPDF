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

/* $Id: BookmarkTree.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.pagination.bookmarks;

// Java
import java.util.ArrayList;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.pagination.Root;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_bookmark-tree">
 * <code>fo:bookmark-tree</code></a> object, first introduced in the XSL 1.1 WD.
 */
public class BookmarkTree extends FObj {
    private final ArrayList bookmarks = new ArrayList();

    /**
     * Create a new BookmarkTree object that is a child of the given
     * {@link FONode}.
     *
     * @param parent
     *            the {@link FONode} parent
     */
    public BookmarkTree(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    protected void addChildNode(final FONode obj) {
        if (obj instanceof Bookmark) {
            this.bookmarks.add(obj);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (this.bookmarks == null) {
            missingChildElementError("(fo:bookmark+)");
        }
        ((Root) this.parent).setBookmarkTree(this);
    }

    /**
     * {@inheritDoc} <br>
     * XSL/FOP: (bookmark+)
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (!localName.equals("bookmark")) {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /**
     * Get the descendant {@link Bookmark}s.
     * 
     * @return an <code>ArrayList</code> containing the {@link Bookmark}
     *         objects.
     */
    public ArrayList getBookmarks() {
        return this.bookmarks;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "bookmark-tree";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_BOOKMARK_TREE}
     */
    @Override
    public int getNameId() {
        return FO_BOOKMARK_TREE;
    }
}
