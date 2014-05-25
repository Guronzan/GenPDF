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

/* $Id: PDFStream.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Class representing a PDF stream.
 * <p>
 * A derivative of the PDF Object, a PDF Stream has not only a dictionary but a
 * stream of PDF commands. The stream of commands is where the real work is
 * done, the dictionary just provides information like the stream length.
 */
public class PDFStream extends AbstractPDFStream {

    /**
     * The stream of PDF commands
     */
    protected StreamCache data;

    private transient Writer streamWriter;

    /**
     * Create an empty stream object
     */
    public PDFStream() {
        setUp();
    }

    public PDFStream(final PDFDictionary dictionary) {
        super(dictionary);
        setUp();
    }

    public PDFStream(final PDFDictionary dictionary,
            final boolean encodeOnTheFly) {
        super(dictionary, encodeOnTheFly);
        setUp();
    }

    public PDFStream(final boolean encodeOnTheFly) {
        super(encodeOnTheFly);
        setUp();
    }

    private void setUp() {
        try {
            this.data = StreamCacheFactory.getInstance().createStreamCache();
            this.streamWriter = new OutputStreamWriter(getBufferOutputStream(),
                    PDFDocument.ENCODING);
            // Buffer to minimize calls to the converter
            this.streamWriter = new java.io.BufferedWriter(this.streamWriter);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Append data to the stream
     *
     * @param s
     *            the string of PDF to add
     */
    public void add(final String s) {
        try {
            this.streamWriter.write(s);
        } catch (final IOException ex) {
            // TODO throw the exception and catch it elsewhere
            ex.printStackTrace();
        }
    }

    private void flush() throws IOException {
        this.streamWriter.flush();
    }

    /**
     * Returns a Writer that writes to the OutputStream of the buffer.
     * 
     * @return the Writer
     */
    public Writer getBufferWriter() {
        return this.streamWriter;
    }

    /**
     * Returns an OutputStream that can be used to write to the buffer which is
     * used to build up the PDF stream.
     * 
     * @return the OutputStream
     * @throws IOException
     *             In case of an I/O problem
     */
    public OutputStream getBufferOutputStream() throws IOException {
        if (this.streamWriter != null) {
            flush(); // Just to be sure
        }
        return this.data.getOutputStream();
    }

    /**
     * Used to set the contents of the PDF stream.
     * 
     * @param data
     *            the contents as a byte array
     * @throws IOException
     *             in case of an I/O problem
     */
    public void setData(final byte[] data) throws IOException {
        this.data.clear();
        this.data.write(data);
    }

    /**
     * Returns the size of the content.
     * 
     * @return size of the content
     */
    public int getDataLength() {
        try {
            flush();
            return this.data.getSize();
        } catch (final Exception e) {
            // TODO throw the exception and catch it elsewhere
            e.printStackTrace();
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected int getSizeHint() throws IOException {
        flush();
        return this.data.getSize();
    }

    /** {@inheritDoc} */
    @Override
    protected void outputRawStreamData(final OutputStream out)
            throws IOException {
        flush();
        this.data.outputContents(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int output(final OutputStream stream) throws IOException {
        final int len = super.output(stream);

        // Now that the data has been written, it can be discarded.
        this.data = null;
        return len;
    }

}
