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

/* $Id: GraphicsSetArcParameters.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.afp.goca;

/**
 * Sets the arc parameters for a GOCA graphics arc (circle/ellipse)
 */
public class GraphicsSetArcParameters extends AbstractGraphicsCoord {

    /**
     * Constructor
     *
     * @param xmaj
     *            x coordinate of the major axis point
     * @param ymin
     *            y coordinate of the minor axis point
     * @param xmin
     *            x coordinate of the minor axis point
     * @param ymaj
     *            y coordinate of the major axis point
     */
    public GraphicsSetArcParameters(final int xmaj, final int ymin,
            final int xmin, final int ymaj) {
        super(xmaj, ymin, xmin, ymaj);
    }

    /** {@inheritDoc} */
    @Override
    protected byte getOrderCode() {
        return 0x22;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getName() + "{xmaj=" + this.coords[0] + ",ymin="
                + this.coords[1] + ",xmin=" + this.coords[2] + ",ymaj="
                + this.coords[3] + "}";
    }
}
