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

/* $Id: PDFGraphicsDevice.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.svg;

import java.awt.GraphicsConfigTemplate;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;

/**
 * This implements the GraphicsDevice interface as appropriate for a
 * PDFGraphics2D. This is quite simple since we only have one
 * GraphicsConfiguration for now (this might change in the future I suppose).
 */
class PDFGraphicsDevice extends GraphicsDevice {

    /**
     * The Graphics Config that created us...
     */
    protected GraphicsConfiguration gc;

    /**
     * Create a new PDF graphics device.
     *
     * @param gc
     *            The graphics configuration we should reference
     */
    PDFGraphicsDevice(final PDFGraphicsConfiguration gc) {
        this.gc = gc;
    }

    /**
     * Ignore template and return the only config we have
     *
     * @param gct
     *            the template configuration
     * @return the best configuration which is the only one
     */
    @Override
    public GraphicsConfiguration getBestConfiguration(
            final GraphicsConfigTemplate gct) {
        return this.gc;
    }

    /**
     * Return an array of our one GraphicsConfig
     *
     * @return an array containing the one graphics configuration
     */
    @Override
    public GraphicsConfiguration[] getConfigurations() {
        return new GraphicsConfiguration[] { this.gc };
    }

    /**
     * Return out sole GraphicsConfig.
     *
     * @return the graphics configuration that created this object
     */
    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        return this.gc;
    }

    /**
     * Generate an IdString..
     *
     * @return the ID string for this device, uses toString
     */
    @Override
    public String getIDstring() {
        return toString();
    }

    /**
     * Let the caller know that we are "a printer"
     *
     * @return the type which is always printer
     */
    @Override
    public int getType() {
        return GraphicsDevice.TYPE_PRINTER;
    }

}
