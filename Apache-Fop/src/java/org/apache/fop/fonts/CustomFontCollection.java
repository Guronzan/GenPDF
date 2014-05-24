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

/* $Id: CustomFontCollection.java 1357883 2012-07-05 20:29:53Z gadams $ */

package org.apache.fop.fonts;

import java.util.List;

/**
 * Sets up a set of custom (embedded) fonts
 */
public class CustomFontCollection implements FontCollection {

    private FontResolver fontResolver;
    private final List<EmbedFontInfo> embedFontInfoList;

    /**
     * Main constructor.
     * 
     * @param fontResolver
     *            a font resolver
     * @param customFonts
     *            the list of custom fonts
     * @param useComplexScriptFeatures
     *            true if complex script features enabled
     */
    public CustomFontCollection(final FontResolver fontResolver,
            final List<EmbedFontInfo> customFonts,
            final boolean useComplexScriptFeatures) {
        this.fontResolver = fontResolver;
        if (this.fontResolver == null) {
            // Ensure that we have minimal font resolution capabilities
            this.fontResolver = FontManager
                    .createMinimalFontResolver(useComplexScriptFeatures);
        }
        this.embedFontInfoList = customFonts;
    }

    /** {@inheritDoc} */
    @Override
    public int setup(int num, final FontInfo fontInfo) {
        if (this.embedFontInfoList == null) {
            return num; // No fonts to process
        }

        String internalName = null;
        // FontReader reader = null;

        for (int i = 0; i < this.embedFontInfoList.size(); i++) {
            final EmbedFontInfo embedFontInfo = this.embedFontInfoList.get(i);

            // String metricsFile = configFontInfo.getMetricsFile();
            internalName = "F" + num;
            num++;
            /*
             * reader = new FontReader(metricsFile);
             * reader.useKerning(configFontInfo.getKerning());
             * reader.setFontEmbedPath(configFontInfo.getEmbedFile());
             * fontInfo.addMetrics(internalName, reader.getFont());
             */

            final LazyFont font = new LazyFont(embedFontInfo, this.fontResolver);
            fontInfo.addMetrics(internalName, font);

            final List<FontTriplet> triplets = embedFontInfo.getFontTriplets();
            for (int tripletIndex = 0; tripletIndex < triplets.size(); tripletIndex++) {
                final FontTriplet triplet = triplets.get(tripletIndex);
                fontInfo.addFontProperties(internalName, triplet);
            }
        }
        return num;
    }
}
