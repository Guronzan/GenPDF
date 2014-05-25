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

import java.util.Locale;

import org.apache.fop.accessibility.StructureTreeElement;
import org.apache.fop.accessibility.StructureTreeEventHandler;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.extensions.ExtensionElementMapping;
import org.apache.fop.fo.flow.AbstractGraphics;
import org.apache.fop.fo.flow.BasicLink;
import org.apache.fop.fo.flow.Block;
import org.apache.fop.fo.flow.BlockContainer;
import org.apache.fop.fo.flow.Character;
import org.apache.fop.fo.flow.ExternalGraphic;
import org.apache.fop.fo.flow.Footnote;
import org.apache.fop.fo.flow.FootnoteBody;
import org.apache.fop.fo.flow.Inline;
import org.apache.fop.fo.flow.InstreamForeignObject;
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
import org.apache.fop.fo.flow.table.TableFooter;
import org.apache.fop.fo.flow.table.TableHeader;
import org.apache.fop.fo.flow.table.TableRow;
import org.apache.fop.fo.pagination.Flow;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.StaticContent;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.apache.fop.util.XMLConstants;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A bridge between {@link FOEventHandler} and {@link StructureTreeEventHandler}
 * .
 */
class StructureTreeEventTrigger extends FOEventHandler {

    private final StructureTreeEventHandler structureTreeEventHandler;

    public StructureTreeEventTrigger(
            final StructureTreeEventHandler structureTreeEventHandler) {
        this.structureTreeEventHandler = structureTreeEventHandler;
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startPageSequence(final PageSequence pageSeq) {
        Locale locale = null;
        if (pageSeq.getLanguage() != null) {
            if (pageSeq.getCountry() != null) {
                locale = new Locale(pageSeq.getLanguage(), pageSeq.getCountry());
            } else {
                locale = new Locale(pageSeq.getLanguage());
            }
        }
        final String role = pageSeq.getCommonAccessibility().getRole();
        this.structureTreeEventHandler.startPageSequence(locale, role);
    }

    @Override
    public void endPageSequence(final PageSequence pageSeq) {
        this.structureTreeEventHandler.endPageSequence();
    }

    @Override
    public void startPageNumber(final PageNumber pagenum) {
        startElementWithID(pagenum);
    }

    @Override
    public void endPageNumber(final PageNumber pagenum) {
        endElement(pagenum);
    }

    @Override
    public void startPageNumberCitation(final PageNumberCitation pageCite) {
        startElementWithID(pageCite);
    }

    @Override
    public void endPageNumberCitation(final PageNumberCitation pageCite) {
        endElement(pageCite);
    }

    @Override
    public void startPageNumberCitationLast(
            final PageNumberCitationLast pageLast) {
        startElementWithID(pageLast);
    }

    @Override
    public void endPageNumberCitationLast(final PageNumberCitationLast pageLast) {
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
        final AttributesImpl attributes = new AttributesImpl();
        final int colSpan = tc.getNumberColumnsSpanned();
        if (colSpan > 1) {
            addNoNamespaceAttribute(attributes, "number-columns-spanned",
                    Integer.toString(colSpan));
        }
        startElement(tc, attributes);
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
        startElementWithID(basicLink);
    }

    @Override
    public void endLink(final BasicLink basicLink) {
        endElement(basicLink);
    }

    @Override
    public void image(final ExternalGraphic eg) {
        startElementWithIDAndAltText(eg);
        endElement(eg);
    }

    @Override
    public void startInstreamForeignObject(final InstreamForeignObject ifo) {
        startElementWithIDAndAltText(ifo);
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
    public void startWrapper(final Wrapper wrapper) {
        startElement(wrapper);
    }

    @Override
    public void endWrapper(final Wrapper wrapper) {
        endElement(wrapper);
    }

    @Override
    public void character(final Character c) {
        startElementWithID(c);
        endElement(c);
    }

    @Override
    public void characters(final FOText foText) {
        startElementWithID(foText);
        endElement(foText);
    }

    private void startElement(final FONode node) {
        startElement(node, new AttributesImpl());
    }

    private void startElementWithID(final FONode node) {
        final AttributesImpl attributes = new AttributesImpl();
        final String localName = node.getLocalName();
        if (node instanceof CommonAccessibilityHolder) {
            addRole((CommonAccessibilityHolder) node, attributes);
        }
        node.setStructureTreeElement(this.structureTreeEventHandler
                .startReferencedNode(localName, attributes));
    }

    private void startElementWithIDAndAltText(final AbstractGraphics node) {
        final AttributesImpl attributes = new AttributesImpl();
        final String localName = node.getLocalName();
        addRole(node, attributes);
        addAttribute(attributes, ExtensionElementMapping.URI, "alt-text",
                ExtensionElementMapping.STANDARD_PREFIX, node.getAltText());
        node.setStructureTreeElement(this.structureTreeEventHandler
                .startImageNode(localName, attributes));
    }

    private StructureTreeElement startElement(final FONode node,
            final AttributesImpl attributes) {
        final String localName = node.getLocalName();
        if (node instanceof CommonAccessibilityHolder) {
            addRole((CommonAccessibilityHolder) node, attributes);
        }
        return this.structureTreeEventHandler.startNode(localName, attributes);
    }

    private void addNoNamespaceAttribute(final AttributesImpl attributes,
            final String name, final String value) {
        attributes.addAttribute("", name, name, XMLConstants.CDATA, value);
    }

    private void addAttribute(final AttributesImpl attributes,
            final String namespace, final String localName,
            final String prefix, final String value) {
        assert namespace.length() > 0 && prefix.length() > 0;
        final String qualifiedName = prefix + ":" + localName;
        attributes.addAttribute(namespace, localName, qualifiedName,
                XMLConstants.CDATA, value);
    }

    private void addRole(final CommonAccessibilityHolder node,
            final AttributesImpl attributes) {
        final String role = node.getCommonAccessibility().getRole();
        if (role != null) {
            addNoNamespaceAttribute(attributes, "role", role);
        }
    }

    private void endElement(final FONode node) {
        final String localName = node.getLocalName();
        this.structureTreeEventHandler.endNode(localName);
    }

}
