package Repositories;

import App.EscapeCenterServer;
import Entities.Booking;
import Entities.Client;
import Entities.Room;
import Support.MySqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Runs on a real MySQL with the real Flyway migrations; each test is rolled back.
@SpringBootTest(classes = EscapeCenterServer.class)
@Transactional
class BookingRepositoryTest extends MySqlIntegrationTest {
    private static final String SLOT_CONSTRAINT_NAME = "uq_bookings_room_start";
    private static final String PHONE_CONSTRAINT_NAME = "uq_clients_phone";
    private static final String PHONE = "050-1111111";
    private static final int GAME_MINUTES = 60;
    private static final LocalDate DAY = LocalDate.of(2099, 1, 1);
    private static final LocalDateTime MORNING = DAY.atTime(10, 0);
    private static final LocalDateTime EVENING = DAY.atTime(18, 0);

    @Autowired
    private BookingRepository bookings;
    @Autowired
    private ClientRepository clients;
    @Autowired
    private RoomRepository rooms;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Room firstRoom;
    private Room secondRoom;
    private Client client;

    @BeforeEach
    void loadSeededRoomsAndAClient() {
        List<Room> seeded = rooms.findAll();
        firstRoom = seeded.get(0);
        secondRoom = seeded.get(1);
        client = clients.saveAndFlush(newClient(PHONE));
    }

    private static Client newClient(String phone) {
        Client newClient = new Client(phone);
        newClient.setFirstName("Dana");
        newClient.setLastName("Levi");
        return newClient;
    }

    private Booking booking(Room room, LocalDateTime startAt) {
        Booking booking = new Booking(room, startAt, startAt.plusMinutes(GAME_MINUTES));
        booking.setClient(client);
        booking.setParticipants(2);
        return booking;
    }

    @Test
    void flywayMigrations_allApplied_andSeedRooms() {
        Integer failedMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class);

        assertEquals(0, failedMigrations);
        assertThat(rooms.count(), greaterThan(1L));
    }

    @Test
    void sameRoomAndStart_violatesTheUniqueConstraint() {
        bookings.saveAndFlush(booking(firstRoom, MORNING));

        DataIntegrityViolationException thrown = assertThrows(DataIntegrityViolationException.class,
                () -> bookings.saveAndFlush(booking(firstRoom, MORNING)));

        assertThat(thrown.getMostSpecificCause().getMessage(), containsString(SLOT_CONSTRAINT_NAME));
    }

    @Test
    void sameStartInAnotherRoom_isAllowed() {
        bookings.saveAndFlush(booking(firstRoom, MORNING));

        bookings.saveAndFlush(booking(secondRoom, MORNING));

        assertTrue(bookings.existsByRoomIdAndStartAt(secondRoom.getId(), MORNING));
    }

    @Test
    void roomThatHoldsOnePerson_isRefusedByTheDatabase() {
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO rooms (name, capacity, duration_minutes) VALUES (?, 1, ?)", "EmptyRoom", GAME_MINUTES));
    }

    @Test
    void samePhone_violatesTheUniqueClientConstraint() {
        DataIntegrityViolationException thrown = assertThrows(DataIntegrityViolationException.class,
                () -> clients.saveAndFlush(newClient(PHONE)));

        assertThat(thrown.getMostSpecificCause().getMessage(), containsString(PHONE_CONSTRAINT_NAME));
    }

    @Test
    void existsByRoomIdAndStartAt_isTrueOnlyForTheBookedRoomAndStart() {
        bookings.saveAndFlush(booking(firstRoom, MORNING));

        assertTrue(bookings.existsByRoomIdAndStartAt(firstRoom.getId(), MORNING));
        assertFalse(bookings.existsByRoomIdAndStartAt(secondRoom.getId(), MORNING));
        assertFalse(bookings.existsByRoomIdAndStartAt(firstRoom.getId(), EVENING));
    }

    @Test
    void findStartTimes_returnsOnlyThatRoomAndDay_andTheEndIsExclusive() {
        bookings.save(booking(firstRoom, MORNING));
        bookings.save(booking(firstRoom, DAY.plusDays(1).atStartOfDay()));
        bookings.save(booking(secondRoom, EVENING));
        bookings.flush();

        List<LocalDateTime> taken = bookings.findStartTimes(firstRoom.getId(), DAY.atStartOfDay(),
                DAY.plusDays(1).atStartOfDay());

        assertThat(taken, contains(MORNING));
    }

    @Test
    void findInRange_filtersByDatesAndOptionalRoom_inStartOrder() {
        bookings.save(booking(firstRoom, EVENING));
        bookings.save(booking(secondRoom, MORNING));
        bookings.save(booking(firstRoom, DAY.plusDays(1).atTime(10, 0)));
        bookings.flush();
        LocalDateTime from = DAY.atStartOfDay();
        LocalDateTime to = DAY.plusDays(1).atStartOfDay();

        List<Booking> allRooms = bookings.findInRange(from, to, null);
        List<Booking> oneRoom = bookings.findInRange(from, to, firstRoom.getId());

        assertThat(allRooms.stream().map(Booking::getStartAt).toList(), contains(MORNING, EVENING));
        assertThat(oneRoom.stream().map(Booking::getStartAt).toList(), contains(EVENING));
        assertThat(bookings.findInRange(DAY.minusDays(5).atStartOfDay(), DAY.minusDays(4).atStartOfDay(), null),
                empty());
    }
}
