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

/* $Id: AFPFont.java 1311638 2012-04-10 08:39:31Z mehdi $ */

package org.apache.fop.afp.fonts;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.fop.fonts.FontType;
import org.apache.fop.fonts.Typeface;

/**
 * All implementations of AFP fonts should extend this base class, the object
 * implements the FontMetrics information.
 * <p/>
 */
public abstract class AFPFont extends Typeface {

    /** The font name */
    protected String name;

    private boolean embeddable = true;

    /**
     * Constructor for the base font requires the name.
     * 
     * @param name
     *            the name of the font
     */
    public AFPFont(final String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String getFontName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public String getEmbedFontName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public String getFullName() {
        return getFontName();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getFamilyNames() {
        final Set<String> s = new HashSet<String>();
        s.add(this.name);
        return s;
    }

    /**
     * Returns the type of the font.
     * 
     * @return the font type
     */
    @Override
    public FontType getFontType() {
        return FontType.OTHER;
    }

    /**
     * Indicates if the font has kerning information.
     * 
     * @return True, if kerning is available.
     */
    @Override
    public boolean hasKerningInfo() {
        return false;
    }

    /**
     * Returns the kerning map for the font.
     * 
     * @return the kerning map
     */
    @Override
    public Map getKerningInfo() {
        return null;
    }

    /**
     * Returns the character set for a given size
     * 
     * @param size
     *            the font size
     * @return the character set object
     */
    public abstract CharacterSet getCharacterSet(final int size);

    /**
     * Controls whether this font is embeddable or not.
     * 
     * @param value
     *            true to enable embedding, false otherwise.
     */
    public void setEmbeddable(final boolean value) {
        this.embeddable = value;
    }

    /**
     * Indicates if this font may be embedded.
     * 
     * @return True, if embedding is possible/permitted
     */
    public boolean isEmbeddable() {
        return this.embeddable;
    }

    /**
     * Maps mapped code points to Unicode code points.
     * 
     * @param character
     *            the mapped code point
     * @return the corresponding Unicode code point
     */
    protected static final char toUnicodeCodepoint(final int character) {
        // AFP fonts use Unicode directly as their mapped code points, so we can
        // simply cast to char
        return (char) character;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "name=" + this.name;
    }
}
