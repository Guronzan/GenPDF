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

/* $Id: MIFHandler.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.render.mif;

import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.flow.BasicLink;
import org.apache.fop.fo.flow.Block;
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
import org.apache.fop.fo.flow.table.Table;
import org.apache.fop.fo.flow.table.TableBody;
import org.apache.fop.fo.flow.table.TableCell;
import org.apache.fop.fo.flow.table.TableColumn;
import org.apache.fop.fo.flow.table.TableFooter;
import org.apache.fop.fo.flow.table.TableHeader;
import org.apache.fop.fo.flow.table.TableRow;
import org.apache.fop.fo.pagination.Flow;
import org.apache.fop.fo.pagination.PageSequence;
import org.apache.fop.fo.pagination.PageSequenceMaster;
import org.apache.fop.fo.pagination.SimplePageMaster;
import org.apache.fop.fo.pagination.StaticContent;
import org.apache.fop.fonts.FontSetup;
import org.apache.fop.render.DefaultFontResolver;
import org.xml.sax.SAXException;

// TODO: do we really want every method throwing a SAXException

/**
 * The MIF Handler. This generates MIF output using the structure events from
 * the FO Tree sent to this structure handler. This builds an MIF file and
 * writes it to the output.
 */
@Slf4j
public class MIFHandler extends FOEventHandler {

    /** the MIFFile instance */
    protected MIFFile mifFile;

    /** the OutputStream to write to */
    protected OutputStream outStream;

    // current state elements
    private MIFElement textFlow;
    private MIFElement para;

