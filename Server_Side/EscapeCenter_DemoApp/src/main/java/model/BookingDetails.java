package model;

/**
 * The part of a booking that staff can edit besides the client. Sent to the server as is.
 * {@code notes} is the client's own text: the server reads it only when the booking is created.
 */
public record BookingDetails(int participants, String experience, String notes, String color,
                             BookingType type, String internalNotes) {
}
