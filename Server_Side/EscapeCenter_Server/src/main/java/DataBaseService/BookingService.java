package DataBaseService;

import Entities.Booking;
import Entities.Client;
import Entities.Room;
import Repositories.BookingRepository;
import Repositories.ClientRepository;
import Repositories.RoomRepository;
import Requests.BookingDetails;
import Requests.ClientDetails;
import Requests.CreateBookingRequest;
import Requests.UpdateBookingRequest;
import Responses.StaffBookingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {
    private static final String BOOKING_NOT_FOUND_MESSAGE = "Booking not found.";
    private static final String OFF_GRID_START_MESSAGE = "startAt: not a game start time of this room.";
    private static final String PAST_START_MESSAGE = "startAt: must be a future date and time.";
    private static final String SLOT_TAKEN_MESSAGE = "This slot is already booked.";
    private static final String ROOM_TOO_SMALL_MESSAGE = "booking.participants: this room holds at most %d people.";

    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final RoomRepository roomRepository;

    public BookingService(BookingRepository bookingRepository, ClientRepository clientRepository,
                          RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.clientRepository = clientRepository;
        this.roomRepository = roomRepository;
    }

    /**
     * Customers can only book future slots and never change an existing client's details. Staff can book a game
     * that already started (to log it and keep notes) and their client details replace the stored ones.
     * The service checks for a taken slot first so the caller gets a clear 409; the unique (room, start)
     * constraint in the database still catches two requests that pass the check at the same moment.
     */
    @Transactional
    public void createBooking(CreateBookingRequest request, boolean bookedByStaff) {
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, RoomService.ROOM_NOT_FOUND_MESSAGE));
        if (!bookedByStaff && !request.startAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PAST_START_MESSAGE);
        }
        if (!SlotSchedule.startTimes(request.startAt().toLocalDate(), room).contains(request.startAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, OFF_GRID_START_MESSAGE);
        }
        requireRoomHolds(room, request.booking().participants());
        if (bookingRepository.existsByRoomIdAndStartAt(room.getId(), request.startAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, SLOT_TAKEN_MESSAGE);
        }
        Booking booking = new Booking(room, request.startAt(), request.startAt().plusMinutes(room.getDurationMinutes()));
        booking.setClient(bookedByStaff ? saveClientDetails(request.client()) : findOrCreateClient(request.client()));
        copyDetails(booking, request.booking());
        bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public List<StaffBookingResponse> getBookings(LocalDate from, LocalDate to, Long roomId) {
        return bookingRepository.findInRange(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), roomId).stream()
                .map(StaffBookingResponse::from)
                .toList();
    }

    @Transactional
    public void updateBooking(Long id, UpdateBookingRequest request) {
        Booking booking = findBookingById(id);
        requireRoomHolds(booking.getRoom(), request.booking().participants());
        booking.setClient(saveClientDetails(request.client()));
        copyDetails(booking, request.booking());
    }

    @Transactional
    public void deleteBooking(Long id) {
        bookingRepository.delete(findBookingById(id));
    }

    // The phone number identifies a client. A stranger who knows it must not be able to change that client's name or email.
    private Client findOrCreateClient(ClientDetails details) {
        return clientRepository.findByPhoneNumber(details.phoneNumber())
                .orElseGet(() -> saveClientDetails(details));
    }

    // Staff only: the given name and email replace the stored ones.
    private Client saveClientDetails(ClientDetails details) {
        Client client = clientRepository.findByPhoneNumber(details.phoneNumber())
                .orElseGet(() -> new Client(details.phoneNumber()));
        client.setFirstName(details.firstName());
        client.setLastName(details.lastName());
        client.setEmail(details.email());
        return clientRepository.save(client);
    }

    private static void copyDetails(Booking booking, BookingDetails details) {
        booking.setParticipants(details.participants());
        booking.setExperience(details.experience());
        booking.setColor(details.color());
        booking.setNotes(details.notes());
    }

    private static void requireRoomHolds(Room room, int participants) {
        if (participants > room.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ROOM_TOO_SMALL_MESSAGE.formatted(room.getCapacity()));
        }
    }

    private Booking findBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, BOOKING_NOT_FOUND_MESSAGE));
    }
}
