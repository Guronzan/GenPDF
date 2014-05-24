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

/* $Id: RtfAfterBeforeBase.java 1297284 2012-03-05 23:29:29Z gadams $ */

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
 * Common code for RtfAfter and RtfBefore.
 * </p>
 *
 * <p>
 * This work was authored by Andreas Lambert (andreas.lambert@cronidesoft.com),
 * Christopher Scott (scottc@westinghouse.com), and Christoph Zahm
 * (zahm@jnet.ch) [support for tables in headers/footers].
 * </p>
 */

abstract class RtfAfterBeforeBase extends RtfContainer implements
IRtfParagraphContainer, IRtfExternalGraphicContainer,
IRtfTableContainer, IRtfTextrunContainer {
    protected RtfAttributes attrib;
    private RtfParagraph para;
    private RtfExternalGraphic externalGraphic;
    private RtfTable table;

    RtfAfterBeforeBase(final RtfSection parent, final Writer w,
            final RtfAttributes attrs) throws IOException {
        super(parent, w, attrs);
        this.attrib = attrs;
    }

    @Override
    public RtfParagraph newParagraph() throws IOException {
        closeAll();
        this.para = new RtfParagraph(this, this.writer);
        return this.para;
    }

    @Override
    public RtfParagraph newParagraph(final RtfAttributes attrs)
            throws IOException {
        closeAll();
        this.para = new RtfParagraph(this, this.writer, attrs);
        return this.para;
    }

    @Override
    public RtfExternalGraphic newImage() throws IOException {
        closeAll();
        this.externalGraphic = new RtfExternalGraphic(this, this.writer);
        return this.externalGraphic;
    }

    private void closeCurrentParagraph() throws IOException {
        if (this.para != null) {
            this.para.close();
        }
    }

    private void closeCurrentExternalGraphic() throws IOException {
        if (this.externalGraphic != null) {
            this.externalGraphic.close();
        }
    }

    private void closeCurrentTable() throws IOException {
        if (this.table != null) {
            this.table.close();
        }
    }

    @Override
    protected void writeRtfPrefix() throws IOException {
        writeGroupMark(true);
        writeMyAttributes();
    }

    /** must be implemented to write the header or footer attributes */
    protected abstract void writeMyAttributes() throws IOException;

    @Override
    protected void writeRtfSuffix() throws IOException {
        writeGroupMark(false);
    }

    public RtfAttributes getAttributes() {
        return this.attrib;
    }

    public void closeAll() throws IOException {
        closeCurrentParagraph();
        closeCurrentExternalGraphic();
        closeCurrentTable();
    }

    /**
     * close current table if any and start a new one
     *
     * @param tc
     *            added by Boris Poudérous on july 2002 in order to process
     *            number-columns-spanned attribute
     */
    @Override
    public RtfTable newTable(final RtfAttributes attrs,
            final ITableColumnsInfo tc) throws IOException {
        closeAll();
        this.table = new RtfTable(this, this.writer, attrs, tc);
        return this.table;
    }

    /** close current table if any and start a new one */
    @Override
    public RtfTable newTable(final ITableColumnsInfo tc) throws IOException {
        closeAll();
        this.table = new RtfTable(this, this.writer, tc);
        return this.table;
    }

    @Override
    public RtfTextrun getTextrun() throws IOException {
        return RtfTextrun.getTextrun(this, this.writer, null);
    }
}
