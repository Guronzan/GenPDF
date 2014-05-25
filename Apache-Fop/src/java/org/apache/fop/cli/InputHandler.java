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

/* $Id: InputHandler.java 1297232 2012-03-05 21:13:28Z gadams $ */

package org.apache.fop.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.ResourceEventProducer;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.render.awt.viewer.Renderable;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Class for handling files input from command line either with XML and XSLT
 * files (and optionally xsl parameters) or FO File input alone.
 */
@Slf4j
public class InputHandler implements ErrorListener, Renderable {

    /** original source file */
    protected File sourcefile;
    private File stylesheet; // for XML/XSLT usage
    private List<String> xsltParams; // for XML/XSLT usage
    private EntityResolver entityResolver = null;
    private URIResolver uriResolver = null;

    /**
     * Constructor for XML->XSLT->FO input
     *
     * @param xmlfile
     *            XML file
     * @param xsltfile
     *            XSLT file
     * @param params
     *            List of command-line parameters (name, value, name, value,
     *            ...) for XSL stylesheet, null if none
     */
    public InputHandler(final File xmlfile, final File xsltfile,
            final List<String> params) {
        this.sourcefile = xmlfile;
        this.stylesheet = xsltfile;
        this.xsltParams = params;
    }

    /**
     * Constructor for FO input
     *
     * @param fofile
     *            the file to read the FO document.
     */
    public InputHandler(final File fofile) {
        this.sourcefile = fofile;
    }

    /**
     * Generate a document, given an initialized Fop object
     *
     * @param userAgent
     *            the user agent
     * @param outputFormat
     *            the output format to generate (MIME type, see MimeConstants)
     * @param out
     *            the output stream to write the generated output to (may be
     *            null if not applicable)
     * @throws FOPException
     *             in case of an error during processing
     */
    public void renderTo(final FOUserAgent userAgent,
            final String outputFormat, final OutputStream out)
            throws FOPException {

        final FopFactory factory = userAgent.getFactory();
        Fop fop;
        if (out != null) {
            fop = factory.newFop(outputFormat, userAgent, out);
        } else {
            fop = factory.newFop(outputFormat, userAgent);
        }

        // if base URL was not explicitly set in FOUserAgent, obtain here
        if (fop.getUserAgent().getBaseURL() == null && this.sourcefile != null) {
            String baseURL = null;

            try {
                baseURL = new File(this.sourcefile.getAbsolutePath())
                .getParentFile().toURI().toURL().toExternalForm();
            } catch (final Exception e) {
                baseURL = "";
            }
            fop.getUserAgent().setBaseURL(baseURL);
        }

        // Resulting SAX events (the generated FO) must be piped through to FOP
        final Result res = new SAXResult(fop.getDefaultHandler());

        transformTo(res);
    }

    /** {@inheritDoc} */
    @Override
    public void renderTo(final FOUserAgent userAgent, final String outputFormat)
            throws FOPException {
        renderTo(userAgent, outputFormat, null);
    }

    /**
     * In contrast to render(Fop) this method only performs the XSLT stage and
     * saves the intermediate XSL-FO file to the output file.
     *
     * @param out
     *            OutputStream to write the transformation result to.
     * @throws FOPException
     *             in case of an error during processing
     */
    public void transformTo(final OutputStream out) throws FOPException {
        final Result res = new StreamResult(out);
        transformTo(res);
    }

    /**
     * Creates a Source for the main input file. Processes XInclude if available
     * in the XML parser.
     *
     * @return the Source for the main input file
     */
    protected Source createMainSource() {
        Source source;
        InputStream in;
        String uri;
        if (this.sourcefile != null) {
            try {
                in = new java.io.FileInputStream(this.sourcefile);
                uri = this.sourcefile.toURI().toASCIIString();
            } catch (final FileNotFoundException e) {
                // handled elsewhere
                return new StreamSource(this.sourcefile);
            }
        } else {
            in = System.in;
            uri = null;
        }
        try {
            final InputSource is = new InputSource(in);
            is.setSystemId(uri);
            final XMLReader xr = getXMLReader();
            if (this.entityResolver != null) {
                xr.setEntityResolver(this.entityResolver);
            }
            source = new SAXSource(xr, is);
        } catch (final SAXException e) {
            if (this.sourcefile != null) {
                source = new StreamSource(this.sourcefile);
            } else {
                source = new StreamSource(in, uri);
            }
        } catch (final ParserConfigurationException e) {
            if (this.sourcefile != null) {
                source = new StreamSource(this.sourcefile);
            } else {
                source = new StreamSource(in, uri);
            }
        }
        return source;
    }

