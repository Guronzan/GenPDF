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

/* $Id: AreaTreeInputHandler.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.cli;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.AreaTreeModel;
import org.apache.fop.area.AreaTreeParser;
import org.apache.fop.area.RenderPagesModel;
import org.apache.fop.fonts.FontInfo;
import org.xml.sax.SAXException;

/**
 * InputHandler for the area tree XML (the old intermediate format) as input.
 */
public class AreaTreeInputHandler extends InputHandler {

    /**
     * Constructor for XML->XSLT->area tree XML input
     * 
     * @param xmlfile
     *            XML file
     * @param xsltfile
     *            XSLT file
     * @param params
     *            List of command-line parameters (name, value, name, value,
     *            ...) for XSL stylesheet, null if none
     */
    public AreaTreeInputHandler(final File xmlfile, final File xsltfile,
            final List params) {
        super(xmlfile, xsltfile, params);
    }

    /**
     * Constructor for area tree XML input
     * 
     * @param atfile
     *            the file to read the area tree document.
     */
    public AreaTreeInputHandler(final File atfile) {
        super(atfile);
    }

    /** {@inheritDoc} */
    @Override
    public void renderTo(final FOUserAgent userAgent,
            final String outputFormat, final OutputStream out)
            throws FOPException {
        final FontInfo fontInfo = new FontInfo();
        final AreaTreeModel treeModel = new RenderPagesModel(userAgent,
                outputFormat, fontInfo, out);

        // Iterate over all intermediate files
        final AreaTreeParser parser = new AreaTreeParser();

        // Resulting SAX events (the generated FO) must be piped through to FOP
        final Result res = new SAXResult(parser.getContentHandler(treeModel,
                userAgent));

        transformTo(res);

        try {
            treeModel.endDocument();
        } catch (final SAXException e) {
            throw new FOPException(e);
        }
    }

}
