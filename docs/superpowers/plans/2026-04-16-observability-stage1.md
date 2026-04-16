# Observability Stage 1 — OTel + Actuator + ECS Logging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Lay the observability foundation by adding OpenTelemetry auto-instrumentation, exposing Actuator health/metrics on a dedicated management port, and switching console logging to ECS structured JSON — all with zero Java code changes.

**Architecture:** `spring-boot-starter-opentelemetry` and `spring-boot-starter-web` are added via the root `configureByLabel("spring")` block so every Spring module inherits them. The bot application's main port (8080) runs Embedded Tomcat (mostly idle — Discord traffic goes through JDA WebSocket), while management endpoints are isolated on port 8081. OTLP export URLs are environment-variable driven (`OTEL_EXPORTER_OTLP_ENDPOINT`) for Stage 2 LGTM integration without code changes. The logback-spring.xml console appender is switched to `StructuredLogEncoder(ecs)` while file appenders keep human-readable text format.

**Tech Stack:** Spring Boot 4.0.4, OpenTelemetry (via Spring Boot starter), Micrometer, Actuator, Logback (ECS), Docker Compose

**Design Spec:** `docs/superpowers/specs/2026-04-16-observability-stage1-design.md`

---

## File Structure

**Files modified:**
- `gradle/libs.versions.toml` — 2 library aliases added
- `build.gradle.kts` — 2 dependencies added to `configureByLabel("spring")`
- `runtime/cfg/application.yml` — `spring.application.name`, `server.*`, `management.*` blocks added
- `runtime/cfg/logback-spring.xml` — console appender encoder replaced with ECS
- `deploy/docker-compose.yml` — ports, healthcheck, environment variables added

**Files NOT changed:**
- Any Java source code (zero code changes — purely dependency + config)
- Test files (existing E2E test uses `@SpringBootTest` with mock web environment by default, compatible with web starter)
- `boot/build.gradle.kts` — no module-specific dependency needed for Stage 1

---

## Task 1: Add dependencies

Add `spring-boot-starter-web` and `spring-boot-starter-opentelemetry` to the shared Spring module configuration so all Spring-labeled modules (boot, configuration, adapter-bot, adapter-persistence) gain web context and OTel auto-instrumentation.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`

- [ ] **Step 1: Verify green baseline**

```bash
cd /home/kshull/project/kairyx/java/KorConverterBot-SpringBoot/.worktrees/feature-observability-stage1
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew build
```

Expected: BUILD SUCCESSFUL. If this fails, STOP and report BLOCKED.

- [ ] **Step 2: Add library aliases to libs.versions.toml**

Add two lines after the existing `springframework-boot-starter-jooq` entry (around line 39):

```toml
springframework-boot-starter-web = { group = "org.springframework.boot", name = "spring-boot-starter-web" }
springframework-boot-starter-opentelemetry = { group = "org.springframework.boot", name = "spring-boot-starter-opentelemetry" }
```

These use Spring Boot BOM version management (no explicit version needed).

- [ ] **Step 3: Add dependencies to `configureByLabel("spring")` in build.gradle.kts**

In the root `build.gradle.kts`, inside `configureByLabel("spring") { dependencies { ... } }`, add two lines after the existing `starter-actuator` line:

```kotlin
configureByLabel("spring") {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    dependencies {
        implementation(rootProject.libs.springframework.boot.starter)
        implementation(rootProject.libs.springframework.boot.starter.web)           // NEW
        implementation(rootProject.libs.springframework.boot.starter.opentelemetry) // NEW
        implementation(rootProject.libs.springframework.boot.starter.validation)
        implementation(rootProject.libs.springframework.boot.starter.actuator)
        implementation(rootProject.libs.springframework.boot.starter.json)

        testImplementation(rootProject.libs.springframework.boot.starter.test)
    }
}
```

- [ ] **Step 4: Verify dependencies resolve and tests pass**

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew build
```

