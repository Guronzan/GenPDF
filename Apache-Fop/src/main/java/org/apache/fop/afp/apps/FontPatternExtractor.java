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

/* $Id: FontPatternExtractor.java 1104135 2011-05-17 11:07:06Z phancock $ */

package org.apache.fop.afp.apps;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.commons.io.HexDump;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.fop.afp.parser.MODCAParser;
import org.apache.fop.afp.parser.UnparsedStructuredField;

/**
 * This class represents a tool for extracting the Type 1 PFB file from an AFP
 * outline font.
 */
public class FontPatternExtractor {

    private final PrintStream printStream = System.out;

    /**
     * Extracts the Type1 PFB file from the given AFP outline font.
     * 
     * @param file
     *            the AFP file to read from
     * @param targetDir
     *            the target directory where the PFB file is to be placed.
     * @throws IOException
     *             if an I/O error occurs
     */
    public void extract(final File file, final File targetDir)
            throws IOException {
        final InputStream in = new java.io.FileInputStream(file);
        try {
            final MODCAParser parser = new MODCAParser(in);
            final ByteArrayOutputStream baout = new ByteArrayOutputStream();
            UnparsedStructuredField strucField;
            while ((strucField = parser.readNextStructuredField()) != null) {
                if (strucField.getSfTypeID() == 0xD3EE89) {
                    final byte[] sfData = strucField.getData();
                    println(strucField.toString());
                    HexDump.dump(sfData, 0, this.printStream, 0);
                    baout.write(sfData);
                }
            }

            final ByteArrayInputStream bin = new ByteArrayInputStream(
                    baout.toByteArray());
            final DataInputStream din = new DataInputStream(bin);
            final long len = din.readInt() & 0xFFFFFFFFL;
            println("Length: " + len);
            din.skip(4); // checksum
            final int tidLen = din.readUnsignedShort() - 2;
            final byte[] tid = new byte[tidLen];
            din.readFully(tid);
            String filename = new String(tid, "ISO-8859-1");
            final int asciiCount1 = countUSAsciiCharacters(filename);
            final String filenameEBCDIC = new String(tid, "Cp1146");
            final int asciiCount2 = countUSAsciiCharacters(filenameEBCDIC);
            println("TID: " + filename + " " + filenameEBCDIC);

            if (asciiCount2 > asciiCount1) {
                // Haven't found an indicator if the name is encoded in EBCDIC
                // or not
                // so we use a trick.
                filename = filenameEBCDIC;
            }
            if (!filename.toLowerCase().endsWith(".pfb")) {
                filename = filename + ".pfb";
            }
            println("Output filename: " + filename);
            final File out = new File(targetDir, filename);

            final OutputStream fout = new java.io.FileOutputStream(out);
            try {
                IOUtils.copyLarge(din, fout);
            } finally {
                IOUtils.closeQuietly(fout);
            }

        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void println(final String s) {
        this.printStream.println(s);
    }

    private void println() {
        this.printStream.println();
    }

    private int countUSAsciiCharacters(final String filename) {
        int count = 0;
        for (int i = 0, c = filename.length(); i < c; i++) {
            if (filename.charAt(i) < 128) {
                count++;
            }
        }
        return count;
    }

    /**
     * Main method
     * 
     * @param args
     *            the command-line arguments
     */
    public static void main(final String[] args) {
        try {
            final FontPatternExtractor app = new FontPatternExtractor();

            app.println("Font Pattern Extractor");
            app.println();

            if (args.length > 0) {
                final String filename = args[0];
                final File file = new File(filename);

                File targetDir = file.getParentFile();
                if (args.length > 1) {
                    targetDir = new File(args[1]);
                    targetDir.mkdirs();
                }

                app.extract(file, targetDir);
            } else {
                app.println("This tool tries to extract the PFB file from an AFP outline font.");
                app.println();
                app.println("Usage: Java -cp ... "
                        + FontPatternExtractor.class.getName()
                        + " <afp-font-file> [<target-dir>]");
                System.exit(-1);
            }

        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
