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

/* $Id: ImageImplRegistry.java 1311364 2012-04-09 18:22:43Z gadams $ */

package org.apache.xmlgraphics.image.loader.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.util.Penalty;
import org.apache.xmlgraphics.util.Service;

/**
 * This class is the registry for all implementations of the various service
 * provider interfaces for the image package.
 */
@Slf4j
public class ImageImplRegistry {

    /**
     * Infinite penalty value which shall force any implementation to become
     * ineligible.
     */
    public static final int INFINITE_PENALTY = Integer.MAX_VALUE;

    /** Holds the list of preloaders */
    private final List preloaders = new java.util.ArrayList();
    // Content: List<ImagePreloader>
    private int lastPreloaderIdentifier;
    private int lastPreloaderSort;

    /** Holds the list of ImageLoaderFactories */
    private final Map loaders = new java.util.HashMap();
    // Content: Map<String,Map<ImageFlavor,ImageLoaderFactory>>

    /** Holds the list of ImageConverters */
    private final List converters = new java.util.ArrayList();
    // Content: List<ImageConverter>

    private int converterModifications;

    /**
     * A Map (key: implementation classes) with additional penalties to
     * fine-tune the registry.
     */
    private final Map additionalPenalties = new java.util.HashMap(); // <String,
    // Penalty>
    // Note: String as key chosen to avoid possible class-unloading leaks

    /** Singleton instance */
    private static ImageImplRegistry defaultInstance;

    /**
     * Main constructor. This constructor allows to disable plug-in discovery
     * for testing purposes.
     *
     * @param discover
     *            true if implementation classes shall automatically be
     *            discovered.
     */
    public ImageImplRegistry(final boolean discover) {
        if (discover) {
            discoverClasspathImplementations();
        }
    }

    /**
     * Main constructor.
     *
     * @see #getDefaultInstance()
     */
    public ImageImplRegistry() {
        this(true);
    }

    /**
     * Returns the default instance of the Image implementation registry.
     *
     * @return the default instance
     */
    public static ImageImplRegistry getDefaultInstance() {
        if (defaultInstance == null) {
            defaultInstance = new ImageImplRegistry();
        }
        return defaultInstance;
    }

    /**
     * Discovers all implementations in the application's classpath.
     */
    public void discoverClasspathImplementations() {
        // Dynamic registration of ImagePreloaders
        Iterator iter = Service.providers(ImagePreloader.class);
        while (iter.hasNext()) {
            registerPreloader((ImagePreloader) iter.next());
        }

        // Dynamic registration of ImageLoaderFactories
        iter = Service.providers(ImageLoaderFactory.class);
        while (iter.hasNext()) {
            registerLoaderFactory((ImageLoaderFactory) iter.next());
        }

        // Dynamic registration of ImageConverters
        iter = Service.providers(ImageConverter.class);
        while (iter.hasNext()) {
            registerConverter((ImageConverter) iter.next());
        }
    }

    /**
     * Registers a new ImagePreloader.
     *
     * @param preloader
     *            An ImagePreloader instance
     */
    public void registerPreloader(final ImagePreloader preloader) {
        if (log.isDebugEnabled()) {
            log.debug("Registered " + preloader.getClass().getName()
                    + " with priority " + preloader.getPriority());
        }
        this.preloaders.add(newPreloaderHolder(preloader));
    }

    private synchronized PreloaderHolder newPreloaderHolder(
            final ImagePreloader preloader) {
        final PreloaderHolder holder = new PreloaderHolder();
        holder.preloader = preloader;
        holder.identifier = ++this.lastPreloaderIdentifier;
        return holder;
    }

    /** Holder class for registered {@link ImagePreloader} instances. */
    private static class PreloaderHolder {
        private ImagePreloader preloader;
        private int identifier;

        @Override
        public String toString() {
            return this.preloader + " " + this.identifier;
        }
    }

    private synchronized void sortPreloaders() {
        if (this.lastPreloaderIdentifier != this.lastPreloaderSort) {
            Collections.sort(this.preloaders, new Comparator() {

                @Override
                public int compare(final Object o1, final Object o2) {
                    final PreloaderHolder h1 = (PreloaderHolder) o1;
                    long p1 = h1.preloader.getPriority();
                    p1 += getAdditionalPenalty(
                            h1.preloader.getClass().getName()).getValue();

                    final PreloaderHolder h2 = (PreloaderHolder) o2;
                    int p2 = h2.preloader.getPriority();
                    p2 += getAdditionalPenalty(
                            h2.preloader.getClass().getName()).getValue();

                    int diff = Penalty.truncate(p1 - p2);
                    if (diff != 0) {
                        return diff;
                    } else {
                        diff = h1.identifier - h2.identifier;
                        return diff;
                    }
                }

            });
            this.lastPreloaderSort = this.lastPreloaderIdentifier;
        }
    }

