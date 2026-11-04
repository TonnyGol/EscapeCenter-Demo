package Responses;

import java.time.LocalDateTime;

/** One game start time of a room on a day. Holds no personal data, so it is safe to show publicly. */
public record SlotResponse(LocalDateTime startAt, LocalDateTime endAt, boolean available) {
}
