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

/* $Id: AFPGraphicsConfiguration.java 985571 2010-08-14 19:28:26Z jeremias $ */

package org.apache.fop.afp.svg;

import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.svg.GraphicsConfiguration;

/**
 * Our implementation of the class that returns information about roughly what
 * we can handle and want to see (alpha for example).
 */
@Slf4j
public class AFPGraphicsConfiguration extends GraphicsConfiguration {
    // We use this to get a good colormodel..
    private static final BufferedImage BI_WITH_ALPHA = new BufferedImage(1, 1,
            BufferedImage.TYPE_INT_ARGB);
    // We use this to get a good colormodel..
    private static final BufferedImage BI_WITHOUT_ALPHA = new BufferedImage(1,
            1, BufferedImage.TYPE_INT_RGB);

    /**
     * Construct a buffered image with an alpha channel, unless transparencty is
     * OPAQUE (no alpha at all).
     *
     * @param width
     *            the width of the image
     * @param height
     *            the height of the image
     * @param transparency
     *            the alpha value of the image
     * @return the new buffered image
     */
    @Override
    public BufferedImage createCompatibleImage(final int width,
            final int height, final int transparency) {
        if (transparency == Transparency.OPAQUE) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        } else {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
    }

    /**
     * Construct a buffered image with an alpha channel.
     *
     * @param width
     *            the width of the image
     * @param height
     *            the height of the image
     * @return the new buffered image
     */
    @Override
    public BufferedImage createCompatibleImage(final int width, final int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * TODO: This should return the page bounds in Pts, I couldn't figure out
     * how to get this for the current page (this still works for now, but it
     * should be fixed...).
     *
     * @return the bounds of the page
     */
    @Override
    public Rectangle getBounds() {
        return null;
    }

    /**
     * Return a good default color model for this 'device'.
     *
     * @return the colour model for the configuration
     */
    @Override
    public ColorModel getColorModel() {
        return BI_WITH_ALPHA.getColorModel();
    }

    /**
     * Return a good color model given <code>transparency</code>
     *
     * @param transparency
     *            the alpha value for the colour model
     * @return the colour model for the configuration
     */
    @Override
    public ColorModel getColorModel(final int transparency) {
        if (transparency == Transparency.OPAQUE) {
            return BI_WITHOUT_ALPHA.getColorModel();
        } else {
            return BI_WITH_ALPHA.getColorModel();
        }
    }

    private AffineTransform defaultTransform = null;
    private AffineTransform normalizingTransform = null;
    private final GraphicsDevice graphicsDevice = new AFPGraphicsDevice(this);;

    /**
     * The default transform (1:1).
     *
     * @return the default transform for the configuration
     */
    @Override
    public AffineTransform getDefaultTransform() {
        log.debug("getDefaultTransform()");
        if (this.defaultTransform == null) {
            this.defaultTransform = new AffineTransform();
        }
        return this.defaultTransform;
    }

    /**
     * The normalizing transform (1:1) (since we currently render images at
     * 72dpi, which we might want to change in the future).
     *
     * @return the normalizing transform for the configuration
     */
    @Override
    public AffineTransform getNormalizingTransform() {
        log.debug("getNormalizingTransform()");
        if (this.normalizingTransform == null) {
            this.normalizingTransform = new AffineTransform(2, 0, 0, 2, 0, 0);
        }
        return this.normalizingTransform;
    }

    /** {@inheritDoc} */
    @Override
    public GraphicsDevice getDevice() {
        log.debug("getDevice()");
        return this.graphicsDevice;
    }
}
