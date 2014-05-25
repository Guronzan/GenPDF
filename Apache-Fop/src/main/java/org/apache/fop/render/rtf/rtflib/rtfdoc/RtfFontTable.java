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

/* $Id: RtfFontTable.java 1297284 2012-03-05 23:29:29Z gadams $ */

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
 * RTF font table.
 * </p>
 *
 * <p>
 * This work was authored by Bertrand Delacretaz (bdelacretaz@codeconsult.ch)
 * and Andreas Putz (a.putz@skynamics.com).
 * </p>
 */

class RtfFontTable extends RtfElement {
    /** Create an RTF header */
    RtfFontTable(final RtfHeader h, final Writer w) throws IOException {
        super(h, w);
    }

    /** write our contents to m_writer. */
    @Override
    protected void writeRtfContent() throws IOException {
        RtfFontManager.getInstance().writeFonts((RtfHeader) this.parent);
    }

    /** true if this element would generate no "useful" RTF content */
    @Override
    public boolean isEmpty() {
        return false;
    }
}