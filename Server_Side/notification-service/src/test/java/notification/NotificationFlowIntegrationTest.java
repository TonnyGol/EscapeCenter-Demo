package notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * The real flow with throwaway RabbitMQ, MySQL and Mailpit: an event goes onto the "bookings" exchange the way
 * the API's OutboxPublisher sends it, and the email shows up in Mailpit. Like the API's tests, the containers are
 * never stopped by hand, Testcontainers removes them at exit.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.retry.initial-interval=100ms")
class NotificationFlowIntegrationTest {
    private static final int MAILPIT_SMTP_PORT = 1025;
    private static final int MAILPIT_HTTP_PORT = 8025;
    private static final String MESSAGES_PATH = "/api/v1/messages";
    private static final String SEARCH_PATH = "/api/v1/search?query=";
    private static final Duration WAIT_LIMIT = Duration.ofSeconds(30);
    private static final String BOOKINGS_EXCHANGE = "bookings";
    private static final String EVENT_TEMPLATE = """
            {"bookingId":%d,"clientEmail":"%s","clientFirstName":"Dana","roomName":"Mansion",
             "startAt":"2026-11-13T10:00:00","participants":4}""";

    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3-management");
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");
    private static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit")
            .withExposedPorts(MAILPIT_SMTP_PORT, MAILPIT_HTTP_PORT)
            .waitingFor(Wait.forHttp(MESSAGES_PATH).forPort(MAILPIT_HTTP_PORT));

    static {
        RABBITMQ.start();
        MYSQL.start();
        MAILPIT.start();
    }

    @DynamicPropertySource
    static void connectToContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.mail.host", MAILPIT::getHost);
        registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(MAILPIT_SMTP_PORT));
    }

    @MockitoSpyBean
    private JavaMailSender mailSender;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void emptyMailbox() throws Exception {
        http.send(mailpitRequest().DELETE().build(), HttpResponse.BodyHandlers.discarding());
    }

    private HttpRequest.Builder mailpitRequest() {
        return mailpitRequest(MESSAGES_PATH);
    }

    private HttpRequest.Builder mailpitRequest(String path) {
        return HttpRequest.newBuilder(URI.create("http://" + MAILPIT.getHost() + ":"
                + MAILPIT.getMappedPort(MAILPIT_HTTP_PORT) + path));
    }

    private int emailsInMailbox() throws Exception {
        String body = http.send(mailpitRequest().GET().build(), HttpResponse.BodyHandlers.ofString()).body();
        return objectMapper.readTree(body).get("total").asInt();
    }

    private int emailsTo(String address) throws Exception {
        String path = SEARCH_PATH + URLEncoder.encode("to:" + address, StandardCharsets.UTF_8);
        String body = http.send(mailpitRequest(path).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
        return objectMapper.readTree(body).get("messages_count").asInt();
    }

    private void publish(String routingKey, String eventId, long bookingId, String email) {
        rabbitTemplate.convertAndSend(BOOKINGS_EXCHANGE, routingKey, EVENT_TEMPLATE.formatted(bookingId, email), message -> {
            message.getMessageProperties().setMessageId(eventId);
            return message;
        });
    }

    @Test
    void bookingCreatedEvent_sendsOneEmailEvenWhenDeliveredTwice() throws Exception {
        publish("booking.created", "11111111-1111-1111-1111-111111111111", 1, "first@example.com");
        publish("booking.created", "11111111-1111-1111-1111-111111111111", 1, "first@example.com");
        // The queue is handled in order, so once this second event's email is there the duplicate has already been seen.
        publish("booking.created", "22222222-2222-2222-2222-222222222222", 2, "second@example.com");

        await().atMost(WAIT_LIMIT).untilAsserted(() -> assertEquals(1, emailsTo("second@example.com")));
        assertEquals(2, emailsInMailbox());
    }

    // Proves the "processed" mark is rolled back with the failed email: otherwise the retry would be skipped as a duplicate.
    @Test
    void emailFailsOnce_isRetriedAndStillSent() throws Exception {
        doThrow(new MailSendException("smtp down")).doCallRealMethod().when(mailSender).send(any(SimpleMailMessage.class));

        publish("booking.created", "44444444-4444-4444-4444-444444444444", 4, "retry@example.com");

        await().atMost(WAIT_LIMIT).untilAsserted(() -> assertEquals(1, emailsTo("retry@example.com")));
    }

    @Test
    void eventThatKeepsFailing_endsInTheDeadLetterQueue() {
        publish("booking.unknown", "33333333-3333-3333-3333-333333333333", 3, "third@example.com");

        await().atMost(WAIT_LIMIT).untilAsserted(() -> {
            Message deadLetter = rabbitTemplate.receive(DeadLetterConfig.DEAD_LETTER_QUEUE);
            assertNotNull(deadLetter);
        });
    }
}
