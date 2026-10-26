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

import com.kniazkov.astranaut.core.algorithms.PositionalDiff;
import com.kniazkov.astranaut.core.algorithms.normalization.Normalizer;
import com.kniazkov.astranaut.core.base.DraftNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.core.base.Type;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Repeatable, stack-safe occurrence visualization through the public core API.
 * @since 0.0.1
 */
class DotContractsTest {
    @Test
    void handlesEmptyRootsAndRepeatedConcurrentCalls() throws Exception {
        final DotGenerator empty = new DotGenerator(new Tree());
        Assertions.assertFalse(empty.generate().contains("label="));
        final DotGenerator generator = new DotGenerator(Tree.createDraft("Root(A,B)"));
        final String expected = generator.generate();
        Assertions.assertEquals(expected, generator.generate());
        try (var pool = Executors.newFixedThreadPool(2)) {
            final var tasks = java.util.Collections.nCopies(
                20, (java.util.concurrent.Callable<String>) generator::generate
            );
            for (final var result : pool.invokeAll(tasks)) {
                Assertions.assertEquals(expected, result.get());
            }
        }
    }

    @Test
    void visitsSharedOccurrencesAndDeepChainsWithoutRecursion() {
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Root");
        final Node shared = DraftNode.create("Leaf");
        builder.setChildren(List.of(shared, shared));
        final String dot = new DotGenerator(new Tree(builder.build())).generate();
        Assertions.assertTrue(dot.contains("node_0 -> node_1"));
        Assertions.assertTrue(dot.contains("node_0 -> node_2"));
        Node node = shared;
        for (int index = 0; index < 20_000; index += 1) {
            builder.setChildren(List.of(node));
            node = builder.build();
        }
        Assertions.assertTrue(new DotGenerator(new Tree(node)).generate().contains("node_20000"));
    }

    @Test
    void supportsDifferenceAndNormalizedStructures() {
        final Tree source = Tree.createDraft("Root(A)");
        final Tree target = Tree.createDraft("Root(B,C)");
        final Tree diff = new PositionalDiff().compare(source, target);
        Assertions.assertTrue(new DotGenerator(diff).generate().contains("label="));
        final Tree normalized = new Tree(
            new Normalizer().normalize(source.getRoot().orElseThrow())
        );
        Assertions.assertTrue(new DotGenerator(normalized).generate().contains("Properties"));
    }

    @Test
    void escapesTypeNamesAndData() {
        final DraftNode.Constructor builder = new DraftNode.Constructor();
        builder.setName("Type<&>\"");
        builder.setData("<&>\"");
        final String dot = new DotGenerator(new Tree(builder.build())).generate();
        Assertions.assertTrue(dot.contains("Type&lt;&amp;&gt;&quot;"));
        Assertions.assertFalse(dot.contains("Type<&>"));
    }

    @Test
    void rejectsCyclesWithoutPublishingPartialOutput() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new DotGenerator(new Tree(new CyclicNode())).generate()
        );
    }

    /**
     * Intentionally invalid source graph for cycle refusal.
     * @since 0.0.1
     */
    private static final class CyclicNode implements Node {
        @Override
        public Type getType() {
            return DraftNode.create("Cycle").getType();
        }

        @Override
        public String getData() {
            return "";
        }

        @Override
        public int getChildCount() {
            return 1;
        }

        @Override
        public Node getChild(final int index) {
            java.util.Objects.checkIndex(index, 1);
            return this;
        }
    }

}
