package Controllers;

import App.EscapeCenterServer;
import Entities.WorkerRole;
import Requests.BookingDetails;
import Requests.ClientDetails;
import Requests.CreateBookingRequest;
import Requests.UpdateBookingRequest;
import Support.MySqlIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static Logging.RequestIdFilter.REQUEST_ID_HEADER;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The test dates are in 2099, so the booking horizon is widened and the rate limits are lifted (RateLimitFilterTest covers them).
@SpringBootTest(classes = EscapeCenterServer.class, properties = {
        "app.booking.max-days-ahead=40000",
        "app.rate-limit.bookings-per-minute=100000",
        "app.rate-limit.authenticated-per-minute=100000"})
@AutoConfigureMockMvc
class ApiControllerTest extends MySqlIntegrationTest {

    private static final String ROOM = "ApiTestRoom";
    private static final int ROOM_CAPACITY = 4;
    private static final int ROOM_DURATION_MINUTES = 90;
    private static final int GAMES_PER_DAY = 7;
    private static final String CLIENT_FIRST_NAME = "ApiFirst";
    private static final String PHONE = "0520000000";
    private static final String CHANGED_PHONE = "0531111111";
    private static final String PHONE_WITH_DASHES = "052-000 0000";
    private static final String DAY = "2099-01-01";
    private static final LocalDateTime FIRST_SLOT = LocalDateTime.of(2099, 1, 1, 10, 0);
    private static final LocalDateTime SECOND_SLOT = LocalDateTime.of(2099, 1, 1, 12, 0);
    private static final LocalDateTime OFF_GRID_START = LocalDateTime.of(2099, 1, 1, 10, 30);
    private static final LocalDateTime PAST_START = LocalDateTime.of(2000, 1, 1, 10, 0);
    private static final Long UNKNOWN_ID = Long.MAX_VALUE;
    private static final String ROOMS_URL = "/api/rooms";
    private static final String SCHEDULE_FIRST_START = "15:00:00";
    private static final int SCHEDULE_GAMES_PER_DAY = 3;
    private static final int SCHEDULE_BREAK_MINUTES = 15;
    private static final String OTHER_CLIENT_FIRST_NAME = "Stranger";
    private static final String NEXT_WEEK = "2099-01-08";
    private static final String BOOKINGS_URL = "/api/bookings";
    private static final String CLIENTS_URL = "/api/clients";
    private static final String STAFF_CHECK_URL = "/api/staff/check";
    private static final LocalDateTime BEYOND_HORIZON_START = LocalDateTime.of(2200, 1, 1, 10, 0);
    private static final int RACING_REQUESTS = 10;
    private static final String RACE_PHONE_PREFIX = "054000000";
    private static final String STAFF_USER = "ApiTestStaff";
    private static final String STAFF_PASSWORD = "staff-test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long roomId;

    @BeforeEach
    void addTestRoom() {
        removeTestRows();
        jdbcTemplate.update("INSERT INTO rooms (name, capacity, duration_minutes) VALUES (?, ?, ?)",
                ROOM, ROOM_CAPACITY, ROOM_DURATION_MINUTES);
        roomId = jdbcTemplate.queryForObject("SELECT id FROM rooms WHERE name = ?", Long.class, ROOM);
    }

    @AfterEach
    void removeTestRows() {
        jdbcTemplate.update("DELETE FROM bookings WHERE room_id IN (SELECT id FROM rooms WHERE name = ?)", ROOM);
        jdbcTemplate.update("DELETE FROM clients WHERE first_name = ?", CLIENT_FIRST_NAME);
        jdbcTemplate.update("DELETE FROM rooms WHERE name = ?", ROOM);
    }

    @Test
    void rooms_arePublic() throws Exception {
        mockMvc.perform(get(ROOMS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '" + ROOM + "')].capacity", hasItem(ROOM_CAPACITY)));
    }

