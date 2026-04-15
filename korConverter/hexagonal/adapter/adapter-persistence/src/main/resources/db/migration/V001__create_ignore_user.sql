-- Legacy baseline: replicates the pre-purist-DDD schema created by the old
-- JPA adapter. No DEFAULT on timestamps is intentional (JPA filled them via
-- @CreationTimestamp/@UpdateTimestamp); V003 adds DEFAULT now() when
-- converting to TIMESTAMPTZ.
CREATE TABLE ignore_user (
    id          BIGINT                         NOT NULL PRIMARY KEY,
    user_id     BIGINT                         NOT NULL,
    channel_id  BIGINT                         NOT NULL,
    name        VARCHAR(255),
    created_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX idx_ignore_user_user_id ON ignore_user (user_id, channel_id);
