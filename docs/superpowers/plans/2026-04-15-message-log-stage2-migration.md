# message_log Stage 2 (V004) — TEXT + DEFAULT 제거 + TIMESTAMPTZ 전환 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the `message_log` table to its purist-DDD target shape by widening text columns to `TEXT`, removing the legacy `DEFAULT 0` on `channel_id`, and converting timestamps to `TIMESTAMPTZ` with Asia/Seoul wall-clock interpretation — in a single Flyway V004 migration with a regression test.

**Architecture:** V004 uses the established jOOQ-ignore pattern from V003: PostgreSQL-specific `ALTER COLUMN TYPE ... USING ... AT TIME ZONE` is wrapped in `--[jooq ignore start/stop]` so Flyway executes it while jOOQ's H2-based DDLDatabase skips it, with plain TYPE-only duplicate ALTERs outside the block to let jOOQ infer the final column types. A new `MessageLogV004MigrationIT` mirrors `LegacyToTargetV003MigrationIT`'s structure: isolated Testcontainer, two-stage Flyway (target=3 then target=4), raw-JDBC pre/post assertions.

**Tech Stack:** PostgreSQL 17, Flyway 11, jOOQ 3.20 (DDLDatabase codegen), Testcontainers, JUnit 5, Gradle 9, Spring Boot 4

**Design Spec:** `docs/superpowers/specs/2026-04-15-message-log-stage2-migration-design.md`

---

## File Structure

**Files created:**
- `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V004__message_log_text_default_and_timestamptz.sql` — forward migration (TEXT widening, DEFAULT drop, TIMESTAMPTZ conversion)
- `korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/migration/MessageLogV004MigrationIT.java` — migration regression test (isolated container, target=3 → raw insert → target=4 → assertions)

**Files regenerated automatically:**
- `korConverter/hexagonal/adapter/adapter-persistence/build/generated/sources/jooq/main/org/specter/converter/adapter/persistence/generated/tables/MessageLog.java` — `MESSAGE`/`CONVERTED_MESSAGE` stay `String` (VARCHAR→TEXT both map to `String`), `CHANNEL_ID` loses default metadata, `CREATED_AT`/`UPDATED_AT` transition from `LocalDateTime` to `OffsetDateTime`

**Files NOT changed:**
- Any Java source in `korConverter/hexagonal/adapter/adapter-persistence/src/main/java/**` — `MessageLogRecordAdapter` inserts only business fields (no timestamps), and `String` is unchanged regardless of VARCHAR vs TEXT
- `korConverter/hexagonal/adapter/adapter-persistence/src/test/java/.../port/MessageLogRecordAdapterTest.java` — short-message assertions only, no timestamp references
- `runtime/cfg/application.yml` — Flyway baseline config from Stage 1 is sufficient

Rationale: the Java type surface exposed by jOOQ for the affected columns is either identical (`String` for both VARCHAR and TEXT) or untouched by adapter code (`OffsetDateTime` for new timestamp type, never read by `MessageLogRecordAdapter`). This is verified against the adapter source and test at plan time.

---

## Task 1: Create V004 migration

The V004 migration lands first so that subsequent jOOQ codegen reflects the target schema. All four changes (TEXT widening ×2, DROP DEFAULT, TIMESTAMPTZ conversion ×2) are in one file.

**Files:**
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V004__message_log_text_default_and_timestamptz.sql`

- [ ] **Step 1: Verify green baseline before changes**

Run from repo root with Java 25:

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :adapter-persistence:test
```

Expected: BUILD SUCCESSFUL. The existing `LegacyToTargetV003MigrationIT` + adapter tests + query tests all pass on Stage 1 commit `1dbae89` (the Stage 2 spec commit). If this fails, STOP and report BLOCKED.

- [ ] **Step 2: Create the V004 SQL file**

Full content of `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V004__message_log_text_default_and_timestamptz.sql`:

