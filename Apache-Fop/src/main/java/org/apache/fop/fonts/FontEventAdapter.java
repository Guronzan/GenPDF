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

/* $Id: FontEventAdapter.java 1301445 2012-03-16 11:44:09Z mehdi $ */

package org.apache.fop.fonts;

import org.apache.fop.events.EventBroadcaster;

/**
 * Event listener interface for font-related events. This interface extends
 * FontEventListener and EventProducer for integration into FOP's event
 * subsystem.
 */
public class FontEventAdapter implements FontEventListener {

    private final EventBroadcaster eventBroadcaster;

    private FontEventProducer eventProducer;

    /**
     * Creates a new FontEventAdapter.
     * 
     * @param broadcaster
     *            the event broadcaster to send the generated events to
     */
    public FontEventAdapter(final EventBroadcaster broadcaster) {
        this.eventBroadcaster = broadcaster;
    }

    private FontEventProducer getEventProducer() {
        if (this.eventProducer == null) {
            this.eventProducer = FontEventProducer.Provider
                    .get(this.eventBroadcaster);
        }
        return this.eventProducer;
    }

    /** {@inheritDoc} */
    @Override
    public void fontSubstituted(final Object source,
            final FontTriplet requested, final FontTriplet effective) {
        getEventProducer().fontSubstituted(source, requested, effective);
    }

    /** {@inheritDoc} */
    @Override
    public void fontLoadingErrorAtAutoDetection(final Object source,
            final String fontURL, final Exception e) {
        getEventProducer().fontLoadingErrorAtAutoDetection(source, fontURL, e);
    }

    /** {@inheritDoc} */
    @Override
    public void glyphNotAvailable(final Object source, final char ch,
            final String fontName) {
        getEventProducer().glyphNotAvailable(source, ch, fontName);
    }

    /** {@inheritDoc} */
    @Override
    public void fontDirectoryNotFound(final Object source, final String dir) {
        getEventProducer().fontDirectoryNotFound(source, dir);
    }

    /** {@inheritDoc} */
    @Override
    public void svgTextStrokedAsShapes(final Object source,
            final String fontFamily) {
        getEventProducer().svgTextStrokedAsShapes(source, fontFamily);
    }

}
