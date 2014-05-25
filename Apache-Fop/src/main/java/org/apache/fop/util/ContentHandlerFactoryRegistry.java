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

/* $Id: ContentHandlerFactoryRegistry.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.util;

import java.util.Iterator;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.util.Service;

/**
 * This class holds references to various XML handlers used by FOP. It also
 * supports automatic discovery of additional XML handlers available through the
 * class path.
 */
@Slf4j
public class ContentHandlerFactoryRegistry {

    /** Map from namespace URIs to ContentHandlerFactories */
    private final Map factories = new java.util.HashMap();

    /**
     * Default constructor.
     */
    public ContentHandlerFactoryRegistry() {
        discover();
    }

    /**
     * Add an XML handler. The handler itself is inspected to find out what it
     * supports.
     *
     * @param classname
     *            the fully qualified class name
     */
    public void addContentHandlerFactory(final String classname) {
        try {
            final ContentHandlerFactory factory = (ContentHandlerFactory) Class
                    .forName(classname).newInstance();
            addContentHandlerFactory(factory);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find " + classname);
        } catch (final InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                    + classname);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access " + classname);
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException(classname + " is not an "
                    + ContentHandlerFactory.class.getName());
        }
    }

    /**
     * Add an ContentHandlerFactory. The instance is inspected to find out what
     * it supports.
     *
     * @param factory
     *            the ContentHandlerFactory instance
     */
    public void addContentHandlerFactory(final ContentHandlerFactory factory) {
        final String[] ns = factory.getSupportedNamespaces();
        for (final String element : ns) {
            this.factories.put(element, factory);
        }
    }

    /**
     * Retrieves a ContentHandlerFactory instance of a given namespace URI.
     *
     * @param namespaceURI
     *            the namespace to be handled.
     * @return the ContentHandlerFactory or null, if no suitable instance is
     *         available.
     */
    public ContentHandlerFactory getFactory(final String namespaceURI) {
        final ContentHandlerFactory factory = (ContentHandlerFactory) this.factories
                .get(namespaceURI);
        return factory;
    }

    /**
     * Discovers ContentHandlerFactory implementations through the classpath and
     * dynamically registers them.
     */
    private void discover() {
        // add mappings from available services
        final Iterator providers = Service
                .providers(ContentHandlerFactory.class);
        if (providers != null) {
            while (providers.hasNext()) {
                final ContentHandlerFactory factory = (ContentHandlerFactory) providers
                        .next();
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Dynamically adding ContentHandlerFactory: "
                                + factory.getClass().getName());
                    }
                    addContentHandlerFactory(factory);
                } catch (final IllegalArgumentException e) {
                    log.error("Error while adding ContentHandlerFactory", e);
                }

            }
        }
    }
}
