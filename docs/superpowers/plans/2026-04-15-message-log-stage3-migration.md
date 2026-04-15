# message_log Stage 3 (V005) Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the purist-DDD migration series by dropping the unused `message_log.updated_at` column, dropping the legacy `hibernate_sequence` if present, and renaming the JPA-artifact index `idx_ignore_user_user_id` to the semantic `idx_ignore_user_lookup`.

**Architecture:** Single V005 Flyway migration bundling three purely structural changes (no data transformation). All SQL is H2-parsable so V003/V004's jooq-ignore workaround is not needed. A new `Stage3CleanupV005MigrationIT` mirrors V003/V004 ITs — isolated Testcontainer, two-stage Flyway (target=4 then target=5), raw-JDBC assertions via `information_schema`, `pg_indexes`, `pg_sequences` metadata queries.

**Tech Stack:** PostgreSQL 17, Flyway 11, jOOQ 3.20 (DDLDatabase codegen), Testcontainers, JUnit 5, Gradle 9, Spring Boot 4

**Design Spec:** `docs/superpowers/specs/2026-04-15-message-log-stage3-migration-design.md`

---

## File Structure

**Files created:**
- `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V005__stage3_cleanup_updated_at_sequence_and_index.sql` — forward migration (3 structural changes)
- `korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/migration/Stage3CleanupV005MigrationIT.java` — migration regression test

**Files regenerated automatically:**
- `korConverter/hexagonal/adapter/adapter-persistence/build/generated/sources/jooq/main/org/specter/converter/adapter/persistence/generated/tables/MessageLog.java` — `UPDATED_AT` TableField removed
- `korConverter/hexagonal/adapter/adapter-persistence/build/generated/sources/jooq/main/org/specter/converter/adapter/persistence/generated/tables/records/MessageLogRecord.java` — `getUpdatedAt()`/`setUpdatedAt()` removed
- `korConverter/hexagonal/adapter/adapter-persistence/build/generated/sources/jooq/main/org/specter/converter/adapter/persistence/generated/Indexes.java` — index constant renamed to `IDX_IGNORE_USER_LOOKUP`

**Files NOT changed:**
- Any Java source in `korConverter/hexagonal/adapter/adapter-persistence/src/main/java/**` — verified via grep: zero references to `MESSAGE_LOG.UPDATED_AT`, `MessageLog.UPDATED_AT`, `hibernate_sequence`, or the renamed index
- Any existing test file
- `runtime/cfg/application.yml` — Flyway baseline config from Stage 1 is sufficient

Rationale: V005's changes are either pure removal (column drop, sequence drop) or metadata-only (index rename). No Java type surface depends on the affected symbols.

---

## Task 1: Create V005 migration

V005 lands with the SQL file only. jOOQ codegen runs automatically on `compileJava` and regenerates `MessageLog.java` without `UPDATED_AT`. Because no adapter code references the removed symbols, the build stays green without touching Java sources.

**Files:**
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V005__stage3_cleanup_updated_at_sequence_and_index.sql`

- [ ] **Step 1: Verify green baseline**

Run from repo root with Java 25:

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
cd /home/kshull/project/kairyx/java/KorConverterBot-SpringBoot
./gradlew :adapter-persistence:test
```

Expected: BUILD SUCCESSFUL. The existing test suite (Stage 1 + Stage 2) passes on commit `681164c` (Stage 3 spec commit). If this fails, STOP and report BLOCKED.

- [ ] **Step 2: Create V005 SQL**

Create `korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V005__stage3_cleanup_updated_at_sequence_and_index.sql` with exactly this content:

