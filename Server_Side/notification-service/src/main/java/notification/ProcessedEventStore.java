package notification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// RabbitMQ delivers at least once, so the same event can arrive twice. The primary key lets only one delivery win.
@Component
class ProcessedEventStore {
    private static final String MARK_PROCESSED_SQL = "INSERT IGNORE INTO processed_events (id) VALUES (?)";

    private final JdbcTemplate jdbcTemplate;

    ProcessedEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // False when the event was already handled. A second delivery at the same moment waits until the first one commits.
    boolean markProcessed(String eventId) {
        return jdbcTemplate.update(MARK_PROCESSED_SQL, eventId) == 1;
    }
}
