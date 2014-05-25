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

/* $Id: Fop.java 1297232 2012-03-05 21:13:28Z gadams $ */

package org.apache.fop.tools.anttasks;

// Ant
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.cli.InputHandler;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.xml.sax.SAXException;

/**
 * Wrapper for FOP which allows it to be accessed from within an Ant task.
 * Accepts the inputs:
 * <ul>
 * <li>fofile -> formatting objects file to be transformed</li>
 * <li>format -> MIME type of the format to generate ex. "application/pdf"</li>
 * <li>outfile -> output filename</li>
 * <li>baseDir -> directory to work from</li>
 * <li>relativebase -> (true | false) control whether to use each FO's directory
 * as base directory. false uses the baseDir parameter.</li>
 * <li>userconfig -> file with user configuration (same as the "-c" command line
 * option)</li>
 * <li>messagelevel -> (error | warn | info | verbose | debug) level to output
 * non-error messages</li>
 * <li>logFiles -> Controls whether the names of the files that are processed
 * are logged or not</li>
 * </ul>
 */
public class Fop extends Task {

    private File foFile;
    private File xmlFile;
    private File xsltFile;
    private String xsltParams;
    private final List/* <FileSet> */filesets = new java.util.ArrayList/*
     * <FileSet
     * >
     */();
    private File outFile;
    private File outDir;
    private String format; // MIME type
    private File baseDir;
    private File userConfig;
    private int messageType = Project.MSG_VERBOSE;
    private boolean logFiles = true;
    private boolean force = false;
    private boolean relativebase = false;
    private boolean throwExceptions = true;

    /**
     * Sets the filename for the userconfig.xml.
     *
     * @param userConfig
     *            Configuration to use
     */
    public void setUserconfig(final File userConfig) {
        this.userConfig = userConfig;
    }

    /**
     * Returns the file for the userconfig.xml.
     *
     * @return the userconfig.xml file
     */
    public File getUserconfig() {
        return this.userConfig;
    }

    /**
     * Sets the input XSL-FO file.
     *
     * @param foFile
     *            input XSL-FO file
     */
    public void setFofile(final File foFile) {
        this.foFile = foFile;
    }

    /**
     * Gets the input XSL-FO file.
     *
     * @return input XSL-FO file
     */
    public File getFofile() {
        return this.foFile;
    }

    /**
     * Gets the input XML file.
     *
     * @return the input XML file.
     */
    public File getXmlFile() {
        return this.xmlFile;
    }

    /**
     * Sets the input XML file.
     *
     * @param xmlFile
     *            the input XML file.
     */
    public void setXmlFile(final File xmlFile) {
        this.xmlFile = xmlFile;
    }

    /**
     * Gets the input XSLT file.
     *
     * @return the input XSLT file.
     */
    public File getXsltFile() {
        return this.xsltFile;
    }

    /**
     * Sets the input XSLT file.
     *
     * @param xsltFile
     *            the input XSLT file.
     */
    public void setXsltFile(final File xsltFile) {
        this.xsltFile = xsltFile;
    }

    /**
     * Gets the XSLT parameters
     *
     * @return the XSLT parameters
     */
    public String getXsltParams() {
        return this.xsltParams;
    }

    /**
     * Sets the XSLT parameters
     *
     * @param xsltParams
     *            the XSLT parameters
     */
    public void setXsltParams(final String xsltParams) {
        this.xsltParams = xsltParams;
    }

    /**
     * Adds a set of XSL-FO files (nested fileset attribute).
     *
     * @param set
     *            a fileset
     */
    public void addFileset(final FileSet set) {
        this.filesets.add(set);
    }

    /**
     * Returns the current list of filesets.
     *
     * @return the filesets
     */
    public List getFilesets() {
        return this.filesets;
    }

