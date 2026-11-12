package notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEventListenerTest {
    private static final String CREATED_KEY = "booking.created";
    private static final String EVENT_ID = "0b1f6c1e-3d1a-4a52-9d57-0c7f4c0f2a11";
    private static final String CLIENT_EMAIL = "dana@example.com";
    private static final String EVENT_TEMPLATE = """
            {"bookingId":7,"clientEmail":%s,"clientFirstName":"Dana","roomName":"Mansion",
             "startAt":"2026-11-13T10:00:00","participants":4}""";

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private ProcessedEventStore processedEvents;

    private BookingEventListener listener() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        return new BookingEventListener(objectMapper, new EmailSender(mailSender, "noreply@test"), processedEvents);
    }

    private static String eventWithEmail(String email) {
        return EVENT_TEMPLATE.formatted(email);
    }

    private static String eventForDana() {
        return eventWithEmail("\"" + CLIENT_EMAIL + "\"");
    }

    @Test
    void createdEvent_sendsConfirmationWithBookingDetails() throws Exception {
        when(processedEvents.markProcessed(EVENT_ID)).thenReturn(true);

        listener().onBookingEvent(eventForDana(), CREATED_KEY, EVENT_ID);

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        assertEquals(CLIENT_EMAIL, sent.getValue().getTo()[0]);
        assertTrue(sent.getValue().getText().contains("Mansion"));
        assertTrue(sent.getValue().getText().contains("13/11/2026 10:00"));
    }

    @Test
    void alreadyProcessedEvent_sendsNothing() throws Exception {
        when(processedEvents.markProcessed(EVENT_ID)).thenReturn(false);

        listener().onBookingEvent(eventForDana(), CREATED_KEY, EVENT_ID);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void eventWithoutEmail_sendsNothing() throws Exception {
        listener().onBookingEvent(eventWithEmail("null"), CREATED_KEY, EVENT_ID);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void unknownRoutingKey_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> listener().onBookingEvent(eventForDana(), "booking.unknown", EVENT_ID));
    }
}
