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

/* $Id: DefaultDSCHandler.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.ps.dsc;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.xmlgraphics.ps.DSCConstants;
import org.apache.xmlgraphics.ps.PSGenerator;
import org.apache.xmlgraphics.ps.dsc.events.DSCComment;

/**
 * Default implementation of a DSCHandler which simply passes through the
 * PostScript content unchanged. Subclasses can implement different behaviour,
 * for example to filter certain DSC comments or to insert PostScript code at
 * specific places.
 */
public class DefaultDSCHandler implements DSCHandler {

    protected OutputStream out;
    protected PSGenerator gen;

    /**
     * Creates a new instance.
     * 
     * @param out
     *            OutputStream to pipe all received events to
     */
    public DefaultDSCHandler(final OutputStream out) {
        this.out = out;
        this.gen = new PSGenerator(this.out);
    }

    /** @see org.apache.xmlgraphics.ps.dsc.DSCHandler#startDocument(java.lang.String) */
    @Override
    public void startDocument(final String header) throws IOException {
        this.gen.writeln(header);
    }

    /** @see org.apache.xmlgraphics.ps.dsc.DSCHandler#endDocument() */
    @Override
    public void endDocument() throws IOException {
        this.gen.writeDSCComment(DSCConstants.EOF);
    }

    /**
     * @see org.apache.xmlgraphics.ps.dsc.DSCHandler#handleDSCComment(org.apache.xmlgraphics.ps.dsc.events.DSCComment)
     */
    @Override
    public void handleDSCComment(final DSCComment comment) throws IOException {
        comment.generate(this.gen);

    }

    /** @see org.apache.xmlgraphics.ps.dsc.DSCHandler#line(java.lang.String) */
    @Override
    public void line(final String line) throws IOException {
        this.gen.writeln(line);
    }

    /** @see org.apache.xmlgraphics.ps.dsc.DSCHandler#comment(java.lang.String) */
    @Override
    public void comment(final String comment) throws IOException {
        this.gen.commentln("%" + comment);
    }

}
