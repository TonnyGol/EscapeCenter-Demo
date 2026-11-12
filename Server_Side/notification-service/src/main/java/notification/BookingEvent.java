package notification;

import java.time.LocalDateTime;

// Same fields as the BookingCreatedEvent record in the API's BookingService: the API writes it, this service reads it.
public record BookingEvent(Long bookingId, String clientEmail, String clientFirstName,
                           String roomName, LocalDateTime startAt, int participants) {
}
