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

/* $Id: IFMimickingTestCase.java 1178747 2011-10-04 10:09:01Z vhennebert $ */

package org.apache.fop.intermediate;

import java.io.File;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.fonts.FontEventProducer;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFSerializer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * This test checks the correct mimicking of a different output format.
 */
public class IFMimickingTestCase {

    private FopFactory fopFactory;

    @Before
    public void setUp() throws Exception {
        this.fopFactory = FopFactory.newInstance();
        final File configFile = new File("test/test-no-xml-metrics.xconf");
        this.fopFactory.setUserConfig(configFile);
    }

    /**
     * Tests IF document handler mimicking with PDF output.
     * 
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void testMimickingPDF() throws Exception {
        doTestMimicking(org.apache.xmlgraphics.util.MimeConstants.MIME_PDF);
    }

    /**
     * Tests IF document handler mimicking with PostScript output.
     * 
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void testMimickingPS() throws Exception {
        doTestMimicking(org.apache.xmlgraphics.util.MimeConstants.MIME_POSTSCRIPT);
    }

    /**
     * Tests IF document handler mimicking with TIFF output.
     * 
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void testMimickingTIFF() throws Exception {
        doTestMimicking(org.apache.xmlgraphics.util.MimeConstants.MIME_TIFF);
    }

    private void doTestMimicking(final String mime) throws FOPException,
            IFException, TransformerException {
        // Set up XMLRenderer to render to a DOM
        final DOMResult domResult = new DOMResult();

        final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();
        userAgent.getEventBroadcaster().addEventListener(new EventListener() {

            @Override
            public void processEvent(final Event event) {
                if (event.getEventGroupID().equals(
                        FontEventProducer.class.getName())) {
                    fail("There must be no font-related event! Got: "
                            + EventFormatter.format(event));
                }
            }

        });

        // Create an instance of the target renderer so the XMLRenderer can use
        // its font setup
        final IFDocumentHandler targetHandler = userAgent.getRendererFactory()
                .createDocumentHandler(userAgent, mime);

        // Setup painter
        final IFSerializer serializer = new IFSerializer();
        serializer.setContext(new IFContext(userAgent));
        serializer.mimicDocumentHandler(targetHandler);
        serializer.setResult(domResult);

        userAgent.setDocumentHandlerOverride(serializer);

        final Fop fop = this.fopFactory.newFop(userAgent);

        // minimal-pdf-a.fo uses the Gladiator font so is an ideal FO file for
        // this test:
        final StreamSource src = new StreamSource(new File(
                "test/xml/pdf-a/minimal-pdf-a.fo"));

        final TransformerFactory tFactory = TransformerFactory.newInstance();
        final Transformer transformer = tFactory.newTransformer();
        setErrorListener(transformer);

        transformer.transform(src, new SAXResult(fop.getDefaultHandler()));
    }

    /**
     * Sets an error listener which doesn't swallow errors like Xalan's default
     * one.
     * 
     * @param transformer
     *            the transformer to set the error listener on
     */
    protected void setErrorListener(final Transformer transformer) {
        transformer.setErrorListener(new ErrorListener() {

            @Override
            public void error(final TransformerException exception)
                    throws TransformerException {
                throw exception;
            }

            @Override
            public void fatalError(final TransformerException exception)
                    throws TransformerException {
                throw exception;
            }

            @Override
            public void warning(final TransformerException exception)
                    throws TransformerException {
                // ignore
            }

        });
    }

}
