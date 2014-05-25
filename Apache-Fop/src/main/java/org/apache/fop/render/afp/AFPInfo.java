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

/* $Id: AFPInfo.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.render.afp;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.fop.afp.AFPGraphics2D;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPResourceInfo;
import org.apache.fop.afp.AFPResourceManager;
import org.apache.fop.fonts.FontInfo;

/**
 * AFP information structure for drawing the XML document.
 */
public final class AFPInfo {
    /** see WIDTH */
    private int width;

    /** see HEIGHT */
    private int height;

    /** see XPOS */
    private int x;

    /** see YPOS */
    private int y;

    /** see HANDLER_CONFIGURATION */
    private Configuration handlerConfiguration;

    /** see AFP_FONT_INFO */
    private FontInfo fontInfo;

    /** See AFP_PAINTING_STATE */
    private AFPPaintingState paintingState;

    /** See AFP_RESOURCE_MANAGER */
    private AFPResourceManager resourceManager;

    /** See AFP_RESOURCE_INFO */
    private AFPResourceInfo resourceInfo;

    /** true if SVG should be rendered as a bitmap instead of natively */
    private boolean paintAsBitmap;

    /**
     * Returns the width.
     *
     * @return the width
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Sets the width.
     *
     * @param width
     *            The pageWidth to set
     */
    public void setWidth(final int width) {
        this.width = width;
    }

    /**
     * Returns the height.
     *
     * @return the height
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Sets the height.
     *
     * @param height
     *            The height to set
     */
    public void setHeight(final int height) {
        this.height = height;
    }

    /**
     * Returns the handler configuration
     *
     * @return the handler configuration
     */
    public Configuration getHandlerConfiguration() {
        return this.handlerConfiguration;
    }

    /**
     * Sets the handler configuration
     *
     * @param cfg
     *            the handler configuration
     */
    public void setHandlerConfiguration(final Configuration cfg) {
        this.handlerConfiguration = cfg;
    }

    /**
     * Return the font info
     *
     * @return the font info
     */
    public FontInfo getFontInfo() {
        return this.fontInfo;
    }

    /**
     * Returns the current AFP state
     *
     * @return the current AFP state
     */
    public AFPPaintingState getPaintingState() {
        return this.paintingState;
    }

    /**
     * Returns the AFPResourceManager
     *
     * @return the AFPResourceManager
     */
    public AFPResourceManager getResourceManager() {
        return this.resourceManager;
    }

    /**
     * Returns true if supports color
     *
     * @return true if supports color
     */
    public boolean isColorSupported() {
        return getPaintingState().isColorImages();
    }

    /**
     * Returns the current x position coordinate
     *
     * @return the current x position coordinate
     */
    protected int getX() {
        return this.x;
    }

    /**
     * Returns the current y position coordinate
     *
     * @return the current y position coordinate
     */
    protected int getY() {
        return this.y;
    }

    /**
     * Returns the resolution
     *
     * @return the resolution
     */
    protected int getResolution() {
        return getPaintingState().getResolution();
    }

    /**
     * Returns the number of bits per pixel to use
     * 
     * @return the number of bits per pixel to use
     */
    protected int getBitsPerPixel() {
        return getPaintingState().getBitsPerPixel();
    }

    /**
     * Sets the current x position coordinate
     *
     * @param x
     *            the current x position coordinate
     */
    protected void setX(final int x) {
        this.x = x;
    }

    /**
     * Sets the current y position coordinate
     *
     * @param y
     *            the current y position coordinate
     */
    protected void setY(final int y) {
        this.y = y;
    }

    /**
     * Sets the current font info
     *
     * @param fontInfo
     *            the current font info
     */
    protected void setFontInfo(final FontInfo fontInfo) {
        this.fontInfo = fontInfo;
    }

    /**
     * Sets the AFP state
     *
     * @param paintingState
     *            the AFP state
     */
    public void setPaintingState(final AFPPaintingState paintingState) {
        this.paintingState = paintingState;
    }

    /**
     * Sets the AFPResourceManager
     *
     * @param resourceManager
     *            the AFPResourceManager
     */
    public void setResourceManager(final AFPResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Sets true if SVG should be rendered as a bitmap instead of natively
     *
     * @param b
     *            boolean value
     */
    public void setPaintAsBitmap(final boolean b) {
        this.paintAsBitmap = b;
    }

    /**
     * Returns true if SVG should be rendered as a bitmap instead of natively
     *
     * @return true if SVG should be rendered as a bitmap instead of natively
     */
    public boolean paintAsBitmap() {
        return this.paintAsBitmap;
    }

    /**
     * Returns true if text should be stroked when painted
     *
     * @return true if text should be stroked when painted
     */
    public boolean strokeText() {
        boolean strokeText = false;
        if (this.handlerConfiguration != null) {
            strokeText = this.handlerConfiguration
                    .getChild("stroke-text", true)
                    .getValueAsBoolean(strokeText);
        }
        return strokeText;
    }

    /**
     * Sets the resource information
     *
     * @param resourceInfo
     *            the resource information
     */
    public void setResourceInfo(final AFPResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    /**
     * Returns the resource information
     *
     * @return the resource information
     */
    public AFPResourceInfo getResourceInfo() {
        return this.resourceInfo;
    }

    /**
     * Creates an AFPGraphics2D implementation
     *
     * @param textAsShapes
     *            true when text is painted as shapes
     * @return a newly created AFPGraphics2D
     */
    public AFPGraphics2D createGraphics2D(final boolean textAsShapes) {
        final AFPGraphics2D g2d = new AFPGraphics2D(textAsShapes,
                this.paintingState, this.resourceManager, this.resourceInfo,
                this.fontInfo);
        g2d.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());
        return g2d;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AFPInfo{width=" + this.width + ", height=" + this.height
                + ", x=" + this.x + ", y=" + this.y + ", cfg="
                + this.handlerConfiguration + ", fontInfo=" + this.fontInfo
                + ", resourceManager=" + this.resourceManager
                + ", paintingState=" + this.paintingState + ", paintAsBitmap="
                + this.paintAsBitmap + ", resourceInfo=" + this.resourceInfo
                + "}";
    }

}
