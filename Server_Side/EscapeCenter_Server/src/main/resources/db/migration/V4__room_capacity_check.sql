-- The booking form offers 2 to capacity participants, so a room that holds fewer than 2 can never be booked there.
ALTER TABLE rooms
    ADD CONSTRAINT chk_rooms_capacity CHECK (capacity >= 2);