```sql
-- V004: message_log Stage 2 alignment to purist-DDD target
--   - message / converted_message: VARCHAR(255) -> TEXT (Discord 2000-char support)
--   - channel_id DEFAULT 0 removal (JPA @ColumnDefault artifact, no business meaning)
--   - created_at / updated_at TIMESTAMPTZ conversion (KST wall-clock interpretation)
-- Adapter code is unaffected: String remains String, adapter does not read timestamps.

-- 1) TEXT widening. Plain ALTER TYPE TEXT is parsable by jOOQ DDLDatabase (H2).
ALTER TABLE message_log ALTER COLUMN message TYPE TEXT;
ALTER TABLE message_log ALTER COLUMN converted_message TYPE TEXT;

-- 2) Drop the JPA-era DEFAULT 0 on channel_id. Nullable remains.
ALTER TABLE message_log ALTER COLUMN channel_id DROP DEFAULT;

-- 3) TIMESTAMPTZ conversion with KST interpretation (same pattern as V003).
--    Existing naive timestamps were written by the old JPA BaseTimeEntity using
--    LocalDateTime.now(ZoneId.systemDefault()) on a container with TZ=Asia/Seoul
--    (deploy/docker-compose.yml). 'AT TIME ZONE Asia/Seoul' re-interprets them
--    as KST and produces the correct UTC instant.
--
--    The multi-action ALTER with USING is PostgreSQL-specific and not supported
--    by jOOQ DDLDatabase (H2 simulation). The jooq-ignore block contains the
--    USING form (executed by Flyway, skipped by jOOQ).
--[jooq ignore start]
ALTER TABLE message_log
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'Asia/Seoul',
    ALTER COLUMN created_at SET DEFAULT now(),
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ
        USING updated_at AT TIME ZONE 'Asia/Seoul',
    ALTER COLUMN updated_at SET DEFAULT now();
--[jooq ignore stop]
-- jOOQ DDLDatabase hints (Flyway also executes these, but they are no-ops in
-- PostgreSQL because the USING-form above already performed the conversion).
-- These TYPE-only ALTERs let jOOQ (H2 simulation) infer the final column types
-- as TIMESTAMPTZ -> OffsetDateTime.
ALTER TABLE message_log ALTER COLUMN created_at TYPE TIMESTAMPTZ;
ALTER TABLE message_log ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE message_log ALTER COLUMN updated_at TYPE TIMESTAMPTZ;
ALTER TABLE message_log ALTER COLUMN updated_at SET DEFAULT now();
```

- [ ] **Step 3: Regenerate jOOQ code and verify existing tests still pass**

jOOQ codegen is wired to `compileJava`. Running the test task regenerates and compiles.

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :adapter-persistence:test
```

Expected: BUILD SUCCESSFUL.
- `IgnoreUserPersistenceAdapterTest` (3 tests) — message_log untouched, passes
- `IgnoreUserQueryAdapterTest` — same
- `MessageLogRecordAdapterTest` (2 tests) — passes because: (a) `MESSAGE`/`CONVERTED_MESSAGE` Java type is still `String`, (b) `CHANNEL_ID` is set explicitly by the adapter (DEFAULT irrelevant), (c) timestamps are never read in assertions
- `LegacyToTargetV003MigrationIT` — passes because it only migrates up to `target("3")`; V004 is ignored at that boundary

If `MessageLogRecordAdapterTest` fails with a compile error, open `build/generated/sources/jooq/main/org/specter/converter/adapter/persistence/generated/tables/MessageLog.java` and confirm `MESSAGE` and `CONVERTED_MESSAGE` are `TableField<MessageLogRecord, String>`. If they became `Clob` or similar, the jOOQ TEXT mapping is off — investigate the DDLDatabase mapping before proceeding.

- [ ] **Step 4: Commit**

```bash
git add korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V004__message_log_text_default_and_timestamptz.sql
git commit -m "$(cat <<'EOF'
feat(adapter-persistence): add V004 message_log alignment (TEXT, DEFAULT drop, TIMESTAMPTZ)

Stage 2 of the purist-DDD migration series. V004 widens message_log.message
and converted_message from VARCHAR(255) to TEXT (supporting the Discord
2000-character limit), drops the legacy JPA @ColumnDefault("0") on
channel_id, and converts created_at/updated_at to TIMESTAMPTZ interpreting
the existing naive values as Asia/Seoul wall-clock (matching the deploy
container TZ, same pattern V003 used for ignore_user).

