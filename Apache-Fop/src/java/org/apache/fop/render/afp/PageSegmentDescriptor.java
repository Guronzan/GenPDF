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

/* $Id: PageSegmentDescriptor.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.render.afp;

/**
 * Class holding information on a page segment.
 */
class PageSegmentDescriptor {

    private final String name;
    private final String uri;

    /**
     * Creates a new page segment descriptor.
     * 
     * @param name
     *            the page segment name
     * @param uri
     *            the URI identifying the external resource file (may be null if
     *            the page segment shall be referenced rather than embedded)
     */
    public PageSegmentDescriptor(final String name, final String uri) {
        this.name = name;
        this.uri = uri;
    }

    /**
     * Returns the name of the page segment (usually 8 upper case letters).
     * 
     * @return the name of the page segment
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the URI of the external resource containing the page segment.
     * 
     * @return the URI of the external resource (or null if the resource is not
     *         to be embedded)
     */
    public String getURI() {
        return this.uri;
    }

}
