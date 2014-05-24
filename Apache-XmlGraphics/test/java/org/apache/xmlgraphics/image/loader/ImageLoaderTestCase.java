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

/* $Id: ImageLoaderTestCase.java 1353184 2012-06-23 19:18:01Z gadams $ */

package org.apache.xmlgraphics.image.loader;

import java.awt.color.ICC_Profile;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.stream.ImageInputStream;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.xmlgraphics.image.loader.impl.ImageLoaderPNG;
import org.apache.xmlgraphics.image.loader.impl.ImageLoaderRawPNG;
import org.apache.xmlgraphics.image.loader.impl.ImageRawStream;
import org.apache.xmlgraphics.image.loader.impl.ImageRendered;
import org.apache.xmlgraphics.image.loader.spi.ImageImplRegistry;
import org.apache.xmlgraphics.image.loader.spi.ImageLoader;
import org.apache.xmlgraphics.image.loader.spi.ImageLoaderFactory;

/**
 * Tests for bundled ImageLoader implementations.
 */
public class ImageLoaderTestCase extends TestCase {

    private final MockImageContext imageContext = MockImageContext
            .getInstance();

    public ImageLoaderTestCase(final String name) {
        super(name);
    }

    private MyImageSessionContext createImageSessionContext() {
        return new MyImageSessionContext(this.imageContext);
    }

    public void testPNG() throws Exception {
        final String uri = "asf-logo.png";

        final MyImageSessionContext sessionContext = createImageSessionContext();
        final ImageManager manager = this.imageContext.getImageManager();

        ImageInfo info = manager.preloadImage(uri, sessionContext);
        assertNotNull("ImageInfo must not be null", info);

        final Image img = manager.getImage(info, ImageFlavor.RENDERED_IMAGE,
                sessionContext);
        assertNotNull("Image must not be null", img);
        assertEquals(ImageFlavor.RENDERED_IMAGE, img.getFlavor());
        final ImageRendered imgRed = (ImageRendered) img;
        assertNotNull(imgRed.getRenderedImage());
        assertEquals(169, imgRed.getRenderedImage().getWidth());
        assertEquals(51, imgRed.getRenderedImage().getHeight());
        info = imgRed.getInfo(); // Switch to the ImageInfo returned by the
                                 // image
        assertEquals(126734, info.getSize().getWidthMpt());
        assertEquals(38245, info.getSize().getHeightMpt());

        sessionContext.checkAllStreamsClosed();
    }

    public void testGIF() throws Exception {
        final String uri = "bgimg72dpi.gif";

        final MyImageSessionContext sessionContext = createImageSessionContext();
        final ImageManager manager = this.imageContext.getImageManager();

        ImageInfo info = manager.preloadImage(uri, sessionContext);
        assertNotNull("ImageInfo must not be null", info);

        final Image img = manager.getImage(info, ImageFlavor.RENDERED_IMAGE,
                sessionContext);
        assertNotNull("Image must not be null", img);
        assertEquals(ImageFlavor.RENDERED_IMAGE, img.getFlavor());
        final ImageRendered imgRed = (ImageRendered) img;
        assertNotNull(imgRed.getRenderedImage());
        assertEquals(192, imgRed.getRenderedImage().getWidth());
        assertEquals(192, imgRed.getRenderedImage().getHeight());
        info = imgRed.getInfo(); // Switch to the ImageInfo returned by the
                                 // image
        assertEquals(192000, info.getSize().getWidthMpt());
        assertEquals(192000, info.getSize().getHeightMpt());

        sessionContext.checkAllStreamsClosed();
    }

    public void testEPSASCII() throws Exception {
        final String uri = "barcode.eps";

        final MyImageSessionContext sessionContext = createImageSessionContext();
        final ImageManager manager = this.imageContext.getImageManager();

        final ImageInfo info = manager.preloadImage(uri, sessionContext);
        assertNotNull("ImageInfo must not be null", info);

        final Image img = manager.getImage(info, ImageFlavor.RAW_EPS,
                sessionContext);
        assertNotNull("Image must not be null", img);
        assertEquals(ImageFlavor.RAW_EPS, img.getFlavor());
        final ImageRawStream imgEPS = (ImageRawStream) img;
        final InputStream in = imgEPS.createInputStream();
        try {
            assertNotNull(in);
            final Reader reader = new InputStreamReader(in, "US-ASCII");
            final char[] c = new char[4];
            reader.read(c);
            if (!"%!PS".equals(new String(c))) {
                fail("EPS header expected");
            }
        } finally {
            IOUtils.closeQuietly(in);
        }

        sessionContext.checkAllStreamsClosed();
    }

    public void testEPSBinary() throws Exception {
        final String uri = "img-with-tiff-preview.eps";

        final MyImageSessionContext sessionContext = createImageSessionContext();
        final ImageManager manager = this.imageContext.getImageManager();

        final ImageInfo info = manager.preloadImage(uri, sessionContext);
        assertNotNull("ImageInfo must not be null", info);

        final Image img = manager.getImage(info, ImageFlavor.RAW_EPS,
                sessionContext);
        assertNotNull("Image must not be null", img);
        assertEquals(ImageFlavor.RAW_EPS, img.getFlavor());
        final ImageRawStream imgEPS = (ImageRawStream) img;
        final InputStream in = imgEPS.createInputStream();
        try {
            assertNotNull(in);
            final Reader reader = new InputStreamReader(in, "US-ASCII");
            final char[] c = new char[4];
            reader.read(c);
            if (!"%!PS".equals(new String(c))) {
                fail("EPS header expected");
            }
        } finally {
            IOUtils.closeQuietly(in);
        }

        sessionContext.checkAllStreamsClosed();
    }

