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

/* $Id: Title.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.pagination;

// XML
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.flow.InlineLevel;
import org.xml.sax.Locator;

/**
 * Class modeling the <a href="http://www.w3.org/TR/xsl/#fo_title">
 * <code>fo:title</code></a> object.
 */
public class Title extends InlineLevel {
    // The value of properties relevant for fo:title.
    // See superclass InlineLevel
    // End of property values

    /**
     * @param parent
     *            FONode that is the parent of this object
     */
    public Title(final FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc} String, String) <br>
     * XSL/FOP: (#PCDATA|%inline;)*
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (!isInlineItem(nsURI, localName)) {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "title";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_TITLE}
     */
    @Override
    public int getNameId() {
        return FO_TITLE;
    }
}
