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

/* $Id: PSImageUtils.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.ps;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.apache.fop.render.ImageHandlerRegistry;
import org.apache.fop.render.RenderingContext;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.pipeline.ImageProviderPipeline;
import org.apache.xmlgraphics.ps.DSCConstants;
import org.apache.xmlgraphics.ps.PSGenerator;
import org.apache.xmlgraphics.ps.PSResource;

/**
 * Utility code for rendering images in PostScript.
 */
public class PSImageUtils extends org.apache.xmlgraphics.ps.PSImageUtils {

    /**
     * Indicates whether the given image (identified by an {@link ImageInfo}
     * object) shall be inlined rather than generated as a PostScript form.
     * 
     * @param info
     *            the info object for the image
     * @param renderingContext
     *            the rendering context
     * @return true if the image shall be inlined, false if forms shall be used.
     */
    public static boolean isImageInlined(final ImageInfo info,
            final PSRenderingContext renderingContext) {
        final String uri = info.getOriginalURI();
        if (uri == null || "".equals(uri)) {
            return true;
        }
        // Investigate choice for inline mode
        final ImageFlavor[] inlineFlavors = determineSupportedImageFlavors(renderingContext);
        final ImageManager manager = renderingContext.getUserAgent()
                .getFactory().getImageManager();
        final ImageProviderPipeline[] inlineCandidates = manager
                .getPipelineFactory().determineCandidatePipelines(info,
                        inlineFlavors);
        final ImageProviderPipeline inlineChoice = manager
                .choosePipeline(inlineCandidates);
        final ImageFlavor inlineFlavor = inlineChoice != null ? inlineChoice
                .getTargetFlavor() : null;

                // Create a rendering context for form creation
                final PSRenderingContext formContext = renderingContext.toFormContext();

                // Investigate choice for form mode
                final ImageFlavor[] formFlavors = determineSupportedImageFlavors(formContext);
                final ImageProviderPipeline[] formCandidates = manager
                .getPipelineFactory().determineCandidatePipelines(info,
                        formFlavors);
                final ImageProviderPipeline formChoice = manager
                .choosePipeline(formCandidates);
                final ImageFlavor formFlavor = formChoice != null ? formChoice
                .getTargetFlavor() : null;

                // Inline if form is not supported or if a better choice is available
        // with inline mode
                return formFlavor == null || !formFlavor.equals(inlineFlavor);
    }

    private static ImageFlavor[] determineSupportedImageFlavors(
            final RenderingContext renderingContext) {
        ImageFlavor[] inlineFlavors;
        final ImageHandlerRegistry imageHandlerRegistry = renderingContext
                .getUserAgent().getFactory().getImageHandlerRegistry();
        inlineFlavors = imageHandlerRegistry
                .getSupportedFlavors(renderingContext);
        return inlineFlavors;
    }

    /**
     * Draws a form at a given location.
     * 
     * @param form
     *            the form resource
     * @param info
     *            the image info object representing the image in the form
     * @param rect
     *            the target rectangle (coordinates in millipoints)
     * @param generator
     *            the PostScript generator
     * @throws IOException
     *             if an I/O error occurs
     */
    public static void drawForm(final PSResource form, final ImageInfo info,
            final Rectangle rect, final PSGenerator generator)
            throws IOException {
        final Rectangle2D targetRect = new Rectangle2D.Double(
                rect.getMinX() / 1000.0, rect.getMinY() / 1000.0,
                rect.getWidth() / 1000.0, rect.getHeight() / 1000.0);
        generator.saveGraphicsState();
        translateAndScale(generator, info.getSize().getDimensionPt(),
                targetRect);

        // The following %%IncludeResource marker is needed later by
        // ResourceHandler!
        generator.writeDSCComment(DSCConstants.INCLUDE_RESOURCE, form);
        generator.getResourceTracker().notifyResourceUsageOnPage(form);

        generator.writeln(form.getName() + " execform");
        generator.restoreGraphicsState();
    }

}
