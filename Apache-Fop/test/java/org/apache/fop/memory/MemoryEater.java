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

/* $Id: MemoryEater.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.memory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;

/**
 * Debug tool to create and process large FO files by replicating them a
 * specified number of times.
 */
@Slf4j
public class MemoryEater {

    private final SAXTransformerFactory tFactory = (SAXTransformerFactory) SAXTransformerFactory
            .newInstance();
    private final FopFactory fopFactory = FopFactory.newInstance();
    private final Templates replicatorTemplates;

    private Stats stats;

    public MemoryEater() throws TransformerConfigurationException {
        final File xsltFile = new File("test/xsl/fo-replicator.xsl");
        final Source xslt = new StreamSource(xsltFile);
        this.replicatorTemplates = this.tFactory.newTemplates(xslt);
    }

    private void eatMemory(final File foFile, final int runRepeats,
            final int replicatorRepeats) throws Exception {
        this.stats = new Stats();
        for (int i = 0; i < runRepeats; i++) {
            eatMemory(i, foFile, replicatorRepeats);
            this.stats.progress(i, runRepeats);
        }
        this.stats.dumpFinalStats();
        log.info(this.stats.getGoogleChartURL());
    }

    private void eatMemory(final int callIndex, final File foFile,
            final int replicatorRepeats) throws Exception {
        final Source src = new StreamSource(foFile);

        final Transformer transformer = this.replicatorTemplates
                .newTransformer();
        transformer.setParameter("repeats", new Integer(replicatorRepeats));

        final OutputStream out = new NullOutputStream(); // write to /dev/nul
        try {
            final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();
            userAgent.setBaseURL(foFile.getParentFile().toURI().toURL()
                    .toExternalForm());
            final Fop fop = this.fopFactory.newFop(
                    org.apache.xmlgraphics.util.MimeConstants.MIME_PDF,
                    userAgent, out);
            final Result res = new SAXResult(fop.getDefaultHandler());

            transformer.transform(src, res);

            this.stats.notifyPagesProduced(fop.getResults().getPageCount());
            if (callIndex == 0) {
                log.info(foFile.getName() + " generates "
                        + fop.getResults().getPageCount() + " pages.");
            }
            this.stats.checkStats();
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private static void prompt() throws IOException {
        final BufferedReader in = new BufferedReader(
                new java.io.InputStreamReader(System.in));
        System.out.print("Press return to continue...");
        in.readLine();
    }

    /**
     * Main method.
     *
     * @param args
     *            the command-line arguments
     */
    public static void main(final String[] args) {
        final boolean doPrompt = true; // true if you want a chance to start the
        // monitoring console
        try {
            int replicatorRepeats = 2;
            int runRepeats = 1;
            if (args.length > 0) {
                replicatorRepeats = Integer.parseInt(args[0]);
            }
            if (args.length > 1) {
                runRepeats = Integer.parseInt(args[1]);
            }
            final File testFile = new File("examples/fo/basic/readme.fo");

            log.info("MemoryEater! About to replicate the test file "
                    + replicatorRepeats + " times and run it " + runRepeats
                    + " times...");
            if (doPrompt) {
                prompt();
            }

            log.info("Processing...");
            final long start = System.currentTimeMillis();

            final MemoryEater app = new MemoryEater();
            app.eatMemory(testFile, runRepeats, replicatorRepeats);

            final long duration = System.currentTimeMillis() - start;
            log.info("Success! Job took " + duration + " ms");

            if (doPrompt) {
                prompt();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
