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

import com.kniazkov.astranaut.core.base.DummyNode;
import com.kniazkov.astranaut.core.base.Node;
import com.kniazkov.astranaut.core.base.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates graph description in DOT format from a tree.
 * @since 0.0.1
 */
final class DotWriter {
    /**
     * Node name start text.
     */
    private static final String NODE = "  node_";

    /**
     * The 'color' property.
     */
    private static final String PROP_COLOR = "color";

    /**
     * The 'bgcolor' property.
     */
    private static final String PROP_BGCOLOR = "bgcolor";

    /**
     * Stores the generated DOT text.
     */
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder builder;

    /**
     * The tree to be converted.
     */
    private final Tree tree;

    /**
     * Last index used for a node.
     */
    private int index;

    /**
     * Constructor.
     * @param tree The tree to be converted
     */
    DotWriter(final Tree tree) {
        this.builder = new StringBuilder(256);
        this.tree = tree;
        this.index = 0;
    }

    /**
     * Renders data to a DOT format.
     * @return A rendered DOT text as string
     */
    public String generate() {
        this.appendStart();
        this.tree.getRoot().ifPresent(this::processNode);
        this.appendEnd();
        return this.builder.toString();
    }

    /**
     * Processes a node with all its children.
     * @param node A node.
     * @throws IllegalArgumentException If a child cycle is found
     */
    private void processNode(final Node node) {
        final List<Frame> stack = new ArrayList<>(1);
        final Set<Node> active = Collections.newSetFromMap(new IdentityHashMap<>());
        stack.add(new Frame(node, this.index));
        active.add(node);
        this.append(node);
        while (!stack.isEmpty()) {
            final Frame frame = stack.getLast();
            if (frame.next < frame.node.getChildCount()) {
                final Node child = frame.node.getChild(frame.next);
                if (!active.add(child)) {
                    throw new IllegalArgumentException("Cyclic syntax graph");
                }
                frame.next += 1;
                this.index += 1;
                this.append(child);
                stack.add(new Frame(child, this.index));
            } else {
                stack.removeLast();
                active.remove(frame.node);
                if (!stack.isEmpty()) {
                    final Frame parent = stack.getLast();
                    this.appendEdge(parent.id, frame.id, parent.next - 1);
                }
            }
        }
    }

    /**
     * Emits one occurrence.
     * @param node Source node
     */
    private void append(final Node node) {
        if (node == DummyNode.INSTANCE) {
            this.appendNullNode();
        } else {
            this.appendNode(node.getTypeName(), node.getData(), node.getProperties());
        }
    }

    /**
     * Appends tree start text.
     */
    private void appendStart() {
        this.builder
            .append("digraph Tree {\n  node [shape=box style=rounded];\n");
    }

    /**
     * Appends tree end text.
     */
    private void appendEnd() {
        this.builder.append("}\n");
    }

    /**
     * Appends tree node text.
     *
     * @param type Nde type
     * @param data Node data
     * @param properties Node properties
     */
    private void appendNode(
        final String type, final String data,
        final Map<String, String> properties
    ) {
        this.builder.append(DotWriter.NODE).append(this.index).append(" [label=<")
            .append(DotText.encodeHtml(type));
        if (!data.isEmpty()) {
            this.builder.append("<br/><font color=\"blue\">")
                .append(DotText.encodeHtml(DotText.truncate(data)))
                .append("</font>");
        }
        this.builder.append('>');
        if (properties.containsKey(DotWriter.PROP_COLOR)) {
            this.builder.append(" color=\"")
                .append(DotText.quote(properties.get(DotWriter.PROP_COLOR)))
                .append('"');
        }
        if (properties.containsKey(DotWriter.PROP_BGCOLOR)) {
            this.builder.append(" style=\"filled,rounded\" fillcolor=\"")
                .append(DotText.quote(properties.get(DotWriter.PROP_BGCOLOR))).append('"');
        }
        this.builder.append("];\n");
    }

    /**
     * Appends null node text.
     */
    private void appendNullNode() {
        this.builder
            .append(DotWriter.NODE)
            .append(this.index)
            .append(" [label=<NULL>];\n");
    }

    /**
     * Appends tree edge text.
     * @param parent A parent node index
     * @param child A child node index
     * @param label An edge label
     */
    private void appendEdge(final int parent, final int child, final int label) {
        this.builder
            .append(DotWriter.NODE)
            .append(parent)
            .append(" -> node_")
            .append(child)
            .append(" [label=\" ")
            .append(label)
            .append("\"];\n");
    }

    /**
     * Iterative depth-first occurrence state.
     * @since 0.0.1
     */
    private static final class Frame {
        /**
         * Source node.
         */
        private final Node node;

        /**
         * Output identifier.
         */
        private final int id;

        /**
         * Next child position.
         */
        private int next;

        /**
         * Creates a traversal frame.
         * @param node Source
         * @param id Output identifier
         */
        private Frame(final Node node, final int id) {
            this.node = node;
            this.id = id;
        }
    }

}
