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
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A bounded external invocation with file-backed streams and private staging output.
 * @since 0.0.1
 */
final class DotProcess {
    /**
     * Executable and optional launcher arguments, never parsed by a shell.
     */
    private final List<String> command;

    /**
     * Process timeout in milliseconds.
     */
    private final long timeout;

    /**
     * Configures a runner.
     * @param command Executable and arguments
     * @param timeout Positive timeout
     * @throws IllegalArgumentException If the command or timeout is invalid
     */
    DotProcess(final List<String> command, final Duration timeout) {
        this.command = DotProcess.checkedCommand(command);
        this.timeout = DotProcess.millis(timeout);
    }

    /**
     * Runs Graphviz and replaces the destination after success.
     * @param dot Input text
     * @param output Destination
     * @param format Validated format
     * @throws IOException On process or file errors
     */
    void render(final String dot, final Path output, final String format) throws IOException {
        final Path target = output.toAbsolutePath();
        try (RenderFiles files = new RenderFiles(target.getParent())) {
            Files.writeString(files.input(), dot, StandardCharsets.UTF_8);
            final List<String> args = new ArrayList<>(this.command);
            args.add("-T".concat(format));
            final Process process = new ProcessBuilder(args)
                .redirectInput(files.input().toFile())
                .redirectOutput(files.output().toFile())
                .redirectError(files.error().toFile()).start();
            try {
                this.await(process);
                DotProcess.check(process, files);
                Files.move(files.output(), target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Waits without leaving a running process after timeout or interruption.
     * @param process Child process
     * @throws IOException On timeout/interruption
     */
    private void await(final Process process) throws IOException {
        try {
            if (!process.waitFor(this.timeout, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly().waitFor();
                throw new IOException("Graphviz process timed out");
            }
        } catch (final InterruptedException error) {
            process.destroyForcibly();
            process.onExit().join();
            Thread.currentThread().interrupt();
            final InterruptedIOException failure =
                new InterruptedIOException("Graphviz interrupted");
            failure.initCause(error);
            throw failure;
        }
    }

    /**
     * Checks exit code and nonempty output, bounding diagnostic memory.
     * @param process Completed process
     * @param files Staged streams
     * @throws IOException On a nonzero exit or absent output
     */
    private static void check(final Process process, final RenderFiles files) throws IOException {
        if (process.exitValue() != 0 || Files.size(files.output()) == 0) {
            final String diagnostic;
            try (var stream = Files.newInputStream(files.error())) {
                diagnostic = new String(stream.readNBytes(8192), StandardCharsets.UTF_8);
            }
            throw new IOException(
                String.format("Graphviz exited with code %d: %s", process.exitValue(), diagnostic)
            );
        }
    }

    /**
     * Validates command configuration.
     * @param command Arguments
     * @return Immutable command
     * @throws IllegalArgumentException If no executable was specified
     */
    private static List<String> checkedCommand(final List<String> command) {
        if (command.isEmpty() || command.getFirst().isBlank()) {
            throw new IllegalArgumentException("Executable required");
        }
        return List.copyOf(command);
    }

    /**
     * Validates the timeout.
     * @param timeout Duration
     * @return Positive milliseconds
     * @throws IllegalArgumentException If the timeout is not positive
     */
    private static long millis(final Duration timeout) {
        final long result = timeout.toMillis();
        if (result <= 0) {
            throw new IllegalArgumentException("Positive timeout required");
        }
        return result;
    }

}
