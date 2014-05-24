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

/* $Id: AdvancedMessageFormat.java 1058988 2011-01-14 12:58:53Z spepping $ */

package org.apache.fop.util.text;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.xmlgraphics.util.Service;

/**
 * Formats messages based on a template and with a set of named parameters. This
 * is similar to {@link java.text.MessageFormat} but uses named parameters and
 * supports conditional sub-groups.
 * <p>
 * Example:
 * </p>
 * <p>
 * <code>Missing field "{fieldName}"[ at location: {location}]!</code>
 * </p>
 * <ul>
 * <li>Curly brackets ("{}") are used for fields.</li>
 * <li>Square brackets ("[]") are used to delimit conditional sub-groups. A
 * sub-group is conditional when all fields inside the sub-group have a null
 * value. In the case, everything between the brackets is skipped.</li>
 * </ul>
 */
public class AdvancedMessageFormat {

    /** Regex that matches "," but not "\," (escaped comma) */
    static final Pattern COMMA_SEPARATOR_REGEX = Pattern.compile("(?<!\\\\),");

    private static final Map<String, PartFactory> PART_FACTORIES = new java.util.HashMap<String, PartFactory>();
    private static final List<ObjectFormatter> OBJECT_FORMATTERS = new java.util.ArrayList<ObjectFormatter>();
    private static final Map<Object, Function> FUNCTIONS = new java.util.HashMap<Object, Function>();

    private CompositePart rootPart;

    static {
        final Iterator<PartFactory> iterPartFactory = Service
                .providers(PartFactory.class);
        while (iterPartFactory.hasNext()) {
            final PartFactory factory = iterPartFactory.next();
            PART_FACTORIES.put(factory.getFormat(), factory);
        }
        final Iterator<ObjectFormatter> iterObjectFormatter = Service
                .providers(ObjectFormatter.class);
        while (iterObjectFormatter.hasNext()) {
            OBJECT_FORMATTERS.add(iterObjectFormatter.next());
        }
        final Iterator<Function> iterFunction = Service
                .providers(Function.class);
        while (iterFunction.hasNext()) {
            final Function function = iterFunction.next();
            FUNCTIONS.put(function.getName(), function);
        }
    }

    /**
     * Construct a new message format.
     *
     * @param pattern
     *            the message format pattern.
     */
    public AdvancedMessageFormat(final CharSequence pattern) {
        parsePattern(pattern);
    }

    private void parsePattern(final CharSequence pattern) {
        this.rootPart = new CompositePart(false);
        final StringBuffer sb = new StringBuffer();
        parseInnerPattern(pattern, this.rootPart, sb, 0);
    }

    private int parseInnerPattern(final CharSequence pattern,
            final CompositePart parent, final StringBuffer sb, final int start) {
        assert sb.length() == 0;
        int i = start;
        final int len = pattern.length();
        loop: while (i < len) {
            char ch = pattern.charAt(i);
            switch (ch) {
            case '{':
                if (sb.length() > 0) {
                    parent.addChild(new TextPart(sb.toString()));
                    sb.setLength(0);
                }
                i++;
                int nesting = 1;
                while (i < len) {
                    ch = pattern.charAt(i);
                    if (ch == '{') {
                        nesting++;
                    } else if (ch == '}') {
                        nesting--;
                        if (nesting == 0) {
                            i++;
                            break;
                        }
                    }
                    sb.append(ch);
                    i++;
                }
                parent.addChild(parseField(sb.toString()));
                sb.setLength(0);
                break;
            case ']':
                i++;
                break loop; // Current composite is finished
            case '[':
                if (sb.length() > 0) {
                    parent.addChild(new TextPart(sb.toString()));
                    sb.setLength(0);
                }
                i++;
                final CompositePart composite = new CompositePart(true);
                parent.addChild(composite);
                i += parseInnerPattern(pattern, composite, sb, i);
                break;
            case '|':
                if (sb.length() > 0) {
                    parent.addChild(new TextPart(sb.toString()));
                    sb.setLength(0);
                }
                parent.newSection();
                i++;
                break;
            case '\\':
                if (i < len - 1) {
                    i++;
                    ch = pattern.charAt(i);
                }
                // no break here! Must be right before "default" section
            default:
                sb.append(ch);
                i++;
            }
        }
        if (sb.length() > 0) {
            parent.addChild(new TextPart(sb.toString()));
            sb.setLength(0);
        }
        return i - start;
    }