    public void testICCProfiles() throws Exception {
        final MyImageSessionContext sessionContext = createImageSessionContext();
        final List/* <ICC_Profile> */profiles = new ArrayList();

        runReaders(profiles, sessionContext, "iccTest.png", "image/png",
                ImageFlavor.RAW_PNG);
        runReaders(profiles, sessionContext, "iccTest.jpg", "image/jpeg",
                ImageFlavor.RAW_JPEG);

        final ICC_Profile first = (ICC_Profile) profiles.get(0);
        final byte[] firstData = first.getData();
        for (int i = 1; i < profiles.size(); i++) {
            final ICC_Profile icc = (ICC_Profile) profiles.get(i);
            final byte[] data = icc.getData();
            assertEquals("Embedded ICC Profiles are not the same size!",
                    firstData.length, data.length);
            for (int j = 0; j < firstData.length; j++) {
                assertEquals("Embedded ICC Profiles differ at index " + j,
                        firstData[j], data[j]);
            }
        }
    }

    private void runReaders(final List profiles, final ImageSessionContext isc,
            final String uri, final String mime, final ImageFlavor rawFlavor)
            throws Exception {
        final ImageLoaderFactory ilfs[] = ImageImplRegistry
                .getDefaultInstance().getImageLoaderFactories(mime);
        if (ilfs != null) {
            for (int i = 0; i < ilfs.length; i++) {
                final ImageLoaderFactory ilf = ilfs[i];
                try {
                    final ImageLoader il = ilf.newImageLoader(rawFlavor);
                    if (il instanceof ImageLoaderRawPNG
                            || il instanceof ImageLoaderPNG) {
                        // temporary measure until ImageLoaderRawPNG and
                        // ImageLoader PNG handle ICC profiles
                        continue;
                    }
                    final ImageInfo im = new ImageInfo(uri, mime);
                    final Image img = il.loadImage(im, isc);
                    final ICC_Profile icc = img.getICCProfile();
                    // Assume the profile can only be correct if the image could
                    // actually be interpreted.
                    if (img.getColorSpace() != null) {
                        profiles.add(icc);
                    }
                } catch (final IllegalArgumentException e) {
                    // Ignore. This imageLoader does not support RAW
                }
                try {
                    final ImageLoader il = ilf
                            .newImageLoader(ImageFlavor.BUFFERED_IMAGE);
                    final ImageInfo im = new ImageInfo(uri, mime);
                    final Image img = il.loadImage(im, isc);
                    final ICC_Profile icc = img.getICCProfile();
                    profiles.add(icc);
                } catch (final IllegalArgumentException e) {
                    // Ignore. This imageLoader does not support Buffered.
                }
            }
        }
    }

    public void testBrokenIccPng() throws Exception {
        final String uri = "corrupt-icc.png";

        final MyImageSessionContext sessionContext = createImageSessionContext();
        final ImageManager manager = this.imageContext.getImageManager();

        final ImageInfo info = manager.preloadImage(uri, sessionContext);
        assertNotNull("ImageInfo must not be null", info);

        final Image img = manager.getImage(info, ImageFlavor.RENDERED_IMAGE,
                sessionContext);
        assertNotNull("Image must not be null", img);
        assertEquals(ImageFlavor.RENDERED_IMAGE, img.getFlavor());
        final ImageRendered imgRed = (ImageRendered) img;
        assertNotNull(imgRed.getRenderedImage());
        assertEquals(400, imgRed.getRenderedImage().getWidth());
        assertEquals(300, imgRed.getRenderedImage().getHeight());

        sessionContext.checkAllStreamsClosed();
    }

    private static class MyImageSessionContext extends MockImageSessionContext {

        private final List streams = new java.util.ArrayList();

        public MyImageSessionContext(final ImageContext context) {
            super(context);
        }

        @Override
        public Source newSource(final String uri) {
            final Source src = super.newSource(uri);
            if (src instanceof ImageSource) {
                final ImageSource is = (ImageSource) src;
                ImageInputStream in = is.getImageInputStream();
                // in = new ObservableImageInputStream(in, is.getSystemId());
                in = ObservableStream.Factory.observe(in, is.getSystemId());
                this.streams.add(in);
                is.setImageInputStream(in);
            }
            return src;
        }

        /** {@inheritDoc} */
        @Override
        protected Source resolveURI(final String uri) {
            final Source src = super.resolveURI(uri);
            if (src instanceof StreamSource) {
                final StreamSource ss = (StreamSource) src;
                if (ss.getInputStream() != null) {
                    final InputStream in = new ObservableInputStream(
                            ss.getInputStream(), ss.getSystemId());
                    this.streams.add(in);
                    ss.setInputStream(in);
                }
            }
            return src;
        }

        public void checkAllStreamsClosed() {
            final Iterator iter = this.streams.iterator();
            while (iter.hasNext()) {
                final ObservableStream stream = (ObservableStream) iter.next();
                iter.remove();
                if (!stream.isClosed()) {
                    fail(stream.getClass().getName() + " is NOT closed: "
                            + stream.getSystemID());
                }
            }
        }

    }

}
