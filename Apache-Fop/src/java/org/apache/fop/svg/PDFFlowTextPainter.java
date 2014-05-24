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

/* $Id: PDFFlowTextPainter.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.svg;

import java.text.AttributedCharacterIterator;
import java.util.List;

import org.apache.batik.gvt.TextNode;
import org.apache.batik.gvt.flow.FlowTextPainter;
import org.apache.fop.fonts.FontInfo;

/**
 * Text Painter for SVG 1.2 (flow) text.
 */
public class PDFFlowTextPainter extends PDFTextPainter {

    /**
     * Main constructor
     * 
     * @param fontInfo
     *            the font directory
     */
    public PDFFlowTextPainter(final FontInfo fontInfo) {
        super(fontInfo);
    }

    /** {@inheritDoc} */
    @Override
    public List getTextRuns(final TextNode node,
            final AttributedCharacterIterator aci) {
        // Text runs are delegated to the normal FlowTextPainter, we just paint
        // the text.
        final FlowTextPainter delegate = (FlowTextPainter) FlowTextPainter
                .getInstance();
        return delegate.getTextRuns(node, aci);
    }

}