    private Part parseField(final String field) {
        final String[] parts = COMMA_SEPARATOR_REGEX.split(field, 3);
        final String fieldName = parts[0];
        if (parts.length == 1) {
            if (fieldName.startsWith("#")) {
                return new FunctionPart(fieldName.substring(1));
            } else {
                return new SimpleFieldPart(fieldName);
            }
        } else {
            final String format = parts[1];
            final PartFactory factory = PART_FACTORIES.get(format);
            if (factory == null) {
                throw new IllegalArgumentException(
                        "No PartFactory available under the name: " + format);
            }
            if (parts.length == 2) {
                return factory.newPart(fieldName, null);
            } else {
                return factory.newPart(fieldName, parts[2]);
            }
        }
    }

    private static Function getFunction(final String functionName) {
        return FUNCTIONS.get(functionName);
    }

    /**
     * Formats a message with the given parameters.
     *
     * @param params
     *            a Map of named parameters (Contents: <String, Object>)
     * @return the formatted message
     */
    public String format(final Map<String, Object> params) {
        final StringBuffer sb = new StringBuffer();
        format(params, sb);
        return sb.toString();
    }

    /**
     * Formats a message with the given parameters.
     *
     * @param params
     *            a Map of named parameters (Contents: <String, Object>)
     * @param target
     *            the target StringBuffer to write the formatted message to
     */
    public void format(final Map<String, Object> params,
            final StringBuffer target) {
        this.rootPart.write(target, params);
    }

    /**
     * Represents a message template part. This interface is implemented by
     * various variants of the single curly braces pattern ({field},
     * {field,if,yes,no} etc.).
     */
    public interface Part {

        /**
         * Writes the formatted part to a string buffer.
         *
         * @param sb
         *            the target string buffer
         * @param params
         *            the parameters to work with
         */
        void write(final StringBuffer sb, final Map<String, Object> params);

        /**
         * Indicates whether there is any content that is generated by this
         * message part.
         *
         * @param params
         *            the parameters to work with
         * @return true if the part has content
         */
        boolean isGenerated(final Map<String, Object> params);
    }

    /**
     * Implementations of this interface parse a field part and return message
     * parts.
     */
    public interface PartFactory {

        /**
         * Creates a new part by parsing the values parameter to configure the
         * part.
         *
         * @param fieldName
         *            the field name
         * @param values
         *            the unparsed parameter values
         * @return the new message part
         */
        Part newPart(final String fieldName, final String values);

        /**
         * Returns the name of the message part format.
         *
         * @return the name of the message part format
         */
        String getFormat();
    }

    /**
     * Implementations of this interface format certain objects to strings.
     */
    public interface ObjectFormatter {

        /**
         * Formats an object to a string and writes the result to a string
         * buffer.
         *
         * @param sb
         *            the target string buffer
         * @param obj
         *            the object to be formatted
         */
        void format(final StringBuffer sb, final Object obj);

        /**
         * Indicates whether a given object is supported.
         *
         * @param obj
         *            the object
         * @return true if the object is supported by the formatter
         */
        boolean supportsObject(final Object obj);
    }

    /**
     * Implementations of this interface do some computation based on the
     * message parameters given to it. Note: at the moment, this has to be done
     * in a local-independent way since there is no locale information.
     */
    public interface Function {

        /**
         * Executes the function.
         *
         * @param params
         *            the message parameters
         * @return the function result
         */
        Object evaluate(final Map<String, Object> params);

        /**
         * Returns the name of the function.
         *
         * @return the name of the function
         */
        Object getName();
    }

    private static class TextPart implements Part {

        private final String text;

        public TextPart(final String text) {
            this.text = text;
        }

        @Override
        public void write(final StringBuffer sb,
                final Map<String, Object> params) {
            sb.append(this.text);
        }

        @Override
        public boolean isGenerated(final Map<String, Object> params) {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return this.text;
        }
    }

    private static class SimpleFieldPart implements Part {

        private final String fieldName;

