# Local Spring Initializr for Boot 2.x / 3.x

## Context

`start.spring.io` now only emits Spring Boot 4 projects. The 2.x and 3.x generators are gone, so
there is no way to bootstrap a legacy-line project from the official site anymore.

This builds a **local replacement**: a small web app at `http://localhost:8080` with the same
workflow as the real Initializr — pick build tool, language, Boot version, Java version, and
dependencies, click Generate, get a `.zip`. Open the unzipped folder in IntelliJ IDEA and let the
IDE resolve dependencies as usual.

Hard constraints from the request:
- **No database** — the tool holds all state in memory; nothing is persisted.
- **No local Maven repository** — the tool itself must not need any downloaded artifact. It runs on
  JDK built-ins only (`com.sun.net.httpserver`, `java.util.zip`), so nothing is fetched to build or
  run it. Dependency *resolution* is IDEA's job, on the generated project.

Repo is currently empty (`master`, no commits). Everything below is new.

## Decisions (confirmed)

| Question | Answer |
|---|---|
| Tool stack | Single-file Java, `java Initializr.java` (JDK 21 source-file mode) |
| Build tools | Maven, Gradle Groovy DSL, Gradle Kotlin DSL |
| Languages | Java **and** Kotlin |
| DB starters | Kept in the catalog (SQL + NoSQL groups) as optional picks |
| Extras | Sample controller + `@SpringBootTest`, a Maven wrapper (mvnw), **and** a `.gitignore` — **no** README in generated projects, **no** custom-version text box |

## Layout

```
Spring Initializr/
  Initializr.java          # the whole server + generator, one file
  web/
    index.html             # UI
    app.js                 # vanilla JS, no npm
    style.css
  README.md                # how to start the tool (for the tool, not generated projects)
```

Java 21's source-file launcher compiles **one** file only (multi-file is Java 22+), so all server,
catalog, and template code lives in `Initializr.java` as top-level/nested classes. Static assets
stay on disk under `web/` so the UI can be edited without restarting.

Run: `java Initializr.java` (optional `java Initializr.java 9090` to change port).

## Initializr.java — structure

Routes, all served by `HttpServer.create(...)` with a fixed thread pool:

| Route | Behavior |
|---|---|
| `GET /`, `/app.js`, `/style.css` | Read from `web/`, path-traversal guarded (resolve + `startsWith` check on the canonical `web/` dir) |
| `GET /api/metadata` | Hand-built JSON: boot versions, Java versions per version, dependency catalog |
| `GET /starter.zip?...` | Streams the ZIP; `Content-Disposition: attachment; filename="<artifact>.zip"` |

**No JSON parsing anywhere.** Requests carry plain query params (`type`, `language`,
`bootVersion`, `javaVersion`, `groupId`, `artifactId`, `name`, `description`, `packageName`,
`packaging`, `dependencies=web,security,...`), decoded with `URLDecoder`. Responses build JSON with
a `StringBuilder` + a small `jsonEscape` helper. This is why the JDK's missing JSON support costs
nothing.

### Data model

```java
record Coord(String groupId, String artifactId, String version, String scope, boolean optional) {}
// version == null  -> managed by the Boot BOM, emit no <version>
record Dep(String id, String group, String label, String desc, Coord boot2, Coord boot3) {}
// a null boot2/boot3 -> unavailable on that line; UI greys it out and the server rejects it
record BootVersion(String version, int major, List<String> javaVersions, String kotlinVersion) {}
```

`Coord` per major line is the crux — it is what makes one catalog serve both Boot 2 and Boot 3.
Most Spring starters share coordinates across lines (`version == null`, BOM-managed). The ones that
genuinely differ, and must be encoded as split entries:

- springdoc — `org.springdoc:springdoc-openapi-ui:1.7.x` (Boot 2) vs
  `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.x` (Boot 3)
- MyBatis — `mybatis-spring-boot-starter` 2.3.x (Boot 2) vs 3.0.x (Boot 3)
- MySQL driver — `mysql:mysql-connector-java` (Boot 2 BOM) vs `com.mysql:mysql-connector-j` (Boot 3 BOM)

