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

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FormattingResults;
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
import org.apache.fop.fonts.FontInfo;
import org.xml.sax.SAXException;

/**
 * This class delegates all FO events to another FOEventHandler instance.
 */
public abstract class DelegatingFOEventHandler extends FOEventHandler {

    private final FOEventHandler delegate;

    /**
     * Creates a new instance that delegates events to the given object.
     *
     * @param delegate
     *            the object to which all FO events will be forwarded
     */
    public DelegatingFOEventHandler(final FOEventHandler delegate) {
        super(delegate.getUserAgent());
        this.delegate = delegate;
    }

    @Override
    public FOUserAgent getUserAgent() {
        return this.delegate.getUserAgent();
    }

    @Override
    public FontInfo getFontInfo() {
        return this.delegate.getFontInfo();
    }

    @Override
    public void startDocument() throws SAXException {
        this.delegate.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        this.delegate.endDocument();
    }

    @Override
    public void startRoot(final Root root) {
        this.delegate.startRoot(root);
    }

    @Override
    public void endRoot(final Root root) {
        this.delegate.endRoot(root);
    }

    @Override
    public void startPageSequence(final PageSequence pageSeq) {
        this.delegate.startPageSequence(pageSeq);
    }

    @Override
    public void endPageSequence(final PageSequence pageSeq) {
        this.delegate.endPageSequence(pageSeq);
    }

    @Override
    public void startPageNumber(final PageNumber pagenum) {
        this.delegate.startPageNumber(pagenum);
    }

    @Override
    public void endPageNumber(final PageNumber pagenum) {
        this.delegate.endPageNumber(pagenum);
    }

    @Override
    public void startPageNumberCitation(final PageNumberCitation pageCite) {
        this.delegate.startPageNumberCitation(pageCite);
    }

    @Override
    public void endPageNumberCitation(final PageNumberCitation pageCite) {
        this.delegate.endPageNumberCitation(pageCite);
    }

    @Override
    public void startPageNumberCitationLast(
            final PageNumberCitationLast pageLast) {
        this.delegate.startPageNumberCitationLast(pageLast);
    }

    @Override
    public void endPageNumberCitationLast(final PageNumberCitationLast pageLast) {
        this.delegate.endPageNumberCitationLast(pageLast);
    }

    @Override
    public void startFlow(final Flow fl) {
        this.delegate.startFlow(fl);
    }

    @Override
    public void endFlow(final Flow fl) {
        this.delegate.endFlow(fl);
    }

    @Override
    public void startBlock(final Block bl) {
        this.delegate.startBlock(bl);
    }

    @Override
    public void endBlock(final Block bl) {
        this.delegate.endBlock(bl);
    }

    @Override
    public void startBlockContainer(final BlockContainer blc) {
        this.delegate.startBlockContainer(blc);
    }

    @Override
    public void endBlockContainer(final BlockContainer blc) {
        this.delegate.endBlockContainer(blc);
    }

    @Override
    public void startInline(final Inline inl) {
        this.delegate.startInline(inl);
    }

    @Override
    public void endInline(final Inline inl) {
        this.delegate.endInline(inl);
    }

    @Override
    public void startTable(final Table tbl) {
        this.delegate.startTable(tbl);
    }

    @Override
    public void endTable(final Table tbl) {
        this.delegate.endTable(tbl);
    }

    @Override
    public void startColumn(final TableColumn tc) {
        this.delegate.startColumn(tc);
    }

    @Override
    public void endColumn(final TableColumn tc) {
        this.delegate.endColumn(tc);
    }

    @Override
    public void startHeader(final TableHeader header) {
        this.delegate.startHeader(header);
    }

    @Override
    public void endHeader(final TableHeader header) {
        this.delegate.endHeader(header);
    }

    @Override
    public void startFooter(final TableFooter footer) {
        this.delegate.startFooter(footer);
    }

    @Override
    public void endFooter(final TableFooter footer) {
        this.delegate.endFooter(footer);
    }

    @Override
    public void startBody(final TableBody body) {
        this.delegate.startBody(body);
    }

