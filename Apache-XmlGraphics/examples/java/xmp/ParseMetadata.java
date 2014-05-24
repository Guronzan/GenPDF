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

/* $Id: ParseMetadata.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package xmp;

import java.net.URL;

import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.xmp.Metadata;
import org.apache.xmlgraphics.xmp.XMPArray;
import org.apache.xmlgraphics.xmp.XMPConstants;
import org.apache.xmlgraphics.xmp.XMPParser;
import org.apache.xmlgraphics.xmp.XMPProperty;
import org.apache.xmlgraphics.xmp.XMPSerializer;
import org.apache.xmlgraphics.xmp.schemas.DublinCoreAdapter;
import org.apache.xmlgraphics.xmp.schemas.DublinCoreSchema;
import org.xml.sax.SAXException;

/**
 * This example shows how to parse an XMP metadata file.
 */
@Slf4j
public class ParseMetadata {

    private static void parseMetadata() throws TransformerException,
    SAXException {
        final URL url = ParseMetadata.class.getResource("pdf-example.xmp");
        final Metadata meta = XMPParser.parseXMP(url);
        XMPProperty prop;
        prop = meta.getProperty(XMPConstants.DUBLIN_CORE_NAMESPACE, "creator");
        XMPArray array;
        array = prop.getArrayValue();
        for (int i = 0, c = array.getSize(); i < c; i++) {
            log.info("Creator: " + array.getValue(i));
        }
        prop = meta.getProperty(XMPConstants.DUBLIN_CORE_NAMESPACE, "title");
        array = prop.getArrayValue();
        log.info("Default Title: " + array.getSimpleValue());
        log.info("German Title: " + array.getLangValue("de"));
        prop = meta.getProperty(XMPConstants.XMP_BASIC_NAMESPACE, "CreateDate");
        log.info("Creation Date: " + prop.getValue());
        prop = meta
                .getProperty(XMPConstants.XMP_BASIC_NAMESPACE, "CreatorTool");
        log.info("Creator Tool: " + prop.getValue());
        prop = meta.getProperty(XMPConstants.ADOBE_PDF_NAMESPACE, "Producer");
        log.info("Producer: " + prop.getValue());
        prop = meta.getProperty(XMPConstants.ADOBE_PDF_NAMESPACE, "PDFVersion");
        log.info("PDF version: " + prop.getValue());

        final DublinCoreAdapter dc = DublinCoreSchema.getAdapter(meta);
        log.info("Default title: " + dc.getTitle());
        log.info("German title: " + dc.getTitle("de"));

        final StreamResult res = new StreamResult(System.out);
        XMPSerializer.writeXML(meta, res);
    }

    /**
     * Command-line interface.
     *
     * @param args
     *            the command-line arguments
     */
    public static void main(final String[] args) {
        try {
            parseMetadata();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
