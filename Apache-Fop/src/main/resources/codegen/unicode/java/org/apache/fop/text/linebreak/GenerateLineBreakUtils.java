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

/* $Id: GenerateLineBreakUtils.java 1296483 2012-03-02 21:34:30Z gadams $ */

package org.apache.fop.text.linebreak;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.util.License;

// CSOFF: LineLengthCheck

/**
 * <p>
 * Utility for generating a Java class representing line break properties from
 * the Unicode property files.
 * </p>
 * <p>
 * Customizations:
 * <ul>
 * <li>The pair table file is a cut+paste of the sample table from the TR14 HTML
 * file into a text file.</li>
 * <li>Because the sample table does not cover all line break classes, check the
 * 'not in pair table' list of property value short names.</li>
 * <li>Check MAX_LINE_LENGTH.</li>
 * </ul>
 *
 */
@Slf4j
public final class GenerateLineBreakUtils {

    private GenerateLineBreakUtils() {
    }

    private static final int MAX_LINE_LENGTH = 110;

    private static final byte DIRECT_BREAK = 0; // _ in table
    private static final byte INDIRECT_BREAK = 1; // % in table
    private static final byte COMBINING_INDIRECT_BREAK = 2; // # in table
    private static final byte COMBINING_PROHIBITED_BREAK = 3; // @ in table
    private static final byte PROHIBITED_BREAK = 4; // ^ in table
    private static final byte EXPLICIT_BREAK = 5; // ! in rules
    private static final String BREAK_CLASS_TOKENS = "_%#@^!";
    private static final String[] NOT_IN_PAIR_TABLE = { "AI", "BK", "CB", "CR",
        "LF", "NL", "SA", "SG", "SP", "XX" };

    private static byte[] lineBreakProperties = new byte[0x10000];
    private static Map lineBreakPropertyValues = new HashMap();
    private static List lineBreakPropertyShortNames = new ArrayList();
    private static List lineBreakPropertyLongNames = new ArrayList();

