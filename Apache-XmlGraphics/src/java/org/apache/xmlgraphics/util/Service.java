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

/* $Id: Service.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

/**
 * This class handles looking up service providers on the class path. It
 * implements the system described in:
 *
 * <a href='http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service
 * Provider'>JAR File Specification Under Service Provider</a>. Note that this
 * interface is very similar to the one they describe which seems to be missing
 * in the JDK.
 *
 * @version $Id: Service.java 1345683 2012-06-03 14:50:33Z gadams $
 *
 *          Originally authored by Thomas DeWeese.
 */
public final class Service {

    private Service() {
    }

    // Remember providers we have looked up before.
    static Map<String, List<String>> classMap = new java.util.HashMap<String, List<String>>();
    static Map<String, List<Object>> instanceMap = new java.util.HashMap<String, List<Object>>();

    /**
     * Returns an iterator where each element should implement the interface (or
     * subclass the baseclass) described by cls. The Classes are found by
     * searching the classpath for service files named:
     * 'META-INF/services/&lt;fully qualified classname&gt; that list fully
     * qualifted classnames of classes that implement the service files classes
     * interface. These classes must have default constructors.
     *
     * @param cls
     *            The class/interface to search for providers of.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> Iterator<T> providers(final Class<T> cls) {
        final String serviceFile = getServiceFilename(cls);

        List<Object> l = instanceMap.get(serviceFile);
        if (l != null) {
            return (Iterator<T>) l.iterator();
        }

        l = new java.util.ArrayList<Object>();
        instanceMap.put(serviceFile, l);

        final ClassLoader cl = getClassLoader(cls);
        if (cl != null) {
            final List<String> names = getProviderNames(cls, cl);
            for (final String name : names) {
                try {
                    // Try and load the class
                    final Object obj = cl.loadClass(name).newInstance();
                    // stick it into our vector...
                    l.add(obj);
                } catch (final Exception ex) {
                    // Just try the next name
                }
            }
        }
        return (Iterator<T>) l.iterator();
    }

    /**
     * Returns an iterator where each element should be the name of a class that
     * implements the interface (or subclass the baseclass) described by cls.
     * The Classes are found by searching the classpath for service files named:
     * 'META-INF/services/&lt;fully qualified classname&gt; that list fully
     * qualified classnames of classes that implement the service files classes
     * interface.
     *
     * @param cls
     *            The class/interface to search for providers of.
     */
    public static synchronized Iterator<String> providerNames(final Class<?> cls) {
        final String serviceFile = getServiceFilename(cls);

        List<String> l = classMap.get(serviceFile);
        if (l != null) {
            return l.iterator();
        }

        l = new java.util.ArrayList<String>();
        classMap.put(serviceFile, l);
        l.addAll(getProviderNames(cls));
        return l.iterator();
    }

    private static List<String> getProviderNames(final Class<?> cls) {
        return getProviderNames(cls, getClassLoader(cls));
    }

    private static List<String> getProviderNames(final Class<?> cls,
            final ClassLoader cl) {
        final List<String> l = new java.util.ArrayList<String>();

        // No class loader so we can't find 'serviceFile'.
        if (cl == null) {
            return l;
        }

        Enumeration<URL> e;
        try {
            e = cl.getResources(getServiceFilename(cls));
        } catch (final IOException ioe) {
            return l;
        }

        while (e.hasMoreElements()) {
            try {
                final URL u = e.nextElement();

                final InputStream is = u.openStream();
                final Reader r = new InputStreamReader(is, "UTF-8");
                final BufferedReader br = new BufferedReader(r);
                try {
                    for (String line = br.readLine(); line != null; line = br
                            .readLine()) {
                        // First strip any comment...
                        final int idx = line.indexOf('#');
                        if (idx != -1) {
                            line = line.substring(0, idx);
                        }

                        // Trim whitespace.
                        line = line.trim();

                        if (line.length() != 0) {
                            l.add(line);
                        }
                    }
                } finally {
                    IOUtils.closeQuietly(br);
                    IOUtils.closeQuietly(is);
                }
            } catch (final Exception ex) {
                // Just try the next file...
            }
        }
        return l;
    }

    private static ClassLoader getClassLoader(final Class<?> cls) {
        ClassLoader cl = null;
        try {
            cl = cls.getClassLoader();
        } catch (final SecurityException se) {
            // Ooops! can't get his class loader.
        }
        // Can always request your own class loader. But it might be 'null'.
        if (cl == null) {
            cl = Service.class.getClassLoader();
        }
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        return cl;
    }

    private static String getServiceFilename(final Class<?> cls) {
        return "META-INF/services/" + cls.getName();
    }

}
