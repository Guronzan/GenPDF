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

/* $Id: DigestFilter.java 985571 2010-08-14 19:28:26Z jeremias $ */

package org.apache.fop.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * SAX filter which produces a digest over the XML elements. Insignificant
 * whitespace as determined by the, comments and processing instructions are not
 * part of the digest. If the filter is namespace aware, the URI and local name
 * for each element is digested, otherwise the QName. Attributes are sorted
 * before the name-value pairs are digested.
 *
 */
public class DigestFilter extends XMLFilterImpl {

    private final MessageDigest digest;
    private byte[] value;
    private boolean isNamespaceAware;

    public DigestFilter(final String algorithm) throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance(algorithm);
    }

    public byte[] getDigestValue() {
        return this.value;
    }

    public String getDigestString() {
        if (this.value != null) {
            final StringBuilder buffer = new StringBuilder(2 * this.value.length);
            for (int i = 0; i < this.value.length; i++) {
                final int val = this.value[i];
                final int hi = val >> 4 & 0xF;
            final int lo = val & 0xF;
            if (hi < 10) {
                buffer.append((char) (hi + 0x30));
            } else {
                buffer.append((char) (hi + 0x61 - 10));
            }
            if (lo < 10) {
                buffer.append((char) (lo + 0x30));
            } else {
                buffer.append((char) (lo + 0x61 - 10));
            }
            }
            return buffer.toString();
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
    public void characters(final char[] chars, final int start, final int length)
            throws SAXException {
        this.digest.update(new String(chars, start, length).getBytes());
        super.characters(chars, start, length);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException {
        this.value = this.digest.digest();
        super.endDocument();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
     * java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String url, final String localName,
            final String qName, final Attributes attr) throws SAXException {
        final Map map = new TreeMap();
        if (this.isNamespaceAware) {
            this.digest.update(url.getBytes());
            this.digest.update(localName.getBytes());
            for (int i = 0; i < attr.getLength(); i++) {
                map.put(attr.getLocalName(i) + attr.getURI(i), attr.getValue(i));
            }
        } else {
            this.digest.update(qName.getBytes());
            for (int i = 0; i < attr.getLength(); i++) {
                map.put(attr.getQName(i), attr.getValue(i));
            }
        }
        for (final Iterator i = map.entrySet().iterator(); i.hasNext();) {
            final Map.Entry entry = (Map.Entry) i.next();
            this.digest.update(((String) entry.getKey()).getBytes());
            this.digest.update(((String) entry.getValue()).getBytes());
        }
        super.startElement(url, localName, qName, attr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    @Override
    public void setFeature(final String feature, final boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (feature.equals("http://xml.org/sax/features/namespaces")) {
            this.isNamespaceAware = value;
        }
        super.setFeature(feature, value);
    }

}
