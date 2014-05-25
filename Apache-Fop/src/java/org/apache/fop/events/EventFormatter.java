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

/* $Id: EventFormatter.java 1324913 2012-04-11 18:43:46Z gadams $ */

package org.apache.fop.events;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.util.XMLResourceBundle;
import org.apache.fop.util.text.AdvancedMessageFormat;
import org.apache.fop.util.text.AdvancedMessageFormat.Part;
import org.apache.fop.util.text.AdvancedMessageFormat.PartFactory;

/**
 * Converts events into human-readable, localized messages.
 */
@Slf4j
public final class EventFormatter {

    private static final Pattern INCLUDES_PATTERN = Pattern
            .compile("\\{\\{.+\\}\\}");

    private EventFormatter() {
        // utility class
    }

    private static ResourceBundle getBundle(final String groupID,
            final Locale locale) {
        ResourceBundle bundle;
        final String baseName = groupID != null ? groupID
                : EventFormatter.class.getName();
        try {
            final ClassLoader classLoader = EventFormatter.class
                    .getClassLoader();
            bundle = XMLResourceBundle.getXMLBundle(baseName, locale,
                    classLoader);
        } catch (final MissingResourceException e) {
            if (log.isTraceEnabled()) {
                log.trace("No XMLResourceBundle for " + baseName
                        + " available.");
            }
            bundle = null;
        }
        return bundle;
    }

    /**
     * Formats an event using the default locale.
     *
     * @param event
     *            the event
     * @return the formatted message
     */
    public static String format(final Event event) {
        return format(event, event.getLocale());
    }

    /**
     * Formats an event using a given locale.
     *
     * @param event
     *            the event
     * @param locale
     *            the locale
     * @return the formatted message
     */
    public static String format(final Event event, final Locale locale) {
        return format(event, getBundle(event.getEventGroupID(), locale));
    }

    private static String format(final Event event, final ResourceBundle bundle) {
        assert event != null;
        final String key = event.getEventKey();
        String template;
        if (bundle != null) {
            template = bundle.getString(key);
        } else {
            template = "Missing bundle. Can't lookup event key: '" + key + "'.";
        }
        return format(event, processIncludes(template, bundle));
    }

    private static String processIncludes(final String template,
            final ResourceBundle bundle) {
        CharSequence input = template;
        int replacements;
        StringBuffer sb;
        do {
            sb = new StringBuffer(Math.max(16, input.length()));
            replacements = processIncludesInner(input, sb, bundle);
            input = sb;
        } while (replacements > 0);
        final String s = sb.toString();
        return s;
    }

    private static int processIncludesInner(final CharSequence template,
            final StringBuffer sb, final ResourceBundle bundle) {
        int replacements = 0;
        if (bundle != null) {
            final Matcher m = INCLUDES_PATTERN.matcher(template);
            while (m.find()) {
                String include = m.group();
                include = include.substring(2, include.length() - 2);
                m.appendReplacement(sb, bundle.getString(include));
                replacements++;
            }
            m.appendTail(sb);
        }
        return replacements;
    }

    /**
     * Formats the event using a given pattern. The pattern needs to be
     * compatible with {@link AdvancedMessageFormat}.
     *
     * @param event
     *            the event
     * @param pattern
     *            the pattern (compatible with {@link AdvancedMessageFormat})
     * @return the formatted message
     */
    public static String format(final Event event, final String pattern) {
        final AdvancedMessageFormat format = new AdvancedMessageFormat(pattern);
        final Map params = new java.util.HashMap(event.getParams());
        params.put("source", event.getSource());
        params.put("severity", event.getSeverity());
        params.put("groupID", event.getEventGroupID());
        params.put("locale", event.getLocale());
        return format.format(params);
    }

    private static class LookupFieldPart implements Part {

        private final String fieldName;

        public LookupFieldPart(final String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public boolean isGenerated(final Map params) {
            return getKey(params) != null;
        }

        @Override
        public void write(final StringBuilder sb, final Map params) {
            final String groupID = (String) params.get("groupID");
            final Locale locale = (Locale) params.get("locale");
            final ResourceBundle bundle = getBundle(groupID, locale);
            if (bundle != null) {
                sb.append(bundle.getString(getKey(params)));
            }
        }

        private String getKey(final Map params) {
            return (String) params.get(this.fieldName);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "{" + this.fieldName + ", lookup}";
        }

    }

    /** PartFactory for lookups. */
    public static class LookupFieldPartFactory implements PartFactory {

        /** {@inheritDoc} */
        @Override
        public Part newPart(final String fieldName, final String values) {
            return new LookupFieldPart(fieldName);
        }

        /** {@inheritDoc} */
        @Override
        public String getFormat() {
            return "lookup";
        }

    }

}
