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

/* $Id: RtfStyleSheetTable.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Singelton of the RTF style sheet table. This class belongs to the
 * <jfor:stylesheet> tag processing.
 * </p>
 *
 * <p>
 * This work was authored by Andreas Putz (a.putz@skynamics.com).
 * </p>
 */
public final class RtfStyleSheetTable {
    // ////////////////////////////////////////////////
    // @@ Symbolic constants
    // ////////////////////////////////////////////////

    /** Start index number for the stylesheet reference table */
    private static int startIndex = 15;

    /** OK status value for attribute handling */
    public static final int STATUS_OK = 0;
    /**
     * Status value for attribute handling, if the stylesheet not found and the
     * stylesheet set to the default stylesheet
     */
    public static final int STATUS_DEFAULT = 1;

    /** Standard style name */
    private static final String STANDARD_STYLE = "Standard";

    // ////////////////////////////////////////////////
    // @@ Singleton
    // ////////////////////////////////////////////////

    /** Singelton instance */
    private static RtfStyleSheetTable instance = null;

    // ////////////////////////////////////////////////
    // @@ Members
    // ////////////////////////////////////////////////

    /** Table of styles */
    private Hashtable styles = null;

    /** Used, style attributes to this vector */
    private Hashtable attrTable = null;

    /** Used, style names to this vector */
    private List nameTable = null;

    /** Default style */
    private String defaultStyleName = STANDARD_STYLE;

    // ////////////////////////////////////////////////
    // @@ Construction
    // ////////////////////////////////////////////////

    /**
     * Constructor.
     */
    private RtfStyleSheetTable() {
        this.styles = new Hashtable();
        this.attrTable = new Hashtable();
        this.nameTable = new java.util.ArrayList();
    }

    /**
     * Singelton.
     *
     * @return The instance of RtfStyleSheetTable
     */
    public static RtfStyleSheetTable getInstance() {
        if (instance == null) {
            instance = new RtfStyleSheetTable();
        }

        return instance;
    }

    // ////////////////////////////////////////////////
    // @@ Member access
    // ////////////////////////////////////////////////

    /**
     * Sets the default style.
     *
     * @param styleName
     *            Name of the default style, defined in the stylesheet
     */
    public void setDefaultStyle(final String styleName) {
        this.defaultStyleName = styleName;
    }

    /**
     * Gets the name of the default style.
     *
     * @return Default style name.
     */
    public String getDefaultStyleName() {
        if (this.attrTable.get(this.defaultStyleName) != null) {
            return this.defaultStyleName;
        }

        if (this.attrTable.get(STANDARD_STYLE) != null) {
            this.defaultStyleName = STANDARD_STYLE;
            return this.defaultStyleName;
        }

        return null;
    }

    // ////////////////////////////////////////////////
    // @@ Public methods
    // ////////////////////////////////////////////////

    /**
     * Adds a style to the table.
     *
     * @param name
     *            Name of style to add
     * @param attrs
     *            Rtf attributes which defines the style
     */
    public void addStyle(final String name, final RtfAttributes attrs) {
        this.nameTable.add(name);
        if (attrs != null) {
            this.attrTable.put(name, attrs);
        }
        this.styles.put(name, new Integer(this.nameTable.size() - 1
                + startIndex));
    }

    /**
     * Adds the style attributes to the given attributes.
     *
     * @param name
     *            Name of style, of which the attributes will copied to attr
     * @param attr
     *            Default rtf attributes
     * @return Status value
     */
    public int addStyleToAttributes(String name, final RtfAttributes attr) {
        // Sets status to ok
        int status = STATUS_OK;

        // Gets the style number from table
        Integer style = (Integer) this.styles.get(name);

        if (style == null && !name.equals(this.defaultStyleName)) {
            // If style not found, and style was not the default style, try the
            // default style
            name = this.defaultStyleName;
            style = (Integer) this.styles.get(name);
            // set status for default style setting
            status = STATUS_DEFAULT;
        }

        // Returns the status for invalid styles
        if (style == null) {
            return status;
        }

        // Adds the attributes to default attributes, if not available in
        // default attributes
        attr.set("cs", style.intValue());

        final Object o = this.attrTable.get(name);
        if (o != null) {
            final RtfAttributes rtfAttr = (RtfAttributes) o;

            for (final Iterator names = rtfAttr.nameIterator(); names.hasNext();) {
                final String attrName = (String) names.next();
                if (!attr.isSet(attrName)) {
                    final Integer i = (Integer) rtfAttr.getValue(attrName);
                    if (i == null) {
                        attr.set(attrName);
                    } else {
                        attr.set(attrName, i.intValue());
                    }
                }
            }
        }
        return status;
    }

    /**
     * Writes the rtf style sheet table.
     *
     * @param header
     *            Rtf header is the parent
     * @throws IOException
     *             On write error
     */
    public void writeStyleSheet(final RtfHeader header) throws IOException {
        if (this.styles == null || this.styles.size() == 0) {
            return;
        }
        header.writeGroupMark(true);
        header.writeControlWord("stylesheet");

        final int number = this.nameTable.size();
        for (int i = 0; i < number; i++) {
            final String name = (String) this.nameTable.get(i);
            header.writeGroupMark(true);
            header.writeControlWord("*\\" + getRtfStyleReference(name));

            final Object o = this.attrTable.get(name);
            if (o != null) {
                header.writeAttributes((RtfAttributes) o, RtfText.ATTR_NAMES);
                header.writeAttributes((RtfAttributes) o, RtfText.ALIGNMENT);
            }

            header.write(name + ";");
            header.writeGroupMark(false);
        }
        header.writeGroupMark(false);
    }

    /**
     * Gets the rtf style reference from the table.
     *
     * @param name
     *            Name of Style
     * @return Rtf attribute of the style reference
     */
    private String getRtfStyleReference(final String name) {
        return "cs" + this.styles.get(name).toString();
    }
}
