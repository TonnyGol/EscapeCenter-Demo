package model;

/** Body of PUT /api/bookings/{id}. Room and time cannot change: moving a booking is delete plus a new booking. */
public record UpdateBookingRequest(Client client, BookingDetails booking) {
}
