package Requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

// Room and time are not editable: moving a booking is a delete plus a new booking, which re-checks availability.
public record UpdateBookingRequest(
        @NotNull @Valid ClientDetails client,
        @NotNull @Valid BookingDetails booking) {
}
