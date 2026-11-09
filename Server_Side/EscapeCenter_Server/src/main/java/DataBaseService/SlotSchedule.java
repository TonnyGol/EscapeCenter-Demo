package DataBaseService;

import Entities.Room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/** The game start times of a room on a day, from the room's own first start, game length, break and game count. */
final class SlotSchedule {

    private SlotSchedule() {
    }

    static List<LocalDateTime> startTimes(LocalDate date, Room room) {
        long minutesBetweenStarts = room.getDurationMinutes() + room.getBreakMinutes();
        return IntStream.range(0, room.getGamesPerDay())
                .mapToObj(game -> date.atTime(room.getFirstGameStart()).plusMinutes(game * minutesBetweenStarts))
                .toList();
    }
}
