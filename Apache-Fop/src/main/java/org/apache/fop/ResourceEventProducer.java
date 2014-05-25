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

/* $Id: ResourceEventProducer.java 985537 2010-08-14 17:17:00Z jeremias $ */

package org.apache.fop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.fop.events.EventBroadcaster;
import org.apache.fop.events.EventProducer;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.w3c.dom.Document;
import org.xml.sax.Locator;

/**
 * Event producer interface for resource events (missing images, fonts etc.).
 */
public interface ResourceEventProducer extends EventProducer {

    /**
     * Provider class for the event producer.
     */
    final class Provider {

        private Provider() {
        }

        /**
         * Returns an event producer.
         * 
         * @param broadcaster
         *            the event broadcaster to use
         * @return the requested event producer
         */
        public static ResourceEventProducer get(
                final EventBroadcaster broadcaster) {
            return (ResourceEventProducer) broadcaster
                    .getEventProducerFor(ResourceEventProducer.class);
        }
    }

    /**
     * Image not found.
     * 
     * @param source
     *            the event source
     * @param uri
     *            the original URI of the image
     * @param fnfe
     *            the "file not found" exception
     * @param loc
     *            the location of the error or null
     * @event.severity ERROR
     */
    void imageNotFound(final Object source, final String uri,
            final FileNotFoundException fnfe, final Locator loc);

    /**
     * Error while processing image.
     * 
     * @param source
     *            the event source
     * @param uri
     *            the original URI of the image
     * @param e
     *            the image exception
     * @param loc
     *            the location of the error or null
     * @event.severity ERROR
     */
    void imageError(final Object source, final String uri,
            final ImageException e, final Locator loc);

    /**
     * I/O error while loading an image.
     * 
     * @param source
     *            the event source
     * @param uri
     *            the original URI of the image
     * @param ioe
     *            the I/O exception
     * @param loc
     *            the location of the error or null
     * @event.severity ERROR
     */
    void imageIOError(final Object source, final String uri,
            final IOException ioe, final Locator loc);

    /**
     * Error while writing/serializing an image to an output format.
     * 
     * @param source
     *            the event source
     * @param e
     *            the original exception
     * @event.severity ERROR
     */
    void imageWritingError(final Object source, final Exception e);

    /**
     * Error while handling a URI.
     * 
     * @param source
     *            the event source
     * @param uri
     *            the original URI of the image
     * @param e
     *            the original exception
     * @param loc
     *            the location of the error or null
     * @event.severity ERROR
     */
    void uriError(final Object source, final String uri, final Exception e,
            final Locator loc);

    /**
     * Intrinsic size of fo:instream-foreign-object could not be determined.
     * 
     * @param source
     *            the event source
     * @param loc
     *            the location of the error or null
     * @event.severity ERROR
     */
    void ifoNoIntrinsicSize(final Object source, final Locator loc);

    /**
     * Error processing foreign XML content.
     * 
     * @param source
     *            the event source
     * @param doc
     *            the foreign XML
     * @param namespaceURI
     *            the namespace URI of the foreign XML
     * @param e
     *            the original exception
     * @event.severity ERROR
     */
    void foreignXMLProcessingError(final Object source, final Document doc,
            final String namespaceURI, final Exception e);

    /**
     * No handler for foreign XML content.
     * 
     * @param source
     *            the event source
     * @param doc
     *            the foreign XML
     * @param namespaceURI
     *            the namespace URI of the foreign XML
     * @event.severity ERROR
     */
    void foreignXMLNoHandler(final Object source, final Document doc,
            final String namespaceURI);

    /**
     * Cannot delete a temporary file.
     * 
     * @param source
     *            the event source
     * @param tempFile
     *            the temporary file
     * @event.severity ERROR
     */
    void cannotDeleteTempFile(final Object source, final File tempFile);

    /**
     * Catalog Resolver not found along the class path
     * 
     * @param source
     *            the event source
     * @event.severity ERROR
     */
    void catalogResolverNotFound(final Object source);

    /**
     * Catalog Resolver not created, due to InstantiationException or
     * IllegalAccessException
     * 
     * @param source
     *            the event source
     * @param message
     *            the exception message
     * @event.severity ERROR
     */
    void catalogResolverNotCreated(final Object source, final String message);
}
