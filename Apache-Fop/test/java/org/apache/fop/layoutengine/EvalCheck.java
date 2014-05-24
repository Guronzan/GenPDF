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

/* $Id: EvalCheck.java 1094690 2011-04-18 18:36:05Z vhennebert $ */

package org.apache.fop.layoutengine;

import javax.xml.transform.TransformerException;

import org.apache.fop.intermediate.IFCheck;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.PrefixResolverDefault;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Simple check that requires an XPath expression to evaluate to true.
 */
public class EvalCheck implements LayoutEngineCheck, IFCheck {

    private final String expected;
    private final String xpath;
    private double tolerance;
    private final PrefixResolver prefixResolver;

    /**
     * Creates a new instance from a DOM node.
     * 
     * @param node
     *            DOM node that defines this check
     */
    public EvalCheck(final Node node) {
        this.expected = node.getAttributes().getNamedItem("expected")
                .getNodeValue();
        this.xpath = node.getAttributes().getNamedItem("xpath").getNodeValue();
        final Node nd = node.getAttributes().getNamedItem("tolerance");
        if (nd != null) {
            this.tolerance = Double.parseDouble(nd.getNodeValue());
        }
        this.prefixResolver = new PrefixResolverDefault(node);
    }

    /** {@inheritDoc} */
    @Override
    public void check(final LayoutResult result) {
        doCheck(result.getAreaTree());
    }

    /** {@inheritDoc} */
    @Override
    public void check(final Document intermediate) {
        doCheck(intermediate);
    }

    private void doCheck(final Document doc) {
        XObject res;
        try {
            res = XPathAPI.eval(doc, this.xpath, this.prefixResolver);
        } catch (final TransformerException e) {
            throw new RuntimeException("XPath evaluation failed: "
                    + e.getMessage());
        }
        final String actual = res.str(); // Second str() seems to fail. D'oh!
        if (this.tolerance != 0) {
            final double v1 = Double.parseDouble(this.expected);
            final double v2 = Double.parseDouble(actual);
            if (Math.abs(v1 - v2) > this.tolerance) {
                throw new RuntimeException(
                        "Expected XPath expression to evaluate to '"
                                + this.expected + "', but got '" + actual
                                + "' (" + this + ", outside tolerance)");
            }
        } else {
            if (!this.expected.equals(actual)) {
                throw new RuntimeException(
                        "Expected XPath expression to evaluate to '"
                                + this.expected + "', but got '" + actual
                                + "' (" + this + ")");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "XPath: " + this.xpath;
    }

}