Catalog groups: Web, Template Engines, Security, Ops, Messaging, Developer Tools, SQL, NoSQL.
(Lombok/devtools/configuration-processor carry `optional`/`provided` flags via `Coord`.)

### Version matrix

A single `BOOT_VERSIONS` array near the top of the file is the only thing to edit when versions age.

- Boot 2 → Java 8 / 11 / 17, Kotlin 1.6.21
- Boot 3 → Java 17 / 21 uniformly across all 3.x lines, Kotlin 1.9.25

(Implementation note: newer 3.x lines can technically target Java 22+, but the matrix was kept
uniform at 17/21 to match the JDK 21 installed locally and avoid a per-line Java version table.)

**Before hardcoding the list I will fetch**
`https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml`
(read-only, one request) to confirm each patch version actually exists, so no generated `pom.xml`
points at a version that was never released. The dropdown groups them under `2.x` / `3.x` headers.

### Generated file set

Root folder inside the zip = artifactId.

- Build file — `pom.xml`, or `build.gradle` + `settings.gradle`, or `build.gradle.kts` +
  `settings.gradle.kts`
- `.gitignore` — always emitted, matching the real start.spring.io generator
  (`GitProjectGenerationConfiguration` in spring-io/initializr): branches its general/STS/IntelliJ
  sections by `buildTool` (Maven ignores `target/` + the wrapper jar it never checks in; Gradle
  ignores `.gradle`/`build/` and adds the IntelliJ `out/` and STS `bin/` lines Gradle's default
  output layout needs), identical NetBeans/VS Code sections for both. Deliberately drops upstream's
  `gradle/wrapper/gradle-wrapper.jar` exception line since this tool doesn't generate a Gradle
  wrapper (out of scope, below)
- `src/main/{java|kotlin}/<pkg path>/<Name>Application.{java|kt}`
- `src/main/resources/application.properties`, plus empty `static/` and `templates/` entries
- `src/test/{java|kotlin}/<pkg path>/<Name>ApplicationTests.{java|kt}` — `@SpringBootTest` context load
- `HelloController.{java|kt}` — only when a web or webflux starter is selected
- `ServletInitializer.{java|kt}` — only when `packaging=war` (plus `spring-boot-starter-tomcat`
  at `provided`)
