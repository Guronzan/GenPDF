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

/* $Id: LayoutEngineTestUtils.java 1203749 2011-11-18 17:08:20Z vhennebert $ */

package org.apache.fop.layoutengine;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Utility class for layout engine tests.
 */
public final class LayoutEngineTestUtils {

    private LayoutEngineTestUtils() {
    }

    private static class FilenameHandler extends DefaultHandler {
        private final StringBuilder buffer = new StringBuilder(128);
        private boolean readingFilename = false;
        private final List<String> filenames;

        public FilenameHandler(final List<String> filenames) {
            this.filenames = filenames;
        }

        @Override
        public void startElement(final String namespaceURI,
                final String localName, final String qName,
                final Attributes atts) throws SAXException {
            if (qName != null && qName.equals("file")) {
                this.buffer.setLength(0);
                this.readingFilename = true;
            } else {
                throw new RuntimeException(
                        "Unexpected element while reading disabled testcase file names: "
                                + qName);
            }
        }

        @Override
        public void endElement(final String namespaceURI,
                final String localName, final String qName) throws SAXException {
            if (qName != null && qName.equals("file")) {
                this.readingFilename = false;
                this.filenames.add(this.buffer.toString());
            } else {
                throw new RuntimeException(
                        "Unexpected element while reading disabled testcase file names: "
                                + qName);
            }
        }

        @Override
        public void characters(final char[] ch, final int start,
                final int length) throws SAXException {
            if (this.readingFilename) {
                this.buffer.append(ch, start, length);
            }
        }
    }

    /**
     * Removes from {@code filter} any tests that have been disabled.
     *
     * @param filter
     *            the filter populated with tests
     * @param disabled
     *            name of the file containing disabled test cases. If null or
     *            empty, no file is read
     * @return {@code filter} minus any disabled tests
     */
    public static IOFileFilter decorateWithDisabledList(IOFileFilter filter,
            final String disabled) {
        if (disabled != null && disabled.length() > 0) {
            filter = new AndFileFilter(new NotFileFilter(new NameFileFilter(
                    LayoutEngineTestUtils.readDisabledTestcases(new File(
                            disabled)))), filter);
        }
        return filter;
    }

    private static String[] readDisabledTestcases(final File f) {
        final List<String> lines = new ArrayList<String>();
        final Source stylesheet = new StreamSource(new File(
                "test/layoutengine/disabled-testcase2filename.xsl"));
        final Source source = new StreamSource(f);
        final Result result = new SAXResult(new FilenameHandler(lines));
        try {
            final Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer(stylesheet);
            transformer.transform(source, result);
        } catch (final TransformerConfigurationException tce) {
            throw new RuntimeException(tce);
        } catch (final TransformerException te) {
            throw new RuntimeException(te);
        }
        return lines.toArray(new String[lines.size()]);
    }

    /**
     * Returns the test files matching the given configuration.
     *
     * @param testConfig
     *            the test configuration
     * @return the applicable test cases
     */
    public static Collection<File[]> getTestFiles(
            final TestFilesConfiguration testConfig) {
        final File mainDir = testConfig.getTestDirectory();
        IOFileFilter filter;
        final String single = testConfig.getSingleTest();
        final String startsWith = testConfig.getStartsWith();
        if (single != null) {
            filter = new NameFileFilter(single);
        } else if (startsWith != null) {
            filter = new PrefixFileFilter(startsWith);
            filter = new AndFileFilter(filter, new SuffixFileFilter(
                    testConfig.getFileSuffix()));
            filter = decorateWithDisabledList(filter,
                    testConfig.getDisabledTests());
        } else {
            filter = new SuffixFileFilter(testConfig.getFileSuffix());
            filter = decorateWithDisabledList(filter,
                    testConfig.getDisabledTests());
        }
        final String testset = testConfig.getTestSet();

        final Collection<File> files = FileUtils.listFiles(new File(mainDir,
                testset), filter, TrueFileFilter.INSTANCE);
        if (testConfig.hasPrivateTests()) {
            final Collection<File> privateFiles = FileUtils.listFiles(new File(
                    mainDir, "private-testcases"), filter,
                    TrueFileFilter.INSTANCE);
            files.addAll(privateFiles);
        }

        final Collection<File[]> parametersForJUnit4 = new ArrayList<File[]>();
        for (final File f : files) {
            parametersForJUnit4.add(new File[] { f });
        }

        return parametersForJUnit4;
    }

    /**
     * This is a helper method that uses the standard parameters for FOP's
     * layout engine tests and returns a set of test files. These pull in System
     * parameters to configure the layout tests to run.
     *
     * @return A collection of file arrays that contain the test files
     */
    public static Collection<File[]> getLayoutTestFiles() {
        String testSet = System.getProperty("fop.layoutengine.testset");
        testSet = (testSet != null ? testSet : "standard") + "-testcases";
        return getLayoutTestFiles(testSet);
    }

    /**
     * This is a helper method that uses the standard parameters for FOP's
     * layout engine tests, given a test set name returns a set of test files.
     *
     * @param testSetName
     *            the name of the test set
     * @return A collection of file arrays that contain the test files
     */
    public static Collection<File[]> getLayoutTestFiles(final String testSetName) {
        final TestFilesConfiguration.Builder builder = new TestFilesConfiguration.Builder();

        builder.testDir("test/layoutengine")
        .singleProperty("fop.layoutengine.single")
        .startsWithProperty("fop.layoutengine.starts-with")
        .suffix(".xml")
        .testSet(testSetName)
        .disabledProperty("fop.layoutengine.disabled",
                "test/layoutengine/disabled-testcases.xml")
                .privateTestsProperty("fop.layoutengine.private");

        final TestFilesConfiguration testConfig = builder.build();
        return getTestFiles(testConfig);
    }

}
