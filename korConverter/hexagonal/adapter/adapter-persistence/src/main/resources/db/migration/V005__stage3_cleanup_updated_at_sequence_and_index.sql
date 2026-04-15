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
