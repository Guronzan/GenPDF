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

/* $Id: ExampleSVG2PDF.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package embedding;

//Java
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;

//Batik
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
//FOP
import org.apache.fop.svg.PDFTranscoder;

/**
 * This class demonstrates the conversion of an SVG file to PDF using FOP.
 */
@Slf4j
public class ExampleSVG2PDF {

    /**
     * Converts an FO file to a PDF file using FOP
     *
     * @param svg
     *            the SVG file
     * @param pdf
     *            the target PDF file
     * @throws IOException
     *             In case of an I/O problem
     * @throws TranscoderException
     *             In case of a transcoding problem
     */
    public void convertSVG2PDF(final File svg, final File pdf)
            throws IOException, TranscoderException {

        // Create transcoder
        final Transcoder transcoder = new PDFTranscoder();
        // Transcoder transcoder = new org.apache.fop.render.ps.PSTranscoder();

        // Setup input
        final InputStream in = new java.io.FileInputStream(svg);
        try {
            final TranscoderInput input = new TranscoderInput(in);

            // Setup output
            OutputStream out = new java.io.FileOutputStream(pdf);
            out = new java.io.BufferedOutputStream(out);
            try {
                final TranscoderOutput output = new TranscoderOutput(out);

                // Do the transformation
                transcoder.transcode(input, output);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    /**
     * Main method.
     *
     * @param args
     *            command-line arguments
     */
    public static void main(final String[] args) {
        try {
            log.info("FOP ExampleSVG2PDF\n");
            log.info("Preparing...");

            // Setup directories
            final File baseDir = new File(".");
            final File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output files
            final File svgfile = new File(baseDir, "xml/svg/helloworld.svg");
            final File pdffile = new File(outDir, "ResultSVG2PDF.pdf");

            log.info("Input: SVG (" + svgfile + ")");
            log.info("Output: PDF (" + pdffile + ")");
            log.info("");
            log.info("Transforming...");

            final ExampleSVG2PDF app = new ExampleSVG2PDF();
            app.convertSVG2PDF(svgfile, pdffile);

            log.info("Success!");
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
