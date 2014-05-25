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

/* $Id: TestAssistant.java 1094690 2011-04-18 18:36:05Z vhennebert $ */

package org.apache.fop.intermediate;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FopFactory;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Helper class for running FOP tests.
 */
public class TestAssistant {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();
    private final FopFactory fopFactoryWithBase14Kerning = FopFactory
            .newInstance();

    private final SAXTransformerFactory tfactory = (SAXTransformerFactory) SAXTransformerFactory
            .newInstance();

    private final DocumentBuilderFactory domBuilderFactory;

    private Templates testcase2fo;
    private Templates testcase2checks;

    /**
     * Main constructor.
     */
    public TestAssistant() {
        this.fopFactory.getFontManager().setBase14KerningEnabled(false);
        this.fopFactoryWithBase14Kerning.getFontManager()
                .setBase14KerningEnabled(true);
        this.domBuilderFactory = DocumentBuilderFactory.newInstance();
        this.domBuilderFactory.setNamespaceAware(true);
        this.domBuilderFactory.setValidating(false);
    }

    /**
     * Returns the stylesheet for convert extracting the XSL-FO part from the
     * test case.
     * 
     * @return the stylesheet
     * @throws TransformerConfigurationException
     *             if an error occurs loading the stylesheet
     */
    public Templates getTestcase2FOStylesheet()
            throws TransformerConfigurationException {
        if (this.testcase2fo == null) {
            // Load and cache stylesheet
            final Source src = new StreamSource(new File(
                    "test/layoutengine/testcase2fo.xsl"));
            this.testcase2fo = this.tfactory.newTemplates(src);
        }
        return this.testcase2fo;
    }

    /**
     * Returns the stylesheet for convert extracting the checks from the test
     * case.
     * 
     * @return the stylesheet
     * @throws TransformerConfigurationException
     *             if an error occurs loading the stylesheet
     */
    private Templates getTestcase2ChecksStylesheet()
            throws TransformerConfigurationException {
        if (this.testcase2checks == null) {
            // Load and cache stylesheet
            final Source src = new StreamSource(new File(
                    "test/layoutengine/testcase2checks.xsl"));
            this.testcase2checks = this.tfactory.newTemplates(src);
        }
        return this.testcase2checks;
    }

    /**
     * Returns the element from the given XML file that encloses the tests.
     *
     * @param testFile
     *            a test case
     * @return the parent element of the group(s) of checks
     * @throws TransformerException
     *             if an error occurs while extracting the test element
     */
    public Element getTestRoot(final File testFile) throws TransformerException {
        final Transformer transformer = getTestcase2ChecksStylesheet()
                .newTransformer();
        final DOMResult res = new DOMResult();
        transformer.transform(new StreamSource(testFile), res);
        final Document doc = (Document) res.getNode();
        return doc.getDocumentElement();
    }

    public FopFactory getFopFactory(final boolean base14KerningEnabled) {
        final FopFactory effFactory = base14KerningEnabled ? this.fopFactoryWithBase14Kerning
                : this.fopFactory;
        return effFactory;
    }

    public FopFactory getFopFactory(final Document testDoc) {
        final boolean base14KerningEnabled = isBase14KerningEnabled(testDoc);
        final FopFactory effFactory = getFopFactory(base14KerningEnabled);

        final boolean strictValidation = isStrictValidation(testDoc);
        effFactory.setStrictValidation(strictValidation);

        return effFactory;
    }

    private boolean isBase14KerningEnabled(final Document testDoc) {
        try {
            final XObject xo = XPathAPI.eval(testDoc,
                    "/testcase/cfg/base14kerning");
            final String s = xo.str();
            return "true".equalsIgnoreCase(s);
        } catch (final TransformerException e) {
            throw new RuntimeException(
                    "Error while evaluating XPath expression", e);
        }
    }

    private boolean isStrictValidation(final Document testDoc) {
        try {
            final XObject xo = XPathAPI.eval(testDoc,
                    "/testcase/cfg/strict-validation");
            return !"false".equalsIgnoreCase(xo.str());
        } catch (final TransformerException e) {
            throw new RuntimeException(
                    "Error while evaluating XPath expression", e);
        }
    }

    /**
     * Loads a test case into a DOM document.
     * 
     * @param testFile
     *            the test file
     * @return the loaded test case
     * @throws IOException
     *             if an I/O error occurs loading the test case
     */
    public Document loadTestCase(final File testFile) throws IOException {
        try {
            final DocumentBuilder builder = this.domBuilderFactory
                    .newDocumentBuilder();
            final Document testDoc = builder.parse(testFile);
            return testDoc;
        } catch (final Exception e) {
            throw new IOException("Error while loading test case: "
                    + e.getMessage());
        }
    }

    /**
     * Serialize the DOM for later inspection.
     * 
     * @param doc
     *            the DOM document
     * @param target
     *            target file
     * @throws TransformerException
     *             if a problem occurs during serialization
     */
    public void saveDOM(final Document doc, final File target)
            throws TransformerException {
        final Transformer transformer = getTransformerFactory()
                .newTransformer();
        final Source src = new DOMSource(doc);
        final Result res = new StreamResult(target);
        transformer.transform(src, res);
    }

    /**
     * Returns the SAXTransformerFactory.
     * 
     * @return the SAXTransformerFactory
     */
    public SAXTransformerFactory getTransformerFactory() {
        return this.tfactory;
    }
}
