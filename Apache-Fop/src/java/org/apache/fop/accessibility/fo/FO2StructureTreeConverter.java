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

package org.apache.fop.accessibility.fo;

import java.util.Stack;

import org.apache.fop.accessibility.Accessibility;
import org.apache.fop.accessibility.StructureTreeEventHandler;
import org.apache.fop.fo.DelegatingFOEventHandler;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.extensions.ExternalDocument;
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
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.xml.sax.SAXException;

/**
 * Allows to create the structure tree of an FO document, by converting FO
 * events into appropriate structure tree events.
 */
public class FO2StructureTreeConverter extends DelegatingFOEventHandler {

    /** The top of the {@link converters} stack. */
    private FOEventHandler converter;

    private final Stack<FOEventHandler> converters = new Stack<FOEventHandler>();

    private final Stack<FOEventRecorder> tableFooterRecorders = new Stack<FOEventRecorder>();

    private final FOEventHandler structureTreeEventTrigger;

    /** The descendants of some elements like fo:leader must be ignored. */
    private final FOEventHandler eventSwallower = new FOEventHandler() {
    };

    /**
     * Creates a new instance.
     *
     * @param structureTreeEventHandler
     *            the object that will hold the structure tree
     * @param delegate
     *            the FO event handler that must be wrapped by this instance
     */
    public FO2StructureTreeConverter(
            final StructureTreeEventHandler structureTreeEventHandler,
            final FOEventHandler delegate) {
        super(delegate);
        this.structureTreeEventTrigger = new StructureTreeEventTrigger(
                structureTreeEventHandler);
        this.converter = this.structureTreeEventTrigger;
    }

    @Override
    public void startDocument() throws SAXException {
        this.converter.startDocument();
        super.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        this.converter.endDocument();
        super.endDocument();
    }

    @Override
    public void startRoot(final Root root) {
        this.converter.startRoot(root);
        super.startRoot(root);
    }

    @Override
    public void endRoot(final Root root) {
        this.converter.endRoot(root);
        super.endRoot(root);
    }

    @Override
    public void startPageSequence(final PageSequence pageSeq) {
        this.converter.startPageSequence(pageSeq);
        super.startPageSequence(pageSeq);
    }

    @Override
    public void endPageSequence(final PageSequence pageSeq) {
        this.converter.endPageSequence(pageSeq);
        super.endPageSequence(pageSeq);
    }

    @Override
    public void startPageNumber(final PageNumber pagenum) {
        this.converter.startPageNumber(pagenum);
        super.startPageNumber(pagenum);
    }

    @Override
    public void endPageNumber(final PageNumber pagenum) {
        this.converter.endPageNumber(pagenum);
        super.endPageNumber(pagenum);
    }

    @Override
    public void startPageNumberCitation(final PageNumberCitation pageCite) {
        this.converter.startPageNumberCitation(pageCite);
        super.startPageNumberCitation(pageCite);
    }

    @Override
    public void endPageNumberCitation(final PageNumberCitation pageCite) {
        this.converter.endPageNumberCitation(pageCite);
        super.endPageNumberCitation(pageCite);
    }

    @Override
    public void startPageNumberCitationLast(
            final PageNumberCitationLast pageLast) {
        this.converter.startPageNumberCitationLast(pageLast);
        super.startPageNumberCitationLast(pageLast);
    }

    @Override
    public void endPageNumberCitationLast(final PageNumberCitationLast pageLast) {
        this.converter.endPageNumberCitationLast(pageLast);
        super.endPageNumberCitationLast(pageLast);
    }

    @Override
    public void startFlow(final Flow fl) {
        this.converter.startFlow(fl);
        super.startFlow(fl);
    }

    @Override
    public void endFlow(final Flow fl) {
        this.converter.endFlow(fl);
        super.endFlow(fl);
    }

    @Override
    public void startBlock(final Block bl) {
        this.converter.startBlock(bl);
        super.startBlock(bl);
    }

    @Override
    public void endBlock(final Block bl) {
        this.converter.endBlock(bl);
        super.endBlock(bl);
    }

