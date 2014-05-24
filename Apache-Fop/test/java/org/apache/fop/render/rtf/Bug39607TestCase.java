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

/* $Id: Bug39607TestCase.java 1178747 2011-10-04 10:09:01Z vhennebert $ */

package org.apache.fop.render.rtf;

import java.io.StringWriter;

import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfDocumentArea;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfFile;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfParagraph;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfSection;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfTable;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfTableRow;
import org.junit.Test;

/**
 * Test for http://issues.apache.org/bugzilla/show_bug.cgi?id=39607
 */
public class Bug39607TestCase {

    /**
     * Test for the NPE describes in bug 39607
     * 
     * @throws Exception
     *             If an error occurs
     */
    @Test
    public void testForNPE() throws Exception {
        final StringWriter writer = new StringWriter();
        final RtfFile f = new RtfFile(writer);

        final RtfDocumentArea doc = f.startDocumentArea();

        final RtfSection section = doc.newSection();

        final RtfParagraph paragraph = section.newParagraph();
        paragraph.newText("Testing fop - rtf module - class RtfTableRow");
        paragraph.close();

        final RtfTable table = section.newTable(null);
        final RtfTableRow row = table.newTableRow();
        row.newTableCell(2000).newParagraph().newText("blah");
        row.newTableCell(5000).newParagraph().newText("doubleBlah");
        row.close();
        table.close();
        section.close();
        doc.close();
        f.flush();
    }

}
