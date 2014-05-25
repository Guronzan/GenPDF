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

/* $Id: GlyphNameFieldPart.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.util.text;

import java.util.Map;

import org.apache.fop.util.text.AdvancedMessageFormat.Part;
import org.apache.fop.util.text.AdvancedMessageFormat.PartFactory;
import org.apache.xmlgraphics.fonts.Glyphs;

/**
 * Function formatting a character to a glyph name.
 */
public class GlyphNameFieldPart implements Part {

    private final String fieldName;

    /**
     * Creates a new glyph name field part
     * 
     * @param fieldName
     *            the field name
     */
    public GlyphNameFieldPart(final String fieldName) {
        this.fieldName = fieldName;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isGenerated(final Map params) {
        final Object obj = params.get(this.fieldName);
        return obj != null && getGlyphName(obj).length() > 0;
    }

    private String getGlyphName(final Object obj) {
        if (obj instanceof Character) {
            return Glyphs.charToGlyphName(((Character) obj).charValue());
        } else {
            throw new IllegalArgumentException(
                    "Value for glyph name part must be a Character but was: "
                            + obj.getClass().getName());
        }
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
        sb.append(getGlyphName(obj));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "{" + this.fieldName + ",glyph-name}";
    }

    /** Factory for {@link GlyphNameFieldPart}. */
    public static class Factory implements PartFactory {

        /** {@inheritDoc} */
        @Override
        public Part newPart(final String fieldName, final String values) {
            return new GlyphNameFieldPart(fieldName);
        }

        /** {@inheritDoc} */
        @Override
        public String getFormat() {
            return "glyph-name";
        }

    }
}