    @Override
    public void startBlockContainer(final BlockContainer blc) {
        this.converter.startBlockContainer(blc);
        super.startBlockContainer(blc);
    }

    @Override
    public void endBlockContainer(final BlockContainer blc) {
        this.converter.endBlockContainer(blc);
        super.endBlockContainer(blc);
    }

    @Override
    public void startInline(final Inline inl) {
        this.converter.startInline(inl);
        super.startInline(inl);
    }

    @Override
    public void endInline(final Inline inl) {
        this.converter.endInline(inl);
        super.endInline(inl);
    }

    @Override
    public void startTable(final Table tbl) {
        this.converter.startTable(tbl);
        this.tableFooterRecorders.push(null);
        super.startTable(tbl);
    }

    @Override
    public void endTable(final Table tbl) {
        final FOEventRecorder tableFooterRecorder = this.tableFooterRecorders
                .pop();
        if (tableFooterRecorder != null) {
            tableFooterRecorder.replay(this.converter);
        }
        this.converter.endTable(tbl);
        super.endTable(tbl);
    }

    @Override
    public void startColumn(final TableColumn tc) {
        this.converter.startColumn(tc);
        super.startColumn(tc);
    }

    @Override
    public void endColumn(final TableColumn tc) {
        this.converter.endColumn(tc);
        super.endColumn(tc);
    }

    @Override
    public void startHeader(final TableHeader header) {
        this.converter.startHeader(header);
        super.startHeader(header);
    }

    @Override
    public void endHeader(final TableHeader header) {
        this.converter.endHeader(header);
        super.endHeader(header);
    }

    @Override
    public void startFooter(final TableFooter footer) {
        this.converters.push(this.converter);
        this.converter = new FOEventRecorder();
        this.converter.startFooter(footer);
        super.startFooter(footer);
    }

    @Override
    public void endFooter(final TableFooter footer) {
        this.converter.endFooter(footer);
        /* Replace the dummy table footer with the real one. */
        this.tableFooterRecorders.pop();
        this.tableFooterRecorders.push((FOEventRecorder) this.converter);
        this.converter = this.converters.pop();
        super.endFooter(footer);
    }

    @Override
    public void startBody(final TableBody body) {
        this.converter.startBody(body);
        super.startBody(body);
    }

    @Override
    public void endBody(final TableBody body) {
        this.converter.endBody(body);
        super.endBody(body);
    }

    @Override
    public void startRow(final TableRow tr) {
        this.converter.startRow(tr);
        super.startRow(tr);
    }

    @Override
    public void endRow(final TableRow tr) {
        this.converter.endRow(tr);
        super.endRow(tr);
    }

    @Override
    public void startCell(final TableCell tc) {
        this.converter.startCell(tc);
        super.startCell(tc);
    }

    @Override
    public void endCell(final TableCell tc) {
        this.converter.endCell(tc);
        super.endCell(tc);
    }

    @Override
    public void startList(final ListBlock lb) {
        this.converter.startList(lb);
        super.startList(lb);
    }

    @Override
    public void endList(final ListBlock lb) {
        this.converter.endList(lb);
        super.endList(lb);
    }

    @Override
    public void startListItem(final ListItem li) {
        this.converter.startListItem(li);
        super.startListItem(li);
    }

    @Override
    public void endListItem(final ListItem li) {
        this.converter.endListItem(li);
        super.endListItem(li);
    }

    @Override
    public void startListLabel(final ListItemLabel listItemLabel) {
        this.converter.startListLabel(listItemLabel);
        super.startListLabel(listItemLabel);
    }

    @Override
    public void endListLabel(final ListItemLabel listItemLabel) {
        this.converter.endListLabel(listItemLabel);
        super.endListLabel(listItemLabel);
    }

    @Override
    public void startListBody(final ListItemBody listItemBody) {
        this.converter.startListBody(listItemBody);
        super.startListBody(listItemBody);
    }

    @Override
    public void endListBody(final ListItemBody listItemBody) {
        this.converter.endListBody(listItemBody);
        super.endListBody(listItemBody);
    }

