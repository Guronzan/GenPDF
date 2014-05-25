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

/* $Id: RendererFactory.java 1237582 2012-01-30 09:49:22Z mehdi $ */

package org.apache.fop.render;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.AreaTreeHandler;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.render.intermediate.AbstractIFDocumentHandlerMaker;
import org.apache.fop.render.intermediate.EventProducingFilter;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFDocumentHandlerConfigurator;
import org.apache.fop.render.intermediate.IFRenderer;
import org.apache.xmlgraphics.util.Service;

/**
 * Factory for FOEventHandlers and Renderers.
 */
@Slf4j
public class RendererFactory {

    private final Map rendererMakerMapping = new java.util.HashMap();
    private final Map eventHandlerMakerMapping = new java.util.HashMap();
    private final Map documentHandlerMakerMapping = new java.util.HashMap();

    private boolean rendererPreferred = false;

    /**
     * Main constructor.
     */
    public RendererFactory() {
        discoverRenderers();
        discoverFOEventHandlers();
        discoverDocumentHandlers();
    }

    /**
     * Controls whether a {@link Renderer} is preferred over a
     * {@link IFDocumentHandler} if both are available for the same MIME type.
     *
     * @param value
     *            true to prefer the {@link Renderer}, false to prefer the
     *            {@link IFDocumentHandler}.
     */
    public void setRendererPreferred(final boolean value) {
        this.rendererPreferred = value;
    }

    /**
     * Indicates whether a {@link Renderer} is preferred over a
     * {@link IFDocumentHandler} if both are available for the same MIME type.
     *
     * @return true if the {@link Renderer} is preferred, false if the
     *         {@link IFDocumentHandler} is preferred.
     */
    public boolean isRendererPreferred() {
        return this.rendererPreferred;
    }

    /**
     * Add a new RendererMaker. If another maker has already been registered for
     * a particular MIME type, this call overwrites the existing one.
     *
     * @param maker
     *            the RendererMaker
     */
    public void addRendererMaker(final AbstractRendererMaker maker) {
        final String[] mimes = maker.getSupportedMimeTypes();
        for (final String mime : mimes) {
            // This overrides any renderer previously set for a MIME type
            if (this.rendererMakerMapping.get(mime) != null) {
                log.trace("Overriding renderer for " + mime + " with "
                        + maker.getClass().getName());
            }
            this.rendererMakerMapping.put(mime, maker);
        }
    }

    /**
     * Add a new FOEventHandlerMaker. If another maker has already been
     * registered for a particular MIME type, this call overwrites the existing
     * one.
     *
     * @param maker
     *            the FOEventHandlerMaker
     */
    public void addFOEventHandlerMaker(final AbstractFOEventHandlerMaker maker) {
        final String[] mimes = maker.getSupportedMimeTypes();
        for (final String mime : mimes) {
            // This overrides any event handler previously set for a MIME type
            if (this.eventHandlerMakerMapping.get(mime) != null) {
                log.trace("Overriding FOEventHandler for " + mime + " with "
                        + maker.getClass().getName());
            }
            this.eventHandlerMakerMapping.put(mime, maker);
        }
    }

    /**
     * Add a new document handler maker. If another maker has already been
     * registered for a particular MIME type, this call overwrites the existing
     * one.
     *
     * @param maker
     *            the intermediate format document handler maker
     */
    public void addDocumentHandlerMaker(
            final AbstractIFDocumentHandlerMaker maker) {
        final String[] mimes = maker.getSupportedMimeTypes();
        for (final String mime : mimes) {
            // This overrides any renderer previously set for a MIME type
            if (this.documentHandlerMakerMapping.get(mime) != null) {
                log.trace("Overriding document handler for " + mime + " with "
                        + maker.getClass().getName());
            }
            this.documentHandlerMakerMapping.put(mime, maker);
        }
    }

