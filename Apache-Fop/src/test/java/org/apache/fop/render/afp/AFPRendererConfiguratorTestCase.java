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

/* $Id$ */

package org.apache.fop.render.afp;

import java.io.IOException;

import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test case for {@link AFPRendererConfigurator}.
 */
public class AFPRendererConfiguratorTestCase {
    private static FOUserAgent userAgent;

    private AFPRendererConfigurator sut;

    /**
     * The FOUserAgent only needs to be created once.
     */
    @BeforeClass
    public static void createUserAgent() {
        userAgent = FopFactory.newInstance().newFOUserAgent();
    }

    /**
     * Assigns an FOUserAgen with a config file at <code>uri</code>
     *
     * @param uri
     *            the URI of the config file
     */
    private void setConfigFile(final String uri) {
        final String confTestsDir = "test/resources/conf/afp/";
        try {
            userAgent.getFactory().setUserConfig(confTestsDir + uri);
            this.sut = new AFPRendererConfigurator(userAgent);
        } catch (final IOException ioe) {
            fail("IOException: " + ioe);
        } catch (final SAXException se) {
            fail("SAXException: " + se);
        }
    }

    /**
     * Test several config files relating to JPEG images in AFP.
     *
     * @throws FOPException
     *             if an error is thrown
     */
    @Test
    public void testJpegImageConfig() throws FOPException {
        testJpegSettings("no_image_config.xconf", 1.0f, false);
        testJpegSettings("can_embed_jpeg.xconf", 1.0f, true);
        testJpegSettings("bitmap_encode_quality.xconf", 0.5f, false);
    }

    private void testJpegSettings(final String uri,
            final float bitmapEncodingQual, final boolean canEmbed)
            throws FOPException {
        final AFPDocumentHandler docHandler = new AFPDocumentHandler();

        setConfigFile(uri);
        this.sut.configure(docHandler);

        final AFPPaintingState paintingState = docHandler.getPaintingState();
        assertEquals(bitmapEncodingQual,
                paintingState.getBitmapEncodingQuality(), 0.01f);
        assertEquals(canEmbed, paintingState.canEmbedJpeg());
    }
}
