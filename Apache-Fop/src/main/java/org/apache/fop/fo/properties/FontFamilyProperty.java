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

/* $Id: FontFamilyProperty.java 1330453 2012-04-25 18:09:51Z vhennebert $ */

package org.apache.fop.fo.properties;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Property class for the font-family property.
 */
@Slf4j
public final class FontFamilyProperty extends ListProperty {

    /** cache holding all canonical FontFamilyProperty instances */
    private static final PropertyCache<FontFamilyProperty> CACHE = new PropertyCache<FontFamilyProperty>();

    /**
     * Inner class for creating instances of ListProperty
     */
    public static class Maker extends PropertyMaker {

        /**
         * @param propId
         *            ID of the property for which Maker should be created
         */
        public Maker(final int propId) {
            super(propId);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Property make(final PropertyList propertyList,
                final String value, final FObj fo) throws PropertyException {
            if ("inherit".equals(value)) {
                return super.make(propertyList, value, fo);
            } else {
                final FontFamilyProperty prop = new FontFamilyProperty();
                String tmpVal;
                int startIndex = 0;
                int commaIndex = value.indexOf(',');
                int quoteIndex;
                int aposIndex;
                char qChar;
                boolean parsed = false;
                while (!parsed) {
                    if (commaIndex == -1) {
                        tmpVal = value.substring(startIndex).trim();
                        parsed = true;
                    } else {
                        tmpVal = value.substring(startIndex, commaIndex).trim();
                        startIndex = commaIndex + 1;
                        commaIndex = value.indexOf(',', startIndex);
                    }
                    aposIndex = tmpVal.indexOf('\'');
                    quoteIndex = tmpVal.indexOf('\"');
                    if (aposIndex != -1 || quoteIndex != -1) {
                        qChar = aposIndex == -1 ? '\"' : '\'';
                        if (tmpVal.lastIndexOf(qChar) != tmpVal.length() - 1) {
                            log.warn("Skipping malformed value for font-family: "
                                    + tmpVal + " in \"" + value + "\".");
                            tmpVal = "";
                        } else {
                            tmpVal = tmpVal.substring(1, tmpVal.length() - 1);
                        }
                    }
                    if (!"".equals(tmpVal)) {
                        int dblSpaceIndex = tmpVal.indexOf("  ");
                        while (dblSpaceIndex != -1) {
                            tmpVal = tmpVal.substring(0, dblSpaceIndex)
                                    + tmpVal.substring(dblSpaceIndex + 1);
                            dblSpaceIndex = tmpVal.indexOf("  ");
                        }
                        prop.addProperty(StringProperty.getInstance(tmpVal));
                    }
                }
                return CACHE.fetch(prop);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Property convertProperty(final Property p,
                final PropertyList propertyList, final FObj fo) {
            if (p instanceof FontFamilyProperty) {
                return p;
            } else {
                return new FontFamilyProperty(p);
            }
        }

    }

    /**
     * @param prop
     *            the first Property to be added to the list
     */
    private FontFamilyProperty(final Property prop) {
        super();
        addProperty(prop);
    }

    /**
     * Default constructor.
     *
     */
    private FontFamilyProperty() {
        super();
    }

    /**
     * Add a new property to the list
     *
     * @param prop
     *            Property to be added to the list
     */
    @Override
    public void addProperty(final Property prop) {
        if (prop.getList() != null) {
            this.list.addAll(prop.getList());
        } else {
            super.addProperty(prop);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getString() {
        if (this.list.size() > 0) {
            final Property first = this.list.get(0);
            return first.getString();
        } else {
            return super.getString();
        }
    }

}
