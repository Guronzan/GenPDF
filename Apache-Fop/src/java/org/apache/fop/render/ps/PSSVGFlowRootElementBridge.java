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

/* $Id: PSSVGFlowRootElementBridge.java 766594 2009-04-20 06:50:59Z jeremias $ */

package org.apache.fop.render.ps;

import java.text.AttributedCharacterIterator;
import java.util.List;

import org.apache.batik.bridge.svg12.SVGFlowRootElementBridge;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.TextNode;
import org.apache.batik.gvt.TextPainter;
import org.apache.batik.gvt.flow.FlowTextPainter;
import org.apache.fop.fonts.FontInfo;

/**
 * Element Bridge for SVG 1.2 flow text, so those texts can be painted using PDF
 * primitives.
 */
public class PSSVGFlowRootElementBridge extends SVGFlowRootElementBridge {

    private final PSTextPainter textPainter;

    /**
     * Main Constructor.
     * 
     * @param fontInfo
     *            the font directory
     */
    public PSSVGFlowRootElementBridge(final FontInfo fontInfo) {
        this.textPainter = new PSFlowTextPainter(fontInfo);
    }

    /** {@inheritDoc} */
    @Override
    protected GraphicsNode instantiateGraphicsNode() {
        final GraphicsNode node = super.instantiateGraphicsNode();
        if (node != null) {
            // Set our own text painter
            ((TextNode) node).setTextPainter(getTextPainter());
        }
        return node;
    }

    /**
     * Returns the text painter used by this bridge.
     * 
     * @return the text painter
     */
    public TextPainter getTextPainter() {
        return this.textPainter;
    }

    private class PSFlowTextPainter extends PSTextPainter {

        /**
         * Main constructor
         * 
         * @param fontInfo
         *            the font directory
         */
        public PSFlowTextPainter(final FontInfo fontInfo) {
            super(fontInfo);
        }

        /** {@inheritDoc} */
        @Override
        public List getTextRuns(final TextNode node,
                final AttributedCharacterIterator aci) {
            // Text runs are delegated to the normal FlowTextPainter, we just
            // paint the text.
            final FlowTextPainter delegate = (FlowTextPainter) FlowTextPainter
                    .getInstance();
            return delegate.getTextRuns(node, aci);
        }

    }

}
