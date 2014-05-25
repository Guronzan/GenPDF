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

/* $Id: URIProperty.java 1304264 2012-03-23 10:26:13Z vhennebert $ */

package org.apache.fop.fo.properties;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.fop.datatypes.URISpecification;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.util.CompareUtil;

import static org.apache.fop.fo.Constants.PR_X_XML_BASE;

/**
 * Class modeling a property that has a value of type &lt;uri-specification>.
 * The purpose is mainly to support resolution of a specified relative URI
 * against a specified or inherited <code>xml:base</code> during the property
 * refinement stage. If no <code>xml:base</code> has been specified, only the
 * original URI, as it appears in the source document, is stored as the
 * property's specified value.
 */
public class URIProperty extends Property {

    /** will be null if the URI is not resolved against an xml:base */
    private URI resolvedURI;

    /**
     * Default constructor, to create a {@link URIProperty} from a
     * {@code java.net.URI} directly.
     * 
     * @param uri
     *            a resolved {@code java.net.URI}
     */
    protected URIProperty(final URI uri) {
        this.resolvedURI = uri;
    }

    /**
     * Alternate constructor, to create a {@link URIProperty} from a string
     * representation.
     * 
     * @param uri
     *            a {@code java.lang.String} representing the URI
     * @param resolve
     *            flag indicating whether this URI was the result of resolution
     * @throws IllegalArgumentException
     *             if the URI should be resolved, but is not valid.
     */
    private URIProperty(final String uri, final boolean resolve) {
        if (resolve && !(uri == null || "".equals(uri))) {
            this.resolvedURI = URI.create(uri);
        } else {
            setSpecifiedValue(uri);
        }
    }

    /**
     * Return a string representing the resolved URI, or the specified value if
     * the URI is not resolved against an <code>xml:base</code>
     * 
     * @return a string representing the URI
     */
    @Override
    public String getString() {
        if (this.resolvedURI == null) {
            return getSpecifiedValue();
        } else {
            return this.resolvedURI.toString();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getString();
    }

    /**
     * Inner {@link PropertyMaker} subclass responsible for making instances of
     * this type.
     */
    public static class Maker extends PropertyMaker {

        /**
         * Create a maker for the given property id
         *
         * @param propId
         *            the id of the property for which a Maker should be created
         */
        public Maker(final int propId) {
            super(propId);
        }

        /**
         * {@inheritDoc} Check if {@code xml:base} has been specified and
         * whether the given {@code value} represents a relative URI. If so,
         * create a property representing the resolved URI.
         */
        @Override
        public Property make(final PropertyList propertyList,
                final String value, final FObj fo) throws PropertyException {

            Property p = null;
            // special treament for data: URIs
            if (value.matches("(?s)^(url\\(('|\")?)?data:.*$")) {
                p = new URIProperty(value, false);
            } else {
                try {
                    final URI specifiedURI = new URI(
                            URISpecification.escapeURI(value));
                    final URIProperty xmlBase = (URIProperty) propertyList.get(
                            PR_X_XML_BASE, true, false);
                    if (xmlBase == null) {
                        // xml:base undefined
                        if (this.propId == PR_X_XML_BASE) {
                            // if current property is xml:base, define a new one
                            p = new URIProperty(specifiedURI);
                            p.setSpecifiedValue(value);
                        } else {
                            // otherwise, just store the specified value (for
                            // backward compatibility)
                            p = new URIProperty(value, false);
                        }
                    } else {
                        // xml:base defined, so resolve
                        p = new URIProperty(
                                xmlBase.resolvedURI.resolve(specifiedURI));
                        p.setSpecifiedValue(value);
                    }
                } catch (final URISyntaxException use) {
                    // Let PropertyList propagate the exception
                    throw new PropertyException("Invalid URI specified");
                }
            }
            return p;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + CompareUtil.getHashCode(getSpecifiedValue());
        result = prime * result + CompareUtil.getHashCode(this.resolvedURI);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof URIProperty)) {
            return false;
        }
        final URIProperty other = (URIProperty) obj;
        return CompareUtil
                .equal(getSpecifiedValue(), other.getSpecifiedValue())
                && CompareUtil.equal(this.resolvedURI, other.resolvedURI);
    }

}
