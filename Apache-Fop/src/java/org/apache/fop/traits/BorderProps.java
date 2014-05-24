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

/* $Id: BorderProps.java 1069439 2011-02-10 15:58:57Z jeremias $ */

package org.apache.fop.traits;

import java.awt.Color;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.util.ColorUtil;

/**
 * Border properties. Class to store border trait properties for the area tree.
 */
public class BorderProps implements Serializable {

    private static final long serialVersionUID = -886871454032189183L;

    /** Separate border model */
    public static final int SEPARATE = 0;
    /** Collapsing border model, for borders inside a table */
    public static final int COLLAPSE_INNER = 1;
    /** Collapsing border model, for borders at the table's outer border */
    public static final int COLLAPSE_OUTER = 2;

    /** Border style (one of EN_*) */
    public int style; // Enum for border style // CSOK: VisibilityModifier
    /** Border color */
    public Color color; // CSOK: VisibilityModifier
    /** Border width */
    public int width; // CSOK: VisibilityModifier
    /** Border mode (one of SEPARATE, COLLAPSE_INNER and COLLAPSE_OUTER) */
    public int mode; // CSOK: VisibilityModifier

    /**
     * Constructs a new BorderProps instance.
     * 
     * @param style
     *            border style (one of EN_*)
     * @param width
     *            border width
     * @param color
     *            border color
     * @param mode
     *            border mode ((one of SEPARATE, COLLAPSE_INNER and
     *            COLLAPSE_OUTER)
     */
    public BorderProps(final int style, final int width, final Color color,
            final int mode) {
        this.style = style;
        this.width = width;
        this.color = color;
        this.mode = mode;
    }

    /**
     * Constructs a new BorderProps instance.
     * 
     * @param style
     *            border style (one of the XSL enum values for border style)
     * @param width
     *            border width
     * @param color
     *            border color
     * @param mode
     *            border mode ((one of SEPARATE, COLLAPSE_INNER and
     *            COLLAPSE_OUTER)
     */
    public BorderProps(final String style, final int width, final Color color,
            final int mode) {
        this(getConstantForStyle(style), width, color, mode);
    }

    /**
     * @param bp
     *            the border properties or null
     * @return the effective width of the clipped part of the border
     */
    public static int getClippedWidth(final BorderProps bp) {
        if (bp != null && bp.mode != SEPARATE) {
            return bp.width / 2;
        } else {
            return 0;
        }
    }

    private String getStyleString() {
        return BorderStyle.valueOf(this.style).getName();
    }

    private static int getConstantForStyle(final String style) {
        return BorderStyle.valueOf(style).getEnumValue();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else {
            if (obj instanceof BorderProps) {
                final BorderProps other = (BorderProps) obj;
                return this.style == other.style
                        && org.apache.xmlgraphics.java2d.color.ColorUtil
                                .isSameColor(this.color, other.color)
                                && this.width == other.width && this.mode == other.mode;
            }
        }
        return false;
    }

    /**
     * Returns a BorderProps represtation of a string of the format as written
     * by BorderProps.toString().
     * 
     * @param foUserAgent
     *            FOP user agent caching ICC profiles
     * @param s
     *            the string
     * @return a BorderProps instance
     */
    public static BorderProps valueOf(final FOUserAgent foUserAgent, String s) {
        if (s.startsWith("(") && s.endsWith(")")) {
            s = s.substring(1, s.length() - 1);
            final Pattern pattern = Pattern.compile("([^,\\(]+(?:\\(.*\\))?)");
            final Matcher m = pattern.matcher(s);
            boolean found;
            found = m.find();
            final String style = m.group();
            found = m.find();
            final String color = m.group();
            found = m.find();
            final int width = Integer.parseInt(m.group());
            int mode = SEPARATE;
            found = m.find();
            if (found) {
                final String ms = m.group();
                if ("collapse-inner".equalsIgnoreCase(ms)) {
                    mode = COLLAPSE_INNER;
                } else if ("collapse-outer".equalsIgnoreCase(ms)) {
                    mode = COLLAPSE_OUTER;
                }
            }
            Color c;
            try {
                c = ColorUtil.parseColorString(foUserAgent, color);
            } catch (final PropertyException e) {
                throw new IllegalArgumentException(e.getMessage());
            }

            return new BorderProps(style, width, c, mode);
        } else {
            throw new IllegalArgumentException(
                    "BorderProps must be surrounded by parentheses");
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sbuf = new StringBuffer();
        sbuf.append('(');
        sbuf.append(getStyleString());
        sbuf.append(',');
        sbuf.append(ColorUtil.colorToString(this.color));
        sbuf.append(',');
        sbuf.append(this.width);
        if (this.mode != SEPARATE) {
            sbuf.append(',');
            if (this.mode == COLLAPSE_INNER) {
                sbuf.append("collapse-inner");
            } else {
                sbuf.append("collapse-outer");
            }
        }
        sbuf.append(')');
        return sbuf.toString();
    }

}
