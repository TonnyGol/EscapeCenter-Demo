-- Each room sets its own game times, so one room can open later or run with longer breaks.
-- The defaults are the hours every room used before this migration.
ALTER TABLE rooms
    ADD COLUMN first_game_start TIME NOT NULL DEFAULT '10:00:00',
    ADD COLUMN games_per_day    INT  NOT NULL DEFAULT 7,
    ADD COLUMN break_minutes    INT  NOT NULL DEFAULT 30;
