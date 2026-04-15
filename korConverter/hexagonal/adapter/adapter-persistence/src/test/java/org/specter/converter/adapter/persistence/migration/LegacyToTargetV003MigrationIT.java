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
 * Regression test for Flyway V003 forward migration against a simulated legacy (post-V2) schema.
 *
 * <p>Uses an isolated Postgres container (not shared with {@code AdapterTestBase}) so the Flyway
 * {@code target("2")} then {@code target("3")} two-stage lifecycle does not interfere with the
 * full-migrate pattern used by adapter/port tests.
 *
 * <p>Raw JDBC (not jOOQ DSL) is used for pre-V3 writes because jOOQ generated types reflect the
 * final post-V3 schema only.
 *
 * <p>Guards three risky aspects of V003: (1) the {@code --[jooq ignore start/stop]} directives
 * still let Flyway execute the PG-specific {@code USING ... AT TIME ZONE} clause, (2) the KST
 * interpretation (2025-12-31 23:00 KST → 14:00 UTC) is arithmetically correct, (3) both tables'
 * {@code id} columns gain auto-generation so adapter INSERTs without {@code id} work.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LegacyToTargetV003MigrationIT {

  // enables non-static @BeforeAll/@AfterAll tied to the class-scoped container lifecycle
  private static final PostgreSQLContainer<?> PG =
      new PostgreSQLContainer<>("postgres:17-alpine");

  @BeforeAll
  void startContainer() {
    PG.start();
  }

  @AfterAll
  void stopContainer() {
    PG.stop();
  }

  @Test
  void v003_migrates_legacy_schema_to_target_version_timestamptz_and_identity() throws Exception {
    // --- 1. Migrate to V2 only ---
    Flyway.configure()
        .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
        .locations("classpath:db/migration")
        .target("2")
        .load()
        .migrate();

    // --- 2. Insert legacy-shaped rows via raw JDBC ---
    // ignore_user: KST wall-clock 2025-12-31 23:00:00 -> expected UTC 14:00:00
    // message_log: just check auto-id after V3
    try (Connection conn = DriverManager.getConnection(
            PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(
          "INSERT INTO ignore_user (id, user_id, channel_id, name, created_at, updated_at) "
              + "VALUES (100, 111, 222, 'legacy-user', "
              + "TIMESTAMP '2025-12-31 23:00:00', TIMESTAMP '2025-12-31 23:00:00')");
      stmt.executeUpdate(
          "INSERT INTO message_log (id, guild, channel, nick_name, effective_name, message, "
              + "is_converted, converted_message, channel_id) "
              + "VALUES (200, 'g', 'c', 'nk', 'ef', 'dkssud', true, '안녕', 333)");
    }

    // --- 3. Migrate to V3 ---
    Flyway.configure()
        .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
        .locations("classpath:db/migration")
        .target("3")
        .load()
        .migrate();

    // --- 4. Assert migration effects ---
    try (Connection conn = DriverManager.getConnection(
            PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Statement stmt = conn.createStatement()) {

      // 4a. ignore_user: version column is 0, timestamps converted to UTC
      try (ResultSet rs = stmt.executeQuery(
          "SELECT version, created_at, updated_at FROM ignore_user WHERE id = 100")) {
        assertThat(rs.next())
            .as("ignore_user row id=100 must survive V3 migration")
            .isTrue();
        assertThat(rs.getLong("version")).isZero();
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
        // 2025-12-31 23:00:00 KST == 2025-12-31 14:00:00 UTC
        assertThat(createdAt).isEqualTo(Instant.parse("2025-12-31T14:00:00Z"));
        assertThat(updatedAt).isEqualTo(Instant.parse("2025-12-31T14:00:00Z"));
      }

      // 4b. ignore_user: auto-id works on new INSERT without id
      stmt.executeUpdate(
          "INSERT INTO ignore_user (user_id, channel_id, name, created_at, updated_at) "
              + "VALUES (999, 888, 'auto', now(), now())");
      try (ResultSet rs = stmt.executeQuery(
          "SELECT id FROM ignore_user WHERE user_id = 999")) {
        assertThat(rs.next())
            .as("new ignore_user INSERT without id must succeed after V003 IDENTITY")
            .isTrue();
        long autoId = rs.getLong("id");
        assertThat(autoId).isGreaterThan(100L);
      }

      // 4c. message_log: auto-id works on new INSERT without id
      stmt.executeUpdate(
          "INSERT INTO message_log (guild, channel, nick_name, effective_name, message, "
              + "is_converted, converted_message, channel_id) "
              + "VALUES ('g2', 'c2', 'nk2', 'ef2', 'hi', false, null, 444)");
      try (ResultSet rs = stmt.executeQuery(
          "SELECT id FROM message_log WHERE channel_id = 444")) {
        assertThat(rs.next())
            .as("new message_log INSERT without id must succeed after V003 IDENTITY")
            .isTrue();
        long autoId = rs.getLong("id");
        assertThat(autoId).isGreaterThan(200L);
      }
    }
  }
}
