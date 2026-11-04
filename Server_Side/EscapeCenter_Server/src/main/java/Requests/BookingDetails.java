package Requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// The upper limit for participants is the room's capacity, which only the service knows.
public record BookingDetails(
        @Min(BookingDetails.MIN_PARTICIPANTS) int participants,
        @Size(max = BookingDetails.MAX_EXPERIENCE_LENGTH) String experience,
        @Size(max = BookingDetails.MAX_NOTES_LENGTH) String notes,
        @Size(max = BookingDetails.MAX_COLOR_LENGTH) String color) {

    public static final int MIN_PARTICIPANTS = 1;
    public static final int MAX_EXPERIENCE_LENGTH = 50;
    public static final int MAX_NOTES_LENGTH = 1000;
    public static final int MAX_COLOR_LENGTH = 20;
}
