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

/* $Id: Stats.java 985571 2010-08-14 19:28:26Z jeremias $ */

package org.apache.fop.memory;

import java.util.Iterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class Stats {

    private static final int INTERVAL = 2000;

    private final long startTime = System.currentTimeMillis();
    private long lastProgressDump = this.startTime;
    private int pagesProduced;

    private int totalPagesProduced;

    private int step;
    private int stepCount;

    private final List samples = new java.util.LinkedList();

    public void checkStats() {
        final long now = System.currentTimeMillis();
        if (now > this.lastProgressDump + INTERVAL) {
            dumpStats();
            reset();
        }
    }

    public void notifyPagesProduced(final int count) {
        this.pagesProduced += count;
        this.totalPagesProduced += count;
    }

    public void reset() {
        this.pagesProduced = 0;
        this.lastProgressDump = System.currentTimeMillis();
    }

    public void dumpStats() {
        final long duration = System.currentTimeMillis()
                - this.lastProgressDump;

        if (this.stepCount != 0) {
            final int progress = 100 * this.step / this.stepCount;
            log.info("Progress: " + progress + "%, "
                    + (this.stepCount - this.step) + " left");
        }

        final long ppm = 60000 * this.pagesProduced / duration;
        log.info("Speed: " + ppm + "ppm");
        this.samples.add(new Sample((int) ppm));
    }

    public void dumpFinalStats() {
        final long duration = System.currentTimeMillis() - this.startTime;
        log.info("Final statistics");
        log.info("Pages produced: " + this.totalPagesProduced);
        final long ppm = 60000 * this.totalPagesProduced / duration;
        log.info("Average speed: " + ppm + "ppm");
    }

    public String getGoogleChartURL() {
        final StringBuffer sb = new StringBuffer(
                "http://chart.apis.google.com/chart?");
        // http://chart.apis.google.com/chart?cht=ls&chd=t:60,40&chs=250x100&chl=Hello|World
        sb.append("cht=ls");
        sb.append("&chd=t:");
        boolean first = true;
        int maxY = 0;
        final Iterator iter = this.samples.iterator();
        while (iter.hasNext()) {
            final Sample sample = (Sample) iter.next();
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(sample.ppm);
            maxY = Math.max(maxY, sample.ppm);
        }
        final int ceilY = (maxY / 1000 + 1) * 1000;
        sb.append("&chs=1000x300"); // image size
        sb.append("&chds=0,").append(ceilY); // data scale
        sb.append("&chg=0,20"); // scale steps
        sb.append("&chxt=y");
        sb.append("&chxl=0:|0|" + ceilY);
        return sb.toString();
    }

    private static class Sample {

        private final int ppm;

        public Sample(final int ppm) {
            this.ppm = ppm;
        }
    }

    public void progress(final int step, final int stepCount) {
        this.step = step;
        this.stepCount = stepCount;

    }

}
