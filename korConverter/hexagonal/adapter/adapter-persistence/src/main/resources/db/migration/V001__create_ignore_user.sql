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
