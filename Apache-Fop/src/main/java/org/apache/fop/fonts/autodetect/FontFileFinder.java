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

/* $Id: FontFileFinder.java 1330050 2012-04-24 22:28:42Z gadams $ */

package org.apache.fop.fonts.autodetect;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.fop.fonts.FontEventListener;

/**
 * Helps to autodetect/locate available operating system fonts.
 */
@Slf4j
public class FontFileFinder extends DirectoryWalker implements FontFinder {

    /** default depth limit of recursion when searching for font files **/
    public static final int DEFAULT_DEPTH_LIMIT = -1;
    private final FontEventListener eventListener;

    /**
     * Default constructor
     *
     * @param listener
     *            for throwing font related events
     */
    public FontFileFinder(final FontEventListener listener) {
        this(DEFAULT_DEPTH_LIMIT, listener);
    }

    /**
     * Constructor
     *
     * @param depthLimit
     *            recursion depth limit
     * @param listener
     *            for throwing font related events
     */
    public FontFileFinder(final int depthLimit, final FontEventListener listener) {
        super(getDirectoryFilter(), getFileFilter(), depthLimit);
        this.eventListener = listener;
    }

    /**
     * Font directory filter. Currently ignores hidden directories.
     *
     * @return IOFileFilter font directory filter
     */
    protected static IOFileFilter getDirectoryFilter() {
        return FileFilterUtils.and(FileFilterUtils.directoryFileFilter(),
                FileFilterUtils.notFileFilter(FileFilterUtils
                        .prefixFileFilter(".")));
    }

    /**
     * Font file filter. Currently searches for files with .ttf, .ttc, .otf, and
     * .pfb extensions.
     *
     * @return IOFileFilter font file filter
     */
    protected static IOFileFilter getFileFilter() {
        return FileFilterUtils.and(FileFilterUtils.fileFileFilter(),
                new WildcardFileFilter(new String[] { "*.ttf", "*.otf",
                        "*.pfb", "*.ttc" }, IOCase.INSENSITIVE));
    }

    /**
     * @param directory
     *            directory to handle
     * @param depth
     *            recursion depth
     * @param results
     *            collection
     * @return whether directory should be handled {@inheritDoc}
     */
    @Override
    protected boolean handleDirectory(final File directory, final int depth,
            final Collection results) {
        return true;
    }

    /**
     * @param file
     *            file to handle
     * @param depth
     *            recursion depth
     * @param results
     *            collection {@inheritDoc}
     */
    @Override
    protected void handleFile(final File file, final int depth,
            final Collection results) {
        try {
            // Looks Strange, but is actually recommended over just .URL()
            results.add(file.toURI().toURL());
        } catch (final MalformedURLException e) {
            log.debug("MalformedURLException" + e.getMessage());
        }
    }

    /**
     * @param directory
     *            the directory being processed
     * @param depth
     *            the current directory level
     * @param results
     *            the collection of results objects {@inheritDoc}
     */
    @Override
    protected void handleDirectoryEnd(final File directory, final int depth,
            final Collection results) {
        if (log.isDebugEnabled()) {
            log.debug(directory + ": found " + results.size() + " font"
                    + (results.size() == 1 ? "" : "s"));
        }
    }

    /**
     * Automagically finds a list of font files on local system
     *
     * @return List&lt;URL&gt; of font files
     * @throws IOException
     *             io exception {@inheritDoc}
     */
    @Override
    public List<URL> find() throws IOException {
        final FontDirFinder fontDirFinder;
        final String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            fontDirFinder = new WindowsFontDirFinder();
        } else {
            if (osName.startsWith("Mac")) {
                fontDirFinder = new MacFontDirFinder();
            } else {
                fontDirFinder = new UnixFontDirFinder();
            }
        }
        final List<File> fontDirs = fontDirFinder.find();
        final List<URL> results = new java.util.ArrayList<URL>();
        for (final File dir : fontDirs) {
            super.walk(dir, results);
        }
        return results;
    }

    /**
     * Searches a given directory for font files
     *
     * @param dir
     *            directory to search
     * @return list of font files
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred
     */
    public List<URL> find(final String dir) throws IOException {
        final List<URL> results = new java.util.ArrayList<URL>();
        final File directory = new File(dir);
        if (!directory.isDirectory()) {
            this.eventListener.fontDirectoryNotFound(this, dir);
        } else {
            super.walk(directory, results);
        }
        return results;
    }
}
