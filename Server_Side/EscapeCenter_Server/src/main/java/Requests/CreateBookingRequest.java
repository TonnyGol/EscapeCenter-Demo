package Requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// "Not in the past" is checked in BookingService, because only customers are held to it, not staff.
public record CreateBookingRequest(
        @NotNull Long roomId,
        @NotNull LocalDateTime startAt,
        @NotNull @Valid ClientDetails client,
        @NotNull @Valid BookingDetails booking) {
}
