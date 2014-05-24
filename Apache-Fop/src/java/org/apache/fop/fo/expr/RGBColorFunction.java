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

/* $Id: RGBColorFunction.java 1328963 2012-04-22 20:09:42Z gadams $ */

package org.apache.fop.fo.expr;

import org.apache.fop.datatypes.PercentBase;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.properties.ColorProperty;
import org.apache.fop.fo.properties.Property;

/**
 * Implements the rgb() function.
 */
class RGBColorFunction extends FunctionBase {

    /** {@inheritDoc} */
    @Override
    public int getRequiredArgsCount() {
        return 3;
    }

    @Override
    /** {@inheritDoc} */
    public PercentBase getPercentBase() {
        return new RGBPercentBase();
    }

    /** {@inheritDoc} */
    @Override
    public Property eval(final Property[] args, final PropertyInfo pInfo)
            throws PropertyException {
        return ColorProperty.getInstance(pInfo.getUserAgent(), "rgb(" + args[0]
                + "," + args[1] + "," + args[2] + ")");

    }

    private static class RGBPercentBase implements PercentBase {
        @Override
        public int getDimension() {
            return 0;
        }

        @Override
        public double getBaseValue() {
            return 255f;
        }

        @Override
        public int getBaseLength(final PercentBaseContext context) {
            return 0;
        }

    }
}
