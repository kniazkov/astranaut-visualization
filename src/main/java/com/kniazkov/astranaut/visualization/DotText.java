/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Ivan Kniazkov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.kniazkov.astranaut.visualization;

/**
 * DOT attribute and HTML-label encoding.
 * @since 0.0.1
 */
final class DotText {
    /**
     * Maximum displayed data length in UTF-16 units.
     */
    private static final int MAX_DATA_LENGTH = 64;

    /**
     * Utility.
     */
    private DotText() { }

    /**
     * Truncates the given text to a maximum of {@code MAX_DATA_LENGTH} characters.
     *  If the text exceeds {@code MAX_DATA_LENGTH}  characters, it is cut at the last space
     *  character within the first {@code MAX_DATA_LENGTH}  characters. If no space is found,
     *  it is cut strictly at the {@code MAX_DATA_LENGTH}  character. An ellipsis ("...")
     *  is appended to indicate truncation.
     * @param text The input string to truncate, must not be {@code null}
     * @return The truncated string with an ellipsis if needed,
     *  or the original string if it's {@code MAX_DATA_LENGTH}  characters or less
     */
    static String truncate(final String text) {
        String result = text;
        do {
            if (text.length() <= DotText.MAX_DATA_LENGTH) {
                break;
            }
            final int space = text.lastIndexOf(' ', DotText.MAX_DATA_LENGTH);
            int cut = space;
            if (space < 0) {
                cut = DotText.MAX_DATA_LENGTH;
            }
            if (cut > 0 && Character.isHighSurrogate(text.charAt(cut - 1))) {
                cut -= 1;
            }
            result = text.substring(0, cut).concat("...");
        } while (false);
        return result;
    }

    /**
     * Encodes text into an HTML-compatible format replacing characters,
     *  which are not accepted in HTML, with corresponding HTML escape symbols.
     * @param str Text to be encoded in HTML
     * @return An encoded text
     */
    static String encodeHtml(final String str) {
        final StringBuilder result = new StringBuilder(str.length());
        for (int index = 0; index < str.length(); index += 1) {
            result.append(DotText.symbol(str.charAt(index)));
        }
        return result.toString();
    }

    /**
     * Quotes an attribute value without permitting DOT statements.
     * @param value Attribute text
     * @return Escaped content of a quoted string
     */
    static String quote(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\r", "\\r").replace("\n", "\\n");
    }

    /**
     * Escapes one HTML-label symbol.
     * @param value Symbol
     * @return Safe text
     */
    private static String symbol(final char value) {
        return switch (value) {
            case '\\' -> "&#92;";
            case '<' -> "&lt;";
            case '>' -> "&gt;";
            case '\'' -> "&apos;";
            case '"' -> "&quot;";
            case '&' -> "&amp;";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> DotText.printable(value);
        };
    }

    /**
     * Replaces control characters that are illegal in XML labels.
     * @param value Symbol
     * @return Printable text
     */
    private static String printable(final char value) {
        final String result;
        if (Character.isISOControl(value)) {
            result = String.format("[U+%04X]", (int) value);
        } else {
            result = Character.toString(value);
        }
        return result;
    }

}