    /**
     * Generate a class managing line break properties for Unicode characters
     * and a sample table for the table driven line breaking algorithm described
     * in <a href="http://unicode.org/reports/tr14/#PairBasedImplementation">UTR
     * #14</a>. TODO: Code points above the base plane are simply ignored.
     *
     * @param lineBreakFileName
     *            Name of line break property file (part of Unicode files).
     * @param propertyValueFileName
     *            Name of property values alias file (part of Unicode files).
     * @param breakPairFileName
     *            Name of pair table file (<i>not</i> part of the unicode
     *            files).
     * @param outFileName
     *            Name of the output file.
     * @throws Exception
     *             in case anything goes wrong.
     */
    private static void convertLineBreakProperties(
            // CSOK: MethodLength
            final String lineBreakFileName, final String propertyValueFileName,
            final String breakPairFileName, final String outFileName)
                    throws Exception {

        readLineBreakProperties(lineBreakFileName, propertyValueFileName);
        // read break pair table
        final int lineBreakPropertyValueCount = lineBreakPropertyValues.size();
        final int tableSize = lineBreakPropertyValueCount
                - NOT_IN_PAIR_TABLE.length;
        final Map notInPairTableMap = new HashMap(NOT_IN_PAIR_TABLE.length);
        for (final String element : NOT_IN_PAIR_TABLE) {
            final Object v = lineBreakPropertyValues.get(element);
            if (v == null) {
                throw new Exception("'not in pair table' property not found: "
                        + element);
            }
            notInPairTableMap.put(element, v);
        }
        final byte[][] pairTable = new byte[tableSize][];
        final byte[] columnHeader = new byte[tableSize];
        final byte[] rowHeader = new byte[tableSize];
        final byte[] columnMap = new byte[lineBreakPropertyValueCount + 1];
        Arrays.fill(columnMap, (byte) 255);
        final byte[] rowMap = new byte[lineBreakPropertyValueCount + 1];
        Arrays.fill(rowMap, (byte) 255);
        final BufferedReader b = new BufferedReader(new FileReader(
                breakPairFileName));
        String line = b.readLine();
        int lineNumber = 1;
        String[] lineTokens;
        String name;
        // read header
        if (line != null) {
            lineTokens = line.split("\\s+");
            byte columnNumber = 0;

            for (final String lineToken : lineTokens) {
                name = lineToken;
                if (name.length() > 0) {
                    if (columnNumber >= columnHeader.length) {
                        throw new Exception(breakPairFileName + ':'
                                + lineNumber + ": unexpected column header "
                                + name);
                    }
                    if (notInPairTableMap.get(name) != null) {
                        throw new Exception(breakPairFileName + ':'
                                + lineNumber + ": invalid column header "
                                + name);
                    }
                    final Byte v = (Byte) lineBreakPropertyValues.get(name);
                    if (v != null) {
                        final byte vv = v.byteValue();
                        columnHeader[columnNumber] = vv;
                        columnMap[vv] = columnNumber;
                    } else {
                        throw new Exception(breakPairFileName + ':'
                                + lineNumber + ": unknown column header "
                                + name);
                    }
                    columnNumber++;
                }
            }
            if (columnNumber < columnHeader.length) {
                final StringBuilder missing = new StringBuilder();
                for (int j = 0; j < lineBreakPropertyShortNames.size(); j++) {
                    boolean found = false;
                    for (int k = 0; k < columnNumber; k++) {
                        if (columnHeader[k] == j + 1) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        if (missing.length() > 0) {
                            missing.append(", ");
                        }
                        missing.append((String) lineBreakPropertyShortNames
                                .get(j));
                    }
                }
                throw new Exception(breakPairFileName + ':' + lineNumber
                        + ": missing column for properties: "
                        + missing.toString());
            }
        } else {
            throw new Exception(breakPairFileName + ':' + lineNumber
                    + ": can't read table header");
        }
        line = b.readLine().trim();
        lineNumber++;
        byte rowNumber = 0;
        while (line != null && line.length() > 0) {
            if (rowNumber >= rowHeader.length) {
                throw new Exception(breakPairFileName + ':' + lineNumber
                        + ": unexpected row " + line);
            }
            pairTable[rowNumber] = new byte[tableSize];
            lineTokens = line.split("\\s+");
            if (lineTokens.length > 0) {
                name = lineTokens[0];
                if (notInPairTableMap.get(name) != null) {
                    throw new Exception(breakPairFileName + ':' + lineNumber
                            + ": invalid row header " + name);
                }
                final Byte v = (Byte) lineBreakPropertyValues.get(name);
                if (v != null) {
                    final byte vv = v.byteValue();
                    rowHeader[rowNumber] = vv;
                    rowMap[vv] = rowNumber;
                } else {
                    throw new Exception(breakPairFileName + ':' + lineNumber
                            + ": unknown row header " + name);
                }
            } else {
                throw new Exception(breakPairFileName + ':' + lineNumber
                        + ": can't read row header");
            }
            int columnNumber = 0;
            String token;
            for (int i = 1; i < lineTokens.length; ++i) {
                token = lineTokens[i];
                if (token.length() == 1) {
                    final byte tokenBreakClass = (byte) BREAK_CLASS_TOKENS
                            .indexOf(token.charAt(0));
                    if (tokenBreakClass >= 0) {
                        pairTable[rowNumber][columnNumber] = tokenBreakClass;
                    } else {
                        throw new Exception(breakPairFileName + ':'
                                + lineNumber + ": unexpected token: " + token);
                    }
                } else {
                    throw new Exception(breakPairFileName + ':' + lineNumber
                            + ": token too long: " + token);
                }
                columnNumber++;
            }
            line = b.readLine().trim();
            lineNumber++;
            rowNumber++;
        }
        if (rowNumber < rowHeader.length) {
            final StringBuilder missing = new StringBuilder();
            for (int j = 0; j < lineBreakPropertyShortNames.size(); j++) {
                boolean found = false;
                for (int k = 0; k < rowNumber; k++) {
                    if (rowHeader[k] == j + 1) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (missing.length() > 0) {
                        missing.append(", ");
                    }
                    missing.append((String) lineBreakPropertyShortNames.get(j));
                }
            }
            throw new Exception(breakPairFileName + ':' + lineNumber
                    + ": missing row for properties: " + missing.toString());
        }

        // generate class
        final int rowsize = 512;
        final int blocksize = lineBreakProperties.length / rowsize;
        final byte[][] row = new byte[rowsize][];
        int idx = 0;
        final StringBuilder doStaticLinkCode = new StringBuilder();
        final PrintWriter out = new PrintWriter(new FileWriter(outFileName));
        License.writeJavaLicenseId(out);
        out.println();
        out.println("package org.apache.fop.text.linebreak;");
        out.println();
        out.println("/*");
        out.println(" * !!! THIS IS A GENERATED FILE !!!");
        out.println(" * If updates to the source are needed, then:");
        out.println(" * - apply the necessary modifications to");
        out.println(" *   'src/codegen/unicode/java/org/apache/fop/text/linebreak/GenerateLineBreakUtils.java'");
        out.println(" * - run 'ant codegen-unicode', which will generate a new LineBreakUtils.java");
        out.println(" *   in 'src/java/org/apache/fop/text/linebreak'");
        out.println(" * - commit BOTH changed files");
        out.println(" */");
        out.println();
        out.println("// CSOFF: WhitespaceAfterCheck");
        out.println("// CSOFF: LineLengthCheck");
        out.println();
        out.println("/** Line breaking utilities. */");
        out.println("public final class LineBreakUtils {");
        out.println();
        out.println("    private LineBreakUtils() {");
        out.println("    }");
        out.println();
        out.println("    /** Break class constant */");
        out.println("    public static final byte DIRECT_BREAK = "
                + DIRECT_BREAK + ';');
        out.println("    /** Break class constant */");
        out.println("    public static final byte INDIRECT_BREAK = "
                + INDIRECT_BREAK + ';');
        out.println("    /** Break class constant */");
        out.println("    public static final byte COMBINING_INDIRECT_BREAK = "
                + COMBINING_INDIRECT_BREAK + ';');
        out.println("    /** Break class constant */");
        out.println("    public static final byte COMBINING_PROHIBITED_BREAK = "
                + COMBINING_PROHIBITED_BREAK + ';');
        out.println("    /** Break class constant */");
        out.println("    public static final byte PROHIBITED_BREAK = "
                + PROHIBITED_BREAK + ';');
        out.println("    /** Break class constant */");
        out.println("    public static final byte EXPLICIT_BREAK = "
                + EXPLICIT_BREAK + ';');
        out.println();
        out.println("    private static final byte[][] PAIR_TABLE = {");
        boolean printComma = false;
        for (int i = 1; i <= lineBreakPropertyValueCount; i++) {
            if (printComma) {
                out.println(",");
            } else {
                printComma = true;
            }
            out.print("        {");
            boolean localPrintComma = false;
            for (int j = 1; j <= lineBreakPropertyValueCount; j++) {
                if (localPrintComma) {
                    out.print(", ");
                } else {
                    localPrintComma = true;
                }
                if (columnMap[j] != -1 && rowMap[i] != -1) {
                    out.print(pairTable[rowMap[i]][columnMap[j]]);
                } else {
                    out.print('0');
                }
            }
            out.print('}');
        }
        out.println("};");
        out.println();
        out.println("    private static byte[][] lineBreakProperties = new byte["
                + rowsize + "][];");
        out.println();
        out.println("    private static void init0() {");
        int rowsPrinted = 0;
        int initSections = 0;
        for (int i = 0; i < rowsize; i++) {
            boolean found = false;
            for (int j = 0; j < i; j++) {
                if (row[j] != null) {
                    boolean matched = true;
                    for (int k = 0; k < blocksize; k++) {
                        if (row[j][k] != lineBreakProperties[idx + k]) {
                            matched = false;
                            break;
                        }
                    }
                    if (matched) {
                        found = true;
                        doStaticLinkCode.append("        lineBreakProperties[");
                        doStaticLinkCode.append(i);
                        doStaticLinkCode.append("] = lineBreakProperties[");
                        doStaticLinkCode.append(j);
                        doStaticLinkCode.append("];\n");
                        break;
                    }
                }
            }
            if (!found) {
                if (rowsPrinted >= 64) {
                    out.println("    }");
                    out.println();
                    initSections++;
                    out.println("    private static void init" + initSections
                            + "() {");
                    rowsPrinted = 0;
                }
                row[i] = new byte[blocksize];
                boolean printLocalComma = false;
                out.print("        lineBreakProperties[" + i
                        + "] = new byte[] { ");
                for (int k = 0; k < blocksize; k++) {
                    row[i][k] = lineBreakProperties[idx + k];
                    if (printLocalComma) {
                        out.print(", ");
                    } else {
                        printLocalComma = true;
                    }
                    out.print(row[i][k]);
                }
                out.println("};");
                rowsPrinted++;
            }
            idx += blocksize;
        }
        out.println("    }");
        out.println();
        out.println("    static {");
        for (int i = 0; i <= initSections; i++) {
            out.println("        init" + i + "();");
        }
        out.print(doStaticLinkCode);
        out.println("    }");
        out.println();
        for (int i = 0; i < lineBreakPropertyShortNames.size(); i++) {
            final String shortName = (String) lineBreakPropertyShortNames
                    .get(i);
            out.println("    /** Linebreak property constant */");
            out.print("    public static final byte LINE_BREAK_PROPERTY_");
            out.print(shortName);
            out.print(" = ");
            out.print(i + 1);
            out.println(';');
        }
        out.println();
        final String shortNamePrefix = "    private static String[] lineBreakPropertyShortNames = {";
        out.print(shortNamePrefix);
        int lineLength = shortNamePrefix.length();
        printComma = false;
        for (int i = 0; i < lineBreakPropertyShortNames.size(); i++) {
            name = (String) lineBreakPropertyShortNames.get(i);
            if (printComma) {
                if (lineLength <= MAX_LINE_LENGTH - 2) {
                    out.print(", ");
                } else {
                    out.print(",");
                }
                // count the space anyway to force a linebreak if the comma
                // causes lineLength == MAX_LINE_LENGTH
                lineLength += 2;
            } else {
                printComma = true;
            }
            if (lineLength > MAX_LINE_LENGTH) {
                out.println();
                out.print("        ");
                lineLength = 8;
            }
            out.print('"');
            out.print(name);
            out.print('"');
            lineLength += 2 + name.length();
        }
        out.println("};");
        out.println();
        final String longNamePrefix = "    private static String[] lineBreakPropertyLongNames = {";
        out.print(longNamePrefix);
        lineLength = longNamePrefix.length();
        printComma = false;
        for (int i = 0; i < lineBreakPropertyLongNames.size(); i++) {
            name = (String) lineBreakPropertyLongNames.get(i);
            if (printComma) {
                out.print(',');
                lineLength++;
            } else {
                printComma = true;
            }
            if (lineLength > MAX_LINE_LENGTH) {
                out.println();
                out.print("        ");
                lineLength = 8;
            }
            out.print('"');
            out.print(name);
            out.print('"');
            lineLength += 2 + name.length();
        }
        out.println("};");
        out.println();
        out.println("    /**");
        out.println("     * Return the short name for the linebreak property corresponding");
        out.println("     * to the given symbolic constant.");
        out.println("     *");
        out.println("     * @param i the numeric value of the linebreak property");
        out.println("     * @return the short name of the linebreak property");
        out.println("     */");
        out.println("    public static String getLineBreakPropertyShortName(byte i) {");
        out.println("        if (i > 0 && i <= lineBreakPropertyShortNames.length) {");
        out.println("            return lineBreakPropertyShortNames[i - 1];");
        out.println("        } else {");
        out.println("            return null;");
        out.println("        }");
        out.println("    }");
        out.println();
        out.println("    /**");
        out.println("     * Return the long name for the linebreak property corresponding");
        out.println("     * to the given symbolic constant.");
        out.println("     *");
        out.println("     * @param i the numeric value of the linebreak property");
        out.println("     * @return the long name of the linebreak property");
        out.println("     */");
        out.println("    public static String getLineBreakPropertyLongName(byte i) {");
        out.println("        if (i > 0 && i <= lineBreakPropertyLongNames.length) {");
        out.println("            return lineBreakPropertyLongNames[i - 1];");
        out.println("        } else {");
        out.println("            return null;");
        out.println("        }");
        out.println("    }");
        out.println();
        out.println("    /**");
        out.println("     * Return the linebreak property constant for the given <code>char</code>");
        out.println("     *");
        out.println("     * @param c the <code>char</code> whose linebreak property to return");
        out.println("     * @return the constant representing the linebreak property");
        out.println("     */");
        out.println("    public static byte getLineBreakProperty(char c) {");
        out.println("        return lineBreakProperties[c / " + blocksize
                + "][c % " + blocksize + "];");
        out.println("    }");
        out.println();
        out.println("    /**");
        out.println("     * Return the break class constant for the given pair of linebreak");
        out.println("     * property constants.");
        out.println("     *");
        out.println("     * @param lineBreakPropertyBefore the linebreak property for the first character");
        out.println("     *        in a two-character sequence");
        out.println("     * @param lineBreakPropertyAfter the linebreak property for the second character");
        out.println("     *        in a two-character sequence");
        out.println("     * @return the constant representing the break class");
        out.println("     */");
        out.println("    public static byte getLineBreakPairProperty(int lineBreakPropertyBefore, int lineBreakPropertyAfter) {");
        out.println("        return PAIR_TABLE[lineBreakPropertyBefore - 1][lineBreakPropertyAfter - 1];");
        out.println("    }");
        out.println();
        out.println("}");
        out.flush();
        out.close();
    }

