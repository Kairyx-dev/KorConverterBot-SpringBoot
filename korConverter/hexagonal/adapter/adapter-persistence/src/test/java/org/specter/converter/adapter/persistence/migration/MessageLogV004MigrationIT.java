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
 * <p>Uses an isolated Postgres container (not shared with {@code AdapterTestBase} or {@code
 * LegacyToTargetV003MigrationIT}) so the two-stage {@code target("3")} then {@code target("4")}
 * Flyway lifecycle does not interfere with the full-migrate pattern used by adapter/port tests.
 *
 * <p>Raw JDBC (not jOOQ DSL) is used for pre-V4 writes because jOOQ generated types reflect the
 * final post-V4 schema only.
 *
 * <p>Guards the four V004 changes: (1) {@code message}/{@code converted_message} accept strings
 * longer than 255 chars (TEXT widening), (2) {@code channel_id} no longer gets 0 when unspecified
 * (DEFAULT drop), (3) existing {@code created_at}/{@code updated_at} values are interpreted as KST
 * wall-clock and converted to correct UTC instants, (4) new INSERTs after V4 still succeed
 * (regression check on unrelated columns).
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
          stmt.executeQuery("SELECT created_at, updated_at FROM message_log WHERE id = 300")) {
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
            .as(
                "new message_log INSERT with 2000-char message must succeed after V004 TEXT widening")
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