    /**
     * Registers a new ImageLoaderFactory.
     *
     * @param loaderFactory
     *            An ImageLoaderFactory instance
     */
    public void registerLoaderFactory(final ImageLoaderFactory loaderFactory) {
        if (!loaderFactory.isAvailable()) {
            if (log.isDebugEnabled()) {
                log.debug("ImageLoaderFactory reports not available: "
                        + loaderFactory.getClass().getName());
            }
            return;
        }
        final String[] mimes = loaderFactory.getSupportedMIMETypes();
        for (final String mime : mimes) {
            synchronized (this.loaders) {
                Map flavorMap = (Map) this.loaders.get(mime);
                if (flavorMap == null) {
                    flavorMap = new java.util.HashMap();
                    this.loaders.put(mime, flavorMap);
                }

                final ImageFlavor[] flavors = loaderFactory
                        .getSupportedFlavors(mime);
                for (final ImageFlavor flavor : flavors) {
                    List factoryList = (List) flavorMap.get(flavor);
                    if (factoryList == null) {
                        factoryList = new java.util.ArrayList();
                        flavorMap.put(flavor, factoryList);
                    }
                    factoryList.add(loaderFactory);

                    if (log.isDebugEnabled()) {
                        log.debug("Registered "
                                + loaderFactory.getClass().getName()
                                + ": MIME = " + mime + ", Flavor = " + flavor);
                    }
                }
            }
        }
    }

    /**
     * Returns the Collection of registered ImageConverter instances.
     *
     * @return a Collection<ImageConverter>
     */
    public Collection getImageConverters() {
        return Collections.unmodifiableList(this.converters);
    }

    /**
     * Returns the number of modifications to the collection of registered
     * ImageConverter instances. This is used to detect changes in the registry
     * concerning ImageConverters.
     *
     * @return the number of modifications
     */
    public int getImageConverterModifications() {
        return this.converterModifications;
    }

    /**
     * Registers a new ImageConverter.
     *
     * @param converter
     *            An ImageConverter instance
     */
    public void registerConverter(final ImageConverter converter) {
        this.converters.add(converter);
        this.converterModifications++;
        if (log.isDebugEnabled()) {
            log.debug("Registered: " + converter.getClass().getName());
        }
    }

