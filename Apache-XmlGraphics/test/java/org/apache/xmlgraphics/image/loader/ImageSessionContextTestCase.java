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

/* $Id: ImageSessionContextTestCase.java 750418 2009-03-05 11:03:54Z vhennebert $ */

package org.apache.xmlgraphics.image.loader;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Tests for AbstractImageSessionContext.
 */
public class ImageSessionContextTestCase extends TestCase {

    private final MockImageContext imageContext = MockImageContext
            .getInstance();

    public ImageSessionContextTestCase(final String name) {
        super(name);
    }

    public void testStreamSourceWithSystemID() {
        final URIResolver resolver = new URIResolver() {
            @Override
            public Source resolve(final String href, final String base)
                    throws TransformerException {
                if (href.startsWith("img:")) {
                    final String filename = href.substring(4);
                    return new StreamSource(base + filename);
                } else {
                    return null;
                }
            }
        };
        final String uri = "img:asf-logo.png";

        final ImageSource imgSrc = checkImageInputStreamAvailable(uri, resolver);
        assertTrue(imgSrc.isFastSource()); // Access through local file system
    }

    public void testStreamSourceWithInputStream() {
        final URIResolver resolver = new URIResolver() {
            @Override
            public Source resolve(final String href, final String base)
                    throws TransformerException {
                if (href.startsWith("img:")) {
                    final String filename = href.substring(4);
                    try {
                        return new StreamSource(new java.io.FileInputStream(
                                new File(
                                        MockImageSessionContext.IMAGE_BASE_DIR,
                                        filename)));
                    } catch (final FileNotFoundException e) {
                        throw new TransformerException(e);
                    }
                } else {
                    return null;
                }
            }
        };
        final String uri = "img:asf-logo.png";

        final ImageSource imgSrc = checkImageInputStreamAvailable(uri, resolver);
        // We don't pass in the URI, so no fast source is possible
        assertTrue(!imgSrc.isFastSource());
    }

    public void testStreamSourceWithFile() {
        final URIResolver resolver = new URIResolver() {
            @Override
            public Source resolve(final String href, final String base)
                    throws TransformerException {
                if (href.startsWith("img:")) {
                    final String filename = href.substring(4);
                    final File f = new File(
                            MockImageSessionContext.IMAGE_BASE_DIR, filename);
                    return new StreamSource(f);
                } else {
                    return null;
                }
            }
        };
        final String uri = "img:asf-logo.png";

        final ImageSource imgSrc = checkImageInputStreamAvailable(uri, resolver);
        assertTrue(imgSrc.isFastSource()); // Accessed through the local file
        // system
    }

    public void testStreamSourceWithInputStreamAndSystemID() {
        final URIResolver resolver = new URIResolver() {
            @Override
            public Source resolve(final String href, final String base)
                    throws TransformerException {
                if (href.startsWith("img:")) {
                    final String filename = href.substring(4);
                    try {
                        final File f = new File(
                                MockImageSessionContext.IMAGE_BASE_DIR,
                                filename);
                        return new StreamSource(new java.io.FileInputStream(f),
                                f.toURI().toASCIIString());
                    } catch (final FileNotFoundException e) {
                        throw new TransformerException(e);
                    }
                } else {
                    return null;
                }
            }
        };
        final String uri = "img:asf-logo.png";

        final ImageSource imgSrc = checkImageInputStreamAvailable(uri, resolver);
        assertTrue(imgSrc.isFastSource()); // Access through local file system
        // (thanks to the URI
        // being passed through by the
        // URIResolver)
    }

    public void testStreamSourceWithReader() {
        final URIResolver resolver = new URIResolver() {
            @Override
            public Source resolve(final String href, final String base)
                    throws TransformerException {
                if (href.startsWith("img:")) {
                    final String filename = href.substring(4);
                    return new StreamSource(new java.io.StringReader(filename));
                } else {
                    return null;
                }
            }
        };
        final String uri = "img:asf-logo.png";

        final Source src = resolve(uri, resolver);
        assertTrue(src instanceof StreamSource); // Source remains unchanged
        assertTrue(ImageUtil.hasReader(src));
    }

    private ImageSource checkImageInputStreamAvailable(final String uri,
            final URIResolver resolver) {
        final Source src = resolve(uri, resolver);
        assertNotNull("Source must not be null", src);
        assertTrue("Source must be an ImageSource", src instanceof ImageSource);
        final ImageSource imgSrc = (ImageSource) src;
        assertTrue(ImageUtil.hasImageInputStream(imgSrc));
        return imgSrc;
    }

