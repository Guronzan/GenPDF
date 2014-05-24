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

/* $Id: ImageCacheTestCase.java 816640 2009-09-18 14:14:55Z maxberger $ */

package org.apache.xmlgraphics.image.loader.cache;

import java.io.FileNotFoundException;

import junit.framework.TestCase;

import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.MockImageContext;
import org.apache.xmlgraphics.image.loader.impl.ImageBuffered;

/**
 * Tests for bundled ImageLoader implementations.
 */
public class ImageCacheTestCase extends TestCase {

    private static final boolean DEBUG = false;

    private final MockImageContext imageContext = MockImageContext
            .getInstance();
    private final ImageSessionContext sessionContext = this.imageContext
            .newSessionContext();
    private final ImageManager manager = this.imageContext.getImageManager();
    private final ImageCacheStatistics statistics = DEBUG ? new ImageCacheLoggingStatistics(
            true) : new ImageCacheStatistics(true);

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.manager.getCache().clearCache();
        this.statistics.reset();
        this.manager.getCache().setCacheListener(this.statistics);
    }

    /**
     * Tests the ImageInfo cache.
     *
             * @throws Exception
             *             if an error occurs
     */
    public void testImageInfoCache() throws Exception {
        final String invalid1 = "invalid1.jpg";
        final String invalid2 = "invalid2.jpg";
        final String valid1 = "bgimg300dpi.bmp";
        final String valid2 = "big-image.png";

                ImageInfo info1, info2;
        info1 = this.manager.getImageInfo(valid1, this.sessionContext);
        assertNotNull(info1);
        assertEquals(valid1, info1.getOriginalURI());

        try {
            this.manager.getImageInfo(invalid1, this.sessionContext);
            fail("Expected FileNotFoundException for invalid URI");
        } catch (final FileNotFoundException e) {
            // expected
        }

        // 2 requests:
        assertEquals(0, this.statistics.getImageInfoCacheHits());
        assertEquals(2, this.statistics.getImageInfoCacheMisses());
        assertEquals(0, this.statistics.getInvalidHits());
        this.statistics.reset();

        // Cache Hit
        info1 = this.manager.getImageInfo(valid1, this.sessionContext);
        assertNotNull(info1);
        assertEquals(valid1, info1.getOriginalURI());

        // Cache Miss
        info2 = this.manager.getImageInfo(valid2, this.sessionContext);
        assertNotNull(info2);
        assertEquals(valid2, info2.getOriginalURI());

        try {
            // Invalid Hit
            this.manager.getImageInfo(invalid1, this.sessionContext);
            fail("Expected FileNotFoundException for invalid URI");
        } catch (final FileNotFoundException e) {
            // expected
        }
        try {
            // Invalid (Cache Miss)
            this.manager.getImageInfo(invalid2, this.sessionContext);
            fail("Expected FileNotFoundException for invalid URI");
        } catch (final FileNotFoundException e) {
            // expected
        }

        // 4 requests:
        assertEquals(1, this.statistics.getImageInfoCacheHits());
        assertEquals(2, this.statistics.getImageInfoCacheMisses());
        assertEquals(1, this.statistics.getInvalidHits());
        this.statistics.reset();
    }

    public void testInvalidURIExpiration() {
        final MockTimeStampProvider provider = new MockTimeStampProvider();
        final ImageCache cache = new ImageCache(provider,
                        new DefaultExpirationPolicy(2));
        cache.setCacheListener(this.statistics);

        final String invalid1 = "invalid1.jpg";
        final String invalid2 = "invalid2.jpg";
        final String valid1 = "valid1.jpg";

        provider.setTimeStamp(1000);
        cache.registerInvalidURI(invalid1);
        provider.setTimeStamp(1100);
        cache.registerInvalidURI(invalid2);

        assertEquals(0, this.statistics.getInvalidHits());

        // not expired, yet
        provider.setTimeStamp(1200);
        assertFalse(cache.isInvalidURI(valid1));
        assertTrue(cache.isInvalidURI(invalid1));
        assertTrue(cache.isInvalidURI(invalid2));
        assertEquals(2, this.statistics.getInvalidHits());

        // first expiration time reached
        provider.setTimeStamp(3050);
        assertFalse(cache.isInvalidURI(valid1));
        assertFalse(cache.isInvalidURI(invalid1));
        assertTrue(cache.isInvalidURI(invalid2));
        assertEquals(3, this.statistics.getInvalidHits());

        // second expiration time reached
        provider.setTimeStamp(3200);
        assertFalse(cache.isInvalidURI(valid1));
        assertFalse(cache.isInvalidURI(invalid1));
        assertFalse(cache.isInvalidURI(invalid2));
        assertEquals(3, this.statistics.getInvalidHits());
    }

    /**
     * Tests the image cache reusing a cacheable Image created by the
             * ImageLoader.
             *
             * @throws Exception
             *             if an error occurs
     */
    public void testImageCache1() throws Exception {
        final String valid1 = "bgimg72dpi.gif";

        final ImageInfo info = this.manager.getImageInfo(valid1,
                        this.sessionContext);
        assertNotNull(info);

        final ImageBuffered img1 = (ImageBuffered) this.manager.getImage(info,
                        ImageFlavor.BUFFERED_IMAGE, this.sessionContext);
        assertNotNull(img1);
        assertNotNull(img1.getBufferedImage());

        final ImageBuffered img2 = (ImageBuffered) this.manager.getImage(info,
                        ImageFlavor.BUFFERED_IMAGE, this.sessionContext);
        // ImageBuffered does not have to be the same instance but we want at
                // least the
        // BufferedImage to be reused.
        assertTrue("BufferedImage must be reused",
                img1.getBufferedImage() == img2.getBufferedImage());

        assertEquals(1, this.statistics.getImageCacheHits());
        assertEquals(1, this.statistics.getImageCacheMisses());
    }

            /**
     * Test to check if doInvalidURIHouseKeeping() throws a
     * ConcurrentModificationException.
     */
    public void testImageCacheHouseKeeping() {
        final ImageCache imageCache = new ImageCache(new TimeStampProvider(),
                new DefaultExpirationPolicy(1));
        imageCache.registerInvalidURI("invalid");
        imageCache.registerInvalidURI("invalid2");
        try {
            Thread.sleep(1200);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        imageCache.doHouseKeeping();
    }
}
