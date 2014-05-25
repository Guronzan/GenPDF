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

/* $Id: URIResolutionTestCase.java 1237582 2012-01-30 09:49:22Z mehdi $ */

package org.apache.fop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.render.xml.XMLRenderer;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests URI resolution facilities.
 */
public class URIResolutionTestCase extends AbstractFOPTest {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();

    private final SAXTransformerFactory tfactory = (SAXTransformerFactory) SAXTransformerFactory
            .newInstance();

    private final static File backupDir = new File(getBaseDir(),
            "build/test-results");

    @BeforeClass
    public static void makeDirs() {
        backupDir.mkdirs();
    }

    /**
     * Test custom URI resolution with a hand-written URIResolver.
     * 
     * @throws Exception
     *             if anything fails
     */
    @Test
    public void testFO1a() throws Exception {
        innerTestFO1(false);
    }

    /**
     * Test custom URI resolution with a hand-written URIResolver.
     * 
     * @throws Exception
     *             if anything fails
     */
    @Test
    public void testFO1b() throws Exception {
        innerTestFO1(true);
    }

    private void innerTestFO1(final boolean withStream) throws Exception {
        final FOUserAgent ua = this.fopFactory.newFOUserAgent();

        final File foFile = new File(getBaseDir(),
                "test/xml/uri-resolution1.fo");

        final MyURIResolver resolver = new MyURIResolver(withStream);
        ua.setURIResolver(resolver);
        ua.setBaseURL(foFile.getParentFile().toURI().toURL().toString());

        final Document doc = createAreaTree(foFile, ua);

        // Check how many times the resolver was consulted
        assertEquals("Expected resolver to do 1 successful URI resolution", 1,
                resolver.successCount);
        assertEquals("Expected resolver to do 0 failed URI resolution", 0,
                resolver.failureCount);
        // Additional XPath checking on the area tree
        assertEquals("viewport for external-graphic is missing", "true",
                evalXPath(doc, "boolean(//flow/block[1]/lineArea/viewport)"));
        assertEquals("46080",
                evalXPath(doc, "//flow/block[1]/lineArea/viewport/@ipd"));
        assertEquals("46080",
                evalXPath(doc, "//flow/block[1]/lineArea/viewport/@bpd"));
    }

    /**
     * Test custom URI resolution with a hand-written URIResolver.
     * 
     * @throws Exception
     *             if anything fails
     */
    @Test
    public void testFO2() throws Exception {
        // TODO This will only work when we can do URI resolution inside Batik!
        final File foFile = new File(getBaseDir(),
                "test/xml/uri-resolution2.fo");

        final FOUserAgent ua = this.fopFactory.newFOUserAgent();
        final MyURIResolver resolver = new MyURIResolver(false);
        ua.setURIResolver(resolver);
        ua.setBaseURL(foFile.getParentFile().toURI().toURL().toString());

        final ByteArrayOutputStream baout = new ByteArrayOutputStream();

        final Fop fop = this.fopFactory.newFop(
                org.apache.xmlgraphics.util.MimeConstants.MIME_PDF, ua, baout);

        final Transformer transformer = this.tfactory.newTransformer(); // Identity
                                                                        // transf.
        final Source src = new StreamSource(foFile);
        final Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(src, res);

        final OutputStream out = new java.io.FileOutputStream(new File(
                backupDir, foFile.getName() + ".pdf"));
        try {
            baout.writeTo(out);
        } finally {
            IOUtils.closeQuietly(out);
        }

        // Check how many times the resolver was consulted
        assertEquals("Expected resolver to do 1 successful URI resolution", 1,
                resolver.successCount);
        assertEquals("Expected resolver to do 0 failed URI resolutions", 0,
                resolver.failureCount);
        // Test using PDF as the area tree doesn't invoke Batik so we could
        // check
        // if the resolver is actually passed to Batik by FOP
        assertTrue("Generated PDF has zero length", baout.size() > 0);
    }

    private Document createAreaTree(final File fo, final FOUserAgent ua)
            throws TransformerException, FOPException {
        final DOMResult domres = new DOMResult();
        // Setup Transformer to convert the area tree to a DOM
        final TransformerHandler athandler = this.tfactory
                .newTransformerHandler();
        athandler.setResult(domres);

        final XMLRenderer atrenderer = new XMLRenderer(ua);
        atrenderer.setContentHandler(athandler);
        ua.setRendererOverride(atrenderer);

        final Fop fop = this.fopFactory.newFop(ua);

        final Transformer transformer = this.tfactory.newTransformer(); // Identity
                                                                        // transf.
        final Source src = new StreamSource(fo);
        final Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(src, res);

        final Document doc = (Document) domres.getNode();
        saveAreaTreeXML(doc, new File(backupDir, fo.getName() + ".at.xml"));
        return doc;
    }

    private String evalXPath(final Document doc, final String xpath) {
        XObject res;
        try {
            res = XPathAPI.eval(doc, xpath);
        } catch (final TransformerException e) {
            throw new RuntimeException("XPath evaluation failed: "
                    + e.getMessage());
        }
        return res.str();
    }

    /**
     * Save the area tree XML for later inspection.
     * 
     * @param doc
     *            area tree as a DOM document
     * @param target
     *            target file
     * @throws TransformerException
     *             if a problem occurs during serialization
     */
    protected void saveAreaTreeXML(final Document doc, final File target)
            throws TransformerException {
        final Transformer transformer = this.tfactory.newTransformer();
        final Source src = new DOMSource(doc);
        final Result res = new StreamResult(target);
        transformer.transform(src, res);
    }

    private class MyURIResolver implements URIResolver {

        private static final String PREFIX = "funky:";

        private final boolean withStream;
        private int successCount = 0;
        private int failureCount = 0;

        public MyURIResolver(final boolean withStream) {
            this.withStream = withStream;
        }

        /**
         * @see javax.xml.transform.URIResolver#resolve(java.lang.String,
         *      java.lang.String)
         */
        @Override
        public Source resolve(final String href, final String base)
                throws TransformerException {
            if (href.startsWith(PREFIX)) {
                final String name = href.substring(PREFIX.length());
                if ("myimage123".equals(name)) {
                    final File image = new File(getBaseDir(),
                            "test/resources/images/bgimg300dpi.jpg");
                    Source src;
                    if (this.withStream) {
                        try {
                            src = new StreamSource(new java.io.FileInputStream(
                                    image));
                        } catch (final FileNotFoundException e) {
                            throw new TransformerException(e.getMessage(), e);
                        }
                    } else {
                        src = new StreamSource(image);
                    }
                    this.successCount++;
                    return src;
                } else {
                    this.failureCount++;
                    throw new TransformerException("funky image not found");
                }
            } else {
                this.failureCount++;
                return null;
            }
        }

    }

}
