package notification;

import java.util.Arrays;

// One email per event type: the routing key says which event arrived.
enum EmailTemplate {
    BOOKING_CREATED("booking.created", "Your booking is confirmed", "email/booking-created.txt"),
    BOOKING_CANCELLED("booking.cancelled", "Your booking was cancelled", "email/booking-cancelled.txt");

    private final String routingKey;
    private final String subject;
    private final String resourcePath;

    EmailTemplate(String routingKey, String subject, String resourcePath) {
        this.routingKey = routingKey;
        this.subject = subject;
        this.resourcePath = resourcePath;
    }

    String subject() {
        return subject;
    }

    String resourcePath() {
        return resourcePath;
    }

    static EmailTemplate forRoutingKey(String routingKey) {
        return Arrays.stream(values())
                .filter(template -> template.routingKey.equals(routingKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No email template for routing key " + routingKey));
    }
}
