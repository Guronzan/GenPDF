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

/* $Id: AbstractImageSessionContext.java 828814 2009-10-22 18:52:33Z jeremias $ */

package org.apache.xmlgraphics.image.loader.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.ImageSource;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.apache.xmlgraphics.image.loader.util.SoftMapCache;

/**
 * Abstract base class for classes implementing ImageSessionContext. This class
 * provides all the special treatment for Source creation, i.e. it provides
 * optimized Source objects where possible.
 */
@Slf4j
public abstract class AbstractImageSessionContext implements
        ImageSessionContext {

    private static boolean noSourceReuse = false;

    static {
        // TODO Temporary measure to track down a problem
        // See: http://markmail.org/message/k6mno3jsxmovaz2e
        final String v = System.getProperty(AbstractImageSessionContext.class
                .getName() + ".no-source-reuse");
        noSourceReuse = Boolean.valueOf(v).booleanValue();
    }

    /**
     * Attempts to resolve the given URI.
     * 
     * @param uri
     *            URI to access
     * @return A {@link javax.xml.transform.Source} object, or null if the URI
     *         cannot be resolved.
     */
    protected abstract Source resolveURI(final String uri);

    /** {@inheritDoc} */
    @Override
    public Source newSource(final String uri) {
        final Source source = resolveURI(uri);
        if (source == null) {
            if (log.isDebugEnabled()) {
                log.debug("URI could not be resolved: " + uri);
            }
            return null;
        }
        if (!(source instanceof StreamSource) && !(source instanceof SAXSource)) {
            // Return any non-stream Sources and let the ImageLoaders deal with
            // them
            return source;
        }

        ImageSource imageSource = null;

        final String resolvedURI = source.getSystemId();
        URL url;
        try {
            url = new URL(resolvedURI);
        } catch (final MalformedURLException e) {
            url = null;
        }
        final File f = /* FileUtils. */toFile(url);
        if (f != null) {
            boolean directFileAccess = true;
            assert source instanceof StreamSource
                    || source instanceof SAXSource;
            InputStream in = ImageUtil.getInputStream(source);
            if (in == null) {
                try {
                    in = new java.io.FileInputStream(f);
                } catch (final FileNotFoundException fnfe) {
                    log.error("Error while opening file."
                            + " Could not load image from system identifier '"
                            + source.getSystemId() + "' (" + fnfe.getMessage()
                            + ")");
                    return null;
                }
            }
            if (in != null) {
                in = ImageUtil.decorateMarkSupported(in);
                try {
                    if (ImageUtil.isGZIPCompressed(in)) {
                        // GZIPped stream are not seekable, so buffer/cache like
                        // other URLs
                        directFileAccess = false;
                    }
                } catch (final IOException ioe) {
                    log.error("Error while checking the InputStream for GZIP compression."
                            + " Could not load image from system identifier '"
                            + source.getSystemId()
                            + "' ("
                            + ioe.getMessage()
                            + ")");
                    return null;
                }
            }

            if (directFileAccess) {
                // Close as the file is reopened in a more optimal way
                IOUtils.closeQuietly(in);
                try {
                    // We let the OS' file system cache do the caching for us
                    // --> lower Java memory consumption, probably no speed loss
                    final ImageInputStream newInputStream = ImageIO
                            .createImageInputStream(f);
                    if (newInputStream == null) {
                        log.error("Unable to create ImageInputStream for local file "
                                + f
                                + " from system identifier '"
                                + source.getSystemId() + "'");
                        return null;
                    } else {
                        imageSource = new ImageSource(newInputStream,
                                resolvedURI, true);
                    }
                } catch (final IOException ioe) {
                    log.error("Unable to create ImageInputStream for local file"
                            + " from system identifier '"
                            + source.getSystemId()
                            + "' ("
                            + ioe.getMessage()
                            + ")");
                }
            }
        }

        if (imageSource == null) {
            if (ImageUtil.hasReader(source)
                    && !ImageUtil.hasInputStream(source)) {
                // We don't handle Reader instances here so return the Source
                // unchanged
                return source;
            }
            // Got a valid source, obtain an InputStream from it
            InputStream in = ImageUtil.getInputStream(source);
            if (in == null && url != null) {
                try {
                    in = url.openStream();
                } catch (final Exception ex) {
                    log.error("Unable to obtain stream from system identifier '"
                            + source.getSystemId() + "'");
                }
            }
            if (in == null) {
                log.error("The Source that was returned from URI resolution didn't contain"
                        + " an InputStream for URI: " + uri);
                return null;
            }

            try {
                // Buffer and uncompress if necessary
                in = ImageUtil.autoDecorateInputStream(in);
                imageSource = new ImageSource(createImageInputStream(in),
                        source.getSystemId(), false);
            } catch (final IOException ioe) {
                log.error("Unable to create ImageInputStream for InputStream"
                        + " from system identifier '" + source.getSystemId()
                        + "' (" + ioe.getMessage() + ")");
            }
        }
        return imageSource;
    }

    protected ImageInputStream createImageInputStream(final InputStream in)
            throws IOException {
        final ImageInputStream iin = ImageIO.createImageInputStream(in);
        return (ImageInputStream) Proxy.newProxyInstance(
                ImageInputStream.class.getClassLoader(),
                new Class[] { ImageInputStream.class },
                new ObservingImageInputStreamInvocationHandler(iin, in));
    }

    private static class ObservingImageInputStreamInvocationHandler implements
            InvocationHandler {

        private final ImageInputStream iin;
        private InputStream in;

        public ObservingImageInputStreamInvocationHandler(
                final ImageInputStream iin, final InputStream underlyingStream) {
            this.iin = iin;
            this.in = underlyingStream;
        }

        /** {@inheritDoc} */
        @Override
        public Object invoke(final Object proxy, final Method method,
                final Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                try {
                    return method.invoke(this.iin, args);
                } finally {
                    IOUtils.closeQuietly(this.in);
                    this.in = null;
                }
            } else {
                return method.invoke(this.iin, args);
            }
        }

    }

    /**
     * Convert from a <code>URL</code> to a <code>File</code>.
     * <p>
     * This method will decode the URL. Syntax such as
     * <code>file:///my%20docs/file.txt</code> will be correctly decoded to
     * <code>/my docs/file.txt</code>.
     * <p>
     * Note: this method has been copied over from Apache Commons IO and
     * enhanced to support UNC paths.
     *
     * @param url
     *            the file URL to convert, <code>null</code> returns
     *            <code>null</code>
     * @return the equivalent <code>File</code> object, or <code>null</code> if
     *         the URL's protocol is not <code>file</code>
     * @throws IllegalArgumentException
     *             if the file is incorrectly encoded
     */
    public static File toFile(final URL url) {
        if (url == null || !url.getProtocol().equals("file")) {
            return null;
        } else {
            try {
                String filename = "";
                if (url.getHost() != null && url.getHost().length() > 0) {
                    filename += Character.toString(File.separatorChar)
                            + Character.toString(File.separatorChar)
                            + url.getHost();
                }
                filename += url.getFile().replace('/', File.separatorChar);
                filename = java.net.URLDecoder.decode(filename, "UTF-8");
                final File f = new File(filename);
                if (!f.isFile()) {
                    return null;
                }
                return f;
            } catch (final java.io.UnsupportedEncodingException uee) {
                assert false;
                return null;
            }
        }
    }

    private final SoftMapCache sessionSources = new SoftMapCache(false); // no
                                                                         // need
                                                                         // for
                                                                         // synchronization

    /** {@inheritDoc} */
    @Override
    public Source getSource(final String uri) {
        return (Source) this.sessionSources.remove(uri);
    }

    /** {@inheritDoc} */
    @Override
    public Source needSource(final String uri) throws FileNotFoundException {
        Source src = getSource(uri);
        if (src == null) {
            if (log.isDebugEnabled()) {
                log.debug("Creating new Source for " + uri);

            }
            src = newSource(uri);
            if (src == null) {
                throw new FileNotFoundException("Image not found: " + uri);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Reusing Source for " + uri);
            }
        }
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public void returnSource(final String uri, final Source src) {
        // Safety check to make sure the Preloaders behave
        final ImageInputStream in = ImageUtil.getImageInputStream(src);
        try {
            if (in != null && in.getStreamPosition() != 0) {
                throw new IllegalStateException(
                        "ImageInputStream is not reset for: " + uri);
            }
        } catch (final IOException ioe) {
            // Ignore exception
            ImageUtil.closeQuietly(src);
        }

        if (isReusable(src)) {
            // Only return the Source if it's reusable
            log.debug("Returning Source for " + uri);
            this.sessionSources.put(uri, src);
        } else {
            // Otherwise, try to close if possible and forget about it
            ImageUtil.closeQuietly(src);
        }
    }

    /**
     * Indicates whether a Source is reusable. A Source object is reusable if
     * it's an {@link ImageSource} (containing an {@link ImageInputStream}) or a
     * {@link DOMSource}.
     * 
     * @param src
     *            the Source object
     * @return true if the Source is reusable
     */
    protected boolean isReusable(final Source src) {
        if (noSourceReuse) {
            return false;
        }
        if (src instanceof ImageSource) {
            final ImageSource is = (ImageSource) src;
            if (is.getImageInputStream() != null) {
                return true;
            }
        }
        if (src instanceof DOMSource) {
            return true;
        }
        return false;
    }
}
