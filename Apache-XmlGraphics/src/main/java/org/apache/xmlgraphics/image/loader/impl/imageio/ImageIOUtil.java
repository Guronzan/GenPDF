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

/* $Id: ImageIOUtil.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.image.loader.impl.imageio;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.util.UnitConv;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Helper and convenience methods for ImageIO.
 */
@Slf4j
public final class ImageIOUtil {

    private ImageIOUtil() {
    }

    /** Key for ImageInfo's custom objects to embed the ImageIO metadata */
    public static final Object IMAGEIO_METADATA = IIOMetadata.class;

    /**
     * Extracts the resolution information from the standard ImageIO metadata.
     *
     * @param iiometa
     *            the metadata provided by ImageIO
     * @param size
     *            the image size object
     */
    public static void extractResolution(final IIOMetadata iiometa,
            final ImageSize size) {
        if (iiometa != null && iiometa.isStandardMetadataFormatSupported()) {
            final Element metanode = (Element) iiometa
                    .getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            final Element dim = getChild(metanode, "Dimension");
            if (dim != null) {
                Element child;
                double dpiHorz = size.getDpiHorizontal();
                double dpiVert = size.getDpiVertical();
                child = getChild(dim, "HorizontalPixelSize");
                if (child != null) {
                    final float value = Float.parseFloat(child
                            .getAttribute("value"));
                    if (value != 0 && !Float.isInfinite(value)) {
                        dpiHorz = UnitConv.IN2MM / value;
                    }
                }
                child = getChild(dim, "VerticalPixelSize");
                if (child != null) {
                    final float value = Float.parseFloat(child
                            .getAttribute("value"));
                    if (value != 0 && !Float.isInfinite(value)) {
                        dpiVert = UnitConv.IN2MM / value;
                    }
                }
                size.setResolution(dpiHorz, dpiVert);
                size.calcSizeFromPixels();
            }
        }
    }

    /**
     * Returns a child element of another element or null if there's no such
     * child.
     *
     * @param el
     *            the parent element
     * @param name
     *            the name of the requested child
     * @return the child or null if there's no such child
     */
    public static Element getChild(final Element el, final String name) {
        final NodeList nodes = el.getElementsByTagName(name);
        if (nodes.getLength() > 0) {
            return (Element) nodes.item(0);
        } else {
            return null;
        }
    }

    /**
     * Dumps the content of an IIOMetadata instance to System.out.
     *
     * @param iiometa
     *            the metadata
     */
    public static void dumpMetadataToSystemOut(final IIOMetadata iiometa) {
        final String[] metanames = iiometa.getMetadataFormatNames();
        for (final String metaname : metanames) {
            log.info("--->" + metaname);
            dumpNodeToSystemOut(iiometa.getAsTree(metaname));
        }
    }

    /**
     * Serializes a W3C DOM node to a String and dumps it to System.out.
     *
     * @param node
     *            a W3C DOM node
     */
    private static void dumpNodeToSystemOut(final Node node) {
        try {
            final Transformer trans = TransformerFactory.newInstance()
                    .newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            final Source src = new DOMSource(node);
            final Result res = new StreamResult(System.out);
            trans.transform(src, res);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
