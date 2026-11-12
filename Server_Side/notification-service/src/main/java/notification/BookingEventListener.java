package notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
class BookingEventListener {
    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);
    private static final String BOOKINGS_EXCHANGE = "bookings";
    private static final String NOTIFICATIONS_QUEUE = "bookings.notifications";
    private static final String ALL_BOOKING_EVENTS_KEY = "booking.*";
    private static final DateTimeFormatter START_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String NO_EMAIL_MESSAGE = "Booking {} has no client email, nothing to send";
    private static final String DUPLICATE_MESSAGE = "Event {} was already handled, skipping";

    private final ObjectMapper objectMapper;
    private final EmailSender emailSender;
    private final ProcessedEventStore processedEvents;

    BookingEventListener(ObjectMapper objectMapper, EmailSender emailSender, ProcessedEventStore processedEvents) {
        this.objectMapper = objectMapper;
        this.emailSender = emailSender;
        this.processedEvents = processedEvents;
    }

    // Declares the same exchange, queue and binding as the API, so either service can start first.
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = NOTIFICATIONS_QUEUE, durable = "true"),
            exchange = @Exchange(name = BOOKINGS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = ALL_BOOKING_EVENTS_KEY))
    // One transaction: if the email fails, the "processed" mark is rolled back too, so the retry sends it again.
    @Transactional
    void onBookingEvent(String body, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
                        @Header(AmqpHeaders.MESSAGE_ID) String eventId) throws JsonProcessingException {
        BookingEvent event = objectMapper.readValue(body, BookingEvent.class);
        if (event.clientEmail() == null || event.clientEmail().isBlank()) {
            log.info(NO_EMAIL_MESSAGE, event.bookingId());
            return;
        }
        EmailTemplate template = EmailTemplate.forRoutingKey(routingKey);
        if (!processedEvents.markProcessed(eventId)) {
            log.info(DUPLICATE_MESSAGE, eventId);
            return;
        }
        emailSender.send(event.clientEmail(), template, templateValues(event));
    }

    private static Map<String, String> templateValues(BookingEvent event) {
        return Map.of(
                "firstName", event.clientFirstName(),
                "roomName", event.roomName(),
                "startAt", event.startAt().format(START_TIME_FORMAT),
                "participants", String.valueOf(event.participants()),
                "bookingId", String.valueOf(event.bookingId()));
    }
}
