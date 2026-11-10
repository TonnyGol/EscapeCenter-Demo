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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {
    private static final String BOOKING_NOT_FOUND_MESSAGE = "Booking not found.";
    private static final String OFF_GRID_START_MESSAGE = "startAt: not a game start time of this room.";
    private static final String PAST_START_MESSAGE = "startAt: must be a future date and time.";
    private static final String SLOT_TAKEN_MESSAGE = "This slot is already booked.";
    private static final String TOO_FAR_AHEAD_MESSAGE = "startAt: too far ahead, bookings open at most %d days in advance.";
    private static final String BAD_RANGE_MESSAGE = "from, to: 'to' must not be before 'from' and the range is at most %d days.";
    private static final long MAX_RANGE_DAYS = 366;
    private static final String NO_PHONE_DIGITS_MESSAGE = "client.phoneNumber: must contain digits.";
    private static final String NON_DIGITS_REGEX = "\\D";
    private static final String INTERNATIONAL_PREFIX = "+";
    private static final String ROOM_TOO_SMALL_MESSAGE = "booking.participants: this room holds at most %d people.";

    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final RoomRepository roomRepository;
    private final int maxDaysAhead;

    public BookingService(BookingRepository bookingRepository, ClientRepository clientRepository,
                          RoomRepository roomRepository, @Value("${app.booking.max-days-ahead}") int maxDaysAhead) {
        this.maxDaysAhead = maxDaysAhead;
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
        if (!bookedByStaff) {
            requireStartInCustomerWindow(request.startAt());
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
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_RANGE_MESSAGE.formatted(MAX_RANGE_DAYS));
        }
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
        return clientRepository.findByPhoneNumber(normalizedPhone(details.phoneNumber()))
                .orElseGet(() -> saveClientDetails(details));
    }

    // Staff only: the given name and email replace the stored ones.
    private Client saveClientDetails(ClientDetails details) {
        String phoneNumber = normalizedPhone(details.phoneNumber());
        Client client = clientRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> new Client(phoneNumber));
        client.setFirstName(details.firstName());
        client.setLastName(details.lastName());
        client.setEmail(details.email());
        return clientRepository.save(client);
    }

    // The phone number is the client's identity, so "050-123 4567" and "0501234567" must be the same client.
    private static String normalizedPhone(String phoneNumber) {
        String digits = phoneNumber.replaceAll(NON_DIGITS_REGEX, "");
        if (digits.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, NO_PHONE_DIGITS_MESSAGE);
        }
        return phoneNumber.trim().startsWith(INTERNATIONAL_PREFIX) ? INTERNATIONAL_PREFIX + digits : digits;
    }

    private static void copyDetails(Booking booking, BookingDetails details) {
        booking.setParticipants(details.participants());
        booking.setExperience(details.experience());
        booking.setColor(details.color());
        booking.setNotes(details.notes());
    }

    // The horizon stops a script from filling the calendar of every room for years ahead.
    private void requireStartInCustomerWindow(LocalDateTime startAt) {
        LocalDateTime now = LocalDateTime.now();
        if (!startAt.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PAST_START_MESSAGE);
        }
        if (startAt.isAfter(now.plusDays(maxDaysAhead))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TOO_FAR_AHEAD_MESSAGE.formatted(maxDaysAhead));
        }
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
