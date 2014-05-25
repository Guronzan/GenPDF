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

/* $Id: RtfListStyleText.java 1296437 2012-03-02 20:24:40Z gadams $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

//Java
import java.io.IOException;

/**
 * Class to handle text list style.
 */
public class RtfListStyleText extends RtfListStyle {
    private final String text;

    /**
     * Constructs a RtfListStyleText object.
     *
     * @param s
     *            Text to be displayed
     */
    public RtfListStyleText(final String s) {
        this.text = s;
    }

    /**
     * Gets called before a RtfListItem has to be written.
     *
     * @param item
     *            RtfListItem whose prefix has to be written {@inheritDoc}
     * @throws IOException
     *             Thrown when an IO-problem occurs
     */
    @Override
    public void writeListPrefix(final RtfListItem item) throws IOException {
        // bulleted list
        item.writeControlWord("pnlvlblt");
        item.writeControlWord("ilvl0");
        item.writeOneAttribute(RtfListTable.LIST_NUMBER,
                new Integer(item.getNumber()));
        item.writeOneAttribute("pnindent",
                item.getParentList().attrib.getValue(RtfListTable.LIST_INDENT));
        item.writeControlWord("pnf1");
        item.writeGroupMark(true);
        // item.writeControlWord("pndec");
        item.writeOneAttribute(RtfListTable.LIST_FONT_TYPE, "2");
        item.writeControlWord("pntxtb");
        RtfStringConverter.getInstance().writeRtfString(item.writer, this.text);
        item.writeGroupMark(false);
    }

    /**
     * Gets called before a paragraph, which is contained by a RtfListItem has
     * to be written.
     *
     * @param element
     *            RtfElement in whose context is to be written {@inheritDoc}
     * @throws IOException
     *             Thrown when an IO-problem occurs
     */
    @Override
    public void writeParagraphPrefix(final RtfElement element)
            throws IOException {
        element.writeGroupMark(true);
        element.writeControlWord("pntext");
        element.writeGroupMark(false);
    }

    /**
     * Gets called when the list table has to be written.
     *
     * @param element
     *            RtfElement in whose context is to be written {@inheritDoc}
     * @throws IOException
     *             Thrown when an IO-problem occurs
     */
    @Override
    public void writeLevelGroup(final RtfElement element) throws IOException {
        element.attrib.set(RtfListTable.LIST_NUMBER_TYPE, 23);
        element.writeGroupMark(true);

        String sCount;
        if (this.text.length() < 10) {
            sCount = "0" + String.valueOf(this.text.length());
        } else {
            sCount = String.valueOf(Integer.toHexString(this.text.length()));
            if (sCount.length() == 1) {
                sCount = "0" + sCount;
            }
        }
        element.writeOneAttributeNS(RtfListTable.LIST_TEXT_FORM, "\\'" + sCount
                + RtfStringConverter.getInstance().escape(this.text));
        element.writeGroupMark(false);

        element.writeGroupMark(true);
        element.writeOneAttributeNS(RtfListTable.LIST_NUM_POSITION, null);
        element.writeGroupMark(false);

        element.attrib.set(RtfListTable.LIST_FONT_TYPE, 2);
    }
}
