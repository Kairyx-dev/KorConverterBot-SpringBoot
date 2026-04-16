# Purist DDD + Hexagonal Architecture Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate KorConverterBot-SpringBoot from basic hexagonal to purist DDD + hexagonal architecture with jOOQ, 5-layer quality gates, and 5-gate CI pipeline.

**Architecture:** Single Bounded Context. Inside-Out build order: module restructure → domain → application → adapter-persistence (jOOQ/Flyway) → adapter-bot → configuration (TX proxy) → boot → quality gates → CI. TDD where applicable.

**Tech Stack:** Java 25, Spring Boot 4.0.4, Gradle 9, jOOQ, Flyway, MapStruct, Testcontainers, ArchUnit, JaCoCo, PIT, Spotless, Checkstyle, Lefthook, jqwik

**Design Spec:** `docs/superpowers/specs/2026-04-15-purist-ddd-migration-design.md`

**Reference Playbook:** `/home/kshull/project/ppzxc/purist-ddd-playbook/` (Part 1-9, 11)

---

## Task 1: Module Restructure + Build Infrastructure

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts` (root)
- Modify: `gradle/libs.versions.toml`
- Create: `korConverter/configuration/build.gradle.kts`
- Create: `korConverter/configuration/src/main/java/.gitkeep`
- Rename: `korConverter/hexagonal/adapter/adapter-jpa/` → `korConverter/hexagonal/adapter/adapter-persistence/`
- Modify: `korConverter/hexagonal/domain/build.gradle.kts`
- Modify: `korConverter/hexagonal/application/build.gradle.kts`
- Modify: `korConverter/hexagonal/adapter/adapter-bot/build.gradle.kts`
- Create: `korConverter/hexagonal/adapter/adapter-persistence/build.gradle.kts` (replace adapter-jpa)
- Modify: `korConverter/boot/build.gradle.kts`
- Delete: `korConverter/common/` (entire module)

- [ ] **Step 1: Create worktree and branch**

```bash
cd /home/kshull/project/kairyx/java/KorConverterBot-SpringBoot
git worktree add ../KorConverterBot-SpringBoot-purist-ddd feature/purist-ddd-migration
cd ../KorConverterBot-SpringBoot-purist-ddd
```

- [ ] **Step 2: Update `gradle/libs.versions.toml`**

Add new dependency versions. Keep all existing entries and add:

```toml
[versions]
# === existing (keep as-is) ===
spring-boot = "4.0.4"
spring-dependency-management = "1.1.7"
lombok = "1.18.44"
junit-bom = "6.0.3"
slf4j = "2.0.17"
mapstruct = "1.6.3"
jda = "6.3.2"
postgresql = "42.7.10"
assertj = "3.27.7"
logback = "1.5.32"
jspecify = "1.0.0"
errorprone = "2.49.0"
nullaway = "0.13.2"
line-build-recipe = "2.4"

# === new ===
jooq = "3.20.4"
flyway = "11.8.2"
testcontainers = "1.21.1"
archunit = "1.4.0"
pitest = "1.17.4"
pitest-gradle = "1.15.0"
spotless = "7.0.4"
checkstyle = "10.25.0"
jqwik = "1.9.3"
mockito = "5.18.0"

[libraries]
# === existing (keep as-is) ===
# ... all existing library entries ...

# === new ===
jooq-core = { module = "org.jooq:jooq", version.ref = "jooq" }
jooq-codegen = { module = "org.jooq:jooq-codegen", version.ref = "jooq" }
flyway-core = { module = "org.flywaydb:flyway-core", version.ref = "flyway" }
flyway-postgresql = { module = "org.flywaydb:flyway-database-postgresql", version.ref = "flyway" }
testcontainers-bom = { module = "org.testcontainers:testcontainers-bom", version.ref = "testcontainers" }
testcontainers-postgresql = { module = "org.testcontainers:postgresql", version.ref = "testcontainers" }
testcontainers-junit = { module = "org.testcontainers:junit-jupiter", version.ref = "testcontainers" }
archunit-junit5 = { module = "com.tngtech.archunit:archunit-junit5", version.ref = "archunit" }
jqwik = { module = "net.jqwik:jqwik", version.ref = "jqwik" }
mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
mockito-junit = { module = "org.mockito:mockito-junit-jupiter", version.ref = "mockito" }

[plugins]
# === existing ===
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
spring-dependency-management = { id = "io.spring.dependency-management", version.ref = "spring-dependency-management" }
line-build-recipe = { id = "com.linecorp.build-recipe-plugin", version.ref = "line-build-recipe" }

# === new ===
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
pitest = { id = "info.solidsoft.pitest", version.ref = "pitest-gradle" }
jooq-codegen = { id = "org.jooq.jooq-codegen-gradle", version.ref = "jooq" }
flyway = { id = "org.flywaydb.flyway", version.ref = "flyway" }
```

- [ ] **Step 3: Rename adapter-jpa directory to adapter-persistence**

```bash
mv korConverter/hexagonal/adapter/adapter-jpa korConverter/hexagonal/adapter/adapter-persistence
```

- [ ] **Step 4: Delete common module**

```bash
rm -rf korConverter/common
```

- [ ] **Step 5: Create configuration module directory**

```bash
mkdir -p korConverter/configuration/src/main/java/org/specter/converter/configuration
mkdir -p korConverter/configuration/src/main/resources/META-INF/spring
```

- [ ] **Step 6: Update `settings.gradle.kts`**

Replace the module definitions. Keep the root project name and plugin management. Change the module list to:

```kotlin
module(":boot", "korConverter/boot")
module(":configuration", "korConverter/configuration")
module(":application", "korConverter/hexagonal/application")
module(":domain", "korConverter/hexagonal/domain")
module(":adapter-persistence", "korConverter/hexagonal/adapter/adapter-persistence")
module(":adapter-bot", "korConverter/hexagonal/adapter/adapter-bot")
```

Remove the `:common` module entry.

- [ ] **Step 7: Update `korConverter/hexagonal/domain/build.gradle.kts`**

Domain must have zero external dependencies (D-1). Remove all dependencies:

```kotlin
// Domain module — pure Java, zero external dependencies (D-1)
dependencies {
    // test only
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj)
    testImplementation(libs.jqwik)
}
```

- [ ] **Step 8: Update `korConverter/hexagonal/application/build.gradle.kts`**

Application depends only on domain (A-1):

```kotlin
dependencies {
    implementation(project(":domain"))

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit)
}
```

- [ ] **Step 9: Create `korConverter/configuration/build.gradle.kts`**

```kotlin
dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    implementation(project(":adapter-persistence"))
    implementation(project(":adapter-bot"))
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
```

- [ ] **Step 10: Rewrite `korConverter/hexagonal/adapter/adapter-persistence/build.gradle.kts`**

Replace the old adapter-jpa build with jOOQ + Flyway:

```kotlin
plugins {
    alias(libs.plugins.jooq.codegen)
}

dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    runtimeOnly(libs.postgresql)

    // jOOQ code generation
    jooqCodegen(libs.postgresql)
    jooqCodegen(libs.jooq.codegen)

    // test
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}

jooq {
    configuration {
        generator {
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                properties {
                    property {
                        key = "scripts"
                        value = "src/main/resources/db/migration/*.sql"
                    }
                    property {
                        key = "sort"
                        value = "flyway"
                    }
                    property {
                        key = "defaultNameCase"
                        value = "lower"
                    }
                }
            }
            generate {
                isDeprecated = false
                isRecords = true
                isFluentSetters = true
            }
            target {
                packageName = "org.specter.converter.adapter.persistence.generated"
                directory = jooqGeneratedDir.get().asFile.absolutePath
            }
        }
    }
}

val jooqGeneratedDir = layout.buildDirectory.dir("generated/sources/jooq/main")

sourceSets {
    main {
        java {
            srcDir(jooqGeneratedDir)
        }
    }
}

