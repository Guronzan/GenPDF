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

/* $Id: PropertyTokenizer.java 1328963 2012-04-22 20:09:42Z gadams $ */

package org.apache.fop.fo.expr;

/**
 * Class to tokenize XSL FO property expression. This class is heavily based on
 * the epxression tokenizer in James Clark's XT, an XSLT processor.
 */
class PropertyTokenizer {

    static final int TOK_EOF = 0;
    static final int TOK_NCNAME = 1;
    static final int TOK_MULTIPLY = TOK_NCNAME + 1;
    static final int TOK_LPAR = TOK_MULTIPLY + 1;
    static final int TOK_RPAR = TOK_LPAR + 1;
    static final int TOK_LITERAL = TOK_RPAR + 1;
    static final int TOK_NUMBER = TOK_LITERAL + 1;
    static final int TOK_FUNCTION_LPAR = TOK_NUMBER + 1;
    static final int TOK_PLUS = TOK_FUNCTION_LPAR + 1;
    static final int TOK_MINUS = TOK_PLUS + 1;
    static final int TOK_MOD = TOK_MINUS + 1;
    static final int TOK_DIV = TOK_MOD + 1;
    static final int TOK_NUMERIC = TOK_DIV + 1;
    static final int TOK_COMMA = TOK_NUMERIC + 1;
    static final int TOK_PERCENT = TOK_COMMA + 1;
    static final int TOK_COLORSPEC = TOK_PERCENT + 1;
    static final int TOK_FLOAT = TOK_COLORSPEC + 1;
    static final int TOK_INTEGER = TOK_FLOAT + 1;

    protected int currentToken = TOK_EOF;
    protected String currentTokenValue = null;
    protected int currentUnitLength = 0;

    private int currentTokenStartIndex = 0;
    private final/* final */String expr;
    private int exprIndex = 0;
    private final int exprLength;

    /**
     * Construct a new PropertyTokenizer object to tokenize the passed String.
     * 
     * @param s
     *            The Property expressio to tokenize.
     */
    PropertyTokenizer(final String s) {
        this.expr = s;
        this.exprLength = s.length();
    }

    /**
     * Parse the next token in the expression string. This sets the following
     * package visible variables: currentToken An enumerated value identifying
     * the recognized token currentTokenValue A String containing the token
     * contents currentUnitLength If currentToken = TOK_NUMERIC, the number of
     * characters in the unit name.
     * 
     * @throws PropertyException
     *             If un unrecognized token is encountered.
     */
    void next() throws PropertyException {
        this.currentTokenValue = null;
        this.currentTokenStartIndex = this.exprIndex;
        boolean bSawDecimal;
        while (true) {
            if (this.exprIndex >= this.exprLength) {
                this.currentToken = TOK_EOF;
                return;
            }
            final char c = this.expr.charAt(this.exprIndex++);
            switch (c) {
            case ' ':
            case '\t':
            case '\r':
            case '\n':
                this.currentTokenStartIndex = this.exprIndex;
                break;
            case ',':
                this.currentToken = TOK_COMMA;
                return;
            case '+':
                this.currentToken = TOK_PLUS;
                return;
            case '-':
                this.currentToken = TOK_MINUS;
                return;
            case '(':
                this.currentToken = TOK_LPAR;
                return;
            case ')':
                this.currentToken = TOK_RPAR;
                return;
            case '"':
            case '\'':
                this.exprIndex = this.expr.indexOf(c, this.exprIndex);
                if (this.exprIndex < 0) {
                    this.exprIndex = this.currentTokenStartIndex + 1;
                    throw new PropertyException("missing quote");
                }
                this.currentTokenValue = this.expr.substring(
                        this.currentTokenStartIndex + 1, this.exprIndex++);
                this.currentToken = TOK_LITERAL;
                return;
            case '*':
                /*
                 * if (currentMaybeOperator) { recognizeOperator = false;
                 */
                this.currentToken = TOK_MULTIPLY;
                /*
                 * } else throw new PropertyException("illegal operator *");
                 */
                return;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                scanDigits();
                if (this.exprIndex < this.exprLength
                        && this.expr.charAt(this.exprIndex) == '.') {
                    this.exprIndex++;
                    bSawDecimal = true;
                    if (this.exprIndex < this.exprLength
                            && isDigit(this.expr.charAt(this.exprIndex))) {
                        this.exprIndex++;
                        scanDigits();
                    }
                } else {
                    bSawDecimal = false;
                }
                if (this.exprIndex < this.exprLength
                        && this.expr.charAt(this.exprIndex) == '%') {
                    this.exprIndex++;
                    this.currentToken = TOK_PERCENT;
                } else {
                    // Check for possible unit name following number
                    this.currentUnitLength = this.exprIndex;
                    scanName();
                    this.currentUnitLength = this.exprIndex
                            - this.currentUnitLength;
                    this.currentToken = this.currentUnitLength > 0 ? TOK_NUMERIC
                            : bSawDecimal ? TOK_FLOAT : TOK_INTEGER;
                }
                this.currentTokenValue = this.expr.substring(
                        this.currentTokenStartIndex, this.exprIndex);
                return;

            case '.':
                nextDecimalPoint();
                return;

            case '#': // Start of color value
                nextColor();
                return;

            default:
                --this.exprIndex;
                scanName();
                if (this.exprIndex == this.currentTokenStartIndex) {
                    throw new PropertyException("illegal character");
                }
                this.currentTokenValue = this.expr.substring(
                        this.currentTokenStartIndex, this.exprIndex);
                if (this.currentTokenValue.equals("mod")) {
                    this.currentToken = TOK_MOD;
                    return;
                } else if (this.currentTokenValue.equals("div")) {
                    this.currentToken = TOK_DIV;
                    return;
                }
                if (followingParen()) {
                    this.currentToken = TOK_FUNCTION_LPAR;
                } else {
                    this.currentToken = TOK_NCNAME;
                }
                return;
            }
        }
    }

