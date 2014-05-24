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

/* $Id: RtfPage.java 1297284 2012-03-05 23:29:29Z gadams $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

import java.io.IOException;
import java.io.Writer;

/**
 * <p>
 * Specifies rtf control words. Is the container for page attributes. Overrides
 * okToWriteRtf.
 * </p>
 *
 * <p>
 * This work was authored by Christopher Scott (scottc@westinghouse.com).
 * </p>
 */

public class RtfPage extends RtfContainer {
    private final RtfAttributes attrib;

    /** RtfPage attributes */
    /** constant for page width */
    public static final String PAGE_WIDTH = "paperw";
    /** constant for page height */
    public static final String PAGE_HEIGHT = "paperh";

    /** constant for landscape format */
    public static final String LANDSCAPE = "landscape";

    /** constant for top margin */
    public static final String MARGIN_TOP = "margt";
    /** constant for bottom margin */
    public static final String MARGIN_BOTTOM = "margb";
    /** constant for left margin */
    public static final String MARGIN_LEFT = "margl";
    /** constant for right margin */
    public static final String MARGIN_RIGHT = "margr";

    /** constant for header position */
    public static final String HEADERY = "headery";
    /** constant for footer position */
    public static final String FOOTERY = "footery";
    /** constant for itap level */
    public static final String ITAP = "itap";

    /** String array of RtfPage attributes */
    public static final String[] PAGE_ATTR = new String[] { PAGE_WIDTH,
        PAGE_HEIGHT, LANDSCAPE, MARGIN_TOP, MARGIN_BOTTOM, MARGIN_LEFT,
        MARGIN_RIGHT, HEADERY, FOOTERY, ITAP };

    /**
     * RtfPage creates new page attributes with the parent container, the writer
     * and the attributes
     */
    RtfPage(final RtfPageArea parent, final Writer w, final RtfAttributes attrs)
            throws IOException {
        super(parent, w);
        this.attrib = attrs;
    }

    /**
     * RtfPage writes the attributes the attributes contained in the string
     * PAGE_ATTR, if not null
     *
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeRtfContent() throws IOException {
        writeAttributes(this.attrib, PAGE_ATTR);

        if (this.attrib != null) {
            final Object widthRaw = this.attrib.getValue(PAGE_WIDTH);
            final Object heightRaw = this.attrib.getValue(PAGE_HEIGHT);

            if (widthRaw instanceof Integer
                    && heightRaw instanceof Integer
                    && ((Integer) widthRaw).intValue() > ((Integer) heightRaw)
                    .intValue()) {
                writeControlWord(LANDSCAPE);
            }
        }
    }

    /**
     * RtfPage - attributes accessor
     *
     * @return attributes
     */
    public RtfAttributes getAttributes() {
        return this.attrib;
    }

    /**
     * RtfPage - is overwritten here because page attributes have no content
     * only attributes. RtfContainer is defined not to write when empty.
     * Therefore must make this true to print.
     *
     * @return true
     */
    @Override
    protected boolean okToWriteRtf() {
        return true;
    }

}
