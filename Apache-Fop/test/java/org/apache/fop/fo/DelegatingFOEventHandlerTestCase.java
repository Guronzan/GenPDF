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

/* $Id$ */

package org.apache.fop.fo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.FODocumentParser.FOEventHandlerFactory;
import org.apache.fop.fo.flow.BasicLink;
import org.apache.fop.fo.flow.Block;
import org.apache.fop.fo.flow.BlockContainer;
import org.apache.fop.fo.flow.Character;
import org.apache.fop.fo.flow.ExternalGraphic;
import org.apache.fop.fo.flow.Footnote;
import org.apache.fop.fo.flow.FootnoteBody;
import org.apache.fop.fo.flow.Inline;
import org.apache.fop.fo.flow.InstreamForeignObject;
import org.apache.fop.fo.flow.Leader;
import org.apache.fop.fo.flow.ListBlock;
import org.apache.fop.fo.flow.ListItem;
import org.apache.fop.fo.flow.ListItemBody;
import org.apache.fop.fo.flow.ListItemLabel;
import org.apache.fop.fo.flow.PageNumber;
import org.apache.fop.fo.flow.PageNumberCitation;
import org.apache.fop.fo.flow.PageNumberCitationLast;
import org.apache.fop.fo.flow.Wrapper;
import org.apache.fop.fo.flow.table.Table;
import org.apache.fop.fo.flow.table.TableBody;
import org.apache.fop.fo.flow.table.TableCell;
import org.apache.fop.fo.flow.table.TableColumn;
import org.apache.fop.fo.flow.table.TableFooter;
import org.apache.fop.fo.flow.table.TableHeader;
import org.apache.fop.fo.flow.table.TableRow;
import org.apache.fop.fo.pagination.Flow;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.Root;
import org.apache.fop.fo.pagination.StaticContent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests that {@link DelegatingFOEventHandler} does forward every event to its
 * delegate event handler.
 */
@Slf4j
public class DelegatingFOEventHandlerTestCase {

    private InputStream document;

    private List<String> expectedEvents;

    private List<String> actualEvents;

    private FODocumentParser documentParser;

    private class DelegatingFOEventHandlerTester extends FOEventHandler {

        DelegatingFOEventHandlerTester(final FOUserAgent foUserAgent) {
            super(foUserAgent);
        }

        private final StringBuilder eventBuilder = new StringBuilder();

        @Override
        public void startDocument() throws SAXException {
            DelegatingFOEventHandlerTestCase.this.actualEvents
                    .add("start document");
        }

        @Override
        public void endDocument() throws SAXException {
            DelegatingFOEventHandlerTestCase.this.actualEvents
                    .add("end   document");
        }

        @Override
        public void startRoot(final Root root) {
            startElement(root);
        }

        @Override
        public void endRoot(final Root root) {
            endElement(root);
        }

        @Override
        public void startPageSequence(final PageSequence pageSeq) {
            startElement(pageSeq);
        }

        @Override
        public void endPageSequence(final PageSequence pageSeq) {
            endElement(pageSeq);
        }

        @Override
        public void startPageNumber(final PageNumber pagenum) {
            startElement(pagenum);
        }

        @Override
        public void endPageNumber(final PageNumber pagenum) {
            endElement(pagenum);
        }

        @Override
        public void startPageNumberCitation(final PageNumberCitation pageCite) {
            startElement(pageCite);
        }

        @Override
        public void endPageNumberCitation(final PageNumberCitation pageCite) {
            endElement(pageCite);
        }

        @Override
        public void startPageNumberCitationLast(
                final PageNumberCitationLast pageLast) {
            startElement(pageLast);
        }

        @Override
        public void endPageNumberCitationLast(
                final PageNumberCitationLast pageLast) {
            endElement(pageLast);
        }

        @Override
        public void startFlow(final Flow fl) {
            startElement(fl);
        }

        @Override
        public void endFlow(final Flow fl) {
            endElement(fl);
        }

        @Override
        public void startBlock(final Block bl) {
            startElement(bl);
        }

        @Override
        public void endBlock(final Block bl) {
            endElement(bl);
        }

        @Override
        public void startBlockContainer(final BlockContainer blc) {
            startElement(blc);
        }

        @Override
        public void endBlockContainer(final BlockContainer blc) {
            endElement(blc);
        }

        @Override
        public void startInline(final Inline inl) {
            startElement(inl);
        }

        @Override
        public void endInline(final Inline inl) {
            endElement(inl);
        }

        @Override
        public void startTable(final Table tbl) {
            startElement(tbl);
        }

        @Override
        public void endTable(final Table tbl) {
            endElement(tbl);
        }

        @Override
        public void startColumn(final TableColumn tc) {
            startElement(tc);
        }

        @Override
        public void endColumn(final TableColumn tc) {
            endElement(tc);
        }

        @Override
        public void startHeader(final TableHeader header) {
            startElement(header);
        }

        @Override
        public void endHeader(final TableHeader header) {
            endElement(header);
        }

        @Override
        public void startFooter(final TableFooter footer) {
            startElement(footer);
        }

        @Override
        public void endFooter(final TableFooter footer) {
            endElement(footer);
        }

        @Override
        public void startBody(final TableBody body) {
            startElement(body);
        }

        @Override
        public void endBody(final TableBody body) {
            endElement(body);
        }

        @Override
        public void startRow(final TableRow tr) {
            startElement(tr);
        }

        @Override
        public void endRow(final TableRow tr) {
            endElement(tr);
        }

        @Override
        public void startCell(final TableCell tc) {
            startElement(tc);
        }

        @Override
        public void endCell(final TableCell tc) {
            endElement(tc);
        }

        @Override
        public void startList(final ListBlock lb) {
            startElement(lb);
        }

