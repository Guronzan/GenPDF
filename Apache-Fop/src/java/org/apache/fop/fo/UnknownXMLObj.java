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

/* $Id: UnknownXMLObj.java 680378 2008-07-28 15:05:14Z jeremias $ */

package org.apache.fop.fo;

import org.apache.fop.apps.FOPException;
import org.xml.sax.Locator;

/**
 * Class for handling generic XML from a namespace not recognized by FOP
 */
public class UnknownXMLObj extends XMLObj {
    private final String namespace;

    /**
     * Inner class for an UnknownXMLObj Maker
     */
    public static class Maker extends ElementMapping.Maker {
        private final String space;

        /**
         * Construct the Maker
         * 
         * @param ns
         *            the namespace for this Maker
         */
        public Maker(final String ns) {
            this.space = ns;
        }

        /**
         * Make an instance
         * 
         * @param parent
         *            FONode that is the parent of the object
         * @return the created UnknownXMLObj
         */
        @Override
        public FONode make(final FONode parent) {
            return new UnknownXMLObj(parent, this.space);
        }
    }

    /**
     * Constructs an unknown xml object (called by Maker).
     *
     * @param parent
     *            the parent formatting object
     * @param space
     *            the namespace for this object
     */
    protected UnknownXMLObj(final FONode parent, final String space) {
        super(parent);
        this.namespace = space;
    }

    /** {@inheritDoc} */
    @Override
    public String getNamespaceURI() {
        return this.namespace;
    }

    /** {@inheritDoc} */
    @Override
    public String getNormalNamespacePrefix() {
        return null; // We don't know that in this case.
    }

    /** {@inheritDoc} */
    @Override
    protected void addChildNode(final FONode child) {
        if (this.doc == null) {
            createBasicDocument();
        }
        super.addChildNode(child);
    }

    /** {@inheritDoc} */
    @Override
    protected void characters(final char[] data, final int start,
            final int length, final PropertyList pList, final Locator locator)
            throws FOPException {
        if (this.doc == null) {
            createBasicDocument();
        }
        super.characters(data, start, length, pList, locator);
    }

}