    @Override
    public void startStatic(final StaticContent staticContent) {
        handleStartArtifact(staticContent);
        this.converter.startStatic(staticContent);
        super.startStatic(staticContent);
    }

    @Override
    public void endStatic(final StaticContent statisContent) {
        this.converter.endStatic(statisContent);
        handleEndArtifact(statisContent);
        super.endStatic(statisContent);
    }

    @Override
    public void startMarkup() {
        this.converter.startMarkup();
        super.startMarkup();
    }

    @Override
    public void endMarkup() {
        this.converter.endMarkup();
        super.endMarkup();
    }

    @Override
    public void startLink(final BasicLink basicLink) {
        this.converter.startLink(basicLink);
        super.startLink(basicLink);
    }

    @Override
    public void endLink(final BasicLink basicLink) {
        this.converter.endLink(basicLink);
        super.endLink(basicLink);
    }

    @Override
    public void image(final ExternalGraphic eg) {
        this.converter.image(eg);
        super.image(eg);
    }

    @Override
    public void pageRef() {
        this.converter.pageRef();
        super.pageRef();
    }

    @Override
    public void startInstreamForeignObject(final InstreamForeignObject ifo) {
        this.converter.startInstreamForeignObject(ifo);
        super.startInstreamForeignObject(ifo);
    }

    @Override
    public void endInstreamForeignObject(final InstreamForeignObject ifo) {
        this.converter.endInstreamForeignObject(ifo);
        super.endInstreamForeignObject(ifo);
    }

    @Override
    public void startFootnote(final Footnote footnote) {
        this.converter.startFootnote(footnote);
        super.startFootnote(footnote);
    }

    @Override
    public void endFootnote(final Footnote footnote) {
        this.converter.endFootnote(footnote);
        super.endFootnote(footnote);
    }

    @Override
    public void startFootnoteBody(final FootnoteBody body) {
        this.converter.startFootnoteBody(body);
        super.startFootnoteBody(body);
    }

    @Override
    public void endFootnoteBody(final FootnoteBody body) {
        this.converter.endFootnoteBody(body);
        super.endFootnoteBody(body);
    }

    @Override
    public void startLeader(final Leader l) {
        this.converters.push(this.converter);
        this.converter = this.eventSwallower;
        this.converter.startLeader(l);
        super.startLeader(l);
    }

    @Override
    public void endLeader(final Leader l) {
        this.converter.endLeader(l);
        this.converter = this.converters.pop();
        super.endLeader(l);
    }

    @Override
    public void startWrapper(final Wrapper wrapper) {
        handleStartArtifact(wrapper);
        this.converter.startWrapper(wrapper);
        super.startWrapper(wrapper);
    }

    @Override
    public void endWrapper(final Wrapper wrapper) {
        this.converter.endWrapper(wrapper);
        handleEndArtifact(wrapper);
        super.endWrapper(wrapper);
    }

    @Override
    public void character(final Character c) {
        this.converter.character(c);
        super.character(c);
    }

    @Override
    public void characters(final FOText foText) {
        this.converter.characters(foText);
        super.characters(foText);
    }

    @Override
    public void startExternalDocument(final ExternalDocument document) {
        this.converter.startExternalDocument(document);
        super.startExternalDocument(document);
    }

    @Override
    public void endExternalDocument(final ExternalDocument document) {
        this.converter.endExternalDocument(document);
        super.endExternalDocument(document);
    }

    private void handleStartArtifact(final CommonAccessibilityHolder fobj) {
        if (isArtifact(fobj)) {
            this.converters.push(this.converter);
            this.converter = this.eventSwallower;
        }
    }

    private void handleEndArtifact(final CommonAccessibilityHolder fobj) {
        if (isArtifact(fobj)) {
            this.converter = this.converters.pop();
        }
    }

    private boolean isArtifact(final CommonAccessibilityHolder fobj) {
        final CommonAccessibility accessibility = fobj.getCommonAccessibility();
        return Accessibility.ROLE_ARTIFACT.equalsIgnoreCase(accessibility
                .getRole());
    }

}