    /**
     * Creates a new MIF handler on a given OutputStream.
     *
     * @param ua
     *            FOUserAgent instance for this process
     * @param os
     *            OutputStream to write to
     */
    public MIFHandler(final FOUserAgent ua, final OutputStream os) {
        super(ua);
        this.outStream = os;
        final boolean base14Kerning = false; // TODO - FIXME
        FontSetup.setup(this.fontInfo, null, new DefaultFontResolver(ua),
                base14Kerning);
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws SAXException {
        log.error("The MIF Handler is non-functional at this time. Please help resurrect it!");
        this.mifFile = new MIFFile();
        try {
            this.mifFile.output(this.outStream);
        } catch (final IOException ioe) {
            throw new SAXException(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {
        // finish all open elements
        this.mifFile.finish(true);
        try {
            this.mifFile.output(this.outStream);
            this.outStream.flush();
        } catch (final IOException ioe) {
            throw new SAXException(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final PageSequence pageSeq) {
        // get the layout master set
        // setup the pages for this sequence
        final String name = pageSeq.getMasterReference();
        final SimplePageMaster spm = pageSeq.getRoot().getLayoutMasterSet()
                .getSimplePageMaster(name);
        if (spm == null) {
            final PageSequenceMaster psm = pageSeq.getRoot()
                    .getLayoutMasterSet().getPageSequenceMaster(name);
        } else {
            // create simple master with regions
            MIFElement prop = new MIFElement("PageType");
            prop.setValue("BodyPage");

            final MIFElement page = new MIFElement("Page");
            page.addElement(prop);

            prop = new MIFElement("PageBackground");
            prop.setValue("'Default'");
            page.addElement(prop);

            // build regions
            MIFElement textRect = new MIFElement("TextRect");
            prop = new MIFElement("ID");
            prop.setValue("1");
            textRect.addElement(prop);
            prop = new MIFElement("ShapeRect");
            prop.setValue("0.0 841.889 453.543 0.0");
            textRect.addElement(prop);
            page.addElement(textRect);

            textRect = new MIFElement("TextRect");
            prop = new MIFElement("ID");
            prop.setValue("2");
            textRect.addElement(prop);
            prop = new MIFElement("ShapeRect");
            prop.setValue("0.0 841.889 453.543 187.65");
            textRect.addElement(prop);
            page.addElement(textRect);

            this.mifFile.addPage(page);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endPageSequence(final PageSequence pageSeq) {
    }

    /** {@inheritDoc} */
    @Override
    public void startFlow(final Flow fl) {
        // start text flow in body region
        this.textFlow = new MIFElement("TextFlow");
    }

    /** {@inheritDoc} */
    @Override
    public void endFlow(final Flow fl) {
        this.textFlow.finish(true);
        this.mifFile.addElement(this.textFlow);
        this.textFlow = null;
    }

    /** {@inheritDoc} */
    @Override
    public void startBlock(final Block bl) {
        this.para = new MIFElement("Para");
        // get font
        this.textFlow.addElement(this.para);
    }

    /** {@inheritDoc} */
    @Override
    public void endBlock(final Block bl) {
        this.para.finish(true);
        this.para = null;
    }

    /** {@inheritDoc} */
    @Override
    public void startInline(final Inline inl) {
    }

    /** {@inheritDoc} */
    @Override
    public void endInline(final Inline inl) {
    }

    /** {@inheritDoc} */
    @Override
    public void startTable(final Table tbl) {
    }

    /** {@inheritDoc} */
    @Override
    public void endTable(final Table tbl) {
    }

    /** {@inheritDoc} */
    @Override
    public void startColumn(final TableColumn tc) {
    }

    /** {@inheritDoc} */
    @Override
    public void endColumn(final TableColumn tc) {
    }

    /** {@inheritDoc} */
    @Override
    public void startHeader(final TableHeader th) {
    }

    /** {@inheritDoc} */
    @Override
    public void endHeader(final TableHeader th) {
    }

    /** {@inheritDoc} */
    @Override
    public void startFooter(final TableFooter tf) {
    }

    /** {@inheritDoc} */
    @Override
    public void endFooter(final TableFooter tf) {
    }

    /** {@inheritDoc} */
    @Override
    public void startBody(final TableBody tb) {
    }

    /** {@inheritDoc} */
    @Override
    public void endBody(final TableBody tb) {
    }

    /** {@inheritDoc} */
    @Override
    public void startRow(final TableRow tr) {
    }

    /** {@inheritDoc} */
    @Override
    public void endRow(final TableRow tr) {
    }

    /** {@inheritDoc} */
    @Override
    public void startCell(final TableCell tc) {
    }

    /** {@inheritDoc} */
    @Override
    public void endCell(final TableCell tc) {
    }

    /** {@inheritDoc} */
    @Override
    public void startList(final ListBlock lb) {
    }

    /** {@inheritDoc} */
    @Override
    public void endList(final ListBlock lb) {
    }

    /** {@inheritDoc} */
    @Override
    public void startListItem(final ListItem li) {
    }

    /** {@inheritDoc} */
    @Override
    public void endListItem(final ListItem li) {
    }

    /** {@inheritDoc} */
    @Override
    public void startListLabel(final ListItemLabel listItemLabel) {
    }

    /** {@inheritDoc} */
    @Override
    public void endListLabel(final ListItemLabel listItemLabel) {
    }

    /** {@inheritDoc} */
    @Override
    public void startListBody(final ListItemBody listItemBody) {
    }

    /** {@inheritDoc} */
    @Override
    public void endListBody(final ListItemBody listItemBody) {
    }

    /** {@inheritDoc} */
    @Override
    public void startStatic(final StaticContent staticContent) {
    }

    /** {@inheritDoc} */
    @Override
    public void endStatic(final StaticContent staticContent) {
    }

    /** {@inheritDoc} */
    @Override
    public void startMarkup() {
    }

    /** {@inheritDoc} */
    @Override
    public void endMarkup() {
    }

    /** {@inheritDoc} */
    @Override
    public void startLink(final BasicLink basicLink) {
    }

    /** {@inheritDoc} */
    @Override
    public void endLink(final BasicLink basicLink) {
    }

    /** {@inheritDoc} */
    @Override
    public void image(final ExternalGraphic eg) {
    }

    /** {@inheritDoc} */
    @Override
    public void pageRef() {
    }

    /** {@inheritDoc} */
    @Override
    public void startInstreamForeignObject(final InstreamForeignObject ifo) {
    }

    /** {@inheritDoc} */
    @Override
    public void endInstreamForeignObject(final InstreamForeignObject ifo) {
    }

    /** {@inheritDoc} */
    @Override
    public void startFootnote(final Footnote footnote) {
    }

    /** {@inheritDoc} */
    @Override
    public void endFootnote(final Footnote footnote) {
    }

    /** {@inheritDoc} */
    @Override
    public void startFootnoteBody(final FootnoteBody body) {
    }

    /** {@inheritDoc} */
    @Override
    public void endFootnoteBody(final FootnoteBody body) {
    }

    /** {@inheritDoc} */
    @Override
    public void startLeader(final Leader l) {
    }

    /** {@inheritDoc} */
    @Override
    public void endLeader(final Leader l) {
    }

    @Override
    public void character(final Character c) {
        appendCharacters(new String(new char[] { c.getCharacter() }));
    }

    /** {@inheritDoc} */
    @Override
    public void characters(final FOText foText) {
        appendCharacters(foText.getCharSequence().toString());
    }

    /** {@inheritDoc} */
    @Override
    public void startPageNumber(final PageNumber pagenum) {
    }

    /** {@inheritDoc} */
    @Override
    public void endPageNumber(final PageNumber pagenum) {
    }

    private void appendCharacters(String str) {
        if (this.para != null) {
            str = str.trim();
            // break into nice length chunks
            if (str.length() == 0) {
                return;
            }
            final MIFElement line = new MIFElement("ParaLine");
            MIFElement prop = new MIFElement("TextRectID");
            prop.setValue("2");
            line.addElement(prop);
            prop = new MIFElement("String");
            prop.setValue("\"" + str + "\"");
            line.addElement(prop);
            this.para.addElement(line);
        }
    }
}
