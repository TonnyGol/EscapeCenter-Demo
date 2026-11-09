package Responses;

import Entities.Booking;
import Entities.BookingStatus;

import java.time.LocalDateTime;

public record StaffBookingResponse(Long id, Long roomId, String roomName, LocalDateTime startAt, LocalDateTime endAt,
                                   ClientResponse client, int participants, String experience, String notes,
                                   String color, BookingStatus status) {

    public static StaffBookingResponse from(Booking booking) {
        return new StaffBookingResponse(booking.getId(), booking.getRoom().getId(), booking.getRoom().getName(),
                booking.getStartAt(), booking.getEndAt(), ClientResponse.from(booking.getClient()),
                booking.getParticipants(), booking.getExperience(), booking.getNotes(), booking.getColor(),
                booking.getStatus());
    }
}
