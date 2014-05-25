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

/* $Id: AffineTransformArrayParser.java 820672 2009-10-01 14:48:27Z jeremias $ */

package org.apache.fop.render.intermediate;

import java.awt.geom.AffineTransform;
import java.io.Reader;
import java.util.List;

import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.TransformListHandler;
import org.apache.batik.parser.TransformListParser;

/**
 * This class parses a sequence of transformations into an array of
 * {@link AffineTransform} instances.
 */
public class AffineTransformArrayParser implements TransformListHandler {

    private static final AffineTransform[] EMPTY_ARRAY = new AffineTransform[0];

    private List transforms;

    /**
     * Utility method for creating an AffineTransform array.
     * 
     * @param r
     *            The reader used to read the transform specification.
     * @return the AffineTransform array
     * @throws ParseException
     *             if there's a parse error
     */
    public static AffineTransform[] createAffineTransform(final Reader r)
            throws ParseException {
        final TransformListParser p = new TransformListParser();
        final AffineTransformArrayParser th = new AffineTransformArrayParser();

        p.setTransformListHandler(th);
        p.parse(r);

        return th.getAffineTransforms();
    }

    /**
     * Utility method for creating an AffineTransform.
     * 
     * @param s
     *            The transform specification.
     * @return the AffineTransform array
     * @throws ParseException
     *             if there's a parse error
     */
    public static AffineTransform[] createAffineTransform(final String s)
            throws ParseException {
        if (s == null) {
            return EMPTY_ARRAY;
        }
        final TransformListParser p = new TransformListParser();
        final AffineTransformArrayParser th = new AffineTransformArrayParser();

        p.setTransformListHandler(th);
        p.parse(s);

        return th.getAffineTransforms();
    }

    /**
     * Returns the AffineTransform array initialized during the last parsing.
     * 
     * @return the array or null if this handler has not been used by a parser.
     */
    public AffineTransform[] getAffineTransforms() {
        if (this.transforms == null) {
            return null;
        } else {
            final int count = this.transforms.size();
            return (AffineTransform[]) this.transforms
                    .toArray(new AffineTransform[count]);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startTransformList() throws ParseException {
        this.transforms = new java.util.ArrayList();
    }

    /** {@inheritDoc} */
    @Override
    public void matrix(final float a, final float b, final float c,
            final float d, final float e, final float f) throws ParseException {
        this.transforms.add(new AffineTransform(a, b, c, d, e, f));
    }

    /** {@inheritDoc} */
    @Override
    public void rotate(final float theta) throws ParseException {
        this.transforms.add(AffineTransform.getRotateInstance(Math
                .toRadians(theta)));
    }

    /** {@inheritDoc} */
    @Override
    public void rotate(final float theta, final float cx, final float cy)
            throws ParseException {
        final AffineTransform at = AffineTransform.getRotateInstance(
                Math.toRadians(theta), cx, cy);
        this.transforms.add(at);
    }

    /** {@inheritDoc} */
    @Override
    public void translate(final float tx) throws ParseException {
        final AffineTransform at = AffineTransform.getTranslateInstance(tx, 0);
        this.transforms.add(at);
    }

    /** {@inheritDoc} */
    @Override
    public void translate(final float tx, final float ty) throws ParseException {
        final AffineTransform at = AffineTransform.getTranslateInstance(tx, ty);
        this.transforms.add(at);
    }

    /** {@inheritDoc} */
    @Override
    public void scale(final float sx) throws ParseException {
        this.transforms.add(AffineTransform.getScaleInstance(sx, sx));
    }

    /** {@inheritDoc} */
    @Override
    public void scale(final float sx, final float sy) throws ParseException {
        this.transforms.add(AffineTransform.getScaleInstance(sx, sy));
    }

    /** {@inheritDoc} */
    @Override
    public void skewX(final float skx) throws ParseException {
        this.transforms.add(AffineTransform.getShearInstance(
                Math.tan(Math.toRadians(skx)), 0));
    }

    /** {@inheritDoc} */
    @Override
    public void skewY(final float sky) throws ParseException {
        this.transforms.add(AffineTransform.getShearInstance(0,
                Math.tan(Math.toRadians(sky))));
    }

    /** {@inheritDoc} */
    @Override
    public void endTransformList() throws ParseException {
    }
}
