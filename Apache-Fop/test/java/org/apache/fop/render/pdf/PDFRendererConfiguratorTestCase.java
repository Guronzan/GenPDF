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

/* $Id: PDFRendererConfiguratorTestCase.java 1178747 2011-10-04 10:09:01Z vhennebert $ */

package org.apache.fop.render.pdf;

import java.io.File;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventListener;
import org.apache.fop.pdf.PDFEncryptionParams;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests that encryption length is properly set up.
 */
public class PDFRendererConfiguratorTestCase {

    private FOUserAgent foUserAgent;

    private PDFDocumentHandler documentHandler;

    private boolean eventTriggered;

    private class EncryptionEventFilter implements EventListener {

        private final int specifiedEncryptionLength;

        private final int correctedEncryptionLength;

        EncryptionEventFilter(final int specifiedEncryptionLength,
                final int correctedEncryptionLength) {
            this.specifiedEncryptionLength = specifiedEncryptionLength;
            this.correctedEncryptionLength = correctedEncryptionLength;
        }

        @Override
        public void processEvent(final Event event) {
            assertEquals(PDFEventProducer.class.getName()
                    + ".incorrectEncryptionLength", event.getEventID());
            assertEquals(this.specifiedEncryptionLength,
                    event.getParam("originalValue"));
            assertEquals(this.correctedEncryptionLength,
                    event.getParam("correctedValue"));
            PDFRendererConfiguratorTestCase.this.eventTriggered = true;
        }
    }

    /**
     * Non-multiple of 8 should be rounded.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void testRoundUp() throws Exception {
        runTest("roundUp", 55, 56);
    }

    /**
     * Non-multiple of 8 should be rounded.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void testRoundDown() throws Exception {
        runTest("roundDown", 67, 64);
    }

    /**
     * Encryption length must be at least 40.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void testBelow40() throws Exception {
        runTest("below40", 32, 40);
    }

    /**
     * Encryption length must be at most 128.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void testAbove128() throws Exception {
        runTest("above128", 233, 128);
    }

    /**
     * A correct value must be properly set up.
     *
     * @throws Exception
     *             if an error occurs
     */
    @Test
    public void testCorrectValue() throws Exception {
        givenAConfigurationFile("correct", new EventListener() {

            @Override
            public void processEvent(final Event event) {
                fail("No event was expected");
            }
        });
        whenCreatingAndConfiguringDocumentHandler();
        thenEncryptionLengthShouldBe(128);

    }

    private void runTest(final String configFilename,
            final int specifiedEncryptionLength,
            final int correctedEncryptionLength) throws Exception {
        givenAConfigurationFile(configFilename, new EncryptionEventFilter(
                specifiedEncryptionLength, correctedEncryptionLength));
        whenCreatingAndConfiguringDocumentHandler();
        assertTrue(this.eventTriggered);
    }

    private void givenAConfigurationFile(final String filename,
            final EventListener eventListener) throws Exception {
        final FopFactory fopFactory = FopFactory.newInstance();
        fopFactory.setUserConfig(new File(
                "test/resources/org/apache/fop/render/pdf/" + filename
                        + ".xconf"));
        this.foUserAgent = fopFactory.newFOUserAgent();
        this.foUserAgent.getEventBroadcaster().addEventListener(eventListener);
    }

    private void whenCreatingAndConfiguringDocumentHandler()
            throws FOPException {
        final PDFDocumentHandlerMaker maker = new PDFDocumentHandlerMaker();
        this.documentHandler = (PDFDocumentHandler) maker
                .makeIFDocumentHandler(this.foUserAgent);
        new PDFRendererConfigurator(this.foUserAgent)
                .configure(this.documentHandler);
    }

    private void thenEncryptionLengthShouldBe(final int expectedEncryptionLength) {
        final PDFEncryptionParams encryptionParams = this.documentHandler
                .getPDFUtil().getEncryptionParams();
        assertEquals(expectedEncryptionLength,
                encryptionParams.getEncryptionLengthInBits());
    }
}
