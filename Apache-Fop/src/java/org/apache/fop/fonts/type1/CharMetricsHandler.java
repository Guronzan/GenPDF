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

/* $Id$ */

package org.apache.fop.fonts.type1;

import java.io.IOException;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fonts.NamedCharacter;
import org.apache.fop.fonts.type1.AFMParser.ValueHandler;

/**
 * A handler that parses the various types of character metrics in an AFM file.
 */
@Slf4j
abstract class CharMetricsHandler {

    private static final String WHITE_SPACE = "\\s*";
    private static final String OPERATOR = "([A-Z0-9]{1,3})";
    private static final String OPERANDS = "(.*)";

    private static final Pattern METRICS_REGEX = Pattern.compile(WHITE_SPACE
            + OPERATOR + WHITE_SPACE + OPERANDS + WHITE_SPACE);
    private static final Pattern SPLIT_REGEX = Pattern.compile(WHITE_SPACE
            + ";" + WHITE_SPACE);

    private CharMetricsHandler() {
    }

    abstract AFMCharMetrics parse(final String line, final Stack<Object> stack,
            final String afmFileName) throws IOException;

    static CharMetricsHandler getHandler(
            final Map<String, ValueHandler> valueParsers, final String line) {
        if (line != null && line.contains(AdobeStandardEncoding.NAME)) {
            return new AdobeStandardCharMetricsHandler(valueParsers);
        } else {
            return new DefaultCharMetricsHandler(valueParsers);
        }
    }

    private static final class DefaultCharMetricsHandler extends
    CharMetricsHandler {
        private final Map<String, ValueHandler> valueParsers;

        private DefaultCharMetricsHandler(
                final Map<String, ValueHandler> valueParsers) {
            this.valueParsers = valueParsers;
        }

        @Override
        AFMCharMetrics parse(final String line, final Stack<Object> stack,
                final String afmFileName) throws IOException {
            final AFMCharMetrics chm = new AFMCharMetrics();
            stack.push(chm);
            final String[] metrics = SPLIT_REGEX.split(line);
            for (final String metric : metrics) {
                final Matcher matcher = METRICS_REGEX.matcher(metric);
                if (matcher.matches()) {
                    final String operator = matcher.group(1);
                    final String operands = matcher.group(2);
                    final ValueHandler handler = this.valueParsers
                            .get(operator);
                    if (handler != null) {
                        handler.parse(operands, 0, stack);
                    }
                }
            }
            stack.pop();
            return chm;
        }
    }

    private static final class AdobeStandardCharMetricsHandler extends
    CharMetricsHandler {
        private final DefaultCharMetricsHandler defaultHandler;

        private AdobeStandardCharMetricsHandler(
                final Map<String, ValueHandler> valueParsers) {
            this.defaultHandler = new DefaultCharMetricsHandler(valueParsers);
        }

        @Override
        AFMCharMetrics parse(final String line, final Stack<Object> stack,
                final String afmFileName) throws IOException {
            final AFMCharMetrics chm = this.defaultHandler.parse(line, stack,
                    afmFileName);
            final NamedCharacter namedChar = chm.getCharacter();
            if (namedChar != null) {
                final int codePoint = AdobeStandardEncoding
                        .getAdobeCodePoint(namedChar.getName());
                if (chm.getCharCode() != codePoint) {
                    log.info(afmFileName + ": named character '"
                            + namedChar.getName() + "'"
                            + " has an incorrect code point: "
                            + chm.getCharCode() + ". Changed to " + codePoint);
                    chm.setCharCode(codePoint);
                }
            }
            return chm;
        }
    }

}
