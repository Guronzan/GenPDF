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

/* $Id: HexFieldPart.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.util.text;

import java.util.Map;

import org.apache.fop.util.text.AdvancedMessageFormat.Part;
import org.apache.fop.util.text.AdvancedMessageFormat.PartFactory;

/**
 * Function formatting a number or character to a hex value.
 */
public class HexFieldPart implements Part {

    private final String fieldName;

    /**
     * Creates a new hex field part
     * 
     * @param fieldName
     *            the field name
     */
    public HexFieldPart(final String fieldName) {
        this.fieldName = fieldName;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isGenerated(final Map params) {
        final Object obj = params.get(this.fieldName);
        return obj != null;
    }

    /** {@inheritDoc} */
    @Override
    public void write(final StringBuilder sb, final Map params) {
        if (!params.containsKey(this.fieldName)) {
            throw new IllegalArgumentException(
                    "Message pattern contains unsupported field name: "
                            + this.fieldName);
        }
        final Object obj = params.get(this.fieldName);
        if (obj instanceof Character) {
            sb.append(Integer.toHexString(((Character) obj).charValue()));
        } else if (obj instanceof Number) {
            sb.append(Integer.toHexString(((Number) obj).intValue()));
        } else {
            throw new IllegalArgumentException(
                    "Incompatible value for hex field part: "
                            + obj.getClass().getName());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "{" + this.fieldName + ",hex}";
    }

    /** Factory for {@link HexFieldPart}. */
    public static class Factory implements PartFactory {

        /** {@inheritDoc} */
        @Override
        public Part newPart(final String fieldName, final String values) {
            return new HexFieldPart(fieldName);
        }

        /** {@inheritDoc} */
        @Override
        public String getFormat() {
            return "hex";
        }

    }
}