    /**
     * Set whether to include files (external-graphics, instream-foreign-object)
     * from a path relative to the .fo file (true) or the working directory
     * (false, default) only useful for filesets
     *
     * @param relbase
     *            true if paths are relative to file.
     */
    public void setRelativebase(final boolean relbase) {
        this.relativebase = relbase;
    }

    /**
     * Gets the relative base attribute
     *
     * @return the relative base attribute
     */
    public boolean getRelativebase() {
        return this.relativebase;
    }

    /**
     * Set whether to check dependencies, or to always generate; optional,
     * default is false.
     *
     * @param force
     *            true if always generate.
     */
    public void setForce(final boolean force) {
        this.force = force;
    }

    /**
     * Gets the force attribute
     *
     * @return the force attribute
     */
    public boolean getForce() {
        return this.force;
    }

    /**
     * Sets the output file.
     *
     * @param outFile
     *            File to output to
     */
    public void setOutfile(final File outFile) {
        this.outFile = outFile;
    }

    /**
     * Gets the output file.
     *
     * @return the output file
     */
    public File getOutfile() {
        return this.outFile;
    }

    /**
     * Sets the output directory.
     *
     * @param outDir
     *            Directory to output to
     */
    public void setOutdir(final File outDir) {
        this.outDir = outDir;
    }

    /**
     * Gets the output directory.
     *
     * @return the output directory
     */
    public File getOutdir() {
        return this.outDir;
    }

    /**
     * Sets output format (MIME type).
     *
     * @param format
     *            the output format
     */
    public void setFormat(final String format) {
        this.format = format;
    }

    /**
     * Gets the output format (MIME type).
     *
     * @return the output format
     */
    public String getFormat() {
        return this.format;
    }

    /**
     * Set whether exceptions are thrown. default is false.
     *
     * @param throwExceptions
     *            true if exceptions should be thrown
     */
    public void setThrowexceptions(final boolean throwExceptions) {
        this.throwExceptions = throwExceptions;
    }

    /**
     * Gets the throw exceptions attribute
     *
     * @return the throw exceptions attribute
     */
    public boolean getThrowexceptions() {
        return this.throwExceptions;
    }

    /**
     * Sets the message level to be used while processing.
     *
     * @param messageLevel
     *            (error | warn| info | verbose | debug)
     */
    public void setMessagelevel(final String messageLevel) {
        if (messageLevel.equalsIgnoreCase("info")) {
            this.messageType = Project.MSG_INFO;
        } else if (messageLevel.equalsIgnoreCase("verbose")) {
            this.messageType = Project.MSG_VERBOSE;
        } else if (messageLevel.equalsIgnoreCase("debug")) {
            this.messageType = Project.MSG_DEBUG;
        } else if (messageLevel.equalsIgnoreCase("err")
                || messageLevel.equalsIgnoreCase("error")) {
            this.messageType = Project.MSG_ERR;
        } else if (messageLevel.equalsIgnoreCase("warn")) {
            this.messageType = Project.MSG_WARN;
        } else {
            log("messagelevel set to unknown value \"" + messageLevel + "\"",
                    Project.MSG_ERR);
            throw new BuildException("unknown messagelevel");
        }
    }

    /**
     * Returns the message type corresponding to Project.MSG_* representing the
     * current message level.
     *
     * @return message type
     * @see org.apache.tools.ant.Project
     */
    public int getMessageType() {
        return this.messageType;
    }

