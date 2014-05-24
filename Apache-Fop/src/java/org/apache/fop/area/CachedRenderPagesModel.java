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

/* $Id: CachedRenderPagesModel.java 1062901 2011-01-24 18:06:25Z adelmelle $ */

package org.apache.fop.area;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.fop.ResourceEventProducer;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fonts.FontInfo;
import org.xml.sax.SAXException;

/**
 * A simple cached render pages model. If the page is prepared for later
 * rendering then this saves the page contents to a file and once the page is
 * resolved the contents are reloaded.
 */
@Slf4j
public class CachedRenderPagesModel extends RenderPagesModel {

    private final Map<PageViewport, String> pageMap = new HashMap<PageViewport, String>();

    /**
     * Base directory to save temporary file in, typically points to the user's
     * temp dir.
     */
    protected File baseDir;

    /**
     * Main Constructor
     *
     * @param userAgent
     *            FOUserAgent object for process
     * @param outputFormat
     *            the MIME type of the output format to use (ex.
     *            "application/pdf").
     * @param fontInfo
     *            FontInfo object
     * @param stream
     *            OutputStream
     * @throws FOPException
     *             if the renderer cannot be properly initialized
     */
    public CachedRenderPagesModel(final FOUserAgent userAgent,
            final String outputFormat, final FontInfo fontInfo,
            final OutputStream stream) throws FOPException {
        super(userAgent, outputFormat, fontInfo, stream);
        // TODO: Avoid System.getProperty()?
        this.baseDir = new File(System.getProperty("java.io.tmpdir"));
    }

    /** {@inheritDoc} */
    @Override
    protected boolean checkPreparedPages(final PageViewport newpage,
            final boolean renderUnresolved) {
        for (final Iterator iter = this.prepared.iterator(); iter.hasNext();) {
            final PageViewport pageViewport = (PageViewport) iter.next();
            if (pageViewport.isResolved() || renderUnresolved) {
                if (pageViewport != newpage) {
                    try {
                        // load page from cache
                        final String name = this.pageMap.get(pageViewport);
                        final File tempFile = new File(this.baseDir, name);
                        log.debug("Loading page from: " + tempFile);
                        final ObjectInputStream in = new ObjectInputStream(
                                new BufferedInputStream(new FileInputStream(
                                        tempFile)));
                        try {
                            pageViewport.loadPage(in);
                        } finally {
                            IOUtils.closeQuietly(in);
                        }
                        if (!tempFile.delete()) {
                            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                                    .get(this.renderer.getUserAgent()
                                            .getEventBroadcaster());
                            eventProducer.cannotDeleteTempFile(this, tempFile);
                        }
                        this.pageMap.remove(pageViewport);
                    } catch (final Exception e) {
                        final AreaEventProducer eventProducer = AreaEventProducer.Provider
                                .get(this.renderer.getUserAgent()
                                        .getEventBroadcaster());
                        eventProducer.pageLoadError(this,
                                pageViewport.getPageNumberString(), e);
                    }
                }

                renderPage(pageViewport);
                pageViewport.clear();
                iter.remove();
            } else {
                if (!this.renderer.supportsOutOfOrder()) {
                    break;
                }
            }
        }
        if (newpage != null && newpage.getPage() != null) {
            savePage(newpage);
            newpage.clear();
        }
        return this.renderer.supportsOutOfOrder() || this.prepared.isEmpty();
    }

    /**
     * Save a page. It saves the contents of the page to a file.
     *
     * @param page
     *            the page to prepare
     */
    protected void savePage(final PageViewport page) {
        try {
            // save page to cache
            ObjectOutputStream tempstream;
            final String fname = "fop-page-" + page.getPageIndex() + ".ser";
            final File tempFile = new File(this.baseDir, fname);
            tempFile.deleteOnExit();
            tempstream = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(tempFile)));
            try {
                page.savePage(tempstream);
            } finally {
                IOUtils.closeQuietly(tempstream);
            }
            this.pageMap.put(page, fname);
            if (log.isDebugEnabled()) {
                log.debug("Page saved to temporary file: " + tempFile);
            }
        } catch (final IOException ioe) {
            final AreaEventProducer eventProducer = AreaEventProducer.Provider
                    .get(this.renderer.getUserAgent().getEventBroadcaster());
            eventProducer.pageSaveError(this, page.getPageNumberString(), ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
    }
}