- `mvnw` + `mvnw.cmd` + `.mvn/wrapper/maven-wrapper.properties` — only when `buildTool=maven`.
  Jar-less wrapper (`distributionType=script`): no binary is checked in, `mvnw`/`mvnw.cmd`
  download the small `maven-wrapper.jar` themselves on first run, which in turn fetches the
  pinned Maven distribution. Script content is the official Apache Maven Wrapper 3.3.2 output,
  captured verbatim via `mvn -N org.apache.maven.plugins:maven-wrapper-plugin:3.3.2:wrapper
  -Dtype=script` against the locally installed Maven, then embedded as Java text-block constants
  (`MVNW_SH`, `MVNW_CMD`) — not reproduced from memory, to guarantee byte-for-byte fidelity.
  `mvnw.cmd` is re-emitted with CRLF line endings (the DOS-batch convention); `mvnw` stays LF.
  `MAVEN_DISTRIBUTION_VERSION` / `MAVEN_WRAPPER_VERSION` constants drive `maven-wrapper.properties`
  (the value actually used at runtime — `distributionUrl`/`wrapperUrl`), so bumping them alone is
  enough to change which Maven/wrapper-jar a generated project fetches. The `MVNW_SH`/`MVNW_CMD`
  script bodies also carry the wrapper version in their header comment and as a fallback
  `wrapperUrl` (only used if the properties file's key is unreadable) — those are baked into the
  verbatim script text, so bumping `MAVEN_WRAPPER_VERSION` alone leaves them stale; re-run the
  `mvn -N ... :wrapper -Dtype=script` capture command with the new plugin version and re-paste the
  two constants to keep the script text in sync too. Known limitation: `java.util.zip.ZipEntry`
  has no public API for the Unix executable bit, so `mvnw` loses `+x` on extraction on
  macOS/Linux — documented in the README as a one-time `chmod +x mvnw`, rather than pulling in a
  zip library for this alone.

Line-specific details the templates must get right:
- **Boot 2 → `javax.*`, Boot 3 → `jakarta.*`** wherever an import is emitted
- Gradle Boot 2 and 3 both apply `org.springframework.boot` + `io.spring.dependency-management`
- Maven + Kotlin needs `kotlin-maven-plugin` with the `spring` compiler plugin and
  `sourceDirs` pointed at `src/main/kotlin`; `${kotlin.version}` comes from the Boot parent
- Kotlin projects add `jackson-module-kotlin` and `kotlin-reflect`
- A `-SNAPSHOT`/`-M`/`-RC` version would need the `spring-milestones` repo — with a fixed preset
  list of GA versions this cannot occur, so the repo block is not emitted

Zip writing uses `ZipOutputStream` with forward-slash entry names (never `File.separator`) and
UTF-8, streamed straight to the response body.

### Input hardening

`groupId`/`packageName` sanitized to legal Java identifiers (strip invalid chars, prefix a `_` on
digit-leading segments, reject Java keywords); `artifactId` restricted to `[A-Za-z0-9._-]`; the
selected `bootVersion`, `javaVersion`, `type`, `language`, and every dependency id validated against
the catalog — anything unknown is a `400`, so nothing user-supplied ever reaches a file path.

## web/ — the UI

Vanilla HTML/CSS/JS, no framework, no npm, no CDN (must work offline). Layout mirrors
start.spring.io: Project / Language / Spring Boot radio columns on the left, Project Metadata form
below, dependency panel on the right with a search box, checkbox groups, and removable chips for
what is selected.

`app.js` fetches `/api/metadata` once on load, then on Boot-version change re-filters the Java
version options and greys out dependencies with no coordinate for that line. Generate simply sets
`window.location = '/starter.zip?' + params` — the browser handles the download, so no fetch/blob
plumbing is needed.

## Verification

1. `java Initializr.java` starts clean on JDK 21 with an empty `~/.m2` — proves the no-repo constraint.
2. `curl -s localhost:8080/api/metadata` returns valid JSON (pipe through `python -m json.tool`).
3. Generate a representative matrix with `curl -o` and unzip each:
   - Boot 2.7.x / Maven / Java 11 / jar / web + security
   - Boot 3.x / Gradle Kotlin DSL / Kotlin / Java 21 / jar / webflux + actuator
   - Boot 3.x / Maven / Java 17 / **war** / web + data-jpa + h2
   - Boot 2.7.x / Gradle Groovy / Java 8 / jar / no dependencies
   Assert per zip: expected paths present, package dirs match `packageName`, `javax.*` vs
   `jakarta.*` correct for the line, no `<version>` on BOM-managed deps, split-coordinate deps
   (springdoc/mybatis/mysql) resolved to the right artifact. For the two Maven cases, also assert
   `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties` are present, `mvnw.cmd` has
   CRLF line endings, and `mvnw` has LF. Every zip should also have a `.gitignore`; assert its
   content matches the build tool (Maven cases: `target/` present, no `.gradle`; Gradle cases:
   `.gradle` + `!**/src/main/**/build/` present, no `target/`).
4. Reject cases return 400: unknown dependency id, Java 21 on Boot 2, `../` in a static path.
5. **Optional, needs your go-ahead** — `mvn -q package` / `gradle build` inside a generated project
   is the only true end-to-end proof, but it downloads into `%USERPROFILE%\.m2`, which cuts against
   "no local Maven repository." I'll skip it unless you say otherwise; opening one generated project
   in IDEA achieves the same confirmation on your terms.

## Out of scope

README in generated projects, custom-version text box, Groovy as a project language,
Gradle wrapper (Maven wrapper was added on request; Gradle wrapper wasn't asked for), Spring Boot 4
(use the official site for that).
