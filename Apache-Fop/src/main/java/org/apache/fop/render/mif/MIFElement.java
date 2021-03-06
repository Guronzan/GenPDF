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

/* $Id: MIFElement.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.render.mif;

// Java
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

/**
 * The is the basis for MIF document elements. This enables the creation of the
 * element and to write it to an output stream including sub-elements or a
 * single value.
 */
public class MIFElement {
    /** name */
    protected String name;
    /** value string */
    protected String valueStr = null;
    /** value elements */
    protected List valueElements = null;
    /** true if started */
    protected boolean started = false;
    /** true if finishing */
    protected boolean finish = false;
    /** true if finished */
    protected boolean finished = false;

    /**
     * @param name
     *            a name
     */
    public MIFElement(final String name) {
        this.name = name;
    }

    /**
     * @param str
     *            a string value
     */
    public void setValue(final String str) {
        this.valueStr = str;
    }

    /**
     * @param el
     *            an MIF element
     */
    public void addElement(final MIFElement el) {
        if (this.valueElements == null) {
            this.valueElements = new java.util.ArrayList();
        }
        this.valueElements.add(el);
    }

    /**
     * Output this element to an output stream. This will output only so far as
     * the fisrt unfinished child element. This method can be called again to
     * continue from the previous point. An element that contains child elements
     * will only be finished when the finish method is called.
     * 
     * @param os
     *            output stream
     * @param indent
     *            indentation
     * @return true if finished
     * @throws IOException
     *             if not caught
     */
    public boolean output(final OutputStream os, final int indent)
            throws IOException {
        if (this.finished) {
            return true;
        }
        if (this.valueElements == null && this.valueStr == null) {
            return false;
        }

        String indentStr = "";
        for (int c = 0; c < indent; c++) {
            indentStr += " ";
        }
        if (!this.started) {
            os.write((indentStr + "<" + this.name).getBytes());
            if (this.valueElements != null) {
                os.write("\n".getBytes());
            }
            this.started = true;
        }
        if (this.valueElements != null) {
            boolean done = true;
            for (final Iterator iter = this.valueElements.iterator(); iter
                    .hasNext();) {
                final MIFElement el = (MIFElement) iter.next();
                final boolean d = el.output(os, indent + 1);
                if (d) {
                    iter.remove();
                } else {
                    done = false;
                    break;
                }
            }
            if (!this.finish || !done) {
                return false;
            }
            os.write((indentStr + "> # end of " + this.name + "\n").getBytes());
        } else {
            os.write((" " + this.valueStr + ">\n").getBytes());
        }
        this.finished = true;
        return true;
    }

    /**
     * @param deep
     *            if true, also perform finish over value elements
     */
    public void finish(final boolean deep) {
        this.finish = true;
        if (deep && this.valueElements != null) {
            for (final Iterator iter = this.valueElements.iterator(); iter
                    .hasNext();) {
                final MIFElement el = (MIFElement) iter.next();
                el.finish(deep);
            }
        }
    }
}
