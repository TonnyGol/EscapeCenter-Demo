package Messaging;

import Entities.OutboxEvent;
import Repositories.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final Map<String, String> ROUTING_KEYS = Map.of(OutboxEvent.BOOKING_CREATED, "booking.created");
    private static final int BATCH_SIZE = 50;
    private static final long CONFIRM_TIMEOUT_MILLIS = 5000;
    private static final String BROKER_DOWN_MESSAGE = "RabbitMQ unreachable, will retry: {}";
    private static final String UNKNOWN_TYPE_MESSAGE = "Outbox event {} has unknown type {} and stays unpublished";

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final int retentionDays;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate,
                           @Value("${app.outbox.retention-days}") int retentionDays) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.retentionDays = retentionDays;
    }

    // The rows stay locked until the transaction ends, so only one instance sends a given row.
    // A crash after a send but before the commit sends that row again: delivery is at-least-once.
    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms}")
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEvent event : outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAt(PageRequest.ofSize(BATCH_SIZE))) {
            String routingKey = ROUTING_KEYS.get(event.getType());
            if (routingKey == null) {
                log.error(UNKNOWN_TYPE_MESSAGE, event.getId(), event.getType());
                continue;
            }
            try {
                sendAndWaitForConfirm(event, routingKey);
            } catch (AmqpException e) {
                log.warn(BROKER_DOWN_MESSAGE, e.getMessage());
                return;
            }
            event.markPublished();
        }
    }

    // Without the confirm, a broker that crashes right after accepting a message loses it while the row says "published".
    private void sendAndWaitForConfirm(OutboxEvent event, String routingKey) {
        rabbitTemplate.invoke(channel -> {
            // The event id travels as the message id so the notification service can spot duplicates.
            channel.convertAndSend(MessagingConfig.BOOKINGS_EXCHANGE, routingKey, event.getPayload(), message -> {
                message.getMessageProperties().setMessageId(event.getId());
                return message;
            });
            channel.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MILLIS);
            return null;
        });
    }

    // The payload holds the client's email, so it is not kept longer than needed.
    @Scheduled(cron = "${app.outbox.purge-cron}")
    @Transactional
    public void deleteOldPublishedEvents() {
        outboxEventRepository.deleteByPublishedAtBefore(LocalDateTime.now().minusDays(retentionDays));
    }
}
