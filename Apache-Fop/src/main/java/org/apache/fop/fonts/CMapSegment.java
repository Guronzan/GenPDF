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

/* $Id: CMapSegment.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.fonts;

/**
 * A segment in a cmap table of format 4. Unicode code points between
 * {@link #getUnicodeStart()} and {@link #getUnicodeEnd()} map to contiguous
 * glyph indices starting from {@link #getGlyphStartIndex()}.
 */
public final class CMapSegment {

    private final int unicodeStart;
    private final int unicodeEnd;
    private final int glyphStartIndex;

    /**
     * Creates a new segment.
     *
     * @param unicodeStart
     *            Unicode start index
     * @param unicodeEnd
     *            Unicode end index
     * @param glyphStartIndex
     *            glyph start index
     */
    public CMapSegment(final int unicodeStart, final int unicodeEnd,
            final int glyphStartIndex) {
        this.unicodeStart = unicodeStart;
        this.unicodeEnd = unicodeEnd;
        this.glyphStartIndex = glyphStartIndex;
    }

    @Override
    public int hashCode() {
        int hc = 17;
        hc = 31 * hc + this.unicodeStart;
        hc = 31 * hc + this.unicodeEnd;
        hc = 31 * hc + this.glyphStartIndex;
        return hc;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof CMapSegment) {
            final CMapSegment ce = (CMapSegment) o;
            return ce.unicodeStart == this.unicodeStart
                    && ce.unicodeEnd == this.unicodeEnd
                    && ce.glyphStartIndex == this.glyphStartIndex;
        }
        return false;
    }

    /**
     * Returns the unicodeStart.
     * 
     * @return the Unicode start index
     */
    public int getUnicodeStart() {
        return this.unicodeStart;
    }

    /**
     * Returns the unicodeEnd.
     * 
     * @return the Unicode end index
     */
    public int getUnicodeEnd() {
        return this.unicodeEnd;
    }

    /**
     * Returns the glyphStartIndex.
     * 
     * @return the glyph start index
     */
    public int getGlyphStartIndex() {
        return this.glyphStartIndex;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CMapSegment: ");
        sb.append("{ UC[");
        sb.append(this.unicodeStart);
        sb.append(',');
        sb.append(this.unicodeEnd);
        sb.append("]: GC[");
        sb.append(this.glyphStartIndex);
        sb.append(',');
        sb.append(this.glyphStartIndex + this.unicodeEnd - this.unicodeStart);
        sb.append("] }");
        return sb.toString();
    }

}
