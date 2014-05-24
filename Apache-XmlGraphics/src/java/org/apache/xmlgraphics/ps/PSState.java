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

/* $Id: PSState.java 1051421 2010-12-21 08:54:25Z jeremias $ */

package org.apache.xmlgraphics.ps;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.xmlgraphics.java2d.color.ColorUtil;

/**
 * This class holds the current state of the PostScript interpreter.
 *
 * @version $Id: PSState.java 1051421 2010-12-21 08:54:25Z jeremias $
 */
public class PSState implements Serializable {

    /** Default for setdash */
    public static final String DEFAULT_DASH = "[] 0";
    /** Default color in PostScript */
    public static final Color DEFAULT_RGB_COLOR = Color.black;

    private AffineTransform transform = new AffineTransform();
    private final List transformConcatList = new java.util.ArrayList();

    private int linecap = 0;
    private int linejoin = 0;
    private float miterLimit = 0;
    private double linewidth = 1.0f;
    private String dashpattern = DEFAULT_DASH;
    private Color color = DEFAULT_RGB_COLOR;

    // Font state
    private String fontname;
    private float fontsize;

    /**
     * Default constructor
     */
    public PSState() {
        // nop
    }

    /**
     * Copy constructor
     * 
     * @param org
     *            the original to copy from
     * @param copyTransforms
     *            true if the list of matrix concats should be cloned, too
     */
    public PSState(final PSState org, final boolean copyTransforms) {
        this.transform = (AffineTransform) org.transform.clone();
        if (copyTransforms) {
            this.transformConcatList.addAll(org.transformConcatList);
        }
        this.linecap = org.linecap;
        this.linejoin = org.linejoin;
        this.miterLimit = org.miterLimit;
        this.linewidth = org.linewidth;
        this.dashpattern = org.dashpattern;
        this.color = org.color;
        this.fontname = org.fontname;
        this.fontsize = org.fontsize;
    }

    /**
     * Returns the transform.
     * 
     * @return the current transformation matrix
     */
    public AffineTransform getTransform() {
        return this.transform;
    }

    /**
     * Check the current transform. The transform for the current state is the
     * combination of all transforms in the current state. The parameter is
     * compared against this current transform.
     *
     * @param tf
     *            the transform the check against
     * @return true if the new transform is different then the current transform
     */
    public boolean checkTransform(final AffineTransform tf) {
        return !tf.equals(this.transform);
    }

    /**
     * Concats the given transformation matrix with the current one.
     * 
     * @param transform
     *            The new transformation matrix
     */
    public void concatMatrix(final AffineTransform transform) {
        this.transformConcatList.add(transform);
        this.transform.concatenate(transform);
    }

    /**
     * Establishes the specified line cap.
     * 
     * @param value
     *            line cap (0, 1 or 2) as defined by the setlinecap command
     * @return true if the line cap changed compared to the previous setting
     */
    public boolean useLineCap(final int value) {
        if (this.linecap != value) {
            this.linecap = value;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Establishes the specified line join.
     * 
     * @param value
     *            line join (0, 1 or 2) as defined by the setlinejoin command
     * @return true if the line join changed compared to the previous setting
     */
    public boolean useLineJoin(final int value) {
        if (this.linejoin != value) {
            this.linejoin = value;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Establishes the specified miter limit.
     * 
     * @param value
     *            the miter limit as defined by the setmiterlimit command
     * @return true if the miter limit changed compared to the previous setting
     */
    public boolean useMiterLimit(final float value) {
        if (this.miterLimit != value) {
            this.miterLimit = value;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Establishes the specified line width.
     * 
     * @param value
     *            line width as defined by the setlinewidth command
     * @return true if the line width changed compared to the previous setting
     */
    public boolean useLineWidth(final double value) {
        if (this.linewidth != value) {
            this.linewidth = value;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Establishes the specified dash.
     * 
     * @param pattern
     *            dash pattern as defined by the setdash command
     * @return true if the dash pattern changed compared to the previous setting
     */
    public boolean useDash(final String pattern) {
        if (!this.dashpattern.equals(pattern)) {
            this.dashpattern = pattern;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Establishes the specified color (RGB).
     * 
     * @param value
     *            color as defined by the setrgbcolor command
     * @return true if the color changed compared to the previous setting
     */
    public boolean useColor(final Color value) {
        if (!ColorUtil.isSameColor(this.color, value)) {
            this.color = value;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Establishes the specified font and size.
     * 
     * @param name
     *            name of the font for the "F" command (see FOP Std Proc Set)
     * @param size
     *            size of the font
     * @return true if the font changed compared to the previous setting
     */
    public boolean useFont(final String name, final float size) {
        if (name == null) {
            throw new NullPointerException("font name must not be null");
        }
        if (this.fontname == null || !this.fontname.equals(name)
                || this.fontsize != size) {
            this.fontname = name;
            this.fontsize = size;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Reestablishes the graphics state represented by this instance by issueing
     * the necessary commands.
     * 
     * @param gen
     *            The generator to use for output
     * @exception IOException
     *                In case of an I/O problem
     */
    public void reestablish(final PSGenerator gen) throws IOException {
        for (int i = 0, len = this.transformConcatList.size(); i < len; i++) {
            gen.concatMatrix((AffineTransform) this.transformConcatList.get(i));
        }
        gen.useLineCap(this.linecap);
        gen.useLineWidth(this.linewidth);
        gen.useDash(this.dashpattern);
        gen.useColor(this.color);
        if (this.fontname != null) {
            gen.useFont(this.fontname, this.fontsize);
        }
    }

}
