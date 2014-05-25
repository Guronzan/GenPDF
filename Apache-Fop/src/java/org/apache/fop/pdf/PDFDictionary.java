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

/* $Id: PDFDictionary.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.CountingOutputStream;

/**
 * Class representing a PDF dictionary object
 */
public class PDFDictionary extends PDFObject {

    /**
     * the entry map
     */
    protected Map<String, Object> entries = new java.util.HashMap<String, Object>();

    /**
     * maintains the order of the entries added to the entry map. Whenever you
     * modify "entries", always make sure you adjust this list accordingly.
     */
    protected List<String> order = new java.util.ArrayList<String>();

    /**
     * Create a new dictionary object.
     */
    public PDFDictionary() {
        super();
    }

    /**
     * Create a new dictionary object.
     * 
     * @param parent
     *            the object's parent if any
     */
    public PDFDictionary(final PDFObject parent) {
        super(parent);
    }

    /**
     * Puts a new name/value pair.
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     */
    public void put(final String name, final Object value) {
        if (value instanceof PDFObject) {
            final PDFObject pdfObj = (PDFObject) value;
            if (!pdfObj.hasObjectNumber()) {
                pdfObj.setParent(this);
            }
        }
        if (!this.entries.containsKey(name)) {
            this.order.add(name);
        }
        this.entries.put(name, value);
    }

    /**
     * Puts a new name/value pair.
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     */
    public void put(final String name, final int value) {
        if (!this.entries.containsKey(name)) {
            this.order.add(name);
        }
        this.entries.put(name, Integer.valueOf(value));
    }

    /**
     * Returns the value given a name.
     * 
     * @param name
     *            the name of the value
     * @return the value or null, if there's no value with the given name.
     */
    public Object get(final String name) {
        return this.entries.get(name);
    }

    /** {@inheritDoc} */
    @Override
    public int output(final OutputStream stream) throws IOException {
        final CountingOutputStream cout = new CountingOutputStream(stream);
        final StringBuilder textBuffer = new StringBuilder(64);
        writeDictionary(cout, textBuffer);
        PDFDocument.flushTextBuffer(textBuffer, cout);
        return cout.getCount();
    }

    /**
     * Writes the contents of the dictionary to a StringBuilder.
     * 
     * @param out
     *            the OutputStream (for binary content)
     * @param textBuffer
     *            the text buffer for text output
     * @throws IOException
     *             if an I/O error occurs
     */
    protected void writeDictionary(final OutputStream out,
            final StringBuilder textBuffer) throws IOException {
        textBuffer.append("<<");
        final boolean compact = this.order.size() <= 2;
        for (final String key : this.order) {
            if (compact) {
                textBuffer.append(' ');
            } else {
                textBuffer.append("\n  ");
            }
            textBuffer.append(PDFName.escapeName(key));
            textBuffer.append(' ');
            final Object obj = this.entries.get(key);
            formatObject(obj, out, textBuffer);
        }
        if (compact) {
            textBuffer.append(' ');
        } else {
            textBuffer.append('\n');
        }
        textBuffer.append(">>\n");
    }

}