        @Override
        public void endList(final ListBlock lb) {
            endElement(lb);
        }

        @Override
        public void startListItem(final ListItem li) {
            startElement(li);
        }

        @Override
        public void endListItem(final ListItem li) {
            endElement(li);
        }

        @Override
        public void startListLabel(final ListItemLabel listItemLabel) {
            startElement(listItemLabel);
        }

        @Override
        public void endListLabel(final ListItemLabel listItemLabel) {
            endElement(listItemLabel);
        }

        @Override
        public void startListBody(final ListItemBody listItemBody) {
            startElement(listItemBody);
        }

        @Override
        public void endListBody(final ListItemBody listItemBody) {
            endElement(listItemBody);
        }

        @Override
        public void startStatic(final StaticContent staticContent) {
            startElement(staticContent);
        }

        @Override
        public void endStatic(final StaticContent statisContent) {
            endElement(statisContent);
        }

        @Override
        public void startLink(final BasicLink basicLink) {
            startElement(basicLink);
        }

        @Override
        public void endLink(final BasicLink basicLink) {
            endElement(basicLink);
        }

        @Override
        public void image(final ExternalGraphic eg) {
            startElement(eg);
            endElement(eg);
        }

        @Override
        public void startInstreamForeignObject(final InstreamForeignObject ifo) {
            startElement(ifo);
        }

        @Override
        public void endInstreamForeignObject(final InstreamForeignObject ifo) {
            endElement(ifo);
        }

        @Override
        public void startFootnote(final Footnote footnote) {
            startElement(footnote);
        }

        @Override
        public void endFootnote(final Footnote footnote) {
            endElement(footnote);
        }

        @Override
        public void startFootnoteBody(final FootnoteBody body) {
            startElement(body);
        }

        @Override
        public void endFootnoteBody(final FootnoteBody body) {
            endElement(body);
        }

        @Override
        public void startLeader(final Leader l) {
            startElement(l);
        }

        @Override
        public void endLeader(final Leader l) {
            endElement(l);
        }

        @Override
        public void startWrapper(final Wrapper wrapper) {
            startElement(wrapper);
        }

        @Override
        public void endWrapper(final Wrapper wrapper) {
            endElement(wrapper);
        }

        @Override
        public void character(final Character c) {
            startElement(c);
            endElement(c);
        }

        private void startElement(final FObj node) {
            addEvent("start ", node);
        }

        private void endElement(final FObj node) {
            addEvent("end   ", node);
        }

        private void addEvent(final String event, final FObj node) {
            this.eventBuilder.append(event);
            this.eventBuilder.append(node.getLocalName());
            addID(node);
            DelegatingFOEventHandlerTestCase.this.actualEvents
                    .add(this.eventBuilder.toString());
            this.eventBuilder.setLength(0);
        }

        private void addID(final FObj node) {
            final String id = node.getId();
            if (id != null && id.length() > 0) {
                this.eventBuilder.append(" id=\"");
                this.eventBuilder.append(id);
                this.eventBuilder.append("\"");
            }
        }
    }

    @Before
    public void setUp() {
        setUpEvents();
        loadDocument();
        createDocumentParser();
    }

    private void setUpEvents() {
        loadDocument();
        loadExpectedEvents();
        this.actualEvents = new ArrayList<String>(this.expectedEvents.size());
    }

    private void loadDocument() {
        this.document = getClass().getResourceAsStream("complete_document.fo");
    }

    private void loadExpectedEvents() {
        this.expectedEvents = new ArrayList<String>();
        final InputStream xslt = getClass().getResourceAsStream(
                "extract-events.xsl");
        try {
            runXSLT(xslt);
        } finally {
            closeStream(xslt);
            closeStream(this.document);
        }
    }

    private void runXSLT(final InputStream xslt) {
        final Transformer transformer = createTransformer(xslt);
        final Source fo = new StreamSource(this.document);
        final Result result = createTransformOutputHandler();
        try {
            transformer.transform(fo, result);
        } catch (final TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private Transformer createTransformer(final InputStream xslt) {
        final TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        try {
            return transformerFactory.newTransformer(new StreamSource(xslt));
        } catch (final TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private Result createTransformOutputHandler() {
        return new SAXResult(new DefaultHandler() {

            private final StringBuilder event = new StringBuilder();

            @Override
            public void startElement(final String uri, final String localName,
                    final String qName, final Attributes attributes)
                    throws SAXException {
                this.event.setLength(0);
            }

            @Override
            public void characters(final char[] ch, final int start,
                    final int length) throws SAXException {
                this.event.append(ch, start, length);
            }

            @Override
            public void endElement(final String uri, final String localName,
                    final String qName) throws SAXException {
                DelegatingFOEventHandlerTestCase.this.expectedEvents
                        .add(this.event.toString());
            }

        });
    }

    private void closeStream(final InputStream stream) {
        try {
            stream.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createDocumentParser() {
        this.documentParser = FODocumentParser
                .newInstance(new FOEventHandlerFactory() {

                    @Override
                    public FOEventHandler newFOEventHandler(
                            final FOUserAgent foUserAgent) {
                        return new DelegatingFOEventHandler(
                                new DelegatingFOEventHandlerTester(foUserAgent)) {
                        };
                    }
                });
    }

    @Test
    public void testFOEventHandler() throws Exception {
        this.documentParser.parse(this.document);
        assertArrayEquals(this.expectedEvents.toArray(),
                this.actualEvents.toArray());
    }

    @After
    public void unloadDocument() throws IOException {
        this.document.close();
    }

    /**
     * Prints the given list to {@code System.out}, each element on a new line.
     * For debugging purpose.
     *
     * @param list
     *            a list
     */
    public void printList(final List<?> list) {
        for (final Object element : list) {
            log.info(element.toString());
        }
    }

}
