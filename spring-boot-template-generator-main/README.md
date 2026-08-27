# Local Spring Initializr

A local replacement for `start.spring.io` that still generates **Spring Boot 2.x and 3.x**
project skeletons (the official site now only emits Boot 4). See [CLAUDE.md](CLAUDE.md) for the
full design notes.

## Requirements

- JDK 17 or later on your `PATH` (`java -version` to check)
- Nothing else — no Maven, no npm, no local `.m2` repository needed to run the tool itself

## Run it

From this directory:

```
java Initializr.java
```

Then open **http://localhost:8080** in a browser.

To use a different port:

```
java Initializr.java 9090
```

There is no build step. `java Initializr.java` compiles and runs the file directly (Java's
single-file source-launch mode), and the server reads its HTML/CSS/JS from the `web/` folder next
to it, so keep that folder alongside `Initializr.java`.

## Generate a project

1. Pick **Project** (Maven / Gradle-Groovy / Gradle-Kotlin), **Language**, **Packaging**, **Spring
   Boot version**, and **Java version**.
2. Fill in Group / Artifact / Name / Description / Package name.
3. Search and check the dependencies you want.
4. Click **Generate** — the browser downloads a `.zip`.
5. Unzip it and open the folder in IntelliJ IDEA (or any IDE). The IDE resolves the project's
   Maven/Gradle dependencies as usual — this tool only writes the skeleton, it never downloads
   anything itself.

Maven projects also get `mvnw` / `mvnw.cmd` (the official jar-less Apache Maven Wrapper — no
binary checked in; it downloads Maven itself on first run). On macOS/Linux, run `chmod +x mvnw`
once after unzipping — the executable bit doesn't survive the zip step. On Windows, `mvnw.cmd`
works as-is.

## Stopping the server

`Ctrl+C` in the terminal it's running in.
