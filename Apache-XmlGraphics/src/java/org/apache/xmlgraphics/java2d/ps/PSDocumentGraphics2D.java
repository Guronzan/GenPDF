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

/* $Id: PSDocumentGraphics2D.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.java2d.ps;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.xmlgraphics.ps.DSCConstants;

/**
 * This class is a wrapper for the <tt>PSGraphics2D</tt> that is used to create
 * a full document around the PostScript rendering from <tt>PSGraphics2D</tt>.
 *
 * @version $Id: PSDocumentGraphics2D.java 1345683 2012-06-03 14:50:33Z gadams $
 * @see org.apache.xmlgraphics.java2d.ps.PSGraphics2D
 *
 *      Originally authored by Keiron Liddle.
 */
public class PSDocumentGraphics2D extends AbstractPSDocumentGraphics2D {

    /**
     * Create a new AbstractPSDocumentGraphics2D. This is used to create a new
     * PostScript document, the height, width and output stream can be setup
     * later. For use by the transcoder which needs font information for the
     * bridge before the document size is known. The resulting document is
     * written to the stream after rendering.
     *
     * @param textAsShapes
     *            set this to true so that text will be rendered using curves
     *            and not the font.
     */
    public PSDocumentGraphics2D(final boolean textAsShapes) {
        super(textAsShapes);
    }

    /**
     * Create a new AbstractPSDocumentGraphics2D. This is used to create a new
     * PostScript document of the given height and width. The resulting document
     * is written to the stream after rendering.
     *
     * @param textAsShapes
     *            set this to true so that text will be rendered using curves
     *            and not the font.
     * @param stream
     *            the stream that the final document should be written to.
     * @param width
     *            the width of the document
     * @param height
     *            the height of the document
     * @throws IOException
     *             an io exception if there is a problem writing to the output
     *             stream
     */
    public PSDocumentGraphics2D(final boolean textAsShapes,
            final OutputStream stream, final int width, final int height)
            throws IOException {
        this(textAsShapes);
        setupDocument(stream, width, height);
    }

    /** {@inheritDoc} */
    @Override
    public void nextPage() throws IOException {
        closePage();
    }

    /** {@inheritDoc} */
    @Override
    protected void writeFileHeader() throws IOException {
        final Long pagewidth = new Long(this.width);
        final Long pageheight = new Long(this.height);

        // PostScript Header
        this.gen.writeln(DSCConstants.PS_ADOBE_30);
        this.gen.writeDSCComment(DSCConstants.CREATOR,
                new String[] { "Apache XML Graphics Commons"
                        + ": PostScript Generator for Java2D" });
        this.gen.writeDSCComment(DSCConstants.CREATION_DATE,
                new Object[] { new java.util.Date() });
        this.gen.writeDSCComment(DSCConstants.PAGES, DSCConstants.ATEND);
        this.gen.writeDSCComment(DSCConstants.BBOX, new Object[] { ZERO, ZERO,
                pagewidth, pageheight });
        this.gen.writeDSCComment(DSCConstants.LANGUAGE_LEVEL, new Integer(
                this.gen.getPSLevel()));
        this.gen.writeDSCComment(DSCConstants.END_COMMENTS);

        // Defaults
        this.gen.writeDSCComment(DSCConstants.BEGIN_DEFAULTS);
        this.gen.writeDSCComment(DSCConstants.END_DEFAULTS);

        // Prolog
        this.gen.writeDSCComment(DSCConstants.BEGIN_PROLOG);
        this.gen.writeDSCComment(DSCConstants.END_PROLOG);

        // Setup
        this.gen.writeDSCComment(DSCConstants.BEGIN_SETUP);
        writeProcSets();
        if (this.customTextHandler instanceof PSTextHandler) {
            ((PSTextHandler) this.customTextHandler).writeSetup();
        }
        this.gen.writeDSCComment(DSCConstants.END_SETUP);
    }

    /** {@inheritDoc} */
    @Override
    protected void writePageHeader() throws IOException {
        final Integer pageNumber = new Integer(this.pagecount);
        this.gen.writeDSCComment(DSCConstants.PAGE,
                new Object[] { pageNumber.toString(), pageNumber });
        this.gen.writeDSCComment(DSCConstants.PAGE_BBOX, new Object[] { ZERO,
                ZERO, new Integer(this.width), new Integer(this.height) });
        this.gen.writeDSCComment(DSCConstants.BEGIN_PAGE_SETUP);
        this.gen.writeln("<<");
        this.gen.writeln("/PageSize [" + this.width + " " + this.height + "]");
        this.gen.writeln("/ImagingBBox null");
        this.gen.writeln(">> setpagedevice");
        if (this.customTextHandler instanceof PSTextHandler) {
            ((PSTextHandler) this.customTextHandler).writePageSetup();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void writePageTrailer() throws IOException {
        this.gen.showPage();
    }

    /**
     * This constructor supports the create method
     * 
     * @param g
     *            the PostScript document graphics to make a copy of
     */
    public PSDocumentGraphics2D(final PSDocumentGraphics2D g) {
        super(g);
    }

}
