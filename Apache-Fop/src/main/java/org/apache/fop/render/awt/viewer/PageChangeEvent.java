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

/* $Id: PageChangeEvent.java 1036179 2010-11-17 19:45:27Z spepping $ */

package org.apache.fop.render.awt.viewer;

import java.util.EventObject;

/**
 * Swing event fired whenever the current page selection of a
 * {@link PreviewPanel} changes. Page numbers are 0-based.
 */
public class PageChangeEvent extends EventObject {

    private static final long serialVersionUID = -5969283475959932887L;

    private final int oldPage;
    private final int newPage;

    /**
     * Creates an new page change event.
     * 
     * @param panel
     *            the preview panel the event is produced for.
     * @param oldPage
     *            the old page (zero based)
     * @param newPage
     *            the new page (zero based)
     */
    public PageChangeEvent(final PreviewPanel panel, final int oldPage,
            final int newPage) {
        super(panel);
        this.oldPage = oldPage;
        this.newPage = newPage;
    }

    /**
     * Returns the new page.
     * 
     * @return the new page (zero based)
     */
    public int getNewPage() {
        return this.newPage;
    }

    /**
     * Returns the old page.
     * 
     * @return the old page (zero based)
     */
    public int getOldPage() {
        return this.oldPage;
    }

}