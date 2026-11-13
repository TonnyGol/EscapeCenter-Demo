package Requests;

import Entities.BookingType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// The upper limit for participants is the room's capacity, which only the service knows.
// notes is the client's own text and is only read when the booking is created; type and internalNotes are staff only.
public record BookingDetails(
        @Min(BookingDetails.MIN_PARTICIPANTS) int participants,
        @Size(max = BookingDetails.MAX_EXPERIENCE_LENGTH) String experience,
        @Size(max = BookingDetails.MAX_NOTES_LENGTH) String notes,
        @Size(max = BookingDetails.MAX_COLOR_LENGTH) String color,
        BookingType type,
        @Size(max = BookingDetails.MAX_NOTES_LENGTH) String internalNotes) {

    public static final int MIN_PARTICIPANTS = 1;
    public static final int MAX_EXPERIENCE_LENGTH = 50;
    public static final int MAX_NOTES_LENGTH = 1000;
    public static final int MAX_COLOR_LENGTH = 20;
}
