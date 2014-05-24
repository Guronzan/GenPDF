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

/* $Id: KeepProperty.java 1303891 2012-03-22 17:04:12Z vhennebert $ */

package org.apache.fop.fo.properties;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.datatypes.CompoundDatatype;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Class for properties that wrap Keep values
 */
@Slf4j
public final class KeepProperty extends Property implements CompoundDatatype {

    /** class holding all canonical KeepProperty instances */
    private static final PropertyCache<KeepProperty> CACHE = new PropertyCache<KeepProperty>();

    private boolean isCachedValue = false;
    private Property withinLine;
    private Property withinColumn;
    private Property withinPage;

    /**
     * Inner class for creating instances of KeepProperty
     */
    public static class Maker extends CompoundPropertyMaker {

        /**
         * @param propId
         *            the id of the property for which a Maker should be created
         */
        public Maker(final int propId) {
            super(propId);
        }

        /**
         * Create a new empty instance of KeepProperty.
         *
         * @return the new instance.
         */
        @Override
        public Property makeNewProperty() {
            return new KeepProperty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Property convertProperty(final Property p,
                final PropertyList propertyList, final FObj fo)
                throws PropertyException {
            if (p instanceof KeepProperty) {
                return p;
            }
            return super.convertProperty(p, propertyList, fo);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setComponent(final int cmpId, final Property cmpnValue,
            final boolean bIsDefault) {
        if (this.isCachedValue) {
            log.warn("KeepProperty.setComponent() called on cached value. Ignoring...");
            return;
        }
        if (cmpId == CP_WITHIN_LINE) {
            setWithinLine(cmpnValue, bIsDefault);
        } else if (cmpId == CP_WITHIN_COLUMN) {
            setWithinColumn(cmpnValue, bIsDefault);
        } else if (cmpId == CP_WITHIN_PAGE) {
            setWithinPage(cmpnValue, bIsDefault);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property getComponent(final int cmpId) {
        if (cmpId == CP_WITHIN_LINE) {
            return getWithinLine();
        } else if (cmpId == CP_WITHIN_COLUMN) {
            return getWithinColumn();
        } else if (cmpId == CP_WITHIN_PAGE) {
            return getWithinPage();
        } else {
            return null;
        }
    }

    /**
     * @param withinLine
     *            withinLine property to set
     * @param bIsDefault
     *            not used (??)
     */
    public void setWithinLine(final Property withinLine,
            final boolean bIsDefault) {
        this.withinLine = withinLine;
    }

    /**
     * @param withinColumn
     *            withinColumn property to set
     * @param bIsDefault
     *            not used (??)
     */
    protected void setWithinColumn(final Property withinColumn,
            final boolean bIsDefault) {
        this.withinColumn = withinColumn;
    }

    /**
     * @param withinPage
     *            withinPage property to set
     * @param bIsDefault
     *            not used (??)
     */
    public void setWithinPage(final Property withinPage,
            final boolean bIsDefault) {
        this.withinPage = withinPage;
    }

    /**
     * @return the withinLine property
     */
    public Property getWithinLine() {
        return this.withinLine;
    }

    /**
     * @return the withinColumn property
     */
    public Property getWithinColumn() {
        return this.withinColumn;
    }

    /**
     * @return the withinPage property
     */
    public Property getWithinPage() {
        return this.withinPage;
    }

    /**
     * Not sure what to do here. There isn't really a meaningful single value.
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return "Keep[" + "withinLine:" + getWithinLine().getObject()
                + ", withinColumn:" + getWithinColumn().getObject()
                + ", withinPage:" + getWithinPage().getObject() + "]";
    }

    /**
     * @return the canonical KeepProperty instance corresponding to this
     *         property
     */
    @Override
    public KeepProperty getKeep() {
        final KeepProperty keep = CACHE.fetch(this);
        /* make sure setComponent() can never alter cached values */
        keep.isCachedValue = true;
        return keep;
    }

    /**
     * @return this.keep cast as Object
     */
    @Override
    public Object getObject() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof KeepProperty) {
            final KeepProperty keep = (KeepProperty) o;
            return keep.withinColumn == this.withinColumn
                    && keep.withinLine == this.withinLine
                    && keep.withinPage == this.withinPage;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hash = 17;
        hash = 37
                * hash
                + (this.withinColumn == null ? 0 : this.withinColumn.hashCode());
        hash = 37 * hash
                + (this.withinLine == null ? 0 : this.withinLine.hashCode());
        hash = 37 * hash
                + (this.withinPage == null ? 0 : this.withinPage.hashCode());
        return hash;
    }
}
