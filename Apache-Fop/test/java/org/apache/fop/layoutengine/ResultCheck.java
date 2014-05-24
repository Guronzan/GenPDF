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

/* $Id: ResultCheck.java 1178747 2011-10-04 10:09:01Z vhennebert $ */

package org.apache.fop.layoutengine;

import org.apache.fop.apps.FormattingResults;
import org.w3c.dom.Node;

/**
 * Simple check that requires a result property to evaluate to the expected
 * value
 */
public class ResultCheck implements LayoutEngineCheck {

    private final String expected;
    private final String property;

    /**
     * Creates a new instance from a DOM node.
     * 
     * @param node
     *            DOM node that defines this check
     */
    public ResultCheck(final Node node) {
        this.expected = node.getAttributes().getNamedItem("expected")
                .getNodeValue();
        this.property = node.getAttributes().getNamedItem("property")
                .getNodeValue();
    }

    /** {@inheritDoc} */
    @Override
    public void check(final LayoutResult result) {
        final FormattingResults results = result.getResults();
        String actual;
        if (this.property.equals("pagecount")) {
            actual = Integer.toString(results.getPageCount());
        } else {
            throw new RuntimeException("No such property test: "
                    + this.property);
        }
        if (!this.expected.equals(actual)) {
            throw new RuntimeException("Expected property to evaluate to '"
                    + this.expected + "', but got '" + actual + "' (" + this
                    + ")");
        }

    }

    @Override
    public String toString() {
        return "Property: " + this.property;
    }

}