Uses the --[jooq ignore start/stop] workaround from V003 for the
PostgreSQL-specific ALTER COLUMN TYPE ... USING ... AT TIME ZONE clause
that jOOQ's H2-based DDLDatabase cannot parse. Plain TYPE-only ALTERs
outside the block give jOOQ enough information to infer the final column
types (CREATED_AT/UPDATED_AT -> OffsetDateTime).

Adapter code is unaffected: String remains String across VARCHAR/TEXT,
MessageLogRecordAdapter sets channel_id explicitly (DEFAULT is irrelevant
to the insert path), and the adapter never reads timestamp columns.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Add MessageLogV004MigrationIT

New integration test that drives Flyway in two stages: `target("3")` to reach the post-V3 state, raw-JDBC insert of legacy-shaped rows, then `target("4")` to apply V004, then raw-JDBC assertions on all four requirements.

**Files:**
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/migration/MessageLogV004MigrationIT.java`

- [ ] **Step 1: Write the test**

Create the file with:

```java
package org.specter.converter.adapter.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Regression test for Flyway V004 forward migration of message_log.
 *
 * <p>Uses an isolated Postgres container (not shared with {@code AdapterTestBase} or
 * {@code LegacyToTargetV003MigrationIT}) so the two-stage {@code target("3")} then
 * {@code target("4")} Flyway lifecycle does not interfere with the full-migrate pattern
 * used by adapter/port tests.
 *
 * <p>Raw JDBC (not jOOQ DSL) is used for pre-V4 writes because jOOQ generated types reflect
 * the final post-V4 schema only.
 *
 * <p>Guards the four V004 changes: (1) {@code message}/{@code converted_message} accept
 * strings longer than 255 chars (TEXT widening), (2) {@code channel_id} no longer gets 0
 * when unspecified (DEFAULT drop), (3) existing {@code created_at}/{@code updated_at} values
 * are interpreted as KST wall-clock and converted to correct UTC instants, (4) new INSERTs
 * after V4 still succeed (regression check on unrelated columns).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MessageLogV004MigrationIT {

  // enables non-static @BeforeAll/@AfterAll tied to the class-scoped container lifecycle
  private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17-alpine");

  @BeforeAll
  void startContainer() {
    PG.start();
  }

  @AfterAll
  void stopContainer() {
    PG.stop();
  }

  @Test
  void v004_migrates_message_log_to_text_no_default_and_timestamptz() throws Exception {
    // --- 1. Migrate to V3 only (post-Stage 1 state) ---
    Flyway.configure()
        .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
        .locations("classpath:db/migration")
        .target("3")
        .load()
        .migrate();

    // --- 2. Insert legacy-shaped row via raw JDBC ---
    // message_log: short message (VARCHAR(255) still enforced), KST wall-clock timestamps.
    // id is explicit because V003's IDENTITY is BY DEFAULT (accepts explicit values).
    try (Connection conn =
            DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(
          "INSERT INTO message_log (id, guild, channel, nick_name, effective_name, message, "
              + "is_converted, converted_message, channel_id, created_at, updated_at) "
              + "VALUES (300, 'g', 'c', 'nk', 'ef', 'dkssud', true, '안녕', 333, "
              + "TIMESTAMP '2025-12-31 23:00:00', TIMESTAMP '2025-12-31 23:00:00')");
    }

    // --- 3. Migrate to V4 ---
    Flyway.configure()
        .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
        .locations("classpath:db/migration")
        .target("4")
        .load()
        .migrate();

    // --- 4. Assert migration effects ---
    try (Connection conn =
            DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Statement stmt = conn.createStatement()) {

      // 4a. Existing row's timestamps converted to UTC via AT TIME ZONE 'Asia/Seoul'.
      //     2025-12-31 23:00:00 KST == 2025-12-31 14:00:00 UTC.
      try (ResultSet rs =
          stmt.executeQuery(
              "SELECT created_at, updated_at FROM message_log WHERE id = 300")) {
        assertThat(rs.next()).as("message_log row id=300 must survive V4 migration").isTrue();
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
        assertThat(createdAt).isEqualTo(Instant.parse("2025-12-31T14:00:00Z"));
        assertThat(updatedAt).isEqualTo(Instant.parse("2025-12-31T14:00:00Z"));
      }

      // 4b. TEXT widening: a 2000-char message is accepted.
      String longMessage = "a".repeat(2000);
      try (var ps =
          conn.prepareStatement(
              "INSERT INTO message_log (guild, channel, nick_name, effective_name, message, "
                  + "is_converted, converted_message, channel_id) "
                  + "VALUES ('g2', 'c2', 'nk2', 'ef2', ?, false, null, 444)")) {
        ps.setString(1, longMessage);
        ps.executeUpdate();
      }
      try (ResultSet rs =
          stmt.executeQuery("SELECT message FROM message_log WHERE channel_id = 444")) {
        assertThat(rs.next())
            .as("new message_log INSERT with 2000-char message must succeed after V004 TEXT widening")
            .isTrue();
        assertThat(rs.getString("message")).hasSize(2000);
      }

      // 4c. channel_id DEFAULT 0 removed: INSERT without channel_id yields NULL.
      stmt.executeUpdate(
          "INSERT INTO message_log (guild, channel, nick_name, effective_name, message, "
              + "is_converted, converted_message) "
              + "VALUES ('g3', 'c3', 'nk3', 'ef3', 'hi', false, null)");
      try (ResultSet rs =
          stmt.executeQuery("SELECT channel_id FROM message_log WHERE guild = 'g3'")) {
        assertThat(rs.next())
            .as("new message_log row without explicit channel_id must survive")
            .isTrue();
        rs.getLong("channel_id");
        assertThat(rs.wasNull())
            .as("channel_id must be NULL after V004 drops the DEFAULT 0")
            .isTrue();
      }
    }
  }
}
```

Why split the long-message INSERT onto `PreparedStatement`: embedding a 2000-character literal inside a SQL string by concatenation works but reads poorly. `PreparedStatement.setString(1, longMessage)` is the idiomatic approach and avoids any escaping concerns.

Why check `rs.wasNull()` separately for `channel_id`: `ResultSet.getLong()` returns `0` for SQL NULL, which is indistinguishable from a stored `0`. The `wasNull()` call is the only way to verify NULL-ness after `getLong()`.

- [ ] **Step 2: Run the new test**

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :adapter-persistence:test --tests "org.specter.converter.adapter.persistence.migration.MessageLogV004MigrationIT"
```

