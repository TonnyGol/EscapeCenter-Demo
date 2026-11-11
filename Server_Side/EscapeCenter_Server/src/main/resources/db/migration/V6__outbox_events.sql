-- Transactional outbox: a booking and its event are saved in one transaction,
-- and a scheduled publisher sends rows with published_at NULL to RabbitMQ.
-- id is a UUID string (CHAR(36)) so the consumer can use it to skip duplicate deliveries.
CREATE TABLE outbox_events (
    id           CHAR(36)    NOT NULL,
    type         VARCHAR(50) NOT NULL,
    payload      JSON        NOT NULL,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME,
    PRIMARY KEY (id),
    INDEX idx_outbox_unpublished (published_at, created_at)
) DEFAULT CHARSET = utf8mb4;
