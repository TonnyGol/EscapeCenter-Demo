CREATE TABLE workers (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_workers_username UNIQUE (username)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE clients (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(50)  NOT NULL,
    last_name  VARCHAR(50)  NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    email      VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT uq_clients_phone UNIQUE (phone)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE rooms (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    name             VARCHAR(50) NOT NULL,
    capacity         INT         NOT NULL,
    duration_minutes INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_rooms_name UNIQUE (name)
) DEFAULT CHARSET = utf8mb4;

-- start_at and end_at are DATETIME, not TIMESTAMP: they hold the venue's local time,
-- so MySQL must not convert them to UTC, and DATETIME has no 2038 limit.
CREATE TABLE bookings (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    client_id    BIGINT        NOT NULL,
    room_id      BIGINT        NOT NULL,
    start_at     DATETIME      NOT NULL,
    end_at       DATETIME      NOT NULL,
    participants INT           NOT NULL,
    experience   VARCHAR(50),
    color        VARCHAR(20),
    notes        VARCHAR(1000),
    status       VARCHAR(20)   NOT NULL DEFAULT 'CONFIRMED',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_bookings_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_bookings_room   FOREIGN KEY (room_id)   REFERENCES rooms (id),
    -- The database's final word on double booking: one game per room per start time.
    CONSTRAINT uq_bookings_room_start UNIQUE (room_id, start_at)
) DEFAULT CHARSET = utf8mb4;
