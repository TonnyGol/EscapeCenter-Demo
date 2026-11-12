-- Owned by this service. IF NOT EXISTS because Spring runs this file on every start (spring.sql.init.mode=always).
-- id is the outbox event's UUID, which the API sends as the RabbitMQ message id.
CREATE TABLE IF NOT EXISTS processed_events (
    id           CHAR(36) NOT NULL,
    processed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4;