Expected: BUILD SUCCESSFUL, 1 test passed.

Diagnosis if failure:
- Flyway error on `target("3")`: V003 is broken on this branch. Run `git log --oneline 4f5131c..HEAD` to confirm `05378a1` is reachable.
- `AssertionError` at line "created_at must equal 14:00:00Z": V004 `AT TIME ZONE 'Asia/Seoul'` clause is wrong or absent. Check the jooq-ignore block in V004.
- `AssertionError` at "channel_id must be NULL": V004's `DROP DEFAULT` on channel_id failed or was omitted.
- `PSQLException: value too long for type character varying(255)`: V004's TEXT widening was not executed (jooq-ignore wrapping a plain ALTER TYPE? the TEXT lines should NOT be in the ignore block).
- Jooq code compile failure on the test: check that `MessageLog.MESSAGE` is still `String`. If it became `byte[]` or `Clob`, the DDLDatabase TEXT mapping needs configuration review.

- [ ] **Step 3: Run the full adapter-persistence test suite**

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :adapter-persistence:test
```

Expected: BUILD SUCCESSFUL. All pre-existing tests (Stage 1 era) still pass plus the new `MessageLogV004MigrationIT`.

- [ ] **Step 4: Apply Spotless formatting**

The project uses google-java-format via Spotless. Apply auto-fix so the next full build does not fail on format check.

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :adapter-persistence:spotlessApply
git diff korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/migration/MessageLogV004MigrationIT.java
```

Expected: zero or small whitespace-only diff. If the diff touches logic, review before committing.

