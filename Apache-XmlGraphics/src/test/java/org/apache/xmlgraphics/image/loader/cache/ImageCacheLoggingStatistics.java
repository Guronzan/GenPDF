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

/* $Id: ImageCacheLoggingStatistics.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.image.loader.cache;

import lombok.extern.slf4j.Slf4j;

/**
 * ImageCacheListener implementation for debugging purposes.
 */
@Slf4j
public class ImageCacheLoggingStatistics extends ImageCacheStatistics {

    /**
     * Main constructor.
     *
     * @param detailed
     *            true if statistics for each URI should be kept.
     */
    public ImageCacheLoggingStatistics(final boolean detailed) {
        super(detailed);
    }

    /** {@inheritDoc} */
    @Override
    public void invalidHit(final String uri) {
        super.invalidHit(uri);
        log.info("Invalid HIT: " + uri);
    }

    /** {@inheritDoc} */
    @Override
    public void cacheHitImage(final ImageKey key) {
        super.cacheHitImage(key);
        log.info("Image Cache HIT: " + key);
    }

    /** {@inheritDoc} */
    @Override
    public void cacheHitImageInfo(final String uri) {
        super.cacheHitImageInfo(uri);
        log.info("ImageInfo Cache HIT: " + uri);
    }

    /** {@inheritDoc} */
    @Override
    public void cacheMissImage(final ImageKey key) {
        super.cacheMissImage(key);
        log.info("Image Cache MISS: " + key);
    }

    /** {@inheritDoc} */
    @Override
    public void cacheMissImageInfo(final String uri) {
        super.cacheMissImageInfo(uri);
        log.info("ImageInfo Cache MISS: " + uri);
    }

}
