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

/* $Id: AreaTreeParserTestCase.java 1237582 2012-01-30 09:49:22Z mehdi $ */

package org.apache.fop.intermediate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.area.AreaTreeModel;
import org.apache.fop.area.AreaTreeParser;
import org.apache.fop.area.RenderPagesModel;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.layoutengine.LayoutEngineTestUtils;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.xml.XMLRenderer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

/**
 * Tests the area tree parser.
 */
@RunWith(Parameterized.class)
@Slf4j
public class AreaTreeParserTestCase extends AbstractIntermediateTest {

    /**
     * Creates the parameters for this test.
     *
     * @return the list of file arrays populated with test files
     * @throws IOException
     *             if an I/O error occurs while reading the test file
     */
    @Parameters
    public static Collection<File[]> getParameters() throws IOException {
        return LayoutEngineTestUtils.getLayoutTestFiles();
    }

    /**
     * Constructor for the test suite that is used for each test file.
     *
     * @param testFile
     *            the test file to run
     * @throws IOException
     * @throws IOException
     *             if an I/O error occurs while loading the test case
     */
    public AreaTreeParserTestCase(final File testFile) throws IOException {
        super(testFile);
    }

    /** {@inheritDoc} */
    @Override
    protected String getIntermediateFileExtension() {
        return ".at.xml";
    }

    /** {@inheritDoc} */
    @Override
    protected Document buildIntermediateDocument(final Templates templates)
            throws Exception {
        final Transformer transformer = templates.newTransformer();
        setErrorListener(transformer);

        // Set up XMLRenderer to render to a DOM
        final TransformerHandler handler = testAssistant
                .getTransformerFactory().newTransformerHandler();
        final DOMResult domResult = new DOMResult();
        handler.setResult(domResult);

        final FOUserAgent userAgent = createUserAgent();

        // Create an instance of the target renderer so the XMLRenderer can use
        // its font setup
        final Renderer targetRenderer = userAgent.getRendererFactory()
                .createRenderer(userAgent, getTargetMIME());

        final XMLRenderer renderer = new XMLRenderer(userAgent);
        renderer.mimicRenderer(targetRenderer);
        renderer.setContentHandler(handler);

        userAgent.setRendererOverride(renderer);

        final Fop fop = this.fopFactory.newFop(
                MimeConstants.MIME_FOP_AREA_TREE, userAgent);
        final Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(new DOMSource(this.testDoc), res);

        return (Document) domResult.getNode();
    }

    /** {@inheritDoc} */
    @Override
    protected void parseAndRender(final Source src, final OutputStream out)
            throws Exception {
        final AreaTreeParser parser = new AreaTreeParser();

        final FOUserAgent userAgent = createUserAgent();
        final FontInfo fontInfo = new FontInfo();
        final AreaTreeModel treeModel = new RenderPagesModel(userAgent,
                getTargetMIME(), fontInfo, out);
        parser.parse(src, treeModel, userAgent);
        treeModel.endDocument();
    }

    /** {@inheritDoc} */
    @Override
    protected Document parseAndRenderToIntermediateFormat(final Source src)
            throws Exception {
        final AreaTreeParser parser = new AreaTreeParser();

        // Set up XMLRenderer to render to a DOM
        final TransformerHandler handler = testAssistant
                .getTransformerFactory().newTransformerHandler();
        final DOMResult domResult = new DOMResult();
        handler.setResult(domResult);
        final FOUserAgent userAgent = createUserAgent();
        final XMLRenderer renderer = new XMLRenderer(userAgent);
        userAgent.setRendererOverride(renderer);
        renderer.setContentHandler(handler);

        final FontInfo fontInfo = new FontInfo();
        final AreaTreeModel treeModel = new RenderPagesModel(userAgent,
                MimeConstants.MIME_FOP_AREA_TREE, fontInfo, null);
        parser.parse(src, treeModel, userAgent);
        treeModel.endDocument();

        return (Document) domResult.getNode();
    }

    @Override
    @Test
    public void runTest() throws Exception {
        try {
            testParserToIntermediateFormat();
            testParserToPDF();
        } catch (final Exception e) {
            log.error("Error on " + this.testFile.getName());
            throw e;
        }
    }

}
