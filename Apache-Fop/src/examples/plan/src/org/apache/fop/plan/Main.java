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

/* $Id: Main.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.plan;

import java.io.InputStream;
import java.io.Writer;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Sample command-line application for converting plan XML to SVG.
 */
@Slf4j
public class Main {

    /**
     * Main method.
     *
     * @param args
     *            command-line arguments
     */
    public static void main(final String[] args) {
        final Main main = new Main();
        main.convert(args);
        System.exit(0);
    }

    /**
     * Runs the conversion
     *
     * @param params
     *            command-line arguments
     */
    public void convert(final String[] params) {
        if (params.length != 2) {
            log.info("arguments: plan.xml output.svg");
            return;
        }
        try {
            final InputStream is = new java.io.FileInputStream(params[0]);
            final Document doc = createSVGDocument(is);
            final SVGTranscoder svgT = new SVGTranscoder();
            final TranscoderInput input = new TranscoderInput(doc);
            final Writer ostream = new java.io.FileWriter(params[1]);
            final TranscoderOutput output = new TranscoderOutput(ostream);
            svgT.transcode(input, output);
            ostream.flush();
            ostream.close();

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to create the SVG document from the plan InputStream.
     *
     * @param is
     *            InputStream
     * @return Document a DOM containing the SVG
     */
    public Document createSVGDocument(final InputStream is) {
        Document doc = null;

        Element root = null;
        try {
            doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(is);

            root = doc.getDocumentElement();

        } catch (final Exception e) {
            e.printStackTrace();
        }
        final PlanRenderer gr = new PlanRenderer();
        gr.setFontInfo("sansserif", 12);
        final Document svgdoc = gr.createSVGDocument(doc);
        return svgdoc;
    }
}
