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

/* $Id: RtfFootnote.java 1297284 2012-03-05 23:29:29Z gadams $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

import java.io.IOException;
import java.io.Writer;

/**
 * <p>
 * Model of an RTF footnote.
 * </p>
 *
 * <p>
 * This work was authored by Peter Herweg (pherweg@web.de) and Marc Wilhelm
 * Kuester.
 * </p>
 */
public class RtfFootnote extends RtfContainer implements IRtfTextrunContainer,
IRtfListContainer {
    RtfTextrun textrunInline = null; // CSOK: VisibilityModifier
    RtfContainer body = null; // CSOK: VisibilityModifier
    RtfList list = null; // CSOK: VisibilityModifier
    boolean bBody = false; // CSOK: VisibilityModifier

    /**
     * Create an RTF list item as a child of given container with default
     * attributes.
     *
     * @param parent
     *            a container
     * @param w
     *            a writer
     * @throws IOException
     *             if not caught
     */
    RtfFootnote(final RtfContainer parent, final Writer w) throws IOException {
        super(parent, w);
        this.textrunInline = new RtfTextrun(this, this.writer, null);
        this.body = new RtfContainer(this, this.writer);
    }

    /**
     * @return a text run
     * @throws IOException
     *             if not caught
     */
    @Override
    public RtfTextrun getTextrun() throws IOException {
        if (this.bBody) {
            final RtfTextrun textrun = RtfTextrun.getTextrun(this.body,
                    this.writer, null);
            textrun.setSuppressLastPar(true);

            return textrun;
        } else {
            return this.textrunInline;
        }
    }

    /**
     * write RTF code of all our children
     *
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeRtfContent() throws IOException {
        this.textrunInline.writeRtfContent();

        writeGroupMark(true);
        writeControlWord("footnote");
        writeControlWord("ftnalt");

        this.body.writeRtfContent();

        writeGroupMark(false);
    }

    /**
     * @param attrs
     *            some attributes
     * @return an rtf list
     * @throws IOException
     *             if not caught
     */
    @Override
    public RtfList newList(final RtfAttributes attrs) throws IOException {
        if (this.list != null) {
            this.list.close();
        }

        this.list = new RtfList(this.body, this.writer, attrs);

        return this.list;
    }

    /** start body */
    public void startBody() {
        this.bBody = true;
    }

    /** end body */
    public void endBody() {
        this.bBody = false;
    }
}