import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A local, self-contained replacement for start.spring.io that still generates
 * Spring Boot 2.x and 3.x project skeletons (the official site now only emits Boot 4).
 *
 * Run with:  java Initializr.java [port]
 *
 * No dependency downloads at any point: this file uses only JDK built-ins
 * (com.sun.net.httpserver, java.util.zip) to run, and only writes a project
 * skeleton to disk as a zip -- resolving the generated project's own
 * dependencies is left to the IDE that opens it.
 */
public class Initializr {

    public static void main(String[] args) throws IOException {
        int port = 8081;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port: '" + args[0] + "' is not a number");
                System.exit(1);
            }
        }
        Path webDir = Paths.get("web").toAbsolutePath().normalize();
        if (!Files.isDirectory(webDir)) {
            System.err.println("Cannot find web/ directory next to Initializr.java (looked in " + webDir + ")");
            System.exit(1);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new StaticHandler(webDir));
        server.createContext("/api/metadata", Initializr::handleMetadata);
        server.createContext("/starter.zip", Initializr::handleGenerate);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Spring Initializr (local) running at http://localhost:" + port);
        System.out.println("Serving static assets from " + webDir);
    }

    // =====================================================================
    // Data model
    // =====================================================================

    /** A single dependency's Maven coordinate for one Spring Boot major line. */
    record Coord(String groupId, String artifactId, String version, String scope, boolean optional) {
        static Coord managed(String g, String a) {
            return new Coord(g, a, null, null, false);
        }
        static Coord managed(String g, String a, String scope) {
            return new Coord(g, a, null, scope, false);
        }
        static Coord managedOptional(String g, String a) {
            return new Coord(g, a, null, null, true);
        }
        static Coord versioned(String g, String a, String v) {
            return new Coord(g, a, v, null, false);
        }
        static Coord versioned(String g, String a, String v, String scope) {
            return new Coord(g, a, v, scope, false);
        }
    }

    /** A catalog entry. boot2/boot3 may be null if unavailable on that line (not the case for any entry below). */
    record Dep(String id, String group, String label, String desc, Coord boot2, Coord boot3) {
        Coord coordFor(int bootMajor) {
            return bootMajor == 2 ? boot2 : boot3;
        }
    }

    record BootVersion(String version, int major, List<String> javaVersions, String kotlinVersion, boolean recommended) {}

    // =====================================================================
    // Catalog: Spring Boot versions
    // =====================================================================

    static final List<BootVersion> BOOT_VERSIONS = List.of(
        new BootVersion("3.5.16", 3, List.of("17", "21"), "1.9.25", true),
        new BootVersion("3.4.13", 3, List.of("17", "21"), "1.9.25", false),
        new BootVersion("3.3.13", 3, List.of("17", "21"), "1.9.25", false),
        new BootVersion("3.2.12", 3, List.of("17", "21"), "1.9.25", false),
        new BootVersion("3.1.12", 3, List.of("17", "21"), "1.9.25", false),
        new BootVersion("3.0.13", 3, List.of("17", "21"), "1.9.25", false),
        new BootVersion("2.7.18", 2, List.of("8", "11", "17"), "1.6.21", false),
        new BootVersion("2.6.15", 2, List.of("8", "11", "17"), "1.6.21", false)
    );

    static final Map<String, BootVersion> BOOT_BY_VERSION = BOOT_VERSIONS.stream()
        .collect(Collectors.toMap(BootVersion::version, v -> v, (a, b) -> a, LinkedHashMap::new));

    // =====================================================================
    // Catalog: dependencies, grouped
    // =====================================================================

    static final LinkedHashMap<String, List<Dep>> CATALOG = buildCatalog();
    static final Map<String, Dep> DEP_BY_ID = CATALOG.values().stream()
        .flatMap(List::stream)
        .collect(Collectors.toMap(Dep::id, d -> d, (a, b) -> a, LinkedHashMap::new));

    static LinkedHashMap<String, List<Dep>> buildCatalog() {
        LinkedHashMap<String, List<Dep>> groups = new LinkedHashMap<>();

        groups.put("Web", List.of(
            new Dep("web", "Web", "Spring Web", "Build web, incl. RESTful, applications using Spring MVC.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-web"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-web")),
            new Dep("webflux", "Web", "Spring Reactive Web", "Build reactive web applications with Spring WebFlux and Netty.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-webflux"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-webflux")),
            new Dep("websocket", "Web", "WebSocket", "Build WebSocket applications with SockJS and STOMP.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-websocket"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-websocket"))
        ));

        groups.put("Template Engines", List.of(
            new Dep("thymeleaf", "Template Engines", "Thymeleaf", "A modern server-side Java template engine.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-thymeleaf"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-thymeleaf")),
            new Dep("freemarker", "Template Engines", "Apache Freemarker", "Java server-side template engine from the Apache Software Foundation.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-freemarker"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-freemarker")),
            new Dep("mustache", "Template Engines", "Mustache", "Logic-less templates, ports available to most languages.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-mustache"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-mustache"))
        ));

        groups.put("Security", List.of(
            new Dep("security", "Security", "Spring Security", "Authentication and authorization using Spring Security.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-security"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-security")),
            new Dep("oauth2-client", "Security", "OAuth2 Client", "OAuth2/OpenID Connect client (login and API access).",
                Coord.managed("org.springframework.boot", "spring-boot-starter-oauth2-client"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-oauth2-client")),
            new Dep("oauth2-resource-server", "Security", "OAuth2 Resource Server", "Protect APIs with OAuth2/JWT bearer tokens.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-oauth2-resource-server"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-oauth2-resource-server"))
        ));

        groups.put("Ops", List.of(
            new Dep("actuator", "Ops", "Spring Boot Actuator", "Production-ready features to help monitor and manage the application.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-actuator"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-actuator")),
            new Dep("devtools", "Ops", "Spring Boot DevTools", "Fast application restarts and live reload for development.",
                Coord.managedOptional("org.springframework.boot", "spring-boot-devtools"),
                Coord.managedOptional("org.springframework.boot", "spring-boot-devtools")),
            new Dep("configuration-processor", "Ops", "Spring Configuration Processor", "Generate metadata for IDE auto-completion of custom @ConfigurationProperties.",
                Coord.managedOptional("org.springframework.boot", "spring-boot-configuration-processor"),
                Coord.managedOptional("org.springframework.boot", "spring-boot-configuration-processor")),
            new Dep("springdoc", "Ops", "OpenAPI / Swagger UI (springdoc)", "Generate OpenAPI 3 docs and a Swagger UI for the API.",
                Coord.versioned("org.springdoc", "springdoc-openapi-ui", "1.7.0"),
                Coord.versioned("org.springdoc", "springdoc-openapi-starter-webmvc-ui", "2.6.0"))
        ));

        groups.put("Messaging", List.of(
            new Dep("amqp", "Messaging", "Spring for RabbitMQ", "Give applications a common platform to send/receive messages using AMQP.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-amqp"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-amqp")),
            new Dep("kafka", "Messaging", "Spring for Apache Kafka", "Publish, subscribe, store, and process streams of records with Apache Kafka.",
                Coord.managed("org.springframework.kafka", "spring-kafka"),
                Coord.managed("org.springframework.kafka", "spring-kafka")),
            new Dep("integration", "Messaging", "Spring Integration", "Support for Enterprise Integration Patterns.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-integration"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-integration"))
        ));

        groups.put("Developer Tools", List.of(
            new Dep("lombok", "Developer Tools", "Lombok", "Reduce boilerplate via annotations (getters/setters/etc, generated at build time).",
                Coord.managedOptional("org.projectlombok", "lombok"),
                Coord.managedOptional("org.projectlombok", "lombok")),
            new Dep("validation", "Developer Tools", "Validation", "Bean Validation with Hibernate Validator.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-validation"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-validation")),
            new Dep("mail", "Developer Tools", "Java Mail Sender", "Send email using Java Mail and Spring Framework's JavaMailSender.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-mail"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-mail"))
        ));

        groups.put("SQL", List.of(
            new Dep("data-jpa", "SQL", "Spring Data JPA", "Persist data in SQL stores with Java Persistence API using Spring Data and Hibernate.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-data-jpa"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-data-jpa")),
            new Dep("jdbc", "SQL", "Spring Data JDBC", "Persist data in SQL stores with plain JDBC using Spring Data.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-jdbc"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-jdbc")),
            new Dep("mybatis", "SQL", "MyBatis Framework", "Persistence framework with SQL mapped to plain old Java objects.",
                Coord.versioned("org.mybatis.spring.boot", "mybatis-spring-boot-starter", "2.3.2"),
                Coord.versioned("org.mybatis.spring.boot", "mybatis-spring-boot-starter", "3.0.3")),
            new Dep("h2", "SQL", "H2 Database", "In-memory (or file-based) SQL database, useful for local development.",
                Coord.managed("com.h2database", "h2", "runtime"),
                Coord.managed("com.h2database", "h2", "runtime")),
            new Dep("mysql", "SQL", "MySQL Driver", "JDBC driver for MySQL.",
                Coord.managed("mysql", "mysql-connector-java", "runtime"),
                Coord.managed("com.mysql", "mysql-connector-j", "runtime")),
            new Dep("postgresql", "SQL", "PostgreSQL Driver", "JDBC driver for PostgreSQL.",
                Coord.managed("org.postgresql", "postgresql", "runtime"),
                Coord.managed("org.postgresql", "postgresql", "runtime"))
        ));

        groups.put("NoSQL", List.of(
            new Dep("data-mongodb", "NoSQL", "Spring Data MongoDB", "Store data in MongoDB, a document-based database, using Spring Data.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-data-mongodb"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-data-mongodb")),
            new Dep("data-redis", "NoSQL", "Spring Data Redis", "Access Redis key-value data stores using Spring Data.",
                Coord.managed("org.springframework.boot", "spring-boot-starter-data-redis"),
                Coord.managed("org.springframework.boot", "spring-boot-starter-data-redis"))
        ));

        return groups;
    }

    // =====================================================================
    // Static asset handler
    // =====================================================================

    static class StaticHandler implements HttpHandler {
        final Path root;

        StaticHandler(Path root) {
            this.root = root;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            try {
                if (!"GET".equals(ex.getRequestMethod())) {
                    sendPlainText(ex, 405, "Method Not Allowed");
                    return;
                }
                String path = ex.getRequestURI().getPath();
                if (path.equals("/") || path.isEmpty()) {
                    path = "/index.html";
                }
                Path file = root.resolve("." + path).normalize();
                if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                    sendPlainText(ex, 404, "Not found");
                    return;
                }
                byte[] body = Files.readAllBytes(file);
                ex.getResponseHeaders().set("Content-Type", contentType(file.toString()));
                ex.sendResponseHeaders(200, body.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(body);
                }
            } finally {
                ex.close();
            }
        }
    }

    static String contentType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        return "application/octet-stream";
    }

    static void sendPlainText(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // =====================================================================
    // /api/metadata
    // =====================================================================

    static void handleMetadata(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equals(ex.getRequestMethod())) {
                sendPlainText(ex, 405, "Method Not Allowed");
                return;
            }
            StringBuilder json = new StringBuilder();
            json.append("{\"bootVersions\":[");
            boolean first = true;
            for (BootVersion bv : BOOT_VERSIONS) {
                if (!first) json.append(",");
                first = false;
                json.append("{\"version\":\"").append(bv.version()).append("\",")
                    .append("\"major\":").append(bv.major()).append(",")
                    .append("\"recommended\":").append(bv.recommended()).append(",")
                    .append("\"kotlinVersion\":\"").append(bv.kotlinVersion()).append("\",")
                    .append("\"javaVersions\":[")
                    .append(bv.javaVersions().stream().map(v -> "\"" + v + "\"").collect(Collectors.joining(",")))
                    .append("]}");
            }
            json.append("],\"dependencyGroups\":[");
            first = true;
            for (Map.Entry<String, List<Dep>> group : CATALOG.entrySet()) {
                if (!first) json.append(",");
                first = false;
                json.append("{\"name\":\"").append(jsonEscape(group.getKey())).append("\",\"dependencies\":[");
                boolean firstDep = true;
                for (Dep d : group.getValue()) {
                    if (!firstDep) json.append(",");
                    firstDep = false;
                    json.append("{\"id\":\"").append(d.id()).append("\",")
                        .append("\"name\":\"").append(jsonEscape(d.label())).append("\",")
                        .append("\"description\":\"").append(jsonEscape(d.desc())).append("\",")
                        .append("\"boot2\":").append(d.boot2() != null).append(",")
                        .append("\"boot3\":").append(d.boot3() != null)
                        .append("}");
                }
                json.append("]}");
            }
            json.append("]}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } finally {
            ex.close();
        }
    }

    static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    // =====================================================================
    // /starter.zip
    // =====================================================================

    static void handleGenerate(HttpExchange ex) throws IOException {
        boolean headersSent = false;
        try {
            if (!"GET".equals(ex.getRequestMethod())) {
                sendPlainText(ex, 405, "Method Not Allowed");
                return;
            }
            Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());

            String buildTool = q.getOrDefault("buildTool", "maven");
            String language = q.getOrDefault("language", "java");
            String bootVersion = q.getOrDefault("bootVersion", "");
            String javaVersion = q.getOrDefault("javaVersion", "");
            String packaging = q.getOrDefault("packaging", "jar");
            String groupIdRaw = q.getOrDefault("groupId", "com.example");
            String artifactIdRaw = q.getOrDefault("artifactId", "demo");
            String nameRaw = q.getOrDefault("name", artifactIdRaw);
            String description = q.getOrDefault("description", "Demo project for Spring Boot");
            String packageNameRaw = q.get("packageName");
            String depsRaw = q.getOrDefault("dependencies", "");

            if (!Set.of("maven", "gradle-groovy", "gradle-kotlin").contains(buildTool)) {
                sendPlainText(ex, 400, "Invalid buildTool: " + buildTool);
                return;
            }
            if (!Set.of("java", "kotlin").contains(language)) {
                sendPlainText(ex, 400, "Invalid language: " + language);
                return;
            }
            if (!Set.of("jar", "war").contains(packaging)) {
                sendPlainText(ex, 400, "Invalid packaging: " + packaging);
                return;
            }
            BootVersion boot = BOOT_BY_VERSION.get(bootVersion);
            if (boot == null) {
                sendPlainText(ex, 400, "Unknown bootVersion: " + bootVersion);
                return;
            }
            if (!boot.javaVersions().contains(javaVersion)) {
                sendPlainText(ex, 400, "Java " + javaVersion + " is not supported on Spring Boot " + bootVersion
                    + " (supported: " + boot.javaVersions() + ")");
                return;
            }

            String artifactId = sanitizeArtifactId(artifactIdRaw);
            if (artifactId.isEmpty()) {
                sendPlainText(ex, 400, "artifactId is empty after sanitization");
                return;
            }
            String groupId = sanitizeDottedIdentifier(groupIdRaw);
            if (groupId.isEmpty()) {
                sendPlainText(ex, 400, "groupId is empty after sanitization");
                return;
            }
            String packageName = packageNameRaw != null && !packageNameRaw.isBlank()
                ? sanitizeDottedIdentifier(packageNameRaw)
                : sanitizeDottedIdentifier(groupIdRaw + "." + artifactIdRaw);
            if (packageName.isEmpty()) {
                sendPlainText(ex, 400, "packageName is empty after sanitization");
                return;
            }
            String name = nameRaw.isBlank() ? artifactId : nameRaw;

            List<Dep> deps = new ArrayList<>();
            for (String id : depsRaw.split(",")) {
                id = id.trim();
                if (id.isEmpty()) continue;
                Dep d = DEP_BY_ID.get(id);
                if (d == null) {
                    sendPlainText(ex, 400, "Unknown dependency id: " + id);
                    return;
                }
                if (d.coordFor(boot.major()) == null) {
                    sendPlainText(ex, 400, "Dependency '" + id + "' is not available for Spring Boot " + boot.major() + ".x");
                    return;
                }
                deps.add(d);
            }

            ProjectSpec spec = new ProjectSpec(buildTool, language, boot, javaVersion,
                groupId, artifactId, name, description, packageName, packaging, deps);

            Map<String, String> files = generateFiles(spec);

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            writeZip(buf, artifactId, files);
            byte[] zipBytes = buf.toByteArray();

            ex.getResponseHeaders().set("Content-Type", "application/zip");
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + artifactId + ".zip\"");
            headersSent = true;
            ex.sendResponseHeaders(200, zipBytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(zipBytes);
            }
        } catch (Exception e) {
            if (headersSent) {
                System.err.println("Error after response headers were sent: " + e);
            } else {
                sendPlainText(ex, 500, "Internal error: " + e);
            }
        } finally {
            ex.close();
        }
    }

    static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return map;
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);
            map.put(key, value);
        }
        return map;
    }

    // =====================================================================
    // Sanitization
    // =====================================================================

    static final Set<String> JAVA_KEYWORDS = Set.of(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null",
        "_"
    );

    /** Sanitizes a dotted identifier (groupId or package name) into legal Java package segments. */
    static String sanitizeDottedIdentifier(String raw) {
        String[] segments = raw.trim().toLowerCase().split("\\.");
        List<String> cleaned = new ArrayList<>();
        for (String seg : segments) {
            String s = seg.replaceAll("[^a-z0-9_]", "");
            if (s.isEmpty()) continue;
            if (Character.isDigit(s.charAt(0))) s = "_" + s;
            if (JAVA_KEYWORDS.contains(s)) s = s + "_";
            cleaned.add(s);
        }
        return String.join(".", cleaned);
    }

    static String sanitizeArtifactId(String raw) {
        String s = raw.trim().replaceAll("[^A-Za-z0-9._-]", "");
        // Used verbatim as the zip's root folder name -- a dots-only value (".", "..", "...")
        // must never survive, or zip entries like "../pom.xml" would escape the target directory.
        if (s.matches("\\.+")) {
            return "";
        }
        return s;
    }

    /** Converts an artifactId like "my-cool-app" into "MyCoolApp" for class name prefixes. */
    static String toPascalCase(String artifactId) {
        String[] parts = artifactId.split("[-_.]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        if (sb.isEmpty()) sb.append("App");
        if (Character.isDigit(sb.charAt(0))) sb.insert(0, "App");
        return sb.toString();
    }

    static String xmlEscape(String s) {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    // =====================================================================
    // Project generation
    // =====================================================================

    record ProjectSpec(
        String buildTool, String language, BootVersion boot, String javaVersion,
        String groupId, String artifactId, String name, String description,
        String packageName, String packaging, List<Dep> deps
    ) {
        String appClassName() {
            return toPascalCase(artifactId) + "Application";
        }

        boolean hasWeb() {
            return deps.stream().anyMatch(d -> d.id().equals("web") || d.id().equals("webflux"));
        }

        boolean isKotlin() {
            return language.equals("kotlin");
        }

        boolean isWar() {
            return packaging.equals("war");
        }

        String packagePath() {
            return packageName.replace('.', '/');
        }

        String srcMainRoot() {
            return "src/main/" + (isKotlin() ? "kotlin" : "java") + "/" + packagePath() + "/";
        }

        String srcTestRoot() {
            return "src/test/" + (isKotlin() ? "kotlin" : "java") + "/" + packagePath() + "/";
        }

        String sourceExt() {
            return isKotlin() ? ".kt" : ".java";
        }
    }

    static Map<String, String> generateFiles(ProjectSpec spec) {
        Map<String, String> files = new LinkedHashMap<>();

        if (spec.buildTool().equals("maven")) {
            files.put("pom.xml", buildPomXml(spec));
            files.put("mvnw", MVNW_SH);
            files.put("mvnw.cmd", MVNW_CMD_CRLF);
            files.put(".mvn/wrapper/maven-wrapper.properties", buildMavenWrapperProperties());
        } else if (spec.buildTool().equals("gradle-groovy")) {
            files.put("build.gradle", buildGradleGroovy(spec));
            files.put("settings.gradle", "rootProject.name = '" + spec.artifactId() + "'\n");
        } else {
            files.put("build.gradle.kts", buildGradleKotlin(spec));
            files.put("settings.gradle.kts", "rootProject.name = \"" + spec.artifactId() + "\"\n");
        }

        files.put(".gitignore", buildGitignore(spec));

        files.put(spec.srcMainRoot() + spec.appClassName() + spec.sourceExt(), buildApplicationClass(spec));
        files.put(spec.srcTestRoot() + spec.appClassName() + "Tests" + spec.sourceExt(), buildTestClass(spec));
        files.put("src/main/resources/application.properties", buildApplicationProperties(spec));
        files.put("src/main/resources/static/.gitkeep", "");
        files.put("src/main/resources/templates/.gitkeep", "");

        if (spec.hasWeb()) {
            files.put(spec.srcMainRoot() + "HelloController" + spec.sourceExt(), buildHelloController(spec));
        }
        if (spec.isWar()) {
            files.put(spec.srcMainRoot() + "ServletInitializer" + spec.sourceExt(), buildServletInitializer(spec));
        }

        return files;
    }

    // ---- Maven ----------------------------------------------------------

    // Pinned Maven distribution + wrapper-plugin versions for the generated mvnw/mvnw.cmd.
    // These are independent of the chosen Spring Boot version -- Maven the build tool has its
    // own release line. Bump here when they age; both are verified to exist on Maven Central.
    static final String MAVEN_DISTRIBUTION_VERSION = "3.9.16";
    static final String MAVEN_WRAPPER_VERSION = "3.3.2";

    static String buildMavenWrapperProperties() {
        return "wrapperVersion=" + MAVEN_WRAPPER_VERSION + "\n"
            + "distributionType=script\n"
            + "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/"
            + MAVEN_DISTRIBUTION_VERSION + "/apache-maven-" + MAVEN_DISTRIBUTION_VERSION + "-bin.zip\n"
            + "wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/"
            + MAVEN_WRAPPER_VERSION + "/maven-wrapper-" + MAVEN_WRAPPER_VERSION + ".jar\n";
    }

    // Official Apache Maven Wrapper scripts (jar-less "script" distribution type -- no binary is
    // checked in; mvnw/mvnw.cmd download the small maven-wrapper.jar themselves on first run).
    // Generated verbatim via the locally installed Maven 3.9.16:
    //   mvn -N org.apache.maven.plugins:maven-wrapper-plugin:3.3.2:wrapper -Dtype=script
    static final String MVNW_SH = """
#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
#
# Required ENV vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir
#
# Optional ENV vars
# -----------------
#   MAVEN_OPTS - parameters passed to the Java VM when running Maven
#     e.g. to debug Maven itself, use
#       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
#   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
# ----------------------------------------------------------------------------

if [ -z "$MAVEN_SKIP_RC" ]; then

  if [ -f /usr/local/etc/mavenrc ]; then
    . /usr/local/etc/mavenrc
  fi

  if [ -f /etc/mavenrc ]; then
    . /etc/mavenrc
  fi

  if [ -f "$HOME/.mavenrc" ]; then
    . "$HOME/.mavenrc"
  fi

fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
darwin=false
mingw=false
case "$(uname)" in
CYGWIN*) cygwin=true ;;
MINGW*) mingw=true ;;
Darwin*)
  darwin=true
  # Use /usr/libexec/java_home if available, otherwise fall back to /Library/Java/Home
  # See https://developer.apple.com/library/mac/qa/qa1170/_index.html
  if [ -z "$JAVA_HOME" ]; then
    if [ -x "/usr/libexec/java_home" ]; then
      JAVA_HOME="$(/usr/libexec/java_home)"
      export JAVA_HOME
    else
      JAVA_HOME="/Library/Java/Home"
      export JAVA_HOME
    fi
  fi
  ;;
esac

if [ -z "$JAVA_HOME" ]; then
  if [ -r /etc/gentoo-release ]; then
    JAVA_HOME=$(java-config --jre-home)
  fi
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] \\
    && JAVA_HOME=$(cygpath --unix "$JAVA_HOME")
  [ -n "$CLASSPATH" ] \\
    && CLASSPATH=$(cygpath --path --unix "$CLASSPATH")
fi

# For Mingw, ensure paths are in UNIX format before anything is touched
if $mingw; then
  [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME" ] \\
    && JAVA_HOME="$(
      cd "$JAVA_HOME" || (
        echo "cannot cd into $JAVA_HOME." >&2
        exit 1
      )
      pwd
    )"
fi

if [ -z "$JAVA_HOME" ]; then
  javaExecutable="$(which javac)"
  if [ -n "$javaExecutable" ] && ! [ "$(expr "$javaExecutable" : '\\([^ ]*\\)')" = "no" ]; then
    # readlink(1) is not available as standard on Solaris 10.
    readLink=$(which readlink)
    if [ ! "$(expr "$readLink" : '\\([^ ]*\\)')" = "no" ]; then
      if $darwin; then
        javaHome="$(dirname "$javaExecutable")"
        javaExecutable="$(cd "$javaHome" && pwd -P)/javac"
      else
        javaExecutable="$(readlink -f "$javaExecutable")"
      fi
      javaHome="$(dirname "$javaExecutable")"
      javaHome=$(expr "$javaHome" : '\\(.*\\)/bin')
      JAVA_HOME="$javaHome"
      export JAVA_HOME
    fi
  fi
fi

if [ -z "$JAVACMD" ]; then
  if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD="$(
      \\unset -f command 2>/dev/null
      \\command -v java
    )"
  fi
fi

if [ ! -x "$JAVACMD" ]; then
  echo "Error: JAVA_HOME is not defined correctly." >&2
  echo "  We cannot execute $JAVACMD" >&2
  exit 1
fi

if [ -z "$JAVA_HOME" ]; then
  echo "Warning: JAVA_HOME environment variable is not set." >&2
fi

# traverses directory structure from process work directory to filesystem root
# first directory with .mvn subdirectory is considered project base directory
find_maven_basedir() {
  if [ -z "$1" ]; then
    echo "Path not specified to find_maven_basedir" >&2
    return 1
  fi

  basedir="$1"
  wdir="$1"
  while [ "$wdir" != '/' ]; do
    if [ -d "$wdir"/.mvn ]; then
      basedir=$wdir
      break
    fi
    # workaround for JBEAP-8937 (on Solaris 10/Sparc)
    if [ -d "${wdir}" ]; then
      wdir=$(
        cd "$wdir/.." || exit 1
        pwd
      )
    fi
    # end of workaround
  done
  printf '%s' "$(
    cd "$basedir" || exit 1
    pwd
  )"
}

# concatenates all lines of a file
concat_lines() {
  if [ -f "$1" ]; then
    # Remove \\r in case we run on Windows within Git Bash
    # and check out the repository with auto CRLF management
    # enabled. Otherwise, we may read lines that are delimited with
    # \\r\\n and produce $'-Xarg\\r' rather than -Xarg due to word
    # splitting rules.
    tr -s '\\r\\n' ' ' <"$1"
  fi
}

log() {
  if [ "$MVNW_VERBOSE" = true ]; then
    printf '%s\\n' "$1"
  fi
}

BASE_DIR=$(find_maven_basedir "$(dirname "$0")")
if [ -z "$BASE_DIR" ]; then
  exit 1
fi

MAVEN_PROJECTBASEDIR=${MAVEN_BASEDIR:-"$BASE_DIR"}
export MAVEN_PROJECTBASEDIR
log "$MAVEN_PROJECTBASEDIR"

##########################################################################################
# Extension to allow automatically downloading the maven-wrapper.jar from Maven-central
# This allows using the maven wrapper in projects that prohibit checking in binary data.
##########################################################################################
wrapperJarPath="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
if [ -r "$wrapperJarPath" ]; then
  log "Found $wrapperJarPath"
else
  log "Couldn't find $wrapperJarPath, downloading it ..."

  if [ -n "$MVNW_REPOURL" ]; then
    wrapperUrl="$MVNW_REPOURL/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  else
    wrapperUrl="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  fi
  while IFS="=" read -r key value; do
    # Remove '\\r' from value to allow usage on windows as IFS does not consider '\\r' as a separator ( considers space, tab, new line ('\\n'), and custom '=' )
    safeValue=$(echo "$value" | tr -d '\\r')
    case "$key" in wrapperUrl)
      wrapperUrl="$safeValue"
      break
      ;;
    esac
  done <"$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"
  log "Downloading from: $wrapperUrl"

  if $cygwin; then
    wrapperJarPath=$(cygpath --path --windows "$wrapperJarPath")
  fi

  if command -v wget >/dev/null; then
    log "Found wget ... using wget"
    [ "$MVNW_VERBOSE" = true ] && QUIET="" || QUIET="--quiet"
    if [ -z "$MVNW_USERNAME" ] || [ -z "$MVNW_PASSWORD" ]; then
      wget $QUIET "$wrapperUrl" -O "$wrapperJarPath" || rm -f "$wrapperJarPath"
    else
      wget $QUIET --http-user="$MVNW_USERNAME" --http-password="$MVNW_PASSWORD" "$wrapperUrl" -O "$wrapperJarPath" || rm -f "$wrapperJarPath"
    fi
  elif command -v curl >/dev/null; then
    log "Found curl ... using curl"
    [ "$MVNW_VERBOSE" = true ] && QUIET="" || QUIET="--silent"
    if [ -z "$MVNW_USERNAME" ] || [ -z "$MVNW_PASSWORD" ]; then
      curl $QUIET -o "$wrapperJarPath" "$wrapperUrl" -f -L || rm -f "$wrapperJarPath"
    else
      curl $QUIET --user "$MVNW_USERNAME:$MVNW_PASSWORD" -o "$wrapperJarPath" "$wrapperUrl" -f -L || rm -f "$wrapperJarPath"
    fi
  else
    log "Falling back to using Java to download"
    javaSource="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/MavenWrapperDownloader.java"
    javaClass="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/MavenWrapperDownloader.class"
    # For Cygwin, switch paths to Windows format before running javac
    if $cygwin; then
      javaSource=$(cygpath --path --windows "$javaSource")
      javaClass=$(cygpath --path --windows "$javaClass")
    fi
    if [ -e "$javaSource" ]; then
      if [ ! -e "$javaClass" ]; then
        log " - Compiling MavenWrapperDownloader.java ..."
        ("$JAVA_HOME/bin/javac" "$javaSource")
      fi
      if [ -e "$javaClass" ]; then
        log " - Running MavenWrapperDownloader.java ..."
        ("$JAVA_HOME/bin/java" -cp .mvn/wrapper MavenWrapperDownloader "$wrapperUrl" "$wrapperJarPath") || rm -f "$wrapperJarPath"
      fi
    fi
  fi
fi
##########################################################################################
# End of extension
##########################################################################################

# If specified, validate the SHA-256 sum of the Maven wrapper jar file
wrapperSha256Sum=""
while IFS="=" read -r key value; do
  case "$key" in wrapperSha256Sum)
    wrapperSha256Sum=$value
    break
    ;;
  esac
done <"$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"
if [ -n "$wrapperSha256Sum" ]; then
  wrapperSha256Result=false
  if command -v sha256sum >/dev/null; then
    if echo "$wrapperSha256Sum  $wrapperJarPath" | sha256sum -c >/dev/null 2>&1; then
      wrapperSha256Result=true
    fi
  elif command -v shasum >/dev/null; then
    if echo "$wrapperSha256Sum  $wrapperJarPath" | shasum -a 256 -c >/dev/null 2>&1; then
      wrapperSha256Result=true
    fi
  else
    echo "Checksum validation was requested but neither 'sha256sum' or 'shasum' are available." >&2
    echo "Please install either command, or disable validation by removing 'wrapperSha256Sum' from your maven-wrapper.properties." >&2
    exit 1
  fi
  if [ $wrapperSha256Result = false ]; then
    echo "Error: Failed to validate Maven wrapper SHA-256, your Maven wrapper might be compromised." >&2
    echo "Investigate or delete $wrapperJarPath to attempt a clean download." >&2
    echo "If you updated your Maven version, you need to update the specified wrapperSha256Sum property." >&2
    exit 1
  fi
fi

MAVEN_OPTS="$(concat_lines "$MAVEN_PROJECTBASEDIR/.mvn/jvm.config") $MAVEN_OPTS"

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  [ -n "$JAVA_HOME" ] \\
    && JAVA_HOME=$(cygpath --path --windows "$JAVA_HOME")
  [ -n "$CLASSPATH" ] \\
    && CLASSPATH=$(cygpath --path --windows "$CLASSPATH")
  [ -n "$MAVEN_PROJECTBASEDIR" ] \\
    && MAVEN_PROJECTBASEDIR=$(cygpath --path --windows "$MAVEN_PROJECTBASEDIR")
fi

# Provide a "standardized" way to retrieve the CLI args that will
# work with both Windows and non-Windows executions.
MAVEN_CMD_LINE_ARGS="$MAVEN_CONFIG $*"
export MAVEN_CMD_LINE_ARGS

WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

# shellcheck disable=SC2086 # safe args
exec "$JAVACMD" \\
  $MAVEN_OPTS \\
  $MAVEN_DEBUG_OPTS \\
  -classpath "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar" \\
  "-Dmaven.multiModuleProjectDirectory=${MAVEN_PROJECTBASEDIR}" \\
  ${WRAPPER_LAUNCHER} $MAVEN_CONFIG "$@"
""";

    static final String MVNW_CMD = """
@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM
@REM Required ENV vars:
@REM JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM MAVEN_BATCH_PAUSE - set to 'on' to wait for a keystroke before ending
@REM MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@REM Begin all REM lines with '@' in case MAVEN_BATCH_ECHO is 'on'
@echo off
@REM set title of command window
title %0
@REM enable echoing by setting MAVEN_BATCH_ECHO to 'on'
@if "%MAVEN_BATCH_ECHO%" == "on"  echo %MAVEN_BATCH_ECHO%

@REM set %HOME% to equivalent of $HOME
if "%HOME%" == "" (set "HOME=%HOMEDRIVE%%HOMEPATH%")

@REM Execute a user defined script before this one
if not "%MAVEN_SKIP_RC%" == "" goto skipRcPre
@REM check for pre script, once with legacy .bat ending and once with .cmd ending
if exist "%USERPROFILE%\\mavenrc_pre.bat" call "%USERPROFILE%\\mavenrc_pre.bat" %*
if exist "%USERPROFILE%\\mavenrc_pre.cmd" call "%USERPROFILE%\\mavenrc_pre.cmd" %*
:skipRcPre

@setlocal

set ERROR_CODE=0

@REM To isolate internal variables from possible post scripts, we use another setlocal
@setlocal

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%" == "" goto OkJHome

echo. >&2
echo Error: JAVA_HOME not found in your environment. >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo. >&2
goto error

:OkJHome
if exist "%JAVA_HOME%\\bin\\java.exe" goto init

echo. >&2
echo Error: JAVA_HOME is set to an invalid directory. >&2
echo JAVA_HOME = "%JAVA_HOME%" >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo. >&2
goto error

@REM ==== END VALIDATION ====

:init

@REM Find the project base dir, i.e. the directory that contains the folder ".mvn".
@REM Fallback to current working directory if not found.

set MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
IF NOT "%MAVEN_PROJECTBASEDIR%"=="" goto endDetectBaseDir

set EXEC_DIR=%CD%
set WDIR=%EXEC_DIR%
:findBaseDir
IF EXIST "%WDIR%"\\.mvn goto baseDirFound
cd ..
IF "%WDIR%"=="%CD%" goto baseDirNotFound
set WDIR=%CD%
goto findBaseDir

:baseDirFound
set MAVEN_PROJECTBASEDIR=%WDIR%
cd "%EXEC_DIR%"
goto endDetectBaseDir

:baseDirNotFound
set MAVEN_PROJECTBASEDIR=%EXEC_DIR%
cd "%EXEC_DIR%"

:endDetectBaseDir

IF NOT EXIST "%MAVEN_PROJECTBASEDIR%\\.mvn\\jvm.config" goto endReadAdditionalConfig

@setlocal EnableExtensions EnableDelayedExpansion
for /F "usebackq delims=" %%a in ("%MAVEN_PROJECTBASEDIR%\\.mvn\\jvm.config") do set JVM_CONFIG_MAVEN_PROPS=!JVM_CONFIG_MAVEN_PROPS! %%a
@endlocal & set JVM_CONFIG_MAVEN_PROPS=%JVM_CONFIG_MAVEN_PROPS%

:endReadAdditionalConfig

SET MAVEN_JAVA_EXE="%JAVA_HOME%\\bin\\java.exe"
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\\.mvn\\wrapper\\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

set WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\\.mvn\\wrapper\\maven-wrapper.properties") DO (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
)

@REM Extension to allow automatically downloading the maven-wrapper.jar from Maven-central
@REM This allows using the maven wrapper in projects that prohibit checking in binary data.
if exist %WRAPPER_JAR% (
    if "%MVNW_VERBOSE%" == "true" (
        echo Found %WRAPPER_JAR%
    )
) else (
    if not "%MVNW_REPOURL%" == "" (
        SET WRAPPER_URL="%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
    )
    if "%MVNW_VERBOSE%" == "true" (
        echo Couldn't find %WRAPPER_JAR%, downloading it ...
        echo Downloading from: %WRAPPER_URL%
    )

    powershell -Command "&{"^
		"$webclient = new-object System.Net.WebClient;"^
		"if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {"^
		"$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
		"}"^
		"[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"^
		"}"
    if "%MVNW_VERBOSE%" == "true" (
        echo Finished downloading %WRAPPER_JAR%
    )
)
@REM End of extension

@REM If specified, validate the SHA-256 sum of the Maven wrapper jar file
SET WRAPPER_SHA_256_SUM=""
FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\\.mvn\\wrapper\\maven-wrapper.properties") DO (
    IF "%%A"=="wrapperSha256Sum" SET WRAPPER_SHA_256_SUM=%%B
)
IF NOT %WRAPPER_SHA_256_SUM%=="" (
    powershell -Command "&{"^
       "Import-Module $PSHOME\\Modules\\Microsoft.PowerShell.Utility -Function Get-FileHash;"^
       "$hash = (Get-FileHash \\"%WRAPPER_JAR%\\" -Algorithm SHA256).Hash.ToLower();"^
       "If('%WRAPPER_SHA_256_SUM%' -ne $hash){"^
       "  Write-Error 'Error: Failed to validate Maven wrapper SHA-256, your Maven wrapper might be compromised.';"^
       "  Write-Error 'Investigate or delete %WRAPPER_JAR% to attempt a clean download.';"^
       "  Write-Error 'If you updated your Maven version, you need to update the specified wrapperSha256Sum property.';"^
       "  exit 1;"^
       "}"^
       "}"
    if ERRORLEVEL 1 goto error
)

@REM Provide a "standardized" way to retrieve the CLI args that will
@REM work with both Windows and non-Windows executions.
set MAVEN_CMD_LINE_ARGS=%*

%MAVEN_JAVA_EXE% ^
  %JVM_CONFIG_MAVEN_PROPS% ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath %WRAPPER_JAR% ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

if not "%MAVEN_SKIP_RC%"=="" goto skipRcPost
@REM check for post script, once with legacy .bat ending and once with .cmd ending
if exist "%USERPROFILE%\\mavenrc_post.bat" call "%USERPROFILE%\\mavenrc_post.bat"
if exist "%USERPROFILE%\\mavenrc_post.cmd" call "%USERPROFILE%\\mavenrc_post.cmd"
:skipRcPost

@REM pause the script if MAVEN_BATCH_PAUSE is set to 'on'
if "%MAVEN_BATCH_PAUSE%"=="on" pause

if "%MAVEN_TERMINATE_CMD%"=="on" exit %ERROR_CODE%

cmd /C exit /B %ERROR_CODE%
""";

    // mvnw.cmd is a Windows batch file; ship it with the conventional CRLF endings (MVNW_CMD
    // above only carries LF). Precomputed once at class-init rather than per-request.
    static final String MVNW_CMD_CRLF = MVNW_CMD.replace("\n", "\r\n");

    static String buildPomXml(ProjectSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        sb.append("  <modelVersion>4.0.0</modelVersion>\n");
        sb.append("  <parent>\n");
        sb.append("    <groupId>org.springframework.boot</groupId>\n");
        sb.append("    <artifactId>spring-boot-starter-parent</artifactId>\n");
        sb.append("    <version>").append(spec.boot().version()).append("</version>\n");
        sb.append("    <relativePath/>\n");
        sb.append("  </parent>\n");
        sb.append("  <groupId>").append(xmlEscape(spec.groupId())).append("</groupId>\n");
        sb.append("  <artifactId>").append(xmlEscape(spec.artifactId())).append("</artifactId>\n");
        sb.append("  <version>0.0.1-SNAPSHOT</version>\n");
        sb.append("  <name>").append(xmlEscape(spec.name())).append("</name>\n");
        sb.append("  <description>").append(xmlEscape(spec.description())).append("</description>\n");
        if (spec.isWar()) {
            sb.append("  <packaging>war</packaging>\n");
        }
        sb.append("  <properties>\n");
        sb.append("    <java.version>").append(spec.javaVersion()).append("</java.version>\n");
        if (spec.isKotlin()) {
            sb.append("    <kotlin.version>").append(spec.boot().kotlinVersion()).append("</kotlin.version>\n");
        }
        sb.append("  </properties>\n");
        sb.append("  <dependencies>\n");
        for (Dep d : spec.deps()) {
            appendPomDependency(sb, d.coordFor(spec.boot().major()));
        }
        if (spec.isKotlin()) {
            appendPomDependency(sb, Coord.managed("org.jetbrains.kotlin", "kotlin-reflect"));
            appendPomDependency(sb, Coord.managed("com.fasterxml.jackson.module", "jackson-module-kotlin"));
        }
        if (spec.isWar()) {
            appendPomDependency(sb, Coord.managed("org.springframework.boot", "spring-boot-starter-tomcat", "provided"));
        }
        appendPomDependency(sb, Coord.managed("org.springframework.boot", "spring-boot-starter-test", "test"));
        sb.append("  </dependencies>\n");
        sb.append("  <build>\n");
        if (spec.isKotlin()) {
            sb.append("    <sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>\n");
            sb.append("    <testSourceDirectory>${project.basedir}/src/test/kotlin</testSourceDirectory>\n");
        }
        sb.append("    <plugins>\n");
        sb.append("      <plugin>\n");
        sb.append("        <groupId>org.springframework.boot</groupId>\n");
        sb.append("        <artifactId>spring-boot-maven-plugin</artifactId>\n");
        sb.append("      </plugin>\n");
        if (spec.isKotlin()) {
            sb.append("      <plugin>\n");
            sb.append("        <groupId>org.jetbrains.kotlin</groupId>\n");
            sb.append("        <artifactId>kotlin-maven-plugin</artifactId>\n");
            sb.append("        <configuration>\n");
            sb.append("          <args>\n");
            sb.append("            <arg>-Xjsr305=strict</arg>\n");
            sb.append("          </args>\n");
            sb.append("          <compilerPlugins>\n");
            sb.append("            <plugin>spring</plugin>\n");
            sb.append("          </compilerPlugins>\n");
            sb.append("        </configuration>\n");
            sb.append("        <dependencies>\n");
            sb.append("          <dependency>\n");
            sb.append("            <groupId>org.jetbrains.kotlin</groupId>\n");
            sb.append("            <artifactId>kotlin-maven-allopen</artifactId>\n");
            sb.append("            <version>${kotlin.version}</version>\n");
            sb.append("          </dependency>\n");
            sb.append("        </dependencies>\n");
            sb.append("      </plugin>\n");
        }
        sb.append("    </plugins>\n");
        sb.append("  </build>\n");
        sb.append("</project>\n");
        return sb.toString();
    }

    static void appendPomDependency(StringBuilder sb, Coord c) {
        sb.append("    <dependency>\n");
        sb.append("      <groupId>").append(xmlEscape(c.groupId())).append("</groupId>\n");
        sb.append("      <artifactId>").append(xmlEscape(c.artifactId())).append("</artifactId>\n");
        if (c.version() != null) {
            sb.append("      <version>").append(xmlEscape(c.version())).append("</version>\n");
        }
        if (c.scope() != null) {
            sb.append("      <scope>").append(c.scope()).append("</scope>\n");
        }
        if (c.optional()) {
            sb.append("      <optional>true</optional>\n");
        }
        sb.append("    </dependency>\n");
    }

    // ---- Gradle (Groovy DSL) --------------------------------------------

    static String buildGradleGroovy(ProjectSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("plugins {\n");
        sb.append("    id 'java'\n");
        if (spec.isKotlin()) {
            sb.append("    id 'org.jetbrains.kotlin.jvm' version '").append(spec.boot().kotlinVersion()).append("'\n");
            sb.append("    id 'org.jetbrains.kotlin.plugin.spring' version '").append(spec.boot().kotlinVersion()).append("'\n");
        }
        sb.append("    id 'org.springframework.boot' version '").append(spec.boot().version()).append("'\n");
        sb.append("    id 'io.spring.dependency-management' version '1.1.7'\n");
        if (spec.isWar()) {
            sb.append("    id 'war'\n");
        }
        sb.append("}\n\n");
        sb.append("group = '").append(spec.groupId()).append("'\n");
        sb.append("version = '0.0.1-SNAPSHOT'\n\n");
        sb.append("java {\n");
        sb.append("    sourceCompatibility = '").append(spec.javaVersion()).append("'\n");
        sb.append("}\n\n");
        sb.append("repositories {\n");
        sb.append("    mavenCentral()\n");
        sb.append("}\n\n");
        sb.append("dependencies {\n");
        for (Dep d : spec.deps()) {
            appendGradleDependency(sb, d, d.coordFor(spec.boot().major()), false);
        }
        if (spec.isKotlin()) {
            sb.append("    implementation 'org.jetbrains.kotlin:kotlin-reflect'\n");
            sb.append("    implementation 'com.fasterxml.jackson.module:jackson-module-kotlin'\n");
        }
        if (spec.isWar()) {
            sb.append("    providedRuntime 'org.springframework.boot:spring-boot-starter-tomcat'\n");
        }
        sb.append("    testImplementation 'org.springframework.boot:spring-boot-starter-test'\n");
        sb.append("}\n\n");
        if (spec.isKotlin()) {
            sb.append("tasks.named('compileKotlin') {\n");
            sb.append("    compilerOptions {\n");
            sb.append("        freeCompilerArgs.add('-Xjsr305=strict')\n");
            sb.append("    }\n");
            sb.append("}\n\n");
        }
        sb.append("tasks.named('test') {\n");
        sb.append("    useJUnitPlatform()\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ---- Gradle (Kotlin DSL) ---------------------------------------------

    static String buildGradleKotlin(ProjectSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("plugins {\n");
        sb.append("    java\n");
        if (spec.isKotlin()) {
            sb.append("    kotlin(\"jvm\") version \"").append(spec.boot().kotlinVersion()).append("\"\n");
            sb.append("    kotlin(\"plugin.spring\") version \"").append(spec.boot().kotlinVersion()).append("\"\n");
        }
        sb.append("    id(\"org.springframework.boot\") version \"").append(spec.boot().version()).append("\"\n");
        sb.append("    id(\"io.spring.dependency-management\") version \"1.1.7\"\n");
        if (spec.isWar()) {
            sb.append("    war\n");
        }
        sb.append("}\n\n");
        sb.append("group = \"").append(spec.groupId()).append("\"\n");
        sb.append("version = \"0.0.1-SNAPSHOT\"\n\n");
        sb.append("java {\n");
        sb.append("    sourceCompatibility = JavaVersion.VERSION_").append(spec.javaVersion()).append("\n");
        sb.append("}\n\n");
        sb.append("repositories {\n");
        sb.append("    mavenCentral()\n");
        sb.append("}\n\n");
        sb.append("dependencies {\n");
        for (Dep d : spec.deps()) {
            appendGradleDependency(sb, d, d.coordFor(spec.boot().major()), true);
        }
        if (spec.isKotlin()) {
            sb.append("    implementation(\"org.jetbrains.kotlin:kotlin-reflect\")\n");
            sb.append("    implementation(\"com.fasterxml.jackson.module:jackson-module-kotlin\")\n");
        }
        if (spec.isWar()) {
            sb.append("    providedRuntime(\"org.springframework.boot:spring-boot-starter-tomcat\")\n");
        }
        sb.append("    testImplementation(\"org.springframework.boot:spring-boot-starter-test\")\n");
        sb.append("}\n\n");
        if (spec.isKotlin()) {
            sb.append("tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {\n");
            sb.append("    compilerOptions {\n");
            sb.append("        freeCompilerArgs.add(\"-Xjsr305=strict\")\n");
            sb.append("    }\n");
            sb.append("}\n\n");
        }
        sb.append("tasks.withType<Test> {\n");
        sb.append("    useJUnitPlatform()\n");
        sb.append("}\n");
        return sb.toString();
    }

    /** Determines the right Gradle configuration (implementation/runtimeOnly/etc.) for a dependency. */
    static void appendGradleDependency(StringBuilder sb, Dep d, Coord c, boolean kotlinDsl) {
        String ga = c.groupId() + ":" + c.artifactId() + (c.version() != null ? ":" + c.version() : "");
        if (d.id().equals("lombok")) {
            emitGradleLine(sb, "compileOnly", ga, kotlinDsl);
            emitGradleLine(sb, "annotationProcessor", ga, kotlinDsl);
            return;
        }
        if (d.id().equals("configuration-processor")) {
            emitGradleLine(sb, "annotationProcessor", ga, kotlinDsl);
            return;
        }
        if (d.id().equals("devtools")) {
            emitGradleLine(sb, "developmentOnly", ga, kotlinDsl);
            return;
        }
        if ("runtime".equals(c.scope())) {
            emitGradleLine(sb, "runtimeOnly", ga, kotlinDsl);
            return;
        }
        emitGradleLine(sb, "implementation", ga, kotlinDsl);
    }

    static void emitGradleLine(StringBuilder sb, String config, String ga, boolean kotlinDsl) {
        if (kotlinDsl) {
            sb.append("    ").append(config).append("(\"").append(ga).append("\")\n");
        } else {
            sb.append("    ").append(config).append(" '").append(ga).append("'\n");
        }
    }

    // ---- Source templates -------------------------------------------------

    static String buildApplicationClass(ProjectSpec spec) {
        String cls = spec.appClassName();
        if (spec.isKotlin()) {
            return "package " + spec.packageName() + "\n\n"
                + "import org.springframework.boot.autoconfigure.SpringBootApplication\n"
                + "import org.springframework.boot.runApplication\n\n"
                + "@SpringBootApplication\n"
                + "class " + cls + "\n\n"
                + "fun main(args: Array<String>) {\n"
                + "    runApplication<" + cls + ">(*args)\n"
                + "}\n";
        }
        return "package " + spec.packageName() + ";\n\n"
            + "import org.springframework.boot.SpringApplication;\n"
            + "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n"
            + "@SpringBootApplication\n"
            + "public class " + cls + " {\n\n"
            + "    public static void main(String[] args) {\n"
            + "        SpringApplication.run(" + cls + ".class, args);\n"
            + "    }\n\n"
            + "}\n";
    }

    static String buildTestClass(ProjectSpec spec) {
        String cls = spec.appClassName() + "Tests";
        if (spec.isKotlin()) {
            return "package " + spec.packageName() + "\n\n"
                + "import org.junit.jupiter.api.Test\n"
                + "import org.springframework.boot.test.context.SpringBootTest\n\n"
                + "@SpringBootTest\n"
                + "class " + cls + " {\n\n"
                + "    @Test\n"
                + "    fun contextLoads() {\n"
                + "    }\n\n"
                + "}\n";
        }
        return "package " + spec.packageName() + ";\n\n"
            + "import org.junit.jupiter.api.Test;\n"
            + "import org.springframework.boot.test.context.SpringBootTest;\n\n"
            + "@SpringBootTest\n"
            + "class " + cls + " {\n\n"
            + "    @Test\n"
            + "    void contextLoads() {\n"
            + "    }\n\n"
            + "}\n";
    }

    static String buildHelloController(ProjectSpec spec) {
        if (spec.isKotlin()) {
            return "package " + spec.packageName() + "\n\n"
                + "import org.springframework.web.bind.annotation.GetMapping\n"
                + "import org.springframework.web.bind.annotation.RestController\n\n"
                + "@RestController\n"
                + "class HelloController {\n\n"
                + "    @GetMapping(\"/\")\n"
                + "    fun hello(): String {\n"
                + "        return \"Hello, Spring Boot!\"\n"
                + "    }\n\n"
                + "}\n";
        }
        return "package " + spec.packageName() + ";\n\n"
            + "import org.springframework.web.bind.annotation.GetMapping;\n"
            + "import org.springframework.web.bind.annotation.RestController;\n\n"
            + "@RestController\n"
            + "public class HelloController {\n\n"
            + "    @GetMapping(\"/\")\n"
            + "    public String hello() {\n"
            + "        return \"Hello, Spring Boot!\";\n"
            + "    }\n\n"
            + "}\n";
    }

    static String buildServletInitializer(ProjectSpec spec) {
        String appCls = spec.appClassName();
        if (spec.isKotlin()) {
            return "package " + spec.packageName() + "\n\n"
                + "import org.springframework.boot.builder.SpringApplicationBuilder\n"
                + "import org.springframework.boot.web.servlet.support.SpringBootServletInitializer\n\n"
                + "class ServletInitializer : SpringBootServletInitializer() {\n\n"
                + "    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {\n"
                + "        return application.sources(" + appCls + "::class.java)\n"
                + "    }\n\n"
                + "}\n";
        }
        return "package " + spec.packageName() + ";\n\n"
            + "import org.springframework.boot.builder.SpringApplicationBuilder;\n"
            + "import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;\n\n"
            + "public class ServletInitializer extends SpringBootServletInitializer {\n\n"
            + "    @Override\n"
            + "    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {\n"
            + "        return application.sources(" + appCls + ".class);\n"
            + "    }\n\n"
            + "}\n";
    }

    static String buildApplicationProperties(ProjectSpec spec) {
        return "spring.application.name=" + spec.artifactId() + "\n";
    }

    // Content matches the real start.spring.io generator (GitProjectGenerationConfiguration in
    // spring-io/initializr), which branches this file by build tool -- Maven ignores target/ plus
    // the wrapper jar it never checks in, Gradle ignores .gradle/build/ and adds the IntelliJ
    // out/ and STS bin/ lines that only apply to Gradle's default output layout. The upstream
    // Gradle section also excepts gradle/wrapper/gradle-wrapper.jar from a broader ignore, but
    // this tool doesn't generate a Gradle wrapper (out of scope, see CLAUDE.md), so that line is
    // dropped rather than reference a file that will never exist.
    static String buildGitignore(ProjectSpec spec) {
        String buildToolSection = spec.buildTool().equals("maven")
            ? """
                target/
                .mvn/wrapper/maven-wrapper.jar
                !**/src/main/**/target/
                !**/src/test/**/target/
                """
            : """
                .gradle
                build/
                !**/src/main/**/build/
                !**/src/test/**/build/
                """;

        String stsSection = spec.buildTool().equals("maven")
            ? """
                ### STS ###
                .apt_generated
                .classpath
                .factorypath
                .project
                .settings
                .springBeans
                .sts4-cache
                """
            : """
                ### STS ###
                .apt_generated
                .classpath
                .factorypath
                .project
                .settings
                .springBeans
                .sts4-cache
                bin/
                !**/src/main/**/bin/
                !**/src/test/**/bin/
                """;

        String intellijSection = spec.buildTool().equals("maven")
            ? """
                ### IntelliJ IDEA ###
                .idea
                *.iws
                *.iml
                *.ipr
                """
            : """
                ### IntelliJ IDEA ###
                .idea
                *.iws
                *.iml
                *.ipr
                out/
                !**/src/main/**/out/
                !**/src/test/**/out/
                """;

        String netBeansSection = spec.buildTool().equals("maven")
            ? """
                ### NetBeans ###
                /nbproject/private/
                /nbbuild/
                /dist/
                /nbdist/
                /.nb-gradle/
                build/
                !**/src/main/**/build/
                !**/src/test/**/build/
                """
            : """
                ### NetBeans ###
                /nbproject/private/
                /nbbuild/
                /dist/
                /nbdist/
                /.nb-gradle/
                """;

        return buildToolSection + "\n" + stsSection + "\n" + intellijSection + "\n" + netBeansSection
            + "\n### VS Code ###\n.vscode/\n";
    }

    // =====================================================================
    // Zip writer
    // =====================================================================

    static void writeZip(OutputStream out, String rootDirName, Map<String, String> files) throws IOException {
        // Note: java.util.zip.ZipEntry has no public API for the Unix executable bit, so mvnw
        // loses +x on extraction on macOS/Linux (harmless on Windows). Run `chmod +x mvnw` once
        // there; pulling in a zip library just for this bit would violate the no-downloads goal.
        try (ZipOutputStream zos = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            // Deterministic ordering, and directory entries first for a tidy archive.
            for (Map.Entry<String, String> entry : new TreeMap<>(files).entrySet()) {
                String entryPath = rootDirName + "/" + entry.getKey();
                byte[] content = entry.getValue().getBytes(StandardCharsets.UTF_8);
                ZipEntry ze = new ZipEntry(entryPath);
                ze.setTime(0); // deterministic output
                zos.putNextEntry(ze);
                zos.write(content);
                zos.closeEntry();
            }
        }
    }
}
