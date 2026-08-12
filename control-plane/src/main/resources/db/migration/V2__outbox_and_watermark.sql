-- Week 2: outbox publish attempts + event sequence watermarks

ALTER TABLE outbox ADD COLUMN publish_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE outbox ADD COLUMN last_error VARCHAR(500);

CREATE TABLE event_sequence_watermark (
    source          VARCHAR(64) NOT NULL,
    robot_id        VARCHAR(64) NOT NULL,
    last_sequence   BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (source, robot_id)
);
