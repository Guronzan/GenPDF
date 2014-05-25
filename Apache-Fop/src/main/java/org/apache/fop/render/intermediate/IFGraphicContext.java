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

/* $Id: IFGraphicContext.java 1311120 2012-04-08 23:48:11Z gadams $ */

package org.apache.fop.render.intermediate;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import org.apache.xmlgraphics.java2d.GraphicContext;

/**
 * Specialized graphic context class for the intermediate format renderer.
 */
public class IFGraphicContext extends GraphicContext {

    private static final AffineTransform[] EMPTY_TRANSFORM_ARRAY = new AffineTransform[0];

    private final ArrayList groupList = new ArrayList();

    /**
     * Default constructor.
     */
    public IFGraphicContext() {
        super();
    }

    /**
     * Copy constructor.
     * 
     * @param graphicContext
     *            the graphic context to make a copy of
     */
    protected IFGraphicContext(final IFGraphicContext graphicContext) {
        super(graphicContext);
        // N.B. do not perform deep copy on groupList; doing so causes
        // a junit regression... have not investigated cause... [GA]
        // groupList = (ArrayList) graphicContext.groupList.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        return new IFGraphicContext(this);
    }

    /**
     * @param group
     *            a group
     */
    public void pushGroup(final Group group) {
        this.groupList.add(group);
        for (int i = 0, c = group.getTransforms().length; i < c; i++) {
            transform(group.getTransforms()[i]);
        }
    }

    /** @return array of groups */
    public Group[] getGroups() {
        return (Group[]) this.groupList.toArray(new Group[getGroupStackSize()]);
    }

    /** @return array of groups after clearing group list */
    public Group[] dropGroups() {
        final Group[] groups = getGroups();
        this.groupList.clear();
        return groups;
    }

    /** @return size of group list */
    public int getGroupStackSize() {
        return this.groupList.size();
    }

    /** a group */
    public static class Group {

        private final AffineTransform[] transforms;

        /**
         * Construct a Group.
         * 
         * @param transforms
         *            an array of transforms
         */
        public Group(final AffineTransform[] transforms) {
            this.transforms = transforms;
        }

        /**
         * Construct a Group.
         * 
         * @param transform
         *            a transform
         */
        public Group(final AffineTransform transform) {
            this(new AffineTransform[] { transform });
        }

        /** Default constructor. */
        public Group() {
            this(EMPTY_TRANSFORM_ARRAY);
        }

        /** @return array of transforms */
        public AffineTransform[] getTransforms() {
            return this.transforms;
        }

        /**
         * @param painter
         *            a painter
         * @throws IFException
         *             in not caught
         */
        public void start(final IFPainter painter) throws IFException {
            painter.startGroup(this.transforms);
        }

        /**
         * @param painter
         *            a painter
         * @throws IFException
         *             in not caught
         */
        public void end(final IFPainter painter) throws IFException {
            painter.endGroup();
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("group: ");
            IFUtil.toString(getTransforms(), sb);
            return sb.toString();
        }

    }

    /** a viewport */
    public static class Viewport extends Group {

        private final Dimension size;
        private final Rectangle clipRect;

        /**
         * Construct a viewport.
         * 
         * @param transforms
         *            an array of transforms
         * @param size
         *            a dimension
         * @param clipRect
         *            a clip rectangle
         */
        public Viewport(final AffineTransform[] transforms,
                final Dimension size, final Rectangle clipRect) {
            super(transforms);
            this.size = size;
            this.clipRect = clipRect;
        }

        /**
         * Construct a viewport.
         * 
         * @param transform
         *            a transform
         * @param size
         *            a dimension
         * @param clipRect
         *            a clip rectangle
         */
        public Viewport(final AffineTransform transform, final Dimension size,
                final Rectangle clipRect) {
            this(new AffineTransform[] { transform }, size, clipRect);
        }

        /** @return the viewport's size */
        public Dimension getSize() {
            return this.size;
        }

        /** @return the clip rectangle */
        public Rectangle getClipRect() {
            return this.clipRect;
        }

        /** {@inheritDoc} */
        @Override
        public void start(final IFPainter painter) throws IFException {
            painter.startViewport(getTransforms(), this.size, this.clipRect);
        }

        /** {@inheritDoc} */
        @Override
        public void end(final IFPainter painter) throws IFException {
            painter.endViewport();
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("viewport: ");
            IFUtil.toString(getTransforms(), sb);
            sb.append(", ").append(getSize());
            if (getClipRect() != null) {
                sb.append(", ").append(getClipRect());
            }
            return sb.toString();
        }

    }

}