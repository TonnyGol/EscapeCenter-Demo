package notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Pins the JSON keys the API writes (see the payload test in the API's BookingServiceTest): a renamed key would
// otherwise arrive as null and every booking would look like "no email", with no error anywhere.
class BookingEventTest {
    private static final String API_PAYLOAD = """
            {"bookingId":7,"clientEmail":"dana@example.com","clientFirstName":"Dana","roomName":"Mansion",
             "startAt":"2026-11-13T10:00:00","participants":4}""";

    @Test
    void readsEveryFieldTheApiWrites() throws Exception {
        BookingEvent event = new ObjectMapper().findAndRegisterModules().readValue(API_PAYLOAD, BookingEvent.class);

        assertEquals(7L, event.bookingId());
        assertNotNull(event.clientEmail());
        assertNotNull(event.clientFirstName());
        assertNotNull(event.roomName());
        assertNotNull(event.startAt());
        assertEquals(4, event.participants());
    }
}
