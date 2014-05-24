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

/* $Id: AbstractNamedAFPObject.java 1151452 2011-07-27 12:50:12Z phancock $ */

package org.apache.fop.afp.modca;

import java.io.UnsupportedEncodingException;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.AFPConstants;

/**
 * This is the base class for all named data stream objects. A named data stream
 * object has an 8 byte EBCIDIC name.
 */
@Slf4j
public abstract class AbstractNamedAFPObject extends
AbstractTripletStructuredObject {

    private static final int DEFAULT_NAME_LENGTH = 8;

    /**
     * The actual name of the object
     */
    protected String name = null;

    /**
     * Default constructor
     */
    protected AbstractNamedAFPObject() {
    }

    /**
     * Constructor for the ActiveEnvironmentGroup, this takes a name parameter
     * which should be 8 characters long.
     *
     * @param name
     *            the object name
     */
    protected AbstractNamedAFPObject(final String name) {
        this.name = name;
    }

    /**
     * Returns the name length
     *
     * @return the name length
     */
    protected int getNameLength() {
        return DEFAULT_NAME_LENGTH;
    }

    /**
     * Returns the name as a byte array in EBCIDIC encoding
     *
     * @return the name as a byte array in EBCIDIC encoding
     */
    public byte[] getNameBytes() {
        final int afpNameLen = getNameLength();
        final int nameLen = this.name.length();
        if (nameLen < afpNameLen) {
            this.name = (this.name + "       ").substring(0, afpNameLen);
        } else if (this.name.length() > afpNameLen) {
            final String truncatedName = this.name.substring(nameLen
                    - afpNameLen, nameLen);
            log.warn("Constructor:: name '" + this.name + "'"
                    + " truncated to " + afpNameLen + " chars" + " ('"
                    + truncatedName + "')");
            this.name = truncatedName;
        }
        byte[] nameBytes = null;
        try {
            nameBytes = this.name.getBytes(AFPConstants.EBCIDIC_ENCODING);
        } catch (final UnsupportedEncodingException usee) {
            nameBytes = this.name.getBytes();
            log.error(
                    "Constructor:: UnsupportedEncodingException translating the name "
                            + this.name, usee);
        }
        return nameBytes;
    }

    @Override
    protected void copySF(final byte[] data, final byte type,
            final byte category) {
        super.copySF(data, type, category);
        final byte[] nameData = getNameBytes();
        System.arraycopy(nameData, 0, data, 9, nameData.length);
    }

    /**
     * Returns the name of this object
     *
     * @return the name of this object
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of this object
     *
     * @param name
     *            the object name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getName();
    }
}
