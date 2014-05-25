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

/* $Id: PlanElementMapping.java 1055034 2011-01-04 13:36:10Z spepping $ */

package org.apache.fop.plan;

import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FONode;
import org.w3c.dom.DOMImplementation;

/**
 * This class provides the element mapping for FOP.
 */
public class PlanElementMapping extends ElementMapping {

    /** Plan Namespace */
    public static final String NAMESPACE = "http://xmlgraphics.apache.org/fop/plan";

    /** Main constructor. */
    public PlanElementMapping() {
        this.namespaceURI = NAMESPACE;
    }

    /** {@inheritDoc} */
    @Override
    public DOMImplementation getDOMImplementation() {
        return getDefaultDOMImplementation();
    }

    /** {@inheritDoc} */
    @Override
    protected void initialize() {
        if (this.foObjs == null) {
            this.foObjs = new java.util.HashMap<String, Maker>();
            this.foObjs.put("plan", new PE());
            this.foObjs.put(DEFAULT, new PlanMaker());
        }
    }

    static class PlanMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new PlanObj(parent);
        }
    }

    static class PE extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new PlanElement(parent);
        }
    }

}