```sql
-- V005__stage3_cleanup_updated_at_sequence_and_index.sql
-- Stage 3 cleanup — purely structural, no data transformation:
--   1) Drop message_log.updated_at (append-only log, field unused by adapter code)
--   2) Drop legacy hibernate_sequence if present (JPA @GeneratedValue artifact)
--   3) Rename idx_ignore_user_user_id to idx_ignore_user_lookup (semantic name)
-- Adapter code and tests are unaffected — verified via grep for MESSAGE_LOG.UPDATED_AT
-- (zero Java references) and index-by-name usage (none; indexes are optimizer-only).

-- 1) Drop the append-only log's unused updated_at column.
--    jOOQ will regenerate MessageLog without UPDATED_AT; no Java code references it.
ALTER TABLE message_log DROP COLUMN updated_at;

-- 2) Drop the legacy JPA shared sequence (if it exists in the deployment).
--    Fresh environments never created it; production may have it from pre-V001 JPA.
--    IF EXISTS makes this idempotent across environments.
DROP SEQUENCE IF EXISTS hibernate_sequence;

-- 3) Rename JPA-artifact index to semantic name.
ALTER INDEX idx_ignore_user_user_id RENAME TO idx_ignore_user_lookup;
```

- [ ] **Step 3: Regenerate jOOQ and verify existing tests still pass**

jOOQ codegen is wired to `compileJava`. Running the test task regenerates and compiles.

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :adapter-persistence:test
```

Expected: BUILD SUCCESSFUL. All existing tests pass:
- `IgnoreUserPersistenceAdapterTest` (3 tests) — index rename is transparent to queries
- `IgnoreUserQueryAdapterTest` — same
- `MessageLogRecordAdapterTest` (2 tests) — passes because the adapter's `.set()` calls never include `UPDATED_AT`, and the `selectFrom(MESSAGE_LOG).fetch()` returns only the columns that now exist
- `LegacyToTargetV003MigrationIT` — passes (migrates only to target=3, V5 not executed)
- `MessageLogV004MigrationIT` — passes (migrates only to target=4; its target=3 INSERT in step 2 includes `updated_at`, which still exists at that Flyway target)

If any test fails:
- Compile error `cannot find symbol UPDATED_AT` → a source file was referencing `MESSAGE_LOG.UPDATED_AT` that grep missed. Investigate and either revert the plan or widen the scope.
- `MessageLogV004MigrationIT` failure on target=3 INSERT → V005 ran prematurely (should not happen because target=3 stops before V5). Check Flyway configuration.
- Any runtime ignore_user query failure → index rename affected query plans unexpectedly. Check `pg_indexes` output in the failure trace.

- [ ] **Step 4: Spot-check the regenerated jOOQ code**

Open `korConverter/hexagonal/adapter/adapter-persistence/build/generated/sources/jooq/main/org/specter/converter/adapter/persistence/generated/tables/MessageLog.java` and verify:

- `UPDATED_AT` field is absent (no `public final TableField<MessageLogRecord, OffsetDateTime> UPDATED_AT = ...` line)
- `MESSAGE`, `CONVERTED_MESSAGE`, `CHANNEL_ID`, `CREATED_AT` fields are still present (Stage 2 state preserved)

Open `korConverter/hexagonal/adapter/adapter-persistence/build/generated/sources/jooq/main/org/specter/converter/adapter/persistence/generated/Indexes.java` and verify the ignore_user index constant is now `IDX_IGNORE_USER_LOOKUP` (exact name may be uppercased by jOOQ).

If either check fails, the migration SQL did not achieve the target state — re-inspect V005 content.

- [ ] **Step 5: Commit**

```bash
git add korConverter/hexagonal/adapter/adapter-persistence/src/main/resources/db/migration/V005__stage3_cleanup_updated_at_sequence_and_index.sql
git commit -m "$(cat <<'EOF'
feat(adapter-persistence): add V005 stage 3 cleanup (updated_at, hibernate_sequence, index rename)

Completes the purist-DDD migration series. Drops the unused
message_log.updated_at column (append-only log artifact from the old
JPA BaseTimeEntity), drops the legacy hibernate_sequence if present
in the deployment (JPA @GeneratedValue artifact, unused after V003's
IDENTITY transition made it redundant), and renames the
idx_ignore_user_user_id index to the semantic idx_ignore_user_lookup.

All three changes are purely structural — no data transformation. Pre-
verified via grep that no Java code references MessageLog.UPDATED_AT,
hibernate_sequence, or the index by name. All SQL is H2-parsable so
the jooq-ignore workaround used in V003/V004 is not needed.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Add Stage3CleanupV005MigrationIT

New integration test that drives Flyway in two stages (`target("4")` → raw JDBC pre-state + dummy row insert → `target("5")` → metadata assertions via `information_schema`, `pg_indexes`, `pg_sequences`).

