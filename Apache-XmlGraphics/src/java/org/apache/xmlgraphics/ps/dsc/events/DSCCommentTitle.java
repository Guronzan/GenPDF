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

/* $Id: DSCCommentTitle.java 727407 2008-12-17 15:05:45Z jeremias $ */

package org.apache.xmlgraphics.ps.dsc.events;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.xmlgraphics.ps.DSCConstants;
import org.apache.xmlgraphics.ps.PSGenerator;

/**
 * Represents a %%Title DSC comment.
 */
public class DSCCommentTitle extends AbstractDSCComment {

    private String title;

    /**
     * Creates a new instance.
     */
    public DSCCommentTitle() {
    }

    /**
     * Creates a new instance.
     * 
     * @param title
     *            the title text
     */
    public DSCCommentTitle(final String title) {
        this.title = title;
    }

    /**
     * Returns the title.
     * 
     * @return the title
     */
    public String getTitle() {
        return this.title;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return DSCConstants.TITLE;
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
        this.title = (String) iter.next();
    }

    /** {@inheritDoc} */
    @Override
    public void generate(final PSGenerator gen) throws IOException {
        gen.writeDSCComment(getName(), getTitle());
    }

}
