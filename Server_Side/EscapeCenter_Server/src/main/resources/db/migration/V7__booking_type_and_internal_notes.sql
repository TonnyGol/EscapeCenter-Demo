-- booking_type holds an Entities.BookingType name. A new type needs no migration, only a new enum constant.
ALTER TABLE bookings
    ADD COLUMN booking_type   VARCHAR(30)   NOT NULL DEFAULT 'CLIENT',
    ADD COLUMN internal_notes VARCHAR(1000);
