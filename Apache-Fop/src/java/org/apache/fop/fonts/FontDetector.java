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

/* $Id: FontDetector.java 1198853 2011-11-07 18:18:29Z vhennebert $ */

package org.apache.fop.fonts;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fonts.autodetect.FontFileFinder;
import org.apache.fop.util.LogUtil;
import org.apache.xmlgraphics.util.ClasspathResource;

/**
 * Detector of operating system and classpath fonts
 */
@Slf4j
public class FontDetector {

    private static final String[] FONT_MIMETYPES = { "application/x-font",
    "application/x-font-truetype" };

    private final FontManager fontManager;
    private final FontAdder fontAdder;
    private final boolean strict;
    private final FontEventListener eventListener;

    /**
     * Main constructor
     *
     * @param manager
     *            the font manager
     * @param adder
     *            the font adder
     * @param strict
     *            true if an Exception should be thrown if an error is found.
     * @param listener
     *            for throwing font related events
     */
    public FontDetector(final FontManager manager, final FontAdder adder,
            final boolean strict, final FontEventListener listener) {
        this.fontManager = manager;
        this.fontAdder = adder;
        this.strict = strict;
        this.eventListener = listener;
    }

    /**
     * Detect installed fonts on the system
     *
     * @param fontInfoList
     *            a list of fontinfo to populate
     * @throws FOPException
     *             thrown if a problem occurred during detection
     */
    public void detect(final List<EmbedFontInfo> fontInfoList)
            throws FOPException {
        // search in font base if it is defined and
        // is a directory but don't recurse
        final FontFileFinder fontFileFinder = new FontFileFinder(
                this.eventListener);
        final String fontBaseURL = this.fontManager.getFontBaseURL();
        if (fontBaseURL != null) {
            try {
                final File fontBase = FileUtils.toFile(new URL(fontBaseURL));
                if (fontBase != null) {
                    final List<URL> fontURLList = fontFileFinder.find(fontBase
                            .getAbsolutePath());
                    this.fontAdder.add(fontURLList, fontInfoList);

                    // Can only use the font base URL if it's a file URL
                }
            } catch (final IOException e) {
                LogUtil.handleException(log, e, this.strict);
            }
        }

        // native o/s font directory finding
        List<URL> systemFontList;
        try {
            systemFontList = fontFileFinder.find();
            this.fontAdder.add(systemFontList, fontInfoList);
        } catch (final IOException e) {
            LogUtil.handleException(log, e, this.strict);
        }

        // classpath font finding
        final ClasspathResource resource = ClasspathResource.getInstance();
        for (final String element : FONT_MIMETYPES) {
            this.fontAdder.add(resource.listResourcesOfMimeType(element),
                    fontInfoList);
        }
    }
}
