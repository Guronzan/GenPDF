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

/* $Id: Overlay.java 1067109 2011-02-04 08:14:41Z jeremias $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.afp.Factory;

/**
 * An overlay is a MO:DCA-P resource object.
 *
 * It may be stored in an external resource library or it may be carried in a
 * resource group. An overlay is similar to a page in that it defines its own
 * environment and carries the same data objects.
 */
public class Overlay extends PageObject {

    /**
     * Construct a new overlay object for the specified name argument, the
     * overlay name should be an 8 character identifier.
     *
     * @param factory
     *            the resource manager of the page.
     * @param name
     *            the name of the page.
     * @param width
     *            the width of the page.
     * @param height
     *            the height of the page.
     * @param rotation
     *            the rotation of the page.
     * @param widthResolution
     *            the width resolution of the page.
     * @param heightResolution
     *            the height resolution of the page.
     */
    public Overlay(final Factory factory, final String name, final int width,
            final int height, final int rotation, final int widthResolution,
            final int heightResolution) {
        super(factory, name, width, height, rotation, widthResolution,
                heightResolution);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeStart(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.BEGIN, Category.OVERLAY);
        os.write(data);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final OutputStream os) throws IOException {
        super.writeContent(os);

        getActiveEnvironmentGroup().writeToStream(os);

        writeObjects(this.objects, os);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEnd(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.END, Category.OVERLAY);
        os.write(data);
    }
}
