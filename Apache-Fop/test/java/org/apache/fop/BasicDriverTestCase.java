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

/* $Id: BasicDriverTestCase.java 1198853 2011-11-07 18:18:29Z vhennebert $ */

package org.apache.fop;

import java.io.File;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.cli.InputHandler;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Basic runtime test for the old Fop class. It is used to verify that nothing
 * obvious is broken after compiling.
 */
public class BasicDriverTestCase extends AbstractFOPTest {

    private final FopFactory fopFactory = FopFactory.newInstance();

    /**
     * Tests Fop with JAXP and OutputStream generating PDF.
     * 
     * @throws Exception
     *             if anything fails
     */
    @Test
    public void testFO2PDFWithJAXP() throws Exception {
        final FOUserAgent foUserAgent = this.fopFactory.newFOUserAgent();
        final File foFile = new File(getBaseDir(), "test/xml/bugtests/block.fo");
        final ByteArrayOutputStream baout = new ByteArrayOutputStream();
        final Fop fop = this.fopFactory.newFop(
                org.apache.xmlgraphics.util.MimeConstants.MIME_PDF,
                foUserAgent, baout);

        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer transformer = factory.newTransformer(); // Identity
                                                                  // transf.
        final Source src = new StreamSource(foFile);
        final Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(src, res);

        assertTrue("Generated PDF has zero length", baout.size() > 0);
    }

    /**
     * Tests Fop with JAXP and OutputStream generating PostScript.
     * 
     * @throws Exception
     *             if anything fails
     */
    @Test
    public void testFO2PSWithJAXP() throws Exception {
        final FOUserAgent foUserAgent = this.fopFactory.newFOUserAgent();
        final File foFile = new File(getBaseDir(), "test/xml/bugtests/block.fo");
        final ByteArrayOutputStream baout = new ByteArrayOutputStream();
        final Fop fop = this.fopFactory.newFop(
                org.apache.xmlgraphics.util.MimeConstants.MIME_POSTSCRIPT,
                foUserAgent, baout);

        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer transformer = factory.newTransformer(); // Identity
                                                                  // transf.
        final Source src = new StreamSource(foFile);
        final Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(src, res);

        assertTrue("Generated PostScript has zero length", baout.size() > 0);
    }

    /**
     * Tests Fop with JAXP and OutputStream generating RTF.
     * 
     * @throws Exception
     *             if anything fails
     */
    @Test
    public void testFO2RTFWithJAXP() throws Exception {
        final FOUserAgent foUserAgent = this.fopFactory.newFOUserAgent();
        final File foFile = new File(getBaseDir(), "test/xml/bugtests/block.fo");
        final ByteArrayOutputStream baout = new ByteArrayOutputStream();
        final Fop fop = this.fopFactory.newFop(
                org.apache.xmlgraphics.util.MimeConstants.MIME_RTF,
                foUserAgent, baout);

        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer transformer = factory.newTransformer(); // Identity
                                                                  // transf.
        final Source src = new StreamSource(foFile);
        final Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(src, res);

        assertTrue("Generated RTF has zero length", baout.size() > 0);
    }

    /**
     * Tests Fop with XsltInputHandler and OutputStream.
     * 
     * @throws Exception
     *             if anything fails
     */
    @Test
    public void testFO2PDFWithXSLTInputHandler() throws Exception {
        final FOUserAgent foUserAgent = this.fopFactory.newFOUserAgent();
        final File xmlFile = new File(getBaseDir(), "test/xml/1.xml");
        final File xsltFile = new File(getBaseDir(), "test/xsl/doc.xsl");
        final ByteArrayOutputStream baout = new ByteArrayOutputStream();

        final InputHandler handler = new InputHandler(xmlFile, xsltFile, null);
        handler.renderTo(foUserAgent,
                org.apache.xmlgraphics.util.MimeConstants.MIME_PDF, baout);

        assertTrue("Generated PDF has zero length", baout.size() > 0);
    }

}
