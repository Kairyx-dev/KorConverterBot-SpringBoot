CREATE TABLE message_log (
    id                BIGSERIAL    PRIMARY KEY,
    guild             VARCHAR(255),
    channel           VARCHAR(255),
    nick_name         VARCHAR(255),
    effective_name    VARCHAR(255),
    message           TEXT,
    is_converted      BOOLEAN      NOT NULL DEFAULT false,
    converted_message TEXT,
    channel_id        BIGINT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
