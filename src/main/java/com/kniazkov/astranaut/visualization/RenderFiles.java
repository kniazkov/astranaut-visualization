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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Invocation-owned files avoid pipe backpressure and unbounded diagnostic buffering.
 * @since 0.0.1
 */
final class RenderFiles implements AutoCloseable {
    /**
     * Private staging directory beside the destination.
     */
    private final Path directory;

    /**
     * Creates staging storage.
     * @param parent Existing destination directory
     * @throws IOException If storage cannot be created
     */
    RenderFiles(final Path parent) throws IOException {
        this.directory = Files.createTempDirectory(parent, ".astranaut-dot-");
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(this.input());
        Files.deleteIfExists(this.output());
        Files.deleteIfExists(this.error());
        Files.delete(this.directory);
    }

    /**
     * Input location.
     * @return Path
     */
    Path input() {
        return this.directory.resolve("input.dot");
    }

    /**
     * Output location.
     * @return Path
     */
    Path output() {
        return this.directory.resolve("output");
    }

    /**
     * Diagnostic location.
     * @return Path
     */
    Path error() {
        return this.directory.resolve("stderr");
    }

}