    private Source resolve(final String uri, final URIResolver resolver) {
        final ImageSessionContext sessionContext = new SimpleURIResolverBasedImageSessionContext(
                this.imageContext, MockImageSessionContext.IMAGE_BASE_DIR,
                resolver);
        final Source src = sessionContext.newSource(uri);
        return src;
    }

    public void testSAXSourceWithSystemID() {
        final URIResolver resolver = new URIResolver() {
            @Override
            public Source resolve(final String href, final String base)
                    throws TransformerException {
                if (href.startsWith("img:")) {
                    final String filename = href.substring(4);
                    final InputSource is = new InputSource(base + filename);
                    return new SAXSource(is);
                } else {
                    return null;
                }
            }
        };
        final String uri = "img:asf-logo.png";

        final ImageSource imgSrc = checkImageInputStreamAvailable(uri, resolver);
        assertTrue(imgSrc.isFastSource());
    }

    public void testSAXSourceWithInputStream() {
        final URIResolver resolver = new URIResolver() {
            @Override
            public Source resolve(final String href, final String base)
                    throws TransformerException {
                if (href.startsWith("img:")) {
                    final String filename = href.substring(4);
                    InputSource is;
                    try {
                        is = new InputSource(new java.io.FileInputStream(
                                new File(
                                        MockImageSessionContext.IMAGE_BASE_DIR,
                                        filename)));
                    } catch (final FileNotFoundException e) {
                        throw new TransformerException(e);
                    }
                    return new SAXSource(is);
                } else {
                    return null;
                }
            }
        };
        final String uri = "img:asf-logo.png";

        checkImageInputStreamAvailable(uri, resolver);
    }

    public void testSAXSourceWithReader() {
        final URIResolver resolver = new URIResolver() {
            @Override
            public Source resolve(final String href, final String base)
                    throws TransformerException {
                if (href.startsWith("img:")) {
                    final String filename = href.substring(4);
                    InputSource is;
                    is = new InputSource(new java.io.StringReader(filename));
                    return new SAXSource(is);
                } else {
                    return null;
                }
            }
        };
        final String uri = "img:asf-logo.png";

        final Source src = resolve(uri, resolver);
        assertTrue(src instanceof SAXSource); // Source remains unchanged
        assertTrue(ImageUtil.hasReader(src));
    }

    private static final String SOME_XML = "<root><child id='1'>Hello World!</child></root>";

    public void testSAXSourceWithXMLReader() {
        final URIResolver resolver = new URIResolver() {
            @Override
            public Source resolve(final String href, final String base)
                    throws TransformerException {
                if (href.startsWith("xml:")) {
                    final String xml = href.substring(4);
                    final InputSource is = new InputSource(
                            new java.io.StringReader(xml));
                    return new SAXSource(createSomeXMLReader(), is);
                } else {
                    return null;
                }
            }
        };
        final String uri = "xml:" + SOME_XML;

        final Source src = resolve(uri, resolver);
        assertTrue(src instanceof SAXSource); // Source remains unchanged
        final SAXSource saxSrc = (SAXSource) src;
        assertNotNull(saxSrc.getXMLReader());
        assertNotNull(saxSrc.getInputSource());
    }

    public void testDOMSource() {
        final URIResolver resolver = new URIResolver() {
            @Override
            public Source resolve(final String href, final String base)
                    throws TransformerException {
                if (href.startsWith("xml:")) {
                    final String xml = href.substring(4);
                    final InputSource is = new InputSource(
                            new java.io.StringReader(xml));
                    final SAXSource sax = new SAXSource(createSomeXMLReader(),
                            is);

                    // Convert SAXSource to DOMSource
                    final TransformerFactory tFactory = TransformerFactory
                            .newInstance();
                    final Transformer transformer = tFactory.newTransformer();
                    final DOMResult res = new DOMResult();
                    transformer.transform(sax, res);
                    return new DOMSource(res.getNode());
                } else {
                    return null;
                }
            }
        };
        final String uri = "xml:" + SOME_XML;

        final Source src = resolve(uri, resolver);
        assertTrue(src instanceof DOMSource); // Source remains unchanged
        final DOMSource domSrc = (DOMSource) src;
        assertNotNull(domSrc.getNode());
    }

    private XMLReader createSomeXMLReader() {
        final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser;
        try {
            parser = parserFactory.newSAXParser();
            return parser.getXMLReader();
        } catch (final Exception e) {
            fail("Could not create XMLReader");
            return null;
        }
    }

}
