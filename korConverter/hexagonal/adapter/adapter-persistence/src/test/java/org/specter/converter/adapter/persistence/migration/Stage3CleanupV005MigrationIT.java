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
 * <p>Uses an isolated Postgres container (not shared with {@code AdapterTestBase}, {@code
 * LegacyToTargetV003MigrationIT}, or {@code MessageLogV004MigrationIT}) so the two-stage {@code
 * target("4")} then {@code target("5")} Flyway lifecycle does not interfere with the full-migrate
 * pattern used by adapter/port tests.
 *
 * <p>Raw JDBC (not jOOQ DSL) is used because the test asserts against metadata tables ({@code
 * information_schema.columns}, {@code pg_indexes}, {@code pg_sequences}) that have no jOOQ
 * generated mappings, and because asserting on the presence/absence of columns and indexes is
 * simpler with raw SQL.
 *
 * <p>Guards the three V005 changes: (1) {@code message_log.updated_at} is absent after V5 and row
 * data for pre-V5 inserts survives (DROP COLUMN preserves other columns), (2) {@code
 * hibernate_sequence} does not exist and {@code DROP SEQUENCE IF EXISTS} did not fail on a fresh
 * container, (3) the index was renamed from {@code idx_ignore_user_user_id} to {@code
 * idx_ignore_user_lookup}.
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
        assertThat(rs.getLong(1)).as("updated_at column must be dropped by V005").isZero();
      }

      // 4b. Pre-V5 row (id=500) survives DROP COLUMN
      try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM message_log WHERE id = 500")) {
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