**Files:**
- Create: `korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/migration/Stage3CleanupV005MigrationIT.java`

- [ ] **Step 1: Write the test**

Create the file with:

```java
package org.specter.converter.adapter.persistence.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Regression test for Flyway V005 forward migration (Stage 3 cleanup).
 *
 * <p>Uses an isolated Postgres container (not shared with {@code AdapterTestBase},
 * {@code LegacyToTargetV003MigrationIT}, or {@code MessageLogV004MigrationIT}) so the
 * two-stage {@code target("4")} then {@code target("5")} Flyway lifecycle does not interfere
 * with the full-migrate pattern used by adapter/port tests.
 *
 * <p>Raw JDBC (not jOOQ DSL) is used because the test asserts against metadata tables
 * ({@code information_schema.columns}, {@code pg_indexes}, {@code pg_sequences}) that have
 * no jOOQ generated mappings, and because asserting on the presence/absence of columns and
 * indexes is simpler with raw SQL.
 *
 * <p>Guards the three V005 changes: (1) {@code message_log.updated_at} is absent after V5
 * and row data for pre-V5 inserts survives (DROP COLUMN preserves other columns),
 * (2) {@code hibernate_sequence} does not exist and {@code DROP SEQUENCE IF EXISTS} did not
 * fail on a fresh container, (3) the index was renamed from {@code idx_ignore_user_user_id}
 * to {@code idx_ignore_user_lookup}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Stage3CleanupV005MigrationIT {

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
  void v005_cleans_up_updated_at_hibernate_sequence_and_renames_index() throws Exception {
    // --- 1. Migrate to V4 only (post-Stage 2 state) ---
    Flyway.configure()
        .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
        .locations("classpath:db/migration")
        .target("4")
        .load()
        .migrate();

    // --- 2. Sanity-check starting state + insert a dummy row ---
    //     Confirms updated_at exists before V5 and that the pre-V5 row will survive
    //     the DROP COLUMN operation (PostgreSQL preserves other columns).
    try (Connection conn =
            DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Statement stmt = conn.createStatement()) {

      try (ResultSet rs =
          stmt.executeQuery(
              "SELECT COUNT(*) FROM information_schema.columns "
                  + "WHERE table_name = 'message_log' AND column_name = 'updated_at'")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(1))
            .as("updated_at must exist before V5 (sanity check on post-V4 state)")
            .isEqualTo(1L);
      }

      stmt.executeUpdate(
          "INSERT INTO message_log (id, guild, channel, nick_name, effective_name, message, "
              + "is_converted, converted_message, channel_id) "
              + "VALUES (500, 'g', 'c', 'nk', 'ef', 'dkssud', true, '안녕', 555)");
    }

    // --- 3. Migrate to V5 ---
    Flyway.configure()
        .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
        .locations("classpath:db/migration")
        .target("5")
        .load()
        .migrate();

    // --- 4. Assert V5 effects ---
    try (Connection conn =
            DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Statement stmt = conn.createStatement()) {

      // 4a. updated_at column is gone
      try (ResultSet rs =
          stmt.executeQuery(
              "SELECT COUNT(*) FROM information_schema.columns "
                  + "WHERE table_name = 'message_log' AND column_name = 'updated_at'")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(1))
            .as("updated_at column must be dropped by V005")
            .isZero();
      }

      // 4b. Pre-V5 row (id=500) survives DROP COLUMN
      try (ResultSet rs =
          stmt.executeQuery("SELECT COUNT(*) FROM message_log WHERE id = 500")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(1))
            .as("pre-V5 inserted row must survive DROP COLUMN updated_at")
            .isEqualTo(1L);
      }

      // 4c. Index was renamed: old name gone, new name present
      try (ResultSet rs =
          stmt.executeQuery(
              "SELECT indexname FROM pg_indexes "
                  + "WHERE tablename = 'ignore_user' "
                  + "  AND indexname IN ('idx_ignore_user_user_id', 'idx_ignore_user_lookup')")) {
        assertThat(rs.next())
            .as("ignore_user must have exactly one of the old/new index names after V005")
            .isTrue();
        assertThat(rs.getString("indexname"))
            .as("V005 must rename idx_ignore_user_user_id to idx_ignore_user_lookup")
            .isEqualTo("idx_ignore_user_lookup");
        assertThat(rs.next())
            .as("only one of the two index names must exist — rename is not a duplicate")
            .isFalse();
      }

      // 4d. hibernate_sequence is absent (IF EXISTS handled missing sequence without error)
      try (ResultSet rs =
          stmt.executeQuery(
              "SELECT COUNT(*) FROM pg_sequences WHERE sequencename = 'hibernate_sequence'")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(1))
            .as(
                "hibernate_sequence must not exist on fresh env (IF EXISTS guards absence without error)")
            .isZero();
      }

      // 4e. New INSERT without updated_at column succeeds after V5
      stmt.executeUpdate(
          "INSERT INTO message_log (guild, channel, nick_name, effective_name, message, "
              + "is_converted, converted_message, channel_id) "
              + "VALUES ('g2', 'c2', 'nk2', 'ef2', 'hi', false, null, 666)");
      try (ResultSet rs =
          stmt.executeQuery("SELECT COUNT(*) FROM message_log WHERE channel_id = 666")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getLong(1))
            .as("new INSERT without updated_at must succeed after V005 DROP COLUMN")
            .isEqualTo(1L);
      }
    }
  }
}
```

