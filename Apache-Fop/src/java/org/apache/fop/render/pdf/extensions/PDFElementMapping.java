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

/* $Id: PDFElementMapping.java 1055034 2011-01-04 13:36:10Z spepping $ */

package org.apache.fop.render.pdf.extensions;

import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FONode;

/**
 * This class provides the element mapping for the PDF-specific extensions.
 */
public class PDFElementMapping extends ElementMapping {

    /** Namespace for the extension */
    public static final String NAMESPACE = "http://xmlgraphics.apache.org/fop/extensions/pdf";

    /** Main constructor */
    public PDFElementMapping() {
        this.namespaceURI = NAMESPACE;
    }

    /** {@inheritDoc} */
    @Override
    protected void initialize() {
        if (this.foObjs == null) {
            this.foObjs = new java.util.HashMap<String, Maker>();
            this.foObjs.put(PDFEmbeddedFileElement.ELEMENT,
                    new PDFEmbeddedFileMaker());
        }
    }

    static class PDFEmbeddedFileMaker extends ElementMapping.Maker {
        @Override
        public FONode make(final FONode parent) {
            return new PDFEmbeddedFileElement(parent);
        }
    }
}
