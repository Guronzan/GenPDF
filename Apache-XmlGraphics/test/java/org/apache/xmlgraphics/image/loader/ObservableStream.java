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

/* $Id: ObservableStream.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.image.loader;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.imageio.stream.ImageInputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * Implemented by observable streams.
 */
public interface ObservableStream {

    /**
     * Indicates whether the stream has been closed.
     *
     * @return true if the stream is closed
     */
    boolean isClosed();

    /**
     * Returns the system ID for the stream being observed.
     *
     * @return the system ID
     */
    String getSystemID();

    public static class Factory {

        public static ImageInputStream observe(final ImageInputStream iin,
                final String systemID) {
            return (ImageInputStream) Proxy.newProxyInstance(Factory.class
                    .getClassLoader(), new Class[] { ImageInputStream.class,
                ObservableStream.class },
                new ObservingImageInputStreamInvocationHandler(iin,
                        systemID));
        }

    }

    @Slf4j
    public static class ObservingImageInputStreamInvocationHandler implements
    InvocationHandler, ObservableStream {

        private final ImageInputStream iin;
        private boolean closed;
        private final String systemID;

        public ObservingImageInputStreamInvocationHandler(
                final ImageInputStream iin, final String systemID) {
            this.iin = iin;
            this.systemID = systemID;
        }

        /** {@inheritDoc} */
        @Override
        public Object invoke(final Object proxy, final Method method,
                final Object[] args) throws Throwable {
            if (method.getDeclaringClass().equals(ObservableStream.class)) {
                return method.invoke(this, args);
            } else if ("close".equals(method.getName())) {
                if (!closed) {
                    log.debug("Stream is being closed: " + getSystemID());
                    closed = true;
                    try {
                        return method.invoke(iin, args);
                    } catch (final InvocationTargetException ite) {
                        log.error("Error while closing underlying stream: ",
                                ite);
                        throw ite;
                    }
                } else {
                    throw new IllegalStateException("Stream is already closed!");
                }
            } else {
                return method.invoke(iin, args);
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getSystemID() {
            return this.systemID;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isClosed() {
            return this.closed;
        }

    }

}