    /**
     * Creates a catalog resolver and uses it for XML parsing and XSLT URI
     * resolution. Tries the Apache Commons Resolver, and if unsuccessful, tries
     * the same built into Java 6.
     *
     * @param userAgent
     *            the user agent instance
     */
    public void createCatalogResolver(final FOUserAgent userAgent) {
        final String[] classNames = new String[] {
                "org.apache.xml.resolver.tools.CatalogResolver",
        "com.sun.org.apache.xml.internal.resolver.tools.CatalogResolver" };
        final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                .get(userAgent.getEventBroadcaster());
        Class resolverClass = null;
        for (int i = 0; i < classNames.length && resolverClass == null; ++i) {
            try {
                resolverClass = Class.forName(classNames[i]);
            } catch (final ClassNotFoundException e) {
                // No worries
            }
        }
        if (resolverClass == null) {
            eventProducer.catalogResolverNotFound(this);
            return;
        }
        try {
            this.entityResolver = (EntityResolver) resolverClass.newInstance();
            this.uriResolver = (URIResolver) resolverClass.newInstance();
        } catch (final InstantiationException e) {
            log.error("Error creating the catalog resolver: " + e.getMessage());
            eventProducer.catalogResolverNotCreated(this, e.getMessage());
        } catch (final IllegalAccessException e) {
            log.error("Error creating the catalog resolver: " + e.getMessage());
            eventProducer.catalogResolverNotCreated(this, e.getMessage());
        }
    }

    /**
     * Creates a Source for the selected stylesheet.
     *
     * @return the Source for the selected stylesheet or null if there's no
     *         stylesheet
     */
    protected Source createXSLTSource() {
        Source xslt = null;
        if (this.stylesheet != null) {
            if (this.entityResolver != null) {
                try {
                    final InputSource is = new InputSource(
                            this.stylesheet.getPath());
                    final XMLReader xr = getXMLReader();
                    xr.setEntityResolver(this.entityResolver);
                    xslt = new SAXSource(xr, is);
                } catch (final SAXException e) {
                    // return StreamSource
                } catch (final ParserConfigurationException e) {
                    // return StreamSource
                }
            }
            if (xslt == null) {
                xslt = new StreamSource(this.stylesheet);
            }
        }
        return xslt;
    }

    private XMLReader getXMLReader() throws ParserConfigurationException,
            SAXException {
        final SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://xml.org/sax/features/namespaces", true);
        spf.setFeature("http://apache.org/xml/features/xinclude", true);
        final XMLReader xr = spf.newSAXParser().getXMLReader();
        return xr;
    }

    /**
     * Transforms the input document to the input format expected by FOP using
     * XSLT.
     *
     * @param result
     *            the Result object where the result of the XSL transformation
     *            is sent to
     * @throws FOPException
     *             in case of an error during processing
     */
    protected void transformTo(final Result result) throws FOPException {
        try {
            // Setup XSLT
            final TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer;

            final Source xsltSource = createXSLTSource();
            if (xsltSource == null) { // FO Input
                transformer = factory.newTransformer();
            } else { // XML/XSLT input
                transformer = factory.newTransformer(xsltSource);

                // Set the value of parameters, if any, defined for stylesheet
                if (this.xsltParams != null) {
                    for (int i = 0; i < this.xsltParams.size(); i += 2) {
                        transformer.setParameter(this.xsltParams.get(i),
                                this.xsltParams.get(i + 1));
                    }
                }
                if (this.uriResolver != null) {
                    transformer.setURIResolver(this.uriResolver);
                }
            }
            transformer.setErrorListener(this);

            // Create a SAXSource from the input Source file
            final Source src = createMainSource();

            // Start XSLT transformation and FOP processing
            transformer.transform(src, result);

        } catch (final Exception e) {
            throw new FOPException(e);
        }
    }

    // --- Implementation of the ErrorListener interface ---

    /**
     * {@inheritDoc}
     */
    @Override
    public void warning(final TransformerException exc) {
        log.warn(exc.getLocalizedMessage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final TransformerException exc) {
        log.error(exc.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fatalError(final TransformerException exc)
            throws TransformerException {
        throw exc;
    }

}
