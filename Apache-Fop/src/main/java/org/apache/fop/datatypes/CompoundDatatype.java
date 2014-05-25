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

/* $Id: CompoundDatatype.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.datatypes;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.properties.Property;

/**
 * This interface is used as a base for compound datatypes.
 */
public interface CompoundDatatype extends Constants {

    /**
     * Sets a component of the compound datatype.
     * 
     * @param cmpId
     *            ID of the component
     * @param cmpnValue
     *            value of the component
     * @param bIsDefault
     *            Indicates if it's the default value
     */
    void setComponent(final int cmpId, final Property cmpnValue,
            final boolean bIsDefault);

    /**
     * Returns a component of the compound datatype.
     * 
     * @param cmpId
     *            ID of the component
     * @return the value of the component
     */
    Property getComponent(final int cmpId);
}