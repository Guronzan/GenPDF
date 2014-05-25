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

/* $Id: AbstractPostScriptTest.java 1198853 2011-11-07 18:18:29Z vhennebert $ */

package org.apache.fop.render.ps;

import java.io.File;
import java.io.IOException;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.render.AbstractRenderingTest;
import org.apache.xmlgraphics.ps.PSResource;
import org.apache.xmlgraphics.ps.dsc.DSCException;
import org.apache.xmlgraphics.ps.dsc.DSCParser;
import org.apache.xmlgraphics.ps.dsc.events.AbstractResourceDSCComment;
import org.apache.xmlgraphics.ps.dsc.events.DSCComment;
import org.apache.xmlgraphics.ps.dsc.events.DSCEvent;

import static org.junit.Assert.assertEquals;

/**
 * Abstract base class for PostScript verification tests.
 */
public abstract class AbstractPostScriptTest extends AbstractRenderingTest {

    /**
     * Renders a test file.
     * 
     * @param ua
     *            the user agent (with override set!)
     * @param resourceName
     *            the resource name for the FO file
     * @param suffix
     *            a suffix for the output filename
     * @return the output file
     * @throws Exception
     *             if an error occurs
     */
    protected File renderFile(final FOUserAgent ua, final String resourceName,
            final String suffix) throws Exception {
        return renderFile(ua, resourceName, suffix,
                org.apache.xmlgraphics.util.MimeConstants.MIME_POSTSCRIPT);
    }

    /**
     * Scans for a certain resource DSC comment and checks against a given
     * resource.
     * 
     * @param parser
     *            the DSC parser
     * @param comment
     *            the comment to scan for
     * @param resource
     *            the resource to check against
     * @throws IOException
     *             if an I/O error occurs
     * @throws DSCException
     *             if a DSC error occurs
     */
    protected void checkResourceComment(final DSCParser parser,
            final String comment, final PSResource resource)
            throws IOException, DSCException {
        AbstractResourceDSCComment resComment;
        resComment = (AbstractResourceDSCComment) gotoDSCComment(parser,
                comment);
        assertEquals(resource, resComment.getResource());
    }

    /**
     * Advances the DSC parser to a DSC comment with the given name.
     * 
     * @param parser
     *            the DSC parser
     * @param name
     *            the name of the DSC comment
     * @return the DSC comment
     * @throws IOException
     *             if an I/O error occurs
     * @throws DSCException
     *             if a DSC error occurs
     */
    protected static DSCComment gotoDSCComment(final DSCParser parser,
            final String name) throws IOException, DSCException {
        while (parser.hasNext()) {
            final DSCEvent event = parser.nextEvent();
            if (event.isDSCComment()) {
                final DSCComment comment = event.asDSCComment();
                if (comment.getName().equals(name)) {
                    return comment;
                }
            }
        }
        return null;
    }

}