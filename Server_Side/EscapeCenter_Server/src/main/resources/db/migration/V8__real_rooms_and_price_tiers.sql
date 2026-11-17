-- The venue's real rooms and prices: a description and a minimum group size per room, seven rooms, and a price per
-- participant that depends on the group size.
-- Games start at 11:30 with a 15 minute break (assumed; the venue publishes opening hours, not breaks).
-- games_per_day keeps the last game inside the same calendar day.
ALTER TABLE rooms
    ADD COLUMN description      VARCHAR(300) NOT NULL DEFAULT '',
    ADD COLUMN min_participants INT          NOT NULL DEFAULT 2;

UPDATE rooms SET capacity = 6, min_participants = 2, duration_minutes = 60, games_per_day = 10, description = 'פריצה לבית השכן העשיר שהופכת לבעיה שמאיימת על חייכם' WHERE name = 'אחוזת השכן';
UPDATE rooms SET capacity = 7, min_participants = 3, duration_minutes = 60, games_per_day = 10, description = 'מצאו את המקדש העתיק ביותר ביפן ומנעו את סוף העולם' WHERE name = 'מקדש הקאמי';
UPDATE rooms SET capacity = 12, min_participants = 3, duration_minutes = 75, games_per_day = 8, description = 'ערב רגוע עם החבר''ה שהופך להרפתקה מסחררת' WHERE name = 'ההתערבות';
UPDATE rooms SET capacity = 7, min_participants = 3, duration_minutes = 70, games_per_day = 9, description = 'פריצה למערת הקפה בג''מייקה אל המשאב היקר בעולם' WHERE name = 'אינפיניטי';
UPDATE rooms SET capacity = 9, min_participants = 3, duration_minutes = 75, games_per_day = 8, description = 'דירת המסתור של פבלו אסקובר התגלתה' WHERE name = 'נרקוס';
UPDATE rooms SET first_game_start = '11:30:00', break_minutes = 15;

INSERT INTO rooms (name, capacity, min_participants, duration_minutes, first_game_start, games_per_day, break_minutes, description) VALUES
    ('משחק מכור', 8, 3, 70, '11:30:00', 9, 15, 'הודעה ממספר לא מזוהה, אזור תעשייה נטוש ומלכודת'),
    ('דו-קרב צוללות', 20, 4, 60, '11:30:00', 10, 15, 'חדר כפול: שני צוותים, ורק אחד מנצח');

ALTER TABLE rooms
    ADD CONSTRAINT chk_rooms_min_participants CHECK (min_participants >= 1 AND min_participants <= capacity);

-- Price per participant by group size: the row with the largest from_participants not above the group applies.
-- discounted_price is for soldiers, students and national service, shown with an ID.
CREATE TABLE room_prices (
    room_id               BIGINT NOT NULL,
    from_participants     INT    NOT NULL,
    price_per_participant INT    NOT NULL,
    discounted_price      INT    NOT NULL,
    PRIMARY KEY (room_id, from_participants),
    CONSTRAINT fk_room_prices_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4;

INSERT INTO room_prices (room_id, from_participants, price_per_participant, discounted_price)
SELECT r.id, t.from_participants, t.price, t.discounted
FROM rooms r
JOIN (
    SELECT 'אחוזת השכן' AS name, 2 AS from_participants, 170 AS price, 170 AS discounted UNION ALL
    SELECT 'אחוזת השכן', 3, 150, 140 UNION ALL SELECT 'אחוזת השכן', 4, 140, 130 UNION ALL
    SELECT 'אחוזת השכן', 5, 130, 120 UNION ALL SELECT 'אחוזת השכן', 6, 120, 110 UNION ALL
    SELECT 'מקדש הקאמי', 2, 170, 170 UNION ALL SELECT 'מקדש הקאמי', 3, 150, 140 UNION ALL
    SELECT 'מקדש הקאמי', 4, 140, 130 UNION ALL SELECT 'מקדש הקאמי', 5, 130, 120 UNION ALL SELECT 'מקדש הקאמי', 6, 120, 110 UNION ALL
    SELECT 'אינפיניטי', 2, 170, 170 UNION ALL SELECT 'אינפיניטי', 3, 150, 140 UNION ALL
    SELECT 'אינפיניטי', 4, 140, 130 UNION ALL SELECT 'אינפיניטי', 5, 130, 120 UNION ALL SELECT 'אינפיניטי', 6, 120, 110 UNION ALL
    SELECT 'ההתערבות', 3, 170, 170 UNION ALL SELECT 'ההתערבות', 4, 140, 130 UNION ALL
    SELECT 'ההתערבות', 5, 130, 120 UNION ALL SELECT 'ההתערבות', 6, 120, 110 UNION ALL
    SELECT 'נרקוס', 3, 170, 170 UNION ALL SELECT 'נרקוס', 4, 140, 130 UNION ALL
    SELECT 'נרקוס', 5, 130, 120 UNION ALL SELECT 'נרקוס', 6, 120, 110 UNION ALL
    SELECT 'משחק מכור', 3, 180, 180 UNION ALL SELECT 'משחק מכור', 4, 150, 140 UNION ALL
    SELECT 'משחק מכור', 5, 140, 130 UNION ALL SELECT 'משחק מכור', 6, 130, 120 UNION ALL SELECT 'משחק מכור', 7, 120, 110 UNION ALL
    SELECT 'דו-קרב צוללות', 4, 140, 130 UNION ALL SELECT 'דו-קרב צוללות', 5, 130, 120 UNION ALL SELECT 'דו-קרב צוללות', 6, 120, 110
) t ON t.name = r.name;
