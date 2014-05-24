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

/* $Id: AvalonAdapter.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.threading;

import org.apache.avalon.framework.logger.Logger;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;

/**
 * Redirects events to an Avalon logger.
 */
class AvalonAdapter implements EventListener {

    private final Logger logger;
    private final String filename;

    public AvalonAdapter(final Logger logger, final String filename) {
        this.logger = logger;
        this.filename = filename;
    }

    @Override
    public void processEvent(final Event event) {
        final String msg = EventFormatter.format(event);
        final EventSeverity severity = event.getSeverity();
        if (severity == EventSeverity.INFO) {
            // logger.info(filename + ": " + msg);
        } else if (severity == EventSeverity.WARN) {
            // logger.warn(filename + ": " + msg);
        } else if (severity == EventSeverity.ERROR) {
            this.logger.error(this.filename + ": " + msg);
        } else if (severity == EventSeverity.FATAL) {
            this.logger.fatalError(this.filename + ": " + msg);
        } else {
            assert false;
        }
    }

}
