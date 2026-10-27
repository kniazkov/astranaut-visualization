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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Literal text cannot become DOT attributes or invalid HTML-label syntax.
 * @since 0.0.1
 */
class DotTextTest {
    @Test
    void quotesAttributesWithoutStatementInjection() {
        Assertions.assertEquals(
            "red\\\"; injected [label=\\\"bad\\\"]\\n\\\\tail",
            DotText.quote("red\"; injected [label=\"bad\"]\n\\tail")
        );
    }

    @Test
    void encodesHtmlAndGraphvizSubstitutionCharacters() {
        Assertions.assertEquals(
            "&lt;&amp;&gt;&quot;&apos;&#92;N[U+0000]",
            DotText.encodeHtml("<&>\"'\\N\0")
        );
    }

    @Test
    void truncationDoesNotSplitSurrogatePairs() {
        final String text = "a".repeat(63).concat("😀end");
        Assertions.assertEquals("a".repeat(63).concat("..."), DotText.truncate(text));
    }
}