    /**
     * Returns an iterator over all registered ImagePreloader instances.
     *
     * @return an iterator over ImagePreloader instances.
     */
    public Iterator getPreloaderIterator() {
        sortPreloaders();
        final Iterator iter = this.preloaders.iterator();
        // Unpack the holders
        return new Iterator() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Object next() {
                final Object obj = iter.next();
                if (obj != null) {
                    return ((PreloaderHolder) obj).preloader;
                } else {
                    return null;
                }
            }

            @Override
            public void remove() {
                iter.remove();
            }

        };
    }

    /**
     * Returns the best ImageLoaderFactory supporting the {@link ImageInfo} and
     * image flavor. If there are multiple ImageLoaderFactories the one with the
     * least usage penalty is selected.
     *
     * @param imageInfo
     *            the image info object
     * @param flavor
     *            the image flavor.
     * @return an ImageLoaderFactory instance or null, if no suitable
     *         implementation was found
     */
    public ImageLoaderFactory getImageLoaderFactory(final ImageInfo imageInfo,
            final ImageFlavor flavor) {
        final String mime = imageInfo.getMimeType();
        final Map flavorMap = (Map) this.loaders.get(mime);
        if (flavorMap != null) {
            final List factoryList = (List) flavorMap.get(flavor);
            if (factoryList != null && factoryList.size() > 0) {
                final Iterator iter = factoryList.iterator();
                int bestPenalty = Integer.MAX_VALUE;
                ImageLoaderFactory bestFactory = null;
                while (iter.hasNext()) {
                    final ImageLoaderFactory factory = (ImageLoaderFactory) iter
                            .next();
                    if (!factory.isSupported(imageInfo)) {
                        continue;
                    }
                    final ImageLoader loader = factory.newImageLoader(flavor);
                    final int penalty = loader.getUsagePenalty();
                    if (penalty < bestPenalty) {
                        bestPenalty = penalty;
                        bestFactory = factory;
                    }
                }
                return bestFactory;
            }
        }
        return null;
    }

    /**
     * Returns an array of {@link ImageLoaderFactory} instances that support the
     * MIME type indicated by an {@link ImageInfo} object and can generate the
     * given image flavor.
     *
     * @param imageInfo
     *            the image info object
     * @param flavor
     *            the target image flavor
     * @return the array of image loader factories
     */
    public ImageLoaderFactory[] getImageLoaderFactories(
            final ImageInfo imageInfo, final ImageFlavor flavor) {
        final String mime = imageInfo.getMimeType();
        final Collection matches = new java.util.TreeSet(
                new ImageLoaderFactoryComparator(flavor));
        final Map flavorMap = (Map) this.loaders.get(mime);
        if (flavorMap != null) {
            final Iterator flavorIter = flavorMap.keySet().iterator();
            while (flavorIter.hasNext()) {
                final ImageFlavor checkFlavor = (ImageFlavor) flavorIter.next();
                if (checkFlavor.isCompatible(flavor)) {
                    final List factoryList = (List) flavorMap.get(checkFlavor);
                    if (factoryList != null && factoryList.size() > 0) {
                        final Iterator factoryIter = factoryList.iterator();
                        while (factoryIter.hasNext()) {
                            final ImageLoaderFactory factory = (ImageLoaderFactory) factoryIter
                                    .next();
                            if (factory.isSupported(imageInfo)) {
                                matches.add(factory);
                            }
                        }
                    }
                }
            }
        }
        if (matches.size() == 0) {
            return null;
        } else {
            return (ImageLoaderFactory[]) matches
                    .toArray(new ImageLoaderFactory[matches.size()]);
        }
    }

    /** Comparator for {@link ImageLoaderFactory} classes. */
    private class ImageLoaderFactoryComparator implements Comparator {

        private final ImageFlavor targetFlavor;

        public ImageLoaderFactoryComparator(final ImageFlavor targetFlavor) {
            this.targetFlavor = targetFlavor;
        }

        @Override
        public int compare(final Object o1, final Object o2) {
            final ImageLoaderFactory f1 = (ImageLoaderFactory) o1;
            final ImageLoader l1 = f1.newImageLoader(this.targetFlavor);
            long p1 = l1.getUsagePenalty();
            p1 += getAdditionalPenalty(l1.getClass().getName()).getValue();

            final ImageLoaderFactory f2 = (ImageLoaderFactory) o2;
            final ImageLoader l2 = f2.newImageLoader(this.targetFlavor);
            long p2 = l2.getUsagePenalty();
            p2 = getAdditionalPenalty(l2.getClass().getName()).getValue();

            // Lowest penalty first
            return Penalty.truncate(p1 - p2);
        }

    }

    /**
     * Returns an array of ImageLoaderFactory instances which support the given
     * MIME type. The instances are returned in no particular order.
     *
     * @param mime
     *            the MIME type to find ImageLoaderFactories for
     * @return the array of ImageLoaderFactory instances
     */
    public ImageLoaderFactory[] getImageLoaderFactories(final String mime) {
        final Map flavorMap = (Map) this.loaders.get(mime);
        if (flavorMap != null) {
            final Set factories = new java.util.HashSet();
            final Iterator iter = flavorMap.values().iterator();
            while (iter.hasNext()) {
                final List factoryList = (List) iter.next();
                factories.addAll(factoryList);
            }
            final int factoryCount = factories.size();
            if (factoryCount > 0) {
                return (ImageLoaderFactory[]) factories
                        .toArray(new ImageLoaderFactory[factoryCount]);
            }
        }
        return null;
    }

    /**
     * Sets an additional penalty for a particular implementation class for any
     * of the interface administered by this registry class. No checking is
     * performed to verify if the className parameter is valid.
     *
     * @param className
     *            the fully qualified class name of the implementation class
     * @param penalty
     *            the additional penalty or null to clear any existing value
     */
    public void setAdditionalPenalty(final String className,
            final Penalty penalty) {
        if (penalty != null) {
            this.additionalPenalties.put(className, penalty);
        } else {
            this.additionalPenalties.remove(className);
        }
        this.lastPreloaderSort = -1; // Force resort, just in case this was a
        // preloader
    }

    /**
     * Returns the additional penalty value set for a particular implementation
     * class. If no such value is set, 0 is returned.
     *
     * @param className
     *            the fully qualified class name of the implementation class
     * @return the additional penalty value
     */
    public Penalty getAdditionalPenalty(final String className) {
        final Penalty p = (Penalty) this.additionalPenalties.get(className);
        return p != null ? p : Penalty.ZERO_PENALTY;
    }

}