Why `information_schema.columns` instead of trying to `SELECT updated_at`: querying a dropped column throws `PSQLException`; using the information schema produces a clean boolean-style assertion.

Why `pg_indexes` and `pg_sequences` (PG-specific, not `information_schema`): PostgreSQL exposes indexes and sequences in these system views, not in the ANSI `information_schema`. The test targets Postgres 17 only (same as the rest of the project) so this is acceptable.

Why id=500 in the pre-V5 insert: matches the spec §7.1 scenario. V003's IT uses id=100, V004's IT uses id=300, V005's IT uses id=500 — distinct ranges make cross-test debugging easier if someone runs multiple migration ITs against the same container by mistake.

- [ ] **Step 2: Run the new test**

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :adapter-persistence:test --tests "org.specter.converter.adapter.persistence.migration.Stage3CleanupV005MigrationIT"
```

Expected: BUILD SUCCESSFUL, 1 test passed.

Diagnosis if failure:
- `4a` fails with non-zero count → V005 did not drop `updated_at`. Check the SQL file.
- `4b` fails → DROP COLUMN accidentally truncated the table (unexpected in PG). Investigate V005.
- `4c` first `rs.next()` is false → no index with either name. V005 did not run or the rename silently skipped. Check V005.
- `4c` second assertion fails → both old and new index names coexist. `ALTER INDEX RENAME` did not replace; it added a copy. Investigate.
- `4d` fails → `hibernate_sequence` exists on a fresh env, which is impossible unless V5 accidentally created one.
- `4e` fails → `DROP COLUMN updated_at` left a phantom constraint. Investigate PG logs.

- [ ] **Step 3: Run the full adapter-persistence test suite**

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :adapter-persistence:test
```

Expected: BUILD SUCCESSFUL. All existing tests plus the new one pass.

- [ ] **Step 4: Apply Spotless formatting**

```bash
export JAVA_HOME=/home/kshull/.sdkman/candidates/java/25.0.2-amzn
export PATH=$JAVA_HOME/bin:$PATH
./gradlew spotlessApply
```

Check the git diff on the new file. Spotless may reformat line breaks in the long multi-line SQL string literals and `.as()` assertion chains (same behavior as Stage 1 and Stage 2 migration ITs).

```bash
git diff korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/migration/Stage3CleanupV005MigrationIT.java
```

Expected: whitespace-only changes. If logic differs, review before committing.

- [ ] **Step 5: Commit**

