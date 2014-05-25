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

/* $Id: CMYKColorFunction.java 1328964 2012-04-22 20:09:49Z gadams $ */

package org.apache.fop.fo.expr;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.properties.ColorProperty;
import org.apache.fop.fo.properties.Property;

/**
 * Implements the cmyk() function.
 */
class CMYKColorFunction extends FunctionBase {

    /** {@inheritDoc} */
    @Override
    public int getRequiredArgsCount() {
        return 4;
    }

    /** {@inheritDoc} */
    @Override
    public Property eval(final Property[] args, final PropertyInfo pInfo)
            throws PropertyException {
        final StringBuilder sb = new StringBuilder();
        sb.append("cmyk(" + args[0] + "," + args[1] + "," + args[2] + ","
                + args[3] + ")");
        final FOUserAgent ua = pInfo == null ? null
                        : pInfo.getFO() == null ? null : pInfo.getFO().getUserAgent();
        return ColorProperty.getInstance(ua, sb.toString());
    }

}