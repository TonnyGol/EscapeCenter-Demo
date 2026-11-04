package model;

/** Body of POST /api/bookings. {@code startAt} is an ISO local date-time such as 2026-11-05T10:00. */
public record CreateBookingRequest(Long roomId, String startAt, Client client, BookingDetails booking) {
}