    /**
     * Read line break property value names and the actual properties for the
     * Unicode characters from the respective Unicode files. TODO: Code points
     * above the base plane are simply ignored.
     *
     * @param lineBreakFileName
     *            Name of line break property file.
     * @param propertyValueFileName
     *            Name of property values alias file.
     * @throws Exception
     *             in case anything goes wrong.
     */
    private static void readLineBreakProperties(final String lineBreakFileName,
            final String propertyValueFileName) throws Exception {
        // read property names
        BufferedReader b = new BufferedReader(new InputStreamReader(new URL(
                propertyValueFileName).openStream()));
        String line = b.readLine();
        int lineNumber = 1;
        byte propertyIndex = 1;
        byte indexForUnknown = 0;
        while (line != null) {
            if (line.startsWith("lb")) {
                String shortName;
                String longName = null;
                int semi = line.indexOf(';');
                if (semi < 0) {
                    throw new Exception(propertyValueFileName + ':'
                            + lineNumber + ": missing property short name in "
                            + line);
                }
                line = line.substring(semi + 1);
                semi = line.indexOf(';');
                if (semi > 0) {
                    shortName = line.substring(0, semi).trim();
                    longName = line.substring(semi + 1).trim();
                    semi = longName.indexOf(';');
                    if (semi > 0) {
                        longName = longName.substring(0, semi).trim();
                    }
                } else {
                    shortName = line.trim();
                }
                if (shortName.equals("XX")) {
                    indexForUnknown = propertyIndex;
                }
                lineBreakPropertyValues.put(shortName, new Byte(propertyIndex));
                lineBreakPropertyShortNames.add(shortName);
                lineBreakPropertyLongNames.add(longName);
                propertyIndex++;
                if (propertyIndex <= 0) {
                    throw new Exception(propertyValueFileName + ':'
                            + lineNumber + ": property rolled over in " + line);
                }
            }
            line = b.readLine();
            lineNumber++;
        }
        if (indexForUnknown == 0) {
            throw new Exception(
                    "index for XX (unknown) line break property value not found");
        }

        // read property values
        Arrays.fill(lineBreakProperties, (byte) 0);
        b = new BufferedReader(new InputStreamReader(
                new URL(lineBreakFileName).openStream()));
        line = b.readLine();
        lineNumber = 1;
        while (line != null) {
            int idx = line.indexOf('#');
            if (idx >= 0) {
                line = line.substring(0, idx);
            }
            line = line.trim();
            if (line.length() > 0) {
                idx = line.indexOf(';');
                if (idx <= 0) {
                    throw new Exception(lineBreakFileName + ':' + lineNumber
                            + ": No field delimiter in " + line);
                }
                final Byte v = (Byte) lineBreakPropertyValues.get(line
                        .substring(idx + 1).trim());
                if (v == null) {
                    throw new Exception(lineBreakFileName + ':' + lineNumber
                            + ": Unknown property value in " + line);
                }
                final String codepoint = line.substring(0, idx);
                int low;
                int high;
                idx = codepoint.indexOf("..");
                try {
                    if (idx >= 0) {
                        low = Integer.parseInt(codepoint.substring(0, idx), 16);
                        high = Integer.parseInt(codepoint.substring(idx + 2),
                                16);
                    } else {
                        low = Integer.parseInt(codepoint, 16);
                        high = low;
                    }
                } catch (final NumberFormatException e) {
                    throw new Exception(lineBreakFileName + ':' + lineNumber
                            + ": Invalid codepoint number in " + line);
                }
                if (high > 0xFFFF) {
                    // ignore non-baseplane characters for now

                } else {
                    if (low < 0 || high < 0) {
                        throw new Exception(lineBreakFileName + ':'
                                + lineNumber + ": Negative codepoint(s) in "
                                + line);
                    }
                    final byte vv = v.byteValue();
                    for (int i = low; i <= high; i++) {
                        if (lineBreakProperties[i] != 0) {
                            throw new Exception(lineBreakFileName + ':'
                                    + lineNumber
                                    + ": Property already set for " + (char) i
                                    + " in " + line);
                        }
                        lineBreakProperties[i] = vv;
                    }
                }
            }
            line = b.readLine();
            lineNumber++;
        }
    }

