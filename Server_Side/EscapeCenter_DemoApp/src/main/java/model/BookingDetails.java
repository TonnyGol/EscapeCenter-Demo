package model;

/** The part of a booking that staff can edit besides the client. Sent to the server as is. */
public record BookingDetails(int participants, String experience, String notes, String color) {
}
