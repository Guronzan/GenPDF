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

/* $Id: PreloaderPlan.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.plan;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.util.DefaultErrorListener;
import org.apache.fop.util.UnclosableInputStream;
import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageContext;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.AbstractImagePreloader;
import org.apache.xmlgraphics.image.loader.impl.ImageXMLDOM;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Image preloader for Plan images.
 */
@Slf4j
public class PreloaderPlan extends AbstractImagePreloader {

    /** {@inheritDoc} */
    @Override
    public ImageInfo preloadImage(final String uri, final Source src,
            final ImageContext context) throws IOException {
        if (!ImageUtil.hasInputStream(src)) {
            // TODO Remove this and support DOMSource and possibly SAXSource
            return null;
        }
        final ImageInfo info = getImage(uri, src, context);
        if (info != null) {
            ImageUtil.closeQuietly(src); // Image is fully read
        }
        return info;
    }

    private ImageInfo getImage(final String uri, final Source src,
            final ImageContext context) throws IOException {

        final InputStream in = new UnclosableInputStream(
                ImageUtil.needInputStream(src));
        try {
            final Document planDoc = getDocument(in);
            final Element rootEl = planDoc.getDocumentElement();
            if (!PlanElementMapping.NAMESPACE.equals(rootEl.getNamespaceURI())) {
                in.reset();
                return null;
            }

            // Have to render the plan to know its size
            final PlanRenderer pr = new PlanRenderer();
            final Document svgDoc = pr.createSVGDocument(planDoc);
            final float width = pr.getWidth();
            final float height = pr.getHeight();

            // Return converted SVG image
            final ImageInfo info = new ImageInfo(uri, "image/svg+xml");
            final ImageSize size = new ImageSize();
            size.setSizeInMillipoints(Math.round(width * 1000),
                    Math.round(height * 1000));
            // Set the resolution to that of the FOUserAgent
            size.setResolution(context.getSourceResolution());
            size.calcPixelsFromSize();
            info.setSize(size);

            // The whole image had to be loaded for this, so keep it
            final Image image = new ImageXMLDOM(info, svgDoc, svgDoc
                    .getDocumentElement().getNamespaceURI());
            info.getCustomObjects().put(ImageInfo.ORIGINAL_IMAGE, image);

            return info;
        } catch (final TransformerException e) {
            try {
                in.reset();
            } catch (final IOException ioe) {
                // we're more interested in the original exception
            }
            log.debug("Error while trying to parsing a Plan file: "
                    + e.getMessage());
            return null;
        }
    }

    private Document getDocument(final InputStream in)
            throws TransformerException {
        final TransformerFactory tFactory = TransformerFactory.newInstance();
        // Custom error listener to minimize output to console
        final ErrorListener errorListener = new DefaultErrorListener(log);
        tFactory.setErrorListener(errorListener);
        final Transformer transformer = tFactory.newTransformer();
        transformer.setErrorListener(errorListener);
        final Source source = new StreamSource(in);
        final DOMResult res = new DOMResult();
        transformer.transform(source, res);

        final Document doc = (Document) res.getNode();
        return doc;
    }

}
