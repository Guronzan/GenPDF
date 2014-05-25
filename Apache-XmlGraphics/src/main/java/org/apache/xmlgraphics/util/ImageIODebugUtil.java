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

/* $Id: ImageIODebugUtil.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.util;

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

import org.w3c.dom.Node;

/**
 * Helper class for debugging stuff in Image I/O.
 *
 * @version $Id: ImageIODebugUtil.java 1345683 2012-06-03 14:50:33Z gadams $
 */
@Slf4j
public final class ImageIODebugUtil {

    private ImageIODebugUtil() {
    }

    public static void dumpMetadata(final IIOMetadata meta,
            final boolean nativeFormat) {
        String format;
        if (nativeFormat) {
            format = meta.getNativeMetadataFormatName();
        } else {
            format = IIOMetadataFormatImpl.standardMetadataFormatName;
        }
        final Node node = meta.getAsTree(format);
        dumpNode(node);
    }

    public static void dumpNode(final Node node) {
        try {
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            final Source src = new DOMSource(node);
            final Result res = new StreamResult(System.out);
            t.transform(src, res);
            log.info("");
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
