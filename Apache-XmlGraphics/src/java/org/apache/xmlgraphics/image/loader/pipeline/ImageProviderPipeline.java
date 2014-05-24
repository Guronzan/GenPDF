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

/* $Id: ImageProviderPipeline.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.image.loader.pipeline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.cache.ImageCache;
import org.apache.xmlgraphics.image.loader.impl.ImageRawStream;
import org.apache.xmlgraphics.image.loader.spi.ImageConverter;
import org.apache.xmlgraphics.image.loader.spi.ImageImplRegistry;
import org.apache.xmlgraphics.image.loader.spi.ImageLoader;
import org.apache.xmlgraphics.image.loader.util.Penalty;

/**
 * Represents a pipeline of ImageConverters with an ImageLoader at the beginning
 * of the pipeline.
 */
@Slf4j
public class ImageProviderPipeline {

    private final ImageCache cache;
    private ImageLoader loader;
    private final List converters = new java.util.ArrayList();

    /**
     * Main constructor.
     *
     * @param cache
     *            the image cache (may be null if no caching is desired)
     * @param loader
     *            the image loader to drive the pipeline with
     */
    public ImageProviderPipeline(final ImageCache cache,
            final ImageLoader loader) {
        this.cache = cache;
        setImageLoader(loader);
    }

    /**
     * Constructor for operation without caching.
     *
     * @param loader
     *            the image loader to drive the pipeline with
     */
    public ImageProviderPipeline(final ImageLoader loader) {
        this(null, loader);
    }

    /**
     * Default constructor without caching and without an ImageLoader (or the
     * ImageLoader may be set later).
     */
    public ImageProviderPipeline() {
        this(null, null);
    }

    /**
     * Executes the image converter pipeline. First, the image indicated by the
     * ImageInfo instance is loaded through an ImageLoader and then optionally
     * converted by a series of ImageConverters. At the end of the pipeline, the
     * fully loaded and converted image is returned.
     *
     * @param info
     *            the image info object indicating the image to load
     * @param hints
     *            a Map of image conversion hints
     * @param context
     *            the session context
     * @return the requested image
     * @throws ImageException
     *             if an error occurs while loader or converting the image
     * @throws IOException
     *             if an I/O error occurs
     */
    public Image execute(final ImageInfo info, final Map hints,
            final ImageSessionContext context) throws ImageException,
            IOException {
        return execute(info, null, hints, context);
    }