```bash
git add korConverter/hexagonal/adapter/adapter-persistence/src/test/java/org/specter/converter/adapter/persistence/migration/Stage3CleanupV005MigrationIT.java
git commit -m "$(cat <<'EOF'
test(adapter-persistence): add Stage3CleanupV005MigrationIT

Drives Flyway in two stages (target=4 then target=5) against a fresh
Testcontainer, inserts a pre-V5 row, then verifies V005's three effects:
(1) message_log.updated_at column is dropped while pre-V5 row survives,
(2) the ignore_user index is renamed from idx_ignore_user_user_id to
idx_ignore_user_lookup with no duplication, (3) hibernate_sequence does
not exist on the fresh container (DROP SEQUENCE IF EXISTS handled the
missing sequence without error). Also asserts that a post-V5 INSERT
without the removed updated_at column succeeds.

Uses information_schema.columns, pg_indexes, and pg_sequences metadata
queries because these Stage 3 changes have no row-level data
transformation to verify — only schema-shape effects.

Mirrors LegacyToTargetV003MigrationIT and MessageLogV004MigrationIT's
isolated-container + raw-JDBC pattern so each Vxxx gets its own focused
regression guard.

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

Expected: BUILD SUCCESSFUL. Runs Spotless check, Checkstyle, jOOQ codegen, ErrorProne/NullAway, all tests, JaCoCo report.

- [ ] **Step 2: Confirm the final schema**

Quick sanity on the generated code:

```bash
grep -n "UPDATED_AT\|LOOKUP\|USER_ID" \
  korConverter/hexagonal/adapter/adapter-persistence/build/generated/sources/jooq/main/org/specter/converter/adapter/persistence/generated/tables/MessageLog.java \
  korConverter/hexagonal/adapter/adapter-persistence/build/generated/sources/jooq/main/org/specter/converter/adapter/persistence/generated/Indexes.java \
  | head -20
```

Expected output (among other lines):
- `MessageLog.java`: NO `UPDATED_AT` line (Stage 3 dropped it)
- `Indexes.java`: Contains `IDX_IGNORE_USER_LOOKUP` constant or equivalent, NOT `IDX_IGNORE_USER_USER_ID`

If `UPDATED_AT` still appears in `MessageLog.java`, codegen ran against a cached state — run `./gradlew clean build` to force regeneration.

- [ ] **Step 3: No commit needed (verification only)**

If Spotless violations appear during the full build that Task 2 Step 4 did not catch:

```bash
./gradlew spotlessApply
git status
git add -A
git commit -m "style: apply spotless formatting after V005 migration"
```

Otherwise skip.

---

## Verification Summary

After all tasks:

1. `git log --oneline 681164c..HEAD` shows two new commits (feat V005, test V005), optionally a third Spotless follow-up.
2. `./gradlew build` passes cleanly.
3. `Stage3CleanupV005MigrationIT` green alongside `LegacyToTargetV003MigrationIT`, `MessageLogV004MigrationIT`.
4. Existing adapter/query/E2E tests unchanged and still passing.
5. Generated `MessageLog.java` no longer declares `UPDATED_AT`; generated `Indexes.java` declares `IDX_IGNORE_USER_LOOKUP`.
6. Only three changed/created artifacts: the V005 SQL, the V005 IT, and the automatic jOOQ codegen delta.

## Production Deployment (runbook, not implementation task)

From spec §8:
1. `pg_dump --table=message_log` backup (preserves `updated_at` data)
2. Record `hibernate_sequence` existence: `SELECT COUNT(*) FROM pg_sequences WHERE sequencename = 'hibernate_sequence'`
3. Record pre-migration indexes: `SELECT indexname FROM pg_indexes WHERE tablename = 'ignore_user'`
4. Stop bot container → start new image → Flyway auto-runs V005
5. Verify `flyway_schema_history` has a V5 success row
6. Verify column removal: `\d message_log` (updated_at absent)
7. Verify index rename: `\d ignore_user` (idx_ignore_user_lookup present)
8. Discord smoke test: ignore-user add/remove, message conversion

## Series Completion

Stage 3 is the final stage of the purist-DDD migration series. After this plan is executed:

- `ignore_user` schema matches the purist-DDD target exactly: `BIGINT IDENTITY PK`, `user_id`/`channel_id`/`name`, `version BIGINT DEFAULT 0`, `TIMESTAMPTZ` timestamps, `idx_ignore_user_lookup` index
- `message_log` schema matches the purist-DDD target exactly: `BIGINT IDENTITY PK`, `VARCHAR` business fields, `TEXT` message fields, nullable `channel_id`, `TIMESTAMPTZ created_at` only
- No further V-migrations are planned in this series
