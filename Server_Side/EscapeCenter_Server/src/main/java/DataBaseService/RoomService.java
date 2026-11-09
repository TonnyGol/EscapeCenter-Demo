package DataBaseService;

import Entities.Room;
import Repositories.BookingRepository;
import Repositories.RoomRepository;
import Responses.RoomResponse;
import Responses.SlotResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoomService {
    static final String ROOM_NOT_FOUND_MESSAGE = "Room not found.";

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRooms() {
        return roomRepository.findAll().stream().map(RoomResponse::from).toList();
    }

    /** A slot is available when nobody booked it and it has not started yet. */
    @Transactional(readOnly = true)
    public List<SlotResponse> getAvailability(Long roomId, LocalDate date) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ROOM_NOT_FOUND_MESSAGE));
        Set<LocalDateTime> taken = new HashSet<>(
                bookingRepository.findStartTimes(roomId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()));
        LocalDateTime now = LocalDateTime.now();
        return SlotSchedule.startTimes(date, room).stream()
                .map(start -> new SlotResponse(start, start.plusMinutes(room.getDurationMinutes()),
                        start.isAfter(now) && !taken.contains(start)))
                .toList();
    }
}
