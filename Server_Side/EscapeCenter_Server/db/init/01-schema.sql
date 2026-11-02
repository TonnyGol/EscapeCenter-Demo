CREATE TABLE IF NOT EXISTS clients (
    firstName   VARCHAR(50)  NOT NULL,
    lastName    VARCHAR(50)  NOT NULL,
    phoneNumber VARCHAR(20),
    email       VARCHAR(100),
    PRIMARY KEY (firstName, lastName)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS bookings (
    BookingID    VARCHAR(50) NOT NULL,
    room         VARCHAR(50) NOT NULL,
    firstName    VARCHAR(50),
    lastName     VARCHAR(50),
    phoneNumber  VARCHAR(20),
    email        VARCHAR(100),
    experience   VARCHAR(50),
    notes        TEXT,
    participants INT,
    color        VARCHAR(20),
    PRIMARY KEY (BookingID, room)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS workers (
    username VARCHAR(50)  NOT NULL PRIMARY KEY,
    password VARCHAR(100) NOT NULL
) DEFAULT CHARSET = utf8mb4;
