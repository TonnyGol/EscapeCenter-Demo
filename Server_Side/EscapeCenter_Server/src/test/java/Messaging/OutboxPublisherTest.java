package Messaging;

import Entities.OutboxEvent;
import Repositories.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {
    private static final int RETENTION_DAYS = 7;
    private static final String BOOKING_CREATED_KEY = "booking.created";
    private static final String PAYLOAD = "{\"bookingId\":1}";

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(outboxEventRepository, rabbitTemplate, RETENTION_DAYS);
    }

    private void stubPending(OutboxEvent... events) {
        when(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAt(any(Pageable.class))).thenReturn(List.of(events));
    }

    @Test
    void publishPendingEvents_sendsEventAndMarksItPublished() {
        OutboxEvent event = new OutboxEvent(OutboxEvent.BOOKING_CREATED, PAYLOAD);
        stubPending(event);

        publisher.publishPendingEvents();

        verify(rabbitTemplate).convertAndSend(eq(MessagingConfig.BOOKINGS_EXCHANGE), eq(BOOKING_CREATED_KEY),
                eq((Object) PAYLOAD), any(MessagePostProcessor.class));
        assertNotNull(event.getPublishedAt());
    }

    @Test
    void publishPendingEvents_brokerDown_leavesEventsUnpublishedAndStops() {
        OutboxEvent first = new OutboxEvent(OutboxEvent.BOOKING_CREATED, PAYLOAD);
        OutboxEvent second = new OutboxEvent(OutboxEvent.BOOKING_CREATED, PAYLOAD);
        stubPending(first, second);
        doThrow(new AmqpConnectException(new IOException("down"))).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));

        publisher.publishPendingEvents();

        assertNull(first.getPublishedAt());
        assertNull(second.getPublishedAt());
    }

    @Test
    void publishPendingEvents_unknownType_isSkippedWithoutBlockingTheRest() {
        OutboxEvent unknown = new OutboxEvent("SomethingElse", PAYLOAD);
        OutboxEvent known = new OutboxEvent(OutboxEvent.BOOKING_CREATED, PAYLOAD);
        stubPending(unknown, known);

        publisher.publishPendingEvents();

        assertNull(unknown.getPublishedAt());
        assertNotNull(known.getPublishedAt());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
    }

    @Test
    void deleteOldPublishedEvents_deletesOnlyBeyondRetention() {
        publisher.deleteOldPublishedEvents();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(outboxEventRepository).deleteByPublishedAtBefore(cutoff.capture());
        assertTrue(cutoff.getValue().isBefore(LocalDateTime.now().minusDays(RETENTION_DAYS).plusMinutes(1)));
        assertTrue(cutoff.getValue().isAfter(LocalDateTime.now().minusDays(RETENTION_DAYS).minusMinutes(1)));
    }
}
