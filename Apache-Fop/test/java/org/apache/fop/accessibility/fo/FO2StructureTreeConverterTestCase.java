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

/* $Id: FO2StructureTreeConverterTestCase.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.accessibility.fo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.accessibility.StructureTree2SAXEventAdapter;
import org.apache.fop.accessibility.StructureTreeEventHandler;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.FODocumentParser;
import org.apache.fop.fo.FODocumentParser.FOEventHandlerFactory;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.LoadingException;
import org.apache.fop.fotreetest.DummyFOEventHandler;
import org.custommonkey.xmlunit.Diff;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.Assert.assertTrue;

public class FO2StructureTreeConverterTestCase {

    private interface FOLoader {

        InputStream getFoInputStream();
    }

    private static final String STRUCTURE_TREE_SEQUENCE_NAME = "structure-tree-sequence";

    private FOLoader foLoader;

    @Test
    public void testCompleteDocument() throws Exception {
        this.foLoader = new FOLoader() {
            @Override
            public InputStream getFoInputStream() {
                return getResource("/org/apache/fop/fo/complete_document.fo");
            }
        };
        testConverter();
    }

    @Test
    public void testTableFooters() throws Exception {
        this.foLoader = new FOLoader() {
            @Override
            public InputStream getFoInputStream() {
                return getResource("table-footers.fo");
            }
        };
        testConverter();
    }

    @Test
    public void testCompleteContentWrappedInTableFooter() throws Exception {
        final Source xslt = new StreamSource(
                getResource("wrapCompleteDocumentInTableFooter.xsl"));
        final Transformer transformer = createTransformer(xslt);
        final InputStream originalFO = getResource("/org/apache/fop/fo/complete_document.fo");
        final ByteArrayOutputStream transformedFoOutput = new ByteArrayOutputStream();
        transformer.transform(new StreamSource(originalFO), new StreamResult(
                transformedFoOutput));
        final byte[] transformedFoOutputBytes = transformedFoOutput
                .toByteArray();
        this.foLoader = new FOLoader() {
            @Override
            public InputStream getFoInputStream() {
                return new ByteArrayInputStream(transformedFoOutputBytes);
            }
        };
        testConverter();
    }

    @Test
    public void testArtifact() throws Exception {
        this.foLoader = new FOLoader() {

            @Override
            public InputStream getFoInputStream() {
                return getResource("artifact.fo");
            }
        };
        testConverter();
    }

    private Transformer createTransformer(final Source xslt)
            throws TransformerFactoryConfigurationError,
    TransformerConfigurationException {
        final TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        return transformerFactory.newTransformer(xslt);
    }

    private static InputStream getResource(final String name) {
        return FO2StructureTreeConverterTestCase.class
                .getResourceAsStream(name);
    }

    private void testConverter() throws Exception {
        final DOMResult expectedStructureTree = loadExpectedStructureTree();
        final DOMResult actualStructureTree = buildActualStructureTree();
        final Diff diff = createDiff(expectedStructureTree, actualStructureTree);
        assertTrue(diff.toString(), diff.identical());
    }

    private DOMResult loadExpectedStructureTree() {
        final DOMResult expectedStructureTree = new DOMResult();
        final InputStream xslt = getResource("fo2StructureTree.xsl");
        runXSLT(xslt, this.foLoader.getFoInputStream(), expectedStructureTree);
        return expectedStructureTree;
    }

    private static void runXSLT(final InputStream xslt, final InputStream doc,
            final Result result) {
        final Source fo = new StreamSource(doc);
        try {
            final Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer(new StreamSource(xslt));
            transformer.transform(fo, result);
        } catch (final TransformerConfigurationException e) {
            throw new RuntimeException(e);
        } catch (final TransformerException e) {
            throw new RuntimeException(e);
        } finally {
            closeStream(xslt);
            closeStream(doc);
        }
    }

    private static void closeStream(final InputStream stream) {
        try {
            stream.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DOMResult buildActualStructureTree() throws Exception {
        final DOMResult actualStructureTree = new DOMResult();
        createStructureTreeFromDocument(this.foLoader.getFoInputStream(),
                actualStructureTree);
        return actualStructureTree;
    }

    private static void createStructureTreeFromDocument(
            final InputStream foInputStream, final Result result)
            throws Exception {
        final TransformerHandler tHandler = createTransformerHandler(result);
        startStructureTreeSequence(tHandler);
        final StructureTreeEventHandler structureTreeEventHandler = StructureTree2SAXEventAdapter
                .newInstance(tHandler);
        final FODocumentParser documentParser = createDocumentParser(structureTreeEventHandler);
        final FOUserAgent userAgent = createFOUserAgent(documentParser);
        parseDocument(foInputStream, documentParser, userAgent);
        endStructureTreeSequence(tHandler);
    }

    private static TransformerHandler createTransformerHandler(
            final Result domResult) throws TransformerConfigurationException,
            TransformerFactoryConfigurationError {
        final SAXTransformerFactory factory = (SAXTransformerFactory) TransformerFactory
                .newInstance();
        final TransformerHandler transformerHandler = factory
                .newTransformerHandler();
        transformerHandler.setResult(domResult);
        return transformerHandler;
    }

    private static void startStructureTreeSequence(
            final TransformerHandler tHandler) throws SAXException {
        tHandler.startDocument();
        tHandler.startElement("", STRUCTURE_TREE_SEQUENCE_NAME,
                STRUCTURE_TREE_SEQUENCE_NAME, new AttributesImpl());
    }

    private static FODocumentParser createDocumentParser(
            final StructureTreeEventHandler structureTreeEventHandler) {
        return FODocumentParser.newInstance(new FOEventHandlerFactory() {
            @Override
            public FOEventHandler newFOEventHandler(
                    final FOUserAgent foUserAgent) {
                return new FO2StructureTreeConverter(structureTreeEventHandler,
                        new DummyFOEventHandler(foUserAgent));
            }
        });
    }

    private static FOUserAgent createFOUserAgent(
            final FODocumentParser documentParser) {
        final FOUserAgent userAgent = documentParser.createFOUserAgent();
        userAgent.setAccessibility(true);
        return userAgent;
    }

    private static void parseDocument(final InputStream foInputStream,
            final FODocumentParser documentParser, final FOUserAgent userAgent)
            throws FOPException, LoadingException {
        try {
            documentParser.parse(foInputStream, userAgent);
        } finally {
            closeStream(foInputStream);
        }
    }

    private static void endStructureTreeSequence(
            final TransformerHandler tHandler) throws SAXException {
        tHandler.endElement("", STRUCTURE_TREE_SEQUENCE_NAME,
                STRUCTURE_TREE_SEQUENCE_NAME);
        tHandler.endDocument();
    }

    private static Diff createDiff(final DOMResult expected,
            final DOMResult actual) {
        final Diff diff = new Diff(getDocument(expected), getDocument(actual));
        return diff;
    }

    private static Document getDocument(final DOMResult result) {
        return (Document) result.getNode();
    }
}