- [ ] **Step 5: Commit**

```bash
git add korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/migration/MessageLogV004MigrationIT.java
git commit -m "$(cat <<'EOF'
test(adapter-persistence): add MessageLogV004MigrationIT

Drives Flyway in two stages (target=3 then target=4) against a fresh
Testcontainer, inserts a legacy-shaped message_log row between stages,
then verifies V004's four effects: timestamp KST->UTC interpretation on
the existing row, 2000-character message INSERT success (TEXT widening),
channel_id NULL after omitted INSERT (DEFAULT 0 removed), and survival
of pre-existing rows.

Mirrors LegacyToTargetV003MigrationIT's isolated-container +
raw-JDBC pattern so each Vxxx gets its own focused regression guard.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Full build verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full project build**

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew build
```

Expected: BUILD SUCCESSFUL. Runs Spotless check, Checkstyle, jOOQ codegen, ErrorProne/NullAway, all tests across all modules, JaCoCo report.

- [ ] **Step 2: Spot-check the regenerated jOOQ code**

Open `korConverter/hexagonal/adapter/adapter-persistence/build/generated/sources/jooq/main/org/specter/converter/adapter/persistence/generated/tables/MessageLog.java` and verify:

- `MESSAGE` field is `TableField<MessageLogRecord, String>` with a CLOB/TEXT-class `SQLDataType` (exact internal type name can be `CLOB` or similar — the important invariant is the Java generic parameter is `String`, not `byte[]` or `Clob`)
- `CONVERTED_MESSAGE` same
- `CHANNEL_ID` is `TableField<MessageLogRecord, Long>` WITHOUT a `.defaultValue(...)` call (or with a null default)
- `CREATED_AT` is `TableField<MessageLogRecord, OffsetDateTime>` using `SQLDataType.TIMESTAMPWITHTIMEZONE(6)`
- `UPDATED_AT` same

If `CREATED_AT`/`UPDATED_AT` are still `LocalDateTime`, the TYPE-only ALTERs outside the jooq-ignore block are missing or wrong — re-check V004's bottom half.

- [ ] **Step 3: No commit needed (verification only)**

If any Spotless violations surfaced during the full build that were not caught in Task 2 Step 4, apply and commit:

```bash
./gradlew spotlessApply
git status
git add -A
git commit -m "style: apply spotless formatting after V004 migration"
```

Otherwise skip.

---

## Verification Summary

After all tasks:

1. `git log --oneline 1dbae89..HEAD` shows two new commits (feat V004, test V004), optionally a third Spotless follow-up.
2. `./gradlew build` passes cleanly.
3. `MessageLogV004MigrationIT` and `LegacyToTargetV003MigrationIT` both green.
4. Existing adapter/query/E2E tests unchanged, still passing.
5. jOOQ generated `MessageLog.java` reflects the final schema (String for TEXT, OffsetDateTime for timestamps, no channel_id default).
6. The three changed/created files and the generated jOOQ delta are the only changes.

## Production Deployment (runbook, not implementation task)

From spec §8:
1. `pg_dump --table=message_log` backup
2. Confirm `deploy/docker-compose.yml` still has `TZ: Asia/Seoul` (unchanged since Stage 1)
3. Stop bot container → start new image → Flyway auto-runs V004
4. Verify `flyway_schema_history` has a V4 success row
5. Discord smoke test: send a 2000-character message to exercise the TEXT widening
6. Spot-check: `SELECT created_at, updated_at FROM message_log ORDER BY id DESC LIMIT 5` — values should look like ISO-8601 with `+00` or `Z` offset

## Follow-up — Stage 3 (not in this plan)

See spec §5:
- Drop `message_log.updated_at` (append-only log, field unused)
- `DROP SEQUENCE IF EXISTS hibernate_sequence` if it exists
- Rename index `idx_ignore_user_user_id` → `idx_ignore_user_lookup`

Stage 3 will be a separate spec + plan cycle. Before starting, verify no code path reads `MessageLog.UPDATED_AT` (expected clean per this plan's file structure analysis, but re-check at Stage 3 start).