    @Test
    void availability_isPublic_marksABookedSlotTaken_andShowsNoPersonalData() throws Exception {
        mockMvc.perform(get(availabilityUrl()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(GAMES_PER_DAY))
                .andExpect(jsonPath("$[0].available").value(true));

        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, CLIENT_FIRST_NAME, 2))).andExpect(status().isCreated());

        mockMvc.perform(get(availabilityUrl()))
                .andExpect(jsonPath("$[0].available").value(false))
                .andExpect(jsonPath("$[1].available").value(true))
                .andExpect(content().string(not(containsString(CLIENT_FIRST_NAME))));
    }

    @Test
    void createBooking_returns201_thenSameSlotReturns409AsProblemDetail() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, CLIENT_FIRST_NAME, 2))).andExpect(status().isCreated());

        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, CLIENT_FIRST_NAME, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void staff_canBookAndEdit_aGameThatAlreadyStarted() throws Exception {
        mockMvc.perform(asAdmin(jsonPost(BOOKINGS_URL, createJson(PAST_START, CLIENT_FIRST_NAME, 2))))
                .andExpect(status().isCreated());

        mockMvc.perform(asAdmin(put(BOOKINGS_URL + "/" + bookingIdAt(PAST_START))).contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(PHONE, 3))).andExpect(status().isNoContent());
        mockMvc.perform(asAdmin(get(BOOKINGS_URL + "?from=2000-01-01&to=2000-01-01&roomId=" + roomId)))
                .andExpect(jsonPath("$[0].participants").value(3));
    }

    @Test
    void createBooking_byACustomer_doesNotChangeAnExistingClient() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, CLIENT_FIRST_NAME, 2))).andExpect(status().isCreated());
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(roomId, SECOND_SLOT, OTHER_CLIENT_FIRST_NAME, PHONE, 2)))
                .andExpect(status().isCreated());

        mockMvc.perform(asAdmin(get(staffListUrl())))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.client.firstName == '" + OTHER_CLIENT_FIRST_NAME + "')]").doesNotExist());
    }

    @Test
    void createBooking_withStartOffTheSlotGrid_returns400() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(OFF_GRID_START, CLIENT_FIRST_NAME, 2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBooking_inThePast_returns400NamingStartAt() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(PAST_START, CLIENT_FIRST_NAME, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("startAt")));
    }

    @Test
    void createBooking_withMoreParticipantsThanTheRoomHolds_returns400() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, CLIENT_FIRST_NAME, ROOM_CAPACITY + 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("participants")));
    }

    @Test
    void createBooking_withBlankFirstName_returns400NamingTheField() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, "", 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("firstName")));
    }

    @Test
    void createBooking_inAnUnknownRoom_returns404() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(UNKNOWN_ID, FIRST_SLOT, CLIENT_FIRST_NAME, 2)))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffCheck_needsALogin() throws Exception {
        mockMvc.perform(get(STAFF_CHECK_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(asAdmin(get(STAFF_CHECK_URL))).andExpect(status().isOk());
    }

    @Test
    void customerBooking_beyondTheHorizon_isRefused() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(BEYOND_HORIZON_START, CLIENT_FIRST_NAME, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("too far ahead")));
    }

    @Test
    void staffList_refusesAnUpsideDownOrHugeRange() throws Exception {
        mockMvc.perform(asAdmin(get(BOOKINGS_URL).param("from", NEXT_WEEK).param("to", DAY)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(asAdmin(get(BOOKINGS_URL).param("from", DAY).param("to", "2101-01-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sameSlotBookedByManyAtOnce_exactlyOneWins() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(RACING_REQUESTS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int racer = 0; racer < RACING_REQUESTS; racer++) {
            String body = createJson(roomId, FIRST_SLOT, CLIENT_FIRST_NAME, RACE_PHONE_PREFIX + racer, 2);
            results.add(pool.submit(() -> {
                go.await();
                return mockMvc.perform(jsonPost(BOOKINGS_URL, body)).andReturn().getResponse().getStatus();
            }));
        }
        go.countDown();
        int created = 0;
        int conflicts = 0;
        for (Future<Integer> result : results) {
            int status = result.get();
            if (status == HttpStatus.CREATED.value()) created++;
            if (status == HttpStatus.CONFLICT.value()) conflicts++;
        }
        pool.shutdown();

        org.junit.jupiter.api.Assertions.assertEquals(1, created);
        org.junit.jupiter.api.Assertions.assertEquals(RACING_REQUESTS - 1, conflicts);
    }

    @Test
    void requestId_isKeptWhenPlain_andReplacedWhenUnsafe() throws Exception {
        mockMvc.perform(get(ROOMS_URL).header(REQUEST_ID_HEADER, "abc-123"))
                .andExpect(header().string(REQUEST_ID_HEADER, "abc-123"));
        mockMvc.perform(get(ROOMS_URL).header(REQUEST_ID_HEADER, "bad id\tERROR forged"))
                .andExpect(header().string(REQUEST_ID_HEADER, not("bad id\tERROR forged")))
                .andExpect(header().exists(REQUEST_ID_HEADER));
        mockMvc.perform(get(ROOMS_URL)).andExpect(header().exists(REQUEST_ID_HEADER));
    }

    @Test
    void staffCheck_rejectsWrongPasswordAndUnknownUser() throws Exception {
        mockMvc.perform(get(STAFF_CHECK_URL).headers(basicAuth(ADMIN_USER, "wrong-password")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(STAFF_CHECK_URL).headers(basicAuth("nobody-" + UNKNOWN_ID, "any")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffRole_logsInButHasNoAdminAccess() throws Exception {
        jdbcTemplate.update("INSERT INTO workers (username, password_hash, role) VALUES (?, ?, ?)",
                STAFF_USER, passwordEncoder.encode(STAFF_PASSWORD), WorkerRole.STAFF.name());
        try {
            mockMvc.perform(get(STAFF_CHECK_URL).headers(basicAuth(STAFF_USER, STAFF_PASSWORD)))
                    .andExpect(status().isForbidden());
        } finally {
            jdbcTemplate.update("DELETE FROM workers WHERE username = ?", STAFF_USER);
        }
    }

    @Test
    void staffEndpoints_withoutLogin_return401() throws Exception {
        mockMvc.perform(get(staffListUrl())).andExpect(status().isUnauthorized());
        mockMvc.perform(put(BOOKINGS_URL + "/1").contentType(MediaType.APPLICATION_JSON).content(updateJson(PHONE)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BOOKINGS_URL + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(CLIENTS_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void staffList_needsDates_andShowsClientDetails() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, CLIENT_FIRST_NAME, 2))).andExpect(status().isCreated());

        mockMvc.perform(asAdmin(get(BOOKINGS_URL))).andExpect(status().isBadRequest());
        mockMvc.perform(asAdmin(get(staffListUrl())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].client.firstName").value(CLIENT_FIRST_NAME))
                .andExpect(jsonPath("$[0].roomName").value(ROOM));
    }

    @Test
    void updateBooking_returns204_thenTheListShowsTheNewPhone_andUnknownIdReturns404() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, CLIENT_FIRST_NAME, 2))).andExpect(status().isCreated());

        mockMvc.perform(asAdmin(put(BOOKINGS_URL + "/" + bookingId())).contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(CHANGED_PHONE))).andExpect(status().isNoContent());
        mockMvc.perform(asAdmin(get(staffListUrl())))
                .andExpect(jsonPath("$[0].client.phoneNumber").value(CHANGED_PHONE));

        mockMvc.perform(asAdmin(put(BOOKINGS_URL + "/" + UNKNOWN_ID)).contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(CHANGED_PHONE))).andExpect(status().isNotFound());
    }

    @Test
    void updateBooking_withMoreParticipantsThanTheRoomHolds_returns400() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, CLIENT_FIRST_NAME, 2))).andExpect(status().isCreated());

        mockMvc.perform(asAdmin(put(BOOKINGS_URL + "/" + bookingId())).contentType(MediaType.APPLICATION_JSON)
                .content(updateJson(PHONE, ROOM_CAPACITY + 1))).andExpect(status().isBadRequest());
    }

    @Test
    void staffList_returnsOnlyTheRequestedDatesAndRoom() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, CLIENT_FIRST_NAME, 2))).andExpect(status().isCreated());

        mockMvc.perform(asAdmin(get(staffListUrl()))).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(asAdmin(get(BOOKINGS_URL + "?from=" + NEXT_WEEK + "&to=" + NEXT_WEEK + "&roomId=" + roomId)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(asAdmin(get(BOOKINGS_URL + "?from=" + DAY + "&to=" + DAY + "&roomId=" + UNKNOWN_ID)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void availability_followsTheRoomsOwnSchedule() throws Exception {
        jdbcTemplate.update("UPDATE rooms SET first_game_start = ?, games_per_day = ?, break_minutes = ? WHERE id = ?",
                SCHEDULE_FIRST_START, SCHEDULE_GAMES_PER_DAY, SCHEDULE_BREAK_MINUTES, roomId);

        mockMvc.perform(get(availabilityUrl()))
                .andExpect(jsonPath("$.length()").value(SCHEDULE_GAMES_PER_DAY))
                .andExpect(jsonPath("$[0].startAt").value(DAY + "T15:00:00"))
                .andExpect(jsonPath("$[1].startAt").value(DAY + "T16:45:00"));
    }

    @Test
    void deleteBooking_returns204_thenSameIdReturns404() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, CLIENT_FIRST_NAME, 2))).andExpect(status().isCreated());
        Long id = bookingId();

        mockMvc.perform(asAdmin(delete(BOOKINGS_URL + "/" + id))).andExpect(status().isNoContent());
        mockMvc.perform(asAdmin(delete(BOOKINGS_URL + "/" + id))).andExpect(status().isNotFound());
    }

    @Test
    void clientSearch_findsAClientByPartOfTheName() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(FIRST_SLOT, CLIENT_FIRST_NAME, 2))).andExpect(status().isCreated());

        mockMvc.perform(asAdmin(get(CLIENTS_URL + "?query=apifir")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.firstName == '" + CLIENT_FIRST_NAME + "')]").exists());
    }

    @Test
    void clients_withTheSameNameAndDifferentPhones_stayTwoClients() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(roomId, FIRST_SLOT, CLIENT_FIRST_NAME, PHONE, 2))).andExpect(status().isCreated());
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(roomId, SECOND_SLOT, CLIENT_FIRST_NAME, CHANGED_PHONE, 2))).andExpect(status().isCreated());

        mockMvc.perform(asAdmin(get(CLIENTS_URL + "?query=" + CLIENT_FIRST_NAME)))
                .andExpect(jsonPath("$[?(@.firstName == '" + CLIENT_FIRST_NAME + "')].phoneNumber", hasItems(PHONE, CHANGED_PHONE)));
    }

    @Test
    void clients_withTheSamePhoneWrittenTwoWays_areOneClient() throws Exception {
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(roomId, FIRST_SLOT, CLIENT_FIRST_NAME, PHONE, 2))).andExpect(status().isCreated());
        mockMvc.perform(jsonPost(BOOKINGS_URL, createJson(roomId, SECOND_SLOT, CLIENT_FIRST_NAME, PHONE_WITH_DASHES, 2))).andExpect(status().isCreated());

        mockMvc.perform(asAdmin(get(CLIENTS_URL + "?query=" + CLIENT_FIRST_NAME)))
                .andExpect(jsonPath("$[?(@.firstName == '" + CLIENT_FIRST_NAME + "')].phoneNumber", contains(PHONE)));
    }

    private String availabilityUrl() {
        return ROOMS_URL + "/" + roomId + "/availability?date=" + DAY;
    }

    private String staffListUrl() {
        return BOOKINGS_URL + "?from=" + DAY + "&to=" + DAY + "&roomId=" + roomId;
    }

    private Long bookingId() {
        return bookingIdAt(FIRST_SLOT);
    }

    private Long bookingIdAt(LocalDateTime startAt) {
        return jdbcTemplate.queryForObject("SELECT id FROM bookings WHERE room_id = ? AND start_at = ?",
                Long.class, roomId, Timestamp.valueOf(startAt));
    }

    private static MockHttpServletRequestBuilder jsonPost(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.headers(basicAuth(ADMIN_USER, ADMIN_PASSWORD));
    }

    private static HttpHeaders basicAuth(String user, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(user, password);
        return headers;
    }

    private String createJson(LocalDateTime startAt, String firstName, int participants) throws Exception {
        return createJson(roomId, startAt, firstName, participants);
    }

    private String createJson(Long room, LocalDateTime startAt, String firstName, int participants) throws Exception {
        return createJson(room, startAt, firstName, PHONE, participants);
    }

    private String createJson(Long room, LocalDateTime startAt, String firstName, String phoneNumber, int participants)
            throws Exception {
        return objectMapper.writeValueAsString(new CreateBookingRequest(room, startAt,
                new ClientDetails(firstName, "ApiLast", phoneNumber, "api@example.com"),
                new BookingDetails(participants, "beginner", "note", "#ffffff")));
    }

    private String updateJson(String phoneNumber) throws Exception {
        return updateJson(phoneNumber, 2);
    }

    private String updateJson(String phoneNumber, int participants) throws Exception {
        return objectMapper.writeValueAsString(new UpdateBookingRequest(
                new ClientDetails(CLIENT_FIRST_NAME, "ApiLast", phoneNumber, "api@example.com"),
                new BookingDetails(participants, "beginner", "note", "#ffffff")));
    }
}