    /**
     * Add a new RendererMaker. If another maker has already been registered for
     * a particular MIME type, this call overwrites the existing one.
     *
     * @param className
     *            the fully qualified class name of the RendererMaker
     */
    public void addRendererMaker(final String className) {
        try {
            final AbstractRendererMaker makerInstance = (AbstractRendererMaker) Class
                    .forName(className).newInstance();
            addRendererMaker(makerInstance);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find " + className);
        } catch (final InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                    + className);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access " + className);
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException(className + " is not an "
                    + AbstractRendererMaker.class.getName());
        }
    }

    /**
     * Add a new FOEventHandlerMaker. If another maker has already been
     * registered for a particular MIME type, this call overwrites the existing
     * one.
     *
     * @param className
     *            the fully qualified class name of the FOEventHandlerMaker
     */
    public void addFOEventHandlerMaker(final String className) {
        try {
            final AbstractFOEventHandlerMaker makerInstance = (AbstractFOEventHandlerMaker) Class
                    .forName(className).newInstance();
            addFOEventHandlerMaker(makerInstance);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find " + className);
        } catch (final InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                    + className);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access " + className);
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException(className + " is not an "
                    + AbstractFOEventHandlerMaker.class.getName());
        }
    }

    /**
     * Add a new document handler maker. If another maker has already been
     * registered for a particular MIME type, this call overwrites the existing
     * one.
     *
     * @param className
     *            the fully qualified class name of the document handler maker
     */
    public void addDocumentHandlerMaker(final String className) {
        try {
            final AbstractIFDocumentHandlerMaker makerInstance = (AbstractIFDocumentHandlerMaker) Class
                    .forName(className).newInstance();
            addDocumentHandlerMaker(makerInstance);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find " + className);
        } catch (final InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                    + className);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access " + className);
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException(className + " is not an "
                    + AbstractIFDocumentHandlerMaker.class.getName());
        }
    }

    /**
     * Returns a RendererMaker which handles the given MIME type.
     *
     * @param mime
     *            the requested output format
     * @return the requested RendererMaker or null if none is available
     */
    public AbstractRendererMaker getRendererMaker(final String mime) {
        final AbstractRendererMaker maker = (AbstractRendererMaker) this.rendererMakerMapping
                .get(mime);
        return maker;
    }

    /**
     * Returns a FOEventHandlerMaker which handles the given MIME type.
     *
     * @param mime
     *            the requested output format
     * @return the requested FOEventHandlerMaker or null if none is available
     */
    public AbstractFOEventHandlerMaker getFOEventHandlerMaker(final String mime) {
        final AbstractFOEventHandlerMaker maker = (AbstractFOEventHandlerMaker) this.eventHandlerMakerMapping
                .get(mime);
        return maker;
    }

    /**
     * Returns a RendererMaker which handles the given MIME type.
     *
     * @param mime
     *            the requested output format
     * @return the requested RendererMaker or null if none is available
     */
    public AbstractIFDocumentHandlerMaker getDocumentHandlerMaker(
            final String mime) {
        final AbstractIFDocumentHandlerMaker maker = (AbstractIFDocumentHandlerMaker) this.documentHandlerMakerMapping
                .get(mime);
        return maker;
    }

    /**
     * Creates a Renderer object based on render-type desired
     *
     * @param userAgent
     *            the user agent for access to configuration
     * @param outputFormat
     *            the MIME type of the output format to use (ex.
     *            "application/pdf").
     * @return the new Renderer instance
     * @throws FOPException
     *             if the renderer cannot be properly constructed
     */
    public Renderer createRenderer(final FOUserAgent userAgent,
            final String outputFormat) throws FOPException {
        if (userAgent.getDocumentHandlerOverride() != null) {
            return createRendererForDocumentHandler(userAgent
                    .getDocumentHandlerOverride());
        } else if (userAgent.getRendererOverride() != null) {
            return userAgent.getRendererOverride();
        } else {
            Renderer renderer;
            if (isRendererPreferred()) {
                // Try renderer first
                renderer = tryRendererMaker(userAgent, outputFormat);
                if (renderer == null) {
                    renderer = tryIFDocumentHandlerMaker(userAgent,
                            outputFormat);
                }
            } else {
                // Try document handler first
                renderer = tryIFDocumentHandlerMaker(userAgent, outputFormat);
                if (renderer == null) {
                    renderer = tryRendererMaker(userAgent, outputFormat);
                }
            }
            if (renderer == null) {
                throw new UnsupportedOperationException(
                        "No renderer for the requested format available: "
                                + outputFormat);
            }
            return renderer;
        }
    }

    private Renderer tryIFDocumentHandlerMaker(final FOUserAgent userAgent,
            final String outputFormat) throws FOPException {
        final AbstractIFDocumentHandlerMaker documentHandlerMaker = getDocumentHandlerMaker(outputFormat);
        if (documentHandlerMaker != null) {
            final IFDocumentHandler documentHandler = createDocumentHandler(
                    userAgent, outputFormat);
            return createRendererForDocumentHandler(documentHandler);
        } else {
            return null;
        }
    }

    private Renderer tryRendererMaker(final FOUserAgent userAgent,
            final String outputFormat) throws FOPException {
        final AbstractRendererMaker maker = getRendererMaker(outputFormat);
        if (maker != null) {
            final Renderer rend = maker.makeRenderer(userAgent);
            final RendererConfigurator configurator = maker
                    .getConfigurator(userAgent);
            if (configurator != null) {
                configurator.configure(rend);
            }
            return rend;
        } else {
            return null;
        }
    }

    private Renderer createRendererForDocumentHandler(
            final IFDocumentHandler documentHandler) {
        final IFRenderer rend = new IFRenderer(documentHandler.getContext()
                .getUserAgent());
        rend.setDocumentHandler(documentHandler);
        return rend;
    }

    /**
     * Creates FOEventHandler instances based on the desired output.
     *
     * @param userAgent
     *            the user agent for access to configuration
     * @param outputFormat
     *            the MIME type of the output format to use (ex.
     *            "application/pdf").
     * @param out
     *            the OutputStream where the output is written to (if
     *            applicable)
     * @return the newly constructed FOEventHandler
     * @throws FOPException
     *             if the FOEventHandler cannot be properly constructed
     */
    public FOEventHandler createFOEventHandler(final FOUserAgent userAgent,
            final String outputFormat, final OutputStream out)
                    throws FOPException {

        if (userAgent.getFOEventHandlerOverride() != null) {
            return userAgent.getFOEventHandlerOverride();
        } else {
            final AbstractFOEventHandlerMaker maker = getFOEventHandlerMaker(outputFormat);
            if (maker != null) {
                return maker.makeFOEventHandler(userAgent, out);
            } else {
                final AbstractRendererMaker rendMaker = getRendererMaker(outputFormat);
                AbstractIFDocumentHandlerMaker documentHandlerMaker = null;
                boolean outputStreamMissing = userAgent.getRendererOverride() == null
                        && userAgent.getDocumentHandlerOverride() == null;
                if (rendMaker == null) {
                    documentHandlerMaker = getDocumentHandlerMaker(outputFormat);
                    if (documentHandlerMaker != null) {
                        outputStreamMissing &= out == null
                                && documentHandlerMaker.needsOutputStream();
                    }
                } else {
                    outputStreamMissing &= out == null
                            && rendMaker.needsOutputStream();
                }
                if (userAgent.getRendererOverride() != null
                        || rendMaker != null
                        || userAgent.getDocumentHandlerOverride() != null
                        || documentHandlerMaker != null) {
                    if (outputStreamMissing) {
                        throw new FOPException("OutputStream has not been set");
                    }
                    // Found a Renderer so we need to construct an
                    // AreaTreeHandler.
                    return new AreaTreeHandler(userAgent, outputFormat, out);
                } else {
                    throw new UnsupportedOperationException(
                            "Don't know how to handle \""
                                    + outputFormat
                                    + "\" as an output format."
                                    + " Neither an FOEventHandler, nor a Renderer could be found"
                                    + " for this output format.");
                }
            }
        }
    }

    /**
     * Creates a {@link IFDocumentHandler} object based on the desired output
     * format.
     *
     * @param userAgent
     *            the user agent for access to configuration
     * @param outputFormat
     *            the MIME type of the output format to use (ex.
     *            "application/pdf").
     * @return the new {@link IFDocumentHandler} instance
     * @throws FOPException
     *             if the document handler cannot be properly constructed
     */
    public IFDocumentHandler createDocumentHandler(final FOUserAgent userAgent,
            final String outputFormat) throws FOPException {
        if (userAgent.getDocumentHandlerOverride() != null) {
            return userAgent.getDocumentHandlerOverride();
        }
        final AbstractIFDocumentHandlerMaker maker = getDocumentHandlerMaker(outputFormat);
        if (maker == null) {
            throw new UnsupportedOperationException(
                    "No IF document handler for the requested format available: "
                            + outputFormat);
        }
        final IFDocumentHandler documentHandler = maker
                .makeIFDocumentHandler(userAgent);
        final IFDocumentHandlerConfigurator configurator = documentHandler
                .getConfigurator();
        if (configurator != null) {
            configurator.configure(documentHandler);
        }
        return new EventProducingFilter(documentHandler, userAgent);
    }

    /**
     * @return an array of all supported MIME types
     */
    public String[] listSupportedMimeTypes() {
        final List lst = new java.util.ArrayList();
        Iterator iter = this.rendererMakerMapping.keySet().iterator();
        while (iter.hasNext()) {
            lst.add(iter.next());
        }
        iter = this.eventHandlerMakerMapping.keySet().iterator();
        while (iter.hasNext()) {
            lst.add(iter.next());
        }
        iter = this.documentHandlerMakerMapping.keySet().iterator();
        while (iter.hasNext()) {
            lst.add(iter.next());
        }
        Collections.sort(lst);
        return (String[]) lst.toArray(new String[lst.size()]);
    }

    /**
     * Discovers Renderer implementations through the classpath and dynamically
     * registers them.
     */
    private void discoverRenderers() {
        // add mappings from available services
        final Iterator providers = Service.providers(Renderer.class);
        if (providers != null) {
            while (providers.hasNext()) {
                final AbstractRendererMaker maker = (AbstractRendererMaker) providers
                        .next();
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Dynamically adding maker for Renderer: "
                                + maker.getClass().getName());
                    }
                    addRendererMaker(maker);
                } catch (final IllegalArgumentException e) {
                    log.error("Error while adding maker for Renderer", e);
                }

            }
        }
    }

    /**
     * Discovers FOEventHandler implementations through the classpath and
     * dynamically registers them.
     */
    private void discoverFOEventHandlers() {
        // add mappings from available services
        final Iterator providers = Service.providers(FOEventHandler.class);
        if (providers != null) {
            while (providers.hasNext()) {
                final AbstractFOEventHandlerMaker maker = (AbstractFOEventHandlerMaker) providers
                        .next();
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Dynamically adding maker for FOEventHandler: "
                                + maker.getClass().getName());
                    }
                    addFOEventHandlerMaker(maker);
                } catch (final IllegalArgumentException e) {
                    log.error("Error while adding maker for FOEventHandler", e);
                }

            }
        }
    }

    /**
     * Discovers {@link IFDocumentHandler} implementations through the classpath
     * and dynamically registers them.
     */
    private void discoverDocumentHandlers() {
        // add mappings from available services
        final Iterator providers = Service.providers(IFDocumentHandler.class);
        if (providers != null) {
            while (providers.hasNext()) {
                final AbstractIFDocumentHandlerMaker maker = (AbstractIFDocumentHandlerMaker) providers
                        .next();
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Dynamically adding maker for IFDocumentHandler: "
                                + maker.getClass().getName());
                    }
                    addDocumentHandlerMaker(maker);
                } catch (final IllegalArgumentException e) {
                    log.error("Error while adding maker for IFDocumentHandler",
                            e);
                }

            }
        }
    }

}
