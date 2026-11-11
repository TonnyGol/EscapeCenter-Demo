package DataBaseService;

import Entities.Booking;
import Entities.Client;
import Entities.OutboxEvent;
import Entities.Room;
import Repositories.BookingRepository;
import Repositories.ClientRepository;
import Repositories.OutboxEventRepository;
import Repositories.RoomRepository;
import Requests.BookingDetails;
import Requests.ClientDetails;
import Requests.CreateBookingRequest;
import Requests.UpdateBookingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    private static final long ROOM_ID = 1L;
    private static final long BOOKING_ID = 7L;
    private static final int MAX_DAYS_AHEAD = 30;
    private static final int ROOM_CAPACITY = 6;
    private static final int GAME_MINUTES = 60;
    private static final int GAMES_PER_DAY = 4;
    private static final LocalTime FIRST_GAME_START = LocalTime.of(10, 0);
    private static final String PHONE = "0501234567";
    private static final String PHONE_WITH_PUNCTUATION = "050-123 4567";

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private BookingService bookingService;
    private Room room;

    @BeforeEach
    void setUp() {
        // findAndRegisterModules adds Java time support, which the event's LocalDateTime needs (Spring's own mapper has it).
        bookingService = new BookingService(bookingRepository, clientRepository, roomRepository, outboxEventRepository,
                new ObjectMapper().findAndRegisterModules(), MAX_DAYS_AHEAD);
        room = newRoom();
    }

    // Room has no public constructor or setters: the database owns it, so tests fill the fields directly.
    private static Room newRoom() {
        Room room = BeanUtils.instantiateClass(Room.class);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);
        ReflectionTestUtils.setField(room, "capacity", ROOM_CAPACITY);
        ReflectionTestUtils.setField(room, "durationMinutes", GAME_MINUTES);
        ReflectionTestUtils.setField(room, "firstGameStart", FIRST_GAME_START);
        ReflectionTestUtils.setField(room, "gamesPerDay", GAMES_PER_DAY);
        return room;
    }

    private void stubRoom() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
    }

    private Booking tomorrowsBooking() {
        LocalDateTime startAt = firstGameOn(LocalDate.now().plusDays(1));
        Booking booking = new Booking(room, startAt, startAt.plusMinutes(GAME_MINUTES));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        return booking;
    }

    private static LocalDateTime firstGameOn(LocalDate day) {
        return day.atTime(FIRST_GAME_START);
    }

    private static ClientDetails clientDetails() {
        return new ClientDetails("Dana", "Levi", PHONE, "dana@example.com");
    }

    private static BookingDetails bookingDetails(int participants) {
        return new BookingDetails(participants, "beginner", "no notes", "red");
    }

    private static CreateBookingRequest request(LocalDateTime startAt, int participants) {
        return new CreateBookingRequest(ROOM_ID, startAt, clientDetails(), bookingDetails(participants));
    }

    private static void assertStatus(HttpStatus expected, Runnable action) {
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(expected, thrown.getStatusCode());
    }

    @Test
    void createBooking_savesBookingWithEndTimeFromRoomDuration() {
        stubRoom();
        LocalDateTime startAt = firstGameOn(LocalDate.now().plusDays(1));
        when(clientRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));

        bookingService.createBooking(request(startAt, 4), false);

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertEquals(startAt, saved.getValue().getStartAt());
        assertEquals(startAt.plusMinutes(GAME_MINUTES), saved.getValue().getEndAt());
        assertEquals(4, saved.getValue().getParticipants());
    }

    @Test
    void createBooking_savesBookingCreatedEventInTheOutbox() {
        stubRoom();
        when(clientRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));

        bookingService.createBooking(request(firstGameOn(LocalDate.now().plusDays(1)), 4), false);

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(saved.capture());
        assertEquals(OutboxEvent.BOOKING_CREATED, saved.getValue().getType());
        assertTrue(saved.getValue().getPayload().contains("dana@example.com"));
    }

    @Test
    void createBooking_rejectsTakenSlotWith409() {
        stubRoom();
        LocalDateTime startAt = firstGameOn(LocalDate.now().plusDays(1));
        when(bookingRepository.existsByRoomIdAndStartAt(ROOM_ID, startAt)).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> bookingService.createBooking(request(startAt, 4), false));
        verify(bookingRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void createBooking_rejectsPastDateForCustomer() {
        stubRoom();
        LocalDateTime startAt = firstGameOn(LocalDate.now().minusDays(1));

        assertStatus(HttpStatus.BAD_REQUEST, () -> bookingService.createBooking(request(startAt, 4), false));
    }

    @Test
    void createBooking_allowsPastDateForStaff() {
        stubRoom();
        LocalDateTime startAt = firstGameOn(LocalDate.now().minusDays(1));
        when(clientRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));

        bookingService.createBooking(request(startAt, 4), true);

        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_rejectsDateBeyondBookingHorizon() {
        stubRoom();
        LocalDateTime startAt = firstGameOn(LocalDate.now().plusDays(MAX_DAYS_AHEAD + 1));

        assertStatus(HttpStatus.BAD_REQUEST, () -> bookingService.createBooking(request(startAt, 4), false));
    }

    @Test
    void createBooking_rejectsTooManyParticipants() {
        stubRoom();
        LocalDateTime startAt = firstGameOn(LocalDate.now().plusDays(1));

        assertStatus(HttpStatus.BAD_REQUEST,
                () -> bookingService.createBooking(request(startAt, ROOM_CAPACITY + 1), false));
    }

    @Test
    void createBooking_rejectsStartTimeOffTheRoomSchedule() {
        stubRoom();
        LocalDateTime offGrid = firstGameOn(LocalDate.now().plusDays(1)).plusMinutes(5);

        assertStatus(HttpStatus.BAD_REQUEST, () -> bookingService.createBooking(request(offGrid, 4), false));
    }

    @Test
    void createBooking_rejectsUnknownRoomWith404() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND,
                () -> bookingService.createBooking(request(firstGameOn(LocalDate.now().plusDays(1)), 4), false));
    }

    @Test
    void createBooking_customerCannotChangeExistingClientDetails() {
        stubRoom();
        Client existing = new Client(PHONE);
        existing.setFirstName("Original");
        when(clientRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(existing));

        bookingService.createBooking(request(firstGameOn(LocalDate.now().plusDays(1)), 4), false);

        assertEquals("Original", existing.getFirstName());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createBooking_samePhoneWrittenDifferently_findsTheSameClient() {
        stubRoom();
        Client existing = new Client(PHONE);
        when(clientRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(existing));
        ClientDetails formattedPhone = new ClientDetails("Dana", "Levi", PHONE_WITH_PUNCTUATION, "dana@example.com");

        bookingService.createBooking(new CreateBookingRequest(ROOM_ID, firstGameOn(LocalDate.now().plusDays(1)),
                formattedPhone, bookingDetails(4)), false);

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertEquals(existing, saved.getValue().getClient());
    }

    @Test
    void createBooking_phoneWithoutDigitsIs400() {
        stubRoom();
        ClientDetails noDigits = new ClientDetails("Dana", "Levi", "---", "dana@example.com");

        assertStatus(HttpStatus.BAD_REQUEST, () -> bookingService.createBooking(
                new CreateBookingRequest(ROOM_ID, firstGameOn(LocalDate.now().plusDays(1)), noDigits, bookingDetails(4)),
                false));
    }

    @Test
    void deleteBooking_removesExistingBooking() {
        Booking booking = new Booking(room, firstGameOn(LocalDate.now()), firstGameOn(LocalDate.now()).plusHours(1));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        bookingService.deleteBooking(BOOKING_ID);

        verify(bookingRepository).delete(booking);
    }

    @Test
    void deleteBooking_unknownIdIs404() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> bookingService.deleteBooking(BOOKING_ID));
    }

    @Test
    void updateBooking_replacesDetailsAndClient() {
        Booking booking = tomorrowsBooking();
        when(clientRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));

        bookingService.updateBooking(BOOKING_ID, new UpdateBookingRequest(clientDetails(), bookingDetails(5)));

        assertEquals(5, booking.getParticipants());
        assertEquals("Dana", booking.getClient().getFirstName());
    }

    @Test
    void updateBooking_toAnotherClientsPhone_keepsThatClientsDetails() {
        Booking booking = tomorrowsBooking();
        booking.setClient(new Client("0509999999"));
        Client other = new Client(PHONE);
        other.setFirstName("Noa");
        when(clientRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(other));

        bookingService.updateBooking(BOOKING_ID, new UpdateBookingRequest(clientDetails(), bookingDetails(5)));

        assertEquals(other, booking.getClient());
        assertEquals("Noa", other.getFirstName());
    }

    @Test
    void updateBooking_rejectsTooManyParticipants() {
        tomorrowsBooking();

        assertStatus(HttpStatus.BAD_REQUEST, () -> bookingService.updateBooking(BOOKING_ID,
                new UpdateBookingRequest(clientDetails(), bookingDetails(ROOM_CAPACITY + 1))));
    }

    @Test
    void getBookings_rejectsRangeEndingBeforeStart() {
        LocalDate today = LocalDate.now();

        assertStatus(HttpStatus.BAD_REQUEST, () -> bookingService.getBookings(today, today.minusDays(1), null));
    }
}
