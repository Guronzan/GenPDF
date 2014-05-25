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

/* $Id: RtfSection.java 1330838 2012-04-26 13:18:53Z gadams $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * <p>
 * Models a section in an RTF document
 * </p>
 *
 * <p>
 * This work was authored by Bertrand Delacretaz (bdelacretaz@codeconsult.ch).
 * </p>
 */

public class RtfSection extends RtfContainer implements IRtfParagraphContainer,
        IRtfTableContainer, IRtfListContainer, IRtfExternalGraphicContainer,
        IRtfBeforeContainer, IRtfParagraphKeepTogetherContainer,
        IRtfAfterContainer, IRtfJforCmdContainer, IRtfTextrunContainer {
    private RtfParagraph paragraph;
    private RtfTable table;
    private RtfList list;
    private RtfExternalGraphic externalGraphic;
    private RtfBefore before;
    private RtfAfter after;
    private RtfJforCmd jforCmd;

    /** Create an RTF container as a child of given container */
    RtfSection(final RtfDocumentArea parent, final Writer w) throws IOException {
        super(parent, w);
    }

    /**
     * Start a new external graphic after closing current paragraph, list and
     * table
     * 
     * @return new RtfExternalGraphic object
     * @throws IOException
     *             for I/O problems
     */
    @Override
    public RtfExternalGraphic newImage() throws IOException {
        closeAll();
        this.externalGraphic = new RtfExternalGraphic(this, this.writer);
        return this.externalGraphic;
    }

    /**
     * Start a new paragraph after closing current paragraph, list and table
     * 
     * @param attrs
     *            attributes for new RtfParagraph
     * @return new RtfParagraph object
     * @throws IOException
     *             for I/O problems
     */
    @Override
    public RtfParagraph newParagraph(final RtfAttributes attrs)
            throws IOException {
        closeAll();
        this.paragraph = new RtfParagraph(this, this.writer, attrs);
        return this.paragraph;
    }

    /**
     * Close current paragraph if any and start a new one with default
     * attributes
     * 
     * @return new RtfParagraph
     * @throws IOException
     *             for I/O problems
     */
    @Override
    public RtfParagraph newParagraph() throws IOException {
        return newParagraph(null);
    }

    /**
     * Close current paragraph if any and start a new one
     * 
     * @return new RtfParagraphKeepTogether
     * @throws IOException
     *             for I/O problems
     */
    @Override
    public RtfParagraphKeepTogether newParagraphKeepTogether()
            throws IOException {
        return new RtfParagraphKeepTogether(this, this.writer);
    }

    /**
     * Start a new table after closing current paragraph, list and table
     * 
     * @param tc
     *            Table context used for number-columns-spanned attribute (added
     *            by Boris Poudérous on july 2002)
     * @return new RtfTable object
     * @throws IOException
     *             for I/O problems
     */
    @Override
    public RtfTable newTable(final ITableColumnsInfo tc) throws IOException {
        closeAll();
        this.table = new RtfTable(this, this.writer, tc);
        return this.table;
    }

    /**
     * Start a new table after closing current paragraph, list and table
     * 
     * @param attrs
     *            attributes of new RtfTable
     * @param tc
     *            Table context used for number-columns-spanned attribute (added
     *            by Boris Poudérous on july 2002)
     * @return new RtfTable object
     * @throws IOException
     *             for I/O problems
     */
    @Override
    public RtfTable newTable(final RtfAttributes attrs,
            final ITableColumnsInfo tc) throws IOException {
        closeAll();
        this.table = new RtfTable(this, this.writer, attrs, tc);
        return this.table;
    }

    /**
     * Start a new list after closing current paragraph, list and table
     * 
     * @param attrs
     *            attributes of new RftList object
     * @return new RtfList
     * @throws IOException
     *             for I/O problems
     */
    @Override
    public RtfList newList(final RtfAttributes attrs) throws IOException {
        closeAll();
        this.list = new RtfList(this, this.writer, attrs);
        return this.list;
    }

    /**
     * IRtfBeforeContainer
     * 
     * @param attrs
     *            attributes of new RtfBefore object
     * @return new RtfBefore object
     * @throws IOException
     *             for I/O problems
     */
    @Override
    public RtfBefore newBefore(final RtfAttributes attrs) throws IOException {
        closeAll();
        this.before = new RtfBefore(this, this.writer, attrs);
        return this.before;
    }

    /**
     * IRtfAfterContainer
     * 
     * @param attrs
     *            attributes of new RtfAfter object
     * @return new RtfAfter object
     * @throws IOException
     *             for I/O problems
     */
    @Override
    public RtfAfter newAfter(final RtfAttributes attrs) throws IOException {
        closeAll();
        this.after = new RtfAfter(this, this.writer, attrs);
        return this.after;
    }

    /**
     *
     * @param attrs
     *            attributes of new RtfJforCmd
     * @return the new RtfJforCmd
     * @throws IOException
     *             for I/O problems
     */
    @Override
    public RtfJforCmd newJforCmd(final RtfAttributes attrs) throws IOException {
        this.jforCmd = new RtfJforCmd(this, this.writer, attrs);
        return this.jforCmd;
    }

    /**
     * Can be overridden to write RTF prefix code, what comes before our
     * children
     * 
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeRtfPrefix() throws IOException {
        writeAttributes(this.attrib, RtfPage.PAGE_ATTR);
        newLine();
        writeControlWord("sectd");
    }

    /**
     * Can be overridden to write RTF suffix code, what comes after our children
     * 
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeRtfSuffix() throws IOException {
        // write suffix /sect only if this section is not last section (see bug
        // #51484)
        final List siblings = this.parent.getChildren();
        if (siblings.indexOf(this) + 1 < siblings.size()) {
            writeControlWord("sect");
        }
    }

    private void closeCurrentTable() throws IOException {
        if (this.table != null) {
            this.table.close();
        }
    }

    private void closeCurrentParagraph() throws IOException {
        if (this.paragraph != null) {
            this.paragraph.close();
        }
    }

    private void closeCurrentList() throws IOException {
        if (this.list != null) {
            this.list.close();
        }
    }

    private void closeCurrentExternalGraphic() throws IOException {
        if (this.externalGraphic != null) {
            this.externalGraphic.close();
        }
    }

    private void closeCurrentBefore() throws IOException {
        if (this.before != null) {
            this.before.close();
        }
    }

    private void closeAll() throws IOException {
        closeCurrentTable();
        closeCurrentParagraph();
        closeCurrentList();
        closeCurrentExternalGraphic();
        closeCurrentBefore();
    }

    /**
     * Returns the current RtfTextrun.
     * 
     * @return Current RtfTextrun
     * @throws IOException
     *             Thrown when an IO-problem occurs.
     */
    @Override
    public RtfTextrun getTextrun() throws IOException {
        return RtfTextrun.getTextrun(this, this.writer, null);
    }
}
