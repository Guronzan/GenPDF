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

/* $Id: PreloaderWMF.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.image.loader.batik;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Source;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.transcoder.wmf.WMFConstants;
import org.apache.batik.transcoder.wmf.tosvg.WMFRecordStore;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;
import org.apache.fop.util.UnclosableInputStream;
import org.apache.xmlgraphics.image.loader.ImageContext;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.AbstractImagePreloader;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;

/**
 * Image preloader for WMF images (Windows Metafile).
 */
@Slf4j
public class PreloaderWMF extends AbstractImagePreloader {

    private boolean batikAvailable = true;

    /** {@inheritDoc} */
    @Override
    public ImageInfo preloadImage(final String uri, final Source src,
            final ImageContext context) throws IOException {
        if (!ImageUtil.hasInputStream(src)) {
            return null;
        }
        ImageInfo info = null;
        if (this.batikAvailable) {
            try {
                final Loader loader = new Loader();
                info = loader.getImage(uri, src, context);
            } catch (final NoClassDefFoundError e) {
                this.batikAvailable = false;
                log.warn("Batik not in class path", e);
                return null;
            }
        }
        if (info != null) {
            ImageUtil.closeQuietly(src); // Image is fully read
        }
        return info;
    }

    /**
     * This method is put in another class so that the class loader does not
     * attempt to load Batik related classes when constructing the WMFPreloader
     * class.
     */
    private final class Loader {

        private Loader() {
        }

        private ImageInfo getImage(final String uri, final Source src,
                final ImageContext context) {
            // parse document and get the size attributes of the svg element

            final InputStream in = new UnclosableInputStream(
                    ImageUtil.needInputStream(src));
            try {
                in.mark(4 + 1);

                final DataInputStream din = new DataInputStream(in);
                final int magic = EndianUtils.swapInteger(din.readInt());
                din.reset();
                if (magic != WMFConstants.META_ALDUS_APM) {
                    return null; // Not a WMF file
                }

                final WMFRecordStore wmfStore = new WMFRecordStore();
                wmfStore.read(din);
                IOUtils.closeQuietly(din);

                final int width = wmfStore.getWidthUnits();
                final int height = wmfStore.getHeightUnits();
                final int dpi = wmfStore.getMetaFileUnitsPerInch();

                final ImageInfo info = new ImageInfo(uri, "image/x-wmf");
                final ImageSize size = new ImageSize();
                size.setSizeInPixels(width, height);
                size.setResolution(dpi);
                size.calcSizeFromPixels();
                info.setSize(size);
                final ImageWMF img = new ImageWMF(info, wmfStore);
                info.getCustomObjects().put(ImageInfo.ORIGINAL_IMAGE, img);

                return info;
            } catch (final NoClassDefFoundError ncdfe) {
                try {
                    in.reset();
                } catch (final IOException ioe) {
                    // we're more interested in the original exception
                }
                PreloaderWMF.this.batikAvailable = false;
                log.warn("Batik not in class path", ncdfe);
                return null;
            } catch (final IOException e) {
                // If the svg is invalid then it throws an IOException
                // so there is no way of knowing if it is an svg document

                log.debug("Error while trying to load stream as an WMF file: "
                        + e.getMessage());
                // assuming any exception means this document is not svg
                // or could not be loaded for some reason
                try {
                    in.reset();
                } catch (final IOException ioe) {
                    // we're more interested in the original exception
                }
                return null;
            }
        }
    }

}
