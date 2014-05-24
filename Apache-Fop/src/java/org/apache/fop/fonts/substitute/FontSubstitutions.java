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

/* $Id: FontSubstitutions.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.fonts.substitute;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;

/**
 * Font substitutions
 */
public class FontSubstitutions extends java.util.ArrayList<FontSubstitution> {

    private static final long serialVersionUID = -9173104935431899722L;

    /** logging instance */
    protected static final Log log = LogFactory.getLog(FontSubstitutions.class);

    /**
     * Adjusts a given fontInfo using this font substitution catalog
     * 
     * @param fontInfo
     *            font info
     */
    public void adjustFontInfo(final FontInfo fontInfo) {
        for (final Iterator<FontSubstitution> subsIt = super.iterator(); subsIt
                .hasNext();) {
            final FontSubstitution substitution = subsIt.next();

            // find the best matching font triplet
            final FontQualifier toQualifier = substitution.getToQualifier();
            final FontTriplet fontTriplet = toQualifier.bestMatch(fontInfo);
            if (fontTriplet == null) {
                log.error("Unable to match font substitution for destination qualifier "
                        + toQualifier);
                continue;
            }
            final String internalFontKey = fontInfo
                    .getInternalFontKey(fontTriplet);

            final FontQualifier fromQualifier = substitution.getFromQualifier();
            final List<FontTriplet> tripletList = fromQualifier.getTriplets();
            for (final Object element : tripletList) {
                final FontTriplet triplet = (FontTriplet) element;
                fontInfo.addFontProperties(internalFontKey, triplet);
            }
        }
    }
}
