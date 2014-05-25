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

/* $Id: MODCAParser.java 1104135 2011-05-17 11:07:06Z phancock $ */

package org.apache.fop.afp.parser;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.parser.UnparsedStructuredField.Introducer;

/**
 * An simple MO:DCA/AFP parser.
 */
@Slf4j
public class MODCAParser {

    private static final int INTRODUCER_LENGTH = 8;

    /**
     * The carriage control character (0x5A) used to indicate the start of a
     * structured field.
     */
    public static final byte CARRIAGE_CONTROL_CHAR = (byte) (0x5A & 0xFF);

    private final DataInputStream din;

    /**
     * Main constructor
     *
     * @param in
     *            the {@link InputStream} to read the AFP file from.
     */
    public MODCAParser(final InputStream in) {
        this.din = new DataInputStream(in);
    }

    /**
     * Reads the next structured field from the input stream.
     * <p>
     * No structure validation of the MO:DCA file is performed.
     *
     * @return a new unparsed structured field (or null when parsing is
     *         finished).
     * @throws IOException
     *             if an I/O error occurs
     */
    public UnparsedStructuredField readNextStructuredField() throws IOException {

        // Find the SF delimiter
        do {
            // Exhausted streams and so no next SF
            // - null return represents this case
            // TODO should this happen?
            if (this.din.available() == 0) {
                return null;
            }
        } while (this.din.readByte() != CARRIAGE_CONTROL_CHAR);

        // Read introducer as byte array to preserve any data not parsed below
        final byte[] introducerData = new byte[INTRODUCER_LENGTH]; // Length of
        // introducer
        this.din.readFully(introducerData);

        final Introducer introducer = new Introducer(introducerData);

        int dataLength = introducer.getLength() - INTRODUCER_LENGTH;

        // Handle optional extension
        byte[] extData = null;
        if (introducer.isExtensionPresent()) {
            short extLength = 0;
            extLength = (short) (this.din.readByte() & 0xFF);
            if (extLength > 0) {
                extData = new byte[extLength - 1];
                this.din.readFully(extData);
                dataLength -= extLength;
            }
        }
        // Read payload
        final byte[] data = new byte[dataLength];
        this.din.readFully(data);

        final UnparsedStructuredField sf = new UnparsedStructuredField(
                introducer, data, extData);

        if (log.isTraceEnabled()) {
            log.trace(sf.toString());
        }

        return sf;
    }
}
