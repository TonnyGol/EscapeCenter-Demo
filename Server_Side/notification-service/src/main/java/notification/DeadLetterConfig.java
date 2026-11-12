package notification;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DeadLetterConfig {
    static final String DEAD_LETTER_QUEUE = "bookings.dlq";
    private static final String DEFAULT_EXCHANGE = "";

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    // Spring Boot uses this bean once the retries are used up. The copy in the dead-letter queue carries
    // the error in its headers (x-exception-message). Republishing needs no change to the existing queue's arguments.
    @Bean
    MessageRecoverer republishToDeadLetterQueue(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DEFAULT_EXCHANGE, DEAD_LETTER_QUEUE);
    }
}
