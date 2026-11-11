package Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    public static final String BOOKING_CREATED = "BookingCreated";

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public void markPublished() {
        this.publishedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