Expected: BUILD SUCCESSFUL. The `spring-boot-starter-web` adds Embedded Tomcat but existing tests should still pass — `IgnoreUserE2ETest` uses `@SpringBootTest(classes = ...)` which defaults to `WebEnvironment.MOCK` (no real port opened).

If the E2E test fails with a port conflict or web context issue, add `webEnvironment = SpringBootTest.WebEnvironment.NONE` to `IgnoreUserE2ETest`'s `@SpringBootTest` annotation. But try without first.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts
git commit -m "$(cat <<'EOF'
build: add spring-boot-starter-web and spring-boot-starter-opentelemetry

Stage 1 of the 3-stage observability roadmap per purist-ddd-playbook
Part 10. Adds web (for management-only port 8081 Actuator endpoints)
and OpenTelemetry (Micrometer -> OTLP auto-export for metrics, traces,
logs) to the shared configureByLabel("spring") block so all Spring
modules inherit OTel auto-instrumentation. No Java code changes — the
starters activate via Spring Boot autoconfig. Tomcat starts on 8080
(idle for bot traffic) with management isolated to 8081 (configured
in application.yml in the next commit).

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Update application.yml

Add `spring.application.name`, `server.port`, the full `management.*` block (Actuator endpoints, OTLP export URLs, sampling), and update the logging section for ECS compatibility.

**Files:**
- Modify: `runtime/cfg/application.yml`

- [ ] **Step 1: Replace runtime/cfg/application.yml with the full target content**

Full replacement content:

```yaml
spring:
  application:
    name: kor-converter-bot
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER_NAME}
    password: ${DB_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 2
    baseline-description: "legacy JPA schema snapshot"

server:
  port: 8080

management:
  server:
    port: 8081
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when-authorized
  otlp:
    metrics:
      export:
        url: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}/v1/metrics
    tracing:
      export:
        url: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}/v1/traces
    logging:
      export:
        url: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}/v1/logs
  tracing:
    sampling:
      probability: ${OTEL_SAMPLING_PROBABILITY:1.0}

logging:
  structured:
    format:
      console: ecs
  level:
    root: INFO

bot:
  token: ${DISCORD_BOT_TOKEN}
```

Key changes from the old file:
- Added: `spring.application.name` (OTel `service.name`)
- Added: `server.port: 8080`
- Added: entire `management.*` block (port 8081, endpoint exposure, OTLP URLs with env-var fallbacks, sampling)
- Changed: `logging.level.root: INFO` moved under `logging.structured.format.console: ecs` block
- Preserved: all existing `datasource`, `flyway`, `bot` sections unchanged

The `logging.structured.format.console: ecs` is a fallback — it only takes effect when there is no custom logback-spring.xml. Since the project has one, Task 3 handles the actual ECS switch. Including it here ensures ECS works if the xml is ever removed.

- [ ] **Step 2: Commit**

```bash
git add runtime/cfg/application.yml
git commit -m "$(cat <<'EOF'
config(runtime): add management port, OTLP export, and ECS logging config

Configures Actuator on dedicated management port 8081 (isolated from the
bot's idle Tomcat on 8080) exposing health, info, metrics, and prometheus
endpoints. OTLP export URLs use OTEL_EXPORTER_OTLP_ENDPOINT env var with
localhost:4318 fallback so Stage 2 LGTM connects without code changes.
Sampling probability defaults to 1.0 (100%) overridable via env var for
production. Adds logging.structured.format.console: ecs as a fallback for
environments without the custom logback-spring.xml. spring.application.name
sets the OTel service.name.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Update logback-spring.xml for ECS

Replace the console appender's text pattern encoder with Spring Boot 4's `StructuredLogEncoder` in ECS format. File appenders keep their human-readable text patterns for local log file debugging.

**Files:**
- Modify: `runtime/cfg/logback-spring.xml`

- [ ] **Step 1: Replace the console appender encoder**

In `runtime/cfg/logback-spring.xml`, replace lines 5-10 (the console appender):

Old:
```xml
  <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <Pattern>%date{ISO8601} ${TIMEZONE:-UNDEFINED} %highlight(%-6level) --- [%20.20t] %cyan(%20.20logger{16}:%-3line) : %msg %kvp{NONE}%n
      </Pattern>
    </encoder>
  </appender>
