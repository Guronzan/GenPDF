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

/* $Id: ResourceAccessor.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.afp.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Defines an interface through which external resource objects can be accessed.
 */
public interface ResourceAccessor {

    /**
     * Creates a new {@link InputStream} for the given URI that allows read
     * access to an external resource.
     * 
     * @param uri
     *            the URI of an external resource.
     * @return the new input stream
     * @throws IOException
     *             if an I/O error occurs while opening the resource
     */
    InputStream createInputStream(final URI uri) throws IOException;

}
