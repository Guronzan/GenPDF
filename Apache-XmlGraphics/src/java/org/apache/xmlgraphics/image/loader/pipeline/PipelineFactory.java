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

/* $Id: PipelineFactory.java 924666 2010-03-18 08:26:30Z jeremias $ */

package org.apache.xmlgraphics.image.loader.pipeline;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.impl.CompositeImageLoader;
import org.apache.xmlgraphics.image.loader.spi.ImageConverter;
import org.apache.xmlgraphics.image.loader.spi.ImageImplRegistry;
import org.apache.xmlgraphics.image.loader.spi.ImageLoader;
import org.apache.xmlgraphics.image.loader.spi.ImageLoaderFactory;
import org.apache.xmlgraphics.image.loader.util.Penalty;
import org.apache.xmlgraphics.util.dijkstra.DefaultEdgeDirectory;
import org.apache.xmlgraphics.util.dijkstra.DijkstraAlgorithm;
import org.apache.xmlgraphics.util.dijkstra.Vertex;

/**
 * Factory class for image processing pipelines.
 */
@Slf4j
 public class PipelineFactory {

    private final ImageManager manager;

     private int converterEdgeDirectoryVersion = -1;

     /** Holds the EdgeDirectory for all image conversions */
     private DefaultEdgeDirectory converterEdgeDirectory;

     /**
      * Main constructor.
      * 
     * @param manager
     *            the ImageManager instance
      */
     public PipelineFactory(final ImageManager manager) {
         this.manager = manager;
     }

     private DefaultEdgeDirectory getEdgeDirectory() {
         final ImageImplRegistry registry = this.manager.getRegistry();
         if (registry.getImageConverterModifications() != this.converterEdgeDirectoryVersion) {
             final Collection converters = registry.getImageConverters();

             // Rebuild edge directory
             final DefaultEdgeDirectory dir = new DefaultEdgeDirectory();
             final Iterator iter = converters.iterator();
             while (iter.hasNext()) {
                 final ImageConverter converter = (ImageConverter) iter.next();
                 Penalty penalty = Penalty.toPenalty(converter
                        .getConversionPenalty());
                 penalty = penalty.add(registry.getAdditionalPenalty(converter
                        .getClass().getName()));
                 dir.addEdge(new ImageConversionEdge(converter, penalty));
             }

             this.converterEdgeDirectoryVersion = registry
                    .getImageConverterModifications();
             this.converterEdgeDirectory = dir; // Replace (thread-safe)
         }
         return this.converterEdgeDirectory;
     }

     /**
      * Creates and returns an {@link ImageProviderPipeline} that allows to load
     * an image of the given MIME type and present it in the requested image
     * flavor.
     * 
     * @param originalImage
     *            the original image that serves as the origin point of the
     *            conversion
     * @param targetFlavor
     *            the requested image flavor
     * @return an {@link ImageProviderPipeline} or null if no suitable pipeline
     *         could be assembled
      */
     public ImageProviderPipeline newImageConverterPipeline(
            final Image originalImage, final ImageFlavor targetFlavor) {
         // Get snapshot to avoid concurrent modification problems
        // (thread-safety)
         final DefaultEdgeDirectory dir = getEdgeDirectory();
         final ImageRepresentation destination = new ImageRepresentation(
                targetFlavor);
         final ImageProviderPipeline pipeline = findPipeline(dir,
                originalImage.getFlavor(), destination);
         return pipeline;
     }

     /**
      * Creates and returns an {@link ImageProviderPipeline} that allows to load
     * an image of the given MIME type and present it in the requested image
     * flavor.
     * 
     * @param imageInfo
     *            the image info object of the original image
     * @param targetFlavor
     *            the requested image flavor
     * @return an {@link ImageProviderPipeline} or null if no suitable pipeline
     *         could be assembled
      */
     public ImageProviderPipeline newImageConverterPipeline(
            final ImageInfo imageInfo, final ImageFlavor targetFlavor) {
         final ImageProviderPipeline[] candidates = determineCandidatePipelines(
                imageInfo, targetFlavor);

         // Choose best pipeline
         if (candidates.length > 0) {
             Arrays.sort(candidates, new PipelineComparator());
             final ImageProviderPipeline pipeline = candidates[0];
             if (pipeline != null && log.isDebugEnabled()) {
                 log.debug("Pipeline: " + pipeline + " with penalty "
                        + pipeline.getConversionPenalty());
             }
             return pipeline;
         } else {
             return null;
         }
     }

     /**
      * Determines all possible pipelines for the given image that can produce
     * the requested target flavor.
     * 
     * @param imageInfo
     *            the image information
     * @param targetFlavor
     *            the target flavor
     * @return the candidate pipelines
      */
     public ImageProviderPipeline[] determineCandidatePipelines(
            final ImageInfo imageInfo, final ImageFlavor targetFlavor) {
         final String originalMime = imageInfo.getMimeType();
         final ImageImplRegistry registry = this.manager.getRegistry();
         final List candidates = new java.util.ArrayList();

         // Get snapshot to avoid concurrent modification problems
        // (thread-safety)
         final DefaultEdgeDirectory dir = getEdgeDirectory();

         ImageLoaderFactory[] loaderFactories = registry
                .getImageLoaderFactories(imageInfo, targetFlavor);
         if (loaderFactories != null) {
             // Directly load image and return it
             ImageLoader loader;
             if (loaderFactories.length == 1) {
                loader = loaderFactories[0].newImageLoader(targetFlavor);
             } else {
                 final int count = loaderFactories.length;
                 final ImageLoader[] loaders = new ImageLoader[count];
                 for (int i = 0; i < count; i++) {
                     loaders[i] = loaderFactories[i]
                            .newImageLoader(targetFlavor);
                 }
                 loader = new CompositeImageLoader(loaders);
             }
             final ImageProviderPipeline pipeline = new ImageProviderPipeline(
                    this.manager.getCache(), loader);
             candidates.add(pipeline);
         } else {
             // Need to use ImageConverters
             if (log.isTraceEnabled()) {
                 log.trace("No ImageLoaderFactory found that can load this format ("
                         + targetFlavor
                        + ") directly. Trying ImageConverters instead...");
             }

             final ImageRepresentation destination = new ImageRepresentation(
                    targetFlavor);
             // Get Loader for originalMIME
             // --> List of resulting flavors, possibly multiple loaders
             loaderFactories = registry.getImageLoaderFactories(originalMime);
             if (loaderFactories != null) {

                 // Find best pipeline -> best loader
                 for (final ImageLoaderFactory loaderFactory : loaderFactories) {
                     final ImageFlavor[] flavors = loaderFactory
                            .getSupportedFlavors(originalMime);
                     for (final ImageFlavor flavor : flavors) {
                         final ImageProviderPipeline pipeline = findPipeline(
                                dir, flavor, destination);
                         if (pipeline != null) {
                             final ImageLoader loader = loaderFactory
                                    .newImageLoader(flavor);
                             pipeline.setImageLoader(loader);
                             candidates.add(pipeline);
                         }
                     }
                 }
             }
         }
         return (ImageProviderPipeline[]) candidates
                .toArray(new ImageProviderPipeline[candidates.size()]);
     }

     /** Compares two pipelines based on their conversion penalty. */
     private static class PipelineComparator implements Comparator {

         @Override
         public int compare(final Object o1, final Object o2) {
             final ImageProviderPipeline p1 = (ImageProviderPipeline) o1;
             final ImageProviderPipeline p2 = (ImageProviderPipeline) o2;
             // Lowest penalty first
             return p1.getConversionPenalty() - p2.getConversionPenalty();
         }

     }

     private ImageProviderPipeline findPipeline(final DefaultEdgeDirectory dir,
             final ImageFlavor originFlavor,
            final ImageRepresentation destination) {
         final DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(dir);
         final ImageRepresentation origin = new ImageRepresentation(originFlavor);
         dijkstra.execute(origin, destination);
         if (log.isTraceEnabled()) {
             log.trace("Lowest penalty: "
                    + dijkstra.getLowestPenalty(destination));
         }

         Vertex prev = destination;
         Vertex pred = dijkstra.getPredecessor(destination);
         if (pred == null) {
             if (log.isTraceEnabled()) {
                 log.trace("No route found!");
             }
             return null;
         } else {
             final LinkedList stops = new LinkedList();
             while ((pred = dijkstra.getPredecessor(prev)) != null) {
                 final ImageConversionEdge edge = (ImageConversionEdge) dir
                        .getBestEdge(pred, prev);
                 stops.addFirst(edge);
                 prev = pred;
             }
             final ImageProviderPipeline pipeline = new ImageProviderPipeline(
                    this.manager.getCache(), null);
             final Iterator iter = stops.iterator();
             while (iter.hasNext()) {
                 final ImageConversionEdge edge = (ImageConversionEdge) iter
                        .next();
                 pipeline.addConverter(edge.getImageConverter());
             }
             return pipeline;
         }
     }

     /**
      * Finds and returns an array of {@link ImageProviderPipeline} instances
     * which can handle the given MIME type and return one of the given
     * {@link ImageFlavor}s.
      * 
     * @param imageInfo
     *            the image info object
     * @param flavors
     *            the possible target flavors
     * @return an array of pipelines
      */
     public ImageProviderPipeline[] determineCandidatePipelines(
            final ImageInfo imageInfo, final ImageFlavor[] flavors) {
         final List candidates = new java.util.ArrayList();
         final int count = flavors.length;
         for (int i = 0; i < count; i++) {
             // Find the best pipeline for each flavor
             final ImageProviderPipeline pipeline = newImageConverterPipeline(
                    imageInfo, flavors[i]);
             if (pipeline == null) {
                 continue; // No suitable pipeline found for flavor
             }
             final Penalty p = pipeline.getConversionPenalty(this.manager
                    .getRegistry());
             if (!p.isInfinitePenalty()) {
                 candidates.add(pipeline);
             }
         }
         return (ImageProviderPipeline[]) candidates
                .toArray(new ImageProviderPipeline[candidates.size()]);
     }

     /**
      * Finds and returns an array of {@link ImageProviderPipeline} instances
     * which can handle the convert the given {@link Image} and return one of
     * the given {@link ImageFlavor}s.
      * 
     * @param sourceImage
     *            the image to be converted
     * @param flavors
     *            the possible target flavors
     * @return an array of pipelines
      */
     public ImageProviderPipeline[] determineCandidatePipelines(
            final Image sourceImage, final ImageFlavor[] flavors) {
         final List candidates = new java.util.ArrayList();
         final int count = flavors.length;
         for (int i = 0; i < count; i++) {
             // Find the best pipeline for each flavor
             final ImageProviderPipeline pipeline = newImageConverterPipeline(
                    sourceImage, flavors[i]);
             if (pipeline != null) {
                 candidates.add(pipeline);
             }
         }
         return (ImageProviderPipeline[]) candidates
                .toArray(new ImageProviderPipeline[candidates.size()]);
     }

}
