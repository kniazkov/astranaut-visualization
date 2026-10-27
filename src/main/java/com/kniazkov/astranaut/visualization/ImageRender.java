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

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * PNG/SVG rendering through the installed Graphviz executable, without a shell.
 * @since 0.0.1
 */
public final class ImageRender {
    /**
     * DOT input.
     */
    private final String dot;

    /**
     * Process runner.
     */
    private final DotProcess process;

    /**
     * Uses dot on PATH with a one-minute process timeout.
     * @param dot DOT input
     */
    public ImageRender(final String dot) {
        this(dot, "dot", Duration.ofMinutes(1));
    }

    /**
     * Selects an executable and a positive timeout.
     * @param dot DOT input
     * @param executable Executable path, not a shell command
     * @param timeout Maximum process duration
     */
    public ImageRender(final String dot, final String executable, final Duration timeout) {
        this(dot, new DotProcess(List.of(executable), timeout));
    }

    /**
     * Selects a process for integration testing.
     * @param dot DOT input
     * @param process Runner
     */
    ImageRender(final String dot, final DotProcess process) {
        this.dot = dot;
        this.process = process;
    }

    /**
     * Publishes the completed image only after successful rendering.
     * @param file Destination PNG or SVG
     * @throws WrongFileExtension If the format is unsupported
     * @throws IOException If execution fails, times out or is interrupted
     */
    public void render(final File file) throws WrongFileExtension, IOException {
        final String name = file.getName();
        final String format = name.substring(name.lastIndexOf('.') + 1);
        if (!List.of("png", "svg").contains(format) || name.lastIndexOf('.') < 0) {
            throw WrongFileExtension.INSTANCE;
        }
        this.process.render(this.dot, file.toPath(), format);
    }
}
