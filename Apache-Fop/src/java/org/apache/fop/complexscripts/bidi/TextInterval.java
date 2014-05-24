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

/* $Id$ */

package org.apache.fop.complexscripts.bidi;

import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.flow.AbstractGraphics;
import org.apache.fop.fo.flow.AbstractPageNumberCitation;
import org.apache.fop.fo.flow.BidiOverride;
import org.apache.fop.fo.flow.Character;
import org.apache.fop.fo.flow.Leader;

// CSOFF: LineLengthCheck
// CSOFF: SimplifyBooleanReturnCheck

/**
 * <p>
 * The <code>TextInterval</code> class is a utility class, the instances of
 * which are used to record backpointers to associated nodes over sub-intervals
 * of a delimited text range.
 * </p>
 *
 * <p>
 * This work was originally authored by Glenn Adams (gadams@apache.org).
 * </p>
 */
class TextInterval {
    private final FONode fn; // associated node
    private final int textStart; // starting index within delimited text range
                                 // of associated node's text
    private final int start; // starting index within delimited text range
    private final int end; // ending index within delimited text range
    private int level; // resolved level or default (-1)

    TextInterval(final FONode fn, final int start, final int end) {
        this(fn, start, start, end, -1);
    }

    TextInterval(final FONode fn, final int textStart, final int start,
            final int end, final int level) {
        this.fn = fn;
        this.textStart = textStart;
        this.start = start;
        this.end = end;
        this.level = level;
    }

    FONode getNode() {
        return this.fn;
    }

    int getTextStart() {
        return this.textStart;
    }

    int getStart() {
        return this.start;
    }

    int getEnd() {
        return this.end;
    }

    int getLevel() {
        return this.level;
    }

    void setLevel(final int level) {
        this.level = level;
    }

    public int length() {
        return this.end - this.start;
    }

    public String getText() {
        if (this.fn instanceof FOText) {
            return ((FOText) this.fn).getCharSequence().toString();
        } else if (this.fn instanceof Character) {
            return new String(
                    new char[] { ((Character) this.fn).getCharacter() });
        } else {
            return null;
        }
    }

    public void assignTextLevels() {
        if (this.fn instanceof FOText) {
            ((FOText) this.fn).setBidiLevel(this.level, this.start
                    - this.textStart, this.end - this.textStart);
        } else if (this.fn instanceof Character) {
            ((Character) this.fn).setBidiLevel(this.level);
        } else if (this.fn instanceof AbstractPageNumberCitation) {
            ((AbstractPageNumberCitation) this.fn).setBidiLevel(this.level);
        } else if (this.fn instanceof AbstractGraphics) {
            ((AbstractGraphics) this.fn).setBidiLevel(this.level);
        } else if (this.fn instanceof Leader) {
            ((Leader) this.fn).setBidiLevel(this.level);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof TextInterval) {
            final TextInterval ti = (TextInterval) o;
            if (ti.getNode() != this.fn) {
                return false;
            } else if (ti.getStart() != this.start) {
                return false;
            } else if (ti.getEnd() != this.end) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int l = this.fn != null ? this.fn.hashCode() : 0;
        l = (l ^ this.start) + (l << 19);
        l = (l ^ this.end) + (l << 11);
        return l;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        char c;
        if (this.fn instanceof FOText) {
            c = 'T';
        } else if (this.fn instanceof Character) {
            c = 'C';
        } else if (this.fn instanceof BidiOverride) {
            c = 'B';
        } else if (this.fn instanceof AbstractPageNumberCitation) {
            c = '#';
        } else if (this.fn instanceof AbstractGraphics) {
            c = 'G';
        } else if (this.fn instanceof Leader) {
            c = 'L';
        } else {
            c = '?';
        }
        sb.append(c);
        sb.append("[" + this.start + "," + this.end + "][" + this.textStart
                + "](" + this.level + ")");
        return sb.toString();
    }
}