    @Override
    public void endBody(final TableBody body) {
        this.delegate.endBody(body);
    }

    @Override
    public void startRow(final TableRow tr) {
        this.delegate.startRow(tr);
    }

    @Override
    public void endRow(final TableRow tr) {
        this.delegate.endRow(tr);
    }

    @Override
    public void startCell(final TableCell tc) {
        this.delegate.startCell(tc);
    }

    @Override
    public void endCell(final TableCell tc) {
        this.delegate.endCell(tc);
    }

    @Override
    public void startList(final ListBlock lb) {
        this.delegate.startList(lb);
    }

    @Override
    public void endList(final ListBlock lb) {
        this.delegate.endList(lb);
    }

    @Override
    public void startListItem(final ListItem li) {
        this.delegate.startListItem(li);
    }

    @Override
    public void endListItem(final ListItem li) {
        this.delegate.endListItem(li);
    }

    @Override
    public void startListLabel(final ListItemLabel listItemLabel) {
        this.delegate.startListLabel(listItemLabel);
    }

    @Override
    public void endListLabel(final ListItemLabel listItemLabel) {
        this.delegate.endListLabel(listItemLabel);
    }

    @Override
    public void startListBody(final ListItemBody listItemBody) {
        this.delegate.startListBody(listItemBody);
    }

    @Override
    public void endListBody(final ListItemBody listItemBody) {
        this.delegate.endListBody(listItemBody);
    }

    @Override
    public void startStatic(final StaticContent staticContent) {
        this.delegate.startStatic(staticContent);
    }

    @Override
    public void endStatic(final StaticContent statisContent) {
        this.delegate.endStatic(statisContent);
    }

    @Override
    public void startMarkup() {
        this.delegate.startMarkup();
    }

    @Override
    public void endMarkup() {
        this.delegate.endMarkup();
    }

    @Override
    public void startLink(final BasicLink basicLink) {
        this.delegate.startLink(basicLink);
    }

    @Override
    public void endLink(final BasicLink basicLink) {
        this.delegate.endLink(basicLink);
    }

    @Override
    public void image(final ExternalGraphic eg) {
        this.delegate.image(eg);
    }

    @Override
    public void pageRef() {
        this.delegate.pageRef();
    }

    @Override
    public void startInstreamForeignObject(final InstreamForeignObject ifo) {
        this.delegate.startInstreamForeignObject(ifo);
    }

    @Override
    public void endInstreamForeignObject(final InstreamForeignObject ifo) {
        this.delegate.endInstreamForeignObject(ifo);
    }

    @Override
    public void startFootnote(final Footnote footnote) {
        this.delegate.startFootnote(footnote);
    }

    @Override
    public void endFootnote(final Footnote footnote) {
        this.delegate.endFootnote(footnote);
    }

    @Override
    public void startFootnoteBody(final FootnoteBody body) {
        this.delegate.startFootnoteBody(body);
    }

    @Override
    public void endFootnoteBody(final FootnoteBody body) {
        this.delegate.endFootnoteBody(body);
    }

    @Override
    public void startLeader(final Leader l) {
        this.delegate.startLeader(l);
    }

    @Override
    public void endLeader(final Leader l) {
        this.delegate.endLeader(l);
    }

    @Override
    public void startWrapper(final Wrapper wrapper) {
        this.delegate.startWrapper(wrapper);
    }

    @Override
    public void endWrapper(final Wrapper wrapper) {
        this.delegate.endWrapper(wrapper);
    }

    @Override
    public void character(final Character c) {
        this.delegate.character(c);
    }

    @Override
    public void characters(final FOText foText) {
        this.delegate.characters(foText);
    }

    @Override
    public void startExternalDocument(final ExternalDocument document) {
        this.delegate.startExternalDocument(document);
    }

    @Override
    public void endExternalDocument(final ExternalDocument document) {
        this.delegate.endExternalDocument(document);
    }

    @Override
    public FormattingResults getResults() {
        return this.delegate.getResults();
    }

}
