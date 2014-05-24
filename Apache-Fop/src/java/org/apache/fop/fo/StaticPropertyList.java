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

/* $Id: StaticPropertyList.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.fo;

import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fo.properties.Property;

/**
 * A very fast implementation of PropertyList that uses arrays to store the
 * explicit set properties and another array to store cached values.
 */
public class StaticPropertyList extends PropertyList {
    private final Property[] explicit;
    private final Property[] values;

    /**
     * Construct a StaticPropertyList.
     * 
     * @param fObjToAttach
     *            The FObj object.
     * @param parentPropertyList
     *            The parent property list.
     */
    public StaticPropertyList(final FObj fObjToAttach,
            final PropertyList parentPropertyList) {
        super(fObjToAttach, parentPropertyList);
        this.explicit = new Property[Constants.PROPERTY_COUNT + 1];
        this.values = new Property[Constants.PROPERTY_COUNT + 1];
    }

    /**
     * Return the value explicitly specified on this FO.
     * 
     * @param propId
     *            The ID of the property whose value is desired.
     * @return The value if the property is explicitly set, otherwise null.
     */
    @Override
    public Property getExplicit(final int propId) {
        return this.explicit[propId];
    }

    /**
     * Set an value defined explicitly on this FO.
     * 
     * @param propId
     *            The ID of the property whose value is desired.
     * @param value
     *            The value of the property to set.
     */
    @Override
    public void putExplicit(final int propId, final Property value) {
        this.explicit[propId] = value;
        if (this.values[propId] != null) { // if the cached value is set
                                           // overwrite it
            this.values[propId] = value;
        }
    }

    /**
     * Override PropertyList.get() and provides fast caching of previously
     * retrieved property values. {@inheritDoc}
     */
    @Override
    public Property get(final int propId, final boolean bTryInherit,
            final boolean bTryDefault) throws PropertyException {
        Property p = this.values[propId];
        if (p == null) {
            p = super.get(propId, bTryInherit, bTryDefault);
            this.values[propId] = p;
        }
        return p;
    }
}
