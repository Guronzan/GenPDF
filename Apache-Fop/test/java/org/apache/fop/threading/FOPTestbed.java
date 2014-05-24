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

/* $Id: FOPTestbed.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.threading;

import java.io.File;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.CascadingRuntimeException;
import org.apache.avalon.framework.activity.Executable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;

/**
 * Testbed for multi-threading tests. The class can run a configurable set of
 * task a number of times in a configurable number of threads to easily
 * reproduce multi-threading issues.
 */
@Slf4j
public class FOPTestbed extends AbstractLogEnabled implements Configurable,
        Initializable {

    private int repeat;
    private final List taskList = new java.util.ArrayList();
    private int threads;
    private File outputDir;
    private Configuration fopCfg;
    private Processor foprocessor;
    private boolean writeToDevNull;

    private int counter = 0;

    private final List results = Collections
            .synchronizedList(new java.util.LinkedList());

    /** {@inheritDoc} */
    @Override
    public void configure(final Configuration configuration)
            throws ConfigurationException {
        this.threads = configuration.getChild("threads").getValueAsInteger(10);
        this.outputDir = new File(configuration.getChild("output-dir")
                .getValue());
        this.writeToDevNull = configuration.getChild("devnull")
                .getValueAsBoolean(false);
        final Configuration tasks = configuration.getChild("tasks");
        this.repeat = tasks.getAttributeAsInteger("repeat", 1);
        final Configuration[] entries = tasks.getChildren("task");
        for (final Configuration entrie : entries) {
            this.taskList.add(new TaskDef(entrie));
        }
        this.fopCfg = configuration.getChild("processor");
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() throws Exception {
        this.foprocessor = createFOProcessor();
    }

    /**
     * Starts the stress test.
     */
    public void doStressTest() {
        getLogger().info("Starting stress test...");
        final long start = System.currentTimeMillis();
        this.counter = 0;

        // Initialize threads
        final ThreadGroup workerGroup = new ThreadGroup("FOP workers");
        final List threadList = new java.util.LinkedList();
        for (int ti = 0; ti < this.threads; ti++) {
            final TaskRunner runner = new TaskRunner();
            ContainerUtil.enableLogging(runner, getLogger());
            final Thread thread = new Thread(workerGroup, runner, "Worker- "
                    + ti);
            threadList.add(thread);
        }

        // Start threads
        final Iterator i = threadList.iterator();
        while (i.hasNext()) {
            ((Thread) i.next()).start();
        }

        // Wait for threads to end
        while (threadList.size() > 0) {
            final Thread t = (Thread) threadList.get(0);
            if (!t.isAlive()) {
                threadList.remove(0);
                continue;
            }
            try {
                Thread.sleep(100);
            } catch (final InterruptedException ie) {
                // ignore
            }
        }
        final long duration = System.currentTimeMillis() - start;

        report(duration);
    }

    private void report(final long duration) {
        final int count = this.results.size();
        int failures = 0;
        long bytesWritten = 0;
        log.info("Report on " + count + " tasks:");
        final Iterator iter = this.results.iterator();
        while (iter.hasNext()) {
            final Result res = (Result) iter.next();
            if (res.failure != null) {
                log.info("FAIL: " + (res.end - res.start) + " " + res.task);
                log.info("  -> " + res.failure.getMessage());
                failures++;
            } else {
                log.info("good: " + (res.end - res.start) + " " + res.filesize
                        + " " + res.task);
                bytesWritten += res.filesize;
            }
        }
        log.info("Stress test duration: " + duration + "ms");
        if (failures > 0) {
            log.info(failures + " failures of " + count + " documents!!!");
        } else {
            final float mb = 1024f * 1024f;
            log.info("Bytes written: " + bytesWritten / mb + " MB, "
                    + bytesWritten * 1000 / duration + " bytes / sec");
            log.info("NO failures with " + count + " documents.");
        }
    }

    private class TaskRunner extends AbstractLogEnabled implements Runnable {

        @Override
        public void run() {
            try {
                for (int r = 0; r < FOPTestbed.this.repeat; r++) {
                    final Iterator i = FOPTestbed.this.taskList.iterator();
                    while (i.hasNext()) {
                        final TaskDef def = (TaskDef) i.next();
                        final Task task = new Task(def,
                                FOPTestbed.this.counter++,
                                FOPTestbed.this.foprocessor);
                        ContainerUtil.enableLogging(task, getLogger());
                        task.execute();
                    }
                }
            } catch (final Exception e) {
                getLogger().error("Thread ended with an exception", e);
            }
        }

    }

    /**
     * Creates a new FOProcessor.
     *
     * @return the newly created instance
     */
    public Processor createFOProcessor() {
        try {
            final Class clazz = Class.forName(this.fopCfg.getAttribute("class",
                    "org.apache.fop.threading.FOProcessorImpl"));
            final Processor fop = (Processor) clazz.newInstance();
            ContainerUtil.enableLogging(fop, getLogger());
            ContainerUtil.configure(fop, this.fopCfg);
            ContainerUtil.initialize(fop);
            return fop;
        } catch (final Exception e) {
            throw new CascadingRuntimeException("Error creating FO Processor",
                    e);
        }
    }

    private class TaskDef {
        private final String fo;
        private String xml;
        private String xslt;
        private Templates templates;

        public TaskDef(final Configuration cfg) throws ConfigurationException {
            this.fo = cfg.getAttribute("fo", null);
            if (this.fo == null) {
                this.xml = cfg.getAttribute("xml");
                this.xslt = cfg.getAttribute("xslt", null);
                if (this.xslt != null) {
                    final TransformerFactory factory = TransformerFactory
                            .newInstance();
                    final Source xsltSource = new StreamSource(new File(
                            this.xslt));
                    try {
                        this.templates = factory.newTemplates(xsltSource);
                    } catch (final TransformerConfigurationException tce) {
                        throw new ConfigurationException("Invalid XSLT", tce);
                    }
                }
            }
        }

        public String getFO() {
            return this.fo;
        }

        public String getXML() {
            return this.xml;
        }

        public Templates getTemplates() {
            return this.templates;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            if (this.fo != null) {
                sb.append("fo=");
                sb.append(this.fo);
            } else {
                sb.append("xml=");
                sb.append(this.xml);
                sb.append(" xslt=");
                sb.append(this.xslt);
            }
            return sb.toString();
        }
    }

    private class Task extends AbstractLogEnabled implements Executable {

        private final TaskDef def;
        private final int num;
        private final Processor fop;

        public Task(final TaskDef def, final int num, final Processor fop) {
            this.def = def;
            this.num = num;
            this.fop = fop;
        }

        @Override
        public void execute() throws Exception {
            getLogger().info("Processing: " + this.def);
            final long start = System.currentTimeMillis();
            try {
                final DecimalFormat df = new DecimalFormat("00000");
                final File outfile = new File(FOPTestbed.this.outputDir,
                        df.format(this.num) + this.fop.getTargetFileExtension());
                OutputStream out;
                if (FOPTestbed.this.writeToDevNull) {
                    out = new NullOutputStream();
                } else {
                    out = new java.io.FileOutputStream(outfile);
                    out = new java.io.BufferedOutputStream(out);
                }
                final CountingOutputStream cout = new CountingOutputStream(out);
                try {
                    Source src;
                    Templates templates;

                    if (this.def.getFO() != null) {
                        src = new StreamSource(new File(this.def.getFO()));
                        templates = null;
                    } else {
                        src = new StreamSource(new File(this.def.getXML()));
                        templates = this.def.getTemplates();
                    }
                    this.fop.process(src, templates, cout);
                } finally {
                    IOUtils.closeQuietly(cout);
                }
                FOPTestbed.this.results.add(new Result(this.def, start, System
                        .currentTimeMillis(), cout.getByteCount()));
            } catch (final Exception e) {
                FOPTestbed.this.results.add(new Result(this.def, start, System
                        .currentTimeMillis(), e));
                throw e;
            }
        }
    }

    private static class Result {

        private final TaskDef task;
        private final long start;
        private final long end;
        private long filesize;
        private final Throwable failure;

        public Result(final TaskDef task, final long start, final long end,
                final long filesize) {
            this(task, start, end, null);
            this.filesize = filesize;
        }

        public Result(final TaskDef task, final long start, final long end,
                final Throwable failure) {
            this.task = task;
            this.start = start;
            this.end = end;
            this.failure = failure;
        }
    }

}
