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

/* $Id: ImageKey.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.image.loader.cache;

import org.apache.xmlgraphics.image.loader.ImageFlavor;

/**
 * Key class for Image instances in the cache.
 */
public class ImageKey {

    private final String uri;
    private final org.apache.xmlgraphics.image.loader.ImageFlavor flavor;

    /**
     * Main constructor.
     * 
     * @param uri
     *            the original URI
     * @param flavor
     *            the image flavor
     */
    public ImageKey(final String uri, final ImageFlavor flavor) {
        if (uri == null) {
            throw new NullPointerException("URI must not be null");
        }
        if (flavor == null) {
            throw new NullPointerException("flavor must not be null");
        }
        this.uri = uri;
        this.flavor = flavor;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (this.flavor == null ? 0 : this.flavor.hashCode());
        result = prime * result + (this.uri == null ? 0 : this.uri.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ImageKey other = (ImageKey) obj;
        if (!this.uri.equals(other.uri)) {
            return false;
        }
        if (!this.flavor.equals(other.flavor)) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.uri + " (" + this.flavor + ")";
    }

}
