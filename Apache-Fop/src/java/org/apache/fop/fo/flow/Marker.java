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

/* $Id: Marker.java 1324916 2012-04-11 18:52:19Z gadams $ */

package org.apache.fop.fo.flow;

import java.util.Map;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOTreeBuilderContext;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.FObjMixed;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.PropertyListMaker;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.fo.properties.PropertyCache;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_marker">
 * <code>fo:marker<code></a> object.
 */
public class Marker extends FObjMixed {
    // The value of properties relevant for fo:marker.
    private String markerClassName;
    // End of property values

    private PropertyListMaker savePropertyListMaker;
    private final Map descendantPropertyLists = new java.util.HashMap();

    /**
     * Create a marker fo.
     *
     * @param parent
     *            the parent {@link FONode}
     */
    public Marker(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) throws FOPException {
        if (findAncestor(FO_FLOW) < 0) {
            invalidChildError(this.locator, getParent().getName(), FO_URI,
                    getLocalName(), "rule.markerDescendantOfFlow");
        }

        this.markerClassName = pList.get(PR_MARKER_CLASS_NAME).getString();

        if (this.markerClassName == null || this.markerClassName.equals("")) {
            missingPropertyError("marker-class-name");
        }
    }

    /**
     * Retrieve the property list of the given {@link FONode} descendant
     *
     * @param foNode
     *            the {@link FONode} whose property list is requested
     * @return the {@link MarkerPropertyList} for the given node
     */
    protected MarkerPropertyList getPropertyListFor(final FONode foNode) {
        return (MarkerPropertyList) this.descendantPropertyLists.get(foNode);
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() {
        final FOTreeBuilderContext builderContext = getBuilderContext();
        // Push a new property list maker which will make MarkerPropertyLists.
        this.savePropertyListMaker = builderContext.getPropertyListMaker();
        builderContext.setPropertyListMaker(new PropertyListMaker() {
            @Override
            public PropertyList make(final FObj fobj,
                    final PropertyList parentPropertyList) {
                final PropertyList pList = new MarkerPropertyList(fobj,
                        parentPropertyList);
                Marker.this.descendantPropertyLists.put(fobj, pList);
                return pList;
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        super.endOfNode();
        // Pop the MarkerPropertyList maker.
        getBuilderContext().setPropertyListMaker(this.savePropertyListMaker);
        this.savePropertyListMaker = null;
    }

    /**
     * {@inheritDoc} <br>
     * XSL Content Model: (#PCDATA|%inline;|%block;)* <br>
     * <i>Additionally: "An fo:marker may contain any formatting objects that
     * are permitted as a replacement of any fo:retrieve-marker that retrieves
     * the fo:marker's children."</i> TODO implement "additional" constraint,
     * possibly within fo:retrieve-marker
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (!isBlockOrInlineItem(nsURI, localName)) {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean inMarker() {
        return true;
    }

    /** @return the "marker-class-name" property */
    public String getMarkerClassName() {
        return this.markerClassName;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "marker";
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@link org.apache.fop.fo.Constants#FO_MARKER}
     */
    @Override
    public int getNameId() {
        return FO_MARKER;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" {").append(getMarkerClassName()).append("}");
        return sb.toString();
    }

    /**
     * An implementation of {@link PropertyList} which only stores the
     * explicitly specified properties/attributes as bundles of
     * name-value-namespace strings
     */
    protected class MarkerPropertyList extends PropertyList implements
            Attributes {

        /** the array of attributes **/
        private MarkerAttribute[] attribs;

        /**
         * Overriding default constructor
         *
         * @param fobj
         *            the {@link FObj} to attach
         * @param parentPropertyList
         *            ignored
         */
        public MarkerPropertyList(final FObj fobj,
                final PropertyList parentPropertyList) {
            /*
             * ignore parentPropertyList won't be used because the attributes
             * will be stored without resolving
             */
            super(fobj, null);
        }

        /**
         * Override that doesn't convert the attributes to {@link Property}
         * instances, but simply stores the attributes for later processing.
         *
         * {@inheritDoc}
         */
        @Override
        public void addAttributesToList(final Attributes attributes)
                throws ValidationException {

            this.attribs = new MarkerAttribute[attributes.getLength()];

            String name;
            String value;
            String namespace;
            String qname;

            for (int i = attributes.getLength(); --i >= 0;) {
                namespace = attributes.getURI(i);
                qname = attributes.getQName(i);
                name = attributes.getLocalName(i);
                value = attributes.getValue(i);

                this.attribs[i] = MarkerAttribute.getInstance(namespace, qname,
                        name, value);
            }
        }

        /**
         * Null implementation; not used by this type of {@link PropertyList}.
         * 
         * @param propId
         *            the propert id
         * @param value
         *            the property value
         */
        @Override
        public void putExplicit(final int propId, final Property value) {
            // nop
        }

        /**
         * Null implementation; not used by this type of {@link PropertyList}.
         * 
         * @param propId
         *            the propert id
         * @return the property id
         */
        @Override
        public Property getExplicit(final int propId) {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public int getLength() {
            if (this.attribs == null) {
                return 0;
            } else {
                return this.attribs.length;
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getURI(final int index) {
            if (this.attribs != null && index < this.attribs.length
                    && index >= 0 && this.attribs[index] != null) {
                return this.attribs[index].namespace;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getLocalName(final int index) {
            if (this.attribs != null && index < this.attribs.length
                    && index >= 0 && this.attribs[index] != null) {
                return this.attribs[index].name;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getQName(final int index) {
            if (this.attribs != null && index < this.attribs.length
                    && index >= 0 && this.attribs[index] != null) {
                return this.attribs[index].qname;
            } else {
                return null;
            }
        }

        /**
         * Default implementation; not used.
         * 
         * @param index
         *            a type index
         * @return type string
         */
        @Override
        public String getType(final int index) {
            return "CDATA";
        }

        /** {@inheritDoc} */
        @Override
        public String getValue(final int index) {
            if (this.attribs != null && index < this.attribs.length
                    && index >= 0 && this.attribs[index] != null) {
                return this.attribs[index].value;
            } else {
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public int getIndex(final String name, final String namespace) {
            final int index = -1;
            if (this.attribs != null && name != null && namespace != null) {
                for (int i = this.attribs.length; --i >= 0;) {
                    if (this.attribs[i] != null
                            && namespace.equals(this.attribs[i].namespace)
                            && name.equals(this.attribs[i].name)) {
                        break;
                    }
                }
            }
            return index;
        }

        /** {@inheritDoc} */
        @Override
        public int getIndex(final String qname) {
            final int index = -1;
            if (this.attribs != null && qname != null) {
                for (int i = this.attribs.length; --i >= 0;) {
                    if (this.attribs[i] != null
                            && qname.equals(this.attribs[i].qname)) {
                        break;
                    }
                }
            }
            return index;
        }

        /**
         * Default implementation; not used
         * 
         * @param name
         *            a type name
         * @param namespace
         *            a type namespace
         * @return type string
         */
        @Override
        public String getType(final String name, final String namespace) {
            return "CDATA";
        }

        /**
         * Default implementation; not used
         * 
         * @param qname
         *            a type name
         * @return type string
         */
        @Override
        public String getType(final String qname) {
            return "CDATA";
        }

        /** {@inheritDoc} */
        @Override
        public String getValue(final String name, final String namespace) {
            final int index = getIndex(name, namespace);
            if (index > 0) {
                return getValue(index);
            }
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getValue(final String qname) {
            final int index = getIndex(qname);
            if (index > 0) {
                return getValue(index);
            }
            return null;
        }
    }

    /** Convenience inner class */
    public static final class MarkerAttribute {

        private static final PropertyCache<MarkerAttribute> CACHE = new PropertyCache<MarkerAttribute>();

        /** namespace */
        protected String namespace;
        /** qualfied name */
        protected String qname;
        /** local name */
        protected String name;
        /** value */
        protected String value;

        /**
         * Main constructor
         * 
         * @param namespace
         *            the namespace URI
         * @param qname
         *            the qualified name
         * @param name
         *            the name
         * @param value
         *            the value
         */
        private MarkerAttribute(final String namespace, final String qname,
                final String name, final String value) {
            this.namespace = namespace;
            this.qname = qname;
            this.name = name == null ? qname : name;
            this.value = value;
        }

        /**
         * Convenience method, reduces the number of distinct MarkerAttribute
         * instances
         *
         * @param namespace
         *            the attribute namespace
         * @param qname
         *            the fully qualified name of the attribute
         * @param name
         *            the attribute name
         * @param value
         *            the attribute value
         * @return the single MarkerAttribute instance corresponding to the
         *         name/value-pair
         */
        private static MarkerAttribute getInstance(final String namespace,
                final String qname, final String name, final String value) {
            return CACHE.fetch(new MarkerAttribute(namespace, qname, name,
                    value));
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            int hash = 17;
            hash = 37 * hash
                    + (this.namespace == null ? 0 : this.namespace.hashCode());
            hash = 37 * hash + (this.qname == null ? 0 : this.qname.hashCode());
            hash = 37 * hash + (this.name == null ? 0 : this.name.hashCode());
            hash = 37 * hash + (this.value == null ? 0 : this.value.hashCode());
            return hash;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o instanceof MarkerAttribute) {
                final MarkerAttribute attr = (MarkerAttribute) o;
                return (attr.namespace == this.namespace || attr.namespace != null
                        && attr.namespace.equals(this.namespace))
                        && (attr.qname == this.qname || attr.qname != null
                                && attr.qname.equals(this.qname))
                        && (attr.name == this.name || attr.name != null
                                && attr.name.equals(this.name))
                        && (attr.value == this.value || attr.value != null
                                && attr.value.equals(this.value));
            } else {
                return false;
            }
        }
    }
}
