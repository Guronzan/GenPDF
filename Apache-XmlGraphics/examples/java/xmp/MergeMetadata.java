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

/* $Id: MergeMetadata.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package xmp;

import java.net.URL;
import java.util.Date;

import javax.xml.transform.TransformerException;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.xmp.Metadata;
import org.apache.xmlgraphics.xmp.XMPConstants;
import org.apache.xmlgraphics.xmp.XMPParser;
import org.apache.xmlgraphics.xmp.XMPProperty;
import org.apache.xmlgraphics.xmp.XMPSerializer;
import org.apache.xmlgraphics.xmp.schemas.DublinCoreAdapter;
import org.xml.sax.SAXException;

/**
 * This example shows how to parse an XMP metadata file.
 */
@Slf4j
public class MergeMetadata {

    private static void mergeMetadata() throws TransformerException,
    SAXException {
        final URL url = MergeMetadata.class.getResource("pdf-example.xmp");
        final Metadata meta1 = XMPParser.parseXMP(url);

        final Metadata meta2 = new Metadata();
        DublinCoreAdapter dc = new DublinCoreAdapter(meta2);
        dc.setTitle("de", "Der Herr der Ringe");
        dc.setTitle("en", "Lord of the Rings");
        dc.addCreator("J.R.R. Tolkien"); // Will replace creator from
        // pdf-example.xmp
        dc.addDate(new Date());

        meta2.mergeInto(meta1);

        final Metadata meta = meta1;
        XMPProperty prop;
        dc = new DublinCoreAdapter(meta);
        final String[] creators = dc.getCreators();
        for (final String creator : creators) {
            log.info("Creator: " + creator);
        }
        log.info("Title: " + dc.getTitle());
        log.info("Title de: " + dc.getTitle("de"));
        log.info("Title en: " + dc.getTitle("en"));
        prop = meta.getProperty(XMPConstants.XMP_BASIC_NAMESPACE, "CreateDate");
        log.info("Creation Date: " + prop.getValue());
        prop = meta
                .getProperty(XMPConstants.XMP_BASIC_NAMESPACE, "CreatorTool");
        log.info("Creator Tool: " + prop.getValue());
        prop = meta.getProperty(XMPConstants.ADOBE_PDF_NAMESPACE, "Producer");
        log.info("Producer: " + prop.getValue());
        prop = meta.getProperty(XMPConstants.ADOBE_PDF_NAMESPACE, "PDFVersion");
        log.info("PDF version: " + prop.getValue());

        XMPSerializer.writeXMPPacket(meta, System.out, false);
    }

    /**
     * Command-line interface.
     *
     * @param args
     *            the command-line arguments
     */
    public static void main(final String[] args) {
        try {
            mergeMetadata();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
