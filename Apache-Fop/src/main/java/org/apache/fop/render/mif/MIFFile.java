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

/* $Id: MIFFile.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.render.mif;

// Java
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

/**
 * The MIF File. This organises the MIF File and the corresponding elements. The
 * catalog elements are used to setup the resources that are referenced.
 */
public class MIFFile extends MIFElement {

    /** colorCatalog */
    protected MIFElement colorCatalog = null;
    /** pgfCatalog */
    protected PGFElement pgfCatalog = null;
    /** fontCatalog */
    protected MIFElement fontCatalog = null;
    /** rulingCatalog */
    protected RulingElement rulingCatalog = null;
    /** tblCatalog */
    protected MIFElement tblCatalog = null;
    /** views */
    protected MIFElement views = null;
    /** variableFormats */
    protected MIFElement variableFormats = null;
    /** xRefFormats */
    protected MIFElement xRefFormats = null;
    /** document */
    protected MIFElement document = null;
    /** bookComponent */
    protected MIFElement bookComponent = null;
    /** initialAutoNums */
    protected MIFElement initialAutoNums = null;
    /** aFrames */
    protected MIFElement aFrames = null;
    /** tbls */
    protected MIFElement tbls = null;
    /** pages */
    protected List pages = new java.util.ArrayList();
    /** textFlows */
    protected List textFlows = null;

    /** default constructor */
    public MIFFile() {
        super("");
        this.valueElements = new java.util.ArrayList();
        setup();
    }

    /**
     * Do some setup. Currently adds some dummy values to the resources.
     */
    protected void setup() {
        final MIFElement unit = new MIFElement("Units");
        unit.setValue("Ucm");
        addElement(unit);

        this.colorCatalog = new MIFElement("ColorCatalog");
        final MIFElement color = new MIFElement("Color");
        MIFElement prop = new MIFElement("ColorTag");
        prop.setValue("`Black'");
        color.addElement(prop);
        prop = new MIFElement("ColorCyan");
        prop.setValue("0.000000");
        color.addElement(prop);

        prop = new MIFElement("ColorMagenta");
        prop.setValue("0.000000");
        color.addElement(prop);
        prop = new MIFElement("ColorYellow");
        prop.setValue("0.000000");
        color.addElement(prop);
        prop = new MIFElement("ColorBlack");
        prop.setValue("100.000000");
        color.addElement(prop);
        prop = new MIFElement("ColorAttribute");
        prop.setValue("ColorIsBlack");
        color.addElement(prop);
        prop = new MIFElement("ColorAttribute");
        prop.setValue("ColorIsReserved");
        color.addElement(prop);
        color.finish(true);

        this.colorCatalog.addElement(color);
        addElement(this.colorCatalog);

        this.pgfCatalog = new PGFElement();
        this.pgfCatalog.lookupElement(null);
        addElement(this.pgfCatalog);

        this.rulingCatalog = new RulingElement();
        this.rulingCatalog.lookupElement(null);
        addElement(this.rulingCatalog);

    }

    /**
     * @param os
     *            output stream
     * @throws IOException
     *             if not caught
     */
    public void output(final OutputStream os) throws IOException {
        if (this.finished) {
            return;
        }

        if (!this.started) {
            os.write("<MIFFile  5.00> # Generated by FOP\n".getBytes());
            this.started = true;
        }
        boolean done = true;

        for (final Iterator iter = this.valueElements.iterator(); iter
                .hasNext();) {
            final MIFElement el = (MIFElement) iter.next();
            final boolean d = el.output(os, 0);
            if (d) {
                iter.remove();
            } else {
                done = false;
                break;
            }
        }
        if (done && this.finish) {
            os.write("# end of MIFFile".getBytes());
        }
    }

    /**
     * @param p
     *            a page element to add
     */
    public void addPage(final MIFElement p) {
        this.pages.add(p);
        addElement(p);
    }
}
