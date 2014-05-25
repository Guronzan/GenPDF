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

/* $Id: FOProcessorImpl.java 1297404 2012-03-06 10:17:54Z vhennebert $ */

package org.apache.fop.threading;

import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.io.FilenameUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;

/**
 * Default implementation of the {@link Processor} interface using FOP.
 */
public class FOProcessorImpl extends AbstractLogEnabled implements Processor,
        Configurable, Initializable {

    private final FopFactory fopFactory = FopFactory.newInstance();
    private final TransformerFactory factory = TransformerFactory.newInstance();
    private String userconfig;
    private String mime;
    private String fileExtension;

    /** {@inheritDoc} */
    @Override
    public void configure(final Configuration configuration)
            throws ConfigurationException {
        this.userconfig = configuration.getChild("userconfig").getValue(null);
        this.mime = configuration.getChild("mime").getValue(
                org.apache.xmlgraphics.util.MimeConstants.MIME_PDF);
        this.fileExtension = configuration.getChild("extension").getValue(
                ".pdf");
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() throws Exception {
        if (this.userconfig != null) {
            getLogger().debug("Setting user config: " + this.userconfig);
            this.fopFactory.setUserConfig(this.userconfig);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void process(final Source src, final Templates templates,
            final OutputStream out) throws org.apache.fop.apps.FOPException,
            java.io.IOException {
        final FOUserAgent foUserAgent = this.fopFactory.newFOUserAgent();
        foUserAgent.setBaseURL(src.getSystemId());
        try {
            final URL url = new URL(src.getSystemId());
            final String filename = FilenameUtils.getName(url.getPath());
            foUserAgent.getEventBroadcaster().addEventListener(
                    new AvalonAdapter(getLogger(), filename));
        } catch (final MalformedURLException mfue) {
            throw new RuntimeException(mfue);
        }
        final Fop fop = this.fopFactory.newFop(this.mime, foUserAgent, out);

        try {
            Transformer transformer;
            if (templates == null) {
                transformer = this.factory.newTransformer();
            } else {
                transformer = templates.newTransformer();
            }
            final Result res = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, res);
        } catch (final TransformerException e) {
            throw new FOPException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getTargetFileExtension() {
        return this.fileExtension;
    }
}