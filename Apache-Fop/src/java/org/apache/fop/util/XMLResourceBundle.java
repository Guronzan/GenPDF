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

/* $Id: XMLResourceBundle.java 1324913 2012-04-11 18:43:46Z gadams $ */

package org.apache.fop.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Stack;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.xmlgraphics.util.QName;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class is a ResourceBundle that loads its contents from XML files instead
 * of properties files (like PropertiesResourceBundle).
 * <p>
 * The XML format for this resource bundle implementation is the following (the
 * same as Apache Cocoon's XMLResourceBundle):
 * 
 * <pre>
 * &lt;catalogue xml:lang="en"&gt;
 *   &lt;message key="key1"&gt;Message &lt;br/&gt; Value 1&lt;/message&gt;
 *   &lt;message key="key2"&gt;Message &lt;br/&gt; Value 1&lt;/message&gt;
 *   ...
 * &lt;/catalogue&gt;
 * </pre>
 */
public class XMLResourceBundle extends ResourceBundle {

    // Note: Some code here has been copied and adapted from Apache Harmony!

    private final Properties resources = new Properties();

    private Locale locale;

    private static SAXTransformerFactory tFactory = (SAXTransformerFactory) SAXTransformerFactory
            .newInstance();

    /**
     * Creates a resource bundle from an InputStream.
     * 
     * @param in
     *            the stream to read from
     * @throws IOException
     *             if an I/O error occurs
     */
    public XMLResourceBundle(final InputStream in) throws IOException {
        try {
            final Transformer transformer = tFactory.newTransformer();
            final StreamSource src = new StreamSource(in);
            final SAXResult res = new SAXResult(new CatalogueHandler());
            transformer.transform(src, res);
        } catch (final TransformerException e) {
            throw new IOException("Error while parsing XML resource bundle: "
                    + e.getMessage());
        }
    }

    /**
     * Gets a resource bundle using the specified base name, default locale, and
     * class loader.
     * 
     * @param baseName
     *            the base name of the resource bundle, a fully qualified class
     *            name
     * @param loader
     *            the class loader from which to load the resource bundle
     * @return a resource bundle for the given base name and the default locale
     * @throws MissingResourceException
     *             if no resource bundle for the specified base name can be
     *             found
     * @see java.util.ResourceBundle#getBundle(String)
     */
    public static ResourceBundle getXMLBundle(final String baseName,
            final ClassLoader loader) throws MissingResourceException {
        return getXMLBundle(baseName, Locale.getDefault(), loader);
    }

    /**
     * Gets a resource bundle using the specified base name, locale, and class
     * loader.
     * 
     * @param baseName
     *            the base name of the resource bundle, a fully qualified class
     *            name
     * @param locale
     *            the locale for which a resource bundle is desired
     * @param loader
     *            the class loader from which to load the resource bundle
     * @return a resource bundle for the given base name and locale
     * @throws MissingResourceException
     *             if no resource bundle for the specified base name can be
     *             found
     * @see java.util.ResourceBundle#getBundle(String, Locale, ClassLoader)
     */
    public static ResourceBundle getXMLBundle(final String baseName,
            final Locale locale, final ClassLoader loader)
            throws MissingResourceException {
        if (loader == null) {
            throw new NullPointerException("loader must not be null");
        }
        if (baseName == null) {
            throw new NullPointerException("baseName must not be null");
        }
        assert locale != null;
        ResourceBundle bundle;
        if (!locale.equals(Locale.getDefault())) {
            bundle = handleGetXMLBundle(baseName, "_" + locale, false, loader);
            if (bundle != null) {
                return bundle;
            }
        }
        bundle = handleGetXMLBundle(baseName, "_" + Locale.getDefault(), true,
                loader);
        if (bundle != null) {
            return bundle;
        }
        throw new MissingResourceException(baseName + " (" + locale + ")",
                baseName + '_' + locale, null);
    }

    static class MissingBundle extends ResourceBundle {
        @Override
        public Enumeration getKeys() {
            return null;
        }

        @Override
        public Object handleGetObject(final String name) {
            return null;
        }
    }

    private static final ResourceBundle MISSING = new MissingBundle();
    private static final ResourceBundle MISSINGBASE = new MissingBundle();

    private static Map cache = new java.util.WeakHashMap();

    // <Object, Hashtable<String, ResourceBundle>>

    private static ResourceBundle handleGetXMLBundle(final String base,
            final String locale, final boolean loadBase,
            final ClassLoader loader) {
        XMLResourceBundle bundle = null;
        final String bundleName = base + locale;
        final Object cacheKey = loader != null ? (Object) loader
                : (Object) "null";
        Hashtable loaderCache; // <String, ResourceBundle>
        synchronized (cache) {
            loaderCache = (Hashtable) cache.get(cacheKey);
            if (loaderCache == null) {
                loaderCache = new Hashtable();
                cache.put(cacheKey, loaderCache);
            }
        }
        final ResourceBundle result = (ResourceBundle) loaderCache
                .get(bundleName);
        if (result != null) {
            if (result == MISSINGBASE) {
                return null;
            }
            if (result == MISSING) {
                if (!loadBase) {
                    return null;
                }
                final String extension = strip(locale);
                if (extension == null) {
                    return null;
                }
                return handleGetXMLBundle(base, extension, loadBase, loader);
            }
            return result;
        }

        final String fileName = bundleName.replace('.', '/') + ".xml";
        final InputStream stream = (InputStream) AccessController
                .doPrivileged(new PrivilegedAction() {
                    @Override
                    public Object run() {
                        return loader == null ? ClassLoader
                                .getSystemResourceAsStream(fileName) : loader
                                .getResourceAsStream(fileName);
                    }
                });
        if (stream != null) {
            try {
                try {
                    bundle = new XMLResourceBundle(stream);
                } finally {
                    stream.close();
                }
                bundle.setLocale(locale);
            } catch (final IOException e) {
                throw new MissingResourceException(e.getMessage(), base, null);
            }
        }

        final String extension = strip(locale);
        if (bundle != null) {
            if (extension != null) {
                final ResourceBundle parent = handleGetXMLBundle(base,
                        extension, true, loader);
                if (parent != null) {
                    bundle.setParent(parent);
                }
            }
            loaderCache.put(bundleName, bundle);
            return bundle;
        }

        if (extension != null) {
            final ResourceBundle fallback = handleGetXMLBundle(base, extension,
                    loadBase, loader);
            if (fallback != null) {
                loaderCache.put(bundleName, fallback);
                return fallback;
            }
        }
        loaderCache.put(bundleName, loadBase ? MISSINGBASE : MISSING);
        return null;
    }

    private void setLocale(final String name) {
        String language = "";
        String country = "";
        String variant = "";
        if (name.length() > 1) {
            int nextIndex = name.indexOf('_', 1);
            if (nextIndex == -1) {
                nextIndex = name.length();
            }
            language = name.substring(1, nextIndex);
            if (nextIndex + 1 < name.length()) {
                final int index = nextIndex;
                nextIndex = name.indexOf('_', nextIndex + 1);
                if (nextIndex == -1) {
                    nextIndex = name.length();
                }
                country = name.substring(index + 1, nextIndex);
                if (nextIndex + 1 < name.length()) {
                    variant = name.substring(nextIndex + 1, name.length());
                }
            }
        }
        this.locale = new Locale(language, country, variant);
    }

    private static String strip(final String name) {
        final int index = name.lastIndexOf('_');
        if (index != -1) {
            return name.substring(0, index);
        }
        return null;
    }

    private Enumeration getLocalKeys() {
        return this.resources.propertyNames();
    }

    /** {@inheritDoc} */
    @Override
    public Locale getLocale() {
        return this.locale;
    }

    /** {@inheritDoc} */
    @Override
    public Enumeration getKeys() {
        if (this.parent == null) {
            return getLocalKeys();
        }
        return new Enumeration() {
            private final Enumeration local = getLocalKeys();
            private final Enumeration pEnum = XMLResourceBundle.this.parent
                    .getKeys();

            private Object nextElement;

            private boolean findNext() {
                if (this.nextElement != null) {
                    return true;
                }
                while (this.pEnum.hasMoreElements()) {
                    final Object next = this.pEnum.nextElement();
                    if (!XMLResourceBundle.this.resources.containsKey(next)) {
                        this.nextElement = next;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean hasMoreElements() {
                if (this.local.hasMoreElements()) {
                    return true;
                }
                return findNext();
            }

            @Override
            public Object nextElement() {
                if (this.local.hasMoreElements()) {
                    return this.local.nextElement();
                }
                if (findNext()) {
                    final Object result = this.nextElement;
                    this.nextElement = null;
                    return result;
                }
                // Cause an exception
                return this.pEnum.nextElement();
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    protected Object handleGetObject(final String key) {
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }
        return this.resources.get(key);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "XMLResourceBundle: " + getLocale();
    }

    private class CatalogueHandler extends DefaultHandler {

        private static final String CATALOGUE = "catalogue";
        private static final String MESSAGE = "message";

        private final StringBuilder valueBuffer = new StringBuilder();
        private final Stack elementStack = new Stack();
        private String currentKey = null;

        private boolean isOwnNamespace(final String uri) {
            return "".equals(uri);
        }

        private QName getParentElementName() {
            return (QName) this.elementStack.peek();
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final String uri, final String localName,
                final String qName, final Attributes atts) throws SAXException {
            super.startElement(uri, localName, qName, atts);
            final QName elementName = new QName(uri, qName);
            if (isOwnNamespace(uri)) {
                if (CATALOGUE.equals(localName)) {
                    // nop
                } else if (MESSAGE.equals(localName)) {
                    if (!CATALOGUE
                            .equals(getParentElementName().getLocalName())) {
                        throw new SAXException(MESSAGE + " must be a child of "
                                + CATALOGUE);
                    }
                    this.currentKey = atts.getValue("key");
                } else {
                    throw new SAXException("Invalid element name: "
                            + elementName);
                }
            } else {
                // ignore
            }
            this.valueBuffer.setLength(0);
            this.elementStack.push(elementName);
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName,
                final String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            this.elementStack.pop();
            if (isOwnNamespace(uri)) {
                if (CATALOGUE.equals(localName)) {
                    // nop
                } else if (MESSAGE.equals(localName)) {
                    if (this.currentKey == null) {
                        throw new SAXException(
                                "current key is null (attribute 'key' might be mistyped)");
                    }
                    XMLResourceBundle.this.resources.put(this.currentKey,
                            this.valueBuffer.toString());
                    this.currentKey = null;
                }
            } else {
                // ignore
            }
            this.valueBuffer.setLength(0);
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch, final int start,
                final int length) throws SAXException {
            super.characters(ch, start, length);
            this.valueBuffer.append(ch, start, length);
        }

    }

}
