CREATE TABLE message_log (
    id                BIGINT                         NOT NULL PRIMARY KEY,
    guild             VARCHAR(255),
    channel           VARCHAR(255),
    nick_name         VARCHAR(255),
    effective_name    VARCHAR(255),
    message           VARCHAR(255),
    is_converted      BOOLEAN                        NOT NULL DEFAULT false,
    converted_message VARCHAR(255),
    channel_id        BIGINT                         DEFAULT 0,
    created_at        TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now()
);
