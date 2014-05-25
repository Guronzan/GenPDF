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

/* $Id: Hyphen.java 1297284 2012-03-05 23:29:29Z gadams $ */

package org.apache.fop.hyphenation;

import java.io.Serializable;

/**
 * <p>This class represents a hyphen. A 'full' hyphen is made of 3 parts:
 * the pre-break text, post-break text and no-break. If no line-break
 * is generated at this position, the no-break text is used, otherwise,
 * pre-break and post-break are used. Typically, pre-break is equal to
 * the hyphen character and the others are empty. However, this general
 * scheme allows support for cases in some languages where words change
 * spelling if they're split across lines, like german's 'backen' which
 * hyphenates 'bak-ken'. BTW, this comes from TeX.</p>
 *
 * <p>This work was authored by Carlos Villegas (cav@uniscope.co.jp).</p>
 */

/**
 * Represents a hyphen.
 */
public class Hyphen implements Serializable {

    private static final long serialVersionUID = 8989909741110279085L;

    /** pre break string */
    public String preBreak; // CSOK: VisibilityModifier

    /** no break string */
    public String noBreak; // CSOK: VisibilityModifier

    /** post break string */
    public String postBreak; // CSOK: VisibilityModifier

    /**
     * Construct a hyphen.
     * 
     * @param pre
     *            break string
     * @param no
     *            break string
     * @param post
     *            break string
     */
    Hyphen(final String pre, final String no, final String post) {
        this.preBreak = pre;
        this.noBreak = no;
        this.postBreak = post;
    }

    /**
     * Construct a hyphen.
     * 
     * @param pre
     *            break string
     */
    Hyphen(final String pre) {
        this.preBreak = pre;
        this.noBreak = null;
        this.postBreak = null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (this.noBreak == null && this.postBreak == null
                && this.preBreak != null && this.preBreak.equals("-")) {
            return "-";
        }
        final StringBuilder res = new StringBuilder("{");
        res.append(this.preBreak);
        res.append("}{");
        res.append(this.postBreak);
        res.append("}{");
        res.append(this.noBreak);
        res.append('}');
        return res.toString();
    }

}