    private void nextDecimalPoint() throws PropertyException {
        if (this.exprIndex < this.exprLength
                && isDigit(this.expr.charAt(this.exprIndex))) {
            ++this.exprIndex;
            scanDigits();
            if (this.exprIndex < this.exprLength
                    && this.expr.charAt(this.exprIndex) == '%') {
                this.exprIndex++;
                this.currentToken = TOK_PERCENT;
            } else {
                // Check for possible unit name following number
                this.currentUnitLength = this.exprIndex;
                scanName();
                this.currentUnitLength = this.exprIndex
                        - this.currentUnitLength;
                this.currentToken = this.currentUnitLength > 0 ? TOK_NUMERIC
                        : TOK_FLOAT;
            }
            this.currentTokenValue = this.expr.substring(
                    this.currentTokenStartIndex, this.exprIndex);
            return;
        }
        throw new PropertyException("illegal character '.'");
    }

    private void nextColor() throws PropertyException {
        if (this.exprIndex < this.exprLength) {
            ++this.exprIndex;
            scanHexDigits();
            final int len = this.exprIndex - this.currentTokenStartIndex - 1;
            if (len % 3 == 0) {
                this.currentToken = TOK_COLORSPEC;
            } else {
                // Actually not a color at all, but an NCNAME starting with "#"
                scanRestOfName();
                this.currentToken = TOK_NCNAME;
            }
            this.currentTokenValue = this.expr.substring(
                    this.currentTokenStartIndex, this.exprIndex);
            return;
        } else {
            throw new PropertyException("illegal character '#'");
        }
    }

    /**
     * Attempt to recognize a valid NAME token in the input expression.
     */
    private void scanName() {
        if (this.exprIndex < this.exprLength
                && isNameStartChar(this.expr.charAt(this.exprIndex))) {
            scanRestOfName();
        }
    }

    private void scanRestOfName() {
        while (++this.exprIndex < this.exprLength) {
            if (!isNameChar(this.expr.charAt(this.exprIndex))) {
                break;
            }
        }
    }

    /**
     * Attempt to recognize a valid sequence of decimal DIGITS in the input
     * expression.
     */
    private void scanDigits() {
        while (this.exprIndex < this.exprLength
                && isDigit(this.expr.charAt(this.exprIndex))) {
            this.exprIndex++;
        }
    }

    /**
     * Attempt to recognize a valid sequence of hexadecimal DIGITS in the input
     * expression.
     */
    private void scanHexDigits() {
        while (this.exprIndex < this.exprLength
                && isHexDigit(this.expr.charAt(this.exprIndex))) {
            this.exprIndex++;
        }
    }

    /**
     * Return a boolean value indicating whether the following non-whitespace
     * character is an opening parenthesis.
     */
    private boolean followingParen() {
        for (int i = this.exprIndex; i < this.exprLength; i++) {
            switch (this.expr.charAt(i)) {
            case '(':
                this.exprIndex = i + 1;
                return true;
            case ' ':
            case '\r':
            case '\n':
            case '\t':
                break;
            default:
                return false;
            }
        }
        return false;
    }

    private static final String NAME_START_CHARS = "_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NAME_CHARS = ".-0123456789";
    private static final String DIGITS = "0123456789";
    private static final String HEX_CHARS = DIGITS + "abcdefABCDEF";

    /**
     * Return a boolean value indicating whether the argument is a decimal digit
     * (0-9).
     * 
     * @param c
     *            The character to check
     */
    private static boolean isDigit(final char c) {
        return DIGITS.indexOf(c) >= 0;
    }

    /**
     * Return a boolean value indicating whether the argument is a hexadecimal
     * digit (0-9, A-F, a-f).
     * 
     * @param c
     *            The character to check
     */
    private static boolean isHexDigit(final char c) {
        return HEX_CHARS.indexOf(c) >= 0;
    }

    /**
     * Return a boolean value indicating whether the argument is whitespace as
     * defined by XSL (space, newline, CR, tab).
     * 
     * @param c
     *            The character to check
     */
    private static boolean isSpace(final char c) {
        switch (c) {
        case ' ':
        case '\r':
        case '\n':
        case '\t':
            return true;
        default:
            return false;
        }
    }

    /**
     * Return a boolean value indicating whether the argument is a valid name
     * start character, ie. can start a NAME as defined by XSL.
     * 
     * @param c
     *            The character to check
     */
    private static boolean isNameStartChar(final char c) {
        return NAME_START_CHARS.indexOf(c) >= 0 || c >= 0x80;
    }

    /**
     * Return a boolean value indicating whether the argument is a valid name
     * character, ie. can occur in a NAME as defined by XSL.
     * 
     * @param c
     *            The character to check
     */
    private static boolean isNameChar(final char c) {
        return NAME_START_CHARS.indexOf(c) >= 0 || NAME_CHARS.indexOf(c) >= 0
                || c >= 0x80;
    }

}