    /**
     * Determine a good block size for the two stage optimized storage of the
     * line breaking properties. Note: the memory utilization calculation is a
     * rule of thumb, don't take it too serious.
     *
     * @param lineBreakFileName
     *            Name of line break property file.
     * @param propertyValueFileName
     *            Name of property values alias file.
     * @throws Exception
     *             in case anything goes wrong.
     */
    private static void optimizeBlocks(final String lineBreakFileName,
            final String propertyValueFileName) throws Exception {
        readLineBreakProperties(lineBreakFileName, propertyValueFileName);
        for (int i = 0; i < 16; i++) {
            final int rowsize = 1 << i;
            final int blocksize = lineBreakProperties.length / rowsize;
            final byte[][] row = new byte[rowsize][];
            int idx = 0;
            int nrOfDistinctBlocks = 0;
            for (int j = 0; j < rowsize; j++) {
                final byte[] block = new byte[blocksize];
                for (int k = 0; k < blocksize; k++) {
                    block[k] = lineBreakProperties[idx];
                    idx++;
                }
                boolean found = false;
                for (int k = 0; k < j; k++) {
                    if (row[k] != null) {
                        boolean matched = true;
                        for (int l = 0; l < blocksize; l++) {
                            if (row[k][l] != block[l]) {
                                matched = false;
                                break;
                            }
                        }
                        if (matched) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    row[j] = block;
                    nrOfDistinctBlocks++;
                } else {
                    row[j] = null;
                }
            }
            final int size = rowsize * 4 + nrOfDistinctBlocks * blocksize;
            log.info("i=" + i + " blocksize=" + blocksize + " blocks="
                    + nrOfDistinctBlocks + " size=" + size);
        }
    }

    /**
     * Main entry point for running GenerateLineBreakUtils
     *
     * @param args
     *            array of command line arg
     */
    public static void main(final String[] args) {
        String lineBreakFileName = "http://www.unicode.org/Public/UNIDATA/LineBreak.txt";
        String propertyValueFileName = "http://www.unicode.org/Public/UNIDATA/PropertyValueAliases.txt";
        String breakPairFileName = "src/codegen/unicode/data/LineBreakPairTable.txt";
        String outFileName = "LineBreakUtils.java";
        boolean ok = true;
        for (int i = 0; i < args.length; i = i + 2) {
            if (i + 1 == args.length) {
                ok = false;
            } else {
                final String opt = args[i];
                if ("-l".equals(opt)) {
                    lineBreakFileName = args[i + 1];
                } else if ("-p".equals(opt)) {
                    propertyValueFileName = args[i + 1];
                } else if ("-b".equals(opt)) {
                    breakPairFileName = args[i + 1];
                } else if ("-o".equals(opt)) {
                    outFileName = args[i + 1];
                } else {
                    ok = false;
                }
            }
        }
        if (!ok) {
            log.info("Usage: GenerateLineBreakUtils [-l <lineBreakFile>] [-p <propertyValueFile>] [-b <breakPairFile>] [-o <outputFile>]");
            log.info("  defaults:");
            log.info("    <lineBreakFile>:     " + lineBreakFileName);
            log.info("    <propertyValueFile>: " + propertyValueFileName);
            log.info("    <breakPairFile>:     " + breakPairFileName);
            log.info("    <outputFile>:        " + outFileName);
        } else {
            try {
                convertLineBreakProperties(lineBreakFileName,
                        propertyValueFileName, breakPairFileName, outFileName);
                log.info("Generated " + outFileName + " from");
                log.info("  <lineBreakFile>:     " + lineBreakFileName);
                log.info("  <propertyValueFile>: " + propertyValueFileName);
                log.info("  <breakPairFile>:     " + breakPairFileName);
            } catch (final Exception e) {
                log.info("An unexpected error occured");
                e.printStackTrace();
            }
        }
    }
}
