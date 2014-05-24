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

/* $Id: FontSubstitutionsConfigurator.java 1296526 2012-03-03 00:18:45Z gadams $ */

package org.apache.fop.fonts.substitute;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.fop.apps.FOPException;

/**
 * Configures a font substitution catalog
 */
public class FontSubstitutionsConfigurator {

    private Configuration cfg = null;

    /**
     * Main constructor
     *
     * @param cfg
     *            a configuration
     */
    public FontSubstitutionsConfigurator(final Configuration cfg) {
        this.cfg = cfg;
    }

    private static FontQualifier getQualfierFromConfiguration(
            final Configuration cfg) throws FOPException {
        final String fontFamily = cfg.getAttribute("font-family", null);
        if (fontFamily == null) {
            throw new FOPException(
                    "substitution qualifier must have a font-family");
        }
        final FontQualifier qualifier = new FontQualifier();
        qualifier.setFontFamily(fontFamily);
        final String fontWeight = cfg.getAttribute("font-weight", null);
        if (fontWeight != null) {
            qualifier.setFontWeight(fontWeight);
        }
        final String fontStyle = cfg.getAttribute("font-style", null);
        if (fontStyle != null) {
            qualifier.setFontStyle(fontStyle);
        }
        return qualifier;
    }

    /**
     * Configures a font substitution catalog
     *
     * @param substitutions
     *            font substitutions
     * @throws FOPException
     *             if something's wrong with the config data
     */
    public void configure(final FontSubstitutions substitutions)
            throws FOPException {
        final Configuration[] substitutionCfgs = this.cfg
                .getChildren("substitution");
        for (final Configuration substitutionCfg : substitutionCfgs) {
            final Configuration fromCfg = substitutionCfg.getChild("from",
                    false);
            if (fromCfg == null) {
                throw new FOPException(
                        "'substitution' element without child 'from' element");
            }
            final Configuration toCfg = substitutionCfg.getChild("to", false);
            final FontQualifier fromQualifier = getQualfierFromConfiguration(fromCfg);
            final FontQualifier toQualifier = getQualfierFromConfiguration(toCfg);
            final FontSubstitution substitution = new FontSubstitution(
                    fromQualifier, toQualifier);
            substitutions.add(substitution);
        }
    }
}
