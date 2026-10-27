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

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Real child JVM used to exercise process failure and termination without Graphviz.
 * @since 0.0.1
 */
public final class FakeDot {
    /**
     * Utility.
     */
    private FakeDot() { }

    /**
     * Produces controlled process behavior.
     * @param args Mode and PID file
     * @throws Exception On fixture failures
     */
    @SuppressWarnings({"PMD.DoNotTerminateVM", "PMD.SystemPrintln"})
    public static void main(final String... args) throws Exception {
        final Path pending = Path.of(args[1].concat(".tmp"));
        Files.writeString(pending, Long.toString(ProcessHandle.current().pid()));
        Files.move(pending, Path.of(args[1]));
        switch (args[0]) {
            case "sleep" -> Thread.sleep(30_000);
            case "fail" -> {
                System.err.print("diagnostic".repeat(200_000));
                System.out.print("partial image");
                System.exit(7);
            }
            case "empty" -> System.in.readAllBytes();
            default -> {
                System.in.transferTo(System.out);
                System.err.print("warning".repeat(200_000));
            }
        }
    }
}
