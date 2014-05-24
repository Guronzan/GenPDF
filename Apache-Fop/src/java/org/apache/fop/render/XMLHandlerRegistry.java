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

/* $Id: XMLHandlerRegistry.java 1055034 2011-01-04 13:36:10Z spepping $ */

package org.apache.fop.render;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlgraphics.util.Service;

/**
 * This class holds references to various XML handlers used by FOP. It also
 * supports automatic discovery of additional XML handlers available through the
 * class path.
 */
public class XMLHandlerRegistry {

    /** the logger */
    private static Log log = LogFactory.getLog(XMLHandlerRegistry.class);

    /** Map containing XML handlers for various document types */
    private final Map<String, List<XMLHandler>> handlers = new java.util.HashMap<String, List<XMLHandler>>();

    /**
     * Default constructor.
     */
    public XMLHandlerRegistry() {
        discoverXMLHandlers();
    }

    /**
     * Add a default XML handler which is able to handle any namespace.
     *
     * @param handler
     *            XMLHandler to use
     */
    private void setDefaultXMLHandler(final XMLHandler handler) {
        addXMLHandler(XMLHandler.HANDLE_ALL, handler);
    }

    /**
     * Add an XML handler. The handler itself is inspected to find out what it
     * supports.
     *
     * @param classname
     *            the fully qualified class name
     */
    public void addXMLHandler(final String classname) {
        try {
            final XMLHandler handlerInstance = (XMLHandler) Class.forName(
                    classname).newInstance();
            addXMLHandler(handlerInstance);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find " + classname);
        } catch (final InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                    + classname);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access " + classname);
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException(classname + " is not an "
                    + XMLHandler.class.getName());
        }
    }

    /**
     * Add an XML handler. The handler itself is inspected to find out what it
     * supports.
     *
     * @param handler
     *            the XMLHandler instance
     */
    public void addXMLHandler(final XMLHandler handler) {
        final String ns = handler.getNamespace();
        if (ns == null) {
            setDefaultXMLHandler(handler);
        } else {
            addXMLHandler(ns, handler);
        }
    }

    /**
     * Add an XML handler for the given MIME type and XML namespace.
     *
     * @param ns
     *            Namespace URI
     * @param handler
     *            XMLHandler to use
     */
    private void addXMLHandler(final String ns, final XMLHandler handler) {
        List<XMLHandler> lst = this.handlers.get(ns);
        if (lst == null) {
            lst = new java.util.ArrayList<XMLHandler>();
            this.handlers.put(ns, lst);
        }
        lst.add(handler);
    }

    /**
     * Returns an XMLHandler which handles an XML dialect of the given namespace
     * and for a specified output format defined by its MIME type.
     *
     * @param renderer
     *            the Renderer for which to retrieve a Renderer
     * @param ns
     *            the XML namespace associated with the XML to be rendered
     * @return the XMLHandler responsible for handling the XML or null if none
     *         is available
     */
    public XMLHandler getXMLHandler(final Renderer renderer, final String ns) {
        XMLHandler handler;

        List<XMLHandler> lst = this.handlers.get(ns);
        handler = getXMLHandler(renderer, lst);
        if (handler == null) {
            lst = this.handlers.get(XMLHandler.HANDLE_ALL);
            handler = getXMLHandler(renderer, lst);
        }
        return handler;
    }

    private XMLHandler getXMLHandler(final Renderer renderer,
            final List<XMLHandler> lst) {
        XMLHandler handler;
        if (lst != null) {
            for (int i = 0, c = lst.size(); i < c; ++i) {
                // TODO Maybe add priorities later
                handler = lst.get(i);
                if (handler.supportsRenderer(renderer)) {
                    return handler;
                }
            }
        }
        return null; // No handler found
    }

    /**
     * Discovers XMLHandler implementations through the classpath and
     * dynamically registers them.
     */
    private void discoverXMLHandlers() {
        // add mappings from available services
        final Iterator<XMLHandler> providers = Service
                .providers(XMLHandler.class);
        if (providers != null) {
            while (providers.hasNext()) {
                final XMLHandler handler = providers.next();
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Dynamically adding XMLHandler: "
                                + handler.getClass().getName());
                    }
                    addXMLHandler(handler);
                } catch (final IllegalArgumentException e) {
                    log.error("Error while adding XMLHandler", e);
                }

            }
        }
    }
}
