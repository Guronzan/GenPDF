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

/* $Id: PSTTFOutputStream.java 1352986 2012-06-22 18:07:04Z vhennebert $ */

package org.apache.fop.render.ps.fonts;

import java.io.IOException;

import org.apache.fop.fonts.truetype.TTFGlyphOutputStream;
import org.apache.fop.fonts.truetype.TTFOutputStream;
import org.apache.fop.fonts.truetype.TTFTableOutputStream;
import org.apache.xmlgraphics.ps.PSGenerator;

/**
 * Streams a TrueType font according to the PostScript format.
 */
public class PSTTFOutputStream implements TTFOutputStream {

    private final PSTTFGenerator ttfGen;

    /**
     * Creates a new instance wrapping the given generator.
     *
     * @param gen
     *            the generator to wrap
     */
    public PSTTFOutputStream(final PSGenerator gen) {
        this.ttfGen = new PSTTFGenerator(gen);
    }

    @Override
    public void startFontStream() throws IOException {
        this.ttfGen.write("/sfnts[");
    }

    @Override
    public TTFTableOutputStream getTableOutputStream() {
        return new PSTTFTableOutputStream(this.ttfGen);
    }

    @Override
    public TTFGlyphOutputStream getGlyphOutputStream() {
        return new PSTTFGlyphOutputStream(this.ttfGen);
    }

    @Override
    public void endFontStream() throws IOException {
        this.ttfGen.writeln("] def");
    }

}