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

/* $Id: FileCompare.java 1311120 2012-04-08 23:48:11Z gadams $ */

package org.apache.fop.tools.anttasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import lombok.extern.slf4j.Slf4j;

import org.apache.tools.ant.BuildException;

/**
 * This class is an extension of Ant, a script utility from
 * http://ant.apache.org. It provides methods to compare two files.
 */
@Slf4j
public class FileCompare {

    private String referenceDirectory;
    private String testDirectory;
    private String[] filenameList;
    private String filenames;

    /**
     * Sets directory for test files.
     *
     * @param testDirectory
     *            the test directory
     */
    public void setTestDirectory(String testDirectory) {
        if (!(testDirectory.endsWith("/") | testDirectory.endsWith("\\"))) {
            testDirectory += File.separator;
        }
        this.testDirectory = testDirectory;
    }

    /**
     * Sets directory for reference files.
     *
     * @param referenceDirectory
     *            the reference directory
     */
    public void setReferenceDirectory(String referenceDirectory) {
        if (!(referenceDirectory.endsWith("/") | referenceDirectory
                .endsWith("\\"))) {
            referenceDirectory += File.separator;
        }
        this.referenceDirectory = referenceDirectory;
    }

    /**
     * Sets the comma-separated list of files to process.
     *
     * @param filenames
     *            list of files, comma-separated
     */
    public void setFilenames(final String filenames) {
        final StringTokenizer tokens = new StringTokenizer(filenames, ",");
        final List filenameListTmp = new java.util.ArrayList(20);
        while (tokens.hasMoreTokens()) {
            filenameListTmp.add(tokens.nextToken());
        }
        this.filenameList = new String[filenameListTmp.size()];
        this.filenameList = (String[]) filenameListTmp.toArray(new String[0]);
    }

    /**
     * Compares two files to see if they are equal
     *
     * @param f1
     *            first file to compare
     * @param f2
     *            second file to compare
     * @return true if files are same, false otherwise
     * @throws IOException
     *             if not caught
     */
    public static boolean compareFiles(final File f1, final File f2)
            throws IOException {
        return compareFileSize(f1, f2) && compareBytes(f1, f2);
    }

    /**
     * Compare the contents of two files.
     *
     * @param file1
     *            the first file to compare
     * @param file2
     *            the second file to compare
     * @return true if files are same byte-by-byte, false otherwise
     */
    private static boolean compareBytes(final File file1, final File file2)
            throws IOException {
        final BufferedInputStream file1Input = new BufferedInputStream(
                new java.io.FileInputStream(file1));
        final BufferedInputStream file2Input = new BufferedInputStream(
                new java.io.FileInputStream(file2));

        int charact1 = 0;
        int charact2 = 0;

        while (charact1 != -1) {
            if (charact1 == charact2) {
                charact1 = file1Input.read();
                charact2 = file2Input.read();
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Does a file size compare of two files
     *
     * @param oldFile
     *            the first file to compare
     * @param newFile
     *            the second file to compare
     * @return true if files are same length, false otherwise
     */
    private static boolean compareFileSize(final File oldFile,
            final File newFile) {
        return oldFile.length() == newFile.length();
    }

    private boolean filesExist(final File oldFile, final File newFile) {
        if (!oldFile.exists()) {
            System.err.println("Task Compare - ERROR: File "
                    + this.referenceDirectory + oldFile.getName()
                    + " doesn't exist!");
            return false;
        } else if (!newFile.exists()) {
            System.err.println("Task Compare - ERROR: File "
                    + this.testDirectory + newFile.getName()
                    + " doesn't exist!");
            return false;
        } else {
            return true;
        }
    }

    private void writeHeader(final PrintWriter results) {
        final String dateTime = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.MEDIUM).format(new Date());
        results.println("<html><head><title>Test Results</title></head><body>\n");
        results.println("<h2>Compare Results<br>");
        results.println("<font size='1'>created " + dateTime + "</font></h2>");
        results.println("<table cellpadding='10' border='2'><thead>"
                + "<th align='center'>reference file</th>"
                + "<th align='center'>test file</th>"
                + "<th align='center'>identical?</th></thead>");
    }

    /**
     * Main method of task compare
     *
     * @throws BuildException
     *             If the execution fails.
     */
    public void execute() throws BuildException {
        boolean identical = false;
        File oldFile;
        File newFile;
        try {
            final PrintWriter results = new PrintWriter(new java.io.FileWriter(
                    "results.html"), true);
            writeHeader(results);
            for (final String element : this.filenameList) {
                oldFile = new File(this.referenceDirectory + element);
                newFile = new File(this.testDirectory + element);
                if (filesExist(oldFile, newFile)) {
                    identical = compareFileSize(oldFile, newFile);
                    if (identical) {
                        identical = compareBytes(oldFile, newFile);
                    }
                    if (!identical) {
                        log.info("Task Compare: \nFiles "
                                + this.referenceDirectory + oldFile.getName()
                                + " - " + this.testDirectory
                                + newFile.getName() + " are *not* identical.");
                        results.println("<tr><td><a href='"
                                + this.referenceDirectory
                                + oldFile.getName()
                                + "'>"
                                + oldFile.getName()
                                + "</a> </td><td> <a href='"
                                + this.testDirectory
                                + newFile.getName()
                                + "'>"
                                + newFile.getName()
                                + "</a>"
                                + " </td><td><font color='red'>No</font></td></tr>");
                    } else {
                        results.println("<tr><td><a href='"
                                + this.referenceDirectory + oldFile.getName()
                                + "'>" + oldFile.getName()
                                + "</a> </td><td> <a href='"
                                + this.testDirectory + newFile.getName() + "'>"
                                + newFile.getName() + "</a>"
                                + " </td><td>Yes</td></tr>");
                    }
                }
            }
            results.println("</table></html>");
        } catch (final IOException ioe) {
            System.err.println("ERROR: " + ioe);
        }
    } // end: execute()

}
