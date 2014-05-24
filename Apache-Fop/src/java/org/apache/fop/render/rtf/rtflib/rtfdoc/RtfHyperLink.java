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

/* $Id: RtfHyperLink.java 1311120 2012-04-08 23:48:11Z gadams $ */

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
 * Creates an hyperlink. This class belongs to the <fo:basic-link> tag
 * processing.
 * </p>
 *
 * <p>
 * This work was authored by Andreas Putz (a.putz@skynamics.com).
 * </p>
 */
public class RtfHyperLink extends RtfContainer implements IRtfTextContainer,
IRtfTextrunContainer {

    // ////////////////////////////////////////////////
    // @@ Members
    // ////////////////////////////////////////////////

    /** The url of the image */
    protected String url = null;

    /** RtfText */
    protected RtfText mText = null;

    // ////////////////////////////////////////////////
    // @@ Construction
    // ////////////////////////////////////////////////

    /**
     * A constructor.
     *
     * @param parent
     *            a <code>RtfContainer</code> value
     * @param writer
     *            a <code>Writer</code> value
     * @param str
     *            text of the link
     * @param attr
     *            a <code>RtfAttributes</code> value
     * @throws IOException
     *             for I/O problems
     */
    public RtfHyperLink(final IRtfTextContainer parent, final Writer writer,
            final String str, final RtfAttributes attr) throws IOException {
        super((RtfContainer) parent, writer, attr);
        new RtfText(this, writer, str, attr);
    }

    /**
     * A constructor.
     *
     * @param parent
     *            a <code>RtfContainer</code> value
     * @param writer
     *            a <code>Writer</code> value
     * @param attr
     *            a <code>RtfAttributes</code> value
     * @throws IOException
     *             for I/O problems
     */
    public RtfHyperLink(final RtfTextrun parent, final Writer writer,
            final RtfAttributes attr) throws IOException {
        super(parent, writer, attr);
    }

    // ////////////////////////////////////////////////
    // @@ RtfElement implementation
    // ////////////////////////////////////////////////

    /**
     * Writes the RTF content to m_writer.
     *
     * @exception IOException
     *                On error
     */
    @Override
    public void writeRtfPrefix() throws IOException {
        super.writeGroupMark(true);
        super.writeControlWord("field");

        super.writeGroupMark(true);
        super.writeStarControlWord("fldinst");

        this.writer.write("HYPERLINK \"" + this.url + "\" ");
        super.writeGroupMark(false);

        super.writeGroupMark(true);
        super.writeControlWord("fldrslt");

        // start a group for this paragraph and write our own attributes if
        // needed
        if (this.attrib != null && this.attrib.isSet("cs")) {
            writeGroupMark(true);
            writeAttributes(this.attrib, new String[] { "cs" });
        }
    }

    /**
     * Writes the RTF content to m_writer.
     *
     * @exception IOException
     *                On error
     */
    @Override
    public void writeRtfSuffix() throws IOException {
        if (this.attrib != null && this.attrib.isSet("cs")) {
            writeGroupMark(false);
        }
        super.writeGroupMark(false);
        super.writeGroupMark(false);
    }

    // ////////////////////////////////////////////////
    // @@ IRtfContainer implementation
    // ////////////////////////////////////////////////

    /**
     * close current text run if any and start a new one with default attributes
     *
     * @param str
     *            if not null, added to the RtfText created
     * @throws IOException
     *             for I/O problems
     * @return new RtfText object
     */
    @Override
    public RtfText newText(final String str) throws IOException {
        return newText(str, null);
    }

    /**
     * close current text run if any and start a new one
     *
     * @param str
     *            if not null, added to the RtfText created
     * @param attr
     *            attributes of text to add
     * @throws IOException
     *             for I/O problems
     * @return the new RtfText object
     */
    @Override
    public RtfText newText(final String str, final RtfAttributes attr)
            throws IOException {
        closeAll();
        this.mText = new RtfText(this, this.writer, str, attr);
        return this.mText;
    }

    /**
     * IRtfTextContainer requirement:
     *
     * @return a copy of our attributes
     * @throws FOPException
     *             if attributes cannot be cloned
     */
    @Override
    public RtfAttributes getTextContainerAttributes() throws FOPException {
        if (this.attrib == null) {
            return null;
        }
        try {
            return (RtfAttributes) this.attrib.clone();
        } catch (final CloneNotSupportedException e) {
            throw new FOPException(e);
        }
    }

    /**
     * add a line break
     *
     * @throws IOException
     *             for I/O problems
     */
    @Override
    public void newLineBreak() throws IOException {
        new RtfLineBreak(this, this.writer);
    }

    // ////////////////////////////////////////////////
    // @@ Common container methods
    // ////////////////////////////////////////////////

    private void closeCurrentText() throws IOException {
        if (this.mText != null) {
            this.mText.close();
        }
    }

    private void closeAll() throws IOException {
        closeCurrentText();
    }

    // ////////////////////////////////////////////////
    // @@ Member access
    // ////////////////////////////////////////////////

    /**
     * Sets the url of the external link.
     *
     * @param url
     *            Link url like "http://..."
     */
    public void setExternalURL(final String url) {
        this.url = url;
    }

    /**
     * Sets the url of the external link.
     *
     * @param jumpTo
     *            Name of the text mark
     */
    public void setInternalURL(final String jumpTo) {
        final int now = jumpTo.length();
        final int max = RtfBookmark.MAX_BOOKMARK_LENGTH;
        this.url = "#" + jumpTo.substring(0, now > max ? max : now);
        this.url = this.url.replace('.', RtfBookmark.REPLACE_CHARACTER);
        this.url = this.url.replace(' ', RtfBookmark.REPLACE_CHARACTER);
    }

    /**
     *
     * @return false (always)
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * @return a text run
     * @throws IOException
     *             if not caught
     */
    @Override
    public RtfTextrun getTextrun() throws IOException {
        final RtfTextrun textrun = RtfTextrun.getTextrun(this, this.writer,
                null);
        return textrun;
    }
}