    /**
     * Executes the image converter pipeline. First, the image indicated by the
     * ImageInfo instance is loaded through an ImageLoader and then optionally
     * converted by a series of ImageConverters. At the end of the pipeline, the
     * fully loaded and converted image is returned.
     *
     * @param info
     *            the image info object indicating the image to load
     * @param originalImage
     *            the original image to start the pipeline off or null if an
     *            ImageLoader is used
     * @param hints
     *            a Map of image conversion hints
     * @param context
     *            the session context
     * @return the requested image
     * @throws ImageException
     *             if an error occurs while loader or converting the image
     * @throws IOException
     *             if an I/O error occurs
     */
    public Image execute(final ImageInfo info, final Image originalImage,
            Map hints, final ImageSessionContext context)
                    throws ImageException, IOException {
        if (hints == null) {
            hints = Collections.EMPTY_MAP;
        }
        long start = System.currentTimeMillis();
        Image img = null;

        // Remember the last image in the pipeline that is cacheable and cache
        // that.
        Image lastCacheableImage = null;

        final int converterCount = this.converters.size();
        int startingPoint = 0;
        if (this.cache != null) {
            for (int i = converterCount - 1; i >= 0; i--) {
                final ImageConverter converter = getConverter(i);
                final ImageFlavor flavor = converter.getTargetFlavor();
                img = this.cache.getImage(info, flavor);
                if (img != null) {
                    startingPoint = i + 1;
                    break;
                }
            }

            if (img == null && this.loader != null) {
                // try target flavor of loader from cache
                final ImageFlavor flavor = this.loader.getTargetFlavor();
                img = this.cache.getImage(info, flavor);
            }
        }
        if (img == null && originalImage != null) {
            img = originalImage;
        }

        boolean entirelyInCache = true;
        long duration;
        if (img == null && this.loader != null) {
            // Load image
            img = this.loader.loadImage(info, hints, context);
            if (log.isTraceEnabled()) {
                duration = System.currentTimeMillis() - start;
                log.trace("Image loading using " + this.loader + " took "
                        + duration + " ms.");
            }

            // Caching
            entirelyInCache = false;
            if (img.isCacheable()) {
                lastCacheableImage = img;
            }
        }
        if (img == null) {
            throw new ImageException(
                    "Pipeline fails. No ImageLoader and no original Image available.");
        }

        if (converterCount > 0) {
            for (int i = startingPoint; i < converterCount; i++) {
                final ImageConverter converter = getConverter(i);
                start = System.currentTimeMillis();
                img = converter.convert(img, hints);
                if (log.isTraceEnabled()) {
                    duration = System.currentTimeMillis() - start;
                    log.trace("Image conversion using " + converter + " took "
                            + duration + " ms.");
                }

                // Caching
                entirelyInCache = false;
                if (img.isCacheable()) {
                    lastCacheableImage = img;
                }
            }
        }

        // Note: Currently we just cache the end result of the pipeline, not all
        // intermediate
        // results as it is expected that the cache hit ration would be rather
        // small.
        if (this.cache != null && !entirelyInCache) {
            if (lastCacheableImage == null) {
                // Try to make the Image cacheable
                lastCacheableImage = forceCaching(img);
            }
            if (lastCacheableImage != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Caching image: " + lastCacheableImage);
                }
                this.cache.putImage(lastCacheableImage);
            }
        }
        return img;
    }

    private ImageConverter getConverter(final int index) {
        return (ImageConverter) this.converters.get(index);
    }

    /**
     * In some cases the provided Image is not cacheable, nor is any of the
     * intermediate Image instances (for example, when loading a raw JPEG file).
     * If the image is loaded over a potentially slow network, it is preferrable
     * to download the whole file and cache it in memory or in a temporary file.
     * It's not always possible to convert an Image into a cacheable variant.
     *
     * @param img
     *            the Image to investigate
     * @return the converted, cacheable Image or null if the Image cannot be
     *         converted
     * @throws IOException
     *             if an I/O error occurs
     */
    protected Image forceCaching(final Image img) throws IOException {
        if (img instanceof ImageRawStream) {
            final ImageRawStream raw = (ImageRawStream) img;
            if (log.isDebugEnabled()) {
                log.debug("Image is made cacheable: " + img.getInfo());
            }

            // Read the whole stream and hold it in memory so the image can be
            // cached
            final ByteArrayOutputStream baout = new ByteArrayOutputStream();
            final InputStream in = raw.createInputStream();
            try {
                IOUtils.copy(in, baout);
            } finally {
                IOUtils.closeQuietly(in);
            }
            final byte[] data = baout.toByteArray();
            raw.setInputStreamFactory(new ImageRawStream.ByteArrayStreamFactory(
                    data));
            return raw;
        }
        return null;
    }

    /**
     * Sets the ImageLoader that is used at the beginning of the pipeline if the
     * image is not loaded, yet.
     *
     * @param imageLoader
     *            the image loader implementation
     */
    public void setImageLoader(final ImageLoader imageLoader) {
        this.loader = imageLoader;
    }

    /**
     * Adds an additional ImageConverter to the end of the pipeline.
     *
     * @param converter
     *            the ImageConverter instance
     */
    public void addConverter(final ImageConverter converter) {
        // TODO check for compatibility
        this.converters.add(converter);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("Loader: ").append(this.loader);
        if (this.converters.size() > 0) {
            sb.append(" Converters: ");
            sb.append(this.converters);
        }
        return sb.toString();
    }

    /**
     * Returns the overall conversion penalty for the pipeline. This can be used
     * to choose among different possible pipelines.
     *
     * @return the overall penalty (a non-negative integer)
     */
    public int getConversionPenalty() {
        return getConversionPenalty(null).getValue();
    }

    /**
     * Returns the overall conversion penalty for the pipeline. This can be used
     * to choose among different possible pipelines.
     *
     * @param registry
     *            the image implementation registry
     * @return the overall penalty (a non-negative integer)
     */
    public Penalty getConversionPenalty(final ImageImplRegistry registry) {
        Penalty penalty = Penalty.ZERO_PENALTY;
        if (this.loader != null) {
            penalty = penalty.add(this.loader.getUsagePenalty());
            if (registry != null) {
                penalty = penalty.add(registry.getAdditionalPenalty(this.loader
                        .getClass().getName()));
            }
        }
        final Iterator iter = this.converters.iterator();
        while (iter.hasNext()) {
            final ImageConverter converter = (ImageConverter) iter.next();
            penalty = penalty.add(converter.getConversionPenalty());
            if (registry != null) {
                penalty = penalty.add(registry.getAdditionalPenalty(converter
                        .getClass().getName()));
            }
        }
        return penalty;
    }

    /**
     * Returns the target flavor generated by this pipeline.
     *
     * @return the target flavor
     */
    public ImageFlavor getTargetFlavor() {
        if (this.converters.size() > 0) {
            return getConverter(this.converters.size() - 1).getTargetFlavor();
        } else if (this.loader != null) {
            return this.loader.getTargetFlavor();
        } else {
            return null;
        }
    }

}
