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

/* $Id: FunctionBase.java 1328964 2012-04-22 20:09:49Z gadams $ */

package org.apache.fop.fo.expr;

import org.apache.fop.datatypes.PercentBase;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.fo.properties.StringProperty;

/**
 * Abstract Base class for XSL-FO functions
 */
public abstract class FunctionBase implements Function {

    /** {@inheritDoc} */
    @Override
    public int getOptionalArgsCount() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public Property getOptionalArgDefault(final int index, final PropertyInfo pi)
            throws PropertyException {
        if (index >= getOptionalArgsCount()) {
            final PropertyException e = new PropertyException(
                    new IndexOutOfBoundsException(
                            "illegal optional argument index"));
            e.setPropertyInfo(pi);
            throw e;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasVariableArgs() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public PercentBase getPercentBase() {
        return null;
    }

    /**
     * @param pi
     *            property information instance that applies to property being
     *            evaluated
     * @return string property whose value is name of property being evaluated
     */
    protected final Property getPropertyName(final PropertyInfo pi) {
        return StringProperty.getInstance(pi.getPropertyMaker().getName());
    }
}
