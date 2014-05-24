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

/* $Id: AbstractResourceDSCComment.java 727407 2008-12-17 15:05:45Z jeremias $ */

package org.apache.xmlgraphics.ps.dsc.events;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.xmlgraphics.ps.PSGenerator;
import org.apache.xmlgraphics.ps.PSProcSet;
import org.apache.xmlgraphics.ps.PSResource;

/**
 * Abstract base class for resource comments.
 */
public abstract class AbstractResourceDSCComment extends AbstractDSCComment {

    private PSResource resource;

    /**
     * Creates a new instance
     */
    public AbstractResourceDSCComment() {
    }

    /**
     * Creates a new instance for a given PSResource instance
     * 
     * @param resource
     *            the resource
     */
    public AbstractResourceDSCComment(final PSResource resource) {
        this.resource = resource;
    }

    /**
     * Returns the associated PSResource.
     * 
     * @return the resource
     */
    public PSResource getResource() {
        return this.resource;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasValues() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void parseValue(final String value) {
        final List params = splitParams(value);
        final Iterator iter = params.iterator();
        final String name = (String) iter.next();
        if (PSResource.TYPE_FONT.equals(name)) {
            final String fontname = (String) iter.next();
            this.resource = new PSResource(name, fontname);
        } else if (PSResource.TYPE_PROCSET.equals(name)) {
            final String procname = (String) iter.next();
            final String version = (String) iter.next();
            final String revision = (String) iter.next();
            this.resource = new PSProcSet(procname, Float.parseFloat(version),
                    Integer.parseInt(revision));
        } else if (PSResource.TYPE_FILE.equals(name)) {
            final String filename = (String) iter.next();
            this.resource = new PSResource(name, filename);
        } else if (PSResource.TYPE_FORM.equals(name)) {
            final String formname = (String) iter.next();
            this.resource = new PSResource(name, formname);
        } else if (PSResource.TYPE_PATTERN.equals(name)) {
            final String patternname = (String) iter.next();
            this.resource = new PSResource(name, patternname);
        } else if (PSResource.TYPE_ENCODING.equals(name)) {
            final String encodingname = (String) iter.next();
            this.resource = new PSResource(name, encodingname);
        } else {
            throw new IllegalArgumentException("Invalid resource type: " + name);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void generate(final PSGenerator gen) throws IOException {
        gen.writeDSCComment(getName(), getResource());
    }

}
