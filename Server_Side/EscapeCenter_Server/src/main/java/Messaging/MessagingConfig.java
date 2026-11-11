package Messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class MessagingConfig {
    public static final String BOOKINGS_EXCHANGE = "bookings";
    public static final String NOTIFICATIONS_QUEUE = "bookings.notifications";
    private static final String ALL_BOOKING_EVENTS_KEY = "booking.*";

    @Bean
    TopicExchange bookingsExchange() {
        return new TopicExchange(BOOKINGS_EXCHANGE);
    }

    // Declared here too, not only by the notification service: an exchange with no queue drops messages,
    // so events published before that service first starts would be lost for good.
    @Bean
    Queue notificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    Binding notificationsBinding(Queue notificationsQueue, TopicExchange bookingsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(bookingsExchange).with(ALL_BOOKING_EVENTS_KEY);
    }
}
