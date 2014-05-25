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

/* $Id: RtfTable.java 1311120 2012-04-08 23:48:11Z gadams $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

import java.io.IOException;
import java.io.Writer;

import org.apache.fop.apps.FOPException;

/**
 * <p>
 * Container for RtfRow elements.
 * </p>
 *
 * <p>
 * This work was authored by Bertrand Delacretaz (bdelacretaz@codeconsult.ch).
 * </p>
 */

public class RtfTable extends RtfContainer {
    private RtfTableRow row;
    private int highestRow = 0;
    private Boolean isNestedTable = null;
    private RtfAttributes borderAttributes = null;

    /**
     * Added by Boris Poudérous on 07/22/2002 in order to process
     * number-columns-spanned attribute
     */
    private final ITableColumnsInfo tableContext;

    /** Shows the table depth necessary for nested tables */
    private int nestedTableDepth = 0;

    /** Create an RTF element as a child of given container */
    RtfTable(final IRtfTableContainer parent, final Writer w,
            final ITableColumnsInfo tc) throws IOException {
        super((RtfContainer) parent, w);
        // Line added by Boris Poudérous on 07/22/2002
        this.tableContext = tc;
    }

    /**
     * Create an RTF element as a child of given container Modified by Boris
     * Poudérous in order to process 'number-columns-spanned' attribute
     */
    RtfTable(final IRtfTableContainer parent, final Writer w,
            final RtfAttributes attrs, final ITableColumnsInfo tc)
            throws IOException {
        super((RtfContainer) parent, w, attrs);
        // Line added by Boris Poudérous on 07/22/2002
        this.tableContext = tc;
    }

    /**
     * Close current row if any and start a new one
     * 
     * @return new RtfTableRow
     * @throws IOException
     *             for I/O problems
     */
    public RtfTableRow newTableRow() throws IOException {
        if (this.row != null) {
            this.row.close();
        }

        this.highestRow++;
        this.row = new RtfTableRow(this, this.writer, this.attrib,
                this.highestRow);
        return this.row;
    }

    /**
     * Close current row if any and start a new one
     * 
     * @param attrs
     *            attributs of new RtfTableRow
     * @return new RtfTableRow
     * @throws IOException
     *             for I/O problems
     * @throws FOPException
     *             if attributes cannot be cloned
     */
    public RtfTableRow newTableRow(final RtfAttributes attrs)
            throws IOException, FOPException {
        RtfAttributes attr = null;
        if (this.attrib != null) {
            try {
                attr = (RtfAttributes) this.attrib.clone();
            } catch (final CloneNotSupportedException e) {
                throw new FOPException(e);
            }
            attr.set(attrs);
        } else {
            attr = attrs;
        }
        if (this.row != null) {
            this.row.close();
        }
        this.highestRow++;

        this.row = new RtfTableRow(this, this.writer, attr, this.highestRow);
        return this.row;
    }

    /**
     * Overridden to write RTF prefix code, what comes before our children
     * 
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeRtfPrefix() throws IOException {
        if (isNestedTable()) {
            writeControlWordNS("pard");
        }

        writeGroupMark(true);
    }

    /**
     * Overridden to write RTF suffix code, what comes after our children
     * 
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeRtfSuffix() throws IOException {
        writeGroupMark(false);

        if (isNestedTable()) {
            getRow().writeRowAndCellsDefintions();
        }
    }

    /**
     *
     * @param id
     *            row to check (??)
     * @return true if id is the highestRow
     */
    public boolean isHighestRow(final int id) {
        return this.highestRow == id ? true : false;
    }

    /**
     * Added by Boris Poudérous on 07/22/2002
     * 
     * @return ITableColumnsInfo for this table
     */
    public ITableColumnsInfo getITableColumnsInfo() {
        return this.tableContext;
    }

    private RtfAttributes headerAttribs = null;

    /**
     * Added by Normand Masse Support for table-header attributes (used instead
     * of table attributes)
     * 
     * @param attrs
     *            attributes to be set
     */
    public void setHeaderAttribs(final RtfAttributes attrs) {
        this.headerAttribs = attrs;
    }

    /**
     *
     * @return RtfAttributes of Header
     */
    public RtfAttributes getHeaderAttribs() {
        return this.headerAttribs;
    }

    /**
     * Added by Normand Masse
     * 
     * @return the table-header attributes if they are present, otherwise the
     *         parent's attributes are returned normally.
     */
    @Override
    public RtfAttributes getRtfAttributes() {
        if (this.headerAttribs != null) {
            return this.headerAttribs;
        }

        return super.getRtfAttributes();
    }

    /** @return true if the the table is a nested table */
    public boolean isNestedTable() {
        if (this.isNestedTable == null) {
            RtfElement e = this;
            while (e.parent != null) {
                if (e.parent instanceof RtfTableCell) {
                    this.isNestedTable = Boolean.TRUE;
                    return true;
                }

                e = e.parent;
            }

            this.isNestedTable = false;
        } else {
            return this.isNestedTable.booleanValue();
        }

        return false;
    }

    /**
     *
     * @return Parent row table (for nested tables only)
     */
    public RtfTableRow getRow() {
        RtfElement e = this;
        while (e.parent != null) {
            if (e.parent instanceof RtfTableRow) {
                return (RtfTableRow) e.parent;
            }

            e = e.parent;
        }

        return null;
    }

    /**
     * Sets the nested table depth.
     * 
     * @param nestedTableDepth
     *            the nested table depth
     */
    public void setNestedTableDepth(final int nestedTableDepth) {
        this.nestedTableDepth = nestedTableDepth;
    }

    /**
     * Returns the nested table depth.
     * 
     * @return the nested table depth
     */
    public int getNestedTableDepth() {
        return this.nestedTableDepth;
    }

    /**
     * Sets the RtfAttributes for the borders of the table.
     * 
     * @param attributes
     *            Border attributes of the table.
     */
    public void setBorderAttributes(final RtfAttributes attributes) {
        this.borderAttributes = attributes;
    }

    /**
     * Returns the RtfAttributes for the borders of the table.
     * 
     * @return Border attributes of the table.
     */
    public RtfAttributes getBorderAttributes() {
        return this.borderAttributes;
    }
}
