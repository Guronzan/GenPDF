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

/* $Id: BitmapProducerJava2D.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop.visual;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.util.DefaultErrorListener;

/**
 * BitmapProducer implementation that uses the Java2DRenderer to create bitmaps.
 * <p>
 * Here's what the configuration element looks like for the class:
 * <p>
 *
 * <pre>
 * <producer classname="org.apache.fop.visual.BitmapProducerJava2D">
 *   <delete-temp-files>false</delete-temp-files>
 * </producer>
 * </pre>
 * <p>
 * The "delete-temp-files" element is optional and defaults to true.
 */
@Slf4j
public class BitmapProducerJava2D extends AbstractBitmapProducer implements
Configurable {

    // configure fopFactory as desired
    private final FopFactory fopFactory = FopFactory.newInstance();

    private boolean deleteTempFiles;

    /** @see org.apache.avalon.framework.configuration.Configurable */
    @Override
    public void configure(final Configuration cfg)
            throws ConfigurationException {
        this.deleteTempFiles = cfg.getChild("delete-temp-files")
                .getValueAsBoolean(true);
    }

    /** @see org.apache.fop.visual.BitmapProducer */
    @Override
    public BufferedImage produce(final File src, final int index,
            final ProducerContext context) {
        try {
            final FOUserAgent userAgent = this.fopFactory.newFOUserAgent();
            userAgent.setTargetResolution(context.getTargetResolution());
            userAgent
            .setBaseURL(src.getParentFile().toURI().toURL().toString());

            final File outputFile = new File(context.getTargetDir(),
                    src.getName() + "." + index + ".java2d.png");
            OutputStream out = new FileOutputStream(outputFile);
            out = new BufferedOutputStream(out);
            try {
                final Fop fop = this.fopFactory.newFop(
                        org.apache.xmlgraphics.util.MimeConstants.MIME_PNG,
                        userAgent, out);
                final SAXResult res = new SAXResult(fop.getDefaultHandler());

                final Transformer transformer = getTransformer(context);
                transformer.setErrorListener(new DefaultErrorListener(log));
                transformer.transform(new StreamSource(src), res);
            } finally {
                IOUtils.closeQuietly(out);
            }

            final BufferedImage img = BitmapComparator.getImage(outputFile);
            if (this.deleteTempFiles) {
                if (!outputFile.delete()) {
                    log.warn("Cannot delete " + outputFile);
                    outputFile.deleteOnExit();
                }
            }
            return img;
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
