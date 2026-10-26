# Astranaut visualization

Optional DOT, PNG and SVG visualization for Java 21 syntax trees.

```xml
<dependency>
  <groupId>com.kniazkov</groupId>
  <artifactId>astranaut-visualization</artifactId>
  <version>0.0.1</version>
</dependency>
```

The only production Maven dependency is `com.kniazkov:astranaut-core:0.0.1`.
There is no graphviz-java, J2V8 or logging backend. The dependency direction is
visualization → core; applications using core alone need no rendering software.

## Usage

```java
import com.kniazkov.astranaut.core.base.Tree;
import com.kniazkov.astranaut.visualization.DotGenerator;
import com.kniazkov.astranaut.visualization.TreeVisualizer;

Tree tree = Tree.createDraft("Addition(Number<'2'>,Number<'3'>)");
String dot = new DotGenerator(tree).generate();
new TreeVisualizer(tree).visualize(new java.io.File("tree.svg"));
```

DOT generation uses Java alone. PNG/SVG rendering requires Graphviz's `dot` executable
on PATH. On Debian/Ubuntu: `sudo apt-get install graphviz`. On Windows/macOS install
Graphviz and add its executable directory to PATH. Verify installation with `dot -V`.
Only `.png` and `.svg` destination suffixes are accepted; DOT text can be written with
`Files.writeString(path, dot)`. The destination directory must already exist.

An executable path containing spaces is a single argument, not a shell command:

```java
new com.kniazkov.astranaut.visualization.ImageRender(
    dot, "/opt/Graphviz tools/bin/dot", java.time.Duration.ofSeconds(30)
).render(new java.io.File("tree with spaces.png"));
```

The default timeout is one minute. No shell is started. Input, output and stderr use
private temporary files beside the destination, avoiding pipe deadlocks and unbounded
in-memory diagnostic accumulation. Disk use follows DOT input and process output; this
is not a disk quota or a sandbox for untrusted Graphviz executables or arbitrary DOT.

Nonzero exit status, empty output, launch failure and timeout produce IOException.
At most 8 KiB of stderr is included in failure diagnostics. Interruption kills and reaps
the direct child, restores the interrupt flag and throws InterruptedIOException. The
configured executable must itself be Graphviz (or a cooperative launcher); management
of arbitrary detached subprocess trees is not promised. Staging files are removed on
success/failure. Existing destination contents remain untouched if rendering fails;
a completed image replaces them only after a successful exit. Filesystem replacement
is not advertised as an atomic operation on every platform.

## Tree semantics

Empty trees, ordinary nodes, holes, patterns, differences and normalized structures
are visualized as their actual node graphs; rendering never applies edit operations.
Shared objects are drawn once per occurrence, retaining separate edges. Edge numbers
refer to the current ordered child list. Node properties `color` and `bgcolor` control
border/background colors; Graphviz determines whether a color value is valid.

Each generate call owns its state. Repeated/concurrent calls on immutable inputs produce
the same DOT. Traversal is iterative and rejects child cycles. Tests cover a 20,000-deep
chain; rendering/layout costs and memory for large graphs remain Graphviz-dependent.
Type names and data are escaped for HTML labels; attributes are quoted. Non-XML control
characters are displayed as `[U+XXXX]`; data is truncated to 64 UTF-16 units (preferably
at a space, without splitting a surrogate pair) followed by `...`.

## Build and migration

Requires JDK 21, Maven 3.6.3+, Graphviz for the full image tests, and the migrated core
artifact installed locally or published in Maven Central:

```sh
# In the matching astranaut-core checkout:
mvn -B -ntp clean install
# In this checkout:
mvn -B -ntp clean verify
```

CI first installs a pinned matching core commit, then runs all visualization tests and
shared Checkstyle/PMD/dependency checks. This makes the initial cross-repository migration
reviewable before core's updated artifact is released. It is not a source inclusion or
reactor dependency: visualization compiles against the installed core JAR.

Migrating callers: replace `com.kniazkov.astranaut.core.utils.TreeVisualizer` and
`com.kniazkov.astranaut.core.utils.visualizer.*` imports with
`com.kniazkov.astranaut.visualization.*`, and explicitly add this dependency. The former
classes and Graphviz/Logback dependencies are removed from core.

Tests retain the original DOT fixtures and image assertions. Additional regressions cover
repeatability/concurrent readers, shared occurrences, deep chains, cycles, escaping,
surrogate-safe truncation, service nodes, real PNG/SVG, paths with spaces, large process
streams, error codes, bounded diagnostics, missing executable, empty output, timeout,
interruption, process termination and staging cleanup.

## Publication

The POM inherits `com.kniazkov:astranaut-parent:0.0.1` from astranaut-build, including its
release signing and Central publishing profile. SCM, license, developer metadata,
sources and Javadocs are configured for this repository. Publication is manual:

```sh
mvn -Prelease clean deploy
```

Use the same Maven `central` credentials and GPG configuration as the other projects.
Publish the migrated core version first, then visualization. Do not republish an immutable
Central version if 0.0.1 has already been released: choose the next version and update
the dependency together. No deployment runs on PRs; credentials are not stored here.