    /**
     * Sets the base directory for single FO file (non-fileset) usage
     *
     * @param baseDir
     *            File to use as a working directory
     */
    public void setBasedir(final File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Gets the base directory.
     *
     * @return the base directory
     */
    public File getBasedir() {
        return this.baseDir != null ? this.baseDir : getProject().resolveFile(
                ".");
    }

    /**
     * Controls whether the filenames of the files that are processed are logged
     * or not.
     *
     * @param logFiles
     *            True if the feature should be enabled
     */
    public void setLogFiles(final boolean logFiles) {
        this.logFiles = logFiles;
    }

    /**
     * Returns True if the filename of each file processed should be logged.
     *
     * @return True if the filenames should be logged.
     */
    public boolean getLogFiles() {
        return this.logFiles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws BuildException {
        try {
            final FOPTaskStarter starter = new FOPTaskStarter(this);
            starter.run();
        } catch (final FOPException ex) {
            throw new BuildException(ex);
        } catch (final IOException ioe) {
            throw new BuildException(ioe);
        } catch (final SAXException saxex) {
            throw new BuildException(saxex);
        }

    }

}

@Slf4j
class FOPTaskStarter {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();

    private final Fop task;
    private String baseURL = null;

    /**
     * Sets the Commons-Logging instance for this class
     *
     * @param log
     *            The Commons-Logging instance
     */
    public void setLogger() {
    }

    FOPTaskStarter(final Fop task) throws SAXException, IOException {
        this.task = task;
        if (task.getUserconfig() != null) {
            this.fopFactory.setUserConfig(task.getUserconfig());
        }
    }

    private static final String[][] SHORT_NAMES = {
        { "pdf", MimeConstants.MIME_PDF },
        { "ps", MimeConstants.MIME_POSTSCRIPT },
        { "mif", MimeConstants.MIME_MIF },
        { "rtf", MimeConstants.MIME_RTF },
        { "pcl", MimeConstants.MIME_PCL },
        { "txt", MimeConstants.MIME_PLAIN_TEXT },
        { "at", MimeConstants.MIME_FOP_AREA_TREE },
        { "xml", MimeConstants.MIME_FOP_AREA_TREE },
        { "tiff", MimeConstants.MIME_TIFF },
        { "tif", MimeConstants.MIME_TIFF },
        { "png", MimeConstants.MIME_PNG },
        { "afp", MimeConstants.MIME_AFP } };

    private String normalizeOutputFormat(final String format) {
        if (format == null) {
            return org.apache.xmlgraphics.util.MimeConstants.MIME_PDF;
        }
        for (final String[] element : SHORT_NAMES) {
            if (element[0].equals(format)) {
                return element[1];
            }
        }
        return format; // no change
    }

    private static final String[][] EXTENSIONS = {
        { MimeConstants.MIME_FOP_AREA_TREE, ".at.xml" },
        { MimeConstants.MIME_FOP_AWT_PREVIEW, null },
        { MimeConstants.MIME_FOP_PRINT, null },
        { MimeConstants.MIME_PDF, ".pdf" },
        { MimeConstants.MIME_POSTSCRIPT, ".ps" },
        { MimeConstants.MIME_PCL, ".pcl" },
        { MimeConstants.MIME_PCL_ALT, ".pcl" },
        { MimeConstants.MIME_PLAIN_TEXT, ".txt" },
        { MimeConstants.MIME_RTF, ".rtf" },
        { MimeConstants.MIME_RTF_ALT1, ".rtf" },
        { MimeConstants.MIME_RTF_ALT2, ".rtf" },
        { MimeConstants.MIME_MIF, ".mif" },
        { MimeConstants.MIME_SVG, ".svg" },
        { MimeConstants.MIME_PNG, ".png" },
        { MimeConstants.MIME_JPEG, ".jpg" },
        { MimeConstants.MIME_TIFF, ".tif" },
        { MimeConstants.MIME_AFP, ".afp" },
        { MimeConstants.MIME_AFP_ALT, ".afp" },
        { MimeConstants.MIME_XSL_FO, ".fo" } };

    private String determineExtension(final String outputFormat) {
        for (final String[] element : EXTENSIONS) {
            if (element[0].equals(outputFormat)) {
                final String ext = element[1];
                if (ext == null) {
                    throw new RuntimeException("Output format '" + outputFormat
                            + "' does not produce a file.");
                } else {
                    return ext;
                }
            }
        }
        return ".unk"; // unknown
    }

    private File replaceExtension(final File file, final String expectedExt,
            final String newExt) {
        String name = file.getName();
        if (name.toLowerCase().endsWith(expectedExt)) {
            name = name.substring(0, name.length() - expectedExt.length());
        }
        name = name.concat(newExt);
        return new File(file.getParentFile(), name);
    }

    public void run() {
        // Set base directory
        if (this.task.getBasedir() != null) {
            try {
                this.baseURL = this.task.getBasedir().toURI().toURL()
                        .toExternalForm();
            } catch (final MalformedURLException mfue) {
                log.error("Error creating base URL from base directory", mfue);
            }
        } else {
            try {
                if (this.task.getFofile() != null) {
                    this.baseURL = this.task.getFofile().getParentFile()
                            .toURI().toURL().toExternalForm();
                }
            } catch (final MalformedURLException mfue) {
                log.error("Error creating base URL from XSL-FO input file",
                        mfue);
            }
        }

        this.task.log("Using base URL: " + this.baseURL, Project.MSG_DEBUG);

        final String outputFormat = normalizeOutputFormat(this.task.getFormat());
        final String newExtension = determineExtension(outputFormat);

        // actioncount = # of fofiles actually processed through FOP
        int actioncount = 0;
        // skippedcount = # of fofiles which haven't changed (force = "false")
        int skippedcount = 0;

        // deal with single source file
        if (this.task.getFofile() != null) {
            if (this.task.getFofile().exists()) {
                File outf = this.task.getOutfile();
                if (outf == null) {
                    throw new BuildException(
                            "outfile is required when fofile is used");
                }
                if (this.task.getOutdir() != null) {
                    outf = new File(this.task.getOutdir(), outf.getName());
                }
                // Render if "force" flag is set OR
                // OR output file doesn't exist OR
                // output file is older than input file
                if (this.task.getForce()
                        || !outf.exists()
                        || this.task.getFofile().lastModified() > outf
                        .lastModified()) {
                    render(this.task.getFofile(), outf, outputFormat);
                    actioncount++;
                } else if (outf.exists()
                        && this.task.getFofile().lastModified() <= outf
                        .lastModified()) {
                    skippedcount++;
                }
            }
        } else if (this.task.getXmlFile() != null
                && this.task.getXsltFile() != null) {
            if (this.task.getXmlFile().exists()
                    && this.task.getXsltFile().exists()) {
                File outf = this.task.getOutfile();
                if (outf == null) {
                    throw new BuildException(
                            "outfile is required when fofile is used");
                }
                if (this.task.getOutdir() != null) {
                    outf = new File(this.task.getOutdir(), outf.getName());
                }
                // Render if "force" flag is set OR
                // OR output file doesn't exist OR
                // output file is older than input file
                if (this.task.getForce()
                        || !outf.exists()
                        || this.task.getXmlFile().lastModified() > outf
                        .lastModified()
                        || this.task.getXsltFile().lastModified() > outf
                        .lastModified()) {
                    render(this.task.getXmlFile(), this.task.getXsltFile(),
                            outf, outputFormat);
                    actioncount++;
                } else if (outf.exists()
                        && (this.task.getXmlFile().lastModified() <= outf
                        .lastModified() || this.task.getXsltFile()
                        .lastModified() <= outf.lastModified())) {
                    skippedcount++;
                }
            }
        }

        final GlobPatternMapper mapper = new GlobPatternMapper();

        String inputExtension = ".fo";
        final File xsltFile = this.task.getXsltFile();
        if (xsltFile != null) {
            inputExtension = ".xml";
        }
        mapper.setFrom("*" + inputExtension);
        mapper.setTo("*" + newExtension);

        // deal with the filesets
        for (int i = 0; i < this.task.getFilesets().size(); i++) {
            final FileSet fs = (FileSet) this.task.getFilesets().get(i);
            final DirectoryScanner ds = fs.getDirectoryScanner(this.task
                    .getProject());
            final String[] files = ds.getIncludedFiles();

            for (final String file : files) {
                final File f = new File(fs.getDir(this.task.getProject()), file);

                File outf = null;
                if (this.task.getOutdir() != null
                        && file.endsWith(inputExtension)) {
                    final String[] sa = mapper.mapFileName(file);
                    outf = new File(this.task.getOutdir(), sa[0]);
                } else {
                    outf = replaceExtension(f, inputExtension, newExtension);
                    if (this.task.getOutdir() != null) {
                        outf = new File(this.task.getOutdir(), outf.getName());
                    }
                }
                final File dir = outf.getParentFile();
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                try {
                    if (this.task.getRelativebase()) {
                        this.baseURL = f.getParentFile().toURI().toURL()
                                .toExternalForm();
                    }
                    if (this.baseURL == null) {
                        this.baseURL = fs.getDir(this.task.getProject())
                                .toURI().toURL().toExternalForm();
                    }

                } catch (final Exception e) {
                    this.task.log("Error setting base URL", Project.MSG_DEBUG);
                }

                // Render if "force" flag is set OR
                // OR output file doesn't exist OR
                // output file is older than input file
                if (this.task.getForce() || !outf.exists()
                        || f.lastModified() > outf.lastModified()) {
                    if (xsltFile != null) {
                        render(f, xsltFile, outf, outputFormat);
                    } else {
                        render(f, outf, outputFormat);
                    }
                    actioncount++;
                } else if (outf.exists()
                        && f.lastModified() <= outf.lastModified()) {
                    skippedcount++;
                }
            }
        }

        if (actioncount + skippedcount == 0) {
            this.task.log(
                    "No files processed. No files were selected by the filesets "
                            + "and no fofile was set.", Project.MSG_WARN);
        } else if (skippedcount > 0) {
            this.task
            .log(skippedcount
                    + " xslfo file(s) skipped (no change found"
                    + " since last generation; set force=\"true\" to override).",
                    Project.MSG_INFO);
        }
    }

    private void renderInputHandler(final InputHandler inputHandler,
            final File outFile, final String outputFormat) throws Exception {
        OutputStream out = null;
        try {
            out = new java.io.FileOutputStream(outFile);
            out = new BufferedOutputStream(out);
        } catch (final Exception ex) {
            throw new BuildException("Failed to open " + outFile, ex);
        }
        boolean success = false;
        try {
            final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();
            userAgent.setBaseURL(this.baseURL);
            inputHandler.renderTo(userAgent, outputFormat, out);
            success = true;
        } catch (final Exception ex) {
            if (this.task.getThrowexceptions()) {
                throw new BuildException(ex);
            }
            throw ex;
        } finally {
            try {
                out.close();
            } catch (final IOException ioe) {
                log.error("Error closing output file", ioe);
            }
            if (!success) {
                outFile.delete();
            }
        }
    }

    private void render(final File foFile, final File outFile,
            final String outputFormat) {
        final InputHandler inputHandler = new InputHandler(foFile);
        try {
            renderInputHandler(inputHandler, outFile, outputFormat);
        } catch (final Exception ex) {
            log.error("Error rendering fo file: " + foFile, ex);
        }
        if (this.task.getLogFiles()) {
            this.task.log(foFile + " -> " + outFile, Project.MSG_INFO);
        }
    }

    private void render(final File xmlFile, final File xsltFile,
            final File outFile, final String outputFormat) {
        // TODO: implement support for XSLT params
        final List xsltParams = null;
        final InputHandler inputHandler = new InputHandler(xmlFile, xsltFile,
                xsltParams);
        try {
            renderInputHandler(inputHandler, outFile, outputFormat);
        } catch (final Exception ex) {
            log.error("Error rendering xml/xslt files: " + xmlFile + ", "
                    + xsltFile, ex);
        }
        if (this.task.getLogFiles()) {
            this.task.log("xml: " + xmlFile + ", xslt: " + xsltFile + " -> "
                    + outFile, Project.MSG_INFO);
        }
    }
}
