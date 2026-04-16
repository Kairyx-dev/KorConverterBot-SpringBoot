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
