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

/* $Id: Startable.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.afp;

/**
 * Set and expose whether an object has started or not.
 */
public interface Startable {

    /**
     * Sets whether or not this object has started or not
     *
     * @param started
     *            true if this object has started
     */
    void setStarted(final boolean started);

    /**
     * Returns true if this object has started
     *
     * @return true if this object has started
     */
    boolean isStarted();
}
