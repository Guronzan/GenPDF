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

/* $Id: IFParserTestCase.java 1242848 2012-02-10 16:51:08Z phancock $ */

package org.apache.fop.intermediate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.layoutengine.LayoutEngineTestUtils;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFParser;
import org.apache.fop.render.intermediate.IFSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

/**
 * Tests the intermediate format parser.
 */
@RunWith(Parameterized.class)
@Slf4j
public class IFParserTestCase extends AbstractIFTest {

    /**
     * Set this to true to get the correspondence between test number and test
     * file.
     */
    private static final boolean DEBUG = false;

    /**
     * Gets the parameters for this test
     *
     * @return a collection of file arrays containing the test files
     * @throws IOException
     *             if an error occurs when trying to read the test files
     */
    @Parameters
    public static Collection<File[]> getParameters() throws IOException {
        final Collection<File[]> testFiles = LayoutEngineTestUtils
                .getLayoutTestFiles();
        if (DEBUG) {
            printFiles(testFiles);
        }
        return testFiles;
    }

    private static void printFiles(final Collection<File[]> files) {
        int index = 0;
        for (final File[] file : files) {
            assert file.length == 1;
            log.info(String.format("%3d %s", index++, file[0]));
        }
    }

    /**
     * Constructor for the test suite that is used for each test file.
     *
     * @param testFile
     *            the test file to run
     * @throws IOException
     *             if an I/O error occurs while loading the test case
     */
    public IFParserTestCase(final File testFile) throws IOException {
        super(testFile);
    }

    /** {@inheritDoc} */
    @Override
    protected void parseAndRender(final Source src, final OutputStream out)
            throws Exception {
        final IFParser parser = new IFParser();

        final FOUserAgent userAgent = createUserAgent();

        final IFDocumentHandler documentHandler = userAgent
                .getRendererFactory().createDocumentHandler(userAgent,
                        getTargetMIME());
        documentHandler.setResult(new StreamResult(out));
        documentHandler.setDefaultFontInfo(new FontInfo());
        parser.parse(src, documentHandler, userAgent);
    }

    /** {@inheritDoc} */
    @Override
    protected Document parseAndRenderToIntermediateFormat(final Source src)
            throws Exception {
        final IFParser parser = new IFParser();

        final FOUserAgent userAgent = createUserAgent();

        final IFSerializer serializer = new IFSerializer();
        serializer.setContext(new IFContext(userAgent));
        final DOMResult domResult = new DOMResult();
        serializer.setResult(domResult);

        parser.parse(src, serializer, userAgent);

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