```

New:
```xml
  <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="org.springframework.boot.logging.logback.StructuredLogEncoder">
      <format>ecs</format>
    </encoder>
  </appender>
```

This produces JSON output with automatic traceId/spanId fields when OTel is active. Example:
```json
{"@timestamp":"2026-04-16T10:30:45.123Z","log":{"level":"INFO","logger":"c.e.Service"},"message":"...","traceId":"0af765...","ecs":{"version":"8.11"}}
```

The `TimeZoneDefiner`'s `TIMEZONE` variable is no longer used by the console appender, but the `file` and `file-warn` appenders still reference it — no removal needed.

- [ ] **Step 2: Verify the build still passes**

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew build
```

Expected: BUILD SUCCESSFUL. The logback-spring.xml is only loaded at runtime (via `-Dlogging.config=file:./cfg/logback-spring.xml`), so compile/test time doesn't exercise it. But `./gradlew build` confirms no other breakage.

If `StructuredLogEncoder` does not exist in Spring Boot 4.0.4's logback integration (class not found at runtime), the fallback is to use `net.logstash.logback:logstash-logback-encoder` with `LogstashEncoder` — but try the Spring Boot native approach first.

- [ ] **Step 3: Commit**

```bash
git add runtime/cfg/logback-spring.xml
git commit -m "$(cat <<'EOF'
config(logging): switch console appender to ECS structured JSON

Replaces the text-pattern console encoder with Spring Boot 4's
StructuredLogEncoder in ECS (Elastic Common Schema) format. Console
output becomes JSON with automatic traceId/spanId correlation from
OpenTelemetry MDC injection. File appenders (file, file-warn) keep
their human-readable text patterns for local debugging.

Docker stdout captures the ECS JSON, making it directly ingestible by
Grafana Loki, Elasticsearch, or any ECS-compatible log backend without
a parsing pipeline. Stage 2 (LGTM) will consume these logs.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Update docker-compose.yml

Expose the management port, add Docker HEALTHCHECK, and wire through OTLP environment variables.

**Files:**
- Modify: `deploy/docker-compose.yml`

- [ ] **Step 1: Replace deploy/docker-compose.yml with the full target content**

Full replacement content:

```yaml

services:
  bot:
    container_name: kor-converter-bot
    image: kor-bot-spring:latest
    volumes:
      - ./cfg:/app/cfg
      - ./log:/app/log
    restart: always
    ports:
      - "8081:8081"
    networks:
      - converter_backend
    environment:
      TZ: "Asia/Seoul"
      DB_HOST: $DB_HOST
      DB_PORT: $DB_PORT
      DB_NAME: $DB_NAME
      DB_USER_NAME: $DB_USER_NAME
      DB_PASSWORD: $DB_PASSWORD
      DISCORD_BOT_TOKEN: $DISCORD_BOT_TOKEN
      OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT:-}
      OTEL_SAMPLING_PROBABILITY: ${OTEL_SAMPLING_PROBABILITY:-1.0}
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s

networks:
  converter_backend:
    external: true
```

Changes from the old file:
- Added: `ports: ["8081:8081"]` — exposes management port to host
- Added: `OTEL_EXPORTER_OTLP_ENDPOINT` env var (empty default = OTel tries localhost:4318, fails gracefully)
- Added: `OTEL_SAMPLING_PROBABILITY` env var (1.0 default)
- Added: `healthcheck` — uses `wget` instead of `curl` because `amazoncorretto:25-alpine` base image ships with BusyBox `wget` but not `curl`
- All existing entries preserved (volumes, networks, existing env vars)

Why `wget --spider` instead of `curl`:
- JIB base image `amazoncorretto:25.0.1-alpine` is Alpine-based
- Alpine ships BusyBox which includes `wget` but NOT `curl`
- `wget --no-verbose --tries=1 --spider` makes an HTTP HEAD request and exits 0 on 200, 1 on failure
- `CMD-SHELL` form (instead of `CMD` exec form) is needed for the `||` shell operator

- [ ] **Step 2: Commit**

```bash
git add deploy/docker-compose.yml
git commit -m "$(cat <<'EOF'
config(deploy): expose management port and add Docker HEALTHCHECK

