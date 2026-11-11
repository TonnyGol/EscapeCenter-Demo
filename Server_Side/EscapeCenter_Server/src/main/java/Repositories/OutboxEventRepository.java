package Repositories;

import Entities.OutboxEvent;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    // Timeout -2 means SKIP LOCKED: a second API instance takes other rows instead of sending the same ones.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<OutboxEvent> findByPublishedAtIsNullOrderByCreatedAt(Pageable page);

    long deleteByPublishedAtBefore(LocalDateTime cutoff);
}
