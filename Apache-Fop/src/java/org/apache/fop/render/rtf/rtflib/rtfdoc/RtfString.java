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

/* $Id: RtfString.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

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
 * Plain text in a RTF file, without any formatings.
 * </p>
 *
 * <p>
 * This work was authored by Peter Herweg (pherweg@web.de).
 * </p>
 */

public class RtfString extends RtfElement {
    private String text = "";

    RtfString(final RtfContainer parent, final Writer w, final String s)
            throws IOException {
        super(parent, w);

        this.text = s;
    }

    /**
     * @return true if this element would generate no "useful" RTF content
     */
    @Override
    public boolean isEmpty() {
        return this.text.trim().equals("");
    }

    /**
     * write RTF code of all our children
     * 
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeRtfContent() throws IOException {
        RtfStringConverter.getInstance().writeRtfString(this.writer, this.text);
    }

    /** @return the text */
    public String getText() {
        return this.text;
    }

    /**
     * @param s
     *            some text
     */
    public void setText(final String s) {
        this.text = s;
    }
}
