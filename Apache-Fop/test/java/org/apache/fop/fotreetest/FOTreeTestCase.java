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

/* $Id: FOTreeTestCase.java 1198853 2011-11-07 18:18:29Z vhennebert $ */

package org.apache.fop.fotreetest;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.DebugHelper;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryConfigurator;
import org.apache.fop.fotreetest.ext.TestElementMapping;
import org.apache.fop.layoutengine.LayoutEngineTestUtils;
import org.apache.fop.layoutengine.TestFilesConfiguration;
import org.apache.fop.util.ConsoleEventListenerForTests;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Test driver class for FO tree tests.
 */
@RunWith(Parameterized.class)
@Slf4j
public class FOTreeTestCase {

    @BeforeClass
    public static void registerElementListObservers() {
        DebugHelper.registerStandardElementListObservers();
    }

    /**
     * Gets the parameters to run the FO tree test cases.
     *
     * @return a collection of file arrays containing the test files
     */
    @Parameters
    public static Collection<File[]> getParameters() {
        final TestFilesConfiguration.Builder builder = new TestFilesConfiguration.Builder();
        builder.testDir("test/fotree")
        .singleProperty("fop.fotree.single")
        .startsWithProperty("fop.fotree.starts-with")
        .suffix(".fo")
        .testSet("testcases")
        .disabledProperty("fop.layoutengine.disabled",
                "test/fotree/disabled-testcases.xml")
                .privateTestsProperty("fop.fotree.private");

        final TestFilesConfiguration testConfig = builder.build();
        return LayoutEngineTestUtils.getTestFiles(testConfig);
    }

    private final FopFactory fopFactory = FopFactory.newInstance();

    private final File testFile;

    /**
     * Main constructor
     *
     * @param testFile
     *            the FO file to test
     */
    public FOTreeTestCase(final File testFile) {
        this.fopFactory.addElementMapping(new TestElementMapping());
        this.testFile = testFile;
    }

    /**
     * Runs a test.
     *
     * @throws Exception
     *             if a test or FOP itself fails
     */
    @Test
    public void runTest() throws Exception {
        try {
            final ResultCollector collector = ResultCollector.getInstance();
            collector.reset();

            final SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setValidating(false);
            final SAXParser parser = spf.newSAXParser();
            XMLReader reader = parser.getXMLReader();

            // Resetting values modified by processing instructions
            this.fopFactory
            .setBreakIndentInheritanceOnReferenceAreaBoundary(FopFactoryConfigurator.DEFAULT_BREAK_INDENT_INHERITANCE);
            this.fopFactory
            .setSourceResolution(FopFactoryConfigurator.DEFAULT_SOURCE_RESOLUTION);

            final FOUserAgent ua = this.fopFactory.newFOUserAgent();
            ua.setBaseURL(this.testFile.getParentFile().toURI().toURL()
                    .toString());
            ua.setFOEventHandlerOverride(new DummyFOEventHandler(ua));
            ua.getEventBroadcaster().addEventListener(
                    new ConsoleEventListenerForTests(this.testFile.getName()));

            // Used to set values in the user agent through processing
            // instructions
            reader = new PIListener(reader, ua);

            final Fop fop = this.fopFactory.newFop(ua);

            reader.setContentHandler(fop.getDefaultHandler());
            reader.setDTDHandler(fop.getDefaultHandler());
            reader.setErrorHandler(fop.getDefaultHandler());
            reader.setEntityResolver(fop.getDefaultHandler());
            try {
                reader.parse(this.testFile.toURI().toURL().toExternalForm());
            } catch (final Exception e) {
                collector.notifyError(e.getLocalizedMessage());
                throw e;
            }

            final List<String> results = collector.getResults();
            if (results.size() > 0) {
                for (int i = 0; i < results.size(); i++) {
                    log.info(results.get(i));
                }
                throw new IllegalStateException(results.get(0));
            }
        } catch (final Exception e) {
            log.info("Error on " + this.testFile.getName());
            throw e;
        }
    }

    private static class PIListener extends XMLFilterImpl {

        private final FOUserAgent userAgent;

        public PIListener(final XMLReader parent, final FOUserAgent userAgent) {
            super(parent);
            this.userAgent = userAgent;
        }

        /** @see org.xml.sax.helpers.XMLFilterImpl */
        @Override
        public void processingInstruction(final String target, final String data)
                throws SAXException {
            if ("fop-useragent-break-indent-inheritance".equals(target)) {
                this.userAgent.getFactory()
                .setBreakIndentInheritanceOnReferenceAreaBoundary(
                        Boolean.valueOf(data).booleanValue());
            } else if ("fop-source-resolution".equals(target)) {
                this.userAgent.getFactory().setSourceResolution(
                        Float.parseFloat(data));
            }
            super.processingInstruction(target, data);
        }

    }

}
