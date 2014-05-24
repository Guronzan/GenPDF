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

/* $Id: CIELabColorFunction.java 1328963 2012-04-22 20:09:42Z gadams $ */

package org.apache.fop.fo.expr;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.datatypes.PercentBase;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.properties.ColorProperty;
import org.apache.fop.fo.properties.Property;

/**
 * Implements the cie-lab-color() function.
 * 
 * @since XSL-FO 2.0
 */
class CIELabColorFunction extends FunctionBase {

    /** {@inheritDoc} */
    @Override
    public int getRequiredArgsCount() {
        return 6;
    }

    @Override
    /** {@inheritDoc} */
    public PercentBase getPercentBase() {
        return new CIELabPercentBase();
    }

    /** {@inheritDoc} */
    @Override
    public Property eval(final Property[] args, final PropertyInfo pInfo)
            throws PropertyException {

        final float red = args[0].getNumber().floatValue();
        final float green = args[1].getNumber().floatValue();
        final float blue = args[2].getNumber().floatValue();
        /* Verify sRGB replacement arguments */
        if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0
                || blue > 255) {
            throw new PropertyException(
                    "sRGB color values out of range. "
                            + "Arguments to cie-lab-color() must be [0..255] or [0%..100%]");
        }

        final float l = args[3].getNumber().floatValue();
        final float a = args[4].getNumber().floatValue();
        final float b = args[5].getNumber().floatValue();
        if (l < 0 || l > 100) {
            throw new PropertyException(
                    "L* value out of range. Valid range: [0..100]");
        }
        if (a < -127 || a > 127 || b < -127 || b > 127) {
            throw new PropertyException(
                    "a* and b* values out of range. Valid range: [-127..+127]");
        }

        final StringBuffer sb = new StringBuffer();
        sb.append("cie-lab-color(" + red + "," + green + "," + blue + "," + l
                + "," + a + "," + b + ")");
        final FOUserAgent ua = pInfo == null ? null
                        : pInfo.getFO() == null ? null : pInfo.getFO().getUserAgent();
        return ColorProperty.getInstance(ua, sb.toString());
    }

    private static class CIELabPercentBase implements PercentBase {
        @Override
        public int getDimension() {
            return 0;
        }

        @Override
        public double getBaseValue() {
            return 1.0f;
        }

        @Override
        public int getBaseLength(final PercentBaseContext context) {
            return 0;
        }

    }

}
