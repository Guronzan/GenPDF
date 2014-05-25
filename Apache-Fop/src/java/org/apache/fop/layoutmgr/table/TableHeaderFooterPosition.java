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

/* $Id: TableHeaderFooterPosition.java 635508 2008-03-10 10:06:37Z jeremias $ */

package org.apache.fop.layoutmgr.table;

import java.util.List;

import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.Position;

/**
 * This class represents a Position specific to TableContentLayoutManager. Used
 * for table headers and footers at the beginning and end of a table.
 */
class TableHeaderFooterPosition extends Position {

    /** True indicates a position for a header, false for a footer. */
    protected boolean header;
    /** Element list representing the header/footer */
    protected List nestedElements;

    /**
     * Creates a new TableHeaderFooterPosition.
     * 
     * @param lm
     *            applicable layout manager
     * @param header
     *            True indicates a position for a header, false for a footer.
     * @param nestedElements
     *            Element list representing the header/footer
     */
    protected TableHeaderFooterPosition(final LayoutManager lm,
            final boolean header, final List nestedElements) {
        super(lm);
        this.header = header;
        this.nestedElements = nestedElements;
    }

    /** {@inheritDoc} */
    @Override
    public boolean generatesAreas() {
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Table");
        sb.append(this.header ? "Header" : "Footer");
        sb.append("Position:");
        sb.append(getIndex()).append("(");
        sb.append(this.nestedElements);
        sb.append(")");
        return sb.toString();
    }
}
