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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Real process I/O, bounded diagnostics, timeout, interruption and cleanup.
 * @since 0.0.1
 */
@Timeout(15)
class DotProcessTest {
    /**
     * Mock DOT input.
     */
    private static final String DOT = "dot";

    @Test
    void transfersLargeInputAndOutputWithSpaces(final @TempDir Path directory) throws Exception {
        final String text = "input ".repeat(200_000);
        final Path output = directory.resolve("image with spaces.svg");
        new ImageRender(text, this.runner("copy", directory)).render(output.toFile());
        Assertions.assertEquals(text, Files.readString(output));
        this.clean(directory);
    }

    @Test
    void nonzeroExitPreservesDestinationAndBoundsDiagnostics(final @TempDir Path directory)
        throws IOException {
        final Path output = directory.resolve("image.svg");
        Files.writeString(output, "original");
        final IOException error = Assertions.assertThrows(
            IOException.class,
            () -> new ImageRender(
                DotProcessTest.DOT, this.runner(
                "fail", directory
            )
            ).render(
                output.toFile()
            )
        );
        Assertions.assertTrue(error.getMessage().contains("code 7"));
        Assertions.assertTrue(error.getMessage().contains("diagnostic"));
        Assertions.assertTrue(error.getMessage().length() < 9000);
        Assertions.assertEquals("original", Files.readString(output));
        this.clean(directory);
    }

    @Test
    void refusesMissingExecutableAndEmptyOutput(final @TempDir Path directory) throws IOException {
        final Path output = directory.resolve("image.svg");
        Assertions.assertThrows(
            IOException.class,
            () -> new ImageRender(
                DotProcessTest.DOT, directory.resolve(
                    "missing executable"
                ).toString(), Duration.ofSeconds(
                    1
                )
            ).render(output.toFile())
        );
        Assertions.assertThrows(
            IOException.class,
            () -> new ImageRender(
                DotProcessTest.DOT, this.runner(
                "empty", directory
            )
            ).render(
                output.toFile()
            )
        );
        Assertions.assertFalse(Files.exists(output));
        this.clean(directory);
    }

    @Test
    void timeoutReapsChildAndRemovesStaging(final @TempDir Path directory) throws IOException {
        final IOException error = Assertions.assertThrows(
            IOException.class,
            () -> new ImageRender(DotProcessTest.DOT, this.runner("sleep", directory))
                .render(directory.resolve("image.png").toFile())
        );
        Assertions.assertTrue(error.getMessage().contains("timed out"));
        this.stopped(directory);
        this.clean(directory);
    }

    @Test
    void interruptionIsPreservedAndChildIsReaped(final @TempDir Path directory) throws Exception {
        final AtomicReference<Exception> failure = new AtomicReference<>();
        final AtomicBoolean interrupted = new AtomicBoolean();
        final Thread thread = Thread.startVirtualThread(
            () -> {
                try {
                    new ImageRender(DotProcessTest.DOT, this.runner("sleep", directory))
                        .render(directory.resolve("image.svg").toFile());
                } catch (final IOException | WrongFileExtension error) {
                    failure.set(error);
                    interrupted.set(Thread.currentThread().isInterrupted());
                }
            }
        );
        try {
            final long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (!Files.exists(directory.resolve("pid")) && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            Assertions.assertTrue(Files.exists(directory.resolve("pid")));
        } finally {
            thread.interrupt();
            thread.join(5000);
        }
        Assertions.assertFalse(thread.isAlive());
        Assertions.assertInstanceOf(InterruptedIOException.class, failure.get());
        Assertions.assertTrue(interrupted.get());
        this.stopped(directory);
        this.clean(directory);
    }

    /**
     * Constructs a portable child-JVM launcher, without a shell.
     * @param mode Behavior
     * @param directory Temporary directory
     * @return Runner
     */
    private DotProcess runner(final String mode, final Path directory) {
        return new DotProcess(
            List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"), FakeDot.class.getName(),
                mode, directory.resolve("pid").toString()
            ), Duration.ofSeconds(2)
        );
    }

    /**
     * Confirms the child exited before the API returned.
     * @param directory Temporary files
     * @throws IOException On read failure
     */
    private void stopped(final Path directory) throws IOException {
        final long pid = Long.parseLong(Files.readString(directory.resolve("pid")));
        Assertions.assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
    }

    /**
     * Confirms private staging files have been removed.
     * @param directory Temporary files
     * @throws IOException On listing failure
     */
    private void clean(final Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            Assertions.assertFalse(
                files.anyMatch(path -> path.getFileName().toString().startsWith(".astranaut-dot-"))
            );
        }
    }
}
