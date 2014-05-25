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

/* $Id: CMapBuilder.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.io.Writer;

/** A cmap builder. */
public class CMapBuilder {

    /** name */
    protected String name;
    /** writer */
    protected Writer writer;

    /**
     * Construct cmap builder.
     * 
     * @param writer
     *            a writer
     * @param name
     *            a name
     */
    public CMapBuilder(final Writer writer, final String name) {
        this.writer = writer;
        this.name = name;
    }

    /**
     * Writes the CMap to a Writer.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    public void writeCMap() throws IOException {
        writePreStream();
        writeStreamComments();
        writeCIDInit();
        writeCIDSystemInfo();
        writeVersion("1");
        writeType("1");
        writeName(this.name);
        writeCodeSpaceRange();
        writeCIDRange();
        writeBFEntries();
        writeWrapUp();
        writeStreamAfterComments();
        writeUseCMap();
    }

    /**
     * @throws IOException
     *             if i/o exception
     */
    protected void writePreStream() throws IOException {
        // writer.write("/Type /CMap\n");
        // writer.write(sysInfo.toPDFString());
        // writer.write("/CMapName /" + name + EOL);
    }

    /**
     * @throws IOException
     *             if i/o exception
     */
    protected void writeStreamComments() throws IOException {
        this.writer.write("%!PS-Adobe-3.0 Resource-CMap\n");
        this.writer.write("%%DocumentNeededResources: ProcSet (CIDInit)\n");
        this.writer.write("%%IncludeResource: ProcSet (CIDInit)\n");
        this.writer.write("%%BeginResource: CMap (" + this.name + ")\n");
        this.writer.write("%%EndComments\n");
    }

    /**
     * @throws IOException
     *             if i/o exception
     */
    protected void writeCIDInit() throws IOException {
        this.writer.write("/CIDInit /ProcSet findresource begin\n");
        this.writer.write("12 dict begin\n");
        this.writer.write("begincmap\n");
    }

    /**
     * @param registry
     *            string
     * @param ordering
     *            string
     * @param supplement
     *            string
     * @throws IOException
     *             if i/o exception
     */
    protected void writeCIDSystemInfo(final String registry,
            final String ordering, final int supplement) throws IOException {
        this.writer.write("/CIDSystemInfo 3 dict dup begin\n");
        this.writer.write("  /Registry (");
        this.writer.write(registry);
        this.writer.write(") def\n");
        this.writer.write("  /Ordering (");
        this.writer.write(ordering);
        this.writer.write(") def\n");
        this.writer.write("  /Supplement ");
        this.writer.write(Integer.toString(supplement));
        this.writer.write(" def\n");
        this.writer.write("end def\n");
    }

    /**
     * @throws IOException
     *             if i/o exception
     */
    protected void writeCIDSystemInfo() throws IOException {
        writeCIDSystemInfo("Adobe", "Identity", 0);
    }

    /**
     * @param version
     *            a version
     * @throws IOException
     *             if i/o exception
     */
    protected void writeVersion(final String version) throws IOException {
        this.writer.write("/CMapVersion ");
        this.writer.write(version);
        this.writer.write(" def\n");
    }

    /**
     * @param type
     *            a type
     * @throws IOException
     *             if i/o exception
     */
    protected void writeType(final String type) throws IOException {
        this.writer.write("/CMapType ");
        this.writer.write(type);
        this.writer.write(" def\n");
    }

    /**
     * @param name
     *            a name
     * @throws IOException
     *             if i/o exception
     */
    protected void writeName(final String name) throws IOException {
        this.writer.write("/CMapName /");
        this.writer.write(name);
        this.writer.write(" def\n");
    }

    /**
     * @throws IOException
     *             if i/o exception
     */
    protected void writeCodeSpaceRange() throws IOException {
        writeCodeSpaceRange(false);
    }

    /**
     * @param singleByte
     *            true if single byte range
     * @throws IOException
     *             if i/o exception
     */
    protected void writeCodeSpaceRange(final boolean singleByte)
            throws IOException {
        this.writer.write("1 begincodespacerange\n");
        if (singleByte) {
            this.writer.write("<00> <FF>\n");
        } else {
            this.writer.write("<0000> <FFFF>\n");
        }
        this.writer.write("endcodespacerange\n");
    }

    /**
     * @throws IOException
     *             if i/o exception
     */
    protected void writeCIDRange() throws IOException {
        this.writer.write("1 begincidrange\n");
        this.writer.write("<0000> <FFFF> 0\n");
        this.writer.write("endcidrange\n");
    }

    /**
     * @throws IOException
     *             if i/o exception
     */
    protected void writeBFEntries() throws IOException {
        // writer.write("1 beginbfrange\n");
        // writer.write("<0020> <0100> <0000>\n");
        // writer.write("endbfrange\n");
    }

    /**
     * @throws IOException
     *             if i/o exception
     */
    protected void writeWrapUp() throws IOException {
        this.writer.write("endcmap\n");
        this.writer.write("CMapName currentdict /CMap defineresource pop\n");
        this.writer.write("end\n");
        this.writer.write("end\n");
    }

    /**
     * @throws IOException
     *             if i/o exception
     */
    protected void writeStreamAfterComments() throws IOException {
        this.writer.write("%%EndResource\n");
        this.writer.write("%%EOF\n");
    }

    /** does nothing */
    protected void writeUseCMap() {
        /*
         * writer.write(" /Type /CMap"); writer.write("/CMapName /" + name +
         * EOL); writer.write("/WMode " + wMode + EOL); if (base != null) {
         * writer.write("/UseCMap "); if (base instanceof String) {
         * writer.write("/"+base); } else {// base instanceof PDFStream
         * writer.write(((PDFStream)base).referencePDF()); } }
         */
    }
}
