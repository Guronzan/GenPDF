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

/* $Id: Dimension2DDouble.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.java2d;

import java.awt.geom.Dimension2D;
import java.io.Serializable;

/**
 * Subclass of Dimension2D which takes double values.
 */
public class Dimension2DDouble extends Dimension2D implements Serializable {

    private static final long serialVersionUID = 7909028357685520189L;

    private double width;
    private double height;

    /**
     * Default constructor.
     */
    public Dimension2DDouble() {
        this.width = 0;
        this.height = 0;
    }

    /**
     * Main constructor.
     * 
     * @param width
     *            initial width
     * @param height
     *            initial height
     */
    public Dimension2DDouble(final double width, final double height) {
        this.width = width;
        this.height = height;
    }

    /** {@inheritDoc} */
    @Override
    public double getWidth() {
        return this.width;
    }

    /** {@inheritDoc} */
    @Override
    public double getHeight() {
        return this.height;
    }

    /** {@inheritDoc} */
    @Override
    public void setSize(final double w, final double h) {
        this.width = w;
        this.height = h;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Dimension2DDouble) {
            final Dimension2DDouble other = (Dimension2DDouble) obj;
            if (Double.doubleToLongBits(this.height) != Double
                    .doubleToLongBits(other.height)) {
                return false;
            }
            if (Double.doubleToLongBits(this.width) != Double
                    .doubleToLongBits(other.width)) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final double sum = this.width + this.height;
        return (int) (sum * (sum + 1) / 2 + this.width);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClass().getName() + "[width=" + this.width + ",height="
                + this.height + "]";
    }
}