        public SimpleFieldPart(final String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public void write(final StringBuffer sb,
                final Map<String, Object> params) {
            if (!params.containsKey(this.fieldName)) {
                throw new IllegalArgumentException(
                        "Message pattern contains unsupported field name: "
                                + this.fieldName);
            }
            final Object obj = params.get(this.fieldName);
            formatObject(obj, sb);
        }

        @Override
        public boolean isGenerated(final Map<String, Object> params) {
            final Object obj = params.get(this.fieldName);
            return obj != null;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "{" + this.fieldName + "}";
        }
    }

    /**
     * Formats an object to a string and writes the result to a string buffer.
     * This method usually uses the object's <code>toString()</code> method
     * unless there is an {@link ObjectFormatter} that supports the object.
     * {@link ObjectFormatter}s are registered through the service provider
     * mechanism defined by the JAR specification.
     *
     * @param obj
     *            the object to be formatted
     * @param target
     *            the target string buffer
     */
    public static void formatObject(final Object obj, final StringBuffer target) {
        if (obj instanceof String) {
            target.append(obj);
        } else {
            boolean handled = false;
            final Iterator<ObjectFormatter> iter = OBJECT_FORMATTERS.iterator();
            while (iter.hasNext()) {
                final ObjectFormatter formatter = iter.next();
                if (formatter.supportsObject(obj)) {
                    formatter.format(target, obj);
                    handled = true;
                    break;
                }
            }
            if (!handled) {
                target.append(String.valueOf(obj));
            }
        }
    }

    private static class FunctionPart implements Part {

        private final Function function;

        public FunctionPart(final String functionName) {
            this.function = getFunction(functionName);
            if (this.function == null) {
                throw new IllegalArgumentException("Unknown function: "
                        + functionName);
            }
        }

        @Override
        public void write(final StringBuffer sb,
                final Map<String, Object> params) {
            final Object obj = this.function.evaluate(params);
            formatObject(obj, sb);
        }

        @Override
        public boolean isGenerated(final Map<String, Object> params) {
            final Object obj = this.function.evaluate(params);
            return obj != null;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "{#" + this.function.getName() + "}";
        }
    }

    private static class CompositePart implements Part {

        protected List<Part> parts = new java.util.ArrayList<Part>();
        private final boolean conditional;
        private boolean hasSections = false;

        public CompositePart(final boolean conditional) {
            this.conditional = conditional;
        }

        private CompositePart(final List<Part> parts) {
            this.parts.addAll(parts);
            this.conditional = true;
        }

        public void addChild(final Part part) {
            if (part == null) {
                throw new NullPointerException("part must not be null");
            }
            if (this.hasSections) {
                final CompositePart composite = (CompositePart) this.parts
                        .get(this.parts.size() - 1);
                composite.addChild(part);
            } else {
                this.parts.add(part);
            }
        }

        public void newSection() {
            if (!this.hasSections) {
                final List<Part> p = this.parts;
                // Dropping into a different mode...
                this.parts = new java.util.ArrayList<Part>();
                this.parts.add(new CompositePart(p));
                this.hasSections = true;
            }
            this.parts.add(new CompositePart(true));
        }

        @Override
        public void write(final StringBuffer sb,
                final Map<String, Object> params) {
            if (this.hasSections) {
                final Iterator<Part> iter = this.parts.iterator();
                while (iter.hasNext()) {
                    final Part part = iter.next();
                    if (part.isGenerated(params)) {
                        part.write(sb, params);
                        break;
                    }
                }
            } else {
                if (isGenerated(params)) {
                    final Iterator<Part> iter = this.parts.iterator();
                    while (iter.hasNext()) {
                        final Part part = iter.next();
                        part.write(sb, params);
                    }
                }
            }
        }

        @Override
        public boolean isGenerated(final Map<String, Object> params) {
            if (this.hasSections) {
                final Iterator<Part> iter = this.parts.iterator();
                while (iter.hasNext()) {
                    final Part part = iter.next();
                    if (part.isGenerated(params)) {
                        return true;
                    }
                }
                return false;
            } else {
                if (this.conditional) {
                    final Iterator<Part> iter = this.parts.iterator();
                    while (iter.hasNext()) {
                        final Part part = iter.next();
                        if (!part.isGenerated(params)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return this.parts.toString();
        }
    }

    static String unescapeComma(final String string) {
        return string.replaceAll("\\\\,", ",");
    }
}
