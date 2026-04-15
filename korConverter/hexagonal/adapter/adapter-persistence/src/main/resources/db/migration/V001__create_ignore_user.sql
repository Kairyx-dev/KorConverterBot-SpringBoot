CREATE TABLE ignore_user (
    id          BIGINT                         NOT NULL PRIMARY KEY,
    user_id     BIGINT                         NOT NULL,
    channel_id  BIGINT                         NOT NULL,
    name        VARCHAR(255),
    created_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX idx_ignore_user_user_id ON ignore_user (user_id, channel_id);
