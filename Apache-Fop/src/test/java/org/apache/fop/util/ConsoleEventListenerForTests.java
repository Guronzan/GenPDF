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

/* $Id: ConsoleEventListenerForTests.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.util;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;

@Slf4j
/** A simple event listener that writes the events to stdout and sterr. */
public class ConsoleEventListenerForTests implements EventListener {

    private String name;
    private final EventSeverity logLevel;

    /**
     * Creates a new event listener with console output on severity INFO. This
     * object will write out the name of the test before the first log message.
     *
     * @param name
     *            the name of the test
     */
    public ConsoleEventListenerForTests(final String name) {
        this(name, EventSeverity.INFO);
    }

    /**
     * Creates a new event listener with console output. This object will write
     * out the name of the test before the first log message.
     *
     * @param name
     *            the name of the test
     * @param logLevel
     *            the logging level
     */
    public ConsoleEventListenerForTests(final String name,
            final EventSeverity logLevel) {
        this.name = name;
        this.logLevel = logLevel;
    }

    /** {@inheritDoc} */
    @Override
    public void processEvent(final Event event) {
        final EventSeverity severity = event.getSeverity();
        if (severity == EventSeverity.FATAL) {
            log("FATAL", event);
            return;
        }
        if (this.logLevel == EventSeverity.FATAL) {
            return;
        }
        if (severity == EventSeverity.ERROR) {
            log("ERROR", event);
            return;
        }
        if (this.logLevel == EventSeverity.ERROR) {
            return;
        }
        if (severity == EventSeverity.WARN) {
            log("WARN ", event);
        }
        if (this.logLevel == EventSeverity.WARN) {
            return;
        }
        if (severity == EventSeverity.INFO) {
            log("INFO ", event);
        }
    }

    private void log(final String levelString, final Event event) {
        if (this.name != null) {
            log.info("Test: " + this.name);
            this.name = null;
        }
        final String msg = EventFormatter.format(event);
        log.info("  [" + levelString + "] " + msg);
    }
}