Exposes port 8081 (Actuator management-only, isolated from bot's 8080)
and adds a Docker HEALTHCHECK polling /actuator/health every 30 seconds
using wget (available in the Alpine-based JIB image; curl is not).
Wires OTEL_EXPORTER_OTLP_ENDPOINT and OTEL_SAMPLING_PROBABILITY as
environment variables for Stage 2 LGTM integration — empty OTLP endpoint
causes OTel to attempt localhost:4318 and fail gracefully (no app crash).

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Full build verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full project build**

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew build
```

Expected: BUILD SUCCESSFUL. All modules compile, all tests pass (domain, application, adapter-persistence, boot).

If `IgnoreUserE2ETest` fails with a web context issue (port conflict, servlet context initialization error):
1. Open `korConverter/boot/src/test/java/org/specter/converter/boot/IgnoreUserE2ETest.java`
2. Change `@SpringBootTest(classes = ...)` to `@SpringBootTest(classes = ..., webEnvironment = SpringBootTest.WebEnvironment.NONE)`
3. Re-run and commit the fix

- [ ] **Step 2: Verify the dependency tree includes OTel and management modules**

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :boot:dependencies --configuration runtimeClasspath 2>&1 | grep -E "spring-boot-(starter-web|starter-opentelemetry|actuator)" | head -10
```

Expected output should include:
- `spring-boot-starter-web`
- `spring-boot-starter-opentelemetry`
- `spring-boot-starter-actuator` (already present)

- [ ] **Step 3: No commit needed (verification only)**

If Step 1 required a test fix, commit it:

```bash
git add korConverter/boot/src/test/java/org/specter/converter/boot/IgnoreUserE2ETest.java
git commit -m "test(boot): add WebEnvironment.NONE to E2E test for web starter compatibility"
```

Otherwise skip.

---

## Verification Summary

After all tasks:

1. `git log --oneline ba1e29b..HEAD` shows 4 new commits (build deps, application.yml, logback-spring.xml, docker-compose.yml), optionally a 5th test fix.
2. `./gradlew build` passes cleanly.
3. Dependency tree includes `spring-boot-starter-web`, `spring-boot-starter-opentelemetry`, `spring-boot-starter-actuator`.
4. `runtime/cfg/application.yml` has `management.server.port: 8081`, OTLP URLs, ECS logging config.
5. `runtime/cfg/logback-spring.xml` console appender uses `StructuredLogEncoder` with `ecs` format.
6. `deploy/docker-compose.yml` exposes port 8081, has HEALTHCHECK, passes OTLP env vars.
7. No Java source code was changed.

## Smoke Test (manual, post-deploy)

After building a JIB image and running the container:

1. `wget -qO- http://localhost:8081/actuator/health` → `{"status":"UP"}`
2. `wget -qO- http://localhost:8081/actuator/info` → service info JSON
3. Container logs (`docker logs <container>`) show ECS JSON format
4. `docker inspect <container> --format='{{json .State.Health}}'` → `"Status":"healthy"`
5. OTLP export logs show connection attempts to `localhost:4318` (expected to fail without LGTM — graceful degradation, no crash)

## Follow-up — Stage 2 (LGTM)

Stage 2 will add `grafana/otel-lgtm:latest` container to docker-compose.yml, set `OTEL_EXPORTER_OTLP_ENDPOINT=http://lgtm:4318`, and verify metrics/traces/logs appear in Grafana UI. No application code or config changes needed — just the Docker Compose service and env var.

## Follow-up — Stage 3 (Business Observability)

Stage 3 will add `@Observed` proxies in Configuration module, custom DB HealthIndicator, and SLO alarm definitions. This is the only stage that touches Java code.