tasks.named("compileJava") {
    dependsOn("jooqCodegen")
}
```

- [ ] **Step 11: Update `korConverter/hexagonal/adapter/adapter-bot/build.gradle.kts`**

```kotlin
dependencies {
    implementation(project(":application"))
    implementation(libs.jda)
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
```

Note: Remove domain dependency — adapter-bot must NOT depend on domain (AD-1).

- [ ] **Step 12: Update `korConverter/boot/build.gradle.kts`**

```kotlin
dependencies {
    implementation(project(":configuration"))
    implementation(project(":adapter-bot"))
    implementation(project(":adapter-persistence"))
    implementation(project(":application"))
    implementation(project(":domain"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}
```

- [ ] **Step 13: Add Spotless plugin to root `build.gradle.kts`**

Add to the root build.gradle.kts plugins block and configuration:

```kotlin
plugins {
    alias(libs.plugins.spotless)
    // ... existing plugins
}

spotless {
    java {
        target("**/*.java")
        targetExclude("**/generated/**", "**/build/**")
        googleJavaFormat()
    }
}
```

- [ ] **Step 14: Verify project compiles (empty modules OK)**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL (some modules may have no sources yet, that's OK)

- [ ] **Step 15: Commit module restructure**

```bash
git add -A
git commit -m "build: restructure modules for purist DDD migration

- Rename adapter-jpa → adapter-persistence
- Add configuration module (independent)
- Remove common module
- Add jOOQ, Flyway, Testcontainers, ArchUnit, jqwik, PIT dependencies
- Add Spotless plugin
- Update module dependencies per hexagonal rules"
```

---

## Task 2: Domain Value Objects + Tests

**Files:**
- Create: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/IgnoreUserId.java`
- Create: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/UserId.java`
- Create: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/ChannelId.java`
- Create: `korConverter/hexagonal/domain/src/test/java/org/specter/converter/domain/model/IgnoreUserIdTest.java`
- Create: `korConverter/hexagonal/domain/src/test/java/org/specter/converter/domain/model/UserIdTest.java`
- Create: `korConverter/hexagonal/domain/src/test/java/org/specter/converter/domain/model/ChannelIdTest.java`

- [ ] **Step 1: Write VO tests**

Create `korConverter/hexagonal/domain/src/test/java/org/specter/converter/domain/model/UserIdTest.java`:

```java
package org.specter.converter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Test;

class UserIdTest {

    @Test
    void valid_positive_value() {
        var userId = new UserId(123L);
        assertThat(userId.value()).isEqualTo(123L);
    }

    @Test
    void zero_rejected() {
        assertThatThrownBy(() -> new UserId(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negative_rejected() {
        assertThatThrownBy(() -> new UserId(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    void positive_values_accepted(@ForAll @LongRange(min = 1) long value) {
        var userId = new UserId(value);
        assertThat(userId.value()).isEqualTo(value);
    }

    @Property
    void non_positive_values_rejected(@ForAll @LongRange(max = 0) long value) {
        assertThatThrownBy(() -> new UserId(value))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

Create `korConverter/hexagonal/domain/src/test/java/org/specter/converter/domain/model/ChannelIdTest.java`:

```java
package org.specter.converter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Test;

class ChannelIdTest {

    @Test
    void valid_positive_value() {
        var channelId = new ChannelId(456L);
        assertThat(channelId.value()).isEqualTo(456L);
    }

    @Test
    void zero_rejected() {
        assertThatThrownBy(() -> new ChannelId(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    void positive_values_accepted(@ForAll @LongRange(min = 1) long value) {
        assertThat(new ChannelId(value).value()).isEqualTo(value);
    }

    @Property
    void non_positive_values_rejected(@ForAll @LongRange(max = 0) long value) {
        assertThatThrownBy(() -> new ChannelId(value))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

Create `korConverter/hexagonal/domain/src/test/java/org/specter/converter/domain/model/IgnoreUserIdTest.java`:

```java
package org.specter.converter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IgnoreUserIdTest {

    @Test
    void valid_positive_value() {
        var id = new IgnoreUserId(1L);
        assertThat(id.value()).isEqualTo(1L);
    }

    @Test
    void zero_allowed_for_unsaved() {
        assertThat(IgnoreUserId.UNSAVED.value()).isEqualTo(0L);
    }

    @Test
    void negative_rejected() {
        assertThatThrownBy(() -> new IgnoreUserId(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :domain:test
```

Expected: FAIL — classes `UserId`, `ChannelId`, `IgnoreUserId` do not exist.

- [ ] **Step 3: Implement Value Objects**

Create `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/UserId.java`:

```java
package org.specter.converter.domain.model;

public record UserId(long value) {
    public UserId {
        if (value <= 0) {
            throw new IllegalArgumentException("UserId must be positive, got: " + value);
        }
    }
}
```

Create `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/ChannelId.java`:

```java
package org.specter.converter.domain.model;

public record ChannelId(long value) {
    public ChannelId {
        if (value <= 0) {
            throw new IllegalArgumentException("ChannelId must be positive, got: " + value);
        }
    }
}
```

Create `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/IgnoreUserId.java`:

```java
package org.specter.converter.domain.model;

public record IgnoreUserId(long value) {

    public static final IgnoreUserId UNSAVED = new IgnoreUserId(0L);

    public IgnoreUserId {
        if (value < 0) {
            throw new IllegalArgumentException("IgnoreUserId must not be negative, got: " + value);
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :domain:test
```

Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add korConverter/hexagonal/domain/src/
git commit -m "feat(domain): add Value Objects — UserId, ChannelId, IgnoreUserId

Records with compact constructor self-validation (D-6, D-7)."
```

---

## Task 3: Domain Events + Exceptions

**Files:**
- Create: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/event/IgnoreUserEvent.java`
- Create: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/event/IgnoreUserAddedEvent.java`
- Create: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/event/IgnoreUserRemovedEvent.java`
- Create: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/exception/IgnoreUserException.java`
- Create: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/exception/IgnoreUserNotFoundException.java`
- Create: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/exception/IgnoreUserAlreadyExistsException.java`

- [ ] **Step 1: Create Domain Event sealed interface**

Create `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/event/IgnoreUserEvent.java`:

```java
package org.specter.converter.domain.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface IgnoreUserEvent
        permits IgnoreUserAddedEvent, IgnoreUserRemovedEvent {
    UUID eventId();
    String eventType();
    long aggregateId();
    Instant occurredAt();
    long aggregateVersion();
}
```

Create `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/event/IgnoreUserAddedEvent.java`:

```java
package org.specter.converter.domain.event;

import java.time.Instant;
import java.util.UUID;

public record IgnoreUserAddedEvent(
        UUID eventId,
        String eventType,
        long aggregateId,
        Instant occurredAt,
        long aggregateVersion) implements IgnoreUserEvent {}
```

Create `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/event/IgnoreUserRemovedEvent.java`:

```java
package org.specter.converter.domain.event;

import java.time.Instant;
import java.util.UUID;

public record IgnoreUserRemovedEvent(
        UUID eventId,
        String eventType,
        long aggregateId,
        Instant occurredAt,
        long aggregateVersion) implements IgnoreUserEvent {}
```

- [ ] **Step 2: Create Domain Exception sealed class**

Create `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/exception/IgnoreUserException.java`:

```java
package org.specter.converter.domain.exception;

public sealed class IgnoreUserException extends RuntimeException
        permits IgnoreUserNotFoundException, IgnoreUserAlreadyExistsException {
    protected IgnoreUserException(String message) {
        super(message);
    }
}
```

Create `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/exception/IgnoreUserNotFoundException.java`:

```java
package org.specter.converter.domain.exception;

import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.UserId;

public final class IgnoreUserNotFoundException extends IgnoreUserException {
    public IgnoreUserNotFoundException(UserId userId, ChannelId channelId) {
        super("IgnoreUser not found: userId=%d, channelId=%d"
                .formatted(userId.value(), channelId.value()));
    }
}
```

Create `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/exception/IgnoreUserAlreadyExistsException.java`:

```java
package org.specter.converter.domain.exception;

import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.UserId;

public final class IgnoreUserAlreadyExistsException extends IgnoreUserException {
    public IgnoreUserAlreadyExistsException(UserId userId, ChannelId channelId) {
        super("IgnoreUser already exists: userId=%d, channelId=%d"
                .formatted(userId.value(), channelId.value()));
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :domain:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/event/
git add korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/exception/
git commit -m "feat(domain): add Domain Events and Exceptions

Sealed interface IgnoreUserEvent (D-13) + sealed class IgnoreUserException."
```

---

## Task 4: IgnoreUser Aggregate Root + Tests

**Files:**
- Modify: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/IgnoreUser.java` (rewrite)
- Create: `korConverter/hexagonal/domain/src/test/java/org/specter/converter/domain/model/IgnoreUserTest.java`

- [ ] **Step 1: Write Aggregate tests**

Create `korConverter/hexagonal/domain/src/test/java/org/specter/converter/domain/model/IgnoreUserTest.java`:

```java
package org.specter.converter.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.specter.converter.domain.event.IgnoreUserAddedEvent;
import org.specter.converter.domain.event.IgnoreUserRemovedEvent;
import org.junit.jupiter.api.Test;

class IgnoreUserTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void create_emits_added_event() {
        var user = IgnoreUser.create(new UserId(123L), new ChannelId(456L), "testUser", NOW);

        var events = user.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(IgnoreUserAddedEvent.class);

        var event = (IgnoreUserAddedEvent) events.getFirst();
        assertThat(event.eventType()).isEqualTo("IGNORE_USER_ADDED");
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.aggregateVersion()).isEqualTo(0L);
    }

    @Test
    void create_sets_unsaved_id() {
        var user = IgnoreUser.create(new UserId(123L), new ChannelId(456L), "testUser", NOW);

        assertThat(user.id()).isEqualTo(IgnoreUserId.UNSAVED);
        assertThat(user.userId()).isEqualTo(new UserId(123L));
        assertThat(user.channelId()).isEqualTo(new ChannelId(456L));
        assertThat(user.name()).isEqualTo("testUser");
        assertThat(user.createdAt()).isEqualTo(NOW);
        assertThat(user.version()).isEqualTo(0L);
    }

    @Test
    void reconstitute_does_not_emit_events() {
        var user = IgnoreUser.reconstitute(
                new IgnoreUserId(1L), new UserId(123L), new ChannelId(456L),
                "testUser", NOW, NOW, 3L);

        assertThat(user.pullDomainEvents()).isEmpty();
        assertThat(user.id()).isEqualTo(new IgnoreUserId(1L));
        assertThat(user.version()).isEqualTo(3L);
    }

    @Test
    void pullDomainEvents_clears_after_call() {
        var user = IgnoreUser.create(new UserId(123L), new ChannelId(456L), "testUser", NOW);

        assertThat(user.pullDomainEvents()).hasSize(1);
        assertThat(user.pullDomainEvents()).isEmpty();
    }

    @Test
    void markForRemoval_emits_removed_event() {
        var user = IgnoreUser.reconstitute(
                new IgnoreUserId(1L), new UserId(123L), new ChannelId(456L),
                "testUser", NOW, NOW, 1L);

        user.markForRemoval(NOW);

        var events = user.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(IgnoreUserRemovedEvent.class);

        var event = (IgnoreUserRemovedEvent) events.getFirst();
        assertThat(event.eventType()).isEqualTo("IGNORE_USER_REMOVED");
        assertThat(event.aggregateId()).isEqualTo(1L);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :domain:test --tests "*.IgnoreUserTest"
```

Expected: FAIL — `IgnoreUser` is still the old record.

- [ ] **Step 3: Rewrite IgnoreUser as Aggregate Root**

Replace `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/IgnoreUser.java`:

```java
package org.specter.converter.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.specter.converter.domain.event.IgnoreUserAddedEvent;
import org.specter.converter.domain.event.IgnoreUserEvent;
import org.specter.converter.domain.event.IgnoreUserRemovedEvent;

public final class IgnoreUser {

    private final IgnoreUserId id;
    private final UserId userId;
    private final ChannelId channelId;
    private final String name;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;
    private final List<IgnoreUserEvent> domainEvents = new ArrayList<>();

    private IgnoreUser(
            IgnoreUserId id,
            UserId userId,
            ChannelId channelId,
            String name,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.channelId = Objects.requireNonNull(channelId, "channelId");
        this.name = Objects.requireNonNull(name, "name");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.version = version;
    }

    public static IgnoreUser create(UserId userId, ChannelId channelId, String name, Instant now) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(now, "now");

        var user = new IgnoreUser(IgnoreUserId.UNSAVED, userId, channelId, name, now, now, 0L);
        user.registerEvent(
                new IgnoreUserAddedEvent(
                        UUID.randomUUID(),
                        "IGNORE_USER_ADDED",
                        user.id.value(),
                        now,
                        user.version));
        return user;
    }

    public static IgnoreUser reconstitute(
            IgnoreUserId id,
            UserId userId,
            ChannelId channelId,
            String name,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        return new IgnoreUser(id, userId, channelId, name, createdAt, updatedAt, version);
    }

    public void markForRemoval(Instant now) {
        Objects.requireNonNull(now, "now");
        registerEvent(
                new IgnoreUserRemovedEvent(
                        UUID.randomUUID(),
                        "IGNORE_USER_REMOVED",
                        this.id.value(),
                        now,
                        this.version));
    }

    public List<IgnoreUserEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void registerEvent(IgnoreUserEvent event) {
        domainEvents.add(event);
    }

    public IgnoreUserId id() { return id; }
    public UserId userId() { return userId; }
    public ChannelId channelId() { return channelId; }
    public String name() { return name; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :domain:test --tests "*.IgnoreUserTest"
```

Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add korConverter/hexagonal/domain/src/
git commit -m "feat(domain): rewrite IgnoreUser as Aggregate Root

Private constructor, create()/reconstitute() separation,
pullDomainEvents(), markForRemoval() (D-5, D-8, D-14)."
```

---

## Task 5: Rename ConverterCoreV2 → ConversionDomainService + Migrate Tests

**Files:**
- Rename: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/core/ConverterCoreV2.java` → `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/ConversionDomainService.java`
- Move: `KeyboardIndex.java` and `KrDataIndex.java` stay in `domain/model/`
- Modify: `korConverter/hexagonal/domain/src/test/java/org/specter/converter/domain/ConverterTest.java` (update imports)
- Delete: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/core/ConverterCore.java` (legacy V1)
- Delete: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/core/` (empty package)
- Delete: `korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/MessageLog.java`

- [ ] **Step 1: Rename ConverterCoreV2 to ConversionDomainService**

```bash
# Move file and rename class
cp korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/core/ConverterCoreV2.java \
   korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/ConversionDomainService.java
```

Edit `ConversionDomainService.java`:
- Change `package org.specter.converter.domain.core;` → `package org.specter.converter.domain.model;`
- Change `public class ConverterCoreV2` → `public class ConversionDomainService`
- Update the constructor name if explicit
- Remove any logging imports/annotations (`@Slf4j`, etc.)

- [ ] **Step 2: Delete legacy files**

```bash
rm korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/core/ConverterCore.java
rm korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/core/ConverterCoreV2.java
rmdir korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/core/
rm korConverter/hexagonal/domain/src/main/java/org/specter/converter/domain/model/MessageLog.java
```

- [ ] **Step 3: Update test imports**

Modify `korConverter/hexagonal/domain/src/test/java/org/specter/converter/domain/ConverterTest.java`:
- Change `import org.specter.converter.domain.core.ConverterCoreV2;` → `import org.specter.converter.domain.model.ConversionDomainService;`
- Change all `ConverterCoreV2` references → `ConversionDomainService`
- Move test file to `domain/model/` package if desired (or keep current location)

- [ ] **Step 4: Run all domain tests**

```bash
./gradlew :domain:test
```

Expected: ALL PASS (existing converter tests + new VO/Aggregate tests)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(domain): rename ConverterCoreV2 → ConversionDomainService

Remove legacy ConverterCore (V1), delete MessageLog from domain (D-12).
Domain Service follows naming convention: {Subject}DomainService."
```

---

## Task 6: Application Layer — Ports + DTOs

**Files:**
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/port/input/AddIgnoreUserUseCase.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/port/input/RemoveIgnoreUserUseCase.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/port/input/ConvertMessageUseCase.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/port/input/CheckIgnoreUserUseCase.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/port/output/LoadIgnoreUserPort.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/port/output/SaveIgnoreUserPort.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/port/output/IgnoreUserQueryPort.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/port/output/RecordMessageLogPort.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/dto/command/AddIgnoreUserCommand.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/dto/command/RemoveIgnoreUserCommand.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/dto/command/ConvertMessageCommand.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/dto/command/RecordMessageLogCommand.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/dto/query/CheckIgnoreUserQuery.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/dto/result/IgnoreUserResult.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/dto/result/ConvertMessageResult.java`
- Delete: `korConverter/hexagonal/application/src/main/java/org/specter/converter/aplication/` (old misspelled package)

- [ ] **Step 1: Delete old application layer**

```bash
rm -rf korConverter/hexagonal/application/src/main/java/org/specter/converter/aplication/
```

- [ ] **Step 2: Create package directories**

```bash
mkdir -p korConverter/hexagonal/application/src/main/java/org/specter/converter/application/port/input
mkdir -p korConverter/hexagonal/application/src/main/java/org/specter/converter/application/port/output
mkdir -p korConverter/hexagonal/application/src/main/java/org/specter/converter/application/dto/command
mkdir -p korConverter/hexagonal/application/src/main/java/org/specter/converter/application/dto/query
mkdir -p korConverter/hexagonal/application/src/main/java/org/specter/converter/application/dto/result
mkdir -p korConverter/hexagonal/application/src/main/java/org/specter/converter/application/service
```

- [ ] **Step 3: Create Output Ports**

Create `LoadIgnoreUserPort.java`:

```java
package org.specter.converter.application.port.output;

import java.util.Optional;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.UserId;

public interface LoadIgnoreUserPort {
    Optional<IgnoreUser> loadByUserIdAndChannelId(UserId userId, ChannelId channelId);
}
```

Create `SaveIgnoreUserPort.java`:

```java
package org.specter.converter.application.port.output;

import org.specter.converter.domain.model.IgnoreUser;

public interface SaveIgnoreUserPort {
    void save(IgnoreUser ignoreUser);
    void delete(IgnoreUser ignoreUser);
}
```

Create `IgnoreUserQueryPort.java`:

```java
package org.specter.converter.application.port.output;

public interface IgnoreUserQueryPort {
    boolean existsByUserIdAndChannelId(long userId, long channelId);
    List<IgnoreUserResult> findAllByChannelId(long channelId);
}
```

Create `RecordMessageLogPort.java`:

```java
package org.specter.converter.application.port.output;

import org.specter.converter.application.dto.command.RecordMessageLogCommand;

public interface RecordMessageLogPort {
    void record(RecordMessageLogCommand command);
}
```

- [ ] **Step 4: Create Command/Query/Result DTOs**

Create `AddIgnoreUserCommand.java`:

```java
package org.specter.converter.application.dto.command;

public record AddIgnoreUserCommand(long userId, long channelId, String name) {}
```

Create `RemoveIgnoreUserCommand.java`:

```java
package org.specter.converter.application.dto.command;

public record RemoveIgnoreUserCommand(long userId, long channelId) {}
```

Create `ConvertMessageCommand.java`:

```java
package org.specter.converter.application.dto.command;

public record ConvertMessageCommand(
        String message,
        long guildId,
        long channelId,
        String nickName,
        String effectiveName) {}
```

Create `RecordMessageLogCommand.java`:

```java
package org.specter.converter.application.dto.command;

public record RecordMessageLogCommand(
        long guildId,
        String channel,
        String nickName,
        String effectiveName,
        String message,
        boolean converted,
        String convertedMessage,
        long channelId) {}
```

Create `CheckIgnoreUserQuery.java`:

```java
package org.specter.converter.application.dto.query;

public record CheckIgnoreUserQuery(long userId, long channelId) {}
```

Create `IgnoreUserResult.java`:

```java
package org.specter.converter.application.dto.result;

public record IgnoreUserResult(long id, long userId, long channelId, String name) {}
```

Create `ConvertMessageResult.java`:

```java
package org.specter.converter.application.dto.result;

public record ConvertMessageResult(
        String originalMessage, String convertedMessage, boolean converted) {}
```

- [ ] **Step 5: Create Input Ports (UseCase interfaces)**

Create `AddIgnoreUserUseCase.java`:

```java
package org.specter.converter.application.port.input;

import org.specter.converter.application.dto.command.AddIgnoreUserCommand;
import org.specter.converter.application.dto.result.IgnoreUserResult;

public interface AddIgnoreUserUseCase {
    IgnoreUserResult execute(AddIgnoreUserCommand command);
}
```

Create `RemoveIgnoreUserUseCase.java`:

```java
package org.specter.converter.application.port.input;

import org.specter.converter.application.dto.command.RemoveIgnoreUserCommand;

public interface RemoveIgnoreUserUseCase {
    void execute(RemoveIgnoreUserCommand command);
}
```

Create `ConvertMessageUseCase.java`:

```java
package org.specter.converter.application.port.input;

import org.specter.converter.application.dto.command.ConvertMessageCommand;
import org.specter.converter.application.dto.result.ConvertMessageResult;

public interface ConvertMessageUseCase {
    ConvertMessageResult execute(ConvertMessageCommand command);
}
```

Create `CheckIgnoreUserUseCase.java`:

```java
package org.specter.converter.application.port.input;

import org.specter.converter.application.dto.query.CheckIgnoreUserQuery;

public interface CheckIgnoreUserUseCase {
    boolean execute(CheckIgnoreUserQuery query);
}
```

- [ ] **Step 6: Verify compilation**

```bash
./gradlew :application:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(application): add Ports, Commands, Queries, Results

Input Ports (UseCase interfaces), Output Ports (3-partition + RecordMessageLog),
Command/Query/Result DTOs (A-2, A-3, A-7, A-10).
Delete old aplication/ package (typo fix)."
```

---

## Task 7: Application Services + Tests

**Files:**
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/service/AddIgnoreUserService.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/service/RemoveIgnoreUserService.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/service/ConvertMessageService.java`
- Create: `korConverter/hexagonal/application/src/main/java/org/specter/converter/application/service/CheckIgnoreUserService.java`
- Create: `korConverter/hexagonal/application/src/test/java/org/specter/converter/application/service/AddIgnoreUserServiceTest.java`
- Create: `korConverter/hexagonal/application/src/test/java/org/specter/converter/application/service/RemoveIgnoreUserServiceTest.java`
- Create: `korConverter/hexagonal/application/src/test/java/org/specter/converter/application/service/ConvertMessageServiceTest.java`
- Create: `korConverter/hexagonal/application/src/test/java/org/specter/converter/application/service/CheckIgnoreUserServiceTest.java`

- [ ] **Step 1: Write AddIgnoreUserService test**

Create `korConverter/hexagonal/application/src/test/java/org/specter/converter/application/service/AddIgnoreUserServiceTest.java`:

```java
package org.specter.converter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.specter.converter.application.dto.command.AddIgnoreUserCommand;
import org.specter.converter.application.port.output.LoadIgnoreUserPort;
import org.specter.converter.application.port.output.SaveIgnoreUserPort;
import org.specter.converter.domain.exception.IgnoreUserAlreadyExistsException;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.UserId;

class AddIgnoreUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final LoadIgnoreUserPort loadPort = mock(LoadIgnoreUserPort.class);
    private final SaveIgnoreUserPort savePort = mock(SaveIgnoreUserPort.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AddIgnoreUserService sut = new AddIgnoreUserService(loadPort, savePort, clock);

    @Test
    void adds_new_ignore_user() {
        when(loadPort.loadByUserIdAndChannelId(any(), any())).thenReturn(Optional.empty());
        var command = new AddIgnoreUserCommand(123L, 456L, "testUser");

        var result = sut.execute(command);

        assertThat(result.userId()).isEqualTo(123L);
        assertThat(result.channelId()).isEqualTo(456L);
        assertThat(result.name()).isEqualTo("testUser");
        verify(savePort).save(any(IgnoreUser.class));
    }

    @Test
    void throws_when_already_exists() {
        var existing = IgnoreUser.reconstitute(
                new org.specter.converter.domain.model.IgnoreUserId(1L),
                new UserId(123L), new ChannelId(456L), "existing", NOW, NOW, 0L);
        when(loadPort.loadByUserIdAndChannelId(any(), any())).thenReturn(Optional.of(existing));

        var command = new AddIgnoreUserCommand(123L, 456L, "testUser");

        assertThatThrownBy(() -> sut.execute(command))
                .isInstanceOf(IgnoreUserAlreadyExistsException.class);
    }
}
```

- [ ] **Step 2: Write RemoveIgnoreUserService test**

Create `korConverter/hexagonal/application/src/test/java/org/specter/converter/application/service/RemoveIgnoreUserServiceTest.java`:

```java
package org.specter.converter.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.specter.converter.application.dto.command.RemoveIgnoreUserCommand;
import org.specter.converter.application.port.output.LoadIgnoreUserPort;
import org.specter.converter.application.port.output.SaveIgnoreUserPort;
import org.specter.converter.domain.exception.IgnoreUserNotFoundException;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.IgnoreUserId;
import org.specter.converter.domain.model.UserId;

class RemoveIgnoreUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final LoadIgnoreUserPort loadPort = mock(LoadIgnoreUserPort.class);
    private final SaveIgnoreUserPort savePort = mock(SaveIgnoreUserPort.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final RemoveIgnoreUserService sut =
            new RemoveIgnoreUserService(loadPort, savePort, clock);

    @Test
    void removes_existing_user() {
        var existing = IgnoreUser.reconstitute(
                new IgnoreUserId(1L), new UserId(123L), new ChannelId(456L),
                "testUser", NOW, NOW, 1L);
        when(loadPort.loadByUserIdAndChannelId(any(), any())).thenReturn(Optional.of(existing));

        sut.execute(new RemoveIgnoreUserCommand(123L, 456L));

        verify(savePort).delete(any(IgnoreUser.class));
    }

    @Test
    void throws_when_not_found() {
        when(loadPort.loadByUserIdAndChannelId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.execute(new RemoveIgnoreUserCommand(123L, 456L)))
                .isInstanceOf(IgnoreUserNotFoundException.class);
    }
}
```

- [ ] **Step 3: Write ConvertMessageService test**

Create `korConverter/hexagonal/application/src/test/java/org/specter/converter/application/service/ConvertMessageServiceTest.java`:

```java
package org.specter.converter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.specter.converter.application.dto.command.ConvertMessageCommand;
import org.specter.converter.application.dto.command.RecordMessageLogCommand;
import org.specter.converter.application.port.output.RecordMessageLogPort;
import org.specter.converter.domain.model.ConversionDomainService;

class ConvertMessageServiceTest {

    private final ConversionDomainService conversionService = new ConversionDomainService();
    private final RecordMessageLogPort messageLogPort = mock(RecordMessageLogPort.class);
    private final ConvertMessageService sut =
            new ConvertMessageService(conversionService, messageLogPort);

    @Test
    void converts_available_message() {
        var command = new ConvertMessageCommand(
                "dkssudgktpdy", 1L, 2L, "nick", "effective");

        var result = sut.execute(command);

        assertThat(result.converted()).isTrue();
        assertThat(result.convertedMessage()).isEqualTo("안녕하세요");
        assertThat(result.originalMessage()).isEqualTo("dkssudgktpdy");
        verify(messageLogPort).record(any(RecordMessageLogCommand.class));
    }

    @Test
    void returns_not_converted_for_unavailable_message() {
        var command = new ConvertMessageCommand(
                "https://example.com", 1L, 2L, "nick", "effective");

        var result = sut.execute(command);

        assertThat(result.converted()).isFalse();
    }
}
```

- [ ] **Step 4: Write CheckIgnoreUserService test**

Create `korConverter/hexagonal/application/src/test/java/org/specter/converter/application/service/CheckIgnoreUserServiceTest.java`:

```java
package org.specter.converter.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.specter.converter.application.dto.query.CheckIgnoreUserQuery;
import org.specter.converter.application.port.output.IgnoreUserQueryPort;

class CheckIgnoreUserServiceTest {

    private final IgnoreUserQueryPort queryPort = mock(IgnoreUserQueryPort.class);
    private final CheckIgnoreUserService sut = new CheckIgnoreUserService(queryPort);

    @Test
    void returns_true_when_user_exists() {
        when(queryPort.existsByUserIdAndChannelId(123L, 456L)).thenReturn(true);

        assertThat(sut.execute(new CheckIgnoreUserQuery(123L, 456L))).isTrue();
    }

    @Test
    void returns_false_when_user_not_found() {
        when(queryPort.existsByUserIdAndChannelId(123L, 456L)).thenReturn(false);

        assertThat(sut.execute(new CheckIgnoreUserQuery(123L, 456L))).isFalse();
    }
}
```

- [ ] **Step 5: Run tests to verify they fail**

```bash
./gradlew :application:test
```

Expected: FAIL — service classes do not exist.

- [ ] **Step 6: Implement all four services**

Create `AddIgnoreUserService.java`:

```java
package org.specter.converter.application.service;

import java.time.Clock;
import java.util.Objects;
import org.specter.converter.application.dto.command.AddIgnoreUserCommand;
import org.specter.converter.application.dto.result.IgnoreUserResult;
import org.specter.converter.application.port.input.AddIgnoreUserUseCase;
import org.specter.converter.application.port.output.LoadIgnoreUserPort;
import org.specter.converter.application.port.output.SaveIgnoreUserPort;
import org.specter.converter.domain.exception.IgnoreUserAlreadyExistsException;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.UserId;

public class AddIgnoreUserService implements AddIgnoreUserUseCase {

    private final LoadIgnoreUserPort loadPort;
    private final SaveIgnoreUserPort savePort;
    private final Clock clock;

    public AddIgnoreUserService(
            LoadIgnoreUserPort loadPort, SaveIgnoreUserPort savePort, Clock clock) {
        this.loadPort = Objects.requireNonNull(loadPort, "loadPort");
        this.savePort = Objects.requireNonNull(savePort, "savePort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public IgnoreUserResult execute(AddIgnoreUserCommand command) {
        var userId = new UserId(command.userId());
        var channelId = new ChannelId(command.channelId());

        loadPort.loadByUserIdAndChannelId(userId, channelId)
                .ifPresent(existing -> {
                    throw new IgnoreUserAlreadyExistsException(userId, channelId);
                });

        var ignoreUser = IgnoreUser.create(userId, channelId, command.name(), clock.instant());
        savePort.save(ignoreUser);
        return new IgnoreUserResult(
                ignoreUser.id().value(), command.userId(), command.channelId(), command.name());
    }
}
```

Create `RemoveIgnoreUserService.java`:

```java
package org.specter.converter.application.service;

import java.time.Clock;
import java.util.Objects;
import org.specter.converter.application.dto.command.RemoveIgnoreUserCommand;
import org.specter.converter.application.port.input.RemoveIgnoreUserUseCase;
import org.specter.converter.application.port.output.LoadIgnoreUserPort;
import org.specter.converter.application.port.output.SaveIgnoreUserPort;
import org.specter.converter.domain.exception.IgnoreUserNotFoundException;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.UserId;

public class RemoveIgnoreUserService implements RemoveIgnoreUserUseCase {

    private final LoadIgnoreUserPort loadPort;
    private final SaveIgnoreUserPort savePort;
    private final Clock clock;

    public RemoveIgnoreUserService(
            LoadIgnoreUserPort loadPort, SaveIgnoreUserPort savePort, Clock clock) {
        this.loadPort = Objects.requireNonNull(loadPort, "loadPort");
        this.savePort = Objects.requireNonNull(savePort, "savePort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void execute(RemoveIgnoreUserCommand command) {
        var userId = new UserId(command.userId());
        var channelId = new ChannelId(command.channelId());

        var ignoreUser = loadPort.loadByUserIdAndChannelId(userId, channelId)
                .orElseThrow(() -> new IgnoreUserNotFoundException(userId, channelId));

        ignoreUser.markForRemoval(clock.instant());
        savePort.delete(ignoreUser);
    }
}
```

Create `ConvertMessageService.java`:

```java
package org.specter.converter.application.service;

import java.util.Objects;
import org.specter.converter.application.dto.command.ConvertMessageCommand;
import org.specter.converter.application.dto.command.RecordMessageLogCommand;
import org.specter.converter.application.dto.result.ConvertMessageResult;
import org.specter.converter.application.port.input.ConvertMessageUseCase;
import org.specter.converter.application.port.output.RecordMessageLogPort;
import org.specter.converter.domain.model.ConversionDomainService;

public class ConvertMessageService implements ConvertMessageUseCase {

    private final ConversionDomainService conversionService;
    private final RecordMessageLogPort messageLogPort;

    public ConvertMessageService(
            ConversionDomainService conversionService, RecordMessageLogPort messageLogPort) {
        this.conversionService = Objects.requireNonNull(conversionService, "conversionService");
        this.messageLogPort = Objects.requireNonNull(messageLogPort, "messageLogPort");
    }

    @Override
    public ConvertMessageResult execute(ConvertMessageCommand command) {
        boolean available = conversionService.checkAvailableStr(command.message());
        String converted = available ? conversionService.engToKor(command.message()) : "";

        messageLogPort.record(new RecordMessageLogCommand(
                command.guildId(),
                String.valueOf(command.channelId()),
                command.nickName(),
                command.effectiveName(),
                command.message(),
                available,
                converted,
                command.channelId()));

        return new ConvertMessageResult(command.message(), converted, available);
    }
}
```

Create `CheckIgnoreUserService.java`:

```java
package org.specter.converter.application.service;

import java.util.Objects;
import org.specter.converter.application.dto.query.CheckIgnoreUserQuery;
import org.specter.converter.application.port.input.CheckIgnoreUserUseCase;
import org.specter.converter.application.port.output.IgnoreUserQueryPort;

public class CheckIgnoreUserService implements CheckIgnoreUserUseCase {

    private final IgnoreUserQueryPort queryPort;

    public CheckIgnoreUserService(IgnoreUserQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort");
    }

    @Override
    public boolean execute(CheckIgnoreUserQuery query) {
        return queryPort.existsByUserIdAndChannelId(query.userId(), query.channelId());
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
./gradlew :application:test
```

Expected: ALL PASS

- [ ] **Step 8: Commit**

```bash
git add korConverter/hexagonal/application/
git commit -m "feat(application): add UseCase services with tests

AddIgnoreUser, RemoveIgnoreUser, ConvertMessage, CheckIgnoreUser.
No Spring annotations (A-4), domain delegation (A-5)."
```

---

## Task 8: Flyway DDL + jOOQ Code Generation

**Files:**
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V001__create_ignore_user.sql`
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V002__create_message_log.sql`
- Delete: old JPA entities, repositories, services, mappers, configuration

- [ ] **Step 1: Delete all old adapter-jpa source code**

```bash
rm -rf korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/jpa/
rm -rf korConverter/hexagonal/adapter/adapter-persistence/src/test/
```

- [ ] **Step 2: Create Flyway migration directories**

```bash
mkdir -p korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration
mkdir -p korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/mapper
mkdir -p korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/port
mkdir -p korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/configuration
```

- [ ] **Step 3: Create V001 — ignore_user DDL**

Create `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V001__create_ignore_user.sql`:

```sql
CREATE TABLE ignore_user (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    channel_id  BIGINT       NOT NULL,
    name        VARCHAR(255),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_ignore_user_lookup ON ignore_user (user_id, channel_id);
```

- [ ] **Step 4: Create V002 — message_log DDL**

Create `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V002__create_message_log.sql`:

```sql
CREATE TABLE message_log (
    id                BIGSERIAL    PRIMARY KEY,
    guild             VARCHAR(255),
    channel           VARCHAR(255),
    nick_name         VARCHAR(255),
    effective_name    VARCHAR(255),
    message           TEXT,
    is_converted      BOOLEAN      NOT NULL DEFAULT false,
    converted_message TEXT,
    channel_id        BIGINT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

- [ ] **Step 5: Run jOOQ code generation**

```bash
./gradlew :adapter-persistence:jooqCodegen
```

Expected: BUILD SUCCESSFUL. Generated classes appear in `korConverter/hexagonal/adapter/adapter-persistence/build/generated/sources/jooq/main/`.

- [ ] **Step 6: Verify generated code exists**

```bash
ls korConverter/hexagonal/adapter/adapter-persistence/build/generated/sources/jooq/main/org/specter/converter/adapter/persistence/generated/
```

Expected: Tables, Keys, records for `ignore_user` and `message_log`.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(adapter): add Flyway DDL migrations + jOOQ code generation

V001: ignore_user with version column for optimistic lock.
V002: message_log for audit records.
Delete old JPA entities, repositories, services."
```

---

## Task 9: Persistence Adapters + AutoConfiguration

**Files:**
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/port/IgnoreUserPersistenceAdapter.java`
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/port/IgnoreUserQueryAdapter.java`
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/port/MessageLogRecordAdapter.java`
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/configuration/PersistenceAutoConfiguration.java`
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Create IgnoreUserPersistenceAdapter**

Create `korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/port/IgnoreUserPersistenceAdapter.java`:

```java
package org.specter.converter.adapter.persistence.port;

import static org.specter.converter.adapter.persistence.generated.Tables.IGNORE_USER;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import org.jooq.DSLContext;
import org.specter.converter.application.port.output.LoadIgnoreUserPort;
import org.specter.converter.application.port.output.SaveIgnoreUserPort;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.IgnoreUserId;
import org.specter.converter.domain.model.UserId;
import org.springframework.context.ApplicationEventPublisher;

public class IgnoreUserPersistenceAdapter implements LoadIgnoreUserPort, SaveIgnoreUserPort {

    private final DSLContext dsl;
    private final ApplicationEventPublisher eventPublisher;

    public IgnoreUserPersistenceAdapter(DSLContext dsl, ApplicationEventPublisher eventPublisher) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    @Override
    public Optional<IgnoreUser> loadByUserIdAndChannelId(UserId userId, ChannelId channelId) {
        return dsl.selectFrom(IGNORE_USER)
                .where(IGNORE_USER.USER_ID.eq(userId.value()))
                .and(IGNORE_USER.CHANNEL_ID.eq(channelId.value()))
                .fetchOptional()
                .map(r -> IgnoreUser.reconstitute(
                        new IgnoreUserId(r.getId()),
                        new UserId(r.getUserId()),
                        new ChannelId(r.getChannelId()),
                        r.getName(),
                        r.getCreatedAt().toInstant(),
                        r.getUpdatedAt().toInstant(),
                        r.getVersion()));
    }

    @Override
    public void save(IgnoreUser ignoreUser) {
        if (ignoreUser.id().equals(IgnoreUserId.UNSAVED)) {
            dsl.insertInto(IGNORE_USER)
                    .set(IGNORE_USER.USER_ID, ignoreUser.userId().value())
                    .set(IGNORE_USER.CHANNEL_ID, ignoreUser.channelId().value())
                    .set(IGNORE_USER.NAME, ignoreUser.name())
                    .set(IGNORE_USER.VERSION, ignoreUser.version())
                    .set(IGNORE_USER.CREATED_AT,
                            OffsetDateTime.ofInstant(ignoreUser.createdAt(), ZoneOffset.UTC))
                    .set(IGNORE_USER.UPDATED_AT,
                            OffsetDateTime.ofInstant(ignoreUser.updatedAt(), ZoneOffset.UTC))
                    .execute();
        } else {
            int affected = dsl.update(IGNORE_USER)
                    .set(IGNORE_USER.NAME, ignoreUser.name())
                    .set(IGNORE_USER.VERSION, ignoreUser.version() + 1)
                    .set(IGNORE_USER.UPDATED_AT,
                            OffsetDateTime.ofInstant(ignoreUser.updatedAt(), ZoneOffset.UTC))
                    .where(IGNORE_USER.ID.eq(ignoreUser.id().value()))
                    .and(IGNORE_USER.VERSION.eq(ignoreUser.version()))
                    .execute();
            if (affected == 0) {
                throw new OptimisticLockException(
                        "IgnoreUser id=%d version conflict".formatted(ignoreUser.id().value()));
            }
        }
        // AD-3: 이벤트 수거 → 발행 (INSERT, UPDATE 모두)
        ignoreUser.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }

    @Override
    public void delete(IgnoreUser ignoreUser) {
        dsl.deleteFrom(IGNORE_USER)
                .where(IGNORE_USER.ID.eq(ignoreUser.id().value()))
                .and(IGNORE_USER.VERSION.eq(ignoreUser.version()))
                .execute();
        ignoreUser.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
```

Create `korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/port/OptimisticLockException.java`:

```java
package org.specter.converter.adapter.persistence.port;

public class OptimisticLockException extends RuntimeException {
    public OptimisticLockException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Create IgnoreUserQueryAdapter**

Create `korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/port/IgnoreUserQueryAdapter.java`:

```java
package org.specter.converter.adapter.persistence.port;

import static org.specter.converter.adapter.persistence.generated.Tables.IGNORE_USER;

import java.util.List;
import java.util.Objects;
import org.jooq.DSLContext;
import org.specter.converter.application.dto.result.IgnoreUserResult;
import org.specter.converter.application.port.output.IgnoreUserQueryPort;

public class IgnoreUserQueryAdapter implements IgnoreUserQueryPort {

    private final DSLContext dsl;

    public IgnoreUserQueryAdapter(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
    }

    @Override
    public boolean existsByUserIdAndChannelId(long userId, long channelId) {
        return dsl.fetchExists(
                dsl.selectFrom(IGNORE_USER)
                        .where(IGNORE_USER.USER_ID.eq(userId))
                        .and(IGNORE_USER.CHANNEL_ID.eq(channelId)));
    }

    @Override
    public List<IgnoreUserResult> findAllByChannelId(long channelId) {
        return dsl.selectFrom(IGNORE_USER)
                .where(IGNORE_USER.CHANNEL_ID.eq(channelId))
                .fetch(r -> new IgnoreUserResult(
                        r.getId(), r.getUserId(), r.getChannelId(), r.getName()));
    }
}
```

- [ ] **Step 3: Create MessageLogRecordAdapter**

Create `korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/port/MessageLogRecordAdapter.java`:

```java
package org.specter.converter.adapter.persistence.port;

import static org.specter.converter.adapter.persistence.generated.Tables.MESSAGE_LOG;

import java.util.Objects;
import org.jooq.DSLContext;
import org.specter.converter.application.dto.command.RecordMessageLogCommand;
import org.specter.converter.application.port.output.RecordMessageLogPort;

public class MessageLogRecordAdapter implements RecordMessageLogPort {

    private final DSLContext dsl;

    public MessageLogRecordAdapter(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
    }

    @Override
    public void record(RecordMessageLogCommand command) {
        dsl.insertInto(MESSAGE_LOG)
                .set(MESSAGE_LOG.GUILD, String.valueOf(command.guildId()))
                .set(MESSAGE_LOG.CHANNEL, command.channel())
                .set(MESSAGE_LOG.NICK_NAME, command.nickName())
                .set(MESSAGE_LOG.EFFECTIVE_NAME, command.effectiveName())
                .set(MESSAGE_LOG.MESSAGE, command.message())
                .set(MESSAGE_LOG.IS_CONVERTED, command.converted())
                .set(MESSAGE_LOG.CONVERTED_MESSAGE, command.convertedMessage())
                .set(MESSAGE_LOG.CHANNEL_ID, command.channelId())
                .execute();
    }
}
```

- [ ] **Step 4: Create PersistenceAutoConfiguration**

Create `korConverter/hexagonal/adapter/adapter-persistence/src/main/java/org/specter/converter/adapter/persistence/configuration/PersistenceAutoConfiguration.java`:

```java
package org.specter.converter.adapter.persistence.configuration;

import org.jooq.DSLContext;
import org.specter.converter.adapter.persistence.port.IgnoreUserPersistenceAdapter;
import org.specter.converter.adapter.persistence.port.IgnoreUserQueryAdapter;
import org.specter.converter.adapter.persistence.port.MessageLogRecordAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class PersistenceAutoConfiguration {

    @Bean
    public IgnoreUserPersistenceAdapter ignoreUserPersistenceAdapter(
            DSLContext dsl, ApplicationEventPublisher eventPublisher) {
        return new IgnoreUserPersistenceAdapter(dsl, eventPublisher);
    }

    @Bean
    public IgnoreUserQueryAdapter ignoreUserQueryAdapter(DSLContext dsl) {
        return new IgnoreUserQueryAdapter(dsl);
    }

    @Bean
    public MessageLogRecordAdapter messageLogRecordAdapter(DSLContext dsl) {
        return new MessageLogRecordAdapter(dsl);
    }
}
```

- [ ] **Step 5: Register AutoConfiguration**

Create `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
org.specter.converter.adapter.persistence.configuration.PersistenceAutoConfiguration
```

- [ ] **Step 6: Verify compilation**

```bash
./gradlew :adapter-persistence:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add korConverter/hexagonal/adapter/adapter-persistence/
git commit -m "feat(adapter): add jOOQ persistence adapters

IgnoreUserPersistenceAdapter (Load+Save with optimistic lock),
IgnoreUserQueryAdapter, MessageLogRecordAdapter.
PersistenceAutoConfiguration with AutoConfiguration.imports."
```

---

## Task 10: Adapter-Bot Refactoring

**Files:**
- Modify: `korConverter/hexagonal/adapter/adapter-bot/src/main/java/org/specter/converter/adapter/bot/listener/MessageListener.java`
- Modify: `korConverter/hexagonal/adapter/adapter-bot/src/main/java/org/specter/converter/adapter/bot/listener/CommandListener.java`
- Modify: `korConverter/hexagonal/adapter/adapter-bot/src/main/java/org/specter/converter/adapter/bot/configuration/BotConfiguration.java` → Rename to `BotAutoConfiguration.java`
- Create: `korConverter/hexagonal/adapter/adapter-bot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Rewrite MessageListener**

Replace `MessageListener.java` to use `ConvertMessageUseCase` and `CheckIgnoreUserUseCase` instead of `DiscordBotInPort`. Remove all domain imports. Use Command/Query/Result DTOs from application layer.

Key changes:
- Replace `DiscordBotInPort discordBotInPort` → `ConvertMessageUseCase convertMessageUseCase` + `CheckIgnoreUserUseCase checkIgnoreUserUseCase`
- Replace `discordBotInPort.checkAvailableStr()` / `engToKor()` → `convertMessageUseCase.execute(new ConvertMessageCommand(...))`
- Replace `discordBotInPort.checkIgnoredUser()` → `checkIgnoreUserUseCase.execute(new CheckIgnoreUserQuery(...))`
- Remove `discordBotInPort.logMessage()` calls (now handled inside ConvertMessageService)

- [ ] **Step 2: Rewrite CommandListener**

Replace `CommandListener.java` to use `AddIgnoreUserUseCase` and `RemoveIgnoreUserUseCase`.

Key changes:
- Replace `DiscordBotInPort` → `AddIgnoreUserUseCase` + `RemoveIgnoreUserUseCase`
- `/ignore` → `addIgnoreUserUseCase.execute(new AddIgnoreUserCommand(userId, channelId, name))`
- `/unignore` → `removeIgnoreUserUseCase.execute(new RemoveIgnoreUserCommand(userId, channelId))`

- [ ] **Step 3: Rename BotConfiguration → BotAutoConfiguration**

Rename the file and class. Add `@AutoConfiguration` annotation (replace `@Configuration`). Wire the new UseCase beans:

```java
@AutoConfiguration
public class BotAutoConfiguration {

    @Bean
    public BotProperties botProperties() { ... }

    @Bean
    public MessageListener messageListener(
            ConvertMessageUseCase convertUseCase,
            CheckIgnoreUserUseCase checkUseCase) {
        return new MessageListener(convertUseCase, checkUseCase);
    }

    @Bean
    public CommandListener commandListener(
            AddIgnoreUserUseCase addUseCase,
            RemoveIgnoreUserUseCase removeUseCase,
            BuildProperties buildProperties) {
        return new CommandListener(addUseCase, removeUseCase, buildProperties);
    }

    @Bean
    public JDA jda(BotProperties props, MessageListener msgListener,
                   CommandListener cmdListener) { ... }
}
```

- [ ] **Step 4: Register AutoConfiguration**

Create `korConverter/hexagonal/adapter/adapter-bot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
org.specter.converter.adapter.bot.configuration.BotAutoConfiguration
```

- [ ] **Step 5: Verify no domain imports in adapter-bot**

```bash
grep -r "import org.specter.converter.domain" korConverter/hexagonal/adapter/adapter-bot/src/main/
```

Expected: No matches (AD-1).

- [ ] **Step 6: Commit**

```bash
git add korConverter/hexagonal/adapter/adapter-bot/
git commit -m "refactor(adapter-bot): use UseCase ports instead of DiscordBotInPort

MessageListener → ConvertMessageUseCase + CheckIgnoreUserUseCase.
CommandListener → AddIgnoreUserUseCase + RemoveIgnoreUserUseCase.
BotAutoConfiguration with AutoConfiguration.imports (AD-1)."
```

---

## Task 11: Configuration Module — TX Proxies

**Files:**
- Create: `korConverter/configuration/src/main/java/org/specter/converter/configuration/ConverterBeanAutoConfiguration.java`
- Create: `korConverter/configuration/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Create ConverterBeanAutoConfiguration**

Create `korConverter/configuration/src/main/java/org/specter/converter/configuration/ConverterBeanAutoConfiguration.java`:

```java
package org.specter.converter.configuration;

import java.lang.reflect.Proxy;
import java.time.Clock;
import org.specter.converter.adapter.persistence.port.IgnoreUserPersistenceAdapter;
import org.specter.converter.adapter.persistence.port.IgnoreUserQueryAdapter;
import org.specter.converter.adapter.persistence.port.MessageLogRecordAdapter;
import org.specter.converter.application.port.input.AddIgnoreUserUseCase;
import org.specter.converter.application.port.input.CheckIgnoreUserUseCase;
import org.specter.converter.application.port.input.ConvertMessageUseCase;
import org.specter.converter.application.port.input.RemoveIgnoreUserUseCase;
import org.specter.converter.application.service.AddIgnoreUserService;
import org.specter.converter.application.service.CheckIgnoreUserService;
import org.specter.converter.application.service.ConvertMessageService;
import org.specter.converter.application.service.RemoveIgnoreUserService;
import org.specter.converter.domain.model.ConversionDomainService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ReflectionUtils;

@AutoConfiguration
public class ConverterBeanAutoConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ConversionDomainService conversionDomainService() {
        return new ConversionDomainService();
    }

    @Bean
    public AddIgnoreUserUseCase addIgnoreUserUseCase(
            IgnoreUserPersistenceAdapter adapter,
            Clock clock,
            PlatformTransactionManager txManager) {
        return createTxProxy(
                new AddIgnoreUserService(adapter, adapter, clock),
                AddIgnoreUserUseCase.class,
                txManager);
    }

    @Bean
    public RemoveIgnoreUserUseCase removeIgnoreUserUseCase(
            IgnoreUserPersistenceAdapter adapter,
            Clock clock,
            PlatformTransactionManager txManager) {
        return createTxProxy(
                new RemoveIgnoreUserService(adapter, adapter, clock),
                RemoveIgnoreUserUseCase.class,
                txManager);
    }

    @Bean
    public ConvertMessageUseCase convertMessageUseCase(
            ConversionDomainService conversionService,
            MessageLogRecordAdapter messageLogAdapter,
            PlatformTransactionManager txManager) {
        return createTxProxy(
                new ConvertMessageService(conversionService, messageLogAdapter),
                ConvertMessageUseCase.class,
                txManager);
    }

    @Bean
    public CheckIgnoreUserUseCase checkIgnoreUserUseCase(
            IgnoreUserQueryAdapter queryAdapter,
            PlatformTransactionManager txManager) {
        return createReadOnlyTxProxy(
                new CheckIgnoreUserService(queryAdapter),
                CheckIgnoreUserUseCase.class,
                txManager);
    }

    @SuppressWarnings("unchecked")
    private <T> T createTxProxy(T target, Class<T> iface,
                                 PlatformTransactionManager txManager) {
        var template = new TransactionTemplate(txManager);
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                (proxy, method, args) -> template.execute(
                        status -> ReflectionUtils.invokeMethod(method, target, args)));
    }

    @SuppressWarnings("unchecked")
    private <T> T createReadOnlyTxProxy(T target, Class<T> iface,
                                         PlatformTransactionManager txManager) {
        var template = new TransactionTemplate(txManager);
        template.setReadOnly(true);
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                (proxy, method, args) -> template.execute(
                        status -> ReflectionUtils.invokeMethod(method, target, args)));
    }
}
```

- [ ] **Step 2: Register AutoConfiguration**

Create `korConverter/configuration/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
org.specter.converter.configuration.ConverterBeanAutoConfiguration
```

- [ ] **Step 3: Commit**

```bash
git add korConverter/configuration/
git commit -m "feat(configuration): add TX proxy bean configuration

ConverterBeanAutoConfiguration with R/W and R/O TX proxies (T-1).
AutoConfiguration.imports registration."
```

---

## Task 12: Boot Module Cleanup + application.yml

**Files:**
- Modify: `korConverter/boot/src/main/java/org/specter/converter/boot/Boot.java`
- Delete: `korConverter/boot/src/main/java/org/specter/converter/boot/configuration/InPortConfiguration.java`
- Modify: `runtime/cfg/application.yml` (replace JPA config with jOOQ/Flyway)

- [ ] **Step 1: Simplify Boot.java**

Replace:

```java
@SpringBootApplication(scanBasePackages = "org.specter.converter")
```

With:

```java
@SpringBootApplication
```

- [ ] **Step 2: Delete old InPortConfiguration**

```bash
rm korConverter/boot/src/main/java/org/specter/converter/boot/configuration/InPortConfiguration.java
rmdir korConverter/boot/src/main/java/org/specter/converter/boot/configuration/
```

- [ ] **Step 3: Update application.yml**

Replace JPA configuration with Flyway/jOOQ:

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER_NAME}
    password: ${DB_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration

logging:
  level:
    root: INFO

bot:
  token: ${DISCORD_BOT_TOKEN}
```

Remove all `spring.jpa.*` entries.

- [ ] **Step 4: Verify full build compiles**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(boot): simplify to pure @SpringBootApplication

Remove scanBasePackages, delete InPortConfiguration.
Replace JPA config with Flyway in application.yml."
```

---

## Task 13: ArchUnit Tests

**Files:**
- Create: `korConverter/boot/src/test/java/org/specter/converter/architecture/ArchitectureTest.java`

- [ ] **Step 1: Create ArchUnit test**

Create `korConverter/boot/src/test/java/org/specter/converter/architecture/ArchitectureTest.java`:

```java
package org.specter.converter.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "org.specter.converter")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta..");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_jooq =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.jooq..");

    @ArchTest
    static final ArchRule application_must_not_depend_on_spring =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta..");

    @ArchTest
    static final ArchRule adapter_bot_must_not_depend_on_domain =
            noClasses().that().resideInAPackage("..adapter.bot..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..domain.model..", "..domain.event..",
                            "..domain.service..", "..domain.exception..");

    @ArchTest
    static final ArchRule adapter_bot_must_not_depend_on_persistence =
            noClasses().that().resideInAPackage("..adapter.bot..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter.persistence..");

    @ArchTest
    static final ArchRule adapter_persistence_must_not_depend_on_bot =
            noClasses().that().resideInAPackage("..adapter.persistence..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter.bot..");
}
```

- [ ] **Step 2: Run ArchUnit tests**

```bash
./gradlew :boot:test --tests "*.ArchitectureTest"
```

Expected: ALL PASS. If any fail, fix the violating imports.

- [ ] **Step 3: Commit**

```bash
git add korConverter/boot/src/test/
git commit -m "test(architecture): add ArchUnit rules

Domain/Application no Spring, adapter-bot no domain, no cross-adapter."
```

---

## Task 14: Adapter Integration Tests (Testcontainers)

**Files:**
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/AdapterTestBase.java`
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/port/IgnoreUserPersistenceAdapterTest.java`
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/port/MessageLogRecordAdapterTest.java`

- [ ] **Step 1: Create shared Testcontainers base**

Create `AdapterTestBase.java`:

```java
package org.specter.converter.adapter.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AdapterTestBase {

    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("postgres:17-alpine").withReuse(true);

    static {
        PG.start();
    }

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }
}
```

- [ ] **Step 2: Create IgnoreUserPersistenceAdapter test**

Create `IgnoreUserPersistenceAdapterTest.java`:

```java
package org.specter.converter.adapter.persistence.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.specter.converter.adapter.persistence.generated.Tables.IGNORE_USER;

import java.time.Instant;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.specter.converter.adapter.persistence.AdapterTestBase;
import org.specter.converter.adapter.persistence.configuration.PersistenceAutoConfiguration;
import org.specter.converter.domain.model.ChannelId;
import org.specter.converter.domain.model.IgnoreUser;
import org.specter.converter.domain.model.UserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = IgnoreUserPersistenceAdapterTest.TestConfig.class)
class IgnoreUserPersistenceAdapterTest extends AdapterTestBase {

    @ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        JooqAutoConfiguration.class,
        PersistenceAutoConfiguration.class
    })
    static class TestConfig {}

    @Autowired DSLContext dsl;
    @Autowired IgnoreUserPersistenceAdapter adapter;

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void cleanup() {
        dsl.deleteFrom(IGNORE_USER).execute();
    }

    @Test
    void save_and_load() {
        var user = IgnoreUser.create(new UserId(123L), new ChannelId(456L), "test", NOW);
        adapter.save(user);

        var loaded = adapter.loadByUserIdAndChannelId(new UserId(123L), new ChannelId(456L));
        assertThat(loaded).isPresent();
        assertThat(loaded.get().userId()).isEqualTo(new UserId(123L));
        assertThat(loaded.get().channelId()).isEqualTo(new ChannelId(456L));
        assertThat(loaded.get().name()).isEqualTo("test");
    }

    @Test
    void load_returns_empty_when_not_found() {
        var result = adapter.loadByUserIdAndChannelId(new UserId(999L), new ChannelId(999L));
        assertThat(result).isEmpty();
    }

    @Test
    void delete_removes_record() {
        var user = IgnoreUser.create(new UserId(123L), new ChannelId(456L), "test", NOW);
        adapter.save(user);

        var loaded = adapter.loadByUserIdAndChannelId(new UserId(123L), new ChannelId(456L)).get();
        adapter.delete(loaded);

        assertThat(adapter.loadByUserIdAndChannelId(new UserId(123L), new ChannelId(456L)))
                .isEmpty();
    }
}
```

- [ ] **Step 3: Create MessageLogRecordAdapter test**

Create `MessageLogRecordAdapterTest.java`:

```java
package org.specter.converter.adapter.persistence.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.specter.converter.adapter.persistence.generated.Tables.MESSAGE_LOG;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.specter.converter.adapter.persistence.AdapterTestBase;
import org.specter.converter.adapter.persistence.configuration.PersistenceAutoConfiguration;
import org.specter.converter.application.dto.command.RecordMessageLogCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = MessageLogRecordAdapterTest.TestConfig.class)
class MessageLogRecordAdapterTest extends AdapterTestBase {

    @ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        JooqAutoConfiguration.class,
        PersistenceAutoConfiguration.class
    })
    static class TestConfig {}

    @Autowired DSLContext dsl;
    @Autowired MessageLogRecordAdapter adapter;

    @BeforeEach
    void cleanup() {
        dsl.deleteFrom(MESSAGE_LOG).execute();
    }

    @Test
    void records_message_log() {
        var command = new RecordMessageLogCommand(
                1L, "general", "nick", "effective",
                "dkssud", true, "안녕", 456L);

        adapter.record(command);

        var count = dsl.fetchCount(MESSAGE_LOG);
        assertThat(count).isEqualTo(1);
    }
}
```

- [ ] **Step 4: Run adapter tests**

```bash
./gradlew :adapter-persistence:test
```

Expected: ALL PASS (requires Docker for Testcontainers).

- [ ] **Step 5: Commit**

```bash
git add korConverter/hexagonal/adapter/adapter-persistence/src/test/
git commit -m "test(adapter): add Testcontainers integration tests

IgnoreUserPersistenceAdapter save/load/delete + MessageLogRecordAdapter."
```

---

## Task 15: Quality Gates — JaCoCo + PIT + Checkstyle + Lefthook

**Files:**
- Modify: `build.gradle.kts` (root — add JaCoCo, PIT, Checkstyle configuration)
- Create: `config/checkstyle/checkstyle.xml`
- Create: `lefthook.yml`

- [ ] **Step 1: Add JaCoCo to root build.gradle.kts**

```kotlin
subprojects {
    apply(plugin = "jacoco")

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        violationRules {
            rule {
                limit {
                    minimum = 0.80.toBigDecimal()
                }
            }
        }
    }
}
```

- [ ] **Step 2: Add PIT to domain module**

Add to `korConverter/hexagonal/domain/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.pitest)
}

pitest {
    targetClasses.set(listOf("org.specter.converter.domain.*"))
    targetTests.set(listOf("org.specter.converter.domain.*"))
    mutationThreshold.set(70)
    junit5PluginVersion.set("1.2.1")
}
```

- [ ] **Step 3: Add Checkstyle configuration**

Create `config/checkstyle/checkstyle.xml` with Google Java Style base + custom rules for forbidden suffixes and Instant.now() prohibition.

Add to root `build.gradle.kts`:

```kotlin
subprojects {
    apply(plugin = "checkstyle")

    checkstyle {
        toolVersion = libs.versions.checkstyle.get()
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }
}
```

- [ ] **Step 4: Create Lefthook configuration**

Create `lefthook.yml`:

```yaml
pre-commit:
  commands:
    spotless:
      run: ./gradlew spotlessCheck
    checkstyle:
      run: ./gradlew checkstyleMain

pre-push:
  commands:
    unit-test:
      run: ./gradlew :domain:test :application:test
```

- [ ] **Step 5: Install Lefthook**

```bash
lefthook install
```

- [ ] **Step 6: Verify full build with quality gates**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL with all quality checks passing.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "build: add 5-layer quality gates

JaCoCo 80%, PIT mutation 70% (domain), Checkstyle, Lefthook.
Spotless Google Java Format already added in Task 1."
```

---

## Task 16: CI Pipeline — 5-Gate GitHub Actions

**Files:**
- Rewrite: `.github/workflows/develop-build.yml`
- Rewrite: `.github/workflows/main-build.yml`

- [ ] **Step 1: Rewrite develop-build.yml with 5 gates**

```yaml
name: CI Pipeline (develop)

on:
  pull_request:
    branches: [develop]

jobs:
  gate-1-format-compile:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'corretto'
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew spotlessCheck checkstyleMain compileJava

  gate-2-unit-archunit-jacoco:
    needs: gate-1-format-compile
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'corretto'
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew :domain:test :application:test :boot:test jacocoTestCoverageVerification

  gate-3-integration:
    needs: gate-2-unit-archunit-jacoco
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'corretto'
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew :adapter-persistence:test

  gate-4-mutation:
    needs: gate-2-unit-archunit-jacoco
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'corretto'
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew :domain:pitest

  gate-5-security:
    needs: gate-1-format-compile
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'corretto'
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew dependencyCheckAnalyze || true
```

- [ ] **Step 2: Copy same structure to main-build.yml**

Same as develop-build.yml but triggered on `pull_request: branches: [main]`.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/
git commit -m "ci: add 5-gate pipeline

Gate 1: format + compile
Gate 2: unit + ArchUnit + JaCoCo
Gate 3: integration (Testcontainers)
Gate 4: mutation (PIT)
Gate 5: security (OWASP)"
```

---

## Task 17: E2E Test (@SpringBootTest)

**Files:**
- Create: `korConverter/boot/src/test/java/org/specter/converter/boot/IgnoreUserE2ETest.java`

- [ ] **Step 1: Create E2E test**

Create `korConverter/boot/src/test/java/org/specter/converter/boot/IgnoreUserE2ETest.java`:

```java
package org.specter.converter.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.specter.converter.application.dto.command.AddIgnoreUserCommand;
import org.specter.converter.application.dto.command.RemoveIgnoreUserCommand;
import org.specter.converter.application.dto.query.CheckIgnoreUserQuery;
import org.specter.converter.application.port.input.AddIgnoreUserUseCase;
import org.specter.converter.application.port.input.CheckIgnoreUserUseCase;
import org.specter.converter.application.port.input.RemoveIgnoreUserUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class IgnoreUserE2ETest {

    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("postgres:17-alpine").withReuse(true);

    static { PG.start(); }

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
        registry.add("bot.token", () -> "test-token");
    }

    @Autowired AddIgnoreUserUseCase addUseCase;
    @Autowired CheckIgnoreUserUseCase checkUseCase;
    @Autowired RemoveIgnoreUserUseCase removeUseCase;

    @Test
    void full_lifecycle_add_check_remove() {
        // Add
        var result = addUseCase.execute(new AddIgnoreUserCommand(111L, 222L, "e2eUser"));
        assertThat(result.userId()).isEqualTo(111L);
        assertThat(result.name()).isEqualTo("e2eUser");

        // Check exists
        assertThat(checkUseCase.execute(new CheckIgnoreUserQuery(111L, 222L))).isTrue();

        // Remove
        removeUseCase.execute(new RemoveIgnoreUserCommand(111L, 222L));

        // Check gone
        assertThat(checkUseCase.execute(new CheckIgnoreUserQuery(111L, 222L))).isFalse();
    }
}
```

Note: JDA bean creation will fail without a real Discord token. The E2E test may need to mock or conditionally exclude the JDA bean. Add `@MockBean JDA jda` or use a test profile that disables bot auto-configuration.

- [ ] **Step 2: Run E2E test**

```bash
./gradlew :boot:test --tests "*.IgnoreUserE2ETest"
```

Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add korConverter/boot/src/test/
git commit -m "test(e2e): add full lifecycle @SpringBootTest

Add → Check → Remove flow with Testcontainers PostgreSQL."
```

---

## Task 18: Update CLAUDE.md + Rules + ADR (Documentation)

**Files:**
- Modify: `.claude/CLAUDE.md`
- Modify: `.claude/rules/validation.md`
- Modify: `.claude/rules/adapter.md` (if exists, update for jOOQ)
- Create: `docs/decisions/0001-bigserial-id-strategy.md`

- [ ] **Step 1: Write ADR for BIGSERIAL ID strategy**

Create `docs/decisions/0001-bigserial-id-strategy.md`:

```markdown
# ADR-0001: BIGSERIAL ID Strategy

## Status
Accepted

## Context
playbook recommends UUIDv7 (RFC 9562) for Aggregate IDs. However, this project:
- Uses Discord Snowflake IDs (long-based) for UserId/ChannelId
- Has existing BIGSERIAL schema
- Operates as single BC + single DB where UUID's distributed benefits are minimal

## Decision
Keep DB BIGSERIAL for IgnoreUserId. IgnoreUserId.UNSAVED (0L) for pre-save state.

## Consequences
- Pro: Simpler schema, no UUID parsing overhead, natural index ordering
- Con: Deviates from playbook UUIDv7 recommendation
- Con: ID not assignable before persistence (UNSAVED sentinel needed)
```

- [ ] **Step 2: Update CLAUDE.md tech stack**

Update the tech stack section to reflect:
- JPA → jOOQ + Flyway
- Added: Testcontainers, ArchUnit, jqwik, PIT, Spotless, Checkstyle, Lefthook
- Removed: H2, Lombok (from domain)
- AutoConfiguration per module (not @ComponentScan)

- [ ] **Step 3: Update validation.md**

Add jOOQ-specific validation rules:
- SavePort: `WHERE version = ?` check
- `reconstitute()` usage in adapter
- No JPA imports anywhere

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "docs: update CLAUDE.md, rules, and add ADR-0001

ADR-0001: BIGSERIAL ID strategy deviation from playbook UUIDv7.
Updated tech stack and validation rules for jOOQ migration."
```

---

## Task 19: Final Build Verification

- [ ] **Step 1: Run full build**

```bash
./gradlew clean build
```

Expected: BUILD SUCCESSFUL with all tests passing.

- [ ] **Step 2: Run Spotless format**

```bash
./gradlew spotlessApply
```

- [ ] **Step 3: Run all tests individually**

```bash
./gradlew :domain:test
./gradlew :application:test
./gradlew :adapter-persistence:test
./gradlew :boot:test
```

Expected: ALL PASS

- [ ] **Step 4: Verify validation checklist**

Run the self-validation from `.claude/rules/validation.md`:
- No Spring imports in domain or application
- No JPA imports anywhere
- Aggregate has private constructor + create()/reconstitute()
- Sealed interface for events, sealed class for exceptions
- Command/Query are records with primitive types
- SavePort does `WHERE version = ?`

- [ ] **Step 5: Final commit if any formatting changes**

```bash
git add -A
git commit -m "style: apply Spotless formatting across all modules"
```
