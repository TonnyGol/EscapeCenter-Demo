package Repositories;

import Entities.Booking;
import Entities.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByRoomIdAndStartAt(Long roomId, LocalDateTime startAt);

    @Query("select b.startAt from Booking b where b.room.id = :roomId and b.startAt >= :from and b.startAt < :to")
    List<LocalDateTime> findStartTimes(@Param("roomId") Long roomId, @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    // The graph loads client and room in the same query, so mapping a list does not run one query per booking.
    @EntityGraph(attributePaths = {"client", "room"})
    @Query("select b from Booking b where b.startAt >= :from and b.startAt < :to "
            + "and (:roomId is null or b.room.id = :roomId) order by b.startAt")
    List<Booking> findInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                              @Param("roomId") Long roomId);

    // An empty text matches everyone; the caller passes lower-case text, so the match ignores case.
    @EntityGraph(attributePaths = {"client", "room"})
    @Query("select b from Booking b where b.startAt >= :from and b.startAt < :to "
            + "and (:status is null or b.status = :status) "
            + "and (:text = '' or lower(concat(b.client.firstName, ' ', b.client.lastName)) like concat('%', :text, '%') "
            + "or b.client.phoneNumber like concat('%', :text, '%') "
            + "or lower(b.client.email) like concat('%', :text, '%')) order by b.startAt")
    List<Booking> search(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                         @Param("text") String text, @Param("status") BookingStatus status);

    @EntityGraph(attributePaths = {"client", "room"})
    List<Booking> findByClientIdOrderByStartAtDesc(Long clientId);
}
