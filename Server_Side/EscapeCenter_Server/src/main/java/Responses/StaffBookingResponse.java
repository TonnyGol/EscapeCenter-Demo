package Responses;

import Entities.Booking;
import Entities.BookingStatus;
import Entities.BookingType;

import java.time.LocalDateTime;

public record StaffBookingResponse(Long id, Long roomId, String roomName, LocalDateTime startAt, LocalDateTime endAt,
                                   ClientResponse client, int participants, String experience, String notes,
                                   String color, BookingStatus status, BookingType type, String internalNotes,
                                   LocalDateTime createdAt) {

    public static StaffBookingResponse from(Booking booking) {
        return new StaffBookingResponse(booking.getId(), booking.getRoom().getId(), booking.getRoom().getName(),
                booking.getStartAt(), booking.getEndAt(), ClientResponse.from(booking.getClient()),
                booking.getParticipants(), booking.getExperience(), booking.getNotes(), booking.getColor(),
                booking.getStatus(), booking.getType(), booking.getInternalNotes(), booking.getCreatedAt());
    }
}
