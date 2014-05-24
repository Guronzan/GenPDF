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

/* $Id: PDFWArray.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.pdf;

import java.util.List;

/**
 * Class representing a <b>W</b> array for CID fonts.
 */
public class PDFWArray {

    /**
     * The metrics
     */
    private final List entries = new java.util.ArrayList();

    /**
     * Default constructor
     */
    public PDFWArray() {
    }

    /**
     * Convenience constructor
     * 
     * @param metrics
     *            the metrics array to initially add
     */
    public PDFWArray(final int[] metrics) {
        addEntry(0, metrics);
    }

    /**
     * Add an entry for single starting CID. i.e. in the form "c [w ...]"
     *
     * @param start
     *            the starting CID value.
     * @param metrics
     *            the metrics array.
     */
    public void addEntry(final int start, final int[] metrics) {
        this.entries.add(new Entry(start, metrics));
    }

    /**
     * Add an entry for a range of CIDs (/W element on p 213)
     *
     * @param first
     *            the first CID in the range
     * @param last
     *            the last CID in the range
     * @param width
     *            the width for all CIDs in the range
     */
    public void addEntry(final int first, final int last, final int width) {
        this.entries.add(new int[] { first, last, width });
    }

    /**
     * Add an entry for a range of CIDs (/W2 element on p 210)
     *
     * @param first
     *            the first CID in the range
     * @param last
     *            the last CID in the range
     * @param width
     *            the width for all CIDs in the range
     * @param posX
     *            the x component for the vertical position vector
     * @param posY
     *            the y component for the vertical position vector
     */
    public void addEntry(final int first, final int last, final int width,
            final int posX, final int posY) {
        this.entries.add(new int[] { first, last, width, posX, posY });
    }

    /**
     * Convert this object to PDF code.
     * 
     * @return byte[] the PDF code
     */
    public byte[] toPDF() {
        return PDFDocument.encode(toPDFString());
    }

    /**
     * Convert this object to PDF code.
     * 
     * @return String the PDF code
     */
    public String toPDFString() {
        final StringBuffer p = new StringBuffer();
        p.append("[ ");
        final int len = this.entries.size();
        for (int i = 0; i < len; i++) {
            final Object entry = this.entries.get(i);
            if (entry instanceof int[]) {
                final int[] line = (int[]) entry;
                for (int j = 0; j < line.length; j++) {
                    p.append(line[j]);
                    p.append(" ");
                }
            } else {
                ((Entry) entry).fillInPDF(p);
            }
        }
        p.append("]");
        return p.toString();
    }

    /**
     * Inner class for entries in the form "c [w ...]"
     */
    private static class Entry {
        private final int start;
        private final int[] metrics;

        public Entry(final int s, final int[] m) {
            this.start = s;
            this.metrics = m;
        }

        public void fillInPDF(final StringBuffer p) {
            // p.setLength(0);
            p.append(this.start);
            p.append(" [");
            for (int i = 0; i < this.metrics.length; i++) {
                p.append(this.metrics[i]);
                p.append(" ");
            }
            p.append("] ");
        }

    }
}
